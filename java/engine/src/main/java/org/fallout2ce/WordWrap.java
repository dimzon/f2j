package org.fallout2ce;

/**
 * Port of {@code src/word_wrap.h} / {@code src/word_wrap.cc}.
 *
 * <p>C reads glyph metrics through global font procs; here they are an
 * explicit {@link FontMetrics} parameter (no hidden state). The future
 * {@code TextFont} port will supply the live implementation.
 */
public final class WordWrap {

    private WordWrap() {
    }

    public static final int MAX_COUNT = 64;

    public interface FontMetrics {
        int monospacedCharacterWidth();

        int stringWidth(String string);

        int letterSpacing();

        int characterWidth(int ch);
    }

    /**
     * 0x4BC6F0 word_wrap. Fills {@code breakpoints} (caller-owned,
     * {@value #MAX_COUNT} entries) and {@code breakpointsLength[0]}.
     * Returns {@code 0} or {@code -1} exactly like C.
     */
    public static int wordWrap(String string, int width, short[] breakpoints, int[] breakpointsLength,
            FontMetrics metrics) {
        breakpoints[0] = 0;
        breakpointsLength[0] = 1;

        for (int index = 1; index < MAX_COUNT; index++) {
            breakpoints[index] = -1;
        }

        if (metrics.monospacedCharacterWidth() > width) {
            return -1;
        }

        if (metrics.stringWidth(string) < width) {
            breakpoints[breakpointsLength[0]] = (short) string.length();
            breakpointsLength[0] += 1;
            return 0;
        }

        int gap = metrics.letterSpacing();

        int accum = 0;
        int prevSpaceOrHyphen = -1;
        int pch = 0;
        int length = string.length();
        while (pch < length) {
            int ch = string.charAt(pch) & 0xFF;
            accum += gap + metrics.characterWidth(ch);
            if (accum <= width) {
                // NOTE: quests.txt #807 uses extended ascii.
                if (isSpace(ch) || ch == '-') {
                    prevSpaceOrHyphen = pch;
                }
            } else {
                if (breakpointsLength[0] == MAX_COUNT) {
                    return -1;
                }

                if (prevSpaceOrHyphen != -1) {
                    // Word wrap.
                    breakpoints[breakpointsLength[0]] = (short) (prevSpaceOrHyphen + 1);
                    breakpointsLength[0] += 1;

                    pch = prevSpaceOrHyphen;
                } else {
                    // Character wrap.
                    breakpoints[breakpointsLength[0]] = (short) pch;
                    breakpointsLength[0] += 1;

                    pch--;
                }

                prevSpaceOrHyphen = -1;
                accum = 0;
            }
            pch++;
        }

        if (breakpointsLength[0] == MAX_COUNT) {
            return -1;
        }

        breakpoints[breakpointsLength[0]] = (short) (pch + 1);
        breakpointsLength[0] += 1;

        return 0;
    }

    // C locale isspace(byte).
    static boolean isSpace(int ch) {
        return ch == ' ' || (ch >= 0x09 && ch <= 0x0D);
    }
}
