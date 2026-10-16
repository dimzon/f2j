package org.fallout2ce;

import java.util.function.Function;

/**
 * Port of {@code src/dbox.cc} / {@code src/dbox.h}: modal dialog boxes
 * ({@code showDialogBox}) and the text-input dialog ({@code showInputDialog}
 * over the {@code _get_input_str} pump).
 *
 * <p>Only the slices exercised by {@code [windbox]} and {@code [winfile]}
 * are ported ({@code _get_input_str}, {@code showInputDialog},
 * {@code showDialogBox} with the {@code input::} red-button/box helpers and
 * the {@code alert::} background/config tables, plus
 * {@code showLoadFileDialog}/{@code showSaveFileDialog} with the
 * {@code file::} namespace).
 *
 * <p>C {@code unsigned char*} buffers become {@code byte[]} with explicit
 * offsets wherever C used pointer arithmetic. Colors arrive as raw ints
 * (low byte palette index, upper bits {@code DRAW_TEXT_FLAG_*}) and are
 * remapped inside, following the v42 pattern.
 *
 * <p>Seams: {@link #soundPlayFile} stands in for {@code game_sound.cc}
 * (not ported; default is a headless-safe no-op returning -1, exactly what
 * C returns when sound is uninitialized). The quit-request checks
 * ({@code _game_user_wants_to_quit != GAME_QUIT_REQUEST_NONE}) read as
 * "no request" — {@code game.cc} is not ported, so there is no source that
 * could set one.
 */
public final class DBox {

    private DBox() {
    }

    // ===== DialogBoxOptions (dbox.h) =====

    /** sfall calls this NORMAL. */
    public static final int DIALOG_BOX_LARGE = 0x01;
    /** sfall calls this SMALL. */
    public static final int DIALOG_BOX_MEDIUM = 0x02;
    /** sfall calls this ALIGN_LEFT. */
    public static final int DIALOG_BOX_NO_HORIZONTAL_CENTERING = 0x04;
    /** sfall calls this ALIGN_TOP. */
    public static final int DIALOG_BOX_NO_VERTICAL_CENTERING = 0x08;
    public static final int DIALOG_BOX_YES_NO = 0x10;
    /** sfall calls this CLEAN. */
    public static final int DIALOG_BOX_NO_BUTTONS = 0x20;

    /**
     * Sound seam for {@code soundPlayFile} (C plays {@code "ib1p1xx1"} on
     * commit and the red-button press/release sounds). Defaults to a no-op
     * returning -1, which is exactly what C returns headless (sound
     * uninitialized), so golden vectors are unaffected.
     */
    public static Function<String, Integer> soundPlayFile = name -> -1;

    /** Port of {@code showInputDialog}'s {@code static std::string result}. */
    private static String inputDialogResult = "";

    /** Test-only reset (mirrors a fresh process; C statics start empty). */
    static void resetForTests() {
        inputDialogResult = "";
        soundPlayFile = name -> -1;
    }

    // ===== shared helpers (dbox::createRedButton, _isdoschar) =====

    private static void redButtonPress(int btn, int keyCode) {
        soundPlayFile.apply("ib1p1xx1");
    }

    private static void redButtonRelease(int btn, int keyCode) {
        soundPlayFile.apply("ib1lu1x1");
    }

    /** Port of {@code dbox::createRedButton}. */
    private static int createRedButton(int windowId, int x, int y, int buttonId,
            Art.FrmImage upFrm, Art.FrmImage downFrm) {
        int btn = WindowManager.buttonCreate(windowId, x, y,
            downFrm.getWidth(), downFrm.getHeight(),
            -1, -1, -1, buttonId,
            upFrm.getData(), downFrm.getData(), null,
            WindowManager.ButtonFlags.BUTTON_FLAG_TRANSPARENT.id());

        if (btn != -1) {
            WindowManager.buttonSetCallbacks(btn, DBox::redButtonPress, DBox::redButtonRelease);
        }
        return btn;
    }

    private static FrmId ifaceFrm(int frameId) {
        return FrmId.ofType(ObjectType.INTERFACE, frameId);
    }

    /** Port of {@code _isdoschar} (character_editor.cc 0x434060). */
    static boolean isDosChar(int ch) {
        if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
            return true;
        }
        return "#@!$`'~^&()-_=[]{}".indexOf(ch) != -1;
    }

    /** Live-text {@link WordWrap} metrics (C reads the global font procs). */
    private static final WordWrap.FontMetrics LIVE_METRICS = new WordWrap.FontMetrics() {
        @Override
        public int monospacedCharacterWidth() {
            return TextFont.fontGetMonospacedCharacterWidth();
        }

        @Override
        public int stringWidth(String string) {
            return TextFont.fontGetStringWidth(string);
        }

        @Override
        public int letterSpacing() {
            return TextFont.fontGetLetterSpacing();
        }

        @Override
        public int characterWidth(int ch) {
            return TextFont.fontGetCharacterWidth(ch);
        }
    };

    // ===== _get_input_str (extracted from character_editor.cc) =====

    /**
     * Port of {@code _get_input_str}.
     *
     * @param text edited in place, like the C {@code std::string&}.
     * @return 1 while editing, 0 on commit ({@code cancelKeyCode}}/
     *         {@code RETURN}), -1 on {@code ESC}.
     */
    static int getInputStr(int win, int cancelKeyCode, StringBuilder text, int maxLength,
            int x, int y, int textColor, int backgroundColor, int flags) {
        int cursorWidth = TextFont.fontGetStringWidth("_") - 4;
        int windowWidth = WindowManager.windowGetWidth(win);
        int lineHeight = TextFont.fontGetLineHeight();
        byte[] windowBuffer = WindowManager.windowGetBuffer(win);

        if (maxLength > 255) {
            maxLength = 255;
        }

        StringBuilder copy = new StringBuilder(text.toString()).append(' ');

        final int[] lastDrawnWidth = { TextFont.fontGetStringWidth(copy.toString()) };

        Runnable redrawText = () -> {
            int newWidth = TextFont.fontGetStringWidth(copy.toString());
            int clearWidth = Math.max(lastDrawnWidth[0], newWidth);

            Draw.bufferFill(windowBuffer, windowWidth * y + x, clearWidth, lineHeight, windowWidth,
                backgroundColor & Color.COLOR_LAST);
            TextFont.fontDrawText(windowBuffer, windowWidth * y + x, copy.toString(),
                windowWidth, windowWidth, textColor);
            WindowManager.windowRefresh(win);

            lastDrawnWidth[0] = newWidth;
        };
        redrawText.run();

        Input.beginTextInput();

        int blinkingCounter = 3;
        boolean blink = false;
        int rc = 1;

        while (rc == 1) {
            Color.fpsLimiter.mark();
            long frameTime = Ticks.getTicks();

            int keyCode = Input.inputGetInput();
            if (keyCode == cancelKeyCode) {
                rc = 0;
            } else if (keyCode == Kb.KEY_RETURN) {
                soundPlayFile.apply("ib1p1xx1");
                rc = 0;
            } else if (keyCode == Kb.KEY_ESCAPE) {
                // C also bails when _game_user_wants_to_quit !=
                // GAME_QUIT_REQUEST_NONE (game.cc, not ported: never set).
                rc = -1;
            } else {
                // BACKSPACE / DELETE
                if ((keyCode == Kb.KEY_DELETE || keyCode == Kb.KEY_BACKSPACE)
                        && text.length() != 0) {
                    text.setLength(text.length() - 1);
                    copy.setLength(0);
                    copy.append(text).append(' ');
                    redrawText.run();
                }
                // Input
                else if (keyCode >= Kb.KEY_FIRST_INPUT_CHARACTER
                        && keyCode <= Kb.KEY_LAST_INPUT_CHARACTER
                        && text.length() < maxLength) {
                    if ((flags & 0x01) != 0 && !isDosChar(keyCode)) {
                        continue;
                    }

                    text.append((char) (keyCode & 0xFF));
                    copy.setLength(0);
                    copy.append(text).append(' ');
                    redrawText.run();
                }
            }

            blinkingCounter -= 1;
            if (blinkingCounter == 0) {
                blinkingCounter = 3;

                int color = blink ? backgroundColor : textColor & Color.COLOR_LAST;
                blink = !blink;

                int currentTextWidth = TextFont.fontGetStringWidth(copy.toString());
                Draw.bufferFill(windowBuffer,
                    windowWidth * y + x + currentTextWidth - cursorWidth,
                    cursorWidth, lineHeight - 2, windowWidth, color & Color.COLOR_LAST);
            }

            WindowManager.windowRefresh(win);

            Delay.delayMs(1000 / 24 - (int) (Ticks.getTicks() - frameTime));

            Svga.renderPresent();
            Color.fpsLimiter.throttle();
        }

        Input.endTextInput();
        return rc;
    }

    // ===== showInputDialog =====

    // input:: namespace layout constants (dbox.cc).
    private static final int INPUT_NAME_BOX_X = 13;
    private static final int INPUT_NAME_BOX_Y = 13;
    private static final int INPUT_DONE_BOX_X = 13;
    private static final int INPUT_DONE_BOX_Y = 40;
    private static final int INPUT_DONE_LABEL_X = 50;
    private static final int INPUT_DONE_LABEL_Y = 44;
    private static final int INPUT_DONE_BUTTON_X = 26;
    private static final int INPUT_DONE_BUTTON_Y = 44;

    public static String showInputDialog(String currentInput, int windowX, int windowY,
            String doneText, int flags) {
        try (TextFont.ScopedFont mainFontGuard = new TextFont.ScopedFont(101)) {
            Art.FrmImage background = new Art.FrmImage();
            Art.FrmImage nameBox = new Art.FrmImage();
            Art.FrmImage doneBox = new Art.FrmImage();
            Art.FrmImage buttonUp = new Art.FrmImage();
            Art.FrmImage buttonDown = new Art.FrmImage();
            try {
                if (!background.lock(ifaceFrm(
                            ArtDefs.InterfaceFrameId.CharacterWindow.id()))
                        || !nameBox.lock(ifaceFrm(
                            ArtDefs.InterfaceFrameId.CharacterEditorNameBox.id()))
                        || !doneBox.lock(ifaceFrm(ArtDefs.InterfaceFrameId.DoneBox.id()))
                        || !buttonUp.lock(ifaceFrm(
                            ArtDefs.InterfaceFrameId.LittleRedButtonUp.id()))
                        || !buttonDown.lock(ifaceFrm(
                            ArtDefs.InterfaceFrameId.LittleRedButtonDown.id()))) {
                    return null;
                }

                int windowWidth = background.getWidth();
                int windowHeight = background.getHeight();

                int win = WindowManager.windowCreate(windowX, windowY, windowWidth, windowHeight,
                    256, flags);
                if (win == -1) {
                    return null;
                }

                try (WindowManager.UniqueHandle window =
                        new WindowManager.UniqueHandle(WindowManager::windowDestroy, win)) {
                    byte[] windowBuf = WindowManager.windowGetBuffer(window.get());
                    System.arraycopy(background.getData(), 0, windowBuf, 0,
                        windowWidth * windowHeight);

                    drawBox(windowBuf, windowWidth, INPUT_NAME_BOX_X, INPUT_NAME_BOX_Y, nameBox);
                    drawBox(windowBuf, windowWidth, INPUT_DONE_BOX_X, INPUT_DONE_BOX_Y, doneBox);

                    try (TextFont.ScopedFont buttonFontGuard = new TextFont.ScopedFont(103)) {
                        TextFont.fontDrawText(windowBuf,
                            windowWidth * INPUT_DONE_LABEL_Y + INPUT_DONE_LABEL_X,
                            doneText, windowWidth, windowWidth, Color.darkYellow());
                    }

                    createRedButton(window.get(), INPUT_DONE_BUTTON_X, INPUT_DONE_BUTTON_Y, 500,
                        buttonUp, buttonDown);

                    WindowManager.windowRefresh(window.get());

                    StringBuilder editableText = new StringBuilder(
                        currentInput != null && !currentInput.equals("None")
                            ? currentInput : "");
                    int status = getInputStr(window.get(), 500, editableText, 11, 23, 19,
                        Color.green() | Color.DRAW_TEXT_FLAG_NONE, 100, 0);

                    if (status == 0 && editableText.length() != 0) {
                        inputDialogResult = editableText.toString();
                        return inputDialogResult;
                    }

                    return null;
                }
            } finally {
                background.close();
                nameBox.close();
                doneBox.close();
                buttonUp.close();
                buttonDown.close();
            }
        }
    }

    public static String showInputDialog(String currentInput, int x, int y, String doneText) {
        return showInputDialog(currentInput, x, y, doneText,
            WindowManager.WindowFlags.WINDOW_MODAL.id()
                | WindowManager.WindowFlags.WINDOW_DONT_MOVE_TOP.id());
    }

    /** Port of {@code input::drawBox}. */
    private static void drawBox(byte[] buffer, int width, int x, int y, Art.FrmImage frm) {
        Draw.blitBufferToBufferTrans(frm.getData(), 0,
            frm.getWidth(), frm.getHeight(), frm.getWidth(),
            buffer, width * y + x, width);
    }

    // ===== showDialogBox (dialog_out) =====

    // alert:: namespace tables (dbox.cc).
    private enum DialogType {
        MEDIUM,
        LARGE,
    }

    private static final int MEDIUM_BG =
        ArtDefs.InterfaceFrameId.MediumDialog.id();
    private static final int LARGE_BG =
        ArtDefs.InterfaceFrameId.LargeDialog.id();

    private static final int MEDIUM_X = 29;
    private static final int MEDIUM_Y = 23;
    private static final int MEDIUM_DONE_X = 51;
    private static final int MEDIUM_DONE_Y = 81;
    private static final int MEDIUM_LINES = 5;

    private static final int LARGE_X = 29;
    private static final int LARGE_Y = 27;
    private static final int LARGE_DONE_X = 37;
    private static final int LARGE_DONE_Y = 98;
    private static final int LARGE_LINES = 6;

    /**
     * Port of {@code showDialogBox} (0x41CF20 dialog_out).
     *
     * @return 1 on Done/Yes/RETURN (or quit request), 0 on No/ESC, -1 on
     *         resource failure.
     */
    public static int showDialogBox(String title, String[] body, int bodyLength, int x, int y,
            int titleColor, String secondaryButtonText, int bodyColor, int flags) {
        Message.MessageList messageList = new Message.MessageList();
        Message.Item messageListItem = new Message.Item();

        boolean initializedButtons = false;

        boolean hasTwoButtons = secondaryButtonText != null;
        boolean hasTitle = title != null;

        if ((flags & DIALOG_BOX_YES_NO) != 0) {
            hasTwoButtons = true;
            flags |= DIALOG_BOX_LARGE;
            flags &= ~DIALOG_BOX_NO_BUTTONS;
        }
        final int buttonFlags = flags;

        int maximumLineWidth = hasTitle ? TextFont.fontGetStringWidth(title) : 0;
        for (int index = 0; index < bodyLength; index++) {
            maximumLineWidth = Math.max(TextFont.fontGetStringWidth(body[index]), maximumLineWidth);
        }

        int linesCount = bodyLength;
        DialogType dialogType;

        if ((buttonFlags & DIALOG_BOX_LARGE) != 0 || hasTwoButtons) {
            dialogType = DialogType.LARGE;
        } else if ((buttonFlags & DIALOG_BOX_MEDIUM) != 0) {
            dialogType = DialogType.MEDIUM;
        } else {
            if (hasTitle) {
                linesCount++;
            }

            dialogType = maximumLineWidth > 168 || linesCount > 5
                ? DialogType.LARGE : DialogType.MEDIUM;
        }

        final int configX;
        final int configY;
        final int configDoneX;
        final int configDoneY;
        final int configLines;
        final int backgroundId;
        if (dialogType == DialogType.LARGE) {
            configX = LARGE_X;
            configY = LARGE_Y;
            configDoneX = LARGE_DONE_X;
            configDoneY = LARGE_DONE_Y;
            configLines = LARGE_LINES;
            backgroundId = LARGE_BG;
        } else {
            configX = MEDIUM_X;
            configY = MEDIUM_Y;
            configDoneX = MEDIUM_DONE_X;
            configDoneY = MEDIUM_DONE_Y;
            configLines = MEDIUM_LINES;
            backgroundId = MEDIUM_BG;
        }

        Art.FrmImage bg = new Art.FrmImage();
        try {
            if (!bg.lock(ifaceFrm(backgroundId))) {
                return -1;
            }

            // Maintain original position in original resolution, otherwise center it.
            x += (Svga.screenGetWidth() - 640) / 2;
            y += (Svga.screenGetHeight() - 480) / 2;

            int win = WindowManager.windowCreate(x, y, bg.getWidth(), bg.getHeight(), 256,
                WindowManager.WindowFlags.WINDOW_MODAL.id()
                    | WindowManager.WindowFlags.WINDOW_MOVE_ON_TOP.id());
            if (win == -1) {
                return -1;
            }

            try (WindowManager.UniqueHandle window =
                    new WindowManager.UniqueHandle(WindowManager::windowDestroy, win)) {
                byte[] windowBuf = WindowManager.windowGetBuffer(window.get());
                System.arraycopy(bg.getData(), 0, windowBuf, 0,
                    bg.getWidth() * bg.getHeight());

                Art.FrmImage done = new Art.FrmImage();
                Art.FrmImage buttonUp = new Art.FrmImage();
                Art.FrmImage buttonDown = new Art.FrmImage();
                try {
                    // Resources init
                    boolean hasPrimaryButton =
                        (buttonFlags & DIALOG_BOX_NO_BUTTONS) == 0;
                    boolean hasSecondaryButton =
                        hasTwoButtons && dialogType == DialogType.LARGE;

                    if (hasPrimaryButton || hasSecondaryButton) {
                        if (!done.lock(ifaceFrm(ArtDefs.InterfaceFrameId.DoneBox.id()))
                                || !buttonUp.lock(ifaceFrm(
                                    ArtDefs.InterfaceFrameId.LittleRedButtonUp.id()))
                                || !buttonDown.lock(ifaceFrm(
                                    ArtDefs.InterfaceFrameId.LittleRedButtonDown.id()))
                                || !Message.messageListInit(messageList)) {
                            return -1;
                        }

                        // asc_5186C8 ("game\\") + "DBOX.MSG".
                        if (!Message.messageListLoad(messageList, "game\\DBOX.MSG")) {
                            Message.messageListFree(messageList);
                            return -1;
                        }
                    }

                    final int bgWidth = bg.getWidth();

                    // Buttons
                    try (TextFont.ScopedFont buttonFontGuard = new TextFont.ScopedFont(103)) {
                        // First button
                        if (hasPrimaryButton) {
                            int doneBoxX = hasTwoButtons
                                ? configDoneX
                                : (bgWidth - done.getWidth()) / 2;

                            Draw.blitBufferToBuffer(done.getData(), 0,
                                done.getWidth(), done.getHeight(), done.getWidth(),
                                windowBuf, bgWidth * configDoneY + doneBoxX, bgWidth);

                            // 100 - DONE, 101 - YES
                            messageListItem.num =
                                (buttonFlags & DIALOG_BOX_YES_NO) == 0 ? 100 : 101;
                            if (Message.messageListGetItem(messageList, messageListItem)) {
                                TextFont.fontDrawText(windowBuf,
                                    bgWidth * (configDoneY + 3) + doneBoxX + 35,
                                    messageListItem.text, bgWidth, bgWidth,
                                    Color.darkYellow());
                            }

                            createRedButton(window.get(), doneBoxX + 13, configDoneY + 4, 500,
                                buttonUp, buttonDown);

                            initializedButtons = true;
                        }

                        // Second button
                        if (hasSecondaryButton) {
                            if ((buttonFlags & DIALOG_BOX_YES_NO) != 0) {
                                // 102 - NO
                                secondaryButtonText =
                                    Message.getmsg(messageList, messageListItem, 102);
                            }

                            int blitXOffset = hasPrimaryButton
                                ? configDoneX + done.getWidth() + 24 : configDoneX;
                            int textXOffset = hasPrimaryButton
                                ? configDoneX + done.getWidth() + 59 : configDoneX + 35;
                            int buttonXOffset = hasPrimaryButton
                                ? configDoneX + done.getWidth() + 37 : configDoneX + 13;

                            Draw.blitBufferToBufferTrans(done.getData(), 0,
                                done.getWidth(), done.getHeight(), done.getWidth(),
                                windowBuf, bgWidth * configDoneY + blitXOffset, bgWidth);

                            if (secondaryButtonText != null) {
                                TextFont.fontDrawText(windowBuf,
                                    bgWidth * (configDoneY + 3) + textXOffset,
                                    secondaryButtonText, bgWidth, bgWidth, Color.darkYellow());
                            }

                            createRedButton(window.get(), buttonXOffset, configDoneY + 4, 501,
                                buttonUp, buttonDown);

                            initializedButtons = true;
                        }
                    }

                    try (TextFont.ScopedFont mainFontGuard = new TextFont.ScopedFont(101)) {
                        int nextY = configY;
                        int maxY = configY + configLines * TextFont.fontGetLineHeight();
                        int maxWidth = bg.getWidth() - configX * 2;

                        // Vertical center
                        if ((buttonFlags & DIALOG_BOX_NO_VERTICAL_CENTERING) == 0) {
                            int numberOfLines = hasTitle ? 1 : 0;

                            for (int index = 0; index < bodyLength; index++) {
                                if (body[index] == null) {
                                    continue;
                                }

                                short[] beginnings = new short[WordWrap.MAX_COUNT];
                                int[] subLineCount = { 0 };

                                if (WordWrap.wordWrap(body[index], maxWidth, beginnings,
                                        subLineCount, LIVE_METRICS) == 0) {
                                    numberOfLines += subLineCount[0] - 1;
                                }
                            }

                            if (numberOfLines > configLines) {
                                numberOfLines = configLines;
                            }

                            nextY += (configLines - numberOfLines)
                                * TextFont.fontGetLineHeight() / 2;
                        }

                        if (hasTitle && title != null) {
                            drawDialogLine(windowBuf, bgWidth, configX, buttonFlags, title,
                                nextY, titleColor);
                            nextY += TextFont.fontGetLineHeight();
                        }

                        for (int index = 0; index < bodyLength && nextY < maxY; index++) {
                            if (body[index] == null) {
                                continue;
                            }

                            int width = TextFont.fontGetStringWidth(body[index]);
                            if (width <= maxWidth) {
                                // Line's short
                                drawDialogLine(windowBuf, bgWidth, configX, buttonFlags,
                                    body[index], nextY, bodyColor);
                                nextY += TextFont.fontGetLineHeight();
                            } else {
                                // Line's long, use word wrap
                                short[] beginnings = new short[WordWrap.MAX_COUNT];
                                int[] count = { 0 };
                                if (WordWrap.wordWrap(body[index], maxWidth, beginnings, count,
                                        LIVE_METRICS) != 0) {
                                    Debug.print("\nError: dialog_out");
                                }

                                String fullText = body[index];

                                for (int beginningIndex = 1;
                                        beginningIndex < count[0] && nextY < maxY;
                                        beginningIndex++) {
                                    int start = beginnings[beginningIndex - 1];
                                    int length = beginnings[beginningIndex] - start;

                                    String subLine = fullText.substring(start,
                                        Math.min(start + length, fullText.length()));
                                    // trim whitespace
                                    if (!subLine.isEmpty() && subLine.endsWith(" ")) {
                                        subLine = subLine.substring(0, subLine.length() - 1);
                                    }
                                    drawDialogLine(windowBuf, bgWidth, configX, buttonFlags,
                                        subLine, nextY, bodyColor);
                                    nextY += TextFont.fontGetLineHeight();
                                }
                            }
                        }

                        WindowManager.windowRefresh(window.get());

                        int rc = -1;
                        while (rc == -1) {
                            Color.fpsLimiter.mark();

                            int keyCode = Input.inputGetInput();

                            if (keyCode == 500) { // First button (Done/Yes)
                                rc = 1;
                            } else if (keyCode == Kb.KEY_RETURN) {
                                soundPlayFile.apply("ib1p1xx1");
                                rc = 1;
                            } else if (keyCode == Kb.KEY_ESCAPE || keyCode == 501) {
                                // Second button (NO) or ESC
                                rc = 0;
                            } else {
                                if ((buttonFlags & DIALOG_BOX_YES_NO) != 0) {
                                    if (keyCode == Kb.KEY_UPPERCASE_Y
                                            || keyCode == Kb.KEY_LOWERCASE_Y) {
                                        rc = 1;
                                    } else if (keyCode == Kb.KEY_UPPERCASE_N
                                            || keyCode == Kb.KEY_LOWERCASE_N) {
                                        rc = 0;
                                    }
                                }
                            }

                            // C also forces rc = 1 when _game_user_wants_to_quit
                            // != GAME_QUIT_REQUEST_NONE (game.cc, not ported).

                            Svga.renderPresent();
                            Color.fpsLimiter.throttle();
                        }

                        if (initializedButtons) {
                            Message.messageListFree(messageList);
                        }

                        return rc;
                    }
                } finally {
                    done.close();
                    buttonUp.close();
                    buttonDown.close();
                }
            }
        } finally {
            bg.close();
        }
    }

    private static void drawDialogLine(byte[] windowBuf, int bgWidth, int configX, int flags,
            String text, int currentY, int textColor) {
        int xOffset;
        if ((flags & DIALOG_BOX_NO_HORIZONTAL_CENTERING) != 0) {
            xOffset = configX;
        } else {
            xOffset = (bgWidth - TextFont.fontGetStringWidth(text)) / 2;
        }

        TextFont.fontDrawText(windowBuf, bgWidth * currentY + xOffset, text, bgWidth, bgWidth,
            textColor);
    }

    // ===== file dialogs (file:: namespace) =====
    //
    // Port of showLoadFileDialog / showSaveFileDialog with the file::
    // helpers (DialogFrms/loadImages, createScrollButton, ModeConfig,
    // Context/initInterface, fileDialogRenderFileList).
    //
    // C char** + length becomes String[] + int length (same convention as
    // showDialogBox's body/bodyLength). C char[32] dest + strncpy-16 becomes
    // a StringBuilder with explicit truncation. The quit-request checks
    // (_game_user_wants_to_quit != GAME_QUIT_REQUEST_NONE, game.cc, not
    // ported: never set) read as "no request". Mouse wheel conversion
    // reuses the v38 port (Mouse.convertMouseWheelToArrowKey). Delays use
    // Delay.delayMs + Ticks.getTicks like the v42/v45 pumps.

    private static final int FILE_DIALOG_FRM_COUNT = 7;
    private static final int FILE_LINE_COUNT = 12;
    private static final int FILE_DOUBLE_CLICK_DELAY = 32;

    private static final int FILE_TITLE_X = 49;
    private static final int FILE_TITLE_Y = 16;
    private static final int FILE_SCROLL_BUTTON_X = 36;
    private static final int FILE_SCROLL_BUTTON_Y = 44;
    private static final int FILE_LIST_X = 55;
    private static final int FILE_LIST_Y = 49;
    private static final int FILE_LIST_WIDTH = 190;
    private static final int FILE_LIST_HEIGHT = 124;

    private static final int FILE_LOAD_DONE_BUTTON_X = 58;
    private static final int FILE_LOAD_DONE_BUTTON_Y = 187;
    private static final int FILE_LOAD_DONE_LABEL_X = 79;
    private static final int FILE_LOAD_DONE_LABEL_Y = 187;
    private static final int FILE_LOAD_CANCEL_BUTTON_X = 163;
    private static final int FILE_LOAD_CANCEL_BUTTON_Y = 187;
    private static final int FILE_LOAD_CANCEL_LABEL_X = 182;
    private static final int FILE_LOAD_CANCEL_LABEL_Y = 187;

    private static final int FILE_SAVE_DONE_BUTTON_X = 58;
    private static final int FILE_SAVE_DONE_BUTTON_Y = 214;
    private static final int FILE_SAVE_DONE_LABEL_X = 79;
    private static final int FILE_SAVE_DONE_LABEL_Y = 213;
    private static final int FILE_SAVE_CANCEL_BUTTON_X = 163;
    private static final int FILE_SAVE_CANCEL_BUTTON_Y = 214;
    private static final int FILE_SAVE_CANCEL_LABEL_X = 182;
    private static final int FILE_SAVE_CANCEL_LABEL_Y = 213;

    private static final int FILE_SAVE_NAME_X = 57;
    private static final int FILE_SAVE_NAME_Y = 190;

    private static final int FILE_DIALOG_SCROLL_DIRECTION_NONE = 0;
    private static final int FILE_DIALOG_SCROLL_DIRECTION_UP = 1;
    private static final int FILE_DIALOG_SCROLL_DIRECTION_DOWN = 2;

    private static final int[] LOAD_FILE_DIALOG_FRM_IDS = {
        ArtDefs.InterfaceFrameId.LoadBox.id(),
        ArtDefs.InterfaceFrameId.LittleRedButtonUp.id(),
        ArtDefs.InterfaceFrameId.LittleRedButtonDown.id(),
        ArtDefs.InterfaceFrameId.CharacterEditorDownArrowOff.id(),
        ArtDefs.InterfaceFrameId.CharacterEditorDownArrowOn.id(),
        ArtDefs.InterfaceFrameId.CharacterEditorUpArrowOff.id(),
        ArtDefs.InterfaceFrameId.CharacterEditorUpArrowOn.id(),
    };

    private static final int[] SAVE_FILE_DIALOG_FRM_IDS = {
        ArtDefs.InterfaceFrameId.SaveBox.id(),
        ArtDefs.InterfaceFrameId.LittleRedButtonUp.id(),
        ArtDefs.InterfaceFrameId.LittleRedButtonDown.id(),
        ArtDefs.InterfaceFrameId.CharacterEditorDownArrowOff.id(),
        ArtDefs.InterfaceFrameId.CharacterEditorDownArrowOn.id(),
        ArtDefs.InterfaceFrameId.CharacterEditorUpArrowOff.id(),
        ArtDefs.InterfaceFrameId.CharacterEditorUpArrowOn.id(),
    };

    /** Port of {@code file::DialogFrms}. */
    private static final class FileDialogFrms {
        final Art.FrmImage background = new Art.FrmImage();
        final Art.FrmImage buttonNormal = new Art.FrmImage();
        final Art.FrmImage buttonPressed = new Art.FrmImage();
        final Art.FrmImage scrollDownNormal = new Art.FrmImage();
        final Art.FrmImage scrollDownPressed = new Art.FrmImage();
        final Art.FrmImage scrollUpNormal = new Art.FrmImage();
        final Art.FrmImage scrollUpPressed = new Art.FrmImage();

        void close() {
            background.close();
            buttonNormal.close();
            buttonPressed.close();
            scrollDownNormal.close();
            scrollDownPressed.close();
            scrollUpNormal.close();
            scrollUpPressed.close();
        }
    }

    /** Port of {@code file::loadImages}. */
    private static boolean fileLoadImages(FileDialogFrms images, int[] frmIds) {
        Art.FrmImage[] frms = {
            images.background,
            images.buttonNormal,
            images.buttonPressed,
            images.scrollDownNormal,
            images.scrollDownPressed,
            images.scrollUpNormal,
            images.scrollUpPressed,
        };
        for (int index = 0; index < FILE_DIALOG_FRM_COUNT; index++) {
            if (!frms[index].lock(ifaceFrm(frmIds[index]))) {
                return false;
            }
        }
        return true;
    }

    /** Port of {@code file::Context} (window + message list). */
    private static final class FileContext implements AutoCloseable {
        final WindowManager.UniqueHandle window =
            new WindowManager.UniqueHandle(WindowManager::windowDestroy);
        final Message.MessageList messageList = new Message.MessageList();

        @Override
        public void close() {
            Message.messageListFree(messageList);
            window.close();
        }
    }

    /** Port of {@code file::createScrollButton}. */
    private static int createScrollButton(int windowId, int x, int y, int buttonId,
            int shortKey, int arrowKey, int hoverKey,
            Art.FrmImage upFrm, Art.FrmImage downFrm) {
        int btn = WindowManager.buttonCreate(windowId, x, y,
            downFrm.getWidth(), downFrm.getHeight(),
            shortKey, arrowKey, hoverKey, buttonId,
            upFrm.getData(), downFrm.getData(), null,
            WindowManager.ButtonFlags.BUTTON_FLAG_TRANSPARENT.id());

        if (btn != -1) {
            WindowManager.buttonSetCallbacks(btn, DBox::redButtonPress, DBox::redButtonRelease);
        }
        return btn;
    }

    /**
     * Port of {@code file::initInterface}.
     *
     * @param xy in/out screen position (C takes {@code int& x, int& y}).
     */
    private static boolean fileInitInterface(FileContext ctx, FileDialogFrms frms, int[] frmIds,
            int doneButtonX, int doneButtonY, int doneLabelX, int doneLabelY,
            int cancelButtonX, int cancelButtonY, int cancelLabelX, int cancelLabelY,
            String title, int[] xy) {
        if (!fileLoadImages(frms, frmIds)) {
            return false;
        }

        int bgWidth = frms.background.getWidth();
        int bgHeight = frms.background.getHeight();
        xy[0] += (Svga.screenGetWidth() - 640) / 2;
        xy[1] += (Svga.screenGetHeight() - 480) / 2;

        int win = WindowManager.windowCreate(xy[0], xy[1], bgWidth, bgHeight, 256,
            WindowManager.WindowFlags.WINDOW_MODAL.id()
                | WindowManager.WindowFlags.WINDOW_MOVE_ON_TOP.id());
        if (win == -1) {
            return false;
        }
        ctx.window.reset(win);

        byte[] windowBuffer = WindowManager.windowGetBuffer(ctx.window.get());
        System.arraycopy(frms.background.getData(), 0, windowBuffer, 0,
            bgWidth * bgHeight);

        Message.Item messageListItem = new Message.Item();
        if (!Message.messageListInit(ctx.messageList)) {
            return false;
        }

        // asc_5186C8 ("game\\") + "DBOX.MSG".
        if (!Message.messageListLoad(ctx.messageList, "game\\" + "DBOX.MSG")) {
            return false;
        }

        TextFont.fontSetCurrent(103);

        String doneText = Message.getmsg(ctx.messageList, messageListItem, 100);
        TextFont.fontDrawText(windowBuffer, bgWidth * doneLabelY + doneLabelX,
            doneText, bgWidth, bgWidth, Color.darkYellow());

        // Item 103 is the Cancel label (100/101/102 are Done/Yes/No).
        String cancelText = Message.getmsg(ctx.messageList, messageListItem, 103);
        TextFont.fontDrawText(windowBuffer, bgWidth * cancelLabelY + cancelLabelX,
            cancelText, bgWidth, bgWidth, Color.darkYellow());

        createRedButton(ctx.window.get(), doneButtonX, doneButtonY, 500,
            frms.buttonNormal, frms.buttonPressed);
        createRedButton(ctx.window.get(), cancelButtonX, cancelButtonY, 501,
            frms.buttonNormal, frms.buttonPressed);

        createScrollButton(ctx.window.get(),
            FILE_SCROLL_BUTTON_X, FILE_SCROLL_BUTTON_Y, 505, -1, 505, 506,
            frms.scrollUpNormal, frms.scrollUpPressed);

        int scrollDownY = FILE_SCROLL_BUTTON_Y + frms.scrollUpPressed.getHeight();
        createScrollButton(ctx.window.get(),
            FILE_SCROLL_BUTTON_X, scrollDownY, 503, -1, 503, 504,
            frms.scrollDownNormal, frms.scrollDownPressed);

        WindowManager.buttonCreate(ctx.window.get(),
            FILE_LIST_X, FILE_LIST_Y, FILE_LIST_WIDTH, FILE_LIST_HEIGHT,
            -1, -1, -1, 502, null, null, null, 0);

        if (title != null) {
            TextFont.fontDrawText(windowBuffer, bgWidth * FILE_TITLE_Y + FILE_TITLE_X,
                title, bgWidth, bgWidth, Color.darkYellow());
        }

        return true;
    }

    /** Port of {@code strncpy(dest, entry, 16)} (C string semantics). */
    private static void copyEntry16(StringBuilder dest, String entry) {
        dest.setLength(0);
        dest.append(entry.substring(0, Math.min(16, entry.length())));
    }

    /** Save-dialog name trim: cut at {@code '.'} or 12 chars (C loop). */
    private static String trimSaveName(String name) {
        int cut = Math.min(name.length(), 12);
        int dot = name.indexOf('.');
        if (dot != -1 && dot < cut) {
            cut = dot;
        }
        return name.substring(0, cut);
    }

    /**
     * Port of {@code fileDialogRenderFileList} (0x41FBDC PrntFlist).
     *
     * <p>The {@code fileListLength} shadowing ({@code fileListLength =
     * lineCount} clamp via a local) is ported with an explicit
     * {@code shown} local.
     */
    static void fileDialogRenderFileList(byte[] buffer, String[] fileList, int pageOffset,
            int fileListLength, int selectedIndex, int pitch) {
        int lineHeight = TextFont.fontGetLineHeight();
        int y = FILE_LIST_Y;
        Draw.bufferFill(buffer, y * pitch + FILE_LIST_X,
            FILE_LIST_WIDTH, FILE_LIST_HEIGHT, pitch, 100);
        if (fileListLength != 0) {
            int shown = fileListLength;
            if (fileListLength - pageOffset > FILE_LINE_COUNT) {
                shown = FILE_LINE_COUNT;
            }

            for (int index = 0; index < shown; index++) {
                int color = index == selectedIndex ? Color.lightYellow() : Color.green();
                TextFont.fontDrawText(buffer, pitch * y + FILE_LIST_X,
                    fileList[pageOffset + index], FILE_LIST_WIDTH, pitch, color);
                y += lineHeight;
            }
        }
    }

    /**
     * Port of {@code showLoadFileDialog} (0x41E2D0).
     *
     * @param dest edited in place like the C {@code char[32]} buffer.
     */
    public static int showLoadFileDialog(String title, String[] fileList, StringBuilder dest,
            int fileListLength, int x, int y, int flags) {
        try (TextFont.ScopedFont mainFontGuard = new TextFont.ScopedFont(103)) {
            boolean isScrollable = fileListLength > FILE_LINE_COUNT;

            int selectedFileIndex = 0;
            int pageOffset = 0;
            // Quirk: length - (lineCount + 1), floored at max(0, length - 1).
            int maxPageOffset = fileListLength - (FILE_LINE_COUNT + 1);
            if (maxPageOffset < 0) {
                maxPageOffset = Math.max(0, fileListLength - 1);
            }

            FileDialogFrms frms = new FileDialogFrms();
            try (FileContext ctx = new FileContext()) {
                int[] xy = { x, y };
                if (!fileInitInterface(ctx, frms, LOAD_FILE_DIALOG_FRM_IDS,
                        FILE_LOAD_DONE_BUTTON_X, FILE_LOAD_DONE_BUTTON_Y,
                        FILE_LOAD_DONE_LABEL_X, FILE_LOAD_DONE_LABEL_Y,
                        FILE_LOAD_CANCEL_BUTTON_X, FILE_LOAD_CANCEL_BUTTON_Y,
                        FILE_LOAD_CANCEL_LABEL_X, FILE_LOAD_CANCEL_LABEL_Y,
                        title, xy)) {
                    return -1;
                }
                int adjY = xy[1];

                TextFont.fontSetCurrent(101);

                int bgWidth = frms.background.getWidth();
                byte[] windowBuffer = WindowManager.windowGetBuffer(ctx.window.get());

                fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                    fileListLength, selectedFileIndex, bgWidth);
                WindowManager.windowRefresh(ctx.window.get());

                int doubleClickSelectedFileIndex = -2;
                int doubleClickTimer = FILE_DOUBLE_CLICK_DELAY;

                int rc = -1;
                while (rc == -1) {
                    Color.fpsLimiter.mark();

                    long tick = Ticks.getTicks();
                    int keyCode = Input.inputGetInput();
                    int scrollDirection = FILE_DIALOG_SCROLL_DIRECTION_NONE;
                    int scrollCounter = 0;
                    boolean isScrolling = false;

                    int[] wheelKey = { keyCode };
                    Mouse.convertMouseWheelToArrowKey(wheelKey);
                    keyCode = wheelKey[0];

                    if (keyCode == 500) {
                        if (fileListLength != 0) {
                            copyEntry16(dest, fileList[selectedFileIndex + pageOffset]);
                            rc = 0;
                        } else {
                            rc = 1;
                        }
                    } else if (keyCode == 501 || keyCode == Kb.KEY_ESCAPE) {
                        rc = 1;
                    } else if (keyCode == 502 && fileListLength != 0) {
                        int[] pos = new int[2];
                        Mouse.mouseGetPosition(pos);

                        int selectedLine =
                            (pos[1] - adjY - FILE_LIST_Y) / TextFont.fontGetLineHeight();
                        if (selectedLine - 1 < 0) {
                            selectedLine = 0;
                        }

                        if (isScrollable || selectedLine < fileListLength) {
                            if (selectedLine >= FILE_LINE_COUNT) {
                                selectedLine = FILE_LINE_COUNT - 1;
                            }
                        } else {
                            selectedLine = fileListLength - 1;
                        }

                        selectedFileIndex = selectedLine;
                        if (selectedFileIndex == doubleClickSelectedFileIndex) {
                            soundPlayFile.apply("ib1p1xx1");
                            copyEntry16(dest, fileList[selectedFileIndex + pageOffset]);
                            rc = 0;
                        }

                        doubleClickSelectedFileIndex = selectedFileIndex;
                        fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                            fileListLength, selectedFileIndex, bgWidth);
                    } else if (keyCode == 506) {
                        scrollDirection = FILE_DIALOG_SCROLL_DIRECTION_UP;
                    } else if (keyCode == 504) {
                        scrollDirection = FILE_DIALOG_SCROLL_DIRECTION_DOWN;
                    } else {
                        switch (keyCode) {
                        case Kb.KEY_ARROW_UP:
                            pageOffset--;
                            if (pageOffset < 0) {
                                selectedFileIndex--;
                                if (selectedFileIndex < 0) {
                                    selectedFileIndex = 0;
                                }
                                pageOffset = 0;
                            }
                            fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                fileListLength, selectedFileIndex, bgWidth);
                            doubleClickSelectedFileIndex = -2;
                            break;
                        case Kb.KEY_ARROW_DOWN:
                            if (isScrollable) {
                                pageOffset++;
                                // FIXME quirk (load): C uses > maxPageOffset
                                // here where the save dialog uses >=, so an
                                // out-of-bounds index reads as selected.
                                if (pageOffset > maxPageOffset) {
                                    selectedFileIndex++;
                                    // FIXME quirk (load): C uses >
                                    // FILE_LINE_COUNT where the save dialog
                                    // uses >=.
                                    if (selectedFileIndex > FILE_LINE_COUNT) {
                                        selectedFileIndex = FILE_LINE_COUNT - 1;
                                    }
                                    pageOffset = maxPageOffset;
                                }
                            } else {
                                selectedFileIndex++;
                                if (selectedFileIndex > maxPageOffset) {
                                    selectedFileIndex = maxPageOffset;
                                }
                            }
                            fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                fileListLength, selectedFileIndex, bgWidth);
                            doubleClickSelectedFileIndex = -2;
                            break;
                        case Kb.KEY_HOME:
                            selectedFileIndex = 0;
                            pageOffset = 0;
                            fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                fileListLength, selectedFileIndex, bgWidth);
                            doubleClickSelectedFileIndex = -2;
                            break;
                        case Kb.KEY_END:
                            if (isScrollable) {
                                selectedFileIndex = FILE_LINE_COUNT - 1;
                                pageOffset = maxPageOffset;
                            } else {
                                selectedFileIndex = maxPageOffset;
                                pageOffset = 0;
                            }
                            fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                fileListLength, selectedFileIndex, bgWidth);
                            doubleClickSelectedFileIndex = -2;
                            break;
                        default:
                            break;
                        }
                    }

                    if (scrollDirection != FILE_DIALOG_SCROLL_DIRECTION_NONE) {
                        int scrollDelay = 4;
                        doubleClickSelectedFileIndex = -2;
                        while (true) {
                            long scrollTick = Ticks.getTicks();
                            scrollCounter += 1;
                            if ((!isScrolling && scrollCounter == 1)
                                    || (isScrolling && scrollCounter > 14.4)) {
                                isScrolling = true;

                                if (scrollCounter > 14.4) {
                                    scrollDelay += 1;
                                    if (scrollDelay > 24) {
                                        scrollDelay = 24;
                                    }
                                }

                                if (scrollDirection == FILE_DIALOG_SCROLL_DIRECTION_UP) {
                                    pageOffset--;
                                    if (pageOffset < 0) {
                                        selectedFileIndex--;
                                        if (selectedFileIndex < 0) {
                                            selectedFileIndex = 0;
                                        }
                                        pageOffset = 0;
                                    }
                                } else {
                                    if (isScrollable) {
                                        pageOffset++;
                                        if (pageOffset > maxPageOffset) {
                                            selectedFileIndex++;
                                            if (selectedFileIndex >= FILE_LINE_COUNT) {
                                                selectedFileIndex = FILE_LINE_COUNT - 1;
                                            }
                                            pageOffset = maxPageOffset;
                                        }
                                    } else {
                                        selectedFileIndex++;
                                        if (selectedFileIndex > maxPageOffset) {
                                            selectedFileIndex = maxPageOffset;
                                        }
                                    }
                                }

                                fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                    fileListLength, selectedFileIndex, bgWidth);
                                WindowManager.windowRefresh(ctx.window.get());
                            }

                            int delay =
                                (scrollCounter > 14.4) ? 1000 / scrollDelay : 1000 / 24;

                            Delay.delayMs(delay - (int) (Ticks.getTicks() - scrollTick));

                            // C also forces rc = 1 when _game_user_wants_to_quit
                            // != GAME_QUIT_REQUEST_NONE (game.cc, not ported:
                            // treated as NONE).

                            int scrollKey = Input.inputGetInput();
                            if (scrollKey == 505 || scrollKey == 503) {
                                break;
                            }

                            Svga.renderPresent();
                        }
                    } else {
                        WindowManager.windowRefresh(ctx.window.get());

                        doubleClickTimer--;
                        if (doubleClickTimer == 0) {
                            doubleClickTimer = FILE_DOUBLE_CLICK_DELAY;
                            doubleClickSelectedFileIndex = -2;
                        }

                        Delay.delayMs(1000 / 24 - (int) (Ticks.getTicks() - tick));
                    }

                    // C also forces rc = 1 when _game_user_wants_to_quit !=
                    // GAME_QUIT_REQUEST_NONE (game.cc, not ported: never set).

                    Svga.renderPresent();
                    Color.fpsLimiter.throttle();
                }

                return rc;
            } finally {
                frms.close();
            }
        }
    }

    /**
     * Port of {@code showSaveFileDialog} (0x41EA78 save_file_dialog).
     *
     * @param dest edited in place like the C {@code char[32]} buffer.
     */
    public static int showSaveFileDialog(String title, String[] fileList, StringBuilder dest,
            int fileListLength, int x, int y, int flags) {
        try (TextFont.ScopedFont mainFontGuard = new TextFont.ScopedFont(103)) {
            boolean isScrollable = fileListLength > FILE_LINE_COUNT;

            int selectedFileIndex = 0;
            int pageOffset = 0;
            // Same maxPageOffset quirk as the load dialog.
            int maxPageOffset = fileListLength - (FILE_LINE_COUNT + 1);
            if (maxPageOffset < 0) {
                maxPageOffset = Math.max(0, fileListLength - 1);
            }

            FileDialogFrms frms = new FileDialogFrms();
            try (FileContext ctx = new FileContext()) {
                int[] xy = { x, y };
                if (!fileInitInterface(ctx, frms, SAVE_FILE_DIALOG_FRM_IDS,
                        FILE_SAVE_DONE_BUTTON_X, FILE_SAVE_DONE_BUTTON_Y,
                        FILE_SAVE_DONE_LABEL_X, FILE_SAVE_DONE_LABEL_Y,
                        FILE_SAVE_CANCEL_BUTTON_X, FILE_SAVE_CANCEL_BUTTON_Y,
                        FILE_SAVE_CANCEL_LABEL_X, FILE_SAVE_CANCEL_LABEL_Y,
                        title, xy)) {
                    return -1;
                }
                int adjY = xy[1];

                int bgWidth = frms.background.getWidth();
                byte[] windowBuffer = WindowManager.windowGetBuffer(ctx.window.get());

                TextFont.fontSetCurrent(101);

                int cursorHeight = TextFont.fontGetLineHeight();
                int cursorWidth = TextFont.fontGetStringWidth("_") - 4;
                fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                    fileListLength, selectedFileIndex, bgWidth);

                // Truncate the incoming dest at '.' or 12 chars (C writes the
                // NUL straight into the caller buffer).
                StringBuilder fileName =
                    new StringBuilder(trimSaveName(dest.toString()));
                dest.setLength(0);
                dest.append(fileName);
                int fileNameLength = fileName.length();

                int fileNameBase = bgWidth * FILE_SAVE_NAME_Y + FILE_SAVE_NAME_X;

                Draw.bufferFill(windowBuffer, fileNameBase,
                    TextFont.fontGetStringWidth(fileName + " "),
                    cursorHeight, bgWidth, 100);
                TextFont.fontDrawText(windowBuffer, fileNameBase, fileName + " ",
                    bgWidth, bgWidth, Color.green());

                WindowManager.windowRefresh(ctx.window.get());

                Input.beginTextInput();

                int blinkingCounter = 3;
                boolean blink = false;

                int doubleClickSelectedFileIndex = -2;
                int doubleClickTimer = FILE_DOUBLE_CLICK_DELAY;

                int rc = -1;
                while (rc == -1) {
                    Color.fpsLimiter.mark();

                    long tick = Ticks.getTicks();
                    int keyCode = Input.inputGetInput();
                    int scrollDirection = FILE_DIALOG_SCROLL_DIRECTION_NONE;
                    int scrollCounter = 0;
                    boolean isScrolling = false;

                    int[] wheelKey = { keyCode };
                    Mouse.convertMouseWheelToArrowKey(wheelKey);
                    keyCode = wheelKey[0];

                    if (keyCode == 500) {
                        rc = 0;
                    } else if (keyCode == Kb.KEY_RETURN) {
                        soundPlayFile.apply("ib1p1xx1");
                        rc = 0;
                    } else if (keyCode == 501 || keyCode == Kb.KEY_ESCAPE) {
                        rc = 1;
                    } else if ((keyCode == Kb.KEY_DELETE || keyCode == Kb.KEY_BACKSPACE)
                            && fileNameLength > 0) {
                        Draw.bufferFill(windowBuffer, fileNameBase,
                            TextFont.fontGetStringWidth(fileName + " "),
                            cursorHeight, bgWidth, 100);
                        fileName.setLength(fileNameLength - 1);
                        fileNameLength--;
                        TextFont.fontDrawText(windowBuffer, fileNameBase, fileName + " ",
                            bgWidth, bgWidth, Color.green());
                        WindowManager.windowRefresh(ctx.window.get());
                    } else if (keyCode < Kb.KEY_FIRST_INPUT_CHARACTER
                            || keyCode > Kb.KEY_LAST_INPUT_CHARACTER
                            || fileNameLength >= 8) {
                        if (keyCode == 502 && fileListLength != 0) {
                            int[] pos = new int[2];
                            Mouse.mouseGetPosition(pos);

                            int selectedLine = (pos[1] - adjY - FILE_LIST_Y)
                                / TextFont.fontGetLineHeight();
                            if (selectedLine - 1 < 0) {
                                selectedLine = 0;
                            }

                            if (isScrollable || selectedLine < fileListLength) {
                                if (selectedLine >= FILE_LINE_COUNT) {
                                    selectedLine = FILE_LINE_COUNT - 1;
                                }
                            } else {
                                selectedLine = fileListLength - 1;
                            }

                            selectedFileIndex = selectedLine;
                            if (selectedFileIndex == doubleClickSelectedFileIndex) {
                                soundPlayFile.apply("ib1p1xx1");
                                dest.setLength(0);
                                dest.append(trimSaveName(
                                    fileList[selectedFileIndex + pageOffset]
                                        .substring(0, Math.min(16,
                                            fileList[selectedFileIndex + pageOffset]
                                                .length()))));
                                rc = 2;
                            } else {
                                doubleClickSelectedFileIndex = selectedFileIndex;
                                Draw.bufferFill(windowBuffer, fileNameBase,
                                    TextFont.fontGetStringWidth(fileName + " "),
                                    cursorHeight, bgWidth, 100);
                                fileName.setLength(0);
                                fileName.append(trimSaveName(
                                    fileList[selectedFileIndex + pageOffset]
                                        .substring(0, Math.min(16,
                                            fileList[selectedFileIndex + pageOffset]
                                                .length()))));
                                fileNameLength = fileName.length();

                                TextFont.fontDrawText(windowBuffer, fileNameBase,
                                    fileName + " ", bgWidth, bgWidth, Color.green());
                                fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                    fileListLength, selectedFileIndex, bgWidth);
                            }
                        } else if (keyCode == 506) {
                            scrollDirection = FILE_DIALOG_SCROLL_DIRECTION_UP;
                        } else if (keyCode == 504) {
                            scrollDirection = FILE_DIALOG_SCROLL_DIRECTION_DOWN;
                        } else {
                            switch (keyCode) {
                            case Kb.KEY_ARROW_UP:
                                pageOffset--;
                                if (pageOffset < 0) {
                                    selectedFileIndex--;
                                    if (selectedFileIndex < 0) {
                                        selectedFileIndex = 0;
                                    }
                                    pageOffset = 0;
                                }
                                fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                    fileListLength, selectedFileIndex, bgWidth);
                                doubleClickSelectedFileIndex = -2;
                                break;
                            case Kb.KEY_ARROW_DOWN:
                                if (isScrollable) {
                                    pageOffset++;
                                    if (pageOffset >= maxPageOffset) {
                                        selectedFileIndex++;
                                        if (selectedFileIndex >= FILE_LINE_COUNT) {
                                            selectedFileIndex = FILE_LINE_COUNT - 1;
                                        }
                                        pageOffset = maxPageOffset;
                                    }
                                } else {
                                    selectedFileIndex++;
                                    if (selectedFileIndex > maxPageOffset) {
                                        selectedFileIndex = maxPageOffset;
                                    }
                                }
                                fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                    fileListLength, selectedFileIndex, bgWidth);
                                doubleClickSelectedFileIndex = -2;
                                break;
                            case Kb.KEY_HOME:
                                selectedFileIndex = 0;
                                pageOffset = 0;
                                fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                    fileListLength, selectedFileIndex, bgWidth);
                                doubleClickSelectedFileIndex = -2;
                                break;
                            case Kb.KEY_END:
                                if (isScrollable) {
                                    // Quirk: literal 11 instead of
                                    // FILE_LINE_COUNT - 1 (same value).
                                    selectedFileIndex = 11;
                                    pageOffset = maxPageOffset;
                                } else {
                                    selectedFileIndex = maxPageOffset;
                                    pageOffset = 0;
                                }
                                fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                    fileListLength, selectedFileIndex, bgWidth);
                                doubleClickSelectedFileIndex = -2;
                                break;
                            default:
                                break;
                            }
                        }
                    } else if (isDosChar(keyCode)) {
                        Draw.bufferFill(windowBuffer, fileNameBase,
                            TextFont.fontGetStringWidth(fileName + " "),
                            cursorHeight, bgWidth, 100);

                        fileName.append((char) (keyCode & 0xFF));
                        fileNameLength++;
                        TextFont.fontDrawText(windowBuffer, fileNameBase, fileName + " ",
                            bgWidth, bgWidth, Color.green());

                        WindowManager.windowRefresh(ctx.window.get());
                    }

                    if (scrollDirection != FILE_DIALOG_SCROLL_DIRECTION_NONE) {
                        int scrollDelay = 4;
                        doubleClickSelectedFileIndex = -2;
                        while (true) {
                            long scrollTick = Ticks.getTicks();
                            scrollCounter += 1;
                            if ((!isScrolling && scrollCounter == 1)
                                    || (isScrolling && scrollCounter > 14.4)) {
                                isScrolling = true;

                                if (scrollCounter > 14.4) {
                                    scrollDelay += 1;
                                    if (scrollDelay > 24) {
                                        scrollDelay = 24;
                                    }
                                }

                                if (scrollDirection == FILE_DIALOG_SCROLL_DIRECTION_UP) {
                                    pageOffset--;
                                    if (pageOffset < 0) {
                                        selectedFileIndex--;
                                        if (selectedFileIndex < 0) {
                                            selectedFileIndex = 0;
                                        }
                                        pageOffset = 0;
                                    }
                                } else {
                                    if (isScrollable) {
                                        pageOffset++;
                                        if (pageOffset > maxPageOffset) {
                                            selectedFileIndex++;
                                            if (selectedFileIndex >= FILE_LINE_COUNT) {
                                                selectedFileIndex = FILE_LINE_COUNT - 1;
                                            }
                                            pageOffset = maxPageOffset;
                                        }
                                    } else {
                                        selectedFileIndex++;
                                        if (selectedFileIndex > maxPageOffset) {
                                            selectedFileIndex = maxPageOffset;
                                        }
                                    }
                                }

                                fileDialogRenderFileList(windowBuffer, fileList, pageOffset,
                                    fileListLength, selectedFileIndex, bgWidth);
                                WindowManager.windowRefresh(ctx.window.get());
                            }

                            blinkingCounter -= 1;
                            if (blinkingCounter == 0) {
                                blinkingCounter = 3;

                                int color = blink ? 100 : Color.green();
                                blink = !blink;

                                Draw.bufferFill(windowBuffer,
                                    fileNameBase
                                        + TextFont.fontGetStringWidth(fileName + " ")
                                        - cursorWidth,
                                    cursorWidth, cursorHeight - 2, bgWidth, color);
                            }

                            // FIXME quirk: no windowRefresh here, so the
                            // blinking above never shows (ported as-is).

                            int delay =
                                (scrollCounter > 14.4) ? 1000 / scrollDelay : 1000 / 24;
                            Delay.delayMs(delay - (int) (Ticks.getTicks() - scrollTick));

                            // C also forces rc = 1 when _game_user_wants_to_quit
                            // != GAME_QUIT_REQUEST_NONE (game.cc, not ported:
                            // treated as NONE).

                            int scrollKey = Input.inputGetInput();
                            if (scrollKey == 505 || scrollKey == 503) {
                                break;
                            }

                            Svga.renderPresent();
                        }
                    } else {
                        blinkingCounter -= 1;
                        if (blinkingCounter == 0) {
                            blinkingCounter = 3;

                            int color = blink ? 100 : Color.green();
                            blink = !blink;

                            Draw.bufferFill(windowBuffer,
                                fileNameBase
                                    + TextFont.fontGetStringWidth(fileName + " ")
                                    - cursorWidth,
                                cursorWidth, cursorHeight - 2, bgWidth, color);
                        }

                        WindowManager.windowRefresh(ctx.window.get());

                        doubleClickTimer--;
                        if (doubleClickTimer == 0) {
                            doubleClickTimer = FILE_DOUBLE_CLICK_DELAY;
                            doubleClickSelectedFileIndex = -2;
                        }

                        Delay.delayMs(1000 / 24 - (int) (Ticks.getTicks() - tick));
                    }

                    // C also forces rc = 1 when _game_user_wants_to_quit !=
                    // GAME_QUIT_REQUEST_NONE (game.cc, not ported: never set).

                    Svga.renderPresent();
                    Color.fpsLimiter.throttle();
                }

                Input.endTextInput();

                if (rc == 0) {
                    if (fileNameLength != 0) {
                        dest.setLength(0);
                        dest.append(fileName);
                    } else {
                        rc = 1;
                    }
                } else {
                    if (rc == 2) {
                        rc = 0;
                    }
                }

                return rc;
            } finally {
                frms.close();
            }
        }
    }
}
