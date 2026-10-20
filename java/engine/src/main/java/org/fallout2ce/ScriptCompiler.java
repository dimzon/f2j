package org.fallout2ce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The front end of the script compiler: it turns a {@code .int} script into
 * something that can be turned into JVM code.
 *
 * <p>It does three things.
 *
 * <p>It decodes the code. Every opcode is two bytes, and {@code PUSH} carries
 * a four byte immediate. A procedure's code is the run of instructions from
 * its body offset to the next body's offset.
 *
 * <p>It works out the stack. The VM has one value stack that every procedure
 * shares, and a procedure's locals live inside it at an offset from the frame
 * pointer. The depth of that stack is not obvious from the code, but it can
 * be worked out: the game's compiler emits straight-line code, so a pass that
 * follows the pops and the pushes gives the depth at every instruction. The
 * frame pointer is a constant too - it is the stack size at the moment
 * {@code PUSH_BASE} runs, less the argument count - which is what makes
 * {@code POP_TO_BASE}, whose depth depends on the frame pointer, computable as
 * well.
 *
 * <p>It resolves the branches. The VM's branches are not relative: the
 * target is a value on the stack, and the compiler pushed it as an
 * immediate. So the pass that follows the stack also knows the target, and a
 * branch whose target is not an instruction of the same procedure is
 * something the compiler reports rather than guesses at.
 *
 * <p>What the compiler produces is the shape of the program, not yet code.
 * The emitter turns it into JVM code, and the game API is reached through
 * {@link GameApi} so that any implementation can be supplied - the game, a
 * mock in a test, or a decorator that logs what a script asks for.
 */
final class ScriptCompiler {

    /** One decoded instruction. */
    static final class Instruction {
        /** The file offset of the instruction. */
        int offset;

        /** The raw 16 bit opcode. */
        int opcode;

        /** The dispatch slot, {@code opcode & 0x3FF}. */
        int slot;

        /** The immediate of a {@code PUSH}. */
        int immediate;

        /** Whether the instruction carries an immediate. */
        boolean hasImmediate;

        /** The stack depth before the instruction. */
        int depthBefore;

        /** The stack depth after the instruction. */
        int depthAfter;

        /** The resolved branch target, or -1. */
        int target = -1;

        /** Whether the instruction is a branch. */
        boolean branch;

        /** The procedure a call enters, or -1. */
        int callTarget = -1;
    }

    /** One procedure of the script. */
    static final class Procedure {
        int index;
        int nameOffset;
        String name;
        int argCount;
        int bodyOffset;
        int bodyEnd;
        final List<Instruction> code = new ArrayList<>();

        /** The stack depth when the procedure is entered, or -1 if unknown. */
        int entryDepth = -1;

        /** Whether the procedure can be entered at more than one depth. */
        boolean recursive;
    }

    /** The compiled shape of a whole script. */
    static final class CompiledScript {
        String name;
        final List<Procedure> procedures = new ArrayList<>();

        /** What the decoder could not make sense of. */
        final List<String> problems = new ArrayList<>();

        /**
         * What the analysis could not make sense of.
         *
         * <p>The analysis follows the stack and the branches, and it is not
         * finished: the top level code is a launcher that jumps into the
         * script's own code, the code after a call is only reached through
         * the return stack, and the code after a jump is not reached at all.
         * The analysis reports what it cannot follow instead of guessing.
         */
        final List<String> analysisProblems = new ArrayList<>();

        /** The API methods the script calls, in the order it calls them. */
        final List<String> apiCalls = new ArrayList<>();

        boolean ok() {
            return problems.isEmpty();
        }
    }

    private static final int OP_PUSH = 0x001;
    private static final int OP_JUMP = 0x004;
    private static final int OP_CALL = 0x005;
    private static final int OP_A_TO_D = 0x00C;
    private static final int OP_D_TO_A = 0x00D;
    private static final int OP_POP_BASE = 0x029;
    private static final int OP_POP_TO_BASE = 0x02A;
    private static final int OP_PUSH_BASE = 0x02B;
    private static final int OP_IF = 0x02F;
    private static final int OP_WHILE = 0x030;

    /** The value stack's slots, each a known constant or null. */
    private static final int MAX_STACK = 1024;

    private ScriptCompiler() {
    }

    /**
     * Compiles the shape of a script.
     *
     * @param name the script's name, for the problems it reports
     * @param data the script's bytes
     */
    static CompiledScript compile(String name, byte[] data) {
        CompiledScript script = new CompiledScript();
        script.name = name;

        int procedureCount = Program.stackReadInt32(data, 42);
        int identifiersPos = 24 * procedureCount + 42 + 4;
        int staticStringsPos = identifiersPos + Program.stackReadInt32(data, identifiersPos) + 4 + 4;
        int staticStringLen = Program.stackReadInt32(data, staticStringsPos);
        if (staticStringLen == -1) {
            staticStringLen = -4;
        }
        int codePos = staticStringsPos + 4 + staticStringLen + 4;

        List<Procedure> procedures = new ArrayList<>();
        for (int index = 0; index < procedureCount; index++) {
            int row = 46 + 24 * index;
            Procedure procedure = new Procedure();
            procedure.index = index;
            procedure.nameOffset = Program.stackReadInt32(data, row);
            procedure.argCount = Program.stackReadInt32(data, row + 20);
            procedure.bodyOffset = Program.stackReadInt32(data, row + 16);
            procedures.add(procedure);
        }

        // A body's extent runs to the next body.
        List<Integer> bodyOffsets = new ArrayList<>();
        for (Procedure procedure : procedures) {
            bodyOffsets.add(procedure.bodyOffset);
        }
        bodyOffsets.sort(Integer::compareTo);
        for (Procedure procedure : procedures) {
            int end = data.length;
            for (Integer offset : bodyOffsets) {
                if (offset > procedure.bodyOffset && offset < end) {
                    end = offset;
                }
            }
            // A procedure with no body of its own has a body offset of zero,
            // and the original enters the top level code when it is called.
            procedure.bodyEnd = procedure.bodyOffset < Program.PROCEDURES_BASE_OFFSET
                ? Program.PROCEDURES_BASE_OFFSET
                : end;
        }

        for (Procedure procedure : procedures) {
            script.procedures.add(procedure);
            decode(script, procedure, data);
        }

        // The top level code, which is what the VM starts on. It runs from the
        // start of the file to the procedure table: the C reads the opcode at
        // the instruction pointer, which starts at zero, and the procedure
        // table is what follows the top level code.
        Procedure top = new Procedure();
        top.index = -1;
        top.bodyOffset = 0;
        top.bodyEnd = Program.PROCEDURES_BASE_OFFSET;
        script.procedures.add(top);
        decode(script, top, data);

        // The stack, and with it the branches.
        analyse(script);

        return script;
    }

    /** Decodes one procedure's code. */
    private static void decode(CompiledScript script, Procedure procedure, byte[] data) {
        int pos = procedure.bodyOffset;
        while (pos + 2 <= procedure.bodyEnd) {
            int opcode = Program.stackReadInt16(data, pos);
            if (((opcode >> 8) & 0x80) == 0) {
                script.problems.add(script.name + ": bad opcode " + Integer.toHexString(opcode)
                    + " at " + pos);
                return;
            }

            Instruction instruction = new Instruction();
            instruction.offset = pos;
            instruction.opcode = opcode;
            instruction.slot = opcode & 0x3FF;

            if (instruction.slot == OP_PUSH && pos + 6 <= procedure.bodyEnd) {
                instruction.hasImmediate = true;
                instruction.immediate = Program.stackReadInt32(data, pos + 2);
                pos += 6;
            } else {
                pos += 2;
            }

            procedure.code.add(instruction);
        }
    }

    /**
     * Works out the stack depth of every instruction, resolves the branches and
     * finds the calls.
     */
    private static void analyse(CompiledScript script) {
        Map<Integer, Integer> depthAt = new HashMap<>();
        Map<Integer, Integer> callSite = new HashMap<>();

        // The top level code runs on an empty stack.
        Procedure top = script.procedures.get(script.procedures.size() - 1);
        simulate(script, top, 0, depthAt, callSite);

        // A procedure's entry depth is the depth at the call that enters it.
        boolean progress = true;
        while (progress) {
            progress = false;
            for (Map.Entry<Integer, Integer> entry : callSite.entrySet()) {
                Procedure procedure = script.procedures.get(entry.getKey());
                if (procedure.entryDepth == -1) {
                    procedure.entryDepth = entry.getValue();
                    progress = true;
                } else if (procedure.entryDepth != entry.getValue()) {
                    procedure.recursive = true;
                }
            }
        }

        for (Procedure procedure : script.procedures) {
            if (procedure.entryDepth == -1) {
                continue;
            }
            simulate(script, procedure, procedure.entryDepth, depthAt, callSite);
        }

        // The branches have to land on an instruction, and the calls have to
        // name a procedure of the script.
        java.util.Set<Integer> offsets = new java.util.HashSet<>();
        for (Procedure procedure : script.procedures) {
            for (Instruction instruction : procedure.code) {
                offsets.add(instruction.offset);
            }
        }
        int procedureCount = script.procedures.size() - 1;
        for (Procedure procedure : script.procedures) {
            for (Instruction instruction : procedure.code) {
                if (instruction.branch && !offsets.contains(instruction.target)) {
                    script.analysisProblems.add(script.name + ": the branch at " + instruction.offset
                        + " does not land on an instruction");
                }
                if (instruction.callTarget >= procedureCount) {
                    script.analysisProblems.add(script.name + ": the call at " + instruction.offset
                        + " names no procedure of the script");
                }
            }
        }

        // The API calls, in the order the script makes them.
        for (Procedure procedure : script.procedures) {
            for (Instruction instruction : procedure.code) {
                String method = apiMethodFor(instruction.slot);
                if (method != null && !script.apiCalls.contains(method)) {
                    script.apiCalls.add(method);
                }
            }
        }
    }

    /**
     * One pass over a procedure's code, following the stack.
     *
     * @param depthAt the depth of every instruction seen so far
     * @param callSite the depth at which each procedure is called
     */
    private static void simulate(CompiledScript script, Procedure procedure, int entryDepth,
            Map<Integer, Integer> depthAt, Map<Integer, Integer> callSite) {
        Integer[] stack = new Integer[MAX_STACK];
        int depth = entryDepth;
        int framePointer = -1;
        int returnDepth = 0;

        for (Instruction instruction : procedure.code) {
            int slot = instruction.slot;
            instruction.depthBefore = depth;

            Integer known = null;
            if (depth > 0) {
                known = stack[depth - 1];
            }

            switch (slot) {
            case OP_PUSH:
                push(stack, depth, instruction.immediate);
                depth++;
                break;

            case OP_D_TO_A:
                // A value off the value stack onto the return stack.
                pop(stack, depth);
                depth--;
                returnDepth++;
                break;

            case OP_A_TO_D:
                // A value off the return stack onto the value stack.
                pop(stack, depth);
                returnDepth--;
                depth++;
                push(stack, depth, null);
                break;

            case OP_PUSH_BASE: {
                // The argument count, then the frame pointer becomes the stack
                // size less the argument count.
                Integer argCount = depth > 0 ? stack[depth - 1] : null;
                pop(stack, depth);
                depth--;
                returnDepth++;
                framePointer = argCount != null ? (depth - argCount) : -1;
                break;
            }

            case OP_POP_BASE:
                returnDepth--;
                break;

            case OP_POP_TO_BASE:
                // The depth falls to the frame pointer.
                if (framePointer < 0) {
                    script.analysisProblems.add(script.name + ": POP_TO_BASE without a frame at "
                        + instruction.offset);
                    return;
                }
                depth = framePointer;
                break;

            case OP_IF: {
                // The condition, and under it the target.
                Integer condition = depth > 0 ? stack[depth - 1] : null;
                Integer target = depth > 1 ? stack[depth - 2] : null;
                pop(stack, depth);
                depth--;
                pop(stack, depth);
                depth--;
                instruction.branch = true;
                if (target != null) {
                    instruction.target = target;
                } else {
                    script.analysisProblems.add(script.name + ": IF with an unknown target at "
                        + instruction.offset);
                }
                break;
            }

            case OP_WHILE: {
                // The condition, and under it the target. The target stays on
                // the stack while the body runs.
                Integer condition = depth > 0 ? stack[depth - 1] : null;
                Integer target = depth > 1 ? stack[depth - 2] : null;
                pop(stack, depth);
                depth--;
                instruction.branch = true;
                if (target != null) {
                    instruction.target = target;
                } else {
                    script.analysisProblems.add(script.name + ": WHILE with an unknown target at "
                        + instruction.offset);
                }
                // The body leaves the target on the stack, the exit takes it.
                break;
            }

            case OP_JUMP: {
                Integer target = depth > 0 ? stack[depth - 1] : null;
                pop(stack, depth);
                depth--;
                instruction.branch = true;
                if (target != null) {
                    instruction.target = target;
                } else {
                    script.analysisProblems.add(script.name + ": JUMP with an unknown target at "
                        + instruction.offset);
                }
                break;
            }

            case OP_CALL: {
                Integer index = depth > 0 ? stack[depth - 1] : null;
                pop(stack, depth);
                depth--;
                if (index != null) {
                    instruction.callTarget = index;
                    callSite.putIfAbsent(index, depth);
                } else {
                    script.analysisProblems.add(script.name + ": CALL with an unknown index at "
                        + instruction.offset);
                }
                break;
            }

            default: {
                int pops = ScriptOpcodeShapes.POP_COUNT[slot];
                for (int i = 0; i < pops; i++) {
                    pop(stack, depth);
                    depth--;
                }
                if (ScriptOpcodeShapes.PUSH_KIND[slot] != -1) {
                    push(stack, depth, null);
                    depth++;
                }
                break;
            }
            }

            instruction.depthAfter = depth;
            if (depth < 0) {
                script.analysisProblems.add(script.name + ": the value stack underflows at "
                    + instruction.offset);
                return;
            }
            if (depth > MAX_STACK) {
                script.analysisProblems.add(script.name + ": the value stack overflows at "
                    + instruction.offset);
                return;
            }
            depthAt.put(instruction.offset, depth);
        }
    }

    private static void push(Integer[] stack, int depth, Integer value) {
        if (depth >= 0 && depth < stack.length) {
            stack[depth] = value;
        }
    }

    private static void pop(Integer[] stack, int depth) {
        if (depth >= 0 && depth < stack.length) {
            stack[depth] = null;
        }
    }

    /**
     * The API method a slot calls, or null when the slot is the VM's own.
     */
    static String apiMethodFor(int slot) {
        if (slot <= 0x04B) {
            return null;
        }
        return ScriptOpcodes.HANDLER[slot];
    }
}
