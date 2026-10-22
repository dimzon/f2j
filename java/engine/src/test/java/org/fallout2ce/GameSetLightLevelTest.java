package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The ambient light builtin of the game.
 *
 * <p>The level is a percentage: 50 is the middle of the range, the top of
 * the range is 100 and the bottom is 0.
 */
class GameSetLightLevelTest {

    private GameScriptApi api;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Light.lightReset();
        api = new GameScriptApi();
    }

    @AfterEach
    void teardown() {
        Light.lightReset();
    }

    @Test
    void theAmbientLightOfTheMap() {
        int min = Light.LIGHT_INTENSITY_MIN;
        int max = Light.LIGHT_INTENSITY_MAX;
        int middle = (min + max) / 2;

        // The middle of the range.
        api.setLightLevel(50);
        assertEquals(middle, Light.lightGetAmbientIntensity());

        // The top of the range.
        api.setLightLevel(100);
        assertEquals(max, Light.lightGetAmbientIntensity());

        // The bottom of the range.
        api.setLightLevel(0);
        assertEquals(min, Light.lightGetAmbientIntensity());

        // And a level in between is spread over the half it is in.
        api.setLightLevel(25);
        assertEquals(min + 25 * (middle - min) / 100, Light.lightGetAmbientIntensity());
        api.setLightLevel(75);
        assertEquals(middle + 75 * (max - middle) / 100, Light.lightGetAmbientIntensity());
    }
}
