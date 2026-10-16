package org.fallout2ce;

/**
 * Stat configured bounds, mirroring the pure parts of {@code src/stat.cc}.
 * The stat engine itself comes with the game phase; {@code statsInit} needs
 * the message/pc stacks, so these tables stay zero-initialized until set,
 * exactly like the headless C++ harness state.
 */
public final class Stat {

    private Stat() {
    }

    public static final int STAT_ERR_INVALID_STAT = -5;

    // Configured bounds tables (src/stat.cc: pcStatMaximums/Minimums,
    // npcStatMaximums/Minimums). Zero-init mirrors the C++ statics before
    // statsInit runs.
    private static final int[] PC_STAT_MAXIMUMS = new int[StatDefs.SAVEABLE_STAT_COUNT];
    private static final int[] PC_STAT_MINIMUMS = new int[StatDefs.SAVEABLE_STAT_COUNT];
    private static final int[] NPC_STAT_MAXIMUMS = new int[StatDefs.SAVEABLE_STAT_COUNT];
    private static final int[] NPC_STAT_MINIMUMS = new int[StatDefs.SAVEABLE_STAT_COUNT];

    /** Port of {@code statGetConfiguredMaximum}: out-of-range stat yields 0. */
    public static int getConfiguredMaximum(int stat, boolean npc) {
        if (stat >= 0 && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            return npc ? NPC_STAT_MAXIMUMS[stat] : PC_STAT_MAXIMUMS[stat];
        }
        return 0;
    }

    /** Port of {@code statGetConfiguredMinimum}: out-of-range stat yields 0. */
    public static int getConfiguredMinimum(int stat, boolean npc) {
        if (stat >= 0 && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            return npc ? NPC_STAT_MINIMUMS[stat] : PC_STAT_MINIMUMS[stat];
        }
        return 0;
    }

    /** Port of {@code statSetPcMaximum}: out-of-range stat is ignored. */
    public static void setPcMaximum(int stat, int maximum) {
        if (stat >= 0 && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            PC_STAT_MAXIMUMS[stat] = maximum;
        }
    }

    /** Port of {@code statSetPcMinimum}: out-of-range stat is ignored. */
    public static void setPcMinimum(int stat, int minimum) {
        if (stat >= 0 && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            PC_STAT_MINIMUMS[stat] = minimum;
        }
    }

    /** Test seam: zero the bounds tables (fresh-process mirror). */
    static void resetForTests() {
        java.util.Arrays.fill(PC_STAT_MAXIMUMS, 0);
        java.util.Arrays.fill(PC_STAT_MINIMUMS, 0);
        java.util.Arrays.fill(NPC_STAT_MAXIMUMS, 0);
        java.util.Arrays.fill(NPC_STAT_MINIMUMS, 0);
    }
}
