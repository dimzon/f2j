package org.fallout2ce.platform.safe;

import java.util.ArrayList;
import java.util.List;

import org.fallout2ce.platform.audio.SampledAudioBackend;
import org.fallout2ce.platform.awt.AwbVideoBackend;
import org.fallout2ce.spi.AudioBackend;
import org.fallout2ce.spi.PlatformPlugin;
import org.fallout2ce.spi.Sleeper;
import org.fallout2ce.spi.SystemClock;
import org.fallout2ce.spi.TickerRegistry;
import org.fallout2ce.spi.VideoBackend;

/**
 * Default working managed-only platform ({@code safe}).
 *
 * <p>Composes the safe implementations: AWT display (window only where a
 * display exists), sampled audio (silent where no mixer exists), system
 * clock/sleep, in-memory tickers. JRE only — no SDL, DirectX or other
 * third-party native bindings. {@code Platform.load()} prefers this
 * provider; {@code headless} stays available for silent/test setups.
 */
public final class SafePlatformProvider implements PlatformPlugin {

    public static final String NAME = "safe";

    private final AwbVideoBackend video = new AwbVideoBackend();
    private final SampledAudioBackend audio = new SampledAudioBackend();

    private final SystemClock clock = System::currentTimeMillis;

    private final Sleeper sleeper = ms -> {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    };

    private final TickerRegistry tickers = new TickerRegistry() {
        private final List<Runnable> registered = new ArrayList<>();

        @Override
        public synchronized void add(Runnable ticker) {
            if (!registered.contains(ticker)) {
                registered.add(ticker);
            }
        }

        @Override
        public synchronized void remove(Runnable ticker) {
            registered.remove(ticker);
        }
    };

    @Override
    public String name() {
        return NAME;
    }

    /** Direct access to the display backend (framebuffer, window). */
    public AwbVideoBackend display() {
        return video;
    }

    /** Direct access to the sound backend. */
    public SampledAudioBackend sound() {
        return audio;
    }

    @Override
    public VideoBackend video() {
        return video;
    }

    @Override
    public AudioBackend audio() {
        return audio;
    }

    @Override
    public SystemClock clock() {
        return clock;
    }

    @Override
    public Sleeper sleeper() {
        return sleeper;
    }

    @Override
    public TickerRegistry tickers() {
        return tickers;
    }
}
