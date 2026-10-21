package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The direction builtin of the game.
 *
 * <p>The direction is the number the game gives it: north-east is zero, and
 * the way back is south-west. The grid is the one the reference harness
 * uses ({@code [tilemath]} in the golden vectors).
 */
class GameRotationToTileTest {

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
    void theDirectionToATile() {
        // One step north-east away from the tile 17488 is the tile 17487.
        assertEquals(Rotation.NE.value(), api.rotationToTile(17488, 17487));

        // And the way back is south-west.
        assertEquals(Rotation.SW.value(), api.rotationToTile(17487, 17488));
    }
}
