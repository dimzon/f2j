package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [vmexternal]} vectors from {@code docker/refharness/golden/v126.txt},
 * produced by {@code sectionVmExternal} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The four "return from an external call" opcodes:
 * {@code POP_FLAGS_RETURN_EXTERN}, {@code POP_FLAGS_EXIT_EXTERN},
 * {@code POP_FLAGS_RETURN_VAL_EXTERN} and
 * {@code POP_FLAGS_RETURN_VAL_EXIT_EXTERN}. They restore the caller's state
 * from the callee's return stack, in the order caller, checkWaitFunc, flags,
 * returnAddress.
 *
 * <p>The return stack is built by hand here, in the order the consumers pop
 * it. That is deliberately NOT the order C's
 * {@code setupExternalCallWithReturnVal} pushes it: C pushes returnAddress
 * last, so the real producer leaves the consumers reading the wrong slots.
 * Since {@code PROCEDURE_FLAG_IMPORTED} is never set anywhere in the source,
 * the real path is dead, and a hand-built stack is the only way to exercise
 * the opcodes' own logic.
 *
 * <p>Two behaviours are easy to miss:
 * <ul>
 *   <li>{@code FINISHED} is scrubbed by programInterpret's epilogue
 *       ({@code program->flags &= ~PROGRAM_FLAG_FINISHED}), so the two
 *       "exit" variants are told apart by the instruction pointer rather than
 *       the flags: an opcode that sets FINISHED stops the loop before the
 *       STOP_PROGRAM behind it runs, one that does not resumes there.
 *   <li>{@code POP_FLAGS_RETURN_VAL_EXTERN} hands the value to the CALLER's
 *       stack, while {@code POP_FLAGS_RETURN_VAL_EXIT_EXTERN} pushes it back
 *       on the CALLEE's stack.
 * </ul>
 */
class ProgramVmExternalCallTest {

    private static final byte[] IDENTIFIERS =
        "main".getBytes(StandardCharsets.ISO_8859_1);

    private static final byte[] STRINGS =
        "s".getBytes(StandardCharsets.ISO_8859_1);

    /** The frame flags opPopFlags restores. Must carry no bit the VM loop
     *  breaks on, or the instruction pointer never gets to show the effect. */
    private static final int FRAME_FLAGS = Program.PROGRAM_FLAG_RUNNING;

    private static final int WINDOW_ID = 77;
    private static final int CALLER_FLAGS = 0x0123;
    private static final int CALLER_IP = 1234;
    private static final int CALLER_RESUME_IP = 2222;

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
        int identifiersOffset = 46 + 24;
        int stringTableOffset = identifiersOffset + 4 + IDENTIFIERS.length;
        int codeOffset = stringTableOffset + 4 + STRINGS.length;
        int bodyOffset = codeOffset + code.length;

        byte[] data = new byte[1024];
        writeBE32(data, 42, 1);
        writeBE32(data, 46 + 0, 4);
        writeBE32(data, 46 + 16, bodyOffset);
        writeBE32(data, 46 + 20, 0);

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
        int windowId;
        int calleeStack;
        int calleeReturnStack;
        int callerIp;
        int callerFlags;
        int callerStack;
    }

    private Result run(byte[] code, boolean withValue, int value) throws Exception {
        Image image = buildProgram(code);

        Path scriptPath = Files.createTempFile("vmexterncall", ".int");
        Files.write(scriptPath, image.data);

        Program callee = Program.programCreateByPath(scriptPath.toString());
        Program caller = Program.programCreateByPath(scriptPath.toString());
        assertNotNull(callee);
        assertNotNull(caller);
        callee.flags = 0;
        callee.instructionPointer = image.codeOffset;
        callee.windowId = 5;
        caller.flags = 0;
        caller.instructionPointer = CALLER_IP;
        caller.windowId = 9;

        // Callee value stack: [flags, checkWaitFunc, windowId] with windowId on
        // top, and the return value above that when there is one.
        Program.programStackPushInteger(callee, FRAME_FLAGS);
        Program.programStackPushPointer(callee, null);
        Program.programStackPushInteger(callee, WINDOW_ID);
        if (withValue) {
            Program.programStackPushInteger(callee, value);
        }

        // Callee return stack, in the order the consumers pop it. The return
        // address points at the STOP_PROGRAM behind the opcode, so an opcode
        // that does not set FINISHED resumes there and runs it.
        Program.programReturnStackPushInteger(callee, image.codeOffset + 2);
        Program.programReturnStackPushInteger(callee, CALLER_FLAGS);
        Program.programReturnStackPushPointer(callee, null);
        Program.programReturnStackPushPointer(callee, caller);

        // The caller's own return stack holds the address the caller resumes at.
        Program.programReturnStackPushInteger(caller, CALLER_RESUME_IP);

        Program.programInterpret(callee, 32);

        Result result = new Result();
        result.ip = callee.instructionPointer;
        result.flags = callee.flags;
        result.windowId = callee.windowId;
        result.calleeStack = callee.stackValues.size();
        result.calleeReturnStack = callee.returnStackValues.size();
        result.callerIp = caller.instructionPointer;
        result.callerFlags = caller.flags;
        result.callerStack = caller.stackValues.size();

        caller.child = null;
        callee.parent = null;
        Program.programFree(caller);
        Program.programFree(callee);
        return result;
    }

    @BeforeEach
    void setup() {
        Db.resetForTests();
        Config.resetLastSectionKey();
        XFile.resetForTests();
        Program.interpreterRegisterOpcodeHandlers();
        Export.resetForTests();
    }

    private static final byte[] POP_FLAGS_RETURN_EXTERN = { (byte) 0x80, (byte) 0x22 };
    private static final byte[] POP_FLAGS_EXIT_EXTERN = { (byte) 0x80, (byte) 0x23 };
    private static final byte[] POP_FLAGS_RETURN_VAL_EXTERN = { (byte) 0x80, (byte) 0x24 };
    private static final byte[] POP_FLAGS_RETURN_VAL_EXIT_EXTERN = { (byte) 0x80, (byte) 0x26 };
    private static final byte[] STOP = { (byte) 0x80, (byte) 0x11 };

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

    @Test
    void returnExternRestoresTheCallerAndResumesAtTheReturnAddress() throws Exception {
        Result r = run(cat(POP_FLAGS_RETURN_EXTERN, STOP), false, 0);

        // No FINISHED, so the loop resumes at the return address and runs the
        // STOP_PROGRAM behind it: the high half is the STOP opcode and the low
        // half is RUNNING | STOPPED.
        assertEquals(0x8011000A, r.flags);
        assertEquals(WINDOW_ID, r.windowId);
        assertEquals(0, r.calleeStack);
        assertEquals(0, r.calleeReturnStack);
        // The caller's flags came back from the return stack; its ip is
        // untouched by this variant.
        assertEquals(CALLER_FLAGS, r.callerFlags);
        assertEquals(CALLER_IP, r.callerIp);
        assertEquals(0, r.callerStack);
    }

    @Test
    void exitExternStopsAtTheReturnAddress() throws Exception {
        Result r = run(cat(POP_FLAGS_EXIT_EXTERN, STOP), false, 0);

        // FINISHED was set, so the loop breaks before the STOP_PROGRAM runs.
        // The epilogue then scrubs FINISHED, leaving just RUNNING.
        assertEquals(Program.PROGRAM_FLAG_RUNNING, r.flags);
        assertEquals(0, r.calleeStack);
        assertEquals(CALLER_FLAGS, r.callerFlags);
        assertEquals(CALLER_IP, r.callerIp);
    }

    @Test
    void returnValExternHandsTheValueToTheCaller() throws Exception {
        Result r = run(cat(POP_FLAGS_RETURN_VAL_EXTERN, STOP), true, 55);

        assertEquals(0x8011000A, r.flags);
        // The value landed on the CALLER's stack, and the caller's ip was set
        // from the caller's own return stack.
        assertEquals(1, r.callerStack);
        assertEquals(CALLER_RESUME_IP, r.callerIp);
        assertEquals(0, r.calleeStack);
        assertEquals(CALLER_FLAGS, r.callerFlags);
    }

    @Test
    void returnValExitExternPushesTheValueBackOnTheCallee() throws Exception {
        Result r = run(cat(POP_FLAGS_RETURN_VAL_EXIT_EXTERN, STOP), true, 66);

        assertEquals(Program.PROGRAM_FLAG_RUNNING, r.flags);
        // This variant puts the value back on the CALLEE's stack and leaves
        // the caller's ip alone.
        assertEquals(1, r.calleeStack);
        assertEquals(0, r.callerStack);
        assertEquals(CALLER_IP, r.callerIp);
    }
}
