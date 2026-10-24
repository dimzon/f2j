package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ScriptsState.CombatStartData;

/**
 * The combat loop of the game.
 *
 * <p>{@code _combat} is a static of {@code src/combat.cc} with internal
 * linkage, so the harness cannot call it; the port is pinned by these unit
 * tests, reading the C++ body 1:1. The player-input and AI turns and the
 * UI/audio/render tails are game-phase seams; the loop structure, the AP
 * refresh, the sequence advance and the close are real.
 */
class GameCombatLoopTest {

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
        dude.flags = dude.flags.and(ObjTypes.ObjectFlags.HIDDEN.not());
        assertEquals(0, ObjectSystem.objectSetLocation(dude, 20100, 0));

        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoTypes.ProtoId.ofPid(16777558)));
        dog = dogSlot[0];
        dog.flags = dog.flags.and(ObjTypes.ObjectFlags.HIDDEN.not());
        assertEquals(0, ObjectSystem.objectSetLocation(dog, 20101, 0));
        dude.data.critter.hp = 10;
        dog.data.critter.hp = 10;
    }

    private CombatStartData requestData() {
        CombatStartData csd = new CombatStartData();
        csd.attacker = dog;
        csd.defender = dude;
        csd.actionPointsBonus = 0;
        csd.accuracyBonus = 0;
        csd.damageBonus = 0;
        csd.minDamage = 0;
        csd.maxDamage = 0;
        csd.overrideAttackResults = 0;
        return csd;
    }

    @Test
    void aDeadOpponentEndsTheFight() {
        // The dog is already dead: its turn only clears lost-turn state, the
        // dude takes the player turn with the input seam returning, and the
        // sequence removes the dead dog, leaving a single combatant.
        dog.data.critter.combat.results = Dam.DEAD;

        Combat.combat(requestData());

        assertEquals(1, Combat.combatNumTurns);
        assertEquals(0, Combat.combatState & Combat.COMBAT_STATE_IN_COMBAT);
        assertNull(Combat.combatList);
    }

    @Test
    void theDudeRegainsHisActionPointsAfterTheClose() {
        dog.data.critter.combat.results = Dam.DEAD;
        Combat.combat(requestData());
        assertEquals(0, Combat.combatState & Combat.COMBAT_STATE_IN_COMBAT);
        int maxAp = Stat.critterGetStat(dude, StatDefs.Stat.STAT_MAXIMUM_ACTION_POINTS.id());
        assertEquals(maxAp, dude.data.critter.combat.ap);
    }

    @Test
    void theCombatDataIsClearedOnClose() {
        dog.data.critter.combat.results = Dam.DEAD;
        Combat.combat(requestData());
        assertEquals(0, dog.data.critter.combat.ap);
        assertNull(dog.data.critter.combat.whoHitMe);
        assertEquals(0, dog.data.critter.combat.maneuver.bits());
        assertEquals(0, dog.data.critter.combat.damageLastTurn);
    }

    @Test
    void aFightAgainstNoOneEndsImmediately() {
        Combat.combat(null);
        assertEquals(1, Combat.combatNumTurns);
        assertEquals(0, Combat.combatState & Combat.COMBAT_STATE_IN_COMBAT);
    }

    @Test
    void thePlayerTurnStateIsRaisedOnTheDudeTurn() {
        dog.data.critter.combat.results = Dam.DEAD;
        Combat.combat(requestData());
        assertTrue((Combat.combatState & Combat.COMBAT_STATE_PLAYER_TURN) != 0);
        assertFalse((Combat.combatState & Combat.COMBAT_STATE_IN_COMBAT) != 0);
    }
}
