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
 * {@code critterGetBodyType} and {@code critterGetKillType}, the
 * critter-vitals slice: {@code critterGetHitPoints},
 * {@code critterAdjustHitPoints}, {@code critterGetPoison},
 * {@code critterAdjustPoison}, {@code critterGetRadiation},
 * {@code critterAdjustRadiation}, and the movement slice:
 * {@code critterGetMovementPointCostAdjustedForCrippledLegs} and
 * {@code critterIsEncumbered}, plus the weapon-use gate
 * {@code critterCanUseWeapon}.
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
 * {@code docker/refharness/ref_main.cc}), and {@code [miscgetters]} in
 * {@code docker/refharness/golden/v81.txt} (produced by
 * {@code sectionMiscGetters} in {@code docker/refharness/ref_main.cc}),
 * and {@code [aidec]} in {@code docker/refharness/golden/v164.txt} for
 * {@code critterCanUseWeapon} / {@code critterSetWhoHitMe}.
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>{@code killTypeGetName}/{@code killTypeGetDescription} are omitted:
 * they need the proto message stack (game phase).</li>
 * <li>{@code gDude} is the player that the object system made.</li>
 * <li>Null {@code critterGetBodyType} input returns BIPED silently (C++
 * debugPrints; Java ports stay silent, see {@link Reaction}).</li>
 * <li>{@code scriptHooks_MoveCost} arrives as a dormant identity seam
 * (no script hooks headless; see {@link Item} for the same convention
 * with {@code scriptHooks_CalcApCost}).</li>
 * <li>{@code critterIsEncumbered} reads capacity via
 * {@link Stat#critterGetStat} ({@code STAT_CARRY_WEIGHT}, clamped 0
 * headless like every saveable stat) against the live
 * {@link Item#objectGetInventoryWeight}: fresh objects carry nothing,
 * so the vector reads {@code 0 < 0} = false.</li>
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

    // critter_get_exp (src/critter.cc 0x42DCB8): the proto's experience.
    public static int critterGetExp(GameObject critter) {
        ProtoId protoId = ProtoId.ofGameObject(critter);
        Proto[] slot = new Proto[1];
        if (ProtoDb.protoGetProto(protoId, slot) == -1) {
            return 0;
        }
        return slot[0].critter.data.experience;
    }

    // critter_flag_check (src/critter.cc 0x42E688): proto flags of a
    // critter proto.
    public static boolean critterFlagCheck(GameObject critter, ObjTypes.CritterFlags flag) {
        ProtoId protoId = ProtoId.ofGameObject(critter);
        if (!protoId.valid() || protoId.objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return false;
        }
        Proto[] slot = new Proto[1];
        if (ProtoDb.protoGetProto(protoId, slot) == -1) {
            return false;
        }
        return (slot[0].critter.data.flags.bits() & flag.bits()) != 0;
    }

    // critter_set_who_hit_me (src/critter.cc 0x42E5B4). The reaction tail
    // for a dude attacker is a game-phase seam; the intelligence roll is
    // taken the way the original does.
    public static int critterSetWhoHitMe(GameObject critter, GameObject hitMe) {
        if (critter == null) {
            return -1;
        }
        if (hitMe != null && FrmId.ofFid(hitMe.fid).objectType() != ObjectType.CRITTER) {
            return -1;
        }
        if (ProtoId.ofGameObject(critter).objectType() == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            if (hitMe == null
                    || critter.data.critter.combat.team != hitMe.data.critter.combat.team
                    || (Stat.statRoll(critter, StatDefs.Stat.STAT_INTELLIGENCE.id(), -1,
                            null).ordinal() < 2
                        && (!PartyMembers.objectIsPartyMember(critter)
                            || !PartyMembers.objectIsPartyMember(hitMe)))) {
                critter.data.critter.combat.whoHitMe = hitMe;
                if (hitMe == ObjectSystem.getDude()) {
                    // reactionSetValue(critter, -3): game-phase seam.
                }
            }
        }
        return 0;
    }

    // critterCanUseWeapon (src/critter.cc): null/non-weapon false, both
    // arms crippled false, one arm crippled plus a two-handed weapon
    // false, otherwise the critter's fire animation art must exist.
    public static boolean critterCanUseWeapon(GameObject critter, GameObject weapon, int hitMode) {
        if (critter == null || weapon == null
                || Item.itemGetType(weapon) != ProtoTypes.ItemType.ITEM_TYPE_WEAPON) {
            return false;
        }

        int damageFlags = critter.data.critter.combat.results.bits();
        if ((damageFlags & Dam.CRIP_ARM_ANY.bits()) == Dam.CRIP_ARM_ANY.bits()) {
            return false;
        }

        if ((damageFlags & Dam.CRIP_ARM_ANY.bits()) != Dam.NONE.bits()
                && Item.weaponIsTwoHanded(weapon)) {
            return false;
        }

        int rotation = critter.rotation.id() + 1;
        int animationCode = Item.weaponGetAnimationCode(weapon);
        int weaponAnimationCode = Item.weaponGetAnimationForHitMode(weapon, hitMode);
        FrmId base = FrmId.ofFid(critter.fid);
        FrmId frmId = FrmId.ofObject(base.objectType().value(), base.frameId(),
                weaponAnimationCode, animationCode, rotation);
        return frmId.exist();
    }

    // KNOCKDOWN_AND_DEATH / SF_DEATH anim ranges (src/animation_defs.h).
    /** FIRST_KNOCKDOWN_AND_DEATH_ANIM = ANIM_FALL_BACK. */
    public static final int FIRST_KNOCKDOWN_AND_DEATH_ANIM = 20;
    /** LAST_KNOCKDOWN_AND_DEATH_ANIM = ANIM_FALL_FRONT_BLOOD. */
    public static final int LAST_KNOCKDOWN_AND_DEATH_ANIM = 35;
    /** FIRST_SF_DEATH_ANIM = ANIM_FALL_BACK_SF. */
    public static final int FIRST_SF_DEATH_ANIM = 48;
    /** LAST_SF_DEATH_ANIM = ANIM_FALL_FRONT_BLOOD_SF. */
    public static final int LAST_SF_DEATH_ANIM = 63;

    /** DUDE_STATE_* (src/obj_types.h); the first three are what this slice reads. */
    public static final int DUDE_STATE_SNEAKING = 0;
    public static final int DUDE_STATE_POISONED = 1;
    public static final int DUDE_STATE_RADIATED = 2;
    public static final int DUDE_STATE_LEVEL_UP_AVAILABLE = 3;
    public static final int DUDE_STATE_ADDICTED = 4;
    public static final int DUDE_STATE_COUNT = 5;
    public static final int DUDE_STATE_FIRST = DUDE_STATE_SNEAKING;

    /** POISON_INDICATOR_THRESHOLD (src/interface.h). */
    public static final int POISON_INDICATOR_THRESHOLD = 0;

    /** RADATION_INDICATOR_THRESHOLD (src/interface.h, typo is upstream's). */
    public static final int RADATION_INDICATOR_THRESHOLD = 65;

    /** Port of {@code dudeStateIsValid} (src/obj_types.h). */
    public static boolean dudeStateIsValid(int state) {
        return state >= DUDE_STATE_FIRST && state < DUDE_STATE_COUNT;
    }

    /**
     * Port of {@code critterIsDead} (0x42DD30): non-critter false; HP at or
     * below zero true; the {@code DAM_DEAD} bit true; otherwise false.
     */
    public static boolean critterIsDead(GameObject critter) {
        if (ProtoTypes.ProtoId.ofGameObject(critter).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return false;
        }

        if (critterGetHitPoints(critter) <= 0) {
            return true;
        }

        return !critter.data.critter.combat.results.and(Dam.DEAD).equals(Dam.NONE);
    }

    /** Port of {@code critterIsCrippled} (0x42DD58): the {@code DAM_CRIP} bit. */
    public static boolean critterIsCrippled(GameObject critter) {
        if (ProtoTypes.ProtoId.ofGameObject(critter).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return false;
        }

        return !critter.data.critter.combat.results.and(Dam.CRIP).equals(Dam.NONE);
    }

    /**
     * Port of {@code critterIsActive} (0x42DCDC): non-critter false; any of
     * knocked-out, dead or lose-turn set false; otherwise the dead bit
     * decides. Hit points play no part here, unlike in critterIsDead.
     */
    public static boolean critterIsActive(GameObject critter) {
        if (ProtoTypes.ProtoId.ofGameObject(critter).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return false;
        }

        int results = critter.data.critter.combat.results.bits();
        if ((results & (Dam.KNOCKED_OUT.bits() | Dam.DEAD.bits())) != 0) {
            return false;
        }

        if ((results & (Dam.KNOCKED_OUT.bits() | Dam.DEAD.bits() | Dam.LOSE_TURN.bits())) != 0) {
            return false;
        }

        return critter.data.critter.combat.results.and(Dam.DEAD).equals(Dam.NONE);
    }

    /**
     * Port of {@code critterIsProne} (0x42DD80): non-critter false; then the
     * knockdown bits, or the animation inside either death range.
     */
    public static boolean critterIsProne(GameObject critter) {
        if (ProtoTypes.ProtoId.ofGameObject(critter).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return false;
        }

        int anim = FrmId.ofFid(critter.fid).animationType();

        return (critter.data.critter.combat.results.bits() & (Dam.KNOCKED_OUT.bits() | Dam.KNOCKED_DOWN.bits())) != 0
            || (anim >= FIRST_KNOCKDOWN_AND_DEATH_ANIM && anim <= LAST_KNOCKDOWN_AND_DEATH_ANIM)
            || (anim >= FIRST_SF_DEATH_ANIM && anim <= LAST_SF_DEATH_ANIM);
    }

    /**
     * Port of {@code dudeHasState} (0x42E2F8) for the states this slice can
     * reach: POISONED and RADIATED read critter poison / radiation; the rest
     * are critter flag bits set by critterInit, which is game phase.
     */
    public static boolean dudeHasState(int state) {
        if (!dudeStateIsValid(state)) {
            return false;
        }

        GameObject dude = ObjectSystem.getDude();
        if (dude == null) {
            return false;
        }

        if (state == DUDE_STATE_POISONED) {
            return critterGetPoison(dude) > POISON_INDICATOR_THRESHOLD;
        }
        if (state == DUDE_STATE_RADIATED) {
            return critterGetRadiation(dude) > RADATION_INDICATOR_THRESHOLD;
        }

        // NOTE: sneaking / level-up / addicted are CRITTER_DUDE_* flag bits.
        // critterInit (game phase) owns them, so they read false headless,
        // exactly like C without it.
        return false;
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
                critterKill(critter, -1, true);
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

    // critter_compute_ap_from_distance (src/critter.cc 0x42E62C):
    // non-critter -> 0, else 8x distance with both legs crippled, 4x
    // with either leg crippled, 1x otherwise, then the sfall MoveCost
    // hook (identity headless, see below). Headless trace: the fresh
    // dog's results are DAM_NONE -> distance (5).
    public static int critterGetMovementPointCostAdjustedForCrippledLegs(GameObject critter, int distance) {
        if (critter == null) {
            return 0;
        }
        if (ProtoId.ofGameObject(critter).objectType()
                != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }

        int flags = critter.data.critter.combat.results.bits();
        int actionPoints;
        if ((flags & Dam.CRIP_LEG_LEFT.bits()) != Dam.NONE.bits()
                && (flags & Dam.CRIP_LEG_RIGHT.bits()) != Dam.NONE.bits()) {
            actionPoints = 8 * distance;
        } else if ((flags & Dam.CRIP_LEG_ANY.bits()) != Dam.NONE.bits()) {
            actionPoints = 4 * distance;
        } else {
            actionPoints = distance;
        }

        return scriptHooksMoveCost(critter, distance, actionPoints);
    }

    // critter_is_overloaded (src/critter.cc 0x42E66C): carry capacity via
    // Stat#critterGetStat (STAT_CARRY_WEIGHT) against the live
    // Item#objectGetInventoryWeight. Headless trace: 0 < 0 is false (the
    // capacity clamps to 0 like every saveable stat, and the fresh dog
    // carries nothing).
    public static boolean critterIsEncumbered(GameObject critter) {
        int maxWeight = Stat.critterGetStat(critter, StatDefs.Stat.STAT_CARRY_WEIGHT.id());
        int currentWeight = Item.objectGetInventoryWeight(critter);
        return maxWeight < currentWeight;
    }

    // Seam for scriptHooks_MoveCost (src/sfall_script_hooks.cc): with no
    // hooks registered the C++ call returns the input cost unchanged, so
    // the headless default is identity (same convention as
    // Item#scriptHooksCalcApCost). Dormant: no script engine headless.
    private static int scriptHooksMoveCost(GameObject critter, int distance, int actionPoints) {
        return actionPoints;
    }

    // critter_kill (src/critter.cc 0x42DA64). The animation/fid, the light
    // and the rect refresh are render (game phase); the dude death-ending
    // tail needs the game. What stays is the observable state: hp zero, the
    // dead bit, party removal, death script removal, hidden-item destruction
    // and the drug-event clear.
    public static void critterKill(GameObject critter, int anim, boolean refreshRect) {
        if (ProtoId.ofGameObject(critter).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return;
        }

        critter.data.critter.hp = 0;
        critter.data.critter.combat.results = critter.data.critter.combat.results.or(Dam.DEAD);

        PartyMembers.partyMemberRemove(critter);

        // scriptHooks_OnDeath(critter): identity headless.
        // Death animation/fid selection, flat/no-block toggles and the
        // light-off are render (game phase, deferred).
        // queueClearByEventType(EVENT_TYPE_DRUG, _critterClearObjDrugs):
        // drug events are game phase yet the queue is here, so the events
        // are cleared the same way (the C callback matches the cleared
        // object).
        EventQueue.queueClearByEventType(EventQueue.EventType.DRUG, null);

        Item.itemDestroyAllHidden(critter);

        if (critter.sid != -1) {
            ObjectSystem.scriptRemove(critter.sid);
            critter.sid = -1;
        }
    }

    // Port of gDude (src/object.cc, unported): null headless, so every
    // critter == gDude check reads false for non-null critters (a null
    // critter is never "the dude"; see Stat#isDude).
    /**
     * The player of the game ({@code gDude} in C). The object system makes
     * it with the objects of the game.
     */
    private static GameObject gDude() {
        return ObjectSystem.getDude();
    }

    private static boolean isDude(GameObject critter) {
        return critter != null && critter == gDude();
    }
}
