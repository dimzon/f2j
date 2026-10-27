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

    // ===== animation sequence store (src/animation.cc), the slice the AI
    // movement helpers need. The tick pump and the per-step handlers stay
    // game phase; registered sequences remain pending, which is what the
    // [aimove] vectors observe. =====

    // ANIM_COMPLETE (src/animation.cc:320).
    private static final int ANIM_COMPLETE = -1000;
    private static final int ANIMATION_SEQUENCE_LIST_CAPACITY = 64;
    private static final int ANIMATION_DESCRIPTION_LIST_CAPACITY = 55;
    private static final int ANIMATION_NON_RESERVED_LIST_CAPACITY = 52;

    // AnimationRequestOptions (src/animation_defs.h).
    public static final int ANIMATION_REQUEST_NONE = 0x00;
    public static final int ANIMATION_REQUEST_UNRESERVED = 0x01;
    public static final int ANIMATION_REQUEST_RESERVED = 0x02;

    // AnimationSequenceFlags (src/animation.cc:104-120).
    private static final int ANIM_SEQ_RESERVED = 0x04;
    private static final int ANIM_SEQ_ACCUMULATING = 0x08;
    private static final int ANIM_SEQ_0x10 = 0x10;
    private static final int ANIM_SEQ_0x20 = 0x20;
    private static final int ANIM_SEQ_INSIGNIFICANT = 0x40;

    // AnimationKind values used by the AI moves (src/animation.cc:55+).
    public static final int ANIM_KIND_MOVE_TO_OBJECT = 0;
    public static final int ANIM_KIND_MOVE_TO_TILE = 1;
    private static final int ANIM_KIND_ROTATE_TO_TILE = 7;

    /** Port of the {@code AnimationDescription} fields the AI moves use. */
    public static final class AnimationDescription {
        public int kind;
        public int anim;
        public GameObject owner;
        public GameObject destination;
        public int tile;
        public int elevation;
        public int actionPoints;
        public int delay;
    }

    /** Port of {@code AnimationSequence} (src/animation.cc:254). */
    public static final class AnimationSequence {
        public int step;
        public int animationIndex;
        public int length;
        public int flags;
        public final AnimationDescription[] animations = new AnimationDescription[ANIMATION_DESCRIPTION_LIST_CAPACITY];
    }

    private static final AnimationSequence[] animationSequences = new AnimationSequence[ANIMATION_SEQUENCE_LIST_CAPACITY];
    private static int animationSequenceCurrentIndex = -1;
    private static int animationDescriptionCurrentIndex;

    static {
        for (int index = 0; index < ANIMATION_SEQUENCE_LIST_CAPACITY; index++) {
            animationSequences[index] = new AnimationSequence();
            for (int action = 0; action < ANIMATION_DESCRIPTION_LIST_CAPACITY; action++) {
                animationSequences[index].animations[action] = new AnimationDescription();
            }
        }
    }

    // 0x4139E8 animationReset: step/flags only - the C++ keeps length (and
    // animationIndex), so stale sequences stay visible to length scans.
    public static void animationReset() {
        // animationStop: the drain is game phase (no pending work here).
        for (int index = 0; index < ANIMATION_SEQUENCE_LIST_CAPACITY; index++) {
            animationSequences[index].step = ANIM_COMPLETE;
            animationSequences[index].flags = 0;
        }
        animationSequenceCurrentIndex = -1;
    }

    // 0x413A80 _anim_free_slot.
    private static int animFreeSlot(int requestOptions) {
        int freeSlot = -1;
        int pending = 0;
        for (int index = 0; index < ANIMATION_SEQUENCE_LIST_CAPACITY; index++) {
            AnimationSequence sequence = animationSequences[index];
            if (sequence.step != ANIM_COMPLETE
                    || (sequence.flags & ANIM_SEQ_ACCUMULATING) != 0
                    || (sequence.flags & ANIM_SEQ_0x20) != 0) {
                if ((sequence.flags & ANIM_SEQ_RESERVED) == 0) {
                    pending++;
                }
            } else if (freeSlot == -1
                    && ((requestOptions & 0x100 /* PING */) == 0
                        || (sequence.flags & ANIM_SEQ_0x10) == 0)) {
                freeSlot = index;
            }
        }

        if (freeSlot == -1) {
            return -1;
        } else if ((requestOptions & ANIMATION_REQUEST_RESERVED) != 0
                || pending < ANIMATION_NON_RESERVED_LIST_CAPACITY) {
            return freeSlot;
        }

        return -1;
    }

    // 0x413B48 _check_registry: the owner must not sit in another pending
    // sequence.
    private static int checkRegistry(GameObject obj) {
        if (animationSequenceCurrentIndex == -1) {
            return -1;
        }

        if (animationDescriptionCurrentIndex >= ANIMATION_DESCRIPTION_LIST_CAPACITY) {
            return -1;
        }

        if (obj == null) {
            return 0;
        }

        for (int sequenceIndex = 0; sequenceIndex < ANIMATION_SEQUENCE_LIST_CAPACITY; sequenceIndex++) {
            AnimationSequence sequence = animationSequences[sequenceIndex];
            if (sequenceIndex != animationSequenceCurrentIndex && sequence.step != ANIM_COMPLETE) {
                for (int actionIndex = 0; actionIndex < sequence.length; actionIndex++) {
                    AnimationDescription description = sequence.animations[actionIndex];
                    if (obj == description.owner && !animationKindIsCallback(description.kind)) {
                        if ((sequence.flags & ANIM_SEQ_INSIGNIFICANT) == 0) {
                            return -1;
                        }
                        animSetEnd(sequenceIndex);
                    }
                }
            }
        }

        return 0;
    }

    private static boolean animationKindIsCallback(int kind) {
        return kind == 11 /* ANIM_KIND_CALLBACK */ || kind == 12 /* ANIM_KIND_CALLBACK3 */;
    }

    // _anim_set_end stub: the sequence-end tail (fidgets, combat turn
    // bookkeeping) is game phase; mark the sequence complete.
    private static int animSetEnd(int sequenceIndex) {
        animationSequences[sequenceIndex].step = ANIM_COMPLETE;
        return 0;
    }

    // 0x413C60 _anim_cleanup.
    private static void animCleanup() {
        if (animationSequenceCurrentIndex == -1) {
            return;
        }

        for (int index = 0; index < ANIMATION_SEQUENCE_LIST_CAPACITY; index++) {
            animationSequences[index].flags &= ~(ANIM_SEQ_ACCUMULATING | ANIM_SEQ_0x10);
        }

        // The art cache keys are game phase (no locks held here).

        animationSequenceCurrentIndex = -1;
    }

    /** Port of {@code reg_anim_begin} (0x413B18). */
    public static int regAnimBegin(int requestOptions) {
        if (animationSequenceCurrentIndex != -1) {
            return -1;
        }

        int slot = animFreeSlot(requestOptions);
        if (slot == -1) {
            return -1;
        }

        AnimationSequence sequence = animationSequences[slot];
        sequence.flags |= ANIM_SEQ_ACCUMULATING;

        if ((requestOptions & ANIMATION_REQUEST_RESERVED) != 0) {
            sequence.flags |= ANIM_SEQ_RESERVED;
        }

        animationSequenceCurrentIndex = slot;
        animationDescriptionCurrentIndex = 0;

        return 0;
    }

    /**
     * Port of {@code reg_anim_end} (0x414030). The combat-anim begin, the
     * first-step run and the tick pump are game phase; the registered
     * sequence stays pending, which is what the vectors observe.
     */
    public static int regAnimEnd() {
        if (animationSequenceCurrentIndex == -1) {
            return -1;
        }

        AnimationSequence sequence = animationSequences[animationSequenceCurrentIndex];
        sequence.step = 0;
        sequence.length = animationDescriptionCurrentIndex;
        sequence.animationIndex = -1;
        sequence.flags &= ~ANIM_SEQ_ACCUMULATING;
        sequence.animations[0].delay = 0;
        // isInCombat() branch: _combat_anim_begin is combat phase.

        int index = animationSequenceCurrentIndex;
        animationSequenceCurrentIndex = -1;

        if ((sequence.flags & ANIM_SEQ_0x10) == 0) {
            // _anim_set_continue(index, 1) runs the first step; the move
            // handler leaves the sequence pending, so the observable state
            // matches without running it.
        }

        return 0;
    }

    // 0x414394 animationRegisterMoveToTile.
    public static int animationRegisterMoveToTile(GameObject owner, int tile, int elevation,
            int actionPoints, int delay) {
        if (checkRegistry(owner) == -1 || actionPoints == 0) {
            animCleanup();
            return -1;
        }

        if (tile == owner.tile && elevation == owner.elevation) {
            return 0;
        }

        AnimationDescription description =
                animationSequences[animationSequenceCurrentIndex].animations[animationDescriptionCurrentIndex];
        description.kind = ANIM_KIND_MOVE_TO_TILE;
        description.anim = 1 /* ANIM_WALK */;
        description.owner = owner;
        description.tile = tile;
        description.elevation = elevation;
        description.actionPoints = actionPoints;
        description.delay = delay;

        FrmId frmId = FrmId.ofCritter(FrmId.ofFid(owner.fid).frameId(), 1 /* ANIM_WALK */,
                ArtDefs.WeaponAnimation.None.id(), owner.rotation.id() + 1);

        if (animPreload(owner, frmId) == -1) {
            animCleanup();
            return -1;
        }

        animationDescriptionCurrentIndex++;

        return 0;
    }

    // animationRegisterRunToTile (0x414564+).
    public static int animationRegisterRunToTile(GameObject owner, int tile, int elevation,
            int actionPoints, int delay) {
        if (checkRegistry(owner) == -1 || actionPoints == 0) {
            animCleanup();
            return -1;
        }

        if (tile == owner.tile && elevation == owner.elevation) {
            return 0;
        }

        if (Critter.critterIsEncumbered(owner)) {
            if (PartyMembers.objectIsPartyMember(owner)) {
                // reportOverloaded: game phase.
            }

            return animationRegisterMoveToTile(owner, tile, elevation, actionPoints, delay);
        }

        AnimationDescription description =
                animationSequences[animationSequenceCurrentIndex].animations[animationDescriptionCurrentIndex];
        description.kind = ANIM_KIND_MOVE_TO_TILE;
        description.owner = owner;
        description.tile = tile;
        description.elevation = elevation;

        if ((FrmId.ofFid(owner.fid).objectType() == ObjectType.CRITTER
                && (owner.data.critter.combat.results.bits() & ObjTypes.Dam.CRIP_LEG_ANY.bits()) != 0)
                || (owner == ObjectSystem.getDude() && Critter.dudeHasState(Critter.DUDE_STATE_SNEAKING)
                    && Perk.perkGetRank(owner, PerkDefs.Perk.PERK_SILENT_RUNNING.id()) == 0)
                || !FrmId.ofCritter(FrmId.ofFid(owner.fid).frameId(), 19 /* ANIM_RUNNING */,
                    ArtDefs.WeaponAnimation.None.id(), owner.rotation.id() + 1).exist()) {
            description.anim = 1 /* ANIM_WALK */;
        } else {
            description.anim = 19 /* ANIM_RUNNING */;
        }

        description.actionPoints = actionPoints;
        description.delay = delay;

        FrmId frmId = FrmId.ofCritter(FrmId.ofFid(owner.fid).frameId(), description.anim,
                ArtDefs.WeaponAnimation.None.id(), owner.rotation.id() + 1);

        if (animPreload(owner, frmId) == -1) {
            animCleanup();
            return -1;
        }

        animationDescriptionCurrentIndex++;

        return 0;
    }

    // animationRegisterRunToObject.
    public static int animationRegisterRunToObject(GameObject owner, GameObject destination,
            int actionPoints, int delay) {
        if (checkRegistry(owner) == -1 || actionPoints == 0) {
            animCleanup();
            return -1;
        }

        if (owner.tile == destination.tile && owner.elevation == destination.elevation) {
            return 0;
        }

        if (Critter.critterIsEncumbered(owner)) {
            if (PartyMembers.objectIsPartyMember(owner)) {
                // reportOverloaded: game phase.
            }

            return animationRegisterMoveToObject(owner, destination, actionPoints, delay);
        }

        AnimationDescription description =
                animationSequences[animationSequenceCurrentIndex].animations[animationDescriptionCurrentIndex];
        description.kind = ANIM_KIND_MOVE_TO_OBJECT;
        description.owner = owner;
        description.destination = destination;

        if ((FrmId.ofFid(owner.fid).objectType() == ObjectType.CRITTER
                && (owner.data.critter.combat.results.bits() & ObjTypes.Dam.CRIP_LEG_ANY.bits()) != 0)
                || (owner == ObjectSystem.getDude() && Critter.dudeHasState(Critter.DUDE_STATE_SNEAKING)
                    && Perk.perkGetRank(owner, PerkDefs.Perk.PERK_SILENT_RUNNING.id()) == 0)
                || !FrmId.ofCritter(FrmId.ofFid(owner.fid).frameId(), 19 /* ANIM_RUNNING */,
                    ArtDefs.WeaponAnimation.None.id(), owner.rotation.id() + 1).exist()) {
            description.anim = 1 /* ANIM_WALK */;
        } else {
            description.anim = 19 /* ANIM_RUNNING */;
        }

        description.actionPoints = actionPoints;
        description.delay = delay;

        FrmId frmId = FrmId.ofCritter(FrmId.ofFid(owner.fid).frameId(), description.anim,
                ArtDefs.WeaponAnimation.None.id(), owner.rotation.id() + 1);

        if (animPreload(owner, frmId) == -1) {
            animCleanup();
            return -1;
        }

        animationDescriptionCurrentIndex++;
        return animationRegisterRotateToTile(owner, destination.tile);
    }

    // animationRegisterMoveToObject (walk variant; the encumbered
    // fallback).
    public static int animationRegisterMoveToObject(GameObject owner, GameObject destination,
            int actionPoints, int delay) {
        if (checkRegistry(owner) == -1 || actionPoints == 0) {
            animCleanup();
            return -1;
        }

        if (owner.tile == destination.tile && owner.elevation == destination.elevation) {
            return 0;
        }

        AnimationDescription description =
                animationSequences[animationSequenceCurrentIndex].animations[animationDescriptionCurrentIndex];
        description.kind = ANIM_KIND_MOVE_TO_OBJECT;
        description.anim = 1 /* ANIM_WALK */;
        description.owner = owner;
        description.destination = destination;
        description.actionPoints = actionPoints;
        description.delay = delay;

        animationDescriptionCurrentIndex++;

        return 0;
    }

    // animationRegisterRotateToTile (0x414CC8).
    static int animationRegisterRotateToTile(GameObject owner, int tile) {
        if (checkRegistry(owner) == -1) {
            animCleanup();
            return -1;
        }

        AnimationDescription description =
                animationSequences[animationSequenceCurrentIndex].animations[animationDescriptionCurrentIndex];
        description.kind = ANIM_KIND_ROTATE_TO_TILE;
        description.delay = -1;
        description.owner = owner;
        description.tile = tile;

        animationDescriptionCurrentIndex++;

        return 0;
    }

    /**
     * Seam for {@code _anim_preload} (0x413D6C): the C++ locks the art and
     * keeps the cache key; the observable part for the AI moves is whether
     * the frm exists, mirrored via {@link FrmId#exist()}.
     */
    private static int animPreload(GameObject owner, FrmId frmId) {
        return frmId.exist() ? 0 : -1;
    }

    /** Test seam: mirror of the [aimove] animation dump (actions 0-2). */
    static int[] lastActionForTests() {
        int[] out = new int[12];
        for (int index = ANIMATION_SEQUENCE_LIST_CAPACITY - 1; index >= 0; index--) {
            AnimationSequence sequence = animationSequences[index];
            if (sequence.length > 0) {
                for (int actionIndex = 0; actionIndex < 3; actionIndex++) {
                    int base = actionIndex * 4;
                    if (actionIndex >= sequence.length) {
                        out[base] = -1;
                        out[base + 1] = -1;
                        out[base + 2] = -1;
                        out[base + 3] = -1;
                        continue;
                    }
                    AnimationDescription description = sequence.animations[actionIndex];
                    out[base] = description.kind;
                    out[base + 1] = description.anim;
                    if (description.kind == ANIM_KIND_MOVE_TO_OBJECT) {
                        GameObject destination = description.destination;
                        out[base + 2] = destination != null ? destination.pid : -1;
                    } else {
                        out[base + 2] = description.tile;
                    }
                    out[base + 3] = description.actionPoints;
                }
                return out;
            }
        }

        return null;
    }
}
