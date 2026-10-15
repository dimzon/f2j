package org.fallout2ce.platform.safe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.fallout2ce.platform.audio.SampledAudioBackend;
import org.fallout2ce.platform.awt.AwbVideoBackend;
import org.fallout2ce.spi.PlatformPlugin;
import org.junit.jupiter.api.Test;

class SafePlatformProviderTest {

    @Test
    void serviceFileDiscoversIt() {
        List<String> names = new ArrayList<>();
        for (PlatformPlugin plugin : ServiceLoader.load(PlatformPlugin.class)) {
            names.add(plugin.name());
        }
        assertTrue(names.contains(SafePlatformProvider.NAME), "discovered: " + names);
    }

    @Test
    void composesSafeBackends() {
        SafePlatformProvider provider = new SafePlatformProvider();
        assertEquals(SafePlatformProvider.NAME, provider.name());
        assertTrue(provider.display() == provider.video());
        assertTrue(provider.display() instanceof AwbVideoBackend);
        assertTrue(provider.sound() == provider.audio());
        assertTrue(provider.sound() instanceof SampledAudioBackend);
        assertTrue(provider.clock().millis() > 0);
        provider.audio().stopAll();
    }
}
