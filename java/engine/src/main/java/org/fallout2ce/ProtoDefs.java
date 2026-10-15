package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Prototype data-member indexes, mirroring the pure parts of
 * {@code src/proto.h}. The registry functions ({@code proto.cc}) come with
 * the game phase.
 */
public final class ProtoDefs {

    private ProtoDefs() {
    }

    public enum ItemDataMember {
        ITEM_DATA_MEMBER_PID(0),
        ITEM_DATA_MEMBER_NAME(1),
        ITEM_DATA_MEMBER_DESCRIPTION(2),
        ITEM_DATA_MEMBER_FID(3),
        ITEM_DATA_MEMBER_LIGHT_DISTANCE(4),
        ITEM_DATA_MEMBER_LIGHT_INTENSITY(5),
        ITEM_DATA_MEMBER_FLAGS(6),
        ITEM_DATA_MEMBER_EXTENDED_FLAGS(7),
        ITEM_DATA_MEMBER_SID(8),
        ITEM_DATA_MEMBER_TYPE(9),
        ITEM_DATA_MEMBER_MATERIAL(11),
        ITEM_DATA_MEMBER_SIZE(12),
        ITEM_DATA_MEMBER_WEIGHT(13),
        ITEM_DATA_MEMBER_COST(14),
        ITEM_DATA_MEMBER_INVENTORY_FID(15),
        ITEM_DATA_MEMBER_WEAPON_RANGE(555);

        private final int id;

        ItemDataMember(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, ItemDataMember> ITEMMEMBER_BY_ID = new HashMap<>();

    static {
        for (ItemDataMember e : ItemDataMember.values()) {
            ITEMMEMBER_BY_ID.put(e.id, e);
        }
    }

    public static ItemDataMember ofItemDataMember(int id) {
        return ITEMMEMBER_BY_ID.get(id);
    }

    public enum CritterDataMember {
        CRITTER_DATA_MEMBER_PID(0),
        CRITTER_DATA_MEMBER_NAME(1),
        CRITTER_DATA_MEMBER_DESCRIPTION(2),
        CRITTER_DATA_MEMBER_FID(3),
        CRITTER_DATA_MEMBER_LIGHT_DISTANCE(4),
        CRITTER_DATA_MEMBER_LIGHT_INTENSITY(5),
        CRITTER_DATA_MEMBER_FLAGS(6),
        CRITTER_DATA_MEMBER_EXTENDED_FLAGS(7),
        CRITTER_DATA_MEMBER_SID(8),
        CRITTER_DATA_MEMBER_DATA(9),
        CRITTER_DATA_MEMBER_HEAD_FID(10),
        CRITTER_DATA_MEMBER_BODY_TYPE(11);

        private final int id;

        CritterDataMember(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, CritterDataMember> CRITTERMEMBER_BY_ID = new HashMap<>();

    static {
        for (CritterDataMember e : CritterDataMember.values()) {
            CRITTERMEMBER_BY_ID.put(e.id, e);
        }
    }

    public static CritterDataMember ofCritterDataMember(int id) {
        return CRITTERMEMBER_BY_ID.get(id);
    }

    public enum SceneryDataMember {
        SCENERY_DATA_MEMBER_PID(0),
        SCENERY_DATA_MEMBER_NAME(1),
        SCENERY_DATA_MEMBER_DESCRIPTION(2),
        SCENERY_DATA_MEMBER_FID(3),
        SCENERY_DATA_MEMBER_LIGHT_DISTANCE(4),
        SCENERY_DATA_MEMBER_LIGHT_INTENSITY(5),
        SCENERY_DATA_MEMBER_FLAGS(6),
        SCENERY_DATA_MEMBER_EXTENDED_FLAGS(7),
        SCENERY_DATA_MEMBER_SID(8),
        SCENERY_DATA_MEMBER_TYPE(9),
        SCENERY_DATA_MEMBER_DATA(10),
        SCENERY_DATA_MEMBER_MATERIAL(11);

        private final int id;

        SceneryDataMember(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, SceneryDataMember> SCENERYMEMBER_BY_ID = new HashMap<>();

    static {
        for (SceneryDataMember e : SceneryDataMember.values()) {
            SCENERYMEMBER_BY_ID.put(e.id, e);
        }
    }

    public static SceneryDataMember ofSceneryDataMember(int id) {
        return SCENERYMEMBER_BY_ID.get(id);
    }

    public enum WallDataMember {
        WALL_DATA_MEMBER_PID(0),
        WALL_DATA_MEMBER_NAME(1),
        WALL_DATA_MEMBER_DESCRIPTION(2),
        WALL_DATA_MEMBER_FID(3),
        WALL_DATA_MEMBER_LIGHT_DISTANCE(4),
        WALL_DATA_MEMBER_LIGHT_INTENSITY(5),
        WALL_DATA_MEMBER_FLAGS(6),
        WALL_DATA_MEMBER_EXTENDED_FLAGS(7),
        WALL_DATA_MEMBER_SID(8),
        WALL_DATA_MEMBER_MATERIAL(9);

        private final int id;

        WallDataMember(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, WallDataMember> WALLMEMBER_BY_ID = new HashMap<>();

    static {
        for (WallDataMember e : WallDataMember.values()) {
            WALLMEMBER_BY_ID.put(e.id, e);
        }
    }

    public static WallDataMember ofWallDataMember(int id) {
        return WALLMEMBER_BY_ID.get(id);
    }

    public enum MiscDataMember {
        MISC_DATA_MEMBER_PID(0),
        MISC_DATA_MEMBER_NAME(1),
        MISC_DATA_MEMBER_DESCRIPTION(2),
        MISC_DATA_MEMBER_FID(3),
        MISC_DATA_MEMBER_LIGHT_DISTANCE(4),
        MISC_DATA_MEMBER_LIGHT_INTENSITY(5),
        MISC_DATA_MEMBER_FLAGS(6),
        MISC_DATA_MEMBER_EXTENDED_FLAGS(7);

        private final int id;

        MiscDataMember(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, MiscDataMember> MISCMEMBER_BY_ID = new HashMap<>();

    static {
        for (MiscDataMember e : MiscDataMember.values()) {
            MISCMEMBER_BY_ID.put(e.id, e);
        }
    }

    public static MiscDataMember ofMiscDataMember(int id) {
        return MISCMEMBER_BY_ID.get(id);
    }

    public enum ProtoDataMemberType {
        PROTO_DATA_MEMBER_TYPE_INT(1),
        PROTO_DATA_MEMBER_TYPE_STRING(2);

        private final int id;

        ProtoDataMemberType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, ProtoDataMemberType> MEMBERTYPE_BY_ID = new HashMap<>();

    static {
        for (ProtoDataMemberType e : ProtoDataMemberType.values()) {
            MEMBERTYPE_BY_ID.put(e.id, e);
        }
    }

    public static ProtoDataMemberType ofProtoDataMemberType(int id) {
        return MEMBERTYPE_BY_ID.get(id);
    }

    /** Port of {@code ProtoDataMemberValue} (int/string union). */
    public static final class ProtoDataMemberValue {
        public int integerValue;
        public String stringValue;
    }

    public enum PrototypeMessage {
        PROTOTYPE_MESSAGE_NAME(0),
        PROTOTYPE_MESSAGE_DESCRIPTION(1);

        private final int id;

        PrototypeMessage(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, PrototypeMessage> MESSAGE_BY_ID = new HashMap<>();

    static {
        for (PrototypeMessage e : PrototypeMessage.values()) {
            MESSAGE_BY_ID.put(e.id, e);
        }
    }

    public static PrototypeMessage ofPrototypeMessage(int id) {
        return MESSAGE_BY_ID.get(id);
    }

    /** Port of {@code isExitGridProtoId} (proto.h). */
    public static boolean isExitGridProtoId(ProtoTypes.ProtoId protoId) {
        return protoId.pid() >= ProtoTypes.ProtoId.ofMiscType(ProtoTypes.MiscProtoTypeId.FirstExitGrid).pid()
                && protoId.pid() <= ProtoTypes.ProtoId.ofMiscType(ProtoTypes.MiscProtoTypeId.LastExitGrid)
                        .pid();
    }
}
