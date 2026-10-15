package org.fallout2ce.spi;

/** Sleep facility (C {@code SDL_Delay}). */
public interface Sleeper {

    /** Sleep at least {@code ms} milliseconds ({@code ms} is always positive). */
    void sleep(long ms);
}
