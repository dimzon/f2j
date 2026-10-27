package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * The stage-4 game-boot skeleton: the ported init chain runs headless
 * without errors, the dude exists, and the queue idles after the init.
 * The script-engine/UI seams are documented in {@code Game.gameInit}.
 */
class GameInitTest {

    @BeforeEach
    void setup() {
        Game.resetForTests();

        Settings.settings.system.language = Message.ENGLISH;
        String root = System.getenv("F2J_GAMEDATA");
        assertNotNull(root);
        assertEquals(0, Db.dbOpen(Paths.get(root, "master.dat").toString(),
                Paths.get(root, "patch000.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "master.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "patch000.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "critter.dat").toString()));
        assertTrue(Color.colorPaletteLoad("color.pal"));

        // The ported C++ boot chain (art/mouse/light/fonts/kbd/input/
        // interface/stats/party/items/ai/worldmap/queue) runs here.
        assertEquals(0, Game.gameInit());
    }

    @AfterEach
    void tearDown() {
        GameRandom.resetProviders();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
    }

    private void bootTileAndObjects() {
        byte[] tileBuf = new byte[640 * 480];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf,
                640, 480, 640, rect -> {
                }));
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));
    }

    @Test
    void gameInitBootsHeadless() {
        bootTileAndObjects();
        assertNotNull(ObjectSystem.getDude());
    }

    @Test
    void queueIdlesAfterInit() {
        bootTileAndObjects();
        // No events scheduled: processing is a no-op.
        assertEquals(0, EventQueue.queueProcessEvents());
    }
}
