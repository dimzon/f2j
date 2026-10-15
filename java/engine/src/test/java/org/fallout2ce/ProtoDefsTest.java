package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.fallout2ce.ProtoDefs.CritterDataMember;
import org.fallout2ce.ProtoDefs.ItemDataMember;
import org.fallout2ce.ProtoDefs.MiscDataMember;
import org.fallout2ce.ProtoDefs.SceneryDataMember;
import org.fallout2ce.ProtoDefs.WallDataMember;
import org.fallout2ce.ProtoTypes.MiscProtoTypeId;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [prototypes]} data-member vectors from
 * {@code docker/refharness/golden/v34.txt}.
 */
class ProtoDefsTest {

    @Test
    void exitGrids() {
        assertTrue(ProtoDefs.isExitGridProtoId(ProtoId.ofPid((5 << 24) | 16)));
        assertFalse(ProtoDefs.isExitGridProtoId(ProtoId.ofPid((5 << 24) | 15)));
        assertTrue(ProtoDefs.isExitGridProtoId(ProtoId.ofPid((5 << 24) | 23)));
        assertFalse(ProtoDefs.isExitGridProtoId(ProtoId.ofPid((5 << 24) | 24)));
        assertFalse(ProtoDefs.isExitGridProtoId(ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.Stimpak)));
        assertTrue(ProtoDefs.isExitGridProtoId(
            ProtoId.ofMiscType(MiscProtoTypeId.FirstExitGrid)));
        assertTrue(ProtoDefs.isExitGridProtoId(
            ProtoId.ofMiscType(MiscProtoTypeId.LastExitGrid)));
    }

    @Test
    void memberIds() {
        assertEquals(555, ItemDataMember.ITEM_DATA_MEMBER_WEAPON_RANGE.id());
        assertEquals(11, ItemDataMember.ITEM_DATA_MEMBER_MATERIAL.id());
        assertEquals(ItemDataMember.ITEM_DATA_MEMBER_COST, ProtoDefs.ofItemDataMember(14));
        assertNull(ProtoDefs.ofItemDataMember(10));
        assertEquals(10, CritterDataMember.CRITTER_DATA_MEMBER_HEAD_FID.id());
        assertEquals(CritterDataMember.CRITTER_DATA_MEMBER_BODY_TYPE,
            ProtoDefs.ofCritterDataMember(11));
        assertEquals(10, SceneryDataMember.SCENERY_DATA_MEMBER_DATA.id());
        assertEquals(SceneryDataMember.SCENERY_DATA_MEMBER_MATERIAL,
            ProtoDefs.ofSceneryDataMember(11));
        assertEquals(9, WallDataMember.WALL_DATA_MEMBER_MATERIAL.id());
        assertEquals(7, MiscDataMember.MISC_DATA_MEMBER_EXTENDED_FLAGS.id());
        assertEquals(1, ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_INT.id());
        assertEquals(2, ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id());
        assertEquals(ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING,
            ProtoDefs.ofProtoDataMemberType(2));
        assertNull(ProtoDefs.ofProtoDataMemberType(3));
        assertEquals(0, ProtoDefs.PrototypeMessage.PROTOTYPE_MESSAGE_NAME.id());
        assertEquals(1, ProtoDefs.PrototypeMessage.PROTOTYPE_MESSAGE_DESCRIPTION.id());
        assertEquals(ProtoDefs.PrototypeMessage.PROTOTYPE_MESSAGE_DESCRIPTION,
            ProtoDefs.ofPrototypeMessage(1));
    }

    @Test
    void memberValue() {
        ProtoDefs.ProtoDataMemberValue value = new ProtoDefs.ProtoDataMemberValue();
        value.integerValue = 42;
        assertEquals(42, value.integerValue);
        assertNull(value.stringValue);
        value.stringValue = "name";
        assertEquals("name", value.stringValue);
    }
}
