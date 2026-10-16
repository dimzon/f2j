package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.Svga.ShowRect;

/**
 * {@code [winfrm]} vectors from {@code docker/refharness/golden/v41.txt}:
 * FRM-backed buttons ({@code buttonCreateWithFrm}/{@code buttonSetDisabledFrm}).
 *
 * <p>The C harness reuses {@code [winbtn]}/{@code [winmenu]}'s live system
 * (re-init fails), so its FRM button takes id 9. Java re-inits cleanly, so
 * this test uses a fresh init: the window is id 1 and the FRM button is id 1.
 * All behavioral vectors (sizes, crcs, event codes, return codes) match
 * verbatim.
 */
class WindowFrmTest {

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
        Art.resetForTests();
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
        // Extra mounts for artInit (interface lists live in master.dat,
        // critter/head lists live in critter.dat); missing files are skipped.
        if (Files.isRegularFile(Paths.get(gameEn, "patch000.dat"))) {
            XFile.xbaseOpen(gameEn + "/patch000.dat");
        }
        if (Files.isRegularFile(Paths.get(gameEn, "critter.dat"))) {
            XFile.xbaseOpen(gameEn + "/critter.dat");
        }
        assertEquals(0, Art.artInit());
        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(Color.colorPaletteLoad("color.pal"));

        Input.programIsActive = true;
    }

    @AfterEach
    void teardown() {
        WindowManager.windowManagerExit();
        WindowManager.resetForTests();
        Art.resetForTests();
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
    void winFrmVectors() {
        // Fresh init in Java (C re-init fails with 4): window id 1, button id 1.
        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(-1, Input.inputInit());
        Mouse.mouseSetFrame(null, 0, 0, 0, 0, 0, 0);
        MouseManager.mouseManagerShowMouse();

        int win = WindowManager.windowCreate(10, 100, 200, 100, 5, 0);
        assertEquals(1, win);
        WindowManager.windowShow(win);

        FrmId empty = FrmId.emptyId();
        FrmId upId = FrmId.ofType(ObjectType.INTERFACE,
            ArtDefs.InterfaceFrameId.LittleRedButtonUp.id());
        FrmId downId = FrmId.ofType(ObjectType.INTERFACE,
            ArtDefs.InterfaceFrameId.LittleRedButtonDown.id());

        // Empty FRM id locks nothing.
        assertEquals(-1,
            WindowManager.buttonCreateWithFrm(win, 5, 5, 111, 112, 113, 114, empty, empty, empty, 0));

        int frmBtn = WindowManager.buttonCreateWithFrm(win, 5, 5, 111, 112, 113, 114,
            upId, downId, empty, 0);
        assertEquals(1, frmBtn);

        // Freshly created buttons sit at the list head.
        WindowManager.Window frmWinPre = WindowManager.windowGetWindow(win);
        assertNotNull(frmWinPre);
        WindowManager.Button frmButton = frmWinPre.buttonListHead;
        assertNotNull(frmButton);
        int frmW = frmButton.rect.right - frmButton.rect.left + 1;
        int frmH = frmButton.rect.bottom - frmButton.rect.top + 1;
        assertEquals(15, frmW);
        assertEquals(16, frmH);
        assertEquals("4DDF7C2A", String.format("%08X", winBufCrc(win)));

        // Click it: hover / exit / down / up codes come from the FRM button.
        Geometry.Rect winRect = new Geometry.Rect();
        assertEquals(0, WindowManager.windowGetRect(win, winRect));
        int cx = winRect.left + 5 + frmW / 2;
        int cy = winRect.top + 5 + frmH / 2;

        Mouse.mouseSetPosition(cx, cy);
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(111, WindowManager.winCheckAllButtons());
        Mouse.mouseSetPosition(150, 180);
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(112, WindowManager.winCheckAllButtons());
        Mouse.mouseSetPosition(cx, cy);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();
        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        assertEquals(113, WindowManager.winCheckAllButtons());
        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(114, WindowManager.winCheckAllButtons());

        // Disabled FRM images: unknown button is rejected, real one repaints.
        // dis-crc==en-crc is no special case: the shared art-cache memory
        // aliases normal/disabledPressed (and pressed/disabledNormal), so each
        // clip-piece repaint toggles the image and disable/enable converge.
        assertEquals(-1, WindowManager.buttonSetDisabledFrm(-1, upId, downId, empty));
        assertEquals(0, WindowManager.buttonSetDisabledFrm(frmBtn, downId, upId, empty));
        WindowManager.buttonDisable(frmBtn);
        assertEquals("98B4150A", String.format("%08X", winBufCrc(win)));
        WindowManager.buttonEnable(frmBtn);
        assertEquals("98B4150A", String.format("%08X", winBufCrc(win)));

        // Raw image re-register: graphic button accepts, text button is rejected.
        int textBtn = WindowManager.winRegisterTextButton(win, 5, 60, 121, 122, 123, 124, "OK", 0);
        assertEquals(2, textBtn);
        Art.FrmImage imgLock = new Art.FrmImage();
        assertEquals(1, imgLock.lock(upId) ? 1 : 0);
        assertEquals(0, WindowManager.winRegisterButtonImage(frmBtn, imgLock.getData(), null, null, true));
        assertEquals("9BE0C6FD", String.format("%08X", winBufCrc(win)));
        assertEquals(-1,
            WindowManager.winRegisterButtonImage(textBtn, imgLock.getData(), null, null, true));
        imgLock.close();

        assertEquals(0, WindowManager.buttonDestroy(frmBtn));
        assertEquals(-1, WindowManager.buttonGetWindowId(frmBtn));
        assertNull(findButton(WindowManager.windowGetWindow(win).buttonListHead, frmBtn));

        WindowManager.windowManagerExit();
    }

    private static WindowManager.Button findButton(WindowManager.Button head, int id) {
        WindowManager.Button curr = head;
        while (curr != null) {
            if (curr.id == id) {
                return curr;
            }
            curr = curr.next;
        }
        return null;
    }
}
