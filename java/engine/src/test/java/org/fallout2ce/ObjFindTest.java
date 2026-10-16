package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [objfind]} vectors from
 * {@code docker/refharness/golden/v67.txt}, produced by
 * {@code sectionObjFind} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The harness runs in one live process, so {@code [objfind]} sees
 * leftovers from the earlier {@code [itemdrop]} section at tile 20100:
 * {@code pre-null=0 count=4 pids=40,41,40} (old stim, old cash, new stim,
 * plus an unprinted 4th 41), {@code destroydog=0 count-after-dog=4}
 * (the dog floats, invisible to find), {@code destroystim=0
 * first-after=40} (the new stim is unlinked from the tile head).
 *
 * <p>This test is self-contained (fresh reset, no leftovers), so the
 * adapted expectations are: pre is null (no tile objects yet), the fresh
 * {@code itemDropAll} chain at tile 20100 is exactly {@code [stim40,
 * cash41]} (append order from {@code _obj_insert}: equal elevation +
 * equal flatness keeps arrival order, since the C art-lock depth
 * tiebreak never fires), count 2; destroying the floating dog changes
 * nothing (count stays 2); destroying the tile-bound stim leaves
 * {@code first-after=41}. Game data needed for the {@code .pro} loads
 * (setup mirrors {@link ObjectCreateTest}).
 */
class ObjFindTest {

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

    private static List<Integer> collectPids() {
        List<Integer> pids = new ArrayList<>();
        for (GameObject obj = ObjectSystem.objectFindFirst();
                obj != null;
                obj = ObjectSystem.objectFindNext()) {
            pids.add(obj.pid);
        }
        return pids;
    }

    @Test
    void objfindVectors() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);
        GameObject stim = stimSlot[0];

        GameObject[] cashSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(cashSlot, ProtoId.ofPid(41)));
        assertNotNull(cashSlot[0]);
        GameObject cash = cashSlot[0];

        // Fresh process: no tile objects yet, so find sees nothing.
        // (C prints pre-null=0 only because of [itemdrop] leftovers.)
        assertNull(ObjectSystem.objectFindFirst());

        assertEquals(0, Item.itemAdd(dog, stim, 1));
        assertEquals(0, Item.itemAdd(dog, cash, 2));
        assertEquals(0, Item.itemDropAll(dog, 20100));

        // Full walk: fresh chain is exactly [stim40, cash41] (append order).
        List<Integer> pids = collectPids();
        assertEquals(2, pids.size());
        assertEquals(40, pids.get(0));
        assertEquals(41, pids.get(1));

        // Elevation-filtered walk sees the same pair at elevation 0.
        GameObject firstAt0 = ObjectSystem.objectFindFirstAtElevation(0);
        assertNotNull(firstAt0);
        assertEquals(40, firstAt0.pid);
        GameObject secondAt0 = ObjectSystem.objectFindNextAtElevation();
        assertNotNull(secondAt0);
        assertEquals(41, secondAt0.pid);
        assertNull(ObjectSystem.objectFindNextAtElevation());
        assertNull(ObjectSystem.objectFindFirstAtElevation(1));

        // Destroying the floating dog changes nothing visible.
        assertEquals(0, ObjectSystem.objectDestroy(dog));
        List<Integer> afterDog = collectPids();
        assertEquals(2, afterDog.size());
        assertEquals(40, afterDog.get(0));
        assertEquals(41, afterDog.get(1));

        // Destroying the tile-bound stim shrinks the chain to [cash41].
        assertEquals(0, ObjectSystem.objectDestroy(stim));
        GameObject firstAfter = ObjectSystem.objectFindFirst();
        assertNotNull(firstAfter);
        assertEquals(41, firstAfter.pid);
        List<Integer> afterStim = collectPids();
        assertEquals(1, afterStim.size());
        assertEquals(41, afterStim.get(0));
    }
}
