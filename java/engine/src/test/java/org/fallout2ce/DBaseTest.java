package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import org.junit.jupiter.api.Test;

/**
 * {@code [dbase]}/{@code [dfile]}/{@code [zip]}/{@code [fixtureDat]} vectors
 * from {@code docker/refharness/golden/v6.txt}. Real-archive tests need
 * {@code F2J_GAMEDATA} pointing at {@code game-en} and abort otherwise.
 */
class DBaseTest {

    static String resource(String name) throws Exception {
        InputStream in = DBaseTest.class.getResourceAsStream("/fixtures/" + name);
        Path tmp = Files.createTempFile("f2j-", "-" + name);
        tmp.toFile().deleteOnExit();
        Files.write(tmp, in.readAllBytes());
        return tmp.toString();
    }

    static byte[] readAll(DFile.Handle h) {
        List<Byte> out = new ArrayList<>();
        byte[] buf = new byte[512];
        while (true) {
            long n = DFile.dfileRead(buf, 0, 1, buf.length, h);
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

    static void closeAll(DBase.Archive db) {
        DBase.dbaseClose(db);
    }

    @Test
    void opensFixtureDat() {
        DBase.Archive db;
        try {
            db = DBase.dbaseOpen(resource("f2j-test.dat"), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertNotNull(db);
        assertEquals(DBase.Format.DAT, db.format);
        assertEquals(6, db.entries.size());
        closeAll(db);
    }

    @Test
    void openMissingAndGarbage() throws Exception {
        assertNull(DBase.dbaseOpen("no-such-archive.dat", null));

        Path empty = Files.createTempFile("f2j-", "-empty.dat");
        empty.toFile().deleteOnExit();
        int[] flags = { -99 };
        assertNull(DBase.dbaseOpen(empty.toString(), flags));
        assertEquals(-99, flags[0]); // untouched on the DAT path
    }

    @Test
    void fixtureFind() throws Exception {
        DBase.Archive db = DBase.dbaseOpen(resource("f2j-test.dat"), null);
        assertNotNull(db);

        DBase.FindData fd = new DBase.FindData();
        assertTrue(DBase.dbaseFindFirstEntry(db, fd, "sub\\*"));
        assertEquals("sub\\nested.txt", fd.fileName);
        assertTrue(!DBase.dbaseFindNextEntry(db, fd));

        assertTrue(!DBase.dbaseFindFirstEntry(db, new DBase.FindData(), "zzz\\*.qqq"));
        assertTrue(DBase.dbaseFindClose(db, fd));
        closeAll(db);
    }

    @Test
    void readsStoredEntry() throws Exception {
        DBase.Archive db = DBase.dbaseOpen(resource("f2j-test.dat"), null);
        assertNotNull(db);

        DFile.Handle h = DFile.dfileOpen(db, "hello.txt", "rb");
        assertNotNull(h);
        assertEquals(6, DFile.dfileGetSize(h));
        // Exact-size read: no clamping, so EOF stays clear (golden v6).
        byte[] six = new byte[6];
        assertEquals(6, DFile.dfileRead(six, 0, 1, 6, h));
        assertArrayEquals("hello\n".getBytes(), six);
        assertEquals(0, DFile.dfileEof(h));
        assertEquals(0, DFile.dfileRead(new byte[1], 0, 1, 1, h));
        assertNotEquals(0, DFile.dfileEof(h)); // now EOF is set
        assertEquals(0, DFile.dfileClose(h));
        closeAll(db);
    }

    @Test
    void readsCompressedEntry() throws Exception {
        DBase.Archive db = DBase.dbaseOpen(resource("f2j-test.dat"), null);
        assertNotNull(db);

        DFile.Handle h = DFile.dfileOpen(db, "big.txt", "rb");
        assertNotNull(h);
        assertEquals(8200, DFile.dfileGetSize(h));
        byte[] content = readAll(h);
        assertEquals(8200, content.length);
        assertEquals(0x70C09C7FL, crc32(content)); // golden v6
        assertEquals("0123456789ABCDEF", new String(content, 0, 16));
        DFile.dfileClose(h);
        closeAll(db);
    }

    @Test
    void mediumEntryVectors() throws Exception {
        DBase.Archive db = DBase.dbaseOpen(resource("f2j-test.dat"), null);
        assertNotNull(db);

        DFile.Handle h = DFile.dfileOpen(db, "medium.txt", "rb");
        assertNotNull(h);
        assertEquals(201, DFile.dfileGetSize(h));
        assertEquals(0xD872C896L, crc32(readAll(h)));
        DFile.dfileClose(h);

        // Stale-position quirk: stored seek moves the pointer, not position.
        DFile.Handle s = DFile.dfileOpen(db, "medium.txt", "rb");
        assertNotNull(s);
        assertEquals(4, DFile.dfileRead(new byte[4], 0, 1, 4, s));
        assertEquals(4, DFile.dfileTell(s));
        assertEquals(0, DFile.dfileSeek(s, 100, DFile.SEEK_SET));
        assertEquals(4, DFile.dfileTell(s));
        byte[] four = new byte[4];
        assertEquals(4, DFile.dfileRead(four, 0, 1, 4, s));
        assertEquals(8, DFile.dfileTell(s));
        assertArrayEquals(new byte[] { 0x49, 0x4C, 0x79, 0x4D }, four); // golden v6
        assertEquals(1, DFile.dfileSeek(s, 201, DFile.SEEK_SET)); // == size fails
        DFile.dfileClose(s);
        closeAll(db);
    }

    @Test
    void textModeCollapsesCrLf() throws Exception {
        DBase.Archive db = DBase.dbaseOpen(resource("f2j-test.dat"), null);
        assertNotNull(db);

        DFile.Handle h = DFile.dfileOpen(db, "crlf.txt", "rt");
        assertNotNull(h);
        assertEquals("a\n", DFile.dfileReadString(h, 128));
        assertEquals("b\n", DFile.dfileReadString(h, 128));
        DFile.dfileClose(h);
        closeAll(db);
    }

    @Test
    void emptyEntry() throws Exception {
        DBase.Archive db = DBase.dbaseOpen(resource("f2j-test.dat"), null);
        assertNotNull(db);

        DFile.Handle h = DFile.dfileOpen(db, "empty.txt", "rb");
        assertNotNull(h);
        assertEquals(0, DFile.dfileGetSize(h));
        assertEquals(0, DFile.dfileRead(new byte[4], 0, 1, 4, h));
        assertNull(DFile.dfileReadString(h, 128));
        assertEquals(1, DFile.dfileSeek(h, 100, DFile.SEEK_SET));
        DFile.dfileClose(h);
        closeAll(db);
    }

    @Test
    void openRejects() throws Exception {
        DBase.Archive db = DBase.dbaseOpen(resource("f2j-test.dat"), null);
        assertNotNull(db);
        assertNull(DFile.dfileOpen(db, "no\\such.txt", "rb"));
        assertNull(DFile.dfileOpen(db, "hello.txt", "wb"));
        assertEquals(-1, DFile.dfileWriteChar('x', DFile.dfileOpen(db, "hello.txt", "rb")));
        closeAll(db);
    }

    @Test
    void zipFixture() throws Exception {
        DBase.Archive db = DBase.dbaseOpen(resource("f2test.zip"), null);
        assertNotNull(db);
        assertEquals(DBase.Format.ZIP, db.format);
        assertEquals(2, db.entries.size()); // dir entry skipped

        DFile.Handle a = DFile.dfileOpen(db, "a.txt", "rb");
        assertNotNull(a);
        assertEquals(11, DFile.dfileGetSize(a));
        assertArrayEquals("stored-data".getBytes(), readAll(a));
        DFile.dfileClose(a);

        DFile.Handle b = DFile.dfileOpen(db, "sub\\b.txt", "rb");
        assertNotNull(b);
        assertEquals(1400, DFile.dfileGetSize(b));
        byte[] content = readAll(b);
        assertEquals(0x7E3B8A02L, crc32(content));
        DFile.dfileClose(b);
        closeAll(db);
    }

    @Test
    void emptyZipRejected() throws Exception {
        int[] flags = { 0 };
        assertNull(DBase.dbaseOpen(resource("f2empty.zip"), flags));
        assertEquals(DBase.ERROR_EMPTY, flags[0]);
    }

    // --- real archives (F2J_GAMEDATA=.../game-en) ---

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        Path p = Paths.get(root, name);
        assumeTrue(Files.isRegularFile(p), "missing " + p);
        return p.toString();
    }

    @Test
    void realMasterDat() {
        DBase.Archive db = DBase.dbaseOpen(gameData("master.dat"), new int[1]);
        assertNotNull(db);
        assertEquals(DBase.Format.DAT, db.format);
        assertEquals(23140, db.entries.size()); // golden v6 (EN master)

        DBase.FindData fd = new DBase.FindData();
        int msgCount = 0;
        List<String> first = new ArrayList<>();
        if (DBase.dbaseFindFirstEntry(db, fd, "text\\english\\game\\*.msg")) {
            do {
                if (first.size() < 3) {
                    first.add(fd.fileName);
                }
                msgCount++;
            } while (msgCount <= db.entries.size() + 1 && DBase.dbaseFindNextEntry(db, fd));
        }
        assertEquals(31, msgCount);
        assertEquals(List.of("text\\english\\game\\CMBATAI2.msg", "text\\english\\game\\COMBAT.MSG",
                "text\\english\\game\\COMBATAI.MSG"), first);

        DFile.Handle misc = DFile.dfileOpen(db, "text\\english\\game\\misc.msg", "rb");
        assertNotNull(misc);
        assertEquals(6728, DFile.dfileGetSize(misc));
        assertEquals(0x2CFB7ACDL, crc32(readAll(misc)));
        DFile.dfileClose(misc);

        DFile.Handle menu = DFile.dfileOpen(db, "art\\intrface\\mainmenu.frm", "rb");
        assertNotNull(menu);
        assertEquals(307274, DFile.dfileGetSize(menu));
        assertEquals(0xACA95567L, crc32(readAll(menu)));
        DFile.dfileClose(menu);

        closeAll(db);
    }

    @Test
    void realPatchDat() {
        DBase.Archive db = DBase.dbaseOpen(gameData("patch000.dat"), new int[1]);
        assertNotNull(db);
        assertEquals(489, db.entries.size()); // golden v6

        DBase.FindData fd = new DBase.FindData();
        assertTrue(DBase.dbaseFindFirstEntry(db, fd, "*"));
        assertEquals("Art\\intrface\\HELPSCRN.FRM", fd.fileName);
        closeAll(db);
    }
}
