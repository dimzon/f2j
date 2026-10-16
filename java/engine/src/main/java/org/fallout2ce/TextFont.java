package org.fallout2ce;

/**
 * Port of {@code src/text_font.h} / {@code src/text_font.cc} (bitmap
 * {@code .fon} fonts, manager registry, metrics, rasterization).
 *
 * <p>Note the endianness trap: {@code .fon} files are little-endian raw
 * x86 dumps (unlike the big-endian FRM headers), so integers are assembled
 * from raw bytes here instead of using {@code Db.fileReadInt32}.
 *
 * <p>Colors arrive as ints: low byte is the palette index, the rest are
 * {@code DRAW_TEXT_FLAG_*} bits (see {@code Color}).
 */
public final class TextFont {

    private TextFont() {
    }

    public static final int TEXT_FONT_MAX = 10;
    public static final int FONT_MANAGER_MAX = 10;

    /** Port of {@code TextFontGlyph}. */
    public static final class Glyph {
        public int width;
        public int dataOffset;
    }

    /** Port of {@code TextFontDescriptor}. */
    public static final class Descriptor {
        public int glyphCount;
        public int lineHeight;
        public int letterSpacing;
        public Glyph[] glyphs;
        public byte[] data;
    }

    /** Font id range claimed by a manager, with its implementation. */
    private static final class Manager {
        int minFont;
        int maxFont;
        FontImpl impl;
    }

    /**
     * Font implementation behind a manager range (C function-pointer block).
     * {@link #TEXT} is the bitmap implementation below; interface fonts
     * plug in their own.
     */
    public interface FontImpl {
        void setCurrent(int font);

        int getLineHeight();

        int getStringWidth(String string);

        int getCharacterWidth(int ch);

        int getMonospacedStringWidth(String string);

        int getLetterSpacing();

        int getBufferSize(String string);

        int getMonospacedCharacterWidth();

        void drawText(byte[] buf, int offset, String string, int length, int pitch, int color);
    }

    private static final Descriptor[] descriptors = new Descriptor[TEXT_FONT_MAX];
    private static final Manager[] managers = new Manager[FONT_MANAGER_MAX];
    private static int managersCount;
    private static int currentFont = -1;
    private static Descriptor currentDescriptor;
    private static FontImpl currentImpl;

    static {
        for (int i = 0; i < TEXT_FONT_MAX; i++) {
            descriptors[i] = new Descriptor();
        }
    }

    /** Port of {@code textFontsInit}. */
    public static int textFontsInit() {
        int current = -1;

        for (int font = 0; font < TEXT_FONT_MAX; font++) {
            if (textFontLoad(font) == 0) {
                if (current == -1) {
                    current = font;
                }
            } else {
                descriptors[font].glyphCount = 0;
            }
        }

        if (current == -1) {
            return -1;
        }

        if (fontManagerAdd(0, 9) == -1) {
            return -1;
        }

        fontSetCurrent(current);

        return 0;
    }

    /** Port of {@code textFontsExit} (frees data; counts stay, as in C). */
    public static void textFontsExit() {
        for (int index = 0; index < TEXT_FONT_MAX; index++) {
            Descriptor descriptor = descriptors[index];
            if (descriptor.glyphCount != 0) {
                descriptor.glyphs = null;
                descriptor.data = null;
            }
        }
    }

    /** Port of {@code textFontLoad}. */
    public static int textFontLoad(int font) {
        if (font < 0 || font >= TEXT_FONT_MAX) {
            // NOTE: C indexes the descriptor array raw (UB outside 0..9).
            return -1;
        }

        Descriptor descriptor = descriptors[font];
        descriptor.data = null;
        descriptor.glyphs = null;

        String language = Settings.settings.system.language;
        // NOTE: DAT entries use backslashes; the C forward slashes rely on
        // the C matcher, so call sites here use backslashes (like Message).
        XFile.Handle stream = Db.fileOpen("text\\" + language + "\\font" + font + ".fon", "rb");
        if (stream == null && !Message.ENGLISH.equalsIgnoreCase(language)) {
            stream = Db.fileOpen("text\\" + Message.ENGLISH + "\\font" + font + ".fon", "rb");
        }
        if (stream == null) {
            stream = Db.fileOpen("font" + font + ".fon", "rb");
            if (stream == null) {
                return -1;
            }
        }

        int rc = -1;
        try {
            int[] header = new int[3];
            if (readInt32LE(stream, header, 0) != 0
                    || readInt32LE(stream, header, 1) != 0
                    || readInt32LE(stream, header, 2) != 0) {
                return -1;
            }
            descriptor.glyphCount = header[0];
            descriptor.lineHeight = header[1];
            descriptor.letterSpacing = header[2];

            // Skip the two stale pointer fields.
            if (skipFully(stream, 8) != 0) {
                return -1;
            }

            if (descriptor.glyphCount <= 0) {
                return -1;
            }

            Glyph[] glyphs = new Glyph[descriptor.glyphCount];
            for (int i = 0; i < glyphs.length; i++) {
                glyphs[i] = new Glyph();
            }
            byte[] raw = new byte[descriptor.glyphCount * 8];
            if (Db.fileRead(raw, 0, raw.length, 1, stream) != 1) {
                return -1;
            }
            for (int i = 0; i < glyphs.length; i++) {
                glyphs[i].width = int32LE(raw, i * 8);
                glyphs[i].dataOffset = int32LE(raw, i * 8 + 4);
            }
            descriptor.glyphs = glyphs;

            Glyph last = glyphs[glyphs.length - 1];
            int dataSize = descriptor.lineHeight * ((last.width + 7) >> 3) + last.dataOffset;
            byte[] data = new byte[dataSize];
            // NOTE: Db.fileRead returns items (bytes / size), unlike C.
            if (Db.fileRead(data, 0, dataSize, 1, stream) != 1) {
                descriptor.glyphs = null;
                return -1;
            }
            descriptor.data = data;

            rc = 0;
        } finally {
            if (rc != 0) {
                descriptor.data = null;
                descriptor.glyphs = null;
            }
            Db.fileClose(stream);
        }

        return rc;
    }

    private static int readInt32LE(XFile.Handle stream, int[] out, int index) {
        byte[] raw = new byte[4];
        if (Db.fileRead(raw, 0, 4, 1, stream) != 1) {
            return -1;
        }
        out[index] = int32LE(raw, 0);
        return 0;
    }

    private static int int32LE(byte[] raw, int offset) {
        return (raw[offset] & 0xFF)
                | (raw[offset + 1] & 0xFF) << 8
                | (raw[offset + 2] & 0xFF) << 16
                | (raw[offset + 3] & 0xFF) << 24;
    }

    private static int skipFully(XFile.Handle stream, int size) {
        byte[] raw = new byte[size];
        return Db.fileRead(raw, 0, size, 1, stream) != 1 ? -1 : 0;
    }

    /**
     * Port of {@code fontManagerAdd}. The overlap check is exclusive-upper
     * ({@code index < maxFont}) while lookup is inclusive — replicated
     * exactly, so {@code {9,9}} slips through (but stays shadowed).
     */
    public static int fontManagerAdd(int minFont, int maxFont) {
        return fontManagerAdd(minFont, maxFont, TEXT);
    }

    /** Port of {@code fontManagerAdd} with an explicit implementation. */
    public static int fontManagerAdd(int minFont, int maxFont, FontImpl impl) {
        if (managersCount >= FONT_MANAGER_MAX) {
            return -1;
        }

        for (int index = minFont; index < maxFont; index++) {
            if (fontManagerFind(index) != -1) {
                return -1;
            }
        }

        Manager manager = new Manager();
        manager.minFont = minFont;
        manager.maxFont = maxFont;
        manager.impl = impl;
        managers[managersCount] = manager;
        managersCount++;

        return 0;
    }

    private static int fontManagerFind(int font) {
        for (int index = 0; index < managersCount; index++) {
            Manager manager = managers[index];
            if (font >= manager.minFont && font <= manager.maxFont) {
                return index;
            }
        }
        return -1;
    }

    private static FontImpl requireImpl() {
        if (currentImpl == null) {
            // NOTE: C calls through a null proc pointer (segfault); fail fast.
            throw new IllegalStateException("no current font");
        }
        return currentImpl;
    }

    /** Port of {@code fontGetCurrent}. */
    public static int fontGetCurrent() {
        return currentFont;
    }

    /** Port of {@code fontSetCurrent}. */
    public static void fontSetCurrent(int font) {
        int manager = fontManagerFind(font);
        if (manager != -1) {
            currentFont = font;
            currentImpl = managers[manager].impl;
            currentImpl.setCurrent(font);
        }
    }

    private static void textFontSetCurrentImpl(int font) {
        if (font < 0 || font >= TEXT_FONT_MAX) {
            return;
        }

        Descriptor descriptor = descriptors[font];
        if (descriptor.glyphCount == 0) {
            return;
        }

        currentDescriptor = descriptor;
    }

    private static Descriptor requireDescriptor() {
        if (currentDescriptor == null) {
            // NOTE: C calls through a null proc pointer (segfault); fail fast.
            throw new IllegalStateException("no current font");
        }
        return currentDescriptor;
    }

    /** The bitmap-font implementation (ports the {@code textFont*} procs). */
    static final FontImpl TEXT = new FontImpl() {
        @Override
        public void setCurrent(int font) {
            textFontSetCurrentImpl(font);
        }

        @Override
        public int getLineHeight() {
            return textGetLineHeight();
        }

        @Override
        public int getStringWidth(String string) {
            return textGetStringWidth(string);
        }

        @Override
        public int getCharacterWidth(int ch) {
            return textGetCharacterWidth(ch);
        }

        @Override
        public int getMonospacedStringWidth(String string) {
            return textGetMonospacedStringWidth(string);
        }

        @Override
        public int getLetterSpacing() {
            return textGetLetterSpacing();
        }

        @Override
        public int getBufferSize(String string) {
            return textGetBufferSize(string);
        }

        @Override
        public int getMonospacedCharacterWidth() {
            return textGetMonospacedCharacterWidth();
        }

        @Override
        public void drawText(byte[] buf, int offset, String string,
                int length, int pitch, int color) {
            textDrawText(buf, offset, string, length, pitch, color);
        }
    };

    /**
     * Port of {@code textFontDrawImpl}. Chars outside the glyph range are
     * skipped; note C reads heap garbage there (nondeterministic), so there
     * is no stable behavior to replicate.
     */
    public static void fontDrawText(byte[] buf, int offset, String string,
            int length, int pitch, int color) {
        if (currentImpl == null) {
            return;
        }
        currentImpl.drawText(buf, offset, string, length, pitch, color);
    }

    private static void textDrawText(byte[] buf, int offset, String string,
            int length, int pitch, int color) {
        if ((color & Color.DRAW_TEXT_FLAG_SHADOWED) != 0) {
            color &= ~Color.DRAW_TEXT_FLAG_SHADOWED;
            fontDrawText(buf, offset + pitch + 1, string, length, pitch, Color.black());
        }

        Descriptor descriptor = currentDescriptor;
        if (descriptor == null) {
            return;
        }

        int monospacedCharacterWidth = 0;
        if ((color & Color.DRAW_TEXT_FLAG_MONOSPACED) != 0) {
            monospacedCharacterWidth = fontGetMonospacedCharacterWidth();
        }

        int pos = offset;
        int end = offset;
        for (int si = 0; si < string.length(); si++) {
            int ch = string.charAt(si);
            if (ch == '\0') {
                break;
            }
            if (ch < descriptor.glyphCount) {
                Glyph glyph = descriptor.glyphs[ch];

                if ((color & Color.DRAW_TEXT_FLAG_MONOSPACED) != 0) {
                    end = pos + monospacedCharacterWidth;
                    pos += (monospacedCharacterWidth - descriptor.letterSpacing - glyph.width) / 2;
                } else {
                    end = pos + glyph.width + descriptor.letterSpacing;
                }

                if (end - offset > length) {
                    break;
                }

                byte[] glyphData = descriptor.data;
                int gi = glyph.dataOffset;
                int ink = color & 0xFF;
                for (int y = 0; y < descriptor.lineHeight; y++) {
                    int bits = 0x80;
                    for (int x = 0; x < glyph.width; x++) {
                        if (bits == 0) {
                            bits = 0x80;
                            gi++;
                        }

                        if ((glyphData[gi] & bits) != 0) {
                            // NOTE: C++ writes through a raw pointer, so rows above
                            // the buffer (e.g. menu pulldown titles with a
                            // negative y) silently land outside; clip them here.
                            if (pos >= 0 && pos < buf.length) {
                                buf[pos] = (byte) ink;
                            }
                        }

                        bits >>= 1;
                        pos++;
                    }
                    gi++;
                    pos += pitch - glyph.width;
                }

                pos = end;
            }
        }

        if ((color & Color.DRAW_TEXT_FLAG_UNDERLINED) != 0) {
            int drawn = pos - offset;
            int underline = offset + pitch * (descriptor.lineHeight - 1);
            byte ink = (byte) (color & 0xFF);
            for (int pix = 0; pix < drawn; pix++) {
                buf[underline++] = ink;
            }
        }
    }

    /** Port of {@code textFontGetLineHeightImpl}. */
    public static int fontGetLineHeight() {
        return requireImpl().getLineHeight();
    }

    private static int textGetLineHeight() {
        return requireDescriptor().lineHeight;
    }

    /** Port of {@code textFontGeStringWidthImpl}. */
    public static int fontGetStringWidth(String string) {
        return requireImpl().getStringWidth(string);
    }

    private static int textGetStringWidth(String string) {
        Descriptor descriptor = requireDescriptor();
        int width = 0;
        for (int i = 0; i < string.length(); i++) {
            int ch = string.charAt(i);
            if (ch == '\0') {
                break;
            }
            if (ch < descriptor.glyphCount) {
                width += descriptor.letterSpacing + descriptor.glyphs[ch].width;
            }
        }
        return width;
    }

    /** Port of {@code textFontGetCharacterWidthImpl}. */
    public static int fontGetCharacterWidth(int ch) {
        return requireImpl().getCharacterWidth(ch);
    }

    private static int textGetCharacterWidth(int ch) {
        Descriptor descriptor = requireDescriptor();
        return descriptor.glyphs[ch & 0xFF].width;
    }

    /** Port of {@code textFontGetMonospacedStringWidthImpl}. */
    public static int fontGetMonospacedStringWidth(String string) {
        return requireImpl().getMonospacedStringWidth(string);
    }

    private static int textGetMonospacedStringWidth(String string) {
        return fontGetMonospacedCharacterWidth() * string.length();
    }

    /** Port of {@code textFontGetLetterSpacingImpl}. */
    public static int fontGetLetterSpacing() {
        return requireImpl().getLetterSpacing();
    }

    private static int textGetLetterSpacing() {
        return requireDescriptor().letterSpacing;
    }

    /** Port of {@code textFontGetBufferSizeImpl}. */
    public static int fontGetBufferSize(String string) {
        return requireImpl().getBufferSize(string);
    }

    private static int textGetBufferSize(String string) {
        return fontGetStringWidth(string) * fontGetLineHeight();
    }

    /** Port of {@code textFontGetMonospacedCharacterWidthImpl}. */
    public static int fontGetMonospacedCharacterWidth() {
        return requireImpl().getMonospacedCharacterWidth();
    }

    private static int textGetMonospacedCharacterWidth() {
        Descriptor descriptor = requireDescriptor();
        int width = 0;
        for (int i = 0; i < descriptor.glyphCount; i++) {
            if (width < descriptor.glyphs[i].width) {
                width = descriptor.glyphs[i].width;
            }
        }
        return width + descriptor.letterSpacing;
    }

    /** Port of {@code fontDrawText2D}. */
    public static void fontDrawText2D(Draw.Buffer dest, int xPos, int yPos,
            String string, int length, int color) {
        assert dest.data() != null && dest.width() > 0 && dest.height() > 0;
        assert xPos >= 0 && xPos < dest.width() && yPos >= 0 && yPos < dest.height()
                && length >= 0 && length < dest.width() - xPos;

        xPos = Math.min(Math.max(xPos, 0), dest.width() - 1);
        yPos = Math.min(Math.max(yPos, 0), dest.height() - 1);
        length = Math.min(Math.max(length, 0), dest.width() - xPos);
        fontDrawText(dest.data(), dest.width() * yPos + xPos, string, length, dest.width(), color);
    }

    /**
     * Port of {@code ScopedFont} (C++ RAII becomes try-with-resources).
     */
    public static final class ScopedFont implements AutoCloseable {
        private final int previousFont;

        public ScopedFont(int font) {
            previousFont = fontGetCurrent();
            fontSetCurrent(font);
        }

        @Override
        public void close() {
            fontSetCurrent(previousFont);
        }
    }

    /** Test-only full reset (mirrors the C static initial state). */
    static void resetForTests() {
        for (int i = 0; i < TEXT_FONT_MAX; i++) {
            descriptors[i] = new Descriptor();
        }
        for (int i = 0; i < FONT_MANAGER_MAX; i++) {
            managers[i] = null;
        }
        managersCount = 0;
        currentFont = -1;
        currentDescriptor = null;
        currentImpl = null;
    }
}
