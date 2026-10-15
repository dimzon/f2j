package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Split/make/native vectors cross-checked against
 * {@code docker/refharness/golden/v4.txt} ({@code [paths]}); resolve
 * is differential by construction (same fixture layout as the harness).
 */
class PlatformPathsTest {

    @TempDir
    Path temp;

    @AfterEach
    void clearCache() {
        PlatformPaths.clearDirectoryEntryCache();
    }

    private static void assertSplit(String input, String drive, String dir, String fname, String ext) {
        PlatformPaths.Split got = PlatformPaths.splitPath(input);
        assertEquals(new PlatformPaths.Split(drive, dir, fname, ext), got, input);
    }

    @Test
    void split() {
        assertSplit("/a/b/c.txt", "", "/a/b/", "c", ".txt");
        assertSplit("a\\b\\c", "", "a\\b\\", "c", "");
        assertSplit("//server/share/f.txt", "//", "/share/", "f", ".txt");
        assertSplit("noext", "", "", "noext", "");
        assertSplit("a/.h", "", "a/", "", ".h");
        assertSplit("a/b.", "", "a/", "b", ".");
    }

    private static void assertMake(String drive, String dir, String fname, String ext, String expected) {
        assertEquals(expected, PlatformPaths.makePath(drive, dir, fname, ext));
    }

    @Test
    void make() {
        assertMake("", "/a/b/", "c", ".txt", "/a/b/c.txt");
        assertMake("", "a\\b\\", "c", "txt", "a\\b\\c.txt");
        assertMake("//server", "/share/", "f", ".txt", "//server/share/f.txt");
        assertMake("", "", "c", "", "c");
        assertMake("C:", "\\a\\", "", ".txt", "C:\\a\\.txt");
    }

    @Test
    void nativeSeparators() {
        assertEquals("a/b/c/d.txt", PlatformPaths.windowsPathToNative("a\\b/c\\d.txt"));
    }

    // NOTE: reference semantics are the non-_WIN32 branches (Linux
    // container); on Windows the C code takes _splitpath/no-op paths.
    @Test
    @EnabledOnOs(OS.LINUX)
    void resolveRestoresRealCase() throws Exception {
        Path sub = Files.createDirectory(temp.resolve("SubDir"));
        Files.writeString(sub.resolve("MixedCase.Txt"), "data");

        String lower = temp.resolve("subdir/mixedcase.txt").toString();
        String expected = sub.resolve("MixedCase.Txt").toString();
        assertEquals(expected, PlatformPaths.resolvePath(lower));
        assertEquals(expected,
                PlatformPaths.resolvePath(temp.resolve("SUBDIR/MIXEDCASE.TXT").toString()));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void resolveLeavesMissingTail() throws Exception {
        Path sub = Files.createDirectory(temp.resolve("SubDir"));
        Files.writeString(sub.resolve("MixedCase.Txt"), "data");

        String input = temp.resolve("subdir/nope/x").toString();
        String expected = temp.resolve("SubDir/nope/x").toString();
        assertEquals(expected, PlatformPaths.resolvePath(input));

        String missing = temp.resolve("nonexistent/y").toString();
        assertEquals(missing, PlatformPaths.resolvePath(missing));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void resolveCacheIsStaleUntilCleared() throws Exception {
        Path sub = Files.createDirectory(temp.resolve("SubDir"));
        String query = sub.resolve("late.txt").toString();

        // Prime the cache while the file does not exist yet.
        assertEquals(query, PlatformPaths.resolvePath(query.toLowerCase(Locale.ROOT)));

        Files.writeString(sub.resolve("Late.Txt"), "data");
        // Still stale (mirrors the C directory-entry cache).
        assertEquals(query, PlatformPaths.resolvePath(query.toLowerCase(Locale.ROOT)));

        PlatformPaths.clearDirectoryEntryCache();
        assertEquals(sub.resolve("Late.Txt").toString(),
                PlatformPaths.resolvePath(query.toLowerCase(Locale.ROOT)));
    }
}
