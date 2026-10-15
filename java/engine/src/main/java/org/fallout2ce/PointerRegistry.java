package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Opaque handle registry for passing heap objects through integer channels
 * (script ints, C callbacks), mirroring {@code src/pointer_registry.h/cc}.
 *
 * <p>Refs are small monotonically increasing ints starting at 1; 0 is
 * reserved for null. {@link #fetch(int, boolean)} with {@code remove=true}
 * releases the handle. Stored values are held strongly so a live handle
 * keeps its object alive (the C++ side keeps the raw pointer valid by
 * contract instead; same observable contract).
 *
 * <p>Quirk: the C++ {@code fetch} uses {@code _map[ref]}, which inserts a
 * null entry for unknown refs. That entry is unobservable, so the Java
 * port returns null without inserting.
 */
public final class PointerRegistry {

    private static final PointerRegistry SHARED = new PointerRegistry();

    private final Map<Integer, Object> map = new HashMap<>();
    private int next = 1;

    private PointerRegistry() {
    }

    public static PointerRegistry shared() {
        return SHARED;
    }

    public int store(Object ptr) {
        if (ptr == null) {
            return 0;
        }
        int ref = next++;
        map.put(ref, ptr);
        return ref;
    }

    public Object fetch(int ref, boolean remove) {
        if (ref == 0) {
            return null;
        }
        Object ptr = map.get(ref);
        if (remove) {
            map.remove(ref);
        }
        return ptr;
    }

    public Object fetch(int ref) {
        return fetch(ref, false);
    }

    public static int ptrToInt(Object ptr) {
        return shared().store(ptr);
    }

    public static Object intToPtr(int ref, boolean remove) {
        return shared().fetch(ref, remove);
    }

    public static Object intToPtr(int ref) {
        return intToPtr(ref, false);
    }

    /**
     * Test seam: the C++ singleton has no reset; tests need isolation.
     */
    static void resetForTests() {
        SHARED.map.clear();
        SHARED.next = 1;
    }
}
