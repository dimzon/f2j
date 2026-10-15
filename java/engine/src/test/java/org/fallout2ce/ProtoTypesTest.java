package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.fallout2ce.ProtoTypes.CritterProtoTypeId;
import org.fallout2ce.ProtoTypes.DamageType;
import org.fallout2ce.ProtoTypes.ItemProtoTypeId;
import org.fallout2ce.ProtoTypes.ItemType;
import org.fallout2ce.ProtoTypes.KillType;
import org.fallout2ce.ProtoTypes.MiscProtoTypeId;
import org.fallout2ce.ProtoTypes.ProtoExtendedFlags;
import org.fallout2ce.ProtoTypes.ProtoFlags;
import org.fallout2ce.ProtoTypes.ProtoId;
import org.fallout2ce.ProtoTypes.SceneryProtoTypeId;

/**
 * {@code [prototypes]} vectors from
 * {@code docker/refharness/golden/v34.txt}.
 */
class ProtoTypesTest {

    @Test
    void countsAndValidators() {
        assertEquals(2, ProtoTypes.GENDER_COUNT);
        assertEquals(7, ProtoTypes.ITEM_TYPE_COUNT);
        assertEquals(0, ProtoTypes.ITEM_TYPE_FIRST);
        assertEquals(6, ProtoTypes.SCENERY_TYPE_COUNT);
        assertEquals(8, ProtoTypes.MATERIAL_TYPE_COUNT);
        assertEquals(7, ProtoTypes.DAMAGE_TYPE_COUNT);
        assertEquals(19, ProtoTypes.CALIBER_TYPE_COUNT);
        assertEquals(2, ProtoTypes.RACE_TYPE_COUNT);
        assertEquals(3, ProtoTypes.BODY_TYPE_COUNT);
        assertEquals(19, ProtoTypes.KILL_TYPE_DEFAULT_COUNT);
        assertEquals(38, ProtoTypes.KILL_TYPE_OVERRIDE_COUNT);
        assertEquals(38, ProtoTypes.KILL_TYPE_PLAYER);
        assertFalse(ProtoTypes.damageTypeIsValid(7));
        assertTrue(ProtoTypes.killTypeIsValid(18));
        assertFalse(ProtoTypes.killTypeIsValid(19));
        assertTrue(ProtoTypes.killTypeOverrideIsValid(38));
        assertFalse(ProtoTypes.killTypeOverrideIsValid(39));
    }

    @Test
    void statPlusDamageType() {
        // dt-fire=19 (THRESHOLD+FIRE), dr-plasma=27 (RESISTANCE+PLASMA).
        assertEquals(StatDefs.Stat.STAT_DAMAGE_THRESHOLD_FIRE,
            ProtoTypes.statPlusDamageType(StatDefs.Stat.STAT_DAMAGE_THRESHOLD, DamageType.DAMAGE_TYPE_FIRE));
        assertEquals(StatDefs.Stat.STAT_DAMAGE_RESISTANCE_PLASMA,
            ProtoTypes.statPlusDamageType(StatDefs.Stat.STAT_DAMAGE_RESISTANCE,
                DamageType.DAMAGE_TYPE_PLASMA));
    }

    @Test
    void itemTypeIds() {
        assertEquals(55, ItemProtoTypeId.values().length);
        assertEquals(3, ItemProtoTypeId.PowerArmor.id());
        assertEquals(40, ItemProtoTypeId.Stimpak.id());
        assertEquals(41, ItemProtoTypeId.Money.id());
        assertEquals(259, ItemProtoTypeId.Jet.id());
        assertEquals(390, ItemProtoTypeId.SolarScorcher.id());
        assertEquals(ItemProtoTypeId.Money, ProtoTypes.ofItemProtoTypeId(41));
        assertNull(ProtoTypes.ofItemProtoTypeId(42));
        // post-old=6 post-new=7 (past the end).
        assertEquals(ItemType.ITEM_TYPE_KEY, ProtoTypes.ofItemType(6));
        assertNull(ProtoTypes.ofItemType(6).next());
    }

    @Test
    void otherTypeIds() {
        assertEquals(0, CritterProtoTypeId.Dude.id());
        assertEquals(CritterProtoTypeId.Dude, ProtoTypes.ofCritterProtoTypeId(0));
        assertEquals(342, CritterProtoTypeId.Dogmeat.id());
        assertEquals(1009, SceneryProtoTypeId.Car.id());
        assertEquals(985, SceneryProtoTypeId.FirstRadioactiveGoo.id());
        assertEquals(4, MiscProtoTypeId.Blood.id());
        assertEquals(16, MiscProtoTypeId.FirstExitGrid.id());
        assertEquals(23, MiscProtoTypeId.LastExitGrid.id());
        assertEquals(0, ProtoTypes.ofWallProtoTypeId(0).id());
        assertNull(ProtoTypes.ofWallProtoTypeId(1));
        assertEquals(0, ProtoTypes.ofTileProtoTypeId(0).id());
        assertNull(ProtoTypes.ofTileProtoTypeId(1));
    }

    @Test
    void flags() {
        // proto-or=24 proto-shoot=-2147483648.
        assertEquals(24, ProtoFlags.FLAT.or(ProtoFlags.NO_BLOCK).bits());
        assertEquals(-2147483648, ProtoFlags.SHOOT_THRU.bits());
        // ext-or=768 ext-xor=512 ext-hidden=134217728.
        assertEquals(768, ProtoExtendedFlags.BIG_GUN.or(ProtoExtendedFlags.IS_TWO_HANDED).bits());
        assertEquals(512,
            ProtoExtendedFlags.BIG_GUN.or(ProtoExtendedFlags.IS_TWO_HANDED).xor(ProtoExtendedFlags.BIG_GUN)
                .bits());
        assertEquals(134217728, ProtoExtendedFlags.HIDDEN.bits());
        assertEquals(16, ProtoTypes.PROTO_LIST_EXTENT_SIZE);
        assertEquals(512, ProtoTypes.PROTO_LIST_MAX_ENTRIES);
    }

    @Test
    void protoIdBasics() {
        // empty-pid=-1 empty-haspid=0 empty-valid=0.
        ProtoId empty = ProtoId.empty();
        assertEquals(-1, empty.pid());
        assertFalse(empty.hasPid());
        assertFalse(empty.valid());

        // stim-pid=40 stim-type=0 stim-protoid=40.
        ProtoId stim = ProtoId.ofItemType(ItemProtoTypeId.Stimpak);
        assertEquals(40, stim.pid());
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_ITEM, stim.objectType());
        assertEquals(40, stim.protoId());
        assertTrue(stim.valid());

        // dog-pid=16777558 dog-type=1.
        ProtoId dog = ProtoId.ofCritterType(CritterProtoTypeId.Dogmeat);
        assertEquals(16777558, dog.pid());
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_CRITTER, dog.objectType());
        assertEquals(342, dog.protoId());

        // from-pid round-trip through a raw int.
        ProtoId fromPid = ProtoId.ofPid(40);
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_ITEM, fromPid.objectType());
        assertEquals(40, fromPid.protoId());
        assertTrue(fromPid.valid());
        assertEquals(stim, fromPid);
        assertEquals(stim.hashCode(), fromPid.hashCode());

        // Wide object type (>= PROTO_COUNT) is clamped to INVALID.
        ProtoId wide = ProtoId.ofPid((7 << 24) | 5);
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_INVALID, wide.objectType());
        assertFalse(wide.valid());
    }

    @Test
    void protoIdComparisons() {
        ProtoId stim = ProtoId.ofItemType(ItemProtoTypeId.Stimpak);
        assertTrue(stim.matchesItemType(ItemProtoTypeId.Stimpak));
        assertTrue(stim.matchesItemType(ItemProtoTypeId.Money) == false);
        assertTrue(ProtoId.ofPid(40).matchesItemType(ItemProtoTypeId.Stimpak));
        assertFalse(stim.matchesCritterType(CritterProtoTypeId.Dude));

        assertEquals(ItemProtoTypeId.Stimpak, stim.asItemTypeId());
        assertEquals(MiscProtoTypeId.Blood,
            ProtoId.ofMiscType(MiscProtoTypeId.Blood).asMiscTypeId());
        // Mismatched typed accessor trips the debug check (C++ assert).
        assertThrows(AssertionError.class, () -> stim.asCritterTypeId());
        // Unknown ids give null (C++ yields an out-of-range value).
        assertNull(ProtoId.ofPid(42).asItemTypeId());
    }

    @Test
    void typeMapping() {
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_ITEM,
            ProtoTypes.objectTypeOf(ItemProtoTypeId.Stimpak));
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_CRITTER,
            ProtoTypes.objectTypeOf(CritterProtoTypeId.Dogmeat));
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_SCENERY,
            ProtoTypes.objectTypeOf(SceneryProtoTypeId.Car));
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_WALL,
            ProtoTypes.objectTypeOf(ProtoTypes.WallProtoTypeId.Reserved));
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_TILE,
            ProtoTypes.objectTypeOf(ProtoTypes.TileProtoTypeId.Reserved));
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_MISC,
            ProtoTypes.objectTypeOf(MiscProtoTypeId.Blood));
    }

    @Test
    void structShapes() {
        ProtoTypes.ItemProto item = new ProtoTypes.ItemProto();
        assertEquals(7, item.data.armor.damageResistance.length);
        assertEquals(7, item.data.armor.damageThreshold.length);
        assertNull(item.data.armor.perk);
        assertEquals(3, item.data.drug.stat.length);

        ProtoTypes.CritterProto critter = new ProtoTypes.CritterProto();
        assertEquals(StatDefs.SAVEABLE_STAT_COUNT, critter.data.baseStats.length);
        assertEquals(StatDefs.SAVEABLE_STAT_COUNT, critter.data.bonusStats.length);
        assertEquals(SkillDefs.SKILL_COUNT, critter.data.skills.length);

        ProtoTypes.Proto proto = new ProtoTypes.Proto();
        assertEquals(0, proto.pid);
        ProtoTypes.ProtoListExtent extent = new ProtoTypes.ProtoListExtent();
        assertEquals(ProtoTypes.PROTO_LIST_EXTENT_SIZE, extent.proto.length);
        ProtoId fromProto = ProtoId.ofProto(proto);
        assertTrue(fromProto.hasPid());
        assertTrue(fromProto.valid());

        ObjTypes.GameObject obj = new ObjTypes.GameObject();
        obj.pid = 40;
        assertEquals(40, ProtoId.ofGameObject(obj).pid());
        assertNull(ProtoId.ofGameObject(null).asItemTypeId());
    }
}
