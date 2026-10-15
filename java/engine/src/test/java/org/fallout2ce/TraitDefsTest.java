package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@code [gamedefs]} trait vectors from
 * {@code docker/refharness/golden/v29.txt}.
 */
class TraitDefsTest {

    @Test
    void countsAndSentinels() {
        assertEquals(16, TraitDefs.TRAIT_COUNT);
        assertEquals(0, TraitDefs.TRAIT_FIRST);
        assertEquals(2, TraitDefs.TRAITS_MAX_SELECTED_COUNT);
        assertEquals(15, TraitDefs.Trait.TRAIT_GIFTED.id());
    }

    @Test
    void validators() {
        assertFalse(TraitDefs.traitIsValid(-1));
        assertTrue(TraitDefs.traitIsValid(TraitDefs.TRAIT_FIRST));
        assertTrue(TraitDefs.traitIsValid(TraitDefs.TRAIT_COUNT - 1));
        assertFalse(TraitDefs.traitIsValid(TraitDefs.TRAIT_COUNT));
    }

    @Test
    void postIncrementEquivalent() {
        // C++ `Trait tr = TRAIT_GIFTED; Trait old = tr++;`
        // -> post-old=15 post-new=16 (past TRAIT_COUNT).
        TraitDefs.Trait old = TraitDefs.of(15);
        assertEquals(TraitDefs.Trait.TRAIT_GIFTED, old);
        assertNull(old.next());
        assertEquals(TraitDefs.Trait.TRAIT_INVALID, TraitDefs.of(-1));
    }
}
