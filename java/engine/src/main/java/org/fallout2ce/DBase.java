package org.fallout2ce;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Port of the archive side of {@code src/dfile.h} / {@code src/dfile.cc}
 * ({@code DBase} open/find/close, DAT + ZIP parsing).
 *
 * <p>Entry lookup uses the same binary search as C {@code bsearch}
 * (glibc {@code l<u} form), so even a non-sorted archive resolves
 * identically. Entry paths are byte-preserving ({@code ISO_8859_1}).
 */
public final class DBase {

    private DBase() {
    }

    public enum Format {
        DAT,
        ZIP
    }

    // DBaseErrorFlags.
    public static final int ERROR_EMPTY = 1 << 1;
    public static final int ERROR_DESCRIPTORS = 1 << 2;
    public static final int ERROR_ZIP64 = 1 << 3;
    public static final int ERROR_ENCRYPTED = 1 << 4;
    public static final int ERROR_MULTI_DISK = 1 << 5;
    public static final int ERROR_UNSUPPORTED_METHOD = 1 << 6;

    /** Mirror of {@code DBaseEntry}. */
    public static final class Entry {
        public String path = "";
        public boolean compressed;
        public int uncompressedSize;
        public int dataSize;
        public int dataOffset;
    }

    /** Mirror of {@code DFileFindData} (no fixed 260 buffers in Java). */
    public static final class FindData {
        public String fileName = "";
        public String pattern = "";
        public int index;
    }

    /** Mirror of {@code struct DBase}. */
    public static final class Archive {
        public String path = "";
        public int dataOffset;
        public Format format;
        public final List<Entry> entries = new ArrayList<>();
        public final List<DFile.Handle> openHandles = new ArrayList<>();
    }

    // 0x4E4F58 dbase_open. Returns null on any failure, like C.
    // errorFlags is only touched on the EMPTY / ZIP-error paths.
    public static Archive dbaseOpen(String filePath, int[] errorFlags) {
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(new File(filePath), "r");
        } catch (IOException e) {
            return null;
        }

        try {
            byte[] magic = new byte[4];
            if (readFullyOrFail(raf, magic, 0, 4) != 4) {
                return null;
            }

            if (magic[0] == 0x50 && magic[1] == 0x4B) {
                if (magic[2] == 0x03 && magic[3] == 0x04) {
                    return dbaseOpenZip(filePath, raf, errorFlags);
                }
                if (magic[2] == 0x05 && magic[3] == 0x06) {
                    if (errorFlags != null) {
                        errorFlags[0] = ERROR_EMPTY;
                    }
                    return null;
                }
            }

            return dbaseOpenDat(filePath, raf);
        } catch (IOException e) {
            return null;
        } finally {
            try {
                raf.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
    }

    private static Archive dbaseOpenDat(String filePath, RandomAccessFile raf) throws IOException {
        long fileSize = raf.length();
        ByteBuffer footer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        raf.seek(fileSize - 8);
        raf.readFully(footer.array());
        int entriesDataSize = footer.getInt(0);
        int dbaseDataSize = footer.getInt(4);

        raf.seek(fileSize - entriesDataSize - 8);
        ByteBuffer counts = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        raf.readFully(counts.array());
        int entriesLength = counts.getInt(0);

        Archive db = new Archive();
        db.format = Format.DAT;

        ByteBuffer entryBuf = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < entriesLength; i++) {
            ByteBuffer pathLenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            raf.readFully(pathLenBuf.array());
            int pathLength = pathLenBuf.getInt(0);

            byte[] pathBytes = new byte[pathLength];
            raf.readFully(pathBytes);

            raf.readFully(entryBuf.array());
            Entry entry = new Entry();
            entry.path = new String(pathBytes, StandardCharsets.ISO_8859_1);
            entry.compressed = entryBuf.get(0) != 0;
            entry.uncompressedSize = entryBuf.getInt(1);
            entry.dataSize = entryBuf.getInt(5);
            entry.dataOffset = entryBuf.getInt(9);
            db.entries.add(entry);
        }

        db.path = filePath;
        db.dataOffset = (int) (fileSize - dbaseDataSize);
        return db;
    }

    private static final int ZIP_CENTRAL_DIR_SIGNATURE = 0x02014b50;
    private static final int ZIP_MAX_EOCD_SEARCH = 65557;

    private static Archive dbaseOpenZip(String filePath, RandomAccessFile raf, int[] errorFlags)
            throws IOException {
        long fileSize = raf.length();
        if (fileSize < 22) {
            return null;
        }

        byte[] tail = new byte[22];
        raf.seek(fileSize - 22);
        raf.readFully(tail);

        long eocdOffset = fileSize - 22;
        boolean found = isEocd(tail, 0);

        if (!found) {
            long searchStart = Math.max(0, fileSize - ZIP_MAX_EOCD_SEARCH);
            int searchSize = (int) (fileSize - searchStart);
            byte[] buf = new byte[searchSize];
            raf.seek(searchStart);
            raf.readFully(buf);
            for (int i = searchSize - 22; i >= 0; i--) {
                if (isEocd(buf, i)) {
                    eocdOffset = searchStart + i;
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            return null;
        }

        raf.seek(eocdOffset + 10);
        ByteBuffer eocd = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
        raf.readFully(eocd.array());
        int totalEntries = eocd.getShort(0) & 0xFFFF;
        long centralDirSize = eocd.getInt(2) & 0xFFFFFFFFL;
        long centralDirOffset = eocd.getInt(6) & 0xFFFFFFFFL;

        int zipErrorFlags = 0;
        if (totalEntries == 0xFFFF || centralDirSize == 0xFFFFFFFFL || centralDirOffset == 0xFFFFFFFFL) {
            zipErrorFlags |= ERROR_ZIP64;
        } else if (totalEntries == 0) {
            zipErrorFlags |= ERROR_EMPTY;
        }
        if (zipErrorFlags != 0) {
            if (errorFlags != null) {
                errorFlags[0] = zipErrorFlags;
            }
            return null;
        }

        Archive db = new Archive();
        db.format = Format.ZIP;
        db.dataOffset = 0;
        db.path = filePath;

        raf.seek(centralDirOffset);
        ByteBuffer fixed = ByteBuffer.allocate(46).order(ByteOrder.LITTLE_ENDIAN);
        for (int entryIndex = 0; entryIndex < totalEntries; entryIndex++) {
            raf.readFully(fixed.array());
            if (fixed.getInt(0) != ZIP_CENTRAL_DIR_SIGNATURE) {
                return failZip(db, errorFlags, zipErrorFlags);
            }
            int flags = fixed.getShort(8) & 0xFFFF;
            int method = fixed.getShort(10) & 0xFFFF;
            int compressedSize = fixed.getInt(20);
            int uncompressedSize = fixed.getInt(24);
            int fileNameLength = fixed.getShort(28) & 0xFFFF;
            int extraFieldLength = fixed.getShort(30) & 0xFFFF;
            int fileCommentLength = fixed.getShort(32) & 0xFFFF;
            int diskNumberStart = fixed.getShort(34) & 0xFFFF;
            int localHeaderOffset = fixed.getInt(42);

            if ((flags & 0x01) != 0) {
                zipErrorFlags |= ERROR_ENCRYPTED;
            }
            if ((flags & 0x08) != 0) {
                zipErrorFlags |= ERROR_DESCRIPTORS;
            }
            if (diskNumberStart != 0) {
                zipErrorFlags |= ERROR_MULTI_DISK;
            }
            if (compressedSize < 0 || uncompressedSize < 0 || localHeaderOffset < 0) {
                continue;
            }

            byte[] fileNameBytes = new byte[fileNameLength];
            raf.readFully(fileNameBytes);
            // Skip extra field and file comment.
            raf.seek(raf.getFilePointer() + extraFieldLength + fileCommentLength);

            for (int j = 0; j < fileNameLength; j++) {
                if (fileNameBytes[j] == '/') {
                    fileNameBytes[j] = '\\';
                }
            }
            if (fileNameLength > 0 && fileNameBytes[fileNameLength - 1] == '\\') {
                continue;
            }

            boolean compressed;
            if (method == 0) {
                compressed = false;
            } else if (method == 8) {
                compressed = true;
            } else {
                zipErrorFlags |= ERROR_UNSUPPORTED_METHOD;
                continue;
            }

            long savedPos = raf.getFilePointer();
            raf.seek((long) localHeaderOffset + 26);
            ByteBuffer local = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            raf.readFully(local.array());
            int localFileNameLength = local.getShort(0) & 0xFFFF;
            int localExtraFieldLength = local.getShort(2) & 0xFFFF;
            raf.seek(savedPos);

            long dataOffset = (long) localHeaderOffset + 30 + localFileNameLength + localExtraFieldLength;
            if (dataOffset < 0 || dataOffset > Integer.MAX_VALUE) {
                continue;
            }

            Entry entry = new Entry();
            entry.path = new String(fileNameBytes, StandardCharsets.ISO_8859_1);
            entry.compressed = compressed;
            entry.uncompressedSize = uncompressedSize;
            entry.dataSize = compressedSize;
            entry.dataOffset = (int) dataOffset;
            db.entries.add(entry);
        }

        if (zipErrorFlags != 0) {
            if (errorFlags != null) {
                errorFlags[0] = zipErrorFlags;
            }
            return null;
        }

        if (db.entries.size() > 1) {
            db.entries.sort(Comparator.comparing((Entry e) -> e.path, DBase::compareKeys));
        }

        return db;
    }

    private static Archive failZip(Archive db, int[] errorFlags, int zipErrorFlags) {
        if (errorFlags != null) {
            errorFlags[0] = zipErrorFlags;
        }
        return null;
    }

    private static boolean isEocd(byte[] p, int off) {
        return p[off] == 0x50 && p[off + 1] == 0x4B && p[off + 2] == 0x05 && p[off + 3] == 0x06;
    }

    // ASCII case-insensitive (SDL_strcasecmp under the C locale).
    static int compareKeys(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            int ca = asciiLower(a.charAt(i));
            int cb = asciiLower(b.charAt(i));
            if (ca != cb) {
                return ca - cb;
            }
        }
        return a.length() - b.length();
    }

    private static int asciiLower(int ch) {
        return ch >= 'A' && ch <= 'Z' ? ch + ('a' - 'A') : ch;
    }

    // glibc bsearch l<u form, like C (works on the archive as-is).
    static Entry bsearchEntry(List<Entry> entries, String filePath) {
        int l = 0;
        int u = entries.size();
        while (l < u) {
            int idx = (l + u) / 2;
            int cmp = compareKeys(filePath, entries.get(idx).path);
            if (cmp < 0) {
                u = idx;
            } else if (cmp > 0) {
                l = idx + 1;
            } else {
                return entries.get(idx);
            }
        }
        return null;
    }

    // 0x4E5270 dbase_close.
    public static boolean dbaseClose(Archive db) {
        for (DFile.Handle handle : new ArrayList<>(db.openHandles)) {
            DFile.dfileClose(handle);
        }
        db.entries.clear();
        db.path = "";
        db.dataOffset = 0;
        return true;
    }

    // 0x4E5308 dbase_findfirst.
    public static boolean dbaseFindFirstEntry(Archive db, FindData findFileData, String pattern) {
        for (int index = 0; index < db.entries.size(); index++) {
            Entry entry = db.entries.get(index);
            if (FPattern.match(pattern, entry.path)) {
                findFileData.fileName = entry.path;
                findFileData.pattern = pattern;
                findFileData.index = index;
                return true;
            }
        }
        return false;
    }

    // 0x4E53A0 dbase_findnext.
    public static boolean dbaseFindNextEntry(Archive db, FindData findFileData) {
        for (int index = findFileData.index + 1; index < db.entries.size(); index++) {
            Entry entry = db.entries.get(index);
            if (FPattern.match(findFileData.pattern, entry.path)) {
                findFileData.fileName = entry.path;
                findFileData.index = index;
                return true;
            }
        }
        return false;
    }

    // 0x4E541C dbase_findclose.
    public static boolean dbaseFindClose(Archive db, FindData findFileData) {
        return true;
    }

    static int readFullyOrFail(RandomAccessFile raf, byte[] buf, int off, int len) throws IOException {
        int total = 0;
        while (total < len) {
            int n = raf.read(buf, off + total, len - total);
            if (n < 0) {
                break;
            }
            total += n;
        }
        return total;
    }
}
