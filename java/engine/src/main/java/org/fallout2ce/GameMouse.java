package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Game-mouse id tables plus the headless-safe engine slice, mirroring
 * {@code src/game_mouse.h} / {@code src/game_mouse.cc}.
 *
 * <p>Ported state ({@code gameMouseInit}, {@code gameMouseExit},
 * {@code gameMouseSetCursor}/{@code gameMouseGetCursor},
 * {@code gameMouseGetMode}, the scrolling and click-to-scroll flags, and the
 * 3d-objects visibility flag) reproduces the {@code [gamemouse]} vectors in
 * {@code docker/refharness/golden/v71.txt} (produced by
 * {@code sectionGameMouse} in {@code docker/refharness/ref_main.cc}).
 *
 * <p>Unlike {@link StatDefs}, these enums have no C++ {@code operator++}, so
 * there is no {@code next()}. {@code MouseCursorType} has an
 * {@code operator+} in C++; plain int addition covers it in Java.
 *
 * <p>FRM tables (transcribed from {@code src/game_mouse.cc}):
 * <ul>
 * <li>{@code kGameMouseCursorFrmIds[27]} (interface frame ids): Blank(266),
 * StandardArrow(267), SmallUpArrow(268), SmallDownArrow(269),
 * ScrollNW(270), ScrollN(271), ScrollNE(272), ScrollE(273), ScrollSE(274),
 * ScrollS(275), ScrollSW(276), ScrollW(277), InvalidScrollNW(330),
 * InvalidScrollN(331), InvalidScrollNE(329), InvalidScrollE(328),
 * InvalidScrollSE(332), InvalidScrollS(334), InvalidScrollSW(333),
 * InvalidScrollW(335), Crosshair(279), Plus(280), Destroy(281),
 * UseCrosshair(293), SingleFrameWatch(310), WaitPlanet(278),
 * WaitWatch(295).</li>
 * <li>{@code gGameMouseModeFrmIds[11]}: ActionMove(249), ActionArrow(250),
 * ActionCrosshair(251), then MouseCursorUseCrosshair(293) x8 (the skill
 * entries are re-pointed by sfall config; see
 * {@code customMouseModeFrmsInit}, NOTE-skipped below).</li>
 * <li>{@code kGameMouseActionMenuItemFrmIds[10][2]} (highlighted, normal):
 * Cancel(252,253), Drop(254,255), Inventory(256,257), Look(258,259),
 * Rotate(260,261), Talk(262,263), UseGet(264,265), Unload(301,302),
 * Skill(303,304), Push(434,435).</li>
 * </ul>
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>Scroll-init source: C declares
 * {@code _gmouse_scrolling_enabled = true} at namespace scope
 * ({@code src/game_mouse.cc:75}), so scrolling starts ENABLED even though
 * the vectors never call reset. Java defaults the flag to true too.</li>
 * <li>3d objects: C creates real game objects via
 * {@code objectCreateWithFrmIdProtoId(Blank / HexMouseCursor, Empty pid)}
 * plus outline/flat flags, a tile move, and five locked action-menu FRMs.
 * Java models only the state machine ({@code objectsInitialized} /
 * {@code objectsVisible} / item highlight); real creation, the tile move,
 * and the menu FRM locks come with the map/game phase (the vectors only
 * observe {@code vis=1}).</li>
 * <li>{@code gameMouseObjectsShow/Hide} repaint through the tile refresher,
 * which is null headless (same reason the harness only reads visibility);
 * Java flips the visibility flag, and the repaint/outline/fid tails are
 * omitted with this NOTE.</li>
 * <li>{@code gameMouseSetMode}/{@code gameMouseCycleMode} need live map
 * objects ({@code _gmouse_3d_move_to} segfaults headless, per the harness
 * NOTE). Java ports the guard clauses (init / modes-enabled / equality)
 * first, then returns behind the {@code mapObjectsAvailable} seam (default
 * false), so Java never crashes. Vectors only pin the {@code mode=0}
 * read.</li>
 * <li>Animated cursors (25 WAIT_PLANET, 26 WAIT_WATCH): the fps-gate /
 * frame-advance is ported faithfully (unvectored; tests use static
 * cursors like the harness). On a gate miss C returns -1 while still
 * holding the fresh lock; Java closes it (no leak) and returns -1.</li>
 * <li>Mouse seam: cursor pixels go through
 * {@link Mouse#mouseSetFrame} (C {@code mouseSetFrame}); the hot-spot math
 * ({@code w/2 - xOff}, {@code h-1 - yOff}, pitch {@code w}, transparent 0)
 * is verbatim.</li>
 * <li>NOTE-skipped (not vectored, game-bound): {@code gameMouseReset} /
 * {@code gameMouseObjectsReset} (mode/show tails need the map),
 * {@code gameMouseRefresh} (full per-frame pump),
 * {@code gameMouseLoadItemHighlight} is ported (pure settings read),
 * {@code customMouseModeFrmsInit} (sfall content config; mode FRMs keep
 * their compiled defaults).</li>
 * </ul>
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

    // ===== engine state (src/game_mouse.cc) =====

    /** Port of {@code kGameMouseCursorFrmIds} (interface frame ids). */
    static final int[] CURSOR_FRM_IDS = {
        ArtDefs.InterfaceFrameId.MouseCursorBlank.id(),
        ArtDefs.InterfaceFrameId.MouseCursorStandardArrow.id(),
        ArtDefs.InterfaceFrameId.MouseCursorSmallUpArrow.id(),
        ArtDefs.InterfaceFrameId.MouseCursorSmallDownArrow.id(),
        ArtDefs.InterfaceFrameId.MouseCursorScrollNorthWest.id(),
        ArtDefs.InterfaceFrameId.MouseCursorScrollNorth.id(),
        ArtDefs.InterfaceFrameId.MouseCursorScrollNorthEast.id(),
        ArtDefs.InterfaceFrameId.MouseCursorScrollEast.id(),
        ArtDefs.InterfaceFrameId.MouseCursorScrollSouthEast.id(),
        ArtDefs.InterfaceFrameId.MouseCursorScrollSouth.id(),
        ArtDefs.InterfaceFrameId.MouseCursorScrollSouthWest.id(),
        ArtDefs.InterfaceFrameId.MouseCursorScrollWest.id(),
        ArtDefs.InterfaceFrameId.MouseCursorInvalidScrollNorthWest.id(),
        ArtDefs.InterfaceFrameId.MouseCursorInvalidScrollNorth.id(),
        ArtDefs.InterfaceFrameId.MouseCursorInvalidScrollNorthEast.id(),
        ArtDefs.InterfaceFrameId.MouseCursorInvalidScrollEast.id(),
        ArtDefs.InterfaceFrameId.MouseCursorInvalidScrollSouthEast.id(),
        ArtDefs.InterfaceFrameId.MouseCursorInvalidScrollSouth.id(),
        ArtDefs.InterfaceFrameId.MouseCursorInvalidScrollSouthWest.id(),
        ArtDefs.InterfaceFrameId.MouseCursorInvalidScrollWest.id(),
        ArtDefs.InterfaceFrameId.MouseCursorCrosshair.id(),
        ArtDefs.InterfaceFrameId.MouseCursorPlus.id(),
        ArtDefs.InterfaceFrameId.MouseCursorDestroy.id(),
        ArtDefs.InterfaceFrameId.MouseCursorUseCrosshair.id(),
        ArtDefs.InterfaceFrameId.MouseCursorSingleFrameWatch.id(),
        ArtDefs.InterfaceFrameId.MouseCursorWaitPlanet.id(),
        ArtDefs.InterfaceFrameId.MouseCursorWaitWatch.id(),
    };

    /** Port of {@code gGameMouseModeFrmIds} (sfall may re-point 4..10). */
    static final int[] MODE_FRM_IDS = {
        ArtDefs.InterfaceFrameId.ActionMove.id(),
        ArtDefs.InterfaceFrameId.ActionArrow.id(),
        ArtDefs.InterfaceFrameId.ActionCrosshair.id(),
        ArtDefs.InterfaceFrameId.MouseCursorUseCrosshair.id(),
        ArtDefs.InterfaceFrameId.MouseCursorUseCrosshair.id(),
        ArtDefs.InterfaceFrameId.MouseCursorUseCrosshair.id(),
        ArtDefs.InterfaceFrameId.MouseCursorUseCrosshair.id(),
        ArtDefs.InterfaceFrameId.MouseCursorUseCrosshair.id(),
        ArtDefs.InterfaceFrameId.MouseCursorUseCrosshair.id(),
        ArtDefs.InterfaceFrameId.MouseCursorUseCrosshair.id(),
        ArtDefs.InterfaceFrameId.MouseCursorUseCrosshair.id(),
    };

    /** Port of {@code kGameMouseActionMenuItemFrmIds} (highlighted, normal). */
    static final int[][] ACTION_MENU_ITEM_FRM_IDS = {
        { ArtDefs.InterfaceFrameId.CancelHighlighted.id(), ArtDefs.InterfaceFrameId.Cancel.id() },
        { ArtDefs.InterfaceFrameId.DropHighlighted.id(), ArtDefs.InterfaceFrameId.Drop.id() },
        { ArtDefs.InterfaceFrameId.InventoryHighlighted.id(), ArtDefs.InterfaceFrameId.Inventory.id() },
        { ArtDefs.InterfaceFrameId.LookHighlighted.id(), ArtDefs.InterfaceFrameId.Look.id() },
        { ArtDefs.InterfaceFrameId.RotateHighlighted.id(), ArtDefs.InterfaceFrameId.Rotate.id() },
        { ArtDefs.InterfaceFrameId.TalkHighlighted.id(), ArtDefs.InterfaceFrameId.Talk.id() },
        { ArtDefs.InterfaceFrameId.UseGetHighlighted.id(), ArtDefs.InterfaceFrameId.UseGet.id() },
        { ArtDefs.InterfaceFrameId.UnloadHighlighted.id(), ArtDefs.InterfaceFrameId.Unload.id() },
        { ArtDefs.InterfaceFrameId.SkillHighlighted.id(), ArtDefs.InterfaceFrameId.Skill.id() },
        { ArtDefs.InterfaceFrameId.PushHighlighted.id(), ArtDefs.InterfaceFrameId.Push.id() },
    };

    private static boolean initialized; // gGameMouseInitialized
    private static boolean enabled; // _gmouse_enabled
    private static boolean objectsInitialized; // gGameMouseObjectsInitialized
    private static boolean objectsVisible; // hex cursor not hidden
    private static int cursor; // gGameMouseCursor (NONE=0 initially)
    private static int mode; // gGameMouseMode (MOVE=0)
    private static boolean modesEnabled = true; // _gmouse_3d_modes_enabled
    private static boolean scrollingEnabled = true; // _gmouse_scrolling_enabled
    private static boolean clickToScroll; // _gmouse_click_to_scroll
    private static boolean clickedOnEdge; // _gmouse_clicked_on_edge
    private static int bkLastCursor = MouseCursorType.MOUSE_CURSOR_INVALID.id(); // _gmouse_bk_last_cursor
    private static int animatedNextFrame; // gGameMouseAnimatedCursorNextFrame
    private static long animatedLastTimestamp; // gGameMouseAnimatedCursorLastUpdateTimestamp
    private static boolean itemHighlightEnabled = true; // gGameMouseItemHighlightEnabled
    private static Art.FrmImage cursorFrm; // gGameMouseCursorFrmHandle

    /**
     * Map seam for the mode/object tails: C executes them over live map
     * objects and segfaults headless (harness NOTE). Java keeps this false
     * outside the game phase so those tails stay dormant and never crash.
     */
    static boolean mapObjectsAvailable;

    /** Port of {@code gameMouseInit} (0x44B2B0). */
    public static int gameMouseInit() {
        if (initialized) {
            return -1;
        }
        if (gameMouseObjectsInit() != 0) {
            return -1;
        }
        initialized = true;
        enabled = true;
        gameMouseSetCursor(MouseCursorType.MOUSE_CURSOR_ARROW.id());
        // SFALL customMouseModeFrmsInit: NOTE-skipped (sfall content config).
        return 0;
    }

    /** Port of {@code gameMouseExit} (0x44B3B8). */
    public static void gameMouseExit() {
        if (!initialized) {
            return;
        }
        Mouse.mouseHideCursor();
        Mouse.mouseSetFrame(null, 0, 0, 0, 0, 0, 0);
        gameMouseObjectsFree();
        if (cursorFrm != null) {
            cursorFrm.close();
            cursorFrm = null;
        }
        enabled = false;
        initialized = false;
        cursor = MouseCursorType.MOUSE_CURSOR_INVALID.id();
    }

    /**
     * Port of {@code gameMouseObjectsInit} (0x44D984) state machine. Real
     * object creation ({@code objectCreateWithFrmIdProtoId} with
     * Blank/HexMouseCursor + Empty pid), outline/flat flags, the tile move,
     * and the action-menu FRM locks are deferred (map/game phase seam);
     * the initialized/visible/highlight state is kept.
     */
    static int gameMouseObjectsInit() {
        if (objectsInitialized) {
            return -1;
        }
        objectsInitialized = true;
        objectsVisible = true;
        gameMouseLoadItemHighlight();
        return 0;
    }

    /** Port of {@code gameMouseObjectsFree} (0x44DB34) state machine. */
    static void gameMouseObjectsFree() {
        if (objectsInitialized) {
            objectsInitialized = false;
            objectsVisible = false;
        }
    }

    /**
     * Port of {@code gameMouseSetCursor} (0x44C840): init gate, dup guard
     * (non-ARROW same cursor with {@code cursor < 25 || cursor >= 27}
     * returns -1), art lock of the cursor FRM, the animated fps-gate /
     * frame-advance for cursors {@code >= 25}, then {@code mouseSetFrame}
     * with the verbatim hot-spot math, unlock-old/swap, return 0. Out of
     * range ids return -1 (C would index out of bounds; unreachable).
     */
    public static int gameMouseSetCursor(int cursorId) {
        if (!initialized) {
            return -1;
        }
        if (cursorId != MouseCursorType.MOUSE_CURSOR_ARROW.id()
                && cursorId == cursor
                && (cursor < FIRST_GAME_MOUSE_ANIMATED_CURSOR
                    || cursor >= MOUSE_CURSOR_TYPE_COUNT)) {
            return -1;
        }
        if (cursorId < 0 || cursorId >= MOUSE_CURSOR_TYPE_COUNT) {
            return -1;
        }

        FrmId fid = FrmId.ofType(ObjectType.INTERFACE, CURSOR_FRM_IDS[cursorId]);
        Art.ArtLock info = Art.artLock(fid);
        if (info == null) {
            return -1;
        }
        int fps = Art.artGetFramesPerSecond(info.art());
        int frameCount = Art.artGetFrameCount(info.art());
        Art.artUnlock(info);

        boolean shouldUpdate = true;
        int frame = 0;
        if (cursorId >= FIRST_GAME_MOUSE_ANIMATED_CURSOR) {
            long tick = Ticks.getTicks();
            if (objectsVisible) {
                gameMouseObjectsHide();
            }
            long delay = fps > 0 ? 1000L / fps : 0L;
            if (Ticks.getTicksBetween(tick, animatedLastTimestamp) < delay) {
                shouldUpdate = false;
            } else {
                if (frameCount <= animatedNextFrame) {
                    animatedNextFrame = 0;
                }
                frame = animatedNextFrame;
                animatedLastTimestamp = tick;
                animatedNextFrame++;
            }
        }

        if (!shouldUpdate) {
            return -1;
        }

        Art.FrmImage image = new Art.FrmImage();
        if (!image.lock(fid, frame, Rotation.NE.value())) {
            return -1;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int hotX = width / 2 - image.getXOffset();
        int hotY = height - 1 - image.getYOffset();
        if (Mouse.mouseSetFrame(image.getData(), width, height, width, hotX, hotY, 0) != 0) {
            image.close();
            return -1;
        }

        if (cursorFrm != null) {
            cursorFrm.close();
        }
        cursorFrm = image;
        cursor = cursorId;
        return 0;
    }

    /** Port of {@code gameMouseGetCursor} (0x44C9E8). */
    public static int gameMouseGetCursor() {
        return cursor;
    }

    /** Port of {@code gameMouseGetMode} (0x44CB6C). */
    public static int gameMouseGetMode() {
        return mode;
    }

    /**
     * Port of {@code gameMouseSetMode} (0x44CA18) guards: uninitialized and
     * modes-disabled return, equal mode returns. The map tail (bouncing-frm
     * reset, flat-frm swap, object move, outline flips, tile refresh,
     * touchscreen mode) needs live map objects and stays behind the
     * {@code mapObjectsAvailable} seam (C crashes headless there).
     */
    public static void gameMouseSetMode(int newMode) {
        if (!initialized) {
            return;
        }
        if (!modesEnabled) {
            return;
        }
        if (newMode == mode) {
            return;
        }
        if (!mapObjectsAvailable) {
            return;
        }
    }

    /**
     * Port of {@code gameMouseCycleMode} (0x44CB74) shape: cycles
     * {@code (mode + 1) % 3}; the combat/active-item branch needs the game
     * state, so without a map it routes straight through
     * {@link #gameMouseSetMode} (a dormant no-op headless).
     */
    public static void gameMouseCycleMode() {
        int next = (mode + 1) % 3;
        gameMouseSetMode(next);
    }

    /** Port of {@code _gmouse_enable} (0x44B454) shape (map-free prefix). */
    public static void gmouseEnable() {
        if (!enabled) {
            cursor = MouseCursorType.MOUSE_CURSOR_INVALID.id();
            gameMouseSetCursor(MouseCursorType.MOUSE_CURSOR_NONE.id());
            scrollingEnabled = true;
            enabled = true;
            bkLastCursor = MouseCursorType.MOUSE_CURSOR_INVALID.id();
        }
    }

    /** Port of {@code _gmouse_disable} (0x44B48C). {@code allowScrolling} maps {@code (allow & 1)}. */
    public static void gmouseDisable(boolean allowScrolling) {
        if (enabled) {
            gameMouseSetCursor(MouseCursorType.MOUSE_CURSOR_NONE.id());
            enabled = false;
            scrollingEnabled = allowScrolling;
        }
    }

    /** Port of {@code _gmouse_enable_scrolling} (0x44B4CC). */
    public static void gmouseEnableScrolling() {
        scrollingEnabled = true;
    }

    /** Port of {@code _gmouse_disable_scrolling} (0x44B4D8). */
    public static void gmouseDisableScrolling() {
        scrollingEnabled = false;
    }

    /** Port of {@code gmouse_scrolling_is_enabled} (0x44B4E4). */
    public static boolean gmouseScrollingIsEnabled() {
        return scrollingEnabled;
    }

    /** Port of {@code _gmouse_get_click_to_scroll} (0x44B504). */
    public static boolean gmouseGetClickToScroll() {
        return clickToScroll;
    }

    /** Port of {@code _gmouse_set_click_to_scroll}. */
    public static void gmouseSetClickToScroll(boolean value) {
        if (value != clickToScroll) {
            clickToScroll = value;
            clickedOnEdge = false;
        }
    }

    /**
     * Visibility half of {@code gameMouseObjectsShow} (0x44CD2C): the tile
     * repaint / outline / fid tails need the map and are omitted (harness
     * NOTE); the flag flip is kept.
     */
    public static void gameMouseObjectsShow() {
        if (!initialized) {
            return;
        }
        objectsVisible = true;
    }

    /**
     * Visibility half of {@code gameMouseObjectsHide} (0x44CE34): see
     * {@link #gameMouseObjectsShow} for the omitted repaint tail.
     */
    public static void gameMouseObjectsHide() {
        if (!initialized) {
            return;
        }
        objectsVisible = false;
    }

    /** Port of {@code gameMouseObjectsIsVisible} (0x44CEB0) over the flag. */
    public static boolean gameMouseObjectsIsVisible() {
        return objectsVisible;
    }

    /** Port of {@code gameMouseLoadItemHighlight} (0x44D954). */
    public static void gameMouseLoadItemHighlight() {
        itemHighlightEnabled = Settings.settings.preferences.item_highlight;
    }

    static boolean gameMouseItemHighlightEnabled() {
        return itemHighlightEnabled;
    }

    /** Test seam: full state reset plus cursor-FRM unlock. */
    static void resetForTests() {
        if (cursorFrm != null) {
            cursorFrm.close();
            cursorFrm = null;
        }
        initialized = false;
        enabled = false;
        objectsInitialized = false;
        objectsVisible = false;
        cursor = MouseCursorType.MOUSE_CURSOR_NONE.id();
        mode = GameMouseMode.GAME_MOUSE_MODE_MOVE.id();
        modesEnabled = true;
        scrollingEnabled = true;
        clickToScroll = false;
        clickedOnEdge = false;
        bkLastCursor = MouseCursorType.MOUSE_CURSOR_INVALID.id();
        animatedNextFrame = 0;
        animatedLastTimestamp = 0;
        itemHighlightEnabled = true;
        mapObjectsAvailable = false;
    }
}
