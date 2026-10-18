package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * Worldmap area (city) table, mirroring {@code src/worldmap.cc}: the
 * {@code wmAreaInfoList}/{@code wmMaxAreaNum} pair, the per-area entrance
 * list and the lookups built on them.
 *
 * <p>Vectors: the {@code [mapareas]} section in
 * {@code docker/refharness/golden/v106.txt} pins the entry count (49 for
 * this build), {@code cityIsValid} on both edges, and — through the four
 * public lookups — that the entrance table parsed far enough to resolve
 * maps to areas: map 0 lands in area 26, map 3 in area 0 and map 150 in
 * area 47. {@code mapAreSameArea} returning false for a map pair that does
 * resolve to one area also pins its {@code wmMapIdxIsSaveable} guard.
 *
 * <p>The table lives in {@code data\CITY.TXT} inside the game database and
 * is read through {@link Config#configRead}, the same route
 * {@link MapRegistry} uses for {@code data\MAPS.TXT}; the area parse
 * therefore runs after the map parse because entrances name their map by
 * {@code lookup_name}.
 *
 * <p>NOTES / seams:
 * <li>{@code mapFid}/{@code labelFid} come from {@code townmap_art_idx} /
 * {@code townmap_label_art_idx}, which C reads with
 * {@code configGetEnum} — that is {@code configGetInt} plus a cast, not a
 * name lookup, so both are plain ints here.</li>
 * <li>{@code x}/{@code y}/{@code size}/{@code state}/{@code lockState} are
 * parsed because the port of {@code wmParseAreasConfig} follows the C
 * control flow; only the entrance {@code map} field is read by this
 * slice's lookups, the rest stay observable state.</li>
 * <li>{@code visitedState} is slot-init only here: writing it back is
 * worldmap save state, which is not ported.</li>
 */
public final class MapAreas {

    private MapAreas() {
    }

    /** CITY_NAME_SIZE. */
    static final int CITY_NAME_SIZE = 40;

    /** ENTRANCE_LIST_CAPACITY. */
    static final int ENTRANCE_LIST_CAPACITY = 10;

    /** Port of {@code City} ({@code src/worldmap.h}). */
    public enum City {
        CITY_INVALID(-1),
        CITY_ARROYO(0),
        CITY_DEN(1),
        CITY_KLAMATH(2),
        CITY_MODOC(3),
        CITY_VAULT_CITY(4),
        CITY_GECKO(5),
        CITY_BROKEN_HILLS(6),
        CITY_NEW_RENO(7),
        CITY_SIERRA_ARMY_BASE(8),
        CITY_VAULT_15(9),
        CITY_NEW_CALIFORNIA_REPUBLIC(10),
        CITY_VAULT_13(11),
        CITY_MILITARY_BASE(12),
        CITY_REDDING(13),
        CITY_SAN_FRANCISCO(14),
        CITY_NAVARRO(15),
        CITY_ENCLAVE(16),
        CITY_ABBEY(17),
        CITY_PRIMITIVE_TRIBE(18),
        CITY_ENV_PROTECTION_AGENCY(19),
        CITY_MODOC_GHOST_TOWN(20),
        CITY_CAR_OUT_OF_GAS(21),
        CITY_DESTROYED_ARROYO(22),
        CITY_KLAMATH_TOXIC_CAVES(23),
        CITY_DEN_SLAVE_RUN(24),
        CITY_RAIDERS(25),
        CITY_RANDOM_ENCOUNTER_DESERT(26),
        CITY_RANDOM_ENCOUNTER_MOUNTAIN(27),
        CITY_RANDOM_ENCOUNTER_CITY(28),
        CITY_RANDOM_ENCOUNTER_COAST(29),
        CITY_GOLGOTHA(30),
        CITY_SPECIAL_ENCOUNTER_WHALE(31),
        CITY_SPECIAL_ENCOUNTER_TIN_WOODSMAN(32),
        CITY_SPECIAL_ENCOUNTER_BIG_HEAD(33),
        CITY_SPECIAL_ENCOUNTER_FEDERATION_SHUTTLE(34),
        CITY_SPECIAL_ENCOUNTER_UNWASHED_VILLAGERS(35),
        CITY_SPECIAL_ENCOUNTER_MONTY_PYTHON_BRIDGE(36),
        CITY_SPECIAL_ENCOUNTER_CAFE_OF_BROKEN_DREAMS(37),
        CITY_SPECIAL_ENCOUNTER_HOLY_HAND_GRANADE_I(38),
        CITY_SPECIAL_ENCOUNTER_HOLY_HAND_GRANADE_II(39),
        CITY_SPECIAL_ENCOUNTER_GUARDIAN_OF_FOREVER(40),
        CITY_SPECIAL_ENCOUNTER_TOXIC_WASTE_DUMP(41),
        CITY_SPECIAL_ENCOUNTER_PARIAHS(42),
        CITY_SPECIAL_ENCOUNTER_MAD_COWS(43),
        CITY_CARAVAN_ENCOUNTERS(44),
        CITY_FAKE_VAULT_13_A(45),
        CITY_FAKE_VAULT_13_B(46),
        CITY_SHADOW_WORLDS(47),
        CITY_RENO_STABLES(48);

        public static final int CITY_FIRST = CITY_ARROYO.id();

        private final int id;

        City(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    /**
     * City by numeric id.
     *
     * <p>NOTE: {@code City.values()} starts at {@code CITY_INVALID}, so its
     * ordinals are shifted by one relative to the ids C uses as array
     * positions. Indexing with it silently returns the neighbouring city.
     */
    static City cityOf(int id) {
        for (City city : City.values()) {
            if (city.id() == id) {
                return city;
            }
        }
        return null;
    }

    /** Port of {@code CityState}. */
    public enum CityState {
        CITY_STATE_UNKNOWN,
        CITY_STATE_KNOWN
    }

    /** Port of {@code CitySize}. */
    public enum CitySize {
        CITY_SIZE_SMALL,
        CITY_SIZE_MEDIUM,
        CITY_SIZE_LARGE,
        CITY_SIZE_COUNT;

        public static final int CITY_SIZE_FIRST = CITY_SIZE_SMALL.ordinal();
    }

    /** Port of {@code LockState}. */
    public enum LockState {
        LOCK_STATE_UNLOCKED,
        LOCK_STATE_LOCKED,
        LOCK_STATE_COUNT
    }

    /** Port of {@code wmStateStrs} ({@code off}/{@code on}). */
    static final String[] WM_STATE_STRS = { "off", "on" };

    /** Port of {@code wmAreaSizeStrs}. */
    static final String[] WM_AREA_SIZE_STRS = { "small", "medium", "large" };

    /** Port of {@code EntranceInfo} ({@code src/worldmap.cc}). */
    static final class EntranceInfo {
        int state;
        int x;
        int y;
        int map;
        int elevation;
        int tile;
        Rotation rotation;
    }

    /** Port of {@code CityInfo} ({@code src/worldmap.cc}). */
    static final class CityInfo {
        String name = "";
        City areaId;
        int x;
        int y;
        CitySize size;
        CityState state;
        LockState lockState;
        Object visitedState;
        int mapFid;
        int labelFid;
        int entrancesLength;
        final EntranceInfo[] entrances = new EntranceInfo[ENTRANCE_LIST_CAPACITY];
    }

    // 0x51DDF8 wmAreaInfoList.
    private static final List<CityInfo> areaInfoList = new ArrayList<>();

    // 0x51DDFC wmMaxAreaNum.
    private static int maxAreaNum;

    /** Port of {@code wmEntranceSlotInit}. */
    private static EntranceInfo entranceSlotInit() {
        EntranceInfo entrance = new EntranceInfo();
        entrance.map = WorldmapDefs.Map.MAP_INVALID.ordinal();
        entrance.rotation = Rotation.NE;
        return entrance;
    }

    /** Port of {@code wmAreaSlotInit}. */
    private static CityInfo areaSlotInit() {
        CityInfo area = new CityInfo();
        area.areaId = City.CITY_INVALID;
        area.size = CitySize.CITY_SIZE_LARGE;
        area.state = CityState.CITY_STATE_UNKNOWN;
        area.lockState = LockState.LOCK_STATE_UNLOCKED;
        area.mapFid = -1;
        area.labelFid = -1;
        area.entrancesLength = 0;
        return area;
    }

    /**
     * Port of {@code wmParseFindMapIdxMatch}: entrances name their map by
     * {@code lookup_name}, case-insensitively.
     */
    private static int wmParseFindMapIdxMatch(String string, int[] valuePtr) {
        for (int index = WorldmapDefs.MAP_FIRST; index < MapRegistry.wmMapMaxCount(); index++) {
            if (string.equalsIgnoreCase(MapRegistry.lookupName(index))) {
                valuePtr[0] = index;
                return 0;
            }
        }

        Debug.print("\nWorldMap Error: Couldn't find match for Map Index!");
        valuePtr[0] = WorldmapDefs.Map.MAP_INVALID.ordinal();
        return -1;
    }

    /** Port of {@code wmParseAreasConfig}. */
    static int wmParseAreasConfig(Config.Cfg cfg, boolean reindex) {
        if (cfg == null) {
            return -1;
        }

        int areaIdx = reindex ? City.CITY_ARROYO.id() : maxAreaNum;
        String[] valuePtr = new String[1];
        int[] num = new int[1];
        int loopSafetyCounter = 0;

        while (loopSafetyCounter < 5000) {
            String section = String.format("Area %02d", areaIdx);

            // configGetEnum is configGetInt plus a cast in C.
            num[0] = 0;
            if (!Config.configGetInt(cfg, section, "townmap_art_idx", num)) {
                break;
            }

            maxAreaNum++;

            CityInfo city = areaSlotInit();
            city.areaId = cityOf(maxAreaNum - 1);
            city.mapFid = num[0];

            if (Config.configGetInt(cfg, section, "townmap_label_art_idx", num) && num[0] != -1) {
                city.labelFid = num[0];
            }

            if (!Config.configGetString(cfg, section, "area_name", valuePtr)) {
                return -1;
            }
            city.name = valuePtr[0].length() <= CITY_NAME_SIZE ? valuePtr[0]
                    : valuePtr[0].substring(0, CITY_NAME_SIZE);

            if (!Config.configGetString(cfg, section, "world_pos", valuePtr)) {
                return -1;
            }
            StringParsers.Cursor cursor = new StringParsers.Cursor(valuePtr[0]);
            if (StringParsers.strParseInt(cursor, num) == -1) {
                return -1;
            }
            city.x = num[0];
            if (StringParsers.strParseInt(cursor, num) == -1) {
                return -1;
            }
            city.y = num[0];

            if (!Config.configGetString(cfg, section, "start_state", valuePtr)) {
                return -1;
            }
            cursor = new StringParsers.Cursor(valuePtr[0]);
            if (StringParsers.strParseStrFromList(cursor, num, WM_STATE_STRS) == -1) {
                return -1;
            }
            city.state = CityState.values()[num[0]];

            if (Config.configGetString(cfg, section, "lock_state", valuePtr)) {
                cursor = new StringParsers.Cursor(valuePtr[0]);
                if (StringParsers.strParseStrFromList(cursor, num, WM_STATE_STRS) == -1) {
                    return -1;
                }
                city.lockState = LockState.values()[num[0]];
            }

            if (!Config.configGetString(cfg, section, "size", valuePtr)) {
                return -1;
            }
            cursor = new StringParsers.Cursor(valuePtr[0]);
            if (StringParsers.strParseStrFromList(cursor, num, WM_AREA_SIZE_STRS) == -1) {
                return -1;
            }
            city.size = CitySize.values()[num[0]];

            while (city.entrancesLength < ENTRANCE_LIST_CAPACITY) {
                String key = "entrance_" + city.entrancesLength;
                if (!Config.configGetString(cfg, section, key, valuePtr)) {
                    break;
                }

                EntranceInfo entrance = entranceSlotInit();
                city.entrances[city.entrancesLength] = entrance;
                cursor = new StringParsers.Cursor(valuePtr[0]);

                if (StringParsers.strParseStrFromList(cursor, num, WM_STATE_STRS) == -1) {
                    return -1;
                }
                entrance.state = num[0];

                if (StringParsers.strParseInt(cursor, num) == -1) {
                    return -1;
                }
                entrance.x = num[0];

                if (StringParsers.strParseInt(cursor, num) == -1) {
                    return -1;
                }
                entrance.y = num[0];

                if (StringParsers.strParseStrFromFunc(cursor, num, MapAreas::wmParseFindMapIdxMatch) == -1) {
                    return -1;
                }
                entrance.map = num[0];

                if (StringParsers.strParseInt(cursor, num) == -1) {
                    return -1;
                }
                entrance.elevation = num[0];

                if (StringParsers.strParseInt(cursor, num) == -1) {
                    return -1;
                }
                entrance.tile = num[0];

                // strParseEnum is strParseInt plus a cast in C.
                if (StringParsers.strParseInt(cursor, num) == -1) {
                    return -1;
                }
                entrance.rotation = Rotation.fromInt(num[0]);

                city.entrancesLength++;
            }

            areaInfoList.add(city);
            areaIdx++;
            loopSafetyCounter++;
        }

        return 0;
    }

    /** Port of {@code wmAreaInit}: maps first, then the area table. */
    static int wmAreaInit() {
        if (MapRegistry.wmMapInit() == -1) {
            return -1;
        }

        if (Config.configRead(GameConfig.gameConfig, "data\\city.txt", true)) {
            if (wmParseAreasConfig(GameConfig.gameConfig, false) == -1) {
                return -1;
            }
        }

        return 0;
    }

    /** Number of parsed areas (the C {@code wmMaxAreaNum} read). */
    public static int wmMaxAreaNum() {
        return maxAreaNum;
    }

    /** Port of {@code cityIsValid}. */
    public static boolean cityIsValid(int city) {
        return city >= City.CITY_ARROYO.id() && city < wmMaxAreaNum();
    }

    // 0x4BFA84 wmMatchAreaContainingMapIdx.
    public static int wmMatchAreaContainingMapIdx(int mapIdx, City[] areaIdxPtr) {
        areaIdxPtr[0] = City.CITY_ARROYO;

        for (int areaIdx = City.CITY_ARROYO.id(); areaIdx < maxAreaNum; areaIdx++) {
            CityInfo cityInfo = areaInfoList.get(areaIdx);
            for (int entranceIdx = 0; entranceIdx < cityInfo.entrancesLength; entranceIdx++) {
                if (cityInfo.entrances[entranceIdx].map == mapIdx) {
                    areaIdxPtr[0] = cityOf(areaIdx);
                    return 0;
                }
            }
        }

        return -1;
    }

    /** Test seam: fresh-process mirror (empty table). */
    static void resetForTests() {
        areaInfoList.clear();
        maxAreaNum = 0;
    }
}