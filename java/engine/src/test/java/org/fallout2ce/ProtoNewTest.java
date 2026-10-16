package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [protonew]} vectors from
 * {@code docker/refharness/golden/v74.txt}: fresh-id creation for
 * {@code OBJ_TYPE_ITEM} (pid 1 off the reset counter, MISC/METAL defaults)
 * and the invalid-type miss. Disk-free: the created proto is found by the
 * list scan, so no VFS/game data is needed.
 */
class ProtoNewTest {

    @BeforeEach
    void setup() {
        Db.resetForTests();
        ProtoDb.resetForTests();
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
    }

    @Test
    void protoNewVectors() {
        ProtoId[] idSlot = new ProtoId[1];
        int newRc = ProtoDb.protoNew(idSlot, ObjTypes.ObjectType.OBJ_TYPE_ITEM.id());
        assertEquals(0, newRc);

        // get=0 via the list scan (no disk): the created proto is found.
        Proto[] got = new Proto[1];
        int getRc = ProtoDb.protoGetProto(idSlot[0], got);
        assertEquals(
                "new-rc=0 newpid=1 get=0 weight=10 cost=0 type=5 material=1",
                "new-rc=" + newRc
                        + " newpid=" + idSlot[0].pid()
                        + " get=" + getRc
                        + " weight=" + got[0].item.weight
                        + " cost=" + got[0].item.cost
                        + " type=" + got[0].item.type.id()
                        + " material=" + got[0].item.material.id());

        // List scan returns the SAME object (cache hit, still no disk).
        Proto[] again = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(idSlot[0], again));
        assertTrue(got[0] != null && got[0] == again[0]);

        // The successful new bumped the item counter exactly once (1 -> 2).
        assertEquals(2, ProtoDb.protoMaxId(ObjTypes.ObjectType.OBJ_TYPE_ITEM.id()));

        // Invalid type: -1 with no side effects (out-param untouched,
        // counters unchanged).
        ProtoId[] badSlot = new ProtoId[1];
        int badRc = ProtoDb.protoNew(badSlot, ObjTypes.ObjectType.OBJ_TYPE_INVALID.id());
        assertEquals("bad-rc=-1", "bad-rc=" + badRc);
        assertEquals(2, ProtoDb.protoMaxId(ObjTypes.ObjectType.OBJ_TYPE_ITEM.id()));
    }
}
