package org.fallout2ce;

import java.util.Arrays;

/**
 * Port of the key vocabulary from {@code src/kb.h} (key codes, modifiers,
 * layouts) and the keyboard queue/layout engine from {@code src/kb.cc}.
 *
 * <p>Event source: {@link #kbSimulateKey(DInput.KeyboardData)} (fed by the
 * input pump, like C++); the SDL event pump itself lives behind the platform
 * SPI. Out-of-range scancodes are ignored (C++ indexes the pressed-state
 * array unchecked, which is UB). {@code field_0} of
 * {@code LogicalKeyEntry} is write-only in C++ and dropped.
 *
 * <p>Only {@code QWERTY} is selectable (other layouts are commented out in
 * {@code keyboardSetLayout}, like C++); the French/German/Italian/Spanish
 * table builders are ported for future use.
 */
public final class Kb {

    private Kb() {
    }

    public static final int KEY_STATE_UP = 0;
    public static final int KEY_STATE_DOWN = 1;
    public static final int KEY_STATE_REPEAT = 2;

    public static final int MODIFIER_KEY_STATE_NUM_LOCK = 0x01;
    public static final int MODIFIER_KEY_STATE_CAPS_LOCK = 0x02;
    public static final int MODIFIER_KEY_STATE_SCROLL_LOCK = 0x04;

    public static final int KEYBOARD_EVENT_MODIFIER_CAPS_LOCK = 0x0001;
    public static final int KEYBOARD_EVENT_MODIFIER_NUM_LOCK = 0x0002;
    public static final int KEYBOARD_EVENT_MODIFIER_SCROLL_LOCK = 0x0004;
    public static final int KEYBOARD_EVENT_MODIFIER_LEFT_SHIFT = 0x0008;
    public static final int KEYBOARD_EVENT_MODIFIER_RIGHT_SHIFT = 0x0010;
    public static final int KEYBOARD_EVENT_MODIFIER_LEFT_ALT = 0x0020;
    public static final int KEYBOARD_EVENT_MODIFIER_RIGHT_ALT = 0x0040;
    public static final int KEYBOARD_EVENT_MODIFIER_LEFT_CONTROL = 0x0080;
    public static final int KEYBOARD_EVENT_MODIFIER_RIGHT_CONTROL = 0x0100;
    public static final int KEYBOARD_EVENT_MODIFIER_ANY_SHIFT =
        KEYBOARD_EVENT_MODIFIER_LEFT_SHIFT | KEYBOARD_EVENT_MODIFIER_RIGHT_SHIFT;
    public static final int KEYBOARD_EVENT_MODIFIER_ANY_ALT =
        KEYBOARD_EVENT_MODIFIER_LEFT_ALT | KEYBOARD_EVENT_MODIFIER_RIGHT_ALT;
    public static final int KEYBOARD_EVENT_MODIFIER_ANY_CONTROL =
        KEYBOARD_EVENT_MODIFIER_LEFT_CONTROL | KEYBOARD_EVENT_MODIFIER_RIGHT_CONTROL;

    public static final int KEY_QUEUE_SIZE = 64;

    public static final int KEY_ESCAPE = 0x1B;
    public static final int KEY_TAB = 0x09;
    public static final int KEY_BACKSPACE = 0x08;
    public static final int KEY_RETURN = '\r';

    public static final int KEY_SPACE = ' ';
    public static final int KEY_EXCLAMATION = '!';
    public static final int KEY_QUOTE = '"';
    public static final int KEY_NUMBER_SIGN = '#';
    public static final int KEY_DOLLAR = '$';
    public static final int KEY_PERCENT = '%';
    public static final int KEY_AMPERSAND = '&';
    public static final int KEY_SINGLE_QUOTE = '\'';
    public static final int KEY_PAREN_LEFT = '(';
    public static final int KEY_PAREN_RIGHT = ')';
    public static final int KEY_ASTERISK = '*';
    public static final int KEY_PLUS = '+';
    public static final int KEY_COMMA = ',';
    public static final int KEY_MINUS = '-';
    public static final int KEY_DOT = '.';
    public static final int KEY_SLASH = '/';
    public static final int KEY_0 = '0';
    public static final int KEY_1 = '1';
    public static final int KEY_2 = '2';
    public static final int KEY_3 = '3';
    public static final int KEY_4 = '4';
    public static final int KEY_5 = '5';
    public static final int KEY_6 = '6';
    public static final int KEY_7 = '7';
    public static final int KEY_8 = '8';
    public static final int KEY_9 = '9';
    public static final int KEY_COLON = ':';
    public static final int KEY_SEMICOLON = ';';
    public static final int KEY_LESS = '<';
    public static final int KEY_EQUAL = '=';
    public static final int KEY_GREATER = '>';
    public static final int KEY_QUESTION = '?';
    public static final int KEY_AT = '@';
    public static final int KEY_UPPERCASE_A = 'A';
    public static final int KEY_UPPERCASE_B = 'B';
    public static final int KEY_UPPERCASE_C = 'C';
    public static final int KEY_UPPERCASE_D = 'D';
    public static final int KEY_UPPERCASE_E = 'E';
    public static final int KEY_UPPERCASE_F = 'F';
    public static final int KEY_UPPERCASE_G = 'G';
    public static final int KEY_UPPERCASE_H = 'H';
    public static final int KEY_UPPERCASE_I = 'I';
    public static final int KEY_UPPERCASE_J = 'J';
    public static final int KEY_UPPERCASE_K = 'K';
    public static final int KEY_UPPERCASE_L = 'L';
    public static final int KEY_UPPERCASE_M = 'M';
    public static final int KEY_UPPERCASE_N = 'N';
    public static final int KEY_UPPERCASE_O = 'O';
    public static final int KEY_UPPERCASE_P = 'P';
    public static final int KEY_UPPERCASE_Q = 'Q';
    public static final int KEY_UPPERCASE_R = 'R';
    public static final int KEY_UPPERCASE_S = 'S';
    public static final int KEY_UPPERCASE_T = 'T';
    public static final int KEY_UPPERCASE_U = 'U';
    public static final int KEY_UPPERCASE_V = 'V';
    public static final int KEY_UPPERCASE_W = 'W';
    public static final int KEY_UPPERCASE_X = 'X';
    public static final int KEY_UPPERCASE_Y = 'Y';
    public static final int KEY_UPPERCASE_Z = 'Z';

    public static final int KEY_BRACKET_LEFT = '[';
    public static final int KEY_BACKSLASH = '\\';
    public static final int KEY_BRACKET_RIGHT = ']';
    public static final int KEY_CARET = '^';
    public static final int KEY_UNDERSCORE = '_';

    public static final int KEY_GRAVE = '`';
    public static final int KEY_LOWERCASE_A = 'a';
    public static final int KEY_LOWERCASE_B = 'b';
    public static final int KEY_LOWERCASE_C = 'c';
    public static final int KEY_LOWERCASE_D = 'd';
    public static final int KEY_LOWERCASE_E = 'e';
    public static final int KEY_LOWERCASE_F = 'f';
    public static final int KEY_LOWERCASE_G = 'g';
    public static final int KEY_LOWERCASE_H = 'h';
    public static final int KEY_LOWERCASE_I = 'i';
    public static final int KEY_LOWERCASE_J = 'j';
    public static final int KEY_LOWERCASE_K = 'k';
    public static final int KEY_LOWERCASE_L = 'l';
    public static final int KEY_LOWERCASE_M = 'm';
    public static final int KEY_LOWERCASE_N = 'n';
    public static final int KEY_LOWERCASE_O = 'o';
    public static final int KEY_LOWERCASE_P = 'p';
    public static final int KEY_LOWERCASE_Q = 'q';
    public static final int KEY_LOWERCASE_R = 'r';
    public static final int KEY_LOWERCASE_S = 's';
    public static final int KEY_LOWERCASE_T = 't';
    public static final int KEY_LOWERCASE_U = 'u';
    public static final int KEY_LOWERCASE_V = 'v';
    public static final int KEY_LOWERCASE_W = 'w';
    public static final int KEY_LOWERCASE_X = 'x';
    public static final int KEY_LOWERCASE_Y = 'y';
    public static final int KEY_LOWERCASE_Z = 'z';
    public static final int KEY_BRACE_LEFT = '{';
    public static final int KEY_BAR = '|';
    public static final int KEY_BRACE_RIGHT = '}';
    public static final int KEY_TILDE = '~';
    public static final int KEY_DEL = 127;

    public static final int KEY_136 = 136;
    public static final int KEY_146 = 146;
    public static final int KEY_149 = 149;
    public static final int KEY_150 = 150;
    public static final int KEY_151 = 151;
    public static final int KEY_152 = 152;
    public static final int KEY_161 = 161;
    public static final int KEY_163 = 163;
    public static final int KEY_164 = 164;
    public static final int KEY_166 = 166;
    public static final int KEY_168 = 168;
    public static final int KEY_167 = 167;
    public static final int KEY_170 = 170;
    public static final int KEY_172 = 172;
    public static final int KEY_176 = 176;
    public static final int KEY_178 = 178;
    public static final int KEY_179 = 179;
    public static final int KEY_180 = 180;
    public static final int KEY_181 = 181;
    public static final int KEY_186 = 186;
    public static final int KEY_191 = 191;
    public static final int KEY_196 = 196;
    public static final int KEY_199 = 199;
    public static final int KEY_209 = 209;
    public static final int KEY_214 = 214;
    public static final int KEY_215 = 215;
    public static final int KEY_220 = 220;
    public static final int KEY_223 = 223;
    public static final int KEY_224 = 224;
    public static final int KEY_228 = 228;
    public static final int KEY_231 = 231;
    public static final int KEY_232 = 232;
    public static final int KEY_233 = 233;
    public static final int KEY_241 = 241;
    public static final int KEY_246 = 246;
    public static final int KEY_247 = 247;
    public static final int KEY_249 = 249;
    public static final int KEY_252 = 252;

    public static final int KEY_ALT_Q = 272;
    public static final int KEY_ALT_W = 273;
    public static final int KEY_ALT_E = 274;
    public static final int KEY_ALT_R = 275;
    public static final int KEY_ALT_T = 276;
    public static final int KEY_ALT_Y = 277;
    public static final int KEY_ALT_U = 278;
    public static final int KEY_ALT_I = 279;
    public static final int KEY_ALT_O = 280;
    public static final int KEY_ALT_P = 281;
    public static final int KEY_ALT_A = 286;
    public static final int KEY_ALT_S = 287;
    public static final int KEY_ALT_D = 288;
    public static final int KEY_ALT_F = 289;
    public static final int KEY_ALT_G = 290;
    public static final int KEY_ALT_H = 291;
    public static final int KEY_ALT_J = 292;
    public static final int KEY_ALT_K = 293;
    public static final int KEY_ALT_L = 294;
    public static final int KEY_ALT_Z = 300;
    public static final int KEY_ALT_X = 301;
    public static final int KEY_ALT_C = 302;
    public static final int KEY_ALT_V = 303;
    public static final int KEY_ALT_B = 304;
    public static final int KEY_ALT_N = 305;
    public static final int KEY_ALT_M = 306;

    public static final int KEY_CTRL_Q = 17;
    public static final int KEY_CTRL_W = 23;
    public static final int KEY_CTRL_E = 5;
    public static final int KEY_CTRL_R = 18;
    public static final int KEY_CTRL_T = 20;
    public static final int KEY_CTRL_Y = 25;
    public static final int KEY_CTRL_U = 21;
    public static final int KEY_CTRL_I = 9;
    public static final int KEY_CTRL_O = 15;
    public static final int KEY_CTRL_P = 16;
    public static final int KEY_CTRL_A = 1;
    public static final int KEY_CTRL_S = 19;
    public static final int KEY_CTRL_D = 4;
    public static final int KEY_CTRL_F = 6;
    public static final int KEY_CTRL_G = 7;
    public static final int KEY_CTRL_H = 8;
    public static final int KEY_CTRL_J = 10;
    public static final int KEY_CTRL_K = 11;
    public static final int KEY_CTRL_L = 12;
    public static final int KEY_CTRL_Z = 26;
    public static final int KEY_CTRL_X = 24;
    public static final int KEY_CTRL_C = 3;
    public static final int KEY_CTRL_V = 22;
    public static final int KEY_CTRL_B = 2;
    public static final int KEY_CTRL_N = 14;
    public static final int KEY_CTRL_M = 13;

    public static final int KEY_F1 = 315;
    public static final int KEY_F2 = 316;
    public static final int KEY_F3 = 317;
    public static final int KEY_F4 = 318;
    public static final int KEY_F5 = 319;
    public static final int KEY_F6 = 320;
    public static final int KEY_F7 = 321;
    public static final int KEY_F8 = 322;
    public static final int KEY_F9 = 323;
    public static final int KEY_F10 = 324;
    public static final int KEY_F11 = 389;
    public static final int KEY_F12 = 390;

    public static final int KEY_SHIFT_F1 = 340;
    public static final int KEY_SHIFT_F2 = 341;
    public static final int KEY_SHIFT_F3 = 342;
    public static final int KEY_SHIFT_F4 = 343;
    public static final int KEY_SHIFT_F5 = 344;
    public static final int KEY_SHIFT_F6 = 345;
    public static final int KEY_SHIFT_F7 = 346;
    public static final int KEY_SHIFT_F8 = 347;
    public static final int KEY_SHIFT_F9 = 348;
    public static final int KEY_SHIFT_F10 = 349;
    public static final int KEY_SHIFT_F11 = 391;
    public static final int KEY_SHIFT_F12 = 392;

    public static final int KEY_CTRL_F1 = 350;
    public static final int KEY_CTRL_F2 = 351;
    public static final int KEY_CTRL_F3 = 352;
    public static final int KEY_CTRL_F4 = 353;
    public static final int KEY_CTRL_F5 = 354;
    public static final int KEY_CTRL_F6 = 355;
    public static final int KEY_CTRL_F7 = 356;
    public static final int KEY_CTRL_F8 = 357;
    public static final int KEY_CTRL_F9 = 358;
    public static final int KEY_CTRL_F10 = 359;
    public static final int KEY_CTRL_F11 = 393;
    public static final int KEY_CTRL_F12 = 394;

    public static final int KEY_ALT_F1 = 360;
    public static final int KEY_ALT_F2 = 361;
    public static final int KEY_ALT_F3 = 362;
    public static final int KEY_ALT_F4 = 363;
    public static final int KEY_ALT_F5 = 364;
    public static final int KEY_ALT_F6 = 365;
    public static final int KEY_ALT_F7 = 366;
    public static final int KEY_ALT_F8 = 367;
    public static final int KEY_ALT_F9 = 368;
    public static final int KEY_ALT_F10 = 369;
    public static final int KEY_ALT_F11 = 395;
    public static final int KEY_ALT_F12 = 396;

    public static final int KEY_HOME = 327;
    public static final int KEY_CTRL_HOME = 375;
    public static final int KEY_ALT_HOME = 407;

    public static final int KEY_PAGE_UP = 329;
    public static final int KEY_CTRL_PAGE_UP = 388;
    public static final int KEY_ALT_PAGE_UP = 409;

    public static final int KEY_INSERT = 338;
    public static final int KEY_CTRL_INSERT = 402;
    public static final int KEY_ALT_INSERT = 418;

    public static final int KEY_DELETE = 339;
    public static final int KEY_CTRL_DELETE = 403;
    public static final int KEY_ALT_DELETE = 419;

    public static final int KEY_END = 335;
    public static final int KEY_CTRL_END = 373;
    public static final int KEY_ALT_END = 415;

    public static final int KEY_PAGE_DOWN = 337;
    public static final int KEY_ALT_PAGE_DOWN = 417;
    public static final int KEY_CTRL_PAGE_DOWN = 374;

    public static final int KEY_ARROW_UP = 328;
    public static final int KEY_CTRL_ARROW_UP = 397;
    public static final int KEY_ALT_ARROW_UP = 408;

    public static final int KEY_ARROW_DOWN = 336;
    public static final int KEY_CTRL_ARROW_DOWN = 401;
    public static final int KEY_ALT_ARROW_DOWN = 416;

    public static final int KEY_ARROW_LEFT = 331;
    public static final int KEY_CTRL_ARROW_LEFT = 371;
    public static final int KEY_ALT_ARROW_LEFT = 411;

    public static final int KEY_ARROW_RIGHT = 333;
    public static final int KEY_CTRL_ARROW_RIGHT = 372;
    public static final int KEY_ALT_ARROW_RIGHT = 413;

    public static final int KEY_CTRL_BACKSLASH = 192;

    public static final int KEY_NUMBERPAD_5 = 332;
    public static final int KEY_CTRL_NUMBERPAD_5 = 399;
    public static final int KEY_ALT_NUMBERPAD_5 = 9999;

    public static final int KEY_FIRST_INPUT_CHARACTER = KEY_SPACE;
    public static final int KEY_LAST_INPUT_CHARACTER = KEY_LOWERCASE_Z;

    /** Keyboard layouts (C {@code KeyboardLayout}). */
    public static final int KEYBOARD_LAYOUT_QWERTY = 0;
    public static final int KEYBOARD_LAYOUT_FRENCH = 1;
    public static final int KEYBOARD_LAYOUT_GERMAN = 2;
    public static final int KEYBOARD_LAYOUT_ITALIAN = 3;
    public static final int KEYBOARD_LAYOUT_SPANISH = 4;

    /** Port of {@code LogicalKeyEntry} (without write-only {@code field_0}). */
    private static final class LogicalKeyEntry {
        int unmodified;
        int shift;
        int lmenu;
        int rmenu;
        int ctrl;
    }

    /** Port of {@code KeyboardEvent}. */
    static final class KeyboardEvent {
        int scanCode;
        int modifiers;
    }

    private static boolean installed;
    private static boolean keyboardDisabled;
    private static boolean numpadDisabled;
    private static boolean numlockDisabled;
    private static int queueWriteIndex;
    private static int queueReadIndex;
    private static int modifierKeysState;
    private static boolean englishUsMapper;
    private static final KeyboardEvent[] eventsQueue = new KeyboardEvent[KEY_QUEUE_SIZE];
    private static final LogicalKeyEntry[] logicalKeyEntries = new LogicalKeyEntry[SdlScancode.NUM_SCANCODES];
    private static final int[] pressedPhysicalKeys = new int[SdlScancode.NUM_SCANCODES];
    private static int idleStartTime;
    private static final KeyboardEvent lastKeyboardEvent = new KeyboardEvent();
    private static int keyboardLayout;
    private static int pressedPhysicalKeysCount;

    static {
        for (int index = 0; index < eventsQueue.length; index++) {
            eventsQueue[index] = new KeyboardEvent();
        }
        for (int index = 0; index < logicalKeyEntries.length; index++) {
            logicalKeyEntries[index] = new LogicalKeyEntry();
        }
    }

    /** Port of {@code keyboardInit} (0x4CBC90). */
    public static int keyboardInit() {
        if (installed) {
            return -1;
        }
        installed = true;

        // NOTE: Uninline.
        keyboardReset();

        kbInitLockStatus(0);
        keyboardSetLayout(KEYBOARD_LAYOUT_QWERTY);

        idleStartTime = (int) Ticks.getTicks();

        return 0;
    }

    /** Port of {@code keyboardFree} (0x4CBD00). */
    public static void keyboardFree() {
        if (installed) {
            installed = false;
        }
    }

    /** Port of {@code keyboardReset} (0x4CBDA8 kb_clear). */
    public static void keyboardReset() {
        if (installed) {
            pressedPhysicalKeysCount = 0;

            Arrays.fill(pressedPhysicalKeys, KEY_STATE_UP);

            queueWriteIndex = 0;
            queueReadIndex = 0;
        }

        DInput.keyboardDeviceReset();
        Input.gnw95ClearTimeStamps();
    }

    /** Port of {@code _kb_getch}. */
    public static int kbGetch() {
        if (!installed) {
            return -1;
        }
        return englishUsMapper ? kbNextAsciiEnglishUs() : keyboardDequeueLogicalKeyCode();
    }

    /** Port of {@code keyboardDisable} (0x4CBE00 kb_disable). */
    public static void keyboardDisable() {
        keyboardDisabled = true;
    }

    /** Port of {@code keyboardEnable} (0x4CBE0C kb_enable). */
    public static void keyboardEnable() {
        keyboardDisabled = false;
    }

    /** Port of {@code keyboardIsDisabled} (0x4CBE18 kb_is_disabled). */
    public static boolean keyboardIsDisabled() {
        return keyboardDisabled;
    }

    /** Port of {@code keyboardSetLayout} (0x4CBE74 kb_set_layout). */
    public static void keyboardSetLayout(int newLayout) {
        int oldLayout = keyboardLayout;
        keyboardLayout = newLayout;

        switch (newLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            englishUsMapper = true;
            buildQwertyConfiguration();
            break;
        // case KEYBOARD_LAYOUT_FRENCH:
        //    ... (commented out in C++, builders ported below)
        //    break;
        default:
            keyboardLayout = oldLayout;
            break;
        }
    }

    /** Port of {@code keyboardGetLayout} (0x4CBEEC kb_get_layout). */
    public static int keyboardGetLayout() {
        return keyboardLayout;
    }

    /** Port of {@code _kb_simulate_key}. */
    public static void kbSimulateKey(DInput.KeyboardData data) {
        idleStartTime = (int) Input.getBkTime();

        int key = data.key();
        int keyState = data.down() ? KEY_STATE_DOWN : KEY_STATE_UP;

        int physicalKey = key;
        if (physicalKey < 0 || physicalKey >= SdlScancode.NUM_SCANCODES) {
            return;
        }

        if (keyState != KEY_STATE_UP && pressedPhysicalKeys[physicalKey] != KEY_STATE_UP) {
            keyState = KEY_STATE_REPEAT;
        }

        if (pressedPhysicalKeys[physicalKey] != keyState) {
            pressedPhysicalKeys[physicalKey] = keyState;
            if (keyState == KEY_STATE_DOWN) {
                pressedPhysicalKeysCount = (pressedPhysicalKeysCount + 1) & 0xFF;
            } else if (keyState == KEY_STATE_UP) {
                pressedPhysicalKeysCount = (pressedPhysicalKeysCount - 1) & 0xFF;
            }
        }

        if (keyState != KEY_STATE_UP) {
            lastKeyboardEvent.scanCode = physicalKey;
            lastKeyboardEvent.modifiers = 0;

            if (physicalKey == SdlScancode.CAPSLOCK) {
                if (pressedPhysicalKeys[SdlScancode.LCTRL] == KEY_STATE_UP
                        && pressedPhysicalKeys[SdlScancode.RCTRL] == KEY_STATE_UP) {
                    // NOTE: mirrors the upstream gap — the C++ code does not
                    // check the keyboard layout here either (TODO in kb.cc).
                    if ((modifierKeysState & MODIFIER_KEY_STATE_CAPS_LOCK) != 0) {
                        // NOTE: mirrors upstream — the C++ _kb_layout branch is
                        // unreachable here (commented TODO in kb.cc).
                        modifierKeysState &= ~MODIFIER_KEY_STATE_CAPS_LOCK;
                    } else {
                        modifierKeysState |= MODIFIER_KEY_STATE_CAPS_LOCK;
                    }
                }
            } else if (physicalKey == SdlScancode.NUMLOCKCLEAR) {
                if (pressedPhysicalKeys[SdlScancode.LCTRL] == KEY_STATE_UP
                        && pressedPhysicalKeys[SdlScancode.RCTRL] == KEY_STATE_UP) {
                    if ((modifierKeysState & MODIFIER_KEY_STATE_NUM_LOCK) != 0) {
                        modifierKeysState &= ~MODIFIER_KEY_STATE_NUM_LOCK;
                    } else {
                        modifierKeysState |= MODIFIER_KEY_STATE_NUM_LOCK;
                    }
                }
            } else if (physicalKey == SdlScancode.SCROLLLOCK) {
                if (pressedPhysicalKeys[SdlScancode.LCTRL] == KEY_STATE_UP
                        && pressedPhysicalKeys[SdlScancode.RCTRL] == KEY_STATE_UP) {
                    if ((modifierKeysState & MODIFIER_KEY_STATE_SCROLL_LOCK) != 0) {
                        modifierKeysState &= ~MODIFIER_KEY_STATE_SCROLL_LOCK;
                    } else {
                        modifierKeysState |= MODIFIER_KEY_STATE_SCROLL_LOCK;
                    }
                }
            } else if ((physicalKey == SdlScancode.LSHIFT || physicalKey == SdlScancode.RSHIFT)
                    && (modifierKeysState & MODIFIER_KEY_STATE_CAPS_LOCK) != 0 && keyboardLayout != 0) {
                if (pressedPhysicalKeys[SdlScancode.LCTRL] == KEY_STATE_UP
                        && pressedPhysicalKeys[SdlScancode.RCTRL] == KEY_STATE_UP) {
                    if ((modifierKeysState & MODIFIER_KEY_STATE_CAPS_LOCK) != 0) {
                        modifierKeysState &= ~MODIFIER_KEY_STATE_CAPS_LOCK;
                    } else {
                        modifierKeysState |= MODIFIER_KEY_STATE_CAPS_LOCK;
                    }
                }
            }

            if (modifierKeysState != 0) {
                if ((modifierKeysState & MODIFIER_KEY_STATE_NUM_LOCK) != 0 && !numlockDisabled) {
                    lastKeyboardEvent.modifiers |= KEYBOARD_EVENT_MODIFIER_NUM_LOCK;
                }

                if ((modifierKeysState & MODIFIER_KEY_STATE_CAPS_LOCK) != 0) {
                    lastKeyboardEvent.modifiers |= KEYBOARD_EVENT_MODIFIER_CAPS_LOCK;
                }

                if ((modifierKeysState & MODIFIER_KEY_STATE_SCROLL_LOCK) != 0) {
                    lastKeyboardEvent.modifiers |= KEYBOARD_EVENT_MODIFIER_SCROLL_LOCK;
                }
            }

            if (pressedPhysicalKeys[SdlScancode.LSHIFT] != KEY_STATE_UP) {
                lastKeyboardEvent.modifiers |= KEYBOARD_EVENT_MODIFIER_LEFT_SHIFT;
            }

            if (pressedPhysicalKeys[SdlScancode.RSHIFT] != KEY_STATE_UP) {
                lastKeyboardEvent.modifiers |= KEYBOARD_EVENT_MODIFIER_RIGHT_SHIFT;
            }

            if (pressedPhysicalKeys[SdlScancode.LALT] != KEY_STATE_UP) {
                lastKeyboardEvent.modifiers |= KEYBOARD_EVENT_MODIFIER_LEFT_ALT;
            }

            if (pressedPhysicalKeys[SdlScancode.RALT] != KEY_STATE_UP) {
                lastKeyboardEvent.modifiers |= KEYBOARD_EVENT_MODIFIER_RIGHT_ALT;
            }

            if (pressedPhysicalKeys[SdlScancode.LCTRL] != KEY_STATE_UP) {
                lastKeyboardEvent.modifiers |= KEYBOARD_EVENT_MODIFIER_LEFT_CONTROL;
            }

            if (pressedPhysicalKeys[SdlScancode.RCTRL] != KEY_STATE_UP) {
                lastKeyboardEvent.modifiers |= KEYBOARD_EVENT_MODIFIER_RIGHT_CONTROL;
            }

            if (((queueWriteIndex + 1) & 0x3F) != queueReadIndex) {
                KeyboardEvent slot = eventsQueue[queueWriteIndex];
                slot.scanCode = lastKeyboardEvent.scanCode;
                slot.modifiers = lastKeyboardEvent.modifiers;
                queueWriteIndex++;
                queueWriteIndex &= 0x3F;
            }
        }
    }

    /**
     * Port of {@code _kb_init_lock_status} (0x4D24F8): seeds lock flags from
     * an SDL modifier mask ({@code SDL_GetModState}). The platform backend
     * calls this with live state when known; {@link #keyboardInit} passes 0
     * (unknown). Scroll lock needs SDL 2.0.18+ headers (like the C++
     * {@code #if}), which the reference toolchain satisfies.
     */
    public static void kbInitLockStatus(int sdlModState) {
        if ((sdlModState & SdlScancode.KMOD_CAPS) != 0) {
            modifierKeysState |= MODIFIER_KEY_STATE_CAPS_LOCK;
        }

        if ((sdlModState & SdlScancode.KMOD_NUM) != 0) {
            modifierKeysState |= MODIFIER_KEY_STATE_NUM_LOCK;
        }

        if ((sdlModState & SdlScancode.KMOD_SCROLL) != 0) {
            modifierKeysState |= MODIFIER_KEY_STATE_SCROLL_LOCK;
        }
    }

    /** Port of {@code _kb_next_ascii_English_US} (0x4CC2F0). */
    private static int kbNextAsciiEnglishUs() {
        KeyboardEvent event = peekEvent(0);
        if (event == null) {
            return -1;
        }

        if ((event.modifiers & KEYBOARD_EVENT_MODIFIER_CAPS_LOCK) != 0) {
            int a = (keyboardLayout != KEYBOARD_LAYOUT_FRENCH ? SdlScancode.A : SdlScancode.Q);
            int m = (keyboardLayout != KEYBOARD_LAYOUT_FRENCH ? SdlScancode.M : SdlScancode.SEMICOLON);
            int q = (keyboardLayout != KEYBOARD_LAYOUT_FRENCH ? SdlScancode.Q : SdlScancode.A);
            int w = (keyboardLayout != KEYBOARD_LAYOUT_FRENCH ? SdlScancode.W : SdlScancode.Z);

            int y;
            switch (keyboardLayout) {
            case KEYBOARD_LAYOUT_QWERTY:
            case KEYBOARD_LAYOUT_FRENCH:
            case KEYBOARD_LAYOUT_ITALIAN:
            case KEYBOARD_LAYOUT_SPANISH:
                y = SdlScancode.Y;
                break;
            default:
                // GERMAN
                y = SdlScancode.Z;
                break;
            }

            int z;
            switch (keyboardLayout) {
            case KEYBOARD_LAYOUT_QWERTY:
            case KEYBOARD_LAYOUT_ITALIAN:
            case KEYBOARD_LAYOUT_SPANISH:
                z = SdlScancode.Z;
                break;
            case KEYBOARD_LAYOUT_FRENCH:
                z = SdlScancode.W;
                break;
            default:
                // GERMAN
                z = SdlScancode.Y;
                break;
            }

            int scanCode = event.scanCode;
            if (scanCode == a
                    || scanCode == SdlScancode.B
                    || scanCode == SdlScancode.C
                    || scanCode == SdlScancode.D
                    || scanCode == SdlScancode.E
                    || scanCode == SdlScancode.F
                    || scanCode == SdlScancode.G
                    || scanCode == SdlScancode.H
                    || scanCode == SdlScancode.I
                    || scanCode == SdlScancode.J
                    || scanCode == SdlScancode.K
                    || scanCode == SdlScancode.L
                    || scanCode == m
                    || scanCode == SdlScancode.N
                    || scanCode == SdlScancode.O
                    || scanCode == SdlScancode.P
                    || scanCode == q
                    || scanCode == SdlScancode.R
                    || scanCode == SdlScancode.S
                    || scanCode == SdlScancode.T
                    || scanCode == SdlScancode.U
                    || scanCode == SdlScancode.V
                    || scanCode == w
                    || scanCode == SdlScancode.X
                    || scanCode == y
                    || scanCode == z) {
                if ((event.modifiers & KEYBOARD_EVENT_MODIFIER_ANY_SHIFT) != 0) {
                    event.modifiers &= ~KEYBOARD_EVENT_MODIFIER_ANY_SHIFT;
                } else {
                    event.modifiers |= KEYBOARD_EVENT_MODIFIER_LEFT_SHIFT;
                }
            }
        }

        return keyboardDequeueLogicalKeyCode();
    }

    /** Port of {@code keyboardDequeueLogicalKeyCode} (0x4CDA4C kb_next_ascii). */
    private static int keyboardDequeueLogicalKeyCode() {
        KeyboardEvent event = peekEvent(0);
        if (event == null) {
            return -1;
        }

        switch (event.scanCode) {
        case SdlScancode.KP_DIVIDE:
        case SdlScancode.KP_MULTIPLY:
        case SdlScancode.KP_MINUS:
        case SdlScancode.KP_PLUS:
        case SdlScancode.KP_ENTER:
            if (numpadDisabled) {
                consumeEvent();
                return -1;
            }
            break;
        case SdlScancode.KP_0:
        case SdlScancode.KP_1:
        case SdlScancode.KP_2:
        case SdlScancode.KP_3:
        case SdlScancode.KP_4:
        case SdlScancode.KP_5:
        case SdlScancode.KP_6:
        case SdlScancode.KP_7:
        case SdlScancode.KP_8:
        case SdlScancode.KP_9:
            if (numpadDisabled) {
                consumeEvent();
                return -1;
            }

            if ((event.modifiers & KEYBOARD_EVENT_MODIFIER_ANY_ALT) == 0
                    && (event.modifiers & KEYBOARD_EVENT_MODIFIER_NUM_LOCK) != 0) {
                if ((event.modifiers & KEYBOARD_EVENT_MODIFIER_ANY_SHIFT) != 0) {
                    event.modifiers &= ~KEYBOARD_EVENT_MODIFIER_ANY_SHIFT;
                } else {
                    event.modifiers |= KEYBOARD_EVENT_MODIFIER_LEFT_SHIFT;
                }
            }

            break;
        }

        LogicalKeyEntry description = logicalKeyEntries[event.scanCode];
        int logicalKey;
        if ((event.modifiers & KEYBOARD_EVENT_MODIFIER_ANY_CONTROL) != 0) {
            logicalKey = description.ctrl;
        } else if ((event.modifiers & KEYBOARD_EVENT_MODIFIER_RIGHT_ALT) != 0) {
            logicalKey = description.rmenu;
        } else if ((event.modifiers & KEYBOARD_EVENT_MODIFIER_LEFT_ALT) != 0) {
            logicalKey = description.lmenu;
        } else if ((event.modifiers & KEYBOARD_EVENT_MODIFIER_ANY_SHIFT) != 0) {
            logicalKey = description.shift;
        } else {
            logicalKey = description.unmodified;
        }

        consumeEvent();

        return logicalKey;
    }

    private static void consumeEvent() {
        if (queueReadIndex != queueWriteIndex) {
            queueReadIndex++;
            queueReadIndex &= (KEY_QUEUE_SIZE - 1);
        }
    }

    /**
     * Port of {@code keyboardPeekEvent} (0x4D2614 kb_buffer_peek): peek
     * without consuming, null when the index is out of range.
     */
    static KeyboardEvent peekEvent(int index) {
        if (queueReadIndex != queueWriteIndex) {
            int end;
            if (queueWriteIndex <= queueReadIndex) {
                end = queueWriteIndex + KEY_QUEUE_SIZE - queueReadIndex - 1;
            } else {
                end = queueWriteIndex - queueReadIndex - 1;
            }

            if (index <= end) {
                return eventsQueue[(queueReadIndex + index) & (KEY_QUEUE_SIZE - 1)];
            }
        }

        return null;
    }

    private static void setEntry(int scancode, int unmodified, int shift, int lmenu, int rmenu, int ctrl) {
        LogicalKeyEntry entry = logicalKeyEntries[scancode];
        entry.unmodified = unmodified;
        entry.shift = shift;
        entry.lmenu = lmenu;
        entry.rmenu = rmenu;
        entry.ctrl = ctrl;
    }

    /** Port of {@code keyboardBuildQwertyConfiguration} (0x4CDC08). */
    private static void buildQwertyConfiguration() {
        int k;

        for (k = 0; k < SdlScancode.NUM_SCANCODES; k++) {
            setEntry(k, -1, -1, -1, -1, -1);
        }

        setEntry(SdlScancode.ESCAPE, KEY_ESCAPE, KEY_ESCAPE, KEY_ESCAPE, KEY_ESCAPE, KEY_ESCAPE);

        setEntry(SdlScancode.F1, KEY_F1, KEY_SHIFT_F1, KEY_ALT_F1, KEY_ALT_F1, KEY_CTRL_F1);
        setEntry(SdlScancode.F2, KEY_F2, KEY_SHIFT_F2, KEY_ALT_F2, KEY_ALT_F2, KEY_CTRL_F2);
        setEntry(SdlScancode.F3, KEY_F3, KEY_SHIFT_F3, KEY_ALT_F3, KEY_ALT_F3, KEY_CTRL_F3);
        setEntry(SdlScancode.F4, KEY_F4, KEY_SHIFT_F4, KEY_ALT_F4, KEY_ALT_F4, KEY_CTRL_F4);
        setEntry(SdlScancode.F5, KEY_F5, KEY_SHIFT_F5, KEY_ALT_F5, KEY_ALT_F5, KEY_CTRL_F5);
        setEntry(SdlScancode.F6, KEY_F6, KEY_SHIFT_F6, KEY_ALT_F6, KEY_ALT_F6, KEY_CTRL_F6);
        setEntry(SdlScancode.F7, KEY_F7, KEY_SHIFT_F7, KEY_ALT_F7, KEY_ALT_F7, KEY_CTRL_F7);
        setEntry(SdlScancode.F8, KEY_F8, KEY_SHIFT_F8, KEY_ALT_F8, KEY_ALT_F8, KEY_CTRL_F8);
        setEntry(SdlScancode.F9, KEY_F9, KEY_SHIFT_F9, KEY_ALT_F9, KEY_ALT_F9, KEY_CTRL_F9);
        setEntry(SdlScancode.F10, KEY_F10, KEY_SHIFT_F10, KEY_ALT_F10, KEY_ALT_F10, KEY_CTRL_F10);
        setEntry(SdlScancode.F11, KEY_F11, KEY_SHIFT_F11, KEY_ALT_F11, KEY_ALT_F11, KEY_CTRL_F11);
        setEntry(SdlScancode.F12, KEY_F12, KEY_SHIFT_F12, KEY_ALT_F12, KEY_ALT_F12, KEY_CTRL_F12);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.GRAVE;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N2;
            break;
        case KEYBOARD_LAYOUT_ITALIAN:
        case KEYBOARD_LAYOUT_SPANISH:
            k = 0;
            break;
        default:
            k = SdlScancode.RIGHTBRACKET;
            break;
        }

        setEntry(k, KEY_GRAVE, KEY_TILDE, -1, -1, -1);

        setEntry(SdlScancode.N1, KEY_1, KEY_EXCLAMATION, -1, -1, -1);
        setEntry(SdlScancode.N2, KEY_2, KEY_AT, -1, -1, -1);
        setEntry(SdlScancode.N3, KEY_3, KEY_NUMBER_SIGN, -1, -1, -1);
        setEntry(SdlScancode.N4, KEY_4, KEY_DOLLAR, -1, -1, -1);
        setEntry(SdlScancode.N5, KEY_5, KEY_PERCENT, -1, -1, -1);
        setEntry(SdlScancode.N6, KEY_6, KEY_CARET, -1, -1, -1);
        setEntry(SdlScancode.N7, KEY_7, KEY_AMPERSAND, -1, -1, -1);
        setEntry(SdlScancode.N8, KEY_8, KEY_ASTERISK, -1, -1, -1);
        setEntry(SdlScancode.N9, KEY_9, KEY_PAREN_LEFT, -1, -1, -1);
        setEntry(SdlScancode.N0, KEY_0, KEY_PAREN_RIGHT, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.MINUS;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N6;
            break;
        default:
            k = SdlScancode.SLASH;
            break;
        }

        setEntry(k, KEY_MINUS, KEY_UNDERSCORE, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.EQUALS;
            break;
        default:
            k = SdlScancode.N0;
            break;
        }

        setEntry(k, KEY_EQUAL, KEY_PLUS, -1, -1, -1);

        setEntry(SdlScancode.BACKSPACE, KEY_BACKSPACE, KEY_BACKSPACE, KEY_BACKSPACE, KEY_BACKSPACE, KEY_DEL);
        setEntry(SdlScancode.TAB, KEY_TAB, KEY_TAB, KEY_TAB, KEY_TAB, KEY_TAB);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.A;
            break;
        default:
            k = SdlScancode.Q;
            break;
        }

        setEntry(k, KEY_LOWERCASE_Q, KEY_UPPERCASE_Q, KEY_ALT_Q, KEY_ALT_Q, KEY_CTRL_Q);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.Z;
            break;
        default:
            k = SdlScancode.W;
            break;
        }

        setEntry(k, KEY_LOWERCASE_W, KEY_UPPERCASE_W, KEY_ALT_W, KEY_ALT_W, KEY_CTRL_W);

        setEntry(SdlScancode.E, KEY_LOWERCASE_E, KEY_UPPERCASE_E, KEY_ALT_E, KEY_ALT_E, KEY_CTRL_E);
        setEntry(SdlScancode.R, KEY_LOWERCASE_R, KEY_UPPERCASE_R, KEY_ALT_R, KEY_ALT_R, KEY_CTRL_R);
        setEntry(SdlScancode.T, KEY_LOWERCASE_T, KEY_UPPERCASE_T, KEY_ALT_T, KEY_ALT_T, KEY_CTRL_T);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
        case KEYBOARD_LAYOUT_FRENCH:
        case KEYBOARD_LAYOUT_ITALIAN:
        case KEYBOARD_LAYOUT_SPANISH:
            k = SdlScancode.Y;
            break;
        default:
            k = SdlScancode.Z;
            break;
        }

        setEntry(k, KEY_LOWERCASE_Y, KEY_UPPERCASE_Y, KEY_ALT_Y, KEY_ALT_Y, KEY_CTRL_Y);

        setEntry(SdlScancode.U, KEY_LOWERCASE_U, KEY_UPPERCASE_U, KEY_ALT_U, KEY_ALT_U, KEY_CTRL_U);
        setEntry(SdlScancode.I, KEY_LOWERCASE_I, KEY_UPPERCASE_I, KEY_ALT_I, KEY_ALT_I, KEY_CTRL_I);
        setEntry(SdlScancode.O, KEY_LOWERCASE_O, KEY_UPPERCASE_O, KEY_ALT_O, KEY_ALT_O, KEY_CTRL_O);
        setEntry(SdlScancode.P, KEY_LOWERCASE_P, KEY_UPPERCASE_P, KEY_ALT_P, KEY_ALT_P, KEY_CTRL_P);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
        case KEYBOARD_LAYOUT_ITALIAN:
        case KEYBOARD_LAYOUT_SPANISH:
            k = SdlScancode.LEFTBRACKET;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N5;
            break;
        default:
            k = SdlScancode.N8;
            break;
        }

        setEntry(k, KEY_BRACKET_LEFT, KEY_BRACE_LEFT, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
        case KEYBOARD_LAYOUT_ITALIAN:
        case KEYBOARD_LAYOUT_SPANISH:
            k = SdlScancode.RIGHTBRACKET;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.MINUS;
            break;
        default:
            k = SdlScancode.N9;
            break;
        }

        setEntry(k, KEY_BRACKET_RIGHT, KEY_BRACE_RIGHT, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.BACKSLASH;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N8;
            break;
        case KEYBOARD_LAYOUT_ITALIAN:
        case KEYBOARD_LAYOUT_SPANISH:
            k = SdlScancode.GRAVE;
            break;
        default:
            k = SdlScancode.MINUS;
            break;
        }

        setEntry(k, KEY_BACKSLASH, KEY_BAR, -1, -1, KEY_CTRL_BACKSLASH);

        setEntry(SdlScancode.CAPSLOCK, -1, -1, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.Q;
            break;
        default:
            k = SdlScancode.A;
            break;
        }

        setEntry(k, KEY_LOWERCASE_A, KEY_UPPERCASE_A, KEY_ALT_A, KEY_ALT_A, KEY_CTRL_A);

        setEntry(SdlScancode.S, KEY_LOWERCASE_S, KEY_UPPERCASE_S, KEY_ALT_S, KEY_ALT_S, KEY_CTRL_S);
        setEntry(SdlScancode.D, KEY_LOWERCASE_D, KEY_UPPERCASE_D, KEY_ALT_D, KEY_ALT_D, KEY_CTRL_D);
        setEntry(SdlScancode.F, KEY_LOWERCASE_F, KEY_UPPERCASE_F, KEY_ALT_F, KEY_ALT_F, KEY_CTRL_F);
        setEntry(SdlScancode.G, KEY_LOWERCASE_G, KEY_UPPERCASE_G, KEY_ALT_G, KEY_ALT_G, KEY_CTRL_G);
        setEntry(SdlScancode.H, KEY_LOWERCASE_H, KEY_UPPERCASE_H, KEY_ALT_H, KEY_ALT_H, KEY_CTRL_H);
        setEntry(SdlScancode.J, KEY_LOWERCASE_J, KEY_UPPERCASE_J, KEY_ALT_J, KEY_ALT_J, KEY_CTRL_J);
        setEntry(SdlScancode.K, KEY_LOWERCASE_K, KEY_UPPERCASE_K, KEY_ALT_K, KEY_ALT_K, KEY_CTRL_K);
        setEntry(SdlScancode.L, KEY_LOWERCASE_L, KEY_UPPERCASE_L, KEY_ALT_L, KEY_ALT_L, KEY_CTRL_L);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.SEMICOLON;
            break;
        default:
            k = SdlScancode.COMMA;
            break;
        }

        setEntry(k, KEY_SEMICOLON, KEY_COLON, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.APOSTROPHE;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N3;
            break;
        default:
            k = SdlScancode.N2;
            break;
        }

        setEntry(k, KEY_SINGLE_QUOTE, KEY_QUOTE, -1, -1, -1);

        setEntry(SdlScancode.RETURN, KEY_RETURN, KEY_RETURN, KEY_RETURN, KEY_RETURN, KEY_CTRL_J);
        setEntry(SdlScancode.LSHIFT, -1, -1, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
        case KEYBOARD_LAYOUT_ITALIAN:
        case KEYBOARD_LAYOUT_SPANISH:
            k = SdlScancode.Z;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.W;
            break;
        default:
            k = SdlScancode.Y;
            break;
        }

        setEntry(k, KEY_LOWERCASE_Z, KEY_UPPERCASE_Z, KEY_ALT_Z, KEY_ALT_Z, KEY_CTRL_Z);

        setEntry(SdlScancode.X, KEY_LOWERCASE_X, KEY_UPPERCASE_X, KEY_ALT_X, KEY_ALT_X, KEY_CTRL_X);
        setEntry(SdlScancode.C, KEY_LOWERCASE_C, KEY_UPPERCASE_C, KEY_ALT_C, KEY_ALT_C, KEY_CTRL_C);
        setEntry(SdlScancode.V, KEY_LOWERCASE_V, KEY_UPPERCASE_V, KEY_ALT_V, KEY_ALT_V, KEY_CTRL_V);
        setEntry(SdlScancode.B, KEY_LOWERCASE_B, KEY_UPPERCASE_B, KEY_ALT_B, KEY_ALT_B, KEY_CTRL_B);
        setEntry(SdlScancode.N, KEY_LOWERCASE_N, KEY_UPPERCASE_N, KEY_ALT_N, KEY_ALT_N, KEY_CTRL_N);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.SEMICOLON;
            break;
        default:
            k = SdlScancode.M;
            break;
        }

        setEntry(k, KEY_LOWERCASE_M, KEY_UPPERCASE_M, KEY_ALT_M, KEY_ALT_M, KEY_CTRL_M);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.M;
            break;
        default:
            k = SdlScancode.COMMA;
            break;
        }

        setEntry(k, KEY_COMMA, KEY_LESS, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.COMMA;
            break;
        default:
            k = SdlScancode.PERIOD;
            break;
        }

        setEntry(k, KEY_DOT, KEY_GREATER, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.SLASH;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.PERIOD;
            break;
        default:
            k = SdlScancode.N7;
            break;
        }

        setEntry(k, KEY_SLASH, KEY_QUESTION, -1, -1, -1);

        setEntry(SdlScancode.RSHIFT, -1, -1, -1, -1, -1);
        setEntry(SdlScancode.LCTRL, -1, -1, -1, -1, -1);
        setEntry(SdlScancode.LALT, -1, -1, -1, -1, -1);
        setEntry(SdlScancode.SPACE, KEY_SPACE, KEY_SPACE, KEY_SPACE, KEY_SPACE, KEY_SPACE);
        setEntry(SdlScancode.RALT, -1, -1, -1, -1, -1);
        setEntry(SdlScancode.RCTRL, -1, -1, -1, -1, -1);

        setEntry(SdlScancode.INSERT, KEY_INSERT, KEY_INSERT, KEY_ALT_INSERT, KEY_ALT_INSERT, KEY_CTRL_INSERT);
        setEntry(SdlScancode.HOME, KEY_HOME, KEY_HOME, KEY_ALT_HOME, KEY_ALT_HOME, KEY_CTRL_HOME);
        setEntry(SdlScancode.PAGEUP, KEY_PAGE_UP, KEY_PAGE_UP, KEY_ALT_PAGE_UP, KEY_ALT_PAGE_UP, KEY_CTRL_PAGE_UP);
        setEntry(SdlScancode.DELETE, KEY_DELETE, KEY_DELETE, KEY_ALT_DELETE, KEY_ALT_DELETE, KEY_CTRL_DELETE);
        setEntry(SdlScancode.END, KEY_END, KEY_END, KEY_ALT_END, KEY_ALT_END, KEY_CTRL_END);
        setEntry(SdlScancode.PAGEDOWN, KEY_PAGE_DOWN, KEY_PAGE_DOWN, KEY_ALT_PAGE_DOWN, KEY_ALT_PAGE_DOWN,
            KEY_CTRL_PAGE_DOWN);

        setEntry(SdlScancode.UP, KEY_ARROW_UP, KEY_ARROW_UP, KEY_ALT_ARROW_UP, KEY_ALT_ARROW_UP, KEY_CTRL_ARROW_UP);
        setEntry(SdlScancode.DOWN, KEY_ARROW_DOWN, KEY_ARROW_DOWN, KEY_ALT_ARROW_DOWN, KEY_ALT_ARROW_DOWN,
            KEY_CTRL_ARROW_DOWN);
        setEntry(SdlScancode.LEFT, KEY_ARROW_LEFT, KEY_ARROW_LEFT, KEY_ALT_ARROW_LEFT, KEY_ALT_ARROW_LEFT,
            KEY_CTRL_ARROW_LEFT);
        setEntry(SdlScancode.RIGHT, KEY_ARROW_RIGHT, KEY_ARROW_RIGHT, KEY_ALT_ARROW_RIGHT, KEY_ALT_ARROW_RIGHT,
            KEY_CTRL_ARROW_RIGHT);

        setEntry(SdlScancode.NUMLOCKCLEAR, -1, -1, -1, -1, -1);

        setEntry(SdlScancode.KP_DIVIDE, KEY_SLASH, KEY_SLASH, -1, -1, 3);
        setEntry(SdlScancode.KP_MULTIPLY, KEY_ASTERISK, KEY_ASTERISK, -1, -1, -1);
        setEntry(SdlScancode.KP_MINUS, KEY_MINUS, KEY_MINUS, -1, -1, -1);

        setEntry(SdlScancode.KP_7, KEY_HOME, KEY_7, KEY_ALT_HOME, KEY_ALT_HOME, KEY_CTRL_HOME);
        setEntry(SdlScancode.KP_8, KEY_ARROW_UP, KEY_8, KEY_ALT_ARROW_UP, KEY_ALT_ARROW_UP, KEY_CTRL_ARROW_UP);
        setEntry(SdlScancode.KP_9, KEY_PAGE_UP, KEY_9, KEY_ALT_PAGE_UP, KEY_ALT_PAGE_UP, KEY_CTRL_PAGE_UP);
        setEntry(SdlScancode.KP_PLUS, KEY_PLUS, KEY_PLUS, -1, -1, -1);
        setEntry(SdlScancode.KP_4, KEY_ARROW_LEFT, KEY_4, KEY_ALT_ARROW_LEFT, KEY_ALT_ARROW_LEFT,
            KEY_CTRL_ARROW_LEFT);
        setEntry(SdlScancode.KP_5, KEY_NUMBERPAD_5, KEY_5, KEY_ALT_NUMBERPAD_5, KEY_ALT_NUMBERPAD_5,
            KEY_CTRL_NUMBERPAD_5);
        setEntry(SdlScancode.KP_6, KEY_ARROW_RIGHT, KEY_6, KEY_ALT_ARROW_RIGHT, KEY_ALT_ARROW_RIGHT,
            KEY_CTRL_ARROW_RIGHT);
        setEntry(SdlScancode.KP_1, KEY_END, KEY_1, KEY_ALT_END, KEY_ALT_END, KEY_CTRL_END);
        setEntry(SdlScancode.KP_2, KEY_ARROW_DOWN, KEY_2, KEY_ALT_ARROW_DOWN, KEY_ALT_ARROW_DOWN,
            KEY_CTRL_ARROW_DOWN);
        setEntry(SdlScancode.KP_3, KEY_PAGE_DOWN, KEY_3, KEY_ALT_PAGE_DOWN, KEY_ALT_PAGE_DOWN, KEY_CTRL_PAGE_DOWN);
        setEntry(SdlScancode.KP_ENTER, KEY_RETURN, KEY_RETURN, -1, -1, -1);
        setEntry(SdlScancode.KP_0, KEY_INSERT, KEY_0, KEY_ALT_INSERT, KEY_ALT_INSERT, KEY_CTRL_INSERT);
        setEntry(SdlScancode.KP_DECIMAL, KEY_DELETE, KEY_DOT, -1, KEY_ALT_DELETE, KEY_CTRL_DELETE);
    }

    /** Port of {@code keyboardBuildFrenchConfiguration} (0x4D0400). */
    static void buildFrenchConfiguration() {
        int k;

        buildQwertyConfiguration();

        setEntry(SdlScancode.GRAVE, KEY_178, -1, -1, -1, -1);
        setEntry(SdlScancode.N1, KEY_AMPERSAND, KEY_1, -1, -1, -1);
        setEntry(SdlScancode.N2, KEY_233, KEY_2, -1, KEY_152, -1);
        setEntry(SdlScancode.N3, KEY_QUOTE, KEY_3, -1, KEY_NUMBER_SIGN, -1);
        setEntry(SdlScancode.N4, KEY_SINGLE_QUOTE, KEY_4, -1, KEY_BRACE_LEFT, -1);
        setEntry(SdlScancode.N5, KEY_PAREN_LEFT, KEY_5, -1, KEY_BRACKET_LEFT, -1);
        setEntry(SdlScancode.N6, KEY_150, KEY_6, -1, KEY_166, -1);
        setEntry(SdlScancode.N7, KEY_232, KEY_7, -1, KEY_GRAVE, -1);
        setEntry(SdlScancode.N8, KEY_UNDERSCORE, KEY_8, -1, KEY_BACKSLASH, -1);
        setEntry(SdlScancode.N9, KEY_231, KEY_9, -1, KEY_136, -1);
        setEntry(SdlScancode.N0, KEY_224, KEY_0, -1, KEY_AT, -1);
        setEntry(SdlScancode.MINUS, KEY_PAREN_RIGHT, KEY_176, -1, KEY_BRACKET_RIGHT, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.EQUALS;
            break;
        default:
            k = SdlScancode.N0;
            break;
        }

        setEntry(k, KEY_EQUAL, KEY_PLUS, -1, KEY_BRACE_RIGHT, -1);

        setEntry(SdlScancode.LEFTBRACKET, KEY_136, KEY_168, -1, -1, -1);
        setEntry(SdlScancode.RIGHTBRACKET, KEY_DOLLAR, KEY_163, -1, KEY_164, -1);
        setEntry(SdlScancode.APOSTROPHE, KEY_249, KEY_PERCENT, -1, -1, -1);
        setEntry(SdlScancode.BACKSLASH, KEY_ASTERISK, KEY_181, -1, -1, -1);

        // gLogicalKeyEntries[DIK_OEM_102]... (commented out in C++).

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.M;
            break;
        default:
            k = SdlScancode.COMMA;
            break;
        }

        setEntry(k, KEY_COMMA, KEY_QUESTION, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.SEMICOLON;
            break;
        default:
            k = SdlScancode.COMMA;
            break;
        }

        setEntry(k, KEY_SEMICOLON, KEY_DOT, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            // NOTE: mirrors the upstream FIXME guess — the C++ maps QWERTY
            // SEMICOLON to COLON (its comment suspects SDL_SCANCODE_COLON).
            k = SdlScancode.SEMICOLON;
            break;
        default:
            k = SdlScancode.PERIOD;
            break;
        }

        setEntry(k, KEY_COLON, KEY_SLASH, -1, -1, -1);

        setEntry(SdlScancode.SLASH, KEY_EXCLAMATION, KEY_167, -1, -1, -1);
    }

    /** Port of {@code keyboardBuildGermanConfiguration} (0x4D0C54). */
    static void buildGermanConfiguration() {
        int k;

        buildQwertyConfiguration();

        setEntry(SdlScancode.GRAVE, KEY_136, KEY_186, -1, -1, -1);
        setEntry(SdlScancode.N2, KEY_2, KEY_QUOTE, -1, KEY_178, -1);
        setEntry(SdlScancode.N3, KEY_3, KEY_167, -1, KEY_179, -1);
        setEntry(SdlScancode.N6, KEY_6, KEY_AMPERSAND, -1, -1, -1);
        setEntry(SdlScancode.N7, KEY_7, KEY_166, -1, KEY_BRACE_LEFT, -1);
        setEntry(SdlScancode.N8, KEY_8, KEY_PAREN_LEFT, -1, KEY_BRACKET_LEFT, -1);
        setEntry(SdlScancode.N9, KEY_9, KEY_PAREN_RIGHT, -1, KEY_BRACKET_RIGHT, -1);
        setEntry(SdlScancode.N0, KEY_0, KEY_EQUAL, -1, KEY_BRACE_RIGHT, -1);
        setEntry(SdlScancode.MINUS, KEY_223, KEY_QUESTION, -1, KEY_BACKSLASH, -1);
        setEntry(SdlScancode.EQUALS, KEY_180, KEY_GRAVE, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.A;
            break;
        default:
            k = SdlScancode.Q;
            break;
        }

        setEntry(k, KEY_LOWERCASE_Q, KEY_UPPERCASE_Q, KEY_ALT_Q, KEY_AT, KEY_CTRL_Q);

        setEntry(SdlScancode.LEFTBRACKET, KEY_252, KEY_220, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.EQUALS;
            break;
        default:
            k = SdlScancode.RIGHTBRACKET;
            break;
        }

        setEntry(k, KEY_PLUS, KEY_ASTERISK, -1, KEY_152, -1);

        setEntry(SdlScancode.SEMICOLON, KEY_246, KEY_214, -1, -1, -1);
        setEntry(SdlScancode.APOSTROPHE, KEY_228, KEY_196, -1, -1, -1);
        setEntry(SdlScancode.BACKSLASH, KEY_NUMBER_SIGN, KEY_SINGLE_QUOTE, -1, -1, -1);

        // gLogicalKeyEntries[DIK_OEM_102]... (commented out in C++).

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.SEMICOLON;
            break;
        default:
            k = SdlScancode.M;
            break;
        }

        setEntry(k, KEY_LOWERCASE_M, KEY_UPPERCASE_M, KEY_ALT_M, KEY_181, KEY_CTRL_M);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.M;
            break;
        default:
            k = SdlScancode.COMMA;
            break;
        }

        setEntry(k, KEY_COMMA, KEY_SEMICOLON, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.COMMA;
            break;
        default:
            k = SdlScancode.PERIOD;
            break;
        }

        setEntry(k, KEY_DOT, KEY_COLON, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.MINUS;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N6;
            break;
        default:
            k = SdlScancode.SLASH;
            break;
        }

        setEntry(k, KEY_150, KEY_151, -1, -1, -1);

        setEntry(SdlScancode.KP_DIVIDE, KEY_247, KEY_247, -1, -1, -1);
        setEntry(SdlScancode.KP_MULTIPLY, KEY_215, KEY_215, -1, -1, -1);
        setEntry(SdlScancode.KP_DECIMAL, KEY_DELETE, KEY_COMMA, -1, KEY_ALT_DELETE, KEY_CTRL_DELETE);
    }

    /** Port of {@code keyboardBuildItalianConfiguration} (0x4D1758). */
    static void buildItalianConfiguration() {
        int k;

        buildQwertyConfiguration();

        setEntry(SdlScancode.GRAVE, KEY_BACKSLASH, KEY_BAR, -1, -1, -1);

        // gLogicalKeyEntries[DIK_OEM_102]... (commented out in C++).

        setEntry(SdlScancode.N1, KEY_1, KEY_EXCLAMATION, -1, -1, -1);
        setEntry(SdlScancode.N2, KEY_2, KEY_QUOTE, -1, -1, -1);
        setEntry(SdlScancode.N3, KEY_3, KEY_163, -1, -1, -1);
        setEntry(SdlScancode.N6, KEY_6, KEY_AMPERSAND, -1, -1, -1);
        setEntry(SdlScancode.N7, KEY_7, KEY_SLASH, -1, -1, -1);
        setEntry(SdlScancode.N8, KEY_8, KEY_PAREN_LEFT, -1, -1, -1);
        setEntry(SdlScancode.N9, KEY_9, KEY_PAREN_RIGHT, -1, -1, -1);
        setEntry(SdlScancode.N0, KEY_0, KEY_EQUAL, -1, -1, -1);
        setEntry(SdlScancode.MINUS, KEY_SINGLE_QUOTE, KEY_QUESTION, -1, -1, -1);
        setEntry(SdlScancode.LEFTBRACKET, KEY_232, KEY_233, -1, KEY_BRACKET_LEFT, -1);
        setEntry(SdlScancode.RIGHTBRACKET, KEY_PLUS, KEY_ASTERISK, -1, KEY_BRACKET_RIGHT, -1);
        setEntry(SdlScancode.BACKSLASH, KEY_249, KEY_167, -1, KEY_BRACKET_RIGHT, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.M;
            break;
        default:
            k = SdlScancode.COMMA;
            break;
        }

        setEntry(k, KEY_COMMA, KEY_SEMICOLON, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.COMMA;
            break;
        default:
            k = SdlScancode.PERIOD;
            break;
        }

        setEntry(k, KEY_DOT, KEY_COLON, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.MINUS;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N6;
            break;
        default:
            k = SdlScancode.SLASH;
            break;
        }

        setEntry(k, KEY_MINUS, KEY_UNDERSCORE, -1, -1, -1);
    }

    /** Port of {@code keyboardBuildSpanishConfiguration} (0x4D1E24). */
    static void buildSpanishConfiguration() {
        int k;

        buildQwertyConfiguration();

        setEntry(SdlScancode.N1, KEY_1, KEY_EXCLAMATION, -1, KEY_BAR, -1);
        setEntry(SdlScancode.N2, KEY_2, KEY_QUOTE, -1, KEY_AT, -1);
        setEntry(SdlScancode.N3, KEY_3, KEY_149, -1, KEY_NUMBER_SIGN, -1);
        setEntry(SdlScancode.N6, KEY_6, KEY_AMPERSAND, -1, KEY_172, -1);
        setEntry(SdlScancode.N7, KEY_7, KEY_SLASH, -1, -1, -1);
        setEntry(SdlScancode.N8, KEY_8, KEY_PAREN_LEFT, -1, -1, -1);
        setEntry(SdlScancode.N9, KEY_9, KEY_PAREN_RIGHT, -1, -1, -1);
        setEntry(SdlScancode.N0, KEY_0, KEY_EQUAL, -1, -1, -1);
        setEntry(SdlScancode.MINUS, KEY_146, KEY_QUESTION, -1, -1, -1);
        setEntry(SdlScancode.EQUALS, KEY_161, KEY_191, -1, -1, -1);
        setEntry(SdlScancode.GRAVE, KEY_176, KEY_170, -1, KEY_BACKSLASH, -1);
        setEntry(SdlScancode.LEFTBRACKET, KEY_GRAVE, KEY_CARET, -1, KEY_BRACKET_LEFT, -1);
        setEntry(SdlScancode.RIGHTBRACKET, KEY_PLUS, KEY_ASTERISK, -1, KEY_BRACKET_RIGHT, -1);

        // gLogicalKeyEntries[DIK_OEM_102]... (commented out in C++).

        setEntry(SdlScancode.SEMICOLON, KEY_241, KEY_209, -1, -1, -1);
        setEntry(SdlScancode.APOSTROPHE, KEY_168, KEY_180, -1, KEY_BRACE_LEFT, -1);
        setEntry(SdlScancode.BACKSLASH, KEY_231, KEY_199, -1, KEY_BRACE_RIGHT, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.M;
            break;
        default:
            k = SdlScancode.COMMA;
            break;
        }

        setEntry(k, KEY_COMMA, KEY_SEMICOLON, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.COMMA;
            break;
        default:
            k = SdlScancode.PERIOD;
            break;
        }

        setEntry(k, KEY_DOT, KEY_COLON, -1, -1, -1);

        switch (keyboardLayout) {
        case KEYBOARD_LAYOUT_QWERTY:
            k = SdlScancode.MINUS;
            break;
        case KEYBOARD_LAYOUT_FRENCH:
            k = SdlScancode.N6;
            break;
        default:
            k = SdlScancode.SLASH;
            break;
        }

        setEntry(k, KEY_MINUS, KEY_UNDERSCORE, -1, -1, -1);
    }

    /** Test seam: fresh (uninstalled, zeroed) state. */
    static void resetForTests() {
        installed = false;
        keyboardDisabled = false;
        numpadDisabled = false;
        numlockDisabled = false;
        queueWriteIndex = 0;
        queueReadIndex = 0;
        modifierKeysState = 0;
        englishUsMapper = false;
        Arrays.fill(pressedPhysicalKeys, KEY_STATE_UP);
        lastKeyboardEvent.scanCode = 0;
        lastKeyboardEvent.modifiers = 0;
        keyboardLayout = KEYBOARD_LAYOUT_QWERTY;
        pressedPhysicalKeysCount = 0;
        idleStartTime = 0;
        for (int index = 0; index < logicalKeyEntries.length; index++) {
            logicalKeyEntries[index] = new LogicalKeyEntry();
        }
    }

    /** Test seam: {@code gKeyboardNumpadDisabled} has no C++ setter. */
    static void setNumpadDisabledForTests(boolean disabled) {
        numpadDisabled = disabled;
    }

    /** Test seam: {@code gKeyboardNumlockDisabled} has no C++ setter. */
    static void setNumlockDisabledForTests(boolean disabled) {
        numlockDisabled = disabled;
    }

    /** Test seam: preset layout for the unreachable table builders. */
    static void setKeyboardLayoutForTests(int layout) {
        keyboardLayout = layout;
    }

    /** Test seam: {@code gPressedPhysicalKeysCount}. */
    static int pressedPhysicalKeysCount() {
        return pressedPhysicalKeysCount;
    }
}
