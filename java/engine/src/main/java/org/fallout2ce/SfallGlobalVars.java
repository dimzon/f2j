package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Sfall global-vars slice, mirroring {@code src/sfall_global_vars.cc} /
 * {@code src/sfall_global_vars.h}: {@code sfall_gl_vars_init},
 * {@code sfall_gl_vars_reset}, {@code sfall_gl_vars_exit},
 * {@code sfall_gl_vars_save}, {@code sfall_gl_vars_load},
 * {@code sfall_gl_vars_store} (int + 8-char-string overloads) and
 * {@code sfall_gl_vars_fetch} (int + 8-char-string overloads) over the
 * static {@code sfall_gl_vars_state} (BSS {@code nullptr}).
 *
 * <p>Vectors: {@code [sfallvars]} in
 * {@code docker/refharness/golden/v84.txt} (2 lines appended to v83),
 * produced by {@code sectionSfallVars} in
 * {@code docker/refharness/ref_main.cc}:
 * {@code init=1 store=1 fetch=1:42 miss=0 str=1:99} and
 * {@code save=1 cleared=0 load=1 back=1:42 backstr=99}.
 *
 * <p>NOTES / mapping decisions (all documented at the use site too):
 * <ul>
 * <li>u64 LE compose: C reinterprets the 8 key bytes as a
 * {@code uint64_t} ({@code *(reinterpret_cast<const uint64_t*>(key))}),
 * which is little-endian dependent (correct only on x86/x64). Java has
 * no reinterpret cast, so {@link #stringKeyToLong} builds the key
 * explicitly LE ({@code b0 | b1<<8 | ... | b7<<56}). Byte-identical to
 * C on LE hosts by construction.</li>
 * <li>String keys: C gates on {@code strlen(key) != 8}. Java gates on
 * {@code key.length() != 8} plus all-ASCII ({@code char <= 0x7F}); a
 * non-ASCII char has no single-byte C counterpart, so {@code false} is
 * the closest safe mirror. A {@code null} key is also {@code false}.</li>
 * <li>Entry layout/widths: C writes the raw struct
 * ({@code GlobalVarEntry}: u64 key + i32 value + i32 unused = 16 bytes)
 * plus the raw {@code int} count via untyped {@code fileWrite} /
 * {@code fileRead} (native = LE bytes on x86) — NOT the big-endian
 * {@code fileWriteInt32} family. Java therefore bypasses
 * {@link Db#fileWriteInt32} and moves raw LE byte arrays through
 * {@link Db#fileWrite} / {@link Db#fileRead}: i32 count as 4 LE bytes,
 * then per entry key as 8 LE bytes, value as 4 LE bytes, unused as 4
 * zero LE bytes. Same widths, same order; Java reads back its own
 * writes.</li>
 * <li>Erase-on-zero: a store to a PRESENT key with {@code value == 0}
 * erases the entry; a store to an ABSENT key always inserts (even with
 * {@code value == 0}), exactly like the static {@code sfall_gl_vars_store}
 * body.</li>
 * <li>Miss-untouched: a fetch miss returns {@code false} and leaves the
 * out-param ({@code int[0]}) untouched — the vectors rely on this
 * ({@code missVal} stays {@code -999}; only the rc is printed).</li>
 * <li>Null-state guards: every C body except init/exit dereferences the
 * state pointer (reset/store/fetch/save/load on a null state segfaults;
 * exit null-checks). Vectors always init first. Java mirrors init
 * (creates) / exit (null-safe clear to null) and throws
 * {@code IllegalStateException} on any other call while uninit,
 * following the repo convention ({@link Nevs}, {@link TextFont}).</li>
 * <li>Load with {@code count <= 0} reads nothing and returns
 * {@code true}, mirroring C's {@code while (count > 0)} skip.</li>
 * <li>Reset: {@link #resetForTests} restores the fresh-process mirror
 * (state null, matching the C BSS {@code nullptr}).</li>
 * </ul>
 */
public final class SfallGlobalVars {

    private SfallGlobalVars() {
    }

    /** Port of {@code SfallGlobalVarsState} ({@code vars} map only). Null until init (C BSS nullptr). */
    private static Map<Long, Integer> vars;

    /** Fresh-process mirror: state null (C BSS {@code nullptr}). */
    static void resetForTests() {
        vars = null;
    }

    private static void requireInit() {
        if (vars == null) {
            // C segfaults (null-state deref); Java: explicit init + IllegalStateException (Nevs convention).
            throw new IllegalStateException("sfall_gl_vars_init() not called");
        }
    }

    /**
     * Port of the 8-char-string to u64 key fold
     * ({@code *(reinterpret_cast<const uint64_t*>(key))}).
     * Little-endian dependent by design (matches x86/x64 C); byte 0 is
     * the least significant.
     */
    static long stringKeyToLong(String key) {
        long numericKey = 0;
        for (int i = 0; i < 8; i++) {
            numericKey |= ((long) (key.charAt(i) & 0xFF)) << (i * 8);
        }
        return numericKey;
    }

    private static boolean isStringKey(String key) {
        if (key == null || key.length() != 8) {
            return false;
        }
        for (int i = 0; i < 8; i++) {
            if (key.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    // sfall_gl_vars_init.
    public static boolean sfallGlVarsInit() {
        try {
            vars = new HashMap<>();
        } catch (OutOfMemoryError e) {
            vars = null;
            return false;
        }
        return true;
    }

    // sfall_gl_vars_reset (C derefs unconditionally; Java guards, see class notes).
    public static void sfallGlVarsReset() {
        requireInit();
        vars.clear();
    }

    // sfall_gl_vars_exit (null-safe, like C).
    public static void sfallGlVarsExit() {
        vars = null;
    }

    private static boolean writeRaw(XFile.Handle stream, byte[] buf) {
        return Db.fileWrite(buf, 0, buf.length, 1, stream) == 1;
    }

    private static boolean readRaw(XFile.Handle stream, byte[] buf) {
        return Db.fileRead(buf, 0, buf.length, 1, stream) == 1;
    }

    private static void putInt32LE(byte[] buf, int offset, int value) {
        buf[offset] = (byte) value;
        buf[offset + 1] = (byte) (value >> 8);
        buf[offset + 2] = (byte) (value >> 16);
        buf[offset + 3] = (byte) (value >> 24);
    }

    private static int getInt32LE(byte[] buf, int offset) {
        return (buf[offset] & 0xFF)
                | ((buf[offset + 1] & 0xFF) << 8)
                | ((buf[offset + 2] & 0xFF) << 16)
                | ((buf[offset + 3] & 0xFF) << 24);
    }

    private static void putInt64LE(byte[] buf, int offset, long value) {
        for (int i = 0; i < 8; i++) {
            buf[offset + i] = (byte) (value >> (i * 8));
        }
    }

    private static long getInt64LE(byte[] buf, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (buf[offset + i] & 0xFF)) << (i * 8);
        }
        return value;
    }

    // sfall_gl_vars_save: raw i32 LE count + raw 16-byte LE entries
    // (u64 key, i32 value, i32 zero unused), like C's untyped fileWrite.
    public static boolean sfallGlVarsSave(XFile.Handle stream) {
        requireInit();
        byte[] countBuf = new byte[4];
        putInt32LE(countBuf, 0, vars.size());
        if (!writeRaw(stream, countBuf)) {
            return false;
        }

        byte[] entryBuf = new byte[16];
        for (Map.Entry<Long, Integer> pair : vars.entrySet()) {
            putInt64LE(entryBuf, 0, pair.getKey());
            putInt32LE(entryBuf, 8, pair.getValue());
            // C zero-initialises the entry (`= { 0 }`), so unused stays 0.
            putInt32LE(entryBuf, 12, 0);
            if (!writeRaw(stream, entryBuf)) {
                return false;
            }
        }

        return true;
    }

    // sfall_gl_vars_load: raw i32 LE count + raw 16-byte LE entries
    // (unused slot ignored), like C's untyped fileRead.
    public static boolean sfallGlVarsLoad(XFile.Handle stream) {
        requireInit();
        byte[] countBuf = new byte[4];
        if (!readRaw(stream, countBuf)) {
            return false;
        }

        int count = getInt32LE(countBuf, 0);

        byte[] entryBuf = new byte[16];
        while (count > 0) {
            if (!readRaw(stream, entryBuf)) {
                return false;
            }

            vars.put(getInt64LE(entryBuf, 0), getInt32LE(entryBuf, 8));

            count--;
        }

        return true;
    }

    // sfall_gl_vars_store(const char* key, int value).
    public static boolean sfallGlVarsStore(String key, int value) {
        requireInit();
        if (!isStringKey(key)) {
            return false;
        }
        return sfallGlVarsStore(stringKeyToLong(key), value);
    }

    // sfall_gl_vars_store(int key, int value).
    public static boolean sfallGlVarsStore(int key, int value) {
        requireInit();
        return sfallGlVarsStore(Integer.toUnsignedLong(key), value);
    }

    // sfall_gl_vars_fetch(const char* key, int& value) (miss leaves value[0] untouched).
    public static boolean sfallGlVarsFetch(String key, int[] value) {
        requireInit();
        if (!isStringKey(key)) {
            return false;
        }
        return sfallGlVarsFetch(stringKeyToLong(key), value);
    }

    // sfall_gl_vars_fetch(int key, int& value) (miss leaves value[0] untouched).
    public static boolean sfallGlVarsFetch(int key, int[] value) {
        requireInit();
        return sfallGlVarsFetch(Integer.toUnsignedLong(key), value);
    }

    // static sfall_gl_vars_store(uint64_t key, int value): present + 0
    // erases; absent always inserts (even 0).
    private static boolean sfallGlVarsStore(long key, int value) {
        if (!vars.containsKey(key)) {
            vars.put(key, value);
        } else {
            if (value == 0) {
                vars.remove(key);
            } else {
                vars.put(key, value);
            }
        }
        return true;
    }

    // static sfall_gl_vars_fetch(uint64_t key, int& value): miss returns
    // false with value[0] untouched.
    private static boolean sfallGlVarsFetch(long key, int[] value) {
        Integer found = vars.get(key);
        if (found == null) {
            return false;
        }
        value[0] = found;
        return true;
    }
}
