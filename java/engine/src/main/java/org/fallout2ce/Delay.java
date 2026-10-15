package org.fallout2ce;

/**
 * Port of {@code src/delay.h} / {@code src/delay.cc} ({@code delay_ms}).
 */
public final class Delay {

    private Delay() {
    }

    /** Sleep seam (C {@code SDL_Delay}). */
    public interface Sleeper {
        void sleep(long ms);
    }

    private static final Sleeper REAL_SLEEP = ms -> {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    };

    public static Sleeper sleeper = REAL_SLEEP;

    /** Port of {@code delay_ms}. */
    public static void delayMs(int ms) {
        if (ms <= 0) {
            return;
        }
        sleeper.sleep(ms);
    }

    /** Test-only reset (real sleep back). */
    static void resetForTests() {
        sleeper = REAL_SLEEP;
    }
}
