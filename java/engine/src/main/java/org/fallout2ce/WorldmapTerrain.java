package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The terrain table of the worldmap, mirroring {@code wmTerrainTypeList}
 * and its parse in {@code src/worldmap.cc}: the {@code terrain_types}
 * list of {@code data\worldmap.txt} and the random maps every terrain
 * names.
 *
 * <p>Vectors: {@code [worldmaptiles]} in
 * {@code docker/refharness/golden/v142.txt}, produced by
 * {@code sectionWorldmapTiles} in {@code docker/refharness/ref_main.cc}:
 * {@code terrain-raw=Desert:1, Mountain:2, City:1, Ocean:1}, the first
 * entries parsed ({@code Desert/1}, {@code Mountain/2}, {@code City/1})
 * and the first random maps of the first terrain ({@code Desert Encounter
 * 1/2}).
 */
public final class WorldmapTerrain {

    private WorldmapTerrain() {
    }

    /** TERRAIN_LOOKUP_NAME_SIZE (src/worldmap.cc). */
    static final int TERRAIN_LOOKUP_NAME_SIZE = 40;

    /** The most random maps a terrain can name. */
    static final int TERRAIN_RANDOM_MAP_MAX_COUNT = 20;

    /** Port of {@code Terrain} ({@code src/worldmap.cc}). */
    public static final class Terrain {
        /** The lookup name, lowercase, as the original lowercases it. */
        public String lookupName = "";

        /** The encounter difficulty of the terrain. */
        public int difficulty;

        /** How many random maps the terrain names. */
        public int mapsLength;

        /** The random maps, by the order the config names them. */
        public final WorldmapDefs.Map[] maps =
            new WorldmapDefs.Map[TERRAIN_RANDOM_MAP_MAX_COUNT];
    }

    // 0x51DDE4 wmTerrainTypeList.
    private static final List<Terrain> terrainTypeList = new ArrayList<>();

    // 0x51DDE8 wmMaxTerrainTypes.
    private static int maxTerrainTypes;

    /**
     * Port of {@code wmParseTerrainTypes} (0x4BE4B0): parse the
     * {@code terrain_types} list.
     *
     * <p>The list is comma separated; every entry names the terrain and
     * its difficulty after a colon. The random maps of every terrain are
     * read from its own {@code Random Maps: <name>} section.
     *
     * @return 0 when the table loaded, -1 when the list was empty or a
     * random map did not resolve
     */
    public static int wmTerrainInit() {
        if (!Config.configRead(GameConfig.gameConfig, "data\\worldmap.txt", true)) {
            return -1;
        }
        return wmParseTerrainTypes(GameConfig.gameConfig);
    }

    /**
     * Port of {@code wmParseTerrainTypes} (0x4BE4B0), on an open config.
     */
    static int wmParseTerrainTypes(Config.Cfg cfg) {
        String[] terrainPtr = new String[1];
        if (!Config.configGetString(cfg, "data", "terrain_types", terrainPtr)) {
            return -1;
        }
        String string = terrainPtr[0];
        if (string.isEmpty()) {
            return -1;
        }

        int terrainCount = 1;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == ',') {
                terrainCount++;
            }
        }

        terrainTypeList.clear();
        maxTerrainTypes = terrainCount;
        for (int i = 0; i < terrainCount; i++) {
            terrainTypeList.add(new Terrain());
        }

        String lower = string.toLowerCase(Locale.ROOT);

        String[] parts = lower.split(",");
        for (int index = 0; index < maxTerrainTypes; index++) {
            String part = parts[index].stripLeading();
            int delim = part.indexOf(':');
            Terrain terrain = terrainTypeList.get(index);
            terrain.lookupName = part.substring(0, delim).trim();
            if (terrain.lookupName.length() > TERRAIN_LOOKUP_NAME_SIZE) {
                terrain.lookupName = terrain.lookupName.substring(0, TERRAIN_LOOKUP_NAME_SIZE);
            }
            terrain.difficulty = Math.max(0, Integer.parseInt(part.substring(delim + 1).trim()));
        }

        for (Terrain terrain : terrainTypeList) {
            if (wmParseTerrainRndMaps(cfg, terrain) == -1) {
                return -1;
            }
        }

        return 0;
    }

    /** Port of {@code wmParseTerrainRndMaps} (0x4BE5D8). */
    private static int wmParseTerrainRndMaps(Config.Cfg cfg, Terrain terrain) {
        String section = "Random Maps: " + terrain.lookupName;

        String[] valuePtr = new String[1];
        for (;;) {
            String key = String.format("map_%02d", terrain.mapsLength);
            if (!Config.configGetString(cfg, section, key, valuePtr)) {
                break;
            }

            int mapIdx = -1;
            for (int index = WorldmapDefs.MAP_FIRST; index < MapRegistry.wmMapMaxCount(); index++) {
                if (MapRegistry.lookupName(index).equalsIgnoreCase(valuePtr[0])) {
                    mapIdx = index;
                    break;
                }
            }
            if (mapIdx == WorldmapDefs.Map.MAP_INVALID.id()) {
                return -1;
            }

            if (terrain.mapsLength >= TERRAIN_RANDOM_MAP_MAX_COUNT) {
                return -1;
            }

            terrain.maps[terrain.mapsLength] = WorldmapDefs.ofMap(mapIdx);
            terrain.mapsLength++;
        }

        return 0;
    }

    /** How many terrains the table holds. */
    public static int wmMaxTerrainTypes() {
        return maxTerrainTypes;
    }

    /** The terrain at an index, for the tests of this slice. */
    static Terrain terrainAt(int index) {
        return terrainTypeList.get(index);
    }

    /**
     * The index of the terrain a name belongs to, 0 when it is not in
     * the table - the same default the original's match yields for a
     * miss.
     */
    public static int terrainIndexByLookup(String name) {
        for (int i = 0; i < terrainTypeList.size(); i++) {
            if (terrainTypeList.get(i).lookupName.equals(name)) {
                return i;
            }
        }
        return 0;
    }

    /** Test seam: fresh-process mirror (empty table). */
    static void resetForTests() {
        terrainTypeList.clear();
        maxTerrainTypes = 0;
    }
}
