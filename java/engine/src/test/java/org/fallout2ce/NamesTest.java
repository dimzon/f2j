package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [names]} vectors from {@code docker/refharness/golden/v88.txt},
 * produced by {@code sectionNames} in {@code docker/refharness/ref_main.cc}:
 * {@code trait=1:[Gifted] perk=1:[Tag!] stat=1:[Strength] skill=1:[Sneak]}.
 *
 * <p>Mirrors the harness {@code getMsgText} helper exactly: local
 * {@link Message.MessageList} via {@link Message#messageListInit},
 * {@code "game\\"} + file via {@link Message#messageListLoad},
 * {@link Message#messageListGetItem}, then {@link Message#messageListFree}.
 *
 * <p>Message id conventions mirror the init loops that own them:
 * trait {@code 100+trait} ({@code src/trait.cc:115}), perk {@code 101+perk}
 * ({@code src/perk.cc:283}), stat {@code 100+stat} ({@code src/stat.cc:169}),
 * skill {@code 100+skill} ({@code src/skill.cc:376}). Ids come from the Defs
 * tables: trait 15 ({@link TraitDefs.Trait#TRAIT_GIFTED}), perk 51
 * ({@link PerkDefs.Perk#PERK_TAG}), stat 0
 * ({@link StatDefs.Stat#STAT_STRENGTH}), skill 8
 * ({@link SkillDefs.Skill#SKILL_SNEAK}).
 *
 * <p>No new production file: pure {@link Message} usage. The Defs tables
 * cannot expose name lookups themselves — names live in message-file state
 * ({@code trait.msg}/{@code perk.msg}/{@code stat.msg}/{@code skill.msg}),
 * only available in the game phase; this test loads the files directly like
 * the harness helper.
 *
 * <p>VFS mounts mirror {@link ItemNamesTest}: {@code master.dat} plus
 * {@code patch000.dat}/{@code critter.dat} when present.
 */
class NamesTest {

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path p = Paths.get(root, name);
        if (!Files.isRegularFile(p)) {
            throw new IllegalStateException("missing " + p);
        }
        return p.toString();
    }

    @BeforeEach
    void setup() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        Message.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        if (Files.isRegularFile(Paths.get(gameEn, "patch000.dat"))) {
            XFile.xbaseOpen(gameEn + "/patch000.dat");
        }
        if (Files.isRegularFile(Paths.get(gameEn, "critter.dat"))) {
            XFile.xbaseOpen(gameEn + "/critter.dat");
        }
    }

    @AfterEach
    void teardown() {
        Message.resetForTests();
        Db.resetForTests();
        ProtoDb.resetForTests();
    }

    /** Harness {@code getMsgText} mirror: load {@code game\<file>}, get, free. */
    static String getMsgText(String file, int num) {
        Message.MessageList msgList = new Message.MessageList();
        assertTrue(Message.messageListInit(msgList));
        assertTrue(Message.messageListLoad(msgList, "game\\" + file));
        Message.Item item = new Message.Item();
        item.num = num;
        assertTrue(Message.messageListGetItem(msgList, item));
        assertTrue(Message.messageListFree(msgList));
        return item.text;
    }

    @Test
    void namesVectors() {
        // Id sources: Defs tables (verified: 15 / 51 / 0 / 8).
        assertEquals(15, TraitDefs.Trait.TRAIT_GIFTED.id());
        assertEquals(51, PerkDefs.Perk.PERK_TAG.id());
        assertEquals(0, StatDefs.Stat.STAT_STRENGTH.id());
        assertEquals(8, SkillDefs.Skill.SKILL_SNEAK.id());

        String traitName = getMsgText("trait.msg", 100 + TraitDefs.Trait.TRAIT_GIFTED.id());
        String perkName = getMsgText("perk.msg", 101 + PerkDefs.Perk.PERK_TAG.id());
        String statName = getMsgText("stat.msg", 100 + StatDefs.Stat.STAT_STRENGTH.id());
        String skillName = getMsgText("skill.msg", 100 + SkillDefs.Skill.SKILL_SNEAK.id());

        assertEquals("Gifted", traitName);
        assertEquals("Tag!", perkName);
        assertEquals("Strength", statName);
        assertEquals("Sneak", skillName);

        // Verbatim golden line (v88.txt [names], 1 line appended to v87).
        String line = "trait=1:[" + traitName + "] perk=1:[" + perkName + "] stat=1:[" + statName + "] skill=1:[" + skillName + "]";
        assertEquals("trait=1:[Gifted] perk=1:[Tag!] stat=1:[Strength] skill=1:[Sneak]", line);
    }
}
