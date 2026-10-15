package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import org.fallout2ce.spi.VideoBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Mode math, blits and palette paths. Blit/zero/palette vectors come from
 * {@code docker/refharness/golden/v16.txt} ({@code [svga]}).
 */
class SvgaTest {

    static class FakeDisplay implements VideoBackend {
        int w;
        int h;
        byte[] fb = new byte[0];
        final byte[] palette = new byte[768];
        final List<String> calls = new ArrayList<>();
        String title;
        boolean exclusive;

        FakeDisplay() {
            for (int i = 0; i < 256; i++) {
                palette[i * 3] = (byte) i;
                palette[i * 3 + 1] = (byte) i;
                palette[i * 3 + 2] = (byte) i;
            }
        }

        @Override
        public void setPalette(byte[] rgb768) {
            System.arraycopy(rgb768, 0, palette, 0, 768);
        }

        @Override
        public void setPaletteEntries(byte[] rgb, int start, int count) {
            System.arraycopy(rgb, 0, palette, start * 3, count * 3);
        }

        @Override
        public void present() {
            calls.add("present");
        }

        @Override
        public void openWindow(String title) {
            this.title = title;
            calls.add("open");
        }

        @Override
        public void resize(int width, int height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("size: " + width + "x" + height);
            }
            w = width;
            h = height;
            fb = new byte[width * height];
            calls.add("resize=" + width + "x" + height);
        }

        @Override
        public byte[] framebuffer() {
            return fb;
        }

        @Override
        public byte[] palette() {
            return palette.clone();
        }

        @Override
        public void freeSurface() {
            Arrays.fill(palette, (byte) 0);
            Arrays.fill(fb, (byte) 0);
        }

        @Override
        public boolean isExclusiveFullscreen() {
            return exclusive;
        }
    }

    private FakeDisplay display;

    private int savedResX;
    private int savedResY;
    private int savedScale;
    private int savedWindowed;
    private boolean savedIfaceBar;

    @BeforeEach
    void saveSettings() {
        savedResX = Settings.settings.screen.resolution_x;
        savedResY = Settings.settings.screen.resolution_y;
        savedScale = Settings.settings.screen.scale;
        savedWindowed = Settings.settings.screen.windowed;
        savedIfaceBar = Settings.settings.ui.iface_bar_mode;
    }

    @AfterEach
    void reset() {
        Settings.settings.screen.resolution_x = savedResX;
        Settings.settings.screen.resolution_y = savedResY;
        Settings.settings.screen.scale = savedScale;
        Settings.settings.screen.windowed = savedWindowed;
        Settings.settings.ui.iface_bar_mode = savedIfaceBar;
        Svga.resetForTests();
        Platform.resetForTests();
    }

    void settings(int x, int y, int scale) {
        Settings.settings.screen.resolution_x = x;
        Settings.settings.screen.resolution_y = y;
        Settings.settings.screen.scale = scale;
        Settings.settings.screen.windowed = Svga.WINDOWMODE_WINDOWED;
        Settings.settings.ui.iface_bar_mode = false;
    }

    void install() {
        display = new FakeDisplay();
        Color.video = display;
    }

    static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    @Test
    void hooksPreset() {
        install();
        assertNotNull(Svga.scrBlit);
        assertNull(Svga.zeroMem);
        Svga.zeroVidMem();
        assertTrue(display.calls.isEmpty());
    }

    @Test
    void mode640480() {
        install();
        settings(640, 480, 1);

        assertEquals(0, Svga.initMode640x480());

        assertEquals(0, Svga.scrSize.left);
        assertEquals(0, Svga.scrSize.top);
        assertEquals(639, Svga.scrSize.right);
        assertEquals(479, Svga.scrSize.bottom);
        assertEquals(640, display.w);
        assertEquals(480, display.h);
        assertEquals("", display.title);
        assertNotNull(Svga.scrBlit);
        assertNotNull(Svga.zeroMem);
        assertEquals(640, Svga.screenGetWidth());
        assertEquals(480, Svga.screenGetHeight());
        assertEquals(380, Svga.screenGetVisibleHeight());
        assertEquals(false, Svga.screenIsExclusiveFullscreen());
    }

    @Test
    void modeIgnoresArguments() {
        install();
        settings(800, 600, 1);

        assertEquals(0, Svga.initMode320x200());

        assertEquals(799, Svga.scrSize.right);
        assertEquals(599, Svga.scrSize.bottom);
        assertEquals(800, display.w);
        assertEquals(600, display.h);
    }

    @Test
    void scaleMath() {
        install();
        settings(1280, 960, 2);

        assertEquals(0, Svga.gnw95InitModeEx(320, 200, 8));

        assertEquals(639, Svga.scrSize.right);
        assertEquals(479, Svga.scrSize.bottom);
        assertEquals(640, display.w);
    }

    @Test
    void scaleFallback() {
        install();
        settings(640, 480, 2);

        assertEquals(0, Svga.gnw95InitModeEx(0, 0, 8));

        assertEquals(639, Svga.scrSize.right);
        assertEquals(640, display.w);
    }

    @Test
    void mode16Fails() {
        install();
        assertEquals(-1, Svga.initMode640x480_16());
    }

    @Test
    void badSizeFails() {
        install();
        settings(0, 0, 1);

        assertEquals(-1, Svga.initMode640x480());
    }

    @Test
    void showRectMatchesGolden() {
        install();
        settings(640, 480, 1);
        assertEquals(0, Svga.initMode640x480());
        Svga.gnw95ZeroVidMem();

        byte[] src = new byte[100 * 50];
        for (int i = 0; i < src.length; i++) {
            src[i] = (byte) i;
        }
        Svga.showRect(src, 100, 0, 10, 5, 80, 40, 200, 100);

        assertEquals(0xF2E193D3L, crc32(display.fb));
        assertEquals(List.of("open", "resize=640x480", "present", "present"), display.calls);
    }

    @Test
    void zeroVidMemMatchesGolden() {
        install();
        settings(640, 480, 1);
        assertEquals(0, Svga.initMode640x480());
        Arrays.fill(display.fb, (byte) 7);

        Svga.zeroVidMem();

        assertEquals(0x82ECC096L, crc32(display.fb));
    }

    @Test
    void zeroVidMemRespectsActive() {
        install();
        settings(640, 480, 1);
        assertEquals(0, Svga.initMode640x480());
        Arrays.fill(display.fb, (byte) 7);
        Svga.programActive = () -> false;

        Svga.zeroVidMem();

        assertEquals(7, Byte.toUnsignedInt(display.fb[0]));
        assertTrue(display.calls.stream().noneMatch("present"::equals));
    }

    @Test
    void paletteRoundTripAndStaleCache() {
        install();
        settings(640, 480, 1);
        assertEquals(0, Svga.initMode640x480());

        // Grays before the first set (fresh surface, like SDL).
        byte[] grays = Svga.directDrawGetPalette();
        assertEquals(5, Byte.toUnsignedInt(grays[5 * 3]));

        Color.colorSetBrightness(1.0);
        byte[] cmap = new byte[768];
        for (int i = 0; i < 768; i++) {
            cmap[i] = (byte) ((i * 3 + 1) & 0x3F);
        }
        Color.setSystemPalette(cmap);
        assertArrayEquals(cmap, Svga.directDrawGetPalette());

        // After free the stale cache survives (C static buffer quirk).
        Svga.directDrawFree();
        assertArrayEquals(cmap, Svga.directDrawGetPalette());
    }

    @Test
    void paletteStartsZeroed() {
        install();
        assertArrayEquals(new byte[768], Svga.directDrawGetPalette());
    }

    @Test
    void exclusiveFullscreenPassthrough() {
        install();
        display.exclusive = true;
        assertTrue(Svga.screenIsExclusiveFullscreen());
    }

    @Test
    void visibleHeightWithIfaceBar() {
        install();
        settings(640, 480, 1);
        assertEquals(0, Svga.initMode640x480());
        Settings.settings.ui.iface_bar_mode = true;
        assertEquals(480, Svga.screenGetVisibleHeight());
    }

    @Test
    void handleWindowSizeChanged() {
        install();
        settings(640, 480, 1);
        assertEquals(0, Svga.initMode640x480());
        display.calls.clear();
        final int[] seams = { 0, 0 };
        Svga.movieRendererReset = () -> seams[0]++;
        Svga.mouseRefreshMapping = () -> seams[1]++;

        Svga.handleWindowSizeChanged();

        assertEquals(1, seams[0]);
        assertEquals(1, seams[1]);
        assertEquals(List.of("resize=640x480"), display.calls);
    }

    @Test
    void renderPresentOrder() {
        install();
        List<String> order = new ArrayList<>();
        Svga.movieOverlay = () -> order.add("overlay");
        Color.video = new FakeDisplay() {
            @Override
            public void present() {
                order.add("present");
            }
        };

        Svga.renderPresent();

        assertEquals(List.of("overlay", "present"), order);
    }

    @Test
    void getStartModeIsNoOp() {
        Svga.getStartMode();
    }
}
