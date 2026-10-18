package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [mapregistry]} vectors from
 * {@code docker/refharness/golden/v105.txt}, produced by
 * {@code sectionMapRegistry} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The section brings up the game config and then queries the worldmap map
 * registry that {@code wmMapInit} fills from {@code data\MAPS.TXT} in the
 * game database. It pins the entry count, {@code mapIsValid} on both edges,
 * the {@code wmMapIdxToName} format and its two out-of-range results,
 * {@code wmMapMatchNameToIdx} across hits/misses/case/extension, the
 * saveable predicate, and a name round-trip proving the lookup directions
 * agree.
 *
 * <p>Isolation: the registry is static state, so setup tears it down along
 * with the config and VFS globals this slice touches, keeping the rest of
 * the suite (notably {@link GameConfigMigrationTest}, which also reads
 * {@code gGameConfig}) unaffected.
 */
class MapRegistryTest {

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

        savedLanguage = Settings.settings.system.language;
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        Settings.settings.system.language = Message.ENGLISH;
    }

    private void loadRegistry() {
        assertEquals(0, MapRegistry.wmMapInit());
    }

    @Test
    void registryCountsAndNames() {
        loadRegistry();

        int maxCount = MapRegistry.wmMapMaxCount();
        assertEquals(151, maxCount);

        assertEquals("valid-min=1 valid-max=1 valid-past=0",
                "valid-min=" + (MapRegistry.mapIsValid(0) ? 1 : 0)
                        + " valid-max=" + (MapRegistry.mapIsValid(maxCount - 1) ? 1 : 0)
                        + " valid-past=" + (MapRegistry.mapIsValid(maxCount) ? 1 : 0));

        assertEquals("desert1.MAP", MapRegistry.wmMapIdxToName(0));
        assertEquals("desert2.MAP", MapRegistry.wmMapIdxToName(1));
        assertEquals("desert3.MAP", MapRegistry.wmMapIdxToName(2));

        // Out of range on both edges: rc -1 and an emptied buffer.
        byte[] buf = new byte[64];
        java.util.Arrays.fill(buf, (byte) 'X');
        assertEquals(-1, MapRegistry.wmMapIdxToName(-1, buf, buf.length));
        assertEquals(0, buf[0]);

        java.util.Arrays.fill(buf, (byte) 'X');
        assertEquals(-1, MapRegistry.wmMapIdxToName(maxCount, buf, buf.length));
        assertEquals(0, buf[0]);
    }

    @Test
    void nameLookupAndSaveable() {
        loadRegistry();

        assertEquals(-1, MapRegistry.wmMapMatchNameToIdx("ARROYO.MAP"));
        assertEquals(-1, MapRegistry.wmMapMatchNameToIdx("ARROYO"));
        assertEquals(-1, MapRegistry.wmMapMatchNameToIdx("NOSUCHMAP.MAP"));
        assertEquals(-1, MapRegistry.wmMapMatchNameToIdx("arroyo.map"));

        assertFalse(MapRegistry.wmMapIdxIsSaveable(0));
        assertFalse(MapRegistry.wmMapIdxIsSaveable(1));
        assertFalse(MapRegistry.wmMapIdxIsSaveable(2));

        // Round-trip: index -> name -> same index, lowercased as the table stores.
        assertEquals(0, MapRegistry.wmMapMatchNameToIdx("desert1.map"));
        assertEquals(1, MapRegistry.wmMapMatchNameToIdx("desert2.map"));
        assertEquals(2, MapRegistry.wmMapMatchNameToIdx("desert3.map"));
    }

    @Test
    void registryIsEmptyBeforeInit() {
        assertEquals(0, MapRegistry.wmMapMaxCount());
        assertFalse(MapRegistry.mapIsValid(0));
    }
}
