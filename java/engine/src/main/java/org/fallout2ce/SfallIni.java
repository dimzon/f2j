package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Port of {@code src/sfall_ini.h} / {@code src/sfall_ini.cc} (sfall
 * {@code get_ini_setting} / {@code set_ini_setting} triplet slice).
 *
 * <p>Vectors: {@code [sfallini]} in
 * {@code docker/refharness/golden/v85.txt} (2 lines appended to v84),
 * produced by {@code sectionSfallIni} in
 * {@code docker/refharness/ref_main.cc}:
 * {@code crit=1:5 books=1:[bks.ini] miss=0:-999 bad=0} and
 * {@code set=1 after=7 cleared=7}. The harness reads the
 * {@code /tmp/ddraw.ini} fixture written by its own {@code [cfgmig]}
 * section; the Java test owns an identical fixture in its
 * {@code @TempDir} instead.
 *
 * <p>NOTES / mapping decisions (all documented at the use site too):
 * <ul>
 * <li>Placement: new {@code SfallIni.java}. {@code SfallConfig.java}
 * ports {@code sfall_config.cc} (the global {@code ddraw.ini} snapshot);
 * this ports the separate {@code sfall_ini.cc} triplet module. Config
 * parsing itself is reused ({@link Config}), not duplicated.</li>
 * <li>Triplet caps: {@code file|section|key} split on the first two
 * {@code '|'}; file part {@code > 63} or section part {@code > 32}
 * rejects (C compares against {@code kFileNameMaxSize}/{@code
 * kSectionMaxSize} with {@code >}). The key is everything after the
 * second {@code '|'} (may be empty, may contain {@code '|'}), like C.
 * (C copies into 63/32-byte stack buffers, so a 63/32-length chunk
 * overflows by its NUL there; Java enforces the same accept/reject
 * boundary without the overflow.)</li>
 * <li>Prefix strip: leading {@code "./"} or {@code ".\"} then one
 * leading {@code '/'} or {@code '\'} is stripped before the system-name
 * compare and before building relative paths, exactly like
 * {@code sfall_strip_ini_path_prefix}.</li>
 * <li>Special names: {@code ddraw.ini} / {@code f2_res.ini}
 * (case-insensitive) resolve with no base path — C passes the stripped
 * name straight to {@code fopen}, i.e. CWD-relative. Java defaults to
 * the same CWD-mirror (the bare relative path through {@link Config});
 * tests redirect CWD-relative lookups via the
 * {@link #searchDirectoryForTests} seam (we must not chdir the JVM).</li>
 * <li>Cache keys: system names key on the stripped name as-is,
 * non-system on {@code ".\\"} + stripped (case preserved in both),
 * like {@code sfall_normalize_ini_cache_key}. The file cache maps that
 * key to the parsed {@link Config.Cfg}; a {@code null} value is the
 * negative entry (unreadable file, not retried), like C's
 * {@code nullptr} cache value.</li>
 * <li>Missing semantics: {@code get_string} returns {@code true} with
 * an empty out value when the key is absent (only a triplet parse
 * failure returns {@code false}); {@code get_int} returns
 * {@code false} and leaves the out-param untouched when the key is
 * absent (the {@code miss=0:-999} vector relies on this). Truncation
 * into the C caller buffers (20/64/256 chars) has no Java counterpart;
 * values are returned whole.</li>
 * <li>Int parse: C runs {@code atol} (decimal prefix, leading-space
 * skip, stops at first non-digit, empty/non-numeric to 0) over its
 * 20-char buffer. Java reuses {@link StringParsers#cAtoi} (same
 * decimal-prefix semantics) over the whole value.</li>
 * <li>Set persistence: {@code set} erases the file's cache entry (so
 * the next get re-reads), loads the current file (base path first for
 * non-system names, then the CWD-relative path), applies the key, and
 * writes back with {@code RETAIN_ALL} when the file was loaded or
 * {@code DEFAULT} when it is created — then returns the write rc, like
 * C. The {@code cleared=7} vector proves the write hit the file: a
 * later get after {@code cacheClear} still sees 7.</li>
 * <li>Base path: one trailing {@code '/'} or {@code '\'} is chopped on
 * set ({@code null} clears), like {@code sfall_ini_set_base_path}.</li>
 * <li>Not ported: the array metarules ({@code get_ini_section},
 * {@code get_ini_sections}, {@code get_ini_config}) and the script
 * opcodes ({@code set_ini_setting}, {@code get_ini_setting},
 * {@code get_ini_string}) — they need the interpreter/array runtime;
 * they land with the first opcode call site.</li>
 * <li>Reset: {@link #resetForTests} clears the cache, the base path,
 * and the test search directory (fresh-process mirror).</li>
 * </ul>
 */
public final class SfallIni {

    private SfallIni() {
    }

    /** Port of {@code kFileNameMaxSize} (triplet file chunk accept limit). */
    static final int FILE_NAME_MAX_SIZE = 63;

    /** Port of {@code kSectionMaxSize} (triplet section chunk accept limit). */
    static final int SECTION_MAX_SIZE = 32;

    /** Port of {@code kSystemConfigFileNames} (no-base-path names). */
    static final String[] SYSTEM_CONFIG_FILE_NAMES = { "ddraw.ini", "f2_res.ini" };

    /** Port of {@code basePath} (C BSS empty). */
    private static String basePath = "";

    /**
     * Test seam redirecting CWD-relative ini lookups (system files and
     * the non-system fallback) into a chosen directory. {@code null}
     * (default) is the pure C behavior: bare relative paths resolved by
     * the process CWD.
     */
    private static String searchDirectoryForTests;

    /**
     * Port of {@code iniConfigCache}: normalized file name to parsed
     * config; a {@code null} value is the negative entry (unreadable
     * file, like C's cached {@code nullptr}). The config-array caches
     * ({@code iniConfigArrayCache[Dat]}) are not ported (see class
     * notes); only this cache is cleared by {@link #sfallIniCacheClear}.
     */
    private static final Map<String, Config.Cfg> iniConfigCache = new HashMap<>();

    /** Test seam: point CWD-relative lookups at a scratch dir (null restores CWD). */
    static void setSearchDirectoryForTests(String directory) {
        searchDirectoryForTests = directory;
    }

    /** Fresh-process mirror: empty cache, empty base path, no search dir. */
    static void resetForTests() {
        iniConfigCache.clear();
        basePath = "";
        searchDirectoryForTests = null;
    }

    // Port of sfall_strip_ini_path_prefix: one "./" (or ".\") then one
    // '/' (or '\').
    private static String stripIniPathPrefix(String iniFileName) {
        int from = 0;
        if (iniFileName.length() - from >= 2 && iniFileName.charAt(from) == '.'
                && (iniFileName.charAt(from + 1) == '\\' || iniFileName.charAt(from + 1) == '/')) {
            from += 2;
        }
        if (from < iniFileName.length()
                && (iniFileName.charAt(from) == '\\' || iniFileName.charAt(from) == '/')) {
            from++;
        }
        return iniFileName.substring(from);
    }

    // Port of is_system_file_name (ASCII case-insensitive compare).
    private static boolean isSystemFileName(String iniFileName) {
        String stripped = stripIniPathPrefix(iniFileName);
        for (String systemFileName : SYSTEM_CONFIG_FILE_NAMES) {
            if (stripped.equalsIgnoreCase(systemFileName)) {
                return true;
            }
        }
        return false;
    }

    // Port of sfall_normalize_ini_cache_key (stripped case preserved,
    // non-system prefixed with ".\\").
    private static String normalizeIniCacheKey(String iniFileName) {
        String stripped = stripIniPathPrefix(iniFileName);
        if (isSystemFileName(iniFileName)) {
            return stripped;
        }
        return ".\\" + stripped;
    }

    // Port of sfall_build_relative_ini_path (the C-relative path handed
    // to fopen: bare stripped name for system files, ".\\" + stripped
    // otherwise).
    private static String buildRelativeIniPath(String iniFileName) {
        String stripped = stripIniPathPrefix(iniFileName);
        if (isSystemFileName(iniFileName)) {
            return stripped;
        }
        return ".\\" + stripped;
    }

    // Maps a C-CWD-relative ini path onto the real filesystem. Without
    // the test seam this is the identity (CWD-mirror, like C); with the
    // seam the lookup is rooted at the scratch directory.
    private static String resolveIoPath(String relativePath) {
        if (searchDirectoryForTests == null) {
            return relativePath;
        }
        String stripped = relativePath;
        if (stripped.startsWith(".\\") || stripped.startsWith("./")) {
            stripped = stripped.substring(2);
        }
        return searchDirectoryForTests + "/" + stripped;
    }

    // Port of parse_ini_triplet: { fileName, section, key }, or null on
    // any error (no '|', file > 63, no second '|', section > 32). The
    // key is the whole remainder (may be empty or contain '|').
    private static String[] parseIniTriplet(String triplet) {
        if (triplet == null) {
            return null;
        }
        int fileSectionSep = triplet.indexOf('|');
        if (fileSectionSep < 0) {
            return null;
        }
        if (fileSectionSep > FILE_NAME_MAX_SIZE) {
            return null;
        }
        int sectionKeySep = triplet.indexOf('|', fileSectionSep + 1);
        if (sectionKeySep < 0) {
            return null;
        }
        if (sectionKeySep - fileSectionSep - 1 > SECTION_MAX_SIZE) {
            return null;
        }
        return new String[] {
                triplet.substring(0, fileSectionSep),
                triplet.substring(fileSectionSep + 1, sectionKeySep),
                triplet.substring(sectionKeySep + 1),
        };
    }

    // Port of sfall_read_named_ini: base directory first for
    // non-system files (raw name appended, like C's "%s\%s"), then the
    // CWD-relative path.
    private static boolean readNamedIni(String iniFileName, Config.Cfg config) {
        if (!basePath.isEmpty() && !isSystemFileName(iniFileName)) {
            if (Config.configRead(config, basePath + "\\" + iniFileName, false)) {
                return true;
            }
        }
        return Config.configRead(config, resolveIoPath(buildRelativeIniPath(iniFileName)), false);
    }

    // Port of sfall_get_ini_config (parsed-Config cache with negative
    // entries; null iniFileName misses).
    private static Config.Cfg getIniConfig(String iniFileName) {
        if (iniFileName == null) {
            return null;
        }
        String cacheKey = normalizeIniCacheKey(iniFileName);
        if (iniConfigCache.containsKey(cacheKey)) {
            return iniConfigCache.get(cacheKey);
        }
        Config.Cfg config = new Config.Cfg();
        if (!Config.configInit(config) || !readNamedIni(iniFileName, config)) {
            iniConfigCache.put(cacheKey, null);
            return null;
        }
        iniConfigCache.put(cacheKey, config);
        return config;
    }

    // Port of sfall_ini_get_int: false on triplet/config/miss (out-param
    // untouched, like the miss=0:-999 vector); decimal-prefix parse of
    // the stored string otherwise.
    public static boolean sfallIniGetInt(String triplet, int[] value) {
        String[] parts = parseIniTriplet(triplet);
        if (parts == null || value == null) {
            return false;
        }
        Config.Cfg config = getIniConfig(parts[0]);
        if (config == null) {
            return false;
        }
        String[] stringValue = new String[1];
        if (!Config.configGetString(config, parts[1], parts[2], stringValue)) {
            return false;
        }
        value[0] = StringParsers.cAtoi(stringValue[0], 0);
        return true;
    }

    // Port of sfall_ini_get_string: false only on triplet failure;
    // otherwise true with the value (empty string when missing).
    public static boolean sfallIniGetString(String triplet, String[] value) {
        String[] parts = parseIniTriplet(triplet);
        if (parts == null || value == null) {
            return false;
        }
        value[0] = "";
        Config.Cfg config = getIniConfig(parts[0]);
        if (config != null) {
            String[] stringValue = new String[1];
            if (Config.configGetString(config, parts[1], parts[2], stringValue)) {
                value[0] = stringValue[0];
            }
        }
        return true;
    }

    // Port of sfall_ini_set_int (compat_itoa base 10 == toString).
    public static boolean sfallIniSetInt(String triplet, int value) {
        return sfallIniSetString(triplet, Integer.toString(value));
    }

    // Port of sfall_ini_set_string: evicts the file cache entry, reloads
    // (base path, then CWD-relative), applies the key, and writes back
    // (RETAIN_ALL over the loaded file, DEFAULT for a new one).
    public static boolean sfallIniSetString(String triplet, String value) {
        String[] parts = parseIniTriplet(triplet);
        if (parts == null || value == null) {
            return false;
        }
        String fileName = parts[0];
        iniConfigCache.remove(normalizeIniCacheKey(fileName));

        Config.Cfg config = new Config.Cfg();
        if (!Config.configInit(config)) {
            return false;
        }

        String path = null;
        boolean loaded = false;
        if (!basePath.isEmpty() && !isSystemFileName(fileName)) {
            path = basePath + "\\" + fileName;
            loaded = Config.configRead(config, path, false);
        }
        if (!loaded) {
            path = resolveIoPath(buildRelativeIniPath(fileName));
            loaded = Config.configRead(config, path, false);
        }

        Config.configSetString(config, parts[1], parts[2], value);
        return Config.configWriteEx(config, path, loaded ? Config.RETAIN_ALL : Config.DEFAULT);
    }

    // Port of sfall_ini_set_base_path (null clears; one trailing slash
    // chopped).
    public static void sfallIniSetBasePath(String path) {
        if (path == null) {
            basePath = "";
            return;
        }
        basePath = path;
        int length = basePath.length();
        if (length > 0) {
            char last = basePath.charAt(length - 1);
            if (last == '\\' || last == '/') {
                basePath = basePath.substring(0, length - 1);
            }
        }
    }

    // Port of sfall_ini_cache_clear (array caches N/A, see class notes).
    public static void sfallIniCacheClear() {
        iniConfigCache.clear();
    }
}
