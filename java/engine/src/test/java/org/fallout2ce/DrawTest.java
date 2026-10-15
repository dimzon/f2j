package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Every vector comes from {@code docker/refharness/golden/v17.txt}
 * ({@code [draw]}), reproduced here with identical inputs.
 */
class DrawTest {

    @AfterEach
    void reset() {
        Color.resetForTests();
        Db.resetForTests();
    }

    static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    @Test
    void lines() {
        byte[] line = new byte[64 * 64];

        Arrays.fill(line, (byte) 0);
        Draw.bufferDrawLine(line, 64, 2, 3, 10, 3, 7);
        assertEquals(0xCC5680C1L, crc32(line));

        Arrays.fill(line, (byte) 0);
        Draw.bufferDrawLine(line, 64, 10, 3, 2, 3, 7);
        assertEquals(0xCC5680C1L, crc32(line));

        Arrays.fill(line, (byte) 0);
        Draw.bufferDrawLine(line, 64, 4, 1, 4, 8, 9);
        assertEquals(0x2A07DB3EL, crc32(line));

        Arrays.fill(line, (byte) 0);
        Draw.bufferDrawLine(line, 64, 4, 8, 4, 1, 9);
        assertEquals(0x2A07DB3EL, crc32(line));

        Arrays.fill(line, (byte) 0);
        Draw.bufferDrawLine(line, 64, 5, 5, 5, 5, 3);
        assertEquals(0x93A0D7BFL, crc32(line));

        Arrays.fill(line, (byte) 0);
        Draw.bufferDrawLine(line, 64, 1, 1, 3, 9, 5);
        assertEquals(0x5124962CL, crc32(line));

        Arrays.fill(line, (byte) 0);
        Draw.bufferDrawLine(line, 64, 1, 6, 12, 2, 6);
        assertEquals(0xA7641F4CL, crc32(line));

        Arrays.fill(line, (byte) 0);
        Draw.bufferDrawLine(line, 64, 12, 2, 1, 6, 6);
        assertEquals(0xA7641F4CL, crc32(line));
    }

    @Test
    void rects() {
        byte[] rect = new byte[16 * 16];

        Arrays.fill(rect, (byte) 0);
        Draw.bufferDrawRect(rect, 16, 2, 2, 9, 7, 7);
        assertEquals(0x4382F7AEL, crc32(rect));

        Arrays.fill(rect, (byte) 0);
        Draw.bufferDrawRectShadowed(rect, 16, 2, 2, 9, 7, 1, 2);
        assertEquals(0x28592A32L, crc32(rect));
    }

    static byte[] blitSrc() {
        byte[] src = new byte[8 * 10];
        for (int i = 0; i < src.length; i++) {
            src[i] = (byte) (i / 10 * 8 + i % 10 + 1);
        }
        return src;
    }

    static byte[] transSrc() {
        byte[] src = new byte[8 * 10];
        for (int i = 0; i < src.length; i++) {
            src[i] = (byte) (((i / 10 + i % 10) % 3 == 0) ? 0 : (i & 0xFF) + 1);
        }
        return src;
    }

    @Test
    void blits() {
        byte[] dst = new byte[12 * 12];

        Arrays.fill(dst, (byte) 0);
        Draw.blitBufferToBuffer(blitSrc(), 8, 8, 10, dst, 12);
        assertEquals(0x2F456926L, crc32(dst));

        Arrays.fill(dst, (byte) 0);
        Draw.blitBufferToBufferTrans(transSrc(), 8, 8, 10, dst, 12);
        assertEquals(0x88D6F72EL, crc32(dst));
    }

    @Test
    void blit2D() {
        byte[] s = new byte[36];
        for (int i = 0; i < s.length; i++) {
            s[i] = (byte) (i + 1);
        }
        Draw.Buffer src = new Draw.Buffer(s, 6, 6);
        byte[] d = new byte[100];
        Draw.Buffer dst = new Draw.Buffer(d, 10, 10);

        Arrays.fill(d, (byte) 0);
        Draw.blitBuffer2D(src, dst, -2, 3);
        assertEquals(0xD3179771L, crc32(d));

        Arrays.fill(d, (byte) 0);
        Draw.blitBuffer2D(src, 1, 1, 3, 3, dst, 4, 4);
        assertEquals(0x21E12991L, crc32(d));

        Arrays.fill(d, (byte) 7);
        Draw.blitBuffer2D(src, dst, 20, 20);
        assertEquals(0x2FCB0660L, crc32(d));
    }

    @Test
    void scaled() {
        byte[] s = new byte[16];
        for (int i = 0; i < s.length; i++) {
            s[i] = (byte) (i + 1);
        }
        Draw.Buffer src = new Draw.Buffer(s, 4, 4);
        byte[] d = new byte[64];
        Draw.Buffer dst = new Draw.Buffer(d, 8, 8);

        Arrays.fill(d, (byte) 0);
        Draw.blitBuffer2DScaled(src, dst, 0, 0, 8, 8);
        assertEquals(0xE0C665BAL, crc32(d));

        Arrays.fill(d, (byte) 0);
        Draw.blitBuffer2DScaled(src, 1, 1, 2, 2, dst, 1, 1, 4, 4);
        assertEquals(0xEBE2D644L, crc32(d));

        byte[] t = new byte[16];
        for (int i = 0; i < t.length; i++) {
            t[i] = (byte) ((i % 5 == 0) ? 0 : i + 1);
        }
        Draw.Buffer tsrc = new Draw.Buffer(t, 4, 4);
        Arrays.fill(d, (byte) 9);
        Draw.blitBuffer2DScaledTrans(tsrc, dst, 0, 0, 8, 8);
        assertEquals(0xACD7F0A3L, crc32(d));
    }

    @Test
    void aspectFit() {
        byte[] s = new byte[8];
        for (int i = 0; i < s.length; i++) {
            s[i] = (byte) (i + 1);
        }
        Draw.Buffer src = new Draw.Buffer(s, 4, 2);
        byte[] d = new byte[64];
        Draw.Buffer dst = new Draw.Buffer(d, 8, 8);

        Arrays.fill(d, (byte) 0);
        Geometry.Rect up = Draw.blitBuffer2DCenteredAspectFit(src, dst, 9, true);
        assertEquals(0xD518BCFEL, crc32(d));
        assertEquals(0, up.left);
        assertEquals(2, up.top);
        assertEquals(7, up.right);
        assertEquals(5, up.bottom);

        Arrays.fill(d, (byte) 0);
        Geometry.Rect flat = Draw.blitBuffer2DCenteredAspectFit(src, dst, 9, false);
        assertEquals(0x81281A9EL, crc32(d));
        assertEquals(2, flat.left);
        assertEquals(3, flat.top);
        assertEquals(5, flat.right);
        assertEquals(4, flat.bottom);

        byte[] big = new byte[64];
        for (int i = 0; i < big.length; i++) {
            big[i] = (byte) (i + 1);
        }
        byte[] small = new byte[16];
        Geometry.Rect down = Draw.blitBuffer2DCenteredAspectFit(
            new Draw.Buffer(big, 8, 8), new Draw.Buffer(small, 4, 4), 9, false);
        assertEquals(0x98CE3B29L, crc32(small));
        assertEquals(0, down.left);
        assertEquals(0, down.top);
        assertEquals(3, down.right);
        assertEquals(3, down.bottom);
    }

    @Test
    void fills() {
        byte[] small = new byte[35];
        Draw.bufferFill2D(new Draw.Buffer(small, 5, 7), 3);
        assertEquals(0x39F675C2L, crc32(small));

        // Null buffer is a no-op.
        Draw.bufferFill2D(new Draw.Buffer(null, 5, 7), 3);

        byte[] pitched = new byte[48];
        Arrays.fill(pitched, (byte) 0);
        Draw.bufferFill(pitched, 6, 5, 8, 4);
        assertEquals(0x594B1DE6L, crc32(pitched));

        byte[] tex = new byte[32];
        for (int i = 0; i < tex.length; i++) {
            tex[i] = (byte) (i + 1);
        }
        Draw.bufTexture(tex, 8, 4, 8, null, 0, 0);
        assertEquals(0x87E6EC25L, crc32(tex));
    }

    @Test
    void lightenSwapOutlineCopies() {
        // Intensity tables need game data (mirrors the harness, which runs
        // [draw] after [color]).
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        java.nio.file.Path master = java.nio.file.Path.of(root, "master.dat");
        assumeTrue(java.nio.file.Files.isRegularFile(master), "missing " + master);
        assertTrue(XFile.xbaseOpen(master.toString()));
        assertTrue(Color.colorPaletteLoad("color.pal"));

        byte[] li = new byte[16];
        for (int i = 0; i < li.length; i++) {
            li[i] = (byte) (i * 16);
        }
        Draw.lightenBuf(li, 4, 4, 4);
        assertEquals(0xE3632303L, crc32(li));

        byte[] sw = new byte[16];
        for (int i = 0; i < sw.length; i++) {
            sw[i] = (byte) ((i % 4 == 0) ? 3 : ((i % 4 == 1) ? 7 : 5));
        }
        Draw.swapColorBuf(sw, 4, 4, 4, 3, 7);
        assertEquals(0xE9FB8ECFL, crc32(sw));

        byte[] ol = new byte[64];
        Arrays.fill(ol, (byte) 0);
        for (int y = 2; y < 6; y++) {
            for (int x = 2; x < 6; x++) {
                ol[y * 8 + x] = 7;
            }
        }
        Draw.bufferOutline(ol, 8, 8, 8, 9);
        assertEquals(0xDA88249AL, crc32(ol));

        byte[] dst = new byte[20];
        Arrays.fill(dst, (byte) 0);
        Draw.srcCopy(dst, 5, blitSrc(), 10, 4, 3);
        assertEquals(0xEAF16D75L, crc32(dst));

        Arrays.fill(dst, (byte) 0);
        Draw.transSrcCopy(dst, 5, transSrc(), 10, 4, 3);
        assertEquals(0x6F1F04E4L, crc32(dst));
    }
}
