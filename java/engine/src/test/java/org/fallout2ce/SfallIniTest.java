package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code [sfallini]} vectors from
 * {@code docker/refharness/golden/v85.txt} (2 lines appended to v84),
 * produced by {@code sectionSfallIni} in
 * {@code docker/refharness/ref_main.cc}.
 *
 * <p>The harness reads the {@code /tmp/ddraw.ini} fixture left behind by
 * its own {@code [cfgmig]} section (C resolves the {@code ddraw.ini}
 * system name CWD-relative). The Java test owns an identical fixture in
 * its {@code @TempDir} and points the implementation at it via the
 * search-directory seam (the JVM must not chdir). The flow mirrors the
 * harness exactly and asserts both vector lines verbatim; a few extra
 * asserts in the same method lock the required edge semantics
 * (string-miss to true+empty, prefix strip, 63/32 triplet caps).
 */
class SfallIniTest {

    @TempDir
    Path temp;

    @BeforeEach
    void setup() throws Exception {
        SfallIni.resetForTests();
        SfallIni.setSearchDirectoryForTests(temp.toString());
        // Byte-identical [Misc] content to the harness [cfgmig] fixture.
        Files.writeString(temp.resolve("ddraw.ini"),
                "[Misc]\nOverrideCriticalTable=5\nBooksFile=bks.ini\n"
                        + "StartingMap=arroyo\nStartYear=2250\nWorldMapFPSPatch=1\n"
                        + "WorldMapDelay2=100\nMovie1=custom.mve\nFallout1Behavior=1\n"
                        + "CheckWeaponAmmoCost=1\n");
    }

    @AfterEach
    void teardown() {
        SfallIni.resetForTests();
    }

    @Test
    void sfallIniVectors() {
        int[] critTable = new int[] { -999 };
        boolean gotCrit = SfallIni.sfallIniGetInt("ddraw.ini|Misc|OverrideCriticalTable", critTable);
        String[] books = new String[1];
        boolean gotBooks = SfallIni.sfallIniGetString("ddraw.ini|Misc|BooksFile", books);
        int[] missVal = new int[] { -999 };
        boolean gotMiss = SfallIni.sfallIniGetInt("ddraw.ini|Misc|Nope", missVal);
        int[] badVal = new int[] { -999 };
        boolean gotBad = SfallIni.sfallIniGetInt("nope", badVal);
        assertEquals("crit=1:5 books=1:[bks.ini] miss=0:-999 bad=0",
                String.format("crit=%d:%d books=%d:[%s] miss=%d:%d bad=%d",
                        gotCrit ? 1 : 0, critTable[0], gotBooks ? 1 : 0, books[0],
                        gotMiss ? 1 : 0, missVal[0], gotBad ? 1 : 0));
        // Miss leaves the int out-param untouched (only the rc is printed above).
        assertEquals(-999, missVal[0]);

        boolean setOk = SfallIni.sfallIniSetInt("ddraw.ini|Misc|OverrideCriticalTable", 7);
        int[] critAfter = new int[] { -999 };
        SfallIni.sfallIniGetInt("ddraw.ini|Misc|OverrideCriticalTable", critAfter);
        SfallIni.sfallIniCacheClear();
        int[] critCleared = new int[] { -999 };
        SfallIni.sfallIniGetInt("ddraw.ini|Misc|OverrideCriticalTable", critCleared);
        assertEquals("set=1 after=7 cleared=7",
                String.format("set=%d after=%d cleared=%d",
                        setOk ? 1 : 0, critAfter[0], critCleared[0]));

        // Edge semantics required by the slice (same method: one new test).
        String[] missing = new String[1];
        assertTrue(SfallIni.sfallIniGetString("ddraw.ini|Misc|Nope", missing));
        assertEquals("", missing[0]);
        String[] prefixed = new String[1];
        assertTrue(SfallIni.sfallIniGetString("./ddraw.ini|Misc|BooksFile", prefixed));
        assertEquals("bks.ini", prefixed[0]);
        assertFalse(SfallIni.sfallIniGetInt("x".repeat(64) + "|Misc|K", new int[] { -999 }));
        assertFalse(SfallIni.sfallIniGetInt("ddraw.ini|" + "y".repeat(33) + "|K", new int[] { -999 }));
    }
}
