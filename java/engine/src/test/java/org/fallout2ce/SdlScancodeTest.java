package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * SDL scancode values from {@code [kbfull]} in
 * {@code docker/refharness/golden/v31.txt} (real SDL headers).
 */
class SdlScancodeTest {

    @Test
    void numScancodes() {
        assertEquals(512, SdlScancode.NUM_SCANCODES);
    }

    @Test
    void letters() {
        assertEquals(4, SdlScancode.A);
        assertEquals(5, SdlScancode.B);
        assertEquals(6, SdlScancode.C);
        assertEquals(7, SdlScancode.D);
        assertEquals(8, SdlScancode.E);
        assertEquals(9, SdlScancode.F);
        assertEquals(10, SdlScancode.G);
        assertEquals(11, SdlScancode.H);
        assertEquals(12, SdlScancode.I);
        assertEquals(13, SdlScancode.J);
        assertEquals(14, SdlScancode.K);
        assertEquals(15, SdlScancode.L);
        assertEquals(16, SdlScancode.M);
        assertEquals(17, SdlScancode.N);
        assertEquals(18, SdlScancode.O);
        assertEquals(19, SdlScancode.P);
        assertEquals(20, SdlScancode.Q);
        assertEquals(21, SdlScancode.R);
        assertEquals(22, SdlScancode.S);
        assertEquals(23, SdlScancode.T);
        assertEquals(24, SdlScancode.U);
        assertEquals(25, SdlScancode.V);
        assertEquals(26, SdlScancode.W);
        assertEquals(27, SdlScancode.X);
        assertEquals(28, SdlScancode.Y);
        assertEquals(29, SdlScancode.Z);
    }

    @Test
    void digits() {
        assertEquals(30, SdlScancode.N1);
        assertEquals(31, SdlScancode.N2);
        assertEquals(32, SdlScancode.N3);
        assertEquals(33, SdlScancode.N4);
        assertEquals(34, SdlScancode.N5);
        assertEquals(35, SdlScancode.N6);
        assertEquals(36, SdlScancode.N7);
        assertEquals(37, SdlScancode.N8);
        assertEquals(38, SdlScancode.N9);
        assertEquals(39, SdlScancode.N0);
    }

    @Test
    void controlAndPunctuation() {
        assertEquals(40, SdlScancode.RETURN);
        assertEquals(41, SdlScancode.ESCAPE);
        assertEquals(42, SdlScancode.BACKSPACE);
        assertEquals(43, SdlScancode.TAB);
        assertEquals(44, SdlScancode.SPACE);
        assertEquals(45, SdlScancode.MINUS);
        assertEquals(46, SdlScancode.EQUALS);
        assertEquals(47, SdlScancode.LEFTBRACKET);
        assertEquals(48, SdlScancode.RIGHTBRACKET);
        assertEquals(49, SdlScancode.BACKSLASH);
        assertEquals(51, SdlScancode.SEMICOLON);
        assertEquals(52, SdlScancode.APOSTROPHE);
        assertEquals(53, SdlScancode.GRAVE);
        assertEquals(54, SdlScancode.COMMA);
        assertEquals(55, SdlScancode.PERIOD);
        assertEquals(56, SdlScancode.SLASH);
        assertEquals(57, SdlScancode.CAPSLOCK);
    }

    @Test
    void functionKeys() {
        assertEquals(58, SdlScancode.F1);
        assertEquals(59, SdlScancode.F2);
        assertEquals(60, SdlScancode.F3);
        assertEquals(61, SdlScancode.F4);
        assertEquals(62, SdlScancode.F5);
        assertEquals(63, SdlScancode.F6);
        assertEquals(64, SdlScancode.F7);
        assertEquals(65, SdlScancode.F8);
        assertEquals(66, SdlScancode.F9);
        assertEquals(67, SdlScancode.F10);
        assertEquals(68, SdlScancode.F11);
        assertEquals(69, SdlScancode.F12);
    }

    @Test
    void navigation() {
        assertEquals(73, SdlScancode.INSERT);
        assertEquals(74, SdlScancode.HOME);
        assertEquals(75, SdlScancode.PAGEUP);
        assertEquals(76, SdlScancode.DELETE);
        assertEquals(77, SdlScancode.END);
        assertEquals(78, SdlScancode.PAGEDOWN);
        assertEquals(79, SdlScancode.RIGHT);
        assertEquals(80, SdlScancode.LEFT);
        assertEquals(81, SdlScancode.DOWN);
        assertEquals(82, SdlScancode.UP);
        assertEquals(83, SdlScancode.NUMLOCKCLEAR);
        assertEquals(71, SdlScancode.SCROLLLOCK);
    }

    @Test
    void keypad() {
        assertEquals(84, SdlScancode.KP_DIVIDE);
        assertEquals(85, SdlScancode.KP_MULTIPLY);
        assertEquals(86, SdlScancode.KP_MINUS);
        assertEquals(87, SdlScancode.KP_PLUS);
        assertEquals(88, SdlScancode.KP_ENTER);
        assertEquals(89, SdlScancode.KP_1);
        assertEquals(90, SdlScancode.KP_2);
        assertEquals(91, SdlScancode.KP_3);
        assertEquals(92, SdlScancode.KP_4);
        assertEquals(93, SdlScancode.KP_5);
        assertEquals(94, SdlScancode.KP_6);
        assertEquals(95, SdlScancode.KP_7);
        assertEquals(96, SdlScancode.KP_8);
        assertEquals(97, SdlScancode.KP_9);
        assertEquals(98, SdlScancode.KP_0);
        assertEquals(220, SdlScancode.KP_DECIMAL);
    }

    @Test
    void modifiersAndKmod() {
        assertEquals(224, SdlScancode.LCTRL);
        assertEquals(225, SdlScancode.LSHIFT);
        assertEquals(226, SdlScancode.LALT);
        assertEquals(228, SdlScancode.RCTRL);
        assertEquals(229, SdlScancode.RSHIFT);
        assertEquals(230, SdlScancode.RALT);
        assertEquals(8192, SdlScancode.KMOD_CAPS);
        assertEquals(4096, SdlScancode.KMOD_NUM);
        assertEquals(32768, SdlScancode.KMOD_SCROLL);
    }
}
