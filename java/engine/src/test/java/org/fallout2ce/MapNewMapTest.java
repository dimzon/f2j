package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * {@code [mapnewmap]} vectors from
 * {@code docker/refharness/golden/v98.txt}, produced by
 * {@code sectionMapChild} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The child boots a fresh engine (settings, 640x480, palette, game mounts,
 * window manager, input, 64x64 iso window, art, tile grids, party, objects,
 * game mouse) and then runs {@code mapNewMap}: elevation 0, center 20100,
 * header reset, remove-all (dude, egg and both mouse cursors survive via
 * {@code NO_REMOVE}), var frees, square reset (every grid fid becomes the
 * grid frame), dude placement (the egg follows the dude via the
 * {@code objectSetLocation} dude-tail; the bouncing cursor stays floating
 * where the init-time mouse move left it) and the window refresh.
 *
 * <p>The harness {@code boot} line itself is not asserted: its
 * {@code mode/wminit/input/win/buf/db/xb} fields are scaffolding owned by
 * other slices (notably {@code input=-1} is SDL-dummy-specific: the dummy
 * {@code _GNW95_input_init} fails after {@code mouseInit} centered the
 * mouse, while the Java platform init succeeds headless by design). The
 * test asserts the returns the map slice needs
 * ({@code art/tile/party/objects/mouse}) plus the {@code newmap},
 * {@code square}, {@code obj} and {@code objects} lines.
 *
 * <p>The mouse starts centered by {@link Mouse#mouseInit} (319,239 cursor +
 * (1,1) hot = (320,240), like the C child), so the init-time 3d move lands
 * the hex cursor on tile 23702; the center is already 20100 after
 * {@code tileInit}, so the dude and the egg land there.
 *
 * <p>Isolation: setup mirrors {@link PartyObjectsTest} (mounts + art + tile
 * grids) plus {@link GameMouseTest} (640x480 screen, palette, centered
 * mouse). Teardown drops every holder the slice touches; the full suite must
 * stay green (especially {@link GameMouseTest}, whose init now creates the
 * two cursor objects, and {@link PartyObjectsTest}).
 */
class MapNewMapTest {

    private int savedLeft;
    private int savedTop;
    private int savedRight;
    private int savedBottom;
    private String savedLanguage;

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path p = Paths.get(root, name);
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
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        Mouse.resetForTests();
        GameMouse.resetForTests();
        Map.resetForTests();
        MapVars.resetForTests();

        savedLeft = Svga.scrSize.left;
        savedTop = Svga.scrSize.top;
        savedRight = Svga.scrSize.right;
        savedBottom = Svga.scrSize.bottom;
        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;

        Mouse.mouseBlit = (src, srcPitch, unused, srcX, srcY, srcWidth, srcHeight, destX, destY) -> {
        };

        savedLanguage = Settings.settings.system.language;
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/patch000.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/critter.dat"));
        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());

        // Centers the cursor (319,239) like the C child (mouseInit ran
        // before the dummy input init failed); the default frame hot spot
        // puts the position at (320,240).
        assertEquals(0, Mouse.mouseInit());

        byte[] tileBuf = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf, 64, 64, 64,
                rect -> {
                }));
    }

    @AfterEach
    void teardown() {
        GameMouse.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        Map.resetForTests();
        MapVars.resetForTests();
        Db.resetForTests();
        ProtoDb.resetForTests();
        Mouse.resetForTests();
        Mouse.mouseBlit = null;
        Settings.settings.system.language = savedLanguage;
        Settings.resetStateForTests();
        Config.resetLastSectionKey();

        Svga.scrSize.left = savedLeft;
        Svga.scrSize.top = savedTop;
        Svga.scrSize.right = savedRight;
        Svga.scrSize.bottom = savedBottom;
    }

    @Test
    void mapNewMapVectors() {
        // party=0, objects=0, mouse=0 (bring-up the map slice needs).
        assertEquals(0, PartyMembers.partyMembersInit());
        byte[] objBuf = new byte[640 * 480];
        assertEquals(0, ObjectSystem.objectsInit(objBuf, 640, 480, 640));
        assertEquals(0, GameMouse.gameMouseInit());

        Map.mapNewMap();

        // newmap elev=0 center=20100 hdr=20,20100,0,0 local=0 global=0 gvarlen=0,0.
        Map.MapHeader header = Map.gMapHeader;
        assertEquals("newmap elev=0 center=20100 hdr=20,20100,0,0 local=0 global=0 gvarlen=0,0",
                "newmap elev=" + Map.gElevation
                        + " center=" + TileMath.gCenterTile
                        + " hdr=" + header.version
                        + "," + header.enteringTile
                        + "," + header.enteringElevation
                        + "," + header.enteringRotation.value()
                        + " local=" + header.localVariablesCount
                        + " global=" + header.globalVariablesCount
                        + " gvarlen=" + MapVars.globalVarsLength()
                        + "," + MapVars.localVarsLength());

        // square=00010001 x4 (grid frame, old flags kept from zeroed grids).
        int[] fids = Map.squareGrids()[0].tileFid;
        assertEquals("square=00010001,00010001,00010001,00010001",
                String.format("square=%08X,%08X,%08X,%08X", fids[0], fids[1], fids[50], fids[99]));

        // obj dump: dude, egg, hex cursor (bouncing cursor stays floating).
        StringBuilder dumped = new StringBuilder();
        int objectCount = 0;
        for (GameObject object = ObjectSystem.objectFindFirst(); object != null;
                object = ObjectSystem.objectFindNext()) {
            objectCount++;
            dumped.append(String.format("obj pid=%d tile=%d elev=%d rot=%d flags=%08X%n",
                    object.pid, object.tile, object.elevation, object.rotation.id(),
                    object.flags.bits()));
        }
        assertEquals("obj pid=16777216 tile=20100 elev=0 rot=0 flags=20000425" + System.lineSeparator()
                + "obj pid=-1 tile=20100 elev=0 rot=0 flags=20000405" + System.lineSeparator()
                + "obj pid=-1 tile=23702 elev=0 rot=0 flags=A000041C" + System.lineSeparator(),
                dumped.toString());

        // objects=3 dude=20100,0,0 light=65536 flags=20000425.
        GameObject dude = ObjectSystem.getDude();
        assertNotNull(dude);
        assertEquals("objects=3 dude=20100,0,0 light=65536 flags=20000425",
                "objects=" + objectCount
                        + " dude=" + dude.tile + "," + dude.elevation + "," + dude.rotation.id()
                        + " light=" + ObjectSystem.objectGetLightIntensity(dude)
                        + " flags=" + String.format("%08X", dude.flags.bits()));
    }
}
