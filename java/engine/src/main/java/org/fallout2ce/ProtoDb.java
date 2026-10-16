package org.fallout2ce;

import org.fallout2ce.ProtoTypes.CritterProtoTypeId;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Prototype database read + creation slice, mirroring {@code src/proto.cc}:
 * path building ({@code proto_make_path}), {@code .lst} mapping
 * ({@code _proto_list_str}), {@code .pro} binary parse ({@code protoRead} and
 * per-type sub-reads), the cache lists with eviction
 * ({@code _proto_find_free_subnode}/{@code _proto_remove_some_list}),
 * {@code protoGetProto} (Dude shortcut, list scan, eviction guard,
 * {@code protoLoadProtoId}), {@code protoGetDataMember} with its
 * stale-value quirk, {@code proto_new} (counter {@code _proto_new_id} +
 * per-type {@code proto_*_init} defaults), {@code proto_copy_proto}
 * (memcpy-sized duplication with the dst pid stamped over the copy) and
 * {@code proto_is_subtype} (the type-tag comparison).
 *
 * <p>VFS reads reuse {@link Db#fileOpen} / {@code fileRead*} (big-endian,
 * like C {@code fileReadInt32} / {@code _db_freadInt} which are aliases).
 * The full {@code protoInit} (mkdirs + message/stat/perk stack) is
 * deliberately skipped, like the harness: names stay unavailable.
 */
public final class ProtoDb {

    private ProtoDb() {
    }

    /** Mirror of {@code _cd_path_base} (empty without game init). */
    static String cdPathBase = "";

    /** Mirror of {@code _proto_path_base} ({@code "proto\\"}). */
    static final String PROTO_PATH_BASE = "proto\\";

    static final ProtoTypes.ProtoList[] protoLists = new ProtoTypes.ProtoList[ObjTypes.OBJ_TYPE_COUNT];

    /** Mirror of {@code gDudeProto} (Dude shortcut target; vectors never touch it). */
    static final Proto dudeProto = new Proto();

    static {
        for (int i = 0; i < protoLists.length; i++) {
            ProtoTypes.ProtoList list = new ProtoTypes.ProtoList();
            list.head = null;
            list.tail = null;
            list.length = 0;
            list.maxEntriesNum = i < ObjTypes.OBJ_TYPE_PROTO_COUNT ? 1 : 0;
            protoLists[i] = list;
        }
        int dudePid = ProtoId.ofCritterType(CritterProtoTypeId.Dude).pid();
        dudeProto.pid = dudePid;
        dudeProto.critter.pid = dudePid;
    }

    // 0x49E270 proto_make_path.
    public static String protoMakePath(ProtoId protoId) {
        StringBuilder path = new StringBuilder();
        path.append(cdPathBase);
        path.append(PROTO_PATH_BASE);
        if (protoId.valid()) {
            String typeName = Art.artGetObjectTypeName(protoId.objectType().id());
            if (typeName != null) {
                path.append(typeName);
            }
        }
        return path.toString();
    }

    // 0x49E758 proto_list_str: maps pid -> filename via proto\{type}\{type}.lst.
    // Writes only the file name into out (caller appends to the directory).
    public static int protoListStr(ProtoId protoId, StringBuilder out) {
        if (!protoId.valid()) {
            return -1;
        }
        if (out == null) {
            return -1;
        }

        String typeName = Art.artGetObjectTypeName(protoId.objectType().id());
        if (typeName == null) {
            return -1;
        }
        String path = protoMakePath(protoId) + "\\" + typeName + ".lst";

        XFile.Handle stream = Db.fileOpen(path, "rt");
        if (stream == null) {
            return -1;
        }

        int i = 1;
        String line = null;
        String last = null;
        int target = protoId.protoId();
        while ((line = Db.fileReadString(stream, 256)) != null) {
            last = line;
            if (i == target) {
                break;
            }
            i++;
        }

        Db.fileClose(stream);

        if (i != target || last == null) {
            return -1;
        }

        String name = last;
        int sp = name.indexOf(' ');
        if (sp >= 0) {
            name = name.substring(0, sp);
        }
        int cr = name.indexOf('\r');
        if (cr >= 0) {
            name = name.substring(0, cr);
        }
        int lf = name.indexOf('\n');
        if (lf >= 0) {
            name = name.substring(0, lf);
        }

        out.setLength(0);
        out.append(name);
        return 0;
    }

    // 0x42DF70 critter_read_data (lives in critter.cc).
    static int protoCritterDataRead(XFile.Handle stream, ProtoTypes.CritterProtoData data) {
        int[] v = new int[1];
        if (Db.fileReadInt32(stream, v) == -1) {
            return -1;
        }
        data.flags = new ObjTypes.CritterFlags(v[0]);
        if (Db.fileReadInt32List(stream, data.baseStats, 0, StatDefs.SAVEABLE_STAT_COUNT) == -1) {
            return -1;
        }
        if (Db.fileReadInt32List(stream, data.bonusStats, 0, StatDefs.SAVEABLE_STAT_COUNT) == -1) {
            return -1;
        }
        if (Db.fileReadInt32List(stream, data.skills, 0, SkillDefs.SKILL_COUNT) == -1) {
            return -1;
        }
        if (Db.fileReadInt32(stream, v) == -1) {
            return -1;
        }
        data.bodyType = ProtoTypes.ofBodyType(v[0]);
        if (Db.fileReadInt32(stream, v) == -1) {
            return -1;
        }
        data.experience = v[0];
        if (Db.fileReadInt32(stream, v) == -1) {
            return -1;
        }
        data.killType = ProtoTypes.ofKillType(v[0]);
        // Damage type is optional (two protos are 412, not 416 bytes):
        // default to NORMAL on short read, still success.
        if (Db.fileReadInt32(stream, v) == -1) {
            data.damageType = ProtoTypes.DamageType.DAMAGE_TYPE_NORMAL;
        } else {
            data.damageType = ProtoTypes.ofDamageType(v[0]);
            if (data.damageType == null) {
                data.damageType = ProtoTypes.DamageType.DAMAGE_TYPE_NORMAL;
            }
        }
        return 0;
    }

    // 0x4A0AEC proto_read_item_data.
    static int protoItemDataRead(ProtoTypes.ItemProtoData data, ProtoTypes.ItemType type,
            XFile.Handle stream) {
        int[] v = new int[1];
        if (type == null) {
            return -1;
        }
        switch (type) {
        case ITEM_TYPE_ARMOR:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.armor.armorClass = v[0];
            if (Db.fileReadInt32List(stream, data.armor.damageResistance, 0, 7) == -1) {
                return -1;
            }
            if (Db.fileReadInt32List(stream, data.armor.damageThreshold, 0, 7) == -1) {
                return -1;
            }
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.armor.perk = PerkDefs.of(v[0]);
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.armor.maleFid = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.armor.femaleFid = v[0];
            return 0;
        case ITEM_TYPE_CONTAINER:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.container.maxSize = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.container.openFlags = v[0];
            return 0;
        case ITEM_TYPE_DRUG:
            for (int i = 0; i < 3; i++) {
                if (Db.fileReadInt32(stream, v) == -1) {
                    return -1;
                }
                data.drug.stat[i] = StatDefs.ofStat(v[0]);
            }
            if (Db.fileReadInt32List(stream, data.drug.amount, 0, 3) == -1) {
                return -1;
            }
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.drug.duration1 = v[0];
            if (Db.fileReadInt32List(stream, data.drug.amount1, 0, 3) == -1) {
                return -1;
            }
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.drug.duration2 = v[0];
            if (Db.fileReadInt32List(stream, data.drug.amount2, 0, 3) == -1) {
                return -1;
            }
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.drug.addictionChance = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.drug.withdrawalEffect = PerkDefs.of(v[0]);
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.drug.withdrawalOnset = v[0];
            return 0;
        case ITEM_TYPE_WEAPON:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.animationCode = ArtDefs.weaponAnimationOf(v[0]);
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.minDamage = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.maxDamage = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.damageType = ProtoTypes.ofDamageType(v[0]);
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.maxRange1 = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.maxRange2 = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.projectilePid = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.minStrength = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.actionPointCost1 = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.actionPointCost2 = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.criticalFailureType = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.perk = PerkDefs.of(v[0]);
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.rounds = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.caliber = ProtoTypes.ofCaliberType(v[0]);
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.ammoTypePid = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.weapon.ammoCapacity = v[0];
            if (Db.fileReadUInt8(stream, v) == -1) {
                return -1;
            }
            data.weapon.soundCode = v[0];
            return 0;
        case ITEM_TYPE_AMMO:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.ammo.caliber = ProtoTypes.ofCaliberType(v[0]);
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.ammo.quantity = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.ammo.armorClassModifier = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.ammo.damageResistanceModifier = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.ammo.damageMultiplier = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.ammo.damageDivisor = v[0];
            return 0;
        case ITEM_TYPE_MISC:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.misc.powerTypePid = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.misc.powerType = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.misc.charges = v[0];
            return 0;
        case ITEM_TYPE_KEY:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.key.keyCode = v[0];
            return 0;
        default:
            return -1;
        }
    }

    // 0x4A0ED0 proto_read_scenery_data.
    static int protoSceneryDataRead(ProtoTypes.SceneryProtoData data, ProtoTypes.SceneryType type,
            XFile.Handle stream) {
        int[] v = new int[1];
        if (type == null) {
            return -1;
        }
        switch (type) {
        case SCENERY_TYPE_DOOR:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.door.openFlags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.door.keyCode = v[0];
            return 0;
        case SCENERY_TYPE_STAIRS:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.stairs.destinationBuiltTile = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.stairs.destinationMap = WorldmapDefs.ofMap(v[0]);
            return 0;
        case SCENERY_TYPE_ELEVATOR:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.elevator.type = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.elevator.level = v[0];
            return 0;
        case SCENERY_TYPE_LADDER_UP:
        case SCENERY_TYPE_LADDER_DOWN:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.ladder.destinationBuiltTile = v[0];
            return 0;
        case SCENERY_TYPE_GENERIC:
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            data.generic.genericFlags = v[0];
            return 0;
        default:
            return -1;
        }
    }

    // 0x4A0FA0 proto_read_protoSubNode. Field order/widths match C exactly:
    // pid, messageId, fid (int32 each), then per-type tail. _db_freadInt is a
    // BE int32 alias, so every int32 uses Db.fileReadInt32 here.
    static int protoRead(Proto proto, XFile.Handle stream) {
        int[] v = new int[1];
        if (Db.fileReadInt32(stream, v) == -1) {
            return -1;
        }
        int pid = v[0];
        if (Db.fileReadInt32(stream, v) == -1) {
            return -1;
        }
        int messageId = v[0];
        if (Db.fileReadInt32(stream, v) == -1) {
            return -1;
        }
        int fid = v[0];

        proto.pid = pid;
        proto.messageId = messageId;
        proto.fid = fid;

        ProtoId id = ProtoId.ofPid(pid);
        ObjTypes.ObjectType objectType = id.objectType();
        int typeId = objectType.id();
        switch (typeId) {
        case 0: { // OBJ_TYPE_ITEM
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int lightDistance = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int lightIntensity = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int flags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int extendedFlags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int sid = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            ProtoTypes.ItemType itemType = ProtoTypes.ofItemType(v[0]);
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            ProtoTypes.MaterialType material = ProtoTypes.ofMaterialType(v[0]);
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int size = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int weight = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int cost = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int inventoryFid = v[0];
            if (Db.fileReadUInt8(stream, v) == -1) {
                return -1;
            }
            int soundId = v[0];

            proto.lightDistance = lightDistance;
            proto.lightIntensity = lightIntensity;
            proto.flags = new ProtoTypes.ProtoFlags(flags);
            proto.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            proto.sid = sid;

            proto.item.pid = pid;
            proto.item.messageId = messageId;
            proto.item.fid = fid;
            proto.item.lightDistance = lightDistance;
            proto.item.lightIntensity = lightIntensity;
            proto.item.flags = new ProtoTypes.ProtoFlags(flags);
            proto.item.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            proto.item.sid = sid;
            proto.item.type = itemType;
            proto.item.material = material;
            proto.item.size = size;
            proto.item.weight = weight;
            proto.item.cost = cost;
            proto.item.inventoryFid = inventoryFid;
            proto.item.soundId = soundId;

            if (protoItemDataRead(proto.item.data, itemType, stream) == -1) {
                return -1;
            }
            return 0;
        }
        case 1: { // OBJ_TYPE_CRITTER
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int lightDistance = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int lightIntensity = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int flags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int extendedFlags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int sid = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int headFid = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int aiPacket = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int team = v[0];

            proto.lightDistance = lightDistance;
            proto.lightIntensity = lightIntensity;
            proto.flags = new ProtoTypes.ProtoFlags(flags);
            proto.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            proto.sid = sid;

            proto.critter.pid = pid;
            proto.critter.messageId = messageId;
            proto.critter.fid = fid;
            proto.critter.lightDistance = lightDistance;
            proto.critter.lightIntensity = lightIntensity;
            proto.critter.flags = new ProtoTypes.ProtoFlags(flags);
            proto.critter.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            proto.critter.sid = sid;
            proto.critter.headFid = headFid;
            proto.critter.aiPacket = aiPacket;
            proto.critter.team = team;

            if (protoCritterDataRead(stream, proto.critter.data) == -1) {
                return -1;
            }
            return 0;
        }
        case 2: { // OBJ_TYPE_SCENERY
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int lightDistance = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int lightIntensity = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int flags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int extendedFlags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int sid = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            ProtoTypes.SceneryType sceneryType = ProtoTypes.ofSceneryType(v[0]);
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            ProtoTypes.MaterialType material = ProtoTypes.ofMaterialType(v[0]);
            if (Db.fileReadUInt8(stream, v) == -1) {
                return -1;
            }
            int soundId = v[0];

            proto.lightDistance = lightDistance;
            proto.lightIntensity = lightIntensity;
            proto.flags = new ProtoTypes.ProtoFlags(flags);
            proto.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            proto.sid = sid;

            proto.scenery.pid = pid;
            proto.scenery.messageId = messageId;
            proto.scenery.fid = fid;
            proto.scenery.lightDistance = lightDistance;
            proto.scenery.lightIntensity = lightIntensity;
            proto.scenery.flags = new ProtoTypes.ProtoFlags(flags);
            proto.scenery.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            proto.scenery.sid = sid;
            proto.scenery.type = sceneryType;
            proto.scenery.material = material;
            proto.scenery.soundId = soundId;

            if (protoSceneryDataRead(proto.scenery.data, sceneryType, stream) == -1) {
                return -1;
            }
            return 0;
        }
        case 3: { // OBJ_TYPE_WALL
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int lightDistance = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int lightIntensity = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int flags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int extendedFlags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int sid = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            ProtoTypes.MaterialType material = ProtoTypes.ofMaterialType(v[0]);

            proto.lightDistance = lightDistance;
            proto.lightIntensity = lightIntensity;
            proto.flags = new ProtoTypes.ProtoFlags(flags);
            proto.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            proto.sid = sid;

            proto.wall.pid = pid;
            proto.wall.messageId = messageId;
            proto.wall.fid = fid;
            proto.wall.lightDistance = lightDistance;
            proto.wall.lightIntensity = lightIntensity;
            proto.wall.flags = new ProtoTypes.ProtoFlags(flags);
            proto.wall.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            proto.wall.sid = sid;
            proto.wall.material = material;
            return 0;
        }
        case 4: { // OBJ_TYPE_TILE (no light fields)
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int flags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int extendedFlags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int sid = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            ProtoTypes.MaterialType material = ProtoTypes.ofMaterialType(v[0]);

            proto.flags = new ProtoTypes.ProtoFlags(flags);
            proto.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            proto.sid = sid;

            proto.tile.pid = pid;
            proto.tile.messageId = messageId;
            proto.tile.fid = fid;
            proto.tile.flags = new ProtoTypes.ProtoFlags(flags);
            proto.tile.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            proto.tile.sid = sid;
            proto.tile.material = material;
            return 0;
        }
        case 5: { // OBJ_TYPE_MISC (no sid on disk)
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int lightDistance = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int lightIntensity = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int flags = v[0];
            if (Db.fileReadInt32(stream, v) == -1) {
                return -1;
            }
            int extendedFlags = v[0];

            proto.lightDistance = lightDistance;
            proto.lightIntensity = lightIntensity;
            proto.flags = new ProtoTypes.ProtoFlags(flags);
            proto.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);

            proto.misc.pid = pid;
            proto.misc.messageId = messageId;
            proto.misc.fid = fid;
            proto.misc.lightDistance = lightDistance;
            proto.misc.lightIntensity = lightIntensity;
            proto.misc.flags = new ProtoTypes.ProtoFlags(flags);
            proto.misc.extendedFlags = new ProtoTypes.ProtoExtendedFlags(extendedFlags);
            return 0;
        }
        default:
            return -1;
        }
    }

    // 0x4A1C3C proto_load_pid.
    static int protoLoadProtoId(ProtoId protoId, Proto[] protoPtr) {
        StringBuilder name = new StringBuilder();
        if (protoListStr(protoId, name) == -1) {
            return -1;
        }
        String path = protoMakePath(protoId) + "\\" + name;

        XFile.Handle stream = Db.fileOpen(path, "rb");
        if (stream == null) {
            protoPtr[0] = null;
            return -1;
        }

        Proto[] slot = new Proto[1];
        if (findFreeSubnode(protoId.objectType().id(), slot) == -1) {
            Db.fileClose(stream);
            return -1;
        }

        if (protoRead(slot[0], stream) != 0) {
            Db.fileClose(stream);
            // C keeps the allocated (garbage) subnode in the cache and
            // leaves *protoPtr pointing at it; mirror that.
            protoPtr[0] = slot[0];
            return -1;
        }

        Db.fileClose(stream);
        protoPtr[0] = slot[0];
        return 0;
    }

    // 0x4A1D98 proto_find_free_subnode.
    static int findFreeSubnode(int type, Proto[] protoPtr) {
        if (type < 0 || type >= protoLists.length) {
            return -1;
        }
        Proto proto = new Proto();
        protoPtr[0] = proto;

        ProtoTypes.ProtoList protoList = protoLists[type];
        ProtoTypes.ProtoListExtent extent = protoList.tail;

        if (protoList.head != null) {
            if (extent.length == ProtoTypes.PROTO_LIST_EXTENT_SIZE) {
                ProtoTypes.ProtoListExtent fresh = new ProtoTypes.ProtoListExtent();
                fresh.length = 0;
                fresh.next = null;
                extent.next = fresh;
                protoList.tail = fresh;
                protoList.length++;
                extent = fresh;
            }
        } else {
            ProtoTypes.ProtoListExtent fresh = new ProtoTypes.ProtoListExtent();
            fresh.next = null;
            fresh.length = 0;
            protoList.length = 1;
            protoList.tail = fresh;
            protoList.head = fresh;
            extent = fresh;
        }

        extent.proto[extent.length] = proto;
        extent.length++;
        return 0;
    }

    // Fid fallback shared by the proto_*_init ports: frame (num - 1), or the
    // type's reserved.frm when art is missing (no art/VFS in this slice, so
    // the Reserved fid always wins — deterministic and disk-free, like the
    // harness which never runs protoInit).
    static int protoInitFid(ObjectType objectType, int protoNum) {
        int fid = FrmId.ofType(objectType, protoNum - 1).fid();
        if (!FrmId.ofFid(fid).exist()) {
            fid = FrmId.ofType(objectType, 0).fid();
        }
        return fid;
    }

    // 0x49EBFC proto_item_subdata_init.
    static int protoItemSubdataInit(Proto proto, ProtoTypes.ItemType type) {
        if (type == null) {
            return -1;
        }
        switch (type) {
        case ITEM_TYPE_ARMOR:
            proto.item.data.armor.armorClass = 0;
            for (int i = 0; i < 7; i++) {
                proto.item.data.armor.damageResistance[i] = 0;
                proto.item.data.armor.damageThreshold[i] = 0;
            }
            proto.item.data.armor.perk = PerkDefs.Perk.PERK_INVALID;
            proto.item.data.armor.maleFid = -1;
            proto.item.data.armor.femaleFid = -1;
            break;
        case ITEM_TYPE_CONTAINER:
            proto.item.data.container.openFlags = 0;
            proto.item.data.container.maxSize = 250;
            proto.item.extendedFlags = proto.item.extendedFlags
                    .or(ProtoTypes.ProtoExtendedFlags.CAN_USE);
            break;
        case ITEM_TYPE_DRUG:
            proto.item.data.drug.stat[0] = StatDefs.Stat.STAT_STRENGTH;
            proto.item.data.drug.stat[1] = StatDefs.Stat.STAT_INVALID;
            proto.item.data.drug.stat[2] = StatDefs.Stat.STAT_INVALID;
            for (int i = 0; i < 3; i++) {
                proto.item.data.drug.amount[i] = 0;
                proto.item.data.drug.amount1[i] = 0;
                proto.item.data.drug.amount2[i] = 0;
            }
            proto.item.data.drug.duration1 = 0;
            proto.item.data.drug.duration2 = 0;
            proto.item.data.drug.addictionChance = 0;
            proto.item.data.drug.withdrawalEffect = PerkDefs.of(PerkDefs.PERK_FIRST);
            proto.item.data.drug.withdrawalOnset = 0;
            proto.item.extendedFlags = proto.item.extendedFlags
                    .or(ProtoTypes.ProtoExtendedFlags.CAN_USE_ON);
            break;
        case ITEM_TYPE_WEAPON:
            proto.item.data.weapon.animationCode = ArtDefs.WeaponAnimation.None;
            proto.item.data.weapon.minDamage = 0;
            proto.item.data.weapon.maxDamage = 0;
            proto.item.data.weapon.damageType = ProtoTypes.DamageType.DAMAGE_TYPE_NORMAL;
            proto.item.data.weapon.maxRange1 = 0;
            proto.item.data.weapon.maxRange2 = 0;
            proto.item.data.weapon.projectilePid = ProtoId.EMPTY_PID;
            proto.item.data.weapon.minStrength = 0;
            proto.item.data.weapon.actionPointCost1 = 0;
            proto.item.data.weapon.actionPointCost2 = 0;
            proto.item.data.weapon.criticalFailureType = 0;
            proto.item.data.weapon.perk = PerkDefs.Perk.PERK_INVALID;
            proto.item.data.weapon.rounds = 0;
            proto.item.data.weapon.caliber = ProtoTypes.CaliberType.CALIBER_NONE;
            proto.item.data.weapon.ammoTypePid = ProtoId.EMPTY_PID;
            proto.item.data.weapon.ammoCapacity = 0;
            proto.item.data.weapon.soundCode = 0;
            break;
        case ITEM_TYPE_AMMO:
            proto.item.data.ammo.caliber = ProtoTypes.CaliberType.CALIBER_NONE;
            proto.item.data.ammo.quantity = 20;
            proto.item.data.ammo.armorClassModifier = 0;
            proto.item.data.ammo.damageResistanceModifier = 0;
            proto.item.data.ammo.damageMultiplier = 1;
            proto.item.data.ammo.damageDivisor = 1;
            break;
        case ITEM_TYPE_MISC:
            proto.item.data.misc.powerTypePid = ProtoId.EMPTY_PID;
            proto.item.data.misc.powerType = 20;
            // NOTE: C leaves charges (malloc garbage) unset here; Java keeps 0.
            break;
        case ITEM_TYPE_KEY:
            proto.item.data.key.keyCode = -1;
            proto.item.extendedFlags = proto.item.extendedFlags
                    .or(ProtoTypes.ProtoExtendedFlags.CAN_USE_ON);
            break;
        default:
            break;
        }
        return 0;
    }

    // 0x49EB2C proto_item_init. C writes the union variant only; the header
    // overlays it, so Java sets both (same duplication as protoRead).
    static int protoItemInit(Proto proto, ProtoId protoId) {
        int protoNum = protoId.protoId();
        int fid = protoInitFid(ObjectType.ITEM, protoNum);

        proto.pid = ProtoId.EMPTY_PID;
        proto.messageId = 100 * protoNum;
        proto.fid = fid;
        proto.lightDistance = 0;
        proto.lightIntensity = 0;
        ProtoTypes.ProtoFlags flags = ProtoTypes.ProtoFlags.FLAT
                .or(ProtoTypes.ProtoFlags.LIGHT_THRU)
                .or(ProtoTypes.ProtoFlags.SHOOT_THRU);
        proto.flags = flags;
        ProtoTypes.ProtoExtendedFlags extendedFlags = ProtoTypes.ProtoExtendedFlags.LOOK
                .or(ProtoTypes.ProtoExtendedFlags.CAN_PICK_UP);
        proto.extendedFlags = extendedFlags;
        proto.sid = -1;

        proto.item.pid = ProtoId.EMPTY_PID;
        proto.item.messageId = 100 * protoNum;
        proto.item.fid = fid;
        proto.item.lightDistance = 0;
        proto.item.lightIntensity = 0;
        proto.item.flags = flags;
        proto.item.extendedFlags = extendedFlags;
        proto.item.sid = -1;
        proto.item.type = ProtoTypes.ItemType.ITEM_TYPE_MISC;
        protoItemSubdataInit(proto, proto.item.type);
        proto.item.material = ProtoTypes.MaterialType.MATERIAL_TYPE_METAL;
        proto.item.size = 1;
        proto.item.weight = 10;
        proto.item.cost = 0;
        proto.item.inventoryFid = -1;
        proto.item.soundId = '0';
        return 0;
    }

    // 0x49EDB4 proto_critter_init — NOTE-deferred.
    static int protoCritterInit(Proto proto, ProtoId protoId) {
        // Full init needs the stat-description defaults table (src/stat.cc
        // gStatDescriptions), the skill reset (src/skill.cc) and the
        // CritterFrmId art lookup; none are ported in this slice. Mirror only
        // the C _protos_been_initialized gate. protoNew ignores this rc and
        // still assigns the pid, like C.
        if (!ObjectSystem.protosInitialized) {
            return -1;
        }
        // Table-driven field init lands with the stat/skill/art ports.
        return -1;
    }

    // 0x49FC74 proto_scenery_subdata_init.
    static int protoScenerySubdataInit(Proto proto, ProtoTypes.SceneryType type) {
        if (type == null) {
            return -1;
        }
        switch (type) {
        case SCENERY_TYPE_DOOR:
            proto.scenery.data.door.openFlags = 0;
            proto.scenery.extendedFlags = proto.scenery.extendedFlags
                    .or(ProtoTypes.ProtoExtendedFlags.CAN_USE);
            break;
        case SCENERY_TYPE_STAIRS:
            proto.scenery.data.stairs.destinationBuiltTile = -1;
            proto.scenery.data.stairs.destinationMap = WorldmapDefs.Map.MAP_INVALID;
            proto.scenery.extendedFlags = proto.scenery.extendedFlags
                    .or(ProtoTypes.ProtoExtendedFlags.CAN_USE);
            break;
        case SCENERY_TYPE_ELEVATOR:
            proto.scenery.data.elevator.type = -1;
            proto.scenery.data.elevator.level = -1;
            proto.scenery.extendedFlags = proto.scenery.extendedFlags
                    .or(ProtoTypes.ProtoExtendedFlags.CAN_USE);
            break;
        case SCENERY_TYPE_LADDER_UP:
        case SCENERY_TYPE_LADDER_DOWN:
            proto.scenery.data.ladder.destinationBuiltTile = -1;
            proto.scenery.extendedFlags = proto.scenery.extendedFlags
                    .or(ProtoTypes.ProtoExtendedFlags.CAN_USE);
            break;
        default:
            break;
        }
        return 0;
    }

    // 0x49FBBC proto_scenery_init (same header/variant duplication as above).
    static int protoSceneryInit(Proto proto, ProtoId protoId) {
        int protoNum = protoId.protoId();
        int fid = protoInitFid(ObjectType.SCENERY, protoNum);

        proto.pid = ProtoId.EMPTY_PID;
        proto.messageId = 100 * protoNum;
        proto.fid = fid;
        proto.lightDistance = 0;
        proto.lightIntensity = 0;
        proto.flags = ProtoTypes.ProtoFlags.NONE;
        ProtoTypes.ProtoExtendedFlags extendedFlags = ProtoTypes.ProtoExtendedFlags.LOOK;
        proto.extendedFlags = extendedFlags;
        proto.sid = -1;

        proto.scenery.pid = ProtoId.EMPTY_PID;
        proto.scenery.messageId = 100 * protoNum;
        proto.scenery.fid = fid;
        proto.scenery.lightDistance = 0;
        proto.scenery.lightIntensity = 0;
        proto.scenery.flags = ProtoTypes.ProtoFlags.NONE;
        proto.scenery.extendedFlags = extendedFlags;
        proto.scenery.sid = -1;
        proto.scenery.type = ProtoTypes.SceneryType.SCENERY_TYPE_GENERIC;
        protoScenerySubdataInit(proto, proto.scenery.type);
        proto.scenery.material = ProtoTypes.MaterialType.MATERIAL_TYPE_INVALID;
        proto.scenery.soundId = '0';
        return 0;
    }

    // 0x49FCFC proto_wall_init.
    static int protoWallInit(Proto proto, ProtoId protoId) {
        int protoNum = protoId.protoId();
        int fid = protoInitFid(ObjectType.WALL, protoNum);

        proto.pid = ProtoId.EMPTY_PID;
        proto.messageId = 100 * protoNum;
        proto.fid = fid;
        proto.lightDistance = 0;
        proto.lightIntensity = 0;
        proto.flags = ProtoTypes.ProtoFlags.NONE;
        proto.extendedFlags = ProtoTypes.ProtoExtendedFlags.LOOK;
        proto.sid = -1;

        proto.wall.pid = ProtoId.EMPTY_PID;
        proto.wall.messageId = 100 * protoNum;
        proto.wall.fid = fid;
        proto.wall.lightDistance = 0;
        proto.wall.lightIntensity = 0;
        proto.wall.flags = ProtoTypes.ProtoFlags.NONE;
        proto.wall.extendedFlags = ProtoTypes.ProtoExtendedFlags.LOOK;
        proto.wall.sid = -1;
        proto.wall.material = ProtoTypes.MaterialType.MATERIAL_TYPE_METAL;
        return 0;
    }

    // 0x49FD84 proto_tile_init (no light fields, like C).
    static int protoTileInit(Proto proto, ProtoId protoId) {
        int protoNum = protoId.protoId();
        int fid = protoInitFid(ObjectType.TILE, protoNum);

        proto.pid = ProtoId.EMPTY_PID;
        proto.messageId = 100 * protoNum;
        proto.fid = fid;
        proto.flags = ProtoTypes.ProtoFlags.NONE;
        proto.extendedFlags = ProtoTypes.ProtoExtendedFlags.LOOK;
        proto.sid = -1;

        proto.tile.pid = ProtoId.EMPTY_PID;
        proto.tile.messageId = 100 * protoNum;
        proto.tile.fid = fid;
        proto.tile.flags = ProtoTypes.ProtoFlags.NONE;
        proto.tile.extendedFlags = ProtoTypes.ProtoExtendedFlags.LOOK;
        proto.tile.sid = -1;
        proto.tile.material = ProtoTypes.MaterialType.MATERIAL_TYPE_METAL;
        return 0;
    }

    // 0x49FDFC proto_misc_init (no sid, like C).
    static int protoMiscInit(Proto proto, ProtoId protoId) {
        int protoNum = protoId.protoId();
        int fid = protoInitFid(ObjectType.MISC, protoNum);

        proto.pid = ProtoId.EMPTY_PID;
        proto.messageId = 100 * protoNum;
        proto.fid = fid;
        proto.lightDistance = 0;
        proto.lightIntensity = 0;
        proto.flags = ProtoTypes.ProtoFlags.NONE;
        proto.extendedFlags = ProtoTypes.ProtoExtendedFlags.NONE;

        proto.misc.pid = ProtoId.EMPTY_PID;
        proto.misc.messageId = 100 * protoNum;
        proto.misc.fid = fid;
        proto.misc.lightDistance = 0;
        proto.misc.lightIntensity = 0;
        proto.misc.flags = ProtoTypes.ProtoFlags.NONE;
        proto.misc.extendedFlags = ProtoTypes.ProtoExtendedFlags.NONE;
        return 0;
    }

    // 0x4A21DC proto_new_id: take the list counter, then bump it. C switches
    // on the type only to pick the phantom ItemProtoTypeId/... tag; pid
    // packing is identical, so one build path covers all six proto types.
    static ProtoId protoNewId(int type) {
        int result = protoLists[type].maxEntriesNum;
        protoLists[type].maxEntriesNum = result + 1;
        if (type >= ObjTypes.OBJ_TYPE_FIRST && type < ObjTypes.OBJ_TYPE_PROTO_COUNT) {
            return ProtoId.ofPid(ProtoId.buildPid(ObjTypes.ofObjectType(type), result));
        }
        return ProtoId.empty();
    }

    // 0x4A2214 proto_max_id.
    public static int protoMaxId(int type) {
        if (type < 0 || type >= protoLists.length) {
            return -1;
        }
        return protoLists[type].maxEntriesNum;
    }

    // 0x4A1E90 proto_new: allocate the subnode first, then take the id, then
    // init. The init rc is ignored and the pid is stored regardless, like C.
    // C sets only the variant pid (the union overlays the header); Java sets
    // both. An invalid type fails the subnode gate with no side effects.
    public static int protoNew(ProtoId[] protoIdPtr, int type) {
        Proto[] slot = new Proto[1];
        if (findFreeSubnode(type, slot) == -1) {
            return -1;
        }
        Proto proto = slot[0];

        ProtoId protoId = protoNewId(type);
        protoIdPtr[0] = protoId;

        switch (type) {
        case 0: // OBJ_TYPE_ITEM
            protoItemInit(proto, protoId);
            proto.pid = protoId.pid();
            proto.item.pid = protoId.pid();
            break;
        case 1: // OBJ_TYPE_CRITTER
            protoCritterInit(proto, protoId);
            proto.pid = protoId.pid();
            proto.critter.pid = protoId.pid();
            break;
        case 2: // OBJ_TYPE_SCENERY
            protoSceneryInit(proto, protoId);
            proto.pid = protoId.pid();
            proto.scenery.pid = protoId.pid();
            break;
        case 3: // OBJ_TYPE_WALL
            protoWallInit(proto, protoId);
            proto.pid = protoId.pid();
            proto.wall.pid = protoId.pid();
            break;
        case 4: // OBJ_TYPE_TILE
            protoTileInit(proto, protoId);
            proto.pid = protoId.pid();
            proto.tile.pid = protoId.pid();
            break;
        case 5: // OBJ_TYPE_MISC
            protoMiscInit(proto, protoId);
            proto.pid = protoId.pid();
            proto.misc.pid = protoId.pid();
            break;
        default:
            return -1;
        }
        return 0;
    }

    // 0x49FE74 proto_copy_proto: the types must match and both sides
    // must load; the copy is a memcpy sized by _proto_sizes[type] (the
    // header plus the active variant), then the dst pid is stamped over
    // the copied header (the union overlay carries the variant pid with
    // it, like C).
    public static int protoCopyProto(ProtoId srcProtoId, ProtoId dstProtoId) {
        if (srcProtoId.objectType() != dstProtoId.objectType()) {
            return -1;
        }

        Proto[] srcSlot = new Proto[1];
        if (protoGetProto(srcProtoId, srcSlot) == -1) {
            return -1;
        }

        Proto[] dstSlot = new Proto[1];
        if (protoGetProto(dstProtoId, dstSlot) == -1) {
            return -1;
        }

        copyProto(dstSlot[0], srcSlot[0], srcProtoId.objectType().id());
        dstSlot[0].pid = dstProtoId.pid();
        setProtoVariantPid(dstSlot[0], srcProtoId.objectType().id(), dstProtoId.pid());

        return 0;
    }

    // 0x49FEDC proto_is_subtype: -1 matches everything; items compare
    // item.type, scenery compares scenery.type, anything else is false.
    public static boolean protoIsSubtype(Proto proto, int subtype) {
        if (subtype == -1) {
            return true;
        }

        switch (ProtoId.ofProto(proto).objectType()) {
        case OBJ_TYPE_ITEM:
            return proto.item.type != null && proto.item.type.id() == subtype;
        case OBJ_TYPE_SCENERY:
            return proto.scenery.type != null && proto.scenery.type.id() == subtype;
        default:
            return false;
        }
    }

    // memcpy scope of proto_copy_proto (_proto_sizes[type]): the header
    // plus the whole active variant. Java keeps the C union variants as
    // separate fields, so every sub-field of the active variant is
    // copied (including the overlaid header copies inside the variant);
    // the pid stamp lands on top afterwards, like C.
    private static void copyProto(Proto dst, Proto src, int type) {
        dst.pid = src.pid;
        dst.messageId = src.messageId;
        dst.fid = src.fid;
        dst.lightDistance = src.lightDistance;
        dst.lightIntensity = src.lightIntensity;
        dst.flags = src.flags;
        dst.extendedFlags = src.extendedFlags;
        dst.sid = src.sid;

        switch (type) {
        case 0: // OBJ_TYPE_ITEM
            copyItemProto(dst.item, src.item);
            break;
        case 1: // OBJ_TYPE_CRITTER
            copyCritterProto(dst.critter, src.critter);
            break;
        case 2: // OBJ_TYPE_SCENERY
            copySceneryProto(dst.scenery, src.scenery);
            break;
        case 3: // OBJ_TYPE_WALL
            copyWallProto(dst.wall, src.wall);
            break;
        case 4: // OBJ_TYPE_TILE
            copyTileProto(dst.tile, src.tile);
            break;
        case 5: // OBJ_TYPE_MISC
            copyMiscProto(dst.misc, src.misc);
            break;
        default:
            break;
        }
    }

    // Pid half of the proto_copy_proto stamp: the C union overlay means
    // the header pid write also lands on the variant pid; Java writes
    // both explicitly.
    private static void setProtoVariantPid(Proto dst, int type, int pid) {
        switch (type) {
        case 0: // OBJ_TYPE_ITEM
            dst.item.pid = pid;
            break;
        case 1: // OBJ_TYPE_CRITTER
            dst.critter.pid = pid;
            break;
        case 2: // OBJ_TYPE_SCENERY
            dst.scenery.pid = pid;
            break;
        case 3: // OBJ_TYPE_WALL
            dst.wall.pid = pid;
            break;
        case 4: // OBJ_TYPE_TILE
            dst.tile.pid = pid;
            break;
        case 5: // OBJ_TYPE_MISC
            dst.misc.pid = pid;
            break;
        default:
            break;
        }
    }

    // ItemProto half of the memcpy: all scalar fields plus every data
    // variant (the C union overlays them on the same bytes, so copying
    // all is exactly the byte copy; the caller only ever reads the one
    // matching type).
    private static void copyItemProto(ProtoTypes.ItemProto dst, ProtoTypes.ItemProto src) {
        dst.pid = src.pid;
        dst.messageId = src.messageId;
        dst.fid = src.fid;
        dst.lightDistance = src.lightDistance;
        dst.lightIntensity = src.lightIntensity;
        dst.flags = src.flags;
        dst.extendedFlags = src.extendedFlags;
        dst.sid = src.sid;
        dst.type = src.type;
        dst.material = src.material;
        dst.size = src.size;
        dst.weight = src.weight;
        dst.cost = src.cost;
        dst.inventoryFid = src.inventoryFid;
        dst.soundId = src.soundId;

        System.arraycopy(src.data.unknown, 0, dst.data.unknown, 0, src.data.unknown.length);

        dst.data.armor.armorClass = src.data.armor.armorClass;
        System.arraycopy(src.data.armor.damageResistance, 0, dst.data.armor.damageResistance, 0,
                src.data.armor.damageResistance.length);
        System.arraycopy(src.data.armor.damageThreshold, 0, dst.data.armor.damageThreshold, 0,
                src.data.armor.damageThreshold.length);
        dst.data.armor.perk = src.data.armor.perk;
        dst.data.armor.maleFid = src.data.armor.maleFid;
        dst.data.armor.femaleFid = src.data.armor.femaleFid;

        dst.data.container.maxSize = src.data.container.maxSize;
        dst.data.container.openFlags = src.data.container.openFlags;

        System.arraycopy(src.data.drug.stat, 0, dst.data.drug.stat, 0, src.data.drug.stat.length);
        System.arraycopy(src.data.drug.amount, 0, dst.data.drug.amount, 0,
                src.data.drug.amount.length);
        dst.data.drug.duration1 = src.data.drug.duration1;
        System.arraycopy(src.data.drug.amount1, 0, dst.data.drug.amount1, 0,
                src.data.drug.amount1.length);
        dst.data.drug.duration2 = src.data.drug.duration2;
        System.arraycopy(src.data.drug.amount2, 0, dst.data.drug.amount2, 0,
                src.data.drug.amount2.length);
        dst.data.drug.addictionChance = src.data.drug.addictionChance;
        dst.data.drug.withdrawalEffect = src.data.drug.withdrawalEffect;
        dst.data.drug.withdrawalOnset = src.data.drug.withdrawalOnset;

        dst.data.weapon.animationCode = src.data.weapon.animationCode;
        dst.data.weapon.minDamage = src.data.weapon.minDamage;
        dst.data.weapon.maxDamage = src.data.weapon.maxDamage;
        dst.data.weapon.damageType = src.data.weapon.damageType;
        dst.data.weapon.maxRange1 = src.data.weapon.maxRange1;
        dst.data.weapon.maxRange2 = src.data.weapon.maxRange2;
        dst.data.weapon.projectilePid = src.data.weapon.projectilePid;
        dst.data.weapon.minStrength = src.data.weapon.minStrength;
        dst.data.weapon.actionPointCost1 = src.data.weapon.actionPointCost1;
        dst.data.weapon.actionPointCost2 = src.data.weapon.actionPointCost2;
        dst.data.weapon.criticalFailureType = src.data.weapon.criticalFailureType;
        dst.data.weapon.perk = src.data.weapon.perk;
        dst.data.weapon.rounds = src.data.weapon.rounds;
        dst.data.weapon.caliber = src.data.weapon.caliber;
        dst.data.weapon.ammoTypePid = src.data.weapon.ammoTypePid;
        dst.data.weapon.ammoCapacity = src.data.weapon.ammoCapacity;
        dst.data.weapon.soundCode = src.data.weapon.soundCode;

        dst.data.ammo.caliber = src.data.ammo.caliber;
        dst.data.ammo.quantity = src.data.ammo.quantity;
        dst.data.ammo.armorClassModifier = src.data.ammo.armorClassModifier;
        dst.data.ammo.damageResistanceModifier = src.data.ammo.damageResistanceModifier;
        dst.data.ammo.damageMultiplier = src.data.ammo.damageMultiplier;
        dst.data.ammo.damageDivisor = src.data.ammo.damageDivisor;

        dst.data.misc.powerTypePid = src.data.misc.powerTypePid;
        dst.data.misc.powerType = src.data.misc.powerType;
        dst.data.misc.charges = src.data.misc.charges;

        dst.data.key.keyCode = src.data.key.keyCode;
    }

    // CritterProto half of the memcpy.
    private static void copyCritterProto(ProtoTypes.CritterProto dst, ProtoTypes.CritterProto src) {
        dst.pid = src.pid;
        dst.messageId = src.messageId;
        dst.fid = src.fid;
        dst.lightDistance = src.lightDistance;
        dst.lightIntensity = src.lightIntensity;
        dst.flags = src.flags;
        dst.extendedFlags = src.extendedFlags;
        dst.sid = src.sid;
        dst.headFid = src.headFid;
        dst.aiPacket = src.aiPacket;
        dst.team = src.team;

        dst.data.flags = src.data.flags;
        System.arraycopy(src.data.baseStats, 0, dst.data.baseStats, 0,
                src.data.baseStats.length);
        System.arraycopy(src.data.bonusStats, 0, dst.data.bonusStats, 0,
                src.data.bonusStats.length);
        System.arraycopy(src.data.skills, 0, dst.data.skills, 0, src.data.skills.length);
        dst.data.bodyType = src.data.bodyType;
        dst.data.experience = src.data.experience;
        dst.data.killType = src.data.killType;
        dst.data.damageType = src.data.damageType;
    }

    // SceneryProto half of the memcpy (all data variants: same-bytes
    // overlay, like the item union above).
    private static void copySceneryProto(ProtoTypes.SceneryProto dst, ProtoTypes.SceneryProto src) {
        dst.pid = src.pid;
        dst.messageId = src.messageId;
        dst.fid = src.fid;
        dst.lightDistance = src.lightDistance;
        dst.lightIntensity = src.lightIntensity;
        dst.flags = src.flags;
        dst.extendedFlags = src.extendedFlags;
        dst.sid = src.sid;
        dst.type = src.type;
        dst.material = src.material;
        dst.field30 = src.field30;
        dst.soundId = src.soundId;

        dst.data.door.openFlags = src.data.door.openFlags;
        dst.data.door.keyCode = src.data.door.keyCode;
        dst.data.stairs.destinationBuiltTile = src.data.stairs.destinationBuiltTile;
        dst.data.stairs.destinationMap = src.data.stairs.destinationMap;
        dst.data.elevator.type = src.data.elevator.type;
        dst.data.elevator.level = src.data.elevator.level;
        dst.data.ladder.destinationBuiltTile = src.data.ladder.destinationBuiltTile;
        dst.data.generic.genericFlags = src.data.generic.genericFlags;
    }

    // WallProto half of the memcpy.
    private static void copyWallProto(ProtoTypes.WallProto dst, ProtoTypes.WallProto src) {
        dst.pid = src.pid;
        dst.messageId = src.messageId;
        dst.fid = src.fid;
        dst.lightDistance = src.lightDistance;
        dst.lightIntensity = src.lightIntensity;
        dst.flags = src.flags;
        dst.extendedFlags = src.extendedFlags;
        dst.sid = src.sid;
        dst.material = src.material;
    }

    // TileProto half of the memcpy (no light fields, like C).
    private static void copyTileProto(ProtoTypes.TileProto dst, ProtoTypes.TileProto src) {
        dst.pid = src.pid;
        dst.messageId = src.messageId;
        dst.fid = src.fid;
        dst.flags = src.flags;
        dst.extendedFlags = src.extendedFlags;
        dst.sid = src.sid;
        dst.material = src.material;
    }

    // MiscProto half of the memcpy (no sid, like C).
    private static void copyMiscProto(ProtoTypes.MiscProto dst, ProtoTypes.MiscProto src) {
        dst.pid = src.pid;
        dst.messageId = src.messageId;
        dst.fid = src.fid;
        dst.lightDistance = src.lightDistance;
        dst.lightIntensity = src.lightIntensity;
        dst.flags = src.flags;
        dst.extendedFlags = src.extendedFlags;
        dst.sid = src.sid;
    }

    // 0x4A2040 proto_remove_some_list: evict the head extent.
    static void removeSomeList(int type) {
        if (type < 0 || type >= protoLists.length) {
            return;
        }
        ProtoTypes.ProtoList protoList = protoLists[type];
        ProtoTypes.ProtoListExtent extent = protoList.head;
        if (extent != null) {
            protoList.length--;
            protoList.head = extent.next;
            if (protoList.head == null) {
                protoList.tail = null;
            }
        }
    }

    // 0x4A2094 proto_remove_list.
    static void removeList(int type) {
        if (type < 0 || type >= protoLists.length) {
            return;
        }
        ProtoTypes.ProtoList protoList = protoLists[type];
        protoList.head = null;
        protoList.tail = null;
        protoList.length = 0;
    }

    // 0x4A2108 proto_ptr.
    public static int protoGetProto(ProtoId protoId, Proto[] protoPtr) {
        protoPtr[0] = null;

        if (!protoId.valid()) {
            return -1;
        }

        if (protoId.matchesCritterType(CritterProtoTypeId.Dude)) {
            protoPtr[0] = dudeProto;
            return 0;
        }

        int type = protoId.objectType().id();
        ProtoTypes.ProtoList protoList = protoLists[type];
        ProtoTypes.ProtoListExtent extent = protoList.head;
        while (extent != null) {
            for (int index = 0; index < extent.length; index++) {
                Proto proto = extent.proto[index];
                if (proto != null && ProtoId.ofPid(proto.pid).equals(protoId)) {
                    protoPtr[0] = proto;
                    return 0;
                }
            }
            extent = extent.next;
        }

        if (protoList.head != null && protoList.tail != null) {
            if (ProtoTypes.PROTO_LIST_EXTENT_SIZE * protoList.length
                    - (ProtoTypes.PROTO_LIST_EXTENT_SIZE - protoList.tail.length)
                    > ProtoTypes.PROTO_LIST_MAX_ENTRIES) {
                removeSomeList(type);
            }
        }

        return protoLoadProtoId(protoId, protoPtr);
    }

    private static String protoGetName(ProtoId protoId) {
        // Message stack comes with the game phase; names stay unavailable here.
        return null;
    }

    private static String protoGetDescription(ProtoId protoId) {
        // Message stack comes with the game phase; names stay unavailable here.
        return null;
    }

    // 0x49FFD8 proto_data_member. Int members set integerValue and fall to
    // `return PROTO_DATA_MEMBER_TYPE_INT`; NAME/DESCRIPTION return STRING
    // (MISC DESCRIPTION FIXME returns INT). Unknown member/type leaves the
    // value untouched (stale) and still returns INT (member-bad=1 quirk).
    public static int protoGetDataMember(ProtoId protoId, int member,
            ProtoDefs.ProtoDataMemberValue value) {
        Proto[] slot = new Proto[1];
        if (protoGetProto(protoId, slot) == -1) {
            return -1;
        }
        Proto proto = slot[0];

        switch (protoId.objectType().id()) {
        case 0: // OBJ_TYPE_ITEM
            switch (member) {
            case 0: // ITEM_DATA_MEMBER_PID
                value.integerValue = proto.pid;
                break;
            case 1: // ITEM_DATA_MEMBER_NAME
                value.stringValue = protoGetName(ProtoId.ofPid(proto.scenery.pid));
                return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id();
            case 2: // ITEM_DATA_MEMBER_DESCRIPTION
                value.stringValue = protoGetDescription(ProtoId.ofPid(proto.pid));
                return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id();
            case 3: // ITEM_DATA_MEMBER_FID
                value.integerValue = proto.fid;
                break;
            case 4: // ITEM_DATA_MEMBER_LIGHT_DISTANCE
                value.integerValue = proto.item.lightDistance;
                break;
            case 5: // ITEM_DATA_MEMBER_LIGHT_INTENSITY
                value.integerValue = proto.item.lightIntensity;
                break;
            case 6: // ITEM_DATA_MEMBER_FLAGS
                value.integerValue = proto.item.flags.bits();
                break;
            case 7: // ITEM_DATA_MEMBER_EXTENDED_FLAGS
                value.integerValue = proto.item.extendedFlags.bits();
                break;
            case 8: // ITEM_DATA_MEMBER_SID
                value.integerValue = proto.item.sid;
                break;
            case 9: // ITEM_DATA_MEMBER_TYPE
                value.integerValue = proto.item.type != null ? proto.item.type.id() : 0;
                break;
            case 11: // ITEM_DATA_MEMBER_MATERIAL
                value.integerValue = proto.item.material != null ? proto.item.material.id() : 0;
                break;
            case 12: // ITEM_DATA_MEMBER_SIZE
                value.integerValue = proto.item.size;
                break;
            case 13: // ITEM_DATA_MEMBER_WEIGHT
                value.integerValue = proto.item.weight;
                break;
            case 14: // ITEM_DATA_MEMBER_COST
                value.integerValue = proto.item.cost;
                break;
            case 15: // ITEM_DATA_MEMBER_INVENTORY_FID
                value.integerValue = proto.item.inventoryFid;
                break;
            case 555: // ITEM_DATA_MEMBER_WEAPON_RANGE
                if (proto.item.type == ProtoTypes.ItemType.ITEM_TYPE_WEAPON) {
                    value.integerValue = proto.item.data.weapon.maxRange1;
                }
                break;
            default:
                break;
            }
            break;
        case 1: // OBJ_TYPE_CRITTER
            switch (member) {
            case 0: // CRITTER_DATA_MEMBER_PID
                value.integerValue = proto.critter.pid;
                break;
            case 1: // CRITTER_DATA_MEMBER_NAME
                value.stringValue = protoGetName(ProtoId.ofPid(proto.critter.pid));
                return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id();
            case 2: // CRITTER_DATA_MEMBER_DESCRIPTION
                value.stringValue = protoGetDescription(ProtoId.ofPid(proto.critter.pid));
                return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id();
            case 3: // CRITTER_DATA_MEMBER_FID
                value.integerValue = proto.critter.fid;
                break;
            case 4: // CRITTER_DATA_MEMBER_LIGHT_DISTANCE
                value.integerValue = proto.critter.lightDistance;
                break;
            case 5: // CRITTER_DATA_MEMBER_LIGHT_INTENSITY
                value.integerValue = proto.critter.lightIntensity;
                break;
            case 6: // CRITTER_DATA_MEMBER_FLAGS
                value.integerValue = proto.critter.flags.bits();
                break;
            case 7: // CRITTER_DATA_MEMBER_EXTENDED_FLAGS
                value.integerValue = proto.critter.extendedFlags.bits();
                break;
            case 8: // CRITTER_DATA_MEMBER_SID
                value.integerValue = proto.critter.sid;
                break;
            case 10: // CRITTER_DATA_MEMBER_HEAD_FID
                value.integerValue = proto.critter.headFid;
                break;
            case 11: // CRITTER_DATA_MEMBER_BODY_TYPE
                value.integerValue = proto.critter.data.bodyType != null
                        ? proto.critter.data.bodyType.id()
                        : 0;
                break;
            default:
                break;
            }
            break;
        case 2: // OBJ_TYPE_SCENERY
            switch (member) {
            case 0: // SCENERY_DATA_MEMBER_PID
                value.integerValue = proto.scenery.pid;
                break;
            case 1: // SCENERY_DATA_MEMBER_NAME
                value.stringValue = protoGetName(ProtoId.ofPid(proto.scenery.pid));
                return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id();
            case 2: // SCENERY_DATA_MEMBER_DESCRIPTION
                value.stringValue = protoGetDescription(ProtoId.ofPid(proto.scenery.pid));
                return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id();
            case 3: // SCENERY_DATA_MEMBER_FID
                value.integerValue = proto.scenery.fid;
                break;
            case 4: // SCENERY_DATA_MEMBER_LIGHT_DISTANCE
                value.integerValue = proto.scenery.lightDistance;
                break;
            case 5: // SCENERY_DATA_MEMBER_LIGHT_INTENSITY
                value.integerValue = proto.scenery.lightIntensity;
                break;
            case 6: // SCENERY_DATA_MEMBER_FLAGS
                value.integerValue = proto.scenery.flags.bits();
                break;
            case 7: // SCENERY_DATA_MEMBER_EXTENDED_FLAGS
                value.integerValue = proto.scenery.extendedFlags.bits();
                break;
            case 8: // SCENERY_DATA_MEMBER_SID
                value.integerValue = proto.scenery.sid;
                break;
            case 9: // SCENERY_DATA_MEMBER_TYPE
                value.integerValue = proto.scenery.type != null ? proto.scenery.type.id() : 0;
                break;
            case 11: // SCENERY_DATA_MEMBER_MATERIAL
                value.integerValue = proto.scenery.material != null ? proto.scenery.material.id() : 0;
                break;
            default:
                break;
            }
            break;
        case 3: // OBJ_TYPE_WALL
            switch (member) {
            case 0: // WALL_DATA_MEMBER_PID
                value.integerValue = proto.wall.pid;
                break;
            case 1: // WALL_DATA_MEMBER_NAME
                value.stringValue = protoGetName(ProtoId.ofPid(proto.wall.pid));
                return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id();
            case 2: // WALL_DATA_MEMBER_DESCRIPTION
                value.stringValue = protoGetDescription(ProtoId.ofPid(proto.wall.pid));
                return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id();
            case 3: // WALL_DATA_MEMBER_FID
                value.integerValue = proto.wall.fid;
                break;
            case 4: // WALL_DATA_MEMBER_LIGHT_DISTANCE
                value.integerValue = proto.wall.lightDistance;
                break;
            case 5: // WALL_DATA_MEMBER_LIGHT_INTENSITY
                value.integerValue = proto.wall.lightIntensity;
                break;
            case 6: // WALL_DATA_MEMBER_FLAGS
                value.integerValue = proto.wall.flags.bits();
                break;
            case 7: // WALL_DATA_MEMBER_EXTENDED_FLAGS
                value.integerValue = proto.wall.extendedFlags.bits();
                break;
            case 8: // WALL_DATA_MEMBER_SID
                value.integerValue = proto.wall.sid;
                break;
            case 9: // WALL_DATA_MEMBER_MATERIAL
                value.integerValue = proto.wall.material != null ? proto.wall.material.id() : 0;
                break;
            default:
                break;
            }
            break;
        case 4: // OBJ_TYPE_TILE
            break;
        case 5: // OBJ_TYPE_MISC
            switch (member) {
            case 0: // MISC_DATA_MEMBER_PID
                value.integerValue = proto.misc.pid;
                break;
            case 1: // MISC_DATA_MEMBER_NAME
                value.stringValue = protoGetName(ProtoId.ofPid(proto.misc.pid));
                return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id();
            case 2: // MISC_DATA_MEMBER_DESCRIPTION
                value.stringValue = protoGetDescription(ProtoId.ofPid(proto.misc.pid));
                // FIXME: erroneously reports INT, should be STRING.
                return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_INT.id();
            case 3: // MISC_DATA_MEMBER_FID
                value.integerValue = proto.misc.fid;
                return 1;
            case 4: // MISC_DATA_MEMBER_LIGHT_DISTANCE
                value.integerValue = proto.misc.lightDistance;
                return 1;
            case 5: // MISC_DATA_MEMBER_LIGHT_INTENSITY
                value.integerValue = proto.misc.lightIntensity;
                break;
            case 6: // MISC_DATA_MEMBER_FLAGS
                value.integerValue = proto.misc.flags.bits();
                break;
            case 7: // MISC_DATA_MEMBER_EXTENDED_FLAGS
                value.integerValue = proto.misc.extendedFlags.bits();
                break;
            default:
                break;
            }
            break;
        default:
            break;
        }

        return ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_INT.id();
    }

    /** Test seam: clear proto lists (fresh-process mirror). */
    static void resetForTests() {
        for (int i = 0; i < protoLists.length; i++) {
            protoLists[i].head = null;
            protoLists[i].tail = null;
            protoLists[i].length = 0;
            // Fresh-process mirror: the static init seeds max_entries_num=1
            // for the six proto types (0 above that); proto_new bumps it per
            // id, so created protos must not leak their counters either.
            protoLists[i].maxEntriesNum = i < ObjTypes.OBJ_TYPE_PROTO_COUNT ? 1 : 0;
        }
        cdPathBase = "";
    }
}
