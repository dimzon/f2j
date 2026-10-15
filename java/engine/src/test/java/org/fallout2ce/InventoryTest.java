package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.fallout2ce.Inventory.Hand;

/**
 * {@code [gametypes]} inventory vectors from
 * {@code docker/refharness/golden/v35.txt}.
 */
class InventoryTest {

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
}
