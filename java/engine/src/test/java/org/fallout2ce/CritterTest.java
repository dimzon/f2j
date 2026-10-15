package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.fallout2ce.Critter.RadiationLevel;

/**
 * {@code [gametypes]} critter vectors from
 * {@code docker/refharness/golden/v35.txt}.
 */
class CritterTest {

    @Test
    void radiationLevel() {
        assertEquals(6, Critter.RADIATION_LEVEL_COUNT);
        assertEquals(0, Critter.RADIATION_LEVEL_FIRST);
        // post-old=5 post-new=6 (past the end).
        assertEquals(RadiationLevel.RADIATION_LEVEL_FATAL, Critter.ofRadiationLevel(5));
        assertNull(Critter.ofRadiationLevel(5).next());
        assertEquals(RadiationLevel.RADIATION_LEVEL_MINOR, Critter.ofRadiationLevel(0).next());
        assertNull(Critter.ofRadiationLevel(6));
    }

    @Test
    void gorisFrameIds() {
        assertEquals(100, Critter.GORIS_COMBAT_BASE_FRAME_ID);
        assertEquals(99, Critter.GORIS_ROBE_BASE_FRAME_ID);
    }
}
