package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Perk id table, mirroring {@code src/perk_defs.h}.
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 */
public final class PerkDefs {

    public static final int PERK_COUNT = 119;
    public static final int PERK_FIRST = 0;

    private PerkDefs() {
    }

    public enum Perk {
        PERK_INVALID(-1),
        PERK_AWARENESS(0),
        PERK_BONUS_HTH_ATTACKS(1),
        PERK_BONUS_HTH_DAMAGE(2),
        PERK_BONUS_MOVE(3),
        PERK_BONUS_RANGED_DAMAGE(4),
        PERK_BONUS_RATE_OF_FIRE(5),
        PERK_EARLIER_SEQUENCE(6),
        PERK_FASTER_HEALING(7),
        PERK_MORE_CRITICALS(8),
        PERK_NIGHT_VISION(9),
        PERK_PRESENCE(10),
        PERK_RAD_RESISTANCE(11),
        PERK_TOUGHNESS(12),
        PERK_STRONG_BACK(13),
        PERK_SHARPSHOOTER(14),
        PERK_SILENT_RUNNING(15),
        PERK_SURVIVALIST(16),
        PERK_MASTER_TRADER(17),
        PERK_EDUCATED(18),
        PERK_HEALER(19),
        PERK_FORTUNE_FINDER(20),
        PERK_BETTER_CRITICALS(21),
        PERK_EMPATHY(22),
        PERK_SLAYER(23),
        PERK_SNIPER(24),
        PERK_SILENT_DEATH(25),
        PERK_ACTION_BOY(26),
        PERK_MENTAL_BLOCK(27),
        PERK_LIFEGIVER(28),
        PERK_DODGER(29),
        PERK_SNAKEATER(30),
        PERK_MR_FIXIT(31),
        PERK_MEDIC(32),
        PERK_MASTER_THIEF(33),
        PERK_SPEAKER(34),
        PERK_HEAVE_HO(35),
        PERK_FRIENDLY_FOE(36),
        PERK_PICKPOCKET(37),
        PERK_GHOST(38),
        PERK_CULT_OF_PERSONALITY(39),
        PERK_SCROUNGER(40),
        PERK_EXPLORER(41),
        PERK_FLOWER_CHILD(42),
        PERK_PATHFINDER(43),
        PERK_ANIMAL_FRIEND(44),
        PERK_SCOUT(45),
        PERK_MYSTERIOUS_STRANGER(46),
        PERK_RANGER(47),
        PERK_QUICK_POCKETS(48),
        PERK_SMOOTH_TALKER(49),
        PERK_SWIFT_LEARNER(50),
        PERK_TAG(51),
        PERK_MUTATE(52),
        PERK_NUKA_COLA_ADDICTION(53),
        PERK_BUFFOUT_ADDICTION(54),
        PERK_MENTATS_ADDICTION(55),
        PERK_PSYCHO_ADDICTION(56),
        PERK_RADAWAY_ADDICTION(57),
        PERK_WEAPON_LONG_RANGE(58),
        PERK_WEAPON_ACCURATE(59),
        PERK_WEAPON_PENETRATE(60),
        PERK_WEAPON_KNOCKBACK(61),
        PERK_POWERED_ARMOR(62),
        PERK_COMBAT_ARMOR(63),
        PERK_WEAPON_SCOPE_RANGE(64),
        PERK_WEAPON_FAST_RELOAD(65),
        PERK_WEAPON_NIGHT_SIGHT(66),
        PERK_WEAPON_FLAMEBOY(67),
        PERK_ARMOR_ADVANCED_I(68),
        PERK_ARMOR_ADVANCED_II(69),
        PERK_JET_ADDICTION(70),
        PERK_TRAGIC_ADDICTION(71),
        PERK_ARMOR_CHARISMA(72),
        PERK_GECKO_SKINNING(73),
        PERK_DERMAL_IMPACT_ARMOR(74),
        PERK_DERMAL_IMPACT_ASSAULT_ENHANCEMENT(75),
        PERK_PHOENIX_ARMOR_IMPLANTS(76),
        PERK_PHOENIX_ASSAULT_ENHANCEMENT(77),
        PERK_VAULT_CITY_INOCULATIONS(78),
        PERK_ADRENALINE_RUSH(79),
        PERK_CAUTIOUS_NATURE(80),
        PERK_COMPREHENSION(81),
        PERK_DEMOLITION_EXPERT(82),
        PERK_GAMBLER(83),
        PERK_GAIN_STRENGTH(84),
        PERK_GAIN_PERCEPTION(85),
        PERK_GAIN_ENDURANCE(86),
        PERK_GAIN_CHARISMA(87),
        PERK_GAIN_INTELLIGENCE(88),
        PERK_GAIN_AGILITY(89),
        PERK_GAIN_LUCK(90),
        PERK_HARMLESS(91),
        PERK_HERE_AND_NOW(92),
        PERK_HTH_EVADE(93),
        PERK_KAMA_SUTRA_MASTER(94),
        PERK_KARMA_BEACON(95),
        PERK_LIGHT_STEP(96),
        PERK_LIVING_ANATOMY(97),
        PERK_MAGNETIC_PERSONALITY(98),
        PERK_NEGOTIATOR(99),
        PERK_PACK_RAT(100),
        PERK_PYROMANIAC(101),
        PERK_QUICK_RECOVERY(102),
        PERK_SALESMAN(103),
        PERK_STONEWALL(104),
        PERK_THIEF(105),
        PERK_WEAPON_HANDLING(106),
        PERK_VAULT_CITY_TRAINING(107),
        PERK_ALCOHOL_RAISED_HIT_POINTS(108),
        PERK_ALCOHOL_RAISED_HIT_POINTS_II(109),
        PERK_ALCOHOL_LOWERED_HIT_POINTS(110),
        PERK_ALCOHOL_LOWERED_HIT_POINTS_II(111),
        PERK_AUTODOC_RAISED_HIT_POINTS(112),
        PERK_AUTODOC_RAISED_HIT_POINTS_II(113),
        PERK_AUTODOC_LOWERED_HIT_POINTS(114),
        PERK_AUTODOC_LOWERED_HIT_POINTS_II(115),
        PERK_EXPERT_EXCREMENT_EXPEDITOR(116),
        PERK_WEAPON_ENHANCED_KNOCKOUT(117),
        PERK_JINXED(118);

        private final int id;

        Perk(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public Perk next() {
            return PerkDefs.of(id + 1);
        }
    }

    private static final Map<Integer, Perk> BY_ID = new HashMap<>();

    static {
        for (Perk perk : Perk.values()) {
            BY_ID.put(perk.id, perk);
        }
    }

    public static Perk of(int id) {
        return BY_ID.get(id);
    }

    public static boolean perkIsValid(int perk) {
        return perk >= PERK_FIRST && perk < PERK_COUNT;
    }
}
