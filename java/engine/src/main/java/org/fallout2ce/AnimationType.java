package org.fallout2ce;

/**
 * Port of {@code AnimationType} from {@code src/animation_defs.h}
 * (values preserved).
 */
public enum AnimationType {
    INVALID(-1),
    STAND(0),
    WALK(1),
    JUMP_BEGIN(2),
    JUMP_END(3),
    CLIMB_LADDER(4),
    FALLING(5),
    UP_STAIRS_RIGHT(6),
    UP_STAIRS_LEFT(7),
    DOWN_STAIRS_RIGHT(8),
    DOWN_STAIRS_LEFT(9),
    MAGIC_HANDS_GROUND(10),
    MAGIC_HANDS_MIDDLE(11),
    MAGIC_HANDS_UP(12),
    DODGE_ANIM(13),
    HIT_FROM_FRONT(14),
    HIT_FROM_BACK(15),
    THROW_PUNCH(16),
    KICK_LEG(17),
    THROW_ANIM(18),
    RUNNING(19),
    FALL_BACK(20),
    FALL_FRONT(21),
    BAD_LANDING(22),
    BIG_HOLE(23),
    CHARRED_BODY(24),
    CHUNKS_OF_FLESH(25),
    DANCING_AUTOFIRE(26),
    ELECTRIFY(27),
    SLICED_IN_HALF(28),
    BURNED_TO_NOTHING(29),
    ELECTRIFIED_TO_NOTHING(30),
    EXPLODED_TO_NOTHING(31),
    MELTED_TO_NOTHING(32),
    FIRE_DANCE(33),
    FALL_BACK_BLOOD(34),
    FALL_FRONT_BLOOD(35),
    PRONE_TO_STANDING(36),
    BACK_TO_STANDING(37),
    TAKE_OUT(38),
    PUT_AWAY(39),
    PARRY_ANIM(40),
    THRUST_ANIM(41),
    SWING_ANIM(42),
    POINT(43),
    UNPOINT(44),
    FIRE_SINGLE(45),
    FIRE_BURST(46),
    FIRE_CONTINUOUS(47),
    FALL_BACK_SF(48),
    FALL_FRONT_SF(49),
    BAD_LANDING_SF(50),
    BIG_HOLE_SF(51),
    CHARRED_BODY_SF(52),
    CHUNKS_OF_FLESH_SF(53),
    DANCING_AUTOFIRE_SF(54),
    ELECTRIFY_SF(55),
    SLICED_IN_HALF_SF(56),
    BURNED_TO_NOTHING_SF(57),
    ELECTRIFIED_TO_NOTHING_SF(58),
    EXPLODED_TO_NOTHING_SF(59),
    MELTED_TO_NOTHING_SF(60),
    FIRE_DANCE_SF(61),
    FALL_BACK_BLOOD_SF(62),
    FALL_FRONT_BLOOD_SF(63),
    CALLED_SHOT_PIC(64);

    public static final int COUNT = 65;
    public static final int FIRST = 0;
    public static final int FIRST_KNOCKDOWN_AND_DEATH_ANIM = 20;
    public static final int LAST_KNOCKDOWN_AND_DEATH_ANIM = 35;
    public static final int FIRST_SF_DEATH_ANIM = 48;
    public static final int LAST_SF_DEATH_ANIM = 63;

    private final int value;

    AnimationType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static AnimationType fromInt(int value) {
        for (AnimationType anim : values()) {
            if (anim.value == value) {
                return anim;
            }
        }
        return INVALID;
    }

    public static boolean isValid(int anim) {
        return anim >= FIRST && anim < COUNT;
    }
}
