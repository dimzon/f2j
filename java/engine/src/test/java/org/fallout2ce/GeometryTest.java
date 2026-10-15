package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Vectors cross-checked against {@code docker/refharness/golden/v1.txt}.
 */
class GeometryTest {

    private static Geometry.Rect rect(int l, int t, int r, int b) {
        return new Geometry.Rect(l, t, r, b);
    }

    private static List<Geometry.Rect> drain(Geometry.RectListNode head) {
        List<Geometry.Rect> out = new ArrayList<>();
        for (Geometry.RectListNode n = head; n != null; n = n.next) {
            out.add(new Geometry.Rect(n.rect));
        }
        return out;
    }

    @Test
    void rectUnion() {
        Geometry.Rect r = new Geometry.Rect();
        Geometry.rectUnion(rect(0, 0, 10, 10), rect(5, 5, 15, 15), r);
        assertEquals(rect(0, 0, 15, 15), r);
        Geometry.rectUnion(rect(-5, -5, 0, 0), rect(10, 10, 20, 20), r);
        assertEquals(rect(-5, -5, 20, 20), r);
    }

    @Test
    void rectIntersectionOverlap() {
        Geometry.Rect r = new Geometry.Rect();
        assertEquals(0, Geometry.rectIntersection(rect(0, 0, 10, 10), rect(5, 5, 15, 15), r));
        assertEquals(rect(5, 5, 10, 10), r);
    }

    @Test
    void rectIntersectionDisjointKeepsS1() {
        Geometry.Rect r = new Geometry.Rect();
        assertEquals(-1, Geometry.rectIntersection(rect(0, 0, 10, 10), rect(20, 20, 30, 30), r));
        assertEquals(rect(0, 0, 10, 10), r);
    }

    @Test
    void rectIntersectionEdgeTouching() {
        Geometry.Rect r = new Geometry.Rect();
        assertEquals(0, Geometry.rectIntersection(rect(0, 0, 10, 10), rect(10, 10, 20, 20), r));
        assertEquals(rect(10, 10, 10, 10), r);
    }

    @Test
    void rectClipHoleYieldsFourSurrounds() {
        List<Geometry.Rect> got = drain(Geometry.rectClip(rect(0, 0, 20, 20), rect(5, 5, 10, 10)));
        assertEquals(List.of(
                rect(0, 0, 20, 4),
                rect(0, 5, 4, 10),
                rect(11, 5, 20, 10),
                rect(0, 11, 20, 20)), got);
    }

    @Test
    void rectClipMissReturnsB() {
        List<Geometry.Rect> got = drain(Geometry.rectClip(rect(0, 0, 20, 20), rect(30, 30, 40, 40)));
        assertEquals(List.of(rect(0, 0, 20, 20)), got);
    }

    @Test
    void rectClipListSubtractsInPrependOrder() {
        // C prepends each fragment at the unlink position, so the final
        // order is the reverse of creation: right, left, bottom, top.
        Geometry.RectListNode node = Geometry.rectMalloc();
        Geometry.copyTo(node.rect, rect(0, 0, 10, 10));
        node.next = null;

        List<Geometry.Rect> got = drain(Geometry.rectClipList(node, rect(3, 3, 5, 5)));
        assertEquals(List.of(
                rect(6, 3, 10, 5),
                rect(0, 3, 2, 5),
                rect(0, 6, 10, 10),
                rect(0, 0, 10, 2)), got);
    }

    @Test
    void rectClipListKeepsDisjointNodes() {
        Geometry.RectListNode n1 = Geometry.rectMalloc();
        Geometry.copyTo(n1.rect, rect(0, 0, 4, 4));
        Geometry.RectListNode n2 = Geometry.rectMalloc();
        Geometry.copyTo(n2.rect, rect(50, 50, 60, 60));
        n1.next = n2;
        n2.next = null;

        List<Geometry.Rect> got = drain(Geometry.rectClipList(n1, rect(10, 10, 20, 20)));
        assertEquals(List.of(rect(0, 0, 4, 4), rect(50, 50, 60, 60)), got);
    }

    @Test
    void poolRecyclesNodes() {
        Geometry.RectListNode a = Geometry.rectMalloc();
        Geometry.rectFree(a);
        assertSame(a, Geometry.rectMalloc());
        Geometry.gnwRectExit();
    }

    @Test
    void widthHeightOffsetCopy() {
        Geometry.Rect r = rect(2, 3, 5, 9);
        assertEquals(4, Geometry.rectGetWidth(r));
        assertEquals(7, Geometry.rectGetHeight(r));
        Geometry.rectOffset(r, 1, -1);
        assertEquals(rect(3, 2, 6, 8), r);
        Geometry.Rect c = new Geometry.Rect();
        Geometry.copyTo(c, r);
        assertEquals(r, c);
    }
}
