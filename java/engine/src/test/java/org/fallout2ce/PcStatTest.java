package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * {@code [pcstat]} vectors from
 * {@code docker/refharness/golden/v78.txt}, produced by
 * {@code sectionPcStat} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Pure tables only: no game data / window system. PC values start
 * BSS-zeroed (fresh-process mirror, like the headless C++ harness where
 * {@code pcStatsReset} never runs, so LEVEL reads 0, not its default 1).
 * Experience paths are skipped like the harness: they pull the level/xp
 * machinery ({@code pcAddExperienceWithOptions} / {@code pcSetExperience}
 * stay dormant game-phase seams in {@link Stat}).
 */
class PcStatTest {

    @Test
    void pcStatVectors() {
        Stat.resetForTests();

        int unspent = StatDefs.PcStat.PC_STAT_UNSPENT_SKILL_POINTS.id();
        int level = StatDefs.PcStat.PC_STAT_LEVEL.id();
        int reputation = StatDefs.PcStat.PC_STAT_REPUTATION.id();

        int sp0 = Stat.pcGetStat(unspent);
        int badGet = Stat.pcGetStat(99);
        int setLevel = Stat.pcSetStat(level, 5);
        int levelVal = Stat.pcGetStat(level);
        int setLevelHigh = Stat.pcSetStat(level, 1000);
        int setRepLow = Stat.pcSetStat(reputation, -21);
        int setRep = Stat.pcSetStat(reputation, 7);
        int repVal = Stat.pcGetStat(reputation);
        int setBad = Stat.pcSetStat(99, 1);

        assertEquals(0, sp0);
        assertEquals(0, badGet);
        assertEquals(0, setLevel);
        assertEquals(5, levelVal);
        assertEquals(-3, setLevelHigh);
        assertEquals(-2, setRepLow);
        assertEquals(0, setRep);
        assertEquals(7, repVal);
        assertEquals(-5, setBad);

        String line = String.format(
                "sp0=%d badget=%d setlevel=%d level=%d toohigh=%d toolow=%d setrep=%d rep=%d badset=%d",
                sp0, badGet, setLevel, levelVal, setLevelHigh, setRepLow, setRep, repVal, setBad);
        assertEquals(
                "sp0=0 badget=0 setlevel=0 level=5 toohigh=-3 toolow=-2 setrep=0 rep=7 badset=-5",
                line);
    }
}
