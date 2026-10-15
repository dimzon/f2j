package org.fallout2ce;

import java.util.Arrays;

/**
 * Port of {@code src/graph_lib.h} / {@code src/graph_lib.cc} (LBM loader,
 * LZSS codec, grayscale palette).
 */
public final class GraphLib {

    private GraphLib() {
    }

    /** Port of {@code GreyTable}. */
    static final byte[] greyTable = new byte[Color.COLOR_COUNT];

    /** Port of {@code HighRGB}. */
    public static int highRGB(int color) {
        int rgb = Color.color2RGB(Byte.toUnsignedInt((byte) color));
        int r = (rgb & 0x7C00) >> 10;
        int g = (rgb & 0x3E0) >> 5;
        int b = rgb & 0x1F;
        return Math.max(r, Math.max(g, b));
    }

    /**
     * Port of {@code load_lbm_to_buf} (PackBits IFF/LBM, clipped, remapped
     * through the loaded color table; returns the image width, -1 on error).
     */
    public static int loadLbmToBuf(String path, byte[] dstBuffer,
            int xMin, int yMin, int xMax, int yMax) {
        XFile.Handle stream = Db.fileOpen(path, "rb");
        if (stream == null) {
            Debug.print("load_lbm_to_buf(%s): fileOpen failed\n", path);
            return -1;
        }

        byte[] form = new byte[4];
        if (Db.fileRead(form, 0, 1, 4, stream) != 4
                || form[0] != 'F' || form[1] != 'O' || form[2] != 'R' || form[3] != 'M') {
            Debug.print("load_lbm_to_buf(%s): incorrect LBM header\n", path);
            Db.fileClose(stream);
            return -1;
        }

        long[] formSize = new long[1];
        byte[] ilbm = new byte[4];
        if (Db.fileReadUInt32(stream, formSize) != 0
                || Db.fileRead(ilbm, 0, 1, 4, stream) != 4) {
            Debug.print("load_lbm_to_buf(%s): incorrect LBM header\n", path);
            Db.fileClose(stream);
            return -1;
        }

        int imgWidth = 0;
        int imgHeight = 0;

        // paletteRemap[i] = system-palette index best matching LBM colour i.
        // Index 0 is populated but never used (0 stays transparent).
        byte[] paletteRemap = new byte[Color.COLOR_COUNT];

        while (true) {
            byte[] chunkType = new byte[4];
            long[] chunkSize = new long[1];
            if (Db.fileRead(chunkType, 0, 1, 4, stream) != 4
                    || Db.fileReadUInt32(stream, chunkSize) != 0) {
                break;
            }

            int aligned = (int) ((chunkSize[0] + 1) & ~1L);

            if (chunkType[0] == 'B' && chunkType[1] == 'M'
                    && chunkType[2] == 'H' && chunkType[3] == 'D') {
                int[] w = new int[1];
                int[] h = new int[1];
                if (Db.fileReadUInt16(stream, w) != 0
                        || Db.fileReadUInt16(stream, h) != 0) {
                    break;
                }
                imgWidth = (short) w[0];
                imgHeight = (short) h[0];

                // Skip every remaining BMHD field (the original never reads them).
                int remaining = aligned - 4;
                if (remaining > 0) {
                    Db.fileSeek(stream, remaining, DFile.SEEK_CUR);
                }
            } else if (isTag(chunkType, 'C', 'M', 'A', 'P')) {
                // The game uses its own palette, reconciled by mapping each
                // LBM color through colorTable (RGB555 to nearest index).
                int entries = (int) Math.min(chunkSize[0] / 3, 256);
                boolean ok = true;
                for (int i = 0; i < entries; i++) {
                    int[] r = new int[1];
                    int[] g = new int[1];
                    int[] b = new int[1];
                    if (Db.fileReadUInt8(stream, r) != 0
                            || Db.fileReadUInt8(stream, g) != 0
                            || Db.fileReadUInt8(stream, b) != 0) {
                        ok = false;
                        break;
                    }
                    int rgb555 = (r[0] >> 3) << 10 | (g[0] >> 3) << 5 | b[0] >> 3;
                    paletteRemap[i] = Color.colorTable[rgb555];
                }
                if (!ok) {
                    Db.fileClose(stream);
                    return -1;
                }
                int remaining = aligned - entries * 3;
                if (remaining > 0) {
                    Db.fileSeek(stream, remaining, DFile.SEEK_CUR);
                }
            } else if (isTag(chunkType, 'B', 'O', 'D', 'Y')) {
                // PackBits RLE always, regardless of the BMHD compression
                // field; remap + clip on the fly, no intermediate buffer.
                // pixel 0 writes 0 (transparent); runs pre-remap once.
                int out = 0;
                int col = 0;
                int row = 0;
                boolean eof = false;
                int[] ctrl = new int[1];
                while (!eof && Db.fileReadUInt8(stream, ctrl) == 0) {
                    int c = ctrl[0];
                    if (c < 0x80) {
                        int count = c + 1;
                        for (int i = 0; i < count && !eof; i++) {
                            int[] px = new int[1];
                            if (Db.fileReadUInt8(stream, px) != 0) {
                                eof = true;
                            } else {
                                byte raw = (byte) px[0];
                                if (col >= xMin && col <= xMax && row >= yMin && row <= yMax) {
                                    dstBuffer[out++] = raw == 0 ? 0 : paletteRemap[raw & 0xFF];
                                }
                                if (++col == imgWidth) {
                                    col = 0;
                                    ++row;
                                }
                            }
                        }
                    } else {
                        int count = 257 - c;
                        int[] px = new int[1];
                        if (Db.fileReadUInt8(stream, px) != 0) {
                            eof = true;
                        } else {
                            byte raw = (byte) px[0];
                            byte mapped = raw == 0 ? 0 : paletteRemap[raw & 0xFF];
                            for (int i = 0; i < count; i++) {
                                if (col >= xMin && col <= xMax && row >= yMin && row <= yMax) {
                                    dstBuffer[out++] = mapped;
                                }
                                if (++col == imgWidth) {
                                    col = 0;
                                    ++row;
                                }
                            }
                        }
                    }
                }

                // Reaching the end of BODY data (normally or via EOF) succeeds.
                Db.fileClose(stream);
                Debug.print("load_lbm_to_buf: loaded %dx%d OK\n", imgWidth, imgHeight);
                return imgWidth;
            } else {
                if (aligned > 0) {
                    Db.fileSeek(stream, aligned, DFile.SEEK_CUR);
                }
            }
        }

        Db.fileClose(stream);
        return -1;
    }

    private static boolean isTag(byte[] chunkType, char a, char b, char c, char d) {
        return chunkType[0] == a && chunkType[1] == b && chunkType[2] == c && chunkType[3] == d;
    }

    /**
     * Port of {@code graphCompress} (LZSS; returns the compressed size, -1
     * when it does not fit). The 1-past-end input reads in C are skipped
     * instead: those bytes are discarded either way.
     */
    public static int graphCompress(byte[] src, byte[] dest, int size) {
        int[] lson = new int[4104];
        int[] rson = new int[4376];
        int[] dad = new int[4104];
        byte[] textBuf = new byte[4122];
        // match = [match_length, match_position].
        int[] match = new int[2];

        initTree(lson, rson, dad);

        Arrays.fill(textBuf, 0, 4078, (byte) ' ');

        int count = 0;
        int v30 = 0;
        for (int index = 4078; index < 4096; index++) {
            // NOTE: C reads one past the end before breaking (the byte is
            // discarded); guarded here instead, same observable state.
            if (v30 >= src.length) {
                break;
            }
            textBuf[index] = src[v30];
            int v8 = v30++;
            if (v8 > size) {
                break;
            }
            count++;
        }

        for (int index = 4077; index > 4059; index--) {
            insertNode(lson, rson, dad, textBuf, match, index);
        }

        insertNode(lson, rson, dad, textBuf, match, 4078);

        byte[] token = new byte[32];
        token[1] = 0;

        int destPos = 0;
        int v3 = 4078;
        int v4 = 0;
        int v10 = 0;
        int v36 = 1;
        int v41 = 1;
        int rc = 0;
        while (count != 0) {
            if (count < match[0]) {
                match[0] = count;
            }

            int v11 = v36 + 1;
            if (match[0] > 2) {
                token[v36 + 1] = (byte) match[1];
                token[v36 + 2] = (byte) (match[0] - 3 | match[1] >> 4 & 0xF0);
                v36 = v11 + 1;
            } else {
                match[0] = 1;
                token[1] |= v41;
                int v13 = v36++;
                token[v13 + 1] = textBuf[v3];
            }

            v41 = v41 * 2 & 0xFF;

            if (v41 == 0) {
                v11 = 0;
                if (v36 != 0) {
                    for (;;) {
                        if (v4 >= size || destPos >= dest.length) {
                            rc = -1;
                            break;
                        }

                        v4++;
                        dest[destPos++] = token[v11 + 1];

                        v11++;
                        if (v11 >= v36) {
                            break;
                        }
                    }

                    if (rc == -1) {
                        break;
                    }
                }

                token[1] = 0;
                v36 = 1;
                v41 = 1;
            }

            int v38 = match[0];
            int v16;
            for (v16 = 0; v16 < v38; v16++) {
                // NOTE: C reads then breaks on v17 >= size (post-increment
                // included); the byte is discarded, so pre-check matches.
                if (v30 >= size || v30 >= src.length) {
                    break;
                }
                byte v34 = src[v30++];

                deleteNode(lson, rson, dad, v10);

                textBuf[v10] = v34;

                if (v10 < 17) {
                    textBuf[v10 + 4096] = v34;
                }

                v3 = v3 + 1 & 0xFFF;
                v10 = (v10 + 1) & 0xFFF;
                insertNode(lson, rson, dad, textBuf, match, v3);
            }

            for (; v16 < v38; v16++) {
                deleteNode(lson, rson, dad, v10);
                v3 = (v3 + 1) & 0xFFF;
                v10 = (v10 + 1) & 0xFFF;
                if (--count != 0) {
                    insertNode(lson, rson, dad, textBuf, match, v3);
                }
            }
        }

        if (rc != -1) {
            for (int v23 = 0; v23 < v36; v23++) {
                if (v4 >= size || destPos >= dest.length) {
                    rc = -1;
                    break;
                }

                v4++;
                v10++;
                dest[destPos++] = token[v23 + 1];
            }
        }

        if (rc == -1) {
            v4 = -1;
        }

        return v4;
    }

    private static void initTree(int[] lson, int[] rson, int[] dad) {
        for (int index = 4097; index < 4353; index++) {
            rson[index] = 4096;
        }

        for (int index = 0; index < 4096; index++) {
            dad[index] = 4096;
        }
    }

    private static void insertNode(int[] lson, int[] rson, int[] dad,
            byte[] textBuf, int[] match, int a1) {
        lson[a1] = 4096;
        rson[a1] = 4096;
        match[0] = 0;

        int v21 = 4097 + Byte.toUnsignedInt(textBuf[a1]);
        int v5 = 1;
        for (;;) {
            int v6 = v21;
            if (v5 < 0) {
                if (lson[v6] == 4096) {
                    lson[v6] = a1;
                    dad[a1] = v21;
                    return;
                }
                v21 = lson[v6];
            } else {
                if (rson[v6] == 4096) {
                    rson[v6] = a1;
                    dad[a1] = v21;
                    return;
                }
                v21 = rson[v6];
            }

            int v9;
            int v10 = a1 + 1;
            int v11 = v21 + 1;
            for (v9 = 1; v9 < 18; v9++) {
                v5 = Byte.toUnsignedInt(textBuf[v10]) - Byte.toUnsignedInt(textBuf[v11]);
                if (v5 != 0) {
                    break;
                }
                v10++;
                v11++;
            }

            if (v9 > match[0]) {
                match[0] = v9;
                match[1] = v21;
                if (v9 >= 18) {
                    break;
                }
            }
        }

        dad[a1] = dad[v21];
        lson[a1] = lson[v21];
        rson[a1] = rson[v21];

        dad[lson[v21]] = a1;
        dad[rson[v21]] = a1;

        if (rson[dad[v21]] == v21) {
            rson[dad[v21]] = a1;
        } else {
            lson[dad[v21]] = a1;
        }

        dad[v21] = 4096;
    }

    private static void deleteNode(int[] lson, int[] rson, int[] dad, int a1) {
        if (dad[a1] != 4096) {
            int v5;
            if (rson[a1] == 4096) {
                v5 = lson[a1];
            } else {
                if (lson[a1] == 4096) {
                    v5 = rson[a1];
                } else {
                    v5 = lson[a1];

                    if (rson[v5] != 4096) {
                        do {
                            v5 = rson[v5];
                        } while (rson[v5] != 4096);

                        rson[dad[v5]] = lson[v5];
                        dad[lson[v5]] = dad[v5];
                        lson[v5] = lson[a1];
                        dad[lson[a1]] = v5;
                    }

                    rson[v5] = rson[a1];
                    dad[rson[a1]] = v5;
                }
            }

            dad[v5] = dad[a1];

            if (rson[dad[a1]] == a1) {
                rson[dad[a1]] = v5;
            } else {
                lson[dad[a1]] = v5;
            }
            dad[a1] = 4096;
        }
    }

    /**
     * Port of {@code graphDecompress} (LZSS; 0 on success, -1 on truncation
     * or empty input).
     */
    public static int graphDecompress(byte[] src, int srcSize, byte[] dest, int destSize) {
        if (src == null || srcSize <= 0 || dest == null || destSize < 0) {
            return -1;
        }

        byte[] textBuf = new byte[4122];

        int v8 = 4078;
        Arrays.fill(textBuf, 0, v8, (byte) ' ');

        int flags = 0;
        int srcIndex = 0;
        int destIndex = 0;
        while (destIndex < destSize) {
            flags >>= 1;
            if ((flags & 0x100) == 0) {
                if (srcIndex >= srcSize) {
                    return -1;
                }

                flags = Byte.toUnsignedInt(src[srcIndex++]);
                flags |= 0xFF00;
            }

            if ((flags & 0x01) == 0) {
                if (srcSize - srcIndex < 2) {
                    return -1;
                }

                int v10 = Byte.toUnsignedInt(src[srcIndex++]);
                int v11 = Byte.toUnsignedInt(src[srcIndex++]);

                v10 |= (v11 & 0xF0) << 4;
                v11 &= 0x0F;
                v11 += 2;

                for (int v16 = 0; v16 <= v11; v16++) {
                    int v17 = v10 + v16 & 0xFFF;

                    byte ch = textBuf[v17];
                    textBuf[v8] = ch;
                    dest[destIndex] = ch;

                    v8 = v8 + 1 & 0xFFF;

                    destIndex++;
                    if (destIndex >= destSize) {
                        break;
                    }
                }
            } else {
                if (srcIndex >= srcSize) {
                    return -1;
                }

                byte ch = src[srcIndex++];
                textBuf[v8] = ch;
                dest[destIndex++] = ch;

                v8 = (v8 + 1) & 0xFFF;
            }
        }

        return 0;
    }

    /** Port of {@code grayscalePaletteUpdate}. */
    public static void grayscalePaletteUpdate(int lowColor, int highColor) {
        for (int index = lowColor; index <= highColor; index++) {
            int indexColor = index & 0xFF;

            int rgb = Color.color2RGB(indexColor);
            int v1 = Math.max((rgb & 0x7C00) >> 10,
                Math.max((rgb & 0x3E0) >> 5, rgb & 0x1F));
            int v2 = Math.min((rgb & 0x7C00) >> 10,
                Math.min((rgb & 0x3E0) >> 5, rgb & 0x1F));
            int v3 = v1 + v2;
            int v4 = (int) (v3 * 240.0 / 510.0);

            int paletteIndex = (v4 & 0xFF) << 10 | (v4 & 0xFF) << 5 | v4 & 0xFF;
            greyTable[index & 0xFF] = Color.colorTable[paletteIndex];
        }
    }

    /** Port of {@code grayscalePaletteApply}. */
    public static void grayscalePaletteApply(byte[] buffer, int width, int height, int pitch) {
        int ptr = 0;
        int skip = pitch - width;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                byte c = buffer[ptr];
                buffer[ptr++] = greyTable[c & 0xFF];
            }
            ptr += skip;
        }
    }
    static void resetForTests() {
        Arrays.fill(greyTable, (byte) 0);
    }
}
