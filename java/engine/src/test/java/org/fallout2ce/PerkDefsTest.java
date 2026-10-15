package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@code [gamedefs]} perk vectors from
 * {@code docker/refharness/golden/v29.txt}.
 */
class PerkDefsTest {

    @Test
    void countsAndSentinels() {
        assertEquals(119, PerkDefs.PERK_COUNT);
        assertEquals(0, PerkDefs.PERK_FIRST);
        assertEquals(51, PerkDefs.Perk.PERK_TAG.id());
        assertEquals(52, PerkDefs.Perk.PERK_MUTATE.id());
        assertEquals(118, PerkDefs.Perk.PERK_JINXED.id());
    }

    @Test
    void fullTableIsDense() {
        for (int id = 0; id < PerkDefs.PERK_COUNT; id++) {
            PerkDefs.Perk perk = PerkDefs.of(id);
            assertTrue(perk != null, "missing perk id " + id);
            assertEquals(id, perk.id());
        }
        assertEquals(PerkDefs.Perk.PERK_INVALID, PerkDefs.of(-1));
        assertNull(PerkDefs.of(119));
    }

    @Test
    void validators() {
        assertFalse(PerkDefs.perkIsValid(-1));
        assertTrue(PerkDefs.perkIsValid(PerkDefs.PERK_FIRST));
        assertTrue(PerkDefs.perkIsValid(PerkDefs.PERK_COUNT - 1));
        assertFalse(PerkDefs.perkIsValid(PerkDefs.PERK_COUNT));
    }

    @Test
    void postIncrementEquivalent() {
        // C++ `Perk pk = PERK_TAG; Perk old = pk++;` -> post-old=51 post-new=52.
        PerkDefs.Perk old = PerkDefs.of(51);
        assertEquals(PerkDefs.Perk.PERK_TAG, old);
        assertEquals(PerkDefs.Perk.PERK_MUTATE, old.next());
        // Past the end: C++ yields out-of-range id 119, Java of() is null.
        assertNull(PerkDefs.Perk.PERK_JINXED.next());
    }
}
