package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The terrain vectors {@code [worldmaptiles]} pins in golden/v142: the
 * four terrains of {@code data\worldmap.txt} (Desert/1, Mountain/2,
 * City/1, Ocean/1) and the first random maps of the first terrain.
 */
class WorldmapTerrainTest {

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        MapRegistry.resetForTests();
        MapAreas.resetForTests();
        Map.resetForTests();
        WorldmapTiles.resetForTests();
        WorldmapTerrain.resetForTests();

        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path master = Paths.get(root, "master.dat");
        if (!Files.isRegularFile(master)) {
            throw new IllegalStateException("missing " + master);
        }
        org.junit.jupiter.api.Assertions.assertTrue(
            XFile.xbaseOpen(master.toString()));
        Settings.settings.system.language = Message.ENGLISH;
    }

    @Test
    void terrainVectors() {
        // The tiles and the map table come first so the config is open
        // and the random maps resolve; the terrain parse reads the same
        // config, like wmConfigInit does.
        assertEquals(0, MapRegistry.wmMapInit());
        assertEquals(0, WorldmapTiles.wmTilesInit());
        int rc = WorldmapTerrain.wmParseTerrainTypes(GameConfig.gameConfig);
        assertEquals(0, rc);

        assertEquals(4, WorldmapTerrain.wmMaxTerrainTypes());

        WorldmapTerrain.Terrain desert = WorldmapTerrain.terrainAt(0);
        assertEquals("desert", desert.lookupName);
        assertEquals(1, desert.difficulty);
        assertTrue(desert.mapsLength >= 2);

        assertEquals("mountain", WorldmapTerrain.terrainAt(1).lookupName);
        assertEquals(2, WorldmapTerrain.terrainAt(1).difficulty);

        assertEquals("city", WorldmapTerrain.terrainAt(2).lookupName);
        assertEquals(1, WorldmapTerrain.terrainAt(2).difficulty);

        assertEquals("ocean", WorldmapTerrain.terrainAt(3).lookupName);
        assertEquals(1, WorldmapTerrain.terrainAt(3).difficulty);

        // The random maps resolve to real maps of the game, the way the
        // original resolves them (map_00/map_01 of the first terrain).
        assertTrue(desert.maps[0].id() >= 0);
        assertTrue(desert.maps[1].id() >= 0);
        assertEquals("desert encounter 1",
            MapRegistry.lookupName(desert.maps[0].id()).toLowerCase());
        assertEquals("desert encounter 2",
            MapRegistry.lookupName(desert.maps[1].id()).toLowerCase());
    }
}
