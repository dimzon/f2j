package org.fallout2ce.platform.headless;

import java.util.ArrayList;
import java.util.List;

import org.fallout2ce.spi.PlatformPlugin;
import org.fallout2ce.spi.Sleeper;
import org.fallout2ce.spi.SystemClock;
import org.fallout2ce.spi.TickerRegistry;
import org.fallout2ce.spi.VideoBackend;

/**
 * Default managed-only platform provider ({@code -Df2j.platform=headless}).
 *
 * <p>Null video (calls are accepted and dropped, like the engine's unlinked
 * SDL surface), wall clock, thread sleep, in-memory ticker list. Uses nothing
 * beyond the JRE: this provider (and the engine core) never touches
 * third-party native libraries.
 */
public final class HeadlessPlatformPlugin implements PlatformPlugin {

    public static final String NAME = "headless";

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

    private final HeadlessTickers tickers = new HeadlessTickers();

    @Override
    public String name() {
        return NAME;
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

    /** In-memory ticker registry with an explicit pump for tests/tools. */
    public static final class HeadlessTickers implements TickerRegistry {
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

        /** Run every registered ticker once, in registration order. */
        public void runAll() {
            List<Runnable> snapshot;
            synchronized (this) {
                snapshot = new ArrayList<>(registered);
            }
            snapshot.forEach(Runnable::run);
        }

        synchronized int size() {
            return registered.size();
        }
    }
}
