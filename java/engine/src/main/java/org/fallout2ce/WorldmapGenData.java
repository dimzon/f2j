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

    // The walk state of the party (wmGenData's walk slice).
    private static int walkDestinationX;
    private static int walkDestinationY;
    private static int walkDistance;
    private static int walkLineDelta;
    private static int walkLineDeltaMainAxisStep;
    private static int walkLineDeltaCrossAxisStep;
    private static int walkWorldPosMainAxisStepX;
    private static int walkWorldPosMainAxisStepY;
    private static int walkWorldPosCrossAxisStepX;
    private static int walkWorldPosCrossAxisStepY;
    private static boolean isWalking;

    /** Port of {@code _terrainCounter} (src/worldmap.cc). */
    private static int terrainCounter;

    /** Port of {@code wmGenDataInit} (0x4BC984), the slice the reads use. */
    public static void wmGenDataInit() {
        currentAreaId = MapAreas.City.CITY_INVALID;
        carFuel = CAR_FUEL_MAX;
        currentCarAreaId = MapAreas.City.CITY_INVALID;
        isInCar = false;
        walkDestinationX = 0;
        walkDestinationY = 0;
        walkDistance = 0;
        walkLineDelta = 0;
        walkLineDeltaMainAxisStep = 0;
        walkLineDeltaCrossAxisStep = 0;
        walkWorldPosMainAxisStepX = 0;
        walkWorldPosMainAxisStepY = 0;
        walkWorldPosCrossAxisStepX = 0;
        walkWorldPosCrossAxisStepY = 0;
        isWalking = false;
        terrainCounter = 0;
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

    /**
     * Port of {@code wmPartyInitWalking} (0x4C1E54): start the party
     * walking to a world position.
     *
     * <p>The Bresenham line is set up the way the original sets it up;
     * the interface centering is not ported.
     */
    public static void wmPartyInitWalking(int x, int y) {
        walkDestinationX = x;
        walkDestinationY = y;
        currentAreaId = MapAreas.City.CITY_INVALID;
        isWalking = true;

        int dx = Math.abs(x - worldPosX);
        int dy = Math.abs(y - worldPosY);

        if (dx < dy) {
            walkDistance = dy;
            walkLineDeltaMainAxisStep = 2 * dx;
            walkWorldPosMainAxisStepX = 0;
            walkLineDelta = 2 * dx - dy;
            walkLineDeltaCrossAxisStep = 2 * (dx - dy);
            walkWorldPosCrossAxisStepX = 1;
            walkWorldPosMainAxisStepY = 1;
            walkWorldPosCrossAxisStepY = 1;
        } else {
            walkDistance = dx;
            walkLineDeltaMainAxisStep = 2 * dy;
            walkWorldPosMainAxisStepY = 0;
            walkLineDelta = 2 * dy - dx;
            walkLineDeltaCrossAxisStep = 2 * (dy - dx);
            walkWorldPosMainAxisStepX = 1;
            walkWorldPosCrossAxisStepX = 1;
            walkWorldPosCrossAxisStepY = 1;
        }

        if (walkDestinationX < worldPosX) {
            walkWorldPosCrossAxisStepX = -walkWorldPosCrossAxisStepX;
            walkWorldPosMainAxisStepX = -walkWorldPosMainAxisStepX;
        }

        if (walkDestinationY < worldPosY) {
            walkWorldPosCrossAxisStepY = -walkWorldPosCrossAxisStepY;
            walkWorldPosMainAxisStepY = -walkWorldPosMainAxisStepY;
        }
    }

    /** The walk of the party is in progress. */
    public static boolean wmPartyIsWalking() {
        return isWalking;
    }

    /** How far the party still walks, in steps. */
    public static int wmPartyWalkDistance() {
        return walkDistance;
    }

    /**
     * Port of {@code wmWorldPosInvalid} (0x4C1E38): whether a world
     * position is blocked by the walk mask of its tile.
     *
     * <p>The walk masks come with the tile art and are not loaded by the
     * port, so the position is never invalid - the same answer the
     * original gives before the masks load.
     */
    private static boolean wmWorldPosInvalid(int x, int y) {
        return false;
    }

    /**
     * Port of {@code wmPartyWalkingStep} (0x4C1F90): one step of the
     * party's walk.
     *
     * <p>The step moves along the line the init set up, once the terrain
     * difficulty allows it; the interface scroll and the trail markers
     * are not ported. A blocked position stops the walk at the current
     * spot and resolves the area from it.
     */
    public static void wmPartyWalkingStep() {
        if (walkDistance <= 0) {
            return;
        }

        terrainCounter++;
        if (terrainCounter > 4) {
            terrainCounter = 1;
        }

        WorldmapTiles.SubtileInfo subtile = WorldmapTiles.wmFindCurSubTileFromPos(worldPosX, worldPosY);
        if (subtile == null) {
            return;
        }

        int terrainDifficulty = WorldmapTerrain.terrainAt(subtile.terrain).difficulty;
        if (terrainDifficulty < 1) {
            terrainDifficulty = 1;
        }

        if (terrainCounter / terrainDifficulty < 1) {
            return;
        }

        if (walkLineDelta >= 0) {
            if (wmWorldPosInvalid(walkWorldPosCrossAxisStepX + worldPosX,
                    walkWorldPosCrossAxisStepY + worldPosY)) {
                stopAtCurrent();
                return;
            }

            walkLineDelta += walkLineDeltaCrossAxisStep;
            worldPosX += walkWorldPosCrossAxisStepX;
            worldPosY += walkWorldPosCrossAxisStepY;
        } else {
            if (wmWorldPosInvalid(walkWorldPosMainAxisStepX + worldPosX,
                    walkWorldPosMainAxisStepY + worldPosY)) {
                stopAtCurrent();
                return;
            }

            walkLineDelta += walkLineDeltaMainAxisStep;
            worldPosX += walkWorldPosMainAxisStepX;
            worldPosY += walkWorldPosMainAxisStepY;
        }

        walkDistance -= 1;
        if (walkDistance == 0) {
            walkDestinationX = 0;
            walkDestinationY = 0;
            isWalking = false;
        }
    }

    /** The stop the walking step does when the walk ends or is blocked. */
    private static void stopAtCurrent() {
        walkDestinationX = 0;
        walkDestinationY = 0;
        isWalking = false;
        currentAreaId = MapAreas.wmMatchAreaFromWorldPos(worldPosX, worldPosY);
        walkDistance = 0;
    }

    /** Test seam: fresh-process mirror. */
    static void resetForTests() {
        worldPosX = 0;
        worldPosY = 0;
        currentAreaId = MapAreas.City.CITY_INVALID;
        carFuel = CAR_FUEL_MAX;
        currentCarAreaId = MapAreas.City.CITY_INVALID;
        isInCar = false;
        walkDestinationX = 0;
        walkDestinationY = 0;
        walkDistance = 0;
        walkLineDelta = 0;
        walkLineDeltaMainAxisStep = 0;
        walkLineDeltaCrossAxisStep = 0;
        walkWorldPosMainAxisStepX = 0;
        walkWorldPosMainAxisStepY = 0;
        walkWorldPosCrossAxisStepX = 0;
        walkWorldPosCrossAxisStepY = 0;
        isWalking = false;
        terrainCounter = 0;
    }
}
