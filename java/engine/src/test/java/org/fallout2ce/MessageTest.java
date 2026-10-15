package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code [message]} vectors from {@code docker/refharness/golden/v9.txt}.
 */
class MessageTest {

    @TempDir
    Path temp;

    @AfterEach
    void reset() {
        Message.resetForTests();
        GameRandom.resetProviders();
        Db.resetForTests();
    }

    /** Mounts {@code dir} and loads {@code text/english/<name>} through it. */
    static Message.MessageList loadThrough(Path dir, String name) {
        assertTrue(XFile.xbaseOpen(dir.toString()));
        Message.MessageList list = new Message.MessageList();
        assertTrue(Message.messageListInit(list));
        assertTrue(Message.messageListLoad(list, name));
        return list;
    }

    static void writeMsg(Path dir, String name, String content) throws Exception {
        Path textDir = dir.resolve("text/english");
        Files.createDirectories(textDir);
        Files.write(textDir.resolve(name), content.getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    void loadsFixture() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("msg"));
        try (InputStream in = MessageTest.class.getResourceAsStream("/fixtures/mini.msg")) {
            Path textDir = dir.resolve("text/english");
            Files.createDirectories(textDir);
            Files.write(textDir.resolve("mini.msg"), in.readAllBytes());
        }
        Message.MessageList list = loadThrough(dir, "mini.msg");

        // Duplicate num was replaced, order kept sorted.
        assertEquals(3, list.entries.size());
        Message.Item item = new Message.Item();

        item.num = 0;
        assertTrue(Message.messageListGetItem(list, item));
        assertEquals("text zero", item.text);
        assertEquals("audio0", item.audio);

        item.num = 1;
        assertTrue(Message.messageListGetItem(list, item));
        assertEquals("ONE-replaced", item.text);

        item.num = 999;
        assertFalse(Message.messageListGetItem(list, item));

        assertEquals("Error", Message.getmsg(list, new Message.Item(), 999));
    }

    @Test
    void malformedFails() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("bad"));
        writeMsg(dir, "bad.msg", "} {0}{a}{b}\n");
        assertTrue(XFile.xbaseOpen(dir.toString()));
        Message.MessageList list = new Message.MessageList();
        Message.messageListInit(list);
        assertFalse(Message.messageListLoad(list, "bad.msg"));

        writeMsg(dir, "cut.msg", "{0}{a}{b");
        Message.MessageList list2 = new Message.MessageList();
        Message.messageListInit(list2);
        assertFalse(Message.messageListLoad(list2, "cut.msg"));

        assertFalse(Message.messageListLoad(list2, "nope.msg"));
    }

    @Test
    void badwordsFilter() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("bw"));
        Files.createDirectories(dir.resolve("data"));
        Files.write(dir.resolve("data/badwords.txt"), "foo\nbar\n".getBytes(StandardCharsets.ISO_8859_1));
        assertTrue(XFile.xbaseOpen(dir.toString()));
        assertEquals(0, Message.badwordsInit());

        Message.languageFilter = true;
        GameRandom.randomSeedPrerandom(5);

        Message.MessageList list = new Message.MessageList();
        Message.messageListInit(list);
        // Direct insert (avoids file I/O for the filter unit).
        Message.messageAdd(list, 0, "", "a Foo x bar!");
        Message.messageAdd(list, 1, "", "clean");

        assertTrue(Message.messageListFilterBadwords(list));
        // "Foo" and "bar" masked with cycling replacements from a fixed seed.
        assertEquals(Message.TEXT_FILTERED, list.entries.get(0).flags & Message.TEXT_FILTERED);
        String masked = list.entries.get(0).text;
        assertEquals("a Foo x bar!".length(), masked.length());
        assertTrue(!masked.contains("Foo") && !masked.contains("bar"));
        assertEquals(0, list.entries.get(1).flags & Message.TEXT_FILTERED);
        assertEquals("clean", list.entries.get(1).text);

        Message.badwordsExit();
        assertTrue(Message.messageListFilterBadwords(list));

        Message.languageFilter = false;
        assertTrue(Message.messageListFilterBadwords(list));
    }

    @Test
    void badwordsMissing() {
        assertEquals(-1, Message.badwordsInit());
    }

    @Test
    void genderWords() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("gen"));
        writeMsg(dir, "gender.msg",
                "{0}{}{<He^She> went}\n{1}{}{plain}\n{2}{}{A<man^woman> and <boy^girl> walked}\n{3}{}{a^b}\n{4}{}{<x^y}\n");

        Message.contentConfig = new Config.Cfg();
        Config.configInit(Message.contentConfig);
        Config.configSetString(Message.contentConfig, "dialog", "gender_words", "1");

        Message.MessageList male = loadThrough(dir, "gender.msg");
        Message.messageListFilterGenderWords(male, Gender.MALE);
        assertEquals("He went", male.entries.get(0).text);
        assertEquals("plain", male.entries.get(1).text);
        assertEquals("Aman and boy walked", male.entries.get(2).text);
        assertEquals("a^b", male.entries.get(3).text);
        assertEquals("<x^y", male.entries.get(4).text);

        // NOTE: mounts persist; second load reuses them like the harness.
        Message.MessageList female = loadThrough(dir, "gender.msg");
        Message.messageListFilterGenderWords(female, Gender.FEMALE);
        assertEquals("She went", female.entries.get(0).text);
        assertEquals("Awoman and girl walked", female.entries.get(2).text);

        // Disabled: untouched.
        Message.contentConfig = new Config.Cfg();
        Message.MessageList plain = loadThrough(dir, "gender.msg");
        Message.messageListFilterGenderWords(plain, Gender.FEMALE);
        assertEquals("<He^She> went", plain.entries.get(0).text);
    }

    @Test
    void repositoryRouting() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("repo"));
        writeMsg(dir, "one.msg", "{10}{}{TEN}\n");

        assertTrue(Message.messageListRepositoryInit());
        Message.MessageList list = loadThrough(dir, "one.msg");
        Message.messageListRepositorySetStandardMessageList(Message.STANDARD_MISC, list);

        assertEquals("TEN", Message.messageListRepositoryGetMsg(Message.STANDARD_MISC, 10));
        assertEquals("Error", Message.messageListRepositoryGetMsg(Message.STANDARD_COMBAT, 1));

        int extra = Message.messageListRepositoryAddExtra("one.msg");
        assertEquals(0x3000, extra);
        assertEquals(extra, Message.messageListRepositoryAddExtra("ONE.MSG"));
        assertEquals("TEN", Message.messageListRepositoryGetMsg(extra, 10));
        assertEquals(-2, Message.messageListRepositoryAddExtra("nope.msg"));

        Message.messageListRepositoryReset();
        Message.messageListRepositoryExit();
    }

    // --- game-en (F2J_GAMEDATA) ---

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        Path p = Paths.get(root, name);
        assumeTrue(Files.isRegularFile(p), "missing " + p);
        return p.toString();
    }

    @Test
    void realMiscMsg() {
        String gameEn = Paths.get(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));

        Message.MessageList misc = new Message.MessageList();
        Message.messageListInit(misc);
        assertTrue(Message.messageListLoad(misc, "game\\misc.msg"));
        assertEquals(125, misc.entries.size()); // golden v9

        Message.Item item = new Message.Item();
        item.num = 9;
        assertTrue(Message.messageListGetItem(misc, item));
        assertEquals("INTRO", item.text);
        item.num = 10;
        assertTrue(Message.messageListGetItem(misc, item));
        assertEquals("NEW GAME", item.text);

        // Reload replaces in place: same count.
        assertTrue(Message.messageListLoad(misc, "game\\misc.msg"));
        assertEquals(125, misc.entries.size());
    }
}
