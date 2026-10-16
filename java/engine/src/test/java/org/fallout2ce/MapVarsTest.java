package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [mapvars]} vectors from
 * {@code docker/refharness/golden/v76.txt} (produced by
 * {@code sectionMapVars} in {@code docker/refharness/ref_main.cc}):
 * out-of-range global get, local alloc(3), local set/get of 42, bad
 * local 99, then the tile-relight pair (clamped set, rebuild reset).
 * Pure state, no game data or window system needed.
 *
 * <p>The golden block is 4 lines: header, TWO embedded
 * {@code INFO: ERROR: attempt to reference ...} stderr lines (C
 * {@code debugPrint} on the global 0 get and the local 99 get), then
 * {@code glob=-1 alloc=0 set=0 get=0:42 bad=-1} and
 * {@code pre=65536 post=655}. Java stays silent on both INFO lines; only
 * the translated vectors are asserted.
 */
class MapVarsTest {

    @BeforeEach
    void setup() {
        MapVars.resetForTests();
        Light.resetForTests();
        ObjectSystem.resetForTests();
    }

    @Test
    void mapvarsVectors() {
        // Global length stays 0 headless: get misses (C prints INFO here;
        // Java silent).
        MapVars.ProgramValue out = new MapVars.ProgramValue();
        out.opcode = MapVars.VALUE_TYPE_INT;
        out.integerValue = -999;
        int globRc = MapVars.mapGetGlobalVar(0, out);

        // Locals grow on demand: alloc returns the old length as base.
        int allocRc = MapVars.mapAllocLocalVars(3);

        int setRc = MapVars.mapSetLocalVar(1, new MapVars.ProgramValue(42));

        MapVars.ProgramValue gotVal = new MapVars.ProgramValue();
        gotVal.opcode = MapVars.VALUE_TYPE_INT;
        gotVal.integerValue = -999;
        int getRc = MapVars.mapGetLocalVar(1, gotVal);

        // Out-of-range local get misses (C prints INFO here; Java silent).
        int badRc = MapVars.mapGetLocalVar(99, gotVal);

        assertEquals(
                "glob=-1 alloc=0 set=0 get=0:42 bad=-1",
                "glob=" + globRc
                        + " alloc=" + allocRc
                        + " set=" + setRc
                        + " get=" + getRc + ":" + gotVal.integerValue
                        + " bad=" + badRc);

        // Rebuild resets tile light then re-applies (light-0 objects
        // reapply nothing headless).
        Light.lightSetTileIntensity(0, 20100, 70000);
        int preRebuild = Light.lightGetTileIntensity(0, 20100);
        ObjectSystem.objRebuildAllLight();
        int postRebuild = Light.lightGetTileIntensity(0, 20100);
        assertEquals("pre=65536 post=655", "pre=" + preRebuild + " post=" + postRebuild);
    }

    @Test
    void pointerRoundTrip() {
        // Non-vectored PTR-branch faithfulness: INT-only vectors never
        // touch this, but the union shape is ported exactly.
        assertEquals(0, MapVars.mapAllocLocalVars(2));

        Object token = new Object();
        MapVars.ProgramValue ptrVal = new MapVars.ProgramValue(token);
        assertEquals(0, MapVars.mapSetLocalVar(0, ptrVal));

        MapVars.ProgramValue got = new MapVars.ProgramValue();
        assertEquals(0, MapVars.mapGetLocalVar(0, got));
        assertEquals(MapVars.VALUE_TYPE_PTR, got.opcode);
        assertEquals(token, got.pointerValue);

        // Overwriting with INT clears the pointer slot.
        assertEquals(0, MapVars.mapSetLocalVar(0, new MapVars.ProgramValue(7)));
        MapVars.ProgramValue gotInt = new MapVars.ProgramValue();
        assertEquals(0, MapVars.mapGetLocalVar(0, gotInt));
        assertEquals(MapVars.VALUE_TYPE_INT, gotInt.opcode);
        assertEquals(7, gotInt.integerValue);
    }
}
