package org.fallout2ce;

/**
 * NPC reaction slice, mirroring {@code src/reaction.cc} /
 * {@code src/reaction.h}: reaction levels off thresholds.
 *
 * <p>Only the harness-vectored surface is ported ({@code [lightreact]} in
 * {@code docker/refharness/golden/v51.txt}, produced by
 * {@code sectionLightReact} in {@code docker/refharness/ref_main.cc}):
 * {@code reactionTranslateValue}, {@code reactionSetThresholds},
 * {@code reactionResetThresholds}, {@code _reaction_influence_}
 * (returns 0).
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>Threshold strictness: C uses strict {@code >} ({@code value >
 * good} yields GOOD, else {@code value > neutral} yields NEUTRAL, else
 * BAD). Boundary values fall DOWN a level ({@code -10} with defaults
 * is BAD, {@code 0} with custom {@code (0, 100)} is BAD); reproduced
 * exactly and asserted.</li>
 * <li>{@code reactionSetThresholds} emits C {@code debugPrint} stderr
 * noise ({@code INFO: Reaction: set thresholds ...}, twice in the
 * golden tail: once from the explicit custom call, once from the reset
 * path which delegates to the setter). Java stays silent (no
 * {@link Debug} call), matching every other port's stderr-noise rule;
 * the vectors only assert the translated levels.</li>
 * <li>{@code reactionSetValue} / {@code reactionGetValue} are OMITTED:
 * they need the script local-var engine
 * ({@code scriptSetLocalVar}/{@code scriptGetLocalVar} on
 * {@code critter->sid}, {@code src/scripts.cc}) plus the game
 * {@code Object} port, and no {@code script*LocalVar} seam exists
 * anywhere in {@code java/} (verified by grep). They will come with
 * the Scripts/Object ports.</li>
 * </ul>
 */
public final class Reaction {

    private Reaction() {
    }

    /** Port of {@code NpcReaction} (declaration order = C values 0-2). */
    public enum NpcReaction {
        BAD(0),
        NEUTRAL(1),
        GOOD(2);

        private final int id;

        NpcReaction(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final int DEFAULT_NEUTRAL_THRESHOLD = -10;
    private static final int DEFAULT_GOOD_THRESHOLD = 10;

    private static int neutralReactionThreshold = DEFAULT_NEUTRAL_THRESHOLD;
    private static int goodReactionThreshold = DEFAULT_GOOD_THRESHOLD;

    /**
     * Port of {@code reactionTranslateValue} (0x4A29E8): strict
     * {@code >} against the good then neutral thresholds, else BAD.
     * (C comment: the original had several redundant sub-BAD thresholds
     * that all mapped to BAD.)
     */
    public static NpcReaction reactionTranslateValue(int value) {
        if (value > goodReactionThreshold) {
            return NpcReaction.GOOD;
        } else if (value > neutralReactionThreshold) {
            return NpcReaction.NEUTRAL;
        } else {
            return NpcReaction.BAD;
        }
    }

    /**
     * Port of {@code reactionSetThresholds}: stores both thresholds.
     * Deliberately silent: C's {@code debugPrint} stderr line is not
     * reproduced in Java.
     */
    public static void reactionSetThresholds(int neutralThreshold, int goodThreshold) {
        neutralReactionThreshold = neutralThreshold;
        goodReactionThreshold = goodThreshold;
    }

    /**
     * Port of {@code reactionResetThresholds}: delegates to the setter
     * with defaults (-10/10), exactly like C (so C prints its second
     * INFO line from inside the setter; Java stays silent).
     */
    public static void reactionResetThresholds() {
        reactionSetThresholds(DEFAULT_NEUTRAL_THRESHOLD, DEFAULT_GOOD_THRESHOLD);
    }

    /** Port of {@code _reaction_influence_} (0x4A29F0): returns 0. */
    public static int reactionInfluence() {
        return 0;
    }

    static void resetForTests() {
        neutralReactionThreshold = DEFAULT_NEUTRAL_THRESHOLD;
        goodReactionThreshold = DEFAULT_GOOD_THRESHOLD;
    }
}
