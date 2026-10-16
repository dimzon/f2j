package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of the script-window text/buffer core in {@code src/window.cc}.
 *
 * <p>{@code Window.java} holds only id tables; the engine lives here:
 * widget font/flags/color state ({@code windowGetFont}/{@code windowSetFont},
 * {@code scriptWindowResetTextAttributes}/{@code Get/SetTextFlags}/
 * {@code Get/SetTextColor}/{@code Highlight}), {@code windowPrintBuf},
 * {@code windowWordWrap}, {@code windowWrapLineWithSpacing}/
 * {@code windowWrapLine}, {@code _drawScaled}/{@code _drawScaledBuf} and
 * {@code _fillBuf3x3}.
 *
 * <p>C {@code unsigned char*} buffers become {@code byte[]} with explicit
 * pitch/offset params wherever C used pointer arithmetic. Colors arrive as
 * ints (low byte palette index, upper bits {@code DRAW_TEXT_FLAG_*});
 * {@code scriptWindowGetTextColor} returns the {@code _colorTable} entry as
 * an unsigned 0..255 int, exactly like the C {@code Color} return.
 */
public final class ScriptWindow {

    private ScriptWindow() {
    }

    /** Port of {@code gWidgetFont}. */
    private static int widgetFont;

    /** Port of {@code gWidgetTextFlags}. */
    private static int widgetTextFlags;

    /** Port of {@code _currentTextColorR/G/B} (0..31 components). */
    private static int currentTextColorR;

    /** Port of {@code _currentTextColorR/G/B} (0..31 components). */
    private static int currentTextColorG;

    /** Port of {@code _currentTextColorR/G/B} (0..31 components). */
    private static int currentTextColorB;

    /** Port of {@code _currentHighlightColorR/G/B} (0..31 components). */
    private static int currentHighlightColorR;

    /** Port of {@code _currentHighlightColorR/G/B} (0..31 components). */
    private static int currentHighlightColorG;

    /** Port of {@code _currentHighlightColorR/G/B} (0..31 components). */
    private static int currentHighlightColorB;

    /** Test-only reset (mirrors the C static zero-initial state). */
    static void resetForTests() {
        widgetFont = 0;
        widgetTextFlags = 0;
        currentTextColorR = 0;
        currentTextColorG = 0;
        currentTextColorB = 0;
        currentHighlightColorR = 0;
        currentHighlightColorG = 0;
        currentHighlightColorB = 0;
    }

    /** Port of {@code windowGetFont}. */
    public static int windowGetFont() {
        return widgetFont;
    }

    /** Port of {@code windowSetFont} (0x4B6128). */
    public static int windowSetFont(int font) {
        widgetFont = font;
        TextFont.fontSetCurrent(font);
        return 1;
    }

    /** Port of {@code scriptWindowResetTextAttributes} (0x4B6138). */
    public static void scriptWindowResetTextAttributes() {
        // NOTE: Uninline.
        scriptWindowSetTextColor(1.0f, 1.0f, 1.0f);

        // NOTE: Uninline.
        scriptWindowSetTextFlags(Color.DRAW_TEXT_FLAG_NO_BG | Color.DRAW_TEXT_FLAG_SHADOWED);
    }

    /** Port of {@code scriptWindowGetTextFlags} (0x4B6160). */
    public static int scriptWindowGetTextFlags() {
        return widgetTextFlags;
    }

    /** Port of {@code scriptWindowSetTextFlags} (0x4B6168). */
    public static int scriptWindowSetTextFlags(int flags) {
        widgetTextFlags = flags;
        return 1;
    }

    /** Port of {@code scriptWindowGetTextColor} (0x4B6174). */
    public static int scriptWindowGetTextColor() {
        return Byte.toUnsignedInt(
            Color.colorTable[currentTextColorB | (currentTextColorG << 5) | (currentTextColorR << 10)]);
    }

    /** Port of {@code scriptWindowGetHighlightColor} (0x4B6198). */
    public static int scriptWindowGetHighlightColor() {
        return Byte.toUnsignedInt(Color.colorTable[currentHighlightColorB
            | (currentHighlightColorG << 5) | (currentHighlightColorR << 10)]);
    }

    /** Port of {@code scriptWindowSetTextColor} (0x4B61BC). */
    public static int scriptWindowSetTextColor(float r, float g, float b) {
        currentTextColorR = (int) (r * 31.0);
        currentTextColorG = (int) (g * 31.0);
        currentTextColorB = (int) (b * 31.0);

        return 1;
    }

    /** Port of {@code scriptWindowSetHighlightColor} (0x4B6208). */
    public static int scriptWindowSetHighlightColor(float r, float g, float b) {
        currentHighlightColorR = (int) (r * 31.0);
        currentHighlightColorG = (int) (g * 31.0);
        currentHighlightColorB = (int) (b * 31.0);

        return 1;
    }

    /**
     * Port of {@code windowPrintBuf}.
     *
     * @param stringLength C byte length (clamped to 255, then copied).
     * @param width        field width; mutated per alignment exactly like C.
     * @param maxY         absolute y guard ({@code y + lineHeight > maxY} is a no-op).
     * @param textAlignment one of {@link Window.TextAlignment} ids.
     */
    public static void windowPrintBuf(int win, String string, int stringLength, int width, int maxY,
        int x, int y, int color, int textAlignment) {
        if (y + TextFont.fontGetLineHeight() > maxY) {
            return;
        }

        if (stringLength > 255) {
            stringLength = 255;
        }

        String text = string.substring(0, Math.min(stringLength, string.length()));

        int stringWidth = TextFont.fontGetStringWidth(text);
        int stringHeight = TextFont.fontGetLineHeight();
        if (stringWidth == 0 || stringHeight == 0) {
            return;
        }

        if ((color & Color.DRAW_TEXT_FLAG_SHADOWED) != 0) {
            stringWidth++;
            stringHeight++;
        }

        byte[] backgroundBuffer = new byte[stringWidth * stringHeight];
        int backgroundOffset = 0;
        TextFont.fontDrawText(backgroundBuffer, 0, text, stringWidth, stringWidth, color);

        switch (textAlignment) {
        case 0: // TEXT_ALIGNMENT_LEFT
            if (stringWidth < width) {
                width = stringWidth;
            }
            break;
        case 1: // TEXT_ALIGNMENT_RIGHT
            if (stringWidth <= width) {
                x += (width - stringWidth);
                width = stringWidth;
            } else {
                // Wide string: horizontal scroll via staging-buffer offset.
                backgroundOffset = stringWidth - width;
            }
            break;
        default: // TEXT_ALIGNMENT_CENTER
            if (stringWidth <= width) {
                x += (width - stringWidth) / 2;
                width = stringWidth;
            } else {
                // Wide string: horizontal scroll via staging-buffer offset
                // (integer division, exactly like C pointer arithmetic).
                backgroundOffset = (stringWidth - width) / 2;
            }
            break;
        }

        if (stringHeight + y > WindowManager.windowGetHeight(win)) {
            stringHeight = WindowManager.windowGetHeight(win) - y;
        }

        byte[] windowBuffer = WindowManager.windowGetBuffer(win);
        int windowWidth = WindowManager.windowGetWidth(win);
        if ((color & Color.DRAW_TEXT_FLAG_NO_BG) != 0) {
            Draw.blitBufferToBufferTrans(backgroundBuffer, backgroundOffset, width, stringHeight,
                stringWidth, windowBuffer, windowWidth * y + x, windowWidth);
        } else {
            Draw.blitBufferToBuffer(backgroundBuffer, backgroundOffset, width, stringHeight,
                stringWidth, windowBuffer, windowWidth * y + x, windowWidth);
        }
    }

    /**
     * Port of {@code windowWordWrap} (0x4B8638).
     *
     * <p>Char-by-char loop ported exactly: the running width includes letter
     * spacing except at breaks; on overflow it backtracks while the width
     * exceeds the max, then back to the nearest space, splits there and skips
     * spaces. A {@code null} input yields an empty list (C returns
     * {@code nullptr} with length 0).
     *
     * @return the wrapped lines; never {@code null}.
     */
    public static List<String> windowWordWrap(String string, int maxLength, int indent) {
        List<String> substringList = new ArrayList<>();
        if (string == null) {
            return substringList;
        }

        int start = 0;
        int pch = 0;
        int width = indent;
        int length = string.length();
        while (pch < length && string.charAt(pch) != '\0') {
            width += TextFont.fontGetCharacterWidth(string.charAt(pch) & 0xFF);
            if (string.charAt(pch) != '\n' && width <= maxLength) {
                width += TextFont.fontGetLetterSpacing();
                pch++;
            } else {
                while (width > maxLength && pch > start) {
                    width -= TextFont.fontGetCharacterWidth(string.charAt(pch) & 0xFF);
                    pch--;
                }
                if (width > maxLength) {
                    // Degenerate single over-wide char: C backs pch below
                    // start (out-of-bounds read); force progress instead.
                    // Unreachable for real fonts/widths.
                    pch = start + 1;
                }

                if (string.charAt(pch) != '\n') {
                    while (pch != start && string.charAt(pch) != ' ') {
                        pch--;
                    }
                }

                substringList.add(string.substring(start, pch));

                while (pch < length && string.charAt(pch) == ' ') {
                    pch++;
                }

                width = 0;
                start = pch;
            }
        }

        if (start != pch) {
            substringList.add(string.substring(start, pch));
        }

        return substringList;
    }

    /**
     * Port of {@code windowFreeWordList} (0x4B880C).
     *
     * <p>No-op for the GC heap, kept for API parity (C frees each substring
     * and the list).
     */
    public static void windowFreeWordList(List<String> substringList, int substringListLength) {
        // GC owns the list; nothing to free.
    }

    /** Port of {@code windowWrapLineWithSpacing} (0x4B8854). */
    public static void windowWrapLineWithSpacing(int win, String string, int width, int height,
        int x, int y, int color, int textAlignment, int spacing) {
        if (string == null) {
            return;
        }

        List<String> substringList = windowWordWrap(string, width, 0);

        for (int index = 0; index < substringList.size(); index++) {
            String line = substringList.get(index);
            int v1 = y + index * (spacing + TextFont.fontGetLineHeight());
            windowPrintBuf(win, line, line.length(), width, height + y, x, v1, color, textAlignment);
        }

        windowFreeWordList(substringList, substringList.size());
    }

    /** Port of {@code windowWrapLine} (0x4B88FC). */
    public static void windowWrapLine(int win, String string, int width, int height,
        int x, int y, int color, int textAlignment) {
        windowWrapLineWithSpacing(win, string, width, height, x, y, color, textAlignment, 0);
    }

    /**
     * Port of {@code _drawScaled} (pitched). Equal sizes take the fast blit
     * path; otherwise fixed-point 16.16 down/upscale loops. The upscaling
     * branch mixes {@code stepX}/{@code offset++} quirks (integer step only,
     * row repeats via {@code memcpy} advancing by width, not pitch) — ported
     * exactly.
     */
    public static void drawScaled(byte[] dest, int destOfs, int destWidth, int destHeight,
        int destPitch, byte[] src, int srcOfs, int srcWidth, int srcHeight, int srcPitch) {
        if (destWidth == srcWidth && destHeight == srcHeight) {
            Draw.blitBufferToBuffer(src, srcOfs, srcWidth, srcHeight, srcPitch,
                dest, destOfs, destPitch);
            return;
        }

        int incrementX = (srcWidth << 16) / destWidth;
        int incrementY = (srcHeight << 16) / destHeight;
        int stepX = incrementX >> 16;
        int stepY = incrementY >> 16;
        int destSkip = destPitch - destWidth;
        int srcSkip = stepY * srcPitch;

        if (srcSkip != 0) {
            // Downscaling.
            int srcRow = srcOfs;
            int d = destOfs;
            int srcPosY = 0;
            for (int y = 0; y < destHeight; y++) {
                int srcPosX = 0;
                int offset = 0;
                for (int x = 0; x < destWidth; x++) {
                    dest[d++] = src[srcRow + offset];
                    offset += stepX;

                    srcPosX += incrementX;
                    if (srcPosX >= 0x10000) {
                        srcPosX &= 0xFFFF;
                    }
                }

                d += destSkip;
                srcRow += srcSkip;

                srcPosY += stepY;
                if (srcPosY >= 0x10000) {
                    srcPosY &= 0xFFFF;
                    srcRow += srcPitch;
                }
            }
        } else {
            // Upscaling.
            int y = 0;
            int srcPosY = 0;
            int srcRow = srcOfs;
            int d = destOfs;
            while (y < destHeight) {
                int destRowStart = d;

                int srcPosX = 0;
                int offset = 0;
                for (int x = 0; x < destWidth; x++) {
                    dest[d++] = src[srcRow + offset];
                    offset += stepX;

                    srcPosX += stepX;
                    if (srcPosX >= 0x10000) {
                        offset++;
                        srcPosX &= 0xFFFF;
                    }
                }

                y++;
                if (y < destHeight) {
                    d += destSkip;
                    srcPosY += incrementY;

                    while (y < destHeight && srcPosY < 0x10000) {
                        System.arraycopy(dest, destRowStart, dest, d, destWidth);
                        d += destWidth;
                        srcPosY += incrementY;
                        y++;
                    }

                    srcPosY &= 0xFFFF;
                    srcRow += srcPitch;
                }
            }
        }
    }

    /**
     * Port of {@code _drawScaledBuf} (0x4BB5D0, pitchless). Same fixed-point
     * loops with {@code srcWidth}/{@code destWidth} as the row strides.
     */
    public static void drawScaledBuf(byte[] dest, int destOfs, int destWidth, int destHeight,
        byte[] src, int srcOfs, int srcWidth, int srcHeight) {
        if (destWidth == srcWidth && destHeight == srcHeight) {
            System.arraycopy(src, srcOfs, dest, destOfs, srcWidth * srcHeight);
            return;
        }

        int incrementX = (srcWidth << 16) / destWidth;
        int incrementY = (srcHeight << 16) / destHeight;
        int stepX = incrementX >> 16;
        int stepY = incrementY >> 16;
        int srcSkip = stepY * srcWidth;

        if (srcSkip != 0) {
            // Downscaling.
            int srcRow = srcOfs;
            int d = destOfs;
            int srcPosY = 0;
            for (int y = 0; y < destHeight; y++) {
                int srcPosX = 0;
                int offset = 0;
                for (int x = 0; x < destWidth; x++) {
                    dest[d++] = src[srcRow + offset];
                    offset += stepX;

                    srcPosX += incrementX;
                    if (srcPosX >= 0x10000) {
                        srcPosX &= 0xFFFF;
                    }
                }

                srcRow += srcSkip;

                srcPosY += stepY;
                if (srcPosY >= 0x10000) {
                    srcPosY &= 0xFFFF;
                    srcRow += srcWidth;
                }
            }
        } else {
            // Upscaling.
            int y = 0;
            int srcPosY = 0;
            int srcRow = srcOfs;
            int d = destOfs;
            while (y < destHeight) {
                int destRowStart = d;

                int srcPosX = 0;
                int offset = 0;
                for (int x = 0; x < destWidth; x++) {
                    dest[d++] = src[srcRow + offset];
                    offset += stepX;

                    srcPosX += stepX;
                    if (srcPosX >= 0x10000) {
                        offset++;
                        srcPosX &= 0xFFFF;
                    }
                }

                y++;
                if (y < destHeight) {
                    srcPosY += incrementY;

                    while (y < destHeight && srcPosY < 0x10000) {
                        System.arraycopy(dest, destRowStart, dest, d, destWidth);
                        d += destWidth;
                        srcPosY += incrementY;
                        y++;
                    }

                    srcPosY &= 0xFFFF;
                    srcRow += srcWidth;
                }
            }
        }
    }

    /**
     * Port of {@code _fillBuf3x3} (0x4BBFC4, 9-tile stretch). Corner/edge
     * source blocks (including C's swapped Bottom-Left/Top-Right blocks and
     * the {@code destHeight - (destHeight - chunkHeight)} height spellings)
     * are replicated verbatim.
     */
    public static void fillBuf3x3(byte[] src, int srcOfs, int srcWidth, int srcHeight,
        byte[] dest, int destOfs, int destWidth, int destHeight) {
        int chunkWidth = srcWidth / 3;
        int chunkHeight = srcHeight / 3;

        // Middle Middle
        int ptr = srcOfs + srcWidth * chunkHeight + chunkWidth;
        for (int x = 0; x < destWidth; x += chunkWidth) {
            for (int y = 0; y < destHeight; y += chunkHeight) {
                int middleWidth;
                if (x + chunkWidth >= destWidth) {
                    middleWidth = destWidth - x;
                } else {
                    middleWidth = chunkWidth;
                }
                int middleY = y + chunkHeight;
                if (middleY >= destHeight) {
                    middleY = destHeight;
                }
                Draw.blitBufferToBuffer(src, ptr,
                    middleWidth,
                    middleY - y,
                    srcWidth,
                    dest, destOfs + destWidth * y + x,
                    destWidth);
            }
        }

        // Middle Column
        for (int x = 0; x < destWidth; x += chunkWidth) {
            // Top Middle
            int topMiddleX = chunkWidth + x;
            if (topMiddleX >= destWidth) {
                topMiddleX = destWidth;
            }
            int topMiddleHeight = chunkHeight;
            if (topMiddleHeight >= destHeight) {
                topMiddleHeight = destHeight;
            }
            Draw.blitBufferToBuffer(src, srcOfs + chunkWidth,
                topMiddleX - x,
                topMiddleHeight,
                srcWidth,
                dest, destOfs + x,
                destWidth);

            // Bottom Middle
            int bottomMiddleX = chunkWidth + x;
            if (bottomMiddleX >= destWidth) {
                bottomMiddleX = destWidth;
            }
            Draw.blitBufferToBuffer(src, srcOfs + srcWidth * 2 * chunkHeight + chunkWidth,
                bottomMiddleX - x,
                destHeight - (destHeight - chunkHeight),
                srcWidth,
                dest, destOfs + destWidth * (destHeight - chunkHeight) + x,
                destWidth);
        }

        // Middle Row
        for (int y = 0; y < destHeight; y += chunkHeight) {
            // Middle Left
            int middleLeftWidth = chunkWidth;
            if (middleLeftWidth >= destWidth) {
                middleLeftWidth = destWidth;
            }
            int middleLeftY = chunkHeight + y;
            if (middleLeftY >= destHeight) {
                middleLeftY = destHeight;
            }
            Draw.blitBufferToBuffer(src, srcOfs + srcWidth * chunkHeight,
                middleLeftWidth,
                middleLeftY - y,
                srcWidth,
                dest, destOfs + destWidth * y,
                destWidth);

            // Middle Right
            int middleRightY = chunkHeight + y;
            if (middleRightY >= destHeight) {
                middleRightY = destHeight;
            }
            Draw.blitBufferToBuffer(src, srcOfs + 2 * chunkWidth + srcWidth * chunkHeight,
                destWidth - (destWidth - chunkWidth),
                middleRightY - y,
                srcWidth,
                dest, destOfs + destWidth * y + destWidth - chunkWidth,
                destWidth);
        }

        // Top Left
        int topLeftWidth = chunkWidth;
        if (topLeftWidth >= destWidth) {
            topLeftWidth = destWidth;
        }
        int topLeftHeight = chunkHeight;
        if (topLeftHeight >= destHeight) {
            topLeftHeight = destHeight;
        }
        Draw.blitBufferToBuffer(src, srcOfs,
            topLeftWidth,
            topLeftHeight,
            srcWidth,
            dest, destOfs,
            destWidth);

        // Bottom Left
        int bottomLeftHeight = chunkHeight;
        if (chunkHeight >= destHeight) {
            bottomLeftHeight = destHeight;
        }
        Draw.blitBufferToBuffer(src, srcOfs + chunkWidth * 2,
            destWidth - (destWidth - chunkWidth),
            bottomLeftHeight,
            srcWidth,
            dest, destOfs + destWidth - chunkWidth,
            destWidth);

        // Top Right
        int topRightWidth = chunkWidth;
        if (chunkWidth >= destWidth) {
            topRightWidth = destWidth;
        }
        Draw.blitBufferToBuffer(src, srcOfs + srcWidth * 2 * chunkHeight,
            topRightWidth,
            destHeight - (destHeight - chunkHeight),
            srcWidth,
            dest, destOfs + destWidth * (destHeight - chunkHeight),
            destWidth);

        // Bottom Right
        Draw.blitBufferToBuffer(src, srcOfs + 2 * chunkWidth + srcWidth * 2 * chunkHeight,
            destWidth - (destWidth - chunkWidth),
            destHeight - (destHeight - chunkHeight),
            srcWidth,
            dest, destOfs + destWidth * (destHeight - chunkHeight) + (destWidth - chunkWidth),
            destWidth);
    }
}
