package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code [xbase]}/{@code [xfile]} vectors from
 * {@code docker/refharness/golden/v7.txt}. Mounts are reset after each test.
 */
class XFileTest {

    @TempDir
    Path temp;

    Path dirA;
    Path dirB;

    @AfterEach
    void reset() {
        Db.resetForTests();
    }

    void mountDirs() throws Exception {
        dirA = Files.createDirectory(temp.resolve("a"));
        dirB = Files.createDirectory(temp.resolve("b"));
        Files.writeString(dirA.resolve("shared.txt"), "A");
        Files.writeString(dirB.resolve("shared.txt"), "B");
        assertTrue(XFile.xbaseOpen(dirA.toString()));
        assertTrue(XFile.xbaseOpen(dirB.toString()));
        // Re-open moves dirA back to the top (golden [xbase] order probe).
        assertTrue(XFile.xbaseOpen(dirA.toString()));
        assertEquals(dirA.toString(), XFile.xbaseHeadPath());
    }

    static String resource(String name) throws Exception {
        InputStream in = XFileTest.class.getResourceAsStream("/fixtures/" + name);
        Path tmp = Files.createTempFile("f2j-", "-" + name);
        tmp.toFile().deleteOnExit();
        Files.write(tmp, in.readAllBytes());
        return tmp.toString();
    }

    static byte[] readAll(XFile.Handle h) {
        java.util.ArrayList<Byte> out = new java.util.ArrayList<>();
        byte[] buf = new byte[64];
        while (true) {
            long n = XFile.xfileRead(buf, 0, 1, buf.length, h);
            if (n == 0) {
                break;
            }
            for (int i = 0; i < n; i++) {
                out.add(buf[i]);
            }
        }
        byte[] bytes = new byte[out.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = out.get(i);
        }
        return bytes;
    }

    static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    @Test
    void mountPriorityAndMoveToTop() throws Exception {
        mountDirs();

        XFile.Handle h = XFile.xfileOpen("shared.txt", "rb");
        assertNotNull(h);
        assertEquals(XFile.Type.FILE, h.type);
        assertArrayEquals("A".getBytes(), readAll(h));
        XFile.xfileClose(h);

        // Re-mounting dirB moves it to the top: "B" wins now.
        assertTrue(XFile.xbaseOpen(dirB.toString()));
        XFile.Handle h2 = XFile.xfileOpen("shared.txt", "rb");
        assertNotNull(h2);
        assertArrayEquals("B".getBytes(), readAll(h2));
        XFile.xfileClose(h2);
    }

    @Test
    void absolutePathBypassesXbases() throws Exception {
        mountDirs();

        XFile.Handle h = XFile.xfileOpen(dirB.resolve("shared.txt").toString(), "rb");
        assertNotNull(h);
        assertArrayEquals("B".getBytes(), readAll(h));
        XFile.xfileClose(h);
    }

    @Test
    void datEntryThroughVfs() throws Exception {
        assertTrue(XFile.xbaseOpen(resource("f2j-test.dat")));
        mountDirs();

        XFile.Handle h = XFile.xfileOpen("hello.txt", "rb");
        assertNotNull(h);
        assertEquals(XFile.Type.DFILE, h.type);
        assertEquals(6, XFile.xfileGetSize(h));
        assertArrayEquals("hello\n".getBytes(), readAll(h));
        XFile.xfileClose(h);
    }

    @Test
    void gzipMagicBranch() throws Exception {
        mountDirs();
        Path gz = dirB.resolve("arch.gz");
        try (GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
            out.write("GZDATA".getBytes(StandardCharsets.ISO_8859_1));
        }

        XFile.Handle h = XFile.xfileOpen("arch.gz", "rb");
        assertNotNull(h);
        assertEquals(XFile.Type.GZFILE, h.type);
        assertEquals(0, XFile.xfileGetSize(h)); // quirk, like C
        assertArrayEquals("GZDATA".getBytes(), readAll(h));
        assertEquals(6, XFile.xfileTell(h));
        assertTrue(XFile.xfileEof(h) != 0);
        XFile.xfileClose(h);
    }

    @Test
    void missingIsNull() throws Exception {
        mountDirs();
        assertNull(XFile.xfileOpen("no_such_file.xyz", "rb"));
    }

    @Test
    void writeLandsInFirstDirXbase() throws Exception {
        mountDirs();

        XFile.Handle w = XFile.xfileOpen("wtest.bin", "wb");
        assertNotNull(w);
        byte[] data = { 0x01, 0x02, 0x03, 0x04 };
        assertEquals(4, XFile.xfileWrite(data, 0, 1, 4, w));
        XFile.xfileClose(w);

        assertArrayEquals(data, Files.readAllBytes(dirA.resolve("wtest.bin")));
    }

    @Test
    void seekTellRewindEof() throws Exception {
        assertTrue(XFile.xbaseOpen(resource("f2j-test.dat")));

        XFile.Handle h = XFile.xfileOpen("medium.txt", "rb");
        assertNotNull(h);
        byte[] four = new byte[4];
        assertEquals(4, XFile.xfileRead(four, 0, 1, 4, h));
        assertEquals(4, XFile.xfileTell(h));
        XFile.xfileRewind(h);
        assertEquals(0, XFile.xfileTell(h));
        assertEquals(0, XFile.xfileEof(h));
        XFile.xfileClose(h);
    }

    @Test
    void textLineCollapsesCrLfOnlyInTextMode() throws Exception {
        assertTrue(XFile.xbaseOpen(resource("f2j-test.dat")));

        // Binary mode: DFile branch passes bytes through raw.
        XFile.Handle h = XFile.xfileOpen("crlf.txt", "rb");
        assertNotNull(h);
        assertEquals(XFile.Type.DFILE, h.type);
        assertEquals("a\r\n", XFile.xfileReadString(h, 128));
        XFile.xfileClose(h);

        // Text mode: \r\n collapses to \n.
        XFile.Handle t = XFile.xfileOpen("crlf.txt", "rt");
        assertNotNull(t);
        assertEquals("a\n", XFile.xfileReadString(t, 128));
        XFile.xfileClose(t);
    }

    @Test
    void printFormatted() throws Exception {
        Path out = temp.resolve("fmt.txt");
        XFile.Handle h = XFile.xfileOpen(out.toString(), "wb");
        assertNotNull(h);
        assertTrue(XFile.xfilePrintFormatted(h, "%s=%d", "k", 42) > 0);
        XFile.xfileClose(h);
        assertEquals("k=42", Files.readString(out));
    }

    @Test
    void xbaseOpenResults() throws Exception {
        mountDirs();
        assertEquals(2, XFile.xbaseCount());
        assertTrue(XFile.xbaseOpen(dirA.toString()));
        assertEquals(2, XFile.xbaseCount());
        assertTrue(!XFile.xbaseOpen(temp.resolve("nope").toString()));

        assertTrue(XFile.xbaseIsValidDirectory(dirA.toString()));
        assertTrue(XFile.xbaseIsValidDirectory(dirA + "/"));
        assertTrue(XFile.xbaseIsValidDirectory(dirA.toString().toUpperCase(java.util.Locale.ROOT)));
        assertTrue(!XFile.xbaseIsValidDirectory(temp.resolve("nope").toString()));
        assertTrue(!XFile.xbaseIsValidDirectory(""));
    }

    @Test
    void reopenAllAndMakeDirectory() throws Exception {
        Path base = Files.createDirectory(temp.resolve("base"));
        assertTrue(XFile.xbaseReopenAll(base.toString()));
        assertEquals(1, XFile.xbaseCount());
        // NOTE: like C, exactly one missing level is created per call.
        assertEquals(0, XFile.xbaseMakeDirectory("sub1"));
        assertEquals(0, XFile.xbaseMakeDirectory("sub1/sub2"));
        assertTrue(Files.isDirectory(base.resolve("sub1/sub2")));
        assertTrue(XFile.xbaseReopenAll(null));
        assertEquals(0, XFile.xbaseCount());
    }

    @Test
    void directoriesOnly() throws Exception {
        Path base = Files.createDirectory(temp.resolve("base"));
        Files.createDirectories(base.resolve("sub/deep"));
        Files.writeString(base.resolve("sub/file.txt"), "x");
        assertTrue(XFile.xbaseOpen(base.toString()));

        XFile.XList xlist = new XFile.XList();
        assertTrue(XFile.xlistInitDirectories("sub\\*", xlist));
        assertEquals(List.of("sub/deep"), xlist.fileNames);
        XFile.xlistFree(xlist);
        assertTrue(xlist.fileNames.isEmpty());
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
    void vfsShadowsLikeGolden() {
        // Order master, patch, a, b + re-open a (golden [xbase]/[xfile]).
        // patch000.dat shadows master.dat's misc.msg (6776 vs 6728 bytes).
        try {
            Path a = Files.createDirectory(temp.resolve("ga"));
            Path b = Files.createDirectory(temp.resolve("gb"));
            Files.writeString(a.resolve("shared.txt"), "A");
            Files.writeString(b.resolve("shared.txt"), "B");

            assertTrue(XFile.xbaseOpen(gameData("master.dat")));
            assertTrue(XFile.xbaseOpen(gameData("patch000.dat")));
            assertTrue(XFile.xbaseOpen(a.toString()));
            assertTrue(XFile.xbaseOpen(b.toString()));
            assertTrue(XFile.xbaseOpen(a.toString()));

            XFile.Handle shared = XFile.xfileOpen("shared.txt", "rb");
            assertNotNull(shared);
            assertArrayEquals("A".getBytes(), readAll(shared));
            XFile.xfileClose(shared);

            XFile.Handle misc = XFile.xfileOpen("text\\english\\game\\misc.msg", "rb");
            assertNotNull(misc);
            assertEquals(XFile.Type.DFILE, misc.type);
            assertEquals(6776, XFile.xfileGetSize(misc));
            byte[] content = readAll(misc);
            assertEquals(6776, content.length);
            assertEquals(0xF6C79331L, crc32(content));
            XFile.xfileClose(misc);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
