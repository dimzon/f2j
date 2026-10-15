package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of {@code src/region.h} / {@code src/region.cc} (mouse regions:
 * named polygons with bounds, centroid, flags and callbacks).
 *
 * <p>{@code Program*} stays an untyped reference until the interpreter
 * lands; callbacks are plain functional interfaces. The closing duplicate
 * point C maintains at {@code points[length]} is implicit here (winding
 * wraps with modulo) — it is not observable through the API.
 */
public final class Region {

    public static final int NAME_LENGTH = 32;

    /** Mouse-event callback (C {@code RegionMouseEventCallback}). */
    public interface MouseEventCallback {
        void onEvent(Region region, Object userData, int event);
    }

    /** Port of {@code struct Region} (mutable, like {@code Geometry.Rect}). */
    public String name = "";
    public final List<Geometry.Point> points = new ArrayList<>();
    public int minX = Integer.MIN_VALUE;
    public int minY = Integer.MIN_VALUE;
    public int maxX = Integer.MAX_VALUE;
    public int maxY = Integer.MAX_VALUE;
    public int centroidX;
    public int centroidY;
    public Object program;
    public final int[] procs = new int[4];
    public final int[] rightProcs = new int[4];
    public int field68;
    public int mouseInside;
    public int field70;
    public int flags;
    public MouseEventCallback mouseEventCallback;
    public MouseEventCallback rightMouseEventCallback;
    public Object mouseEventCallbackUserData;
    public Object rightMouseEventCallbackUserData;
    public Object userData;

    /** Port of {@code _regionSetBound}. */
    public static void regionSetBound(Region region) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int pointCount = 0;
        int sumX = 0;
        int sumY = 0;

        for (int index = 0; index < region.points.size(); index++) {
            Geometry.Point point = region.points.get(index);
            if (minX >= point.x()) {
                minX = point.x();
            }
            if (minY >= point.y()) {
                minY = point.y();
            }
            if (maxX <= point.x()) {
                maxX = point.x();
            }
            if (maxY <= point.y()) {
                maxY = point.y();
            }
            sumX += point.x();
            sumY += point.y();
            pointCount++;
        }

        region.minY = minY;
        region.maxX = maxX;
        region.maxY = maxY;
        region.minX = minX;

        if (pointCount != 0) {
            region.centroidX = sumX / pointCount;
            region.centroidY = sumY / pointCount;
        }
    }

    /** Port of {@code regionContainsPoint} (missing from region.h upstream). */
    public static boolean regionContainsPoint(Region region, int x, int y) {
        if (region == null) {
            return false;
        }

        if (region.points.isEmpty()) {
            // NOTE: C dereferences a null/uninitialized point (crash or
            // heap garbage); an empty region contains nothing instead.
            return false;
        }

        if (x < region.minX || x > region.maxX || y < region.minY || y > region.maxY) {
            return false;
        }

        int previousQuadrant;

        Geometry.Point prev = region.points.get(0);
        if (x >= prev.x()) {
            if (y >= prev.y()) {
                previousQuadrant = 2;
            } else {
                previousQuadrant = 1;
            }
        } else {
            if (y >= prev.y()) {
                previousQuadrant = 3;
            } else {
                previousQuadrant = 0;
            }
        }

        int winding = 0;
        int count = region.points.size();
        for (int index = 0; index < count; index++) {
            int quadrant;

            Geometry.Point point = region.points.get((index + 1) % count);
            if (x >= point.x()) {
                if (y >= point.y()) {
                    quadrant = 2;
                } else {
                    quadrant = 1;
                }
            } else {
                if (y >= point.y()) {
                    quadrant = 3;
                } else {
                    quadrant = 0;
                }
            }

            int quadrantDelta = quadrant - previousQuadrant;
            switch (quadrantDelta) {
                case -3:
                    quadrantDelta = 1;
                    break;
                case -2:
                case 2:
                    // NOTE: opposite quadrants imply different y-sides, so
                    // (prev.y - point.y) is never zero here (same as C,
                    // where x86 doubles divide without trapping anyway).
                    if ((double) x < (double) point.x()
                            - (double) (prev.x() - point.x()) / (double) (prev.y() - point.y())
                                    * (double) (point.y() - y)) {
                        quadrantDelta = -quadrantDelta;
                    }
                    break;
                case 3:
                    quadrantDelta = -1;
                    break;
                default:
                    break;
            }

            prev = point;
            previousQuadrant = quadrant;

            winding += quadrantDelta;
        }

        if (winding == 4 || winding == -4) {
            return true;
        }

        return false;
    }

    /** Port of {@code regionCreate} (growth policy is an ArrayList detail). */
    public static Region regionCreate(int initialCapacity) {
        Region region = new Region();
        region.name = "";
        region.flags = 0;
        region.minY = Integer.MIN_VALUE;
        region.maxY = Integer.MAX_VALUE;
        region.minX = Integer.MIN_VALUE;
        region.maxX = Integer.MAX_VALUE;
        return region;
    }

    /** Port of {@code regionAddPoint}. */
    public static void regionAddPoint(Region region, int x, int y) {
        if (region == null) {
            Debug.print("regionAddPoint(): null region ptr\n");
            return;
        }

        region.points.add(new Geometry.Point(x, y));
    }

    /** Port of {@code regionDelete} (GC owns the memory; null-guarded). */
    public static void regionDelete(Region region) {
        if (region == null) {
            Debug.print("regionDelete(): null region ptr\n");
        }
    }

    /** Port of {@code regionSetName} (truncated to 31 chars, like strncpy). */
    public static void regionSetName(Region region, String name) {
        if (region == null) {
            Debug.print("regionAddName(): null region ptr\n");
            return;
        }

        if (name == null) {
            region.name = "";
            return;
        }

        // NOTE: C strncpy caps at 31 chars without forcing a terminator.
        region.name = name.length() > NAME_LENGTH - 1 ? name.substring(0, NAME_LENGTH - 1) : name;
    }

    /** Port of {@code regionGetName} ({@code "<null>"} for null). */
    public static String regionGetName(Region region) {
        if (region == null) {
            Debug.print("regionGetName(): null region ptr\n");
            return "<null>";
        }

        return region.name;
    }

    /** Port of {@code regionGetUserData}. */
    public static Object regionGetUserData(Region region) {
        if (region == null) {
            Debug.print("regionGetUserData(): null region ptr\n");
            return null;
        }

        return region.userData;
    }

    /** Port of {@code regionSetUserData}. */
    public static void regionSetUserData(Region region, Object data) {
        if (region == null) {
            Debug.print("regionSetUserData(): null region ptr\n");
            return;
        }

        region.userData = data;
    }

    /** Port of {@code regionAddFlag} (no null guard in C either). */
    public static void regionAddFlag(Region region, int value) {
        region.flags |= value;
    }
}
