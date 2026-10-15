package org.fallout2ce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import org.fallout2ce.Geometry.Rect;

/**
 * Window system types, mirroring {@code src/window_manager.h}.
 * The engine ({@code window_manager.cc}) comes with the UI phase.
 *
 * <p>Colors with flags are plain ints ({@code Color} | {@code DrawTextFlags}
 * composes natively). Image/buffer pointers become {@code byte[]} (offsets
 * are explicit params where C++ used pointer arithmetic).
 */
public final class WindowManager {

    private WindowManager() {
    }

    public static final int BUTTON_GROUP_BUTTON_LIST_CAPACITY = 64;

    public enum WindowManagerErr {
        WINDOW_MANAGER_OK(0),
        WINDOW_MANAGER_ERR_INITIALIZING_VIDEO_MODE(1),
        WINDOW_MANAGER_ERR_NO_MEMORY(2),
        WINDOW_MANAGER_ERR_INITIALIZING_TEXT_FONTS(3),
        WINDOW_MANAGER_ERR_WINDOW_SYSTEM_ALREADY_INITIALIZED(4),
        WINDOW_MANAGER_ERR_WINDOW_SYSTEM_NOT_INITIALIZED(5),
        WINDOW_MANAGER_ERR_CURRENT_WINDOWS_TOO_BIG(6),
        WINDOW_MANAGER_ERR_INITIALIZING_DEFAULT_DATABASE(7),
        WINDOW_MANAGER_ERR_8(8),
        WINDOW_MANAGER_ERR_ALREADY_RUNNING(9),
        WINDOW_MANAGER_ERR_TITLE_NOT_SET(10),
        WINDOW_MANAGER_ERR_INITIALIZING_INPUT(11);

        private final int id;

        WindowManagerErr(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, WindowManagerErr> ERR_BY_ID = new HashMap<>();

    static {
        for (WindowManagerErr e : WindowManagerErr.values()) {
            ERR_BY_ID.put(e.id, e);
        }
    }

    public static WindowManagerErr ofWindowManagerErr(int id) {
        return ERR_BY_ID.get(id);
    }

    public enum WindowManagerInitFlags {
        WINDOW_MANAGER_INIT_FLAG_NONE(0x0),
        WINDOW_MANAGER_INIT_FLAG_BUFFERED(0x1);

        private final int id;

        WindowManagerInitFlags(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public enum WindowFlags {
        WINDOW_USE_DEFAULTS(0x1),
        WINDOW_DONT_MOVE_TOP(0x2),
        WINDOW_MOVE_ON_TOP(0x4),
        WINDOW_HIDDEN(0x8),
        WINDOW_MODAL(0x10),
        WINDOW_TRANSPARENT(0x20),
        WINDOW_FLAG_0x40(0x40),
        WINDOW_DRAGGABLE_BY_BACKGROUND(0x80),
        WINDOW_MANAGED(0x100);

        private final int id;

        WindowFlags(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public enum ButtonFlags {
        BUTTON_FLAG_CHECKABLE(0x01),
        BUTTON_FLAG_CHECK_ON_DOWN(0x02),
        BUTTON_FLAG_NO_TOGGLE_OFF(0x04),
        BUTTON_FLAG_DISABLED(0x08),
        BUTTON_DRAG_HANDLE(0x10),
        BUTTON_FLAG_TRANSPARENT(0x20),
        BUTTON_FLAG_0x40(0x40),
        BUTTON_FLAG_GRAPHIC(0x010000),
        BUTTON_FLAG_CHECKED(0x020000),
        BUTTON_FLAG_RADIO(0x040000),
        BUTTON_FLAG_RIGHT_MOUSE_BUTTON_CONFIGURED(0x080000);

        private final int id;

        ButtonFlags(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    /** Port of {@code MenuPulldown} (items as a plain array). */
    public static final class MenuPulldown {
        public final Rect rect = new Rect();
        public int keyCode;
        public String[] items = new String[0];
        public int foregroundColor;
        public int backgroundColor;
    }

    /** Port of {@code MenuBar} (fixed pulldown table + active count). */
    public static final class MenuBar {
        public int win;
        public final Rect rect = new Rect();
        public int pulldownsLength;
        public final MenuPulldown[] pulldowns = new MenuPulldown[15];
        public int foregroundColor;
        public int backgroundColor;

        public MenuBar() {
            for (int i = 0; i < pulldowns.length; i++) {
                pulldowns[i] = new MenuPulldown();
            }
        }
    }

    /** Port of {@code WindowBlitProc} (offsets explicit, C++ used pointers). */
    @FunctionalInterface
    public interface WindowBlitProc {
        void blit(byte[] src, int srcOfs, int width, int height, int srcPitch,
            byte[] dest, int destOfs, int destPitch);
    }

    /** Port of {@code Window}. */
    public static final class Window {
        public int id;
        public int flags;
        public final Rect rect = new Rect();
        public int width;
        public int height;
        public int color;
        public int tx;
        public int ty;
        public byte[] buffer;
        public Button buttonListHead;
        public Button hoveredButton;
        public Button clickedButton;
        public MenuBar menuBar;
        public WindowBlitProc blitProc;
    }

    /** Port of {@code ButtonCallback}. */
    @FunctionalInterface
    public interface ButtonCallback {
        void handle(int btn, int keyCode);
    }

    /** Port of {@code RadioButtonCallback}. */
    @FunctionalInterface
    public interface RadioButtonCallback {
        void handle(int btn);
    }

    /** Port of {@code Button}. */
    public static final class Button {
        public int id;
        public int flags;
        public final Rect rect = new Rect();
        public int mouseEnterEventCode;
        public int mouseExitEventCode;
        public int lefMouseDownEventCode;
        public int leftMouseUpEventCode;
        public int rightMouseDownEventCode;
        public int rightMouseUpEventCode;
        public byte[] normalImage;
        public byte[] pressedImage;
        public byte[] hoverImage;
        public byte[] disabledNormalImage;
        public byte[] disabledPressedImage;
        public byte[] disabledHoverImage;
        public byte[] currentImage;
        public byte[] mask;
        public ButtonCallback mouseEnterProc;
        public ButtonCallback mouseExitProc;
        public ButtonCallback leftMouseDownProc;
        public ButtonCallback leftMouseUpProc;
        public ButtonCallback rightMouseDownProc;
        public ButtonCallback rightMouseUpProc;
        public ButtonCallback pressSoundFunc;
        public ButtonCallback releaseSoundFunc;
        public ButtonGroup buttonGroup;
        public Button prev;
        public Button next;
        public final List<Art.FrmImage> frmImages = new ArrayList<>();
    }

    /** Port of {@code ButtonGroup}. */
    public static final class ButtonGroup {
        public int maxChecked;
        public int currChecked;
        public RadioButtonCallback func;
        public int buttonsLength;
        public final Button[] buttons = new Button[BUTTON_GROUP_BUTTON_LIST_CAPACITY];
    }

    /**
     * Port of {@code UniqueHandle} (RAII dispose): pass the destroy function
     * explicitly (C++ binds it as a template argument).
     */
    public static final class UniqueHandle implements AutoCloseable {
        private final IntConsumer destroyFn;
        private int handle = -1;

        public UniqueHandle(IntConsumer destroyFn) {
            this.destroyFn = destroyFn;
        }

        public UniqueHandle(IntConsumer destroyFn, int handle) {
            this.destroyFn = destroyFn;
            this.handle = handle;
        }

        public int get() {
            return handle;
        }

        public int release() {
            int h = handle;
            handle = -1;
            return h;
        }

        public void reset(int h) {
            if (handle != -1) {
                destroyFn.accept(handle);
            }
            handle = h;
        }

        @Override
        public void close() {
            reset(-1);
        }
    }

    /** Port of {@code gWindowSystemInitialized}. */
    static boolean windowSystemInitialized;

    /** Port of {@code _GNW_wcolor[6]}. */
    static final int[] gnwWcolor = new int[6];

    // ===== window store / refresh engine (window_manager.cc, v38 slice) =====
    //
    // Buttons, menus, text input dialogs and managed windows come with the
    // rest of the file; movie/interpreter/game hooks become seams then.
    // C `rand()` for tx/ty dither seeds is a counter here (unsigned glibc
    // rand is not bit-identical; seeds are unobservable on opaque windows).

    static final int MAX_WINDOW_COUNT = 50;
    static final int BUTTON_GROUP_LIST_CAPACITY = 64;

    /** Port of {@code VideoSystemInitProc}. */
    @FunctionalInterface
    public interface VideoSystemInitProc {
        int init();
    }

    /** Port of {@code VideoSystemExitProc}. */
    @FunctionalInterface
    public interface VideoSystemExitProc {
        void exit();
    }

    /** Title handler seam (C called SDL_SetWindowTitle directly). */
    public static java.util.function.Consumer<String> titleHandler = title -> {
    };

    /** Message box seam (C showed an SDL box and returned true). */
    public static java.util.function.Function<String, Boolean> messageBoxHandler = text -> {
        Debug.print("showMessageBox: %s\n", text);
        return true;
    };

    /** SDL window destroy seam (C called SDL_DestroyWindow directly). */
    public static Runnable sdlWindowDestroy = () -> {
    };

    /** Timed-message slot (window_manager_private.cc state, filled later). */
    static final class TimedMessageSlot {
        int y;
        boolean taken;
    }

    // window_manager_private.cc timed-message state (init here, used there).
    static final int TIMED_MSGS = 5;
    static int tmPersistence;
    static int tmAdd;
    static int tmKill = -1;
    static int scrCenterX;
    static int tmTextX;
    static int tmTextY;
    static int tmH;
    static final TimedMessageSlot[] tmLocation = new TimedMessageSlot[TIMED_MSGS];
    static int curry;
    static int currx;

    /** Placeholder for the timed-message watcher (body with private.cc). */
    static final Runnable tmWatchMsgs = () -> {
    };

    private static boolean alreadyRunning;
    private static boolean insideWinExit;
    private static final int[] windowIndexes = new int[MAX_WINDOW_COUNT];
    private static final Window[] windows = new Window[MAX_WINDOW_COUNT];
    private static VideoSystemInitProc videoSystemInitProc;
    private static VideoSystemExitProc videoSystemExitProc;
    private static int windowsLength;
    private static int windowFlags;
    private static boolean buffering;
    private static int bkColor;
    private static int doingRefreshAll;
    private static byte[] screenBuffer;
    private static Object gnwTexture;
    private static final ButtonGroup[] buttonGroups = new ButtonGroup[BUTTON_GROUP_LIST_CAPACITY];
    private static int txTyCounter;

    static {
        for (int index = 0; index < buttonGroups.length; index++) {
            buttonGroups[index] = new ButtonGroup();
        }
        for (int index = 0; index < tmLocation.length; index++) {
            tmLocation[index] = new TimedMessageSlot();
        }
    }

    /** Port of {@code windowManagerInit} (0x4D5C30). */
    public static int windowManagerInit(VideoSystemInitProc videoInitProc,
            VideoSystemExitProc videoExitProc, int flags) {
        // NOTE: _WIN32 single-instance mutex/title omitted (launcher concern).

        if (alreadyRunning) {
            return WindowManagerErr.WINDOW_MANAGER_ERR_ALREADY_RUNNING.id();
        }

        if (windowSystemInitialized) {
            return WindowManagerErr.WINDOW_MANAGER_ERR_WINDOW_SYSTEM_ALREADY_INITIALIZED.id();
        }

        for (int index = 0; index < MAX_WINDOW_COUNT; index++) {
            windowIndexes[index] = -1;
        }

        if (Db.dbTotal() == 0) {
            if (Db.dbOpen(null, "") == -1) {
                return WindowManagerErr.WINDOW_MANAGER_ERR_INITIALIZING_DEFAULT_DATABASE.id();
            }
        }

        if (TextFont.textFontsInit() == -1) {
            return WindowManagerErr.WINDOW_MANAGER_ERR_INITIALIZING_TEXT_FONTS.id();
        }

        // _get_start_mode_() is an empty no-op in C++.

        videoSystemInitProc = videoInitProc;
        // NOTE: C++ discards the passed exit proc and keeps directInputFree.
        videoSystemExitProc = DInput::directInputFree;

        int rc = videoInitProc.init();
        if (rc == -1) {
            if (videoSystemExitProc != null) {
                videoSystemExitProc.exit();
            } else {
                Svga.directDrawFree();
            }

            return WindowManagerErr.WINDOW_MANAGER_ERR_INITIALIZING_VIDEO_MODE.id();
        }

        if (rc == 8) {
            return WindowManagerErr.WINDOW_MANAGER_ERR_8.id();
        }

        if ((flags & WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()) != 0) {
            screenBuffer = new byte[(Svga.scrSize.bottom - Svga.scrSize.top + 1)
                * (Svga.scrSize.right - Svga.scrSize.left + 1)];
            // Enable buffering since we successfully allocated a buffer.
            buffering = true;
        }

        doingRefreshAll = 0;

        if (!Color.initColors()) {
            byte[] palette = new byte[768];
            Draw.bufferFill(palette, 768, 1, 768, Color.COLOR_FIRST);

            // TODO: Incomplete.
            // _colorBuildColorTable(_getSystemPalette(), palette);
        }

        Debug.print("windowManagerInit: debug init\n");

        if (Input.inputInit() == -1) {
            return WindowManagerErr.WINDOW_MANAGER_ERR_INITIALIZING_INPUT.id();
        }

        gnwIntrInit();

        Window window = new Window();
        windows[0] = window;

        window.id = 0;
        window.flags = 0;
        window.rect.left = Svga.scrSize.left;
        window.rect.top = Svga.scrSize.top;
        window.rect.right = Svga.scrSize.right;
        window.rect.bottom = Svga.scrSize.bottom;
        window.width = Svga.scrSize.right - Svga.scrSize.left + 1;
        window.height = Svga.scrSize.bottom - Svga.scrSize.top + 1;
        window.tx = 0;
        window.ty = 0;
        window.buffer = null;
        window.buttonListHead = null;
        window.hoveredButton = null;
        window.clickedButton = null;
        window.menuBar = null;

        windowsLength = 1;
        windowSystemInitialized = true;
        gnwWcolor[3] = 21140;
        gnwWcolor[4] = 32747;
        gnwWcolor[5] = 31744;
        windowIndexes[0] = 0;
        gnwTexture = null;
        bkColor = Color.COLOR_FIRST;
        gnwWcolor[0] = 10570;
        windowFlags = flags;
        gnwWcolor[2] = 8456;
        gnwWcolor[1] = 15855;

        Runtime.getRuntime().addShutdownHook(new Thread(WindowManager::windowManagerExit));

        return WindowManagerErr.WINDOW_MANAGER_OK.id();
    }

    /** Port of {@code _GNW_intr_init} (timed-message slots). */
    static void gnwIntrInit() {
        int spacing;
        int top;

        tmPersistence = 3000;
        tmAdd = 0;
        tmKill = -1;
        scrCenterX = Svga.scrSize.right / 2;

        if (Svga.scrSize.bottom >= 479) {
            tmTextY = 16;
            tmTextX = 16;
        } else {
            tmTextY = 10;
            tmTextX = 10;
        }

        tmH = 2 * tmTextY + TextFont.fontGetLineHeight();

        spacing = Svga.scrSize.bottom / 8;
        top = Svga.scrSize.bottom / 4;

        for (int index = 0; index < TIMED_MSGS; index++) {
            tmLocation[index].y = spacing * index + top;
            tmLocation[index].taken = false;
        }
    }

    /** Port of {@code windowManagerExit} (0x4D616C). */
    public static void windowManagerExit() {
        if (!insideWinExit) {
            insideWinExit = true;
            if (windowSystemInitialized) {
                gnwIntrExit();

                for (int index = windowsLength - 1; index >= 0; index--) {
                    windowFree(windows[index].id);
                }

                gnwTexture = null;
                screenBuffer = null;

                if (videoSystemExitProc != null) {
                    videoSystemExitProc.exit();
                }

                Input.inputExit();
                Geometry.gnwRectExit();
                TextFont.textFontsExit();
                Color.colorsClose();

                sdlWindowDestroy.run();

                windowSystemInitialized = false;
            }
            insideWinExit = false;
        }
    }

    /** Port of {@code _GNW_intr_exit} (timed-message teardown). */
    static void gnwIntrExit() {
        Input.tickersRemove(tmWatchMsgs);
        while (tmKill != -1) {
            tmKillMsg();
        }
    }

    /** Placeholder for {@code tm_kill_msg} (body with private.cc). */
    static void tmKillMsg() {
        tmKill = -1;
    }

    /** Port of {@code windowCreate} (win_add, 0x4D6238). */
    public static int windowCreate(int x, int y, int width, int height, int color, int flags) {
        if (!windowSystemInitialized) {
            return -1;
        }

        if (windowsLength == MAX_WINDOW_COUNT) {
            return -1;
        }

        if (width > Geometry.rectGetWidth(Svga.scrSize)) {
            return -1;
        }

        if (height > Geometry.rectGetHeight(Svga.scrSize)) {
            return -1;
        }

        Window window = new Window();
        windows[windowsLength] = window;

        window.buffer = new byte[width * height];

        int id = 1;
        while (windowGetWindow(id) != null) {
            id++;
        }

        window.id = id;

        if ((flags & WindowFlags.WINDOW_USE_DEFAULTS.id()) != 0) {
            flags |= windowFlags;
        }

        window.width = width;
        window.height = height;
        window.flags = flags;
        window.tx = (txTyCounter++ * 2) & 0xFFFE;
        window.ty = (txTyCounter++ * 2) & 0xFFFE;

        if (color == Color.COLOR_COUNT) {
            if (gnwTexture == null) {
                color = (Color.colorTable[gnwWcolor[0]] & 0xFF) | Color.DRAW_TEXT_FLAG_NONE;
            }
        } else if ((color & 0xFF00) != 0) {
            int colorIndex = (color & Color.COLOR_LAST) - 1;
            color = (Color.colorTable[gnwWcolor[colorIndex]] & 0xFF) | (color & ~0xFFFF);
        }

        window.buttonListHead = null;
        window.hoveredButton = null;
        window.clickedButton = null;
        window.menuBar = null;
        window.blitProc = (src, srcOfs, w, h, srcPitch, dst, dstOfs, dstPitch) ->
            Draw.blitBufferToBufferTrans(src, srcOfs, w, h, srcPitch, dst, dstOfs, dstPitch);
        window.color = color;
        windowIndexes[id] = windowsLength;
        windowsLength++;

        windowFill(id, 0, 0, width, height, color);

        window.flags |= WindowFlags.WINDOW_HIDDEN.id();
        winMove(id, x, y);
        window.flags = flags;

        if ((flags & WindowFlags.WINDOW_MOVE_ON_TOP.id()) == 0) {
            int topWindowIndex = windowsLength - 2;
            while (topWindowIndex > 0) {
                if ((windows[topWindowIndex].flags & WindowFlags.WINDOW_MOVE_ON_TOP.id()) == 0) {
                    break;
                }
                topWindowIndex--;
            }

            if (topWindowIndex != windowsLength - 2) {
                int insertionIndex = topWindowIndex + 1;
                int shiftIndex = windowsLength - 1;
                while (shiftIndex > insertionIndex) {
                    Window tmp = windows[shiftIndex - 1];
                    windows[shiftIndex] = tmp;
                    windowIndexes[tmp.id] = shiftIndex;
                    shiftIndex--;
                }

                windows[insertionIndex] = window;
                windowIndexes[id] = insertionIndex;
            }
        }

        return id;
    }

    /** Port of {@code windowDestroy} (win_remove, 0x4D6468). */
    public static void windowDestroy(int win) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return;
        }

        if (window == null) {
            return;
        }

        Geometry.Rect rect = new Geometry.Rect(window.rect);

        int windowIndex = windowIndexes[window.id];
        windowFree(win);

        windowIndexes[win] = -1;

        for (int index = windowIndex; index < windowsLength - 1; index++) {
            windows[index] = windows[index + 1];
            windowIndexes[windows[index].id] = index;
        }

        windowsLength--;

        // NOTE: Uninline.
        windowRefreshAll(rect);
    }

    /** Port of {@code windowFree} (0x4D650C). */
    public static void windowFree(int win) {
        Window window = windowGetWindow(win);
        if (window == null) {
            return;
        }

        window.buffer = null;
        window.menuBar = null;

        Button curr = window.buttonListHead;
        while (curr != null) {
            Button next = curr.next;
            buttonFree(curr);
            curr = next;
        }
        window.buttonListHead = null;
    }

    /** Port of {@code buttonFree} (needed by windowFree; rest with buttons). */
    public static void buttonFree(Button button) {
        if ((button.flags & ButtonFlags.BUTTON_FLAG_GRAPHIC.id()) == 0) {
            button.normalImage = null;
            button.pressedImage = null;
            button.hoverImage = null;
            button.disabledNormalImage = null;
            button.disabledPressedImage = null;
            button.disabledHoverImage = null;
        }

        ButtonGroup buttonGroup = button.buttonGroup;
        if (buttonGroup != null) {
            for (int index = 0; index < buttonGroup.buttonsLength; index++) {
                if (button == buttonGroup.buttons[index]) {
                    for (; index < buttonGroup.buttonsLength - 1; index++) {
                        buttonGroup.buttons[index] = buttonGroup.buttons[index + 1];
                    }

                    buttonGroup.buttonsLength--;

                    break;
                }
            }
        }

        for (Art.FrmImage frmImage : button.frmImages) {
            try {
                frmImage.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        button.frmImages.clear();
    }

    /** Port of {@code _win_buffering} (0x4D6558). */
    public static void winBuffering(boolean bufferWindows) {
        if (screenBuffer != null) {
            buffering = bufferWindows;
        }
    }

    /** Port of {@code windowDrawBorder} (0x4D6568). */
    public static void windowDrawBorder(int win) {
        if (!windowSystemInitialized) {
            return;
        }

        Window window = windowGetWindow(win);
        if (window == null) {
            return;
        }

        Draw.lightenBuf(window.buffer, 5, window.width - 10, 5, window.width);
        Draw.lightenBuf(window.buffer, 0, 5, window.height, window.width);
        Draw.lightenBuf(window.buffer, window.width - 5, 5, window.height, window.width);
        Draw.lightenBuf(window.buffer, window.width * (window.height - 5) + 5, window.width - 10, 5,
            window.width);

        Draw.bufferDrawRect(window.buffer, window.width, 0, 0, window.width - 1, window.height - 1,
            Color.colorTable[Color.rgb555(0x000000)] & 0xFF);

        Draw.bufferDrawRectShadowed(window.buffer, window.width, 1, 1, window.width - 2,
            window.height - 2, Color.colorTable[gnwWcolor[1]] & 0xFF,
            Color.colorTable[gnwWcolor[2]] & 0xFF);
        Draw.bufferDrawRectShadowed(window.buffer, window.width, 5, 5, window.width - 6,
            window.height - 6, Color.colorTable[gnwWcolor[2]] & 0xFF,
            Color.colorTable[gnwWcolor[1]] & 0xFF);
    }

    /** Port of {@code windowDrawText} (0x4D684C). */
    public static void windowDrawText(int win, String str, int maxWidth, int x, int y, int color) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return;
        }

        if (window == null) {
            return;
        }

        if (maxWidth == 0) {
            if ((color & Color.DRAW_TEXT_FLAG_MONOSPACED) != 0) {
                maxWidth = TextFont.fontGetMonospacedStringWidth(str);
            } else {
                maxWidth = TextFont.fontGetStringWidth(str);
            }
        }

        if (maxWidth + x > window.width) {
            if ((color & Color.DRAW_TEXT_FLAG_OVERFLOW) == 0) {
                return;
            }

            maxWidth = window.width - x;
        }

        int bufOfs = x + y * window.width;

        if (TextFont.fontGetLineHeight() + y > window.height) {
            return;
        }

        if ((color & Color.DRAW_TEXT_FLAG_NO_BG) == 0) {
            if (window.color == Color.COLOR_COUNT && gnwTexture != null) {
                Draw.bufTexture(window.buffer, bufOfs, maxWidth, TextFont.fontGetLineHeight(), window.width,
                    gnwTexture, window.tx + x, window.ty + y);
            } else {
                Draw.bufferFill(window.buffer, bufOfs, maxWidth, TextFont.fontGetLineHeight(), window.width,
                    window.color & Color.COLOR_LAST);
            }
        }

        int textColor;
        if ((color & 0xFF00) != 0) {
            int colorIndex = (color & Color.COLOR_LAST) - 1;
            textColor = (Color.colorTable[gnwWcolor[colorIndex]] & 0xFF) | (color & ~0xFFFF);
        } else {
            textColor = color;
        }

        TextFont.fontDrawText(window.buffer, bufOfs, str, maxWidth, window.width, textColor);

        if ((color & Color.DRAW_TEXT_FLAG_REFRESH) != 0) {
            // TODO: Check.
            Geometry.Rect rect = new Geometry.Rect();
            rect.left = window.rect.left + x;
            rect.top = window.rect.top + y;
            rect.right = rect.left + maxWidth;
            rect.bottom = rect.top + TextFont.fontGetLineHeight();
            gnwWinRefresh(window, rect, null);
        }
    }

    /** Port of {@code windowDrawLine} (0x4D6B24). */
    public static void windowDrawLine(int win, int left, int top, int right, int bottom, int color) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return;
        }

        if (window == null) {
            return;
        }

        if ((color & 0xFF00) != 0) {
            int colorIndex = (color & Color.COLOR_LAST) - 1;
            color = (Color.colorTable[gnwWcolor[colorIndex]] & 0xFF) | (color & ~0xFFFF);
        }

        Draw.bufferDrawLine(window.buffer, window.width, left, top, right, bottom, color & Color.COLOR_LAST);
    }

    /** Port of {@code windowDrawRect} (0x4D6B88). */
    public static void windowDrawRect(int win, int left, int top, int right, int bottom, int color) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return;
        }

        if (window == null) {
            return;
        }

        if ((color & 0xFF00) != 0) {
            int colorIndex = (color & Color.COLOR_LAST) - 1;
            color = (Color.colorTable[gnwWcolor[colorIndex]] & 0xFF) | (color & ~0xFFFF);
        }

        if (right < left) {
            int tmp = left;
            left = right;
            right = tmp;
        }

        if (bottom < top) {
            int tmp = top;
            top = bottom;
            bottom = tmp;
        }

        Draw.bufferDrawRect(window.buffer, window.width, left, top, right, bottom, color & Color.COLOR_LAST);
    }

    /** Port of {@code windowFill} (0x4D6CC8). */
    public static void windowFill(int win, int x, int y, int width, int height, int color) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return;
        }

        if (window == null) {
            return;
        }

        if (color == Color.COLOR_COUNT) {
            if (gnwTexture != null) {
                Draw.bufTexture(window.buffer, window.width * y + x, width, height, window.width,
                    gnwTexture, x + window.tx, y + window.ty);
            } else {
                color = (Color.colorTable[gnwWcolor[0]] & 0xFF) | Color.DRAW_TEXT_FLAG_NONE;
            }
        } else if ((color & 0xFF00) != 0) {
            int colorIndex = (color & Color.COLOR_LAST) - 1;
            color = (Color.colorTable[gnwWcolor[colorIndex]] & 0xFF) | (color & ~0xFFFF);
        }

        if (color < Color.COLOR_COUNT) {
            Draw.bufferFill(window.buffer, window.width * y + x, width, height, window.width,
                color & Color.COLOR_LAST);
        }
    }

    /** Port of {@code windowShow} (0x4D6DAC). */
    public static void windowShow(int win) {
        Window window = windowGetWindow(win);
        if (window == null) {
            return;
        }
        int index = windowIndexes[window.id];

        if (!windowSystemInitialized) {
            return;
        }

        if ((window.flags & WindowFlags.WINDOW_HIDDEN.id()) != 0) {
            window.flags &= ~WindowFlags.WINDOW_HIDDEN.id();
            if (index == windowsLength - 1) {
                gnwWinRefresh(window, window.rect, null);
            }
        }

        if (index < windowsLength - 1 && (window.flags & WindowFlags.WINDOW_DONT_MOVE_TOP.id()) == 0) {
            while (index < windowsLength - 1) {
                Window nextWindow = windows[index + 1];
                if ((window.flags & WindowFlags.WINDOW_MOVE_ON_TOP.id()) == 0
                        && (nextWindow.flags & WindowFlags.WINDOW_MOVE_ON_TOP.id()) != 0) {
                    break;
                }

                windows[index] = nextWindow;
                windowIndexes[nextWindow.id] = index;
                index++;
            }

            windows[index] = window;
            windowIndexes[window.id] = index;
            gnwWinRefresh(window, window.rect, null);
        } else {
            // SFALL: Fix for the window with the "DontMoveTop" flag not being
            // redrawn after the show function call if it is not the topmost
            // one.
            gnwWinRefresh(window, window.rect, null);
        }
    }

    /** Port of {@code windowHide} (0x4D6E64). */
    public static void windowHide(int win) {
        if (!windowSystemInitialized) {
            return;
        }

        Window window = windowGetWindow(win);
        if (window == null) {
            return;
        }

        if ((window.flags & WindowFlags.WINDOW_HIDDEN.id()) == 0) {
            window.flags |= WindowFlags.WINDOW_HIDDEN.id();
            refreshAll(window.rect, null);
        }
    }

    /** Port of {@code _win_move} (0x4D6EA0). */
    public static void winMove(int win, int x, int y) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return;
        }

        if (window == null) {
            return;
        }

        Geometry.Rect rect = new Geometry.Rect(window.rect);

        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }

        if ((window.flags & WindowFlags.WINDOW_MANAGED.id()) != 0) {
            x += 2;
        }

        if (x + window.width - 1 > Svga.scrSize.right) {
            x = Svga.scrSize.right - window.width + 1;
        }

        if (y + window.height - 1 > Svga.scrSize.bottom) {
            y = Svga.scrSize.bottom - window.height + 1;
        }

        if ((window.flags & WindowFlags.WINDOW_MANAGED.id()) != 0) {
            // TODO: Not sure what this means.
            x &= ~0x03; // truncates x to a multiple of 4 pixels
        }

        window.rect.left = x;
        window.rect.top = y;
        window.rect.right = window.width + x - 1;
        window.rect.bottom = window.height + y - 1;

        if ((window.flags & WindowFlags.WINDOW_HIDDEN.id()) == 0) {
            gnwWinRefresh(window, window.rect, null);

            if (windowSystemInitialized) {
                refreshAll(rect, null);
            }
        }
    }

    /** Port of {@code windowRefresh} (0x4D6F5C). */
    public static void windowRefresh(int win) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return;
        }

        if (window == null) {
            return;
        }

        gnwWinRefresh(window, window.rect, null);
    }

    /** Port of {@code windowRefreshRect} (0x4D6F80). */
    public static void windowRefreshRect(int win, Geometry.Rect rect) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return;
        }

        if (window == null) {
            return;
        }

        Geometry.Rect newRect = new Geometry.Rect(rect);
        Geometry.rectOffset(newRect, window.rect.left, window.rect.top);
        gnwWinRefresh(window, newRect, null);
    }

    /** Port of {@code _GNW_win_refresh}. */
    static void gnwWinRefresh(Window window, Geometry.Rect rect, byte[] dest) {
        // dest is only used when refreshing the portion of the screen containing the cursor (which is subsequently drawn on the buffer)

        if ((window.flags & WindowFlags.WINDOW_HIDDEN.id()) != 0) {
            return;
        }

        // CE: original code seems to have had special code for handling non-refresh-all for transparent updates, but it was incomplete and removed

        Geometry.RectListNode refreshRectList;
        // Initial rectangle list node representing the intersection of window and refresh areas
        if ((window.flags & WindowFlags.WINDOW_TRANSPARENT.id()) != 0 && buffering && doingRefreshAll == 0) {
            Geometry.Rect dirtyRect = new Geometry.Rect(window.rect);
            windowRefreshAll(dirtyRect);
            return;
        } else {
            refreshRectList = Geometry.rectMalloc();
            if (refreshRectList == null) {
                return;
            }

            refreshRectList.next = null;

            refreshRectList.rect.left = Math.max(window.rect.left, rect.left);
            refreshRectList.rect.top = Math.max(window.rect.top, rect.top);
            refreshRectList.rect.right = Math.min(window.rect.right, rect.right);
            refreshRectList.rect.bottom = Math.min(window.rect.bottom, rect.bottom);

            if (refreshRectList.rect.right >= refreshRectList.rect.left
                    && refreshRectList.rect.bottom >= refreshRectList.rect.top) {
                int destPitch = 0;
                if (dest != null) {
                    destPitch = rect.right - rect.left + 1;
                }

                // computes clip boundaries for this window, considering all windows higher in z-index
                refreshRectList = winClip(window, refreshRectList, dest);

                if (window.id != 0) {
                    // loop through all clip rectangles blitting to dest or screen
                    Geometry.RectListNode clipRect = refreshRectList;
                    while (clipRect != null) {
                        gnwButtonRefresh(window, clipRect.rect);

                        if (dest != null) {
                            if (buffering && (window.flags & WindowFlags.WINDOW_TRANSPARENT.id()) != 0) {
                                // window->blitProc is always blitBufferToBufferTrans
                                window.blitProc.blit(window.buffer,
                                    clipRect.rect.left - window.rect.left
                                        + (clipRect.rect.top - window.rect.top) * window.width,
                                    clipRect.rect.right - clipRect.rect.left + 1,
                                    clipRect.rect.bottom - clipRect.rect.top + 1,
                                    window.width,
                                    dest,
                                    destPitch * (clipRect.rect.top - rect.top) + clipRect.rect.left
                                        - rect.left,
                                    destPitch);
                            } else {
                                Draw.blitBufferToBuffer(window.buffer,
                                    clipRect.rect.left - window.rect.left
                                        + (clipRect.rect.top - window.rect.top) * window.width,
                                    clipRect.rect.right - clipRect.rect.left + 1,
                                    clipRect.rect.bottom - clipRect.rect.top + 1,
                                    window.width,
                                    dest,
                                    destPitch * (clipRect.rect.top - rect.top) + clipRect.rect.left
                                        - rect.left,
                                    destPitch);
                            }
                        } else {
                            if (buffering) {
                                if ((window.flags & WindowFlags.WINDOW_TRANSPARENT.id()) != 0) {
                                    window.blitProc.blit(window.buffer,
                                        clipRect.rect.left - window.rect.left
                                            + (clipRect.rect.top - window.rect.top) * window.width,
                                        clipRect.rect.right - clipRect.rect.left + 1,
                                        clipRect.rect.bottom - clipRect.rect.top + 1,
                                        window.width,
                                        screenBuffer,
                                        clipRect.rect.top * (Svga.scrSize.right - Svga.scrSize.left + 1)
                                            + clipRect.rect.left,
                                        Svga.scrSize.right - Svga.scrSize.left + 1);
                                } else {
                                    Draw.blitBufferToBuffer(window.buffer,
                                        clipRect.rect.left - window.rect.left
                                            + (clipRect.rect.top - window.rect.top) * window.width,
                                        clipRect.rect.right - clipRect.rect.left + 1,
                                        clipRect.rect.bottom - clipRect.rect.top + 1,
                                        window.width,
                                        screenBuffer,
                                        clipRect.rect.top * (Svga.scrSize.right - Svga.scrSize.left + 1)
                                            + clipRect.rect.left,
                                        Svga.scrSize.right - Svga.scrSize.left + 1);
                                }
                            } else {
                                Svga.scrBlit.show(window.buffer, window.width,
                                    clipRect.rect.bottom - clipRect.rect.top + 1,
                                    clipRect.rect.left - window.rect.left,
                                    clipRect.rect.top - window.rect.top,
                                    clipRect.rect.right - clipRect.rect.left + 1,
                                    clipRect.rect.bottom - clipRect.rect.top + 1,
                                    clipRect.rect.left,
                                    clipRect.rect.top);
                            }
                        }

                        clipRect = clipRect.next;
                    }
                } else {
                    // this is special window 0 with no window->buffer, so we fill the rectangles with the background color
                    Geometry.RectListNode clipNode = refreshRectList;
                    while (clipNode != null) {
                        int width = clipNode.rect.right - clipNode.rect.left + 1;
                        int height = clipNode.rect.bottom - clipNode.rect.top + 1;
                        byte[] buf = new byte[width * height];
                        Draw.bufferFill(buf, width, height, width, bkColor);
                        if (dest != null) {
                            int dp = destPitch * (clipNode.rect.top - rect.top) + clipNode.rect.left
                                - rect.left;
                            Draw.blitBufferToBuffer(buf, 0, width, height, width, dest, dp, destPitch);
                        } else {
                            if (buffering) {
                                int dp = clipNode.rect.top * (Svga.scrSize.right - Svga.scrSize.left + 1)
                                    + clipNode.rect.left;
                                Draw.blitBufferToBuffer(buf, 0, width, height, width, screenBuffer, dp,
                                    Svga.scrSize.right - Svga.scrSize.left + 1);
                            } else {
                                Svga.scrBlit.show(buf, width,
                                    height,
                                    0,
                                    0,
                                    width,
                                    height,
                                    clipNode.rect.left,
                                    clipNode.rect.top);
                            }
                        }

                        clipNode = clipNode.next;
                    }
                }

                Geometry.RectListNode screenRect = refreshRectList;
                while (screenRect != null) {
                    Geometry.RectListNode nextRect = screenRect.next;

                    // if double-buffering, copy double buffer to screen
                    if (buffering && dest == null) {
                        int pitch = Svga.scrSize.right - Svga.scrSize.left + 1;
                        Svga.scrBlit.show(screenBuffer, pitch,
                            screenRect.rect.bottom - screenRect.rect.top + 1,
                            screenRect.rect.left,
                            screenRect.rect.top,
                            screenRect.rect.right - screenRect.rect.left + 1,
                            screenRect.rect.bottom - screenRect.rect.top + 1,
                            screenRect.rect.left,
                            screenRect.rect.top);
                    }

                    Geometry.rectFree(screenRect); // this is where we clean up the main linkedList

                    screenRect = nextRect;
                }

                if (doingRefreshAll == 0 && dest == null && !Mouse.cursorIsHidden()) {
                    if (Mouse.mouseIn(rect.left, rect.top, rect.right, rect.bottom)) {
                        Mouse.mouseShowCursor();
                    }
                }
            } else {
                Geometry.rectFree(refreshRectList);
            }
        }
    }

    /** Placeholder for {@code _GNW_button_refresh} (body with buttons). */
    static void gnwButtonRefresh(Window window, Geometry.Rect rect) {
    }

    /** Port of {@code windowRefreshAll} (0x4D759C). */
    public static void windowRefreshAll(Geometry.Rect rect) {
        if (windowSystemInitialized) {
            refreshAll(rect, null);
        }
    }

    /** Port of {@code _win_clip} (0x4D75B0). */
    static Geometry.RectListNode winClip(Window currentWindow, Geometry.RectListNode rectList, byte[] dest) {
        for (int index = windowIndexes[currentWindow.id] + 1; index < windowsLength; index++) {
            if (rectList == null) {
                break;
            }

            Window window = windows[index];
            if ((window.flags & WindowFlags.WINDOW_HIDDEN.id()) == 0) {
                if (!buffering || (window.flags & WindowFlags.WINDOW_TRANSPARENT.id()) == 0) {
                    rectList = Geometry.rectClipList(rectList, window.rect);
                } else {
                    if (doingRefreshAll == 0) {
                        gnwWinRefresh(window, window.rect, null);
                        rectList = Geometry.rectClipList(rectList, window.rect);
                    }
                }
            }
        }

        if (dest == screenBuffer || dest == null) {
            if (!Mouse.cursorIsHidden()) {
                Geometry.Rect rect = new Geometry.Rect();
                Mouse.mouseGetRect(rect);
                rectList = Geometry.rectClipList(rectList, rect);
            }
        }

        return rectList;
    }

    /** Port of {@code _win_get_mouse_buf} (0x4D765C helper). */
    public static void winGetMouseBuf(byte[] dest) {
        Geometry.Rect rect = new Geometry.Rect();
        Mouse.mouseGetRect(rect);
        refreshAll(rect, dest);
    }

    /** Port of {@code _refresh_all} (0x4D7814). */
    static void refreshAll(Geometry.Rect rect, byte[] dest) {
        doingRefreshAll = 1;

        for (int index = 0; index < windowsLength; index++) {
            gnwWinRefresh(windows[index], rect, dest);
        }

        doingRefreshAll = 0;

        if (dest == null) {
            if (!Mouse.cursorIsHidden()) {
                if (Mouse.mouseIn(rect.left, rect.top, rect.right, rect.bottom)) {
                    Mouse.mouseShowCursor();
                }
            }
        }
    }

    /** Port of {@code windowIsValidWindowId} (0x4D7888). */
    public static boolean windowIsValidWindowId(int win) {
        return windowGetWindow(win) != null;
    }

    /** Port of {@code windowGetWindow}. */
    public static Window windowGetWindow(int win) {
        if (win < 0 || win >= MAX_WINDOW_COUNT) {
            return null;
        }
        int index = windowIndexes[win];
        if (index < 0 || index >= windowsLength) {
            return null;
        }
        return windows[index];
    }

    /** Port of {@code windowGetBuffer}. */
    public static byte[] windowGetBuffer(int win) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return null;
        }

        if (window == null) {
            return null;
        }

        return window.buffer;
    }

    /** Port of {@code windowGetBuffer2D}. */
    public static Draw.Buffer windowGetBuffer2D(int win) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return null;
        }

        if (window == null) {
            return null;
        }
        return new Draw.Buffer(window.buffer, window.width, window.height);
    }

    /** Port of {@code windowGetAtPoint} (0x4D78CC). */
    public static int windowGetAtPoint(int x, int y) {
        for (int index = windowsLength - 1; index >= 0; index--) {
            Window window = windows[index];
            if (x >= window.rect.left && x <= window.rect.right
                && y >= window.rect.top && y <= window.rect.bottom) {
                return window.id;
            }
        }

        return -1;
    }

    /** Port of {@code windowGetWidth} (0x4D7918). */
    public static int windowGetWidth(int win) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return -1;
        }

        if (window == null) {
            return -1;
        }

        return window.width;
    }

    /** Port of {@code windowGetHeight} (0x4D7934). */
    public static int windowGetHeight(int win) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return -1;
        }

        if (window == null) {
            return -1;
        }

        return window.height;
    }

    /** Port of {@code windowGetRect} (win_get_rect, 0x4D7950). */
    public static int windowGetRect(int win, Geometry.Rect rect) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return -1;
        }

        if (window == null) {
            return -1;
        }

        Geometry.copyTo(rect, window.rect);
        return 0;
    }

    /** Port of {@code _win_text}. */
    public static void winText(int win, String[] fileNameList, int maxWidth, int x, int y, int color) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return;
        }

        if (window == null) {
            return;
        }

        int width = window.width;
        int bufOfs = y * width + x;
        int lineHeight = TextFont.fontGetLineHeight();

        int step = width * lineHeight;
        int separatorTop = lineHeight / 2;
        int separatorBottom = separatorTop + 1;
        int separatorRight = maxWidth - 1;

        for (int index = 0; index < fileNameList.length; index++) {
            String fileName = fileNameList[index];
            if (!fileName.isEmpty()) {
                windowDrawText(win, fileName, maxWidth, x, y, color);
            } else {
                if (maxWidth != 0) {
                    Draw.bufferDrawLine(window.buffer, bufOfs, width, 0, separatorTop, separatorRight,
                        separatorTop, Color.colorTable[gnwWcolor[2]] & 0xFF);
                    Draw.bufferDrawLine(window.buffer, bufOfs, width, 0, separatorBottom, separatorRight,
                        separatorBottom, Color.colorTable[gnwWcolor[1]] & 0xFF);
                }
            }

            bufOfs += step;
            y += lineHeight;
        }
    }

    /** Port of {@code programWindowSetTitle} (0x4D80D8; win32 mutex skipped). */
    public static void programWindowSetTitle(String title) {
        if (title == null) {
            return;
        }

        String truncated = title.length() > 255 ? title.substring(0, 255) : title;
        Svga.programWindowTitle = truncated;
        titleHandler.accept(truncated);
    }

    /** Port of {@code showMessageBox} (0x4D8200; dialog via seam). */
    public static boolean showMessageBox(String text) {
        return messageBoxHandler.apply(text);
    }

    /** Test seam: shutdown-hook-free reset of the whole window system. */
    static void resetForTests() {
        windowSystemInitialized = false;
        java.util.Arrays.fill(gnwWcolor, 0);
        alreadyRunning = false;
        insideWinExit = false;
        for (int index = 0; index < MAX_WINDOW_COUNT; index++) {
            windowIndexes[index] = -1;
            windows[index] = null;
        }
        videoSystemInitProc = null;
        videoSystemExitProc = null;
        windowsLength = 0;
        windowFlags = 0;
        buffering = false;
        bkColor = 0;
        doingRefreshAll = 0;
        screenBuffer = null;
        gnwTexture = null;
        for (int index = 0; index < buttonGroups.length; index++) {
            buttonGroups[index] = new ButtonGroup();
        }
        txTyCounter = 0;
        tmPersistence = 0;
        tmAdd = 0;
        tmKill = -1;
        scrCenterX = 0;
        tmTextX = 0;
        tmTextY = 0;
        tmH = 0;
        curry = 0;
        currx = 0;
        for (int index = 0; index < tmLocation.length; index++) {
            tmLocation[index] = new TimedMessageSlot();
        }
        titleHandler = title -> {
        };
        messageBoxHandler = text -> {
            Debug.print("showMessageBox: %s\n", text);
            return true;
        };
        sdlWindowDestroy = () -> {
        };
    }
}
