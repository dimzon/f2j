package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * The tiles of the worldmap, mirroring the tile slice of
 * {@code src/worldmap.cc}: the {@code Tile Data} and {@code Tile N}
 * sections of {@code data\worldmap.txt} that {@code wmConfigInit} reads,
 * the subtile grid every tile carries, and the subtile marking the
 * visited-state setters ride on.
 *
 * <p>Vectors: {@code [worldmaptiles]} in
 * {@code docker/refharness/golden/v139.txt}, produced by
 * {@code sectionWorldmapTiles} in {@code docker/refharness/ref_main.cc}:
 * {@code num-horizontal=4 tiles=20 bounds=1400,1500}, the fields of the
 * first tiles ({@code art 339..341}, {@code diff -15},
 * {@code mask wrldmp00}), all 42 subtile strings of tile 0, and the
 * radius marking - the ring around a visited center goes known, the
 * center itself visited.
 *
 * <p>NOTES / seams:
 * <ul>
 * <li>The subtile strings are required the way the original requires them
 * (a missing one stops the load), but their terrain, fill, encounter
 * frequencies and encounter type are not parsed - they wait for the
 * terrain and encounter tables of the same file. The subtile keeps its
 * state, which is what the marking writes, and the fill branches of
 * {@link #wmSubTileMarkRadiusVisited} stay dormant with them (a fresh
 * subtile is no_fill, so the vector's marking is unaffected).</li>
 * <li>{@code wmFindCurSubTileFromPos} in C never fails - the -1 the
 * callers check for is vestigial, and a call before the worldmap parse
 * reads through a null list. The port refuses instead: no tiles, no
 * subtile.</li>
 * </ul>
 */
public final class WorldmapTiles {

    private WorldmapTiles() {
    }

    /** WM_TILE_WIDTH: the worldmap tile is 350 world units wide. */
    public static final int WM_TILE_WIDTH = 350;

    /** WM_TILE_HEIGHT: the worldmap tile is 300 world units tall. */
    public static final int WM_TILE_HEIGHT = 300;

    /** WM_SUBTILE_SIZE: the subtile is 50 world units. */
    public static final int WM_SUBTILE_SIZE = 50;

    /** SUBTILE_GRID_WIDTH: the subtile grid inside a tile. */
    public static final int SUBTILE_GRID_WIDTH = 7;

    /** SUBTILE_GRID_HEIGHT. */
    public static final int SUBTILE_GRID_HEIGHT = 6;

    /** TILE_WALK_MASK_NAME_SIZE. */
    static final int TILE_WALK_MASK_NAME_SIZE = 40;

    /** Port of {@code SubtileState} ({@code src/worldmap.cc}). */
    public enum SubtileState {
        SUBTILE_STATE_UNKNOWN(0),
        SUBTILE_STATE_KNOWN(1),
        SUBTILE_STATE_VISITED(2);

        private final int id;

        SubtileState(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    /**
     * Port of {@code SubtileInfo}, the state slice: how much the player
     * has seen of this square of the world.
     */
    public static final class SubtileInfo {
        /** How much the player has seen: unknown, known or visited. */
        public SubtileState state = SubtileState.SUBTILE_STATE_UNKNOWN;
    }

    /**
     * Port of {@code TileInfo}, the parsed slice: the art of the tile, how
     * hard its encounters are, and the mask of where the party may walk.
     */
    public static final class TileInfo {
        /** The frame id of the tile's art, -1 until parsed. */
        public int fid = -1;

        /** The encounter difficulty modifier of the tile. */
        public int encounterDifficultyModifier;

        /** The walk mask name, empty when the tile has none. */
        public String walkMaskName = "";

        /** The subtile grid: [column][row], 6 by 7, every cell in place. */
        public final SubtileInfo[][] subtiles = new SubtileInfo[SUBTILE_GRID_HEIGHT][SUBTILE_GRID_WIDTH];

        TileInfo() {
            for (int column = 0; column < SUBTILE_GRID_HEIGHT; column++) {
                for (int row = 0; row < SUBTILE_GRID_WIDTH; row++) {
                    subtiles[column][row] = new SubtileInfo();
                }
            }
        }
    }

    // 0x51DDEC wmTileInfoList.
    private static final List<TileInfo> tileInfoList = new ArrayList<>();

    // 0x51DDF0 wmMaxTileNum: how many tiles the list holds.
    private static int wmMaxTileNum;

    // 0x51DDF4 wmNumHorizontalTiles: the width of the grid in tiles.
    private static int wmNumHorizontalTiles;

    /**
     * Port of the tile part of {@code wmConfigInit}: read
     * {@code data\worldmap.txt} and load its tiles.
     *
     * <p>0 when the tiles loaded, -1 when the file would not read or a
     * tile came without one of its subtile strings.
     */
    public static int wmTilesInit() {
        if (Config.configRead(GameConfig.gameConfig, "data\\worldmap.txt", true)) {
            return wmParseTilesConfig(GameConfig.gameConfig);
        }
        return 0;
    }

    /**
     * Port of the tile loop of {@code wmConfigInit} (0x4C0F76 area): the
     * {@code Tile Data} header gives the width of the grid, the
     * {@code Tile N} sections give the tiles until one comes without its
     * art, and every tile names all 42 of its subtiles.
     */
    static int wmParseTilesConfig(Config.Cfg cfg) {
        int[] num = new int[1];
        if (!Config.configGetInt(cfg, "Tile Data", "num_horizontal_tiles", num)) {
            // The original puts a message box up and stops the load; the
            // message box is the interface's, which is not ported.
            return -1;
        }
        wmNumHorizontalTiles = num[0];

        String[] valuePtr = new String[1];
        for (int tileIndex = 0; tileIndex < 9999; tileIndex++) {
            String section = "Tile " + tileIndex;

            if (!Config.configGetInt(cfg, section, "art_idx", num)) {
                break;
            }

            TileInfo tile = new TileInfo();
            tileInfoList.add(tile);
            wmMaxTileNum++;

            tile.fid = num[0];

            if (Config.configGetInt(cfg, section, "encounter_difficulty", num)) {
                tile.encounterDifficultyModifier = num[0];
            }

            if (Config.configGetString(cfg, section, "walk_mask_name", valuePtr)) {
                String name = valuePtr[0];
                tile.walkMaskName = name.length() <= TILE_WALK_MASK_NAME_SIZE
                    ? name : name.substring(0, TILE_WALK_MASK_NAME_SIZE);
            }

            for (int column = 0; column < SUBTILE_GRID_HEIGHT; column++) {
                for (int row = 0; row < SUBTILE_GRID_WIDTH; row++) {
                    // The string is required the way the original requires
                    // it; what it says waits for the terrain and encounter
                    // tables, and the subtile starts from the state every
                    // parse starts it from.
                    if (!Config.configGetString(cfg, section, row + "_" + column, valuePtr)) {
                        return -1;
                    }
                }
            }
        }

        return 0;
    }

    /** The width of the worldmap grid in tiles. */
    public static int wmNumHorizontalTiles() {
        return wmNumHorizontalTiles;
    }

    /** How many tiles the worldmap grid holds. */
    public static int wmMaxTileNum() {
        return wmMaxTileNum;
    }

    /** The tile at an index of the list, for the tests of this slice. */
    static TileInfo tileAt(int index) {
        return tileInfoList.get(index);
    }

    /**
     * Port of {@code wmSubtileCoordsValid}: the subtile-grid coordinates
     * are inside the tiles of the worldmap (they count subtiles, not
     * world units). The tiles are not loaded until {@link #wmTilesInit}
     * runs, and no coordinates are valid before that.
     */
    public static boolean wmSubtileCoordsValid(int x, int y) {
        if (wmNumHorizontalTiles <= 0 || wmMaxTileNum <= 0) {
            return false;
        }

        return x >= 0
            && x < SUBTILE_GRID_WIDTH * wmNumHorizontalTiles
            && y >= 0
            && y < SUBTILE_GRID_HEIGHT * (wmMaxTileNum / wmNumHorizontalTiles);
    }

    /**
     * Whether world coordinates are inside the world the tiles cover -
     * the bounds {@code wmAreaSetWorldPos} checks and the safe find below
     * reads. World units, and nothing is inside before the tiles load.
     */
    public static boolean wmWorldPosInBounds(int x, int y) {
        if (wmNumHorizontalTiles <= 0 || wmMaxTileNum <= 0) {
            return false;
        }
        return x >= 0
            && x < WM_TILE_WIDTH * wmNumHorizontalTiles
            && y >= 0
            && y < WM_TILE_HEIGHT * (wmMaxTileNum / wmNumHorizontalTiles);
    }

    /**
     * Port of {@code wmSubTileGetVisitedState} (0x4C3740): how much the
     * player has seen of the subtile at a world position.
     *
     * <p>The original walks its list without a look at the bounds; the
     * port refuses when the tiles are not loaded, the same refusal the
     * find below gives.
     *
     * @return 0 with the state written, or -1 when there is no subtile
     */
    public static int wmSubTileGetVisitedState(int x, int y, SubtileState[] statePtr) {
        SubtileInfo subtile = wmFindCurSubTileFromPos(x, y);
        if (subtile == null) {
            return -1;
        }
        statePtr[0] = subtile.state;
        return 0;
    }

    /**
     * Port of {@code wmFindCurSubTileFromPos} (0x4C0C2C): the subtile at
     * a world position. The original never fails - the -1 its callers
     * check for is vestigial - but it reads through a null list when the
     * tiles are not loaded, and the port refuses that instead.
     */
    static SubtileInfo wmFindCurSubTileFromPos(int x, int y) {
        if (!wmWorldPosInBounds(x, y)) {
            return null;
        }

        int tileIndex = y / WM_TILE_HEIGHT * wmNumHorizontalTiles
            + x / WM_TILE_WIDTH % wmNumHorizontalTiles;
        int column = y % WM_TILE_HEIGHT / WM_SUBTILE_SIZE;
        int row = x % WM_TILE_WIDTH / WM_SUBTILE_SIZE;
        return tileInfoList.get(tileIndex).subtiles[column][row];
    }

    /**
     * Port of {@code wmMarkSubTileOffsetVisitedFunc} (0x4C3434): write a
     * state to a subtile at an offset, wrapping across the tile edges the
     * way the grid wraps - off the right edge is the next tile over,
     * unless this is the last tile in the row, and the same for the rest.
     *
     * <p>A known state never overwrites a visited one.
     */
    static int wmMarkSubTileOffsetVisitedFunc(int tile, int subtileX, int subtileY,
            int offsetX, int offsetY, SubtileState subtileState) {
        int actualSubtileX = subtileX + offsetX;
        int actualTile = tile;
        int actualSubtileY = subtileY + offsetY;

        if (actualSubtileX >= 0) {
            if (actualSubtileX >= SUBTILE_GRID_WIDTH) {
                if (tile % wmNumHorizontalTiles == wmNumHorizontalTiles - 1) {
                    return -1;
                }

                actualTile = tile + 1;
                actualSubtileX %= SUBTILE_GRID_WIDTH;
            }
        } else {
            if (tile % wmNumHorizontalTiles == 0) {
                return -1;
            }

            actualSubtileX += SUBTILE_GRID_WIDTH;
            actualTile = tile - 1;
        }

        if (actualSubtileY >= 0) {
            if (actualSubtileY >= SUBTILE_GRID_HEIGHT) {
                if (actualTile > wmMaxTileNum - wmNumHorizontalTiles - 1) {
                    return -1;
                }

                actualTile += wmNumHorizontalTiles;
                actualSubtileY %= SUBTILE_GRID_HEIGHT;
            }
        } else {
            if (actualTile < wmNumHorizontalTiles) {
                return -1;
            }

            actualSubtileY += SUBTILE_GRID_HEIGHT;
            actualTile -= wmNumHorizontalTiles;
        }

        SubtileInfo subtileInfo = tileInfoList.get(actualTile).subtiles[actualSubtileY][actualSubtileX];
        if (subtileState != SubtileState.SUBTILE_STATE_KNOWN
                || subtileInfo.state == SubtileState.SUBTILE_STATE_UNKNOWN) {
            subtileInfo.state = subtileState;
        }

        return 0;
    }

    /** Port of {@code wmMarkSubTileOffsetVisited} (0x4C3420). */
    static int wmMarkSubTileOffsetVisited(int tile, int subtileX, int subtileY,
            int offsetX, int offsetY) {
        return wmMarkSubTileOffsetVisitedFunc(tile, subtileX, subtileY,
            offsetX, offsetY, SubtileState.SUBTILE_STATE_VISITED);
    }

    /** Port of {@code wmMarkSubTileOffsetKnown} (0x4C0CA8). */
    static int wmMarkSubTileOffsetKnown(int tile, int subtileX, int subtileY,
            int offsetX, int offsetY) {
        return wmMarkSubTileOffsetVisitedFunc(tile, subtileX, subtileY,
            offsetX, offsetY, SubtileState.SUBTILE_STATE_KNOWN);
    }

    /**
     * Port of {@code wmSubTileMarkRadiusKnown} (0x4C35A8): mark the ring
     * of subtiles around a position known.
     */
    static void wmSubTileMarkRadiusKnown(int x, int y, int radius) {
        int tile = x / WM_TILE_WIDTH % wmNumHorizontalTiles
            + y / WM_TILE_HEIGHT * wmNumHorizontalTiles;
        int subtileX = x % WM_TILE_WIDTH / WM_SUBTILE_SIZE;
        int subtileY = y % WM_TILE_HEIGHT / WM_SUBTILE_SIZE;

        for (int offsetY = -radius; offsetY <= radius; offsetY++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                wmMarkSubTileOffsetKnown(tile, subtileX, subtileY, offsetX, offsetY);
            }
        }
    }

    /**
     * Port of {@code wmSubTileMarkRadiusVisited} (0x4C3534): mark the
     * ring of subtiles around a position known and the position itself
     * visited.
     *
     * <p>The original then walks the fill of the visited subtile - the
     * wedge shapes the map art draws - marking the wedge visited too. The
     * fill is not parsed (see the class notes), so the walk is dormant
     * and a no_fill subtile, which the vector probes, behaves the same.
     */
    public static int wmSubTileMarkRadiusVisited(int x, int y, int radius) {
        int tile = x / WM_TILE_WIDTH % wmNumHorizontalTiles
            + y / WM_TILE_HEIGHT * wmNumHorizontalTiles;
        int subtileX = x % WM_TILE_WIDTH / WM_SUBTILE_SIZE;
        int subtileY = y % WM_TILE_HEIGHT / WM_SUBTILE_SIZE;

        for (int offsetY = -radius; offsetY <= radius; offsetY++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                wmMarkSubTileOffsetKnown(tile, subtileX, subtileY, offsetX, offsetY);
            }
        }

        tileInfoList.get(tile).subtiles[subtileY][subtileX].state =
            SubtileState.SUBTILE_STATE_VISITED;

        return 0;
    }

    /**
     * Port of {@code wmMarkSubTileRadiusVisited} (0x4C35D8): mark around
     * the party's position, one subtile out - two with the Scout perk,
     * which the perk engine of the game phase owns and the port has no
     * rank for yet.
     */
    static void wmMarkSubTileRadiusVisited(int x, int y) {
        wmSubTileMarkRadiusVisited(x, y, perkHasRank(null, 0) ? 2 : 1);
    }

    /** Seam for {@code perkHasRank(gDude, PERK_SCOUT)}: no perks yet. */
    private static boolean perkHasRank(Object critter, int perk) {
        return false;
    }

    /** Test seam: fresh-process mirror (empty grid). */
    static void resetForTests() {
        tileInfoList.clear();
        wmMaxTileNum = 0;
        wmNumHorizontalTiles = 0;
    }
}
