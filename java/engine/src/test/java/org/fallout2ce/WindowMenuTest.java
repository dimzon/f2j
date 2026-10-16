package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.Svga.ShowRect;

/**
 * {@code [winmenu]} vectors from {@code docker/refharness/golden/v40.txt}:
 * menu bars/pulldowns plus timed messages.
 *
 * <p>The C harness reuses {@code [winbtn]}'s live system (re-init fails on
 * process-lifetime font managers), so its menu window takes the next free id
 * and timed windows scan as ids 3,4. Java re-init works, so this test uses a
 * fresh init: the menu window is id 1 and timed windows are ids 2,3. All
 * behavioral vectors (return codes, counts, crc, rects, click flow) match.
 */
class WindowMenuTest {

    private int savedLeft;
    private int savedTop;
    private int savedRight;
    private int savedBottom;
    private String savedLanguage;
    private ShowRect savedScrBlit;

    static long crc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

    static long winBufCrc(int win) {
        byte[] buf = WindowManager.windowGetBuffer(win);
        return buf != null ? crc(buf) : 0;
    }

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
    void winMenuVectors() {
        // Fresh init in Java (C re-init fails with 4): menu win is id 1.
        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(-1, Input.inputInit());
        Mouse.mouseSetFrame(null, 0, 0, 0, 0, 0, 0);
        MouseManager.mouseManagerShowMouse();

        int win = WindowManager.windowCreate(10, 10, 200, 60, 5, 0);
        assertEquals(1, win);
        WindowManager.windowShow(win);

        // Pulldown without a bar is rejected.
        assertEquals(-1, WindowManager.winRegisterMenuPulldown(win, 2, "Nope", 99, 1,
            new String[] { "Nope" }, 3, 5));
        assertEquals(0, WindowManager.winRegisterMenuBar(win, 0, 0, 200, 12, 3, 5));
        assertEquals(-1, WindowManager.winRegisterMenuBar(win, 0, 0, 200, 12, 3, 5));

        // Bar wider than its window is rejected (separate small window).
        int smallWin = WindowManager.windowCreate(300, 10, 40, 30, 5, 0);
        assertEquals(-1, WindowManager.winRegisterMenuBar(smallWin, 0, 0, 100, 12, 3, 5));
        WindowManager.windowDestroy(smallWin);

        assertEquals(0, WindowManager.winRegisterMenuPulldown(win, 2, "Game", 101, 3,
            new String[] { "New", "Load", "Exit" }, 3, 5));
        assertEquals(0, WindowManager.winRegisterMenuPulldown(win, 60, "Opt", 102, 2,
            new String[] { "On", "Off" }, 3, 5));
        WindowManager.Window menuWin = WindowManager.windowGetWindow(win);
        assertNotNull(menuWin);
        assertNotNull(menuWin.menuBar);
        assertEquals(2, menuWin.menuBar.pulldownsLength);
        assertEquals("30F44E81", String.format("%08X", winBufCrc(win)));

        // Pure helpers: first-letter search is case-insensitive, width is max.
        String[] names = { "New", "Load", "Exit" };
        assertEquals(1, WindowManager.findFirstLetter('l', names, 3));
        assertEquals(2, WindowManager.findFirstLetter('E', names, 3));
        assertEquals(-1, WindowManager.findFirstLetter('z', names, 3));
        assertEquals(32, WindowManager.winWidthNeeded(names, 3));
        assertEquals(42, WindowManager.gnwCheckMenuBars(42));

        // Deleting the bar clears it; re-register works and passthrough holds.
        WindowManager.winDeleteMenuBar(win);
        assertEquals(42, WindowManager.gnwCheckMenuBars(42));
        assertEquals(0, WindowManager.winRegisterMenuBar(win, 0, 0, 200, 12, 3, 5));
        WindowManager.winDeleteMenuBar(win);

        // Timed messages: two slots, click-dismiss each via its full-window button.
        // Timed windows carry WINDOW_MOVE_ON_TOP; locate them by flag scan (C ids
        // 3,4, Java ids 2,3 on the fresh init).
        assertEquals(0, WindowManager.winTimedMsg("Hi", 3));
        assertEquals(0, WindowManager.winTimedMsg("Bye", 3));
        List<Integer> tmWins = new ArrayList<>();
        for (int id = 1; id < 20 && tmWins.size() < 2; id++) {
            WindowManager.Window w = WindowManager.windowGetWindow(id);
            if (w != null && (w.flags & WindowManager.WindowFlags.WINDOW_MOVE_ON_TOP.id()) != 0) {
                tmWins.add(id);
            }
        }
        assertEquals(List.of(2, 3), tmWins);
        int tmWin1 = tmWins.get(0);
        int tmWin2 = tmWins.get(1);
        Geometry.Rect r1 = new Geometry.Rect();
        assertEquals(0, WindowManager.windowGetRect(tmWin1, r1));
        Geometry.Rect r2 = new Geometry.Rect();
        assertEquals(0, WindowManager.windowGetRect(tmWin2, r2));
        assertEquals(297, r1.left);
        assertEquals(119, r1.top);
        assertEquals(341, r1.right);
        assertEquals(166, r1.bottom);
        assertEquals(291, r2.left);
        assertEquals(178, r2.top);
        assertEquals(346, r2.right);
        assertEquals(225, r2.bottom);

        Mouse.mouseSetPosition((r1.left + r1.right) / 2, (r1.top + r1.bottom) / 2);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        WindowManager.winCheckAllButtons();
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        assertNull(WindowManager.windowGetWindow(tmWin1));
        assertNotNull(WindowManager.windowGetWindow(tmWin2));

        Mouse.mouseSetPosition((r2.left + r2.right) / 2, (r2.top + r2.bottom) / 2);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        WindowManager.winCheckAllButtons();
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        assertNull(WindowManager.windowGetWindow(tmWin2));
        assertFalse(WindowManager.windowIsValidWindowId(tmWin2));

        WindowManager.windowManagerExit();
    }
}
