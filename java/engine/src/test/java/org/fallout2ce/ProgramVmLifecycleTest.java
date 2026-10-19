package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [vmlifecycle]} vectors from {@code docker/refharness/golden/v124.txt},
 * produced by {@code sectionVmLifecycle} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>{@code CALLSTART}, {@code SPAWN}, {@code FORK} and {@code EXEC}. All four
 * reach {@code runScript}, which C routes through {@code interpreterFilenameMangler}
 * - the default {@code defaultFilename} is the identity function, so the popped
 * string is a literal path and the harness aims it at a child script it writes.
 *
 * <p>Pushing the script name takes a trick worth spelling out. There is no
 * push-string opcode: {@code programGetNextOpcode} reads a 16-bit opcode, the
 * VM insists on bit {@code 0x80} of the high byte and dispatches on
 * {@code opcode & 0x3FF}, and {@code opPush} copies <em>that opcode</em> into
 * the pushed value's type field. The opcode's high byte therefore <em>is</em> the
 * value type, which is why {@code VALUE_TYPE_INT} is {@code 0xC001} and
 * {@code VALUE_TYPE_STRING} is {@code 0x9001}: both mask to index {@code 0x001}
 * and both land on {@code opPush}. {@code 0x9001} carries
 * {@code RAW_VALUE_TYPE_STATIC_STRING}, so {@code programGetString} resolves the
 * operand as an offset into the static string table.
 */
class ProgramVmLifecycleTest {

    private static final byte[] IDENTIFIERS =
        "main".getBytes(StandardCharsets.ISO_8859_1);

    private static final String MISSING_PATH = "/tmp/vmlifecycle-missing.int";

    private static final int WINDOW_ID = 77;

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

    /** Static strings hold the child path at offset 0 and {@link #MISSING_PATH}
     *  straight after it, matching {@code vmLifeRun}. */
    private static byte[] staticStrings(String childPath) {
        byte[] child = childPath.getBytes(StandardCharsets.ISO_8859_1);
        byte[] missing = MISSING_PATH.getBytes(StandardCharsets.ISO_8859_1);
        byte[] out = new byte[child.length + 1 + missing.length + 1];
        System.arraycopy(child, 0, out, 0, child.length);
        System.arraycopy(missing, 0, out, child.length + 1, missing.length);
        return out;
    }

    private static Image buildProgram(byte[] code, String childPath) {
        byte[] strings = staticStrings(childPath);
        int identifiersOffset = 46 + 24;
        int stringTableOffset = identifiersOffset + 4 + IDENTIFIERS.length;
        int codeOffset = stringTableOffset + 4 + strings.length;
        int bodyOffset = codeOffset + code.length;

        byte[] data = new byte[1024];
        writeBE32(data, 42, 1);
        writeBE32(data, 46 + 0, 4);
        writeBE32(data, 46 + 16, bodyOffset);
        writeBE32(data, 46 + 20, 0);

        writeBE32(data, identifiersOffset, IDENTIFIERS.length);
        System.arraycopy(IDENTIFIERS, 0, data, identifiersOffset + 4, IDENTIFIERS.length);
        writeBE32(data, stringTableOffset, strings.length);
        System.arraycopy(strings, 0, data, stringTableOffset + 4, strings.length);
        System.arraycopy(code, 0, data, codeOffset, code.length);

        Image image = new Image();
        image.data = data;
        image.codeOffset = codeOffset;
        return image;
    }

    /** Writes the child script the lifecycle opcodes will load. Its bodyOffset
     *  points at zero bytes (NOOP), so runScript's 24-instruction head start
     *  does nothing and returns. */
    private static String writeChildScript() throws Exception {
        Path childPath = Files.createTempFile("vmlifecycle-child", ".int");
        byte[] child = new byte[1024];
        int childProc = 46;
        int childBody = 46 + 24 + 4 + IDENTIFIERS.length;
        writeBE32(child, 42, 1);
        writeBE32(child, childProc + 0, 4);
        writeBE32(child, childProc + 16, childBody);
        Files.write(childPath, child);
        return childPath.toString();
    }

    private static final class Result {
        int flags;
        int windowId;
        Program program;
        Program parent;
    }

    private Result run(byte[] code, String childPath, boolean attachParent) throws Exception {
        Image image = buildProgram(code, childPath);

        Path scriptPath = Files.createTempFile("vmlifecycle", ".int");
        Files.write(scriptPath, image.data);

        Program program = Program.programCreateByPath(scriptPath.toString());
        assertNotNull(program);
        program.flags = 0;
        program.instructionPointer = image.codeOffset;
        program.windowId = WINDOW_ID;

        Program parent = null;
        if (attachParent) {
            parent = Program.programCreateByPath(scriptPath.toString());
            parent.flags = Program.PROGRAM_FLAG_CHILD_CALL | Program.PROGRAM_FLAG_CHILD_SPAWN;
            parent.child = program;
            program.parent = parent;
        }

        Program.programInterpret(program, 32);

        Result result = new Result();
        result.flags = program.flags;
        result.windowId = program.windowId;
        result.program = program;
        result.parent = parent;
        return result;
    }

    @BeforeEach
    void setup() {
        Db.resetForTests();
        Config.resetLastSectionKey();
        XFile.resetForTests();
        Program.interpreterRegisterOpcodeHandlers();
    }

    private static byte[] pushStaticString(int offset) {
        return new byte[] {
            (byte) 0x90, (byte) 0x01,
            (byte) (offset >> 24), (byte) (offset >> 16),
            (byte) (offset >> 8), (byte) offset,
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

    private static final byte[] CALLSTART = { (byte) 0x80, (byte) 0x08 };
    private static final byte[] EXEC = { (byte) 0x80, (byte) 0x09 };
    private static final byte[] SPAWN = { (byte) 0x80, (byte) 0x0A };
    private static final byte[] FORK = { (byte) 0x80, (byte) 0x0B };
    private static final byte[] STOP = { (byte) 0x80, (byte) 0x11 };

    private static void release(Result r) {
        if (r.program.child != null) {
            Program child = r.program.child;
            r.program.child = null;
            child.parent = null;
            Program.programFree(child);
        }
        if (r.parent != null) {
            r.program.parent = null;
            Program.programFree(r.parent);
        }
        Program.programFree(r.program);
    }

    @Test
    void callstartRunsAChildAndKeepsItAttached() throws Exception {
        String childPath = writeChildScript();
        int childString = 0;

        Result r = run(cat(pushStaticString(childString), CALLSTART, STOP), childPath, false);
        try {
            // CHILD_CALL (0x20) in the low half, CALLSTART (0x8008) high.
            assertEquals(0x80080020, r.flags);
            assertNotNull(r.program.child);
            assertSame(r.program, r.program.child.parent);
            assertEquals(WINDOW_ID, r.program.child.windowId);
            assertTrue(!r.program.exited);
        } finally {
            release(r);
        }
    }

    @Test
    void spawnSetsTheChildSpawnFlagRatherThanChildCall() throws Exception {
        String childPath = writeChildScript();

        Result r = run(cat(pushStaticString(0), SPAWN, STOP), childPath, false);
        try {
            // CHILD_SPAWN is 0x0100, not CHILD_CALL 0x20.
            assertEquals(0x800A0100, r.flags);
            assertNotNull(r.program.child);
            assertSame(r.program, r.program.child.parent);
        } finally {
            release(r);
        }
    }

    @Test
    void forkDeliberatelyDiscardsItsFork() throws Exception {
        String childPath = writeChildScript();

        Result r = run(cat(pushStaticString(0), FORK, STOP), childPath, false);
        try {
            // No child link survives; the fork is dropped on the floor.
            assertNull(r.program.child);
            assertEquals(0x80110008, r.flags);
        } finally {
            release(r);
        }
    }

    @Test
    void execHandsTheParentTheForkAndExitsThisProgram() throws Exception {
        String childPath = writeChildScript();

        Result r = run(cat(pushStaticString(0), EXEC, STOP), childPath, true);
        try {
            // EXITED (0x01), and STOP_PROGRAM never ran - the purge breaks the
            // loop, so the high half still holds the EXEC opcode.
            assertEquals(0x80090001, r.flags);
            assertTrue(r.program.exited);
            assertNull(r.program.parent);
            assertNotNull(r.parent.child);
            assertSame(r.parent, r.parent.child.parent);
            assertNull(r.parent.child.child);
        } finally {
            release(r);
        }
    }

    @Test
    void forkOfAMissingScriptIsFatalBeforeAnyFlagChanges() throws Exception {
        String childPath = writeChildScript();
        int missingString = childPath.length() + 1;

        Result r = run(cat(pushStaticString(missingString), FORK, STOP), childPath, false);
        try {
            assertEquals(0x800B0005, r.flags);
            assertNull(r.program.child);
            assertTrue(!r.program.exited);
        } finally {
            release(r);
        }
    }

    @Test
    void callstartKeepsChildCallSetWhenTheScriptIsMissing() throws Exception {
        String childPath = writeChildScript();
        int missingString = childPath.length() + 1;

        Result r = run(cat(pushStaticString(missingString), CALLSTART, STOP), childPath, false);
        try {
            // CHILD_CALL is set BEFORE the name is popped, so it survives the
            // fatal: 0x20 | EXITED | FATAL_ERROR = 0x25.
            assertEquals(0x80080025, r.flags);
            assertNull(r.program.child);
        } finally {
            release(r);
        }
    }

    @Test
    void missingPathIsAbsentSoRunScriptReturnsNull() {
        assertTrue(!Files.exists(Paths.get(MISSING_PATH)));
    }
}
