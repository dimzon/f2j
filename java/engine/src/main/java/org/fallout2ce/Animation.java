package org.fallout2ce;

import org.fallout2ce.ObjTypes.GameObject;

/**
 * Animation state slice, mirroring {@code src/animation.cc}: {@code _dude_stand}
 * ({@code dude_stand}, 0x4185D0) plus the queue seams {@code animationStop}
 * ({@code 0x4186E4}) and {@code reg_anim_clear}.
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
}
