package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * {@code [perkrank]} vectors from
 * {@code docker/refharness/golden/v154.txt}, produced by
 * {@code sectionPerkRank} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The perk engine (src/perk.cc) keeps a rank table per party member
 * (the dude is index 0). A fresh game zeroes every rank; an invalid perk
 * reads 0; a critter that is not a party member falls back to the dude's
 * slot. A rank of 0 means "does not have the perk".
 */
class GamePerkRankTest {

    private GameObject dude;
    private GameObject dog;

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
        Combat.resetForTests();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();

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

        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoTypes.ProtoId.ofPid(16777558)));
        dog = dogSlot[0];
    }

    @Test
    void theVectorsOfAFreshGame() {
        assertEquals(26, PartyMembers.descriptionsLength);

        assertEquals(0, Perk.perkGetRank(dude, 0)); // awareness
        assertEquals(0, Perk.perkGetRank(dude, 1)); // bonus hth attacks
        assertEquals(0, Perk.perkGetRank(dude, 3)); // bonus move
        assertEquals(0, Perk.perkGetRank(dude, Perk.PERK_INVALID));
        assertFalse(Perk.perkHasRank(dude, 0));
        assertEquals(0, Perk.perkGetRank(dog, 3));
    }

    @Test
    void validityAndRanks() {
        assertTrue(Perk.perkIsValid(0));
        assertTrue(Perk.perkIsValid(Perk.PERK_COUNT - 1));
        assertFalse(Perk.perkIsValid(Perk.PERK_COUNT));
        assertFalse(Perk.perkIsValid(Perk.PERK_INVALID));
    }
}
