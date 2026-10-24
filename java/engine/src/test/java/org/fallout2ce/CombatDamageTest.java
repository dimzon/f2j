package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.ObjTypes;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The damage slice of the combat: {@code weaponGetDamage}, the unarmed
 * hit table and the ammo modifiers, against real protos.
 */
class CombatDamageTest {

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
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
    }

    private static GameObject create(int pid) {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoId.ofPid(pid)));
        return slot[0];
    }

    @Test
    void weaponDamageFallsInTheProtoRange() {
        GameObject dude = create(16777558); // Dog (a critter).
        GameObject gun = create(390); // Scorcher.
        assertEquals(0, Item.itemAdd(dude, gun, 1));
        gun.flags = gun.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND)
            .or(ObjTypes.ObjectFlags.EQUIPPED);

        Proto[] slot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(gun), slot));
        int min = slot[0].item.data.weapon.minDamage;
        int max = slot[0].item.data.weapon.maxDamage;

        // The random draw stays in the proto range; the melee-damage stat
        // of the (non-dude) critter is 0, so the max matches the proto.
        int damage = Item.weaponGetDamage(dude, CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
        assertTrue(damage >= min && damage <= max, damage + " vs " + min + ".." + max);
    }

    @Test
    void ammoModifiersMatchTheProto() {
        GameObject gun = create(390);

        Proto[] slot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(gun), slot));

        int expectedMult = 1;
        int expectedDiv = 1;
        ProtoId ammoId = Item.weaponGetAmmoTypeProtoId(gun);
        if (ammoId.valid()) {
            Proto[] ammoSlot = new Proto[1];
            if (ProtoDb.protoGetProto(ammoId, ammoSlot) != -1) {
                expectedMult = ammoSlot[0].item.data.ammo.damageMultiplier;
                expectedDiv = ammoSlot[0].item.data.ammo.damageDivisor;
            }
        }

        assertEquals(expectedMult, Item.weaponGetAmmoDamageMultiplier(gun));
        assertEquals(expectedDiv, Item.weaponGetAmmoDamageDivisor(gun));
    }

    @Test
    void unarmedTableHasTheVanillaPunches() {
        int[] punch = Item.unarmedMinMax(CombatDefs.HitMode.HIT_MODE_PUNCH.id());
        assertEquals(1, punch[0]);
        assertEquals(2, punch[1]);
        assertEquals(0, punch[2]);

        int[] strong = Item.unarmedMinMax(CombatDefs.HitMode.HIT_MODE_STRONG_PUNCH.id());
        assertEquals(1, strong[0]);
        assertEquals(2, strong[1]);
        assertEquals(3, strong[2]);

        int[] hammer = Item.unarmedMinMax(CombatDefs.HitMode.HIT_MODE_HAMMER_PUNCH.id());
        assertEquals(5, hammer[2]);

        // HIT_MODE_KICK is 5 in the enum; the table is keyed by the id.
        int[] kick = Item.unarmedMinMax(CombatDefs.HitMode.HIT_MODE_KICK.id());
        assertEquals(1, kick[0]);
        assertEquals(2, kick[1]);
        assertEquals(0, kick[2]);
    }
}
