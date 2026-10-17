package org.fallout2ce;

/**
 * Port of {@code src/font_manager.h} / {@code src/font_manager.cc}
 * (interface {@code .aaf} fonts, ids 100..115).
 *
 * <p>Unlike the little-endian {@code .fon} files, {@code .aaf} is
 * big-endian on disk (the C code byte-swaps after raw reads), so the
 * {@code Db.fileReadInt32/fileReadInt16} helpers fit directly.
 *
 * <p>Glyph pixels are small intensity values selecting a row of the color
 * blend table ({@code (pixel << 8) + background}); real data stays within
 * the table, so the index is used verbatim like C.
 *
 * <p>Scaled floats use {@code Math.round} (half-up); C {@code lround} is
 * half-away-from-zero, which only differs for negative halves — vectors
 * stay non-negative.
 */
public final class InterfaceFont {

    private InterfaceFont() {
    }

    public static final int INTERFACE_FONT_MAX = 16;
    public static final int INTERFACE_FONT_BASE = 100;

    /** Port of {@code InterfaceFontGlyph}. */
    public static final class Glyph {
        public short width;
        public short height;
        public int offset;
    }

    /** Port of {@code InterfaceFontDescriptor}. */
    public static final class Descriptor {
        public short maxHeight;
        public short letterSpacing;
        public short wordSpacing;
        public short lineSpacing;
        public short field8;
        public short fieldA;
        public final Glyph[] glyphs = new Glyph[256];
        public byte[] data;

        Descriptor() {
            for (int i = 0; i < glyphs.length; i++) {
                glyphs[i] = new Glyph();
            }
        }
    }

    private static boolean initialized;
    private static int fontsLength;
    static final Descriptor[] descriptors = new Descriptor[INTERFACE_FONT_MAX];
    private static int currentFont;
    private static Descriptor currentDescriptor;

    static {
        for (int i = 0; i < INTERFACE_FONT_MAX; i++) {
            descriptors[i] = new Descriptor();
        }
    }

    /** The {@link TextFont.FontImpl} plugged into the manager registry. */
    static final TextFont.FontImpl IMPL = new TextFont.FontImpl() {
        @Override
        public void setCurrent(int font) {
            interfaceFontSetCurrentImpl(font);
        }

        @Override
        public int getLineHeight() {
            return interfaceFontGetLineHeight();
        }

        @Override
        public int getStringWidth(String string) {
            return interfaceFontGetStringWidth(string);
        }

        @Override
        public int getCharacterWidth(int ch) {
            return interfaceFontGetCharacterWidth(ch);
        }

        @Override
        public int getMonospacedStringWidth(String string) {
            return interfaceFontGetMonospacedStringWidth(string);
        }

        @Override
        public int getLetterSpacing() {
            return interfaceFontGetLetterSpacing();
        }

        @Override
        public int getBufferSize(String string) {
            return interfaceFontGetBufferSize(string);
        }

        @Override
        public int getMonospacedCharacterWidth() {
            return interfaceFontGetMonospacedCharacterWidth();
        }

        @Override
        public void drawText(byte[] buf, int offset, String string,
                int length, int pitch, int color) {
            interfaceFontDrawText(buf, offset, string, length, pitch, color);
        }
    };

    /** Port of {@code interfaceFontsInit}. */
    public static int interfaceFontsInit() {
        int current = -1;

        for (int font = 0; font < INTERFACE_FONT_MAX; font++) {
            if (interfaceFontLoad(font) == 0) {
                ++fontsLength;

                if (current == -1) {
                    current = font;
                }
            } else {
                descriptors[font].maxHeight = 0;
                descriptors[font].data = null;
            }
        }

        if (current == -1) {
            return -1;
        }

        initialized = true;

        interfaceFontSetCurrentImpl(current + INTERFACE_FONT_BASE);

        return 0;
    }

    /** Port of {@code interfaceFontsExit}. */
    public static void interfaceFontsExit() {
        for (int font = 0; font < INTERFACE_FONT_MAX; font++) {
            if (descriptors[font].data != null) {
                descriptors[font].data = null;
            }
        }
    }

    /** Port of {@code interfaceFontDrawTextScaled2D}. */
    public static void interfaceFontDrawTextScaled2D(Draw.Buffer dest, int x, int y,
            String string, int color, float scale) {
        if (!initialized || dest.data() == null || string == null || string.isEmpty()) {
            return;
        }

        interfaceFontDrawScaled(dest, x, y, string, color, Math.max(scale, 0.01f));
    }

    /** Port of {@code interfaceFontGetStringWidthScaled}. */
    public static int interfaceFontGetStringWidthScaled(String string, int color, float scale) {
        if (!initialized || string == null || string.isEmpty()) {
            return 0;
        }

        return interfaceFontGetScaledWidth(string, color, Math.max(scale, 0.01f));
    }

    /** Port of {@code interfaceFontLoad}. */
    static int interfaceFontLoad(int font) {
        if (font < 0 || font >= INTERFACE_FONT_MAX) {
            // NOTE: C indexes the descriptor array raw (UB outside 0..15).
            return -1;
        }

        Descriptor descriptor = descriptors[font];

        // NOTE: DAT entries use backslashes (like Message/TextFont).
        String language = Settings.settings.system.language;
        XFile.Handle stream = Db.fileOpen("text\\" + language + "\\font" + font + ".aaf", "rb");
        if (stream == null && !Message.ENGLISH.equalsIgnoreCase(language)) {
            stream = Db.fileOpen("text\\" + Message.ENGLISH + "\\font" + font + ".aaf", "rb");
        }
        if (stream == null) {
            stream = Db.fileOpen("font" + font + ".aaf", "rb");
            if (stream == null) {
                return -1;
            }
        }

        try {
            long fileSize = Db.fileGetSize(stream);

            int[] sig = new int[1];
            if (Db.fileReadInt32(stream, sig) != 0) {
                return -1;
            }
            if (sig[0] != 0x41414646) {
                return -1;
            }

            int[] header = new int[4];
            for (int i = 0; i < 4; i++) {
                int[] value = new int[1];
                if (Db.fileReadInt16(stream, value) != 0) {
                    return -1;
                }
                header[i] = value[0];
            }
            descriptor.maxHeight = (short) header[0];
            descriptor.letterSpacing = (short) header[1];
            descriptor.wordSpacing = (short) header[2];
            descriptor.lineSpacing = (short) header[3];

            for (int index = 0; index < 256; index++) {
                Glyph glyph = descriptor.glyphs[index];
                int[] width = new int[1];
                int[] height = new int[1];
                int[] offset = new int[1];
                if (Db.fileReadInt16(stream, width) != 0
                        || Db.fileReadInt16(stream, height) != 0
                        || Db.fileReadInt32(stream, offset) != 0) {
                    return -1;
                }
                glyph.width = (short) width[0];
                glyph.height = (short) height[0];
                glyph.offset = offset[0];
            }

            int glyphDataSize = (int) (fileSize - 2060);
            if (glyphDataSize < 0) {
                // NOTE: C malloc-huge-or-fail path; fail closed.
                return -1;
            }
            byte[] data = new byte[glyphDataSize];
            if (Db.fileRead(data, 0, glyphDataSize, 1, stream) != 1) {
                return -1;
            }
            descriptor.data = data;

            return 0;
        } finally {
            Db.fileClose(stream);
        }
    }

    /** Port of {@code interfaceFontSetCurrentImpl}. */
    static void interfaceFontSetCurrentImpl(int font) {
        if (!initialized) {
            return;
        }

        font -= INTERFACE_FONT_BASE;

        if (font < 0 || font >= INTERFACE_FONT_MAX) {
            // NOTE: C indexes the array raw (UB); ignore instead.
            return;
        }

        if (descriptors[font].data != null) {
            currentFont = font;
            currentDescriptor = descriptors[font];
        }
    }

    /** Port of {@code interfaceFontGetLineHeightImpl}. */
    static int interfaceFontGetLineHeight() {
        if (!initialized) {
            return 0;
        }
        return currentDescriptor.lineSpacing + currentDescriptor.maxHeight;
    }

    /** Port of {@code interfaceFontGetStringWidthImpl}. */
    static int interfaceFontGetStringWidth(String string) {
        if (!initialized) {
            return 0;
        }

        int stringWidth = 0;
        for (int i = 0; i < string.length(); i++) {
            int ch = string.charAt(i) & 0xFF;
            if (ch == ' ') {
                stringWidth += currentDescriptor.wordSpacing;
            } else {
                stringWidth += currentDescriptor.glyphs[ch].width;
            }
            stringWidth += currentDescriptor.letterSpacing;
        }
        return stringWidth;
    }

    /** Port of {@code interfaceFontGetCharacterWidthImpl}. */
    static int interfaceFontGetCharacterWidth(int ch) {
        if (!initialized) {
            return 0;
        }

        int character = ch & 0xFF;
        if (character == ' ') {
            return currentDescriptor.wordSpacing;
        }
        return currentDescriptor.glyphs[character].width;
    }

    /** Port of {@code interfaceFontGetMonospacedStringWidthImpl}. */
    static int interfaceFontGetMonospacedStringWidth(String string) {
        if (!initialized) {
            return 0;
        }
        return interfaceFontGetMonospacedCharacterWidth() * string.length();
    }

    /** Port of {@code interfaceFontGetLetterSpacingImpl}. */
    static int interfaceFontGetLetterSpacing() {
        if (!initialized) {
            return 0;
        }
        return currentDescriptor.letterSpacing;
    }

    /** Port of {@code interfaceFontGetBufferSizeImpl}. */
    static int interfaceFontGetBufferSize(String string) {
        if (!initialized) {
            return 0;
        }
        return interfaceFontGetStringWidth(string) * interfaceFontGetLineHeight();
    }

    /** Port of {@code interfaceFontGetMonospacedCharacterWidthImpl}. */
    static int interfaceFontGetMonospacedCharacterWidth() {
        if (!initialized) {
            return 0;
        }

        int spacing;
        if (currentDescriptor.wordSpacing <= currentDescriptor.field8) {
            spacing = currentDescriptor.lineSpacing;
        } else {
            spacing = currentDescriptor.letterSpacing;
        }
        return spacing + currentDescriptor.maxHeight;
    }

    /** Port of {@code interfaceFontDrawImpl}. */
    static void interfaceFontDrawText(byte[] buf, int offset, String string,
            int length, int pitch, int color) {
        if (!initialized) {
            return;
        }

        if ((color & Color.DRAW_TEXT_FLAG_SHADOWED) != 0) {
            color &= ~Color.DRAW_TEXT_FLAG_SHADOWED;
            // NOTE: unlike text fonts the remaining flags are preserved.
            interfaceFontDrawText(buf, offset + pitch + 1, string, length, pitch,
                Color.black() | color & ~0xFF);
        }

        byte[] palette = Color.getColorBlendTable(color & 0xFF);
        try {
            int monospacedCharacterWidth = 0;
            if ((color & Color.DRAW_TEXT_FLAG_MONOSPACED) != 0) {
                monospacedCharacterWidth = interfaceFontGetMonospacedCharacterWidth();
            }

            int pos = offset;
            int end = offset;
            for (int si = 0; si < string.length(); si++) {
                int ch = string.charAt(si);
                if (ch == '\0') {
                    break;
                }
                ch &= 0xFF;

                int characterWidth;
                if (ch == ' ') {
                    characterWidth = currentDescriptor.wordSpacing;
                } else {
                    characterWidth = currentDescriptor.glyphs[ch].width;
                }

                if ((color & Color.DRAW_TEXT_FLAG_MONOSPACED) != 0) {
                    end = pos + monospacedCharacterWidth;
                    pos += (monospacedCharacterWidth - characterWidth
                        - currentDescriptor.letterSpacing) / 2;
                } else {
                    end = pos + characterWidth + currentDescriptor.letterSpacing;
                }

                if (end - offset > length) {
                    break;
                }

                Glyph glyph = currentDescriptor.glyphs[ch];
                int gi = glyph.offset;

                // Skip blank pixels (font line height vs glyph height).
                pos += (currentDescriptor.maxHeight - glyph.height) * pitch;

                byte[] data = currentDescriptor.data;
                for (int y = 0; y < glyph.height; y++) {
                    for (int x = 0; x < glyph.width; x++) {
                        // NOTE: read bg first: in buf[pos++] = ...buf[pos]...
                        // Java evaluates the left index before the right side
                        // (reads pos + 1), while C reads the old *ptr (UB that
                        // gcc resolves as read-then-increment).
                        int pixel = Byte.toUnsignedInt(data[gi++]);
                        int bg = Byte.toUnsignedInt(buf[pos]);
                        buf[pos++] = palette[(pixel << 8) | bg];
                    }
                    pos += pitch - glyph.width;
                }

                pos = end;
            }

            if ((color & Color.DRAW_TEXT_FLAG_UNDERLINED) != 0) {
                int drawn = pos - offset;
                int underline = offset + pitch * (currentDescriptor.maxHeight - 1);
                byte ink = (byte) (color & 0xFF);
                for (int i = 0; i < drawn; i++) {
                    buf[underline++] = ink;
                }
            }
        } finally {
            Color.freeColorBlendTable(color & 0xFF);
        }
    }

    private static void interfaceFontDrawScaled(Draw.Buffer dest, int x, int y,
            String string, int color, float scale) {
        if ((color & Color.DRAW_TEXT_FLAG_SHADOWED) != 0) {
            color &= ~Color.DRAW_TEXT_FLAG_SHADOWED;
            interfaceFontDrawScaled(dest, x + 1, y + 1, string,
                Color.black() | color & ~0xFF, scale);
        }

        if ((color & (Color.DRAW_TEXT_FLAG_MONOSPACED | Color.DRAW_TEXT_FLAG_UNDERLINED)) != 0) {
            Debug.print("FONTMGR: scaled interface font draw ignores unsupported flags\n");
        }

        byte[] palette = Color.getColorBlendTable(color & 0xFF);
        try {
            float cursorX = x;
            for (int si = 0; si < string.length(); si++) {
                int ch = string.charAt(si);
                if (ch == '\0') {
                    break;
                }
                ch &= 0xFF;

                int characterWidth = ch == ' '
                    ? currentDescriptor.wordSpacing
                    : currentDescriptor.glyphs[ch].width;

                int advance = characterWidth + currentDescriptor.letterSpacing;
                int glyphX = Math.round(cursorX);

                Glyph glyph = currentDescriptor.glyphs[ch];
                if (glyph.width > 0 && glyph.height > 0) {
                    byte[] glyphData = currentDescriptor.data;
                    int glyphBase = glyph.offset;
                    int scaledGlyphWidth = Math.max(1, Math.round(glyph.width * scale));
                    int scaledGlyphHeight = Math.max(1, Math.round(glyph.height * scale));
                    int glyphY = y + Math.max(0,
                        Math.round((currentDescriptor.maxHeight - glyph.height) * scale));

                    int destLeft = Math.max(glyphX, 0);
                    int destTop = Math.max(glyphY, 0);
                    int destRight = Math.min(glyphX + scaledGlyphWidth, dest.width());
                    int destBottom = Math.min(glyphY + scaledGlyphHeight, dest.height());

                    for (int destY = destTop; destY < destBottom; destY++) {
                        int scaledY = destY - glyphY;
                        int srcY = Math.min(Math.max((int) (scaledY / scale), 0), glyph.height - 1);

                        for (int destX = destLeft; destX < destRight; destX++) {
                            int scaledX = destX - glyphX;
                            int srcX = Math.min(Math.max((int) (scaledX / scale), 0), glyph.width - 1);
                            int pixel = Byte.toUnsignedInt(
                                glyphData[glyphBase + srcY * glyph.width + srcX]);
                            int at = destY * dest.width() + destX;
                            dest.data()[at] = palette[(pixel << 8)
                                | Byte.toUnsignedInt(dest.data()[at])];
                        }
                    }
                }

                cursorX += advance * scale;
                if (Math.round(cursorX) >= dest.width()) {
                    break;
                }
            }
        } finally {
            Color.freeColorBlendTable(color & 0xFF);
        }
    }

    private static int interfaceFontGetScaledWidth(String string, int color, float scale) {
        if ((color & (Color.DRAW_TEXT_FLAG_MONOSPACED | Color.DRAW_TEXT_FLAG_UNDERLINED)) != 0) {
            Debug.print("FONTMGR: scaled interface font draw ignores unsupported flags\n");
        }

        float width = 0.0f;
        for (int si = 0; si < string.length(); si++) {
            int ch = string.charAt(si);
            if (ch == '\0') {
                break;
            }
            ch &= 0xFF;
            int characterWidth = ch == ' '
                ? currentDescriptor.wordSpacing
                : currentDescriptor.glyphs[ch].width;
            width += (characterWidth + currentDescriptor.letterSpacing) * scale;
        }

        return Math.max(0, Math.round(width));
    }

    /** Test-only full reset (mirrors the C static initial state). */
    static void resetForTests() {
        initialized = false;
        fontsLength = 0;
        for (int i = 0; i < INTERFACE_FONT_MAX; i++) {
            descriptors[i] = new Descriptor();
        }
        currentFont = 0;
        currentDescriptor = null;
    }
}
