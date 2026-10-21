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
 * The inventory builtin of the game.
 *
 * <p>Nothing happens when there is no owner or no object at all. The setup
 * mirrors the harness flow of {@link InventoryTest}.
 */
class GameAddObjToInvenTest {

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
    void anObjectGoesIntoTheInventory() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);
        GameObject stim = stimSlot[0];

        // The critter carries nothing yet.
        assertNull(Inventory.objectGetCarriedObjectByProtoId(dog, ProtoId.ofPid(40)));

        api.addObjToInven(dog, stim);

        // The stim is carried now.
        assertEquals(stim, Inventory.objectGetCarriedObjectByProtoId(dog, ProtoId.ofPid(40)));

        // Nothing happens without an owner or without an object.
        api.addObjToInven(null, stim);
        api.addObjToInven(dog, null);
        assertNull(Inventory.objectGetCarriedObjectByProtoId(dog, ProtoId.ofPid(41)));
    }

    @Test
    void anObjectGoesOutOfTheInventory() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);
        GameObject stim = stimSlot[0];

        api.addObjToInven(dog, stim);
        assertEquals(stim, Inventory.objectGetCarriedObjectByProtoId(dog, ProtoId.ofPid(40)));

        api.rmObjFromInven(dog, stim);
        assertNull(Inventory.objectGetCarriedObjectByProtoId(dog, ProtoId.ofPid(40)));

        // Nothing happens without an owner or without an object.
        api.rmObjFromInven(null, stim);
        api.rmObjFromInven(dog, null);
    }
}
