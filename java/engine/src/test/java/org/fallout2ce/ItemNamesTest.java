package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * {@code [itemnames]} vectors from
 * {@code docker/refharness/golden/v87.txt}, produced by
 * {@code sectionItemNames} in {@code docker/refharness/ref_main.cc}:
 * {@code init=1 load=1 stim=1:[Stimpak] cash=1:[Money] miss=0} and
 * {@code repoinit=1 extra=12288}.
 *
 * <p>Path mirrors the harness exactly: {@code asc_5186C8} ({@code "game\\"})
 * plus {@code "pro_item.msg"} ({@code "pro_"} + first 4 of the
 * {@code "items"} type name + {@code ".msg"}), loaded through
 * {@link Message#messageListLoad} like the v45 DBox path
 * ({@code "game\\DBOX.MSG"}). Stimpak uses the hardcoded harness id 4000
 * ({@code NAME} offset 0); money uses the {@code messageId} from the loaded
 * proto struct ({@link ProtoDb#protoGetProto}). No window system needed.
 */
class ItemNamesTest {

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
        Message.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        // Mirror WinDbgDboxTest: patch/critter mounts when present.
        if (Files.isRegularFile(Paths.get(gameEn, "patch000.dat"))) {
            XFile.xbaseOpen(gameEn + "/patch000.dat");
        }
        if (Files.isRegularFile(Paths.get(gameEn, "critter.dat"))) {
            XFile.xbaseOpen(gameEn + "/critter.dat");
        }
    }

    @AfterEach
    void teardown() {
        Message.resetForTests();
        Db.resetForTests();
        ProtoDb.resetForTests();
    }

    @Test
    void itemNamesVectors() {
        // asc_5186C8 ("game\\") + "pro_item.msg".
        String path = "game\\" + "pro_item.msg";

        Message.MessageList msgList = new Message.MessageList();
        assertTrue(Message.messageListInit(msgList));
        assertTrue(Message.messageListLoad(msgList, path));

        Message.Item stimItem = new Message.Item();
        stimItem.num = 4000;
        assertTrue(Message.messageListGetItem(msgList, stimItem));
        assertEquals("Stimpak", stimItem.text);

        Proto[] moneySlot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofPid(41), moneySlot));
        assertNotNull(moneySlot[0]);
        Message.Item cashItem = new Message.Item();
        cashItem.num = moneySlot[0].item.messageId;
        assertTrue(Message.messageListGetItem(msgList, cashItem));
        assertEquals("Money", cashItem.text);

        Message.Item missItem = new Message.Item();
        missItem.num = 999999;
        assertFalse(Message.messageListGetItem(msgList, missItem));

        assertTrue(Message.messageListFree(msgList));

        assertTrue(Message.messageListRepositoryInit());
        assertEquals(12288, Message.messageListRepositoryAddExtra(path));
        Message.messageListRepositoryExit();
    }
}
