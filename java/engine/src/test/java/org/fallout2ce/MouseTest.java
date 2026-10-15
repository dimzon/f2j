package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.Geometry.Rect;

/**
 * {@code [mousewin]} mouse vectors from
 * {@code docker/refharness/golden/v38.txt}.
 */
class MouseTest {

    private byte[] savedColorTable;
    private int savedLeft;
    private int savedTop;
    private int savedRight;
    private int savedBottom;

    @BeforeEach
    void setup() {
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        WindowManager.resetForTests();

        savedColorTable = Color.colorTable.clone();
        for (int i = 0; i < Color.colorTable.length; i++) {
            Color.colorTable[i] = (byte) i;
        }

        savedLeft = Svga.scrSize.left;
        savedTop = Svga.scrSize.top;
        savedRight = Svga.scrSize.right;
        savedBottom = Svga.scrSize.bottom;
        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;

        Mouse.mouseBlit = (src, srcPitch, unused, srcX, srcY, srcWidth, srcHeight, destX, destY) -> {
        };
    }

    @AfterEach
    void teardown() {
        Mouse.resetForTests();
        Kb.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        WindowManager.resetForTests();
        System.arraycopy(savedColorTable, 0, Color.colorTable, 0, savedColorTable.length);

        Svga.scrSize.left = savedLeft;
        Svga.scrSize.top = savedTop;
        Svga.scrSize.right = savedRight;
        Svga.scrSize.bottom = savedBottom;
        Mouse.mouseBlit = null;
    }

    @Test
    void initShowMoveClick() {
        assertEquals(0, Mouse.mouseInit());
        Mouse.mouseShowCursor();
        assertFalse(Mouse.cursorIsHidden());

        Mouse.mouseSetPosition(100, 80);
        int[] pos = new int[2];
        Mouse.mouseGetPosition(pos);
        assertEquals(100, pos[0]);
        assertEquals(80, pos[1]);

        Rect rect = new Rect();
        Mouse.mouseGetRect(rect);
        assertEquals(99, rect.left);
        assertEquals(79, rect.top);
        assertEquals(106, rect.right);
        assertEquals(86, rect.bottom);

        assertTrue(Mouse.mouseIn(0, 0, 640, 480));
        assertFalse(Mouse.mouseIn(0, 0, 10, 10));
        assertTrue(Mouse.mouseClickIn(90, 70, 110, 90));
        assertFalse(Mouse.mouseClickIn(0, 0, 10, 10));
    }

    @Test
    void buttonDownRepeatUp() {
        assertEquals(0, Mouse.mouseInit());
        Mouse.mouseShowCursor();

        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        assertEquals(1, Mouse.mouseGetEvent());
        assertEquals(1, Mouse.mouseGetLastButtons());

        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        assertEquals(4, Mouse.mouseGetEvent());

        Mouse.mouseSimulateInput(0, 0, 0);
        assertEquals(16, Mouse.mouseGetEvent());
    }

    @Test
    void wheelToArrowKey() {
        assertEquals(0, Mouse.mouseInit());
        Mouse.mouseShowCursor();

        int[] wheel = new int[2];
        Mouse.mouseGetWheel(wheel);
        assertEquals(0, wheel[0]);
        assertEquals(0, wheel[1]);

        int[] key = { -1 };
        Mouse.convertMouseWheelToArrowKey(key);
        assertEquals(-1, key[0]);

        DInput.addMouseWheel(0, 1);
        Mouse.mouseInfo();
        assertEquals(64, Mouse.mouseGetEvent());
        Mouse.convertMouseWheelToArrowKey(key);
        assertEquals(Kb.KEY_ARROW_UP, key[0]);

        DInput.addMouseWheel(0, -1);
        Mouse.mouseInfo();
        key[0] = -1;
        Mouse.convertMouseWheelToArrowKey(key);
        assertEquals(Kb.KEY_ARROW_DOWN, key[0]);
    }

    @Test
    void hideShowAndRaw() {
        assertEquals(0, Mouse.mouseInit());
        Mouse.mouseShowCursor();
        Mouse.mouseHideCursor();
        assertTrue(Mouse.cursorIsHidden());
        Mouse.mouseShowCursor();
        assertFalse(Mouse.cursorIsHidden());

        int[] raw = new int[3];
        Mouse.mouseGetRawState(raw);
        assertEquals(0, raw[2]);

        int[] pos = new int[2];
        Mouse.mouseGetPositionInWindow(999, pos);

        assertFalse(Mouse.mouseHitTestInWindow(999, 0, 0, 10, 10));
    }

    @Test
    void freeKeepsInitialized() {
        assertEquals(0, Mouse.mouseInit());
        Mouse.mouseShowCursor();
        Mouse.mouseFree();
        // C++ quirk: free does not clear the initialized flag.
        Mouse.mouseSimulateInput(0, 0, Mouse.STATE_LEFT_BUTTON_DOWN);
        assertEquals(1, Mouse.mouseGetEvent());
    }
}
