package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [artload]} vectors from {@code docker/refharness/golden/v12.txt}
 * plus codec round-trips (hermetic).
 */
class ArtLoadTest {

    @AfterEach
    void reset() {
        Art.resetForTests();
        Db.resetForTests();
    }

    static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        Path p = Paths.get(root, name);
        assumeTrue(Files.isRegularFile(p), "missing " + p);
        return p.toString();
    }

    static void mountGameEn() {
        String gameEn = Paths.get(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/patch000.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/critter.dat"));
        assertEquals(0, Art.artInit());
    }

    @Test
    void loadsFrm() {
        mountGameEn();

        Art.ArtData art = Art.artLoad("art\\intrface\\mainmenu.frm");
        assertNotNull(art);
        assertEquals(1, Art.artGetFrameCount(art));
        assertEquals(640, Art.artGetWidth(art, 0, Rotation.NE.value()));

        assertNull(Art.artLoad("art\\no\\such.frm"));
        assertNull(Art.artLoad("text\\english\\game\\misc.msg"));
        assertNull(Art.artLoad(null));
    }

    @Test
    void fidImageLock() {
        mountGameEn();

        FrmId menu = FrmId.ofType(ObjectType.INTERFACE, 140);
        try (Art.FrmImage image = new Art.FrmImage()) {
            assertTrue(image.lock(menu));
            assertEquals(640, image.getWidth());
            assertEquals(480, image.getHeight());
            assertEquals(0, image.getXOffset());
            assertEquals(239, image.getYOffset());
            assertEquals(0xE4F6BDB0L, crc32(image.getData()));
        }

        try (Art.FrmImage named = new Art.FrmImage()) {
            assertTrue(named.lock("art\\intrface\\mainmenu.frm"));
            assertEquals(640, named.getWidth());
        }

        try (Art.FrmImage rel = new Art.FrmImage()) {
            assertTrue(rel.lock(ObjectType.INTERFACE.value(), "mainmenu.frm"));
            assertEquals(640, rel.getWidth());
        }

        try (Art.FrmImage bad = new Art.FrmImage()) {
            assertTrue(!bad.lock(menu, 999, Rotation.NE.value()));
        }

        FrmId noSuch = FrmId.ofType(ObjectType.INTERFACE, 4095);
        try (Art.FrmImage missing = new Art.FrmImage()) {
            assertTrue(!missing.lock(noSuch));
        }
    }

    @Test
    void artLockTrio() {
        mountGameEn();

        FrmId menu = FrmId.ofType(ObjectType.INTERFACE, 140);
        Art.ArtLock lock = Art.artLock(menu);
        assertNotNull(lock);
        assertEquals(1, Art.artUnlock(lock));
        assertEquals(1, Art.artCacheFlush());
    }

    @Test
    void codecRoundTrip() {
        mountGameEn();

        Art.ArtData art = Art.artLoad("art\\critters\\hmjmpsab.frm");
        assertNotNull(art);

        byte[] flat = ArtCodec.serialize(art);
        Art.ArtData back = ArtCodec.deserialize(flat);
        assertEquals(art.version, back.version);
        assertEquals(art.frameCount, back.frameCount);
        assertArrayEquals(art.dataOffsets, back.dataOffsets);
        assertArrayEquals(art.padding, back.padding);
        assertEquals(art.frames.size(), back.frames.size());

        // Re-serialization is byte-identical (canonical layout).
        assertArrayEquals(flat, ArtCodec.serialize(back));

        for (int r = 0; r < Rotation.COUNT; r++) {
            assertArrayEquals(Art.artGetFrameData(art, 0, r), Art.artGetFrameData(back, 0, r));
        }
    }
}
