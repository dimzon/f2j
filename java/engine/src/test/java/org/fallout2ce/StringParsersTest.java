package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Vectors cross-checked against {@code docker/refharness/golden/v2.txt}
 * ({@code [strParsers]} / {@code [strParseFunc]}).
 */
class StringParsersTest {

    private static final String[] LIST = { "one", "two", "three" };

    @Test
    void parseInt() {
        StringParsers.Cursor c = new StringParsers.Cursor("  12, 34,ABC");
        int[] value = { -99 };
        assertEquals(0, StringParsers.strParseInt(c, value));
        assertEquals(12, value[0]);
        // Remainder is lowercased in place, like C.
        assertEquals(" 34,abc", c.rest());
    }

    @Test
    void parseIntEmptyToken() {
        StringParsers.Cursor c = new StringParsers.Cursor(",");
        int[] value = { -99 };
        assertEquals(0, StringParsers.strParseInt(c, value));
        assertEquals(0, value[0]);
        assertEquals("", c.rest());
    }

    @Test
    void parseIntChained() {
        StringParsers.Cursor c = new StringParsers.Cursor("10,20");
        int[] value = { -99 };
        assertEquals(0, StringParsers.strParseInt(c, value));
        assertEquals(10, value[0]);
        assertEquals(0, StringParsers.strParseInt(c, value));
        assertEquals(20, value[0]);
        assertEquals("", c.rest());
    }

    @Test
    void parseList() {
        StringParsers.Cursor c = new StringParsers.Cursor("Two, x");
        int[] value = { -99 };
        assertEquals(0, StringParsers.strParseStrFromList(c, value, LIST));
        assertEquals(1, value[0]);
        assertEquals(" x", c.rest());
    }

    @Test
    void parseListMiss() {
        StringParsers.Cursor c = new StringParsers.Cursor("seven");
        int[] value = { -99 };
        assertEquals(-1, StringParsers.strParseStrFromList(c, value, LIST));
        assertEquals(-1, value[0]);
        assertEquals("", c.rest());
    }

    @Test
    void parseWithKey() {
        StringParsers.Cursor c = new StringParsers.Cursor("count=3, x=4");
        int[] value = { -99 };
        assertEquals(0, StringParsers.strParseIntWithKey(c, "count", value, "="));
        assertEquals(3, value[0]);
        assertEquals(" x=4", c.rest());
    }

    @Test
    void parseWithKeyMissLeavesCursor() {
        StringParsers.Cursor c = new StringParsers.Cursor("x=4");
        int[] value = { -99 };
        assertEquals(-1, StringParsers.strParseIntWithKey(c, "count", value, "="));
        assertEquals(-99, value[0]);
        assertEquals("x=4", c.rest());
    }

    @Test
    void parseKeyValue() {
        StringParsers.Cursor c = new StringParsers.Cursor("count=3, x=4");
        StringBuilder key = new StringBuilder();
        int[] value = { -99 };
        assertEquals(0, StringParsers.strParseKeyValue(c, key, value, "="));
        assertEquals("count", key.toString());
        assertEquals(3, value[0]);
        // NOTE: unlike WithKey, the trailing comma is NOT consumed in C.
        assertEquals(", x=4", c.rest());
    }

    @Test
    void parseFunc() {
        StringParsers.Cursor c = new StringParsers.Cursor("YES, next");
        int[] value = { -99 };
        assertEquals(0, StringParsers.strParseStrFromFunc(c, value, StringParsersTest::yesNo));
        assertEquals(42, value[0]);
        assertEquals(" next", c.rest());
    }

    @Test
    void parseFuncMiss() {
        StringParsers.Cursor c = new StringParsers.Cursor("nope");
        int[] value = { -99 };
        assertEquals(-1, StringParsers.strParseStrFromFunc(c, value, StringParsersTest::yesNo));
        assertEquals(-1, value[0]);
        assertEquals("", c.rest());
    }

    @Test
    void nullCursor() {
        assertEquals(0, StringParsers.strParseInt(null, new int[1]));
    }

    private static int yesNo(String token, int[] value) {
        if (token.equals("yes")) {
            value[0] = 42;
            return 0;
        }
        return 1;
    }
}
