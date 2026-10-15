package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of the cache-free part of {@code src/art.cc}: FRM header/frame
 * decoding ({@code artRead*}), accessors, art lists ({@code artInit} minus
 * the cache), path building data, and small queries.
 *
 * <p>Decoded frames live as objects (C walks one flat buffer with
 * {@code sizeof(Art) == 88} / {@code sizeof(ArtFrame) == 12} pointer
 * arithmetic; the walk order and validation are mirrored, the storage is
 * not). Rotations sharing a data offset alias the same group, like C.
 */
public final class Art {

    private Art() {
    }

    public static final int NAME_SIZE = 13;

    // C struct sizes on the decoded buffer (with alignment padding).
    static final int STRUCT_SIZE = 88;
    static final int FRAME_STRUCT_SIZE = 12;

    /** Mirror of {@code ArtFrame} (pixels follow, like C). */
    public static final class Frame {
        public int width;
        public int height;
        public int size;
        public int x;
        public int y;
        public byte[] pixels = new byte[0];
    }

    /** Mirror of {@code Art} (header + per-rotation frame groups). */
    public static final class ArtData {
        public int version;
        public int framesPerSecond;
        public int actionFrame;
        public int frameCount;
        public final int[] xOffsets = new int[Rotation.COUNT];
        public final int[] yOffsets = new int[Rotation.COUNT];
        public final int[] dataOffsets = new int[Rotation.COUNT];
        public final int[] padding = new int[Rotation.COUNT];
        public int dataSize;
        // Decoded groups in read order; rotationBase maps each rotation to
        // its group start (aliased rotations share it, like C).
        public final List<Frame> frames = new ArrayList<>();
        public final int[] rotationBase = new int[Rotation.COUNT];
    }

    /** Mirror of one {@code ArtListDescription} row. */
    public static final class ArtList {
        public int flags;
        public String name = "";
        public final List<String> fileNames = new ArrayList<>();
        // C checks the raw pointer (null = never loaded).
        boolean loaded;
    }

    public static final class HeadDescription {
        public int goodFidgetCount;
        public int neutralFidgetCount;
        public int badFidgetCount;
    }

    /** Mirror of the art-list globals (order = type id, like C). */
    public static final class ArtLists {
        static final String[] TYPE_NAMES = {
            "items", "critters", "scenery", "walls", "tiles", "misc",
            "intrface", "inven", "heads", "backgrnd", "skilldex"
        };

        final List<ArtList> descriptions = new ArrayList<>();
        final List<Integer> anonAlias = new ArrayList<>();
        final List<Integer> shouldRunData = new ArrayList<>();
        final List<HeadDescription> headDescriptions = new ArrayList<>();
        int vaultGuyNum = 0; // CritterFrameId::First
        final int[][] vaultPersonNums = new int[2][2];
        int mapperBlankTile = -1;
        boolean languageInitialized;
        String language = "";

        ArtLists() {
            for (String name : TYPE_NAMES) {
                ArtList list = new ArtList();
                list.name = name;
                descriptions.add(list);
            }
        }

        int fileNamesLength(int objectType) {
            if (!ObjectType.isValid(objectType)) {
                return 0;
            }
            return descriptions.get(objectType).fileNames.size();
        }

        String typeName(int objectType) {
            return descriptions.get(objectType).name;
        }

        String fileName(int objectType, int frameId) {
            return descriptions.get(objectType).fileNames.get(frameId);
        }
    }

    static final ArtLists artLists = new ArtLists();

    /** Mirror of {@code _cd_path_base} (empty outside game init). */
    static String cdPathBase = "";

    static final String HEAD_CODES_1 = "gggnnnbbbgnb";
    static final String HEAD_CODES_2 = "vfngfbnfvppp";

    static final String DEFAULT_JUMPSUIT_MALE = "hmjmps";
    static final String DEFAULT_JUMPSUIT_FEMALE = "hfjmps";
    static final String DEFAULT_TRIBAL_MALE = "hmwarr";
    static final String DEFAULT_TRIBAL_FEMALE = "hfprim";

    // 0x419E1C artReadHeader (reads into the object; see artRead).
    static int artReadHeader(ArtData art, XFile.Handle stream) {
        int[] version = new int[1];
        if (Db.fileReadInt32(stream, version) == -1) {
            return -1;
        }
        art.version = version[0];

        int[] fps = new int[1];
        if (Db.fileReadInt16(stream, fps) == -1) {
            return -1;
        }
        art.framesPerSecond = (short) (int) fps[0];

        int[] action = new int[1];
        if (Db.fileReadInt16(stream, action) == -1) {
            return -1;
        }
        art.actionFrame = (short) (int) action[0];

        int[] count = new int[1];
        if (Db.fileReadInt16(stream, count) == -1) {
            return -1;
        }
        art.frameCount = (short) (int) count[0];

        short[] xoff = new short[Rotation.COUNT];
        if (Db.fileReadInt16List(stream, xoff, 0, Rotation.COUNT) == -1) {
            return -1;
        }
        short[] yoff = new short[Rotation.COUNT];
        if (Db.fileReadInt16List(stream, yoff, 0, Rotation.COUNT) == -1) {
            return -1;
        }
        int[] doff = new int[Rotation.COUNT];
        if (Db.fileReadInt32List(stream, doff, 0, Rotation.COUNT) == -1) {
            return -1;
        }
        for (int i = 0; i < Rotation.COUNT; i++) {
            art.xOffsets[i] = xoff[i];
            art.yOffsets[i] = yoff[i];
            art.dataOffsets[i] = doff[i];
        }

        int[] dataSize = new int[1];
        if (Db.fileReadInt32(stream, dataSize) == -1) {
            return -1;
        }
        art.dataSize = dataSize[0];

        long frameDataOffset = Db.fileTell(stream);
        long fileSize = XFile.xfileGetSize(stream);
        if (frameDataOffset < 0 || fileSize < 0 || art.frameCount < 0) {
            return -1;
        }

        long payloadSize;
        if (fileSize != 0) {
            if (fileSize < frameDataOffset) {
                return -1;
            }

            payloadSize = fileSize - frameDataOffset;
            if (art.dataSize < 0) {
                return -1;
            }

            // The file payload is authoritative (split .FR0-.FR5 repeat the
            // aggregate size; some FRMs report only the first rotation).
            art.dataSize = (int) payloadSize;
        } else {
            // Gzip streams have no decompressed size: keep the header size.
            if (art.dataSize <= 0) {
                return -1;
            }

            payloadSize = art.dataSize;
        }

        for (int rotation = 0; rotation < Rotation.COUNT; rotation++) {
            if (art.dataOffsets[rotation] < 0
                    || art.dataOffsets[rotation] > payloadSize
                    || (rotation > 0 && art.dataOffsets[rotation] < art.dataOffsets[rotation - 1])) {
                return -1;
            }
        }

        return 0;
    }

    static int artReadFrameData(List<Frame> out, XFile.Handle stream, int count, int sourceSize,
            int[] paddingPtr) {
        int sourceOffset = 0;
        int padding = 0;
        // NOTE: frames are packed on disk (no padding); padding only aligns
        // the decoded memory layout, like C.
        for (int index = 0; index < count; index++) {
            if (sourceSize - sourceOffset < FRAME_STRUCT_SIZE) {
                return -1;
            }
            int[] w = new int[1];
            int[] h = new int[1];
            int[] size = new int[1];
            int[] x = new int[1];
            int[] y = new int[1];
            if (Db.fileReadInt16(stream, w) == -1) {
                return -1;
            }
            if (Db.fileReadInt16(stream, h) == -1) {
                return -1;
            }
            if (Db.fileReadInt32(stream, size) == -1) {
                return -1;
            }
            if (Db.fileReadInt16(stream, x) == -1) {
                return -1;
            }
            if (Db.fileReadInt16(stream, y) == -1) {
                return -1;
            }
            sourceOffset += FRAME_STRUCT_SIZE;

            int width = (short) (int) w[0];
            int height = (short) (int) h[0];
            int frameSize = size[0];
            if (width < 0
                    || height < 0
                    || frameSize < 0
                    || (long) width * (long) height != (long) frameSize
                    || frameSize > sourceSize - sourceOffset) {
                return -1;
            }

            Frame frame = new Frame();
            frame.width = width;
            frame.height = height;
            frame.size = frameSize;
            frame.x = (short) (int) x[0];
            frame.y = (short) (int) y[0];
            frame.pixels = new byte[frameSize];
            if (frameSize > 0) {
                byte[] pixels = new byte[frameSize];
                if (Db.fileRead(pixels, 0, frameSize, 1, stream) != 1) {
                    return -1;
                }
                frame.pixels = pixels;
            }

            sourceOffset += frameSize;
            padding += paddingForSize(frameSize);

            out.add(frame);
        }

        paddingPtr[0] = padding;
        return 0;
    }

    // Worst-case decoded size (every frame unaligned), like C.
    static int artGetDataSize(ArtData art) {
        if (art.dataSize < 0 || art.frameCount < 0) {
            return -1;
        }

        long dataSize = (long) STRUCT_SIZE + (long) art.dataSize;

        for (int index = 0; index < Rotation.COUNT; index++) {
            if (index == 0 || art.dataOffsets[index - 1] != art.dataOffsets[index]) {
                dataSize += (long) (Integer.BYTES - 1) * art.frameCount;
            }
        }

        return dataSize <= Integer.MAX_VALUE ? (int) dataSize : -1;
    }

    static int paddingForSize(int size) {
        return (Integer.BYTES - size % Integer.BYTES) % Integer.BYTES;
    }

    /**
     * Full FRM read into objects ({@code artRead} codes: -1 bad args,
     * -2 open, -3 header, -5 frame data). Returns 0 with {@code out[0]} set.
     */
    public static int artRead(String path, ArtData[] out) {
        if (path == null || out == null) {
            return -1;
        }

        XFile.Handle stream = Db.fileOpen(path, "rb");
        if (stream == null) {
            return -2;
        }

        ArtData art = new ArtData();
        if (artReadHeader(art, stream) != 0) {
            Debug.print("ART ERROR: rejected %s: invalid header\n", path);
            Db.fileClose(stream);
            return -3;
        }

        int totalAllocSize = artGetDataSize(art);
        if (totalAllocSize <= 0) {
            Debug.print("ART ERROR: artRead computed invalid totalAllocSize %d for %s\n", totalAllocSize, path);
            Db.fileClose(stream);
            return -5;
        }

        long frameDataOffset = Db.fileTell(stream);
        if (frameDataOffset < 0) {
            Debug.print("ART ERROR: rejected %s: invalid frame data offset\n", path);
            Db.fileClose(stream);
            return -5;
        }

        int currentPadding = paddingForSize(STRUCT_SIZE);
        int previousPadding = 0;

        for (int index = 0; index < Rotation.COUNT; index++) {
            art.padding[index] = currentPadding;

            if (index == 0 || art.dataOffsets[index - 1] != art.dataOffsets[index]) {
                art.padding[index] += previousPadding;
                currentPadding += previousPadding;

                int nextDataOffset = art.dataSize;
                for (int nextIndex = index + 1; nextIndex < Rotation.COUNT; nextIndex++) {
                    if (art.dataOffsets[nextIndex] != art.dataOffsets[index]) {
                        nextDataOffset = art.dataOffsets[nextIndex];
                        break;
                    }
                }

                if (Db.fileSeek(stream, frameDataOffset + art.dataOffsets[index], DFile.SEEK_SET) != 0) {
                    Db.fileClose(stream);
                    return -5;
                }

                int groupStart = art.frames.size();
                art.rotationBase[index] = groupStart;
                int[] padOut = new int[1];
                if (artReadFrameData(art.frames, stream, art.frameCount,
                        nextDataOffset - art.dataOffsets[index], padOut) != 0) {
                    Debug.print("ART ERROR: rejected %s: invalid frame data for rotation %d\n", path, index);
                    Db.fileClose(stream);
                    return -5;
                }
                previousPadding = padOut[0];
            } else {
                // Aliased rotation shares the previous group, like C.
                art.rotationBase[index] = art.rotationBase[index - 1];
            }
        }

        Db.fileClose(stream);
        out[0] = art;
        return 0;
    }

    // 0x419760 (null -> 10, zero fps -> 10).
    public static int artGetFramesPerSecond(ArtData art) {
        if (art == null) {
            return 10;
        }

        return art.framesPerSecond == 0 ? 10 : art.framesPerSecond;
    }

    // 0x419778
    public static int artGetActionFrame(ArtData art) {
        return art == null ? -1 : art.actionFrame;
    }

    // 0x41978C
    public static int artGetFrameCount(ArtData art) {
        return art == null ? -1 : art.frameCount;
    }

    static Frame artGetFrame(ArtData art, int frame, int rotation) {
        if (!Rotation.isValid(rotation)) {
            return null;
        }

        if (art == null) {
            return null;
        }

        if (frame < 0 || frame >= art.frameCount) {
            return null;
        }

        return art.frames.get(art.rotationBase[rotation] + frame);
    }

    // 0x4197A0
    public static int artGetWidth(ArtData art, int frame, int rotation) {
        Frame frm = artGetFrame(art, frame, rotation);
        if (frm == null) {
            return -1;
        }

        return frm.width;
    }

    // 0x4197B8
    public static int artGetHeight(ArtData art, int frame, int rotation) {
        Frame frm = artGetFrame(art, frame, rotation);
        if (frm == null) {
            return -1;
        }

        return frm.height;
    }

    // 0x4197D4
    public static int artGetSize(ArtData art, int frame, int rotation, int[] widthPtr, int[] heightPtr) {
        Frame frm = artGetFrame(art, frame, rotation);
        if (frm == null) {
            if (widthPtr != null) {
                widthPtr[0] = 0;
            }

            if (heightPtr != null) {
                heightPtr[0] = 0;
            }

            return -1;
        }

        if (widthPtr != null) {
            widthPtr[0] = frm.width;
        }

        if (heightPtr != null) {
            heightPtr[0] = frm.height;
        }

        return 0;
    }

    // 0x419820
    public static int artGetFrameOffsets(ArtData art, int frame, int rotation, int[] xPtr, int[] yPtr) {
        int[] width = new int[1];
        int[] height = new int[1];
        return artGetFrameData(art, frame, rotation, width, height, xPtr, yPtr) != null ? 0 : -1;
    }

    // 0x41984C (no rotation bounds check, like C).
    public static int artGetRotationOffsets(ArtData art, int rotation, int[] xPtr, int[] yPtr) {
        if (art == null) {
            return -1;
        }

        xPtr[0] = art.xOffsets[rotation];
        yPtr[0] = art.yOffsets[rotation];

        return 0;
    }

    // 0x419870
    public static byte[] artGetFrameData(ArtData art, int frame, int rotation) {
        int[] width = new int[1];
        int[] height = new int[1];
        int[] x = new int[1];
        int[] y = new int[1];
        return artGetFrameData(art, frame, rotation, width, height, x, y);
    }

    public static byte[] artGetFrameData(ArtData art, int frame, int rotation, int[] widthPtr,
            int[] heightPtr, int[] xOffsetPtr, int[] yOffsetPtr) {
        Frame frm = artGetFrame(art, frame, rotation);
        if (frm == null) {
            return null;
        }

        if (widthPtr != null) {
            widthPtr[0] = frm.width;
        }

        if (heightPtr != null) {
            heightPtr[0] = frm.height;
        }

        if (xOffsetPtr != null) {
            xOffsetPtr[0] = frm.x;
        }

        if (yOffsetPtr != null) {
            yOffsetPtr[0] = frm.y;
        }

        return frm.pixels;
    }

    static int anonAlias(int frameId) {
        return artLists.anonAlias.get(frameId);
    }

    // 0x4199AC
    public static int artCritterFrmIdShouldRun(FrmId frmId) {
        if (frmId.objectType() == ObjectType.CRITTER && frmId.valid() && frmId.hasFid()) {
            return artLists.shouldRunData.get(frmId.frameId());
        }

        return 0;
    }

    public static String artGetObjectTypeName(int objectType) {
        return ObjectType.isValid(objectType) ? artLists.descriptions.get(objectType).name : null;
    }

    // 0x418F34
    public static int artIsObjectTypeHidden(int objectType) {
        return ObjectType.isValid(objectType) ? artLists.descriptions.get(objectType).flags & 1 : 0;
    }

    // 0x409DF0
    public static void artToggleObjectTypeHidden(int objectType) {
        if (ObjectType.isValid(objectType)) {
            ArtList list = artLists.descriptions.get(objectType);
            list.flags ^= 1;
        }
    }

    // 0x418F7C (needs loaded head descriptions; negative head reads
    // out of bounds in C — Java throws instead, both undefined).
    public static int artGetFidgetCount(int headFrameId, int fidget) {
        if (headFrameId >= artLists.headDescriptions.size()) {
            return 0;
        }

        HeadDescription description = artLists.headDescriptions.get(headFrameId);
        switch (fidget) {
        case -1:
            return -1;
        case 1:
            return description.goodFidgetCount;
        case 4:
            return description.neutralFidgetCount;
        case 7:
            return description.badFidgetCount;
        default:
            return 0;
        }
    }

    public static int artListIndex(int objectType, String name) {
        if (!ObjectType.isValid(objectType)) {
            return -1;
        }
        ArtList list = artLists.descriptions.get(objectType);
        if (!list.loaded) {
            return -1;
        }

        String upperName = asciiUpper(name.length() > NAME_SIZE - 1
                ? name.substring(0, NAME_SIZE - 1)
                : name);

        for (int index = 0; index < list.fileNames.size(); index++) {
            String entry = list.fileNames.get(index);
            String upperEntry = asciiUpper(
                    entry.length() > NAME_SIZE - 1 ? entry.substring(0, NAME_SIZE - 1) : entry);

            int end = 0;
            while (end < upperEntry.length()) {
                char ch = upperEntry.charAt(end);
                if (!((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_')) {
                    break;
                }
                end++;
            }
            upperEntry = upperEntry.substring(0, end);

            if (upperEntry.equals(upperName)) {
                return index;
            }
        }

        return -1;
    }

    static String asciiUpper(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            out.append(ch >= 'a' && ch <= 'z' ? (char) (ch - ('a' - 'A')) : ch);
        }
        return out.toString();
    }

    public static int artCopyFileName(FrmId frmId, String[] dest) {
        if (!frmId.valid()) {
            return -1;
        }

        ArtList list = artLists.descriptions.get(frmId.objectType().value());
        if (!frmId.hasFid() || frmId.frameId() >= list.fileNames.size()) {
            return -1;
        }

        dest[0] = list.fileNames.get(frmId.frameId());
        return 0;
    }

    // 0x419314 (returns weapon/animation codes; null on failure).
    public static int[] animCodes(int animation, int weaponType) {
        if (!WeaponAnimation.isValid(weaponType)) {
            return null;
        }

        char[] codes = new char[2]; // [weapon, animation]
        if (animation >= AnimationType.TAKE_OUT.value()
                && animation <= AnimationType.FIRE_CONTINUOUS.value()) {
            codes[1] = (char) ('c' + (animation - AnimationType.TAKE_OUT.value()));
            if (weaponType == WeaponAnimation.NONE.value()) {
                return null;
            }

            codes[0] = critterWeaponCode(weaponType);
            return toIntArray(codes);
        } else if (animation == AnimationType.PRONE_TO_STANDING.value()) {
            codes[1] = 'h';
            codes[0] = 'c';
            return toIntArray(codes);
        } else if (animation == AnimationType.BACK_TO_STANDING.value()) {
            codes[1] = 'j';
            codes[0] = 'c';
            return toIntArray(codes);
        } else if (animation == AnimationType.CALLED_SHOT_PIC.value()) {
            codes[1] = 'a';
            codes[0] = 'n';
            return toIntArray(codes);
        } else if (animation >= AnimationType.FIRST_SF_DEATH_ANIM) {
            codes[1] = (char) ('a' + (animation - AnimationType.FIRST_SF_DEATH_ANIM));
            codes[0] = 'r';
            return toIntArray(codes);
        } else if (animation >= AnimationType.FIRST_KNOCKDOWN_AND_DEATH_ANIM) {
            codes[1] = (char) ('a' + (animation - AnimationType.FIRST_KNOCKDOWN_AND_DEATH_ANIM));
            codes[0] = 'b';
            return toIntArray(codes);
        } else if (animation == AnimationType.THROW_ANIM.value()) {
            if (weaponType == WeaponAnimation.KNIFE.value()) {
                codes[0] = 'd';
                codes[1] = 'm';
            } else if (weaponType == WeaponAnimation.SPEAR.value()) {
                codes[0] = 'g';
                codes[1] = 'm';
            } else {
                codes[0] = 'a';
                codes[1] = 's';
            }
            return toIntArray(codes);
        } else if (animation == AnimationType.DODGE_ANIM.value()) {
            if (weaponType == WeaponAnimation.NONE.value()) {
                codes[0] = 'a';
                codes[1] = 'n';
            } else {
                codes[0] = critterWeaponCode(weaponType);
                codes[1] = 'e';
            }
            return toIntArray(codes);
        }

        codes[1] = (char) ('a' + animation);
        if (animation <= AnimationType.WALK.value() && weaponType != WeaponAnimation.NONE.value()) {
            codes[0] = critterWeaponCode(weaponType);
            return toIntArray(codes);
        }
        codes[0] = 'a';

        return toIntArray(codes);
    }

    private static int[] toIntArray(char[] codes) {
        return new int[] { codes[0], codes[1] };
    }

    static char critterWeaponCode(int weaponType) {
        switch (weaponType) {
        case 11:
            return 's';
        case 12:
            return 'o';
        case 13:
            return 'p';
        case 14:
            return 'q';
        case 15:
            return 't';
        default:
            return (char) ('d' + (weaponType - 1));
        }
    }

    // art\heads\HEADS.LST-style fidget triplets live here after artInit.
    static String headPath(String base, String typeName, String fileName, int animType, int weaponCode) {
        String head1 = HEAD_CODES_1;
        String head2 = HEAD_CODES_2;
        char headSuffix = head2.charAt(animType);
        if (headSuffix == 'f') {
            return String.format("%sart\\%s\\%s%c%c%d.frm", base, typeName, fileName,
                    head1.charAt(animType), 102, weaponCode);
        }
        return String.format("%sart\\%s\\%s%c%c.frm", base, typeName, fileName,
                head1.charAt(animType), headSuffix);
    }

    // artInit minus the cache (wired in the cache phase).
    public static int artInit() {
        for (int objectType = ObjectType.FIRST; objectType < ObjectType.COUNT; objectType++) {
            ArtList list = artLists.descriptions.get(objectType);
            list.flags = 0;
            List<String> names = readList(cdPathBase + "art\\" + list.name + "\\" + list.name + ".lst");
            if (names == null) {
                Debug.print("art_read_lst failed in art_init\n");
                return -1;
            }
            list.fileNames.clear();
            list.fileNames.addAll(names);
            list.loaded = true;
        }

        // Critter alias + run tables.
        ArtList critters = artLists.descriptions.get(ObjectType.CRITTER.value());
        artLists.anonAlias.clear();
        artLists.shouldRunData.clear();
        for (int i = 0; i < critters.fileNames.size(); i++) {
            artLists.anonAlias.add(0);
            artLists.shouldRunData.add(0);
        }

        // SFALL: player model defaults (content config may be empty).
        String jumpsuitMale = configOr(DEFAULT_JUMPSUIT_MALE, "start", "model_male_default");
        String jumpsuitFemale = configOr(DEFAULT_JUMPSUIT_FEMALE, "start", "model_female_default");
        String tribalMale = configOr(DEFAULT_TRIBAL_MALE, "start", "model_male");
        String tribalFemale = configOr(DEFAULT_TRIBAL_FEMALE, "start", "model_female");

        ArtLists lists = artLists;
        lists.vaultGuyNum = 0;
        for (int i = 0; i < critters.fileNames.size(); i++) {
            String name = critters.fileNames.get(i);
            if (name.equalsIgnoreCase(jumpsuitMale)) {
                lists.vaultPersonNums[1][0] = i;
            } else if (name.equalsIgnoreCase(jumpsuitFemale)) {
                lists.vaultPersonNums[1][1] = i;
            }
            if (name.equalsIgnoreCase(tribalMale)) {
                lists.vaultPersonNums[0][0] = i;
                lists.vaultGuyNum = i;
            } else if (name.equalsIgnoreCase(tribalFemale)) {
                lists.vaultPersonNums[0][1] = i;
            }
        }

        List<String> critterLines = readLines(
                cdPathBase + "art\\" + critters.name + "\\" + critters.name + ".lst");
        if (critterLines == null) {
            Debug.print("Unable to open %s in art_init\n", "art\\critters\\critters.lst");
            return -1;
        }
        // NOTE: C re-reads the same .lst (names already consumed above);
        // alias/run data come from the comma suffixes per line index.
        for (int i = 0; i < critters.fileNames.size() && i < critterLines.size(); i++) {
            String line = critterLines.get(i);
            int sep1 = line.indexOf(',');
            if (sep1 >= 0) {
                lists.anonAlias.set(i, StringParsers.cAtoi(line, sep1 + 1));
                int sep2 = line.indexOf(',', sep1 + 1);
                if (sep2 >= 0) {
                    lists.shouldRunData.set(i, StringParsers.cAtoi(line, sep2 + 1));
                } else {
                    lists.shouldRunData.set(i, 0);
                }
            } else {
                lists.anonAlias.set(i, lists.vaultGuyNum);
                lists.shouldRunData.set(i, 1);
            }
        }

        // Mapper blank tile.
        ArtList tiles = artLists.descriptions.get(ObjectType.TILE.value());
        lists.mapperBlankTile = -1;
        for (int i = 0; i < tiles.fileNames.size(); i++) {
            if (tiles.fileNames.get(i).equalsIgnoreCase("grid001.frm")) {
                lists.mapperBlankTile = i;
            }
        }

        // Head fidget triplets.
        ArtList heads = artLists.descriptions.get(ObjectType.HEAD.value());
        List<String> headLines = readLines(cdPathBase + "art\\" + heads.name + "\\" + heads.name + ".lst");
        if (headLines == null) {
            Debug.print("Unable to open %s in art_init\n", "art\\heads\\heads.lst");
            return -1;
        }
        lists.headDescriptions.clear();
        for (int i = 0; i < heads.fileNames.size() && i < headLines.size(); i++) {
            // Mirrors the comma-chasing pointer windows in artInit; values
            // are atoi windows (the strpbrk NUL is a no-op for atoi).
            HeadDescription description = new HeadDescription();
            String line = headLines.get(i);
            int s1 = line.indexOf(',');
            int g = s1 >= 0 ? s1 + 1 : Math.min(1, line.length());
            int s2 = line.indexOf(',', s1 >= 0 ? s1 + 1 : 1);
            int n = s2 >= 0 ? s2 + 1 : g;
            int s3 = s2 >= 0 ? line.indexOf(',', s2 + 1) : -1;
            int b = s3 >= 0 ? s3 + 1 : n;
            description.goodFidgetCount = StringParsers.cAtoi(line, Math.min(g, line.length()));
            description.neutralFidgetCount = StringParsers.cAtoi(line, Math.min(n, line.length()));
            description.badFidgetCount = StringParsers.cAtoi(line, Math.min(b, line.length()));
            lists.headDescriptions.add(description);
        }

        return 0;
    }

    private static String configOr(String def, String section, String key) {
        String[] value = new String[1];
        if (Config.configGetString(Message.contentConfig, section, key, value)) {
            return value[0];
        }
        return def;
    }

    // .lst reader: one token per line (cut at " ,;\\r\\t\\n"), 12 chars max.
    static List<String> readList(String path) {
        List<String> lines = readLines(path);
        if (lines == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (String line : lines) {
            int end = line.length();
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == ' ' || ch == ',' || ch == ';' || ch == '\r' || ch == '\t' || ch == '\n') {
                    end = i;
                    break;
                }
            }
            String token = line.substring(0, end);
            if (token.length() > NAME_SIZE - 1) {
                token = token.substring(0, NAME_SIZE - 1);
            }
            names.add(token);
        }
        return names;
    }

    static List<String> readLines(String path) {
        XFile.Handle stream = Db.fileOpen(path, "rt");
        if (stream == null) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = Db.fileReadString(stream, 200)) != null) {
            lines.add(line);
        }
        Db.fileClose(stream);
        return lines;
    }

    public static void artReset() {
    }

    public static void artExit() {
        artLists.descriptions.clear();
        artLists.anonAlias.clear();
        artLists.shouldRunData.clear();
        artLists.headDescriptions.clear();
    }

    /** Test seam: clear lists state (C keeps globals across runs). */
    static void resetForTests() {
        artLists.descriptions.clear();
        artLists.anonAlias.clear();
        artLists.shouldRunData.clear();
        artLists.headDescriptions.clear();
        for (String name : ArtLists.TYPE_NAMES) {
            ArtList list = new ArtList();
            list.name = name;
            artLists.descriptions.add(list);
        }
        cdPathBase = "";
    }
}
