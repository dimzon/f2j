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

    // ===== mouse engine (mouse.cc) =====
    //
    // The iOS tap-through helper (handleHudTapThrough, Apple-only) is not
    // ported. Out-of-range simulate paths that are UB in C++ are guarded.

    private static final byte[] DEFAULT_CURSOR_CODES = {
        1, 1, 1, 1, 1, 1, 1, 0,
        1, 15, 15, 15, 15, 15, 1, 0,
        1, 15, 15, 15, 15, 1, 1, 0,
        1, 15, 15, 15, 15, 1, 1, 0,
        1, 15, 15, 15, 15, 15, 1, 1,
        1, 15, 1, 1, 15, 15, 15, 1,
        1, 1, 1, 1, 1, 15, 15, 1,
        0, 0, 0, 0, 1, 1, 1, 1,
    };

    private static final byte[] defaultCursor = DEFAULT_CURSOR_CODES.clone();

    private static boolean mouseIdling;
    private static byte[] mouseCursorData;
    private static byte[] mouseShape;
    private static int mouseShapeOffset;
    private static byte[] mouseFramePtr;
    private static double mouseSensitivity = 1.0;
    private static int lastButtons;
    private static boolean cursorHidden = true;
    private static int rawX;
    private static int rawY;
    private static int mouseCursorHeight;
    private static int mouseCursorY;
    private static int mouseCursorX;
    // No setter exists in C++; always false after init.
    private static boolean mouseDisabled;
    private static int mouseEvent;
    private static int mouseSpeed;
    private static int mouseCurrentFrame;
    private static boolean mouseInitialized;
    private static int mouseCursorPitch;
    private static int mouseCursorWidth;
    private static int mouseNumFrames;
    private static int mouseHotY;
    private static int mouseHotX;
    private static int mouseIdleStartTime;
    private static int mouseAnimTicker;

    /** Port of {@code _mouse_blit_trans} (set by the video layer). */
    public static Window.WindowDrawingProc2 mouseBlitTrans;

    /** Port of {@code _mouse_blit} (set by the video layer). */
    public static Window.WindowDrawingProc mouseBlit;

    private static byte mouseTransparent;
    private static int mouseWheelX;
    private static int mouseWheelY;

    private static int previousRightButtonTimestamp;
    private static int previousLeftButtonTimestamp;
    private static int previousEvent;
    private static boolean touchShiftHeld;
    private static int touchSwipeStartY;
    private static int touchPrevX;
    private static int touchPrevY;

    /** Port of {@code mouseInit} (0x4C9F40). */
    public static int mouseInit() {
        mouseInitialized = false;
        mouseDisabled = false;

        cursorHidden = true;

        mousePrepareDefaultCursor();

        if (mouseSetFrame(null, 0, 0, 0, 0, 0, 0) == -1) {
            return -1;
        }

        if (!DInput.mouseDeviceAcquire()) {
            return -1;
        }

        mouseInitialized = true;
        mouseCursorX = Svga.scrSize.right / 2;
        mouseCursorY = Svga.scrSize.bottom / 2;
        rawX = Svga.scrSize.right / 2;
        rawY = Svga.scrSize.bottom / 2;
        mouseIdleStartTime = (int) Ticks.getTicks();

        return 0;
    }

    /** Port of {@code mouseFree} (0x4C9FD8; leaves initialized set, like C++). */
    public static void mouseFree() {
        DInput.mouseDeviceUnacquire();

        mouseCursorData = null;

        if (mouseFramePtr != null) {
            Input.tickersRemove(Mouse::mouseAnim);
            mouseFramePtr = null;
        }
    }

    /** Port of {@code mousePrepareDefaultCursor} (0x4CA01C). */
    private static void mousePrepareDefaultCursor() {
        int black = Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0x000000)]);
        int darkGrey = Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0x424242)]);
        int white = Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0xFFFFFF)]);
        for (int index = 0; index < 64; index++) {
            switch (Byte.toUnsignedInt(defaultCursor[index])) {
            case 0:
                defaultCursor[index] = (byte) black;
                break;
            case 1:
                defaultCursor[index] = (byte) darkGrey;
                break;
            case 15:
                defaultCursor[index] = (byte) white;
                break;
            default:
                break;
            }
        }
    }

    /** Port of {@code mouseSetFrame} (0x4CA0AC). */
    public static int mouseSetFrame(byte[] frame, int width, int height, int pitch, int hotX, int hotY,
            int transparentColor) {
        byte[] cursorFrame = frame;

        if (frame == null) {
            // NOTE: Original code looks tail recursion optimization.
            return mouseSetFrame(defaultCursor, DEFAULT_CURSOR_WIDTH, DEFAULT_CURSOR_HEIGHT,
                DEFAULT_CURSOR_WIDTH, 1, 1, Color.colorTable[Color.rgb555(0x000000)] & 0xFF);
        }

        boolean cursorWasHidden = cursorHidden;
        if (!cursorHidden && mouseInitialized) {
            cursorHidden = true;
            Geometry.Rect rect = new Geometry.Rect();
            mouseGetRect(rect);
            WindowManager.windowRefreshAll(rect);
        }

        if (width != mouseCursorWidth || height != mouseCursorHeight) {
            mouseCursorData = new byte[width * height];
        }

        mouseCursorWidth = width;
        mouseCursorHeight = height;
        mouseCursorPitch = pitch;
        mouseShape = cursorFrame;
        mouseShapeOffset = 0;
        mouseTransparent = (byte) transparentColor;

        if (mouseFramePtr != null) {
            Input.tickersRemove(Mouse::mouseAnim);
            mouseFramePtr = null;
        }

        int hotXDelta = mouseHotX - hotX;
        mouseHotX = hotX;

        mouseCursorX += hotXDelta;

        int hotYDelta = mouseHotY - hotY;
        mouseHotY = hotY;

        mouseCursorY += hotYDelta;

        mouseClip();

        if (!cursorWasHidden) {
            mouseShowCursor();
        }

        rawX = mouseCursorX;
        rawY = mouseCursorY;

        return 0;
    }

    /**
     * Port of {@code _mouse_anim} (0x4CA2D0). NOTE: Looks like this code is
     * not reachable (nothing registers the ticker in C++ either).
     */
    private static void mouseAnim() {
        if (Ticks.getTicksSince(mouseAnimTicker) >= mouseSpeed) {
            mouseAnimTicker = (int) Ticks.getTicks();

            mouseCurrentFrame++;
            if (mouseCurrentFrame == mouseNumFrames) {
                mouseCurrentFrame = 0;
            }

            int base = mouseCursorWidth * mouseCurrentFrame * mouseCursorHeight;
            mouseShape = mouseFramePtr;
            mouseShapeOffset = base;

            if (!cursorHidden) {
                mouseShowCursor();
            }
        }
    }

    /** Port of {@code mouseShowCursor} (0x4CA34C). */
    public static void mouseShowCursor() {
        byte[] cursorData = mouseCursorData;
        if (mouseInitialized) {
            if (mouseBlitTrans == null || cursorHidden) {
                WindowManager.winGetMouseBuf(mouseCursorData);
                cursorData = mouseCursorData;
                int cursorDataIndex = 0;

                for (int y = 0; y < mouseCursorHeight; y++) {
                    for (int x = 0; x < mouseCursorWidth; x++) {
                        int pixel = Byte.toUnsignedInt(mouseShape[mouseShapeOffset + y * mouseCursorPitch + x]);
                        if (pixel != Byte.toUnsignedInt(mouseTransparent)) {
                            cursorData[cursorDataIndex] = (byte) pixel;
                        }
                        cursorDataIndex++;
                    }
                }
            }

            int clipX;
            int clipWidth;
            if (mouseCursorX >= Svga.scrSize.left) {
                if (mouseCursorWidth + mouseCursorX - 1 <= Svga.scrSize.right) {
                    clipWidth = mouseCursorWidth;
                    clipX = 0;
                } else {
                    clipX = 0;
                    clipWidth = Svga.scrSize.right - mouseCursorX + 1;
                }
            } else {
                clipX = Svga.scrSize.left - mouseCursorX;
                clipWidth = mouseCursorWidth - (Svga.scrSize.left - mouseCursorX);
            }

            int clipY;
            int clipHeight;
            if (mouseCursorY >= Svga.scrSize.top) {
                if (mouseCursorHeight + mouseCursorY - 1 <= Svga.scrSize.bottom) {
                    clipY = 0;
                    clipHeight = mouseCursorHeight;
                } else {
                    clipY = 0;
                    clipHeight = Svga.scrSize.bottom - mouseCursorY + 1;
                }
            } else {
                clipY = Svga.scrSize.top - mouseCursorY;
                clipHeight = mouseCursorHeight - (Svga.scrSize.top - mouseCursorY);
            }

            mouseCursorData = cursorData;
            if (mouseBlitTrans != null && cursorHidden) {
                mouseBlitTrans.draw(mouseShape, mouseShapeOffset, mouseCursorPitch, mouseCursorHeight,
                    clipX, clipY, clipWidth, clipHeight, clipX + mouseCursorX, clipY + mouseCursorY,
                    mouseTransparent);
            } else {
                mouseBlit.draw(mouseCursorData, mouseCursorWidth, mouseCursorHeight, clipX, clipY,
                    clipWidth, clipHeight, clipX + mouseCursorX, clipY + mouseCursorY);
            }

            cursorData = mouseCursorData;
            cursorHidden = false;
        }
        mouseCursorData = cursorData;
    }

    /** Port of {@code mouseHideCursor} (0x4CA534). */
    public static void mouseHideCursor() {
        if (mouseInitialized) {
            if (!cursorHidden) {
                Geometry.Rect rect = new Geometry.Rect();
                rect.left = mouseCursorX;
                rect.top = mouseCursorY;
                rect.right = mouseCursorX + mouseCursorWidth - 1;
                rect.bottom = mouseCursorY + mouseCursorHeight - 1;

                cursorHidden = true;
                WindowManager.windowRefreshAll(rect);
            }
        }
    }

    /** Port of {@code _mouse_info} (0x4CA59C). */
    public static void mouseInfo() {
        if (!mouseInitialized) {
            return;
        }

        if (cursorHidden) {
            return;
        }

        if (mouseDisabled) {
            return;
        }

        Touch.Gesture gesture = new Touch.Gesture();
        if (Touch.touchGetGesture(gesture)) {
            // Multi-finger gestures for keyboard-less touch play:
            //   3-finger swipe down → ESC (options menu)
            //   3-finger long press → hold Left Shift (highlights interactables)
            //   4-finger long press → F6  (quicksave)
            if (gesture.type == Touch.GestureType.kPan && gesture.numberOfTouches == 3) {
                if (gesture.state == Touch.GestureState.kBegan) {
                    touchSwipeStartY = gesture.y;
                } else if (gesture.state == Touch.GestureState.kEnded) {
                    int dy = gesture.y - touchSwipeStartY;
                    if (dy > Svga.screenGetHeight() / 4) {
                        Input.enqueueInputEvent(Kb.KEY_ESCAPE);
                    }
                }
                return;
            }

            // Four-finger long press → F6 (quicksave). Long-press is more
            // reliable than a tap since all 4 fingers rarely land and lift
            // within the 75ms tap window; and more reliable than a swipe
            // since iPadOS intercepts multi-finger vertical swipes.
            if (gesture.type == Touch.GestureType.kLongPress && gesture.numberOfTouches == 4) {
                if (gesture.state == Touch.GestureState.kBegan) {
                    Input.enqueueInputEvent(Kb.KEY_F6);
                }
                return;
            }

            // FO2tweaks' highlighting uses sfall's key_pressed(), which reads
            // SDL's own SDL_GetKeyboardState. Engine-internal _kb_simulate_key
            // bypasses that, so push real key events instead (via the input
            // event queue, like SDL_PushEvent).
            if (gesture.type == Touch.GestureType.kLongPress && gesture.numberOfTouches == 3) {
                if (gesture.state == Touch.GestureState.kBegan && !touchShiftHeld) {
                    Input.postKeyEvent(SdlScancode.LSHIFT, true);
                    touchShiftHeld = true;
                } else if (gesture.state == Touch.GestureState.kEnded && touchShiftHeld) {
                    Input.postKeyEvent(SdlScancode.LSHIFT, false);
                    touchShiftHeld = false;
                }
                return;
            }

            switch (gesture.type) {
            case kTap: {
                // Toolbar taps bypass the mouse pipeline entirely: the handler
                // invokes the action in place, so the cursor never moves.
                // Skip when touchscreen mode is active (dialog, inventory, etc.)
                // so toolbar doesn't intercept taps meant for overlapping UI.
                if (!Touch.touchGetTouchscreenMode()
                        && gesture.numberOfTouches == 1
                        && QuickToolbar.toolbar.containsPoint(gesture.x, gesture.y)) {
                    if (QuickToolbar.toolbar.handleTap(gesture.x, gesture.y)) {
                        break;
                    }
                }

                if (DInput.mouseDeviceUsesRelativeMode()) {
                    if (gesture.numberOfTouches == 1) {
                        mouseSimulateInput(0, 0, STATE_LEFT_BUTTON_DOWN);
                    } else if (gesture.numberOfTouches == 2) {
                        mouseSimulateInput(0, 0, STATE_RIGHT_BUTTON_DOWN);
                    }
                } else {
                    mouseSetPosition(gesture.x, gesture.y);
                    if (gesture.numberOfTouches == 1) {
                        mouseSimulateInput(gesture.x, gesture.y, STATE_LEFT_BUTTON_DOWN);
                    } else if (gesture.numberOfTouches == 2) {
                        mouseSimulateInput(gesture.x, gesture.y, STATE_RIGHT_BUTTON_DOWN);
                    }
                }
                break;
            }
            case kLongPress:
            case kPan:
                if (gesture.state == Touch.GestureState.kBegan) {
                    touchPrevX = gesture.x;
                    touchPrevY = gesture.y;
                }
                if (!DInput.mouseDeviceUsesRelativeMode()) {
                    touchPrevX = 0;
                    touchPrevY = 0;
                }

                if (gesture.type == Touch.GestureType.kLongPress) {
                    if (gesture.numberOfTouches == 1) {
                        mouseSimulateInput(gesture.x - touchPrevX, gesture.y - touchPrevY,
                            STATE_LEFT_BUTTON_DOWN);
                    } else if (gesture.numberOfTouches == 2) {
                        mouseSimulateInput(gesture.x - touchPrevX, gesture.y - touchPrevY,
                            STATE_RIGHT_BUTTON_DOWN);
                    }
                } else if (gesture.type == Touch.GestureType.kPan) {
                    if (!Touch.touchGetPanMode() && gesture.numberOfTouches == 1) {
                        mouseSimulateInput(gesture.x - touchPrevX, gesture.y - touchPrevY, 0);
                    } else if (Touch.touchGetPanMode() || gesture.numberOfTouches == 2) {
                        int coefficient = Touch.touchGetPanMode() ? 8 : 2;
                        mouseWheelX = (touchPrevX - gesture.x) / coefficient;
                        mouseWheelY = (gesture.y - touchPrevY) / coefficient;

                    if (mouseWheelX != 0 || mouseWheelY != 0) {
                        mouseEvent |= EVENT_WHEEL;
                        rawButtons |= EVENT_WHEEL;
                    }
                    }
                }

                touchPrevX = gesture.x;
                touchPrevY = gesture.y;
                break;
            case kUnrecognized:
                break;
            default:
                break;
            }

            return;
        }

        int x;
        int y;
        int buttons = 0;

        DInput.MouseData mouseData = DInput.mouseDeviceGetData();
        if (mouseData != null) {
            x = mouseData.x();
            y = mouseData.y();

            if (mouseData.left()) {
                buttons |= STATE_LEFT_BUTTON_DOWN;
            }

            if (mouseData.right()) {
                buttons |= STATE_RIGHT_BUTTON_DOWN;
            }
        } else {
            x = 0;
            y = 0;
        }

        // Mouse sensitivity only applies to relative movement. In windowed mode
        // SDL provides absolute coordinates that should not be scaled.
        if (DInput.mouseDeviceUsesRelativeMode()) {
            x = (int) (x * mouseSensitivity);
            y = (int) (y * mouseSensitivity);
        }

        mouseSimulateInput(x, y, buttons);

        // TODO: Move to `_mouse_simulate_input`.
        mouseWheelX = mouseData != null ? mouseData.wheelX() : 0;
        mouseWheelY = mouseData != null ? mouseData.wheelY() : 0;

        if (mouseWheelX != 0 || mouseWheelY != 0) {
            mouseEvent |= EVENT_WHEEL;
            rawButtons |= EVENT_WHEEL;
        }
    }

    private static int rawButtons;

    /** Port of {@code _mouse_simulate_input} (0x4CA698). */
    public static void mouseSimulateInput(int deltaX, int deltaY, int buttons) {
        if (!mouseInitialized || cursorHidden) {
            return;
        }

        if (deltaX == 0 && deltaY == 0 && buttons == lastButtons) {
            if (lastButtons == 0) {
                if (!mouseIdling) {
                    mouseIdleStartTime = (int) Ticks.getTicks();
                    mouseIdling = true;
                }

                lastButtons = 0;
                rawButtons = 0;
                mouseEvent = 0;

                return;
            }
        }

        mouseIdling = false;
        lastButtons = buttons;
        previousEvent = mouseEvent;
        mouseEvent = 0;

        if ((previousEvent & (EVENT_LEFT_BUTTON_DOWN | EVENT_LEFT_BUTTON_REPEAT)) != 0) {
            if ((buttons & 0x01) != 0) {
                mouseEvent |= EVENT_LEFT_BUTTON_REPEAT;

                if (Ticks.getTicksSince(previousLeftButtonTimestamp) > BUTTON_REPEAT_TIME) {
                    mouseEvent |= EVENT_LEFT_BUTTON_DOWN;
                    previousLeftButtonTimestamp = (int) Ticks.getTicks();
                }
            } else {
                mouseEvent |= EVENT_LEFT_BUTTON_UP;
            }
        } else {
            if ((buttons & 0x01) != 0) {
                mouseEvent |= EVENT_LEFT_BUTTON_DOWN;
                previousLeftButtonTimestamp = (int) Ticks.getTicks();
            }
        }

        if ((previousEvent & (EVENT_RIGHT_BUTTON_DOWN | EVENT_RIGHT_BUTTON_REPEAT)) != 0) {
            if ((buttons & 0x02) != 0) {
                mouseEvent |= EVENT_RIGHT_BUTTON_REPEAT;
                if (Ticks.getTicksSince(previousRightButtonTimestamp) > BUTTON_REPEAT_TIME) {
                    mouseEvent |= EVENT_RIGHT_BUTTON_DOWN;
                    previousRightButtonTimestamp = (int) Ticks.getTicks();
                }
            } else {
                mouseEvent |= EVENT_RIGHT_BUTTON_UP;
            }
        } else {
            if ((buttons & 0x02) != 0) {
                mouseEvent |= EVENT_RIGHT_BUTTON_DOWN;
                previousRightButtonTimestamp = (int) Ticks.getTicks();
            }
        }

        rawButtons = mouseEvent;

        if (deltaX != 0 || deltaY != 0) {
            Geometry.Rect mouseRect = new Geometry.Rect();
            mouseRect.left = mouseCursorX;
            mouseRect.top = mouseCursorY;
            mouseRect.right = mouseCursorWidth + mouseCursorX - 1;
            mouseRect.bottom = mouseCursorHeight + mouseCursorY - 1;
            if (DInput.mouseDeviceUsesRelativeMode()) {
                mouseCursorX += deltaX;
                mouseCursorY += deltaY;
            } else {
                mouseSetPosition(deltaX, deltaY);
            }
            mouseClip();

            WindowManager.windowRefreshAll(mouseRect);

            mouseShowCursor();

            if (DInput.mouseDeviceUsesRelativeMode()) {
                rawX = mouseCursorX;
                rawY = mouseCursorY;
            } else {
                rawX = deltaX;
                rawY = deltaY;
            }
        }
    }

    /** Port of {@code _mouse_in} (0x4CA8C8). */
    public static boolean mouseIn(int left, int top, int right, int bottom) {
        if (!mouseInitialized) {
            return false;
        }

        return mouseCursorHeight + mouseCursorY > top
            && right >= mouseCursorX
            && mouseCursorWidth + mouseCursorX > left
            && bottom >= mouseCursorY;
    }

    /** Port of {@code _mouse_click_in} (0x4CA934). */
    public static boolean mouseClickIn(int left, int top, int right, int bottom) {
        if (!mouseInitialized) {
            return false;
        }

        return mouseHotY + mouseCursorY >= top
            && mouseHotX + mouseCursorX <= right
            && mouseHotX + mouseCursorX >= left
            && mouseHotY + mouseCursorY <= bottom;
    }

    /** Port of {@code mouseGetRect} (0x4CA9A0). */
    public static void mouseGetRect(Geometry.Rect rect) {
        rect.left = mouseCursorX;
        rect.top = mouseCursorY;
        rect.right = mouseCursorWidth + mouseCursorX - 1;
        rect.bottom = mouseCursorHeight + mouseCursorY - 1;
    }

    /** Port of {@code mouseGetPosition} (0x4CA9DC). */
    public static void mouseGetPosition(int[] out) {
        out[0] = mouseHotX + mouseCursorX;
        out[1] = mouseHotY + mouseCursorY;
    }

    /** Port of {@code _mouse_set_position} (0x4CAA04). */
    public static void mouseSetPosition(int x, int y) {
        mouseCursorX = x - mouseHotX;
        mouseCursorY = y - mouseHotY;
        rawY = y - mouseHotY;
        rawX = x - mouseHotX;
        mouseClip();
    }

    /** Port of {@code _mouse_clip} (0x4CAA38). */
    private static void mouseClip() {
        if (mouseHotX + mouseCursorX < Svga.scrSize.left) {
            mouseCursorX = Svga.scrSize.left - mouseHotX;
        } else if (mouseHotX + mouseCursorX > Svga.scrSize.right) {
            mouseCursorX = Svga.scrSize.right - mouseHotX;
        }

        if (mouseHotY + mouseCursorY < Svga.scrSize.top) {
            mouseCursorY = Svga.scrSize.top - mouseHotY;
        } else if (mouseHotY + mouseCursorY > Svga.scrSize.bottom) {
            mouseCursorY = Svga.scrSize.bottom - mouseHotY;
        }
    }

    /** Port of {@code mouseGetEvent} (0x4CAAA0). */
    public static int mouseGetEvent() {
        return mouseEvent;
    }

    /** Port of {@code cursorIsHidden} (0x4CAAA8). */
    public static boolean cursorIsHidden() {
        return cursorHidden;
    }

    /** Port of {@code _mouse_get_raw_state} (0x4CAB5C). */
    public static void mouseGetRawState(int[] out) {
        DInput.MouseData mouseData = DInput.mouseDeviceGetData();
        int dx;
        int dy;
        boolean left;
        boolean right;
        if (mouseData != null) {
            dx = mouseData.x();
            dy = mouseData.y();
            left = mouseData.left();
            right = mouseData.right();
        } else {
            dx = 0;
            dy = 0;
            left = (mouseEvent & EVENT_LEFT_BUTTON_DOWN) != 0;
            right = (mouseEvent & EVENT_RIGHT_BUTTON_DOWN) != 0;
        }

        rawButtons = 0;
        if (DInput.mouseDeviceUsesRelativeMode()) {
            rawX += dx;
            rawY += dy;
        } else {
            rawX = dx;
            rawY = dy;
        }

        if (left) {
            rawButtons |= EVENT_LEFT_BUTTON_DOWN;
        }

        if (right) {
            rawButtons |= EVENT_RIGHT_BUTTON_DOWN;
        }

        out[0] = rawX;
        out[1] = rawY;
        out[2] = rawButtons;
    }

    /** Port of {@code mouseSetSensitivity} (0x4CAC3C). */
    public static void mouseSetSensitivity(double value) {
        if (value >= SENSITIVITY_MIN && value <= SENSITIVITY_MAX) {
            mouseSensitivity = value;
        }
    }

    /** Port of {@code mouseGetPositionInWindow}. */
    public static void mouseGetPositionInWindow(int win, int[] out) {
        mouseGetPosition(out);

        WindowManager.Window window = WindowManager.windowGetWindow(win);
        if (window != null) {
            out[0] -= window.rect.left;
            out[1] -= window.rect.top;
        }
    }

    /** Port of {@code mouseHitTestInWindow}. */
    public static boolean mouseHitTestInWindow(int win, int left, int top, int right, int bottom) {
        WindowManager.Window window = WindowManager.windowGetWindow(win);
        if (window != null) {
            left += window.rect.left;
            top += window.rect.top;
            right += window.rect.left;
            bottom += window.rect.top;
        }

        return mouseClickIn(left, top, right, bottom);
    }

    /** Port of {@code mouseGetWheel}. */
    public static void mouseGetWheel(int[] out) {
        out[0] = mouseWheelX;
        out[1] = mouseWheelY;
    }

    /** Port of {@code convertMouseWheelToArrowKey}. */
    public static void convertMouseWheelToArrowKey(int[] keyCodePtr) {
        if (keyCodePtr[0] == -1) {
            if ((mouseGetEvent() & EVENT_WHEEL) != 0) {
                int[] wheel = new int[2];
                mouseGetWheel(wheel);

                if (wheel[1] > 0) {
                    keyCodePtr[0] = Kb.KEY_ARROW_UP;
                } else if (wheel[1] < 0) {
                    keyCodePtr[0] = Kb.KEY_ARROW_DOWN;
                }
            }
        }
    }

    /** Port of {@code mouse_get_last_buttons}. */
    public static int mouseGetLastButtons() {
        return lastButtons;
    }

    /** Test seam: fresh (uninitialized) mouse state. */
    static void resetForTests() {
        mouseIdling = false;
        mouseCursorData = null;
        mouseShape = null;
        mouseShapeOffset = 0;
        mouseFramePtr = null;
        mouseSensitivity = 1.0;
        lastButtons = 0;
        cursorHidden = true;
        rawX = 0;
        rawY = 0;
        mouseCursorHeight = 0;
        mouseCursorY = 0;
        mouseCursorX = 0;
        mouseDisabled = false;
        mouseEvent = 0;
        mouseSpeed = 0;
        mouseCurrentFrame = 0;
        mouseInitialized = false;
        mouseCursorPitch = 0;
        mouseCursorWidth = 0;
        mouseNumFrames = 0;
        mouseHotY = 0;
        mouseHotX = 0;
        mouseIdleStartTime = 0;
        mouseAnimTicker = 0;
        mouseBlitTrans = null;
        mouseBlit = null;
        mouseTransparent = 0;
        mouseWheelX = 0;
        mouseWheelY = 0;
        previousRightButtonTimestamp = 0;
        previousLeftButtonTimestamp = 0;
        previousEvent = 0;
        touchShiftHeld = false;
        touchSwipeStartY = 0;
        touchPrevX = 0;
        touchPrevY = 0;
        rawButtons = 0;
        System.arraycopy(DEFAULT_CURSOR_CODES, 0, defaultCursor, 0, defaultCursor.length);
    }
}
