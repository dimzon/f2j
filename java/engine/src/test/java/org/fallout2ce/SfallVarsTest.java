package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code [sfallvars]} vectors from
 * {@code docker/refharness/golden/v84.txt} (2 lines appended to v83),
 * produced by {@code sectionSfallVars} in
 * {@code docker/refharness/ref_main.cc}.
 *
 * <p>Pure: no game data, no window system (key/value store plus a scratch
 * file). The flow mirrors the harness exactly: init, int store/fetch,
 * miss fetch (leaves {@code missVal} at {@code -999}; only the rc is
 * printed), 8-char-string store/fetch, then save to a scratch file,
 * reset, cleared fetch, load back, and both re-fetches. Asserts both
 * vector lines verbatim.
 */
class SfallVarsTest {

    @TempDir
    Path temp;

    @BeforeEach
    void setup() {
        SfallGlobalVars.resetForTests();
    }

    @AfterEach
    void teardown() {
        SfallGlobalVars.resetForTests();
        Db.resetForTests();
    }

    @Test
    void sfallVarsVectors() {
        boolean initOk = SfallGlobalVars.sfallGlVarsInit();
        boolean storeInt = SfallGlobalVars.sfallGlVarsStore(7, 42);
        int[] intVal = new int[] { -999 };
        boolean fetchInt = SfallGlobalVars.sfallGlVarsFetch(7, intVal);
        int[] missVal = new int[] { -999 };
        boolean fetchMiss = SfallGlobalVars.sfallGlVarsFetch(8, missVal);
        boolean storeStr = SfallGlobalVars.sfallGlVarsStore("mykey123", 99);
        int[] strVal = new int[] { -999 };
        boolean fetchStr = SfallGlobalVars.sfallGlVarsFetch("mykey123", strVal);
        assertEquals("init=1 store=1 fetch=1:42 miss=0 str=1:99",
                String.format("init=%d store=%d fetch=%d:%d miss=%d str=%d:%d",
                        initOk ? 1 : 0, storeInt ? 1 : 0, fetchInt ? 1 : 0, intVal[0],
                        fetchMiss ? 1 : 0, storeStr ? 1 : 0, strVal[0]));
        // Miss leaves the out-param untouched (only the rc is printed above).
        assertEquals(-999, missVal[0]);

        String scratch = temp.resolve("sgv.bin").toString();
        XFile.Handle saveStream = Db.fileOpen(scratch, "wb");
        assertNotNull(saveStream);
        boolean saveOk = SfallGlobalVars.sfallGlVarsSave(saveStream);
        assertEquals(0, Db.fileClose(saveStream));
        SfallGlobalVars.sfallGlVarsReset();
        int[] clearedVal = new int[] { -999 };
        boolean fetchCleared = SfallGlobalVars.sfallGlVarsFetch(7, clearedVal);
        XFile.Handle loadStream = Db.fileOpen(scratch, "rb");
        assertNotNull(loadStream);
        boolean loadOk = SfallGlobalVars.sfallGlVarsLoad(loadStream);
        assertEquals(0, Db.fileClose(loadStream));
        int[] backVal = new int[] { -999 };
        boolean fetchBack = SfallGlobalVars.sfallGlVarsFetch(7, backVal);
        int[] backStr = new int[] { -999 };
        SfallGlobalVars.sfallGlVarsFetch("mykey123", backStr);
        assertEquals("save=1 cleared=0 load=1 back=1:42 backstr=99",
                String.format("save=%d cleared=%d load=%d back=%d:%d backstr=%d",
                        saveOk ? 1 : 0, fetchCleared ? 1 : 0, loadOk ? 1 : 0,
                        fetchBack ? 1 : 0, backVal[0], backStr[0]));

        SfallGlobalVars.sfallGlVarsExit();
    }
}
