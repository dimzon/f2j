package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Mouse cursor cache types, mirroring {@code src/mouse_manager.h}.
 * The cursor engine ({@code mouse_manager.cc}) comes with the UI phase.
 */
public final class MouseManager {

    private MouseManager() {
    }

    public static final int CACHE_CAPACITY = 32;

    /** Port of {@code MouseManagerNameMangler}. */
    @FunctionalInterface
    public interface NameMangler {
        String mangle(String fileName);
    }

    /** Port of {@code MouseManagerRateProvider}. */
    @FunctionalInterface
    public interface RateProvider {
        int rate();
    }

    /** Port of {@code MouseManagerTimeProvider}. */
    @FunctionalInterface
    public interface TimeProvider {
        int time();
    }

    public enum MouseType {
        MOUSE_MANAGER_MOUSE_TYPE_NONE(0),
        MOUSE_MANAGER_MOUSE_TYPE_STATIC(1),
        MOUSE_MANAGER_MOUSE_TYPE_ANIMATED(2);

        private final int id;

        MouseType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, MouseType> TYPE_BY_ID = new HashMap<>();

    static {
        for (MouseType e : MouseType.values()) {
            TYPE_BY_ID.put(e.id, e);
        }
    }

    public static MouseType ofMouseType(int id) {
        return TYPE_BY_ID.get(id);
    }

    /** Port of {@code MouseManagerStaticData}. */
    public static final class StaticData {
        public byte[] data;
        public int field4;
        public int field8;
        public int width;
        public int height;
    }

    /** Port of {@code MouseManagerAnimatedData}. */
    public static final class AnimatedData {
        public byte[][] field0;
        public byte[][] field4;
        public int[] field8;
        public int[] fieldC;
        public int width;
        public int height;
        public float field18;
        public int field1C;
        public int field20;
        public byte field24;
        public byte frameCount;
        public byte field26;
    }

    /** Port of {@code MouseManagerCacheEntry} (union as parallel fields). */
    public static final class CacheEntry {
        public Object data;
        public StaticData staticData;
        public AnimatedData animatedData;
        public int type;
        public final byte[] palette = new byte[256 * 3];
        public int ref;
        public String fileName = "";
        public String field32c = "";
    }
}
