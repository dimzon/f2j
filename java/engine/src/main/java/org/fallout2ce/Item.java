package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Item id tables, mirroring the pure parts of {@code src/item.h}.
 * The item engine ({@code item.cc}) comes with the game phase.
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
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
}
