package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [scriptprog]} vectors from
 * {@code docker/refharness/golden/v91.txt}, produced by
 * {@code sectionScriptProg} in {@code docker/refharness/ref_main.cc}:
 * {@code loaded=1 start=1 miss=-1 pop=7,42}.
 *
 * <p>Flow mirrors the harness exactly: load {@code scripts\ABEL.int}
 * through the VFS ({@code F2J_GAMEDATA} + {@code master.dat}, like the
 * other VFS tests), find {@code "start"} (procedure index 1) and
 * {@code "nope"} (miss, -1), push 42 then 7, pop twice (LIFO: 7, 42),
 * free. No window system needed; execution stays out of reach.
 *
 * <p>The harness notes missing-file loads are fatal by design (noreturn,
 * no interpreter context), so only the successful load is pinned — the
 * Java null-on-miss divergence is covered by the same single vector.
 */
class ScriptProgTest {

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

    @BeforeEach
    void setup() {
        Db.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        if (Files.isRegularFile(Paths.get(gameEn, "patch000.dat"))) {
            XFile.xbaseOpen(gameEn + "/patch000.dat");
        }
        if (Files.isRegularFile(Paths.get(gameEn, "critter.dat"))) {
            XFile.xbaseOpen(gameEn + "/critter.dat");
        }
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
    }

    @Test
    void scriptProgVectors() {
        Program prog = Program.programCreateByPath("scripts\\ABEL.int");
        int startIdx = prog != null ? Program.programFindProcedure(prog, "start") : -999;
        int missIdx = prog != null ? Program.programFindProcedure(prog, "nope") : -999;
        int pop1 = -999;
        int pop2 = -999;
        if (prog != null) {
            Program.programStackPushInteger(prog, 42);
            Program.programStackPushInteger(prog, 7);
            pop1 = Program.programStackPopInteger(prog);
            pop2 = Program.programStackPopInteger(prog);
            Program.programFree(prog);
        }
        assertEquals("loaded=1 start=1 miss=-1 pop=7,42",
                "loaded=" + (prog != null ? 1 : 0)
                        + " start=" + startIdx
                        + " miss=" + missIdx
                        + " pop=" + pop1 + "," + pop2);
    }
}
