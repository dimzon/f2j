package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [protodb]} vectors from
 * {@code docker/refharness/golden/v47.txt}: path building, stimpak lazy
 * load + cache hit, integer data members (incl. the bad-member stale-value
 * quirk) and invalid-id misses. No window system needed (VFS only, no
 * {@code protoInit}).
 */
class ProtoDbTest {

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
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
    }

    @Test
    void protodbVectors() {
        assertEquals("proto\\items", ProtoDb.protoMakePath(ProtoId.ofPid(40)));

        Proto[] stim1 = new Proto[1];
        Proto[] stim2 = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofPid(40), stim1));
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofPid(40), stim2));
        assertTrue(stim1[0] != null && stim1[0] == stim2[0]);
        assertEquals(40, stim1[0].item.pid);
        assertEquals(4000, stim1[0].item.messageId);
        assertEquals(15, stim1[0].item.fid);
        assertEquals(2, stim1[0].item.type.id());
        assertEquals(1, stim1[0].item.material.id());
        assertEquals(0, stim1[0].item.size);
        assertEquals(0, stim1[0].item.weight);
        assertEquals(175, stim1[0].item.cost);
        assertEquals(0, stim1[0].item.lightDistance);
        assertEquals(0, stim1[0].item.lightIntensity);
        assertEquals(8, stim1[0].item.flags.bits());

        ProtoDefs.ProtoDataMemberValue memberValue = new ProtoDefs.ProtoDataMemberValue();
        assertEquals(1, ProtoDb.protoGetDataMember(ProtoId.ofPid(40), 0, memberValue));
        assertEquals(40, memberValue.integerValue);
        assertEquals(1, ProtoDb.protoGetDataMember(ProtoId.ofPid(40), 13, memberValue));
        assertEquals(0, memberValue.integerValue);
        assertEquals(1, ProtoDb.protoGetDataMember(ProtoId.ofPid(40), 14, memberValue));
        assertEquals(175, memberValue.integerValue);
        assertEquals(1, ProtoDb.protoGetDataMember(ProtoId.ofPid(40), 9999, memberValue));

        Proto[] badProto = new Proto[1];
        assertEquals(-1, ProtoDb.protoGetProto(ProtoId.empty(), badProto));
        assertEquals(-1, ProtoDb.protoGetProto(ProtoId.ofPid(16777215), badProto));
        assertNull(badProto[0]);
    }
}
