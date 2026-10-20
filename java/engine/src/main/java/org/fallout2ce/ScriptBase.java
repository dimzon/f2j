package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * The base class of the compiled scripts.
 *
 * <p>Every script the compiler produces is a subclass of this: one method per
 * script procedure, and the methods call the helpers here for everything the
 * VM's value stack does.
 *
 * <p>The game API is a field, not a static, so a compiled script is given
 * whichever implementation the caller wants - the game, a mock in a test, or
 * a decorator that logs what the script asks for.
 *
 * <p>What lives here is the script's own state, and it is the state the VM
 * keeps: the value stack that every procedure shares, the frame pointer that
 * says where a procedure's locals start, the return stack, the flags, and
 * the string heap. The game's own state - the objects, the map, the global
 * variables - is not here; that is what {@link GameApi} is for.
 */
public abstract class ScriptBase {

    /** The game API the compiled code calls. */
    protected GameApi gameApi;

    /** The value stack, shared by every procedure of the script. */
    protected final List<Program.ProgramValue> stack = new ArrayList<>();

    /** The return stack. */
    protected final List<Program.ProgramValue> returnStack = new ArrayList<>();

    /** Where the current procedure's locals start in the value stack. */
    protected int framePointer = -1;

    /** The program's flags, as the VM keeps them. */
    protected int flags;

    /** The window the script is talking to. */
    protected int windowId;

    /** The script's own action. */
    protected int scriptAction;

    /** The script's owner, the object it runs for. */
    protected Object self;

    protected ScriptBase(GameApi gameApi) {
        this.gameApi = gameApi;
    }

    /** The script API the compiled code calls. */
    public final GameApi gameApi() {
        return gameApi;
    }

    // ------------------------------------------------------------------
    // The value stack. The compiled code calls these; they are what the
    // VM's opcode handlers do one for one.
    // ------------------------------------------------------------------

    /** Pushes an integer. */
    protected final void pushInt(int value) {
        stack.add(new Program.ProgramValue(value));
    }

    /** Pushes a float. */
    protected final void pushFloat(float value) {
        stack.add(new Program.ProgramValue(value));
    }

    /** Pushes a string. */
    protected final void pushString(String value) {
        Program.ProgramValue programValue = new Program.ProgramValue();
        programValue.opcode = Program.VALUE_TYPE_STRING;
        programValue.integerValue = intern(value);
        stack.add(programValue);
    }

    /** Pushes a pointer - the game's object, or null. */
    protected final void pushPointer(Object value) {
        Program.ProgramValue programValue = new Program.ProgramValue();
        programValue.opcode = Program.VALUE_TYPE_PTR;
        programValue.pointerValue = value;
        stack.add(programValue);
    }

    /** Pushes a value as it is. */
    protected final void pushValue(Program.ProgramValue value) {
        stack.add(value);
    }

    /** Reads the top of the value stack without removing it. */
    protected final Program.ProgramValue peek() {
        return stack.get(stack.size() - 1);
    }

    /** Removes and returns the top of the value stack. */
    protected final Program.ProgramValue popValue() {
        return stack.remove(stack.size() - 1);
    }

    /** Removes and returns the top of the value stack as an integer. */
    protected final int popInt() {
        return popValue().integerValue;
    }

    /** Removes and returns the top of the value stack as a string. */
    protected final String popString() {
        Program.ProgramValue value = popValue();
        return value.opcode == Program.VALUE_TYPE_STRING
            ? stringAt(value)
            : null;
    }

    /** Removes and returns the top of the value stack as a pointer. */
    protected final Object popPointer() {
        return popValue().pointerValue;
    }

    /** The number of values on the value stack. */
    protected final int stackSize() {
        return stack.size();
    }

    // ------------------------------------------------------------------
    // The return stack.
    // ------------------------------------------------------------------

    /** Moves the top of the value stack onto the return stack. */
    protected final void dataToAddress() {
        returnStack.add(popValue());
    }

    /** Moves the top of the return stack onto the value stack. */
    protected final void addressToData() {
        stack.add(returnStack.remove(returnStack.size() - 1));
    }

    /** Pushes an integer onto the return stack. */
    protected final void pushReturnInt(int value) {
        returnStack.add(new Program.ProgramValue(value));
    }

    /** Removes and returns the top of the return stack as an integer. */
    protected final int popReturnInt() {
        return returnStack.remove(returnStack.size() - 1).integerValue;
    }

    // ------------------------------------------------------------------
    // The procedure frame.
    // ------------------------------------------------------------------

    /**
     * Starts a procedure's frame: the frame pointer becomes the stack size
     * less the number of arguments.
     */
    protected final void pushBase(int argumentCount) {
        pushReturnInt(framePointer);
        framePointer = stack.size() - argumentCount;
    }

    /** Ends a procedure's frame, restoring the caller's. */
    protected final void popBase() {
        framePointer = popReturnInt();
    }

    /** Throws the values above the frame pointer away. */
    protected final void popToBase() {
        while (stack.size() != framePointer) {
            popValue();
        }
    }

    /** One of the current procedure's locals. */
    protected final Program.ProgramValue frameLocal(int index) {
        return stack.get(framePointer + index);
    }

    /** Writes one of the current procedure's locals. */
    protected final void setFrameLocal(int index, Program.ProgramValue value) {
        stack.set(framePointer + index, value);
    }

    // ------------------------------------------------------------------
    // The builtins that are the script's own state, rather than the game's.
    // They are not on the game API: the compiled code reaches them here.
    // ------------------------------------------------------------------

    /**
     * op_self_obj: the object the script runs for.
     *
     * <p>C's scriptGetSelf finds the script that owns the program and returns
     * its owner, or for a spatial script a hidden marker object. With no
     * script attached there is no owner, and the original answers null.
     */
    public final Object selfObj() {
        return self;
    }

    /**
     * op_get_local_var: one of the script's own local variables.
     *
     * <p>Without a script there is no context, and the original answers -1.
     */
    public final Program.ProgramValue getLocalVar(int index) {
        if (index < 0 || framePointer < 0 || framePointer + index >= stack.size()) {
            return new Program.ProgramValue(-1);
        }
        return frameLocal(index);
    }

    /**
     * op_set_local_var: write one of the script's own local variables.
     * Without a script there is no context and the write is dropped.
     */
    public final void setLocalVar(int index, Program.ProgramValue value) {
        if (index < 0 || framePointer < 0 || framePointer + index >= stack.size()) {
            return;
        }
        setFrameLocal(index, value);
    }

    /** op_script_action: the script's action. */
    public final int scriptAction() {
        return scriptAction;
    }

    // ------------------------------------------------------------------
    // The string heap. The VM interns a string and keeps its offset, so a
    // script can compare strings by value.
    // ------------------------------------------------------------------

    private final List<String> strings = new ArrayList<>();

    /** Adds a string to the heap and returns its offset. */
    protected final int intern(String value) {
        strings.add(value);
        return strings.size() - 1;
    }

    /** The string at an offset. */
    protected final String stringAt(Program.ProgramValue value) {
        int offset = value.integerValue;
        if (offset < 0 || offset >= strings.size()) {
            return null;
        }
        return strings.get(offset);
    }
}
