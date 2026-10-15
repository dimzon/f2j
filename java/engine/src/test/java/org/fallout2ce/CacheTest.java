package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Lifecycle vectors mirroring {@code docker/refharness/golden/v12.txt}
 * ({@code [cache]}); pressure paths assert codes, not allocator internals.
 */
class CacheTest {

    static final int[] SIZES = { 100, 200, 300, 500 };

    Cache.Store cache;

    int sizeOf(int key, int[] sizePtr) {
        if (key < 1 || key > 4) {
            return -1;
        }
        sizePtr[0] = SIZES[key - 1];
        return 0;
    }

    int readOf(int key, int[] size, byte[] buffer) {
        if (key < 1 || key > 4) {
            return -1;
        }
        byte fill = (byte) (0x10 + key);
        for (int i = 0; i < size[0]; i++) {
            buffer[i] = fill;
        }
        return 0;
    }

    @AfterEach
    void reset() {
        Cache.resetSoundHookForTests();
    }

    @Test
    void lifecycle() {
        cache = new Cache.Store();
        assertTrue(Cache.cacheInit(cache, this::sizeOf, this::readOf, 1000));

        Cache.Entry e1 = Cache.cacheLock(cache, 1);
        assertNotNull(e1);
        assertEquals(1, e1.referenceCount);
        assertEquals(0, e1.hits);
        assertEquals(0x11, e1.data[0] & 0xFF);

        Cache.Entry e1b = Cache.cacheLock(cache, 1);
        assertNotNull(e1b);
        assertEquals(2, e1b.referenceCount);
        assertEquals(1, e1b.hits);
        assertTrue(e1 == e1b);

        assertTrue(Cache.cacheUnlock(cache, e1b));
        assertTrue(Cache.cacheUnlock(cache, e1));
        assertEquals(0, e1.referenceCount);
        assertTrue(!Cache.cacheUnlock(cache, e1));
        assertTrue(!Cache.cacheUnlock(cache, null));

        assertNull(Cache.cacheLock(cache, 99));

        // Flush drops it; relock re-reads identical content.
        assertTrue(Cache.cacheFlush(cache));
        Cache.Entry e1c = Cache.cacheLock(cache, 1);
        assertNotNull(e1c);
        assertEquals(0x11, e1c.data[0] & 0xFF);
        assertTrue(Cache.cacheUnlock(cache, e1c));

        assertTrue(Cache.cacheFree(cache));
        assertTrue(!Cache.cacheFree(null));
    }

    @Test
    void evictionCodes() {
        cache = new Cache.Store();
        assertTrue(Cache.cacheInit(cache, this::sizeOf, this::readOf, 600));

        Cache.Entry big = Cache.cacheLock(cache, 4);
        assertNotNull(big);
        assertTrue(Cache.cacheUnlock(cache, big));

        Cache.Entry after = Cache.cacheLock(cache, 2);
        assertNotNull(after);
        assertTrue(Cache.cacheUnlock(cache, after));

        assertTrue(Cache.cacheFree(cache));
    }

    @Test
    void tooBigNeverLoads() {
        cache = new Cache.Store();
        assertTrue(Cache.cacheInit(cache, this::sizeOf, this::readOf, 400));

        assertNull(Cache.cacheLock(cache, 4));

        assertTrue(Cache.cacheFree(cache));
    }

    @Test
    void soundHookFiresOnMiss() {
        cache = new Cache.Store();
        assertTrue(Cache.cacheInit(cache, this::sizeOf, this::readOf, 1000));

        AtomicInteger calls = new AtomicInteger();
        Cache.soundContinueAllHook = calls::incrementAndGet;

        Cache.Entry e1 = Cache.cacheLock(cache, 1);
        assertNotNull(e1);
        assertEquals(1, calls.get());

        Cache.Entry e1b = Cache.cacheLock(cache, 1);
        assertNotNull(e1b);
        assertEquals(1, calls.get()); // hit: no hook

        assertTrue(Cache.cacheUnlock(cache, e1));
        assertTrue(Cache.cacheUnlock(cache, e1b));
        assertTrue(Cache.cacheFree(cache));
    }

    @Test
    void statsText() {
        cache = new Cache.Store();
        assertTrue(Cache.cacheInit(cache, this::sizeOf, this::readOf, 1000));

        StringBuilder dest = new StringBuilder();
        assertTrue(Cache.cachePrintStats(cache, dest, 1024));
        assertEquals("Cache stats are disabled.\n", dest.toString());
        assertTrue(!Cache.cachePrintStats(null, dest, 1024));

        assertTrue(Cache.cacheFree(cache));
    }
}
