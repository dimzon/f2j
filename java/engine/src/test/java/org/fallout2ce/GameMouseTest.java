package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [gamemouse]} vectors from
 * {@code docker/refharness/golden/v71.txt}, produced by
 * {@code sectionGameMouse} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Mirrors the harness flow exactly: init / double-init / cursor reads and
 * transitions (including the duplicate non-ARROW guard), the mode read (the
 * set/cycle tails need live map objects and stay dormant headless, per the
 * harness NOTE), the scrolling and click-to-scroll flags, and the objects
 * visibility read (show/hide repaint needs the tile refresher, null
 * headless, per the harness NOTE), then exit.
 */
class GameMouseTest {

    private int savedLeft;
    private int savedTop;
    private int savedRight;
    private int savedBottom;
    private String savedLanguage;

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path p = Paths.get(root, name);
        if (!Files.isRegularFile(p)) {
            throw new IllegalStateException("missing " + p);
        }
        return p.toString();
    }

    @BeforeEach
    void setup() {
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        Db.resetForTests();
        TextFont.resetForTests();
        GameMouse.resetForTests();

        savedLeft = Svga.scrSize.left;
        savedTop = Svga.scrSize.top;
        savedRight = Svga.scrSize.right;
        savedBottom = Svga.scrSize.bottom;
        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;

        Mouse.mouseBlit = (src, srcPitch, unused, srcX, srcY, srcWidth, srcHeight, destX, destY) -> {
        };

        savedLanguage = Settings.settings.system.language;
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/patch000.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/critter.dat"));
        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());

        // Headless mouse frame setup (WindowButtonTest pattern): needs no
        // initialized mouse; mouseSetFrame(null,...) installs the default.
        Mouse.mouseSetFrame(null, 0, 0, 0, 0, 0, 0);

        Input.programIsActive = true;
    }

    @AfterEach
    void teardown() {
        GameMouse.gameMouseExit();
        GameMouse.resetForTests();
        Art.resetForTests();
        Db.resetForTests();
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        TextFont.resetForTests();
        Settings.settings.system.language = savedLanguage;

        Svga.scrSize.left = savedLeft;
        Svga.scrSize.top = savedTop;
        Svga.scrSize.right = savedRight;
        Svga.scrSize.bottom = savedBottom;
        Mouse.mouseBlit = null;
    }

    @Test
    void gameMouseVectors() {
        int initRc = GameMouse.gameMouseInit();
        int initAgain = GameMouse.gameMouseInit();
        int cursorAfterInit = GameMouse.gameMouseGetCursor();
        int setCross = GameMouse.gameMouseSetCursor(
                GameMouse.MouseCursorType.MOUSE_CURSOR_CROSSHAIR.id());
        int cursorCross = GameMouse.gameMouseGetCursor();
        int setCrossAgain = GameMouse.gameMouseSetCursor(
                GameMouse.MouseCursorType.MOUSE_CURSOR_CROSSHAIR.id());
        int setArrow = GameMouse.gameMouseSetCursor(
                GameMouse.MouseCursorType.MOUSE_CURSOR_ARROW.id());
        int cursorArrow = GameMouse.gameMouseGetCursor();
        assertEquals(0, initRc);
        assertEquals(-1, initAgain);
        assertEquals(1, cursorAfterInit);
        assertEquals(0, setCross);
        assertEquals(20, cursorCross);
        assertEquals(-1, setCrossAgain);
        assertEquals(0, setArrow);
        assertEquals(1, cursorArrow);

        int modeInit = GameMouse.gameMouseGetMode();
        assertEquals(0, modeInit);

        boolean scrollInit = GameMouse.gmouseScrollingIsEnabled();
        GameMouse.gmouseEnableScrolling();
        boolean scrollOn = GameMouse.gmouseScrollingIsEnabled();
        GameMouse.gmouseDisableScrolling();
        boolean scrollOff = GameMouse.gmouseScrollingIsEnabled();
        boolean clickInit = GameMouse.gmouseGetClickToScroll();
        GameMouse.gmouseSetClickToScroll(true);
        boolean clickOn = GameMouse.gmouseGetClickToScroll();
        assertTrue(scrollInit);
        assertTrue(scrollOn);
        assertFalse(scrollOff);
        assertFalse(clickInit);
        assertTrue(clickOn);

        assertTrue(GameMouse.gameMouseObjectsIsVisible());

        GameMouse.gameMouseExit();
    }

    @Test
    void gmouseEnableDisableVectors() {
        // [gmouseen] from docker/refharness/golden/v72.txt via sectionGmouseEn:
        // fresh init (v71 exited), scrolloff-first ordering, guarded no-ops.
        int initRc = GameMouse.gameMouseInit();
        GameMouse.gmouseDisableScrolling();
        int scrollOff = GameMouse.gmouseScrollingIsEnabled() ? 1 : 0;
        GameMouse.gmouseDisable(false);
        int disCursor = GameMouse.gameMouseGetCursor();
        int disScroll = GameMouse.gmouseScrollingIsEnabled() ? 1 : 0;
        GameMouse.gmouseDisable(false);
        int disAgainCursor = GameMouse.gameMouseGetCursor();
        int disAgainScroll = GameMouse.gmouseScrollingIsEnabled() ? 1 : 0;
        GameMouse.gmouseEnable();
        int enCursor = GameMouse.gameMouseGetCursor();
        int enScroll = GameMouse.gmouseScrollingIsEnabled() ? 1 : 0;
        GameMouse.gmouseEnable();
        int enAgainCursor = GameMouse.gameMouseGetCursor();
        String line = "init=" + initRc + " scrolloff=" + scrollOff
                + " dis=" + disCursor + "," + disScroll
                + " disagain=" + disAgainCursor + "," + disAgainScroll
                + " en=" + enCursor + "," + enScroll
                + " enagain=" + enAgainCursor;
        assertEquals("init=0 scrolloff=0 dis=0,0 disagain=0,0 en=0,1 enagain=0", line);

        GameMouse.gameMouseExit();
    }
}
