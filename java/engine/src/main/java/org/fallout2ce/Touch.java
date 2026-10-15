package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Touch gesture types, mirroring {@code src/touch.h} (enumerator names kept
 * 1:1). The gesture engine ({@code touch.cc}) comes with the UI phase; the
 * SDL touch events arrive through the platform backend.
 */
public final class Touch {

    private Touch() {
    }

    public enum GestureType {
        kUnrecognized(0),
        kTap(1),
        kLongPress(2),
        kPan(3);

        private final int id;

        GestureType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, GestureType> GESTURE_BY_ID = new HashMap<>();

    static {
        for (GestureType e : GestureType.values()) {
            GESTURE_BY_ID.put(e.id, e);
        }
    }

    public static GestureType ofGestureType(int id) {
        return GESTURE_BY_ID.get(id);
    }

    public enum GestureState {
        kPossible(0),
        kBegan(1),
        kChanged(2),
        kEnded(3);

        private final int id;

        GestureState(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, GestureState> STATE_BY_ID = new HashMap<>();

    static {
        for (GestureState e : GestureState.values()) {
            STATE_BY_ID.put(e.id, e);
        }
    }

    public static GestureState ofGestureState(int id) {
        return STATE_BY_ID.get(id);
    }

    /** Port of {@code Gesture}. */
    public static final class Gesture {
        public GestureType type = GestureType.kUnrecognized;
        public GestureState state = GestureState.kPossible;
        public int numberOfTouches;
        public int x;
        public int y;
    }
}
