package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Floor tile rendering ({@code tileRenderFloorsInRect}), pinned against the
 * C++ section {@code [tilerender]} in the ref harness (golden v180).
 *
 * <p>The square fids, the ambient light, the center and the render pass are
 * wired exactly like the harness section; the result is the window buffer's
 * CRC and the non-zero count.
 */
class GameTileRenderTest {

    private byte[] windowBuffer;

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

        windowBuffer = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200,
                windowBuffer, 64, 64, 64, rect -> {
                }));

        Map.TileData[] squareGrid = (Map.TileData[]) TileMath.gTileSquares;
        for (int i = 0; i < 100 * 100; i++) {
            squareGrid[0].tileFid[i] = 1 + (i % 4);
        }
    }

    @Test
    void floorRenderMatchesTheHarnessVector() {
        Light.lightSetAmbientIntensity(65536, false);
        assertEquals(0, TileMath.tileSetCenter(20100,
                TileMath.TILE_SET_CENTER_REFRESH_WINDOW));

        Draw.bufferFill(windowBuffer, 64, 64, 64, 0);
        TileFloorRender.tileRenderFloorsInRect(TileMath.gTileWindowRect, 0);

        int nonZero = 0;
        for (int i = 0; i < 64 * 64; i++) {
            if (windowBuffer[i] != 0) {
                nonZero++;
            }
        }

        // Golden [tilerender]: crc=14E8A553 nonzero=4043
        // px=204,30,13,14,14,14,14,78
        assertEquals(4043, nonZero);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(windowBuffer, 0, windowBuffer.length);
        assertEquals(0x14E8A553L, crc.getValue());
        assertEquals(204, Byte.toUnsignedInt(windowBuffer[8 * 64 + 8]));
        assertEquals(30, Byte.toUnsignedInt(windowBuffer[16 * 64 + 16]));
        assertEquals(13, Byte.toUnsignedInt(windowBuffer[24 * 64 + 24]));
        assertEquals(14, Byte.toUnsignedInt(windowBuffer[32 * 64 + 32]));
        assertEquals(14, Byte.toUnsignedInt(windowBuffer[40 * 64 + 40]));
        assertEquals(14, Byte.toUnsignedInt(windowBuffer[48 * 64 + 48]));
        assertEquals(14, Byte.toUnsignedInt(windowBuffer[56 * 64 + 56]));
        assertEquals(78, Byte.toUnsignedInt(windowBuffer[32 * 64 + 16]));
    }
}
