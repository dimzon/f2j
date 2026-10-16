package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.Svga.ShowRect;

/**
 * {@code [windlg]} vectors from {@code docker/refharness/golden/v42.txt}:
 * modal text dialogs ({@code _win_get_str}/{@code win_yes_no}/{@code _win_msg}/
 * {@code _create_pull_down}/{@code _win_input_str}/{@code win_get_num_i}/
 * {@code _calc_max_field_chars_wcursor}/{@code get_num_i}).
 *
 * <p>The C harness reuses {@code [winfrm]}'s live system (re-init fails with
 * 4), so its pulldown window takes id 4. Java re-inits cleanly, so this test
 * uses a fresh init: the pulldown window is id 1. All behavioral vectors
 * (rect, crc, return codes, strings, numbers) match verbatim.
 */
class WindowDlgTest {

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
    void winDlgVectors() {
        // Fresh init in Java (C re-init fails with 4): pulldown win is id 1.
        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(-1, Input.inputInit());
        Mouse.mouseSetFrame(null, 0, 0, 0, 0, 0, 0);
        MouseManager.mouseManagerShowMouse();
        Kb.keyboardSetLayout(Kb.KEYBOARD_LAYOUT_QWERTY);

        // Park the mouse where no dialog button can fire: modal pumps call
        // winCheckAllButtons first, so every key must come from kbGetch.
        Mouse.mouseSetPosition(639, 479);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();

        assertEquals(3, WindowManager.calcMaxFieldCharsWcursor(0, 99));
        assertEquals(4, WindowManager.calcMaxFieldCharsWcursor(-5, 100));
        assertEquals(2, WindowManager.calcMaxFieldCharsWcursor(7, 7));

        String[] items = { "New", "Load", "Exit" };
        Geometry.Rect pdRect = new Geometry.Rect();
        int pdWin = WindowManager.createPullDown(items, 3, 300, 200, 3, 5, pdRect);
        assertEquals(1, pdWin);
        assertEquals(300, pdRect.left);
        assertEquals(200, pdRect.top);
        assertEquals(335, pdRect.right);
        assertEquals(263, pdRect.bottom);
        assertEquals("BFA67474", String.format("%08X", winBufCrc(pdWin)));
        WindowManager.windowDestroy(pdWin);
        assertNull(WindowManager.windowGetWindow(pdWin));

        kbSim(SdlScancode.Y);
        assertEquals(1, WindowManager.winYesNo("Quit?", 200, 150, 3));
        kbSim(SdlScancode.N);
        assertEquals(0, WindowManager.winYesNo("Quit?", 200, 150, 3));

        kbSim(SdlScancode.ESCAPE);
        assertEquals(0, WindowManager.winMsg("Hello", 200, 150, 3));

        StringBuilder name = new StringBuilder();
        kbSim(SdlScancode.A);
        kbSim(SdlScancode.B);
        kbSim(SdlScancode.RETURN);
        assertEquals(0, WindowManager.winGetStr(name, 8, "Name", 200, 150));
        assertEquals("ab", name.toString());

        int inWin = WindowManager.windowCreate(10, 250, 200, 60, 5, 0);
        assertNotNull(WindowManager.windowGetWindow(inWin));
        WindowManager.windowShow(inWin);
        StringBuilder ibuf = new StringBuilder("Q");
        kbSim(SdlScancode.ESCAPE);
        assertEquals(-1, WindowManager.winInputStr(inWin, ibuf, 10, 16, 16, 3, 5));
        assertEquals("Q", ibuf.toString());
        StringBuilder tbuf = new StringBuilder("Hi");
        kbSim(SdlScancode.X);
        kbSim(SdlScancode.RETURN);
        assertEquals(0, WindowManager.winInputStr(inWin, tbuf, 10, 16, 16, 3, 5));
        assertEquals("Hix", tbuf.toString());
        WindowManager.windowDestroy(inWin);

        int[] numVal = { 5 };
        kbSim(SdlScancode.N4);
        kbSim(SdlScancode.N2);
        kbSim(SdlScancode.RETURN);
        assertEquals(0, WindowManager.winGetNumI(numVal, 0, 99, true, "Age", 200, 150));
        assertEquals(42, numVal[0]);
        int[] numVal2 = { 5 };
        kbSim(SdlScancode.ESCAPE);
        assertEquals(-1, WindowManager.winGetNumI(numVal2, 0, 99, true, "Age", 200, 150));
        assertEquals(5, numVal2[0]);

        WindowManager.windowManagerExit();
    }
}
