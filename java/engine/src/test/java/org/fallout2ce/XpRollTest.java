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
 * {@code [xproll]} vectors from
 * {@code docker/refharness/golden/v79.txt}, produced by
 * {@code sectionXpRoll} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Setup mirrors {@link CritterStatsTest} (game data for the dog
 * {@code .pro}, zeroed bounds so the dog's STR clamps to 0 like the
 * headless harness where {@code statsInit} never runs).
 *
 * <p>RNG treatment (exact, no deviation): the test replicates the harness
 * RNG prefix verbatim — {@code randomSeedPrerandom(7)} plus the single
 * {@code randomBetween(5, 3)} call that closes {@code sectionRandom} — and
 * then runs the three {@code statRoll} legs back-to-back like
 * {@code sectionXpRoll}. This reproduces the C stream position exactly
 * because the intervening harness sections consume nothing from the game
 * PRNG stream (audited: object/proto/queue/inventory/trait/gametime/
 * window/tile/art/cache/color/svga/draw/font/region/datafile/config/input/
 * kb/dinput/graph/options/skill-tag/stat paths call neither
 * {@code randomBetween} nor {@code randomRoll}; the only other
 * {@code randomBetween} sites — combat, drug/addiction, item-damage, use-
 * item, badwords-filter — sit in code paths the harness never executes,
 * and window placement uses libc {@code rand()}, a separate stream). The
 * Java PRNG itself is already pinned bit-identical by {@link GameRandomTest}
 * (seeds 12345/1/7 reproduce the {@code [random]} vectors), so the same
 * seed plus the same call sequence yields chances 8, 9 verbatim. Dog
 * creation consumes no RNG on either side (no {@code random} calls in
 * {@code src/object.cc} / {@code src/proto.cc}); the test creates the dog
 * before seeding as extra insulation.
 *
 * <p>ROLL ids: C {@code enum Roll} is {@code CRITICAL_FAILURE=0,
 * FAILURE=1, SUCCESS=2, CRITICAL_SUCCESS=3}; {@link GameRandom.Roll}
 * declares the same order, so {@code ordinal()} pins the golden ids
 * (roll leg ids 1, 2, 1) alongside the enum asserts.
 */
class XpRollTest {

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
        GameRandom.resetProviders();
    }

    @Test
    void xpLadderVectors() {
        Stat.resetForTests();

        int xp1 = Stat.pcGetExperienceForLevel(1);
        int xp2 = Stat.pcGetExperienceForLevel(2);
        int xp5 = Stat.pcGetExperienceForLevel(5);
        int xp21 = Stat.pcGetExperienceForLevel(21);
        int xp99 = Stat.pcGetExperienceForLevel(99);
        int xp0 = Stat.pcGetExperienceForLevel(0);
        int xp100 = Stat.pcGetExperienceForLevel(100);

        assertEquals(0, xp1);
        assertEquals(1000, xp2);
        assertEquals(10000, xp5);
        assertEquals(210000, xp21);
        assertEquals(4851000, xp99);
        assertEquals(-1, xp0);
        assertEquals(-1, xp100);

        String line = String.format("xp=%d,%d,%d,%d,%d,%d,%d",
                xp1, xp2, xp5, xp21, xp99, xp0, xp100);
        assertEquals("xp=0,1000,10000,210000,4851000,-1,-1", line);
    }

    @Test
    void statRollVectors() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        // Dog STR clamps to 0 headless (base 6 + bonus 0 vs zero bounds),
        // so leg values are 0, 0 + 10, 0 — same as C.
        assertEquals(0, Stat.critterGetStat(dog, StatDefs.Stat.STAT_STRENGTH.id()));

        // Harness [random] tail, replicated exactly (see class notes).
        GameRandom.randomSeedPrerandom(7);
        assertEquals(5, GameRandom.randomBetween(5, 3));

        int[] howMuch1 = new int[] { -999 };
        GameRandom.Roll roll1 = Stat.statRoll(
                dog, StatDefs.Stat.STAT_STRENGTH.id(), 0, howMuch1);
        int[] howMuch2 = new int[] { -999 };
        GameRandom.Roll roll2 = Stat.statRoll(
                dog, StatDefs.Stat.STAT_STRENGTH.id(), 10, howMuch2);
        GameRandom.Roll rollNull = Stat.statRoll(
                dog, StatDefs.Stat.STAT_STRENGTH.id(), 0, null);

        assertEquals(GameRandom.Roll.FAILURE, roll1);
        assertEquals(1, roll1.ordinal());
        assertEquals(-8, howMuch1[0]);
        assertEquals(GameRandom.Roll.SUCCESS, roll2);
        assertEquals(2, roll2.ordinal());
        assertEquals(1, howMuch2[0]);
        assertEquals(GameRandom.Roll.FAILURE, rollNull);
        assertEquals(1, rollNull.ordinal());

        String line = String.format("roll=%d:%d rollmod=%d:%d rollnull=%d",
                roll1.ordinal(), howMuch1[0], roll2.ordinal(), howMuch2[0],
                rollNull.ordinal());
        assertEquals("roll=1:-8 rollmod=2:1 rollnull=1", line);
    }
}
