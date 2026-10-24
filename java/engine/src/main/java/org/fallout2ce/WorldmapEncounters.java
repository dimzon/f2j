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

    /** Port of {@code EncounterConditionType} (src/worldmap.cc). */
    public static final int ENCOUNTER_CONDITION_TYPE_NONE = 0;
    public static final int ENCOUNTER_CONDITION_TYPE_GLOBAL = 1;
    public static final int ENCOUNTER_CONDITION_TYPE_NUMBER_OF_CRITTERS = 2;
    public static final int ENCOUNTER_CONDITION_TYPE_RANDOM = 3;
    public static final int ENCOUNTER_CONDITION_TYPE_PLAYER = 4;
    public static final int ENCOUNTER_CONDITION_TYPE_DAYS_PLAYED = 5;
    public static final int ENCOUNTER_CONDITION_TYPE_TIME_OF_DAY = 6;

    /** Port of {@code EncounterConditionalOperator} (src/worldmap.cc). */
    public static final int ENCOUNTER_CONDITIONAL_OPERATOR_NONE = 0;
    public static final int ENCOUNTER_CONDITIONAL_OPERATOR_EQUAL = 1;
    public static final int ENCOUNTER_CONDITIONAL_OPERATOR_NOT_EQUAL = 2;
    public static final int ENCOUNTER_CONDITIONAL_OPERATOR_LESS_THAN = 3;
    public static final int ENCOUNTER_CONDITIONAL_OPERATOR_GREATER_THAN = 4;

    /** Port of {@code EncounterLogicalOperator}. */
    public static final int ENCOUNTER_LOGICAL_OPERATOR_NONE = 0;
    public static final int ENCOUNTER_LOGICAL_OPERATOR_AND = 1;
    public static final int ENCOUNTER_LOGICAL_OPERATOR_OR = 2;

    /** Port of {@code EncounterConditionEntry}. */
    public static final class EncounterConditionEntry {
        /** The kind of the condition. */
        public int type;

        /** The comparison of the condition. */
        public int conditionalOperator;

        /** The parameter of the condition (a gvar index, a chance...). */
        public int param;

        /** The value the condition is compared against. */
        public int value;
    }

    /** Port of {@code EncounterCondition}. */
    public static final class EncounterCondition {
        /** How many entries the condition holds. */
        public int entriesLength;

        /** The entries, up to three. */
        public final EncounterConditionEntry[] entries = new EncounterConditionEntry[3];

        /** The logical operators joining the entries. */
        public final int[] logicalOperators = new int[2];
    }

    /** Port of {@code EncounterTableSubEntry}, the scalar slice. */
    public static final class EncounterTableSubEntry {
        /** The least of the encounter, without a range 0. */
        public int minimumCount;

        /** The most of the encounter, without a range 0. */
        public int maximumCount;

        /** The encounter index this sub-entry names. */
        public int encounterIndex;

        /** The name the index waits for, from the base types. */
        public String name = "";

        /** The situation of the encounter. */
        public int situation;
    }

    /** Port of {@code EncounterTableEntry}, the scalar slice. */
    public static final class EncounterTableEntry {
        /** The raw config line, the way the file has it. */
        public String raw = "";

        /** The chance of the entry, read from {@code Chance:}. */
        public int chance;

        /** The counter of the entry, read from {@code Counter:}. */
        public int counter;

        /** Whether the entry is special ({@code special}). */
        public boolean special;

        /** The map the entry is tied to, read from {@code map:}. */
        public WorldmapDefs.Map map = WorldmapDefs.Map.MAP_INVALID;

        /** The scenery of the entry, read from {@code Scenery:}. */
        public int scenery;

        /** How many sub-entries the entry holds. */
        public int subEntriesLength;

        /** The sub-entries, by the order the line names them. */
        public final EncounterTableSubEntry[] subEntries =
            new EncounterTableSubEntry[6];

        /** The condition of the entry. */
        public EncounterCondition condition = new EncounterCondition();
    }

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

        /** The entries, by the order the config names them. */
        public final EncounterTableEntry[] entries =
            new EncounterTableEntry[ENCOUNTER_TABLE_ENTRY_MAX_COUNT];
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
                EncounterTableEntry entry = parseEntry(valuePtr[0]);
                table.entries[table.entriesLength] = entry;
                table.entriesLength++;
            }
        }

        return 0;
    }

    /**
     * Port of the scalar walk of {@code wmParseEncounterTableIndex}:
     * the chance, the counter, the special flag, the map and the
     * scenery of an entry line.
     *
     * <p>The sub-entries and the condition of the line stay for the
     * next block; the raw line is kept so nothing is lost before it.
     */
    static EncounterTableEntry parseEntry(String raw) {
        EncounterTableEntry entry = new EncounterTableEntry();
        entry.raw = raw;

        entry.chance = intAfter(raw, "Chance:");
        entry.counter = intAfter(raw, "Counter:");
        entry.special = raw.contains("special");

        String map = segmentAfter(raw, "map:");
        if (!map.isEmpty()) {
            int mapIdx = lookupMapByName(map.trim().toLowerCase());
            if (mapIdx != WorldmapDefs.Map.MAP_INVALID.id()) {
                entry.map = WorldmapDefs.ofMap(mapIdx);
            }
        }

        String scenery = segmentAfter(raw, "Scenery:");
        if (!scenery.isEmpty()) {
            entry.scenery = sceneryId(scenery.trim().toLowerCase());
        }

        parseSubEntries(entry, raw);

        entry.condition = parseCondition(raw);

        return entry;
    }

    /**
     * Port of {@code wmParseConditional} (0x4BE988): the {@code if(...)}
     * tail of an entry line.
     *
     * <p>The kinds the sub-condition starts with are the random, the
     * global, the player level, the days played, the time of day and the
     * critter count; each is followed by a comparison and a value, and
     * the entries are joined with {@code and}/{@code or}. The kinds
     * without an evaluation (rand) keep the none operator.
     */
    static EncounterCondition parseCondition(String raw) {
        EncounterCondition condition = new EncounterCondition();
        if (raw == null) {
            return condition;
        }
        String s = raw.toLowerCase();
        int ifAt = s.indexOf("if(");
        if (ifAt < 0) {
            return condition;
        }
        s = s.substring(ifAt + 3);

        while (condition.entriesLength < 3 && !s.isEmpty()) {
            EncounterConditionEntry entry = new EncounterConditionEntry();
            s = s.trim();

            if (s.startsWith("rand(")) {
                entry.type = ENCOUNTER_CONDITION_TYPE_RANDOM;
                entry.conditionalOperator = ENCOUNTER_CONDITIONAL_OPERATOR_NONE;
                int close = s.indexOf(')', 5);
                entry.param = Integer.parseInt(s.substring(5, close));
                s = s.substring(close + 1);
            } else {
                int type;
                int prefix;
                if (s.startsWith("global(")) {
                    type = ENCOUNTER_CONDITION_TYPE_GLOBAL;
                    prefix = 7;
                } else if (s.startsWith("player(level)")) {
                    type = ENCOUNTER_CONDITION_TYPE_PLAYER;
                    prefix = 13;
                } else if (s.startsWith("days_played")) {
                    type = ENCOUNTER_CONDITION_TYPE_DAYS_PLAYED;
                    prefix = 11;
                } else if (s.startsWith("time_of_day")) {
                    type = ENCOUNTER_CONDITION_TYPE_TIME_OF_DAY;
                    prefix = 11;
                } else if (s.startsWith("enctr(num_critters)")) {
                    type = ENCOUNTER_CONDITION_TYPE_NUMBER_OF_CRITTERS;
                    prefix = 19;
                } else {
                    break;
                }
                entry.type = type;
                s = s.substring(prefix).trim();

                if (type == ENCOUNTER_CONDITION_TYPE_GLOBAL) {
                    int close = s.indexOf(')');
                    entry.param = Integer.parseInt(s.substring(0, close));
                    s = s.substring(close + 1).trim();
                }

                entry.conditionalOperator = comparisonOperator(s);
                s = s.substring(comparisonLength(s)).trim();
                entry.value = leadingInt(s);
                s = skipValue(s, entry.value);
            }

            condition.entries[condition.entriesLength] = entry;
            condition.entriesLength++;

            if (s.startsWith("and")) {
                condition.logicalOperators[condition.entriesLength - 1] =
                    ENCOUNTER_LOGICAL_OPERATOR_AND;
                s = s.substring(3);
            } else if (s.startsWith("or")) {
                condition.logicalOperators[condition.entriesLength - 1] =
                    ENCOUNTER_LOGICAL_OPERATOR_OR;
                s = s.substring(2);
            } else {
                break;
            }
        }

        return condition;
    }

    /** The comparison operator of a tail, from the operator table. */
    private static int comparisonOperator(String s) {
        s = s.trim();
        if (s.startsWith("==")) {
            return ENCOUNTER_CONDITIONAL_OPERATOR_EQUAL;
        }
        if (s.startsWith("!=")) {
            return ENCOUNTER_CONDITIONAL_OPERATOR_NOT_EQUAL;
        }
        if (s.startsWith("<")) {
            return ENCOUNTER_CONDITIONAL_OPERATOR_LESS_THAN;
        }
        if (s.startsWith(">")) {
            return ENCOUNTER_CONDITIONAL_OPERATOR_GREATER_THAN;
        }
        return ENCOUNTER_CONDITIONAL_OPERATOR_NONE;
    }

    /** How many characters the comparison operator takes. */
    private static int comparisonLength(String s) {
        s = s.trim();
        if (s.startsWith("==") || s.startsWith("!=")) {
            return 2;
        }
        return 1;
    }

    /** The leading integer of a tail, 0 when none. */
    private static int leadingInt(String s) {
        s = s.trim();
        int digits = 0;
        while (digits < s.length() && Character.isDigit(s.charAt(digits))) {
            digits++;
        }
        return digits > 0 ? Integer.parseInt(s.substring(0, digits)) : 0;
    }

    /** Skip past the value and its closing punctuation. */
    private static String skipValue(String s, int value) {
        s = s.trim();
        int digits = 0;
        while (digits < s.length() && Character.isDigit(s.charAt(digits))) {
            digits++;
        }
        s = s.substring(digits).trim();
        while (!s.isEmpty() && (s.charAt(0) == ')' || s.charAt(0) == ',')) {
            s = s.substring(1).trim();
        }
        return s;
    }

    /**
     * Port of {@code wmParseEncounterSubEncStr}: the {@code Enc:}
     * segment of an entry line.
     *
     * <p>Every chunk is {@code (min-max) name SITUATION}. The index the
     * name resolves to waits for the base encounter types, so the name
     * is kept until then; the situation is the enum id.
     */
    private static void parseSubEntries(EncounterTableEntry entry, String raw) {
        int at = raw.indexOf("Enc:");
        if (at < 0) {
            return;
        }
        int from = at + 4;
        int to = raw.indexOf(',', from);
        String chunk = (to < 0 ? raw.substring(from) : raw.substring(from, to)).trim();

        while (!chunk.isEmpty() && entry.subEntriesLength < 6) {
            EncounterTableSubEntry sub = new EncounterTableSubEntry();

            if (chunk.charAt(0) == '(') {
                int close = chunk.indexOf(')');
                String range = chunk.substring(1, close);
                int dash = range.indexOf('-');
                sub.minimumCount = Integer.parseInt(range.substring(0, dash).trim());
                sub.maximumCount = Integer.parseInt(range.substring(dash + 1).trim());
                chunk = chunk.substring(close + 1).trim();
            }

            int space = chunk.indexOf(' ');
            String name = space < 0 ? chunk : chunk.substring(0, space);
            sub.name = name;
            chunk = space < 0 ? "" : chunk.substring(space + 1).trim();

            int sitSpace = chunk.indexOf(' ');
            String situation = sitSpace < 0 ? chunk : chunk.substring(0, sitSpace);
            sub.situation = situationId(situation);
            chunk = sitSpace < 0 ? "" : chunk.substring(sitSpace + 1).trim();

            // The trailing "player" token is the marker of the player
            // encounter index (-1), not another sub-entry.
            if (!chunk.isEmpty()) {
                if (chunk.equalsIgnoreCase("player")) {
                    sub.encounterIndex = -1;
                }
                chunk = "";
            }

            entry.subEntries[entry.subEntriesLength] = sub;
            entry.subEntriesLength++;
        }
    }

    /** Port of the situation name list ({@code wmEncOpStrs}). */
    private static int situationId(String name) {
        switch (name.toLowerCase()) {
        case "nothing":
            return 0;
        case "ambush":
            return 1;
        case "fighting":
            return 2;
        case "and":
            return 3;
        default:
            return 0;
        }
    }

    /** The integer after a key, or 0 when absent. */
    private static int intAfter(String raw, String key) {
        int at = raw.indexOf(key);
        if (at < 0) {
            return 0;
        }
        int from = at + key.length();
        int to = from;
        while (to < raw.length() && Character.isDigit(raw.charAt(to))) {
            to++;
        }
        return to > from ? Integer.parseInt(raw.substring(from, to)) : 0;
    }

    /** The text after a key, up to the next comma or the end. */
    private static String segmentAfter(String raw, String key) {
        int at = raw.indexOf(key);
        if (at < 0) {
            return "";
        }
        int from = at + key.length();
        int to = raw.indexOf(',', from);
        return to < 0 ? raw.substring(from) : raw.substring(from, to);
    }

    /** Port of the scenery name list ({@code wmSceneryStrs}). */
    private static int sceneryId(String name) {
        switch (name) {
        case "none":
            return 0;
        case "light":
            return 1;
        case "normal":
            return 2;
        case "heavy":
            return 3;
        default:
            return 0;
        }
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
