package org.fallout2ce;

/**
 * Hex-tile coordinate math slice, mirroring {@code src/tile.cc} /
 * {@code src/tile.h}: init/center/screen round-trip, rotation tables, tile
 * distance/direction/rotation/edge/square helpers.
 *
 * <p>Only the harness-vectored surface is ported ({@code [tilemath]} in
 * {@code docker/refharness/golden/v48.txt}, produced by
 * {@code sectionTileMath} in {@code docker/refharness/ref_main.cc}):
 * {@code tileInit} (tables, {@code _tile_mask} build, border compute, double
 * {@code tileSetCenter}), {@code tileSetCenter} ({@code IGNORE} path),
 * {@code tileToScreenXY}, {@code tileFromScreenXY}, {@code squareTileFromTile},
 * {@code tileDistanceBetween}, {@code tileIsInFrontOf},
 * {@code tileIsToRightOf}, {@code tileGetTileInDirection},
 * {@code tileGetRotationTo}, {@code tileNumBeyond} (C
 * {@code _tile_num_beyond}), {@code tileIsEdge}, plus
 * {@code tileReset}/{@code tileExit}.
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>{@code squareGrid} ({@code TileData**}) is accepted as
 * {@code Object} and stored but never read: verified by grep that nothing
 * in this slice touches {@code gTileSquares} (only floor/roof render
 * paths do, which are not ported).</li>
 * <li>Grid-outline pixel paints ({@code bufferFill}/{@code bufferDrawLine}
 * on {@code _tile_grid*}) are skipped: those buffers are only read by the
 * (unported) grid render paths, and their colors are runtime
 * {@code _colorTable} lookups, so they carry no vectored state.</li>
 * <li>Non-{@code IGNORE} {@code tileSetCenter} branches need
 * {@code gDude}, {@code _obj_scroll_blocking_at} and EDG edge state
 * (none ported) and throw {@link UnsupportedOperationException}.
 * The shared legacy-border check only touches ported state and is kept.</li>
 * <li>The {@code REFRESH} tail of {@code tileSetCenter} needs the render
 * pipeline (floor/roof/object draws) and likewise throws.</li>
 * <li>Map-edge queries are constant seams matching fresh-process C
 * defaults ({@code edgeDataLoaded=false} so {@code mapEdgeIsEnabled()}
 * is false; alignments are 0).</li>
 * <li>The stencil hook keeps C's early-return conditions
 * ({@code !enabled}, edge active, border uninitialized) and is otherwise
 * a no-op: the flood fill needs {@code _obj_scroll_blocking_at} (not
 * ported) and only feeds unvectored render caches. Java never prints
 * C's {@code tile_hires_stencil_init} stderr noise.</li>
 * <li>{@code gElevation} ({@code src/map.cc}, default 0) lives here until
 * the map module is ported.</li>
 * </ul>
 */
public final class TileMath {

    private TileMath() {
    }

    // tile.h constants.
    public static final int TILE_SET_CENTER_REFRESH_WINDOW = 0x01;
    public static final int TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS = 0x02;
    public static final int TILE_MAX_DISTANCE = 9999;

    // map.h original-resolution iso window (border math is 640x480-based).
    public static final int ORIGINAL_ISO_WINDOW_WIDTH = 640;
    public static final int ORIGINAL_ISO_WINDOW_HEIGHT = 380;

    // 0x51D96C off_tile (extern in tile.h; indexed by Rotation).
    public static final int[] OFF_TILE = { 16, 32, 16, -16, -32, -16 };

    // 0x51D984 companion rotation table (extern in tile.h; C has no
    // semantic name, so the address-derived name is kept verbatim).
    public static final int[] DWORD_51D984 = { -12, 0, 12, 12, 0, -12 };

    // 0x50E7C7 slope used by tileIsInFrontOf (exact bits: -4.0).
    private static final double DBL_50E7C7 = -4.0;

    // Slope used by tileIsToRightOf. The C comment calls this out: it
    // looks like 4/3 but its bit pattern (0x3FF55555555556) differs from
    // 4.0/3.0 by one ulp, which flips the famous 17488-vs-15288 vector.
    // The decimal literal below parses to those exact bits in Java too.
    private static final double TO_RIGHT_SLOPE = 1.3333333333333335;

    // tile.h TileWindowRefreshProc (C function pointer taking Rect*).
    public interface TileWindowRefreshProc {
        void refresh(Geometry.Rect rect);
    }

    // tile.h externs (fresh-process defaults: 0 / false).
    public static int gHexGridSize;
    public static int gCenterTile;
    public static boolean gTileBorderInitialized;
    public static int gTileBorderMinX;
    public static int gTileBorderMinY;
    public static int gTileBorderMaxX;
    public static int gTileBorderMaxY;

    // map.cc gElevation (default 0); parked here until map is ported.
    public static int gElevation;

    static int gHexGridWidth;
    static int gHexGridHeight;
    static int gSquareGridWidth;
    static int gSquareGridHeight;
    static int gSquareGridSize;
    static int gTileWindowWidth;
    static int gTileWindowHeight;
    static int gTileWindowPitch;
    static byte[] gTileWindowBuffer;
    static Object gTileSquares;
    static TileWindowRefreshProc gTileWindowRefreshProc;
    static Geometry.Rect gTileWindowRect = new Geometry.Rect();

    static int tileX;
    static int tileY;
    static int tileOffX;
    static int tileOffY;
    static int squareX;
    static int squareY;
    static int squareOffX;
    static int squareOffY;

    // 0x66B594 dir_tile / 0x668224-adjacent dir_tile2.
    static int[][] dirTile = new int[2][Rotation.COUNT];
    static int[][] dirTile2 = new int[2][Rotation.COUNT];

    // 0x66B9C4 tile_mask.
    static byte[] tileMask = new byte[512];

    static boolean tileEnabled = true;
    static boolean tileScrollBlockingEnabled = true;
    static boolean tileScrollLimitingEnabled = true;
    static boolean tileRoofIsVisible = true;
    static boolean tileGridIsVisible;
    static boolean tileWindowRefreshMapper;
    static boolean stencilEnabled;

    // Map-edge seams: fresh-process C defaults are edgeDataLoaded=false
    // (so mapEdgeIsEnabled() is false) and zero alignments.
    private static boolean mapEdgeIsEnabled() {
        return false;
    }

    private static int mapEdgeGetTileXAlignment() {
        return 0;
    }

    private static int mapEdgeGetTileYAlignment() {
        return 0;
    }

    // tile_hires_stencil.cc tile_hires_stencil_init body, minus the
    // debugPrint (Java must NOT reproduce the INFO stderr line).
    private static void tileHiresStencilInit() {
        stencilEnabled = !Settings.settings.system.executableIsMapper()
                && Settings.settings.ui.enable_high_resolution_stencil;
        if (!stencilEnabled) {
            return;
        }
        if (Settings.settings.ui.ignore_map_edges) {
            stencilEnabled = false;
        }
    }

    // tile_hires_stencil.cc
    // tile_hires_stencil_on_center_tile_or_elevation_change: only the
    // early-return conditions are ported (no crash + same vectored state);
    // the flood fill needs _obj_scroll_blocking_at (not ported).
    private static void tileHiresStencilOnCenterTileOrElevationChange() {
        if (!stencilEnabled) {
            return;
        }
        if (mapEdgeIsEnabled() || !gTileBorderInitialized) {
            return;
        }
        // SEAM: visited/visible flood fill skipped (render caches only).
    }

    // 0x4B0C40 tile_init. The squareGrid pointer has no Java TileData
    // counterpart yet; it is stored untyped (nothing in this slice reads
    // gTileSquares). Grid-outline paints are skipped (see class notes).
    public static int tileInit(Object squareGrid, int squareGridWidth, int squareGridHeight,
            int hexGridWidth, int hexGridHeight, byte[] buf,
            int windowWidth, int windowHeight, int windowPitch,
            TileWindowRefreshProc windowRefreshProc) {
        gSquareGridWidth = squareGridWidth;
        gTileSquares = squareGrid;
        gHexGridHeight = hexGridHeight;
        gSquareGridHeight = squareGridHeight;
        gHexGridWidth = hexGridWidth;
        dirTile[0][0] = -1;
        dirTile[0][4] = 1;
        dirTile[1][1] = -1;
        gHexGridSize = hexGridWidth * hexGridHeight;
        dirTile[1][3] = 1;
        gTileWindowBuffer = buf;
        dirTile2[0][0] = -1;
        gTileWindowWidth = windowWidth;
        dirTile2[0][3] = -1;
        gTileWindowHeight = windowHeight;
        dirTile2[1][1] = 1;
        gTileWindowPitch = windowPitch;
        dirTile2[1][2] = 1;
        gTileWindowRect.right = windowWidth - 1;
        gSquareGridSize = squareGridHeight * squareGridWidth;
        gTileWindowRect.bottom = windowHeight - 1;
        gTileWindowRect.left = 0;
        gTileWindowRefreshProc = windowRefreshProc;
        gTileWindowRect.top = 0;
        dirTile[0][1] = hexGridWidth - 1;
        dirTile[0][2] = hexGridWidth;
        tileGridIsVisible = false;
        dirTile[0][3] = hexGridWidth + 1;
        dirTile[1][2] = hexGridWidth;
        dirTile2[0][4] = hexGridWidth;
        dirTile2[0][5] = hexGridWidth;
        dirTile[0][5] = -hexGridWidth;
        dirTile[1][0] = -hexGridWidth - 1;
        dirTile[1][4] = 1 - hexGridWidth;
        dirTile[1][5] = -hexGridWidth;
        dirTile2[0][1] = -hexGridWidth - 1;
        dirTile2[1][4] = -hexGridWidth;
        dirTile2[0][2] = hexGridWidth - 1;
        dirTile2[1][5] = -hexGridWidth;
        dirTile2[1][0] = hexGridWidth + 1;
        dirTile2[1][3] = 1 - hexGridWidth;

        int v11;
        int v12 = 0;
        int v13;

        v11 = 0;
        v12 = 0;
        do {
            v13 = 64;
            do {
                tileMask[v12++] = (byte) (v13 > v11 ? 1 : 0);
                v13 -= 4;
            } while (v13 != 0);

            do {
                tileMask[v12++] = (byte) (v13 > v11 ? 2 : 0);
                v13 += 4;
            } while (v13 != 64);

            v11 += 16;
        } while (v11 != 64);

        v11 = 0;
        do {
            v13 = 0;
            do {
                tileMask[v12++] = 0;
                v13++;
            } while (v13 < 32);
            v11++;
        } while (v11 < 8);

        v11 = 0;
        do {
            v13 = 0;
            do {
                tileMask[v12++] = (byte) (v13 > v11 ? 0 : 3);
                v13 += 4;
            } while (v13 != 64);

            v13 = 64;
            do {
                tileMask[v12++] = (byte) (v13 > v11 ? 0 : 4);
                v13 -= 4;
            } while (v13 != 0);

            v11 += 16;
        } while (v11 != 64);

        // NOTE: _tile_grid/_tile_grid_occupied/_tile_grid_blocked outline
        // paints + the blocked-row scan fills are skipped (render-only,
        // runtime color-table colors; see class notes).

        // Border math pretends to be at original resolution (computed once).
        gTileWindowWidth = ORIGINAL_ISO_WINDOW_WIDTH;
        gTileWindowHeight = ORIGINAL_ISO_WINDOW_HEIGHT;

        tileHiresStencilInit();

        tileSetCenter(hexGridWidth * (hexGridHeight / 2) + hexGridWidth / 2,
                TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS);
        tileSetBorder(windowWidth, windowHeight, hexGridWidth, hexGridHeight);

        // Restore the real window size and re-center for correct offsets.
        gTileWindowWidth = windowWidth;
        gTileWindowHeight = windowHeight;

        tileSetCenter(hexGridWidth * (hexGridHeight / 2) + hexGridWidth / 2,
                TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS);

        if (Settings.settings.system.executableIsMapper()) {
            tileWindowRefreshMapper = true;
        }

        return 0;
    }

    // 0x4B11E4 tile_set_border.
    private static void tileSetBorder(int windowWidth, int windowHeight, int hexGridWidth, int hexGridHeight) {
        int v1 = tileFromScreenXY(-320, -240);
        int v2 = tileFromScreenXY(-320, ORIGINAL_ISO_WINDOW_HEIGHT + 240);

        gTileBorderMinX = Math.abs(hexGridWidth - 1 - v2 % hexGridWidth - tileX) + 6;
        gTileBorderMinY = Math.abs(tileY - v1 / hexGridWidth) + 7;
        gTileBorderMaxX = hexGridWidth - gTileBorderMinX - 1;
        gTileBorderMaxY = hexGridHeight - gTileBorderMinY - 1;

        if ((gTileBorderMinX & 1) == 0) {
            gTileBorderMinX++;
        }

        if ((gTileBorderMaxX & 1) == 0) {
            gTileBorderMinX--;
        }

        gTileBorderInitialized = true;
    }

    // NOTE: Collapsed 0x4B129C _tile_reset_ (empty in C).
    private static void tileReset_() {
    }

    // NOTE: Uncollapsed 0x4B129C tile_reset (delegates, like C).
    public static void tileReset() {
        tileReset_();
    }

    // NOTE: Uncollapsed 0x4B129C tile_exit. C's body only calls the empty
    // _tile_reset_ (all state is static, nothing heap-owned in this slice);
    // the JVM equivalent of "free" is dropping the static roots, so exit
    // resets to fresh-process state (same as resetForTests).
    public static void tileExit() {
        tileReset_();
        clearState();
    }

    public static void tileDisable() {
        tileEnabled = false;
    }

    public static void tileEnable() {
        tileEnabled = true;
    }

    // 0x4B12F8 tile_set_center. Only the IGNORE path is ported (vectors
    // use IGNORE exclusively); map-dependent branches throw (see notes).
    public static int tileSetCenter(int tile, int flags) {
        if (!tileIsValid(tile)) {
            return -1;
        }

        if ((flags & TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS) == 0) {
            throw new UnsupportedOperationException(
                    "tileSetCenter without IGNORE needs gDude/_obj_scroll_blocking_at/EDG (not ported)");
        }

        int centerX = gHexGridWidth - 1 - tile % gHexGridWidth;
        int centerY = tile / gHexGridWidth;

        // Legacy global borders are for maps without EDG data; the edge
        // seam is disabled, so (!edgeActive || !zoneSelected) is true.
        if (gTileBorderInitialized && tileScrollBlockingEnabled) {
            if (centerX <= gTileBorderMinX || centerX >= gTileBorderMaxX
                    || centerY <= gTileBorderMinY || centerY >= gTileBorderMaxY) {
                return -1;
            }
        }

        tileY = centerY;
        tileOffX = mapEdgeGetTileXAlignment() + (gTileWindowWidth - 32) / 2;
        tileX = centerX;
        tileOffY = mapEdgeGetTileYAlignment() + (gTileWindowHeight - 16) / 2;

        if ((centerX & 1) != 0) {
            tileX -= 1;
            tileOffX -= 32;
        }

        squareX = tileX / 2;
        squareY = tileY / 2;
        squareOffX = tileOffX - 16;
        squareOffY = tileOffY - 2;

        if ((tileY & 1) != 0) {
            squareOffY -= 12;
            squareOffX -= 16;
        }

        gCenterTile = tile;

        tileHiresStencilOnCenterTileOrElevationChange();

        if ((flags & TILE_SET_CENTER_REFRESH_WINDOW) != 0) {
            throw new UnsupportedOperationException(
                    "tileSetCenter refresh needs the render pipeline (not ported)");
        }

        return 0;
    }

    // 0x4B1674 tile_coord. Out params are length-1 int arrays (repo
    // convention for C pointer outs); caller-owned, zeroed first like C.
    public static int tileToScreenXY(int tile, int[] screenX, int[] screenY) {
        screenX[0] = 0;
        screenY[0] = 0;

        if (!tileIsValid(tile)) {
            return -1;
        }

        int v3 = gHexGridWidth - 1 - tile % gHexGridWidth;
        int v4 = tile / gHexGridWidth;

        screenX[0] = tileOffX;
        screenY[0] = tileOffY;

        int v5 = (v3 - tileX) / -2;
        screenX[0] += 48 * ((v3 - tileX) / 2);
        screenY[0] += 12 * v5;

        if ((v3 & 1) != 0) {
            if (v3 <= tileX) {
                screenX[0] -= 16;
                screenY[0] += 12;
            } else {
                screenX[0] += 32;
            }
        }

        int v6 = v4 - tileY;
        screenX[0] += 16 * v6;
        screenY[0] += 12 * v6;

        return 0;
    }

    // 0x4B1754 tile_num (CE ignoreBounds variant).
    public static int tileFromScreenXY(int screenX, int screenY) {
        return tileFromScreenXY(screenX, screenY, false);
    }

    public static int tileFromScreenXY(int screenX, int screenY, boolean ignoreBounds) {
        // NOTE: Java / and % truncate toward zero like C, so the
        // negative-side (x + 1) / n - 1 formulations port verbatim.
        int yTileOff = screenY - tileOffY;
        int y;
        if (yTileOff >= 0) {
            y = yTileOff / 12;
        } else {
            y = (yTileOff + 1) / 12 - 1;
        }

        int xTileOff = screenX - tileOffX - 16 * y;
        int yOffset = yTileOff - y * 12;

        int x;
        if (xTileOff >= 0) {
            x = xTileOff / 64;
        } else {
            x = (xTileOff + 1) / 64 - 1;
        }

        int xOffset = xTileOff - x * 64;

        int tY = x + y;
        int tX = 2 * x;

        if (xOffset >= 32) {
            xOffset -= 32;
            tX++;
        }

        int xTile = tileX + tX;
        int yTile = tileY + tY;

        switch (tileMask[32 * yOffset + xOffset]) {
        case 1:
            yTile--;
            break;
        case 2:
            xTile++;
            if ((xTile & 1) != 0) {
                yTile--;
            }
            break;
        case 3:
            xTile--;
            if ((xTile & 1) == 0) {
                yTile++;
            }
            break;
        case 4:
            yTile++;
            break;
        default:
            break;
        }

        int xPos = gHexGridWidth - 1 - xTile;
        if (ignoreBounds
                || (xPos >= 0 && xPos < gHexGridWidth && yTile >= 0 && yTile < gHexGridHeight)) {
            return gHexGridWidth * yTile + xPos;
        } else {
            return -1;
        }
    }

    // squareTileFromTile (no C address tag upstream of the reader loop).
    public static int squareTileFromTile(int tile) {
        if (!tileIsValid(tile)) {
            return -1;
        }

        int tileCol = gHexGridWidth - 1 - tile % gHexGridWidth;
        int tileRow = tile / gHexGridWidth;
        int squareCol = tileCol / 2;
        int squareRow = tileRow / 2;

        return gSquareGridWidth - 1 - squareCol + gSquareGridWidth * squareRow;
    }

    // 0x4B1DC8 square_coord (elevation ignored, like C). Out params are
    // length-1 int arrays (repo convention for C pointer outs).
    public static int squareTileToScreenXY(int squareTile, int[] coordX, int[] coordY, int elevation) {
        coordX[0] = 0;
        coordY[0] = 0;

        if (squareTile < 0 || squareTile >= gSquareGridSize) {
            return -1;
        }

        int tileCol = gSquareGridWidth - 1 - squareTile % gSquareGridWidth;
        int tileRow = squareTile / gSquareGridWidth;

        coordX[0] = squareOffX;
        coordY[0] = squareOffY;

        int colDelta = tileCol - squareX;
        coordX[0] += 48 * colDelta;
        coordY[0] -= 12 * colDelta;

        int rowDelta = tileRow - squareY;
        coordX[0] += 32 * rowDelta;
        coordY[0] += 24 * rowDelta;

        return 0;
    }

    // 0x4B1E60 square_coord_roof (elevation ignored, like C; the -96 roof
    // shift is applied after the identical floor math).
    public static int squareTileToRoofScreenXY(int squareTile, int[] screenX, int[] screenY, int elevation) {
        screenX[0] = 0;
        screenY[0] = 0;

        if (squareTile < 0 || squareTile >= gSquareGridSize) {
            return -1;
        }

        int tileCol = gSquareGridWidth - 1 - squareTile % gSquareGridWidth;
        int tileRow = squareTile / gSquareGridWidth;

        screenX[0] = squareOffX;
        screenY[0] = squareOffY;

        int colDelta = tileCol - squareX;
        screenX[0] += 48 * colDelta;
        screenY[0] -= 12 * colDelta;

        int rowDelta = tileRow - squareY;
        screenX[0] += 32 * rowDelta;
        screenY[0] += 24 * rowDelta;
        screenY[0] -= 96;

        return 0;
    }

    // 0x4B1F04 square_num (elevation ignored, like C; bounds gate
    // returning -1).
    public static int squareTileFromScreenXY(int screenX, int screenY, int elevation) {
        int[] coordX = new int[1];
        int[] coordY = new int[1];

        squareTileScreenToCoord(screenX, screenY, elevation, coordX, coordY);

        if (coordX[0] >= 0 && coordX[0] < gSquareGridWidth
                && coordY[0] >= 0 && coordY[0] < gSquareGridHeight) {
            return coordX[0] + gSquareGridWidth * coordY[0];
        }

        return -1;
    }

    // 0x4B1F94 square_xy (elevation ignored, like C). NOTE: Java / and %
    // truncate toward zero like C, so the (v + 1) / n - 1 negative forms
    // port verbatim; the final coordX mirror is width - 1 - coordX.
    public static void squareTileScreenToCoord(int screenX, int screenY, int elevation,
            int[] coordX, int[] coordY) {
        int v4 = screenX - squareOffX;
        int v5 = screenY - squareOffY - 12;
        int v6 = 3 * v4 - 4 * v5;
        coordX[0] = v6 >= 0 ? (v6 / 192) : ((v6 + 1) / 192 - 1);

        int v8 = 4 * v5 + v4;
        coordY[0] = v8 >= 0 ? (v8 / 128) : ((v8 + 1) / 128 - 1);

        coordX[0] += squareX;
        coordY[0] += squareY;

        coordX[0] = gSquareGridWidth - 1 - coordX[0];
    }

    // 0x4B203C square_xy_roof (elevation ignored, like C; +96 roof shift
    // folded into v5, otherwise identical to square_xy).
    public static void squareTileScreenToCoordRoof(int screenX, int screenY, int elevation,
            int[] coordX, int[] coordY) {
        int v4 = screenX - squareOffX;
        int v5 = screenY + 96 - squareOffY - 12;
        int v6 = 3 * v4 - 4 * v5;
        coordX[0] = v6 >= 0 ? (v6 / 192) : ((v6 + 1) / 192 - 1);

        int v8 = 4 * v5 + v4;
        coordY[0] = v8 >= 0 ? (v8 / 128) : ((v8 + 1) / 128 - 1);

        coordX[0] += squareX;
        coordY[0] += squareY;

        coordX[0] = gSquareGridWidth - 1 - coordX[0];
    }

    // 0x4B185C tile_dist.
    public static int tileDistanceBetween(int tile1, int tile2) {
        if (!tileIsValid(tile1) || !tileIsValid(tile2)) {
            return TILE_MAX_DISTANCE;
        }

        int distance = 0;
        int curTile = tile1;
        while (curTile != tile2 && distance < TILE_MAX_DISTANCE) {
            int dir = tileGetRotationTo(curTile, tile2).value();
            curTile += dirTile[(curTile % gHexGridWidth) & 1][dir];
            distance++;
        }

        return distance;
    }

    // 0x4B1994 tile_in_front_of.
    public static boolean tileIsInFrontOf(int tile1, int tile2) {
        int[] x1 = new int[1];
        int[] y1 = new int[1];
        tileToScreenXY(tile1, x1, y1);

        int[] x2 = new int[1];
        int[] y2 = new int[1];
        tileToScreenXY(tile2, x2, y2);

        int dx = x2[0] - x1[0];
        int dy = y2[0] - y1[0];

        return (double) dx <= (double) dy * DBL_50E7C7;
    }

    // 0x4B1A00 tile_to_right_of (exact 1.3333333333333335 bits; see note).
    public static boolean tileIsToRightOf(int tile1, int tile2) {
        int[] x1 = new int[1];
        int[] y1 = new int[1];
        tileToScreenXY(tile1, x1, y1);

        int[] x2 = new int[1];
        int[] y2 = new int[1];
        tileToScreenXY(tile2, x2, y2);

        int dx = x2[0] - x1[0];
        int dy = y2[0] - y1[0];

        return (double) dx <= (double) dy * TO_RIGHT_SLOPE;
    }

    // 0x4B1A6C tile_num_in_direction.
    public static int tileGetTileInDirection(int tile, Rotation rotation, int distance) {
        int newTile = tile;
        for (int index = 0; index < distance; index++) {
            if (tileIsEdge(newTile)) {
                break;
            }

            int parity = (newTile % gHexGridWidth) & 1;
            newTile += dirTile[parity][rotation.value()];
        }

        return newTile;
    }

    // 0x4B1ABC tile_dir.
    public static Rotation tileGetRotationTo(int tile1, int tile2) {
        int[] x1 = new int[1];
        int[] y1 = new int[1];
        tileToScreenXY(tile1, x1, y1);

        int[] x2 = new int[1];
        int[] y2 = new int[1];
        tileToScreenXY(tile2, x2, y2);

        int dy = y2[0] - y1[0];
        int dx = x2[0] - x1[0];

        if (dx != 0) {
            // C trunc() toward zero == Java (int) cast for this +-180 range.
            int raw = (int) (Math.atan2((double) -dy, (double) dx) * 180.0 / Math.PI);
            int angle = 360 - (raw + 180) - 90;
            if (angle < 0) {
                angle += 360;
            }

            angle /= 60;

            if (angle >= Rotation.COUNT) {
                angle = Rotation.NW.value();
            }
            return Rotation.fromInt(angle);
        }

        return dy < 0 ? Rotation.NE : Rotation.SE;
    }

    // 0x4B1B84 tile_num_beyond (C _tile_num_beyond; leading underscore
    // dropped per repo convention).
    public static int tileNumBeyond(int from, int to, int distance) {
        if (distance <= 0 || from == to) {
            return from;
        }

        int[] fromX = new int[1];
        int[] fromY = new int[1];
        tileToScreenXY(from, fromX, fromY);
        int tileX0 = fromX[0] + 16;
        int tileY0 = fromY[0] + 8;

        int[] toX = new int[1];
        int[] toY = new int[1];
        tileToScreenXY(to, toX, toY);
        int targetX = toX[0] + 16;
        int targetY = toY[0] + 8;

        int deltaX = targetX - tileX0;
        int deltaY = targetY - tileY0;

        int v27 = 2 * Math.abs(deltaX);

        int stepX = 0;
        if (deltaX > 0) {
            stepX = 1;
        } else if (deltaX < 0) {
            stepX = -1;
        }

        int v26 = 2 * Math.abs(deltaY);

        int stepY = 0;
        if (deltaY > 0) {
            stepY = 1;
        } else if (deltaY < 0) {
            stepY = -1;
        }

        int v28 = from;
        int curX = tileX0;
        int curY = tileY0;

        int v6 = 0;

        if (v27 > v26) {
            int middle = v26 - v27 / 2;
            while (true) {
                int tile = tileFromScreenXY(curX, curY);
                if (tile != v28) {
                    v6 += 1;
                    if (v6 == distance || tileIsEdge(tile)) {
                        return tile;
                    }

                    v28 = tile;
                }

                if (middle >= 0) {
                    middle -= v27;
                    curY += stepY;
                }

                middle += v26;
                curX += stepX;
            }
        } else {
            int middle = v27 - v26 / 2;
            while (true) {
                int tile = tileFromScreenXY(curX, curY);
                if (tile != v28) {
                    v6 += 1;
                    if (v6 == distance || tileIsEdge(tile)) {
                        return tile;
                    }

                    v28 = tile;
                }

                if (middle >= 0) {
                    middle -= v26;
                    curX += stepX;
                }

                middle += v27;
                curY += stepY;
            }
        }

        // C assert(false): provably unreachable (loops only exit via
        // return once the walk leaves the grid and tileFromScreenXY
        // yields -1/edge); kept as an explicit throw.
        // throw new AssertionError("tileNumBeyond: unreachable");
    }

    // 0x4B1D20 tile_on_edge.
    public static boolean tileIsEdge(int tile) {
        if (!tileIsValid(tile)) {
            return false;
        }

        if (tile < gHexGridWidth) {
            return true;
        }

        if (tile >= gHexGridSize - gHexGridWidth) {
            return true;
        }

        if (tile % gHexGridWidth == 0) {
            return true;
        }

        if (tile % gHexGridWidth == gHexGridWidth - 1) {
            return true;
        }

        return false;
    }

    public static void tileScrollBlockingEnable() {
        tileScrollBlockingEnabled = true;
    }

    public static void tileScrollBlockingDisable() {
        tileScrollBlockingEnabled = false;
    }

    public static boolean tileScrollBlockingIsEnabled() {
        return tileScrollBlockingEnabled;
    }

    public static void tileScrollLimitingEnable() {
        tileScrollLimitingEnabled = true;
    }

    public static void tileScrollLimitingDisable() {
        tileScrollLimitingEnabled = false;
    }

    public static boolean tileScrollLimitingIsEnabled() {
        return tileScrollLimitingEnabled;
    }

    // tile.h tileIsValid inline.
    public static boolean tileIsValid(int tile) {
        return tile >= 0 && tile < gHexGridSize;
    }

    private static void clearState() {
        gHexGridSize = 0;
        gCenterTile = 0;
        gTileBorderInitialized = false;
        gTileBorderMinX = 0;
        gTileBorderMinY = 0;
        gTileBorderMaxX = 0;
        gTileBorderMaxY = 0;
        gElevation = 0;
        gHexGridWidth = 0;
        gHexGridHeight = 0;
        gSquareGridWidth = 0;
        gSquareGridHeight = 0;
        gSquareGridSize = 0;
        gTileWindowWidth = 0;
        gTileWindowHeight = 0;
        gTileWindowPitch = 0;
        gTileWindowBuffer = null;
        gTileSquares = null;
        gTileWindowRefreshProc = null;
        gTileWindowRect = new Geometry.Rect();
        tileX = 0;
        tileY = 0;
        tileOffX = 0;
        tileOffY = 0;
        squareX = 0;
        squareY = 0;
        squareOffX = 0;
        squareOffY = 0;
        dirTile = new int[2][Rotation.COUNT];
        dirTile2 = new int[2][Rotation.COUNT];
        tileMask = new byte[512];
        tileEnabled = true;
        tileScrollBlockingEnabled = true;
        tileScrollLimitingEnabled = true;
        tileRoofIsVisible = true;
        tileGridIsVisible = false;
        tileWindowRefreshMapper = false;
        stencilEnabled = false;
    }

    // 0x4B1234 tileWindowRefresh: the window rect goes to the registered
    // refresher when tiles are enabled (the elevation arg is dropped from
    // the Java proc, like the other render-phase trims).
    public static void tileWindowRefresh() {
        if (tileEnabled) {
            gTileWindowRefreshProc.refresh(gTileWindowRect);
        }
    }

    // 0x4B1214 tileWindowRefreshRect (rect overload kept: callers with
    // render-derived rects pass null, like the other deferred rect seams;
    // the test refresher ignores its argument).
    public static void tileWindowRefreshRect(Geometry.Rect rect, int elevation) {
        if (tileEnabled) {
            if (elevation == Map.gElevation) {
                gTileWindowRefreshProc.refresh(rect);
            }
        }
    }

    /** Test seam: full state reset (fresh-process mirror). */
    static void resetForTests() {
        clearState();
    }
}
