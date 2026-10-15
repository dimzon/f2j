package org.fallout2ce;

import java.util.Arrays;

import org.fallout2ce.spi.TickerRegistry;

/**
 * Port of {@code src/cycle.h} / {@code src/cycle.cc} (palette color cycling).
 *
 * <p>The ticker mutates the system palette in place (via
 * {@link Color#getSystemPalette}) and pushes entries 229-255 through
 * {@link Palette#paletteSetEntriesInRange}. Time comes from {@link Ticks};
 * ticker registration is a platform SPI seam (native input pumps plug in
 * via ServiceLoader).
 */
public final class ColorCycle {

    private ColorCycle() {
    }

    static final int SLOW_PERIOD = 1000 / 5;
    static final int MEDIUM_PERIOD = 1000 / 7;
    static final int FAST_PERIOD = 1000 / 10;
    static final int VERY_FAST_PERIOD = 1000 / 30;

    static final int RANGE_START = 229;
    static final int RANGE_END = 255;

    // Green.
    private static final byte[] ORIGINAL_SLIME = {
        0, 108, 0,
        11, 115, 7,
        27, 123, 15,
        43, (byte) 131, 27,
    };

    // Light gray?
    private static final byte[] ORIGINAL_SHORELINE = {
        83, 63, 43,
        75, 59, 43,
        67, 55, 39,
        63, 51, 39,
        55, 47, 35,
        51, 43, 35,
    };

    // Orange.
    private static final byte[] ORIGINAL_FIRE_SLOW = {
        (byte) 255, 0, 0,
        (byte) 215, 0, 0,
        (byte) 147, 43, 11,
        (byte) 255, 119, 0,
        (byte) 255, 59, 0,
    };

    // Red.
    private static final byte[] ORIGINAL_FIRE_FAST = {
        71, 0, 0,
        123, 0, 0,
        (byte) 179, 0, 0,
        123, 0, 0,
        71, 0, 0,
    };

    // Light blue.
    private static final byte[] ORIGINAL_MONITORS = {
        107, 107, 111,
        99, 103, 127,
        87, 107, (byte) 143,
        0, (byte) 147, (byte) 163,
        107, (byte) 187, (byte) 255,
    };

    static final byte[] slime = ORIGINAL_SLIME.clone();
    static final byte[] shoreline = ORIGINAL_SHORELINE.clone();
    static final byte[] fireSlow = ORIGINAL_FIRE_SLOW.clone();
    static final byte[] fireFast = ORIGINAL_FIRE_FAST.clone();
    static final byte[] monitors = ORIGINAL_MONITORS.clone();

    private static boolean initialized;
    private static boolean enabled;
    private static int speedFactor = 1;

    private static long lastSlow;
    private static long lastMedium;
    private static long lastFast;
    private static long lastVeryFast;

    private static int slimeStart;
    private static int shorelineStart;
    private static int fireSlowStart;
    private static int fireFastStart;
    private static int monitorsStart;
    private static int bobberRed;
    private static int bobberDiff = -4;

    /** Ticker-registry seam (C {@code tickersAdd}/{@code tickersRemove}). */
    private static final TickerRegistry NO_TICKERS = new TickerRegistry() {
        @Override
        public void add(Runnable ticker) {
        }

        @Override
        public void remove(Runnable ticker) {
        }
    };

    public static TickerRegistry tickers = NO_TICKERS;

    /** The ticker proc registered with the engine (C {@code colorCycleTicker}). */
    public static final Runnable ticker = ColorCycle::colorCycleTicker;

    private static void halve(byte[] table) {
        for (int i = 0; i < table.length; i++) {
            table[i] = (byte) (Byte.toUnsignedInt(table[i]) >> 2);
        }
    }

    /** Port of {@code colorCycleInit}. */
    public static void colorCycleInit() {
        if (initialized) {
            return;
        }

        if (!Settings.settings.system.color_cycling) {
            return;
        }

        halve(slime);
        halve(shoreline);
        halve(fireSlow);
        halve(fireFast);
        halve(monitors);

        tickers.add(ticker);

        initialized = true;
        enabled = true;

        cycleSetSpeedFactor(Settings.settings.system.cycle_speed_factor);
    }

    /** Port of {@code colorCycleReset}. */
    public static void colorCycleReset() {
        if (initialized) {
            lastSlow = 0;
            lastMedium = 0;
            lastFast = 0;
            lastVeryFast = 0;
            tickers.add(ticker);
            enabled = true;
        }
    }

    /** Port of {@code colorCycleFree}. */
    public static void colorCycleFree() {
        if (initialized) {
            tickers.remove(ticker);
            initialized = false;
            enabled = false;
        }
    }

    /** Port of {@code colorCycleDisable}. */
    public static void colorCycleDisable() {
        enabled = false;
    }

    /** Port of {@code colorCycleEnable}. */
    public static void colorCycleEnable() {
        enabled = true;
    }

    /** Port of {@code colorCycleEnabled}. */
    public static boolean colorCycleEnabled() {
        return enabled;
    }

    /** Port of {@code cycleSetSpeedFactor}. */
    public static void cycleSetSpeedFactor(int value) {
        speedFactor = value;
        Settings.settings.system.cycle_speed_factor = value;
    }

    static int getSpeedFactor() {
        return speedFactor;
    }

    /** Port of {@code colorCycleTicker}. */
    public static void colorCycleTicker() {
        if (!enabled) {
            return;
        }

        boolean changed = false;

        byte[] palette = Color.getSystemPalette();
        long time = Ticks.getTicks();

        if (Ticks.getTicksBetween(time, lastSlow) >= (long) SLOW_PERIOD * speedFactor) {
            changed = true;
            lastSlow = time;

            int paletteIndex = 229 * 3;
            for (int i = slimeStart; i < slime.length; i++) {
                palette[paletteIndex++] = slime[i];
            }
            for (int i = 0; i < slimeStart; i++) {
                palette[paletteIndex++] = slime[i];
            }
            slimeStart -= 3;
            if (slimeStart < 0) {
                slimeStart = 9;
            }

            paletteIndex = 248 * 3;
            for (int i = shorelineStart; i < shoreline.length; i++) {
                palette[paletteIndex++] = shoreline[i];
            }
            for (int i = 0; i < shorelineStart; i++) {
                palette[paletteIndex++] = shoreline[i];
            }
            shorelineStart -= 3;
            if (shorelineStart < 0) {
                shorelineStart = 15;
            }

            paletteIndex = 238 * 3;
            for (int i = fireSlowStart; i < fireSlow.length; i++) {
                palette[paletteIndex++] = fireSlow[i];
            }
            for (int i = 0; i < fireSlowStart; i++) {
                palette[paletteIndex++] = fireSlow[i];
            }
            fireSlowStart -= 3;
            if (fireSlowStart < 0) {
                fireSlowStart = 12;
            }
        }

        if (Ticks.getTicksBetween(time, lastMedium) >= (long) MEDIUM_PERIOD * speedFactor) {
            changed = true;
            lastMedium = time;

            int paletteIndex = 243 * 3;
            for (int i = fireFastStart; i < fireFast.length; i++) {
                palette[paletteIndex++] = fireFast[i];
            }
            for (int i = 0; i < fireFastStart; i++) {
                palette[paletteIndex++] = fireFast[i];
            }
            fireFastStart -= 3;
            if (fireFastStart < 0) {
                fireFastStart = 12;
            }
        }

        if (Ticks.getTicksBetween(time, lastFast) >= (long) FAST_PERIOD * speedFactor) {
            changed = true;
            lastFast = time;

            int paletteIndex = 233 * 3;
            for (int i = monitorsStart; i < monitors.length; i++) {
                palette[paletteIndex++] = monitors[i];
            }
            for (int i = 0; i < monitorsStart; i++) {
                palette[paletteIndex++] = monitors[i];
            }
            monitorsStart -= 3;
            if (monitorsStart < 0) {
                monitorsStart = 12;
            }
        }

        if (Ticks.getTicksBetween(time, lastVeryFast) >= (long) VERY_FAST_PERIOD * speedFactor) {
            changed = true;
            lastVeryFast = time;

            if (bobberRed == 0 || bobberRed == 60) {
                bobberDiff = -bobberDiff;
            }
            bobberRed += bobberDiff;

            int paletteIndex = 254 * 3;
            palette[paletteIndex++] = (byte) bobberRed;
            palette[paletteIndex++] = 0;
            palette[paletteIndex] = 0;
        }

        if (changed) {
            Palette.paletteSetEntriesInRange(
                    Arrays.copyOfRange(palette, RANGE_START * 3, (RANGE_END + 1) * 3),
                    RANGE_START, RANGE_END);
        }
    }

    /** Test-only full reset (tables back to pre-halved values). */
    static void resetForTests() {
        System.arraycopy(ORIGINAL_SLIME, 0, slime, 0, slime.length);
        System.arraycopy(ORIGINAL_SHORELINE, 0, shoreline, 0, shoreline.length);
        System.arraycopy(ORIGINAL_FIRE_SLOW, 0, fireSlow, 0, fireSlow.length);
        System.arraycopy(ORIGINAL_FIRE_FAST, 0, fireFast, 0, fireFast.length);
        System.arraycopy(ORIGINAL_MONITORS, 0, monitors, 0, monitors.length);
        initialized = false;
        enabled = false;
        speedFactor = 1;
        lastSlow = 0;
        lastMedium = 0;
        lastFast = 0;
        lastVeryFast = 0;
        slimeStart = 0;
        shorelineStart = 0;
        fireSlowStart = 0;
        fireFastStart = 0;
        monitorsStart = 0;
        bobberRed = 0;
        bobberDiff = -4;
        tickers = NO_TICKERS;
    }
}
