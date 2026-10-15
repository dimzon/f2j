package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Every vector comes from {@code docker/refharness/golden/v18.txt}
 * ({@code [font]}), reproduced with identical inputs (game data required).
 */
class TextFontTest {

    private String savedLanguage;

    @AfterEach
    void reset() {
        TextFont.resetForTests();
        Db.resetForTests();
        if (savedLanguage != null) {
            Settings.settings.system.language = savedLanguage;
            savedLanguage = null;
        }
    }

    static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        Path p = Path.of(root, name);
        assumeTrue(Files.isRegularFile(p), "missing " + p);
        return p.toString();
    }

    void mountGameEn() {
        if (savedLanguage == null) {
            savedLanguage = Settings.settings.system.language;
        }
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        Settings.settings.system.language = Message.ENGLISH;
    }

    @Test
    void initAndMetrics() {
        mountGameEn();

        assertEquals(0, TextFont.textFontsInit());
        assertEquals(0, TextFont.fontGetCurrent());

        assertEquals(16, TextFont.fontGetLineHeight());
        assertEquals(1, TextFont.fontGetLetterSpacing());
        assertEquals(18, TextFont.fontGetStringWidth("Hi!"));
        assertEquals(7, TextFont.fontGetCharacterWidth('H'));
        assertEquals(10, TextFont.fontGetMonospacedCharacterWidth());
        assertEquals(288, TextFont.fontGetBufferSize("Hi!"));
        assertEquals(30, TextFont.fontGetMonospacedStringWidth("Hi!"));
    }

    @Test
    void draws() {
        mountGameEn();
        assertEquals(0, TextFont.textFontsInit());
        // Shadow ink is COLOR_BLACK from the loaded palette (the harness
        // runs [font] after [color]).
        assertTrue(Color.colorPaletteLoad("color.pal"));

        byte[] text = new byte[64 * 32];

        Arrays.fill(text, (byte) 0);
        TextFont.fontDrawText(text, 0, "Hi!", text.length, 64, 42);
        assertEquals(0x0141BCC3L, crc32(text));

        Arrays.fill(text, (byte) 0);
        TextFont.fontDrawText(text, 0, "Hi!", text.length, 64, 42 | Color.DRAW_TEXT_FLAG_SHADOWED);
        assertEquals(0x7865E4D9L, crc32(text));

        Arrays.fill(text, (byte) 0);
        TextFont.fontDrawText(text, 0, "Hi!", text.length, 64, 42 | Color.DRAW_TEXT_FLAG_MONOSPACED);
        assertEquals(0x8883CBF9L, crc32(text));

        Arrays.fill(text, (byte) 0);
        TextFont.fontDrawText(text, 0, "Hi!", text.length, 64, 42 | Color.DRAW_TEXT_FLAG_UNDERLINED);
        assertEquals(0x66ABC100L, crc32(text));

        Arrays.fill(text, (byte) 0);
        TextFont.fontDrawText(text, 0, "Hi!", 4, 64, 42);
        assertEquals(0xF1E8BA9EL, crc32(text));

        byte[] canvas = new byte[40 * 20];
        Arrays.fill(canvas, (byte) 0);
        TextFont.fontDrawText2D(new Draw.Buffer(canvas, 40, 20), 3, 5, "Hi!", 36, 42);
        assertEquals(0x413CB18EL, crc32(canvas));
    }

    @Test
    void currentSwitchingQuirks() {
        mountGameEn();
        assertEquals(0, TextFont.textFontsInit());

        TextFont.fontSetCurrent(1);
        assertEquals(1, TextFont.fontGetCurrent());
        assertEquals(10, TextFont.fontGetLineHeight());
        assertEquals(13, TextFont.fontGetStringWidth("Hi!"));

        // Font 4 is missing: current changes, descriptor stays on font 1.
        TextFont.fontSetCurrent(4);
        assertEquals(4, TextFont.fontGetCurrent());
        assertEquals(10, TextFont.fontGetLineHeight());

        // Out of every manager range: no-op.
        TextFont.fontSetCurrent(99);
        TextFont.fontSetCurrent(-1);
        assertEquals(4, TextFont.fontGetCurrent());
    }

    @Test
    void managerAddQuirks() {
        mountGameEn();
        assertEquals(0, TextFont.textFontsInit());

        assertEquals(-1, TextFont.fontManagerAdd(0, 9));
        // Overlap check is exclusive-upper: {9,9} slips through (shadowed).
        assertEquals(0, TextFont.fontManagerAdd(9, 9));
    }

    @Test
    void exitAndReload() {
        mountGameEn();
        assertEquals(0, TextFont.textFontsInit());

        TextFont.textFontsExit();

        assertEquals(0, TextFont.textFontLoad(0));
        assertEquals(-1, TextFont.textFontLoad(9));
        assertEquals(-1, TextFont.textFontLoad(-1));
        assertEquals(-1, TextFont.textFontLoad(10));
    }

    @Test
    void metricsWithoutFontFail() {
        assertThrows(IllegalStateException.class, TextFont::fontGetLineHeight);
        assertThrows(IllegalStateException.class, () -> TextFont.fontGetStringWidth("Hi!"));
        // Drawing without a font is a silent no-op (null proc check in C).
        byte[] text = new byte[64];
        TextFont.fontDrawText(text, 0, "Hi!", text.length, 8, 42);
        assertArrayEquals(new byte[64], text);
    }

    @Test
    void scopedFontRestores() {
        mountGameEn();
        assertEquals(0, TextFont.textFontsInit());

        try (TextFont.ScopedFont scope = new TextFont.ScopedFont(1)) {
            assertEquals(1, TextFont.fontGetCurrent());
        }
        assertEquals(0, TextFont.fontGetCurrent());
    }
}
