package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Animation request flags and animation ids, mirroring
 * {@code src/animation_defs.h}.
 *
 * <p>{@code AnimationRequestOptions} is an immutable bit bag (see
 * {@link MapDefs.MapHeaderFlags}); the C++ in-place {@code |=} becomes
 * reassignment ({@code opts = opts.or_(...)}).
 */
public final class AnimationDefs {

    private AnimationDefs() {
    }

    public static final class AnimationRequestOptions {
        public static final AnimationRequestOptions NONE = new AnimationRequestOptions(0x00);
        public static final AnimationRequestOptions UNRESERVED = new AnimationRequestOptions(0x01);
        public static final AnimationRequestOptions RESERVED = new AnimationRequestOptions(0x02);
        public static final AnimationRequestOptions NO_STAND = new AnimationRequestOptions(0x04);
        public static final AnimationRequestOptions PING = new AnimationRequestOptions(0x100);
        public static final AnimationRequestOptions INSIGNIFICANT = new AnimationRequestOptions(0x200);

        private final int bits;

        public AnimationRequestOptions(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public AnimationRequestOptions or_(AnimationRequestOptions rhs) {
            return new AnimationRequestOptions(bits | rhs.bits);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof AnimationRequestOptions && ((AnimationRequestOptions) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "AnimationRequestOptions(0x" + Integer.toHexString(bits) + ")";
        }
    }

    public static final int ANIM_COUNT = 65;
    public static final int ANIM_FIRST = 0;
    public static final int FIRST_KNOCKDOWN_AND_DEATH_ANIM = 20;
    public static final int LAST_KNOCKDOWN_AND_DEATH_ANIM = 35;
    public static final int FIRST_SF_DEATH_ANIM = 48;
    public static final int LAST_SF_DEATH_ANIM = 63;

    public enum AnimationType {
        ANIM_INVALID(-1),
        ANIM_STAND(0),
        ANIM_WALK(1),
        ANIM_JUMP_BEGIN(2),
        ANIM_JUMP_END(3),
        ANIM_CLIMB_LADDER(4),
        ANIM_FALLING(5),
        ANIM_UP_STAIRS_RIGHT(6),
        ANIM_UP_STAIRS_LEFT(7),
        ANIM_DOWN_STAIRS_RIGHT(8),
        ANIM_DOWN_STAIRS_LEFT(9),
        ANIM_MAGIC_HANDS_GROUND(10),
        ANIM_MAGIC_HANDS_MIDDLE(11),
        ANIM_MAGIC_HANDS_UP(12),
        ANIM_DODGE_ANIM(13),
        ANIM_HIT_FROM_FRONT(14),
        ANIM_HIT_FROM_BACK(15),
        ANIM_THROW_PUNCH(16),
        ANIM_KICK_LEG(17),
        ANIM_THROW_ANIM(18),
        ANIM_RUNNING(19),
        ANIM_FALL_BACK(20),
        ANIM_FALL_FRONT(21),
        ANIM_BAD_LANDING(22),
        ANIM_BIG_HOLE(23),
        ANIM_CHARRED_BODY(24),
        ANIM_CHUNKS_OF_FLESH(25),
        ANIM_DANCING_AUTOFIRE(26),
        ANIM_ELECTRIFY(27),
        ANIM_SLICED_IN_HALF(28),
        ANIM_BURNED_TO_NOTHING(29),
        ANIM_ELECTRIFIED_TO_NOTHING(30),
        ANIM_EXPLODED_TO_NOTHING(31),
        ANIM_MELTED_TO_NOTHING(32),
        ANIM_FIRE_DANCE(33),
        ANIM_FALL_BACK_BLOOD(34),
        ANIM_FALL_FRONT_BLOOD(35),
        ANIM_PRONE_TO_STANDING(36),
        ANIM_BACK_TO_STANDING(37),
        ANIM_TAKE_OUT(38),
        ANIM_PUT_AWAY(39),
        ANIM_PARRY_ANIM(40),
        ANIM_THRUST_ANIM(41),
        ANIM_SWING_ANIM(42),
        ANIM_POINT(43),
        ANIM_UNPOINT(44),
        ANIM_FIRE_SINGLE(45),
        ANIM_FIRE_BURST(46),
        ANIM_FIRE_CONTINUOUS(47),
        ANIM_FALL_BACK_SF(48),
        ANIM_FALL_FRONT_SF(49),
        ANIM_BAD_LANDING_SF(50),
        ANIM_BIG_HOLE_SF(51),
        ANIM_CHARRED_BODY_SF(52),
        ANIM_CHUNKS_OF_FLESH_SF(53),
        ANIM_DANCING_AUTOFIRE_SF(54),
        ANIM_ELECTRIFY_SF(55),
        ANIM_SLICED_IN_HALF_SF(56),
        ANIM_BURNED_TO_NOTHING_SF(57),
        ANIM_ELECTRIFIED_TO_NOTHING_SF(58),
        ANIM_EXPLODED_TO_NOTHING_SF(59),
        ANIM_MELTED_TO_NOTHING_SF(60),
        ANIM_FIRE_DANCE_SF(61),
        ANIM_FALL_BACK_BLOOD_SF(62),
        ANIM_FALL_FRONT_BLOOD_SF(63),
        ANIM_CALLED_SHOT_PIC(64);

        private final int id;

        AnimationType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, AnimationType> BY_ID = new HashMap<>();

    static {
        for (AnimationType anim : AnimationType.values()) {
            BY_ID.put(anim.id, anim);
        }
    }

    public static AnimationType of(int id) {
        return BY_ID.get(id);
    }

    public static boolean animationTypeIsValid(int anim) {
        return anim >= ANIM_FIRST && anim < ANIM_COUNT;
    }
}
