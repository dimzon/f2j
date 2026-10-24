package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * {@code [animovetile]} vectors from
 * {@code docker/refharness/golden/v149.txt}, produced by
 * {@code sectionAnimMoveTile} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>There is no animation queue in the port, so an object that would walk
 * teleports to the destination instead, on its own elevation. The guards
 * are the original's: nothing without an object, nothing to a tile of
 * nowhere (-1 and below), nothing for an object that is not an active
 * critter. The walk/run choice ({@code flags} 0 walks, anything else runs,
 * {@code 0x10} only clears the queue first) collapses into the same
 * teleport. The in-combat and script-context gates stay dormant: combat
 * state and the script registry are game phase.
 *
 * <p>Setup mirrors {@link CritterStateTest}: the dude comes from
 * {@code objectsInit} with a real critter proto.
 */
class GameAnimMoveTileTest {

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
    void anActiveCritterWalksToTheTile() {
        dude.data.critter.hp = 10;
        dude.data.critter.combat.results = Dam.NONE;

        int[] flags = { 0, 1, 0x10, 0x11 };
        int[] tiles = { 3015, 3021, 3015, 3021 };
        for (int i = 0; i < flags.length; i++) {
            api.animateMoveObjToTile(dude, tiles[i], flags[i]);
            assertEquals(tiles[i], dude.tile);
        }
    }

    @Test
    void theGuardsHoldTheCritterBack() {
        dude.data.critter.hp = 10;
        dude.data.critter.combat.results = Dam.NONE;
        int home = dude.tile;

        api.animateMoveObjToTile(dude, -1, 0);
        api.animateMoveObjToTile(dude, -2, 0);
        assertEquals(home, dude.tile);

        dude.data.critter.combat.results = Dam.KNOCKED_OUT;
        api.animateMoveObjToTile(dude, 3015, 0);
        assertEquals(home, dude.tile);
        dude.data.critter.combat.results = Dam.NONE;

        api.animateMoveObjToTile(null, 3015, 0);

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoTypes.ProtoId.ofPid(40)));
        api.animateMoveObjToTile(stimSlot[0], 3015, 0);
        assertEquals(home, dude.tile);
    }
}
