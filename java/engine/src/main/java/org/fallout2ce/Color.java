package org.fallout2ce;

/**
 * Port of {@code src/color.h} (tables, flags, {@code rgb555}) — pure part.
 *
 * <p>C {@code unsigned char} maps to Java {@code byte}; read it back with
 * {@link Byte#toUnsignedInt} (accessors below already do). Palette loading
 * ({@code colorPaletteLoad}, {@code _initColors}, blending tables) needs
 * game files + SDL and is phase 2: {@link #initColors()} is an explicit
 * stub until then.
 */
public final class Color {

    private Color() {
    }

    public static final int COLOR_COUNT = 256;
    public static final int COLOR_COMPONENTS_RGB = 3;
    public static final int COLOR_MAP_SIZE = COLOR_COUNT * COLOR_COMPONENTS_RGB;
    public static final int COLOR_PALETTE_SIZE_15BIT = 32768;

    // DrawTextFlags (unsigned int in C; values fit in signed int).
    public static final int DRAW_TEXT_FLAG_NONE = 0x0000000;
    public static final int DRAW_TEXT_FLAG_SHADOWED = 0x0010000;
    public static final int DRAW_TEXT_FLAG_UNDERLINED = 0x0020000;
    public static final int DRAW_TEXT_FLAG_MONOSPACED = 0x0040000;
    public static final int DRAW_TEXT_FLAG_REFRESH = 0x01000000;
    public static final int DRAW_TEXT_FLAG_NO_BG = 0x02000000;
    public static final int DRAW_TEXT_FLAG_OVERFLOW = 0x04000000;

    public static final int COLOR_INVALID = -1;

    // Mirrors of the C externs (zero-initialized until initColors, as in C).
    public static final byte[] cmap = new byte[COLOR_MAP_SIZE];
    public static final byte[] systemCmap = new byte[COLOR_MAP_SIZE];
    public static final byte[] currentGammaTable = new byte[64];
    public static final byte[][] blendTable = new byte[COLOR_COUNT][];
    public static final byte[] mappedColor = new byte[COLOR_COUNT];
    public static final byte[][] colorMixAddTable = new byte[COLOR_COUNT][COLOR_COUNT];
    public static final byte[][] intensityColorTable = new byte[COLOR_COUNT][COLOR_COUNT];
    public static final byte[][] colorMixMulTable = new byte[COLOR_COUNT][COLOR_COUNT];
    public static final byte[] colorTable = new byte[COLOR_PALETTE_SIZE_15BIT];

    /**
     * Converts a 24-bit {@code 0xRRGGBB} color to the 15-bit RGB555 index
     * used by {@link #colorTable}. Exact port of the C {@code constexpr}.
     */
    public static int rgb555(int rgb) {
        return ((rgb >> 19) & 0x1F) << 10 | ((rgb >> 11) & 0x1F) << 5 | ((rgb >> 3) & 0x1F);
    }

    // Named colors (C macros over _colorTable[...]); unsigned 0..255.
    public static int lightRed() {
        return palette(0xFF5252);
    }

    public static int red() {
        return palette(0xFF0000);
    }

    public static int red2() {
        return palette(0xE60000);
    }

    public static int darkRed() {
        return palette(0xBD1042);
    }

    public static int white() {
        return palette(0xFFFFFF);
    }

    public static int paleBlue() {
        return palette(0xC5D6FF);
    }

    public static int lightGrey() {
        return palette(0xA5A5A5);
    }

    public static int lightGrey2() {
        return palette(0x9C94A5);
    }

    public static int grey() {
        return palette(0x8C8C8C);
    }

    public static int grey2() {
        return palette(0x7B7B7B);
    }

    public static int darkGrey() {
        return palette(0x424242);
    }

    public static int darkGrey2() {
        return palette(0x525252);
    }

    public static int darkGrey3() {
        return palette(0x212121);
    }

    public static int black() {
        return palette(0x000000);
    }

    public static int black2() {
        return palette(0x000010);
    }

    public static int cyan() {
        return palette(0x4AFFFF);
    }

    public static int lightBlue() {
        return palette(0x315A8C);
    }

    public static int blue() {
        return palette(0x0000FF);
    }

    public static int blue2() {
        return palette(0x0000AD);
    }

    public static int sageGreen() {
        return palette(0x8CBD8C);
    }

    public static int lightPink() {
        return palette(0xFF7B7B);
    }

    public static int magenta() {
        return palette(0xFF00FF);
    }

    public static int lightPink2() {
        return palette(0xF77B7B);
    }

    public static int lightGreen() {
        return palette(0x007300);
    }

    public static int lightGreen2() {
        return palette(0x429C21);
    }

    public static int lightGreen3() {
        return palette(0x00C500);
    }

    public static int green() {
        return palette(0x00FF00);
    }

    public static int darkGreen() {
        return palette(0x007B00);
    }

    public static int darkGreen2() {
        return palette(0x084A08);
    }

    public static int grassGreen() {
        return palette(0x7B7B29);
    }

    public static int lightYellow() {
        return palette(0xFFFF5A);
    }

    public static int lightGold() {
        return palette(0xF7EF10);
    }

    public static int lightGold2() {
        return palette(0xEFEF42);
    }

    public static int amber() {
        return palette(0xFF9442);
    }

    public static int yellow2() {
        return palette(0xA59C19);
    }

    public static int sand() {
        return palette(0xAD844A);
    }

    public static int yellowGreen() {
        return palette(0xEFFF08);
    }

    public static int darkYellow() {
        return palette(0x948C19);
    }

    public static int darkYellow2() {
        return palette(0x736319);
    }

    public static int darkYellow3() {
        return palette(0x947B29);
    }

    public static int darkYellow4() {
        return palette(0x948C08);
    }

    public static int lightOrange() {
        return palette(0xFFBD7B);
    }

    public static int darkBrown() {
        return palette(0x634210);
    }

    public static int olive() {
        return palette(0xADAD5A);
    }

    public static int dullBrown() {
        return palette(0x6B5A4A);
    }

    private static int palette(int rgb) {
        return Byte.toUnsignedInt(colorTable[rgb555(rgb)]);
    }

    /**
     * Stub for {@code _initColors} (needs {@code color.pal} + SDL palette).
     * Returns {@code false} until the file-I/O phase lands.
     */
    public static boolean initColors() {
        // TODO(phase2): port color.cc palette loading (db/xfile + SDL).
        return false;
    }

    /** Port of {@code _colorsClose} (tables back to zero). */
    public static void colorsClose() {
        java.util.Arrays.fill(cmap, (byte) 0);
        java.util.Arrays.fill(systemCmap, (byte) 0);
        java.util.Arrays.fill(currentGammaTable, (byte) 0);
        java.util.Arrays.fill(mappedColor, (byte) 0);
        java.util.Arrays.fill(colorTable, (byte) 0);
        for (int i = 0; i < COLOR_COUNT; i++) {
            blendTable[i] = null;
            java.util.Arrays.fill(colorMixAddTable[i], (byte) 0);
            java.util.Arrays.fill(intensityColorTable[i], (byte) 0);
            java.util.Arrays.fill(colorMixMulTable[i], (byte) 0);
        }
    }
}
