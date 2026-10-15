package org.fallout2ce;

/**
 * iOS quick-toolbar hooks, mirroring the non-iOS inline fallbacks from
 * {@code src/platform/ios/quick_toolbar.h} (all no-ops that never claim a
 * tap). The platform backend replaces the provider with the real toolbar.
 */
public final class QuickToolbar {

    private QuickToolbar() {
    }

    /** Port of the toolbar hit-test (default: absent). */
    public interface Toolbar {
        boolean containsPoint(int x, int y);

        boolean handleTap(int x, int y);
    }

    private static final Toolbar ABSENT = new Toolbar() {
        @Override
        public boolean containsPoint(int x, int y) {
            return false;
        }

        @Override
        public boolean handleTap(int x, int y) {
            return false;
        }
    };

    public static Toolbar toolbar = ABSENT;

    /** Test seam: restore the absent toolbar. */
    static void resetForTests() {
        toolbar = ABSENT;
    }
}
