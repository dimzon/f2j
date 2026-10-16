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
 * {@code [windbg]} + {@code [windbox]} vectors from
 * {@code docker/refharness/golden/v45.txt}: the debug window
 * ({@code _win_debug}/{@code _win_debug_delete}) and the dialog boxes
 * ({@code showDialogBox}/{@code showInputDialog}).
 *
 * <p>The C harness reuses the live system from earlier sections (re-init
 * fails with 4), so its debug window takes id 4. Java re-inits cleanly per
 * test, so the double-init reproduces {@code wminit-again=4} verbatim while
 * the debug window itself is id 1: it is resolved via
 * {@code windowGetAtPoint(230, 176)} like the harness and asserted for
 * in-test consistency (recreate reuses the id). All behavioral vectors
 * (return codes, buffer crcs {@code 5831104D}/{@code 5C29DAC8}, gone flags,
 * dialog results, input strings) match verbatim.
 *
 * <p>{@code DBOX.MSG} loads from game data through the VFS (a load failure
 * returns -1 from the dialogs, so the 0/1 assertions self-check it).
 */
class WinDbgDboxTest {

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
        DBox.resetForTests();
        Art.resetForTests();
        Db.resetForTests();
        TextFont.resetForTests();
        InterfaceFont.resetForTests();

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

        // Mirror game.cc: modern (interface) fonts back font ids 100+.
        assertEquals(0, InterfaceFont.interfaceFontsInit());
        assertEquals(0, TextFont.fontManagerAdd(100, 110, InterfaceFont.IMPL));
        TextFont.fontSetCurrent(100);

        Input.programIsActive = true;
    }

    @AfterEach
    void teardown() {
        WindowManager.windowManagerExit();
        WindowManager.resetForTests();
        DBox.resetForTests();
        Art.resetForTests();
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        Db.resetForTests();
        TextFont.resetForTests();
        InterfaceFont.resetForTests();
        Settings.settings.system.language = savedLanguage;

        Svga.scrSize.left = savedLeft;
        Svga.scrSize.top = savedTop;
        Svga.scrSize.right = savedRight;
        Svga.scrSize.bottom = savedBottom;
        Svga.scrBlit = savedScrBlit;
        Mouse.mouseBlit = null;
    }

    @Test
    void winDbgVectors() {
        // The C harness reuses [wintext]'s live system, so re-init fails
        // with 4 there; the double init below reproduces wminit-again=4
        // verbatim on a fresh Java system.
        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(4, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(-1, Input.inputInit());

        assertEquals(0, WindowManager.winDebug("Hi"));
        // Debug window sits at 80,80; its center hits nothing else alive.
        int dbgWin = WindowManager.windowGetAtPoint(230, 176);
        assertEquals("5831104D", String.format("%08X", winBufCrc(dbgWin)));
        assertEquals(0, WindowManager
            .winDebug("Hello world, this is a long debug line that wraps!\nsecond"));
        assertEquals("5C29DAC8", String.format("%08X", winBufCrc(dbgWin)));

        // The Close-button callback destroys the window; the next message
        // recreates it from scratch.
        WindowManager.winDebugDelete(-1, -1);
        assertNull(WindowManager.windowGetWindow(dbgWin));
        assertEquals(0, WindowManager.windowGetAtPoint(230, 176));
        assertEquals(0, WindowManager.winDebug("Again"));
        int dbgWin3 = WindowManager.windowGetAtPoint(230, 176);
        assertEquals(dbgWin, dbgWin3);
        WindowManager.winDebugDelete(-1, -1);

        WindowManager.windowManagerExit();
    }

    @Test
    void winDboxVectors() {
        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(4, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(-1, Input.inputInit());
        Mouse.mouseSetFrame(null, 0, 0, 0, 0, 0, 0);
        MouseManager.mouseManagerShowMouse();
        Kb.keyboardSetLayout(Kb.KEYBOARD_LAYOUT_QWERTY);

        // Park the mouse away from every button: the modal pumps below read
        // keys from kbGetch, mouse input would corrupt the sequences.
        Mouse.mouseSetPosition(639, 479);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();

        String[] body = { "line one", "line two" };
        kbSim(SdlScancode.ESCAPE);
        assertEquals(0, DBox.showDialogBox("Hi", body, 2, 200, 150, 3, null, 5, 0));
        kbSim(SdlScancode.Y);
        assertEquals(1,
            DBox.showDialogBox("Hi", body, 2, 200, 150, 3, null, 5, DBox.DIALOG_BOX_YES_NO));
        kbSim(SdlScancode.N);
        assertEquals(0,
            DBox.showDialogBox("Hi", body, 2, 200, 150, 3, null, 5, DBox.DIALOG_BOX_YES_NO));
        kbSim(SdlScancode.ESCAPE);
        assertEquals(0,
            DBox.showDialogBox("Hi", body, 2, 200, 150, 3, null, 5, DBox.DIALOG_BOX_NO_BUTTONS));

        kbSim(SdlScancode.RETURN);
        // Static-buffer copy pattern (C must copy before the next call).
        String inpAb = new String(notNull(DBox.showInputDialog("ab", 200, 150, "Done")));
        kbSim(SdlScancode.BACKSPACE);
        kbSim(SdlScancode.C);
        kbSim(SdlScancode.RETURN);
        String inpAc = new String(notNull(DBox.showInputDialog("ab", 200, 150, "Done")));
        kbSim(SdlScancode.ESCAPE);
        assertNull(DBox.showInputDialog("ab", 200, 150, "Done"));
        assertEquals("ab", inpAb);
        assertEquals("ac", inpAc);

        WindowManager.windowManagerExit();
    }

    private static String notNull(String value) {
        assertNotNull(value);
        return value;
    }
}
