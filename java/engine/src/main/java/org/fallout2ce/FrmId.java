package org.fallout2ce;

/**
 * Port of {@code FrmId} from {@code src/art.h} (FID math, equality and
 * path building; file-existence checks read the VFS like C).
 *
 * <p>FID structure: 3 bits rotation, 4 bits object type, 8 bits animation
 * type, 4 bits weapon code, 12 bits frame id.
 */
public final class FrmId {

    public static final int EMPTY_FID = -1;
    public static final int INVALID_FRAME_ID = -1;
    public static final int MIN_FRAME_ID = 0;
    public static final int MAX_FRAME_ID = 4095;

    static final int FRAME_ID_MASK = 0x00000FFF;
    static final int WEAPON_ANIMATION_MASK = 0x0000F000;
    static final int ANIMATION_TYPE_MASK = 0x00FF0000;
    static final int OBJECT_TYPE_MASK = 0x0F000000;
    static final int ROTATION_MASK = 0x70000000;

    static final int WEAPON_ANIMATION_MASK_POSITION = 12;
    static final int ANIMATION_TYPE_MASK_POSITION = 16;
    static final int OBJECT_TYPE_MASK_POSITION = 24;
    static final int ROTATION_MASK_POSITION = 28;

    private final ObjectType objectType;
    private final int fid;
    private final int frameId;
    private final String path;

    private FrmId(ObjectType objectType, int fid, int frameId, String path) {
        this.objectType = objectType;
        this.fid = fid;
        this.frameId = frameId;
        this.path = path;
    }

    private static int frameIdOrInvalid(int frameId) {
        return frameId < MIN_FRAME_ID ? INVALID_FRAME_ID : frameIdFromFid(frameId);
    }

    /** Default (empty) id, like {@code FrmId::Empty()}. */
    public static final FrmId EMPTY = new FrmId(ObjectType.INVALID, EMPTY_FID, INVALID_FRAME_ID, null);

    /** Default (empty) id. */
    public static FrmId emptyId() {
        return EMPTY;
    }

    /** {@code explicit FrmId(int fid)}. */
    public static FrmId ofFid(int fid) {
        ObjectType type = fid == EMPTY_FID ? ObjectType.INVALID : ObjectType.fromInt(objectTypeFromFid(fid));
        return new FrmId(type, fid, frameIdOrInvalid(fid), null);
    }

    /** Template {@code FrmId(TFrameId)} for non-critter types (anim 0). */
    public static FrmId ofType(ObjectType type, int frameId) {
        return new FrmId(type, buildFid(type, frameId), frameIdOrInvalid(frameId), null);
    }

    /** {@code FrmId(MiscFrameId, AnimationType)}. */
    public static FrmId ofMisc(int misc, int animType) {
        return new FrmId(ObjectType.MISC, buildFid(ObjectType.MISC, misc, animType),
                frameIdOrInvalid(misc), null);
    }

    /** {@code FrmId(HeadFrameId, HeadAnimation, HeadFidgetAnimation)}. */
    public static FrmId ofHead(int head, int headAnimation, int fidgetAnimation) {
        return new FrmId(ObjectType.HEAD,
                buildFid(ObjectType.HEAD, head, headAnimation, fidgetAnimation),
                frameIdOrInvalid(head), null);
    }

    /** {@code FrmId(CritterFrameId, AnimationType, WeaponAnimation, Rotation)}. */
    public static FrmId ofCritter(int critter, int animType, int weaponAnimation, int rotation) {
        return new FrmId(ObjectType.CRITTER,
                buildObjectFid(ObjectType.CRITTER, critter, animType, weaponAnimation, rotation),
                frameIdOrInvalid(critter), null);
    }

    /** {@code FrmId(ObjectType, int, AnimationType, WeaponAnimation, Rotation)}. */
    public static FrmId ofObject(int objectType, int frmId, int animType, int weaponAnimation, int rotation) {
        return new FrmId(ObjectType.fromInt(objectType),
                buildObjectFid(ObjectType.fromInt(objectType), frmId, animType, weaponAnimation, rotation),
                frameIdOrInvalid(frmId), null);
    }

    /** Path-backed id. */
    public static FrmId ofPath(ObjectType objectType, String path) {
        return new FrmId(objectType, EMPTY_FID, INVALID_FRAME_ID, path);
    }

    public boolean hasFid() {
        return fid > EMPTY_FID;
    }

    public boolean hasObjectType() {
        return ObjectType.isValid(objectType.value());
    }

    public boolean hasWeaponAnimation() {
        return hasFid() && WeaponAnimation.isValid(weaponAnimationFromFid(fid));
    }

    public boolean hasRotation() {
        return hasFid() && Rotation.isValid(rotationFromFid(fid));
    }

    public boolean hasAnimationType() {
        return hasFid() && AnimationType.isValid(animationTypeFromFid(fid));
    }

    public boolean valid() {
        return !empty()
                && hasObjectType()
                && ((frameId >= MIN_FRAME_ID && frameId <= MAX_FRAME_ID) || path != null);
    }

    public boolean exist() {
        if (!hasFid() || !valid()) {
            return false;
        }
        return exist(fid);
    }

    static boolean exist(int fid) {
        if (fid <= EMPTY_FID) {
            return false;
        }
        String filePath = buildPath(fid);
        if (filePath == null) {
            return false;
        }
        int[] size = new int[1];
        return Db.dbGetFileSize(filePath, size) != -1;
    }

    public boolean empty() {
        return equals(EMPTY);
    }

    public int fid() {
        return fid;
    }

    /** Null when unresolvable (C returns nullptr), like C. */
    public String filePath() {
        if (path != null) {
            return path;
        }
        return buildPath(fid);
    }

    public ObjectType objectType() {
        return hasObjectType() ? objectType : ObjectType.INVALID;
    }

    public int frameId() {
        return frameId;
    }

    public int weaponAnimation() {
        return hasWeaponAnimation() ? weaponAnimationFromFid(fid) : WeaponAnimation.NONE.value();
    }

    public int rotation() {
        return hasRotation() ? rotationFromFid(fid) : Rotation.INVALID.value();
    }

    public int animationType() {
        return hasAnimationType() ? animationTypeFromFid(fid) : AnimationType.INVALID.value();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FrmId other)) {
            return false;
        }
        if (fid != other.fid) {
            return false;
        }
        if (objectType != other.objectType) {
            return false;
        }
        if (path == null && other.path == null) {
            return true;
        }
        if (path == null || other.path == null) {
            return false;
        }
        return path.equals(other.path);
    }

    @Override
    public int hashCode() {
        int h = fid;
        h = 31 * h + objectType.value();
        h = 31 * h + (path == null ? 0 : path.hashCode());
        return h;
    }

    static int frameIdFromFid(int fid) {
        return fid & FRAME_ID_MASK;
    }

    static int weaponAnimationFromFid(int fid) {
        return (fid & WEAPON_ANIMATION_MASK) >> WEAPON_ANIMATION_MASK_POSITION;
    }

    static int animationTypeFromFid(int fid) {
        return (fid & ANIMATION_TYPE_MASK) >> ANIMATION_TYPE_MASK_POSITION;
    }

    static int objectTypeFromFid(int fid) {
        return (fid & OBJECT_TYPE_MASK) >> OBJECT_TYPE_MASK_POSITION;
    }

    static int rotationFromFid(int fid) {
        return (fid & ROTATION_MASK) >> ROTATION_MASK_POSITION;
    }

    static int buildFid(ObjectType objectType, int frmId) {
        return buildFid(objectType, frmId, 0, 0, Rotation.NE.value());
    }

    static int buildFid(ObjectType objectType, int frmId, int animType) {
        return buildFid(objectType, frmId, animType, 0, Rotation.NE.value());
    }

    static int buildFid(ObjectType objectType, int frmId, int animType, int weaponAnimation) {
        return buildFid(objectType, frmId, animType, weaponAnimation, Rotation.NE.value());
    }

    static int buildFid(ObjectType objectType, int frmId, int animType, int weaponAnimation, int rotation) {
        if (!ObjectType.isValid(objectType.value()) || !Rotation.isValid(rotation)
                || frmId < MIN_FRAME_ID) {
            return EMPTY_FID;
        }

        return ((rotation << ROTATION_MASK_POSITION) & ROTATION_MASK)
                | ((objectType.value() << OBJECT_TYPE_MASK_POSITION) & OBJECT_TYPE_MASK)
                | ((animType << ANIMATION_TYPE_MASK_POSITION) & ANIMATION_TYPE_MASK)
                | ((weaponAnimation << WEAPON_ANIMATION_MASK_POSITION) & WEAPON_ANIMATION_MASK)
                | (frmId & FRAME_ID_MASK);
    }

    static int buildObjectFid(ObjectType objectType, int frmId, int animType, int weaponAnimation,
            int rotation) {
        // Non-critters (and some critter anims) always use rotation NE;
        // others fall back to E and then NE when art is missing.
        if (objectType != ObjectType.CRITTER
                || animType == AnimationType.FIRE_DANCE.value()
                || animType < AnimationType.FALL_BACK.value()
                || animType > AnimationType.FALL_FRONT_BLOOD.value()) {
            rotation = Rotation.NE.value();
        } else if (!exist(buildFid(objectType, frmId, animType, weaponAnimation, rotation))) {
            rotation = rotation != Rotation.E.value()
                    && exist(buildFid(objectType, frmId, animType, weaponAnimation, Rotation.E.value()))
                            ? Rotation.E.value()
                            : Rotation.NE.value();
        }

        return buildFid(objectType, frmId, animType, weaponAnimation, rotation);
    }

    static int buildAliasFid(int fid) {
        if (fid <= EMPTY_FID || objectTypeFromFid(fid) != ObjectType.CRITTER.value()) {
            return EMPTY_FID;
        }

        int anim = animationTypeFromFid(fid);
        if (anim == AnimationType.ELECTRIFY.value()
                || anim == AnimationType.CHARRED_BODY.value()
                || anim == AnimationType.BURNED_TO_NOTHING.value()
                || anim == AnimationType.ELECTRIFIED_TO_NOTHING.value()
                || anim == AnimationType.ELECTRIFY_SF.value()
                || anim == AnimationType.CHARRED_BODY_SF.value()
                || anim == AnimationType.BURNED_TO_NOTHING_SF.value()
                || anim == AnimationType.ELECTRIFIED_TO_NOTHING_SF.value()
                || anim == AnimationType.FIRE_DANCE.value()
                || anim == AnimationType.CALLED_SHOT_PIC.value()) {
            int aliasedFrameId = Art.anonAlias(frameIdFromFid(fid));
            return buildFid(ObjectType.CRITTER, aliasedFrameId, anim,
                    weaponAnimationFromFid(fid), rotationFromFid(fid));
        }

        return EMPTY_FID;
    }

    /**
     * Fid to {@code art\...} path (null when unresolvable). Reads the loaded
     * art lists; {@code _cd_path_base} is empty outside game init.
     */
    static String buildPath(int fid) {
        int baseFid = fid;
        int rotation = rotationFromFid(fid);

        int aliasFid = buildAliasFid(fid);
        if (aliasFid != EMPTY_FID) {
            baseFid = aliasFid;
        }

        int frmId = frameIdFromFid(baseFid);
        int animType = animationTypeFromFid(baseFid);
        int weaponCode = weaponAnimationFromFid(baseFid);
        int objectType = objectTypeFromFid(baseFid);

        if (!ObjectType.isValid(objectType)) {
            return null;
        }

        Art.ArtLists lists = Art.artLists;
        if (frmId >= lists.fileNamesLength(objectType)) {
            return null;
        }

        String base = Art.cdPathBase;
        String typeName = lists.typeName(objectType);
        String fileName = lists.fileName(objectType, frmId);

        if (objectType == ObjectType.CRITTER.value()) {
            int[] codes = Art.animCodes(animType, weaponCode);
            if (codes == null) {
                return null;
            }
            if (rotation > Rotation.NE.value()) {
                return String.format("%sart\\%s\\%s%c%c.fr%c", base, typeName, fileName,
                        (char) (int) codes[0], (char) (int) codes[1], (char) (rotation + ('0' - 1)));
            }
            return String.format("%sart\\%s\\%s%c%c.frm", base, typeName, fileName,
                    (char) (int) codes[0], (char) (int) codes[1]);
        }

        if (objectType == ObjectType.HEAD.value()) {
            return Art.headPath(base, typeName, fileName, animType, weaponCode);
        }

        return String.format("%sart\\%s\\%s", base, typeName, fileName);
    }
}
