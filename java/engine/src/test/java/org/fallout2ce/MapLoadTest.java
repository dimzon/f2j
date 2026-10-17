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
 * {@code [mapload]} vectors from
 * {@code docker/refharness/golden/v103.txt}, produced by
 * {@code sectionMapLoad} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Loads the real {@code ARTEMPLE.MAP} (version 20, entering tile 18492,
 * elev 0, rot NE, 0 local/global vars) through {@link Map#mapLoadByName}.
 * The map's flags are {@code 12} (elevations 1+2 present), so the elevation-0
 * grid stays at the grid frame after {@code _square_load}.
 *
 * <p>The C++ child also ran {@code mapSetEnteringLocation(-1,-1,INVALID)}
 * (isoInit's tail) so the load adopts the header's entering spot; the test
 * mirrors that. Script/object/random-encounter phases are game-phase seams
 * (script engine, object load, worldmap) — the {@code objects=571} vector
 * therefore is not reproduced here.
 */
class MapLoadTest {

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
        Message.resetForTests();

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
        Message.resetForTests();
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
    void mapLoadVectors() {
        assertEquals(0, PartyMembers.partyMembersInit());
        byte[] objBuf = new byte[640 * 480];
        assertEquals(0, ObjectSystem.objectsInit(objBuf, 640, 480, 640));
        assertEquals(0, GameMouse.gameMouseInit());

        // isoInit's tail: entering spot sentinel (the load adopts the header's).
        EnteringLocation.mapSetEnteringLocation(-1, -1, Rotation.INVALID);

        int rc = Map.mapLoadByName("ARTEMPLE.MAP");

        assertEquals("loadrc=0 ver=20 tile=18492 elev=0 rot=0 local=0 global=0 gvarlen=0,0",
                "loadrc=" + rc
                        + " ver=" + Map.gMapHeader.version
                        + " tile=" + Map.gMapHeader.enteringTile
                        + " elev=" + Map.gMapHeader.enteringElevation
                        + " rot=" + Map.gMapHeader.enteringRotation.value()
                        + " local=" + Map.gMapHeader.localVariablesCount
                        + " global=" + Map.gMapHeader.globalVariablesCount
                        + " gvarlen=" + MapVars.globalVarsLength()
                        + "," + MapVars.localVarsLength());

        int[] fids = Map.squareGrids()[0].tileFid;
        assertEquals("square=00010001,00010001,00010001,00010001",
                String.format("square=%08X,%08X,%08X,%08X", fids[0], fids[1], fids[50], fids[99]));

        GameObject dude = ObjectSystem.getDude();
        assertNotNull(dude);
        assertEquals("dude=18492,0,0",
                "dude=" + dude.tile + "," + dude.elevation + "," + dude.rotation.id());
    }
}
