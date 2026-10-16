package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Game-mouse id tables, mirroring the enumerations in
 * {@code src/game_mouse.h}.
 *
 * <p>Only the enumerations (+ derived counts) are ported. The game-mouse
 * ENGINE ({@code gameMouseInit}, cursor/map state, action-menu rendering in
 * {@code src/game_mouse.cc}) needs the game map and comes with the game-state
 * phase (future work).
 *
 * <p>Unlike {@link StatDefs}, these enums have no C++ {@code operator++}, so
 * there is no {@code next()}. {@code MouseCursorType} has an
 * {@code operator+} in C++; plain int addition covers it in Java.
 */
public final class GameMouse {

    public static final int GAME_MOUSE_MODE_COUNT = 11;
    public static final int FIRST_GAME_MOUSE_MODE_SKILL = 4;
    public static final int GAME_MOUSE_MODE_SKILL_COUNT =
            GAME_MOUSE_MODE_COUNT - FIRST_GAME_MOUSE_MODE_SKILL;

    public static final int GAME_MOUSE_ACTION_MENU_ITEM_COUNT = 10;

    public static final int MOUSE_CURSOR_TYPE_COUNT = 27;
    public static final int FIRST_GAME_MOUSE_ANIMATED_CURSOR = 25;
    public static final int MOUSE_CURSOR_SCROLL_OFFSET_INVALID = 8;

    private GameMouse() {
    }

    public enum GameMouseMode {
        GAME_MOUSE_MODE_INVALID(-1),
        GAME_MOUSE_MODE_MOVE(0),
        GAME_MOUSE_MODE_ARROW(1),
        GAME_MOUSE_MODE_CROSSHAIR(2),
        GAME_MOUSE_MODE_USE_CROSSHAIR(3),
        GAME_MOUSE_MODE_USE_FIRST_AID(4),
        GAME_MOUSE_MODE_USE_DOCTOR(5),
        GAME_MOUSE_MODE_USE_LOCKPICK(6),
        GAME_MOUSE_MODE_USE_STEAL(7),
        GAME_MOUSE_MODE_USE_TRAPS(8),
        GAME_MOUSE_MODE_USE_SCIENCE(9),
        GAME_MOUSE_MODE_USE_REPAIR(10);

        private final int id;

        GameMouseMode(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, GameMouseMode> MODE_BY_ID = new HashMap<>();

    static {
        for (GameMouseMode mode : GameMouseMode.values()) {
            MODE_BY_ID.put(mode.id, mode);
        }
    }

    public static GameMouseMode ofGameMouseMode(int id) {
        return MODE_BY_ID.get(id);
    }

    public static boolean gameMouseModeIsValid(int mode) {
        return mode >= 0 && mode < GAME_MOUSE_MODE_COUNT;
    }

    public enum GameMouseActionMenuItem {
        GAME_MOUSE_ACTION_MENU_ITEM_INVALID(-1),
        GAME_MOUSE_ACTION_MENU_ITEM_CANCEL(0),
        GAME_MOUSE_ACTION_MENU_ITEM_DROP(1),
        GAME_MOUSE_ACTION_MENU_ITEM_INVENTORY(2),
        GAME_MOUSE_ACTION_MENU_ITEM_LOOK(3),
        GAME_MOUSE_ACTION_MENU_ITEM_ROTATE(4),
        GAME_MOUSE_ACTION_MENU_ITEM_TALK(5),
        GAME_MOUSE_ACTION_MENU_ITEM_USE(6),
        GAME_MOUSE_ACTION_MENU_ITEM_UNLOAD(7),
        GAME_MOUSE_ACTION_MENU_ITEM_USE_SKILL(8),
        GAME_MOUSE_ACTION_MENU_ITEM_PUSH(9);

        private final int id;

        GameMouseActionMenuItem(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, GameMouseActionMenuItem> ITEM_BY_ID = new HashMap<>();

    static {
        for (GameMouseActionMenuItem item : GameMouseActionMenuItem.values()) {
            ITEM_BY_ID.put(item.id, item);
        }
    }

    public static GameMouseActionMenuItem ofActionMenuItem(int id) {
        return ITEM_BY_ID.get(id);
    }

    public static boolean actionMenuItemIsValid(int item) {
        return item >= 0 && item < GAME_MOUSE_ACTION_MENU_ITEM_COUNT;
    }

    public enum MouseCursorType {
        MOUSE_CURSOR_INVALID(-1),
        MOUSE_CURSOR_NONE(0),
        MOUSE_CURSOR_ARROW(1),
        MOUSE_CURSOR_SMALL_ARROW_UP(2),
        MOUSE_CURSOR_SMALL_ARROW_DOWN(3),
        MOUSE_CURSOR_SCROLL_NW(4),
        MOUSE_CURSOR_SCROLL_N(5),
        MOUSE_CURSOR_SCROLL_NE(6),
        MOUSE_CURSOR_SCROLL_E(7),
        MOUSE_CURSOR_SCROLL_SE(8),
        MOUSE_CURSOR_SCROLL_S(9),
        MOUSE_CURSOR_SCROLL_SW(10),
        MOUSE_CURSOR_SCROLL_W(11),
        MOUSE_CURSOR_SCROLL_NW_INVALID(12),
        MOUSE_CURSOR_SCROLL_N_INVALID(13),
        MOUSE_CURSOR_SCROLL_NE_INVALID(14),
        MOUSE_CURSOR_SCROLL_E_INVALID(15),
        MOUSE_CURSOR_SCROLL_SE_INVALID(16),
        MOUSE_CURSOR_SCROLL_S_INVALID(17),
        MOUSE_CURSOR_SCROLL_SW_INVALID(18),
        MOUSE_CURSOR_SCROLL_W_INVALID(19),
        MOUSE_CURSOR_CROSSHAIR(20),
        MOUSE_CURSOR_PLUS(21),
        MOUSE_CURSOR_DESTROY(22),
        MOUSE_CURSOR_USE_CROSSHAIR(23),
        MOUSE_CURSOR_WATCH(24),
        MOUSE_CURSOR_WAIT_PLANET(25),
        MOUSE_CURSOR_WAIT_WATCH(26);

        private final int id;

        MouseCursorType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, MouseCursorType> CURSOR_BY_ID = new HashMap<>();

    static {
        for (MouseCursorType cursor : MouseCursorType.values()) {
            CURSOR_BY_ID.put(cursor.id, cursor);
        }
    }

    public static MouseCursorType ofMouseCursorType(int id) {
        return CURSOR_BY_ID.get(id);
    }

    public static boolean mouseCursorTypeIsValid(int cursor) {
        return cursor >= 0 && cursor < MOUSE_CURSOR_TYPE_COUNT;
    }
}
