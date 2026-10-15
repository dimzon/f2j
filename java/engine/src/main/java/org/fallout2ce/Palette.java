package org.fallout2ce;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

/**
 * Port of {@code src/palette.h} / {@code src/palette.cc} (global palettes,
 * fade-step calibration, fade/set entry points).
 *
 * <p>Sound / color-cycle calls are seams below so unit tests run headless.
 * Time comes from {@link Ticks}. The C names the seams stand in for:
 * {@code backgroundSoundIsEnabled}, {@code speechIsEnabled},
 * {@code soundContinueAll}, {@code colorCycleEnabled} /
 * {@code colorCycleDisable} / {@code colorCycleEnable}, {@code getTicks}.
 */
public final class Palette {

    private Palette() {
    }

    /** Port of {@code gPaletteWhite} (filled with 63 by {@link #paletteInit}). */
    public static final byte[] gPaletteWhite = new byte[768];

    /** Port of {@code gPaletteBlack} (filled with 0 by {@link #paletteInit}). */
    public static final byte[] gPaletteBlack = new byte[768];

    /** Port of {@code gPalette} (current logical palette). */
    public static final byte[] gPalette = new byte[768];

    /** Port of {@code gPaletteFadeSteps}. */
    private static int fadeSteps;

    /** Color-cycle seam (C {@code colorCycle*}). */
    public interface CycleControl {
        boolean enabled();

        void disable();

        void enable();
    }

    private static final CycleControl NO_CYCLE = new CycleControl() {
        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public void disable() {
        }

        @Override
        public void enable() {
        }
    };

    public static CycleControl colorCycle = NO_CYCLE;

    /** Port of {@code backgroundSoundIsEnabled() || speechIsEnabled()}. */
    public static BooleanSupplier soundEnabled = () -> false;

    /** Port of {@code soundContinueAll}. */
    public static Runnable soundContinueAllHook = () -> {
    };

    public static int getFadeSteps() {
        return fadeSteps;
    }

    /**
     * Port of {@code paletteInit}. Runs a 60-step no-op fade purely to time
     * it, then scales the step count so fades last ~700&nbsp;ms
     * ({@code settings.ui.anim_speed} adjusted).
     */
    public static void paletteInit() {
        Arrays.fill(gPaletteBlack, (byte) 0);
        Arrays.fill(gPaletteWhite, (byte) 63);
        System.arraycopy(Color.cmap, 0, gPalette, 0, gPalette.length);

        long tick = Ticks.getTicks();
        if (soundEnabled.getAsBoolean()) {
            Color.colorPaletteSetTransitionCallback(soundContinueAllHook);
        }

        colorPaletteFadeBetweenReference();

        Color.colorPaletteSetTransitionCallback(null);

        // Never 0 in the game (the fade above throttles); tests must advance
        // the fake clock or this divides by zero, exactly like the C
        // unsigned division.
        long actualFadeDuration = Ticks.getTicksSince(tick);

        long fadeDurationMs = (int) (700.0 / Settings.settings.ui.anim_speed) & 0xFFFFFFFFL;

        // 32-bit unsigned arithmetic, as in C.
        fadeSteps = (int) (60 * fadeDurationMs / actualFadeDuration & 0xFFFFFFFFL);

        // NOTE: %u has no Java equivalent; values are already masked to
        // 32-bit unsigned range.
        Debug.print("\nReference fade time: %dms, Target fade time: %dms, Fade steps: %d\n",
                actualFadeDuration, fadeDurationMs, fadeSteps);
    }

    private static void colorPaletteFadeBetweenReference() {
        Color.colorPaletteFadeBetween(gPalette, gPalette, 60);
    }

    /** Port of {@code paletteReset} (collapsed no-op in C). */
    public static void paletteReset() {
    }

    /** Port of {@code paletteExit} (collapsed no-op in C). */
    public static void paletteExit() {
    }

    /** Port of {@code paletteFadeTo}. */
    public static void paletteFadeTo(byte[] palette) {
        boolean colorCycleWasEnabled = colorCycle.enabled();
        colorCycle.disable();

        if (soundEnabled.getAsBoolean()) {
            Color.colorPaletteSetTransitionCallback(soundContinueAllHook);
        }

        Color.colorPaletteFadeBetween(gPalette, palette, fadeSteps);
        Color.colorPaletteSetTransitionCallback(null);

        System.arraycopy(palette, 0, gPalette, 0, 768);

        if (colorCycleWasEnabled) {
            colorCycle.enable();
        }
    }

    /** Port of {@code paletteSetEntries}. */
    public static void paletteSetEntries(byte[] palette) {
        System.arraycopy(palette, 0, gPalette, 0, gPalette.length);
        Color.setSystemPalette(palette);
    }

    /** Port of {@code paletteSetEntriesInRange}. */
    public static void paletteSetEntriesInRange(byte[] palette, int start, int end) {
        System.arraycopy(palette, 0, gPalette, 3 * start, 3 * (end - start + 1));
        Color.setSystemPaletteEntries(palette, start, end);
    }

    /** Test-only full reset (mirrors the C static initial state). */
    static void resetForTests() {
        Arrays.fill(gPaletteWhite, (byte) 0);
        Arrays.fill(gPaletteBlack, (byte) 0);
        Arrays.fill(gPalette, (byte) 0);
        fadeSteps = 0;
        colorCycle = NO_CYCLE;
        soundEnabled = () -> false;
        soundContinueAllHook = () -> {
        };
    }
}
