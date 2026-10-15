package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Allocator callback shapes from {@code src/memory_defs.h}: lambdas plug
 * into the functional interfaces and round-trip.
 */
class MemoryDefsTest {

    @Test
    void procsRoundTrip() {
        List<Object> live = new ArrayList<>();
        MemoryDefs.MallocProc malloc = size -> {
            Object block = new byte[(int) size];
            live.add(block);
            return block;
        };
        MemoryDefs.ReallocProc realloc = (ptr, newSize) -> {
            live.remove(ptr);
            Object grown = new byte[(int) newSize];
            live.add(grown);
            return grown;
        };
        MemoryDefs.FreeProc free = live::remove;

        Object a = malloc.malloc(16);
        assertEquals(1, live.size());
        Object b = realloc.realloc(a, 32);
        assertEquals(1, live.size());
        assertSame(b, live.get(0));
        free.free(b);
        assertEquals(0, live.size());

        Object c = realloc.realloc(null, 0);
        assertEquals(1, live.size());
        free.free(c);
        assertEquals(0, live.size());
    }
}
