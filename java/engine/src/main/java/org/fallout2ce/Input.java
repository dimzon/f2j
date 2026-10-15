package org.fallout2ce;

/**
 * Port of the portable core of {@code src/input.h} / {@code src/input.cc}:
 * ticker registry + pump, input event queue, screenshots.
 *
 * <p>Not ported yet (need kb/mouse/movie/sfall/touch/dinput): {@code
 * inputInit/Exit/GetInput}, {@code _process_bk}, the SDL event pump,
 * key repeat, {@code inputPauseForTocks}, text input. Tick times come from
 * {@link Ticks} (C {@code SDL_GetTicks}).
 */
public final class Input {

    private Input() {
    }

    /** Queue capacity (C ring buffer of 40). */
    static final int QUEUE_CAPACITY = 40;

    /** Default screenshot key (C {@code KEY_ALT_C}, kb.h not ported yet). */
    public static final int SCREENSHOT_KEY_ALT_C = 302;

    /** Queued input event (C {@code InputEvent}). */
    public record InputEvent(int logicalKey, int mouseX, int mouseY) {
    }

    private static final class TickerNode {
        int flags;
        final Runnable proc;
        TickerNode next;

        TickerNode(Runnable proc) {
            this.proc = proc;
        }
    }

    private static TickerNode tickerHead;
    private static boolean runLoopDisabled;
    private static long tickerLastTimestamp;

    private static final InputEvent[] eventQueue = new InputEvent[QUEUE_CAPACITY];
    private static int queueReadIndex = -1;
    private static int queueWriteIndex;
    private static int inputMx = -1;
    private static int inputMy = -1;

    /** Mouse position source (C {@code mouseGetPosition}). */
    public interface MousePosition {
        int x();

        int y();
    }

    private static final MousePosition NO_MOUSE = new MousePosition() {
        @Override
        public int x() {
            return -1;
        }

        @Override
        public int y() {
            return -1;
        }
    };

    public static MousePosition mousePosition = NO_MOUSE;

    /** Screenshot capture (needs the window manager; wired later). */
    public static Runnable windowRefreshAll = () -> {
    };

    /** Screenshot output directory ({@code ""} = working directory, as in C). */
    public static String screenshotDirectory = "";

    /** Screenshot handler (C {@code ScreenshotHandler*}). */
    public interface ScreenshotHandler {
        int handle(int width, int height, byte[] data, byte[] palette);
    }

    /** Port of {@code gScreenshotKeyCode} (default: Alt+C). */
    public static int screenshotKeyCode = SCREENSHOT_KEY_ALT_C;

    /** Port of {@code gScreenshotHandler} (default: BMP writer). */
    public static ScreenshotHandler screenshotHandler = Input::screenshotHandlerDefaultImpl;

    /** Block-mouse-up flag for inventory click-through (C {@code gBlockMouseUpEvent}). */
    public static boolean blockMouseUpEvent;

    /** Port of {@code tickersAdd} (duplicates re-arm, never duplicate). */
    public static void tickersAdd(Runnable proc) {
        TickerNode curr = tickerHead;
        while (curr != null) {
            if (curr.proc == proc) {
                if ((curr.flags & 0x01) != 0) {
                    curr.flags &= ~0x01;
                    return;
                }
            }
            curr = curr.next;
        }

        TickerNode node = new TickerNode(proc);
        node.next = tickerHead;
        tickerHead = node;
    }

    /** Port of {@code tickersRemove} (lazy: unlinked on the next pump). */
    public static void tickersRemove(Runnable proc) {
        TickerNode curr = tickerHead;
        while (curr != null) {
            if (curr.proc == proc) {
                curr.flags |= 0x01;
                return;
            }
            curr = curr.next;
        }
    }

    /** Port of {@code tickersEnable}. */
    public static void tickersEnable() {
        runLoopDisabled = false;
    }

    /** Port of {@code tickersDisable}. */
    public static void tickersDisable() {
        runLoopDisabled = true;
    }

    /** Port of {@code tickersExecute}. */
    public static void tickersExecute() {
        if (runLoopDisabled) {
            return;
        }

        tickerLastTimestamp = Ticks.getTicks();

        TickerNode curr = tickerHead;
        TickerNode prev = null;

        while (curr != null) {
            TickerNode next = curr.next;
            if ((curr.flags & 1) != 0) {
                if (prev == null) {
                    tickerHead = next;
                } else {
                    prev.next = next;
                }
            } else {
                curr.proc.run();
                prev = curr;
            }
            curr = next;
        }
    }

    /** Port of {@code _get_bk_time}. */
    public static long getBkTime() {
        return tickerLastTimestamp;
    }

    /** Port of {@code RepeatInfo} (input.cc): per-scancode press timing. */
    static final class Gnw95KeyTimeStamp {
        int tick = -1;
        int repeatCount;
    }

    private static final Gnw95KeyTimeStamp[] gnw95KeyTimeStamps =
        new Gnw95KeyTimeStamp[SdlScancode.NUM_SCANCODES];

    static {
        for (int index = 0; index < gnw95KeyTimeStamps.length; index++) {
            gnw95KeyTimeStamps[index] = new Gnw95KeyTimeStamp();
        }
    }

    /** Port of {@code _GNW95_clear_time_stamps} (input.cc). */
    public static void gnw95ClearTimeStamps() {
        for (int index = 0; index < gnw95KeyTimeStamps.length; index++) {
            gnw95KeyTimeStamps[index].tick = -1;
            gnw95KeyTimeStamps[index].repeatCount = 0;
        }
    }

    /** Port of {@code enqueueInputEvent}. */
    public static void enqueueInputEvent(int logicalKey) {
        if (logicalKey == -1) {
            return;
        }

        if (logicalKey == screenshotKeyCode) {
            takeScreenshot();
            return;
        }

        if (queueWriteIndex == queueReadIndex) {
            return;
        }

        eventQueue[queueWriteIndex] =
            new InputEvent(logicalKey, mousePosition.x(), mousePosition.y());

        queueWriteIndex++;

        if (queueWriteIndex == QUEUE_CAPACITY) {
            queueWriteIndex = 0;
            return;
        }

        if (queueReadIndex == -1) {
            queueReadIndex = 0;
        }
    }

    /** Port of {@code dequeueInputEvent} (feeds {@code inputGetInput}). */
    static int dequeueInputEvent() {
        if (queueReadIndex == -1) {
            return -1;
        }

        InputEvent event = eventQueue[queueReadIndex];
        inputMx = event.mouseX();
        inputMy = event.mouseY();

        queueReadIndex++;

        if (queueReadIndex == QUEUE_CAPACITY) {
            queueReadIndex = 0;
        }

        if (queueReadIndex == queueWriteIndex) {
            queueReadIndex = -1;
            queueWriteIndex = 0;
        }

        return event.logicalKey();
    }

    /** Port of {@code inputEventQueueReset} (platform/sfall drains are seams). */
    public static void inputEventQueueReset() {
        queueReadIndex = -1;
        queueWriteIndex = 0;
        platformEventDrain.run();
        sfallClearKeys.run();
    }

    /** Native event-queue drain (C {@code SDL_PollEvent} loop). */
    public static Runnable platformEventDrain = () -> {
    };

    /** Sfall synthetic-key clear (sfall not ported yet). */
    public static Runnable sfallClearKeys = () -> {
    };

    /** Port of {@code get_input_position}. */
    public static Geometry.Point inputPosition() {
        return new Geometry.Point(inputMx, inputMy);
    }

    /** Port of {@code inputBlockForTocks}. */
    public static void inputBlockForTocks(long ms) {
        Delay.delayMs((int) ms);
    }

    /** Port of {@code screenshotHandlerConfigure} (null selects the BMP writer). */
    public static void screenshotHandlerConfigure(int keyCode, ScreenshotHandler handler) {
        screenshotKeyCode = keyCode;

        if (handler == null) {
            handler = Input::screenshotHandlerDefaultImpl;
        }

        screenshotHandler = handler;
    }

    /**
     * Port of {@code takeScreenshot} (cursor hooks arrive with the mouse
     * port; the refresh comes from the window manager).
     */
    public static void takeScreenshot() {
        int width = Svga.screenGetWidth();
        int height = Svga.screenGetHeight();
        screenshotBuffer = new byte[width * height];

        Svga.ShowRect oldBlit = Svga.scrBlit;
        Svga.scrBlit = Input::screenshotBlitter;

        windowRefreshAll.run();

        Svga.scrBlit = oldBlit;

        // Save with the palette currently presented on screen so the
        // exported colors match brightness-adjusted rendering.
        byte[] palette = Svga.directDrawGetPalette();
        screenshotHandler.handle(width, height, screenshotBuffer, palette);
        screenshotBuffer = null;
    }

    private static byte[] screenshotBuffer;

    private static void screenshotBlitter(byte[] src, int srcPitch, int unused,
            int srcX, int srcY, int width, int height, int destX, int destY) {
        int destWidth = Svga.screenGetWidth();
        for (int y = 0; y < height; y++) {
            System.arraycopy(src, (srcY + y) * srcPitch + srcX,
                screenshotBuffer, (destY + y) * destWidth + destX, width);
        }
    }

    /**
     * Port of {@code screenshotHandlerDefaultImpl}: 8-bit BMP writer.
     * Byte-identical to C (little-endian headers, BGR quads, bottom-up
     * rows); the harness golden pins the exact bytes.
     */
    public static int screenshotHandlerDefaultImpl(int width, int height, byte[] data, byte[] palette) {
        for (int index = 0; index < 100000; index++) {
            if (!screenshotExists(String.format("scr%05d.bmp", index))) {
                return writeBmp(String.format("scr%05d.bmp", index), width, height, data, palette);
            }
        }
        return -1;
    }

    /**
     * Port of {@code screenshotHandlerPngImpl} (lodepng becomes ImageIO;
     * bytes differ by encoder, pixels round-trip exactly).
     */
    public static int screenshotHandlerPngImpl(int width, int height, byte[] data, byte[] palette) {
        for (int index = 0; index < 100000; index++) {
            String name = String.format("scr%05d.png", index);
            if (!screenshotExists(name)) {
                return writePng(name, width, height, data, palette);
            }
        }
        return -1;
    }

    private static String screenshotPath(String fileName) {
        if (screenshotDirectory == null || screenshotDirectory.isEmpty()) {
            return fileName;
        }
        return screenshotDirectory + "/" + fileName;
    }

    private static boolean screenshotExists(String fileName) {
        return CompatFiles.fileExists(screenshotPath(fileName));
    }

    private static int writeBmp(String fileName, int width, int height, byte[] data, byte[] palette) {
        byte[] out = new byte[width * height + 14 + 40 + 1024];
        int pos = 0;

        pos = putInt16LE(out, pos, 0x4D42);
        pos = putInt32LE(out, pos, width * height + 14 + 40 + 1024);
        pos = putInt16LE(out, pos, 0);
        pos = putInt16LE(out, pos, 0);
        pos = putInt32LE(out, pos, 14 + 40 + 1024);
        pos = putInt32LE(out, pos, 40);
        pos = putInt32LE(out, pos, width);
        pos = putInt32LE(out, pos, height);
        pos = putInt16LE(out, pos, 1);
        pos = putInt16LE(out, pos, 8);
        pos = putInt32LE(out, pos, 0);
        pos = putInt32LE(out, pos, 0);
        pos = putInt32LE(out, pos, 0);
        pos = putInt32LE(out, pos, 0);
        pos = putInt32LE(out, pos, 0);
        pos = putInt32LE(out, pos, 0);

        for (int index = 0; index < 256; index++) {
            out[pos++] = (byte) (Byte.toUnsignedInt(palette[index * 3 + 2]) << 2);
            out[pos++] = (byte) (Byte.toUnsignedInt(palette[index * 3 + 1]) << 2);
            out[pos++] = (byte) (Byte.toUnsignedInt(palette[index * 3]) << 2);
            out[pos++] = 0;
        }

        for (int y = height - 1; y >= 0; y--) {
            System.arraycopy(data, y * width, out, pos, width);
            pos += width;
        }

        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(screenshotPath(fileName)), out);
        } catch (java.io.IOException e) {
            return -1;
        }
        return 0;
    }

    private static int putInt16LE(byte[] out, int pos, int value) {
        out[pos++] = (byte) value;
        out[pos++] = (byte) (value >> 8);
        return pos;
    }

    private static int putInt32LE(byte[] out, int pos, int value) {
        out[pos++] = (byte) value;
        out[pos++] = (byte) (value >> 8);
        out[pos++] = (byte) (value >> 16);
        out[pos++] = (byte) (value >> 24);
        return pos;
    }

    private static int writePng(String fileName, int width, int height, byte[] data, byte[] palette) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        int[] rgb = new int[width * height];
        for (int i = 0; i < rgb.length; i++) {
            int index = Byte.toUnsignedInt(data[i]) * 3;
            int r = Byte.toUnsignedInt(palette[index]) << 2;
            int g = Byte.toUnsignedInt(palette[index + 1]) << 2;
            int b = Byte.toUnsignedInt(palette[index + 2]) << 2;
            rgb[i] = r << 16 | g << 8 | b;
        }
        image.setRGB(0, 0, width, height, rgb, 0, width);
        try {
            javax.imageio.ImageIO.write(image, "png",
                java.nio.file.Paths.get(screenshotPath(fileName)).toFile());
        } catch (java.io.IOException e) {
            return -1;
        }
        return 0;
    }

    /** Test-only full reset (mirrors a fresh process). */
    static void resetForTests() {
        tickerHead = null;
        runLoopDisabled = false;
        tickerLastTimestamp = 0;
        java.util.Arrays.fill(eventQueue, null);
        queueReadIndex = -1;
        queueWriteIndex = 0;
        inputMx = -1;
        inputMy = -1;
        mousePosition = NO_MOUSE;
        windowRefreshAll = () -> {
        };
        screenshotDirectory = "";
        screenshotHandler = Input::screenshotHandlerDefaultImpl;
        screenshotKeyCode = SCREENSHOT_KEY_ALT_C;
        blockMouseUpEvent = false;
        platformEventDrain = () -> {
        };
        sfallClearKeys = () -> {
        };
    }
}
