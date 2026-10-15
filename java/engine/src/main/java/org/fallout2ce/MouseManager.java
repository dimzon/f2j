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

    // ===== cursor engine (mouse_manager.cc) =====

    private static NameMangler nameMangler = MouseManager::nameManglerDefault;
    private static RateProvider rateProvider = MouseManager::rateProviderDefault;
    private static TimeProvider timeProvider = MouseManager::timeProviderDefault;

    private static int currentRef = 1;
    private static final CacheEntry[] cache = new CacheEntry[CACHE_CAPACITY];
    private static boolean animating;
    private static byte[] currentPalette;
    private static AnimatedData currentAnimatedData;
    private static byte[] currentStaticData;
    private static int currentCacheEntryIndex;

    static {
        for (int index = 0; index < cache.length; index++) {
            cache[index] = new CacheEntry();
        }
    }

    private static boolean cleanupAndFail(XFile.Handle stream, AnimatedData animatedData, int loadedFrameCount) {
        if (animatedData != null) {
            animatedData.frameCount = (byte) loadedFrameCount;

            CacheEntry tempEntry = new CacheEntry();
            tempEntry.animatedData = animatedData;
            tempEntry.type = MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id();
            mouseManagerFreeCacheEntry(tempEntry);
        }

        Db.fileClose(stream);
        return false;
    }

    private static boolean cleanupAndFail(XFile.Handle stream) {
        return cleanupAndFail(stream, null, 0);
    }

    /** Port of {@code mouseManagerNameManglerDefaultImpl} (identity). */
    public static String nameManglerDefault(String name) {
        return name;
    }

    /** Port of {@code mouseManagerRateProviderDefaultImpl} (1000). */
    public static int rateProviderDefault() {
        return 1000;
    }

    /** Port of {@code mouseManagerTimeProviderDefaultImpl} (ticks). */
    public static int timeProviderDefault() {
        return (int) Ticks.getTicks();
    }

    /** Port of {@code mouseManagerSetNameMangler}. */
    public static void mouseManagerSetNameMangler(NameMangler func) {
        nameMangler = func;
    }

    /** Test seam: replace the rate provider. */
    static void setRateProviderForTests(RateProvider provider) {
        rateProvider = provider;
    }

    /** Test seam: replace the time provider. */
    static void setTimeProviderForTests(TimeProvider provider) {
        timeProvider = provider;
    }

    private static int strnicmp(String a, String b, int n) {
        for (int i = 0; i < n; i++) {
            char ca = i < a.length() ? a.charAt(i) : 0;
            char cb = i < b.length() ? b.charAt(i) : 0;
            int d = Character.toLowerCase(ca) - Character.toLowerCase(cb);
            if (d != 0) {
                return d;
            }
            if (ca == 0) {
                return 0;
            }
        }
        return 0;
    }

    private static int stricmp(String a, String b) {
        int len = Math.max(a.length(), b.length()) + 1;
        return strnicmp(a, b, len);
    }

    /** Port of {@code mouseManagerFreeCacheEntry}. */
    public static void mouseManagerFreeCacheEntry(CacheEntry entry) {
        if (entry.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id()) {
            entry.staticData = null;
        } else if (entry.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id()) {
            entry.animatedData = null;
        }

        entry.type = MouseType.MOUSE_MANAGER_MOUSE_TYPE_NONE.id();
        entry.fileName = "";
    }

    /** Port of {@code mouseManagerInsertCacheEntry}. */
    public static int mouseManagerInsertCacheEntry(Object data, int type, byte[] palette, String fileName) {
        int foundIndex = -1;
        int index = 0;
        for (; index < CACHE_CAPACITY; index++) {
            CacheEntry entry = cache[index];
            if (entry.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_NONE.id() && foundIndex == -1) {
                foundIndex = index;
            }

            if (stricmp(fileName, entry.fileName) == 0) {
                mouseManagerFreeCacheEntry(entry);
                foundIndex = index;
                break;
            }
        }

        if (foundIndex != -1) {
            index = foundIndex;
        }

        if (index == CACHE_CAPACITY) {
            int oldest = -1;
            int oldestRef = currentRef;
            for (int scan = 0; scan < CACHE_CAPACITY; scan++) {
                CacheEntry entry = cache[scan];
                if (oldestRef > entry.ref) {
                    oldestRef = entry.ref;
                    oldest = scan;
                }
            }

            if (oldest == -1) {
                Debug.print("Mouse cache overflow!!!!\n");
                throw new IllegalStateException("Mouse cache overflow");
            }

            index = oldest;
            mouseManagerFreeCacheEntry(cache[index]);
        }

        CacheEntry entry = cache[index];
        entry.type = type;
        System.arraycopy(palette, 0, entry.palette, 0, entry.palette.length);
        entry.ref = currentRef++;
        entry.fileName = fileName.length() > 31 ? fileName.substring(0, 31) : fileName;
        entry.field32c = "";
        // The C union aliases data/staticData/animatedData to one pointer.
        entry.data = data;
        entry.staticData = type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id()
            ? (StaticData) data : null;
        entry.animatedData = type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id()
            ? (AnimatedData) data : null;

        return index;
    }

    /** Port of {@code mouseManagerFlushCache} (inlined in C++). */
    public static void mouseManagerFlushCache() {
        for (int index = 0; index < CACHE_CAPACITY; index++) {
            mouseManagerFreeCacheEntry(cache[index]);
        }
    }

    /** Found-entry view (C++ out-params): palette, hot, size, type. */
    public static final class FoundEntry {
        public byte[] palette;
        public int hotX;
        public int hotY;
        public int width;
        public int height;
        public int type;
    }

    /** Port of {@code mouseManagerFindCacheEntry} (entry + decoded view). */
    public static CacheEntry mouseManagerFindCacheEntry(String fileName, FoundEntry view) {
        for (int index = 0; index < CACHE_CAPACITY; index++) {
            CacheEntry entry = cache[index];
            if (strnicmp(entry.fileName, fileName, 31) == 0
                    || strnicmp(entry.field32c, fileName, 31) == 0) {
                view.palette = entry.palette;
                view.type = entry.type;

                currentCacheEntryIndex = index;

                if (entry.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id()) {
                    view.hotX = entry.staticData.field4;
                    view.hotY = entry.staticData.field8;
                    view.width = entry.staticData.width;
                    view.height = entry.staticData.height;
                } else if (entry.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id()) {
                    view.width = entry.animatedData.width;
                    view.height = entry.animatedData.height;
                    view.hotX = entry.animatedData.field8[entry.animatedData.field26];
                    view.hotY = entry.animatedData.fieldC[entry.animatedData.field26];
                }

                return entry;
            }
        }

        return null;
    }

    /** Port of {@code mouseManagerInit} (initMousemgr). */
    public static void mouseManagerInit() {
        Mouse.mouseSetSensitivity(1.0);
    }

    /** Port of {@code mouseManagerExit} (mousemgrClose). */
    public static void mouseManagerExit() {
        Mouse.mouseSetFrame(null, 0, 0, 0, 0, 0, 0);

        currentStaticData = null;

        // NOTE: Uninline.
        mouseManagerFlushCache();

        currentPalette = null;
        currentAnimatedData = null;
    }

    /** Port of {@code mouseManagerUpdate} (mousemgrUpdate). */
    public static void mouseManagerUpdate() {
        if (!animating) {
            return;
        }

        if (currentAnimatedData == null) {
            Debug.print("Animating == 1 but curAnim == 0\n");
            animating = false;
            return;
        }

        if (timeProvider.time() >= currentAnimatedData.field1C) {
            currentAnimatedData.field1C = (int) (currentAnimatedData.field18
                    / currentAnimatedData.frameCount * rateProvider.rate()
                    + timeProvider.time());
            if (currentAnimatedData.field24 != currentAnimatedData.field26) {
                int next = currentAnimatedData.field26 + currentAnimatedData.field20;
                if (next < 0) {
                    next = currentAnimatedData.frameCount - 1;
                } else if (next >= currentAnimatedData.frameCount) {
                    next = 0;
                }

                currentAnimatedData.field26 = (byte) next;
                byte[] dst = currentAnimatedData.field0[next];
                byte[] src = currentAnimatedData.field4[next];
                System.arraycopy(src, 0, dst, 0,
                    currentAnimatedData.width * currentAnimatedData.height);

                Datafile.datafileRemapPixelsRgb8(
                    currentAnimatedData.field0[currentAnimatedData.field26],
                    currentPalette,
                    currentAnimatedData.width,
                    currentAnimatedData.height);

                Mouse.mouseSetFrame(currentAnimatedData.field0[next],
                    currentAnimatedData.width,
                    currentAnimatedData.height,
                    currentAnimatedData.width,
                    currentAnimatedData.field8[next],
                    currentAnimatedData.fieldC[next],
                    0);
            }
        }
    }

    /** Port of {@code mouseManagerSetFrame} (0x485868 mouseSetFrame). */
    public static boolean mouseManagerSetFrame(String fileName, int frame) {
        String mangledFileName = nameMangler.mangle(fileName);

        FoundEntry view = new FoundEntry();
        CacheEntry entry = mouseManagerFindCacheEntry(fileName, view);
        if (entry != null) {
            if (view.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id()) {
                entry.animatedData.field24 = (byte) frame;
                if (entry.animatedData.field24 >= entry.animatedData.field26) {
                    int v1 = entry.animatedData.field24 - entry.animatedData.field26;
                    int v2 = entry.animatedData.frameCount
                        + entry.animatedData.field26 - entry.animatedData.field24;
                    if (v1 >= v2) {
                        entry.animatedData.field20 = -1;
                    } else {
                        entry.animatedData.field20 = 1;
                    }
                } else {
                    int v1 = entry.animatedData.field26 - entry.animatedData.field24;
                    int v2 = entry.animatedData.frameCount
                        + entry.animatedData.field24 - entry.animatedData.field26;
                    if (v1 < v2) {
                        entry.animatedData.field20 = -1;
                    } else {
                        entry.animatedData.field20 = 1;
                    }
                }

                if (!animating || currentAnimatedData != entry.animatedData) {
                    byte[] dst = entry.animatedData.field0[entry.animatedData.field26];
                    byte[] src = entry.animatedData.field4[entry.animatedData.field26];
                    System.arraycopy(src, 0, dst, 0,
                        entry.animatedData.width * entry.animatedData.height);

                    Mouse.mouseSetFrame(entry.animatedData.field0[entry.animatedData.field26],
                        entry.animatedData.width,
                        entry.animatedData.height,
                        entry.animatedData.width,
                        entry.animatedData.field8[entry.animatedData.field26],
                        entry.animatedData.fieldC[entry.animatedData.field26],
                        0);

                    animating = true;
                }

                currentAnimatedData = entry.animatedData;
                currentPalette = view.palette;
                currentAnimatedData.field1C = timeProvider.time();
                return true;
            }

            mouseManagerSetMousePointer(fileName);
            return true;
        }

        if (animating) {
            currentPalette = null;
            animating = false;
            currentAnimatedData = null;
        } else {
            currentStaticData = null;
        }

        XFile.Handle stream = Db.fileOpen(mangledFileName, "r");
        if (stream == null) {
            Debug.print("mouseSetFrame: couldn't find %s\n", mangledFileName);
            return false;
        }

        String header = Db.fileReadString(stream, 80);
        if (header == null || header.length() < 4 || !header.substring(0, 4).equalsIgnoreCase("anim")) {
            Db.fileClose(stream);
            mouseManagerSetMousePointer(fileName);
            return true;
        }

        // NOTE: Uninline.
        String[] headerParts = header.substring(4).strip().split("\\s+");
        if (headerParts.length < 2) {
            return cleanupAndFail(stream);
        }

        int frameCount;
        float rate;
        try {
            frameCount = Integer.parseInt(headerParts[0]);
            rate = Float.parseFloat(headerParts[1]);
        } catch (NumberFormatException e) {
            return cleanupAndFail(stream);
        }
        if (frameCount <= 0) {
            return cleanupAndFail(stream);
        }

        AnimatedData animatedData = new AnimatedData();
        animatedData.field0 = new byte[frameCount][];
        animatedData.field4 = new byte[frameCount][];
        animatedData.field8 = new int[frameCount];
        animatedData.fieldC = new int[frameCount];
        animatedData.field18 = rate;
        animatedData.field1C = timeProvider.time();
        animatedData.field26 = 0;
        animatedData.field24 = (byte) frame;
        animatedData.frameCount = (byte) frameCount;
        if (frameCount / 2 <= frame) {
            animatedData.field20 = -1;
        } else {
            animatedData.field20 = 1;
        }

        int width = 0;
        int height = 0;
        int loadedFrameCount = 0;
        for (int index = 0; index < frameCount; index++) {
            String line = Db.fileReadString(stream, 80);
            if (line == null || line.strip().isEmpty()) {
                Debug.print("Not enough frames in %s, got %d, needed %d", mangledFileName, index, frameCount);
                return cleanupAndFail(stream, animatedData, loadedFrameCount);
            }

            // NOTE: Uninline.
            int sep = line.indexOf(' ');
            if (sep < 0) {
                Debug.print("Bad line %s in %s\n", line.strip(), fileName);
                return cleanupAndFail(stream, animatedData, loadedFrameCount);
            }

            String artName = line.substring(0, sep);
            String[] coords = line.substring(sep + 1).strip().split("\\s+");
            if (coords.length < 2) {
                return cleanupAndFail(stream, animatedData, loadedFrameCount);
            }

            int hotX;
            int hotY;
            try {
                hotX = Integer.parseInt(coords[0]);
                hotY = Integer.parseInt(coords[1]);
            } catch (NumberFormatException e) {
                return cleanupAndFail(stream, animatedData, loadedFrameCount);
            }

            Datafile.Image raw = Datafile.datafileReadRaw(nameMangler.mangle(artName));
            if (raw == null) {
                return cleanupAndFail(stream, animatedData, loadedFrameCount);
            }
            width = raw.width();
            height = raw.height();

            animatedData.field4[index] = raw.data();
            animatedData.field0[index] = raw.data().clone();
            Datafile.datafileRemapPixelsRgb8(animatedData.field0[index], Datafile.datafileGetPalette(),
                width, height);
            animatedData.field8[index] = hotX;
            animatedData.fieldC[index] = hotY;
            loadedFrameCount++;
        }

        Db.fileClose(stream);

        animatedData.width = width;
        animatedData.height = height;

        currentCacheEntryIndex = mouseManagerInsertCacheEntry(animatedData,
            MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id(), Datafile.datafileGetPalette(), fileName);
        CacheEntry inserted = cache[currentCacheEntryIndex];
        inserted.field32c = fileName.length() > 31 ? fileName.substring(0, 31) : fileName;

        currentAnimatedData = animatedData;
        currentPalette = cache[currentCacheEntryIndex].palette;
        animating = true;

        Mouse.mouseSetFrame(animatedData.field0[0],
            animatedData.width,
            animatedData.height,
            animatedData.width,
            animatedData.field8[0],
            animatedData.fieldC[0],
            0);

        return true;
    }

    /** Port of {@code mouseManagerSetMouseShape} (0x485E58). */
    public static boolean mouseManagerSetMouseShape(String fileName, int hotX, int hotY) {
        FoundEntry view = new FoundEntry();
        CacheEntry entry = mouseManagerFindCacheEntry(fileName, view);
        String mangledFileName = nameMangler.mangle(fileName);

        if (entry == null) {
            StaticData staticData = new StaticData();
            Datafile.Image raw = Datafile.datafileReadRaw(mangledFileName);
            if (raw == null) {
                return false;
            }
            int width = raw.width();
            int height = raw.height();
            staticData.data = raw.data();
            staticData.field4 = hotX;
            staticData.field8 = hotY;
            staticData.width = width;
            staticData.height = height;
            currentCacheEntryIndex = mouseManagerInsertCacheEntry(staticData,
                MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id(), Datafile.datafileGetPalette(), fileName);

            // NOTE: Original code is slightly different. It obtains address of
            // `staticData` and sets it's it into `cacheEntry`, which is a bit
            // awkward. Maybe there is more level on indirection was used. Any way
            // in order to make code path below unaltered take entire cache entry.
            entry = cache[currentCacheEntryIndex];

            view.type = MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id();
            view.palette = cache[currentCacheEntryIndex].palette;
        }

        if (view.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id()) {
            currentStaticData = new byte[entry.staticData.width * entry.staticData.height];
            System.arraycopy(entry.staticData.data, 0, currentStaticData, 0, currentStaticData.length);
            Datafile.datafileRemapPixelsRgb8(currentStaticData, view.palette, entry.staticData.width,
                entry.staticData.height);
            Mouse.mouseSetFrame(currentStaticData, entry.staticData.width, entry.staticData.height,
                entry.staticData.width, hotX, hotY, 0);
            animating = false;
        } else if (view.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id()) {
            currentAnimatedData = entry.animatedData;
            animating = true;
            currentPalette = view.palette;
        }

        return true;
    }

    /** Port of {@code mouseManagerSetMousePointer} (0x486010). */
    public static boolean mouseManagerSetMousePointer(String fileName) {
        FoundEntry view = new FoundEntry();
        CacheEntry entry = mouseManagerFindCacheEntry(fileName, view);
        if (entry != null) {
            currentStaticData = null;

            currentPalette = null;
            animating = false;
            currentAnimatedData = null;

            if (view.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id()) {
                currentStaticData = new byte[entry.staticData.width * entry.staticData.height];
                System.arraycopy(entry.staticData.data, 0, currentStaticData, 0, currentStaticData.length);
                Datafile.datafileRemapPixelsRgb8(currentStaticData, view.palette, entry.staticData.width,
                    entry.staticData.height);
                Mouse.mouseSetFrame(currentStaticData, entry.staticData.width, entry.staticData.height,
                    entry.staticData.width, view.hotX, view.hotY, 0);
                animating = false;
            } else if (view.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id()) {
                currentAnimatedData = entry.animatedData;
                currentPalette = view.palette;
                currentAnimatedData.field26 = 0;
                currentAnimatedData.field24 = 0;
                Mouse.mouseSetFrame(currentAnimatedData.field0[0],
                    currentAnimatedData.width,
                    currentAnimatedData.height,
                    currentAnimatedData.width,
                    currentAnimatedData.field8[0],
                    currentAnimatedData.fieldC[0],
                    0);
                animating = true;
            }
            return true;
        }

        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && fileName.substring(dot + 1).equalsIgnoreCase("mou")) {
            return mouseManagerSetMouseShape(fileName, 0, 0);
        }

        String mangledFileName = nameMangler.mangle(fileName);
        XFile.Handle stream = Db.fileOpen(mangledFileName, "r");
        if (stream == null) {
            Debug.print("Can't find %s\n", mangledFileName);
            return false;
        }

        String header = Db.fileReadString(stream, 79);
        if (header == null || header.isEmpty()) {
            Db.fileClose(stream);
            return false;
        }

        boolean rc;
        if (header.length() >= 4 && header.substring(0, 4).equalsIgnoreCase("anim")) {
            Db.fileClose(stream);
            rc = mouseManagerSetFrame(fileName, 0);
        } else {
            // NOTE: Uninline.
            int sep = header.indexOf(' ');
            if (sep < 0) {
                Db.fileClose(stream);
                return false;
            }

            String artName = header.substring(0, sep);
            String[] coords = header.substring(sep + 1).strip().split("\\s+");
            if (coords.length < 2) {
                Db.fileClose(stream);
                return false;
            }

            int hotX;
            int hotY;
            try {
                hotX = Integer.parseInt(coords[0]);
                hotY = Integer.parseInt(coords[1]);
            } catch (NumberFormatException e) {
                Db.fileClose(stream);
                return false;
            }

            Db.fileClose(stream);

            rc = mouseManagerSetMouseShape(artName, hotX, hotY);
        }

        if (rc) {
            CacheEntry current = cache[currentCacheEntryIndex];
            current.field32c = fileName.length() > 31 ? fileName.substring(0, 31) : fileName;
        }

        return rc;
    }

    /** Port of {@code mouseManagerResetMouse} (0x4862AC mousemgrResetMouse). */
    public static void mouseManagerResetMouse() {
        CacheEntry entry = cache[currentCacheEntryIndex];

        int imageWidth = 0;
        int imageHeight = 0;
        if (entry.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id()) {
            imageWidth = entry.staticData.width;
            imageHeight = entry.staticData.height;
        } else if (entry.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id()) {
            imageWidth = entry.animatedData.width;
            imageHeight = entry.animatedData.height;
        } else {
            return;
        }

        if (entry.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id()) {
            if (currentStaticData != null) {
                currentStaticData = new byte[imageWidth * imageHeight];
                System.arraycopy(entry.staticData.data, 0, currentStaticData, 0,
                    imageWidth * imageHeight);
                Datafile.datafileRemapPixelsRgb8(currentStaticData, entry.palette, imageWidth, imageHeight);

                Mouse.mouseSetFrame(currentStaticData,
                    imageWidth,
                    imageHeight,
                    imageWidth,
                    entry.staticData.field4,
                    entry.staticData.field8,
                    0);
            } else {
                Debug.print("Hm, current mouse type is M_STATIC, but no current mouse pointer\n");
            }
        } else if (entry.type == MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id()) {
            if (currentAnimatedData != null) {
                int frames = currentAnimatedData.frameCount;
                for (int index = 0; index < frames; index++) {
                    System.arraycopy(currentAnimatedData.field4[index], 0,
                        currentAnimatedData.field0[index], 0, imageWidth * imageHeight);
                    Datafile.datafileRemapPixelsRgb8(currentAnimatedData.field0[index], entry.palette,
                        imageWidth, imageHeight);
                }

                Mouse.mouseSetFrame(
                    currentAnimatedData.field0[currentAnimatedData.field26],
                    imageWidth,
                    imageHeight,
                    imageWidth,
                    currentAnimatedData.field8[currentAnimatedData.field26],
                    currentAnimatedData.fieldC[currentAnimatedData.field26],
                    0);
            } else {
                Debug.print("Hm, current mouse type is M_ANIMATED, but no current mouse pointer\n");
            }
        }
    }

    /** Port of {@code mouseManagerHideMouse} (0x4865C4 mouseHide). */
    public static void mouseManagerHideMouse() {
        Mouse.mouseHideCursor();
    }

    /** Port of {@code mouseManagerShowMouse} (0x4865CC mouseShow). */
    public static void mouseManagerShowMouse() {
        Mouse.mouseShowCursor();
    }

    /** Test seam: fresh cursor manager state. */
    static void resetForTests() {
        nameMangler = MouseManager::nameManglerDefault;
        rateProvider = () -> 1000;
        timeProvider = () -> (int) Ticks.getTicks();
        currentRef = 1;
        for (int index = 0; index < cache.length; index++) {
            cache[index] = new CacheEntry();
        }
        animating = false;
        currentPalette = null;
        currentAnimatedData = null;
        currentStaticData = null;
        currentCacheEntryIndex = 0;
    }
}
