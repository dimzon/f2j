package org.fallout2ce;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Port of {@code src/datafile.h} / {@code src/datafile.cc} (PCX/raw image
 * loading with palette remap).
 */
public final class Datafile {

    private Datafile() {
    }

    /** Decoded image: pixels (remapped unless raw) plus dimensions. */
    public record Image(byte[] data, int width, int height) {
    }

    /** Port of {@code gDatafilePalette}. */
    public static final byte[] datafilePalette = new byte[Color.COLOR_MAP_SIZE];

    /** Port of {@code gDatafileNameMangler} (identity default). */
    public static Function<String, String> fileNameMangler = Function.identity();

    /** Port of {@code gDatafileLoader} (null default). */
    public interface Loader {
        byte[] load(String path, byte[] palette, int[] dims);
    }

    public static Loader loader;

    /** Port of {@code datafileRemapPixelsRgb8}. */
    public static void datafileRemapPixelsRgb8(byte[] data, byte[] palette, int width, int height) {
        byte[] indexed = new byte[Color.COLOR_COUNT];
        indexed[0] = (byte) Color.COLOR_FIRST;
        for (int index = 1; index < Color.COLOR_COUNT; index++) {
            int r = Byte.toUnsignedInt(palette[index * 3]) >> 3;
            int g = Byte.toUnsignedInt(palette[index * 3 + 1]) >> 3;
            int b = Byte.toUnsignedInt(palette[index * 3 + 2]) >> 3;
            indexed[index] = Color.colorTable[r << 10 | g << 5 | b];
        }

        int size = width * height;
        for (int index = 0; index < size; index++) {
            data[index] = indexed[Byte.toUnsignedInt(data[index])];
        }
    }

    /** Port of {@code datafileRemapPixelsRgb6} (unused in the game). */
    public static void datafileRemapPixelsRgb6(byte[] data, byte[] palette, int width, int height) {
        byte[] indexed = new byte[Color.COLOR_COUNT];
        indexed[0] = (byte) Color.COLOR_FIRST;
        for (int index = 1; index < Color.COLOR_COUNT; index++) {
            int r = Byte.toUnsignedInt(palette[index * 3]) >> 1;
            int g = Byte.toUnsignedInt(palette[index * 3 + 1]) >> 1;
            int b = Byte.toUnsignedInt(palette[index * 3 + 2]) >> 1;
            indexed[index] = Color.colorTable[r << 10 | g << 5 | b];
        }

        int size = width * height;
        for (int index = 0; index < size; index++) {
            data[index] = indexed[Byte.toUnsignedInt(data[index])];
        }
    }

    /** Port of {@code datafileReadRaw} (null when missing and unhandled). */
    public static Image datafileReadRaw(String path) {
        String mangled = fileNameMangler.apply(path);
        int dot = mangled.lastIndexOf('.');
        if (dot != -1 && mangled.substring(dot + 1).equalsIgnoreCase("pcx")) {
            int[] width = new int[1];
            int[] height = new int[1];
            byte[] data = Pcx.pcxRead(mangled, width, height, datafilePalette);
            if (data == null) {
                return null;
            }
            return new Image(data, width[0], height[0]);
        }

        if (loader != null) {
            int[] dims = new int[2];
            byte[] data = loader.load(mangled, datafilePalette, dims);
            if (data == null) {
                return null;
            }
            return new Image(data, dims[0], dims[1]);
        }

        return null;
    }

    /** Port of {@code datafileRead} (raw plus 8-bit remap). */
    public static Image datafileRead(String path) {
        Image image = datafileReadRaw(path);
        if (image != null) {
            datafileRemapPixelsRgb8(image.data(), datafilePalette, image.width(), image.height());
        }
        return image;
    }

    /** Port of {@code datafileLoadPalette} (palette out, pixels dropped). */
    public static byte[] datafileLoadPalette(String path) {
        if (datafileReadRaw(path) != null) {
            return datafilePalette;
        }
        return null;
    }

    /**
     * Port of {@code datafilePackUntilZero}: compacts the nonzero prefix
     * in place, returns the new dimensions.
     */
    public static int[] datafilePackUntilZero(byte[] data, int width, int height) {
        if (width <= 0 || height <= 0) {
            return new int[] { 0, 0 };
        }

        byte[] compact = new byte[width * height];
        int write = 0;

        int y = 0;
        int x = 0;
        int rowStart = 0;

        for (y = 0; y < height; y++) {
            if (data[rowStart] == 0) {
                break;
            }

            int current = rowStart;
            for (x = 0; x < width; x++) {
                if (data[current] == 0) {
                    break;
                }
                compact[write++] = data[current++];
            }

            rowStart += width;
        }

        System.arraycopy(compact, 0, data, 0, x * y);

        return new int[] { x, y };
    }

    /** Port of {@code datafileGetPalette}. */
    public static byte[] datafileGetPalette() {
        return datafilePalette;
    }

    /** Port of {@code datafileLoad} (raw block, null when missing). */
    public static byte[] datafileLoad(String path) {
        String mangled = fileNameMangler.apply(path);
        XFile.Handle stream = Db.fileOpen(mangled, "rb");
        if (stream == null) {
            return null;
        }

        long size = Db.fileGetSize(stream);
        byte[] data = new byte[(int) size];
        Db.fileRead(data, 0, (int) size, 1, stream);
        Db.fileClose(stream);
        return data;
    }

    /** Test-only reset (identity mangler, no loader, zero palette). */
    static void resetForTests() {
        Arrays.fill(datafilePalette, (byte) 0);
        fileNameMangler = Function.identity();
        loader = null;
    }
}
