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
 * {@code [vmcontrol]} vectors from {@code docker/refharness/golden/v118.txt},
 * produced by {@code sectionVmControl} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>JUMP, IF, WHILE, STORE and FETCH on hand-built bytecode. Two stack-order
 * facts the vectors pin:
 * <ul>
 * <li>IF and WHILE pop the <em>condition</em> from the top, so the jump target
 * is pushed first. A first draft pushed the condition first and IF treated the
 * target as the condition, so a falsy condition did not jump;</li>
 * <li>PUSH_BASE computes {@code frame = size - argumentCount} after popping the
 * count, so STORE/FETCH index from the first argument, and a value below the
 * frame is needed for the frame to be non-zero.</li>
 * </ul>
 *
 * <p>WHILE's truthy path leaves the target on the stack (C's behavior), which
 * the {@code while-true} vector records.
 */
class ProgramVmControlTest {

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
        int frame;
        int base;
        String stack;
    }

    private Result run(byte[] code, int budget) throws Exception {
        byte[] data = new byte[256];
        System.arraycopy(code, 0, data, OPCODE_BASE_OFFSET, code.length);

        scriptPath = Files.createTempFile("vmcontrol", ".int");
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
        result.frame = program.framePointer;
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

    @Test
    void jumpSkipsCode() throws Exception {
        byte[] code = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x46, // PUSH int 70
            (byte) 0x80, 0x04, // JUMP
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1 (skipped)
            (byte) 0x80, 0x11, // STOP_PROGRAM
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (target)
            (byte) 0x80, 0x11, // STOP_PROGRAM
        };
        Result result = run(code, 32);
        assertEquals(78, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals("C001:2", result.stack);
    }

    @Test
    void ifBranchesOnCondition() throws Exception {
        byte[] ifTrue = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x4C, // PUSH int 76 (target)
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5 (condition)
            (byte) 0x80, 0x2F, // IF
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1
            (byte) 0x80, 0x11, // STOP_PROGRAM
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (target)
            (byte) 0x80, 0x11, // STOP_PROGRAM
        };
        Result truthy = run(ifTrue, 32);
        assertEquals(76, truthy.ip);
        assertEquals("C001:1", truthy.stack);

        byte[] ifFalse = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x4C, // PUSH int 76 (target)
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH int 0 (condition)
            (byte) 0x80, 0x2F, // IF
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1 (skipped)
            (byte) 0x80, 0x11, // STOP_PROGRAM
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (target)
            (byte) 0x80, 0x11, // STOP_PROGRAM
        };
        Result falsy = run(ifFalse, 32);
        assertEquals(84, falsy.ip);
        assertEquals("C001:2", falsy.stack);
    }

    @Test
    void whileBranchesOnCondition() throws Exception {
        byte[] whileTrue = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x46, // PUSH int 70 (target)
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1 (condition)
            (byte) 0x80, 0x30, // WHILE
            (byte) 0x80, 0x11, // STOP_PROGRAM
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (target)
            (byte) 0x80, 0x11, // STOP_PROGRAM
        };
        Result truthy = run(whileTrue, 32);
        // The target stays on the stack on the truthy path.
        assertEquals(70, truthy.ip);
        assertEquals("C001:70", truthy.stack);

        byte[] whileFalse = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x46, // PUSH int 70 (target)
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH int 0 (condition)
            (byte) 0x80, 0x30, // WHILE
            (byte) 0x80, 0x11, // STOP_PROGRAM
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (target)
            (byte) 0x80, 0x11, // STOP_PROGRAM
        };
        Result falsy = run(whileFalse, 32);
        assertEquals(78, falsy.ip);
        assertEquals("C001:2", falsy.stack);
    }

    @Test
    void storeAndFetchUseFrame() throws Exception {
        byte[] code = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x64, // PUSH int 100 (below frame)
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5 (arg0)
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x06, // PUSH int 6 (arg1)
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (nargs)
            (byte) 0x80, 0x2B, // PUSH_BASE (frame = 3 - 2 = 1)
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x09, // PUSH int 9
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH int 0 (addr)
            (byte) 0x80, 0x31, // STORE
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH int 0 (addr)
            (byte) 0x80, 0x32, // FETCH
            (byte) 0x80, 0x11, // STOP_PROGRAM
        };
        Result result = run(code, 32);
        assertEquals(104, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals(1, result.frame);
        assertEquals(-1, result.base);
        assertEquals("C001:9 C001:6 C001:9 C001:100", result.stack);
    }

    @Test
    void gameDataPathExists() {
        // Keeps the F2J_GAMEDATA contract explicit even though this slice
        // does not read game data.
        assertTrue(Files.isDirectory(Paths.get(gameData("master.dat")).getParent()));
    }
}