package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjTypes.ObjectFlags;
import org.fallout2ce.ObjTypes.OutlineType;

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
 * plus outline/flat flags, a tile move ({@code _gmouse_3d_move_to}), and
 * five locked action-menu FRMs; Java ports all of it (v98). The tile move
 * needs live tile/art/mouse state, which the map-phase tests set up.</li>
 * <li>{@code gameMouseObjectsShow/Hide} repaint through the tile refresher;
 * rect unions need the render engine, so the refresh call keeps a null
 * rect, like the other deferred rect seams in {@link ObjectSystem}.</li>
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
    private static GameObject bouncingCursor; // gGameMouseBouncingCursor
    private static GameObject hexCursor; // gGameMouseHexCursor
    private static boolean mapperMode; // _gmouse_mapper_mode (mapper UI sets it)
    private static Art.ArtLock actionMenuFrm; // gGameMouseActionMenuFrmHandle
    private static Art.ArtLock actionPickFrm; // gGameMouseActionPickFrmHandle
    private static Art.ArtLock actionToHitFrm; // gGameMouseActionHitFrmHandle
    private static Art.ArtLock bouncingCursorFrm; // gGameMouseBouncingCursorFrmHandle
    private static Art.ArtLock hexCursorFrm; // gGameMouseHexCursorFrmHandle

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
     * Port of {@code gameMouseObjectsInit} (0x44D984): creates the bouncing
     * (Blank fid) and hex (HexMouseCursor fid) cursor objects with the
     * Empty pid, sets the hex outline ({@code SAME_TEAM | PALETTED}), toggles
     * it flat, locks the action-menu FRMs, sets the cursor flags
     * ({@code LIGHT_THRU | NO_SAVE | NO_REMOVE | SHOOT_THRU | NO_BLOCK}),
     * moves both to the mouse tile ({@link #gmouse3dMoveTo}) and keeps the
     * visible/highlight state.
     */
    static int gameMouseObjectsInit() {
        if (objectsInitialized) {
            return -1;
        }

        GameObject[] bouncingSlot = new GameObject[1];
        if (ObjectSystem.objectCreateWithFrmIdProtoId(bouncingSlot,
                FrmId.ofType(ObjectType.INTERFACE, ArtDefs.InterfaceFrameId.Blank.id()),
                ProtoTypes.ProtoId.empty()) != 0) {
            return -1;
        }
        bouncingCursor = bouncingSlot[0];

        GameObject[] hexSlot = new GameObject[1];
        if (ObjectSystem.objectCreateWithFrmIdProtoId(hexSlot,
                FrmId.ofType(ObjectType.INTERFACE, ArtDefs.InterfaceFrameId.HexMouseCursor.id()),
                ProtoTypes.ProtoId.empty()) != 0) {
            return -1;
        }
        hexCursor = hexSlot[0];

        if (ObjectSystem.objectSetOutline(hexCursor,
                OutlineType.SAME_TEAM.or(ObjTypes.OUTLINE_PALETTED)) != 0) {
            return -1;
        }

        if (gameMouseActionMenuInit() != 0) {
            return -1;
        }

        bouncingCursor.flags = bouncingCursor.flags.or(ObjectFlags.LIGHT_THRU);
        bouncingCursor.flags = bouncingCursor.flags.or(ObjectFlags.NO_SAVE);
        bouncingCursor.flags = bouncingCursor.flags.or(ObjectFlags.NO_REMOVE);
        bouncingCursor.flags = bouncingCursor.flags.or(ObjectFlags.SHOOT_THRU);
        bouncingCursor.flags = bouncingCursor.flags.or(ObjectFlags.NO_BLOCK);

        hexCursor.flags = hexCursor.flags.or(ObjectFlags.NO_REMOVE);
        hexCursor.flags = hexCursor.flags.or(ObjectFlags.NO_SAVE);
        hexCursor.flags = hexCursor.flags.or(ObjectFlags.LIGHT_THRU);
        hexCursor.flags = hexCursor.flags.or(ObjectFlags.SHOOT_THRU);
        hexCursor.flags = hexCursor.flags.or(ObjectFlags.NO_BLOCK);

        ObjectSystem.toggleFlat(hexCursor);

        int[] mouse = new int[2];
        Mouse.mouseGetPosition(mouse);
        gmouse3dMoveTo(mouse[0], mouse[1], org.fallout2ce.Map.gElevation);

        objectsInitialized = true;
        objectsVisible = true;
        gameMouseLoadItemHighlight();
        return 0;
    }

    /** Port of {@code gameMouseObjectsFree} (0x44DB34, inlined upstream). */
    static void gameMouseObjectsFree() {
        if (objectsInitialized) {
            gameMouseActionMenuFree();

            bouncingCursor.flags = bouncingCursor.flags.and(ObjectFlags.NO_SAVE.not());
            hexCursor.flags = hexCursor.flags.and(ObjectFlags.NO_SAVE.not());

            ObjectSystem.objectDestroy(bouncingCursor);
            ObjectSystem.objectDestroy(hexCursor);

            objectsInitialized = false;
        }
    }

    /**
     * Port of {@code gameMouseActionMenuInit} (0x44DB78): locks the five
     * action-menu FRMs (any miss unwinds the acquired ones and returns -1).
     * Width/height/data caching is render engine (game phase, deferred).
     */
    private static int gameMouseActionMenuInit() {
        actionMenuFrm = Art.artLock(
                FrmId.ofType(ObjectType.INTERFACE, ArtDefs.InterfaceFrameId.ActionMenu.id()));
        if (actionMenuFrm == null) {
            gameMouseActionMenuFree();
            return -1;
        }

        actionPickFrm = Art.artLock(
                FrmId.ofType(ObjectType.INTERFACE, ArtDefs.InterfaceFrameId.ActionPick.id()));
        if (actionPickFrm == null) {
            gameMouseActionMenuFree();
            return -1;
        }

        actionToHitFrm = Art.artLock(
                FrmId.ofType(ObjectType.INTERFACE, ArtDefs.InterfaceFrameId.ActionToHit.id()));
        if (actionToHitFrm == null) {
            gameMouseActionMenuFree();
            return -1;
        }

        bouncingCursorFrm = Art.artLock(
                FrmId.ofType(ObjectType.INTERFACE, ArtDefs.InterfaceFrameId.Blank.id()));
        if (bouncingCursorFrm == null) {
            gameMouseActionMenuFree();
            return -1;
        }

        hexCursorFrm = Art.artLock(
                FrmId.ofType(ObjectType.INTERFACE, ArtDefs.InterfaceFrameId.HexMouseCursor.id()));
        if (hexCursorFrm == null) {
            gameMouseActionMenuFree();
            return -1;
        }

        return 0;
    }

    /** Port of {@code gameMouseActionMenuFree} (0x44DB34, inlined upstream). */
    private static void gameMouseActionMenuFree() {
        if (actionMenuFrm != null) {
            actionMenuFrm.close();
            actionMenuFrm = null;
        }
        if (actionPickFrm != null) {
            actionPickFrm.close();
            actionPickFrm = null;
        }
        if (actionToHitFrm != null) {
            actionToHitFrm.close();
            actionToHitFrm = null;
        }
        if (bouncingCursorFrm != null) {
            bouncingCursorFrm.close();
            bouncingCursorFrm = null;
        }
        if (hexCursorFrm != null) {
            hexCursorFrm.close();
            hexCursorFrm = null;
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
     * Port of {@code gameMouseObjectsShow} (0x44CD2C): shows both cursors,
     * clears the hex outline outside MOVE mode, refreshes the hex fid and
     * repaints when anything changed (rect unions need the render engine,
     * so the refresh call keeps a null rect, like the other deferred rect
     * seams). The hover-test tail is render pump (game phase, deferred).
     */
    public static void gameMouseObjectsShow() {
        if (!initialized) {
            return;
        }

        int refreshFlags = 0;

        if (ObjectSystem.objectShow(bouncingCursor) == 0) {
            refreshFlags |= REFRESH_BOUNCING_CURSOR;
        }

        if (ObjectSystem.objectShow(hexCursor) == 0) {
            refreshFlags |= REFRESH_HEX_CURSOR;
        }

        if (mode != GameMouseMode.GAME_MOUSE_MODE_MOVE.id()) {
            if (ObjectSystem.objectDisableOutline(hexCursor) == 0) {
                refreshFlags |= REFRESH_HEX_CURSOR;
            }
        }

        if (gameMouseUpdateHexCursorFid() == 0) {
            refreshFlags |= REFRESH_HEX_CURSOR;
        }

        if (refreshFlags != 0) {
            TileMath.tileWindowRefreshRect(null, org.fallout2ce.Map.gElevation);
        }

        objectsVisible = true;
    }

    /**
     * Port of {@code gameMouseObjectsHide} (0x44CE34): hides both cursors
     * and repaints when anything changed (rects deferred, like
     * {@link #gameMouseObjectsShow}).
     */
    public static void gameMouseObjectsHide() {
        if (!initialized) {
            return;
        }

        int refreshFlags = 0;

        if (ObjectSystem.objectHide(bouncingCursor) == 0) {
            refreshFlags |= REFRESH_BOUNCING_CURSOR;
        }

        if (ObjectSystem.objectHide(hexCursor) == 0) {
            refreshFlags |= REFRESH_HEX_CURSOR;
        }

        if (refreshFlags != 0) {
            TileMath.tileWindowRefreshRect(null, org.fallout2ce.Map.gElevation);
        }

        objectsVisible = false;
    }

    /** Port of {@code REFRESH_BOUNCING_CURSOR} (0x01, src/game_mouse.cc). */
    private static final int REFRESH_BOUNCING_CURSOR = 0x01;

    /** Port of {@code REFRESH_HEX_CURSOR} (0x02, src/game_mouse.cc). */
    private static final int REFRESH_HEX_CURSOR = 0x02;

    /**
     * Port of {@code gameMouseSetBouncingCursorFrmId} (0x44CC98): init gate,
     * list-hit gate (the C {@code exist()} check; the vectored fids always
     * hit loaded art), dup-fid guard, then the fid store (the mapper-mode
     * repaint tail needs the render engine, so the refresh call keeps a
     * null rect, like the other deferred rect seams).
     */
    static int gameMouseSetBouncingCursorFrmId(FrmId frmId) {
        if (!initialized) {
            return -1;
        }

        if (frmId == null || !frmId.valid()) {
            return -1;
        }

        if (bouncingCursor.fid == frmId.fid()) {
            return -1;
        }

        if (!mapperMode) {
            return ObjectSystem.objectSetFrmId(bouncingCursor, frmId);
        }

        int refreshFlags = 0;

        if (bouncingCursor.fid != FrmId.EMPTY_FID) {
            refreshFlags |= REFRESH_BOUNCING_CURSOR;
        }

        int rc = -1;
        if (ObjectSystem.objectSetFrmId(bouncingCursor, frmId) == 0) {
            rc = 0;
            refreshFlags |= REFRESH_HEX_CURSOR;
        }

        if ((hexCursor.flags.and(ObjectFlags.HIDDEN)).equals(ObjectFlags.NONE)) {
            TileMath.tileWindowRefreshRect(null, org.fallout2ce.Map.gElevation);
        }

        return rc;
    }

    /** Port of {@code gameMouseResetBouncingCursorFrmId} (0x44CD0C). */
    static void gameMouseResetBouncingCursorFrmId() {
        gameMouseSetBouncingCursorFrmId(
                FrmId.ofType(ObjectType.INTERFACE, ArtDefs.InterfaceFrameId.Blank.id()));
    }

    /**
     * Port of {@code gameMouse3dSetFlatFrmId} (0x44DF1C, static in C):
     * stores the hex fid (rect deferred, like the other setters).
     */
    static int gameMouse3dSetFlatFrmId(FrmId frmId) {
        if (ObjectSystem.objectSetFrmId(hexCursor, frmId) == 0) {
            return 0;
        }

        return -1;
    }

    /**
     * Port of {@code gameMouseUpdateHexCursorFid} (0x44DF40, static in C):
     * no-op when the hex already shows the mode fid, else the flat set.
     */
    static int gameMouseUpdateHexCursorFid() {
        int frmId = MODE_FRM_IDS[mode];
        if (FrmId.ofFid(hexCursor.fid).frameId() == frmId) {
            return -1;
        }

        return gameMouse3dSetFlatFrmId(FrmId.ofType(ObjectType.INTERFACE, frmId));
    }

    /**
     * Port of {@code _gmouse_3d_move_to} (0x44DF94, static in C): game
     * (non-mapper) branch moves the hex by pixel offsets outside MOVE mode
     * and re-places both cursors from the mouse tile in MOVE mode (out-rect
     * unions deferred, like the other render tails); the mapper branch
     * mirrors C (square-tile math, executable/roof/dude guards, offsets).
     */
    static int gmouse3dMoveTo(int x, int y, int elevation) {
        if (!mapperMode) {
            if (mode != GameMouseMode.GAME_MOUSE_MODE_MOVE.id()) {
                int offsetX = 0;
                int offsetY = 0;
                try (Art.ArtLock lock = Art.artLock(FrmId.ofFid(hexCursor.fid))) {
                    if (lock != null && lock.art() != null) {
                        int[] rotationX = new int[1];
                        int[] rotationY = new int[1];
                        Art.artGetRotationOffsets(lock.art(), Rotation.NE.value(), rotationX, rotationY);

                        int[] frameX = new int[1];
                        int[] frameY = new int[1];
                        Art.artGetFrameOffsets(lock.art(), 0, Rotation.NE.value(), frameX, frameY);

                        offsetX += frameX[0];
                        offsetY += frameY[0];
                    }
                }

                ObjectSystem.objMove(hexCursor, x + offsetX, y + offsetY, elevation);
            } else {
                int tile = TileMath.tileFromScreenXY(x, y);
                if (tile != -1) {
                    int[] screenX = new int[1];
                    int[] screenY = new int[1];
                    if (TileMath.tileToScreenXY(tile, screenX, screenY) == 0) {
                        ObjectSystem.objMove(bouncingCursor, screenX[0] + 16, screenY[0] + 15, 0);
                    }

                    ObjectSystem.objectSetLocation(hexCursor, tile, elevation);
                }
            }
            return 0;
        }

        int tile;
        int x1 = 0;
        int y1 = 0;

        int fid = bouncingCursor.fid;
        if (FrmId.ofFid(fid).objectType() == ObjectType.TILE) {
            int squareTile = TileMath.squareTileFromScreenXY(x, y, elevation);
            if (squareTile != -1) {
                tile = MapDefs.HEX_GRID_WIDTH * (2 * (squareTile / MapDefs.SQUARE_GRID_WIDTH) + 1)
                        + 2 * (squareTile % MapDefs.SQUARE_GRID_WIDTH) + 1;
                x1 = -8;
                y1 = 13;

                GameObject dude = ObjectSystem.getDude();
                if (Settings.settings.system.executableIsMapper() && TileMath.tileRoofIsVisible
                        && dude != null
                        && dude.flags.and(ObjectFlags.HIDDEN).equals(ObjectFlags.NONE)) {
                    y1 = -83;
                }
            } else {
                tile = -1;
            }
        } else {
            tile = TileMath.tileFromScreenXY(x, y);
        }

        if (tile != -1) {
            if (ObjectSystem.objectSetLocation(bouncingCursor, tile, elevation) == 0) {
                if (x1 != 0 || y1 != 0) {
                    ObjectSystem.objOffset(bouncingCursor, x1, y1);
                }
            }

            if (mode != GameMouseMode.GAME_MOUSE_MODE_MOVE.id()) {
                int offsetX = 0;
                int offsetY = 0;
                try (Art.ArtLock lock = Art.artLock(FrmId.ofFid(hexCursor.fid))) {
                    if (lock != null && lock.art() != null) {
                        int[] rotationX = new int[1];
                        int[] rotationY = new int[1];
                        Art.artGetRotationOffsets(lock.art(), Rotation.NE.value(), rotationX, rotationY);

                        int[] frameX = new int[1];
                        int[] frameY = new int[1];
                        Art.artGetFrameOffsets(lock.art(), 0, Rotation.NE.value(), frameX, frameY);

                        offsetX += frameX[0];
                        offsetY += frameY[0];
                    }
                }

                ObjectSystem.objMove(hexCursor, x + offsetX, y + offsetY, elevation);
            }
        }

        return 0;
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

    /** Test seam: full state reset plus FRM unlocks. */
    static void resetForTests() {
        if (cursorFrm != null) {
            cursorFrm.close();
            cursorFrm = null;
        }
        gameMouseActionMenuFree();
        bouncingCursor = null;
        hexCursor = null;
        mapperMode = false;
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
        ObjectSystem.resetForTests();
    }
}
