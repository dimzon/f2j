package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Critter id tables, mirroring the pure parts of {@code src/critter.h},
 * plus the body/kill read slice of {@code src/critter.cc}:
 * {@code critterGetBodyType} and {@code critterGetKillType}.
 *
 * <p>BodyType/KillType enums live in {@link ProtoTypes} (reused, not
 * duplicated).
 *
 * <p>Vectors: {@code [explosives]} body/kill reads in
 * {@code docker/refharness/golden/v65.txt} (produced by
 * {@code sectionExplosives} in {@code docker/refharness/ref_main.cc}).
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>{@code killTypeGetName}/{@code killTypeGetDescription} are omitted:
 * they need the proto message stack (game phase).</li>
 * <li>{@code gDude} is modelled as a {@code null} holder (no dude in
 * {@link ObjectSystem} yet): the dude-gender branch of
 * {@code critterGetKillType} is dormant headless (see {@link Stat}/
 * {@link Skill} for the same rule); the {@code null == null} C quirk is
 * avoided via a null guard in {@link #isDude}.</li>
 * <li>Null {@code critterGetBodyType} input returns BIPED silently (C++
 * debugPrints; Java ports stay silent, see {@link Reaction}).</li>
 * </ul>
 */
public final class Critter {

    private Critter() {
    }

    public static final int RADIATION_LEVEL_COUNT = 6;
    public static final int RADIATION_LEVEL_FIRST = 0;

    public enum RadiationLevel {
        RADIATION_LEVEL_NONE(0),
        RADIATION_LEVEL_MINOR(1),
        RADIATION_LEVEL_ADVANCED(2),
        RADIATION_LEVEL_CRITICAL(3),
        RADIATION_LEVEL_DEADLY(4),
        RADIATION_LEVEL_FATAL(5);

        private final int id;

        RadiationLevel(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public RadiationLevel next() {
            return Critter.ofRadiationLevel(id + 1);
        }
    }

    private static final Map<Integer, RadiationLevel> RADIATION_BY_ID = new HashMap<>();

    static {
        for (RadiationLevel e : RadiationLevel.values()) {
            RADIATION_BY_ID.put(e.id, e);
        }
    }

    public static RadiationLevel ofRadiationLevel(int id) {
        return RADIATION_BY_ID.get(id);
    }

    // CritterFrameId only knows Invalid/Reserved/First; the Goris ids are
    // functional casts in C++, kept as plain ints here.
    public static final int GORIS_COMBAT_BASE_FRAME_ID = 100;
    public static final int GORIS_ROBE_BASE_FRAME_ID = 99;

    // critter_body_type (src/critter.cc 0x42DDC4): null or non-critter ->
    // BIPED, else the proto body type. C++ skips the proto-miss check
    // (vectors always load).
    public static ProtoTypes.BodyType critterGetBodyType(GameObject critter) {
        if (critter == null) {
            return ProtoTypes.BodyType.BODY_TYPE_BIPED;
        }

        ProtoId protoId = ProtoId.ofGameObject(critter);
        if (protoId.objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return ProtoTypes.BodyType.BODY_TYPE_BIPED;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(protoId, slot);

        return slot[0].critter.data.bodyType;
    }

    // critter_kill_count_type (src/critter.cc 0x42D920): the dude reads the
    // gender stat (MAN/WOMAN; dormant headless, gDude is null), non-critter
    // -> INVALID, else the proto kill type. C++ skips the proto-miss check
    // (vectors always load).
    public static ProtoTypes.KillType critterGetKillType(GameObject critter) {
        if (isDude(critter)) {
            // C GENDER_FEMALE is 1 (src/proto_types.h); the Java Gender
            // enum keeps the same order (MALE 0, FEMALE 1).
            if (Stat.critterGetStat(critter, StatDefs.Stat.STAT_GENDER.id())
                    == Gender.FEMALE.ordinal()) {
                return ProtoTypes.KillType.KILL_TYPE_WOMAN;
            }
            return ProtoTypes.KillType.KILL_TYPE_MAN;
        }

        ProtoId protoId = ProtoId.ofGameObject(critter);
        if (protoId.objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return ProtoTypes.KillType.KILL_TYPE_INVALID;
        }

        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(protoId, slot);

        return slot[0].critter.data.killType;
    }

    // Port of gDude (src/object.cc, unported): null headless, so every
    // critter == gDude check reads false for non-null critters (a null
    // critter is never "the dude"; see Stat#isDude).
    private static final GameObject G_DUDE = null;

    private static boolean isDude(GameObject critter) {
        return critter != null && critter == G_DUDE;
    }
}
