package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Perk property ids, mirroring the pure parts of {@code src/perk.h}.
 * The perk engine ({@code perk.cc}) comes with the game phase.
 */
public final class Perk {

    private Perk() {
    }

    public enum PerkProperty {
        FrmId(0),
        MaxRank(1),
        MinLevel(2),
        Stat(3),
        StatModifier(4),
        Param1(5),
        Value1(6),
        ParamMode(7),
        Param2(8),
        Value2(9),
        Strength(10),
        Perception(11),
        Endurance(12),
        Charisma(13),
        Intelligence(14),
        Agility(15),
        Luck(16);

        private final int id;

        PerkProperty(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, PerkProperty> PROPERTY_BY_ID = new HashMap<>();

    static {
        for (PerkProperty e : PerkProperty.values()) {
            PROPERTY_BY_ID.put(e.id, e);
        }
    }

    public static PerkProperty ofPerkProperty(int id) {
        return PROPERTY_BY_ID.get(id);
    }
}
