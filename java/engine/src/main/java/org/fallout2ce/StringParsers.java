package org.fallout2ce;

/**
 * Port of {@code src/string_parsers.h} / {@code src/string_parsers.cc}.
 *
 * <p>C works on a mutable buffer via {@code char**}: every call lowercases
 * the remainder in place ({@code compat_strlwr} = ASCII {@code tolower}),
 * then consumes one comma-separated token. {@link Cursor} mirrors that:
 * {@link Cursor#text} is mutated, {@link Cursor#pos} is the cursor.
 * {@code strcmp(str, key)} compares against the caller key as-is.
 *
 * <p>The {@code strParse*Enum} C++ templates have no Java counterpart yet;
 * they will land with the first enum-typed call site.
 */
public final class StringParsers {

    private StringParsers() {
    }

    /** Mirror of {@code char** stringPtr} over a mutable buffer. */
    public static final class Cursor {
        public final StringBuilder text;
        public int pos;

        public Cursor(String text) {
            this.text = new StringBuilder(text);
            this.pos = 0;
        }

        /** Remaining unparsed input (for tests/debugging). */
        public String rest() {
            return text.substring(pos);
        }
    }

    /** Mirror of {@code StringParserCallback}: token is already lowered. */
    public interface Callback {
        int parse(String token, int[] value);
    }

    // 0x4AFD10 strParseValue
    public static int strParseInt(Cursor cursor, int[] value) {
        if (cursor == null) {
            return 0;
        }

        strlwrFrom(cursor);
        int str = cursor.pos + span(cursor, cursor.pos, " ");

        int tokenLength = cspn(cursor, str, ",");
        int consumedLength = (str - cursor.pos) + tokenLength;

        cursor.pos = cursor.pos + consumedLength;
        if (cursor.pos < cursor.text.length()) {
            cursor.pos++;
        }

        value[0] = cAtoi(cursor.text, str);
        return 0;
    }

    // 0x4AFE08 strParseStrFromList
    public static int strParseStrFromList(Cursor cursor, int[] value, String[] stringList) {
        if (cursor == null) {
            return 0;
        }

        strlwrFrom(cursor);
        int str = cursor.pos + span(cursor, cursor.pos, " ");

        int tokenLength = cspn(cursor, str, ",");
        int consumedLength = (str - cursor.pos) + tokenLength;

        cursor.pos = cursor.pos + consumedLength;
        if (cursor.pos < cursor.text.length()) {
            cursor.pos++;
        }

        String token = cursor.text.substring(str, str + tokenLength);
        int i;
        for (i = 0; i < stringList.length; i++) {
            if (token.equalsIgnoreCase(stringList[i])) {
                break;
            }
        }

        if (i == stringList.length) {
            Debug.print("\nstrParseStrFromList Error: Couldn't find match for string: %s!", token);
            value[0] = -1;
            return -1;
        }

        value[0] = i;
        return 0;
    }

    // 0x4AFEDC strParseStrFromFunc
    public static int strParseStrFromFunc(Cursor cursor, int[] value, Callback callback) {
        if (cursor == null) {
            return 0;
        }

        strlwrFrom(cursor);
        int str = cursor.pos + span(cursor, cursor.pos, " ");

        int tokenLength = cspn(cursor, str, ",");
        int consumedLength = (str - cursor.pos) + tokenLength;

        cursor.pos = cursor.pos + consumedLength;
        if (cursor.pos < cursor.text.length()) {
            cursor.pos++;
        }

        String token = cursor.text.substring(str, str + tokenLength);
        int result = callback.parse(token, value);

        if (result != 0) {
            Debug.print("\nstrParseStrFromFunc Error: Couldn't find match for string: %s!", token);
            value[0] = -1;
            return -1;
        }

        return 0;
    }

    // 0x4AFF7C strParseStrSepVal
    public static int strParseIntWithKey(Cursor cursor, String key, int[] value, String delimiter) {
        int result = -1;

        if (cursor == null) {
            return 0;
        }

        if (cursor.pos >= cursor.text.length()) {
            return -1;
        }

        strlwrFrom(cursor);

        if (cursor.text.charAt(cursor.pos) == ',') {
            cursor.pos++;
        }

        int str = cursor.pos + span(cursor, cursor.pos, " ");

        int segmentLength = cspn(cursor, str, ",");
        int consumedLength = (str - cursor.pos) + segmentLength;

        int keyLength = cspn(cursor, str, delimiter);
        String head = cursor.text.substring(str, str + keyLength);

        if (head.equals(key)) {
            cursor.pos = cursor.pos + consumedLength;
            // NOTE: if the delimiter is missing, C reads past the segment
            // NUL (adjacent buffer bytes); bounded here to "" -> 0.
            value[0] = cAtoi(cursor.text, Math.min(str + keyLength + 1, cursor.text.length()));
            result = 0;
        }

        if (cursor.pos < cursor.text.length() && cursor.text.charAt(cursor.pos) == ',') {
            cursor.pos++;
        }

        return result;
    }

    // 0x4B005C strParseStrAndSepVal
    public static int strParseKeyValue(Cursor cursor, StringBuilder key, int[] value, String delimiter) {
        if (cursor == null) {
            return 0;
        }

        if (cursor.pos >= cursor.text.length()) {
            return -1;
        }

        strlwrFrom(cursor);

        if (cursor.text.charAt(cursor.pos) == ',') {
            cursor.pos++;
        }

        int str = cursor.pos + span(cursor, cursor.pos, " ");

        int segmentLength = cspn(cursor, str, ",");
        int consumedLength = (str - cursor.pos) + segmentLength;

        int keyLength = cspn(cursor, str, delimiter);

        key.setLength(0);
        key.append(cursor.text, str, str + keyLength);

        cursor.pos = cursor.pos + consumedLength;
        value[0] = cAtoi(cursor.text, Math.min(str + keyLength + 1, cursor.text.length()));

        return 0;
    }

    // compat_strlwr over the remainder (ASCII only, like SDL_strlwr).
    static void strlwrFrom(Cursor cursor) {
        StringBuilder text = cursor.text;
        for (int i = cursor.pos; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                text.setCharAt(i, (char) (ch + ('a' - 'A')));
            }
        }
    }

    // strspn(str, accept): length of leading run of accepted chars.
    static int span(Cursor cursor, int from, String accept) {
        int i = from;
        while (i < cursor.text.length() && accept.indexOf(cursor.text.charAt(i)) >= 0) {
            i++;
        }
        return i - from;
    }

    // strcspn(str, reject): length of leading run without rejected chars.
    static int cspn(Cursor cursor, int from, String reject) {
        int i = from;
        while (i < cursor.text.length() && reject.indexOf(cursor.text.charAt(i)) < 0) {
            i++;
        }
        return i - from;
    }

    // atoi: skip C-locale whitespace, optional sign, digit run; else 0.
    static int cAtoi(CharSequence s, int from) {
        int i = from;
        while (i < s.length() && isSpace(s.charAt(i))) {
            i++;
        }
        boolean negative = false;
        if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            negative = s.charAt(i) == '-';
            i++;
        }
        int value = 0;
        boolean any = false;
        while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            value = value * 10 + (s.charAt(i) - '0');
            any = true;
            i++;
        }
        if (!any) {
            return 0;
        }
        return negative ? -value : value;
    }

    static boolean isSpace(char ch) {
        return ch == ' ' || (ch >= 0x09 && ch <= 0x0D);
    }
}
