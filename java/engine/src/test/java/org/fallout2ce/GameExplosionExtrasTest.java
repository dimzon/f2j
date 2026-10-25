package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.CombatDefs.Attack;
import org.fallout2ce.CombatDefs.HitLocation;
import org.fallout2ce.CombatDefs.HitMode;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * [explosionextras], golden/v160.txt: the real C++ _compute_explosion_on_extras
 * ring walk, bomb target pickup and the attacker backwash branch.
 */
class GameExplosionExtrasTest {
    private GameObject attacker;
    private GameObject defender;
    private GameObject weapon;
    private GameObject extraA;
    private GameObject extraB;
    private int tileNE;
    private int tileSW;

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
        extraA = create(16777558);
        extraB = create(16777558);
        assertEquals(0, ObjectSystem.objectSetLocation(attacker, 1100, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(defender, 1101, 0));
        tileNE = TileMath.tileGetTileInDirection(1101, Rotation.NE, 1);
        tileSW = TileMath.tileGetTileInDirection(1101, Rotation.SW, 1);
        assertEquals(0, ObjectSystem.objectSetLocation(extraA, tileNE, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(extraB, tileSW, 0));
        assertEquals(0, Item.itemAdd(attacker, weapon, 1));
        weapon.flags = weapon.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        Proto[] proto = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(attacker), proto));
        proto[0].critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()] = 200;
        attacker.data.critter.hp = 50;
        defender.data.critter.hp = 50;
        extraA.data.critter.hp = 50;
        extraB.data.critter.hp = 50;
        attacker.data.critter.combat.ap = 10;
        Item.ammoSetQuantity(weapon, 10);
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

    private Attack makeAttack(GameObject attackerObj, GameObject defenderObj) {
        Attack attack = new Attack();
        Combat.attackInit(attack, attackerObj, defenderObj,
                HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                HitLocation.HIT_LOCATION_TORSO.id());
        attack.attackerFlags = attack.attackerFlags.or(Dam.HIT);
        return attack;
    }

    @Test
    void explosionExtrasVectors() {
        Item.explosionSettingsReset();
        Item.explosionSetRadius(1);
        int[][] expectations = {
            { 1, 7, 8 },
            { 7, 8, 7 },
        };
        for (int[] exp : expectations) {
            GameRandom.randomSeedPrerandom(exp[0]);
            Attack attack = makeAttack(attacker, defender);
            Combat.computeExplosionOnExtras(attack, false, true, false);
            assertEquals(2, attack.extrasLength, "extras seed " + exp[0]);
            assertEquals(tileNE, attack.extras[0].tile, "first tile seed " + exp[0]);
            assertEquals(16777558, attack.extras[0].pid, "first pid seed " + exp[0]);
            assertEquals(exp[1], attack.extrasDamage[0], "first dmg seed " + exp[0]);
            assertEquals(0, attack.extrasFlags[0].bits(), "first flags seed " + exp[0]);
            assertEquals(0, attack.extrasKnockback[0], "first kb seed " + exp[0]);
            assertEquals(HitLocation.HIT_LOCATION_TORSO.id(), attack.extrasHitLocation[0],
                    "first loc seed " + exp[0]);
            assertEquals(tileSW, attack.extras[1].tile, "second tile seed " + exp[0]);
            assertEquals(exp[2], attack.extrasDamage[1], "second dmg seed " + exp[0]);
        }
    }

    @Test
    void explosionBackwashVector() {
        GameObject attacker2 = create(16777558);
        GameObject defender2 = create(16777558);
        GameObject weapon2 = create(8);
        int neighborTile = TileMath.tileGetTileInDirection(1201, Rotation.NE, 1);
        assertEquals(0, ObjectSystem.objectSetLocation(attacker2, neighborTile, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(defender2, 1201, 0));
        assertEquals(0, Item.itemAdd(attacker2, weapon2, 1));
        weapon2.flags = weapon2.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        attacker2.data.critter.hp = 50;
        defender2.data.critter.hp = 50;

        Item.explosionSettingsReset();
        Item.explosionSetRadius(1);
        GameRandom.randomSeedPrerandom(3);
        Attack attack = makeAttack(attacker2, defender2);
        Combat.computeExplosionOnExtras(attack, false, true, false);
        assertEquals(0, attack.extrasLength, "backwash extras");
        assertTrue((attack.attackerFlags.bits() & Dam.BACKWASH.bits()) != 0,
                "backwash flag");
    }
}
