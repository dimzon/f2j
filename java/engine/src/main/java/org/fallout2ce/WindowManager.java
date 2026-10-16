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
        public int itemsLength;
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

    /** Port of {@code ListSelectionHandler} (window_manager_private.h). */
    @FunctionalInterface
    public interface ListSelectionHandler {
        void handle(String[] items, int index);
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
    // The button engine below is ported; menus (managed windows, text input
    // dialogs, pulldown interaction) still come with the rest of the file;
    // movie/interpreter/game hooks become seams then.
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

    /** Port of {@code tm_queue} entry ({@code created}/{@code id}/{@code location}). */
    static final class TimedMessageQueueEntry {
        long created;
        int id;
        int location;
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
    static final TimedMessageQueueEntry[] tmQueue = new TimedMessageQueueEntry[TIMED_MSGS];
    static boolean tmWatchActive;

    /** Port of {@code tm_watch_msgs} (ticker proc; re-entrancy via {@code tmWatchActive}). */
    static final Runnable tmWatchMsgs = WindowManager::tmWatchMsgsProc;

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

    /** Port of {@code _last_button_winID}. */
    private static int lastButtonWinId = -1;

    /** Port of {@code _curr_menu} (re-entrancy guard for {@code gnwProcessMenu}). */
    private static MenuBar currMenu;

    static {
        for (int index = 0; index < buttonGroups.length; index++) {
            buttonGroups[index] = new ButtonGroup();
        }
        for (int index = 0; index < tmLocation.length; index++) {
            tmLocation[index] = new TimedMessageSlot();
        }
        for (int index = 0; index < tmQueue.length; index++) {
            tmQueue[index] = new TimedMessageQueueEntry();
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

    /** Port of {@code tm_kill_msg} (0x4DD6C0). */
    static void tmKillMsg() {
        if (tmKill != -1) {
            windowDestroy(tmQueue[tmKill].id);
            tmLocation[tmQueue[tmKill].location].taken = false;

            tmKill++;
            if (tmKill == TIMED_MSGS) {
                tmKill = 0;
            }

            if (tmKill == tmAdd) {
                tmAdd = 0;
                tmKill = -1;
                Input.tickersRemove(tmWatchMsgs);
            }
        }
    }

    /** Port of {@code _win_register_menu_bar} (0x4DC6C0). */
    public static int winRegisterMenuBar(int win, int x, int y, int width, int height,
            int foregroundColor, int backgroundColor) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return -1;
        }

        if (window == null) {
            return -1;
        }

        if (window.menuBar != null) {
            return -1;
        }

        int right = x + width;
        if (right > window.width) {
            return -1;
        }

        int bottom = y + height;
        if (bottom > window.height) {
            return -1;
        }

        MenuBar menuBar = new MenuBar();
        window.menuBar = menuBar;

        menuBar.win = win;
        menuBar.rect.left = x;
        menuBar.rect.top = y;
        menuBar.rect.right = right - 1;
        menuBar.rect.bottom = bottom - 1;
        menuBar.pulldownsLength = 0;
        menuBar.foregroundColor = foregroundColor;
        menuBar.backgroundColor = backgroundColor;

        windowFill(win, x, y, width, height, backgroundColor);
        windowDrawRect(win, x, y, right - 1, bottom - 1, foregroundColor);

        return 0;
    }

    /** Port of {@code _win_register_menu_pulldown} (0x4DC768). */
    public static int winRegisterMenuPulldown(int win, int x, String title, int keyCode,
            int itemsLength, String[] items, int foregroundColor, int backgroundColor) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return -1;
        }

        if (window == null) {
            return -1;
        }

        MenuBar menuBar = window.menuBar;
        if (menuBar == null) {
            return -1;
        }

        if (menuBar.pulldownsLength == 15) {
            return -1;
        }

        int titleX = menuBar.rect.left + x;
        int titleY = (menuBar.rect.top + menuBar.rect.bottom - TextFont.fontGetLineHeight()) / 2;
        int btn = buttonCreate(win,
            titleX,
            titleY,
            TextFont.fontGetStringWidth(title),
            TextFont.fontGetLineHeight(),
            -1,
            -1,
            keyCode,
            -1,
            null,
            null,
            null,
            0);
        if (btn == -1) {
            return -1;
        }

        windowDrawText(win, title, 0, titleX, titleY,
            window.menuBar.foregroundColor | Color.DRAW_TEXT_FLAG_NO_BG);

        MenuPulldown pulldown = window.menuBar.pulldowns[window.menuBar.pulldownsLength];
        pulldown.rect.left = titleX;
        pulldown.rect.top = titleY;
        pulldown.rect.right = TextFont.fontGetStringWidth(title) + titleX - 1;
        pulldown.rect.bottom = TextFont.fontGetLineHeight() + titleY - 1;
        pulldown.keyCode = keyCode;
        pulldown.itemsLength = itemsLength;
        pulldown.items = items;
        pulldown.foregroundColor = foregroundColor;
        pulldown.backgroundColor = backgroundColor;

        window.menuBar.pulldownsLength++;

        return 0;
    }

    /** Port of {@code _win_delete_menu_bar} (0x4DC8D0). */
    public static void winDeleteMenuBar(int win) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return;
        }

        if (window == null) {
            return;
        }

        if (window.menuBar == null) {
            return;
        }

        windowFill(win,
            window.menuBar.rect.left,
            window.menuBar.rect.top,
            Geometry.rectGetWidth(window.menuBar.rect),
            Geometry.rectGetHeight(window.menuBar.rect),
            window.color);

        window.menuBar = null;
    }

    /** Port of {@code _find_first_letter} (0x4DC9F0). */
    public static int findFirstLetter(int ch, String[] stringList, int stringListLength) {
        if (ch >= 'A' && ch <= 'Z') {
            ch += ' ';
        }

        for (int index = 0; index < stringListLength; index++) {
            String string = stringList[index];
            int first = string.isEmpty() ? 0 : string.charAt(0);
            if (first == ch || first == ch - ' ') {
                return index;
            }
        }

        return -1;
    }

    /** Port of {@code _win_width_needed} (0x4DCA30). */
    public static int winWidthNeeded(String[] fileNameList, int fileNameListLength) {
        int maxWidth = 0;

        for (int index = 0; index < fileNameListLength; index++) {
            int width = TextFont.fontGetStringWidth(fileNameList[index]);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        return maxWidth;
    }

    /** Port of {@code win_timed_msg} (0x4DD4C8). */
    public static int winTimedMsg(String msg, int color) {
        if (!windowSystemInitialized) {
            return -1;
        }

        if (tmAdd == tmKill) {
            return -1;
        }

        int width = TextFont.fontGetStringWidth(msg) + 2 * tmTextX;
        int x = scrCenterX - width / 2;
        int height = tmH;
        int y = 0;

        int index;
        for (index = 0; index < TIMED_MSGS; index++) {
            if (!tmLocation[index].taken) {
                tmLocation[index].taken = true;
                y = tmLocation[index].y;
                break;
            }
        }

        if (index == TIMED_MSGS) {
            return -1;
        }

        int win = windowCreate(x, y, width, height, 0x100 | 1, WindowFlags.WINDOW_MOVE_ON_TOP.id());
        windowDrawBorder(win);
        windowDrawText(win, msg, 0, tmTextX, tmTextY, color);

        int btn = buttonCreate(win,
            0,
            0,
            width,
            height,
            -1,
            -1,
            -1,
            -1,
            null,
            null,
            null,
            0);
        buttonSetMouseCallbacks(btn, null, null, null, WindowManager::tmClickResponse);

        windowRefresh(win);

        tmQueue[tmAdd].id = win;
        tmQueue[tmAdd].created = Ticks.getTicks();
        tmQueue[tmAdd].location = index;

        tmAdd++;
        if (tmAdd == TIMED_MSGS) {
            tmAdd = 0;
        } else if (tmKill == -1) {
            tmKill = 0;
            Input.tickersAdd(tmWatchMsgs);
        }

        return 0;
    }

    /** Port of {@code tm_watch_msgs} (0x4DD66C). */
    static void tmWatchMsgsProc() {
        if (tmWatchActive) {
            return;
        }

        tmWatchActive = true;
        while (tmKill != -1) {
            if (Ticks.getTicksSince(tmQueue[tmKill].created) < tmPersistence) {
                break;
            }

            tmKillMsg();
        }
        tmWatchActive = false;
    }

    /** Port of {@code tm_kill_out_of_order} (0x4DD744). */
    static void tmKillOutOfOrder(int queueIndex) {
        if (tmKill == -1) {
            return;
        }

        if (!tmIndexActive(queueIndex)) {
            return;
        }

        windowDestroy(tmQueue[queueIndex].id);

        tmLocation[tmQueue[queueIndex].location].taken = false;

        int copyTo = queueIndex;
        while (copyTo != tmKill) {
            int copyFrom = copyTo - 1;
            if (copyFrom < 0) {
                copyFrom = TIMED_MSGS - 1;
            }

            tmQueue[copyTo].created = tmQueue[copyFrom].created;
            tmQueue[copyTo].id = tmQueue[copyFrom].id;
            tmQueue[copyTo].location = tmQueue[copyFrom].location;
            copyTo = copyFrom;
        }

        tmKill++;
        if (tmKill == TIMED_MSGS) {
            tmKill = 0;
        }

        if (tmAdd == tmKill) {
            tmAdd = 0;
            tmKill = -1;
            Input.tickersRemove(tmWatchMsgs);
        }
    }

    /** Port of {@code tm_click_response} (0x4DD82C). */
    static void tmClickResponse(int btn, int keyCode) {
        if (tmKill == -1) {
            return;
        }

        int win = buttonGetWindowId(btn);
        int queueIndex = tmKill;
        while (win != tmQueue[queueIndex].id) {
            queueIndex++;
            if (queueIndex == TIMED_MSGS) {
                queueIndex = 0;
            }

            if (queueIndex == tmKill || !tmIndexActive(queueIndex)) {
                return;
            }
        }

        tmKillOutOfOrder(queueIndex);
    }

    /** Port of {@code tm_index_active} (0x4DD870). */
    static boolean tmIndexActive(int queueIndex) {
        if (tmKill == tmAdd) {
            return true;
        }

        if (tmKill < tmAdd) {
            return queueIndex >= tmKill && queueIndex < tmAdd;
        }

        return queueIndex < tmAdd || queueIndex >= tmKill;
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
    static void gnwWinRefresh(Window window, Geometry.Rect rect, byte[] dest) {        // dest is only used when refreshing the portion of the screen containing the cursor (which is subsequently drawn on the buffer)

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

    /** Port of {@code _GNW_button_refresh} (0x4D9A58). */
    static void gnwButtonRefresh(Window window, Geometry.Rect rect) {
        Button button = window.buttonListHead;
        if (button != null) {
            while (button.next != null) {
                button = button.next;
            }
        }

        while (button != null) {
            buttonDraw(button, window, button.currentImage, false, rect, false);
            button = button.prev;
        }
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

    // ===== button engine (window_manager.cc) =====

    /** Port of {@code buttonCreate} (0x4D8260). */
    public static int buttonCreate(int win, int x, int y, int width, int height,
            int mouseEnterEventCode, int mouseExitEventCode, int mouseDownEventCode, int mouseUpEventCode,
            byte[] normal, byte[] pressed, byte[] hover, int flags) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return -1;
        }

        if (window == null) {
            return -1;
        }

        if (normal == null && (pressed != null || hover != null)) {
            return -1;
        }

        Button button = buttonCreateInternal(win, x, y, width, height,
            mouseEnterEventCode, mouseExitEventCode, mouseDownEventCode, mouseUpEventCode,
            flags | ButtonFlags.BUTTON_FLAG_GRAPHIC.id(), normal, pressed, hover);
        if (button == null) {
            return -1;
        }

        buttonDraw(button, window, button.normalImage, false, null, false);

        return button.id;
    }

    /** Port of {@code buttonCreateWithFrm} (window_manager.cc). */
    public static int buttonCreateWithFrm(int win, int x, int y,
            int mouseEnterEventCode, int mouseExitEventCode, int mouseDownEventCode, int mouseUpEventCode,
            FrmId normalId, FrmId pressedId, FrmId hoverId, int flags) {
        Art.FrmImage normalFrm = new Art.FrmImage();
        if (!normalFrm.lock(normalId)) {
            normalFrm.close();
            return -1;
        }

        int width = normalFrm.getWidth();
        int height = normalFrm.getHeight();

        Art.FrmImage pressedFrm = new Art.FrmImage();
        pressedFrm.lock(pressedId);
        Art.FrmImage hoverFrm = new Art.FrmImage();
        hoverFrm.lock(hoverId);

        int btnId = buttonCreate(win, x, y, width, height,
            mouseEnterEventCode, mouseExitEventCode,
            mouseDownEventCode, mouseUpEventCode,
            normalFrm.getData(),
            pressedFrm.getData(),
            hoverFrm.getData(),
            flags);
        if (btnId == -1) {
            normalFrm.close();
            pressedFrm.close();
            hoverFrm.close();
            return -1;
        }

        Button button = buttonGetButton(btnId, null);
        button.frmImages.add(normalFrm);
        if (pressedFrm.isLocked()) {
            button.frmImages.add(pressedFrm);
        } else {
            pressedFrm.close();
        }
        if (hoverFrm.isLocked()) {
            button.frmImages.add(hoverFrm);
        } else {
            hoverFrm.close();
        }
        return btnId;
    }

    /** Port of {@code buttonSetDisabledFrm} (window_manager.cc). */
    public static int buttonSetDisabledFrm(int btn,
            FrmId normalId, FrmId pressedId, FrmId hoverId) {
        Art.FrmImage normalFrm = new Art.FrmImage();
        if (!normalFrm.lock(normalId)) {
            normalFrm.close();
            return -1;
        }

        Art.FrmImage pressedFrm = new Art.FrmImage();
        pressedFrm.lock(pressedId);
        Art.FrmImage hoverFrm = new Art.FrmImage();
        hoverFrm.lock(hoverId);

        if (winRegisterButtonDisable(btn, normalFrm.getData(), pressedFrm.getData(),
                hoverFrm.getData()) == -1) {
            normalFrm.close();
            pressedFrm.close();
            hoverFrm.close();
            return -1;
        }

        Button button = buttonGetButton(btn, null);
        if (normalFrm.isLocked()) {
            button.frmImages.add(normalFrm);
        } else {
            normalFrm.close();
        }
        if (pressedFrm.isLocked()) {
            button.frmImages.add(pressedFrm);
        } else {
            pressedFrm.close();
        }
        if (hoverFrm.isLocked()) {
            button.frmImages.add(hoverFrm);
        } else {
            hoverFrm.close();
        }
        return 0;
    }

    /** Port of {@code _win_register_text_button} (0x4D8308). */
    public static int winRegisterTextButton(int win, int x, int y,
            int mouseEnterEventCode, int mouseExitEventCode, int mouseDownEventCode, int mouseUpEventCode,
            String title, int flags) {
        Window window = windowGetWindow(win);

        if (!windowSystemInitialized) {
            return -1;
        }

        if (window == null) {
            return -1;
        }

        int buttonWidth = TextFont.fontGetStringWidth(title) + 16;
        int buttonHeight = TextFont.fontGetLineHeight() + 7;
        byte[] normal = new byte[buttonWidth * buttonHeight];
        byte[] pressed = new byte[buttonWidth * buttonHeight];

        if (window.color != Color.COLOR_COUNT || gnwTexture == null) {
            int fill = window.color & Color.COLOR_LAST;
            Draw.bufferFill(normal, buttonWidth, buttonHeight, buttonWidth, fill);
            Draw.bufferFill(pressed, buttonWidth, buttonHeight, buttonWidth, fill);
        }

        Draw.lightenBuf(normal, buttonWidth, buttonHeight, buttonWidth);

        int textColor = (Color.colorTable[gnwWcolor[3]] & 0xFF) | Color.DRAW_TEXT_FLAG_SHADOWED;
        TextFont.fontDrawText(normal, buttonWidth * 3 + 8, title, buttonWidth, buttonWidth, textColor);
        int light = Color.colorTable[gnwWcolor[1]] & 0xFF;
        int shadow = Color.colorTable[gnwWcolor[2]] & 0xFF;
        int black = Color.colorTable[Color.rgb555(0x000000)] & 0xFF;
        Draw.bufferDrawRectShadowed(normal, buttonWidth, 2, 2, buttonWidth - 3, buttonHeight - 3, light, shadow);
        Draw.bufferDrawRectShadowed(normal, buttonWidth, 1, 1, buttonWidth - 2, buttonHeight - 2, light, shadow);
        Draw.bufferDrawRect(normal, buttonWidth, 0, 0, buttonWidth - 1, buttonHeight - 1, black);

        TextFont.fontDrawText(pressed, buttonWidth * 4 + 9, title, buttonWidth, buttonWidth, textColor);
        Draw.bufferDrawRectShadowed(pressed, buttonWidth, 2, 2, buttonWidth - 3, buttonHeight - 3, shadow,
            light);
        Draw.bufferDrawRectShadowed(pressed, buttonWidth, 1, 1, buttonWidth - 2, buttonHeight - 2, shadow,
            light);
        Draw.bufferDrawRect(pressed, buttonWidth, 0, 0, buttonWidth - 1, buttonHeight - 1, black);

        Button button = buttonCreateInternal(win, x, y, buttonWidth, buttonHeight,
            mouseEnterEventCode, mouseExitEventCode, mouseDownEventCode, mouseUpEventCode,
            flags, normal, pressed, null);
        if (button == null) {
            return -1;
        }

        buttonDraw(button, window, button.normalImage, false, null, false);

        return button.id;
    }

    /** Port of {@code _win_register_button_disable} (0x4D8674). */
    public static int winRegisterButtonDisable(int btn, byte[] normal, byte[] pressed, byte[] hover) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Button button = buttonGetButton(btn, null);
        if (button == null) {
            return -1;
        }

        button.disabledNormalImage = normal;
        button.disabledPressedImage = pressed;
        button.disabledHoverImage = hover;

        return 0;
    }

    /** Port of {@code _win_register_button_image} (0x4D86A8). */
    public static int winRegisterButtonImage(int btn, byte[] normal, byte[] pressed, byte[] hover,
            boolean draw) {
        if (!windowSystemInitialized) {
            return -1;
        }

        if (normal == null && (pressed != null || hover != null)) {
            return -1;
        }

        Window[] windowPtr = new Window[1];
        Button button = buttonGetButton(btn, windowPtr);
        if (button == null) {
            return -1;
        }

        if ((button.flags & ButtonFlags.BUTTON_FLAG_GRAPHIC.id()) == 0) {
            return -1;
        }

        byte[] data = button.currentImage;
        if (data == button.normalImage) {
            button.currentImage = normal;
        } else if (data == button.pressedImage) {
            button.currentImage = pressed;
        } else if (data == button.hoverImage) {
            button.currentImage = hover;
        }

        button.normalImage = normal;
        button.pressedImage = pressed;
        button.hoverImage = hover;

        buttonDraw(button, windowPtr[0], button.currentImage, draw, null, false);

        return 0;
    }

    /** Port of {@code buttonSetMouseCallbacks} (0x4D8758). */
    public static int buttonSetMouseCallbacks(int btn, ButtonCallback mouseEnterProc,
            ButtonCallback mouseExitProc, ButtonCallback mouseDownProc, ButtonCallback mouseUpProc) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Button button = buttonGetButton(btn, null);
        if (button == null) {
            return -1;
        }

        button.mouseEnterProc = mouseEnterProc;
        button.mouseExitProc = mouseExitProc;
        button.leftMouseDownProc = mouseDownProc;
        button.leftMouseUpProc = mouseUpProc;

        return 0;
    }

    /** Port of {@code buttonSetRightMouseCallbacks} (0x4D8798). */
    public static int buttonSetRightMouseCallbacks(int btn, int rightMouseDownEventCode,
            int rightMouseUpEventCode, ButtonCallback rightMouseDownProc, ButtonCallback rightMouseUpProc) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Button button = buttonGetButton(btn, null);
        if (button == null) {
            return -1;
        }

        button.rightMouseDownEventCode = rightMouseDownEventCode;
        button.rightMouseUpEventCode = rightMouseUpEventCode;
        button.rightMouseDownProc = rightMouseDownProc;
        button.rightMouseUpProc = rightMouseUpProc;

        if (rightMouseDownEventCode != -1 || rightMouseUpEventCode != -1 || rightMouseDownProc != null
                || rightMouseUpProc != null) {
            button.flags |= ButtonFlags.BUTTON_FLAG_RIGHT_MOUSE_BUTTON_CONFIGURED.id();
        } else {
            button.flags &= ~ButtonFlags.BUTTON_FLAG_RIGHT_MOUSE_BUTTON_CONFIGURED.id();
        }

        return 0;
    }

    /** Port of {@code buttonSetCallbacks} (0x4D87F8). */
    public static int buttonSetCallbacks(int btn, ButtonCallback pressSoundFunc,
            ButtonCallback releaseSoundFunc) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Button button = buttonGetButton(btn, null);
        if (button == null) {
            return -1;
        }

        button.pressSoundFunc = pressSoundFunc;
        button.releaseSoundFunc = releaseSoundFunc;

        return 0;
    }

    /** Port of {@code buttonSetMask} (0x4D8828). */
    public static int buttonSetMask(int btn, byte[] mask) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Button button = buttonGetButton(btn, null);
        if (button == null) {
            return -1;
        }

        button.mask = mask;

        return 0;
    }

    /** Port of {@code buttonCreateInternal} (0x4D8854). */
    static Button buttonCreateInternal(int win, int x, int y, int width, int height,
            int mouseEnterEventCode, int mouseExitEventCode, int mouseDownEventCode, int mouseUpEventCode,
            int flags, byte[] up, byte[] dn, byte[] hover) {
        Window window = windowGetWindow(win);
        if (window == null) {
            return null;
        }

        if ((flags & ButtonFlags.BUTTON_FLAG_CHECKABLE.id()) == 0) {
            if ((flags & ButtonFlags.BUTTON_FLAG_CHECK_ON_DOWN.id()) != 0) {
                flags &= ~ButtonFlags.BUTTON_FLAG_CHECK_ON_DOWN.id();
            }

            if ((flags & ButtonFlags.BUTTON_FLAG_NO_TOGGLE_OFF.id()) != 0) {
                flags &= ~ButtonFlags.BUTTON_FLAG_NO_TOGGLE_OFF.id();
            }
        }

        Button button = new Button();
        button.id = buttonNewId();
        button.flags = flags;
        button.rect.left = x;
        button.rect.top = y;
        button.rect.right = x + width - 1;
        button.rect.bottom = y + height - 1;
        button.mouseEnterEventCode = mouseEnterEventCode;
        button.mouseExitEventCode = mouseExitEventCode;
        button.lefMouseDownEventCode = mouseDownEventCode;
        button.leftMouseUpEventCode = mouseUpEventCode;
        button.rightMouseDownEventCode = -1;
        button.rightMouseUpEventCode = -1;
        button.normalImage = up;
        button.pressedImage = dn;
        button.hoverImage = hover;
        button.disabledNormalImage = null;
        button.disabledPressedImage = null;
        button.disabledHoverImage = null;
        button.currentImage = null;
        button.mask = null;
        button.mouseEnterProc = null;
        button.mouseExitProc = null;
        button.leftMouseDownProc = null;
        button.leftMouseUpProc = null;
        button.rightMouseDownProc = null;
        button.rightMouseUpProc = null;
        button.pressSoundFunc = null;
        button.releaseSoundFunc = null;
        button.buttonGroup = null;
        button.prev = null;

        button.next = window.buttonListHead;
        if (button.next != null) {
            button.next.prev = button;
        }
        window.buttonListHead = button;

        return button;
    }

    /** Port of {@code _win_button_down} (0x4D89E4). */
    public static boolean winButtonDown(int btn) {
        if (!windowSystemInitialized) {
            return false;
        }

        Button button = buttonGetButton(btn, null);
        if (button == null) {
            return false;
        }

        return (button.flags & ButtonFlags.BUTTON_FLAG_CHECKABLE.id()) != 0
            && (button.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0;
    }

    /** Port of {@code buttonGetButton} (0x4D79DC; window out-param is {@code Window[1]}). */
    static Button buttonGetButton(int btn, Window[] windowPtr) {
        for (int index = 0; index < windowsLength; index++) {
            Window window = windows[index];
            Button button = window.buttonListHead;
            while (button != null) {
                if (button.id == btn) {
                    if (windowPtr != null) {
                        windowPtr[0] = window;
                    }
                    return button;
                }
                button = button.next;
            }
        }

        return null;
    }

    /** Port of {@code button_new_id} (0x4D9458, inlined in C). */
    private static int buttonNewId() {
        int btn = 1;
        while (buttonGetButton(btn, null) != null) {
            btn++;
        }
        return btn;
    }

    /** Port of {@code _win_check_all_buttons} (top of window_manager.cc slice). */
    public static int winCheckAllButtons() {
        if (!windowSystemInitialized) {
            return -1;
        }

        int keyCode = -1;
        int[] keyCodeOut = new int[1];
        for (int index = windowsLength - 1; index >= 1; index--) {
            keyCodeOut[0] = -1;
            if (gnwCheckButtons(windows[index], keyCodeOut) == 0) {
                keyCode = keyCodeOut[0];
                break;
            }

            if ((windows[index].flags & WindowFlags.WINDOW_MODAL.id()) != 0) {
                break;
            }
        }

        return keyCode;
    }

    /** Port of {@code _GNW_check_menu_bars} (0x4D7A34). */
    public static int gnwCheckMenuBars(int input) {
        if (!windowSystemInitialized) {
            return -1;
        }

        for (int index = windowsLength - 1; index >= 1; index--) {
            Window window = windows[index];
            if (window.menuBar != null) {
                for (int pulldownIndex = 0; pulldownIndex < window.menuBar.pulldownsLength; pulldownIndex++) {
                    if (input == window.menuBar.pulldowns[pulldownIndex].keyCode) {
                        input = gnwProcessMenu(window.menuBar, pulldownIndex);
                        break;
                    }
                }
            }

            if ((window.flags & WindowFlags.WINDOW_MODAL.id()) != 0) {
                break;
            }
        }

        return input;
    }

    /** Port of {@code _GNW_process_menu} (0x4DC930, window_manager_private.cc). */
    static int gnwProcessMenu(MenuBar menuBar, int pulldownIndex) {
        if (currMenu != null) {
            return -1;
        }

        currMenu = menuBar;

        int keyCode;
        Geometry.Rect rect = new Geometry.Rect();
        do {
            MenuPulldown pulldown = menuBar.pulldowns[pulldownIndex];
            int win = createPullDown(pulldown.items, pulldown.itemsLength,
                pulldown.rect.left, menuBar.rect.bottom + 1,
                pulldown.foregroundColor, pulldown.backgroundColor, rect);
            if (win == -1) {
                currMenu = null;
                return -1;
            }

            keyCode = processPullDown(win, rect, pulldown.items, pulldown.itemsLength,
                pulldown.foregroundColor, pulldown.backgroundColor, menuBar, pulldownIndex);
            if (keyCode < -1) {
                pulldownIndex = -2 - keyCode;
            }
        } while (keyCode < -1);

        if (keyCode != -1) {
            Input.inputEventQueueReset();
            Input.enqueueInputEvent(keyCode);
            keyCode = menuBar.pulldowns[pulldownIndex].keyCode;
        }

        currMenu = null;

        return keyCode;
    }

    /** Port of {@code win_drag} (0x4D765C): harness never drags, minimal stub. */
    static void winDrag(int win) {
        Window window = windowGetWindow(win);
        if (!windowSystemInitialized) {
            return;
        }
        if (window == null) {
            return;
        }
        windowShow(win);
    }

    /** Port of {@code _GNW_check_buttons} (0x4D8A10). */
    static int gnwCheckButtons(Window window, int[] keyCodePtr) {
        if ((window.flags & WindowFlags.WINDOW_HIDDEN.id()) != 0) {
            return -1;
        }

        Button button = window.buttonListHead;
        Button prevHoveredButton = window.hoveredButton;
        Button prevClickedButton = window.clickedButton;

        Geometry.Rect buttonScreenRect = new Geometry.Rect();
        if (prevHoveredButton != null) {
            Geometry.copyTo(buttonScreenRect, prevHoveredButton.rect);
            Geometry.rectOffset(buttonScreenRect, window.rect.left, window.rect.top);
        } else if (prevClickedButton != null) {
            Geometry.copyTo(buttonScreenRect, prevClickedButton.rect);
            Geometry.rectOffset(buttonScreenRect, window.rect.left, window.rect.top);
        }

        keyCodePtr[0] = -1;

        if (Mouse.mouseClickIn(window.rect.left, window.rect.top, window.rect.right, window.rect.bottom)) {
            int mouseEvent = Mouse.mouseGetEvent();
            if ((window.flags & WindowFlags.WINDOW_FLAG_0x40.id()) != 0
                    || (mouseEvent & Mouse.EVENT_LEFT_BUTTON_DOWN) == 0) {
                if (mouseEvent == 0) {
                    window.clickedButton = null;
                }
            } else {
                windowShow(window.id);
            }

            if (prevHoveredButton != null) {
                if (!buttonUnderMouse(prevHoveredButton, buttonScreenRect)) {
                    if ((prevHoveredButton.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                        keyCodePtr[0] = prevHoveredButton.mouseExitEventCode;
                    }

                    if ((prevHoveredButton.flags & ButtonFlags.BUTTON_FLAG_CHECKABLE.id()) != 0
                            && (prevHoveredButton.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                        buttonDraw(prevHoveredButton, window, prevHoveredButton.pressedImage, true, null,
                            true);
                    } else {
                        buttonDraw(prevHoveredButton, window, prevHoveredButton.normalImage, true, null,
                            true);
                    }

                    window.hoveredButton = null;

                    lastButtonWinId = window.id;

                    if ((prevHoveredButton.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                        if (prevHoveredButton.mouseExitProc != null) {
                            prevHoveredButton.mouseExitProc.handle(prevHoveredButton.id, keyCodePtr[0]);
                            if ((prevHoveredButton.flags & ButtonFlags.BUTTON_FLAG_0x40.id()) == 0) {
                                keyCodePtr[0] = -1;
                            }
                        }
                    }
                    return 0;
                }
                button = prevHoveredButton;
            } else if (prevClickedButton != null) {
                if (buttonUnderMouse(prevClickedButton, buttonScreenRect)) {
                    if ((prevClickedButton.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                        keyCodePtr[0] = prevClickedButton.mouseEnterEventCode;
                    }

                    if ((prevClickedButton.flags & ButtonFlags.BUTTON_FLAG_CHECKABLE.id()) != 0
                            && (prevClickedButton.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                        buttonDraw(prevClickedButton, window, prevClickedButton.pressedImage, true, null,
                            true);
                    } else {
                        buttonDraw(prevClickedButton, window, prevClickedButton.normalImage, true, null,
                            true);
                    }

                    window.hoveredButton = prevClickedButton;

                    lastButtonWinId = window.id;

                    if ((prevClickedButton.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                        if (prevClickedButton.mouseEnterProc != null) {
                            prevClickedButton.mouseEnterProc.handle(prevClickedButton.id, keyCodePtr[0]);
                            if ((prevClickedButton.flags & ButtonFlags.BUTTON_FLAG_0x40.id()) == 0) {
                                keyCodePtr[0] = -1;
                            }
                        }
                    }
                    return 0;
                }
            }

            int dragWindowId = lastButtonWinId;
            if (lastButtonWinId != -1 && lastButtonWinId != window.id) {
                Window previousWindow = windowGetWindow(lastButtonWinId);
                if (previousWindow != null) {
                    lastButtonWinId = -1;

                    Button previousHoveredButton = previousWindow.hoveredButton;
                    if (previousHoveredButton != null) {
                        if ((previousHoveredButton.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                            keyCodePtr[0] = previousHoveredButton.mouseExitEventCode;
                        }

                        if ((previousHoveredButton.flags & ButtonFlags.BUTTON_FLAG_CHECKABLE.id()) != 0
                                && (previousHoveredButton.flags
                                        & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                            buttonDraw(previousHoveredButton, previousWindow,
                                previousHoveredButton.pressedImage, true, null, true);
                        } else {
                            buttonDraw(previousHoveredButton, previousWindow,
                                previousHoveredButton.normalImage, true, null, true);
                        }

                        previousWindow.clickedButton = null;
                        previousWindow.hoveredButton = null;

                        if ((previousHoveredButton.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                            if (previousHoveredButton.mouseExitProc != null) {
                                previousHoveredButton.mouseExitProc.handle(previousHoveredButton.id,
                                    keyCodePtr[0]);
                                if ((previousHoveredButton.flags & ButtonFlags.BUTTON_FLAG_0x40.id()) == 0) {
                                    keyCodePtr[0] = -1;
                                }
                            }
                        }
                        return 0;
                    }
                }
            }

            ButtonCallback cb = null;

            while (button != null) {
                if ((button.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                    Geometry.copyTo(buttonScreenRect, button.rect);
                    Geometry.rectOffset(buttonScreenRect, window.rect.left, window.rect.top);
                    if (buttonUnderMouse(button, buttonScreenRect)) {
                        if ((button.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                            if ((mouseEvent & Mouse.EVENT_ANY_BUTTON_DOWN) != 0) {
                                if ((mouseEvent & Mouse.EVENT_RIGHT_BUTTON_DOWN) != 0
                                        && (button.flags & ButtonFlags.BUTTON_FLAG_RIGHT_MOUSE_BUTTON_CONFIGURED
                                                .id()) == 0) {
                                    button = null;
                                    break;
                                }

                                if (button != window.hoveredButton && button != window.clickedButton) {
                                    break;
                                }

                                window.clickedButton = button;
                                window.hoveredButton = button;

                                if ((button.flags & ButtonFlags.BUTTON_FLAG_CHECKABLE.id()) != 0) {
                                    if ((button.flags & ButtonFlags.BUTTON_FLAG_CHECK_ON_DOWN.id()) != 0) {
                                        if ((button.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                                            if ((button.flags
                                                    & ButtonFlags.BUTTON_FLAG_NO_TOGGLE_OFF.id()) == 0) {
                                                if (button.buttonGroup != null) {
                                                    button.buttonGroup.currChecked--;
                                                }

                                                if ((mouseEvent & Mouse.EVENT_LEFT_BUTTON_DOWN) != 0) {
                                                    keyCodePtr[0] = button.leftMouseUpEventCode;
                                                    cb = button.leftMouseUpProc;
                                                } else {
                                                    keyCodePtr[0] = button.rightMouseUpEventCode;
                                                    cb = button.rightMouseUpProc;
                                                }

                                                button.flags &= ~ButtonFlags.BUTTON_FLAG_CHECKED.id();
                                            }
                                        } else {
                                            if (buttonCheckGroup(button) == -1) {
                                                button = null;
                                                break;
                                            }

                                            if ((mouseEvent & Mouse.EVENT_LEFT_BUTTON_DOWN) != 0) {
                                                keyCodePtr[0] = button.lefMouseDownEventCode;
                                                cb = button.leftMouseDownProc;
                                            } else {
                                                keyCodePtr[0] = button.rightMouseDownEventCode;
                                                cb = button.rightMouseDownProc;
                                            }

                                            button.flags |= ButtonFlags.BUTTON_FLAG_CHECKED.id();
                                        }
                                    }
                                } else {
                                    if (buttonCheckGroup(button) == -1) {
                                        button = null;
                                        break;
                                    }

                                    if ((mouseEvent & Mouse.EVENT_LEFT_BUTTON_DOWN) != 0) {
                                        keyCodePtr[0] = button.lefMouseDownEventCode;
                                        cb = button.leftMouseDownProc;
                                    } else {
                                        keyCodePtr[0] = button.rightMouseDownEventCode;
                                        cb = button.rightMouseDownProc;
                                    }
                                }

                                buttonDraw(button, window, button.pressedImage, true, null, true);
                                break;
                            }

                            Button clickedButton = window.clickedButton;
                            if (button == clickedButton
                                    && (mouseEvent & Mouse.EVENT_ANY_BUTTON_UP) != 0) {
                                window.clickedButton = null;
                                window.hoveredButton = clickedButton;

                                if ((clickedButton.flags & ButtonFlags.BUTTON_FLAG_CHECKABLE.id()) != 0) {
                                    if ((clickedButton.flags
                                            & ButtonFlags.BUTTON_FLAG_CHECK_ON_DOWN.id()) == 0) {
                                        if ((clickedButton.flags
                                                & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                                            if ((clickedButton.flags
                                                    & ButtonFlags.BUTTON_FLAG_NO_TOGGLE_OFF.id()) == 0) {
                                                if (clickedButton.buttonGroup != null) {
                                                    clickedButton.buttonGroup.currChecked--;
                                                }

                                                if ((mouseEvent & Mouse.EVENT_LEFT_BUTTON_UP) != 0) {
                                                    keyCodePtr[0] = button.leftMouseUpEventCode;
                                                    cb = button.leftMouseUpProc;
                                                } else {
                                                    keyCodePtr[0] = button.rightMouseUpEventCode;
                                                    cb = button.rightMouseUpProc;
                                                }

                                                button.flags &= ~ButtonFlags.BUTTON_FLAG_CHECKED.id();
                                            }
                                        } else {
                                            if (buttonCheckGroup(clickedButton) == -1) {
                                                button = null;
                                                buttonDraw(clickedButton, window, clickedButton.normalImage,
                                                    true, null, true);
                                                break;
                                            }

                                            if ((mouseEvent & Mouse.EVENT_LEFT_BUTTON_UP) != 0) {
                                                keyCodePtr[0] = clickedButton.lefMouseDownEventCode;
                                                cb = clickedButton.leftMouseDownProc;
                                            } else {
                                                keyCodePtr[0] = clickedButton.rightMouseDownEventCode;
                                                cb = clickedButton.rightMouseDownProc;
                                            }

                                            clickedButton.flags |= ButtonFlags.BUTTON_FLAG_CHECKED.id();
                                        }
                                    }
                                } else {
                                    if ((clickedButton.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                                        if (clickedButton.buttonGroup != null) {
                                            clickedButton.buttonGroup.currChecked--;
                                        }
                                    }

                                    if ((mouseEvent & Mouse.EVENT_LEFT_BUTTON_UP) != 0) {
                                        keyCodePtr[0] = clickedButton.leftMouseUpEventCode;
                                        cb = clickedButton.leftMouseUpProc;
                                    } else {
                                        keyCodePtr[0] = clickedButton.rightMouseUpEventCode;
                                        cb = clickedButton.rightMouseUpProc;
                                    }
                                }

                                if (button.hoverImage != null) {
                                    buttonDraw(button, window, button.hoverImage, true, null, true);
                                } else {
                                    buttonDraw(button, window, button.normalImage, true, null, true);
                                }
                                break;
                            }
                        }

                        if (window.hoveredButton == null && mouseEvent == 0) {
                            window.hoveredButton = button;
                            if ((button.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                                keyCodePtr[0] = button.mouseEnterEventCode;
                                cb = button.mouseEnterProc;
                            }

                            buttonDraw(button, window, button.hoverImage, true, null, true);
                        }
                        break;
                    }
                }
                button = button.next;
            }

            if (button != null) {
                if ((button.flags & ButtonFlags.BUTTON_DRAG_HANDLE.id()) != 0
                        && (mouseEvent & Mouse.EVENT_ANY_BUTTON_DOWN) != 0
                        && (mouseEvent & Mouse.EVENT_ANY_BUTTON_REPEAT) == 0) {
                    winDrag(window.id);
                    buttonDraw(button, window, button.normalImage, true, null, true);
                }
            } else if ((window.flags & WindowFlags.WINDOW_DRAGGABLE_BY_BACKGROUND.id()) != 0) {
                dragWindowId |= mouseEvent << 8;
                if ((mouseEvent & Mouse.EVENT_ANY_BUTTON_DOWN) != 0
                        && (mouseEvent & Mouse.EVENT_ANY_BUTTON_REPEAT) == 0) {
                    winDrag(window.id);
                }
            }

            lastButtonWinId = window.id;

            if (button != null) {
                if (cb != null) {
                    cb.handle(button.id, keyCodePtr[0]);
                    if ((button.flags & ButtonFlags.BUTTON_FLAG_0x40.id()) == 0) {
                        keyCodePtr[0] = -1;
                    }
                }
            }

            return 0;
        }

        if (prevHoveredButton != null) {
            keyCodePtr[0] = prevHoveredButton.mouseExitEventCode;

            byte[] data;
            if ((prevHoveredButton.flags & ButtonFlags.BUTTON_FLAG_CHECKABLE.id()) != 0
                    && (prevHoveredButton.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                data = prevHoveredButton.pressedImage;
            } else {
                data = prevHoveredButton.normalImage;
            }

            buttonDraw(prevHoveredButton, window, data, true, null, true);

            window.hoveredButton = null;
        }

        if (keyCodePtr[0] != -1) {
            lastButtonWinId = window.id;

            if ((prevHoveredButton.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                if (prevHoveredButton.mouseExitProc != null) {
                    prevHoveredButton.mouseExitProc.handle(prevHoveredButton.id, keyCodePtr[0]);
                    if ((prevHoveredButton.flags & ButtonFlags.BUTTON_FLAG_0x40.id()) == 0) {
                        keyCodePtr[0] = -1;
                    }
                }
            }
            return 0;
        }

        if (prevHoveredButton != null) {
            if ((prevHoveredButton.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
                if (prevHoveredButton.mouseExitProc != null) {
                    prevHoveredButton.mouseExitProc.handle(prevHoveredButton.id, keyCodePtr[0]);
                }
            }
        }

        return -1;
    }

    /** Port of {@code _button_under_mouse} (0x4D9214). */
    static boolean buttonUnderMouse(Button button, Geometry.Rect rect) {
        if (!Mouse.mouseClickIn(rect.left, rect.top, rect.right, rect.bottom)) {
            return false;
        }

        if (button.mask == null) {
            return true;
        }

        int[] pos = new int[2];
        Mouse.mouseGetPosition(pos);
        int x = pos[0] - rect.left;
        int y = pos[1] - rect.top;

        int width = button.rect.right - button.rect.left + 1;
        return button.mask[width * y + x] != 0;
    }

    /** Port of {@code buttonGetWindowId} (0x4D927C). */
    public static int buttonGetWindowId(int btn) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Window[] windowPtr = new Window[1];
        if (buttonGetButton(btn, windowPtr) == null) {
            return -1;
        }

        return windowPtr[0].id;
    }

    /** Port of {@code _win_last_button_winID} (0x4D92B4). */
    public static int winLastButtonWinID() {
        return lastButtonWinId;
    }

    /** Port of {@code buttonDestroy} (0x4D92BC). */
    public static int buttonDestroy(int btn) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Window[] windowPtr = new Window[1];
        Button button = buttonGetButton(btn, windowPtr);
        if (button == null) {
            return -1;
        }
        Window window = windowPtr[0];

        if (button.prev != null) {
            button.prev.next = button.next;
        } else {
            window.buttonListHead = button.next;
        }

        if (button.next != null) {
            button.next.prev = button.prev;
        }

        windowFill(window.id, button.rect.left, button.rect.top,
            button.rect.right - button.rect.left + 1, button.rect.bottom - button.rect.top + 1,
            window.color);

        if (button == window.hoveredButton) {
            window.hoveredButton = null;
        }

        if (button == window.clickedButton) {
            window.clickedButton = null;
        }

        buttonFree(button);

        return 0;
    }

    /** Port of {@code buttonEnable} (0x4D9474). */
    public static int buttonEnable(int btn) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Window[] windowPtr = new Window[1];
        Button button = buttonGetButton(btn, windowPtr);
        if (button == null) {
            return -1;
        }

        if ((button.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) != 0) {
            button.flags &= ~ButtonFlags.BUTTON_FLAG_DISABLED.id();
            buttonDraw(button, windowPtr[0], button.currentImage, true, null, false);
        }

        return 0;
    }

    /** Port of {@code buttonDisable} (0x4D94D0). */
    public static int buttonDisable(int btn) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Window[] windowPtr = new Window[1];
        Button button = buttonGetButton(btn, windowPtr);
        if (button == null) {
            return -1;
        }

        if ((button.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) == 0) {
            button.flags |= ButtonFlags.BUTTON_FLAG_DISABLED.id();

            buttonDraw(button, windowPtr[0], button.currentImage, true, null, false);

            if (button == windowPtr[0].hoveredButton) {
                if (windowPtr[0].hoveredButton.mouseExitEventCode != -1) {
                    Input.enqueueInputEvent(windowPtr[0].hoveredButton.mouseExitEventCode);
                }

                windowPtr[0].hoveredButton = null;
            }
        }

        return 0;
    }

    /** Port of {@code _win_set_button_rest_state} (0x4D9554). */
    public static int winSetButtonRestState(int btn, boolean checked, int flags) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Window[] windowPtr = new Window[1];
        Button button = buttonGetButton(btn, windowPtr);
        if (button == null) {
            return -1;
        }

        if ((button.flags & ButtonFlags.BUTTON_FLAG_CHECKABLE.id()) != 0) {
            int keyCode = -1;

            if ((button.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                if (!checked) {
                    button.flags &= ~ButtonFlags.BUTTON_FLAG_CHECKED.id();

                    if ((flags & 0x02) == 0) {
                        buttonDraw(button, windowPtr[0], button.normalImage, true, null, false);
                    }

                    if (button.buttonGroup != null) {
                        button.buttonGroup.currChecked--;
                    }

                    keyCode = button.leftMouseUpEventCode;
                }
            } else {
                if (checked) {
                    button.flags |= ButtonFlags.BUTTON_FLAG_CHECKED.id();

                    if ((flags & 0x02) == 0) {
                        buttonDraw(button, windowPtr[0], button.pressedImage, true, null, false);
                    }

                    if (button.buttonGroup != null) {
                        button.buttonGroup.currChecked++;
                    }

                    keyCode = button.lefMouseDownEventCode;
                }
            }

            if (keyCode != -1) {
                if ((flags & 0x01) != 0) {
                    Input.enqueueInputEvent(keyCode);
                }
            }
        }

        return 0;
    }

    /** Port of {@code _win_group_check_buttons} (0x4D962C). */
    public static int winGroupCheckButtons(int buttonCount, int[] btns, int maxChecked,
            RadioButtonCallback func) {
        if (!windowSystemInitialized) {
            return -1;
        }

        if (buttonCount > BUTTON_GROUP_BUTTON_LIST_CAPACITY) {
            return -1;
        }

        for (int groupIndex = 0; groupIndex < buttonGroups.length; groupIndex++) {
            ButtonGroup buttonGroup = buttonGroups[groupIndex];
            if (buttonGroup.buttonsLength == 0) {
                buttonGroup.currChecked = 0;

                for (int buttonIndex = 0; buttonIndex < buttonCount; buttonIndex++) {
                    Button button = buttonGetButton(btns[buttonIndex], null);
                    if (button == null) {
                        return -1;
                    }

                    buttonGroup.buttons[buttonIndex] = button;

                    button.buttonGroup = buttonGroup;

                    if ((button.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                        buttonGroup.currChecked++;
                    }
                }

                buttonGroup.buttonsLength = buttonCount;
                buttonGroup.maxChecked = maxChecked;
                buttonGroup.func = func;
                return 0;
            }
        }

        return -1;
    }

    /** Port of {@code _win_group_radio_buttons} (0x4D96EC). */
    public static int winGroupRadioButtons(int count, int[] btns) {
        if (!windowSystemInitialized) {
            return -1;
        }

        if (winGroupCheckButtons(count, btns, 1, null) == -1) {
            return -1;
        }

        Button button = buttonGetButton(btns[0], null);
        ButtonGroup buttonGroup = button.buttonGroup;

        for (int index = 0; index < buttonGroup.buttonsLength; index++) {
            Button groupButton = buttonGroup.buttons[index];
            groupButton.flags |= ButtonFlags.BUTTON_FLAG_RADIO.id();
        }

        return 0;
    }

    /** Port of {@code _button_check_group} (0x4D9744). */
    static int buttonCheckGroup(Button button) {
        if (button.buttonGroup == null) {
            return 0;
        }

        if ((button.flags & ButtonFlags.BUTTON_FLAG_RADIO.id()) != 0) {
            if (button.buttonGroup.currChecked > 0) {
                for (int index = 0; index < button.buttonGroup.buttonsLength; index++) {
                    Button groupButton = button.buttonGroup.buttons[index];
                    if ((groupButton.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                        groupButton.flags &= ~ButtonFlags.BUTTON_FLAG_CHECKED.id();

                        Window[] windowPtr = new Window[1];
                        buttonGetButton(groupButton.id, windowPtr);
                        buttonDraw(groupButton, windowPtr[0], groupButton.normalImage, true, null, true);

                        if (groupButton.leftMouseUpProc != null) {
                            groupButton.leftMouseUpProc.handle(groupButton.id,
                                groupButton.leftMouseUpEventCode);
                        }
                    }
                }
            }

            if ((button.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) == 0) {
                button.buttonGroup.currChecked++;
            }

            return 0;
        }

        if (button.buttonGroup.currChecked < button.buttonGroup.maxChecked) {
            if ((button.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) == 0) {
                button.buttonGroup.currChecked++;
            }

            return 0;
        }

        if (button.buttonGroup.func != null) {
            button.buttonGroup.func.handle(button.id);
        }

        return -1;
    }

    /** Port of {@code _button_draw} (0x4D9808). Null data only updates state, never NPEs. */
    static void buttonDraw(Button button, Window window, byte[] data, boolean draw, Geometry.Rect bound,
            boolean sound) {
        byte[] previousImage = null;
        if (data != null) {
            Geometry.Rect screenRect = new Geometry.Rect();
            Geometry.copyTo(screenRect, button.rect);
            Geometry.rectOffset(screenRect, window.rect.left, window.rect.top);

            Geometry.Rect clippedRect = new Geometry.Rect();
            if (bound != null) {
                if (Geometry.rectIntersection(screenRect, bound, screenRect) == -1) {
                    return;
                }

                Geometry.copyTo(clippedRect, screenRect);
                Geometry.rectOffset(clippedRect, -window.rect.left, -window.rect.top);
            } else {
                Geometry.copyTo(clippedRect, button.rect);
            }

            if (data == button.normalImage
                    && (button.flags & ButtonFlags.BUTTON_FLAG_CHECKED.id()) != 0) {
                data = button.pressedImage;
            }

            if ((button.flags & ButtonFlags.BUTTON_FLAG_DISABLED.id()) != 0) {
                if (data == button.normalImage) {
                    data = button.disabledNormalImage;
                } else if (data == button.pressedImage) {
                    data = button.disabledPressedImage;
                } else if (data == button.hoverImage) {
                    data = button.disabledHoverImage;
                }
            } else {
                if (data == button.disabledNormalImage) {
                    data = button.normalImage;
                } else if (data == button.disabledPressedImage) {
                    data = button.pressedImage;
                } else if (data == button.disabledHoverImage) {
                    data = button.hoverImage;
                }
            }

            if (data != null) {
                if (!draw) {
                    int width = button.rect.right - button.rect.left + 1;
                    int srcOfs = (clippedRect.top - button.rect.top) * width + clippedRect.left
                        - button.rect.left;
                    int destOfs = window.width * clippedRect.top + clippedRect.left;
                    int blitWidth = clippedRect.right - clippedRect.left + 1;
                    int blitHeight = clippedRect.bottom - clippedRect.top + 1;
                    if ((button.flags & ButtonFlags.BUTTON_FLAG_TRANSPARENT.id()) != 0) {
                        Draw.blitBufferToBufferTrans(data, srcOfs, blitWidth, blitHeight, width,
                            window.buffer, destOfs, window.width);
                    } else {
                        Draw.blitBufferToBuffer(data, srcOfs, blitWidth, blitHeight, width, window.buffer,
                            destOfs, window.width);
                    }
                }

                previousImage = button.currentImage;
                button.currentImage = data;

                if (draw) {
                    gnwWinRefresh(window, screenRect, null);
                }
            }
        }

        if (sound) {
            if (previousImage != data) {
                if (data == button.pressedImage && button.pressSoundFunc != null) {
                    button.pressSoundFunc.handle(button.id, button.lefMouseDownEventCode);
                } else if (data == button.normalImage && button.releaseSoundFunc != null) {
                    button.releaseSoundFunc.handle(button.id, button.leftMouseUpEventCode);
                }
            }
        }
    }

    /** Port of {@code _win_button_press_and_release} (0x4D9AA0). */
    public static int winButtonPressAndRelease(int btn) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Window[] windowPtr = new Window[1];
        Button button = buttonGetButton(btn, windowPtr);
        if (button == null) {
            return -1;
        }

        buttonDraw(button, windowPtr[0], button.pressedImage, true, null, true);

        if (button.leftMouseDownProc != null) {
            button.leftMouseDownProc.handle(btn, button.lefMouseDownEventCode);

            if ((button.flags & ButtonFlags.BUTTON_FLAG_0x40.id()) != 0) {
                Input.enqueueInputEvent(button.lefMouseDownEventCode);
            }
        } else {
            if (button.lefMouseDownEventCode != -1) {
                Input.enqueueInputEvent(button.lefMouseDownEventCode);
            }
        }

        buttonDraw(button, windowPtr[0], button.normalImage, true, null, true);

        if (button.leftMouseUpProc != null) {
            button.leftMouseUpProc.handle(btn, button.leftMouseUpEventCode);

            if ((button.flags & ButtonFlags.BUTTON_FLAG_0x40.id()) != 0) {
                Input.enqueueInputEvent(button.leftMouseUpEventCode);
            }
        } else {
            if (button.leftMouseUpEventCode != -1) {
                Input.enqueueInputEvent(button.leftMouseUpEventCode);
            }
        }

        return 0;
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

    /** Port of {@code _win_get_str} (0x4DB478). */
    public static int winGetStr(StringBuilder dest, int length, String title, int x, int y) {
        if (!windowSystemInitialized) {
            return -1;
        }

        int titleWidth = TextFont.fontGetStringWidth(title) + 12;
        if (titleWidth < TextFont.fontGetMonospacedCharacterWidth() * length) {
            titleWidth = TextFont.fontGetMonospacedCharacterWidth() * length;
        }

        int windowWidth = titleWidth + 16;
        if (windowWidth < 160) {
            windowWidth = 160;
        }

        int windowHeight = 5 * TextFont.fontGetLineHeight() + 16;

        int win = windowCreate(x, y, windowWidth, windowHeight, Color.COLOR_COUNT,
            WindowFlags.WINDOW_MODAL.id() | WindowFlags.WINDOW_MOVE_ON_TOP.id());
        if (win == -1) {
            return -1;
        }

        windowDrawBorder(win);

        byte[] windowBuffer = windowGetBuffer(win);

        Draw.bufferFill(windowBuffer, windowWidth * (TextFont.fontGetLineHeight() + 14) + 14,
            windowWidth - 28,
            TextFont.fontGetLineHeight() + 2,
            windowWidth,
            Color.colorTable[gnwWcolor[0]] & 0xFF);
        TextFont.fontDrawText(windowBuffer, windowWidth * 8 + 8, title, windowWidth, windowWidth,
            (Color.colorTable[gnwWcolor[4]] & 0xFF) | Color.DRAW_TEXT_FLAG_NONE);

        Draw.bufferDrawRectShadowed(windowBuffer,
            windowWidth,
            14,
            TextFont.fontGetLineHeight() + 14,
            windowWidth - 14,
            2 * TextFont.fontGetLineHeight() + 16,
            Color.colorTable[gnwWcolor[2]] & 0xFF,
            Color.colorTable[gnwWcolor[1]] & 0xFF);

        winRegisterTextButton(win,
            windowWidth / 2 - 72,
            windowHeight - 8 - TextFont.fontGetLineHeight() - 6,
            -1,
            -1,
            -1,
            Kb.KEY_RETURN,
            "Done",
            0);

        winRegisterTextButton(win,
            windowWidth / 2 + 8,
            windowHeight - 8 - TextFont.fontGetLineHeight() - 6,
            -1,
            -1,
            -1,
            Kb.KEY_ESCAPE,
            "Cancel",
            0);

        windowRefresh(win);

        int rc = winInputStr(win,
            dest,
            length,
            16,
            TextFont.fontGetLineHeight() + 16,
            (Color.colorTable[gnwWcolor[3]] & 0xFF) | Color.DRAW_TEXT_FLAG_NONE,
            Color.colorTable[gnwWcolor[0]] & 0xFF);

        windowDestroy(win);

        return rc;
    }

    /** Port of {@code win_yes_no} (0x4DB920). */
    public static int winYesNo(String question, int x, int y, int color) {
        if (!windowSystemInitialized) {
            return -1;
        }

        int height = 3 * TextFont.fontGetLineHeight() + 16;
        int width = Math.max(TextFont.fontGetStringWidth(question) + 16, 144) + 16;

        int win = windowCreate(x, y, width, height, Color.COLOR_COUNT,
            WindowFlags.WINDOW_MODAL.id() | WindowFlags.WINDOW_MOVE_ON_TOP.id());
        if (win == -1) {
            return -1;
        }

        windowDrawBorder(win);

        Window window = windowGetWindow(win);
        byte[] windowBuffer = window.buffer;

        int textColor;
        if ((color & 0xFF00) != 0) {
            int colorIndex = (color & Color.COLOR_LAST) - 1;
            textColor = (Color.colorTable[gnwWcolor[colorIndex]] & 0xFF) | (color & ~0xFFFF);
        } else {
            textColor = color;
        }

        TextFont.fontDrawText(windowBuffer, 8 * width + 16,
            question,
            width,
            width,
            textColor);

        winRegisterTextButton(win,
            width / 2 - 64,
            height - TextFont.fontGetLineHeight() - 14,
            -1,
            -1,
            -1,
            'y',
            "Yes",
            0);
        winRegisterTextButton(win,
            width / 2 + 16,
            height - TextFont.fontGetLineHeight() - 14,
            -1,
            -1,
            -1,
            'n',
            "No",
            0);
        windowRefresh(win);

        int rc = -1;
        while (rc == -1) {
            Color.fpsLimiter.mark();

            switch (Input.inputGetInput()) {
            case Kb.KEY_ESCAPE:
            case 'n':
            case 'N':
                rc = 0;
                break;
            case Kb.KEY_RETURN:
            case Kb.KEY_SPACE:
            case 'y':
            case 'Y':
                rc = 1;
                break;
            default:
                break;
            }

            Svga.renderPresent();
            Color.fpsLimiter.throttle();
        }

        windowDestroy(win);

        return rc;
    }

    /** Port of {@code _win_msg} (0x4DBA98). */
    public static int winMsg(String string, int x, int y, int color) {
        if (!windowSystemInitialized) {
            return -1;
        }

        int windowHeight = 3 * TextFont.fontGetLineHeight() + 16;

        int windowWidth = TextFont.fontGetStringWidth(string) + 16;
        if (windowWidth < 80) {
            windowWidth = 80;
        }

        windowWidth += 16;

        int win = windowCreate(x, y, windowWidth, windowHeight, Color.COLOR_COUNT,
            WindowFlags.WINDOW_MODAL.id() | WindowFlags.WINDOW_MOVE_ON_TOP.id());
        if (win == -1) {
            return -1;
        }

        windowDrawBorder(win);

        Window window = windowGetWindow(win);
        byte[] windowBuffer = window.buffer;

        int textColor;
        if ((color & 0xFF00) != 0) {
            int colorIndex = (color & Color.COLOR_LAST) - 1;
            textColor = (Color.colorTable[gnwWcolor[colorIndex]] & 0xFF) | (color & ~0xFFFF);
        } else {
            textColor = color;
        }

        TextFont.fontDrawText(windowBuffer, windowWidth * 8 + 16, string, windowWidth, windowWidth,
            textColor);

        winRegisterTextButton(win,
            windowWidth / 2 - 32,
            windowHeight - 8 - TextFont.fontGetLineHeight() - 6,
            -1,
            -1,
            -1,
            Kb.KEY_ESCAPE,
            "Done",
            0);

        windowRefresh(win);

        while (Input.inputGetInput() != Kb.KEY_ESCAPE) {
            Color.fpsLimiter.mark();
            Svga.renderPresent();
            Color.fpsLimiter.throttle();
        }

        windowDestroy(win);

        return 0;
    }

    /** Port of {@code _create_pull_down} (0x4DBC34). */
    public static int createPullDown(String[] stringList, int stringListLength, int x, int y,
            int foregroundColor, int backgroundColor, Geometry.Rect rect) {
        int windowHeight = stringListLength * TextFont.fontGetLineHeight() + 16;
        int windowWidth = winWidthNeeded(stringList, stringListLength) + 4;
        if (windowHeight < 2 || windowWidth < 2) {
            return -1;
        }

        int win = windowCreate(x, y, windowWidth, windowHeight, backgroundColor,
            WindowFlags.WINDOW_MODAL.id() | WindowFlags.WINDOW_MOVE_ON_TOP.id());
        if (win == -1) {
            return -1;
        }

        String[] items = stringList;
        if (stringListLength != stringList.length) {
            items = new String[stringListLength];
            System.arraycopy(stringList, 0, items, 0, stringListLength);
        }
        winText(win, items, windowWidth - 4, 2, 8, foregroundColor);
        windowDrawRect(win, 0, 0, windowWidth - 1, windowHeight - 1,
            Color.colorTable[Color.rgb555(0x000000)] & 0xFF);
        windowDrawRect(win, 1, 1, windowWidth - 2, windowHeight - 2, foregroundColor);
        windowRefresh(win);
        windowGetRect(win, rect);

        return win;
    }

    /**
     * Port of {@code _wd} (0x51E414): the debug-window id, -1 while closed.
     *
     * <p>Test seam: {@link #resetForTests} sets this back to -1 <em>without</em>
     * destroying any window (C has no reset; the windows themselves are torn
     * down by {@code resetForTests}, which mirrors a fresh process).
     */
    private static int debugWindow = -1;

    /**
     * Port of {@code _win_debug} (0x4DC30C, window_manager_private.cc).
     *
     * <p>The text cursor ({@code _currx}/{@code _curry}) reuses this class's
     * {@code currx}/{@code curry} fields: in C they are touched only by the
     * debug window, and nothing else in the port reads them. Raw color ints
     * ({@code 0x100 | n}) flow through, remapped inside
     * {@code windowFill}/{@code windowDrawText} exactly like C.
     */
    public static int winDebug(String string) {
        if (!windowSystemInitialized) {
            return -1;
        }

        // CE: Debug window metrics were designed for default DOS-style font (0).
        // We don't expect caller to properly set one, so without manually forcing
        // it debug window might contain mixed fonts.
        int oldFont = TextFont.fontGetCurrent();
        TextFont.fontSetCurrent(0);

        int lineHeight = TextFont.fontGetLineHeight();

        int winWidth = Settings.settings.debug.window_width;
        int winHeight = Settings.settings.debug.window_height;
        if (debugWindow == -1) {
            debugWindow = windowCreate(80, 80, winWidth, winHeight, 256,
                WindowFlags.WINDOW_MOVE_ON_TOP.id());
            if (debugWindow == -1) {
                return -1;
            }

            windowDrawBorder(debugWindow);

            Window window = windowGetWindow(debugWindow);
            byte[] windowBuffer = window.buffer;

            windowFill(debugWindow, 8, 8, winWidth - 16, lineHeight, 0x100 | 1);

            windowDrawText(debugWindow,
                "Debug",
                0,
                (winWidth - TextFont.fontGetStringWidth("Debug")) / 2,
                8,
                Color.DRAW_TEXT_FLAG_NO_BG | 0x100 | 4);

            Draw.bufferDrawRectShadowed(windowBuffer,
                winWidth,
                8,
                8,
                winWidth - 9,
                lineHeight + 8,
                Color.colorTable[gnwWcolor[2]] & 0xFF,
                Color.colorTable[gnwWcolor[1]] & 0xFF);

            windowFill(debugWindow, 9, 26, winWidth - 18, winHeight - 57, 0x100 | 1);

            Draw.bufferDrawRectShadowed(windowBuffer,
                winWidth,
                8,
                25,
                winWidth - 9,
                lineHeight + winHeight - 47,
                Color.colorTable[gnwWcolor[2]] & 0xFF,
                Color.colorTable[gnwWcolor[1]] & 0xFF);

            currx = 9;
            curry = 26;

            int btn = winRegisterTextButton(debugWindow,
                (winWidth - TextFont.fontGetStringWidth("Close")) / 2,
                winHeight - 8 - lineHeight - 6,
                -1,
                -1,
                -1,
                -1,
                "Close",
                0);
            buttonSetMouseCallbacks(btn, null, null, null, WindowManager::winDebugDelete);

            buttonCreate(debugWindow,
                8,
                8,
                winWidth - 16,
                lineHeight,
                -1,
                -1,
                -1,
                -1,
                null,
                null,
                null,
                ButtonFlags.BUTTON_DRAG_HANDLE.id());
        }

        int pch = 0;
        while (pch < string.length() && string.charAt(pch) != '\0') {
            char ch = string.charAt(pch);
            int characterWidth = TextFont.fontGetCharacterWidth(ch & 0xFF);
            if (ch == '\n' || currx + characterWidth > winWidth - 9) {
                currx = 9;
                curry += lineHeight;
            }

            while (winHeight - 32 - curry < lineHeight) {
                Window window = windowGetWindow(debugWindow);
                byte[] windowBuffer = window.buffer;
                Draw.blitBufferToBuffer(windowBuffer,
                    lineHeight * winWidth + winWidth * 26 + 9,
                    winWidth - 18,
                    winHeight - 58 - lineHeight - 1,
                    winWidth,
                    windowBuffer,
                    winWidth * 26 + 9,
                    winWidth);
                curry -= lineHeight;
                windowFill(debugWindow, 9, curry, winWidth - 18, lineHeight, 0x100 | 1);
            }

            if (ch != '\n') {
                windowDrawText(debugWindow, String.valueOf(ch), 0, currx, curry,
                    Color.DRAW_TEXT_FLAG_NO_BG | 0x100 | 4);
                currx += characterWidth + TextFont.fontGetLetterSpacing();
            }

            pch++;
        }

        windowRefresh(debugWindow);

        // CE: Restore font.
        TextFont.fontSetCurrent(oldFont);

        return 0;
    }

    /** Port of {@code _win_debug_delete} (0x4DC65C). */
    public static void winDebugDelete(int btn, int keyCode) {
        windowDestroy(debugWindow);
        debugWindow = -1;
    }

    /** Port of {@code _win_pull_down} (window_manager_private.cc). */
    public static int winPullDown(String[] items, int itemsLength, int x, int y, int color) {
        if (!windowSystemInitialized) {
            return -1;
        }

        Geometry.Rect rect = new Geometry.Rect();
        int win = createPullDown(items, itemsLength, x, y, color,
            (Color.colorTable[gnwWcolor[0]] & 0xFF) | Color.DRAW_TEXT_FLAG_NONE, rect);
        if (win == -1) {
            return -1;
        }

        return processPullDown(win, rect, items, itemsLength, color,
            (Color.colorTable[gnwWcolor[0]] & 0xFF) | Color.DRAW_TEXT_FLAG_NONE, null, -1);
    }

    /** Port of {@code process_pull_down} (0x4DBD04, window_manager_private.cc). */
    public static int processPullDown(int win, Geometry.Rect rect, String[] items, int itemsLength,
            int foregroundColor, int backgroundColor, MenuBar menuBar, int pulldownIndex) {
        if (menuBar != null) {
            Window parentWindow = windowGetWindow(menuBar.win);
            byte[] parentWindowBuffer = parentWindow.buffer;
            MenuPulldown pulldown = menuBar.pulldowns[pulldownIndex];

            int swapX = pulldown.rect.left;
            int swapY = pulldown.rect.top;
            int swapWidth = pulldown.rect.right - swapX + 1;
            int swapHeight = pulldown.rect.bottom - swapY + 1;

            int color1 = resolveMenuColor(menuBar.foregroundColor);
            int color2 = resolveMenuColor(menuBar.backgroundColor);

            swapPulldownColors(menuBar.win, parentWindowBuffer, swapX, swapY, swapWidth, swapHeight,
                color1, color2);
            windowRefreshRect(menuBar.win, pulldown.rect);
        }

        Window window = windowGetWindow(win);
        byte[] windowBuffer = window.buffer;
        int width = Geometry.rectGetWidth(rect);
        int height = Geometry.rectGetHeight(rect);

        int focusedIndex = -1;
        int rc = -1;
        int[] pos = new int[2];
        Mouse.mouseGetPosition(pos);
        int mx1 = pos[0];
        int my1 = pos[1];
        int input;

        while (true) {
            Color.fpsLimiter.mark();

            input = Input.inputGetInput();
            if (input != -1) {
                break;
            }

            Mouse.mouseGetPosition(pos);
            int mx2 = pos[0];
            int my2 = pos[1];

            if (mx2 < mx1 - 4 || mx2 > mx1 + 4 || my2 < my1 - 4 || my2 > my1 + 4) {
                break;
            }

            Svga.renderPresent();
            Color.fpsLimiter.throttle();
        }

        while (true) {
            Color.fpsLimiter.mark();

            Mouse.mouseGetPosition(pos);
            int mx2 = pos[0];
            int my2 = pos[1];

            if (input == -2) {
                if (menuBar != null
                    && Mouse.mouseClickIn(menuBar.rect.left, menuBar.rect.top,
                        menuBar.rect.right, menuBar.rect.bottom)) {
                    int index;
                    for (index = 0; index < menuBar.pulldownsLength; index++) {
                        MenuPulldown pulldown = menuBar.pulldowns[index];
                        if (Mouse.mouseClickIn(pulldown.rect.left, pulldown.rect.top,
                            pulldown.rect.right, pulldown.rect.bottom)) {
                            break;
                        }
                    }

                    if (index < menuBar.pulldownsLength && index != pulldownIndex) {
                        rc = -2 - index;
                        break;
                    }
                }
            }

            int mouseEvent = Mouse.mouseGetEvent();
            if ((mouseEvent & Mouse.EVENT_LEFT_BUTTON_UP) != 0
                || ((mouseEvent & Mouse.EVENT_LEFT_BUTTON_DOWN) != 0
                    && (mouseEvent & Mouse.EVENT_LEFT_BUTTON_REPEAT) == 0)) {
                if (Mouse.mouseClickIn(rect.left, rect.top + 8, rect.right, rect.bottom - 9)) {
                    rc = focusedIndex;
                } else {
                    rc = -1;
                }
                break;
            }

            boolean done = false;
            if (input == Kb.KEY_ESCAPE) {
                rc = -1;
                done = true;
            } else if (input == Kb.KEY_RETURN) {
                rc = focusedIndex;
                done = true;
            } else if (input == Kb.KEY_ARROW_LEFT) {
                if (menuBar != null && pulldownIndex > 0) {
                    rc = -2 - (pulldownIndex - 1);
                    done = true;
                }
            } else if (input == Kb.KEY_ARROW_RIGHT) {
                if (menuBar != null && pulldownIndex < menuBar.pulldownsLength - 1) {
                    rc = -2 - (pulldownIndex + 1);
                    done = true;
                }
            } else if (input == Kb.KEY_ARROW_UP) {
                while (focusedIndex > 0) {
                    focusedIndex--;

                    if (!items[focusedIndex].isEmpty()) {
                        break;
                    }
                }
                input = -3;
            } else if (input == Kb.KEY_ARROW_DOWN) {
                while (focusedIndex < itemsLength - 1) {
                    focusedIndex++;

                    if (!items[focusedIndex].isEmpty()) {
                        break;
                    }
                }
                input = -3;
            } else {
                if (mx2 != mx1 || my2 != my1) {
                    if (Mouse.mouseClickIn(rect.left, rect.top + 8, rect.right, rect.bottom - 9)) {
                        input = (my2 - rect.top - 8) / TextFont.fontGetLineHeight();
                        if (input != -1) {
                            focusedIndex = !items[input].isEmpty() ? input : -1;
                            input = -3;
                        }
                    }

                    mx1 = mx2;
                    my1 = my2;
                }
            }

            if (done) {
                break;
            }

            if (input == -3) {
                windowFill(win, 2, 8, width - 4, height - 16, backgroundColor);
                winText(win, sliceItems(items, 0, itemsLength), width - 4, 2, 8, foregroundColor);

                if (focusedIndex != -1) {
                    Draw.lightenBuf(windowBuffer,
                        (focusedIndex * TextFont.fontGetLineHeight() + 8) * width + 2,
                        width - 4, TextFont.fontGetLineHeight(), width);
                }

                windowRefresh(win);
            }

            input = Input.inputGetInput();

            Svga.renderPresent();
            Color.fpsLimiter.throttle();
        }

        if (menuBar != null) {
            Window parentWindow = windowGetWindow(menuBar.win);
            byte[] parentWindowBuffer = parentWindow.buffer;
            MenuPulldown pulldown = menuBar.pulldowns[pulldownIndex];

            int swapX = pulldown.rect.left;
            int swapY = pulldown.rect.top;
            int swapWidth = pulldown.rect.right - swapX + 1;
            int swapHeight = pulldown.rect.bottom - swapY + 1;

            int color1 = resolveMenuColor(menuBar.foregroundColor);
            int color2 = resolveMenuColor(menuBar.backgroundColor);

            swapPulldownColors(menuBar.win, parentWindowBuffer, swapX, swapY, swapWidth, swapHeight,
                color1, color2);
            windowRefreshRect(menuBar.win, pulldown.rect);

            Svga.renderPresent();
        }

        windowDestroy(win);

        return rc;
    }

    /** Port of {@code _win_list_select} (window_manager_private.cc). */
    public static int winListSelect(String title, String[] fileList, int fileListLength,
            ListSelectionHandler callback, int x, int y, int color) {
        return winListSelectAt(title, fileList, fileListLength, callback, x, y, color, 0);
    }

    /** Port of {@code _win_list_select_at} (0x4DA70C, window_manager_private.cc). */
    public static int winListSelectAt(String title, String[] items, int itemsLength,
            ListSelectionHandler callback, int x, int y, int color, int start) {
        if (!windowSystemInitialized) {
            return -1;
        }

        int listViewWidth = winWidthNeeded(items, itemsLength);
        int windowWidth = listViewWidth + 16;

        int titleWidth = TextFont.fontGetStringWidth(title);
        if (titleWidth > windowWidth) {
            windowWidth = titleWidth;
            listViewWidth = titleWidth - 16;
        }

        windowWidth += 20;

        int win = -1;
        int windowHeight = 0;
        int listViewCapacity = 10;
        for (int heightMultiplier = 13; heightMultiplier > 8; heightMultiplier--) {
            windowHeight = heightMultiplier * TextFont.fontGetLineHeight() + 22;
            win = windowCreate(x, y, windowWidth, windowHeight, Color.COLOR_COUNT,
                WindowFlags.WINDOW_MODAL.id() | WindowFlags.WINDOW_MOVE_ON_TOP.id());
            if (win != -1) {
                break;
            }
            listViewCapacity--;
        }

        if (win == -1) {
            return -1;
        }

        Window window = windowGetWindow(win);
        Geometry.Rect windowRect = window.rect;
        byte[] windowBuffer = window.buffer;

        int lineHeight = TextFont.fontGetLineHeight();
        int light = Color.colorTable[gnwWcolor[1]] & 0xFF;
        int shadow = Color.colorTable[gnwWcolor[2]] & 0xFF;
        int fill = Color.colorTable[gnwWcolor[0]] & 0xFF;

        Draw.bufferDrawRect(windowBuffer, windowWidth, 0, 0, windowWidth - 1, windowHeight - 1,
            Color.colorTable[Color.rgb555(0x000000)] & 0xFF);
        Draw.bufferDrawRectShadowed(windowBuffer, windowWidth, 1, 1, windowWidth - 2, windowHeight - 2,
            light, shadow);

        Draw.bufferFill(windowBuffer, windowWidth * 5 + 5, windowWidth - 11, lineHeight + 3,
            windowWidth, fill);

        TextFont.fontDrawText(
            windowBuffer, windowWidth / 2 + 8 * windowWidth - TextFont.fontGetStringWidth(title) / 2,
            title, windowWidth, windowWidth,
            (Color.colorTable[gnwWcolor[3]] & 0xFF) | Color.DRAW_TEXT_FLAG_NONE);

        Draw.bufferDrawRectShadowed(windowBuffer, windowWidth, 5, 5, windowWidth - 6,
            lineHeight + 8, shadow, light);

        int listViewX = 8;
        int listViewY = lineHeight + 16;
        int listViewBase = windowWidth * listViewY + listViewX;
        int listViewMaxY = listViewCapacity * lineHeight + listViewY;

        Draw.bufferFill(windowBuffer, listViewBase + windowWidth * (-2) + (-3),
            listViewWidth + listViewX - 2, listViewCapacity * lineHeight + 2, windowWidth, fill);

        int scrollOffset = start;
        if (start < 0 || start >= itemsLength) {
            scrollOffset = 0;
        }

        // Relative to `scrollOffset`.
        int selectedItemIndex;
        if (itemsLength - scrollOffset < listViewCapacity) {
            int newScrollOffset = itemsLength - listViewCapacity;
            if (newScrollOffset < 0) {
                newScrollOffset = 0;
            }
            int oldScrollOffset = scrollOffset;
            scrollOffset = newScrollOffset;
            selectedItemIndex = oldScrollOffset - newScrollOffset;
        } else {
            selectedItemIndex = 0;
        }

        // NOTE: upstream draws `_win_text(items + start, min(itemsLength, capacity))`
        // here (and `items + scrollOffset` on the -4 repaint path). With start > 0
        // and a short list that pointer/count pair overreads the item array (a
        // segfault in C; `sliceItems` throws AIOOBE on the same misuse). The
        // golden vectors never exercise it (offsets only run on the 12-item list).
        winText(win, sliceItems(items, start, Math.min(itemsLength, listViewCapacity)),
            listViewWidth, listViewX, listViewY, color | Color.DRAW_TEXT_FLAG_NO_BG);

        Draw.lightenBuf(windowBuffer, listViewBase + windowWidth * selectedItemIndex * lineHeight,
            listViewWidth, lineHeight, windowWidth);

        Draw.bufferDrawRectShadowed(windowBuffer, windowWidth, 5, listViewY - 3,
            listViewWidth + 10, listViewMaxY, shadow, light);

        winRegisterTextButton(win, windowWidth - 25, listViewY - 3, -1, -1, Kb.KEY_ARROW_UP, -1,
            "\u0018", 0);

        winRegisterTextButton(win, windowWidth - 25, listViewMaxY - lineHeight - 5, -1, -1,
            Kb.KEY_ARROW_DOWN, -1, "\u0019", 0);

        winRegisterTextButton(win, windowWidth / 2 - 32, windowHeight - 8 - lineHeight - 6, -1, -1,
            -1, Kb.KEY_ESCAPE, "Done", 0);

        int scrollbarX = windowWidth - 21;
        int scrollbarY = listViewY + lineHeight + 7;
        int scrollbarKnobSize = 14;
        int scrollbarHeight = listViewMaxY - scrollbarY;

        Draw.bufferFill(windowBuffer, windowWidth * scrollbarY + scrollbarX, scrollbarKnobSize + 1,
            scrollbarHeight - lineHeight - 8, windowWidth, fill);

        buttonCreate(win, scrollbarX, scrollbarY, scrollbarKnobSize + 1,
            scrollbarHeight - lineHeight - 8, -1, -1, 2048, -1, null, null, null, 0);

        Draw.bufferDrawRectShadowed(windowBuffer, windowWidth, windowWidth - 22, scrollbarY - 1,
            scrollbarX + scrollbarKnobSize + 1, listViewMaxY - lineHeight - 9, shadow, light);
        Draw.bufferDrawRectShadowed(windowBuffer, windowWidth, scrollbarX, scrollbarY,
            scrollbarX + scrollbarKnobSize, scrollbarY + scrollbarKnobSize, light, shadow);

        Draw.lightenBuf(windowBuffer, windowWidth * scrollbarY + scrollbarX, scrollbarKnobSize,
            scrollbarKnobSize, windowWidth);

        for (int index = 0; index < listViewCapacity; index++) {
            buttonCreate(win, listViewX, listViewY + index * lineHeight, listViewWidth, lineHeight,
                512 + index, -1, 1024 + index, -1, null, null, null, 0);
        }

        buttonCreate(win, 0, 0, windowWidth, lineHeight + 8, -1, -1, -1, -1, null, null, null,
            ButtonFlags.BUTTON_DRAG_HANDLE.id());

        windowRefresh(win);

        int absoluteSelectedItemIndex = -1;

        // Relative to `scrollOffset`.
        int previousSelectedItemIndex = -1;
        while (true) {
            Color.fpsLimiter.mark();

            int keyCode = Input.inputGetInput();
            int[] mousePos = new int[2];
            Mouse.mouseGetPosition(mousePos);
            int mouseY = mousePos[1];

            if (keyCode == Kb.KEY_RETURN
                || (keyCode >= 1024 && keyCode < listViewCapacity + 1024)) {
                if (selectedItemIndex != -1) {
                    absoluteSelectedItemIndex = scrollOffset + selectedItemIndex;
                    if (absoluteSelectedItemIndex < itemsLength) {
                        if (callback == null) {
                            break;
                        }

                        callback.handle(items, absoluteSelectedItemIndex);
                    }
                    absoluteSelectedItemIndex = -1;
                }
            } else if (keyCode == 2048) {
                if (window.rect.top + scrollbarY > mouseY) {
                    keyCode = Kb.KEY_PAGE_UP;
                } else if (window.rect.top + scrollbarKnobSize + scrollbarY < mouseY) {
                    keyCode = Kb.KEY_PAGE_DOWN;
                }
            }

            if (keyCode == Kb.KEY_ESCAPE) {
                break;
            }

            if (keyCode >= 512 && keyCode < listViewCapacity + 512) {
                int itemIndex = keyCode - 512;
                if (itemIndex != selectedItemIndex && itemIndex < itemsLength) {
                    previousSelectedItemIndex = selectedItemIndex;
                    selectedItemIndex = itemIndex;
                    keyCode = -3;
                } else {
                    continue;
                }
            } else {
                if (keyCode == Kb.KEY_HOME) {
                    if (scrollOffset > 0) {
                        keyCode = -4;
                        scrollOffset = 0;
                    }
                } else if (keyCode == Kb.KEY_ARROW_UP) {
                    if (selectedItemIndex > 0) {
                        keyCode = -3;
                        previousSelectedItemIndex = selectedItemIndex;
                        selectedItemIndex -= 1;
                    } else {
                        if (scrollOffset > 0) {
                            keyCode = -4;
                            scrollOffset -= 1;
                        }
                    }
                } else if (keyCode == Kb.KEY_PAGE_UP) {
                    if (scrollOffset > 0) {
                        scrollOffset -= listViewCapacity;
                        if (scrollOffset < 0) {
                            scrollOffset = 0;
                        }
                        keyCode = -4;
                    }
                } else if (keyCode == Kb.KEY_END) {
                    if (scrollOffset < itemsLength - listViewCapacity) {
                        keyCode = -4;
                        scrollOffset = itemsLength - listViewCapacity;
                    }
                } else if (keyCode == Kb.KEY_ARROW_DOWN) {
                    if (selectedItemIndex < listViewCapacity - 1
                        && selectedItemIndex < itemsLength - 1) {
                        keyCode = -3;
                        previousSelectedItemIndex = selectedItemIndex;
                        selectedItemIndex += 1;
                    } else {
                        if (scrollOffset + listViewCapacity < itemsLength) {
                            keyCode = -4;
                            scrollOffset += 1;
                        }
                    }
                } else if (keyCode == Kb.KEY_PAGE_DOWN) {
                    if (scrollOffset < itemsLength - listViewCapacity) {
                        scrollOffset += listViewCapacity;
                        if (scrollOffset > itemsLength - listViewCapacity) {
                            scrollOffset = itemsLength - listViewCapacity;
                        }
                        keyCode = -4;
                    }
                } else {
                    if (itemsLength > listViewCapacity) {
                        if ((keyCode >= 'a' && keyCode <= 'z')
                            || (keyCode >= 'A' && keyCode <= 'Z')) {
                            int found = findFirstLetter(keyCode, items, itemsLength);
                            if (found != -1) {
                                scrollOffset = found;
                                if (scrollOffset > itemsLength - listViewCapacity) {
                                    scrollOffset = itemsLength - listViewCapacity;
                                }
                                keyCode = -4;
                                selectedItemIndex = found - scrollOffset;
                            }
                        }
                    }
                }
            }

            if (keyCode == -4) {
                Draw.bufferFill(windowBuffer, listViewBase, listViewWidth,
                    listViewMaxY - listViewY, windowWidth, fill);

                winText(win, sliceItems(items, scrollOffset, Math.min(itemsLength, listViewCapacity)),
                    listViewWidth, listViewX, listViewY, color | Color.DRAW_TEXT_FLAG_NO_BG);

                Draw.lightenBuf(windowBuffer,
                    listViewBase + windowWidth * selectedItemIndex * lineHeight, listViewWidth,
                    lineHeight, windowWidth);

                if (itemsLength > listViewCapacity) {
                    Draw.bufferFill(windowBuffer, windowWidth * scrollbarY + scrollbarX,
                        scrollbarKnobSize + 1, scrollbarKnobSize + 1, windowWidth, fill);

                    scrollbarY = (scrollOffset * (listViewMaxY - listViewY - 2 * lineHeight - 16
                        - scrollbarKnobSize - 1)) / (itemsLength - listViewCapacity)
                        + listViewY + lineHeight + 7;

                    Draw.bufferDrawRectShadowed(windowBuffer, windowWidth, scrollbarX, scrollbarY,
                        scrollbarX + scrollbarKnobSize, scrollbarY + scrollbarKnobSize, light,
                        shadow);

                    Draw.lightenBuf(windowBuffer, windowWidth * scrollbarY + scrollbarX,
                        scrollbarKnobSize, scrollbarKnobSize, windowWidth);

                    gnwWinRefresh(window, windowRect, null);
                }
            } else if (keyCode == -3) {
                Geometry.Rect itemRect = new Geometry.Rect();
                itemRect.left = windowRect.left + listViewX;
                itemRect.right = itemRect.left + listViewWidth;

                if (previousSelectedItemIndex != -1) {
                    itemRect.top = windowRect.top + listViewY
                        + previousSelectedItemIndex * lineHeight;
                    itemRect.bottom = itemRect.top + lineHeight;

                    Draw.bufferFill(windowBuffer,
                        listViewBase + windowWidth * previousSelectedItemIndex * lineHeight,
                        listViewWidth, lineHeight, windowWidth, fill);

                    int textColor;
                    if ((color & 0xFF00) != 0) {
                        int colorIndex = (color & Color.COLOR_LAST) - 1;
                        textColor = (Color.colorTable[gnwWcolor[colorIndex]] & 0xFF)
                            | (color & ~0xFFFF);
                    } else {
                        textColor = color;
                    }

                    TextFont.fontDrawText(windowBuffer,
                        listViewBase + windowWidth * previousSelectedItemIndex * lineHeight,
                        items[scrollOffset + previousSelectedItemIndex], windowWidth, windowWidth,
                        textColor);

                    gnwWinRefresh(window, itemRect, null);
                }

                if (selectedItemIndex != -1) {
                    itemRect.top = windowRect.top + listViewY + selectedItemIndex * lineHeight;
                    itemRect.bottom = itemRect.top + lineHeight;

                    Draw.lightenBuf(windowBuffer,
                        listViewBase + windowWidth * selectedItemIndex * lineHeight, listViewWidth,
                        lineHeight, windowWidth);

                    gnwWinRefresh(window, itemRect, null);
                }
            }

            Svga.renderPresent();
            Color.fpsLimiter.throttle();
        }

        windowDestroy(win);

        return absoluteSelectedItemIndex;
    }

    /** C {@code items + offset} slice for the explicit-length ports above. */
    private static String[] sliceItems(String[] items, int offset, int count) {
        String[] slice = new String[count];
        System.arraycopy(items, offset, slice, 0, count);
        return slice;
    }

    /** Menu-bar color resolve shared by the {@code process_pull_down} swap pair. */
    private static int resolveMenuColor(int color) {
        if ((color & 0xFF00) != 0) {
            int colorIndex = (color & Color.COLOR_LAST) - 1;
            color = (Color.colorTable[gnwWcolor[colorIndex]] & 0xFF) | (color & ~0xFFFF);
        }
        return color;
    }

    /**
     * Pulldown highlight swap shared by the {@code process_pull_down} swap pair.
     *
     * <p>NOTE: pulldown titles can sit half above a short menu bar (titleY goes
     * negative), so the swap rect may start above the parent buffer. C++ swaps
     * through a raw pointer and corrupts the heap there (unobservable in the
     * vectors); clip to the window instead, like TextFont does for the titles.
     */
    private static void swapPulldownColors(int win, byte[] parentWindowBuffer, int swapX, int swapY,
            int swapWidth, int swapHeight, int color1, int color2) {
        int parentWidth = windowGetWidth(win);
        int parentHeight = windowGetHeight(win);
        int clipLeft = Math.max(swapX, 0);
        int clipTop = Math.max(swapY, 0);
        int clipRight = Math.min(swapX + swapWidth - 1, parentWidth - 1);
        int clipBottom = Math.min(swapY + swapHeight - 1, parentHeight - 1);
        if (clipRight >= clipLeft && clipBottom >= clipTop) {
            Draw.swapColorBuf(parentWindowBuffer, clipTop * parentWidth + clipLeft,
                clipRight - clipLeft + 1, clipBottom - clipTop + 1, parentWidth,
                color1 & Color.COLOR_LAST, color2 & Color.COLOR_LAST);
        }
    }

    /** Port of {@code _win_input_str} (0x4DCA5C). */
    public static int winInputStr(int win, StringBuilder dest, int maxLength, int x, int y,
            int textColor, int backgroundColor) {
        Window window = windowGetWindow(win);
        int baseOfs = window.width * y + x;
        byte[] buffer = window.buffer;

        int cursorPos = dest.length();
        dest.append('_');

        int lineHeight = TextFont.fontGetLineHeight();
        int stringWidth = TextFont.fontGetStringWidth(dest.toString());
        Draw.bufferFill(buffer, baseOfs, stringWidth, lineHeight, window.width,
            backgroundColor & Color.COLOR_LAST);
        TextFont.fontDrawText(buffer, baseOfs, dest.toString(), stringWidth, window.width, textColor);

        Geometry.Rect dirtyRect = new Geometry.Rect();
        dirtyRect.left = window.rect.left + x;
        dirtyRect.top = window.rect.top + y;
        dirtyRect.right = dirtyRect.left + stringWidth;
        dirtyRect.bottom = dirtyRect.top + lineHeight;
        gnwWinRefresh(window, dirtyRect, null);

        boolean isFirstKey = true;
        for (; cursorPos <= maxLength; cursorPos++) {
            Color.fpsLimiter.mark();

            int keyCode = Input.inputGetInput();
            if (keyCode != -1) {
                if (keyCode == Kb.KEY_ESCAPE) {
                    dest.setLength(cursorPos);
                    return -1;
                }

                if (keyCode == Kb.KEY_BACKSPACE) {
                    if (cursorPos > 0) {
                        stringWidth = TextFont.fontGetStringWidth(dest.toString());

                        if (isFirstKey) {
                            Draw.bufferFill(buffer, baseOfs, stringWidth, lineHeight, window.width,
                                backgroundColor & Color.COLOR_LAST);

                            dirtyRect.left = window.rect.left + x;
                            dirtyRect.top = window.rect.top + y;
                            dirtyRect.right = dirtyRect.left + stringWidth;
                            dirtyRect.bottom = dirtyRect.top + lineHeight;
                            gnwWinRefresh(window, dirtyRect, null);

                            dest.setLength(0);
                            dest.append('_');
                            cursorPos = 1;
                        } else {
                            dest.setCharAt(cursorPos, ' ');
                            dest.setCharAt(cursorPos - 1, '_');
                        }

                        Draw.bufferFill(buffer, baseOfs, stringWidth, lineHeight, window.width,
                            backgroundColor & Color.COLOR_LAST);
                        TextFont.fontDrawText(buffer, baseOfs, dest.toString(), stringWidth,
                            window.width, textColor);

                        dirtyRect.left = window.rect.left + x;
                        dirtyRect.top = window.rect.top + y;
                        dirtyRect.right = dirtyRect.left + stringWidth;
                        dirtyRect.bottom = dirtyRect.top + lineHeight;
                        gnwWinRefresh(window, dirtyRect, null);

                        dest.setLength(cursorPos);
                        cursorPos -= 2;

                        isFirstKey = false;
                    } else {
                        cursorPos--;
                    }
                } else if (keyCode == Kb.KEY_RETURN) {
                    break;
                } else {
                    if (cursorPos == maxLength) {
                        cursorPos = maxLength - 1;
                    } else {
                        if (keyCode > 0 && keyCode < 256) {
                            dest.setCharAt(cursorPos, (char) keyCode);
                            dest.append('_');

                            int w = TextFont.fontGetStringWidth(dest.toString());
                            Draw.bufferFill(buffer, baseOfs, w, lineHeight, window.width,
                                backgroundColor & Color.COLOR_LAST);
                            TextFont.fontDrawText(buffer, baseOfs, dest.toString(), w, window.width,
                                textColor);

                            dirtyRect.left = window.rect.left + x;
                            dirtyRect.top = window.rect.top + y;
                            dirtyRect.right = dirtyRect.left + w;
                            dirtyRect.bottom = dirtyRect.top + lineHeight;
                            gnwWinRefresh(window, dirtyRect, null);

                            isFirstKey = false;
                        } else {
                            cursorPos--;
                        }
                    }
                }
            } else {
                cursorPos--;
            }

            Svga.renderPresent();
            Color.fpsLimiter.throttle();
        }

        dest.setLength(cursorPos);

        return 0;
    }

    /** Port of {@code win_get_num_i} (0x4DCD68). */
    public static int winGetNumI(int[] value, int min, int max, boolean clear, String title, int x,
            int y) {
        if (!windowSystemInitialized) {
            return -1;
        }

        if (max < min) {
            return -1;
        }

        if (value[0] < min) {
            value[0] = min;
        } else if (value[0] > max) {
            value[0] = max;
        }

        int original = value[0];
        int maxCharsWcursor = calcMaxFieldCharsWcursor(min, max);
        if (maxCharsWcursor == -1) {
            return -1;
        }

        int inputWidth = TextFont.fontGetMonospacedCharacterWidth() * maxCharsWcursor;

        int width = TextFont.fontGetStringWidth(title);
        if (width < inputWidth) {
            width = inputWidth;
        }

        width += 16;
        if (width < 160) {
            width = 160;
        }

        int height = 5 * TextFont.fontGetLineHeight() + 16;
        int inputX = (width - inputWidth) / 2;
        int inputY = TextFont.fontGetLineHeight();
        int inputHeight = TextFont.fontGetLineHeight() + 2;

        int win = windowCreate(x, y, width, height, Color.COLOR_COUNT,
            WindowFlags.WINDOW_MODAL.id() | WindowFlags.WINDOW_MOVE_ON_TOP.id());
        if (win == -1) {
            return -1;
        }

        windowDrawBorder(win);
        windowFill(win, inputX, inputY + 14, inputWidth, inputHeight, 0x100 | 1);
        windowDrawText(win, title, width - 16, 8, 8, 0x100 | 5);

        Draw.bufferDrawRectShadowed(windowGetBuffer(win),
            width,
            inputX - 2,
            inputY + 12,
            inputX + inputWidth + 1,
            inputY + 14 + inputHeight - 1,
            Color.colorTable[gnwWcolor[2]] & 0xFF,
            Color.colorTable[gnwWcolor[1]] & 0xFF);

        winRegisterTextButton(win,
            width / 2 - 72,
            height - TextFont.fontGetLineHeight() - 14,
            -1,
            -1,
            -1,
            Kb.KEY_RETURN,
            "Done",
            0);

        winRegisterTextButton(win,
            width / 2 + 16,
            height - TextFont.fontGetLineHeight() - 14,
            -1,
            -1,
            -1,
            Kb.KEY_ESCAPE,
            "Cancel",
            0);

        String hint = String.format("Please enter a number between %d and %d.", min, max);
        windowRefresh(win);

        int rc;
        while (true) {
            rc = getNumI(win, value, maxCharsWcursor, clear, min < 0, inputX, inputY + 14);
            if (value[0] >= min && value[0] <= max) {
                break;
            }

            if (rc == -1) {
                break;
            }

            winMsg(hint, x - 70, y + 100, 0x100 | 6);
            value[0] = original;
        }

        windowDestroy(win);

        return rc;
    }

    /** Port of {@code _calc_max_field_chars_wcursor} (0x4DD03C). */
    public static int calcMaxFieldCharsWcursor(int value1, int value2) {
        int len1 = Integer.toString(value1).length();
        int len2 = Integer.toString(value2).length();
        return Math.max(len1, len2) + 1;
    }

    /** Port of {@code get_num_i} (0x4DD0AC). */
    public static int getNumI(int win, int[] value, int maxCharsWcursor, boolean clear,
            boolean allowNegative, int x, int y) {
        boolean firstPress = false;

        Window window = windowGetWindow(win);
        if (window == null) {
            return -1;
        }

        int original = value[0];

        int width = maxCharsWcursor * TextFont.fontGetMonospacedCharacterWidth();
        int height = TextFont.fontGetLineHeight();

        StringBuilder sb = new StringBuilder();
        if (clear) {
            sb.setLength(0);
        } else {
            String initial = Integer.toString(value[0]);
            if (initial.length() > maxCharsWcursor) {
                initial = initial.substring(0, maxCharsWcursor);
            }
            sb.append(initial);
        }

        int cursorPos = sb.length();
        sb.append('_');

        windowDrawText(win, sb.toString(), width, x, y, 0x100 | 4);

        Geometry.Rect rect = new Geometry.Rect();
        rect.left = x;
        rect.top = y;
        rect.right = x + width;
        rect.bottom = y + height;
        windowRefreshRect(win, rect);

        boolean done = false;
        while (cursorPos <= maxCharsWcursor && !done) {
            Color.fpsLimiter.mark();

            int input = Input.inputGetInput();
            if (input == Kb.KEY_RETURN) {
                done = true;
            } else if (input == Kb.KEY_BACKSPACE) {
                if (cursorPos > 0) {
                    int stringWidth = TextFont.fontGetStringWidth(sb.toString());
                    if (firstPress) {
                        sb.setLength(0);
                        sb.append('_');
                        cursorPos = 1;
                        firstPress = false;
                    } else {
                        sb.setCharAt(cursorPos - 1, '_');
                        sb.setLength(cursorPos);
                        cursorPos--;
                    }

                    windowFill(win, x, y, stringWidth, height, 0x100 | 1);
                    windowDrawText(win, sb.toString(), width, x, y, 0x100 | 4);
                    windowRefreshRect(win, rect);
                }
            } else if (input == Kb.KEY_ESCAPE) {
                value[0] = original;
                return -1;
            } else if (input == Kb.KEY_ARROW_LEFT) {
                if (cursorPos > 0) {
                    int stringWidth = TextFont.fontGetStringWidth(sb.toString());
                    sb.setCharAt(cursorPos - 1, '_');
                    sb.setLength(cursorPos);
                    windowFill(win, x, y, stringWidth, height, 0x100 | 1);
                    windowDrawText(win, sb.toString(), width, x, y, 0x100 | 4);
                    windowRefreshRect(win, rect);

                    firstPress = false;
                    cursorPos--;
                }
            } else {
                if (cursorPos != maxCharsWcursor - 1) {
                    if ((input == '-' && allowNegative && cursorPos == 0)
                        || (input >= '0' && input <= '9')) {
                        sb.setCharAt(cursorPos, (char) input);
                        sb.append('_');

                        int stringWidth = TextFont.fontGetStringWidth(sb.toString());
                        windowFill(win, x, y, stringWidth, height, 0x100 | 1);
                        windowDrawText(win, sb.toString(), width, x, y, 0x100 | 4);
                        windowRefreshRect(win, rect);

                        firstPress = false;
                        cursorPos++;
                    }
                }
            }

            Svga.renderPresent();
            Color.fpsLimiter.throttle();
        }

        value[0] = atoiField(sb.toString());

        return 0;
    }

    /** C {@code atoi} on the dialog field: optional {@code '-'} then digits, stops at cursor/NUL. */
    private static int atoiField(String string) {
        int index = 0;
        boolean negative = false;
        if (index < string.length() && string.charAt(index) == '-') {
            negative = true;
            index++;
        }
        int result = 0;
        while (index < string.length()) {
            char ch = string.charAt(index);
            if (ch < '0' || ch > '9') {
                break;
            }
            result = result * 10 + (ch - '0');
            index++;
        }
        return negative ? -result : result;
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
        debugWindow = -1;
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
        lastButtonWinId = -1;
        currMenu = null;
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
        for (int index = 0; index < tmQueue.length; index++) {
            tmQueue[index] = new TimedMessageQueueEntry();
        }
        tmWatchActive = false;
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
