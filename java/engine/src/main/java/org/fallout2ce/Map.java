package org.fallout2ce;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjTypes.ObjectFlags;

/**
 * New-map slice, mirroring {@code src/map.cc}: the map header
 * ({@code gMapHeader}), the transition ({@code gMapTransition},
 * {@code mapSetTransition}), the elevation ({@code gElevation},
 * {@code mapSetElevation}), the square-grid reset ({@code _square_reset})
 * and {@code mapNewMap} (0x482938) with {@code _map_place_dude_and_mouse}.
 *
 * <p>Vectors: {@code [mapnewmap]} in
 * {@code docker/refharness/golden/v98.txt}, produced by
 * {@code sectionMapChild} in {@code docker/refharness/ref_main.cc}:
 * {@code boot} (engine bring-up returns), {@code newmap} (elevation, center,
 * header, var lengths), {@code square} (grid fids after reset) and the
 * object dump (dude + egg on the center tile, hex cursor where the mouse
 * move left it) plus {@code objects=3 dude=...}.
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>Fid codec: the {@code TileFrmId} half layout (12 bits frame + 4 bits
 * flags per half, floor low / roof high) is mirrored by the private
 * helpers; the full art fid tables stay in {@link ArtDefs}.</li>
 * <li>Refresh tails ({@code tileWindowRefresh} is real;
 * {@code tileWindowRefreshRect} keeps a null rect like the other deferred
 * rect overloads in {@link ObjectSystem}).</li>
 * <li>Combat state ({@code isInCombat} / {@code _game_user_wants_to_quit})
 * is unported: the private seam returns false, like
 * {@link Stat}'s, so the outline/quit tails stay dormant.</li>
 * <li>Worldmap entrance marking ({@code wmMapMarkMapEntranceState}) is game
 * phase (needs the worldmap area model); the call is kept behind a
 * documented seam. The vectors never reach it (no exit grids on the dude
 * tile).</li>
 * <li>Script update proc ({@code scriptsExecMapUpdateProc}) is game phase;
 * {@code mapSid} stays {@code -1} (fresh-process mirror), so the call is
 * skipped exactly like C.</li>
 * <li>Roof-fill rendering ({@code tile_fill_roof}) is render engine (game
 * phase); the roof-cache state update is kept, the fill calls are
 * deferred. The vectors never reach them (empty squares).</li>
 * <li>Stencil ({@code tile_hires_stencil_on_center_tile_or_elevation_change})
 * is render culling (game phase, unported); skipped with this NOTE. The
 * harness vectors never observe stencil state.</li>
 * <li>Both {@code gElevation} mirrors ({@link #gElevation} here and
 * {@link TileMath#gTileWindowRect}'s parked copy) are starting from 0;
 * {@link #mapSetElevation} keeps them in sync.</li>
 * </ul>
 */
public final class Map {

    private Map() {
    }

    /** Port of {@code TileData} ({@code src/map_defs.h}): one grid's fids. */
    public static final class TileData {
        public final int[] tileFid = new int[MapDefs.SQUARE_GRID_SIZE];
    }

    /** Port of {@code MapHeader} ({@code src/map_defs.h}), full field set. */
    public static final class MapHeader {
        public int version;
        public final byte[] name = new byte[16];
        public int enteringTile;
        public int enteringElevation;
        public Rotation enteringRotation = Rotation.NE;
        public int localVariablesCount;
        public int scriptIndex;
        public int flags;
        public int darkness;
        public int globalVariablesCount;
        public WorldmapDefs.Map index;
        public long lastVisitTime;
        public final int[] field3C = new int[44];
    }

    /** Port of {@code MapTransition} ({@code src/map_defs.h}). */
    public static final class MapTransition {
        public WorldmapDefs.Map map;
        public int tile;
        public int elevation;
        public Rotation rotation = Rotation.NE;
    }

    /** 12-bit frame + 4-bit flags half-fid ({@code HalfTileFrmId}). */
    static final int HALF_FID_FRAME_MASK = 0x0FFF;
    static final int HALF_FID_FLAGS_MASK = 0xF000;
    static final int HALF_FID_FLAGS_POSITION = 12;
    static final int ROOF_HALF_FID_SHIFT = 16;

    /** Port of {@code _square} ({@code src/map.cc}): one grid per elevation. */
    private static final TileData[] square = {
        new TileData(), new TileData(), new TileData(),
    };

    /** Port of {@code gElevation} ({@code src/map.cc}, BSS 0). */
    public static int gElevation;

    /** Port of {@code gCenterTile} reads: the scroll domain owns it. */
    static int centerTile() {
        return TileMath.gCenterTile;
    }

    /** Port of {@code gMapHeader} ({@code src/map.cc}). */
    public static final MapHeader gMapHeader = new MapHeader();

    /** Port of {@code gMapTransition} ({@code src/map.cc}, static). */
    private static final MapTransition mapTransition = new MapTransition();

    /** Script-side map id ({@code gMapSid}, fresh-process mirror: -1). */
    private static int mapSid = -1;

    /** Square grids for {@link TileMath#tileInit} (C passes {@code _square}). */
    static TileData[] squareGrids() {
        return square;
    }

    /** Test seam: fresh-process mirror (elevation/header/transition/square). */
    static void resetForTests() {
        gElevation = 0;
        gMapHeader.version = 0;
        java.util.Arrays.fill(gMapHeader.name, (byte) 0);
        gMapHeader.enteringTile = 0;
        gMapHeader.enteringElevation = 0;
        gMapHeader.enteringRotation = Rotation.NE;
        gMapHeader.localVariablesCount = 0;
        gMapHeader.scriptIndex = 0;
        gMapHeader.flags = 0;
        gMapHeader.darkness = 0;
        gMapHeader.globalVariablesCount = 0;
        gMapHeader.index = null;
        gMapHeader.lastVisitTime = 0;
        java.util.Arrays.fill(gMapHeader.field3C, 0);
        mapTransition.map = null;
        mapTransition.tile = 0;
        mapTransition.elevation = 0;
        mapTransition.rotation = Rotation.NE;
        mapSid = -1;
        for (TileData grid : square) {
            java.util.Arrays.fill(grid.tileFid, 0);
        }
    }

    /**
     * Port of {@code mapSetTransition} (0x483578): null guard, struct copy,
     * {@code MAP_FIRST} (Java: {@code MAP_RND_DESERT_1}, the C alias target)
     * normalization and the combat-quit tail (dormant: {@link #isInCombat}
     * seam, like {@link Stat}).
     */
    public static int mapSetTransition(MapTransition transition) {
        if (transition == null) {
            return -1;
        }

        mapTransition.map = transition.map;
        mapTransition.tile = transition.tile;
        mapTransition.elevation = transition.elevation;
        mapTransition.rotation = transition.rotation;

        if (mapTransition.map == WorldmapDefs.Map.MAP_RND_DESERT_1) {
            mapTransition.map = WorldmapDefs.Map.MAP_TRANSITION;
        }

        if (isInCombat()) {
            // _game_user_wants_to_quit = GAME_QUIT_REQUEST_END_COMBAT:
            // combat phase, deferred (see class notes).
        }

        return 0;
    }

    /** Seam for {@code isInCombat} (combat state unported; cf. {@link Stat}). */
    private static boolean isInCombat() {
        return false;
    }

    /**
     * Seam for {@code wmMapMarkMapEntranceState} (worldmap area model, game
     * phase, unported); kept so the exit-grid scan in
     * {@link ObjectSystem#objectSetLocation} stays structurally 1:1. The
     * vectors never reach it.
     */
    static int wmMapMarkMapEntranceState(int mapIdx, int elevation, int state) {
        return -1;
    }

    /**
     * Port of {@code map_set_elevation} (0x482158): validity gate, mouse
     * hide/show around the cursor swap (the {@code WAIT_PLANET} guard reads
     * the live cursor like C), the entrance-mark branch (kept behind the
     * elevation-change guard with the worldmap seam), elevation store,
     * animation clear (queue seam), dude stand, party sync, the
     * map-update-proc guard ({@code mapSid == -1} skips, like C), the
     * visibility restore and the stencil tail (render culling, deferred,
     * see class notes).
     */
    public static int mapSetElevation(int elevation) {
        if (!MapDefs.elevationIsValid(elevation)) {
            return -1;
        }

        boolean gameMouseWasVisible = false;
        if (GameMouse.gameMouseGetCursor() != GameMouse.MouseCursorType.MOUSE_CURSOR_WAIT_PLANET.id()) {
            gameMouseWasVisible = GameMouse.gameMouseObjectsIsVisible();
            GameMouse.gameMouseObjectsHide();
            GameMouse.gameMouseSetCursor(GameMouse.MouseCursorType.MOUSE_CURSOR_NONE.id());
        }

        if (elevation != gElevation) {
            wmMapMarkMapEntranceState(mapIndex(), elevation, 1);
        }

        gElevation = elevation;
        TileMath.gElevation = elevation;

        Animation.regAnimClear(ObjectSystem.getDude());
        GameObject dude = ObjectSystem.getDude();
        if (dude != null) {
            Animation.dudeStand(dude, dude.rotation.id(), FrmId.ofFid(dude.fid));
        }
        PartyMembers.partyMemberSyncPosition();

        if (mapSid != -1) {
            // scriptsExecMapUpdateProc(): script engine, game phase.
        }

        if (gameMouseWasVisible) {
            GameMouse.gameMouseObjectsShow();
        }

        // tile_hires_stencil_on_center_tile_or_elevation_change():
        // render culling, deferred (see class notes).

        return 0;
    }

    /** Map index read for the entrance-mark call (header domain). */
    private static int mapIndex() {
        return gMapHeader.index != null ? gMapHeader.index.id() : 0;
    }

    /** Port of {@code mapSetStart} (0x4824CC): sets entering tile/elevation/rotation in the header. */
    public static void mapSetStart(int tile, int elevation, Rotation rotation) {
        gMapHeader.enteringTile = tile;
        gMapHeader.enteringElevation = elevation;
        gMapHeader.enteringRotation = rotation;
    }

    /** Port of {@code mapGetCurrentMap} (0x4826B8): returns the header map index. */
    public static int mapGetCurrentMap() {
        return mapIndex();
    }

    /**
     * Port of {@code _square_reset} (0x484210): every grid fid becomes the
     * grid frame ({@code TileFrameId::Grid}) with the old half flags kept.
     */
    static void squareReset() {
        int gridFrameId = ArtDefs.TileFrameId.Grid.id();
        for (int elevation = 0; elevation < MapDefs.ELEVATION_COUNT; elevation++) {
            int[] fids = square[elevation].tileFid;
            for (int i = 0; i < fids.length; i++) {
                int fid = fids[i];
                int floorFlags = (fid & HALF_FID_FLAGS_MASK) >> HALF_FID_FLAGS_POSITION;
                int roofFlags = ((fid >> ROOF_HALF_FID_SHIFT) & HALF_FID_FLAGS_MASK) >> HALF_FID_FLAGS_POSITION;
                int floorHalf = (gridFrameId & HALF_FID_FRAME_MASK) | (floorFlags << HALF_FID_FLAGS_POSITION);
                int roofHalf = (gridFrameId & HALF_FID_FRAME_MASK) | (roofFlags << HALF_FID_FLAGS_POSITION);
                fids[i] = (floorHalf & 0xFFFF) | ((roofHalf & 0xFFFF) << ROOF_HALF_FID_SHIFT);
            }
        }
    }

    /**
     * Port of {@code _map_place_dude_and_mouse} (0x48411C): seen-bits clear,
     * dude fid fix-up (already {@code STAND} here), placement on the center
     * tile with the header rotation (lands the egg too, via the
     * {@code objectSetLocation} dude-tail), light store, {@code NO_SAVE},
     * dude stand, party sync, bouncing-cursor fid reset, mouse show and the
     * window refresh.
     */
    static void placeDudeAndMouse() {
        ObjectSystem.objClearSeen();

        GameObject dude = ObjectSystem.getDude();
        if (dude != null) {
            if (FrmId.ofFid(dude.fid).animationType() != AnimationType.STAND.value()) {
                ObjectSystem.objectSetFrame(dude, 0);
                dude.fid = FrmId.ofCritter(FrmId.ofFid(dude.fid).frameId(),
                        AnimationType.STAND.value(), WeaponAnimation.NONE.value(),
                        dude.rotation.id() + 1).fid();
            }

            if (dude.tile == -1) {
                ObjectSystem.objectSetLocation(dude, TileMath.gCenterTile, gElevation);
                ObjectSystem.objectSetRotation(dude, gMapHeader.enteringRotation.value());
            }

            ObjectSystem.objectSetLight(dude, 4, 0x10000);
            dude.flags = dude.flags.or(ObjectFlags.NO_SAVE);

            Animation.dudeStand(dude, dude.rotation.id(), FrmId.ofFid(dude.fid));
            PartyMembers.partyMemberSyncPosition();
        }

        GameMouse.gameMouseResetBouncingCursorFrmId();
        GameMouse.gameMouseObjectsShow();
    }

    /**
     * Port of {@code mapNewMap} (0x482938): edge free, elevation 0, center
     * 20100, transition/header reset, remove-all (NO_REMOVE objects survive:
     * dude, egg, both mouse cursors), animation stop, map var frees (both
     * arrays are null in the vectored setup, like the fresh C child),
     * square reset, dude+mouse placement and the window refresh.
     */
    public static void mapNewMap() {
        MapEdge.mapEdgeFree();
        mapSetElevation(0);
        TileMath.tileSetCenter(20100, TileMath.TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS);
        gMapHeader.enteringElevation = 0;
        gMapHeader.enteringRotation = Rotation.NE;
        gMapHeader.localVariablesCount = 0;
        gMapHeader.version = 20;
        gMapHeader.name[0] = 0;
        gMapHeader.enteringTile = 20100;
        ObjectSystem.objRemoveAll();
        Animation.animationStop();

        // NOTE: Uninline (C keeps the static frees here).
        MapVars.mapGlobalVariablesFree();
        MapVars.mapLocalVariablesFree();

        squareReset();
        placeDudeAndMouse();
        TileMath.tileWindowRefresh();
    }
}
