package org.fallout2ce.spi;

/**
 * Display backend (C {@code directDraw*} in {@code svga.cc}).
 *
 * <p>Managed providers render nowhere (headless null backend); native
 * providers (SDL/OpenGL, separate modules) do the real work.
 */
public interface VideoBackend {

    /** Port of {@code directDrawSetPalette}. */
    void setPalette(byte[] rgb768);

    /** Port of {@code directDrawSetPaletteInRange}. */
    void setPaletteEntries(byte[] rgb, int start, int count);

    /** Port of {@code renderPresent} (without the fps-counter overlay). */
    void present();

    /**
     * Open the top window (ports the window half of
     * {@code _GNW95_init_window}). Idempotent; ignored where there is no
     * display to open on.
     */
    default void openWindow(String title) {
    }

    /**
     * Resize the drawing surface (ports {@code directDrawInit}). The palette
     * is preserved, like the C re-init path.
     */
    default void resize(int width, int height) {
    }

    /**
     * Direct indexed framebuffer (ports {@code gSdlSurface->pixels}). The
     * game blits here; {@link #present()} shows it.
     */
    default byte[] framebuffer() {
        return new byte[0];
    }

    /**
     * Current surface palette (ports {@code directDrawGetPalette}). All
     * zeros when there is no surface.
     */
    default byte[] palette() {
        return new byte[256 * 3];
    }

    /** Release the drawing surface (ports {@code directDrawFree}). */
    default void freeSurface() {
    }

    /** Port of {@code screenIsExclusiveFullscreen}. */
    default boolean isExclusiveFullscreen() {
        return false;
    }
}
