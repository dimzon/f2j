package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Vectors from {@code docker/refharness/golden/v21.txt} ({@code [datafile]}),
 * reading the same committed fixtures
 * ({@code src/test/resources/fixtures/*.pcx}).
 */
class DatafileTest {

    @AfterEach
    void reset() {
        Datafile.resetForTests();
        Pcx.resetForTests();
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
        Path tmp = Files.createTempDirectory("f2j-pcx");
        try (java.io.InputStream in = DatafileTest.class.getResourceAsStream("/fixtures/" + name)) {
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
    void pcxRead() throws Exception {
        String pcx = fixture("test.pcx");
        byte[] pal = new byte[768];
        Arrays.fill(pal, (byte) 0xAA);
        int[] w = new int[1];
        int[] h = new int[1];

        byte[] img = Pcx.pcxRead(pcx, w, h, pal);

        assertNotNull(img);
        assertEquals(6, w[0]);
        assertEquals(4, h[0]);
        assertEquals(0x0779E7D2L, crc32(img, w[0] * h[0]));
        assertEquals(0x8DE4C36EL, crc32(pal, 768));
    }

    @Test
    void pcxReadOddStride() throws Exception {
        String pcx = fixture("test-odd.pcx");
        byte[] pal = new byte[768];
        int[] w = new int[1];
        int[] h = new int[1];

        byte[] img = Pcx.pcxRead(pcx, w, h, pal);

        assertNotNull(img);
        assertEquals(5, w[0]);
        assertEquals(3, h[0]);
        // Row stride is the image width (C overlap quirk included).
        assertEquals(0xE05588ADL, crc32(img, w[0] * h[0]));
    }

    @Test
    void pcxReadMissing() {
        int[] w = new int[] { -1 };
        int[] h = new int[] { -1 };
        assertNull(Pcx.pcxRead("/tmp/nope.pcx", w, h, new byte[768]));
        assertEquals(-1, w[0]);
        assertEquals(-1, h[0]);
    }

    @Test
    void datafileReadRemaps() throws Exception {
        mountAndLoadPalette();
        String pcx = fixture("test.pcx");

        Datafile.Image image = Datafile.datafileRead(pcx);

        assertNotNull(image);
        assertEquals(6, image.width());
        assertEquals(4, image.height());
        assertEquals(0x9B14C0FBL, crc32(image.data(), image.width() * image.height()));
    }

    @Test
    void remapRgb6() {
        mountAndLoadPalette();
        byte[] data = new byte[] { 0, 1, 2, 3 };
        byte[] pal = new byte[768];
        for (int i = 0; i < 768; i++) {
            pal[i] = (byte) ((i * 5 + 1) & 0x3F);
        }
        Datafile.datafileRemapPixelsRgb6(data, pal, 2, 2);
        assertEquals(0x00, data[0] & 0xFF);
        assertEquals(0x71, data[1] & 0xFF);
        assertEquals(0x66, data[2] & 0xFF);
        assertEquals(0x22, data[3] & 0xFF);
    }

    @Test
    void packUntilZero() {
        byte[] data = new byte[] { 1, 2, 0, 4, 5, 6, 0, 0, 0, 0, 0, 0 };
        int[] dims = Datafile.datafilePackUntilZero(data, 4, 3);
        assertEquals(2, dims[0]);
        assertEquals(2, dims[1]);
        assertEquals(0x0E683D67L, crc32(data, dims[0] * dims[1]));

        int[] degenerate = Datafile.datafilePackUntilZero(data, 5, 0);
        assertEquals(0, degenerate[0]);
        assertEquals(0, degenerate[1]);
    }

    @Test
    void datafileLoadBlock() throws Exception {
        String zip = fixture("f2test.zip");
        byte[] blob = Datafile.datafileLoad(zip);
        assertNotNull(blob);
        assertEquals(325, blob.length);
        assertEquals(0x56C3CC27L, crc32(blob, blob.length));
        assertNull(Datafile.datafileLoad("/tmp/nope.pcx"));
    }

    @Test
    void palettes() throws Exception {
        mountAndLoadPalette();
        String pcx = fixture("test.pcx");

        byte[] loaded = Datafile.datafileLoadPalette(pcx);
        assertNotNull(loaded);
        assertEquals(0x8DE4C36EL, crc32(loaded, 768));
        assertNull(Datafile.datafileLoadPalette("/tmp/nope.pcx"));
        assertEquals(0x8DE4C36EL, crc32(Datafile.datafileGetPalette(), 768));
    }

    @Test
    void manglerHook() throws Exception {
        mountAndLoadPalette();
        String pcx = fixture("test.pcx");
        Datafile.fileNameMangler = path -> path.equals("alias.pcx") ? pcx : path;

        Datafile.Image image = Datafile.datafileReadRaw("alias.pcx");

        assertNotNull(image);
        assertEquals(6, image.width());
        assertEquals(4, image.height());
    }

    @Test
    void loaderHook() throws Exception {
        mountAndLoadPalette();
        // Fixture palette first (mirrors the harness, where earlier reads
        // filled gDatafilePalette before the loader test).
        assertNotNull(Datafile.datafileReadRaw(fixture("test.pcx")));
        Datafile.loader = (path, palette, dims) -> {
            palette[0] = 0x42;
            dims[0] = 2;
            dims[1] = 2;
            return new byte[] { 9, 8, 7, 6 };
        };

        Datafile.Image image = Datafile.datafileRead("custom.xxx");

        assertNotNull(image);
        assertEquals(2, image.width());
        assertEquals(2, image.height());
        assertEquals(0x85A3347BL, crc32(image.data(), 4));
        assertEquals(0x42, Datafile.datafileGetPalette()[0] & 0xFF);
    }
}
