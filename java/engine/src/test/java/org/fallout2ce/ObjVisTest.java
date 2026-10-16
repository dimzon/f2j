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
import org.fallout2ce.ObjTypes.ObjectFlags;
import org.fallout2ce.ObjTypes.OutlineType;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [objvis]} vectors from
 * {@code docker/refharness/golden/v69.txt}: hide/show cycle on the dog
 * ({@code hide=0 flag=1 hideagain=-1 show=0 showagain=-1 null=-1,-1}) and
 * the disconnect tail ({@code disc=0 tile=-1 again=0}). Produced by
 * {@code sectionObjVis} in {@code docker/refharness/ref_main.cc}. Game
 * data needed for {@code .pro} loads (setup mirrors
 * {@link ObjectCreateTest}).
 *
 * <p>The harness runs in one live process, so its chain counts include
 * {@code [itemdrop]} + {@code [objfind]} leftovers at tile 20100
 * ({@code chain=4 ... chain-after=3}). This test is self-contained (fresh
 * reset, no leftovers): its own {@code itemDropAll} of a single stim lands
 * exactly one node at tile 20100, so the adapted expectations are
 * {@code chain=1} before the disconnect and {@code chain-after=0} after.
 * Every other vector (all rcs, the hidden flag, {@code tile=-1}, and the
 * second-disconnect {@code again=0} from the double-listed floating node)
 * is asserted verbatim.
 */
class ObjVisTest {

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

    private static int chainCount() {
        int count = 0;
        for (GameObject obj = ObjectSystem.objectFindFirst();
                obj != null;
                obj = ObjectSystem.objectFindNext()) {
            count++;
        }
        return count;
    }

    @Test
    void objvisVectors() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        GameObject dog = dogSlot[0];
        assertNotNull(dog);

        // hide=0 flag=1 hideagain=-1 show=0 showagain=-1 null=-1,-1.
        assertEquals(0, ObjectSystem.objectHide(dog));
        assertEquals(1, !dog.flags.and(ObjectFlags.HIDDEN).equals(ObjectFlags.NONE) ? 1 : 0);
        // The dog carries no outline (NONE), so the hide sets no DISABLED.
        assertEquals(OutlineType.NONE, dog.outline);
        assertEquals(-1, ObjectSystem.objectHide(dog));
        assertEquals(0, ObjectSystem.objectShow(dog));
        assertEquals(ObjectFlags.NONE,
                dog.flags.and(ObjectFlags.HIDDEN));
        assertEquals(-1, ObjectSystem.objectShow(dog));
        assertEquals(-1, ObjectSystem.objectHide(null));
        assertEquals(-1, ObjectSystem.objectShow(null));

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        GameObject stim = stimSlot[0];
        assertNotNull(stim);

        assertEquals(0, Item.itemAdd(dog, stim, 1));
        assertEquals(0, Item.itemDropAll(dog, 20100));

        // Fresh state: exactly one node at tile 20100 (C prints 4 from
        // [itemdrop]+[objfind] leftovers).
        assertEquals(1, chainCount());

        // disc=0 tile=-1.
        assertEquals(0, ObjectSystem.objectDisconnect(stim));
        assertEquals(-1, stim.tile);

        // Fresh state: the tile head is empty now (C prints 3 leftovers).
        assertEquals(0, chainCount());

        // again=0: the double-listed floating node (objectConnect never
        // unlinks it) is found via the floating head and unlinked.
        assertEquals(0, ObjectSystem.objectDisconnect(stim));
    }
}
