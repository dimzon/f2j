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
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The weapon data readers of the combat slice, against real protos.
 */
class ItemWeaponDataTest {

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
    void weaponReadersMatchTheProto() {
        GameObject gun = create(390); // Scorcher.

        Proto[] slot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(gun), slot));
        Proto weaponProto = slot[0];

        assertEquals((weaponProto.item.extendedFlags.bits()
                & ProtoTypes.ProtoExtendedFlags.IS_TWO_HANDED.bits()) != 0,
            Item.weaponIsTwoHanded(gun));
        assertEquals(weaponProto.item.data.weapon.minStrength,
            Item.weaponGetMinStrengthRequired(gun));
        assertEquals(weaponProto.item.data.weapon.perk,
            Item.weaponGetPerk(gun));

        int expectedAmmoAc = 0;
        ProtoId ammoId = Item.weaponGetAmmoTypeProtoId(gun);
        if (ammoId.valid()) {
            Proto[] ammoSlot = new Proto[1];
            if (ProtoDb.protoGetProto(ammoId, ammoSlot) != -1) {
                expectedAmmoAc = ammoSlot[0].item.data.ammo.armorClassModifier;
            }
        }
        assertEquals(expectedAmmoAc, Item.weaponGetAmmoArmorClassModifier(gun));
    }
}
