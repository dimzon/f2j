package org.fallout2ce;

/**
 * Port of the external variable / procedure registries ({@code src/export.cc},
 * the {@code procHashTable} at 0x570C00 and {@code varHashTable} at 0x57BA1C).
 *
 * <p>Two fixed 1013-slot open-addressed tables shared across every script. A
 * name hashes to a slot and probes forward in steps of 7, wrapping at the end,
 * which reproduces the original's slot layout exactly - the layout is
 * observable through the collision behaviour, so it is ported literally rather
 * than replaced with a map.
 *
 * <p>The two tables disagree about how a slot reads as free, and both quirks
 * are kept: {@link ExternalProcedure} occupancy is tested by
 * {@code program != nullptr} while {@link ExternalVariable} occupancy is tested
 * by {@code name[0] == '\0'}.
 *
 * <p>Names are truncated to 31 characters ({@code strncpy(name, id, 31)} into a
 * {@code char[32]} that zero-initialisation leaves NUL-terminated), so two
 * identifiers sharing a 31-character prefix collide.
 */
final class Export {

    /** Both tables are 1013 slots wide, and the probe step and wraparound use it. */
    static final int TABLE_SIZE = 1013;

    /** {@code strncpy(..., 31)} into {@code char name[32]}. */
    private static final int NAME_LIMIT = 31;

    /** Port of {@code ExternalVariable}. */
    static final class ExternalVariable {
        /** Empty means "unused" - this is the occupancy test for this table. */
        String name = "";

        String programName;

        /** C's is a plain struct, so it is never null. */
        Program.ProgramValue value = new Program.ProgramValue();

        /** Backing store when {@link #value} holds a string type. */
        String stringValue;
    }

    /** Port of {@code ExternalProcedure}. */
    static final class ExternalProcedure {
        String name = "";

        /** Non-null means "used" - this is the occupancy test for this table. */
        Program program;

        int argumentCount;
        int address;
    }

    private static final ExternalProcedure[] PROCEDURES = new ExternalProcedure[TABLE_SIZE];

    private static final ExternalVariable[] VARIABLES = new ExternalVariable[TABLE_SIZE];

    static {
        for (int i = 0; i < TABLE_SIZE; i++) {
            PROCEDURES[i] = new ExternalProcedure();
            VARIABLES[i] = new ExternalVariable();
        }
    }

    private Export() {
    }

    /**
     * 0x440F10 hashName. C uses {@code unsigned int} with {@code h >> 29}, so
     * Java's wrapping {@code int} arithmetic matches as long as the final modulo
     * is taken on the unsigned value.
     */
    static int hashName(String identifier) {
        int h = 0;
        for (int i = 0; i < identifier.length(); i++) {
            int ch = identifier.charAt(i) & 0xFF;
            h += (Character.toLowerCase((char) ch) & 0xFF) + (h * 8) + (h >>> 29);
        }
        return (int) ((h & 0xFFFFFFFFL) % TABLE_SIZE);
    }

    /** C's {@code idx += 7; if (idx >= TABLE_SIZE) idx -= TABLE_SIZE;}. */
    private static int advance(int idx) {
        int next = idx + 7;
        if (next >= TABLE_SIZE) {
            next -= TABLE_SIZE;
        }
        return next;
    }

    /** C's {@code strncpy(dst, src, 31)} into a zero-filled 32-byte field. */
    private static String truncateName(String identifier) {
        return identifier.length() <= NAME_LIMIT ? identifier : identifier.substring(0, NAME_LIMIT);
    }

    // 0x440F58 findProc: occupancy is `program != nullptr`.
    private static ExternalProcedure findProcedure(String identifier) {
        int idx = hashName(identifier);
        int start = idx;

        ExternalProcedure proc = PROCEDURES[idx];
        if (proc.program != null && proc.name.equalsIgnoreCase(identifier)) {
            return proc;
        }

        do {
            idx = advance(idx);
            proc = PROCEDURES[idx];
            if (proc.program != null && proc.name.equalsIgnoreCase(identifier)) {
                return proc;
            }
        } while (idx != start);

        return null;
    }

    // 0x441018 findEmptyProc: occupancy is `name[0] == '\0'`.
    private static ExternalProcedure addProcedure(String identifier) {
        int idx = hashName(identifier);
        int start = idx;

        ExternalProcedure proc = PROCEDURES[idx];
        if (proc.name.isEmpty()) {
            return proc;
        }

        do {
            idx = advance(idx);
            proc = PROCEDURES[idx];
            if (proc.name.isEmpty()) {
                return proc;
            }
        } while (idx != start);

        return null;
    }

    // 0x4410AC findVar: occupancy is `name[0] == '\0'`.
    private static ExternalVariable findVariable(String identifier) {
        int idx = hashName(identifier);
        int start = idx;

        ExternalVariable var = VARIABLES[idx];
        if (var.name.equalsIgnoreCase(identifier)) {
            return var;
        }

        do {
            var = VARIABLES[idx];
            if (var.name.isEmpty()) {
                return null;
            }

            idx = advance(idx);
            var = VARIABLES[idx];
            if (var.name.equalsIgnoreCase(identifier)) {
                return var;
            }
        } while (idx != start);

        return null;
    }

    // 0x44118C findEmptyVar.
    private static ExternalVariable addVariable(String identifier) {
        int idx = hashName(identifier);
        int start = idx;

        ExternalVariable var = VARIABLES[idx];
        if (var.name.isEmpty()) {
            return var;
        }

        do {
            idx = advance(idx);
            var = VARIABLES[idx];
            if (var.name.isEmpty()) {
                return var;
            }
        } while (idx != start);

        return null;
    }

    private static boolean isString(Program.ProgramValue value) {
        return (value.opcode & Program.VALUE_TYPE_MASK) == Program.VALUE_TYPE_STRING;
    }

    /**
     * 0x4413D4 externalVariableGetValue. Returns 1 when the name is unknown.
     * A stored string is re-interned into {@code program}'s own heap rather
     * than handed back as a borrowed offset.
     */
    static int getValue(Program program, String name, Program.ProgramValue value) {
        ExternalVariable var = findVariable(name);
        if (var == null) {
            return 1;
        }

        if (isString(var.value)) {
            value.opcode = var.value.opcode;
            value.integerValue = Program.programPushString(program, var.stringValue);
        } else {
            // C copies the whole struct.
            value.opcode = var.value.opcode;
            value.integerValue = var.value.integerValue;
            value.floatValue = var.value.floatValue;
            value.pointerValue = var.value.pointerValue;
        }

        return 0;
    }

    /**
     * 0x44127C externalVariableSetValue. Returns 1 when the name is unknown, so
     * the variable has to be exported first.
     */
    static int setValue(Program program, String name, Program.ProgramValue programValue) {
        ExternalVariable var = findVariable(name);
        if (var == null) {
            return 1;
        }

        if (isString(programValue)) {
            // C keeps the stored type dynamic and copies the bytes out, because
            // the offset in programValue is only meaningful inside `program`.
            String stringValue = Program.programGetString(program, programValue.opcode, programValue.integerValue);
            var.value.opcode = Program.VALUE_TYPE_DYNAMIC_STRING;
            var.value.integerValue = 0;
            var.stringValue = stringValue;
        } else {
            var.value.opcode = programValue.opcode;
            var.value.integerValue = programValue.integerValue;
            var.stringValue = null;
        }

        return 0;
    }

    /**
     * 0x4414B8 externalVariableCreate. Returns 1 when the name is already
     * exported by a <em>different</em> program - re-exporting from the same
     * program is fine and just resets the value to 0.
     */
    static int createVariable(Program program, String identifier) {
        String programName = program.name;
        ExternalVariable var = findVariable(identifier);

        if (var != null) {
            if (!var.programName.equalsIgnoreCase(programName)) {
                return 1;
            }
        } else {
            var = addVariable(identifier);
            if (var == null) {
                return 1;
            }

            var.name = truncateName(identifier);
            var.programName = programName;
        }

        var.value.opcode = Program.VALUE_TYPE_INT;
        var.value.integerValue = 0;
        var.stringValue = null;

        return 0;
    }

    /**
     * 0x4414B8's procedure sibling. Returns 1 when the name is already exported
     * by a different {@link Program} instance.
     */
    static int createProcedure(Program program, String identifier, int address, int argumentCount) {
        ExternalProcedure proc = findProcedure(identifier);

        if (proc != null) {
            if (proc.program != program) {
                return 1;
            }
        } else {
            proc = addProcedure(identifier);
            if (proc == null) {
                return 1;
            }

            proc.name = truncateName(identifier);
        }

        proc.argumentCount = argumentCount;
        proc.address = address;
        proc.program = program;

        return 0;
    }

    /** Resolves an exported procedure, or null. Used by the externals call opcodes. */
    static ExternalProcedure lookupProcedure(String identifier) {
        return findProcedure(identifier);
    }

    /**
     * Clears every entry owned by {@code program}. Port of
     * {@code _removeProgramReferences} (0x441920).
     *
     * <p>Procedures only - exported <em>variables</em> deliberately survive,
     * because C keeps them keyed on {@code programName} and never sweeps them.
     * The trailing bytes of {@code name} also survive in C; only
     * {@code name[0]} is cleared, which is all the occupancy tests and
     * {@code compat_stricmp} can observe.
     */
    static void removeProgramReferences(Program program) {
        for (int i = 0; i < TABLE_SIZE; i++) {
            ExternalProcedure proc = PROCEDURES[i];
            if (proc.program == program) {
                proc.name = "";
                proc.program = null;
            }
        }
    }

    /** Test hook: drops every entry from both tables. */
    static void resetForTests() {
        for (int i = 0; i < TABLE_SIZE; i++) {
            PROCEDURES[i] = new ExternalProcedure();
            VARIABLES[i] = new ExternalVariable();
        }
    }
}
