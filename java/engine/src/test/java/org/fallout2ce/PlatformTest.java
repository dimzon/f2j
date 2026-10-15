package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.fallout2ce.spi.PlatformPlugin;
import org.fallout2ce.spi.Sleeper;
import org.fallout2ce.spi.SystemClock;
import org.fallout2ce.spi.TickerRegistry;
import org.fallout2ce.spi.VideoBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Selection rules, seam installation and ServiceLoader wiring. */
class PlatformTest {

    static class FakePlugin implements PlatformPlugin {
        final String name;
        final VideoBackend video = new VideoBackend() {
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
        final SystemClock clock = () -> 42L;
        final Sleeper sleeper = ms -> {
        };
        final TickerRegistry tickers = new TickerRegistry() {
            @Override
            public void add(Runnable ticker) {
            }

            @Override
            public void remove(Runnable ticker) {
            }
        };

        FakePlugin(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
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

    @AfterEach
    void reset() {
        Platform.resetForTests();
        System.clearProperty(Platform.PROPERTY);
    }

    static List<PlatformPlugin> plugins(PlatformPlugin... plugins) {
        List<PlatformPlugin> list = new ArrayList<>();
        for (PlatformPlugin plugin : plugins) {
            list.add(plugin);
        }
        return list;
    }

    @Test
    void selectEmpty() {
        assertNull(Platform.select(plugins(), null));
    }

    @Test
    void selectSingle() {
        FakePlugin sdl = new FakePlugin("sdl");
        assertSame(sdl, Platform.select(plugins(sdl), null));
    }

    @Test
    void selectPrefersSafe() {
        FakePlugin headless = new FakePlugin(Platform.HEADLESS);
        FakePlugin safe = new FakePlugin(Platform.SAFE);
        FakePlugin sdl = new FakePlugin("sdl");
        assertSame(safe, Platform.select(plugins(sdl, headless, safe), null));
        assertSame(safe, Platform.select(plugins(safe, sdl), null));
    }

    @Test
    void selectPrefersHeadlessOverPlain() {
        FakePlugin headless = new FakePlugin(Platform.HEADLESS);
        FakePlugin sdl = new FakePlugin("sdl");
        assertSame(headless, Platform.select(plugins(sdl, headless), null));
        assertSame(headless, Platform.select(plugins(headless, sdl), null));
    }

    @Test
    void selectAmbiguousThrows() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> Platform.select(plugins(new FakePlugin("sdl"), new FakePlugin("gl")), null));
        assertTrue(e.getMessage().contains("sdl"));
        assertTrue(e.getMessage().contains("gl"));
    }

    @Test
    void selectExplicit() {
        FakePlugin sdl = new FakePlugin("sdl");
        FakePlugin gl = new FakePlugin("gl");
        assertSame(gl, Platform.select(plugins(sdl, gl), "gl"));
    }

    @Test
    void selectUnknownThrows() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> Platform.select(plugins(new FakePlugin("sdl")), "vulkan"));
        assertTrue(e.getMessage().contains("vulkan"));
    }

    @Test
    void installWiresSeams() {
        FakePlugin plugin = new FakePlugin("fake");

        Platform.install(plugin);

        assertSame(plugin, Platform.current());
        assertSame(plugin.video, Color.video);
        assertSame(plugin.clock, Ticks.clock);
        assertSame(plugin.sleeper, Delay.sleeper);
        assertSame(plugin.sleeper, FpsLimiter.sleeper);
        assertSame(plugin.tickers, ColorCycle.tickers);
    }

    @Test
    void resetRestoresManagedDefaults() {
        Platform.install(new FakePlugin("fake"));

        Platform.resetForTests();

        assertNull(Platform.current());
        // Built-ins accept calls without any provider installed.
        Color.video.setPalette(new byte[768]);
        Color.video.present();
        assertTrue(Ticks.getTicks() >= 0);
    }

    @Test
    void loadDiscoversSafeByDefault() {
        PlatformPlugin loaded = Platform.load();

        assertEquals(Platform.SAFE, loaded.name());
        assertSame(loaded, Platform.current());
        assertSame(loaded.video(), Color.video);
        assertSame(loaded.clock(), Ticks.clock);
    }

    @Test
    void loadHonorsExplicitHeadless() {
        System.setProperty(Platform.PROPERTY, Platform.HEADLESS);
        try {
            assertEquals(Platform.HEADLESS, Platform.load().name());
        } finally {
            System.clearProperty(Platform.PROPERTY);
        }
    }

    @Test
    void loadUnknownPropertyThrows() {
        System.setProperty(Platform.PROPERTY, "vulkan");
        try {
            assertThrows(IllegalStateException.class, Platform::load);
        } finally {
            System.clearProperty(Platform.PROPERTY);
        }
    }
}
