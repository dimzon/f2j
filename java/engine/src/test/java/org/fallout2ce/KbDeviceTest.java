package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [kbfull]} behavior vectors from
 * {@code docker/refharness/golden/v31.txt}, plus unit coverage for the
 * paths the harness cannot reach (peek arithmetic, numpad-disabled seam,
 * lock-status seam, unreachable layout builders).
 */
class KbDeviceTest {

    @BeforeEach
    void init() {
        Kb.resetForTests();
        assertEquals(0, Kb.keyboardInit());
    }

    @AfterEach
    void reset() {
        Kb.resetForTests();
    }

    static void press(int scancode) {
        Kb.kbSimulateKey(new DInput.KeyboardData(scancode, true));
    }

    static void release(int scancode) {
        Kb.kbSimulateKey(new DInput.KeyboardData(scancode, false));
    }

    @Test
    void initLayoutAndDisable() {
        // init=0 init-again=-1 layout=0.
        assertEquals(-1, Kb.keyboardInit());
        assertEquals(Kb.KEYBOARD_LAYOUT_QWERTY, Kb.keyboardGetLayout());

        // Unknown layout restores the old one.
        Kb.keyboardSetLayout(99);
        assertEquals(Kb.KEYBOARD_LAYOUT_QWERTY, Kb.keyboardGetLayout());

        Kb.keyboardDisable();
        assertTrue(Kb.keyboardIsDisabled());
        Kb.keyboardEnable();
        assertFalse(Kb.keyboardIsDisabled());
    }

    @Test
    void pressCountWraps() {
        // 297 distinct presses (lock keys skipped) -> 297 & 0xFF = 41.
        for (int sc = 0; sc < 300; sc++) {
            if (sc == SdlScancode.CAPSLOCK || sc == SdlScancode.NUMLOCKCLEAR
                    || sc == SdlScancode.SCROLLLOCK) {
                continue;
            }
            press(sc);
        }
        assertEquals(41, Kb.pressedPhysicalKeysCount());
        for (int sc = 0; sc < 300; sc++) {
            if (sc == SdlScancode.CAPSLOCK || sc == SdlScancode.NUMLOCKCLEAR
                    || sc == SdlScancode.SCROLLLOCK) {
                continue;
            }
            release(sc);
        }
        assertEquals(0, Kb.pressedPhysicalKeysCount());
    }

    @Test
    void basicPressAndShift() {
        Kb.keyboardReset();
        press(SdlScancode.A);
        assertEquals(97, Kb.kbGetch());
        assertEquals(-1, Kb.kbGetch());
        release(SdlScancode.A);

        press(SdlScancode.LSHIFT);
        press(SdlScancode.A);
        assertEquals(-1, Kb.kbGetch());
        assertEquals(65, Kb.kbGetch());
        release(SdlScancode.A);
        release(SdlScancode.LSHIFT);
    }

    @Test
    void functionAndAltKeys() {
        press(SdlScancode.F1);
        assertEquals(315, Kb.kbGetch());
        release(SdlScancode.F1);

        press(SdlScancode.LALT);
        press(SdlScancode.X);
        assertEquals(-1, Kb.kbGetch());
        assertEquals(301, Kb.kbGetch());
        release(SdlScancode.X);
        release(SdlScancode.LALT);
    }

    @Test
    void capsLockToggle() {
        press(SdlScancode.CAPSLOCK);
        assertEquals(-1, Kb.kbGetch());
        release(SdlScancode.CAPSLOCK);

        press(SdlScancode.A);
        assertEquals(65, Kb.kbGetch());
        release(SdlScancode.A);

        press(SdlScancode.LSHIFT);
        press(SdlScancode.A);
        assertEquals(-1, Kb.kbGetch());
        assertEquals(97, Kb.kbGetch());
        release(SdlScancode.A);
        release(SdlScancode.LSHIFT);

        press(SdlScancode.CAPSLOCK);
        assertEquals(-1, Kb.kbGetch());
        release(SdlScancode.CAPSLOCK);

        press(SdlScancode.A);
        assertEquals(97, Kb.kbGetch());
        release(SdlScancode.A);
    }

    @Test
    void queueFullDrops() {
        Kb.keyboardReset();
        for (int i = 0; i < 70; i++) {
            press(SdlScancode.A);
        }
        int matches = 0;
        for (int i = 0; i < 63; i++) {
            if (Kb.kbGetch() == 97) {
                matches++;
            }
        }
        assertEquals(63, matches);
        assertEquals(-1, Kb.kbGetch());
        release(SdlScancode.A);
    }

    @Test
    void resetClearsAndFreeDisables() {
        press(SdlScancode.A);
        Kb.keyboardReset();
        assertEquals(0, Kb.pressedPhysicalKeysCount());
        assertEquals(-1, Kb.kbGetch());

        Kb.keyboardFree();
        assertEquals(-1, Kb.kbGetch());
        // Reset while uninstalled still flushes device state.
        Kb.keyboardReset();
    }

    @Test
    void peekArithmetic() {
        Kb.keyboardReset();
        assertNull(Kb.peekEvent(0));

        press(SdlScancode.A);
        press(SdlScancode.B);
        press(SdlScancode.C);
        Kb.KeyboardEvent first = Kb.peekEvent(0);
        Kb.KeyboardEvent third = Kb.peekEvent(2);
        assertEquals(SdlScancode.A, first.scanCode);
        assertEquals(SdlScancode.C, third.scanCode);
        assertNull(Kb.peekEvent(3));
        // Peeking does not consume.
        assertEquals(97, Kb.kbGetch());
        release(SdlScancode.A);
        release(SdlScancode.B);
        release(SdlScancode.C);
    }

    @Test
    void numpadDisabledDrops() {
        Kb.setNumpadDisabledForTests(true);
        try {
            press(SdlScancode.KP_5);
            assertEquals(-1, Kb.kbGetch());
            assertEquals(-1, Kb.kbGetch());
            release(SdlScancode.KP_5);

            press(SdlScancode.KP_DIVIDE);
            assertEquals(-1, Kb.kbGetch());
            release(SdlScancode.KP_DIVIDE);
        } finally {
            Kb.setNumpadDisabledForTests(false);
        }

        press(SdlScancode.KP_5);
        assertEquals(Kb.KEY_NUMBERPAD_5, Kb.kbGetch());
        release(SdlScancode.KP_5);
    }

    @Test
    void lockStatusSeam() {
        Kb.resetForTests();
        assertEquals(0, Kb.keyboardInit());
        Kb.kbInitLockStatus(SdlScancode.KMOD_CAPS | SdlScancode.KMOD_NUM);
        press(SdlScancode.A);
        // Caps on, no shift -> shift added -> uppercase.
        assertEquals(65, Kb.kbGetch());
        release(SdlScancode.A);
    }

    @Test
    void outOfRangeScancodeIgnored() {
        Kb.keyboardReset();
        Kb.kbSimulateKey(new DInput.KeyboardData(-1, true));
        Kb.kbSimulateKey(new DInput.KeyboardData(512, true));
        Kb.kbSimulateKey(new DInput.KeyboardData(9999, true));
        assertEquals(0, Kb.pressedPhysicalKeysCount());
        assertEquals(-1, Kb.kbGetch());
    }

    @Test
    void gnw95TimestampsCleared() {
        // keyboardReset flushes the timestamp table (no stale state).
        Kb.keyboardReset();
        Input.gnw95ClearTimeStamps();
    }

    @Test
    void frenchBuilderOverrides() {
        // Unreachable via setLayout (like C++); tables verified mechanically.
        Kb.setKeyboardLayoutForTests(Kb.KEYBOARD_LAYOUT_FRENCH);
        try {
            Kb.buildFrenchConfiguration();
            // Order check: the override wins over the QWERTY base.
            press(SdlScancode.N2);
            assertEquals(233, Kb.kbGetch());
            release(SdlScancode.N2);
        } finally {
            Kb.resetForTests();
            Kb.keyboardInit();
        }
    }

    @Test
    void germanBuilderOverrides() {
        Kb.setKeyboardLayoutForTests(Kb.KEYBOARD_LAYOUT_GERMAN);
        try {
            Kb.buildGermanConfiguration();
            press(SdlScancode.MINUS);
            assertEquals(223, Kb.kbGetch());
            release(SdlScancode.MINUS);
        } finally {
            Kb.resetForTests();
            Kb.keyboardInit();
        }
    }

    @Test
    void italianAndSpanishBuilders() {
        Kb.setKeyboardLayoutForTests(Kb.KEYBOARD_LAYOUT_ITALIAN);
        Kb.buildItalianConfiguration();
        press(SdlScancode.LSHIFT);
        press(SdlScancode.N3);
        assertEquals(-1, Kb.kbGetch());
        assertEquals(163, Kb.kbGetch());
        release(SdlScancode.N3);
        release(SdlScancode.LSHIFT);

        Kb.setKeyboardLayoutForTests(Kb.KEYBOARD_LAYOUT_SPANISH);
        Kb.buildSpanishConfiguration();
        press(SdlScancode.LSHIFT);
        press(SdlScancode.N3);
        assertEquals(-1, Kb.kbGetch());
        assertEquals(149, Kb.kbGetch());
        release(SdlScancode.N3);
        release(SdlScancode.LSHIFT);

        Kb.resetForTests();
        Kb.keyboardInit();
    }
}
