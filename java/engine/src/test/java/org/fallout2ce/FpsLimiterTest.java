package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.fallout2ce.spi.Sleeper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Exact frame-budget tests (fake clock + recording sleeper) plus the
 * lower-bound behavior from {@code golden/v15.txt}.
 */
class FpsLimiterTest {

    static class RecordingSleeper implements Sleeper {
        final List<Long> sleeps = new ArrayList<>();

        @Override
        public void sleep(long ms) {
            sleeps.add(ms);
        }
    }

    private final long[] now = { 0L };
    private RecordingSleeper sleeper;

    @AfterEach
    void reset() {
        Ticks.resetForTests();
        Delay.resetForTests();
        FpsLimiter.resetForTests();
    }

    void setup() {
        now[0] = 1000L;
        Ticks.clock = () -> now[0];
        sleeper = new RecordingSleeper();
        FpsLimiter.sleeper = sleeper;
    }

    @Test
    void sleepsRemainderOfFrame() {
        setup();
        FpsLimiter limiter = new FpsLimiter(60);

        limiter.mark();
        now[0] += 5;
        limiter.throttle();

        assertEquals(List.of(11L), sleeper.sleeps);
    }

    @Test
    void fullFrameWhenIdle() {
        setup();
        FpsLimiter limiter = new FpsLimiter(60);

        limiter.mark();
        limiter.throttle();

        assertEquals(List.of(16L), sleeper.sleeps);
    }

    @Test
    void noSleepWhenOverBudget() {
        setup();
        FpsLimiter limiter = new FpsLimiter(60);

        limiter.mark();
        now[0] += 16;
        limiter.throttle();
        now[0] += 100;
        limiter.throttle();

        assertTrue(sleeper.sleeps.isEmpty());
    }

    @Test
    void customFpsBudgets() {
        setup();

        FpsLimiter thirty = new FpsLimiter(30);
        thirty.mark();
        thirty.throttle();
        assertEquals(List.of(33L), sleeper.sleeps);

        sleeper.sleeps.clear();
        FpsLimiter thousand = new FpsLimiter(1000);
        thousand.mark();
        thousand.throttle();
        assertEquals(List.of(1L), sleeper.sleeps);
    }

    @Test
    void defaultIs60() {
        setup();
        FpsLimiter limiter = new FpsLimiter();
        limiter.mark();
        limiter.throttle();
        assertEquals(List.of(16L), sleeper.sleeps);
    }

    @Test
    void wrapAround() {
        now[0] = 0xFFFF_FFF0L;
        Ticks.clock = () -> now[0];
        sleeper = new RecordingSleeper();
        FpsLimiter.sleeper = sleeper;
        FpsLimiter limiter = new FpsLimiter(60);

        limiter.mark();
        // 21ms later in wrapped time: over budget, no sleep (unsigned math).
        now[0] = 0x1_0000_0005L;
        limiter.throttle();

        assertTrue(sleeper.sleeps.isEmpty());
    }

    @Test
    void zeroFpsDividesByZero() {
        setup();
        FpsLimiter limiter = new FpsLimiter(0);
        limiter.mark();
        // C divides unsigned by zero here (UB/crash); Java throws.
        assertThrows(ArithmeticException.class, limiter::throttle);
    }

    @Test
    void delayMs() {
        RecordingSleeper delaySleeper = new RecordingSleeper();
        Delay.sleeper = delaySleeper;

        Delay.delayMs(0);
        Delay.delayMs(-3);
        assertTrue(delaySleeper.sleeps.isEmpty());

        Delay.delayMs(50);
        assertEquals(List.of(50L), delaySleeper.sleeps);
    }
}
