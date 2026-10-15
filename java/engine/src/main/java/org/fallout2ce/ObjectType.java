package org.fallout2ce;

/**
 * Port of {@code ObjectType} from {@code src/obj_types.h} (values preserved).
 */
public enum ObjectType {
    INVALID(-1),
    ITEM(0),
    CRITTER(1),
    SCENERY(2),
    WALL(3),
    TILE(4),
    MISC(5),
    INTERFACE(6),
    INVENTORY(7),
    HEAD(8),
    BACKGROUND(9),
    SKILLDEX(10);

    public static final int COUNT = 11;
    public static final int PROTO_COUNT = 6; // up to INTERFACE
    public static final int FIRST = 0;

    private final int value;

    ObjectType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static ObjectType fromInt(int value) {
        for (ObjectType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return INVALID;
    }

    public static boolean isValid(int type) {
        return type >= FIRST && type < COUNT;
    }
}
