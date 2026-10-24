package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The gen-data slice {@code [worldmaptiles]} pins in golden/v141:
 * the init leaves the party's area unset, and the position is the
 * default start (173,122) - the shipped config has no worldmap_x/y.
 */
class WorldmapGenDataTest {

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        ContentConfig.resetForTests();
        Db.resetForTests();
        MapRegistry.resetForTests();
        MapAreas.resetForTests();
        Map.resetForTests();
        WorldmapTiles.resetForTests();
        WorldmapGenData.resetForTests();

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

        ContentConfig.contentConfigInit();
    }

    @Test
    void theInitLeavesTheDefaultStart() {
        assertEquals(0, WorldmapTiles.wmTilesInit());
        WorldmapGenData.wmGenDataInit();

        // party-pos=173,122 party-area=-1
        assertEquals(173, WorldmapGenData.wmGetPartyWorldPosX());
        assertEquals(122, WorldmapGenData.wmGetPartyWorldPosY());
        assertEquals(MapAreas.City.CITY_INVALID.id(),
            WorldmapGenData.wmGetPartyCurArea().id());
    }
}
