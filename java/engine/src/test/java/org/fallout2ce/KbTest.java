package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Key vocabulary from {@code docker/refharness/golden/v24.txt}
 * ({@code [kb]}) plus header cross-checks.
 */
class KbTest {

    @Test
    void controlKeys() {
        assertEquals(27, Kb.KEY_ESCAPE);
        assertEquals(9, Kb.KEY_TAB);
        assertEquals(8, Kb.KEY_BACKSPACE);
        assertEquals(13, Kb.KEY_RETURN);
        assertEquals(32, Kb.KEY_SPACE);
        assertEquals(127, Kb.KEY_DEL);
    }

    @Test
    void asciiLettersAndDigits() {
        assertEquals(97, Kb.KEY_LOWERCASE_A);
        assertEquals(122, Kb.KEY_LOWERCASE_Z);
        assertEquals(65, Kb.KEY_UPPERCASE_A);
        assertEquals(90, Kb.KEY_UPPERCASE_Z);
        assertEquals(48, Kb.KEY_0);
        assertEquals(57, Kb.KEY_9);
    }

    @Test
    void functionKeys() {
        assertEquals(315, Kb.KEY_F1);
        assertEquals(324, Kb.KEY_F10);
        assertEquals(389, Kb.KEY_F11);
        assertEquals(390, Kb.KEY_F12);
        assertEquals(340, Kb.KEY_SHIFT_F1);
        assertEquals(350, Kb.KEY_CTRL_F1);
        assertEquals(360, Kb.KEY_ALT_F1);
    }

    @Test
    void navigationKeys() {
        assertEquals(328, Kb.KEY_ARROW_UP);
        assertEquals(336, Kb.KEY_ARROW_DOWN);
        assertEquals(331, Kb.KEY_ARROW_LEFT);
        assertEquals(333, Kb.KEY_ARROW_RIGHT);
        assertEquals(397, Kb.KEY_CTRL_ARROW_UP);
        assertEquals(408, Kb.KEY_ALT_ARROW_UP);
        assertEquals(327, Kb.KEY_HOME);
        assertEquals(335, Kb.KEY_END);
        assertEquals(329, Kb.KEY_PAGE_UP);
        assertEquals(337, Kb.KEY_PAGE_DOWN);
        assertEquals(338, Kb.KEY_INSERT);
    }

    @Test
    void altAndCtrlLetters() {
        assertEquals(302, Kb.KEY_ALT_C);
        assertEquals(3, Kb.KEY_CTRL_C);
        assertEquals(301, Kb.KEY_ALT_X);
        assertEquals(1, Kb.KEY_CTRL_A);
        assertEquals(306, Kb.KEY_ALT_M);
        assertEquals(332, Kb.KEY_NUMBERPAD_5);
    }

    @Test
    void rangesAndSizes() {
        assertEquals(32, Kb.KEY_FIRST_INPUT_CHARACTER);
        assertEquals(122, Kb.KEY_LAST_INPUT_CHARACTER);
        assertEquals(64, Kb.KEY_QUEUE_SIZE);
    }

    @Test
    void modifiers() {
        assertEquals(8, Kb.KEYBOARD_EVENT_MODIFIER_LEFT_SHIFT);
        assertEquals(128, Kb.KEYBOARD_EVENT_MODIFIER_LEFT_CONTROL);
        assertEquals(32, Kb.KEYBOARD_EVENT_MODIFIER_LEFT_ALT);
        assertEquals(24, Kb.KEYBOARD_EVENT_MODIFIER_ANY_SHIFT);
        assertEquals(96, Kb.KEYBOARD_EVENT_MODIFIER_ANY_ALT);
        assertEquals(384, Kb.KEYBOARD_EVENT_MODIFIER_ANY_CONTROL);
        assertEquals(1, Kb.MODIFIER_KEY_STATE_NUM_LOCK);
        assertEquals(2, Kb.MODIFIER_KEY_STATE_CAPS_LOCK);
        assertEquals(0, Kb.KEY_STATE_UP);
        assertEquals(1, Kb.KEY_STATE_DOWN);
        assertEquals(2, Kb.KEY_STATE_REPEAT);
    }

    @Test
    void layouts() {
        assertEquals(0, Kb.KEYBOARD_LAYOUT_QWERTY);
        assertEquals(1, Kb.KEYBOARD_LAYOUT_FRENCH);
        assertEquals(2, Kb.KEYBOARD_LAYOUT_GERMAN);
        assertEquals(3, Kb.KEYBOARD_LAYOUT_ITALIAN);
        assertEquals(4, Kb.KEYBOARD_LAYOUT_SPANISH);
    }
}
