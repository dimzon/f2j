package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Combat id tables, mirroring the pure parts of {@code src/combat_defs.h}.
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 * {@code CombatStartData}/{@code Attack} need the game {@code Object} and
 * come with the game-state phase.
 */
public final class CombatDefs {

    private CombatDefs() {
    }

    public static final int CRITICAL_EFFECT_COUNT = 6;
    public static final int CRITICAL_EFFECT_FIRST = 0;

    public enum CriticalEffect {
        CRITICAL_EFFECT_1(0),
        CRITICAL_EFFECT_2(1),
        CRITICAL_EFFECT_3(2),
        CRITICAL_EFFECT_4(3),
        CRITICAL_EFFECT_5(4),
        CRITICAL_EFFECT_6(5);

        private final int id;

        CriticalEffect(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public CriticalEffect next() {
            return CombatDefs.ofCriticalEffect(id + 1);
        }
    }

    private static final Map<Integer, CriticalEffect> CRIT_BY_ID = new HashMap<>();

    static {
        for (CriticalEffect e : CriticalEffect.values()) {
            CRIT_BY_ID.put(e.id, e);
        }
    }

    public static CriticalEffect ofCriticalEffect(int id) {
        return CRIT_BY_ID.get(id);
    }

    public static boolean criticalEffectIsValid(int effect) {
        return effect >= CRITICAL_EFFECT_FIRST && effect < CRITICAL_EFFECT_COUNT;
    }

    /** Port of {@code CombatState} (unsigned bitmask): immutable bit bag. */
    public static final class CombatState {
        public static final CombatState OUT_COMBAT = new CombatState(0x00);
        public static final CombatState IN_COMBAT = new CombatState(0x01);
        public static final CombatState PLAYER_TURN = new CombatState(0x02);
        public static final CombatState EXIT_REQUESTED = new CombatState(0x08);

        private final int bits;

        public CombatState(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public CombatState not() {
            return new CombatState(~bits);
        }

        public CombatState and(CombatState rhs) {
            return new CombatState(bits & rhs.bits);
        }

        public CombatState and(int rhs) {
            return new CombatState(bits & rhs);
        }

        public CombatState or(CombatState rhs) {
            return new CombatState(bits | rhs.bits);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CombatState && ((CombatState) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "CombatState(0x" + Integer.toHexString(bits) + ")";
        }
    }

    public static final int HIT_MODE_COUNT = 20;
    public static final int HIT_MODE_FIRST = 0;
    public static final int FIRST_ADVANCED_PUNCH_HIT_MODE = 8;
    public static final int LAST_ADVANCED_PUNCH_HIT_MODE = 13;
    public static final int FIRST_ADVANCED_KICK_HIT_MODE = 14;
    public static final int LAST_ADVANCED_KICK_HIT_MODE = 19;
    public static final int FIRST_ADVANCED_UNARMED_HIT_MODE = FIRST_ADVANCED_PUNCH_HIT_MODE;
    public static final int LAST_ADVANCED_UNARMED_HIT_MODE = LAST_ADVANCED_KICK_HIT_MODE;

    public enum HitMode {
        HIT_MODE_INVALID(-1),
        HIT_MODE_LEFT_WEAPON_PRIMARY(0),
        HIT_MODE_LEFT_WEAPON_SECONDARY(1),
        HIT_MODE_RIGHT_WEAPON_PRIMARY(2),
        HIT_MODE_RIGHT_WEAPON_SECONDARY(3),
        HIT_MODE_PUNCH(4),
        HIT_MODE_KICK(5),
        HIT_MODE_LEFT_WEAPON_RELOAD(6),
        HIT_MODE_RIGHT_WEAPON_RELOAD(7),
        HIT_MODE_STRONG_PUNCH(8),
        HIT_MODE_HAMMER_PUNCH(9),
        HIT_MODE_HAYMAKER(10),
        HIT_MODE_JAB(11),
        HIT_MODE_PALM_STRIKE(12),
        HIT_MODE_PIERCING_STRIKE(13),
        HIT_MODE_STRONG_KICK(14),
        HIT_MODE_SNAP_KICK(15),
        HIT_MODE_POWER_KICK(16),
        HIT_MODE_HIP_KICK(17),
        HIT_MODE_HOOK_KICK(18),
        HIT_MODE_PIERCING_KICK(19);

        private final int id;

        HitMode(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public HitMode next() {
            return CombatDefs.ofHitMode(id + 1);
        }
    }

    private static final Map<Integer, HitMode> HITMODE_BY_ID = new HashMap<>();

    static {
        for (HitMode e : HitMode.values()) {
            HITMODE_BY_ID.put(e.id, e);
        }
    }

    public static HitMode ofHitMode(int id) {
        return HITMODE_BY_ID.get(id);
    }

    public static boolean hitModeIsValid(int hitMode) {
        return hitMode >= HIT_MODE_FIRST && hitMode < HIT_MODE_COUNT;
    }

    public static final int HIT_LOCATION_COUNT = 9;
    public static final int HIT_LOCATION_SPECIFIC_COUNT = HIT_LOCATION_COUNT - 1;
    public static final int HIT_LOCATION_FIRST = 0;

    public enum HitLocation {
        HIT_LOCATION_HEAD(0),
        HIT_LOCATION_LEFT_ARM(1),
        HIT_LOCATION_RIGHT_ARM(2),
        HIT_LOCATION_TORSO(3),
        HIT_LOCATION_RIGHT_LEG(4),
        HIT_LOCATION_LEFT_LEG(5),
        HIT_LOCATION_EYES(6),
        HIT_LOCATION_GROIN(7),
        HIT_LOCATION_UNCALLED(8);

        private final int id;

        HitLocation(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public HitLocation next() {
            return CombatDefs.ofHitLocation(id + 1);
        }
    }

    private static final Map<Integer, HitLocation> HITLOC_BY_ID = new HashMap<>();

    static {
        for (HitLocation e : HitLocation.values()) {
            HITLOC_BY_ID.put(e.id, e);
        }
    }

    public static HitLocation ofHitLocation(int id) {
        return HITLOC_BY_ID.get(id);
    }

    public static boolean hitLocationIsValid(int hitLocation) {
        return hitLocation >= HIT_LOCATION_FIRST && hitLocation < HIT_LOCATION_COUNT;
    }

    public static final int CRIT_DATA_MEMBER_COUNT = 7;
    public static final int CRIT_DATA_MEMBER_FIRST = 0;

    public enum CriticalHitDataMember {
        CRIT_DATA_MEMBER_DAMAGE_MULTIPLIER(0),
        CRIT_DATA_MEMBER_FLAGS(1),
        CRIT_DATA_MEMBER_MASSIVE_CRITICAL_STAT(2),
        CRIT_DATA_MEMBER_MASSIVE_CRITICAL_STAT_MODIFIER(3),
        CRIT_DATA_MEMBER_MASSIVE_CRITICAL_FLAGS(4),
        CRIT_DATA_MEMBER_MESSAGE_ID(5),
        CRIT_DATA_MEMBER_MASSIVE_CRITICAL_MESSAGE_ID(6);

        private final int id;

        CriticalHitDataMember(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public CriticalHitDataMember next() {
            return CombatDefs.ofCriticalHitDataMember(id + 1);
        }
    }

    private static final Map<Integer, CriticalHitDataMember> CRITMEMBER_BY_ID = new HashMap<>();

    static {
        for (CriticalHitDataMember e : CriticalHitDataMember.values()) {
            CRITMEMBER_BY_ID.put(e.id, e);
        }
    }

    public static CriticalHitDataMember ofCriticalHitDataMember(int id) {
        return CRITMEMBER_BY_ID.get(id);
    }

    public static boolean criticalHitDataMemberIsValid(int member) {
        return member >= CRIT_DATA_MEMBER_FIRST && member < CRIT_DATA_MEMBER_COUNT;
    }

    public enum CombatBadShot {
        COMBAT_BAD_SHOT_OK(0),
        COMBAT_BAD_SHOT_NO_AMMO(1),
        COMBAT_BAD_SHOT_OUT_OF_RANGE(2),
        COMBAT_BAD_SHOT_NOT_ENOUGH_AP(3),
        COMBAT_BAD_SHOT_ALREADY_DEAD(4),
        COMBAT_BAD_SHOT_AIM_BLOCKED(5),
        COMBAT_BAD_SHOT_ARM_CRIPPLED(6),
        COMBAT_BAD_SHOT_BOTH_ARMS_CRIPPLED(7);

        private final int id;

        CombatBadShot(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, CombatBadShot> BADSHOT_BY_ID = new HashMap<>();

    static {
        for (CombatBadShot e : CombatBadShot.values()) {
            BADSHOT_BY_ID.put(e.id, e);
        }
    }

    public static CombatBadShot ofCombatBadShot(int id) {
        return BADSHOT_BY_ID.get(id);
    }

    /**
     * Port of {@code CriticalHitDescription}. The C union overlays
     * {@code values[7]} on the named fields; Java keeps named fields with an
     * indexed view ({@link #values()} builds the array,
     * {@link #setIndexed} writes through).
     */
    public static final class CriticalHitDescription {
        public int damageMultiplier;
        public int flags;
        public StatDefs.Stat massiveCriticalStat;
        public int massiveCriticalStatModifier;
        public int massiveCriticalFlags;
        public int messageId;
        public int massiveCriticalMessageId;

        public int[] values() {
            return new int[] {
                damageMultiplier,
                flags,
                massiveCriticalStat != null ? massiveCriticalStat.id() : -1,
                massiveCriticalStatModifier,
                massiveCriticalFlags,
                messageId,
                massiveCriticalMessageId,
            };
        }

        public void setIndexed(CriticalHitDataMember member, int value) {
            switch (member) {
            case CRIT_DATA_MEMBER_DAMAGE_MULTIPLIER:
                damageMultiplier = value;
                break;
            case CRIT_DATA_MEMBER_FLAGS:
                flags = value;
                break;
            case CRIT_DATA_MEMBER_MASSIVE_CRITICAL_STAT:
                massiveCriticalStat = StatDefs.ofStat(value);
                break;
            case CRIT_DATA_MEMBER_MASSIVE_CRITICAL_STAT_MODIFIER:
                massiveCriticalStatModifier = value;
                break;
            case CRIT_DATA_MEMBER_MASSIVE_CRITICAL_FLAGS:
                massiveCriticalFlags = value;
                break;
            case CRIT_DATA_MEMBER_MESSAGE_ID:
                messageId = value;
                break;
            case CRIT_DATA_MEMBER_MASSIVE_CRITICAL_MESSAGE_ID:
                massiveCriticalMessageId = value;
                break;
            default:
                throw new IllegalArgumentException("member=" + member);
            }
        }
    }
}
