package org.fallout2ce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fallout2ce.ScriptCompiler.CompiledScript;
import org.fallout2ce.ScriptCompiler.Instruction;
import org.fallout2ce.ScriptCompiler.Procedure;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The emitter: it turns the shape of a script into a class file.
 *
 * <p>What it produces is a subclass of {@link ScriptBase}: one method per
 * procedure of the script, plus the script's own code and the launcher that
 * the VM starts on. The methods share the value stack of the base class, so
 * a procedure's locals are where the frame pointer says they are.
 *
 * <p>The control flow is resolved, not dispatched. A branch becomes a jump,
 * a call becomes a call, and the game's builtins become calls on the game
 * API - the interface the compiled code is written against, so the game, a
 * mock or a decorator that logs what the script asks for can be supplied.
 *
 * <p>The class file is written with ASM. The bytes are what the JVM loads;
 * nothing is compiled at run time and no source is kept.
 *
 * <p>The generated class lives in a package of its own, and the class loader
 * that defines it is not the one that holds the base class, so the two are
 * in different runtime packages. A protected member of the base class can
 * still be reached, but only through a reference that names the subclass -
 * which is what a compiler does as well.
 */
final class ScriptEmitter {

    /** The name of the base class, as the class file spells it. */
    private static final String BASE = "org/fallout2ce/ScriptBase";

    /** The name of the game API, as the class file spells it. */
    private static final String API = "org/fallout2ce/GameApi";

    /** The value the stack holds, as the class file spells it. */
    private static final String VALUE = "org/fallout2ce/Program$ProgramValue";

    /** The descriptor of the value the stack holds. */
    private static final String VALUE_DESC = "L" + VALUE + ";";

    /** The descriptor of a string. */
    private static final String STRING_DESC = "Ljava/lang/String;";

    /** The descriptor of a pointer. */
    private static final String OBJECT_DESC = "Ljava/lang/Object;";

    /** The code carries on with the next instruction. */
    private static final int CONTINUE = 0;

    /** The method has ended. */
    private static final int ENDED = 1;

    /** The control has gone somewhere else. */
    private static final int JUMPED = 2;

    /** The generated class of one script. */
    static final class Emitted {
        /** The internal name of the class. */
        String className;

        /** The class file. */
        byte[] bytes;

        /** The opcodes the emitter could not turn into code. */
        final List<String> problems = new ArrayList<>();
    }

    private ScriptEmitter() {
    }

    /** Emits the class of a script. */
    static Emitted emit(CompiledScript script) {
        Emitted emitted = new Emitted();
        emitted.className = className(script);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                if (type1.equals(type2)) {
                    return type1;
                }
                if (isSubclassOf(type1, type2)) {
                    return type2;
                }
                if (isSubclassOf(type2, type1)) {
                    return type1;
                }
                return "java/lang/Object";
            }
        };
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
            emitted.className, null, BASE, null);

        // The constructor: the game API the script talks to, its name and
        // its static strings.
        MethodVisitor constructor = writer.visitMethod(
            Opcodes.ACC_PUBLIC, "<init>",
            "(L" + API + ";Ljava/lang/String;Ljava/lang/String;)V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitVarInsn(Opcodes.ALOAD, 1);
        constructor.visitLdcInsn(script.name);
        constructor.visitLdcInsn(script.staticStrings);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, BASE, "<init>",
            "(L" + API + ";Ljava/lang/String;Ljava/lang/String;)V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        // The procedures, the script's own code and the launcher.
        for (Procedure procedure : script.procedures) {
            emitProcedure(script, procedure, writer, emitted);
        }
        // The call of a procedure by index: the original names the procedure
        // on the stack, so the code looks the name up.
        emitCallProcedure(script, writer);

        writer.visitEnd();
        emitted.bytes = writer.toByteArray();
        return emitted;
    }

    /**
     * Whether a type is a subclass of another.
     *
     * <p>Only the classes the generated code knows about are in here: the
     * class loader that holds the generated class is not the one that holds
     * the base class, so the common superclass cannot be worked out by
     * loading the classes.
     */
    private static boolean isSubclassOf(String type, String supertype) {
        if (supertype.equals("java/lang/Object")) {
            return true;
        }
        if (type.equals(BASE)) {
            return supertype.equals(BASE);
        }
        if (type.startsWith("org/fallout2ce/generated/")) {
            return supertype.equals(BASE);
        }
        return false;
    }

    /** The internal name of the generated class. */
    private static String className(CompiledScript script) {
        StringBuilder out = new StringBuilder("org/fallout2ce/generated/CompiledScript_");
        for (int i = 0; i < script.name.length(); i++) {
            char c = script.name.charAt(i);
            out.append(Character.isLetterOrDigit(c) ? c : '_');
        }
        return out.append('_').append(Integer.toHexString(script.name.hashCode())).toString();
    }

    /** The name of the method a procedure gets. */
    private static String methodName(Procedure procedure) {
        if (procedure.index == -1) {
            return "run";
        }
        if (procedure.index == -2) {
            return "code";
        }
        return "proc" + procedure.index;
    }

    /**
     * One block of a procedure.
     *
     * <p>A block ends where the execution can leave it: at a branch, at a
     * return or at a call. The code that the execution never comes to is
     * left out: the original keeps it - the exit sequence under an
     * EXIT_PROGRAM, for one - and the compiled code has no use for it.
     */
    private static final class Block {
        /** The instructions of the block. */
        final List<Instruction> code = new ArrayList<>();

        /** Where the block starts. */
        final Label label = new Label();
    }

    /** Emits one procedure. */
    private static void emitProcedure(CompiledScript script, Procedure procedure,
            ClassWriter writer, Emitted emitted) {
        // The launcher is the way into the script, and the VM stops a
        // program that fails, so the launcher catches the failure and
        // leaves quietly. The code of the script itself goes into a method
        // of its own, so that the catch has something to hold.
        boolean launcher = procedure.index == -1;
        int access = launcher ? Opcodes.ACC_PRIVATE : Opcodes.ACC_PROTECTED;
        String name = launcher ? "runBody" : methodName(procedure);
        MethodVisitor method = writer.visitMethod(access, name, "()V", null, null);
        method.visitCode();
        Code code = new Code(method, emitted.className);

        List<Block> blocks = blocks(procedure);
        Map<Integer, Block> byOffset = new HashMap<>();
        for (Block block : blocks) {
            byOffset.put(block.code.get(0).offset, block);
        }

        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            method.visitLabel(block.label);

            int state = CONTINUE;
            for (Instruction instruction : block.code) {
                state = emitInstruction(script, procedure, instruction, code, byOffset, emitted);
                if (state != CONTINUE) {
                    break;
                }
            }

            if (state == CONTINUE) {
                // The block falls through to the next one.
                if (i + 1 < blocks.size()) {
                    method.visitJumpInsn(Opcodes.GOTO, blocks.get(i + 1).label);
                } else {
                    method.visitInsn(Opcodes.RETURN);
                }
            }
        }

        if (blocks.isEmpty()) {
            // The execution never comes to the procedure, so it has no code
            // of its own.
            method.visitInsn(Opcodes.RETURN);
        }

        try {
            method.visitMaxs(0, 0);
        } catch (RuntimeException failure) {
            throw new IllegalStateException(script.name + ": the procedure "
                + methodName(procedure) + " does not fit: " + failure, failure);
        }
        method.visitEnd();

        if (launcher) {
            emitLauncher(writer, emitted);
        }
    }

    /**
     * The way into the script.
     *
     * <p>The VM stops a program that fails, and it does it quietly: the
     * error leaves the interpreter and the program ends. The launcher
     * catches the failure of the script and leaves the same way.
     */
    private static void emitLauncher(ClassWriter writer, Emitted emitted) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        method.visitCode();
        Label start = new Label();
        Label end = new Label();
        Label failure = new Label();
        method.visitTryCatchBlock(start, end, failure, "org/fallout2ce/ScriptBase$FatalError");
        method.visitLabel(start);
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, emitted.className, "runBody", "()V", false);
        method.visitLabel(end);
        method.visitInsn(Opcodes.RETURN);
        method.visitLabel(failure);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    /**
     * The blocks of a procedure.
     *
     * <p>The branch targets start a block: the execution can land there from
     * anywhere. A block also ends where the execution leaves it, so that
     * every block ends with a jump and the code that follows one is only
     * reached through it.
     */
    private static List<Block> blocks(Procedure procedure) {
        List<Block> blocks = new ArrayList<>();

        // The branch targets: the execution can land on them from anywhere.
        List<Integer> targets = new ArrayList<>();
        for (Instruction instruction : procedure.code) {
            if (instruction.reached && instruction.branch && instruction.target > 0) {
                targets.add(instruction.target);
            }
        }

        Block current = null;
        for (Instruction instruction : procedure.code) {
            if (!instruction.reached) {
                continue;
            }
            if (current == null || endsBlock(instruction) || targets.contains(instruction.offset)) {
                current = new Block();
                blocks.add(current);
            }
            current.code.add(instruction);
        }
        return blocks;
    }

    /**
     * Whether the execution can leave the code after an instruction.
     *
     * <p>A branch leaves it: the path the condition does not take carries
     * on. A call leaves it: the code under the call belongs to the caller
     * and comes back when the callee returns. A return leaves it for good.
     * A jump into another procedure does not: the call comes back.
     */
    private static boolean endsBlock(Instruction instruction) {
        int slot = instruction.slot;
        return instruction.branch || slot == 0x005 || slot == 0x00E
            || slot == 0x010 || slot == 0x011
            || (slot >= 0x01C && slot <= 0x026);
    }

    /**
     * Emits one instruction.
     *
     * @return whether the method ends with it, or whether the control has
     *         gone somewhere else
     */
    private static int emitInstruction(CompiledScript script, Procedure procedure,
            Instruction instruction, Code code, Map<Integer, Block> byOffset, Emitted emitted) {
        int slot = instruction.slot;

        switch (slot) {
        case 0x000:
            return CONTINUE;
        case 0x001:
            // A constant. The type of the value is in the opcode itself:
            // the VM reads the type out of the opcode it dispatched on.
            code.loadThis();
            switch (instruction.opcode & 0xFFFF) {
            case Program.VALUE_TYPE_FLOAT:
                code.constant(Float.intBitsToFloat(instruction.immediate));
                code.callOnStack("pushFloat", "(F)V");
                break;
            case Program.VALUE_TYPE_STRING:
                code.constant(instruction.immediate);
                code.callOnStack("pushStaticString", "(I)V");
                break;
            default:
                code.constant(instruction.immediate);
                code.callOnStack("pushInt", "(I)V");
                break;
            }
            return CONTINUE;
        case 0x002:
        case 0x04A:
            code.call("enterCritical", "()V");
            return CONTINUE;
        case 0x003:
        case 0x04B:
            code.call("leaveCritical", "()V");
            return CONTINUE;
        case 0x004:
            return emitJump(script, procedure, instruction, code, byOffset, emitted);
        case 0x005: {
            // The procedure is named on the stack, so the call is a switch
            // the generated class has.
            int index = code.callInt("popInt");
            code.loadThis();
            code.loadInt(index);
            code.callOnStack("callProcedure", "(I)V");
            return CONTINUE;
        }
        case 0x00C:
            code.call("addressToData", "()V");
            return CONTINUE;
        case 0x00D:
            code.call("dataToAddress", "()V");
            return CONTINUE;
        case 0x00E:
            code.call("exitScript", "()V");
            code.returning();
            return ENDED;
        case 0x00F:
            code.call("detach", "()V");
            return CONTINUE;
        case 0x010:
            code.call("exitProgram", "()V");
            code.returning();
            return ENDED;
        case 0x011:
            code.call("stopProgram", "()V");
            code.returning();
            return ENDED;
        case 0x012: {
            // One of the script's globals.
            int index = code.callInt("popInt");
            int value = code.callValue("fetchGlobal", index);
            code.pushValue(value);
            return CONTINUE;
        }
        case 0x013: {
            // The address comes off the stack first, then the value.
            int index = code.callInt("popInt");
            int value = code.callValue("popValue");
            code.loadThis();
            code.loadInt(index);
            code.load(value);
            code.callOnStack("storeGlobal", "(I" + VALUE_DESC + ")V");
            return CONTINUE;
        }
        case 0x014:
        case 0x015:
        case 0x016: {
            // An external variable: the name is on the stack, and the
            // original reads it out of the script's identifiers. The
            // compiled code knows the name when it is written.
            String identifier = externalName(script, instruction);
            if (identifier == null) {
                emitted.problems.add(script.name + ": the external variable at "
                    + instruction.offset + " has no name");
                return CONTINUE;
            }
            if (slot == 0x014) {
                // The identifier comes off the stack, and the value goes
                // on it.
                code.discardInt("popInt");
                code.loadThis();
                code.constant(identifier);
                code.callOnStack("fetchExternal", "(Ljava/lang/String;)V");
                return CONTINUE;
            }
            if (slot == 0x015) {
                // The identifier comes off the stack first, then the value.
                code.discardInt("popInt");
                int value = code.callValue("popValue");
                code.loadThis();
                code.constant(identifier);
                code.load(value);
                code.callOnStack("storeExternal",
                    "(Ljava/lang/String;" + VALUE_DESC + ")V");
                return CONTINUE;
            }
            // The export: the identifier comes off the stack and nothing
            // goes on it.
            code.discardInt("popInt");
            code.loadThis();
            code.constant(identifier);
            code.callOnStack("exportVariable", "(Ljava/lang/String;)V");
            return CONTINUE;
        }
        case 0x018:
            code.call("swap", "()V");
            return CONTINUE;
        case 0x019:
            code.call("swapReturn", "()V");
            return CONTINUE;
        case 0x01A:
            code.discardValue("popValue");
            return CONTINUE;
        case 0x01B:
            code.call("duplicate", "()V");
            return CONTINUE;
        case 0x01C:
            code.discardInt("popReturnInt");
            code.returning();
            return ENDED;
        case 0x01D:
            code.discardInt("popReturnInt");
            code.call("exitProgram", "()V");
            code.returning();
            return ENDED;
        case 0x01F:
            code.call("popFlags", "()V");
            return CONTINUE;
        case 0x020:
        case 0x022:
            code.call("popFlags", "()V");
            code.discardInt("popReturnInt");
            code.returning();
            return ENDED;
        case 0x021:
        case 0x023:
            code.call("popFlags", "()V");
            code.discardInt("popReturnInt");
            code.call("exitProgram", "()V");
            code.returning();
            return ENDED;
        case 0x024:
        case 0x025:
        case 0x026: {
            // The value survives the return.
            int value = code.callValue("popValue");
            code.call("popFlags", "()V");
            code.discardInt("popReturnInt");
            if (slot != 0x024) {
                code.call("exitProgram", "()V");
            }
            code.pushValue(value);
            code.returning();
            return ENDED;
        }
        case 0x029:
            code.call("popBase", "()V");
            return CONTINUE;
        case 0x02A:
            code.call("popToBase", "()V");
            return CONTINUE;
        case 0x02B: {
            // The argument count comes off the stack.
            int count = code.callInt("popInt");
            code.loadThis();
            code.loadInt(count);
            code.callOnStack("pushBase", "(I)V");
            return CONTINUE;
        }
        case 0x02C:
            code.call("setGlobalBase", "()V");
            return CONTINUE;
        case 0x02F:
        case 0x030:
            return emitBranch(script, procedure, instruction, code, byOffset, emitted);
        case 0x031: {
            int index = code.callInt("popInt");
            int value = code.callValue("popValue");
            code.loadThis();
            code.loadInt(index);
            code.load(value);
            code.callOnStack("setFrameLocal", "(I" + VALUE_DESC + ")V");
            return CONTINUE;
        }
        case 0x032: {
            int index = code.callInt("popInt");
            int value = code.callValue("frameLocal", index);
            code.pushValue(value);
            return CONTINUE;
        }
        case 0x033:
            code.call("compareEqual", "()V");
            return CONTINUE;
        case 0x034:
            code.call("compareNotEqual", "()V");
            return CONTINUE;
        case 0x035:
            code.call("compareLessOrEqual", "()V");
            return CONTINUE;
        case 0x036:
            code.call("compareGreaterOrEqual", "()V");
            return CONTINUE;
        case 0x037:
            code.call("compareLess", "()V");
            return CONTINUE;
        case 0x038:
            code.call("compareGreater", "()V");
            return CONTINUE;
        case 0x039:
            code.call("add", "()V");
            return CONTINUE;
        case 0x03A:
            code.call("subtract", "()V");
            return CONTINUE;
        case 0x03B:
            code.call("multiply", "()V");
            return CONTINUE;
        case 0x03C:
            code.call("divide", "()V");
            return CONTINUE;
        case 0x03D:
            code.call("modulo", "()V");
            return CONTINUE;
        case 0x03E:
            code.call("logicalAnd", "()V");
            return CONTINUE;
        case 0x03F:
            code.call("logicalOr", "()V");
            return CONTINUE;
        case 0x040:
            code.call("bitwiseAnd", "()V");
            return CONTINUE;
        case 0x041:
            code.call("bitwiseOr", "()V");
            return CONTINUE;
        case 0x042:
            code.call("bitwiseXor", "()V");
            return CONTINUE;
        case 0x043:
            code.call("bitwiseNot", "()V");
            return CONTINUE;
        case 0x044:
            code.call("floor", "()V");
            return CONTINUE;
        case 0x045:
            code.call("logicalNot", "()V");
            return CONTINUE;
        case 0x046:
            code.call("negate", "()V");
            return CONTINUE;
        default:
            break;
        }

        // The game's builtins.
        String builtin = GameApiSlots.METHOD[slot];
        if (builtin != null) {
            emitApiCall(slot, builtin, code);
            return CONTINUE;
        }

        emitted.problems.add(script.name + ": the opcode 0x"
            + Integer.toHexString(instruction.opcode) + " has no code yet");
        return CONTINUE;
    }

    /**
     * The name of an external variable.
     *
     * <p>The name is on the stack, and the original reads it out of the
     * script's identifiers.
     */
    private static String externalName(CompiledScript script, Instruction instruction) {
        Integer offset = instruction.topValue;
        return offset == null ? null : script.identifierAt(offset);
    }

    /**
     * Emits a branch.
     *
     * <p>The target of a branch is a value on the stack, and the analysis
     * has resolved it, so the jump goes where the analysis says. The VM
     * jumps when the condition is empty, and that is what the code does.
     */
    private static int emitBranch(CompiledScript script, Procedure procedure,
            Instruction instruction, Code code, Map<Integer, Block> byOffset, Emitted emitted) {
        Block target = byOffset.get(instruction.target);
        if (target == null) {
            // The branch lands in another procedure, and the compiled code
            // has no way to jump into the middle of one.
            return CONTINUE;
        }

        // The condition comes off the stack, and under it the target.
        int condition = code.callValue("popValue");
        if (instruction.slot == 0x02F) {
            // The if: the target comes off the stack too.
            code.discardValue("popValue");
            code.isEmpty(condition);
            code.jumpWhenEmpty(target.label);
            return CONTINUE;
        }

        // The while: the target stays on the stack while the body runs, and
        // it comes off the stack when the execution goes back.
        code.isEmpty(condition);
        Label otherwise = code.label();
        code.jumpWhenNotEmpty(otherwise);
        code.discardValue("popValue");
        code.jump(target.label);
        code.mark(otherwise);
        return CONTINUE;
    }

    /**
     * Emits a jump.
     *
     * <p>The original's calls are a jump with the return address on the
     * return stack, and that is what the emitter looks for: a jump to
     * another procedure, with the address of the instruction after it on the
     * return stack, is a call. A jump to another procedure without one is a
     * tail jump, which in the compiled code is a return.
     */
    private static int emitJump(CompiledScript script, Procedure procedure,
            Instruction instruction, Code code, Map<Integer, Block> byOffset, Emitted emitted) {
        // The jump pops the address it goes to.
        code.discardValue("popValue");

        if (instruction.targetProcedure == procedure.index) {
            // A jump inside the procedure: the execution continues there.
            Block target = byOffset.get(instruction.target);
            if (target == null) {
                code.returning();
                return ENDED;
            }
            code.jump(target.label);
            return JUMPED;
        }

        if (instruction.targetProcedure == -1) {
            // The launcher: the original jumps to the end of the program
            // there.
            code.call("exitProgram", "()V");
            code.returning();
            return ENDED;
        }

        Procedure target = procedureByIndex(script, instruction.targetProcedure);
        if (target == null) {
            code.returning();
            return ENDED;
        }

        // The execution continues in the procedure it jumps to, so it is a
        // call. The original leaves the return address on the return stack
        // before it, and the compiled code does the same, so the two agree
        // on where the return goes.
        code.call(methodName(target), "()V");
        if (!hasReturnAddress(procedure, instruction)) {
            // A tail jump: the procedure it jumped to does not come back
            // here.
            code.returning();
            return ENDED;
        }
        return CONTINUE;
    }

    /** The procedure with an index, or null. */
    private static Procedure procedureByIndex(CompiledScript script, int index) {
        for (Procedure procedure : script.procedures) {
            if (procedure.index == index) {
                return procedure;
            }
        }
        return null;
    }

    /**
     * Whether the address of the instruction after a jump is on the return
     * stack, which is what makes the jump a call.
     */
    private static boolean hasReturnAddress(Procedure procedure, Instruction instruction) {
        int continuation = -1;
        for (Instruction candidate : procedure.code) {
            if (candidate.offset > instruction.offset) {
                continuation = candidate.offset;
                break;
            }
        }
        if (continuation < 0) {
            return false;
        }
        for (Instruction candidate : procedure.code) {
            if (candidate.offset >= instruction.offset) {
                break;
            }
            if (candidate.slot == 0x00D && candidate.topValue != null
                    && candidate.topValue == continuation) {
                return true;
            }
            if (candidate.branch) {
                break;
            }
        }
        return false;
    }

    /** Emits a call on the game API. */
    private static void emitApiCall(int slot, String name, Code code) {
        String kinds = GameApiSlots.KINDS[slot];
        String[] parts = kinds.isEmpty() ? new String[0] : kinds.split(",");

        // The arguments come off the stack in the reverse order of the
        // method's parameters, into locals of their own.
        int[] arguments = new int[parts.length];
        for (int i = parts.length - 1; i >= 0; i--) {
            switch (parts[i]) {
            case "int":
                arguments[i] = code.callInt("popInt");
                break;
            case "string":
                arguments[i] = code.callString("popString");
                break;
            case "ptr":
                arguments[i] = code.callObject("popPointer");
                break;
            default:
                arguments[i] = code.callValue("popValue");
                break;
            }
        }

        // The call.
        code.loadApi();
        StringBuilder descriptor = new StringBuilder("(");
        for (String part : parts) {
            descriptor.append(argumentDescriptor(part));
        }
        descriptor.append(')').append(resultDescriptor(GameApiSlots.RESULTS[slot]));
        for (int i = 0; i < parts.length; i++) {
            if ("int".equals(parts[i])) {
                code.loadInt(arguments[i]);
            } else {
                code.load(arguments[i]);
            }
        }
        code.callInterface(name, descriptor.toString());

        // And what it leaves behind. The value goes into a local first, so
        // the JVM's stack is empty again.
        switch (GameApiSlots.RESULTS[slot]) {
        case "int": {
            int number = code.local();
            code.storeInt(number);
            code.pushInt(number);
            break;
        }
        case "string": {
            int text = code.local();
            code.store(text);
            code.pushString(text);
            break;
        }
        case "ptr": {
            int pointer = code.local();
            code.store(pointer);
            code.pushPointer(pointer);
            break;
        }
        case "value": {
            int value = code.local();
            code.store(value);
            code.pushValue(value);
            break;
        }
        default:
            break;
        }
    }

    /** The descriptor of an argument. */
    private static String argumentDescriptor(String kind) {
        switch (kind) {
        case "int": return "I";
        case "string": return STRING_DESC;
        case "ptr": return OBJECT_DESC;
        default: return VALUE_DESC;
        }
    }

    /** The descriptor of what a call leaves behind. */
    private static String resultDescriptor(String kind) {
        switch (kind) {
        case "int": return "I";
        case "string": return STRING_DESC;
        case "ptr": return OBJECT_DESC;
        case "value": return VALUE_DESC;
        default: return "V";
        }
    }

    /**
     * The call of a procedure by index.
     *
     * <p>The original names the procedure on the stack and jumps to it, so
     * the compiled code looks the name up.
     */
    private static void emitCallProcedure(CompiledScript script, ClassWriter writer) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PROTECTED, "callProcedure",
            "(I)V", null, null);
        method.visitCode();
        Code code = new Code(method, className(script));

        List<Procedure> procedures = new ArrayList<>();
        for (Procedure procedure : script.procedures) {
            if (procedure.index >= 0) {
                procedures.add(procedure);
            }
        }
        procedures.sort((left, right) -> Integer.compare(left.index, right.index));

        int[] keys = new int[procedures.size()];
        Label[] labels = new Label[procedures.size()];
        for (int i = 0; i < procedures.size(); i++) {
            keys[i] = procedures.get(i).index;
            labels[i] = new Label();
        }
        Label none = new Label();
        method.visitVarInsn(Opcodes.ILOAD, 1);
        method.visitLookupSwitchInsn(none, keys, labels);
        for (int i = 0; i < procedures.size(); i++) {
            method.visitLabel(labels[i]);
            if ((procedures.get(i).flags & 0x10) != 0) {
                // The procedure wants the critical section.
                code.call("enterCritical", "()V");
            }
            code.call(methodName(procedures.get(i)), "()V");
            method.visitInsn(Opcodes.RETURN);
        }
        method.visitLabel(none);
        method.visitInsn(Opcodes.RETURN);

        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    /**
     * The code of one method.
     *
     * <p>Every value the code works with goes into a local of its own, so
     * that the JVM's stack is empty again after every step. That is what
     * lets the branches land anywhere: the stack is the same wherever the
     * execution comes from.
     */
    private static final class Code {
        /** The method the code goes into. */
        private final MethodVisitor method;

        /** The class the code belongs to: the owner of the inherited members. */
        private final String owner;

        /** The next free local slot. */
        private int nextLocal = 1;

        Code(MethodVisitor method, String owner) {
            this.method = method;
            this.owner = owner;
        }

        /** A local slot of its own, so that no two values share one. */
        int local() {
            return nextLocal++;
        }

        /** The script itself. */
        void loadThis() {
            method.visitVarInsn(Opcodes.ALOAD, 0);
        }

        /** A local of its own, loaded. */
        void load(int slot) {
            method.visitVarInsn(Opcodes.ALOAD, slot);
        }

        /** A local of its own, loaded as an integer. */
        void loadInt(int slot) {
            method.visitVarInsn(Opcodes.ILOAD, slot);
        }

        /** A local of its own, kept. */
        void store(int slot) {
            method.visitVarInsn(Opcodes.ASTORE, slot);
        }

        /** A local of its own, kept as an integer. */
        void storeInt(int slot) {
            method.visitVarInsn(Opcodes.ISTORE, slot);
        }

        /** A constant. */
        void constant(int value) {
            method.visitLdcInsn(value);
        }

        /** A constant float. */
        void constant(float value) {
            method.visitLdcInsn(value);
        }

        /** A constant string. */
        void constant(String value) {
            method.visitLdcInsn(value);
        }

        /** Throws a value off the JVM's stack. */
        void discard() {
            method.visitInsn(Opcodes.POP);
        }

        /**
         * Calls one of the base class's methods and throws what it gives
         * back away.
         *
         * <p>The value goes off the JVM's stack right away and not through a
         * local: a value that is stored and then dropped is a shape that
         * makes the frame computation of the class file library stumble.
         */
        void discardValue(String name) {
            loadThis();
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, "()" + VALUE_DESC, false);
            method.visitInsn(Opcodes.POP);
        }

        /** The same for a method that gives an integer back. */
        void discardInt(String name) {
            loadThis();
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, "()I", false);
            method.visitInsn(Opcodes.POP);
        }

        /** The method ends. */
        void returning() {
            method.visitInsn(Opcodes.RETURN);
        }

        /**
         * Calls one of the base class's methods.
         *
         * <p>The receiver goes on the stack first, so the method takes no
         * arguments of its own.
         */
        void call(String name, String descriptor) {
            loadThis();
            callOnStack(name, descriptor);
        }

        /**
         * Calls one of the base class's methods.
         *
         * <p>The receiver and the arguments are on the stack already.
         */
        void callOnStack(String name, String descriptor) {
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, false);
        }

        /** Calls a method of the game API. */
        void callInterface(String name, String descriptor) {
            method.visitMethodInsn(Opcodes.INVOKEINTERFACE, API, name, descriptor, true);
        }

        /** The game API the script talks to. */
        void loadApi() {
            loadThis();
            method.visitFieldInsn(Opcodes.GETFIELD, owner, "gameApi", "L" + API + ";");
        }

        /** Calls one of the base class's methods that gives an integer back. */
        int callInt(String name) {
            loadThis();
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, "()I", false);
            int slot = local();
            method.visitVarInsn(Opcodes.ISTORE, slot);
            return slot;
        }

        /** Calls one of the base class's methods that gives a value back. */
        int callValue(String name) {
            loadThis();
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, "()" + VALUE_DESC, false);
            int slot = local();
            method.visitVarInsn(Opcodes.ASTORE, slot);
            return slot;
        }

        /** Calls one of the base class's methods that takes an index. */
        int callValue(String name, int argument) {
            loadThis();
            method.visitVarInsn(Opcodes.ILOAD, argument);
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, "(I)" + VALUE_DESC, false);
            int slot = local();
            method.visitVarInsn(Opcodes.ASTORE, slot);
            return slot;
        }

        /** Calls one of the base class's methods that gives a string back. */
        int callString(String name) {
            loadThis();
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, "()" + STRING_DESC, false);
            int slot = local();
            method.visitVarInsn(Opcodes.ASTORE, slot);
            return slot;
        }

        /** Calls one of the base class's methods that gives a pointer back. */
        int callObject(String name) {
            loadThis();
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, "()" + OBJECT_DESC, false);
            int slot = local();
            method.visitVarInsn(Opcodes.ASTORE, slot);
            return slot;
        }

        /** Pushes a value onto the script's stack. */
        void pushValue(int value) {
            loadThis();
            method.visitVarInsn(Opcodes.ALOAD, value);
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "pushValue",
                "(" + VALUE_DESC + ")V", false);
        }

        /** Pushes an integer onto the script's stack. */
        void pushInt(int value) {
            loadThis();
            method.visitVarInsn(Opcodes.ILOAD, value);
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "pushInt", "(I)V", false);
        }

        /** Pushes a string onto the script's stack. */
        void pushString(int value) {
            loadThis();
            method.visitVarInsn(Opcodes.ALOAD, value);
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "pushString",
                "(" + STRING_DESC + ")V", false);
        }

        /** Pushes a pointer onto the script's stack. */
        void pushPointer(int value) {
            loadThis();
            method.visitVarInsn(Opcodes.ALOAD, value);
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "pushPointer",
                "(" + OBJECT_DESC + ")V", false);
        }

        /** Whether the value is empty, as the VM's branches ask. */
        void isEmpty(int value) {
            method.visitVarInsn(Opcodes.ALOAD, value);
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, VALUE, "isEmpty", "()Z", false);
        }

        /**
         * Jumps when the value is empty.
         *
         * <p>The VM's branches go to the target when the condition is
         * empty, and the condition is empty when the value says so.
         */
        void jumpWhenEmpty(Label target) {
            method.visitJumpInsn(Opcodes.IFNE, target);
        }

        /** Jumps when the value is not empty. */
        void jumpWhenNotEmpty(Label target) {
            method.visitJumpInsn(Opcodes.IFEQ, target);
        }

        /** Jumps. */
        void jump(Label target) {
            method.visitJumpInsn(Opcodes.GOTO, target);
        }

        /** A label of its own. */
        Label label() {
            return new Label();
        }

        /** Marks a label. */
        void mark(Label label) {
            method.visitLabel(label);
        }
    }
}
