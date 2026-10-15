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

    /**
     * Sound backend. Defaults to silent (providers without audio inherit
     * this); working providers override it.
     */
    default AudioBackend audio() {
        return new AudioBackend() {
            private float volume = 1.0f;

            @Override
            public void playOneShot(byte[] wavData) {
            }

            @Override
            public MusicHandle playMusic(byte[] wavData) {
                return () -> {
                };
            }

            @Override
            public void stopAll() {
            }

            @Override
            public void setVolume(float volume) {
                this.volume = Math.min(1.0f, Math.max(0.0f, volume));
            }

            @Override
            public float volume() {
                return volume;
            }
        };
    }
}
