package org.fallout2ce;

import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Stat configured bounds + critter stat engine slice, mirroring the pure parts
 * of {@code src/stat.cc}.
 *
 * <p>The stat engine itself comes with the game phase; {@code statsInit} needs
 * the message/pc stacks, so the bounds tables stay zero-initialized until set,
 * exactly like the headless C++ harness state.
 *
 * <p>Vectors: {@code [critterstats]} in
 * {@code docker/refharness/golden/v58.txt}, produced by
 * {@code sectionCritterStats} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>{@code gDude} (set by the object/game ports, unported) is modelled as a
 * {@code null} holder below: there is no {@code gDude} in
 * {@link ObjectSystem} yet, so every {@code critter == gDude} check reads
 * false for non-null critters (see {@link #isDude}). All dude-only branches
 * (trait adjust on set, perk/item/vault bonuses, tag-adjacent armor logic)
 * are therefore dormant until the game phase. The {@code null == null} C
 * quirk is avoided on purpose: {@link #isDude} guards against null, so a
 * null critter is never "the dude".</li>
 * <li>Combat state ({@code isInCombat} / {@code _combat_whose_turn},
 * {@code src/combat.h}) is unported: {@link #isInCombat} returns false and
 * {@link #combatWhoseTurn} returns null, so the {@code STAT_ARMOR_CLASS}
 * combat branch is dormant headless (like the harness, which never enters
 * combat). The branch is still transcribed faithfully.</li>
 * <li>Inventory weight ({@code objectGetInventoryWeight},
 * {@code src/object.cc}) is unported: {@link #objectGetInventoryWeight}
 * returns 0 (harness inventories are empty, so the
 * {@code STAT_MAXIMUM_ACTION_POINTS} overweight branch is dormant for the
 * vectors).</li>
 * <li>Equipped-item / weapon-animation helpers ({@code critterGetItem1/2},
 * {@code itemGetType}, {@code weaponGetAnimationCode}) are unported: the
 * private stubs below return null / -1 / 0, so the HTH_EVADE
 * {@code hasWeapon} scan stays false. Dormant (dude-only + no combat).</li>
 * <li>Perk lookups ({@code perkGetRank}, {@code perkGetSkillModifier},
 * vault inoculation bonuses) need the perk engine ({@code src/perk.cc},
 * unported; {@link Perk} only carries property ids). The private stubs
 * return 0 and are only reachable from dude-gated branches, so the vectors
 * never observe them.</li>
 * <li>{@code critterSetBaseStat} MUTATES shared {@link ProtoDb} cache data
 * on success (like C, which writes into the cached {@code Proto*}). The
 * golden path never commits a write (the only set attempt returns -3
 * before the store), so tests must mirror the harness exactly and perform
 * no committed writes; otherwise the {@link ProtoDb} cache must be
 * reloaded. See {@code CritterStatsTest}.</li>
 * <li>{@code critterAdjustHitPoints} kill tail ({@code critterKill},
 * unported) is deferred: hp is clamped to the computed maximum without
 * killing. Poison/radiation adjusts stay dude-gated (non-dude returns -1
 * before any queue/message side effects, like C).</li>
 * </ul>
 */
public final class Stat {

    private Stat() {
    }

    public static final int STAT_ERR_INVALID_STAT = -5;

    // Configured bounds tables (src/stat.cc: pcStatMaximums/Minimums,
    // npcStatMaximums/Minimums). Zero-init mirrors the C++ statics before
    // statsInit runs.
    private static final int[] PC_STAT_MAXIMUMS = new int[StatDefs.SAVEABLE_STAT_COUNT];
    private static final int[] PC_STAT_MINIMUMS = new int[StatDefs.SAVEABLE_STAT_COUNT];
    private static final int[] NPC_STAT_MAXIMUMS = new int[StatDefs.SAVEABLE_STAT_COUNT];
    private static final int[] NPC_STAT_MINIMUMS = new int[StatDefs.SAVEABLE_STAT_COUNT];

    /**
     * Port of {@code gDude} ({@code src/object.cc}, unported): null headless,
     * so all dude branches are dormant (see class notes).
     */
    private static final GameObject G_DUDE = null;

    private static boolean isDude(GameObject critter) {
        return critter != null && critter == G_DUDE;
    }

    // Port of `unspentApBonus` / `unspentApPerkBonus` (src/stat.cc, init 4).
    private static int unspentApBonus = 4;
    private static int unspentApPerkBonus = 4;

    /** Port of {@code statGetConfiguredMaximum}: out-of-range stat yields 0. */
    public static int getConfiguredMaximum(int stat, boolean npc) {
        if (stat >= 0 && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            return npc ? NPC_STAT_MAXIMUMS[stat] : PC_STAT_MAXIMUMS[stat];
        }
        return 0;
    }

    /** Port of {@code statGetConfiguredMinimum}: out-of-range stat yields 0. */
    public static int getConfiguredMinimum(int stat, boolean npc) {
        if (stat >= 0 && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            return npc ? NPC_STAT_MINIMUMS[stat] : PC_STAT_MINIMUMS[stat];
        }
        return 0;
    }

    /** Port of {@code statSetPcMaximum}: out-of-range stat is ignored. */
    public static void setPcMaximum(int stat, int maximum) {
        if (stat >= 0 && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            PC_STAT_MAXIMUMS[stat] = maximum;
        }
    }

    /** Port of {@code statSetPcMinimum}: out-of-range stat is ignored. */
    public static void setPcMinimum(int stat, int minimum) {
        if (stat >= 0 && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            PC_STAT_MINIMUMS[stat] = minimum;
        }
    }

    /** Port of {@code statSetUnspentApBonus}. */
    public static void statSetUnspentApBonus(int multiplier) {
        unspentApBonus = multiplier;
    }

    /** Port of {@code statGetUnspentApBonus}. */
    public static int statGetUnspentApBonus() {
        return unspentApBonus;
    }

    /** Port of {@code statSetUnspentApPerkBonus}. */
    public static void statSetUnspentApPerkBonus(int multiplier) {
        unspentApPerkBonus = multiplier;
    }

    /** Port of {@code statGetUnspentApPerkBonus}. */
    public static int statGetUnspentApPerkBonus() {
        return unspentApPerkBonus;
    }

    // Port of `statGetMaximum` (src/stat.cc): dude reads the pc tables,
    // everyone else the npc tables. Headless both are zero.
    private static int statGetMaximum(GameObject critter, int stat) {
        if (stat < 0 || stat >= StatDefs.SAVEABLE_STAT_COUNT) {
            return 0;
        }
        return isDude(critter) ? PC_STAT_MAXIMUMS[stat] : NPC_STAT_MAXIMUMS[stat];
    }

    // Port of `statGetMinimum` (src/stat.cc).
    private static int statGetMinimum(GameObject critter, int stat) {
        if (stat < 0 || stat >= StatDefs.SAVEABLE_STAT_COUNT) {
            return 0;
        }
        return isDude(critter) ? PC_STAT_MINIMUMS[stat] : NPC_STAT_MINIMUMS[stat];
    }

    // Seam for `isInCombat()` (src/combat.h): combat state unported, default
    // false (harness never enters combat).
    private static boolean isInCombat() {
        return false;
    }

    // Seam for `_combat_whose_turn()` (src/combat.cc): no combat, no turn.
    private static GameObject combatWhoseTurn() {
        return null;
    }

    // Seam for `objectGetInventoryWeight()` (src/object.cc, unported):
    // harness inventories are empty, so 0 is faithful for the vectors.
    private static int objectGetInventoryWeight(GameObject critter) {
        return 0;
    }

    // Seams for the equipped-item / weapon-animation helpers (unported, only
    // reachable from dormant dude/combat branches).
    private static GameObject critterGetItem1(GameObject critter) {
        return null;
    }

    private static GameObject critterGetItem2(GameObject critter) {
        return null;
    }

    private static int itemGetType(GameObject item) {
        return -1;
    }

    private static int weaponGetAnimationCode(GameObject weapon) {
        return 0;
    }

    // Seams for the perk engine (src/perk.cc, unported): only reachable from
    // dude-gated branches, so 0 is faithful for the vectors.
    private static int perkGetRank(GameObject critter, int perk) {
        return 0;
    }

    private static int perkGetVaultCityInoculationsRadBonus() {
        return 0;
    }

    private static int perkGetVaultCityInoculationsPoisonBonus() {
        return 0;
    }

    private static Proto protoFor(GameObject critter) {
        if (critter == null) {
            return null;
        }
        Proto[] slot = new Proto[1];
        if (ProtoDb.protoGetProto(ProtoId.ofGameObject(critter), slot) == -1) {
            return null;
        }
        return slot[0];
    }

    /**
     * Port of {@code critterGetStat} (src/stat.cc 0x4AEF48): base + trait
     * (dude-only) + bonus, per-stat switch (blindness, overweight AP drain,
     * armor-class combat branch, AGE), dude perk switch, then the
     * min/max clamp against the (zero headless) bounds tables. Non-saveable
     * stats read the live hp/poison/radiation fields, anything else is 0.
     */
    public static int critterGetStat(GameObject critter, int stat) {
        if (critter == null
                || ProtoId.ofGameObject(critter).objectType()
                        != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }

        if (stat >= StatDefs.STAT_FIRST && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            int value = critterGetBaseStatWithTraitModifier(critter, stat);
            value += critterGetBonusStat(critter, stat);

            switch (stat) {
            case 1: // STAT_PERCEPTION
                if ((critter.data.critter.combat.results.bits() & Dam.BLIND.bits()) != 0) {
                    value -= 5;
                }
                break;
            case 8: { // STAT_MAXIMUM_ACTION_POINTS
                int remainingCarryWeight =
                        critterGetStat(critter, 12 /* STAT_CARRY_WEIGHT */)
                                - objectGetInventoryWeight(critter);
                if (remainingCarryWeight < 0) {
                    value -= -remainingCarryWeight / 40 + 1;
                }
                break;
            }
            case 9: // STAT_ARMOR_CLASS
                if (isInCombat()) {
                    if (combatWhoseTurn() != critter) {
                        int armorClassBonus =
                                critter.data.critter.combat.ap * unspentApBonus;

                        if (isDude(critter)) {
                            if (perkGetRank(critter,
                                    PerkDefs.Perk.PERK_HTH_EVADE.id()) != 0) {
                                boolean hasWeapon = false;

                                GameObject item2 = critterGetItem2(critter);
                                if (item2 != null) {
                                    if (itemGetType(item2) == 3 /* ITEM_TYPE_WEAPON */) {
                                        if (weaponGetAnimationCode(item2) != 0 /* None */) {
                                            hasWeapon = true;
                                        }
                                    }
                                }

                                if (!hasWeapon) {
                                    GameObject item1 = critterGetItem1(critter);
                                    if (item1 != null) {
                                        if (itemGetType(item1) == 3 /* ITEM_TYPE_WEAPON */) {
                                            if (weaponGetAnimationCode(item1) != 0 /* None */) {
                                                hasWeapon = true;
                                            }
                                        }
                                    }
                                }

                                if (!hasWeapon) {
                                    armorClassBonus += critter.data.critter.combat.ap
                                            * perkGetRank(critter,
                                                    PerkDefs.Perk.PERK_HTH_EVADE.id())
                                            * unspentApPerkBonus;
                                    value += Skill.skillGetValue(critter,
                                            SkillDefs.Skill.SKILL_UNARMED.id()) / 12;
                                }
                            }
                        }
                        value += armorClassBonus / 4;
                    }
                }
                break;
            case 33: // STAT_AGE
                value += GameTime.gameTimeGetTime() / GameTime.TICKS_PER_YEAR;
                break;
            default:
                break;
            }

            if (isDude(critter)) {
                switch (stat) {
                case 0: // STAT_STRENGTH
                    if (perkGetRank(critter, PerkDefs.Perk.PERK_GAIN_STRENGTH.id()) != 0) {
                        value++;
                    }
                    if (perkGetRank(critter, PerkDefs.Perk.PERK_ADRENALINE_RUSH.id()) != 0) {
                        if (critterGetStat(critter, 35 /* CURRENT_HP */)
                                < (critterGetStat(critter, 7 /* MAX_HP */) / 2)) {
                            value++;
                        }
                    }
                    break;
                case 1: // STAT_PERCEPTION
                    if (perkGetRank(critter, PerkDefs.Perk.PERK_GAIN_PERCEPTION.id()) != 0) {
                        value++;
                    }
                    break;
                case 2: // STAT_ENDURANCE
                    if (perkGetRank(critter, PerkDefs.Perk.PERK_GAIN_ENDURANCE.id()) != 0) {
                        value++;
                    }
                    break;
                case 3: { // STAT_CHARISMA
                    if (perkGetRank(critter, PerkDefs.Perk.PERK_GAIN_CHARISMA.id()) != 0) {
                        value++;
                    }

                    boolean hasMirrorShades = false;

                    GameObject item2 = critterGetItem2(critter);
                    if (item2 != null && ProtoId.ofGameObject(item2).pid() == 433) {
                        hasMirrorShades = true;
                    }

                    GameObject item1 = critterGetItem1(critter);
                    if (item1 != null && ProtoId.ofGameObject(item1).pid() == 433) {
                        hasMirrorShades = true;
                    }

                    if (hasMirrorShades) {
                        value++;
                    }
                    break;
                }
                case 4: // STAT_INTELLIGENCE
                    if (perkGetRank(critter, PerkDefs.Perk.PERK_GAIN_INTELLIGENCE.id()) != 0) {
                        value++;
                    }
                    break;
                case 5: // STAT_AGILITY
                    if (perkGetRank(critter, PerkDefs.Perk.PERK_GAIN_AGILITY.id()) != 0) {
                        value++;
                    }
                    break;
                case 6: // STAT_LUCK
                    if (perkGetRank(critter, PerkDefs.Perk.PERK_GAIN_LUCK.id()) != 0) {
                        value++;
                    }
                    break;
                case 7: // STAT_MAXIMUM_HIT_POINTS
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_ALCOHOL_RAISED_HIT_POINTS.id()) != 0) {
                        value += 2;
                    }
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_ALCOHOL_RAISED_HIT_POINTS_II.id()) != 0) {
                        value += 4;
                    }
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_ALCOHOL_LOWERED_HIT_POINTS.id()) != 0) {
                        value -= 2;
                    }
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_ALCOHOL_LOWERED_HIT_POINTS_II.id()) != 0) {
                        value -= 4;
                    }
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_AUTODOC_RAISED_HIT_POINTS.id()) != 0) {
                        value += 2;
                    }
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_AUTODOC_RAISED_HIT_POINTS_II.id()) != 0) {
                        value += 4;
                    }
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_AUTODOC_LOWERED_HIT_POINTS.id()) != 0) {
                        value -= 2;
                    }
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_AUTODOC_LOWERED_HIT_POINTS_II.id()) != 0) {
                        value -= 4;
                    }
                    break;
                case 24: // STAT_DAMAGE_RESISTANCE
                case 30: // STAT_DAMAGE_RESISTANCE_EXPLOSION
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_DERMAL_IMPACT_ARMOR.id()) != 0) {
                        value += 5;
                    } else if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_DERMAL_IMPACT_ASSAULT_ENHANCEMENT.id())
                            != 0) {
                        value += 10;
                    }
                    break;
                case 25: // STAT_DAMAGE_RESISTANCE_LASER
                case 26: // STAT_DAMAGE_RESISTANCE_FIRE
                case 27: // STAT_DAMAGE_RESISTANCE_PLASMA
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_PHOENIX_ARMOR_IMPLANTS.id()) != 0) {
                        value += 5;
                    } else if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_PHOENIX_ASSAULT_ENHANCEMENT.id()) != 0) {
                        value += 10;
                    }
                    break;
                case 31: // STAT_RADIATION_RESISTANCE
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_VAULT_CITY_INOCULATIONS.id()) != 0) {
                        value += perkGetVaultCityInoculationsRadBonus();
                    }
                    break;
                case 32: // STAT_POISON_RESISTANCE
                    if (perkGetRank(critter,
                            PerkDefs.Perk.PERK_VAULT_CITY_INOCULATIONS.id()) != 0) {
                        value += perkGetVaultCityInoculationsPoisonBonus();
                    }
                    break;
                default:
                    break;
                }
            }

            int minimum = statGetMinimum(critter, stat);
            if (value < minimum) {
                value = minimum;
            } else {
                int maximum = statGetMaximum(critter, stat);
                if (value > maximum) {
                    value = maximum;
                }
            }
            return value;
        }

        switch (stat) {
        case 35: // STAT_CURRENT_HIT_POINTS
            return critterGetHitPoints(critter);
        case 36: // STAT_CURRENT_POISON_LEVEL
            return critterGetPoison(critter);
        case 37: // STAT_CURRENT_RADIATION_LEVEL
            return critterGetRadiation(critter);
        default:
            return 0;
        }
    }

    /**
     * Port of {@code critterGetBaseStatWithTraitModifier} (0x4AF3E0): base
     * plus the dude-only trait modifier (dormant headless, still wired).
     */
    public static int critterGetBaseStatWithTraitModifier(GameObject critter, int stat) {
        int value = critterGetBaseStat(critter, stat);
        if (isDude(critter)) {
            value += TraitDefs.traitGetStatModifier(stat);
        }
        return value;
    }

    /**
     * Port of {@code critterGetBaseStat} (0x4AF408): saveable stats read the
     * proto {@code baseStats}; CURRENT_HP/POISON/RADIATION read the live
     * object fields (unvectored); anything else is 0.
     */
    public static int critterGetBaseStat(GameObject critter, int stat) {
        if (stat >= StatDefs.STAT_FIRST && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            Proto proto = protoFor(critter);
            if (proto == null) {
                return 0;
            }
            return proto.critter.data.baseStats[stat];
        }

        switch (stat) {
        case 35: // STAT_CURRENT_HIT_POINTS
            return critterGetHitPoints(critter);
        case 36: // STAT_CURRENT_POISON_LEVEL
            return critterGetPoison(critter);
        case 37: // STAT_CURRENT_RADIATION_LEVEL
            return critterGetRadiation(critter);
        default:
            return 0;
        }
    }

    /**
     * Port of {@code critterGetBonusStat} (0x4AF474): saveable stats read the
     * proto {@code bonusStats}, anything else is 0.
     */
    public static int critterGetBonusStat(GameObject critter, int stat) {
        if (stat >= StatDefs.STAT_FIRST && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            Proto proto = protoFor(critter);
            if (proto == null) {
                return 0;
            }
            return proto.critter.data.bonusStats[stat];
        }
        return 0;
    }

    /**
     * Port of {@code critterSetBaseStat} (0x4AF4BC): invalid stat -5,
     * derived-stat write -1, below-minimum -2, above-maximum -3, else the
     * proto store (+ derived recalc for STR..LUCK). CURRENT_* adjust via the
     * live-field helpers (unvectored).
     *
     * <p>Successful saveable writes mutate the shared {@link ProtoDb} cache
     * (like C); the golden path stops at -3 before any store.
     */
    public static int critterSetBaseStat(GameObject critter, int stat, int value) {
        if (!StatDefs.statIsValid(stat)) {
            return -5;
        }

        if (stat >= StatDefs.STAT_FIRST && stat < StatDefs.SAVEABLE_STAT_COUNT) {
            if (stat > StatDefs.Stat.STAT_LUCK.id()
                    && stat <= StatDefs.Stat.STAT_POISON_RESISTANCE.id()) {
                // Cannot change base value of derived stats.
                return -1;
            }

            if (isDude(critter)) {
                value -= TraitDefs.traitGetStatModifier(stat);
            }

            if (value < statGetMinimum(critter, stat)) {
                return -2;
            }

            if (value > statGetMaximum(critter, stat)) {
                return -3;
            }

            Proto proto = protoFor(critter);
            if (proto == null) {
                return -1;
            }
            proto.critter.data.baseStats[stat] = value;

            if (stat >= StatDefs.Stat.STAT_STRENGTH.id()
                    && stat <= StatDefs.Stat.STAT_LUCK.id()) {
                critterUpdateDerivedStats(critter);
            }

            return 0;
        }

        switch (stat) {
        case 35: // STAT_CURRENT_HIT_POINTS
            return critterAdjustHitPoints(critter, value - critterGetHitPoints(critter));
        case 36: // STAT_CURRENT_POISON_LEVEL
            return critterAdjustPoison(critter, value - critterGetPoison(critter));
        case 37: // STAT_CURRENT_RADIATION_LEVEL
            return critterAdjustRadiation(critter, value - critterGetRadiation(critter));
        default:
            // Should be unreachable
            return 0;
        }
    }

    /**
     * Port of {@code critterUpdateDerivedStats} (0x4AF6FC): recomputes the
     * derived proto {@code baseStats} from the (clamped) primary stats.
     * Mutates the shared proto cache like C; only reachable after a
     * committed STR..LUCK write, which the vectors never perform.
     */
    public static void critterUpdateDerivedStats(GameObject critter) {
        int strength = critterGetStat(critter, StatDefs.Stat.STAT_STRENGTH.id());
        int perception = critterGetStat(critter, StatDefs.Stat.STAT_PERCEPTION.id());
        int endurance = critterGetStat(critter, StatDefs.Stat.STAT_ENDURANCE.id());
        int intelligence = critterGetStat(critter, StatDefs.Stat.STAT_INTELLIGENCE.id());
        int agility = critterGetStat(critter, StatDefs.Stat.STAT_AGILITY.id());
        int luck = critterGetStat(critter, StatDefs.Stat.STAT_LUCK.id());

        Proto proto = protoFor(critter);
        if (proto == null) {
            return;
        }

        proto.critter.data.baseStats[StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id()] =
                critterGetBaseStatWithTraitModifier(critter,
                        StatDefs.Stat.STAT_STRENGTH.id())
                        + critterGetBaseStatWithTraitModifier(critter,
                                StatDefs.Stat.STAT_ENDURANCE.id())
                                * 2
                        + 15;
        proto.critter.data.baseStats[StatDefs.Stat.STAT_MAXIMUM_ACTION_POINTS.id()] =
                agility / 2 + 5;
        proto.critter.data.baseStats[StatDefs.Stat.STAT_ARMOR_CLASS.id()] = agility;
        proto.critter.data.baseStats[StatDefs.Stat.STAT_MELEE_DAMAGE.id()] =
                Math.max(strength - 5, 1);
        proto.critter.data.baseStats[StatDefs.Stat.STAT_CARRY_WEIGHT.id()] =
                25 * strength + 25;
        proto.critter.data.baseStats[StatDefs.Stat.STAT_SEQUENCE.id()] = 2 * perception;
        proto.critter.data.baseStats[StatDefs.Stat.STAT_HEALING_RATE.id()] =
                Math.max(endurance / 3, 1);
        proto.critter.data.baseStats[StatDefs.Stat.STAT_CRITICAL_CHANCE.id()] = luck;
        proto.critter.data.baseStats[StatDefs.Stat.STAT_BETTER_CRITICALS.id()] = 0;
        proto.critter.data.baseStats[StatDefs.Stat.STAT_RADIATION_RESISTANCE.id()] =
                2 * endurance;
        proto.critter.data.baseStats[StatDefs.Stat.STAT_POISON_RESISTANCE.id()] =
                5 * endurance;

        // Silence unused-variable warnings for the transcribed-but-unused
        // primaries (intelligence is read by C too but never stored).
        if (intelligence == Integer.MIN_VALUE) {
            throw new AssertionError("unreachable");
        }
    }

    /** Port of {@code critter_get_hits} (src/critter.cc): non-critter yields 0. */
    public static int critterGetHitPoints(GameObject critter) {
        if (critter == null
                || ProtoId.ofGameObject(critter).objectType()
                        != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }
        return critter.data.critter.hp;
    }

    /**
     * Port of {@code critter_adjust_hits} (src/critter.cc) minus the
     * {@code critterKill} tail (unported, deferred): hp is added and clamped
     * to the computed maximum. Non-critter yields 0.
     */
    public static int critterAdjustHitPoints(GameObject critter, int hp) {
        if (critter == null
                || ProtoId.ofGameObject(critter).objectType()
                        != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }

        int maximumHp = critterGetStat(critter, StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id());
        int newHp = critter.data.critter.hp + hp;

        critter.data.critter.hp = newHp;
        if (maximumHp < newHp) {
            critter.data.critter.hp = maximumHp;
        }
        // NOTE: C kills the critter when newHp <= 0 (and not already dead);
        // critterKill comes with the game phase.

        return 0;
    }

    /** Port of {@code critter_get_poison} (src/critter.cc): non-critter yields 0. */
    public static int critterGetPoison(GameObject critter) {
        if (critter == null
                || ProtoId.ofGameObject(critter).objectType()
                        != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }
        return critter.data.critter.poison;
    }

    /**
     * Port of {@code critter_adjust_poison} gate (src/critter.cc): only works
     * on the dude (non-dude returns -1 before any queue/message effects).
     * The dude tail (resist scaling, queue, messages) comes with the game
     * phase.
     */
    public static int critterAdjustPoison(GameObject critter, int amount) {
        if (!isDude(critter)) {
            return -1;
        }
        return 0;
    }

    /** Port of {@code critter_get_rads} (src/critter.cc): non-critter yields 0. */
    public static int critterGetRadiation(GameObject critter) {
        if (critter == null
                || ProtoId.ofGameObject(critter).objectType()
                        != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }
        return critter.data.critter.radiation;
    }

    /**
     * Port of {@code critter_adjust_rads} gate (src/critter.cc): only works
     * on the dude (non-dude returns -1 before hooks/resist/geiger effects).
     * The dude tail comes with the game phase.
     */
    public static int critterAdjustRadiation(GameObject critter, int amount) {
        if (!isDude(critter)) {
            return -1;
        }
        return 0;
    }

    /** Test seam: zero the bounds tables + reset the AP bonus seams. */
    static void resetForTests() {
        java.util.Arrays.fill(PC_STAT_MAXIMUMS, 0);
        java.util.Arrays.fill(PC_STAT_MINIMUMS, 0);
        java.util.Arrays.fill(NPC_STAT_MAXIMUMS, 0);
        java.util.Arrays.fill(NPC_STAT_MINIMUMS, 0);
        unspentApBonus = 4;
        unspentApPerkBonus = 4;
    }
}
