package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * The light builtin of the game: the light of an object.
 *
 * <p>The intensity is a percentage of the full light, and a light of zero
 * puts the object out.
 */
class GameObjSetLightLevelTest {

    private GameScriptApi api;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        api = new GameScriptApi();
    }

    @Test
    void theLightOfAnObject() {
        GameObject object = new GameObject();

        api.objSetLightLevel(object, 3, 50);
        assertEquals(3, object.lightDistance);
        assertEquals((50 * 65536) / 100, object.lightIntensity);

        // A light of zero puts the object out.
        api.objSetLightLevel(object, 3, 0);
        assertEquals(0, object.lightDistance);
        assertEquals(0, object.lightIntensity);

        // And there is no light for no object.
        api.objSetLightLevel(null, 3, 50);
    }
}
