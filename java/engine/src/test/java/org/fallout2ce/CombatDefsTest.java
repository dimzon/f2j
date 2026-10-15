package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.fallout2ce.CombatDefs.CombatBadShot;
import org.fallout2ce.CombatDefs.CombatState;
import org.fallout2ce.CombatDefs.CriticalEffect;
import org.fallout2ce.CombatDefs.CriticalHitDataMember;
import org.fallout2ce.CombatDefs.CriticalHitDescription;
import org.fallout2ce.CombatDefs.HitLocation;
import org.fallout2ce.CombatDefs.HitMode;

/**
 * {@code [combatdefs]} vectors from
 * {@code docker/refharness/golden/v32.txt}.
 */
class CombatDefsTest {

    @Test
    void criticalEffect() {
        assertEquals(6, CombatDefs.CRITICAL_EFFECT_COUNT);
        assertEquals(0, CombatDefs.CRITICAL_EFFECT_FIRST);
        assertFalse(CombatDefs.criticalEffectIsValid(-1));
        assertFalse(CombatDefs.criticalEffectIsValid(6));
        assertTrue(CombatDefs.criticalEffectIsValid(5));
        // post-old=2 post-new=3.
        assertEquals(CriticalEffect.CRITICAL_EFFECT_3, CombatDefs.ofCriticalEffect(2));
        assertEquals(CriticalEffect.CRITICAL_EFFECT_4, CombatDefs.ofCriticalEffect(2).next());
        assertNull(CombatDefs.ofCriticalEffect(6));
    }

    @Test
    void combatState() {
        // state-or=3 state-and=2 state-andu=0 state-not=-1.
        CombatState or = CombatState.IN_COMBAT.or(CombatState.PLAYER_TURN);
        assertEquals(3, or.bits());
        assertEquals(2, or.and(CombatState.PLAYER_TURN).bits());
        assertEquals(0, or.and(0x08).bits());
        assertEquals(-1, CombatState.OUT_COMBAT.not().bits());
    }

    @Test
    void hitMode() {
        assertEquals(20, CombatDefs.HIT_MODE_COUNT);
        assertEquals(0, CombatDefs.HIT_MODE_FIRST);
        assertEquals(8, CombatDefs.FIRST_ADVANCED_PUNCH_HIT_MODE);
        assertEquals(13, CombatDefs.LAST_ADVANCED_PUNCH_HIT_MODE);
        assertEquals(14, CombatDefs.FIRST_ADVANCED_KICK_HIT_MODE);
        assertEquals(19, CombatDefs.LAST_ADVANCED_KICK_HIT_MODE);
        assertFalse(CombatDefs.hitModeIsValid(-1));
        assertFalse(CombatDefs.hitModeIsValid(20));
        // post-old=4 post-new=5.
        assertEquals(HitMode.HIT_MODE_PUNCH, CombatDefs.ofHitMode(4));
        assertEquals(HitMode.HIT_MODE_KICK, CombatDefs.ofHitMode(4).next());
        assertEquals(HitMode.HIT_MODE_INVALID, CombatDefs.ofHitMode(-1));
    }

    @Test
    void hitLocation() {
        assertEquals(9, CombatDefs.HIT_LOCATION_COUNT);
        assertEquals(8, CombatDefs.HIT_LOCATION_SPECIFIC_COUNT);
        assertEquals(0, CombatDefs.HIT_LOCATION_FIRST);
        assertEquals(8, HitLocation.HIT_LOCATION_UNCALLED.id());
        assertFalse(CombatDefs.hitLocationIsValid(9));
        // post-old=7 post-new=8.
        assertEquals(HitLocation.HIT_LOCATION_GROIN, CombatDefs.ofHitLocation(7));
        assertEquals(HitLocation.HIT_LOCATION_UNCALLED, CombatDefs.ofHitLocation(7).next());
        assertNull(CombatDefs.ofHitLocation(8).next());
    }

    @Test
    void criticalHitDataMemberAndBadShot() {
        assertEquals(7, CombatDefs.CRIT_DATA_MEMBER_COUNT);
        assertEquals(0, CombatDefs.CRIT_DATA_MEMBER_FIRST);
        assertTrue(CombatDefs.criticalHitDataMemberIsValid(6));
        // post-old=1 post-new=2.
        assertEquals(CriticalHitDataMember.CRIT_DATA_MEMBER_FLAGS, CombatDefs.ofCriticalHitDataMember(1));
        assertEquals(CriticalHitDataMember.CRIT_DATA_MEMBER_MASSIVE_CRITICAL_STAT,
            CombatDefs.ofCriticalHitDataMember(1).next());
        assertEquals(0, CombatBadShot.COMBAT_BAD_SHOT_OK.id());
        assertEquals(7, CombatBadShot.COMBAT_BAD_SHOT_BOTH_ARMS_CRIPPLED.id());
        assertEquals(CombatBadShot.COMBAT_BAD_SHOT_AIM_BLOCKED, CombatDefs.ofCombatBadShot(5));
    }

    @Test
    void criticalHitDescription() {
        // critdesc=3,640,0,-2,256,100,101.
        CriticalHitDescription desc = new CriticalHitDescription();
        desc.damageMultiplier = 3;
        desc.flags = 640;
        desc.massiveCriticalStat = StatDefs.Stat.STAT_STRENGTH;
        desc.massiveCriticalStatModifier = -2;
        desc.massiveCriticalFlags = 256;
        desc.messageId = 100;
        desc.massiveCriticalMessageId = 101;
        assertArrayEquals(new int[] { 3, 640, 0, -2, 256, 100, 101 }, desc.values());

        CriticalHitDescription back = new CriticalHitDescription();
        int[] golden = { 3, 640, 0, -2, 256, 100, 101 };
        CriticalHitDataMember[] members = CriticalHitDataMember.values();
        for (int i = 0; i < golden.length; i++) {
            back.setIndexed(members[i], golden[i]);
        }
        assertArrayEquals(golden, back.values());
        assertEquals(StatDefs.Stat.STAT_STRENGTH, back.massiveCriticalStat);
    }
}
