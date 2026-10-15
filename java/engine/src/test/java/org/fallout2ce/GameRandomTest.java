package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Vectors cross-checked against {@code docker/refharness/golden/v1.txt}.
 */
class GameRandomTest {

    @AfterEach
    void reset() {
        GameRandom.resetProviders();
    }

    @Test
    void seededSequence100() {
        GameRandom.randomSeedPrerandom(12345);
        int[] expected = { 15, 20, 21, 62, 43, 98, 50, 2, 58, 9 };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], GameRandom.randomBetween(1, 100), "index " + i);
        }
    }

    @Test
    void seededSequence25() {
        GameRandom.randomSeedPrerandom(1);
        int[] expected = { 11, 25, 25, 17, 2 };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], GameRandom.randomBetween(1, 25), "index " + i);
        }
    }

    @Test
    void sameSeedSameSequence() {
        GameRandom.randomSeedPrerandom(999);
        int a = GameRandom.randomBetween(1, 1000);
        int b = GameRandom.randomBetween(1, 1000);
        GameRandom.randomSeedPrerandom(999);
        assertEquals(a, GameRandom.randomBetween(1, 1000));
        assertEquals(b, GameRandom.randomBetween(1, 1000));
    }

    @Test
    void degenerateRange() {
        assertEquals(10, GameRandom.randomBetween(10, 10));
    }

    @Test
    void reversedRangeClampsToMin() {
        GameRandom.randomSeedPrerandom(7);
        // Faithful C quirk: reversed ranges always fall back to min
        // (C also debugPrints; the log line itself is backend formatting).
        assertEquals(5, GameRandom.randomBetween(5, 3));
    }

    @Test
    void rollOnDayZeroNeverCritical() {
        // Day-0 defaults: criticals disabled, so difficulty 100 -> SUCCESS,
        // difficulty 1 -> FAILURE, whatever the underlying d% is.
        GameRandom.randomSeedPrerandom(4242);
        int[] howMuch = new int[1];
        assertEquals(GameRandom.Roll.SUCCESS, GameRandom.randomRoll(100, 0, howMuch));
        assertTrue(howMuch[0] >= 0);

        GameRandom.randomSeedPrerandom(4242);
        assertEquals(GameRandom.Roll.FAILURE, GameRandom.randomRoll(1, 0, howMuch));
        assertTrue(howMuch[0] <= 0);
    }

    @Test
    void rollRespectsNullHowMuch() {
        GameRandom.randomSeedPrerandom(11);
        assertNotNull(GameRandom.randomRoll(50, 0, null));
    }

    @Test
    void saveLoadAreNoops() {
        assertEquals(0, GameRandom.randomSave());
        assertEquals(0, GameRandom.randomLoad());
    }
}
