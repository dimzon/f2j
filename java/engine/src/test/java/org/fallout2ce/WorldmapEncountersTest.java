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

        // The scalar fields of the first entries, pinned by the raw lines
        // of the vector (Chance:50%,Scenery:Normal,If...).
        WorldmapEncounters.EncounterTableEntry dstN0 = dstN.entries[0];
        assertEquals(50, dstN0.chance);
        assertEquals(0, dstN0.counter);
        assertEquals(false, dstN0.special);
        assertEquals(2, dstN0.scenery);
        assertEquals(25, dstN.entries[1].chance);
        assertEquals(0, dstN.entries[1].scenery);

        WorldmapEncounters.EncounterTableEntry dstS0 = dstS.entries[0];
        assertEquals(20, dstS0.chance);
        assertEquals(0, dstS0.scenery);

        // The sub-entries of the same lines: (2-5) Raiders AMBUSH Player.
        WorldmapEncounters.EncounterTableSubEntry sub0 = dstN0.subEntries[0];
        assertEquals(1, dstN0.subEntriesLength);
        assertEquals(2, sub0.minimumCount);
        assertEquals(5, sub0.maximumCount);
        assertEquals("Raiders", sub0.name);
        assertEquals(1, sub0.situation);
        assertEquals(-1, sub0.encounterIndex);

        WorldmapEncounters.EncounterTableSubEntry sub1 = dstN.entries[1].subEntries[0];
        assertEquals(3, sub1.minimumCount);
        assertEquals(8, sub1.maximumCount);
        assertEquals("Rad_Scorpions", sub1.name);

        WorldmapEncounters.EncounterTableSubEntry subS1 = dstS.entries[1].subEntries[0];
        assertEquals(2, subS1.minimumCount);
        assertEquals(5, subS1.maximumCount);
        assertEquals("Manti", subS1.name);

        // The conditions of the same lines: If(Player(Level) > 4).
        WorldmapEncounters.EncounterConditionEntry c0 = dstN0.condition.entries[0];
        assertEquals(1, dstN0.condition.entriesLength);
        assertEquals(WorldmapEncounters.ENCOUNTER_CONDITION_TYPE_PLAYER, c0.type);
        assertEquals(WorldmapEncounters.ENCOUNTER_CONDITIONAL_OPERATOR_GREATER_THAN,
            c0.conditionalOperator);
        assertEquals(4, c0.value);

        WorldmapEncounters.EncounterConditionEntry cS0 = dstS0.condition.entries[0];
        assertEquals(WorldmapEncounters.ENCOUNTER_CONDITION_TYPE_PLAYER, cS0.type);
        assertEquals(4, cS0.value);
    }
}
