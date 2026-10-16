package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.Svga.ShowRect;

/**
 * {@code [winmenu2]} vectors from {@code docker/refharness/golden/v43.txt}:
 * interactive pulldowns ({@code win_pull_down}/{@code process_pull_down}),
 * menu-bar processing ({@code _GNW_process_menu}) and list selection
 * ({@code _win_list_select}/{@code _win_list_select_at}).
 *
 * <p>The C harness reuses the live system (re-init fails with 4, so its
 * windows keep climbing ids). Java re-inits cleanly: init returns 0 and the
 * menu window is id 1. No window ids are printed in this section, so every
 * other vector matches verbatim.
 */
class WindowMenu2Test {

    private int savedLeft;
    private int savedTop;
    private int savedRight;
    private int savedBottom;
    private String savedLanguage;
    private ShowRect savedScrBlit;

    static String cbItems;
    static int cbIndex;

    static String gameData(String name) throws Exception {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path p = Path.of(root, name);
        if (!Files.isRegularFile(p)) {
            throw new IllegalStateException("missing " + p);
        }
        return p.toString();
    }

    static void kbSim(int scancode) {
        Kb.kbSimulateKey(new DInput.KeyboardData(scancode, true));
    }

    @BeforeEach
    void setup() throws Exception {
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        WindowManager.resetForTests();
        Db.resetForTests();
        TextFont.resetForTests();

        savedLeft = Svga.scrSize.left;
        savedTop = Svga.scrSize.top;
        savedRight = Svga.scrSize.right;
        savedBottom = Svga.scrSize.bottom;
        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;

        savedScrBlit = Svga.scrBlit;
        Svga.scrBlit = (src, srcPitch, unused, srcX, srcY, srcWidth, srcHeight, destX, destY) -> {
        };
        Mouse.mouseBlit = (src, srcPitch, unused, srcX, srcY, srcWidth, srcHeight, destX, destY) -> {
        };

        savedLanguage = Settings.settings.system.language;
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(Color.colorPaletteLoad("color.pal"));

        Input.programIsActive = true;
    }

    @AfterEach
    void teardown() {
        WindowManager.windowManagerExit();
        WindowManager.resetForTests();
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        Db.resetForTests();
        TextFont.resetForTests();
        Settings.settings.system.language = savedLanguage;

        Svga.scrSize.left = savedLeft;
        Svga.scrSize.top = savedTop;
        Svga.scrSize.right = savedRight;
        Svga.scrSize.bottom = savedBottom;
        Svga.scrBlit = savedScrBlit;
        Mouse.mouseBlit = null;
    }

    @Test
    void winMenu2Vectors() {
        // Fresh init in Java (C reuses the live system, so its re-init fails
        // with 4): init returns 0 here.
        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(-1, Input.inputInit());
        Mouse.mouseSetFrame(null, 0, 0, 0, 0, 0, 0);
        MouseManager.mouseManagerShowMouse();
        Kb.keyboardSetLayout(Kb.KEYBOARD_LAYOUT_QWERTY);

        // Park the mouse away from every button: the modal pumps below read
        // keys from kbGetch, mouse input would corrupt the sequences.
        Mouse.mouseSetPosition(639, 479);
        Mouse.mouseSimulateInput(0, 0, 0);

        // Standalone pulldown (no menu bar): arrows skip the "" separator.
        String[] navItems = { "A", "", "C" };
        kbSim(SdlScancode.DOWN);
        kbSim(SdlScancode.DOWN);
        kbSim(SdlScancode.DOWN);
        kbSim(SdlScancode.RETURN);
        assertEquals(2, WindowManager.winPullDown(navItems, 3, 300, 200, 3));
        kbSim(SdlScancode.ESCAPE);
        assertEquals(-1, WindowManager.winPullDown(navItems, 3, 300, 200, 3));

        // Menu-bar processing on a registered bar: DOWN+RETURN picks item 0
        // of pulldown 0 (returns its keyCode, enqueues the item index).
        int mwin = WindowManager.windowCreate(10, 300, 200, 60, 5, 0);
        assertEquals(1, mwin);
        WindowManager.windowShow(mwin);
        assertEquals(0, WindowManager.winRegisterMenuBar(mwin, 0, 0, 200, 12, 3, 5));
        String[] gameItems = { "New", "Load", "Exit" };
        String[] optItems = { "On", "Off" };
        assertEquals(0, WindowManager.winRegisterMenuPulldown(mwin, 2, "Game", 101, 3, gameItems, 3, 5));
        assertEquals(0, WindowManager.winRegisterMenuPulldown(mwin, 60, "Opt", 102, 2, optItems, 3, 5));
        WindowManager.MenuBar menuBar = WindowManager.windowGetWindow(mwin).menuBar;
        assertNotNull(menuBar);
        kbSim(SdlScancode.DOWN);
        kbSim(SdlScancode.RETURN);
        assertEquals(101, WindowManager.gnwProcessMenu(menuBar, 0));
        assertEquals(0, Input.inputGetInput());
        // RIGHT switches to pulldown 1, ESC aborts the whole menu.
        kbSim(SdlScancode.RIGHT);
        kbSim(SdlScancode.ESCAPE);
        assertEquals(-1, WindowManager.gnwProcessMenu(menuBar, 0));
        WindowManager.winDeleteMenuBar(mwin);
        WindowManager.windowDestroy(mwin);

        // List selection: immediate RETURN, arrows, ESC, callback, start offset.
        String[] files = { "alpha", "bravo", "CHARLIE", "delta" };
        kbSim(SdlScancode.RETURN);
        assertEquals(0, WindowManager.winListSelect("Pick", files, 4, null, 200, 100, 3));
        kbSim(SdlScancode.DOWN);
        kbSim(SdlScancode.DOWN);
        kbSim(SdlScancode.RETURN);
        assertEquals(2, WindowManager.winListSelect("Pick", files, 4, null, 200, 100, 3));
        kbSim(SdlScancode.ESCAPE);
        assertEquals(-1, WindowManager.winListSelect("Pick", files, 4, null, 200, 100, 3));

        cbItems = null;
        cbIndex = -99;
        kbSim(SdlScancode.DOWN);
        kbSim(SdlScancode.RETURN);
        kbSim(SdlScancode.ESCAPE);
        assertEquals(-1, WindowManager.winListSelect("Pick", files, 4, (items, index) -> {
            cbItems = String.join(",", items);
            cbIndex = index;
        }, 200, 100, 3));
        assertEquals(1, cbIndex);
        // NOTE: start>0 with a short list overreads the item array upstream
        // (_win_text(items+start, itemsLength)), so the offset/scroll paths use
        // a 12-item list (capacity is 10): start=2 selects index 2, 'g' jumps
        // to index 6 through the first-letter branch.
        String[] files12 = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l" };
        kbSim(SdlScancode.RETURN);
        assertEquals(2, WindowManager.winListSelectAt("Pick", files12, 12, null, 200, 100, 3, 2));
        kbSim(SdlScancode.G);
        kbSim(SdlScancode.RETURN);
        assertEquals(6, WindowManager.winListSelectAt("Pick", files12, 12, null, 200, 100, 3, 0));

        WindowManager.windowManagerExit();
    }
}
