package org.fallout2ce;

import org.fallout2ce.ObjTypes.GameObject;

/**
 * Hit-location accuracy penalties, target highlight, and the combat-id
 * search, mirroring the pure slice of {@code src/combat.cc}.
 *
 * <p>Only the data-table accessors are ported
 * ({@code combat_get_hit_location_penalty},
 * {@code combat_set_hit_location_penalty},
 * {@code combat_reset_hit_location_penalty}, {@code combatGetTargetHighlight})
 * plus the stateless linear scan {@code _find_cid}.
 * {@code combatInit} is deliberately not ported: it touches {@code gDude} and
 * segfaults headless. The tables therefore start zeroed (BSS mirror), exactly
 * as the harness observes before {@code combat_reset_hit_location_penalty}
 * restores the compiled defaults.
 */
public final class Combat {

    private Combat() {
    }

    /**
     * Compiled defaults, mirroring {@code hit_location_penalty_default}
     * ({@code src/combat.cc}), index = hit location id.
     */
    private static final int[] HIT_LOCATION_PENALTY_DEFAULT = {
        -40, // HEAD
        -30, // LEFT_ARM
        -30, // RIGHT_ARM
        0, // TORSO
        -20, // RIGHT_LEG
        -20, // LEFT_LEG
        -60, // EYES
        -30, // GROIN
        0, // UNCALLED
    };

    /** {@code hit_location_penalty} (BSS: starts zeroed). */
    private static final int[] hitLocationPenalty = new int[CombatDefs.HIT_LOCATION_COUNT];

    /** {@code _combat_highlight} (BSS 0). */
    private static int combatHighlight;

    /**
     * Port of {@code combat_get_hit_location_penalty}: an invalid hit location
     * yields 0.
     */
    public static int getHitLocationPenalty(int hitLocation) {
        if (CombatDefs.hitLocationIsValid(hitLocation)) {
            return hitLocationPenalty[hitLocation];
        }

        return 0;
    }

    /**
     * Port of {@code combat_set_hit_location_penalty}: an invalid hit location
     * is ignored.
     */
    public static void setHitLocationPenalty(int hitLocation, int penalty) {
        if (CombatDefs.hitLocationIsValid(hitLocation)) {
            hitLocationPenalty[hitLocation] = penalty;
        }
    }

    /** Port of {@code combat_reset_hit_location_penalty}. */
    public static void resetHitLocationPenalty() {
        for (int hitLocation = CombatDefs.HIT_LOCATION_FIRST;
                hitLocation < CombatDefs.HIT_LOCATION_COUNT; hitLocation++) {
            hitLocationPenalty[hitLocation] = HIT_LOCATION_PENALTY_DEFAULT[hitLocation];
        }
    }

    /** Port of {@code combatGetTargetHighlight}. */
    public static int getTargetHighlight() {
        return combatHighlight;
    }

    /**
     * Port of {@code _find_cid} ({@code src/combat.cc:0x420E24}): pure linear
     * scan returning the first index {@code >= startIndex} whose
     * {@code cid} matches, else {@code critterListLength}. Fresh objects
     * carry {@code cid -1} ({@code src/object.cc} {@code obj_allocate}),
     * so a fresh list matches at 0 and a missing cid yields the length.
     */
    public static int findCid(int startIndex, int cid, GameObject[] critterList, int critterListLength) {
        int index;
        for (index = startIndex; index < critterListLength; index++) {
            if (critterList[index].cid == cid) {
                break;
            }
        }
        return index;
    }

    /** Test seam: fresh-process mirror (BSS zeroes: penalties + highlight). */
    static void resetForTests() {
        java.util.Arrays.fill(hitLocationPenalty, 0);
        combatHighlight = 0;
    }
}
