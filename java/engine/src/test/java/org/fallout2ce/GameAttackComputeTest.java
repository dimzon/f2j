package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.CombatDefs.Attack;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * The attack computation of the game.
 *
 * <p>{@code attackCompute} is a static of {@code src/combat.cc} with
 * internal linkage, so the harness cannot call it; the port is pinned by
 * these unit tests, reading the C++ body 1:1. The burst spray, the ranged
 * miss interception and the explosion extras are seams; the critical-hit
 * tables are deferred (a critical hit without them is vanilla x2 damage
 * with no effects).
 *
 * <p>The roll is driven deterministically: a seed is chosen so that the
 * first {@code randomBetween(1, 100)} of the roll lands in the wanted
 * bracket.
 */
class GameAttackComputeTest {

    private GameObject dude;
    private GameObject dog;
    private GameObject gun;

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
        Combat.resetForTests();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
        Skill.resetForTests();
        Stat.resetForTests();
        TraitDefs.resetForTests();
        GameRandom.resetProviders();

        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;

        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("master.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("patch000.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("critter.dat")));
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());
        assertEquals(0, Mouse.mouseInit());

        byte[] tileBuf = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf, 64, 64, 64,
                rect -> {
                }));

        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));
        dude = ObjectSystem.getDude();
        dude.flags = dude.flags.and(ObjTypes.ObjectFlags.HIDDEN.not());
        assertEquals(0, ObjectSystem.objectSetLocation(dude, 20100, 0));

        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoTypes.ProtoId.ofPid(16777558)));
        dog = dogSlot[0];
        dog.flags = dog.flags.and(ObjTypes.ObjectFlags.HIDDEN.not());
        assertEquals(0, ObjectSystem.objectSetLocation(dog, 20101, 0));
        dude.data.critter.hp = 50;
        dog.data.critter.hp = 50;

        GameObject[] gunSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(gunSlot, ProtoTypes.ProtoId.ofPid(390)));
        gun = gunSlot[0];
    }

    private void armDude() {
        assertEquals(0, Item.itemAdd(dude, gun, 1));
        gun.flags = gun.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        // The Solar Scorcher uses energy weapons. Raise that skill, not an
        // index into ATTACK_SUBTYPE: weaponGetSkillValue reads a skill value.
        int skill = Item.weaponGetSkillForHitMode(gun,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
        assertEquals(SkillDefs.Skill.SKILL_ENERGY_WEAPONS.id(), skill);
        while (Skill.skillAddForce(dude, skill) == 0) {
            // raise to the maximum
        }
        ObjectSystem.objectSetLight(dog, 4, 0x10000);
    }

    @AfterEach
    void cleanup() {
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        Skill.resetForTests();
        Stat.resetForTests();
        GameRandom.resetProviders();
    }

    private static int findSeed(int min, int max, int threshold, boolean below) {
        for (int seed = 1; seed <= 100000; seed++) {
            GameRandom.randomSeedPrerandom(seed);
            int v = GameRandom.randomBetween(min, max);
            if (below ? v <= threshold : v > threshold) {
                return seed;
            }
        }
        throw new IllegalStateException("no seed found");
    }

    private Attack attack(GameObject attacker, GameObject defender, int hitMode) {
        Attack attack = new Attack();
        Combat.attackInit(attack, attacker, defender, hitMode,
            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());
        return attack;
    }

    private void raiseSkill(int skill) {
        while (Skill.skillAddForce(dude, skill) == 0) {
            // raise to the maximum
        }
    }

    @Test
    void outOfRangeIsRefused() {
        ObjectSystem.objectSetLocation(dude, 20100 + 8, 0);
        Attack attack = attack(dog, dude, CombatDefs.HitMode.HIT_MODE_PUNCH.id());
        assertEquals(-1, Combat.attackCompute(attack));
        assertEquals(0, attack.attackerFlags.bits());
    }

    @Test
    void aSuccessHitFlagsAndDamages() {
        raiseSkill(SkillDefs.Skill.SKILL_UNARMED.id());
        ObjectSystem.objectSetLight(dude, 4, 0x10000);
        GameRandom.randomSeedPrerandom(findSeed(1, 100, 95, true));
        Attack attack = attack(dog, dude, CombatDefs.HitMode.HIT_MODE_PUNCH.id());
        assertEquals(0, Combat.attackCompute(attack));
        assertTrue((attack.attackerFlags.bits() & Dam.HIT.bits()) != 0);
        assertTrue(attack.defenderDamage > 0);
    }

    @Test
    void aFailureStaysUnarmed() {
        raiseSkill(SkillDefs.Skill.SKILL_UNARMED.id());
        GameRandom.randomSeedPrerandom(findSeed(1, 100, 95, false));
        Attack attack = attack(dog, dude, CombatDefs.HitMode.HIT_MODE_PUNCH.id());
        assertEquals(0, Combat.attackCompute(attack));
        assertEquals(0, attack.attackerFlags.bits() & Dam.HIT.bits());
        assertEquals(0, attack.defenderDamage);
    }

    @Test
    void aRangedGunCountsAmmo() {
        armDude();
        Attack attack = attack(dude, dog,
            CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
        assertEquals(0, Combat.attackCompute(attack));
        assertEquals(1, attack.ammoQuantity);
    }

    @Test
    void aGunHitUsesEnergySkillAndDamages() {
        armDude();
        GameRandom.randomSeedPrerandom(1);
        Attack attack = attack(dude, dog,
            CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
        assertEquals(0, Combat.attackCompute(attack));
        assertEquals(300, Item.weaponGetSkillValue(dude,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));
        assertTrue((attack.attackerFlags.bits() & Dam.HIT.bits()) != 0);
        assertTrue(attack.defenderDamage > 0);
    }
}
