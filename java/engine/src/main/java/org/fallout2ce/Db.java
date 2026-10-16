package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of {@code src/db.h} / {@code src/db.cc}: VFS mounts, the
 * {@code File} ({@link XFile.Handle}) API with big-endian typed helpers,
 * sorted file lists and the read-progress hook.
 */
public final class Db {

    private Db() {
    }

    static final class FileList {
        final List<String> fileNames = new ArrayList<>();
    }

    static final List<FileList> fileListHead = new ArrayList<>();

    static Runnable readProgressHandler;
    static int readProgressBytesRead;
    static int readProgressChunkSize;

    // 0x4C5D30 db_init.
    public static int dbOpen(String filePath1, String filePath2) {
        if (filePath1 != null) {
            if (!XFile.xbaseOpen(filePath1)) {
                return -1;
            }
        }

        if (filePath2 != null) {
            XFile.xbaseOpen(filePath2);
        }

        return 0;
    }

    // 0x4C5D58 db_total (always 1, like C).
    public static int dbTotal() {
        return 1;
    }

    // 0x4C5D60 db_exit.
    public static void dbCloseAll() {
        XFile.xbaseReopenAll(null);
    }

    // 0x4C5D68 db_dir_entry.
    public static int dbGetFileSize(String filePath, int[] sizePtr) {
        XFile.Handle stream = fileOpen(filePath, "rb");
        if (stream == null) {
            return -1;
        }

        sizePtr[0] = (int) XFile.xfileGetSize(stream);

        fileClose(stream);

        return 0;
    }

    // 0x4C5DD4 db_read_to_buf.
    public static int dbGetFileContents(String filePath, byte[] out, int offset) {
        XFile.Handle stream = fileOpen(filePath, "rb");
        if (stream == null) {
            return -1;
        }

        long size = XFile.xfileGetSize(stream);
        if (readProgressHandler != null) {
            int pos = offset;
            long remainingSize = size;
            long chunkSize = (long) readProgressChunkSize - readProgressBytesRead;

            while (remainingSize >= chunkSize) {
                long bytesRead = XFile.xfileRead(out, pos, 1, (int) chunkSize, stream);
                pos += (int) bytesRead;
                remainingSize -= bytesRead;

                readProgressBytesRead = 0;
                readProgressHandler.run();

                chunkSize = readProgressChunkSize;
            }

            if (remainingSize != 0) {
                readProgressBytesRead += (int) XFile.xfileRead(out, pos, 1, (int) remainingSize, stream);
            }
        } else {
            XFile.xfileRead(out, offset, 1, (int) size, stream);
        }

        fileClose(stream);

        return 0;
    }

    // 0x4C5EB4 db_fclose.
    public static int fileClose(XFile.Handle stream) {
        return XFile.xfileClose(stream);
    }

    // 0x4C5EC8 db_fopen.
    public static XFile.Handle fileOpen(String filename, String mode) {
        return fileOpenImpl(filename, mode, true);
    }

    static XFile.Handle fileOpenImpl(String filename, String mode, boolean useAliases) {
        boolean readOnly = mode != null
                && mode.indexOf('r') >= 0
                && mode.indexOf('w') < 0
                && mode.indexOf('a') < 0
                && mode.indexOf('+') < 0;

        if (useAliases && readOnly) {
            String resolved = SfallFileSystem.sfallFileSystemResolveAlias(filename);
            if (resolved != null) {
                XFile.Handle stream = fileOpenImpl(resolved, mode, false);
                if (stream != null) {
                    return stream;
                }
            }
        }

        return XFile.xfileOpen(filename, mode);
    }

    // 0x4C5ED0 db_fprintf.
    public static int filePrintFormatted(XFile.Handle stream, String format, Object... args) {
        return XFile.xfilePrintFormattedArgs(stream, format, args);
    }

    // 0x4C5F24 db_fgetc.
    public static int fileReadChar(XFile.Handle stream) {
        if (readProgressHandler != null) {
            int ch = XFile.xfileReadChar(stream);

            readProgressBytesRead++;
            if (readProgressBytesRead >= readProgressChunkSize) {
                readProgressHandler.run();
                readProgressBytesRead = 0;
            }

            return ch;
        }

        return XFile.xfileReadChar(stream);
    }

    // 0x4C5F70 db_fgets. Returns null on EOF, like C.
    public static String fileReadString(XFile.Handle stream, int size) {
        if (readProgressHandler != null) {
            String line = XFile.xfileReadString(stream, size);
            if (line == null) {
                return null;
            }

            readProgressBytesRead += line.length();
            while (readProgressBytesRead >= readProgressChunkSize) {
                readProgressHandler.run();
                readProgressBytesRead -= readProgressChunkSize;
            }

            return line;
        }

        return XFile.xfileReadString(stream, size);
    }

    // 0x4C5FEC db_fputs.
    public static int fileWriteString(String string, XFile.Handle stream) {
        return XFile.xfileWriteString(string, stream);
    }

    // 0x4C5FFC db_fread. Returns full items read.
    public static long fileRead(byte[] out, int offset, int size, int count, XFile.Handle stream) {
        if (readProgressHandler != null) {
            int pos = offset;

            long totalBytesRead = 0;
            long remainingSize = (long) size * count;
            long chunkSize = (long) readProgressChunkSize - readProgressBytesRead;

            while (remainingSize >= chunkSize) {
                long bytesRead = XFile.xfileRead(out, pos, 1, (int) chunkSize, stream);
                pos += (int) bytesRead;
                totalBytesRead += bytesRead;
                remainingSize -= bytesRead;

                readProgressBytesRead = 0;
                readProgressHandler.run();

                chunkSize = readProgressChunkSize;
            }

            if (remainingSize != 0) {
                long bytesRead = XFile.xfileRead(out, pos, 1, (int) remainingSize, stream);
                readProgressBytesRead += (int) bytesRead;
                totalBytesRead += bytesRead;
            }

            return size == 0 ? 0 : totalBytesRead / size;
        }

        return XFile.xfileRead(out, offset, size, count, stream);
    }

    // 0x4C60B8 db_fwrite.
    public static long fileWrite(byte[] data, int offset, int size, int count, XFile.Handle stream) {
        return XFile.xfileWrite(data, offset, size, count, stream);
    }

    // 0x4C60C0 db_fseek.
    public static long fileSeek(XFile.Handle stream, long offset, int origin) {
        return XFile.xfileSeek(stream, offset, origin);
    }

    // 0x4C60C8 db_ftell.
    public static long fileTell(XFile.Handle stream) {
        return XFile.xfileTell(stream);
    }

    // 0x4C60D0 db_rewind.
    public static void fileRewind(XFile.Handle stream) {
        XFile.xfileRewind(stream);
    }

    // 0x4C60D8 db_feof.
    public static int fileEof(XFile.Handle stream) {
        return XFile.xfileEof(stream);
    }

    // 0x4C60E0 db_freadByte.
    public static int fileReadUInt8(XFile.Handle stream, int[] valuePtr) {
        int value = fileReadChar(stream);
        if (value == -1) {
            return -1;
        }

        valuePtr[0] = value & 0xFF;

        return 0;
    }

    // 0x4C60F4 db_freadShort (big-endian).
    public static int fileReadInt16(XFile.Handle stream, int[] valuePtr) {
        int[] high = new int[1];
        if (fileReadUInt8(stream, high) == -1) {
            return -1;
        }

        int[] low = new int[1];
        if (fileReadUInt8(stream, low) == -1) {
            return -1;
        }

        valuePtr[0] = (short) ((high[0] << 8) | low[0]);

        return 0;
    }

    // Uncollapsed 0x4C60F4 (unsigned short).
    public static int fileReadUInt16(XFile.Handle stream, int[] valuePtr) {
        int[] value = new int[1];
        if (fileReadInt16(stream, value) == -1) {
            return -1;
        }
        valuePtr[0] = value[0] & 0xFFFF;
        return 0;
    }

    // 0x4C614C db_freadInt (big-endian; -1 on short read).
    public static int fileReadInt32(XFile.Handle stream, int[] valuePtr) {
        byte[] raw = new byte[4];
        if (XFile.xfileRead(raw, 0, 4, 1, stream) != 1) {
            return -1;
        }

        valuePtr[0] = ((raw[0] & 0xFF) << 24) | ((raw[1] & 0xFF) << 16)
                | ((raw[2] & 0xFF) << 8) | (raw[3] & 0xFF);

        return 0;
    }

    // Uncollapsed 0x4C614C (alias).
    public static int dbFreadInt(XFile.Handle stream, int[] valuePtr) {
        return fileReadInt32(stream, valuePtr);
    }

    // Uncollapsed 0x4C614C (unsigned).
    public static int fileReadUInt32(XFile.Handle stream, long[] valuePtr) {
        int[] value = new int[1];
        if (dbFreadInt(stream, value) == -1) {
            return -1;
        }
        valuePtr[0] = value[0] & 0xFFFFFFFFL;
        return 0;
    }

    // Uncollapsed 0x4C614C (float bits).
    public static int fileReadFloat(XFile.Handle stream, float[] valuePtr) {
        int[] value = new int[1];
        if (fileReadInt32(stream, value) == -1) {
            return -1;
        }
        valuePtr[0] = Float.intBitsToFloat(value[0]);
        return 0;
    }

    public static int fileReadBool(XFile.Handle stream, boolean[] valuePtr) {
        int[] value = new int[1];
        if (fileReadInt32(stream, value) == -1) {
            return -1;
        }

        valuePtr[0] = value[0] != 0;

        return 0;
    }

    // 0x4C61AC db_fwriteByte.
    public static int fileWriteUInt8(XFile.Handle stream, int value) {
        return XFile.xfileWriteChar(value, stream);
    }

    // 0x4C61C8 db_fwriteShort (big-endian).
    public static int fileWriteInt16(XFile.Handle stream, int value) {
        if (fileWriteUInt8(stream, (value >> 8) & 0xFF) == -1) {
            return -1;
        }

        if (fileWriteUInt8(stream, value & 0xFF) == -1) {
            return -1;
        }

        return 0;
    }

    // Uncollapsed 0x4C61C8 (unsigned short).
    public static int fileWriteUInt16(XFile.Handle stream, int value) {
        return fileWriteInt16(stream, value);
    }

    // 0x4C6214 db_fwriteInt.
    public static int fileWriteInt32(XFile.Handle stream, int value) {
        return dbFwriteLong(stream, value);
    }

    // 0x4C6244 db_fwriteLong.
    public static int dbFwriteLong(XFile.Handle stream, int value) {
        if (fileWriteInt16(stream, (value >> 16) & 0xFFFF) == -1) {
            return -1;
        }

        if (fileWriteInt16(stream, value & 0xFFFF) == -1) {
            return -1;
        }

        return 0;
    }

    // Uncollapsed 0x4C6214/0x4C6244 (unsigned).
    public static int fileWriteUInt32(XFile.Handle stream, long value) {
        return dbFwriteLong(stream, (int) value);
    }

    // 0x4C62C4 db_fwriteFloat.
    public static int fileWriteFloat(XFile.Handle stream, float value) {
        return dbFwriteLong(stream, Float.floatToIntBits(value));
    }

    public static int fileWriteBool(XFile.Handle stream, boolean value) {
        return dbFwriteLong(stream, value ? 1 : 0);
    }

    // 0x4C62FC db_freadByteCount.
    public static int fileReadUInt8List(XFile.Handle stream, byte[] arr, int offset, int count) {
        for (int index = 0; index < count; index++) {
            int[] ch = new int[1];
            if (fileReadUInt8(stream, ch) == -1) {
                return -1;
            }

            arr[offset + index] = (byte) ch[0];
        }

        return 0;
    }

    // Uncollapsed 0x4C62FC (fixed-length byte string, no terminator).
    public static int fileReadFixedLengthString(XFile.Handle stream, byte[] out, int offset, int length) {
        return fileReadUInt8List(stream, out, offset, length);
    }

    // 0x4C6330 db_freadShortCount.
    public static int fileReadInt16List(XFile.Handle stream, short[] arr, int offset, int count) {
        for (int index = 0; index < count; index++) {
            int[] value = new int[1];
            if (fileReadInt16(stream, value) == -1) {
                return -1;
            }

            arr[offset + index] = (short) (int) value[0];
        }

        return 0;
    }

    // Uncollapsed 0x4C6330 (unsigned shorts).
    public static int fileReadUInt16List(XFile.Handle stream, int[] arr, int offset, int count) {
        for (int index = 0; index < count; index++) {
            int[] value = new int[1];
            if (fileReadUInt16(stream, value) == -1) {
                return -1;
            }

            arr[offset + index] = value[0];
        }

        return 0;
    }

    // 0x4C63BC db_freadIntCount (bulk read + per-item byteswap).
    public static int fileReadInt32List(XFile.Handle stream, int[] arr, int offset, int count) {
        if (count == 0) {
            return 0;
        }

        byte[] raw = new byte[count * 4];
        if (fileRead(raw, 0, raw.length, 1, stream) < 1) {
            return -1;
        }

        for (int index = 0; index < count; index++) {
            int value = ((raw[index * 4] & 0xFF) << 24) | ((raw[index * 4 + 1] & 0xFF) << 16)
                    | ((raw[index * 4 + 2] & 0xFF) << 8) | (raw[index * 4 + 3] & 0xFF);
            arr[offset + index] = value;
        }

        return 0;
    }

    // Uncollapsed 0x4C63BC (alias).
    public static int dbFreadIntCount(XFile.Handle stream, int[] arr, int offset, int count) {
        return fileReadInt32List(stream, arr, offset, count);
    }

    // Uncollapsed 0x4C63BC (unsigned).
    public static int fileReadUInt32List(XFile.Handle stream, long[] arr, int offset, int count) {
        if (count == 0) {
            return 0;
        }

        byte[] raw = new byte[count * 4];
        if (fileRead(raw, 0, raw.length, 1, stream) < 1) {
            return -1;
        }

        for (int index = 0; index < count; index++) {
            long value = ((long) (raw[index * 4] & 0xFF) << 24) | ((raw[index * 4 + 1] & 0xFF) << 16)
                    | ((raw[index * 4 + 2] & 0xFF) << 8) | (raw[index * 4 + 3] & 0xFF);
            arr[offset + index] = value;
        }

        return 0;
    }

    // 0x4C6464 db_fwriteByteCount.
    public static int fileWriteUInt8List(XFile.Handle stream, byte[] arr, int offset, int count) {
        for (int index = 0; index < count; index++) {
            if (fileWriteUInt8(stream, arr[offset + index] & 0xFF) == -1) {
                return -1;
            }
        }

        return 0;
    }

    // Uncollapsed 0x4C6464.
    public static int fileWriteFixedLengthString(XFile.Handle stream, byte[] data, int offset, int length) {
        return fileWriteUInt8List(stream, data, offset, length);
    }

    // 0x4C6490 db_fwriteShortCount.
    public static int fileWriteInt16List(XFile.Handle stream, short[] arr, int offset, int count) {
        for (int index = 0; index < count; index++) {
            if (fileWriteInt16(stream, arr[offset + index]) == -1) {
                return -1;
            }
        }

        return 0;
    }

    // Uncollapsed 0x4C6490 (unsigned shorts).
    public static int fileWriteUInt16List(XFile.Handle stream, int[] arr, int offset, int count) {
        for (int index = 0; index < count; index++) {
            if (fileWriteInt16(stream, arr[offset + index]) == -1) {
                return -1;
            }
        }

        return 0;
    }

    // 0x4C64F8 db_fwriteIntCount.
    public static int fileWriteInt32List(XFile.Handle stream, int[] arr, int offset, int count) {
        for (int index = 0; index < count; index++) {
            if (dbFwriteLong(stream, arr[offset + index]) == -1) {
                return -1;
            }
        }

        return 0;
    }

    // 0x4C6550 db_fwriteLongCount.
    public static int dbFwriteLongCount(XFile.Handle stream, int[] arr, int offset, int count) {
        for (int index = 0; index < count; index++) {
            int value = arr[offset + index];

            if (fileWriteInt16(stream, (value >> 16) & 0xFFFF) == -1) {
                return -1;
            }

            if (fileWriteInt16(stream, value & 0xFFFF) == -1) {
                return -1;
            }
        }

        return 0;
    }

    // Uncollapsed 0x4C64F8/0x4C6550 (unsigned).
    public static int fileWriteUInt32List(XFile.Handle stream, long[] arr, int offset, int count) {
        int[] ints = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ints[i] = (int) arr[i];
        }
        return fileWriteInt32List(stream, ints, offset, count);
    }

    // 0x4C6628 db_get_file_list. Returns the live name list.
    public static List<String> fileNameListInit(String pattern) {
        FileList fileList = new FileList();

        XFile.XList xlist = new XFile.XList();
        XFile.xlistInit(pattern, xlist);

        if (!xlist.fileNames.isEmpty()) {
            xlist.fileNames.sort(Db::compareNames);

            // Deduplicate case-insensitively, keeping the LATER duplicate
            // (C memmoves it over the earlier one).
            List<String> deduped = new ArrayList<>();
            for (int i = 0; i < xlist.fileNames.size(); i++) {
                if (i + 1 < xlist.fileNames.size()
                        && compareNames(xlist.fileNames.get(i), xlist.fileNames.get(i + 1)) == 0) {
                    continue;
                }
                deduped.add(xlist.fileNames.get(i));
            }

            boolean isWildcard = !pattern.isEmpty() && pattern.charAt(0) == '*';

            for (String name : deduped) {
                String nativeName = PlatformPaths.windowsPathToNative(name);
                PlatformPaths.Split split = PlatformPaths.splitPath(nativeName);

                if (!isWildcard || split.dir().isEmpty()
                        || (split.dir().indexOf('\\') < 0 && split.dir().indexOf('/') < 0)) {
                    fileList.fileNames.add(split.fname() + split.ext());
                }
            }
        }

        fileListHead.add(fileList);

        return fileList.fileNames;
    }

    // 0x4C6868 db_free_file_list.
    public static void fileNameListFree(List<String> fileNames) {
        for (int i = 0; i < fileListHead.size(); i++) {
            if (fileListHead.get(i).fileNames == fileNames) {
                fileListHead.remove(i);
                return;
            }
        }
    }

    // 0x4AA2A4 getFileList (select_file_list.cc): fileNameListInit above
    // (already sorted case-insensitively, deduped and basename-stripped),
    // re-sorted with strcmp byte order. Null on empty, like C's nullptr
    // (NOTE: C leaks the empty FileList node here; Java frees it —
    // unobservable, the head is only searched by pointer on free).
    public static List<String> getFileList(String pattern) {
        List<String> names = fileNameListInit(pattern);
        if (names.isEmpty()) {
            fileNameListFree(names);
            return null;
        }

        names.sort(Db::compareFileNames);

        return names;
    }

    // 0x4AA2DC freeFileList (select_file_list.cc).
    public static void freeFileList(List<String> fileList) {
        if (fileList != null) {
            fileNameListFree(fileList);
        }
    }

    // TODO: Return type should be long.
    //
    // 0x4C68BC db_filelength.
    public static int fileGetSize(XFile.Handle stream) {
        return (int) XFile.xfileGetSize(stream);
    }

    // 0x4C68C4 db_register_callback.
    public static void fileSetReadProgressHandler(Runnable handler, int size) {
        if (handler != null && size != 0) {
            readProgressHandler = handler;
            readProgressChunkSize = size;
        } else {
            readProgressHandler = null;
            readProgressChunkSize = 0;
        }
    }

    // 0x4C68E8 db_list_compare.
    static int compareNames(String a, String b) {
        return DBase.compareKeys(a, b);
    }

    // 0x4AA250 compare (select_file_list.cc): C strcmp byte order
    // (unsigned char). Java compares UTF-16 code units, which is
    // bit-identical for ASCII — the whole engine file-name set
    // (pinned below by CMBATAI2.msg..scrname.msg) is ASCII.
    static int compareFileNames(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            int ca = a.charAt(i);
            int cb = b.charAt(i);
            if (ca != cb) {
                return ca - cb;
            }
        }
        return a.length() - b.length();
    }

    /** Test seam: reset statics (mounts live in {@link XFile}). */
    static void resetForTests() {
        fileListHead.clear();
        readProgressHandler = null;
        readProgressBytesRead = 0;
        readProgressChunkSize = 0;
        SfallFileSystem.sfallFileSystemReset();
        XFile.resetForTests();
        PlatformPaths.clearDirectoryEntryCache();
    }
}
