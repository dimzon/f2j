package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [vmproc]} vectors from {@code docker/refharness/golden/v121.txt},
 * produced by {@code sectionVmProc} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Procedure metadata and critical-section opcodes, on a program with a real
 * procedure table, identifier blob and static-string table. Four procedures:
 * {@code main} (0 args), {@code alpha} (2 args), {@code beta} (1 arg) and a
 * second {@code beta} (3 args).
 *
 * <p>Identifier offsets are measured from the block's own big-endian length
 * field - {@code programGetIdentifier} is {@code identifiers + offset} with no
 * {@code +4}, unlike {@code programGetString}'s {@code staticStrings + 4 +
 * offset}. Verified against {@code scripts\gl_highlighting.int} (15 procedures,
 * identifiers length 538 at 406, proc0 nameOffset 6 -&gt; {@code "........"}).
 * With {@code "main\0alpha\0beta"} laid out after the length field the
 * nameOffsets are 4, 9 and 15.
 */
class ProgramVmProcTest {

    private static final int PROCEDURES_BASE_OFFSET = 42;

    private static final int PROCEDURE_SIZE = 24;

    /** Field offsets inside a {@code Procedure} row. */
    private static final int NAME_OFFSET = 0;
    private static final int FLAGS_OFFSET = 4;
    private static final int CONDITION_OFFSET = 12;
    private static final int BODY_OFFSET_FIELD = 16;
    private static final int ARG_COUNT_OFFSET = 20;

    private static final int PROCEDURE_COUNT = 4;

    // C's sizeof on a string literal counts the implicit trailing NUL, which
    // Java's getBytes() does not append - so add it explicitly to keep the
    // identifiers / static-string block lengths (and therefore every offset
    // derived from them) identical to the harness.
    private static final byte[] IDENTIFIERS =
        concat("main\0alpha\0beta".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), new byte[] { 0 });

    private static final byte[] STRINGS =
        concat("beta\0gamma\0BETA".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), new byte[] { 0 });

    /** nameOffset / flags / argCount, matching the harness VmProc table. */
    private static final int[][] PROCS = {
        { 4, 0x00, 0 },
        { 9, 0x00, 2 },
        { 15, 0x00, 1 },
        { 15, 0x00, 3 },
    };

    private static String gameData(String name) {
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

    private static void writeBE32(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 24);
        data[offset + 1] = (byte) (value >> 16);
        data[offset + 2] = (byte) (value >> 8);
        data[offset + 3] = (byte) value;
    }

    /** The image plus the offset the top-level code starts at (programInterpret
     *  runs from {@code instructionPointer}, which the harness sets explicitly;
     *  C's programCreateByPath leaves it at 0). */
    private static final class Image {
        byte[] data;
        int codeOffset;
    }

    /** Lays out the image exactly like {@code vmProcRun}: procedure table,
     *  identifiers blob, static-string table, then the top-level code. */
    private static Image buildProgram(byte[] code) {
        int identifiersOffset = 46 + PROCEDURE_SIZE * PROCEDURE_COUNT;
        int stringTableOffset = identifiersOffset + 4 + IDENTIFIERS.length;
        int codeOffset = stringTableOffset + 4 + STRINGS.length;
        int bodyOffset = codeOffset + code.length;

        byte[] data = new byte[1024];
        writeBE32(data, PROCEDURES_BASE_OFFSET, PROCEDURE_COUNT);
        for (int i = 0; i < PROCEDURE_COUNT; i++) {
            int row = 46 + PROCEDURE_SIZE * i;
            writeBE32(data, row + NAME_OFFSET, PROCS[i][0]);
            writeBE32(data, row + FLAGS_OFFSET, PROCS[i][1]);
            writeBE32(data, row + 8, 0); // time
            writeBE32(data, row + CONDITION_OFFSET, -1);
            writeBE32(data, row + BODY_OFFSET_FIELD, bodyOffset);
            writeBE32(data, row + ARG_COUNT_OFFSET, PROCS[i][2]);
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
    }

    private Result run(byte[] code, boolean attachParent) throws Exception {
        Image image = buildProgram(code);

        Path scriptPath = Files.createTempFile("vmproc", ".int");
        Files.write(scriptPath, image.data);

        Program program = Program.programCreateByPath(scriptPath.toString());
        assertTrue(program != null);
        program.flags = 0;
        program.instructionPointer = image.codeOffset;

        Program parent = null;
        if (attachParent) {
            parent = Program.programCreateByPath(scriptPath.toString());
            parent.flags = Program.PROGRAM_FLAG_CHILD_CALL | Program.PROGRAM_FLAG_CHILD_SPAWN;
            parent.child = program;
            program.parent = parent;
        }

        Program.programInterpret(program, 32);

        Result result = new Result();
        result.ip = program.instructionPointer;
        result.flags = program.flags;
        result.stack = dump(program.stackValues);

        if (parent != null) {
            assertEquals(0, parent.flags & (Program.PROGRAM_FLAG_CHILD_CALL | Program.PROGRAM_FLAG_CHILD_SPAWN));
            assertTrue(parent.child == null);
            parent.child = null;
            program.parent = null;
            Program.programFree(parent);
        }
        Program.programFree(program);
        return result;
    }

    private Result run(byte[] code) throws Exception {
        return run(code, false);
    }

    @BeforeEach
    void setup() {
        Db.resetForTests();
        Config.resetLastSectionKey();
        XFile.resetForTests();
        Program.interpreterRegisterOpcodeHandlers();
    }

    private static byte[] pushInt(int value) {
        return new byte[] { (byte) 0xC0, 0x01, 0, 0, 0, (byte) value };
    }

    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] out = new byte[length];
        int pos = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, pos, part.length);
            pos += part.length;
        }
        return out;
    }

    private static final byte[] ENTER_CRITICAL = { (byte) 0x80, 0x02 };
    private static final byte[] LEAVE_CRITICAL = { (byte) 0x80, 0x03 };
    private static final byte[] START_CRITICAL = { (byte) 0x80, 0x4A };
    private static final byte[] END_CRITICAL = { (byte) 0x80, 0x4B };
    private static final byte[] STOP_PROGRAM = { (byte) 0x80, 0x11 };
    private static final byte[] DETACH = { (byte) 0x80, 0x0F };

    /** The 0x80 CRITICAL_SECTION bit survives the per-step flag rewrite. */
    @Test
    void criticalEnter() throws Exception {
        Result result = run(concat(ENTER_CRITICAL, pushInt(42), STOP_PROGRAM));
        assertEquals(192, result.ip);
        assertEquals(0x80110088, result.flags);
        assertEquals("C001:42", result.stack);
    }

    @Test
    void criticalLeave() throws Exception {
        Result result = run(concat(ENTER_CRITICAL, LEAVE_CRITICAL, pushInt(42), STOP_PROGRAM));
        assertEquals(194, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals("C001:42", result.stack);
    }

    @Test
    void criticalStartEnd() throws Exception {
        Result result = run(concat(START_CRITICAL, pushInt(42), END_CRITICAL, pushInt(43), STOP_PROGRAM));
        assertEquals(200, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals("C001:43 C001:42", result.stack);
    }

    @Test
    void fetchProcedureAddress() throws Exception {
        Result result = run(concat(pushInt(2), new byte[] { (byte) 0x80, 0x2D }, STOP_PROGRAM));
        assertEquals(192, result.ip);
        assertEquals(0x80110008, result.flags);
        // Every row points at the same body, just past the code.
        assertEquals("C001:192", result.stack);
    }

    @Test
    void checkArgumentCountOk() throws Exception {
        Result result = run(concat(pushInt(1), pushInt(2), new byte[] { (byte) 0x80, 0x27 }, pushInt(7), STOP_PROGRAM));
        assertEquals(204, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals("C001:7", result.stack);
    }

    /** Mismatch: the fatal names the procedure, so the IP stays past the
     *  already-fetched opcode and EXITED|FATAL_ERROR lands on top of it. */
    @Test
    void checkArgumentCountBad() throws Exception {
        Result result = run(concat(pushInt(1), pushInt(9), new byte[] { (byte) 0x80, 0x27 }, STOP_PROGRAM));
        assertEquals(196, result.ip);
        assertEquals(0x80270005, result.flags);
        assertEquals("", result.stack);
    }

    @Test
    void lookupByName() throws Exception {
        Result result = run(concat(pushString(0), new byte[] { (byte) 0x80, 0x28 }, STOP_PROGRAM));
        assertEquals(192, result.ip);
        assertEquals(0x80110008, result.flags);
        // First match wins even though procedure 3 repeats the name.
        assertEquals("C001:2", result.stack);
    }

    /** The compare is SDL_strcasecmp, so the case variant resolves the same. */
    @Test
    void lookupByNameCaseInsensitive() throws Exception {
        Result result = run(concat(pushString(11), new byte[] { (byte) 0x80, 0x28 }, STOP_PROGRAM));
        assertEquals(192, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals("C001:2", result.stack);
    }

    @Test
    void lookupByNameMiss() throws Exception {
        Result result = run(concat(pushString(5), new byte[] { (byte) 0x80, 0x28 }, STOP_PROGRAM));
        assertEquals(190, result.ip);
        assertEquals(0x80280005, result.flags);
        assertEquals("", result.stack);
    }

    @Test
    void detachWithoutParent() throws Exception {
        Result result = run(concat(DETACH, pushInt(42), STOP_PROGRAM));
        assertEquals(192, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals("C001:42", result.stack);
    }

    @Test
    void detachWithParent() throws Exception {
        Result result = run(concat(DETACH, pushInt(42), STOP_PROGRAM), true);
        assertEquals(192, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals("C001:42", result.stack);
    }

    private static byte[] pushString(int offset) {
        return new byte[] { (byte) 0x90, 0x01, 0, 0, 0, (byte) offset };
    }

    /** Runs code on a frame seeded the way programSetupCallWithReturnVal
     *  builds it: one resume address on the return stack, then
     *  flags / checkWaitFunc / windowId bottom-up. */
    private Result runOnFrame(byte[] code, int resumeAddress) throws Exception {
        // programInterpret reads from instructionPointer, so put the code at
        // offset 54 like the other VM sections.
        byte[] data = new byte[256];
        System.arraycopy(code, 0, data, 54, code.length);
        Path scriptPath = Files.createTempFile("vmretval", ".int");
        Files.write(scriptPath, data);

        Program program = Program.programCreateByPath(scriptPath.toString());
        assertTrue(program != null);
        program.flags = 0;
        program.instructionPointer = 54;

        Program.programReturnStackPushInteger(program, resumeAddress);
        Program.programStackPushInteger(program, program.flags & 0xFFFF);
        Program.programStackPushPointer(program, program.checkWaitFunc);
        Program.programStackPushInteger(program, program.windowId);

        Program.programInterpret(program, -1);

        Result result = new Result();
        result.ip = program.instructionPointer;
        result.flags = program.flags;
        result.stack = dump(program.stackValues);
        Program.programFree(program);
        return result;
    }

    /** POP_FLAGS_RETURN_VAL_EXIT pops its value before restoring the frame,
     *  so the value is already gone when opPopFlags reads the three frame
     *  cells and it lands back on the caller's stack. This is the frame shape
     *  [vmcall]'s push-return shows programSetupCall breaking. */
    @Test
    void popFlagsReturnValExit() throws Exception {
        Result result = runOnFrame(concat(pushInt(77), new byte[] { (byte) 0x80, 0x25 }), 200);
        assertEquals(200, result.ip);
        assertEquals(0x00000000, result.flags);
        assertEquals("C001:77", result.stack);
    }

    /** POP_FLAGS_EXIT reads the three frame cells directly, so the code must
     *  not leave a value on top of them. */
    @Test
    void popFlagsExitOnSeededFrame() throws Exception {
        Result result = runOnFrame(new byte[] { (byte) 0x80, 0x21 }, 200);
        assertEquals(200, result.ip);
        assertEquals(0x00000000, result.flags);
        assertEquals("", result.stack);
    }

    @Test
    void gameDataPathExists() {
        // Keeps the F2J_GAMEDATA contract explicit even though this slice
        // does not read game data.
        assertTrue(Files.isDirectory(Paths.get(gameData("master.dat")).getParent()));
    }
}