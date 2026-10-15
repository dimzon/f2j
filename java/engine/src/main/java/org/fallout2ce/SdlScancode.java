package org.fallout2ce;

/**
 * SDL scancode ids used by the keyboard driver ({@code src/kb.cc}), mirroring
 * {@code SDL_scancode.h}. Values are pinned by {@code [kbfull]} in
 * {@code docker/refharness/golden/v31.txt} (notably {@code KP_DECIMAL=220}
 * and {@code KMOD_SCROLL=32768} from the SDL 2.0.18+ headers).
 */
public final class SdlScancode {

    private SdlScancode() {
    }

    public static final int NUM_SCANCODES = 512;

    public static final int UNKNOWN = 0;

    public static final int KMOD_CAPS = 8192;
    public static final int KMOD_NUM = 4096;
    public static final int KMOD_SCROLL = 32768;

    public static final int A = 4;
    public static final int B = 5;
    public static final int C = 6;
    public static final int D = 7;
    public static final int E = 8;
    public static final int F = 9;
    public static final int G = 10;
    public static final int H = 11;
    public static final int I = 12;
    public static final int J = 13;
    public static final int K = 14;
    public static final int L = 15;
    public static final int M = 16;
    public static final int N = 17;
    public static final int O = 18;
    public static final int P = 19;
    public static final int Q = 20;
    public static final int R = 21;
    public static final int S = 22;
    public static final int T = 23;
    public static final int U = 24;
    public static final int V = 25;
    public static final int W = 26;
    public static final int X = 27;
    public static final int Y = 28;
    public static final int Z = 29;

    public static final int N1 = 30;
    public static final int N2 = 31;
    public static final int N3 = 32;
    public static final int N4 = 33;
    public static final int N5 = 34;
    public static final int N6 = 35;
    public static final int N7 = 36;
    public static final int N8 = 37;
    public static final int N9 = 38;
    public static final int N0 = 39;

    public static final int RETURN = 40;
    public static final int ESCAPE = 41;
    public static final int BACKSPACE = 42;
    public static final int TAB = 43;
    public static final int SPACE = 44;
    public static final int MINUS = 45;
    public static final int EQUALS = 46;
    public static final int LEFTBRACKET = 47;
    public static final int RIGHTBRACKET = 48;
    public static final int BACKSLASH = 49;
    public static final int SEMICOLON = 51;
    public static final int APOSTROPHE = 52;
    public static final int GRAVE = 53;
    public static final int COMMA = 54;
    public static final int PERIOD = 55;
    public static final int SLASH = 56;
    public static final int CAPSLOCK = 57;

    public static final int F1 = 58;
    public static final int F2 = 59;
    public static final int F3 = 60;
    public static final int F4 = 61;
    public static final int F5 = 62;
    public static final int F6 = 63;
    public static final int F7 = 64;
    public static final int F8 = 65;
    public static final int F9 = 66;
    public static final int F10 = 67;
    public static final int F11 = 68;
    public static final int F12 = 69;

    public static final int SCROLLLOCK = 71;
    public static final int INSERT = 73;
    public static final int HOME = 74;
    public static final int PAGEUP = 75;
    public static final int PRIOR = 75;
    public static final int DELETE = 76;
    public static final int END = 77;
    public static final int PAGEDOWN = 78;
    public static final int RIGHT = 79;
    public static final int LEFT = 80;
    public static final int DOWN = 81;
    public static final int UP = 82;
    public static final int NUMLOCKCLEAR = 83;

    public static final int KP_DIVIDE = 84;
    public static final int KP_MULTIPLY = 85;
    public static final int KP_MINUS = 86;
    public static final int KP_PLUS = 87;
    public static final int KP_ENTER = 88;
    public static final int KP_1 = 89;
    public static final int KP_2 = 90;
    public static final int KP_3 = 91;
    public static final int KP_4 = 92;
    public static final int KP_5 = 93;
    public static final int KP_6 = 94;
    public static final int KP_7 = 95;
    public static final int KP_8 = 96;
    public static final int KP_9 = 97;
    public static final int KP_0 = 98;
    public static final int KP_DECIMAL = 220;

    public static final int APPLICATION = 101;
    public static final int KP_EQUALS = 103;
    public static final int F13 = 104;
    public static final int F14 = 105;
    public static final int F15 = 106;
    public static final int STOP = 120;
    public static final int KP_COMMA = 133;

    public static final int LCTRL = 224;
    public static final int LSHIFT = 225;
    public static final int LALT = 226;
    public static final int LGUI = 227;
    public static final int RCTRL = 228;
    public static final int RSHIFT = 229;
    public static final int RALT = 230;
    public static final int RGUI = 231;
}
