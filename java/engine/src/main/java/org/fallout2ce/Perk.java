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

    /** Port of {@code PERK_COUNT} / {@code PERK_FIRST} (src/perk_defs.h). */
    public static final int PERK_COUNT = 120;
    public static final int PERK_FIRST = 0;
    public static final int PERK_INVALID = -1;

    /** PERK_BONUS_MOVE = 4. */
    public static final int PERK_BONUS_MOVE = 4;
    /** PERK_SLAYER = 23. */
    public static final int PERK_SLAYER = 23;
    /** PERK_SNIPER = 24. */
    public static final int PERK_SNIPER = 24;
    /** PERK_SILENT_DEATH = 25. */
    public static final int PERK_SILENT_DEATH = 25;
    /** PERK_QUICK_RECOVERY = 103. */
    public static final int PERK_QUICK_RECOVERY = 103;
    /** PERK_JINXED = 119. */
    public static final int PERK_JINXED = 119;

    public static boolean perkIsValid(int perk) {
        return perk >= PERK_FIRST && perk < PERK_COUNT;
    }

    // 0x496AB0 perkGetRank: the critter's party slot (dude is index 0), or
    // the fallback slot when the critter is not a party member.
    public static int perkGetRank(ObjTypes.GameObject critter, int perk) {
        if (!perkIsValid(perk)) {
            return 0;
        }

        int[][] ranks = PartyMembers.perkEnsureRanks();

        if (critter == ObjectSystem.getDude()) {
            return ranks[0][perk];
        }

        for (int index = 1; index < PartyMembers.descriptionsLength; index++) {
            if (ProtoTypes.ProtoId.ofGameObject(critter).equals(PartyMembers.partyMemberProtoIds.get(index))) {
                return ranks[index][perk];
            }
        }

        return ranks[0][perk];
    }

    /** Port of {@code perkHasRank} (src/perk.h). */
    public static boolean perkHasRank(ObjTypes.GameObject critter, int perk) {
        return perkGetRank(critter, perk) != 0;
    }
}
