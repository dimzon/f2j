package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code [pcstat]} vectors from
 * {@code docker/refharness/golden/v78.txt}, produced by
 * {@code sectionPcStat} in {@code docker/refharness/ref_main.cc};
 * {@code [statssave]} vectors from
 * {@code docker/refharness/golden/v94.txt}, produced by
 * {@code sectionStatsSave} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Pure tables only: no game data / window system. PC values start
 * BSS-zeroed (fresh-process mirror, like the headless C++ harness where
 * {@code pcStatsReset} never runs, so LEVEL reads 0, not its default 1).
 * Experience paths are skipped like the harness: they pull the level/xp
 * machinery ({@code pcAddExperienceWithOptions} / {@code pcSetExperience}
 * stay dormant game-phase seams in {@link Stat}).
 */
class PcStatTest {

    @TempDir
    Path temp;

    @BeforeEach
    void setup() {
        Stat.resetForTests();
    }

    @AfterEach
    void teardown() {
        Stat.resetForTests();
        Db.resetForTests();
    }

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

    @Test
    void statsSaveVectors() {
        Stat.resetForTests();

        // ap=4,4 set=6,7 (AP bonuses start at their compiled defaults).
        int apBonus0 = Stat.statGetUnspentApBonus();
        int apPerk0 = Stat.statGetUnspentApPerkBonus();
        Stat.statSetUnspentApBonus(6);
        Stat.statSetUnspentApPerkBonus(7);
        int apBonus = Stat.statGetUnspentApBonus();
        int apPerk = Stat.statGetUnspentApPerkBonus();
        assertEquals(4, apBonus0);
        assertEquals(4, apPerk0);
        assertEquals(6, apBonus);
        assertEquals(7, apPerk);

        String apLine = String.format("ap=%d,%d set=%d,%d", apBonus0, apPerk0, apBonus, apPerk);
        assertEquals("ap=4,4 set=6,7", apLine);

        // save=0 load=0 back=5,7 (5 pc values round-trip through a file).
        int level = StatDefs.PcStat.PC_STAT_LEVEL.id();
        int reputation = StatDefs.PcStat.PC_STAT_REPUTATION.id();
        assertEquals(0, Stat.pcSetStat(level, 5));
        assertEquals(0, Stat.pcSetStat(reputation, 7));

        String path = temp.resolve("stt.bin").toString();
        XFile.Handle saveStream = Db.fileOpen(path, "wb");
        assertNotNull(saveStream);
        int saveRc = Stat.statsSave(saveStream);
        assertEquals(0, Db.fileClose(saveStream));

        assertEquals(0, Stat.pcSetStat(level, 1));
        assertEquals(0, Stat.pcSetStat(reputation, 0));

        XFile.Handle loadStream = Db.fileOpen(path, "rb");
        assertNotNull(loadStream);
        int loadRc = Stat.statsLoad(loadStream);
        assertEquals(0, Db.fileClose(loadStream));

        int levelBack = Stat.pcGetStat(level);
        int repBack = Stat.pcGetStat(reputation);
        assertEquals(0, saveRc);
        assertEquals(0, loadRc);
        assertEquals(5, levelBack);
        assertEquals(7, repBack);

        String saveLine =
                String.format("save=%d load=%d back=%d,%d", saveRc, loadRc, levelBack, repBack);
        assertEquals("save=0 load=0 back=5,7", saveLine);
    }
}
