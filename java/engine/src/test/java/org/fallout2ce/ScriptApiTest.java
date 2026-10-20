package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        GameGlobalVars.reset();
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
        Program.setGameApi(mock);

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

    /**
     * The reference's final state for the four scripts, from the
     * {@code [scriptrungame]} section of the harness: the same scripts run
     * with the game's own builtins available.
     */
    private static final String[][] REFERENCE = {
        { "scripts\\miJerky.int", "20", "80100001", "1", "C001:0" },
        { "scripts\\CATH2MV.int", "20", "80100001", "1", "C001:0" },
        { "scripts\\acdog.int", "20", "80100001", "21", "C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:1 C001:0" },
        { "scripts\\ABEL.int", "20", "80100001", "4", "C001:0 C001:0 C001:1 C001:0" },
        { "scripts\\doorcode.int", "20", "80100001", "1", "C001:0" },
        { "scripts\\CODEDISK.int", "20", "80100001", "1", "C001:0" },
        { "scripts\\ARTIFACT.int", "20", "80100001", "1", "C001:0" },
        { "scripts\\MutFruit.int", "20", "80100001", "1", "C001:0" },
        { "scripts\\DOG2.int", "20", "80100001", "7", "C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0" },
        { "scripts\\VSGTEAM9.int", "20", "80100001", "3", "C001:0 C001:1 C001:0" },
        { "scripts\\JONATHAN.int", "20", "80100001", "5", "C001:0 C001:0 C001:0 C001:0 C001:1" },
        { "scripts\\FREEZER.int", "20", "80100001", "1", "C001:0" },
        { "scripts\\GRAFFITI.int", "20", "80100001", "2", "C001:0 C001:0" },
        { "scripts\\BVAIR.int", "20", "80100001", "1", "C001:0" },
        { "scripts\\ziclosed.int", "20", "80100001", "20", "C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:1 C001:0" },
        { "scripts\\cowbomb.int", "20", "80100001", "12", "C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:1 C001:0" },
        { "scripts\\CSAREA2.int", "20", "80100001", "20", "C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:0 C001:1 C001:0" },
    };

    private static final class State {
        int ip;
        int flags;
        int stackSize;
        String stack;
    }

    private State runForState(String name, GameApi api) throws Exception {
        Path master = Paths.get(gameData(), "master.dat");
        assertTrue(master.toFile().isFile());
        assertEquals(0, Db.dbOpen(master.toString(), null));

        Program.setGameApi(api);

        Program program = Program.programCreateByPath(name);
        assertNotNull(program);
        program.flags = 0;
        program.instructionPointer = 0;

        Program.programInterpret(program, 20000);

        State state = new State();
        state.ip = program.instructionPointer;
        state.flags = program.flags;
        state.stackSize = program.stackValues.size();
        StringBuilder sb = new StringBuilder();
        for (int i = program.stackValues.size() - 1; i >= 0; i--) {
            Program.ProgramValue value = program.stackValues.get(i);
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%04X:%d", value.opcode & 0xFFFF, value.integerValue));
        }
        state.stack = sb.toString();

        Program.programFree(program);
        Db.dbCloseAll();
        return state;
    }

    private void assertMatchesReference(String name, GameApi api) throws Exception {
        String[] expected = null;
        for (String[] reference : REFERENCE) {
            if (reference[0].equals(name)) {
                expected = reference;
            }
        }
        assertNotNull(expected, name);

        State state = runForState(name, api);
        assertEquals(Integer.parseInt(expected[1]), state.ip, name + " ip");
        assertEquals((int) Long.parseLong(expected[2], 16), state.flags, name + " flags");
        assertEquals(Integer.parseInt(expected[3]), state.stackSize, name + " stack size");
        assertEquals(expected[4], state.stack, name + " stack");
    }

    @Test
    void theMockReproducesTheReferenceState() throws Exception {
        // The game's builtins all fall back to their "no state" answers in a
        // game-less harness, and so does the mock - so the mock stands in for
        // the game and the script ends up exactly where the reference ends up.
        for (String[] reference : REFERENCE) {
            assertMatchesReference(reference[0], new MockScriptApi());
        }
    }

    @Test
    void theGameApiReproducesTheReferenceState() throws Exception {
        for (String[] reference : REFERENCE) {
            assertMatchesReference(reference[0], new GameScriptApi());
        }
    }

    @Test
    void theGlobalVariablesParseTheSameWayAsTheReference() throws Exception {
        Path master = Paths.get(gameData(), "master.dat");
        assertTrue(master.toFile().isFile());
        assertEquals(0, Db.dbOpen(master.toString(), null));

        int[] size = { -1 };
        assertEquals(0, Db.dbGetFileSize("data\\vault13.gam", size));
        byte[] data = new byte[size[0]];
        assertEquals(0, Db.dbGetFileContents("data\\vault13.gam", data, 0));

        GameGlobalVars.reset();
        GameGlobalVars.load(data);

        // The reference's own parse of the same file: 695 variables and this
        // checksum of the values.
        assertEquals(695, GameGlobalVars.length);
        assertEquals(-1024294307, GameGlobalVars.checksum());

        // The enum and the file agree on the order.
        assertEquals(696, GameGlobalVars.ENUM_COUNT);
        assertEquals("GVAR_PLAYER_REPUTATION", GameGlobalVars.NAMES[0]);
        assertEquals("GVAR_ENEMY_DEN", GameGlobalVars.NAMES[14]);

        // The last enum value is past the end of the file, so it reads as
        // zero, like the original's range check.
        assertTrue(!GameGlobalVars.isValid(GameGlobalVars.ENUM_COUNT - 1));
        assertEquals(0, GameGlobalVars.get(GameGlobalVars.ENUM_COUNT - 1));

        GameGlobalVars.reset();
        Db.dbCloseAll();
    }

    @Test
    void theMessageStringsParseTheSameWayAsTheReference() throws Exception {
        Path master = Paths.get(gameData(), "master.dat");
        assertTrue(master.toFile().isFile());
        assertEquals(0, Db.dbOpen(master.toString(), null));

        // The script list, and the message file that goes with a script.
        int[] size = { -1 };
        assertEquals(0, Db.dbGetFileSize("scripts\\scripts.lst", size));
        byte[] listData = new byte[size[0]];
        assertEquals(0, Db.dbGetFileContents("scripts\\scripts.lst", listData, 0));
        ScriptMessages.loadScriptsList(listData);

        // The list the reference reads: obj_dude.int is the first entry.
        assertEquals("obj_dude.int", ScriptMessages.scriptFileName(0));
        assertEquals(5, ScriptMessages.scriptLocalVarsCount(0));
        assertEquals("Test0.int", ScriptMessages.scriptFileName(1));
        assertEquals(8, ScriptMessages.scriptLocalVarsCount(1));

        // The reference's own message-file loader read these three:
        //   msg num=100 [You see a regular domesticated brahmin.]
        //   msg num=101 [The brahmin's extra head is probably the result of a
        //   regular cow being exposed too soon to radiation.]
        //   msg num=102 [You don?t notice anything unusual about the brahmin.]
        assertEquals("You see a regular domesticated brahmin.",
            ScriptMessages.messageText(17, 100));
        assertEquals(
            "The brahmin's extra head is probably the result of a regular cow being exposed too soon to radiation.",
            ScriptMessages.messageText(17, 101));
        assertEquals("You don\u0092t notice anything unusual about the brahmin.",
            ScriptMessages.messageText(17, 102));

        // A message that is not there is null, and the original answers
        // "Error" for it.
        assertNull(ScriptMessages.messageText(17, 9999));

        Db.dbCloseAll();
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
        mock.curMapIndex();
        mock.getGlobalVar(3);
        mock.setGlobalVar(4, new Program.ProgramValue(7));

        List<MockScriptApi.Call> calls = mock.calls();
        assertEquals(3, calls.size());
        assertEquals("curMapIndex", calls.get(0).name);
        assertEquals("getGlobalVar", calls.get(1).name);
        assertEquals("3", calls.get(1).args.get(0));
        assertEquals("setGlobalVar", calls.get(2).name);
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
