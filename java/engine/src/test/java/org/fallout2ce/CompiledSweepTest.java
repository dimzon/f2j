package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The compiled code against the whole game, not only the reference scripts.
 *
 * <p>Every script the game ships is run twice: through the interpreter and
 * through the compiled code, both against a mock. The two must make the same
 * calls to the game and leave the same stack behind.
 *
 * <p>One script is known to differ, and the reason is in the notes of
 * the port:
 *
 * <ul>
 * <li>{@code KRUPPER.int} returns to an address that is not the return
 * address of the call. The interpreter jumps there; the compiled code has no
 * way to jump to an address it does not know when it is written.
 * </ul>
 */
class CompiledSweepTest {

    /** The scripts that are known to differ, and why. */
    private static final List<String> KNOWN = List.of(
        "scripts\\KRUPPER.int");

    /** A run of a script. */
    private static final class Run {
        MockScriptApi mock;
        String stack;
    }

    /** Runs a script through the interpreter with a mock behind it. */
    private static Run runWithTheVm(String name) {
        Export.clear();
        MockScriptApi mock = new MockScriptApi();
        Program.setGameApi(mock);
        Program program = Program.programCreateByPath(name);
        if (program == null) {
            return null;
        }
        program.flags = 0;
        program.instructionPointer = 0;
        Program.programInterpret(program, 20000);
        Run run = new Run();
        run.mock = mock;
        StringBuilder out = new StringBuilder();
        for (Program.ProgramValue value : program.stackValues) {
            out.append(String.format("%04X:%d ", value.opcode & 0xFFFF, value.integerValue));
        }
        run.stack = out.toString();
        Program.programFree(program);
        return run;
    }

    /** The calls the mock recorded, as one string. */
    private static String trace(MockScriptApi mock) {
        StringBuilder out = new StringBuilder();
        for (MockScriptApi.Call call : mock.calls()) {
            out.append(call.name).append('(');
            for (int i = 0; i < call.args.size(); i++) {
                if (i > 0) {
                    out.append(", ");
                }
                out.append(call.args.get(i));
            }
            out.append(")\n");
        }
        return out.toString();
    }

    /** The stack of a compiled script. */
    private static String stack(List<Program.ProgramValue> values) {
        StringBuilder out = new StringBuilder();
        for (Program.ProgramValue value : values) {
            out.append(String.format("%04X:%d ", value.opcode & 0xFFFF, value.integerValue));
        }
        return out.toString();
    }

    /** The class loader that defines the compiled scripts. */
    private static final class ScriptLoader extends ClassLoader {
        ScriptLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    @Test
    void theCompiledCodeDoesWhatTheGameDoes() throws Exception {
        String root = System.getenv("F2J_GAMEDATA");
        Db.resetForTests();
        assertEquals(0, Db.dbOpen(Paths.get(root, "master.dat").toString(), null));

        // The API and the registry of the external variables are global,
        // and the other tests expect them the way they found them.
        GameApi api = Program.gameApi();

        XFile.XList xlist = new XFile.XList();
        XFile.xlistInit("*.int", xlist);

        int same = 0;
        List<String> different = new ArrayList<>();
        for (String name : xlist.fileNames) {
            byte[] data = ScriptScanner.readScript(name);
            if (data == null) {
                continue;
            }
            ScriptCompiler.CompiledScript script = ScriptCompiler.compile(name, data);
            ScriptEmitter.Emitted emitted = ScriptEmitter.emit(script);
            assertTrue(emitted.problems.isEmpty(), name + ": " + emitted.problems);

            Run vm = runWithTheVm(name);
            if (vm == null) {
                continue;
            }

            // The registry is shared, so the compiled code starts from the
            // same state the interpreter found.
            Export.clear();

            Class<?> type = new ScriptLoader(CompiledSweepTest.class.getClassLoader())
                .define(emitted.className.replace('/', '.'), emitted.bytes);
            MockScriptApi compiled = new MockScriptApi();
            ScriptBase instance = (ScriptBase) type.getConstructor(GameApi.class, String.class,
                String.class).newInstance(compiled, name, script.staticStrings);
            instance.run();

            if (trace(vm.mock).equals(trace(compiled)) && vm.stack.equals(stack(instance.stack))) {
                same++;
            } else {
                different.add(name);
            }
        }
        Db.dbCloseAll();
        Program.setGameApi(api);
        Export.clear();

        List<String> unexpected = new ArrayList<>(different);
        unexpected.removeAll(KNOWN);
        System.out.println("scripts=" + xlist.fileNames.size() + " same=" + same
            + " different=" + different.size());
        for (String name : different) {
            System.out.println("  " + name);
        }
        assertTrue(unexpected.isEmpty(), "the compiled code differs: " + unexpected);
        assertTrue(same > xlist.fileNames.size() * 9 / 10,
            "only " + same + " of " + xlist.fileNames.size() + " scripts agree");
    }
}
