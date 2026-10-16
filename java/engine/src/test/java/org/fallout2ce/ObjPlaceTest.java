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
 * {@code [objplace]} vectors from
 * {@code docker/refharness/golden/v86.txt}, produced by
 * {@code sectionObjPlace} in {@code docker/refharness/ref_main.cc}:
 * {@code locbad=-1,-1 null=-1 loc=0 tile=20100 elev=0 find=16777558
 * missnull=1}, {@code bright=65536 float=65536} (the 70000 tile
 * intensity clamps to {@code MAX}=ambient), {@code frame=0 bad=-1
 * null=-1 en=0 dis=0}. Game data needed for the {@code .pro} loads
 * plus critter art for the frame lock (setup mirrors
 * {@link ObjectCreateTest} + {@link ArtLoadTest}).
 */
class ObjPlaceTest {

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
        Art.resetForTests();
        Light.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/patch000.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/critter.dat"));
        assertEquals(0, Art.artInit());
    }

    @AfterEach
    void teardown() {
        Art.resetForTests();
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        Light.resetForTests();
    }

    @Test
    void objplaceVectors() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        GameObject dog = dogSlot[0];
        assertNotNull(dog);

        // locbad=-1,-1 null=-1 loc=0 tile=20100 elev=0 find=16777558 missnull=1.
        assertEquals(-1, ObjectSystem.objectSetLocation(dog, -1, 0));
        assertEquals(-1, ObjectSystem.objectSetLocation(dog, 20100, 9));
        assertEquals(-1, ObjectSystem.objectSetLocation(null, 20100, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(dog, 20100, 0));
        assertEquals(20100, dog.tile);
        assertEquals(0, dog.elevation);
        GameObject placed = ObjectSystem.objectFindById(dog.id);
        assertNotNull(placed);
        assertEquals(16777558, placed.pid);
        assertNull(ObjectSystem.objectFindById(999999));

        // bright=65536 float=65536 (70000 clamps to MAX=ambient).
        Light.lightSetTileIntensity(0, 20100, 70000);
        assertEquals(65536, ObjectSystem.objectGetLightIntensity(dog));
        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);
        assertEquals(65536, ObjectSystem.objectGetLightIntensity(stimSlot[0]));

        // frame=0 bad=-1 null=-1 en=0 dis=0.
        assertEquals(0, ObjectSystem.objectSetFrame(dog, 0));
        assertEquals(-1, ObjectSystem.objectSetFrame(dog, 999));
        assertEquals(-1, ObjectSystem.objectSetFrame(null, 0));
        assertEquals(0, ObjectSystem.objectEnableOutline(dog));
        assertEquals(0, ObjectSystem.objectDisableOutline(dog));
    }
}
