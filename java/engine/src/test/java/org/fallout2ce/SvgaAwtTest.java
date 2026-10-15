package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

import org.fallout2ce.platform.awt.AwbVideoBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end render through the real AWT backend (headless-safe, no
 * window): game palette + main-menu FRM blit. Proves the managed display
 * pipeline shows game pixels with no SDL involved.
 */
class SvgaAwtTest {

    private int savedResX;
    private int savedResY;
    private int savedScale;
    private int savedWindowed;

    @AfterEach
    void reset() {
        Settings.settings.screen.resolution_x = savedResX;
        Settings.settings.screen.resolution_y = savedResY;
        Settings.settings.screen.scale = savedScale;
        Settings.settings.screen.windowed = savedWindowed;
        Svga.resetForTests();
        Art.resetForTests();
        Platform.resetForTests();
    }

    void saveSettings() {
        savedResX = Settings.settings.screen.resolution_x;
        savedResY = Settings.settings.screen.resolution_y;
        savedScale = Settings.settings.screen.scale;
        savedWindowed = Settings.settings.screen.windowed;
    }

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        Path p = Path.of(root, name);
        assumeTrue(Files.isRegularFile(p), "missing " + p);
        return p.toString();
    }

    static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    @Test
    void mainMenuRenders() {
        saveSettings();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/patch000.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/critter.dat"));
        assertEquals(0, Art.artInit());
        assertTrue(Color.colorPaletteLoad("color.pal"));

        AwbVideoBackend display = new AwbVideoBackend();
        Color.video = display;
        Settings.settings.screen.resolution_x = 640;
        Settings.settings.screen.resolution_y = 480;
        Settings.settings.screen.scale = 1;
        Settings.settings.screen.windowed = Svga.WINDOWMODE_WINDOWED;

        Color.colorSetBrightness(1.0);
        Color.setSystemPalette(Color.cmap);
        assertEquals(0, Svga.initMode640x480());
        assertEquals(640, Svga.screenGetWidth());
        assertEquals(480, Svga.screenGetHeight());

        Svga.gnw95ZeroVidMem();
        try (Art.FrmImage image = new Art.FrmImage()) {
            assertTrue(image.lock(FrmId.ofType(ObjectType.INTERFACE, 140)));
            int w = Math.min(image.getWidth(), 640);
            int h = Math.min(image.getHeight(), 480);
            Svga.showRect(image.getData(), image.getWidth(), 0, 0, 0, w, h, 0, 0);
        }

        // mainmenu.frm frame 0 (matches ArtLoadTest's frame-data CRC:
        // a full-screen blit over a cleared buffer reproduces it).
        assertEquals(0xE4F6BDB0L, crc32(display.framebuffer()));
    }
}
