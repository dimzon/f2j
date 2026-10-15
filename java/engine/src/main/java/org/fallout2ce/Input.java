package org.fallout2ce;

/**
 * Port of the portable core of {@code src/input.h} / {@code src/input.cc}:
 * ticker registry + pump, input event queue, screenshots, init/exit and the
 * SDL message pump.
 *
 * <p>Not ported yet (need buttons/menus/movie/sfall/game): {@code
 * inputGetInput}, {@code _process_bk}, the click hook, text input.
 * Tick times come from {@link Ticks} (C {@code SDL_GetTicks}).
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

    /** Port of {@code tickersAdd} (removed entries re-arm, active ones duplicate). */
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
        programIsActive = false;
        keyboardKeyRepeatRate = 80;
        keyboardKeyRepeatDelay = 500;
        java.util.Arrays.fill(normalizedQwertyKeys, 0);
        platformEvents.clear();
        sfallConsumeSyntheticKey = (key, down) -> false;
        sfallHandleKeyPressed = (key, down) -> -1;
        movieFocusGained = () -> {
        };
        audioResume = () -> {
        };
        audioPause = () -> {
        };
        quitHandler = () -> System.exit(0);
        textInputBegin = () -> {
        };
        textInputEnd = () -> {
        };
    }

    // ===== input init / message pump (v38 slice; inputGetInput with v39) =====

    /** Port of {@code gProgramIsActive} (win32.cc, default false). */
    public static boolean programIsActive;

    private static int keyboardKeyRepeatRate = 80;
    private static int keyboardKeyRepeatDelay = 500;

    /** Test seam: key repeat rate/rate-delay (no C++ setters exist). */
    static void setKeyboardKeyRepeatForTests(int rate, int delay) {
        keyboardKeyRepeatRate = rate;
        keyboardKeyRepeatDelay = delay;
    }

    /** SDL scancodes normalized for QWERTY (C {@code gNormalizedQwertyKeys}). */
    private static final int[] normalizedQwertyKeys = new int[SdlScancode.NUM_SCANCODES];

    /** Test seam: normalized scancode lookup. */
    static int normalizedKey(int scancode) {
        return normalizedQwertyKeys[scancode];
    }

    /** Port of {@code inputInit} (0x4C8A70). */
    public static int inputInit() {
        if (!DInput.directInputInit()) {
            return -1;
        }

        if (Kb.keyboardInit() == -1) {
            return -1;
        }

        if (Mouse.mouseInit() == -1) {
            return -1;
        }

        if (gnw95InputInit() == -1) {
            return -1;
        }

        buildNormalizedQwertyKeys();
        gnw95ClearTimeStamps();

        queueWriteIndex = 0;
        queueReadIndex = -1;
        inputMx = -1;
        inputMy = -1;
        runLoopDisabled = false;
        screenshotHandler = Input::screenshotHandlerDefaultImpl;
        tickerHead = null;
        screenshotKeyCode = SCREENSHOT_KEY_ALT_C;

        return 0;
    }

    /** Port of {@code inputExit} (0x4C8B40). */
    public static void inputExit() {
        gnw95InputInit();
        Mouse.mouseFree();
        Kb.keyboardFree();
        DInput.directInputFree();

        tickerHead = null;
    }

    /** Port of {@code _GNW95_input_init} (0x4C9C20; always ok). */
    private static int gnw95InputInit() {
        return 0;
    }

    /** Port of {@code buildNormalizedQwertyKeys} (0x4C9490). */
    static void buildNormalizedQwertyKeys() {
        int[] keys = normalizedQwertyKeys;
        int k;
        int layout = Kb.keyboardGetLayout();

        keys[SdlScancode.ESCAPE] = SdlScancode.ESCAPE;
        keys[SdlScancode.N1] = SdlScancode.N1;
        keys[SdlScancode.N2] = SdlScancode.N2;
        keys[SdlScancode.N3] = SdlScancode.N3;
        keys[SdlScancode.N4] = SdlScancode.N4;
        keys[SdlScancode.N5] = SdlScancode.N5;
        keys[SdlScancode.N6] = SdlScancode.N6;
        keys[SdlScancode.N7] = SdlScancode.N7;
        keys[SdlScancode.N8] = SdlScancode.N8;
        keys[SdlScancode.N9] = SdlScancode.N9;
        keys[SdlScancode.N0] = SdlScancode.N0;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.MINUS;
            break;
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N6;
            break;
        default:
            k = SdlScancode.SLASH;
            break;
        }
        keys[SdlScancode.MINUS] = k;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N0;
            break;
        default:
            k = SdlScancode.EQUALS;
            break;
        }
        keys[SdlScancode.EQUALS] = k;

        keys[SdlScancode.BACKSPACE] = SdlScancode.BACKSPACE;
        keys[SdlScancode.TAB] = SdlScancode.TAB;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.A;
            break;
        default:
            k = SdlScancode.Q;
            break;
        }
        keys[SdlScancode.Q] = k;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.Z;
            break;
        default:
            k = SdlScancode.W;
            break;
        }
        keys[SdlScancode.W] = k;

        keys[SdlScancode.E] = SdlScancode.E;
        keys[SdlScancode.R] = SdlScancode.R;
        keys[SdlScancode.T] = SdlScancode.T;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_QWERTY:
        case Kb.KEYBOARD_LAYOUT_FRENCH:
        case Kb.KEYBOARD_LAYOUT_ITALIAN:
        case Kb.KEYBOARD_LAYOUT_SPANISH:
            k = SdlScancode.Y;
            break;
        default:
            k = SdlScancode.Z;
            break;
        }
        keys[SdlScancode.Y] = k;

        keys[SdlScancode.U] = SdlScancode.U;
        keys[SdlScancode.I] = SdlScancode.I;
        keys[SdlScancode.O] = SdlScancode.O;
        keys[SdlScancode.P] = SdlScancode.P;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_QWERTY:
        case Kb.KEYBOARD_LAYOUT_ITALIAN:
        case Kb.KEYBOARD_LAYOUT_SPANISH:
            k = SdlScancode.LEFTBRACKET;
            break;
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N5;
            break;
        default:
            k = SdlScancode.N8;
            break;
        }
        keys[SdlScancode.LEFTBRACKET] = k;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_QWERTY:
        case Kb.KEYBOARD_LAYOUT_ITALIAN:
        case Kb.KEYBOARD_LAYOUT_SPANISH:
            k = SdlScancode.RIGHTBRACKET;
            break;
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.MINUS;
            break;
        default:
            k = SdlScancode.N9;
            break;
        }
        keys[SdlScancode.RIGHTBRACKET] = k;

        keys[SdlScancode.RETURN] = SdlScancode.RETURN;
        keys[SdlScancode.LCTRL] = SdlScancode.LCTRL;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.Q;
            break;
        default:
            k = SdlScancode.A;
            break;
        }
        keys[SdlScancode.A] = k;

        keys[SdlScancode.S] = SdlScancode.S;
        keys[SdlScancode.D] = SdlScancode.D;
        keys[SdlScancode.F] = SdlScancode.F;
        keys[SdlScancode.G] = SdlScancode.G;
        keys[SdlScancode.H] = SdlScancode.H;
        keys[SdlScancode.J] = SdlScancode.J;
        keys[SdlScancode.K] = SdlScancode.K;
        keys[SdlScancode.L] = SdlScancode.L;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.SEMICOLON;
            break;
        default:
            k = SdlScancode.COMMA;
            break;
        }
        keys[SdlScancode.SEMICOLON] = k;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.APOSTROPHE;
            break;
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N4;
            break;
        default:
            k = SdlScancode.MINUS;
            break;
        }
        keys[SdlScancode.APOSTROPHE] = k;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.GRAVE;
            break;
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N2;
            break;
        case Kb.KEYBOARD_LAYOUT_ITALIAN:
        case Kb.KEYBOARD_LAYOUT_SPANISH:
            k = 0;
            break;
        default:
            k = SdlScancode.RIGHTBRACKET;
            break;
        }
        keys[SdlScancode.GRAVE] = k;

        keys[SdlScancode.LSHIFT] = SdlScancode.LSHIFT;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.BACKSLASH;
            break;
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N8;
            break;
        case Kb.KEYBOARD_LAYOUT_ITALIAN:
        case Kb.KEYBOARD_LAYOUT_SPANISH:
            k = SdlScancode.GRAVE;
            break;
        default:
            k = SdlScancode.Y;
            break;
        }
        keys[SdlScancode.BACKSLASH] = k;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_QWERTY:
        case Kb.KEYBOARD_LAYOUT_ITALIAN:
        case Kb.KEYBOARD_LAYOUT_SPANISH:
            k = SdlScancode.Z;
            break;
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.W;
            break;
        default:
            k = SdlScancode.Y;
            break;
        }
        keys[SdlScancode.Z] = k;

        keys[SdlScancode.X] = SdlScancode.X;
        keys[SdlScancode.C] = SdlScancode.C;
        keys[SdlScancode.V] = SdlScancode.V;
        keys[SdlScancode.B] = SdlScancode.B;
        keys[SdlScancode.N] = SdlScancode.N;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.SEMICOLON;
            break;
        default:
            k = SdlScancode.M;
            break;
        }
        keys[SdlScancode.M] = k;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.M;
            break;
        default:
            k = SdlScancode.COMMA;
            break;
        }
        keys[SdlScancode.COMMA] = k;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.COMMA;
            break;
        default:
            k = SdlScancode.PERIOD;
            break;
        }
        keys[SdlScancode.PERIOD] = k;

        switch (layout) {
        case Kb.KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.SLASH;
            break;
        case Kb.KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.PERIOD;
            break;
        default:
            k = SdlScancode.N7;
            break;
        }
        keys[SdlScancode.SLASH] = k;

        keys[SdlScancode.RSHIFT] = SdlScancode.RSHIFT;
        keys[SdlScancode.KP_MULTIPLY] = SdlScancode.KP_MULTIPLY;
        keys[SdlScancode.SPACE] = SdlScancode.SPACE;
        keys[SdlScancode.LALT] = SdlScancode.LALT;
        keys[SdlScancode.CAPSLOCK] = SdlScancode.CAPSLOCK;
        keys[SdlScancode.F1] = SdlScancode.F1;
        keys[SdlScancode.F2] = SdlScancode.F2;
        keys[SdlScancode.F3] = SdlScancode.F3;
        keys[SdlScancode.F4] = SdlScancode.F4;
        keys[SdlScancode.F5] = SdlScancode.F5;
        keys[SdlScancode.F6] = SdlScancode.F6;
        keys[SdlScancode.F7] = SdlScancode.F7;
        keys[SdlScancode.F8] = SdlScancode.F8;
        keys[SdlScancode.F9] = SdlScancode.F9;
        keys[SdlScancode.F10] = SdlScancode.F10;
        keys[SdlScancode.NUMLOCKCLEAR] = SdlScancode.NUMLOCKCLEAR;
        keys[SdlScancode.SCROLLLOCK] = SdlScancode.SCROLLLOCK;
        keys[SdlScancode.KP_7] = SdlScancode.KP_7;
        keys[SdlScancode.KP_9] = SdlScancode.KP_9;
        keys[SdlScancode.KP_8] = SdlScancode.KP_8;
        keys[SdlScancode.KP_MINUS] = SdlScancode.KP_MINUS;
        keys[SdlScancode.KP_4] = SdlScancode.KP_4;
        keys[SdlScancode.KP_5] = SdlScancode.KP_5;
        keys[SdlScancode.KP_6] = SdlScancode.KP_6;
        keys[SdlScancode.KP_PLUS] = SdlScancode.KP_PLUS;
        keys[SdlScancode.KP_1] = SdlScancode.KP_1;
        keys[SdlScancode.KP_2] = SdlScancode.KP_2;
        keys[SdlScancode.KP_3] = SdlScancode.KP_3;
        keys[SdlScancode.KP_0] = SdlScancode.KP_0;
        keys[SdlScancode.KP_DECIMAL] = SdlScancode.KP_DECIMAL;
        keys[SdlScancode.F11] = SdlScancode.F11;
        keys[SdlScancode.F12] = SdlScancode.F12;
        keys[SdlScancode.F13] = -1;
        keys[SdlScancode.F14] = -1;
        keys[SdlScancode.F15] = -1;
        keys[SdlScancode.KP_EQUALS] = -1;
        keys[SdlScancode.STOP] = -1;
        keys[SdlScancode.KP_ENTER] = SdlScancode.KP_ENTER;
        keys[SdlScancode.RCTRL] = SdlScancode.RCTRL;
        keys[SdlScancode.KP_COMMA] = -1;
        keys[SdlScancode.KP_DIVIDE] = SdlScancode.KP_DIVIDE;
        keys[SdlScancode.RALT] = SdlScancode.RALT;
        keys[SdlScancode.HOME] = SdlScancode.HOME;
        keys[SdlScancode.UP] = SdlScancode.UP;
        keys[SdlScancode.PRIOR] = SdlScancode.PRIOR;
        keys[SdlScancode.LEFT] = SdlScancode.LEFT;
        keys[SdlScancode.RIGHT] = SdlScancode.RIGHT;
        keys[SdlScancode.END] = SdlScancode.END;
        keys[SdlScancode.DOWN] = SdlScancode.DOWN;
        keys[SdlScancode.PAGEUP] = SdlScancode.PAGEUP;
        keys[SdlScancode.PAGEDOWN] = SdlScancode.PAGEDOWN;
        keys[SdlScancode.INSERT] = SdlScancode.INSERT;
        keys[SdlScancode.DELETE] = SdlScancode.DELETE;
        keys[SdlScancode.LGUI] = -1;
        keys[SdlScancode.RGUI] = -1;
        keys[SdlScancode.APPLICATION] = -1;
    }

    /** Port of {@code _GNW95_process_key} (0x4C9E14). */
    static void gnw95ProcessKey(int scancode, boolean down) {
        // Use originally pressed scancode, not qwerty-remapped one, for tracking
        // timestamps, see usage from |_GNW95_process_message|.
        if (scancode < 0 || scancode >= SdlScancode.NUM_SCANCODES) {
            return;
        }

        int normalized = normalizedQwertyKeys[scancode];

        Gnw95KeyTimeStamp stamp = gnw95KeyTimeStamps[scancode];
        if (down) {
            stamp.tick = (int) Ticks.getTicks();
            stamp.repeatCount = 0;
        } else {
            stamp.tick = -1;
        }

        // Ignore keys which were remapped to -1.
        if (normalized == -1) {
            return;
        }

        Kb.kbSimulateKey(new DInput.KeyboardData(normalized, down));
    }

    /** Platform event for the message pump (C {@code SDL_Event} cases). */
    public sealed interface PlatformEvent
        permits PlatformEvent.KeyEvent, PlatformEvent.FingerEvent, PlatformEvent.WindowEvent,
            PlatformEvent.QuitEvent {

        /** Port of {@code SDL_KEYDOWN/SDL_KEYUP} (scancode + state + repeat). */
        record KeyEvent(int scancode, boolean down, boolean repeat) implements PlatformEvent {
        }

        /** Port of {@code SDL_FINGERDOWN/MOTION/UP} (0..1 coords, ms stamp). */
        record FingerEvent(long fingerId, int timestamp, float x, float y, int phase)
            implements PlatformEvent {
        }

        /** Port of {@code SDL_WINDOWEVENT} cases. */
        record WindowEvent(int kind) implements PlatformEvent {
        }

        /** Port of {@code SDL_QUIT}. */
        record QuitEvent() implements PlatformEvent {
        }
    }

    public static final int FINGER_BEGAN = 0;
    public static final int FINGER_MOVED = 1;
    public static final int FINGER_ENDED = 2;

    public static final int WINDOW_EXPOSED = 0;
    public static final int WINDOW_SIZE_CHANGED = 1;
    public static final int WINDOW_FOCUS_GAINED = 2;
    public static final int WINDOW_FOCUS_LOST = 3;

    private static final java.util.ArrayDeque<PlatformEvent> platformEvents = new java.util.ArrayDeque<>();

    /** Backend push: key press/release (C {@code SDL_PushEvent} / pump). */
    public static void postKeyEvent(int scancode, boolean down) {
        platformEvents.add(new PlatformEvent.KeyEvent(scancode, down, false));
    }

    /** Backend push: touch finger phase. */
    public static void postFingerEvent(long fingerId, int timestamp, float x, float y, int phase) {
        platformEvents.add(new PlatformEvent.FingerEvent(fingerId, timestamp, x, y, phase));
    }

    /** Backend push: window notification. */
    public static void postWindowEvent(int kind) {
        platformEvents.add(new PlatformEvent.WindowEvent(kind));
    }

    /** Backend push: quit request. */
    public static void postQuitEvent() {
        platformEvents.add(new PlatformEvent.QuitEvent());
    }

    /** Sfall synthetic-key consume hook (default: not consumed). */
    public static java.util.function.BiFunction<Integer, Boolean, Boolean> sfallConsumeSyntheticKey =
        (key, down) -> false;

    /** Sfall key-pressed override hook (default: -1, process normally). */
    public static java.util.function.BiFunction<Integer, Boolean, Integer> sfallHandleKeyPressed =
        (key, down) -> -1;

    /** Movie focus-gained hook (movie not ported yet). */
    public static Runnable movieFocusGained = () -> {
    };

    /** Audio resume hook (audio engine not ported yet). */
    public static Runnable audioResume = () -> {
    };

    /** Audio pause hook (audio engine not ported yet). */
    public static Runnable audioPause = () -> {
    };

    /** Quit hook (C called {@code exit(EXIT_SUCCESS)}). */
    public static Runnable quitHandler = () -> System.exit(0);

    /** Text input begin hook (C called {@code SDL_StartTextInput}). */
    public static Runnable textInputBegin = () -> {
    };

    /** Text input end hook (C called {@code SDL_StopTextInput}). */
    public static Runnable textInputEnd = () -> {
    };

    /** Port of {@code _GNW95_process_message} (0x4C9CF0). */
    public static void gnw95ProcessMessage() {
        // We need to process event loop even if program is not active or keyboard
        // is disabled, because if we ignore it, we'll never be able to reactivate
        // it again.

        PlatformEvent event;
        while ((event = platformEvents.poll()) != null) {
            if (event instanceof PlatformEvent.KeyEvent keyEvent) {
                boolean syntheticSfallKey =
                    sfallConsumeSyntheticKey.apply(keyEvent.scancode(), keyEvent.down());
                if (!Kb.keyboardIsDisabled()) {
                    if (!keyEvent.repeat() && !syntheticSfallKey) {
                        int keyOverride =
                            sfallHandleKeyPressed.apply(keyEvent.scancode(), keyEvent.down());
                        if (keyOverride == SdlScancode.UNKNOWN) {
                            continue;
                        }
                        if (keyOverride != -1) {
                            gnw95ProcessKey(keyOverride, keyEvent.down());
                            continue;
                        }
                    }
                    gnw95ProcessKey(keyEvent.scancode(), keyEvent.down());
                }
            } else if (event instanceof PlatformEvent.FingerEvent fingerEvent) {
                if (fingerEvent.phase() == FINGER_BEGAN) {
                    Touch.touchHandleStart(fingerEvent.fingerId(), fingerEvent.timestamp(),
                        fingerEvent.x(), fingerEvent.y());
                } else if (fingerEvent.phase() == FINGER_MOVED) {
                    Touch.touchHandleMove(fingerEvent.fingerId(), fingerEvent.timestamp(),
                        fingerEvent.x(), fingerEvent.y());
                } else if (fingerEvent.phase() == FINGER_ENDED) {
                    Touch.touchHandleEnd(fingerEvent.fingerId(), fingerEvent.timestamp(),
                        fingerEvent.x(), fingerEvent.y());
                }
            } else if (event instanceof PlatformEvent.WindowEvent windowEvent) {
                if (windowEvent.kind() == WINDOW_EXPOSED) {
                    WindowManager.windowRefreshAll(Svga.scrSize);
                } else if (windowEvent.kind() == WINDOW_SIZE_CHANGED) {
                    Svga.handleWindowSizeChanged();
                } else if (windowEvent.kind() == WINDOW_FOCUS_GAINED) {
                    programIsActive = true;
                    movieFocusGained.run();
                    if (!DInput.mouseDeviceInitMode()) {
                        Debug.print("Failed to initialize mouse mode on focus gained\n");
                    }
                    WindowManager.windowRefreshAll(Svga.scrSize);
                    audioResume.run();
                } else if (windowEvent.kind() == WINDOW_FOCUS_LOST) {
                    programIsActive = false;
                    DInput.mouseDeviceInitMode();
                    audioPause.run();
                }
            } else if (event instanceof PlatformEvent.QuitEvent) {
                quitHandler.run();
            }
        }

        Touch.touchProcessGesture();

        if (programIsActive && !Kb.keyboardIsDisabled()) {
            // NOTE: Uninline
            int tick = (int) Ticks.getTicks();

            for (int key = 0; key < SdlScancode.NUM_SCANCODES; key++) {
                Gnw95KeyTimeStamp stamp = gnw95KeyTimeStamps[key];
                if (stamp.tick != -1) {
                    int elapsedTime = stamp.tick > tick ? Integer.MAX_VALUE : tick - stamp.tick;
                    int delay = stamp.repeatCount == 0 ? keyboardKeyRepeatDelay : keyboardKeyRepeatRate;
                    if (elapsedTime > delay) {
                        gnw95ProcessKey(key, true);

                        stamp.tick = tick;
                        stamp.repeatCount++;
                    }
                }
            }
        }
    }

    /** Port of {@code _GNW95_lost_focus} (0x4C9EEC). */
    public static void gnw95LostFocus() {
        while (!programIsActive) {
            gnw95ProcessMessage();

            Delay.delayMs(125);
        }
    }

    /** Port of {@code inputGetHookMouseButton} (SDL button to hook index). */
    static int inputGetHookMouseButton(int sdlButton) {
        switch (sdlButton) {
        case 1:
            return 0;
        case 2:
            return 1;
        case 3:
            return 2;
        case 4:
            return 3;
        case 5:
            return 4;
        default:
            if (sdlButton >= 6 && sdlButton <= 8) {
                return sdlButton - 1;
            }
            return -1;
        }
    }

    /** Port of {@code beginTextInput}. */
    public static void beginTextInput() {
        textInputBegin.run();
    }

    /** Port of {@code endTextInput}. */
    public static void endTextInput() {
        textInputEnd.run();
    }
}
