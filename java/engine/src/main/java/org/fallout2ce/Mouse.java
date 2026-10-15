package org.fallout2ce;

/**
 * Mouse constants, mirroring {@code src/mouse.h}.
 * The mouse engine ({@code mouse.cc}) comes with the UI phase.
 */
public final class Mouse {

    private Mouse() {
    }

    public static final int DEFAULT_CURSOR_WIDTH = 8;
    public static final int DEFAULT_CURSOR_HEIGHT = 8;
    public static final int DEFAULT_CURSOR_SIZE = DEFAULT_CURSOR_WIDTH * DEFAULT_CURSOR_HEIGHT;

    public static final int STATE_LEFT_BUTTON_DOWN = 0x01;
    public static final int STATE_RIGHT_BUTTON_DOWN = 0x02;

    public static final int EVENT_LEFT_BUTTON_DOWN = 0x01;
    public static final int EVENT_RIGHT_BUTTON_DOWN = 0x02;
    public static final int EVENT_LEFT_BUTTON_REPEAT = 0x04;
    public static final int EVENT_RIGHT_BUTTON_REPEAT = 0x08;
    public static final int EVENT_LEFT_BUTTON_UP = 0x10;
    public static final int EVENT_RIGHT_BUTTON_UP = 0x20;
    public static final int EVENT_ANY_BUTTON_DOWN = EVENT_LEFT_BUTTON_DOWN | EVENT_RIGHT_BUTTON_DOWN;
    public static final int EVENT_ANY_BUTTON_REPEAT = EVENT_LEFT_BUTTON_REPEAT | EVENT_RIGHT_BUTTON_REPEAT;
    public static final int EVENT_ANY_BUTTON_UP = EVENT_LEFT_BUTTON_UP | EVENT_RIGHT_BUTTON_UP;
    public static final int EVENT_LEFT_BUTTON_DOWN_REPEAT =
        EVENT_LEFT_BUTTON_DOWN | EVENT_LEFT_BUTTON_REPEAT;
    public static final int EVENT_RIGHT_BUTTON_DOWN_REPEAT =
        EVENT_RIGHT_BUTTON_DOWN | EVENT_RIGHT_BUTTON_REPEAT;
    public static final int EVENT_WHEEL = 0x40;

    public static final int BUTTON_REPEAT_TIME = 250;

    public static final double SENSITIVITY_MIN = 0.25;
    public static final double SENSITIVITY_MAX = 2.5;
}
