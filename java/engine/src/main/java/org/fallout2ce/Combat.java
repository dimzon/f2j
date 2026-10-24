package org.fallout2ce;

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

        if (toHit > 95) {
            toHit = 95;
        }

        return toHit;
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

    /** Port of {@code isInCombat} (src/combat.h). */
    public static boolean isInCombat() {
        return (combatState & COMBAT_STATE_IN_COMBAT) != 0;
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
    }
}
