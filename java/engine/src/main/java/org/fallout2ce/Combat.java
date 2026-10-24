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

    /** Test seam: fresh-process mirror (BSS zeroes: penalties + highlight). */
    static void resetForTests() {
        java.util.Arrays.fill(hitLocationPenalty, 0);
        combatHighlight = 0;
    }
}
