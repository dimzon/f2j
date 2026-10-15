package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Vectors cross-checked against {@code docker/refharness/golden/v6.txt}
 * ({@code [pattern]}). {@code \u001A} is the SUB matcher.
 */
class FPatternTest {

    private static void assertMatch(String pat, String name, boolean expected) {
        assertEquals(expected, FPattern.match(pat, name), "[" + escape(pat) + "] [" + name + "]");
    }

    private static String escape(String s) {
        return s.replace("\u001A", "<SUB>");
    }

    @Test
    void datPatterns() {
        assertMatch("text\\english\\game\\*.msg", "text\\english\\game\\misc.msg", true);
        assertMatch("text\\english\\game\\*.msg", "text\\english\\cuts\\elder.sve", false);
        // NOTE: DELIM=0, so * crosses separators (unlike fnmatch).
        assertMatch("*.msg", "a\\misc.msg", true);
        assertMatch("MISC.MSG", "misc.msg", true);
        assertMatch("text\\english\\game\\misc.msg", "TEXT\\ENGLISH\\GAME\\MISC.MSG", true);
    }

    @Test
    void wildcards() {
        assertMatch("*.msg", "misc.msg", true);
        assertMatch("?", "a", true);
        assertMatch("?", "", false);
        assertMatch("?", "ab", false);
        assertMatch("f??.txt", "foo.txt", true);
        assertMatch("a*c", "ax-yyy.c", true);
        assertMatch("", "", true);
        assertMatch("*", "", false);
        assertMatch("", "a", false);
        assertMatch("abc", "abc", true);
        assertMatch("abc", "abd", false);
    }

    @Test
    void subMatcher() {
        assertMatch("a\u001Ac", "axc", true);
        assertMatch("a\u001Ac", "ax-yyy.c", false);
    }

    @Test
    void sets() {
        assertMatch("a[0-9]z", "a1z", true);
        assertMatch("a[!0-9]z", "aoz", true);
        assertMatch("a[!0-9]z", "a1z", false);
        assertMatch("a[x-]b", "a-b", false);
        assertMatch("a[!-]b", "a-b", false);
    }

    @Test
    void quotes() {
        assertMatch("a`?b", "a?b", true);
        assertMatch("a``b", "a`b", true);
    }

    @Test
    void negation() {
        assertMatch("!a", "b", true);
        assertMatch("!a", "a", false);
    }

    @Test
    void validity() {
        assertFalse(FPattern.isValid("a["));
        assertTrue(FPattern.isValid("a"));
        assertFalse(FPattern.match("a[", "a["));
    }

    @Test
    void nulls() {
        assertFalse(FPattern.match(null, "a"));
        assertFalse(FPattern.match("a", null));
        assertFalse(FPattern.isValid(null));
    }
}
