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
        List<String> analysis = new ArrayList<>();
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
            analysis.addAll(script.analysisProblems);
        }
        Db.dbCloseAll();

        assertTrue(problems.isEmpty(),
            problems.size() + " problems, first: " + (problems.isEmpty() ? "" : problems.get(0)));
        System.out.println("scripts=" + names.size() + " procedures=" + procedures
            + " instructions=" + instructions + " analysis problems=" + analysis.size());
        for (int i = 0; i < Math.min(analysis.size(), 20); i++) {
            System.out.println("  " + analysis.get(i));
        }
    }

    @Test
    void theAnalysisFollowsAShippedScript() {
        // The analysis follows the graph: the launcher, the script's own
        // code and the procedures, with the stack and the branches.
        List<String> names = shippedScripts();
        assertTrue(names.size() > 1000);

        byte[] data = ScriptScanner.readScript("scripts\\miJerky.int");
        assertNotNull(data);
        Db.dbCloseAll();

        ScriptCompiler.CompiledScript script = ScriptCompiler.compile("scripts\\miJerky.int", data);
        assertTrue(script.problems.isEmpty(), script.problems.toString());
        assertTrue(script.analysisProblems.isEmpty(), script.analysisProblems.toString());

        // The launcher, the script's own code and the procedure.
        assertEquals(3, script.procedures.size());
        assertEquals(0, script.procedures.get(0).index);
        assertEquals(-2, script.procedures.get(1).index);
        assertEquals(-1, script.procedures.get(2).index);
    }
}
