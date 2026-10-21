package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The source builtin of the game.
 *
 * <p>The source is not the object the script runs for, but the one that
 * started it. The scripts of the game are started by the game itself and
 * have no source, so the original answers the same in its harness.
 */
class GameSourceObjTest {

    private GameScriptApi api;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        api = new GameScriptApi();
    }

    @Test
    void thereIsNoSource() {
        assertNull(api.sourceObj());
    }
}
