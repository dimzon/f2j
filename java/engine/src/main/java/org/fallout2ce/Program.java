package org.fallout2ce;

import java.nio.charset.StandardCharsets;
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
 * integer-stack LIFO pops); plus {@code [programstack]} in
 * {@code docker/refharness/golden/v92.txt} ({@code sectionProgramStack}):
 * {@code float=40961:2.500000 str=[hi] ptrnull=1 int=9} (float round-trip
 * pins {@code VALUE_TYPE_FLOAT} 40961 = 0xA001; dynamic-string push/pop;
 * null-pointer push/pop; raw {@code ProgramValue} push + integer pop);
 * plus {@code [progretstack]} in
 * {@code docker/refharness/golden/v93.txt} ({@code sectionProgRetStack}):
 * {@code procs=29 retptrnull=1 retpop=11,22} ({@code scripts\ABEL.int}
 * has 29 procedures; return-stack integer/pointer push/pop).
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
 * event table. Stacks and the dynamic-string heap live on the
 * {@code Program} instance and die with {@code programFree}, so this
 * slice owns no statics.</li>
  * <li>v92 string-heap port: {@code dynamicStrings} is a verbatim
  * little-endian image of the C heap (int32 used-count header, then
  * {@code short len + short refcount + bytes} blocks, {@code 0x8000}
  * terminator). {@code programPushString} ports the block math exactly:
  * even alignment, truncation at 32766, free-block reuse with splitting,
  * and same-content dedup (equal-size used block holding equal bytes
  * returns the existing offset — so pushing the same string twice yields
  * the same id). C heap bytes are the raw {@code char} payload; Java
  * encodes/decodes them as Latin-1 (identity for the ASCII vectors).</li>
  * <li>v92 refcounts: C {@code programStackPushValue} /
  * {@code programStackPopValue} bump/drop the dynamic-string refcount
  * through the exact-opcode ({@code == VALUE_TYPE_DYNAMIC_STRING}) gate;
  * mirrored here (decrease clamps at zero like C). Pop-then-read stays
  * valid because the heap is only compacted by the unported
  * {@code programMarkHeap} GC pass.</li>
  * <li>v92 pop gates: {@code programStackPopString} admits static
  * ({@code 0x9001}) and dynamic ({@code 0x9801}) cells via the
  * {@code (opcode & VALUE_TYPE_MASK) != VALUE_TYPE_STRING} gate and
  * resolves through {@code programGetString} (dynamic flag wins, offset
  * biased past the 4-byte header); {@code programStackPopPointer} maps
  * an INT 0 cell (uninitialized exported object var) to null. Empty /
  * mismatched pops are C noreturn fatals; Java throws
  * {@code IllegalStateException} with the same message text and those
  * paths are deliberately untested.</li>
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

    /** Port of {@code RAW_VALUE_TYPE_STATIC_STRING} (0x1000, {@code src/interpreter.h}). */
    public static final int RAW_VALUE_TYPE_STATIC_STRING = 0x1000;

    /** Port of {@code RAW_VALUE_TYPE_DYNAMIC_STRING} (0x0800, {@code src/interpreter.h}). */
    public static final int RAW_VALUE_TYPE_DYNAMIC_STRING = 0x0800;

    /** Port of {@code kDynamicStringsMaxBlockSize} (32766, {@code src/interpreter.cc}). */
    static final int DYNAMIC_STRINGS_MAX_BLOCK_SIZE = 32766;

    /** Port of {@code PROGRAM_FLAG_CHILD_CALL} (0x20, {@code src/interpreter.h}). */
    public static final int PROGRAM_FLAG_CHILD_CALL = 0x20;

    /** Port of {@code PROGRAM_FLAG_CHILD_SPAWN} (0x0100, {@code src/interpreter.h}). */
    public static final int PROGRAM_FLAG_CHILD_SPAWN = 0x0100;

    // Remaining ProgramFlags (src/interpreter.h). Only the VM loop and the
    // opcode handlers read these, so they arrive with the VM port.

    /** Port of {@code PROGRAM_FLAG_EXITED} (0x01). */
    static final int PROGRAM_FLAG_EXITED = 0x01;

    /** Port of {@code PROGRAM_FLAG_RUNNING} (0x02). */
    static final int PROGRAM_FLAG_RUNNING = 0x02;

    /** Port of {@code PROGRAM_FLAG_FATAL_ERROR} (0x04). */
    static final int PROGRAM_FLAG_FATAL_ERROR = 0x04;

    /** Port of {@code PROGRAM_FLAG_STOPPED} (0x08). */
    static final int PROGRAM_FLAG_STOPPED = 0x08;

    /** Port of {@code PROGRAM_IS_WAITING} (0x10). */
    static final int PROGRAM_IS_WAITING = 0x10;

    /** Port of {@code PROGRAM_FLAG_FINISHED} (0x40). */
    static final int PROGRAM_FLAG_FINISHED = 0x40;

    /** Port of {@code PROGRAM_FLAG_CRITICAL_SECTION} (0x80). */
    static final int PROGRAM_FLAG_CRITICAL_SECTION = 0x80;

    // Opcode enum (src/interpreter.h). The VM dispatches on
    // (opcode & 0x3FF), so these double as the handler-table keys.
    static final int OPCODE_NOOP = 0x8000;
    static final int OPCODE_PUSH = 0x8001;
    static final int OPCODE_ENTER_CRITICAL_SECTION = 0x8002;
    static final int OPCODE_LEAVE_CRITICAL_SECTION = 0x8003;
    static final int OPCODE_JUMP = 0x8004;
    static final int OPCODE_CALL = 0x8005;
    static final int OPCODE_CALL_AT = 0x8006;
    static final int OPCODE_CALL_WHEN = 0x8007;
    static final int OPCODE_CALLSTART = 0x8008;
    static final int OPCODE_EXEC = 0x8009;
    static final int OPCODE_SPAWN = 0x800A;
    static final int OPCODE_FORK = 0x800B;
    static final int OPCODE_A_TO_D = 0x800C;
    static final int OPCODE_D_TO_A = 0x800D;
    static final int OPCODE_EXIT = 0x800E;
    static final int OPCODE_DETACH = 0x800F;
    static final int OPCODE_EXIT_PROGRAM = 0x8010;
    static final int OPCODE_STOP_PROGRAM = 0x8011;
    static final int OPCODE_FETCH_GLOBAL = 0x8012;
    static final int OPCODE_STORE_GLOBAL = 0x8013;
    static final int OPCODE_FETCH_EXTERNAL = 0x8014;
    static final int OPCODE_STORE_EXTERNAL = 0x8015;
    static final int OPCODE_EXPORT_VARIABLE = 0x8016;
    static final int OPCODE_EXPORT_PROCEDURE = 0x8017;
    static final int OPCODE_SWAP = 0x8018;
    static final int OPCODE_SWAPA = 0x8019;
    static final int OPCODE_POP = 0x801A;
    static final int OPCODE_DUP = 0x801B;
    static final int OPCODE_POP_RETURN = 0x801C;
    static final int OPCODE_POP_EXIT = 0x801D;
    static final int OPCODE_POP_ADDRESS = 0x801E;
    static final int OPCODE_POP_FLAGS = 0x801F;
    static final int OPCODE_POP_FLAGS_RETURN = 0x8020;
    static final int OPCODE_POP_FLAGS_EXIT = 0x8021;
    static final int OPCODE_POP_FLAGS_RETURN_EXTERN = 0x8022;
    static final int OPCODE_POP_FLAGS_EXIT_EXTERN = 0x8023;
    static final int OPCODE_POP_FLAGS_RETURN_VAL_EXTERN = 0x8024;
    static final int OPCODE_POP_FLAGS_RETURN_VAL_EXIT = 0x8025;
    static final int OPCODE_POP_FLAGS_RETURN_VAL_EXIT_EXTERN = 0x8026;
    static final int OPCODE_CHECK_PROCEDURE_ARGUMENT_COUNT = 0x8027;
    static final int OPCODE_LOOKUP_PROCEDURE_BY_NAME = 0x8028;
    static final int OPCODE_POP_BASE = 0x8029;
    static final int OPCODE_POP_TO_BASE = 0x802A;
    static final int OPCODE_PUSH_BASE = 0x802B;
    static final int OPCODE_SET_GLOBAL = 0x802C;
    static final int OPCODE_FETCH_PROCEDURE_ADDRESS = 0x802D;
    static final int OPCODE_DUMP = 0x802E;
    static final int OPCODE_IF = 0x802F;
    static final int OPCODE_WHILE = 0x8030;
    static final int OPCODE_STORE = 0x8031;
    static final int OPCODE_FETCH = 0x8032;
    static final int OPCODE_EQUAL = 0x8033;
    static final int OPCODE_NOT_EQUAL = 0x8034;
    static final int OPCODE_LESS_THAN_EQUAL = 0x8035;
    static final int OPCODE_GREATER_THAN_EQUAL = 0x8036;
    static final int OPCODE_LESS_THAN = 0x8037;
    static final int OPCODE_GREATER_THAN = 0x8038;
    static final int OPCODE_ADD = 0x8039;
    static final int OPCODE_SUB = 0x803A;
    static final int OPCODE_MUL = 0x803B;
    static final int OPCODE_DIV = 0x803C;
    static final int OPCODE_MOD = 0x803D;
    static final int OPCODE_AND = 0x803E;
    static final int OPCODE_OR = 0x803F;
    static final int OPCODE_BITWISE_AND = 0x8040;
    static final int OPCODE_BITWISE_OR = 0x8041;
    static final int OPCODE_BITWISE_XOR = 0x8042;
    static final int OPCODE_BITWISE_NOT = 0x8043;
    static final int OPCODE_FLOOR = 0x8044;
    static final int OPCODE_NOT = 0x8045;
    static final int OPCODE_NEGATE = 0x8046;
    static final int OPCODE_WAIT = 0x8047;
    static final int OPCODE_CANCEL = 0x8048;
    static final int OPCODE_CANCEL_ALL = 0x8049;
    static final int OPCODE_START_CRITICAL = 0x804A;
    static final int OPCODE_END_CRITICAL = 0x804B;

    /**
     * Port of {@code programInterpret} (src/interpreter.cc): the VM loop.
     *
     * <p>C wraps the body in {@code setjmp} so {@code programFatalError} can
     * {@code longjmp} out and stamp {@code PROGRAM_FLAG_FATAL_ERROR}. Java has
     * no setjmp, so the equivalent is a catch around the loop — same
     * observable outcome (flags stamped, current program restored), without
     * needing a jump target.
     *
     * <p>{@code numInstructions < 0} runs until the program stops; a
     * positive budget runs that many steps. A program in a critical section is
     * lifted to three steps, exactly like C.
     */
    static void programInterpret(Program program, int numInstructions) {
        Program oldCurrentProgram = interpreterCurrentProgram;

        if (!interpreterEnabled || interpreterBusy) {
            return;
        }

        if (program.exited
                || (program.flags & PROGRAM_FLAG_CHILD_CALL) != 0
                || (program.flags & PROGRAM_FLAG_CHILD_SPAWN) != 0) {
            return;
        }

        interpreterCurrentProgram = program;

        try {
            if ((program.flags & PROGRAM_FLAG_CRITICAL_SECTION) != 0 && numInstructions < 3) {
                numInstructions = 3;
            }

            while ((program.flags & PROGRAM_FLAG_CRITICAL_SECTION) != 0 || --numInstructions != -1) {
                if ((program.flags & (PROGRAM_FLAG_EXITED | PROGRAM_FLAG_FATAL_ERROR
                        | PROGRAM_FLAG_STOPPED | PROGRAM_FLAG_CHILD_CALL
                        | PROGRAM_FLAG_FINISHED | PROGRAM_FLAG_CHILD_SPAWN)) != 0) {
                    break;
                }

                if (program.exited) {
                    break;
                }

                int opcode = programGetNextOpcode(program);

                // NOTE in C: only the low 16 flags survive each step.
                program.flags &= 0xFFFF;
                program.flags |= opcode << 16;

                if (((opcode >> 8) & 0x80) == 0) {
                    programFatalError("Bad opcode " + Integer.toHexString(opcode) + ".");
                }

                OpcodeHandler handler = INTERPRETER_OPCODE_HANDLERS[opcode & 0x3FF];
                if (handler == null) {
                    programFatalError("Undefined opcode " + Integer.toHexString(opcode) + ".");
                }

                handler.run(program);
            }
        } catch (FatalError error) {
            // longjmp from programFatalError().
            interpreterCurrentProgram = oldCurrentProgram;
            program.flags |= PROGRAM_FLAG_EXITED | PROGRAM_FLAG_FATAL_ERROR;
            return;
        }

        program.flags &= ~PROGRAM_FLAG_FINISHED;
        interpreterCurrentProgram = oldCurrentProgram;
    }

    /** Handler signature: {@code OpcodeHandler} (src/interpreter.h). */
    interface OpcodeHandler {
        void run(Program program);
    }

    /** {@code gInterpreterOpcodeHandlers} (src/interpreter.cc), 1024 entries. */
    private static final OpcodeHandler[] INTERPRETER_OPCODE_HANDLERS = new OpcodeHandler[1024];

    // 0x467F44 interpreterRegisterOpcode.
    static void interpreterRegisterOpcode(int opcode, OpcodeHandler handler) {
        INTERPRETER_OPCODE_HANDLERS[opcode & 0x3FF] = handler;
    }

    // opNoop.
    private static void opNoop(Program program) {
    }

    // opExitProgram: the only thing a run-to-completion script needs.
    private static void opExitProgram(Program program) {
        program.flags |= PROGRAM_FLAG_EXITED;
    }

    /**
     * Port of {@code interpreterRegisterOpcodeHandlers} for the subset this
     * slice runs. C registers all 76 handlers here; the rest arrive with
     * their opcodes.
     */
    static void interpreterRegisterOpcodeHandlers() {
        interpreterEnabled = true;
        interpreterRegisterOpcode(OPCODE_NOOP, Program::opNoop);
        interpreterRegisterOpcode(OPCODE_EXIT_PROGRAM, Program::opExitProgram);
    }

    /** {@code gInterpreterCurrentProgram} (src/interpreter.cc). */
    private static Program interpreterCurrentProgram;

    /** {@code interpreterEnabled} (src/interpreter.cc). */
    private static boolean interpreterEnabled = true;

    /** {@code interpreterBusy} (src/interpreter.cc). */
    private static boolean interpreterBusy;

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

    /** Port of {@code returnStackValues} (ArrayDeque LIFO from the tail; return-stack ops below). */
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

    // 0x4672C0 stackReadInt16 (big-endian, sign-extended like the C short read).
    public static int stackReadInt16(byte[] data, int pos) {
        return (short) (((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF));
    }

    // 0x4672D8 stackWriteInt16.
    static void stackWriteInt16(int value, byte[] data, int pos) {
        data[pos] = (byte) (value >> 8);
        data[pos + 1] = (byte) value;
    }

    // 0x4672EC stackWriteInt32.
    static void stackWriteInt32(int value, byte[] data, int pos) {
        data[pos] = (byte) (value >> 24);
        data[pos + 1] = (byte) (value >> 16);
        data[pos + 2] = (byte) (value >> 8);
        data[pos + 3] = (byte) value;
    }

    /** Raised where C calls {@code programFatalError} (a longjmp out of the VM). */
    static final class FatalError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        FatalError(String message) {
            super(message);
        }
    }

    /** Port of {@code programFatalError} (src/interpreter.cc). */
    static void programFatalError(String message) {
        throw new FatalError(message);
    }

    /** Port of {@code stackPushInt32} (src/interpreter.cc): two big-endian
     *  shorts, with C's overflow guard at {@code 0x1000}. */
    static void stackPushInt32(byte[] data, int[] pointer, int value) {
        if (pointer[0] + 4 >= 0x1000) {
            // FIXME in C: should be pushLongStack.
            programFatalError("pushShortStack: Stack overflow.");
        }
        int pos = pointer[0];
        stackWriteInt16(value >> 16, data, pos);
        stackWriteInt16(value & 0xFFFF, data, pos + 2);
        pointer[0] = pos + 4;
    }

    /** Port of {@code stackPopInt32} (src/interpreter.cc). */
    static int stackPopInt32(byte[] data, int[] pointer) {
        if (pointer[0] < 4) {
            programFatalError("Stack underflow long.");
        }
        pointer[0] -= 4;
        return stackReadInt32(data, pointer[0]);
    }

    /** Port of {@code stackPopInt16} (src/interpreter.cc). */
    static int stackPopInt16(byte[] data, int[] pointer) {
        if (pointer[0] < 2) {
            programFatalError("Stack underflow short.");
        }
        pointer[0] -= 2;
        return stackReadInt16(data, pointer[0]);
    }

    // 0x4678BC programGetNextOpcode (NOTE: inlined in C).
    static int programGetNextOpcode(Program program) {
        int instructionPointer = program.instructionPointer;
        program.instructionPointer = instructionPointer + 2;
        return stackReadInt16(program.data, instructionPointer);
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

        long fileSize = Db.fileGetSize(stream);
        if (fileSize < PROCEDURES_BASE_OFFSET + 4) {
            Db.fileClose(stream);
            return null;
        }

        byte[] data = new byte[(int) fileSize];
        long itemsRead = Db.fileRead(data, 0, 1, (int) fileSize, stream);
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
    // C bumps the dynamic-string refcount on push (exact-opcode gate on
    // VALUE_TYPE_DYNAMIC_STRING); mirrored here so push/pop balance and the
    // heap accounting matches C on every path.
    public static void programStackPushValue(Program program, ProgramValue value) {
        if (program.stackValues.size() >= 0x1000) {
            throw new IllegalStateException("programStackPushValue: Stack overflow.");
        }
        program.stackValues.addLast(new ProgramValue(value));
        if (value.opcode == VALUE_TYPE_DYNAMIC_STRING) {
            interpreterStringRefCountIncrease(program, VALUE_TYPE_DYNAMIC_STRING, value.integerValue);
        }
    }

    // programStackPushInteger.
    public static void programStackPushInteger(Program program, int value) {
        programStackPushValue(program, new ProgramValue(value));
    }

    // programStackPushFloat (opcode VALUE_TYPE_FLOAT = 40961 = 0xA001).
    public static void programStackPushFloat(Program program, float value) {
        programStackPushValue(program, new ProgramValue(value));
    }

    // programStackPushString (interns via programPushString, then pushes a
    // VALUE_TYPE_DYNAMIC_STRING cell; the push itself bumps the refcount).
    public static void programStackPushString(Program program, String value) {
        ProgramValue programValue = new ProgramValue();
        programValue.opcode = VALUE_TYPE_DYNAMIC_STRING;
        programValue.integerValue = programPushString(program, value);
        programStackPushValue(program, programValue);
    }

    // programStackPushPointer (opcode VALUE_TYPE_PTR).
    public static void programStackPushPointer(Program program, Object value) {
        programStackPushValue(program, new ProgramValue(value));
    }

    // programStackPopValue (underflow is a C fatal; Java throws, same text).
    // C drops the dynamic-string refcount on pop (same exact-opcode gate).
    public static ProgramValue programStackPopValue(Program program) {
        if (program.stackValues.isEmpty()) {
            throw new IllegalStateException("programStackPopValue: Stack underflow.");
        }
        ProgramValue value = program.stackValues.removeLast();
        if (value.opcode == VALUE_TYPE_DYNAMIC_STRING) {
            interpreterStringRefCountDecrease(program, value.opcode, value.integerValue);
        }
        return value;
    }

    // programStackPopInteger (wrong-type pop is a C fatal; Java throws, same text).
    public static int programStackPopInteger(Program program) {
        ProgramValue value = programStackPopValue(program);
        if (value.opcode != VALUE_TYPE_INT) {
            throw new IllegalStateException("integer expected, got " + Integer.toHexString(value.opcode));
        }
        return value.integerValue;
    }

    // programStackPopString: mask gate admits both static (0x9001) and
    // dynamic (0x9801) strings since 0x9801 & 0xF7FF == 0x9001; resolves via
    // programGetString. Mismatch is a C fatal; Java throws (untested path).
    public static String programStackPopString(Program program) {
        ProgramValue value = programStackPopValue(program);
        if ((value.opcode & VALUE_TYPE_MASK) != VALUE_TYPE_STRING) {
            throw new IllegalStateException("string expected, got " + Integer.toHexString(value.opcode));
        }
        return programGetString(program, value.opcode, value.integerValue);
    }

    // programStackPopPointer: an INT 0 cell (uninitialized exported object
    // var) reads back as null instead of faulting; any other non-PTR cell
    // is a C fatal (Java throws, untested path).
    public static Object programStackPopPointer(Program program) {
        ProgramValue value = programStackPopValue(program);
        if (value.opcode == VALUE_TYPE_INT && value.integerValue == 0) {
            return null;
        }
        if (value.opcode != VALUE_TYPE_PTR) {
            throw new IllegalStateException("pointer expected, got " + Integer.toHexString(value.opcode));
        }
        return value.pointerValue;
    }

    // programReturnStackPushValue (same overflow/refcount discipline as the
    // value stack, but on returnStackValues).
    public static void programReturnStackPushValue(Program program, ProgramValue value) {
        if (program.returnStackValues.size() >= 0x1000) {
            throw new IllegalStateException("programReturnStackPushValue: Stack overflow.");
        }
        program.returnStackValues.addLast(new ProgramValue(value));
        if (value.opcode == VALUE_TYPE_DYNAMIC_STRING) {
            interpreterStringRefCountIncrease(program, VALUE_TYPE_DYNAMIC_STRING, value.integerValue);
        }
    }

    // programReturnStackPushInteger.
    public static void programReturnStackPushInteger(Program program, int value) {
        programReturnStackPushValue(program, new ProgramValue(value));
    }

    // programReturnStackPushPointer (opcode VALUE_TYPE_PTR).
    public static void programReturnStackPushPointer(Program program, Object value) {
        programReturnStackPushValue(program, new ProgramValue(value));
    }

    // programReturnStackPopValue (underflow is a C fatal; Java throws, same text).
    public static ProgramValue programReturnStackPopValue(Program program) {
        if (program.returnStackValues.isEmpty()) {
            throw new IllegalStateException("programReturnStackPopValue: Stack underflow.");
        }
        ProgramValue value = program.returnStackValues.removeLast();
        if (value.opcode == VALUE_TYPE_DYNAMIC_STRING) {
            interpreterStringRefCountDecrease(program, value.opcode, value.integerValue);
        }
        return value;
    }

    // programReturnStackPopInteger: TYPE-BLIND, unlike programStackPopInteger
    // (which fatals on a non-INT opcode). C just returns integerValue with no
    // opcode check; mirrored here exactly.
    public static int programReturnStackPopInteger(Program program) {
        ProgramValue value = programReturnStackPopValue(program);
        return value.integerValue;
    }

    // programReturnStackPopPointer: TYPE-BLIND, unlike programStackPopPointer
    // (which maps INT 0 to null and fatals on other non-PTR cells). C just
    // returns pointerValue with no opcode check; mirrored here exactly.
    public static Object programReturnStackPopPointer(Program program) {
        ProgramValue value = programReturnStackPopValue(program);
        return value.pointerValue;
    }

    // 0x467440 interpreterStringRefCountDecrease (public in C header).
    // Dynamic-string heap only; zero stays zero (C debug-prints instead).
    public static void interpreterStringRefCountDecrease(Program program, int opcode, int value) {
        if (opcode == VALUE_TYPE_DYNAMIC_STRING) {
            int at = value + 2;
            short refcount = getInt16Le(program.dynamicStrings, at);
            if (refcount != 0) {
                putInt16Le(program.dynamicStrings, at, (short) (refcount - 1));
            }
        }
    }

    // 0x467424 interpreterStringRefCountIncrease (static/inlined in C).
    private static void interpreterStringRefCountIncrease(Program program, int opcode, int value) {
        if (opcode == VALUE_TYPE_DYNAMIC_STRING) {
            int at = value + 2;
            short refcount = getInt16Le(program.dynamicStrings, at);
            putInt16Le(program.dynamicStrings, at, (short) (refcount + 1));
        }
    }

    // 0x4678E0 programGetString. Dynamic flag wins (it is always set together
    // with the static flag on dynamic opcodes). Offsets bias past the 4-byte
    // heap header, verbatim: heap string lives at dynamicStrings + 4 + offset.
    public static String programGetString(Program program, int opcode, int offset) {
        if ((opcode & RAW_VALUE_TYPE_DYNAMIC_STRING) != 0) {
            if (program.dynamicStrings == null) {
                return null;
            }
            return readCString(program.dynamicStrings, 4 + offset);
        }
        if ((opcode & RAW_VALUE_TYPE_STATIC_STRING) != 0) {
            return readCString(program.data, program.staticStrings + 4 + offset);
        }
        return null;
    }

    // 0x467A80 programPushString: bump-allocated heap with free-block reuse
    // and same-content dedup (equal-size used block holding the same bytes
    // returns the existing offset). Offsets are block indices into
    // dynamicStrings (header-biased: first block returns 4). Byte-exact port
    // of the C block math, including even alignment, truncation at
    // 32766, free-block splitting, and the 0x8000 terminator.
    public static int programPushString(Program program, String string) {
        if (program == null) {
            return 0;
        }

        byte[] src = string.getBytes(StandardCharsets.ISO_8859_1);
        int bufferLength = src.length + 1;
        if ((bufferLength & 1) != 0) {
            bufferLength++;
        }
        if (bufferLength > DYNAMIC_STRINGS_MAX_BLOCK_SIZE) {
            bufferLength = DYNAMIC_STRINGS_MAX_BLOCK_SIZE;
        }

        if (program.dynamicStrings != null) {
            byte[] heap = program.dynamicStrings;
            int h = 4;
            while (getUint16Le(heap, h) != 0x8000) {
                short blockLength = getInt16Le(heap, h);
                if (blockLength >= 0) {
                    // C strcmp against the full original: a truncated store
                    // (prefix + NUL) never equals its longer source, so the
                    // plain full-string compare below is verbatim.
                    if (blockLength == bufferLength
                            && readCString(heap, h + 4).equals(new String(src, StandardCharsets.ISO_8859_1))) {
                        return h;
                    }
                } else {
                    int freeLength = -blockLength;
                    if (freeLength > bufferLength) {
                        if (freeLength - bufferLength <= 4) {
                            putInt16Le(heap, h, (short) freeLength);
                        } else {
                            putInt16Le(heap, h + bufferLength + 4, (short) (-(freeLength - bufferLength - 4)));
                            putInt16Le(heap, h + bufferLength + 6, (short) 0);
                            putInt16Le(heap, h, (short) bufferLength);
                        }
                        putInt16Le(heap, h + 2, (short) 0);
                        writeHeapString(heap, h + 4, src, bufferLength);
                        heap[h + bufferLength + 3] = 0;
                        return h;
                    }
                }
                h += Math.abs(blockLength) + 4;
            }
        } else {
            program.dynamicStrings = new byte[8];
            putInt32Le(program.dynamicStrings, 0, 0);
            putUint16Le(program.dynamicStrings, 4, 0x8000);
            putInt16Le(program.dynamicStrings, 6, (short) 1);
        }

        int used = getInt32Le(program.dynamicStrings, 0);
        byte[] grown = new byte[used + 8 + 4 + bufferLength];
        System.arraycopy(program.dynamicStrings, 0, grown, 0, program.dynamicStrings.length);
        program.dynamicStrings = grown;

        int newBlock = used + 4;
        if (getUint16Le(program.dynamicStrings, newBlock) != 0x8000) {
            throw new IllegalStateException("Internal consistency error, string table mangled");
        }

        putInt32Le(program.dynamicStrings, 0, used + bufferLength + 4);
        putInt16Le(program.dynamicStrings, newBlock, (short) bufferLength);
        putInt16Le(program.dynamicStrings, newBlock + 2, (short) 0);
        writeHeapString(program.dynamicStrings, newBlock + 4, src, bufferLength);

        int newTerminator = newBlock + bufferLength;
        program.dynamicStrings[newTerminator + 3] = 0;
        putUint16Le(program.dynamicStrings, newTerminator + 4, 0x8000);
        putInt16Le(program.dynamicStrings, newTerminator + 6, (short) 1);

        return newBlock;
    }

    // --- Little-endian heap accessors (C derefs native LE shorts/ints). ---

    private static short getInt16Le(byte[] data, int pos) {
        return (short) ((data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8));
    }

    private static int getUint16Le(byte[] data, int pos) {
        return (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8);
    }

    private static void putInt16Le(byte[] data, int pos, short value) {
        data[pos] = (byte) (value & 0xFF);
        data[pos + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private static void putUint16Le(byte[] data, int pos, int value) {
        data[pos] = (byte) (value & 0xFF);
        data[pos + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private static int getInt32Le(byte[] data, int pos) {
        return (data[pos] & 0xFF)
                | ((data[pos + 1] & 0xFF) << 8)
                | ((data[pos + 2] & 0xFF) << 16)
                | ((data[pos + 3] & 0xFF) << 24);
    }

    private static void putInt32Le(byte[] data, int pos, int value) {
        data[pos] = (byte) (value & 0xFF);
        data[pos + 1] = (byte) ((value >> 8) & 0xFF);
        data[pos + 2] = (byte) ((value >> 16) & 0xFF);
        data[pos + 3] = (byte) ((value >> 24) & 0xFF);
    }

    // NUL-terminated bytes decoded as Latin-1 (C char semantics for ASCII vectors).
    private static String readCString(byte[] data, int pos) {
        int end = pos;
        while (end < data.length && data[end] != 0) {
            end++;
        }
        return new String(data, pos, end - pos, StandardCharsets.ISO_8859_1);
    }

    // C strncpy(heap, string, bufferLength - 1) + NUL at [bufferLength - 1],
    // zero-padding a reused free block (fresh blocks are already zero).
    private static void writeHeapString(byte[] heap, int pos, byte[] src, int bufferLength) {
        int copyLength = Math.min(src.length, bufferLength - 1);
        System.arraycopy(src, 0, heap, pos, copyLength);
        for (int i = pos + copyLength; i < pos + bufferLength; i++) {
            heap[i] = 0;
        }
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
