package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [mapnames]} vectors from
 * {@code docker/refharness/golden/v107.txt}, produced by
 * {@code sectionMapNames} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The three lookups all read {@code game\map.msg}: {@code mapGetName}
 * indexes {@code 200 + map * 3 + elevation}, {@code mapGetCityName} resolves
 * the map's area and reads {@code 1500 + city}, and {@code mapDescriptionById}
 * goes through {@code wmGetAreaIdxName} on the same id.
 *
 * <p>Empty results are kept rather than skipped: a map with only elevation 0
 * named yields empty strings for 1 and 2, which is what the message file
 * actually contains and what the lookup returns.
 */
class MapNamesTest {

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

        savedLanguage = Settings.settings.system.language;
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        Settings.settings.system.language = Message.ENGLISH;
    }

    @BeforeEach
    void loadMessages() {
        // The city/description lookups resolve through the area table, which
        // in turn needs the map table, so both come up first.
        assertEquals(0, MapAreas.wmAreaInit());
        assertTrue(Map.mapInitMessageList());
    }

    @Test
    void mapNamesByElevation() {
        assertEquals("Desert", Map.mapGetName(0, 0));
        assertEquals("", Map.mapGetName(0, 1));
        assertEquals("", Map.mapGetName(0, 2));

        assertEquals("Temple: Foyer", Map.mapGetName(3, 0));
        assertEquals("Temple: Hallway", Map.mapGetName(3, 1));
        assertEquals("Temple: Inner Sanctum", Map.mapGetName(3, 2));

        assertEquals("Underground Lake", Map.mapGetName(26, 0));
        assertEquals("", Map.mapGetName(26, 1));
        assertEquals("", Map.mapGetName(26, 2));

        // Out-of-range ids return null rather than a lookup.
        assertNull(Map.mapGetName(-1, 0));
        assertNull(Map.mapGetName(0, 3));
    }

    @Test
    void cityNamesResolveThroughArea() {
        assertEquals("Desert", Map.mapGetCityName(0));
        assertEquals("Arroyo", Map.mapGetCityName(3));
        assertEquals("Ghost Farm", Map.mapGetCityName(26));
    }

    @Test
    void descriptionsGoThroughAreaName() {
        assertEquals("Desert", Map.mapDescriptionById(0));
        assertEquals("Ghost Farm", Map.mapDescriptionById(26));
    }

    @Test
    void areaNameLookupMatchesDescription() {
        // mapDescriptionById routes through wmGetAreaIdxName; both read
        // message 1500 + city, so they must agree for the same map.
        MapAreas.City[] city = new MapAreas.City[1];
        assertEquals(0, MapAreas.wmMatchAreaContainingMapIdx(0, city));
        assertEquals(Map.mapDescriptionById(0), MapAreas.wmGetAreaIdxName(city[0]));
    }
}
