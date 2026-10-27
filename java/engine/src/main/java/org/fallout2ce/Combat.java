package org.fallout2ce;

import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * Hit-location accuracy penalties, target highlight, and the combat-id
 * search, mirroring the pure slice of {@code src/combat.cc}.
 *
 * <p>Only the data-table accessors are ported
 * ({@code combat_get_hit_location_penalty},
 * {@code combat_set_hit_location_penalty},
 * {@code combat_reset_hit_location_penalty}, {@code combatGetTargetHighlight})
 * plus the stateless linear scan {@code _find_cid}.
 * {@code combatInit} is deliberately not ported: it touches {@code gDude} and
 * segfaults headless. The tables therefore start zeroed (BSS mirror), exactly
 * as the harness observes before {@code combat_reset_hit_location_penalty}
 * restores the compiled defaults.
 */
public final class Combat {

    private Combat() {
    }

    /**
     * Compiled defaults, mirroring {@code hit_location_penalty_default}
     * ({@code src/combat.cc}), index = hit location id.
     */
    private static final int[] HIT_LOCATION_PENALTY_DEFAULT = {
        -40, // HEAD
        -30, // LEFT_ARM
        -30, // RIGHT_ARM
        0, // TORSO
        -20, // RIGHT_LEG
        -20, // LEFT_LEG
        -60, // EYES
        -30, // GROIN
        0, // UNCALLED
    };

    /** {@code hit_location_penalty} (BSS: starts zeroed). */
    private static final int[] hitLocationPenalty = new int[CombatDefs.HIT_LOCATION_COUNT];

    /** {@code _combat_highlight} (BSS 0). */
    private static int combatHighlight;

    /**
     * Port of {@code combat_get_hit_location_penalty}: an invalid hit location
     * yields 0.
     */
    public static int getHitLocationPenalty(int hitLocation) {
        if (CombatDefs.hitLocationIsValid(hitLocation)) {
            return hitLocationPenalty[hitLocation];
        }

        return 0;
    }

    /**
     * Port of {@code combat_set_hit_location_penalty}: an invalid hit location
     * is ignored.
     */
    public static void setHitLocationPenalty(int hitLocation, int penalty) {
        if (CombatDefs.hitLocationIsValid(hitLocation)) {
            hitLocationPenalty[hitLocation] = penalty;
        }
    }

    /** Port of {@code combat_reset_hit_location_penalty}. */
    public static void resetHitLocationPenalty() {
        for (int hitLocation = CombatDefs.HIT_LOCATION_FIRST;
                hitLocation < CombatDefs.HIT_LOCATION_COUNT; hitLocation++) {
            hitLocationPenalty[hitLocation] = HIT_LOCATION_PENALTY_DEFAULT[hitLocation];
        }
    }

    /** Port of {@code combatGetTargetHighlight}. */
    public static int getTargetHighlight() {
        return combatHighlight;
    }

    /**
     * Port of {@code _find_cid} ({@code src/combat.cc:0x420E24}): pure linear
     * scan returning the first index {@code >= startIndex} whose
     * {@code cid} matches, else {@code critterListLength}. Fresh objects
     * carry {@code cid -1} ({@code src/object.cc} {@code obj_allocate}),
     * so a fresh list matches at 0 and a missing cid yields the length.
     */
    public static int findCid(int startIndex, int cid, GameObject[] critterList, int critterListLength) {
        int index;
        for (index = startIndex; index < critterListLength; index++) {
            if (critterList[index].cid == cid) {
                break;
            }
        }
        return index;
    }

    /**
     * Port of {@code attackDetermineToHit} (0x424394): the chance to hit
     * in percent.
     *
     * <p>The weapon's skill (or unarmed) is the base; the range penalty of
     * a ranged or thrown attack and its perception bonus, the two-handed
     * and minimum-strength modifiers for the dude (dormant headless: no
     * dude), the target's armor class, the hit-location penalty, the
     * multihex bonus, the light of the target for the dude (dormant),
     * and the blind/knocked states are applied the way the original does.
     * The combat-difficulty team branch is headless-neutral (no combat
     * data), the accuracy bonus of a combat start is not loaded, and the
     * script hook is identity.
     */
    public static int attackDetermineToHit(ObjTypes.GameObject attacker, int tile,
            ObjTypes.GameObject defender, int hitLocation, int hitMode,
            boolean useDistance) {
        ObjTypes.GameObject weapon = Item.getWeaponForHitMode(attacker, hitMode);

        boolean targetIsCritter = defender != null
            && ProtoTypes.ProtoId.ofGameObject(defender).objectType()
                == ObjTypes.ObjectType.OBJ_TYPE_CRITTER;

        boolean isRangedWeapon = false;

        int toHit;
        if (weapon == null || isUnarmedHitMode(hitMode)) {
            toHit = Skill.skillGetValue(attacker, SkillDefs.Skill.SKILL_UNARMED.id());
        } else {
            toHit = Item.weaponGetSkillValue(attacker, hitMode);

            int attackType = Item.weaponGetAttackTypeForHitMode(weapon, hitMode);
            if (attackType == Item.AttackType.ATTACK_TYPE_RANGED.id()
                    || attackType == Item.AttackType.ATTACK_TYPE_THROW.id()) {
                isRangedWeapon = true;

                int perceptionBonusMult = 2;
                int minEffectiveDist = 0;
                int perception = Stat.critterGetStat(attacker, StatDefs.Stat.STAT_PERCEPTION.id());

                int distanceMod = 0;
                if (defender != null && useDistance) {
                    distanceMod = TileMath.tileDistanceBetween(tile, defender.tile);
                }

                if (distanceMod >= minEffectiveDist) {
                    distanceMod -= perceptionBonusMult * perception;
                } else {
                    distanceMod += minEffectiveDist;
                }

                if (distanceMod < -2 * perception) {
                    distanceMod = -2 * perception;
                }

                distanceMod *= -4;

                toHit += distanceMod;
            }

            if (isDude(attacker) && TraitDefs.traitIsSelectedAndActive(TraitDefs.Trait.TRAIT_ONE_HANDER.id())) {
                if (Item.weaponIsTwoHanded(weapon)) {
                    toHit -= 40;
                } else {
                    toHit += 20;
                }
            }

            int minStrength = Item.weaponGetMinStrengthRequired(weapon);
            int minStrengthMod = minStrength - Stat.critterGetStat(attacker, StatDefs.Stat.STAT_STRENGTH.id());
            if (minStrengthMod > 0) {
                toHit -= 20 * minStrengthMod;
            }
        }

        if (targetIsCritter && defender != null) {
            int armorClass = Stat.critterGetStat(defender, StatDefs.Stat.STAT_ARMOR_CLASS.id());
            armorClass += Item.weaponGetAmmoArmorClassModifier(weapon);
            if (armorClass < 0) {
                armorClass = 0;
            }
            toHit -= armorClass;
        }

        int penalty = Combat.getHitLocationPenalty(hitLocation);
        toHit += isRangedWeapon ? penalty : penalty / 2;

        if (defender != null && (defender.flags.bits() & ObjTypes.ObjectFlags.MULTIHEX.bits()) != 0) {
            toHit += 15;
        }

        if (isDude(attacker) && defender != null) {
            int lightIntensity = ObjectSystem.objectGetLightIntensity(defender);
            if (lightIntensity <= 26214) {
                toHit -= 40;
            } else if (lightIntensity <= 39321) {
                toHit -= 25;
            } else if (lightIntensity <= 52428) {
                toHit -= 10;
            }
        }

        if ((attacker.data.critter.combat.results.bits()
                & ObjTypes.Dam.BLIND.bits()) != 0) {
            toHit -= 25;
        }

        if (targetIsCritter && defender != null
                && (defender.data.critter.combat.results.bits()
                    & (ObjTypes.Dam.KNOCKED_OUT.bits() | ObjTypes.Dam.KNOCKED_DOWN.bits())) != 0) {
            toHit += 40;
        }

        if (gcsd != null) {
            toHit += gcsd.accuracyBonus;
        }
        if (toHit > 95) {
            toHit = 95;
        }

        return toHit;
    }

    /**
     * Port of {@code _determine_to_hit} (0x42436C): the primary-hit-mode
     * wrapper around {@link #attackDetermineToHit}.
     */
    public static int determineToHit(ObjTypes.GameObject attacker,
            ObjTypes.GameObject defender, int hitLocation, int hitMode) {
        return attackDetermineToHit(attacker, attacker.tile, defender, hitLocation,
                hitMode, true);
    }

    /**
     * Port of {@code _combat_safety_invalidate_weapon} (0x4213E8): whether a
     * weapon is unsafe to fire because it could hit a friendly in the blast or
     * the burst/continuous spray. {@code safeDistancePtr} (single-element
     * array) receives the safety distance for explosive weapons when only the
     * attacker is at risk; a null pointer skips that branch.
     */
    public static boolean combatSafetyInvalidateWeapon(ObjTypes.GameObject attacker,
            ObjTypes.GameObject weapon, int hitMode, ObjTypes.GameObject defender,
            int[] safeDistancePtr) {
        return combatSafetyInvalidateWeaponFunc(attacker, weapon, hitMode, defender,
                safeDistancePtr, null);
    }

    /** {@code _combatTestIncidentalHit} (src/combat.cc:2423). */
    static boolean combatTestIncidentalHit(ObjTypes.GameObject attacker,
            ObjTypes.GameObject defender, ObjTypes.GameObject attackerFriend,
            ObjTypes.GameObject weapon) {
        return combatSafetyInvalidateWeaponFunc(attacker, weapon,
            CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), defender, null, attackerFriend);
    }

    private static boolean combatSafetyInvalidateWeaponFunc(ObjTypes.GameObject attacker,
            ObjTypes.GameObject weapon, int hitMode, ObjTypes.GameObject defender,
            int[] safeDistancePtr, ObjTypes.GameObject attackerFriend) {
        if (safeDistancePtr != null) {
            safeDistancePtr[0] = 0;
        }

        if (ProtoTypes.ProtoId.ofGameObject(attacker)
                .matchesCritterType(ProtoTypes.CritterProtoTypeId.AutoCannon)) {
            return false;
        }

        int intelligence = Stat.critterGetStat(attacker, StatDefs.Stat.STAT_INTELLIGENCE.id());
        int team = attacker.data.critter.combat.team;
        int damageRadius = Item.weaponGetDamageRadius(weapon, hitMode);
        int[] maxDamagePtr = new int[1];
        Item.weaponGetDamageMinMax(weapon, null, maxDamagePtr);
        int maxDamage = maxDamagePtr[0];
        ProtoTypes.DamageType damageType = Item.weaponGetDamageType(attacker, weapon);

        if (damageRadius > 0) {
            if (intelligence < 5) {
                damageRadius -= 5 - intelligence;
                if (damageRadius < 0) {
                    damageRadius = 0;
                }
            }

            if (attackerFriend != null) {
                if (ObjectSystem.objectGetDistanceBetween(defender, attackerFriend) < damageRadius) {
                    Debug.print("Friendly was in the way!");
                    return true;
                }
            }

            for (int index = 0; index < listTotal; index++) {
                GameObject candidate = combatList[index];
                if (candidate.data.critter.combat.team == team
                        && candidate != attacker
                        && candidate != defender
                        && !Critter.critterIsDead(candidate)) {
                    if (ObjectSystem.objectGetDistanceBetween(defender, candidate) < damageRadius
                            && candidate != candidate.data.critter.combat.whoHitMe) {
                        int damageThreshold = Stat.critterGetStat(candidate,
                                ProtoTypes.statPlusDamageType(StatDefs.Stat.STAT_DAMAGE_THRESHOLD,
                                        damageType).id());
                        int damageResistance = Stat.critterGetStat(candidate,
                                ProtoTypes.statPlusDamageType(StatDefs.Stat.STAT_DAMAGE_RESISTANCE,
                                        damageType).id());
                        if (damageResistance * (maxDamage - damageThreshold) / 100 > 0) {
                            return true;
                        }
                    }
                }
            }

            if (ObjectSystem.objectGetDistanceBetween(defender, attacker) <= damageRadius) {
                if (safeDistancePtr != null) {
                    safeDistancePtr[0] = damageRadius
                            - ObjectSystem.objectGetDistanceBetween(defender, attacker) + 1;
                    return false;
                }

                return true;
            }

            return false;
        }

        int anim = Item.weaponGetAnimationForHitMode(weapon, hitMode);
        if (anim != AnimationDefs.AnimationType.ANIM_FIRE_BURST.id()
                && anim != AnimationDefs.AnimationType.ANIM_FIRE_CONTINUOUS.id()) {
            return false;
        }

        CombatDefs.Attack attack = new CombatDefs.Attack();
        attackInit(attack, attacker, defender, hitMode,
                CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());

        int accuracy = attackDetermineToHit(attacker, attacker.tile, defender,
                CombatDefs.HitLocation.HIT_LOCATION_TORSO.id(), hitMode, true);
        int[] roundsHitMainTarget = new int[1];
        int[] roundsFired = new int[1];
        computeSpray(attack, accuracy, roundsHitMainTarget, roundsFired, anim);

        if (attackerFriend != null) {
            for (int index = 0; index < attack.extrasLength; index++) {
                if (attack.extras[index] == attackerFriend) {
                    Debug.print("Friendly was in the way!");
                    return true;
                }
            }
        }

        for (int index = 0; index < attack.extrasLength; index++) {
            GameObject candidate = attack.extras[index];
            if (candidate.data.critter.combat.team == team
                    && candidate != attacker
                    && candidate != defender
                    && !Critter.critterIsDead(candidate)
                    && candidate != candidate.data.critter.combat.whoHitMe) {
                int damageThreshold = Stat.critterGetStat(candidate,
                        ProtoTypes.statPlusDamageType(StatDefs.Stat.STAT_DAMAGE_THRESHOLD,
                                damageType).id());
                int damageResistance = Stat.critterGetStat(candidate,
                        ProtoTypes.statPlusDamageType(StatDefs.Stat.STAT_DAMAGE_RESISTANCE,
                                damageType).id());
                if (damageResistance * (maxDamage - damageThreshold) / 100 > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Port of {@code attackInit} (0x422E1C): set up an attack record.
     */
    public static void attackInit(CombatDefs.Attack attack, ObjTypes.GameObject attacker,
            ObjTypes.GameObject defender, int hitMode, int hitLocation) {
        attack.attacker = attacker;
        attack.hitMode = hitMode;
        attack.weapon = Item.getWeaponForHitMode(attacker, hitMode);
        attack.attackHitLocation = CombatDefs.HitLocation.HIT_LOCATION_TORSO.id();
        attack.attackerDamage = 0;
        attack.attackerFlags = ObjTypes.Dam.NONE;
        attack.ammoQuantity = 0;
        attack.criticalMessageId = -1;
        attack.defender = defender;
        attack.tile = defender != null ? defender.tile : -1;
        attack.defenderHitLocation = hitLocation;
        attack.defenderDamage = 0;
        attack.defenderFlags = ObjTypes.Dam.NONE;
        attack.defenderKnockback = 0;
        attack.extrasLength = 0;
        attack.intendedTarget = defender;
        attack.extras = new ObjTypes.GameObject[6];
        attack.extrasHitLocation = new int[6];
        attack.extrasDamage = new int[6];
        attack.extrasFlags = new ObjTypes.Dam[6];
        attack.extrasKnockback = new int[6];
    }

    /**
     * Port of {@code attackComputeDamage} (0x4247B8), the vanilla
     * formula.
     *
     * <p>The target's threshold and resistance come from its stats at
     * the damage type of the weapon; a bypass strike cuts both to a
     * fifth (EMP is immune to the cut), the penetrate perk and the
     * penetrating unarmed hits cut the threshold, the dude's Finesse
     * raises the resistance. The ammo resistance modifier joins in and
     * the resistance is clamped to 0..100; every round draws its damage,
     * multiplies by the base multiplier and the ammo multiplier, divides
     * by the ammo divisor and by 2, applies the difficulty percent, then
     * the threshold and the resistance.
     *
     * <p>The dude-gated perk bonuses (Bonus Ranged Damage, Living
     * Anatomy, Pyromaniac) are dormant headless; the knockback tail and
     * the script hook come with their own slices.
     */
    public static void attackComputeDamage(CombatDefs.Attack attack, int numRounds,
            int baseDamageMult) {
        boolean hit = (attack.attackerFlags.bits() & ObjTypes.Dam.HIT.bits()) != 0;

        ObjTypes.GameObject critter;
        ObjTypes.Dam flags;
        if (hit) {
            critter = attack.defender;
            flags = attack.defenderFlags;
        } else {
            critter = attack.attacker;
            flags = attack.attackerFlags;
        }

        int damage = 0;

        if (critter != null
                && FrmId.ofFid(critter.fid).objectType()
                    == ObjectType.CRITTER) {
            ProtoTypes.DamageType damageType = Item.weaponGetDamageType(attack.attacker,
                attack.weapon);
            int damageThreshold = Stat.critterGetStat(critter,
                StatDefs.Stat.STAT_DAMAGE_THRESHOLD.id() + damageType.id());
            int damageResistance = Stat.critterGetStat(critter,
                StatDefs.Stat.STAT_DAMAGE_RESISTANCE.id() + damageType.id());

            if ((flags.bits() & ObjTypes.Dam.BYPASS.bits()) != 0
                    && damageType != ProtoTypes.DamageType.DAMAGE_TYPE_EMP) {
                damageThreshold = 20 * damageThreshold / 100;
                damageResistance = 20 * damageResistance / 100;
            } else {
                if (Item.weaponGetPerk(attack.weapon) == PerkDefs.Perk.PERK_WEAPON_PENETRATE
                        || Item.unarmedIsPenetrating(attack.hitMode)) {
                    damageThreshold = 20 * damageThreshold / 100;
                }

                if (isDude(attack.attacker)
                        && TraitDefs.traitIsSelectedAndActive(TraitDefs.Trait.TRAIT_FINESSE.id())) {
                    damageResistance += 30;
                }
            }

            int damageBonus = 0;

            int difficultyDamagePercent = 100;

            int damageResistanceWithAmmo = damageResistance
                + Item.weaponGetAmmoDamageResistanceModifier(attack.weapon);
            if (damageResistanceWithAmmo > 100) {
                damageResistanceWithAmmo = 100;
            } else if (damageResistanceWithAmmo < 0) {
                damageResistanceWithAmmo = 0;
            }

            int damageMultiplier = baseDamageMult
                * Item.weaponGetAmmoDamageMultiplier(attack.weapon);
            int damageDivisor = Item.weaponGetAmmoDamageDivisor(attack.weapon);

            for (int index = 0; index < numRounds; index++) {
                int roundDamage = Item.weaponGetDamage(attack.attacker, attack.hitMode);

                roundDamage += damageBonus;

                roundDamage *= damageMultiplier;

                if (damageDivisor != 0) {
                    roundDamage /= damageDivisor;
                }

                roundDamage /= 2;

                roundDamage *= difficultyDamagePercent;
                roundDamage /= 100;

                roundDamage -= damageThreshold;

                if (roundDamage > 0) {
                    roundDamage -= roundDamage * damageResistanceWithAmmo / 100;
                }

                if (roundDamage > 0) {
                    damage += roundDamage;
                }
            }
        }

        if (hit) {
            attack.defenderDamage = damage;
        } else {
            attack.attackerDamage = damage;
        }
    }

    /** gDude seam: headless there is no dude. */
    private static boolean isDude(ObjTypes.GameObject critter) {
        return critter != null && critter == ObjectSystem.getDude();
    }

    /** Port of {@code is_unarmed_hit_mode} (src/combat.h). */
    private static boolean isUnarmedHitMode(int hitMode) {
        return hitMode == CombatDefs.HitMode.HIT_MODE_PUNCH.id()
            || hitMode == CombatDefs.HitMode.HIT_MODE_KICK.id()
            || (hitMode >= CombatDefs.FIRST_ADVANCED_UNARMED_HIT_MODE
                && hitMode <= CombatDefs.LAST_ADVANCED_UNARMED_HIT_MODE);
    }

    // ===== kills and combat experience (gKillsByType, _combat_exps) =====

    private static final int[] killsByType = new int[ProtoTypes.KillType.values().length];
    private static int combatExps;

    /** Port of {@code gCombatState} (src/combat.cc): COMBAT_STATE_IN_COMBAT. */
    public static int combatState;

    public static final int COMBAT_STATE_OUT_COMBAT = 0x00;
    public static final int COMBAT_STATE_IN_COMBAT = 0x01;
    public static final int COMBAT_STATE_EXIT_REQUESTED = 0x02;
    public static final int COMBAT_STATE_PLAYER_TURN = 0x04;

    /** Port of {@code isInCombat} (src/combat.h). */
    public static boolean isInCombat() {
        return (combatState & COMBAT_STATE_IN_COMBAT) != 0;
    }

    // ===== combat structure (src/combat.cc) =====

    static GameObject[] combatList;
    static int listTotal;
    static int listCom;
    static int listNoncom;
    static int combatNumTurns;
    static ScriptsState.CombatStartData gcsd;

    /** Port of {@code CombatAiInfo} (src/combat.cc:66). */
    public static final class CombatAiInfo {
        public GameObject friendlyDead;
        public GameObject lastTarget;
        public GameObject lastItem;
        public int lastMove;
    }

    /** Port of {@code _aiInfoList} (src/combat.cc:171): cid-indexed. */
    public static CombatAiInfo[] aiInfoList;

    /**
     * Port of {@code _combat_begin} (0x421B8C). The animation and UI tails
     * are game-phase seams; what stays is the list build, the combat-data
     * reset, the AI info list allocation and the in-combat state.
     */
    static void combatBegin(GameObject attacker) {
        if (isInCombat()) {
            return;
        }

        combatNumTurns = 0;
        combatExps = 0;
        combatList = ObjectSystem.objectListCreateForCombat(Map.gElevation);
        listTotal = combatList.length;
        listNoncom = listTotal;
        listCom = 0;
        gcsd = null;

        for (int index = 0; index < listTotal; index++) {
            GameObject critter = combatList[index];
            ObjTypes.CritterCombatData combatData = critter.data.critter.combat;
            combatData.maneuver = combatData.maneuver.and(ObjTypes.CritterManeuver.ENGAGING);
            combatData.damageLastTurn = 0;
            combatData.whoHitMe = null;
            combatData.ap = 0;
            critter.cid = index;

            ObjectSystem.scriptSetObjects(critter.sid, null, null);
            ObjectSystem.scriptSetFixedParam(critter.sid, 0);
        }

        combatState |= COMBAT_STATE_IN_COMBAT;

        combatEndingGuy = null;

        // _combat_begin_extra(attacker): the main-attack attackInit and
        // _combat_ai_begin (roster copy) stay game-phase-callable; the AI
        // info list allocates here like _combat_begin does.
        aiInfoList = new CombatAiInfo[listTotal];
        for (int index = 0; index < listTotal; index++) {
            aiInfoList[index] = new CombatAiInfo();
        }

        CombatAi.combatAiBegin(combatList, listTotal);
    }

    /**
     * Port of {@code _combat_sequence_init} (0x42243C): attacker first,
     * defender second, dude third.
     */
    static void combatSequenceInit(GameObject attacker, GameObject defender) {
        int next = 0;
        if (attacker != null) {
            for (int index = 0; index < listTotal; index++) {
                if (combatList[index] == attacker) {
                    GameObject temp = combatList[next];
                    combatList[index] = temp;
                    combatList[next] = attacker;
                    next += 1;
                    break;
                }
            }
        }

        if (defender != null) {
            for (int index = 0; index < listTotal; index++) {
                if (combatList[index] == defender) {
                    GameObject temp = combatList[next];
                    combatList[index] = temp;
                    combatList[next] = defender;
                    next += 1;
                    break;
                }
            }
        }

        GameObject dude = ObjectSystem.getDude();
        if (attacker != dude && defender != dude) {
            for (int index = 0; index < listTotal; index++) {
                if (combatList[index] == dude) {
                    GameObject temp = combatList[next];
                    combatList[index] = temp;
                    combatList[next] = dude;
                    next += 1;
                    break;
                }
            }
        }

        listCom = next;
        listNoncom -= next;

        if (attacker != null) {
            Critter.critterSetWhoHitMe(attacker, defender);
        }
        if (defender != null) {
            Critter.critterSetWhoHitMe(defender, attacker);
        }
    }

    // 0x4223C8 _compare_faster: higher sequence first, luck the tiebreak.
    static int compareFaster(GameObject critter1, GameObject critter2) {
        int sequence1 = Stat.critterGetStat(critter1, StatDefs.Stat.STAT_SEQUENCE.id());
        int sequence2 = Stat.critterGetStat(critter2, StatDefs.Stat.STAT_SEQUENCE.id());
        if (sequence1 > sequence2) {
            return -1;
        } else if (sequence1 < sequence2) {
            return 1;
        }

        int luck1 = Stat.critterGetStat(critter1, StatDefs.Stat.STAT_LUCK.id());
        int luck2 = Stat.critterGetStat(critter2, StatDefs.Stat.STAT_LUCK.id());
        if (luck1 > luck2) {
            return -1;
        } else if (luck1 < luck2) {
            return 1;
        }

        return 0;
    }

    /**
     * Port of {@code _combat_sequence} (0x422580). The non-combatant join
     * stage (_combat_add_noncoms) needs the AI and the turn, so it reads
     * false headless.
     */
    static void combatSequence() {
        int count = listCom;

        for (int index = 0; index < count; index++) {
            GameObject critter = combatList[index];
            if ((critter.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) != 0) {
                combatList[index] = combatList[count - 1];
                combatList[count - 1] = critter;

                combatList[count - 1] = combatList[listNoncom + count - 1];
                combatList[listNoncom + count - 1] = critter;

                index -= 1;
                count -= 1;
            }
        }

        GameObject dude = ObjectSystem.getDude();
        for (int index = 0; index < count; index++) {
            GameObject critter = combatList[index];
            if (critter != dude) {
                if ((critter.data.critter.combat.results.bits() & ObjTypes.Dam.KNOCKED_OUT.bits()) != 0
                        || critter.data.critter.combat.maneuver.equals(ObjTypes.CritterManeuver.DISENGAGING)) {
                    critter.data.critter.combat.maneuver = critter.data.critter.combat.maneuver
                            .and(ObjTypes.CritterManeuver.ENGAGING.not());
                    listNoncom += 1;

                    combatList[index] = combatList[count - 1];
                    combatList[count - 1] = critter;

                    count -= 1;
                    index -= 1;
                }
            }
        }

        if (count != 0) {
            listCom = count;
            java.util.Arrays.sort(combatList, 0, count, (a, b) -> compareFaster(a, b));
            count = listCom;
        }

        listCom = count;

        GameTime.gameTimeAddSeconds(5);
    }

    // ===== turns (src/combat.cc) =====

    static GameObject combatTurnObj;
    static int combatFreeMove;
    static int combatElev;
    static final CombatDefs.Attack mainAttack = new CombatDefs.Attack();

    /** Scheduling boundary of {@code _action_attack}; completion must be later. */
    @FunctionalInterface
    interface AttackScheduler {
        int schedule(CombatDefs.Attack attack);
    }

    // NOTE: managed headless scheduling has no visual frames. It starts one
    // pending animation; combatTurnRun completes it at the engine's wait point.
    private static final AttackScheduler HEADLESS_ATTACK_SCHEDULER = attack -> {
        combatAnimBegin();
        return 0;
    };
    static AttackScheduler attackScheduler = HEADLESS_ATTACK_SCHEDULER;
    static int combatTurnRunning;
    static boolean combatCleanupEnabled;
    static boolean combatCallDisplay;
    /** {@code _combat_ending_guy} (src/combat.cc): who ended the fight. */
    static GameObject combatEndingGuy;

    /**
     * {@code _combat_attack} (src/combat.cc:3631): compute, apply start
     * modifiers, schedule, then spend AP. Ammo and HP change on completion.
     */
    public static int combatAttack(GameObject attacker, GameObject defender,
            int hitMode, int hitLocation) {
        // NOTE: the headless scheduler supports one outstanding main attack.
        // Reject another submission rather than overwriting its callback data.
        if (combatCleanupEnabled || attacker == null || defender == null) {
            return -1;
        }
        if (attacker != ObjectSystem.getDude()
                && hitMode == CombatDefs.HitMode.HIT_MODE_PUNCH.id()
                && GameRandom.randomBetween(1, 4) == 1) {
            FrmId base = FrmId.ofFid(attacker.fid);
            FrmId kick = FrmId.ofObject(base.objectType().value(), base.frameId(),
                    AnimationType.KICK_LEG.value(), base.weaponAnimation(), base.rotation());
            if (kick.exist()) {
                hitMode = CombatDefs.HitMode.HIT_MODE_KICK.id();
            }
        }

        attackInit(mainAttack, attacker, defender, hitMode, hitLocation);
        if (attackCompute(mainAttack) == -1) {
            return -1;
        }
        if (gcsd != null) {
            mainAttack.defenderDamage += gcsd.damageBonus;
            if (mainAttack.defenderDamage < gcsd.minDamage) {
                mainAttack.defenderDamage = gcsd.minDamage;
            }
            if (mainAttack.defenderDamage > gcsd.maxDamage) {
                mainAttack.defenderDamage = gcsd.maxDamage;
            }
            if (gcsd.overrideAttackResults != 0) {
                // Original bug: both writes target defenderFlags. Preserve it.
                mainAttack.defenderFlags = gcsd.attackerResults;
                mainAttack.defenderFlags = gcsd.targetResults;
            }
        }

        boolean aiming = mainAttack.defenderHitLocation != CombatDefs.HitLocation.HIT_LOCATION_TORSO.id()
                && mainAttack.defenderHitLocation != CombatDefs.HitLocation.HIT_LOCATION_UNCALLED.id();
        // Interface-selected aiming for the dude's torso shot is not yet
        // available; it defaults to false at this boundary.
        int actionPoints = Item.weaponGetActionPointCost(attacker, mainAttack.hitMode, aiming);
        if (attackScheduler.schedule(mainAttack) == -1) {
            return -1;
        }
        if (actionPoints > attacker.data.critter.combat.ap) {
            attacker.data.critter.combat.ap = 0;
        } else {
            attacker.data.critter.combat.ap -= actionPoints;
        }
        if (attacker == ObjectSystem.getDude()) {
            Critter.critterSetWhoHitMe(attacker, defender);
        }
        // explosionSettingsReset / aiInfoSetLastTarget and UI updates are
        // deferred with their respective modules.
        combatCallDisplay = true;
        combatCleanupEnabled = true;
        return 0;
    }

    /** {@code _combat_anim_begin} (src/combat.cc:5503). */
    static void combatAnimBegin() {
        combatTurnRunning++;
        // First animation of a dude attack also disables the UI in C++.
    }

    /** {@code _combat_anim_finished} (src/combat.cc:5515), observable cleanup. */
    static void combatAnimFinished() {
        combatTurnRunning--;
        if (combatTurnRunning != 0 || !combatCleanupEnabled) {
            return;
        }
        // Clear before damage scripts run, as in C++; they can issue calls.
        combatCleanupEnabled = false;
        GameObject weapon = Item.getWeaponForHitMode(mainAttack.attacker, mainAttack.hitMode);
        if (weapon != null && Item.ammoGetCapacity(weapon) > 0) {
            Item.ammoSetQuantity(weapon, Item.ammoGetQuantity(weapon) - mainAttack.ammoQuantity);
        }
        // _combat_display is battle text/UI. The flag still clears once.
        combatCallDisplay = false;
        applyDamage(mainAttack, true);

        GameObject attacker = mainAttack.attacker;
        if (scrEndCombat()) {
            GameObject dude = ObjectSystem.getDude();
            if ((dude.data.critter.combat.results.bits() & Dam.KNOCKED_OUT.bits()) != 0) {
                if (attacker.data.critter.combat.team == dude.data.critter.combat.team) {
                    combatEndingGuy = dude.data.critter.combat.whoHitMe;
                } else {
                    combatEndingGuy = attacker;
                }
            }
        }
        attackInit(mainAttack, attacker, null, CombatDefs.HitMode.HIT_MODE_PUNCH.id(),
                CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());
        int results = attacker.data.critter.combat.results.bits();
        if ((results & (Dam.KNOCKED_OUT.bits() | Dam.KNOCKED_DOWN.bits())) != 0
                && (results & (Dam.KNOCKED_OUT.bits() | Dam.DEAD.bits() | Dam.LOSE_TURN.bits())) == 0) {
            combatStandup(attacker);
        }
    }

    /** {@code _combat_player_knocked_out_by} (0x426D94). */
    public static int combatPlayerKnockedOutBy() {
        GameObject dude = ObjectSystem.getDude();
        if ((dude.data.critter.combat.results.bits() & Dam.DEAD.bits()) != 0) {
            return -1;
        }
        if (combatEndingGuy == null) {
            return -1;
        }
        return combatEndingGuy.data.critter.combat.team;
    }

    /**
     * Port of {@code _scr_end_combat} (src/scripts.cc:3345): run the map
     * script's COMBAT proc with the team that knocked the dude out and
     * report whether it overrode the ending. Without a loaded map script
     * ({@code gMapSid} fresh-mirror -1) it returns false, like C.
     */
    static boolean scrEndCombat() {
        int mapSid = Map.mapSid();
        if (mapSid == 0 || mapSid == -1) {
            return false;
        }
        int team = combatPlayerKnockedOutBy();
        if (team == -1) {
            return false;
        }
        ObjectSystem.scriptSetFixedParam(mapSid, team);
        ObjectSystem.scriptExecProc(mapSid, ObjectSystem.SCRIPT_PROC_COMBAT);
        boolean success = false;
        ObjectSystem.Script[] after = new ObjectSystem.Script[1];
        if (ObjectSystem.scriptGetScript(mapSid, after) != -1) {
            if (after[0].scriptOverrides != 0) {
                success = true;
            }
        }
        return success;
    }

    /**
     * Port of {@code _combat_to_hit} (0x426744): the accuracy shown for a
     * target. The interface-derived hit mode uses the same headless primary
     * hand seam as {@link #combatAttackThis}.
     */
    public static boolean combatToHit(GameObject target, int[] accuracy) {
        GameObject dude = ObjectSystem.getDude();
        int hitMode = CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id();
        boolean aiming = false;
        if (Interface.interfaceGetCurrentHand() == Inventory.Hand.HAND_LEFT) {
            hitMode = CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_PRIMARY.id();
        }
        if (combatCheckBadShot(dude, target, hitMode, aiming)
                != CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_OK) {
            return false;
        }
        accuracy[0] = attackDetermineToHit(dude, dude.tile, target,
                CombatDefs.HitLocation.HIT_LOCATION_UNCALLED.id(), hitMode, true);
        return true;
    }

    /** Headless counterpart of the animation wait in {@code _combat_turn_run}. */
    static void combatTurnRun() {
        if (attackScheduler == HEADLESS_ATTACK_SCHEDULER) {
            while (combatTurnRunning > 0) {
                combatAnimFinished();
            }
        }
    }

    /**
     * Port of {@code _combat_standup} (0x426640): stand a prone critter
     * up, costing 3 AP (1 with Quick Recovery for the dude). The standing
     * animation is a render seam; the observable part is the AP cost and
     * the knockdown-bit clear.
     */
    static void combatStandup(GameObject a1) {
        int v2 = 3;
        if (a1 == ObjectSystem.getDude()
                && Perk.perkHasRank(a1, Perk.PERK_QUICK_RECOVERY)) {
            v2 = 1;
        }

        if (v2 > a1.data.critter.combat.ap) {
            a1.data.critter.combat.ap = 0;
        } else {
            a1.data.critter.combat.ap -= v2;
        }

        // dude interfaceRenderActionPoints: UI seam.
        // _dude_standup: the standing animation is a render seam.
        a1.data.critter.combat.results = a1.data.critter.combat.results
                .and(ObjTypes.Dam.KNOCKED_DOWN.not());
        combatTurnRun();
    }

    /**
     * {@code _combat_check_bad_shot} (src/combat.cc:5823). The aim-blocked
     * check is a seam: the headless tile has no scenery to block lines.
     */
    public static CombatDefs.CombatBadShot combatCheckBadShot(GameObject attacker,
            GameObject defender, int hitMode, boolean aiming) {
        int range = 1;
        if (defender != null) {
            range = ObjectSystem.objectGetDistanceBetween(attacker, defender);
            if ((defender.data.critter.combat.results.bits()
                    & ObjTypes.Dam.DEAD.bits()) != ObjTypes.Dam.NONE.bits()) {
                return CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_ALREADY_DEAD;
            }
        }

        GameObject weapon = Item.getWeaponForHitMode(attacker, hitMode);
        if (weapon != null) {
            int results = attacker.data.critter.combat.results.bits();
            if ((results & (ObjTypes.Dam.CRIP_ARM_LEFT.bits()
                    | ObjTypes.Dam.CRIP_ARM_RIGHT.bits())) == (ObjTypes.Dam.CRIP_ARM_LEFT.bits()
                    | ObjTypes.Dam.CRIP_ARM_RIGHT.bits())) {
                return CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_BOTH_ARMS_CRIPPLED;
            }
            if ((results & ObjTypes.Dam.CRIP_ARM_ANY.bits()) != 0) {
                if (Item.weaponIsTwoHanded(weapon)) {
                    return CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_ARM_CRIPPLED;
                }
            }
        }

        if (Item.weaponGetActionPointCost(attacker, hitMode, aiming) > attacker.data.critter.combat.ap) {
            return CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_NOT_ENOUGH_AP;
        }
        if (Item.weaponGetRange(attacker, hitMode) < range) {
            return CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_OUT_OF_RANGE;
        }

        int attackType = Item.weaponGetAttackTypeForHitMode(weapon, hitMode);
        if (Item.ammoGetCapacity(weapon) > 0) {
            if (!Item.weaponHasAmmoForAttack(weapon, hitMode)) {
                return CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_NO_AMMO;
            }
        }
        if (attackType == Item.AttackType.ATTACK_TYPE_RANGED.id()
                || attackType == Item.AttackType.ATTACK_TYPE_THROW.id()
                || Item.weaponGetRange(attacker, hitMode) > 1) {
            // _combat_is_shot_blocked: headless seam, clear line of fire.
        }

        return CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_OK;
    }

    /**
     * {@code _combat_attack_this} (src/combat.cc:5895): the player's chosen
     * attack. The interface-driven hit-mode/aiming selection and the called
     * shot window are seams; an armed primary attack with the current hand
     * runs through the request/attack path.
     */
    public static void combatAttackThis(GameObject target) {
        if (target == null) {
            return;
        }
        if ((combatState & COMBAT_STATE_PLAYER_TURN) == 0) {
            return;
        }

        GameObject dude = ObjectSystem.getDude();
        // interfaceGetCurrentHitMode: default primary action, not aiming.
        int hitMode = CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id();
        boolean aiming = false;
        if (Interface.interfaceGetCurrentHand() == Inventory.Hand.HAND_LEFT) {
            hitMode = CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_PRIMARY.id();
        }

        CombatDefs.CombatBadShot rc = combatCheckBadShot(dude, target, hitMode, aiming);
        if (rc != CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_OK) {
            // The message/UI reactions of the bad-shot cases are game-phase
            // seams; the request is not issued.
            return;
        }

        if (!isInCombat()) {
            ScriptsState.CombatStartData combat = new ScriptsState.CombatStartData();
            combat.attacker = dude;
            combat.defender = target;
            combat.actionPointsBonus = 0;
            combat.accuracyBonus = 0;
            combat.damageBonus = 0;
            combat.minDamage = 0;
            combat.maxDamage = Integer.MAX_VALUE;
            combat.overrideAttackResults = 0;
            ScriptsState.scriptsRequestCombat(combat);
            return;
        }

        combatAttack(dude, target, hitMode, CombatDefs.HitLocation.HIT_LOCATION_UNCALLED.id());
    }

    /** Seam for {@code _combat_input} (player turn loop, game phase). */
    static int combatInput() {
        return 0;
    }

    /**
     * Port of {@code _combat_turn} (0x422E1C), the turn of one combatant.
     * The player-input and AI turns and the UI/audio/render tails are game-
     * phase seams; what stays is the turn state, the script combat proc with
     * its override, the stand-up, the gcsd seed and the cleanup.
     */
    static int combatTurn(GameObject obj, boolean reloadedDuringCombat) {
        combatTurnObj = obj;

        attackInit(mainAttack, obj, null,
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(),
            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());

        int results = obj.data.critter.combat.results.bits();
        if ((results & (ObjTypes.Dam.KNOCKED_OUT.bits() | ObjTypes.Dam.DEAD.bits()
                | ObjTypes.Dam.LOSE_TURN.bits())) != 0) {
            obj.data.critter.combat.results = obj.data.critter.combat.results
                    .and(ObjTypes.Dam.LOSE_TURN.not());
        } else {
            if (obj == ObjectSystem.getDude()) {
                // keyboardReset/inputEventQueueReset/interfaceRender*: seams.
                combatFreeMove = 2 * Perk.perkGetRank(obj, Perk.PERK_BONUS_MOVE);
            } else {
                // soundContinueAll: audio seam.
            }

            boolean scriptOverrides = false;
            if (obj.sid != -1) {
                ObjectSystem.scriptSetObjects(obj.sid, null, null);
                ObjectSystem.scriptSetFixedParam(obj.sid, 4);
                ObjectSystem.scriptExecProc(obj.sid, ObjectSystem.SCRIPT_PROC_COMBAT);

                ObjectSystem.Script[] slot = new ObjectSystem.Script[1];
                if (ObjectSystem.scriptGetScript(obj.sid, slot) != -1) {
                    scriptOverrides = slot[0].scriptOverrides != 0;
                }

                // _game_user_wants_to_quit == END_COMBAT: game-phase seam.
            }

            if (!scriptOverrides) {
                if (!reloadedDuringCombat && Critter.critterIsProne(obj)) {
                    combatStandup(obj);
                }

                if (obj == ObjectSystem.getDude()) {
                    // gameUiEnable/_gmouse_3d_refresh/outlines: seams.
                    if (gcsd != null) {
                        combatAttackThis(gcsd.defender);
                    }

                    if (!reloadedDuringCombat) {
                        combatState |= COMBAT_STATE_PLAYER_TURN;
                    }

                    if (combatInput() == -1) {
                        obj.data.critter.combat.damageLastTurn = 0;
                        combatFreeMove = 0;
                        return -1;
                    }
                } else {
                    // outline enable: render seam.
                    // _combat_ai(obj, target): AI seam.
                    // retry loop with _combat_turn_run: animation seam.
                }
            }

            combatTurnRun();

            if (obj == ObjectSystem.getDude()) {
                // UI cleanup seams; turnObj goes null and back to dude.
                combatTurnObj = null;
                combatTurnObj = ObjectSystem.getDude();
            } else {
                // outline disable: render seam.
            }
        }

        GameObject dude = ObjectSystem.getDude();
        if ((dude.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) != 0) {
            return -1;
        }

        if (obj != dude || combatElev == dude.elevation) {
            combatFreeMove = 0;
            return 0;
        }

        return -1;
    }

    /**
     * Port of {@code _combat_set_move_all} (0x4228B0): every combatant's
     * action points refresh from their stat plus the combat-start bonus.
     */
    static void combatSetMoveAll() {
        for (int index = 0; index < listCom; index++) {
            GameObject object = combatList[index];
            int actionPoints = Stat.critterGetStat(object,
                StatDefs.Stat.STAT_MAXIMUM_ACTION_POINTS.id());
            if (gcsd != null) {
                actionPoints += gcsd.actionPointsBonus;
            }
            object.data.critter.combat.ap = actionPoints;
            // _combatAIInfoSetLastMove(object, 0): AI seam.
        }
    }

    /**
     * Port of {@code _combat_over} (0x422130): close the fight: clear the
     * combat data, hand the banked experience over (a seam), clear the
     * in-combat state (the player-turn re-raise is the original's quirk)
     * and drop the list. The AI reload, outlines, UI and Goris animation
     * are seams.
     */
    static void combatOver() {
        for (int index = 0; index < listNoncom + listCom; index++) {
            GameObject critter = combatList[index];
            critter.data.critter.combat.damageLastTurn = 0;
            critter.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
        }

        for (int index = 0; index < listTotal; index++) {
            GameObject critter = combatList[index];
            critter.data.critter.combat.ap = 0;
            // objectClearOutline: render seam.
            critter.data.critter.combat.whoHitMe = null;
            ObjectSystem.scriptSetObjects(critter.sid, null, null);
            ObjectSystem.scriptSetFixedParam(critter.sid, 0);
        }

        // tileWindowRefresh/interface: render seams.

        GameObject dude = ObjectSystem.getDude();
        dude.data.critter.combat.ap = Stat.critterGetStat(dude,
            StatDefs.Stat.STAT_MAXIMUM_ACTION_POINTS.id());

        // _combat_give_exps(combatExps): pcAddExperience is game phase.
        combatExps = 0;

        combatState &= ~(COMBAT_STATE_IN_COMBAT | COMBAT_STATE_PLAYER_TURN);
        combatState |= COMBAT_STATE_PLAYER_TURN;

        aiInfoList = null;
        CombatAi.combatAiOver();

        combatList = null;
        listTotal = 0;
        listCom = 0;
        listNoncom = 0;
    }

    // 0x4217FC _combatInitAIInfoList body is inlined at the combatBegin
    // allocation; the readers/writers below mirror the combat.cc accessors.

    /** Port of {@code aiInfoGetFriendlyDead} (0x421798). */
    public static GameObject aiInfoGetFriendlyDead(GameObject obj) {
        if (!isInCombat() || obj == null || obj.cid == -1) {
            return null;
        }
        return aiInfoList[obj.cid].friendlyDead;
    }

    /** Port of {@code aiInfoSetFriendlyDead} (0x4218AC). */
    public static int aiInfoSetFriendlyDead(GameObject a1, GameObject a2) {
        if (!isInCombat() || a1 == null || a1.cid == -1 || a1 == a2) {
            return -1;
        }
        aiInfoList[a1.cid].friendlyDead = a2;
        return 0;
    }

    /** Port of {@code aiInfoGetLastTarget} (0x4218EC). */
    public static GameObject aiInfoGetLastTarget(GameObject obj) {
        if (!isInCombat() || obj == null || obj.cid == -1) {
            return null;
        }
        return aiInfoList[obj.cid].lastTarget;
    }

    /** Port of {@code aiInfoSetLastTarget} (0x421918). */
    public static int aiInfoSetLastTarget(GameObject a1, GameObject a2) {
        if (!isInCombat()) {
            return 0;
        }
        if (a1 == null || a1.cid == -1 || a1 == a2) {
            return -1;
        }
        if (Critter.critterIsDead(a2)) {
            a2 = null;
        }
        aiInfoList[a1.cid].lastTarget = a2;
        return 0;
    }

    /** Port of {@code aiInfoGetLastItem} (0x42196C). */
    public static GameObject aiInfoGetLastItem(GameObject obj) {
        if (!isInCombat() || obj == null || obj.cid == -1) {
            return null;
        }
        return aiInfoList[obj.cid].lastItem;
    }

    /** Port of {@code aiInfoSetLastItem} (0x421998). */
    public static int aiInfoSetLastItem(GameObject obj, GameObject a2) {
        if (!isInCombat()) {
            return 0;
        }
        if (obj == null || obj.cid == -1) {
            return -1;
        }
        aiInfoList[obj.cid].lastItem = a2;
        return 0;
    }

    /** Port of {@code _combatAIInfoSetLastMove} (0x421804). */
    public static int combatAiInfoSetLastMove(GameObject obj, int lastMove) {
        if (!isInCombat()) {
            return 0;
        }
        if (obj == null || obj.cid == -1) {
            return -1;
        }
        aiInfoList[obj.cid].lastMove = lastMove;
        return 0;
    }

    /**
     * Port of {@code _combat} (0x422D2C), the fight loop. The player-input
     * and AI turns inside each turn are seams (turn returns without
     * input), as are the Goris animations, the end-due-to-load branch and
     * the map-update scripts.
     */
    static void combat(ScriptsState.CombatStartData csd) {
        if (csd == null
                || (csd.attacker == null || csd.attacker.elevation == Map.gElevation)
                || (csd.defender == null || csd.defender.elevation == Map.gElevation)) {
            boolean wasInCombat = isInCombat();

            combatBegin(null);

            int curIndex;

            if (wasInCombat) {
                GameObject dude = ObjectSystem.getDude();
                if (combatTurn(dude, true) == -1) {
                    curIndex = -1;
                } else {
                    int index;
                    for (index = 0; index < listCom; index++) {
                        if (combatList[index] == dude) {
                            break;
                        }
                    }
                    curIndex = index + 1;
                }
                gcsd = null;
            } else {
                GameObject defender = csd != null ? csd.defender : null;
                GameObject attacker = csd != null ? csd.attacker : null;
                combatSequenceInit(attacker, defender);
                gcsd = csd;
                curIndex = 0;
            }

            do {
                if (curIndex == -1) {
                    break;
                }

                combatSetMoveAll();

                for (; curIndex < listCom; curIndex++) {
                    if (combatTurn(combatList[curIndex], false) == -1) {
                        break;
                    }
                    // _combat_ending_guy: never set headless.
                    gcsd = null;
                }

                if (curIndex < listCom) {
                    break;
                }

                combatSequence();
                curIndex = 0;
                combatNumTurns += 1;
            } while (!combatShouldEnd());

            // _combat_end_due_to_load branch: save/load seam.
            // Goris begin/end animations, UI, hooks: seams.
            combatOver();
            // scriptsExecMapUpdateProc: map-script engine seam.
        }
    }

    // ===== attack computation (src/combat.cc) =====

    private static final int WEAPON_CRITICAL_FAILURE_TYPE_COUNT = 7;
    private static final int WEAPON_CRITICAL_FAILURE_EFFECT_COUNT = 5;

    // _cf_table (src/combat.cc): critical-failure effects per weapon type
    // and effect roll.
    private static final int[][] CF_TABLE = {
        { 0, Dam.LOSE_TURN.bits(), Dam.LOSE_TURN.bits(),
            Dam.HURT_SELF.bits() | Dam.KNOCKED_DOWN.bits(), Dam.CRIP_RANDOM.bits() },
        { 0, Dam.LOSE_TURN.bits(), Dam.DROP.bits(),
            Dam.RANDOM_HIT.bits(), Dam.HIT_SELF.bits() },
        { 0, Dam.LOSE_AMMO.bits(), Dam.DROP.bits(),
            Dam.RANDOM_HIT.bits(), Dam.DESTROY.bits() },
        { Dam.LOSE_TURN.bits(), Dam.LOSE_TURN.bits() | Dam.LOSE_AMMO.bits(),
            Dam.DROP.bits() | Dam.LOSE_TURN.bits(), Dam.RANDOM_HIT.bits(),
            Dam.EXPLODE.bits() | Dam.LOSE_TURN.bits() },
        { Dam.DUD.bits(), Dam.DROP.bits(),
            Dam.DROP.bits() | Dam.HURT_SELF.bits(), Dam.RANDOM_HIT.bits(),
            Dam.EXPLODE.bits() },
        { Dam.LOSE_TURN.bits(), Dam.DUD.bits(), Dam.DESTROY.bits(),
            Dam.RANDOM_HIT.bits(),
            Dam.EXPLODE.bits() | Dam.LOSE_TURN.bits() | Dam.KNOCKED_DOWN.bits() },
        { 0, Dam.LOSE_TURN.bits(), Dam.RANDOM_HIT.bits(),
            Dam.DESTROY.bits(),
            Dam.EXPLODE.bits() | Dam.LOSE_TURN.bits() | Dam.ON_FIRE.bits() },
    };

    /** Port of {@code explosionGetDamageType} (src/item.cc): BSS default. */
    public static int explosionGetDamageType() {
        return ProtoTypes.DamageType.DAMAGE_TYPE_EXPLOSION.id();
    }

    /** Port of {@code _is_hit_from_front} (src/actions.cc). */
    static boolean isHitFromFront(GameObject attacker, GameObject defender) {
        int diff = attacker.rotation.id() - defender.rotation.id();
        if (diff < 0) {
            diff = -diff;
        }
        return diff != 0 && diff != 1 && diff != 5;
    }

    // 0x424088 _attackFindInvalidFlags.
    static ObjTypes.Dam attackFindInvalidFlags(GameObject critter, GameObject item) {
        ObjTypes.Dam flags = ObjTypes.Dam.NONE;
        if (ProtoTypes.ProtoId.ofGameObject(critter).objectType()
                == ObjTypes.ObjectType.OBJ_TYPE_CRITTER
                && Critter.critterFlagCheck(critter, ObjTypes.CritterFlags.NO_DROP)) {
            flags = flags.or(ObjTypes.Dam.DROP);
        }
        if (item != null && (item.flags.bits() & ObjTypes.ObjectFlags.HIDDEN.bits()) != 0) {
            flags = flags.or(ObjTypes.Dam.DROP);
        }
        return flags;
    }

    // 0x42432C _do_random_cripple.
    static void doRandomCripple(ObjTypes.Dam[] flagsPtr) {
        flagsPtr[0] = flagsPtr[0].and(ObjTypes.Dam.CRIP_RANDOM.not());
        switch (GameRandom.randomBetween(0, 3)) {
        case 0:
            flagsPtr[0] = flagsPtr[0].or(ObjTypes.Dam.CRIP_LEG_LEFT);
            break;
        case 1:
            flagsPtr[0] = flagsPtr[0].or(ObjTypes.Dam.CRIP_LEG_RIGHT);
            break;
        case 2:
            flagsPtr[0] = flagsPtr[0].or(ObjTypes.Dam.CRIP_ARM_LEFT);
            break;
        case 3:
            flagsPtr[0] = flagsPtr[0].or(ObjTypes.Dam.CRIP_ARM_RIGHT);
            break;
        }
    }

    // 0x4240DC attackComputeCriticalFailure.
    static int attackComputeCriticalFailure(CombatDefs.Attack attack) {
        attack.attackerFlags = attack.attackerFlags.and(ObjTypes.Dam.HIT.not());

        if (attack.attacker != null
                && Critter.critterFlagCheck(attack.attacker, ObjTypes.CritterFlags.INVULNERABLE)) {
            return 0;
        }

        if (attack.attacker == ObjectSystem.getDude()) {
            // gRemoveCriticalTimeLimits (sfall toggle) is off headless.
            if (GameTime.gameTimeGetTime() / GameRandom.GAME_TIME_TICKS_PER_DAY < 6) {
                return 0;
            }
        }

        int attackType = Item.weaponGetAttackTypeForHitMode(attack.weapon, attack.hitMode);
        int criticalFailureTableIndex = Item.weaponGetCriticalFailureType(attack.weapon);
        if (criticalFailureTableIndex == -1) {
            criticalFailureTableIndex = 0;
        }

        int chance = GameRandom.randomBetween(1, 100)
            - 5 * (Stat.critterGetStat(attack.attacker, StatDefs.Stat.STAT_LUCK.id()) - 5);

        int effect;
        if (chance <= 20) {
            effect = 0;
        } else if (chance <= 50) {
            effect = 1;
        } else if (chance <= 75) {
            effect = 2;
        } else if (chance <= 95) {
            effect = 3;
        } else {
            effect = 4;
        }

        int flags = CF_TABLE[criticalFailureTableIndex][effect];
        if (flags == ObjTypes.Dam.NONE.bits()) {
            return 0;
        }

        attack.attackerFlags = attack.attackerFlags.or(ObjTypes.Dam.CRITICAL);
        attack.attackerFlags = attack.attackerFlags.or(new ObjTypes.Dam(flags));

        ObjTypes.Dam invalidFlags = attackFindInvalidFlags(attack.attacker, attack.weapon);
        attack.attackerFlags = attack.attackerFlags.and(invalidFlags.not());

        if ((attack.attackerFlags.bits() & ObjTypes.Dam.HIT_SELF.bits()) != 0) {
            int rounds = attackType == Item.AttackType.ATTACK_TYPE_RANGED.id()
                ? attack.ammoQuantity : 1;
            attackComputeDamage(attack, rounds, 2);
        } else if ((attack.attackerFlags.bits() & ObjTypes.Dam.EXPLODE.bits()) != 0) {
            attackComputeDamage(attack, 1, 2);
        }

        if ((attack.attackerFlags.bits() & ObjTypes.Dam.HURT_SELF.bits()) != 0) {
            attack.attackerDamage += GameRandom.randomBetween(1, 5);
        }

        if ((attack.attackerFlags.bits() & ObjTypes.Dam.LOSE_TURN.bits()) != 0) {
            attack.attacker.data.critter.combat.ap = 0;
        }

        if ((attack.attackerFlags.bits() & ObjTypes.Dam.LOSE_AMMO.bits()) != 0) {
            if (attackType == Item.AttackType.ATTACK_TYPE_RANGED.id()) {
                attack.ammoQuantity = Item.ammoGetQuantity(attack.weapon);
            } else {
                attack.attackerFlags = attack.attackerFlags.and(ObjTypes.Dam.LOSE_AMMO.not());
            }
        }

        if ((attack.attackerFlags.bits() & ObjTypes.Dam.CRIP_RANDOM.bits()) != 0) {
            ObjTypes.Dam[] flagsPtr = { attack.attackerFlags };
            doRandomCripple(flagsPtr);
            attack.attackerFlags = flagsPtr[0];
        }

        if ((attack.attackerFlags.bits() & ObjTypes.Dam.RANDOM_HIT.bits()) != 0) {
            // _combat_ai_random_target: AI seam, no random target.
            attack.defender = attack.intendedTarget;
            if (attack.defender != null) {
                attack.tile = attack.defender.tile;
            }
        }

        return 0;
    }

    // 0x42402C attackComputeCriticalHit.
    static int attackComputeCriticalHit(CombatDefs.Attack attack) {
        GameObject defender = attack.defender;
        if (defender != null
                && Critter.critterFlagCheck(defender, ObjTypes.CritterFlags.INVULNERABLE)) {
            return 2;
        }
        if (defender != null && ProtoTypes.ProtoId.ofGameObject(defender).objectType()
                != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 2;
        }

        attack.attackerFlags = attack.attackerFlags.or(ObjTypes.Dam.CRITICAL);

        int chance = GameRandom.randomBetween(1, 100)
            + Stat.critterGetStat(attack.attacker, StatDefs.Stat.STAT_BETTER_CRITICALS.id());

        int effect;
        if (chance <= 20) {
            effect = 0;
        } else if (chance <= 45) {
            effect = 1;
        } else if (chance <= 70) {
            effect = 2;
        } else if (chance <= 90) {
            effect = 3;
        } else if (chance <= 100) {
            effect = 4;
        } else {
            effect = 5;
        }

        int[] table;
        if (defender == ObjectSystem.getDude()) {
            table = CriticalHitTables.CRITICAL_HIT_TABLE_PLAYER
                    [attack.defenderHitLocation][effect];
        } else {
            int killType = Critter.critterGetKillType(defender).id();
            table = CriticalHitTables.CRITICAL_HIT_TABLES
                    [killType][attack.defenderHitLocation][effect];
        }

        int damageMultiplier = table[0];
        int flags = table[1];
        int massiveStat = table[2];
        int massiveModifier = table[3];
        int massiveFlags = table[4];
        int messageId = table[5];
        int massiveMessageId = table[6];

        attack.defenderFlags = attack.defenderFlags.or(new ObjTypes.Dam(flags));
        attack.criticalMessageId = messageId;

        if (massiveStat != -1) {
            if (Stat.statRoll(defender, massiveStat, massiveModifier, null).ordinal()
                    <= GameRandom.Roll.FAILURE.ordinal()) {
                attack.defenderFlags = attack.defenderFlags.or(new ObjTypes.Dam(massiveFlags));
                attack.criticalMessageId = massiveMessageId;
            }
        }

        if ((attack.defenderFlags.bits() & ObjTypes.Dam.CRIP_RANDOM.bits()) != 0) {
            ObjTypes.Dam[] flagsPtr = { attack.defenderFlags };
            doRandomCripple(flagsPtr);
            attack.defenderFlags = flagsPtr[0];
        }

        if (Item.weaponGetPerk(attack.weapon)
                == PerkDefs.Perk.PERK_WEAPON_ENHANCED_KNOCKOUT) {
            attack.defenderFlags = attack.defenderFlags.or(ObjTypes.Dam.KNOCKED_OUT);
        }

        GameObject weapon = null;
        if (defender != ObjectSystem.getDude()) {
            weapon = Item.getWeaponForHitMode(defender,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
        }
        ObjTypes.Dam invalidFlags = attackFindInvalidFlags(defender, weapon);
        attack.defenderFlags = attack.defenderFlags.and(invalidFlags.not());

        return damageMultiplier;
    }

    // 0x42346C attackComputeEnhancedKnockout.
    static int attackComputeEnhancedKnockout(CombatDefs.Attack attack) {
        if (Item.weaponGetPerk(attack.weapon)
                == PerkDefs.Perk.PERK_WEAPON_ENHANCED_KNOCKOUT) {
            int difficulty = Stat.critterGetStat(attack.attacker,
                StatDefs.Stat.STAT_STRENGTH.id()) - 8;
            int chance = GameRandom.randomBetween(1, 100);
            if (chance <= difficulty) {
                GameObject weapon = null;
                if (attack.defender != ObjectSystem.getDude()) {
                    weapon = Item.getWeaponForHitMode(attack.defender,
                        CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
                }
                if ((attackFindInvalidFlags(attack.defender, weapon).bits()
                        & ObjTypes.Dam.KNOCKED_OUT.bits()) == ObjTypes.Dam.NONE.bits()) {
                    attack.defenderFlags = attack.defenderFlags.or(ObjTypes.Dam.KNOCKED_OUT);
                }
            }
        }
        return 0;
    }

    private static final CombatDefs.Attack shootCtd = new CombatDefs.Attack();

    // 0x423128 _check_ranged_miss: a missed shot may still hit whoever stands
    // beyond/along the line, through the shoot-blocking walk.
    static boolean checkRangedMiss(CombatDefs.Attack attack) {
        int range = Item.weaponGetRange(attack.attacker, attack.hitMode);
        int to = TileMath.tileNumBeyond(attack.attacker.tile, attack.defender.tile, range);

        int roll = GameRandom.Roll.FAILURE.ordinal();
        GameObject critter = attack.attacker;
        if (critter != null) {
            int curr = attack.attacker.tile;
            while (curr != to) {
                critter = ObjectSystem.makeStraightPath(attack.attacker, curr, to);
                if (critter != null) {
                    if ((critter.flags.bits() & ObjTypes.ObjectFlags.SHOOT_THRU.bits()) == 0) {
                        if (FrmId.ofFid(critter.fid).objectType() != ObjectType.CRITTER) {
                            roll = GameRandom.Roll.SUCCESS.ordinal();
                            break;
                        }
                        if (critter != attack.defender) {
                            int v6 = attackDetermineToHit(attack.attacker, attack.attacker.tile,
                                critter, attack.defenderHitLocation, attack.hitMode, true) / 3;
                            if (Critter.critterIsDead(critter)) {
                                v6 = 5;
                            }
                            if (GameRandom.randomBetween(1, 100) <= v6) {
                                roll = GameRandom.Roll.SUCCESS.ordinal();
                                break;
                            }
                        }
                        curr = critter.tile;
                    }
                }
                if (critter == null) {
                    break;
                }
            }
        }

        attack.defenderHitLocation = CombatDefs.HitLocation.HIT_LOCATION_TORSO.id();
        if (roll < GameRandom.Roll.SUCCESS.ordinal() || critter == null
                || (critter.flags.bits() & ObjTypes.ObjectFlags.SHOOT_THRU.bits()) != 0) {
            return false;
        }

        attack.defender = critter;
        attack.tile = critter.tile;
        attack.attackerFlags = attack.attackerFlags.or(ObjTypes.Dam.HIT);
        attack.defenderHitLocation = CombatDefs.HitLocation.HIT_LOCATION_TORSO.id();
        attackComputeDamage(attack, 1, 2);
        return true;
    }

    // 0x423284 _shoot_along_path: walks the burst path, scoring hits on the
    // main target and collecting extras.
    static int shootAlongPath(CombatDefs.Attack attack, int endTile, int rounds, int anim) {
        int remainingRounds = rounds;
        int roundsHitMainTarget = 0;
        int currentTile = attack.attacker.tile;

        GameObject critter = attack.attacker;
        while (critter != null) {
            if ((remainingRounds <= 0 && anim != AnimationType.FIRE_CONTINUOUS.value())
                    || currentTile == endTile || attack.extrasLength >= 6) {
                break;
            }

            critter = ObjectSystem.makeStraightPath(attack.attacker, currentTile, endTile);

            if (critter != null) {
                if (FrmId.ofFid(critter.fid).objectType() != ObjectType.CRITTER) {
                    break;
                }

                int accuracy = attackDetermineToHit(attack.attacker, attack.attacker.tile,
                    critter, CombatDefs.HitLocation.HIT_LOCATION_TORSO.id(),
                    attack.hitMode, true);
                if (anim == AnimationType.FIRE_CONTINUOUS.value()) {
                    remainingRounds = 1;
                }

                int roundsHit = 0;
                while (GameRandom.randomBetween(1, 100) <= accuracy && remainingRounds > 0) {
                    remainingRounds -= 1;
                    roundsHit += 1;
                }

                if (roundsHit != 0) {
                    if (critter == attack.defender) {
                        roundsHitMainTarget += roundsHit;
                    } else {
                        int index = 0;
                        while (index < attack.extrasLength && critter != attack.extras[index]) {
                            index++;
                        }

                        attack.extrasHitLocation[index] = CombatDefs.HitLocation.HIT_LOCATION_TORSO.id();
                        attack.extras[index] = critter;
                        attackInit(shootCtd, attack.attacker, critter, attack.hitMode,
                            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());
                        shootCtd.attackerFlags = shootCtd.attackerFlags.or(ObjTypes.Dam.HIT);
                        attackComputeDamage(shootCtd, roundsHit, 2);

                        if (index == attack.extrasLength) {
                            attack.extrasDamage[index] = shootCtd.defenderDamage;
                            attack.extrasFlags[index] = shootCtd.defenderFlags;
                            attack.extrasKnockback[index] = shootCtd.defenderKnockback;
                            attack.extrasLength++;
                        } else {
                            if (anim == AnimationType.FIRE_BURST.value()) {
                                attack.extrasDamage[index] += shootCtd.defenderDamage;
                                attack.extrasFlags[index] = attack.extrasFlags[index].or(shootCtd.defenderFlags);
                                attack.extrasKnockback[index] += shootCtd.defenderKnockback;
                            }
                        }
                    }
                }

                currentTile = critter.tile;
            }
        }

        if (anim == AnimationType.FIRE_CONTINUOUS.value()) {
            roundsHitMainTarget = 0;
        }

        return roundsHitMainTarget;
    }

    private static final CombatDefs.Attack explosionCtd = new CombatDefs.Attack();

    // 0x426CC4 _combat_is_shot_blocked.
    static boolean combatIsShotBlocked(GameObject sourceObj, int from, int to,
            GameObject targetObj) {
        GameObject obstacle = sourceObj;
        int current = from;
        while (obstacle != null && current != to) {
            obstacle = ObjectSystem.makeStraightPath(sourceObj, current, to);
            if (obstacle != null) {
                if (FrmId.ofFid(obstacle.fid).objectType() != ObjectType.CRITTER
                        && obstacle != targetObj) {
                    return true;
                }
                if ((obstacle.flags.bits() & ObjTypes.ObjectFlags.MULTIHEX.bits()) != 0) {
                    current = obstacle.tile;
                    if (current != to) {
                        Rotation rotation = TileMath.tileGetRotationTo(current, to);
                        current = TileMath.tileGetTileInDirection(current, rotation, 1);
                    }
                } else {
                    current = obstacle.tile;
                }
            }
        }
        return false;
    }

    static void applyRangedMissTrajectory(CombatDefs.Attack attack,
            int distance, int range, boolean isGrenade) {
        int tile;
        if (isGrenade) {
            int throwDistance = GameRandom.randomBetween(1, distance / 2);
            if (throwDistance == 0) {
                throwDistance = 1;
            }
            int rotation = GameRandom.randomBetween(Rotation.FIRST, Rotation.LAST);
            tile = TileMath.tileGetTileInDirection(attack.defender.tile,
                Rotation.fromInt(rotation), throwDistance);
        } else {
            tile = TileMath.tileNumBeyond(attack.attacker.tile,
                attack.defender.tile, range);
        }

        attack.tile = tile;

        GameObject accidentalTarget = attack.defender;
        accidentalTarget = ObjectSystem.makeStraightPath(accidentalTarget,
            attack.defender.tile, attack.tile);
        if (accidentalTarget != null && accidentalTarget != attack.defender) {
            attack.tile = accidentalTarget.tile;
        } else {
            accidentalTarget = ObjectSystem.objBlockingAt(null, attack.tile,
                attack.defender.elevation);
        }

        if (accidentalTarget != null
                && (accidentalTarget.flags.bits() & ObjTypes.ObjectFlags.SHOOT_THRU.bits()) == 0) {
            attack.attackerFlags = attack.attackerFlags.or(ObjTypes.Dam.HIT);
            attack.defender = accidentalTarget;
            attackComputeDamage(attack, 1, 2);
        }
    }

    // 0x423C10 _compute_explosion_on_extras.
    static void computeExplosionOnExtras(CombatDefs.Attack attack,
            boolean isFromAttacker, boolean isGrenade, boolean noDamage) {
        GameObject targetObj;
        if (isFromAttacker) {
            targetObj = attack.attacker;
        } else {
            if ((attack.attackerFlags.bits() & ObjTypes.Dam.HIT.bits()) != 0) {
                targetObj = attack.defender;
            } else {
                targetObj = null;
            }
        }

        int explosionTile;
        if (targetObj != null) {
            explosionTile = targetObj.tile;
        } else {
            explosionTile = attack.tile;
        }

        if (explosionTile == -1) {
            return;
        }

        int ringTileIdx = 0;
        int radius = 0;
        Rotation rotation = Rotation.NE;
        int tile = -1;
        int ringFirstTile = explosionTile;
        int maxTargets = Item.explosionGetMaxTargets();

        while (attack.extrasLength < maxTargets) {
            if (radius != 0
                    && (tile == -1
                        || (tile = TileMath.tileGetTileInDirection(tile, rotation, 1)) != ringFirstTile)) {
                ringTileIdx++;
                if (ringTileIdx % radius == 0) {
                    rotation = Rotation.fromInt((rotation.value() + 1) % Rotation.COUNT);
                }
            } else {
                radius++;
                if (isGrenade && Item.weaponGetGrenadeExplosionRadius(attack.weapon) < radius) {
                    tile = -1;
                } else if (isGrenade
                        || Item.weaponGetRocketExplosionRadius(attack.weapon) >= radius) {
                    tile = TileMath.tileGetTileInDirection(ringFirstTile, Rotation.NE, 1);
                } else {
                    tile = -1;
                }

                ringFirstTile = tile;
                rotation = Rotation.SE;
                ringTileIdx = 0;
            }

            if (tile == -1) {
                break;
            }

            GameObject obstacle = ObjectSystem.objBlockingAt(targetObj, tile,
                attack.attacker.elevation);
            if (obstacle != null
                    && FrmId.ofFid(obstacle.fid).objectType() == ObjectType.CRITTER
                    && (obstacle.data.critter.combat.results.bits()
                        & ObjTypes.Dam.DEAD.bits()) == ObjTypes.Dam.NONE.bits()
                    && (obstacle.flags.bits() & ObjTypes.ObjectFlags.SHOOT_THRU.bits()) == 0
                    && !combatIsShotBlocked(obstacle, obstacle.tile, explosionTile, null)) {
                if (obstacle == attack.attacker) {
                    attack.attackerFlags = attack.attackerFlags.and(ObjTypes.Dam.HIT.not());
                    attackComputeDamage(attack, 1, 2);
                    attack.attackerFlags = attack.attackerFlags.or(ObjTypes.Dam.HIT);
                    attack.attackerFlags = attack.attackerFlags.or(ObjTypes.Dam.BACKWASH);
                } else {
                    int index = 0;
                    while (index < attack.extrasLength && attack.extras[index] != obstacle) {
                        index++;
                    }

                    if (index == attack.extrasLength) {
                        attack.extrasHitLocation[index] = CombatDefs.HitLocation.HIT_LOCATION_TORSO.id();
                        attack.extras[index] = obstacle;
                        attackInit(explosionCtd, attack.attacker, obstacle,
                            attack.hitMode,
                            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());
                        if (!noDamage) {
                            explosionCtd.attackerFlags = explosionCtd.attackerFlags.or(ObjTypes.Dam.HIT);
                            attackComputeDamage(explosionCtd, 1, 2);
                        }

                        attack.extrasDamage[index] = explosionCtd.defenderDamage;
                        attack.extrasFlags[index] = explosionCtd.defenderFlags;
                        attack.extrasKnockback[index] = explosionCtd.defenderKnockback;
                        attack.extrasLength++;
                    }
                }
            }
        }
    }

    // 0x4235C0 _compute_spray: burst and continuous fire.
    static int computeSpray(CombatDefs.Attack attack, int accuracy,
            int[] roundsHitMainTargetPtr, int[] roundsFiredPtr, int anim) {
        roundsHitMainTargetPtr[0] = 0;

        int currentAmmo = Item.ammoGetQuantity(attack.weapon);
        int burstRounds = Item.weaponGetBurstRounds(attack.weapon);
        int ammoCostPerRound = 1; // scriptHooks_AmmoCost: identity.

        if (ammoCostPerRound == 0) {
            roundsFiredPtr[0] = currentAmmo > 0 ? burstRounds : 0;
        } else {
            int roundsFired = currentAmmo / ammoCostPerRound;
            if (roundsFired > burstRounds) {
                roundsFired = burstRounds;
            }
            if (roundsFired == 0 && currentAmmo > 0) {
                roundsFired = 1;
            }
            roundsFiredPtr[0] = roundsFired;
        }

        int ammoQuantity = roundsFiredPtr[0];
        int criticalChance = Stat.critterGetStat(attack.attacker,
            StatDefs.Stat.STAT_CRITICAL_CHANCE.id());
        int roll = GameRandom.randomRoll(accuracy, criticalChance, null).ordinal();
        if (roll == GameRandom.Roll.CRITICAL_FAILURE.ordinal()) {
            return roll;
        }
        if (roll == GameRandom.Roll.CRITICAL_SUCCESS.ordinal()) {
            accuracy += 20;
        }

        int leftRounds;
        int mainTargetRounds;
        int centerRounds;
        int rightRounds;
        if (anim == AnimationType.FIRE_BURST.value()) {
            // gBurstModEnabled: sfall toggle, off.
            centerRounds = ammoQuantity / 3;
            if (centerRounds == 0) {
                centerRounds = 1;
            }
            leftRounds = ammoQuantity / 3;
            rightRounds = ammoQuantity - centerRounds - leftRounds;
            mainTargetRounds = centerRounds / 2;
            if (mainTargetRounds == 0) {
                mainTargetRounds = 1;
                centerRounds -= 1;
            }
        } else {
            leftRounds = 1;
            mainTargetRounds = 1;
            centerRounds = 1;
            rightRounds = 1;
        }

        for (int index = 0; index < mainTargetRounds; index++) {
            if (GameRandom.randomRoll(accuracy, 0, null).ordinal()
                    >= GameRandom.Roll.SUCCESS.ordinal()) {
                roundsHitMainTargetPtr[0] += 1;
            }
        }

        if (roundsHitMainTargetPtr[0] == 0 && checkRangedMiss(attack)) {
            roundsHitMainTargetPtr[0] = 1;
        }

        int range = Item.weaponGetRange(attack.attacker, attack.hitMode);
        int mainTargetEndTile = TileMath.tileNumBeyond(
            attack.attacker.tile, attack.defender.tile, range);
        roundsHitMainTargetPtr[0] += shootAlongPath(attack, mainTargetEndTile,
            centerRounds - roundsHitMainTargetPtr[0], anim);

        int centerTile;
        if (ObjectSystem.objectGetDistanceBetween(attack.attacker, attack.defender) <= 3) {
            centerTile = TileMath.tileNumBeyond(
                attack.attacker.tile, attack.defender.tile, 3);
        } else {
            centerTile = attack.defender.tile;
        }

        Rotation rotation = TileMath.tileGetRotationTo(centerTile, attack.attacker.tile);

        int leftTile = TileMath.tileGetTileInDirection(centerTile,
            Rotation.fromInt((rotation.value() + 1) % Rotation.COUNT), 1);
        int leftEndTile = TileMath.tileNumBeyond(attack.attacker.tile, leftTile, range);
        roundsHitMainTargetPtr[0] += shootAlongPath(attack, leftEndTile, leftRounds, anim);

        int rightTile = TileMath.tileGetTileInDirection(centerTile,
            Rotation.fromInt((rotation.value() + 5) % Rotation.COUNT), 1);
        int rightEndTile = TileMath.tileNumBeyond(attack.attacker.tile, rightTile, range);
        roundsHitMainTargetPtr[0] += shootAlongPath(attack, rightEndTile, rightRounds, anim);

        if (roll != GameRandom.Roll.FAILURE.ordinal()
                || (roundsHitMainTargetPtr[0] <= 0 && attack.extrasLength <= 0)) {
            if (roll >= GameRandom.Roll.SUCCESS.ordinal()
                    && roundsHitMainTargetPtr[0] == 0 && attack.extrasLength == 0) {
                roll = GameRandom.Roll.FAILURE.ordinal();
            }
        } else {
            roll = GameRandom.Roll.SUCCESS.ordinal();
        }

        return roll;
    }

    /**
     * Port of {@code attackCompute} (0x42378C). Burst/continuous fire and
     * the missed-shot interception are implemented; accidental trajectory
     * hits and explosion extras remain seams.
     */
    static int attackCompute(CombatDefs.Attack attack) {
        int range = Item.weaponGetRange(attack.attacker, attack.hitMode);
        int distance = TileMath.tileDistanceBetween(attack.attacker.tile, attack.defender.tile);

        if (range < distance) {
            return -1;
        }

        int anim = Item.critterGetAnimationForHitMode(attack.attacker, attack.hitMode);
        int accuracy = attackDetermineToHit(attack.attacker, attack.attacker.tile,
            attack.defender, attack.defenderHitLocation, attack.hitMode, true);

        boolean isGrenade = false;
        ProtoTypes.DamageType damageType = Item.weaponGetDamageType(attack.attacker, attack.weapon);
        if (anim == AnimationType.THROW_ANIM.value()
                && (damageType.id() == explosionGetDamageType()
                    || damageType == ProtoTypes.DamageType.DAMAGE_TYPE_PLASMA
                    || damageType == ProtoTypes.DamageType.DAMAGE_TYPE_EMP)) {
            isGrenade = true;
        }

        if (attack.defenderHitLocation == CombatDefs.HitLocation.HIT_LOCATION_UNCALLED.id()) {
            attack.defenderHitLocation = CombatDefs.HitLocation.HIT_LOCATION_TORSO.id();
        }

        int attackType = Item.weaponGetAttackTypeForHitMode(attack.weapon, attack.hitMode);
        int roundsHitMainTarget = 1;
        int damageMultiplier = 2;
        int roundsFired = 1;

        int roll;
        if (anim == AnimationType.FIRE_BURST.value()
                || anim == AnimationType.FIRE_CONTINUOUS.value()) {
            int[] mainHit = { 0 };
            int[] fired = { 0 };
            roll = computeSpray(attack, accuracy, mainHit, fired, anim);
            roundsHitMainTarget = mainHit[0];
            roundsFired = fired[0];
        } else {
            int chance = Stat.critterGetStat(attack.attacker,
                StatDefs.Stat.STAT_CRITICAL_CHANCE.id());
            roll = GameRandom.randomRoll(accuracy,
                chance - Combat.getHitLocationPenalty(attack.defenderHitLocation), null).ordinal();
        }

        GameObject dude = ObjectSystem.getDude();
        if (roll == GameRandom.Roll.FAILURE.ordinal()) {
            if (TraitDefs.traitIsSelectedAndActive(TraitDefs.Trait.TRAIT_JINXED.id())
                    || Perk.perkHasRank(dude, Perk.PERK_JINXED)) {
                if (GameRandom.randomBetween(0, 1) == 1) {
                    roll = GameRandom.Roll.CRITICAL_FAILURE.ordinal();
                }
            }
        }

        if (roll == GameRandom.Roll.SUCCESS.ordinal()) {
            if ((attackType == Item.AttackType.ATTACK_TYPE_MELEE.id()
                    || attackType == Item.AttackType.ATTACK_TYPE_UNARMED.id())
                    && attack.attacker == dude) {
                if (Perk.perkHasRank(attack.attacker, Perk.PERK_SLAYER)) {
                    roll = GameRandom.Roll.CRITICAL_SUCCESS.ordinal();
                }
                if (Perk.perkHasRank(dude, Perk.PERK_SILENT_DEATH)
                        && !isHitFromFront(dude, attack.defender)
                        && Critter.dudeHasState(Critter.DUDE_STATE_SNEAKING)
                        && dude != attack.defender.data.critter.combat.whoHitMe) {
                    damageMultiplier = 4;
                }
                int bonusCriticalChance = Item.unarmedGetBonusCriticalChance(attack.hitMode);
                if (bonusCriticalChance != 0
                        && GameRandom.randomBetween(1, 100) <= bonusCriticalChance) {
                    roll = GameRandom.Roll.CRITICAL_SUCCESS.ordinal();
                }
            }
        }

        if (attackType == Item.AttackType.ATTACK_TYPE_RANGED.id()
                && roll == GameRandom.Roll.SUCCESS.ordinal()
                && attack.attacker == dude) {
            if (Perk.perkGetRank(dude, Perk.PERK_SNIPER) != 0) {
                int d10 = GameRandom.randomBetween(1, 10);
                if (d10 <= Stat.critterGetStat(dude, StatDefs.Stat.STAT_LUCK.id())) {
                    roll = GameRandom.Roll.CRITICAL_SUCCESS.ordinal();
                }
            }
        }

        // scriptHooks_AfterHitRoll: identity.

        if (Item.ammoGetCapacity(attack.weapon) > 0) {
            int rounds = 1;
            if (anim == AnimationType.FIRE_BURST.value()
                    || anim == AnimationType.FIRE_CONTINUOUS.value()) {
                rounds = roundsFired;
            }
            attack.ammoQuantity = rounds;
            int[] ammoQty = { rounds };
            if (Item.weaponComputeAmmoCost(attack.weapon, ammoQty) == -1) {
                return -1;
            }
            attack.ammoQuantity = ammoQty[0];
            // scriptHooks_AmmoCost: identity.
        } else if (attackType == Item.AttackType.ATTACK_TYPE_RANGED.id()) {
            attack.ammoQuantity = roundsFired;
        }

        switch (roll) {
        case 3: // CRITICAL_SUCCESS
            damageMultiplier = attackComputeCriticalHit(attack);
            if ((attackType == Item.AttackType.ATTACK_TYPE_MELEE.id()
                    || attackType == Item.AttackType.ATTACK_TYPE_UNARMED.id())
                    && attack.attacker == dude) {
                if (Perk.perkHasRank(dude, Perk.PERK_SILENT_DEATH)
                        && !isHitFromFront(dude, attack.defender)
                        && Critter.dudeHasState(Critter.DUDE_STATE_SNEAKING)
                        && dude != attack.defender.data.critter.combat.whoHitMe) {
                    damageMultiplier *= 2;
                }
            }
            // fallthrough
        case 2: // SUCCESS
            attack.attackerFlags = attack.attackerFlags.or(ObjTypes.Dam.HIT);
            attackComputeEnhancedKnockout(attack);
            attackComputeDamage(attack, roundsHitMainTarget, damageMultiplier);
            break;
        case 1: // FAILURE
            if (attackType == Item.AttackType.ATTACK_TYPE_RANGED.id()
                    || attackType == Item.AttackType.ATTACK_TYPE_THROW.id()) {
                checkRangedMiss(attack);
            }
            break;
        case 0: // CRITICAL_FAILURE
            attackComputeCriticalFailure(attack);
            break;
        }

        if (attackType == Item.AttackType.ATTACK_TYPE_RANGED.id()
                || attackType == Item.AttackType.ATTACK_TYPE_THROW.id()) {
            if ((attack.attackerFlags.bits()
                    & (ObjTypes.Dam.HIT.bits() | ObjTypes.Dam.CRITICAL.bits())) == 0) {
                applyRangedMissTrajectory(attack, distance, range, isGrenade);
            }
        }

        if (damageType.id() == explosionGetDamageType() || isGrenade) {
            if ((attack.attackerFlags.bits() & ObjTypes.Dam.HIT.bits()) != 0
                    || (attack.attackerFlags.bits() & ObjTypes.Dam.CRITICAL.bits()) == 0) {
                computeExplosionOnExtras(attack, false, isGrenade, false);
            } else {
                if ((attack.attackerFlags.bits() & ObjTypes.Dam.EXPLODE.bits()) != 0) {
                    computeExplosionOnExtras(attack, true, isGrenade, false);
                }
            }
        }

        attackComputeDeathFlags(attack);

        return 0;
    }

    /**
     * Port of {@code _combat_should_end} (0x422C4C).
     */
    static boolean combatShouldEnd() {
        if (listCom <= 1) {
            return true;
        }

        int index;
        GameObject dude = ObjectSystem.getDude();
        for (index = 0; index < listCom; index++) {
            if (combatList[index] == dude) {
                break;
            }
        }

        if (index == listCom) {
            return true;
        }

        int team = dude.data.critter.combat.team;

        for (index = 0; index < listCom; index++) {
            GameObject critter = combatList[index];
            GameObject critterWhoHitMe = critter.data.critter.combat.whoHitMe;
            if (critterWhoHitMe == null) {
                continue;
            }

            if ((critterWhoHitMe.data.critter.combat.results.bits() & ObjTypes.Dam.KNOCKED_OUT.bits()) != 0) {
                continue;
            }

            boolean targetAlive = (critterWhoHitMe.data.critter.combat.results.bits()
                    & ObjTypes.Dam.DEAD.bits()) == ObjTypes.Dam.NONE.bits();
            if (critter.data.critter.combat.team == team) {
                if ((critterWhoHitMe.data.critter.combat.maneuver.bits()
                        & ObjTypes.CritterManeuver.DISENGAGING.bits()) == 0
                        && targetAlive) {
                    break;
                }
            } else {
                if (critterWhoHitMe.data.critter.combat.team == team || targetAlive) {
                    break;
                }
            }
        }

        return index == listCom;
    }

    private static boolean killTypeIsValid(int killType) {
        return killType >= 0 && killType < killsByType.length;
    }

    /** Port of {@code killsIncByType} (src/critter.cc 0x42D878). */
    public static int killsIncByType(int killType) {
        if (killTypeIsValid(killType)) {
            killsByType[killType]++;
            return 0;
        }
        return -1;
    }

    /** Port of {@code killsGetByType}. */
    public static int killsGetByType(int killType) {
        if (killTypeIsValid(killType)) {
            return killsByType[killType];
        }
        return 0;
    }

    /** The combat experience bank ({@code _combat_exps}). */
    public static int combatExps() {
        return combatExps;
    }

    // ===== damage application (src/combat.cc) =====

    /**
     * Port of {@code _compute_dmg_damage} (0x413660): threshold, then
     * resistance, clamped at zero, with the multihex/electrical knockback
     * gate.
     */
    public static int computeDmgDamage(int min, int max, ObjTypes.GameObject obj,
            int[] knockbackDistancePtr, ProtoTypes.DamageType damageType) {
        if (Critter.critterFlagCheck(obj, ObjTypes.CritterFlags.NO_KNOCKBACK)) {
            knockbackDistancePtr = null;
        }

        int damage = GameRandom.randomBetween(min, max)
            - Stat.critterGetStat(obj, StatDefs.Stat.STAT_DAMAGE_THRESHOLD.id() + damageType.id());
        if (damage > 0) {
            damage -= Stat.critterGetStat(obj,
                StatDefs.Stat.STAT_DAMAGE_RESISTANCE.id() + damageType.id()) * damage / 100;
        }

        if (damage < 0) {
            damage = 0;
        }

        if (knockbackDistancePtr != null) {
            if ((obj.flags.bits() & ObjTypes.ObjectFlags.MULTIHEX.bits()) == 0
                    && damageType != ProtoTypes.DamageType.DAMAGE_TYPE_ELECTRICAL) {
                knockbackDistancePtr[0] = damage / 10;
            }
        }

        return damage;
    }

    // 0x424EE8 _check_for_death.
    private static void checkForDeath(ObjTypes.GameObject object, int damage, ObjTypes.Dam[] flags) {
        if (object == null || !Critter.critterFlagCheck(object, ObjTypes.CritterFlags.INVULNERABLE)) {
            if (object == null || ProtoTypes.ProtoId.ofGameObject(object).objectType()
                    == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
                if (damage > 0) {
                    if (Critter.critterGetHitPoints(object) - damage <= 0) {
                        flags[0] = flags[0].or(ObjTypes.Dam.DEAD);
                    }
                }
            }
        }
    }

    /** Port of {@code attackComputeDeathFlags} (0x424BB0). */
    public static void attackComputeDeathFlags(CombatDefs.Attack attack) {
        ObjTypes.Dam[] attackerFlags = { attack.attackerFlags };
        ObjTypes.Dam[] defenderFlags = { attack.defenderFlags };
        checkForDeath(attack.attacker, attack.attackerDamage, attackerFlags);
        checkForDeath(attack.defender, attack.defenderDamage, defenderFlags);
        attack.attackerFlags = attackerFlags[0];
        attack.defenderFlags = defenderFlags[0];

        for (int index = 0; index < attack.extrasLength; index++) {
            ObjTypes.Dam[] extraFlags = { attack.extrasFlags[index] };
            checkForDeath(attack.extras[index], attack.extrasDamage[index], extraFlags);
            attack.extrasFlags[index] = extraFlags[0];
        }
    }

    // 0x424F2C _set_new_results.
    private static void setNewResults(ObjTypes.GameObject critter, ObjTypes.Dam flags) {
        if (critter == null) {
            return;
        }
        if (FrmId.ofFid(critter.fid).objectType() != ObjectType.CRITTER) {
            return;
        }
        if (Critter.critterFlagCheck(critter, ObjTypes.CritterFlags.INVULNERABLE)) {
            return;
        }

        if ((flags.bits() & ObjTypes.Dam.DEAD.bits()) != 0) {
            EventQueue.queueRemoveEvents(critter);
        } else if ((flags.bits() & ObjTypes.Dam.KNOCKED_OUT.bits()) != 0) {
            EventQueue.queueRemoveEventsByType(critter, EventQueue.EventType.KNOCKOUT);
            int endurance = Stat.critterGetStat(critter, StatDefs.Stat.STAT_ENDURANCE.id());
            EventQueue.queueAddEvent(10 * (35 - 3 * endurance), critter, null,
                EventQueue.EventType.KNOCKOUT);
        }

        // The dude arm-cripple interface update is a game-phase seam.
        critter.data.critter.combat.results = critter.data.critter.combat.results.or(flags.and(
            ObjTypes.Dam.KNOCKED_OUT.or(ObjTypes.Dam.KNOCKED_DOWN).or(ObjTypes.Dam.CRIP)
                .or(ObjTypes.Dam.DEAD).or(ObjTypes.Dam.LOSE_TURN)));
    }

    // 0x425020 _damage_object.
    private static void damageObject(ObjTypes.GameObject target, int damage, boolean animated,
            boolean hitUnintendedTarget, ObjTypes.GameObject damageSource) {
        if (target == null) {
            return;
        }
        if (FrmId.ofFid(target.fid).objectType() != ObjectType.CRITTER) {
            return;
        }
        if (Critter.critterFlagCheck(target, ObjTypes.CritterFlags.INVULNERABLE)) {
            return;
        }
        if (damage <= 0) {
            return;
        }

        Critter.critterAdjustHitPoints(target, -damage);

        // dude interfaceRenderHitPoints(animated): game-phase seam.
        target.data.critter.combat.damageLastTurn += damage;

        if (!hitUnintendedTarget) {
            if (!PartyMembers.objectIsPartyMember(target) || !PartyMembers.objectIsPartyMember(damageSource)) {
                ObjectSystem.scriptSetFixedParam(target.sid, damage);
                ObjectSystem.scriptExecProc(target.sid, ObjectSystem.SCRIPT_PROC_DAMAGE);
            }
        }

        if ((target.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) != 0) {
            ObjectSystem.scriptSetObjects(target.sid, target.data.critter.combat.whoHitMe, null);
            ObjectSystem.scriptExecProc(target.sid, ObjectSystem.SCRIPT_PROC_DESTROY);
            Item.itemDestroyAllHidden(target);

            if (target != ObjectSystem.getDude()) {
                ObjTypes.GameObject whoHitMe = target.data.critter.combat.whoHitMe;
                ObjTypes.GameObject dude = ObjectSystem.getDude();
                if (whoHitMe == dude || (whoHitMe != null && dude != null
                        && whoHitMe.data.critter.combat.team == dude.data.critter.combat.team)) {
                    boolean scriptOverrides = false;
                    ObjectSystem.Script[] slot = new ObjectSystem.Script[1];
                    if (ObjectSystem.scriptGetScript(target.sid, slot) != -1) {
                        scriptOverrides = slot[0].scriptOverrides != 0;
                    }
                    if (!scriptOverrides) {
                        combatExps += Critter.critterGetExp(target);
                        killsIncByType(Critter.critterGetKillType(target).id());
                    }
                }
            }

            if (target.sid != -1) {
                ObjectSystem.scriptRemove(target.sid);
                target.sid = -1;
            }

            PartyMembers.partyMemberRemove(target);
            // scriptHooks_OnDeath(target): identity headless.
        }
    }

    // 0x424C04 _apply_damage. The AI bystander/retaliation notifications
    // are game-phase seams; the script procs run through the registry.
    public static void applyDamage(CombatDefs.Attack attack, boolean animated) {
        ObjTypes.GameObject attacker = attack.attacker;
        boolean attackerIsCritter = attacker != null
                && FrmId.ofFid(attacker.fid).objectType() == ObjectType.CRITTER;
        boolean hitUnintendedTarget = attack.defender != attack.intendedTarget;

        if (attackerIsCritter && (attacker.data.critter.combat.results.bits()
                & ObjTypes.Dam.DEAD.bits()) == ObjTypes.Dam.NONE.bits()) {
            setNewResults(attacker, attack.attackerFlags);
            damageObject(attacker, attack.attackerDamage, animated, hitUnintendedTarget, attacker);
        }

        // bystanders might flee: _combatai_notify_onlookers (game phase).
        ObjTypes.GameObject defender = attack.defender;
        boolean defenderIsCritter = defender != null
                && FrmId.ofFid(defender.fid).objectType() == ObjectType.CRITTER;

        if (!defenderIsCritter && !hitUnintendedTarget) {
            boolean shouldRunDamageProc = !PartyMembers.objectIsPartyMember(attack.defender)
                    || !PartyMembers.objectIsPartyMember(attack.attacker);
            if (shouldRunDamageProc) {
                if (defender != null && defender.sid != -1) {
                    ObjectSystem.scriptSetFixedParam(defender.sid, attack.attackerDamage);
                    ObjectSystem.scriptSetObjects(defender.sid, attack.attacker, attack.weapon);
                    ObjectSystem.scriptExecProc(defender.sid, ObjectSystem.SCRIPT_PROC_DAMAGE);
                }
            }
        }

        if (defenderIsCritter && (defender.data.critter.combat.results.bits()
                & ObjTypes.Dam.DEAD.bits()) == ObjTypes.Dam.NONE.bits()) {
            setNewResults(defender, attack.defenderFlags);

            if ((defender.data.critter.combat.results.bits()
                    & (ObjTypes.Dam.DEAD.bits() | ObjTypes.Dam.KNOCKED_OUT.bits())) != 0) {
                if (!hitUnintendedTarget || defender != ObjectSystem.getDude()) {
                    Critter.critterSetWhoHitMe(defender, attack.attacker);
                }
            } else if (attackerIsCritter
                    && (defender == attack.intendedTarget
                        || defender.data.critter.combat.team != attacker.data.critter.combat.team)) {
                // _combatai_check_retaliation(defender, attack.attacker): game phase.
            }

            ObjectSystem.scriptSetObjects(defender.sid, attack.attacker, attack.weapon);
            damageObject(defender, attack.defenderDamage, animated, hitUnintendedTarget, attacker);

            if (attack.defenderDamage >= 0 && (attack.attackerFlags.bits()
                    & ObjTypes.Dam.HIT.bits()) != ObjTypes.Dam.NONE.bits()) {
                ObjectSystem.scriptSetObjects(attack.attacker.sid, null, attack.defender);
                ObjectSystem.scriptSetFixedParam(attack.attacker.sid, 2);
                ObjectSystem.scriptExecProc(attack.attacker.sid, ObjectSystem.SCRIPT_PROC_COMBAT);
            }
        }

        for (int index = 0; index < attack.extrasLength; index++) {
            ObjTypes.GameObject obj = attack.extras[index];
            if (FrmId.ofFid(obj.fid).objectType() == ObjectType.CRITTER
                    && (obj.data.critter.combat.results.bits()
                        & ObjTypes.Dam.DEAD.bits()) == ObjTypes.Dam.NONE.bits()) {
                setNewResults(obj, attack.extrasFlags[index]);

                if (defenderIsCritter) {
                    if ((obj.data.critter.combat.results.bits()
                            & (ObjTypes.Dam.DEAD.bits() | ObjTypes.Dam.KNOCKED_OUT.bits())) != 0) {
                        Critter.critterSetWhoHitMe(obj, attack.attacker);
                    } else if (attackerIsCritter
                            && obj.data.critter.combat.team != attacker.data.critter.combat.team) {
                        // _combatai_check_retaliation(obj, attack.attacker): game phase.
                    }
                }

                ObjectSystem.scriptSetObjects(obj.sid, attack.attacker, attack.weapon);
                damageObject(obj, attack.extrasDamage[index], animated, hitUnintendedTarget, attack.attacker);

                if (attack.extrasDamage[index] >= 0 && (attack.attackerFlags.bits()
                        & ObjTypes.Dam.HIT.bits()) != ObjTypes.Dam.NONE.bits()) {
                    ObjectSystem.scriptSetObjects(attack.attacker.sid, null, obj);
                    ObjectSystem.scriptSetFixedParam(attack.attacker.sid, 2);
                    ObjectSystem.scriptExecProc(attack.attacker.sid, ObjectSystem.SCRIPT_PROC_COMBAT);
                }
            }
        }
    }

    /**
     * Port of {@code actionDamage} (0x4122E0), the un-animated arm: the
     * force-field attacker is hidden and placed, the target is the blocking
     * object, the damage bypasses armor or runs through the threshold and
     * resistance, a lethal hit kills before the report applies the damage
     * the same way.
     */
    public static void actionDamage(int tile, int elevation, int minDamage, int maxDamage,
            ProtoTypes.DamageType damageType, boolean animated, boolean bypassArmor) {
        ObjTypes.GameObject[] attackerSlot = new ObjTypes.GameObject[1];
        if (ObjectSystem.objectCreateWithFrmIdProtoId(attackerSlot,
                FrmId.ofType(ObjectType.SCENERY, ArtDefs.SceneryFrameId.ForceField3.id()),
                ProtoTypes.ProtoId.empty()) == -1) {
            return;
        }
        ObjTypes.GameObject attacker = attackerSlot[0];
        ObjectSystem.objectHide(attacker);
        attacker.flags = attacker.flags.or(ObjTypes.ObjectFlags.NO_SAVE);
        ObjectSystem.objectSetLocation(attacker, tile, elevation);

        ObjTypes.GameObject defender = ObjectSystem.objBlockingAt(null, tile, elevation);
        CombatDefs.Attack attack = new CombatDefs.Attack();
        attackInit(attack, attacker, defender, CombatDefs.HitMode.HIT_MODE_PUNCH.id(),
            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());
        attack.tile = tile;
        attack.attackerFlags = ObjTypes.Dam.HIT;

        if (defender != null) {
            int damage;
            if (bypassArmor) {
                damage = maxDamage;
            } else {
                int[] knockback = { 0 };
                damage = computeDmgDamage(minDamage, maxDamage, defender, knockback, damageType);
                attack.defenderKnockback = knockback[0];
            }
            attack.defenderDamage = damage;
        }

        attackComputeDeathFlags(attack);

        if (animated) {
            // The animation queue of the animated arm is game phase.
        } else {
            if (defender != null) {
                if ((attack.defenderFlags.bits() & ObjTypes.Dam.DEAD.bits()) != ObjTypes.Dam.NONE.bits()) {
                    Critter.critterKill(defender, -1, true);
                }
            }
            applyDamage(attack, false);
            ObjectSystem.objectDestroy(attacker);
        }
    }

    /** Test seam: fresh-process mirror (BSS zeroes: penalties + highlight). */
    static void resetForTests() {
        java.util.Arrays.fill(hitLocationPenalty, 0);
        combatHighlight = 0;
        java.util.Arrays.fill(killsByType, 0);
        combatExps = 0;
        combatState = COMBAT_STATE_OUT_COMBAT;
        combatList = null;
        listTotal = 0;
        listCom = 0;
        listNoncom = 0;
        combatNumTurns = 0;
        gcsd = null;
        combatTurnObj = null;
        combatFreeMove = 0;
        combatElev = 0;
        combatTurnRunning = 0;
        combatCleanupEnabled = false;
        combatCallDisplay = false;
        combatEndingGuy = null;
        attackScheduler = HEADLESS_ATTACK_SCHEDULER;
    }
}
