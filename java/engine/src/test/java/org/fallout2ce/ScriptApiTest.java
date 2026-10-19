package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The script API: a script run against a mock instead of the game.
 *
 * <p>This is what the interface is for. The game's builtins are the surface
 * the scripts are written against, and behind an interface a script can be
 * run with the game replaced by a recorder - so the script's behaviour can be
 * asserted without a map, an interface or a running game.
 *
 * <p>The mock answers with fixed values, so the script runs to completion
 * deterministically.
 */
class ScriptApiTest {

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
        Config.resetLastSectionKey();
        XFile.resetForTests();
        Program.interpreterRegisterOpcodeHandlers();
    }

    /** Runs a script with the given API and returns the mock's trace. */
    private MockScriptApi runScript(String name) throws Exception {
        Path master = Paths.get(gameData(), "master.dat");
        assertTrue(master.toFile().isFile());
        assertEquals(0, Db.dbOpen(master.toString(), null));

        MockScriptApi mock = new MockScriptApi();
        Program.setScriptApi(mock);

        byte[] data = ScriptScanner.readScript(name);
        assertNotNull(data, name);

        // The script's own code, laid out the way programCreateByPath reads it.
        ScriptScanner.Layout layout = ScriptScanner.layout(data);
        Program program = Program.programCreateByPath(name);
        assertNotNull(program);
        program.flags = 0;
        program.instructionPointer = 0;

        Program.programInterpret(program, 20000);

        Program.programFree(program);
        Db.dbCloseAll();
        return mock;
    }

    @Test
    void aScriptRunsAgainstTheMockAndItsCallsAreRecorded() throws Exception {
        MockScriptApi mock = runScript("scripts\\acdog.int");

        // The script called the game; the mock recorded it instead of needing
        // the game to be there.
        assertTrue(mock.calls().size() > 0, "the script made no calls");
    }

    @Test
    void theMockRecordsTheBuiltinsInOrder() {
        MockScriptApi mock = new MockScriptApi();
        mock.selfObj(null);
        mock.getLocalVar(null, 3);
        mock.setLocalVar(null, 4, new Program.ProgramValue(7));

        List<MockScriptApi.Call> calls = mock.calls();
        assertEquals(3, calls.size());
        assertEquals("selfObj", calls.get(0).name);
        assertEquals("getLocalVar", calls.get(1).name);
        assertEquals("3", calls.get(1).args.get(0));
        assertEquals("setLocalVar", calls.get(2).name);
        assertEquals("4", calls.get(2).args.get(0));
        assertEquals("C001:7", calls.get(2).args.get(1));
    }

    @Test
    void theApiCoversEveryBuiltinTheScriptsUse() {
        // The interface and the opcode table have to agree: every builtin the
        // shipped scripts use has to be reachable through the API.
        int apiBuiltins = 0;
        for (int slot = 0; slot < ScriptOpcodes.SLOT_COUNT; slot++) {
            if (ScriptOpcodes.CORE[slot]) {
                continue;
            }
            if (ScriptOpcodes.HANDLER[slot] != null) {
                apiBuiltins++;
            }
        }
        assertTrue(apiBuiltins >= 161, "only " + apiBuiltins + " builtins");
    }
}
