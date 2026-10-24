package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The base-encounter vectors {@code [worldmaptiles]} pins in golden/v145:
 * the Raiders encounter with its first type lines, 6 types, team 45 and
 * the surrounding formation.
 */
class WorldmapEncBaseTest {

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
        WorldmapEncBase.resetForTests();

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
    void raidersBaseVectors() {
        assertEquals(0, MapRegistry.wmMapInit());
        assertEquals(0, WorldmapTiles.wmTilesInit());

        int raiders = WorldmapEncBase.wmReadEncBaseType("Raiders");
        assertTrue(raiders >= 0);

        WorldmapEncBase.Encounter encounter = WorldmapEncBase.encounterAt(raiders);
        assertEquals(6, encounter.entriesLength);

        WorldmapEncBase.EncounterEntry e0 = encounter.entries[0];
        assertEquals(35, e0.ratio);
        assertEquals(WorldmapEncBase.ENCOUNTER_RATIO_MODE_USE_RATIO, e0.ratioMode);
        assertEquals(16777252, e0.pid);
        assertEquals(255, e0.scriptIdx);
        assertEquals(2, e0.itemsLength);

        WorldmapEncBase.EncounterItem i0 = e0.items[0];
        assertEquals(8, i0.pid);
        assertEquals(true, i0.isEquipped);

        WorldmapEncBase.EncounterItem i1 = e0.items[1];
        assertEquals(40, i1.pid);
        assertEquals(false, i1.isEquipped);

        WorldmapEncBase.EncounterEntry e1 = encounter.entries[1];
        assertEquals(45, e1.ratio);
        assertEquals(16777249, e1.pid);
        assertEquals(1, e1.itemsLength);
        assertEquals(7, e1.items[0].pid);

        // The team applies to critter entries.
        assertEquals(45, e0.team);
        assertEquals(45, e1.team);

        // Surrounding, Spacing:5, Distance:Player(Perception).
        assertEquals(WorldmapEncBase.ENCOUNTER_FORMATION_TYPE_SURROUNDING, encounter.position);
        assertEquals(5, encounter.spacing);
        assertEquals("Player(Perception)", encounter.distance);
    }
}
