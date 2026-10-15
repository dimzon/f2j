package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Vectors cross-checked against {@code docker/refharness/golden/v2.txt}
 * ({@code [wordWrap]}). Metrics mirror the harness fake font: char 5,
 * spacing 1, monospaced 6, string 6*len.
 */
class WordWrapTest {

    private static final WordWrap.FontMetrics FAKE = new WordWrap.FontMetrics() {
        @Override
        public int monospacedCharacterWidth() {
            return 6;
        }

        @Override
        public int stringWidth(String string) {
            return string.length() * 6;
        }

        @Override
        public int letterSpacing() {
            return 1;
        }

        @Override
        public int characterWidth(int ch) {
            return 5;
        }
    };

    private static short[] wrap(String text, int width, int[] rc) {
        short[] bps = new short[WordWrap.MAX_COUNT];
        int[] len = new int[1];
        rc[0] = WordWrap.wordWrap(text, width, bps, len, FAKE);
        short[] used = new short[len[0]];
        System.arraycopy(bps, 0, used, 0, len[0]);
        return used;
    }

    private static void assertBps(String text, int width, int expectedRc, short... expected) {
        int[] rc = new int[1];
        short[] got = wrap(text, width, rc);
        assertEquals(expectedRc, rc[0], text);
        assertEquals(expected.length, got.length, text);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], got[i], text + " bp[" + i + "]");
        }
    }

    @Test
    void shortStringFits() {
        assertBps("hi", 100, 0, (short) 0, (short) 2);
    }

    @Test
    void tooNarrowFails() {
        assertBps("hi", 3, -1, (short) 0);
    }

    @Test
    void wordWrap() {
        assertBps("aa bb cc", 20, 0, (short) 0, (short) 3, (short) 6, (short) 9);
    }

    @Test
    void charWrap() {
        assertBps("abcdefgh", 20, 0, (short) 0, (short) 3, (short) 6, (short) 9);
    }

    @Test
    void hyphenWrap() {
        assertBps("aa-bb cc", 20, 0, (short) 0, (short) 3, (short) 6, (short) 9);
    }

    @Test
    void emptyString() {
        assertBps("", 100, 0, (short) 0, (short) 0);
    }

    @Test
    void exactMonoWidth() {
        assertBps("hi", 6, 0, (short) 0, (short) 1, (short) 3);
    }
}
