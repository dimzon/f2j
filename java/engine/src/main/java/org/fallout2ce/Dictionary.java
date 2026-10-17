package org.fallout2ce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Port of {@code src/dictionary.h} / {@code src/dictionary.cc}.
 *
 * <p>Entries stay sorted by key, compared ASCII case-insensitively
 * ({@code compat_stricmp} = {@code SDL_strcasecmp}). Values are raw
 * {@code valueSize}-byte copies, like C {@code memcpy} — callers own the
 * interpretation. The lookup loop is the original walk (bounds step by 1,
 * not a true bisection); it is O(n) in C too.
 *
 * <p>Serialization mirrors the C byte format exactly (big-endian ints,
 * 1-byte key length): a fixture written by the C harness
 * ({@code src/test/resources/fixtures/dict-v1.bin}) is loaded in tests.
 */
public final class Dictionary {

    private Dictionary() {
    }

    // 0xFEBAFEBA type marker (see NOTE in dictionary.cc).
    public static final int MARKER = 0xFEBAFEBA;

    /** Mirror of {@code DictionaryEntry} (key keeps original case). */
    public static final class Entry {
        public String key;
        public byte[] value; // nullable, exactly valueSize bytes when non-null
    }

    /** Mirror of {@code struct Dictionary}. */
    public static final class Dict {
        public int marker;
        public int entriesLength;
        public int entriesCapacity;
        public int valueSize;
        public DictionaryIO io;
        public final ArrayList<Entry> entries = new ArrayList<>();
    }

    /** Mirror of {@code DictionaryIO} (may be null = raw byte copy). */
    public static final class DictionaryIO {
        public ReadProc readProc;
        public WriteProc writeProc;
    }

    /** Mirror of {@code DictionaryReadProc}. Returns 0 on success. */
    public interface ReadProc {
        int read(DataInput in, byte[] buffer, int size, int a3) throws IOException;
    }

    /** Mirror of {@code DictionaryWriteProc}. Returns 0 on success. */
    public interface WriteProc {
        int write(DataOutput out, byte[] buffer, int size, int a3) throws IOException;
    }

    private static boolean isInitialized(Dict dictionary) {
        return dictionary.marker == MARKER;
    }

    // ASCII case-insensitive compare (SDL_strcasecmp semantics).
    static int compareKeys(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            int ca = Character.toLowerCase(a.charAt(i));
            int cb = Character.toLowerCase(b.charAt(i));
            if (ca != cb) {
                return ca - cb;
            }
        }
        return a.length() - b.length();
    }

    // 0x4D9BA8 assoc_init
    public static int dictionaryInit(Dict dictionary, int initialCapacity, int valueSize, DictionaryIO io) {
        dictionary.entriesCapacity = initialCapacity;
        dictionary.valueSize = valueSize;
        dictionary.entriesLength = 0;
        dictionary.entries.clear();

        if (io != null) {
            dictionary.io = new DictionaryIO();
            dictionary.io.readProc = io.readProc;
            dictionary.io.writeProc = io.writeProc;
        } else {
            dictionary.io = new DictionaryIO();
        }

        dictionary.entries.ensureCapacity(initialCapacity);
        dictionary.marker = MARKER;
        return 0;
    }

    // 0x4D9C0C assoc_resize
    public static int dictionarySetCapacity(Dict dictionary, int newCapacity) {
        if (!isInitialized(dictionary)) {
            return -1;
        }

        if (newCapacity < dictionary.entriesLength) {
            return -1;
        }

        dictionary.entriesCapacity = newCapacity;
        dictionary.entries.ensureCapacity(newCapacity);
        return 0;
    }

    // 0x4D9C48 assoc_free
    public static int dictionaryFree(Dict dictionary) {
        if (!isInitialized(dictionary)) {
            return -1;
        }

        dictionary.entries.clear();
        dictionary.marker = 0;
        dictionary.entriesLength = 0;
        dictionary.entriesCapacity = 0;
        dictionary.valueSize = 0;
        dictionary.io = null;
        return 0;
    }

    /**
     * 0x4D9CC4 assoc_find. Returns 0 + index when found, else -1 with the
     * insertion point in {@code indexPtr[0]}.
     */
    static int findIndexForKey(Dict dictionary, String key, int[] indexPtr) {
        if (!isInitialized(dictionary)) {
            return -1;
        }

        if (dictionary.entriesLength == 0) {
            indexPtr[0] = 0;
            return -1;
        }

        int r = dictionary.entriesLength - 1;
        int l = 0;
        int mid = 0;
        int cmp = 0;
        while (r >= l) {
            mid = (l + r) / 2;

            cmp = compareKeys(key, dictionary.entries.get(mid).key);
            if (cmp == 0) {
                break;
            }

            if (cmp > 0) {
                l = l + 1;
            } else {
                r = r - 1;
            }
        }

        if (cmp == 0) {
            indexPtr[0] = mid;
            return 0;
        }

        if (cmp < 0) {
            indexPtr[0] = mid;
        } else {
            indexPtr[0] = mid + 1;
        }

        return -1;
    }

    // 0x4D9D5C assoc_search
    public static int dictionaryGetIndexByKey(Dict dictionary, String key) {
        if (!isInitialized(dictionary)) {
            return -1;
        }

        int[] index = new int[1];
        if (findIndexForKey(dictionary, key, index) != 0) {
            return -1;
        }

        return index[0];
    }

    // 0x4D9D88 assoc_insert
    public static int dictionaryAddValue(Dict dictionary, String key, byte[] value) {
        if (!isInitialized(dictionary)) {
            return -1;
        }

        int[] at = new int[1];
        if (findIndexForKey(dictionary, key, at) == 0) {
            // Element for this key already exists.
            return -1;
        }
        int newElementIndex = at[0];

        if (dictionary.entriesLength == dictionary.entriesCapacity) {
            if (dictionarySetCapacity(dictionary, 2 * (dictionary.entriesCapacity + 1)) == -1) {
                return -1;
            }
        }

        Entry entry = new Entry();
        entry.key = key;
        if (value != null && dictionary.valueSize != 0) {
            // NOTE: C memcpy's valueSize bytes unconditionally (caller must
            // provide enough); bounded here with an explicit error.
            if (value.length < dictionary.valueSize) {
                throw new IllegalArgumentException(
                        "value too short: " + value.length + " < valueSize " + dictionary.valueSize);
            }
            entry.value = new byte[dictionary.valueSize];
            System.arraycopy(value, 0, entry.value, 0, dictionary.valueSize);
        }

        dictionary.entries.add(newElementIndex, entry);
        dictionary.entriesLength++;
        return 0;
    }

    // 0x4D9EE8 assoc_delete
    public static int dictionaryRemoveValue(Dict dictionary, String key) {
        if (!isInitialized(dictionary)) {
            return -1;
        }

        int[] at = new int[1];
        if (findIndexForKey(dictionary, key, at) == -1) {
            return -1;
        }

        dictionary.entries.remove(at[0]);
        dictionary.entriesLength--;
        return 0;
    }

    // NOTE: Unused. 0x4D9F84 assoc_copy.
    // NOTE: keeps the C FIXME — init failure returns 0, not -1.
    public static int dictionaryCopy(Dict dest, Dict src) {
        if (!isInitialized(src)) {
            return -1;
        }

        if (dictionaryInit(dest, src.entriesCapacity, src.valueSize, src.io) != 0) {
            // NOTE: mirrors the upstream quirk - returns 0 on init failure.
            return 0;
        }

        for (int index = 0; index < src.entriesLength; index++) {
            Entry entry = src.entries.get(index);
            if (dictionaryAddValue(dest, entry.key, entry.value) == -1) {
                return -1;
            }
        }

        return 0;
    }

    // 0x4DA090 assoc_read_long (big-endian; -1 on short read).
    public static int dictionaryReadInt(DataInput in, int[] valuePtr) throws IOException {
        try {
            int b0 = in.readUnsignedByte();
            int b1 = in.readUnsignedByte();
            int b2 = in.readUnsignedByte();
            int b3 = in.readUnsignedByte();
            valuePtr[0] = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
            return 0;
        } catch (EOFException e) {
            return -1;
        }
    }

    // 0x4DA0F4 assoc_read_assoc_array
    public static int dictionaryReadHeader(DataInput in, Dict dictionary) throws IOException {
        int[] value = new int[1];

        if (dictionaryReadInt(in, value) != 0) {
            return -1;
        }
        dictionary.entriesLength = value[0];

        if (dictionaryReadInt(in, value) != 0) {
            return -1;
        }
        dictionary.entriesCapacity = value[0];

        if (dictionaryReadInt(in, value) != 0) {
            return -1;
        }
        dictionary.valueSize = value[0];

        // NOTE: Originally reads `values` pointer.
        if (dictionaryReadInt(in, value) != 0) {
            return -1;
        }

        return 0;
    }

    // NOTE: Unused. 0x4DA158 assoc_load.
    public static int dictionaryLoad(DataInput in, Dict dictionary, int a3) throws IOException {
        if (!isInitialized(dictionary)) {
            return -1;
        }

        dictionary.entries.clear();

        if (dictionaryReadHeader(in, dictionary) != 0) {
            return -1;
        }

        if (dictionary.entriesCapacity <= 0) {
            dictionary.entriesLength = 0;
            return 0;
        }

        dictionary.entries.ensureCapacity(dictionary.entriesCapacity);

        if (dictionary.entriesLength <= 0) {
            dictionary.entriesLength = 0;
            return 0;
        }

        // NOTE: C trusts the header and reads entriesLength entries; a
        // corrupt stream is detected through read failures below.
        List<Entry> loaded = new ArrayList<>(dictionary.entriesLength);
        for (int index = 0; index < dictionary.entriesLength; index++) {
            Entry entry = new Entry();
            int keyLength;
            try {
                keyLength = in.readUnsignedByte();
            } catch (EOFException e) {
                return -1;
            }

            byte[] keyBytes = new byte[keyLength];
            try {
                in.readFully(keyBytes);
            } catch (EOFException e) {
                return -1;
            }
            // NOTE: C uses fgets (stops at newline); keys with newlines are
            // not representable in this format.
            entry.key = new String(keyBytes, java.nio.charset.StandardCharsets.ISO_8859_1);

            if (dictionary.valueSize != 0) {
                entry.value = new byte[dictionary.valueSize];
                if (dictionary.io != null && dictionary.io.readProc != null) {
                    if (dictionary.io.readProc.read(in, entry.value, dictionary.valueSize, a3) != 0) {
                        return -1;
                    }
                } else {
                    try {
                        in.readFully(entry.value);
                    } catch (EOFException e) {
                        return -1;
                    }
                }
            }
            loaded.add(entry);
        }

        dictionary.entries.addAll(loaded);
        return 0;
    }

    // NOTE: Unused. 0x4DA2EC assoc_write_long.
    public static void dictionaryWriteInt(DataOutput out, int value) throws IOException {
        out.writeByte((value >> 24) & 0xFF);
        out.writeByte((value >> 16) & 0xFF);
        out.writeByte((value >> 8) & 0xFF);
        out.writeByte(value & 0xFF);
    }

    // NOTE: Unused. 0x4DA360 assoc_write_assoc_array.
    public static void dictionaryWriteHeader(DataOutput out, Dict dictionary) throws IOException {
        dictionaryWriteInt(out, dictionary.entriesLength);
        dictionaryWriteInt(out, dictionary.entriesCapacity);
        dictionaryWriteInt(out, dictionary.valueSize);
        // NOTE: Originally writes `entries` pointer.
        dictionaryWriteInt(out, 0);
    }

    // NOTE: Unused. 0x4DA3A4 assoc_save.
    public static int dictionaryWrite(DataOutput out, Dict dictionary, int a3) throws IOException {
        if (!isInitialized(dictionary)) {
            return -1;
        }

        dictionaryWriteHeader(out, dictionary);

        for (int index = 0; index < dictionary.entriesLength; index++) {
            Entry entry = dictionary.entries.get(index);
            byte[] keyBytes = entry.key.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            // NOTE: C fputc's strlen (truncates at 256+); same bound here.
            if (keyBytes.length > 255) {
                return -1;
            }
            out.writeByte(keyBytes.length);
            out.write(keyBytes);

            if (dictionary.io != null && dictionary.io.writeProc != null) {
                if (dictionary.valueSize != 0) {
                    if (dictionary.io.writeProc.write(out, entry.value, dictionary.valueSize, a3) != 0) {
                        return -1;
                    }
                }
            } else {
                if (dictionary.valueSize != 0) {
                    out.write(entry.value, 0, dictionary.valueSize);
                }
            }
        }

        return 0;
    }
}
