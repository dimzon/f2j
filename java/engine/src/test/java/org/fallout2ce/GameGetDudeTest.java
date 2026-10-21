package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The player builtin of the game.
 *
 * <p>The player is made with the objects of the game: the proto of the
 * player is the critter proto zero, the one the original calls
 * {@code CritterProtoTypeId::Dude}. The setup mirrors the harness flow of
 * {@link PartyObjectsTest}.
 */
class GameGetDudeTest {

    private GameScriptApi api;

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
        TileMath.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Db.resetForTests();
        ProtoDb.resetForTests();
        Light.resetForTests();

        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/patch000.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/critter.dat"));
        assertEquals(0, Art.artInit());
        byte[] tileBuf = new byte[640 * 480];
        assertEquals(0, TileMath.tileInit(null, 100, 100, 200, 200, tileBuf, 640, 480, 640,
                rect -> {
                }));
        assertEquals(0, TileMath.tileSetCenter(17488,
                TileMath.TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS));
        api = new GameScriptApi();
    }

    @AfterEach
    void teardown() {
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Db.resetForTests();
        ProtoDb.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
    }

    @Test
    void thePlayerOfTheGame() {
        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));

        GameObject dude = ObjectSystem.getDude();
        assertNotNull(dude);

        // And the scripts get the player the game made.
        assertSame(dude, api.opGetDude());

        // The player is the critter proto zero.
        assertEquals(16777216, dude.pid);
        assertEquals(0, ProtoId.ofCritterType(ProtoTypes.CritterProtoTypeId.Dude).protoId());
    }
}
