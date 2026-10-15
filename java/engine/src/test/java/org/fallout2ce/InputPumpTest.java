package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Input init/pump vectors: init/exit lifecycle, normalized table samples,
 * key processing, message dispatch (keys, fingers, window, quit), hooks.
 */
class InputPumpTest {

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
        Touch.resetForTests();
        WindowManager.resetForTests();

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
        Input.resetForTests();
        Kb.resetForTests();
        Mouse.resetForTests();
        DInput.resetForTests();
        Touch.resetForTests();
        WindowManager.resetForTests();

        Svga.scrSize.left = savedLeft;
        Svga.scrSize.top = savedTop;
        Svga.scrSize.right = savedRight;
        Svga.scrSize.bottom = savedBottom;
        Mouse.mouseBlit = null;
    }

    @Test
    void initExit() {
        assertEquals(0, Input.inputInit());
        assertEquals(-1, Input.inputInit());
        Input.inputExit();
        assertEquals(0, Input.inputInit());
        Input.inputExit();
    }

    @Test
    void normalizedTable() {
        Input.buildNormalizedQwertyKeys();
        assertEquals(SdlScancode.A, Input.normalizedKey(SdlScancode.A));
        assertEquals(SdlScancode.MINUS, Input.normalizedKey(SdlScancode.MINUS));
        assertEquals(SdlScancode.EQUALS, Input.normalizedKey(SdlScancode.EQUALS));
        assertEquals(SdlScancode.Q, Input.normalizedKey(SdlScancode.Q));
        assertEquals(SdlScancode.GRAVE, Input.normalizedKey(SdlScancode.GRAVE));
        assertEquals(-1, Input.normalizedKey(SdlScancode.F13));
        assertEquals(-1, Input.normalizedKey(SdlScancode.LGUI));
        assertEquals(SdlScancode.PRIOR, Input.normalizedKey(SdlScancode.PRIOR));
    }

    @Test
    void frenchLayoutSample() {
        Kb.keyboardInit();
        Kb.setKeyboardLayoutForTests(Kb.KEYBOARD_LAYOUT_FRENCH);
        try {
            Input.buildNormalizedQwertyKeys();
            assertEquals(SdlScancode.N6, Input.normalizedKey(SdlScancode.MINUS));
            assertEquals(SdlScancode.A, Input.normalizedKey(SdlScancode.Q));
        } finally {
            Kb.resetForTests();
        }
    }

    @Test
    void processKeyPressRelease() {
        assertEquals(0, Input.inputInit());
        Input.buildNormalizedQwertyKeys();

        Input.gnw95ProcessKey(SdlScancode.A, true);
        assertEquals(97, Kb.kbGetch());
        Input.gnw95ProcessKey(SdlScancode.A, false);
        assertEquals(-1, Kb.kbGetch());

        // Remapped to -1: no event.
        Input.gnw95ProcessKey(SdlScancode.F13, true);
        assertEquals(-1, Kb.kbGetch());

        // Out of range: ignored, no crash.
        Input.gnw95ProcessKey(-1, true);
        Input.gnw95ProcessKey(512, true);
        Input.gnw95ProcessKey(9999, true);
        assertEquals(-1, Kb.kbGetch());
    }

    @Test
    void messageKeyFlow() {
        assertEquals(0, Input.inputInit());
        Input.buildNormalizedQwertyKeys();
        Input.programIsActive = true;

        Input.postKeyEvent(SdlScancode.B, true);
        Input.postKeyEvent(SdlScancode.B, false);
        Input.gnw95ProcessMessage();
        assertEquals(98, Kb.kbGetch());
        assertEquals(-1, Kb.kbGetch());

        // Disabled keyboard: events ignored.
        Kb.keyboardDisable();
        try {
            Input.postKeyEvent(SdlScancode.C, true);
            Input.gnw95ProcessMessage();
            assertEquals(-1, Kb.kbGetch());
        } finally {
            Kb.keyboardEnable();
        }

        // Sfall override: A becomes F1.
        Input.sfallHandleKeyPressed = (key, down) -> SdlScancode.F1;
        try {
            Input.postKeyEvent(SdlScancode.A, true);
            Input.gnw95ProcessMessage();
            assertEquals(315, Kb.kbGetch());
        } finally {
            reinit();
        }

        // Sfall consume: skips the override but still processes the key.
        Input.sfallConsumeSyntheticKey = (key, down) -> true;
        Input.sfallHandleKeyPressed = (key, down) -> SdlScancode.F1;
        try {
            Input.postKeyEvent(SdlScancode.A, true);
            Input.gnw95ProcessMessage();
            assertEquals(97, Kb.kbGetch());
        } finally {
            reinit();
        }

        // UNKNOWN override: skipped.
        Input.sfallHandleKeyPressed = (key, down) -> SdlScancode.UNKNOWN;
        try {
            Input.postKeyEvent(SdlScancode.A, true);
            Input.gnw95ProcessMessage();
            assertEquals(-1, Kb.kbGetch());
        } finally {
            Input.resetForTests();
        }
    }

    @Test
    void keyRepeat() {
        assertEquals(0, Input.inputInit());
        Input.buildNormalizedQwertyKeys();
        Input.programIsActive = true;
        Input.setKeyboardKeyRepeatForTests(-1, -1);

        Input.postKeyEvent(SdlScancode.A, true);
        Input.gnw95ProcessMessage();
        assertEquals(97, Kb.kbGetch());
        assertEquals(97, Kb.kbGetch());
        Input.gnw95ProcessKey(SdlScancode.A, false);
    }

    @Test
    void messageFingers() {
        Touch.resetForTests();
        Input.postFingerEvent(1, 1000, 0.1f, 0.2f, Input.FINGER_BEGAN);
        Input.postFingerEvent(1, 1050, 0.1f, 0.2f, Input.FINGER_ENDED);
        Input.gnw95ProcessMessage();

        Touch.Gesture gesture = new Touch.Gesture();
        assertTrue(Touch.touchGetGesture(gesture));
        assertEquals(Touch.GestureType.kTap, gesture.type);
    }

    @Test
    void messageWindowAndQuit() {
        List<String> audio = new ArrayList<>();
        Input.audioResume = () -> audio.add("resume");
        Input.audioPause = () -> audio.add("pause");
        List<String> movies = new ArrayList<>();
        Input.movieFocusGained = () -> movies.add("focus");
        List<Boolean> quits = new ArrayList<>();
        Input.quitHandler = () -> quits.add(true);

        Input.postWindowEvent(Input.WINDOW_EXPOSED);
        Input.postWindowEvent(Input.WINDOW_SIZE_CHANGED);
        Input.postWindowEvent(Input.WINDOW_FOCUS_GAINED);
        Input.gnw95ProcessMessage();
        assertTrue(Input.programIsActive);
        assertEquals(List.of("focus"), movies);
        assertEquals(List.of("resume"), audio);

        Input.postWindowEvent(Input.WINDOW_FOCUS_LOST);
        Input.gnw95ProcessMessage();
        assertFalse(Input.programIsActive);
        assertEquals(List.of("resume", "pause"), audio);

        Input.programIsActive = true;
        Input.gnw95LostFocus();

        Input.postQuitEvent();
        Input.gnw95ProcessMessage();
        assertEquals(List.of(true), quits);

        Input.beginTextInput();
        Input.endTextInput();
    }

    @Test
    void hookButtonMapping() {        assertEquals(0, Input.inputGetHookMouseButton(1));
        assertEquals(1, Input.inputGetHookMouseButton(2));
        assertEquals(2, Input.inputGetHookMouseButton(3));
        assertEquals(3, Input.inputGetHookMouseButton(4));
        assertEquals(4, Input.inputGetHookMouseButton(5));
        assertEquals(5, Input.inputGetHookMouseButton(6));
        assertEquals(6, Input.inputGetHookMouseButton(7));
        assertEquals(7, Input.inputGetHookMouseButton(8));
        assertEquals(-1, Input.inputGetHookMouseButton(0));
        assertEquals(-1, Input.inputGetHookMouseButton(9));
    }

    static void reinit() {
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        assertEquals(0, Input.inputInit());
        Input.buildNormalizedQwertyKeys();
        Input.programIsActive = true;
    }
}
