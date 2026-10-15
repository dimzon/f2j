package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code [db]} vectors from {@code docker/refharness/golden/v7.txt} plus
 * typed I/O round-trips. Mounts reset after each test.
 */
class DbTest {

    @TempDir
    Path temp;

    @AfterEach
    void reset() {
        Db.resetForTests();
    }

    static String resource(String name) throws Exception {
        InputStream in = DbTest.class.getResourceAsStream("/fixtures/" + name);
        Path tmp = Files.createTempFile("f2j-", "-" + name);
        tmp.toFile().deleteOnExit();
        Files.write(tmp, in.readAllBytes());
        return tmp.toString();
    }

    @Test
    void openMountsAndTotal() throws Exception {
        assertEquals(0, Db.dbOpen(resource("f2j-test.dat"), null));
        assertEquals(1, Db.dbTotal());
        assertEquals(1, XFile.xbaseCount());
        Db.dbCloseAll();
        assertEquals(0, XFile.xbaseCount());
    }

    @Test
    void openMissing() {
        assertEquals(-1, Db.dbOpen(temp.resolve("nope.dat").toString(), null));
        assertEquals(0, Db.dbOpen(null, null));
    }

    @Test
    void sizeAndContents() throws Exception {
        assertEquals(0, Db.dbOpen(resource("f2j-test.dat"), null));

        int[] size = { -99 };
        assertEquals(0, Db.dbGetFileSize("hello.txt", size));
        assertEquals(6, size[0]);
        assertEquals(-1, Db.dbGetFileSize("no\\such.txt", size));

        byte[] out = new byte[6];
        assertEquals(0, Db.dbGetFileContents("hello.txt", out, 0));
        assertArrayEquals("hello\n".getBytes(), out);
        assertEquals(-1, Db.dbGetFileContents("no\\such.txt", out, 0));
    }

    @Test
    void progressAccounting() throws Exception {
        assertEquals(0, Db.dbOpen(resource("f2j-test.dat"), null));

        AtomicInteger calls = new AtomicInteger();
        Db.fileSetReadProgressHandler(calls::incrementAndGet, 4);

        byte[] out = new byte[8200];
        assertEquals(0, Db.dbGetFileContents("big.txt", out, 0));
        assertEquals(2050, calls.get()); // 8200 / 4, no tail
        assertEquals(0, Db.readProgressBytesRead);

        Db.fileSetReadProgressHandler(null, 0);

        byte[] out2 = new byte[6];
        assertEquals(0, Db.dbGetFileContents("hello.txt", out2, 0));
        assertEquals(2050, calls.get()); // cleared handler: no further calls
    }

    @Test
    void typedBigEndianRoundTrip() throws Exception {
        Path file = temp.resolve("be.bin");
        XFile.Handle w = Db.fileOpen(file.toString(), "wb");
        assertNotNull(w);
        assertEquals(0, Db.fileWriteInt32(w, 0x01020304));
        assertEquals(0, Db.fileWriteFloat(w, 1.5f));
        assertEquals(0, Db.fileWriteBool(w, true));
        assertEquals(0, Db.fileWriteInt16(w, -2));
        // NOTE: like C fputc, success echoes the byte (callers test == -1).
        assertEquals(0xAB, Db.fileWriteUInt8(w, 0xAB));
        Db.fileClose(w);

        // Layout is big-endian on disk, like C.
        byte[] raw = Files.readAllBytes(file);
        assertArrayEquals(
                new byte[] { 0x01, 0x02, 0x03, 0x04, 0x3F, (byte) 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
                        (byte) 0xFF, (byte) 0xFE, (byte) 0xAB },
                raw);

        XFile.Handle r = Db.fileOpen(file.toString(), "rb");
        assertNotNull(r);
        int[] i32 = { 0 };
        assertEquals(0, Db.fileReadInt32(r, i32));
        assertEquals(0x01020304, i32[0]);
        float[] f = { 0 };
        assertEquals(0, Db.fileReadFloat(r, f));
        assertEquals(1.5f, f[0]);
        assertEquals(0x3FC00000, Float.floatToIntBits(f[0]));
        boolean[] b = { false };
        assertEquals(0, Db.fileReadBool(r, b));
        assertTrue(b[0]);
        int[] i16 = { 0 };
        assertEquals(0, Db.fileReadInt16(r, i16));
        assertEquals(-2, i16[0]);
        int[] u8 = { 0 };
        assertEquals(0, Db.fileReadUInt8(r, u8));
        assertEquals(0xAB, u8[0]);
        int[] pastEnd = { 0 };
        assertEquals(-1, Db.fileReadInt32(r, pastEnd));
        Db.fileClose(r);
    }

    @Test
    void int32ListBulk() throws Exception {
        Path file = temp.resolve("list.bin");
        XFile.Handle w = Db.fileOpen(file.toString(), "wb");
        assertNotNull(w);
        assertEquals(0, Db.fileWriteInt32List(w, new int[] { 1, -2, 0x01020304 }, 0, 3));
        assertEquals(0, Db.fileWriteInt32List(w, new int[0], 0, 0));
        Db.fileClose(w);

        XFile.Handle r = Db.fileOpen(file.toString(), "rb");
        assertNotNull(r);
        int[] back = new int[3];
        assertEquals(0, Db.fileReadInt32List(r, back, 0, 3));
        assertArrayEquals(new int[] { 1, -2, 0x01020304 }, back);
        assertEquals(0, Db.fileReadInt32List(r, new int[0], 0, 0));
        Db.fileClose(r);
    }

    @Test
    void fixedLengthString() throws Exception {
        Path file = temp.resolve("fix.bin");
        XFile.Handle w = Db.fileOpen(file.toString(), "wb");
        assertNotNull(w);
        assertEquals(0, Db.fileWriteFixedLengthString(w, "AB".getBytes(), 0, 2));
        Db.fileClose(w);

        XFile.Handle r = Db.fileOpen(file.toString(), "rb");
        assertNotNull(r);
        byte[] out = new byte[2];
        assertEquals(0, Db.fileReadFixedLengthString(r, out, 0, 2));
        assertArrayEquals("AB".getBytes(), out);
        Db.fileClose(r);
    }

    @Test
    void nameListWildcardFiltersDirs() throws Exception {
        assertEquals(0, Db.dbOpen(resource("f2j-test.dat"), null));

        // Wildcard: entries with directories are dropped, basenames kept.
        List<String> names = Db.fileNameListInit("*.txt");
        assertEquals(List.of("big.txt", "crlf.txt", "empty.txt", "hello.txt", "medium.txt"), names);
        Db.fileNameListFree(names);

        // Non-wildcard: every match kept (as basename).
        List<String> hello = Db.fileNameListInit("hello*");
        assertEquals(List.of("hello.txt"), hello);
        Db.fileNameListFree(hello);

        List<String> none = Db.fileNameListInit("zzz*");
        assertTrue(none.isEmpty());
        Db.fileNameListFree(none);
    }

    @Test
    void aliasRoundTrip() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("d"));
        Files.writeString(dir.resolve("real.txt"), "R");
        assertEquals(0, Db.dbOpen(dir.toString(), null));

        int id = SfallFileSystem.sfallFileSystemCopy("alias\\x.txt", "real.txt");
        assertTrue(id >= 0);
        assertEquals(id, SfallFileSystem.sfallFileSystemFind("ALIAS\\X.TXT"));
        // Resolved to the normalized source.
        XFile.Handle h = Db.fileOpen("alias\\x.txt", "rb");
        assertNotNull(h);
        byte[] one = new byte[1];
        assertEquals(1, Db.fileRead(one, 0, 1, 1, h));
        assertEquals('R', one[0]);
        Db.fileClose(h);

        // sound\sfx\ is never aliased.
        Files.createDirectories(dir.resolve("sound/sfx"));
        Files.writeString(dir.resolve("sound/sfx/e.dat"), "S");
        assertTrue(SfallFileSystem.sfallFileSystemCopy("sound\\sfx\\e.dat", "real.txt") >= 0);
        XFile.Handle s = Db.fileOpen("sound\\sfx\\e.dat", "rb");
        assertNotNull(s);
        byte[] one2 = new byte[1];
        Db.fileRead(one2, 0, 1, 1, s);
        assertEquals('S', one2[0]);
        Db.fileClose(s);

        SfallFileSystem.sfallFileSystemDelete(id);
        assertEquals(-1, SfallFileSystem.sfallFileSystemFind("alias\\x.txt"));
        assertEquals(-1, SfallFileSystem.sfallFileSystemCopy("", "real.txt"));
    }

    // --- game-en (F2J_GAMEDATA) ---

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        Path p = Paths.get(root, name);
        assumeTrue(Files.isRegularFile(p), "missing " + p);
        return p.toString();
    }

    @Test
    void realArtNameList() {
        String master = gameData("master.dat");
        String patch = gameData("patch000.dat");
        assertEquals(0, Db.dbOpen(master, patch));

        List<String> names = Db.fileNameListInit("Art\\*");
        assertEquals(8167, names.size()); // golden v7
        assertEquals("Adobe.frm", names.get(0));
        assertEquals("BACK1.FRM", names.get(1));
        assertEquals("BACKGRND.LST", names.get(2));
        assertEquals("YRT1012.FRM", names.get(names.size() - 1));
        Db.fileNameListFree(names);
    }
}
