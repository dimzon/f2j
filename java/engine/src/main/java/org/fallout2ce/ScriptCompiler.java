package org.fallout2ce;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
         * The instructions the analysis reached with two different stack
         * depths.
         *
         * <p>That is the original's own doing and not a problem: a builtin
         * that leaves a result behind is not always followed by a pop, so
         * the two paths into a join point can carry different stacks. The
         * VM does not care, and neither does the compiler: the stack of a
         * compiled script is a list, and the locals are addressed through
         * the frame pointer, which is known at the time.
         */
        final List<String> stackMismatches = new ArrayList<>();

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

        /** How many procedures the script has. */
        int procedureCount;

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
    private static final int OP_EXIT_PROGRAM = 0x010;
    private static final int OP_STOP_PROGRAM = 0x011;
    private static final int OP_POP_RETURN = 0x01C;
    private static final int OP_POP_EXIT = 0x01D;

    /**
     * The opcodes that come back through the return stack: the return
     * address comes off it, and the execution goes there.
     */
    private static final int[] OP_RETURNS = {
        0x01C, 0x01D, 0x020, 0x021, 0x022, 0x023, 0x024, 0x025, 0x026,
    };

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

        script.procedureCount = procedures.size();

        // The stack, and with it the branches.
        analyse(script, top);

        // The top level code is a launcher: it enters the critical section,
        // puts the exit address on the return stack and jumps into the
        // script's own code. That code is a block of its own, between the
        // tables and the procedure bodies, and it belongs to no procedure.
        int mainStart = -1;
        for (Instruction instruction : top.code) {
            if (instruction.branch && instruction.target > 0) {
                mainStart = instruction.target;
                break;
            }
        }
        if (mainStart > 0) {
            int mainEnd = data.length;
            for (Procedure procedure : procedures) {
                if (procedure.bodyOffset > mainStart && procedure.bodyOffset < mainEnd) {
                    mainEnd = procedure.bodyOffset;
                }
            }
            Procedure main = new Procedure();
            main.index = -2;
            main.bodyOffset = mainStart;
            main.bodyEnd = mainEnd;
            // The script own code comes before the top level code, which is
            // the last thing the VM runs.
            script.procedures.add(script.procedures.size() - 1, main);
            decode(script, main, data);

            script.analysisProblems.clear();
            analyse(script, top);
        }

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
     * Works out the stack depth of every instruction, resolves the branches
     * and finds the calls.
     *
     * <p>The instructions make a graph, not a line. A jump leaves the code
     * under it, a call leaves the code under it until the callee returns,
     * and a return comes back through the return stack. So the analysis
     * follows the graph: every instruction is reached with the state the
     * machine is in, and the state is the depth of the value stack, the two
     * values on top of it, the frame pointer and the return stack.
     *
     * <p>Only the top of the stack is tracked, because that is all the
     * analysis needs: a branch target, the procedure a call names and the
     * argument count are all on top. What is under them is not known, and
     * the analysis does not pretend it is.
     */
    private static void analyse(CompiledScript script, Procedure top) {
        // The instructions, so that a branch can be resolved to the
        // procedure and the position it lands on.
        Map<Integer, Procedure> owner = new HashMap<>();
        Map<Integer, Integer> position = new HashMap<>();
        for (Procedure procedure : script.procedures) {
            for (int index = 0; index < procedure.code.size(); index++) {
                owner.put(procedure.code.get(index).offset, procedure);
                position.put(procedure.code.get(index).offset, index);
            }
        }

        Deque<Work> work = new ArrayDeque<>();
        Map<String, State> seen = new HashMap<>();
        List<String> mismatches = new ArrayList<>();

        // The VM starts on the top level code, on an empty stack.
        work.add(new Work(top, 0, new State()));

        while (!work.isEmpty()) {
            Work item = work.poll();
            Procedure procedure = item.procedure;
            Instruction instruction = procedure.code.get(item.index);
            State state = item.state;

            // The stack is one: an instruction is reached with one state.
            String key = procedure.index + ":" + item.index;
            State first = seen.get(key);
            if (first != null) {
                if (first.depth() != state.depth()) {
                    // A procedure that calls itself is entered with a deeper
                    // stack every time, and that is right. The original's
                    // calls are a jump with the return address on the
                    // return stack, so the recursion is not always a call
                    // opcode, and the procedure's own entry is where it
                    // shows.
                    if (item.index == 0) {
                        procedure.recursive = true;
                    }
                    if (!procedure.recursive) {
                        mismatches.add(script.name + ": the stack does not match at "
                            + instruction.offset);
                    }
                }
                continue;
            }
            seen.put(key, state);

            instruction.depthBefore = state.depth();
            State next = state.copy();
            int slot = instruction.slot;

            switch (slot) {
            case OP_PUSH:
                next.push(instruction.immediate);
                instruction.depthAfter = next.depth();
                addFallthrough(work, procedure, item.index + 1, next);
                break;

            case OP_JUMP: {
                // The target is on top, and it is the only way on.
                Integer target = state.top();
                next.pop();
                instruction.depthAfter = next.depth();
                instruction.branch = true;
                if (target == null) {
                    script.analysisProblems.add(script.name + ": JUMP with an unknown target at "
                        + instruction.offset);
                } else {
                    instruction.target = target;
                    branch(script, work, owner, position, target, next);
                }
                break;
            }

            case OP_IF: {
                // The condition on top, the target under it. Both come off
                // the stack, and the target is where the execution goes when
                // the condition is empty.
                Integer target = state.second();
                next.pop();
                next.pop();
                instruction.depthAfter = next.depth();
                instruction.branch = true;
                if (target == null) {
                    script.analysisProblems.add(script.name + ": IF with an unknown target at "
                        + instruction.offset);
                } else {
                    instruction.target = target;
                    branch(script, work, owner, position, target, next);
                }
                addFallthrough(work, procedure, item.index + 1, next);
                break;
            }

            case OP_WHILE: {
                // The condition on top, the target under it. When the
                // condition is empty the target comes off the stack too and
                // the execution goes back; otherwise the target stays.
                Integer target = state.second();
                next.pop();
                instruction.depthAfter = next.depth();
                instruction.branch = true;
                if (target == null) {
                    script.analysisProblems.add(script.name + ": WHILE with an unknown target at "
                        + instruction.offset);
                } else {
                    instruction.target = target;
                    // The condition is empty: the target comes off the
                    // stack too and the execution goes back.
                    State back = next.copy();
                    back.pop();
                    branch(script, work, owner, position, target, back);
                }
                addFallthrough(work, procedure, item.index + 1, next);
                break;
            }

            case OP_POP_RETURN:
            case OP_POP_EXIT:
            case 0x020:
            case 0x021:
            case 0x022:
            case 0x023:
            case 0x024:
            case 0x025:
            case 0x026: {
                // The return address comes off the return stack, and the
                // execution goes there. The code under the return is not
                // reached.
                Integer address = next.returns == null ? null : next.returns.value;
                next.returnPop();
                int pops = ScriptOpcodeShapes.POP_COUNT[slot];
                for (int i = 0; i < pops; i++) {
                    next.pop();
                }
                if (ScriptOpcodeShapes.PUSH_KIND[slot] != -1) {
                    next.push(null);
                }
                instruction.depthAfter = next.depth();
                if (address == null) {
                    script.analysisProblems.add(script.name
                        + ": a return with an unknown address at " + instruction.offset);
                } else {
                    branch(script, work, owner, position, address, next);
                }
                break;
            }

            case OP_EXIT_PROGRAM:
            case OP_STOP_PROGRAM: {
                // The program ends here. The code under it is not reached,
                // and the original's exit sequence is exactly that: code
                // that never runs.
                instruction.depthAfter = next.depth();
                break;
            }

            case OP_CALL: {
                // The procedure is named on top. The code under the call is
                // not reached until the callee returns.
                Integer index = state.top();
                next.pop();
                instruction.depthAfter = next.depth();
                if (index == null) {
                    script.analysisProblems.add(script.name + ": CALL with an unknown index at "
                        + instruction.offset);
                    break;
                }
                instruction.callTarget = index;
                if (index < 0 || index >= script.procedureCount) {
                    script.analysisProblems.add(script.name + ": the call at " + instruction.offset
                        + " names no procedure of the script");
                    break;
                }
                Procedure callee = script.procedures.get(index);
                if (callee.entryDepth == -1) {
                    callee.entryDepth = next.depth();
                } else if (callee.entryDepth != next.depth()) {
                    callee.recursive = true;
                }
                work.add(new Work(callee, 0, next));
                break;
            }

            case OP_D_TO_A: {
                // A value off the value stack onto the return stack. That is
                // where the return address goes.
                next.returnPush(state.top());
                next.pop();
                instruction.depthAfter = next.depth();
                addFallthrough(work, procedure, item.index + 1, next);
                break;
            }

            case OP_A_TO_D: {
                // A value off the return stack onto the value stack.
                next.push(next.returns == null ? null : next.returns.value);
                next.returnPop();
                instruction.depthAfter = next.depth();
                addFallthrough(work, procedure, item.index + 1, next);
                break;
            }

            case OP_PUSH_BASE: {
                // The argument count on top; the frame pointer becomes the
                // stack size less the argument count, and the old frame
                // pointer goes onto the return stack.
                Integer argumentCount = state.top();
                next.pop();
                next.returnPush(null);
                next.framePointer = argumentCount != null ? next.depth() - argumentCount : -1;
                instruction.depthAfter = next.depth();
                addFallthrough(work, procedure, item.index + 1, next);
                break;
            }

            case OP_POP_BASE: {
                // The old frame pointer comes back off the return stack.
                next.returnPop();
                instruction.depthAfter = next.depth();
                addFallthrough(work, procedure, item.index + 1, next);
                break;
            }

            case OP_POP_TO_BASE: {
                // The stack falls to the frame pointer.
                if (state.framePointer < 0) {
                    script.analysisProblems.add(script.name + ": POP_TO_BASE without a frame at "
                        + instruction.offset);
                    break;
                }
                // The stack falls to the frame pointer, and what was on it
                // is gone.
                next.values = trimTo(next.values, state.framePointer);
                instruction.depthAfter = next.depth();
                addFallthrough(work, procedure, item.index + 1, next);
                break;
            }

            default: {
                // The shape of the opcode says what it reads and what it
                // leaves behind.
                int pops = ScriptOpcodeShapes.POP_COUNT[slot];
                for (int i = 0; i < pops; i++) {
                    next.pop();
                }
                if (ScriptOpcodeShapes.PUSH_KIND[slot] != -1) {
                    next.push(null);
                }
                instruction.depthAfter = next.depth();
                addFallthrough(work, procedure, item.index + 1, next);
                break;
            }
            }
        }

        // A procedure that is entered with different stacks is recursive,
        // and that is not a problem: the stack grows with the recursion.
        script.stackMismatches.addAll(mismatches);
    }

    /** The stack, cut down to the given depth. */
    private static Value trimTo(Value values, int depth) {
        Value result = values;
        while (result != null && result.depth > depth) {
            result = result.tail;
        }
        return result;
    }

    /** The execution carries on at the next instruction of the procedure. */
    private static void addFallthrough(Deque<Work> work, Procedure procedure, int index, State state) {
        if (index >= 0 && index < procedure.code.size()) {
            work.add(new Work(procedure, index, state));
        }
    }

    /** A branch: the execution continues at the target. */
    private static void branch(CompiledScript script, Deque<Work> work,
            Map<Integer, Procedure> owner, Map<Integer, Integer> position,
            int target, State state) {
        Procedure procedure = owner.get(target);
        Integer index = position.get(target);
        if (procedure == null || index == null) {
            script.analysisProblems.add(script.name + ": the branch to " + target
                + " lands on no instruction");
            return;
        }
        work.add(new Work(procedure, index, state));
    }

    /**
     * One value of a stack, with the rest of the stack under it.
     *
     * <p>The stacks are shared: a push makes a new head over the old one,
     * so the paths through the program share what they have in common.
     */
    private static final class Value {
        final Integer value;
        final Value tail;
        final int depth;

        Value(Integer value, Value tail) {
            this.value = value;
            this.tail = tail;
            this.depth = tail == null ? 1 : tail.depth + 1;
        }
    }

    /**
     * The state of the machine at one instruction.
     *
     * <p>The value stack holds the values the analysis knows: a branch
     * target, the procedure a call names, the argument count. What is
     * pushed by anything but a PUSH is not known, and the analysis does not
     * pretend it is - the value is there, but it is null.
     */
    private static final class State {
        Value values;
        Value returns;
        int framePointer = -1;

        State copy() {
            State other = new State();
            other.values = values;
            other.returns = returns;
            other.framePointer = framePointer;
            return other;
        }

        /** The depth of the value stack. */
        int depth() {
            return values == null ? 0 : values.depth;
        }

        /** The value on top of the value stack. */
        Integer top() {
            return values == null ? null : values.value;
        }

        /** The value under the top of the value stack. */
        Integer second() {
            return values == null || values.tail == null ? null : values.tail.value;
        }

        /** A value goes onto the value stack. */
        void push(Integer value) {
            values = new Value(value, values);
        }

        /** A value comes off the value stack. */
        void pop() {
            values = values == null ? null : values.tail;
        }

        /** A value goes onto the return stack. */
        void returnPush(Integer value) {
            returns = new Value(value, returns);
        }

        /** A value comes off the return stack. */
        void returnPop() {
            returns = returns == null ? null : returns.tail;
        }
    }

    /** An instruction to analyse, with the state it is reached with. */
    private static final class Work {
        final Procedure procedure;
        final int index;
        final State state;

        Work(Procedure procedure, int index, State state) {
            this.procedure = procedure;
            this.index = index;
            this.state = state;
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
