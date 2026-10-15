package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.fallout2ce.MapDefs.MapHeaderFlags;

/**
 * {@code [gamedefs]} map vectors from
 * {@code docker/refharness/golden/v29.txt}.
 */
class MapDefsTest {

    @Test
    void gridSizes() {
        assertEquals(3, MapDefs.ELEVATION_COUNT);
        assertEquals(10000, MapDefs.SQUARE_GRID_SIZE);
        assertEquals(100 * 100, MapDefs.SQUARE_GRID_SIZE);
        assertEquals(40000, MapDefs.HEX_GRID_SIZE);
        assertEquals(200 * 200, MapDefs.HEX_GRID_SIZE);
    }

    @Test
    void validators() {
        assertTrue(MapDefs.elevationIsValid(0));
        assertTrue(MapDefs.elevationIsValid(2));
        assertFalse(MapDefs.elevationIsValid(3));
        assertFalse(MapDefs.elevationIsValid(-1));
        assertTrue(MapDefs.squareGridTileIsValid(0));
        assertTrue(MapDefs.squareGridTileIsValid(9999));
        assertFalse(MapDefs.squareGridTileIsValid(10000));
        assertTrue(MapDefs.hexGridTileIsValid(39999));
        assertFalse(MapDefs.hexGridTileIsValid(40000));
    }

    @Test
    void flagOps() {
        // flags = SAVED; flags |= ELEVATION_0; flags |= ELEVATION_1;
        // -> flags-or=7.
        MapHeaderFlags flags = MapHeaderFlags.SAVED
                .or_(MapHeaderFlags.ELEVATION_0)
                .or_(MapHeaderFlags.ELEVATION_1);
        assertEquals(7, flags.bits());

        // flags &= ELEVATION_1; -> flags-and=4.
        assertEquals(4, flags.and_(MapHeaderFlags.ELEVATION_1).bits());

        // ~SAVED -> not-saved=-2.
        assertEquals(-2, MapHeaderFlags.SAVED.not().bits());
    }
}
