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
 * {@code [automap]} vectors from
 * {@code docker/refharness/golden/v113.txt}, produced by
 * {@code sectionAutomap} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Pins the parse of the real {@code MAPS\AUTOMAP.DB}: version 1, data
 * size 1925, and a {@code [160][3]} offset table with only 9 populated
 * entries. The value those entries hold is {@code -1}, not the {@code 0}
 * the header comment describes — worth pinning, because "0 means absent"
 * would make the populated count come out wrong.
 */
class AutomapTest {

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
        XFile.resetForTests();
        Automap.resetForTests();

        Settings.settings.system.language = Message.ENGLISH;
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        XFile.xbaseOpen(gameEn + "/master.dat");
        // AUTOMAP.DB ships as a loose file under data\, so the relative
        // "MAPS\AUTOMAP.DB" only resolves with data\ mounted as an xbase.
        XFile.xbaseOpen(gameEn + "/data");
    }

    @Test
    void headerParsesFromRealDatabase() {
        Automap.AutomapHeader[] out = new Automap.AutomapHeader[1];
        assertEquals(0, Automap.automapGetHeader(out));

        Automap.AutomapHeader header = out[0];
        assertEquals(1, header.version);
        assertEquals(1925, header.dataSize);

        int nonZero = 0;
        for (int map = 0; map < Automap.AUTOMAP_MAP_COUNT; map++) {
            for (int elevation = 0; elevation < 3; elevation++) {
                if (header.offsets[map][elevation] != 0) {
                    nonZero++;
                }
            }
        }
        assertEquals(9, nonZero);
    }

    @Test
    void populatedEntriesHoldMinusOne() {
        Automap.AutomapHeader[] out = new Automap.AutomapHeader[1];
        assertEquals(0, Automap.automapGetHeader(out));

        // The populated entries carry -1 (a backward seek), so "0 means
        // absent" would both miscount and lose the value.
        assertEquals(-1, out[0].offsets[0][0]);
        assertEquals(-1, out[0].offsets[0][1]);
        assertEquals(-1, out[0].offsets[0][2]);
        assertEquals(-1, out[0].offsets[1][0]);
    }

    @Test
    void entryValidationGatesBothIds() {
        assertTrue(Automap.automapEntryIsValid(0, 0));
        assertTrue(Automap.automapEntryIsValid(159, 2));
        assertFalse(Automap.automapEntryIsValid(160, 0));
        assertFalse(Automap.automapEntryIsValid(0, 3));
        assertFalse(Automap.automapEntryIsValid(-1, 0));
    }

    @Test
    void displayMapOnlyMovesWhenAvailable() {
        Automap.automapSetDisplayMap(3, true);
        // An unavailable map must not displace the current one.
        Automap.automapSetDisplayMap(42, false);
        assertEquals(-1, Automap.automapGetWindow());
    }
}