package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * In-memory vectors cross-checked against
 * {@code docker/refharness/golden/v3.txt} ({@code [dict]}); the byte
 * format is validated against a real C-written fixture ({@code [dictFile]}).
 */
class DictionaryTest {

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }

    private static List<String> order(Dictionary.Dict d) {
        List<String> keys = new ArrayList<>();
        for (Dictionary.Entry e : d.entries) {
            keys.add(e.key);
        }
        return keys;
    }

    @Test
    void uninitializedOpsFail() {
        Dictionary.Dict d = new Dictionary.Dict();
        assertEquals(-1, Dictionary.dictionaryGetIndexByKey(d, "a"));
        assertEquals(-1, Dictionary.dictionaryAddValue(d, "a", null));
        assertEquals(-1, Dictionary.dictionaryFree(d));
    }

    @Test
    void addKeepsCaseInsensitiveOrder() {
        Dictionary.Dict d = new Dictionary.Dict();
        assertEquals(0, Dictionary.dictionaryInit(d, 0, 4, null));
        assertEquals(0, Dictionary.dictionaryAddValue(d, "delta", bytes(4, 0, 0, 0)));
        assertEquals(0, Dictionary.dictionaryAddValue(d, "alpha", bytes(1, 0, 0, 0)));
        assertEquals(0, Dictionary.dictionaryAddValue(d, "CHARLIE", bytes(3, 0, 0, 0)));
        assertEquals(0, Dictionary.dictionaryAddValue(d, "bravo", bytes(2, 0, 0, 0)));

        assertEquals(List.of("alpha", "bravo", "CHARLIE", "delta"), order(d));
        assertEquals(6, d.entriesCapacity); // growth: 0 -> 2 -> 6
        assertArrayEquals(bytes(3, 0, 0, 0), d.entries.get(2).value);
    }

    @Test
    void getIsCaseInsensitive() {
        Dictionary.Dict d = new Dictionary.Dict();
        Dictionary.dictionaryInit(d, 0, 4, null);
        Dictionary.dictionaryAddValue(d, "alpha", bytes(1, 0, 0, 0));

        assertEquals(0, Dictionary.dictionaryGetIndexByKey(d, "ALPHA"));
        assertEquals(-1, Dictionary.dictionaryGetIndexByKey(d, "zzz"));
        assertEquals(-1, Dictionary.dictionaryAddValue(d, "Alpha", bytes(9, 0, 0, 0)));
    }

    @Test
    void remove() {
        Dictionary.Dict d = new Dictionary.Dict();
        Dictionary.dictionaryInit(d, 0, 4, null);
        Dictionary.dictionaryAddValue(d, "alpha", bytes(1, 0, 0, 0));
        Dictionary.dictionaryAddValue(d, "bravo", bytes(2, 0, 0, 0));

        assertEquals(0, Dictionary.dictionaryRemoveValue(d, "BRAVO"));
        assertEquals(List.of("alpha"), order(d));
        assertEquals(-1, Dictionary.dictionaryRemoveValue(d, "bravo"));
    }

    @Test
    void copyAndCapacity() {
        Dictionary.Dict d = new Dictionary.Dict();
        Dictionary.dictionaryInit(d, 0, 4, null);
        Dictionary.dictionaryAddValue(d, "alpha", bytes(1, 0, 0, 0));
        Dictionary.dictionaryAddValue(d, "bravo", bytes(2, 0, 0, 0));

        Dictionary.Dict c = new Dictionary.Dict();
        assertEquals(0, Dictionary.dictionaryCopy(c, d));
        assertEquals(order(d), order(c));
        assertEquals(d.entriesCapacity, c.entriesCapacity);

        assertEquals(-1, Dictionary.dictionarySetCapacity(d, 1));
        assertEquals(0, Dictionary.dictionarySetCapacity(d, 10));
        assertEquals(10, d.entriesCapacity);

        assertEquals(0, Dictionary.dictionaryFree(d));
        assertEquals(-1, Dictionary.dictionaryGetIndexByKey(d, "alpha"));
    }

    @Test
    void loadsCFixture() throws Exception {
        // dict-v1.bin was written by the C harness (sectionDictFile).
        InputStream fixture = DictionaryTest.class.getResourceAsStream("/fixtures/dict-v1.bin");
        byte[] raw = fixture.readAllBytes();
        assertEquals(35, raw.length);

        Dictionary.Dict d = new Dictionary.Dict();
        Dictionary.dictionaryInit(d, 0, 0, null);
        assertEquals(0, Dictionary.dictionaryLoad(new DataInputStream(new ByteArrayInputStream(raw)), d, 0));

        assertEquals(2, d.entriesLength);
        assertEquals(2, d.entriesCapacity);
        assertEquals(4, d.valueSize);
        assertEquals(List.of("alpha", "beta"), order(d));
        assertArrayEquals(bytes(1, 2, 3, 4), d.entries.get(0).value);
        assertArrayEquals(bytes(5, 6, 7, 8), d.entries.get(1).value);
    }

    @Test
    void roundTripMatchesCLayout() throws Exception {
        Dictionary.Dict d = new Dictionary.Dict();
        Dictionary.dictionaryInit(d, 0, 4, null);
        Dictionary.dictionaryAddValue(d, "alpha", bytes(1, 2, 3, 4));
        Dictionary.dictionaryAddValue(d, "beta", bytes(5, 6, 7, 8));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertEquals(0, Dictionary.dictionaryWrite(new DataOutputStream(bytes), d, 0));

        // Byte-identical to what the C harness wrote for the same content.
        InputStream fixture = DictionaryTest.class.getResourceAsStream("/fixtures/dict-v1.bin");
        assertArrayEquals(fixture.readAllBytes(), bytes.toByteArray());

        Dictionary.Dict back = new Dictionary.Dict();
        Dictionary.dictionaryInit(back, 0, 0, null);
        assertEquals(0,
                Dictionary.dictionaryLoad(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), back, 0));
        assertEquals(order(d), order(back));
        assertArrayEquals(d.entries.get(1).value, back.entries.get(1).value);
    }
}
