package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.fallout2ce.Item.AttackType;
import org.fallout2ce.Item.HealingItem;

/**
 * {@code [gametypes]} item vectors from
 * {@code docker/refharness/golden/v35.txt}.
 */
class ItemTest {

    @Test
    void attackType() {
        assertEquals(-1, AttackType.ATTACK_TYPE_INVALID.id());
        assertEquals(0, Item.ATTACK_TYPE_FIRST);
        assertEquals(5, Item.ATTACK_TYPE_COUNT);
        assertEquals(AttackType.ATTACK_TYPE_RANGED, Item.ofAttackType(4));
        assertNull(Item.ofAttackType(5));
    }

    @Test
    void healingItem() {
        assertEquals(0, Item.HEALING_ITEM_FIRST);
        assertEquals(3, Item.HEALING_ITEM_COUNT);
        // post-old=2 post-new=3 (past the end).
        assertEquals(HealingItem.HEALING_ITEM_HEALING_POWDER, Item.ofHealingItem(2));
        assertNull(Item.ofHealingItem(2).next());
        assertEquals(HealingItem.HEALING_ITEM_HEALING_POWDER, Item.ofHealingItem(1).next());
    }
}
