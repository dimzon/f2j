package org.fallout2ce;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Port of {@code src/file_utils.h} / {@code src/file_utils.cc}.
 *
 * <p>NOTE: gzip <em>output</em> bytes are not compared across
 * implementations (headers/compression differ between zlib builds); tests
 * assert magic bytes + content round-trips, which is what the game needs.
 */
public final class FileUtils {

    private FileUtils() {
    }

    private static final int GZIP_MAGIC_0 = 0x1F;
    private static final int GZIP_MAGIC_1 = 0x8B;

    // 0x452740 gzRealUncompressCopyReal_file
    public static int fileCopyDecompressed(String existingFilePath, String newFilePath) {
        int[] magic;
        try {
            magic = readMagic(existingFilePath);
        } catch (IOException e) {
            return -1;
        }
        if (magic == null) {
            return -1;
        }

        if (magic[0] == GZIP_MAGIC_0 && magic[1] == GZIP_MAGIC_1) {
            try (InputStream in = new GZIPInputStream(Files.newInputStream(Paths.get(existingFilePath)));
                    OutputStream out = Files.newOutputStream(Paths.get(newFilePath))) {
                copyAll(in, out);
            } catch (IOException e) {
                return -1;
            }
        } else {
            copyRaw(existingFilePath, newFilePath);
        }

        return 0;
    }

    // 0x452804 gzcompress_file
    public static int fileCopyCompressed(String existingFilePath, String newFilePath) {
        int[] magic;
        try {
            magic = readMagic(existingFilePath);
        } catch (IOException e) {
            return -1;
        }
        if (magic == null) {
            return -1;
        }

        if (magic[0] == GZIP_MAGIC_0 && magic[1] == GZIP_MAGIC_1) {
            // Already gzipped: plain copy.
            copyRaw(existingFilePath, newFilePath);
        } else {
            try (InputStream in = Files.newInputStream(Paths.get(existingFilePath));
                    OutputStream out = new GZIPOutputStream(Files.newOutputStream(Paths.get(newFilePath)))) {
                copyAll(in, out);
            } catch (IOException e) {
                return -1;
            }
        }

        return 0;
    }

    // NOTE: keeps the inverted magic check from C (gz path is taken when the
    // source is NOT gzip; gunzip then fails on plain data -> -1).
    public static int gzDecompressFile(String existingFilePath, String newFilePath) {
        int[] magic;
        try {
            magic = readMagic(existingFilePath);
        } catch (IOException e) {
            return -1;
        }
        if (magic == null) {
            return -1;
        }

        if (magic[0] != GZIP_MAGIC_0 || magic[1] != GZIP_MAGIC_1) {
            try (InputStream in = new GZIPInputStream(Files.newInputStream(Paths.get(existingFilePath)));
                    OutputStream out = Files.newOutputStream(Paths.get(newFilePath))) {
                copyAll(in, out);
            } catch (IOException e) {
                return -1;
            }
        } else {
            copyRaw(existingFilePath, newFilePath);
        }

        return 0;
    }

    // Like C fgetc: -1 on EOF/short file, null only when not openable.
    private static int[] readMagic(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(path)) {
            return new int[] { in.read(), in.read() };
        }
    }

    private static void copyRaw(String existingFilePath, String newFilePath) {
        try {
            Files.copy(Paths.get(existingFilePath), Paths.get(newFilePath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // NOTE: unreachable in C flow (source was just read for magic).
        }
    }

    private static void copyAll(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[0xFFFF];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
    }
}
