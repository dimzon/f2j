package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * {@code rgb555} vectors cross-checked against
 * {@code docker/refharness/golden/v1.txt}.
 */
class ColorTest {

    @Test
    void rgb555() {
        assertEquals(0, Color.rgb555(0x000000));
        assertEquals(32767, Color.rgb555(0xFFFFFF));
        assertEquals(31744, Color.rgb555(0xFF0000));
        assertEquals(992, Color.rgb555(0x00FF00));
        assertEquals(31, Color.rgb555(0x0000FF));
        assertEquals(32074, Color.rgb555(0xFF5252));
        assertEquals(25439, Color.rgb555(0xC5D6FF));
        assertEquals(2250, Color.rgb555(0x123456));
        assertEquals(15845, Color.rgb555(0x7B7B29));
    }

    @Test
    void tableSizes() {
        assertEquals(768, Color.cmap.length);
        assertEquals(768, Color.systemCmap.length);
        assertEquals(64, Color.currentGammaTable.length);
        assertEquals(256, Color.mappedColor.length);
        assertEquals(32768, Color.colorTable.length);
    }

    @Test
    void flags() {
        assertEquals(0x0000000, Color.DRAW_TEXT_FLAG_NONE);
        assertEquals(0x0010000, Color.DRAW_TEXT_FLAG_SHADOWED);
        assertEquals(0x01000000, Color.DRAW_TEXT_FLAG_REFRESH);
    }

    @Test
    void paletteLoadIsPhase2() {
        // Tables start zeroed (as in C before _initColors); loading needs
        // game files + SDL and is an explicit stub for now.
        assertFalse(Color.initColors());
        assertEquals(0, Color.red());
        Color.colorTable[Color.rgb555(0xFF0000)] = (byte) 0xAB;
        assertEquals(0xAB, Color.red());
        Color.colorsClose();
        assertEquals(0, Color.red());
    }
}
