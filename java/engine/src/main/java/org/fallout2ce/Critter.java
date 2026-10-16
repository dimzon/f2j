package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Critter id tables, mirroring the pure parts of {@code src/critter.h},
 * plus the body/kill read slice of {@code src/critter.cc}:
 * {@code critterGetBodyType} and {@code critterGetKillType}, and the
 * critter-vitals slice: {@code critterGetHitPoints},
 * {@code critterAdjustHitPoints}, {@code critterGetPoison},
 * {@code critterAdjustPoison}, {@code critterGetRadiation},
 * {@code critterAdjustRadiation}.
 *
 * <p>BodyType/KillType enums live in {@link ProtoTypes} (reused, not
 * duplicated). The live hp/poison/radiation reads duplicate the
 * {@link Stat} engine copies on purpose: the stat paths
 * ({@code critterGetStat} {@code CURRENT_*}) route through
 * {@link Stat#critterGetHitPoints} et al, while callers of the critter
 * API use these. Maximum HP comes from
 * {@link Stat#critterGetStat} (clamped 0 headless, see below).
 *
 * <p>Vectors: {@code [explosives]} body/kill reads in
 * {@code docker/refharness/golden/v65.txt} (produced by
 * {@code sectionExplosives} in {@code docker/refharness/ref_main.cc}),
 * and {@code [vitals]} in {@code docker/refharness/golden/v66.txt}
 * (produced by {@code sectionVitals} in
 * {@code docker/refharness/ref_main.cc}).
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

    // critter_get_hits (src/critter.cc 0x42D18C): non-critter -> 0, else
    // the live hp field. Critter-ness is tested via ProtoId.objectType
    // (mirrors Stat#critterGetHitPoints); fresh objects are zero-init by
    // ObjectSystem, so a fresh dog reads 0.
    public static int critterGetHitPoints(GameObject critter) {
        if (critter == null) {
            return 0;
        }
        if (ProtoId.ofGameObject(critter).objectType()
                != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }
        return critter.data.critter.hp;
    }

    // critter_adjust_hits (src/critter.cc 0x42D1A4): non-critter -> 0;
    // else hp += amount, then if maximumHp >= newHp keep it (killing at
    // newHp <= 0 unless already dead), else clamp to maximumHp.
    // Maximum HP comes from Stat#critterGetStat (clamped 0 headless, so
    // the vector adjust (+5 on hp-0 dog) takes the else branch: hp = 0).
    // Headless trace: maximumHp(0) >= newHp(5)? No -> hp = maximumHp = 0.
    public static int critterAdjustHitPoints(GameObject critter, int hp) {
        if (critter == null) {
            return 0;
        }
        if (ProtoId.ofGameObject(critter).objectType()
                != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }

        int maximumHp = Stat.critterGetStat(critter,
                StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id());
        int newHp = critter.data.critter.hp + hp;

        critter.data.critter.hp = newHp;
        if (maximumHp >= newHp) {
            if (newHp <= 0
                    && (critter.data.critter.combat.results.bits()
                            & Dam.DEAD.bits()) == Dam.NONE.bits()) {
                critterKill(critter);
            }
        } else {
            critter.data.critter.hp = maximumHp;
        }

        return 0;
    }

    // critter_get_poison (src/critter.cc 0x42D1F8): non-critter -> 0.
    public static int critterGetPoison(GameObject critter) {
        if (critter == null) {
            return 0;
        }
        if (ProtoId.ofGameObject(critter).objectType()
                != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }
        return critter.data.critter.poison;
    }

    // critter_adjust_poison (src/critter.cc 0x42D210): dude-only,
    // non-dude -> -1 before any queue/message side effects. The C++
    // MessageListItem declaration has no Java equivalent (game-phase
    // message stack) and is a no-op. The dude tail (poison-resistance
    // scaling, event queue, monitor messages, indicator refresh) comes
    // with the game phase; the vectors stop at the dude gate.
    public static int critterAdjustPoison(GameObject critter, int amount) {
        if (!isDude(critter)) {
            return -1;
        }
        return 0;
    }

    // critter_get_rads (src/critter.cc 0x42D38C): non-critter -> 0.
    public static int critterGetRadiation(GameObject obj) {
        if (obj == null) {
            return 0;
        }
        if (ProtoId.ofGameObject(obj).objectType()
                != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }
        return obj.data.critter.radiation;
    }

    // critter_adjust_rads (src/critter.cc 0x42D3A4): dude-only,
    // non-dude -> -1 before the hook and all side effects. The C++
    // MessageListItem declaration is a no-op (see critterAdjustPoison).
    // The dude tail (hook, radiation-resistance scaling, geiger/monitor
    // messages, indicator refresh) comes with the game phase; the
    // vectors stop at the dude gate.
    public static int critterAdjustRadiation(GameObject obj, int amount) {
        if (!isDude(obj)) {
            return -1;
        }

        amount = scriptHooksAdjustRads(obj, amount);

        return 0;
    }

    // Seam for scriptHooks_AdjustRads (src/sfall_script_hooks.cc): with no
    // hooks registered the C++ call returns the amount unchanged, so the
    // headless default is identity. Dormant: unreachable while the dude
    // gate above holds (no dude headless).
    private static int scriptHooksAdjustRads(GameObject critter, int amount) {
        return amount;
    }

    // Seam for critterKill(critter, ANIM_INVALID, true) (src/critter.cc,
    // game phase: death animation/flags/rect refresh). The kill branch
    // needs maximumHp >= newHp AND newHp <= 0, which is unreachable
    // headless (maximumHp is 0, so any lethal adjust overshoots into the
    // clamp branch); kept as a seam so the branch stays transcribed.
    private static void critterKill(GameObject critter) {
    }

    // Port of gDude (src/object.cc, unported): null headless, so every
    // critter == gDude check reads false for non-null critters (a null
    // critter is never "the dude"; see Stat#isDude).
    private static final GameObject G_DUDE = null;

    private static boolean isDude(GameObject critter) {
        return critter != null && critter == G_DUDE;
    }
}
