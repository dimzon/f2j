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
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Inventory id tables, mirroring the pure parts of {@code src/inventory.h},
 * plus the inventory lookup slice of {@code src/inventory.cc}:
 * {@code inventoryItemByIndex}, {@code inventoryFindByType} and the
 * equipped-item scans ({@code critterGetItem1}/{@code critterGetItem2}/
 * {@code critterGetArmor}) backing the {@link Item} weight/cost extras,
 * plus the {@code inven_wield} equip slice ({@link #inventoryEquipFunc}).
 *
 * <p>Vectors: {@code [inventory]} in
 * {@code docker/refharness/golden/v59.txt} (produced by
 * {@code sectionInventory} in {@code docker/refharness/ref_main.cc}) and
 * {@code [equip]} in {@code docker/refharness/golden/v60.txt} (produced by
 * {@code sectionEquip}).
 *
 * <p>NOTE: the {@code _inven_dude} hand-override globals
 * ({@code gInventoryLeftHandItem}/{@code gInventoryRightHandItem}/
 * {@code gInventoryArmor}) are inventory-UI state (game phase); the scans
 * below read the inventory flags directly, which matches C++ whenever the
 * UI overrides are null (always in the harness).
 *
 * <p>Game-phase seams (all documented at the use site): the
 * {@code scriptHooks_InvenWield} hook (always true headless: no hooks
 * registered), {@code isoIsDisabled} (always true headless: fresh-process
 * {@code gIsoEnabled} is false and the harness never enables iso), the
 * dude-only branches ({@code gDude}, interface hand, fid-set), the
 * animation/sound registrations (only reached when animating),
 * {@code _dude_stand} (rotation/fid/location/frame + tile refresh) and the
 * tile refresh rects. None is reachable from the vectors with
 * {@code animate=false} on a non-dude, except
 * {@link Item#adjustCritterStatsOnArmorChange}, which is fully ported.
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

    /**
     * Port of {@code InvenSlot} ({@code src/interpreter_extra.h}: Armor 0,
     * RightHand 1, LeftHand 2). Only used as the script-hook slot tag here;
     * the wield/unwield slot engine is game phase.
     */
    public enum InvenSlot {
        ARMOR(0),
        RIGHT_HAND(1),
        LEFT_HAND(2);

        private final int id;

        InvenSlot(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
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

    // inven_wield (src/inventory.cc 0x472758/0x472768 inventoryEquipFunc).
    // Armor path: swap the worn flag, pick the male/female frame id, then
    // the dude fid-set or (non-dude) adjustCritterStatsOnArmorChange.
    // Weapon/hand path: non-dudes wield with the right hand; the art gate
    // (FrmId(critter, anim, wanim, rot + 1).exist()) rejects missing art
    // with debugPrint + -1; then the hand flag is set, the previous hand
    // is cleared (+ flare-light restore), the LitFlare light branch runs
    // unvectored, the weapon animation is picked, and the hand ==
    // handIndex sound/anim tails run (animate=false takes the _dude_stand
    // path, seamed below), plus the final animate tail.
    //
    // Vectors ([equip] in golden/v60.txt) use animate=false on a non-dude
    // dog: the jacket equip succeeds via adjustCritterStatsOnArmorChange,
    // the scorcher wield fails at the art gate (dog has no gun frames).
    public static int inventoryEquipFunc(
            GameObject critter, GameObject item, Hand handIndex, boolean animate) {
        ItemType itemType = Item.itemGetType(item);

        InvenSlot invenSlot = itemType == ItemType.ITEM_TYPE_ARMOR
                ? InvenSlot.ARMOR
                : (handIndex == Hand.HAND_RIGHT ? InvenSlot.RIGHT_HAND : InvenSlot.LEFT_HAND);
        if (!scriptHooksInvenWield(critter, item, invenSlot)) {
            return -1;
        }

        if (animate) {
            if (!isoDisabled()) {
                regAnimBegin();
            }
        }

        if (itemType == ItemType.ITEM_TYPE_ARMOR) {
            GameObject armor = critterGetArmor(critter);
            if (armor != null) {
                armor.flags = armor.flags.and(ObjectFlags.WORN.not());
            }

            item.flags = item.flags.or(ObjectFlags.WORN);

            // C GENDER_FEMALE is 1 (src/proto_types.h); the Java Gender
            // enum keeps the same order (MALE 0, FEMALE 1).
            int baseFrameId;
            if (Stat.critterGetStat(critter, StatDefs.Stat.STAT_GENDER.id())
                    == Gender.FEMALE.ordinal()) {
                baseFrameId = Item.armorGetFemaleFrameId(item);
            } else {
                baseFrameId = Item.armorGetMaleFrameId(item);
            }

            if (baseFrameId == ArtDefs.CritterFrameId.Invalid.id()) {
                baseFrameId = ArtDefs.CritterFrameId.First.id();
            }

            if (isDude(critter)) {
                if (!isoDisabled()) {
                    // animationRegisterSetFrmId(critter,
                    // CritterFrmId(baseFrameId, STAND, weapon, rot + 1), 0):
                    // animation engine (game phase).
                }
            } else {
                Item.adjustCritterStatsOnArmorChange(critter, armor, item);
            }
        } else {
            Hand hand;
            if (isDude(critter)) {
                hand = interfaceCurrentHand();
            } else {
                hand = Hand.HAND_RIGHT;
            }

            int weaponAnimationCode = Item.weaponGetAnimationCode(item);
            int hitModeAnimationCode = Item.weaponGetAnimationForHitMode(
                    item, CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
            FrmId frmId = FrmId.ofObject(
                    FrmId.ofFid(critter.fid).objectType().value(),
                    FrmId.ofFid(critter.fid).frameId(),
                    hitModeAnimationCode,
                    weaponAnimationCode,
                    critter.rotation.id() + 1);
            if (!frmId.exist()) {
                // C++ debugPrint "\ninven_wield failed!  ERROR ERROR
                // ERROR!" to stderr (the INFO line in the golden file);
                // Java ports stay silent (see Reaction).
                return -1;
            }

            GameObject equippedItem;
            if (handIndex == Hand.HAND_RIGHT) {
                equippedItem = critterGetItem2(critter);
                item.flags = item.flags.or(ObjectFlags.IN_RIGHT_HAND);
            } else {
                equippedItem = critterGetItem1(critter);
                item.flags = item.flags.or(ObjectFlags.IN_LEFT_HAND);
            }

            if (equippedItem != null) {
                equippedItem.flags = equippedItem.flags.and(ObjectFlags.IN_ANY_HAND.not());

                if (ProtoId.ofGameObject(equippedItem)
                        .matchesItemType(ItemProtoTypeId.LitFlare)) {
                    critterRestoreLightWithoutFlare(critter);
                }
            }

            if (ProtoId.ofGameObject(item).matchesItemType(ItemProtoTypeId.LitFlare)) {
                int lightDistance = item.lightDistance;
                if (lightDistance < critter.lightDistance) {
                    lightDistance = critter.lightDistance;
                }

                int lightIntensity = item.lightIntensity;
                if (lightIntensity < critter.lightIntensity) {
                    lightIntensity = critter.lightIntensity;
                }

                // C++ also passes a rect for the tile refresh; the Java
                // objectSetLight slice takes none (see ObjectSystem), so
                // the refresh is a seam.
                ObjectSystem.objectSetLight(critter, lightDistance, lightIntensity);
                tileWindowRefreshRect();
            }

            int weaponAnimation;
            if (Item.itemGetType(item) == ItemType.ITEM_TYPE_WEAPON) {
                weaponAnimation = Item.weaponGetAnimationCode(item);
            } else {
                weaponAnimation = WeaponAnimation.NONE.value();
            }

            if (hand == handIndex) {
                if (FrmId.ofFid(critter.fid).weaponAnimation()
                        != WeaponAnimation.NONE.value()) {
                    if (animate) {
                        if (!isoDisabled()) {
                            // sfxBuildCharName + animationRegisterPlaySoundEffect
                            // + animationRegisterAnimate(PUT_AWAY): sound and
                            // animation engines (game phase).
                        }
                    }
                }

                if (animate && !isoDisabled()) {
                    if (weaponAnimation != WeaponAnimation.NONE.value()) {
                        // animationRegisterTakeOutWeapon: animation engine.
                    } else {
                        // animationRegisterSetFrmId(STAND/None): animation engine.
                    }
                } else {
                    dudeStand(critter, critter.rotation,
                            FrmId.ofObject(
                                    FrmId.ofFid(critter.fid).objectType().value(),
                                    FrmId.ofFid(critter.fid).frameId(),
                                    AnimationType.STAND.value(),
                                    weaponAnimation,
                                    critter.rotation.id() + 1));
                }
            }
        }

        if (animate) {
            if (!isoDisabled()) {
                return regAnimEnd();
            }
        }

        return 0;
    }

    // CE: restores a critter's own light unless a lit flare is still in
    // either hand (src/inventory.cc). The dude branch and the tile refresh
    // need the game/map engine (see seams); the vectors never reach this
    // (equippedItem is null on the art-gate failure path).
    private static void critterRestoreLightWithoutFlare(GameObject critter) {
        GameObject leftHandItem = critterGetItem1(critter);
        if (leftHandItem != null
                && ProtoId.ofGameObject(leftHandItem)
                        .matchesItemType(ItemProtoTypeId.LitFlare)) {
            return;
        }

        GameObject rightHandItem = critterGetItem2(critter);
        if (rightHandItem != null
                && ProtoId.ofGameObject(rightHandItem)
                        .matchesItemType(ItemProtoTypeId.LitFlare)) {
            return;
        }

        int lightDistance;
        int lightIntensity;
        if (isDude(critter)) {
            lightDistance = 4;
            lightIntensity = Light.LIGHT_INTENSITY_MAX;
        } else {
            Proto[] slot = new Proto[1];
            if (ProtoDb.protoGetProto(ProtoId.ofGameObject(critter), slot) == -1) {
                return;
            }

            lightDistance = slot[0].lightDistance;
            lightIntensity = slot[0].lightIntensity;
        }

        if (ObjectSystem.objectSetLight(critter, lightDistance, lightIntensity) == 0) {
            tileWindowRefreshRect();
        }
    }

    // Port of gDude (src/object.cc, unported): null headless, so every
    // critter == gDude check reads false for non-null critters (a null
    // critter is never "the dude"; see Stat#isDude).
    private static final GameObject G_DUDE = null;

    private static boolean isDude(GameObject critter) {
        return critter != null && critter == G_DUDE;
    }

    // scriptHooks_InvenWield (src/sfall_script_hooks.cc): with no hooks
    // registered the hook call has no return values and C returns true.
    // The script engine is game phase; headless this is always true.
    private static boolean scriptHooksInvenWield(
            GameObject critter, GameObject item, InvenSlot slot) {
        return true;
    }

    // isoIsDisabled (src/map.cc 0x482148): reads gIsoEnabled, which is
    // false in a fresh process (and the harness never enables iso), so
    // headless this is always true. Full port comes with the map/iso
    // engine; the equip vectors only reach iso-gated tails when animating.
    private static boolean isoDisabled() {
        return true;
    }

    // interfaceGetCurrentHand (src/interface.cc): dude-only interface
    // state (game phase); unreachable headless (no dude).
    private static Hand interfaceCurrentHand() {
        return Hand.HAND_RIGHT;
    }

    // _dude_stand (src/animation.cc): sets rotation/fid/location/frame and
    // refreshes tiles (animation/render engine, game phase). Unreachable
    // in the vectors (the armor path on a non-dude takes adjustStats; the
    // weapon path fails at the art gate first).
    private static void dudeStand(GameObject obj, ObjTypes.Rotation rotation, FrmId frmId) {
    }

    // reg_anim_begin/reg_anim_end plus the animationRegister*/sfx tails
    // (animation and sound engines, game phase); only reached when
    // animating.
    private static void regAnimBegin() {
    }

    private static int regAnimEnd() {
        return 0;
    }

    // tileWindowRefreshRect (tile render; game phase).
    private static void tileWindowRefreshRect() {
    }
}
