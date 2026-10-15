package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Widget vectors ({@code windowUpdateWidgets} returns 1, delete is a no-op).
 */
class WidgetTest {

    @AfterEach
    void teardown() {
        Widget.resetForTests();
    }

    @Test
    void updateAndDelete() {
        Widget.resetForTests();
        assertEquals(1, Widget.windowUpdateWidgets());
        Widget.windowDeleteWidgets(1);
        assertEquals(1, Widget.windowUpdateWidgets());
    }
}
