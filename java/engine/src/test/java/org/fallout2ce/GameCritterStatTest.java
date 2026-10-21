package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The critter stat builtin of the game.
 *
 * <p>A critter that is not there has no stat, and the original answers -1
 * for it ({@code op_get_critter_stat} in the C++ reads the stat of the
 * character and reports the error when there is no character).
 */
class GameCritterStatTest {

    private GameScriptApi api;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        api = new GameScriptApi();
    }

    @Test
    void theStatOfACritter() {
        ObjTypes.GameObject critter = new ObjTypes.GameObject();
        critter.fid = FrmId.ofFid(0).fid();

        // The stat of a critter the game has not set up is nothing.
        assertEquals(0, api.getCritterStat(critter,
            StatDefs.Stat.STAT_STRENGTH.id()));

        // A critter that is not there has no stat at all.
        assertEquals(-1, api.getCritterStat(null, StatDefs.Stat.STAT_STRENGTH.id()));
    }
}
