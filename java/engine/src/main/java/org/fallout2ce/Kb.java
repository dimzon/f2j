package org.fallout2ce;

/**
 * Port of the key vocabulary from {@code src/kb.h} (key codes, modifiers,
 * layouts). The keyboard queue/layout engine itself ({@code kb.cc}) is not
 * ported yet; {@link KeyTranslator} maps AWT events onto these codes.
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
}
