package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@code [gamedefs]} stat vectors from
 * {@code docker/refharness/golden/v29.txt}.
 */
class StatDefsTest {

    @Test
    void countsAndSentinels() {
        assertEquals(38, StatDefs.STAT_COUNT);
        assertEquals(0, StatDefs.STAT_FIRST);
        assertEquals(33, StatDefs.SPECIAL_STAT_COUNT);
        assertEquals(35, StatDefs.SAVEABLE_STAT_COUNT);
        assertEquals(7, StatDefs.PRIMARY_STAT_COUNT);
        assertEquals(1, StatDefs.PRIMARY_STAT_MIN);
        assertEquals(10, StatDefs.PRIMARY_STAT_MAX);
        assertEquals(10, StatDefs.PRIMARY_STAT_RANGE);
        assertEquals(99, StatDefs.PC_LEVEL_MAX);
        assertEquals(5, StatDefs.PC_STAT_COUNT);
        assertEquals(0, StatDefs.PC_STAT_FIRST);
    }

    @Test
    void ids() {
        assertEquals(14, StatDefs.Stat.STAT_HEALING_RATE.id());
        assertEquals(StatDefs.Stat.STAT_HEALING_RATE, StatDefs.ofStat(14));
        assertEquals(StatDefs.Stat.STAT_INVALID, StatDefs.ofStat(-1));
        assertNull(StatDefs.ofStat(38));
        assertEquals(4, StatDefs.PcStat.PC_STAT_KARMA.id());
        assertEquals(StatDefs.PcStat.PC_STAT_KARMA, StatDefs.ofPcStat(4));
        assertNull(StatDefs.ofPcStat(5));
    }

    @Test
    void validators() {
        assertFalse(StatDefs.statIsValid(-1));
        assertTrue(StatDefs.statIsValid(StatDefs.STAT_FIRST));
        assertTrue(StatDefs.statIsValid(StatDefs.STAT_COUNT - 1));
        assertFalse(StatDefs.statIsValid(StatDefs.STAT_COUNT));
        assertFalse(StatDefs.pcStatIsValid(-1));
        assertTrue(StatDefs.pcStatIsValid(StatDefs.PC_STAT_FIRST));
        assertFalse(StatDefs.pcStatIsValid(StatDefs.PC_STAT_COUNT));
    }

    @Test
    void postIncrementEquivalent() {
        // C++ `Stat s = STAT_HEALING_RATE; Stat old = s++;`
        // -> post-old=14 post-new=15.
        StatDefs.Stat old = StatDefs.ofStat(14);
        StatDefs.Stat advanced = old.next();
        assertEquals(StatDefs.Stat.STAT_HEALING_RATE, old);
        assertEquals(StatDefs.Stat.STAT_CRITICAL_CHANCE, advanced);

        // C++ `PcStat pc = PC_STAT_KARMA; PcStat old = pc++;`
        // -> pc-post-old=4 pc-post-new=5 (past PC_STAT_COUNT: of() is null,
        // C++ yields the out-of-range id 5).
        StatDefs.PcStat pcOld = StatDefs.ofPcStat(4);
        assertEquals(StatDefs.PcStat.PC_STAT_KARMA, pcOld);
        assertNull(pcOld.next());
    }
}
