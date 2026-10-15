package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.Touch.Gesture;
import org.fallout2ce.Touch.GestureState;
import org.fallout2ce.Touch.GestureType;

/**
 * {@code [mousewin]} touch vectors from
 * {@code docker/refharness/golden/v38.txt} (long-press covered there;
 * here tap, pan and modes).
 */
class TouchTest {

    private int savedLeft;
    private int savedTop;
    private int savedRight;
    private int savedBottom;

    @BeforeEach
    void setup() {
        Touch.resetForTests();

        savedLeft = Svga.scrSize.left;
        savedTop = Svga.scrSize.top;
        savedRight = Svga.scrSize.right;
        savedBottom = Svga.scrSize.bottom;
        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;
    }

    @AfterEach
    void teardown() {
        Touch.resetForTests();

        Svga.scrSize.left = savedLeft;
        Svga.scrSize.top = savedTop;
        Svga.scrSize.right = savedRight;
        Svga.scrSize.bottom = savedBottom;
    }

    @Test
    void modes() {
        Touch.touchSetTouchscreenMode(true);
        assertTrue(Touch.touchGetTouchscreenMode());
        Touch.touchSetPanMode(true);
        assertTrue(Touch.touchGetPanMode());
        Touch.touchSetTouchscreenMode(false);
        assertFalse(Touch.touchGetTouchscreenMode());
        Touch.touchSetPanMode(false);
        assertFalse(Touch.touchGetPanMode());
    }

    @Test
    void tap() {
        Touch.touchHandleStart(1, 1000, 0.1f, 0.2f);
        Touch.touchHandleEnd(1, 1050, 0.1f, 0.2f);
        Touch.touchProcessGesture();

        Gesture gesture = new Gesture();
        assertTrue(Touch.touchGetGesture(gesture));
        assertEquals(GestureType.kTap, gesture.type);
        assertEquals(GestureState.kEnded, gesture.state);
        assertEquals(1, gesture.numberOfTouches);
        assertEquals(64, gesture.x);
        assertEquals(96, gesture.y);
        assertFalse(Touch.touchGetGesture(gesture));
    }

    @Test
    void pan() {
        Touch.touchHandleStart(2, 2000, 0.5f, 0.5f);
        Touch.touchHandleMove(2, 2100, 0.52f, 0.5f);
        Touch.touchProcessGesture();

        Gesture gesture = new Gesture();
        assertTrue(Touch.touchGetGesture(gesture));
        assertEquals(GestureType.kPan, gesture.type);
        assertEquals(GestureState.kBegan, gesture.state);
        assertEquals(1, gesture.numberOfTouches);
        assertEquals(332, gesture.x);
        assertEquals(240, gesture.y);

        Touch.touchHandleEnd(2, 2200, 0.52f, 0.5f);
        Touch.touchProcessGesture();

        Gesture end = new Gesture();
        assertTrue(Touch.touchGetGesture(end));
        assertEquals(GestureType.kPan, end.type);
        assertEquals(GestureState.kEnded, end.state);
    }

    @Test
    void unknownFingerIgnored() {
        Touch.touchHandleMove(9, 1000, 0.5f, 0.5f);
        Touch.touchHandleEnd(9, 1000, 0.5f, 0.5f);
        Touch.touchProcessGesture();
        assertFalse(Touch.touchGetGesture(new Gesture()));
    }
}
