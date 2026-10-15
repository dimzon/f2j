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
}
