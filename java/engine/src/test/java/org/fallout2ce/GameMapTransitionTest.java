package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * {@code mapHandleTransition} (map.cc), pinned against the C++ section
 * {@code [maptransitionrun]} in the ref harness (golden v177).
 *
 * <p>The trivial return, the no-reload placement/centering path, the
 * in-combat gate (which also fires the {@code mapSetTransition} END_COMBAT
 * quit) and the {@code mapLoadById} invalid-index guard are real; the
 * worldmap UI branches (wmTownMap/wmWorldMap) and the map-music start are
 * hooks. The real-map reload branch is a harness gap (a second map load in
 * one harness process crashes), so it is pinned by the same code path the
 * first load runs.
 */
class GameMapTransitionTest {

    private GameObject dude;
    private int townMapCalls;
    private int worldMapCalls;
    private int musicCalls;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        Map.resetForTests();
        Combat.resetForTests();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
        WorldmapGenData.wmGenDataInit();

        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;

        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("master.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("patch000.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("critter.dat")));
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());
        assertEquals(0, Mouse.mouseInit());

        byte[] tileBuf = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf, 64, 64, 64,
                rect -> {
                }));

        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));
        dude = ObjectSystem.getDude();
        dude.flags = dude.flags.and(ObjTypes.ObjectFlags.HIDDEN.not());
        assertEquals(0, ObjectSystem.objectSetLocation(dude, 18492, 0));
        assertEquals(0, TileMath.tileSetCenter(18492, TileMath.TILE_SET_CENTER_REFRESH_WINDOW));

        // Golden header state: ARTEMPLE registers as map idx 111
        // (MAP_RND_COAST_11), the loaded map's index.
        Map.gMapHeader.index = WorldmapDefs.Map.MAP_RND_COAST_11;

        townMapCalls = 0;
        worldMapCalls = 0;
        musicCalls = 0;
        Map.wmTownMap = () -> townMapCalls++;
        Map.wmWorldMap = () -> worldMapCalls++;
        Map.wmMapMusicStart = () -> musicCalls++;
        GameScriptApi.gameUserWantsToQuit = GameScriptApi.GAME_QUIT_REQUEST_NONE;
    }

    private Map.MapTransition transition(WorldmapDefs.Map map, int tile, int elevation, Rotation rotation) {
        Map.MapTransition t = new Map.MapTransition();
        t.map = map;
        t.tile = tile;
        t.elevation = elevation;
        t.rotation = rotation;
        return t;
    }

    @Test
    void aZeroTransitionReturnsImmediately() {
        assertEquals(0, Map.mapHandleTransition());
        assertEquals(18492, dude.tile);
        assertEquals(0, dude.elevation);
        assertEquals(0, townMapCalls);
        assertEquals(0, musicCalls);
    }

    @Test
    void sameMapElevationChangeSkipsReloadAndMovesTheDude() {
        assertEquals(0, Map.mapSetTransition(transition(
                WorldmapDefs.Map.MAP_RND_COAST_11, 20100, 1, Rotation.SW)));

        assertEquals(0, Map.mapHandleTransition());

        // Golden [maptransitionrun] elev: dude=20100,1,3 center=20100.
        assertEquals(20100, dude.tile);
        assertEquals(1, dude.elevation);
        assertEquals(ObjTypes.Rotation.ROTATION_SW, dude.rotation);
        // No reload, no worldmap UI.
        assertEquals(0, townMapCalls);
        assertEquals(0, musicCalls);
        // The transition was cleared: the next handle is trivial.
        assertEquals(0, Map.mapHandleTransition());
        assertEquals(0, townMapCalls);
    }

    @Test
    void outOfCombatInvalidTransitionRunsTheTownMap() {
        assertEquals(0, Map.mapSetTransition(transition(
                WorldmapDefs.Map.MAP_INVALID, -1, 0, Rotation.INVALID)));

        assertEquals(0, Map.mapHandleTransition());

        assertEquals(1, townMapCalls);
        assertEquals(0, musicCalls);
        // The transition was cleared, so the next handle is trivial.
        assertEquals(0, Map.mapHandleTransition());
        assertEquals(1, townMapCalls);
    }

    @Test
    void inCombatInvalidTransitionIsGatedAndRequestsEndCombat() {
        // The in-combat gate: mapSetTransition raises the END_COMBAT quit and
        // mapHandleTransition skips the worldmap UI, leaving the transition
        // in place.
        Combat.combatState = Combat.COMBAT_STATE_IN_COMBAT | Combat.COMBAT_STATE_PLAYER_TURN;
        assertEquals(0, Map.mapSetTransition(transition(
                WorldmapDefs.Map.MAP_INVALID, -1, 0, Rotation.INVALID)));
        assertEquals(GameScriptApi.GAME_QUIT_REQUEST_END_COMBAT,
            GameScriptApi.gameUserWantsToQuit);

        assertEquals(0, Map.mapHandleTransition());
        assertEquals(0, townMapCalls);
        assertEquals(0, musicCalls);

        // Still in combat: still gated, the transition is untouched.
        assertEquals(0, Map.mapHandleTransition());
        assertEquals(0, townMapCalls);

        // Leaving combat releases the pending transition into the town map.
        GameScriptApi.gameUserWantsToQuit = GameScriptApi.GAME_QUIT_REQUEST_NONE;
        Combat.combatState = Combat.COMBAT_STATE_OUT_COMBAT;
        assertEquals(0, Map.mapHandleTransition());
        assertEquals(1, townMapCalls);
    }

    @Test
    void mapLoadByIdRejectsAnUnknownMapIndex() {
        // The registry has no data in this setup, so any index is invalid;
        // the guard return matches the C++ wmMapIdxToName failure path.
        assertEquals(-1, Map.mapLoadById(9000));
        assertEquals(0, musicCalls);
    }

    @Test
    void wmTeleportToAreaRefusesInvalidCities() {
        assertEquals(-1, MapAreas.wmTeleportToArea(-1));
        assertEquals(-1, MapAreas.wmTeleportToArea(999));
    }
}
