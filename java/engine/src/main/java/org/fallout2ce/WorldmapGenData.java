package org.fallout2ce;

/**
 * The worldmap gen data, mirroring {@code wmGenData} in
 * {@code src/worldmap.cc}: where the party stands on the world, and which
 * area that is.
 *
 * <p>Vectors: {@code [worldmaptiles]} in
 * {@code docker/refharness/golden/v141.txt}, produced by
 * {@code sectionWorldmapTiles} in {@code docker/refharness/ref_main.cc}:
 * {@code party-pos=173,122} (the default start; the shipped config has no
 * {@code worldmap_x/y}) and {@code party-area=-1} (the init leaves the
 * area unset - the party's own movement sets it).
 *
 * <p>NOTES / seams:
 * <ul>
 * <li>{@code wmGenDataInit} also resets the car, the walk state, the
 * viewport and the rest of the struct; the port carries the slice the
 * metarule reads and the future worldmap uses. The view offsets live with
 * the view, which is not ported.</li>
 * <li>{@code wmGenDataClampWorldPosToBounds} reads the tiles;
 * {@link WorldmapTiles#wmTilesInit} must have run for the bounds to be
 * there. Before that there is no world to clamp against, and the start
 * stays as the config gives it.</li>
 * </ul>
 */
public final class WorldmapGenData {

    private WorldmapGenData() {
    }

    // 0x672D04 wmGenData.
    private static int worldPosX;
    private static int worldPosY;

    /** The area the party is in, invalid until the party moves. */
    private static MapAreas.City currentAreaId = MapAreas.City.CITY_INVALID;

    /** Port of {@code wmGenDataInit} (0x4BC984), the slice the reads use. */
    public static void wmGenDataInit() {
        currentAreaId = MapAreas.City.CITY_INVALID;
        wmGenDataSetStartWorldPos();
        wmGenDataClampWorldPosToBounds();
    }

    /** Port of {@code wmGenDataSetStartWorldPos} (0x4BC884). */
    private static void wmGenDataSetStartWorldPos() {
        worldPosX = 173;
        worldPosY = 122;

        int[] value = new int[1];
        if (wmGetStartWorldMapConfigValue("worldmap_x", value)) {
            worldPosX = value[0];
        }

        if (wmGetStartWorldMapConfigValue("worldmap_y", value)) {
            worldPosY = value[0];
        }
    }

    /** Port of {@code wmGetStartWorldMapConfigValue} (0x4BC930). */
    private static boolean wmGetStartWorldMapConfigValue(String key, int[] valuePtr) {
        int[] value = new int[1];
        if (!Config.configGetInt(ContentConfig.contentConfig, ContentConfig.START_SECTION,
                key, value)) {
            return false;
        }

        if (value[0] == -1) {
            return false;
        }

        valuePtr[0] = Math.max(value[0], 0);
        return true;
    }

    /** Port of {@code wmGenDataClampWorldPosToBounds} (0x4BC9F4). */
    private static void wmGenDataClampWorldPosToBounds() {
        if (WorldmapTiles.wmNumHorizontalTiles() <= 0
                || WorldmapTiles.wmMaxTileNum() <= 0) {
            return;
        }

        int worldMaxX = WorldmapTiles.WM_TILE_WIDTH * WorldmapTiles.wmNumHorizontalTiles();
        int worldMaxY = WorldmapTiles.WM_TILE_HEIGHT
            * (WorldmapTiles.wmMaxTileNum() / WorldmapTiles.wmNumHorizontalTiles());
        if (worldMaxX <= 0 || worldMaxY <= 0) {
            return;
        }

        worldPosX = Math.min(Math.max(worldPosX, 0), worldMaxX - 1);
        worldPosY = Math.min(Math.max(worldPosY, 0), worldMaxY - 1);
    }

    /** Port of {@code wmGetPartyWorldPos} (0x4C47A4). */
    public static int wmGetPartyWorldPosX() {
        return worldPosX;
    }

    /** Port of {@code wmGetPartyWorldPos} (0x4C47A4). */
    public static int wmGetPartyWorldPosY() {
        return worldPosY;
    }

    /** Port of {@code wmGetPartyCurArea} (0x4C47B8). */
    public static MapAreas.City wmGetPartyCurArea() {
        return currentAreaId;
    }

    /** Test seam: fresh-process mirror. */
    static void resetForTests() {
        worldPosX = 0;
        worldPosY = 0;
        currentAreaId = MapAreas.City.CITY_INVALID;
    }
}
