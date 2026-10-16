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
 * {@code [skillroll]} vectors from
 * {@code docker/refharness/golden/v80.txt}, produced by
 * {@code sectionSkillRoll} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Setup mirrors {@link CritterStatsTest} (game data for the dog/stim
 * {@code .pro} loads, zeroed bounds so the dog's sneak reads 5 and its
 * critical chance clamps to 0 like the headless harness where
 * {@code statsInit} never runs and {@code gDude} is null).
 *
 * <p>RNG treatment (exact, no deviation): the test replicates the harness
 * RNG prefix verbatim — {@code randomSeedPrerandom(7)} plus the single
 * {@code randomBetween(5, 3)} call that closes {@code sectionRandom} — and
 * then replays the three {@code sectionXpRoll} {@code statRoll} legs
 * (d10 draws 8, 9, plus the unprinted {@code rollnull} draw) before running
 * the four {@code skillRoll} legs back-to-back like
 * {@code sectionSkillRoll}. This reproduces the C stream position exactly
 * because the audit holds: {@code grep} over
 * {@code docker/refharness/ref_main.cc} shows the only
 * {@code randomBetween}/{@code randomRoll} consumers between
 * {@code [random]} and {@code [skillroll]} are the {@code sectionRandom}
 * tail itself and the three {@code sectionXpRoll} {@code statRoll} calls
 * (each a single {@code randomBetween(1, 10)}); every intervening section
 * (object/proto/queue/inventory/trait/gametime/window/tile/art/cache/color/
 * svga/draw/font/region/datafile/config/input/kb/dinput/graph/options/
 * skill-tag/pcstat paths) calls neither, and the remaining
 * {@code randomBetween} sites (combat, drug/addiction, item-damage,
 * use-item, badwords-filter) sit in code paths the harness never executes.
 * The Java PRNG itself is already pinned bit-identical by
 * {@link GameRandomTest} (seeds 12345/1/7 reproduce the {@code [random]}
 * vectors), so the same seed plus the same call sequence yields d100
 * chances 91, 4, 2 verbatim. Dog/stim creation consumes no RNG on either
 * side (no {@code random} calls in {@code src/object.cc} /
 * {@code src/proto.cc}); the test creates both before seeding as extra
 * insulation. The invalid-skill leg consumes no RNG on either side
 * (early {@code FAILURE} return before any roll). Day-0 plain path holds
 * on both sides (harness day {@code 302400/864000=0}, Java default clock
 * 0 with {@code criticalsNoTimeLimits=false}), so each valid leg consumes
 * exactly one d100 with no crit-upgrade draw; deltas are 5-91, 100-4,
 * 0-2.
 *
 * <p>ROLL ids: C {@code enum Roll} is {@code CRITICAL_FAILURE=0,
 * FAILURE=1, SUCCESS=2, CRITICAL_SUCCESS=3}; {@link GameRandom.Roll}
 * declares the same order, so {@code ordinal()} pins the golden ids
 * (legs 1, 2, 1, 1) alongside the enum asserts.
 */
class SkillRollTest {

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
    void skillRollVectors() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);
        GameObject stim = stimSlot[0];

        // Preconditions matching the harness: dog sneak 5, stim sneak 0
        // (non-critter), dog critical chance clamped 0 headless.
        assertEquals(5, Skill.skillGetValue(dog, SkillDefs.Skill.SKILL_SNEAK.id()));
        assertEquals(0, Skill.skillGetValue(stim, SkillDefs.Skill.SKILL_SNEAK.id()));
        assertEquals(0, Stat.critterGetStat(dog, StatDefs.Stat.STAT_CRITICAL_CHANCE.id()));

        // Harness [random] tail, replicated exactly (see class notes).
        GameRandom.randomSeedPrerandom(7);
        assertEquals(5, GameRandom.randomBetween(5, 3));

        // v79 trio replay to restore the exact stream position (prefix
        // pin: the two printed deltas must read -8 and 1).
        int[] xpHow1 = new int[] { -999 };
        Stat.statRoll(dog, StatDefs.Stat.STAT_STRENGTH.id(), 0, xpHow1);
        assertEquals(-8, xpHow1[0]);
        int[] xpHow2 = new int[] { -999 };
        Stat.statRoll(dog, StatDefs.Stat.STAT_STRENGTH.id(), 10, xpHow2);
        assertEquals(1, xpHow2[0]);
        Stat.statRoll(dog, StatDefs.Stat.STAT_STRENGTH.id(), 0, null);

        int sneak = SkillDefs.Skill.SKILL_SNEAK.id();
        int[] how1 = new int[] { -999 };
        GameRandom.Roll roll1 = Skill.skillRoll(dog, sneak, 0, how1);
        int[] how2 = new int[] { -999 };
        GameRandom.Roll roll2 = Skill.skillRoll(dog, sneak, 95, how2);
        GameRandom.Roll rollBad = Skill.skillRoll(dog, -1, 0, null);
        int[] howStim = new int[] { -999 };
        GameRandom.Roll rollStim = Skill.skillRoll(stim, sneak, 0, howStim);

        assertEquals(GameRandom.Roll.FAILURE, roll1);
        assertEquals(1, roll1.ordinal());
        assertEquals(-86, how1[0]);
        assertEquals(GameRandom.Roll.SUCCESS, roll2);
        assertEquals(2, roll2.ordinal());
        assertEquals(96, how2[0]);
        assertEquals(GameRandom.Roll.FAILURE, rollBad);
        assertEquals(1, rollBad.ordinal());
        assertEquals(GameRandom.Roll.FAILURE, rollStim);
        assertEquals(1, rollStim.ordinal());
        assertEquals(-2, howStim[0]);

        String line = String.format("roll=%d:%d rollmod=%d:%d rollbad=%d rollstim=%d:%d",
                roll1.ordinal(), how1[0], roll2.ordinal(), how2[0],
                rollBad.ordinal(), rollStim.ordinal(), howStim[0]);
        assertEquals("roll=1:-86 rollmod=2:96 rollbad=1 rollstim=1:-2", line);
    }
}
