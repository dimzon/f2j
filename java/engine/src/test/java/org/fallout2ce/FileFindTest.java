package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Non-{@code _WIN32} branch ({@code fpattern_match} with pre-lowered sides).
 * NOTE: patterns are glued as strings — {@code Path.resolve} rejects
 * wildcards on Windows.
 */
class FileFindTest {

    @TempDir
    Path temp;

    static String query(Path dir, String pattern) {
        return dir.toString() + "\\" + pattern;
    }

    @Test
    void findsByPattern() throws Exception {
        Files.writeString(temp.resolve("a.txt"), "a");
        Files.writeString(temp.resolve("B.MSG"), "b");
        Files.createDirectory(temp.resolve("sub"));

        FileFind.Search search = new FileFind.Search();
        assertTrue(FileFind.fileFindFirst(query(temp, "*.txt"), search));
        assertEquals("a.txt", FileFind.fileFindGetName(search));
        assertFalse(FileFind.fileFindIsDirectory(search));
        FileFind.findFindClose(search);
    }

    @Test
    void iteratesAllAndReportsDirs() throws Exception {
        Files.writeString(temp.resolve("a.txt"), "a");
        Files.writeString(temp.resolve("B.MSG"), "b");
        Files.createDirectory(temp.resolve("sub"));

        FileFind.Search search = new FileFind.Search();
        assertTrue(FileFind.fileFindFirst(query(temp, "*"), search));
        Set<String> names = new HashSet<>();
        boolean sawDir = false;
        do {
            names.add(FileFind.fileFindGetName(search));
            if (FileFind.fileFindIsDirectory(search)) {
                sawDir = true;
                assertEquals("sub", FileFind.fileFindGetName(search));
            }
        } while (FileFind.fileFindNext(search));
        assertEquals(Set.of("a.txt", "B.MSG", "sub"), names);
        assertTrue(sawDir);
    }

    @Test
    void missingDirFails() {
        FileFind.Search search = new FileFind.Search();
        assertFalse(FileFind.fileFindFirst(query(temp.resolve("nope"), "*.txt"), search));
    }

    @Test
    void noMatchFails() throws Exception {
        Files.writeString(temp.resolve("a.txt"), "a");
        FileFind.Search search = new FileFind.Search();
        assertFalse(FileFind.fileFindFirst(query(temp, "*.zzz"), search));
    }
}
