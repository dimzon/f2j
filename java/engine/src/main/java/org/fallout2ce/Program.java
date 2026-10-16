package org.fallout2ce;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Script-program loading slice, porting the load/find/stack/free domain of
 * {@code src/interpreter.cc} / {@code src/interpreter.h}:
 * {@code programCreateByPath} (0x467734),
 * {@code programFindProcedure} (0x46DCD0), {@code programStackPushValue} /
 * {@code programStackPushInteger} / {@code programStackPopValue} /
 * {@code programStackPopInteger}, {@code programFree} (0x467614),
 * {@code stackReadInt32} (0x4672A4, big-endian) and the {@code Program} /
 * {@code ProgramValue} / {@code Procedure} shapes.
 *
 * <p>Vectors: {@code [scriptprog]} in
 * {@code docker/refharness/golden/v91.txt}, produced by
 * {@code sectionScriptProg} in {@code docker/refharness/ref_main.cc}:
 * {@code loaded=1 start=1 miss=-1 pop=7,42} ({@code scripts\ABEL.int}
 * loads; {@code "start"} is procedure index 1; {@code "nope"} misses;
 * integer-stack LIFO pops).
 *
 * <p>NOTES / mapping decisions (all documented at the use site too):
 * <ul>
 * <li>Home: the v37 opaque {@code Program} stub is extended here into the
 * real program home. Bytecode execution stays out of scope (dormant
 * fields below), so this stays in {@code Program.java} rather than a new
 * {@code InterpreterProgram.java}: one domain per C file would split a
 * 380-byte stub from its own struct for no benefit.</li>
 * <li>{@code ProgramValue} home: {@link MapVars.ProgramValue} (v76) covers
 * only the map-var surface ({@code opcode} + int/pointer, no float slot),
 * so it does not match interpreter semantics (C union is
 * int/float/pointer plus int/unsigned/bool/float/Object ctors). It is
 * promoted here as {@link ProgramValue} with the full union shape and
 * all C++ ctors; {@code MapVars.ProgramValue} is now a thin compat
 * subclass, so there is exactly one value home and no duplication.</li>
 * <li>Endianness: {@code stackReadInt32} reads big-endian
 * ({@code data[pos] << 24} first); {@code .int} bytecode is BE on disk.
 * Java bytes are signed, so every lane is masked with {@code 0xFF} (C
 * relies on {@code unsigned char} promotion instead).</li>
 * <li>Offsets, not pointers: C threads raw pointers into the loaded blob
 * ({@code procedures = data + 42}, {@code identifiers}, ...); Java keeps
 * the bytes in {@code data} ({@code byte[]}, like {@code Art} data) and
 * stores {@code procedures} / {@code identifiers} / {@code staticStrings}
 * as {@code int} offsets. The layout math is verbatim
 * ({@code procedures = 42};
 * {@code identifiers = 24 * count(procedures) + procedures + 4};
 * {@code staticStrings = identifiers + int32(identifiers) + 4};
 * {@code Procedure} is 6 int32 fields = 24 bytes, name offset at +0).</li>
 * <li>Stacks: C {@code std::vector<ProgramValue>} push_back/pop_back
 * becomes {@link ArrayDeque} addLast/removeLast (LIFO from the tail,
 * same order). The 0x1000 overflow / empty-underflow / wrong-type pops
 * are C fatal errors; Java throws {@link IllegalStateException} with the
 * same message text (untestable in vectors, never hit on the golden
 * path).</li>
 * <li>Missing-file divergence (deliberate, headless-safe): C
 * {@code programCreateByPath} calls the noreturn
 * {@code programFatalError} when {@code fileOpen} misses, which is
 * instant-death and untestable in vectors (the harness only pins the
 * successful load). Java returns {@code null} on a missing/short file
 * instead, mirroring the {@code Art.artLoadFrm} null-on-miss convention.
 * Likewise, truncated blobs that cannot hold the table math return
 * {@code null} rather than reading out of bounds.</li>
 * <li>Dormant execution fields: {@code instructionPointer},
 * {@code waitEnd} / {@code waitStart} (C {@code unsigned int}, widened
 * to {@code long}), {@code flags}, {@code windowId} and
 * {@code dynamicStrings} (null until the string-heap phase) are
 * transcribed and zero/null-initialized like C {@code memset}, but no
 * bytecode executes in this slice. {@code jmp_buf env} and the
 * {@code checkWaitFunc} function pointer have no headless Java form and
 * are omitted until the execution phase.</li>
 * <li>{@code programFree} purge: C {@code _purgeProgram} calls
 * {@code intLibRemoveProgramReferences}, whose port is
 * {@link Nevs#removeProgramReferences} (explicitly public for future
 * interpreter wiring); it is called here, and is a no-op while the
 * event table is empty. Parent-flag detach and the child-chain walk are
 * verbatim.</li>
 * <li>Reset: no {@code resetForTests} — {@code programCreateByPath}
 * never touches the global program list (listing nodes are added only
 * by the separate, unported {@code programListNodeCreate}), and
 * {@code programFree} touches only the program graph plus the empty
 * event table. This slice owns no statics.</li>
 * </ul>
 */
public class Program {

    /** Port of {@code VALUE_TYPE_INT} (0xC001, {@code src/interpreter.h}). */
    public static final int VALUE_TYPE_INT = 0xC001;

    /** Port of {@code VALUE_TYPE_FLOAT} (0xA001, {@code src/interpreter.h}). */
    public static final int VALUE_TYPE_FLOAT = 0xA001;

    /** Port of {@code VALUE_TYPE_STRING} (0x9001, {@code src/interpreter.h}). */
    public static final int VALUE_TYPE_STRING = 0x9001;

    /** Port of {@code VALUE_TYPE_DYNAMIC_STRING} (0x9801, {@code src/interpreter.h}). */
    public static final int VALUE_TYPE_DYNAMIC_STRING = 0x9801;

    /** Port of {@code VALUE_TYPE_PTR} (0xE001, {@code src/interpreter.h}). */
    public static final int VALUE_TYPE_PTR = 0xE001;

    /** Port of {@code VALUE_TYPE_MASK} (0xF7FF, {@code src/interpreter.h}). */
    public static final int VALUE_TYPE_MASK = 0xF7FF;

    /** Port of {@code PROGRAM_FLAG_CHILD_CALL} (0x20, {@code src/interpreter.h}). */
    public static final int PROGRAM_FLAG_CHILD_CALL = 0x20;

    /** Port of {@code PROGRAM_FLAG_CHILD_SPAWN} (0x0100, {@code src/interpreter.h}). */
    public static final int PROGRAM_FLAG_CHILD_SPAWN = 0x0100;

    /** C {@code data + 42}: byte offset of the procedure table. */
    static final int PROCEDURES_BASE_OFFSET = 42;

    /** Serialized {@code Procedure}: 6 int32 fields (name/flags/time/condition/body/argc). */
    static final int PROCEDURE_SIZE = 24;

    /**
     * Full port of {@code ProgramValue} ({@code src/interpreter.h}:
     * {@code opcode} tag + int/float/pointer union, all C++ ctors).
     * Promoted home of {@link MapVars.ProgramValue}, which stays as a
     * compat subclass; see class notes.
     */
    public static class ProgramValue {
        public int opcode;
        public int integerValue;
        public float floatValue;
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

        public ProgramValue(long value) {
            this.opcode = VALUE_TYPE_INT;
            this.integerValue = (int) value;
            this.pointerValue = null;
        }

        public ProgramValue(boolean value) {
            this.opcode = VALUE_TYPE_INT;
            this.integerValue = value ? 1 : 0;
            this.pointerValue = null;
        }

        public ProgramValue(float value) {
            this.opcode = VALUE_TYPE_FLOAT;
            this.floatValue = value;
            this.pointerValue = null;
        }

        public ProgramValue(Object value) {
            this.opcode = VALUE_TYPE_PTR;
            this.pointerValue = value;
        }

        public ProgramValue(ProgramValue other) {
            this.opcode = other.opcode;
            this.integerValue = other.integerValue;
            this.floatValue = other.floatValue;
            this.pointerValue = other.pointerValue;
        }
    }

    // --- Transcribed Program struct (execution fields dormant, see class notes). ---

    /** Port of {@code name} (C heap copy; Java strings are immutable, so assignment is the copy). */
    public String name;

    /** Port of {@code data} (loaded bytecode blob). */
    public byte[] data;

    /** Port of {@code parent}. */
    public Program parent;

    /** Port of {@code child}. */
    public Program child;

    /** Port of {@code instructionPointer} (dormant: no execution in this slice). */
    public int instructionPointer;

    /** Port of {@code framePointer} (init -1). */
    public int framePointer = -1;

    /** Port of {@code basePointer} (init -1). */
    public int basePointer = -1;

    /** Port of {@code staticStrings}: int offset into {@link #data} (was a pointer). */
    public int staticStrings;

    /** Port of {@code dynamicStrings} (null until the string-heap phase). */
    public byte[] dynamicStrings;

    /** Port of {@code identifiers}: int offset into {@link #data} (was a pointer). */
    public int identifiers;

    /** Port of {@code procedures}: int offset into {@link #data} (was a pointer). */
    public int procedures;

    /** Port of {@code waitEnd} (C {@code unsigned int}; dormant). */
    public long waitEnd;

    /** Port of {@code waitStart} (C {@code unsigned int}; dormant). */
    public long waitStart;

    /** Port of {@code startTime} (init -1, never executed marker). */
    public int startTime = -1;

    /** Port of {@code flags} (dormant). */
    public int flags;

    /** Port of {@code windowId} (dormant). */
    public int windowId;

    /** Port of {@code exited} (init false). */
    public boolean exited;

    /** Port of {@code stackValues} (ArrayDeque LIFO from the tail, like vector push_back/pop_back). */
    public final Deque<ProgramValue> stackValues = new ArrayDeque<>();

    /** Port of {@code returnStackValues} (same discipline; unused in this slice). */
    public final Deque<ProgramValue> returnStackValues = new ArrayDeque<>();

    protected Program() {
    }

    // 0x4672A4 stackReadInt32 (big-endian; every lane masked, see class notes).
    public static int stackReadInt32(byte[] data, int pos) {
        return ((data[pos] & 0xFF) << 24)
                | ((data[pos + 1] & 0xFF) << 16)
                | ((data[pos + 2] & 0xFF) << 8)
                | (data[pos + 3] & 0xFF);
    }

    // Port of Program::procedureCount (int32 at the procedure table head).
    public int procedureCount() {
        return stackReadInt32(data, procedures);
    }

    // Port of programGetIdentifier (NUL-terminated bytes at identifiers + offset).
    static String programGetIdentifier(Program program, int offset) {
        StringBuilder sb = new StringBuilder();
        int pos = program.identifiers + offset;
        while (pos >= 0 && pos < program.data.length && program.data[pos] != 0) {
            sb.append((char) (program.data[pos] & 0xFF));
            pos++;
        }
        return sb.toString();
    }

    // 0x467734 programCreateByPath. Missing/short files return null (deliberate
    // headless-safe divergence from C's noreturn fatal; see class notes).
    public static Program programCreateByPath(String path) {
        XFile.Handle stream = Db.fileOpen(path, "rb");
        if (stream == null) {
            return null;
        }

        int fileSize = Db.fileGetSize(stream);
        if (fileSize < PROCEDURES_BASE_OFFSET + 4) {
            Db.fileClose(stream);
            return null;
        }

        byte[] data = new byte[fileSize];
        long itemsRead = Db.fileRead(data, 0, 1, fileSize, stream);
        Db.fileClose(stream);
        if (itemsRead != fileSize) {
            return null;
        }

        int procedures = PROCEDURES_BASE_OFFSET;
        int procedureCount = stackReadInt32(data, procedures);
        if (procedureCount < 0) {
            return null;
        }
        int identifiers = PROCEDURE_SIZE * procedureCount + procedures + 4;
        if (identifiers < 0 || identifiers + 4 > data.length) {
            return null;
        }
        int staticStrings = identifiers + stackReadInt32(data, identifiers) + 4;
        if (staticStrings < 0 || staticStrings > data.length) {
            return null;
        }

        Program program = new Program();
        program.name = path;
        program.child = null;
        program.parent = null;
        program.startTime = -1;
        program.exited = false;
        program.basePointer = -1;
        program.framePointer = -1;
        program.data = data;
        program.procedures = procedures;
        program.identifiers = identifiers;
        program.staticStrings = staticStrings;
        return program;
    }

    // 0x46DCD0 programFindProcedure (case-insensitive name lookup, miss -> -1).
    public static int programFindProcedure(Program program, String name) {
        int procedureCount = program.procedureCount();
        for (int index = 0; index < procedureCount; index++) {
            int ptr = program.procedures + 4 + PROCEDURE_SIZE * index;
            int identifierOffset = stackReadInt32(program.data, ptr);
            if (programGetIdentifier(program, identifierOffset).equalsIgnoreCase(name)) {
                return index;
            }
        }
        return -1;
    }

    // programStackPushValue (overflow is a C fatal; Java throws, same text).
    public static void programStackPushValue(Program program, ProgramValue value) {
        if (program.stackValues.size() >= 0x1000) {
            throw new IllegalStateException("programStackPushValue: Stack overflow.");
        }
        program.stackValues.addLast(new ProgramValue(value));
    }

    // programStackPushInteger.
    public static void programStackPushInteger(Program program, int value) {
        programStackPushValue(program, new ProgramValue(value));
    }

    // programStackPopValue (underflow is a C fatal; Java throws, same text).
    public static ProgramValue programStackPopValue(Program program) {
        if (program.stackValues.isEmpty()) {
            throw new IllegalStateException("programStackPopValue: Stack underflow.");
        }
        return program.stackValues.removeLast();
    }

    // programStackPopInteger (wrong-type pop is a C fatal; Java throws, same text).
    public static int programStackPopInteger(Program program) {
        ProgramValue value = programStackPopValue(program);
        if (value.opcode != VALUE_TYPE_INT) {
            throw new IllegalStateException("integer expected, got " + Integer.toHexString(value.opcode));
        }
        return value.integerValue;
    }

    // 0x467614 programFree (detach + child-chain purge + release; no globals touched).
    public static void programFree(Program program) {
        Program parent = program.parent;
        if (parent != null) {
            parent.flags &= ~PROGRAM_FLAG_CHILD_CALL;
            parent.flags &= ~PROGRAM_FLAG_CHILD_SPAWN;
            if (program == parent.child) {
                parent.child = null;
            }
        }

        Program curr = program.child;
        while (curr != null) {
            purgeProgram(curr);
            curr.parent = null;

            Program next = curr.child;
            curr.child = null;

            curr = next;
        }

        purgeProgram(program);

        program.dynamicStrings = null;
        program.data = null;
        program.name = null;
        program.stackValues.clear();
        program.returnStackValues.clear();
    }

    // Inlined C _purgeProgram body (intlib removal is Nevs.removeProgramReferences here).
    private static void purgeProgram(Program program) {
        if (!program.exited) {
            Nevs.removeProgramReferences(program);
            program.exited = true;
        }
    }
}
