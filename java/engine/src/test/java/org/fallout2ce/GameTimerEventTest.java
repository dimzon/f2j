package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * The timer builtins of the game.
 *
 * <p>A timer event belongs to the object whose script set it, and taking
 * the events of an object away leaves the other objects alone. The setup
 * mirrors the harness flow of {@link InventoryTest}.
 */
class GameTimerEventTest {

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
        EventQueue.queueInit();
        EventQueue.queueClear();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        api = new GameScriptApi();
    }

    @AfterEach
    void teardown() {
        EventQueue.queueClear();
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
    }

    @Test
    void theTimerEventOfAnObject() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        GameObject[] catSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(catSlot, ProtoId.ofPid(16777559)));
        assertNotNull(catSlot[0]);
        GameObject cat = catSlot[0];

        // There is no event of the object yet.
        assertFalse(EventQueue.queueHasEvent(dog, EventQueue.EventType.SCRIPT));

        // The event belongs to the object it was set for.
        api.addTimerEvent(dog, 100, 7);
        assertTrue(EventQueue.queueHasEvent(dog, EventQueue.EventType.SCRIPT));
        assertFalse(EventQueue.queueHasEvent(cat, EventQueue.EventType.SCRIPT));

        // And taking it away leaves the other objects alone.
        api.rmTimerEvent(dog);
        assertFalse(EventQueue.queueHasEvent(dog, EventQueue.EventType.SCRIPT));

        // Nothing happens without an object.
        api.addTimerEvent(null, 100, 7);
        api.rmTimerEvent(null);
    }
}
