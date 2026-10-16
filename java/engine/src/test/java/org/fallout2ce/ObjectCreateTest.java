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
 * {@code [objcreate]} vectors from
 * {@code docker/refharness/golden/v57.txt}: stimpak create (pid/fid/
 * flags/sid/id), invalid-pid miss, cash id bump (double-increment quirk)
 * and dogmeat pid/fid/flags. Game data needed for {@code .pro} loads
 * (setup mirrors {@link ProtoDbTest}).
 */
class ObjectCreateTest {

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
    void objcreateVectors() {
        GameObject[] stim = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stim, ProtoId.ofPid(40)));
        assertNotNull(stim[0]);
        assertEquals(40, stim[0].pid);
        assertEquals(15, stim[0].fid);
        assertEquals(8, stim[0].flags.bits());
        assertEquals(-1, stim[0].sid);
        assertEquals(6, stim[0].id);

        GameObject[] bad = new GameObject[1];
        assertEquals(-1, ObjectSystem.objectCreateWithProtoId(bad, ProtoId.empty()));
        assertNull(bad[0]);

        GameObject[] cash = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(cash, ProtoId.ofPid(41)));
        GameObject[] dog = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dog, ProtoId.ofPid(16777558)));
        assertEquals(8, cash[0].id);
        assertNotNull(dog[0]);
        assertEquals(16777558, dog[0].pid);
        assertEquals(16777232, dog[0].fid);
        assertEquals(536870912, dog[0].flags.bits());
    }
}
