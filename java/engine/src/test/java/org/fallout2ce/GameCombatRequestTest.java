package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjectSystem.Script;
import org.fallout2ce.ScriptsState.CombatStartData;

/**
 * {@code [combatrequest]} vectors from
 * {@code docker/refharness/golden/v153.txt}, produced by
 * {@code sectionCombatRequest} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The script combat request is pure state: requesting copies the
 * CombatStartData and raises the flag; a locked request is refused; the
 * clear clears the lock when the requesting script's owner is the
 * attacker. The {@code _combat} run itself is game phase and a seam.
 *
 * <p>The attack opcode raises the same request outside combat and marks
 * the attacker engaging inside combat; both need the script context, which
 * the test presets.
 */
class GameCombatRequestTest {

    private GameScriptApi api;
    private GameObject dude;
    private GameObject dog;

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

        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoTypes.ProtoId.ofPid(16777558)));
        dog = dogSlot[0];
        dog.data.critter.hp = 10;
        dog.data.critter.combat.results = Dam.NONE;
        // The headless dude is hidden; the attack guards reject hidden
        // critters, so the fresh dog is the attacker in the opcode tests.
    }

    private GameObject newDog() {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoTypes.ProtoId.ofPid(16777558)));
        slot[0].data.critter.hp = 10;
        slot[0].data.critter.combat.results = Dam.NONE;
        return slot[0];
    }

    private CombatStartData requestData() {
        CombatStartData csd = new CombatStartData();
        csd.attacker = dude;
        csd.defender = dog;
        csd.actionPointsBonus = 0;
        csd.accuracyBonus = 15;
        csd.damageBonus = 0;
        csd.minDamage = 3;
        csd.maxDamage = 12;
        csd.overrideAttackResults = 1;
        csd.attackerResults = Dam.HIT;
        csd.targetResults = Dam.DEAD;
        return csd;
    }

    @Test
    void theRequestCopiesAndRaisesTheFlag() {
        CombatStartData csd = requestData();
        assertEquals(0, ScriptsState.scriptsRequestCombat(csd));
        assertTrue((ScriptsState.scriptsRequests & ScriptsState.SCRIPT_REQUEST_COMBAT) != 0);
        assertSame(dude, ScriptsState.requestedCSD.attacker);
        assertSame(dog, ScriptsState.requestedCSD.defender);
        assertEquals(15, ScriptsState.requestedCSD.accuracyBonus);
        assertEquals(3, ScriptsState.requestedCSD.minDamage);
        assertEquals(12, ScriptsState.requestedCSD.maxDamage);
        assertEquals(1, ScriptsState.requestedCSD.overrideAttackResults);
        assertTrue(ScriptsState.requestedCSD.targetResults.and(Dam.DEAD).bits() != 0);
    }

    @Test
    void aLockedRequestIsRefusedAndTheClearLetsItThrough() {
        CombatStartData csd = requestData();
        assertEquals(0, ScriptsState.scriptsRequestCombat(csd));
        ScriptsState.scriptsRequestCombatLocked(csd);
        assertEquals(-1, ScriptsState.scriptsRequestCombat(csd));

        int[] sidSlot = new int[1];
        assertEquals(0, ObjectSystem.scriptAdd(sidSlot, ObjectSystem.SCRIPT_TYPE_CRITTER));
        Script[] slot = new Script[1];
        assertEquals(0, ObjectSystem.scriptGetScript(sidSlot[0], slot));
        slot[0].owner = dude;
        ScriptsState.scriptsClearCombatRequests(slot[0]);
        assertEquals(0, ScriptsState.scriptsRequestCombat(csd));
    }

    @Test
    void theAttackOpcodeRequestsCombatOutsideCombat() {
        GameObject attacker = dog;
        GameObject defender = newDog();
        Program program = Program.programCreateByPath("scripts\\acdog.int");
        int[] sidSlot = new int[1];
        assertEquals(0, ObjectSystem.scriptAdd(sidSlot, ObjectSystem.SCRIPT_TYPE_CRITTER));
        Script[] slot = new Script[1];
        assertEquals(0, ObjectSystem.scriptGetScript(sidSlot[0], slot));
        slot[0].program = program;
        slot[0].owner = attacker;
        Program.setInterpreterCurrentProgramForTests(program);

        api.attack(defender, 0, 1, 15, 3, 12, 0x100, 0x100);
        assertTrue((ScriptsState.scriptsRequests & ScriptsState.SCRIPT_REQUEST_COMBAT) != 0);
        assertSame(attacker, ScriptsState.requestedCSD.attacker);
        assertSame(defender, ScriptsState.requestedCSD.defender);
        assertEquals(15, ScriptsState.requestedCSD.accuracyBonus);
        assertEquals(3, ScriptsState.requestedCSD.minDamage);
        assertEquals(12, ScriptsState.requestedCSD.maxDamage);
        assertEquals(1, ScriptsState.requestedCSD.overrideAttackResults);
        assertEquals(Dam.HIT.bits(), ScriptsState.requestedCSD.attackerResults.bits());
        assertEquals(Dam.HIT.bits(), ScriptsState.requestedCSD.targetResults.bits());

        ScriptsState.resetForTests();
        Program.setInterpreterCurrentProgramForTests(null);
        Program.programFree(program);
    }

    @Test
    void theAttackOpcodeMarksEngagingInCombat() {
        GameObject attacker = dog;
        GameObject defender = newDog();
        Program program = Program.programCreateByPath("scripts\\acdog.int");
        int[] sidSlot = new int[1];
        assertEquals(0, ObjectSystem.scriptAdd(sidSlot, ObjectSystem.SCRIPT_TYPE_CRITTER));
        Script[] slot = new Script[1];
        assertEquals(0, ObjectSystem.scriptGetScript(sidSlot[0], slot));
        slot[0].program = program;
        slot[0].owner = attacker;
        Program.setInterpreterCurrentProgramForTests(program);

        Combat.combatState = Combat.COMBAT_STATE_IN_COMBAT;
        api.attack(defender, 0, 1, 15, 3, 12, 0x100, 0x100);
        assertTrue((attacker.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.ENGAGING.bits()) != 0);
        assertSame(defender, attacker.data.critter.combat.whoHitMe);
        assertEquals(0, ScriptsState.scriptsRequests & ScriptsState.SCRIPT_REQUEST_COMBAT);

        Combat.resetForTests();
        Program.setInterpreterCurrentProgramForTests(null);
        Program.programFree(program);
    }
}
