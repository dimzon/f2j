package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Stat id tables, mirroring {@code src/stat_defs.h}.
 *
 * <p>The C++ {@code operator++} (post-increment returning the old value and
 * advancing the id by one, used for iteration) becomes {@link Stat#next()} /
 * {@link PcStat#next()}; iterate with {@code values()} instead. Past the
 * end {@code next()} returns null (C++ yields an out-of-range id, which its
 * callers guard with {@code statIsValid}).
 */
public final class StatDefs {

    public static final int PRIMARY_STAT_MIN = 1;
    public static final int PRIMARY_STAT_MAX = 10;
    public static final int PRIMARY_STAT_RANGE = PRIMARY_STAT_MAX - PRIMARY_STAT_MIN + 1;

    public static final int PC_LEVEL_MAX = 99;

    public static final int STAT_COUNT = 38;
    public static final int STAT_FIRST = 0;
    public static final int PRIMARY_STAT_COUNT = 7;
    public static final int SPECIAL_STAT_COUNT = 33;
    public static final int SAVEABLE_STAT_COUNT = 35;

    public static final int PC_STAT_COUNT = 5;
    public static final int PC_STAT_FIRST = 0;

    private StatDefs() {
    }

    public enum Stat {
        STAT_INVALID(-1),
        STAT_STRENGTH(0),
        STAT_PERCEPTION(1),
        STAT_ENDURANCE(2),
        STAT_CHARISMA(3),
        STAT_INTELLIGENCE(4),
        STAT_AGILITY(5),
        STAT_LUCK(6),
        STAT_MAXIMUM_HIT_POINTS(7),
        STAT_MAXIMUM_ACTION_POINTS(8),
        STAT_ARMOR_CLASS(9),
        STAT_UNARMED_DAMAGE(10),
        STAT_MELEE_DAMAGE(11),
        STAT_CARRY_WEIGHT(12),
        STAT_SEQUENCE(13),
        STAT_HEALING_RATE(14),
        STAT_CRITICAL_CHANCE(15),
        STAT_BETTER_CRITICALS(16),
        STAT_DAMAGE_THRESHOLD(17),
        STAT_DAMAGE_THRESHOLD_LASER(18),
        STAT_DAMAGE_THRESHOLD_FIRE(19),
        STAT_DAMAGE_THRESHOLD_PLASMA(20),
        STAT_DAMAGE_THRESHOLD_ELECTRICAL(21),
        STAT_DAMAGE_THRESHOLD_EMP(22),
        STAT_DAMAGE_THRESHOLD_EXPLOSION(23),
        STAT_DAMAGE_RESISTANCE(24),
        STAT_DAMAGE_RESISTANCE_LASER(25),
        STAT_DAMAGE_RESISTANCE_FIRE(26),
        STAT_DAMAGE_RESISTANCE_PLASMA(27),
        STAT_DAMAGE_RESISTANCE_ELECTRICAL(28),
        STAT_DAMAGE_RESISTANCE_EMP(29),
        STAT_DAMAGE_RESISTANCE_EXPLOSION(30),
        STAT_RADIATION_RESISTANCE(31),
        STAT_POISON_RESISTANCE(32),
        STAT_AGE(33),
        STAT_GENDER(34),
        STAT_CURRENT_HIT_POINTS(35),
        STAT_CURRENT_POISON_LEVEL(36),
        STAT_CURRENT_RADIATION_LEVEL(37);

        private final int id;

        Stat(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public Stat next() {
            return StatDefs.ofStat(id + 1);
        }
    }

    private static final Map<Integer, Stat> BY_ID = new HashMap<>();

    static {
        for (Stat stat : Stat.values()) {
            BY_ID.put(stat.id, stat);
        }
    }

    public static Stat ofStat(int id) {
        return BY_ID.get(id);
    }

    public static boolean statIsValid(int stat) {
        return stat >= STAT_FIRST && stat < STAT_COUNT;
    }

    public enum PcStat {
        PC_STAT_UNSPENT_SKILL_POINTS(0),
        PC_STAT_LEVEL(1),
        PC_STAT_EXPERIENCE(2),
        PC_STAT_REPUTATION(3),
        PC_STAT_KARMA(4);

        private final int id;

        PcStat(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public PcStat next() {
            return StatDefs.ofPcStat(id + 1);
        }
    }

    private static final Map<Integer, PcStat> PC_BY_ID = new HashMap<>();

    static {
        for (PcStat stat : PcStat.values()) {
            PC_BY_ID.put(stat.id, stat);
        }
    }

    public static PcStat ofPcStat(int id) {
        return PC_BY_ID.get(id);
    }

    public static boolean pcStatIsValid(int pcStat) {
        return pcStat >= PC_STAT_FIRST && pcStat < PC_STAT_COUNT;
    }
}
