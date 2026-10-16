package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [names]} vectors from {@code docker/refharness/golden/v88.txt},
 * produced by {@code sectionNames} in {@code docker/refharness/ref_main.cc}:
 * {@code trait=1:[Gifted] perk=1:[Tag!] stat=1:[Strength] skill=1:[Sneak]}.
 *
 * <p>Also covers {@code [morenames]} vectors from
 * {@code docker/refharness/golden/v89.txt} (2 lines appended to v88),
 * produced by {@code sectionMoreNames}:
 * {@code dogid=34200 dog=1:[Dogmeat] kill=1:[Dogs]} and
 * {@code stimdesc=1:[A healing chem. When injected, the chem provides immediate healing of minor wounds.] gifteddesc=1:[You have more innate abilities than most, so you have not spent as much time honing your skills.  Your primary statistics are e]}.
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

    /** Mirror of C {@code snprintf(dest, destSize, "%s", text)} truncation. */
    static String cTruncate(String text, int destSize) {
        return text.substring(0, Math.min(text.length(), destSize - 1));
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

    /**
     * Harness {@code sectionMoreNames} mirror (v89.txt {@code [morenames]}, 2 lines
     * appended to v88).
     *
     * <p>Message id conventions mirror the owners: dog id from the loaded proto
     * struct ({@link ProtoDb#protoGetProto}, pid 16777558), kill
     * {@code 1450+killType} ({@code src/critter.cc:759} {@code killTypeGetName},
     * dog = 11 {@link ProtoTypes.KillType#KILL_TYPE_DOG}), stimpak description
     * {@code 4001} ({@code NAME} 4000 + 1) in {@code pro_item.msg}, gifted
     * description {@code 200+trait} ({@code src/trait.cc:120}) in
     * {@code trait.msg}.
     *
     * <p>Buffer sizes mirror the harness exactly: dog/kill {@code char[64]},
     * stim/gifted {@code char[128]}. Only gifted truncates (raw 219 chars, cut
     * to 127); the rest fit whole.
     */
    @Test
    void moreNamesVectors() {
        // Dog proto pid from harness ProtoId(16777558); kill dog id from KillType.
        Proto[] dogSlot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofPid(16777558), dogSlot));
        assertNotNull(dogSlot[0]);
        int dogMsgId = dogSlot[0].messageId;
        assertEquals(34200, dogMsgId);
        assertEquals(11, ProtoTypes.KillType.KILL_TYPE_DOG.id());
        assertEquals(15, TraitDefs.Trait.TRAIT_GIFTED.id());

        String dogRaw = getMsgText("pro_crit.msg", dogMsgId);
        String killRaw = getMsgText("proto.msg", 1450 + ProtoTypes.KillType.KILL_TYPE_DOG.id());
        String stimRaw = getMsgText("pro_item.msg", 4001);
        String giftedRaw = getMsgText("trait.msg", 200 + TraitDefs.Trait.TRAIT_GIFTED.id());

        // FULL verbatim texts (copied from golden file / game data).
        assertEquals("Dogmeat", dogRaw);
        assertEquals("Dogs", killRaw);
        assertEquals("A healing chem. When injected, the chem provides immediate healing of minor wounds.", stimRaw);
        assertEquals("You have more innate abilities than most, so you have not spent as much time honing your skills.  Your primary statistics are each +1, but you lose -10% on all skills to start, and receive 5 less skill points per level.", giftedRaw);

        // C snprintf truncation (64 -> 63, 128 -> 127); only gifted is cut.
        String dogName = cTruncate(dogRaw, 64);
        String killName = cTruncate(killRaw, 64);
        String stimDesc = cTruncate(stimRaw, 128);
        String giftedDesc = cTruncate(giftedRaw, 128);
        assertEquals("Dogmeat", dogName);
        assertEquals("Dogs", killName);
        assertEquals("A healing chem. When injected, the chem provides immediate healing of minor wounds.", stimDesc);
        assertEquals("You have more innate abilities than most, so you have not spent as much time honing your skills.  Your primary statistics are e", giftedDesc);

        // Verbatim golden lines (v89.txt [morenames], 2 lines appended to v88).
        String line1 = "dogid=" + dogMsgId + " dog=1:[" + dogName + "] kill=1:[" + killName + "]";
        assertEquals("dogid=34200 dog=1:[Dogmeat] kill=1:[Dogs]", line1);
        String line2 = "stimdesc=1:[" + stimDesc + "] gifteddesc=1:[" + giftedDesc + "]";
        assertEquals("stimdesc=1:[A healing chem. When injected, the chem provides immediate healing of minor wounds.] gifteddesc=1:[You have more innate abilities than most, so you have not spent as much time honing your skills.  Your primary statistics are e]", line2);
    }
}
