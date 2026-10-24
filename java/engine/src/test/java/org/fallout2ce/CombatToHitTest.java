package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * The hit-chance math of the combat slice: {@code attackDetermineToHit}
 * on unarmed attacks, where the base is the skill and the modifiers are
 * the target's armor class and the hit-location penalty.
 */
class CombatToHitTest {

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

    private GameObject attacker;
    private GameObject defender;

    @BeforeEach
    void setup() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        TextObjects.resetForTests();
        Combat.resetForTests();

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
        assertEquals(0, PartyMembers.partyMembersInit());

        byte[] objBuf = new byte[640 * 480];
        assertEquals(0, ObjectSystem.objectsInit(objBuf, 640, 480, 640));

        GameObject[] attackerSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(attackerSlot, ProtoId.ofPid(16777558)));
        attacker = attackerSlot[0];
        assertEquals(0, ObjectSystem.objectSetLocation(attacker, 20100, 0));

        GameObject[] defenderSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(defenderSlot, ProtoId.ofPid(16777558)));
        defender = defenderSlot[0];
        assertEquals(0, ObjectSystem.objectSetLocation(defender, 20101, 0));
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        TextObjects.resetForTests();
        Combat.resetForTests();
    }

    @Test
    void unarmedToHitAppliesTheLocationPenalty() {
        // With the BSS-zeroed penalty table every location is equally
        // accurate.
        int torsoZero = Combat.attackDetermineToHit(attacker, attacker.tile, defender,
            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id(),
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(), false);
        int headZero = Combat.attackDetermineToHit(attacker, attacker.tile, defender,
            CombatDefs.HitLocation.HIT_LOCATION_HEAD.id(),
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(), false);
        assertEquals(torsoZero, headZero);

        // After the default table is restored, the torso is the most
        // accurate and the eyes the least (half the penalty for melee).
        Combat.resetHitLocationPenalty();
        int torso = Combat.attackDetermineToHit(attacker, attacker.tile, defender,
            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id(),
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(), false);
        int head = Combat.attackDetermineToHit(attacker, attacker.tile, defender,
            CombatDefs.HitLocation.HIT_LOCATION_HEAD.id(),
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(), false);
        int eyes = Combat.attackDetermineToHit(attacker, attacker.tile, defender,
            CombatDefs.HitLocation.HIT_LOCATION_EYES.id(),
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(), false);
        assertTrue(torso > head);
        assertTrue(head > eyes);
        assertTrue(eyes >= -100);
        assertTrue(torso <= 95);
    }
}
