package org.fallout2ce;

/**
 * Map grid constants and header flags, mirroring {@code src/map_defs.h}.
 *
 * <p>{@code MapHeaderFlags} is an immutable bit bag: the C++ in-place
 * {@code |=}/{@code &=} become reassignment ({@code flags = flags.or_(...)}),
 * and the value operators ({@code ~}, {@code &}, {@code |}) become methods
 * returning a new bag. The C++ {@code operator|} masks its result with
 * {@code 0xF}; {@link MapHeaderFlags#or_(MapHeaderFlags)} preserves that.
 */
public final class MapDefs {

    public static final int ELEVATION_COUNT = 3;

    public static final int SQUARE_GRID_WIDTH = 100;
    public static final int SQUARE_GRID_HEIGHT = 100;
    public static final int SQUARE_GRID_SIZE = SQUARE_GRID_WIDTH * SQUARE_GRID_HEIGHT;

    public static final int HEX_GRID_WIDTH = 200;
    public static final int HEX_GRID_HEIGHT = 200;
    public static final int HEX_GRID_SIZE = HEX_GRID_WIDTH * HEX_GRID_HEIGHT;

    private MapDefs() {
    }

    public static final class MapHeaderFlags {
        public static final MapHeaderFlags NONE = new MapHeaderFlags(0x00);
        public static final MapHeaderFlags SAVED = new MapHeaderFlags(0x01);
        public static final MapHeaderFlags ELEVATION_0 = new MapHeaderFlags(0x02);
        public static final MapHeaderFlags ELEVATION_1 = new MapHeaderFlags(0x04);
        public static final MapHeaderFlags ELEVATION_2 = new MapHeaderFlags(0x08);

        private final int bits;

        public MapHeaderFlags(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public MapHeaderFlags not() {
            return new MapHeaderFlags(~bits);
        }

        public MapHeaderFlags and_(MapHeaderFlags rhs) {
            return new MapHeaderFlags(bits & rhs.bits);
        }

        /** Bitwise or, masked with {@code 0xF} like the C++ operator. */
        public MapHeaderFlags or_(MapHeaderFlags rhs) {
            return new MapHeaderFlags((bits | rhs.bits) & 0xF);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MapHeaderFlags && ((MapHeaderFlags) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "MapHeaderFlags(0x" + Integer.toHexString(bits) + ")";
        }
    }

    public static boolean elevationIsValid(int elevation) {
        return elevation >= 0 && elevation < ELEVATION_COUNT;
    }

    public static boolean squareGridTileIsValid(int tile) {
        return tile >= 0 && tile < SQUARE_GRID_SIZE;
    }

    public static boolean hexGridTileIsValid(int tile) {
        return tile >= 0 && tile < HEX_GRID_SIZE;
    }
}
