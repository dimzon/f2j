package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.fallout2ce.MouseManager.FoundEntry;
import org.fallout2ce.MouseManager.MouseType;

/**
 * {@code [mousewin]} cursor-cache vectors from
 * {@code docker/refharness/golden/v38.txt} (descriptors synthesized here,
 * pixels from the shared PCX fixture).
 */
class MouseManagerTest {

    @TempDir
    Path temp;

    private byte[] savedDatafilePalette;

    @BeforeEach
    void setup() throws Exception {
        Kb.resetForTests();
        Mouse.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        WindowManager.resetForTests();
        MouseManager.resetForTests();
        Db.resetForTests();

        savedDatafilePalette = Datafile.datafilePalette.clone();

        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;
        Mouse.mouseBlit = (src, srcPitch, unused, srcX, srcY, srcWidth, srcHeight, destX, destY) -> {
        };

        for (String name : new String[] { "cur.pcx", "cur.anim", "ptr.txt", "bad.anim" }) {
            try (InputStream in = getClass().getResourceAsStream("/fixtures/" + name)) {
                assertNotNull(in, name);
                Files.write(temp.resolve(name), in.readAllBytes());
            }
        }
        assertTrue(XFile.xbaseOpen(temp.toString()));

        assertEquals(0, Mouse.mouseInit());
        Mouse.mouseShowCursor();
    }

    @AfterEach
    void teardown() {
        MouseManager.resetForTests();
        Mouse.resetForTests();
        Kb.resetForTests();
        Input.resetForTests();
        DInput.resetForTests();
        WindowManager.resetForTests();
        Db.resetForTests();
        System.arraycopy(savedDatafilePalette, 0, Datafile.datafilePalette, 0,
            savedDatafilePalette.length);
        Mouse.mouseBlit = null;
    }

    @Test
    void animShapeAndMissing() {
        assertTrue(MouseManager.mouseManagerSetFrame("cur.anim", 0));

        FoundEntry view = new FoundEntry();
        MouseManager.CacheEntry found =
            MouseManager.mouseManagerFindCacheEntry("cur.anim", view);
        assertNotNull(found);
        assertEquals("cur.anim", found.fileName);
        assertEquals(8, view.width);
        assertEquals(8, view.height);
        assertEquals(MouseType.MOUSE_MANAGER_MOUSE_TYPE_ANIMATED.id(), view.type);
        assertEquals(1, view.hotX);
        assertEquals(1, view.hotY);

        assertTrue(MouseManager.mouseManagerSetFrame("cur.anim", 0));
        assertTrue(MouseManager.mouseManagerSetMousePointer("ptr.txt"));
        assertTrue(MouseManager.mouseManagerSetMouseShape("cur.pcx", 5, 6));
        assertFalse(MouseManager.mouseManagerSetMousePointer("nope.txt"));
        assertFalse(MouseManager.mouseManagerSetFrame("bad.anim", 0));

        // The stale-hot quirk: the entry keeps insert-time hot (3,4),
        // the live frame used call-time hot (5,6).
        FoundEntry cur = new FoundEntry();
        MouseManager.mouseManagerFindCacheEntry("cur.pcx", cur);
        assertEquals(3, cur.hotX);
        assertEquals(4, cur.hotY);
        assertEquals(MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id(), cur.type);
    }

    @Test
    void animatedUpdateAdvances() throws Exception {
        MouseManager.setTimeProviderForTests(() -> Integer.MAX_VALUE);
        assertTrue(MouseManager.mouseManagerSetFrame("cur.anim", 1));
        MouseManager.mouseManagerUpdate();

        FoundEntry view = new FoundEntry();
        MouseManager.mouseManagerFindCacheEntry("cur.anim", view);
        assertEquals(2, view.hotX);
        assertEquals(2, view.hotY);
    }

    @Test
    void resetHideShow() {
        assertTrue(MouseManager.mouseManagerSetFrame("cur.anim", 0));
        MouseManager.mouseManagerResetMouse();

        MouseManager.mouseManagerHideMouse();
        assertTrue(Mouse.cursorIsHidden());
        MouseManager.mouseManagerShowMouse();
        assertFalse(Mouse.cursorIsHidden());

        MouseManager.mouseManagerInit();
        MouseManager.mouseManagerExit();
        FoundEntry view = new FoundEntry();
        MouseManager.CacheEntry stale =
            MouseManager.mouseManagerFindCacheEntry("cur.anim", view);
        // Stale field_32c alias survives the flush (C++ keeps it too);
        // the entry reads back as NONE.
        assertNotNull(stale);
        assertEquals(MouseType.MOUSE_MANAGER_MOUSE_TYPE_NONE.id(), view.type);
    }

    @Test
    void cacheEviction() throws Exception {
        for (int i = 0; i < MouseManager.CACHE_CAPACITY; i++) {
            String name = "e" + i + ".pcx";
            Files.writeString(temp.resolve(name), "x");
            MouseManager.StaticData data = new MouseManager.StaticData();
            data.width = 1;
            data.height = 1;
            data.data = new byte[1];
            int index = MouseManager.mouseManagerInsertCacheEntry(data,
                MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id(), new byte[768], name);
            assertEquals(i, index);
        }

        MouseManager.StaticData last = new MouseManager.StaticData();
        last.width = 1;
        last.height = 1;
        last.data = new byte[1];
        int evicted = MouseManager.mouseManagerInsertCacheEntry(last,
            MouseType.MOUSE_MANAGER_MOUSE_TYPE_STATIC.id(), new byte[768], "last.pcx");
        assertEquals(0, evicted);
        assertNull(MouseManager.mouseManagerFindCacheEntry("e0.pcx", new FoundEntry()));

        MouseManager.mouseManagerFlushCache();
        assertNull(MouseManager.mouseManagerFindCacheEntry("last.pcx", new FoundEntry()));
    }

    @Test
    void providers() {
        assertEquals("n", MouseManager.nameManglerDefault("n"));
        assertEquals(1000, MouseManager.rateProviderDefault());
        MouseManager.timeProviderDefault();

        MouseManager.mouseManagerSetNameMangler(name -> "mangled-" + name);
        try {
            assertFalse(MouseManager.mouseManagerSetFrame("cur.anim", 0));
        } finally {
            MouseManager.resetForTests();
        }
    }
}
