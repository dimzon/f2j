package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * {@code [statskill]} vectors from
 * {@code docker/refharness/golden/v53.txt}.
 *
 * <p>Pure tables only: no game data / window system. Bounds and tagged
 * state start zeroed (fresh-process mirror, like the headless C++ harness
 * where {@code statsInit} is out of reach); the difficulty setting is
 * saved and restored like the harness.
 */
class StatSkillTest {

    @Test
    void statSkill() {
        Stat.resetForTests();
        Skill.resetForTests();

        // cost=1,1,2,3,4,5,6
        assertEquals(1, Skill.skillsGetCost(0));
        assertEquals(1, Skill.skillsGetCost(100));
        assertEquals(2, Skill.skillsGetCost(101));
        assertEquals(3, Skill.skillsGetCost(126));
        assertEquals(4, Skill.skillsGetCost(151));
        assertEquals(5, Skill.skillsGetCost(176));
        assertEquals(6, Skill.skillsGetCost(201));

        // pcmax0=0 npcmax0=0 bad=0 setmax=12 setmin=1
        int strength = StatDefs.Stat.STAT_STRENGTH.id();
        assertEquals(0, Stat.getConfiguredMaximum(strength, false));
        assertEquals(0, Stat.getConfiguredMaximum(strength, true));
        assertEquals(0, Stat.getConfiguredMaximum(-1, false));
        Stat.setPcMaximum(strength, 12);
        Stat.setPcMinimum(strength, 1);
        assertEquals(12, Stat.getConfiguredMaximum(strength, false));
        assertEquals(1, Stat.getConfiguredMinimum(strength, false));

        // defsneak=5 defbad=-5 tagged=0 diff=0,0 hard=-10 easy=20
        int sneak = SkillDefs.Skill.SKILL_SNEAK.id();
        int smallGuns = SkillDefs.Skill.SKILL_SMALL_GUNS.id();
        assertEquals(5, Skill.getDefaultValue(sneak));
        assertEquals(-5, Skill.getDefaultValue(-1));
        assertFalse(Skill.isTagged(sneak));

        int savedDiff = Settings.settings.preferences.game_difficulty;
        try {
            Settings.settings.preferences.game_difficulty = Settings.GAME_DIFFICULTY_NORMAL;
            assertEquals(0, Skill.getGameDifficultyModifier(sneak));
            assertEquals(0, Skill.getGameDifficultyModifier(smallGuns));
            Settings.settings.preferences.game_difficulty = Settings.GAME_DIFFICULTY_HARD;
            assertEquals(-10, Skill.getGameDifficultyModifier(sneak));
            Settings.settings.preferences.game_difficulty = Settings.GAME_DIFFICULTY_EASY;
            assertEquals(20, Skill.getGameDifficultyModifier(sneak));
        } finally {
            Settings.settings.preferences.game_difficulty = savedDiff;
        }
    }
}
