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
 * {@code itemAttemptAdd} (quantity/container/misc/critter gates),
 * {@code itemGetSize}, {@code containerGetMaxSize},
 * {@code containerGetTotalSize}, {@code miscItemGetPowerTypeProtoId},
 * {@code itemRemove} (+ {@code itemRemoveWithReason}/{@code itemRemoveQuietly}
 * via {@code RemoveInventoryObjectHookReason}), {@code itemMove}/
 * {@code itemMoveForce} ({@code _item_move_func}), {@code itemMoveAll} (+
 * {@code itemMoveAllHidden}/{@code itemDestroyAllHidden}) and the small
 * helpers they need ({@code itemIsHidden},
 * ammo quantity/capacity, weapon ammo-type id, {@code _item_identical}),
 * plus the weapon/armor mapping slice backing {@code inven_wield}:
 * {@code weaponGetSkillForHitMode}, {@code weaponGetAttackTypeForHitMode},
 * {@code weaponGetAnimationForHitMode}, {@code weaponGetAnimationCode},
 * {@code weaponGetDamageType}, the {@code armorGet*} getters and
 * {@code adjustCritterStatsOnArmorChange} (ported here from
 * {@code src/inventory.cc}, next to its only callers), plus the
 * weapon-ballistics slice: {@code weaponGetDamageMinMax},
 * {@code weaponGetRange}, {@code weaponGetActionPointCost} (with the
 * primary/secondary cost readers), {@code weaponIsGrenade} and
 * {@code weaponGetDamageRadius} (with the grenade/rocket explosion-radius
 * readers).
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 *
 * <p>Vectors: {@code [inventory]} in
 * {@code docker/refharness/golden/v59.txt} (produced by
 * {@code sectionInventory} in {@code docker/refharness/ref_main.cc}),
 * {@code [equip]} in {@code docker/refharness/golden/v60.txt} (produced by
 * {@code sectionEquip}; see {@link Inventory#inventoryEquipFunc}) and
 * {@code [ballistics]} in {@code docker/refharness/golden/v61.txt}
 * (produced by {@code sectionBallistics}; bare-hands PUNCH probes plus the
 * scorcher damage/radius and jacket armor probes) and {@code [itemmove]} in
 * {@code docker/refharness/golden/v62.txt} (produced by
 * {@code sectionItemMove}: blocked non-force move, force move + owner pin,
 * move-all, remove + destroy + null-destroy) and {@code [itemdrop]} in
 * {@code docker/refharness/golden/v63.txt} (produced by
 * {@code sectionItemDrop}: full drop at tile {@code 20100}, empty drop,
 * bad-tile drop with stack restore), plus the ammo/container/size slice:
 * {@code ammoGetCapacity}, {@code ammoGetQuantity}, {@code ammoSetQuantity}
 * (clamp to {@code [0, capacity]}), {@code itemGetSize} (pure proto read),
 * {@code containerGetMaxSize}, {@code containerGetTotalSize}.
 * {@code [ammocont]} vectors live in
 * {@code docker/refharness/golden/v64.txt} (produced by
 * {@code sectionAmmoCont} in {@code docker/refharness/ref_main.cc}).
 *
 * <p>NOTE (caliber scope): {@code ammoGetCaliber} ({@code src/item.cc}
 * {@code 0x4786C8}) is intentionally unported. It is not a one-liner pure
 * read: ammo protos read {@code data.ammo.caliber} directly, but weapons
 * hop through {@code data.item.weapon.ammoTypePid} into a second
 * {@code protoGetProto} with a {@code -1} miss check. It is not vectored
 * by the harness, so it stays out until a vector needs it.
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
 * <li>{@code scriptHooks_RemoveInventoryObject} fires
 * {@code HOOK_REMOVEINVENOBJ}; with no hooks registered the call is a
 * no-op, so headless it is a silent seam (like {@link Reaction}).
 * {@code stealthBoyTurnOn/Off} need the script/skill engine (game phase);
 * the headless harness never arms them.</li>
 * <li>{@code _item_move_func} re-add-failure tail ({@code _obj_connect} +
 * tile refresh) needs the map engine (game phase); harness objects sit at
 * tile {@code -1}, so the refresh is dormant.</li>
 * <li>{@code objectDestroy} on the {@code itemDestroyAllHidden} path is
 * {@link ObjectSystem#objectDestroy} (same slice, v62).</li>
 * <li>{@code itemDropAll} money branch: the whole stack removes at once
 * and the dropped object's {@code charges} field is restored to the
 * stack quantity after a successful connect, like C++.</li>
 * <li>{@code itemDropAll} equipped tail: the 5-arg
 * {@code scriptHooks_InvenWield} unwield seam always runs headless (no
 * hooks; C++ ignores the result), the armor frame/stat adjust is the
 * fully ported {@code adjustCritterStatsOnArmorChange}, and the tail
 * re-fids via {@link ObjectSystem#objectSetFrmId} with the
 * {@code ANIM_STAND} tile-refresh seam. The vectors carry no equipped
 * items, so the tail is skipped there.</li>
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

    /**
     * Port of {@code RemoveInventoryObjectHookReason}
     * ({@code src/sfall_script_hooks.h}): the {@code HOOK_REMOVEINVENOBJ}
     * reason tag threaded through {@code itemRemoveInternal}. Values are
     * the C++ hook ids (preserved).
     */
    public enum RemoveInventoryObjectHookReason {
        ITEM_REMOVED_INVENTORY(4831349),
        ITEM_REMOVED(4548572),
        ITEM_REMOVED_MULTI(4563866),
        ITEM_DESTROYED(4543215),
        ITEM_DESTROY_MULTI(4571599),
        ITEM_MOVE(4683293),
        ITEM_REPLACE(4686256),
        CONSUME_DRUG(4666772),
        USE_OBJ(4666865),
        EQUIP_ARMOR(4658121),
        EQUIP_WEAPON(4658675),
        UNLOAD_WEAPON(4667030),
        USE_DRUG_ON(4834866),
        STEAL_VIEW(4668206),
        ARMOR_EQUIPPED(4651961),
        LEFT_HAND_EQUIPPED(4651899),
        RIGHT_HAND_EQUIPPED(4651934),
        REPLACE_WEAPON(4658526),
        THROW(4266040),
        SUB_CONTAINER(4683191),
        AI_USE_DRUG_ON(4359920),
        BARTER_ARMOR(4675656),
        BARTER_WEAPON(4675722),
        INVENTORY_DROP_CAPS(4667295),
        DROP_INTO_CONTAINER(4678833);

        private final int id;

        RemoveInventoryObjectHookReason(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, RemoveInventoryObjectHookReason> REMOVE_REASON_BY_ID =
            new HashMap<>();

    static {
        for (RemoveInventoryObjectHookReason e : RemoveInventoryObjectHookReason.values()) {
            REMOVE_REASON_BY_ID.put(e.id, e);
        }
    }

    public static RemoveInventoryObjectHookReason ofRemoveInventoryObjectHookReason(int id) {
        return REMOVE_REASON_BY_ID.get(id);
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

    // 0x477158 item_attempt_add: quantity gate, then the owner-kind
    // gates, then itemAdd. Item owners check container capacity (+ the
    // carrier weight when the container itself is carried); misc owners
    // only accept their power-type pid; critter owners must be biped (or
    // carry the BARTER proto flag: -5 otherwise, including the proto-miss)
    // and must not exceed carry weight (-6). Non-item/non-critter owners
    // (scenery and below) skip straight to itemAdd, like C++.
    public static int itemAttemptAdd(GameObject owner, GameObject itemToAdd, int quantity) {
        if (quantity < 1) {
            return -1;
        }

        ObjectType parentType = FrmId.ofFid(owner.fid).objectType();
        if (parentType == ObjectType.ITEM) {
            ItemType itemType = itemGetType(owner);
            if (itemType == ItemType.ITEM_TYPE_CONTAINER) {
                int sizeToAdd = itemGetSize(itemToAdd) * quantity;

                int currentSize = containerGetTotalSize(owner);
                int maxSize = containerGetMaxSize(owner);
                if (currentSize + sizeToAdd > maxSize) {
                    return -6;
                }

                GameObject containerOwner = ObjectSystem.objectGetOwner(owner);
                if (containerOwner != null
                        && FrmId.ofFid(containerOwner.fid).objectType() == ObjectType.CRITTER) {
                    int weightToAdd = itemGetWeight(itemToAdd) * quantity;

                    int currentWeight = objectGetInventoryWeight(containerOwner);
                    int maxWeight = Stat.critterGetStat(containerOwner,
                            StatDefs.Stat.STAT_CARRY_WEIGHT.id());
                    if (currentWeight + weightToAdd > maxWeight) {
                        return -6;
                    }
                }
            } else if (itemType == ItemType.ITEM_TYPE_MISC) {
                ProtoId powerTypeProtoId = miscItemGetPowerTypeProtoId(owner);
                if (powerTypeProtoId.pid() != itemToAdd.pid) {
                    return -1;
                }
            } else {
                return -1;
            }
        } else if (parentType == ObjectType.CRITTER) {
            if (critterGetBodyType(owner) != ProtoTypes.BodyType.BODY_TYPE_BIPED) {
                // SFALL: non-biped critters with the Barter flag (Skynet,
                // Goris) can still be planted on.
                Proto[] slot = new Proto[1];
                if (ProtoDb.protoGetProto(ProtoId.ofGameObject(owner), slot) == -1) {
                    return -5;
                }

                if ((slot[0].critter.data.flags.bits()
                        & ObjTypes.CritterFlags.BARTER.bits()) == 0) {
                    return -5;
                }
            }

            int weightToAdd = itemGetWeight(itemToAdd) * quantity;

            int currentWeight = objectGetInventoryWeight(owner);
            int maxWeight =
                    Stat.critterGetStat(owner, StatDefs.Stat.STAT_CARRY_WEIGHT.id());
            if (currentWeight + weightToAdd > maxWeight) {
                return -6;
            }
        }

        return itemAdd(owner, itemToAdd, quantity);
    }

    // item_get_size (src/item.cc): null -> 0 (C++ skips the proto-miss
    // check; vectors always load).
    public static int itemGetSize(GameObject item) {
        if (item == null) {
            return 0;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(item), slot);

        return slot[0].item.size;
    }

    // 0x479A00 container_get_max_size: null -> 0.
    public static int containerGetMaxSize(GameObject container) {
        if (container == null) {
            return 0;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(container), slot);

        return slot[0].item.data.container.maxSize;
    }

    // 0x479A20 container_get_total_size: null -> 0, else quantity x size
    // over the stack list.
    public static int containerGetTotalSize(GameObject container) {
        if (container == null) {
            return 0;
        }

        int totalSize = 0;

        List<InventoryItem> items = container.data.inventory.items;
        for (int index = 0; index < items.size(); index++) {
            InventoryItem entry = items.get(index);
            totalSize += entry.quantity * itemGetSize(entry.item);
        }

        return totalSize;
    }

    // 0x479454 misc_item_get_power_type_pid: null -> empty (C++ skips the
    // proto-miss check; vectors always load).
    public static ProtoId miscItemGetPowerTypeProtoId(GameObject miscItem) {
        if (miscItem == null) {
            return ProtoId.empty();
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(miscItem), slot);

        return ProtoId.ofPid(slot[0].item.data.misc.powerTypePid);
    }

    // critter_get_body_type (src/critter.cc 0x42E6AC-ish): null or
    // non-critter -> BIPED (C++ debugPrints on null; Java stays silent,
    // see Reaction). C++ skips the proto-miss check (vectors always load).
    private static ProtoTypes.BodyType critterGetBodyType(GameObject critter) {
        if (critter == null) {
            return ProtoTypes.BodyType.BODY_TYPE_BIPED;
        }

        ProtoId protoId = ProtoId.ofGameObject(critter);
        if (protoId.objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return ProtoTypes.BodyType.BODY_TYPE_BIPED;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(protoId, slot);

        return slot[0].critter.data.bodyType;
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
        return itemRemoveInternal(owner, itemToRemove, quantity,
                RemoveInventoryObjectHookReason.ITEM_REMOVED_INVENTORY, null, true);
    }

    // item_remove_with_reason (src/item.cc): reason + target ride into
    // the RemoveInventoryObject hook.
    public static int itemRemoveWithReason(GameObject owner, GameObject itemToRemove, int quantity,
            RemoveInventoryObjectHookReason reason, GameObject target) {
        return itemRemoveInternal(owner, itemToRemove, quantity, reason, target, true);
    }

    // item_remove_quietly: the hook is skipped.
    public static int itemRemoveQuietly(GameObject owner, GameObject itemToRemove, int quantity) {
        return itemRemoveInternal(owner, itemToRemove, quantity,
                RemoveInventoryObjectHookReason.ITEM_REMOVED_INVENTORY, null, false);
    }

    // item_remove_internal: pointer scan (+ sub-container recursion, which
    // re-tags the reason as SubContainer with a null target, keeping the
    // hook flag);
    // miss or short stack -> -1; full drop compacts (shifts the tail);
    // partial drop decrements. The hook is silent headless (no script
    // engine); stealth-boy + _obj_copy/_obj_disconnect tails deferred
    // (game phase); the harness never arms them.
    private static int itemRemoveInternal(GameObject owner, GameObject itemToRemove, int quantity,
            RemoveInventoryObjectHookReason reason, GameObject target, boolean runHook) {
        List<InventoryItem> items = owner.data.inventory.items;

        int index = 0;
        for (; index < items.size(); index++) {
            InventoryItem entry = items.get(index);
            if (entry.item == itemToRemove) {
                break;
            }

            if (itemGetType(entry.item) == ItemType.ITEM_TYPE_CONTAINER) {
                if (itemRemoveInternal(entry.item, itemToRemove, quantity,
                        RemoveInventoryObjectHookReason.SUB_CONTAINER, null, runHook) == 0) {
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

        if (runHook) {
            scriptHooksRemoveInventoryObject(owner, itemToRemove, quantity, reason, target);
        }

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
                GameObject ultimateOwner = ObjectSystem.objectGetOwner(itemToRemove);
                if (ultimateOwner != null) {
                    // stealthBoyTurnOff(ultimateOwner, itemToRemove):
                    // skill engine (game phase).
                }
            }
        }

        itemToRemove.owner = null;
        itemToRemove.flags = itemToRemove.flags.and(ObjectFlags.EQUIPPED.not());

        return 0;
    }

    // 0x477608 _item_move_func: remove-with-reason (ItemMove, target rides
    // along) FIRST; a -1 there returns -1 with the source untouched. Then
    // the force-gated add; on add failure the stack is re-added to the
    // source (re-add failure drops to the tile-connect tail) and -1 is
    // returned. Success pins item->owner to the target (itemAdd already
    // does; C++ writes it again).
    private static int itemMoveFunc(
            GameObject source, GameObject target, GameObject item, int quantity, boolean force) {
        if (itemRemoveWithReason(
                source, item, quantity, RemoveInventoryObjectHookReason.ITEM_MOVE, target)
                == -1) {
            return -1;
        }

        int rc;
        if (force) {
            rc = itemAdd(target, item, quantity);
        } else {
            rc = itemAttemptAdd(target, item, quantity);
        }

        if (rc != 0) {
            if (itemAdd(source, item, quantity) != 0) {
                GameObject owner = ObjectSystem.objectGetOwner(source);
                if (owner == null) {
                    owner = source;
                }

                if (owner.tile != -1) {
                    // _obj_connect(item, tile, elevation, rect) +
                    // tileWindowRefreshRect: map engine (game phase).
                    // Harness objects sit at tile -1, so this is dormant.
                }
            }
            return -1;
        }

        item.owner = target;

        return 0;
    }

    // 0x47769C item_move.
    public static int itemMove(GameObject from, GameObject to, GameObject item, int quantity) {
        return itemMoveFunc(from, to, item, quantity, false);
    }

    // 0x4776A4 item_move_force.
    public static int itemMoveForce(GameObject from, GameObject to, GameObject item, int quantity) {
        return itemMoveFunc(from, to, item, quantity, true);
    }

    // 0x4776AC item_move_all: force-move slot 0 until the source is empty.
    public static void itemMoveAll(GameObject from, GameObject to) {
        List<InventoryItem> items = from.data.inventory.items;
        while (!items.isEmpty()) {
            InventoryItem entry = items.get(0);
            itemMoveForce(from, to, entry.item, entry.quantity);
        }
    }

    // 0x4776E0 item_move_all_hidden: force-move hidden stacks only (the
    // moved stack shifts into the scan slot, so the index only advances
    // past non-hidden stacks). Not vectored; trivially pure, ported whole.
    public static int itemMoveAllHidden(GameObject from, GameObject to) {
        List<InventoryItem> items = from.data.inventory.items;
        for (int index = 0; index < items.size();) {
            InventoryItem entry = items.get(index);
            if (itemIsHidden(entry.item)) {
                itemMoveForce(from, to, entry.item, entry.quantity);
            } else {
                index++;
            }
        }
        return 0;
    }

    // 0x477770 item_destroy_all_hidden: remove one + destroy every hidden
    // stack (same scan shape as move-all-hidden). Not vectored; trivially
    // pure, ported whole (destroy is ObjectSystem#objectDestroy, v62).
    public static int itemDestroyAllHidden(GameObject owner) {
        List<InventoryItem> items = owner.data.inventory.items;
        for (int index = 0; index < items.size(); index++) {
            InventoryItem entry = items.get(index);
            GameObject item = entry.item;
            if (itemIsHidden(item)) {
                itemRemoveWithReason(owner, item, 1,
                        RemoveInventoryObjectHookReason.ITEM_DESTROYED, null);
                ObjectSystem.objectDestroy(item);
            } else {
                index++;
            }
        }
        return 0;
    }

    // 0x477804 item_drop_all: while the inventory is non-empty, drop the
    // head stack. Money stacks drop whole (remove-all + connect, then the
    // charges field is restored to the stack quantity); other stacks drop
    // one unit at a time (remove-one + connect per unit, with the
    // re-add-on-connect-failure tail). Equipped head items arm the
    // unwield tail first (InvenWield hook + armor frame/stat adjust);
    // after the loop the equipped tail re-fids the critter (with the
    // ANIM_STAND tile refresh). An empty inventory returns 0 without
    // touching anything.
    public static int itemDropAll(GameObject critter, int tile) {
        boolean hasEquippedItems = false;

        int frameId = FrmId.ofFid(critter.fid).frameId();

        List<InventoryItem> items = critter.data.inventory.items;
        while (!items.isEmpty()) {
            InventoryItem inventoryItem = items.get(0);
            GameObject item = inventoryItem.item;
            if (ProtoId.ofGameObject(item).matchesItemType(ItemProtoTypeId.Money)) {
                int quantity = inventoryItem.quantity;
                if (itemRemove(critter, item, quantity) != 0) {
                    return -1;
                }

                if (ObjectSystem.objectConnect(item, tile, critter.elevation) != 0) {
                    if (itemAdd(critter, item, 1) != 0) {
                        ObjectSystem.objectDestroy(item);
                    }
                    return -1;
                }

                item.data.item.misc.charges = quantity;
            } else {
                if ((item.flags.bits() & ObjectFlags.EQUIPPED.bits()) != 0) {
                    hasEquippedItems = true;
                    Inventory.InvenSlot invenSlot = Inventory.InvenSlot.RIGHT_HAND;

                    if ((item.flags.bits() & ObjectFlags.WORN.bits()) != 0) {
                        invenSlot = Inventory.InvenSlot.ARMOR;
                    } else if ((item.flags.bits() & ObjectFlags.IN_LEFT_HAND.bits()) != 0) {
                        invenSlot = Inventory.InvenSlot.LEFT_HAND;
                    }

                    // C++ ignores the hook result here (unwield always
                    // proceeds); headless there are no hooks anyway.
                    scriptHooksInvenWield(critter, item, invenSlot, 0, 1);

                    if ((item.flags.bits() & ObjectFlags.WORN.bits()) != 0) {
                        Proto[] proto = new Proto[1];
                        if (ProtoDb.protoGetProto(ProtoId.ofGameObject(critter), proto) == -1) {
                            return -1;
                        }

                        frameId = FrmId.ofFid(proto[0].fid).frameId();
                        adjustCritterStatsOnArmorChange(critter, item, null);
                    }
                }

                // The head entry shifts under itemRemove (full drops
                // compact to the next stack), so the item is re-read per
                // unit, exactly like the C++ pointer.
                int quantity = items.get(0).quantity;
                for (int it = 0; it < quantity; it++) {
                    item = items.get(0).item;
                    if (itemRemove(critter, item, 1) != 0) {
                        return -1;
                    }

                    if (ObjectSystem.objectConnect(item, tile, critter.elevation) != 0) {
                        if (itemAdd(critter, item, 1) != 0) {
                            ObjectSystem.objectDestroy(item);
                        }
                        return -1;
                    }
                }
            }
        }

        if (hasEquippedItems) {
            FrmId current = FrmId.ofFid(critter.fid);
            FrmId critterFrmId = FrmId.ofCritter(frameId, current.animationType(),
                    WeaponAnimation.NONE.value(), current.rotation());
            ObjectSystem.objectSetFrmId(critter, critterFrmId);
            if (FrmId.ofFid(critter.fid).animationType() == AnimationType.STAND.value()) {
                tileWindowRefreshRect();
            }
        }

        return 0;
    }

    // scriptHooks_RemoveInventoryObject (src/sfall_script_hooks.cc): fires
    // HOOK_REMOVEINVENOBJ; with no hooks registered the call is a no-op.
    // The script engine is game phase; headless this is silent (see
    // Reaction).
    private static void scriptHooksRemoveInventoryObject(GameObject owner, GameObject item,
            int quantity, RemoveInventoryObjectHookReason reason, GameObject target) {
    }

    // scriptHooks_InvenWield (src/sfall_script_hooks.cc, 5-arg unwield
    // form): with no hooks registered the hook call has no return values
    // and C proceeds. The script engine is game phase; headless this is
    // always true (the itemDropAll unwield call ignores the result
    // anyway, like C++).
    private static boolean scriptHooksInvenWield(GameObject critter, GameObject item,
            Inventory.InvenSlot slot, int isWield, int isRemove) {
        return true;
    }

    // tileWindowRefreshRect(rect, gElevation) (tile render; game phase):
    // only reached from the itemDropAll equipped tail when the
    // re-fidded critter stands. gElevation is map engine state (game
    // phase); headless the refresh is a no-op (like Inventory's seam).
    private static void tileWindowRefreshRect() {
    }

    // Weapon extended-flags -> skill table (src/item.cc 0x519160
    // attack_skill). Entries are SkillDefs ids (SKILL_INVALID -1,
    // SKILL_UNARMED 3, SKILL_MELEE_WEAPONS 4, SKILL_THROWING 5,
    // SKILL_SMALL_GUNS 0).
    private static final int[] ATTACK_SKILL = { -1, 3, 3, 4, 4, 5, 0, 0, 0 };

    // Extended-flags -> animation table (0x519184 attack_anim). Entries
    // are AnimationType values.
    private static final int[] ATTACK_ANIM = { 0, 16, 17, 42, 41, 18, 45, 46, 47 };

    // Extended-flags -> weapon class table (0x5191A8 attack_subtype).
    // Entries are AttackType ids (NONE 0, UNARMED 1, MELEE 2, THROW 3,
    // RANGED 4).
    private static final int[] ATTACK_SUBTYPE = { 0, 1, 1, 2, 2, 3, 4, 4, 4 };

    // 0x478280 weapon_get_attack_type_for_hit_mode. Returns the AttackType
    // id; null weapon -> UNARMED, like C++. The primary/secondary nibble
    // pick mirrors C++; weapon protos only use indices 0-8.
    public static int weaponGetAttackTypeForHitMode(GameObject weapon, int hitMode) {
        if (weapon == null) {
            return AttackType.ATTACK_TYPE_UNARMED.id();
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot);

        int index;
        if (hitMode == CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_PRIMARY.id()
                || hitMode == CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()) {
            index = slot[0].item.extendedFlags.bits() & 0xF;
        } else {
            index = (slot[0].item.extendedFlags.bits() & 0xF0) >> 4;
        }

        return ATTACK_SUBTYPE[index];
    }

    // 0x4782CC weapon_get_skill_for_hit_mode. Returns the SkillDefs skill
    // id; null weapon -> SKILL_UNARMED. A SMALL_GUNS base becomes
    // ENERGY_WEAPONS for laser/plasma/electrical damage, else BIG_GUNS
    // when the proto carries PROTO_EXT_FLAG_BIG_GUN.
    public static int weaponGetSkillForHitMode(GameObject weapon, int hitMode) {
        if (weapon == null) {
            return SkillDefs.Skill.SKILL_UNARMED.id();
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot);

        int index;
        if (hitMode == CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_PRIMARY.id()
                || hitMode == CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()) {
            index = slot[0].item.extendedFlags.bits() & 0xF;
        } else {
            index = (slot[0].item.extendedFlags.bits() & 0xF0) >> 4;
        }

        int skill = ATTACK_SKILL[index];

        if (skill == SkillDefs.Skill.SKILL_SMALL_GUNS.id()) {
            ProtoTypes.DamageType damageType = weaponGetDamageType(null, weapon);
            if (damageType == ProtoTypes.DamageType.DAMAGE_TYPE_LASER
                    || damageType == ProtoTypes.DamageType.DAMAGE_TYPE_PLASMA
                    || damageType == ProtoTypes.DamageType.DAMAGE_TYPE_ELECTRICAL) {
                skill = SkillDefs.Skill.SKILL_ENERGY_WEAPONS.id();
            } else {
                if ((slot[0].item.extendedFlags.bits()
                        & ProtoTypes.ProtoExtendedFlags.BIG_GUN.bits()) != 0) {
                    skill = SkillDefs.Skill.SKILL_BIG_GUNS.id();
                }
            }
        }

        return skill;
    }

    // 0x478570 weapon_get_damage_type.
    public static ProtoTypes.DamageType weaponGetDamageType(GameObject critter, GameObject weapon) {
        if (weapon != null) {
            Proto[] slot = new Proto[1];
            ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot);

            return slot[0].item.data.weapon.damageType;
        }

        if (critter != null) {
            return critterGetDamageType(critter);
        }

        return ProtoTypes.DamageType.DAMAGE_TYPE_NORMAL;
    }

    // critter_get_damage_type (src/critter.cc): non-critter or proto miss
    // -> NORMAL, else the critter proto damage type.
    private static ProtoTypes.DamageType critterGetDamageType(GameObject critter) {
        if (FrmId.ofFid(critter.fid).objectType() != ObjectType.CRITTER) {
            return ProtoTypes.DamageType.DAMAGE_TYPE_NORMAL;
        }

        Proto[] slot = new Proto[1];
        if (ProtoDb.protoGetProto(ProtoId.ofGameObject(critter), slot) == -1) {
            return ProtoTypes.DamageType.DAMAGE_TYPE_NORMAL;
        }

        return slot[0].critter.data.damageType;
    }

    // 0x47860C weapon_get_animation_for_hit_mode. Returns the AnimationType
    // value id; kicks (including advanced) -> KICK_LEG, null weapon ->
    // THROW_PUNCH.
    public static int weaponGetAnimationForHitMode(GameObject weapon, int hitMode) {
        if (hitMode == CombatDefs.HitMode.HIT_MODE_KICK.id()
                || (hitMode >= CombatDefs.FIRST_ADVANCED_KICK_HIT_MODE
                        && hitMode <= CombatDefs.LAST_ADVANCED_KICK_HIT_MODE)) {
            return AnimationType.KICK_LEG.value();
        }

        if (weapon == null) {
            return AnimationType.THROW_PUNCH.value();
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot);

        int index;
        if (hitMode == CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_PRIMARY.id()
                || hitMode == CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()) {
            index = slot[0].item.extendedFlags.bits() & 0xF;
        } else {
            index = (slot[0].item.extendedFlags.bits() & 0xF0) >> 4;
        }

        return ATTACK_ANIM[index];
    }

    // 0x478DA8 weapon_get_animation_code. Returns the WeaponAnimation id
    // (ArtDefs ids match WeaponAnimation values); null weapon -> None.
    public static int weaponGetAnimationCode(GameObject weapon) {
        if (weapon == null) {
            return ArtDefs.WeaponAnimation.None.id();
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot);

        ArtDefs.WeaponAnimation code = slot[0].item.data.weapon.animationCode;
        return code == null ? ArtDefs.WeaponAnimation.None.id() : code.id();
    }

    // 0x4783EC weapon_get_damage_min_max: null weapon -> -1, else the
    // proto min/max damage. A null out-param is skipped, like the C++
    // nullptr checks (out-params use the single-element-array convention,
    // see ProtoDb#protoGetProto).
    public static int weaponGetDamageMinMax(GameObject weapon, int[] minDamage, int[] maxDamage) {
        if (weapon == null) {
            return -1;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot);

        if (minDamage != null && minDamage.length > 0) {
            minDamage[0] = slot[0].item.data.weapon.minDamage;
        }

        if (maxDamage != null && maxDamage.length > 0) {
            maxDamage[0] = slot[0].item.data.weapon.maxDamage;
        }

        return 0;
    }

    // critter_get_weapon_for_hit_mode (src/item.cc): left-hand modes read
    // item1, right-hand modes item2, every unarmed mode (basic PUNCH/KICK
    // plus advanced) -> null.
    private static GameObject critterGetWeaponForHitMode(GameObject critter, int hitMode) {
        if (hitMode == CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_PRIMARY.id()
                || hitMode == CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_SECONDARY.id()
                || hitMode == CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_RELOAD.id()) {
            return Inventory.critterGetItem1(critter);
        }

        if (hitMode == CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()
                || hitMode == CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id()
                || hitMode == CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_RELOAD.id()) {
            return Inventory.critterGetItem2(critter);
        }

        return null;
    }

    // 0x478AAC weapon_get_range: armed non-throw modes read the proto
    // maxRange1/2 nibble; THROW modes clamp to 3x effective Strength (the
    // dude/party Heave-Ho sfall bonus is dormant headless: gDude is null
    // and no critter is a party member, so perkGetRank never runs);
    // unarmed modes (PUNCH/KICK plus advanced 8-19, or a null weapon) fall
    // to the LONG_LIMBS tail (2 vs 1).
    public static int weaponGetRange(GameObject critter, int hitMode) {
        GameObject weapon = critterGetWeaponForHitMode(critter, hitMode);

        if (weapon != null
                && hitMode != CombatDefs.HitMode.HIT_MODE_PUNCH.id()
                && hitMode != CombatDefs.HitMode.HIT_MODE_KICK.id()
                && (hitMode < CombatDefs.FIRST_ADVANCED_PUNCH_HIT_MODE
                        || hitMode > CombatDefs.LAST_ADVANCED_KICK_HIT_MODE)) {
            Proto[] slot = new Proto[1];
            ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot);

            int range;
            if (hitMode == CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_PRIMARY.id()
                    || hitMode == CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()) {
                range = slot[0].item.data.weapon.maxRange1;
            } else {
                range = slot[0].item.data.weapon.maxRange2;
            }

            if (weaponGetAttackTypeForHitMode(weapon, hitMode)
                    == AttackType.ATTACK_TYPE_THROW.id()) {
                int effectiveStrength =
                        Stat.critterGetStat(critter, StatDefs.Stat.STAT_STRENGTH.id());
                if (isDude(critter) || objectIsPartyMember(critter)) {
                    // SFALL: Heave Ho! bonus, capped at PRIMARY_STAT_MAX.
                    if (effectiveStrength < StatDefs.PRIMARY_STAT_MAX) {
                        effectiveStrength += 2
                                * perkGetRank(critter, PerkDefs.Perk.PERK_HEAVE_HO);
                        if (effectiveStrength > StatDefs.PRIMARY_STAT_MAX) {
                            effectiveStrength = StatDefs.PRIMARY_STAT_MAX;
                        }
                    }
                }

                int maxRange = 3 * effectiveStrength;
                if (range >= maxRange) {
                    range = maxRange;
                }
            }

            return range;
        }

        if (critterFlagCheck(ProtoId.ofGameObject(critter), ObjTypes.CritterFlags.LONG_LIMBS)) {
            return 2;
        }

        return 1;
    }

    // 0x478B24 weapon_get_action_point_cost: reload modes cost 2 (1 with
    // the fast-reload perk, 0 for the Solar Scorcher); unarmed modes read
    // the unarmed table below; other armed modes read the proto
    // primary/secondary cost with the dormant dude-only Fast Shot tail; a
    // null weapon off the reload path costs 3. The dude-only Bonus HtH /
    // Bonus Rate of Fire tails are dormant headless (gDude is null), then
    // the aim +1 and the <1 clamp, then the CalcApCost hook (no hooks
    // headless: identity).
    public static int weaponGetActionPointCost(GameObject critter, int hitMode, boolean aiming) {
        int actionPoints;

        GameObject weapon = critterGetWeaponForHitMode(critter, hitMode);

        if (hitMode == CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_RELOAD.id()
                || hitMode == CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_RELOAD.id()) {
            if (weapon != null) {
                Proto[] slot = new Proto[1];
                ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot);

                if (slot[0].item.data.weapon.perk == PerkDefs.Perk.PERK_WEAPON_FAST_RELOAD) {
                    actionPoints = 1;
                } else if (ProtoId.ofGameObject(weapon)
                        .matchesItemType(ItemProtoTypeId.SolarScorcher)) {
                    actionPoints = 0;
                } else {
                    actionPoints = 2;
                }
            } else {
                actionPoints = 2;
            }

            return scriptHooksCalcApCost(critter, hitMode, aiming, actionPoints, weapon);
        }

        // CE: the unarmed branch differs from vanilla Sfall; the harness
        // never runs combatInit, so the table below stays zeroed.
        if (isUnarmedHitMode(hitMode)) {
            actionPoints = unarmedGetActionPointCost(hitMode);
        } else {
            if (weapon != null) {
                if (hitMode == CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_PRIMARY.id()
                        || hitMode == CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()) {
                    actionPoints = weaponGetPrimaryActionPointCost(weapon);
                } else {
                    actionPoints = weaponGetSecondaryActionPointCost(weapon);
                }

                if (isDude(critter)) {
                    if (TraitDefs.traitIsSelectedAndActive(
                            TraitDefs.Trait.TRAIT_FAST_SHOT.id())) {
                        if (weaponGetRange(critter, hitMode) > 2) {
                            actionPoints--;
                        }
                    }
                }
            } else {
                actionPoints = 3;
            }
        }

        if (isDude(critter)) {
            int attackType = weaponGetAttackTypeForHitMode(weapon, hitMode);

            if (perkHasRank(G_DUDE, PerkDefs.Perk.PERK_BONUS_HTH_ATTACKS)) {
                if (attackType == AttackType.ATTACK_TYPE_MELEE.id()
                        || attackType == AttackType.ATTACK_TYPE_UNARMED.id()) {
                    actionPoints -= 1;
                }
            }

            if (perkHasRank(G_DUDE, PerkDefs.Perk.PERK_BONUS_RATE_OF_FIRE)) {
                if (attackType == AttackType.ATTACK_TYPE_RANGED.id()) {
                    actionPoints -= 1;
                }
            }
        }

        if (aiming) {
            actionPoints += 1;
        }

        if (actionPoints < 1) {
            actionPoints = 1;
        }

        return scriptHooksCalcApCost(critter, hitMode, aiming, actionPoints, weapon);
    }

    // 0x47906C weapon_get_primary_action_point_cost (null -> -1).
    public static int weaponGetPrimaryActionPointCost(GameObject weapon) {
        if (weapon == null) {
            return -1;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot);

        return slot[0].item.data.weapon.actionPointCost1;
    }

    // 0x479084 weapon_get_secondary_action_point_cost (null -> -1).
    public static int weaponGetSecondaryActionPointCost(GameObject weapon) {
        if (weapon == null) {
            return -1;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(weapon), slot);

        return slot[0].item.data.weapon.actionPointCost2;
    }

    // weapon_is_grenade (src/item.cc): explosion/plasma/EMP damage.
    public static boolean weaponIsGrenade(GameObject weapon) {
        ProtoTypes.DamageType damageType = weaponGetDamageType(null, weapon);
        return damageType == ProtoTypes.DamageType.DAMAGE_TYPE_EXPLOSION
                || damageType == ProtoTypes.DamageType.DAMAGE_TYPE_PLASMA
                || damageType == ProtoTypes.DamageType.DAMAGE_TYPE_EMP;
    }

    // 0x47910C weapon_get_damage_radius: RANGED single-fire explosions read
    // the rocket radius, THROW grenades the grenade radius, else 0.
    public static int weaponGetDamageRadius(GameObject weapon, int hitMode) {
        int attackType = weaponGetAttackTypeForHitMode(weapon, hitMode);
        int anim = weaponGetAnimationForHitMode(weapon, hitMode);
        ProtoTypes.DamageType damageType = weaponGetDamageType(null, weapon);

        int radius = 0;
        if (attackType == AttackType.ATTACK_TYPE_RANGED.id()) {
            if (anim == AnimationType.FIRE_SINGLE.value()
                    && damageType == ProtoTypes.DamageType.DAMAGE_TYPE_EXPLOSION) {
                radius = weaponGetRocketExplosionRadius(weapon);
            }
        } else if (attackType == AttackType.ATTACK_TYPE_THROW.id()) {
            if (weaponIsGrenade(weapon)) {
                radius = weaponGetGrenadeExplosionRadius(weapon);
            }
        }

        return radius;
    }

    // 0x479180 weapon_get_grenade_explosion_radius: the sfall override wins
    // unless reset to -1; otherwise the post-explosionsReset default (2).
    public static int weaponGetGrenadeExplosionRadius(GameObject weapon) {
        if (gExplosionRadius != -1) {
            return gExplosionRadius;
        }

        return 2;
    }

    // 0x479188 weapon_get_rocket_explosion_radius (same shape, default 3).
    public static int weaponGetRocketExplosionRadius(GameObject weapon) {
        if (gExplosionRadius != -1) {
            return gExplosionRadius;
        }

        return 3;
    }

    // 0x4792F8 armor_get_armor_class (null -> 0).
    public static int armorGetArmorClass(GameObject armor) {        if (armor == null) {
            return 0;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(armor), slot);

        return slot[0].item.data.armor.armorClass;
    }

    // 0x479318 armor_get_damage_resistance (damageType is a DamageType id;
    // null armor -> 0).
    public static int armorGetDamageResistance(GameObject armor, int damageType) {
        if (armor == null) {
            return 0;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(armor), slot);

        return slot[0].item.data.armor.damageResistance[damageType];
    }

    // 0x479338 armor_get_damage_threshold (null -> 0).
    public static int armorGetDamageThreshold(GameObject armor, int damageType) {
        if (armor == null) {
            return 0;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(armor), slot);

        return slot[0].item.data.armor.damageThreshold[damageType];
    }

    // 0x479358 armor_get_perk (null -> PERK_INVALID).
    public static PerkDefs.Perk armorGetPerk(GameObject armor) {
        if (armor == null) {
            return PerkDefs.Perk.PERK_INVALID;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(armor), slot);

        return slot[0].item.data.armor.perk;
    }

    // 0x479380 armor_get_male_fid: the male fid's frame id
    // (CritterFrameId::Invalid is -1, preserved); null armor -> -1.
    public static int armorGetMaleFrameId(GameObject armor) {
        if (armor == null) {
            return ArtDefs.CritterFrameId.Invalid.id();
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(armor), slot);

        return FrmId.ofFid(slot[0].item.data.armor.maleFid).frameId();
    }

    // 0x4793A8 armor_get_female_fid (same shape as the male getter).
    public static int armorGetFemaleFrameId(GameObject armor) {
        if (armor == null) {
            return ArtDefs.CritterFrameId.Invalid.id();
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(armor), slot);

        return FrmId.ofFid(slot[0].item.data.armor.femaleFid).frameId();
    }

    // adjust_critter_stats_on_armor_change (src/inventory.cc 0x4715F8):
    // removes the [oldArmor] bonuses and adds the [newArmor] ones (either
    // may be null). The party-member perk tails need the perk engine (game
    // phase); the equip vectors never reach them (the harness dog is not a
    // party member: harness objects get ids < 18000, which C rejects up
    // front, and the headless roster is empty).
    public static void adjustCritterStatsOnArmorChange(
            GameObject critter, GameObject oldArmor, GameObject newArmor) {
        int armorClassBonus =
                Stat.critterGetBonusStat(critter, StatDefs.Stat.STAT_ARMOR_CLASS.id());
        int oldArmorClass = armorGetArmorClass(oldArmor);
        int newArmorClass = armorGetArmorClass(newArmor);
        Stat.critterSetBonusStat(critter, StatDefs.Stat.STAT_ARMOR_CLASS.id(),
                armorClassBonus - oldArmorClass + newArmorClass);

        StatDefs.Stat damageResistanceStat = StatDefs.Stat.STAT_DAMAGE_RESISTANCE;
        StatDefs.Stat damageThresholdStat = StatDefs.Stat.STAT_DAMAGE_THRESHOLD;
        for (int damageType = ProtoTypes.DAMAGE_TYPE_FIRST;
                damageType < ProtoTypes.DAMAGE_TYPE_COUNT;
                damageType++) {
            int damageResistanceBonus =
                    Stat.critterGetBonusStat(critter, damageResistanceStat.id());
            int oldArmorDamageResistance = armorGetDamageResistance(oldArmor, damageType);
            int newArmorDamageResistance = armorGetDamageResistance(newArmor, damageType);
            Stat.critterSetBonusStat(critter, damageResistanceStat.id(),
                    damageResistanceBonus - oldArmorDamageResistance + newArmorDamageResistance);

            int damageThresholdBonus =
                    Stat.critterGetBonusStat(critter, damageThresholdStat.id());
            int oldArmorDamageThreshold = armorGetDamageThreshold(oldArmor, damageType);
            int newArmorDamageThreshold = armorGetDamageThreshold(newArmor, damageType);
            Stat.critterSetBonusStat(critter, damageThresholdStat.id(),
                    damageThresholdBonus - oldArmorDamageThreshold + newArmorDamageThreshold);

            damageResistanceStat = damageResistanceStat.next();
            damageThresholdStat = damageThresholdStat.next();
        }

        if (objectIsPartyMember(critter)) {
            if (oldArmor != null) {
                perkRemoveEffect(critter, armorGetPerk(oldArmor));
            }

            if (newArmor != null) {
                perkAddEffect(critter, armorGetPerk(newArmor));
            }
        }
    }

    // Port of objectIsPartyMember (src/party_member.cc 0x494FC4): the
    // gPartyMembers roster scan needs the party/map engine (game phase);
    // headless the roster is empty, so this is false for every vector
    // object.
    private static boolean objectIsPartyMember(GameObject critter) {
        return false;
    }

    // perkAddEffect/perkRemoveEffect (src/perk.cc): perk engine (game
    // phase); unreachable headless (see adjustCritterStatsOnArmorChange).
    private static void perkAddEffect(GameObject critter, PerkDefs.Perk perk) {
    }

    private static void perkRemoveEffect(GameObject critter, PerkDefs.Perk perk) {
    }

    // gExplosionRadius (src/item.cc): the sfall explosion-radius override;
    // -1 (post-explosionSettingsReset, the game state) means "no override".
    // The harness never touches it (explosionsReset is game phase), so no
    // resetForTests entry is needed.
    private static int gExplosionRadius = -1;

    // is_unarmed_hit_mode (src/combat.h): PUNCH/KICK plus the advanced
    // unarmed range.
    private static boolean isUnarmedHitMode(int hitMode) {
        return hitMode == CombatDefs.HitMode.HIT_MODE_PUNCH.id()
                || hitMode == CombatDefs.HitMode.HIT_MODE_KICK.id()
                || (hitMode >= CombatDefs.FIRST_ADVANCED_UNARMED_HIT_MODE
                        && hitMode <= CombatDefs.LAST_ADVANCED_UNARMED_HIT_MODE);
    }

    // gUnarmedHitDescriptions action-point-cost column (src/combat.cc): BSS
    // mirror, like Combat#hitLocationPenalty. unarmedInit (vanilla +
    // custom) fills the table but runs only from combatInit, which touches
    // gDude and never runs headless; the vectors therefore observe the
    // zeroed table (bare PUNCH costs 0, clamped to 1 by the caller). Never
    // mutated headless, so no resetForTests entry is needed.
    private static final int[] UNARMED_ACTION_POINT_COST =
            new int[CombatDefs.HIT_MODE_COUNT];

    // unarmed_get_action_point_cost (src/combat.cc): raw table read.
    private static int unarmedGetActionPointCost(int hitMode) {
        return UNARMED_ACTION_POINT_COST[hitMode];
    }

    // critter_flag_check (src/critter.cc 0x42E6AC): proto critter flag read
    // (invalid/non-critter/proto miss -> false).
    private static boolean critterFlagCheck(ProtoId protoId, ObjTypes.CritterFlags flag) {
        if (!protoId.valid()) {
            return false;
        }

        if (protoId.objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return false;
        }

        Proto[] slot = new Proto[1];
        if (ProtoDb.protoGetProto(protoId, slot) == -1) {
            return false;
        }

        return (slot[0].critter.data.flags.bits() & flag.bits()) != 0;
    }

    // Port of gDude (src/object.cc, unported): null headless, so every
    // critter == gDude check reads false for non-null critters (a null
    // critter is never "the dude"; see Stat#isDude).
    private static final GameObject G_DUDE = null;

    private static boolean isDude(GameObject critter) {
        return critter != null && critter == G_DUDE;
    }

    // Seams for the perk engine (unported): only reachable from the dormant
    // dude/party branches (see Skill#perkGetRank).
    private static int perkGetRank(GameObject critter, PerkDefs.Perk perk) {
        return 0;
    }

    private static boolean perkHasRank(GameObject critter, PerkDefs.Perk perk) {
        return false;
    }

    // scriptHooks_CalcApCost (src/sfall_script_hooks.cc): with no hooks
    // registered the call has no return values and C returns the input.
    // The script engine is game phase; headless this is the identity.
    private static int scriptHooksCalcApCost(
            GameObject critter, int hitMode, boolean aiming, int actionPoints, GameObject weapon) {
        return actionPoints;
    }

    private static boolean isCritter(GameObject obj) {
        return FrmId.ofFid(obj.fid).objectType() == ObjectType.CRITTER;
    }
}
