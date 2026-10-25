package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.CombatDefs.Attack;
import org.fallout2ce.CombatDefs.HitLocation;
import org.fallout2ce.CombatDefs.HitMode;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * [rangedmiss], golden/v161.txt: the random-trajectory and grenade-landing
 * block of attackCompute after a miss, pinned against the real C++ helper.
 */
class GameRangedMissTest {
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
        GameRandom.randomSeedPrerandom(1);
    }

    @AfterEach
    void tearDown() {
        GameRandom.resetProviders();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
    }

    private static GameObject create(int pid) {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoId.ofPid(pid)));
        return slot[0];
    }

    private Attack makeAttack() {
        Attack attack = new Attack();
        Combat.attackInit(attack, attacker, defender,
                HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                HitLocation.HIT_LOCATION_TORSO.id());
        return attack;
    }

    private void equip(GameObject weaponObj) {
        assertEquals(0, Item.itemAdd(attacker, weaponObj, 1));
        weaponObj.flags = weaponObj.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        attacker.data.critter.hp = 50;
        defender.data.critter.hp = 50;
        Item.ammoSetQuantity(weaponObj, 10);
    }

    @Test
    void grenadeLandingVectors() {
        assertEquals(0, ObjectSystem.objectSetLocation(attacker, 2100, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(defender, 2101, 0));
        equip(weapon);

        Proto[] slot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot));
        ProtoTypes.DamageType savedDamageType = slot[0].item.data.weapon.damageType;
        int savedFlags = slot[0].item.extendedFlags.bits();
        slot[0].item.data.weapon.damageType =
                ProtoTypes.DamageType.DAMAGE_TYPE_EXPLOSION;
        slot[0].item.extendedFlags = new ProtoTypes.ProtoExtendedFlags((savedFlags & ~0xF) | 5);

        int[][] expectations = {
            { 3, 2301 },
            { 9, 2102 },
            { 17, 1902 },
        };
        try {
            int distance = ObjectSystem.objectGetDistanceBetweenTiles(attacker,
                    attacker.tile, defender, defender.tile);
            int range = Item.weaponGetRange(attacker,
                    HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
            for (int[] exp : expectations) {
                GameRandom.randomSeedPrerandom(exp[0]);
                Attack attack = makeAttack();
                Combat.applyRangedMissTrajectory(attack, distance, range, true);
                assertEquals(exp[1], attack.tile, "tile seed " + exp[0]);
                assertEquals(0, attack.attackerFlags.bits(), "flags seed " + exp[0]);
                assertEquals(0, attack.defenderDamage, "dmg seed " + exp[0]);
                assertEquals(16777558, attack.defender.pid, "target seed " + exp[0]);
            }
        } finally {
            slot[0].item.data.weapon.damageType = savedDamageType;
            slot[0].item.extendedFlags = new ProtoTypes.ProtoExtendedFlags(savedFlags);
        }
    }

    @Test
    void rangedTrajectoryVectors() {
        assertEquals(0, ObjectSystem.objectSetLocation(attacker, 2200, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(defender, 2205, 0));
        equip(weapon);

        int distance = ObjectSystem.objectGetDistanceBetweenTiles(attacker,
                attacker.tile, defender, defender.tile);
        int range = Item.weaponGetRange(attacker,
                HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
        for (int seed : new int[] { 5, 11, 23 }) {
            GameRandom.randomSeedPrerandom(seed);
            Attack attack = makeAttack();
            Combat.applyRangedMissTrajectory(attack, distance, range, false);
            assertEquals(1825, attack.tile, "tile seed " + seed);
            assertEquals(0, attack.attackerFlags.bits(), "flags seed " + seed);
            assertEquals(0, attack.defenderDamage, "dmg seed " + seed);
            assertEquals(16777558, attack.defender.pid, "target seed " + seed);
        }
    }
}
