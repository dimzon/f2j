package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [tilemath]} vectors from
 * {@code docker/refharness/golden/v48.txt} (produced by
 * {@code sectionTileMath}: headless init with 200x200 hex / 100x100 square
 * grids, then {@code tileSetCenter(17488, IGNORE)}). Pure math + init, no
 * game data needed.
 *
 * <p>Note {@code back=17288}: the tile-center screen point maps to the
 * NEIGHBOR tile through the {@code _tile_mask} switch. This quirk is
 * asserted as-is (reproduced, not "fixed").
 */
class TileMathTest {

    @BeforeEach
    void setup() {
        // Pin the Settings defaults the harness observes (fresh process +
        // settingsInit defaults: not-mapper, stencil on, edges honored).
        Settings.resetStateForTests();
        TileMath.resetForTests();
    }

    @AfterEach
    void teardown() {
        TileMath.tileExit();
    }

    @Test
    void tileMathVectors() {
        // C Rotation order guard: the tables and angle math index by it.
        assertEquals(0, Rotation.NE.value());
        assertEquals(1, Rotation.E.value());
        assertEquals(2, Rotation.SE.value());
        assertEquals(3, Rotation.SW.value());
        assertEquals(4, Rotation.W.value());
        assertEquals(5, Rotation.NW.value());

        byte[] buf = new byte[640 * 480];
        int initRc = TileMath.tileInit(null, 100, 100, 200, 200, buf, 640, 480, 640, rect -> {
        });
        int centerAfterInit = TileMath.gCenterTile;
        int setRc = TileMath.tileSetCenter(17488,
                TileMath.TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS);
        int centerAfterSet = TileMath.gCenterTile;
        assertEquals(0, initRc);
        assertEquals(20100, centerAfterInit);
        assertEquals(0, setRc);
        assertEquals(17488, centerAfterSet);

        assertArrayEquals(new int[] { 16, 32, 16, -16, -32, -16 }, TileMath.OFF_TILE);
        assertArrayEquals(new int[] { -12, 0, 12, 12, 0, -12 }, TileMath.DWORD_51D984);

        assertEquals(11, TileMath.tileDistanceBetween(17488, 15288));
        assertEquals(0, TileMath.tileDistanceBetween(17488, 17488));
        assertEquals(9999, TileMath.tileDistanceBetween(-1, 17488));
        assertTrue(TileMath.tileIsInFrontOf(17488, 15288));
        assertFalse(TileMath.tileIsToRightOf(17488, 15288));
        assertEquals(Rotation.NW, TileMath.tileGetRotationTo(17488, 15288));
        assertEquals(5, TileMath.tileGetRotationTo(17488, 15288).value());

        assertEquals(17487, TileMath.tileGetTileInDirection(17488, Rotation.NE, 1));
        assertEquals(17285, TileMath.tileGetTileInDirection(17488, Rotation.NE, 3));
        assertEquals(17088, TileMath.tileNumBeyond(17488, 15288, 2));
        assertEquals(17488, TileMath.tileNumBeyond(17488, 15288, 0));

        assertTrue(TileMath.tileIsEdge(0));
        assertTrue(TileMath.tileIsEdge(199));
        assertFalse(TileMath.tileIsEdge(20100));
        assertFalse(TileMath.tileIsEdge(-1));
        assertEquals(5050, TileMath.squareTileFromTile(20100));
        assertEquals(-1, TileMath.squareTileFromTile(-1));

        int[] sx = new int[1];
        int[] sy = new int[1];
        assertEquals(0, TileMath.tileToScreenXY(17488, sx, sy));
        assertEquals(17288, TileMath.tileFromScreenXY(sx[0], sy[0]));
        assertEquals(-1, TileMath.tileToScreenXY(-1, sx, sy));
        assertEquals(-1, TileMath.tileFromScreenXY(-1000, -1000));
    }
}
