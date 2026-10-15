package org.fallout2ce.spi;

/**
 * Millisecond clock (C {@code SDL_GetTicks}).
 *
 * <p>Values are 32-bit unsigned milliseconds; only differences are
 * meaningful (see {@code Ticks} in the engine).
 */
public interface SystemClock {

    /** Milliseconds (masked to 32 bits by the caller). */
    long millis();
}
