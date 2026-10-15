package org.fallout2ce;

import org.fallout2ce.spi.SystemClock;

/**
 * Port of the tick helpers from {@code src/input.cc} ({@code getTicks},
 * {@code getTicksSince}, {@code getTicksBetween}).
 *
 * <p>Values are 32-bit unsigned milliseconds (wrap every ~49 days), stored in
 * {@code long}. The game only ever compares differences, so tests drive time
 * through {@link #clock}. The clock type comes from the platform SPI;
 * native providers plug in their own (e.g. {@code SDL_GetTicks}).
 */
public final class Ticks {

    private Ticks() {
    }

    /** Clock seam (C {@code SDL_GetTicks}). */
    public static SystemClock clock = System::currentTimeMillis;

    /** Port of {@code getTicks}. */
    public static long getTicks() {
        return clock.millis() & 0xFFFFFFFFL;
    }

    /** Port of {@code getTicksSince}. */
    public static long getTicksSince(long start) {
        return getTicksBetween(getTicks(), start);
    }

    /**
     * Port of {@code getTicksBetween}. Note the quirk: a wrapped (negative)
     * difference saturates to {@code INT_MAX} instead of wrapping.
     */
    public static long getTicksBetween(long end, long start) {
        long e = end & 0xFFFFFFFFL;
        long s = start & 0xFFFFFFFFL;
        if (s > e) {
            return Integer.MAX_VALUE;
        }
        return e - s;
    }

    /** Test-only reset (real clock back). */
    static void resetForTests() {
        clock = System::currentTimeMillis;
    }
}
