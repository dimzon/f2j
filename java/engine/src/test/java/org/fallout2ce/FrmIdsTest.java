package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * {@code [frmids]} vectors from
 * {@code docker/refharness/golden/v95.txt}, produced by
 * {@code sectionFrmIds} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Pure tables only: no game data / window system. Skill/stat/trait
 * frame tables are compiled in on both sides (the headless C++ harness
 * never runs the init loaders either).
 */
class FrmIdsTest {

    @Test
    void frmIds() {
        int sneak = Skill.skillGetFrmId(SkillDefs.Skill.SKILL_SNEAK.id());
        int skillBad = Skill.skillGetFrmId(-1);
        int str = Stat.statGetFrmId(StatDefs.Stat.STAT_STRENGTH.id());
        int statBad = Stat.statGetFrmId(-1);
        int gifted = TraitDefs.traitGetFrmId(TraitDefs.Trait.TRAIT_GIFTED.id());

        assertEquals(36, sneak);
        assertEquals(-1, skillBad);
        assertEquals(0, str);
        assertEquals(-1, statBad);
        assertEquals(70, gifted);

        String line = String.format("sneak=%d skillbad=%d str=%d statbad=%d gifted=%d",
            sneak, skillBad, str, statBad, gifted);
        assertEquals("sneak=36 skillbad=-1 str=0 statbad=-1 gifted=70", line);
    }
}
