package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Trait id table, mirroring {@code src/trait_defs.h}.
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 */
public final class TraitDefs {

    public static final int TRAITS_MAX_SELECTED_COUNT = 2;

    public static final int TRAIT_COUNT = 16;
    public static final int TRAIT_FIRST = 0;

    private TraitDefs() {
    }

    public enum Trait {
        TRAIT_INVALID(-1),
        TRAIT_FAST_METABOLISM(0),
        TRAIT_BRUISER(1),
        TRAIT_SMALL_FRAME(2),
        TRAIT_ONE_HANDER(3),
        TRAIT_FINESSE(4),
        TRAIT_KAMIKAZE(5),
        TRAIT_HEAVY_HANDED(6),
        TRAIT_FAST_SHOT(7),
        TRAIT_BLOODY_MESS(8),
        TRAIT_JINXED(9),
        TRAIT_GOOD_NATURED(10),
        TRAIT_CHEM_RELIANT(11),
        TRAIT_CHEM_RESISTANT(12),
        TRAIT_SEX_APPEAL(13),
        TRAIT_SKILLED(14),
        TRAIT_GIFTED(15);

        private final int id;

        Trait(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public Trait next() {
            return TraitDefs.of(id + 1);
        }
    }

    private static final Map<Integer, Trait> BY_ID = new HashMap<>();

    static {
        for (Trait trait : Trait.values()) {
            BY_ID.put(trait.id, trait);
        }
    }

    public static Trait of(int id) {
        return BY_ID.get(id);
    }

    public static boolean traitIsValid(int trait) {
        return trait >= TRAIT_FIRST && trait < TRAIT_COUNT;
    }
}
