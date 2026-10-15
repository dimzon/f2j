package org.fallout2ce;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Port of {@code src/config.h} / {@code src/config.cc}.
 *
 * <p>Sections and keys iterate in ASCII case-insensitive order, exactly
 * like the {@code Dictionary}org backing them in C (backed here by
 * {@code TreeMap}s with the same comparator). {@code set} removes before
 * re-adding, so a re-set key takes the new case — like C.
 */
public final class Config {

    private Config() {
    }

    public static final int MAX_LINE_LENGTH = 2048;
    public static final int INITIAL_CAPACITY = 10;

    public static final int DEFAULT = 0;
    public static final int IS_DB = 1 << 0;
    public static final int RETAIN_COMMENTS = 1 << 1;
    public static final int RETAIN_ORDER = 1 << 2;
    public static final int RETAIN_UNKNOWN = 1 << 3;
    public static final int RETAIN_ALL = RETAIN_COMMENTS | RETAIN_ORDER | RETAIN_UNKNOWN;

    /** Mirror of {@code Dictionary Config} (section name -> section). */
    public static final class Cfg {
        final TreeMap<String, TreeMap<String, String>> sections =
                new TreeMap<>(DBase::compareKeys);
        boolean initialized;
    }

    /** Last section key read (C global, init {@code "unknown"}). */
    static String lastSectionKey = "unknown";

    /** Test seam: C globals reset on process restart. */
    static void resetLastSectionKey() {
        lastSectionKey = "unknown";
    }

    /** CLI override callback ({@code onOverride} in C, may be null). */
    public interface OverrideHandler {
        void overridden(String sectionKey, String key);
    }

    // 0x42BD90
    public static boolean configInit(Cfg config) {
        if (config == null) {
            return false;
        }
        config.sections.clear();
        config.initialized = true;
        return true;
    }

    // 0x42BDBC
    public static void configFree(Cfg config) {
        if (config == null) {
            return;
        }
        config.sections.clear();
        config.initialized = false;
    }

    public static boolean configCopy(Cfg destination, Cfg source) {
        if (destination == null || source == null
                || !destination.initialized || !source.initialized) {
            return false;
        }
        assert destination != source;
        assert destination.sections.isEmpty();

        for (var sectionEntry : source.sections.entrySet()) {
            if (!configEnsureSectionExists(destination, sectionEntry.getKey())) {
                return false;
            }
            for (var keyEntry : sectionEntry.getValue().entrySet()) {
                if (!configSetString(destination, sectionEntry.getKey(), keyEntry.getKey(), keyEntry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    // 0x42BE38 ([section]key=value; section is NOT trimmed — kept quirk).
    public static boolean configParseCommandLineArguments(Cfg config, String[] argv, OverrideHandler onOverride) {
        if (config == null) {
            return false;
        }

        for (String arg : argv) {
            int open = arg.indexOf('[');
            if (open < 0) {
                continue;
            }

            String sectionKey = arg.substring(open + 1);
            int close = sectionKey.indexOf(']');
            if (close < 0) {
                continue;
            }
            sectionKey = sectionKey.substring(0, close);

            String[] kv = parseKeyValue(arg.substring(open + 1 + close + 1));
            if (kv != null) {
                if (onOverride != null) {
                    onOverride.overridden(sectionKey, kv[0]);
                }
                if (!configSetString(config, sectionKey, kv[0], kv[1])) {
                    return false;
                }
            }
        }

        return true;
    }

    // 0x42BF48
    public static boolean configGetString(Cfg config, String sectionKey, String key, String[] valuePtr) {
        if (config == null || sectionKey == null || key == null || valuePtr == null) {
            return false;
        }

        TreeMap<String, String> section = config.sections.get(sectionKey);
        if (section == null) {
            return false;
        }

        String value = section.get(key);
        if (value == null) {
            return false;
        }

        valuePtr[0] = value;
        return true;
    }

    public static boolean configGetString(Cfg config, String sectionKey, String key, String[] valuePtr,
            String defaultValue) {
        if (config == null || sectionKey == null || key == null || valuePtr == null) {
            return false;
        }
        String[] value = new String[1];
        if (!configGetString(config, sectionKey, key, value) || value[0].isEmpty()) {
            value[0] = defaultValue;
        }
        valuePtr[0] = value[0];
        return true;
    }

    // 0x42BF90 (re-set refreshes the key case, like C remove+re-add).
    public static boolean configSetString(Cfg config, String sectionKey, String key, String value) {
        if (config == null || sectionKey == null || key == null || value == null) {
            return false;
        }

        if (!configEnsureSectionExists(config, sectionKey)) {
            return false;
        }

        TreeMap<String, String> section = config.sections.get(sectionKey);
        section.remove(key);
        section.put(key, value);
        return true;
    }

    public static boolean configRemoveKey(Cfg config, String sectionKey, String key) {
        if (config == null || sectionKey == null || key == null) {
            return false;
        }

        TreeMap<String, String> section = config.sections.get(sectionKey);
        if (section == null) {
            return true;
        }

        if (!section.containsKey(key)) {
            return true;
        }

        section.remove(key);
        return true;
    }

    // 0x42C05C
    public static boolean configGetInt(Cfg config, String sectionKey, String key, int[] valuePtr) {
        return configGetIntImpl(config, sectionKey, key, valuePtr, 0);
    }

    static boolean configGetIntImpl(Cfg config, String sectionKey, String key, int[] valuePtr, int base) {
        if (valuePtr == null) {
            return false;
        }

        String[] stringValue = new String[1];
        if (!configGetString(config, sectionKey, key, stringValue)) {
            return false;
        }

        // NOTE: errors ignored for atoi-compat (e.g. "38%"), like C.
        valuePtr[0] = (int) strtolPrefix(stringValue[0], base);

        return true;
    }

    public static boolean configGetInt(Cfg config, String sectionKey, String key, int[] valuePtr, int defaultValue) {
        return configGetIntBase(config, sectionKey, key, valuePtr, defaultValue, 0);
    }

    public static boolean configGetIntBase(Cfg config, String sectionKey, String key, int[] valuePtr,
            int defaultValue, int base) {
        if (config == null || sectionKey == null || key == null || valuePtr == null) {
            return false;
        }
        if (!configGetIntImpl(config, sectionKey, key, valuePtr, base)) {
            valuePtr[0] = defaultValue;
        }
        return true;
    }

    // 0x42C090 (atoi words, SFALL tail fix included).
    public static boolean configGetIntList(Cfg config, String sectionKey, String key, int[] arr, int count) {
        if (arr == null || count < 2) {
            return false;
        }

        String[] string = new String[1];
        if (!configGetString(config, sectionKey, key, string)) {
            return false;
        }

        // NOTE: C strncpy's into a 2048 buffer (truncation edge ignored).
        String text = string[0].length() > MAX_LINE_LENGTH - 1
                ? string[0].substring(0, MAX_LINE_LENGTH - 1)
                : string[0];

        int at = 0;
        int out = 0;
        int remaining = count;
        while (true) {
            int comma = text.indexOf(',', at);
            if (comma < 0) {
                break;
            }

            remaining--;
            if (remaining == 0) {
                break;
            }

            arr[out++] = StringParsers.cAtoi(text, at);
            at = comma + 1;
        }

        // SFALL: Fix getting last item in a list if the list has less than the
        // requested number of values (for `chem_primary_desire`).
        if (remaining > 0) {
            arr[out++] = StringParsers.cAtoi(text, at);
            remaining--;
        }

        return remaining == 0;
    }

    // 0x42C160
    public static boolean configSetInt(Cfg config, String sectionKey, String key, int value) {
        return configSetString(config, sectionKey, key, Integer.toString(value));
    }

    // 0x42C280
    public static boolean configRead(Cfg config, String filePath, boolean isDb) {
        if (config == null || filePath == null) {
            return false;
        }

        if (isDb) {
            XFile.Handle stream = Db.fileOpen(filePath, "rb");
            if (stream == null) {
                return false;
            }

            String line;
            while ((line = Db.fileReadString(stream, MAX_LINE_LENGTH)) != null) {
                configParseLine(config, line);
            }
            Db.fileClose(stream);
        } else {
            java.io.RandomAccessFile stream = CompatFiles.fopen(filePath, "rt");
            if (stream == null) {
                return false;
            }

            try {
                String line;
                while ((line = CompatFiles.fgets(stream, MAX_LINE_LENGTH)) != null) {
                    configParseLine(config, line);
                }
            } catch (java.io.IOException e) {
                try {
                    stream.close();
                } catch (java.io.IOException ignored) {
                    // Ignore.
                }
                return false;
            }
            try {
                stream.close();
            } catch (java.io.IOException e) {
                return false;
            }
        }

        return true;
    }

    // 0x42C324
    public static boolean configWrite(Cfg config, String filePath, boolean isDb) {
        return configWriteEx(config, filePath, isDb ? IS_DB : DEFAULT);
    }

    public static boolean configWriteEx(Cfg config, String filePath, int flags) {
        if (config == null || filePath == null) {
            return false;
        }

        if ((flags & IS_DB) != 0) {
            return configWriteDb(config, filePath);
        }

        if ((flags & (RETAIN_COMMENTS | RETAIN_ORDER | RETAIN_UNKNOWN)) != 0) {
            return configWriteSideBySide(config, filePath, flags);
        }

        return configWriteStandard(config, filePath);
    }

    static boolean configWriteDb(Cfg config, String filePath) {
        XFile.Handle stream = Db.fileOpen(filePath, "wt");
        if (stream == null) {
            return false;
        }

        for (var sectionEntry : config.sections.entrySet()) {
            Db.filePrintFormatted(stream, "[%s]\n", sectionEntry.getKey());

            for (var keyEntry : sectionEntry.getValue().entrySet()) {
                Db.filePrintFormatted(stream, "%s=%s\n", keyEntry.getKey(), keyEntry.getValue());
            }
            Db.filePrintFormatted(stream, "\n");
        }

        Db.fileClose(stream);
        return true;
    }

    static boolean configWriteStandard(Cfg config, String filePath) {
        StringBuilder out = new StringBuilder();
        for (var sectionEntry : config.sections.entrySet()) {
            writeSection(out, sectionEntry.getKey(), sectionEntry.getValue(), null);
            out.append('\n');
        }
        return writeFile(filePath, out.toString());
    }

    static void writeSection(StringBuilder out, String sectionName, TreeMap<String, String> section,
            java.util.Set<String> handledKeys) {
        if (sectionName != null) {
            out.append('[').append(sectionName).append(']').append('\n');
        }

        for (var entry : section.entrySet()) {
            if (handledKeys == null || !handledKeys.contains(entry.getKey())) {
                out.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
            }
        }
    }

    static boolean configWriteSideBySide(Cfg config, String filePath, int flags) {
        boolean retainUnknown = (flags & RETAIN_UNKNOWN) != 0;

        String originalText = readFile(filePath);
        if (originalText == null) {
            if (!java.nio.file.Files.exists(java.nio.file.Paths.get(filePath))) {
                return configWriteStandard(config, filePath);
            }
            return false;
        }

        String tempPath = filePath + ".tmp";
        if (tempPath.length() >= PlatformPaths.MAX_PATH) {
            return false;
        }

        TreeSet<String> handledSections = new TreeSet<>(DBase::compareKeys);
        TreeSet<String> handledKeys = new TreeSet<>(DBase::compareKeys);
        List<String> pendingLines = new ArrayList<>();

        StringBuilder out = new StringBuilder();
        String currentSectionName = "";
        TreeMap<String, String> currentSection = null;
        // Preamble counts as known so its comments survive.
        boolean currentSectionKnown = true;

        for (String rawLine : splitLines(originalText)) {
            String line = rawLine;
            int t = 0;
            while (t < line.length() && isSpace(line.charAt(t))) {
                t++;
            }
            String trimmed = line.substring(t);

            if (trimmed.isEmpty() || trimmed.charAt(0) == ';' || trimmed.charAt(0) == '#') {
                if (!currentSectionKnown && !retainUnknown) {
                    continue;
                }
                if ((flags & RETAIN_COMMENTS) != 0 || trimmed.isEmpty()) {
                    pendingLines.add(line);
                }
                continue;
            }

            if (trimmed.charAt(0) == '[') {
                if (currentSection != null) {
                    writeSection(out, null, currentSection, handledKeys);
                    handledKeys.clear();
                    currentSection = null;
                }

                currentSectionKnown = false;
                currentSectionName = "";

                int end = trimmed.indexOf(']');
                if (end >= 0) {
                    currentSectionName = trimString(trimmed.substring(1, end));
                    TreeMap<String, String> found = config.sections.get(currentSectionName);
                    if (found != null) {
                        currentSection = found;
                        handledSections.add(currentSectionName);
                        currentSectionKnown = true;
                    }
                }

                if (currentSectionKnown || retainUnknown) {
                    for (String l : pendingLines) {
                        out.append(l);
                    }
                    pendingLines.clear();
                    out.append(line);
                } else {
                    pendingLines.clear();
                }
                continue;
            }

            int equals = trimmed.indexOf('=');
            if (equals >= 0) {
                String key = trimString(trimmed.substring(0, equals));

                String value = null;
                if (currentSection != null) {
                    value = currentSection.get(key);
                }

                if (value != null) {
                    for (String l : pendingLines) {
                        out.append(l);
                    }
                    pendingLines.clear();

                    String k = key;
                    String comment = null;

                    if ((flags & RETAIN_COMMENTS) != 0) {
                        k = trimmed.substring(0, equals);
                        int semi = trimmed.indexOf(';', equals + 1);
                        if (semi < 0) {
                            semi = trimmed.indexOf('#', equals + 1);
                        }
                        if (semi >= 0) {
                            comment = trimmed.substring(semi);
                        }
                    }

                    out.append(k).append('=').append(value);
                    if (comment != null) {
                        out.append(' ').append(comment);
                    } else {
                        out.append('\n');
                    }
                    handledKeys.add(key);
                } else if (retainUnknown) {
                    for (String l : pendingLines) {
                        out.append(l);
                    }
                    pendingLines.clear();
                    out.append(line);
                }
                continue;
            }

            if ((currentSectionKnown || retainUnknown) && (flags & RETAIN_COMMENTS) != 0) {
                pendingLines.add(line);
            }
        }

        if (currentSection != null) {
            writeSection(out, null, currentSection, handledKeys);
        }

        for (String l : pendingLines) {
            out.append(l);
        }
        pendingLines.clear();

        boolean firstNewSection = true;
        for (var sectionEntry : config.sections.entrySet()) {
            if (!handledSections.contains(sectionEntry.getKey())) {
                if (firstNewSection) {
                    out.append('\n');
                    firstNewSection = false;
                }
                writeSection(out, sectionEntry.getKey(), sectionEntry.getValue(), null);
                out.append('\n');
            }
        }

        if (!writeFile(tempPath, out.toString())) {
            return false;
        }

        String backupPath = filePath + ".bak";
        java.nio.file.Path backup = java.nio.file.Paths.get(backupPath);
        try {
            if (java.nio.file.Files.exists(backup)) {
                CompatFiles.remove(backupPath);
            }
            if (CompatFiles.rename(filePath, backupPath) != 0) {
                CompatFiles.remove(tempPath);
                return false;
            }
            if (CompatFiles.rename(tempPath, filePath) != 0) {
                CompatFiles.rename(backupPath, filePath);
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        CompatFiles.remove(backupPath);
        return true;
    }

    static String readFile(String filePath) {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
            return new String(bytes, StandardCharsets.ISO_8859_1);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    static boolean writeFile(String filePath, String text) {
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath),
                    text.getBytes(StandardCharsets.ISO_8859_1));
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /** Split keeping line endings (fgets parity: each line holds its \n). */
    static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(text.substring(start, i + 1));
                start = i + 1;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start));
        }
        return lines;
    }

    // 0x42C4BC (comment strip, [section], key=value into last section).
    static boolean configParseLine(Cfg config, String string) {
        StringBuilder line = new StringBuilder(string);
        int semi = line.indexOf(";");
        if (semi < 0) {
            semi = line.indexOf("#");
        }
        if (semi >= 0) {
            line.setLength(semi);
        }

        int s = 0;
        while (s < line.length() && isSpace(line.charAt(s))) {
            s++;
        }

        if (s < line.length() && line.charAt(s) == '[') {
            int close = line.indexOf("]", s + 1);
            if (close >= 0) {
                lastSectionKey = trimString(line.substring(s + 1, close));
                return true;
            }
        }

        String rest = line.substring(s);
        int equals = rest.indexOf('=');
        if (equals < 0) {
            return false;
        }

        String key = trimString(rest.substring(0, equals));
        String value = trimString(rest.substring(equals + 1));
        return configSetString(config, lastSectionKey, key, value);
    }

    /** Split first '=' + trim both (null when no '='). */
    static String[] parseKeyValue(String string) {
        if (string == null) {
            return null;
        }
        int equals = string.indexOf('=');
        if (equals < 0) {
            return null;
        }
        return new String[] { trimString(string.substring(0, equals)), trimString(string.substring(equals + 1)) };
    }

    // 0x42C638
    static boolean configEnsureSectionExists(Cfg config, String sectionKey) {
        if (config == null || sectionKey == null) {
            return false;
        }
        config.sections.putIfAbsent(sectionKey, new TreeMap<>(DBase::compareKeys));
        return true;
    }

    // 0x42C698 trim both ends (C locale spaces).
    static String trimString(String string) {
        int start = 0;
        int end = string.length();
        while (start < end && isSpace(string.charAt(start))) {
            start++;
        }
        while (end > start && isSpace(string.charAt(end - 1))) {
            end--;
        }
        return string.substring(start, end);
    }

    static boolean isSpace(char ch) {
        return StringParsers.isSpace(ch);
    }

    /** strtol with base detection (0 = auto) and C clamping; errors ignored. */
    static long strtolPrefix(String string, int base) {
        int n = string.length();
        int i = 0;
        while (i < n && isSpace(string.charAt(i))) {
            i++;
        }
        boolean negative = false;
        if (i < n && (string.charAt(i) == '+' || string.charAt(i) == '-')) {
            negative = string.charAt(i) == '-';
            i++;
        }
        int radix = base;
        if (radix == 0) {
            if (i < n && string.charAt(i) == '0') {
                if (i + 1 < n && (string.charAt(i + 1) == 'x' || string.charAt(i + 1) == 'X')) {
                    radix = 16;
                    i += 2;
                } else {
                    radix = 8;
                    i++;
                }
            } else {
                radix = 10;
            }
        } else if (radix == 16) {
            if (i + 1 < n && string.charAt(i) == '0'
                    && (string.charAt(i + 1) == 'x' || string.charAt(i + 1) == 'X')) {
                i += 2;
            }
        }

        long value = 0;
        boolean any = false;
        while (i < n) {
            int digit = Character.digit(string.charAt(i), radix);
            if (digit < 0) {
                break;
            }
            any = true;
            if (value > (Long.MAX_VALUE - digit) / radix) {
                return negative ? Long.MIN_VALUE : Long.MAX_VALUE;
            }
            value = value * radix + digit;
            i++;
        }
        if (!any) {
            return 0;
        }
        return negative ? -value : value;
    }

    // 0x42C718 (strtod prefix; hex floats unsupported — see NOTE).
    public static boolean configGetDouble(Cfg config, String sectionKey, String key, double[] valuePtr) {
        if (valuePtr == null) {
            return false;
        }

        String[] stringValue = new String[1];
        if (!configGetString(config, sectionKey, key, stringValue)) {
            return false;
        }

        valuePtr[0] = strtodPrefix(stringValue[0]);

        return true;
    }

    // NOTE: C strtod also parses hex floats; configs only carry %f text.
    static double strtodPrefix(String string) {
        int n = string.length();
        int i = 0;
        while (i < n && isSpace(string.charAt(i))) {
            i++;
        }
        boolean negative = false;
        int start = i;
        if (i < n && (string.charAt(i) == '+' || string.charAt(i) == '-')) {
            negative = string.charAt(i) == '-';
            i++;
        }
        String rest = string.substring(i).toLowerCase(Locale.ROOT);
        if (rest.startsWith("infinity") || rest.startsWith("inf")) {
            return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        if (rest.startsWith("nan")) {
            return Double.NaN;
        }

        int j = i;
        boolean digits = false;
        while (j < n && Character.isDigit(string.charAt(j))) {
            digits = true;
            j++;
        }
        if (j < n && string.charAt(j) == '.') {
            j++;
            while (j < n && Character.isDigit(string.charAt(j))) {
                digits = true;
                j++;
            }
        }
        if (!digits) {
            return 0.0;
        }
        if (j < n && (string.charAt(j) == 'e' || string.charAt(j) == 'E')) {
            int k = j + 1;
            if (k < n && (string.charAt(k) == '+' || string.charAt(k) == '-')) {
                k++;
            }
            int expStart = k;
            while (k < n && Character.isDigit(string.charAt(k))) {
                k++;
            }
            if (k > expStart) {
                j = k;
            }
        }

        try {
            return Double.parseDouble(string.substring(start, j));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // 0x42C74C
    public static boolean configSetDouble(Cfg config, String sectionKey, String key, double value) {
        return configSetString(config, sectionKey, key,
                String.format(Locale.ROOT, "%.6f", value));
    }

    public static boolean configGetBool(Cfg config, String sectionKey, String key, boolean[] valuePtr) {
        if (valuePtr == null) {
            return false;
        }

        int[] integerValue = new int[1];
        if (!configGetInt(config, sectionKey, key, integerValue)) {
            return false;
        }

        valuePtr[0] = integerValue[0] != 0;

        return true;
    }

    public static boolean configGetBool(Cfg config, String sectionKey, String key, boolean[] valuePtr,
            boolean defaultValue) {
        if (config == null || sectionKey == null || key == null || valuePtr == null) {
            return false;
        }
        if (!configGetBool(config, sectionKey, key, valuePtr)) {
            valuePtr[0] = defaultValue;
        }
        return true;
    }

    public static boolean configSetBool(Cfg config, String sectionKey, String key, boolean value) {
        return configSetInt(config, sectionKey, key, value ? 1 : 0);
    }

    /** RAII mirror of {@code ScopedConfig} (use try-with-resources). */
    public static final class ScopedConfig implements AutoCloseable {
        private final Cfg config = new Cfg();
        private boolean loaded;

        public ScopedConfig() {
            loaded = configInit(config);
        }

        public ScopedConfig(String filePath, boolean isDb) {
            this();
            if (loaded) {
                loaded = configRead(config, filePath, isDb);
            }
        }

        public Cfg get() {
            return config;
        }

        public boolean isLoaded() {
            return loaded;
        }

        @Override
        public void close() {
            if (config.initialized) {
                configFree(config);
            }
            loaded = false;
        }
    }
}
