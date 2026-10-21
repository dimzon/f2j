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
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The trait builtin of the game.
 *
 * <p>The perks need the perk engine and the traits the trait engine, and
 * the port has neither of them yet, so only the things of the object are
 * known. The setup mirrors the harness flow of {@link InventoryTest}.
 */
class GameHasTraitTest {

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
    void theThingsOfAnObject() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        // The rotation of the object.
        assertEquals(dog.rotation.id(), api.hasTrait(1, dog, 10));

        // The object is not hidden.
        assertEquals(1, api.hasTrait(1, dog, 666));

        // The weight of the inventory of the object.
        assertEquals(0, api.hasTrait(1, dog, 669));

        // The perks are not known without the perk engine.
        assertEquals(0, api.hasTrait(0, dog, 0));

        // And there is no trait without an object.
        assertEquals(0, api.hasTrait(1, null, 10));
    }
}
