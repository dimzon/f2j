package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The roll builtins of the game: whether a roll was a success and whether
 * it was a critical one.
 *
 * <p>The roll is the number the game gives it, and a roll that is none of
 * the four the game knows has no answer.
 */
class GameRollTest {

    private GameScriptApi api;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        api = new GameScriptApi();
    }

    @Test
    void whetherARollWasASuccess() {
        // The roll of a critical failure is 0, a failure is 1, a success is
        // 2 and a critical success is 3.
        assertEquals(0, api.success(0));
        assertEquals(0, api.success(1));
        assertEquals(1, api.success(2));
        assertEquals(1, api.success(3));

        // A roll the game does not know has no answer.
        assertEquals(-1, api.success(4));
    }

    @Test
    void whetherARollWasCritical() {
        assertEquals(1, api.critical(0));
        assertEquals(0, api.critical(1));
        assertEquals(0, api.critical(2));
        assertEquals(1, api.critical(3));

        // A roll the game does not know has no answer.
        assertEquals(-1, api.critical(4));
    }
}
