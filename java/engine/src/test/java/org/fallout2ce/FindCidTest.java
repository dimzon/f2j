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
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [findcid]} vectors from
 * {@code docker/refharness/golden/v82.txt}, produced by
 * {@code sectionFindCid} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Game data needed for the {@code .pro} loads (setup mirrors
 * {@link ObjectCreateTest}). The flow mirrors the harness exactly: dog1,
 * dog2, stim, list order {@code [dog2, dog1, stim]}; all fresh cids are
 * {@code -1} ({@code obj_allocate}), so the first match lands at 0, the
 * from-1 scan at 1, and the missing cid yields the length 3. Asserts the
 * line verbatim.
 *
 * <p>No new state is introduced ({@link Combat#findCid} is a pure scan),
 * so no {@code Combat} reset is needed.
 */
class FindCidTest {

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
    void findCidVectors() {
        GameObject[] dog1Slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dog1Slot, ProtoId.ofPid(16777558)));
        assertNotNull(dog1Slot[0]);
        GameObject dog1 = dog1Slot[0];

        GameObject[] dog2Slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dog2Slot, ProtoId.ofPid(16777558)));
        assertNotNull(dog2Slot[0]);
        GameObject dog2 = dog2Slot[0];

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);
        GameObject stim = stimSlot[0];

        GameObject[] list = { dog2, dog1, stim };
        int foundDog = Combat.findCid(0, dog1.cid, list, 3);
        int foundFrom1 = Combat.findCid(1, dog1.cid, list, 3);
        int notFound = Combat.findCid(0, 9999, list, 3);
        String line = String.format("cid=%d found=%d from1=%d missing=%d",
                dog1.cid, foundDog, foundFrom1, notFound);
        assertEquals("cid=-1 found=0 from1=1 missing=3", line);
    }
}
