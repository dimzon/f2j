package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [stats]} vectors from {@code docker/refharness/golden/v108.txt},
 * produced by {@code sectionStats} in
 * {@code docker/refharness/ref_main.cc}.
 *
 * <p>Covers the part of {@code statsInit} that is reachable headless: the
 * {@code game\stat.msg} description tables and the XP thresholds. The stat
 * engine itself and the standard-message-list registration are game phase.
 *
 * <p>The XP lines matter because this build has no {@code [stats]} section in
 * {@code fallout2.cfg}, so {@code xp_table} is empty and the threshold
 * count stays zero — meaning every level takes the fallback formula branch
 * rather than the content-table one. The vectors pin the formula result at
 * several levels, including the odd/even split and the level-98 truncation.
 */
class StatDescriptionsTest {

    private String savedLanguage;

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
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        Stat.resetForTests();

        savedLanguage = Settings.settings.system.language;
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        XFile.xbaseOpen(gameEn + "/master.dat");
        Settings.settings.system.language = Message.ENGLISH;

        assertEquals(0, Stat.statsInit());
    }

    @Test
    void statNamesAndDescriptions() {
        assertEquals("Strength", Stat.statGetName(0));
        assertEquals("Perception", Stat.statGetName(1));
        assertEquals("Agility", Stat.statGetName(5));
        // The two CURRENT pseudostats carry a name but no prose.
        assertEquals("Current Poison Level", Stat.statGetName(36));
        assertEquals("Current Radiation Level", Stat.statGetName(37));

        assertEquals("Current Poison Level", Stat.statGetDescription(36));
        assertEquals("Current Radiation Level", Stat.statGetDescription(37));

        // Out of range is null on both accessors, not a lookup.
        assertNull(Stat.statGetName(-1));
        assertNull(Stat.statGetDescription(-1));
    }

    @Test
    void pcStatNames() {
        assertEquals("Unspent Skill Points", Stat.pcStatGetName(0));
        assertEquals("Level", Stat.pcStatGetName(1));
        assertEquals("Karma", Stat.pcStatGetName(4));
    }

    @Test
    void experienceUsesFallbackFormula() {
        // No [stats]/xp_table in this build: the threshold count is zero, so
        // every level takes the fallback branch (1000 * (level/2) * level for
        // odd levels, 1000 * (level/2) * (level - 1) for even).
        assertEquals(0, Stat.pcGetExperienceForLevel(1));
        assertEquals(1000, Stat.pcGetExperienceForLevel(2));
        assertEquals(3000, Stat.pcGetExperienceForLevel(3));
        assertEquals(45000, Stat.pcGetExperienceForLevel(10));
        assertEquals(1225000, Stat.pcGetExperienceForLevel(50));
        assertEquals(4753000, Stat.pcGetExperienceForLevel(98));

        // pcGetExperienceForNextLevel is one past the current level, which is
        // still 1 before statsInit moves PC_STAT_LEVEL off its reset value.
        assertEquals(1000, Stat.pcGetExperienceForNextLevel());
    }
}
