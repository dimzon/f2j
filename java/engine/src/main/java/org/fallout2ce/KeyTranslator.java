package org.fallout2ce;

import java.awt.event.KeyEvent;

/**
 * Maps AWT key events onto game {@link Kb} codes, feeding
 * {@link Input#enqueueInputEvent}.
 *
 * <p>Unlike the C scancode path (redone per layout in {@code kb.cc}),
 * printable characters come from the OS via {@code getKeyChar} (shift and
 * layouts handled for free; Ctrl codes arrive as 1..26 and match
 * {@code KEY_CTRL_*} directly). Function and navigation keys map by
 * {@code getKeyCode} with Ctrl > Alt > Shift precedence. Numpad keys map
 * to their symbols (numlock nuances deferred to the full {@code kb.cc}
 * port). Unmapped keys yield -1 (dropped by the queue, as in C).
 */
public final class KeyTranslator {

    private KeyTranslator() {
    }

    /** Port of the C keyboard-dequeue mapping (AWT edition). */
    public static int toLogicalKey(KeyEvent event) {
        int ch = event.getKeyChar();
        if (ch != KeyEvent.CHAR_UNDEFINED) {
            if (ch == '\n') {
                return Kb.KEY_RETURN;
            }
            return ch;
        }

        boolean ctrl = event.isControlDown();
        boolean alt = event.isAltDown();
        boolean shift = event.isShiftDown();

        switch (event.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                return Kb.KEY_ESCAPE;
            case KeyEvent.VK_TAB:
                return Kb.KEY_TAB;
            case KeyEvent.VK_BACK_SPACE:
                return Kb.KEY_BACKSPACE;
            case KeyEvent.VK_ENTER:
                return Kb.KEY_RETURN;
            case KeyEvent.VK_DELETE:
                return Kb.KEY_DELETE;
            case KeyEvent.VK_F1:
                return fKey(Kb.KEY_F1, Kb.KEY_SHIFT_F1, Kb.KEY_CTRL_F1, Kb.KEY_ALT_F1,
                    shift, ctrl, alt);
            case KeyEvent.VK_F2:
                return fKey(Kb.KEY_F2, Kb.KEY_SHIFT_F2, Kb.KEY_CTRL_F2, Kb.KEY_ALT_F2,
                    shift, ctrl, alt);
            case KeyEvent.VK_F3:
                return fKey(Kb.KEY_F3, Kb.KEY_SHIFT_F3, Kb.KEY_CTRL_F3, Kb.KEY_ALT_F3,
                    shift, ctrl, alt);
            case KeyEvent.VK_F4:
                return fKey(Kb.KEY_F4, Kb.KEY_SHIFT_F4, Kb.KEY_CTRL_F4, Kb.KEY_ALT_F4,
                    shift, ctrl, alt);
            case KeyEvent.VK_F5:
                return fKey(Kb.KEY_F5, Kb.KEY_SHIFT_F5, Kb.KEY_CTRL_F5, Kb.KEY_ALT_F5,
                    shift, ctrl, alt);
            case KeyEvent.VK_F6:
                return fKey(Kb.KEY_F6, Kb.KEY_SHIFT_F6, Kb.KEY_CTRL_F6, Kb.KEY_ALT_F6,
                    shift, ctrl, alt);
            case KeyEvent.VK_F7:
                return fKey(Kb.KEY_F7, Kb.KEY_SHIFT_F7, Kb.KEY_CTRL_F7, Kb.KEY_ALT_F7,
                    shift, ctrl, alt);
            case KeyEvent.VK_F8:
                return fKey(Kb.KEY_F8, Kb.KEY_SHIFT_F8, Kb.KEY_CTRL_F8, Kb.KEY_ALT_F8,
                    shift, ctrl, alt);
            case KeyEvent.VK_F9:
                return fKey(Kb.KEY_F9, Kb.KEY_SHIFT_F9, Kb.KEY_CTRL_F9, Kb.KEY_ALT_F9,
                    shift, ctrl, alt);
            case KeyEvent.VK_F10:
                return fKey(Kb.KEY_F10, Kb.KEY_SHIFT_F10, Kb.KEY_CTRL_F10, Kb.KEY_ALT_F10,
                    shift, ctrl, alt);
            case KeyEvent.VK_F11:
                return fKey(Kb.KEY_F11, Kb.KEY_SHIFT_F11, Kb.KEY_CTRL_F11, Kb.KEY_ALT_F11,
                    shift, ctrl, alt);
            case KeyEvent.VK_F12:
                return fKey(Kb.KEY_F12, Kb.KEY_SHIFT_F12, Kb.KEY_CTRL_F12, Kb.KEY_ALT_F12,
                    shift, ctrl, alt);
            case KeyEvent.VK_UP:
                return navKey(Kb.KEY_ARROW_UP, Kb.KEY_CTRL_ARROW_UP, Kb.KEY_ALT_ARROW_UP,
                    ctrl, alt);
            case KeyEvent.VK_DOWN:
                return navKey(Kb.KEY_ARROW_DOWN, Kb.KEY_CTRL_ARROW_DOWN, Kb.KEY_ALT_ARROW_DOWN,
                    ctrl, alt);
            case KeyEvent.VK_LEFT:
                return navKey(Kb.KEY_ARROW_LEFT, Kb.KEY_CTRL_ARROW_LEFT, Kb.KEY_ALT_ARROW_LEFT,
                    ctrl, alt);
            case KeyEvent.VK_RIGHT:
                return navKey(Kb.KEY_ARROW_RIGHT, Kb.KEY_CTRL_ARROW_RIGHT, Kb.KEY_ALT_ARROW_RIGHT,
                    ctrl, alt);
            case KeyEvent.VK_HOME:
                return navKey(Kb.KEY_HOME, Kb.KEY_CTRL_HOME, Kb.KEY_ALT_HOME, ctrl, alt);
            case KeyEvent.VK_END:
                return navKey(Kb.KEY_END, Kb.KEY_CTRL_END, Kb.KEY_ALT_END, ctrl, alt);
            case KeyEvent.VK_PAGE_UP:
                return navKey(Kb.KEY_PAGE_UP, Kb.KEY_CTRL_PAGE_UP, Kb.KEY_ALT_PAGE_UP, ctrl, alt);
            case KeyEvent.VK_PAGE_DOWN:
                return navKey(Kb.KEY_PAGE_DOWN, Kb.KEY_CTRL_PAGE_DOWN, Kb.KEY_ALT_PAGE_DOWN,
                    ctrl, alt);
            case KeyEvent.VK_INSERT:
                return navKey(Kb.KEY_INSERT, Kb.KEY_CTRL_INSERT, Kb.KEY_ALT_INSERT, ctrl, alt);
            case KeyEvent.VK_NUMPAD0:
                return Kb.KEY_0;
            case KeyEvent.VK_NUMPAD1:
                return Kb.KEY_1;
            case KeyEvent.VK_NUMPAD2:
                return Kb.KEY_2;
            case KeyEvent.VK_NUMPAD3:
                return Kb.KEY_3;
            case KeyEvent.VK_NUMPAD4:
                return Kb.KEY_4;
            case KeyEvent.VK_NUMPAD5:
                return Kb.KEY_5;
            case KeyEvent.VK_NUMPAD6:
                return Kb.KEY_6;
            case KeyEvent.VK_NUMPAD7:
                return Kb.KEY_7;
            case KeyEvent.VK_NUMPAD8:
                return Kb.KEY_8;
            case KeyEvent.VK_NUMPAD9:
                return Kb.KEY_9;
            case KeyEvent.VK_MULTIPLY:
                return Kb.KEY_ASTERISK;
            case KeyEvent.VK_ADD:
                return Kb.KEY_PLUS;
            case KeyEvent.VK_SUBTRACT:
                return Kb.KEY_MINUS;
            case KeyEvent.VK_DECIMAL:
                return Kb.KEY_DOT;
            case KeyEvent.VK_DIVIDE:
                return Kb.KEY_SLASH;
            default:
                break;
        }

        if (alt) {
            switch (event.getKeyCode()) {
                case KeyEvent.VK_A:
                    return Kb.KEY_ALT_A;
                case KeyEvent.VK_B:
                    return Kb.KEY_ALT_B;
                case KeyEvent.VK_C:
                    return Kb.KEY_ALT_C;
                case KeyEvent.VK_D:
                    return Kb.KEY_ALT_D;
                case KeyEvent.VK_E:
                    return Kb.KEY_ALT_E;
                case KeyEvent.VK_F:
                    return Kb.KEY_ALT_F;
                case KeyEvent.VK_G:
                    return Kb.KEY_ALT_G;
                case KeyEvent.VK_H:
                    return Kb.KEY_ALT_H;
                case KeyEvent.VK_I:
                    return Kb.KEY_ALT_I;
                case KeyEvent.VK_J:
                    return Kb.KEY_ALT_J;
                case KeyEvent.VK_K:
                    return Kb.KEY_ALT_K;
                case KeyEvent.VK_L:
                    return Kb.KEY_ALT_L;
                case KeyEvent.VK_M:
                    return Kb.KEY_ALT_M;
                case KeyEvent.VK_N:
                    return Kb.KEY_ALT_N;
                case KeyEvent.VK_O:
                    return Kb.KEY_ALT_O;
                case KeyEvent.VK_P:
                    return Kb.KEY_ALT_P;
                case KeyEvent.VK_Q:
                    return Kb.KEY_ALT_Q;
                case KeyEvent.VK_R:
                    return Kb.KEY_ALT_R;
                case KeyEvent.VK_S:
                    return Kb.KEY_ALT_S;
                case KeyEvent.VK_T:
                    return Kb.KEY_ALT_T;
                case KeyEvent.VK_U:
                    return Kb.KEY_ALT_U;
                case KeyEvent.VK_V:
                    return Kb.KEY_ALT_V;
                case KeyEvent.VK_W:
                    return Kb.KEY_ALT_W;
                case KeyEvent.VK_X:
                    return Kb.KEY_ALT_X;
                case KeyEvent.VK_Y:
                    return Kb.KEY_ALT_Y;
                case KeyEvent.VK_Z:
                    return Kb.KEY_ALT_Z;
                default:
                    break;
            }
        }

        return -1;
    }

    private static int fKey(int plain, int shift, int ctrl, int alt,
            boolean isShift, boolean isCtrl, boolean isAlt) {
        if (isCtrl) {
            return ctrl;
        }
        if (isAlt) {
            return alt;
        }
        if (isShift) {
            return shift;
        }
        return plain;
    }

    private static int navKey(int plain, int ctrl, int alt, boolean isCtrl, boolean isAlt) {
        if (isCtrl) {
            return ctrl;
        }
        if (isAlt) {
            return alt;
        }
        return plain;
    }
}
