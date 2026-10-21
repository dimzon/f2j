package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/** Temporary probe: is the dude proto the one of the critter type "Dude"? */
class ProbeProtosTest {

    @Test
    void probe() throws Exception {
        String root = System.getenv("F2J_GAMEDATA");
        Path master = Paths.get(root, "master.dat");
        assertTrue(Files.isRegularFile(master));
        String gameEn = Paths.get(root, "master.dat").getParent().toString();

        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        XFile.xbaseOpen(gameEn + "/master.dat");

        ProtoId dude = ProtoId.ofCritterType(ProtoTypes.CritterProtoTypeId.Dude);
        System.out.println("PROBE pid игрока = " + dude.protoId() + " type=" + dude.objectType());

        Proto[] slot = new Proto[1];
        int rc = ProtoDb.protoGetProto(dude, slot);
        System.out.println("PROBE protoGetProto rc=" + rc);
        if (rc != -1 && slot[0] != null) {
            Proto proto = slot[0];
            System.out.println("PROBE pid=" + proto.pid + " fid=" + proto.fid
                    + " messageId=" + proto.messageId + " sid=" + proto.sid
                    + " flags=" + proto.flags + " critter=" + proto.critter);
        }

        GameObject[] created = new GameObject[1];
        int crc = ObjectSystem.objectCreateWithProtoId(created, dude);
        System.out.println("PROBE create rc=" + crc + " obj=" + (created[0] == null ? "нет" : ("fid=" + created[0].fid)));
    }
}
