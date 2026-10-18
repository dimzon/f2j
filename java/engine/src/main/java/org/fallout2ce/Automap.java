package org.fallout2ce;

/**
 * Automap database reader, mirroring the file-facing half of
 * {@code src/automap.cc}: {@code AutomapHeader},
 * {@code automapLoadHeader}/{@code automapSaveHeader},
 * {@code automapGetHeader} and the display-map selection state.
 *
 * <p>Vectors: the {@code [automap]} section in
 * {@code docker/refharness/golden/v113.txt} pins the header parse of the
 * real {@code MAPS\AUTOMAP.DB}: version 1, data size 1925, and a
 * {@code [160][3]} offset table of which only 9 entries are populated (with
 * {@code -1}, a backward seek, rather than the {@code 0} the header comment
 * describes).
 *
 * <p>NOTES / seams:
 * <li>The offset table is written big-endian: {@code _db_freadInt} is
 * {@code fileReadInt32}, and {@code automapLoadHeader} rejects any version
 * other than 1 after reading the whole table.</li>
 * <li>{@code automapLoad}/{@code automapSave} only move
 * {@code gAutomapFlags} (one int) — they belong to the save file, so they are
 * ported but not vectored here.</li>
 * <li>The render half ({@code automapShow},
 * {@code automapRenderInPipboyWindow}, {@code _automapDisplayMap},
 * {@code _decode_map_data}, {@code _copy_file_data}) needs a display and
 * stays unported; only the file and selection paths live here.</li>
 * <li>{@code automapEntryIsValid} is file-static in C and gates the offset
 * table on both ids; the same bounds are expressed by
 * {@link #AUTOMAP_MAP_COUNT} and the elevation check below.</li>
 */
public final class Automap {

    private Automap() {
    }

    /** AUTOMAP_DB (src/automap.h). */
    public static final String AUTOMAP_DB = "AUTOMAP.DB";

    /** AUTOMAP_MAP_COUNT (src/automap.h). */
    public static final int AUTOMAP_MAP_COUNT = 160;

    /** The only header version C accepts. */
    static final int AUTOMAP_VERSION = 1;

    /** Port of {@code AutomapHeader} (src/automap.h). */
    static final class AutomapHeader {
        int version;
        int dataSize;
        final int[][] offsets = new int[AUTOMAP_MAP_COUNT][3];
    }

    /** Port of {@code gAutomapHeader} (src/automap.cc). */
    private static final AutomapHeader gAutomapHeader = new AutomapHeader();

    /** Port of {@code gAutomapFlags} (src/automap.cc), save-game state. */
    private static int gAutomapFlags;

    /** Port of {@code gDisplayMap} (src/automap.cc). */
    private static int gDisplayMap = -1;

    /** Port of {@code gAutomapWindow} (src/automap.cc). */
    private static int gAutomapWindow = -1;

    /** Port of {@code automapLoadHeader} (src/automap.cc). */
    private static int automapLoadHeader(XFile.Handle stream) {
        int[] value = new int[1];
        if (Db.fileReadUInt8(stream, value) == -1) {
            return -1;
        }
        gAutomapHeader.version = value[0];

        if (Db.fileReadInt32(stream, value) == -1) {
            return -1;
        }
        gAutomapHeader.dataSize = value[0];

        int[] offsets = new int[AUTOMAP_MAP_COUNT * 3];
        if (Db.fileReadInt32List(stream, offsets, 0, offsets.length) == -1) {
            return -1;
        }
        for (int map = 0; map < AUTOMAP_MAP_COUNT; map++) {
            for (int elevation = 0; elevation < 3; elevation++) {
                gAutomapHeader.offsets[map][elevation] = offsets[map * 3 + elevation];
            }
        }

        if (gAutomapHeader.version != AUTOMAP_VERSION) {
            return -1;
        }

        return 0;
    }

    /** Port of {@code automapGetHeader} (src/automap.cc). */
    static int automapGetHeader(AutomapHeader[] out) {
        String path = "MAPS\\" + AUTOMAP_DB;
        XFile.Handle stream = Db.fileOpen(path, "rb");
        if (stream == null) {
            return -1;
        }

        if (automapLoadHeader(stream) == -1) {
            Db.fileClose(stream);
            return -1;
        }

        Db.fileClose(stream);

        if (out != null && out.length > 0) {
            out[0] = gAutomapHeader;
        }
        return 0;
    }

    /** Port of {@code automapEntryIsValid} (src/automap.cc). */
    static boolean automapEntryIsValid(int map, int elevation) {
        return map >= WorldmapDefs.MAP_FIRST && map < AUTOMAP_MAP_COUNT
                && elevation >= 0 && elevation < 3;
    }

    /** Port of {@code automapSetDisplayMap} (src/automap.cc). */
    public static void automapSetDisplayMap(int map, boolean available) {
        if (available) {
            gDisplayMap = map;
        }
    }

    /** Port of {@code automapGetWindow} (src/automap.cc). */
    public static int automapGetWindow() {
        return gAutomapWindow;
    }

    /** Port of {@code automapLoad} (src/automap.cc): the save-game flag word. */
    public static int automapLoad(XFile.Handle stream) {
        int[] value = new int[1];
        if (Db.fileReadInt32(stream, value) == -1) {
            return -1;
        }
        gAutomapFlags = value[0];
        return 0;
    }

    /** Port of {@code automapSave} (src/automap.cc). */
    public static int automapSave(XFile.Handle stream) {
        return Db.fileWriteInt32(stream, gAutomapFlags);
    }

    /** Test seam: fresh-process mirror. */
    static void resetForTests() {
        gAutomapHeader.version = 0;
        gAutomapHeader.dataSize = 0;
        for (int map = 0; map < AUTOMAP_MAP_COUNT; map++) {
            for (int elevation = 0; elevation < 3; elevation++) {
                gAutomapHeader.offsets[map][elevation] = 0;
            }
        }
        gAutomapFlags = 0;
        gDisplayMap = -1;
        gAutomapWindow = -1;
    }
}