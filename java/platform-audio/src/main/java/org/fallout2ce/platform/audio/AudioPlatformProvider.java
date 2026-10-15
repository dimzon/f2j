package org.fallout2ce.platform.audio;

import java.util.ArrayList;
import java.util.List;

import org.fallout2ce.spi.AudioBackend;
import org.fallout2ce.spi.PlatformPlugin;
import org.fallout2ce.spi.Sleeper;
import org.fallout2ce.spi.SystemClock;
import org.fallout2ce.spi.TickerRegistry;
import org.fallout2ce.spi.VideoBackend;

/**
 * Managed audio provider ({@code -Df2j.platform=audio}): real
 * {@code javax.sound.sampled} playback with silent degradation, no window.
 */
public final class AudioPlatformProvider implements PlatformPlugin {

    public static final String NAME = "audio";

    private final SampledAudioBackend audio = new SampledAudioBackend();

    private final VideoBackend video = new VideoBackend() {
        @Override
        public void setPalette(byte[] rgb768) {
        }

        @Override
        public void setPaletteEntries(byte[] rgb, int start, int count) {
        }

        @Override
        public void present() {
        }
    };

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

    /** The working sampled backend (also reachable standalone). */
    @Override
    public AudioBackend audio() {
        return audio;
    }

    @Override
    public VideoBackend video() {
        return video;
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
