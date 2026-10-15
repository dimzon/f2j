package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Pure unit tests for {@code Ticks} ({@code getTicksBetween} quirk). */
class TicksTest {

    @AfterEach
    void reset() {
        Ticks.resetForTests();
    }

    @Test
    void between() {
        assertEquals(0, Ticks.getTicksBetween(100, 100));
        assertEquals(50, Ticks.getTicksBetween(150, 100));
        // Wrapped (negative) difference saturates, it does not wrap.
        assertEquals(Integer.MAX_VALUE, Ticks.getTicksBetween(100, 150));
    }

    @Test
    void inputsAre32Bit() {
        assertEquals(1, Ticks.getTicksBetween(0x1_0000_0001L, 0x1_0000_0000L));
        Ticks.clock = () -> 0x1_FFFF_FFFFL;
        assertEquals(0xFFFFFFFFL, Ticks.getTicks());
    }

    @Test
    void since() {
        long[] now = { 1000L };
        Ticks.clock = () -> now[0];
        assertEquals(0, Ticks.getTicksSince(1000));
        now[0] = 1250;
        assertEquals(250, Ticks.getTicksSince(1000));
        // Clock went backwards (wrap): saturates.
        now[0] = 500;
        assertEquals(Integer.MAX_VALUE, Ticks.getTicksSince(1000));
    }
}
