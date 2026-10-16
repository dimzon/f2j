package org.fallout2ce;

import java.util.Arrays;

/**
 * Map variable slice, mirroring the var domain of {@code src/map.cc}:
 * {@code mapSetGlobalVar} (0x482220), {@code mapGetGlobalVar} (0x482250),
 * {@code mapSetLocalVar} (0x482280), {@code mapGetLocalVar} (0x4822B0),
 * {@code mapAllocLocalVars} (0x4822E0), over the statics
 * {@code gMapGlobalVarsLength} / {@code gMapLocalVarsLength} /
 * {@code gMapGlobalVars} / {@code gMapLocalVars} /
 * {@code gMapGlobalPointers} / {@code gMapLocalPointers} (BSS zeros/nulls).
 *
 * <p>Vectors: {@code [mapvars]} in
 * {@code docker/refharness/golden/v76.txt}, produced by
 * {@code sectionMapVars} in {@code docker/refharness/ref_main.cc}:
 * {@code glob=-1 alloc=0 set=0 get=0:42 bad=-1} and (via
 * {@link ObjectSystem#objRebuildAllLight}) {@code pre=65536 post=655}.
 * The golden block is 4 lines: header, TWO embedded
 * {@code INFO: ERROR: attempt to reference ...} stderr lines (C
 * {@code debugPrint} on the out-of-range global get and the local 99
 * get), then the two vector lines. Java stays silent on both INFO lines
 * per the established rule.
 *
 * <p>NOTES / mapping decisions (all documented at the use site too):
 * <ul>
 * <li>{@code ProgramValue} home: no reusable Java script value exists.
 * {@code Program} ({@code src/interpreter.h} {@code Program}) is still an
 * opaque stub, and {@code ProtoDefs.ProtoDataMemberValue} is the unrelated
 * {@code ProtoDataMemberValue} int/string union. So the minimal harness
 * surface ({@code opcode} + {@code integerValue} / {@code pointerValue})
 * lives here as the nested {@link ProgramValue} class; the interpreter
 * phase will promote it.</li>
 * <li>Pointer mapping: C {@code void* pointerValue} ({@code nullptr} =
 * no pointer) becomes a Java {@code Object} reference ({@code null} = no
 * pointer). The vectors use INT only, but both PTR branches are ported
 * faithfully: set with {@code VALUE_TYPE_PTR} stores int-slot 0 plus the
 * reference; get with a non-null reference returns {@code VALUE_TYPE_PTR}
 * plus the reference, else {@code VALUE_TYPE_INT} plus the int slot.</li>
 * <li>Alloc growth: C {@code internal_realloc} + {@code memset} zero-fill
 * plus {@code gMapLocalPointers.resize(length)} (value-init nulls)
 * becomes {@link Arrays#copyOf} for both arrays (zero/null-fill is
 * automatic). The failure path (C restores the old length and returns
 * -1) is mirrored via {@code OutOfMemoryError}: length restored, -1.
 * Globals never grow in this slice (headless length stays 0), so only
 * locals have an allocator, exactly like C.</li>
 * <li>Rebuild walk: the tile-light rebuild ({@code _obj_rebuild_all_light}
 * in {@code src/object.cc}) is object.cc domain, so it lives in
 * {@link ObjectSystem#objRebuildAllLight}, not here; this class owns only
 * the var domain.</li>
 * <li>Reset: {@link #resetForTests} restores the fresh-process mirror
 * (lengths 0, empty arrays), matching the C BSS zeros/nulls.</li>
 * </ul>
 */
public final class MapVars {

    private MapVars() {
    }

    /** Port of {@code VALUE_TYPE_INT} (0xC001, {@code src/interpreter.h}). */
    public static final int VALUE_TYPE_INT = 0xC001;

    /** Port of {@code VALUE_TYPE_PTR} (0xE001, {@code src/interpreter.h}). */
    public static final int VALUE_TYPE_PTR = 0xE001;

    /**
     * Minimal port of {@code ProgramValue} ({@code src/interpreter.h}:
     * {@code opcode} enum + {@code integerValue}/{@code pointerValue}
     * union). See class notes for why it lives here.
     */
    public static final class ProgramValue {
        public int opcode;
        public int integerValue;
        public Object pointerValue;

        public ProgramValue() {
            this.opcode = VALUE_TYPE_INT;
            this.integerValue = 0;
            this.pointerValue = null;
        }

        public ProgramValue(int value) {
            this.opcode = VALUE_TYPE_INT;
            this.integerValue = value;
            this.pointerValue = null;
        }

        public ProgramValue(Object value) {
            this.opcode = VALUE_TYPE_PTR;
            this.integerValue = 0;
            this.pointerValue = value;
        }
    }

    /** Port of {@code gMapGlobalVarsLength} (init 0). */
    private static int globalVarsLength = 0;

    /** Port of {@code gMapLocalVarsLength} (init 0). */
    private static int localVarsLength = 0;

    /** Port of {@code gMapGlobalVars} (init null; empty mirror here). */
    private static int[] globalVars = new int[0];

    /** Port of {@code gMapLocalVars} (init null; empty mirror here). */
    private static int[] localVars = new int[0];

    /** Port of {@code gMapGlobalPointers} (init empty). */
    private static Object[] globalPointers = new Object[0];

    /** Port of {@code gMapLocalPointers} (init empty). */
    private static Object[] localPointers = new Object[0];

    // 0x482220 mapSetGlobalVar (silent: no debugPrint).
    public static int mapSetGlobalVar(int var, ProgramValue value) {
        if (var < 0 || var >= globalVarsLength) {
            return -1;
        }

        if (value.opcode == VALUE_TYPE_PTR) {
            globalVars[var] = 0;
            globalPointers[var] = value.pointerValue;
        } else {
            globalVars[var] = value.integerValue;
            globalPointers[var] = null;
        }

        return 0;
    }

    // 0x482250 mapGetGlobalVar (silent: no debugPrint).
    public static int mapGetGlobalVar(int var, ProgramValue value) {
        if (var < 0 || var >= globalVarsLength) {
            return -1;
        }

        if (globalPointers[var] != null) {
            value.opcode = VALUE_TYPE_PTR;
            value.pointerValue = globalPointers[var];
        } else {
            value.opcode = VALUE_TYPE_INT;
            value.integerValue = globalVars[var];
        }

        return 0;
    }

    // 0x482280 mapSetLocalVar (silent: no debugPrint).
    public static int mapSetLocalVar(int var, ProgramValue value) {
        if (var < 0 || var >= localVarsLength) {
            return -1;
        }

        if (value.opcode == VALUE_TYPE_PTR) {
            localVars[var] = 0;
            localPointers[var] = value.pointerValue;
        } else {
            localVars[var] = value.integerValue;
            localPointers[var] = null;
        }

        return 0;
    }

    // 0x4822B0 mapGetLocalVar (silent: no debugPrint).
    public static int mapGetLocalVar(int var, ProgramValue value) {
        if (var < 0 || var >= localVarsLength) {
            return -1;
        }

        if (localPointers[var] != null) {
            value.opcode = VALUE_TYPE_PTR;
            value.pointerValue = localPointers[var];
        } else {
            value.opcode = VALUE_TYPE_INT;
            value.integerValue = localVars[var];
        }

        return 0;
    }

    // 0x4822E0 mapAllocLocalVars: returns the old length (base offset of
    // the new room); failure restores the length and returns -1.
    public static int mapAllocLocalVars(int numNewVars) {
        int oldLength = localVarsLength;
        localVarsLength += numNewVars;
        try {
            localVars = Arrays.copyOf(localVars, localVarsLength);
            localPointers = Arrays.copyOf(localPointers, localVarsLength);
        } catch (OutOfMemoryError e) {
            localVarsLength = oldLength;
            return -1;
        }

        return oldLength;
    }

    /** Test seam: fresh-process mirror (lengths 0, empty arrays). */
    static void resetForTests() {
        globalVarsLength = 0;
        localVarsLength = 0;
        globalVars = new int[0];
        localVars = new int[0];
        globalPointers = new Object[0];
        localPointers = new Object[0];
    }
}
