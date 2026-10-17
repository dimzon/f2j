package org.fallout2ce;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Port of the entry-handle side of {@code src/dfile.h} / {@code src/dfile.cc}.
 *
 * <p>Each handle owns a {@link RandomAccessFile} (like C opens its own
 * {@code FILE*} per {@code DFile}). Compressed DAT entries inflate through
 * {@link Inflater} with the zlib header; ZIP entries use raw deflate
 * ({@code inflateInit2(-15)}), selected by {@code format} exactly like
 * {@code dfileDecompressInit}.
 *
 * <p>NOTE: the {@code dfileOpenInternal} reopen branch (reuse a passed-in
 * handle) has no callers in C and is not ported.
 */
public final class DFile {

    private DFile() {
    }

    public static final int DECOMPRESSION_BUFFER_SIZE = 0x1000;

    // DFile flags.
    public static final int HAS_UNGETC = 0x01; // never set (dead in C too)
    public static final int EOF = 0x02;
    public static final int ERROR = 0x04;
    public static final int TEXT = 0x08;
    public static final int HAS_COMPRESSED_UNGETC = 0x10;

    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;

    /** Mirror of {@code struct DFile}. */
    public static final class Handle {
        public DBase.Archive dbase;
        public DBase.Entry entry;
        public int flags;
        public RandomAccessFile stream;
        public Inflater inflater;
        public byte[] decompressionBuffer;
        public int ungotten;
        public int compressedUngotten;
        public int compressedBytesRead;
        public long position;
    }

    // 0x4E5D9C dfile_fopen_helper (fresh-handle path; the reuse branch in C
    // has no callers). Returns null on any failure, like C.
    private static Handle dfileOpenInternal(DBase.Archive dbase, String filePath, String mode) {
        DBase.Entry entry = DBase.bsearchEntry(dbase.entries, filePath);
        if (entry == null) {
            return null;
        }

        if (mode.isEmpty() || mode.charAt(0) != 'r') {
            return null;
        }

        Handle handle = new Handle();
        handle.dbase = dbase;

        handle.entry = entry;

        try {
            handle.stream = new RandomAccessFile(new File(dbase.path), "r");
        } catch (IOException e) {
            return null;
        }

        try {
            handle.stream.seek((long) dbase.dataOffset + entry.dataOffset);
        } catch (IOException e) {
            closeQuietly(handle);
            return null;
        }

        if (entry.compressed) {
            handle.inflater = new Inflater(dbase.format == DBase.Format.ZIP);
            handle.decompressionBuffer = new byte[DECOMPRESSION_BUFFER_SIZE];
        }

        if (mode.length() > 1 && mode.charAt(1) == 't') {
            handle.flags |= TEXT;
        }

        dbase.openHandles.add(handle);
        return handle;
    }

    // 0x4E5504 dfile_fopen.
    public static Handle dfileOpen(DBase.Archive dbase, String filePath, String mode) {
        if (dbase == null || filePath == null || mode == null) {
            return null;
        }
        return dfileOpenInternal(dbase, filePath, mode);
    }

    // 0x4E5424 dfile_filelength.
    public static long dfileGetSize(Handle stream) {
        return stream.entry.uncompressedSize;
    }

    // 0x4E542C dfile_fclose.
    public static int dfileClose(Handle stream) {
        if (stream.inflater != null) {
            stream.inflater.end();
            stream.inflater = null;
        }
        stream.decompressionBuffer = null;
        closeQuietly(stream);

        stream.dbase.openHandles.remove(stream);
        stream.dbase = null;
        stream.entry = null;
        stream.stream = null;
        stream.flags = 0;
        stream.position = 0;
        return 0;
    }

    private static void closeQuietly(Handle handle) {
        if (handle.stream != null) {
            try {
                handle.stream.close();
            } catch (IOException e) {
                // Ignore, like C ignoring fclose errors here.
            }
            handle.stream = null;
        }
    }

    // 0x4E56C0 dfile_vfprintf (not implemented in C either).
    public static int dfilePrintFormattedArgs(Handle stream, String format, Object... args) {
        return -1;
    }

    // 0x4E5700 dfile_fgetc (\r\n reported as \n).
    public static int dfileReadChar(Handle stream) {
        if ((stream.flags & (EOF | ERROR)) != 0) {
            return -1;
        }

        if ((stream.flags & HAS_UNGETC) != 0) {
            stream.flags &= ~HAS_UNGETC;
            return stream.ungotten;
        }

        int ch = dfileReadCharInternal(stream);
        if (ch == -1) {
            stream.flags |= EOF;
        }

        return ch;
    }

    // 0x4E5764 dfile_fgets (both \r\n and \n recognized, reported as \n).
    // Returns null when nothing was read, like C.
    public static String dfileReadString(Handle stream, int size) {
        if ((stream.flags & (EOF | ERROR)) != 0) {
            return null;
        }

        StringBuilder out = new StringBuilder();

        int remaining = size;
        if ((stream.flags & HAS_UNGETC) != 0) {
            out.append((char) (stream.ungotten & 0xFF));
            remaining--;
            stream.flags &= ~HAS_UNGETC;
        }

        for (int index = 0; index < remaining - 1; index++) {
            int ch = dfileReadCharInternal(stream);
            if (ch == -1) {
                break;
            }

            out.append((char) (ch & 0xFF));

            if (ch == '\n') {
                break;
            }
        }

        if (out.length() == 0) {
            return null;
        }

        return out.toString();
    }

    // 0x4E5830 dfile_fputc (not implemented in C either).
    public static int dfileWriteChar(int ch, Handle stream) {
        return -1;
    }

    // 0x4E5854 dfile_fputs (not implemented in C either).
    public static int dfileWriteString(String string, Handle stream) {
        return -1;
    }

    // 0x4E58FC dfile_fread. Returns full items read.
    public static long dfileRead(byte[] out, int offset, int size, int count, Handle stream) {
        if ((stream.flags & (EOF | ERROR)) != 0) {
            return 0;
        }

        long remainingSize = (long) stream.entry.uncompressedSize - stream.position;
        if ((stream.flags & HAS_UNGETC) != 0) {
            remainingSize++;
        }

        long bytesToRead = (long) size * count;
        if (remainingSize < bytesToRead) {
            bytesToRead = remainingSize;
            stream.flags |= EOF;
        }

        long extraBytesRead = 0;
        int pos = offset;
        if ((stream.flags & HAS_UNGETC) != 0) {
            out[pos++] = (byte) (stream.ungotten & 0xFF);
            bytesToRead--;
            stream.flags &= ~HAS_UNGETC;
            extraBytesRead = 1;
        }

        long bytesRead;
        if (stream.entry.compressed) {
            if (!dfileReadCompressed(stream, out, pos, bytesToRead)) {
                stream.flags |= ERROR;
                return 0;
            }
            bytesRead = bytesToRead;
        } else {
            try {
                bytesRead = readUpTo(stream.stream, out, pos, bytesToRead) + extraBytesRead;
            } catch (IOException e) {
                stream.flags |= ERROR;
                return 0;
            }
            stream.position += bytesRead;
        }

        return size == 0 ? 0 : bytesRead / size;
    }

    // 0x4E59F8 dfile_fwrite (not implemented in C either: returns count - 1).
    public static long dfileWrite(byte[] data, int offset, int size, int count, Handle stream) {
        return (long) count - 1;
    }

    // 0x4E5A74 dfile_fseek. Returns 0 or 1, like C.
    public static int dfileSeek(Handle stream, long offset, int origin) {
        if ((stream.flags & ERROR) != 0) {
            return 1;
        }

        if ((stream.flags & TEXT) != 0) {
            // NOTE: text streams only support rewinding (see C NOTE).
            if (offset != 0 && origin != SEEK_SET) {
                return 1;
            }
        }

        long offsetFromBeginning;
        switch (origin) {
        case SEEK_SET:
            offsetFromBeginning = offset;
            break;
        case SEEK_CUR:
            offsetFromBeginning = stream.position + offset;
            break;
        case SEEK_END:
            offsetFromBeginning = (long) stream.entry.uncompressedSize + offset;
            break;
        default:
            return 1;
        }

        if (offsetFromBeginning >= stream.entry.uncompressedSize) {
            return 1;
        }

        long pos = stream.position;
        if (offsetFromBeginning == pos) {
            stream.flags &= ~(HAS_UNGETC | EOF);
            return 0;
        }

        if (offsetFromBeginning != 0) {
            if (stream.entry.compressed) {
                if (offsetFromBeginning < pos) {
                    dfileRewind(stream);
                    if ((stream.flags & ERROR) != 0) {
                        return 1;
                    }
                }

                while (offsetFromBeginning > stream.position) {
                    if (dfileReadCharInternal(stream) == -1) {
                        return 1;
                    }
                }
            } else {
                try {
                    stream.stream.seek(stream.stream.getFilePointer() + (offsetFromBeginning - pos));
                } catch (IOException e) {
                    stream.flags |= ERROR;
                    return 1;
                }

                // NOTE: C does NOT update position here (only the
                // compressedBytesRead field, an upstream quirk). The tracked
                // position goes stale; mirrored exactly.
                stream.compressedBytesRead = (int) offsetFromBeginning;
            }

            stream.flags &= ~(HAS_UNGETC | EOF);
            return 0;
        }

        try {
            stream.stream.seek((long) stream.dbase.dataOffset + stream.entry.dataOffset);
        } catch (IOException e) {
            stream.flags |= ERROR;
            return 1;
        }

        if (stream.entry.compressed) {
            stream.inflater = new Inflater(stream.dbase.format == DBase.Format.ZIP);
        }
        stream.position = 0;
        stream.compressedBytesRead = 0;
        stream.flags &= ~(HAS_UNGETC | EOF);

        return 0;
    }

    // 0x4E5C88 dfile_ftell.
    public static long dfileTell(Handle stream) {
        return stream.position;
    }

    // 0x4E5CB0 dfile_rewind (also clears ERROR).
    public static void dfileRewind(Handle stream) {
        dfileSeek(stream, 0, SEEK_SET);
        stream.flags &= ~ERROR;
    }

    // 0x4E5D10 dfile_feof.
    public static int dfileEof(Handle stream) {
        return stream.flags & EOF;
    }

    // 0x4E5F9C dfile_fgetc_helper.
    static int dfileReadCharInternal(Handle stream) {
        if (stream.entry.compressed) {
            byte[] ch = new byte[1];
            if (!dfileReadCompressed(stream, ch, 0, 1)) {
                return -1;
            }

            if ((stream.flags & TEXT) != 0) {
                if (ch[0] == '\r') {
                    byte[] nextCh = new byte[1];
                    if (dfileReadCompressed(stream, nextCh, 0, 1)) {
                        if (nextCh[0] == '\n') {
                            ch[0] = nextCh[0];
                        } else {
                            dfileUngetCompressed(stream, nextCh[0] & 0xFF);
                        }
                    }
                }
            }

            return ch[0] & 0xFF;
        }

        if (stream.position >= stream.entry.uncompressedSize) {
            return -1;
        }

        int ch;
        try {
            ch = stream.stream.read();
        } catch (IOException e) {
            return -1;
        }
        if (ch != -1) {
            if ((stream.flags & TEXT) != 0) {
                if (ch == '\r') {
                    if (stream.position + 1 < stream.entry.uncompressedSize) {
                        int nextCh;
                        try {
                            nextCh = stream.stream.read();
                        } catch (IOException e) {
                            return -1;
                        }
                        if (nextCh == '\n') {
                            ch = nextCh;
                            stream.position++;
                        } else if (nextCh != -1) {
                            try {
                                stream.stream.seek(stream.stream.getFilePointer() - 1);
                            } catch (IOException e) {
                                return -1;
                            }
                        }
                    }
                }
            }

            stream.position++;
        }

        return ch;
    }

    // 0x4E6078 dfile_read_comp_bytes.
    static boolean dfileReadCompressed(Handle stream, byte[] out, int offset, long size) {
        int pos = offset;
        long remaining = size;

        if ((stream.flags & HAS_COMPRESSED_UNGETC) != 0) {
            out[pos++] = (byte) (stream.compressedUngotten & 0xFF);
            remaining--;
            stream.flags &= ~HAS_COMPRESSED_UNGETC;
            stream.position++;

            if (remaining == 0) {
                return true;
            }
        }

        long outEnd = pos + remaining;
        while (pos < outEnd) {
            if (stream.inflater.needsInput()) {
                long bytesToRead = Math.min(DECOMPRESSION_BUFFER_SIZE,
                        (long) stream.entry.dataSize - stream.compressedBytesRead);
                if (bytesToRead <= 0) {
                    return false;
                }
                try {
                    stream.stream.readFully(stream.decompressionBuffer, 0, (int) bytesToRead);
                } catch (IOException e) {
                    return false;
                }
                stream.inflater.setInput(stream.decompressionBuffer, 0, (int) bytesToRead);
                stream.compressedBytesRead += bytesToRead;
            }

            int n;
            try {
                n = stream.inflater.inflate(out, pos, (int) (outEnd - pos));
            } catch (DataFormatException e) {
                return false;
            }
            if (n == 0) {
                // inflate() made no progress with output space left:
                // truncated or corrupt stream (C: avail_out != 0 -> false).
                // needsInput is handled by the loop top; anything else fails.
                if (stream.inflater.needsInput()) {
                    continue;
                }
                return false;
            }
            pos += n;
        }

        stream.position += size;
        return true;
    }

    // 0x4E613C dfile_zungetc.
    static void dfileUngetCompressed(Handle stream, int ch) {
        stream.compressedUngotten = ch;
        stream.flags |= HAS_COMPRESSED_UNGETC;
        stream.position--;
    }

    private static long readUpTo(RandomAccessFile raf, byte[] out, int offset, long length) throws IOException {
        long total = 0;
        while (total < length) {
            int n = raf.read(out, (int) (offset + total), (int) Math.min(length - total, Integer.MAX_VALUE));
            if (n < 0) {
                break;
            }
            total += n;
        }
        return total;
    }
}
