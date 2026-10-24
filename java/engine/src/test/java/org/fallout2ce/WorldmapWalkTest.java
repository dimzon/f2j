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
 * The party walk of the world: the Bresenham line of
 * {@code wmPartyInitWalking} and the steps of {@code wmPartyWalkingStep},
 * on the real world data.
 */
class WorldmapWalkTest {

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        ProtoDb.resetForTests();
        MapRegistry.resetForTests();
        MapAreas.resetForTests();
        Map.resetForTests();
        WorldmapTiles.resetForTests();
        WorldmapTerrain.resetForTests();
        WorldmapEncounters.resetForTests();
        WorldmapGenData.resetForTests();

        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path master = Paths.get(root, "master.dat");
        if (!Files.isRegularFile(master)) {
            throw new IllegalStateException("missing " + master);
        }
        assertTrue(XFile.xbaseOpen(master.toString()));
        Settings.settings.system.language = Message.ENGLISH;

        assertTrue(Config.configRead(GameConfig.gameConfig, "data\\worldmap.txt", true));
        assertEquals(0, MapRegistry.wmMapInit());
        assertEquals(0, WorldmapTerrain.wmParseTerrainTypes(GameConfig.gameConfig));
        assertEquals(0, WorldmapTiles.wmParseTilesConfig(GameConfig.gameConfig));
        WorldmapGenData.wmGenDataInit();
    }

    @Test
    void thePartyWalksTheLine() {
        // Start 173,122 (the default start), walk to 200,150.
        assertEquals(173, WorldmapGenData.wmGetPartyWorldPosX());
        assertEquals(122, WorldmapGenData.wmGetPartyWorldPosY());

        WorldmapGenData.wmPartyInitWalking(200, 150);
        assertTrue(WorldmapGenData.wmPartyIsWalking());
        assertEquals(28, WorldmapGenData.wmPartyWalkDistance());

        int guard = 0;
        while (WorldmapGenData.wmPartyIsWalking() && guard < 1000) {
            WorldmapGenData.wmPartyWalkingStep();
            guard++;
        }

        assertFalse(WorldmapGenData.wmPartyIsWalking());
        assertEquals(0, WorldmapGenData.wmPartyWalkDistance());
        assertEquals(200, WorldmapGenData.wmGetPartyWorldPosX());
        assertEquals(150, WorldmapGenData.wmGetPartyWorldPosY());
    }

    @Test
    void aZeroWalkStaysPut() {
        WorldmapGenData.wmPartyInitWalking(173, 122);
        assertTrue(WorldmapGenData.wmPartyIsWalking());
        assertEquals(0, WorldmapGenData.wmPartyWalkDistance());

        WorldmapGenData.wmPartyWalkingStep();

        assertTrue(WorldmapGenData.wmPartyIsWalking());
        assertEquals(173, WorldmapGenData.wmGetPartyWorldPosX());
        assertEquals(122, WorldmapGenData.wmGetPartyWorldPosY());
    }
}
