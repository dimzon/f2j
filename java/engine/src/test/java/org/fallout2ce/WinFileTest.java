package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [winfile]} vectors from
 * {@code docker/refharness/golden/v46.txt}: the load/save file dialogs
 * ({@code showLoadFileDialog}/{@code showSaveFileDialog}).
 *
 * <p>Dialog button codes (500/501) and typing keys have no SDL scancodes,
 * and {@code _process_bk} appends kb keys after queued events, so every seed
 * travels via {@link Input#enqueueInputEvent} in consumption order (never
 * kbSim). The mouse parks at 639,479 so the modal pumps never see button
 * input. The empty-list load passes an empty {@code String[]} with length 0
 * (C passes {@code nullptr} + 0).
 *
 * <p>{@code DBOX.MSG} loads from game data through the VFS (a load failure
 * returns -1 from the dialogs, so the 0/1 assertions self-check it).
 */
class WinFileTest {

    private int savedLeft;
    private int savedTop;
    private int savedRight;
    private int savedBottom;
    private String savedLanguage;
    private Svga.ShowRect savedScrBlit;

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
        assertEquals(true, XFile.xbaseOpen(gameEn + "/master.dat"));
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
        assertEquals(true, Color.colorPaletteLoad("color.pal"));

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
    void winFileVectors() {
        // The C harness reuses the live system from earlier sections, so
        // re-init fails with 4 there; the double init below reproduces
        // wminit-again=4 verbatim on a fresh Java system.
        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(4, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(-1, Input.inputInit());
        Mouse.mouseSetFrame(null, 0, 0, 0, 0, 0, 0);
        MouseManager.mouseManagerShowMouse();
        Kb.keyboardSetLayout(Kb.KEYBOARD_LAYOUT_QWERTY);

        // Park the mouse away from every button: the modal pumps below read
        // keys from the queued events, mouse input would corrupt them.
        Mouse.mouseSetPosition(639, 479);
        Mouse.mouseSimulateInput(0, 0, 0);
        WindowManager.winCheckAllButtons();

        String[] slots = { "slot1", "slot2", "slot3" };

        // NOTE: C runs the ESC case first but prints load-rc first.
        StringBuilder loadEscDest = new StringBuilder("?");
        Input.enqueueInputEvent(Kb.KEY_ESCAPE);
        int loadEscRc = DBox.showLoadFileDialog("Load", slots, loadEscDest, 3, 200, 100, 0);

        StringBuilder loadDest = new StringBuilder("?");
        Input.enqueueInputEvent(Kb.KEY_ARROW_DOWN);
        Input.enqueueInputEvent(Kb.KEY_ARROW_DOWN);
        Input.enqueueInputEvent(500);
        int loadRc = DBox.showLoadFileDialog("Load", slots, loadDest, 3, 200, 100, 0);
        assertEquals(0, loadRc);
        assertEquals("slot3", loadDest.toString());
        assertEquals(1, loadEscRc);
        assertEquals("?", loadEscDest.toString());

        StringBuilder emptyDest = new StringBuilder("?");
        Input.enqueueInputEvent(500);
        int loadEmptyRc = DBox.showLoadFileDialog("Load", new String[0], emptyDest, 0, 200, 100, 0);
        assertEquals(1, loadEmptyRc);
        assertEquals("?", emptyDest.toString());

        StringBuilder saveDest = new StringBuilder("");
        Input.enqueueInputEvent('x');
        Input.enqueueInputEvent('y');
        Input.enqueueInputEvent(500);
        int saveRc = DBox.showSaveFileDialog("Save", slots, saveDest, 3, 200, 100, 0);
        assertEquals(0, saveRc);
        assertEquals("xy", saveDest.toString());

        StringBuilder saveEmptyDest = new StringBuilder("");
        Input.enqueueInputEvent(500);
        int saveEmptyRc = DBox.showSaveFileDialog("Save", slots, saveEmptyDest, 3, 200, 100, 0);
        assertEquals(1, saveEmptyRc);

        StringBuilder saveEscDest = new StringBuilder("?");
        Input.enqueueInputEvent(Kb.KEY_ESCAPE);
        int saveEscRc = DBox.showSaveFileDialog("Save", slots, saveEscDest, 3, 200, 100, 0);
        assertEquals(1, saveEscRc);
        assertEquals("?", saveEscDest.toString());

        WindowManager.windowManagerExit();
    }
}
