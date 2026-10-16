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
import org.fallout2ce.ObjTypes.ObjectFlags;

/**
 * {@code [partyobjects]} vectors from
 * {@code docker/refharness/golden/v96.txt}, produced by
 * {@code sectionPartyObjects} in {@code docker/refharness/ref_main.cc}:
 * {@code party=0 objects=0 dude=16777216:18000 egg=-1:<counter>
 * dup-add=0}.
 *
 * <p>Id-counter coupling (adapted from C, where ~60 earlier sections
 * leave the egg at 130): the Java test starts fresh
 * ({@code objectIdCounter = 4}). The dude create mints id 6 and then
 * overwrites it with the fixed {@code (pid & kMaxProtoId) + 18000} =
 * 18000 (the counter is untouched); the egg create is the next counter
 * use ({@code 6 -> 7} scan, {@code 7 -> 8} return), so the egg id is 8.
 * Nothing else in the setup consumes the counter (both objects stay
 * floating, invisible to the {@code scriptsNewObjectId} tile-head scan).
 *
 * <p>Isolation: {@link Stat}, {@link Skill}, {@link Critter},
 * {@link Inventory} and {@link Item} key their dude branches off their
 * own null holders, so identity comparison against them can never fire
 * for this live dude (dormant by construction); the test still clears
 * the holders and lists at the end, and the full suite must stay green
 * (especially Stat/Skill/Critter tests).
 *
 * <p>Setup mirrors {@link ObjPlaceTest} (master + patch + critter mounts
 * for art/proto loads) plus the harness's effective tile state
 * ({@link TileMathTest} geometry + center 17488, which the offset-table
 * init walks).
 */
class PartyObjectsTest {

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
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/patch000.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/critter.dat"));
        assertEquals(0, Art.artInit());
        byte[] tileBuf = new byte[640 * 480];
        assertEquals(0, TileMath.tileInit(null, 100, 100, 200, 200, tileBuf, 640, 480, 640,
                rect -> {
                }));
        assertEquals(0, TileMath.tileSetCenter(17488,
                TileMath.TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS));
    }

    @AfterEach
    void teardown() {
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Db.resetForTests();
        ProtoDb.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
    }

    @Test
    void partyObjectsVectors() {
        // party=0 (26 descriptions parsed from data\party.txt).
        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(26, PartyMembers.descriptionCount());

        // objects=0.
        byte[] objBuf = new byte[640 * 480];
        assertEquals(0, ObjectSystem.objectsInit(objBuf, 640, 480, 640));
        assertTrue(TextObjects.isInitialized());

        // dude=16777216:18000.
        GameObject dude = ObjectSystem.getDude();
        assertNotNull(dude);
        assertEquals(16777216, dude.pid);
        assertEquals(18000, dude.id);
        int dudeFlags = dude.flags.bits();
        int requiredFlags = ObjectFlags.NO_REMOVE.bits() | ObjectFlags.NO_SAVE.bits()
                | ObjectFlags.HIDDEN.bits() | ObjectFlags.LIGHT_THRU.bits();
        assertEquals(requiredFlags, dudeFlags & requiredFlags);
        assertEquals(4, dude.lightDistance);
        assertEquals(0x10000, dude.lightIntensity);

        // egg=-1:8 (counter trace in the class doc; C shows 130 from leftovers).
        GameObject egg = ObjectSystem.getEgg();
        assertNotNull(egg);
        assertEquals(-1, egg.pid);
        assertEquals(8, egg.id);
        assertEquals(requiredFlags, egg.flags.bits() & requiredFlags);

        // dup-add=0 (pointer-or-pid match, not a duplicate).
        assertEquals(1, PartyMembers.partyLength());
        assertEquals(0, PartyMembers.partyMemberAdd(dude));
        assertEquals(1, PartyMembers.partyLength());

        // Reset keeps the roster/holders (remove-all skips NO_REMOVE).
        ObjectSystem.objectsReset();
        assertNotNull(ObjectSystem.getDude());
        assertNotNull(ObjectSystem.getEgg());
        assertEquals(1, PartyMembers.partyLength());

        // Exit clears engine state; explicit resets clear the holders.
        ObjectSystem.objectsExit();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        assertNull(ObjectSystem.getDude());
        assertNull(ObjectSystem.getEgg());
        assertEquals(0, PartyMembers.partyLength());
    }
}
