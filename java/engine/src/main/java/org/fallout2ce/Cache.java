package org.fallout2ce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Port of {@code src/cache.h} / {@code src/cache.cc} (lookup, insertion,
 * usage-ordered eviction, refcounts, statistics).
 *
 * <p>There is deliberately NO {@code Heap} port: the 1428-line C block
 * allocator (movable blocks, free lists, coalescing) exists for manual
 * memory management. In Java entries hold plain {@code byte[]} reclaimed
 * by the GC, and eviction stays deterministic on {@code maxSize}
 * accounting, exactly like the observable C behavior. Avenues that only
 * exist to fight fragmentation (the +25% growth loop, system fallback,
 * alignment) are folded into a single flush-and-retry. SoftReferences
 * are NOT used: they would make eviction timing nondeterministic and
 * break differential testing; the 32MB default budget is trivial for
 * the JVM.
 */
public final class Cache {

    private Cache() {
    }

    public static final int ENTRIES_INITIAL_CAPACITY = 100;
    public static final int ENTRIES_GROW_CAPACITY = 50;

    public static final int ENTRY_NONE = 0x00;
    public static final int ENTRY_MARKED_FOR_EVICTION = 0x01;

    /** Mirror of {@code CacheSizeProc} (0 = ok). */
    public interface SizeProc {
        int size(int key, int[] sizePtr);
    }

    /** Mirror of {@code CacheReadProc} (0 = ok, fills {@code size} bytes). */
    public interface ReadProc {
        int read(int key, int[] size, byte[] buffer);
    }

    /** Mirror of {@code CacheEntry}. */
    public static final class Entry {
        public int key;
        public int size;
        public byte[] data;
        public int referenceCount;
        public long hits;
        public int flags = ENTRY_NONE;
        public long mru;
    }

    /** Mirror of {@code Cache} (entries stay key-sorted, like C). */
    public static final class Store {
        int size;
        int maxSize;
        final List<Entry> entries = new ArrayList<>();
        int entriesCapacity = ENTRIES_INITIAL_CAPACITY;
        long hits;
        SizeProc sizeProc;
        ReadProc readProc;
    }

    /** Seam for {@code soundContinueAll()} (sound phase wires it). */
    static Runnable soundContinueAllHook = () -> {
    };

    // 0x41FCC0 cache_init.
    public static boolean cacheInit(Store cache, SizeProc sizeProc, ReadProc readProc, int maxSize) {
        cache.size = 0;
        cache.maxSize = maxSize;
        cache.entries.clear();
        cache.entriesCapacity = ENTRIES_INITIAL_CAPACITY;
        cache.hits = 0;
        cache.sizeProc = sizeProc;
        cache.readProc = readProc;
        return true;
    }

    // 0x41FD50 cache_exit.
    public static boolean cacheFree(Store cache) {
        if (cache == null) {
            return false;
        }

        cacheClean(cache);
        cacheFlush(cache);

        cache.size = 0;
        cache.maxSize = 0;
        cache.entries.clear();
        cache.entriesCapacity = 0;
        cache.hits = 0;
        cache.sizeProc = null;
        cache.readProc = null;

        return true;
    }

    // 0x41FDE8 cache_lock. Returns the entry (data valid while locked).
    public static Entry cacheLock(Store cache, int key) {
        if (cache == null) {
            return null;
        }

        int[] at = new int[1];
        int rc = findIndexForKey(cache, key, at);
        Entry entry;
        if (rc == 2) {
            entry = cache.entries.get(at[0]);
            entry.hits++;
        } else if (rc == 3) {
            entry = fetchEntryForKey(cache, key, at[0]);
            if (entry == null) {
                return null;
            }

            // NOTE: C runs this through a ticker that nothing increments,
            // so it fires on every fetch; simplified to the observable.
            soundContinueAllHook.run();
        } else {
            return null;
        }

        entry.referenceCount++;

        cache.hits++;
        entry.mru = cache.hits;

        if (cache.hits == 0xFFFFFFFFL) {
            resetStatistics(cache);
        }

        return entry;
    }

    // 0x4200B8 cache_unlock.
    public static boolean cacheUnlock(Store cache, Entry entry) {
        if (cache == null || entry == null) {
            return false;
        }

        if (entry.referenceCount == 0) {
            return false;
        }

        entry.referenceCount--;
        return true;
    }

    // 0x42012C cache_flush.
    public static boolean cacheFlush(Store cache) {
        if (cache == null) {
            return false;
        }

        for (Entry entry : cache.entries) {
            if (entry.referenceCount == 0) {
                entry.flags |= ENTRY_MARKED_FOR_EVICTION;
            }
        }

        cacheSweep(cache);

        int optimalCapacity = cache.entries.size() + ENTRIES_GROW_CAPACITY;
        if (optimalCapacity < cache.entriesCapacity) {
            setCapacity(cache, optimalCapacity);
        }

        return true;
    }

    // 0x42019C cache_stats (always this text, like C).
    public static boolean cachePrintStats(Store cache, StringBuilder dest, int size) {
        if (cache == null || dest == null) {
            return false;
        }

        String text = "Cache stats are disabled.\n";
        dest.append(text, 0, size > 0 ? Math.min(text.length(), size - 1) : 0);
        return true;
    }

    // 0x4203AC cache_add.
    static Entry fetchEntryForKey(Store cache, int key, int index) {
        int[] size = new int[1];
        if (cache.sizeProc.size(key, size) != 0) {
            return null;
        }

        if (!ensureSize(cache, size[0])) {
            // NOTE: single flush-and-retry instead of the C fragmentation
            // growth loop (no heap here); same observable outcome.
            cacheFlush(cache);
            if (!ensureSize(cache, size[0])) {
                return null;
            }
        }

        byte[] data = new byte[size[0]];
        if (cache.readProc.read(key, size, data) != 0) {
            return null;
        }

        Entry entry = new Entry();
        entry.size = size[0];
        entry.key = key;
        entry.data = data;

        int at = index;
        boolean isNewKey = true;
        if (at < cache.entries.size()) {
            if (key < cache.entries.get(at).key) {
                if (at == 0 || key > cache.entries.get(at - 1).key) {
                    isNewKey = false;
                }
            }
        }

        if (isNewKey) {
            int[] fresh = new int[1];
            if (findIndexForKey(cache, key, fresh) != 3) {
                return null;
            }
            at = fresh[0];
        }

        insertEntryAtIndex(cache, entry, at);
        return entry;
    }

    // 0x4205E8 cache_insert.
    static void insertEntryAtIndex(Store cache, Entry entry, int index) {
        if (cache.entries.size() == cache.entriesCapacity - 1) {
            cache.entriesCapacity += ENTRIES_GROW_CAPACITY;
        }

        cache.entries.add(index, entry);
        cache.size += entry.size;
    }

    /**
     * 0x420654 cache_find. Returns 2 when present, 3 with the insertion
     * point otherwise (proper bisection, unlike the dictionary walk).
     */
    static int findIndexForKey(Store cache, int key, int[] indexPtr) {
        int length = cache.entries.size();
        if (length == 0) {
            indexPtr[0] = 0;
            return 3;
        }

        int r = length - 1;
        int l = 0;
        int mid = 0;
        int cmp = 0;
        do {
            mid = (l + r) / 2;

            cmp = key - cache.entries.get(mid).key;
            if (cmp == 0) {
                indexPtr[0] = mid;
                return 2;
            }

            if (cmp > 0) {
                l = mid + 1;
            } else {
                r = mid - 1;
            }
        } while (r >= l);

        if (cmp < 0) {
            indexPtr[0] = mid;
        } else {
            indexPtr[0] = mid + 1;
        }

        return 3;
    }

    // 0x420764 cache_unlock_all.
    static boolean cacheClean(Store cache) {
        for (Entry entry : cache.entries) {
            if (entry.referenceCount != 0) {
                entry.referenceCount = 0;
            }
        }

        return true;
    }

    // 0x4207D4 cache_reset_counter.
    static boolean resetStatistics(Store cache) {
        if (cache == null) {
            return false;
        }

        List<Entry> sorted = new ArrayList<>(cache.entries);
        sorted.sort(Cache::compareByMostRecentHit);

        for (int index = 0; index < sorted.size(); index++) {
            sorted.get(index).mru = index;
        }

        cache.hits = sorted.size();

        return true;
    }

    // 0x42084C cache_make_room.
    static boolean ensureSize(Store cache, int size) {
        if (size > cache.maxSize) {
            return false;
        }

        if (cache.maxSize - cache.size >= size) {
            return true;
        }

        List<Entry> sorted = new ArrayList<>(cache.entries);
        sorted.sort(Cache::compareByUsage);

        // Sweeping threshold: 20% of cache size plus the new entry.
        int threshold = size + (int) (cache.size * 0.2);

        int accum = 0;
        int index;
        for (index = 0; index < sorted.size(); index++) {
            Entry entry = sorted.get(index);
            if (entry.referenceCount == 0) {
                if (entry.size >= threshold) {
                    entry.flags |= ENTRY_MARKED_FOR_EVICTION;

                    accum = 0;
                    break;
                } else {
                    accum += entry.size;

                    if (accum >= threshold) {
                        break;
                    }
                }
            }
        }

        if (accum != 0) {
            if (index == sorted.size()) {
                index -= 1;
            }

            for (; index >= 0; index--) {
                Entry entry = sorted.get(index);
                if (entry.referenceCount == 0) {
                    entry.flags |= ENTRY_MARKED_FOR_EVICTION;
                }
            }
        }

        cacheSweep(cache);

        if (cache.maxSize - cache.size >= size) {
            return true;
        }

        return false;
    }

    // 0x42099C cache_purge.
    static boolean cacheSweep(Store cache) {
        for (int index = 0; index < cache.entries.size(); index++) {
            Entry entry = cache.entries.get(index);
            if ((entry.flags & ENTRY_MARKED_FOR_EVICTION) != ENTRY_NONE) {
                if (entry.referenceCount != 0) {
                    entry.flags &= ~ENTRY_MARKED_FOR_EVICTION;
                } else {
                    int entrySize = entry.size;
                    cache.entries.remove(index);
                    cache.size -= entrySize;
                    index--;
                }
            }
        }

        return true;
    }

    // 0x420A40 cache_resize_array.
    static boolean setCapacity(Store cache, int newCapacity) {
        if (newCapacity < cache.entries.size()) {
            return false;
        }

        cache.entriesCapacity = newCapacity;
        return true;
    }

    // 0x420A74 cache_compare_make_room.
    static int compareByUsage(Entry lhs, Entry rhs) {
        if (lhs.referenceCount != 0 && rhs.referenceCount == 0) {
            return 1;
        }

        if (rhs.referenceCount != 0 && lhs.referenceCount == 0) {
            return -1;
        }

        if (lhs.hits < rhs.hits) {
            return -1;
        } else if (lhs.hits > rhs.hits) {
            return 1;
        }

        if (lhs.mru < rhs.mru) {
            return -1;
        } else if (lhs.mru > rhs.mru) {
            return 1;
        }

        return 0;
    }

    // 0x420AE8 cache_compare_reset_counter (descending mru).
    static int compareByMostRecentHit(Entry lhs, Entry rhs) {
        if (lhs.mru < rhs.mru) {
            return 1;
        } else if (lhs.mru > rhs.mru) {
            return -1;
        } else {
            return 0;
        }
    }

    /** Test seam: restore the no-op sound hook. */
    static void resetSoundHookForTests() {
        soundContinueAllHook = () -> {
        };
    }
}
