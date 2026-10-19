package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [vmdelayed]} vectors from {@code docker/refharness/golden/v123.txt},
 * produced by {@code sectionVmDelayed} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The part of the delayed/conditional-call family that does not read the
 * clock: {@code CALL_WHEN}, {@code CANCEL} and {@code CANCEL_ALL}. They only
 * rewrite procedure-table fields, so the harness prints every procedure row's
 * {@code flags}/{@code time}/{@code conditionOffset} afterwards and the
 * vectors assert where the writes landed.
 *
 * <p>{@code CALL_AT} and {@code WAIT} are deliberately absent. Both fold
 * {@code getInterpreterTime()} into a stored value, and C reaches that through
 * the file-static {@code interpreterTimerFunc} with no setter, so there is
 * nothing to pin the clock against from outside. {@code interpreterEventsSuspended}
 * looks like the escape hatch - it guards the {@code getInterpreterTime()} call
 * in {@code opDelayedCall} - but nothing in the original ever writes it, so it
 * is dead and the branch is always taken.
 *
 * <p>Push order is the other trap: {@code opConditionalCall} pops two integers
 * into {@code data[0]}, {@code data[1]}, then uses {@code data[0]} as the
 * procedure index. The index therefore sits on TOP of the stack, so a script
 * pushes the condition offset first.
 */
class ProgramVmDelayedTest {

    private static final int PROCEDURES_BASE_OFFSET = 42;
    private static final int PROCEDURE_SIZE = 24;
    private static final int PROCEDURE_COUNT = 4;

    private static final byte[] IDENTIFIERS =
        "main\0one\0two\0three".getBytes(StandardCharsets.ISO_8859_1);

    private static final byte[] STRINGS =
        "s".getBytes(StandardCharsets.ISO_8859_1);

    private static void writeBE32(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 24);
        data[offset + 1] = (byte) (value >> 16);
        data[offset + 2] = (byte) (value >> 8);
        data[offset + 3] = (byte) value;
    }

    private static final class Image {
        byte[] data;
        int codeOffset;
    }

    /** Mirrors {@code vmDelayedRun}. {@code initial[i]} is
     *  {flags, time, conditionOffset} for procedure {@code i}. */
    private static Image buildProgram(byte[] code, int[][] initial) {
        int identifiersOffset = 46 + PROCEDURE_SIZE * PROCEDURE_COUNT;
        int stringTableOffset = identifiersOffset + 4 + IDENTIFIERS.length;
        int codeOffset = stringTableOffset + 4 + STRINGS.length;
        int bodyOffset = codeOffset + code.length;

        byte[] data = new byte[1024];
        writeBE32(data, PROCEDURES_BASE_OFFSET, PROCEDURE_COUNT);
        for (int i = 0; i < PROCEDURE_COUNT; i++) {
            int row = 46 + PROCEDURE_SIZE * i;
            writeBE32(data, row + Program.PROCEDURE_NAME_OFFSET, 4 + i * 4);
            writeBE32(data, row + Program.PROCEDURE_FLAGS_OFFSET, initial[i][0]);
            writeBE32(data, row + Program.PROCEDURE_TIME_OFFSET, initial[i][1]);
            writeBE32(data, row + Program.PROCEDURE_CONDITION_OFFSET, initial[i][2]);
            writeBE32(data, row + Program.PROCEDURE_BODY_OFFSET, bodyOffset);
            writeBE32(data, row + Program.PROCEDURE_ARG_COUNT_OFFSET, 0);
        }

        writeBE32(data, identifiersOffset, IDENTIFIERS.length);
        System.arraycopy(IDENTIFIERS, 0, data, identifiersOffset + 4, IDENTIFIERS.length);
        writeBE32(data, stringTableOffset, STRINGS.length);
        System.arraycopy(STRINGS, 0, data, stringTableOffset + 4, STRINGS.length);
        System.arraycopy(code, 0, data, codeOffset, code.length);

        Image image = new Image();
        image.data = data;
        image.codeOffset = codeOffset;
        return image;
    }

    private static String dump(List<Program.ProgramValue> stack) {
        StringBuilder sb = new StringBuilder();
        for (int i = stack.size() - 1; i >= 0; i--) {
            Program.ProgramValue value = stack.get(i);
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%04X:%d", value.opcode & 0xFFFF, value.integerValue));
        }
        return sb.toString();
    }

    private static final class Result {
        int ip;
        int flags;
        String stack;
        int[][] rows;
    }

    private Result run(byte[] code, int[][] initial, int budget) throws Exception {
        Image image = buildProgram(code, initial);

        Path scriptPath = Files.createTempFile("vmdelayed", ".int");
        Files.write(scriptPath, image.data);

        Program program = Program.programCreateByPath(scriptPath.toString());
        assertTrue(program != null);
        program.flags = 0;
        program.instructionPointer = image.codeOffset;

        Program.programInterpret(program, budget);

        Result result = new Result();
        result.ip = program.instructionPointer;
        result.flags = program.flags;
        result.stack = dump(program.stackValues);
        result.rows = new int[PROCEDURE_COUNT][];
        for (int i = 0; i < PROCEDURE_COUNT; i++) {
            int row = program.procedures + 4 + PROCEDURE_SIZE * i;
            result.rows[i] = new int[] {
                Program.stackReadInt32(program.data, row + Program.PROCEDURE_FLAGS_OFFSET),
                Program.stackReadInt32(program.data, row + Program.PROCEDURE_TIME_OFFSET),
                Program.stackReadInt32(program.data, row + Program.PROCEDURE_CONDITION_OFFSET),
            };
        }

        Program.programFree(program);
        return result;
    }

    @BeforeEach
    void setup() {
        Db.resetForTests();
        Config.resetLastSectionKey();
        XFile.resetForTests();
        Program.interpreterRegisterOpcodeHandlers();
    }

    private static byte[] pushInt(int value) {
        return new byte[] {
            (byte) 0xC0, (byte) 0x01,
            (byte) (value >> 24), (byte) (value >> 16),
            (byte) (value >> 8), (byte) value,
        };
    }

    private static byte[] cat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) {
            len += p.length;
        }
        byte[] out = new byte[len];
        int at = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, at, p.length);
            at += p.length;
        }
        return out;
    }

    private static final byte[] CALL_WHEN = { (byte) 0x80, (byte) 0x07 };
    private static final byte[] CANCEL = { (byte) 0x80, (byte) 0x48 };
    private static final byte[] CANCEL_ALL = { (byte) 0x80, (byte) 0x49 };
    private static final byte[] STOP = { (byte) 0x80, (byte) 0x11 };

    /** Condition offset 0x1234, then procedure index 2 on top. */
    private static byte[] callWhenTwo() {
        return cat(pushInt(0x1234), pushInt(2), CALL_WHEN, STOP);
    }

    private static int[][] clear() {
        return new int[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };
    }

    @Test
    void callWhenSetsConditionalFlagAndConditionOffsetOnTheIndexedRow() throws Exception {
        Result r = run(callWhenTwo(), clear(), 32);

        assertEquals(0x80110008, r.flags);
        assertEquals("", r.stack);
        assertArray(new int[] { 0x02, 0, 0x1234 }, r.rows[2]);
        // Every other row, main included, is untouched.
        assertArray(new int[] { 0, 0, 0 }, r.rows[0]);
        assertArray(new int[] { 0, 0, 0 }, r.rows[1]);
        assertArray(new int[] { 0, 0, 0 }, r.rows[3]);
    }

    @Test
    void callWhenOrsTheFlagSoExistingFlagsSurvive() throws Exception {
        int[][] initial = clear();
        initial[2][0] = Program.PROCEDURE_FLAG_CRITICAL; // 0x10

        Result r = run(callWhenTwo(), initial, 32);

        // 0x10 | 0x02, not a replacement of 0x02.
        assertArray(new int[] { 0x12, 0, 0x1234 }, r.rows[2]);
    }

    @Test
    void cancelZeroesOneRowAndLeavesTheRestAlone() throws Exception {
        int[][] initial = {
            { 0x00, 0, 0 },
            { 0x01, 1000, 0 },
            { 0x02, 0, 0x1234 },
            { 0x03, 2000, 0x5678 },
        };

        Result r = run(cat(pushInt(3), CANCEL, STOP), initial, 32);

        assertEquals(0x80110008, r.flags);
        assertArray(new int[] { 0, 0, 0 }, r.rows[3]);
        assertArray(new int[] { 0x00, 0, 0 }, r.rows[0]);
        assertArray(new int[] { 0x01, 1000, 0 }, r.rows[1]);
        assertArray(new int[] { 0x02, 0, 0x1234 }, r.rows[2]);
    }

    @Test
    void cancelAllZeroesEveryRowIncludingMain() throws Exception {
        int[][] initial = {
            { 0x00, 0, 0 },
            { 0x01, 1000, 0 },
            { 0x02, 0, 0x1234 },
            { 0x03, 2000, 0x5678 },
        };

        Result r = run(cat(CANCEL_ALL, STOP), initial, 32);

        assertEquals(0x80110008, r.flags);
        for (int i = 0; i < PROCEDURE_COUNT; i++) {
            assertArray(new int[] { 0, 0, 0 }, r.rows[i]);
        }
    }

    @Test
    void cancelRejectsAnIndexEqualToProcedureCount() throws Exception {
        int[][] initial = {
            { 0x00, 0, 0 },
            { 0x01, 1000, 0 },
            { 0x02, 0, 0x1234 },
            { 0x03, 2000, 0x5678 },
        };

        Result r = run(cat(pushInt(4), CANCEL, STOP), initial, 32);

        // STOP_PROGRAM never ran: the opcode stays CANCEL (0x8048) in the high
        // half and the low half carries EXITED | FATAL_ERROR (0x05).
        assertEquals(0x80480005, r.flags);
        assertEquals("", r.stack);
        // The fatal fires before any write, so nothing was cleared.
        assertArray(new int[] { 0x03, 2000, 0x5678 }, r.rows[3]);
    }

    private static void assertArray(int[] expected, int[] actual) {
        assertEquals(expected[0], actual[0], "flags");
        assertEquals(expected[1], actual[1], "time");
        assertEquals(expected[2], actual[2], "conditionOffset");
    }
}
