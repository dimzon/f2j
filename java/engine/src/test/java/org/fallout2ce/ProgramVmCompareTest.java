package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [vmcompare]} vectors from {@code docker/refharness/golden/v117.txt},
 * produced by {@code sectionVmCompare} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The six comparison operators on ints, floats, mixed int/float and static
 * strings. Each leaves one 0/1 result.
 *
 * <p>The operators share one dispatch: pop two values, switch on the first's
 * type, then the second's. Strings compare with strcmp semantics, ints and
 * floats numerically. The pointer cases (not exercised here) have per-operator
 * null-test special cases; see {@code Program.opCompare}.
 */
class ProgramVmCompareTest {

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

    private static String dump(Program program, List<Program.ProgramValue> stack) {
        StringBuilder sb = new StringBuilder();
        for (int i = stack.size() - 1; i >= 0; i--) {
            Program.ProgramValue value = stack.get(i);
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (value.opcode == Program.VALUE_TYPE_FLOAT) {
                sb.append(String.format(Locale.ROOT, "A001:%.5f", value.floatValue));
            } else if (value.opcode == Program.VALUE_TYPE_DYNAMIC_STRING) {
                sb.append("9801:").append(Program.programGetString(program, value.opcode, value.integerValue));
            } else {
                sb.append(String.format("%04X:%d", value.opcode & 0xFFFF, value.integerValue));
            }
        }
        return sb.toString();
    }

    private static final class Result {
        int ip;
        int flags;
        String stack;
    }

    private Result run(byte[] code, int budget) throws Exception {
        return runTable(null, code, budget);
    }

    private Result runTable(byte[] stringTable, byte[] code, int budget) throws Exception {
        byte[] data = new byte[256];
        int codeOffset = OPCODE_BASE_OFFSET;
        if (stringTable != null) {
            System.arraycopy(stringTable, 0, data, OPCODE_BASE_OFFSET, stringTable.length);
            codeOffset = OPCODE_BASE_OFFSET + stringTable.length;
        }
        System.arraycopy(code, 0, data, codeOffset, code.length);

        scriptPath = Files.createTempFile("vmcompare", ".int");
        Files.write(scriptPath, data);

        Program program = Program.programCreateByPath(scriptPath.toString());
        assertNotNull(program);

        program.flags = 0;
        program.framePointer = -1;
        program.basePointer = -1;
        program.instructionPointer = codeOffset;
        Program.programInterpret(program, budget);

        Result result = new Result();
        result.ip = program.instructionPointer;
        result.flags = program.flags;
        result.stack = dump(program, program.stackValues);
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
    void intComparisons() throws Exception {
        byte[] eq = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0x80, 0x33,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(eq, 32).stack);

        byte[] eqNe = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x06,
            (byte) 0x80, 0x33,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:0", run(eqNe, 32).stack);

        byte[] ne = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x06,
            (byte) 0x80, 0x34,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(ne, 32).stack);

        byte[] lt = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x03,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0x80, 0x37,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(lt, 32).stack);

        byte[] le = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0x80, 0x35,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(le, 32).stack);

        byte[] ge = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0x80, 0x36,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(ge, 32).stack);

        byte[] gt = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x06,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0x80, 0x38,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(gt, 32).stack);
    }

    @Test
    void floatAndMixedComparisons() throws Exception {
        byte[] eqFloat = {
            (byte) 0xA0, (byte) 0x01, 0x3F, (byte) 0xC0, 0x00, 0x00,
            (byte) 0xA0, (byte) 0x01, 0x3F, (byte) 0xC0, 0x00, 0x00,
            (byte) 0x80, 0x33,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(eqFloat, 32).stack);

        byte[] ltFloat = {
            (byte) 0xA0, (byte) 0x01, 0x3F, (byte) 0xC0, 0x00, 0x00,
            (byte) 0xA0, (byte) 0x01, 0x40, 0x20, 0x00, 0x00,
            (byte) 0x80, 0x37,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(ltFloat, 32).stack);

        byte[] eqIntFloat = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x02,
            (byte) 0xA0, (byte) 0x01, 0x40, 0x00, 0x00, 0x00,
            (byte) 0x80, 0x33,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", run(eqIntFloat, 32).stack);
    }

    @Test
    void stringComparisons() throws Exception {
        // "ab\0cd\0": offset 0 is "ab", offset 3 is "cd".
        byte[] stringTable = { 'a', 'b', 0, 'c', 'd', 0 };

        byte[] eqStr = {
            (byte) 0x90, (byte) 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0x90, (byte) 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0x80, 0x33,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", runTable(stringTable, eqStr, 32).stack);

        byte[] neStr = {
            (byte) 0x90, (byte) 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0x90, (byte) 0x01, 0x00, 0x00, 0x00, 0x03,
            (byte) 0x80, 0x34,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:1", runTable(stringTable, neStr, 32).stack);

        byte[] gtStr = {
            (byte) 0x90, (byte) 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0x90, (byte) 0x01, 0x00, 0x00, 0x00, 0x03,
            (byte) 0x80, 0x38,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:0", runTable(stringTable, gtStr, 32).stack);
    }

    @Test
    void gameDataPathExists() {
        // Keeps the F2J_GAMEDATA contract explicit even though this slice
        // does not read game data.
        assertTrue(Files.isDirectory(Paths.get(gameData("master.dat")).getParent()));
    }
}