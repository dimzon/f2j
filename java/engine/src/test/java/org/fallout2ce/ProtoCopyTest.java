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
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [protocopy]} vectors from
 * {@code docker/refharness/golden/v75.txt}: fresh-id creation off a clean
 * counter (pid 1, not the harness pid 2: the C process runs this right
 * after {@code [protonew}], which had already bumped the item counter),
 * the stim(40) copy (weight 10 -&gt; 0, dst pid stamped back),
 * cross-type/missing-src misses, the drug-subtype probes, the object
 * twin (fresh id off the shared object counter, distinct pointer) and
 * the inventory cost probes (carrier 175, bare 0).
 *
 * <p>Id adaptation: the fresh Java test owns its creations, so the
 * counter-derived ids are stim 6 (first creation, counter 4 -&gt; 5 scan
 * -&gt; 6 return), twin 8 and carrier 10 — the same double-increment
 * pattern {@link ObjectCreateTest} pins (stim 6, cash 8). Game data is
 * needed for the {@code .pro} loads (setup mirrors
 * {@link ObjectCreateTest}).
 */
class ProtoCopyTest {

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
    void protocopyVectors() {
        // Fresh proto_new (ITEM): pid 1 off the reset counter. The C
        // harness reports fresh=0:2 only because [protonew] ran first in
        // the same process and bumped the item counter to 2.
        ProtoId[] freshSlot = new ProtoId[1];
        int freshRc = ProtoDb.protoNew(freshSlot, ObjTypes.ObjectType.OBJ_TYPE_ITEM.id());
        assertEquals(0, freshRc);
        assertEquals(1, freshSlot[0].pid());

        // Copy stim(40) over the fresh proto: rc 0, the proto_new
        // weight default (10) is overwritten by the stim weight (0),
        // and the dst pid is stamped back over the copied header.
        int copyRc = ProtoDb.protoCopyProto(ProtoId.ofPid(40), freshSlot[0]);
        Proto[] copied = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(freshSlot[0], copied));
        assertEquals(
                "fresh=0:1 copy=0 weight=0 pid=1",
                "fresh=" + freshRc + ":" + freshSlot[0].pid()
                        + " copy=" + copyRc
                        + " weight=" + copied[0].item.weight
                        + " pid=" + copied[0].pid);

        // Cross-type (item -> critter) and missing-src copies miss.
        int crossRc = ProtoDb.protoCopyProto(ProtoId.ofPid(40), ProtoId.ofPid(16777558));
        int missRc = ProtoDb.protoCopyProto(ProtoId.ofPid(999), freshSlot[0]);
        assertEquals("cross=-1 miss=-1", "cross=" + crossRc + " miss=" + missRc);

        // The failed miss leaves the dst untouched (still stim weight).
        Proto[] afterMiss = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(freshSlot[0], afterMiss));
        assertEquals(0, afterMiss[0].item.weight);

        // Subtype probes on the stim proto (ITEM_TYPE_DRUG = 2).
        Proto[] stimProto = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofPid(40), stimProto));
        int sub2 = ProtoDb.protoIsSubtype(stimProto[0], 2) ? 1 : 0;
        int sub3 = ProtoDb.protoIsSubtype(stimProto[0], 3) ? 1 : 0;
        int subAny = ProtoDb.protoIsSubtype(stimProto[0], -1) ? 1 : 0;
        assertEquals("sub=1,0,1", "sub=" + sub2 + "," + sub3 + "," + subAny);

        // Object twin: stim create (id 6) then copy (id 8), same pid,
        // distinct pointer.
        GameObject[] stim = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stim, ProtoId.ofPid(40)));
        assertNotNull(stim[0]);
        assertEquals(6, stim[0].id);
        GameObject[] twin = new GameObject[1];
        int copyObjRc = ObjectSystem.objCopy(twin, stim[0]);
        assertNotNull(twin[0]);
        assertEquals(
                "objcopy=0 twinpid=40 twinid=8 sametwin=1",
                "objcopy=" + copyObjRc
                        + " twinpid=" + twin[0].pid
                        + " twinid=" + twin[0].id
                        + " sametwin=" + (twin[0] != stim[0] ? 1 : 0));

        // Costs: dog carrier holding the stim prices 175, the bare stim
        // prices 0.
        GameObject[] carrier = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(carrier, ProtoId.ofPid(16777558)));
        assertEquals(0, Item.itemAdd(carrier[0], stim[0], 1));
        int objCost = Item.objectGetCost(carrier[0]);
        int bareCost = Item.objectGetCost(stim[0]);
        assertEquals("objcost=175 barecost=0", "objcost=" + objCost + " barecost=" + bareCost);
    }
}
