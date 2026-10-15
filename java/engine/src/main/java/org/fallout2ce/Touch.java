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

    private static final int TOUCH_PHASE_BEGAN = 0;
    private static final int TOUCH_PHASE_MOVED = 1;
    private static final int TOUCH_PHASE_ENDED = 2;

    private static final int MAX_TOUCHES = 10;

    private static final int TAP_MAXIMUM_DURATION = 75;
    private static final int PAN_MINIMUM_MOVEMENT = 4;
    private static final int LONG_PRESS_MINIMUM_DURATION = 500;

    private static final class TouchLocation {
        int x;
        int y;
    }

    private static final class TouchPoint {
        boolean used;
        long fingerId;
        TouchLocation startLocation = new TouchLocation();
        int startTimestamp;
        TouchLocation currentLocation = new TouchLocation();
        int currentTimestamp;
        int phase;
    }

    private static final TouchPoint[] touches = new TouchPoint[MAX_TOUCHES];
    private static final Gesture currentGesture = new Gesture();
    private static final java.util.ArrayDeque<Gesture> gestureEventsQueue = new java.util.ArrayDeque<>();
    private static boolean useTouchscreenMode;
    private static boolean usePanMode;

    static {
        for (int index = 0; index < touches.length; index++) {
            touches[index] = new TouchPoint();
        }
    }

    // C++ compares Uint32 timestamps unsigned; mirror with explicit helpers.
    private static boolean ult(int a, int b) {
        return Integer.compareUnsigned(a, b) < 0;
    }

    private static boolean ugt(int a, int b) {
        return Integer.compareUnsigned(a, b) > 0;
    }

    private static boolean uge(int a, int b) {
        return Integer.compareUnsigned(a, b) >= 0;
    }

    private static boolean ule(int a, int b) {
        return Integer.compareUnsigned(a, b) <= 0;
    }

    private static long usub(int a, int b) {
        return (a - b) & 0xFFFFFFFFL;
    }

    private static int findTouch(long fingerId) {
        for (int index = 0; index < MAX_TOUCHES; index++) {
            if (touches[index].fingerId == fingerId) {
                return index;
            }
        }
        return -1;
    }

    private static int findUnusedTouchIndex() {
        for (int index = 0; index < MAX_TOUCHES; index++) {
            if (!touches[index].used) {
                return index;
            }
        }
        return -1;
    }

    private static TouchLocation startLocationCentroid(int[] indexes, int length) {
        TouchLocation centroid = new TouchLocation();
        for (int index = 0; index < length; index++) {
            centroid.x += touches[indexes[index]].startLocation.x;
            centroid.y += touches[indexes[index]].startLocation.y;
        }
        centroid.x /= length;
        centroid.y /= length;
        return centroid;
    }

    private static TouchLocation currentLocationCentroid(int[] indexes, int length) {
        TouchLocation centroid = new TouchLocation();
        for (int index = 0; index < length; index++) {
            centroid.x += touches[indexes[index]].currentLocation.x;
            centroid.y += touches[indexes[index]].currentLocation.y;
        }
        centroid.x /= length;
        centroid.y /= length;
        return centroid;
    }

    private static Gesture copyOf(Gesture gesture) {
        Gesture copy = new Gesture();
        copy.type = gesture.type;
        copy.state = gesture.state;
        copy.numberOfTouches = gesture.numberOfTouches;
        copy.x = gesture.x;
        copy.y = gesture.y;
        return copy;
    }

    /**
     * Port of {@code touch_handle_start} (decomposed SDL_TouchFingerEvent;
     * coordinates are 0..1 fractions of the screen).
     */
    public static void touchHandleStart(long fingerId, int timestamp, float x, float y) {
        // On iOS fingerId is a reused UITouch address (tapCount ignored here).
        int index = findTouch(fingerId);
        if (index == -1) {
            index = findUnusedTouchIndex();
        }

        if (index != -1) {
            TouchPoint touch = touches[index];
            touch.used = true;
            touch.fingerId = fingerId;
            touch.startTimestamp = timestamp;
            touch.startLocation.x = (int) (x * Svga.screenGetWidth());
            touch.startLocation.y = (int) (y * Svga.screenGetHeight());
            touch.currentTimestamp = touch.startTimestamp;
            touch.currentLocation.x = touch.startLocation.x;
            touch.currentLocation.y = touch.startLocation.y;
            touch.phase = TOUCH_PHASE_BEGAN;
        }
    }

    /** Port of {@code touch_handle_move}. */
    public static void touchHandleMove(long fingerId, int timestamp, float x, float y) {
        int index = findTouch(fingerId);
        if (index != -1) {
            TouchPoint touch = touches[index];
            touch.currentTimestamp = timestamp;
            touch.currentLocation.x = (int) (x * Svga.screenGetWidth());
            touch.currentLocation.y = (int) (y * Svga.screenGetHeight());
            touch.phase = TOUCH_PHASE_MOVED;
        }
    }

    /** Port of {@code touch_handle_end}. */
    public static void touchHandleEnd(long fingerId, int timestamp, float x, float y) {
        int index = findTouch(fingerId);
        if (index != -1) {
            TouchPoint touch = touches[index];
            touch.currentTimestamp = timestamp;
            touch.currentLocation.x = (int) (x * Svga.screenGetWidth());
            touch.currentLocation.y = (int) (y * Svga.screenGetHeight());
            touch.phase = TOUCH_PHASE_ENDED;
        }
    }

    /** Port of {@code touch_process_gesture}. */
    public static void touchProcessGesture() {
        int sequenceStartTimestamp = -1;
        int sequenceStartIndex = -1;

        // Find start of sequence (earliest touch).
        for (int index = 0; index < MAX_TOUCHES; index++) {
            if (touches[index].used) {
                if (ugt(sequenceStartTimestamp, touches[index].startTimestamp)) {
                    sequenceStartTimestamp = touches[index].startTimestamp;
                    sequenceStartIndex = index;
                }
            }
        }

        if (sequenceStartIndex == -1) {
            return;
        }

        int sequenceEndTimestamp = -1;
        if (touches[sequenceStartIndex].phase == TOUCH_PHASE_ENDED) {
            sequenceEndTimestamp = touches[sequenceStartIndex].currentTimestamp;

            // Find end timestamp of sequence.
            for (int index = 0; index < MAX_TOUCHES; index++) {
                if (touches[index].used
                        && uge(touches[index].startTimestamp, sequenceStartTimestamp)
                        && ule(touches[index].startTimestamp, sequenceEndTimestamp)) {
                    if (touches[index].phase == TOUCH_PHASE_ENDED) {
                        if (ult(sequenceEndTimestamp, touches[index].currentTimestamp)) {
                            sequenceEndTimestamp = touches[index].currentTimestamp;

                            // Start over since we can have fingers missed.
                            index = -1;
                        }
                    } else {
                        // Sequence is current.
                        sequenceEndTimestamp = -1;
                        break;
                    }
                }
            }
        }

        int[] active = new int[MAX_TOUCHES];
        int activeCount = 0;

        int[] ended = new int[MAX_TOUCHES];
        int endedCount = 0;

        // Split participating fingers into two buckets - active fingers (currently
        // on screen) and ended (lifted up).
        for (int index = 0; index < MAX_TOUCHES; index++) {
            if (touches[index].used
                    && uge(touches[index].currentTimestamp, sequenceStartTimestamp)
                    && ule(touches[index].currentTimestamp, sequenceEndTimestamp)) {
                if (touches[index].phase == TOUCH_PHASE_ENDED) {
                    ended[endedCount++] = index;
                } else {
                    active[activeCount++] = index;
                }

                // If this sequence is over, unmark participating finger as used.
                if (sequenceEndTimestamp != -1) {
                    touches[index].used = false;
                }
            }
        }

        if (currentGesture.type == GestureType.kPan || currentGesture.type == GestureType.kLongPress) {
            if (currentGesture.state != GestureState.kEnded) {
                // For continuous gestures we want number of fingers to remain the
                // same as it was when gesture was recognized.
                if (activeCount == currentGesture.numberOfTouches && endedCount == 0) {
                    TouchLocation centroid = currentLocationCentroid(active, activeCount);
                    currentGesture.state = GestureState.kChanged;
                    currentGesture.x = centroid.x;
                    currentGesture.y = centroid.y;
                    gestureEventsQueue.push(copyOf(currentGesture));
                } else {
                    currentGesture.state = GestureState.kEnded;
                    gestureEventsQueue.push(copyOf(currentGesture));
                }
            }

            // Reset continuous gesture if when current sequence is over.
            if (currentGesture.state == GestureState.kEnded && sequenceEndTimestamp != -1) {
                currentGesture.type = GestureType.kUnrecognized;
            }
        } else {
            if (activeCount == 0 && endedCount != 0) {
                // For taps we need all participating fingers to be both started
                // and ended simultaneously (within predefined threshold).
                int startEarliestTimestamp = -1;
                int startLatestTimestamp = 0;
                int endEarliestTimestamp = -1;
                int endLatestTimestamp = 0;

                for (int index = 0; index < endedCount; index++) {
                    // Unsigned selection (C++ std::min/max on Uint32).
                    int start = touches[ended[index]].startTimestamp;
                    int end = touches[ended[index]].currentTimestamp;
                    if (ult(start, startEarliestTimestamp)) {
                        startEarliestTimestamp = start;
                    }
                    if (ugt(start, startLatestTimestamp)) {
                        startLatestTimestamp = start;
                    }
                    if (ult(end, endEarliestTimestamp)) {
                        endEarliestTimestamp = end;
                    }
                    if (ugt(end, endLatestTimestamp)) {
                        endLatestTimestamp = end;
                    }
                }

                if (usub(startLatestTimestamp, startEarliestTimestamp) <= TAP_MAXIMUM_DURATION
                        && usub(endLatestTimestamp, endEarliestTimestamp) <= TAP_MAXIMUM_DURATION) {
                    TouchLocation currentCentroid = currentLocationCentroid(ended, endedCount);

                    currentGesture.type = GestureType.kTap;
                    currentGesture.state = GestureState.kEnded;
                    currentGesture.numberOfTouches = endedCount;
                    currentGesture.x = currentCentroid.x;
                    currentGesture.y = currentCentroid.y;
                    gestureEventsQueue.push(copyOf(currentGesture));

                    // Reset tap gesture immediately.
                    currentGesture.type = GestureType.kUnrecognized;
                }
            } else if (activeCount != 0 && endedCount == 0) {
                TouchLocation startCentroid = startLocationCentroid(active, activeCount);
                TouchLocation currentCentroid = currentLocationCentroid(active, activeCount);

                // Disambiguate between pan and long press.
                if (Math.abs(currentCentroid.x - startCentroid.x) >= PAN_MINIMUM_MOVEMENT
                        || Math.abs(currentCentroid.y - startCentroid.y) >= PAN_MINIMUM_MOVEMENT) {
                    currentGesture.type = GestureType.kPan;
                    currentGesture.state = GestureState.kBegan;
                    currentGesture.numberOfTouches = activeCount;
                    currentGesture.x = currentCentroid.x;
                    currentGesture.y = currentCentroid.y;
                    gestureEventsQueue.push(copyOf(currentGesture));
                } else if ((int) Ticks.getTicks() - touches[active[0]].startTimestamp
                        >= LONG_PRESS_MINIMUM_DURATION) {
                    currentGesture.type = GestureType.kLongPress;
                    currentGesture.state = GestureState.kBegan;
                    currentGesture.numberOfTouches = activeCount;
                    currentGesture.x = currentCentroid.x;
                    currentGesture.y = currentCentroid.y;
                    gestureEventsQueue.push(copyOf(currentGesture));
                }

                if (useTouchscreenMode) {
                    Mouse.mouseHideCursor();
                    Mouse.mouseSetPosition(currentCentroid.x, currentCentroid.y);
                    Mouse.mouseShowCursor();
                }
            }
        }
    }

    /** Port of {@code touch_get_gesture} (LIFO stack, like C++ std::stack). */
    public static boolean touchGetGesture(Gesture gesture) {
        if (gestureEventsQueue.isEmpty()) {
            return false;
        }

        Gesture top = gestureEventsQueue.pop();
        gesture.type = top.type;
        gesture.state = top.state;
        gesture.numberOfTouches = top.numberOfTouches;
        gesture.x = top.x;
        gesture.y = top.y;
        return true;
    }

    /** Port of {@code touch_set_touchscreen_mode}. */
    public static void touchSetTouchscreenMode(boolean value) {
        useTouchscreenMode = value;
    }

    /** Port of {@code touch_get_touchscreen_mode}. */
    public static boolean touchGetTouchscreenMode() {
        return useTouchscreenMode;
    }

    /** Port of {@code touch_set_pan_mode}. */
    public static void touchSetPanMode(boolean value) {
        usePanMode = value;
    }

    /** Port of {@code touch_get_pan_mode}. */
    public static boolean touchGetPanMode() {
        return usePanMode;
    }

    /** Test seam: fresh touch state. */
    static void resetForTests() {
        for (int index = 0; index < touches.length; index++) {
            touches[index] = new TouchPoint();
        }
        currentGesture.type = GestureType.kUnrecognized;
        currentGesture.state = GestureState.kPossible;
        currentGesture.numberOfTouches = 0;
        currentGesture.x = 0;
        currentGesture.y = 0;
        gestureEventsQueue.clear();
        useTouchscreenMode = false;
        usePanMode = false;
    }
}
