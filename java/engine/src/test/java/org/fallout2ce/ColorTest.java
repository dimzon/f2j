package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@code rgb555} vectors from {@code golden/v1.txt}, {@code [color]} vectors
 * from {@code golden/v13.txt}, plus hermetic synthetic-palette tests.
 */
class ColorTest {

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

    static long crc32(byte[][] rows) {
        CRC32 crc = new CRC32();
        for (byte[] row : rows) {
            crc.update(row);
        }
        return crc.getValue();
    }

    /**
     * Masks the 2 trailing blend-table rows {@code _buildBlendTable} never
     * writes (malloc garbage in C) so the CRC is deterministic. Mirrors the
     * masking in {@code ref_main.cc}.
     */
    static long maskedBlendCrc(byte[] table) {
        byte[] copy = table.clone();
        for (int i = 3584; i < 4096; i++) {
            copy[i] = 0;
        }
        return crc32(copy);
    }

    static class RecordingVideo implements Color.Video {
        final List<byte[]> palettes = new ArrayList<>();
        final List<byte[]> entries = new ArrayList<>();
        int presents;

        @Override
        public void setPalette(byte[] rgb768) {
            palettes.add(rgb768.clone());
        }

        @Override
        public void setPaletteEntries(byte[] rgb, int start, int count) {
            entries.add(rgb.clone());
        }

        @Override
        public void present() {
            presents++;
        }
    }

    static class CountingFps implements Color.FpsLimiter {
        int marks;
        int throttles;

        @Override
        public void mark() {
            marks++;
        }

        @Override
        public void throttle() {
            throttles++;
        }
    }

    @Test
    void rgb555() {
        assertEquals(0, Color.rgb555(0x000000));
        assertEquals(32767, Color.rgb555(0xFFFFFF));
        assertEquals(31744, Color.rgb555(0xFF0000));
        assertEquals(992, Color.rgb555(0x00FF00));
        assertEquals(31, Color.rgb555(0x0000FF));
        assertEquals(32074, Color.rgb555(0xFF5252));
        assertEquals(25439, Color.rgb555(0xC5D6FF));
        assertEquals(2250, Color.rgb555(0x123456));
        assertEquals(15845, Color.rgb555(0x7B7B29));
    }

    @Test
    void tableSizes() {
        assertEquals(768, Color.cmap.length);
        assertEquals(768, Color.systemCmap.length);
        assertEquals(64, Color.currentGammaTable.length);
        assertEquals(256, Color.mappedColor.length);
        assertEquals(32768, Color.colorTable.length);
    }

    @Test
    void flags() {
        assertEquals(0x0000000, Color.DRAW_TEXT_FLAG_NONE);
        assertEquals(0x0010000, Color.DRAW_TEXT_FLAG_SHADOWED);
        assertEquals(0x01000000, Color.DRAW_TEXT_FLAG_REFRESH);
    }

    @Test
    void loadMissing() {
        assertFalse(Color.colorPaletteLoad("no_such.pal"));
        assertEquals("color.c: color table not found\n", Color.colorError());
    }

    /** Builds a synthetic color.pal: 256 triples + 32 KiB table, no NEWC. */
    static Path writeSyntheticPal(Path dir, int unmappedIndex) throws Exception {
        byte[] pal = new byte[768 + 32768];
        for (int i = 0; i < 256; i++) {
            int v = i & 0x3F;
            pal[i * 3] = (byte) v;
            pal[i * 3 + 1] = (byte) v;
            pal[i * 3 + 2] = (byte) v;
        }
        // One out-of-range triple -> unmapped, zeroed.
        pal[unmappedIndex * 3] = 0x40;
        pal[unmappedIndex * 3 + 1] = 0x20;
        pal[unmappedIndex * 3 + 2] = 0x10;
        for (int i = 0; i < 32768; i++) {
            pal[768 + i] = (byte) (i * 31 + 7);
        }
        Path p = dir.resolve("color.pal");
        Files.write(p, pal);
        return p;
    }

    @Test
    void syntheticLoadMapping() throws Exception {
        Path dir = Files.createTempDirectory("f2j-color");
        Path pal = writeSyntheticPal(dir, 100);

        assertTrue(Color.colorPaletteLoad(pal.toString()));
        assertEquals("color.c: No errors\n", Color.colorError());

        // Index 0 comes from the file too (COLOR_FIRST = 0), overwriting the
        // { 3F 3F 3F } initial bytes.
        assertEquals(0, Byte.toUnsignedInt(Color.cmap[0]));
        assertEquals(0, Byte.toUnsignedInt(Color.cmap[1]));
        assertEquals(0, Byte.toUnsignedInt(Color.cmap[2]));

        assertEquals(1, Color.mappedColor[0]);
        assertEquals(1, Color.mappedColor[1]);
        assertEquals(1 & 0x3F, Byte.toUnsignedInt(Color.cmap[3]));
        assertEquals(0, Color.mappedColor[100]);
        assertEquals(0, Byte.toUnsignedInt(Color.cmap[300]));
        assertEquals(0, Byte.toUnsignedInt(Color.cmap[301]));
        assertEquals(0, Byte.toUnsignedInt(Color.cmap[302]));

        int mapped = 0;
        for (int i = 0; i < 256; i++) {
            if (Color.mappedColor[i] != 0) {
                mapped++;
            }
        }
        assertEquals(255, mapped);

        byte[] expectedTable = new byte[32768];
        for (int i = 0; i < 32768; i++) {
            expectedTable[i] = (byte) (i * 31 + 7);
        }
        assertArrayEquals(expectedTable, Color.colorTable);

        // Unmapped rows: intensity filled with COLOR_FIRST (0), mix falls back.
        for (int i = 0; i < 256; i++) {
            assertEquals(0, Byte.toUnsignedInt(Color.intensityColorTable[100][i]));
        }
        assertEquals(50, Byte.toUnsignedInt(Color.colorMixAddTable[100][50]));
        assertEquals(50, Byte.toUnsignedInt(Color.colorMixMulTable[100][50]));
        assertEquals(50, Byte.toUnsignedInt(Color.colorMixAddTable[50][100]));
        assertEquals(50, Byte.toUnsignedInt(Color.colorMixMulTable[50][100]));
    }

    @Test
    void syntheticBlendRefcountQuirk() throws Exception {
        Path dir = Files.createTempDirectory("f2j-color");
        Path pal = writeSyntheticPal(dir, 100);
        assertTrue(Color.colorPaletteLoad(pal.toString()));

        byte[] first = Color.getColorBlendTable(7);
        byte[] second = Color.getColorBlendTable(7);
        assertSame(first, second);
        // One get bumps the refcount to 2: a single free must not release.
        Color.freeColorBlendTable(7);
        assertSame(first, Color.getColorBlendTable(7));
        // Drain: 3 outstanding gets need 3 frees to release.
        Color.freeColorBlendTable(7);
        Color.freeColorBlendTable(7);
        Color.freeColorBlendTable(7);
        byte[] rebuilt = Color.getColorBlendTable(7);
        assertNotSame(first, rebuilt);
        assertEquals(maskedBlendCrc(first), maskedBlendCrc(rebuilt));
        Color.freeColorBlendTable(7);
        Color.freeColorBlendTable(7);
    }

    @Test
    void closeFreesBlendsOnly() throws Exception {
        Path dir = Files.createTempDirectory("f2j-color");
        Path pal = writeSyntheticPal(dir, 100);
        assertTrue(Color.colorPaletteLoad(pal.toString()));
        Color.getColorBlendTable(7);
        Color.freeColorBlendTable(7);

        Color.colorsClose();

        assertNull(Color.blendTable[7]);
        // C does not zero the other tables on close.
        assertEquals(1, Color.mappedColor[1]);
    }

    @Test
    void brightness() {
        RecordingVideo video = new RecordingVideo();
        Color.video = video;

        Color.colorSetBrightness(1.0);
        byte[] identity = new byte[64];
        for (int i = 0; i < 64; i++) {
            identity[i] = (byte) i;
        }
        assertArrayEquals(identity, Color.currentGammaTable);
        // systemCmap is zero, so the pushed palette is all zeros.
        assertEquals(1, video.palettes.size());
        assertArrayEquals(new byte[768], video.palettes.get(0));

        Color.colorSetBrightness(2.0);
        assertEquals(0, Byte.toUnsignedInt(Color.currentGammaTable[0]));
        assertEquals(49, Byte.toUnsignedInt(Color.currentGammaTable[7]));
        assertEquals(63, Byte.toUnsignedInt(Color.currentGammaTable[8]));
        assertEquals(63, Byte.toUnsignedInt(Color.currentGammaTable[63]));
    }

    @Test
    void fade() {
        RecordingVideo video = new RecordingVideo();
        CountingFps fps = new CountingFps();
        Color.video = video;
        Color.fpsLimiter = fps;
        Color.colorSetBrightness(1.0);
        video.palettes.clear();

        final int[] callbacks = { 0 };
        Color.transitionCallback = () -> callbacks[0]++;

        byte[] oldPal = new byte[768];
        byte[] newPal = new byte[768];
        for (int i = 0; i < 768; i++) {
            newPal[i] = 63;
        }
        Color.colorPaletteFadeBetween(oldPal, newPal, 4);

        // steps intermediate + 1 final; callback only at step 0 (0 % 128).
        assertEquals(5, video.palettes.size());
        assertEquals(5, video.presents);
        assertEquals(1, callbacks[0]);
        assertEquals(5, fps.marks);
        assertEquals(5, fps.throttles);
        assertArrayEquals(newPal, video.palettes.get(4));
        // Step 2 of 4: 0 + 63 * 2 / 4 = 31 (integer math, as in C).
        assertEquals(31, Byte.toUnsignedInt(video.palettes.get(2)[0]));
        assertArrayEquals(newPal, Color.systemCmap);
    }

    @Test
    void systemPaletteEntries() {
        RecordingVideo video = new RecordingVideo();
        Color.video = video;
        Color.colorSetBrightness(1.0);

        byte[] entries = { 10, 20, 30, 40, 50, 60 };
        Color.setSystemPaletteEntries(entries, 5, 6);

        assertEquals(10, Byte.toUnsignedInt(Color.systemCmap[15]));
        assertEquals(60, Byte.toUnsignedInt(Color.systemCmap[20]));
        assertEquals(0, Byte.toUnsignedInt(Color.systemCmap[0]));
        assertEquals(1, video.entries.size());
        assertArrayEquals(entries, video.entries.get(0));
    }

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        Path p = Path.of(root, name);
        assumeTrue(Files.isRegularFile(p), "missing " + p);
        return p.toString();
    }

    @Test
    void realDataVectors() {
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));

        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals("color.c: No errors\n", Color.colorError());

        assertEquals(0xA28DF92EL, crc32(Color.cmap));
        assertEquals(0x8D26D2A8L, crc32(Color.colorTable));
        assertEquals(0xC9B21ACDL, crc32(Color.intensityColorTable));
        assertEquals(0xB9E86010L, crc32(Color.colorMixAddTable));
        assertEquals(0xD94D2D5AL, crc32(Color.colorMixMulTable));

        int mapped = 0;
        for (int i = 0; i < 256; i++) {
            if (Color.mappedColor[i] != 0) {
                mapped++;
            }
        }
        assertEquals(228, mapped);

        assertEquals(133, Color.red());
        assertEquals(220, Color.white());
        assertEquals(228, Color.black());
        assertEquals(32767, Color.color2RGB(Color.white()));
        assertEquals(228, Color.calculateColor(0, Color.white()));

        byte[] blend = Color.getColorBlendTable(Color.red());
        assertEquals(0x1629B495L, maskedBlendCrc(blend));
        assertSame(blend, Color.getColorBlendTable(Color.red()));
        Color.freeColorBlendTable(Color.red());
        Color.freeColorBlendTable(Color.red());
        assertEquals(0x1629B495L, maskedBlendCrc(Color.getColorBlendTable(Color.red())));
        Color.freeColorBlendTable(Color.red());
        Color.freeColorBlendTable(Color.red());

        assertFalse(Color.colorPaletteLoad("no_such.pal"));
        assertEquals("color.c: color table not found\n", Color.colorError());
    }
}
