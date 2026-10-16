package org.fallout2ce;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjTypes.InventoryItem;
import org.fallout2ce.ObjTypes.ObjectFlags;
import org.fallout2ce.ProtoTypes.ItemProtoTypeId;
import org.fallout2ce.ProtoTypes.ItemType;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoExtendedFlags;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Item id tables, mirroring the pure parts of {@code src/item.h}, plus the
 * inventory/item-accessor slice of {@code src/item.cc}: {@code itemGetType},
 * {@code itemGetWeight}, {@code itemGetCost}, {@code objectGetCost},
 * {@code objectGetInventoryWeight}, {@code itemGetQuantity}, {@code itemAdd},
 * {@code itemRemove} and the small helpers they need ({@code itemIsHidden},
 * ammo quantity/capacity, weapon ammo-type id, {@code _item_identical}).
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 *
 * <p>Vectors: {@code [inventory]} in
 * {@code docker/refharness/golden/v59.txt}, produced by
 * {@code sectionInventory} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>Storage home: the inventory lives on {@link ObjTypes.GameObject}
 * ({@code data.inventory}, v35/v33) and is reused as-is; the C
 * {@code length}/{@code capacity} + {@code capacity + 10} realloc growth
 * collapses into the list (growth is unobservable).</li>
 * <li>{@code itemGetName}/{@code itemGetDescription} are omitted: they need
 * the message stack (game phase), like the v53 {@code statsInit} rule.</li>
 * <li>{@code itemAdd} merge path: C++ destroys the absorbed stack object
 * ({@code objectDestroy}); the object-list removal comes with the
 * object/map ports, so the absorbed reference is simply dropped here.</li>
 * <li>{@code itemRemove} partial-drop path: C++ clones the remainder via
 * {@code _obj_copy}/{@code _obj_disconnect}; those come with the
 * object/map ports, so the remainder keeps the same reference here.
 * Vectors pin the full-drop + shift path only.</li>
 * <li>Script hooks ({@code scriptHooks_RemoveInventoryObject},
 * {@code stealthBoyTurnOn/Off}) need the script/skill engine (game phase);
 * the headless harness never arms them.</li>
 * </ul>
 */
public final class Item {

    private Item() {
    }

    public static final int ATTACK_TYPE_COUNT = 5;
    public static final int ATTACK_TYPE_FIRST = 0;

    public enum AttackType {
        ATTACK_TYPE_INVALID(-1),
        ATTACK_TYPE_NONE(0),
        ATTACK_TYPE_UNARMED(1),
        ATTACK_TYPE_MELEE(2),
        ATTACK_TYPE_THROW(3),
        ATTACK_TYPE_RANGED(4);

        private final int id;

        AttackType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, AttackType> ATTACK_BY_ID = new HashMap<>();

    static {
        for (AttackType e : AttackType.values()) {
            ATTACK_BY_ID.put(e.id, e);
        }
    }

    public static AttackType ofAttackType(int id) {
        return ATTACK_BY_ID.get(id);
    }

    public static final int HEALING_ITEM_COUNT = 3;
    public static final int HEALING_ITEM_FIRST = 0;

    public enum HealingItem {
        HEALING_ITEM_STIMPAK(0),
        HEALING_ITEM_SUPER_STIMPAK(1),
        HEALING_ITEM_HEALING_POWDER(2);

        private final int id;

        HealingItem(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public HealingItem next() {
            return Item.ofHealingItem(id + 1);
        }
    }

    private static final Map<Integer, HealingItem> HEALING_BY_ID = new HashMap<>();

    static {
        for (HealingItem e : HealingItem.values()) {
            HEALING_BY_ID.put(e.id, e);
        }
    }

    public static HealingItem ofHealingItem(int id) {
        return HEALING_BY_ID.get(id);
    }

    // 0x477AFC item_get_type.
    public static ItemType itemGetType(GameObject item) {
        ProtoId protoId = ProtoId.ofGameObject(item);
        if (protoId.objectType() != ObjTypes.ObjectType.OBJ_TYPE_ITEM) {
            return ItemType.ITEM_TYPE_MISC;
        }

        if (protoId.matchesItemType(ItemProtoTypeId.Shiv)) {
            return ItemType.ITEM_TYPE_MISC;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(protoId, slot);

        return slot[0].item.type;
    }

    // item_is_hidden (src/item.cc): proto extended HIDDEN flag, like C++.
    public static boolean itemIsHidden(GameObject item) {
        ProtoId protoId = ProtoId.ofGameObject(item);
        if (protoId.objectType() != ObjTypes.ObjectType.OBJ_TYPE_ITEM) {
            return false;
        }

        Proto[] slot = new Proto[1];
        if (ProtoDb.protoGetProto(protoId, slot) == -1) {
            return false;
        }

        return (slot[0].item.extendedFlags.bits() & ProtoExtendedFlags.HIDDEN.bits()) != 0;
    }

    // 0x47868? ammo_get_capacity.
    public static int ammoGetCapacity(GameObject ammoOrWeapon) {
        if (ammoOrWeapon == null) {
            return 0;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(ammoOrWeapon), slot);

        if (slot[0].item.type == ItemType.ITEM_TYPE_AMMO) {
            return slot[0].item.data.ammo.quantity;
        } else {
            return slot[0].item.data.weapon.ammoCapacity;
        }
    }

    // 0x4786A0 ammo_get_quantity.
    public static int ammoGetQuantity(GameObject ammoOrWeapon) {
        if (ammoOrWeapon == null) {
            return 0;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(ammoOrWeapon), slot);

        if (slot[0].item.type == ItemType.ITEM_TYPE_AMMO) {
            return ammoOrWeapon.data.item.ammo.quantity;
        } else {
            return ammoOrWeapon.data.item.weapon.ammoQuantity;
        }
    }

    // 0x478714 ammo_set_quantity (clamps to [0, capacity], like C++).
    public static void ammoSetQuantity(GameObject ammoOrWeapon, int quantity) {
        if (ammoOrWeapon == null) {
            return;
        }

        int capacity = ammoGetCapacity(ammoOrWeapon);
        if (quantity > capacity) {
            quantity = capacity;
        }

        if (quantity < 0) {
            quantity = 0;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(ammoOrWeapon), slot);

        if (slot[0].item.type == ItemType.ITEM_TYPE_AMMO) {
            ammoOrWeapon.data.item.ammo.quantity = quantity;
        } else {
            ammoOrWeapon.data.item.weapon.ammoQuantity = quantity;
        }
    }

    // 0x478DF8 weapon_get_ammo_type_pid.
    public static ProtoId weaponGetAmmoTypeProtoId(GameObject weapon) {
        if (weapon == null) {
            return ProtoId.empty();
        }

        if (itemGetType(weapon) != ItemType.ITEM_TYPE_WEAPON) {
            return ProtoId.empty();
        }

        return ProtoId.ofPid(weapon.data.item.weapon.ammoTypePid);
    }

    // 0x477B88 item_get_weight: null -> 0, proto weight, hidden -> 0,
    // power-armor halving, container recursion, weapon ammo weight.
    public static int itemGetWeight(GameObject item) {
        if (item == null) {
            return 0;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(item), slot);
        Proto proto = slot[0];

        int weight = proto.item.weight;

        if (itemIsHidden(item)) {
            weight = 0;
        }

        ItemType itemType = proto.item.type;
        if (itemType == ItemType.ITEM_TYPE_ARMOR) {
            ItemProtoTypeId id = ProtoTypes.ofItemProtoTypeId(proto.pid);
            if (id == ItemProtoTypeId.PowerArmor
                    || id == ItemProtoTypeId.HardenedPowerArmor
                    || id == ItemProtoTypeId.AdvancedPowerArmor
                    || id == ItemProtoTypeId.AdvancedPowerArmorMkII) {
                weight /= 2;
            }
        } else if (itemType == ItemType.ITEM_TYPE_CONTAINER) {
            weight += objectGetInventoryWeight(item);
        } else if (itemType == ItemType.ITEM_TYPE_WEAPON) {
            int ammoQuantity = ammoGetQuantity(item);
            if (ammoQuantity > 0) {
                ProtoId ammoTypeId = weaponGetAmmoTypeProtoId(item);
                if (ammoTypeId.valid()) {
                    Proto[] ammoSlot = new Proto[1];
                    // C++ skips the rc check (UB on miss); Java treats a miss
                    // as no ammo weight (vectors always load).
                    if (ProtoDb.protoGetProto(ammoTypeId, ammoSlot) != -1) {
                        weight += ammoSlot[0].item.weight
                                * ((ammoQuantity - 1) / ammoSlot[0].item.data.ammo.quantity + 1);
                    }
                }
            }
        }

        return weight;
    }

    // 0x477CAC item_get_cost.
    public static int itemGetCost(GameObject obj) {
        if (obj == null) {
            return 0;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(obj), slot);
        Proto proto = slot[0];

        int cost = proto.item.cost;

        switch (proto.item.type) {
        case ITEM_TYPE_CONTAINER:
            cost += objectGetCost(obj);
            break;
        case ITEM_TYPE_WEAPON: {
            int ammoQuantity = ammoGetQuantity(obj);
            if (ammoQuantity > 0) {
                ProtoId ammoTypeId = weaponGetAmmoTypeProtoId(obj);
                if (ammoTypeId.valid()) {
                    Proto[] ammoSlot = new Proto[1];
                    ProtoDb.protoGetProto(ammoTypeId, ammoSlot);
                    cost += ammoQuantity * ammoSlot[0].item.cost
                            / ammoSlot[0].item.data.ammo.quantity;
                }
            }
            break;
        }
        case ITEM_TYPE_AMMO:
            cost *= ammoGetQuantity(obj);
            cost /= ammoGetCapacity(obj);
            break;
        default:
            break;
        }

        return cost;
    }

    // 0x477DAC item_total_cost.
    public static int objectGetCost(GameObject obj) {
        if (obj == null) {
            return 0;
        }

        int cost = 0;

        List<InventoryItem> items = obj.data.inventory.items;
        for (int index = 0; index < items.size(); index++) {
            InventoryItem entry = items.get(index);
            if (itemGetType(entry.item) == ItemType.ITEM_TYPE_AMMO) {
                Proto[] slot = new Proto[1];
                ProtoDb.protoGetProto(ProtoId.ofGameObject(entry.item), slot);

                // Ammo stacks count in clips: all but the current one are
                // full, the current one is proportional (see itemGetCost).
                cost += slot[0].item.cost * (entry.quantity - 1);
                cost += itemGetCost(entry.item);
            } else {
                cost += itemGetCost(entry.item) * entry.quantity;
            }
        }

        if (isCritter(obj)) {
            GameObject item2 = Inventory.critterGetItem2(obj);
            if (item2 != null && (item2.flags.bits() & ObjectFlags.IN_RIGHT_HAND.bits()) == 0) {
                cost += itemGetCost(item2);
            }

            GameObject item1 = Inventory.critterGetItem1(obj);
            if (item1 != null && (item1.flags.bits() & ObjectFlags.IN_LEFT_HAND.bits()) == 0) {
                cost += itemGetCost(item1);
            }

            GameObject armor = Inventory.critterGetArmor(obj);
            if (armor != null && (armor.flags.bits() & ObjectFlags.WORN.bits()) == 0) {
                cost += itemGetCost(armor);
            }
        }

        return cost;
    }

    // 0x477E98 object_get_inventory_weight: weight x quantity over the
    // stack list, plus unequipped hand/armor extras on critters.
    public static int objectGetInventoryWeight(GameObject obj) {
        if (obj == null) {
            return 0;
        }

        int weight = 0;

        List<InventoryItem> items = obj.data.inventory.items;
        for (int index = 0; index < items.size(); index++) {
            InventoryItem entry = items.get(index);
            weight += itemGetWeight(entry.item) * entry.quantity;
        }

        if (isCritter(obj)) {
            GameObject item2 = Inventory.critterGetItem2(obj);
            if (item2 != null && (item2.flags.bits() & ObjectFlags.IN_RIGHT_HAND.bits()) == 0) {
                weight += itemGetWeight(item2);
            }

            GameObject item1 = Inventory.critterGetItem1(obj);
            if (item1 != null && (item1.flags.bits() & ObjectFlags.IN_LEFT_HAND.bits()) == 0) {
                weight += itemGetWeight(item1);
            }

            GameObject armor = Inventory.critterGetArmor(obj);
            if (armor != null && (armor.flags.bits() & ObjectFlags.WORN.bits()) == 0) {
                weight += itemGetWeight(armor);
            }
        }

        return weight;
    }

    // 0x47808C item_get_quantity: pointer-equality scan (+ container
    // recursion with the sfall early-break fix).
    public static int itemGetQuantity(GameObject obj, GameObject item) {
        int quantity = 0;

        List<InventoryItem> items = obj.data.inventory.items;
        for (int index = 0; index < items.size(); index++) {
            InventoryItem entry = items.get(index);
            if (entry.item == item) {
                quantity = entry.quantity;
                break;
            } else {
                if (itemGetType(entry.item) == ItemType.ITEM_TYPE_CONTAINER) {
                    quantity = itemGetQuantity(entry.item, item);
                    if (quantity > 0) {
                        break;
                    }
                }
            }
        }

        return quantity;
    }

    // 0x4779F0 _item_identical: the stacking rule. Same pointer stacks;
    // otherwise pid/sid/unique-id must match, neither side may be
    // equipped/queued, containers never stack, stocked items never stack,
    // empty weapons of one type always stack, and ammo/money compare with
    // the per-clip quantity masked out (C++ swaps it aside for the memcmp).
    private static boolean itemsIdentical(GameObject item1, GameObject item2) {
        if (item1 == item2) {
            return true;
        }

        if (item1.pid != item2.pid) {
            return false;
        }

        if (GameTime.scriptsIsUniqueObjectId(item1.id)
                || GameTime.scriptsIsUniqueObjectId(item2.id)) {
            return false;
        }

        if (item1.sid != item2.sid) {
            return false;
        }

        int equippedOrQueued = ObjectFlags.EQUIPPED.bits() | ObjectFlags.QUEUED.bits();
        if ((item1.flags.bits() & equippedOrQueued) != 0) {
            return false;
        }

        if ((item2.flags.bits() & equippedOrQueued) != 0) {
            return false;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(item1), slot);
        Proto proto = slot[0];

        if (proto.item.type == ItemType.ITEM_TYPE_CONTAINER) {
            return false;
        }

        if (!item1.data.inventory.items.isEmpty() || !item2.data.inventory.items.isEmpty()) {
            return false;
        }

        boolean sameFlags = item1.data.flags == item2.data.flags;

        if (sameFlags && proto.item.type == ItemType.ITEM_TYPE_WEAPON
                && item1.data.item.weapon.ammoQuantity < 1
                && item2.data.item.weapon.ammoQuantity < 1) {
            return true;
        }

        boolean moneyLike = proto.item.type == ItemType.ITEM_TYPE_AMMO
                || ProtoId.ofGameObject(item1).matchesItemType(ItemProtoTypeId.Money);

        int savedQuantity = 0;
        if (moneyLike) {
            savedQuantity = item2.data.item.ammo.quantity;
            item2.data.item.ammo.quantity = item1.data.item.ammo.quantity;
        }

        // Field-wise port of the C++ memcmp over ItemObjectData (Java has no
        // padding, so the field list is the exact equivalent).
        boolean same = sameFlags
                && item1.data.item.weapon.ammoQuantity == item2.data.item.weapon.ammoQuantity
                && item1.data.item.weapon.ammoTypePid == item2.data.item.weapon.ammoTypePid
                && item1.data.item.ammo.quantity == item2.data.item.ammo.quantity
                && item1.data.item.misc.charges == item2.data.item.misc.charges
                && item1.data.item.key.keyCode == item2.data.item.key.keyCode;

        if (moneyLike) {
            item2.data.item.ammo.quantity = savedQuantity;
        }

        return same;
    }

    // 0x4772B8 item_add_force: quantity < 1 -> -1; identical stacks merge
    // (ammo clips have their own merge math); new stacks store item +
    // quantity and take ownership; returns 0.
    public static int itemAdd(GameObject owner, GameObject itemToAdd, int quantity) {
        if (quantity < 1) {
            return -1;
        }

        List<InventoryItem> items = owner.data.inventory.items;

        int index = 0;
        while (index < items.size() && !itemsIdentical(items.get(index).item, itemToAdd)) {
            index++;
        }

        if (index == items.size()) {
            // Fresh stack: the C capacity + 10 realloc growth is
            // unobservable behind the list.
            InventoryItem entry = new InventoryItem();
            entry.item = itemToAdd;
            entry.quantity = quantity;

            // ActivatedStealthBoy + IN_ANY_HAND arms stealthBoyTurnOn(owner):
            // skill/light engine (game phase); the stack stores like C++.
            items.add(entry);
            itemToAdd.owner = owner;

            return 0;
        }

        if (itemToAdd == items.get(index).item) {
            // C++ debugPrint warning ("same item twice"); Java ports stay
            // silent (see Reaction).
            return 0;
        }

        if (itemGetType(itemToAdd) == ItemType.ITEM_TYPE_AMMO) {
            int ammoQuantityToAdd = ammoGetQuantity(itemToAdd);
            int ammoQuantity = ammoGetQuantity(items.get(index).item);
            int capacity = ammoGetCapacity(itemToAdd);

            ammoQuantity += ammoQuantityToAdd;
            if (ammoQuantity > capacity) {
                ammoSetQuantity(itemToAdd, ammoQuantity - capacity);
                items.get(index).quantity++;
            } else {
                ammoSetQuantity(itemToAdd, ammoQuantity);
            }

            items.get(index).quantity += quantity - 1;
        } else {
            items.get(index).quantity += quantity;
        }

        // C++ objectDestroy(old stack object): the object-list removal comes
        // with the object/map ports, so the absorbed reference is dropped.
        items.get(index).item = itemToAdd;
        itemToAdd.owner = owner;

        return 0;
    }

    // 0x477490 item_remove.
    public static int itemRemove(GameObject owner, GameObject itemToRemove, int quantity) {
        return itemRemoveInternal(owner, itemToRemove, quantity);
    }

    // item_remove_internal: pointer scan (+ sub-container recursion);
    // miss or short stack -> -1; full drop compacts (shifts the tail);
    // partial drop decrements. Hook + stealth-boy engine tails deferred
    // (game phase); the harness never arms them.
    private static int itemRemoveInternal(GameObject owner, GameObject itemToRemove, int quantity) {
        List<InventoryItem> items = owner.data.inventory.items;

        int index = 0;
        for (; index < items.size(); index++) {
            InventoryItem entry = items.get(index);
            if (entry.item == itemToRemove) {
                break;
            }

            if (itemGetType(entry.item) == ItemType.ITEM_TYPE_CONTAINER) {
                if (itemRemoveInternal(entry.item, itemToRemove, quantity) == 0) {
                    return 0;
                }
            }
        }

        if (index == items.size()) {
            return -1;
        }

        InventoryItem entry = items.get(index);
        if (entry.quantity < quantity) {
            return -1;
        }

        // scriptHooks_RemoveInventoryObject(owner, ...): script engine.

        if (entry.quantity == quantity) {
            // _item_compact: shift the tail down.
            items.remove(index);
        } else {
            // C++ _obj_copy + _obj_disconnect clone the remainder into a
            // fresh object; those come with the object/map ports, so the
            // remainder keeps the same reference here (vectors pin the
            // full-drop path only).
            entry.quantity -= quantity;

            if (itemGetType(itemToRemove) == ItemType.ITEM_TYPE_AMMO) {
                ammoSetQuantity(entry.item, ammoGetCapacity(itemToRemove));
            }
        }

        ProtoId removedId = ProtoId.ofGameObject(itemToRemove);
        if (removedId.matchesItemType(ItemProtoTypeId.StealthBoy)
                || removedId.matchesItemType(ItemProtoTypeId.ActivatedStealthBoy)) {
            GameObject item1 = Inventory.critterGetItem1(owner);
            GameObject item2 = Inventory.critterGetItem2(owner);
            if (itemToRemove == item1 || itemToRemove == item2) {
                // stealthBoyTurnOff(owner, itemToRemove): skill engine.
            }
        }

        itemToRemove.owner = null;
        itemToRemove.flags = itemToRemove.flags.and(ObjectFlags.EQUIPPED.not());

        return 0;
    }

    private static boolean isCritter(GameObject obj) {
        return FrmId.ofFid(obj.fid).objectType() == ObjectType.CRITTER;
    }
}
