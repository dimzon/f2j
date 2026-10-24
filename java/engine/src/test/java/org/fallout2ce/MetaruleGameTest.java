package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The game-flag metarules of {@code op_metarule}: SIGNAL_END_GAME (13),
 * FIRST_RUN (14) and IS_LOADGAME (22).
 */
class MetaruleGameTest {

    private static Program.ProgramValue value() {
        return new Program.ProgramValue();
    }

    @Test
    void gameFlags() {
        GameScriptApi api = new GameScriptApi();
        GameScriptApi.gameUserWantsToQuit = GameScriptApi.GAME_QUIT_REQUEST_NONE;

        // FIRST_RUN: the header starts with no saved flag.
        Map.gMapHeader.flags = 0;
        assertEquals(1, api.metarule(14, value()));

        Map.gMapHeader.flags |= 0x01; // MAP_HEADER_SAVED
        assertEquals(0, api.metarule(14, value()));

        // IS_LOADGAME: no game phase, never loading.
        assertEquals(0, api.metarule(22, value()));

        // SIGNAL_END_GAME: sets the quit-request flag of the game.
        assertEquals(0, api.metarule(13, value()));
        assertEquals(GameScriptApi.GAME_QUIT_REQUEST_MAIN_MENU,
            GameScriptApi.gameUserWantsToQuit);
    }
}
