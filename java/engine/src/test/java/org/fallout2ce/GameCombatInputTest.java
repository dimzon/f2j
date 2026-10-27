package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * The player-combat input loop ({@code _combat_input}), pinned against the
 * C++ section {@code [combatinput]} in the ref harness (golden v176).
 *
 * <p>The loop's stop conditions — AP exhausted, dead/knocked-out dude,
 * EXIT_REQUESTED, the SPACE key — and the input queue itself are real; the
 * render/UI tails stay game-phase seams.
 */
class GameCombatInputTest {

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
        ScriptsState.resetForTests();
        WindowManager.resetForTests();
        InterfaceFont.resetForTests();
        TextFont.resetForTests();
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();

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
        // Mirror game.cc: modern (interface) fonts back font ids 100+.
        assertEquals(0, InterfaceFont.interfaceFontsInit());
        assertEquals(0, TextFont.fontManagerAdd(100, 110, InterfaceFont.IMPL));
        TextFont.fontSetCurrent(100);
        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        Input.programIsActive = true;

        byte[] tileBuf = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf, 64, 64, 64,
                rect -> {
                }));

        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));
        dude = ObjectSystem.getDude();
        dude.flags = dude.flags.and(ObjTypes.ObjectFlags.HIDDEN.not());
        assertEquals(0, ObjectSystem.objectSetLocation(dude, 20100, 0));
        dude.data.critter.hp = 10;
    }

    private void startPlayerTurn() {
        Combat.combatState = Combat.COMBAT_STATE_IN_COMBAT | Combat.COMBAT_STATE_PLAYER_TURN;
    }

    @Test
    void exhaustedActionPointsBreakTheLoop() {
        startPlayerTurn();
        dude.data.critter.combat.ap = 0;
        dude.data.critter.combat.results = Dam.NONE;
        assertEquals(0, Combat.combatInput());
    }

    @Test
    void aDeadDudeBreaksTheLoopImmediately() {
        startPlayerTurn();
        dude.data.critter.combat.ap = 5;
        dude.data.critter.combat.results = Dam.DEAD;
        assertEquals(0, Combat.combatInput());
    }

    @Test
    void anExitRequestEndsTheLoopAndClearsTheFlag() {
        startPlayerTurn();
        Combat.combatState |= Combat.COMBAT_STATE_EXIT_REQUESTED;
        dude.data.critter.combat.results = Dam.NONE;
        // The harness prints the state before the call: IN (1) | PLAYER (2)
        // | EXIT (8) = 11.
        assertEquals(11, Combat.combatState);
        assertEquals(-1, Combat.combatInput());
        assertEquals(0, Combat.combatState & Combat.COMBAT_STATE_EXIT_REQUESTED);
    }

    @Test
    void theSpaceKeyBreaksTheLoop() {
        startPlayerTurn();
        dude.data.critter.combat.ap = 5;
        dude.data.critter.combat.results = Dam.NONE;
        Input.inputEventQueueReset();
        Input.enqueueInputEvent(Kb.KEY_SPACE);
        assertEquals(0, Combat.combatInput());
    }
}
