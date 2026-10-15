package org.fallout2ce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of {@code src/message.h} / {@code src/message.cc}.
 *
 * <p>Settings seams ({@code language}, {@code languageFilter},
 * {@code contentConfig}) default to vanilla values and are wired for real
 * when the settings/content phases land.
 */
public final class Message {

    private Message() {
    }

    public static final int TEXT_FILTERED = 0x01;
    public static final int FIELD_MAX_SIZE = 1024;
    public static final int BADWORD_LENGTH_MAX = 80;

    // StandardMessageList ids.
    public static final int STANDARD_COMBAT = 0;
    public static final int STANDARD_COMBAT_AI = 1;
    public static final int STANDARD_SCRNAME = 2;
    public static final int STANDARD_MISC = 3;
    public static final int STANDARD_CUSTOM = 4;
    public static final int STANDARD_INVENTORY = 5;
    public static final int STANDARD_ITEM = 6;
    public static final int STANDARD_LSGAME = 7;
    public static final int STANDARD_MAP = 8;
    public static final int STANDARD_OPTIONS = 9;
    public static final int STANDARD_PERK = 10;
    public static final int STANDARD_PIPBOY = 11;
    public static final int STANDARD_QUESTS = 12;
    public static final int STANDARD_PROTO = 13;
    public static final int STANDARD_SCRIPT = 14;
    public static final int STANDARD_SKILL = 15;
    public static final int STANDARD_SKILLDEX = 16;
    public static final int STANDARD_STAT = 17;
    public static final int STANDARD_TRAIT = 18;
    public static final int STANDARD_WORLDMAP = 19;
    public static final int STANDARD_EDITOR = 20;
    public static final int STANDARD_COUNT = 21;

    public static final int PROTO_ITEMS = 0;
    public static final int PROTO_CRITTERS = 1;
    public static final int PROTO_SCENERY = 2;
    public static final int PROTO_TILES = 3;
    public static final int PROTO_WALLS = 4;
    public static final int PROTO_MISC = 5;
    public static final int PROTO_COUNT = 6;

    static final int FIRST_PROTO_ID = 0x1000;
    static final int FIRST_PERSISTENT_ID = 0x2000;
    static final int LAST_PERSISTENT_ID = 0x2FFF;
    static final int FIRST_TEMPORARY_ID = 0x3000;
    static final int LAST_TEMPORARY_ID = 0x3FFF;

    public static final String ENGLISH = "english";

    static final String REPLACEMENTS = "!@#$%&*@#*!&$%#&%#*%!$&%@*$@&";
    static final String ERROR_TEXT = "Error";

    /** Mirror of {@code MessageListItem} (audio/text may be null). */
    public static final class Item {
        public int num;
        public int flags;
        public String audio;
        public String text;
    }

    /** Mirror of {@code MessageList}. */
    public static final class MessageList {
        public final List<Item> entries = new ArrayList<>();
    }

    // Settings seams (vanilla defaults; settings phase wires them).
    static String language = ENGLISH;
    static boolean languageFilter;
    static Config.Cfg contentConfig = new Config.Cfg();

    // badwords state (uppercased words).
    static final List<String> badwords = new ArrayList<>();

    static MessageList[] standardLists = new MessageList[STANDARD_COUNT];
    static MessageList[] protoLists = new MessageList[PROTO_COUNT];
    static final Map<Integer, MessageList> persistentLists = new HashMap<>();
    static final Map<Integer, MessageList> temporaryLists = new HashMap<>();
    static final Map<String, Integer> tempPaths = new HashMap<>();
    static int nextTemporaryId = FIRST_TEMPORARY_ID;

    // 0x484770 init_message. -1 when data\badwords.txt is missing.
    public static int badwordsInit() {
        XFile.Handle stream = Db.fileOpen("data\\badwords.txt", "rt");
        if (stream == null) {
            return -1;
        }

        List<String> words = new ArrayList<>();
        String line;
        while ((line = Db.fileReadString(stream, BADWORD_LENGTH_MAX - 1)) != null) {
            words.add(line);
        }

        List<String> stored = new ArrayList<>(words.size());
        for (String word : words) {
            String stripped = word.endsWith("\n") ? word.substring(0, word.length() - 1) : word;
            stored.add(asciiUpper(stripped));
        }

        Db.fileClose(stream);

        badwords.clear();
        badwords.addAll(stored);
        return 0;
    }

    // 0x4848F0 exit_message.
    public static void badwordsExit() {
        badwords.clear();
    }

    // 0x48494C message_init (null-safe, like C).
    public static boolean messageListInit(MessageList messageList) {
        if (messageList != null) {
            messageList.entries.clear();
        }
        return true;
    }

    // 0x484964 message_exit.
    public static boolean messageListFree(MessageList messageList) {
        if (messageList == null) {
            return false;
        }
        messageList.entries.clear();
        return true;
    }

    // 0x484AA4 message_load.
    public static boolean messageListLoad(MessageList messageList, String path) {
        if (messageList == null || path == null) {
            return false;
        }

        String localizedPath = "text\\" + language + "\\" + path;
        XFile.Handle file = Db.fileOpen(localizedPath, "rt");

        // SFALL: fallback to english.
        if (file == null && !language.equalsIgnoreCase(ENGLISH)) {
            localizedPath = "text\\" + ENGLISH + "\\" + path;
            file = Db.fileOpen(localizedPath, "rt");
        }

        if (file == null) {
            return false;
        }

        boolean success = false;
        String num = "";
        String audio = "";
        String text = "";
        while (true) {
            String[] field = new String[1];
            int rc = loadField(file, field);
            if (rc != 0) {
                if (rc == 1) {
                    success = true;
                }
                break;
            }
            num = field[0];

            if (loadField(file, field) != 0) {
                Debug.print("\nError loading audio field.\n");
                break;
            }
            audio = field[0];

            if (loadField(file, field) != 0) {
                Debug.print("\nError loading text field.\n");
                break;
            }
            text = field[0];

            int[] parsed = new int[1];
            if (!parseNumber(parsed, num)) {
                Debug.print("\nError parsing number.\n");
                break;
            }

            if (!messageAdd(messageList, parsed[0], audio, text)) {
                Debug.print("\nError adding message.\n");
                break;
            }
        }

        if (!success) {
            // NOTE: C uses %lx; Java %x renders the same hex.
            Debug.print("Error loading message file %s at offset %x.", localizedPath, Db.fileTell(file));
        }

        Db.fileClose(file);
        return success;
    }

    // 0x484C30 message_search.
    public static boolean messageListGetItem(MessageList msg, Item entry) {
        if (msg == null || entry == null || msg.entries.isEmpty()) {
            return false;
        }

        int[] index = new int[1];
        if (!messageFind(msg, entry.num, index)) {
            return false;
        }

        Item ptr = msg.entries.get(index[0]);
        entry.flags = ptr.flags;
        entry.audio = ptr.audio;
        entry.text = ptr.text;

        return true;
    }

    // 0x484CB8 message_make_path.
    public static String messageMakePath(String path) {
        if (path == null) {
            return null;
        }
        return "text\\" + language + "\\" + path;
    }

    // 0x484D10 message_find (same quirky walk as Dictionary).
    static boolean messageFind(MessageList msg, int num, int[] outIndex) {
        if (msg.entries.isEmpty()) {
            outIndex[0] = 0;
            return false;
        }

        int r = msg.entries.size() - 1;
        int l = 0;
        int mid = 0;
        int cmp = 0;
        do {
            mid = (l + r) / 2;
            cmp = num - msg.entries.get(mid).num;
            if (cmp == 0) {
                outIndex[0] = mid;
                return true;
            }

            if (cmp > 0) {
                l = l + 1;
            } else {
                r = r - 1;
            }
        } while (r >= l);

        if (cmp < 0) {
            outIndex[0] = mid;
        } else {
            outIndex[0] = mid + 1;
        }

        return false;
    }

    // 0x484D68 message_add (replace keeps flags; insert starts at 0).
    static boolean messageAdd(MessageList msg, int num, String audio, String text) {
        int[] at = new int[1];
        if (messageFind(msg, num, at)) {
            Item existing = msg.entries.get(at[0]);
            existing.audio = audio;
            existing.text = text;
            existing.num = num;
            return true;
        }

        Item entry = new Item();
        entry.flags = 0;
        entry.audio = audio;
        entry.text = text;
        entry.num = num;
        msg.entries.add(at[0], entry);
        return true;
    }

    // 0x484F60 message_parse_number (value always assigned, like atoi).
    static boolean parseNumber(int[] outNum, String str) {
        if (str.isEmpty()) {
            return false;
        }

        int i = 0;
        if (str.charAt(0) == '+' || str.charAt(0) == '-') {
            i++;
        }

        boolean success = true;
        while (i < str.length()) {
            char ch = str.charAt(i);
            if (ch < '0' || ch > '9') {
                success = false;
                break;
            }
            i++;
        }

        outNum[0] = StringParsers.cAtoi(str, 0);
        return success;
    }

    /**
     * 0x484FB4 message_load_field. Returns 0 ok, 1 clean EOF, 2 mismatched
     * delimiters, 3 unterminated, 4 over the 1024 limit.
     */
    static int loadField(XFile.Handle file, String[] out) {
        int ch;
        while (true) {
            ch = Db.fileReadChar(file);
            if (ch == -1) {
                return 1;
            }
            if (ch == '}') {
                Debug.print("\nError reading message file - mismatched delimiters.\n");
                return 2;
            }
            if (ch == '{') {
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        while (true) {
            ch = Db.fileReadChar(file);
            if (ch == -1) {
                Debug.print("\nError reading message file - EOF reached.\n");
                return 3;
            }
            if (ch == '}') {
                out[0] = sb.toString();
                return 0;
            }
            if (ch != '\n') {
                sb.append((char) ch);
                if (sb.length() >= FIELD_MAX_SIZE) {
                    Debug.print("\nError reading message file - text exceeds limit.\n");
                    return 4;
                }
            }
        }
    }

    // 0x48504C getmsg (miss leaves audio/flags stale, like C).
    public static String getmsg(MessageList msg, Item entry, int num) {
        entry.num = num;

        if (!messageListGetItem(msg, entry)) {
            entry.text = ERROR_TEXT;
            Debug.print("\n ** String not found @ getmsg(), MESSAGE.C **\n");
        }

        return entry.text;
    }

    // 0x485078 message_filter.
    public static boolean messageListFilterBadwords(MessageList messageList) {
        if (messageList == null) {
            return false;
        }
        if (messageList.entries.isEmpty()) {
            return true;
        }
        if (badwords.isEmpty()) {
            return true;
        }
        if (!languageFilter) {
            return true;
        }

        int replacementsCount = REPLACEMENTS.length();
        int replacementsIndex = GameRandom.randomBetween(1, replacementsCount) - 1;

        for (Item item : messageList.entries) {
            String upper = asciiUpper(item.text);
            StringBuilder text = new StringBuilder(item.text);

            for (String badword : badwords) {
                int from = 0;
                while (true) {
                    int at = upper.indexOf(badword, from);
                    if (at < 0) {
                        break;
                    }
                    // NOTE: C reads the NUL past the end (not alpha); the
                    // at==0 arm skips the boundary check entirely.
                    int end = at + badword.length();
                    boolean boundary = at == 0
                            || (!isAlpha(upper.charAt(at - 1))
                                    && (end >= upper.length() || !isAlpha(upper.charAt(end))));
                    if (boundary) {
                        item.flags |= TEXT_FILTERED;
                        for (int j = 0; j < badword.length(); j++) {
                            text.setCharAt(at + j, REPLACEMENTS.charAt(replacementsIndex++));
                            if (replacementsIndex == replacementsCount) {
                                replacementsIndex = 0;
                            }
                        }
                    }
                    from = at + 1;
                }
            }

            item.text = text.toString();
        }

        return true;
    }

    // Gender words <masc^fem> (enabled via content config).
    public static void messageListFilterGenderWords(MessageList messageList, Gender gender) {
        if (messageList == null) {
            return;
        }

        boolean[] enabled = new boolean[1];
        Config.configGetBool(contentConfig, "dialog", "gender_words", enabled);
        if (!enabled[0]) {
            return;
        }

        for (Item item : messageList.entries) {
            StringBuilder text = new StringBuilder(item.text);
            int pos = 0;
            while (true) {
                int sep = text.indexOf("^", pos);
                if (sep < 0) {
                    break;
                }
                // NOTE: C NULs the '^' before strrchr, so '<' is searched
                // only BEFORE the separator; the restore is a no-op here.
                int start = text.substring(pos, sep).lastIndexOf('<');
                if (start >= 0) {
                    start += pos;
                }
                int end = text.indexOf(">", sep + 1);

                if (start >= 0 && end >= 0) {
                    String piece;
                    if (gender == Gender.FEMALE) {
                        piece = text.substring(sep + 1, end);
                    } else {
                        piece = text.substring(start + 1, sep);
                    }
                    String result = text.substring(0, start) + piece + text.substring(end + 1);
                    text.setLength(0);
                    text.append(result);
                } else {
                    pos = sep + 1;
                }
            }
            item.text = text.toString();
        }
    }

    public static boolean messageListRepositoryInit() {
        standardLists = new MessageList[STANDARD_COUNT];
        protoLists = new MessageList[PROTO_COUNT];
        persistentLists.clear();
        temporaryLists.clear();
        tempPaths.clear();
        nextTemporaryId = FIRST_TEMPORARY_ID;

        String[] fileList = new String[1];
        Config.configGetString(contentConfig, "text", "extra_msg_file_list", fileList, null);

        String list = fileList[0];
        int nextMessageListId = 0;
        while (list != null) {
            int comma = list.indexOf(',');
            String head = comma >= 0 ? list.substring(0, comma) : list;
            list = comma >= 0 ? list.substring(comma + 1) : null;

            int colon = head.indexOf(':');
            String name = head;
            if (colon >= 0) {
                name = head.substring(0, colon);
                try {
                    nextMessageListId = Integer.parseInt(head.substring(colon + 1));
                } catch (NumberFormatException e) {
                    nextMessageListId = 0;
                }
            }

            MessageList messageList = repositoryLoad("game\\" + name + ".msg");
            if (messageList != null) {
                persistentLists.put(FIRST_PERSISTENT_ID + nextMessageListId, messageList);
            }

            // NOTE: keeps Sfall's odd auto-numbering break (see C comment).
            nextMessageListId++;
            if (nextMessageListId == LAST_PERSISTENT_ID - FIRST_PERSISTENT_ID + 1) {
                break;
            }
        }

        return true;
    }

    public static void messageListRepositoryReset() {
        temporaryLists.clear();
        tempPaths.clear();
        nextTemporaryId = FIRST_TEMPORARY_ID;
    }

    public static void messageListRepositoryExit() {
        temporaryLists.clear();
        persistentLists.clear();
        tempPaths.clear();
        standardLists = new MessageList[STANDARD_COUNT];
        protoLists = new MessageList[PROTO_COUNT];
    }

    public static void messageListRepositorySetStandardMessageList(int id, MessageList messageList) {
        standardLists[id] = messageList;
    }

    public static void messageListRepositorySetProtoMessageList(int id, MessageList messageList) {
        protoLists[id] = messageList;
    }

    public static int messageListRepositoryAddExtra(String path) {
        String normalized = asciiLower(path != null ? path : "");

        Integer existing = tempPaths.get(normalized);
        if (existing != null) {
            return existing;
        }

        if (nextTemporaryId > LAST_TEMPORARY_ID) {
            return -3;
        }

        MessageList messageList = repositoryLoad(path);
        if (messageList == null) {
            return -2;
        }

        int id = nextTemporaryId++;
        temporaryLists.put(id, messageList);
        tempPaths.put(normalized, id);

        return id;
    }

    public static String messageListRepositoryGetMsg(int messageListId, int messageId) {
        MessageList messageList = null;

        if (messageListId >= 0 && messageListId <= STANDARD_COUNT - 1) {
            messageList = standardLists[messageListId];
        } else if (messageListId >= FIRST_PROTO_ID && messageListId <= FIRST_PROTO_ID + PROTO_COUNT - 1) {
            messageList = protoLists[messageListId - FIRST_PROTO_ID];
        } else if (messageListId >= FIRST_PERSISTENT_ID && messageListId <= LAST_PERSISTENT_ID) {
            messageList = persistentLists.get(messageListId);
        } else if (messageListId >= FIRST_TEMPORARY_ID && messageListId <= LAST_TEMPORARY_ID) {
            messageList = temporaryLists.get(messageListId);
        }

        Item item = new Item();
        return getmsg(messageList, item, messageId);
    }

    static MessageList repositoryLoad(String path) {
        MessageList messageList = new MessageList();
        if (!messageListInit(messageList)) {
            return null;
        }
        if (!messageListLoad(messageList, path)) {
            return null;
        }
        return messageList;
    }

    static String asciiUpper(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            out.append(ch >= 'a' && ch <= 'z' ? (char) (ch - ('a' - 'A')) : ch);
        }
        return out.toString();
    }

    static String asciiLower(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            out.append(ch >= 'A' && ch <= 'Z' ? (char) (ch + ('a' - 'A')) : ch);
        }
        return out.toString();
    }

    // C locale isalpha (letters only).
    static boolean isAlpha(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }

    /** Test seam: vanilla defaults (settings/content phases wire these). */
    static void resetForTests() {
        language = ENGLISH;
        languageFilter = false;
        contentConfig = new Config.Cfg();
        badwords.clear();
        standardLists = new MessageList[STANDARD_COUNT];
        protoLists = new MessageList[PROTO_COUNT];
        persistentLists.clear();
        temporaryLists.clear();
        tempPaths.clear();
        nextTemporaryId = FIRST_TEMPORARY_ID;
    }
}
