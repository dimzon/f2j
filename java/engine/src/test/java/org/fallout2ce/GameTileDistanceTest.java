package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The tile distance builtins of the game.
 *
 * <p>The numbers are the ones the reference harness prints: the distance
 * between the tiles 17488 and 15288 is 11, a tile has no distance to
 * itself but zero, and a tile that is not on the grid has no distance at
 * all ({@code [tilemath]} in the golden vectors).
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
}
