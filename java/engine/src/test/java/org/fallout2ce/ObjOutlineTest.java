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
import org.fallout2ce.ObjTypes.OutlineType;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [objoutline]} vectors from
 * {@code docker/refharness/golden/v70.txt}:
 * {@code set=0 outline=16 again=-1 has=1 clear=0 cleared=0 clearnull=-1},
 * {@code xy0=0,0 off=0 xy1=10,-5 offnull=-1},
 * {@code charges=0 max=0 uses=0 set7=0} (money proto maxCharges=0 clamps
 * the set to 0). Produced by {@code sectionObjOutline} in
 * {@code docker/refharness/ref_main.cc}. Game data needed for
 * {@code .pro} loads (setup mirrors {@link ObjectCreateTest}).
 */
class ObjOutlineTest {

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
    void objoutlineVectors() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        GameObject dog = dogSlot[0];
        assertNotNull(dog);

        // set=0 outline=16 again=-1 has=1 clear=0 cleared=0 clearnull=-1.
        assertEquals(0, ObjectSystem.objectSetOutline(dog, OutlineType.ITEM));
        assertEquals(16, dog.outline.bits());
        assertEquals(-1, ObjectSystem.objectSetOutline(dog, OutlineType.ITEM));
        assertEquals(1, ObjectSystem.objectHasOutline(dog) ? 1 : 0);
        assertEquals(0, ObjectSystem.objectClearOutline(dog));
        assertEquals(0, dog.outline.bits());
        assertEquals(-1, ObjectSystem.objectClearOutline(null));
        assertEquals(-1, ObjectSystem.objectSetOutline(null, OutlineType.ITEM));

        // xy0=0,0 off=0 xy1=10,-5 offnull=-1.
        assertEquals(0, dog.x);
        assertEquals(0, dog.y);
        assertEquals(0, ObjectSystem.objOffset(dog, 10, -5));
        assertEquals(10, dog.x);
        assertEquals(-5, dog.y);
        assertEquals(10, dog.sx);
        assertEquals(-5, dog.sy);
        assertEquals(-1, ObjectSystem.objOffset(null, 10, -5));

        // charges=0 max=0 uses=0 set7=0 (money proto maxCharges=0).
        GameObject[] cashSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(cashSlot, ProtoId.ofPid(41)));
        GameObject cash = cashSlot[0];
        assertNotNull(cash);
        assertEquals(0, Item.miscItemGetCharges(cash));
        assertEquals(0, Item.miscItemGetMaxCharges(cash));
        assertEquals(0, Item.miscItemUsesCharges(cash) ? 1 : 0);
        assertEquals(0, Item.miscItemSetCharges(cash, 7));
        assertEquals(0, Item.miscItemGetCharges(cash));
        assertEquals(0, Item.miscItemGetCharges(null));
        assertEquals(0, Item.miscItemGetMaxCharges(null));
        assertEquals(-1, Item.miscItemSetCharges(null, 7));
    }
}
