package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Every vector comes from {@code docker/refharness/golden/v19.txt}
 * ({@code [ifacefont]}): pristine degradation, metrics and draws through
 * the public proc pointers (game.cc wires the modern manager the same way).
 */
class InterfaceFontTest {

    private String savedLanguage;

    @AfterEach
    void reset() {
        InterfaceFont.resetForTests();
        TextFont.resetForTests();
        Color.resetForTests();
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

    /** Mirror game.cc: register the modern range, select font 100. */
    void wireModern() {
        assertEquals(0, TextFont.fontManagerAdd(100, 110, InterfaceFont.IMPL));
        TextFont.fontSetCurrent(100);
        assertEquals(100, TextFont.fontGetCurrent());
    }

    @Test
    void pristineDegrades() {
        assertEquals(0, InterfaceFont.interfaceFontGetStringWidthScaled("Hi!", 42, 1.0f));

        byte[] pre = new byte[32];
        Arrays.fill(pre, (byte) 0);
        InterfaceFont.interfaceFontDrawTextScaled2D(new Draw.Buffer(pre, 8, 4), 0, 0, "Hi!", 42, 1.0f);
        assertEquals(0x190A55ADL, crc32(pre));
    }

    @Test
    void initAndMetrics() {
        mountGameEn();
        assertTrue(Color.colorPaletteLoad("color.pal"));

        assertEquals(0, InterfaceFont.interfaceFontsInit());
        wireModern();

        assertEquals(12, TextFont.fontGetLineHeight());
        assertEquals(1, TextFont.fontGetLetterSpacing());
        assertEquals(17, TextFont.fontGetStringWidth("Hi!"));
        assertEquals(9, TextFont.fontGetCharacterWidth('A'));
        assertEquals(4, TextFont.fontGetCharacterWidth(' '));
        assertEquals(10, TextFont.fontGetMonospacedCharacterWidth());
        assertEquals(204, TextFont.fontGetBufferSize("Hi!"));
        assertEquals(30, TextFont.fontGetMonospacedStringWidth("Hi!"));
    }

    @Test
    void draws() {
        mountGameEn();
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, InterfaceFont.interfaceFontsInit());
        wireModern();

        byte[] text = new byte[64 * 32];

        Arrays.fill(text, (byte) 0);
        TextFont.fontDrawText(text, 0, "Hi!", text.length, 64, 42);
        assertEquals(0xEB7236D3L, crc32(text));

        Arrays.fill(text, (byte) 0);
        TextFont.fontDrawText(text, 0, "Hi!", text.length, 64, 42 | Color.DRAW_TEXT_FLAG_SHADOWED);
        assertEquals(0x0730C59EL, crc32(text));

        Arrays.fill(text, (byte) 0);
        TextFont.fontDrawText(text, 0, "Hi!", text.length, 64, 42 | Color.DRAW_TEXT_FLAG_MONOSPACED);
        assertEquals(0x20A0D317L, crc32(text));

        Arrays.fill(text, (byte) 0);
        TextFont.fontDrawText(text, 0, "Hi!", text.length, 64, 42 | Color.DRAW_TEXT_FLAG_UNDERLINED);
        assertEquals(0xD92F0C6DL, crc32(text));

        Arrays.fill(text, (byte) 0);
        TextFont.fontDrawText(text, 0, "Hi!", 4, 64, 42);
        assertEquals(0xF1E8BA9EL, crc32(text));
    }

    @Test
    void scaled() {
        mountGameEn();
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, InterfaceFont.interfaceFontsInit());

        byte[] canvas = new byte[40 * 20];
        Draw.Buffer dest = new Draw.Buffer(canvas, 40, 20);

        Arrays.fill(canvas, (byte) 0);
        InterfaceFont.interfaceFontDrawTextScaled2D(dest, 2, 2, "Hi!", 42, 1.0f);
        assertEquals(0x48DA6C5EL, crc32(canvas));
        assertEquals(17, InterfaceFont.interfaceFontGetStringWidthScaled("Hi!", 42, 1.0f));

        Arrays.fill(canvas, (byte) 0);
        InterfaceFont.interfaceFontDrawTextScaled2D(dest, 2, 2, "Hi!", 42, 2.0f);
        assertEquals(0xEF8CBBC6L, crc32(canvas));
        assertEquals(34, InterfaceFont.interfaceFontGetStringWidthScaled("Hi!", 42, 2.0f));
    }

    @Test
    void currentSwitching() {
        mountGameEn();
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, InterfaceFont.interfaceFontsInit());
        wireModern();

        TextFont.fontSetCurrent(101);
        assertEquals(101, TextFont.fontGetCurrent());
        assertEquals(10, TextFont.fontGetLineHeight());

        // Font 9 has no data: current changes, descriptor stays on font 1.
        TextFont.fontSetCurrent(109);
        assertEquals(109, TextFont.fontGetCurrent());
        assertEquals(10, TextFont.fontGetLineHeight());
    }

    @Test
    void exitAndReinit() {
        mountGameEn();
        assertEquals(0, InterfaceFont.interfaceFontsInit());

        InterfaceFont.interfaceFontsExit();

        assertEquals(0, InterfaceFont.interfaceFontsInit());
    }
}
