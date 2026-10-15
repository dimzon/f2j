package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.fallout2ce.MouseManager.MouseType;
import org.fallout2ce.Touch.GestureState;
import org.fallout2ce.Touch.GestureType;
import org.fallout2ce.Window.ManagedButtonMouseEvent;
import org.fallout2ce.Window.TextAlignment;
import org.fallout2ce.WindowManager.ButtonFlags;
import org.fallout2ce.WindowManager.WindowFlags;
import org.fallout2ce.WindowManager.WindowManagerErr;

/**
 * {@code [uitypes]} vectors from
 * {@code docker/refharness/golden/v37.txt}.
 */
class UiTypesTest {

    @Test
    void windowManagerCodes() {
        assertEquals(0, WindowManagerErr.WINDOW_MANAGER_OK.id());
        assertEquals(9, WindowManagerErr.WINDOW_MANAGER_ERR_ALREADY_RUNNING.id());
        assertEquals(10, WindowManagerErr.WINDOW_MANAGER_ERR_TITLE_NOT_SET.id());
        assertEquals(11, WindowManagerErr.WINDOW_MANAGER_ERR_INITIALIZING_INPUT.id());
        assertEquals(WindowManagerErr.WINDOW_MANAGER_ERR_NO_MEMORY,
            WindowManager.ofWindowManagerErr(2));
        assertNull(WindowManager.ofWindowManagerErr(12));
        assertEquals(0, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_NONE.id());
        assertEquals(1, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id());
        assertEquals(64, WindowManager.BUTTON_GROUP_BUTTON_LIST_CAPACITY);
    }

    @Test
    void windowAndButtonFlags() {
        assertEquals(1, WindowFlags.WINDOW_USE_DEFAULTS.id());
        assertEquals(2, WindowFlags.WINDOW_DONT_MOVE_TOP.id());
        assertEquals(4, WindowFlags.WINDOW_MOVE_ON_TOP.id());
        assertEquals(8, WindowFlags.WINDOW_HIDDEN.id());
        assertEquals(16, WindowFlags.WINDOW_MODAL.id());
        assertEquals(32, WindowFlags.WINDOW_TRANSPARENT.id());
        assertEquals(128, WindowFlags.WINDOW_DRAGGABLE_BY_BACKGROUND.id());
        assertEquals(256, WindowFlags.WINDOW_MANAGED.id());
        assertEquals(1, ButtonFlags.BUTTON_FLAG_CHECKABLE.id());
        assertEquals(2, ButtonFlags.BUTTON_FLAG_CHECK_ON_DOWN.id());
        assertEquals(4, ButtonFlags.BUTTON_FLAG_NO_TOGGLE_OFF.id());
        assertEquals(8, ButtonFlags.BUTTON_FLAG_DISABLED.id());
        assertEquals(16, ButtonFlags.BUTTON_DRAG_HANDLE.id());
        assertEquals(32, ButtonFlags.BUTTON_FLAG_TRANSPARENT.id());
        assertEquals(65536, ButtonFlags.BUTTON_FLAG_GRAPHIC.id());
        assertEquals(131072, ButtonFlags.BUTTON_FLAG_CHECKED.id());
        assertEquals(262144, ButtonFlags.BUTTON_FLAG_RADIO.id());
        assertEquals(524288, ButtonFlags.BUTTON_FLAG_RIGHT_MOUSE_BUTTON_CONFIGURED.id());
    }

    @Test
    void windowEnums() {
        assertEquals(0, TextAlignment.TEXT_ALIGNMENT_LEFT.id());
        assertEquals(1, TextAlignment.TEXT_ALIGNMENT_RIGHT.id());
        assertEquals(2, TextAlignment.TEXT_ALIGNMENT_CENTER.id());
        assertEquals(TextAlignment.TEXT_ALIGNMENT_CENTER, Window.ofTextAlignment(2));
        assertNull(Window.ofTextAlignment(3));
        assertEquals(4, Window.MANAGED_BUTTON_MOUSE_EVENT_COUNT);
        assertEquals(ManagedButtonMouseEvent.MANAGED_BUTTON_MOUSE_EVENT_EXIT,
            Window.ofManagedButtonMouseEvent(3));
        assertEquals(2, Window.MANAGED_BUTTON_RIGHT_MOUSE_EVENT_COUNT);
        assertEquals(Window.ManagedButtonRightMouseEvent.MANAGED_BUTTON_RIGHT_MOUSE_EVENT_BUTTON_UP,
            Window.ofManagedButtonRightMouseEvent(1));
    }

    @Test
    void windowStructDefaults() {
        WindowManager.Window window = new WindowManager.Window();
        assertEquals(0, window.id);
        assertNull(window.buffer);
        assertNull(window.buttonListHead);
        assertNull(window.menuBar);
        assertNull(window.blitProc);
        assertEquals(0, window.rect.left);

        WindowManager.Button button = new WindowManager.Button();
        assertNull(button.prev);
        assertNull(button.next);
        assertNull(button.buttonGroup);
        assertTrue(button.frmImages.isEmpty());

        WindowManager.ButtonGroup group = new WindowManager.ButtonGroup();
        assertEquals(64, group.buttons.length);

        WindowManager.MenuBar menuBar = new WindowManager.MenuBar();
        assertEquals(15, menuBar.pulldowns.length);
    }

    @Test
    void uniqueHandle() {
        List<Integer> destroyed = new ArrayList<>();
        WindowManager.UniqueHandle handle = new WindowManager.UniqueHandle(destroyed::add, 7);
        assertEquals(7, handle.get());
        assertEquals(7, handle.release());
        assertEquals(-1, handle.get());
        assertTrue(destroyed.isEmpty());

        handle.reset(9);
        handle.close();
        assertEquals(-1, handle.get());
        assertEquals(List.of(9), destroyed);
    }

    @Test
    void mouseConsts() {
        assertEquals(8, Mouse.DEFAULT_CURSOR_WIDTH);
        assertEquals(8, Mouse.DEFAULT_CURSOR_HEIGHT);
        assertEquals(64, Mouse.DEFAULT_CURSOR_SIZE);
        assertEquals(1, Mouse.STATE_LEFT_BUTTON_DOWN);
        assertEquals(2, Mouse.STATE_RIGHT_BUTTON_DOWN);
        assertEquals(3, Mouse.EVENT_ANY_BUTTON_DOWN);
        assertEquals(12, Mouse.EVENT_ANY_BUTTON_REPEAT);
        assertEquals(48, Mouse.EVENT_ANY_BUTTON_UP);
        assertEquals(3, Mouse.EVENT_LEFT_BUTTON_DOWN | Mouse.EVENT_RIGHT_BUTTON_DOWN);
        assertEquals(64, Mouse.EVENT_WHEEL);
        assertEquals(250, Mouse.BUTTON_REPEAT_TIME);
        assertEquals(0.25, Mouse.SENSITIVITY_MIN);
        assertEquals(2.5, Mouse.SENSITIVITY_MAX);
    }

    @Test
    void touchTypes() {
        assertEquals(1, GestureType.kTap.id());
        assertEquals(3, GestureType.kPan.id());
        assertEquals(GestureType.kLongPress, Touch.ofGestureType(2));
        assertNull(Touch.ofGestureType(4));
        assertEquals(0, GestureState.kPossible.id());
        assertEquals(3, GestureState.kEnded.id());

        Touch.Gesture gesture = new Touch.Gesture();
        assertEquals(GestureType.kUnrecognized, gesture.type);
        assertEquals(GestureState.kPossible, gesture.state);
    }

    @Test
    void mouseManagerTypes() {
        assertEquals(32, MouseManager.CACHE_CAPACITY);
        assertEquals(0, MouseType.MOUSE_MANAGER_MOUSE_TYPE_NONE.id());
        assertEquals(1, MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id());
        assertEquals(2, MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id());
        assertEquals(MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED, MouseManager.ofMouseType(2));

        MouseManager.CacheEntry entry = new MouseManager.CacheEntry();
        assertEquals(768, entry.palette.length);
        assertEquals("", entry.fileName);
        assertNull(entry.data);

        MouseManager.NameMangler mangler = name -> name + ".frm";
        assertEquals("cursor.frm", mangler.mangle("cursor"));
    }
}
