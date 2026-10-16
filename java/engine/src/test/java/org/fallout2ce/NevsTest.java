package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [nevsevents]} vectors from
 * {@code docker/refharness/golden/v83.txt}, produced by
 * {@code sectionNevsEvents} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Pure: no game data, no window system (null programs only exercise the
 * registry bookkeeping; firing needs the interpreter). The flow mirrors the
 * harness exactly: initonce, add {@code ev1} twice (overwrite, still 0),
 * signal/clear the missing name, clear {@code ev1}, then fill all 40 slots
 * ({@code ev00}..{@code ev39}), overflow with {@code over}, clear
 * {@code ev07}, re-add {@code over}, and close. Asserts both vector lines
 * verbatim.
 *
 * <p>The four {@code INFO: nevs_signal(...)} /
 * {@code INFO: nevs_clearevent(...)} stderr lines in the golden block are C
 * {@code debugPrint} noise; Java stays silent per the established rule.
 */
class NevsTest {

    @BeforeEach
    void setup() {
        Nevs.resetForTests();
    }

    @AfterEach
    void teardown() {
        Nevs.resetForTests();
    }

    @Test
    void nevsEventVectors() {
        Nevs.nevsInitonce();

        int add1 = Nevs.nevsAddEvent("ev1", null, 3, Nevs.NevsType.EVENT);
        int addAgain = Nevs.nevsAddEvent("ev1", null, 5, Nevs.NevsType.HANDLER);
        // NOTE: signalling a FOUND event would log its raw pointer (ASLR
        // noise), so like the harness only missing names are signalled here
        // (same rc either way: null programs never fire).
        int sigMiss = Nevs.nevsSignal("nope");
        int clearMiss = Nevs.nevsClearEvent("nope");
        int clear1 = Nevs.nevsClearEvent("ev1");
        assertEquals("add=0,0 signal=1 clearmiss=1 clear=0",
                String.format("add=%d,%d signal=%d clearmiss=%d clear=%d",
                        add1, addAgain, sigMiss, clearMiss, clear1));

        int fillFail = 0;
        for (int i = 0; i < 40; i++) {
            if (Nevs.nevsAddEvent(String.format("ev%02d", i), null, 1, Nevs.NevsType.EVENT) != 0) {
                fillFail++;
            }
        }
        int overAdd = Nevs.nevsAddEvent("over", null, 1, Nevs.NevsType.EVENT);
        int clearSlot = Nevs.nevsClearEvent("ev07");
        int reAdd = Nevs.nevsAddEvent("over", null, 1, Nevs.NevsType.EVENT);
        assertEquals("fillfail=0 over=1 clearslot=0 readd=0",
                String.format("fillfail=%d over=%d clearslot=%d readd=%d",
                        fillFail, overAdd, clearSlot, reAdd));

        Nevs.nevsClose();
    }

    @Test
    void uninitGuardThrows() {
        // C debugPrint + exit(99) becomes IllegalStateException (see Nevs docs).
        assertThrows(IllegalStateException.class, () -> Nevs.nevsSignal("ev1"));
        assertThrows(IllegalStateException.class,
                () -> Nevs.nevsAddEvent("ev1", null, 1, Nevs.NevsType.EVENT));
        assertThrows(IllegalStateException.class, () -> Nevs.nevsClearEvent("ev1"));
    }
}
