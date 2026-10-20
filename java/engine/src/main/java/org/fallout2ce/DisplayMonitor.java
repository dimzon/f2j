package org.fallout2ce;

/**
 * The message monitor - the box in the corner of the screen that collects
 * what the game says.
 *
 * <p>The original keeps a fixed buffer of wrapped lines and shows the last
 * of them. Wrapping a line needs the font's metrics, so that part waits for
 * the font and the window; until then the monitor is not initialised and
 * adding a message does nothing, which is what the original does too when the
 * monitor has not been set up.
 */
final class DisplayMonitor {

    /** The knob the original puts in front of the first line of a message. */
    static final char KNOB = '\u0095';

    /** The line buffer, as a flat list of lines. */
    private static final java.util.List<String> LINES = new java.util.ArrayList<>();

    /** How many lines the buffer holds. */
    static final int LINES_CAPACITY = 6;

    /** Set once the monitor is on screen. */
    private static boolean initialized;

    private DisplayMonitor() {
    }

    /** The messages the monitor is holding, oldest first. */
    static java.util.List<String> lines() {
        return LINES;
    }

    /**
     * Port of {@code displayMonitorAddMessage} (src/display_monitor.cc).
     *
     * <p>Without a monitor there is nowhere to put the message, and the
     * original returns early in the same case.
     */
    static void addMessage(String message) {
        if (!initialized || message == null) {
            return;
        }

        LINES.add(message);
        while (LINES.size() > LINES_CAPACITY) {
            LINES.remove(0);
        }
    }
}
