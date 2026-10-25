package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.CombatDefs.HitLocation;
import org.fallout2ce.CombatDefs.HitMode;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * [combatattack], golden/v156.txt: the real C++ _combat_attack and its
 * completion callback, with only _action_attack scheduling substituted.
 */
class GameCombatAttackTest {
    private GameObject attacker;
    private GameObject defender;
    private GameObject weapon;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        Map.resetForTests();
        Stat.resetForTests();
        Skill.resetForTests();
        TraitDefs.resetForTests();
        GameRandom.resetProviders();
        Combat.resetForTests();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();

        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("master.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("patch000.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("critter.dat")));
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());
        assertEquals(0, Mouse.mouseInit());
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200,
                new byte[64 * 64], 64, 64, 64, rect -> { }));
        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));

        attacker = create(16777558);
        defender = create(16777558);
        weapon = create(8);
        assertEquals(0, ObjectSystem.objectSetLocation(attacker, 20100, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(defender, 20101, 0));
        assertEquals(0, Item.itemAdd(attacker, weapon, 1));
        // The reference sets the equipped flag directly: a dog has no gun art.
        weapon.flags = weapon.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        Proto[] proto = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(attacker), proto));
        proto[0].critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()] = 200;
        attacker.data.critter.hp = 50;
        defender.data.critter.hp = 50;
        attacker.data.critter.combat.ap = 10;
        Item.ammoSetQuantity(weapon, 10);
        GameRandom.randomSeedPrerandom(1);
    }

    @AfterEach
    void cleanup() {
        Combat.resetForTests();
        GameRandom.resetProviders();
        ObjectSystem.resetForTests();
        ProtoDb.resetForTests();
        PartyMembers.resetForTests();
        Db.resetForTests();
    }

    private GameObject create(int pid) {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoId.ofPid(pid)));
        assertNotNull(slot[0]);
        return slot[0];
    }

    private int submit(HitLocation location) {
        return Combat.combatAttack(attacker, defender,
                HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), location.id());
    }

    private void assertPending(int ap, int damage, int flags) {
        assertEquals(ap, attacker.data.critter.combat.ap);
        assertEquals(Dam.HIT.bits(), Combat.mainAttack.attackerFlags.and(Dam.HIT).bits());
        assertEquals(damage, Combat.mainAttack.defenderDamage);
        assertEquals(flags, Combat.mainAttack.defenderFlags.bits());
        assertEquals(1, Combat.mainAttack.ammoQuantity);
        assertEquals(10, Item.ammoGetQuantity(weapon));
        assertEquals(50, defender.data.critter.hp);
        assertEquals(0, defender.data.critter.combat.damageLastTurn);
        assertTrue(Combat.combatCleanupEnabled);
    }

    @Test
    void weaponSkillIsAValueAndUsesTheWeaponNibble() {
        assertEquals(0, Item.weaponGetSkillForHitMode(weapon,
                HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));
        assertEquals(205, Item.weaponGetSkillValue(attacker,
                HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));
        assertEquals(0, Item.weaponGetSkillValue(null,
                HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));
    }

    @Test
    void normalAttackSpendsApBeforeAmmoOrDamage() {
        assertEquals(0, submit(HitLocation.HIT_LOCATION_TORSO));
        assertPending(5, 10, 0);
    }

    @Test
    void calledShotCostsOneMoreAp() {
        assertEquals(0, submit(HitLocation.HIT_LOCATION_HEAD));
        assertPending(4, 10, 0);
    }

    @Test
    void lowApSaturatesAtZero() {
        attacker.data.critter.combat.ap = 2;
        assertEquals(0, submit(HitLocation.HIT_LOCATION_TORSO));
        assertPending(0, 10, 0);
    }

    @Test
    void schedulingFailureDoesNotSpendResources() {
        Combat.attackScheduler = attack -> -1;
        assertEquals(-1, submit(HitLocation.HIT_LOCATION_TORSO));
        assertEquals(10, attacker.data.critter.combat.ap);
        assertEquals(10, Combat.mainAttack.defenderDamage);
        assertEquals(10, Item.ammoGetQuantity(weapon));
        assertEquals(50, defender.data.critter.hp);
        assertFalse(Combat.combatCleanupEnabled);
        assertEquals(0, Combat.combatTurnRunning);
    }

    private ScriptsState.CombatStartData start(int damageBonus) {
        ScriptsState.CombatStartData start = new ScriptsState.CombatStartData();
        start.damageBonus = damageBonus;
        start.minDamage = 3;
        start.maxDamage = 9;
        start.overrideAttackResults = 1;
        start.attackerResults = Dam.DEAD;
        start.targetResults = Dam.KNOCKED_DOWN;
        return start;
    }

    @Test
    void startBonusClampsAndSecondOverrideWinsOnTheDefender() {
        Combat.gcsd = start(100);
        assertEquals(0, submit(HitLocation.HIT_LOCATION_TORSO));
        assertPending(5, 9, Dam.KNOCKED_DOWN.bits());
        assertEquals(0, Combat.mainAttack.attackerFlags.and(Dam.DEAD).bits());
    }

    @Test
    void negativeStartBonusClampsToMinimum() {
        Combat.gcsd = start(-100);
        assertEquals(0, submit(HitLocation.HIT_LOCATION_TORSO));
        assertPending(5, 3, Dam.KNOCKED_DOWN.bits());
    }

    @Test
    void accuracyBonusTurnsAnOtherwiseImpossibleShotIntoAHit() {
        Proto[] proto = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(attacker), proto));
        proto[0].critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()] = 20;
        int accuracy = Combat.attackDetermineToHit(attacker, attacker.tile, defender,
                HitLocation.HIT_LOCATION_TORSO.id(), HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), true);
        assertTrue(accuracy < 0);
        Combat.gcsd = new ScriptsState.CombatStartData();
        Combat.gcsd.accuracyBonus = 100;
        Combat.gcsd.maxDamage = Integer.MAX_VALUE;
        assertEquals(accuracy + 100, Combat.attackDetermineToHit(attacker, attacker.tile, defender,
                HitLocation.HIT_LOCATION_TORSO.id(), HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), true));
        assertEquals(0, submit(HitLocation.HIT_LOCATION_TORSO));
        assertPending(5, 10, 0);
    }

    @Test
    void finalCallbackConsumesAmmoAppliesDamageAndResetsTheAttack() {
        assertEquals(0, submit(HitLocation.HIT_LOCATION_TORSO));
        assertPending(5, 10, 0);
        // Multiple animation starts delay cleanup until the last callback.
        Combat.combatAnimBegin();
        Combat.combatAnimFinished();
        assertPending(5, 10, 0);
        Combat.combatAnimFinished();
        assertEquals(9, Item.ammoGetQuantity(weapon));
        assertEquals(0, defender.data.critter.hp); // native zero max-HP clamp
        assertEquals(10, defender.data.critter.combat.damageLastTurn);
        assertFalse(Combat.combatCleanupEnabled);
        assertEquals(0, Combat.combatTurnRunning);
        assertNull(Combat.mainAttack.defender);
        assertEquals(0, Combat.mainAttack.defenderDamage);
    }

    @Test
    void headlessWaitCompletesPendingDamage() {
        assertEquals(0, submit(HitLocation.HIT_LOCATION_TORSO));
        Combat.combatTurnRun();
        assertEquals(9, Item.ammoGetQuantity(weapon));
        assertEquals(10, defender.data.critter.combat.damageLastTurn);
        Combat.combatTurnRun();
        assertEquals(9, Item.ammoGetQuantity(weapon)); // no double completion
    }

    @Test
    void outOfRangeDoesNotReachScheduling() {
        assertEquals(0, ObjectSystem.objectSetLocation(defender, 23000, 0));
        Combat.attackScheduler = attack -> {
            fail("out-of-range attack reached scheduler");
            return 0;
        };
        assertEquals(-1, submit(HitLocation.HIT_LOCATION_TORSO));
        assertEquals(10, attacker.data.critter.combat.ap);
        assertEquals(0, Combat.mainAttack.attackerFlags.bits());
        assertEquals(0, Combat.mainAttack.defenderDamage);
        assertEquals(0, Combat.mainAttack.ammoQuantity);
        assertEquals(10, Item.ammoGetQuantity(weapon));
        assertEquals(50, defender.data.critter.hp);
    }

    @Test
    void badShotVectors() {
        int rightPrimary = HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id();
        assertEquals(CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_OK,
                Combat.combatCheckBadShot(attacker, defender, rightPrimary, false));

        defender.data.critter.combat.results = Dam.DEAD;
        assertEquals(CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_ALREADY_DEAD,
                Combat.combatCheckBadShot(attacker, defender, rightPrimary, false));
        defender.data.critter.combat.results = Dam.NONE;

        attacker.data.critter.combat.ap = 2;
        assertEquals(CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_NOT_ENOUGH_AP,
                Combat.combatCheckBadShot(attacker, defender, rightPrimary, false));
        attacker.data.critter.combat.ap = 10;

        Item.ammoSetQuantity(weapon, 0);
        assertEquals(CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_NO_AMMO,
                Combat.combatCheckBadShot(attacker, defender, rightPrimary, false));
        Item.ammoSetQuantity(weapon, 10);

        assertEquals(0, ObjectSystem.objectSetLocation(defender, 23000, 0));
        assertEquals(CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_OUT_OF_RANGE,
                Combat.combatCheckBadShot(attacker, defender, rightPrimary, false));
        assertEquals(0, ObjectSystem.objectSetLocation(defender, 20101, 0));

        attacker.data.critter.combat.results = Dam.CRIP_ARM_LEFT.or(Dam.CRIP_ARM_RIGHT);
        assertEquals(CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_BOTH_ARMS_CRIPPLED,
                Combat.combatCheckBadShot(attacker, defender, rightPrimary, false));
        attacker.data.critter.combat.results = Dam.CRIP_ARM_LEFT;
        assertEquals(CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_OK,
                Combat.combatCheckBadShot(attacker, defender, rightPrimary, false));
        attacker.data.critter.combat.results = Dam.NONE;
    }
}
