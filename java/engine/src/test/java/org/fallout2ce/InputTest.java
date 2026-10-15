package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Ticker order/removal, event queue wraparound, screenshot writers.
 * Ticker/blitter/BMP vectors come from {@code golden/v23.txt}
 * ({@code [input]}); the PNG writer round-trips pixels (encoders differ).
 */
class InputTest {

    @AfterEach
    void reset() {
        Input.resetForTests();
        Svga.resetForTests();
        Platform.resetForTests();
        Ticks.resetForTests();
        Delay.resetForTests();
    }

    static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    @Test
    void tickersPrependAndPump() {
        List<Integer> log = new ArrayList<>();
        Runnable a = () -> log.add(1);
        Runnable b = () -> log.add(2);

        Input.tickersAdd(a);
        Input.tickersAdd(b);
        Input.tickersExecute();
        assertEquals(List.of(2, 1), log);

        // Lazy removal: skipped (and unlinked) on the next pump.
        Input.tickersRemove(a);
        log.clear();
        Input.tickersExecute();
        assertEquals(List.of(2), log);

        // Re-adding clears the flag without duplicating.
        Input.tickersAdd(a);
        log.clear();
        Input.tickersExecute();
        assertEquals(List.of(1, 2), log);

        Input.tickersDisable();
        log.clear();
        Input.tickersExecute();
        assertTrue(log.isEmpty());

        Input.tickersEnable();
        Input.tickersExecute();
        assertEquals(2, log.size());

        Input.tickersRemove(a);
        Input.tickersRemove(b);
        Input.tickersExecute();
    }

    @Test
    void tickerTimestamp() {
        long[] now = { 5000L };
        Ticks.clock = () -> now[0];
        assertEquals(0, Input.getBkTime());
        Input.tickersExecute();
        assertEquals(5000, Input.getBkTime());
        now[0] = 5025;
        Input.tickersExecute();
        assertEquals(5025, Input.getBkTime());
    }

    @Test
    void eventQueue() {
        Input.mousePosition = new Input.MousePosition() {
            @Override
            public int x() {
                return 11;
            }

            @Override
            public int y() {
                return 22;
            }
        };

        assertEquals(-1, Input.dequeueInputEvent());

        Input.enqueueInputEvent(-1);
        assertEquals(-1, Input.dequeueInputEvent());

        Input.enqueueInputEvent(41);
        Input.enqueueInputEvent(42);
        assertEquals(41, Input.dequeueInputEvent());
        assertEquals(11, Input.inputPosition().x());
        assertEquals(22, Input.inputPosition().y());
        assertEquals(42, Input.dequeueInputEvent());
        assertEquals(-1, Input.dequeueInputEvent());

        // Wraparound: 40 slots hold, the 41st is dropped.
        for (int i = 0; i < 40; i++) {
            Input.enqueueInputEvent(100 + i);
        }
        Input.enqueueInputEvent(999);
        for (int i = 0; i < 40; i++) {
            assertEquals(100 + i, Input.dequeueInputEvent());
        }
        assertEquals(-1, Input.dequeueInputEvent());

        // Reset drains.
        Input.enqueueInputEvent(7);
        int[] drains = { 0, 0 };
        Input.platformEventDrain = () -> drains[0]++;
        Input.sfallClearKeys = () -> drains[1]++;
        Input.inputEventQueueReset();
        assertEquals(-1, Input.dequeueInputEvent());
        assertEquals(1, drains[0]);
        assertEquals(1, drains[1]);
    }

    @Test
    void screenshotKeyTakesScreenshot() {
        List<String> calls = new ArrayList<>();
        Input.windowRefreshAll = () -> calls.add("refresh");
        Input.screenshotHandler = (w, h, data, palette) -> {
            calls.add("handle=" + w + "x" + h);
            return 0;
        };
        Svga.scrSize.right = 9;
        Svga.scrSize.bottom = 9;

        Input.enqueueInputEvent(Input.SCREENSHOT_KEY_ALT_C);

        assertEquals(List.of("refresh", "handle=10x10"), calls);
        // Other keys queue normally.
        Input.enqueueInputEvent(43);
        assertEquals(43, Input.dequeueInputEvent());
    }

    @Test
    void takeScreenshotPaintsThroughBlit() {
        Svga.scrSize.right = 7;
        Svga.scrSize.bottom = 5;
        byte[] seen = new byte[1];
        int[] size = new int[2];
        Input.windowRefreshAll = () -> {
            byte[] src = new byte[] { 1, 2, 3, 4, 5, 6 };
            Svga.scrBlit.show(src, 3, 0, 1, 0, 2, 2, 4, 2);
        };
        Input.screenshotHandler = (w, h, data, palette) -> {
            seen[0] = data[2 * 8 + 4];
            size[0] = w;
            size[1] = h;
            return 0;
        };

        Input.takeScreenshot();

        assertEquals(8, size[0]);
        assertEquals(6, size[1]);
        assertEquals(2, seen[0] & 0xFF);
    }

    @Test
    void bmpWriterMatchesGolden() throws Exception {
        Path dir = Files.createTempDirectory("f2j-shot");
        Input.screenshotDirectory = dir.toString();

        byte[] data = new byte[12];
        for (int i = 0; i < 12; i++) {
            data[i] = (byte) (i * 17 + 3);
        }
        byte[] palette = new byte[768];
        for (int i = 0; i < 768; i++) {
            palette[i] = (byte) ((i * 3 + 5) & 0xFF);
        }

        assertEquals(0, Input.screenshotHandlerDefaultImpl(4, 3, data, palette));

        Path shot = dir.resolve("scr00000.bmp");
        assertTrue(Files.isRegularFile(shot));
        byte[] bytes = Files.readAllBytes(shot);
        assertEquals(1090, bytes.length);
        assertEquals(0xB170652FL, crc32(bytes));

        // Second shot takes the next free index.
        assertEquals(0, Input.screenshotHandlerDefaultImpl(4, 3, data, palette));
        assertTrue(Files.isRegularFile(dir.resolve("scr00001.bmp")));
    }

    @Test
    void pngWriterRoundTrips() throws Exception {
        Path dir = Files.createTempDirectory("f2j-shot");
        Input.screenshotDirectory = dir.toString();

        byte[] data = new byte[] { 0, 1, 2, 3, 4, 5 };
        byte[] palette = new byte[768];
        for (int i = 0; i < 768; i++) {
            palette[i] = (byte) ((i * 7 + 1) & 0x3F);
        }

        assertEquals(0, Input.screenshotHandlerPngImpl(3, 2, data, palette));

        Path shot = dir.resolve("scr00000.png");
        assertTrue(Files.isRegularFile(shot));
        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(shot.toFile());
        assertEquals(3, image.getWidth());
        assertEquals(2, image.getHeight());
        for (int i = 0; i < 6; i++) {
            int index = Byte.toUnsignedInt(data[i]) * 3;
            int expected = Byte.toUnsignedInt(palette[index]) << 2 << 16
                | Byte.toUnsignedInt(palette[index + 1]) << 2 << 8
                | Byte.toUnsignedInt(palette[index + 2]) << 2;
            assertEquals(expected, image.getRGB(i % 3, i / 3) & 0xFFFFFF);
        }
    }

    @Test
    void configureHandler() {
        int[] calls = { 0 };
        Input.screenshotHandlerConfigure(400, (w, h, data, palette) -> {
            calls[0]++;
            return 0;
        });
        assertEquals(400, Input.screenshotKeyCode);
        Input.takeScreenshot();
        assertEquals(1, calls[0]);

        Input.screenshotHandlerConfigure(Input.SCREENSHOT_KEY_ALT_C, null);
        assertEquals(Input.SCREENSHOT_KEY_ALT_C, Input.screenshotKeyCode);
    }

    @Test
    void blockForTocksSleeps() {
        long[] now = { 1000L };
        Ticks.clock = () -> now[0];
        List<Long> sleeps = new ArrayList<>();
        Delay.sleeper = sleeps::add;

        Input.inputBlockForTocks(50);

        assertEquals(List.of(50L), sleeps);
    }

    @Test
    void blockMouseUpDefault() {
        assertFalse(Input.blockMouseUpEvent);
        Input.blockMouseUpEvent = true;
        Input.resetForTests();
        assertFalse(Input.blockMouseUpEvent);
    }

    @Test
    void queueResetState() {
        Input.enqueueInputEvent(1);
        Input.resetForTests();
        assertEquals(-1, Input.dequeueInputEvent());
        assertEquals(-1, Input.inputPosition().x());
        assertEquals(Input.SCREENSHOT_KEY_ALT_C, Input.screenshotKeyCode);
    }
}
