package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Script-window id tables, mirroring the pure parts of {@code src/window.h}.
 * The script-window engine ({@code window.cc}) comes with the UI phase.
 */
public final class Window {

    private Window() {
    }

    public enum TextAlignment {
        TEXT_ALIGNMENT_LEFT(0),
        TEXT_ALIGNMENT_RIGHT(1),
        TEXT_ALIGNMENT_CENTER(2);

        private final int id;

        TextAlignment(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, TextAlignment> ALIGN_BY_ID = new HashMap<>();

    static {
        for (TextAlignment e : TextAlignment.values()) {
            ALIGN_BY_ID.put(e.id, e);
        }
    }

    public static TextAlignment ofTextAlignment(int id) {
        return ALIGN_BY_ID.get(id);
    }

    public enum ManagedButtonMouseEvent {
        MANAGED_BUTTON_MOUSE_EVENT_BUTTON_DOWN(0),
        MANAGED_BUTTON_MOUSE_EVENT_BUTTON_UP(1),
        MANAGED_BUTTON_MOUSE_EVENT_ENTER(2),
        MANAGED_BUTTON_MOUSE_EVENT_EXIT(3);

        private final int id;

        ManagedButtonMouseEvent(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public static final int MANAGED_BUTTON_MOUSE_EVENT_COUNT = 4;

    private static final Map<Integer, ManagedButtonMouseEvent> MBEV_BY_ID = new HashMap<>();

    static {
        for (ManagedButtonMouseEvent e : ManagedButtonMouseEvent.values()) {
            MBEV_BY_ID.put(e.id, e);
        }
    }

    public static ManagedButtonMouseEvent ofManagedButtonMouseEvent(int id) {
        return MBEV_BY_ID.get(id);
    }

    public enum ManagedButtonRightMouseEvent {
        MANAGED_BUTTON_RIGHT_MOUSE_EVENT_BUTTON_DOWN(0),
        MANAGED_BUTTON_RIGHT_MOUSE_EVENT_BUTTON_UP(1);

        private final int id;

        ManagedButtonRightMouseEvent(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public static final int MANAGED_BUTTON_RIGHT_MOUSE_EVENT_COUNT = 2;

    private static final Map<Integer, ManagedButtonRightMouseEvent> MRBEV_BY_ID = new HashMap<>();

    static {
        for (ManagedButtonRightMouseEvent e : ManagedButtonRightMouseEvent.values()) {
            MRBEV_BY_ID.put(e.id, e);
        }
    }

    public static ManagedButtonRightMouseEvent ofManagedButtonRightMouseEvent(int id) {
        return MRBEV_BY_ID.get(id);
    }

    /** Port of {@code WINDOWDRAWINGPROC}. */
    @FunctionalInterface
    public interface WindowDrawingProc {
        void draw(byte[] src, int srcPitch, int unused, int srcX, int srcY,
            int srcWidth, int srcHeight, int destX, int destY);
    }

    /** Port of {@code WindowDrawingProc2}. */
    @FunctionalInterface
    public interface WindowDrawingProc2 {
        void draw(byte[] buf, int a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, byte a10);
    }

    /** Port of {@code WindowInputHandler}. */
    @FunctionalInterface
    public interface WindowInputHandler {
        boolean handle(int key);
    }

    /** Port of {@code WindowDeleteCallback}. */
    @FunctionalInterface
    public interface WindowDeleteCallback {
        void handle(int windowIndex, String windowName);
    }

    /** Port of {@code DisplayInWindowCallback}. */
    @FunctionalInterface
    public interface DisplayInWindowCallback {
        void handle(int windowIndex, String windowName, byte[] data, int width, int height);
    }

    /** Port of {@code ManagedButtonMouseEventCallback} ({@code void*} as Object). */
    @FunctionalInterface
    public interface ManagedButtonMouseEventCallback {
        void handle(Object userData, int eventType);
    }

    /** Port of {@code ManagedWindowCreateCallback}. */
    @FunctionalInterface
    public interface ManagedWindowCreateCallback {
        void handle(int windowIndex, String windowName, int[] flagsPtr);
    }
}
