package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.fallout2ce.AnimationDefs.AnimationRequestOptions;

/**
 * {@code [gamedefs]} animation vectors from
 * {@code docker/refharness/golden/v29.txt}.
 */
class AnimationDefsTest {

    @Test
    void countsAndSentinels() {
        assertEquals(65, AnimationDefs.ANIM_COUNT);
        assertEquals(0, AnimationDefs.ANIM_FIRST);
        assertEquals(0, AnimationDefs.AnimationType.ANIM_STAND.id());
        assertEquals(47, AnimationDefs.AnimationType.ANIM_FIRE_CONTINUOUS.id());
        assertEquals(64, AnimationDefs.AnimationType.ANIM_CALLED_SHOT_PIC.id());
        assertEquals(20, AnimationDefs.FIRST_KNOCKDOWN_AND_DEATH_ANIM);
        assertEquals(35, AnimationDefs.LAST_KNOCKDOWN_AND_DEATH_ANIM);
        assertEquals(48, AnimationDefs.FIRST_SF_DEATH_ANIM);
        assertEquals(63, AnimationDefs.LAST_SF_DEATH_ANIM);
    }

    @Test
    void fullTableIsDense() {
        for (int id = 0; id < AnimationDefs.ANIM_COUNT; id++) {
            AnimationDefs.AnimationType anim = AnimationDefs.of(id);
            assertTrue(anim != null, "missing anim id " + id);
            assertEquals(id, anim.id());
        }
        assertEquals(AnimationDefs.AnimationType.ANIM_INVALID, AnimationDefs.of(-1));
        assertNull(AnimationDefs.of(65));
    }

    @Test
    void validators() {
        assertFalse(AnimationDefs.animationTypeIsValid(-1));
        assertTrue(AnimationDefs.animationTypeIsValid(AnimationDefs.ANIM_COUNT - 1));
        assertFalse(AnimationDefs.animationTypeIsValid(AnimationDefs.ANIM_COUNT));
    }

    @Test
    void requestOptions() {
        // UNRESERVED | RESERVED | NO_STAND -> opts-std=7.
        AnimationRequestOptions opts = AnimationRequestOptions.UNRESERVED
                .or_(AnimationRequestOptions.RESERVED)
                .or_(AnimationRequestOptions.NO_STAND);
        assertEquals(7, opts.bits());

        // PING | INSIGNIFICANT -> opts-ping=768 (0x300).
        assertEquals(768, AnimationRequestOptions.PING.or_(AnimationRequestOptions.INSIGNIFICANT).bits());
    }
}
