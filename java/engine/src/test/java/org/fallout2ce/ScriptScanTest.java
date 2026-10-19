package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [scriptscan]} vectors from {@code docker/refharness/golden/v129.txt},
 * produced by {@code sectionScriptScan} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>An opcode census over every {@code .int} in the game data: the scripts
 * walked linearly, every opcode counted.
 *
 * <p>This is the measurement that sizes what is left. The intlib registers
 * 448 opcodes, but the shipped scripts only use a fraction of them, and the
 * fraction they actually use is the list that has to be ported. The census
 * covers 1888 scripts and about 5.3 million instructions, so it is the whole
 * shipped script corpus, not a sample.
 *
 * <p>Both {@code master.dat} and {@code patch000.dat} are mounted: the
 * scripts live in both, and the reference harness has them both open because
 * the game mounts them at startup.
 */
class ScriptScanTest {

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

    @Test
    void censusMatchesTheReference() throws Exception {
        ScriptScanner.Census census = ScriptScanner.census(gameData());

        assertEquals(1443, census.scripts);
        assertEquals(3588075L, census.instructions);
        // Every opcode the scripts use has a known name.
        assertEquals(0, census.unknown);

        // The histogram, in the same order the reference prints it.
        List<String> lines = new ArrayList<>();
        for (int slot = 0; slot < census.used.length; slot++) {
            if (census.used[slot] != 0) {
                lines.add(String.format("op=0x%03X count=%d", slot, census.used[slot]));
            }
        }
        assertEquals(214, lines.size());

        // Spot-check the ends of the histogram rather than pinning all 214
        // lines here: the golden file carries the full census.
        assertEquals("op=0x001 count=1535568", lines.get(0));
        assertEquals("op=0x155 count=1", lines.get(lines.size() - 1));

        // The core interpreter opcodes and the game's own opcodes, split.
        int coreUsed = 0;
        int gameUsed = 0;
        for (int slot = 0; slot < census.used.length; slot++) {
            if (census.used[slot] == 0) {
                continue;
            }
            if (slot <= 0x04B) {
                coreUsed++;
            } else {
                gameUsed++;
            }
        }
        assertEquals(53, coreUsed);
        assertEquals(161, gameUsed);
    }

    @Test
    void everyOpcodeTheScriptsUseIsKnown() throws Exception {
        ScriptScanner.Census census = ScriptScanner.census(gameData());

        int withScriptName = 0;
        for (int slot = 0; slot < census.used.length; slot++) {
            if (census.used[slot] == 0) {
                continue;
            }
            // Every opcode the shipped scripts use must be one the engine
            // knows about.
            assertTrue(ScriptOpcodes.HANDLER[slot] != null,
                "slot 0x" + Integer.toHexString(slot) + " is not a known opcode");
            if (ScriptOpcodes.SCRIPT_NAME[slot] != null) {
                withScriptName++;
            }
        }
        // Most of the game's opcodes carry the original script-level name;
        // the rest are only named after the C handler.
        assertTrue(withScriptName > 150, "only " + withScriptName + " have script names");
    }

    @Test
    void aRealScriptDecodesToAReadableListing() throws Exception {
        String root = gameData();
        Path master = Paths.get(root, "master.dat");
        assertTrue(Files.isRegularFile(master));
        assertEquals(0, Db.dbOpen(master.toString(), null));

        byte[] data = ScriptScanner.readScript("scripts\\acdog.int");
        assertTrue(data != null);

        List<ScriptScanner.Instruction> instructions = ScriptScanner.decode(data);
        assertTrue(instructions.size() > 10);

        // The compiler's top-level prologue: enter the critical section, push
        // the return address, hand it to the return stack, push the procedure
        // body offset and jump to it.
        assertEquals("ENTER_CRITICAL_SECTION", instructions.get(0).name);
        assertEquals("PUSH", instructions.get(1).name);
        assertEquals(18, instructions.get(1).operand);
        assertEquals("D_TO_A", instructions.get(2).name);
        assertEquals("PUSH", instructions.get(3).name);
        assertEquals("JUMP", instructions.get(4).name);
        assertEquals("EXIT_PROGRAM", instructions.get(5).name);

        // The listing must read as text.
        String listing = ScriptScanner.listing(instructions);
        assertTrue(listing.contains("ENTER_CRITICAL_SECTION"), listing);
        assertTrue(listing.contains("EXIT_PROGRAM"), listing);
    }
}
