package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.fallout2ce.ArtDefs.MiscFrameId;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjectType;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [objplaceontile]} vectors from
 * {@code docker/refharness/golden/v100.txt}, produced by
 * {@code sectionObjPlaceOnTile} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Pure slice: no mounts, no window system. Objects are initialized, then a
 * ScrollBlocker object is created with the Empty pid (no proto load), placed
 * on tile 20100 via {@code objectSetLocation}, found via {@code
 * objectFindFirst}, moved to tile 20517 via {@code objectSetLocation}, found
 * again, and destroyed.
 *
 * <p>The empty pid yields {@code pid=-1} in both C++ and Java, and object
 * creation/placement are fully deterministic here.
 */
class MapPlaceOnTileTest {

    @Test
    void objectPlaceOnTileVectors() {
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        byte[] objBuf = new byte[640 * 480];
        ObjectSystem.objectsInit(objBuf, 640, 480, 640);

        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithFrmIdProtoId(
                slot,
                FrmId.ofType(ObjectType.MISC, MiscFrameId.ScrollBlocker.id()),
                ProtoId.empty()));
        GameObject obj = slot[0];
        assertNotNull(obj);
        assertEquals(-1, obj.pid);

        assertEquals(0, ObjectSystem.objectSetLocation(obj, 20100, 0));
        assertEquals(20100, obj.tile);
        assertEquals(0, obj.elevation);

        boolean found = false;
        for (GameObject o = ObjectSystem.objectFindFirst(); o != null;
                o = ObjectSystem.objectFindNext()) {
            if (o == obj) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        assertEquals(0, ObjectSystem.objectSetLocation(obj, 20517, 0));
        assertEquals(20517, obj.tile);
        assertEquals(0, obj.elevation);

        found = false;
        for (GameObject o = ObjectSystem.objectFindFirst(); o != null;
                o = ObjectSystem.objectFindNext()) {
            if (o == obj) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        assertEquals(0, ObjectSystem.objectDestroy(obj));
    }
}
