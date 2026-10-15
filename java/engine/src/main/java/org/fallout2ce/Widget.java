package org.fallout2ce;

/**
 * Region widgets, mirroring {@code src/widget.h/cc} (both functions are
 * TODO-incomplete in C++ and stay that way here).
 */
public final class Widget {

    private Widget() {
    }

    private static final int[] updateRegions = new int[32];

    private static void showRegion(int region) {
        // TODO: Incomplete.
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
        // TODO: Incomplete.
    }

    /** Test seam: fresh widget state. */
    static void resetForTests() {
        java.util.Arrays.fill(updateRegions, 0);
    }
}
