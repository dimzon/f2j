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

    /** Port of {@code CAR_FUEL_MAX} ({@code src/worldmap.h}). */
    public static final int CAR_FUEL_MAX = 80000;

    /** The car's fuel, full until the party drives. */
    private static int carFuel = CAR_FUEL_MAX;

    /** The area the car is in, invalid until the party finds it. */
    private static MapAreas.City currentCarAreaId = MapAreas.City.CITY_INVALID;

    /** Whether the party rides the car. */
    private static boolean isInCar;

    /** Port of {@code wmGenDataInit} (0x4BC984), the slice the reads use. */
    public static void wmGenDataInit() {
        currentAreaId = MapAreas.City.CITY_INVALID;
        carFuel = CAR_FUEL_MAX;
        currentCarAreaId = MapAreas.City.CITY_INVALID;
        isInCar = false;
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

    /**
     * Port of {@code wmCarFillGas} (0x4C4E34): fill the car's tank.
     *
     * @return 0 when all of the amount fit, or the amount that did not
     * fit when the tank ran over
     */
    public static int wmCarFillGas(int amount) {
        if (amount + carFuel <= CAR_FUEL_MAX) {
            carFuel += amount;
            return 0;
        }

        int remaining = CAR_FUEL_MAX - carFuel;
        carFuel = CAR_FUEL_MAX;
        return remaining;
    }

    /** Port of {@code wmCarGasAmount} (0x4C4E74). */
    public static int wmCarGasAmount() {
        return carFuel;
    }

    /** Port of {@code wmCarIsOutOfGas} (0x4C4E7C). */
    public static boolean wmCarIsOutOfGas() {
        return carFuel <= 0;
    }

    /** Port of {@code wmCarCurrentArea} (0x4C4E8C). */
    public static MapAreas.City wmCarCurrentArea() {
        return currentCarAreaId;
    }

    /**
     * Port of {@code wmCarGiveToParty} (0x4C4E94): hand the car to the
     * party and start its transition.
     *
     * <p>An out-of-gas car is refused; the message the original shows is
     * the monitor's, which is not ported. With fuel, the party rides, the
     * game is told to transition, and the out-of-gas city hiding on the
     * worldmap (the one that marks where the car was) is done by the
     * areas.
     */
    public static int wmCarGiveToParty() {
        if (carFuel <= 0) {
            return -1;
        }

        isInCar = true;

        Map.MapTransition transition = new Map.MapTransition();
        transition.map = WorldmapDefs.Map.MAP_TRANSITION;
        Map.mapSetTransition(transition);

        MapAreas.wmHideCarOutOfGasCity();

        return 0;
    }

    /** Test seam: fresh-process mirror. */
    static void resetForTests() {
        worldPosX = 0;
        worldPosY = 0;
        currentAreaId = MapAreas.City.CITY_INVALID;
        carFuel = CAR_FUEL_MAX;
        currentCarAreaId = MapAreas.City.CITY_INVALID;
        isInCar = false;
    }
}
