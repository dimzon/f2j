package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.fallout2ce.platform.awt.AwbVideoBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Device layer against golden v25 ({@code [dinput]}): init/mode quirk,
 * wheel drain, mapping math, warp guards.
 */
class DInputTest {

    private boolean savedMouseLock;
    private boolean mouseLockSaved;

    @AfterEach
    void reset() {
        DInput.resetForTests();
        Input.resetForTests();
        Svga.resetForTests();
        Platform.resetForTests();
        if (mouseLockSaved) {
            Settings.settings.screen.mouse_lock = savedMouseLock;
            mouseLockSaved = false;
        }
    }

    void mouseLock(boolean value) {
        if (!mouseLockSaved) {
            savedMouseLock = Settings.settings.screen.mouse_lock;
            mouseLockSaved = true;
        }
        Settings.settings.screen.mouse_lock = value;
    }

    static class FakeMouse implements DInput.MouseDevice {
        int x;
        int y;
        int dx;
        int dy;
        boolean left;
        boolean right;

        @Override
        public int x() {
            return x;
        }

        @Override
        public int y() {
            return y;
        }

        @Override
        public int dx() {
            return dx;
        }

        @Override
        public int dy() {
            return dy;
        }

        @Override
        public boolean left() {
            return left;
        }

        @Override
        public boolean right() {
            return right;
        }
    }

    @Test
    void initAndAcquire() {
        mouseLock(false);

        assertFalse(DInput.mouseDeviceUsesRelativeMode());
        assertTrue(DInput.directInputInit());
        assertTrue(DInput.mouseDeviceAcquire());
        assertTrue(DInput.keyboardDeviceAcquire());
        assertTrue(DInput.keyboardDeviceGetData());
        assertTrue(DInput.keyboardDeviceUnacquire());
        assertTrue(DInput.mouseDeviceUnacquire());
        DInput.mouseDeviceFree();
        DInput.keyboardDeviceFree();
        DInput.directInputFree();
    }

    @Test
    void getDataAbsolute() {
        mouseLock(false);
        assertTrue(DInput.directInputInit());
        FakeMouse mouse = new FakeMouse();
        mouse.x = 100;
        mouse.y = 50;
        mouse.left = true;
        DInput.mouse = mouse;
        DInput.windowSize = new DInput.WindowSize() {
            @Override
            public int width() {
                return 1280;
            }

            @Override
            public int height() {
                return 960;
            }
        };
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;
        DInput.mouseDeviceRefreshWindowMapping();

        DInput.addMouseWheel(3, -2);
        DInput.MouseData data = DInput.mouseDeviceGetData();

        assertEquals(50, data.x());
        assertEquals(25, data.y());
        assertTrue(data.left());
        assertFalse(data.right());
        assertEquals(3, data.wheelX());
        assertEquals(-2, data.wheelY());

        // Wheel drains on read, like C.
        DInput.MouseData again = DInput.mouseDeviceGetData();
        assertEquals(0, again.wheelX());
        assertEquals(0, again.wheelY());
    }

    @Test
    void relativeQuirkMatchesGolden() {
        List<Boolean> sets = new ArrayList<>();
        DInput.relativeMode = relative -> {
            sets.add(relative);
            return false;
        };
        mouseLock(true);

        // Dummy/SDL-less drivers reject relative mode, but the flag sticks.
        assertFalse(DInput.mouseDeviceInitMode());
        assertTrue(DInput.mouseDeviceUsesRelativeMode());

        mouseLock(false);
        DInput.relativeMode = relative -> true;
        assertTrue(DInput.mouseDeviceInitMode());
        assertFalse(DInput.mouseDeviceUsesRelativeMode());
        assertEquals(List.of(true), sets);
    }

    @Test
    void relativeReadsDeltas() {
        mouseLock(true);
        FakeMouse mouse = new FakeMouse();
        mouse.dx = 7;
        mouse.dy = -3;
        DInput.mouse = mouse;
        assertTrue(DInput.directInputInit());
        assertTrue(DInput.mouseDeviceUsesRelativeMode());

        DInput.MouseData data = DInput.mouseDeviceGetData();

        assertEquals(7, data.x());
        assertEquals(-3, data.y());
    }

    @Test
    void mappingMath() {
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;
        DInput.windowSize = new DInput.WindowSize() {
            @Override
            public int width() {
                return 1280;
            }

            @Override
            public int height() {
                return 960;
            }
        };
        DInput.mouseDeviceRefreshWindowMapping();

        assertEquals(50, DInput.mapWindowToLogicalPosition(100, 50)[0]);
        assertEquals(25, DInput.mapWindowToLogicalPosition(100, 50)[1]);
    }

    @Test
    void mappingWithoutWindowPassesThrough() {
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;
        DInput.mouseDeviceRefreshWindowMapping();

        assertEquals(100, DInput.mapWindowToLogicalPosition(100, 50)[0]);
        assertEquals(50, DInput.mapWindowToLogicalPosition(100, 50)[1]);
    }

    @Test
    void warpGuards() {
        List<int[]> warps = new ArrayList<>();
        DInput.mouseWarp = (x, y) -> warps.add(new int[] { x, y });
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;
        DInput.windowSize = new DInput.WindowSize() {
            @Override
            public int width() {
                return 1280;
            }

            @Override
            public int height() {
                return 960;
            }
        };
        DInput.mouseDeviceRefreshWindowMapping();

        DInput.mouseDeviceSetLogicalPosition(320, 240);
        assertEquals(1, warps.size());
        assertEquals(640, warps.get(0)[0]);
        assertEquals(480, warps.get(0)[1]);

        // Relative mode suppresses the warp (enter it like the quirk test).
        DInput.relativeMode = relative -> false;
        mouseLock(true);
        DInput.mouseDeviceInitMode();
        assertTrue(DInput.mouseDeviceUsesRelativeMode());
        DInput.mouseDeviceSetLogicalPosition(0, 0);
        assertEquals(1, warps.size());
    }

    @Test
    void awtWiring() {
        AwbVideoBackend display = new AwbVideoBackend();
        display.component().dispatchEvent(new java.awt.event.MouseEvent(display.component(),
            java.awt.event.MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 200, 100, 0,
            false));
        display.component().dispatchEvent(new java.awt.event.MouseEvent(display.component(),
            java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 200, 100, 1,
            false, java.awt.event.MouseEvent.BUTTON1));
        display.component().dispatchEvent(new java.awt.event.MouseWheelEvent(display.component(),
            java.awt.event.MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, 200, 100, 0,
            false, java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));

        // Game boot wires the backend tracker into the device seam.
        DInput.mouse = new DInput.MouseDevice() {
            @Override
            public int x() {
                return display.mouseX();
            }

            @Override
            public int y() {
                return display.mouseY();
            }

            @Override
            public int dx() {
                return 0;
            }

            @Override
            public int dy() {
                return 0;
            }

            @Override
            public boolean left() {
                return display.mouseLeft();
            }

            @Override
            public boolean right() {
                return display.mouseRight();
            }
        };
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;
        DInput.mouseDeviceRefreshWindowMapping();
        int[] wheel = display.takeWheel();
        DInput.addMouseWheel(wheel[0], wheel[1]);

        DInput.MouseData data = DInput.mouseDeviceGetData();

        assertEquals(200, data.x());
        assertEquals(100, data.y());
        assertTrue(data.left());
        assertEquals(-1, data.wheelY());
    }

    @Test
    void keyboardResetRunsSeams() {        int[] calls = { 0, 0 };
        Input.platformEventDrain = () -> calls[0]++;
        Input.sfallClearKeys = () -> calls[1]++;

        assertTrue(DInput.keyboardDeviceReset());

        assertEquals(1, calls[0]);
        assertEquals(1, calls[1]);
        Input.platformEventDrain = () -> {
        };
        Input.sfallClearKeys = () -> {
        };
    }
}
