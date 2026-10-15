package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.Geometry.Rect;
import org.fallout2ce.Svga.ShowRect;

/**
 * {@code [mousewin]} window vectors from
 * {@code docker/refharness/golden/v38.txt}.
 */
class WindowManagerTest {

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

    static int initBuffered() {
        return WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id());
    }

    @Test
    void initCreateDrawDestroy() {
        assertEquals(0, initBuffered());
        assertEquals(-1, Input.inputInit());

        int win = WindowManager.windowCreate(10, 20, 100, 50, 5, 0);
        assertEquals(1, win);
        assertEquals(100, WindowManager.windowGetWidth(win));
        assertEquals(50, WindowManager.windowGetHeight(win));

        Rect rect = new Rect();
        assertEquals(0, WindowManager.windowGetRect(win, rect));
        assertEquals(10, rect.left);
        assertEquals(20, rect.top);
        assertEquals(109, rect.right);
        assertEquals(69, rect.bottom);

        byte[] buf = WindowManager.windowGetBuffer(win);
        assertEquals("A73BFA77", String.format("%08X", crc(buf)));

        WindowManager.windowDrawBorder(win);
        assertEquals("357807D8", String.format("%08X", crc(buf)));

        WindowManager.windowDrawText(win, "Hi", 90, 2, 2, 3);
        assertEquals("9A0FA7B9", String.format("%08X", crc(buf)));

        WindowManager.windowDrawLine(win, 0, 0, 99, 49, 7);
        WindowManager.windowDrawRect(win, 5, 5, 20, 20, 9);
        WindowManager.windowFill(win, 10, 10, 20, 20, 11);
        assertEquals("BC78F380", String.format("%08X", crc(buf)));

        WindowManager.windowShow(win);
        assertEquals(1, WindowManager.windowGetAtPoint(15, 25));
        WindowManager.windowHide(win);
        // Hidden windows still hit-test (C++ checks no HIDDEN flag here).
        assertEquals(1, WindowManager.windowGetAtPoint(15, 25));

        WindowManager.windowDestroy(win);
        assertNull(WindowManager.windowGetWindow(win));
        assertEquals(-1, WindowManager.windowGetWidth(win));
    }

    @Test
    void doubleInitFails() {
        assertEquals(0, initBuffered());
        assertEquals(4, WindowManager.windowManagerInit(() -> 0, () -> {
        }, 0));
    }

    @Test
    void invalidQueries() {
        assertEquals(0, initBuffered());
        assertEquals(-1, WindowManager.windowGetWidth(999));
        assertEquals(-1, WindowManager.windowGetHeight(999));
        assertNull(WindowManager.windowGetBuffer(999));
        assertNull(WindowManager.windowGetBuffer2D(999));
        assertEquals(-1, WindowManager.windowGetRect(999, new Rect()));
        assertFalse(WindowManager.windowIsValidWindowId(999));
        assertEquals(-1, WindowManager.windowGetAtPoint(639, 479 - 480));
        WindowManager.windowDestroy(999);
    }

    @Test
    void titleAndMessageBox() {
        List<String> titles = new java.util.ArrayList<>();
        WindowManager.titleHandler = titles::add;
        WindowManager.programWindowSetTitle("FALLOUT II");
        assertEquals(List.of("FALLOUT II"), titles);
        WindowManager.programWindowSetTitle(null);

        List<String> boxes = new java.util.ArrayList<>();
        WindowManager.messageBoxHandler = text -> {
            boxes.add(text);
            return true;
        };
        assertTrue(WindowManager.showMessageBox("boom"));
        assertEquals(List.of("boom"), boxes);
    }

    @Test
    void winTextAndBuffer2D() {
        assertEquals(0, initBuffered());
        int win = WindowManager.windowCreate(0, 0, 100, 40, 5, 0);
        WindowManager.winText(win, new String[] { "Hi", "" }, 90, 2, 2, 3);
        Draw.Buffer view = WindowManager.windowGetBuffer2D(win);
        assertEquals(100, view.width());
        assertEquals(40, view.height());
        WindowManager.winBuffering(false);
        WindowManager.winBuffering(true);
        WindowManager.windowDestroy(win);
    }
}
