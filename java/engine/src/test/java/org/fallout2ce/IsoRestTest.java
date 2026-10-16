package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [isorest]} vectors from
 * {@code docker/refharness/golden/v97.txt}, produced by
 * {@code sectionIsoRest} in {@code docker/refharness/ref_main.cc}:
 * {@code colorCycleInit(); iface=interfaceInit(); elevatorsInit();
 * enter=mapSetEnteringLocation(-1,-1,ROTATION_INVALID)} printing
 * {@code iface=0 enter=0}.
 *
 * <p>The two embedded {@code INTRFACE:}/{@code Elevators init:} lines are
 * {@code debugPrint} noise (their text is not asserted), but their
 * config-sourced cause is: the default {@code ui.iface_bar_width} is 800 and
 * the harness screen is 640x480, so {@link Interface#customInterfaceBarInit}
 * takes the {@code debugPrint} branch and falls back to the 640-wide default
 * bar; with no {@code [Misc]} {@code ElevatorsFile} in the sfall global
 * config {@link Elevators#elevatorsInit} returns before touching state.
 *
 * <p>Mounts mirror {@link PartyObjectsTest} (master + patch + critter for
 * {@link Art#artInit}'s lists) plus {@link WindowButtonTest} (640x480
 * {@code scrSize}, palette, window manager) - the interface bar FRMs live in
 * master.dat. Isolation: the bar window and its buffer live in
 * {@link Interface} + {@link WindowManager} state that {@code @AfterEach}
 * frees ({@code interfaceFree} destroys the window and buttons, then
 * {@code resetForTests} restores the BSS mirror), so the rest of the suite
 * never sees the bar window.
 */
class IsoRestTest {

    private int savedLeft;
    private int savedTop;
    private int savedRight;
    private int savedBottom;
    private String savedLanguage;
    private Svga.ShowRect savedScrBlit;

    static String gameData(String name) {
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
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        SfallConfig.resetForTests();
        Interface.resetForTests();
        Elevators.resetForTests();
        EnteringLocation.resetForTests();
        ColorCycle.resetForTests();
        Art.resetForTests();
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        TextFont.resetForTests();
        WindowManager.resetForTests();

        savedLeft = Svga.scrSize.left;
        savedTop = Svga.scrSize.top;
        savedRight = Svga.scrSize.right;
        savedBottom = Svga.scrSize.bottom;
        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;

        // Buffered refresh blits the screen buffer out; there is no video
        // surface in tests, so drop the copy (WindowButtonTest convention).
        savedScrBlit = Svga.scrBlit;
        Svga.scrBlit = (src, srcPitch, unused, srcX, srcY, srcWidth, srcHeight, destX, destY) -> {
        };
        Mouse.mouseBlit = (src, srcPitch, unused, srcX, srcY, srcWidth, srcHeight, destX, destY) -> {
        };

        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/patch000.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/critter.dat"));
        assertEquals(0, Art.artInit());

        savedLanguage = Settings.settings.system.language;
        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(Color.colorPaletteLoad("color.pal"));

        assertEquals(0, WindowManager.windowManagerInit(() -> 0, () -> {
        }, WindowManager.WindowManagerInitFlags.WINDOW_MANAGER_INIT_FLAG_BUFFERED.id()));
        Input.programIsActive = true;
    }

    @AfterEach
    void teardown() {
        Interface.resetForTests();
        Elevators.resetForTests();
        EnteringLocation.resetForTests();
        ColorCycle.resetForTests();
        WindowManager.windowManagerExit();
        WindowManager.resetForTests();
        Art.resetForTests();
        Db.resetForTests();
        SfallConfig.resetForTests();
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        TextFont.resetForTests();
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Settings.settings.system.language = savedLanguage;

        Svga.scrSize.left = savedLeft;
        Svga.scrSize.top = savedTop;
        Svga.scrSize.right = savedRight;
        Svga.scrSize.bottom = savedBottom;
        Svga.scrBlit = savedScrBlit;
        Mouse.mouseBlit = null;
    }

    /** The harness screen is 640x480 and the default bar width is 800. */
    @Test
    void customBarWidthFallsBackToDefault() {
        assertEquals(800, Settings.settings.ui.iface_bar_width);
        assertEquals(640, Svga.screenGetWidth());
        assertEquals(480, Svga.screenGetHeight());
        assertTrue(Interface.interfaceBarWidth > 640);

        // iface=0.
        assertEquals(0, Interface.interfaceInit());

        // The INTRFACE: debugPrint cause: width 800 > screen 640, no custom art.
        assertFalse(Interface.interfaceBarIsCustom);
        assertEquals(Interface.INTERFACE_BAR_WIDTH, Interface.interfaceBarWidth);
        assertEquals(0, Interface.interfaceBarContentOffset);

        // Rects land on the default-bar offsets.
        Geometry.Rect endButtons = Interface.interfaceBarEndButtonsRect();
        assertEquals(580, endButtons.left);
        assertEquals(38, endButtons.top);
        assertEquals(637, endButtons.right);
        assertEquals(96, endButtons.bottom);
        Geometry.Rect mainAction = Interface.interfaceBarMainActionRect();
        assertEquals(267, mainAction.left);
        assertEquals(26, mainAction.top);
        assertEquals(455, mainAction.right);
        assertEquals(93, mainAction.bottom);

        // insideInit was set and then cleared by the init tail.
        assertFalse(Interface.interfaceBarInitialized());
        assertTrue(Interface.interfaceBarEnabled());
        assertTrue(Interface.interfaceBarHidden());
        assertEquals(Inventory.Hand.HAND_LEFT, Interface.interfaceGetCurrentHand());

        // intface_init_items: both hands carry the null sentinel (C: (Object*)-1).
        assertNull(Interface.interfaceItem(Inventory.Hand.HAND_LEFT));
        assertNull(Interface.interfaceItem(Inventory.Hand.HAND_RIGHT));

        // AP bar: extend_ap_bar is off by default, so the original 90px slot.
        assertEquals(90, Interface.apBarWidth());
        assertEquals(10, Interface.apBarMaxAP());
        Geometry.Rect apBar = Interface.apBarRect();
        assertEquals(316, apBar.left);
        assertEquals(14, apBar.top);
        assertEquals(405, apBar.right);
        assertEquals(18, apBar.bottom);
    }

    /** iface=0: window created hidden at the centered bar position. */
    @Test
    void interfaceInitCreatesHiddenBarWindow() {
        assertEquals(0, Interface.interfaceInit());

        int win = Interface.interfaceBarWindow;
        assertTrue(win != -1);
        assertTrue(WindowManager.windowIsValidWindowId(win));
        assertEquals(Interface.INTERFACE_BAR_WIDTH, WindowManager.windowGetWidth(win));
        assertEquals(Interface.INTERFACE_BAR_HEIGHT, WindowManager.windowGetHeight(win));

        Geometry.Rect rect = new Geometry.Rect();
        assertEquals(0, WindowManager.windowGetRect(win, rect));
        // (screenGetWidth() - 640) / 2 = 0; screenGetHeight() - 100 = 380.
        assertEquals(0, rect.left);
        assertEquals(380, rect.top);
        assertEquals(639, rect.right);
        assertEquals(479, rect.bottom);

        int flags = WindowManager.windowGetWindow(win).flags;
        assertTrue((flags & WindowManager.WindowFlags.WINDOW_HIDDEN.id()) != 0);

        byte[] buf = WindowManager.windowGetBuffer(win);
        assertNotNull(buf);
        assertEquals(Interface.INTERFACE_BAR_WIDTH * Interface.INTERFACE_BAR_HEIGHT, buf.length);
        assertTrue(buf == Interface.interfaceWindowBuffer());

        // Buffer content: the maininterface FRM blitted over the first 99 rows,
        // the last row keeps the COLOR_BLACK fill from windowCreate. The
        // buttons draw their own art over rows 6..95 (buttonCreate calls
        // buttonDraw), so the untouched strips are rows 0..5 and 96..98.
        Art.FrmImage background = new Art.FrmImage();
        try {
            assertTrue(background.lock(FrmId.ofType(ObjectType.INTERFACE,
                    ArtDefs.InterfaceFrameId.MainInterface.id())));
            byte[] frm = background.getData();
            assertTrue(frm.length >= Interface.INTERFACE_BAR_WIDTH
                    * (Interface.INTERFACE_BAR_HEIGHT - 1));

            assertRowsEqualFrm(frm, buf, 0, 6);
            assertRowsEqualFrm(frm, buf, 96, 3);

            // The single attack button drew over its 188x67 rect at (267, 26).
            byte[] buttonRegion = Arrays.copyOfRange(buf,
                    26 * Interface.INTERFACE_BAR_WIDTH + 267,
                    (26 + 67) * Interface.INTERFACE_BAR_WIDTH + 267 + 188);
            byte[] frmRegion = Arrays.copyOfRange(frm,
                    26 * Interface.INTERFACE_BAR_WIDTH + 267,
                    (26 + 67) * Interface.INTERFACE_BAR_WIDTH + 267 + 188);
            assertFalse(Arrays.equals(buttonRegion, frmRegion),
                    "single attack button art was not drawn into the bar buffer");
        } finally {
            background.close();
        }

        int black = Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0x000000)]);
        int lastRow = Interface.INTERFACE_BAR_HEIGHT - 1;
        for (int x = 0; x < Interface.INTERFACE_BAR_WIDTH; x++) {
            assertEquals(black, Byte.toUnsignedInt(buf[lastRow * Interface.INTERFACE_BAR_WIDTH + x]),
                    "last row pixel " + x);
        }

        // iface=-1: the gInterfaceBarWindow guard rejects a second init.
        assertEquals(-1, Interface.interfaceInit());
    }

    /** Asserts {@code count} rows of the bar buffer match the FRM rows verbatim. */
    private static void assertRowsEqualFrm(byte[] frm, byte[] buf, int firstRow, int count) {
        int pitch = Interface.INTERFACE_BAR_WIDTH;
        assertArrayEquals(Arrays.copyOfRange(frm, firstRow * pitch, (firstRow + count) * pitch),
                Arrays.copyOfRange(buf, firstRow * pitch, (firstRow + count) * pitch),
                "rows " + firstRow + ".." + (firstRow + count - 1));
    }

    /** colorCycleInit: init guard makes the second call a no-op. */
    @Test
    void colorCycleInitHalvesTablesOnce() {
        assertTrue(Settings.settings.system.color_cycling);

        ColorCycle.colorCycleInit();

        assertTrue(ColorCycle.colorCycleEnabled());
        // 108 >> 2 and 43 >> 2 on the two slime triples.
        assertEquals(27, Byte.toUnsignedInt(ColorCycle.slime[1]));
        assertEquals(32, Byte.toUnsignedInt(ColorCycle.slime[10]));
        assertEquals(6, Byte.toUnsignedInt(ColorCycle.slime[11]));

        ColorCycle.colorCycleInit();

        assertEquals(27, Byte.toUnsignedInt(ColorCycle.slime[1]));
        assertEquals(6, Byte.toUnsignedInt(ColorCycle.slime[11]));
        assertTrue(ColorCycle.colorCycleEnabled());
    }

    /** Elevators init: no-op with no ElevatorsFile configured. */
    @Test
    void elevatorsInitIsNoOpWithoutConfig() {
        String[] configured = new String[1];
        assertFalse(Config.configGetString(SfallConfig.sfallConfig, SfallConfig.MISC_KEY,
                SfallConfig.ELEVATORS_FILE_KEY, configured));
        assertNull(configured[0]);

        Elevators.elevatorsInit();

        // Nothing was written into the config (the early return).
        assertFalse(Config.configGetString(SfallConfig.sfallConfig, SfallConfig.MISC_KEY,
                SfallConfig.ELEVATORS_FILE_KEY, configured));
        assertNull(configured[0]);
    }

    /** enter=0: the (-1,-1,Invalid) sentinel and a real spot both store. */
    @Test
    void enteringLocationStoresVerbatim() {
        assertEquals(0, EnteringLocation.mapSetEnteringLocation(-1, -1, Rotation.INVALID));
        assertEquals(-1, EnteringLocation.getEnteringElevation());
        assertEquals(-1, EnteringLocation.getEnteringTile());
        assertEquals(Rotation.INVALID, EnteringLocation.getEnteringRotation());

        assertEquals(0, EnteringLocation.mapSetEnteringLocation(2, 17488, Rotation.SW));
        assertEquals(2, EnteringLocation.getEnteringElevation());
        assertEquals(17488, EnteringLocation.getEnteringTile());
        assertEquals(Rotation.SW, EnteringLocation.getEnteringRotation());

        assertEquals(0, EnteringLocation.mapSetEnteringLocation(-1, -1, Rotation.INVALID));
    }

    /** resetForTests isolation: the bar window is gone for the rest of the suite. */
    @Test
    void resetForTestsReleasesBarWindow() {
        assertEquals(0, Interface.interfaceInit());
        int win = Interface.interfaceBarWindow;
        assertTrue(win != -1);

        Interface.resetForTests();

        assertEquals(-1, Interface.interfaceBarWindow);
        assertNull(Interface.interfaceWindowBuffer());
        assertFalse(WindowManager.windowIsValidWindowId(win));
        assertEquals(800, Interface.interfaceBarWidth);
        assertEquals(0, Interface.interfaceBarContentOffset);
        assertFalse(Interface.interfaceBarIsCustom);
        assertEquals(108, Byte.toUnsignedInt(ColorCycle.slime[1]));
        assertEquals(0, EnteringLocation.getEnteringElevation());
        assertEquals(-1, EnteringLocation.getEnteringTile());
        assertEquals(Rotation.NE, EnteringLocation.getEnteringRotation());

        // Init is re-armable after the reset.
        assertEquals(0, Interface.interfaceInit());
        assertEquals(-1, Interface.interfaceInit());
    }
}