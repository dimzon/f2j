package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [worldmaptiles]} vectors from
 * {@code docker/refharness/golden/v139.txt}, produced by
 * {@code sectionWorldmapTiles} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The section loads the tiles the engine's own {@code wmWorldMap_init}
 * loads from {@code data\WORLDMAP.TXT} in the game database and pins the
 * grid (4 tiles wide, 20 tiles, a 1400x1500 world), the fields of the
 * first tiles (art 339 to 341 and 358 for the last, difficulty -15, walk
 * mask wrldmp00), the 42 subtile strings every tile names, and the
 * radius marking: the ring around a visited center goes known, the
 * center itself visited.
 *
 * <p>The engine's fresh subtile at 100,100 reads known in the vector
 * because its full init also marks the radius around the party's start
 * position; this slice loads only the tiles, so its fresh subtile reads
 * unknown and the radius probe is what pins the marking.
 *
 * <p>Isolation: the tile list is static state, so setup tears it down
 * along with the config/VFS globals the parse reads through.
 */
class WorldmapTilesTest {

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        MapRegistry.resetForTests();
        MapAreas.resetForTests();
        Map.resetForTests();
        WorldmapTiles.resetForTests();

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
    void theGridComesOutOfTheFile() {
        assertEquals(0, WorldmapTiles.wmTilesInit());

        // num-horizontal=4 tiles=20 bounds=1400,1500
        assertEquals(4, WorldmapTiles.wmNumHorizontalTiles());
        assertEquals(20, WorldmapTiles.wmMaxTileNum());
        assertEquals(1400, WorldmapTiles.WM_TILE_WIDTH * WorldmapTiles.wmNumHorizontalTiles());
        assertEquals(1500, WorldmapTiles.WM_TILE_HEIGHT
            * (WorldmapTiles.wmMaxTileNum() / WorldmapTiles.wmNumHorizontalTiles()));
    }

    @Test
    void theTilesKeepTheirFields() {
        assertEquals(0, WorldmapTiles.wmTilesInit());

        // tile 0 art=339 diff=1/0 mask=1/wrldmp00
        assertEquals(339, WorldmapTiles.tileAt(0).fid);
        assertEquals(0, WorldmapTiles.tileAt(0).encounterDifficultyModifier);
        assertEquals("wrldmp00", WorldmapTiles.tileAt(0).walkMaskName);

        // tile 1 art=340 (no mask), tile 2 art=341 diff=-15
        assertEquals(340, WorldmapTiles.tileAt(1).fid);
        assertEquals("", WorldmapTiles.tileAt(1).walkMaskName);
        assertEquals(341, WorldmapTiles.tileAt(2).fid);
        assertEquals(-15, WorldmapTiles.tileAt(2).encounterDifficultyModifier);

        // tiles=20 last-art=358
        assertEquals(358, WorldmapTiles.tileAt(19).fid);
    }

    @Test
    void theSubtileStringsParse() {
        assertTrue(Config.configRead(GameConfig.gameConfig, "data\\worldmap.txt", true));
        assertEquals(0, MapRegistry.wmMapInit());
        assertEquals(0, WorldmapTerrain.wmParseTerrainTypes(GameConfig.gameConfig));
        assertEquals(0, WorldmapTiles.wmParseTilesConfig(GameConfig.gameConfig));

        // tile0-st-0=Ocean,Fill_W,None,None,None,Fish_O
        WorldmapTiles.SubtileInfo st0 = WorldmapTiles.tileAt(0).subtiles[0][0];
        WorldmapTerrain.Terrain ocean = WorldmapTerrain.terrainAt(3);
        assertEquals(3, st0.terrain);
        assertEquals("ocean", ocean.lookupName);
        assertEquals(4, st0.fill); // fill_w
        assertEquals(0, st0.encounterChance[0]);
        assertEquals(0, st0.encounterChance[1]);
        assertEquals(0, st0.encounterChance[2]);
        assertEquals("Fish_O", st0.encounterType);

        // tile0-st-last=Desert,No_Fill,Uncommon,Uncommon,Uncommon,Kla_D
        WorldmapTiles.SubtileInfo stLast = WorldmapTiles.tileAt(0).subtiles[5][6];
        assertEquals(0, stLast.terrain); // desert
        assertEquals(0, stLast.fill); // no_fill
        assertEquals(2, stLast.encounterChance[0]);
        assertEquals(2, stLast.encounterChance[2]);
        assertEquals("Kla_D", stLast.encounterType);
    }

    @Test
    void theRadiusMarkingWritesTheGrid() {
        assertEquals(0, WorldmapTiles.wmTilesInit());

        // Every subtile starts unknown: the 42 strings of every tile are
        // required by the load, and tile0-subtile-keys=42 in the vector.
        WorldmapTiles.SubtileState[] state = new WorldmapTiles.SubtileState[1];
        assertEquals(0, WorldmapTiles.wmSubTileGetVisitedState(100, 100, state));
        assertEquals(WorldmapTiles.SubtileState.SUBTILE_STATE_UNKNOWN, state[0]);

        // Outside the tiles there is no subtile to read.
        assertEquals(-1, WorldmapTiles.wmSubTileGetVisitedState(1400, 0, state));
        assertEquals(-1, WorldmapTiles.wmSubTileGetVisitedState(0, 1500, state));

        // radius rc=0 at 750,750 - well inside the 1400x1500 world - then
        // the 3x3 subtile block: the ring known, the center visited.
        assertEquals(0, WorldmapTiles.wmSubTileMarkRadiusVisited(750, 750, 1));
        for (int y = 700; y <= 800; y += 50) {
            for (int x = 700; x <= 800; x += 50) {
                assertEquals(0, WorldmapTiles.wmSubTileGetVisitedState(x, y, state));
                assertEquals(x == 750 && y == 750
                    ? WorldmapTiles.SubtileState.SUBTILE_STATE_VISITED
                    : WorldmapTiles.SubtileState.SUBTILE_STATE_KNOWN,
                    state[0], x + "," + y);
            }
        }
    }
}
