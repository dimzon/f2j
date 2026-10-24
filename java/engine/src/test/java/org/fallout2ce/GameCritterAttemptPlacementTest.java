package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * {@code [critterplace]} vectors from
 * {@code docker/refharness/golden/v150.txt}, produced by
 * {@code sectionCritterPlace} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The critter is parked at tile 0 first and then placed near the tile.
 * A free tile takes it as is; a blocked one lands it on the last search
 * candidate - six steps in the last rotation (1815 from 3015) - because
 * the search as written never stops at a hit. A tile of nowhere parks it
 * but settles nothing. The combat-delete tail stays deferred: combat is
 * game phase.
 *
 * <p>Setup mirrors {@link CritterStateTest}: the dude comes from
 * {@code objectsInit} with a real critter proto.
 */
class GameCritterAttemptPlacementTest {

    private GameScriptApi api;
    private GameObject dude;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        Map.resetForTests();

        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;

        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("master.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("patch000.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("critter.dat")));
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());
        assertEquals(0, Mouse.mouseInit());

        byte[] tileBuf = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf, 64, 64, 64,
                rect -> {
                }));

        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));
        dude = ObjectSystem.getDude();
        api = new GameScriptApi();
    }

    @Test
    void aFreeTileTakesTheCritter() {
        dude.data.critter.hp = 10;
        dude.data.critter.combat.results = Dam.NONE;

        assertEquals(0, api.critterAttemptPlacement(dude, 3015, dude.elevation));
        assertEquals(3015, dude.tile);
    }

    @Test
    void aBlockedTileLandsOnTheLastCandidate() {
        dude.data.critter.hp = 10;
        dude.data.critter.combat.results = Dam.NONE;
        assertEquals(0, ObjectSystem.objectSetLocation(dude, 3021, dude.elevation));

        GameObject[] blockerSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(blockerSlot,
                ProtoTypes.ProtoId.ofPid(16777558)));
        assertNotNull(blockerSlot[0]);
        assertEquals(0, ObjectSystem.objectSetLocation(blockerSlot[0], 3015, dude.elevation));

        assertEquals(0, api.critterAttemptPlacement(dude, 3015, dude.elevation));
        assertEquals(1815, dude.tile);
    }

    @Test
    void nowhereParksButSettlesNowhere() {
        dude.data.critter.hp = 10;
        dude.data.critter.combat.results = Dam.NONE;

        assertEquals(-1, api.critterAttemptPlacement(dude, -1, dude.elevation));
        assertEquals(0, dude.tile);

        assertEquals(-1, api.critterAttemptPlacement(null, 3015, dude.elevation));
    }
}
