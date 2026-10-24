package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [mapareas]} vectors from
 * {@code docker/refharness/golden/v106.txt}, produced by
 * {@code sectionMapAreas} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The section loads the area table {@code wmAreaInit} builds from
 * {@code data\CITY.TXT} in the game database (on top of the map table) and
 * queries it four ways. The interesting coverage is indirect but strong: the
 * lookups resolve a map to an area only by scanning the area's entrance
 * list, so map 0 landing in area 26, map 3 in area 0 and map 150 in area 47
 * proves the entrances parsed, the {@code lookup_name} mapping worked, and
 * the area table is in the right order.
 *
 * <p>{@code same 0-0=0} is a real semantic pin rather than a missing lookup:
 * maps 0 and 0 do resolve to one area, but neither is saveable, so
 * {@code mapAreSameArea} bails before comparing areas.
 *
 * <p>Isolation: both tables are static state, so setup tears down the area
 * and map registries along with the config/VFS globals.
 */
class MapAreasTest {

    private String savedLanguage;

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path p = Paths.get(root, name);
        if (!Files.isRegularFile(p)) {
            throw new IllegalStateException("missing " + p);
        }
        return p.toString();
    }

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        MapRegistry.resetForTests();
        MapAreas.resetForTests();
        Map.resetForTests();
        WorldmapTiles.resetForTests();

        savedLanguage = Settings.settings.system.language;
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        Settings.settings.system.language = Message.ENGLISH;
    }

    private void loadAreas() {
        assertEquals(0, MapAreas.wmAreaInit());
    }

    private static int matchArea(int mapIdx) {
        MapAreas.City[] area = new MapAreas.City[1];
        int rc = MapAreas.wmMatchAreaContainingMapIdx(mapIdx, area);
        return rc == 0 ? area[0].id() : -1;
    }

    @Test
    void tableSizeAndValidity() {
        loadAreas();

        int maxAreaNum = MapAreas.wmMaxAreaNum();
        assertEquals("init=0 maxarea=49 city-first=0 city-invalid=-1",
                "init=0"
                        + " maxarea=" + maxAreaNum
                        + " city-first=" + MapAreas.City.CITY_FIRST
                        + " city-invalid=" + MapAreas.City.CITY_INVALID.id());

        assertEquals("city-valid min=1 max=1 past=0",
                "city-valid min=" + (MapAreas.cityIsValid(MapAreas.City.CITY_FIRST) ? 1 : 0)
                        + " max=" + (MapAreas.cityIsValid(maxAreaNum - 1) ? 1 : 0)
                        + " past=" + (MapAreas.cityIsValid(maxAreaNum) ? 1 : 0));
    }

    @Test
    void mapResolvesThroughEntrances() {
        loadAreas();


        MapAreas.City[] area = new MapAreas.City[1];
        assertEquals(0, MapAreas.wmMatchAreaContainingMapIdx(0, area));

        assertEquals(26, area[0].id());

        assertEquals(0, MapAreas.wmMatchAreaContainingMapIdx(3, area));
        assertEquals(0, area[0].id());

        assertEquals(0, MapAreas.wmMatchAreaContainingMapIdx(150, area));
        assertEquals(47, area[0].id());

        assertEquals(26, matchArea(0));
        assertEquals(0, matchArea(3));
        assertEquals(47, matchArea(150));
    }

    @Test
    void sameAreaRespectsSaveableGuard() {
        loadAreas();

        // Both resolve to area 26, but neither map is saveable, so the guard
        // fires before any area comparison.
        assertFalse(Map.mapAreSameArea(0, 0));
        assertFalse(Map.mapAreSameArea(0, 150));
        assertFalse(Map.mapAreSameArea(-1, 0));

        assertEquals(26, Map.getMapIdxSame(0, 0));
        assertEquals(-1, Map.getMapIdxSame(0, 150));
    }

    @Test
    void loadedAreaFollowsHeaderIndex() {
        loadAreas();

        Map.gMapHeader.index = WorldmapDefs.Map.MAP_RND_DESERT_1;
        assertEquals(26, Map.mapGetLoadedAreaId());

        // Index 111 rather than the table's last entry (150): the table has
        // 151 rows but Map only names up to 149, so 150 cannot cross over.
        Map.gMapHeader.index = WorldmapDefs.ofMap(111);
        assertEquals(47, Map.mapGetLoadedAreaId());
    }

    /**
     * The area setters of the mark_area_known and wm_area_set_pos
     * builtins. The worldmap tiles are not ported, so the setters run the
     * same control flow C runs before its worldmap parse: the visible
     * state of an unlocked area is written, the world position is refused
     * by the tile bounds, and the visited marking stops at the subtile
     * the original looks up first.
     */
    @Test
    void areaSettersRunWithTheTilesUnloaded() {
        loadAreas();

        MapAreas.City arroyo = MapAreas.City.CITY_ARROYO;
        assertTrue(MapAreas.wmAreaSetVisibleState(arroyo,
            MapAreas.CityState.CITY_STATE_KNOWN, true));
        assertFalse(MapAreas.wmAreaSetVisibleState(MapAreas.City.CITY_INVALID,
            MapAreas.CityState.CITY_STATE_KNOWN, true));

        // Every position is refused while the tiles are not loaded: the x
        // bound is zero wide, whatever y would say.
        assertEquals(-1, MapAreas.wmAreaSetWorldPos(arroyo, 100, 100));
        assertEquals(-1, MapAreas.wmAreaSetWorldPos(arroyo, -1, 100));
        assertEquals(-1, MapAreas.wmAreaSetWorldPos(MapAreas.City.CITY_INVALID, 0, 0));

        // The marking stops at the subtile gate, so the city is not marked.
        assertFalse(MapAreas.wmAreaMarkVisitedState(arroyo, MapAreas.VisitedState.VISITED));
        assertFalse(MapAreas.wmAreaMarkVisitedState(MapAreas.City.CITY_INVALID,
            MapAreas.VisitedState.VISITED));
        assertEquals(0, MapAreas.wmAreaMarkVisited(arroyo));
    }

    /**
     * The area setters with the tiles of the worldmap loaded: the same
     * setters the test above runs unloaded, now inside the 1400x1500
     * world the tiles give. The position moves inside the bounds and is
     * refused outside, and the visited marking goes through: a visit of
     * an unknown area lands as known, the quirk of the original.
     */
    @Test
    void areaSettersRunWithTheTilesLoaded() {
        loadAreas();
        assertEquals(0, WorldmapTiles.wmTilesInit());

        MapAreas.City arroyo = MapAreas.City.CITY_ARROYO;
        assertEquals(0, MapAreas.wmAreaSetWorldPos(arroyo, 100, 100));
        assertEquals(-1, MapAreas.wmAreaSetWorldPos(arroyo, 1400, 0));
        assertEquals(-1, MapAreas.wmAreaSetWorldPos(arroyo, 0, 1500));
        assertEquals(-1, MapAreas.wmAreaSetWorldPos(arroyo, -1, 0));

        assertTrue(MapAreas.wmAreaSetVisibleState(arroyo,
            MapAreas.CityState.CITY_STATE_KNOWN, true));
        assertTrue(MapAreas.wmAreaMarkVisitedState(arroyo, MapAreas.VisitedState.VISITED));
        assertEquals(MapAreas.VisitedState.KNOWN.id(),
            MapAreas.wmAreaVisitedState(arroyo).id());
    }

    /** The map marking of the mark_area_known builtin, on the real table. */
    @Test
    void mapMarkingFollowsTheSaveableGuard() {
        loadAreas();

        // An invalid map is a miss.
        assertEquals(-1, MapAreas.wmMapMarkVisited(-1));

        // Map 0 resolves to area 26 but is not saveable, so the guard
        // walks away without marking anything.
        assertEquals(0, MapAreas.wmMapMarkVisited(0));

        // A saveable map that an area names goes through to the marking
        // (which stops at the subtile gate, the same as above).
        int saveableAndNamed = -1;
        for (int mapIdx = 0; mapIdx < 151 && saveableAndNamed < 0; mapIdx++) {
            if (MapRegistry.wmMapIdxIsSaveable(mapIdx)) {
                MapAreas.City[] area = new MapAreas.City[1];
                if (MapAreas.wmMatchAreaContainingMapIdx(mapIdx, area) == 0) {
                    saveableAndNamed = mapIdx;
                }
            }
        }
        assertTrue(saveableAndNamed >= 0, "no saveable map names an area");
        assertEquals(0, MapAreas.wmMapMarkVisited(saveableAndNamed));
    }
}
