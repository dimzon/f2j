package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The say-screen and gsay stubs of the game, and the reversed stand.
 *
 * <p>There is nothing to pin against the reference: a stub takes its
 * values off the stack and does nothing, and the stack discipline of all
 * nine blocks is already pinned against the original by
 * {@code GameApiDispatchStackTest}. This test pins that the game answers
 * the nine opcodes and that the nine answers do nothing observable.
 */
class GameSayStubsTest {

    private GameScriptApi api;

    @BeforeEach
    void setup() {
        api = new GameScriptApi();
    }

    @Test
    void theGameAnswersTheSayScreen() {
        assertTrue(api.supports(0x804D & 0x3FF)); // opSayEnd
        assertTrue(api.supports(0x804E & 0x3FF)); // opSayStart
        assertTrue(api.supports(0x8052 & 0x3FF)); // opSayOption
        assertTrue(api.supports(0x8053 & 0x3FF)); // opSayReply
        assertTrue(api.supports(0x8054 & 0x3FF)); // opSayMessage
        assertTrue(api.supports(0x806B & 0x3FF)); // opDisplay

        api.opSayEnd();
        api.opSayStart();
        api.opSayOption(new Program.ProgramValue(0), new Program.ProgramValue(0));
        api.opSayReply(new Program.ProgramValue(0), new Program.ProgramValue(0));
        api.opSayMessage(new Program.ProgramValue(0), new Program.ProgramValue(0));
        api.opDisplay("hello");
    }

    @Test
    void theGameAnswersTheGsayFrameAndTheReversedStand() {
        assertTrue(api.supports(0x811C & 0x3FF)); // _op_gsay_start
        assertTrue(api.supports(0x811D & 0x3FF)); // _op_gsay_end
        assertTrue(api.supports(0x80CD & 0x3FF)); // animate_stand_reverse_obj

        api.opGsayStart();
        api.opGsayEnd();
        api.animateStandReverseObj(null);
    }
}
