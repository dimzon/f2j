package org.fallout2ce;

import org.fallout2ce.ObjTypes.CritterManeuver;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjTypes.GameObjectListNode;
import org.fallout2ce.ObjTypes.ObjectFlags;
import org.fallout2ce.ObjTypes.OutlineType;
import org.fallout2ce.ObjTypes.Rotation;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoFlags;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Object creation slice, mirroring {@code src/object.cc} (creation path
 * only): {@code objectCreateWithProtoId} ({@code obj_pid_new}),
 * {@code objectCreateWithFrmIdProtoId} ({@code obj_new}), the floating
 * object list ({@code gObjectListHead} + {@code _obj_insert} for
 * tile {@code -1}), {@code scriptsNewObjectId} ({@code new_obj_id} from
 * {@code src/scripts.cc}), {@code objectSetLight} /
 * {@code _obj_turn_off_light} / {@code _obj_turn_on_light},
 * {@code _obj_toggle_flat}, {@code objectSetScriptFromProto} (gate only,
 * from {@code src/proto_instance.cc}) and {@code _proto_update_init} /
 * {@code objectDataReset} (gates only, from {@code src/proto.cc}).
 *
 * <p>Vectors: {@code [objcreate]} in
 * {@code docker/refharness/golden/v57.txt}, produced by
 * {@code sectionObjCreate} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Game objects reuse {@link ObjTypes.GameObject} (v33, renamed from C
 * {@code Object}); nothing is duplicated here. Flags stay immutable bit
 * bags: in-place {@code |=}/{@code &=}/{@code ^=} become reassignment.
 * Out-params reuse the single-element-array convention (see
 * {@link ProtoDb#protoGetProto}).
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>Fid source: C passes {@code Proto*} where {@code FrmId} is expected
 * via {@code FrmId(Proto* proto)} ({@code proto->fid}); Java passes
 * {@code FrmId.ofFid(proto.fid)} explicitly. The path-based assert
 * ({@code fid} path unsupported) is an explicit {@link AssertionError}
 * (vectors always pass fid-based ids).</li>
 * <li>{@code _obj_insert} order: tile {@code -1} prepends to the floating
 * head, exactly like C. The tile-bound branch (elevation/flat/art
 * ordering over {@code gObjectListHeadByTile}) needs the map and art
 * lock stack and is deferred to the map port; tile-bound nodes prepend
 * to the same head meanwhile so ids and iteration stay consistent.</li>
 * <li>{@code objectFindFirst}/{@code objectFindNext} iterate the floating
 * head only (the harness never places objects on tiles, so the C
 * tile-array scan sees the same empty set); the hidden-type filter
 * ({@link Art#artIsObjectTypeHidden}) is kept. Elevation/location
 * variants come with the map port.</li>
 * <li>{@code scriptsNewObjectId}: counter init 4, collision scan over the
 * object list, silent {@code >= 18000} guard (C {@code debugPrint} is
 * silent in Java, like {@link Reaction}), and the double-increment quirk
 * ({@code ++counter} before the scan and again before return), giving
 * ids 6, 8, 10, ...</li>
 * <li>{@code _proto_update_init} gate: the harness never calls
 * {@code protoInit}, so C returns -1 before touching data (fresh
 * calloc-zeroed objects make the skipped memset unobservable). Java
 * mirrors this with the {@code protosInitialized} seam (default false);
 * {@code _proto_update_gen} (item/scenery charges, container flags),
 * the critter branch ({@code _combat_data_init}, derived stats) and the
 * exit-grid misc fixup come with the proto/game ports.</li>
 * <li>{@code objectSetLight} with a null rect never touches tile arrays
 * here: fresh/vector protos carry light 0,0, and {@code _obj_adjust_light}
 * bails on the floating tile ({@code -1}) before the falloff tables.
 * The falloff application ({@code _light_offsets}, {@code objectGetRect})
 * comes with the map/render ports.</li>
 * <li>{@code objectSetScriptFromProto} runs the pure prefix (sid reset,
 * proto load, sid pick by object type: below TILE from
 * {@code proto.sid}, TILE from {@code proto.tile.sid}, MISC forced
 * {@code -1}); the {@code scriptAdd} tail needs the script engine, so a
 * non-{@code -1} sid also returns -1 with the object sid left at
 * {@code -1} (deferred, documented). Vector protos resolve to
 * {@code -1} (stim prints {@code sid=-1}); the extra-id rule
 * ({@code object->id == -1} grabs another id) is unreachable here
 * because the id is assigned before the call.</li>
 * <li>{@code protoGetProto}-miss quirk: like C, the miss returns 0
 * <em>after</em> insert (the object stays listed with pid/id set).</li>
 * </ul>
 */
public final class ObjectSystem {

    private ObjectSystem() {
    }

    /** Port of {@code gObjectListHead} (floating objects, tile {@code -1}). */
    private static GameObjectListNode head = null;

    /** Port of {@code gObjectFindLastObjectListNode} (single-list cursor). */
    private static GameObjectListNode findCursor = null;

    /** Port of {@code gObjectIdCounter} ({@code src/scripts.cc}, init 4). */
    private static int objectIdCounter = 4;

    /**
     * Seam mirroring {@code _protos_been_initialized} (set by
     * {@code protoInit}, unported; the harness never inits). Stays false
     * until the proto/game ports land.
     */
    static boolean protosInitialized = false;

    // 0x489C9C obj_pid_new.
    public static int objectCreateWithProtoId(GameObject[] objectPtr, ProtoId protoId) {
        objectPtr[0] = null;

        Proto[] proto = new Proto[1];
        if (ProtoDb.protoGetProto(protoId, proto) == -1) {
            return -1;
        }

        return objectCreateWithFrmIdProtoId(objectPtr, FrmId.ofFid(proto[0].fid), protoId);
    }

    // 0x489A84 obj_new.
    public static int objectCreateWithFrmIdProtoId(GameObject[] objectPtr, FrmId frmId, ProtoId protoId) {
        GameObjectListNode node = new GameObjectListNode();
        node.obj = objectAllocate();
        node.next = null;

        if (frmId.valid()) {
            if (!frmId.hasFid()) {
                throw new AssertionError("objectCreateWithFrmIdProtoId called with path based"
                        + " FrmId which is not supported!");
            }
        }

        node.obj.fid = frmId.fid();
        insertNode(node);

        if (objectPtr != null) {
            objectPtr[0] = node.obj;
        }

        node.obj.pid = protoId.pid();
        node.obj.id = scriptsNewObjectId();

        if (!protoId.valid() || protoId.objectType() == ObjTypes.ObjectType.OBJ_TYPE_TILE) {
            node.obj.data.inventory.items.clear();
            return 0;
        }

        protoUpdateInit(node.obj);

        Proto[] proto = new Proto[1];
        if (ProtoDb.protoGetProto(protoId, proto) == -1) {
            return 0;
        }

        objectSetLight(node.obj, proto[0].lightDistance, proto[0].lightIntensity);

        int protoFlags = proto[0].flags.bits();
        if ((protoFlags & ProtoFlags.FLAT.bits()) != ProtoFlags.NONE.bits()) {
            toggleFlat(node.obj);
        }

        if ((protoFlags & ProtoFlags.NO_BLOCK.bits()) != ProtoFlags.NONE.bits()) {
            node.obj.flags = node.obj.flags.or(ObjectFlags.NO_BLOCK);
        }

        if ((protoFlags & ProtoFlags.MULTIHEX.bits()) != ProtoFlags.NONE.bits()) {
            node.obj.flags = node.obj.flags.or(ObjectFlags.MULTIHEX);
        }

        if ((protoFlags & ProtoFlags.TRANS_NONE.bits()) != ProtoFlags.NONE.bits()) {
            node.obj.flags = node.obj.flags.or(ObjectFlags.TRANS_NONE);
        } else {
            if ((protoFlags & ProtoFlags.TRANS_WALL.bits()) != ProtoFlags.NONE.bits()) {
                node.obj.flags = node.obj.flags.or(ObjectFlags.TRANS_WALL);
            } else if ((protoFlags & ProtoFlags.TRANS_GLASS.bits()) != ProtoFlags.NONE.bits()) {
                node.obj.flags = node.obj.flags.or(ObjectFlags.TRANS_GLASS);
            } else if ((protoFlags & ProtoFlags.TRANS_STEAM.bits()) != ProtoFlags.NONE.bits()) {
                node.obj.flags = node.obj.flags.or(ObjectFlags.TRANS_STEAM);
            } else if ((protoFlags & ProtoFlags.TRANS_ENERGY.bits()) != ProtoFlags.NONE.bits()) {
                node.obj.flags = node.obj.flags.or(ObjectFlags.TRANS_ENERGY);
            } else if ((protoFlags & ProtoFlags.TRANS_RED.bits()) != ProtoFlags.NONE.bits()) {
                node.obj.flags = node.obj.flags.or(ObjectFlags.TRANS_RED);
            }
        }

        if ((protoFlags & ProtoFlags.LIGHT_THRU.bits()) != ProtoFlags.NONE.bits()) {
            node.obj.flags = node.obj.flags.or(ObjectFlags.LIGHT_THRU);
        }

        if ((protoFlags & ProtoFlags.SHOOT_THRU.bits()) != ProtoFlags.NONE.bits()) {
            node.obj.flags = node.obj.flags.or(ObjectFlags.SHOOT_THRU);
        }

        if ((protoFlags & ProtoFlags.WALL_TRANS_END.bits()) != ProtoFlags.NONE.bits()) {
            node.obj.flags = node.obj.flags.or(ObjectFlags.WALL_TRANS_END);
        }

        if ((protoFlags & ProtoFlags.NO_HIGHLIGHT.bits()) != ProtoFlags.NONE.bits()) {
            node.obj.flags = node.obj.flags.or(ObjectFlags.NO_HIGHLIGHT);
        }

        objectSetScriptFromProto(node.obj);

        return 0;
    }

    // 0x4A386C new_obj_id (src/scripts.cc).
    public static int scriptsNewObjectId() {
        GameObject found;

        do {
            objectIdCounter++;
            found = objectFindFirst();

            while (found != null) {
                if (objectIdCounter == found.id) {
                    break;
                }

                found = objectFindNext();
            }
        } while (found != null);

        // C prints "\n    ERROR: new_obj_id() !!!! Picked PLAYER ID!!!!" at
        // >= 18000 via debugPrint; Java ports stay silent (see Reaction).
        objectIdCounter++;

        return objectIdCounter;
    }

    // 0x48B3C0 obj_find_first (floating-head slice; elevation/location
    // variants come with the map port).
    public static GameObject objectFindFirst() {
        findCursor = head;
        return advanceFind();
    }

    // 0x48B41C obj_find_next.
    public static GameObject objectFindNext() {
        if (findCursor == null) {
            return null;
        }

        findCursor = findCursor.next;
        return advanceFind();
    }

    // 0x48AD50 obj_set_light (rect overload deferred: the slice always
    // passes null; rect rendering comes with the map/render ports).
    public static int objectSetLight(GameObject obj, int lightDistance, int lightIntensity) {
        if (obj == null) {
            return -1;
        }

        int rc = turnOffLight(obj);
        if (lightIntensity > 0) {
            obj.lightDistance = Math.min(lightDistance, 8);
            obj.lightIntensity = lightIntensity;

            rc = turnOnLight(obj);
        } else {
            obj.lightIntensity = 0;
            obj.lightDistance = 0;
        }

        return rc;
    }

    // 0x48AD48 obj_turn_on_light.
    public static int turnOnLight(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        if (obj.lightIntensity <= 0) {
            obj.flags = obj.flags.and(ObjectFlags.LIGHTING.not());
            return -1;
        }

        if (obj.flags.and(ObjectFlags.LIGHTING).equals(ObjectFlags.NONE)) {
            obj.flags = obj.flags.or(ObjectFlags.LIGHTING);

            // Rect is null in this slice, so the return is ignored like C.
            adjustLight(obj, false);
        }

        return 0;
    }

    // 0x48AD9C obj_turn_off_light.
    public static int turnOffLight(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        if (obj.lightIntensity <= 0) {
            obj.flags = obj.flags.and(ObjectFlags.LIGHTING.not());
            return -1;
        }

        if (!obj.flags.and(ObjectFlags.LIGHTING).equals(ObjectFlags.NONE)) {
            // Rect is null in this slice, so the return is ignored like C.
            adjustLight(obj, true);

            obj.flags = obj.flags.and(ObjectFlags.LIGHTING.not());
        }

        return 0;
    }

    // 0x48AF2C obj_toggle_flat (rect overload deferred like objectSetLight).
    public static int toggleFlat(GameObject object) {
        if (object == null) {
            return -1;
        }

        GameObjectListNode prev = null;
        GameObjectListNode node = head;
        while (node != null && node.obj != object) {
            prev = node;
            node = node.next;
        }

        if (node == null) {
            return -1;
        }

        if (prev != null) {
            prev.next = node.next;
        } else {
            head = node.next;
        }

        object.flags = object.flags.xor(ObjectFlags.FLAT);

        insertNode(node);

        return 0;
    }

    // 0x49A9B4 objectSetScriptFromProto (src/proto_instance.cc): pure
    // prefix only; the scriptAdd tail needs the script engine (see class
    // notes). Operates on obj.sid directly (C takes &obj->sid).
    static int objectSetScriptFromProto(GameObject object) {
        object.sid = -1;

        ProtoId protoId = ProtoId.ofGameObject(object);
        Proto[] slot = new Proto[1];
        if (ProtoDb.protoGetProto(protoId, slot) == -1) {
            return -1;
        }
        Proto proto = slot[0];

        int type = protoId.objectType().id();
        int sid;
        if (type < ObjTypes.ObjectType.OBJ_TYPE_TILE.id()) {
            sid = proto.sid;
        } else if (type == ObjTypes.ObjectType.OBJ_TYPE_TILE.id()) {
            sid = proto.tile.sid;
        } else if (type == ObjTypes.ObjectType.OBJ_TYPE_MISC.id()) {
            sid = -1;
        } else {
            throw new AssertionError("objectSetScriptFromProto: unreachable object type");
        }

        if (sid == -1) {
            return -1;
        }

        // Script engine (scriptAdd/scriptGetScript/spatial setup) unported;
        // the object sid stays -1 (see class notes).
        return -1;
    }

    // 0x49F8A0 proto_update_init (src/proto.cc): gate mirror only; the
    // _proto_update_gen tail (item/scenery/critter/exit-grid init) comes
    // with the proto/game ports (see class notes).
    static int protoUpdateInit(GameObject obj) {
        if (!protosInitialized) {
            return -1;
        }

        objectDataReset(obj);

        return 0;
    }

    // 0x49EEA4 clear_pupdate_data (src/proto.cc): zero the data union.
    static void objectDataReset(GameObject obj) {
        obj.data.inventory.items.clear();

        obj.data.critter.reaction = 0;
        obj.data.critter.combat.maneuver = CritterManeuver.NONE;
        obj.data.critter.combat.ap = 0;
        obj.data.critter.combat.results = Dam.NONE;
        obj.data.critter.combat.damageLastTurn = 0;
        obj.data.critter.combat.aiPacket = 0;
        obj.data.critter.combat.team = 0;
        obj.data.critter.combat.whoHitMe = null;
        obj.data.critter.combat.whoHitMeCid = 0;
        obj.data.critter.hp = 0;
        obj.data.critter.radiation = 0;
        obj.data.critter.poison = 0;

        obj.data.flags = 0;

        obj.data.item.weapon.ammoQuantity = 0;
        obj.data.item.weapon.ammoTypePid = 0;
        obj.data.item.ammo.quantity = 0;
        obj.data.item.misc.charges = 0;
        obj.data.item.key.keyCode = 0;

        obj.data.scenery.door.openFlags = 0;
        obj.data.scenery.stairs.destinationBuiltTile = 0;
        obj.data.scenery.stairs.destinationMap = null;
        obj.data.scenery.elevator.type = 0;
        obj.data.scenery.elevator.level = 0;
        obj.data.scenery.ladder.destinationBuiltTile = 0;
        obj.data.scenery.ladder.destinationMap = null;

        obj.data.misc.map = null;
        obj.data.misc.tile = 0;
        obj.data.misc.elevation = 0;
        obj.data.misc.rotation = Rotation.ROTATION_NE;
    }

    // Floating-head slice of 0x48D8E8 obj_insert: tile -1 prepends like C;
    // the tile-bound ordering branch is deferred to the map port.
    private static void insertNode(GameObjectListNode node) {
        if (node == null) {
            return;
        }

        node.next = head;
        head = node;
    }

    private static GameObject advanceFind() {
        while (findCursor != null) {
            GameObject obj = findCursor.obj;
            if (obj != null
                    && Art.artIsObjectTypeHidden(FrmId.ofFid(obj.fid).objectType().value()) == 0) {
                return obj;
            }
            findCursor = findCursor.next;
        }
        return null;
    }

    // Guards-only slice of 0x48D8E8-adjacent _obj_adjust_light: the tile
    // falloff application needs _light_offsets/objectGetRect (map/render
    // ports). increase=false adds light, true removes it.
    private static int adjustLight(GameObject obj, boolean remove) {
        if (obj == null) {
            return -1;
        }

        if (obj.lightIntensity <= 0) {
            return -1;
        }

        if (!obj.flags.and(ObjectFlags.HIDDEN).equals(ObjectFlags.NONE)) {
            return -1;
        }

        if (obj.flags.and(ObjectFlags.LIGHTING).equals(ObjectFlags.NONE)) {
            return -1;
        }

        if (!MapDefs.hexGridTileIsValid(obj.tile)) {
            return -1;
        }

        // Falloff application deferred (see class notes).
        return -1;
    }

    // 0x48D7D8 obj_allocate (fresh-zero + sentinel fields).
    private static GameObject objectAllocate() {
        GameObject object = new GameObject();
        object.id = -1;
        object.tile = -1;
        object.cid = -1;
        object.outline = OutlineType.NONE;
        object.pid = ProtoId.EMPTY_PID;
        object.sid = -1;
        object.owner = null;
        object.scriptIndex = -1;
        return object;
    }

    /** Test seam: clear the list + counter (fresh-process mirror). */
    static void resetForTests() {
        head = null;
        findCursor = null;
        objectIdCounter = 4;
        protosInitialized = false;
    }
}
