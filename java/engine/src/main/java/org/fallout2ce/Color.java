package org.fallout2ce;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Port of {@code src/color.h} / {@code src/color.cc} (palette tables,
 * {@code colorPaletteLoad}, blend tables, brightness, fades).
 *
 * <p>C {@code unsigned char} maps to Java {@code byte}; read it back with
 * {@link Byte#toUnsignedInt}. The two trailing blend-table rows are
 * <em>not</em> written by {@code _buildBlendTable} (they keep malloc garbage
 * in C); the harness masks them out of the vectors and this port leaves them
 * zero.
 *
 * <p>SDL / video / fps-limiter calls are seams below so unit tests run headless.
 */
public final class Color {

    private Color() {
    }

    public static final int COLOR_COUNT = 256;
    public static final int COLOR_FIRST = 0;
    public static final int COLOR_LAST = 255;
    public static final int COLOR_COMPONENTS_RGB = 3;
    public static final int COLOR_MAP_SIZE = COLOR_COUNT * COLOR_COMPONENTS_RGB;
    public static final int COLOR_PALETTE_SIZE_15BIT = 32768;

    /** Written blend rows; the 2 trailing rows of the 4 KiB table stay untouched. */
    static final int BLEND_WRITTEN_ROWS = 14;
    static final int BLEND_TABLE_SIZE = 4096;

    // DrawTextFlags (unsigned int in C; values fit in signed int).
    public static final int DRAW_TEXT_FLAG_NONE = 0x0000000;
    public static final int DRAW_TEXT_FLAG_SHADOWED = 0x0010000;
    public static final int DRAW_TEXT_FLAG_UNDERLINED = 0x0020000;
    public static final int DRAW_TEXT_FLAG_MONOSPACED = 0x0040000;
    public static final int DRAW_TEXT_FLAG_REFRESH = 0x01000000;
    public static final int DRAW_TEXT_FLAG_NO_BG = 0x02000000;
    public static final int DRAW_TEXT_FLAG_OVERFLOW = 0x04000000;

    public static final int COLOR_INVALID = -1;

    // Mirrors of the C externs.
    public static final byte[] cmap = new byte[COLOR_MAP_SIZE];
    public static final byte[] systemCmap = new byte[COLOR_MAP_SIZE];
    public static final byte[] currentGammaTable = new byte[64];
    public static final byte[][] blendTable = new byte[COLOR_COUNT][];
    public static final byte[] mappedColor = new byte[COLOR_COUNT];
    public static final byte[][] colorMixAddTable = new byte[COLOR_COUNT][COLOR_COUNT];
    public static final byte[][] intensityColorTable = new byte[COLOR_COUNT][COLOR_COUNT];
    public static final byte[][] colorMixMulTable = new byte[COLOR_COUNT][COLOR_COUNT];
    public static final byte[] colorTable = new byte[COLOR_PALETTE_SIZE_15BIT];

    static final String ERROR_NONE = "color.c: No errors\n";
    static final String ERROR_NOT_FOUND = "color.c: color table not found\n";

    private static String error = ERROR_NONE;
    private static boolean colorsInited;
    private static double brightness = 1.0;
    private static final int[] blendRefs = new int[COLOR_COUNT];

    static {
        // _cmap starts as { 0x3F, 0x3F, 0x3F, 0, ... } in C.
        cmap[0] = 0x3F;
        cmap[1] = 0x3F;
        cmap[2] = 0x3F;
    }

    /** Video seam (SDL in the game, assertions in tests). */
    public interface Video {
        void setPalette(byte[] rgb768);

        void setPaletteEntries(byte[] rgb, int start, int count);

        void present();
    }

    /** Fps-limiter seam ({@code sharedFpsLimiter} in C). */
    public interface FpsLimiter {
        void mark();

        void throttle();
    }

    private static final Video NO_VIDEO = new Video() {
        @Override
        public void setPalette(byte[] rgb768) {
        }

        @Override
        public void setPaletteEntries(byte[] rgb, int start, int count) {
        }

        @Override
        public void present() {
        }
    };

    private static final FpsLimiter NO_FPS = new FpsLimiter() {
        @Override
        public void mark() {
        }

        @Override
        public void throttle() {
        }
    };

    public static Video video = NO_VIDEO;
    public static FpsLimiter fpsLimiter = NO_FPS;

    /** Port of {@code gColorFileNameMangler} (mapper override hook). */
    public static Function<String, String> fileNameMangler;

    /** Port of {@code gColorPaletteTransitionCallback}. */
    public static Runnable transitionCallback;

    /**
     * Converts a 24-bit {@code 0xRRGGBB} color to the 15-bit RGB555 index
     * used by {@link #colorTable}. Exact port of the C {@code constexpr}.
     */
    public static int rgb555(int rgb) {
        return ((rgb >> 19) & 0x1F) << 10 | ((rgb >> 11) & 0x1F) << 5 | ((rgb >> 3) & 0x1F);
    }

    /** Port of {@code _calculateColor}. */
    public static int calculateColor(int intensity, int color) {
        return Byte.toUnsignedInt(intensityColorTable[Byte.toUnsignedInt((byte) color)][intensity / 512]);
    }

    /** Port of {@code Color2RGB} (uses the current gamma-corrected cmap). */
    public static int color2RGB(int c) {
        int u = Byte.toUnsignedInt((byte) c) * 3;
        int r = Byte.toUnsignedInt(cmap[u]) >> 1;
        int g = Byte.toUnsignedInt(cmap[u + 1]) >> 1;
        int b = Byte.toUnsignedInt(cmap[u + 2]) >> 1;
        return r << 10 | g << 5 | b;
    }

    /** Port of {@code colorPaletteFadeBetween}. */
    public static void colorPaletteFadeBetween(byte[] oldPalette, byte[] newPalette, int steps) {
        byte[] palette = new byte[COLOR_MAP_SIZE];
        for (int step = 0; step < steps; step++) {
            fpsLimiter.mark();
            for (int index = 0; index < COLOR_MAP_SIZE; index++) {
                int oldEntry = Byte.toUnsignedInt(oldPalette[index]);
                int newEntry = Byte.toUnsignedInt(newPalette[index]);
                palette[index] = (byte) (oldEntry - (oldEntry - newEntry) * step / steps);
            }
            if (transitionCallback != null && step % 128 == 0) {
                transitionCallback.run();
            }
            setSystemPalette(palette);
            video.present();
            fpsLimiter.throttle();
        }
        fpsLimiter.mark();
        setSystemPalette(newPalette);
        video.present();
        fpsLimiter.throttle();
    }

    /** Port of {@code colorPaletteSetTransitionCallback}. */
    public static void colorPaletteSetTransitionCallback(Runnable callback) {
        transitionCallback = callback;
    }

    /** Port of {@code _setSystemPalette}. */
    public static void setSystemPalette(byte[] palette) {
        byte[] transformed = new byte[COLOR_MAP_SIZE];
        for (int index = 0; index < COLOR_MAP_SIZE; index++) {
            transformed[index] = currentGammaTable[Byte.toUnsignedInt(palette[index])];
            systemCmap[index] = palette[index];
        }
        video.setPalette(transformed);
    }

    /** Port of {@code _getSystemPalette}. */
    public static byte[] getSystemPalette() {
        return systemCmap;
    }

    /** Port of {@code _setSystemPaletteEntries}. */
    public static void setSystemPaletteEntries(byte[] palette, int start, int end) {
        int length = end - start + 1;
        byte[] transformed = new byte[length * 3];
        for (int index = 0; index < length; index++) {
            transformed[index * 3] = currentGammaTable[Byte.toUnsignedInt(palette[index * 3])];
            transformed[index * 3 + 1] = currentGammaTable[Byte.toUnsignedInt(palette[index * 3 + 1])];
            transformed[index * 3 + 2] = currentGammaTable[Byte.toUnsignedInt(palette[index * 3 + 2])];

            systemCmap[start * 3 + index * 3] = palette[index * 3];
            systemCmap[start * 3 + index * 3 + 1] = palette[index * 3 + 1];
            systemCmap[start * 3 + index * 3 + 2] = palette[index * 3 + 2];
        }
        video.setPaletteEntries(transformed, start, length);
    }

    private static void setIntensityTableColor(int color) {
        int shift = 0;
        int rgb = color2RGB(color);
        int u = Byte.toUnsignedInt((byte) color);
        for (int index = 0; index < 128; index++) {
            int r = (rgb & 0x7C00) >> 10;
            int g = (rgb & 0x3E0) >> 5;
            int b = rgb & 0x1F;

            int darkerR = r * shift >> 16;
            int darkerG = g * shift >> 16;
            int darkerB = b * shift >> 16;
            int darkerColor = darkerR << 10 | darkerG << 5 | darkerB;
            intensityColorTable[u][index] = colorTable[darkerColor];

            int lighterR = r + ((0x1F - r) * shift >> 16);
            int lighterG = g + ((0x1F - g) * shift >> 16);
            int lighterB = b + ((0x1F - b) * shift >> 16);
            int lighterColor = lighterR << 10 | lighterG << 5 | lighterB;
            intensityColorTable[u][128 + index] = colorTable[lighterColor];

            shift += 512;
        }
    }

    private static void setIntensityTables() {
        for (int index = COLOR_FIRST; index < COLOR_COUNT; index++) {
            if (mappedColor[index] != 0) {
                setIntensityTableColor(index);
            } else {
                Arrays.fill(intensityColorTable[index], (byte) COLOR_FIRST);
            }
        }
    }

    private static void setMixTableColor(int color) {
        int colorRgb = color2RGB(color);
        int u = Byte.toUnsignedInt((byte) color);
        for (int other = COLOR_FIRST; other < COLOR_COUNT; other++) {
            if (mappedColor[u] != 0 && mappedColor[other] != 0) {
                int otherRgb = color2RGB(other);

                int colorR = (colorRgb & 0x7C00) >> 10;
                int colorG = (colorRgb & 0x3E0) >> 5;
                int colorB = colorRgb & 0x1F;

                int otherR = (otherRgb & 0x7C00) >> 10;
                int otherG = (otherRgb & 0x3E0) >> 5;
                int otherB = otherRgb & 0x1F;

                int addedR = colorR + otherR;
                int addedG = colorG + otherG;
                int addedB = colorB + otherB;

                int maxAdded = addedR;
                if (addedG > maxAdded) {
                    maxAdded = addedG;
                }
                if (addedB > maxAdded) {
                    maxAdded = addedB;
                }

                int additive;
                if (maxAdded <= 0x1F) {
                    additive = Byte.toUnsignedInt(colorTable[addedR << 10 | addedG << 5 | addedB]);
                } else {
                    int overflow = maxAdded - 0x1F;

                    int normalizedR = addedR - overflow;
                    int normalizedG = addedG - overflow;
                    int normalizedB = addedB - overflow;

                    if (normalizedR < 0) {
                        normalizedR = 0;
                    }
                    if (normalizedG < 0) {
                        normalizedG = 0;
                    }
                    if (normalizedB < 0) {
                        normalizedB = 0;
                    }

                    int saturated = Byte.toUnsignedInt(
                            colorTable[normalizedR << 10 | normalizedG << 5 | normalizedB]);

                    int intensity = (int) ((((double) maxAdded + (-31.0)) * 0.0078125 + 1.0) * 65536.0);
                    additive = calculateColor(intensity, saturated);
                }

                colorMixAddTable[u][other] = (byte) additive;

                int multipliedR = colorR * otherR >> 5;
                int multipliedG = colorG * otherG >> 5;
                int multipliedB = colorB * otherB >> 5;

                colorMixMulTable[u][other] = colorTable[multipliedR << 10 | multipliedG << 5 | multipliedB];
            } else {
                if (mappedColor[other] != 0) {
                    colorMixAddTable[u][other] = (byte) other;
                    colorMixMulTable[u][other] = (byte) other;
                } else {
                    colorMixAddTable[u][other] = (byte) color;
                    colorMixMulTable[u][other] = (byte) color;
                }
            }
        }
    }

    /** Port of {@code colorPaletteLoad}. */
    public static boolean colorPaletteLoad(String path) {
        if (fileNameMangler != null) {
            path = fileNameMangler.apply(path);
        }

        XFile.Handle stream = Db.fileOpen(path, "rb");
        if (stream == null) {
            error = ERROR_NOT_FOUND;
            return false;
        }

        byte[] one = new byte[1];
        for (int index = COLOR_FIRST; index < COLOR_COUNT; index++) {
            Db.fileRead(one, 0, 1, 1, stream);
            int r = Byte.toUnsignedInt(one[0]);
            Db.fileRead(one, 0, 1, 1, stream);
            int g = Byte.toUnsignedInt(one[0]);
            Db.fileRead(one, 0, 1, 1, stream);
            int b = Byte.toUnsignedInt(one[0]);

            if (r <= 0x3F && g <= 0x3F && b <= 0x3F) {
                mappedColor[index] = 1;
            } else {
                r = 0;
                g = 0;
                b = 0;
                mappedColor[index] = 0;
            }

            cmap[index * 3] = (byte) r;
            cmap[index * 3 + 1] = (byte) g;
            cmap[index * 3 + 2] = (byte) b;
        }

        Db.fileRead(colorTable, 0, COLOR_PALETTE_SIZE_15BIT, 1, stream);

        byte[] typeBytes = new byte[4];
        Db.fileRead(typeBytes, 0, 4, 1, stream);
        int type = Byte.toUnsignedInt(typeBytes[0])
                | Byte.toUnsignedInt(typeBytes[1]) << 8
                | Byte.toUnsignedInt(typeBytes[2]) << 16
                | Byte.toUnsignedInt(typeBytes[3]) << 24;

        if (type == 0x4357454E) { // "NEWC" on little-endian
            for (int row = 0; row < COLOR_COUNT; row++) {
                Db.fileRead(intensityColorTable[row], 0, COLOR_COUNT, 1, stream);
            }
            for (int row = 0; row < COLOR_COUNT; row++) {
                Db.fileRead(colorMixAddTable[row], 0, COLOR_COUNT, 1, stream);
            }
            for (int row = 0; row < COLOR_COUNT; row++) {
                Db.fileRead(colorMixMulTable[row], 0, COLOR_COUNT, 1, stream);
            }
        } else {
            setIntensityTables();

            for (int index = COLOR_FIRST; index < COLOR_COUNT; index++) {
                setMixTableColor(index);
            }
        }

        error = ERROR_NONE;

        rebuildColorBlendTables();

        Db.fileClose(stream);

        return true;
    }

    /** Port of {@code _colorError}. */
    public static String colorError() {
        return error;
    }

    private static void buildBlendTable(byte[] table, int ch) {
        int rgb = color2RGB(ch);
        int r = (rgb & 0x7C00) >> 10;
        int g = (rgb & 0x3E0) >> 5;
        int b = rgb & 0x1F;

        // Row 0: identity.
        for (int i = COLOR_FIRST; i < COLOR_COUNT; i++) {
            table[i] = (byte) i;
        }

        int row = COLOR_COUNT;
        int blendWeight = 6;
        int r2 = r;
        int g2 = g;
        int b2 = b;
        for (int j = 0; j < 7; j++) {
            for (int i = COLOR_FIRST; i < COLOR_COUNT; i++) {
                int mixed = color2RGB(i);
                int mixedR = (mixed & 0x7C00) >> 10;
                int mixedG = (mixed & 0x3E0) >> 5;
                int mixedB = mixed & 0x1F;
                int index = (r2 + mixedR * blendWeight) / 7 << 10
                        | (g2 + mixedG * blendWeight) / 7 << 5
                        | (b2 + mixedB * blendWeight) / 7;
                table[row + i] = colorTable[index];
            }
            blendWeight--;
            row += COLOR_COUNT;
            r2 += r;
            g2 += g;
            b2 += b;
        }

        int shadeStep = 0;
        for (int j = 0; j < 6; j++) {
            int shadeIntensity = shadeStep / 7 + 0xFFFF;
            for (int i = COLOR_FIRST; i < COLOR_COUNT; i++) {
                table[row + i] = (byte) calculateColor(shadeIntensity, ch);
            }
            shadeStep += 0x10000;
            row += COLOR_COUNT;
        }
    }

    private static void rebuildColorBlendTables() {
        for (int i = COLOR_FIRST; i < COLOR_COUNT; i++) {
            if (blendTable[i] != null) {
                buildBlendTable(blendTable[i], i);
            }
        }
    }

    /**
     * Port of {@code _getColorBlendTable}, refcount quirk included: a freshly
     * built table starts at 1 and is incremented on the same call, so one
     * {@code get} needs two {@code free}s to release it.
     */
    public static byte[] getColorBlendTable(int color) {
        int u = Byte.toUnsignedInt((byte) color);
        if (blendTable[u] == null) {
            blendTable[u] = new byte[BLEND_TABLE_SIZE];
            blendRefs[u] = 1;
            buildBlendTable(blendTable[u], u);
        }
        blendRefs[u]++;
        return blendTable[u];
    }

    /** Port of {@code _freeColorBlendTable}. */
    public static void freeColorBlendTable(int color) {
        int u = Byte.toUnsignedInt((byte) color);
        if (blendTable[u] != null) {
            blendRefs[u]--;
            if (blendRefs[u] == 0) {
                blendTable[u] = null;
            }
        }
    }

    /** Port of {@code colorSetBrightness}. */
    public static void colorSetBrightness(double value) {
        brightness = value;
        for (int i = 0; i < 64; i++) {
            double gamma = Math.pow(i, brightness);
            if (gamma < 0.0) {
                gamma = 0.0;
            } else if (gamma > 63.0) {
                gamma = 63.0;
            }
            currentGammaTable[i] = (byte) (int) gamma;
        }
        setSystemPalette(systemCmap);
    }

    /** Port of {@code _initColors}. */
    public static boolean initColors() {
        if (colorsInited) {
            return true;
        }
        colorsInited = true;
        colorSetBrightness(1.0);
        if (!colorPaletteLoad("color.pal")) {
            return false;
        }
        setSystemPalette(cmap);
        return true;
    }

    /**
     * Port of {@code _colorsClose}: releases blend tables only (C does not
     * zero the other tables or reset the init flag).
     */
    public static void colorsClose() {
        for (int index = COLOR_FIRST; index < COLOR_COUNT; index++) {
            freeColorBlendTable(index);
        }
    }

    /** Test-only full reset (mirrors the C static initial state). */
    static void resetForTests() {
        Arrays.fill(cmap, (byte) 0);
        cmap[0] = 0x3F;
        cmap[1] = 0x3F;
        cmap[2] = 0x3F;
        Arrays.fill(systemCmap, (byte) 0);
        Arrays.fill(currentGammaTable, (byte) 0);
        Arrays.fill(mappedColor, (byte) 0);
        Arrays.fill(colorTable, (byte) 0);
        for (int i = 0; i < COLOR_COUNT; i++) {
            blendTable[i] = null;
            blendRefs[i] = 0;
            Arrays.fill(colorMixAddTable[i], (byte) 0);
            Arrays.fill(intensityColorTable[i], (byte) 0);
            Arrays.fill(colorMixMulTable[i], (byte) 0);
        }
        error = ERROR_NONE;
        colorsInited = false;
        brightness = 1.0;
        fileNameMangler = null;
        transitionCallback = null;
        video = NO_VIDEO;
        fpsLimiter = NO_FPS;
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
}
