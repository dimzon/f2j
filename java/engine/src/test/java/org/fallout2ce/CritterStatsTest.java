package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [critterstats]} vectors from
 * {@code docker/refharness/golden/v58.txt}, produced by
 * {@code sectionCritterStats} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Game data needed for the {@code .pro} loads (setup mirrors
 * {@link ObjectCreateTest}). Bounds/tag/trait/skill state is reset to the
 * fresh-process mirror (zero bounds, zeroed tags, reset traits, maximum
 * 300), exactly like the headless C++ harness where {@code statsInit} /
 * {@code skillsInit} are out of reach and {@code gDude} is null.
 *
 * <p>Write-mutation policy: the test mirrors the harness exactly and only
 * attempts the bounds-rejected set ({@code -3}, no store), so the shared
 * {@link ProtoDb} cache is never polluted for other tests. Committed writes
 * would mutate cached protos (in C too) and require a reload.
 *
 * <p>{@code gDude} modelling: there is no dude in {@link ObjectSystem} yet;
 * {@link Stat} / {@link Skill} hold a {@code null} holder, so all
 * {@code critter == gDude} checks read false and every dude-only branch
 * (trait adjust on set is still wired but dormant, perk/item/vault bonuses,
 * tag bonuses) stays dormant until the game phase.
 */
class CritterStatsTest {

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
        ObjectSystem.resetForTests();
        Stat.resetForTests();
        Skill.resetForTests();
        TraitDefs.resetForTests();
        EventQueue.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        Stat.resetForTests();
        Skill.resetForTests();
        TraitDefs.resetForTests();
        EventQueue.resetForTests();
    }

    @Test
    void critterStatsVectors() {
        GameObject[] dogSlot = new GameObject[1];
        int dogRc = ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558));
        assertEquals(0, dogRc);
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);

        // dog-rc=0 str=6 agi=6 luck=2 bonus=0 clamped=0
        // (str/agi/luck via the UNCLAMPED base getters; clamped=0 documents
        // the zero-bounds clamp of critterGetStat headless.)
        int dogStr = Stat.critterGetBaseStat(dog, StatDefs.Stat.STAT_STRENGTH.id());
        int dogAgi = Stat.critterGetBaseStat(dog, StatDefs.Stat.STAT_AGILITY.id());
        int dogLuck = Stat.critterGetBaseStat(dog, StatDefs.Stat.STAT_LUCK.id());
        int dogBonus = Stat.critterGetBonusStat(dog, StatDefs.Stat.STAT_STRENGTH.id());
        int dogClamped = Stat.critterGetStat(dog, StatDefs.Stat.STAT_STRENGTH.id());
        assertEquals(6, dogStr);
        assertEquals(6, dogAgi);
        assertEquals(2, dogLuck);
        assertEquals(0, dogBonus);
        assertEquals(0, dogClamped);

        // set-rc=-3 str-after=0 (bounds-rejected: no store, no pollution).
        int setRc = Stat.critterSetBaseStat(dog, StatDefs.Stat.STAT_STRENGTH.id(), 6);
        int dogStrAfter = Stat.critterGetStat(dog, StatDefs.Stat.STAT_STRENGTH.id());
        assertEquals(-3, setRc);
        assertEquals(0, dogStrAfter);

        // sneak=5 guns=5 stim-skill=0 bad-skill=-5
        int sneakVal = Skill.skillGetValue(dog, SkillDefs.Skill.SKILL_SNEAK.id());
        int gunsVal = Skill.skillGetValue(dog, SkillDefs.Skill.SKILL_SMALL_GUNS.id());
        int stimSkill = Skill.skillGetValue(stimSlot[0], SkillDefs.Skill.SKILL_SNEAK.id());
        int badSkill = Skill.skillGetValue(dog, -1);
        assertEquals(5, sneakVal);
        assertEquals(5, gunsVal);
        assertEquals(0, stimSkill);
        assertEquals(-5, badSkill);
    }
}
