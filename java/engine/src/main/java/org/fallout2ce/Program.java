package org.fallout2ce;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
 * are C fatal errors; they go through the same
 * {@link #programFatalError} longjmp the VM loop catches, so a fault
 * stamps {@code PROGRAM_FLAG_EXITED|PROGRAM_FLAG_FATAL_ERROR} and
 * unwinds instead of propagating. The v120 {@code push-return} vector
 * pins that path: {@code programStackPopInteger} lands on a PTR cell
 * and faults with the IP left past the already-fetched opcode.</li>
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
 * to {@code long}), {@code flags} and {@code dynamicStrings} (null
 * until the string-heap phase) are transcribed and zero/null-initialized
 * like C {@code memset}. {@code jmp_buf env} has no headless Java form
 * and is omitted; its save/restore around {@code programInterpret} is
 * covered by the catch in {@link #programInterpret}. {@code windowId} is
 * restored by {@code opPopFlags}, and {@code checkWaitFunc} is carried
 * as a null-by-default {@link InterpretCheckWaitFunc} field (still only
 * set by the unported WAIT opcode).</li>
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
 * mismatched pops are C noreturn fatals, ported as the same
 * {@link #programFatalError} longjmp.</li>
 * </ul>
 */
public class Program implements Export.StringHeap {

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

    /** Port of {@code PROCEDURE_FLAG_TIMED} (0x01, {@code src/interpreter.h}). */
    static final int PROCEDURE_FLAG_TIMED = 0x01;

    /** Port of {@code PROCEDURE_FLAG_CONDITIONAL} (0x02). */
    static final int PROCEDURE_FLAG_CONDITIONAL = 0x02;

    /** Port of {@code PROCEDURE_FLAG_IMPORTED} (0x04). */
    static final int PROCEDURE_FLAG_IMPORTED = 0x04;

    /** Port of {@code PROCEDURE_FLAG_EXPORTED} (0x08). */
    static final int PROCEDURE_FLAG_EXPORTED = 0x08;

    /** Port of {@code PROCEDURE_FLAG_CRITICAL} (0x10). */
    static final int PROCEDURE_FLAG_CRITICAL = 0x10;

    /** Port of {@code InterpretCheckWaitFunc} (src/interpreter.h). */
    interface InterpretCheckWaitFunc {
        int check(Program program);
    }

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

                // The waiting state. C's checkWait returns TRUE while the wait
                // is still in the future, and the loop keeps waiting only when
                // it returns FALSE - so the sense is inverted, and it is
                // ported that way.
                if ((program.flags & PROGRAM_IS_WAITING) != 0) {
                    interpreterBusy = true;

                    if (program.checkWaitFunc != null) {
                        if (program.checkWaitFunc.check(program) == 0) {
                            interpreterBusy = false;
                            continue;
                        }
                    }

                    interpreterBusy = false;
                    program.checkWaitFunc = null;
                    program.flags &= ~PROGRAM_IS_WAITING;
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
    /** The opcode table, indexed by {@code opcode & 0x3FF}. */
    static final OpcodeHandler[] INTERPRETER_OPCODE_HANDLERS = new OpcodeHandler[1024];

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

    // opPush (0x467C94). The 4-byte immediate that follows the opcode is the
    // value; its type is the *opcode itself* (0xC001 int, 0xA001 float, 0x9001
    // static string, ...), which the loop already parked in flags' top 16 bits.
    // The dispatcher masks with 0x3FF, so every type variant lands on this one
    // handler.
    private static void opPush(Program program) {
        int pos = program.instructionPointer;
        program.instructionPointer = pos + 4;
        int value = stackReadInt32(program.data, pos);
        ProgramValue result = new ProgramValue();
        result.opcode = (program.flags >> 16) & 0xFFFF;
        result.integerValue = value;
        // C stores the immediate in a union, so the same 4 bytes are readable
        // as either an int or a float; keep both Java fields in sync.
        result.floatValue = Float.intBitsToFloat(value);
        programStackPushValue(program, result);
    }

    // opPop (0x46BE10): discard the top value.
    private static void opPop(Program program) {
        programStackPopValue(program);
    }

    // opDuplicate (0x46BE4C): pop one, push it back twice.
    private static void opDuplicate(Program program) {
        ProgramValue value = programStackPopValue(program);
        programStackPushValue(program, value);
        programStackPushValue(program, value);
    }

    // opSwapStack (0x46BCAC): exchange the top two value-stack cells.
    private static void opSwapStack(Program program) {
        ProgramValue topValue = programStackPopValue(program);
        ProgramValue nextValue = programStackPopValue(program);
        programStackPushValue(program, topValue);
        programStackPushValue(program, nextValue);
    }

    // opSwapReturnStack (0x46AFFC): same exchange on the return stack.
    private static void opSwapReturnStack(Program program) {
        ProgramValue topValue = programReturnStackPopValue(program);
        ProgramValue nextValue = programReturnStackPopValue(program);
        programReturnStackPushValue(program, topValue);
        programReturnStackPushValue(program, nextValue);
    }

    // opPushBase (0x467CD0): pop the argument count, save the old frame pointer
    // on the return stack, and set the frame to size - argumentCount.
    private static void opPushBase(Program program) {
        int argumentCount = programStackPopInteger(program);
        programReturnStackPushInteger(program, program.framePointer);
        program.framePointer = program.stackValues.size() - argumentCount;
    }

    // opPopBase (0x467D3C): restore the frame pointer from the return stack.
    private static void opPopBase(Program program) {
        int data = programReturnStackPopInteger(program);
        program.framePointer = data;
    }

    // opPopToBase (0x467D94): discard values until the stack shrinks back to
    // the frame.
    private static void opPopToBase(Program program) {
        while (program.stackValues.size() != program.framePointer) {
            programStackPopValue(program);
        }
    }

    // opPopReturn (0x46B63C): set the IP from the top of the return stack.
    private static void opPopReturn(Program program) {
        program.instructionPointer = programReturnStackPopInteger(program);
    }

    // opAtoD (0x46BA2C): move the top return-stack cell onto the value stack.
    private static void opAtoD(Program program) {
        ProgramValue value = programReturnStackPopValue(program);
        programStackPushValue(program, value);
    }

    // opDtoA (0x46BA68): move the top value-stack cell onto the return stack.
    private static void opDtoA(Program program) {
        ProgramValue value = programStackPopValue(program);
        programReturnStackPushValue(program, value);
    }

    // opExit (0x46C1A0): EXITED plus the child-spawn detach and the
    // intLib reference removal (Nevs.removeProgramReferences in the port).
    private static void opExit(Program program) {
        program.flags |= PROGRAM_FLAG_EXITED;

        Program parent = program.parent;
        if (parent != null) {
            if ((parent.flags & PROGRAM_FLAG_CHILD_SPAWN) != 0) {
                parent.flags &= ~PROGRAM_FLAG_CHILD_SPAWN;
            }
        }

        if (!program.exited) {
            Nevs.removeProgramReferences(program);
            program.exited = true;
        }
    }

    // opStopProgram (0x46BAC8).
    private static void opStopProgram(Program program) {
        program.flags |= PROGRAM_FLAG_STOPPED;
    }

    // opSetGlobal (0x467DE0): base pointer = current stack size.
    private static void opSetGlobal(Program program) {
        program.basePointer = program.stackValues.size();
    }

    // opAdd (0x469BC4). String + anything concatenates (the string first, then
    // the other operand formatted); float + anything stays float; int + int
    // stays int unless it would overflow, in which case it promotes to float.
    private static void opAdd(Program program) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        switch (value[1].opcode) {
        case VALUE_TYPE_STRING:
        case VALUE_TYPE_DYNAMIC_STRING: {
            String strings1 = programGetString(program, value[1].opcode, value[1].integerValue);
            String strings0;
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                strings0 = programGetString(program, value[0].opcode, value[0].integerValue);
                break;
            case VALUE_TYPE_FLOAT:
                strings0 = formatFloat5(value[0].floatValue);
                break;
            case VALUE_TYPE_INT:
                strings0 = Integer.toString(value[0].integerValue);
                break;
            case VALUE_TYPE_PTR:
                strings0 = formatPointer(value[0].pointerValue);
                break;
            default:
                strings0 = "";
                break;
            }
            programStackPushString(program, strings1 + strings0);
            break;
        }
        case VALUE_TYPE_FLOAT:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING: {
                String strings0 = programGetString(program, value[0].opcode, value[0].integerValue);
                programStackPushString(program, formatFloat5(value[1].floatValue) + strings0);
                break;
            }
            case VALUE_TYPE_FLOAT:
                programStackPushFloat(program, value[1].floatValue + value[0].floatValue);
                break;
            case VALUE_TYPE_INT:
                programStackPushFloat(program, value[1].floatValue + (float) value[0].integerValue);
                break;
            default:
                break;
            }
            break;
        case VALUE_TYPE_INT:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING: {
                String strings0 = programGetString(program, value[0].opcode, value[0].integerValue);
                programStackPushString(program, Integer.toString(value[1].integerValue) + strings0);
                break;
            }
            case VALUE_TYPE_FLOAT:
                programStackPushFloat(program, (float) value[1].integerValue + value[0].floatValue);
                break;
            case VALUE_TYPE_INT:
                if ((value[0].integerValue <= 0
                        || (Integer.MAX_VALUE - value[0].integerValue) > value[1].integerValue)
                        && (value[0].integerValue >= 0
                                || (Integer.MIN_VALUE - value[0].integerValue) <= value[1].integerValue)) {
                    programStackPushInteger(program, value[1].integerValue + value[0].integerValue);
                } else {
                    programStackPushFloat(program,
                            (float) value[1].integerValue + (float) value[0].integerValue);
                }
                break;
            default:
                break;
            }
            break;
        case VALUE_TYPE_PTR:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING: {
                String strings0 = programGetString(program, value[0].opcode, value[0].integerValue);
                programStackPushString(program, formatPointer(value[1].pointerValue) + strings0);
                break;
            }
            default:
                break;
            }
            break;
        default:
            break;
        }
    }

    // opSubtract (0x46A1D8).
    private static void opSubtract(Program program) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        switch (value[1].opcode) {
        case VALUE_TYPE_FLOAT:
            switch (value[0].opcode) {
            case VALUE_TYPE_FLOAT:
                programStackPushFloat(program, value[1].floatValue - value[0].floatValue);
                break;
            default:
                programStackPushFloat(program, value[1].floatValue - (float) value[0].integerValue);
                break;
            }
            break;
        case VALUE_TYPE_INT:
            switch (value[0].opcode) {
            case VALUE_TYPE_FLOAT:
                programStackPushFloat(program, value[1].integerValue - value[0].floatValue);
                break;
            default:
                programStackPushInteger(program, value[1].integerValue - value[0].integerValue);
                break;
            }
            break;
        default:
            break;
        }
    }

    // opMultiply (0x46A300).
    private static void opMultiply(Program program) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        switch (value[1].opcode) {
        case VALUE_TYPE_FLOAT:
            switch (value[0].opcode) {
            case VALUE_TYPE_FLOAT:
                programStackPushFloat(program, value[1].floatValue * value[0].floatValue);
                break;
            default:
                programStackPushFloat(program, value[1].floatValue * value[0].integerValue);
                break;
            }
            break;
        case VALUE_TYPE_INT:
            switch (value[0].opcode) {
            case VALUE_TYPE_FLOAT:
                programStackPushFloat(program, value[1].integerValue * value[0].floatValue);
                break;
            default:
                programStackPushInteger(program, value[0].integerValue * value[1].integerValue);
                break;
            }
            break;
        default:
            break;
        }
    }

    // opDivide (0x46A424). A zero divisor is a C noreturn fatal.
    private static void opDivide(Program program) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        switch (value[1].opcode) {
        case VALUE_TYPE_FLOAT: {
            float divisor;
            if (value[0].opcode == VALUE_TYPE_FLOAT) {
                divisor = value[0].floatValue;
            } else {
                divisor = (float) value[0].integerValue;
            }
            if (divisor == 0.0f) {
                programFatalError("Division (DIV) by zero");
            }
            programStackPushFloat(program, value[1].floatValue / divisor);
            break;
        }
        case VALUE_TYPE_INT:
            if (value[0].opcode == VALUE_TYPE_FLOAT) {
                float divisor = value[0].floatValue;
                if (divisor == 0.0f) {
                    programFatalError("Division (DIV) by zero");
                }
                programStackPushFloat(program, (float) value[1].integerValue / divisor);
            } else {
                if (value[0].integerValue == 0) {
                    programFatalError("Division (DIV) by zero");
                }
                programStackPushInteger(program, value[1].integerValue / value[0].integerValue);
            }
            break;
        default:
            break;
        }
    }

    // opModulo (0x46A5B8). MOD rejects floats on either side.
    private static void opModulo(Program program) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        if (value[1].opcode == VALUE_TYPE_FLOAT) {
            programFatalError("Trying to MOD a float");
        }

        if (value[1].opcode != VALUE_TYPE_INT) {
            return;
        }

        if (value[0].opcode == VALUE_TYPE_FLOAT) {
            programFatalError("Trying to MOD with a float");
        }

        if (value[0].integerValue == 0) {
            programFatalError("Division (MOD) by zero");
        }

        programStackPushInteger(program, value[1].integerValue % value[0].integerValue);
    }

    // opLogicalOperatorNot (0x46AACC). NOTE: it reads integerValue, so for a
    // float this is the raw IEEE-754 bits, not the numeric value.
    private static void opLogicalOperatorNot(Program program) {
        ProgramValue value = programStackPopValue(program);
        programStackPushInteger(program, value.integerValue == 0 ? 1 : 0);
    }

    // opUnaryMinus (0x46AB2C).
    private static void opUnaryMinus(Program program) {
        ProgramValue programValue = programStackPopValue(program);
        switch (programValue.opcode) {
        case VALUE_TYPE_INT:
            programStackPushInteger(program, -programValue.integerValue);
            break;
        case VALUE_TYPE_FLOAT:
            programStackPushFloat(program, -programValue.floatValue);
            break;
        default:
            programFatalError("Invalid arg given to NEG");
        }
    }

    // opBitwiseOperatorNot (0x46AB84).
    private static void opBitwiseOperatorNot(Program program) {
        int value = programStackPopInteger(program);
        programStackPushInteger(program, ~value);
    }

    // opFloor (0x46ABDC). A float floors to an int; a string is a fatal.
    private static void opFloor(Program program) {
        ProgramValue value = programStackPopValue(program);

        if (value.opcode == VALUE_TYPE_STRING) {
            programFatalError("Invalid arg given to floor()");
        } else if (value.opcode == VALUE_TYPE_FLOAT) {
            value.opcode = VALUE_TYPE_INT;
            value.integerValue = (int) value.floatValue;
            value.floatValue = Float.intBitsToFloat(value.integerValue);
        }

        programStackPushValue(program, value);
    }

    // opBitwiseOperatorAnd (0x46AC78). Other types push nothing (C returns).
    private static void opBitwiseOperatorAnd(Program program) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        int result;
        switch (value[1].opcode) {
        case VALUE_TYPE_FLOAT:
            switch (value[0].opcode) {
            case VALUE_TYPE_FLOAT:
                result = (int) value[1].floatValue & (int) value[0].floatValue;
                break;
            default:
                result = (int) value[1].floatValue & value[0].integerValue;
                break;
            }
            break;
        case VALUE_TYPE_INT:
            switch (value[0].opcode) {
            case VALUE_TYPE_FLOAT:
                result = value[1].integerValue & (int) value[0].floatValue;
                break;
            default:
                result = value[1].integerValue & value[0].integerValue;
                break;
            }
            break;
        default:
            return;
        }

        programStackPushInteger(program, result);
    }

    // opBitwiseOperatorOr (0x46ADA4).
    private static void opBitwiseOperatorOr(Program program) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        int result;
        switch (value[1].opcode) {
        case VALUE_TYPE_FLOAT:
            switch (value[0].opcode) {
            case VALUE_TYPE_FLOAT:
                result = (int) value[1].floatValue | (int) value[0].floatValue;
                break;
            default:
                result = (int) value[1].floatValue | value[0].integerValue;
                break;
            }
            break;
        case VALUE_TYPE_INT:
            switch (value[0].opcode) {
            case VALUE_TYPE_FLOAT:
                result = value[1].integerValue | (int) value[0].floatValue;
                break;
            default:
                result = value[1].integerValue | value[0].integerValue;
                break;
            }
            break;
        default:
            return;
        }

        programStackPushInteger(program, result);
    }

    // opBitwiseOperatorXor (0x46AED0).
    private static void opBitwiseOperatorXor(Program program) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        int result;
        switch (value[1].opcode) {
        case VALUE_TYPE_FLOAT:
            switch (value[0].opcode) {
            case VALUE_TYPE_FLOAT:
                result = (int) value[1].floatValue ^ (int) value[0].floatValue;
                break;
            default:
                result = (int) value[1].floatValue ^ value[0].integerValue;
                break;
            }
            break;
        case VALUE_TYPE_INT:
            switch (value[0].opcode) {
            case VALUE_TYPE_FLOAT:
                result = value[1].integerValue ^ (int) value[0].floatValue;
                break;
            default:
                result = value[1].integerValue ^ value[0].integerValue;
                break;
            }
            break;
        default:
            return;
        }

        programStackPushInteger(program, result);
    }

    // The six comparison operators share one shape: pop two values, then
    // dispatch on the type of the first (value[1]) and, inside, the second
    // (value[0]). Strings compare with strcmp semantics; ints and floats
    // compare numerically; pointers have per-operator null-test special cases.
    private static void opCompare(Program program, Cmp cmp) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        int result;
        switch (value[1].opcode) {
        case VALUE_TYPE_STRING:
        case VALUE_TYPE_DYNAMIC_STRING: {
            String strings1 = programGetString(program, value[1].opcode, value[1].integerValue);
            String strings0;
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                strings0 = programGetString(program, value[0].opcode, value[0].integerValue);
                break;
            case VALUE_TYPE_FLOAT:
                strings0 = formatFloat5(value[0].floatValue);
                break;
            case VALUE_TYPE_INT:
                strings0 = Integer.toString(value[0].integerValue);
                break;
            default:
                invalidComparison(value, cmp);
                return;
            }
            result = cmp.strings(strings1, strings0) ? 1 : 0;
            break;
        }
        case VALUE_TYPE_FLOAT:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING: {
                String strings1 = formatFloat5(value[1].floatValue);
                String strings0 = programGetString(program, value[0].opcode, value[0].integerValue);
                result = cmp.strings(strings1, strings0) ? 1 : 0;
                break;
            }
            case VALUE_TYPE_FLOAT:
                result = cmp.floats(value[1].floatValue, value[0].floatValue) ? 1 : 0;
                break;
            case VALUE_TYPE_INT:
                result = cmp.floats(value[1].floatValue, (float) value[0].integerValue) ? 1 : 0;
                break;
            default:
                invalidComparison(value, cmp);
                return;
            }
            break;
        case VALUE_TYPE_INT:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING: {
                String strings1 = Integer.toString(value[1].integerValue);
                String strings0 = programGetString(program, value[0].opcode, value[0].integerValue);
                result = cmp.strings(strings1, strings0) ? 1 : 0;
                break;
            }
            case VALUE_TYPE_FLOAT:
                result = cmp.floats((float) value[1].integerValue, value[0].floatValue) ? 1 : 0;
                break;
            case VALUE_TYPE_INT:
                result = cmp.ints(value[1].integerValue, value[0].integerValue) ? 1 : 0;
                break;
            case VALUE_TYPE_PTR:
                // C casts both sides to uintptr_t; Java compares the null-ness
                // view instead (deliberate divergence, untested).
                if (cmp == Cmp.EQ) {
                    result = ((value[1].integerValue == 0) == (value[0].pointerValue == null)) ? 1 : 0;
                } else if (cmp == Cmp.NE) {
                    result = ((value[1].integerValue == 0) != (value[0].pointerValue == null)) ? 1 : 0;
                } else {
                    invalidComparison(value, cmp);
                    return;
                }
                break;
            default:
                invalidComparison(value, cmp);
                return;
            }
            break;
        case VALUE_TYPE_PTR:
            switch (value[0].opcode) {
            case VALUE_TYPE_INT: {
                boolean ptrNull = value[1].pointerValue == null;
                switch (cmp) {
                case EQ:
                    result = (ptrNull == (value[0].integerValue == 0)) ? 1 : 0;
                    break;
                case NE:
                    result = (ptrNull != (value[0].integerValue == 0)) ? 1 : 0;
                    break;
                case LE:
                    // ptr <= int: for int > 0 C compares addresses (almost
                    // always true); for int <= 0 it is the null test.
                    result = (value[0].integerValue > 0) ? 1 : (ptrNull ? 1 : 0);
                    break;
                case GE:
                    if (value[0].integerValue != 0) {
                        programFatalError("Invalid pointer comparison: >= only supports comparison to 0.");
                    }
                    result = 1;
                    break;
                case GT:
                    if (value[0].integerValue != 0) {
                        programFatalError("Invalid pointer comparison: > only supports comparison to 0.");
                    }
                    result = ptrNull ? 0 : 1;
                    break;
                default:
                    invalidComparison(value, cmp);
                    return;
                }
                break;
            }
            case VALUE_TYPE_PTR:
                switch (cmp) {
                case EQ:
                    result = (value[1].pointerValue == value[0].pointerValue) ? 1 : 0;
                    break;
                case NE:
                    result = (value[1].pointerValue != value[0].pointerValue) ? 1 : 0;
                    break;
                default:
                    invalidComparison(value, cmp);
                    return;
                }
                break;
            default:
                invalidComparison(value, cmp);
                return;
            }
            break;
        default:
            invalidComparison(value, cmp);
            return;
        }

        programStackPushInteger(program, result);
    }

    private static void invalidComparison(ProgramValue[] value, Cmp cmp) {
        programFatalError("Invalid comparison: " + cmp.symbol() + " does not support "
                + value[1].typeDebugString() + " and " + value[0].typeDebugString() + ".");
    }

    private static void opConditionalOperatorEqual(Program program) {
        opCompare(program, Cmp.EQ);
    }

    private static void opConditionalOperatorNotEqual(Program program) {
        opCompare(program, Cmp.NE);
    }

    private static void opConditionalOperatorLessThan(Program program) {
        opCompare(program, Cmp.LT);
    }

    private static void opConditionalOperatorLessThanEquals(Program program) {
        opCompare(program, Cmp.LE);
    }

    private static void opConditionalOperatorGreaterThan(Program program) {
        opCompare(program, Cmp.GT);
    }

    private static void opConditionalOperatorGreaterThanEquals(Program program) {
        opCompare(program, Cmp.GE);
    }

    // opLogicalOperatorAnd (0x46A6B4) / opLogicalOperatorOr (0x46A8D8).
    // NOTE: the float cases read integerValue (the union view, i.e. the raw
    // IEEE-754 bits) and mask it with 0x7FFFFFFF, exactly like C.
    private static void opLogicalOperatorAnd(Program program) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        int result;
        switch (value[1].opcode) {
        case VALUE_TYPE_STRING:
        case VALUE_TYPE_DYNAMIC_STRING:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                result = 1;
                break;
            case VALUE_TYPE_FLOAT:
                result = (value[0].integerValue & 0x7FFFFFFF) != 0 ? 1 : 0;
                break;
            case VALUE_TYPE_INT:
                result = value[0].integerValue != 0 ? 1 : 0;
                break;
            case VALUE_TYPE_PTR:
                result = value[0].pointerValue != null ? 1 : 0;
                break;
            default:
                result = 0;
                break;
            }
            break;
        case VALUE_TYPE_FLOAT:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                result = value[1].integerValue != 0 ? 1 : 0;
                break;
            case VALUE_TYPE_FLOAT:
                result = ((value[1].integerValue & 0x7FFFFFFF) != 0
                        && (value[0].integerValue & 0x7FFFFFFF) != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_INT:
                result = ((value[1].integerValue & 0x7FFFFFFF) != 0
                        && value[0].integerValue != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_PTR:
                result = ((value[1].integerValue & 0x7FFFFFFF) != 0
                        && value[0].pointerValue != null) ? 1 : 0;
                break;
            default:
                result = 0;
                break;
            }
            break;
        case VALUE_TYPE_INT:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                result = value[1].integerValue != 0 ? 1 : 0;
                break;
            case VALUE_TYPE_FLOAT:
                result = (value[1].integerValue != 0
                        && (value[0].integerValue & 0x7FFFFFFF) != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_INT:
                result = (value[1].integerValue != 0 && value[0].integerValue != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_PTR:
                result = (value[1].integerValue != 0 && value[0].pointerValue != null) ? 1 : 0;
                break;
            default:
                result = 0;
                break;
            }
            break;
        case VALUE_TYPE_PTR:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                result = value[1].pointerValue != null ? 1 : 0;
                break;
            case VALUE_TYPE_FLOAT:
                result = (value[1].pointerValue != null
                        && (value[0].integerValue & 0x7FFFFFFF) != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_INT:
                result = (value[1].pointerValue != null && value[0].integerValue != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_PTR:
                result = (value[1].pointerValue != null && value[0].pointerValue != null) ? 1 : 0;
                break;
            default:
                result = 0;
                break;
            }
            break;
        default:
            result = 0;
            break;
        }

        programStackPushInteger(program, result);
    }

    private static void opLogicalOperatorOr(Program program) {
        ProgramValue[] value = new ProgramValue[2];
        for (int arg = 0; arg < 2; arg++) {
            value[arg] = programStackPopValue(program);
        }

        int result;
        switch (value[1].opcode) {
        case VALUE_TYPE_STRING:
        case VALUE_TYPE_DYNAMIC_STRING:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
            case VALUE_TYPE_FLOAT:
            case VALUE_TYPE_INT:
            case VALUE_TYPE_PTR:
                result = 1;
                break;
            default:
                result = 0;
                break;
            }
            break;
        case VALUE_TYPE_FLOAT:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                result = 1;
                break;
            case VALUE_TYPE_FLOAT:
                result = ((value[1].integerValue & 0x7FFFFFFF) != 0
                        || (value[0].integerValue & 0x7FFFFFFF) != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_INT:
                result = ((value[1].integerValue & 0x7FFFFFFF) != 0
                        || value[0].integerValue != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_PTR:
                result = ((value[1].integerValue & 0x7FFFFFFF) != 0
                        || value[0].pointerValue != null) ? 1 : 0;
                break;
            default:
                result = 0;
                break;
            }
            break;
        case VALUE_TYPE_INT:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                result = 1;
                break;
            case VALUE_TYPE_FLOAT:
                result = (value[1].integerValue != 0
                        || (value[0].integerValue & 0x7FFFFFFF) != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_INT:
                result = (value[1].integerValue != 0 || value[0].integerValue != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_PTR:
                result = (value[1].integerValue != 0 || value[0].pointerValue != null) ? 1 : 0;
                break;
            default:
                result = 0;
                break;
            }
            break;
        case VALUE_TYPE_PTR:
            switch (value[0].opcode) {
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                result = 1;
                break;
            case VALUE_TYPE_FLOAT:
                result = (value[1].pointerValue != null
                        || (value[0].integerValue & 0x7FFFFFFF) != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_INT:
                result = (value[1].pointerValue != null || value[0].integerValue != 0) ? 1 : 0;
                break;
            case VALUE_TYPE_PTR:
                result = (value[1].pointerValue != null || value[0].pointerValue != null) ? 1 : 0;
                break;
            default:
                result = 0;
                break;
            }
            break;
        default:
            result = 0;
            break;
        }

        programStackPushInteger(program, result);
    }

    // opJump (0x46B080): set the IP from the top of the value stack.
    private static void opJump(Program program) {
        program.instructionPointer = programStackPopInteger(program);
    }

    // opIf (0x468400): pop the condition; truthy discards the jump target,
    // falsy pops it into the IP.
    private static void opIf(Program program) {
        ProgramValue value = programStackPopValue(program);
        if (!value.isEmpty()) {
            programStackPopValue(program);
        } else {
            program.instructionPointer = programStackPopInteger(program);
        }
    }

    // opWhile (0x4684A4): pop the condition; falsy pops the jump target into
    // the IP. NOTE: the truthy path leaves the target on the stack, matching
    // C (the compiler's body is responsible for it).
    private static void opWhile(Program program) {
        ProgramValue value = programStackPopValue(program);
        if (value.isEmpty()) {
            program.instructionPointer = programStackPopInteger(program);
        }
    }

    // opStore (0x468518): store into the frame at framePointer + addr,
    // dropping the old value's dynamic-string refcount and bumping the new.
    private static void opStore(Program program) {
        int addr = programStackPopInteger(program);
        ProgramValue value = programStackPopValue(program);
        int pos = program.framePointer + addr;

        ProgramValue oldValue = programStackAt(program, pos);
        if (oldValue.opcode == VALUE_TYPE_DYNAMIC_STRING) {
            interpreterStringRefCountDecrease(program, oldValue.opcode, oldValue.integerValue);
        }

        programStackSet(program, pos, value);

        if (value.opcode == VALUE_TYPE_DYNAMIC_STRING) {
            interpreterStringRefCountIncrease(program, VALUE_TYPE_DYNAMIC_STRING, value.integerValue);
        }
    }

    // opConditionalCall (0x468C78): CALL_WHEN arms a procedure's condition.
    // Two integers come off the stack in this order: data[0] (the TOP of stack)
    // is the procedure index, data[1] is the condition offset. So a script
    // pushes the condition offset first and the index second.
    private static void opConditionalCall(Program program) {
        int data0 = programStackPopInteger(program);
        int data1 = programStackPopInteger(program);

        int procedurePtr = program.procedures + 4 + PROCEDURE_SIZE * data0;
        int flags = stackReadInt32(program.data, procedurePtr + PROCEDURE_FLAGS_OFFSET);

        stackWriteInt32(flags | PROCEDURE_FLAG_CONDITIONAL, program.data, procedurePtr + PROCEDURE_FLAGS_OFFSET);
        stackWriteInt32(data1, program.data, procedurePtr + PROCEDURE_CONDITION_OFFSET);
    }

    // opCancel (0x469BA8): drop one procedure's scheduled/conditional state.
    // Unlike CALL_AT and WAIT this never reads the clock, so it is the one
    // part of the delayed-call family that vectors deterministically.
    private static void opCancel(Program program) {
        int data = programStackPopInteger(program);

        if (data < 0 || data >= program.procedureCount()) {
            programFatalError("Invalid procedure offset given to cancel");
        }

        int procedurePtr = program.procedures + 4 + PROCEDURE_SIZE * data;
        stackWriteInt32(0, program.data, procedurePtr + PROCEDURE_FLAGS_OFFSET);
        stackWriteInt32(0, program.data, procedurePtr + PROCEDURE_TIME_OFFSET);
        stackWriteInt32(0, program.data, procedurePtr + PROCEDURE_CONDITION_OFFSET);
    }

    // opCancelAll (0x469CE0): opCancel over every procedure. C's loop starts at
    // index 0, unlike opLookupStringProc which skips main.
    private static void opCancelAll(Program program) {
        int count = program.procedureCount();

        for (int index = 0; index < count; index++) {
            int procedurePtr = program.procedures + 4 + PROCEDURE_SIZE * index;
            stackWriteInt32(0, program.data, procedurePtr + PROCEDURE_FLAGS_OFFSET);
            stackWriteInt32(0, program.data, procedurePtr + PROCEDURE_TIME_OFFSET);
            stackWriteInt32(0, program.data, procedurePtr + PROCEDURE_CONDITION_OFFSET);
        }
    }

    // ---------------------------------------------------------------------
    // Program lifecycle: CALLSTART / SPAWN / FORK / EXEC.
    //
    // All four reach runScript, which loads a script by path and immediately
    // runs it for 24 instructions. C routes the name through
    // interpreterFilenameMangler, whose default (defaultFilename) is the
    // identity function, so the popped string IS the path.
    // ---------------------------------------------------------------------

    /** Port of {@code ProgramListNode}: a doubly-linked node over programs. */
    private static final class ProgramListNode {
        final Program program;
        ProgramListNode next;
        ProgramListNode prev;

        ProgramListNode(Program program) {
            this.program = program;
        }
    }

    /** Port of {@code gInterpreterProgramListHead}. */
    private static ProgramListNode programListHead;

    // programListNodeCreate (0x468248): push onto the front of the list.
    private static void programListNodeCreate(Program program) {
        ProgramListNode node = new ProgramListNode(program);
        node.next = programListHead;
        node.prev = null;

        if (programListHead != null) {
            programListHead.prev = node;
        }

        programListHead = node;
    }

    // runProgram (0x4682B4).
    private static void runProgram(Program program) {
        program.flags |= PROGRAM_FLAG_RUNNING;
        programListNodeCreate(program);
    }

    /**
     * runScript (0x46824C): load a script by name and give it a 24-instruction
     * head start. Returns null if the file will not load - C lets the callers
     * turn that into a programFatalError rather than raising here.
     */
    static Program runScript(String name) {
        Program program = programCreateByPath(name);
        if (program != null) {
            runProgram(program);
            programInterpret(program, 24);
        }
        return program;
    }

    // forkProgram (0x469C7C): pop a script name and run a copy of it. Note the
    // caller drops the result on the floor - only EXEC keeps it.
    private static Program forkProgram(Program program) {
        String name = programStackPopString(program);
        Program forked = runScript(name);

        if (forked == null) {
            programFatalError("couldn't fork script '" + name + "'");
        }

        forked.windowId = program.windowId;
        return forked;
    }

    // opCallStart (0x46BBFC): run a child script and keep it attached.
    private static void opCallStart(Program program) {
        if (program.child != null) {
            programFatalError("Error, already have a child process\n");
        }

        program.flags |= PROGRAM_FLAG_CHILD_CALL;

        String name = programStackPopString(program);

        Program child = runScript(name);
        if (child == null) {
            programFatalError("Error spawning child " + name);
        }

        child.parent = program;
        child.windowId = program.windowId;
        program.child = child;
    }

    // opSpawn (0x469C28): opCallStart with the SPAWN flag, and a critical
    // section runs the child to completion before carrying on.
    private static void opSpawn(Program program) {
        if (program.child != null) {
            programFatalError("Error, already have a child process\n");
        }

        program.flags |= PROGRAM_FLAG_CHILD_SPAWN;

        String name = programStackPopString(program);

        Program child = runScript(name);
        if (child == null) {
            programFatalError("Error spawning child " + name);
        }

        child.parent = program;
        child.windowId = program.windowId;
        program.child = child;

        if ((program.flags & PROGRAM_FLAG_CRITICAL_SECTION) != 0) {
            child.flags |= PROGRAM_FLAG_CRITICAL_SECTION;
            programInterpret(child, -1);
        }
    }

    // opFork (0x469C70): the fork is deliberately discarded. Only EXEC keeps it.
    private static void opFork(Program program) {
        forkProgram(program);
    }

    // opExec (0x469A74): replace this program with a fork, handing the parent
    // the new child. The trailing parent re-read is verbatim: C re-assigns
    // `parent = program->parent` AFTER nulling it, so the CHILD_SPAWN-clearing
    // branch can never run. The original even flags it as a probable inlining
    // artefact. Kept so the port matches rather than "fixes" it.
    private static void opExec(Program program) {
        Program parent = program.parent;
        Program fork = forkProgram(program);

        if (parent != null) {
            fork.parent = parent;
            parent.child = fork;
        }

        fork.child = null;

        program.parent = null;
        program.flags |= PROGRAM_FLAG_EXITED;

        parent = program.parent;
        if (parent != null) {
            if ((parent.flags & PROGRAM_FLAG_CHILD_SPAWN) != 0) {
                parent.flags &= ~PROGRAM_FLAG_CHILD_SPAWN;
            }
        }

        purgeProgram(program);
    }

    // opFetchExternalVariable (0x46C0F4): read a shared variable by identifier.
    // The popped value supplies the identifier offset, not the name itself.
    private static void opFetchExternalVariable(Program program) {
        ProgramValue addr = programStackPopValue(program);
        String identifier = programGetIdentifier(program, addr.integerValue);

        ProgramValue value = new ProgramValue();
        if (Export.getValue(program, identifier, value) != 0) {
            programFatalError("External variable " + identifier + " does not exist\n");
        }

        programStackPushValue(program, value);
    }

    // opStoreExternalVariable (0x46C13C): identifier offset on top, value under.
    private static void opStoreExternalVariable(Program program) {
        ProgramValue addr = programStackPopValue(program);
        ProgramValue value = programStackPopValue(program);
        String identifier = programGetIdentifier(program, addr.integerValue);

        if (Export.setValue(program, identifier, value) != 0) {
            programFatalError("External variable " + identifier + " does not exist\n");
        }
    }

    // opExportVariable (0x46C0A0): declare a shared variable, initialised to 0.
    private static void opExportVariable(Program program) {
        ProgramValue addr = programStackPopValue(program);
        String identifier = programGetIdentifier(program, addr.integerValue);

        if (Export.createVariable(program.name, identifier) != 0) {
            programFatalError("External variable " + identifier + " already exists");
        }
    }

    // opExportProcedure (0x46C084): publish a procedure so another script can
    // call it. Pops the argument count first, then the procedure index.
    private static void opExportProcedure(Program program) {
        int procedureIndex = programStackPopInteger(program);
        int argumentCount = programStackPopInteger(program);

        int procedurePtr = program.procedures + 4 + PROCEDURE_SIZE * procedureIndex;
        String procedureName = programGetIdentifier(program,
            stackReadInt32(program.data, procedurePtr + PROCEDURE_NAME_OFFSET));
        int procedureAddress = stackReadInt32(program.data, procedurePtr + PROCEDURE_BODY_OFFSET);

        if (Export.createProcedure(program, procedureName, procedureAddress, argumentCount) != 0) {
            programFatalError("Error exporting procedure " + procedureName);
        }
    }

    // setupExternalCallWithReturnVal (0x46D0B0): prepare the callee for a
    // cross-program call. The callee's return stack receives the caller's
    // saved state and the callee's VALUE stack receives the frame that
    // opPopFlags will restore.
    //
    // NOTE the push order on the return stack. The four consumers below pop
    // them as caller, checkWaitFunc, flags, returnAddress - so a LIFO stack
    // needs returnAddress pushed FIRST. C pushes it LAST, which leaves the
    // consumers reading the wrong slots. Ported verbatim, bug and all; see
    // the opPopFlags*Extern comments.
    private static void setupExternalCallWithReturnVal(Program caller, Program callee,
        int address, int returnAddress) {
        programReturnStackPushInteger(callee, callee.instructionPointer);

        programReturnStackPushInteger(callee, caller.flags & 0xFFFF);

        programReturnStackPushPointer(callee, caller.checkWaitFunc);

        programReturnStackPushPointer(callee, caller);

        programReturnStackPushInteger(callee, returnAddress);

        programStackPushInteger(callee, callee.flags & 0xFFFF);

        programStackPushPointer(callee, callee.checkWaitFunc);

        programStackPushInteger(callee, callee.windowId);

        callee.flags &= ~0xFFFF;
        callee.instructionPointer = address;
        callee.windowId = caller.windowId;

        caller.flags |= PROGRAM_FLAG_CHILD_CALL;
    }

    // setupExternalCall (0x46DB18): the above plus a trailing zero, which the
    // callee sees as the "no return value" slot.
    private static void setupExternalCall(Program caller, Program callee, int address,
        int returnAddress) {
        setupExternalCallWithReturnVal(caller, callee, address, returnAddress);
        programStackPushInteger(callee, 0);
    }

    // The four "return from an external call" opcodes. They restore the
    // caller's state from the callee's return stack, in the order caller,
    // checkWaitFunc, flags, returnAddress.
    //
    // C pops them with programReturnStackPopPointer / PopInteger, which read
    // a union - so an int read as a pointer yields the int's bits. Java's
    // ProgramValue keeps the two apart, so an int read as a pointer is null
    // here. Only the order matters for the port: see setupExternalCall.

    // opPopFlagsReturnExtern (0x46C250): return to the caller.
    private static void opPopFlagsReturnExtern(Program program) {
        opPopFlags(program);

        Program caller = (Program) programReturnStackPopPointer(program);
        caller.checkWaitFunc = (InterpretCheckWaitFunc) programReturnStackPopPointer(program);
        caller.flags = programReturnStackPopInteger(program);

        program.instructionPointer = programReturnStackPopInteger(program);
    }

    // opPopFlagsExitExtern (0x46C2C0): return to the caller and finish.
    private static void opPopFlagsExitExtern(Program program) {
        opPopFlags(program);

        Program caller = (Program) programReturnStackPopPointer(program);
        caller.checkWaitFunc = (InterpretCheckWaitFunc) programReturnStackPopPointer(program);
        caller.flags = programReturnStackPopInteger(program);

        program.instructionPointer = programReturnStackPopInteger(program);

        program.flags |= PROGRAM_FLAG_FINISHED;
    }

    // opPopFlagsReturnValExtern (0x46C330): return to the caller, handing a
    // value to the CALLER's stack. A string is re-interned into the caller's
    // own heap, because the callee's offset means nothing there.
    private static void opPopFlagsReturnValExtern(Program program) {
        ProgramValue value = programStackPopValue(program);

        opPopFlags(program);

        Program caller = (Program) programReturnStackPopPointer(program);
        caller.checkWaitFunc = (InterpretCheckWaitFunc) programReturnStackPopPointer(program);
        caller.flags = programReturnStackPopInteger(program);
        if ((value.opcode & 0xF7FF) == VALUE_TYPE_STRING) {
            String string = programGetString(program, value.opcode, value.integerValue);
            ProgramValue otherValue = new ProgramValue();
            otherValue.integerValue = programPushString(caller, string);
            otherValue.opcode = VALUE_TYPE_DYNAMIC_STRING;
            programStackPushValue(caller, otherValue);
        } else {
            programStackPushValue(caller, value);
        }

        if ((caller.flags & PROGRAM_FLAG_CRITICAL_SECTION) != 0) {
            program.flags &= ~PROGRAM_FLAG_CRITICAL_SECTION;
        }

        program.instructionPointer = programReturnStackPopInteger(program);
        caller.instructionPointer = programReturnStackPopInteger(caller);
    }

    // opPopFlagsReturnValExitExtern (0x46C3B8): unlike the variant above, the
    // value goes back on the CALLEE's stack, not the caller's.
    private static void opPopFlagsReturnValExitExtern(Program program) {
        ProgramValue value = programStackPopValue(program);

        opPopFlags(program);

        Program caller = (Program) programReturnStackPopPointer(program);
        caller.checkWaitFunc = (InterpretCheckWaitFunc) programReturnStackPopPointer(program);
        caller.flags = programReturnStackPopInteger(program);

        program.instructionPointer = programReturnStackPopInteger(program);

        program.flags |= PROGRAM_FLAG_FINISHED;

        programStackPushValue(program, value);
    }

    // opDelayedCall (0x468A38): CALL_AT schedules a procedure to run later.
    // The two popped integers are the procedure index (on top) and the delay
    // in seconds.
    private static void opDelayedCall(Program program) {
        int data0 = programStackPopInteger(program);
        int data1 = programStackPopInteger(program);

        int procedurePtr = program.procedures + 4 + PROCEDURE_SIZE * data0;

        int delay = 1000 * data1;

        if (!interpreterEventsSuspended) {
            delay += (int) getInterpreterTime();
        }

        int flags = stackReadInt32(program.data, procedurePtr + PROCEDURE_FLAGS_OFFSET);

        stackWriteInt32(delay, program.data, procedurePtr + PROCEDURE_TIME_OFFSET);
        stackWriteInt32(flags | PROCEDURE_FLAG_TIMED, program.data, procedurePtr + PROCEDURE_FLAGS_OFFSET);
    }

    // opWait (0x46B0DC): suspend until the interpreter clock passes waitEnd.
    private static void opWait(Program program) {
        int data = programStackPopInteger(program);

        program.waitStart = getInterpreterTime();
        program.waitEnd = program.waitStart + data;
        program.checkWaitFunc = Program::checkWait;
        program.flags |= PROGRAM_IS_WAITING;
    }

    /**
     * checkWait (0x46B0A4). Note the sense: it returns TRUE while the wait is
     * still in the future, and the VM loop keeps waiting when it returns
     * FALSE - so a program waits past its deadline and does not wait at all
     * before it. Ported as C has it.
     */
    private static int checkWait(Program program) {
        return getInterpreterTime() <= program.waitEnd ? 1 : 0;
    }

    /**
     * getInterpreterTime (0x4670A4): {@code 1000 * timerFunc() / 1000} over
     * unsigned 32-bit arithmetic, so the multiply wraps.
     */
    static long getInterpreterTime() {
        long ticks = Ticks.getTicks() & 0xFFFFFFFFL;
        long scaled = (1000L * ticks) & 0xFFFFFFFFL;
        return scaled / 1000L;
    }

    /**
     * Port of the file-static {@code interpreterEventsSuspended}. C never
     * writes it, so the branch that would skip the clock in opDelayedCall is
     * dead there too.
     */
    static boolean interpreterEventsSuspended = false;

    // opFetchGlobalVariable (0x46BAD0): push the global at basePointer + addr.
    // Globals are addressed by NEGATIVE offsets: opSetGlobal snapshots
    // basePointer = stackValues.size(), so the only in-range addresses are
    // -(size)..-1 - the values that were already on the stack when SET_GLOBAL
    // ran. Locals are positive offsets from framePointer, which is why the base
    // never moves as locals come and go.
    private static void opFetchGlobalVariable(Program program) {
        int addr = programStackPopInteger(program);
        ProgramValue value = programStackAt(program, program.basePointer + addr);
        programStackPushValue(program, value);
    }

    // opStoreGlobalVariable (0x46BB5C): opStore's refcount discipline against
    // the global slot at basePointer + addr.
    private static void opStoreGlobalVariable(Program program) {
        int addr = programStackPopInteger(program);
        ProgramValue value = programStackPopValue(program);
        int pos = program.basePointer + addr;

        ProgramValue oldValue = programStackAt(program, pos);
        if (oldValue.opcode == VALUE_TYPE_DYNAMIC_STRING) {
            interpreterStringRefCountDecrease(program, oldValue.opcode, oldValue.integerValue);
        }

        programStackSet(program, pos, value);

        if (value.opcode == VALUE_TYPE_DYNAMIC_STRING) {
            interpreterStringRefCountIncrease(program, VALUE_TYPE_DYNAMIC_STRING, value.integerValue);
        }
    }

    // opDump (0x467DEC): discard the top N values. NOTE the loop is verbatim -
    // C comments that the original walked backwards to -1.
    private static void opDump(Program program) {
        int data = programStackPopInteger(program);
        for (int index = 0; index < data; index++) {
            programStackPopValue(program);
        }
    }

    // opFetch (0x468678): push the frame value at framePointer + addr.
    private static void opFetch(Program program) {
        int addr = programStackPopInteger(program);
        ProgramValue value = programStackAt(program, program.framePointer + addr);
        programStackPushValue(program, value);
    }

    // opPopExit (0x46B658): return from a procedure, marking FINISHED.
    private static void opPopExit(Program program) {
        program.instructionPointer = programReturnStackPopInteger(program);
        program.flags |= PROGRAM_FLAG_FINISHED;
    }

    // opPopFlags (0x46B590): restore windowId / checkWaitFunc / flags from the
    // value stack (the frame programSetupCall pushed).
    private static void opPopFlags(Program program) {
        program.windowId = programStackPopInteger(program);
        program.checkWaitFunc = (InterpretCheckWaitFunc) programStackPopPointer(program);
        program.flags = programStackPopInteger(program) & 0xFFFF;
    }

    // opPopFlagsReturn (0x46B67C).
    private static void opPopFlagsReturn(Program program) {
        opPopFlags(program);
        program.instructionPointer = programReturnStackPopInteger(program);
    }

    // opPopFlagsExit (0x46B698).
    private static void opPopFlagsExit(Program program) {
        opPopFlags(program);
        program.instructionPointer = programReturnStackPopInteger(program);
        program.flags |= PROGRAM_FLAG_FINISHED;
    }

    // opPopAddress (0x46BA10): discard the top return-stack cell.
    private static void opPopAddress(Program program) {
        programReturnStackPopValue(program);
    }

    // opCall (0x46B108): jump to a procedure body by index. Imported
    // procedures are a TODO in C too.
    private static void opCall(Program program) {
        int value = programStackPopInteger(program);
        int ptr = program.procedures + 4 + PROCEDURE_SIZE * value;
        int flags = stackReadInt32(program.data, ptr + 4);
        if ((flags & PROCEDURE_FLAG_IMPORTED) != 0) {
            // TODO: Incomplete (matches C).
        } else {
            program.instructionPointer = stackReadInt32(program.data, ptr + 16);
            if ((flags & PROCEDURE_FLAG_CRITICAL) != 0) {
                program.flags |= PROGRAM_FLAG_CRITICAL_SECTION;
            }
        }
    }

    // programSetupCallWithReturnVal (0x46CED0): save the caller's IP and the
    // return address on the return stack, save flags/windowId/checkWaitFunc on
    // the value stack, then jump to the callee.
    static void programSetupCallWithReturnVal(Program program, int address, int returnAddress) {
        programReturnStackPushInteger(program, program.instructionPointer);
        programReturnStackPushInteger(program, returnAddress);
        programStackPushInteger(program, program.flags & 0xFFFF);
        programStackPushPointer(program, program.checkWaitFunc);
        programStackPushInteger(program, program.windowId);
        program.flags &= ~0xFFFF;
        program.instructionPointer = address;
    }

    // programSetupCall (0x46CF78): the same, plus a trailing zero.
    static void programSetupCall(Program program, int address, int returnAddress) {
        programSetupCallWithReturnVal(program, address, returnAddress);
        programStackPushInteger(program, 0);
    }

    // programExecuteProcedure (0x46DD2C), local branch. The return address 24
    // is the fixed offset of the compiler's POP, POP_FLAGS_EXIT sequence in a
    // script's top-level code (verified against scripts\ABEL.int). The C
    // jmp_buf save/restore around programInterpret has no Java form; the catch
    // in programInterpret is the equivalent.
    public static void programExecuteProcedure(Program program, int procedureIndex) {
        int procedurePtr = program.procedures + 4 + PROCEDURE_SIZE * procedureIndex;
        int procedureFlags = stackReadInt32(program.data, procedurePtr + 4);
        if ((procedureFlags & PROCEDURE_FLAG_IMPORTED) != 0) {
            // External procedure: resolved through the shared registry.
            String procedureIdentifier = programGetIdentifier(program,
                stackReadInt32(program.data, procedurePtr + PROCEDURE_NAME_OFFSET));
            Export.ExternalProcedure external = Export.lookupProcedure(procedureIdentifier);
            if (external == null || external.program == null) {
                return;
            }
            if (external.argumentCount != 0) {
                // C refuses an external procedure with arguments here.
                return;
            }

            setupExternalCall(program, external.program, external.address, 32);
            programInterpret(external.program, -1);
            return;
        }

        int procedureAddress = stackReadInt32(program.data, procedurePtr + 16);
        programSetupCall(program, procedureAddress, 24);
        programInterpret(program, -1);
    }

    // opPopFlagsReturnValExit (0x46B6BC): return a value to the caller. The
    // value is popped first and re-pushed after the frame is restored, so it
    // lands on top of whatever the caller had left. Pairs with
    // programSetupCallWithReturnVal (no trailing zero), unlike the plain
    // POP_FLAGS_* pair.
    private static void opPopFlagsReturnValExit(Program program) {
        ProgramValue value = programStackPopValue(program);
        opPopFlags(program);
        program.instructionPointer = programReturnStackPopInteger(program);
        program.flags |= PROGRAM_FLAG_FINISHED;
        programStackPushValue(program, value);
    }

    // opDetach (0x46C1A0): unlink from the parent, clearing both child flags.
    private static void opDetach(Program program) {
        Program parent = program.parent;
        if (parent == null) {
            return;
        }

        parent.flags &= ~PROGRAM_FLAG_CHILD_CALL;
        parent.flags &= ~PROGRAM_FLAG_CHILD_SPAWN;
        if (parent.child == program) {
            parent.child = null;
        }
    }

    // opCheckProcedureArgumentCount (0x46C574): pops (index, expected) and
    // compares against the table's argCount; a mismatch is a fatal that
    // names the procedure.
    private static void opCheckProcedureArgumentCount(Program program) {
        int expectedArgumentCount = programStackPopInteger(program);
        int procedureIndex = programStackPopInteger(program);
        int procedurePtr = program.procedures + 4 + PROCEDURE_SIZE * procedureIndex;
        if (stackReadInt32(program.data, procedurePtr + 20) != expectedArgumentCount) {
            programFatalError("Wrong number of args to procedure "
                + programGetIdentifier(program, stackReadInt32(program.data, procedurePtr)) + "\n");
        }
    }

    // opLookupStringProc (0x46C6B4): pops a procedure name and pushes its
    // index. Index 0 (main) is skipped - it cannot be looked up - so the
    // search starts at 1 and the first match wins even when a later
    // procedure repeats the name. The compare is compat_stricmp
    // (SDL_strcasecmp), i.e. ASCII case-insensitive; equalsIgnoreCase is the
    // Java analogue.
    private static void opLookupStringProc(Program program) {
        String nameToLookup = programStackPopString(program);
        int procedureCount = program.procedureCount();
        for (int index = 1; index < procedureCount; index++) {
            int procedurePtr = program.procedures + 4 + PROCEDURE_SIZE * index;
            String name = programGetIdentifier(program, stackReadInt32(program.data, procedurePtr));
            if (name.equalsIgnoreCase(nameToLookup)) {
                programStackPushInteger(program, index);
                return;
            }
        }

        programFatalError("Couldn't find string procedure " + nameToLookup + "\n");
    }

    // opFetchProcedureAddress (0x46BD60): push a procedure's bodyOffset.
    private static void opFetchProcedureAddress(Program program) {
        int procedureIndex = programStackPopInteger(program);
        int procedurePtr = program.procedures + 4 + PROCEDURE_SIZE * procedureIndex;
        programStackPushInteger(program, stackReadInt32(program.data, procedurePtr + 16));
    }

    // opEnterCriticalSection / opLeaveCriticalSection (0x46B078 / 0x46B06C).
    // START_CRITICAL / END_CRITICAL (0x804A / 0x804B) are the extended-set
    // aliases registered to the same two handlers.

    private static void opEnterCriticalSection(Program program) {
        program.flags |= PROGRAM_FLAG_CRITICAL_SECTION;
    }

    private static void opLeaveCriticalSection(Program program) {
        program.flags &= ~PROGRAM_FLAG_CRITICAL_SECTION;
    }

    // C snprintf("%.5f", ...) under the C locale; Locale.ROOT keeps the
    // decimal point stable regardless of the JVM default locale.
    private static String formatFloat5(float value) {
        return String.format(java.util.Locale.ROOT, "%.5f", value);
    }

    // C's "%p" prints the raw address, which Java cannot reproduce. This is a
    // debug-only path (object + string concatenation); it keeps a stable
    // non-null rendering instead. Deliberate divergence, untested.
    private static String formatPointer(Object pointer) {
        return pointer == null ? "(nil)" : pointer.toString();
    }

    /**
     * Port of {@code interpreterRegisterOpcodeHandlers} for the subset these
     * slices run. C registers all 76 handlers here; the rest arrive with
     * their opcodes.
     */
    static void interpreterRegisterOpcodeHandlers() {
        interpreterEnabled = true;
        // The game's own builtins come from the script API, so a script can be
        // run against a mock instead of the game.
        GameApiDispatch.install(gameApi);
        interpreterRegisterOpcode(OPCODE_NOOP, Program::opNoop);
        interpreterRegisterOpcode(OPCODE_PUSH, Program::opPush);
        interpreterRegisterOpcode(OPCODE_A_TO_D, Program::opAtoD);
        interpreterRegisterOpcode(OPCODE_D_TO_A, Program::opDtoA);
        interpreterRegisterOpcode(OPCODE_EXIT, Program::opExit);
        interpreterRegisterOpcode(OPCODE_EXIT_PROGRAM, Program::opExitProgram);
        interpreterRegisterOpcode(OPCODE_STOP_PROGRAM, Program::opStopProgram);
        interpreterRegisterOpcode(OPCODE_SWAP, Program::opSwapStack);
        interpreterRegisterOpcode(OPCODE_SWAPA, Program::opSwapReturnStack);
        interpreterRegisterOpcode(OPCODE_POP, Program::opPop);
        interpreterRegisterOpcode(OPCODE_DUP, Program::opDuplicate);
        interpreterRegisterOpcode(OPCODE_POP_RETURN, Program::opPopReturn);
        interpreterRegisterOpcode(OPCODE_POP_BASE, Program::opPopBase);
        interpreterRegisterOpcode(OPCODE_POP_TO_BASE, Program::opPopToBase);
        interpreterRegisterOpcode(OPCODE_PUSH_BASE, Program::opPushBase);
        interpreterRegisterOpcode(OPCODE_SET_GLOBAL, Program::opSetGlobal);
        interpreterRegisterOpcode(OPCODE_ADD, Program::opAdd);
        interpreterRegisterOpcode(OPCODE_SUB, Program::opSubtract);
        interpreterRegisterOpcode(OPCODE_MUL, Program::opMultiply);
        interpreterRegisterOpcode(OPCODE_DIV, Program::opDivide);
        interpreterRegisterOpcode(OPCODE_MOD, Program::opModulo);
        interpreterRegisterOpcode(OPCODE_NOT, Program::opLogicalOperatorNot);
        interpreterRegisterOpcode(OPCODE_NEGATE, Program::opUnaryMinus);
        interpreterRegisterOpcode(OPCODE_BITWISE_NOT, Program::opBitwiseOperatorNot);
        interpreterRegisterOpcode(OPCODE_FLOOR, Program::opFloor);
        interpreterRegisterOpcode(OPCODE_BITWISE_AND, Program::opBitwiseOperatorAnd);
        interpreterRegisterOpcode(OPCODE_BITWISE_OR, Program::opBitwiseOperatorOr);
        interpreterRegisterOpcode(OPCODE_BITWISE_XOR, Program::opBitwiseOperatorXor);
        interpreterRegisterOpcode(OPCODE_EQUAL, Program::opConditionalOperatorEqual);
        interpreterRegisterOpcode(OPCODE_NOT_EQUAL, Program::opConditionalOperatorNotEqual);
        interpreterRegisterOpcode(OPCODE_LESS_THAN, Program::opConditionalOperatorLessThan);
        interpreterRegisterOpcode(OPCODE_LESS_THAN_EQUAL, Program::opConditionalOperatorLessThanEquals);
        interpreterRegisterOpcode(OPCODE_GREATER_THAN, Program::opConditionalOperatorGreaterThan);
        interpreterRegisterOpcode(OPCODE_GREATER_THAN_EQUAL, Program::opConditionalOperatorGreaterThanEquals);
        interpreterRegisterOpcode(OPCODE_JUMP, Program::opJump);
        interpreterRegisterOpcode(OPCODE_IF, Program::opIf);
        interpreterRegisterOpcode(OPCODE_WHILE, Program::opWhile);
        interpreterRegisterOpcode(OPCODE_STORE, Program::opStore);
        interpreterRegisterOpcode(OPCODE_FETCH, Program::opFetch);
        interpreterRegisterOpcode(OPCODE_AND, Program::opLogicalOperatorAnd);
        interpreterRegisterOpcode(OPCODE_OR, Program::opLogicalOperatorOr);
        interpreterRegisterOpcode(OPCODE_CALL, Program::opCall);
        interpreterRegisterOpcode(OPCODE_POP_EXIT, Program::opPopExit);
        interpreterRegisterOpcode(OPCODE_POP_FLAGS, Program::opPopFlags);
        interpreterRegisterOpcode(OPCODE_POP_FLAGS_RETURN, Program::opPopFlagsReturn);
        interpreterRegisterOpcode(OPCODE_POP_FLAGS_EXIT, Program::opPopFlagsExit);
        interpreterRegisterOpcode(OPCODE_POP_ADDRESS, Program::opPopAddress);
        interpreterRegisterOpcode(OPCODE_POP_FLAGS_RETURN_VAL_EXIT, Program::opPopFlagsReturnValExit);
        interpreterRegisterOpcode(OPCODE_DETACH, Program::opDetach);
        interpreterRegisterOpcode(OPCODE_CHECK_PROCEDURE_ARGUMENT_COUNT, Program::opCheckProcedureArgumentCount);
        interpreterRegisterOpcode(OPCODE_LOOKUP_PROCEDURE_BY_NAME, Program::opLookupStringProc);
        interpreterRegisterOpcode(OPCODE_FETCH_PROCEDURE_ADDRESS, Program::opFetchProcedureAddress);
        interpreterRegisterOpcode(OPCODE_ENTER_CRITICAL_SECTION, Program::opEnterCriticalSection);
        interpreterRegisterOpcode(OPCODE_LEAVE_CRITICAL_SECTION, Program::opLeaveCriticalSection);
        // The extended set aliases the same two critical-section handlers.
        interpreterRegisterOpcode(OPCODE_START_CRITICAL, Program::opEnterCriticalSection);
        interpreterRegisterOpcode(OPCODE_END_CRITICAL, Program::opLeaveCriticalSection);
        interpreterRegisterOpcode(OPCODE_FETCH_GLOBAL, Program::opFetchGlobalVariable);
        interpreterRegisterOpcode(OPCODE_STORE_GLOBAL, Program::opStoreGlobalVariable);
        interpreterRegisterOpcode(OPCODE_DUMP, Program::opDump);
        interpreterRegisterOpcode(OPCODE_CALL_WHEN, Program::opConditionalCall);
        interpreterRegisterOpcode(OPCODE_CANCEL, Program::opCancel);
        interpreterRegisterOpcode(OPCODE_CANCEL_ALL, Program::opCancelAll);
        interpreterRegisterOpcode(OPCODE_CALLSTART, Program::opCallStart);
        interpreterRegisterOpcode(OPCODE_SPAWN, Program::opSpawn);
        interpreterRegisterOpcode(OPCODE_FORK, Program::opFork);
        interpreterRegisterOpcode(OPCODE_EXEC, Program::opExec);
        interpreterRegisterOpcode(OPCODE_EXPORT_VARIABLE, Program::opExportVariable);
        interpreterRegisterOpcode(OPCODE_EXPORT_PROCEDURE, Program::opExportProcedure);
        interpreterRegisterOpcode(OPCODE_FETCH_EXTERNAL, Program::opFetchExternalVariable);
        interpreterRegisterOpcode(OPCODE_STORE_EXTERNAL, Program::opStoreExternalVariable);
        interpreterRegisterOpcode(OPCODE_POP_FLAGS_RETURN_EXTERN, Program::opPopFlagsReturnExtern);
        interpreterRegisterOpcode(OPCODE_POP_FLAGS_EXIT_EXTERN, Program::opPopFlagsExitExtern);
        interpreterRegisterOpcode(OPCODE_POP_FLAGS_RETURN_VAL_EXTERN, Program::opPopFlagsReturnValExtern);
        interpreterRegisterOpcode(OPCODE_POP_FLAGS_RETURN_VAL_EXIT_EXTERN, Program::opPopFlagsReturnValExitExtern);
        interpreterRegisterOpcode(OPCODE_CALL_AT, Program::opDelayedCall);
        interpreterRegisterOpcode(OPCODE_WAIT, Program::opWait);
    }

    /** {@code gInterpreterCurrentProgram} (src/interpreter.cc). */
    private static Program interpreterCurrentProgram;

    /** {@code interpreterEnabled} (src/interpreter.cc). */
    private static boolean interpreterEnabled = true;

    /**
     * The script API the game's builtins are reached through.
     *
     * <p>Swapping it is what makes a script testable: a mock records what the
     * script asks for instead of needing the game to be running.
     */
    private static GameApi gameApi = new UnsupportedScriptApi();

    /** The script API the builtins are dispatched to. */
    public static GameApi gameApi() {
        return gameApi;
    }

    /**
     * Replaces the script API and re-registers the opcode table, so the new
     * API's builtins take the place of the old ones.
     */
    public static void setGameApi(GameApi api) {
        gameApi = api;
        interpreterRegisterOpcodeHandlers();
    }

    /** {@code interpreterBusy} (src/interpreter.cc). */
    private static boolean interpreterBusy;

    /** C {@code data + 42}: byte offset of the procedure table. */
    static final int PROCEDURES_BASE_OFFSET = 42;

    /** Serialized {@code Procedure}: 6 int32 fields (name/flags/time/condition/body/argc). */
    static final int PROCEDURE_SIZE = 24;

    /** Field offsets inside a serialized {@code Procedure}, i.e. {@code offsetof}. */
    static final int PROCEDURE_NAME_OFFSET = 0;
    static final int PROCEDURE_FLAGS_OFFSET = 4;
    static final int PROCEDURE_TIME_OFFSET = 8;
    static final int PROCEDURE_CONDITION_OFFSET = 12;
    static final int PROCEDURE_BODY_OFFSET = 16;
    static final int PROCEDURE_ARG_COUNT_OFFSET = 20;

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

        /** A value of a type of its own, with an integer of its own. */
        public ProgramValue(int opcode, int integerValue) {
            this.opcode = opcode;
            this.integerValue = integerValue;
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
            // Union alias: the raw bits are also the integerValue view.
            this.integerValue = Float.floatToRawIntBits(value);
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

        // ProgramValue::isEmpty. NOTE: the engine treats a zero string index
        // as empty too, which is wrong for static strings but is the behavior
        // scripts see (see the C comment).
        public boolean isEmpty() {
            switch (opcode) {
            case VALUE_TYPE_INT:
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                return integerValue == 0;
            case VALUE_TYPE_FLOAT:
                return floatValue == 0.0f;
            case VALUE_TYPE_PTR:
                return pointerValue == null;
            default:
                return true;
            }
        }

        public boolean isInt() {
            return opcode == VALUE_TYPE_INT;
        }

        public boolean isFloat() {
            return opcode == VALUE_TYPE_FLOAT;
        }

        public boolean isString() {
            return opcode == VALUE_TYPE_STRING || opcode == VALUE_TYPE_DYNAMIC_STRING;
        }

        public boolean isPointer() {
            return opcode == VALUE_TYPE_PTR;
        }

        public float asFloat() {
            switch (opcode) {
            case VALUE_TYPE_INT:
                return (float) integerValue;
            case VALUE_TYPE_FLOAT:
                return floatValue;
            default:
                return 0.0f;
            }
        }

        public int asInt() {
            switch (opcode) {
            case VALUE_TYPE_INT:
                return integerValue;
            case VALUE_TYPE_FLOAT:
                return (int) floatValue;
            default:
                return 0;
            }
        }

        // C prints a diagnostic and returns "" here; the port throws, since the
        // callers are error paths and a silent "" would hide the mismatch.
        public String asString(Program program) {
            if (!isString()) {
                throw new IllegalStateException(
                        "ProgramValue.asString: string expected, got " + Integer.toHexString(opcode));
            }
            return programGetString(program, opcode, integerValue);
        }

        public String typeDebugString() {
            switch (opcode) {
            case VALUE_TYPE_INT:
                return "INTEGER";
            case VALUE_TYPE_FLOAT:
                return "FLOAT";
            case VALUE_TYPE_STRING:
            case VALUE_TYPE_DYNAMIC_STRING:
                return "STRING";
            case VALUE_TYPE_PTR:
                return "POINTER";
            default:
                return "(UNKNOWN)";
            }
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

    /**
     * The object the script runs for ({@code Script::owner}).
     *
     * <p>C keeps this in the script registry, next to the program the script
     * belongs to; here the program carries it, because the program is what
     * the registry holds.
     */
    public ObjTypes.GameObject scriptOwner;

    /** The script's action ({@code Script::action}). */
    public int scriptAction;

    /** Port of {@code startTime} (init -1, never executed marker). */
    public int startTime = -1;

    /** Port of {@code flags} (dormant). */
    public int flags;

    /** Port of {@code windowId} (dormant). */
    public int windowId;

    /** Port of {@code checkWaitFunc} (null until the WAIT opcode sets it). */
    public InterpretCheckWaitFunc checkWaitFunc;

    /** Port of {@code exited} (init false). */
    public boolean exited;

    /** Port of {@code stackValues} (C {@code std::vector<ProgramValue>}: LIFO from the
     *  tail, with indexed access for STORE/FETCH). */
    public final List<ProgramValue> stackValues = new ArrayList<>();

    /** Port of {@code returnStackValues} (same shape, on the return stack). */
    public final List<ProgramValue> returnStackValues = new ArrayList<>();

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

    // programStackPushValue (overflow is a C fatal; same programFatalError
    // longjmp). C bumps the dynamic-string refcount on push (exact-opcode gate
    // on VALUE_TYPE_DYNAMIC_STRING); mirrored here so push/pop balance and the
    // heap accounting matches C on every path.
    public static void programStackPushValue(Program program, ProgramValue value) {
        if (program.stackValues.size() >= 0x1000) {
            programFatalError("programStackPushValue: Stack overflow.");
        }
        program.stackValues.add(new ProgramValue(value));
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

    // programStackPopValue (underflow is a C fatal; same programFatalError
    // longjmp). C drops the dynamic-string refcount on pop (same exact-opcode
    // gate).
    public static ProgramValue programStackPopValue(Program program) {
        if (program.stackValues.isEmpty()) {
            programFatalError("programStackPopValue: Stack underflow.");
        }
        ProgramValue value = program.stackValues.remove(program.stackValues.size() - 1);
        if (value.opcode == VALUE_TYPE_DYNAMIC_STRING) {
            interpreterStringRefCountDecrease(program, value.opcode, value.integerValue);
        }
        return value;
    }

    // Indexed access for STORE/FETCH: C's vector::at(framePointer + addr).
    static ProgramValue programStackAt(Program program, int index) {
        if (index < 0 || index >= program.stackValues.size()) {
            throw new IllegalStateException("programStackAt: index out of range");
        }
        return program.stackValues.get(index);
    }

    static void programStackSet(Program program, int index, ProgramValue value) {
        if (index < 0 || index >= program.stackValues.size()) {
            throw new IllegalStateException("programStackSet: index out of range");
        }
        program.stackValues.set(index, new ProgramValue(value));
    }

    // programStackPopInteger (wrong-type pop is a C fatal; same programFatalError
    // longjmp).
    public static int programStackPopInteger(Program program) {
        ProgramValue value = programStackPopValue(program);
        if (value.opcode != VALUE_TYPE_INT) {
            programFatalError("integer expected, got " + Integer.toHexString(value.opcode));
        }
        return value.integerValue;
    }

    // programStackPopString: mask gate admits both static (0x9001) and
    // dynamic (0x9801) strings since 0x9801 & 0xF7FF == 0x9001; resolves via
    // programGetString. Mismatch is a C fatal.
    public static String programStackPopString(Program program) {
        ProgramValue value = programStackPopValue(program);
        if ((value.opcode & VALUE_TYPE_MASK) != VALUE_TYPE_STRING) {
            programFatalError("string expected, got " + Integer.toHexString(value.opcode));
        }
        return programGetString(program, value.opcode, value.integerValue);
    }

    // programStackPopPointer: an INT 0 cell (uninitialized exported object
    // var) reads back as null instead of faulting; any other non-PTR cell
    // is a C fatal.
    public static Object programStackPopPointer(Program program) {
        ProgramValue value = programStackPopValue(program);
        if (value.opcode == VALUE_TYPE_INT && value.integerValue == 0) {
            return null;
        }
        if (value.opcode != VALUE_TYPE_PTR) {
            programFatalError("pointer expected, got " + Integer.toHexString(value.opcode));
        }
        return value.pointerValue;
    }

    // programReturnStackPushValue (same overflow/refcount discipline as the
    // value stack, but on returnStackValues).
    public static void programReturnStackPushValue(Program program, ProgramValue value) {
        if (program.returnStackValues.size() >= 0x1000) {
            programFatalError("programReturnStackPushValue: Stack overflow.");
        }
        program.returnStackValues.add(new ProgramValue(value));
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

    // programReturnStackPopValue (underflow is a C fatal; same programFatalError
    // longjmp).
    public static ProgramValue programReturnStackPopValue(Program program) {
        if (program.returnStackValues.isEmpty()) {
            programFatalError("programReturnStackPopValue: Stack underflow.");
        }
        ProgramValue value = program.returnStackValues.remove(program.returnStackValues.size() - 1);
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

    /**
     * The offset of a string in the program's own heap.
     *
     * <p>The external variable registry asks the script for this, because
     * a string it hands out belongs to the script it came from.
     */
    @Override
    public int heapString(String value) {
        return programPushString(this, value);
    }

    /** The string at an offset of the program's own heap. */
    @Override
    public String heapText(int opcode, int offset) {
        return programGetString(this, opcode, offset);
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
