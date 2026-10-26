package org.fallout2ce;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Animation state slice, mirroring {@code src/animation.cc}: {@code _dude_stand}
 * ({@code dude_stand}, 0x4185D0), the queue seams {@code animationStop}
 * ({@code 0x4186E4}) and {@code reg_anim_clear}, and the pathfinder
 * ({@code _make_path} 0x415EE8 / {@code pathfinderFindPath} 0x415EFC with
 * {@code canUseDoor}).
 *
 * <p>Vectors: {@code [mapnewmap]} in
 * {@code docker/refharness/golden/v98.txt}, produced by
 * {@code sectionMapChild} in {@code docker/refharness/ref_main.cc}. The slice
 * is exercised through {@link Map#mapNewMap} (via {@code mapSetElevation} and
 * {@code _map_place_dude_and_mouse}): with the harness setup the dude fid is
 * already {@code STAND} and the weapon animation is {@code None}, so the
 * take-out/stand offset branch is skipped; the frame store, fid store,
 * location re-connect (dude already placed / not yet placed), zero offset
 * and the refresh tail run for real.
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>Refresh tail ({@code tileWindowRefreshRect}): render engine (game
 * phase); the call is kept with a null rect, like the other deferred rect
 * overloads in {@link ObjectSystem}.</li>
 * <li>{@code animationStop} / {@code regAnimClear} drain the animation
 * request queue ({@code gAnimationSequences}); the queue module is unported,
 * so both are documented no-ops. The harness vectors never have pending
 * animations at these points (fresh child, queue empty).</li>
 * </ul>
 */
public final class Animation {

    private Animation() {
    }

    /**
     * Port of {@code _dude_stand} (0x4185D0): rotation store, take-out/stand
     * frame-offset accumulation when the fid carries a weapon animation (with
     * the fid rebuild for the non-{@code STAND}/{@code FIRE_DANCE} case),
     * fid store, location re-connect, frame 0 store, offset nudge and the
     * refresh tail (rect deferred, see class notes).
     */
    public static void dudeStand(GameObject obj, int rotation, FrmId fid) {
        if (obj == null) {
            return;
        }

        ObjectSystem.objectSetRotation(obj, rotation);

        int x = 0;
        int y = 0;

        int weaponAnimationCode = FrmId.ofFid(obj.fid).weaponAnimation();
        if (weaponAnimationCode != WeaponAnimation.NONE.value()) {
            if (!fid.valid()) {
                FrmId takeOutFrmId = FrmId.ofCritter(FrmId.ofFid(obj.fid).frameId(),
                        AnimationType.TAKE_OUT.value(), weaponAnimationCode, obj.rotation.id() + 1);
                try (Art.ArtLock takeOutLock = Art.artLock(takeOutFrmId)) {
                    if (takeOutLock != null && takeOutLock.art() != null) {
                        int frameCount = Art.artGetFrameCount(takeOutLock.art());
                        for (int frame = 0; frame < frameCount; frame++) {
                            int[] offsetX = new int[1];
                            int[] offsetY = new int[1];
                            Art.artGetFrameOffsets(takeOutLock.art(), frame, obj.rotation.id(), offsetX, offsetY);
                            x += offsetX[0];
                            y += offsetY[0];
                        }
                    }
                }

                FrmId standFid = FrmId.ofCritter(FrmId.ofFid(obj.fid).frameId(),
                        AnimationType.STAND.value(), WeaponAnimation.NONE.value(), obj.rotation.id() + 1);
                try (Art.ArtLock standLock = Art.artLock(standFid)) {
                    if (standLock != null && standLock.art() != null) {
                        int[] offsetX = new int[1];
                        int[] offsetY = new int[1];
                        if (Art.artGetRotationOffsets(standLock.art(), obj.rotation.id(), offsetX, offsetY) == 0) {
                            x += offsetX[0];
                            y += offsetY[0];
                        }
                    }
                }
            }
        }

        FrmId finalFrmId = fid;
        if (!finalFrmId.valid()) {
            int anim;
            if (FrmId.ofFid(obj.fid).animationType() == AnimationType.FIRE_DANCE.value()) {
                anim = AnimationType.FIRE_DANCE.value();
            } else {
                anim = AnimationType.STAND.value();
            }
            finalFrmId = FrmId.ofCritter(FrmId.ofFid(obj.fid).frameId(), anim,
                    WeaponAnimation.NONE.value(), obj.rotation.id() + 1);
        }

        ObjectSystem.objectSetFrmId(obj, finalFrmId);
        ObjectSystem.objectSetLocation(obj, obj.tile, obj.elevation);
        ObjectSystem.objectSetFrame(obj, 0);
        ObjectSystem.objOffset(obj, x, y);
        TileMath.tileWindowRefreshRect(null, obj.elevation);
    }

    /**
     * Port of {@code animationStop} (0x4186E4) as a documented seam: the
     * animation request queue is game phase (unported), and the harness
     * vectors always stop an empty queue here.
     */
    public static void animationStop() {
    }

    /**
     * Port of {@code reg_anim_clear} as a documented seam: clears pending
     * requests owning {@code obj} from the animation queue (game phase,
     * unported); the vectors never have pending requests here.
     */
    public static void regAnimClear(GameObject obj) {
    }

    /**
     * Port of {@code PathBuilderCallback}: blocker lookup used by the
     * pathfinder ({@code _obj_blocking_at} in the movement code).
     */
    public interface PathBuilderCallback {
        GameObject at(GameObject exclude, int tile, int elevation);
    }

    // 0x66BE28 PathNode (src/animation.cc:285).
    private static final class PathNode {
        int tile;
        int from;
        Rotation rotation = Rotation.NE;
        int estimate;
        int cost;
    }

    // PATH_NODE_CAPACITY (src/animation.cc:368).
    private static final int PATH_NODE_CAPACITY = 10000;

    // BSS pathfinder scratch (src/animation.cc:371-380). Node lists are not
    // memset between calls - the sentinel {@code tile == -1} marks free slots,
    // exactly like C.
    private static final PathNode[] closedPathNodeList = new PathNode[PATH_NODE_CAPACITY];
    private static final PathNode[] openPathNodeList = new PathNode[PATH_NODE_CAPACITY];
    private static final byte[] pathfinderProcessedTiles = new byte[5000];

    static {
        for (int index = 0; index < PATH_NODE_CAPACITY; index++) {
            closedPathNodeList[index] = new PathNode();
            openPathNodeList[index] = new PathNode();
            openPathNodeList[index].tile = -1;
        }
    }

    // 0x41633C _idist: screen-space hex distance.
    private static int idist(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        if (dx < 0) {
            dx = -dx;
        }

        int dy = y2 - y1;
        if (dy < 0) {
            dy = -dy;
        }

        int dm = (dx <= dy) ? dx : dy;

        return dx + dy - (dm / 2);
    }

    // 0x416360 _tile_idistance.
    private static int tileIdistance(int tile1, int tile2) {
        int[] x1 = new int[1];
        int[] y1 = new int[1];
        TileMath.tileToScreenXY(tile1, x1, y1);

        int[] x2 = new int[1];
        int[] y2 = new int[1];
        TileMath.tileToScreenXY(tile2, x2, y2);

        return idist(x1[0], y1[0], x2[0], y2[0]);
    }

    // 0x415E24 canUseDoor (src/animation.cc): whether {@code door} may be
    // walked through on the way to the destination. The portal walk-through
    // check only applies to the dude.
    private static boolean canUseDoor(GameObject critter, GameObject door) {
        if (critter == ObjectSystem.getDude()) {
            if (!ObjectSystem.objPortalIsWalkThru(door)) {
                return false;
            }
        }

        if (FrmId.ofFid(critter.fid).objectType() != ObjectType.CRITTER) {
            return false;
        }

        if (FrmId.ofFid(door.fid).objectType() != ObjectType.SCENERY) {
            return false;
        }

        ProtoTypes.BodyType bodyType = Critter.critterGetBodyType(critter);
        if (bodyType != ProtoTypes.BodyType.BODY_TYPE_BIPED
                && bodyType != ProtoTypes.BodyType.BODY_TYPE_ROBOTIC) {
            return false;
        }

        ProtoTypes.Proto[] proto = new ProtoTypes.Proto[1];
        if (ProtoDb.protoGetProto(ProtoId.ofPid(door.pid), proto) == -1) {
            return false;
        }

        if (proto[0].scenery.type != ProtoTypes.SceneryType.SCENERY_TYPE_DOOR) {
            return false;
        }

        if (ProtoInstance.objectIsLocked(door)) {
            return false;
        }

        if (Critter.critterGetKillType(critter) == ProtoTypes.KillType.KILL_TYPE_GECKO) {
            return false;
        }

        return true;
    }

    // 0x415EE8 _make_path.
    public static int makePath(GameObject object, int from, int to, byte[] rotations,
            int requireEmptyDest) {
        return pathfinderFindPath(object, from, to, rotations, requireEmptyDest,
                ObjectSystem::objBlockingAt);
    }

    // 0x415EFC pathfinderFindPath: A* over the hex grid. The destination may
    // be retargeted to the first reachable neighbour when a multihex object
    // blocks it (SFALL center-tile fix); the reconstruction writes rotations
    // backwards and reverses them in place. Returns path length, or 0.
    public static int pathfinderFindPath(GameObject object, int from, int to,
            byte[] rotations, int requireEmptyDest, PathBuilderCallback callback) {
        if (requireEmptyDest != 0) {
            if (callback.at(object, to, object.elevation) != null) {
                return 0;
            }
        }

        boolean isCritter = false;
        ProtoTypes.KillType critterType = ProtoTypes.KillType.KILL_TYPE_INVALID;
        if (ProtoId.ofPid(object.pid).objectType() == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            isCritter = true;
            critterType = Critter.critterGetKillType(object);
        }

        boolean isNotInCombat = !Combat.isInCombat();

        java.util.Arrays.fill(pathfinderProcessedTiles, (byte) 0);

        pathfinderProcessedTiles[from / 8] |= (byte) (1 << (from & 7));

        PathNode initial = openPathNodeList[0];
        initial.tile = from;
        initial.from = -1;
        initial.rotation = Rotation.NE;
        initial.estimate = tileIdistance(from, to);
        initial.cost = 0;

        for (int index = 1; index < PATH_NODE_CAPACITY; index += 1) {
            openPathNodeList[index].tile = -1;
        }

        int[] toScreenX = new int[1];
        int[] toScreenY = new int[1];
        TileMath.tileToScreenXY(to, toScreenX, toScreenY);

        int closedPathNodeListLength = 0;
        int openPathNodeListLength = 1;
        PathNode temp = new PathNode();

        // Radioactive goo surcharge only applies to critter movers; the
        // checked range is SceneryProtoTypeId.FirstRadioactiveGoo..Last
        // (src/animation.cc kRadioactiveGooProtoIds).
        int gooPidBase = (ObjTypes.ObjectType.OBJ_TYPE_SCENERY.id() << 24)
                + ProtoTypes.SceneryProtoTypeId.FirstRadioactiveGoo.id();

        while (true) {
            int picked = -1;

            PathNode prev = null;
            int seen = 0;
            for (int index = 0; seen < openPathNodeListLength; index += 1) {
                PathNode curr = openPathNodeList[index];
                if (curr.tile != -1) {
                    seen++;
                    if (picked == -1
                            || (curr.estimate + curr.cost) < (prev.estimate + prev.cost)) {
                        prev = curr;
                        picked = index;
                    }
                }
            }

            PathNode curr = openPathNodeList[picked];

            temp.tile = curr.tile;
            temp.from = curr.from;
            temp.rotation = curr.rotation;
            temp.estimate = curr.estimate;
            temp.cost = curr.cost;

            openPathNodeListLength -= 1;

            curr.tile = -1;

            if (temp.tile == to) {
                if (openPathNodeListLength == 0) {
                    openPathNodeListLength = 1;
                }
                break;
            }

            PathNode curr1 = closedPathNodeList[closedPathNodeListLength];
            curr1.tile = temp.tile;
            curr1.from = temp.from;
            curr1.rotation = temp.rotation;
            curr1.estimate = temp.estimate;
            curr1.cost = temp.cost;

            closedPathNodeListLength += 1;

            if (closedPathNodeListLength == PATH_NODE_CAPACITY) {
                // Search path node capacity exhausted
                return 0;
            }

            for (Rotation rotation : Rotation.values()) {
                if (rotation == Rotation.INVALID) {
                    continue;
                }
                int tile = TileMath.tileGetTileInDirection(temp.tile, rotation, 1);
                if (tile < 0 || tile >= MapDefs.HEX_GRID_SIZE) {
                    continue;
                }

                int bit = 1 << (tile & 7);
                if ((pathfinderProcessedTiles[tile / 8] & bit) != 0) {
                    continue;
                }

                if (tile != to) {
                    GameObject blocker = callback.at(object, tile, object.elevation);
                    if (blocker != null) {
                        // SFALL: Fix pathing to the center tile of multihex
                        // objects. When the target object blocks an adjacent
                        // tile, use that adjacent tile as the practical path
                        // target.
                        if (blocker.tile == to) {
                            to = tile;
                            TileMath.tileToScreenXY(to, toScreenX, toScreenY);
                        } else if (!canUseDoor(object, blocker)) {
                            continue;
                        }
                    }
                }

                int slot = 0;
                for (; slot < PATH_NODE_CAPACITY; slot++) {
                    if (openPathNodeList[slot].tile == -1) {
                        break;
                    }
                }

                if (slot == PATH_NODE_CAPACITY) {
                    return 0;
                }

                openPathNodeListLength += 1;

                if (openPathNodeListLength == PATH_NODE_CAPACITY) {
                    return 0;
                }

                pathfinderProcessedTiles[tile / 8] |= (byte) bit;

                PathNode node = openPathNodeList[slot];
                node.tile = tile;
                node.from = temp.tile;
                node.rotation = rotation;

                int[] newX = new int[1];
                int[] newY = new int[1];
                TileMath.tileToScreenXY(tile, newX, newY);

                node.estimate = idist(newX[0], newY[0], toScreenX[0], toScreenY[0]);
                node.cost = temp.cost + 50;

                if (isNotInCombat && temp.rotation != rotation) {
                    node.cost += 10;
                }

                if (isCritter) {
                    GameObject o = ObjectSystem.objectFindFirstAtLocation(object.elevation, node.tile);
                    while (o != null) {
                        int pid = o.pid;
                        if (pid >= gooPidBase && pid <= gooPidBase + (ProtoTypes.SceneryProtoTypeId.LastRadioactiveGoo.id()
                                - ProtoTypes.SceneryProtoTypeId.FirstRadioactiveGoo.id())) {
                            break;
                        }
                        o = ObjectSystem.objectFindNextAtLocation();
                    }

                    if (o != null) {
                        if (critterType == ProtoTypes.KillType.KILL_TYPE_GECKO) {
                            node.cost += 100;
                        } else {
                            node.cost += 400;
                        }
                    }
                }
            }

            if (openPathNodeListLength == 0) {
                break;
            }
        }

        if (openPathNodeListLength != 0) {
            int index = 0;
            for (; index < 800; index++) {
                if (temp.tile == from) {
                    break;
                }

                if (rotations != null) {
                    rotations[index] = (byte) (temp.rotation.value() & 0xFF);
                }

                int j = 0;
                while (closedPathNodeList[j].tile != temp.from) {
                    j++;
                }

                PathNode node = closedPathNodeList[j];
                temp.tile = node.tile;
                temp.from = node.from;
                temp.rotation = node.rotation;
                temp.estimate = node.estimate;
                temp.cost = node.cost;
            }

            if (rotations != null) {
                // Looks like array resevering, probably because A* finishes
                // it's path from end to start, this probably reverses it
                // start-to-end.
                int beginning = 0;
                int ending = index - 1;
                int middle = index / 2;
                for (int reverseIndex = 0; reverseIndex < middle; reverseIndex++) {
                    byte swap = rotations[ending];
                    rotations[ending] = rotations[beginning];
                    rotations[beginning] = swap;

                    ending -= 1;
                    beginning += 1;
                }
            }

            return index;
        }

        return 0;
    }
}
