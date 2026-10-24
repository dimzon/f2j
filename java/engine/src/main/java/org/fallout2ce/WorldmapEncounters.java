package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * The encounter tables of the worldmap, mirroring the header slice of
 * {@code wmEncounterTableList} in {@code src/worldmap.cc}: the names,
 * the maps and the entry counts of the {@code Encounter Table N}
 * sections of {@code data\worldmap.txt}.
 *
 * <p>Vectors: {@code [worldmaptiles]} in
 * {@code docker/refharness/golden/v143.txt}, produced by
 * {@code sectionWorldmapTiles} in {@code docker/refharness/ref_main.cc}:
 * {@code encounter-tables=76}, and the headers of the first tables
 * ({@code DstN}: 3 maps, 5 entries; {@code DstS}: 3 maps, 8 entries).
 *
 * <p>The entries themselves - the conditions, the sub-entries and the
 * encounter data they carry - come with the next block of the world.
 */
public final class WorldmapEncounters {

    private WorldmapEncounters() {
    }

    /** EncounterTable_LOOKUP_NAME_SIZE (src/worldmap.cc). */
    static final int ENCOUNTER_TABLE_LOOKUP_NAME_SIZE = 40;

    /** The most maps a table can name. */
    static final int ENCOUNTER_TABLE_MAP_MAX_COUNT = 6;

    /** The most entries a table can hold. */
    static final int ENCOUNTER_TABLE_ENTRY_MAX_COUNT = 40;

    /** Port of {@code EncounterTable}, the header slice. */
    public static final class EncounterTable {
        /** The lookup name. */
        public String lookupName = "";

        /** The index of the table, in load order. */
        public int index;

        /** How many maps the table names. */
        public int mapsLength;

        /** The maps, by the order the config names them. */
        public final WorldmapDefs.Map[] maps =
            new WorldmapDefs.Map[ENCOUNTER_TABLE_MAP_MAX_COUNT];

        /** How many entries the table holds (parsed in the next block). */
        public int entriesLength;
    }

    // 0x51DDE4-adjacent wmEncounterTableList.
    private static final List<EncounterTable> encounterTableList = new ArrayList<>();

    // wmMaxEncounterInfoTables.
    private static int maxEncounterInfoTables;

    /**
     * Port of the header walk of {@code wmReadEncounterType}: read the
     * {@code Encounter Table N} sections until one comes without a
     * lookup name, keeping the header fields of each.
     *
     * <p>The map names resolve through the map table's lookup name, the
     * same match the original's {@code wmParseFindMapIdxMatch} makes.
     *
     * @return 0 when the tables were counted
     */
    public static int wmEncounterTablesInit() {
        if (!Config.configRead(GameConfig.gameConfig, "data\\worldmap.txt", true)) {
            return -1;
        }
        return wmParseEncounterTables(GameConfig.gameConfig);
    }

    /** Port of the header walk, on an open config. */
    static int wmParseEncounterTables(Config.Cfg cfg) {
        encounterTableList.clear();
        maxEncounterInfoTables = 0;

        String[] valuePtr = new String[1];
        for (int index = 0;; index++) {
            String section = "Encounter Table " + index;
            if (!Config.configGetString(cfg, section, "lookup_name", valuePtr)) {
                break;
            }

            EncounterTable table = new EncounterTable();
            table.index = index;
            table.lookupName = valuePtr[0];
            if (table.lookupName.length() > ENCOUNTER_TABLE_LOOKUP_NAME_SIZE) {
                table.lookupName = table.lookupName.substring(0, ENCOUNTER_TABLE_LOOKUP_NAME_SIZE);
            }
            encounterTableList.add(table);
            maxEncounterInfoTables++;

            if (Config.configGetString(cfg, section, "maps", valuePtr)) {
                String[] maps = valuePtr[0].split(",");
                for (String part : maps) {
                    if (table.mapsLength >= ENCOUNTER_TABLE_MAP_MAX_COUNT) {
                        break;
                    }
                    int mapIdx = lookupMapByName(part.trim());
                    if (mapIdx == WorldmapDefs.Map.MAP_INVALID.id()) {
                        break;
                    }
                    table.maps[table.mapsLength] = WorldmapDefs.ofMap(mapIdx);
                    table.mapsLength++;
                }
            }

            while (table.entriesLength < ENCOUNTER_TABLE_ENTRY_MAX_COUNT) {
                String key = String.format("enc_%02d", table.entriesLength);
                if (!Config.configGetString(cfg, section, key, valuePtr)) {
                    break;
                }
                table.entriesLength++;
            }
        }

        return 0;
    }

    /** The map table index of a map's lookup name, or MAP_INVALID. */
    private static int lookupMapByName(String name) {
        for (int index = WorldmapDefs.MAP_FIRST; index < MapRegistry.wmMapMaxCount(); index++) {
            if (MapRegistry.lookupName(index).equalsIgnoreCase(name)) {
                return index;
            }
        }
        return WorldmapDefs.Map.MAP_INVALID.id();
    }

    /** How many encounter tables the world has. */
    public static int wmMaxEncounterInfoTables() {
        return maxEncounterInfoTables;
    }

    /** The table at an index, for the tests of this slice. */
    static EncounterTable tableAt(int index) {
        return encounterTableList.get(index);
    }

    /** Test seam: fresh-process mirror. */
    static void resetForTests() {
        encounterTableList.clear();
        maxEncounterInfoTables = 0;
    }
}
