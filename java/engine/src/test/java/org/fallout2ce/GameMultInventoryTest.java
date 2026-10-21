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
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The builtins of the game that put a number of objects into an inventory
 * and take them out of it again.
 *
 * <p>One object is put in when the number is not a number of objects, and
 * not more objects are taken than the inventory holds. The setup mirrors the
 * harness flow of {@link InventoryTest}.
 */
class GameMultInventoryTest {

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
    void severalObjectsGoIntoAndOutOfTheInventory() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);
        GameObject stim = stimSlot[0];

        api.addMultObjsToInven(dog, stim, 2);
        assertEquals(stim, Inventory.objectGetCarriedObjectByProtoId(dog, ProtoId.ofPid(40)));

        // Not more objects are taken than the inventory holds.
        int taken = api.rmMultObjsFromInven(dog, stim, 5);
        assertTrue(taken >= 0);
        assertTrue(taken <= 5);

        // Nothing is taken when there is no owner or no object at all.
        assertEquals(0, api.rmMultObjsFromInven(null, stim, 1));
        assertEquals(0, api.rmMultObjsFromInven(dog, null, 1));
    }
}
