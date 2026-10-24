package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * The combat turn of the game.
 *
 * <p>{@code _combat_turn} is a static of {@code src/combat.cc} with
 * internal linkage, so the harness cannot call it; the port is pinned by
 * these unit tests, reading the C++ body 1:1. The player-input and AI
 * turns and the UI/audio/render tails are game-phase seams; what stays is
 * the turn state (lost-turn clear, script combat proc with its override,
 * stand-up AP cost, gcsd seed) and the cleanup.
 */
class GameCombatTurnTest {

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

        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoTypes.ProtoId.ofPid(16777558)));
        dog = dogSlot[0];
        dog.flags = dog.flags.and(ObjTypes.ObjectFlags.HIDDEN.not());
    }

    @Test
    void aLostTurnIsClearedAndNothingElseHappens() {
        dog.data.critter.combat.results = Dam.LOSE_TURN;
        assertEquals(0, Combat.combatTurn(dog, false));
        assertEquals(0, dog.data.critter.combat.results.and(Dam.LOSE_TURN).bits());
        assertSame(dog, Combat.combatTurnObj);
    }

    @Test
    void aDeadLostTurnKeepsTheDeadBit() {
        dog.data.critter.combat.results = Dam.DEAD.or(Dam.LOSE_TURN);
        assertEquals(0, Combat.combatTurn(dog, false));
        assertTrue(dog.data.critter.combat.results.and(Dam.DEAD).bits() != 0);
        assertEquals(0, dog.data.critter.combat.results.and(Dam.LOSE_TURN).bits());
    }

    @Test
    void aProneCritterStandsUpCostingAp() {
        dog.data.critter.hp = 10;
        dog.data.critter.combat.results = Dam.KNOCKED_DOWN;
        dog.data.critter.combat.ap = 10;

        assertEquals(0, Combat.combatTurn(dog, false));
        assertEquals(7, dog.data.critter.combat.ap);
        assertEquals(0, dog.data.critter.combat.results.and(Dam.KNOCKED_DOWN).bits());
    }

    @Test
    void aProneCritterWithNoApLosesTheRest() {
        dog.data.critter.hp = 10;
        dog.data.critter.combat.results = Dam.KNOCKED_DOWN;
        dog.data.critter.combat.ap = 2;

        assertEquals(0, Combat.combatTurn(dog, false));
        assertEquals(0, dog.data.critter.combat.ap);
        assertEquals(0, dog.data.critter.combat.results.and(Dam.KNOCKED_DOWN).bits());
    }

    @Test
    void reloadedTurnsDoNotStandUp() {
        dog.data.critter.hp = 10;
        dog.data.critter.combat.results = Dam.KNOCKED_DOWN;
        dog.data.critter.combat.ap = 10;

        assertEquals(0, Combat.combatTurn(dog, true));
        assertEquals(10, dog.data.critter.combat.ap);
        assertTrue(dog.data.critter.combat.results.and(Dam.KNOCKED_DOWN).bits() != 0);
    }

    @Test
    void theDudeTurnRaisesPlayerTurnState() {
        dude.data.critter.hp = 10;
        Combat.combatElev = 0;

        assertEquals(0, Combat.combatTurn(dude, false));
        assertTrue((Combat.combatState & Combat.COMBAT_STATE_PLAYER_TURN) != 0);
    }

    @Test
    void aDeadDudeEndsTheTurn() {
        dude.data.critter.hp = 0;
        dude.data.critter.combat.results = Dam.DEAD;

        assertEquals(-1, Combat.combatTurn(dog, false));
    }
}
