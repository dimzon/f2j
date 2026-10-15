package org.fallout2ce;

/**
 * Opaque script-program handle, standing in for {@code Program} from
 * {@code src/interpreter.h}. Used by the script-window API ({@code window.h})
 * before the interpreter engine lands; the shape is filled in with the
 * interpreter phase, and program execution goes through its seams.
 */
public class Program {

    protected Program() {
    }
}
