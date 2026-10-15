package org.fallout2ce;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Port of the file half of {@code src/platform_compat.cc}:
 * {@code compat_fopen}/{@code compat_gzopen} (with native-path preparation),
 * {@code compat_fgets}/{@code compat_gzgets} (with {@code \r\n} collapsing),
 * {@code compat_is_dir}, {@code compat_mkdir}/{@code compat_mkdir_recursive},
 * {@code getFileSize}.
 *
 * <p>Every path goes through {@link #prepareNativePath} (truncate to 259,
 * backslashes to slashes, case-insensitive resolution), exactly like C.
 * Write-mode opens clear the resolve cache before and after, like C.
 */
public final class CompatFiles {

    private CompatFiles() {
    }

    /** Mirror of {@code compat_prepare_native_path}. */
    public static String prepareNativePath(String path) {
        String truncated = path.length() > PlatformPaths.MAX_PATH - 1
                ? path.substring(0, PlatformPaths.MAX_PATH - 1)
                : path;
        String nativePath = PlatformPaths.windowsPathToNative(truncated);
        return PlatformPaths.resolvePath(nativePath);
    }

    static boolean modeMayWrite(String mode) {
        return mode != null && (mode.indexOf('w') >= 0 || mode.indexOf('a') >= 0 || mode.indexOf('+') >= 0);
    }

    /**
     * Mirror of {@code compat_fopen}. Modes: {@code r} read, {@code w}
     * truncate+write, {@code a} append, {@code +} read/write; {@code b}/{@code t}
     * ignored (as on Linux). Returns null on failure.
     */
    public static RandomAccessFile fopen(String path, String mode) {
        boolean mayWrite = modeMayWrite(mode);
        if (mayWrite) {
            PlatformPaths.clearDirectoryEntryCache();
        }

        String nativePath = prepareNativePath(path);
        RandomAccessFile raf = openRaw(nativePath, mode);
        if (mayWrite && raf != null) {
            PlatformPaths.clearDirectoryEntryCache();
        }
        return raf;
    }

    private static RandomAccessFile openRaw(String nativePath, String mode) {
        boolean plus = mode.indexOf('+') >= 0;
        boolean write = mode.indexOf('w') >= 0;
        boolean append = mode.indexOf('a') >= 0;
        try {
            // NOTE: like C fopen, missing parents fail (callers mkdir first).
            if (write || append || plus) {
                RandomAccessFile raf = new RandomAccessFile(nativePath, "rw");
                if (write && !plus) {
                    raf.setLength(0);
                }
                if (append) {
                    raf.seek(raf.length());
                }
                return raf;
            }
            return new RandomAccessFile(nativePath, "r");
        } catch (IOException e) {
            return null;
        }
    }

    /** GZIP input with observable stream-end (zlib flags EOF on trailer). */
    public static class EndAwareGzipStream extends GZIPInputStream {
        EndAwareGzipStream(java.io.InputStream in) throws IOException {
            super(in);
        }

        /** True once the inflater consumed the trailer (like zlib). */
        public boolean streamEnded() {
            return inf.finished();
        }
    }

    /** Gzip input side of {@code compat_gzopen} (mode must contain {@code r}). */
    public static EndAwareGzipStream gzopenRead(String path) {
        String nativePath = prepareNativePath(path);
        try {
            return new EndAwareGzipStream(new FileInputStream(nativePath));
        } catch (IOException e) {
            return null;
        }
    }

    /** Gzip output side of {@code compat_gzopen} ({@code w} truncate, {@code a} append). */
    public static GZIPOutputStream gzopenWrite(String path, boolean append) {
        PlatformPaths.clearDirectoryEntryCache();
        String nativePath = prepareNativePath(path);
        try {
            FileOutputStream fos = append
                    ? new FileOutputStream(nativePath, true)
                    : new FileOutputStream(nativePath, false);
            GZIPOutputStream gz = new GZIPOutputStream(fos);
            PlatformPaths.clearDirectoryEntryCache();
            return gz;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Mirror of {@code compat_fgets}: fgets semantics (up to
     * {@code maxCount - 1} chars stopping after {@code \n}), then a trailing
     * {@code \r\n} collapses to {@code \n}. Returns null on EOF/error with
     * nothing read. Reads through {@link RandomAccessFile} byte by byte.
     */
    public static String fgets(RandomAccessFile raf, int maxCount) throws IOException {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < maxCount - 1; i++) {
            int ch = raf.read();
            if (ch < 0) {
                break;
            }
            out.append((char) ch);
            if (ch == '\n') {
                break;
            }
        }
        if (out.length() == 0) {
            return null;
        }
        int len = out.length();
        if (len >= 2 && out.charAt(len - 1) == '\n' && out.charAt(len - 2) == '\r') {
            out.setCharAt(len - 2, '\n');
            out.setLength(len - 1);
        }
        return out.toString();
    }

    /** Mirror of {@code compat_gzgets} over a {@link GZIPInputStream}. */
    public static String gzgets(java.io.InputStream in, int maxCount) throws IOException {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < maxCount - 1; i++) {
            int ch = in.read();
            if (ch < 0) {
                break;
            }
            out.append((char) ch);
            if (ch == '\n') {
                break;
            }
        }
        if (out.length() == 0) {
            return null;
        }
        int len = out.length();
        if (len >= 2 && out.charAt(len - 1) == '\n' && out.charAt(len - 2) == '\r') {
            out.setCharAt(len - 2, '\n');
            out.setLength(len - 1);
        }
        return out.toString();
    }

    /** Mirror of {@code compat_is_dir} (stat follows symlinks). */
    public static boolean isDir(String path) {
        String nativePath = prepareNativePath(path);
        return Files.isDirectory(Paths.get(nativePath));
    }

    /** Mirror of {@code compat_mkdir} (clears resolve cache around). */
    public static int mkdir(String path) {
        PlatformPaths.clearDirectoryEntryCache();
        String nativePath = prepareNativePath(path);
        int rc;
        try {
            Files.createDirectory(Paths.get(nativePath));
            rc = 0;
        } catch (IOException e) {
            rc = -1;
        }
        if (rc == 0) {
            PlatformPaths.clearDirectoryEntryCache();
        }
        return rc;
    }

    /** Mirror of {@code compat_mkdir_recursive}. */
    public static int mkdirRecursive(String path) {
        PlatformPaths.Split split = PlatformPaths.splitPath(path);
        String drive = split.drive();

        int start = drive.length();
        if (start < path.length() && isSeparator(path.charAt(start))) {
            start++;
        }
        for (int i = start; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '\\' || ch == '/') {
                if (mkdir(path.substring(0, i)) < 0) {
                    break;
                }
            }
        }
        return mkdir(path);
    }

    private static boolean isSeparator(char ch) {
        return ch == '\\' || ch == '/';
    }

    /** Mirror of {@code getFileSize(FILE*)} (preserves position). */
    public static long getFileSize(RandomAccessFile raf) throws IOException {
        long originalOffset = raf.getFilePointer();
        long size = raf.length();
        raf.seek(originalOffset);
        return size;
    }

    /** Mirror of {@code compat_filelength(int fd)} kept for callers. */
    public static long fileLength(FileChannel channel) throws IOException {
        long originalOffset = channel.position();
        long size = channel.size();
        channel.position(originalOffset);
        return size;
    }

    /** Open-or-null helper for output streams. */
    public static FileOutputStream openOutput(String nativePath, boolean append) {
        try {
            return append ? new FileOutputStream(nativePath, true) : new FileOutputStream(nativePath, false);
        } catch (IOException e) {
            return null;
        }
    }

    /** Mirror of {@code compat_remove} (clears resolve cache). */
    public static int remove(String path) {
        PlatformPaths.clearDirectoryEntryCache();
        String nativePath = prepareNativePath(path);
        int rc;
        try {
            rc = Files.deleteIfExists(Paths.get(nativePath)) ? 0 : -1;
        } catch (IOException e) {
            rc = -1;
        }
        if (rc == 0) {
            PlatformPaths.clearDirectoryEntryCache();
        }
        return rc;
    }

    /** Mirror of {@code compat_rename} (clears resolve cache). */
    public static int rename(String oldFileName, String newFileName) {
        PlatformPaths.clearDirectoryEntryCache();
        String nativeOld = prepareNativePath(oldFileName);
        String nativeNew = prepareNativePath(newFileName);
        int rc;
        try {
            Files.move(Paths.get(nativeOld), Paths.get(nativeNew));
            rc = 0;
        } catch (IOException e) {
            rc = -1;
        }
        if (rc == 0) {
            PlatformPaths.clearDirectoryEntryCache();
        }
        return rc;
    }

    /** Mirror of {@code compat_file_exists} (stat, not a directory). */
    public static boolean fileExists(String filePath) {
        String nativePath = prepareNativePath(filePath);
        Path p = Paths.get(nativePath);
        return Files.exists(p) && !Files.isDirectory(p);
    }

    /** Mirror of {@code compat_strdup}. */
    public static String strdup(String string) {
        return new String(string);
    }
}
