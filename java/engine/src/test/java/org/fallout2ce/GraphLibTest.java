package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Vectors from {@code docker/refharness/golden/v26.txt} ({@code [graph]}):
 * LBM decode (committed fixture), LZSS round-trip, grayscale.
 */
class GraphLibTest {

    @AfterEach
    void reset() {
        GraphLib.resetForTests();
        Color.resetForTests();
        Db.resetForTests();
    }

    static long crc32(byte[] bytes, int length) {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, length);
        return crc.getValue();
    }

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        Path p = Path.of(root, name);
        assumeTrue(Files.isRegularFile(p), "missing " + p);
        return p.toString();
    }

    static String fixture(String name) throws Exception {
        Path tmp = Files.createTempDirectory("f2j-lbm");
        try (java.io.InputStream in = GraphLibTest.class.getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(in, name);
            Files.copy(in, tmp.resolve(name));
        }
        return tmp.resolve(name).toString();
    }

    static void mountAndLoadPalette() {
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(Color.colorPaletteLoad("color.pal"));
    }

    @Test
    void highRGB() {
        mountAndLoadPalette();
        assertEquals(31, GraphLib.highRGB(Color.white()));
        assertEquals(0, GraphLib.highRGB(Color.black()));
    }

    @Test
    void loadLbm() throws Exception {
        mountAndLoadPalette();
        String lbm = fixture("test.lbm");

        byte[] pixels = new byte[64];
        Arrays.fill(pixels, (byte) 0xAA);
        assertEquals(6, GraphLib.loadLbmToBuf(lbm, pixels, 0, 0, 5, 3));
        assertEquals(0x52FF2860L, crc32(pixels, 6 * 4));
    }

    @Test
    void loadLbmClip() throws Exception {
        mountAndLoadPalette();
        String lbm = fixture("test.lbm");

        byte[] clip = new byte[16];
        Arrays.fill(clip, (byte) 0xAA);
        assertEquals(6, GraphLib.loadLbmToBuf(lbm, clip, 1, 1, 4, 2));
        assertEquals(0x9AC24FE6L, crc32(clip, 16));
    }

    @Test
    void loadLbmMissing() {
        assertEquals(-1, GraphLib.loadLbmToBuf("/tmp/nope.lbm", new byte[8], 0, 0, 1, 1));
    }

    @Test
    void compressDecompress() {
        byte[] plain = new byte[64];
        for (int i = 0; i < 64; i++) {
            plain[i] = (byte) (i / 8);
        }

        byte[] packed = new byte[128];
        int packedSize = GraphLib.graphCompress(plain, packed, 64);
        assertEquals(27, packedSize);
        assertEquals(0x798E8D45L, crc32(packed, packedSize));

        byte[] unpacked = new byte[64];
        Arrays.fill(unpacked, (byte) 0xAA);
        assertEquals(0, GraphLib.graphDecompress(packed, packedSize, unpacked, 64));
        assertArrayEquals(plain, unpacked);

        assertEquals(-1, GraphLib.graphDecompress(new byte[] { 1, 2, 3, 4 }, 4, new byte[64], 64));
        assertEquals(-1, GraphLib.graphDecompress(null, 0, new byte[4], 4));
    }

    @Test
    void grayscale() {
        mountAndLoadPalette();

        byte[] gray = new byte[16];
        for (int i = 0; i < 16; i++) {
            gray[i] = (byte) (i * 16);
        }
        GraphLib.grayscalePaletteUpdate(0, 255);
        GraphLib.grayscalePaletteApply(gray, 4, 4, 4);
        assertEquals(0x1AD0173BL, crc32(gray, 16));

        byte[] pitched = new byte[24];
        Arrays.fill(pitched, (byte) 0);
        for (int i = 0; i < 16; i++) {
            pitched[i] = (byte) (i * 16);
        }
        GraphLib.grayscalePaletteApply(pitched, 4, 4, 6);
        assertEquals(0x38D4751FL, crc32(pitched, 24));
    }
}
