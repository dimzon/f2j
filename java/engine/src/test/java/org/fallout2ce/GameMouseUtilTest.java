package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [gmouseutil]} vectors from
 * {@code docker/refharness/golden/v52.txt} (produced by
 * {@code sectionGameMouseUtil}): enumeration counts, the sorted
 * {@code text\english\game\*.msg} VFS listing, the empty-pattern probe and
 * the autorun mutex. Mounts reset after each test.
 *
 * <p>Mount set: {@code master.dat + patch000.dat} via {@link Db#dbOpen},
 * like {@code DbTest.realArtNameList}. Only DAT xbases can contribute
 * {@code text\english\game\*.msg} (the C harness also has dir xbases
 * mounted by then, none holding such paths); the count (31) plus
 * first/last pin the result identical.
 *
 * <p>{@link Db#getFileList} returns null on empty, mirroring C's
 * {@code nullptr}; {@link Db#fileNameListInit} (the db.cc layer) returns an
 * empty list instead.
 */
class GameMouseUtilTest {

    @AfterEach
    void reset() {
        Db.resetForTests();
    }

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        assumeTrue(root != null, "F2J_GAMEDATA not set");
        Path p = Paths.get(root, name);
        assumeTrue(Files.isRegularFile(p), "missing " + p);
        return p.toString();
    }

    @Test
    void gmouseUtilVectors() {
        // modes=11 first-skill=4 skills=7 items=10 cursors=27
        // first-anim=25 scroll-invalid-off=8
        assertEquals(11, GameMouse.GAME_MOUSE_MODE_COUNT);
        assertEquals(4, GameMouse.FIRST_GAME_MOUSE_MODE_SKILL);
        assertEquals(7, GameMouse.GAME_MOUSE_MODE_SKILL_COUNT);
        assertEquals(10, GameMouse.GAME_MOUSE_ACTION_MENU_ITEM_COUNT);
        assertEquals(27, GameMouse.MOUSE_CURSOR_TYPE_COUNT);
        assertEquals(25, GameMouse.FIRST_GAME_MOUSE_ANIMATED_CURSOR);
        assertEquals(8, GameMouse.MOUSE_CURSOR_SCROLL_OFFSET_INVALID);

        // Representative ids behind the derived counts.
        assertEquals(0, GameMouse.GameMouseMode.GAME_MOUSE_MODE_MOVE.id());
        assertEquals(4, GameMouse.GameMouseMode.GAME_MOUSE_MODE_USE_FIRST_AID.id());
        assertEquals(10, GameMouse.GameMouseMode.GAME_MOUSE_MODE_USE_REPAIR.id());
        assertEquals(0, GameMouse.GameMouseActionMenuItem.GAME_MOUSE_ACTION_MENU_ITEM_CANCEL.id());
        assertEquals(9, GameMouse.GameMouseActionMenuItem.GAME_MOUSE_ACTION_MENU_ITEM_PUSH.id());
        assertEquals(0, GameMouse.MouseCursorType.MOUSE_CURSOR_NONE.id());
        assertEquals(25, GameMouse.MouseCursorType.MOUSE_CURSOR_WAIT_PLANET.id());
        assertEquals(26, GameMouse.MouseCursorType.MOUSE_CURSOR_WAIT_WATCH.id());
        assertTrue(GameMouse.gameMouseModeIsValid(10));
        assertTrue(GameMouse.mouseCursorTypeIsValid(26));

        // msgs=31 first=[CMBATAI2.msg] last=[scrname.msg]
        assertEquals(0, Db.dbOpen(gameData("master.dat"), gameData("patch000.dat")));
        List<String> msgs = Db.getFileList("text\\english\\game\\*.msg");
        assertEquals(31, msgs.size());
        assertEquals("CMBATAI2.msg", msgs.get(0));
        assertEquals("scrname.msg", msgs.get(msgs.size() - 1));
        Db.freeFileList(msgs);

        // none=0 nonenull=1
        List<String> none = Db.getFileList("no\\such\\*.zzz");
        assertNull(none);
        assertEquals(0, none == null ? 0 : none.size());

        // autorun=1
        assertTrue(Autorun.autorunMutexCreate());
        Autorun.autorunMutexClose();
    }
}
