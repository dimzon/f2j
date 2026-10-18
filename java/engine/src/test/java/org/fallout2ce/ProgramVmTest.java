package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [vm]} vectors from {@code docker/refharness/golden/v114.txt},
 * produced by {@code sectionVm} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Runs the VM loop itself: three {@code NOOP}s then {@code EXIT_PROGRAM}.
 * That is the smallest program that proves fetch, decode, dispatch and the
 * instruction budget all work — without dragging in the arithmetic opcodes.
 *
 * <p>Two things the vector pins that a signature-level test cannot:
 * <ul>
 * <li>the {@code flags} word keeps the last opcode in its top 16 bits
 * ({@code 0x80100001} = EXITED plus {@code EXIT_PROGRAM} << 16), so the
 * "clear low bits, store opcode" step cannot silently go missing;</li>
 * <li>a program already carrying {@code PROGRAM_FLAG_EXITED} is not run at
 * all — the IP has to stay put, which is what a port that checks the
 * exit flag only after the first step would get wrong.</li>
 * </ul>
 *
 * <p>The opcodes are written as explicit bytes, not {@code memcpy} of
 * {@code int16}: the host is little-endian while the VM reads big-endian, and
 * an {@code int16} copy would store {@code 0x8000} as bytes that decode
 * back as {@code 0x0080}.
 */
class ProgramVmTest {

    private static final int OPCODE_BASE_OFFSET = 54;

    private java.nio.file.Path scriptPath;

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

    /** Builds the probe script: three NOOPs then EXIT_PROGRAM. */
    private void writeProbeScript() throws Exception {
        byte[] data = new byte[64];
        // procedures count / identifiers count / strings count all zero, so the
        // loader lands the code region at byte 54 like C's offset arithmetic.
        scriptPath = Files.createTempFile("vm-probe", ".int");
        Files.write(scriptPath, data);
        byte[] ops = {
            (byte) 0x80, (byte) 0x00,
            (byte) 0x80, (byte) 0x00,
            (byte) 0x80, (byte) 0x00,
            (byte) 0x80, (byte) 0x10,
        };
        try (java.io.RandomAccessFile raf =
                new java.io.RandomAccessFile(scriptPath.toFile(), "rw")) {
            raf.seek(OPCODE_BASE_OFFSET);
            raf.write(ops);
        }
    }

    @BeforeEach
    void setup() throws Exception {
        Db.resetForTests();
        Config.resetLastSectionKey();
        XFile.resetForTests();
        Program.interpreterRegisterOpcodeHandlers();
        writeProbeScript();
    }

    private Program load() {
        Program program = Program.programCreateByPath(scriptPath.toString());
        assertNotNull(program);
        return program;
    }

    @Test
    void loopRunsAndStopsOnExitProgram() {
        Program program = load();
        assertEquals(0, program.procedureCount());

        program.flags = 0;
        program.instructionPointer = OPCODE_BASE_OFFSET;
        Program.programInterpret(program, 4);

        // Three NOOPs advance 2 bytes each, EXIT_PROGRAM advances 2 more.
        assertEquals(OPCODE_BASE_OFFSET + 8, program.instructionPointer);
        assertFalse(program.exited);

        // EXITED | (EXIT_PROGRAM << 16).
        assertEquals(0x80100001, program.flags);
        assertTrue((program.flags & Program.PROGRAM_FLAG_EXITED) != 0);
    }

    @Test
    void alreadyExitedProgramDoesNotRun() {
        Program program = load();
        program.flags = Program.PROGRAM_FLAG_EXITED;
        program.instructionPointer = OPCODE_BASE_OFFSET;
        Program.programInterpret(program, 4);

        // The loop breaks on the flag check before fetching anything.
        assertEquals(OPCODE_BASE_OFFSET, program.instructionPointer);
        assertEquals(Program.PROGRAM_FLAG_EXITED, program.flags);
    }

    @Test
    void budgetStopsBeforeExit() {
        Program program = load();
        program.flags = 0;
        program.instructionPointer = OPCODE_BASE_OFFSET;
        // Two steps: both NOOPs, EXIT_PROGRAM never reached.
        Program.programInterpret(program, 2);

        assertEquals(OPCODE_BASE_OFFSET + 4, program.instructionPointer);
        // The loop only *tests* PROGRAM_FLAG_STOPPED, it never sets it: after
        // two NOOPs the word is just the last opcode in the top 16 bits.
        assertEquals(0x8000 << 16, program.flags);
        assertEquals(0, program.flags & Program.PROGRAM_FLAG_EXITED);
    }

    @Test
    void undefinedOpcodeRaisesFatalError() {
        Program program = load();
        program.flags = 0;
        program.instructionPointer = OPCODE_BASE_OFFSET;
        // 0x0080 has bit 7 of the (opcode >> 8) byte clear, which C treats as
        // a bad opcode rather than an undefined one.
        program.data[OPCODE_BASE_OFFSET] = 0;
        program.data[OPCODE_BASE_OFFSET + 1] = (byte) 0x80;
        program.instructionPointer = OPCODE_BASE_OFFSET;

        // Guard the premise: the opcode really decodes to the bad value.
        assertEquals(0x0080, Program.stackReadInt16(program.data, OPCODE_BASE_OFFSET));

        // programFatalError is C's longjmp out of the VM, and the port keeps
        // that shape by catching it inside programInterpret - so the effect
        // lands in the flags rather than propagating to the caller. The word
        // also keeps the bad opcode in its top 16 bits, exactly like a normal
        // step: the store happens before the validity check.
        Program.programInterpret(program, 4);

        assertEquals(0x00800005, program.flags);
        assertTrue((program.flags & Program.PROGRAM_FLAG_FATAL_ERROR) != 0);
        assertTrue((program.flags & Program.PROGRAM_FLAG_EXITED) != 0);
    }

    @Test
    void gameDataPathExists() {
        // Keeps the F2J_GAMEDATA contract explicit even though this slice
        // does not read game data.
        assertTrue(Files.isDirectory(Paths.get(gameData("master.dat")).getParent()));
    }
}