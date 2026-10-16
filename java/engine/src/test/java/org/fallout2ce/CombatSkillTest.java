package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code [combatskill]} vectors from
 * {@code docker/refharness/golden/v54.txt} (produced by
 * {@code sectionCombatSkill}). Needs no game data and no window system:
 * penalty/highlight tables start zeroed (no {@code combatInit}: it touches
 * {@code gDude}), and skill-use stamps come from the {@code 302400} game-time
 * static init.
 *
 * <p>The flow mirrors the harness exactly: two updates, save/read/crc; a
 * third update, save/read/crc; load of the first file, save/read/crc. The
 * third update lands in the last free (zero) slot — no shift, no
 * {@code -1} — so {@code crc2} differs while the load round-trip restores
 * {@code crc1 == crc3}.
 */
class CombatSkillTest {

    @TempDir
    Path temp;

    @BeforeEach
    void setup() {
        Combat.resetForTests();
        Skill.resetForTests();
        EventQueue.resetForTests();
    }

    @AfterEach
    void teardown() {
        Combat.resetForTests();
        Skill.resetForTests();
        EventQueue.resetForTests();
        Db.resetForTests();
    }

    private static String crcHex(byte[] buf) {
        CRC32 crc = new CRC32();
        crc.update(buf);
        return String.format("%08X", crc.getValue());
    }

    private int saveUsage(String name) {
        XFile.Handle stream = Db.fileOpen(temp.resolve(name).toString(), "wb");
        assertNotNull(stream);
        int rc = Skill.skillsUsageSave(stream);
        assertEquals(0, Db.fileClose(stream));
        return rc;
    }

    @Test
    void combatSkillVectors() throws Exception {
        int head = CombatDefs.HitLocation.HIT_LOCATION_HEAD.id();
        int torso = CombatDefs.HitLocation.HIT_LOCATION_TORSO.id();

        // Penalty table starts zeroed (BSS mirror, no combatInit).
        int pen0 = Combat.getHitLocationPenalty(head);
        Combat.setHitLocationPenalty(head, -15);
        int penSet = Combat.getHitLocationPenalty(head);
        Combat.setHitLocationPenalty(99, -15);
        int penBad = Combat.getHitLocationPenalty(99);
        Combat.resetHitLocationPenalty();
        int penHead = Combat.getHitLocationPenalty(head);
        int penTorso = Combat.getHitLocationPenalty(torso);
        int highlight = Combat.getTargetHighlight();
        assertEquals(0, pen0);
        assertEquals(-15, penSet);
        assertEquals(0, penBad);
        assertEquals(-40, penHead);
        assertEquals(0, penTorso);
        assertEquals(0, highlight);

        // Skill-use timestamps stamp the 302400 game-time static init.
        assertEquals(302400, EventQueue.gameTimeGetTime());
        int sneak = SkillDefs.Skill.SKILL_SNEAK.id();

        int upd1 = Skill.updateLastUse(sneak);
        int upd2 = Skill.updateLastUse(sneak);
        int saveRc1 = saveUsage("sku1.bin");
        byte[] buf1 = Files.readAllBytes(temp.resolve("sku1.bin"));
        int upd3 = Skill.updateLastUse(sneak);
        int saveRc2 = saveUsage("sku2.bin");
        byte[] buf2 = Files.readAllBytes(temp.resolve("sku2.bin"));
        XFile.Handle loadBack = Db.fileOpen(temp.resolve("sku1.bin").toString(), "rb");
        assertNotNull(loadBack);
        int loadRc = Skill.skillsUsageLoad(loadBack);
        assertEquals(0, Db.fileClose(loadBack));
        int saveRc3 = saveUsage("sku3.bin");
        byte[] buf3 = Files.readAllBytes(temp.resolve("sku3.bin"));

        assertEquals(0, upd1);
        assertEquals(0, upd2);
        assertEquals(0, upd3);
        assertEquals(0, saveRc1);
        assertEquals(0, saveRc2);
        assertEquals(0, saveRc3);
        assertEquals(0, loadRc);
        assertEquals(216, buf1.length);
        assertEquals(216, buf2.length);
        assertEquals(216, buf3.length);
        String crc1 = crcHex(buf1);
        String crc2 = crcHex(buf2);
        String crc3 = crcHex(buf3);
        assertEquals("E0A24725", crc1);
        assertEquals("94261896", crc2);
        assertEquals("E0A24725", crc3);
        assertEquals(crc1, crc3);
        assertNotEquals(crc1, crc2);
    }
}
