package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.fallout2ce.WorldmapDefs.Map;

/**
 * {@code [objtypes]} map vectors from
 * {@code docker/refharness/golden/v33.txt}, plus a density check over the
 * whole table (transcribed from {@code src/worldmap.h}).
 */
class WorldmapDefsTest {

    @Test
    void sentinels() {
        assertEquals(-2, Map.MAP_TRANSITION.id());
        assertEquals(-1, Map.MAP_INVALID.id());
        assertEquals(0, WorldmapDefs.MAP_FIRST);
        assertEquals(0, Map.MAP_RND_DESERT_1.id());
        assertEquals(126, Map.MAP_ARROYO_TEMPLE.id());
        assertEquals(149, Map.MAP_IN_GAME_MOVIE1.id());
    }

    @Test
    void tableIsDense() {
        assertEquals(152, Map.values().length);
        for (int id = 0; id <= 149; id++) {
            Map map = WorldmapDefs.ofMap(id);
            assertTrue(map != null, "missing map id " + id);
            assertEquals(id, map.id());
        }
        assertEquals(Map.MAP_TRANSITION, WorldmapDefs.ofMap(-2));
        assertEquals(Map.MAP_INVALID, WorldmapDefs.ofMap(-1));
        assertNull(WorldmapDefs.ofMap(-3));
        assertNull(WorldmapDefs.ofMap(150));
    }

    @Test
    void postIncrementEquivalent() {
        // C++ `Map mp = MAP_IN_GAME_MOVIE1; Map old = mp++;`
        // -> post-old=149 post-new=150 (past the table).
        assertEquals(Map.MAP_IN_GAME_MOVIE1, WorldmapDefs.ofMap(149));
        assertNull(WorldmapDefs.ofMap(149).next());
    }
}
