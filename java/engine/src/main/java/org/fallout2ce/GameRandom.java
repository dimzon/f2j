package org.fallout2ce;

/**
 * Port of {@code src/random.h} / {@code src/random.cc}.
 *
 * <p>The PRNG is a faithful port of the original Park–Miller shuffle-table
 * generator ({@code ran1}): Java {@code int} is 32-bit two's complement
 * exactly like C {@code int} here (no intermediate overflow: the largest
 * product is {@code 16807 * 127772 = 2147462344 < 2^31}).
 *
 * <p>Seams: {@code randomRoll} needs live game state
 * ({@code gameTimeGetTime}, {@code criticalsNoTimeLimits}); until
 * {@code Game}/{@code Combat} are ported these are injectable providers
 * defaulting to a fresh (day-0) game. {@code randomInit} seeds from the
 * wall clock; only the explicit-{@code seed} path is differential-tested.
 */
public final class GameRandom {

    private GameRandom() {
    }

    /** Port of {@code enum Roll} (order preserved). */
    public enum Roll {
        CRITICAL_FAILURE,
        FAILURE,
        SUCCESS,
        CRITICAL_SUCCESS
    }

    /** From {@code src/scripts.h}: ticks per game day. */
    public static final long GAME_TIME_TICKS_PER_DAY = 864000L;

    // 0x50D4BA: chi-square critical value, alpha=0.05, 24 dof.
    private static final double CHI_SQUARED_95_THRESHOLD = 36.42;
    // 0x50D4C2
    private static final double EXPECTED_BUCKET_HITS = 4000.0;

    // 0x51C694 iy
    private static int iy;
    // 0x6648D0 iv
    private static final int[] iv = new int[32];
    // 0x664950 idum
    private static int idum;

    /** Provides {@code gameTimeGetTime()} until {@code Game} is ported. */
    public interface GameClock {
        long gameTimeGetTime();
    }

    /** Provides {@code criticalsNoTimeLimits()} until {@code Combat} is ported. */
    public interface CriticalsPolicy {
        boolean criticalsNoTimeLimits();
    }

    private static GameClock gameClock = () -> 0L;
    private static CriticalsPolicy criticalsPolicy = () -> false;
    private static java.util.Random stdRand = new java.util.Random();

    /** Test/porting seam. */
    public static void setGameClock(GameClock clock) {
        gameClock = clock;
    }

    /** Test/porting seam. */
    public static void setCriticalsPolicy(CriticalsPolicy policy) {
        criticalsPolicy = policy;
    }

    /** Test seam: deterministic {@code rand()} replacement. */
    public static void setStdRandSeed(long seed) {
        stdRand = new java.util.Random(seed);
    }

    /** Test seam: restore day-0 defaults. */
    public static void resetProviders() {
        gameClock = () -> 0L;
        criticalsPolicy = () -> false;
    }

    // 0x4A2FE0 roll_init
    public static void randomInit() {
        long randomSeed = randomGetSeed();
        stdRand = new java.util.Random((int) randomSeed);

        int pseudorandomSeed = randomInt32();
        randomSeedPrerandomInternal(pseudorandomSeed);

        randomValidatePrerandom();
    }

    // NOTE: Collapsed 0x4A2FFC.
    public static void randomReset() {
    }

    // NOTE: Uncollapsed 0x4A2FFC (returns 0 in C).
    public static void randomExit() {
    }

    // NOTE: Uncollapsed 0x4A2FFC (returns 0 in C).
    public static int randomSave() {
        return 0;
    }

    // NOTE: Uncollapsed 0x4A2FFC (returns 0 in C).
    public static int randomLoad() {
        return 0;
    }

    /**
     * 0x4A3000 roll_check. {@code howMuch} is the {@code int*} out-param
     * ({@code null} allowed, length &ge; 1 otherwise).
     */
    public static Roll randomRoll(int difficulty, int criticalSuccessModifier, int[] howMuch) {
        int delta = difficulty - randomBetween(1, 100);
        Roll result = randomTranslateRoll(delta, criticalSuccessModifier);

        if (howMuch != null) {
            howMuch[0] = delta;
        }

        return result;
    }

    // 0x4A3030 roll_check_critical
    static Roll randomTranslateRoll(int delta, int criticalSuccessModifier) {
        long gameTime = gameClock.gameTimeGetTime();

        Roll roll;
        if (delta < 0) {
            roll = Roll.FAILURE;

            if (criticalsPolicy.criticalsNoTimeLimits() || (gameTime / GAME_TIME_TICKS_PER_DAY) >= 1) {
                // 10% to become critical failure.
                if (randomBetween(1, 100) <= -delta / 10) {
                    roll = Roll.CRITICAL_FAILURE;
                }
            }
        } else {
            roll = Roll.SUCCESS;

            if (criticalsPolicy.criticalsNoTimeLimits() || (gameTime / GAME_TIME_TICKS_PER_DAY) >= 1) {
                // 10% + modifier to become critical success.
                if (randomBetween(1, 100) <= delta / 10 + criticalSuccessModifier) {
                    roll = Roll.CRITICAL_SUCCESS;
                }
            }
        }

        return roll;
    }

    // 0x4A30C0 roll_random
    public static int randomBetween(int min, int max) {
        int result;

        if (min <= max) {
            result = min + getRandom(max - min + 1);
        } else {
            result = max + getRandom(min - max + 1);
        }

        if (result < min || result > max) {
            Debug.print("Random number %d is not in range %d to %d", result, min, max);
            result = min;
        }

        return result;
    }

    // 0x4A30FC ran1
    static int getRandom(int max) {
        int seed = 16807 * (idum % 127773) - 2836 * (idum / 127773);

        if (seed < 0) {
            seed += 0x7FFFFFFF;
        }

        if (seed < 0) {
            seed += 0x7FFFFFFF;
        }

        int idx = iy & 0x1F;
        int value = iv[idx];
        iv[idx] = seed;
        iy = value;
        idum = seed;

        return value % max;
    }

    // 0x4A31A0 roll_set_seed
    public static void randomSeedPrerandom(int seed) {
        if (seed == -1) {
            seed = randomInt32();
        }

        randomSeedPrerandomInternal(seed);
    }

    // 0x4A31C4 random_seed.
    // NOTE: C uses std::rand(); glibc and MSVC disagree there too, and the
    // value only seeds the prerandom table on the wall-clock path, so any
    // hosted RNG is equally faithful here.
    static int randomInt32() {
        return stdRand.nextInt(Integer.MAX_VALUE);
    }

    // 0x4A31E0 seed_generator
    static void randomSeedPrerandomInternal(int seed) {
        int num = seed;
        if (num < 1) {
            num = 1;
        }

        for (int index = 39; index >= 0; index--) {
            num = 16807 * (num % 127773) - 2836 * (num / 127773);

            if (num < 0) {
                num += Integer.MAX_VALUE;
            }

            if (index < 32) {
                iv[index] = num;
            }
        }

        iy = iv[0];
        idum = num;
    }

    // 0x4A3258 timer_read
    static long randomGetSeed() {
        return System.currentTimeMillis();
    }

    // 0x4A3264 check_chi_squared
    static void randomValidatePrerandom() {
        int[] results = new int[25];

        for (int attempt = 0; attempt < 100000; attempt++) {
            int value = randomBetween(1, 25);
            if (value - 1 < 0) {
                Debug.print("I made a negative number %d\n", value - 1);
            }

            results[value - 1]++;
        }

        double chiSquared = 0.0;

        for (int index = 0; index < 25; index++) {
            double contrib = (results[index] - EXPECTED_BUCKET_HITS) * (results[index] - EXPECTED_BUCKET_HITS)
                    / EXPECTED_BUCKET_HITS;
            chiSquared += contrib;
        }

        Debug.print("Chi squared is %f, P = %f at 0.05\n", chiSquared, EXPECTED_BUCKET_HITS);

        if (chiSquared < CHI_SQUARED_95_THRESHOLD) {
            Debug.print("Sequence is random, 95%% confidence.\n");
        } else {
            Debug.print("Warning! Sequence is not random, 95%% confidence.\n");
        }
    }
}
