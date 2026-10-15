package org.fallout2ce.platform.awt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.fallout2ce.spi.PlatformPlugin;
import org.junit.jupiter.api.Test;

/**
 * Pixel-exact palette expansion (mirrors {@code directDrawSetPalette}:
 * {@code component << 2}). Everything here runs headless: only the top
 * window needs a display (guarded by assumptions).
 */
class AwbVideoBackendTest {

    /** 640x480 backend with entries 0..3 = black/red/green/blue. */
    static AwbVideoBackend rgbBackend() {
        AwbVideoBackend backend = new AwbVideoBackend();
        byte[] palette = new byte[768];
        palette[3] = 63;
        palette[7] = 63;
        palette[11] = 63;
        backend.setPalette(palette);
        byte[] fb = backend.framebuffer();
        fb[0] = 0;
        fb[1] = 1;
        fb[2] = 2;
        fb[3] = 3;
        return backend;
    }

    @Test
    void serviceFileDiscoversIt() {
        List<String> names = new ArrayList<>();
        for (PlatformPlugin plugin : ServiceLoader.load(PlatformPlugin.class)) {
            names.add(plugin.name());
        }
        assertTrue(names.contains(AwbVideoBackend.NAME), "discovered: " + names);
    }

    @Test
    void providerWiresManagedBackends() {
        AwbVideoBackend.Provider provider = new AwbVideoBackend.Provider();
        assertEquals(AwbVideoBackend.NAME, provider.name());
        assertTrue(provider.display() == provider.video());
        assertTrue(provider.clock().millis() > 0);
        provider.tickers().add(() -> {
        });
    }

    @Test
    void presentExpandsPalette() {
        AwbVideoBackend backend = rgbBackend();

        backend.present();

        assertEquals(0x000000, backend.image().getRGB(0, 0) & 0xFFFFFF);
        assertEquals(0xFC0000, backend.image().getRGB(1, 0) & 0xFFFFFF);
        assertEquals(0x00FC00, backend.image().getRGB(2, 0) & 0xFFFFFF);
        assertEquals(0x0000FC, backend.image().getRGB(3, 0) & 0xFFFFFF);
        assertEquals(640, backend.image().getWidth());
        assertEquals(480, backend.image().getHeight());
    }

    @Test
    void signedBytesAreUnsignedIndices() {
        AwbVideoBackend backend = new AwbVideoBackend();
        byte[] palette = new byte[768];
        palette[255 * 3] = 63;
        palette[255 * 3 + 1] = 63;
        palette[255 * 3 + 2] = 63;
        backend.setPalette(palette);
        backend.framebuffer()[0] = (byte) 255;

        backend.present();

        assertEquals(0xFCFCFC, backend.image().getRGB(0, 0) & 0xFFFFFF);
    }

    @Test
    void entriesUpdateRangeOnly() {
        AwbVideoBackend backend = rgbBackend();
        backend.setPaletteEntries(new byte[] { 0, 0, 63 }, 3, 1);

        backend.present();

        // Entry 3 was blue, now it is white-ish; entry 1 stays red.
        assertEquals(0x0000FC, backend.image().getRGB(3, 0) & 0xFFFFFF);
        assertEquals(0xFC0000, backend.image().getRGB(1, 0) & 0xFFFFFF);
    }

    @Test
    void freshPaletteIsGrayscale() {
        AwbVideoBackend backend = new AwbVideoBackend();

        // Identity gray ramp, like a fresh SDL surface (directDrawInit).
        assertEquals(5, Byte.toUnsignedInt(backend.palette()[5 * 3]));
        assertEquals(5, Byte.toUnsignedInt(backend.palette()[5 * 3 + 1]));
        assertEquals(5, Byte.toUnsignedInt(backend.palette()[5 * 3 + 2]));
        backend.framebuffer()[0] = 5;
        backend.present();
        assertEquals(0x141414, backend.image().getRGB(0, 0) & 0xFFFFFF);

        backend.freeSurface();
        assertEquals(0, Byte.toUnsignedInt(backend.palette()[5 * 3]));
        backend.present();
        assertEquals(0x000000, backend.image().getRGB(0, 0) & 0xFFFFFF);
    }

    @Test
    void badPaletteSizesThrow() {
        AwbVideoBackend backend = new AwbVideoBackend();
        assertThrows(IllegalArgumentException.class, () -> backend.setPalette(new byte[767]));
        assertThrows(IllegalArgumentException.class,
            () -> backend.setPaletteEntries(new byte[3], 255, 2));
        assertThrows(IllegalArgumentException.class,
            () -> backend.setPaletteEntries(new byte[3], -1, 1));
    }

    @Test
    void resizeClearsFramebuffer() {
        AwbVideoBackend backend = rgbBackend();

        backend.resize(320, 200);

        assertEquals(320, backend.width());
        assertEquals(200, backend.height());
        assertEquals(320 * 200, backend.framebuffer().length);
        backend.present();
        assertEquals(0x000000, backend.image().getRGB(0, 0) & 0xFFFFFF);
        assertEquals(320, backend.image().getWidth());
        assertEquals(200, backend.image().getHeight());
        assertThrows(IllegalArgumentException.class, () -> backend.resize(0, 200));
    }

    @Test
    void windowStaysShutHeadless() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AwbVideoBackend backend = rgbBackend();

        backend.openWindow("f2j-test");
        try {
            assertTrue(backend.isWindowOpen());
            backend.present();
        } finally {
            backend.closeWindow();
        }
        assertFalse(backend.isWindowOpen());
    }

    @Test
    void openWindowIsSafeHeadless() {
        // Must never throw: present() works with or without a display.
        AwbVideoBackend backend = rgbBackend();
        backend.openWindow("f2j-test");
        backend.present();
        assertEquals(0xFC0000, backend.image().getRGB(1, 0) & 0xFFFFFF);
        backend.closeWindow();
    }

    @Test
    void toStringNamesSize() {
        assertEquals("AwbVideoBackend[640x480]", new AwbVideoBackend().toString());
    }

    static java.awt.event.MouseEvent mouseMove(java.awt.Component target, int x, int y) {
        return new java.awt.event.MouseEvent(target, java.awt.event.MouseEvent.MOUSE_MOVED,
            System.currentTimeMillis(), 0, x, y, 0, false);
    }

    static java.awt.event.MouseEvent mouseButton(java.awt.Component target, int id, int x, int y,
            int button) {
        return new java.awt.event.MouseEvent(target, id, System.currentTimeMillis(), 0, x, y, 1,
            false, button);
    }

    @Test
    void mouseTracking() {
        AwbVideoBackend backend = new AwbVideoBackend();
        assertNotNull(backend.component());

        backend.component().dispatchEvent(mouseMove(backend.component(), 120, 30));
        assertEquals(120, backend.mouseX());
        assertEquals(30, backend.mouseY());
        assertFalse(backend.mouseLeft());
        assertFalse(backend.mouseRight());

        backend.component().dispatchEvent(mouseButton(backend.component(),
            java.awt.event.MouseEvent.MOUSE_PRESSED, 120, 30,
            java.awt.event.MouseEvent.BUTTON1));
        assertTrue(backend.mouseLeft());
        backend.component().dispatchEvent(mouseButton(backend.component(),
            java.awt.event.MouseEvent.MOUSE_RELEASED, 120, 30,
            java.awt.event.MouseEvent.BUTTON1));
        assertFalse(backend.mouseLeft());

        backend.component().dispatchEvent(mouseButton(backend.component(),
            java.awt.event.MouseEvent.MOUSE_PRESSED, 5, 5,
            java.awt.event.MouseEvent.BUTTON3));
        assertTrue(backend.mouseRight());
    }

    @Test
    void wheelDrains() {
        AwbVideoBackend backend = new AwbVideoBackend();
        assertEquals(0, backend.takeWheel()[0]);
        backend.component().dispatchEvent(new java.awt.event.MouseWheelEvent(backend.component(),
            java.awt.event.MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, 10, 10, 0, false,
            java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 3));

        int[] deltas = backend.takeWheel();
        assertEquals(0, deltas[0]);
        assertEquals(3, deltas[1]);
        assertEquals(0, backend.takeWheel()[1]);
    }
}
