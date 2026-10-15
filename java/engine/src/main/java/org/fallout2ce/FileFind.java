package org.fallout2ce;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Port of the non-{@code _WIN32} branch of {@code src/file_find.cc}.
 *
 * <p>The pattern is the filename part (reassembled via makepath) and both
 * sides are ASCII-lowercased before matching, so the plain
 * {@link FPattern#match} (which folds the same way) is exactly equivalent
 * to the C UNIX-variant call. Symlinks report as non-directories
 * ({@code d_type == DT_LNK} in C).
 */
public final class FileFind {

    private FileFind() {
    }

    /** Mirror of {@code DirectoryFileFindData} (non-Windows layout). */
    public static final class Search {
        public String pattern = "";
        public DirectoryStream<Path> dir;
        public Iterator<Path> it;
        public String name = "";
        public boolean isDirectory;
    }

    // 0x4E6380 xsys_findfirst.
    public static boolean fileFindFirst(String path, Search search) {
        PlatformPaths.Split split = PlatformPaths.splitPath(path);
        search.pattern = asciiLower(PlatformPaths.makePath("", "", split.fname(), split.ext()));

        String basePath = PlatformPaths.resolvePath(
                PlatformPaths.makePath(split.drive(), split.dir(), "", ""));

        DirectoryStream<Path> dir;
        try {
            dir = Files.newDirectoryStream(Paths.get(basePath));
        } catch (IOException e) {
            return false;
        }
        search.dir = dir;
        search.it = dir.iterator();

        if (!advance(search)) {
            closeQuietly(search);
            return false;
        }
        return true;
    }

    // 0x4E63A8 xsys_findnext.
    public static boolean fileFindNext(Search search) {
        if (!advance(search)) {
            closeQuietly(search);
            return false;
        }
        return true;
    }

    // 0x4E63CC xsys_findclose.
    public static boolean findFindClose(Search search) {
        closeQuietly(search);
        return true;
    }

    public static boolean fileFindIsDirectory(Search search) {
        return search.isDirectory;
    }

    public static String fileFindGetName(Search search) {
        return search.name;
    }

    private static boolean advance(Search search) {
        while (search.it.hasNext()) {
            Path entry = search.it.next();
            String entryName = entry.getFileName().toString();
            search.name = entryName;
            search.isDirectory = !Files.isSymbolicLink(entry) && Files.isDirectory(entry);
            if (FPattern.match(search.pattern, asciiLower(entryName))) {
                return true;
            }
        }
        return false;
    }

    private static void closeQuietly(Search search) {
        if (search.dir != null) {
            try {
                search.dir.close();
            } catch (IOException e) {
                // Ignore, like C ignoring closedir errors on the miss path.
            }
            search.dir = null;
            search.it = null;
        }
    }

    static String asciiLower(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            out.append(ch >= 'A' && ch <= 'Z' ? (char) (ch + ('a' - 'A')) : ch);
        }
        return out.toString();
    }

    /** Test helper: names in a directory (readdir order, like C). */
    static List<String> listNames(String dir) throws IOException {
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path entry : stream) {
                names.add(entry.getFileName().toString());
            }
        }
        return names;
    }

    /** Test helper for {@link LinkOption}-free symlink checks. */
    static boolean isSymlink(Path path) {
        return Files.isSymbolicLink(path);
    }
}
