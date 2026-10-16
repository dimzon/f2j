package org.fallout2ce;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjTypes.InventoryItem;
import org.fallout2ce.ObjTypes.ObjectFlags;
import org.fallout2ce.ProtoTypes.ItemType;

/**
 * Inventory id tables, mirroring the pure parts of {@code src/inventory.h},
 * plus the inventory lookup slice of {@code src/inventory.cc}:
 * {@code inventoryItemByIndex}, {@code inventoryFindByType} and the
 * equipped-item scans ({@code critterGetItem1}/{@code critterGetItem2}/
 * {@code critterGetArmor}) backing the {@link Item} weight/cost extras.
 *
 * <p>Vectors: {@code [inventory]} in
 * {@code docker/refharness/golden/v59.txt}, produced by
 * {@code sectionInventory} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>NOTE: the {@code _inven_dude} hand-override globals
 * ({@code gInventoryLeftHandItem}/{@code gInventoryRightHandItem}/
 * {@code gInventoryArmor}) are inventory-UI state (game phase); the scans
 * below read the inventory flags directly, which matches C++ whenever the
 * UI overrides are null (always in the harness).
 */
public final class Inventory {

    private Inventory() {
    }

    public static final int INVENTORY_SLOT_WIDTH = 64;
    public static final int INVENTORY_SLOT_HEIGHT = 48;

    public static final int EXPANDED_BARTER_EXTRA_SLOTS = 1;

    public static final int HAND_COUNT = 2;

    public enum Hand {
        HAND_LEFT(0),
        HAND_RIGHT(1);

        private final int id;

        Hand(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, Hand> HAND_BY_ID = new HashMap<>();

    static {
        for (Hand e : Hand.values()) {
            HAND_BY_ID.put(e.id, e);
        }
    }

    public static Hand ofHand(int id) {
        return HAND_BY_ID.get(id);
    }

    /** Port of {@code CritterEquipped}. */
    public static final class CritterEquipped {
        public ObjTypes.GameObject leftHand;
        public ObjTypes.GameObject rightHand;
        public ObjTypes.GameObject armor;
        public int weight;
    }

    // 0x472740 inven_item_by_index: out of bounds -> null.
    public static GameObject inventoryItemByIndex(GameObject obj, int index) {
        List<InventoryItem> items = obj.data.inventory.items;

        if (index < 0 || index >= items.size()) {
            return null;
        }

        return items.get(index).item;
    }

    // 0x472698 inven_find_by_type: finds the next item of the given type
    // (id -1 matches any). The index starts at -1 (*indexPtr += 1 first);
    // a null pointer uses a throwaway dummy, like C++. Returns the item or
    // null. Out-param uses the single-element-array convention (see
    // ProtoDb#protoGetProto).
    public static GameObject inventoryFindByType(GameObject obj, ItemType itemType, int[] indexPtr) {
        int[] slot = indexPtr != null ? indexPtr : new int[] { -1 };

        slot[0] += 1;

        List<InventoryItem> items = obj.data.inventory.items;

        if (slot[0] >= items.size()) {
            return null;
        }

        while (itemType.id() != -1 && Item.itemGetType(items.get(slot[0]).item) != itemType) {
            slot[0] += 1;

            if (slot[0] >= items.size()) {
                return null;
            }
        }

        return items.get(slot[0]).item;
    }

    // 0x471BBC critter_get_item1 (left hand flag scan).
    public static GameObject critterGetItem1(GameObject critter) {
        for (InventoryItem entry : critter.data.inventory.items) {
            if ((entry.item.flags.bits() & ObjectFlags.IN_LEFT_HAND.bits()) != 0) {
                return entry.item;
            }
        }

        return null;
    }

    // critter_get_item2 (right hand flag scan).
    public static GameObject critterGetItem2(GameObject critter) {
        for (InventoryItem entry : critter.data.inventory.items) {
            if ((entry.item.flags.bits() & ObjectFlags.IN_RIGHT_HAND.bits()) != 0) {
                return entry.item;
            }
        }

        return null;
    }

    // 0x471C08 critter_get_armor (worn flag scan).
    public static GameObject critterGetArmor(GameObject critter) {
        for (InventoryItem entry : critter.data.inventory.items) {
            if ((entry.item.flags.bits() & ObjectFlags.WORN.bits()) != 0) {
                return entry.item;
            }
        }

        return null;
    }
}
