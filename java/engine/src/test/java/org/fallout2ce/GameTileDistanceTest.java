package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The tile builtins of the game: the distance between two tiles and the
 * tile that lies a step away.
 *
 * <p>The numbers are the ones the reference harness prints: the distance
 * between the tiles 17488 and 15288 is 11, a tile has no distance to
 * itself but zero, and a tile that is not on the grid has no distance at
 * all. One step north-east from the tile 17488 is the tile 17487, and
 * three steps are 17285 ({@code [tilemath]} in the golden vectors).
 */
class GameTileDistanceTest {

    private GameScriptApi api;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        TileMath.resetForTests();
        byte[] buf = new byte[640 * 480];
        TileMath.tileInit(null, 100, 100, 200, 200, buf, 640, 480, 640, rect -> {
        });
        api = new GameScriptApi();
    }

    @AfterEach
    void teardown() {
        TileMath.tileExit();
    }

    @Test
    void theDistanceBetweenTwoTiles() {
        assertEquals(11, api.tileDistance(17488, 15288));
        assertEquals(0, api.tileDistance(17488, 17488));
        assertEquals(TileMath.TILE_MAX_DISTANCE, api.tileDistance(-1, 17488));
    }

    @Test
    void theDistanceBetweenTwoObjects() {
        ObjTypes.GameObject first = new ObjTypes.GameObject();
        first.tile = 17488;
        first.elevation = 0;
        ObjTypes.GameObject second = new ObjTypes.GameObject();
        second.tile = 15288;
        second.elevation = 0;

        assertEquals(11, api.tileDistanceObjs(first, second));

        // Objects on different elevations are as far apart as the grid goes.
        second.elevation = 1;
        assertEquals(TileMath.TILE_MAX_DISTANCE, api.tileDistanceObjs(first, second));

        // And an object that is not there has no distance either.
        assertEquals(TileMath.TILE_MAX_DISTANCE, api.tileDistanceObjs(first, null));
        assertEquals(TileMath.TILE_MAX_DISTANCE, api.tileDistanceObjs(null, second));
    }

    @Test
    void theTileAStepAway() {
        assertEquals(17487, api.tileNumInDirection(17488, Rotation.NE.value(), 1));
        assertEquals(17285, api.tileNumInDirection(17488, Rotation.NE.value(), 3));

        // A direction that is not one of the six has nowhere to go.
        assertEquals(17488, api.tileNumInDirection(17488, 6, 1));
    }
}
