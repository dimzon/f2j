package org.fallout2ce;

/**
 * Port of {@code src/pcx.h} / {@code src/pcx.cc} (8-bit PCX decoder).
 *
 * <p>Header shorts are little-endian raw reads (x86 dumps, like
 * {@code .fon} — not the big-endian {@code Db} helpers). {@code -1} from
 * {@code fileReadChar} is normalized with {@code & 0xFF}, exactly like the
 * C assignment into {@code unsigned char}.
 */
public final class Pcx {

    private Pcx() {
    }

    /** Port of {@code PcxHeader}. */
    public static final class Header {
        public int identifier;
        public int version;
        public int encoding;
        public int bitsPerPixel;
        public short minX;
        public short minY;
        public short maxX;
        public short maxY;
        public short horizontalResolution;
        public short verticalResolution;
        public final byte[] palette = new byte[48];
        public int reserved1;
        public int planeCount;
        public short bytesPerLine;
        public short paletteType;
        public short horizontalScreenSize;
        public short verticalScreenSize;
        public final byte[] reserved2 = new byte[54];
    }

    /** Port of {@code gPcxLastRunLength} / {@code gPcxLastValue}. */
    static int lastRunLength;
    static int lastValue;

    /** Port of {@code pcxReadHeader} (no error checks, as in C). */
    public static void pcxReadHeader(Header header, XFile.Handle stream) {
        header.identifier = Db.fileReadChar(stream) & 0xFF;
        header.version = Db.fileReadChar(stream) & 0xFF;
        header.encoding = Db.fileReadChar(stream) & 0xFF;
        header.bitsPerPixel = Db.fileReadChar(stream) & 0xFF;

        header.minX = readInt16LE(stream);
        header.minY = readInt16LE(stream);
        header.maxX = readInt16LE(stream);
        header.maxY = readInt16LE(stream);
        header.horizontalResolution = readInt16LE(stream);
        header.verticalResolution = readInt16LE(stream);

        for (int index = 0; index < 48; index++) {
            header.palette[index] = (byte) Db.fileReadChar(stream);
        }

        header.reserved1 = Db.fileReadChar(stream) & 0xFF;
        header.planeCount = Db.fileReadChar(stream) & 0xFF;

        header.bytesPerLine = readInt16LE(stream);
        header.paletteType = readInt16LE(stream);
        header.horizontalScreenSize = readInt16LE(stream);
        header.verticalScreenSize = readInt16LE(stream);

        for (int index = 0; index < 54; index++) {
            header.reserved2[index] = (byte) Db.fileReadChar(stream);
        }
    }

    private static short readInt16LE(XFile.Handle stream) {
        int low = Db.fileReadChar(stream) & 0xFF;
        int high = Db.fileReadChar(stream) & 0xFF;
        return (short) (low | high << 8);
    }

    /** Port of {@code pcxReadLine}. */
    public static int pcxReadLine(byte[] data, int offset, int size, XFile.Handle stream) {
        int runLength = lastRunLength;
        int value = lastValue;

        int uncompressedSize = 0;
        int index = 0;
        do {
            uncompressedSize += runLength;
            while (runLength > 0 && index < size) {
                data[offset + index] = (byte) value;
                runLength--;
                index++;
            }

            lastRunLength = runLength;
            lastValue = value;

            if (runLength != 0) {
                uncompressedSize -= runLength;
                break;
            }

            value = Db.fileReadChar(stream) & 0xFF;
            if ((value & 0xC0) == 0xC0) {
                lastRunLength = value & 0x3F;
                value = Db.fileReadChar(stream) & 0xFF;
                runLength = lastRunLength;
            } else {
                runLength = 1;
            }
        } while (index < size);

        lastRunLength = runLength;
        lastValue = value;

        return uncompressedSize;
    }

    /** Port of {@code pcxReadPalette}. */
    public static int pcxReadPalette(Header header, byte[] palette, XFile.Handle stream) {
        if (header.version != 5) {
            return 0;
        }

        long pos = Db.fileTell(stream);
        long size = Db.fileGetSize(stream);
        Db.fileSeek(stream, size - 769, DFile.SEEK_SET);
        if ((Db.fileReadChar(stream) & 0xFF) != 12) {
            Db.fileSeek(stream, pos, DFile.SEEK_SET);
            return 0;
        }

        byte[] pal = new byte[768];
        if (Db.fileRead(pal, 0, 768, 1, stream) != 1) {
            Db.fileSeek(stream, pos, DFile.SEEK_SET);
            return 0;
        }
        System.arraycopy(pal, 0, palette, 0, 768);

        Db.fileSeek(stream, pos, DFile.SEEK_SET);

        return 1;
    }

    /**
     * Port of {@code pcxRead}. Width/height come back through the holders;
     * {@code null} when the file is missing. Row stride is the image width
     * (replicating the C overlap quirk when it is narrower than the padded
     * line length).
     */
    public static byte[] pcxRead(String path, int[] widthPtr, int[] heightPtr, byte[] palette) {
        XFile.Handle stream = Db.fileOpen(path, "rb");
        if (stream == null) {
            return null;
        }

        Header header = new Header();
        pcxReadHeader(header, stream);

        int width = header.maxX - header.minX + 1;
        int height = header.maxY - header.minY + 1;

        widthPtr[0] = width;
        heightPtr[0] = height;

        int bytesPerLine = header.planeCount * header.bytesPerLine;
        byte[] data = new byte[bytesPerLine * height];

        lastRunLength = 0;
        lastValue = 0;

        for (int y = 0; y < height; y++) {
            pcxReadLine(data, y * width, bytesPerLine, stream);
        }

        pcxReadPalette(header, palette, stream);

        Db.fileClose(stream);

        return data;
    }

    /** Test-only reset (RLE carry state). */
    static void resetForTests() {
        lastRunLength = 0;
        lastValue = 0;
    }
}
