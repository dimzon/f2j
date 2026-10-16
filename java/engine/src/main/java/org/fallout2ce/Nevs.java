package org.fallout2ce;

/**
 * Named-event (nevs) registry slice, mirroring {@code src/nevs.cc}:
 * {@code _nevs_initonce} (0x488418), {@code _nevs_close} (0x4883AC),
 * {@code _nevs_addevent} (0x4884C8), {@code _nevs_clearevent} (0x48859C),
 * {@code _nevs_signal} (0x48862C), {@code _nevs_update} (0x4886AC), over the
 * statics {@code gNevs} (0x6391C8) / {@code gNevsHits} (0x6391CC),
 * {@code NEVS_COUNT 40}, and {@code NevsType} ({@code src/nevs.h}).
 *
 * <p>Vectors: {@code [nevsevents]} in
 * {@code docker/refharness/golden/v83.txt}, produced by
 * {@code sectionNevsEvents} in {@code docker/refharness/ref_main.cc}:
 * {@code add=0,0 signal=1 clearmiss=1 clear=0} and
 * {@code fillfail=0 over=1 clearslot=0 readd=0}. The golden block is 6 lines
 * plus the header: FOUR embedded {@code INFO: nevs_signal(...)} /
 * {@code INFO: nevs_clearevent(...)} stderr lines (C {@code debugPrint} on
 * the missing-name signal, the missing-name clear, the {@code ev1} clear,
 * and the {@code ev07} clear) interleaved with the two vector lines. Java
 * stays silent on all four per the established rule (debug output is never
 * part of the stdout vectors).
 *
 * <p>NOTES / mapping decisions (all documented at the use site too):
 * <ul>
 * <li>Initonce guard: C {@code _nevs_alloc} / {@code _nevs_find} call
 * {@code debugPrint} + {@code exit(99)} when {@code gNevs == nullptr}
 * (initonce never ran). Java has no process-exit path for test-only
 * slices; following the repo convention for such guards
 * ({@link TextFont} throws {@link IllegalStateException} for unusable
 * state), the guard throws {@code IllegalStateException}
 * ({@code "nevs_initonce() not called"}). Use explicit
 * {@link #nevsInitonce()} before any other call.</li>
 * <li>Program handle: C {@code Program*} ({@code src/interpreter.h}) maps
 * to the opaque {@link Program} stub (identity comparison, like C pointer
 * comparison; {@code null} programs are the vector case). No execution
 * happens through this handle here; dispatch goes through the
 * {@link ProgramExecutor} seam, see below.</li>
 * <li>Name storage: C {@code char name[32]} + raw {@code strcpy} (buffer
 * overflow / UB on names of 32+ chars). Java stores a {@code String};
 * names longer than 31 chars (32 minus the NUL) are truncated to 31, the
 * closest safe mirror of the fixed buffer. Vectors use short names only,
 * so the guard never fires there. Lookup is case-insensitive
 * ({@code compat_stricmp} &rarr; {@code String.equalsIgnoreCase}).</li>
 * <li>{@code _nevs_update} execution seam: C dispatches each hit via
 * {@code programExecuteProcedureAsync(program, proc)} (or
 * {@code callbackProc()}, which is always {@code nullptr} in practice:
 * {@code _nevs_addevent} unconditionally clears it and nothing else sets
 * it). The interpreter is unported, so the exec call sits behind a
 * nullable {@link ProgramExecutor} (default {@code null}: dispatch is
 * skipped). The slot accounting around it ({@code busy} set/clear,
 * {@code hits -= 1}, {@code gNevsHits +=} remainder, EVENT slots reset
 * after firing) is mirrored exactly and runs regardless of the seam.
 * Vectors never trigger update ({@code null} programs are never usable,
 * so hits stay 0 and update early-returns), hence the seam is inert
 * there. The {@code callbackProc} field is kept (nullable
 * {@code Runnable}, always {@code null} via addevent, like C) and runs
 * when set.</li>
 * <li>Interpreter-lib delete callback: C {@code _nevs_initonce}
 * registers {@code _nevs_removeprogramreferences} via
 * {@code intLibRegisterProgramDeleteCallback}. Java has no
 * interpreter-lib port yet (no program-delete callback registry), so
 * registration is NOTE-skipped; the removal walk itself is ported as
 * {@link #removeProgramReferences} for the future wiring.</li>
 * <li>Reset: {@link #resetForTests} restores the fresh-process mirror
 * ({@code gNevs} null, {@code gNevsHits} 0, executor uninstalled),
 * matching the C BSS zeros/nulls.</li>
 * </ul>
 */
public final class Nevs {

    private Nevs() {
    }

    /** Port of {@code NevsType} ({@code src/nevs.h}: EVENT = 0, HANDLER = 1). */
    public enum NevsType {
        EVENT,
        HANDLER;

        /** C enum value ({@code ordinal()} matches by declaration order). */
        public int id() {
            return ordinal();
        }
    }

    /**
     * Execution seam for {@link #nevsUpdate} dispatch (port of
     * {@code programExecuteProcedureAsync}; the interpreter phase owns the
     * real implementation). Uninstalled ({@code null}, the default) means
     * dispatch is skipped while slot accounting still runs exactly.
     */
    public interface ProgramExecutor {
        void executeAsync(Program program, int proc);
    }

    /** Port of {@code Nevs} ({@code src/nevs.cc}); one fixed table slot. */
    static final class Slot {
        boolean used;
        String name = "";
        Program program;
        int proc;
        NevsType type = NevsType.EVENT;
        int hits;
        boolean busy;
        Runnable callbackProc;

        void reset() {
            used = false;
            name = "";
            program = null;
            proc = 0;
            type = NevsType.EVENT;
            hits = 0;
            busy = false;
            callbackProc = null;
        }
    }

    /** Port of {@code NEVS_COUNT} (40). */
    static final int NEVS_COUNT = 40;

    /** Max name chars storable in the C {@code char[32]} buffer (minus NUL). */
    static final int NAME_CAPACITY = 31;

    // 0x6391C8 nevs (gNevs)
    private static Slot[] nevsTable;

    // 0x6391CC anyhits (gNevsHits)
    private static int nevsHits;

    private static ProgramExecutor programExecutor;

    /** Test seam: install (or clear, with {@code null}) the update dispatcher. */
    public static void setProgramExecutor(ProgramExecutor executor) {
        programExecutor = executor;
    }

    /** Fresh-process mirror: table null, hits 0, executor uninstalled. */
    static void resetForTests() {
        nevsTable = null;
        nevsHits = 0;
        programExecutor = null;
    }

    private static void requireInit() {
        if (nevsTable == null) {
            // C: debugPrint + exit(99); Java: explicit init + IllegalStateException.
            throw new IllegalStateException("nevs_initonce() not called");
        }
    }

    // 0x488340 nevs_alloc (NOTE: Uninline reset mirrored by Slot.reset).
    private static Slot nevsAlloc() {
        requireInit();
        for (int index = 0; index < NEVS_COUNT; index++) {
            Slot slot = nevsTable[index];
            if (!slot.used) {
                slot.reset();
                return slot;
            }
        }
        return null;
    }

    // 0x48846C nevs_find (case-insensitive, like compat_stricmp).
    private static Slot nevsFind(String name) {
        requireInit();
        for (int index = 0; index < NEVS_COUNT; index++) {
            Slot slot = nevsTable[index];
            if (slot.used && slot.name.equalsIgnoreCase(name)) {
                return slot;
            }
        }
        return null;
    }

    // 0x488418 nevs_initonce. NOTE: intLib program-delete callback
    // registration is skipped (no interpreter-lib port yet); the removal
    // walk lives in removeProgramReferences for the future wiring.
    public static void nevsInitonce() {
        if (nevsTable == null) {
            nevsTable = new Slot[NEVS_COUNT];
            for (int index = 0; index < NEVS_COUNT; index++) {
                nevsTable[index] = new Slot();
            }
        }
    }

    // 0x4883AC nevs_close.
    public static void nevsClose() {
        nevsTable = null;
    }

    // 0x4883D4 nevs_removeprogramreferences (static in C; public here so the
    // future interpreter-lib port can wire it as the delete callback).
    public static void removeProgramReferences(Program program) {
        if (nevsTable != null) {
            for (int i = 0; i < NEVS_COUNT; i++) {
                Slot slot = nevsTable[i];
                if (slot.used && slot.program == program) {
                    slot.reset();
                }
            }
        }
    }

    // 0x4884C8 nevs_addevent (find-or-alloc, overwrite in place; C strcpy
    // into char[32] becomes a 31-char truncation guard, see class notes).
    public static int nevsAddEvent(String name, Program program, int proc, NevsType type) {
        Slot slot = nevsFind(name);
        if (slot == null) {
            slot = nevsAlloc();
        }
        if (slot == null) {
            return 1;
        }
        slot.used = true;
        slot.name = name.length() > NAME_CAPACITY ? name.substring(0, NAME_CAPACITY) : name;
        slot.program = program;
        slot.proc = proc;
        slot.type = type;
        slot.callbackProc = null;
        return 0;
    }

    // 0x48859C nevs_clearevent (silent: no debugPrint).
    public static int nevsClearEvent(String name) {
        Slot slot = nevsFind(name);
        if (slot != null) {
            slot.reset();
            return 0;
        }
        return 1;
    }

    // 0x48862C nevs_signal (silent: no debugPrint; never fires with a null
    // program, so null-program vectors always return 1 here).
    public static int nevsSignal(String name) {
        Slot slot = nevsFind(name);
        if (slot == null) {
            return 1;
        }
        if (slot.used
                && ((slot.program != null && slot.proc != 0) || slot.callbackProc != null)
                && !slot.busy) {
            slot.hits++;
            nevsHits++;
            return 0;
        }
        return 1;
    }

    // 0x4886AC nevs_update (hits loop with execution seam, see class notes).
    public static void nevsUpdate() {
        if (nevsHits == 0) {
            return;
        }
        nevsHits = 0;
        if (nevsTable == null) {
            return;
        }
        for (int index = 0; index < NEVS_COUNT; index++) {
            Slot slot = nevsTable[index];
            if (slot.used
                    && ((slot.program != null && slot.proc != 0) || slot.callbackProc != null)
                    && !slot.busy) {
                if (slot.hits > 0) {
                    slot.busy = true;
                    slot.hits -= 1;
                    nevsHits += slot.hits;
                    if (slot.callbackProc == null) {
                        if (programExecutor != null) {
                            programExecutor.executeAsync(slot.program, slot.proc);
                        }
                        // NOTE: executor null (no interpreter yet) means the
                        // dispatch call is skipped; accounting above is exact.
                    } else {
                        slot.callbackProc.run();
                    }
                    slot.busy = false;
                    if (slot.type == NevsType.EVENT) {
                        slot.reset();
                    }
                }
            }
        }
    }
}
