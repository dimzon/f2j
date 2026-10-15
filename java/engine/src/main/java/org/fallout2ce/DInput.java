package org.fallout2ce;

/**
 * Port of {@code src/dinput.h} / {@code src/dinput.cc} (mouse/keyboard
 * device layer: relative mode, wheel accumulator, window mapping).
 *
 * <p>SDL reads become seams below (absolute position, relative deltas,
 * buttons, relative-mode switch, warp, window size). Touch events are
 * mobile-only and stay unported.
 */
public final class DInput {

    private DInput() {
    }

    /** Port of {@code MouseData}. */
    public record MouseData(int x, int y, boolean left, boolean right, int wheelX, int wheelY) {
    }

    /** Port of {@code KeyboardData} (never filled in C either). */
    public record KeyboardData(int key, boolean down) {
    }

    /** Mouse hardware source (C {@code SDL_GetMouseState} / relative deltas). */
    public interface MouseDevice {
        int x();

        int y();

        int dx();

        int dy();

        boolean left();

        boolean right();
    }

    private static final MouseDevice NO_MOUSE = new MouseDevice() {
        @Override
        public int x() {
            return 0;
        }

        @Override
        public int y() {
            return 0;
        }

        @Override
        public int dx() {
            return 0;
        }

        @Override
        public int dy() {
            return 0;
        }

        @Override
        public boolean left() {
            return false;
        }

        @Override
        public boolean right() {
            return false;
        }
    };

    public static MouseDevice mouse = NO_MOUSE;

    /** Relative-mode switch (C {@code SDL_SetRelativeMouseMode}, 0 = ok). */
    public interface RelativeMode {
        boolean set(boolean relative);
    }

    /** Managed default: nothing to switch, always reported ok (like SDL). */
    public static RelativeMode relativeMode = relative -> true;

    /** Window size source (C {@code SDL_GetWindowSize}, zeros when headless). */
    public interface WindowSize {
        int width();

        int height();
    }

    public static WindowSize windowSize = new WindowSize() {
        @Override
        public int width() {
            return 0;
        }

        @Override
        public int height() {
            return 0;
        }
    };

    /** Cursor warp (C {@code SDL_WarpMouseInWindow}). */
    public interface MouseWarp {
        void warp(int x, int y);
    }

    public static MouseWarp mouseWarp = (x, y) -> {
    };

    /** Platform event pump (C {@code SDL_PumpEvents}). */
    public static Runnable eventPump = () -> {
    };

    /** Sfall synthetic-key clear (sfall not ported yet). */
    public static Runnable sfallClearKeys = () -> {
    };

    private static int wheelDeltaX;
    private static int wheelDeltaY;
    private static int mappingWindowWidth;
    private static int mappingWindowHeight;
    private static int mappingLogicalWidth;
    private static int mappingLogicalHeight;
    private static boolean relativeModeFlag;

    /** Port of {@code directInputInit}. */
    public static boolean directInputInit() {
        mouseDeviceRefreshWindowMapping();

        if (!mouseDeviceInit()) {
            Debug.print("directInputInit: mouseDeviceInit failed\n");
            directInputFree();
            return false;
        }

        if (!keyboardDeviceInit()) {
            directInputFree();
            return false;
        }

        return true;
    }

    /** Port of {@code directInputFree}. */
    public static void directInputFree() {
    }

    /** Port of {@code mouseDeviceUsesRelativeMode}. */
    public static boolean mouseDeviceUsesRelativeMode() {
        return relativeModeFlag;
    }

    /** Port of {@code mouseDeviceInitMode}. */
    public static boolean mouseDeviceInitMode() {
        // "Relative mode" means cursor position is owned by the application, and we move based on mouse deltas
        // "Absolute mode" means cursor position is controlled by the OS, and we read it directly.
        // Mouse sensitivity settings can only apply in relative mode.
        boolean wantsRelativeMode = Svga.programActive.getAsBoolean()
            && (Svga.screenIsExclusiveFullscreen() || Settings.settings.screen.mouse_lock);

        if (wantsRelativeMode) {
            relativeModeFlag = true;
            return relativeMode.set(true);
        }

        if (!relativeMode.set(false)) {
            return false;
        }

        relativeModeFlag = false;
        mouseDeviceRefreshWindowMapping();
        return true;
    }

    /** Port of {@code mouseDeviceAcquire}. */
    public static boolean mouseDeviceAcquire() {
        return true;
    }

    /** Port of {@code mouseDeviceUnacquire}. */
    public static boolean mouseDeviceUnacquire() {
        return true;
    }

    /** Port of {@code mouseDeviceGetData} (wheel drains on read, as in C). */
    public static MouseData mouseDeviceGetData() {
        // CE: this is sometimes called outside loops calling `get_input`,
        // so events might not be pumped yet (see Input.platformEventDrain).
        eventPump.run();

        int x;
        int y;
        if (mouseDeviceUsesRelativeMode()) {
            x = mouse.dx();
            y = mouse.dy();
        } else {
            x = mouse.x();
            y = mouse.y();
            int[] mapped = mapWindowToLogicalPosition(x, y);
            x = mapped[0];
            y = mapped[1];
        }

        MouseData data = new MouseData(x, y, mouse.left(), mouse.right(), wheelDeltaX, wheelDeltaY);

        wheelDeltaX = 0;
        wheelDeltaY = 0;

        return data;
    }

    /** Wheel accumulator for backends (C {@code handleMouseEvent}). */
    public static void addMouseWheel(int dx, int dy) {
        wheelDeltaX += dx;
        wheelDeltaY += dy;
    }

    /** Port of {@code keyboardDeviceAcquire}. */
    public static boolean keyboardDeviceAcquire() {
        return true;
    }

    /** Port of {@code keyboardDeviceUnacquire}. */
    public static boolean keyboardDeviceUnacquire() {
        return true;
    }

    /** Port of {@code keyboardDeviceReset}. */
    public static boolean keyboardDeviceReset() {
        Input.platformEventDrain.run();
        Input.sfallClearKeys.run();
        return true;
    }

    /** Port of {@code keyboardDeviceGetData} (never fills, as in C). */
    public static boolean keyboardDeviceGetData() {
        return true;
    }

    /** Port of {@code mouseDeviceInit}. */
    public static boolean mouseDeviceInit() {
        mouseDeviceRefreshWindowMapping();
        return mouseDeviceInitMode();
    }

    /** Port of {@code mouseDeviceFree}. */
    public static void mouseDeviceFree() {
    }

    /** Port of {@code keyboardDeviceInit}. */
    public static boolean keyboardDeviceInit() {
        return true;
    }

    /** Port of {@code keyboardDeviceFree}. */
    public static void keyboardDeviceFree() {
    }

    static int[] mapWindowToLogicalPosition(int x, int y) {
        if (mappingWindowWidth <= 0 || mappingWindowHeight <= 0) {
            return new int[] { x, y };
        }

        return new int[] {
            x * mappingLogicalWidth / mappingWindowWidth,
            y * mappingLogicalHeight / mappingWindowHeight,
        };
    }

    /** Port of {@code mouseDeviceRefreshWindowMapping}. */
    public static void mouseDeviceRefreshWindowMapping() {
        // When mouse is in "absolute mode" and scaling is > 1, we need to transform screen coordinates to game coordinates.
        // Cache the window sizes so we have them available in mouseDeviceMapWindowToLogicalPosition.
        // Note: if we add letterboxing or other more complex scaling, we'll have to account for it here.
        mappingLogicalWidth = Svga.screenGetWidth();
        mappingLogicalHeight = Svga.screenGetHeight();

        mappingWindowWidth = windowSize.width();
        mappingWindowHeight = windowSize.height();
    }

    /** Port of {@code mouseDeviceSetLogicalPosition}. */
    public static void mouseDeviceSetLogicalPosition(int x, int y) {
        if (mouseDeviceUsesRelativeMode()
                || mappingLogicalWidth <= 0
                || mappingLogicalHeight <= 0
                || mappingWindowWidth <= 0
                || mappingWindowHeight <= 0) {
            return;
        }

        int windowX = x * mappingWindowWidth / mappingLogicalWidth;
        int windowY = y * mappingWindowHeight / mappingLogicalHeight;
        mouseWarp.warp(windowX, windowY);
    }

    /** Test-only full reset (mirrors a fresh process). */
    static void resetForTests() {
        mouse = NO_MOUSE;
        relativeMode = relative -> true;
        windowSize = new WindowSize() {
            @Override
            public int width() {
                return 0;
            }

            @Override
            public int height() {
                return 0;
            }
        };
        mouseWarp = (x, y) -> {
        };
        eventPump = () -> {
        };
        sfallClearKeys = () -> {
        };
        wheelDeltaX = 0;
        wheelDeltaY = 0;
        mappingWindowWidth = 0;
        mappingWindowHeight = 0;
        mappingLogicalWidth = 0;
        mappingLogicalHeight = 0;
        relativeModeFlag = false;
    }
}
