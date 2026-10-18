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

    /** Ports of {@code MAP_HEADER_ELEVATION_0/1/2} ({@code src/map_defs.h}). */
    static final int MAP_HEADER_ELEVATION_0 = 0x02;
    static final int MAP_HEADER_ELEVATION_1 = 0x04;
    static final int MAP_HEADER_ELEVATION_2 = 0x08;

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
     * Port of {@code _square_load} (0x48431C): reset, then for every elevation
     * whose flag is absent read {@code SQUARE_GRID_SIZE} fids, clearing the
     * roof TemporarilyHidden bit and mapping roof frame {@code Invalid} to
     * {@code Last}.
     */
    static int squareLoad(XFile.Handle stream, int flags) {
        squareReset();
        int[] elevFlags = { MAP_HEADER_ELEVATION_0, MAP_HEADER_ELEVATION_1, MAP_HEADER_ELEVATION_2 };
        for (int elevation = 0; elevation < MapDefs.ELEVATION_COUNT; elevation++) {
            if ((flags & elevFlags[elevation]) == 0) {
                int[] tileFids = square[elevation].tileFid;
                if (Db.fileReadInt32List(stream, tileFids, 0, MapDefs.SQUARE_GRID_SIZE) != 0) {
                    return -1;
                }
                for (int tile = 0; tile < MapDefs.SQUARE_GRID_SIZE; tile++) {
                    int fid = tileFids[tile];
                    int roofFlags = ((fid >> ROOF_HALF_FID_SHIFT) & HALF_FID_FLAGS_MASK) >> HALF_FID_FLAGS_POSITION;
                    roofFlags &= ~ArtDefs.TileFlags.TemporarilyHidden.bits();
                    int roofFrame = (fid >> ROOF_HALF_FID_SHIFT) & HALF_FID_FRAME_MASK;
                    if (roofFrame == ArtDefs.TileFrameId.Invalid.id()) {
                        roofFrame = ArtDefs.TileFrameId.Last.id();
                    }
                    int floorFrame = fid & HALF_FID_FRAME_MASK;
                    int floorFlags = (fid & HALF_FID_FLAGS_MASK) >> HALF_FID_FLAGS_POSITION;
                    tileFids[tile] = ((floorFrame & HALF_FID_FRAME_MASK) | (floorFlags << HALF_FID_FLAGS_POSITION))
                            | (((roofFrame & HALF_FID_FRAME_MASK) | (roofFlags << HALF_FID_FLAGS_POSITION))
                                    << ROOF_HALF_FID_SHIFT);
                }
            }
        }
        return 0;
    }

    /**
     * Port of {@code mapHeaderRead} (0x4844B4): version, name, entering
     * tile/elevation/rotation, local/script/flags/darkness/global counts,
     * map index, lastVisitTime, then 44 reserved ints.
     */
    static int mapHeaderRead(XFile.Handle stream) {
        int[] v = new int[1];
        if (Db.fileReadInt32(stream, v) == -1) return -1;
        gMapHeader.version = v[0];
        byte[] name = new byte[16];
        if (Db.fileReadFixedLengthString(stream, name, 0, 16) == -1) return -1;
        System.arraycopy(name, 0, gMapHeader.name, 0, 16);
        if (Db.fileReadInt32(stream, v) == -1) return -1;
        gMapHeader.enteringTile = v[0];
        if (Db.fileReadInt32(stream, v) == -1) return -1;
        gMapHeader.enteringElevation = v[0];
        if (Db.fileReadInt32(stream, v) == -1) return -1;
        gMapHeader.enteringRotation = Rotation.fromInt(v[0]);
        if (Db.fileReadInt32(stream, v) == -1) return -1;
        gMapHeader.localVariablesCount = v[0];
        if (Db.fileReadInt32(stream, v) == -1) return -1;
        gMapHeader.scriptIndex = v[0];
        if (Db.fileReadInt32(stream, v) == -1) return -1;
        gMapHeader.flags = v[0];
        if (Db.fileReadInt32(stream, v) == -1) return -1;
        gMapHeader.darkness = v[0];
        if (Db.fileReadInt32(stream, v) == -1) return -1;
        gMapHeader.globalVariablesCount = v[0];
        if (Db.fileReadInt32(stream, v) == -1) return -1;
        gMapHeader.index = WorldmapDefs.ofMap(v[0]);
        long[] u = new long[1];
        if (Db.fileReadUInt32(stream, u) == -1) return -1;
        gMapHeader.lastVisitTime = u[0];
        if (Db.fileReadInt32List(stream, gMapHeader.field3C, 0, 44) == -1) return -1;
        return 0;
    }

    /** Port of {@code mapLoadSaved} as a game-phase seam (save-game load unported). */
    static int mapLoadSaved(String fileName) {
        return -1;
    }

    /**
     * Port of {@code mapLoadByName} (0x482A68): uppercase the name, look for a
     * {@code .SAV} sibling, else load the {@code .MAP}; on success copy the name
     * into the header. The {@code .SAV}/{@code wmMapMusicStart} paths are game
     * phase (documented seams).
     */
    public static int mapLoadByName(String fileName) {
        String upper = fileName.toUpperCase();
        int rc = -1;
        if (!Settings.settings.system.executableIsMapper()) {
            int mapDot = upper.indexOf(".MAP");
            if (mapDot != -1) {
                String savName = upper.substring(0, mapDot) + ".SAV";
                XFile.Handle stream = Db.fileOpen("MAPS\\" + savName, "rb");
                if (stream != null) {
                    Db.fileClose(stream);
                    rc = mapLoadSaved(upper);
                }
            }
        }
        if (rc == -1) {
            XFile.Handle stream = Db.fileOpen("MAPS\\" + upper, "rb");
            if (stream != null) {
                rc = mapLoad(stream);
                Db.fileClose(stream);
            }
            if (rc == 0) {
                // gMapHeader.name = fileName; gDude whoHitMe = nullptr (dormant).
            }
        }
        return rc;
    }

    /**
     * Port of {@code mapLoad} (0x482B74): header, version, entering spot,
     * remove-all, map vars init/load, square load, elevation, tile center,
     * ambient light, dude placement and refresh. Script/object/random-encounter
     * phases are documented seams (script engine, object load, worldmap).
     */
    public static int mapLoad(XFile.Handle stream) {
        if (stream == null) {
            return -1;
        }
        if (mapHeaderRead(stream) != 0) {
            return -1;
        }
        if (gMapHeader.version != 19 && gMapHeader.version != 20) {
            return -1;
        }
        if (EnteringLocation.getEnteringElevation() == -1) {
            EnteringLocation.mapSetEnteringLocation(gMapHeader.enteringElevation,
                    gMapHeader.enteringTile, gMapHeader.enteringRotation);
        }
        ObjectSystem.objRemoveAll();
        if (gMapHeader.globalVariablesCount < 0) {
            gMapHeader.globalVariablesCount = 0;
        }
        if (gMapHeader.localVariablesCount < 0) {
            gMapHeader.localVariablesCount = 0;
        }
        if (MapVars.mapGlobalVariablesInit(gMapHeader.globalVariablesCount) != 0) {
            return -1;
        }
        if (MapVars.mapGlobalVariablesLoad(stream) != 0) {
            return -1;
        }
        if (MapVars.mapLocalVariablesInit(gMapHeader.localVariablesCount) != 0) {
            return -1;
        }
        if (MapVars.mapLocalVariablesLoad(stream) != 0) {
            return -1;
        }
        if (squareLoad(stream, gMapHeader.flags) != 0) {
            return -1;
        }

        if (ObjectSystem.scriptLoadAll(stream) != 0) {
            return -1;
        }
        if (ObjectSystem.objectLoadAll(stream) != 0) {
            return -1;
        }

        // _map_fix_critter_combat_data: game phase (script engine), seam.

        if (mapSetElevation(EnteringLocation.getEnteringElevation()) != 0) {
            return -1;
        }
        // mapEdgeLoad: game phase (worldmap edges).
        if (TileMath.tileSetCenter(EnteringLocation.getEnteringTile(),
                TileMath.TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS) != 0) {
            return -1;
        }
        Light.lightSetAmbientIntensity(Light.LIGHT_INTENSITY_MAX, false);
        GameObject dude = ObjectSystem.getDude();
        if (dude != null) {
            ObjectSystem.objectSetLocation(dude, EnteringLocation.getEnteringTile(), gElevation);
            ObjectSystem.objectSetRotation(dude, EnteringLocation.getEnteringRotation().value());
        }

        // Map script (scriptAdd): game phase (script engine). The C++ also
        // creates a hidden ScrollBlocker object for the map script; port the
        // object creation (the script binding itself is a documented seam).
        if (gMapHeader.scriptIndex > 0) {
            GameObject[] blockerPtr = new GameObject[1];
            ObjectSystem.objectCreateWithFrmIdProtoId(blockerPtr,
                    FrmId.ofType(ObjectType.MISC, ArtDefs.MiscFrameId.ScrollBlocker.id()),
                    ProtoTypes.ProtoId.empty());
            GameObject blocker = blockerPtr[0];
            if (blocker != null) {
                blocker.flags = blocker.flags.or(ObjectFlags.LIGHT_THRU)
                        .or(ObjectFlags.NO_SAVE).or(ObjectFlags.HIDDEN);
                ObjectSystem.objectSetLocation(blocker, 1, 0);
            }
        }

        // gMapHeader.index / globalVarsRead / scriptsEnable / wmSetupRandomEncounter:
        // game phase (worldmap, script engine).
        placeDudeAndMouse();
        // isoEnable / fileSetReadProgressHandler: game phase (tickers).
        return 0;
    }

    // 0x4843B8 mapHeaderWrite.
    static int mapHeaderWrite(MapHeader ptr, XFile.Handle stream) {
        if (Db.fileWriteInt32(stream, ptr.version) == -1) return -1;
        if (Db.fileWriteFixedLengthString(stream, ptr.name, 0, 16) == -1) return -1;
        if (Db.fileWriteInt32(stream, ptr.enteringTile) == -1) return -1;
        if (Db.fileWriteInt32(stream, ptr.enteringElevation) == -1) return -1;
        if (Db.fileWriteInt32(stream, ptr.enteringRotation.value()) == -1) return -1;
        if (Db.fileWriteInt32(stream, ptr.localVariablesCount) == -1) return -1;
        if (Db.fileWriteInt32(stream, ptr.scriptIndex) == -1) return -1;
        if (Db.fileWriteInt32(stream, ptr.flags) == -1) return -1;
        if (Db.fileWriteInt32(stream, ptr.darkness) == -1) return -1;
        if (Db.fileWriteInt32(stream, ptr.globalVariablesCount) == -1) return -1;
        if (Db.fileWriteInt32(stream, ptr.index != null ? ptr.index.id() : -1) == -1) return -1;
        if (Db.fileWriteUInt32(stream, ptr.lastVisitTime) == -1) return -1;
        if (Db.fileWriteInt32List(stream, ptr.field3C, 0, 44) == -1) return -1;
        return 0;
    }

    // 0x483980 _map_save_file.
    /**
     * Port of {@code _map_save_file} (0x483980).
     *
     * <p>Render boundary: three object fields ({@code sx}/{@code sy} and the
     * {@code OBJECT_SEEN} flag) are serialised by this path but written by the
     * renderer as a side effect in C++ — {@code _obj_render_object}
     * (object.cc:5029) overwrites {@code sx}/{@code sy} only for objects whose
     * art is cache-resident, and {@code _obj_process_seen} (object.cc:3136)
     * stamps {@code OBJECT_SEEN} from the tiles the render sweep visited. The
     * render engine is not ported, so those values are engine state rather
     * than save state here; the {@code [mapsave]} harness vectors normalise
     * them on both sides so the vector pins this save path itself.
     */
    static int mapSaveFile(XFile.Handle stream) {
        if (stream == null) {
            return -1;
        }
        // scriptsDisable: seam.
        int[] elevFlags = { MAP_HEADER_ELEVATION_0, MAP_HEADER_ELEVATION_1, MAP_HEADER_ELEVATION_2 };
        for (int elevation = 0; elevation < MapDefs.ELEVATION_COUNT; elevation++) {
            int tile;
            for (tile = 0; tile < MapDefs.SQUARE_GRID_SIZE; tile++) {
                int fid = square[elevation].tileFid[tile];
                if ((fid & HALF_FID_FRAME_MASK) != ArtDefs.TileFrameId.Grid.id()) break;
                if (((fid >> ROOF_HALF_FID_SHIFT) & HALF_FID_FRAME_MASK) != ArtDefs.TileFrameId.Grid.id()) break;
            }
            if (tile == MapDefs.SQUARE_GRID_SIZE) {
                GameObject object = ObjectSystem.objectFindFirstAtElevation(elevation);
                if (object != null) {
                    while (object != null && (object.flags.bits() & ObjectFlags.NO_SAVE.bits()) != 0) {
                        object = ObjectSystem.objectFindNextAtElevation();
                    }
                    if (object != null) {
                        gMapHeader.flags &= ~elevFlags[elevation];
                    } else {
                        gMapHeader.flags |= elevFlags[elevation];
                    }
                } else {
                    gMapHeader.flags |= elevFlags[elevation];
                }
            } else {
                gMapHeader.flags &= ~elevFlags[elevation];
            }
        }

        gMapHeader.localVariablesCount = MapVars.localVarsLength();
        gMapHeader.globalVariablesCount = MapVars.globalVarsLength();
        gMapHeader.darkness = 1;

        if (mapHeaderWrite(gMapHeader, stream) != 0) return -1;
        if (gMapHeader.globalVariablesCount != 0) {
            Db.fileWriteInt32List(stream, MapVars.globalVars(), 0, gMapHeader.globalVariablesCount);
        }
        if (gMapHeader.localVariablesCount != 0) {
            Db.fileWriteInt32List(stream, MapVars.localVars(), 0, gMapHeader.localVariablesCount);
        }
        for (int elevation = 0; elevation < MapDefs.ELEVATION_COUNT; elevation++) {
            if ((gMapHeader.flags & elevFlags[elevation]) == 0) {
                Db.fileWriteInt32List(stream, square[elevation].tileFid, 0, MapDefs.SQUARE_GRID_SIZE);
            }
        }

        // _win_msg: seam.
        ObjectSystem.scriptSaveAll(stream);
        ObjectSystem.objectSaveAll(stream);
        // scriptsEnable: seam.
        return 0;
    }

    /**
     * Port of {@code _map_save} (0x483850): open the save path, write the map
     * file, and (when not in-game) save the map edges (seam). Header name must
     * be set.
     */
    public static int mapSave(boolean isInGame) {
        int rc = -1;
        if (gMapHeader.name[0] != 0) {
            String name = new String(gMapHeader.name).trim();
            XFile.Handle stream = Db.fileOpen("MAPS\\" + name, "wb");
            if (stream != null) {
                rc = mapSaveFile(stream);
                Db.fileClose(stream);
            }
            if (rc == 0 && !isInGame) {
                // mapEdgeSave: seam (game phase).
            }
        }
        return rc;
    }

    /**
     * Port of {@code _map_save_in_game} (0x483C98): the leaving-map path.
     * Animation stop / party prep / random-encounter erase / automap are
     * game-phase seams; the save itself is {@link #mapSave}.
     */
    public static int mapSaveInGame(boolean isLeavingMap) {
        if (gMapHeader.name[0] == '\0') {
            return 0;
        }
        // animationStop / _partyMemberSaveProtos / _queue_leaving_map /
        // _partyMemberPrepLoad / scriptsExecMapExitProc / _obj_reset_roof:
        // game-phase seams.
        gMapHeader.flags |= 0x01; // MAP_HEADER_SAVED
        gMapHeader.lastVisitTime = GameTime.gameTimeGetTime();

        String name = new String(gMapHeader.name).trim();
        String savName = replaceExtension(name, "SAV");
        if (isLeavingMap && !mapIsSaveable()) {
            // Not saving a RANDOM encounter map: erase the .SAV sibling
            // (compat_remove; seam — no Java delete API in this slice).
        } else {
            byte[] saved = gMapHeader.name.clone();
            System.arraycopy(savName.getBytes(), 0, gMapHeader.name, 0,
                    Math.min(savName.length(), gMapHeader.name.length));
            int rc = mapSave(true);
            System.arraycopy(saved, 0, gMapHeader.name, 0, saved.length);
            if (rc == -1) {
                return -1;
            }
        }

        if (isLeavingMap) {
            gMapHeader.name[0] = '\0';
            ObjectSystem.objRemoveAll();
            squareReset();
        }
        return 0;
    }

    static String replaceExtension(String name, String ext) {
        int dot = name.lastIndexOf('.');
        return dot == -1 ? name + "." + ext : name.substring(0, dot + 1) + ext;
    }

    /** Seam for {@code wmMapIsSaveable} (worldmap saveable flag, game phase). */
    static boolean mapIsSaveable() {
        return false;
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
