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
import org.fallout2ce.ObjTypes.Rotation;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [objattrs]} vectors from
 * {@code docker/refharness/golden/v68.txt}: rotation flow (clockwise x2,
 * counter-clockwise, set NW, invalid-6, null) and ownership flow
 * (free/add/move). Produced by {@code sectionObjAttrs} in
 * {@code docker/refharness/ref_main.cc}. Game data needed for {@code .pro}
 * loads (setup mirrors {@link ObjectCreateTest}).
 */
class ObjAttrsTest {

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
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
    }

    @Test
    void objattrsVectors() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        GameObject dog = dogSlot[0];
        assertNotNull(dog);

        int rot0 = dog.rotation.id();
        int cwRc1 = ObjectSystem.objectRotateClockwise(dog);
        int cwRc2 = ObjectSystem.objectRotateClockwise(dog);
        int rot2 = dog.rotation.id();
        int ccwRc = ObjectSystem.objectRotateCounterClockwise(dog);
        int rot1 = dog.rotation.id();
        int setRc = ObjectSystem.objectSetRotation(dog, Rotation.ROTATION_NW.id());
        int rotNw = dog.rotation.id();
        int setBad = ObjectSystem.objectSetRotation(dog, 6);
        int rotAfterBad = dog.rotation.id();
        int setNull = ObjectSystem.objectSetRotation(null, Rotation.ROTATION_NE.id());

        assertEquals(0, rot0);
        assertEquals(0, cwRc1);
        assertEquals(0, cwRc2);
        assertEquals(2, rot2);
        assertEquals(0, ccwRc);
        assertEquals(1, rot1);
        assertEquals(0, setRc);
        assertEquals(5, rotNw);
        assertEquals(-1, setBad);
        assertEquals(5, rotAfterBad);
        assertEquals(-1, setNull);

        GameObject[] dog2Slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dog2Slot, ProtoId.ofPid(16777558)));
        GameObject dog2 = dog2Slot[0];
        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        GameObject stim = stimSlot[0];
        assertNotNull(stim);

        GameObject ownerFree = ObjectSystem.objectGetOwner(stim);
        assertNull(ownerFree);

        assertEquals(0, Item.itemAdd(dog, stim, 1));
        GameObject ownerAfterAdd = ObjectSystem.objectGetOwner(stim);
        assertNotNull(ownerAfterAdd);

        Item.itemMoveForce(dog, dog2, stim, 1);
        GameObject ownerAfterMove = ObjectSystem.objectGetOwner(stim);

        assertEquals(1, ownerFree == null ? 1 : 0);
        assertEquals(0, ownerAfterAdd == null ? 1 : 0);
        assertEquals(1, ownerAfterMove == dog2 ? 1 : 0);
    }
}
