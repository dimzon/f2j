package org.fallout2ce.platform.headless;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.fallout2ce.spi.PlatformPlugin;
import org.junit.jupiter.api.Test;

class HeadlessPlatformPluginTest {

    @Test
    void serviceFileDiscoversIt() {
        List<String> names = new ArrayList<>();
        for (PlatformPlugin plugin : ServiceLoader.load(PlatformPlugin.class)) {
            names.add(plugin.name());
        }
        assertTrue(names.contains(HeadlessPlatformPlugin.NAME), "discovered: " + names);
    }

    @Test
    void nullVideoAcceptsCalls() {
        HeadlessPlatformPlugin plugin = new HeadlessPlatformPlugin();
        assertEquals(HeadlessPlatformPlugin.NAME, plugin.name());
        plugin.video().setPalette(new byte[768]);
        plugin.video().setPaletteEntries(new byte[6], 5, 2);
        plugin.video().present();
        assertTrue(plugin.clock().millis() > 0);
    }

    @Test
    void tickersPumpInOrder() {
        HeadlessPlatformPlugin plugin = new HeadlessPlatformPlugin();
        HeadlessPlatformPlugin.HeadlessTickers tickers =
                (HeadlessPlatformPlugin.HeadlessTickers) plugin.tickers();
        List<String> calls = new ArrayList<>();
        Runnable first = () -> calls.add("first");
        tickers.add(first);
        tickers.add(() -> calls.add("second"));
        tickers.add(first);
        tickers.runAll();
        assertEquals(List.of("first", "second"), calls);
        tickers.remove(first);
        tickers.runAll();
        assertEquals(List.of("first", "second", "second"), calls);
    }
}
