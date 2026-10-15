package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.fallout2ce.CombatAiDefs.AreaAttackMode;
import org.fallout2ce.CombatAiDefs.AttackWho;
import org.fallout2ce.CombatAiDefs.BestWeapon;
import org.fallout2ce.CombatAiDefs.ChemUse;
import org.fallout2ce.CombatAiDefs.Disposition;
import org.fallout2ce.CombatAiDefs.DistanceMode;
import org.fallout2ce.CombatAiDefs.HurtTooMuch;
import org.fallout2ce.CombatAiDefs.RunAwayMode;

/**
 * {@code [combatdefs]} AI vectors from
 * {@code docker/refharness/golden/v32.txt}.
 */
class CombatAiDefsTest {

    @Test
    void runAwayMode() {
        assertEquals(7, CombatAiDefs.RUN_AWAY_MODE_COUNT);
        assertEquals(0, CombatAiDefs.RUN_AWAY_MODE_FIRST);
        assertEquals(6, CombatAiDefs.RUN_AWAY_MODE_LAST);
        assertTrue(CombatAiDefs.runAwayModeIsValid(6));
        assertFalse(CombatAiDefs.runAwayModeIsValid(7));
        // post-old=6 post-new=7 (past the end).
        assertEquals(RunAwayMode.RUN_AWAY_MODE_NEVER, CombatAiDefs.ofRunAwayMode(6));
        assertNull(CombatAiDefs.ofRunAwayMode(6).next());
        // postdec-old=0 postdec-new=-1 (past the start).
        assertEquals(RunAwayMode.RUN_AWAY_MODE_NONE, CombatAiDefs.ofRunAwayMode(0));
        assertEquals(RunAwayMode.RUN_AWAY_MODE_INVALID, CombatAiDefs.ofRunAwayMode(0).prev());
        assertNull(CombatAiDefs.ofRunAwayMode(-2));
    }

    @Test
    void countsAndFirsts() {
        assertEquals(5, CombatAiDefs.AREA_ATTACK_MODE_COUNT);
        assertEquals(0, CombatAiDefs.AREA_ATTACK_MODE_FIRST);
        assertEquals(8, CombatAiDefs.BEST_WEAPON_COUNT);
        assertEquals(0, CombatAiDefs.BEST_WEAPON_FIRST);
        assertEquals(5, CombatAiDefs.DISTANCE_COUNT);
        assertEquals(0, CombatAiDefs.DISTANCE_FIRST);
        assertEquals(5, CombatAiDefs.ATTACK_WHO_COUNT);
        assertEquals(0, CombatAiDefs.ATTACK_WHO_FIRST);
        assertEquals(6, CombatAiDefs.CHEM_USE_COUNT);
        assertEquals(0, CombatAiDefs.CHEM_USE_FIRST);
        assertEquals(6, CombatAiDefs.DISPOSITION_COUNT);
        assertEquals(0, CombatAiDefs.DISPOSITION_FIRST);
        assertEquals(4, CombatAiDefs.HURT_COUNT);
        assertEquals(0, CombatAiDefs.HURT_FIRST);
    }

    @Test
    void validatorsAndLookups() {
        assertTrue(CombatAiDefs.areaAttackModeIsValid(4));
        assertFalse(CombatAiDefs.areaAttackModeIsValid(5));
        assertEquals(AreaAttackMode.AREA_ATTACK_MODE_BE_ABSOLUTELY_SURE, CombatAiDefs.ofAreaAttackMode(4));
        assertTrue(CombatAiDefs.bestWeaponIsValid(7));
        assertEquals(BestWeapon.BEST_WEAPON_RANDOM, CombatAiDefs.ofBestWeapon(7));
        assertTrue(CombatAiDefs.distanceModeIsValid(4));
        assertEquals(DistanceMode.DISTANCE_STAY, CombatAiDefs.ofDistanceMode(4));
        assertTrue(CombatAiDefs.attackWhoIsValid(4));
        assertEquals(AttackWho.ATTACK_WHO_CLOSEST, CombatAiDefs.ofAttackWho(4));
        assertTrue(CombatAiDefs.chemUseIsValid(5));
        assertEquals(ChemUse.CHEM_USE_ALWAYS, CombatAiDefs.ofChemUse(5));
        assertTrue(CombatAiDefs.dispositionIsValid(5));
        assertEquals(HurtTooMuch.HURT_CRIPPLED_ARMS, CombatAiDefs.ofHurtTooMuch(3));
        assertNull(CombatAiDefs.ofHurtTooMuch(4));
    }

    @Test
    void dispositionPostDecrement() {
        // disp-postdec-old=1 disp-postdec-new=0.
        assertEquals(Disposition.DISPOSITION_CUSTOM, CombatAiDefs.ofDisposition(1));
        assertEquals(Disposition.DISPOSITION_NONE, CombatAiDefs.ofDisposition(1).prev());
        assertEquals(Disposition.DISPOSITION_INVALID, CombatAiDefs.ofDisposition(0).prev());
    }
}
