package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Hermetic ticker tests (fake clock) plus real-data vectors from
 * {@code docker/refharness/golden/v14.txt}.
 */
class ColorCycleTest {

    static class RecordingTickers implements ColorCycle.TickerRegistry {
        final List<Runnable> added = new ArrayList<>();
        final List<Runnable> removed = new ArrayList<>();

        @Override
        public void add(Runnable ticker) {
            added.add(ticker);
        }

        @Override
        public void remove(Runnable ticker) {
            removed.add(ticker);
        }
    }

    static class RecordingVideo implements Color.Video {
        final List<byte[]> entries = new ArrayList<>();

        @Override
        public void setPalette(byte[] rgb768) {
        }

        @Override
        public void setPaletteEntries(byte[] rgb, int start, int count) {
            entries.add(rgb.clone());
        }

        @Override
        public void present() {
        }
    }

    private final long[] now = { 0L };
    private boolean savedCycling;
    private boolean cyclingSaved;
    private int savedSpeedFactor;
    private boolean speedSaved;

    @AfterEach
    void reset() {
        ColorCycle.resetForTests();
        Palette.resetForTests();
        Color.resetForTests();
        Db.resetForTests();
        Ticks.resetForTests();
        if (cyclingSaved) {
            Settings.settings.system.color_cycling = savedCycling;
        }
        if (speedSaved) {
            Settings.settings.system.cycle_speed_factor = savedSpeedFactor;
        }
    }

    void cycling(boolean value) {
        if (!cyclingSaved) {
            savedCycling = Settings.settings.system.color_cycling;
            cyclingSaved = true;
        }
        Settings.settings.system.color_cycling = value;
    }

    void speedFactorSetting(int value) {
        if (!speedSaved) {
            savedSpeedFactor = Settings.settings.system.cycle_speed_factor;
            speedSaved = true;
        }
        Settings.settings.system.cycle_speed_factor = value;
    }

    void useFakeClock() {
        now[0] = 0;
        Ticks.clock = () -> now[0];
    }

    void advance(long ms) {
        now[0] += ms;
    }

    /** Installs {@code systemCmap} content without game data. */
    void installPalette(byte[] pattern) {
        Color.video = new RecordingVideo();
        Color.colorSetBrightness(1.0);
        Color.setSystemPalette(pattern);
    }

    static long rangeCrc() {
        CRC32 crc = new CRC32();
        crc.update(Color.systemCmap, 229 * 3, 81);
        return crc.getValue();
    }

    static int slimeR() {
        return Byte.toUnsignedInt(Color.systemCmap[229 * 3]);
    }

    static int slimeG() {
        return Byte.toUnsignedInt(Color.systemCmap[229 * 3 + 1]);
    }

    static int slimeB() {
        return Byte.toUnsignedInt(Color.systemCmap[229 * 3 + 2]);
    }

    static int bobber() {
        return Byte.toUnsignedInt(Color.systemCmap[254 * 3]);
    }

    @Test
    void initHalvesTablesOnce() {
        useFakeClock();
        RecordingTickers tickers = new RecordingTickers();
        ColorCycle.tickers = tickers;
        cycling(true);
        speedFactorSetting(1);

        ColorCycle.colorCycleInit();

        assertTrue(ColorCycle.colorCycleEnabled());
        assertEquals(1, tickers.added.size());
        assertTrue(tickers.added.get(0) == ColorCycle.ticker);
        // 108 >> 2, 83 >> 2, 255 >> 2, 255 >> 2.
        assertEquals(27, Byte.toUnsignedInt(ColorCycle.slime[1]));
        assertEquals(20, Byte.toUnsignedInt(ColorCycle.shoreline[0]));
        assertEquals(63, Byte.toUnsignedInt(ColorCycle.fireSlow[0]));
        assertEquals(63, Byte.toUnsignedInt(ColorCycle.monitors[14]));
        assertEquals(1, ColorCycle.getSpeedFactor());

        ColorCycle.colorCycleInit();
        assertEquals(27, Byte.toUnsignedInt(ColorCycle.slime[1]));
        assertEquals(1, tickers.added.size());
    }

    @Test
    void initRespectsSetting() {
        useFakeClock();
        RecordingTickers tickers = new RecordingTickers();
        ColorCycle.tickers = tickers;
        cycling(false);

        ColorCycle.colorCycleInit();

        assertFalse(ColorCycle.colorCycleEnabled());
        assertTrue(tickers.added.isEmpty());
        assertEquals(108, Byte.toUnsignedInt(ColorCycle.slime[1]));
    }

    @Test
    void tickerRotatesAllBands() {
        useFakeClock();
        cycling(true);
        speedFactorSetting(1);
        byte[] pattern = new byte[768];
        for (int i = 0; i < 768; i++) {
            pattern[i] = (byte) (i & 0x3F);
        }
        installPalette(pattern);
        RecordingVideo video = (RecordingVideo) Color.video;
        ColorCycle.colorCycleInit();

        now[0] = 1000;
        ColorCycle.colorCycleTicker();

        // slime_start was 0: identity order, halved values.
        assertEquals(0, slimeR());
        assertEquals(27, slimeG());
        assertEquals(0, slimeB());
        assertEquals(4, bobber());
        // Pushed through to video + gPalette.
        assertFalse(video.entries.isEmpty());
        assertArrayEquals(
                Arrays.copyOfRange(Color.systemCmap, 229 * 3, 768),
                Arrays.copyOfRange(Palette.gPalette, 229 * 3, 768));

        long crc1 = rangeCrc();
        ColorCycle.colorCycleTicker();
        assertEquals(crc1, rangeCrc());

        advance(250);
        ColorCycle.colorCycleTicker();
        // slime_start was 9: slime[9..11] >> 2 = 43, 131, 27 -> 10, 32, 6.
        assertEquals(10, slimeR());
        assertEquals(32, slimeG());
        assertEquals(6, slimeB());
        assertEquals(8, bobber());
    }

    @Test
    void tickerFiresBandsIndependently() {
        useFakeClock();
        cycling(true);
        speedFactorSetting(1);
        installPalette(new byte[768]);
        ColorCycle.colorCycleInit();

        now[0] = 1000;
        ColorCycle.colorCycleTicker();
        long crc1 = rangeCrc();

        // +100ms: fast (100) + very-fast (33) fire; medium (142) + slow (200) hold.
        advance(100);
        ColorCycle.colorCycleTicker();
        // Slime untouched, bobber advanced 4 -> 8.
        assertEquals(0, slimeR());
        assertEquals(27, slimeG());
        assertEquals(8, bobber());
        // Monitors rotated (crc differs from a slow-only world is implied).
        assertTrue(rangeCrc() != crc1);
    }

    @Test
    void speedFactorScalesPeriods() {
        useFakeClock();
        cycling(true);
        speedFactorSetting(1);
        installPalette(new byte[768]);
        ColorCycle.colorCycleInit();
        ColorCycle.cycleSetSpeedFactor(2);
        assertEquals(2, Settings.settings.system.cycle_speed_factor);

        now[0] = 1000;
        ColorCycle.colorCycleTicker();
        long crc1 = rangeCrc();

        // +250ms at factor 2: fast (200) + very-fast (66) fire;
        // medium (284) + slow (400) hold.
        advance(250);
        ColorCycle.colorCycleTicker();
        assertEquals(0, slimeR());
        assertEquals(8, bobber());
        assertTrue(rangeCrc() != crc1);
    }

    @Test
    void bobberBounces() {
        useFakeClock();
        cycling(true);
        speedFactorSetting(1);
        installPalette(new byte[768]);
        ColorCycle.colorCycleInit();

        List<Integer> reds = new ArrayList<>();
        now[0] = 1000;
        for (int i = 0; i < 31; i++) {
            ColorCycle.colorCycleTicker();
            reds.add(bobber());
            advance(33);
        }

        // 4, 8, ..., 60, 56, 52, ..., 4, 0(? no: 4 -> ... -> 0 never lands:
        // 60 -> 56 ... 4 -> 0? 4 - 4 = 0, then 0 flips to +4).
        assertEquals(4, reds.get(0));
        assertEquals(60, reds.get(14));
        assertEquals(56, reds.get(15));
        assertEquals(4, reds.get(28));
        assertEquals(0, reds.get(29));
        assertEquals(4, reds.get(30));
    }

    @Test
    void disableEnableFreeReset() {
        useFakeClock();
        RecordingTickers tickers = new RecordingTickers();
        ColorCycle.tickers = tickers;
        cycling(true);
        speedFactorSetting(1);
        installPalette(new byte[768]);
        ColorCycle.colorCycleInit();

        now[0] = 1000;
        ColorCycle.colorCycleTicker();
        long crc1 = rangeCrc();

        ColorCycle.colorCycleDisable();
        assertFalse(ColorCycle.colorCycleEnabled());
        advance(1000);
        ColorCycle.colorCycleTicker();
        assertEquals(crc1, rangeCrc());

        ColorCycle.colorCycleEnable();
        ColorCycle.colorCycleTicker();
        assertTrue(rangeCrc() != crc1);

        ColorCycle.colorCycleFree();
        assertFalse(ColorCycle.colorCycleEnabled());
        assertEquals(1, tickers.removed.size());
        advance(1000);
        long crcAfterFree = rangeCrc();
        ColorCycle.colorCycleTicker();
        assertEquals(crcAfterFree, rangeCrc());

        // Reset after free is a no-op.
        ColorCycle.colorCycleReset();
        assertFalse(ColorCycle.colorCycleEnabled());
    }

    @Test
    void resetRearms() {
        useFakeClock();
        cycling(true);
        speedFactorSetting(1);
        installPalette(new byte[768]);
        ColorCycle.colorCycleInit();

        now[0] = 1000;
        ColorCycle.colorCycleTicker();
        long crc1 = rangeCrc();

        ColorCycle.colorCycleTicker();
        assertEquals(crc1, rangeCrc());

        // Reset zeroes the last-fire stamps: next tick fires everything.
        ColorCycle.colorCycleReset();
        assertTrue(ColorCycle.colorCycleEnabled());
        ColorCycle.colorCycleTicker();
        assertTrue(rangeCrc() != crc1);
        assertEquals(8, bobber());
    }

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        Path p = Path.of(root, name);
        assumeTrue(Files.isRegularFile(p), "missing " + p);
        return p.toString();
    }

    @Test
    void realDataVectors() throws Exception {
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(Color.colorPaletteLoad("color.pal"));
        cycling(true);
        speedFactorSetting(1);
        // NOTE: no setSystemPalette, mirroring the harness (all zeros).
        Color.video = new RecordingVideo();

        ColorCycle.colorCycleInit();
        assertTrue(ColorCycle.colorCycleEnabled());
        assertEquals(1, ColorCycle.getSpeedFactor());
        assertEquals(0x9BE58BD8L, rangeCrc());

        Thread.sleep(300);
        ColorCycle.colorCycleTicker();
        assertEquals(0x6E269B19L, rangeCrc());
        assertEquals(0, slimeR());
        assertEquals(27, slimeG());
        assertEquals(0, slimeB());
        assertEquals(4, bobber());

        ColorCycle.colorCycleTicker();
        assertEquals(0x6E269B19L, rangeCrc());

        ColorCycle.colorCycleReset();
        ColorCycle.colorCycleTicker();
        assertEquals(0xAA26225CL, rangeCrc());
        assertEquals(10, slimeR());
        assertEquals(32, slimeG());
        assertEquals(6, slimeB());
        assertEquals(8, bobber());

        ColorCycle.colorCycleDisable();
        ColorCycle.colorCycleTicker();
        assertEquals(0xAA26225CL, rangeCrc());

        ColorCycle.colorCycleEnable();
        Thread.sleep(300);
        ColorCycle.colorCycleTicker();
        assertEquals(0x9656ACF4L, rangeCrc());
        assertEquals(6, slimeR());
        assertEquals(30, slimeG());
        assertEquals(3, slimeB());
        assertEquals(12, bobber());

        ColorCycle.colorCycleFree();
        assertFalse(ColorCycle.colorCycleEnabled());
        ColorCycle.colorCycleTicker();
        assertEquals(0x9656ACF4L, rangeCrc());
    }
}
