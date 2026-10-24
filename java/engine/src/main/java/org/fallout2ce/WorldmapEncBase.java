package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * The base encounters of the worldmap, mirroring {@code wmEncBaseTypeList}
 * in {@code src/worldmap.cc}: the {@code Encounter: <name>} sections of
 * {@code data\worldmap.txt}, their type lines, their team and their
 * formation block.
 *
 * <p>Vectors: {@code [worldmaptiles]} in
 * {@code docker/refharness/golden/v145.txt}, produced by
 * {@code sectionWorldmapTiles} in {@code docker/refharness/ref_main.cc}:
 * the first base encounter (Raiders) with its first type lines
 * ({@code Ratio:35%, pid:16777252, Item:8{Wielded}, Item:40, Script:255}),
 * 6 types, team 45 and the surrounding formation.
 *
 * <p>NOTES / seams:
 * <ul>
 * <li>The conditions ({@code if(...)}) of the type lines are kept raw;
 * their parse comes with the condition block of the world.</li>
 * <li>The formation's distance keeps its raw expression (for example
 * {@code Player(Perception)}); evaluating it waits for the movement
 * block.</li>
 * </ul>
 */
public final class WorldmapEncBase {

    private WorldmapEncBase() {
    }

    /** ENCOUNTER_ENTRY_MAX_COUNT (src/worldmap.cc). */
    static final int ENCOUNTER_ENTRY_MAX_COUNT = 10;

    /** ENCOUNTER_ITEM_MAX_COUNT. */
    static final int ENCOUNTER_ITEM_MAX_COUNT = 10;

    /** Port of {@code EncounterRatioMode}. */
    static final int ENCOUNTER_RATIO_MODE_USE_RATIO = 0;
    static final int ENCOUNTER_RATIO_MODE_SINGLE = 1;

    /** Port of {@code EncounterFormationType} (wmFormationStrs). */
    static final int ENCOUNTER_FORMATION_TYPE_SURROUNDING = 0;

    /** Port of {@code EncounterItem}. */
    public static final class EncounterItem {
        /** The least of the item, without a range 1. */
        public int minimumQuantity = 1;

        /** The most of the item, without a range 1. */
        public int maximumQuantity = 1;

        /** The proto of the item. */
        public int pid;

        /** Whether the item is wielded/worn. */
        public boolean isEquipped;
    }

    /** Port of {@code EncounterEntry}, the scalar slice. */
    public static final class EncounterEntry {
        /** The raw type line. */
        public String raw = "";

        /** The ratio mode, use-ratio when a Ratio: key was read. */
        public int ratioMode = ENCOUNTER_RATIO_MODE_SINGLE;

        /** The ratio, 100 by default. */
        public int ratio = 100;

        /** Whether the entry is a dead one. */
        public boolean dead;

        /** The proto of the encounter, empty until read. */
        public int pid = -1;

        /** The distance of the encounter, 0 by default. */
        public int distance;

        /** The tile of the encounter, -1 by default. */
        public int tile = -1;

        /** The script of the encounter, -1 by default. */
        public int scriptIdx = -1;

        /** The team, -1 until the section's team applies. */
        public int team = -1;

        /** How many items the entry carries. */
        public int itemsLength;

        /** The items, by the order the line names them. */
        public final EncounterItem[] items = new EncounterItem[ENCOUNTER_ITEM_MAX_COUNT];
    }

    /** Port of {@code Encounter}, the scalar slice. */
    public static final class Encounter {
        /** The name, the section's own. */
        public String name = "";

        /** The formation, surrounding by default. */
        public int position = ENCOUNTER_FORMATION_TYPE_SURROUNDING;

        /** The spacing of the formation, 1 by default. */
        public int spacing = 1;

        /** The raw distance expression of the formation. */
        public String distance = "";

        /** How many entries the encounter holds. */
        public int entriesLength;

        /** The entries, by the order the config names them. */
        public final EncounterEntry[] entries = new EncounterEntry[ENCOUNTER_ENTRY_MAX_COUNT];
    }

    // wmEncBaseTypeList.
    private static final List<Encounter> encBaseTypeList = new ArrayList<>();

    // wmMaxEncBaseTypes.
    private static int maxEncBaseTypes;

    /**
     * Port of {@code wmReadEncBaseType} (0x4BDF34): read the base
     * encounter of a name.
     *
     * @return the index of the encounter, or -1 when its section was not
     * there
     */
    public static int wmReadEncBaseType(String name) {
        if (!Config.configRead(GameConfig.gameConfig, "data\\worldmap.txt", true)) {
            return -1;
        }
        return wmReadEncBaseType(GameConfig.gameConfig, name);
    }

    /** Port of {@code wmReadEncBaseType}, on an open config. */
    static int wmReadEncBaseType(Config.Cfg cfg, String name) {
        String section = "Encounter: " + name;

        String[] valuePtr = new String[1];
        if (!Config.configGetString(cfg, section, "type_00", valuePtr)) {
            return -1;
        }

        Encounter encounter = new Encounter();
        encounter.name = name;
        encBaseTypeList.add(encounter);
        maxEncBaseTypes++;

        for (;;) {
            EncounterEntry entry = parseTypeLine(valuePtr[0]);
            encounter.entries[encounter.entriesLength] = entry;
            encounter.entriesLength++;

            String key = String.format("type_%02d", encounter.entriesLength);
            if (!Config.configGetString(cfg, section, key, valuePtr)) {
                break;
            }
        }

        int[] intPtr = new int[1];
        if (Config.configGetInt(cfg, section, "team_num", intPtr)) {
            for (int i = 0; i < encounter.entriesLength; i++) {
                if (ProtoTypes.ProtoId.ofPid(encounter.entries[i].pid).objectType()
                        == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
                    encounter.entries[i].team = intPtr[0];
                }
            }
        }

        if (Config.configGetString(cfg, section, "position", valuePtr)) {
            String position = valuePtr[0];
            int comma = position.indexOf(',');
            String formation = (comma < 0 ? position : position.substring(0, comma)).trim();
            encounter.position = formationId(formation);
            encounter.spacing = intAfter(position, "Spacing:");
            encounter.distance = segmentAfter(position, "Distance:").trim();
        }

        return maxEncBaseTypes - 1;
    }

    /** Port of {@code wmParseEncBaseSubTypeStr} (0x4BE140), scalar slice. */
    private static EncounterEntry parseTypeLine(String raw) {
        EncounterEntry entry = new EncounterEntry();
        entry.raw = raw;

        // The original lowercases the line before reading its keys.
        String s = raw.toLowerCase();

        int ratio = intAfter(s, "ratio:");
        if (ratio != 0) {
            entry.ratioMode = ENCOUNTER_RATIO_MODE_USE_RATIO;
            entry.ratio = ratio;
        }

        if (s.startsWith("dead,")) {
            entry.dead = true;
            s = s.substring(5);
        }

        entry.pid = intAfter(s, "pid:");
        entry.distance = intAfter(s, "distance:");
        entry.tile = intAfter(s, "tilenum:");
        entry.scriptIdx = intAfter(s, "script:");

        int at = 0;
        for (int i = 0; i < ENCOUNTER_ITEM_MAX_COUNT; i++) {
            int itemAt = s.indexOf("item:", at);
            if (itemAt < 0) {
                break;
            }
            int from = itemAt + 5;
            int to = s.indexOf(',', from);
            String itemText = (to < 0 ? s.substring(from) : s.substring(from, to)).trim();
            if (itemText.isEmpty()) {
                break;
            }
            entry.items[entry.itemsLength] = parseItem(itemText);
            entry.itemsLength++;
            at = to < 0 ? s.length() : to + 1;
        }

        return entry;
    }

    /** Port of {@code wmParseItemType} (0x4BE888). */
    private static EncounterItem parseItem(String text) {
        EncounterItem item = new EncounterItem();

        String s = text.trim();
        if (s.startsWith("(")) {
            int close = s.indexOf(')');
            String range = s.substring(1, close);
            int dash = range.indexOf('-');
            if (dash >= 0) {
                item.minimumQuantity = Integer.parseInt(range.substring(0, dash).trim());
                item.maximumQuantity = Integer.parseInt(range.substring(dash + 1).trim());
            } else {
                item.minimumQuantity = Integer.parseInt(range.trim());
                item.maximumQuantity = item.minimumQuantity;
            }
            s = s.substring(close + 1).trim();
        }

        int digits = 0;
        while (digits < s.length() && Character.isDigit(s.charAt(digits))) {
            digits++;
        }
        item.pid = digits > 0 ? Integer.parseInt(s.substring(0, digits)) : -1;
        s = s.substring(digits).toLowerCase();

        if (s.contains("wielded") || s.contains("worn")) {
            item.isEquipped = true;
        }

        return item;
    }

    /** Port of the formation name list ({@code wmFormationStrs}). */
    private static int formationId(String name) {
        switch (name.toLowerCase()) {
        case "surrounding":
            return 0;
        case "line":
            return 1;
        case "formation":
            return 2;
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

    /** How many base encounters the world has. */
    public static int wmMaxEncBaseTypes() {
        return maxEncBaseTypes;
    }

    /** The encounter at an index, for the tests of this slice. */
    static Encounter encounterAt(int index) {
        return encBaseTypeList.get(index);
    }

    /** Test seam: fresh-process mirror. */
    static void resetForTests() {
        encBaseTypeList.clear();
        maxEncBaseTypes = 0;
    }
}
