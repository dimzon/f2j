package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [lightreact]} vectors from
 * {@code docker/refharness/golden/v51.txt} (produced by
 * {@code sectionLightReact}: headless tile-array init plus threshold
 * translation; the ambient setter is skipped in the harness because
 * gDude perks are unavailable headless). Pure state, no game data or
 * window system needed.
 *
 * <p>The golden block is 4 lines: header, the light vector line, TWO
 * embedded {@code INFO: Reaction: set thresholds ...} stderr lines
 * (from the custom-threshold call and from the reset path, which
 * delegates to the setter), then the reaction vector line. Java stays
 * silent on both INFO lines; only the translated levels are asserted.
 */
class LightReactTest {

    @BeforeEach
    void setup() {
        Light.resetForTests();
        Reaction.resetForTests();
    }

    @Test
    void lightVectors() {
        // init=0 ambient=65536 base=655 clamped=65536 true=70000
        // inc=70100 dec=69900 badelev=0 badtile=0 reset=655
        assertEquals(0, Light.lightInit());
        assertEquals(65536, Light.lightGetAmbientIntensity());
        assertEquals(655, Light.lightGetTileIntensity(0, 20100));

        Light.lightSetTileIntensity(0, 20100, 70000);
        assertEquals(65536, Light.lightGetTileIntensity(0, 20100));
        assertEquals(70000, Light.lightGetTrueTileIntensity(0, 20100));

        Light.lightIncreaseTileIntensity(0, 20100, 100);
        assertEquals(70100, Light.lightGetTrueTileIntensity(0, 20100));
        Light.lightDecreaseTileIntensity(0, 20100, 200);
        assertEquals(69900, Light.lightGetTrueTileIntensity(0, 20100));

        assertEquals(0, Light.lightGetTileIntensity(9, 20100));
        assertEquals(0, Light.lightGetTileIntensity(0, -1));

        // Guarded no-op: invalid elevation write must not disturb state.
        Light.lightSetTileIntensity(9, 20100, 5);
        assertEquals(69900, Light.lightGetTrueTileIntensity(0, 20100));

        Light.lightResetTileIntensity();
        assertEquals(655, Light.lightGetTileIntensity(0, 20100));
    }

    @Test
    void ambientSetterSeams() {
        // Not vectored (harness skips the setter: gDude perks unavailable
        // headless), but ported faithfully: perk bonus, clamp, refresh.
        List<String> refreshes = new ArrayList<>();
        Light.tileWindowRefresh = () -> refreshes.add("refresh");

        // Ambient starts at MAX: lowering it changes the value and refreshes.
        Light.lightSetAmbientIntensity(16384, true);
        assertEquals(16384, Light.lightGetAmbientIntensity());
        assertEquals(List.of("refresh"), refreshes);

        // Same value: no refresh.
        Light.lightSetAmbientIntensity(16384, true);
        assertEquals(List.of("refresh"), refreshes);

        // Ceiling clamp: 100000 normalizes to MAX (a change), refreshes once.
        Light.lightSetAmbientIntensity(100000, true);
        assertEquals(65536, Light.lightGetAmbientIntensity());
        assertEquals(List.of("refresh", "refresh"), refreshes);

        // shouldUpdateScreen=false: no refresh even on change.
        Light.lightSetAmbientIntensity(16384, false);
        assertEquals(16384, Light.lightGetAmbientIntensity());
        assertEquals(List.of("refresh", "refresh"), refreshes);

        // Floor clamp (no screen update requested).
        Light.lightSetAmbientIntensity(0, false);
        assertEquals(16384, Light.lightGetAmbientIntensity());

        // Night Vision rank seam: 1 rank * (65536 * 20 / 100) = +13107.
        Light.dudeNightVisionRank = 1;
        Light.lightSetAmbientIntensity(16384, false);
        assertEquals(16384 + 13107, Light.lightGetAmbientIntensity());
    }

    @Test
    void reactionVectors() {
        // bad=0 edge=0 neutral=1 good=2 custom=0,1,2 reset=1
        assertEquals(Reaction.NpcReaction.BAD, Reaction.reactionTranslateValue(-50));
        // Strict '>' boundary: -10 with defaults is BAD, not NEUTRAL.
        assertEquals(Reaction.NpcReaction.BAD, Reaction.reactionTranslateValue(-10));
        assertEquals(Reaction.NpcReaction.NEUTRAL, Reaction.reactionTranslateValue(0));
        assertEquals(Reaction.NpcReaction.GOOD, Reaction.reactionTranslateValue(11));

        // C prints INFO here (custom thresholds); Java silent.
        Reaction.reactionSetThresholds(0, 100);
        // Strict '>' boundary again: 0 with (0,100) is BAD.
        assertEquals(Reaction.NpcReaction.BAD, Reaction.reactionTranslateValue(0));
        assertEquals(Reaction.NpcReaction.NEUTRAL, Reaction.reactionTranslateValue(50));
        assertEquals(Reaction.NpcReaction.GOOD, Reaction.reactionTranslateValue(101));

        // C prints INFO again from inside the setter; Java silent.
        Reaction.reactionResetThresholds();
        assertEquals(Reaction.NpcReaction.NEUTRAL, Reaction.reactionTranslateValue(0));

        assertEquals(0, Reaction.reactionInfluence());
    }
}
