package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * {@code [mapstart]} vectors from
 * {@code docker/refharness/golden/v101.txt}, produced by
 * {@code sectionMapStart} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>{@link Map#mapSetStart(int,int,Rotation)} stores the entering
 * tile/elevation/rotation verbatim in the map header; {@link Map#mapGetCurrentMap()}
 * returns the header map index ({@code 0} = {@code MAP_RND_DESERT_1} in a fresh
 * child, mirroring C++ {@code MAP_INVALID}→id 0 read-back).
 *
 * <p>Pure slice: no mounts, no window system. The test pins the header
 * fields and the current-map read-back, exactly like the C vectors.
 */
class MapStartTest {

    @Test
    void mapStartVectors() {
        // start=20100,0,0 current=0
        Map.mapSetStart(20100, 0, Rotation.NE);
        assertEquals("start=20100,0,0 current=0",
            "start=" + Map.gMapHeader.enteringTile
                    + "," + Map.gMapHeader.enteringElevation
                    + "," + Map.gMapHeader.enteringRotation.value()
                    + " current=" + Map.mapGetCurrentMap());
    }
}
