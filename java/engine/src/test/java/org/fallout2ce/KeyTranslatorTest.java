package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Canvas;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Synthetic AWT events (headless-safe, no window) through
 * {@link KeyTranslator}, ending in the {@link Input} queue.
 */
class KeyTranslatorTest {

    private final Canvas source = new Canvas();

    @AfterEach
    void reset() {
        Input.resetForTests();
    }

    static KeyEvent key(Canvas source, int id, int modifiers, int keyCode, char keyChar) {
        return new KeyEvent(source, id, System.currentTimeMillis(), modifiers, keyCode, keyChar);
    }

    @Test
    void printableCharsPassThrough() {
        assertEquals(97, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_A, 'a')));
        assertEquals(65, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_A, 'A')));
        assertEquals(48, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_0, '0')));
        assertEquals(33, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_1, '!')));
        assertEquals(127, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_DELETE, (char) 127)));
    }

    @Test
    void controlCodesPassThrough() {
        assertEquals(1, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_A, (char) 1)));
        assertEquals(3, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_C, (char) 3)));
        assertEquals(13, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_ENTER, '\n')));
    }

    @Test
    void functionKeys() {
        assertEquals(315, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_F1, KeyEvent.CHAR_UNDEFINED)));
        assertEquals(340, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_F1,
            KeyEvent.CHAR_UNDEFINED)));
        assertEquals(354, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_F5,
            KeyEvent.CHAR_UNDEFINED)));
        assertEquals(396, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_F12,
            KeyEvent.CHAR_UNDEFINED)));
    }

    @Test
    void navigationKeys() {
        assertEquals(328, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED)));
        assertEquals(397, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_UP,
            KeyEvent.CHAR_UNDEFINED)));
        assertEquals(408, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_UP,
            KeyEvent.CHAR_UNDEFINED)));
        assertEquals(327, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_HOME, KeyEvent.CHAR_UNDEFINED)));
        assertEquals(339, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED)));
        assertEquals(27, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED)));
        assertEquals(9, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED)));
        assertEquals(13, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED)));
    }

    @Test
    void altLetters() {
        assertEquals(302, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_C,
            KeyEvent.CHAR_UNDEFINED)));
        assertEquals(272, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_Q,
            KeyEvent.CHAR_UNDEFINED)));
    }

    @Test
    void numpadMapsToSymbols() {
        assertEquals(48, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_NUMPAD0, KeyEvent.CHAR_UNDEFINED)));
        assertEquals(42, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_MULTIPLY, KeyEvent.CHAR_UNDEFINED)));
    }

    @Test
    void unknownIsDropped() {
        assertEquals(-1, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_CAPS_LOCK, KeyEvent.CHAR_UNDEFINED)));
        assertEquals(-1, KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED)));
    }

    @Test
    void queueIntegration() {
        Input.enqueueInputEvent(KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED)));
        assertEquals(328, Input.dequeueInputEvent());

        // Unknown keys never reach the queue.
        Input.enqueueInputEvent(KeyTranslator.toLogicalKey(key(source,
            KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_CAPS_LOCK, KeyEvent.CHAR_UNDEFINED)));
        assertEquals(-1, Input.dequeueInputEvent());
    }
}
