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
 * {@code [miscgetters]} vectors from
 * {@code docker/refharness/golden/v81.txt}, produced by
 * {@code sectionMiscGetters} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Setup mirrors {@link CritterStatsTest} (game data for the dog/stim
 * {@code .pro} loads). The edge line needs no game data: it pins the
 * unloaded map-edge state (BSS false/null/zero in C++, fresh-process
 * defaults in {@link MapEdge}). The gate reuses the live
 * {@link Settings} values, whose Java defaults match the C++ ones
 * ({@code edg_support=true}, {@code ignore_map_edges=false}), so
 * {@code edge=0} falls out of the unloaded flag exactly as headless C++.
 *
 * <p>The movement line pins the uncrippled dog ({@code movecost=5}),
 * the non-critter stim ({@code bad=0}), and the unladen dog
 * ({@code encumbered=0}: capacity clamps to 0 headless and the fresh
 * inventory weighs 0). {@code _square_roof_intersect} is not covered:
 * it needs loaded roof grids and segfaults headless (see the
 * {@code sectionMiscGetters} NOTE and {@link MapEdge}).
 *
 * <p>Write-mutation policy: the test mirrors the harness exactly and
 * performs no stores, so the shared {@link ProtoDb} cache is never
 * polluted for other tests.
 */
class MiscGettersTest {

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
        MapEdge.resetForTests();
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
        MapEdge.resetForTests();
    }

    @Test
    void miscGettersVectors() {
        int edgeOn = MapEdge.mapEdgeIsEnabled() ? 1 : 0;
        int zoneSel = MapEdge.mapEdgeZoneIsSelected() ? 1 : 0;
        int alignX = MapEdge.mapEdgeGetTileXAlignment();
        int alignY = MapEdge.mapEdgeGetTileYAlignment();
        String edgeLine = String.format("edge=%d zone=%d align=%d,%d",
                edgeOn, zoneSel, alignX, alignY);
        assertEquals("edge=0 zone=0 align=0,0", edgeLine);

        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);
        GameObject stim = stimSlot[0];

        int moveCost = Critter.critterGetMovementPointCostAdjustedForCrippledLegs(dog, 5);
        int moveCostBad = Critter.critterGetMovementPointCostAdjustedForCrippledLegs(stim, 5);
        int encumbered = Critter.critterIsEncumbered(dog) ? 1 : 0;
        String moveLine = String.format("movecost=%d bad=%d encumbered=%d",
                moveCost, moveCostBad, encumbered);
        assertEquals("movecost=5 bad=0 encumbered=0", moveLine);
    }
}
