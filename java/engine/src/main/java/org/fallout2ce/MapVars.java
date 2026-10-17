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
 * <li>{@code ProgramValue} home: the minimal harness surface used to live
 * here as the nested {@link ProgramValue} class because no reusable Java
 * script value existed. The interpreter loading slice has since promoted
 * the full {@code ProgramValue} (int/float/pointer union plus all C++
 * ctors) to {@link Program.ProgramValue}; this nested class is now a thin
 * compat subclass preserving the map-var surface, so there is exactly one
 * value home and no duplication. The interpreter phase will unify the
 * remaining call sites.</li>
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

    /** Port of {@code VALUE_TYPE_INT} (0xC001, {@code src/interpreter.h}); delegates to {@link Program}. */
    public static final int VALUE_TYPE_INT = Program.VALUE_TYPE_INT;

    /** Port of {@code VALUE_TYPE_PTR} (0xE001, {@code src/interpreter.h}); delegates to {@link Program}. */
    public static final int VALUE_TYPE_PTR = Program.VALUE_TYPE_PTR;

    /**
     * Map-var view of {@code ProgramValue}: compat subclass of
     * {@link Program.ProgramValue} exposing the int/pointer ctors the
     * var domain uses (float lives only in the parent).
     */
    public static final class ProgramValue extends Program.ProgramValue {

        public ProgramValue() {
            super();
        }

        public ProgramValue(int value) {
            super(value);
        }

        public ProgramValue(Object value) {
            super(value);
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

    /** Length read for the map header/global vectors. */
    static int globalVarsLength() {
        return globalVarsLength;
    }

    /** Length read for the map header/local vectors. */
    static int localVarsLength() {
        return localVarsLength;
    }

    // 0x484038 mapGlobalVariablesFree (NOTE: Uninline in C).
    public static void mapGlobalVariablesFree() {
        globalVars = null;
        globalVarsLength = 0;
        globalPointers = new Object[0];
    }

    // 0x4840D4 mapLocalVariablesFree (NOTE: Uninline in C).
    public static void mapLocalVariablesFree() {
        localVars = null;
        localVarsLength = 0;
        localPointers = new Object[0];
    }

    // 0x483FE4 mapGlobalVariablesInit (NOTE: Uninline in C): free, then
    // allocate {@code count} ints + pointer slots (zero-filled; C malloc is
    // not zeroed, but the matching Load reads every slot).
    public static int mapGlobalVariablesInit(int count) {
        mapGlobalVariablesFree();
        if (count != 0) {
            globalVars = new int[count];
            globalPointers = new Object[count];
        }
        globalVarsLength = count;
        return 0;
    }

    // 0x48405C mapGlobalVariablesLoad (NOTE: Uninline in C).
    public static int mapGlobalVariablesLoad(XFile.Handle stream) {
        if (Db.fileReadInt32List(stream, globalVars, 0, globalVarsLength) != 0) {
            return -1;
        }
        return 0;
    }

    // 0x484080 mapLocalVariablesInit (NOTE: Uninline in C).
    public static int mapLocalVariablesInit(int count) {
        mapLocalVariablesFree();
        if (count != 0) {
            localVars = new int[count];
            localPointers = new Object[count];
        }
        localVarsLength = count;
        return 0;
    }

    // 0x4840F8 mapLocalVariablesLoad (NOTE: Uninline in C).
    public static int mapLocalVariablesLoad(XFile.Handle stream) {
        if (Db.fileReadInt32List(stream, localVars, 0, localVarsLength) != 0) {
            return -1;
        }
        return 0;
    }

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
            // internal_realloc(null, size) is malloc: a freed (null) base
            // starts a fresh array, like C.
            localVars = localVars == null
                    ? new int[localVarsLength]
                    : Arrays.copyOf(localVars, localVarsLength);
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
