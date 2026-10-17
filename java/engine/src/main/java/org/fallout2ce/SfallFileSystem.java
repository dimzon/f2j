package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of {@code src/sfall_filesystem.cc} (alias registry only).
 *
 * <p>{@code artCacheFlush()} runs after every alias mutation, matching
 * {@code src/sfall_filesystem.cc}.
 */
public final class SfallFileSystem {

    private SfallFileSystem() {
    }

    static final class Alias {
        boolean active;
        String path = "";
        String source = "";
        String normalizedPath = "";
    }

    static final List<Alias> aliases = new ArrayList<>();

    static String normalizePath(String path) {
        String value = path != null ? path : "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '/') {
                out.append('\\');
            } else if (ch >= 'A' && ch <= 'Z') {
                out.append((char) (ch - 'A' + 'a'));
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static boolean isSoundEffectsPath(String normalizedPath) {
        return normalizedPath.startsWith("sound\\sfx\\");
    }

    static int findAliasIndex(String path) {
        if (path == null || path.isEmpty()) {
            return -1;
        }
        String normalized = normalizePath(path);
        for (int index = 0; index < aliases.size(); index++) {
            Alias alias = aliases.get(index);
            if (alias.active && alias.normalizedPath.equals(normalized)) {
                return index;
            }
        }
        return -1;
    }

    public static int sfallFileSystemCopy(String path, String source) {
        if (path == null || path.isEmpty() || source == null || source.isEmpty()) {
            return -1;
        }

        int existingIndex = findAliasIndex(path);
        if (existingIndex != -1) {
            return existingIndex;
        }

        int[] fileSize = new int[1];
        if (Db.dbGetFileSize(source, fileSize) != 0) {
            return -1;
        }

        Alias alias = new Alias();
        alias.active = true;
        alias.path = path;
        alias.source = normalizePath(source);
        alias.normalizedPath = normalizePath(path);

        for (int index = 0; index < aliases.size(); index++) {
            if (!aliases.get(index).active) {
                aliases.set(index, alias);
                Art.artCacheFlush();
                return index;
            }
        }

        aliases.add(alias);
        Art.artCacheFlush();
        return aliases.size() - 1;
    }

    public static int sfallFileSystemFind(String path) {
        return findAliasIndex(path);
    }

    public static void sfallFileSystemDelete(int id) {
        if (id < 0 || id >= aliases.size() || !aliases.get(id).active) {
            return;
        }
        Alias alias = aliases.get(id);
        alias.active = false;
        alias.path = "";
        alias.source = "";
        alias.normalizedPath = "";
        Art.artCacheFlush();
    }

    public static void sfallFileSystemReset() {
        if (aliases.isEmpty()) {
            return;
        }
        aliases.clear();
        Art.artCacheFlush();
    }

    public static String sfallFileSystemResolveAlias(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String normalized = normalizePath(path);
        if (isSoundEffectsPath(normalized)) {
            return null;
        }
        int aliasIndex = findAliasIndex(path);
        if (aliasIndex == -1 || aliases.get(aliasIndex).source.equals(path)) {
            return null;
        }
        return aliases.get(aliasIndex).source;
    }

    /** Test seam: registry size. */
    static int aliasCount() {
        return aliases.size();
    }
}
