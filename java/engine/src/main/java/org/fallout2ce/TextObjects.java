package org.fallout2ce;

/**
 * Floating-text state slice, mirroring {@code src/text_object.cc} lifecycle
 * only: {@code textObjectsInit} ({@code text_object_init}),
 * {@code textObjectsReset} ({@code text_object_reset}) and
 * {@code textObjectsFree} ({@code text_object_exit}), plus the delay
 * setters and the enabled flag.
 *
 * <p>No vectors observe text objects; {@link ObjectSystem#objectsInit}
 * calls {@link #textObjectsInit} (like C), so the faithful call shape —
 * buffer store, ticker registration, settings delays, enabled flag —
 * lives here instead of a bare seam.
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>{@code textObjectAdd} (render-buffer build: word wrap, font
 * measure/draw, outline, placement) and {@code textObjectsTicker} (timed
 * expiry + {@code tileWindowRefreshRect}) need the font/tile/render
 * engines (game phase); the list itself is therefore not modelled and
 * {@code textObjectsGetCount} stays 0 headless.</li>
 * <li>The ticker is a stable no-op identity so
 * {@code tickersAdd}/{@code tickersRemove} match C registration
 * exactly; it never fires work headless.</li>
 * <li>Delays read {@link Settings} live on every init, like C
 * ({@code preferences.text_base_delay}/{@code text_line_delay} in
 * seconds, stored in milliseconds).</li>
 * </ul>
 */
public final class TextObjects {

    private TextObjects() {
    }

    /** Port of {@code gTextObjectsCount} (list unported: stays 0 headless). */
    private static int count;

    /** Port of {@code gTextObjectsBaseDelay} (ms). */
    private static int baseDelay;

    /** Port of {@code gTextObjectsLineDelay} (ms). */
    private static int lineDelay;

    /** Port of {@code gTextObjectsWindowWidth}. */
    private static int windowWidth;

    /** Port of {@code gTextObjectsWindowHeight}. */
    private static int windowHeight;

    /** Port of {@code gTextObjectsWindowBuffer} (borrowed, like C). */
    private static byte[] windowBuffer;

    /** Port of {@code text_object_enabled}. */
    private static boolean enabled;

    /** Port of {@code text_object_initialized}. */
    private static boolean initialized;

    /** Stable ticker identity (C {@code textObjectsTicker}); no-op headless. */
    private static final Runnable ticker = TextObjects::textObjectsTicker;

    // 0x4B0130 text_object_init.
    public static int textObjectsInit(byte[] buffer, int width, int height) {
        if (initialized) {
            return -1;
        }

        windowBuffer = buffer;
        windowWidth = width;
        windowHeight = height;
        count = 0;

        Input.tickersAdd(ticker);

        baseDelay = (int) (Settings.settings.preferences.text_base_delay * 1000.0);
        lineDelay = (int) (Settings.settings.preferences.text_line_delay * 1000.0);

        enabled = true;
        initialized = true;

        return 0;
    }

    // 0x4B021C text_object_reset.
    public static int textObjectsReset() {
        if (!initialized) {
            return -1;
        }

        // List free: the render list is unported (game phase), so the count
        // reset is the whole state change.
        count = 0;
        Input.tickersAdd(ticker);

        return 0;
    }

    // 0x4B0280 text_object_exit.
    public static void textObjectsFree() {
        if (initialized) {
            textObjectsReset();
            Input.tickersRemove(ticker);
            initialized = false;
        }
    }

    // 0x4B02A4 text_object_disable.
    public static void textObjectsDisable() {
        enabled = false;
    }

    // 0x4B02B0 text_object_enable.
    public static void textObjectsEnable() {
        enabled = true;
    }

    // 0x4B02C4 text_object_set_base_delay (seconds, min 1.0, like C).
    public static void textObjectsSetBaseDelay(double value) {
        if (value < 1.0) {
            value = 1.0;
        }

        baseDelay = (int) (value * 1000.0);
    }

    // 0x4B031C text_object_set_line_delay (seconds, min 0.0, like C).
    public static void textObjectsSetLineDelay(double value) {
        if (value < 0.0) {
            value = 0.0;
        }

        lineDelay = (int) (value * 1000.0);
    }

    /** Test seam: initialization flag. */
    static boolean isInitialized() {
        return initialized;
    }

    /** Test seam: enabled flag. */
    static boolean isEnabled() {
        return enabled;
    }

    // 0x4B07F8 text_object_bk: timed expiry + tile refresh need the
    // tile/render engines (game phase); no-op headless.
    private static void textObjectsTicker() {
    }

    /** Test seam: fresh-process mirror (also unregisters the ticker). */
    static void resetForTests() {
        count = 0;
        baseDelay = 0;
        lineDelay = 0;
        windowWidth = 0;
        windowHeight = 0;
        windowBuffer = null;
        enabled = false;
        initialized = false;
        Input.tickersRemove(ticker);
    }
}
