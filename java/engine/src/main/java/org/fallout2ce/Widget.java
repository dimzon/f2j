package org.fallout2ce;

/**
 * Region widgets, mirroring {@code src/widget.h/cc}.
 *
 * <p>NOTE: {@code showRegion} and {@code windowDeleteWidgets} are
 * intentionally empty — the C++ originals are TODO-incomplete
 * ({@code src/widget.cc}) and the behavior is not observable without the
 * window refresh/engine phase. The mirrors keep the same shape.
 */
public final class Widget {

    private Widget() {
    }

    private static final int[] updateRegions = new int[32];

    private static void showRegion(int region) {
        // NOTE: mirrors upstream TODO — no-op until the refresh engine lands.
    }

    /** Port of {@code windowUpdateWidgets} (0x4B5C24 update_widgets). */
    public static int windowUpdateWidgets() {
        for (int index = 0; index < 32; index++) {
            if (updateRegions[index] != 0) {
                showRegion(updateRegions[index]);
            }
        }

        return 1;
    }

    /** Port of {@code windowDeleteWidgets} (0x4B5998, incomplete). */
    public static void windowDeleteWidgets(int win) {
        // NOTE: mirrors upstream TODO — no-op (incomplete in C++).
    }

    /** Test seam: fresh widget state. */
    static void resetForTests() {
        java.util.Arrays.fill(updateRegions, 0);
    }
}
