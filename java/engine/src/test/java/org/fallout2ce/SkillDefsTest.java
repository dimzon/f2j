package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@code [gamedefs]} skill vectors from
 * {@code docker/refharness/golden/v29.txt}.
 */
class SkillDefsTest {

    @Test
    void countsAndSentinels() {
        assertEquals(18, SkillDefs.SKILL_COUNT);
        assertEquals(0, SkillDefs.SKILL_FIRST);
        assertEquals(4, SkillDefs.NUM_TAGGED_SKILLS);
        assertEquals(3, SkillDefs.DEFAULT_TAGGED_SKILLS);
        assertEquals(8, SkillDefs.Skill.SKILL_SNEAK.id());
    }

    @Test
    void validators() {
        assertFalse(SkillDefs.skillIsValid(-1));
        assertTrue(SkillDefs.skillIsValid(SkillDefs.SKILL_FIRST));
        assertTrue(SkillDefs.skillIsValid(SkillDefs.SKILL_COUNT - 1));
        assertFalse(SkillDefs.skillIsValid(SkillDefs.SKILL_COUNT));
    }

    @Test
    void postIncrementEquivalent() {
        // C++ `Skill sk = SKILL_SNEAK; Skill old = sk++;`
        // -> post-old=8 post-new=9.
        SkillDefs.Skill old = SkillDefs.of(8);
        assertEquals(SkillDefs.Skill.SKILL_SNEAK, old);
        assertEquals(SkillDefs.Skill.SKILL_LOCKPICK, old.next());
        assertEquals(SkillDefs.Skill.SKILL_INVALID, SkillDefs.of(-1));
        assertNull(SkillDefs.of(18));
    }
}
