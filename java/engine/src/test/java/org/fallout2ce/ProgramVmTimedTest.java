package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [vmtimed]} vectors from {@code docker/refharness/golden/v127.txt},
 * produced by {@code sectionVmTimed} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>{@code CALL_AT} and {@code WAIT}, the two opcodes that fold the
 * interpreter clock into a stored value.
 *
 * <p>{@code getInterpreterTime()} is {@code 1000 * getTicks() / 1000} over
 * unsigned 32-bit arithmetic, so it is the wall clock and its absolute value
 * cannot go into a golden file. What can be pinned is the relation:
 * <ul>
 *   <li>{@code CALL_AT} stores {@code 1000 * delay + now} in the procedure's
 *       time field. The vector records the delay term and checks that the
 *       embedded clock reading falls inside a bracket taken around the call -
 *       which still catches a dropped or doubled delay term.
 *   <li>{@code WAIT} stores {@code waitEnd = waitStart + duration}, so the
 *       SPAN is exact even though both endpoints are wall-clock.
 * </ul>
 *
 * <p>{@code checkWait} is inverted in C: it returns TRUE while the wait is
 * still in the future, and the VM loop keeps waiting only when it returns
 * FALSE. So a program with a positive duration does not wait at all, and one
 * with a negative duration - whose deadline is already in the past - spins on
 * the waiting state until its instruction budget runs out. Both are pinned.
 */
class ProgramVmTimedTest {

    private static final byte[] IDENTIFIERS =
        "main\0one".getBytes(StandardCharsets.ISO_8859_1);

    private static final byte[] STRINGS =
        "s".getBytes(StandardCharsets.ISO_8859_1);

    private static final int PROCEDURE_COUNT = 2;

    /** The delay CALL_AT is given, in seconds. */
    private static final int DELAY_SECONDS = 2;

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

    private static Image buildProgram(byte[] code) {
        int identifiersOffset = 46 + 24 * PROCEDURE_COUNT;
        int stringTableOffset = identifiersOffset + 4 + IDENTIFIERS.length;
        int codeOffset = stringTableOffset + 4 + STRINGS.length;
        int bodyOffset = codeOffset + code.length;

        byte[] data = new byte[1024];
        writeBE32(data, 42, PROCEDURE_COUNT);
        for (int i = 0; i < PROCEDURE_COUNT; i++) {
            int row = 46 + 24 * i;
            writeBE32(data, row + Program.PROCEDURE_NAME_OFFSET, 4 + i * 4);
            writeBE32(data, row + Program.PROCEDURE_FLAGS_OFFSET, 0);
            writeBE32(data, row + Program.PROCEDURE_TIME_OFFSET, 0);
            writeBE32(data, row + Program.PROCEDURE_CONDITION_OFFSET, -1);
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

    private static final class Result {
        int ip;
        int flags;
        String stack;
        int procFlags;
        int procTime;
        long waitSpan;
        boolean waiting;
        long t0;
        long t1;
    }

    private Result run(byte[] code) throws Exception {
        Image image = buildProgram(code);

        Path scriptPath = Files.createTempFile("vmtimed", ".int");
        Files.write(scriptPath, image.data);

        Program program = Program.programCreateByPath(scriptPath.toString());
        assertNotNull(program);
        program.flags = 0;
        program.instructionPointer = image.codeOffset;

        long t0 = Program.getInterpreterTime();
        Program.programInterpret(program, 32);
        long t1 = Program.getInterpreterTime();

        Result result = new Result();
        result.ip = program.instructionPointer;
        result.flags = program.flags;
        StringBuilder sb = new StringBuilder();
        for (int i = program.stackValues.size() - 1; i >= 0; i--) {
            Program.ProgramValue value = program.stackValues.get(i);
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%04X:%d", value.opcode & 0xFFFF, value.integerValue));
        }
        result.stack = sb.toString();
        int row = program.procedures + 4 + Program.PROCEDURE_SIZE * 1;
        result.procFlags = Program.stackReadInt32(program.data, row + Program.PROCEDURE_FLAGS_OFFSET);
        result.procTime = Program.stackReadInt32(program.data, row + Program.PROCEDURE_TIME_OFFSET);
        result.waitSpan = program.waitEnd - program.waitStart;
        result.waiting = (program.flags & Program.PROGRAM_IS_WAITING) != 0;

        // Bracketing the call, kept for the CALL_AT window check.
        result.t0 = t0;
        result.t1 = t1;

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

    private static final byte[] CALL_AT = { (byte) 0x80, (byte) 0x06 };
    private static final byte[] WAIT = { (byte) 0x80, (byte) 0x47 };
    private static final byte[] STOP = { (byte) 0x80, (byte) 0x11 };

    @Test
    void callAtStoresTheDelayTermPlusTheCurrentClock() throws Exception {
        // The procedure index is popped first, so it goes on top.
        Result r = run(cat(pushInt(DELAY_SECONDS), pushInt(1), CALL_AT, STOP));

        assertEquals(0x80110008, r.flags);
        assertEquals("", r.stack);
        // PROCEDURE_FLAG_TIMED is set on the scheduled row.
        assertEquals(Program.PROCEDURE_FLAG_TIMED, r.procFlags);
        // The stored time is 1000 * delay plus a clock reading taken during the
        // call, so the embedded reading has to fall inside the bracket.
        int delayTerm = 1000 * DELAY_SECONDS;
        long now = r.procTime - delayTerm;
        assertTrue(now >= r.t0 && now <= r.t1,
            "embedded clock reading " + now + " outside [" + r.t0 + ", " + r.t1 + "]");
    }

    @Test
    void waitWithAPositiveDurationDoesNotActuallyWait() throws Exception {
        Result r = run(cat(pushInt(5000), WAIT, pushInt(42), STOP));

        assertEquals(0x80110008, r.flags);
        // The program carried on to the push after the WAIT.
        assertEquals("C001:42", r.stack);
        // The span is exactly the requested duration.
        assertEquals(5000, r.waitSpan);
        // The waiting state was cleared again before the next instruction.
        assertTrue(!r.waiting);
    }

    @Test
    void waitWithANegativeDurationSpinsUntilTheBudgetRunsOut() throws Exception {
        Result r = run(cat(pushInt(-1), WAIT, pushInt(42), STOP));

        // The instruction pointer never advanced past the WAIT, and the
        // waiting bit is still set: the loop kept spinning on it. The high
        // half still holds the WAIT opcode (0x8047) because the loop never
        // fetched another one.
        assertEquals(0x80470000, r.flags & 0xFFFF0000);
        assertEquals(Program.PROGRAM_IS_WAITING, r.flags & 0xFFFF);
        assertTrue(r.waiting);
        assertEquals("", r.stack);
        assertEquals(-1, r.waitSpan);
    }
}
