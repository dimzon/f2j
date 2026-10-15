package org.fallout2ce;

/**
 * Allocator callback types, mirroring {@code src/memory_defs.h}.
 *
 * <p>The custom C allocator ({@code memory.cc}) is not ported — Java uses
 * the garbage collector (see PORTING.md). These interfaces preserve the
 * callback shapes for the future {@code memory_manager} seams.
 */
public final class MemoryDefs {

    private MemoryDefs() {
    }

    /** Port of {@code MallocProc}. */
    @FunctionalInterface
    public interface MallocProc {
        Object malloc(long size);
    }

    /** Port of {@code ReallocProc}. */
    @FunctionalInterface
    public interface ReallocProc {
        Object realloc(Object ptr, long newSize);
    }

    /** Port of {@code FreeProc}. */
    @FunctionalInterface
    public interface FreeProc {
        void free(Object ptr);
    }
}
