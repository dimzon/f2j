package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code mapScroll} (map.cc), pinned against the C++ section
 * {@code [mapscroll]} in the ref harness (golden v178).
 *
 * <p>The 33ms gate, the screen-space offset math and the center projection/
 * recenter are real; the buffer blit and the refresh tails are render seams.
 * The scroll timestamp is probed so both sides of the gate are deterministic.
 */
class GameMapScrollTest {

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

        // Golden state: the loaded map centers on 20100.
        assertEquals(0, TileMath.tileSetCenter(20100, TileMath.TILE_SET_CENTER_REFRESH_WINDOW));
        Map.setIsoWindowScrollTimestampForTests(Ticks.getTicks() - 1000);
    }

    @Test
    void aZeroScrollReturnsMinusOneWithoutMoving() {
        assertEquals(-1, Map.mapScroll(0, 0));
        assertEquals(20100, TileMath.gCenterTile);
    }

    @Test
    void theScrollMovesTheCenterPerDirection() {
        assertEquals(0, Map.mapScroll(1, 0));
        assertEquals(20299, TileMath.gCenterTile);

        Map.setIsoWindowScrollTimestampForTests(Ticks.getTicks() - 1000);
        assertEquals(0, Map.mapScroll(0, 1));
        assertEquals(20500, TileMath.gCenterTile);

        Map.setIsoWindowScrollTimestampForTests(Ticks.getTicks() - 1000);
        assertEquals(0, Map.mapScroll(-1, 0));
        assertEquals(20501, TileMath.gCenterTile);

        Map.setIsoWindowScrollTimestampForTests(Ticks.getTicks() - 1000);
        assertEquals(0, Map.mapScroll(0, -1));
        assertEquals(20100, TileMath.gCenterTile);
    }

    @Test
    void anImmediateSecondScrollIsRefused() {
        Map.setIsoWindowScrollTimestampForTests(Ticks.getTicks() - 1000);
        assertEquals(0, Map.mapScroll(1, 0));
        // The first call refreshed the timestamp; the next call is within the
        // 33ms gate.
        assertEquals(-2, Map.mapScroll(1, 0));
    }

    @Test
    void aScrollOffTheGridFailsCleanly() {
        Map.setIsoWindowScrollTimestampForTests(Ticks.getTicks() - 1000);
        assertEquals(-1, Map.mapScroll(9999, 0));
        assertEquals(20100, TileMath.gCenterTile);
    }
}
