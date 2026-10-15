package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Critter id tables, mirroring the pure parts of {@code src/critter.h}.
 * The critter engine ({@code critter.cc}) comes with the game phase.
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 */
public final class Critter {

    private Critter() {
    }

    public static final int RADIATION_LEVEL_COUNT = 6;
    public static final int RADIATION_LEVEL_FIRST = 0;

    public enum RadiationLevel {
        RADIATION_LEVEL_NONE(0),
        RADIATION_LEVEL_MINOR(1),
        RADIATION_LEVEL_ADVANCED(2),
        RADIATION_LEVEL_CRITICAL(3),
        RADIATION_LEVEL_DEADLY(4),
        RADIATION_LEVEL_FATAL(5);

        private final int id;

        RadiationLevel(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public RadiationLevel next() {
            return Critter.ofRadiationLevel(id + 1);
        }
    }

    private static final Map<Integer, RadiationLevel> RADIATION_BY_ID = new HashMap<>();

    static {
        for (RadiationLevel e : RadiationLevel.values()) {
            RADIATION_BY_ID.put(e.id, e);
        }
    }

    public static RadiationLevel ofRadiationLevel(int id) {
        return RADIATION_BY_ID.get(id);
    }

    // CritterFrameId only knows Invalid/Reserved/First; the Goris ids are
    // functional casts in C++, kept as plain ints here.
    public static final int GORIS_COMBAT_BASE_FRAME_ID = 100;
    public static final int GORIS_ROBE_BASE_FRAME_ID = 99;
}
