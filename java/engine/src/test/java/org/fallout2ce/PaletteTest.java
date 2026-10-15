package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Hermetic tests for {@code Palette} (fake clock / video / cycle / sound).
 */
class PaletteTest {

    static class RecordingVideo implements Color.Video {
        final List<byte[]> palettes = new ArrayList<>();
        final List<Runnable> callbacks = new ArrayList<>();

        @Override
        public void setPalette(byte[] rgb768) {
            palettes.add(rgb768.clone());
            callbacks.add(Color.transitionCallback);
        }

        @Override
        public void setPaletteEntries(byte[] rgb, int start, int count) {
        }

        @Override
        public void present() {
        }
    }

    static class RecordingCycle implements Palette.CycleControl {
        boolean enabled = true;
        int disables;
        int enables;

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public void disable() {
            disables++;
        }

        @Override
        public void enable() {
            enables++;
        }
    }

    private double savedAnimSpeed = Double.NaN;

    @AfterEach
    void reset() {
        Palette.resetForTests();
        Color.resetForTests();
        Db.resetForTests();
        Ticks.resetForTests();
        if (!Double.isNaN(savedAnimSpeed)) {
            Settings.settings.ui.anim_speed = savedAnimSpeed;
            savedAnimSpeed = Double.NaN;
        }
    }

    void animSpeed(double value) {
        if (Double.isNaN(savedAnimSpeed)) {
            savedAnimSpeed = Settings.settings.ui.anim_speed;
        }
        Settings.settings.ui.anim_speed = value;
    }

    /** Fake clock advancing {@code step} ms per read. */
    static void clockStep(long step) {
        long[] now = { 1_000_000L };
        Ticks.clock = () -> {
            long t = now[0];
            now[0] += step;
            return t;
        };
    }

    @Test
    void initCalibratesSteps() {
        RecordingVideo video = new RecordingVideo();
        Color.video = video;
        Color.colorSetBrightness(1.0);
        video.palettes.clear();
        video.callbacks.clear();
        clockStep(35);
        animSpeed(1.0);
        Palette.soundEnabled = () -> true;

        Palette.paletteInit();

        byte[] white = new byte[768];
        byte[] black = new byte[768];
        for (int i = 0; i < 768; i++) {
            white[i] = 63;
        }
        assertArrayEquals(white, Palette.gPaletteWhite);
        assertArrayEquals(black, Palette.gPaletteBlack);
        assertArrayEquals(Color.cmap, Palette.gPalette);
        // 60 * 700 / 35.
        assertEquals(1200, Palette.getFadeSteps());
        // Reference fade is a no-op over 60 steps + final set.
        assertEquals(61, video.palettes.size());
        for (Runnable callback : video.callbacks) {
            assertTrue(callback == Palette.soundContinueAllHook);
        }
        // Cleared after the fade, as in C.
        assertEquals(null, Color.transitionCallback);
    }

    @Test
    void initHonorsAnimSpeed() {
        Color.video = new RecordingVideo();
        clockStep(70);
        animSpeed(2.0);

        Palette.paletteInit();

        // 60 * 350 / 70.
        assertEquals(300, Palette.getFadeSteps());
    }

    @Test
    void initZeroDurationDividesByZero() {
        Color.video = new RecordingVideo();
        Ticks.clock = () -> 5L;

        // C performs an unsigned /0 here (UB/crash); Java throws.
        assertThrows(ArithmeticException.class, Palette::paletteInit);
    }

    @Test
    void fadeTo() {
        RecordingVideo video = new RecordingVideo();
        RecordingCycle cycle = new RecordingCycle();
        Color.video = video;
        Palette.colorCycle = cycle;
        Color.colorSetBrightness(1.0);
        clockStep(35);
        animSpeed(1.0);
        Palette.soundEnabled = () -> true;
        Palette.paletteInit();
        video.palettes.clear();
        video.callbacks.clear();

        byte[] target = new byte[768];
        for (int i = 0; i < 768; i++) {
            target[i] = (byte) (i & 0x3F);
        }
        Palette.paletteFadeTo(target);

        assertEquals(1, cycle.disables);
        assertEquals(1, cycle.enables);
        assertEquals(1200 + 1, video.palettes.size());
        assertArrayEquals(target, video.palettes.get(video.palettes.size() - 1));
        assertArrayEquals(target, Palette.gPalette);
        for (Runnable callback : video.callbacks) {
            assertTrue(callback == Palette.soundContinueAllHook);
        }
        assertEquals(null, Color.transitionCallback);
    }

    @Test
    void fadeToCycleStaysDisabled() {
        Color.video = new RecordingVideo();
        RecordingCycle cycle = new RecordingCycle();
        cycle.enabled = false;
        Palette.colorCycle = cycle;
        clockStep(35);

        Palette.paletteInit();
        Palette.paletteFadeTo(new byte[768]);

        assertEquals(1, cycle.disables);
        assertEquals(0, cycle.enables);
    }

    @Test
    void setEntries() {
        RecordingVideo video = new RecordingVideo();
        Color.video = video;
        Color.colorSetBrightness(1.0);
        video.palettes.clear();

        byte[] palette = new byte[768];
        for (int i = 0; i < 768; i++) {
            palette[i] = (byte) (i & 0x3F);
        }
        Palette.paletteSetEntries(palette);

        assertArrayEquals(palette, Palette.gPalette);
        assertArrayEquals(palette, Color.systemCmap);
        assertEquals(1, video.palettes.size());
    }

    @Test
    void setEntriesInRange() {
        Color.video = new RecordingVideo();
        Color.colorSetBrightness(1.0);

        byte[] entries = { 10, 20, 30, 40, 50, 60 };
        Palette.paletteSetEntriesInRange(entries, 5, 6);

        assertEquals(10, Byte.toUnsignedInt(Palette.gPalette[15]));
        assertEquals(60, Byte.toUnsignedInt(Palette.gPalette[20]));
        assertEquals(0, Byte.toUnsignedInt(Palette.gPalette[0]));
        assertEquals(10, Byte.toUnsignedInt(Color.systemCmap[15]));
    }

    @Test
    void resetAndExitAreNoOps() {
        Palette.paletteReset();
        Palette.paletteExit();
        assertFalse(Palette.colorCycle.enabled());
        assertEquals(0, Palette.getFadeSteps());
    }
}
