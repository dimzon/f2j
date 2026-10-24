package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The encounter-table header vectors {@code [worldmaptiles]} pins in
 * golden/v143: 76 tables, with the first two (DstN: 3 maps, 5 entries;
 * DstS: 3 maps, 8 entries).
 */
class WorldmapEncountersTest {

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
        WorldmapEncounters.resetForTests();

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
    void encounterTableVectors() {
        assertEquals(0, MapRegistry.wmMapInit());
        assertEquals(0, WorldmapTiles.wmTilesInit());
        assertEquals(0, WorldmapEncounters.wmParseEncounterTables(GameConfig.gameConfig));

        assertEquals(76, WorldmapEncounters.wmMaxEncounterInfoTables());

        WorldmapEncounters.EncounterTable dstN = WorldmapEncounters.tableAt(0);
        assertEquals("DstN", dstN.lookupName);
        assertEquals(3, dstN.mapsLength);
        assertEquals(5, dstN.entriesLength);
        assertEquals("desert encounter 1",
            MapRegistry.lookupName(dstN.maps[0].id()).toLowerCase());
        assertEquals("desert encounter 2",
            MapRegistry.lookupName(dstN.maps[1].id()).toLowerCase());
        assertEquals("desert encounter 3",
            MapRegistry.lookupName(dstN.maps[2].id()).toLowerCase());

        WorldmapEncounters.EncounterTable dstS = WorldmapEncounters.tableAt(1);
        assertEquals("DstS", dstS.lookupName);
        assertEquals(3, dstS.mapsLength);
        assertEquals(8, dstS.entriesLength);
    }
}
