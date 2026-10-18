package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [vmstack]} vectors from {@code docker/refharness/golden/v115.txt},
 * produced by {@code sectionVmStack} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The stack/frame opcodes, run on hand-built bytecode so the vectors pin
 * the exact cell order without needing a real {@code .int}:
 * <ul>
 * <li>{@code move} — PUSH int/float, POP, DUP, SWAP;</li>
 * <li>{@code pushfloat} — PUSH float, where the opcode is the value type
 * ({@code 0xA001}) and the immediate is the IEEE-754 bits;</li>
 * <li>{@code frame} — a value below the frame, two args, PUSH_BASE, two
 * locals, POP_TO_BASE (must stop at the frame) and POP_BASE;</li>
 * <li>{@code global} — D_TO_A / A_TO_D round-trip, two D_TO_A, SWAPA,
 * SET_GLOBAL;</li>
 * <li>{@code exit} — EXIT, which stamps EXITED and marks the program
 * exited.</li>
 * </ul>
 *
 * <p>The {@code frame} vector is the interesting one: PUSH_BASE computes
 * {@code frame = size - argumentCount} <em>after</em> popping the argument
 * count, so the frame lands on the first argument, not above it. A first
 * draft of the harness pushed only the args and got frame 0, which made
 * POP_TO_BASE clear the whole stack and fataled the next D_TO_A — the
 * vector exists to keep that convention pinned.
 */
class ProgramVmStackTest {

    private static final int OPCODE_BASE_OFFSET = 54;

    private Path scriptPath;

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path p = Paths.get(root, name);
        if (!Files.isRegularFile(p)) {
            throw new IllegalStateException("missing " + p);
        }
        return p.toString();
    }

    /** Writes a 256-byte program: header zeros, then the code blob at 54. */
    private void writeProgram(byte[] code) throws Exception {
        byte[] data = new byte[256];
        scriptPath = Files.createTempFile("vmstack", ".int");
        Files.write(scriptPath, data);
        try (RandomAccessFile raf = new RandomAccessFile(scriptPath.toFile(), "rw")) {
            raf.seek(OPCODE_BASE_OFFSET);
            raf.write(code);
        }
    }

    private static String dump(Deque<Program.ProgramValue> stack) {
        StringBuilder sb = new StringBuilder();
        Iterator<Program.ProgramValue> it = stack.descendingIterator();
        while (it.hasNext()) {
            Program.ProgramValue value = it.next();
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%04X:%d", value.opcode & 0xFFFF, value.integerValue));
        }
        return sb.toString();
    }

    private static final class Result {
        int ip;
        boolean exited;
        int flags;
        int frame;
        int base;
        String stack;
        String ret;
    }

    private Result run(byte[] code, int budget) throws Exception {
        writeProgram(code);
        Program program = Program.programCreateByPath(scriptPath.toString());
        assertNotNull(program);

        program.flags = 0;
        program.framePointer = -1;
        program.basePointer = -1;
        program.instructionPointer = OPCODE_BASE_OFFSET;
        Program.programInterpret(program, budget);

        Result result = new Result();
        result.ip = program.instructionPointer;
        result.exited = program.exited;
        result.flags = program.flags;
        result.frame = program.framePointer;
        result.base = program.basePointer;
        result.stack = dump(program.stackValues);
        result.ret = dump(program.returnStackValues);
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

    @Test
    void pushPopDupSwap() throws Exception {
        byte[] code = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x0B,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x16,
            (byte) 0xA0, (byte) 0x01, 0x3F, (byte) 0xC0, 0x00, 0x00,
            (byte) 0x80, 0x1A,
            (byte) 0x80, 0x1B,
            (byte) 0x80, 0x18,
            (byte) 0x80, 0x1A,
            (byte) 0x80, 0x11,
        };

        Result result = run(code, 32);

        assertEquals(82, result.ip);
        assertFalse(result.exited);
        assertEquals(0x80110008, result.flags);
        assertEquals(-1, result.frame);
        assertEquals(-1, result.base);
        assertEquals("C001:22 C001:11", result.stack);
        assertEquals("", result.ret);
    }

    @Test
    void pushFloatCarriesBits() throws Exception {
        byte[] code = {
            (byte) 0xA0, (byte) 0x01, 0x3F, (byte) 0xC0, 0x00, 0x00,
            (byte) 0x80, 0x11,
        };

        Result result = run(code, 32);

        // 1.5f = 0x3FC00000; the opcode is the type tag, not 0x8001.
        assertEquals("A001:1069547520", result.stack);
        assertEquals(0x80110008, result.flags);
    }

    @Test
    void pushBasePopToBasePopBase() throws Exception {
        byte[] code = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x64,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x06,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x02,
            (byte) 0x80, 0x2B,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x07,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x08,
            (byte) 0x80, 0x2A,
            (byte) 0x80, 0x29,
            (byte) 0x80, 0x11,
        };

        Result result = run(code, 32);

        // frame = 3 - 2 = 1, so POP_TO_BASE keeps the 100 below it.
        assertEquals(98, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals(-1, result.frame);
        assertEquals(-1, result.base);
        assertEquals("C001:100", result.stack);
        assertEquals("", result.ret);
    }

    @Test
    void returnStackRoundTripSwapAndSetGlobal() throws Exception {
        byte[] code = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x0B,
            (byte) 0x80, 0x0D,
            (byte) 0x80, 0x0C,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x16,
            (byte) 0x80, 0x0D,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x21,
            (byte) 0x80, 0x0D,
            (byte) 0x80, 0x19,
            (byte) 0x80, 0x2C,
            (byte) 0x80, 0x11,
        };

        Result result = run(code, 32);

        assertEquals(86, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals(-1, result.frame);
        assertEquals(1, result.base);
        assertEquals("C001:11", result.stack);
        // SWAPA exchanged the two return-stack cells.
        assertEquals("C001:22 C001:33", result.ret);
    }

    @Test
    void exitMarksProgramExited() throws Exception {
        byte[] code = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x2A,
            (byte) 0x80, 0x0E,
        };

        Result result = run(code, 32);

        assertEquals(62, result.ip);
        assertTrue(result.exited);
        assertEquals(0x800E0001, result.flags);
        assertTrue((result.flags & Program.PROGRAM_FLAG_EXITED) != 0);
        assertEquals("C001:42", result.stack);
    }

    @Test
    void gameDataPathExists() {
        // Keeps the F2J_GAMEDATA contract explicit even though this slice
        // does not read game data.
        assertTrue(Files.isDirectory(Paths.get(gameData("master.dat")).getParent()));
    }
}