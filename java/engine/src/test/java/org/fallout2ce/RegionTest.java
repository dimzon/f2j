package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Vectors from {@code docker/refharness/golden/v20.txt} ({@code [region]}).
 * Pure geometry: no game data, no video.
 */
class RegionTest {

    @Test
    void createDefaults() {
        Region square = Region.regionCreate(4);

        assertEquals("", Region.regionGetName(square));
        assertEquals(0, square.flags);
        assertEquals(Integer.MIN_VALUE, square.minX);
        assertEquals(Integer.MIN_VALUE, square.minY);
        assertEquals(Integer.MAX_VALUE, square.maxX);
        assertEquals(Integer.MAX_VALUE, square.maxY);
        assertEquals(0, square.centroidX);
        assertEquals(0, square.centroidY);
        Region.regionDelete(square);
    }

    @Test
    void squareBoundsAndContains() {
        Region square = Region.regionCreate(4);
        Region.regionAddPoint(square, 0, 0);
        Region.regionAddPoint(square, 10, 0);
        Region.regionAddPoint(square, 10, 10);
        Region.regionAddPoint(square, 0, 10);
        Region.regionSetBound(square);

        assertEquals(0, square.minX);
        assertEquals(0, square.minY);
        assertEquals(10, square.maxX);
        assertEquals(10, square.maxY);
        assertEquals(5, square.centroidX);
        assertEquals(5, square.centroidY);

        assertTrue(Region.regionContainsPoint(square, 5, 5));
        assertTrue(Region.regionContainsPoint(square, 0, 0));
        assertTrue(Region.regionContainsPoint(square, 5, 0));
        assertFalse(Region.regionContainsPoint(square, 15, 5));
        assertFalse(Region.regionContainsPoint(square, -1, -1));
        assertFalse(Region.regionContainsPoint(null, 5, 5));
        Region.regionDelete(square);
    }

    @Test
    void triangle() {
        // Grows from the null-points path (initial capacity 0).
        Region tri = Region.regionCreate(0);
        Region.regionAddPoint(tri, 0, 0);
        Region.regionAddPoint(tri, 8, 0);
        Region.regionAddPoint(tri, 0, 6);
        Region.regionSetBound(tri);

        assertEquals(0, tri.minX);
        assertEquals(0, tri.minY);
        assertEquals(8, tri.maxX);
        assertEquals(6, tri.maxY);
        assertEquals(2, tri.centroidX);
        assertEquals(2, tri.centroidY);
        assertTrue(Region.regionContainsPoint(tri, 1, 1));
        assertFalse(Region.regionContainsPoint(tri, 7, 5));
        Region.regionDelete(tri);
    }

    @Test
    void emptyContainsNothing() {
        Region empty = Region.regionCreate(0);
        assertFalse(Region.regionContainsPoint(empty, 0, 0));
        Region.regionDelete(empty);
    }

    @Test
    void names() {
        Region region = Region.regionCreate(1);

        Region.regionSetName(region, "hello");
        assertEquals("hello", Region.regionGetName(region));

        Region.regionSetName(region, null);
        assertEquals("", Region.regionGetName(region));

        Region.regionSetName(region, "0123456789abcdef0123456789abcdefXYZ");
        assertEquals("0123456789abcdef0123456789abcde", Region.regionGetName(region));

        assertEquals("<null>", Region.regionGetName(null));
        Region.regionDelete(region);
    }

    @Test
    void userDataRoundTrip() {
        Region region = Region.regionCreate(1);

        assertNull(Region.regionGetUserData(region));
        Object tag = new Object();
        Region.regionSetUserData(region, tag);
        assertTrue(Region.regionGetUserData(region) == tag);
        assertNull(Region.regionGetUserData(null));
        Region.regionSetUserData(null, tag);
        Region.regionDelete(region);
    }

    @Test
    void flags() {
        Region region = Region.regionCreate(1);
        Region.regionAddFlag(region, 1);
        Region.regionAddFlag(region, 4);
        assertEquals(5, region.flags);
        Region.regionDelete(region);
    }

    @Test
    void nullGuards() {
        // Debug-logging no-ops (must not throw).
        Region.regionAddPoint(null, 1, 2);
        Region.regionDelete(null);
    }
}
