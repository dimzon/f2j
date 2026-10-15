package org.fallout2ce.platform.awt;

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.fallout2ce.spi.PlatformPlugin;
import org.fallout2ce.spi.Sleeper;
import org.fallout2ce.spi.SystemClock;
import org.fallout2ce.spi.TickerRegistry;
import org.fallout2ce.spi.VideoBackend;

/**
 * Working display backend on plain JRE APIs ({@code -Df2j.platform=awt}).
 *
 * <p>The game renders 8-bit indexed pixels into {@link #framebuffer()}; this
 * backend expands them through the 6-bit palette (exactly like
 * {@code directDrawSetPalette}: {@code component << 2}) into a
 * {@link BufferedImage} on every {@link #present()}.
 *
 * <p>No SDL, DirectX or other third-party native bindings are used. The top
 * window opens lazily via {@link #openWindow(String)} and only when a display
 * exists; everything else (framebuffer, palette, offscreen image) works
 * fully headless, which is what the tests assert.
 */
public final class AwbVideoBackend implements VideoBackend {

    public static final String NAME = "awt";

    /** Game default resolution (matches the engine's 640x480 mode). */
    public static final int DEFAULT_WIDTH = 640;
    public static final int DEFAULT_HEIGHT = 480;

    private final Object lock = new Object();

    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private byte[] framebuffer = new byte[DEFAULT_WIDTH * DEFAULT_HEIGHT];
    private final byte[] palette = new byte[256 * 3];
    private volatile BufferedImage image = newImage(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    private volatile JFrame window;

    /** Fresh surfaces start with the identity gray ramp (like SDL). */
    public AwbVideoBackend() {
        for (int i = 0; i < 256; i++) {
            palette[i * 3] = (byte) i;
            palette[i * 3 + 1] = (byte) i;
            palette[i * 3 + 2] = (byte) i;
        }
    }

    /**
     * Offscreen image in the pipeline-optimal format: a compatible image
     * lives where the JRE's standard pipeline (OpenGL with
     * {@code -Dsun.java2d.opengl=True}, Direct3D, Metal) can blit it
     * accelerated; headless falls back to a plain heap image. (A
     * {@code VolatileImage} would be faster still but needs content-loss
     * handling; a managed compatible image is the pragmatic choice.)
     */
    private static BufferedImage newImage(int w, int h) {
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                return GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration()
                        .createCompatibleImage(w, h);
            } catch (Exception e) {
                // Fall through to the heap image below.
            }
        }
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    /** Framebuffer width in pixels. */
    public int width() {
        return width;
    }

    /** Framebuffer height in pixels. */
    public int height() {
        return height;
    }

    /** Current surface palette copy (zeros after {@link #freeSurface()}). */
    @Override
    public byte[] palette() {
        synchronized (lock) {
            return palette.clone();
        }
    }

    /** Release the drawing surface (palette and pixels go to zero). */
    @Override
    public void freeSurface() {
        synchronized (lock) {
            Arrays.fill(palette, (byte) 0);
            Arrays.fill(framebuffer, (byte) 0);
        }
    }

    @Override
    public boolean isExclusiveFullscreen() {
        return false;
    }

    /**
     * Direct indexed framebuffer (row-major, one byte per pixel). The game
     * blits here; {@link #present()} expands it through the palette.
     */
    public byte[] framebuffer() {
        return framebuffer;
    }

    /** Resize the framebuffer (cleared, like {@code _GNW95_zero_vid_mem}). */
    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("size: " + newWidth + "x" + newHeight);
        }
        synchronized (lock) {
            width = newWidth;
            height = newHeight;
            framebuffer = new byte[newWidth * newHeight];
            image = newImage(newWidth, newHeight);
        }
    }

    /** Last presented image (offscreen; the test seam). */
    public BufferedImage image() {
        return image;
    }

    @Override
    public void setPalette(byte[] rgb768) {
        if (rgb768.length != palette.length) {
            throw new IllegalArgumentException("palette: " + rgb768.length);
        }
        synchronized (lock) {
            System.arraycopy(rgb768, 0, palette, 0, palette.length);
        }
    }

    @Override
    public void setPaletteEntries(byte[] rgb, int start, int count) {
        if (start < 0 || count < 0 || start + count > 256 || rgb.length < count * 3) {
            throw new IllegalArgumentException("range: " + start + "+" + count);
        }
        synchronized (lock) {
            System.arraycopy(rgb, 0, palette, start * 3, count * 3);
        }
    }

    @Override
    public void present() {
        final BufferedImage frame;
        final byte[] pixels;
        final byte[] cmap;
        final int w;
        final int h;
        synchronized (lock) {
            w = width;
            h = height;
            pixels = framebuffer;
            cmap = palette.clone();
            frame = image;
        }
        int[] rgb = new int[w * h];
        for (int i = 0; i < rgb.length; i++) {
            int index = Byte.toUnsignedInt(pixels[i]) * 3;
            int r = Byte.toUnsignedInt(cmap[index]) << 2;
            int g = Byte.toUnsignedInt(cmap[index + 1]) << 2;
            int b = Byte.toUnsignedInt(cmap[index + 2]) << 2;
            rgb[i] = r << 16 | g << 8 | b;
        }
        frame.setRGB(0, 0, w, h, rgb, 0, w);
        JFrame shown = window;
        if (shown != null) {
            shown.repaint();
        }
    }

    /**
     * Open the top window (no-op where no display exists). Safe to call from
     * any thread; idempotent.
     */
    public void openWindow(String title) {
        if (GraphicsEnvironment.isHeadless() || window != null) {
            return;
        }
        try {
            EventQueue.invokeAndWait(() -> {
                if (window != null) {
                    return;
                }
                JFrame frame = new JFrame(title);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JPanel canvas = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                    }
                };
                frame.setContentPane(canvas);
                frame.pack();
                frame.setSize(width, height);
                frame.setLocationRelativeTo(null);
                window = frame;
                frame.setVisible(true);
            });
        } catch (Exception e) {
            throw new IllegalStateException("openWindow", e);
        }
    }

    /** Whether the top window is currently open. */
    public boolean isWindowOpen() {
        return window != null;
    }

    /** Close the top window if open (no-op otherwise, any thread). */
    public void closeWindow() {
        JFrame shown = window;
        if (shown == null) {
            return;
        }
        window = null;
        EventQueue.invokeLater(shown::dispose);
    }

    /** The {@code awt} platform provider (video + managed system clock/sleep/tickers). */
    public static final class Provider implements PlatformPlugin {

        private final AwbVideoBackend video = new AwbVideoBackend();

        private final SystemClock clock = System::currentTimeMillis;

        private final Sleeper sleeper = ms -> {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        private final TickerRegistry tickers = new TickerRegistry() {
            @Override
            public void add(Runnable ticker) {
            }

            @Override
            public void remove(Runnable ticker) {
            }
        };

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public VideoBackend video() {
            return video;
        }

        @Override
        public SystemClock clock() {
            return clock;
        }

        @Override
        public Sleeper sleeper() {
            return sleeper;
        }

        @Override
        public TickerRegistry tickers() {
            return tickers;
        }

        /** Direct access to the display backend (framebuffer, window). */
        public AwbVideoBackend display() {
            return video;
        }
    }

    @Override
    public String toString() {
        return "AwbVideoBackend[" + width + "x" + height + "]";
    }
}
