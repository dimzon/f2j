package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The critter stat builtins of the game.
 *
 * <p>A critter that is not there has no stat, and the original answers -1
 * for it. The stat may only be changed on the dude, and the object system
 * keeps the dude as a game-phase global that the port does not have, so
 * the original refuses every critter here as well.
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

    @Test
    void theStatOfACritterCannotBeChanged() {
        ObjTypes.GameObject critter = new ObjTypes.GameObject();
        critter.fid = FrmId.ofFid(0).fid();

        assertEquals(-1, api.setCritterStat(critter, StatDefs.Stat.STAT_STRENGTH.id(), 1));
        assertEquals(-1, api.setCritterStat(null, StatDefs.Stat.STAT_STRENGTH.id(), 1));
    }
}
