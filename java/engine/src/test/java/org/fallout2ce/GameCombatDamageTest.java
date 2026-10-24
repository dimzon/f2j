package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.CombatDefs.Attack;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * {@code [combatdamage]} vectors from
 * {@code docker/refharness/golden/v152.txt}, produced by
 * {@code sectionCombatDamage} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The fresh dog's threshold/resistance read zero headless (the stat
 * clamp) and its experience reads 150 from the proto. A 15-damage hit on
 * 20 HP flags no death; applying it clamps HP to zero (the zero maximum
 * HP) but leaves the critter alive. A 10-damage hit on 5 HP flags death;
 * applying it kills: the dead bit is already set by
 * {@code _set_new_results}, so the HP adjust keeps the raw -5 instead of
 * re-killing, and the dog is banked in the kills table. The experience
 * bank is internal to the C++ engine and unobservable there; the Java
 * port exposes it as {@code Combat.combatExps()} and pins it to the proto
 * experience.
 */
class GameCombatDamageTest {

    private GameScriptApi api;
    private GameObject dude;

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
        api = new GameScriptApi();
    }

    private GameObject newDog() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoTypes.ProtoId.ofPid(16777558)));
        return dogSlot[0];
    }

    @Test
    void theFreshDogReadsZeroThresholdResistanceAndProtoExperience() {
        GameObject dog = newDog();
        assertEquals(0, Stat.critterGetStat(dog,
            StatDefs.Stat.STAT_DAMAGE_THRESHOLD.id()
                + ProtoTypes.DamageType.DAMAGE_TYPE_NORMAL.id()));
        assertEquals(0, Stat.critterGetStat(dog,
            StatDefs.Stat.STAT_DAMAGE_RESISTANCE.id()
                + ProtoTypes.DamageType.DAMAGE_TYPE_NORMAL.id()));
        assertEquals(150, Critter.critterGetExp(dog));
    }

    @Test
    void aNonLethalHitClampsHpButKeepsTheCritterAlive() {
        GameObject dog = newDog();
        dog.data.critter.hp = 20;
        dog.data.critter.combat.results = Dam.NONE;

        Attack attack = new Attack();
        Combat.attackInit(attack, dude, dog,
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(),
            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());
        attack.attackerFlags = Dam.HIT;
        attack.intendedTarget = dog;
        attack.defenderDamage = 15;

        Combat.attackComputeDeathFlags(attack);
        assertEquals(0, attack.defenderFlags.and(Dam.DEAD).bits());

        Combat.applyDamage(attack, false);
        assertEquals(0, dog.data.critter.hp);
        assertEquals(0, dog.data.critter.combat.results.and(Dam.DEAD).bits());
        assertEquals(15, dog.data.critter.combat.damageLastTurn);
    }

    @Test
    void aLethalHitKillsAndBanksKillsAndExperience() {
        GameObject dog = newDog();
        dog.data.critter.hp = 5;
        dog.data.critter.combat.results = Dam.NONE;

        Attack attack = new Attack();
        Combat.attackInit(attack, dude, dog,
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(),
            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());
        attack.intendedTarget = dog;
        attack.defenderDamage = 10;
        attack.attackerFlags = Dam.HIT;
        attack.defenderFlags = Dam.NONE;

        Combat.attackComputeDeathFlags(attack);
        assertTrue(attack.defenderFlags.and(Dam.DEAD).bits() != 0);

        Combat.applyDamage(attack, false);
        assertEquals(-5, dog.data.critter.hp);
        assertTrue(dog.data.critter.combat.results.and(Dam.DEAD).bits() != 0);
        assertEquals(1, Combat.killsGetByType(ProtoTypes.KillType.KILL_TYPE_DOG.id()));
        assertEquals(150, Combat.combatExps());
    }

    @Test
    void theCritterDamageBuiltinRunsTheSameArm() {
        GameObject dog = newDog();
        // The builtin targets the object blocking the critter's tile, so
        // the dog must stand on one.
        assertEquals(0, ObjectSystem.objectSetLocation(dog, 20100, 0));
        dog.data.critter.hp = 5;
        dog.data.critter.combat.results = Dam.NONE;

        // 0x200 = animated (a seam, harmless here); normal damage type.
        api.critterDamage(dog, 10, 0x200);
        assertTrue(dog.data.critter.combat.results.and(Dam.DEAD).bits() != 0);
    }
}
