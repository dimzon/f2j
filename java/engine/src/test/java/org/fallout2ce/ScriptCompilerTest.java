package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The script compiler's front end: it turns a shipped script into the shape
 * of a program - the code, the stack and the branches - which the emitter
 * then turns into JVM code.
 *
 * <p>The decoder is settled: every script in the game decodes without a
 * problem. The analysis - the stack and the branches - is not, and it says
 * so: what it cannot follow it reports instead of guessing.
 */
class ScriptCompilerTest {

    private static String gameData() {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        return root;
    }

    @BeforeEach
    void setup() {
        Db.resetForTests();
        XFile.resetForTests();
    }

    /** Every script in the game's master archive, with the archive open. */
    private List<String> shippedScripts() {
        Path master = Paths.get(gameData(), "master.dat");
        assertTrue(master.toFile().isFile());
        assertEquals(0, Db.dbOpen(master.toString(), null));

        XFile.XList xlist = new XFile.XList();
        XFile.xlistInit("*.int", xlist);

        List<String> names = new ArrayList<>();
        for (String name : xlist.fileNames) {
            names.add(name);
        }
        return names;
    }

    @Test
    void everyShippedScriptDecodes() {
        List<String> names = shippedScripts();
        assertTrue(names.size() > 1000, "the master archive has " + names.size() + " scripts");

        List<String> problems = new ArrayList<>();
        int procedures = 0;
        int instructions = 0;
        for (String name : names) {
            byte[] data = ScriptScanner.readScript(name);
            assertNotNull(data, name);
            ScriptCompiler.CompiledScript script = ScriptCompiler.compile(name, data);
            procedures += script.procedures.size();
            for (ScriptCompiler.Procedure procedure : script.procedures) {
                instructions += procedure.code.size();
            }
            problems.addAll(script.problems);
        }
        Db.dbCloseAll();

        assertTrue(problems.isEmpty(),
            problems.size() + " problems, first: " + (problems.isEmpty() ? "" : problems.get(0)));
        System.out.println("scripts=" + names.size() + " procedures=" + procedures
            + " instructions=" + instructions);
    }

    @Test
    void theAnalysisSaysWhatItCannotFollow() {
        // The analysis is not finished, but it must not pretend: what it
        // cannot follow, it reports.
        List<String> names = shippedScripts();
        assertTrue(names.size() > 1000);

        byte[] data = ScriptScanner.readScript("scripts\\miJerky.int");
        assertNotNull(data);
        Db.dbCloseAll();

        ScriptCompiler.CompiledScript script = ScriptCompiler.compile("scripts\\miJerky.int", data);
        assertTrue(script.problems.isEmpty(), script.problems.toString());

        // The top level code is a launcher: it enters the critical section,
        // puts the exit address on the return stack and jumps into the
        // script's own code. The analysis follows the jump and then reports
        // the code it cannot reach.
        System.out.println("analysis problems: " + script.analysisProblems.size());
        for (String problem : script.analysisProblems) {
            System.out.println("  " + problem);
        }
        assertTrue(script.analysisProblems.size() > 0,
            "the analysis followed everything, which it should not");
    }
}
