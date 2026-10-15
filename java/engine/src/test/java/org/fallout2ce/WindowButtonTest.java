package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.Svga.ShowRect;

/**
 * {@code [winbtn]} vectors from {@code docker/refharness/golden/v39.txt}:
 * button engine plus the {@code inputGetInput} path.
 */
class WindowButtonTest {

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
    void winBtnVectors() {
        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(-1, Input.inputInit());
        Mouse.mouseSetFrame(null, 0, 0, 0, 0, 0, 0);
        MouseManager.mouseManagerShowMouse();

        int win = WindowManager.windowCreate(10, 10, 120, 80, 5, 0);
        WindowManager.windowShow(win);

        int btn1 = WindowManager.buttonCreate(win, 5, 5, 40, 20, 11, 12, 13, 14, null, null, null, 0);
        int btn2 = WindowManager.buttonCreate(win, 50, 5, 40, 20, 21, 22, 23, 24, null, null, null,
            0x40);
        assertEquals(1, btn1);
        assertEquals(2, btn2);
        assertEquals(1, WindowManager.buttonGetWindowId(btn1));
        assertEquals(-1, WindowManager.winLastButtonWinID());

        Mouse.mouseSetPosition(35, 25);
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(11, WindowManager.winCheckAllButtons());
        Mouse.mouseSetPosition(115, 75);
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(12, WindowManager.winCheckAllButtons());

        Mouse.mouseSetPosition(35, 25);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        assertEquals(13, WindowManager.winCheckAllButtons());
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(14, WindowManager.winCheckAllButtons());

        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_RIGHT_BUTTON_DOWN);
        assertEquals(-1, WindowManager.winCheckAllButtons());
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(-1, WindowManager.winCheckAllButtons());
        assertEquals(0, WindowManager.buttonSetRightMouseCallbacks(btn1, 31, 32, null, null));
        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_RIGHT_BUTTON_DOWN);
        assertEquals(31, WindowManager.winCheckAllButtons());
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(32, WindowManager.winCheckAllButtons());

        int[] procCalls = { 0 };
        int[] procCode = { -99 };
        WindowManager.ButtonCallback enterProc = (btn, keyCode) -> {
            procCalls[0]++;
            procCode[0] = keyCode;
        };
        assertEquals(0, WindowManager.buttonSetMouseCallbacks(btn1, enterProc, null, null, null));
        Mouse.mouseSetPosition(115, 75);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        Mouse.mouseSetPosition(35, 25);
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(-1, WindowManager.winCheckAllButtons());
        assertEquals(1, procCalls[0]);
        assertEquals(11, procCode[0]);
        assertEquals(0, WindowManager.buttonSetMouseCallbacks(btn2, enterProc, null, null, null));
        Mouse.mouseSetPosition(115, 75);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        Mouse.mouseSetPosition(80, 25);
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(21, WindowManager.winCheckAllButtons());

        int checkable = WindowManager.ButtonFlags.BUTTON_FLAG_CHECKABLE.id();
        int btn3 = WindowManager.buttonCreate(win, 5, 30, 40, 20, 41, 42, 33, 34, null, null, null,
            checkable);
        Mouse.mouseSetPosition(35, 55);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        WindowManager.winCheckAllButtons();
        WindowManager.winCheckAllButtons();
        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        assertEquals(-1, WindowManager.winCheckAllButtons());
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(33, WindowManager.winCheckAllButtons());
        assertTrue(WindowManager.winButtonDown(btn3));

        assertEquals(0, WindowManager.buttonDisable(btn3));
        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        assertEquals(-1, WindowManager.winCheckAllButtons());
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        assertEquals(0, WindowManager.buttonEnable(btn3));
        assertEquals(42, Input.inputGetInput());

        int btn4 = WindowManager.buttonCreate(win, 5, 55, 30, 15, 51, 52, 53, 54, null, null, null,
            checkable);
        int btn5 = WindowManager.buttonCreate(win, 40, 55, 30, 15, 61, 62, 63, 64, null, null, null,
            checkable);
        assertEquals(0, WindowManager.winGroupRadioButtons(2, new int[] { btn4, btn5 }));
        assertEquals(0, WindowManager.winSetButtonRestState(btn4, true, 0));
        assertTrue(WindowManager.winButtonDown(btn4));
        Mouse.mouseSetPosition(20, 72);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        WindowManager.winCheckAllButtons();
        WindowManager.winCheckAllButtons();
        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        WindowManager.winCheckAllButtons();
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        assertFalse(WindowManager.winButtonDown(btn4));
        assertFalse(WindowManager.winButtonDown(btn5));

        assertEquals(0, WindowManager.winSetButtonRestState(btn5, true, 1));
        assertEquals(41, Input.inputGetInput());

        assertEquals(0, WindowManager.winButtonPressAndRelease(btn1));
        assertEquals(63, Input.inputGetInput());
        assertEquals(13, Input.inputGetInput());

        int textBtn = WindowManager.winRegisterTextButton(win, 75, 50, 71, 72, 73, 74, "OK", 0);
        assertEquals(6, textBtn);
        assertEquals("69F37E79", String.format("%08X", winBufCrc(win)));

        byte[] normal = new byte[40 * 20];
        for (int i = 0; i < normal.length; i++) {
            normal[i] = (byte) (i & 0x3F);
        }
        int gfxBtn = WindowManager.buttonCreate(win, 75, 5, 40, 20, 81, 82, 83, 84, normal, null,
            null, 0);
        WindowManager.windowRefresh(win);
        assertEquals(7, gfxBtn);
        assertEquals("38A2D861", String.format("%08X", winBufCrc(win)));

        byte[] mask = new byte[40 * 20];
        mask[10 * 40 + 20] = 1;
        assertEquals(0, WindowManager.buttonSetMask(gfxBtn, mask));
        Mouse.mouseSetPosition(10 + 75 + 20, 10 + 5 + 10);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        assertEquals(81, WindowManager.winCheckAllButtons());
        Mouse.mouseSetPosition(10 + 75 + 21, 10 + 5 + 10);
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(82, WindowManager.winCheckAllButtons());
        assertEquals(-1, WindowManager.winCheckAllButtons());

        assertEquals(42, WindowManager.gnwCheckMenuBars(42));

        Input.enqueueInputEvent(55);
        assertEquals(14, Input.inputGetInput());
        assertEquals(55, Input.inputGetInput());
        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        assertEquals(-2, Input.inputGetInput());
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        assertEquals(1, WindowManager.winLastButtonWinID());

        assertEquals(0, WindowManager.buttonDestroy(btn2));
        assertEquals(-1, WindowManager.buttonGetWindowId(btn2));

        WindowManager.windowManagerExit();
    }
}
