package org.fallout2ce;

/**
 * Entering-location state slice, mirroring {@code src/map.cc}: the
 * {@code gEnteringElevation} / {@code gEnteringTile} /
 * {@code gEnteringRotation} statics (0x519558 / 0x51955C / 0x519560) and
 * their setter {@code mapSetEnteringLocation} (0x481ED4).
 *
 * <p>Vectors: {@code [isorest]} in
 * {@code docker/refharness/golden/v97.txt}, produced by
 * {@code sectionIsoRest} in {@code docker/refharness/ref_main.cc}:
 * {@code mapSetEnteringLocation(-1, -1, ROTATION_INVALID)} returns
 * {@code enter=0}. There is no validation in C - every triple is stored
 * verbatim and 0 is returned - and Java keeps that: the
 * {@code (-1, -1, Invalid)} triple is the sentinel the map load path
 * tests ({@code if (gEnteringElevation == -1)} takes the header's entering
 * spot), not an error.
 *
 * <p>Holding state: {@link MapVars} owns the map <em>variable</em> domain
 * and {@link TileMath} the tile-scroll domain, so the entering location
 * (read by the map load/elevation/tile-center path that lands with the map
 * phase) gets its own small holder, following the {@link MapEdge} precedent
 * for a narrow {@code map.cc} slice.
 */
public final class EnteringLocation {

    private EnteringLocation() {
    }

    /** 0x519558 mapEntranceElevation (BSS 0). */
    private static int enteringElevation = 0;

    /** 0x51955C mapEntranceTileNum (BSS -1). */
    private static int enteringTile = -1;

    /** 0x519560 mapEntranceRotation (BSS ROTATION_NE). */
    private static Rotation enteringRotation = Rotation.NE;

    /** Port of {@code mapSetEnteringLocation} (0x481ED4): stores verbatim. */
    public static int mapSetEnteringLocation(int elevation, int tile, Rotation rotation) {
        enteringElevation = elevation;
        enteringTile = tile;
        enteringRotation = rotation;
        return 0;
    }

    /** {@code gEnteringElevation} read side. */
    public static int getEnteringElevation() {
        return enteringElevation;
    }

    /** {@code gEnteringTile} read side. */
    public static int getEnteringTile() {
        return enteringTile;
    }

    /** {@code gEnteringRotation} read side. */
    public static Rotation getEnteringRotation() {
        return enteringRotation;
    }

    /** Test seam: fresh-process mirror (elevation 0, tile -1, rotation NE). */
    static void resetForTests() {
        enteringElevation = 0;
        enteringTile = -1;
        enteringRotation = Rotation.NE;
    }
}