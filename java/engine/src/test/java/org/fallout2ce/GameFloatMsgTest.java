package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The floating text builtin of the game.
 *
 * <p>The text is a floating one that the render engine draws over the
 * object, and the port has no render engine yet, so the text is not shown.
 */
class GameFloatMsgTest {

    private GameScriptApi api;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        api = new GameScriptApi();
    }

    @Test
    void theTextIsNotShown() {
        Program.ProgramValue value = new Program.ProgramValue();
        value.pointerValue = "Hello";

        assertDoesNotThrow(() -> api.floatMsg(value, 0));
    }
}
