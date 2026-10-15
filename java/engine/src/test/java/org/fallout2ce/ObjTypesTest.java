package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.fallout2ce.ObjTypes.CritterFlags;
import org.fallout2ce.ObjTypes.CritterManeuver;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjTypes.ObjectFlags;
import org.fallout2ce.ObjTypes.ObjectType;
import org.fallout2ce.ObjTypes.OutlineType;
import org.fallout2ce.ObjTypes.Rotation;

/**
 * {@code [objtypes]} vectors from
 * {@code docker/refharness/golden/v33.txt}.
 */
class ObjTypesTest {

    @Test
    void rotation() {
        assertEquals(0, ObjTypes.ROTATION_FIRST);
        assertEquals(5, ObjTypes.ROTATION_LAST);
        assertEquals(6, ObjTypes.ROTATION_COUNT);
        // add=2 sub=1 mod=5 post-old=5 post-new=6 pre-new=-1.
        assertEquals(Rotation.ROTATION_SE, Rotation.ROTATION_NE.plus(2));
        assertEquals(Rotation.ROTATION_E, Rotation.ROTATION_W.minus(3));
        assertEquals(Rotation.ROTATION_NW, Rotation.ROTATION_NW.mod(ObjTypes.ROTATION_COUNT));
        assertEquals(Rotation.ROTATION_NW, ObjTypes.ofRotation(5));
        assertNull(ObjTypes.ofRotation(5).next());
        assertEquals(Rotation.ROTATION_INVALID, ObjTypes.ofRotation(0).prev());
        assertFalse(ObjTypes.rotationIsValid(-1));
        assertTrue(ObjTypes.rotationIsValid(5));
        assertFalse(ObjTypes.rotationIsValid(6));
    }

    @Test
    void objectType() {
        assertEquals(11, ObjTypes.OBJ_TYPE_COUNT);
        assertEquals(0, ObjTypes.OBJ_TYPE_FIRST);
        assertEquals(6, ObjTypes.OBJ_TYPE_PROTO_COUNT);
        assertTrue(ObjTypes.objectTypeIsValid(10));
        assertFalse(ObjTypes.objectTypeIsValid(11));
        // post-old=3 post-new=4.
        assertEquals(ObjectType.OBJ_TYPE_WALL, ObjTypes.ofObjectType(3));
        assertEquals(ObjectType.OBJ_TYPE_TILE, ObjTypes.ofObjectType(3).next());
        assertEquals(ObjectType.OBJ_TYPE_INVALID, ObjTypes.ofObjectType(-1));
    }

    @Test
    void outlineType() {
        // outline-or=17 outline-and=0 outline-andu=32 outline-oru=48.
        assertEquals(17, OutlineType.HOSTILE.or(OutlineType.ITEM).bits());
        assertEquals(0, OutlineType.HOSTILE.and(OutlineType.ITEM).bits());
        assertEquals(32, OutlineType.BLOCKED.and(0xFF).bits());
        assertEquals(48, OutlineType.BLOCKED.or(OutlineType.ITEM).bits());
        assertEquals(0x40000000, ObjTypes.OUTLINE_PALETTED);
        assertEquals(0x80000000, ObjTypes.OUTLINE_DISABLED);
    }

    @Test
    void objectFlags() {
        assertEquals(50331648, ObjectFlags.IN_ANY_HAND.bits());
        assertEquals(117440512, ObjectFlags.EQUIPPED.bits());
        assertEquals(1032192, ObjectFlags.FLAG_0XFC000.bits());
        assertEquals(-1610612720, ObjectFlags.OPEN_DOOR.bits());
        // xor=5 not=-1 or=48 xoreq=16 andeq=16.
        assertEquals(5, ObjectFlags.HIDDEN.xor(ObjectFlags.NO_SAVE).bits());
        assertEquals(-1, ObjectFlags.NONE.not().bits());
        assertEquals(48, ObjectFlags.NO_BLOCK.or(ObjectFlags.LIGHTING).bits());
        assertEquals(16, ObjectFlags.NO_BLOCK.or(ObjectFlags.LIGHTING).xor(ObjectFlags.LIGHTING).bits());
        assertEquals(16,
            ObjectFlags.NO_BLOCK.or(ObjectFlags.LIGHTING).xor(ObjectFlags.LIGHTING)
                .and(ObjectFlags.NO_BLOCK).bits());
    }

    @Test
    void dudeAndCritterFlags() {
        assertEquals(5, ObjTypes.DUDE_STATE_COUNT);
        assertTrue(ObjTypes.dudeStateIsValid(4));
        assertFalse(ObjTypes.dudeStateIsValid(5));
        // crit-or=96 crit-not=-1 crit-andeq=512 crit-oreq=1536.
        assertEquals(96, CritterFlags.NO_STEAL.or(CritterFlags.NO_DROP).bits());
        assertEquals(-1, CritterFlags.NONE.not().bits());
        assertEquals(512, CritterFlags.NO_HEAL.and(CritterFlags.NO_HEAL).bits());
        assertEquals(1536, CritterFlags.NO_HEAL.or(CritterFlags.INVULNERABLE).bits());
        assertEquals(1, ObjTypes.CONTAINER_FLAG_OPEN);
        assertEquals(33554432, ObjTypes.DOOR_FLAG_LOCKED);
        assertEquals(33554432, ObjTypes.OBJ_LOCKED);
        assertEquals(67108864, ObjTypes.OBJ_JAMMED);
    }

    @Test
    void maneuverAndDam() {
        // man-or=3 man-not=-1.
        assertEquals(4, CritterManeuver.FLEEING.bits());
        assertEquals(3, CritterManeuver.ENGAGING.or(CritterManeuver.DISENGAGING).bits());
        assertEquals(-1, CritterManeuver.NONE.not().bits());
        // dam-cripleg=12 dam-criparm=48 dam-crip=124 dam-or=384 dam-not=-1.
        assertEquals(12, Dam.CRIP_LEG_ANY.bits());
        assertEquals(48, Dam.CRIP_ARM_ANY.bits());
        assertEquals(124, Dam.CRIP.bits());
        assertEquals(384, Dam.DEAD.or(Dam.HIT).bits());
        assertEquals(-1, Dam.NONE.not().bits());
        // dam-oreq=3072 dam-andeq=1024.
        assertEquals(3072, Dam.ON_FIRE.or(Dam.BYPASS).bits());
        assertEquals(1024, Dam.ON_FIRE.or(Dam.BYPASS).and(Dam.ON_FIRE).bits());
    }

    @Test
    void builtTile() {
        assertEquals(67108863, ObjTypes.BUILT_TILE_TILE_MASK);
        assertEquals(-536870912, ObjTypes.BUILT_TILE_ELEVATION_MASK);
        assertEquals(29, ObjTypes.BUILT_TILE_ELEVATION_SHIFT);
        assertEquals(469762048, ObjTypes.BUILT_TILE_ROTATION_MASK);
        assertEquals(26, ObjTypes.BUILT_TILE_ROTATION_SHIFT);

        // bt=1075494110 bt-tile=1752286 bt-elev=2 bt-rot=0.
        int bt = ObjTypes.builtTileCreate(0x1ABCDE, 2);
        assertEquals(1075494110, bt);
        assertEquals(1752286, ObjTypes.builtTileGetTile(bt));
        assertEquals(2, ObjTypes.builtTileGetElevation(bt));
        assertEquals(Rotation.ROTATION_NE, ObjTypes.builtTileGetRotation(bt));

        // Full word: tile/elevation/rotation round-trip, elevation 7 of a
        // negative word reads back as 7 (unsigned expression in C++).
        int full = ObjTypes.builtTileCreate(0x3FFFFFF, 7) | (5 << 26);
        assertEquals(-134217729, full);
        assertEquals(67108863, ObjTypes.builtTileGetTile(full));
        assertEquals(7, ObjTypes.builtTileGetElevation(full));
        assertEquals(Rotation.ROTATION_NW, ObjTypes.builtTileGetRotation(full));
    }

    @Test
    void gameObjectDefaults() {
        GameObject obj = new GameObject();
        assertEquals(0, obj.id);
        assertEquals(Rotation.ROTATION_NE, obj.rotation);
        assertEquals(ObjectFlags.NONE, obj.flags);
        assertEquals(OutlineType.NONE, obj.outline);
        assertNull(obj.owner);
        assertNull(obj.data.critter.combat.whoHitMe);
        assertEquals(0, obj.data.critter.combat.whoHitMeCid);
        assertTrue(obj.data.inventory.items.isEmpty());
        assertNull(obj.data.scenery.stairs.destinationMap);
        assertNull(obj.data.misc.map);

        obj.pid = 0x10000;
        obj.data.critter.hp = 45;
        obj.data.critter.combat.ap = 8;
        obj.data.critter.combat.results = Dam.HIT.or(Dam.CRITICAL);
        obj.data.inventory.items.add(new ObjTypes.InventoryItem());
        assertEquals(1, obj.data.inventory.items.size());
        assertEquals(0x300, obj.data.critter.combat.results.bits());

        ObjTypes.GameObjectListNode node = new ObjTypes.GameObjectListNode();
        node.obj = obj;
        assertEquals(obj, node.obj);
        assertNull(node.next);
    }
}
