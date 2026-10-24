package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * The combat structure of the game.
 *
 * <p>{@code _combat_begin}, {@code _combat_sequence_init},
 * {@code _combat_sequence} and {@code _combat_should_end} are statics of
 * {@code src/combat.cc} with internal linkage, so the reference harness
 * cannot call them - the port is pinned by these unit tests, reading the
 * C++ bodies 1:1.
 *
 * <p>{@code _combat_begin} builds the critter list of the elevation (the
 * hidden ones stay out), resets the combat data (maneuver to engaging
 * only, damage last turn, who-hit-me, ap) and raises the in-combat state.
 * {@code _combat_sequence_init} orders attacker, defender and dude first.
 * {@code _combat_sequence} removes the dead, moves the knocked-out aside
 * and sorts the rest by sequence. {@code _combat_should_end} ends the
 * fight when only one combatant or only allies remain.
 */
class GameCombatSeqTest {

    private GameObject dude;
    private GameObject dog;
    private GameObject other;

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
        // objectsInit leaves the dude hidden (no map loaded); the game phase
        // reveals him when a map loads, which is the state the combat list
        // expects.
        dude.flags = dude.flags.and(ObjTypes.ObjectFlags.HIDDEN.not());
        assertEquals(0, ObjectSystem.objectSetLocation(dude, 20100, 0));

        dog = newDogAt(20101);
        other = newDogAt(20102);
        dude.data.critter.hp = 10;
        dog.data.critter.hp = 10;
        other.data.critter.hp = 10;
    }

    private GameObject newDogAt(int tile) {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoTypes.ProtoId.ofPid(16777558)));
        assertEquals(0, ObjectSystem.objectSetLocation(slot[0], tile, 0));
        return slot[0];
    }

    @Test
    void beginBuildsTheListAndRaisesCombat() {
        Combat.combatBegin(dude);
        assertTrue(Combat.isInCombat());
        assertEquals(3, Combat.listTotal);
        assertEquals(3, Combat.listNoncom);
        assertEquals(0, Combat.listCom);

        for (int i = 0; i < 3; i++) {
            GameObject critter = Combat.combatList[i];
            assertEquals(i, critter.cid);
            assertEquals(0, critter.data.critter.combat.ap);
            assertEquals(0, critter.data.critter.combat.damageLastTurn);
            assertEquals(null, critter.data.critter.combat.whoHitMe);
        }
    }

    @Test
    void hiddenCrittersStayOutOfTheList() {
        ObjectSystem.objectHide(other);
        Combat.combatBegin(dude);
        assertEquals(2, Combat.listTotal);
    }

    @Test
    void sequenceInitOrdersAttackerDefenderThenDude() {
        Combat.combatBegin(dude);
        Combat.combatSequenceInit(dog, dude);

        assertEquals(2, Combat.listCom);
        assertEquals(1, Combat.listNoncom);
        assertSame(dog, Combat.combatList[0]);
        assertSame(dude, Combat.combatList[1]);
        assertSame(dude, dog.data.critter.combat.whoHitMe);
        assertSame(dog, dude.data.critter.combat.whoHitMe);
    }

    @Test
    void sequenceRemovesTheDeadAndSortsBySequence() {
        Combat.combatBegin(dude);
        Combat.combatSequenceInit(dog, dude);

        dog.data.critter.combat.results = Dam.DEAD;
        Combat.combatSequence();

        // The dead leave the combatant list; the non-combatant count is
        // untouched by the dead-removal round.
        assertEquals(1, Combat.listCom);
        assertEquals(1, Combat.listNoncom);
        assertSame(dude, Combat.combatList[0]);
    }

    @Test
    void shouldEndWithOnlyTheDudeLeft() {
        Combat.combatBegin(dude);
        Combat.combatSequenceInit(dog, dude);
        assertFalse(Combat.combatShouldEnd());

        dog.data.critter.combat.results = Dam.DEAD;
        Combat.combatSequence();
        assertTrue(Combat.combatShouldEnd());
    }
}
