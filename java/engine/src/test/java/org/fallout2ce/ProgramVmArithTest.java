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
 * {@code [vmarith]} vectors from {@code docker/refharness/golden/v116.txt},
 * produced by {@code sectionVmArith} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The arithmetic, bitwise and logical operators, each on hand-built
 * bytecode leaving one result on the stack: ADD (int overflow promoting to
 * float, int, float, and string + int concatenation), SUB, MUL, DIV (int and
 * float), MOD, NEGATE (int and float), FLOOR, BITWISE_AND/OR/XOR/NOT and NOT.
 *
 * <p>Two conventions the vectors pin:
 * <ul>
 * <li>ADD's int + int overflow check: the sum stays int only when it fits,
 * otherwise it promotes to float (the vector's 2000000000 + 2000000000
 * becomes 4000000000.00000, not a wrapped int);</li>
 * <li>PUSH's type is the opcode itself, so the float result prints as
 * {@code A001} and the concatenated string as {@code 9801} (dynamic).</li>
 * </ul>
 */
class ProgramVmArithTest {

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

    private Result run(String staticString, byte[] code, int budget) throws Exception {
        byte[] data = new byte[256];
        int codeOffset = OPCODE_BASE_OFFSET;
        if (staticString != null) {
            byte[] text = staticString.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            System.arraycopy(text, 0, data, OPCODE_BASE_OFFSET, text.length);
            data[OPCODE_BASE_OFFSET + text.length] = 0;
            codeOffset = OPCODE_BASE_OFFSET + text.length + 1;
        }
        System.arraycopy(code, 0, data, codeOffset, code.length);

        scriptPath = Files.createTempFile("vmarith", ".int");
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
    void addIntOverflowPromotesToFloat() throws Exception {
        byte[] code = {
            (byte) 0xC0, (byte) 0x01, 0x77, 0x35, (byte) 0x94, 0x00,
            (byte) 0xC0, (byte) 0x01, 0x77, 0x35, (byte) 0x94, 0x00,
            (byte) 0x80, 0x39,
            (byte) 0x80, 0x11,
        };
        Result result = run(null, code, 32);
        assertEquals(70, result.ip);
        assertEquals(0x80110008, result.flags);
        assertEquals("A001:4000000000.00000", result.stack);
    }

    @Test
    void addIntStaysInt() throws Exception {
        byte[] code = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x02,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x03,
            (byte) 0x80, 0x39,
            (byte) 0x80, 0x11,
        };
        Result result = run(null, code, 32);
        assertEquals("C001:5", result.stack);
    }

    @Test
    void addFloat() throws Exception {
        byte[] code = {
            (byte) 0xA0, (byte) 0x01, 0x3F, (byte) 0xC0, 0x00, 0x00,
            (byte) 0xA0, (byte) 0x01, 0x40, 0x10, 0x00, 0x00,
            (byte) 0x80, 0x39,
            (byte) 0x80, 0x11,
        };
        Result result = run(null, code, 32);
        assertEquals("A001:3.75000", result.stack);
    }

    @Test
    void addStringIntConcatenates() throws Exception {
        byte[] code = {
            (byte) 0x90, (byte) 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0x80, 0x39,
            (byte) 0x80, 0x11,
        };
        Result result = run("ab", code, 32);
        assertEquals(73, result.ip);
        assertEquals("9801:ab5", result.stack);
    }

    @Test
    void subtractMultiplyDivideModulo() throws Exception {
        byte[] sub = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x0A,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x03,
            (byte) 0x80, 0x3A,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:7", run(null, sub, 32).stack);

        byte[] mul = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x06,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x07,
            (byte) 0x80, 0x3B,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:42", run(null, mul, 32).stack);

        byte[] div = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x14,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x04,
            (byte) 0x80, 0x3C,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:5", run(null, div, 32).stack);

        byte[] divFloat = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x0A,
            (byte) 0xA0, (byte) 0x01, 0x40, (byte) 0x80, 0x00, 0x00,
            (byte) 0x80, 0x3C,
            (byte) 0x80, 0x11,
        };
        assertEquals("A001:2.50000", run(null, divFloat, 32).stack);

        byte[] mod = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x11,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0x80, 0x3D,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:2", run(null, mod, 32).stack);
    }

    @Test
    void negateFloorBitwiseAndNot() throws Exception {
        byte[] negateInt = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x07,
            (byte) 0x80, 0x46,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:-7", run(null, negateInt, 32).stack);

        byte[] negateFloat = {
            (byte) 0xA0, (byte) 0x01, 0x3F, (byte) 0xC0, 0x00, 0x00,
            (byte) 0x80, 0x46,
            (byte) 0x80, 0x11,
        };
        assertEquals("A001:-1.50000", run(null, negateFloat, 32).stack);

        byte[] floor = {
            (byte) 0xA0, (byte) 0x01, 0x40, 0x70, 0x00, 0x00,
            (byte) 0x80, 0x44,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:3", run(null, floor, 32).stack);

        byte[] bitAnd = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x0F,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x3C,
            (byte) 0x80, 0x40,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:12", run(null, bitAnd, 32).stack);

        byte[] bitOr = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x0F,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x30,
            (byte) 0x80, 0x41,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:63", run(null, bitOr, 32).stack);

        byte[] bitXor = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x0F,
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x3C,
            (byte) 0x80, 0x42,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:51", run(null, bitXor, 32).stack);

        byte[] bitNot = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0x80, 0x43,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:-1", run(null, bitNot, 32).stack);

        byte[] logicalNot = {
            (byte) 0xC0, (byte) 0x01, 0x00, 0x00, 0x00, 0x05,
            (byte) 0x80, 0x45,
            (byte) 0x80, 0x11,
        };
        assertEquals("C001:0", run(null, logicalNot, 32).stack);
    }

    @Test
    void gameDataPathExists() {
        // Keeps the F2J_GAMEDATA contract explicit even though this slice
        // does not read game data.
        assertTrue(Files.isDirectory(Paths.get(gameData("master.dat")).getParent()));
    }
}