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
import org.fallout2ce.ObjTypes.ObjectFlags;
import org.fallout2ce.ProtoTypes.ProtoId;
import org.fallout2ce.ProtoTypes.Proto;

/**
 * The inventory metarules of {@code op_metarule}: DROP_ALL_INVEN (42),
 * INVEN_UNWIELD_WHO (43), WEAPON_DAMAGE_TYPE (49) and
 * CRITTER_BARTERS (50), against real protos.
 */
class MetaruleInventoryTest {

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
        PartyMembers.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
    }

    private static Program.ProgramValue pointer(GameObject object) {
        Program.ProgramValue value = new Program.ProgramValue();
        value.opcode = Program.VALUE_TYPE_PTR;
        value.pointerValue = object;
        return value;
    }

    private static GameObject create(int pid) {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoId.ofPid(pid)));
        return slot[0];
    }

    @Test
    void dropAllDropsTheInventoryOnTheTile() {
        GameScriptApi api = new GameScriptApi();
        GameObject dog = create(16777558); // Dog.
        assertEquals(0, ObjectSystem.objectSetLocation(dog, 20100, 0));

        GameObject stim = create(40);
        GameObject cash = create(41);
        assertEquals(0, Item.itemAdd(dog, stim, 1));
        assertEquals(0, Item.itemAdd(dog, cash, 3));

        assertEquals(0, api.metarule(42, pointer(dog)));
        assertEquals(0, dog.data.inventory.items.size());

        // The cash lies on the tile now, still three of it.
        GameObject droppedCash = null;
        for (GameObject o = ObjectSystem.objectFindFirst(); o != null;
                o = ObjectSystem.objectFindNext()) {
            if (o.pid == 41) {
                droppedCash = o;
            }
        }
        assertTrue(droppedCash != null);
        assertEquals(3, droppedCash.data.item.misc.charges);
    }

    @Test
    void unwieldTakesTheHandWeaponOffNonDude() {
        GameScriptApi api = new GameScriptApi();
        GameObject dog = create(16777558);
        GameObject gun = create(390); // A weapon proto (scorcher).

        assertEquals(0, Item.itemAdd(dog, gun, 1));
        gun.flags = gun.flags.or(ObjectFlags.IN_RIGHT_HAND).or(ObjectFlags.EQUIPPED);

        assertEquals(0, api.metarule(43, pointer(dog)));
        assertEquals(0, gun.flags.bits() & ObjectFlags.IN_ANY_HAND.bits());
    }

    @Test
    void weaponDamageTypeReadsTheProto() {
        GameScriptApi api = new GameScriptApi();
        GameObject gun = create(390);

        Proto[] slot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(gun), slot));
        int expected = slot[0].item.data.weapon.damageType.id();

        assertEquals(expected, api.metarule(49, pointer(gun)));

        // A non-weapon item answers 0 (the original's error value).
        GameObject stim = create(40);
        assertEquals(0, api.metarule(49, pointer(stim)));
    }

    @Test
    void barterReadsTheCritterFlag() {
        GameScriptApi api = new GameScriptApi();
        GameObject dog = create(16777558);

        Proto[] slot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(dog), slot));
        int expected = (slot[0].critter.data.flags.bits()
                & ObjTypes.CritterFlags.BARTER.bits()) != 0 ? 1 : 0;

        assertEquals(expected, api.metarule(50, pointer(dog)));

        // A non-critter object is not a barter.
        GameObject stim = create(40);
        assertEquals(0, api.metarule(50, pointer(stim)));
    }
}
