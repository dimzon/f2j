package org.fallout2ce;

import java.util.Arrays;
import java.util.List;

import org.fallout2ce.ObjTypes.CritterManeuver;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjTypes.GameObjectListNode;
import org.fallout2ce.ObjTypes.InventoryItem;
import org.fallout2ce.ObjTypes.ObjectFlags;
import org.fallout2ce.ObjTypes.OutlineType;
import org.fallout2ce.ObjTypes.Rotation;
import org.fallout2ce.ProtoTypes.CritterProtoTypeId;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoFlags;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Object creation slice, mirroring {@code src/object.cc} (creation path
 * only): {@code objectCreateWithProtoId} ({@code obj_pid_new}),
 * {@code objectCreateWithFrmIdProtoId} ({@code obj_new}), the floating
 * object list ({@code gObjectListHead} + {@code _obj_insert} for
 * tile {@code -1}), the tile-placed list ({@code gObjectListHeadByTile} +
 * {@code _obj_connect}/{@code _obj_connect_to_tile}, with the
 * elevation/flat ordering walk), {@code objectSetFrmId}
 * ({@code obj_change_fid}, rect overload deferred), {@code scriptsNewObjectId} ({@code new_obj_id} from
 * {@code src/scripts.cc}), {@code objCopy} ({@code _obj_copy}:
 * memcpy-scope field copy, fresh id, sid/rotation setup, recursive
 * inventory re-home), {@code objectSetLight} /
 * {@code _obj_turn_off_light} / {@code _obj_turn_on_light},
 * {@code _obj_toggle_flat}, {@code objectSetScriptFromProto} (gate only,
 * from {@code src/proto_instance.cc}), {@code _proto_update_init} /
 * {@code objectDataReset} (gates only, from {@code src/proto.cc}), plus the
 * destroy path: {@code objectDestroy} ({@code obj_erase_object}) over
 * {@code _obj_remove} / {@code _obj_inven_free} / {@code objectGetListNode}
 * (floating-head slice) and {@code objectGetOwner}, plus the visibility
 * slice: {@code objectShow} / {@code objectHide} ({@code obj_turn_on} /
 * {@code obj_turn_off}) and {@code objectDisconnect}
 * ({@code obj_disconnect}) with {@code objectHasOutline}
 * ({@code src/object.h}).
 *
 * <p>Vectors: {@code [objcreate]} in
 * {@code docker/refharness/golden/v57.txt}, produced by
 * {@code sectionObjCreate} in {@code docker/refharness/ref_main.cc}, and
 * the destroy tail ({@code destroy}/{@code destroynull}) of
 * {@code [itemmove]} in {@code docker/refharness/golden/v62.txt}
 * (produced by {@code sectionItemMove}), and the tile-connect state
 * backing {@code [itemdrop]} in
 * {@code docker/refharness/golden/v63.txt} (produced by
 * {@code sectionItemDrop}: {@code itemDropAll} drops land on a tile via
 * {@code objectConnect}), and {@code [objvis]} in
 * {@code docker/refharness/golden/v69.txt} (produced by
 * {@code sectionObjVis}), and {@code [objplace]} in
 * {@code docker/refharness/golden/v86.txt} (produced by
 * {@code sectionObjPlace}), and {@code [partyobjects]} in
 * {@code docker/refharness/golden/v96.txt} (produced by
 * {@code sectionPartyObjects}: {@code partyMembersInit} return,
 * {@code objectsInit} return, dude pid/id, egg pid/id, dude re-add).
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
 * head, exactly like C. The tile-bound branch walks the tile list with
 * the C elevation/flat ordering (lower elevation first; a flat object
 * sorts before a non-flat one at equal elevation); the art-lock depth
 * tiebreak inside the equal-elevation/equal-flatness case needs the art
 * lock stack and stays a seam (game phase), so same-flatness objects
 * keep arrival order there. C++ would index out of bounds for an
 * invalid non-{@code -1} tile; Java guards it back to the floating
 * head (unreachable: every writer validates the tile first).</li>
 * <li>{@code objectFindFirst}/{@code objectFindNext} scan the tile
 * heads ({@code gObjectListHeadByTile}, tiles {@code 0..39999} in
 * order), exactly like C ({@code objectFindFirst}/{@code objectFindNext}
 * in {@code src/object.cc}): floating (tile {@code -1}) objects are
 * invisible to find. The hidden-type filter
 * ({@link Art#artIsObjectTypeHidden}) is kept. Cursor state mirrors
 * {@code gObjectFindTile}/{@code gObjectFindElevation}/
 * {@code gObjectFindLastObjectListNode}.</li>
 * <li>{@code scriptsNewObjectId}: counter init 4, collision scan over the
 * tile heads via {@code objectFindFirst}/{@code objectFindNext} (floating
 * objects are skipped, exactly like C), silent {@code >= 18000} guard (C {@code debugPrint} is
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
  * <li>{@code objectDestroy} takes no rect: the slice always passes null
  * (the light call keeps its null-rect shape: the adjust result is
  * ignored, like C++). The non-null-rect path ({@code objectGetRect} into
  * the caller rect) needs the render engine (game phase).</li>
  * <li>{@code _obj_remove} runs no event-queue removal: the C++ body calls
  * {@code _obj_inven_free}, the sid destroy proc + {@code scriptRemove},
  * and {@code aiRemoveBurstDisabled}, but never {@code queueRemoveEvents}.
  * The sid/AI tails need the script/AI engines (game phase); harness sids
  * are {@code -1}, so they are dormant.</li>
  * <li>{@code objectShow} / {@code objectHide} / {@code objectDisconnect}
 * take no rect: the slice always passes null (the light calls keep their
 * null-rect shape: the adjust result is ignored, like C++; the
 * non-null-rect paths ({@code objectGetRect} into the caller rect, plus
 * the {@code gDude}/{@code gEgg} rect unions) need the render engine
 * (game phase). {@code gDude} / {@code gEgg} are unported game-phase
 * globals (null headless), so the {@code obj == gDude} branches can never
 * fire here; dormant by construction.</li>
 * <li>{@code objectDisconnect} frees the list NODE, not the object (the GC
 * owns the object; only the unlink observes), then sets
 * {@code obj.tile = -1}, exactly like C. Double-list: {@code objectConnect}
 * already allocates a second tile node without unlinking the floating
 * node (C++ {@code _obj_connect} never unlinks there — verified, no fix
 * needed), so the first disconnect unlinks the TILE node and the second
 * finds the FLOATING node (tile already {@code -1} scans the floating
 * head) and also returns 0; a third misses ({@code -1}). The head follows
 * the node's pre-disconnect tile via {@code unlinkNode}, exactly like
 * C.</li>
 * <li>{@code objectHasOutline} mirrors the C inline
 * ({@code (outline & OUTLINE_TYPE_MAX) != OUTLINE_TYPE_NONE}); the hide
 * path sets {@code OUTLINE_DISABLED} only when it holds, the show path
 * always clears {@code OUTLINE_DISABLED}, exactly like C++.</li>
 * <li>{@code resetForTests} gains no new state: flags/outline/tile live on
 * the {@link GameObject} instances; the list heads + find cursor +
 * counter reset already covers this slice.</li>
 * <li>{@code _gmouse_remove_item_outline} needs the game-mouse highlight
 * state ({@code gGameMouseHighlightedItem}, game phase, null headless) +
  * tile refresh; headless it is a no-op seam.</li>
 * <li>{@code objectGetListNode}/{@code _obj_remove} unlink: the head
 * follows the node's tile ({@code gObjectListHead} for tile
 * {@code -1}, {@code gObjectListHeadByTile[tile]} otherwise), exactly
 * like C. {@code _obj_connect} allocates a second node for the tile
 * list and leaves the floating node in place (C++ never unlinks the
 * old node there); both lists then reference the object, so
 * {@code objectFindFirst}/{@code objectFindNext} (now the tile-head
 * slice, like C++) keep seeing dropped objects only via the tile
 * heads; the floating nodes stay invisible to find.</li>
 * <li>{@code objectSetLocation} ({@code obj_move_to_tile}) takes no
 * rect: the slice always passes null (adjust-light results ignored,
 * no rect fill/union, like C++). It unlinks the existing node and
 * reuses the {@code _obj_connect_to_tile} body inline (field set +
 * {@code insertNode} + add-light); the {@code objectConnect}
 * second-node quirk does NOT apply (C reuses the unlinked node
 * there). The {@code isInCombat} outline tail, the {@code gDude}
 * exit-grid/roof/egg/elevation tails and the non-null-rect unions
 * need combat/map/render (game phase); dormant headless.</li>
 * <li>{@code objectFindById} ({@code objFindObjPtrFromID}) scans via
 * {@code objectFindFirst}/{@code objectFindNext}, exactly like C.</li>
 * <li>{@code objectGetLightIntensity} ({@code obj_get_visible_light})
 * reads the RAW tile value ({@code lightGetTrueTileIntensity}, not
 * the clamped getter) vs ambient with the C clamp pin
 * ({@code >= ambient} keeps, capped at {@code LIGHT_INTENSITY_MAX},
 * else ambient). The {@code gDude} self-light subtract is dormant
 * (unported game-phase global, null headless).</li>
 * <li>{@code objectSetFrame} ({@code obj_set_frame}) locks art for the
 * fid and gates {@code frame >= frameCount} only (no lower-bound
 * gate, like C++); fid untouched. Next/prev wrap like C++
 * ({@code >= count -> 0}, {@code < 0 -> count - 1}). A null art
 * lock (or null decoded art) returns -1, like the C null-art miss.
 * Rect overloads deferred (null-rect shape only).</li>
 * <li>{@code objectEnableOutline} clears {@code OUTLINE_DISABLED} and
 * {@code objectDisableOutline} sets it iff outlined (null returns
 * -1), exactly like C++; rect overloads deferred.</li>
 * <li>{@code resetForTests} gains no new state: setLocation reuses the
 * listed node (no new globals), findById rides the find cursor,
 * light state resets via {@code Light}, art via {@code Art}.</li>
 * <li>{@code objectsInit} ({@code obj_init}, 0x488780) ports the whole
 * headless-observable prefix: seen-array zeroing, update-area bounds +
 * hex-size math, tile-head clear, the offset/order/render table inits,
 * {@code lightInit} (existing), {@code textObjectsInit} (minimal, see
 * {@link TextObjects}), the light + blend table inits, the
 * {@code centerToUpperLeft} store via {@link TileMath#tileFromScreenXY},
 * the window state store, and the dude/egg creation
 * ({@code objectCreateWithFrmIdProtoId} + flags + dude
 * {@code objectSetLight(4, 0x10000)} + {@link PartyMembers#partyMemberAdd},
 * whose {@code exit(1)} becomes {@link IllegalStateException}). The egg
 * gets no light call, like C++. Dude pid is
 * {@code buildPid(CRITTER, 0)} = 16777216; its id is the fixed
 * {@code (pid & kMaxProtoId) + 18000} = 18000 (the id counter is
 * untouched). Error paths mirror C (offset failure → {@code -1};
 * order/render/light/text failures → order + offset exits → {@code -1}).
 * {@code objectsReset}/{@code objectsExit} mirror C (remove-all over
 * both lists, {@code NO_REMOVE} cleared first on exit; exit leaves
 * {@code objectsInitialized} set, like C — {@code resetForTests} owns
 * the fresh state).</li>
 * <li>{@code _obj_offset_table_init} runs the real tile math
 * ({@code tileFromScreenXY}/{@code tileToScreenXY}/
 * {@code tileSetCenter(+1, IGNORE)} twice, like C); it needs tile state
 * from {@link TileMath#tileInit} first, exactly as the harness has it
 * when the section runs. The order-table sort uses a stable sort where
 * C uses {@code qsort}, but the keys (distinct tile offsets) form a
 * total order, so the permutation is identical.</li>
 * <li>Blend-table colors use the runtime {@code colorTable[rgb555()]}
 * lookups, like the C macros; headless the table is zero-filled (no
 * palette load ported yet), so the indices read 0 until the color/game
 * ports land. Unobserved by vectors either way.</li>
 * <li>{@code gDude}/{@code gEgg} used to be dormant null-seam
 * globals in the notes above; this slice ports them for real (see
 * {@link #getDude}/{@link #getEgg}). {@link Stat}, {@link Skill},
 * {@link Critter}, {@link Inventory} and {@link Item} keep their own
 * null holders, so every {@code == gDude} branch there stays dormant
 * (identity vs null can never fire) even with a live dude — isolation
 * holds by construction, and the test teardown clears the holders
 * anyway.</li>
 * </ul>
 */
public final class ObjectSystem {

    private ObjectSystem() {
    }

    /** Port of {@code gObjectListHead} (floating objects, tile {@code -1}). */
    private static GameObjectListNode head = null;

    /**
     * Port of {@code gObjectListHeadByTile} (tile-placed objects, indexed
     * by hex-grid tile). Entries are null until the first
     * {@code objectConnect} to that tile, like the C zeroed array.
     */
    private static GameObjectListNode[] tileHeads = new GameObjectListNode[MapDefs.HEX_GRID_SIZE];

    /** Port of {@code gObjectFindLastObjectListNode} (tile-scan cursor). */
    private static GameObjectListNode findCursor = null;

    /** Port of {@code gObjectFindTile} (tile-scan cursor). */
    private static int findTile = 0;

    /** Port of {@code gObjectFindElevation} (tile-scan cursor). */
    private static int findElevation = 0;

    /** Port of {@code gObjectIdCounter} ({@code src/scripts.cc}, init 4). */
    private static int objectIdCounter = 4;

    /**
     * Seam mirroring {@code _protos_been_initialized} (set by
     * {@code protoInit}, unported; the harness never inits). Stays false
     * until the proto/game ports land.
     */
    static boolean protosInitialized = false;

    /** Port of {@code gObjectsInitialized} (0x5195F8). */
    private static boolean objectsInitialized;

    /** Port of {@code gObjectsUpdateAreaHexWidth} (0x5195FC). */
    private static int updateAreaHexWidth;

    /** Port of {@code gObjectsUpdateAreaHexHeight} (0x519600). */
    private static int updateAreaHexHeight;

    /** Port of {@code gObjectsUpdateAreaHexSize} (0x519604). */
    private static int updateAreaHexSize;

    /** Ports of {@code _orderTable[2]} (0x519608). */
    private static int[] orderTableEven;
    private static int[] orderTableOdd;

    /** Ports of {@code _offsetTable[2]} (0x519610). */
    private static int[] offsetTableEven;
    private static int[] offsetTableOdd;

    /** Port of {@code _offsetDivTable} (0x519618). */
    private static int[] offsetDivTable;

    /** Port of {@code _offsetModTable} (0x51961C). */
    private static int[] offsetModTable;

    /** Port of {@code _renderTable} (0x519620, null until init). */
    private static GameObjectListNode[] renderTable;

    /** Port of {@code gObjectsUpdateAreaPixelBounds} (0x639D90). */
    private static Geometry.Rect updateAreaPixelBounds = new Geometry.Rect();

    // 0x6391D0 light_blocked: render-only occlusion cache, never read by
    // this slice (game phase); not modelled.

    /** Port of {@code _light_offsets[2][6][36]} (0x639530), indexed [parity][rotation]. */
    private static int[][][] lightOffsets = new int[2][ObjTypes.ROTATION_COUNT][36];

    /** Port of {@code gObjectsWindowRect} (0x639BF0). */
    private static Geometry.Rect windowRect = new Geometry.Rect();

    /** Port of {@code _glassGrayTable} (0x660EA0). */
    private static int[] glassGrayTable = new int[Color.COLOR_COUNT];

    /** Port of {@code _commonGrayTable} (0x660FA0). */
    private static int[] commonGrayTable = new int[Color.COLOR_COUNT];

    /** Port of {@code gObjectsWindowBufferSize} (0x6610A0). */
    private static int windowBufferSize;

    /** Port of {@code gObjectsWindowBuffer} (0x6610A4, borrowed, like C). */
    private static byte[] windowBuffer;

    /** Port of {@code gObjectsWindowHeight} (0x6610A8). */
    private static int windowHeight;

    /** Port of {@code gObjectsWindowWidth} (0x6610B4). */
    private static int windowWidth;

    /** Port of {@code gObjectsWindowPitch} (0x6610B0). */
    private static int windowPitch;

    /** Port of {@code obj_dude} (0x6610B8). */
    private static GameObject dude;

    /** Port of {@code obj_egg} (0x6610AC). */
    private static GameObject egg;

    /** Seen-bit size ({@code _obj_seen}/{@code _obj_seen_check}, 5001 bytes each). */
    static final int OBJ_SEEN_SIZE = 5001;

    // 0x6610BC obj_seen_check: render-only, never zeroed by objectsInit;
    // modelled for shape parity, zeroed by resetForTests only.
    private static byte[] objSeenCheck = new byte[OBJ_SEEN_SIZE];

    /** Port of {@code _obj_seen} (0x662445, zeroed by init/reset). */
    private static byte[] objSeen = new byte[OBJ_SEEN_SIZE];

    /** Ports of {@code _obj_last_roof_x} / {@code _obj_last_roof_y} (BSS -1). */
    private static int objLastRoofX = -1;
    private static int objLastRoofY = -1;

    /** Port of {@code _obj_last_elev} (BSS -1). */
    private static int objLastRoofElev = -1;

    /** Port of {@code _obj_last_is_empty} (BSS true). */
    private static boolean objLastIsEmpty = true;

    /** Seam for {@code isInCombat()} (combat state unported; cf. {@link Stat}). */
    private static boolean isInCombat() {
        return false;
    }

    // 0x48C76C obj_set_seen (NOTE: Uninline in C).
    static void objSetSeen(int tile) {
        objSeen[tile >> 3] |= (byte) (1 << (tile & 7));
    }

    // 0x48C754 _obj_clear_seen.
    static void objClearSeen() {
        java.util.Arrays.fill(objSeen, (byte) 0);
    }

    // 0x489F90 _obj_move (rect overload deferred: the slice always passes
    // null, so the rect fill/union tails are dropped, like C++). Null -> -1.
    // A listed tile detaches to floating (tile -1, elevation stored) and
    // re-inserts; an unlisted tile only changes elevation, like C. The art
    // size sets the screen offset; the dude tail moves the egg along.
    static int objMove(GameObject a1, int x, int y, int elevation) {
        if (a1 == null) {
            return -1;
        }

        GameObjectListNode[] prevSlot = new GameObjectListNode[1];
        GameObjectListNode node = null;
        boolean reinsert = false;

        int tile = a1.tile;
        if (MapDefs.hexGridTileIsValid(tile)) {
            node = findListNode(a1, prevSlot);
            if (node == null) {
                return -1;
            }

            adjustLight(a1, true);
            unlinkNode(node, prevSlot[0]);

            a1.tile = -1;
            a1.elevation = elevation;
            reinsert = true;
        } else {
            if (elevation == a1.elevation) {
                // Rect-only path (deferred, see class notes).
            } else {
                node = findListNode(a1, prevSlot);
                if (node == null) {
                    return -1;
                }

                unlinkNode(node, prevSlot[0]);

                a1.elevation = elevation;
                reinsert = true;
            }
        }

        try (Art.ArtLock lock = Art.artLock(FrmId.ofFid(a1.fid))) {
            if (lock != null && lock.art() != null) {
                int[] width = new int[1];
                int[] height = new int[1];
                Art.artGetSize(lock.art(), a1.frame, a1.rotation.id(), width, height);
                a1.sx = x - width[0] / 2;
                a1.sy = y - (height[0] - 1);
            }
        }

        if (reinsert) {
            insertNode(node);
        }

        if (a1 == dude) {
            objMove(egg, x, y, elevation);
        }

        return 0;
    }

    // 0x48B338 _obj_remove_all: tile heads then floating head via _obj_remove
    // (NO_REMOVE survives, prev advances, like C). The _scr_remove_all
    // script tail is game phase (no script registry headless; harness sids
    // are -1); the roof-cache stores run for real below.
    static void objRemoveAll() {
        removeAllObjects();
    }

    // constexpr isExitGridProtoId (src/proto.h:143): MISC pid inside
    // [FirstExitGrid, LastExitGrid].
    private static boolean isExitGridProtoId(GameObject obj) {
        return obj.pid >= ProtoTypes.MiscProtoTypeId.FirstExitGrid.id()
                && obj.pid <= ProtoTypes.MiscProtoTypeId.LastExitGrid.id();
    }

    // objectAttemptPlacementPartyMember (src/proto_instance.cc:2319): null /
    // tile guards, the wmEval search loop (worldmap/pathfinder phase behind
    // the seam below; the vectors never reach it with a one-dude party),
    // then show + place.
    static int objectAttemptPlacementPartyMember(GameObject obj, int tile, int elevation) {
        if (obj == null) {
            return -1;
        }

        if (tile == -1) {
            return -1;
        }

        int destinationTile = tile;
        int rotation = org.fallout2ce.Rotation.NW.value();
        if (!wmEvalTileNumForPlacement(tile)) {
            GameObject dude = getDude();
            destinationTile = dude != null ? dude.tile : tile;
            for (int i = 1; i <= 100; i++) {
                rotation++;
                destinationTile = TileMath.tileGetTileInDirection(destinationTile,
                        org.fallout2ce.Rotation.fromInt(rotation % org.fallout2ce.Rotation.COUNT), 1);
                if (wmEvalTileNumForPlacement(destinationTile)) {
                    break;
                }

                if (dude != null && TileMath.tileDistanceBetween(dude.tile, destinationTile) > 8) {
                    destinationTile = tile;
                    break;
                }
            }
        }

        objectShow(obj);
        objectSetLocation(obj, destinationTile, elevation);

        return 0;
    }

    /**
     * Seam for {@code wmEvalTileNumForPlacement} (worldmap blocking +
     * pathfinder, game phase, unported). The vectors never reach the search
     * (party is the dude alone), so the seam assumes placeable until the
     * worldmap slice lands.
     */
    private static boolean wmEvalTileNumForPlacement(int tile) {
        return true;
    }

    /** Port of {@code _centerToUpperLeft} (0x51962C). */
    private static int centerToUpperLeft;

    /** Ports of the {@code _obj_blend_table_*} tables (0x519780+). */
    private static byte[] wallBlendTable;
    private static byte[] glassBlendTable;
    private static byte[] steamBlendTable;
    private static byte[] energyBlendTable;
    private static byte[] redBlendTable;

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

    // 0x... _obj_copy (src/object.cc): null src -> -1, else allocate +
    // data-reset, memcpy the whole Object over the fresh one, publish via
    // the out-param, insert, mint a fresh id, re-run the sid setup when
    // the copied sid is live, re-apply the src rotation, clear QUEUED,
    // then re-home the inventory through recursive copies + itemAdd.
    // Failure paths return -1 with the node left listed: C only destroys
    // the node wrapper there (the listed object leaks, marked TODO), and
    // the GC owns the Java object, so no unlink runs either.
    public static int objCopy(GameObject[] objectPtr, GameObject src) {
        if (src == null) {
            return -1;
        }

        GameObjectListNode node = new GameObjectListNode();
        node.obj = objectAllocate();
        node.next = null;

        objectDataReset(node.obj);

        copyGameObject(node.obj, src);

        if (objectPtr != null) {
            objectPtr[0] = node.obj;
        }

        insertNode(node);

        node.obj.id = scriptsNewObjectId();

        if (node.obj.sid != -1) {
            node.obj.sid = -1;
            objectSetScriptFromProto(node.obj);
        }

        if (objectSetRotation(node.obj, src.rotation) == -1) {
            return -1;
        }

        node.obj.flags = node.obj.flags.and(ObjectFlags.QUEUED.not());

        node.obj.data.inventory.items.clear();

        List<InventoryItem> srcItems = src.data.inventory.items;
        for (int index = 0; index < srcItems.size(); index++) {
            InventoryItem srcEntry = srcItems.get(index);

            GameObject[] newItem = new GameObject[1];
            if (objCopy(newItem, srcEntry.item) == -1) {
                return -1;
            }

            // NOTE: C checks `== 1` here, which itemAdd never returns
            // (it returns 0/-1; the realloc failure is -1 and slips
            // through there). Java checks failure directly; unreachable
            // in practice (list adds don't fail), vectors never arm it.
            if (Item.itemAdd(node.obj, newItem[0], srcEntry.quantity) != 0) {
                return -1;
            }
        }

        return 0;
    }

    // memcpy(node->obj, src, sizeof(Object)): field-for-field copy of the
    // whole struct (Java has no padding, so the field list is the exact
    // equivalent). The inventory stack list is deliberately NOT carried
    // over: C memcpys the items pointer but zeroes length/capacity, then
    // refills via recursive copies + itemAdd. The dst keeps its fresh
    // empty list from objectAllocate/objectDataReset, which the caller
    // refills the same way.
    private static void copyGameObject(GameObject dst, GameObject src) {
        dst.id = src.id;
        dst.tile = src.tile;
        dst.x = src.x;
        dst.y = src.y;
        dst.sx = src.sx;
        dst.sy = src.sy;
        dst.frame = src.frame;
        dst.rotation = src.rotation;
        dst.fid = src.fid;
        dst.flags = src.flags;
        dst.elevation = src.elevation;
        dst.pid = src.pid;
        dst.cid = src.cid;
        dst.lightDistance = src.lightDistance;
        dst.lightIntensity = src.lightIntensity;
        dst.outline = src.outline;
        dst.sid = src.sid;
        dst.owner = src.owner;
        dst.scriptIndex = src.scriptIndex;

        dst.data.flags = src.data.flags;

        dst.data.critter.reaction = src.data.critter.reaction;
        dst.data.critter.combat.maneuver = src.data.critter.combat.maneuver;
        dst.data.critter.combat.ap = src.data.critter.combat.ap;
        dst.data.critter.combat.results = src.data.critter.combat.results;
        dst.data.critter.combat.damageLastTurn = src.data.critter.combat.damageLastTurn;
        dst.data.critter.combat.aiPacket = src.data.critter.combat.aiPacket;
        dst.data.critter.combat.team = src.data.critter.combat.team;
        dst.data.critter.combat.whoHitMe = src.data.critter.combat.whoHitMe;
        dst.data.critter.combat.whoHitMeCid = src.data.critter.combat.whoHitMeCid;
        dst.data.critter.hp = src.data.critter.hp;
        dst.data.critter.radiation = src.data.critter.radiation;
        dst.data.critter.poison = src.data.critter.poison;

        dst.data.item.weapon.ammoQuantity = src.data.item.weapon.ammoQuantity;
        dst.data.item.weapon.ammoTypePid = src.data.item.weapon.ammoTypePid;
        dst.data.item.ammo.quantity = src.data.item.ammo.quantity;
        dst.data.item.misc.charges = src.data.item.misc.charges;
        dst.data.item.key.keyCode = src.data.item.key.keyCode;

        dst.data.scenery.door.openFlags = src.data.scenery.door.openFlags;
        dst.data.scenery.stairs.destinationBuiltTile = src.data.scenery.stairs.destinationBuiltTile;
        dst.data.scenery.stairs.destinationMap = src.data.scenery.stairs.destinationMap;
        dst.data.scenery.elevator.type = src.data.scenery.elevator.type;
        dst.data.scenery.elevator.level = src.data.scenery.elevator.level;
        dst.data.scenery.ladder.destinationBuiltTile = src.data.scenery.ladder.destinationBuiltTile;
        dst.data.scenery.ladder.destinationMap = src.data.scenery.ladder.destinationMap;

        dst.data.misc.map = src.data.misc.map;
        dst.data.misc.tile = src.data.misc.tile;
        dst.data.misc.elevation = src.data.misc.elevation;
        dst.data.misc.rotation = src.data.misc.rotation;
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

    // 0x48B3A8 obj_find_first: tile-head scan (floating objects are
    // invisible), exactly like C.
    public static GameObject objectFindFirst() {
        findElevation = 0;

        for (findTile = 0; findTile < MapDefs.HEX_GRID_SIZE; findTile++) {
            GameObjectListNode node = tileHeads[findTile];
            while (node != null) {
                GameObject obj = node.obj;
                if (!isFindHidden(obj)) {
                    findCursor = node;
                    return obj;
                }
                node = node.next;
            }
        }

        findCursor = null;
        return null;
    }

    // 0x48B41C obj_find_next.
    public static GameObject objectFindNext() {
        if (findCursor == null) {
            return null;
        }

        GameObjectListNode node = findCursor.next;

        while (true) {
            if (node == null) {
                findTile++;
                if (findTile >= MapDefs.HEX_GRID_SIZE) {
                    break;
                }

                node = tileHeads[findTile];
            }

            while (node != null) {
                GameObject obj = node.obj;
                if (!isFindHidden(obj)) {
                    findCursor = node;
                    return obj;
                }
                node = node.next;
            }
        }

        findCursor = null;
        return null;
    }

    // 0x48B48C obj_find_first_at: same tile-head scan with the elevation
    // filter, exactly like C.
    public static GameObject objectFindFirstAtElevation(int elevation) {
        findElevation = elevation;
        findTile = 0;

        for (findTile = 0; findTile < MapDefs.HEX_GRID_SIZE; findTile++) {
            GameObjectListNode node = tileHeads[findTile];
            while (node != null) {
                GameObject obj = node.obj;
                if (obj != null && obj.elevation == elevation && !isFindHidden(obj)) {
                    findCursor = node;
                    return obj;
                }
                node = node.next;
            }
        }

        findCursor = null;
        return null;
    }

    // 0x48B510 obj_find_next_at.
    public static GameObject objectFindNextAtElevation() {
        if (findCursor == null) {
            return null;
        }

        GameObjectListNode node = findCursor.next;

        while (true) {
            if (node == null) {
                findTile++;
                if (findTile >= MapDefs.HEX_GRID_SIZE) {
                    break;
                }

                node = tileHeads[findTile];
            }

            while (node != null) {
                GameObject obj = node.obj;
                if (obj != null && obj.elevation == findElevation && !isFindHidden(obj)) {
                    findCursor = node;
                    return obj;
                }
                node = node.next;
            }
        }

        findCursor = null;
        return null;
    }

    // 0x48B2E8 objFindObjPtrFromID: tile-head scan via
    // objectFindFirst/objectFindNext, exactly like C.
    public static GameObject objectFindById(int id) {
        GameObject obj = objectFindFirst();
        while (obj != null) {
            if (obj.id == id) {
                return obj;
            }
            obj = objectFindNext();
        }

        return null;
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

    // 0x48AD04 obj_get_visible_light: RAW tile intensity
    // (lightGetTrueTileIntensity, not the clamped getter) vs ambient,
    // with the C clamp pin ({@code >= ambient} keeps, capped at
    // LIGHT_INTENSITY_MAX, else ambient). The gDude self-light subtract
    // is dormant (unported game-phase global, null headless). Like C++,
    // a null object is not guarded (it dereferences).
    public static int objectGetLightIntensity(GameObject obj) {
        int ambientIntensity = Light.lightGetAmbientIntensity();
        int tileIntensity = Light.lightGetTrueTileIntensity(obj.elevation, obj.tile);

        if (obj == dude) {
            tileIntensity -= dude.lightIntensity;
        }

        if (tileIntensity >= ambientIntensity) {
            if (tileIntensity > Light.LIGHT_INTENSITY_MAX) {
                tileIntensity = Light.LIGHT_INTENSITY_MAX;
            }
        } else {
            tileIntensity = ambientIntensity;
        }

        return tileIntensity;
    }

    // 0x48AF2C obj_toggle_flat (rect overload deferred like objectSetLight;
    // the slice always passes null, so both C++ paths collapse into the
    // unlink + xor + insert below).
    public static int toggleFlat(GameObject object) {
        if (object == null) {
            return -1;
        }

        GameObjectListNode[] prevSlot = new GameObjectListNode[1];
        GameObjectListNode node = findListNode(object, prevSlot);
        if (node == null) {
            return -1;
        }

        unlinkNode(node, prevSlot[0]);

        object.flags = object.flags.xor(ObjectFlags.FLAT);

        insertNode(node);

        return 0;
    }

    // 0x489EC4 obj_connect (rect overload deferred: the slice always
    // passes null, like objectDestroy; the light call inside
    // connectToTile keeps its null-rect shape).
    public static int objectConnect(GameObject object, int tile, int elevation) {
        if (object == null) {
            return -1;
        }

        if (!MapDefs.hexGridTileIsValid(tile)) {
            return -1;
        }

        if (!MapDefs.elevationIsValid(elevation)) {
            return -1;
        }

        GameObjectListNode node = new GameObjectListNode();
        node.obj = object;
        node.next = null;

        return connectToTile(node, tile, elevation);
    }

    // 0x48A568 obj_move_to_tile (rect overload deferred: the slice always
    // passes null, so the rect fill/union tails are dropped and the
    // adjust-light results are ignored, like C++). Null -> -1, bad
    // tile/elev -> -1, list-node miss -> -1, else unlink + the
    // _obj_connect_to_tile body on the SAME node (field set, insert,
    // add-light), the combat outline tail (dormant: isInCombat seam), the
    // dude exit-grid scan (body kept; the worldmap mark is a documented
    // seam), seen-bit store, roof-cache store (fill calls deferred, see
    // class notes), the egg follow and the elevation-change tail
    // (textObjectsReset/mapSetElevation/tileSetCenter kept; the combat quit
    // flag is deferred, like the non-dude _combat_delete_critter tail).
    public static int objectSetLocation(GameObject obj, int tile, int elevation) {
        if (obj == null) {
            return -1;
        }

        if (!MapDefs.hexGridTileIsValid(tile)) {
            return -1;
        }

        if (!MapDefs.elevationIsValid(elevation)) {
            return -1;
        }

        GameObjectListNode[] prevSlot = new GameObjectListNode[1];
        GameObjectListNode node = findListNode(obj, prevSlot);
        if (node == null) {
            return -1;
        }

        // _obj_adjust_light(obj, 1, null): null-rect shape, result ignored.
        adjustLight(obj, true);

        int oldElevation = obj.elevation;

        unlinkNode(node, prevSlot[0]);

        // _obj_connect_to_tile body (same node, not a second node like
        // objectConnect; see class notes).
        node.obj.tile = tile;
        node.obj.elevation = elevation;
        node.obj.x = 0;
        node.obj.y = 0;
        node.obj.owner = null;

        insertNode(node);

        // _obj_adjust_light(obj, 0, null): null-rect shape, result ignored.
        adjustLight(node.obj, false);

        if (isInCombat()) {
            if (FrmId.ofFid(obj.fid).objectType() == ObjectType.CRITTER) {
                // _combat_update_critter_outline_for_los(obj, enableOutline):
                // combat engine (game phase). The seam is false, so this
                // stays dormant, exactly like C headless.
            }
        }

        if (obj == dude) {
            GameObjectListNode listNode = tileHeads[tile];
            while (listNode != null) {
                GameObject other = listNode.obj;
                if (elevation == other.elevation) {
                    if (FrmId.ofFid(other.fid).objectType() == ObjectType.MISC && isExitGridProtoId(other)) {
                        if (other.flags.and(ObjectFlags.HIDDEN).equals(ObjectFlags.NONE)) {
                            ObjTypes.MiscObjectData data = other.data.misc;
                            Map.MapTransition transition = new Map.MapTransition();
                            transition.map = data.map;
                            transition.tile = data.tile;
                            transition.elevation = data.elevation;
                            transition.rotation = org.fallout2ce.Rotation.fromInt(data.rotation.id());
                            Map.mapSetTransition(transition);
                            Map.wmMapMarkMapEntranceState(
                                    transition.map != null ? transition.map.id() : -1,
                                    transition.elevation, 1);
                        }
                    }
                }

                listNode = listNode.next;
            }

            // NOTE: Uninline (obj_set_seen).
            objSetSeen(tile);

            int roofX = tile % MapDefs.HEX_GRID_WIDTH / 2;
            int roofY = tile / MapDefs.HEX_GRID_WIDTH / 2;
            if (roofX != objLastRoofX || roofY != objLastRoofY || elevation != objLastRoofElev) {
                int currentSquare = Map.squareGrids()[elevation].tileFid[
                        roofX + MapDefs.SQUARE_GRID_WIDTH * roofY];
                int previousSquare = objLastRoofX != -1 && objLastRoofY != -1
                        ? Map.squareGrids()[elevation].tileFid[
                                objLastRoofX + MapDefs.SQUARE_GRID_WIDTH * objLastRoofY]
                        : 0;
                int currentRoofFrame = (currentSquare >> Map.ROOF_HALF_FID_SHIFT) & Map.HALF_FID_FRAME_MASK;
                int currentRoofFlags = ((currentSquare >> Map.ROOF_HALF_FID_SHIFT) & Map.HALF_FID_FLAGS_MASK)
                        >> Map.HALF_FID_FLAGS_POSITION;
                int previousRoofFlags = ((previousSquare >> Map.ROOF_HALF_FID_SHIFT) & Map.HALF_FID_FLAGS_MASK)
                        >> Map.HALF_FID_FLAGS_POSITION;
                boolean isEmpty = currentRoofFrame == ArtDefs.TileFrameId.Grid.id();
                if (isEmpty != objLastIsEmpty || currentRoofFlags != previousRoofFlags) {
                    // tile_fill_roof(last/current, elevation, true/false):
                    // roof render (game phase, deferred, see class notes).
                    // The vectors never reach it (empty squares).
                }

                objLastRoofX = roofX;
                objLastRoofY = roofY;
                objLastRoofElev = elevation;
                objLastIsEmpty = isEmpty;
            }

            objectSetLocation(egg, tile, elevation);

            if (elevation != oldElevation) {
                // SFALL: Remove text floaters after moving to another elevation.
                TextObjects.textObjectsReset();

                Map.mapSetElevation(elevation);
                TileMath.tileSetCenter(tile, TileMath.TILE_SET_CENTER_REFRESH_WINDOW
                        | TileMath.TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS);
                // _game_user_wants_to_quit = GAME_QUIT_REQUEST_END_COMBAT:
                // combat phase, deferred (see class notes).
            }
        } else {
            if (elevation != objLastRoofElev
                    && ProtoTypes.ProtoId.ofGameObject(obj).objectType()
                            == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
                // _combat_delete_critter(obj): combat engine (game phase,
                // deferred, see class notes).
            }
        }

        return 0;
    }

    // Sets object fid (0x48AA3C obj_change_fid, rect overload deferred:
    // the dirty-rect union needs objectGetRect/render engine, game
    // phase; the slice passes a rect only from the itemDropAll equipped
    // tail, which the vectors never reach).
    public static int objectSetFrmId(GameObject obj, FrmId frmId) {
        if (obj == null) {
            return -1;
        }

        if (frmId.valid()) {
            if (!frmId.hasFid()) {
                throw new AssertionError("objectSetFrmId called with path based"
                        + " FrmId which is not supported!");
            }
        }

        obj.fid = frmId.fid();

        return 0;
    }

    // Sets object frame (0x48AA84 obj_set_frame, null-rect shape only:
    // the slice always passes null, so the objectGetRect/rectUnion tail
    // is dropped, like the other setters). Null -> -1, art-lock miss ->
    // -1, frame >= frameCount -> -1 (no lower-bound gate, like C++);
    // else store. Fid untouched.
    public static int objectSetFrame(GameObject obj, int frame) {
        if (obj == null) {
            return -1;
        }

        int framesPerDirection;
        try (Art.ArtLock lock = Art.artLock(FrmId.ofFid(obj.fid))) {
            if (lock == null || lock.art() == null) {
                return -1;
            }

            framesPerDirection = Art.artGetFrameCount(lock.art());
        }

        if (frame >= framesPerDirection) {
            return -1;
        }

        obj.frame = frame;

        return 0;
    }

    // 0x48AAF0 obj_inc_frame (null-rect shape only, like objectSetFrame).
    // Null -> -1, art-lock miss -> -1, else frame + 1 wrapping
    // (count -> 0).
    public static int objectSetNextFrame(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        int framesPerDirection;
        try (Art.ArtLock lock = Art.artLock(FrmId.ofFid(obj.fid))) {
            if (lock == null || lock.art() == null) {
                return -1;
            }

            framesPerDirection = Art.artGetFrameCount(lock.art());
        }

        int nextFrame = obj.frame + 1;
        if (nextFrame >= framesPerDirection) {
            nextFrame = 0;
        }

        obj.frame = nextFrame;

        return 0;
    }

    // 0x48AB60 obj_dec_frame (null-rect shape only, like objectSetFrame).
    // Null -> -1, art-lock miss -> -1, else frame - 1 wrapping
    // (< 0 -> count - 1).
    public static int objectSetPrevFrame(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        int framesPerDirection;
        try (Art.ArtLock lock = Art.artLock(FrmId.ofFid(obj.fid))) {
            if (lock == null || lock.art() == null) {
                return -1;
            }

            framesPerDirection = Art.artGetFrameCount(lock.art());
        }

        int prevFrame = obj.frame - 1;
        if (prevFrame < 0) {
            prevFrame = framesPerDirection - 1;
        }

        obj.frame = prevFrame;

        return 0;
    }

    // 0x48B0FC obj_erase_object (rect overload deferred: the slice always
    // passes null, so the rect is dropped; the light call keeps its
    // null-rect shape: the adjust result is ignored, like C++).
    public static int objectDestroy(GameObject object) {
        if (object == null) {
            return -1;
        }

        removeItemOutline(object);

        GameObjectListNode[] prevSlot = new GameObjectListNode[1];
        GameObjectListNode node = findListNode(object, prevSlot);
        if (node != null) {
            // _obj_adjust_light(object, 1, rect): with a null rect C++
            // ignores the -1, so the guards-only slice runs bare.
            adjustLight(object, true);

            if (removeNode(node, prevSlot[0]) != 0) {
                return -1;
            }

            return 0;
        }

        // Non-listed object: C++ builds a throwaway node and removes
        // (node, node), so no unlink runs; only the inventory free, the
        // sid/AI tails and the deallocation observe. The Java temp node
        // mirrors that (deallocation itself is the GC, see removeNode).
        GameObjectListNode temp = new GameObjectListNode();
        temp.obj = object;
        temp.next = null;

        if (removeNode(temp, temp) != 0) {
            return -1;
        }

        return 0;
    }

    // src/object.h objectHasOutline: (outline & OUTLINE_TYPE_MAX) !=
    // OUTLINE_TYPE_NONE. Null-safe (C checks obj != nullptr first).
    public static boolean objectHasOutline(GameObject obj) {
        if (obj == null) {
            return false;
        }

        return (obj.outline.bits() & OutlineType.MAX.bits()) != OutlineType.NONE.bits();
    }

    // 0x48C2E4 obj_set_outline (null-rect shape only: the slice always
    // passes null, so the objectGetRect tail is dropped, like the
    // objectDestroy/objectShow seam). Null -> -1, already outlined -> -1,
    // NO_HIGHLIGHT -> -1, else store; HIDDEN adds OUTLINE_DISABLED.
    public static int objectSetOutline(GameObject obj, OutlineType outlineType) {
        if (obj == null) {
            return -1;
        }

        if (objectHasOutline(obj)) {
            return -1;
        }

        if (!obj.flags.and(ObjectFlags.NO_HIGHLIGHT).equals(ObjectFlags.NONE)) {
            return -1;
        }

        obj.outline = outlineType;

        if (!obj.flags.and(ObjectFlags.HIDDEN).equals(ObjectFlags.NONE)) {
            obj.outline = obj.outline.or(ObjTypes.OUTLINE_DISABLED);
        }

        return 0;
    }

    // 0x48C2F0 obj_remove_outline (null-rect shape only, like
    // objectSetOutline). Null -> -1, else clear to NONE.
    public static int objectClearOutline(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        obj.outline = OutlineType.NONE;

        return 0;
    }

    // 0x48AEE4 obj_turn_on_outline (null-rect shape only: the slice
    // always passes null, so the objectGetRect tail is dropped, like
    // objectSetOutline). Null -> -1, else clear OUTLINE_DISABLED.
    public static int objectEnableOutline(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        obj.outline = obj.outline.and(~ObjTypes.OUTLINE_DISABLED);

        return 0;
    }

    // 0x48AF00 obj_turn_off_outline (null-rect shape only, like
    // objectEnableOutline). Null -> -1, else set OUTLINE_DISABLED iff
    // outlined.
    public static int objectDisableOutline(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        if (objectHasOutline(obj)) {
            obj.outline = obj.outline.or(ObjTypes.OUTLINE_DISABLED);
        }

        return 0;
    }

    // 0x48A1FC obj_offset (null-rect shape only: the slice always passes
    // null, so the rect fill/union tails are dropped, like objectDestroy).
    // Null -> -1, list-node miss -> -1, else unlink, x/sx += x, y/sy += y,
    // re-insert. The gDude/gEgg branch is dormant headless (unported
    // game-phase globals, null here), so every object takes the plain
    // path, exactly like C++ for non-dude objects.
    public static int objOffset(GameObject obj, int x, int y) {
        if (obj == null) {
            return -1;
        }

        GameObjectListNode[] prevSlot = new GameObjectListNode[1];
        GameObjectListNode node = findListNode(obj, prevSlot);
        if (node == null) {
            return -1;
        }

        unlinkNode(node, prevSlot[0]);

        obj.x += x;
        obj.sx += x;

        obj.y += y;
        obj.sy += y;

        insertNode(node);

        // Dude tail (rect unions need the render engine, deferred like the
        // other rect seams): the egg follows the same offset, which keeps
        // the tile-list order 1:1 (the vectors observe it).
        if (obj == dude) {
            objOffset(egg, x, y);
        }

        return 0;
    }

    // 0x48ADF0 obj_turn_on (rect overload deferred: the slice always passes
    // null, so the rect fill is dropped and the adjust-light result is
    // ignored, like C++).
    public static int objectShow(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        if (obj.flags.and(ObjectFlags.HIDDEN).equals(ObjectFlags.NONE)) {
            return -1;
        }

        obj.flags = obj.flags.and(ObjectFlags.HIDDEN.not());
        obj.outline = obj.outline.and(~ObjTypes.OUTLINE_DISABLED);

        // _obj_adjust_light(obj, 0, null): with a null rect C++ ignores the
        // -1, so the guards-only slice runs bare.
        adjustLight(obj, false);

        // obj == gDude tail (src/object.cc): rect-only (egg rect union for
        // the refresh), deferred with the other rect seams (the vectors
        // never observe it).

        return 0;
    }

    // 0x48AE68 obj_turn_off (rect overload deferred, like objectShow).
    public static int objectHide(GameObject object) {
        if (object == null) {
            return -1;
        }

        if (!object.flags.and(ObjectFlags.HIDDEN).equals(ObjectFlags.NONE)) {
            return -1;
        }

        // _obj_adjust_light(object, 1, null): null-rect shape, result
        // ignored, like C++.
        adjustLight(object, true);

        object.flags = object.flags.or(ObjectFlags.HIDDEN);

        if (objectHasOutline(object)) {
            object.outline = object.outline.or(ObjTypes.OUTLINE_DISABLED);
        }

        // obj == gDude tail (rect-only egg union): deferred, like objectShow.

        return 0;
    }

    // 0x489F34 obj_disconnect (rect overload deferred, like objectDestroy:
    // the slice always passes null, so the rect fill is dropped and the
    // adjust-light result is ignored, like C++). Frees the list NODE, not
    // the object (GC owns it; only the unlink observes), then sets
    // tile = -1. See the class-notes double-list quirk: the first call
    // unlinks the TILE node, the second finds the leftover FLOATING node
    // (tile already -1 scans the floating head) and also returns 0.
    public static int objectDisconnect(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        GameObjectListNode[] prevSlot = new GameObjectListNode[1];
        GameObjectListNode node = findListNode(obj, prevSlot);
        if (node == null) {
            return -1;
        }

        // _obj_adjust_light(obj, 1, null): null-rect shape, result ignored,
        // like C++.
        adjustLight(obj, true);

        // Head follows the node's pre-disconnect tile (tile is still set),
        // exactly like C; tile = -1 lands after the unlink.
        unlinkNode(node, prevSlot[0]);

        obj.tile = -1;

        return 0;
    }

    // objectGetOwner (src/object.cc): the ultimate owner up the owner
    // chain; a directly owned object returns its owner, an unowned one
    // null. Like C++, a null object is not guarded (it dereferences).
    public static GameObject objectGetOwner(GameObject object) {
        GameObject owner = object.owner;
        if (owner == null) {
            return null;
        }

        while (owner.owner != null) {
            owner = owner.owner;
        }

        return owner;
    }

    // objectSetRotation (src/object.cc, 0x48ABD4): null object -> -1,
    // invalid rotation -> -1, else store and return 0. The C++ dirty-rect
    // overload (objectGetRect + rectUnion into the caller rect) needs the
    // render engine (objectGetRect, game phase); the vectors always pass
    // null, so this slice is the plain field set.
    public static int objectSetRotation(GameObject obj, int rotation) {
        if (obj == null) {
            return -1;
        }

        if (!ObjTypes.rotationIsValid(rotation)) {
            return -1;
        }

        obj.rotation = ObjTypes.ofRotation(rotation);
        return 0;
    }

    // Rotation-enum overload (delegates to the int form, like the C++
    // Rotation parameter).
    public static int objectSetRotation(GameObject obj, Rotation rotation) {
        if (obj == null) {
            return -1;
        }

        if (rotation == null) {
            return -1;
        }

        return objectSetRotation(obj, rotation.id());
    }

    // 0x48AC20 obj_inc_rotation: +1, wrapping COUNT -> NE. C++ dereferences
    // obj before the set call; Java guards null -> -1 (the set path still
    // returns -1 for null, like C++ objectSetRotation).
    public static int objectRotateClockwise(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        int rotation = obj.rotation.id() + 1;
        if (rotation >= ObjTypes.ROTATION_COUNT) {
            rotation = ObjTypes.ROTATION_FIRST;
        }

        return objectSetRotation(obj, rotation);
    }

    // 0x48AC38 obj_dec_rotation: -1, wrapping <0 -> NW (null guard as above).
    public static int objectRotateCounterClockwise(GameObject obj) {
        if (obj == null) {
            return -1;
        }

        int rotation = obj.rotation.id() - 1;
        if (rotation < 0) {
            rotation = ObjTypes.ROTATION_LAST;
        }

        return objectSetRotation(obj, rotation);
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

    // 0x48D8E8 obj_insert: tile -1 prepends to the floating head, like
    // C; tile-bound nodes walk the tile list with the C
    // elevation/flat ordering (the art-lock depth tiebreak needs the
    // art lock stack and stays a seam, game phase; see class notes).
    private static void insertNode(GameObjectListNode node) {
        if (node == null) {
            return;
        }

        if (node.obj.tile == -1 || !MapDefs.hexGridTileIsValid(node.obj.tile)) {
            node.next = head;
            head = node;
            return;
        }

        GameObjectListNode prev = null;
        GameObjectListNode current = tileHeads[node.obj.tile];
        while (current != null) {
            GameObject obj = current.obj;
            if (obj.elevation > node.obj.elevation) {
                break;
            }

            if (obj.elevation == node.obj.elevation) {
                boolean objFlat = (obj.flags.bits() & ObjectFlags.FLAT.bits()) == 0;
                boolean newFlat =
                        (node.obj.flags.bits() & ObjectFlags.FLAT.bits()) != 0;
                if (objFlat && newFlat) {
                    break;
                }

                // Equal elevation + equal flatness: the C art-lock depth
                // tiebreak (artLock both fids) needs the art lock stack
                // (game phase), so arrival order is kept here.
            }

            prev = current;
            current = current.next;
        }

        node.next = current;
        if (prev != null) {
            prev.next = node;
        } else {
            tileHeads[node.obj.tile] = node;
        }
    }

    // Floating-head + tile-array slice of objectGetListNode
    // (src/object.cc): the head follows the object's tile, exactly like
    // C (tile -1 scans the floating head, otherwise the tile list). The
    // previous node rides in prevPtr[0] (null when the node is the head),
    // like the C++ out-param.
    private static GameObjectListNode findListNode(GameObject object, GameObjectListNode[] prevPtr) {
        GameObjectListNode prev = null;
        GameObjectListNode node;
        if (object.tile != -1 && MapDefs.hexGridTileIsValid(object.tile)) {
            node = tileHeads[object.tile];
        } else if (object.tile == -1) {
            node = head;
        } else {
            node = null;
        }
        while (node != null) {
            if (node.obj == object) {
                break;
            }

            prev = node;
            node = node.next;
        }

        if (prevPtr != null && prevPtr.length > 0) {
            prevPtr[0] = prev;
        }

        return node;
    }

    // 0x48DA58 obj_remove: null object or NO_REMOVE -> -1; otherwise free
    // the inventory contents, run the sid destroy proc + scriptRemove and
    // aiRemoveBurstDisabled, unlink (a1 == a2 skips the unlink, which is
    // the non-listed path), and deallocate. The queue is untouched (C++
    // never calls queueRemoveEvents here); deallocation is the GC, so only
    // the unlink observes.
    private static int removeNode(GameObjectListNode node, GameObjectListNode prev) {
        if (node.obj == null) {
            return -1;
        }

        if (!node.obj.flags.and(ObjectFlags.NO_REMOVE).equals(ObjectFlags.NONE)) {
            return -1;
        }

        freeInventory(node.obj);

        if (node.obj.sid != -1) {
            // scriptExecProc(sid, SCRIPT_PROC_DESTROY) + scriptRemove(sid):
            // script engine (game phase); harness sids are -1.
        }

        // aiRemoveBurstDisabled(obj): AI engine (game phase).

        if (node != prev) {
            unlinkNode(node, prev);
        }

        return 0;
    }

    // Head unlink shared by _obj_remove and _obj_toggle_flat: the head
    // follows the node's tile, exactly like C (tile -1 advances the
    // floating head, otherwise the tile list).
    private static void unlinkNode(GameObjectListNode node, GameObjectListNode prev) {
        if (prev != null) {
            prev.next = node.next;
        } else {
            int tile = node.obj.tile;
            if (tile != -1 && MapDefs.hexGridTileIsValid(tile)) {
                tileHeads[tile] = node.next;
            } else {
                head = node.next;
            }
        }
    }

    // 0x48DB28 obj_connect_to_tile (rect overload deferred like
    // objectConnect: the slice always passes null, so the adjust-light
    // result is ignored and no rect fill runs, like C++).
    private static int connectToTile(GameObjectListNode node, int tile, int elevation) {
        if (node == null) {
            return -1;
        }

        if (!MapDefs.hexGridTileIsValid(tile)) {
            return -1;
        }

        if (!MapDefs.elevationIsValid(elevation)) {
            return -1;
        }

        node.obj.tile = tile;
        node.obj.elevation = elevation;
        node.obj.x = 0;
        node.obj.y = 0;
        node.obj.owner = null;

        insertNode(node);

        // _obj_adjust_light(obj, 0, null): with a null rect C++ ignores
        // the -1, so the guards-only slice runs bare (vector protos
        // carry light 0,0 and bail at the intensity guard).
        adjustLight(node.obj, false);

        return 0;
    }

    // 0x48B1B0 obj_inven_free: destroy every member (NO_REMOVE cleared
    // first, like C++) and empty the stack list. Members are not list
    // nodes, so their removal takes the (node, node) path: no unlink,
    // recursive member free + sid/AI tails, then the GC.
    private static int freeInventory(GameObject owner) {
        List<InventoryItem> items = owner.data.inventory.items;
        for (int index = 0; index < items.size(); index++) {
            InventoryItem entry = items.get(index);

            GameObjectListNode node = new GameObjectListNode();
            node.obj = entry.item;
            node.next = null;

            node.obj.flags = node.obj.flags.and(ObjectFlags.NO_REMOVE.not());
            removeNode(node, node);

            entry.item = null;
        }

        items.clear();

        return 0;
    }

    // _gmouse_remove_item_outline seam (src/game_mouse.cc): clears the
    // outline when the destroyed object is the highlighted item
    // (gGameMouseHighlightedItem) + tile refresh. The highlight state is
    // game-mouse engine (game phase, null headless); headless this is a
    // no-op.
    private static void removeItemOutline(GameObject object) {
    }

    private static boolean isFindHidden(GameObject obj) {
        if (obj == null) {
            return true;
        }
        return Art.artIsObjectTypeHidden(FrmId.ofFid(obj.fid).objectType().value()) != 0;
    }

    // 0x48AC54 _obj_rebuild_all_light: resets tile intensity, then
    // re-applies each tile-placed object via _obj_adjust_light(obj, 0,
    // null). The guards-only adjustLight slice applies nothing for
    // light-0 objects (they bail at the intensity guard), so headless
    // this is just the reset; the tileHeads walk itself is faithful.
    public static void objRebuildAllLight() {
        Light.lightResetTileIntensity();

        for (int tile = 0; tile < MapDefs.HEX_GRID_SIZE; tile++) {
            GameObjectListNode node = tileHeads[tile];
            while (node != null) {
                adjustLight(node.obj, false);
                node = node.next;
            }
        }
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

    // 0x488780 obj_init.
    public static int objectsInit(byte[] buf, int width, int height, int pitch) {
        Arrays.fill(objSeen, (byte) 0);

        updateAreaPixelBounds.right = width + 320;
        updateAreaPixelBounds.left = -320;
        updateAreaPixelBounds.bottom = height + 240;
        updateAreaPixelBounds.top = -240;

        updateAreaHexWidth = (updateAreaPixelBounds.right + 320 + 1) / 32 + 1;
        updateAreaHexHeight = (updateAreaPixelBounds.bottom + 240 + 1) / 12 + 1;
        updateAreaHexSize = updateAreaHexWidth * updateAreaHexHeight;

        tileHeads = new GameObjectListNode[MapDefs.HEX_GRID_SIZE];

        if (offsetTableInit() == -1) {
            return -1;
        }

        if (orderTableInit() == -1) {
            gotoErr();
            return -1;
        }

        if (renderTableInit() == -1) {
            gotoErr2();
            return -1;
        }

        if (Light.lightInit() == -1) {
            gotoErr2();
            return -1;
        }

        if (TextObjects.textObjectsInit(buf, width, height) == -1) {
            gotoErr2();
            return -1;
        }

        lightTableInit();
        blendTableInit();

        centerToUpperLeft = TileMath.tileFromScreenXY(
                updateAreaPixelBounds.left, updateAreaPixelBounds.top) - TileMath.gCenterTile;
        windowWidth = width;
        windowHeight = height;
        windowBuffer = buf;

        windowRect.left = 0;
        windowRect.top = 0;
        windowRect.right = width - 1;
        windowRect.bottom = height - 1;

        windowBufferSize = height * width;
        windowPitch = pitch;

        FrmId dudeFrmId = FrmId.ofCritter(Art.artLists.vaultGuyNum,
                AnimationType.STAND.value(), WeaponAnimation.NONE.value(),
                Rotation.ROTATION_NE.id());
        GameObject[] dudeSlot = new GameObject[1];
        objectCreateWithFrmIdProtoId(dudeSlot,
                dudeFrmId, ProtoId.ofCritterType(CritterProtoTypeId.Dude));
        dude = dudeSlot[0];

        dude.flags = dude.flags.or(ObjectFlags.NO_REMOVE).or(ObjectFlags.NO_SAVE)
                .or(ObjectFlags.HIDDEN).or(ObjectFlags.LIGHT_THRU);
        objectSetLight(dude, 4, 0x10000);

        if (PartyMembers.partyMemberAdd(dude) == -1) {
            Debug.print("\n  Error: Can't add Player into party!");
            throw new IllegalStateException("Can't add Player into party!");
        }

        FrmId eggFrmId = FrmId.ofType(ObjectType.INTERFACE, ArtDefs.InterfaceFrameId.Egg.id());
        GameObject[] eggSlot = new GameObject[1];
        objectCreateWithFrmIdProtoId(eggSlot, eggFrmId, ProtoId.empty());
        egg = eggSlot[0];

        egg.flags = egg.flags.or(ObjectFlags.NO_REMOVE).or(ObjectFlags.NO_SAVE)
                .or(ObjectFlags.HIDDEN).or(ObjectFlags.LIGHT_THRU);

        objectsInitialized = true;

        return 0;
    }

    // C `goto err_2` target: order + offset exits.
    private static void gotoErr2() {
        orderTableExit();
        gotoErr();
    }

    // C `goto err` target: offset exit.
    private static void gotoErr() {
        offsetTableExit();
    }

    // 0x488A00 obj_reset.
    public static void objectsReset() {
        if (objectsInitialized) {
            TextObjects.textObjectsReset();
            removeAllObjects();
            Arrays.fill(objSeen, (byte) 0);
            Light.lightReset();
        }
    }

    // 0x488A30 obj_exit (leaves objectsInitialized set, like C; the
    // process-teardown flag is only cleared by resetForTests).
    public static void objectsExit() {
        if (objectsInitialized) {
            dude.flags = dude.flags.and(ObjectFlags.NO_REMOVE.not());
            egg.flags = egg.flags.and(ObjectFlags.NO_REMOVE.not());

            removeAllObjects();
            TextObjects.textObjectsFree();

            // NOTE: Uninline.
            blendTableExit();

            Light.lightExit();

            // NOTE: Uninline.
            renderTableExit();

            // NOTE: Uninline.
            orderTableExit();

            offsetTableExit();
        }
    }

    /** Port of {@code gDude} (0x6610B8). */
    public static GameObject getDude() {
        return dude;
    }

    /** Port of {@code gEgg} (0x6610AC). */
    public static GameObject getEgg() {
        return egg;
    }

    // 0x48CDA0 (approx) _obj_offset_table_init: the two malloc-failure
    // exits collapse (Java allocation cannot fail); the tile == -1 and
    // tileSetCenter failures stay.
    private static int offsetTableInit() {
        if (offsetTableEven != null || offsetTableOdd != null) {
            return -1;
        }

        offsetTableEven = new int[updateAreaHexSize];
        offsetTableOdd = new int[updateAreaHexSize];

        for (int parity = 0; parity < 2; parity++) {
            int originTile = TileMath.tileFromScreenXY(
                    updateAreaPixelBounds.left, updateAreaPixelBounds.top);
            if (originTile != -1) {
                int[] offsets = (TileMath.gCenterTile & 1) != 0 ? offsetTableOdd : offsetTableEven;

                int[] originTileX = new int[1];
                int[] originTileY = new int[1];
                TileMath.tileToScreenXY(originTile, originTileX, originTileY);

                int parityShift = 16;
                int tileX = originTileX[0] + 16;
                int tileY = originTileY[0] + 8;
                if (tileX > updateAreaPixelBounds.left) {
                    parityShift = -parityShift;
                }

                int o = 0;
                int cursorX = tileX;
                for (int y = 0; y < updateAreaHexHeight; y++) {
                    for (int x = 0; x < updateAreaHexWidth; x++) {
                        int tile = TileMath.tileFromScreenXY(cursorX, tileY);
                        if (tile == -1) {
                            offsetTableExit();
                            return -1;
                        }

                        cursorX += 32;
                        offsets[o++] = tile - originTile;
                    }

                    cursorX = parityShift + tileX;
                    tileY += 12;
                    parityShift = -parityShift;
                }
            }

            if (TileMath.tileSetCenter(TileMath.gCenterTile + 1,
                    TileMath.TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS) == -1) {
                offsetTableExit();
                return -1;
            }
        }

        offsetDivTable = new int[updateAreaHexSize];
        for (int i = 0; i < updateAreaHexSize; i++) {
            offsetDivTable[i] = i / updateAreaHexWidth;
        }

        offsetModTable = new int[updateAreaHexSize];
        for (int i = 0; i < updateAreaHexSize; i++) {
            offsetModTable[i] = i % updateAreaHexWidth;
        }

        return 0;
    }

    // 0x48CDA0 obj_offset_table_exit.
    private static void offsetTableExit() {
        offsetModTable = null;
        offsetDivTable = null;
        offsetTableOdd = null;
        offsetTableEven = null;
    }

    // 0x48CE10 obj_order_table_init. C sorts with qsort; Java sorts
    // stable, but the keys (distinct tile offsets) are a total order, so
    // the permutation is identical.
    private static int orderTableInit() {
        if (orderTableEven != null || orderTableOdd != null) {
            return -1;
        }

        orderTableEven = new int[updateAreaHexSize];
        orderTableOdd = new int[updateAreaHexSize];
        for (int index = 0; index < updateAreaHexSize; index++) {
            orderTableEven[index] = index;
            orderTableOdd[index] = index;
        }

        sortOrderTable(orderTableEven, offsetTableEven);
        sortOrderTable(orderTableOdd, offsetTableOdd);

        return 0;
    }

    private static void sortOrderTable(int[] order, int[] offsets) {
        Integer[] boxed = new Integer[order.length];
        for (int i = 0; i < order.length; i++) {
            boxed[i] = order[i];
        }
        Arrays.sort(boxed, (a, b) -> Integer.compare(offsets[a], offsets[b]));
        for (int i = 0; i < order.length; i++) {
            order[i] = boxed[i];
        }
    }

    // 0x48CF50 obj_order_table_exit (NOTE: inlined upstream).
    private static void orderTableExit() {
        orderTableOdd = null;
        orderTableEven = null;
    }

    // 0x48CF8C obj_render_table_init.
    private static int renderTableInit() {
        if (renderTable != null) {
            return -1;
        }

        renderTable = new GameObjectListNode[updateAreaHexSize];

        return 0;
    }

    // 0x48D000 obj_render_table_exit (NOTE: inlined upstream).
    private static void renderTableExit() {
        renderTable = null;
    }

    // 0x48D020 obj_light_table_init.
    private static void lightTableInit() {
        for (int s = 0; s < 2; s++) {
            int v4 = TileMath.gCenterTile + s;
            for (int r = ObjTypes.ROTATION_FIRST; r < ObjTypes.ROTATION_COUNT; r++) {
                org.fallout2ce.Rotation rotation = org.fallout2ce.Rotation.fromInt(r);
                int v15 = 8;
                int p = 0;
                for (int j = 0; j < 8; j++) {
                    int tile = TileMath.tileGetTileInDirection(v4,
                            org.fallout2ce.Rotation.fromInt((r + 1) % ObjTypes.ROTATION_COUNT), j);
                    for (int m = 0; m < v15; m++) {
                        lightOffsets[v4 & 1][r][p++] =
                                TileMath.tileGetTileInDirection(tile, rotation, m + 1) - v4;
                    }
                    v15--;
                }
            }
        }
    }

    // 0x48D1E4 obj_blend_table_init (colors via the runtime
    // colorTable[rgb555()] lookups, like the C macros).
    private static void blendTableInit() {
        for (int index = Color.COLOR_FIRST; index < Color.COLOR_COUNT; index++) {
            int rgb = Color.color2RGB(index & Color.COLOR_LAST);
            int r = (rgb & 0x7C00) >> 10;
            int g = (rgb & 0x3E0) >> 5;
            int b = rgb & 0x1F;
            glassGrayTable[index] = ((r + 5 * g + 4 * b) / 10) >> 2;
            commonGrayTable[index] = ((b + 3 * r + 6 * g) / 10) >> 2;
        }

        glassGrayTable[0] = Color.COLOR_FIRST;
        commonGrayTable[0] = Color.COLOR_FIRST;

        wallBlendTable = Color.getColorBlendTable(
                Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0xC5D6FF)]));
        glassBlendTable = Color.getColorBlendTable(
                Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0x4AFFFF)]));
        steamBlendTable = Color.getColorBlendTable(
                Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0xFFFFFF)]));
        energyBlendTable = Color.getColorBlendTable(
                Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0xEFFF08)]));
        redBlendTable = Color.getColorBlendTable(
                Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0xFF0000)]));
    }

    // 0x48D2E8 obj_blend_table_exit (NOTE: inlined upstream).
    private static void blendTableExit() {
        Color.freeColorBlendTable(Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0xC5D6FF)]));
        Color.freeColorBlendTable(Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0x4AFFFF)]));
        Color.freeColorBlendTable(Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0xFFFFFF)]));
        Color.freeColorBlendTable(Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0xEFFF08)]));
        Color.freeColorBlendTable(Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0xFF0000)]));
        wallBlendTable = null;
        glassBlendTable = null;
        steamBlendTable = null;
        energyBlendTable = null;
        redBlendTable = null;
    }

    // 0x... _obj_remove_all (src/object.cc): tile heads then floating
    // head; NO_REMOVE entries stay (prev advances, like C). The
    // _scr_remove_all script tail and the roof-cache stores are game
    // phase (script/render engines, unported).
    private static void removeAllObjects() {
        for (int tile = 0; tile < MapDefs.HEX_GRID_SIZE; tile++) {
            GameObjectListNode node = tileHeads[tile];
            GameObjectListNode prev = null;
            while (node != null) {
                GameObjectListNode next = node.next;
                if (removeNode(node, prev) == -1) {
                    prev = node;
                }
                node = next;
            }
        }

        GameObjectListNode node = head;
        GameObjectListNode prev = null;
        while (node != null) {
            GameObjectListNode next = node.next;
            if (removeNode(node, prev) == -1) {
                prev = node;
            }
            node = next;
        }

        objLastRoofY = -1;
        objLastRoofElev = -1;
        objLastIsEmpty = true;
        objLastRoofX = -1;
    }

    /** Test seam: clear the lists + find cursor state + counter (fresh-process mirror). */
    static void resetForTests() {
        head = null;
        tileHeads = new GameObjectListNode[MapDefs.HEX_GRID_SIZE];
        findCursor = null;
        findTile = 0;
        findElevation = 0;
        objectIdCounter = 4;
        protosInitialized = false;

        objectsInitialized = false;
        updateAreaHexWidth = 0;
        updateAreaHexHeight = 0;
        updateAreaHexSize = 0;
        updateAreaPixelBounds = new Geometry.Rect();
        orderTableEven = null;
        orderTableOdd = null;
        offsetTableEven = null;
        offsetTableOdd = null;
        offsetDivTable = null;
        offsetModTable = null;
        renderTable = null;
        lightOffsets = new int[2][ObjTypes.ROTATION_COUNT][36];
        windowRect = new Geometry.Rect();
        glassGrayTable = new int[Color.COLOR_COUNT];
        commonGrayTable = new int[Color.COLOR_COUNT];
        windowBufferSize = 0;
        windowBuffer = null;
        windowHeight = 0;
        windowWidth = 0;
        windowPitch = 0;
        dude = null;
        egg = null;
        objSeenCheck = new byte[OBJ_SEEN_SIZE];
        objSeen = new byte[OBJ_SEEN_SIZE];
        objLastRoofX = -1;
        objLastRoofY = -1;
        objLastRoofElev = -1;
        objLastIsEmpty = true;
        centerToUpperLeft = 0;
        wallBlendTable = null;
        glassBlendTable = null;
        steamBlendTable = null;
        energyBlendTable = null;
        redBlendTable = null;
    }
}
