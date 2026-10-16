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
 * {@code [vitals]} vectors from
 * {@code docker/refharness/golden/v66.txt}, produced by
 * {@code sectionVitals} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Fresh objects read zero vitals (hp/poison/radiation are zero-init by
 * {@link ObjectSystem}); the HP adjust clamps against maximum HP, which is
 * 0 headless (bounds tables zeroed, see {@link CritterStatsTest}), so +5 on
 * the hp-0 dog yields hp 0. Poison/radiation adjusts are dude-only, so a
 * non-dude dog yields -1. Game data needed for the {@code .pro} loads
 * (setup mirrors {@link ObjectCreateTest}).
 *
 * <p>No new globals: vitals live on the {@link GameObject} instances
 * ({@link ObjectSystem#resetForTests} covers them). {@link Stat#resetForTests}
 * is still run because {@link Critter#critterAdjustHitPoints} routes
 * maximum HP through {@link Stat#critterGetStat}, which reads the shared
 * bounds tables.
 */
class VitalsTest {

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
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        Stat.resetForTests();
    }

    @Test
    void vitalsVectors() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);

        int hp = Critter.critterGetHitPoints(dogSlot[0]);
        int poison = Critter.critterGetPoison(dogSlot[0]);
        int rad = Critter.critterGetRadiation(dogSlot[0]);
        int stimHp = Critter.critterGetHitPoints(stimSlot[0]);
        int adj = Critter.critterAdjustHitPoints(dogSlot[0], 5);
        int hp1 = Critter.critterGetHitPoints(dogSlot[0]);
        int poisonRc = Critter.critterAdjustPoison(dogSlot[0], 5);
        int radRc = Critter.critterAdjustRadiation(dogSlot[0], 5);

        assertEquals(0, hp);
        assertEquals(0, poison);
        assertEquals(0, rad);
        assertEquals(0, stimHp);
        assertEquals(0, adj);
        assertEquals(0, hp1);
        assertEquals(-1, poisonRc);
        assertEquals(-1, radRc);

        assertEquals("hp=0 poison=0 rad=0 stimhp=0 adj=0 hp1=0 poisonrc=-1 radrc=-1",
                "hp=" + hp + " poison=" + poison + " rad=" + rad + " stimhp=" + stimHp
                        + " adj=" + adj + " hp1=" + hp1 + " poisonrc=" + poisonRc
                        + " radrc=" + radRc);
    }
}
