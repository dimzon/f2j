package org.fallout2ce;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Port of the pure path helpers from {@code src/platform_compat.cc}.
 *
 * <p>Only the non-{@code _WIN32} branches are ported (the reference
 * container is Linux; MSVC uses {@code _splitpath}/{@code _makepath}).
 * {@link #resolvePath} walks the real filesystem case-insensitively,
 * exactly like C, including its directory-listing cache (stale until
 * evicted — see {@link #clearDirectoryEntryCache}).
 */
public final class PlatformPaths {

    private PlatformPaths() {
    }

    public static final int MAX_PATH = 260;
    public static final int MAX_DRIVE = 3;
    public static final int MAX_DIR = 256;
    public static final int MAX_FNAME = 256;
    public static final int MAX_EXT = 256;

    private static final int DIRECTORY_CACHE_MAX_SIZE = 1024;

    /** Mirror of the splitpath out-params (C allows null = skip; here all four). */
    public record Split(String drive, String dir, String fname, String ext) {
    }

    static boolean isPathSeparator(char ch) {
        return ch == '/' || ch == '\\';
    }

    // Non-Windows compat_splitpath.
    public static Split splitPath(String path) {
        int driveStart = 0;
        int pathAfterDrive = 0;
        if (path.length() > 1
                && isPathSeparator(path.charAt(0))
                && isPathSeparator(path.charAt(1))) {
            pathAfterDrive = 2;
            while (pathAfterDrive < path.length()
                    && !isPathSeparator(path.charAt(pathAfterDrive))
                    && path.charAt(pathAfterDrive) != '.') {
                pathAfterDrive++;
            }
        }

        int driveSize = Math.min(pathAfterDrive - driveStart, MAX_DRIVE - 1);
        String drive = path.substring(driveStart, driveStart + driveSize);

        int fnameStart = pathAfterDrive;
        for (int i = pathAfterDrive; i < path.length(); i++) {
            if (isPathSeparator(path.charAt(i))) {
                fnameStart = i + 1;
            }
        }

        int end = path.length();
        int extStart = end;
        for (int p = end; p != fnameStart; p--) {
            if (path.charAt(p - 1) == '.') {
                extStart = p - 1;
                break;
            }
        }

        String dir = path.substring(pathAfterDrive, Math.min(fnameStart, pathAfterDrive + MAX_DIR - 1));
        String fname = path.substring(fnameStart, Math.min(extStart, fnameStart + MAX_FNAME - 1));
        String ext = path.substring(extStart, Math.min(end, extStart + MAX_EXT - 1));

        return new Split(drive, dir, fname, ext);
    }

    // Non-Windows compat_makepath. The cursor dance (write '/', maybe
    // overwrite it) is mirrored exactly — see golden v4 `make` vectors.
    public static String makePath(String drive, String dir, String fname, String ext) {
        StringBuilder buf = new StringBuilder();
        int pos = 0;

        if (!drive.isEmpty()) {
            buf.append(drive);
            pos = buf.length();
            if (isPathSeparator(buf.charAt(pos - 1))) {
                pos--;
            } else {
                buf.append('/');
            }
        }

        if (!dir.isEmpty()) {
            if (!isPathSeparator(dir.charAt(0)) && isPathSeparator(peek(buf, pos))) {
                pos++;
            }
            pos = blit(buf, pos, dir);
            if (isPathSeparator(buf.charAt(pos - 1))) {
                pos--;
            } else {
                if (pos < buf.length()) {
                    buf.setCharAt(pos, '/');
                } else {
                    buf.append('/');
                }
            }
        }

        if (!fname.isEmpty()) {
            if (!isPathSeparator(fname.charAt(0)) && isPathSeparator(peek(buf, pos))) {
                pos++;
            }
            pos = blit(buf, pos, fname);
        } else {
            if (isPathSeparator(peek(buf, pos))) {
                pos++;
            }
        }

        if (!ext.isEmpty()) {
            if (ext.charAt(0) != '.') {
                if (pos < buf.length()) {
                    buf.setCharAt(pos, '.');
                } else {
                    buf.append('.');
                }
                pos++;
            }
            pos = blit(buf, pos, ext);
        }

        return buf.substring(0, Math.min(pos, buf.length()));
    }

    // Non-Windows compat_windows_path_to_native.
    public static String windowsPathToNative(String path) {
        return path.replace('\\', '/');
    }

    // Non-Windows compat_resolve_path. Returns the resolved path (C edits
    // in place); unmatched tail is left as-is.
    public static String resolvePath(String path) {
        StringBuilder sb = new StringBuilder(path);
        String directoryPath;
        int pch;
        if (sb.length() > 0 && sb.charAt(0) == '/') {
            directoryPath = "/";
            pch = 1;
        } else {
            directoryPath = ".";
            pch = 0;
        }

        while (true) {
            while (pch < sb.length() && sb.charAt(pch) == '/') {
                pch++;
            }

            if (pch >= sb.length()) {
                break;
            }

            int sep = -1;
            for (int i = pch; i < sb.length(); i++) {
                if (sb.charAt(i) == '/') {
                    sep = i;
                    break;
                }
            }
            int end = sep >= 0 ? sep : sb.length();

            Map<String, String> entries = directoryEntryCacheGet(directoryPath);
            if (entries == null) {
                break;
            }

            String component = sb.substring(pch, end);
            String real = entries.get(asciiLower(component));
            if (real == null) {
                break;
            }

            // NOTE: C strncpy's `length` bytes; on match lengths are equal.
            sb.replace(pch, end, real);

            if (sep < 0) {
                break;
            }

            directoryPath = sb.substring(0, sep);
            pch = sep + 1;
        }

        return sb.toString();
    }

    /** Test/future-rename seam: C clears this cache on rename. */
    public static void clearDirectoryEntryCache() {
        directoryEntryCache.clear();
    }

    // Reads *path with a zero-initial buffer (C zeroes path[0] first).
    private static char peek(StringBuilder buf, int pos) {
        return pos < buf.length() ? buf.charAt(pos) : 0;
    }

    // strcpy-at-pos overwrite; returns the new end.
    private static int blit(StringBuilder buf, int pos, String s) {
        for (int i = 0; i < s.length(); i++) {
            if (pos + i < buf.length()) {
                buf.setCharAt(pos + i, s.charAt(i));
            } else {
                buf.append(s.charAt(i));
            }
        }
        return pos + s.length();
    }

    private static String asciiLower(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            out.append(ch >= 'A' && ch <= 'Z' ? (char) (ch + ('a' - 'A')) : ch);
        }
        return out.toString();
    }

    private static final Map<String, Map<String, String>> directoryEntryCache = new HashMap<>();

    private static Map<String, String> directoryEntryCacheGet(String directoryPath) {
        Map<String, String> existing = directoryEntryCache.get(directoryPath);
        if (existing != null) {
            return existing;
        }

        Path dir = Paths.get(directoryPath);
        Map<String, String> entries = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                entries.put(asciiLower(name), name);
            }
        } catch (IOException | java.nio.file.InvalidPathException e) {
            return null;
        }

        if (directoryEntryCache.size() >= DIRECTORY_CACHE_MAX_SIZE) {
            directoryEntryCache.clear();
        }
        directoryEntryCache.put(directoryPath, entries);
        return entries;
    }
}
