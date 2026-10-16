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

import org.fallout2ce.Inventory.Hand;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [gametypes]} inventory vectors from
 * {@code docker/refharness/golden/v35.txt}, plus the {@code [inventory]}
 * item-accessor vectors from {@code docker/refharness/golden/v59.txt}
 * (produced by {@code sectionInventory} in
 * {@code docker/refharness/ref_main.cc}).
 *
 * <p>The accessor test mirrors the harness flow exactly (dog + stimpak +
 * cash creates, weight/cost/type probes, two adds, quantity/slot/find
 * probes, stimpak removal + shift probes). Game data needed for the
 * {@code .pro} loads (setup mirrors {@link ObjectCreateTest}).
 */
class InventoryTest {

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
    void handAndSlots() {
        assertEquals(2, Inventory.HAND_COUNT);
        assertEquals(Hand.HAND_LEFT, Inventory.ofHand(0));
        assertEquals(Hand.HAND_RIGHT, Inventory.ofHand(1));
        assertNull(Inventory.ofHand(2));
        assertEquals(64, Inventory.INVENTORY_SLOT_WIDTH);
        assertEquals(48, Inventory.INVENTORY_SLOT_HEIGHT);
        assertEquals(1, Inventory.EXPANDED_BARTER_EXTRA_SLOTS);
    }

    @Test
    void critterEquipped() {
        Inventory.CritterEquipped equipped = new Inventory.CritterEquipped();
        assertNull(equipped.leftHand);
        assertNull(equipped.rightHand);
        assertNull(equipped.armor);
        assertEquals(0, equipped.weight);

        ObjTypes.GameObject armor = new ObjTypes.GameObject();
        equipped.armor = armor;
        equipped.weight = 1200;
        assertEquals(armor, equipped.armor);
        assertEquals(1200, equipped.weight);
    }

    @Test
    void inventoryVectors() {
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

        // w=0 cost=175 types=2,5 invw0=0 add=0,0 invw1=0 qty=3
        // slot0=40 slot9null=1 find=41@1
        assertEquals(0, Item.itemGetWeight(stim));
        assertEquals(175, Item.itemGetCost(stim));
        assertEquals(2, Item.itemGetType(stim).id());
        int cashType = Item.itemGetType(cash).id();
        assertEquals(5, cashType);
        assertEquals(0, Item.objectGetInventoryWeight(dog));
        assertEquals(0, Item.itemAdd(dog, stim, 1));
        assertEquals(0, Item.itemAdd(dog, cash, 3));
        assertEquals(0, Item.objectGetInventoryWeight(dog));
        assertEquals(3, Item.itemGetQuantity(dog, cash));

        GameObject slot0 = Inventory.inventoryItemByIndex(dog, 0);
        assertNotNull(slot0);
        assertEquals(40, slot0.pid);
        assertNull(Inventory.inventoryItemByIndex(dog, 9));

        int[] findIdx = new int[] { -1 };
        GameObject found = Inventory.inventoryFindByType(
                dog, ProtoTypes.ofItemType(cashType), findIdx);
        assertNotNull(found);
        assertEquals(41, found.pid);
        assertEquals(1, findIdx[0]);

        // rem=0 slot0after=41 qtyafter=0
        assertEquals(0, Item.itemRemove(dog, stim, 1));
        GameObject slot0After = Inventory.inventoryItemByIndex(dog, 0);
        assertNotNull(slot0After);
        assertEquals(41, slot0After.pid);
        assertEquals(0, Item.itemGetQuantity(dog, stim));
    }
}
