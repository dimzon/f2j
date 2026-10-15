package org.fallout2ce;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

/**
 * Port of {@code src/svga.h} / {@code src/svga.cc} (video modes, blits,
 * palette management, present).
 *
 * <p>The SDL window/renderer/texture trio is replaced by the platform
 * {@code VideoBackend} (see {@code Color#video}): its framebuffer plays the
 * role of {@code gSdlSurface->pixels}, so the surface-to-texture copy in
 * {@code _GNW95_ShowRect} is the same pixels and needs no second copy.
 * Dest pitch is always the mode width — the managed framebuffer is tightly
 * packed, where SDL pads rows to 4 bytes (all stock modes are multiples
 * of 4, so this is unobservable in practice).
 *
 * <p>Not ported yet: {@code renderFpsCounter} (needs {@code text_font}),
 * the {@code _mouse_blit*} hooks (need {@code mouse}), movie overlay calls
 * are seams below.
 */
public final class Svga {

    private Svga() {
    }

    /** Port of {@code _scr_size}. */
    public static final Geometry.Rect scrSize = new Geometry.Rect();

    /** Window-mode ids (C {@code WindowMode}). */
    public static final int WINDOWMODE_FULLSCREEN = 0;
    public static final int WINDOWMODE_WINDOWED = 1;
    public static final int WINDOWMODE_WINDOWED_FULLSCREEN = 2;

    /** Blit proc ({@code _scr_blit} in C). */
    public interface ShowRect {
        void show(byte[] src, int srcPitch, int unused,
                int srcX, int srcY, int srcWidth, int srcHeight,
                int destX, int destY);
    }

    /** Port of {@code _scr_blit} (preset, as in C). */
    public static ShowRect scrBlit = Svga::showRect;

    /** Port of {@code _zero_mem} (starts null, as in C). */
    public static Runnable zeroMem;

    /** Port of {@code gProgramWindowTitle}. */
    public static String programWindowTitle = "";

    /** Port of {@code gProgramIsActive}. */
    public static BooleanSupplier programActive = () -> true;

    /** Port of {@code movieRenderDirectOverlay} (movie.cc, not ported yet). */
    public static Runnable movieOverlay = () -> {
    };

    /** Port of {@code movieHandleRendererReset} (movie.cc, not ported yet). */
    public static Runnable movieRendererReset = () -> {
    };

    /** Port of {@code mouseDeviceRefreshWindowMapping} (mouse, not ported yet). */
    public static Runnable mouseRefreshMapping = () -> {
    };

    /** Current surface width (dest pitch for blits). */
    private static int surfaceWidth;

    /** Current surface height. */
    private static int surfaceHeight;

    /**
     * Whether a surface exists. {@code directDrawGetPalette} reads the
     * backend only then; otherwise it returns the stale cache — mirroring
     * the C static buffer (which keeps the last palette after
     * {@code directDrawFree}).
     */
    private static boolean surfaceExists;

    /** Last palette handed out (C static buffer in disguise). */
    private static byte[] lastPalette = new byte[768];

    /** Port of {@code _init_mode_320_200}. */
    public static int initMode320x200() {
        return gnw95InitModeEx(320, 200, 8);
    }

    /** Port of {@code _init_mode_320_400}. */
    public static int initMode320x400() {
        return gnw95InitModeEx(320, 400, 8);
    }

    /** Port of {@code _init_mode_640_480_16} (always fails, as in C). */
    public static int initMode640x480_16() {
        return -1;
    }

    /** Port of {@code _init_mode_640_480}. */
    public static int initMode640x480() {
        return initVesaMode(640, 480);
    }

    /** Port of {@code _init_mode_640_400}. */
    public static int initMode640x400() {
        return initVesaMode(640, 400);
    }

    /** Port of {@code _init_mode_800_600}. */
    public static int initMode800x600() {
        return initVesaMode(800, 600);
    }

    /** Port of {@code _init_mode_1024_768}. */
    public static int initMode1024x768() {
        return initVesaMode(1024, 768);
    }

    /** Port of {@code _init_mode_1280_1024}. */
    public static int initMode1280x1024() {
        return initVesaMode(1280, 1024);
    }

    /** Port of {@code _get_start_mode_} (no-op, as in C). */
    public static void getStartMode() {
    }

    /** Port of {@code _zero_vid_mem} (dispatches via {@link #zeroMem}). */
    public static void zeroVidMem() {
        if (zeroMem != null) {
            zeroMem.run();
        }
    }

    /**
     * Port of {@code _GNW95_init_mode_ex}. The arguments are ignored in
     * favor of the settings, exactly like C. The SDL event pump has no
     * managed equivalent.
     */
    public static int gnw95InitModeEx(int width, int height, int bpp) {
        width = Settings.settings.screen.resolution_x;
        height = Settings.settings.screen.resolution_y;
        int scale = Settings.settings.screen.scale;

        // Only allow scaling if resulting game resolution is >= 640x480.
        if (width / scale < 640 || height / scale < 480) {
            scale = 1;
        } else {
            width /= scale;
            height /= scale;
        }

        try {
            Color.video.openWindow(programWindowTitle);
        } catch (RuntimeException e) {
            return -1;
        }

        if (directDrawInit(width, height, bpp) == -1) {
            return -1;
        }

        scrSize.left = 0;
        scrSize.top = 0;
        scrSize.right = width - 1;
        scrSize.bottom = height - 1;

        scrBlit = Svga::showRect;
        zeroMem = Svga::gnw95ZeroVidMem;
        // NOTE: _mouse_blit_trans/_mouse_blit need mouse.cc (not ported yet).

        return 0;
    }

    /** Port of {@code _init_vesa_mode}. */
    public static int initVesaMode(int width, int height) {
        return gnw95InitModeEx(width, height, 8);
    }

    /**
     * Port of {@code directDrawInit}. Bit depth is accepted and ignored
     * (callers only ever pass 8; the 16-bit path returns early elsewhere).
     * The palette is preserved across re-inits by the backend, like the C
     * save/free/restore path.
     */
    public static int directDrawInit(int width, int height, int bpp) {
        try {
            Color.video.resize(width, height);
        } catch (IllegalArgumentException e) {
            surfaceExists = false;
            return -1;
        }
        surfaceWidth = width;
        surfaceHeight = height;
        surfaceExists = true;
        return 0;
    }

    /** Port of {@code directDrawFree}. */
    public static void directDrawFree() {
        Color.video.freeSurface();
        surfaceExists = false;
    }

    /** Port of {@code directDrawGetPalette} (stale cache included). */
    public static byte[] directDrawGetPalette() {
        if (surfaceExists) {
            lastPalette = Color.video.palette();
        }
        return lastPalette.clone();
    }

    /**
     * Port of {@code _GNW95_ShowRect} (row copy from draw.cc
     * {@code srcCopy}; the texture-surface copy addresses the same pixels
     * in the managed model).
     */
    public static void showRect(byte[] src, int srcPitch, int unused,
            int srcX, int srcY, int srcWidth, int srcHeight,
            int destX, int destY) {
        byte[] dest = Color.video.framebuffer();
        for (int y = 0; y < srcHeight; y++) {
            System.arraycopy(src, (srcY + y) * srcPitch + srcX,
                    dest, (destY + y) * surfaceWidth + destX, srcWidth);
        }
        Color.video.present();
    }

    /** Port of {@code _GNW95_zero_vid_mem}. */
    public static void gnw95ZeroVidMem() {
        if (!programActive.getAsBoolean()) {
            return;
        }
        Arrays.fill(Color.video.framebuffer(), (byte) 0);
        Color.video.present();
    }

    /** Port of {@code screenGetWidth}. */
    public static int screenGetWidth() {
        return Geometry.rectGetWidth(scrSize);
    }

    /** Port of {@code screenGetHeight}. */
    public static int screenGetHeight() {
        return Geometry.rectGetHeight(scrSize);
    }

    /** Port of {@code screenGetVisibleHeight}. */
    public static int screenGetVisibleHeight() {
        // interface.h INTERFACE_BAR_HEIGHT (interface.cc not ported yet).
        int windowBottomMargin = 0;
        if (!Settings.settings.ui.iface_bar_mode) {
            windowBottomMargin = 100;
        }
        return screenGetHeight() - windowBottomMargin;
    }

    /** Port of {@code screenIsExclusiveFullscreen}. */
    public static boolean screenIsExclusiveFullscreen() {
        return Color.video.isExclusiveFullscreen();
    }

    /** Port of {@code handleWindowSizeChanged}. */
    public static void handleWindowSizeChanged() {
        movieRendererReset.run();
        Color.video.resize(screenGetWidth(), screenGetHeight());
        mouseRefreshMapping.run();
    }

    /** Port of {@code renderPresent}. */
    public static void renderPresent() {
        movieOverlay.run();
        Color.video.present();
    }

    /** Test-only full reset (mirrors the C static initial state). */
    static void resetForTests() {
        scrSize.left = 0;
        scrSize.top = 0;
        scrSize.right = 0;
        scrSize.bottom = 0;
        scrBlit = Svga::showRect;
        zeroMem = null;
        programWindowTitle = "";
        programActive = () -> true;
        movieOverlay = () -> {
        };
        movieRendererReset = () -> {
        };
        mouseRefreshMapping = () -> {
        };
        surfaceWidth = 0;
        surfaceHeight = 0;
        surfaceExists = false;
        lastPalette = new byte[768];
    }
}
