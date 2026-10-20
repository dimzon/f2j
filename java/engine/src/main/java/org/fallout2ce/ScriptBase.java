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

    /** Where the script's globals are, as the VM keeps them. */
    protected int basePointer;

    /** The wait function the original keeps together with the flags. */
    protected Object checkWaitFunc;

    /** Whether the script has finished, as the VM keeps it. */
    protected boolean finished;

    /** The critical section's flag, as the VM keeps it. */
    protected static final int CRITICAL_SECTION = 0x0001;

    protected ScriptBase(GameApi gameApi) {
        this.gameApi = gameApi;
    }

    /** The script API the compiled code calls. */
    public final GameApi gameApi() {
        return gameApi;
    }

    /**
     * Runs the script.
     *
     * <p>The compiled code overrides this: the launcher of the script is
     * what the VM starts on, and it is what runs here.
     */
    public abstract void run();

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
    // The VM's own opcodes. The compiled code calls these instead of
    // dispatching on an opcode, and they do what the original's handlers
    // do, one for one.
    // ------------------------------------------------------------------

    /** op_set_global (0x2C): the script's globals start here. */
    protected final void setGlobalBase() {
        basePointer = stack.size();
    }

    /** op_fetch_global (0x12): one of the script's globals. */
    protected final Program.ProgramValue fetchGlobal(int index) {
        int position = basePointer + index;
        if (position < 0 || position >= stack.size()) {
            throw new IllegalStateException("fetchGlobal: index out of range");
        }
        return stack.get(position);
    }

    /** op_store_global (0x13): write one of the script's globals. */
    protected final void storeGlobal(int index, Program.ProgramValue value) {
        int position = basePointer + index;
        if (position < 0 || position >= stack.size()) {
            throw new IllegalStateException("storeGlobal: index out of range");
        }
        stack.set(position, value);
    }

    /** op_enter_critical_section (0x02) and op_start_critical (0x4A). */
    protected final void enterCritical() {
        flags |= CRITICAL_SECTION;
    }

    /** op_leave_critical_section (0x03) and op_end_critical (0x4B). */
    protected final void leaveCritical() {
        flags &= ~CRITICAL_SECTION;
    }

    /** op_swap (0x18): the two values on top of the stack change places. */
    protected final void swap() {
        int top = stack.size() - 1;
        Program.ProgramValue value = stack.get(top);
        stack.set(top, stack.get(top - 1));
        stack.set(top - 1, value);
    }

    /** op_swap_a (0x19): the two values on the return stack change places. */
    protected final void swapReturn() {
        int top = returnStack.size() - 1;
        Program.ProgramValue value = returnStack.get(top);
        returnStack.set(top, returnStack.get(top - 1));
        returnStack.set(top - 1, value);
    }

    /** op_duplicate (0x1B): the value on top of the stack goes on top again. */
    protected final void duplicate() {
        stack.add(stack.get(stack.size() - 1));
    }

    /** op_pop_flags (0x1F): the window, the wait function and the flags. */
    protected final void popFlags() {
        windowId = popInt();
        checkWaitFunc = popPointer();
        flags = popInt() & 0xFFFF;
    }

    /**
     * op_detach (0x0F): the script leaves the script that started it.
     *
     * <p>The original takes the child off its parent. A compiled script has
     * no parent - the game's own script system is what starts scripts, and
     * that is the game's business - so there is nothing to detach from.
     */
    protected final void detach() {
    }

    /** op_exit (0x0E): the script ends. */
    protected final void exitScript() {
        finished = true;
    }

    /** op_exit_program (0x10): the program ends. */
    protected final void exitProgram() {
        finished = true;
    }

    /** op_stop_program (0x11): the program stops. */
    protected final void stopProgram() {
        finished = true;
    }

    // ------------------------------------------------------------------
    // The arithmetic. The original works on the values and not on their
    // types: a string plus a number is a string, an integer plus an
    // integer is an integer unless it overflows, and then it is a float.
    // ------------------------------------------------------------------

    /** op_add (0x39). */
    protected final void add() {
        Program.ProgramValue right = popValue();
        Program.ProgramValue left = popValue();
        switch (right.opcode) {
        case Program.VALUE_TYPE_STRING:
        case Program.VALUE_TYPE_DYNAMIC_STRING:
            pushString(stringAt(right) + text(left));
            break;
        case Program.VALUE_TYPE_FLOAT:
            if (left.opcode == Program.VALUE_TYPE_FLOAT) {
                pushFloat(right.floatValue + left.floatValue);
            } else {
                pushFloat(right.floatValue + left.integerValue);
            }
            break;
        case Program.VALUE_TYPE_INT:
            if (isString(left)) {
                pushString(Integer.toString(right.integerValue) + text(left));
            } else if (left.opcode == Program.VALUE_TYPE_FLOAT) {
                pushFloat(right.integerValue + left.floatValue);
            } else {
                int sum = right.integerValue + left.integerValue;
                if (((right.integerValue ^ sum) & (left.integerValue ^ sum)) < 0) {
                    // The sum overflowed, so it is a float.
                    pushFloat((float) right.integerValue + left.integerValue);
                } else {
                    pushInt(sum);
                }
            }
            break;
        default:
            pushString(text(right) + text(left));
            break;
        }
    }

    /** op_sub (0x3A). */
    protected final void subtract() {
        Program.ProgramValue right = popValue();
        Program.ProgramValue left = popValue();
        switch (right.opcode) {
        case Program.VALUE_TYPE_FLOAT:
            pushFloat(right.floatValue - (left.opcode == Program.VALUE_TYPE_FLOAT
                ? left.floatValue : left.integerValue));
            break;
        case Program.VALUE_TYPE_INT:
            if (left.opcode == Program.VALUE_TYPE_FLOAT) {
                pushFloat(right.integerValue - left.floatValue);
            } else {
                pushInt(right.integerValue - left.integerValue);
            }
            break;
        default:
            break;
        }
    }

    /** op_mul (0x3B). */
    protected final void multiply() {
        Program.ProgramValue right = popValue();
        Program.ProgramValue left = popValue();
        switch (right.opcode) {
        case Program.VALUE_TYPE_FLOAT:
            pushFloat(right.floatValue * (left.opcode == Program.VALUE_TYPE_FLOAT
                ? left.floatValue : left.integerValue));
            break;
        case Program.VALUE_TYPE_INT:
            if (left.opcode == Program.VALUE_TYPE_FLOAT) {
                pushFloat(right.integerValue * left.floatValue);
            } else {
                pushInt(left.integerValue * right.integerValue);
            }
            break;
        default:
            break;
        }
    }

    /** op_div (0x3C). */
    protected final void divide() {
        Program.ProgramValue right = popValue();
        Program.ProgramValue left = popValue();
        switch (right.opcode) {
        case Program.VALUE_TYPE_FLOAT: {
            float divisor = left.opcode == Program.VALUE_TYPE_FLOAT
                ? left.floatValue : left.integerValue;
            if (divisor == 0.0f) {
                throw new IllegalStateException("Division (DIV) by zero");
            }
            pushFloat(right.floatValue / divisor);
            break;
        }
        case Program.VALUE_TYPE_INT:
            if (left.opcode == Program.VALUE_TYPE_FLOAT) {
                if (left.floatValue == 0.0f) {
                    throw new IllegalStateException("Division (DIV) by zero");
                }
                pushFloat((float) right.integerValue / left.floatValue);
            } else {
                if (left.integerValue == 0) {
                    throw new IllegalStateException("Division (DIV) by zero");
                }
                pushInt(right.integerValue / left.integerValue);
            }
            break;
        default:
            break;
        }
    }

    /** op_mod (0x3D). */
    protected final void modulo() {
        Program.ProgramValue right = popValue();
        Program.ProgramValue left = popValue();
        if (right.opcode == Program.VALUE_TYPE_FLOAT) {
            throw new IllegalStateException("Trying to MOD a float");
        }
        if (right.opcode != Program.VALUE_TYPE_INT) {
            return;
        }
        if (left.opcode == Program.VALUE_TYPE_FLOAT) {
            throw new IllegalStateException("Trying to MOD with a float");
        }
        if (left.integerValue == 0) {
            throw new IllegalStateException("Division (MOD) by zero");
        }
        pushInt(right.integerValue % left.integerValue);
    }

    /** op_floor (0x44). */
    protected final void floor() {
        Program.ProgramValue value = popValue();
        if (value.opcode == Program.VALUE_TYPE_STRING) {
            throw new IllegalStateException("Invalid arg given to floor()");
        }
        if (value.opcode == Program.VALUE_TYPE_FLOAT) {
            value.opcode = Program.VALUE_TYPE_INT;
            value.integerValue = (int) value.floatValue;
            value.floatValue = Float.intBitsToFloat(value.integerValue);
        }
        pushValue(value);
    }

    /** op_neg (0x46). */
    protected final void negate() {
        Program.ProgramValue value = popValue();
        switch (value.opcode) {
        case Program.VALUE_TYPE_INT:
            pushInt(-value.integerValue);
            break;
        case Program.VALUE_TYPE_FLOAT:
            pushFloat(-value.floatValue);
            break;
        default:
            throw new IllegalStateException("Invalid arg given to NEG");
        }
    }

    /** op_not (0x45): the original looks at the value's integer, whatever it is. */
    protected final void logicalNot() {
        Program.ProgramValue value = popValue();
        pushInt(value.integerValue == 0 ? 1 : 0);
    }

    /** op_and (0x3E) and op_or (0x3F). */
    protected final void logical(int kind) {
        Program.ProgramValue right = popValue();
        Program.ProgramValue left = popValue();
        boolean a = truthy(right);
        boolean b = truthy(left);
        pushInt((kind == 0 ? (a && b) : (a || b)) ? 1 : 0);
    }

    /**
     * Whether a value counts as true.
     *
     * <p>A string always does, a float does when its bits are not zero, an
     * integer when it is not zero and a pointer when it is not null.
     */
    private static boolean truthy(Program.ProgramValue value) {
        switch (value.opcode) {
        case Program.VALUE_TYPE_STRING:
        case Program.VALUE_TYPE_DYNAMIC_STRING:
            return true;
        case Program.VALUE_TYPE_FLOAT:
            return (value.integerValue & 0x7FFFFFFF) != 0;
        case Program.VALUE_TYPE_INT:
            return value.integerValue != 0;
        case Program.VALUE_TYPE_PTR:
            return value.pointerValue != null;
        default:
            return false;
        }
    }

    /** op_bitwise_not (0x43). */
    protected final void bitwiseNot() {
        pushInt(~popInt());
    }

    /** op_bitwise_and (0x40), op_bitwise_or (0x41), op_bitwise_xor (0x42). */
    protected final void bitwise(int kind) {
        int right = popInt();
        int left = popInt();
        switch (kind) {
        case 0: pushInt(left & right); break;
        case 1: pushInt(left | right); break;
        default: pushInt(left ^ right); break;
        }
    }

    /** Whether a value is a string of either kind. */
    private static boolean isString(Program.ProgramValue value) {
        return value.opcode == Program.VALUE_TYPE_STRING
            || value.opcode == Program.VALUE_TYPE_DYNAMIC_STRING;
    }

    // ------------------------------------------------------------------
    // The comparisons. The original compares the values and not their
    // types: a string against a number compares the text of the number.
    // ------------------------------------------------------------------

    /** The comparison the opcodes ask for. */
    protected enum Compare {
        EQUAL, NOT_EQUAL, LESS, LESS_OR_EQUAL, GREATER, GREATER_OR_EQUAL
    }

    /** op_equal (0x33) and the five that go with it. */
    protected final void compare(Compare kind) {
        Program.ProgramValue right = popValue();
        Program.ProgramValue left = popValue();

        boolean result;
        switch (right.opcode) {
        case Program.VALUE_TYPE_STRING:
        case Program.VALUE_TYPE_DYNAMIC_STRING: {
            String first = stringAt(right);
            String second = text(left);
            result = kind == Compare.EQUAL ? first.equals(second)
                : kind == Compare.NOT_EQUAL ? !first.equals(second)
                : kind == Compare.LESS ? first.compareTo(second) < 0
                : kind == Compare.LESS_OR_EQUAL ? first.compareTo(second) <= 0
                : kind == Compare.GREATER ? first.compareTo(second) > 0
                : first.compareTo(second) >= 0;
            break;
        }
        case Program.VALUE_TYPE_FLOAT: {
            float first = right.floatValue;
            float second = left.opcode == Program.VALUE_TYPE_FLOAT ? left.floatValue
                : left.opcode == Program.VALUE_TYPE_INT ? left.integerValue
                : 0.0f;
            result = compareFloats(kind, first, second);
            break;
        }
        case Program.VALUE_TYPE_INT: {
            if (left.opcode == Program.VALUE_TYPE_STRING
                    || left.opcode == Program.VALUE_TYPE_DYNAMIC_STRING) {
                String first = Integer.toString(right.integerValue);
                String second = text(left);
                result = kind == Compare.EQUAL ? first.equals(second)
                    : kind == Compare.NOT_EQUAL ? !first.equals(second)
                    : kind == Compare.LESS ? first.compareTo(second) < 0
                    : kind == Compare.LESS_OR_EQUAL ? first.compareTo(second) <= 0
                    : kind == Compare.GREATER ? first.compareTo(second) > 0
                    : first.compareTo(second) >= 0;
                break;
            }
            float first = right.integerValue;
            float second = left.opcode == Program.VALUE_TYPE_FLOAT ? left.floatValue
                : left.integerValue;
            result = compareFloats(kind, first, second);
            break;
        }
        default: {
            float first = 0.0f;
            float second = 0.0f;
            result = compareFloats(kind, first, second);
            break;
        }
        }
        pushInt(result ? 1 : 0);
    }

    /** The comparison of two floats. */
    private static boolean compareFloats(Compare kind, float a, float b) {
        switch (kind) {
        case EQUAL: return a == b;
        case NOT_EQUAL: return a != b;
        case LESS: return a < b;
        case LESS_OR_EQUAL: return a <= b;
        case GREATER: return a > b;
        default: return a >= b;
        }
    }

    /** The text of a value, the way the original formats it. */
    private String text(Program.ProgramValue value) {
        switch (value.opcode) {
        case Program.VALUE_TYPE_STRING:
        case Program.VALUE_TYPE_DYNAMIC_STRING:
            return stringAt(value);
        case Program.VALUE_TYPE_FLOAT:
            return String.format(java.util.Locale.ROOT, "%.5f", value.floatValue);
        case Program.VALUE_TYPE_INT:
            return Integer.toString(value.integerValue);
        default:
            return "";
        }
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
