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
 * {@code [vmglobals]} vectors from {@code docker/refharness/golden/v122.txt},
 * produced by {@code sectionVmGlobals} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Global-variable opcodes ({@code FETCH_GLOBAL}, {@code STORE_GLOBAL}) and
 * {@code DUMP}, on a self-contained program that needs no call frame.
 *
 * <p>The addressing is the thing worth pinning down. {@code opSetGlobal} is
 * {@code basePointer = stackValues.size()}, so a global lives at
 * {@code basePointer + addr} and the only in-range addresses are
 * {@code -(size)..-1}: the values that were already on the stack when
 * {@code SET_GLOBAL} ran. Globals are therefore addressed by <em>negative</em>
 * offsets, while locals are positive offsets from {@code framePointer}. The
 * vector pushes three values, snapshots, then addresses them as -1 and -3;
 * anything positive runs {@code stackValues->at()} off the end and aborts C with
 * an uncaught {@code std::vector::at} throw.
 */
class ProgramVmGlobalsTest {

    private static final byte[] IDENTIFIERS =
        "proc".getBytes(StandardCharsets.ISO_8859_1);

    private static final byte[] STRINGS =
        "str".getBytes(StandardCharsets.ISO_8859_1);

    private static void writeBE32(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 24);
        data[offset + 1] = (byte) (value >> 16);
        data[offset + 2] = (byte) (value >> 8);
        data[offset + 3] = (byte) value;
    }

    /** The image plus the offset the top-level code starts at. */
    private static final class Image {
        byte[] data;
        int codeOffset;
    }

    /** Mirrors {@code vmGlobalsRun}: one procedure row, identifier blob,
     *  static-string table, then the top-level code. */
    private static Image buildProgram(byte[] code) {
        int identifiersOffset = 46 + 24;
        int stringTableOffset = identifiersOffset + 4 + IDENTIFIERS.length;
        int codeOffset = stringTableOffset + 4 + STRINGS.length;
        int bodyOffset = codeOffset + code.length;

        byte[] data = new byte[1024];
        writeBE32(data, 42, 1);
        writeBE32(data, 46 + 0, 4);  // nameOffset
        writeBE32(data, 46 + 4, 0);  // flags
        writeBE32(data, 46 + 8, 0);  // time
        writeBE32(data, 46 + 12, -1); // conditionOffset
        writeBE32(data, 46 + 16, bodyOffset);
        writeBE32(data, 46 + 20, 0); // argCount

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
        int base;
        String stack;
    }

    private Result run(byte[] code, int budget) throws Exception {
        Image image = buildProgram(code);

        Path scriptPath = Files.createTempFile("vmglobals", ".int");
        Files.write(scriptPath, image.data);

        Program program = Program.programCreateByPath(scriptPath.toString());
        assertTrue(program != null);
        program.flags = 0;
        program.instructionPointer = image.codeOffset;

        Program.programInterpret(program, budget);

        Result result = new Result();
        result.ip = program.instructionPointer;
        result.flags = program.flags;
        result.base = program.basePointer;
        result.stack = dump(program.stackValues);

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

    private static final byte[] SET_GLOBAL = { (byte) 0x80, (byte) 0x2C };
    private static final byte[] STORE_GLOBAL = { (byte) 0x80, (byte) 0x13 };
    private static final byte[] FETCH_GLOBAL = { (byte) 0x80, (byte) 0x12 };
    private static final byte[] DUMP = { (byte) 0x80, (byte) 0x2E };
    private static final byte[] STOP = { (byte) 0x80, (byte) 0x11 };

    @Test
    void storeGlobalWritesThroughBasePointerAndFetchReadsItBack() throws Exception {
        Result r = run(cat(
            pushInt(10), pushInt(20), pushInt(30),
            SET_GLOBAL,
            pushInt(99), pushInt(-1), STORE_GLOBAL,
            pushInt(-1), FETCH_GLOBAL,
            STOP), 32);

        assertEquals(3, r.base);
        assertEquals(0x80110008, r.flags);
        assertEquals("C001:99 C001:99 C001:20 C001:10", r.stack);
    }

    @Test
    void dumpDiscardsTheTopNValues() throws Exception {
        Result r = run(cat(
            pushInt(10), pushInt(20), pushInt(30),
            SET_GLOBAL,
            pushInt(99), pushInt(100),
            pushInt(2), DUMP,
            STOP), 32);

        assertEquals(3, r.base);
        assertEquals(0x80110008, r.flags);
        assertEquals("C001:30 C001:20 C001:10", r.stack);
    }

    @Test
    void operandsPushedAfterSetGlobalDoNotShiftTheGlobals() throws Exception {
        Result r = run(cat(
            pushInt(10), pushInt(20), pushInt(30),
            SET_GLOBAL,
            pushInt(99), pushInt(-1), STORE_GLOBAL,
            pushInt(-3), FETCH_GLOBAL,
            STOP), 32);

        assertEquals(3, r.base);
        assertEquals(0x80110008, r.flags);
        assertEquals("C001:10 C001:99 C001:20 C001:10", r.stack);
    }

    @Test
    void dumpWithCountZeroLeavesTheStackAlone() throws Exception {
        Result r = run(cat(
            pushInt(7),
            pushInt(0), DUMP,
            STOP), 32);

        assertEquals(0x80110008, r.flags);
        assertEquals("C001:7", r.stack);
    }
}
