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
import org.fallout2ce.ObjTypes;
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

    @Test
    void equipVectors() {
        // [equip] from docker/refharness/golden/v60.txt, produced by
        // sectionEquip in docker/refharness/ref_main.cc. Dog wields with
        // animate=false (no iso anim); the C++ art-gate failure emits an
        // INFO "inven_wield failed!" stderr line which Java stays silent
        // on (see Reaction), so only the stdout lines are asserted.
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        GameObject[] jacketSlot = new GameObject[1];
        int jacketRc = ObjectSystem.objectCreateWithProtoId(jacketSlot, ProtoId.ofPid(3));
        GameObject jacket = jacketSlot[0];
        int jacketType = jacket != null ? Item.itemGetType(jacket).id() : -999;
        int equipArmor = -99;
        int wornAfter = -99;
        if (jacketRc == 0 && jacketType == ProtoTypes.ItemType.ITEM_TYPE_ARMOR.id()) {
            equipArmor = Inventory.inventoryEquipFunc(dog, jacket, Hand.HAND_RIGHT, false);
            wornAfter = (jacket.flags.bits() & ObjTypes.ObjectFlags.WORN.bits()) != 0 ? 1 : 0;
        }
        assertEquals(0, jacketRc);
        assertEquals(0, jacketType);
        assertEquals(0, equipArmor);
        assertEquals(1, wornAfter);

        // Weapon skill/attack mapping is pure proto reads (scorcher=390).
        GameObject[] gunSlot = new GameObject[1];
        int gunRc = ObjectSystem.objectCreateWithProtoId(gunSlot, ProtoId.ofPid(390));
        GameObject gun = gunSlot[0];
        int gunType = gun != null ? Item.itemGetType(gun).id() : -999;
        int gunSkill = gun != null
                ? Item.weaponGetSkillForHitMode(
                        gun, CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id())
                : -999;
        int gunAttack = gun != null
                ? Item.weaponGetAttackTypeForHitMode(
                        gun, CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id())
                : -999;
        assertEquals(0, gunRc);
        assertEquals(3, gunType);
        assertEquals(2, gunSkill);
        assertEquals(4, gunAttack);

        // Dog has no gun frames: art gate rejects the wield, no crash.
        int equipGun = -99;
        if (gunRc == 0) {
            equipGun = Inventory.inventoryEquipFunc(dog, gun, Hand.HAND_RIGHT, false);
        }
        GameObject handItem = Inventory.critterGetItem2(dog);
        assertEquals(-1, equipGun);
        assertEquals(-1, handItem != null ? handItem.pid : -1);
    }
}
