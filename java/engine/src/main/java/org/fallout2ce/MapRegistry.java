package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * Worldmap map registry, mirroring the global map table in
 * {@code src/worldmap.cc}: {@code wmMapInfoList}/{@code wmMaxMapNum} plus
 * the name lookups and the predicates built on them.
 *
 * <p>Vectors: the {@code [mapregistry]} section in
 * {@code docker/refharness/golden/v105.txt} pins {@code wmMapMaxCount}
 * (151 entries for this build), {@code mapIsValid} at both edges, the
 * {@code wmMapIdxToName} format ({@code name.MAP}, lowercased) plus its two
 * out-of-range results, {@code wmMapMatchNameToIdx} for hits, misses and
 * the extension/case handling, {@code wmMapIdxIsSaveable}, and a name
 * round-trip that proves the two directions agree.
 *
 * <p>Only {@code wmMapInit}/{@code wmParseMapsConfig} are ported: the table
 * lives in {@code data\MAPS.TXT} inside the game database and is read
 * through {@link Config#configRead}. The rest of {@code wmWorldMap_init}
 * (message list, gen data, visited state, encounter tables) is worldmap
 * interface state and stays unported, so this slice exposes only the
 * map-name half of the registry.
 *
 * <p>NOTES / seams:
 * <li>C stores the names as fixed {@code char[40]} arrays; Java uses
 * {@link String} and clips at the array size, matching {@code strncpy}
 * plus the explicit NUL the C code writes afterwards.</li>
 * <li>The ambient-sfx and start-point tables stay at their
 * {@code wmMapSlotInit} defaults: the keys that fill them need the
 * message list and proto lookups, which this slice does not reach.</li>
 * <li>{@code wmMapMatchNameToIdx} lowercases its argument in place and
 * truncates at the first {@code '.'}. Both are kept because they decide
 * whether a caller-supplied buffer is mutated, not just what index
 * comes back.</li>
 */
public final class MapRegistry {

    private MapRegistry() {
    }

    /**
     * MAP_AMBIENT_SOUND_EFFECTS_CAPACITY. Raised from 6 to 7 upstream so
     * "Tartar 3rd Floor 2" and "Livos Living Rooms" fit; 4 would truncate
     * those configs and drop sfx on save.
     */
    static final int MAP_AMBIENT_SOUND_EFFECTS_CAPACITY = 7;

    /** MAP_STARTING_POINTS_CAPACITY. */
    static final int MAP_STARTING_POINTS_CAPACITY = 15;

    /** Port of {@code MapAmbientSoundEffectInfo} ({@code src/worldmap.cc}). */
    static final class MapAmbientSoundEffectInfo {
        String name = "";
        int chance;
    }

    /** Port of {@code MapStartPointInfo} ({@code src/worldmap.cc}). */
    static final class MapStartPointInfo {
        int elevation;
        int tile;
        Rotation rotation;
    }

    /** Port of the {@code MapFlags} bits ({@code src/worldmap.h}). */
    static final class MapFlags {
        static final int MAP_NONE = 0x00;
        static final int MAP_SAVED = 0x01;
        static final int MAP_DEAD_BODIES_AGE = 0x02;
        static final int MAP_PIPBOY_ACTIVE = 0x04;
        static final int MAP_CAN_REST_ELEVATION_0 = 0x08;
        static final int MAP_CAN_REST_ELEVATION_1 = 0x10;
        static final int MAP_CAN_REST_ELEVATION_2 = 0x20;

        int bits;

        MapFlags(int bits) {
            this.bits = bits;
        }

        boolean has(int mask) {
            return (bits & mask) == mask;
        }
    }

    /** Port of {@code MapInfo} ({@code src/worldmap.cc}). */
    static final class MapInfo {
        String lookupName = "";
        int field_28;
        int field_2C;
        String mapFileName = "";
        String music = "";
        MapFlags flags;
        int ambientSoundEffectsLength;
        final MapAmbientSoundEffectInfo[] ambientSoundEffects =
                new MapAmbientSoundEffectInfo[MAP_AMBIENT_SOUND_EFFECTS_CAPACITY];
        int startPointsLength;
        final MapStartPointInfo[] startPoints = new MapStartPointInfo[MAP_STARTING_POINTS_CAPACITY];
    }

    // 0x51DE0C wmMapInfoList.
    private static final List<MapInfo> mapInfoList = new ArrayList<>();

    // 0x51DE10 wmMaxMapNum.
    private static int maxMapNum;

    /** Port of {@code wmMapSlotInit} ({@code src/worldmap.cc}). */
    private static MapInfo mapSlotInit() {
        MapInfo map = new MapInfo();
        map.field_28 = -1;
        map.field_2C = -1;
        map.flags = new MapFlags(MapFlags.MAP_SAVED | MapFlags.MAP_DEAD_BODIES_AGE
                | MapFlags.MAP_PIPBOY_ACTIVE | MapFlags.MAP_CAN_REST_ELEVATION_0
                | MapFlags.MAP_CAN_REST_ELEVATION_1 | MapFlags.MAP_CAN_REST_ELEVATION_2);
        for (int index = 0; index < MAP_AMBIENT_SOUND_EFFECTS_CAPACITY; index++) {
            map.ambientSoundEffects[index] = new MapAmbientSoundEffectInfo();
        }
        for (int index = 0; index < MAP_STARTING_POINTS_CAPACITY; index++) {
            map.startPoints[index] = new MapStartPointInfo();
        }
        return map;
    }

    /**
     * Port of {@code wmParseMapsConfig} restricted to the keys this slice
     * reads: {@code lookup_name}, {@code map_name} and {@code saved}.
     */
    static int wmParseMapsConfig(Config.Cfg cfg, boolean reindex) {
        if (cfg == null) {
            return -1;
        }

        int mapIdx = reindex ? WorldmapDefs.MAP_FIRST : maxMapNum;
        int loopSafetyCounter = 0;
        String[] valuePtr = new String[1];
        while (loopSafetyCounter < 5000) {
            String section = String.format("Map %03d", mapIdx);

            if (!Config.configGetString(cfg, section, "lookup_name", valuePtr)) {
                break;
            }

            maxMapNum++;

            MapInfo map = mapSlotInit();
            map.lookupName = clip(valuePtr[0], 40);

            if (!Config.configGetString(cfg, section, "map_name", valuePtr)) {
                return -1;
            }

            map.mapFileName = clip(valuePtr[0], 39).toLowerCase();

            if (Config.configGetString(cfg, section, "saved", valuePtr)) {
                int[] num = new int[1];
                StringParsers.Cursor cursor = new StringParsers.Cursor(valuePtr[0]);
                if (StringParsers.strParseStrFromList(cursor, num, new String[] { "no", "yes" }) == -1) {
                    return -1;
                }
                // wmSetFlags: yes sets MAP_SAVED, no clears it — the slot init
                // sets the bit, so a "no" entry must drop it again.
                if (num[0] != 0) {
                    map.flags.bits |= MapFlags.MAP_SAVED;
                } else {
                    map.flags.bits &= ~MapFlags.MAP_SAVED;
                }
            }

            mapInfoList.add(map);
            mapIdx++;
            loopSafetyCounter++;
        }

        return 0;
    }

    /** Clip like {@code strncpy} into a fixed {@code char[n]}. */
    private static String clip(String value, int size) {
        return value.length() <= size ? value : value.substring(0, size);
    }

    /**
     * Port of {@code wmMapInit}: reads the map table from
     * {@code data\MAPS.TXT} in the game database.
     */
    static int wmMapInit() {
        if (Config.configRead(GameConfig.gameConfig, "data\\maps.txt", true)) {
            if (wmParseMapsConfig(GameConfig.gameConfig, false) == -1) {
                return -1;
            }
        }
        return 0;
    }

    // 0x4BF96C wmMapMaxCount.
    public static int wmMapMaxCount() {
        return maxMapNum;
    }

    /** Port of {@code mapIsValid} ({@code src/worldmap.h}, inline). */
    public static boolean mapIsValid(int map) {
        return map >= WorldmapDefs.MAP_FIRST && map < wmMapMaxCount();
    }

    // 0x4BF974 wmMapIdxToName.
    public static int wmMapIdxToName(int mapIdx, byte[] dest, int size) {
        if (!mapIsValid(mapIdx)) {
            dest[0] = 0;
            return -1;
        }
        String name = mapInfoList.get(mapIdx).mapFileName + ".MAP";
        int n = Math.min(name.length(), size - 1);
        for (int index = 0; index < n; index++) {
            dest[index] = (byte) name.charAt(index);
        }
        dest[n] = 0;
        return 0;
    }

    /** String-returning convenience for callers that need no buffer. */
    public static String wmMapIdxToName(int mapIdx) {
        if (!mapIsValid(mapIdx)) {
            return "";
        }
        return mapInfoList.get(mapIdx).mapFileName + ".MAP";
    }

    // 0x4BF9BC wmMapMatchNameToIdx.
    public static int wmMapMatchNameToIdx(String name) {
        // C lowercases and truncates in place at the first '.', then restores
        // the '.'; the returned index only depends on the truncated form.
        String lower = name.toLowerCase();

        int pch = lower.indexOf('.');
        String probe = pch != -1 ? lower.substring(0, pch) : lower;

        int map = WorldmapDefs.Map.MAP_INVALID.id();
        for (int index = WorldmapDefs.MAP_FIRST; index < maxMapNum; index++) {
            if (mapInfoList.get(index).mapFileName.equals(probe)) {
                map = index;
                break;
            }
        }

        return map;
    }

    // 0x4BFA44 wmMapIdxIsSaveable.
    public static boolean wmMapIdxIsSaveable(int mapIdx) {
        return mapInfoList.get(mapIdx).flags.has(MapFlags.MAP_SAVED);
    }

    /**
     * The {@code lookup_name} of an entry, which is how area entrances
     * reference their map (C: {@code wmParseFindMapIdxMatch}).
     */
    static String lookupName(int mapIdx) {
        return mapInfoList.get(mapIdx).lookupName;
    }

    /** Test seam: fresh-process mirror (empty registry). */
    static void resetForTests() {
        mapInfoList.clear();
        maxMapNum = 0;
    }
}
