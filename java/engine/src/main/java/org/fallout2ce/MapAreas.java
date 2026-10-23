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

    /**
     * City of a number the game gives, invalid when it is none of them.
     *
     * <p>The C cast turns any out-of-range number into an invalid city
     * value, which the validity checks then refuse; this turns it into
     * {@link City#CITY_INVALID}, which they refuse the same way.
     */
    public static City cityFromInt(int value) {
        City city = cityOf(value);
        return city != null ? city : City.CITY_INVALID;
    }

    /** Port of {@code CityState}. */
    public enum CityState {
        CITY_STATE_UNKNOWN,
        CITY_STATE_KNOWN
    }

    /** Port of {@code VisitedState} ({@code src/worldmap.h}). */
    public enum VisitedState {
        UNKNOWN(0),
        KNOWN(1),
        VISITED(2),
        KNOWN_FO1(3),
        INVISIBLE(-66);

        private final int id;

        VisitedState(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** The state of a number the game gives, unknown when it is none of them. */
        public static VisitedState fromInt(int value) {
            for (VisitedState state : values()) {
                if (state.id == value) {
                    return state;
                }
            }
            return UNKNOWN;
        }
    }

    /** WM_TILE_WIDTH: the worldmap tile is 350 world units wide. */
    static final int WM_TILE_WIDTH = 350;

    /** WM_TILE_HEIGHT: the worldmap tile is 300 world units tall. */
    static final int WM_TILE_HEIGHT = 300;

    /** WM_VIEW_X: where the mark sits in the worldmap view. */
    static final int WM_VIEW_X = 22;

    /** WM_VIEW_Y. */
    static final int WM_VIEW_Y = 21;

    /** SUBTILE_GRID_WIDTH: the subtile grid inside a tile. */
    static final int SUBTILE_GRID_WIDTH = 7;

    /** SUBTILE_GRID_HEIGHT. */
    static final int SUBTILE_GRID_HEIGHT = 6;

    // 0x51DDF0 wmMaxTileNum / 0x51DDF4 wmNumHorizontalTiles: the tiles of
    // the worldmap. The worldmap parse that loads them is not ported, so
    // they stay zero - the same zero the C statics hold before that parse
    // runs - and the bounds they gate refuse everything, as they do in C.
    private static int wmMaxTileNum;
    private static int wmNumHorizontalTiles;

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

    /**
     * Port of {@code wmGetAreaIdxName}: message 1500 + area, clipped to
     * {@code CITY_NAME_SIZE} like the C {@code strncpy}.
     *
     * <p>NOTE: {@code getmsg} has no language fallback of its own - on a miss
     * it substitutes {@code _message_error_str}, the same message both this
     * and {@code mapGetCityName} would return.
     */
    public static String wmGetAreaIdxName(City areaIdx) {
        Message.Item entry = new Message.Item();
        String text = Message.getmsg(Map.gMapMessageList, entry, 1500 + areaIdx.id());
        return text.length() <= CITY_NAME_SIZE ? text : text.substring(0, CITY_NAME_SIZE);
    }

    // 0x4BFA84 wmMatchAreaContainingMapIdx.
    public static int wmMatchAreaContainingMapIdx(int mapIdx, City[] areaIdxPtr) {        areaIdxPtr[0] = City.CITY_ARROYO;

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

    /**
     * Port of {@code wmMapMarkVisited} (0x4BFB08): mark the area a map
     * belongs to as visited.
     *
     * <p>A map that is not saved is not marked: the original looks and
     * walks away. A map that no area names is a miss. The marking itself
     * is {@link #wmAreaMarkVisitedState}, and its answer is not asked for
     * here, the way the original does not ask.
     */
    public static int wmMapMarkVisited(int mapIdx) {
        if (!MapRegistry.mapIsValid(mapIdx)) {
            return -1;
        }

        if (!MapRegistry.wmMapIdxIsSaveable(mapIdx)) {
            return 0;
        }

        City[] areaIdxPtr = new City[1];
        if (wmMatchAreaContainingMapIdx(mapIdx, areaIdxPtr) == -1) {
            return -1;
        }

        wmAreaMarkVisited(areaIdxPtr[0]);

        return 0;
    }

    /**
     * Port of {@code wmAreaSetVisibleState} (0x4C46CC): set whether an
     * area is on the worldmap at all. A locked area refuses, unless the
     * change is forced.
     */
    public static boolean wmAreaSetVisibleState(City areaIdx, CityState state, boolean force) {
        if (!cityIsValid(areaIdx.id())) {
            return false;
        }

        CityInfo city = areaInfoList.get(areaIdx.id());
        if (city.lockState != LockState.LOCK_STATE_LOCKED || force) {
            city.state = state;
            return true;
        }

        return false;
    }

    /**
     * Port of {@code wmAreaMarkVisited} (0x4C4624): mark an area visited.
     */
    public static int wmAreaMarkVisited(City areaIdx) {
        return wmAreaMarkVisitedState(areaIdx, VisitedState.VISITED) ? 1 : 0;
    }

    /**
     * Port of {@code wmAreaMarkVisitedState} (0x4C4634): mark how much the
     * player knows of an area.
     *
     * <p>The state is kept on the city, and the subtile under the city's
     * mark is looked up first: the marking stops when there is no subtile
     * there. The subtile grid comes with the worldmap tiles, which the
     * port does not have, so the coordinates are never valid and the
     * marking stops for every city - the same place it stops in C before
     * the worldmap parse runs.
     */
    public static boolean wmAreaMarkVisitedState(City areaIdx, VisitedState state) {
        if (!cityIsValid(areaIdx.id())) {
            return false;
        }

        CityInfo city = areaInfoList.get(areaIdx.id());
        VisitedState oldVisitedState = visitedStateOf(city);
        boolean noRadius = state == VisitedState.KNOWN_FO1;
        if (noRadius) {
            state = VisitedState.KNOWN;
        }

        int[] markPos = new int[2];
        wmAreaGetMarkWorldPos(city, markPos);

        if (!wmSubtileCoordsValid(markPos[0], markPos[1])) {
            return false;
        }

        if (city.state == CityState.CITY_STATE_KNOWN && state != VisitedState.UNKNOWN) {
            if (state == VisitedState.VISITED) {
                wmMarkSubTileRadiusVisited(markPos[0], markPos[1]);
            } else if (!noRadius) {
                wmSubTileMarkRadiusKnown(markPos[0], markPos[1], 1);
            }
        }

        city.visitedState = state;

        // The subtile's own state goes known with the city in C's Known
        // branch; there is no subtile to write it to here.

        if (state == VisitedState.VISITED && oldVisitedState == VisitedState.UNKNOWN) {
            city.visitedState = VisitedState.KNOWN;
        }

        return true;
    }

    /**
     * Port of {@code wmAreaSetWorldPos} (0x4C4710): move an area on the
     * worldmap.
     *
     * <p>The position is checked against the tiles of the worldmap, which
     * the worldmap parse loads and the port does not have, so every
     * position is refused - the same refusal C gives before that parse
     * runs. The x check refuses first, so the division of the y bound is
     * never reached, exactly as in C.
     */
    public static int wmAreaSetWorldPos(City areaIdx, int x, int y) {
        if (!cityIsValid(areaIdx.id())) {
            return -1;
        }

        if (x < 0 || x >= WM_TILE_WIDTH * wmNumHorizontalTiles) {
            return -1;
        }

        if (y < 0 || y >= WM_TILE_HEIGHT * (wmMaxTileNum / wmNumHorizontalTiles)) {
            return -1;
        }

        CityInfo city = areaInfoList.get(areaIdx.id());
        city.x = x;
        city.y = y;

        return 0;
    }

    /**
     * The visited state a city holds.
     *
     * <p>The slot init leaves the field empty, and the empty field is the
     * C zero - {@link VisitedState#UNKNOWN}.
     */
    private static VisitedState visitedStateOf(CityInfo city) {
        return city.visitedState instanceof VisitedState
            ? (VisitedState) city.visitedState
            : VisitedState.UNKNOWN;
    }

    /**
     * Port of {@code wmAreaGetMarkWorldPos}: where the mark of a city sits
     * in the world, from the city's own position and the half of its size.
     */
    private static void wmAreaGetMarkWorldPos(CityInfo city, int[] pos) {
        int markHalfWidth = WM_VIEW_X;
        int markHalfHeight = WM_VIEW_Y;

        switch (city.size) {
        case CITY_SIZE_SMALL:
            markHalfWidth = 7;
            markHalfHeight = 6;
            break;
        case CITY_SIZE_MEDIUM:
            markHalfWidth = 12;
            markHalfHeight = 11;
            break;
        default:
            break;
        }

        pos[0] = Math.max(city.x + markHalfWidth - WM_VIEW_X, 0);
        pos[1] = Math.max(city.y + markHalfHeight - WM_VIEW_Y, 0);
    }

    /**
     * Port of {@code wmSubtileCoordsValid}: the subtile coordinates are
     * inside the tiles of the worldmap. The tiles are not loaded (see
     * {@link #wmNumHorizontalTiles}), so no coordinates are valid yet -
     * the same false the guard gives in C before the worldmap parse.
     */
    private static boolean wmSubtileCoordsValid(int x, int y) {
        if (wmNumHorizontalTiles <= 0 || wmMaxTileNum <= 0) {
            return false;
        }

        return x >= 0
            && x < SUBTILE_GRID_WIDTH * wmNumHorizontalTiles
            && y >= 0
            && y < SUBTILE_GRID_HEIGHT * (wmMaxTileNum / wmNumHorizontalTiles);
    }

    /**
     * Seam for {@code wmMarkSubTileRadiusVisited}: the subtile grid is not
     * ported, so the radius around a visited city is marked nowhere.
     */
    private static void wmMarkSubTileRadiusVisited(int x, int y) {
    }

    /**
     * Seam for {@code wmSubTileMarkRadiusKnown}: the subtile grid is not
     * ported, so the radius around a known city is marked nowhere.
     */
    private static void wmSubTileMarkRadiusKnown(int x, int y, int radius) {
    }

    /** Test seam: fresh-process mirror (empty table). */
    static void resetForTests() {
        areaInfoList.clear();
        maxAreaNum = 0;
    }
}