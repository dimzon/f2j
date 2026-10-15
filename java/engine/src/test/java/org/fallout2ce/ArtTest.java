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
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code [art]} vectors from {@code docker/refharness/golden/v11.txt}.
 * List/path tests mount game data ({@code F2J_GAMEDATA}); the decoder is
 * covered hermetically with a synthetic FRM.
 */
class ArtTest {

    @TempDir
    Path temp;

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
    void artListsAndPaths() {
        mountGameEn();

        assertEquals(140, Art.artListIndex(ObjectType.INTERFACE.value(), "mainmenu"));
        assertEquals(-1, Art.artListIndex(ObjectType.INTERFACE.value(), "zzz_nope"));
        assertEquals(-1, Art.artListIndex(ObjectType.INVALID.value(), "mainmenu"));
        assertEquals("intrface", Art.artGetObjectTypeName(ObjectType.INTERFACE.value()));
        assertNull(Art.artGetObjectTypeName(ObjectType.INVALID.value()));

        assertEquals(0, Art.artIsObjectTypeHidden(ObjectType.INTERFACE.value()));
        Art.artToggleObjectTypeHidden(ObjectType.INTERFACE.value());
        assertEquals(1, Art.artIsObjectTypeHidden(ObjectType.INTERFACE.value()));
        Art.artToggleObjectTypeHidden(ObjectType.INTERFACE.value());

        FrmId menu = FrmId.ofType(ObjectType.INTERFACE, 140);
        assertEquals("art\\intrface\\mainmenu.frm", menu.filePath());

        assertEquals(11, Art.artListIndex(ObjectType.CRITTER.value(), "hmjmps"));
        FrmId dude = FrmId.ofCritter(11, AnimationType.WALK.value(),
                WeaponAnimation.NONE.value(), Rotation.NE.value());
        assertEquals("art\\critters\\hmjmpsab.frm", dude.filePath());
        assertTrue(dude.exist());
    }

    @Test
    void realMainmenuFrm() {
        mountGameEn();

        Art.ArtData[] out = new Art.ArtData[1];
        assertEquals(0, Art.artRead("art\\intrface\\mainmenu.frm", out));
        Art.ArtData art = out[0];
        assertEquals(4, art.version);
        assertEquals(1, Art.artGetFramesPerSecond(art));
        assertEquals(0, Art.artGetActionFrame(art));
        assertEquals(1, Art.artGetFrameCount(art));
        assertEquals(640, Art.artGetWidth(art, 0, Rotation.NE.value()));
        assertEquals(480, Art.artGetHeight(art, 0, Rotation.NE.value()));

        int[] x = new int[1];
        int[] y = new int[1];
        assertEquals(0, Art.artGetFrameOffsets(art, 0, Rotation.NE.value(), x, y));
        assertEquals(0, x[0]);
        assertEquals(0, y[0]);
        int[] rx = new int[1];
        int[] ry = new int[1];
        assertEquals(0, Art.artGetRotationOffsets(art, Rotation.NE.value(), rx, ry));
        assertEquals(0, rx[0]);
        assertEquals(239, ry[0]);

        byte[] pixels = Art.artGetFrameData(art, 0, Rotation.NE.value());
        assertNotNull(pixels);
        assertEquals(640 * 480, pixels.length);
        assertEquals(0xE4F6BDB0L, crc32(pixels));

        // All rotations share the single group.
        for (int r = 0; r < Rotation.COUNT; r++) {
            assertEquals(640, Art.artGetWidth(art, 0, r));
            assertEquals(0xE4F6BDB0L, crc32(Art.artGetFrameData(art, 0, r)));
        }

        assertEquals(-1, Art.artGetWidth(art, 999, Rotation.NE.value()));
        assertEquals(-1, Art.artGetWidth(art, 0, 99));
        assertEquals(-1, Art.artGetWidth(null, 0, Rotation.NE.value()));
        assertEquals(10, Art.artGetFramesPerSecond(null));
        assertEquals(-1, Art.artGetActionFrame(null));
        assertEquals(-1, Art.artGetFrameCount(null));
    }

    @Test
    void realCritterRotations() {
        mountGameEn();

        Art.ArtData[] out = new Art.ArtData[1];
        assertEquals(0, Art.artRead("art\\critters\\hmjmpsab.frm", out));
        Art.ArtData art = out[0];
        assertEquals(4, art.version);
        assertEquals(10, Art.artGetFramesPerSecond(art));
        assertEquals(8, Art.artGetFrameCount(art));

        int[][] expected = {
            // w, h, x, y
            { 25, 63, 3, -4 },
            { 17, 62, 9, 0 },
            { 22, 62, 1, 1 },
            { 26, 63, -3, 0 },
            { 17, 63, -9, 0 },
            { 22, 64, -2, -1 },
        };
        long[] expectedCrc = {
            0x12535569L, 0xA8B127BBL, 0xE8511184L, 0x54DD4714L, 0x79725CB9L, 0xA093D616L,
        };
        for (int r = 0; r < Rotation.COUNT; r++) {
            assertEquals(expected[r][0], Art.artGetWidth(art, 0, r), "rot " + r);
            assertEquals(expected[r][1], Art.artGetHeight(art, 0, r), "rot " + r);
            byte[] pixels = Art.artGetFrameData(art, 0, r);
            assertNotNull(pixels);
            assertEquals(expected[r][0] * expected[r][1], pixels.length);
            assertEquals(expectedCrc[r], crc32(pixels), "rot " + r);
            int[] x = new int[1];
            int[] y = new int[1];
            assertEquals(0, Art.artGetFrameOffsets(art, 0, r, x, y));
            assertEquals(expected[r][2], x[0], "rot " + r);
            assertEquals(expected[r][3], y[0], "rot " + r);
        }
    }

    @Test
    void frameOffsets() {
        mountGameEn();

        Art.ArtData[] out = new Art.ArtData[1];
        assertEquals(0, Art.artRead("art\\critters\\hmjmpsab.frm", out));
        int[] x = new int[1];
        int[] y = new int[1];
        assertEquals(0, Art.artGetFrameOffsets(out[0], 0, Rotation.NE.value(), x, y));
        assertEquals(3, x[0]);
        assertEquals(-4, y[0]);
    }

    @Test
    void syntheticFrm() throws Exception {
        // Minimal 1-frame FRM, little-endian (2x1 pixels + pad).
        Path dir = Files.createDirectory(temp.resolve("frm"));
        Files.write(dir.resolve("a.frm"), toLittleEndianFrm());
        assertTrue(XFile.xbaseOpen(dir.toString()));

        Art.ArtData[] out = new Art.ArtData[1];
        assertEquals(0, Art.artRead("a.frm", out));
        Art.ArtData art = out[0];
        assertEquals(4, art.version);
        assertEquals(12, Art.artGetFramesPerSecond(art));
        assertEquals(1, Art.artGetFrameCount(art));
        assertEquals(2, Art.artGetWidth(art, 0, Rotation.NE.value()));
        assertEquals(1, Art.artGetHeight(art, 0, Rotation.NE.value()));
        assertArrayEquals(new byte[] { (byte) 0xAA, (byte) 0xBB },
                Art.artGetFrameData(art, 0, Rotation.NE.value()));
        int[] x = new int[1];
        int[] y = new int[1];
        assertEquals(0, Art.artGetFrameOffsets(art, 0, Rotation.NE.value(), x, y));
        assertEquals(3, x[0]);
        assertEquals(4, y[0]);
        // 88 header + 14 data + 3 worst-case pad (single shared group).
        assertEquals(105, Art.artGetDataSize(art));
        assertEquals(2, Art.paddingForSize(2));
        assertEquals(0, Art.paddingForSize(4));
    }

    // FRM integers are big-endian (like fileReadInt16 assembles them).
    static byte[] toLittleEndianFrm() {
        java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(raw);
        try {
            dos.writeInt(4); // version
            dos.writeShort(12); // fps
            dos.writeShort(1); // action
            dos.writeShort(1); // frames
            for (int i = 0; i < 6; i++) {
                dos.writeShort(i); // xOffsets
            }
            for (int i = 0; i < 6; i++) {
                dos.writeShort(-i); // yOffsets
            }
            for (int i = 0; i < 6; i++) {
                dos.writeInt(0); // dataOffsets (shared)
            }
            dos.writeInt(14); // dataSize: frame header + pixels
            dos.writeShort(2); // w
            dos.writeShort(1); // h
            dos.writeInt(2); // size
            dos.writeShort(3); // x
            dos.writeShort(4); // y
            dos.write(new byte[] { (byte) 0xAA, (byte) 0xBB });
            dos.flush();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return raw.toByteArray();
    }

    @Test
    void readErrors() {
        mountGameEn();

        Art.ArtData[] out = new Art.ArtData[1];
        assertEquals(-2, Art.artRead("art\\no\\such.frm", out));
        assertEquals(-3, Art.artRead("text\\english\\game\\misc.msg", out));
        assertNull(out[0]);
    }

    @Test
    void animCodes() {
        int[] walk = Art.animCodes(AnimationType.WALK.value(), WeaponAnimation.NONE.value());
        assertNotNull(walk);
        assertEquals('b', walk[1]);
        assertEquals('a', walk[0]);

        assertNull(Art.animCodes(AnimationType.WALK.value(), 99));

        int[] thrown = Art.animCodes(AnimationType.THROW_ANIM.value(), WeaponAnimation.KNIFE.value());
        assertNotNull(thrown);
        assertEquals('d', thrown[0]);
        assertEquals('m', thrown[1]);

        int[] sf = Art.animCodes(AnimationType.FALL_BACK_SF.value(), WeaponAnimation.NONE.value());
        assertNotNull(sf);
        assertEquals('a', sf[1]);
        assertEquals('r', sf[0]);
    }
}
