package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * {@code [maptransition]} vectors from
 * {@code docker/refharness/golden/v99.txt}, produced by
 * {@code sectionMapTransition} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>{@link Map#mapSetTransition(Map.MapTransition)} copies the transition
 * fields, normalizes {@code MAP_FIRST} (alias of {@code MAP_RND_DESERT_1},
 * id {@code 0}) to {@code MAP_TRANSITION} (id {@code -2}), and keeps the
 * combat-quit tail dormant headless (cf. {@link Stat}'s {@code isInCombat}
 * seam). A null transition returns {@code -1}. {@code MAP_INVALID} ({@code
 * -1}) is not normalized.
 *
 * <p>Pure slice: no init, no mounts, no render/tile state - the test pins
 * the return code plus the normalized map/tile/elevation/rotation read-back.
 */
class MapTransitionTest {

    @Test
    void mapTransitionVectors() {
        // MAP_RND_DESERT_1 (id 0) normalizes to MAP_TRANSITION (id -2).
        Map.MapTransition t1 = new Map.MapTransition();
        t1.map = WorldmapDefs.Map.MAP_RND_DESERT_1;
        t1.tile = 5;
        t1.elevation = 1;
        t1.rotation = Rotation.NE;
        int rc1 = Map.mapSetTransition(t1);
        assertEquals("rc=0 map=0 tile=5 elev=1 rot=0",
            "rc=" + rc1 + " map=" + t1.map.id() + " tile=" + t1.tile
                    + " elev=" + t1.elevation + " rot=" + t1.rotation.value());

        // MAP_INVALID (-1) passes through verbatim.
        Map.MapTransition t2 = new Map.MapTransition();
        t2.map = WorldmapDefs.Map.MAP_INVALID;
        t2.tile = -1;
        t2.elevation = 0;
        t2.rotation = Rotation.INVALID;
        int rc2 = Map.mapSetTransition(t2);
        assertEquals("rc=0 map=-1 tile=-1 elev=0 rot=-1",
            "rc=" + rc2 + " map=" + t2.map.id() + " tile=" + t2.tile
                    + " elev=" + t2.elevation + " rot=" + t2.rotation.value());

        // MAP_TRANSITION (-2) is already normalized; stored verbatim.
        Map.MapTransition t3 = new Map.MapTransition();
        t3.map = WorldmapDefs.Map.MAP_TRANSITION;
        t3.tile = 0;
        t3.elevation = 0;
        t3.rotation = Rotation.NE;
        int rc3 = Map.mapSetTransition(t3);
        assertEquals("rc=0 map=-2 tile=0 elev=0 rot=0",
            "rc=" + rc3 + " map=" + t3.map.id() + " tile=" + t3.tile
                    + " elev=" + t3.elevation + " rot=" + t3.rotation.value());

        // Null transition -> -1, like the C null guard.
        assertEquals(-1, Map.mapSetTransition(null));
    }
}
