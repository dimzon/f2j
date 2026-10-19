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
 * {@code [vmcall]} vectors from {@code docker/refharness/golden/v120.txt},
 * produced by {@code sectionVmCall} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>{@code programExecuteProcedure} on a one-procedure program. The top-level
 * code (bytes 0-41) is NOOPs except {@code POP, POP_FLAGS_EXIT} at offset 24 -
 * the fixed return address the compiler emits (verified against
 * {@code scripts\ABEL.int}). The procedure table is at 42, the body at 78.
 *
 * <p>{@code push-exit}: {@code PUSH int 5, POP_EXIT} returns to 24 with
 * {@code FINISHED} set; the value stack still holds the call frame plus 5.
 *
 * <p>{@code push-return}: {@code PUSH int 5, POP_RETURN} jumps back to 24
 * without stamping {@code FINISHED}, so the VM runs on into the top-level
 * {@code POP_FLAGS_EXIT} - which fatals. {@code programSetupCall} pushes
 * flags/checkWaitFunc/windowId plus one trailing zero for the callee's return
 * value, but {@code opPopFlags} pops only three: the windowId read takes the
 * trailing zero and the flags read lands on the {@code checkWaitFunc} PTR cell.
 * The resulting {@code programStackPopInteger} fault longjmps, stamping
 * {@code EXITED|FATAL_ERROR} on top of the already-fetched {@code POP_FLAGS_EXIT}
 * opcode ({@code 0x8021 << 16}) and leaving the IP at 28.
 */
class ProgramVmCallTest {

    private static final int OPCODE_BASE_OFFSET = 54;

    private static final int BODY_OFFSET = 78;

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

    /** Builds a 256-byte program: NOOP header, POP/POP_FLAGS_EXIT at 24, one
     *  procedure whose body starts at {@link #BODY_OFFSET}. */
    private static byte[] buildProgram(byte[] body) {
        byte[] data = new byte[256];
        // NOOP-filled top-level code, except POP/POP_FLAGS_EXIT at 24-27.
        for (int i = 0; i < 42; i += 2) {
            data[i] = (byte) 0x80;
            data[i + 1] = 0x00;
        }
        data[24] = (byte) 0x80;
        data[25] = (byte) 0x1A; // POP
        data[26] = (byte) 0x80;
        data[27] = (byte) 0x21; // POP_FLAGS_EXIT
        // procedure count = 1 at 42 (big-endian).
        data[45] = 1;
        // procedure body offset = 78 at 62-65 (big-endian).
        data[65] = (byte) BODY_OFFSET;
        System.arraycopy(body, 0, data, BODY_OFFSET, body.length);
        return data;
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
        String ret;
    }

    private Result run(byte[] body) throws Exception {
        byte[] data = buildProgram(body);

        scriptPath = Files.createTempFile("vmcall", ".int");
        Files.write(scriptPath, data);

        Program program = Program.programCreateByPath(scriptPath.toString());
        assertNotNull(program);

        Program.programExecuteProcedure(program, 0);

        Result result = new Result();
        result.ip = program.instructionPointer;
        result.flags = program.flags;
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
    void pushExit() throws Exception {
        byte[] body = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5
            (byte) 0x80, 0x1D, // POP_EXIT
        };
        Result result = run(body);
        assertEquals(24, result.ip);
        assertEquals(0x801D0000, result.flags);
        assertEquals("C001:5 C001:0 C001:0 E001:0 C001:0", result.stack);
        assertEquals("C001:0", result.ret);
    }

    @Test
    void pushReturn() throws Exception {
        byte[] body = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5
            (byte) 0x80, 0x1C, // POP_RETURN
        };
        Result result = run(body);
        assertEquals(28, result.ip);
        assertEquals(0x80210005, result.flags);
        assertEquals("C001:0", result.stack);
        assertEquals("C001:0", result.ret);
    }

    @Test
    void gameDataPathExists() {
        // Keeps the F2J_GAMEDATA contract explicit even though this slice
        // does not read game data.
        assertTrue(Files.isDirectory(Paths.get(gameData("master.dat")).getParent()));
    }
}