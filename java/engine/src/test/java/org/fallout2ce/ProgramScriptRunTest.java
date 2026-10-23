package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [scriptrun]} vectors from {@code docker/refharness/golden/v128.txt},
 * produced by {@code sectionScriptRun} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Real game scripts, loaded straight out of {@code master.dat} through the
 * VFS and executed.
 *
 * <p>This is the first slice that runs bytecode the game's own compiler
 * produced, so it covers the whole pipeline rather than one opcode: the DAT
 * read, the file layout ({@code [top-level code][procedure count][procedure
 * table][identifiers][static strings]}), the procedure table, the identifier
 * table, and the real opcode mix.
 *
 * <p>The compiler's top-level prologue is
 * {@code ENTER_CRITICAL_SECTION; PUSH returnAddress; D_TO_A; PUSH bodyOffset;
 * JUMP; EXIT_PROGRAM} - {@code D_TO_A} moves the return address onto the
 * return stack and the {@code JUMP} enters the script's first procedure, so
 * the procedure really runs and the script really exits through
 * {@code EXIT_PROGRAM}.
 *
 * <p>The claim being pinned is parity, not completion. The game's intlib
 * registers its own script API as extra opcodes - the range 0x804C to 0x8281,
 * about 566 of them - and every one of those needs a running game. The core
 * interpreter opcodes are exactly 0x8000 to 0x804B, so the harness clears
 * every handler slot above 0x04B to take the game out of the picture, and
 * Java simply has no intlib at all.
 *
 * <p>Both engines therefore stop at the same place: the first opcode that
 * needs the game. That is what makes this a real comparison of the
 * interpreter rather than a test of a synthetic program - the DAT read, the
 * file layout, the procedure and identifier tables and the real opcode mix
 * the game's compiler emitted are all covered, up to the boundary where the
 * game takes over.
 */
class ProgramScriptRunTest {

    private static final int BUDGET = 200;

    private static final String[] SCRIPTS = {
        "scripts\\miJerky.int",
        "scripts\\CATH2MV.int",
        "scripts\\acdog.int",
        "scripts\\ABEL.int",
    };

    private static String gameData() {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        return root;
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

    private Result run(String path) {
        Program program = Program.programCreateByPath(path);
        assertNotNull(program, path);

        // programCreateByPath leaves the instruction pointer at 0, which is
        // where a script's top-level code starts.
        program.flags = 0;
        program.instructionPointer = 0;

        Program.programInterpret(program, BUDGET);

        Result result = new Result();
        result.ip = program.instructionPointer;
        result.flags = program.flags;
        result.stack = dump(program.stackValues);

        Program.programFree(program);
        return result;
    }

    @BeforeEach
    void setup() throws Exception {
        Db.resetForTests();
        Config.resetLastSectionKey();
        XFile.resetForTests();
        // The game's builtins are the API's, and the test pins the core
        // interpreter: it refuses every builtin, so the scripts stop at the
        // first opcode that needs the game.
        Program.setGameApi(new UnsupportedScriptApi());

        Path master = Paths.get(gameData(), "master.dat");
        assertTrue(Files.isRegularFile(master), master.toString());
        assertEquals(0, Db.dbOpen(master.toString(), null));
    }

    @Test
    void realScriptsStopExactlyWhereTheGameIsNeeded() {
        // The smallest script needs no game at all: it runs its one procedure
        // and leaves through EXIT_PROGRAM.
        Result miJerky = run("scripts\\miJerky.int");
        assertEquals(20, miJerky.ip);
        assertEquals(0x80100001, miJerky.flags);
        assertEquals("C001:0", miJerky.stack);

        // The rest call into the game. 0x80C7 is an intlib opcode, so both
        // engines stop there with EXITED | FATAL_ERROR.
        Result cath2mv = run("scripts\\CATH2MV.int");
        assertEquals(160, cath2mv.ip);
        assertEquals(0x80C70005, cath2mv.flags);
        assertEquals("C001:184", cath2mv.stack);

        Result acdog = run("scripts\\acdog.int");
        assertEquals(2310, acdog.ip);
        assertEquals(0x80C10005, acdog.flags);
        // 22 values: the script's globals, plus the one it had computed.
        assertEquals(22, acdog.stack.split(" ").length);
        assertTrue(acdog.stack.startsWith("C001:0 C001:2380 "), acdog.stack);
        assertTrue(acdog.stack.endsWith("C001:1 C001:0"), acdog.stack);

        Result abel = run("scripts\\ABEL.int");
        assertEquals(1264, abel.ip);
        assertEquals(0x80BC0005, abel.flags);
        assertEquals("C001:0 C001:0 C001:0", abel.stack);
    }
}
