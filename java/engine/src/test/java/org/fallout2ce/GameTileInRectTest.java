package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The tile-in-rectangle builtin of the game.
 *
 * <p>The numbers are the ones the reference harness prints
 * ({@code [tileinrect]} in the golden vectors): the rectangle the tiles
 * 2020 (20,10) and 4010 (10,20) bound holds (15,15) and both its corners,
 * but not (21,15), (15,21) or (9,9); a tile of -1 and a tile far off the
 * grid are outside too. The second and third tiles are dropped: other
 * corners there change nothing. Swapped corners bound an empty range.
 */
class GameTileInRectTest {

    private GameScriptApi api;

    @BeforeEach
    void setup() {
        api = new GameScriptApi();
    }

    @Test
    void theTilesInsideTheRectangle() {
        assertEquals(1, api.tileInTileRect(2020, 0, 0, 4010, 3015));
        assertEquals(1, api.tileInTileRect(2020, 0, 0, 4010, 2010));
        assertEquals(1, api.tileInTileRect(2020, 0, 0, 4010, 4020));
    }

    @Test
    void theTilesOutsideTheRectangle() {
        assertEquals(0, api.tileInTileRect(2020, 0, 0, 4010, 3021));
        assertEquals(0, api.tileInTileRect(2020, 0, 0, 4010, 4215));
        assertEquals(0, api.tileInTileRect(2020, 0, 0, 4010, 1809));
        assertEquals(0, api.tileInTileRect(2020, 0, 0, 4010, -1));
        assertEquals(0, api.tileInTileRect(2020, 0, 0, 4010, 40000));
    }

    @Test
    void theDroppedCornersAndTheSwappedOnes() {
        assertEquals(1, api.tileInTileRect(2020, 1999, 3999, 4010, 3015));
        assertEquals(0, api.tileInTileRect(4010, 0, 0, 2020, 3015));
    }
}
