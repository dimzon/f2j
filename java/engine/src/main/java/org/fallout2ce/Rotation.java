package org.fallout2ce;

/**
 * Port of {@code Rotation} from {@code src/obj_types.h} (values preserved).
 */
public enum Rotation {
    INVALID(-1),
    NE(0),
    E(1),
    SE(2),
    SW(3),
    W(4),
    NW(5);

    public static final int COUNT = 6;
    public static final int FIRST = 0;
    public static final int LAST = 5;

    private final int value;

    Rotation(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static Rotation fromInt(int value) {
        for (Rotation rotation : values()) {
            if (rotation.value == value) {
                return rotation;
            }
        }
        return INVALID;
    }

    public static boolean isValid(int rotation) {
        return rotation >= FIRST && rotation < COUNT;
    }
}
