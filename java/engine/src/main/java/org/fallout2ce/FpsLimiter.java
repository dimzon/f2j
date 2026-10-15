package org.fallout2ce;

/**
 * Port of {@code src/fps_limiter.h} / {@code src/fps_limiter.cc}.
 *
 * <p>Time comes from {@link Ticks} (32-bit unsigned milliseconds, as
 * {@code SDL_GetTicks}); sleeping goes through {@link Delay.Sleeper} so
 * tests assert exact frame budgets without waiting.
 */
public final class FpsLimiter {

    private final int fps;
    private long ticks;

    /** Sleep seam (C {@code SDL_Delay} inside {@code throttle}). */
    public static Delay.Sleeper sleeper = Delay.sleeper;

    /** Port of {@code FpsLimiter(fps)}. */
    public FpsLimiter(int fps) {
        this.fps = fps;
        this.ticks = 0;
    }

    /** Port of {@code FpsLimiter()} (default 60 fps). */
    public FpsLimiter() {
        this(60);
    }

    /** Port of {@code mark}. */
    public void mark() {
        ticks = Ticks.getTicks();
    }

    /**
     * Port of {@code throttle}. Sleeps the remainder of the frame, if any.
     * Differences are 32-bit unsigned, exactly like the C subtraction.
     */
    public void throttle() {
        long frameMs = 1000 / fps;
        long elapsed = Ticks.getTicks() - ticks & 0xFFFFFFFFL;
        if (frameMs > elapsed) {
            sleeper.sleep(frameMs - elapsed);
        }
    }

    /** Test-only reset (real sleep back). */
    static void resetForTests() {
        sleeper = Delay.sleeper;
    }
}
