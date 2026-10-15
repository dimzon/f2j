package org.fallout2ce;

/**
 * Port of the {@code FPAT_WINDOWS_PATHS} (DOS) variant of
 * {@code third_party/fpattern} ({@code fpattern_windows_match}).
 *
 * <p>Semantics: {@code DELIM=0}, so separators are ordinary characters
 * ({@code *} crosses them); {@code ?} matches any single char except NUL;
 * {@code \x1A} (SUB) matches runs without {@code '.'}; backtick quotes;
 * {@code [!...]} negates; {@code !} negates the rest; case-insensitive
 * ASCII ({@code tolower} under the C locale).
 */
public final class FPattern {

    private FPattern() {
    }

    private static final char CLOS = '*';
    private static final char SUB = 0x1A;
    private static final char ANY = '?';
    private static final char QUOTE = '`';
    private static final char SET_L = '[';
    private static final char SET_R = ']';
    private static final char SET_NOT = '!';
    private static final char SET_THRU = '-';
    private static final char NOT = '!';

    static int lower(int ch) {
        return ch >= 'A' && ch <= 'Z' ? ch + ('a' - 'A') : ch;
    }

    // NUL-terminated view: C reads pat[len] == '\0' past the end.
    private static int charAtOrNul(String s, int index) {
        return index < s.length() ? s.charAt(index) : 0;
    }

    /** Port of {@code fpattern_windows_isvalid}. */
    public static boolean isValid(String pat) {
        if (pat == null) {
            return false;
        }

        int len = 0;
        while (charAtOrNul(pat, len) != 0) {
            switch (charAtOrNul(pat, len)) {
            case SET_L:
                len++;
                if (charAtOrNul(pat, len) == SET_NOT) {
                    len++;
                }
                while (charAtOrNul(pat, len) != SET_R) {
                    if (charAtOrNul(pat, len) == QUOTE) {
                        len++;
                    }
                    if (charAtOrNul(pat, len) == 0) {
                        return false;
                    }
                    len++;

                    if (charAtOrNul(pat, len) == SET_THRU) {
                        len++;
                        if (charAtOrNul(pat, len) == QUOTE) {
                            len++;
                        }
                        if (charAtOrNul(pat, len) == 0) {
                            return false;
                        }
                        len++;
                    }

                    if (charAtOrNul(pat, len) == 0) {
                        return false;
                    }
                }
                break;

            case QUOTE:
                len++;
                if (charAtOrNul(pat, len) == 0) {
                    return false;
                }
                break;

            case NOT:
                len++;
                if (charAtOrNul(pat, len) == 0) {
                    return false;
                }
                break;

            default:
                break;
            }
            len++;
        }

        return true;
    }

    private static boolean submatch(String pat, int pi, String fname, int fi) {
        while (pi < pat.length()) {
            int fch = fi < fname.length() ? fname.charAt(fi) : 0;
            int pch = pat.charAt(pi);
            pi++;

            switch (pch) {
            case ANY:
                if (fch == 0) {
                    return false;
                }
                fi++;
                break;

            case CLOS: {
                int i = fi;
                while (i < fname.length()) {
                    i++;
                }
                while (i >= fi) {
                    if (submatch(pat, pi, fname, i)) {
                        return true;
                    }
                    i--;
                }
                return false;
            }

            case SUB: {
                int i = fi;
                while (i < fname.length() && fname.charAt(i) != '.') {
                    i++;
                }
                while (i >= fi) {
                    if (submatch(pat, pi, fname, i)) {
                        return true;
                    }
                    i--;
                }
                return false;
            }

            case QUOTE: {
                if (pi >= pat.length()) {
                    return false;
                }
                int q = pat.charAt(pi);
                if (lower(fch) != lower(q) || q == 0) {
                    return false;
                }
                fi++;
                pi++;
                break;
            }

            case SET_L: {
                boolean yes = true;
                if (pi < pat.length() && pat.charAt(pi) == SET_NOT) {
                    pi++;
                    yes = false;
                }

                boolean match = !yes;
                while (pi < pat.length() && pat.charAt(pi) != SET_R) {
                    if (pat.charAt(pi) == QUOTE) {
                        pi++;
                    }
                    if (pi >= pat.length()) {
                        break;
                    }
                    int lo = pat.charAt(pi++);
                    int hi = lo;

                    if (pi < pat.length() && pat.charAt(pi) == SET_THRU) {
                        pi++;
                        if (pi < pat.length() && pat.charAt(pi) == QUOTE) {
                            pi++;
                        }
                        if (pi >= pat.length()) {
                            break;
                        }
                        hi = pat.charAt(pi++);
                    }

                    if (pi >= pat.length()) {
                        break;
                    }

                    if (lower(fch) >= lower(lo) && lower(fch) <= lower(hi)) {
                        match = yes;
                    }
                }

                if (!match) {
                    return false;
                }
                if (pi >= pat.length()) {
                    return false;
                }
                fi++;
                pi++;
                break;
            }

            case NOT:
                if (pi >= pat.length()) {
                    return false;
                }
                return !submatch(pat, pi, fname, fi);

            default:
                if (lower(fch) != lower(pch)) {
                    return false;
                }
                fi++;
                break;
            }
        }

        return fi >= fname.length();
    }

    /** Port of {@code fpattern_windows_match} (validates first). */
    public static boolean match(String pat, String fname) {
        if (fname == null || pat == null) {
            return false;
        }
        if (!isValid(pat)) {
            return false;
        }
        if (fname.isEmpty()) {
            return pat.isEmpty();
        }
        return submatch(pat, 0, fname, 0);
    }

    /** Port of {@code fpattern_windows_matchn} (assumes valid). */
    public static boolean matchN(String pat, String fname) {
        if (fname == null || pat == null) {
            return false;
        }
        return submatch(pat, 0, fname, 0);
    }
}
