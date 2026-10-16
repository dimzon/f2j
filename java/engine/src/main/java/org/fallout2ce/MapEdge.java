package org.fallout2ce;

/**
 * Map-edge (EDG) state slice, mirroring {@code src/map_edge.cc} /
 * {@code src/map_edge.h}: the enabled gate, the selected-zone flag, and
 * the boundary tile alignments.
 *
 * <p>Only the harness-vectored surface is ported ({@code [miscgetters]} in
 * {@code docker/refharness/golden/v81.txt}, produced by
 * {@code sectionMiscGetters} in {@code docker/refharness/ref_main.cc}):
 * {@code mapEdgeIsEnabled}, {@code mapEdgeZoneIsSelected},
 * {@code mapEdgeGetTileXAlignment}, {@code mapEdgeGetTileYAlignment},
 * plus {@code mapEdgeFree} (the state clear the load path runs first).
 *
 * <p>The gate reuses the live {@link Settings} values
 * ({@code ui.edg_support}, {@code ui.ignore_map_edges}) instead of
 * hardcoding the headless outcome. Java defaults match the C++ ones
 * ({@code edg_support=true}, {@code ignore_map_edges=false}, see
 * {@code src/settings.h}), so the vector ({@code edge=0}) falls out of
 * the unloaded state ({@code edgeDataLoaded=false}) exactly as in the
 * headless harness, and a future map phase that loads EDG data gets the
 * real gate for free.
 *
 * <p>{@link TileMath} keeps its own private constant seams
 * ({@code mapEdgeIsEnabled()} false, zero alignments) and is untouched:
 * those match the same fresh-process defaults and need no live edge
 * state until the map phase.
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>{@code currentEdgeZone} is only a selected-flag holder
 * ({@code null} = unselected): the full {@code EdgeZone} geometry
 * (tileRect/pixelRect/scrollBorderRect) and the EDG load/save/select/
 * clamp/visible-area paths arrive with the map phase.</li>
 * <li>{@code _square_roof_intersect} is deliberately NOT ported: it
 * needs loaded roof grids and segfaults headless (see the
 * {@code sectionMiscGetters} NOTE); it arrives with the map phase.</li>
 * <li>{@code mapEdgeFree} mirrors C exactly: it clears the loaded flag,
 * the zone, and the current alignments, but not {@code mapperMode}
 * (C leaves it set); {@link #resetForTests} clears that too.</li>
 * </ul>
 */
public final class MapEdge {

    private MapEdge() {
    }

    /** Port of {@code edgeDataLoaded} (BSS false). */
    private static boolean edgeDataLoaded;

    /** Port of {@code mapperMode} (BSS false). */
    private static boolean mapperMode;

    /**
     * Port of {@code currentEdgeZone} (BSS null): zone geometry is
     * unported, so only the selected flag ({@code != null}) is modelled.
     */
    private static Object currentEdgeZone;

    /** Port of {@code currentTileXAlignment} (BSS 0). */
    private static int currentTileXAlignment;

    /** Port of {@code currentTileYAlignment} (BSS 0). */
    private static int currentTileYAlignment;

    /**
     * Port of {@code mapEdgeIsEnabled} (src/map_edge.cc): data loaded,
     * not in mapper mode, and the user enabled edge support without
     * ignoring map edges. Headless all but the loaded flag read true,
     * so the gate is false.
     */
    public static boolean mapEdgeIsEnabled() {
        return edgeDataLoaded && !mapperMode
                && Settings.settings.ui.edg_support
                && !Settings.settings.ui.ignore_map_edges;
    }

    /** Port of {@code mapEdgeZoneIsSelected}: null zone headless. */
    public static boolean mapEdgeZoneIsSelected() {
        return currentEdgeZone != null;
    }

    /** Port of {@code mapEdgeGetTileXAlignment} (BSS 0 headless). */
    public static int mapEdgeGetTileXAlignment() {
        return currentTileXAlignment;
    }

    /** Port of {@code mapEdgeGetTileYAlignment} (BSS 0 headless). */
    public static int mapEdgeGetTileYAlignment() {
        return currentTileYAlignment;
    }

    /** Port of {@code mapEdgeFree}: clears loaded state (not mapper mode). */
    public static void mapEdgeFree() {
        edgeDataLoaded = false;
        currentEdgeZone = null;
        currentTileXAlignment = 0;
        currentTileYAlignment = 0;
    }

    static void resetForTests() {
        mapEdgeFree();
        mapperMode = false;
    }
}
