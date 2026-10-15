package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Inventory id tables, mirroring the pure parts of {@code src/inventory.h}.
 * The inventory UI ({@code inventory.cc}) comes with the game phase.
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
}
