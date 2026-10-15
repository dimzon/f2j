package org.fallout2ce;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Port of {@code src/geometry.h} / {@code src/geometry.cc}.
 *
 * <p>Conventions: C structs stay mutable classes with public fields
 * ({@link Rect}); out-params are passed in by the caller, return codes
 * ({@code 0}/{@code -1}) are preserved. The node pool ({@code _rectList})
 * is an unbounded free list — the C code preallocates 10 nodes at a time
 * from the heap, which is behaviorally identical.
 */
public final class Geometry {

    private Geometry() {
    }

    /** Port of {@code Point}. */
    public record Point(int x, int y) {
    }

    /** Port of {@code Size}. */
    public record Size(int width, int height) {
    }

    /** Port of {@code Rect} (mutable: C code writes into it in place). */
    public static final class Rect {
        public int left;
        public int top;
        public int right;
        public int bottom;

        public Rect() {
        }

        public Rect(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public Rect(Rect other) {
            copyTo(this, other);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Rect r)) {
                return false;
            }
            return left == r.left && top == r.top && right == r.right && bottom == r.bottom;
        }

        @Override
        public int hashCode() {
            int h = left;
            h = 31 * h + top;
            h = 31 * h + right;
            h = 31 * h + bottom;
            return h;
        }

        @Override
        public String toString() {
            return "Rect(" + left + "," + top + "-" + right + "," + bottom + ")";
        }
    }

    /** Port of {@code RectListNode} (singly linked via {@link #next}). */
    public static final class RectListNode {
        public final Rect rect = new Rect();
        public RectListNode next;
    }

    // 0x51DEF4 rectList
    private static final Deque<RectListNode> freeList = new ArrayDeque<>();

    // 0x4C6900 GNW_rect_exit
    public static void gnwRectExit() {
        freeList.clear();
    }

    /**
     * Port of {@code _rect_clip_list}. Subtracts {@code clip} from every
     * overlapping node of the list headed by {@code head} and returns the
     * new head. (C takes a {@code RectListNode**}; Java returns the head.)
     */
    public static RectListNode rectClipList(RectListNode head, Rect clip) {
        Rect clipRect = new Rect(clip);

        // Simulates the C double-pointer with explicit prev linkage.
        RectListNode newHead = head;
        RectListNode prev = null;
        RectListNode current = head;
        while (current != null) {
            RectListNode node = current;
            current = current.next;
            if (clipRect.right >= node.rect.left
                    && clipRect.bottom >= node.rect.top
                    && clipRect.left <= node.rect.right
                    && clipRect.top <= node.rect.bottom) {
                Rect nodeRect = new Rect(node.rect);

                // Unlink + recycle (mirrors _rect_free into _rectList).
                if (prev == null) {
                    newHead = current;
                } else {
                    prev.next = current;
                }
                node.next = null;
                freeList.push(node);

                // Tail-inserted fragments; `tail` tracks the C `rectListNodePtr`.
                // NOTE: each fragment is PREPENDED at the unlink position
                // (frag->next = *ptr; *ptr = frag), so the final order is
                // the reverse of creation: right, left, bottom, top.
                RectListNode fragHead = null;
                RectListNode fragTail = null;

                if (nodeRect.top < clipRect.top) {
                    RectListNode frag = rectMalloc();
                    copyTo(frag.rect, nodeRect);
                    frag.rect.bottom = clipRect.top - 1;
                    frag.next = fragHead;
                    fragHead = frag;
                    if (fragTail == null) {
                        fragTail = frag;
                    }
                    nodeRect.top = clipRect.top;
                }

                if (nodeRect.bottom > clipRect.bottom) {
                    RectListNode frag = rectMalloc();
                    copyTo(frag.rect, nodeRect);
                    frag.rect.top = clipRect.bottom + 1;
                    frag.next = fragHead;
                    fragHead = frag;
                    if (fragTail == null) {
                        fragTail = frag;
                    }
                    nodeRect.bottom = clipRect.bottom;
                }

                if (nodeRect.left < clipRect.left) {
                    RectListNode frag = rectMalloc();
                    copyTo(frag.rect, nodeRect);
                    frag.rect.right = clipRect.left - 1;
                    frag.next = fragHead;
                    fragHead = frag;
                    if (fragTail == null) {
                        fragTail = frag;
                    }
                }

                if (nodeRect.right > clipRect.right) {
                    RectListNode frag = rectMalloc();
                    copyTo(frag.rect, nodeRect);
                    frag.rect.left = clipRect.right + 1;
                    frag.next = fragHead;
                    fragHead = frag;
                    if (fragTail == null) {
                        fragTail = frag;
                    }
                }

                if (fragHead != null) {
                    // Splice fragments at the unlink position.
                    if (prev == null) {
                        newHead = fragHead;
                    } else {
                        prev.next = fragHead;
                    }
                    fragTail.next = current;
                    prev = fragTail;
                }
            } else {
                prev = node;
            }
        }
        return newHead;
    }

    // 0x4C6AAC rect_clip
    public static RectListNode rectClip(Rect b, Rect t) {
        Rect clippedT = new Rect();
        if (rectIntersection(t, b, clippedT) == 0) {
            RectListNode head = null;
            RectListNode tail = null;

            Rect[] clippedB = { new Rect(), new Rect(), new Rect(), new Rect() };
            clippedB[0].left = b.left;
            clippedB[0].top = b.top;
            clippedB[0].right = b.right;
            clippedB[0].bottom = clippedT.top - 1;

            clippedB[1].left = b.left;
            clippedB[1].top = clippedT.top;
            clippedB[1].right = clippedT.left - 1;
            clippedB[1].bottom = clippedT.bottom;

            clippedB[2].left = clippedT.right + 1;
            clippedB[2].top = clippedT.top;
            clippedB[2].right = b.right;
            clippedB[2].bottom = clippedT.bottom;

            clippedB[3].left = b.left;
            clippedB[3].top = clippedT.bottom + 1;
            clippedB[3].right = b.right;
            clippedB[3].bottom = b.bottom;

            for (Rect r : clippedB) {
                if (r.left <= r.right && r.top <= r.bottom) {
                    RectListNode node = rectMalloc();
                    copyTo(node.rect, r);
                    node.next = null;
                    if (head == null) {
                        head = tail = node;
                    } else {
                        tail.next = node;
                        tail = node;
                    }
                }
            }
            return head;
        }

        RectListNode list = rectMalloc();
        copyTo(list.rect, b);
        list.next = null;
        return list;
    }

    // 0x4C6BB8 rect_malloc
    public static RectListNode rectMalloc() {
        RectListNode node = freeList.poll();
        return node != null ? node : new RectListNode();
    }

    // 0x4C6C04 rect_free
    public static void rectFree(RectListNode node) {
        node.next = null;
        freeList.push(node);
    }

    // 0x4C6C18 rect_min_bound
    public static void rectUnion(Rect s1, Rect s2, Rect r) {
        r.left = Math.min(s1.left, s2.left);
        r.top = Math.min(s1.top, s2.top);
        r.right = Math.max(s1.right, s2.right);
        r.bottom = Math.max(s1.bottom, s2.bottom);
    }

    /**
     * 0x4C6C68 rect_inside_bound. Returns {@code 0} with the intersection
     * in {@code r}, or {@code -1} with a copy of {@code s1} in {@code r}.
     */
    public static int rectIntersection(Rect s1, Rect s2, Rect r) {
        copyTo(r, s1);

        if (s1.left <= s2.right && s2.left <= s1.right && s2.bottom >= s1.top && s2.top <= s1.bottom) {
            if (s2.left > s1.left) {
                r.left = s2.left;
            }
            if (s2.right < s1.right) {
                r.right = s2.right;
            }
            if (s2.top > s1.top) {
                r.top = s2.top;
            }
            if (s2.bottom < s1.bottom) {
                r.bottom = s2.bottom;
            }
            return 0;
        }

        return -1;
    }

    public static void copyTo(Rect dest, Rect src) {
        dest.left = src.left;
        dest.top = src.top;
        dest.right = src.right;
        dest.bottom = src.bottom;
    }

    public static int rectGetWidth(Rect rect) {
        return rect.right - rect.left + 1;
    }

    public static int rectGetHeight(Rect rect) {
        return rect.bottom - rect.top + 1;
    }

    public static void rectOffset(Rect rect, int dx, int dy) {
        rect.left += dx;
        rect.top += dy;
        rect.right += dx;
        rect.bottom += dy;
    }
}
