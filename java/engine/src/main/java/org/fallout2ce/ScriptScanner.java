package org.fallout2ce;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader and decoder for the Fallout 2 script format ({@code .int}).
 *
 * <p>A script's layout is
 * {@code [top-level code][procedure count][procedure table][identifiers][static strings][procedure bodies]}.
 * The top-level code is the first {@link #TOP_LEVEL_CODE_SIZE} bytes; the
 * procedure bodies come after the static string table.
 *
 * <p>The code is walked linearly: every opcode is two bytes, and
 * {@code PUSH} carries a four-byte operand. The game's compiler emits code
 * linearly, so no control flow has to be followed - the same assumption
 * {@code src/scan_unimplemented_opcodes.h} makes.
 *
 * <p>The census this produces is what sizes the remaining work: the intlib
 * registers hundreds of opcodes, but the shipped scripts only use a subset,
 * and that subset is the list that has to be ported.
 */
final class ScriptScanner {

    /** The top-level code is the first 42 bytes; the procedure table follows. */
    static final int TOP_LEVEL_CODE_SIZE = 0x2A;

    /** The dispatch slot of {@code PUSH}, whose operand is four bytes. */
    private static final int PUSH_SLOT = 0x001;

    private ScriptScanner() {
    }

    /** The parts of a script the reader needs. */
    static final class Layout {
        int procedureCount;
        int identifiersPos;
        int staticStringsPos;
        int staticStringLen;
        int codePos;
    }

    /** Parses the script's layout. Mirrors the C scanner exactly. */
    static Layout layout(byte[] data) {
        Layout layout = new Layout();
        layout.procedureCount = Program.stackReadInt32(data, 42);
        layout.identifiersPos = 24 * layout.procedureCount + 42 + 4;
        layout.staticStringsPos = layout.identifiersPos
                + Program.stackReadInt32(data, layout.identifiersPos) + 4 + 4;
        layout.staticStringLen = Program.stackReadInt32(data, layout.staticStringsPos);
        if (layout.staticStringLen == -1) {
            layout.staticStringLen = -4;
        }
        layout.codePos = layout.staticStringsPos + 4 + layout.staticStringLen + 4;
        return layout;
    }

    /**
     * Walks the code from {@code from} to {@code to}, counting each opcode
     * into {@code histogram} and each opcode with no known name into
     * {@code unknown}.
     */
    static void walk(byte[] data, int from, int to, int[] histogram, int[] unknown) {
        int pos = from;
        while (pos + 2 <= to) {
            int opcode = Program.stackReadInt16(data, pos);
            if (((opcode >> 8) & 0x80) == 0) {
                return;
            }
            int slot = opcode & 0x3FF;
            histogram[slot]++;
            if (slot < ScriptOpcodes.SLOT_COUNT && ScriptOpcodes.HANDLER[slot] == null) {
                unknown[0]++;
            }
            pos += 2;
            if (slot == PUSH_SLOT) {
                pos += 4;
            }
        }
    }

    /** The census of every script in the game data. */
    static final class Census {
        int scripts;
        long instructions;
        int unknown;
        int[] histogram = new int[ScriptOpcodes.SLOT_COUNT];
        int[] used = new int[ScriptOpcodes.SLOT_COUNT];
    }

    /**
     * Walks every {@code .int} in master.dat.
     *
     * <p>The archive is read directly rather than through the VFS. The VFS
     * resolves names case-insensitively across every mounted archive, and
     * master.dat and patch000.dat both carry scripts whose names differ only
     * in case - {@code "scripts\arbridge.int"} and
     * {@code "Scripts\ARBRIDGE.int"}. Which archive wins then depends on the
     * mount order, which is a property of the game's startup rather than of
     * the script format. Reading master.dat's own entries keeps the census
     * well defined.
     */
    static Census census(String gameDataRoot) throws Exception {
        Path master = Paths.get(gameDataRoot, "master.dat");
        assertTrue(Files.isRegularFile(master), master.toString());

        int[] errorFlags = { 0 };
        DBase.Archive archive = DBase.dbaseOpen(master.toString(), errorFlags);
        assertTrue(archive != null, "dbaseOpen failed for " + master);

        Census census = new Census();
        int[] unknown = { 0 };

        for (DBase.Entry entry : archive.entries) {
            if (!entry.path.toLowerCase().endsWith(".int")) {
                continue;
            }

            byte[] data = readFromArchive(archive, entry.path);
            if (data == null) {
                continue;
            }

            Layout layout = layout(data);
            walk(data, 0, TOP_LEVEL_CODE_SIZE, census.histogram, unknown);
            walk(data, layout.codePos, data.length, census.histogram, unknown);
            census.scripts++;
        }

        census.unknown = unknown[0];
        for (int slot = 0; slot < census.histogram.length; slot++) {
            if (census.histogram[slot] != 0) {
                census.instructions += census.histogram[slot];
                census.used[slot] = census.histogram[slot];
            }
        }

        return census;
    }

    /** Reads one entry out of an archive, without going through the VFS. */
    static byte[] readFromArchive(DBase.Archive archive, String path) {
        DFile.Handle handle = DFile.dfileOpen(archive, path, "rb");
        if (handle == null) {
            return null;
        }
        long size = DFile.dfileGetSize(handle);
        if (size <= 0) {
            DFile.dfileClose(handle);
            return null;
        }
        byte[] data = new byte[(int) size];
        DFile.dfileRead(data, 0, 1, (int) size, handle);
        DFile.dfileClose(handle);
        return data;
    }

    /** Reads a script out of the VFS, or null when it is not there. */
    static byte[] readScript(String name) {
        int[] size = { -1 };
        if (Db.dbGetFileSize(name, size) != 0 || size[0] <= 0) {
            return null;
        }
        byte[] data = new byte[size[0]];
        if (Db.dbGetFileContents(name, data, 0) != 0) {
            return null;
        }
        return data;
    }

    /** One decoded instruction. */
    static final class Instruction {
        int offset;
        int opcode;
        int slot;
        String name;
        int operand;
        boolean hasOperand;
    }

    /**
     * Decodes the code regions of a script into a readable listing.
     *
     * <p>This is the script-level view: {@code PUSH} shows its immediate, and
     * every other opcode shows its name. It is a debugging aid rather than a
     * golden vector - the reference harness has no name table to compare
     * against.
     */
    static List<Instruction> decode(byte[] data) {
        List<Instruction> out = new ArrayList<>();
        Layout layout = layout(data);

        decodeRegion(data, 0, TOP_LEVEL_CODE_SIZE, "top", out);
        decodeRegion(data, layout.codePos, data.length, "body", out);
        return out;
    }

    private static void decodeRegion(byte[] data, int from, int to, String region,
            List<Instruction> out) {
        int pos = from;
        while (pos + 2 <= to) {
            int opcode = Program.stackReadInt16(data, pos);
            if (((opcode >> 8) & 0x80) == 0) {
                return;
            }
            int slot = opcode & 0x3FF;

            Instruction instruction = new Instruction();
            instruction.offset = pos;
            instruction.opcode = opcode;
            instruction.slot = slot;
            instruction.name = ScriptOpcodes.name(slot);
            if (slot == PUSH_SLOT && pos + 6 <= to) {
                instruction.hasOperand = true;
                instruction.operand = Program.stackReadInt32(data, pos + 2);
            }
            out.add(instruction);

            pos += 2;
            if (slot == PUSH_SLOT) {
                pos += 4;
            }
        }
    }

    /** Formats a listing the way the compiler's output reads. */
    static String listing(List<Instruction> instructions) {
        StringBuilder sb = new StringBuilder();
        for (Instruction instruction : instructions) {
            sb.append(String.format("%04X: %-16s", instruction.offset, instruction.name));
            if (instruction.hasOperand) {
                sb.append(' ').append(instruction.operand);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
