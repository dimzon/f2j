package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Mirrors {@code docker/refharness/golden/v28.txt} ({@code [ptrreg]}).
 */
class PointerRegistryTest {

    @BeforeEach
    void reset() {
        PointerRegistry.resetForTests();
    }

    @Test
    void goldenVectors() {
        Object a = new Object();
        Object b = new Object();

        // store-null=0 store-a=1 store-b=2 ... store-c=3
        assertEquals(0, PointerRegistry.ptrToInt(null));
        assertEquals(1, PointerRegistry.ptrToInt(a));
        assertEquals(2, PointerRegistry.ptrToInt(b));

        // fetch-a=1 fetch-remove-b=1 fetch-b-gone=1 fetch-zero=1 fetch-missing=1
        assertSame(a, PointerRegistry.intToPtr(1));
        assertSame(b, PointerRegistry.intToPtr(2, true));
        assertNull(PointerRegistry.intToPtr(2));
        assertNull(PointerRegistry.intToPtr(0));
        assertNull(PointerRegistry.intToPtr(999));

        // Refs are monotonic: the next store continues the sequence.
        assertEquals(3, PointerRegistry.ptrToInt(a));
    }

    @Test
    void fetchWithoutRemoveKeepsHandle() {
        Object a = new Object();
        int ref = PointerRegistry.ptrToInt(a);
        assertSame(a, PointerRegistry.intToPtr(ref));
        assertSame(a, PointerRegistry.intToPtr(ref, false));
        assertSame(a, PointerRegistry.intToPtr(ref, true));
        assertNull(PointerRegistry.intToPtr(ref));
    }
}
