package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * The player builtin of the game.
 *
 * <p>The player is the object the game made when the game started, and the
 * port has no game phase yet, so there is no player until the game makes
 * one.
 */
class GameGetDudeTest {

    private GameScriptApi api;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        ObjectSystem.resetForTests();
        api = new GameScriptApi();
    }

    @AfterEach
    void teardown() {
        ObjectSystem.gDude = null;
    }

    @Test
    void thePlayerOfTheGame() {
        // There is no player before the game starts.
        assertNull(api.opGetDude());

        // And the game player is the one the scripts get.
        GameObject dude = new GameObject();
        ObjectSystem.gDude = dude;
        assertSame(dude, api.opGetDude());
    }
}
