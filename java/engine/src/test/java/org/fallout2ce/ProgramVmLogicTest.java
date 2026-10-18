package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [vmlogic]} vectors from {@code docker/refharness/golden/v119.txt},
 * produced by {@code sectionVmLogic} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>AND and OR on ints and floats. The float cases read the raw
 * IEEE-754 bits (the union's integerValue view) masked with {@code 0x7FFFFFFF},
 * so {@code and-float} with 1.5 is true and {@code or-float} with 0.0 is false.
 */
class ProgramVmLogicTest {

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
    }

    private Result run(byte[] code, int budget) throws Exception {
        byte[] data = new byte[256];
        System.arraycopy(code, 0, data, OPCODE_BASE_OFFSET, code.length);

        scriptPath = Files.createTempFile("vmlogic", ".int");
        Files.write(scriptPath, data);

        Program program = Program.programCreateByPath(scriptPath.toString());
        assertNotNull(program);

        program.flags = 0;
        program.framePointer = -1;
        program.basePointer = -1;
        program.instructionPointer = OPCODE_BASE_OFFSET;
        Program.programInterpret(program, budget);

        Result result = new Result();
        result.ip = program.instructionPointer;
        result.flags = program.flags;
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

    @Test
    void andAndOrOnInts() throws Exception {
        byte[] andTrue = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x01,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x01,
            (byte) 0x80, 0x3E, // AND
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(andTrue, 32).stack);

        byte[] andFalse = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x01,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0x80, 0x3E, // AND
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:0", run(andFalse, 32).stack);

        byte[] orTrue = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x01,
            (byte) 0x80, 0x3F, // OR
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(orTrue, 32).stack);

        byte[] orFalse = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0x80, 0x3F, // OR
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:0", run(orFalse, 32).stack);
    }

    @Test
    void floatBitsQuirk() throws Exception {
        byte[] andFloat = {
            (byte) 0xA0, (byte) 0x01, 0x3F, (byte) 0xC0, 0x00, 0x00, // PUSH float 1.5
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1
            (byte) 0x80, 0x3E, // AND
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(andFloat, 32).stack);

        byte[] orFloat = {
            (byte) 0xA0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH float 0.0
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH int 0
            (byte) 0x80, 0x3F, // OR
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:0", run(orFloat, 32).stack);
    }

    @Test
    void gameDataPathExists() {
        // Keeps the F2J_GAMEDATA contract explicit even though this slice
        // does not read game data.
        assertTrue(Files.isDirectory(Paths.get(gameData("master.dat")).getParent()));
    }
}