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
 * Stage-4 end-to-end: load the real ARTEMPLE.MAP (script index > 0) and
 * let the map script bind to the ScrollBlocker and run its MAP_ENTER
 * procedure headless. The script engine executes the real VM; the assertion
 * is that the boot chain + map load + script execution completes without a
 * fatal program error and that the map script is registered.
 */
class GameMapScriptEndToEndTest {

    @BeforeEach
    void setup() {
        Game.resetForTests();
        MapVars.resetForTests();
        Mouse.resetForTests();
        GameMouse.resetForTests();

        String root = System.getenv("F2J_GAMEDATA");
        assertNotNull(root);
        assertEquals(0, Db.dbOpen(Paths.get(root, "master.dat").toString(),
                Paths.get(root, "patch000.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "master.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "patch000.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "critter.dat").toString()));
        assertEquals(0, Art.artInit());
        assertEquals(0, Mouse.mouseInit());

        byte[] tileBuf = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf,
                64, 64, 64, rect -> {
                }));
        assertEquals(0, PartyMembers.partyMembersInit());
        byte[] objBuf = new byte[640 * 480];
        assertEquals(0, ObjectSystem.objectsInit(objBuf, 640, 480, 640));
        assertEquals(0, GameMouse.gameMouseInit());
        assertEquals(0, Game.gameInit());
    }

    @AfterEach
    void tearDown() {
        GameMouse.resetForTests();
        GameRandom.resetProviders();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        MapVars.resetForTests();
        Map.resetForTests();
        Mouse.resetForTests();
        Game.resetForTests();
    }

    @Test
    void mapScriptEndToEnd() {
        EnteringLocation.mapSetEnteringLocation(-1, -1, Rotation.INVALID);
        try {
            assertEquals(0, Map.mapLoadByName("ARTEMPLE.MAP"));
        } catch (UnsupportedOperationException e) {
            // The script hit a documented game-phase seam; rethrow with the
            // script name to identify the caller.
            throw new AssertionError("map script hit a seam: " + e.getMessage(), e);
        }

        // The map script is a SYSTEM-type script with the ScrollBlocker as
        // its owner, and it ran MAP_ENTER without a fatal error.
        assertTrue(Map.gMapHeader.scriptIndex > 0);

        GameObject blocker = null;
        for (GameObject o = ObjectSystem.objectFindFirst(); o != null;
                o = ObjectSystem.objectFindNext()) {
            if (o.sid != -1 && o != ObjectSystem.getDude()) {
                blocker = o;
                break;
            }
        }
        assertNotNull(blocker);

        ObjectSystem.Script[] slot = new ObjectSystem.Script[1];
        assertEquals(0, ObjectSystem.scriptGetScript(blocker.sid, slot));
        ObjectSystem.Script script = slot[0];
        assertEquals(blocker, script.owner);
        assertEquals(Map.gMapHeader.scriptIndex - 1, script.index);
        assertNotNull(script.program);
        assertEquals(0, script.program.flags & Program.PROGRAM_FLAG_FATAL_ERROR);
    }
}
