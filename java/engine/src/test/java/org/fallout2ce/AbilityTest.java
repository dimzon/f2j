package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.fallout2ce.Perk.PerkProperty;
import org.fallout2ce.Skill.SkillStealResult;

/**
 * {@code [ability]} vectors from
 * {@code docker/refharness/golden/v36.txt}.
 */
class AbilityTest {

    @Test
    void statError() {
        assertEquals(-5, Stat.STAT_ERR_INVALID_STAT);
    }

    @Test
    void stealResult() {
        assertEquals(0, SkillStealResult.Caught.id());
        assertEquals(1, SkillStealResult.Success.id());
        assertEquals(2, SkillStealResult.Fail.id());
        assertEquals(SkillStealResult.Fail, Skill.ofSkillStealResult(2));
        assertNull(Skill.ofSkillStealResult(3));
    }

    @Test
    void perkProperty() {
        assertEquals(0, PerkProperty.FrmId.id());
        assertEquals(16, PerkProperty.Luck.id());
        assertEquals(17, PerkProperty.values().length);
        assertEquals(PerkProperty.StatModifier, Perk.ofPerkProperty(4));
        assertNull(Perk.ofPerkProperty(17));
    }
}
