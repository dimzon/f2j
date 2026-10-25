package org.fallout2ce;

/**
 * Port of { gCriticalHitTables } and { gPlayerCriticalHitTable }
 * (src/combat.cc). The tables are kept in small generated fragments so no
 * static initializer overflows the JVM method size limit; this class
 * combines them.
 */
public final class CriticalHitTables {

    private CriticalHitTables() {
    }

    public static final int[][][][] CRITICAL_HIT_TABLES = combine();

    public static final int[][][] CRITICAL_HIT_TABLE_PLAYER = CriticalHitTablesPlayer.PART;

    private static int[][][][] combine() {
        int[][][][] out = new int[38][9][6][7];
        int offset = 0;
        copy(out, 0, CriticalHitTablesPart0.PART);
        copy(out, 8, CriticalHitTablesPart1.PART);
        copy(out, 16, CriticalHitTablesPart2.PART);
        copy(out, 24, CriticalHitTablesPart3.PART);
        copy(out, 32, CriticalHitTablesPart4.PART);

        return out;
    }

    private static void copy(int[][][][] dst, int offset, int[][][][] src) {
        for (int k = 0; k < src.length; k++) {
            for (int loc = 0; loc < 9; loc++) {
                for (int e = 0; e < 6; e++) {
                    System.arraycopy(src[k][loc][e], 0,
                            dst[offset + k][loc][e], 0, 7);
                }
            }
        }
    }
}
