package org.fallout2ce;

/**
 * Port of {@code src/autorun.h} / {@code src/autorun.cc}
 * (0x4139C0 {@code autorun_mutex_create}, 0x413A00
 * {@code autorun_mutex_destroy}).
 *
 * <p>NOTE: on {@code _WIN32} C creates the named
 * {@code InterplayGenericAutorunMutex} to enforce a single desktop instance
 * and returns false when it already exists; on every other platform it
 * returns true unconditionally. Java always takes the non-Win32 path
 * ({@code return true} / no-op close): a single desktop instance is a
 * launcher concern, the same rationale under which {@link WindowManager}
 * skips the Win32 single-instance mutex/title in
 * {@code windowManagerInit} ("launcher concern").
 */
public final class Autorun {

    private Autorun() {
    }

    // 0x4139C0 autorun_mutex_create.
    public static boolean autorunMutexCreate() {
        return true;
    }

    // 0x413A00 autorun_mutex_destroy.
    public static void autorunMutexClose() {
    }
}
