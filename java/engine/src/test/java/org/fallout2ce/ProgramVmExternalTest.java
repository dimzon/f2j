package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [vmexternal]} vectors from {@code docker/refharness/golden/v125.txt},
 * produced by {@code sectionVmExternal} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>{@code EXPORT_VARIABLE}, {@code EXPORT_PROCEDURE}, {@code FETCH_EXTERNAL}
 * and {@code STORE_EXTERNAL}, over the two shared 1013-slot registries in
 * {@code src/export.cc}. The popped operand is an identifier-table offset, not
 * the name.
 *
 * <p>Identifier offsets are measured from the block's own length field:
 * {@code programGetIdentifier} is {@code identifiers + offset} with no
 * {@code +4}, and {@code identifiers} points AT that field, so the blob bytes
 * start at offset 4. In {@code "main\0shared\0thing"} that puts "main" at 4,
 * "shared" at 9 and "thing" at 16.
 *
 * <p>Two properties of the registry make these cases order-dependent, so they
 * run as one sequential method rather than five independent tests:
 * <ul>
 *   <li>The tables are global and never reset, so entries leak from one case to
 *       the next. The genuinely-missing cases therefore have to run first.
 *   <li>{@code externalVariableCreate} compares {@code program->name}, not the
 *       Program pointer, so re-exporting a live name only succeeds when the
 *       name matches - every case loads from the same path.
 * </ul>
 */
class ProgramVmExternalTest {

    private static final byte[] IDENTIFIERS =
        "main\0shared\0thing".getBytes(StandardCharsets.ISO_8859_1);

    private static final byte[] STRINGS =
        "hello".getBytes(StandardCharsets.ISO_8859_1);

    /** "main\0shared\0thing": "shared" at 9, "thing" at 16. */
    private static final int SHARED_OFFSET = 9;
    private static final int THING_OFFSET = 16;

    /** 36 characters, so the stored name is clipped by strncpy. */
    private static final byte[] LONG_IDENTIFIERS =
        "main\0abcdefghijklmnopqrstuvwxyz0123456789".getBytes(StandardCharsets.ISO_8859_1);

    private static final int LONG_OFFSET = 9;

    private static void writeBE32(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 24);
        data[offset + 1] = (byte) (value >> 16);
        data[offset + 2] = (byte) (value >> 8);
        data[offset + 3] = (byte) value;
    }

    private static final class Image {
        byte[] data;
        int codeOffset;
    }

    private static Image buildProgram(byte[] code, byte[] identifiers) {
        int identifiersOffset = 46 + 24;
        int stringTableOffset = identifiersOffset + 4 + identifiers.length;
        int codeOffset = stringTableOffset + 4 + STRINGS.length;
        int bodyOffset = codeOffset + code.length;

        byte[] data = new byte[1024];
        writeBE32(data, 42, 1);
        writeBE32(data, 46 + 0, 4);
        writeBE32(data, 46 + 16, bodyOffset);
        writeBE32(data, 46 + 20, 0);

        writeBE32(data, identifiersOffset, identifiers.length);
        System.arraycopy(identifiers, 0, data, identifiersOffset + 4, identifiers.length);
        writeBE32(data, stringTableOffset, STRINGS.length);
        System.arraycopy(STRINGS, 0, data, stringTableOffset + 4, STRINGS.length);
        System.arraycopy(code, 0, data, codeOffset, code.length);

        Image image = new Image();
        image.data = data;
        image.codeOffset = codeOffset;
        return image;
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
        int flags;
        String stack;
    }

    /** One path for every case, so {@code program->name} matches across them. */
    private Path scriptPath;

    private Result run(byte[] code, byte[] identifiers) throws Exception {
        Image image = buildProgram(code, identifiers);

        Files.write(scriptPath, image.data);

        Program program = Program.programCreateByPath(scriptPath.toString());
        assertNotNull(program);
        program.flags = 0;
        program.instructionPointer = image.codeOffset;

        Program.programInterpret(program, 32);

        Result result = new Result();
        result.flags = program.flags;
        result.stack = dump(program.stackValues);

        Program.programFree(program);
        return result;
    }

    private Result run(byte[] code) throws Exception {
        return run(code, IDENTIFIERS);
    }

    @BeforeEach
    void setup() throws Exception {
        Db.resetForTests();
        Config.resetLastSectionKey();
        XFile.resetForTests();
        Program.interpreterRegisterOpcodeHandlers();
        Export.resetForTests();
        scriptPath = Files.createTempFile("vmexternal", ".int");
    }

    private static byte[] pushInt(int value) {
        return new byte[] {
            (byte) 0xC0, (byte) 0x01,
            (byte) (value >> 24), (byte) (value >> 16),
            (byte) (value >> 8), (byte) value,
        };
    }

    private static byte[] cat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) {
            len += p.length;
        }
        byte[] out = new byte[len];
        int at = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, at, p.length);
            at += p.length;
        }
        return out;
    }

    private static final byte[] EXPORT_VARIABLE = { (byte) 0x80, (byte) 0x16 };
    private static final byte[] FETCH_EXTERNAL = { (byte) 0x80, (byte) 0x14 };
    private static final byte[] STORE_EXTERNAL = { (byte) 0x80, (byte) 0x15 };
    private static final byte[] STOP = { (byte) 0x80, (byte) 0x11 };

    @Test
    void registryVectorsRunInOrder() throws Exception {
        // Must be first: the table is empty, so an unexported name is fatal.
        Result fetchMissing = run(cat(pushInt(SHARED_OFFSET), FETCH_EXTERNAL, STOP));
        assertEquals(0x80140005, fetchMissing.flags);
        assertEquals("", fetchMissing.stack);

        Result storeMissing = run(cat(pushInt(42), pushInt(SHARED_OFFSET), STORE_EXTERNAL, STOP));
        assertEquals(0x80150005, storeMissing.flags);
        assertEquals("", storeMissing.stack);

        Result roundTrip = run(cat(
            pushInt(SHARED_OFFSET), EXPORT_VARIABLE,
            pushInt(42), pushInt(SHARED_OFFSET), STORE_EXTERNAL,
            pushInt(SHARED_OFFSET), FETCH_EXTERNAL,
            STOP));
        assertEquals(0x80110008, roundTrip.flags);
        assertEquals("C001:42", roundTrip.stack);

        // A second name sits beside the first without disturbing it.
        Result twoNames = run(cat(
            pushInt(THING_OFFSET), EXPORT_VARIABLE,
            pushInt(7), pushInt(THING_OFFSET), STORE_EXTERNAL,
            pushInt(THING_OFFSET), FETCH_EXTERNAL,
            pushInt(SHARED_OFFSET), FETCH_EXTERNAL,
            STOP));
        assertEquals(0x80110008, twoNames.flags);
        assertEquals("C001:42 C001:7", twoNames.stack);

        // Re-exporting under the same programName resets the value to 0.
        Result reExport = run(cat(
            pushInt(SHARED_OFFSET), EXPORT_VARIABLE,
            pushInt(SHARED_OFFSET), FETCH_EXTERNAL,
            STOP));
        assertEquals(0x80110008, reExport.flags);
        assertEquals("C001:0", reExport.stack);
    }

    @Test
    void aNameLongerThan31CharactersCanNeverBeFoundAgain() throws Exception {
        // strncpy(name, id, 31) clips the STORED name, but the lookup compares
        // the stored name against the FULL identifier - so the export succeeds
        // and the fetch can never match it.
        Result r = run(cat(
            pushInt(LONG_OFFSET), EXPORT_VARIABLE,
            pushInt(LONG_OFFSET), FETCH_EXTERNAL,
            STOP), LONG_IDENTIFIERS);

        assertEquals(0x80140005, r.flags);
        assertEquals("", r.stack);
    }

    @Test
    void hashIsStableAndCaseInsensitive() {
        int shared = Export.hashName("shared");
        assertTrue(shared >= 0 && shared < Export.TABLE_SIZE);
        // compat_stricmp / tolower: the hash ignores case, so SHARED and
        // ShArEd land in the same slot as shared.
        assertEquals(shared, Export.hashName("SHARED"));
        assertEquals(shared, Export.hashName("ShArEd"));
        assertTrue(Export.hashName("thing") >= 0);
        // Distinct names generally hash apart, which keeps the probe chains short.
        assertTrue(Export.hashName("thing") != shared);
    }
}
