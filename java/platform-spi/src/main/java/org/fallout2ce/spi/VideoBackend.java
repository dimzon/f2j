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
}
