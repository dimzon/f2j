package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code [config]} vectors from {@code docker/refharness/golden/v8.txt}.
 */
class ConfigTest {

    @TempDir
    Path temp;

    @AfterEach
    void reset() {
        Config.resetLastSectionKey();
        Db.resetForTests();
    }

    static Config.Cfg readRepoCfg() {
        Config.Cfg cfg = new Config.Cfg();
        assertTrue(Config.configInit(cfg));
        // Committed copy of files/fallout2.cfg for hermetic tests.
        try (java.io.InputStream in = ConfigTest.class.getResourceAsStream("/fixtures/fallout2.cfg")) {
            Path tmp = Files.createTempFile("f2j-", "-fallout2.cfg");
            tmp.toFile().deleteOnExit();
            Files.write(tmp, in.readAllBytes());
            assertTrue(Config.configRead(cfg, tmp.toString(), false));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @Test
    void readsRealCfg() {
        Config.Cfg cfg = readRepoCfg();

        String[] master = new String[1];
        assertTrue(Config.configGetString(cfg, "system", "master_dat", master));
        assertEquals("master.dat", master[0]);

        int[] resx = { -99 };
        assertTrue(Config.configGetInt(cfg, "screen", "resolution_x", resx));
        assertEquals(800, resx[0]);

        boolean[] music = { false };
        assertTrue(Config.configGetBool(cfg, "sound", "music", music));
        assertTrue(music[0]);

        assertFalse(Config.configGetString(cfg, "nope", "nope", new String[1]));

        String[] def = new String[1];
        assertTrue(Config.configGetString(cfg, "nope", "nope", def, "DEF"));
        assertEquals("DEF", def[0]);

        int[] idef = { -99 };
        assertTrue(Config.configGetInt(cfg, "nope", "nope", idef, 42));
        assertEquals(42, idef[0]);
    }

    @Test
    void intQuirks() {
        Config.Cfg cfg = new Config.Cfg();
        Config.configInit(cfg);
        Config.configSetString(cfg, "probe", "hex", "0x10");
        Config.configSetString(cfg, "probe", "oct", "010");
        Config.configSetString(cfg, "probe", "pct", "38%");
        Config.configSetString(cfg, "probe", "trail", "12x");
        Config.configSetString(cfg, "probe", "empty", "");
        Config.configSetString(cfg, "probe", "huge", "9999999999999999999");

        int[] v = { 0 };
        assertTrue(Config.configGetInt(cfg, "probe", "hex", v));
        assertEquals(16, v[0]);
        assertTrue(Config.configGetInt(cfg, "probe", "oct", v));
        assertEquals(8, v[0]);
        assertTrue(Config.configGetInt(cfg, "probe", "pct", v));
        assertEquals(38, v[0]);
        assertTrue(Config.configGetInt(cfg, "probe", "trail", v));
        assertEquals(12, v[0]);
        assertTrue(Config.configGetInt(cfg, "probe", "empty", v));
        assertEquals(0, v[0]);
        assertTrue(Config.configGetInt(cfg, "probe", "huge", v));
        assertEquals(-1, v[0]);

        int[] b16 = { 0 };
        assertTrue(Config.configGetIntBase(cfg, "probe", "hex", b16, 0, 16));
        assertEquals(16, b16[0]);
    }

    @Test
    void doubles() {
        Config.Cfg cfg = new Config.Cfg();
        Config.configInit(cfg);
        assertTrue(Config.configSetDouble(cfg, "probe", "pi", 3.14159));

        double[] d = { 0 };
        assertTrue(Config.configGetDouble(cfg, "probe", "pi", d));
        assertEquals(3.14159, d[0], 1e-9);
    }

    @Test
    void intList() {
        Config.Cfg cfg = new Config.Cfg();
        Config.configInit(cfg);
        Config.configSetString(cfg, "probe", "list", "1,2,3");

        int[] arr = new int[3];
        assertTrue(Config.configGetIntList(cfg, "probe", "list", arr, 3));
        assertArrayEquals(new int[] { 1, 2, 3 }, arr);

        Config.configSetString(cfg, "probe", "short", "7,8");
        int[] arr2 = { -1, -1, -1 };
        assertFalse(Config.configGetIntList(cfg, "probe", "short", arr2, 3));
        assertArrayEquals(new int[] { 7, 8, -1 }, arr2);
    }

    @Test
    void commandLineOverrides() {
        Config.Cfg cfg = readRepoCfg();
        String[] argv = { "prog", "[screen]resolution_x=1024", "junk", "[bad", "[s]k=v=w", "[ sp ]k2 = v2 " };
        assertTrue(Config.configParseCommandLineArguments(cfg, argv, null));

        int[] resx = { 0 };
        assertTrue(Config.configGetInt(cfg, "screen", "resolution_x", resx));
        assertEquals(1024, resx[0]);

        String[] kv = new String[1];
        assertTrue(Config.configGetString(cfg, "s", "k", kv));
        assertEquals("v=w", kv[0]);

        // NOTE: section is NOT trimmed (kept C quirk).
        String[] k2 = new String[1];
        assertTrue(Config.configGetString(cfg, " sp ", "k2", k2));
        assertEquals("v2", k2[0]);
    }

    @Test
    void standardWriteRoundTrip() throws Exception {
        Config.Cfg cfg = readRepoCfg();
        assertTrue(Config.configSetString(cfg, "news", "nk", "nv"));

        Path out = temp.resolve("std.ini");
        assertTrue(Config.configWrite(cfg, out.toString(), false));

        Config.Cfg back = new Config.Cfg();
        Config.configInit(back);
        assertTrue(Config.configRead(back, out.toString(), false));
        String[] nv = new String[1];
        assertTrue(Config.configGetString(back, "news", "nk", nv));
        assertEquals("nv", nv[0]);

        // Sections iterate case-insensitively sorted (Dictionary parity).
        String text = Files.readString(out);
        assertTrue(text.indexOf("[news]") < text.indexOf("[screen]")); // n < s
    }

    @Test
    void sideBySideRetains() throws Exception {
        Path orig = temp.resolve("orig.ini");
        Files.write(orig, ("; top comment\n[screen]\n; res comment\nresolution_x=800 ; inline\n"
                + "unknown_key=zzz\n\n[new]\na=1\n").getBytes(StandardCharsets.ISO_8859_1));

        Config.Cfg cfg = new Config.Cfg();
        Config.configInit(cfg);
        assertTrue(Config.configRead(cfg, orig.toString(), false));
        assertTrue(Config.configSetString(cfg, "screen", "resolution_x", "1024"));
        assertTrue(Config.configSetString(cfg, "added", "k", "v"));
        assertTrue(Config.configRemoveKey(cfg, "new", "a"));

        assertTrue(Config.configWriteEx(cfg, orig.toString(),
                Config.RETAIN_COMMENTS | Config.RETAIN_ORDER | Config.RETAIN_UNKNOWN));

        // NOTE: removed-under-RETAIN_UNKNOWN resurrects verbatim, like C.
        assertEquals(
                "; top comment\n[screen]\n; res comment\nresolution_x=1024 ; inline\nunknown_key=zzz\n\n[new]\na=1\n\n[added]\nk=v\n\n",
                Files.readString(orig));
        assertTrue(!Files.exists(temp.resolve("orig.ini.tmp")));
        assertTrue(!Files.exists(temp.resolve("orig.ini.bak")));
    }

    @Test
    void copyAndRemove() {
        Config.Cfg cfg = readRepoCfg();
        Config.Cfg dest = new Config.Cfg();
        Config.configInit(dest);
        assertTrue(Config.configCopy(dest, cfg));

        String[] master = new String[1];
        assertTrue(Config.configGetString(dest, "system", "master_dat", master));
        assertEquals("master.dat", master[0]);

        assertTrue(Config.configRemoveKey(dest, "system", "master_dat"));
        assertFalse(Config.configGetString(dest, "system", "master_dat", new String[1]));
        assertTrue(Config.configRemoveKey(dest, "system", "master_dat"));
        assertTrue(Config.configRemoveKey(dest, "nope", "nope"));
    }
}
