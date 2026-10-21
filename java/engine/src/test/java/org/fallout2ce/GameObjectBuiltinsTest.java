package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * The object builtins of the game: making an object and taking it away.
 *
 * <p>Nothing is made when the proto id is zero, and nothing is taken away
 * when there is no object at all. The setup mirrors the harness flow of
 * {@link InventoryTest}.
 */
class GameObjectBuiltinsTest {

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
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        api = new GameScriptApi();
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
    }

    @Test
    void anObjectIsMadeAndTakenAway() {
        // A stim of the proto 40 on the tile 17488.
        Object created = api.createObject(40, 17488, 0, 0);
        assertNotNull(created);
        GameObject object = (GameObject) created;
        assertEquals(40, object.pid);

        // Nothing is made of a proto id of zero.
        assertNull(api.createObject(0, 17488, 0, 0));

        api.destroyObject(created);
        assertNull(ObjectSystem.objectFindById(object.id));

        // Nothing is taken away when there is no object at all.
        api.destroyObject(null);
    }
}
