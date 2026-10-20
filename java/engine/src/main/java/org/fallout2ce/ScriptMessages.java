package org.fallout2ce;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The game's message strings.
 *
 * <p>Two pieces: the list of the game's scripts, and the message file that
 * goes with each of them.
 *
 * <p>The script list is {@code scripts\scripts.lst}, one line per script:
 * the file name, a comment, and {@code # local_vars=N}. The name is what
 * the message file is called - a script {@code ACBrahmn.int} has a message
 * file {@code text\english\dialog\ACBrahmn.msg}.
 *
 * <p>A message file holds entries of three brace-delimited fields: the
 * message number, the audio file, and the text. Newlines inside a field are
 * dropped as they are read.
 */
final class ScriptMessages {

    /** The field size the original's reader allows. */
    private static final int FIELD_MAX_SIZE = 4096;

    /** The script name field is 13 characters in the original. */
    private static final int NAME_MAX_SIZE = 13;

    /** One entry of a message file. */
    static final class Entry {
        int num;
        String audio = "";
        String text = "";
    }

    /** The script list: the names, in list order. */
    private static final List<String> SCRIPT_NAMES = new ArrayList<>();

    /** The local variable counts, parallel to the names. */
    private static final List<Integer> SCRIPT_LOCAL_VARS = new ArrayList<>();

    /** The message files, loaded on demand and kept. */
    private static final Map<Integer, List<Entry>> MESSAGE_LISTS = new HashMap<>();

    private ScriptMessages() {
    }

    /**
     * Port of {@code scriptsLoadScriptsList} (src/scripts.cc): reads
     * {@code scripts\scripts.lst}.
     */
    static void loadScriptsList(byte[] data) {
        SCRIPT_NAMES.clear();
        SCRIPT_LOCAL_VARS.clear();

        String text = new String(data, StandardCharsets.ISO_8859_1);
        for (String line : text.split("\n")) {
            String entry = line;
            if (entry.endsWith("\r")) {
                entry = entry.substring(0, entry.length() - 1);
            }

            int dot = entry.indexOf(".int");
            String name = "";
            if (dot >= 0) {
                int length = dot;
                if (length > NAME_MAX_SIZE) {
                    continue;
                }
                name = entry.substring(0, length);
            }

            int localVars = 0;
            int comment = entry.indexOf('#');
            if (comment >= 0) {
                int localVarsAt = entry.indexOf("local_vars=", comment);
                if (localVarsAt >= 0) {
                    localVars = parseInt(entry.substring(localVarsAt + "local_vars=".length()));
                }
            }

            SCRIPT_NAMES.add(name);
            SCRIPT_LOCAL_VARS.add(localVars);
        }
    }

    /**
     * The script's file name, the original's {@code scriptsGetFileName}: the
     * list entry with {@code .int} appended, or an empty name when the index
     * is out of range.
     */
    static String scriptFileName(int scriptIndex) {
        if (scriptIndex < 0 || scriptIndex >= SCRIPT_NAMES.size()) {
            return "";
        }
        return SCRIPT_NAMES.get(scriptIndex) + ".int";
    }

    /** The number of local variables the script list gives for a script. */
    static int scriptLocalVarsCount(int scriptIndex) {
        if (scriptIndex < 0 || scriptIndex >= SCRIPT_LOCAL_VARS.size()) {
            return 0;
        }
        return SCRIPT_LOCAL_VARS.get(scriptIndex);
    }

    /**
     * Port of {@code _message_load_field} (src/message.cc): reads one
     * brace-delimited field, dropping the newlines.
     *
     * @return 0 on success, 1 at the end of the file, 2 on a mismatched
     *         delimiter, 3 at the end of a field, 4 when the field is too long
     */
    private static int loadField(String text, int[] position, StringBuilder out) {
        int pos = position[0];

        while (true) {
            if (pos >= text.length()) {
                position[0] = pos;
                return 1;
            }
            char ch = text.charAt(pos++);
            if (ch == '}') {
                position[0] = pos;
                return 2;
            }
            if (ch == '{') {
                break;
            }
        }

        while (true) {
            if (pos >= text.length()) {
                position[0] = pos;
                return 3;
            }
            char ch = text.charAt(pos++);
            if (ch == '}') {
                position[0] = pos;
                return 0;
            }
            if (ch != '\n') {
                out.append(ch);
                if (out.length() >= FIELD_MAX_SIZE) {
                    position[0] = pos;
                    return 4;
                }
            }
        }
    }

    /**
     * Port of {@code messageListLoad} (src/message.cc): reads a message file
     * of three-field entries.
     */
    private static List<Entry> loadMessageList(byte[] data) {
        // The C reads message files in text mode, where the DFile turns a
        // CRLF into a newline, and the field reader then drops the newline.
        // The lines of a long message start with a space, so what is left is
        // the message on one line.
        String text = new String(data, StandardCharsets.ISO_8859_1).replace("\r\n", "\n");
        List<Entry> entries = new ArrayList<>();
        int[] position = { 0 };
        StringBuilder num = new StringBuilder();
        StringBuilder audio = new StringBuilder();
        StringBuilder message = new StringBuilder();

        while (true) {
            num.setLength(0);
            if (loadField(text, position, num) != 0) {
                break;
            }
            audio.setLength(0);
            if (loadField(text, position, audio) != 0) {
                break;
            }
            message.setLength(0);
            if (loadField(text, position, message) != 0) {
                break;
            }

            Entry entry = new Entry();
            entry.num = parseInt(num.toString());
            entry.audio = audio.toString();
            entry.text = message.toString();
            entries.add(entry);
        }

        return entries;
    }

    /**
     * The text of a message, or null when there is none.
     *
     * <p>The list index is one-based, the way the original's
     * {@code scriptsGetMessageList} treats it, and the message file is the
     * script's name under {@code dialog}.
     */
    static String messageText(int messageListId, int messageId) {
        if (messageListId == 0 && messageId == 0) {
            return "";
        }
        if (messageListId == -1 && messageId == -1) {
            return "";
        }
        if (messageListId == -2 && messageId == -2) {
            return null;
        }

        int listIndex = messageListId - 1;
        if (listIndex < 0) {
            return null;
        }

        String scriptName = scriptFileName(listIndex & 0xFFFFFF);
        int dot = scriptName.lastIndexOf('.');
        if (dot >= 0) {
            scriptName = scriptName.substring(0, dot);
        }

        List<Entry> entries = MESSAGE_LISTS.get(messageListId);
        if (entries == null) {
            byte[] data = readMessageFile(scriptName);
            if (data == null) {
                return null;
            }
            entries = loadMessageList(data);
            MESSAGE_LISTS.put(messageListId, entries);
        }

        for (Entry entry : entries) {
            if (entry.num == messageId) {
                return entry.text;
            }
        }

        return null;
    }

    /** Reads {@code text\english\dialog\<name>.msg} out of the game data. */
    private static byte[] readMessageFile(String scriptName) {
        if (scriptName.isEmpty()) {
            return null;
        }

        int[] size = { -1 };
        String path = "text\\english\\dialog\\" + scriptName + ".msg";
        if (Db.dbGetFileSize(path, size) != 0 || size[0] <= 0) {
            return null;
        }

        byte[] data = new byte[size[0]];
        if (Db.dbGetFileContents(path, data, 0) != 0) {
            return null;
        }

        return data;
    }

    /** The original's {@code _message_parse_number}: a plain decimal number. */
    private static int parseInt(String text) {
        String trimmed = text.trim();
        int end = 0;
        if (!trimmed.isEmpty() && (trimmed.charAt(0) == '-' || trimmed.charAt(0) == '+')) {
            end = 1;
        }
        while (end < trimmed.length() && Character.isDigit(trimmed.charAt(end))) {
            end++;
        }
        if (end == 0 || (end == 1 && !Character.isDigit(trimmed.charAt(0)))) {
            return 0;
        }
        try {
            return Integer.parseInt(trimmed.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
