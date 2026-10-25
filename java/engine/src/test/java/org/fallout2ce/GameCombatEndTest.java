package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * [combatend], golden/v162.txt: _combat_player_knocked_out_by with arranged
 * dude/ending-guy state, plus the ported _scr_end_combat guard and
 * _combat_to_hit headless seam.
 */
class GameCombatEndTest {
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

    @Test
    void playerKnockedOutByVectors() {
        GameObject dude = ObjectSystem.getDude();
        dude.data.critter.combat.results = dude.data.critter.combat.results.or(Dam.DEAD);
        assertEquals(-1, Combat.combatPlayerKnockedOutBy(), "dead");

        dude.data.critter.combat.results = dude.data.critter.combat.results.and(Dam.DEAD.not());
        assertEquals(-1, Combat.combatPlayerKnockedOutBy(), "no ending guy");

        attacker.data.critter.combat.team = 3;
        Combat.combatEndingGuy = attacker;
        assertEquals(3, Combat.combatPlayerKnockedOutBy(), "team");

        Combat.combatEndingGuy = null;
        assertEquals(-1, Combat.combatPlayerKnockedOutBy(), "team null");

        dude.data.critter.combat.results = dude.data.critter.combat.results.or(Dam.DEAD);
        assertEquals(-1, Combat.combatPlayerKnockedOutBy(), "dead with ending guy");
    }

    @Test
    void scrEndCombatGuardReturnsFalseWithoutMapScript() {
        assertEquals(-1, Map.mapSid());
        assertFalse(Combat.scrEndCombat());
    }

    @Test
    void combatToHitUsesPrimaryHandSeam() {
        GameObject dude = ObjectSystem.getDude();
        assertEquals(0, ObjectSystem.objectSetLocation(dude, 20100, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(defender, 20101, 0));
        assertEquals(0, Item.itemAdd(dude, weapon, 1));
        weapon.flags = weapon.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        Item.ammoSetQuantity(weapon, 10);
        dude.data.critter.hp = 50;
        defender.data.critter.hp = 50;
        dude.data.critter.combat.ap = 10;

        Proto[] proto = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(dude), proto));
        proto[0].critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()] = 200;

        assertEquals(CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_OK,
                Combat.combatCheckBadShot(dude, defender,
                        HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), false));

        // The harness cannot register the dude on the map headless
        // (C++ objectSetLocation segfaults), so the exact accuracy number
        // for the dude is not pinned; the gate and the accuracy path are
        // verified against the vector pieces they call.
        int[] accuracy = { 0 };
        assertTrue(Combat.combatToHit(defender, accuracy));
        int hitMode = HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id();
        if (Interface.interfaceGetCurrentHand() == Inventory.Hand.HAND_LEFT) {
            hitMode = HitMode.HIT_MODE_LEFT_WEAPON_PRIMARY.id();
        }
        int[] direct = { 0 };
        direct[0] = Combat.attackDetermineToHit(dude, dude.tile, defender,
                HitLocation.HIT_LOCATION_UNCALLED.id(), hitMode, true);
        assertEquals(direct[0], accuracy[0]);
        assertTrue(accuracy[0] > 0, "accuracy " + accuracy[0]);
    }
}
