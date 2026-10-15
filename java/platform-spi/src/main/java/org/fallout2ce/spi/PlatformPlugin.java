package org.fallout2ce.spi;

/**
 * Platform backend plugin, discovered via {@link java.util.ServiceLoader}.
 *
 * <p>Implementations that need third-party native libraries (SDL/OpenGL)
 * live in their own Maven modules and are never on the default classpath:
 * out of the box only managed (JRE-only) providers resolve.
 */
public interface PlatformPlugin {

    /** Unique provider name, selected with {@code -Df2j.platform=<name>}. */
    String name();

    VideoBackend video();

    SystemClock clock();

    Sleeper sleeper();

    TickerRegistry tickers();
}
