package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * {@code [weapondata]} vectors from
 * {@code docker/refharness/golden/v155.txt}, produced by
 * {@code sectionWeaponData} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The attack computation leans on weapon readers: the per-hit-mode
 * animation (weapon extended-flags nibble into the attack-anim table,
 * kicks and bare hands fixed), the critical-failure table index, the
 * ammo-cost fixup of the Super Cattle Prod / Mega Power Fist, and the
 * unarmed bonus-critical-chance table, which the headless harness sees
 * zeroed (unarmedInit is game-phase).
 */
class GameWeaponDataTest {

    private GameObject gun;
    private GameObject dog;

    @BeforeEach
    void setup() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        String gameEn = java.nio.file.Path.of(CritterStateTest.gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));

        GameObject[] gunSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(gunSlot, ProtoTypes.ProtoId.ofPid(390)));
        gun = gunSlot[0];

        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoTypes.ProtoId.ofPid(16777558)));
        dog = dogSlot[0];
    }

    @Test
    void theAnimationPerHitMode() {
        assertEquals(AnimationType.FIRE_SINGLE.value(),
            Item.weaponGetAnimationForHitMode(gun,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));
        assertEquals(AnimationType.STAND.value(),
            Item.weaponGetAnimationForHitMode(gun,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id()));
        assertEquals(AnimationType.THROW_PUNCH.value(),
            Item.weaponGetAnimationForHitMode(null,
                CombatDefs.HitMode.HIT_MODE_PUNCH.id()));
        assertEquals(AnimationType.KICK_LEG.value(),
            Item.weaponGetAnimationForHitMode(null,
                CombatDefs.HitMode.HIT_MODE_KICK.id()));
        assertEquals(AnimationType.THROW_PUNCH.value(),
            Item.critterGetAnimationForHitMode(dog,
                CombatDefs.HitMode.HIT_MODE_PUNCH.id()));
    }

    @Test
    void theCriticalFailureType() {
        assertEquals(-1, Item.weaponGetCriticalFailureType(null));
        assertEquals(3, Item.weaponGetCriticalFailureType(gun));
    }

    @Test
    void theAmmoCostFixup() {
        int[] qty = { 2 };
        assertEquals(0, Item.weaponComputeAmmoCost(gun, qty));
        assertEquals(2, qty[0]);

        GameObject[] fistSlot = new GameObject[1];
        if (ObjectSystem.objectCreateWithProtoId(fistSlot, ProtoTypes.ProtoId.ofPid(407)) == 0) {
            int[] fistQty = { 2 };
            assertEquals(0, Item.weaponComputeAmmoCost(fistSlot[0], fistQty));
            assertEquals(4, fistQty[0]);
        }
    }

    @Test
    void theUnarmedBonusCriticalChanceIsZeroedHeadless() {
        assertEquals(0, Item.unarmedGetBonusCriticalChance(
            CombatDefs.HitMode.HIT_MODE_HAMMER_PUNCH.id()));
        assertEquals(0, Item.unarmedGetBonusCriticalChance(
            CombatDefs.HitMode.HIT_MODE_PIERCING_KICK.id()));
        assertEquals(0, Item.unarmedGetBonusCriticalChance(
            CombatDefs.HitMode.HIT_MODE_PUNCH.id()));
    }
}
