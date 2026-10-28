package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.spi.VideoBackend;

/**
 * The game main loop ({@code mainLoop}, main.cc), pinned against the C++
 * section {@code [mainloop]} in the ref harness (golden v176).
 *
 * <p>The loop body, the input queue, {@code scriptsHandleRequests} and the
 * death check are real; gameHandleKey, the sfall MainLoopHook,
 * mapHandleTransition, the endgame death ending and the render/FPS tails are
 * seams. The sfall hook in the harness replaces the C++ MainLoopHook with an
 * iteration counter that requests the main menu on the second pass; the tests
 * mirror that, so the observed iteration counts and quit/state transitions
 * match the golden values.
 */
class GameMainLoopTest {

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
        // Buffered refresh blits the screen buffer out; there is no video
        // surface in tests, so drop the copy (IsoRestTest convention).
        Svga.scrBlit = (src, srcPitch, unused, srcX, srcY, srcWidth, srcHeight, destX, destY) -> {
        };
        Mouse.mouseBlit = (src, srcPitch, unused, srcX, srcY, srcWidth, srcHeight, destX, destY) -> {
        };
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

        // scr_game_init equivalence: script mode on.
        ScriptsState.gameModeEnabled = true;
        ScriptsState.critterProcessingEnabled = true;
        Game.mainGamePaused = 0;
        Game.mainShowDeathScene = false;
    }

    private int runMainLoopWithHook(List<Integer> keysRead, int quitAfterIterations) {
        final int[] iterations = { 0 };
        Game.sfallGlScrProcessMain = () -> {
            iterations[0]++;
            if (iterations[0] >= quitAfterIterations) {
                GameScriptApi.gameUserWantsToQuit =
                    GameScriptApi.GAME_QUIT_REQUEST_MAIN_MENU;
            }
        };
        Game.gameHandleKey = (keyCode, inCombat) -> keysRead.add(keyCode);
        Game.mapHandleTransitionHook = () -> 0;
        Game.endgameSetupDeathEnding = () -> {
        };

        Game.mainLoop();
        return iterations[0];
    }

    @Test
    void theLoopRunsUntilTheQuitRequest() {
        GameScriptApi.gameUserWantsToQuit = GameScriptApi.GAME_QUIT_REQUEST_NONE;
        Input.inputEventQueueReset();

        int iterations = runMainLoopWithHook(new ArrayList<>(), 2);

        // Golden [mainloop] normal: final-iter=2 final-quit=2.
        assertEquals(2, iterations);
        assertEquals(GameScriptApi.GAME_QUIT_REQUEST_MAIN_MENU,
            GameScriptApi.gameUserWantsToQuit);
    }

    @Test
    void theLoopReadsTheQueuedKey() {
        GameScriptApi.gameUserWantsToQuit = GameScriptApi.GAME_QUIT_REQUEST_NONE;
        Input.inputEventQueueReset();
        Input.enqueueInputEvent(Kb.KEY_SPACE);

        List<Integer> keysRead = new ArrayList<>();
        int iterations = runMainLoopWithHook(keysRead, 2);

        // Golden [mainloop] space: key=32 then key=-1.
        assertEquals(2, iterations);
        assertEquals(List.of(Kb.KEY_SPACE, -1), keysRead);
    }

    @Test
    void aDeadDudeEndsTheLoopOnTheFirstPass() {
        dude.data.critter.combat.results = Dam.DEAD;
        GameScriptApi.gameUserWantsToQuit = GameScriptApi.GAME_QUIT_REQUEST_NONE;
        Input.inputEventQueueReset();

        int iterations = runMainLoopWithHook(new ArrayList<>(), 99);

        // Golden [mainloop] dead: death=1 final-iter=1 final-quit=2.
        assertEquals(1, iterations);
        assertTrue(Game.mainShowDeathScene);
        assertEquals(GameScriptApi.GAME_QUIT_REQUEST_MAIN_MENU,
            GameScriptApi.gameUserWantsToQuit);
    }

    @Test
    void scriptsEnableRequiresGameMode() {
        ScriptsState.gameModeEnabled = false;
        assertEquals(-1, ScriptsState.scriptsEnable());
        assertFalse(ObjectSystem.scriptsEnabled);

        ScriptsState.gameModeEnabled = true;
        assertEquals(0, ScriptsState.scriptsEnable());
        assertTrue(ObjectSystem.scriptsEnabled);
        assertTrue(ScriptsState.critterProcessingEnabled);

        assertEquals(0, ScriptsState.scriptsDisable());
        assertFalse(ObjectSystem.scriptsEnabled);
    }

    @Test
    void theLoopPresentsOncePerIteration() {
        // The loop tail calls renderFpsCounter + renderPresent; renderPresent
        // is wired to Svga.renderPresent (video backend present), while the
        // FPS overlay stays a seam unless the debug flag is on.
        RecordingVideo video = new RecordingVideo();
        VideoBackend saved = Color.video;
        Color.video = video;
        try {
            GameScriptApi.gameUserWantsToQuit = GameScriptApi.GAME_QUIT_REQUEST_NONE;
            Input.inputEventQueueReset();

            int iterations = runMainLoopWithHook(new ArrayList<>(), 2);

            assertEquals(2, iterations);
            assertEquals(2, video.presents);
        } finally {
            Color.video = saved;
        }
    }

    /** Recording backend: counts presents, owns a 640x480 framebuffer. */
    private static final class RecordingVideo implements VideoBackend {
        int presents;
        final byte[] framebuffer = new byte[640 * 480];

        @Override
        public void setPalette(byte[] rgb768) {
        }

        @Override
        public void setPaletteEntries(byte[] rgb, int start, int count) {
        }

        @Override
        public void present() {
            presents++;
        }

        @Override
        public byte[] framebuffer() {
            return framebuffer;
        }

        @Override
        public byte[] palette() {
            return new byte[256 * 3];
        }

        @Override
        public void freeSurface() {
        }
    }
}
