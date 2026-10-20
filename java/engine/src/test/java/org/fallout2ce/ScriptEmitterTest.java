package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The emitter: the script compiler's code turned into Java, and the Java
 * turned into a class.
 *
 * <p>The point of the compiler is that a script can be run without the
 * interpreter: the code is there, the branches are jumps and the game's
 * builtins are calls on the game API. So the test runs the same script two
 * ways - through the VM and through the compiled code - against the same
 * mock, and the two must agree.
 */
class ScriptEmitterTest {

    private static String gameData() {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        return root;
    }

    @BeforeEach
    void setup() {
        GameGlobalVars.reset();
        Db.resetForTests();
        Config.resetLastSectionKey();
        XFile.resetForTests();
        Program.interpreterRegisterOpcodeHandlers();
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

    /** What a run of a script left behind. */
    private static final class Run {
        MockScriptApi mock;
        String stack;
    }

    /** Runs a script through the VM with a mock behind it. */
    private Run runWithTheVm(String name) throws Exception {
        MockScriptApi mock = new MockScriptApi();
        Program.setGameApi(mock);

        assertNotNull(ScriptScanner.readScript(name), name);
        Program program = Program.programCreateByPath(name);
        assertNotNull(program);
        program.flags = 0;
        program.instructionPointer = 0;
        Program.programInterpret(program, 20000);

        Run run = new Run();
        run.mock = mock;
        run.stack = stack(program.stackValues);

        Program.programFree(program);
        return run;
    }

    /** Compiles a script into a class. */
    private static Class<?> compile(ScriptEmitter.Emitted emitted) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "there is no java compiler");

        Path directory = Files.createTempDirectory("f2j-script");
        Path file = directory.resolve(emitted.className + ".java");
        Files.writeString(file, emitted.source);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> units = manager.getJavaFileObjects(file.toFile());
            JavaCompiler.CompilationTask task = compiler.getTask(null, manager, diagnostics,
                List.of("-d", directory.toString()), null, units);
            assertTrue(task.call(), "the generated code does not compile: "
                + diagnostics.getDiagnostics() + "\n" + emitted.source);
        }

        URL url = directory.toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[] { url },
                ScriptEmitterTest.class.getClassLoader())) {
            return loader.loadClass("org.fallout2ce." + emitted.className);
        }
    }

    @Test
    void aCompiledScriptDoesWhatTheScriptItCameFromDoes() throws Exception {
        String name = "scripts\\miJerky.int";

        // The game's archive: the script comes out of it.
        Path master = Paths.get(gameData(), "master.dat");
        assertTrue(master.toFile().isFile());
        assertEquals(0, Db.dbOpen(master.toString(), null));

        // The script, compiled.
        byte[] data = ScriptScanner.readScript(name);
        assertNotNull(data, name);
        ScriptCompiler.CompiledScript script = ScriptCompiler.compile(name, data);
        assertTrue(script.problems.isEmpty(), script.problems.toString());
        assertTrue(script.analysisProblems.isEmpty(), script.analysisProblems.toString());

        ScriptEmitter.Emitted emitted = ScriptEmitter.emit(script);
        assertTrue(emitted.problems.isEmpty(), emitted.problems.toString());

        // The script, through the VM.
        Run vm = runWithTheVm(name);

        // The script, through the compiled code.
        Class<?> type = compile(emitted);
        MockScriptApi compiled = new MockScriptApi();
        ScriptBase instance = (ScriptBase) type.getConstructor(GameApi.class).newInstance(compiled);
        instance.run();

        assertEquals(trace(vm.mock), trace(compiled));
        assertEquals(vm.stack, stack(instance.stack));

        Db.dbCloseAll();
    }

    /**
     * The reference scripts, run through the VM and through the compiled
     * code.
     *
     * <p>They are the scripts the reference harness runs, so they are the
     * ones whose behaviour is known. The compiled code has to do the same
     * thing: the same calls to the game, in the same order, and the same
     * stack at the end.
     */
    private static final String[] REFERENCE_SCRIPTS = {
        "scripts\\miJerky.int",
        "scripts\\CATH2MV.int",
        "scripts\\acdog.int",
        "scripts\\ABEL.int",
        "scripts\\doorcode.int",
        "scripts\\CODEDISK.int",
        "scripts\\ARTIFACT.int",
        "scripts\\MutFruit.int",
        "scripts\\DOG2.int",
        "scripts\\VSGTEAM9.int",
        "scripts\\JONATHAN.int",
        "scripts\\FREEZER.int",
        "scripts\\GRAFFITI.int",
        "scripts\\BVAIR.int",
        "scripts\\ziclosed.int",
        "scripts\\cowbomb.int",
        "scripts\\CSAREA2.int",
    };

    /** The stack of a program, the way the reference writes it. */
    private static String stack(List<Program.ProgramValue> values) {
        StringBuilder out = new StringBuilder();
        for (int i = values.size() - 1; i >= 0; i--) {
            Program.ProgramValue value = values.get(i);
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(String.format("%04X:%d", value.opcode & 0xFFFF, value.integerValue));
        }
        return out.toString();
    }

    @Test
    void everyReferenceScriptBehavesTheSameCompiled() throws Exception {
        Path master = Paths.get(gameData(), "master.dat");
        assertTrue(master.toFile().isFile());
        assertEquals(0, Db.dbOpen(master.toString(), null));

        for (String name : REFERENCE_SCRIPTS) {
            // The script, compiled.
            byte[] data = ScriptScanner.readScript(name);
            assertNotNull(data, name);
            ScriptCompiler.CompiledScript script = ScriptCompiler.compile(name, data);
            assertTrue(script.problems.isEmpty(), script.problems.toString());
            assertTrue(script.analysisProblems.isEmpty(), script.analysisProblems.toString());
            ScriptEmitter.Emitted emitted = ScriptEmitter.emit(script);
            assertTrue(emitted.problems.isEmpty(), emitted.problems.toString());
            Class<?> type = compile(emitted);

            // The script, through the VM.
            Run vm = runWithTheVm(name);

            // The script, through the compiled code.
            MockScriptApi compiled = new MockScriptApi();
            ScriptBase instance = (ScriptBase) type.getConstructor(GameApi.class)
                .newInstance(compiled);

            instance.run();
            assertEquals(trace(vm.mock), trace(compiled), name);
            assertEquals(vm.stack, stack(instance.stack), name);
        }

        Db.dbCloseAll();
    }

    @Test
    void everyReferenceScriptBehavesTheSameCompiledAgainstTheGame() throws Exception {
        Path master = Paths.get(gameData(), "master.dat");
        assertTrue(master.toFile().isFile());
        assertEquals(0, Db.dbOpen(master.toString(), null));

        for (String name : REFERENCE_SCRIPTS) {
            // The script, compiled.
            byte[] data = ScriptScanner.readScript(name);
            assertNotNull(data, name);
            ScriptCompiler.CompiledScript script = ScriptCompiler.compile(name, data);
            ScriptEmitter.Emitted emitted = ScriptEmitter.emit(script);
            Class<?> type = compile(emitted);

            // The script, through the VM and the game's own API.
            GameScriptApi game = new GameScriptApi();
            Program.setGameApi(game);
            Program program = Program.programCreateByPath(name);
            assertNotNull(program);
            program.flags = 0;
            program.instructionPointer = 0;
            Program.programInterpret(program, 20000);
            String vmStack = stack(program.stackValues);
            Program.programFree(program);

            // The script, through the compiled code and the game's API.
            ScriptBase instance = (ScriptBase) type.getConstructor(GameApi.class)
                .newInstance(new GameScriptApi());
            instance.run();

            assertEquals(vmStack, stack(instance.stack), name);
        }

        Db.dbCloseAll();
    }

    @Test
    void theEmitterCoversTheShippedScripts() {
        // How much of the game's scripts the emitter can turn into code.
        Path master = Paths.get(gameData(), "master.dat");
        assertTrue(master.toFile().isFile());
        assertEquals(0, Db.dbOpen(master.toString(), null));

        XFile.XList xlist = new XFile.XList();
        XFile.xlistInit("*.int", xlist);

        int emitted = 0;
        int withProblems = 0;
        List<String> reasons = new ArrayList<>();
        for (String name : xlist.fileNames) {
            byte[] data = ScriptScanner.readScript(name);
            if (data == null) {
                continue;
            }
            ScriptCompiler.CompiledScript script = ScriptCompiler.compile(name, data);
            ScriptEmitter.Emitted result = ScriptEmitter.emit(script);
            if (result.problems.isEmpty()) {
                emitted++;
            } else {
                withProblems++;
                for (String problem : result.problems) {
                    reasons.add(problem);
                }
            }
        }
        Db.dbCloseAll();

        System.out.println("scripts=" + xlist.fileNames.size() + " emitted=" + emitted
            + " with problems=" + withProblems);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String reason : reasons) {
            String key = reason.substring(reason.indexOf(": ") + 2);
            int at = key.indexOf(" at ");
            if (at > 0) {
                key = key.substring(0, at);
            }
            counts.merge(key, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            System.out.println("  " + entry.getValue() + " " + entry.getKey());
        }

        assertTrue(emitted > 0, "the emitter produced nothing");
    }
}
