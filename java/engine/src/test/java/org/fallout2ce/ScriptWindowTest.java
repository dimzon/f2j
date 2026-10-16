package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [wintext]} vectors from {@code docker/refharness/golden/v44.txt}:
 * script-window text/buffer core ({@code windowGetFont}/{@code windowSetFont},
 * text flags/colors, {@code windowPrintBuf} alignment, {@code windowWordWrap},
 * {@code windowWrapLine}[{@code WithSpacing}], {@code _drawScaled}/
 * {@code _drawScaledBuf}, {@code _fillBuf3x3}).
 *
 * <p>The C harness reuses a live system (re-init fails with 4), so Java
 * inits once (rc 0) and then re-inits to reproduce {@code wminit-again=4};
 * {@code input-again=-1} follows the same double-init pattern as
 * {@code WindowDlgTest}. Window ids are never printed, so the fresh-init id
 * needs no adaptation.
 */
class ScriptWindowTest {

    private int savedLeft;
    private int savedTop;
    private int savedRight;
    private int savedBottom;
    private String savedLanguage;
    private Svga.ShowRect savedScrBlit;

    static long crc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

    static long winBufCrc(int win) {
        byte[] buf = WindowManager.windowGetBuffer(win);
        return buf != null ? crc(buf) : 0;
    }

    static String hex(long crc) {
        return String.format("%08X", crc);
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
        ScriptWindow.resetForTests();

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
        Settings.settings.system.language = Message.ENGLISH;
        assertEquals(true, Color.colorPaletteLoad("color.pal"));

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
        ScriptWindow.resetForTests();
        Settings.settings.system.language = savedLanguage;

        Svga.scrSize.left = savedLeft;
        Svga.scrSize.top = savedTop;
        Svga.scrSize.right = savedRight;
        Svga.scrSize.bottom = savedBottom;
        Svga.scrBlit = savedScrBlit;
        Mouse.mouseBlit = null;
    }

    @Test
    void winTextVectors() {
        // Fresh init, then re-init to reproduce the C live-system vectors.
        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(4, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        assertEquals(-1, Input.inputInit());

        // Widget font/text attributes (no window needed); font restored inline
        // like the C harness (plus TextFont/ScriptWindow resets in teardown).
        int fontBefore = ScriptWindow.windowGetFont();
        int setFontRc = ScriptWindow.windowSetFont(1);
        int fontAfter = ScriptWindow.windowGetFont();
        int setFlagsRc = ScriptWindow.scriptWindowSetTextFlags(
            Color.DRAW_TEXT_FLAG_NO_BG | Color.DRAW_TEXT_FLAG_UNDERLINED);
        int flagsAfter = ScriptWindow.scriptWindowGetTextFlags();
        int setColorRc = ScriptWindow.scriptWindowSetTextColor(1.0f, 0.0f, 0.0f);
        int textColor = ScriptWindow.scriptWindowGetTextColor();
        int setHiRc = ScriptWindow.scriptWindowSetHighlightColor(0.0f, 1.0f, 0.0f);
        int hiColor = ScriptWindow.scriptWindowGetHighlightColor();
        ScriptWindow.scriptWindowResetTextAttributes();
        int resetFlags = ScriptWindow.scriptWindowGetTextFlags();
        int resetColor = ScriptWindow.scriptWindowGetTextColor();
        ScriptWindow.windowSetFont(fontBefore);

        assertEquals(0, fontBefore);
        assertEquals(1, setFontRc);
        assertEquals(1, fontAfter);
        assertEquals(1, setFlagsRc);
        assertEquals(33685504, flagsAfter);
        assertEquals(1, setColorRc);
        assertEquals(133, textColor);
        assertEquals(1, setHiRc);
        assertEquals(215, hiColor);
        assertEquals(33619968, resetFlags);
        assertEquals(220, resetColor);

        int win = WindowManager.windowCreate(10, 370, 200, 100, 5, 0);
        WindowManager.windowShow(win);

        // Alignment variants paint at different x offsets; maxY guard is a no-op.
        ScriptWindow.windowPrintBuf(win, "Hi", 2, 100, 1000, 2, 2, 3,
            Window.TextAlignment.TEXT_ALIGNMENT_LEFT.id());
        long leftCrc = winBufCrc(win);
        ScriptWindow.windowPrintBuf(win, "Hi", 2, 100, 1000, 2, 20, 3,
            Window.TextAlignment.TEXT_ALIGNMENT_CENTER.id());
        long centerCrc = winBufCrc(win);
        ScriptWindow.windowPrintBuf(win, "Hi", 2, 100, 1000, 2, 40, 3,
            Window.TextAlignment.TEXT_ALIGNMENT_RIGHT.id());
        long rightCrc = winBufCrc(win);
        ScriptWindow.windowPrintBuf(win, "Hi", 2, 100, 10, 2, 60, 3,
            Window.TextAlignment.TEXT_ALIGNMENT_LEFT.id());
        long guardCrc = winBufCrc(win);
        assertEquals("7207D814", hex(leftCrc));
        assertEquals("5B10B211", hex(centerCrc));
        assertEquals("B5D710DD", hex(rightCrc));
        assertEquals("B5D710DD", hex(guardCrc));
        assertEquals(rightCrc, guardCrc);

        // Word wrap splits on width; null yields an empty list.
        List<String> wrapped = ScriptWindow.windowWordWrap("aa bb cc dd ee", 40, 0);
        assertEquals(4, wrapped.size());
        assertEquals("aa", wrapped.get(0));
        assertEquals("bb", wrapped.get(1));
        assertEquals("cc", wrapped.get(2));
        assertEquals("dd ee", wrapped.get(3));
        assertEquals("[aa|bb|cc|dd ee]", "[" + String.join("|", wrapped) + "]");
        ScriptWindow.windowFreeWordList(wrapped, wrapped.size());
        List<String> nullWrapped = ScriptWindow.windowWordWrap(null, 40, 0);
        assertEquals(0, nullWrapped.size());
        assertEquals(1, nullWrapped.isEmpty() ? 1 : 0);

        // Multiline render, plain and spaced.
        ScriptWindow.windowWrapLine(win, "aa bb cc dd ee", 60, 60, 100, 2, 3,
            Window.TextAlignment.TEXT_ALIGNMENT_LEFT.id());
        long wrapCrc = winBufCrc(win);
        ScriptWindow.windowWrapLineWithSpacing(win, "aa bb cc dd ee", 60, 60, 100, 2, 3,
            Window.TextAlignment.TEXT_ALIGNMENT_LEFT.id(), 2);
        long spacingCrc = winBufCrc(win);
        WindowManager.windowDestroy(win);
        assertEquals("E722C2DA", hex(wrapCrc));
        assertEquals("CA1784B0", hex(spacingCrc));

        // Pure buffer scalers on fixed patterns.
        byte[] src4 = new byte[4 * 4];
        for (int i = 0; i < 16; i++) {
            src4[i] = (byte) (i * 17);
        }
        byte[] dstUp = new byte[8 * 8];
        ScriptWindow.drawScaled(dstUp, 0, 8, 8, 8, src4, 0, 4, 4, 4);
        byte[] dstSame = new byte[4 * 4];
        ScriptWindow.drawScaledBuf(dstSame, 0, 4, 4, src4, 0, 4, 4);
        byte[] dstDown = new byte[2 * 2];
        ScriptWindow.drawScaled(dstDown, 0, 2, 2, 2, src4, 0, 4, 4, 4);
        assertEquals("46D01941", hex(crc(dstUp)));
        assertEquals("8407759B", hex(crc(dstSame)));
        assertEquals("DFD41527", hex(crc(dstDown)));

        byte[] tile3 = new byte[9 * 9];
        for (int i = 0; i < 81; i++) {
            tile3[i] = (byte) (i + 1);
        }
        byte[] tileDst = new byte[12 * 12];
        ScriptWindow.fillBuf3x3(tile3, 0, 9, 9, tileDst, 0, 12, 12);
        assertEquals("6452ADE4", hex(crc(tileDst)));

        WindowManager.windowManagerExit();
    }
}
