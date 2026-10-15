package org.fallout2ce;

/**
 * Port of {@code WeaponAnimation} from {@code src/art_defs.h} (values
 * preserved; remaining art_defs enums land with their users).
 */
public enum WeaponAnimation {
    NONE(0),
    KNIFE(1),
    CLUB(2),
    HAMMER(3),
    SPEAR(4),
    PISTOL(5),
    SMG(6),
    SHOTGUN(7),
    LASER_RIFLE(8),
    MINIGUN(9),
    LAUNCHER(10),
    SFALL_S(11),
    SFALL_O(12),
    SFALL_P(13),
    SFALL_Q(14),
    SFALL_T(15);

    private final int value;

    WeaponAnimation(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static WeaponAnimation fromInt(int value) {
        for (WeaponAnimation anim : values()) {
            if (anim.value == value) {
                return anim;
            }
        }
        return NONE;
    }

    public static boolean isValid(int weaponAnimation) {
        return weaponAnimation >= NONE.value && weaponAnimation <= SFALL_T.value;
    }
}
