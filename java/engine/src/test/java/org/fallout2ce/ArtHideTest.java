package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [arthide]} vectors from
 * {@code docker/refharness/golden/v73.txt}, produced by
 * {@code sectionArtHide} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>C vectors (verbatim):
 * {@code hide=0 bad=0 on=1 blindnull=0 blindpid=-1 restored=0 sighted=1}.
 * The blind find in C succeeds with pid -1: those are the game-mouse
 * cursor objects (Empty pid) left over from {@code [gamemouse]} plus old
 * drops — items are hidden, cursors stay visible.
 *
 * <p>This test runs fresh (no cursor objects, no drops): with items
 * hidden and only its own dropped stim on tiles, {@code objectFindFirst}
 * must return null (every tile object is an item). Adapted expectations:
 * {@code hide=0 bad=0 on=1 blindnull=1 restored=0 sighted=1} — same codes
 * except {@code blindnull=1}, and no {@code blindpid} line (null find has
 * no pid to print). Game data needed for the {@code .pro} loads (setup
 * mirrors {@link ObjFindTest}).
 */
class ArtHideTest {

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path p = Paths.get(root, name);
        if (!Files.isRegularFile(p)) {
            throw new IllegalStateException("missing " + p);
        }
        return p.toString();
    }

    @BeforeEach
    void setup() {
        Art.resetForTests();
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
    }

    @AfterEach
    void teardown() {
        Art.resetForTests();
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
    }

    @Test
    void arthideVectors() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);
        GameObject stim = stimSlot[0];

        assertEquals(0, Item.itemAdd(dog, stim, 1));
        assertEquals(0, Item.itemDropAll(dog, 20100));

        // Sanity: the dropped stim is visible before hiding.
        GameObject pre = ObjectSystem.objectFindFirst();
        assertNotNull(pre);
        assertEquals(40, pre.pid);

        int hide0 = Art.artIsObjectTypeHidden(ObjectType.ITEM.value());
        int hideBad = Art.artIsObjectTypeHidden(ObjectType.INVALID.value());
        assertEquals(0, hide0);
        assertEquals(0, hideBad);

        Art.artToggleObjectTypeHidden(ObjectType.ITEM.value());
        try {
            int hide1 = Art.artIsObjectTypeHidden(ObjectType.ITEM.value());
            assertEquals(1, hide1);

            // Fresh process: every tile object is an item, so the blind
            // find sees nothing (C saw cursor objects with pid -1 here).
            GameObject blindFind = ObjectSystem.objectFindFirst();
            assertNull(blindFind);
        } finally {
            Art.artToggleObjectTypeHidden(ObjectType.ITEM.value());
        }

        int hideRestored = Art.artIsObjectTypeHidden(ObjectType.ITEM.value());
        assertEquals(0, hideRestored);

        GameObject sightedFind = ObjectSystem.objectFindFirst();
        assertNotNull(sightedFind);
        assertEquals(40, sightedFind.pid);
    }
}
