package org.fallout2ce;

import java.util.HashSet;
import java.util.Set;

import org.fallout2ce.Config.Cfg;
import org.fallout2ce.Config.ScopedConfig;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * Combat AI: port of {@code src/combat_ai.cc} slices. This covers the AI
 * packet table ({@code aiInit} parsing {@code data/ai.txt} + {@code aiGetPacketByNum}),
 * the packet reader helpers ({@code aiGetAreaAttackMode}, {@code aiGetRunAwayMode}),
 * the threat rating ({@code _combatai_rating}), and the weapon-decision
 * helpers used by the turn/AI slice: {@code _cai_get_min_hp},
 * {@code _caiHasWeapPrefType}, {@code aiGetWeaponRangeForHitMode},
 * {@code aiWeaponAttackInRange}, {@code _ai_can_use_weapon} and
 * {@code _combatai_check_retaliation}. The movement/attack/chem decision
 * tree itself stays with the turn/AI slice.
 *
 * <p>Vectors: {@code [aidec]} in {@code docker/refharness/golden/v164.txt}
 * (produced by {@code sectionAiDecisions} in
 * {@code docker/refharness/ref_main.cc}).
 */
public final class CombatAi {

    private CombatAi() {
    }

    public static final int AI_PACKET_CHEM_PRIMARY_DESIRE_COUNT = 3;

    // kChemUseStims*HpRatio (src/combat_ai.cc): the heal threshold used by
    // aiCanUseItem for each chem-use mode.
    private static final int K_CHEM_USE_STIMS_WHEN_HURT_LITTLE_HP_RATIO = 60;
    private static final int K_CHEM_USE_STIMS_WHEN_HURT_LOTS_HP_RATIO = 30;
    private static final int K_CHEM_USE_STIMS_HP_RATIO = 50;

    // burstDisabledCritters (src/combat_ai.cc): critters whose burst/continuous
    // secondary mode is suppressed.
    private static final Set<GameObject> burstDisabledCritters = new HashSet<>();

    /** {@code AiMessageRange} (src/combat_ai.h). */
    public static final class AiMessageRange {
        public int start;
        public int end;
    }

    /** {@code AiPacket} (src/combat_ai.h). */
    public static final class AiPacket {
        public String name;
        public int packetNum;
        public int maxDist;
        public int minToHit;
        public int minHp;
        public int aggression;
        public int hurtTooMuch;
        public int secondaryFreq;
        public int calledFreq;
        public int font;
        public int color;
        public int outlineColor;
        public int chance;
        public final AiMessageRange run = new AiMessageRange();
        public final AiMessageRange move = new AiMessageRange();
        public final AiMessageRange attack = new AiMessageRange();
        public final AiMessageRange miss = new AiMessageRange();
        public final AiMessageRange[] hit = new AiMessageRange[CombatDefs.HIT_LOCATION_SPECIFIC_COUNT];
        public int areaAttackMode;
        public int runAwayMode;
        public int bestWeapon;
        public int distance;
        public int attackWho;
        public int chemUse;
        public final int[] chemPrimaryDesire = new int[AI_PACKET_CHEM_PRIMARY_DESIRE_COUNT];
        public int disposition;
        public String bodyType;
        public String generalType;

        AiPacket() {
            for (int i = 0; i < hit.length; i++) {
                hit[i] = new AiMessageRange();
            }
        }
    }

    // gHurtTooMuchKeys + _rmatchHurtVals (src/combat_ai.cc).
    private static final String[] HURT_TOO_MUCH_KEYS = {
        "blind",
        "crippled",
        "crippled_legs",
        "crippled_arms",
    };
    private static final int[] RMATCH_HURT_VALS = {
        Dam.BLIND.bits(),
        Dam.CRIP_LEG_LEFT.bits() | Dam.CRIP_LEG_RIGHT.bits()
                | Dam.CRIP_ARM_LEFT.bits() | Dam.CRIP_ARM_RIGHT.bits(),
        Dam.CRIP_LEG_LEFT.bits() | Dam.CRIP_LEG_RIGHT.bits(),
        Dam.CRIP_ARM_LEFT.bits() | Dam.CRIP_ARM_RIGHT.bits(),
        Dam.NONE.bits(),
    };

    // _hp_run_away_value (src/combat_ai.cc 0x518138).
    private static final int[] HP_RUN_AWAY_VALUE = {
        0, 25, 40, 60, 75, 100,
    };

    // _weapPrefOrderings (src/combat_ai.cc 0x518158): rows are indexed by
    // best_weapon + 1, columns ordered like the value is scanned in C++.
    private static final int AT_NONE = Item.AttackType.ATTACK_TYPE_NONE.id();
    private static final int AT_UNARMED = Item.AttackType.ATTACK_TYPE_UNARMED.id();
    private static final int AT_MELEE = Item.AttackType.ATTACK_TYPE_MELEE.id();
    private static final int AT_THROW = Item.AttackType.ATTACK_TYPE_THROW.id();
    private static final int AT_RANGED = Item.AttackType.ATTACK_TYPE_RANGED.id();
    private static final int[][] WEAP_PREF_ORDERINGS = {
        { AT_RANGED, AT_THROW, AT_MELEE, AT_UNARMED, AT_NONE },
        { AT_RANGED, AT_THROW, AT_MELEE, AT_UNARMED, AT_NONE }, // BEST_WEAPON_NO_PREF
        { AT_MELEE, AT_NONE, AT_NONE, AT_NONE, AT_NONE }, // BEST_WEAPON_MELEE
        { AT_MELEE, AT_RANGED, AT_NONE, AT_NONE, AT_NONE }, // BEST_WEAPON_MELEE_OVER_RANGED
        { AT_RANGED, AT_MELEE, AT_NONE, AT_NONE, AT_NONE }, // BEST_WEAPON_RANGED_OVER_MELEE
        { AT_RANGED, AT_NONE, AT_NONE, AT_NONE, AT_NONE }, // BEST_WEAPON_RANGED
        { AT_UNARMED, AT_NONE, AT_NONE, AT_NONE, AT_NONE }, // BEST_WEAPON_UNARMED
        { AT_UNARMED, AT_THROW, AT_NONE, AT_NONE, AT_NONE }, // BEST_WEAPON_UNARMED_OVER_THROW
        { AT_NONE, AT_NONE, AT_NONE, AT_NONE, AT_NONE }, // BEST_WEAPON_RANDOM
    };

    private static AiPacket[] aiPackets = new AiPacket[0];
    private static boolean aiInitialized;

    /** {@code aiPacketInit} (src/combat_ai.cc 0x426FE0). */
    private static void aiPacketInit(AiPacket ai) {
        ai.name = null;
        ai.areaAttackMode = CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_INVALID.id();
        ai.runAwayMode = CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_INVALID.id();
        ai.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_INVALID.id();
        ai.distance = CombatAiDefs.DistanceMode.DISTANCE_INVALID.id();
        ai.attackWho = CombatAiDefs.AttackWho.ATTACK_WHO_INVALID.id();
        ai.chemUse = CombatAiDefs.ChemUse.CHEM_USE_INVALID.id();
        for (int i = 0; i < AI_PACKET_CHEM_PRIMARY_DESIRE_COUNT; i++) {
            ai.chemPrimaryDesire[i] = -1;
        }
        ai.disposition = CombatAiDefs.Disposition.DISPOSITION_INVALID.id();
    }

    /** {@code _parse_hurt_str} (src/combat_ai.cc). */
    private static int parseHurtStr(String value) {
        int result = Dam.NONE.bits();
        String s = value.toLowerCase();
        String[] parts = s.split(",");
        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            int index = -1;
            for (int i = CombatAiDefs.HURT_FIRST; i < CombatAiDefs.HURT_COUNT; i++) {
                if (token.equals(HURT_TOO_MUCH_KEYS[i])) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                Debug.print("Unrecognized flag: %s", token);
            } else {
                result |= RMATCH_HURT_VALS[index];
            }
        }
        return result;
    }

    /** {@code _cai_match_str_to_list} (src/combat_ai.cc). */
    private static int matchStrToList(String str, String[] list, int count) {
        for (int i = 0; i < count; i++) {
            if (str.equalsIgnoreCase(list[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@code aiInit} (src/combat_ai.cc 0x42703C): load the combat-AI message
     * list and parse {@code data/ai.txt} into the packet table.
     */
    public static int aiInit() {
        if (aiMessageListInit() == -1) {
            return -1;
        }

        try (ScopedConfig config = new ScopedConfig("data\\ai.txt", true)) {
            if (!config.isLoaded()) {
                return -1;
            }
            java.util.Set<String> sectionKeys = config.get().sections.keySet();
            int length = sectionKeys.size();
            aiPackets = new AiPacket[length];
            for (int i = 0; i < length; i++) {
                AiPacket ai = new AiPacket();
                aiPacketInit(ai);
                aiPackets[i] = ai;
            }

            int packetIndex = 0;
            for (String sectionKey : sectionKeys) {
                Cfg cfg = config.get();
                AiPacket ai = aiPackets[packetIndex++];
                ai.name = sectionKey;

                int[] iv = new int[1];
                if (!Config.configGetInt(cfg, sectionKey, "packet_num", iv)) return -1;
                ai.packetNum = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "max_dist", iv)) return -1;
                ai.maxDist = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "min_to_hit", iv)) return -1;
                ai.minToHit = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "min_hp", iv)) return -1;
                ai.minHp = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "aggression", iv)) return -1;
                ai.aggression = iv[0];

                String[] sv = new String[1];
                if (Config.configGetString(cfg, sectionKey, "hurt_too_much", sv)) {
                    ai.hurtTooMuch = parseHurtStr(sv[0]);
                }

                if (!Config.configGetInt(cfg, sectionKey, "secondary_freq", iv)) return -1;
                ai.secondaryFreq = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "called_freq", iv)) return -1;
                ai.calledFreq = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "font", iv)) return -1;
                ai.font = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "color", iv)) return -1;
                ai.color = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "outline_color", iv)) return -1;
                ai.outlineColor = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "chance", iv)) return -1;
                ai.chance = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "run_start", iv)) return -1;
                ai.run.start = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "run_end", iv)) return -1;
                ai.run.end = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "move_start", iv)) return -1;
                ai.move.start = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "move_end", iv)) return -1;
                ai.move.end = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "attack_start", iv)) return -1;
                ai.attack.start = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "attack_end", iv)) return -1;
                ai.attack.end = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "miss_start", iv)) return -1;
                ai.miss.start = iv[0];
                if (!Config.configGetInt(cfg, sectionKey, "miss_end", iv)) return -1;
                ai.miss.end = iv[0];

                String[] hitKeys = {
                    "hit_head_start", "hit_head_end",
                    "hit_left_arm_start", "hit_left_arm_end",
                    "hit_right_arm_start", "hit_right_arm_end",
                    "hit_torso_start", "hit_torso_end",
                    "hit_right_leg_start", "hit_right_leg_end",
                    "hit_left_leg_start", "hit_left_leg_end",
                    "hit_eyes_start", "hit_eyes_end",
                    "hit_groin_start", "hit_groin_end",
                };
                int hitIndex = 0;
                for (int k = 0; k < CombatDefs.HIT_LOCATION_SPECIFIC_COUNT; k++) {
                    if (!Config.configGetInt(cfg, sectionKey, hitKeys[hitIndex++], iv)) return -1;
                    ai.hit[k].start = iv[0];
                    if (!Config.configGetInt(cfg, sectionKey, hitKeys[hitIndex++], iv)) return -1;
                    ai.hit[k].end = iv[0];
                }
                ai.hit[CombatDefs.HitLocation.HIT_LOCATION_GROIN.id()].end++;

                if (Config.configGetString(cfg, sectionKey, "area_attack_mode", sv)) {
                    ai.areaAttackMode = matchStrToList(sv[0], PartyMembers.AREA_ATTACK_MODE_KEYS,
                            CombatAiDefs.AREA_ATTACK_MODE_COUNT);
                } else {
                    ai.areaAttackMode = CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_INVALID.id();
                }

                if (Config.configGetString(cfg, sectionKey, "run_away_mode", sv)) {
                    ai.runAwayMode = matchStrToList(sv[0], PartyMembers.RUN_AWAY_MODE_KEYS,
                            CombatAiDefs.RUN_AWAY_MODE_COUNT);
                    if (ai.runAwayMode >= CombatAiDefs.RUN_AWAY_MODE_FIRST) {
                        ai.runAwayMode--;
                    }
                }

                if (Config.configGetString(cfg, sectionKey, "best_weapon", sv)) {
                    ai.bestWeapon = matchStrToList(sv[0], PartyMembers.BEST_WEAPON_KEYS,
                            CombatAiDefs.BEST_WEAPON_COUNT);
                }

                if (Config.configGetString(cfg, sectionKey, "distance", sv)) {
                    ai.distance = matchStrToList(sv[0], PartyMembers.DISTANCE_MODE_KEYS,
                            CombatAiDefs.DISTANCE_COUNT);
                }

                if (Config.configGetString(cfg, sectionKey, "attack_who", sv)) {
                    ai.attackWho = matchStrToList(sv[0], PartyMembers.ATTACK_WHO_KEYS,
                            CombatAiDefs.ATTACK_WHO_COUNT);
                }

                if (Config.configGetString(cfg, sectionKey, "chem_use", sv)) {
                    ai.chemUse = matchStrToList(sv[0], PartyMembers.CHEM_USE_KEYS,
                            CombatAiDefs.CHEM_USE_COUNT);
                }

                Config.configGetIntList(cfg, sectionKey, "chem_primary_desire",
                        ai.chemPrimaryDesire, AI_PACKET_CHEM_PRIMARY_DESIRE_COUNT);

                if (Config.configGetString(cfg, sectionKey, "disposition", sv)) {
                    ai.disposition = matchStrToList(sv[0], PartyMembers.DISPOSITION_KEYS,
                            CombatAiDefs.DISPOSITION_COUNT);
                    if (ai.disposition >= CombatAiDefs.DISPOSITION_FIRST) {
                        ai.disposition--;
                    }
                }

                if (Config.configGetString(cfg, sectionKey, "body_type", sv)) {
                    ai.bodyType = sv[0];
                }
                if (Config.configGetString(cfg, sectionKey, "general_type", sv)) {
                    ai.generalType = sv[0];
                }
            }
        }

        aiInitialized = true;
        return 0;
    }

    /** {@code aiMessageListInit} (src/combat_ai.cc 0x42BB34). */
    private static int aiMessageListInit() {
        Message.MessageList gCombatAiMessageList = new Message.MessageList();
        if (!Message.messageListInit(gCombatAiMessageList)) {
            return -1;
        }
        String path = "game\\combatai.msg";
        if (!Message.messageListLoad(gCombatAiMessageList, path)) {
            return -1;
        }
        if (Settings.settings.preferences.language_filter) {
            Message.messageListFilterBadwords(gCombatAiMessageList);
        }
        Message.messageListRepositorySetStandardMessageList(
                Message.STANDARD_COMBAT_AI, gCombatAiMessageList);
        return 0;
    }

    /** {@code aiGetPacketByNum} (src/combat_ai.cc 0x42811C). */
    public static AiPacket aiGetPacketByNum(int aiPacketId) {
        for (int i = 0; i < aiPackets.length; i++) {
            if (aiPacketId == aiPackets[i].packetNum) {
                return aiPackets[i];
            }
        }
        Debug.print("Missing AI Packet");
        return aiPackets.length > 0 ? aiPackets[0] : null;
    }

    /** {@code aiGetPacket} (src/combat_ai.cc). */
    public static AiPacket aiGetPacket(GameObject obj) {
        return aiGetPacketByNum(obj.data.critter.combat.aiPacket);
    }

    /** {@code aiGetAreaAttackMode} (src/combat_ai.cc 0x428184). */
    public static int aiGetAreaAttackMode(GameObject obj) {
        return aiGetPacket(obj).areaAttackMode;
    }

    /** {@code aiGetRunAwayMode} (src/combat_ai.cc 0x428190). */
    public static int aiGetRunAwayMode(GameObject obj) {
        int runAwayMode = CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_INVALID.id();
        AiPacket ai = aiGetPacket(obj);
        if (ai.runAwayMode != CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_INVALID.id()) {
            return ai.runAwayMode;
        }
        int hpRatio = 100 * ai.minHp / Stat.critterGetStat(obj,
                StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id());
        for (int index = CombatAiDefs.RUN_AWAY_MODE_FIRST;
                index < CombatAiDefs.RUN_AWAY_MODE_LAST; index++) {
            if (hpRatio >= HP_RUN_AWAY_VALUE[index]) {
                runAwayMode = index;
            }
        }
        return runAwayMode;
    }

    /** {@code _combatai_rating} (src/combat_ai.cc 0x42B8BC). */
    public static int combataiRating(GameObject obj) {
        if (obj == null) {
            return 0;
        }
        if (FrmId.ofFid(obj.fid).objectType() != ObjectType.CRITTER) {
            return 0;
        }
        int results = obj.data.critter.combat.results.bits();
        if ((results & (Dam.DEAD.bits() | Dam.KNOCKED_OUT.bits())) != 0) {
            return 0;
        }

        int meleeDamage = Stat.critterGetStat(obj, StatDefs.Stat.STAT_MELEE_DAMAGE.id());

        GameObject item = Inventory.critterGetItem2(obj);
        if (item != null && Item.itemGetType(item) == ProtoTypes.ItemType.ITEM_TYPE_WEAPON) {
            int[] minDamage = new int[1];
            int[] maxDamage = new int[1];
            if (Item.weaponGetDamageMinMax(item, minDamage, maxDamage) != -1
                    && meleeDamage < maxDamage[0]) {
                meleeDamage = maxDamage[0];
            }
        }

        item = Inventory.critterGetItem1(obj);
        if (item != null && Item.itemGetType(item) == ProtoTypes.ItemType.ITEM_TYPE_WEAPON) {
            int[] minDamage = new int[1];
            int[] maxDamage = new int[1];
            if (Item.weaponGetDamageMinMax(item, minDamage, maxDamage) != -1
                    && meleeDamage < maxDamage[0]) {
                meleeDamage = maxDamage[0];
            }
        }

        return meleeDamage + Stat.critterGetStat(obj,
                StatDefs.Stat.STAT_ARMOR_CLASS.id());
    }

    /** {@code _cai_get_min_hp} (src/combat_ai.cc 0x42B100). */
    public static int caiGetMinHp(AiPacket ai) {
        if (ai == null) {
            return 0;
        }

        int runAwayMode = ai.runAwayMode;
        if (CombatAiDefs.runAwayModeIsValid(runAwayMode)
                && runAwayMode != CombatAiDefs.RUN_AWAY_MODE_LAST) {
            return HP_RUN_AWAY_VALUE[runAwayMode];
        } else if (runAwayMode == CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_INVALID.id()) {
            return ai.minHp;
        }

        return 0;
    }

    /** {@code _caiHasWeapPrefType} (src/combat_ai.cc 0x42938C). */
    public static boolean caiHasWeapPrefType(AiPacket ai, int attackType) {
        int bestWeapon = ai.bestWeapon + 1;

        for (int index = 0; index < Item.ATTACK_TYPE_COUNT; index++) {
            if (attackType == WEAP_PREF_ORDERINGS[bestWeapon][index]) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@code aiGetWeaponRangeForHitMode} (src/combat_ai.cc): unarmed reach,
     * the weapon's primary/secondary proto range, and the thrown-weapon
     * 3 x effective-strength clamp (with the Heave Ho! party bonus).
     */
    public static int aiGetWeaponRangeForHitMode(GameObject critter, GameObject weapon, int hitMode) {
        if (weapon == null) {
            if (Critter.critterFlagCheck(critter, ObjTypes.CritterFlags.LONG_LIMBS)) {
                return 2;
            }

            return 1;
        }

        ProtoTypes.Proto[] slot = new ProtoTypes.Proto[1];
        ProtoDb.protoGetProto(ProtoTypes.ProtoId.ofGameObject(weapon), slot);

        int range;
        if (hitMode == CombatDefs.HitMode.HIT_MODE_LEFT_WEAPON_PRIMARY.id()
                || hitMode == CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()) {
            range = slot[0].item.data.weapon.maxRange1;
        } else {
            range = slot[0].item.data.weapon.maxRange2;
        }

        if (Item.weaponGetAttackTypeForHitMode(weapon, hitMode)
                == Item.AttackType.ATTACK_TYPE_THROW.id()) {
            int effectiveStrength = Stat.critterGetStat(critter,
                    StatDefs.Stat.STAT_STRENGTH.id());
            if (critter == ObjectSystem.getDude() || PartyMembers.objectIsPartyMember(critter)) {
                if (effectiveStrength < StatDefs.PRIMARY_STAT_MAX) {
                    effectiveStrength += 2 * Perk.perkGetRank(critter,
                            PerkDefs.Perk.PERK_HEAVE_HO.id());
                    if (effectiveStrength > StatDefs.PRIMARY_STAT_MAX) {
                        effectiveStrength = StatDefs.PRIMARY_STAT_MAX;
                    }
                }
            }

            int maxRange = 3 * effectiveStrength;
            if (range >= maxRange) {
                range = maxRange;
            }
        }

        return range;
    }

    /** {@code aiWeaponAttackInRange} (src/combat_ai.cc). */
    public static boolean aiWeaponAttackInRange(GameObject critter, GameObject weapon,
            GameObject target) {
        int distance = ObjectSystem.objectGetDistanceBetween(critter, target);
        if (aiGetWeaponRangeForHitMode(critter, weapon,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()) >= distance) {
            return true;
        }

        if (Item.weaponGetAttackTypeForHitMode(weapon,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id())
                == Item.AttackType.ATTACK_TYPE_NONE.id()) {
            return false;
        }

        return aiGetWeaponRangeForHitMode(critter, weapon,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id()) >= distance;
    }

    /** {@code _ai_can_use_weapon} (src/combat_ai.cc 0x4298EC). */
    public static boolean aiCanUseWeapon(GameObject critter, GameObject weapon, int hitMode) {
        boolean result = Critter.critterCanUseWeapon(critter, weapon, hitMode);

        AiPacket ai = aiGetPacket(critter);
        if (result) {
            int skill = Item.weaponGetSkillForHitMode(weapon, hitMode);
            if (Skill.skillGetValue(critter, skill) < ai.minToHit) {
                result = false;
            }
        }

        if (result) {
            int attackType = Item.weaponGetAttackTypeForHitMode(weapon,
                    CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
            result = caiHasWeapPrefType(ai, attackType);
        }

        return scriptHooksCanUseWeapon(result, critter, weapon, hitMode);
    }

    // Seam for scriptHooks_CanUseWeapon (src/sfall_script_hooks.cc): with no
    // hooks registered the C++ call returns the input unchanged, so the
    // headless default is identity.
    private static boolean scriptHooksCanUseWeapon(boolean result, GameObject critter,
            GameObject weapon, int hitMode) {
        return result;
    }

    /** {@code _combatai_check_retaliation} (src/combat_ai.cc 0x42B9D4). */
    public static void combataiCheckRetaliation(GameObject a1, GameObject a2) {
        GameObject whoHitMe = a1.data.critter.combat.whoHitMe;
        if (whoHitMe != null) {
            int candidateRating = combataiRating(a2);
            int whoHitMeRating = combataiRating(whoHitMe);
            if (candidateRating > whoHitMeRating) {
                Critter.critterSetWhoHitMe(a1, a2);
            }
        } else {
            Critter.critterSetWhoHitMe(a1, a2);
        }
    }

    /** {@code aiGetChemUse} (src/combat_ai.cc 0x4280F0). */
    public static int aiGetChemUse(GameObject obj) {
        return aiGetPacket(obj).chemUse;
    }

    /** {@code aiHaveAmmo} (src/combat_ai.cc 0x4292D4). */
    public static boolean aiHaveAmmo(GameObject critter, GameObject weapon, GameObject[] ammoPtr) {
        if (ammoPtr != null) {
            ammoPtr[0] = null;
        }

        if (ProtoTypes.ProtoId.ofGameObject(weapon)
                .matchesItemType(ProtoTypes.ItemProtoTypeId.SolarScorcher)) {
            return Light.lightGetAmbientIntensity() > Light.LIGHT_INTENSITY_MAX * 0.95;
        }

        int[] inventoryItemIndex = new int[] { -1 };
        while (true) {
            GameObject ammo = Inventory.inventoryFindByType(critter,
                    ProtoTypes.ItemType.ITEM_TYPE_AMMO, inventoryItemIndex);
            if (ammo == null) {
                break;
            }

            if (Item.weaponCanBeReloadedWith(weapon, ammo)) {
                if (ammoPtr != null) {
                    ammoPtr[0] = ammo;
                }
                return true;
            }

            if (Item.weaponGetAnimationCode(weapon) != ArtDefs.WeaponAnimation.None.id()) {
                if (Item.weaponGetRange(critter,
                        CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()) < 3) {
                    Inventory.inventoryUnequip(critter, Inventory.Hand.HAND_RIGHT);
                }
            } else {
                Inventory.inventoryUnequip(critter, Inventory.Hand.HAND_RIGHT);
            }
        }

        return false;
    }

    /** {@code _ai_best_weapon} (src/combat_ai.cc 0x4293BC). */
    public static GameObject aiBestWeapon(GameObject attacker, GameObject weapon1,
            GameObject weapon2, GameObject defender) {
        if (attacker == null) {
            return null;
        }

        AiPacket ai = aiGetPacket(attacker);
        if (ai.bestWeapon == CombatAiDefs.BestWeapon.BEST_WEAPON_RANDOM.id()) {
            return GameRandom.randomBetween(1, 100) <= 50 ? weapon1 : weapon2;
        }

        boolean ignoreWeapon1 = false;
        int order2 = 999;
        int order1 = 999;
        int avgDamage1 = 0;

        CombatDefs.Attack attack = new CombatDefs.Attack();
        Combat.attackInit(attack, attacker, defender,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());

        int attackType1 = Item.AttackType.ATTACK_TYPE_INVALID.id();
        int distance = 0;
        int attackType2 = Item.AttackType.ATTACK_TYPE_INVALID.id();
        int avgDamage2 = 0;

        boolean ignoreWeapon2 = false;

        if (weapon1 != null) {
            attackType1 = Item.weaponGetAttackTypeForHitMode(weapon1,
                    CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
            int[] minDamage = new int[1];
            int[] maxDamage = new int[1];
            if (Item.weaponGetDamageMinMax(weapon1, minDamage, maxDamage) == -1) {
                return null;
            }

            avgDamage1 = (maxDamage[0] + minDamage[0]) / 2;
            if (Item.weaponGetDamageRadius(weapon1,
                    CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()) > 0
                    && defender != null) {
                attack.weapon = weapon1;
                Combat.computeExplosionOnExtras(attack, false,
                        Item.weaponIsGrenade(weapon1), true);
                avgDamage1 *= attack.extrasLength + 1;
            }

            if (Item.weaponGetPerk(weapon1) != PerkDefs.Perk.PERK_INVALID) {
                avgDamage1 *= 2;
            }

            if (defender != null) {
                if (Combat.combatSafetyInvalidateWeapon(attacker, weapon1,
                        CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                        defender, null)) {
                    ignoreWeapon1 = true;
                }
            }

            if (Item.itemIsHidden(weapon1)) {
                return weapon1;
            }
        } else {
            distance = ObjectSystem.objectGetDistanceBetween(attacker, defender);
            if (Item.weaponGetRange(attacker,
                    CombatDefs.HitMode.HIT_MODE_PUNCH.id()) >= distance) {
                attackType1 = Item.AttackType.ATTACK_TYPE_UNARMED.id();
            }
        }

        if (!ignoreWeapon1) {
            for (int index = 0; index < Item.ATTACK_TYPE_COUNT; index++) {
                if (WEAP_PREF_ORDERINGS[ai.bestWeapon + 1][index] == attackType1) {
                    order1 = index;
                    break;
                }
            }
        }

        if (weapon2 != null) {
            attackType2 = Item.weaponGetAttackTypeForHitMode(weapon2,
                    CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id());
            int[] minDamage = new int[1];
            int[] maxDamage = new int[1];
            if (Item.weaponGetDamageMinMax(weapon2, minDamage, maxDamage) == -1) {
                return null;
            }

            avgDamage2 = (maxDamage[0] + minDamage[0]) / 2;
            if (Item.weaponGetDamageRadius(weapon2,
                    CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()) > 0
                    && defender != null) {
                attack.weapon = weapon2;
                Combat.computeExplosionOnExtras(attack, false,
                        Item.weaponIsGrenade(weapon2), true);
                avgDamage2 *= attack.extrasLength + 1;
            }

            if (Item.weaponGetPerk(weapon2) != PerkDefs.Perk.PERK_INVALID) {
                avgDamage2 *= 2;
            }

            if (defender != null) {
                if (Combat.combatSafetyInvalidateWeapon(attacker, weapon2,
                        CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                        defender, null)) {
                    ignoreWeapon2 = true;
                }
            }

            if (Item.itemIsHidden(weapon2)) {
                return weapon2;
            }
        } else {
            if (distance == 0) {
                distance = ObjectSystem.objectGetDistanceBetween(attacker, weapon1);
            }

            if (Item.weaponGetRange(attacker,
                    CombatDefs.HitMode.HIT_MODE_PUNCH.id()) >= distance) {
                attackType2 = Item.AttackType.ATTACK_TYPE_UNARMED.id();
            }
        }

        if (!ignoreWeapon2) {
            for (int index = 0; index < Item.ATTACK_TYPE_COUNT; index++) {
                if (WEAP_PREF_ORDERINGS[ai.bestWeapon + 1][index] == attackType2) {
                    order2 = index;
                    break;
                }
            }
        }

        if (order1 == order2) {
            if (order1 == 999) {
                return null;
            }

            if (Math.abs(avgDamage2 - avgDamage1) <= 5) {
                return Item.itemGetCost(weapon2) > Item.itemGetCost(weapon1)
                        ? weapon2 : weapon1;
            }

            return avgDamage2 > avgDamage1 ? weapon2 : weapon1;
        }

        if (ProtoTypes.ProtoId.ofGameObject(weapon1)
                .matchesItemType(ProtoTypes.ItemProtoTypeId.Flare) && weapon2 != null) {
            return weapon2;
        }

        if (ProtoTypes.ProtoId.ofGameObject(weapon2)
                .matchesItemType(ProtoTypes.ItemProtoTypeId.Flare) && weapon1 != null) {
            return weapon1;
        }

        if ((ai.bestWeapon == -1
                || ai.bestWeapon >= CombatAiDefs.BestWeapon.BEST_WEAPON_UNARMED_OVER_THROW.id())
                && Math.abs(avgDamage2 - avgDamage1) > 5) {
            return avgDamage2 > avgDamage1 ? weapon2 : weapon1;
        }

        return order1 > order2 ? weapon2 : weapon1;
    }

    /** {@code _ai_search_inven_weap} (src/combat_ai.cc 0x4299A0). */
    public static GameObject aiSearchInvenWeap(GameObject critter, boolean checkRequiredActionPoints,
            GameObject defender) {
        int bodyType = Critter.critterGetBodyType(critter).id();
        if (bodyType != ProtoTypes.BodyType.BODY_TYPE_BIPED.id()
                && bodyType != ProtoTypes.BodyType.BODY_TYPE_ROBOTIC.id()
                && !ProtoTypes.ProtoId.ofGameObject(critter)
                        .matchesCritterType(ProtoTypes.CritterProtoTypeId.Goris)) {
            return null;
        }

        int[] token = new int[] { -1 };
        GameObject bestWeapon = null;
        GameObject rightHandWeapon = Inventory.critterGetItem2(critter);
        while (true) {
            GameObject weapon = Inventory.inventoryFindByType(critter,
                    ProtoTypes.ItemType.ITEM_TYPE_WEAPON, token);
            if (weapon == null) {
                break;
            }

            if (weapon == rightHandWeapon) {
                continue;
            }

            if (checkRequiredActionPoints) {
                if (Item.weaponGetPrimaryActionPointCost(weapon)
                        > critter.data.critter.combat.ap) {
                    continue;
                }
            }

            if (!aiCanUseWeapon(critter, weapon,
                    CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id())) {
                continue;
            }

            if (Item.weaponGetAttackTypeForHitMode(weapon,
                    CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id())
                    == Item.AttackType.ATTACK_TYPE_RANGED.id()) {
                if (!Item.weaponHasAmmoForAttack(weapon,
                        CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id())) {
                    if (!aiHaveAmmo(critter, weapon, null)) {
                        continue;
                    }
                }
            }

            bestWeapon = aiBestWeapon(critter, bestWeapon, weapon, defender);
        }

        return bestWeapon;
    }

    /** {@code _ai_search_inven_armor} (src/combat_ai.cc 0x429A6C). */
    public static GameObject aiSearchInvenArmor(GameObject critter) {
        if (!PartyMembers.objectIsPartyMember(critter)) {
            return null;
        }

        if (Critter.critterGetBodyType(critter) != ProtoTypes.BodyType.BODY_TYPE_BIPED) {
            return null;
        }

        int armorScore = 0;
        GameObject armor = Inventory.critterGetArmor(critter);
        if (armor != null) {
            armorScore = Item.armorGetArmorClass(armor);
            for (int damageType = ProtoTypes.DAMAGE_TYPE_FIRST;
                    damageType < ProtoTypes.DAMAGE_TYPE_COUNT; damageType++) {
                armorScore += Item.armorGetDamageResistance(armor, damageType);
                armorScore += Item.armorGetDamageThreshold(armor, damageType);
            }
        }

        GameObject bestArmor = null;

        int[] inventoryItemIndex = new int[] { -1 };
        while (true) {
            GameObject candidate = Inventory.inventoryFindByType(critter,
                    ProtoTypes.ItemType.ITEM_TYPE_ARMOR, inventoryItemIndex);
            if (candidate == null) {
                break;
            }

            if (armor != candidate) {
                int candidateScore = Item.armorGetArmorClass(candidate);
                for (int damageType = ProtoTypes.DAMAGE_TYPE_FIRST;
                        damageType < ProtoTypes.DAMAGE_TYPE_COUNT; damageType++) {
                    candidateScore += Item.armorGetDamageResistance(candidate, damageType);
                    candidateScore += Item.armorGetDamageThreshold(candidate, damageType);
                }

                if (candidateScore > armorScore) {
                    armorScore = candidateScore;
                    bestArmor = candidate;
                }
            }
        }

        return bestArmor;
    }

    /** {@code aiCanUseItem} (src/combat_ai.cc 0x429B44). */
    public static boolean aiCanUseItem(GameObject critter, GameObject item) {
        if (critter == null || item == null) {
            return false;
        }

        AiPacket ai = aiGetPacketByNum(critter.data.critter.combat.aiPacket);
        if (ai == null) {
            return false;
        }

        for (int index = 0; index < AI_PACKET_CHEM_PRIMARY_DESIRE_COUNT; index++) {
            if (item.pid == ai.chemPrimaryDesire[index]) {
                return true;
            }
        }

        if (Critter.critterGetBodyType(critter) != ProtoTypes.BodyType.BODY_TYPE_BIPED) {
            return false;
        }

        int killType = Critter.critterGetKillType(critter).id();
        if (killType != ProtoTypes.KillType.KILL_TYPE_MAN.id()
                && killType != ProtoTypes.KillType.KILL_TYPE_WOMAN.id()
                && killType != ProtoTypes.KillType.KILL_TYPE_SUPER_MUTANT.id()
                && killType != ProtoTypes.KillType.KILL_TYPE_GHOUL.id()
                && killType != ProtoTypes.KillType.KILL_TYPE_CHILD.id()) {
            return false;
        }

        if (Stat.critterGetStat(critter, StatDefs.Stat.STAT_INTELLIGENCE.id()) < 3) {
            return false;
        }

        if (!Item.itemIsHealing(ProtoTypes.ProtoId.ofGameObject(item))) {
            return false;
        }

        int hpRatio = K_CHEM_USE_STIMS_HP_RATIO;
        int chemUse = aiGetChemUse(critter);
        if (chemUse == CombatAiDefs.ChemUse.CHEM_USE_CLEAN.id()) {
            hpRatio = 0;
        } else if (chemUse == CombatAiDefs.ChemUse.CHEM_USE_STIMS_WHEN_HURT_LITTLE.id()) {
            hpRatio = K_CHEM_USE_STIMS_WHEN_HURT_LITTLE_HP_RATIO;
        } else if (chemUse == CombatAiDefs.ChemUse.CHEM_USE_STIMS_WHEN_HURT_LOTS.id()) {
            hpRatio = K_CHEM_USE_STIMS_WHEN_HURT_LOTS_HP_RATIO;
        }

        int currentHp = Stat.critterGetStat(critter, StatDefs.Stat.STAT_CURRENT_HIT_POINTS.id());
        int stimsHp = Stat.critterGetStat(critter, StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id())
                * hpRatio / 100;
        return currentHp <= stimsHp;
    }

    /** {@code _ai_pick_hit_mode} (src/combat_ai.cc 0x429DB4). */
    public static int aiPickHitMode(GameObject attacker, GameObject weapon, GameObject defender) {
        if (weapon == null) {
            return CombatDefs.HitMode.HIT_MODE_PUNCH.id();
        }

        if (Item.itemGetType(weapon) != ProtoTypes.ItemType.ITEM_TYPE_WEAPON) {
            return CombatDefs.HitMode.HIT_MODE_PUNCH.id();
        }

        int attackType = Item.weaponGetAttackTypeForHitMode(weapon,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id());
        int intelligence = Stat.critterGetStat(attacker, StatDefs.Stat.STAT_INTELLIGENCE.id());
        if (attackType == Item.AttackType.ATTACK_TYPE_NONE.id()
                || !aiCanUseWeapon(attacker, weapon,
                        CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id())) {
            return CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id();
        }

        if (aiIsBurstDisabled(attacker)) {
            int secondaryAnimation = Item.weaponGetAnimationForHitMode(weapon,
                    CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id());
            if (secondaryAnimation == AnimationDefs.AnimationType.ANIM_FIRE_BURST.id()
                    || secondaryAnimation == AnimationDefs.AnimationType.ANIM_FIRE_CONTINUOUS.id()) {
                return CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id();
            }
        }

        boolean useSecondaryMode = false;

        AiPacket ai = aiGetPacket(attacker);
        if (ai == null) {
            return CombatDefs.HitMode.HIT_MODE_PUNCH.id();
        }

        if (ai.areaAttackMode != -1) {
            int mode = ai.areaAttackMode;
            if (mode == CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_ALWAYS.id()) {
                useSecondaryMode = true;
            } else if (mode == CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_SOMETIMES.id()) {
                if (GameRandom.randomBetween(1, ai.secondaryFreq) == 1) {
                    useSecondaryMode = true;
                }
            } else if (mode == CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_BE_SURE.id()) {
                if (Combat.determineToHit(attacker, defender,
                        CombatDefs.HitLocation.HIT_LOCATION_TORSO.id(),
                        CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id()) >= 85
                        && !Combat.combatSafetyInvalidateWeapon(attacker, weapon,
                                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id(),
                                defender, null)) {
                    useSecondaryMode = true;
                }
            } else if (mode == CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_BE_CAREFUL.id()) {
                if (Combat.determineToHit(attacker, defender,
                        CombatDefs.HitLocation.HIT_LOCATION_TORSO.id(),
                        CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id()) >= 50
                        && !Combat.combatSafetyInvalidateWeapon(attacker, weapon,
                                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id(),
                                defender, null)) {
                    useSecondaryMode = true;
                }
            } else if (mode == CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_BE_ABSOLUTELY_SURE.id()) {
                if (Combat.determineToHit(attacker, defender,
                        CombatDefs.HitLocation.HIT_LOCATION_TORSO.id(),
                        CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id()) >= 95
                        && !Combat.combatSafetyInvalidateWeapon(attacker, weapon,
                                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id(),
                                defender, null)) {
                    useSecondaryMode = true;
                }
            }
        } else {
            if (intelligence < 6 || ObjectSystem.objectGetDistanceBetween(attacker, defender) < 10) {
                if (GameRandom.randomBetween(1, ai.secondaryFreq) == 1) {
                    useSecondaryMode = true;
                }
            }
        }

        if (useSecondaryMode) {
            if (!caiHasWeapPrefType(ai, attackType)) {
                useSecondaryMode = false;
            }
        }

        if (useSecondaryMode) {
            if (ObjectSystem.objectGetDistanceBetween(attacker, defender)
                    > Item.weaponGetRange(attacker,
                            CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id())) {
                useSecondaryMode = false;
            }
        }

        if (useSecondaryMode) {
            if (attacker.data.critter.combat.ap
                    < Item.weaponGetActionPointCost(attacker,
                            CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id(), false)) {
                useSecondaryMode = false;
            }
        }

        if (useSecondaryMode) {
            if (attackType != Item.AttackType.ATTACK_TYPE_THROW.id()
                    || aiSearchInvenWeap(attacker, false, defender) != null
                    || Stat.statRoll(attacker, StatDefs.Stat.STAT_INTELLIGENCE.id(),
                            0, null).ordinal() <= 1) {
                return CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id();
            }
        }

        return CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id();
    }

    /** {@code aiIsBurstDisabled} (src/combat_ai.cc). */
    public static boolean aiIsBurstDisabled(GameObject critter) {
        if (critter == null) {
            return false;
        }

        return burstDisabledCritters.contains(critter);
    }

    /** {@code aiSetBurstDisabled} (src/combat_ai.cc). */
    public static void aiSetBurstDisabled(GameObject critter, boolean disable) {
        if (critter == null
                || FrmId.ofFid(critter.fid).objectType() != ObjectType.CRITTER
                || critter == ObjectSystem.getDude()) {
            return;
        }

        if (disable) {
            burstDisabledCritters.add(critter);
        } else {
            burstDisabledCritters.remove(critter);
        }
    }

    /** {@code aiRemoveBurstDisabled} (src/combat_ai.cc). */
    public static void aiRemoveBurstDisabled(GameObject critter) {
        if (critter == null) {
            return;
        }

        burstDisabledCritters.remove(critter);
    }

    // ===== target selection (src/combat_ai.cc) =====

    // _curr_crit_list/_curr_crit_num (src/combat_ai.cc:263-268): the combat
    // roster the target-selection helpers scan.
    private static GameObject[] currCritList = new GameObject[0];

    // _combat_obj (src/combat_ai.cc:270): origin scratch for the distance
    // comparator, set by _ai_sort_list_distance.
    private static GameObject combatObj;

    /** Port of {@code _combat_ai_begin} (src/combat_ai.cc:3133). */
    public static void combatAiBegin(GameObject[] list, int count) {
        if (count != 0) {
            currCritList = new GameObject[count];
            System.arraycopy(list, 0, currCritList, 0, count);
        } else {
            currCritList = new GameObject[0];
        }
    }

    /** Port of {@code _combat_ai_over} (src/combat_ai.cc:3148). */
    public static void combatAiOver() {
        currCritList = new GameObject[0];
    }

    // 0x428B1C _compare_nearer: distance to _combat_obj, nulls first.
    private static int compareNearer(GameObject object1, GameObject object2) {
        if (object1 == null && object2 == null) {
            return 0;
        }
        if (object1 != null && object2 == null) {
            return -1;
        }
        if (object1 == null && object2 != null) {
            return 1;
        }
        int distance1 = ObjectSystem.objectGetDistanceBetween(object1, combatObj);
        int distance2 = ObjectSystem.objectGetDistanceBetween(object2, combatObj);
        return Integer.compare(distance1, distance2);
    }

    // 0x428B8C _compare_strength: rating ascending, nulls first.
    private static int compareStrength(GameObject object1, GameObject object2) {
        if (object1 == null && object2 == null) {
            return 0;
        }
        if (object1 != null && object2 == null) {
            return -1;
        }
        if (object1 == null && object2 != null) {
            return 1;
        }
        int rating1 = combataiRating(object1);
        int rating2 = combataiRating(object2);
        return Integer.compare(rating1, rating2);
    }

    // 0x428BE4 _compare_weakness: rating descending, nulls first.
    private static int compareWeakness(GameObject object1, GameObject object2) {
        if (object1 == null && object2 == null) {
            return 0;
        }
        if (object1 != null && object2 == null) {
            return -1;
        }
        if (object1 == null && object2 != null) {
            return 1;
        }
        int rating1 = combataiRating(object1);
        int rating2 = combataiRating(object2);
        return Integer.compare(rating2, rating1);
    }

    private interface CritterComparator {
        int compare(GameObject object1, GameObject object2);
    }

    // NOTE: C qsort is unstable; these sorts keep equal elements in place
    // (insertion sort). The [aitarget] vectors use distinct keys, so the
    // orders match; in-game ties may order differently than glibc.
    private static void sortList(GameObject[] critterList, int length, CritterComparator cmp) {
        for (int index = 1; index < length; index++) {
            GameObject key = critterList[index];
            int position = index - 1;
            while (position >= 0 && cmp.compare(critterList[position], key) > 0) {
                critterList[position + 1] = critterList[position];
                position--;
            }
            critterList[position + 1] = key;
        }
    }

    /** {@code _ai_sort_list_distance} (0x428B74). */
    public static void aiSortListDistance(GameObject[] critterList, int length, GameObject origin) {
        combatObj = origin;
        sortList(critterList, length, CombatAi::compareNearer);
    }

    /** {@code _ai_sort_list_strength} (0x428BD0). */
    public static void aiSortListStrength(GameObject[] critterList, int length) {
        sortList(critterList, length, CombatAi::compareStrength);
    }

    /** {@code _ai_sort_list_weakness} (0x428C28). */
    public static void aiSortListWeakness(GameObject[] critterList, int length) {
        sortList(critterList, length, CombatAi::compareWeakness);
    }

    /** Port of {@code _ai_find_nearest_team} (0x428C3C). */
    public static GameObject aiFindNearestTeam(GameObject a1, GameObject a2, int flags) {
        if (a2 == null) {
            return null;
        }

        int team = a2.data.critter.combat.team;

        if (currCritList.length == 0) {
            return null;
        }

        aiSortListDistance(currCritList, currCritList.length, a1);

        for (int index = 0; index < currCritList.length; index++) {
            GameObject obj = currCritList[index];
            if (a1 != obj
                && (obj.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) == 0
                && (((flags & 0x02) != 0 && team != obj.data.critter.combat.team)
                    || ((flags & 0x01) != 0 && team == obj.data.critter.combat.team))) {
                return obj;
            }
        }

        return null;
    }

    /** Port of {@code _ai_find_nearest_team_in_combat} (0x428CF4). */
    public static GameObject aiFindNearestTeamInCombat(GameObject a1, GameObject a2, int flags) {
        if (a2 == null) {
            return null;
        }

        int team = a2.data.critter.combat.team;

        if (currCritList.length == 0) {
            return null;
        }

        aiSortListDistance(currCritList, currCritList.length, a1);

        for (int index = 0; index < currCritList.length; index++) {
            GameObject obj = currCritList[index];
            if (obj != a1
                && (obj.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) == 0
                && (((flags & 0x02) != 0 && team != obj.data.critter.combat.team)
                    || ((flags & 0x01) != 0 && team == obj.data.critter.combat.team))) {
                if (obj.data.critter.combat.whoHitMe != null) {
                    return obj;
                }
            }
        }

        return null;
    }

    /** Port of {@code aiFindAttackers} (0x428DB0): fills slots 1-3. */
    public static void aiFindAttackers(GameObject critter, GameObject[] targets) {
        targets[1] = null;
        targets[2] = null;
        targets[3] = null;

        if (currCritList.length == 0) {
            return;
        }

        aiSortListDistance(currCritList, currCritList.length, critter);

        int foundTargetCount = 0;
        int team = critter.data.critter.combat.team;

        for (int index = 0; foundTargetCount < 3 && index < currCritList.length; index++) {
            GameObject candidate = currCritList[index];
            if (candidate != critter) {
                if (targets[1] == null) {
                    if ((candidate.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) == 0
                        && candidate.data.critter.combat.whoHitMe == critter
                        && !targetExists(targets, candidate)) {
                        foundTargetCount++;
                        targets[1] = candidate;
                        continue;
                    }
                }

                if (targets[2] == null) {
                    if (team == candidate.data.critter.combat.team) {
                        GameObject whoHitCandidate = candidate.data.critter.combat.whoHitMe;
                        if (whoHitCandidate != null
                            && whoHitCandidate != critter
                            && team != whoHitCandidate.data.critter.combat.team
                            && (whoHitCandidate.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) == 0
                            && !targetExists(targets, whoHitCandidate)) {
                            foundTargetCount++;
                            targets[2] = whoHitCandidate;
                            continue;
                        }
                    }
                }

                if (targets[3] == null) {
                    if (candidate.data.critter.combat.team != team
                        && (candidate.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) == 0) {
                        GameObject whoHitCandidate = candidate.data.critter.combat.whoHitMe;
                        if (whoHitCandidate != null
                            && whoHitCandidate.data.critter.combat.team == team
                            && !targetExists(targets, candidate)) {
                            foundTargetCount++;
                            targets[3] = candidate;
                            continue;
                        }
                    }
                }
            }
        }
    }

    private static boolean targetExists(GameObject[] targets, GameObject target) {
        for (GameObject existing : targets) {
            if (existing == target) {
                return true;
            }
        }
        return false;
    }

    /** Port of {@code _ai_danger_source} (0x428F4C). */
    public static GameObject aiDangerSource(GameObject a1) {
        if (a1 == null) {
            return null;
        }

        boolean ignoreFleeingCritters = false;
        int attackWho;
        GameObject[] targets = new GameObject[4];
        boolean preservedPartyTarget = false;

        if (PartyMembers.objectIsPartyMember(a1)) {
            int disposition = aiGetDisposition(a1);
            int distanceMode = aiGetDistance(a1);

            int dispositionPlusOne = disposition + 1;
            if (dispositionPlusOne == CombatAiDefs.Disposition.DISPOSITION_CUSTOM.id()
                || dispositionPlusOne == CombatAiDefs.Disposition.DISPOSITION_COWARD.id()
                || dispositionPlusOne == CombatAiDefs.Disposition.DISPOSITION_DEFENSIVE.id()
                || dispositionPlusOne == CombatAiDefs.Disposition.DISPOSITION_AGGRESSIVE.id()) {
                ignoreFleeingCritters = true;
            } else if (dispositionPlusOne == CombatAiDefs.Disposition.DISPOSITION_NONE.id()
                || dispositionPlusOne == CombatAiDefs.Disposition.DISPOSITION_BERKSERK.id()) {
                ignoreFleeingCritters = false;
            }

            if (ignoreFleeingCritters
                && distanceMode == CombatAiDefs.DistanceMode.DISTANCE_CHARGE.id()) {
                ignoreFleeingCritters = false;
            }

            attackWho = aiGetAttackWho(a1);
            if (attackWho == CombatAiDefs.AttackWho.ATTACK_WHO_WHOMEVER_ATTACKING_ME.id()) {
                // CE: Slightly improve "Whomever is attacking me" targeting:
                // first attempt to continue attacking the previous target,
                // otherwise pick the nearest critter attacking the dude.
                GameObject candidate = Combat.aiInfoGetLastTarget(a1);
                if (candidate != null) {
                    GameObject critter = candidate;
                    if ((critter.data.critter.combat.results.bits()
                            & (ObjTypes.Dam.DEAD.bits() | ObjTypes.Dam.KNOCKED_OUT.bits())) != 0
                        || a1.data.critter.combat.team == critter.data.critter.combat.team
                        || Combat.aiInfoGetLastTarget(critter) != ObjectSystem.getDude()) {
                        candidate = null;
                    }

                    // Do not chase fleeing critter.
                    if (ignoreFleeingCritters && Critter.critterIsFleeing(critter)) {
                        candidate = null;
                    }
                }

                if (candidate == null) {
                    aiSortListDistance(currCritList, currCritList.length, a1);

                    for (int index = 0; index < currCritList.length; index++) {
                        GameObject critter = currCritList[index];
                        if (critter != a1) {
                            boolean deadOrKo = (critter.data.critter.combat.results.bits()
                                & (ObjTypes.Dam.DEAD.bits() | ObjTypes.Dam.KNOCKED_OUT.bits())) != 0;
                            boolean sameTeam = a1.data.critter.combat.team
                                == critter.data.critter.combat.team;
                            boolean attacksDude =
                                Combat.aiInfoGetLastTarget(critter) == ObjectSystem.getDude();
                            if (deadOrKo
                                || sameTeam
                                || !attacksDude) {
                                continue;
                            }

                            int path = Animation.pathfinderFindPath(a1, a1.tile, critter.tile,
                                null, 0, ObjectSystem::objBlockingAt);

                            // SFALL: No ammo is a recoverable result because
                            // AI can reload after selecting the target.
                            CombatDefs.CombatBadShot badShot = Combat.combatCheckBadShot(a1,
                                critter, CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                                false);
                            if (path == 0
                                && badShot != CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_OK
                                && badShot != CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_NO_AMMO) {
                                continue;
                            }

                            // Do not chase fleeing critter.
                            if (ignoreFleeingCritters && Critter.critterIsFleeing(critter)) {
                                continue;
                            }

                            candidate = critter;
                            break;
                        }
                    }
                }

                if (candidate != null) {
                    return candidate;
                }
            } else if (attackWho == CombatAiDefs.AttackWho.ATTACK_WHO_STRONGEST.id()
                || attackWho == CombatAiDefs.AttackWho.ATTACK_WHO_WEAKEST.id()
                || attackWho == CombatAiDefs.AttackWho.ATTACK_WHO_CLOSEST.id()) {
                targets[0] = a1.data.critter.combat.whoHitMe;
                if (targets[0] == a1
                    || (targets[0] != null
                        && (targets[0].data.critter.combat.results.bits()
                            & ObjTypes.Dam.DEAD.bits()) != 0)) {
                    targets[0] = null;
                }
                preservedPartyTarget = true;
                a1.data.critter.combat.whoHitMe = null;
            }
        } else {
            attackWho = CombatAiDefs.AttackWho.ATTACK_WHO_INVALID.id();
        }

        GameObject whoHitMe = a1.data.critter.combat.whoHitMe;
        if (!preservedPartyTarget) {
            if (whoHitMe == null || a1 == whoHitMe) {
                targets[0] = null;
            } else {
                if ((whoHitMe.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) == 0) {
                    if (attackWho == CombatAiDefs.AttackWho.ATTACK_WHO_WHOMEVER.id()
                        || attackWho == CombatAiDefs.AttackWho.ATTACK_WHO_INVALID.id()) {
                        return whoHitMe;
                    }
                } else {
                    if (whoHitMe.data.critter.combat.team != a1.data.critter.combat.team) {
                        targets[0] = aiFindNearestTeam(a1, whoHitMe, 1);
                    } else {
                        targets[0] = null;
                    }
                }
            }
        }

        aiFindAttackers(a1, targets);

        if (ignoreFleeingCritters) {
            for (int index = 0; index < 4; index++) {
                if (targets[index] != null && Critter.critterIsFleeing(targets[index])) {
                    targets[index] = null;
                }
            }
        }

        if (attackWho == CombatAiDefs.AttackWho.ATTACK_WHO_STRONGEST.id()) {
            aiSortListStrength(targets, 4);
        } else if (attackWho == CombatAiDefs.AttackWho.ATTACK_WHO_WEAKEST.id()) {
            aiSortListWeakness(targets, 4);
        } else {
            aiSortListDistance(targets, 4, a1);
        }

        for (int index = 0; index < 4; index++) {
            GameObject candidate = targets[index];
            if (candidate != null
                && isWithinPerceptionDetailed(a1, candidate,
                    CombatAiDefs.PerceptionType.PERCEPTION_AI_TARGET.id())
                    != CombatAiDefs.PerceptionResult.PERCEPTION_OUT_OF_RANGE.id()) {
                CombatDefs.CombatBadShot badShot = Combat.combatCheckBadShot(a1, candidate,
                    CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), false);
                int path = Animation.pathfinderFindPath(a1, a1.tile, candidate.tile, null, 0,
                    ObjectSystem::objBlockingAt);
                if (path != 0
                    || badShot == CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_OK
                    || badShot == CombatDefs.CombatBadShot.COMBAT_BAD_SHOT_NO_AMMO) {
                    return candidate;
                }
                Debug.print("\nai_danger_source: I couldn't get at my target!"
                    + "  Picking alternate!");
            }
        }

        return null;
    }

    // ===== perception (src/combat_ai.cc:3701+) =====

    /** Port of {@code adjustPerceptionDistanceForDudeSneak} (0x42B9B0). */
    static int adjustPerceptionDistanceForDudeSneak(int maxDistance, GameObject target) {
        if (target != ObjectSystem.getDude()) {
            return maxDistance;
        }

        if (Critter.dudeIsSneaking()) {
            int sneak = Skill.skillGetValue(ObjectSystem.getDude(),
                SkillDefs.Skill.SKILL_SNEAK.id());
            maxDistance /= 4;
            if (sneak > 120) {
                maxDistance -= 1;
            }
        } else if (Critter.dudeHasState(Critter.DUDE_STATE_SNEAKING)) {
            maxDistance = maxDistance * 2 / 3;
        }

        return maxDistance;
    }

    /** {@code scriptHooks_WithinPerception} seam: identity headless. */
    private static int scriptHooksWithinPerception(GameObject watcher, GameObject target,
            int type, int result) {
        return result;
    }

    /** Port of {@code isWithinPerceptionDetailed} (0x42BA04). */
    public static int isWithinPerceptionDetailed(GameObject critter, GameObject target, int type) {
        if (target == null) {
            return CombatAiDefs.PerceptionResult.PERCEPTION_OUT_OF_RANGE.id();
        }

        int distance = ObjectSystem.objectGetDistanceBetween(target, critter);
        int perception = Stat.critterGetStat(critter, StatDefs.Stat.STAT_PERCEPTION.id());
        if (canSee(critter, target)) {
            int maxDistance = perception * 5;
            if ((target.flags.bits() & ObjTypes.ObjectFlags.TRANS_GLASS.bits()) != 0) {
                maxDistance /= 2;
            }

            maxDistance = adjustPerceptionDistanceForDudeSneak(maxDistance, target);

            if (distance <= maxDistance) {
                return scriptHooksWithinPerception(critter, target, type,
                    CombatAiDefs.PerceptionResult.PERCEPTION_IN_RANGE.id());
            }
        }

        int maxDistance;
        if (Combat.isInCombat()) {
            maxDistance = perception * 2;
        } else {
            maxDistance = perception;
        }

        maxDistance = adjustPerceptionDistanceForDudeSneak(maxDistance, target);

        if (distance <= maxDistance) {
            return scriptHooksWithinPerception(critter, target, type,
                CombatAiDefs.PerceptionResult.PERCEPTION_IN_RANGE.id());
        }

        return scriptHooksWithinPerception(critter, target, type,
            CombatAiDefs.PerceptionResult.PERCEPTION_OUT_OF_RANGE.id());
    }

    /** Port of {@code isWithinPerception} (0x42BB24). */
    public static boolean isWithinPerception(GameObject watcher, GameObject target) {
        return isWithinPerceptionDetailed(watcher, target,
            CombatAiDefs.PerceptionType.PERCEPTION_OTHER.id())
            != CombatAiDefs.PerceptionResult.PERCEPTION_OUT_OF_RANGE.id();
    }

    // _can_see (src/actions.cc:1580): the target lies in the watcher's
    // facing cone (same rotation or one step off).
    public static boolean canSee(GameObject source, GameObject target) {
        int diff = source.rotation.id()
            - TileMath.tileGetRotationTo(source.tile, target.tile).value();
        if (diff < 0) {
            diff = -diff;
        }

        return diff == 0 || diff == 1 || diff == 5;
    }

    // ===== packet readers (src/combat_ai.cc) =====

    /** {@code aiGetBestWeapon} (0x4281FC). */
    public static int aiGetBestWeapon(GameObject obj) {
        return aiGetPacket(obj).bestWeapon;
    }

    /** {@code aiGetDistance} (0x428208). */
    public static int aiGetDistance(GameObject obj) {
        return aiGetPacket(obj).distance;
    }

    /** {@code aiGetAttackWho} (0x428214). */
    public static int aiGetAttackWho(GameObject obj) {
        return aiGetPacket(obj).attackWho;
    }

    /** {@code aiGetDisposition} (0x428340). */
    public static int aiGetDisposition(GameObject obj) {
        if (obj == null) {
            return CombatAiDefs.Disposition.DISPOSITION_NONE.id();
        }

        return aiGetPacket(obj).disposition;
    }

    // ===== AI movement (src/combat_ai.cc) =====

    /** {@code _combatai_msg} seam: the taunt/message display is game phase. */
    private static void combataiMsg(GameObject critter, int type) {
    }

    // 0x428838 aiFinishMovement: the combat-turn wait is game phase (the
    // turn-run counter is zero outside an AI turn); the SFALL incapacitation
    // AP clear stays.
    private static void aiFinishMovement(GameObject critter) {
        ObjTypes.CritterCombatData combatData = critter.data.critter.combat;
        if ((combatData.results.bits()
            & (ObjTypes.Dam.DEAD.bits() | ObjTypes.Dam.KNOCKED_OUT.bits()
                | ObjTypes.Dam.LOSE_TURN.bits())) != 0) {
            combatData.ap = 0;
        }
    }

    /** Port of {@code _ai_run_away} (0x428868). */
    public static void aiRunAway(GameObject critter, GameObject enemy) {
        if (enemy == null) {
            enemy = ObjectSystem.getDude();
        }

        ObjTypes.CritterCombatData combatData = critter.data.critter.combat;

        AiPacket ai = aiGetPacket(critter);
        int distance = ObjectSystem.objectGetDistanceBetween(critter, enemy);
        if (distance <= ai.maxDist) {
            combatData.maneuver = combatData.maneuver.or(ObjTypes.CritterManeuver.FLEEING);

            int rotation = TileMath.tileGetRotationTo(enemy.tile, critter.tile).value();

            int destination = critter.tile;
            int actionPoints = combatData.ap;
            for (; actionPoints > 0; actionPoints -= 1) {
                destination = TileMath.tileGetTileInDirection(critter.tile,
                    org.fallout2ce.Rotation.fromInt(rotation), actionPoints);
                if (Animation.makePath(critter, critter.tile, destination, null, 1) > 0) {
                    break;
                }

                destination = TileMath.tileGetTileInDirection(critter.tile,
                    org.fallout2ce.Rotation.fromInt((rotation + 1) % 6), actionPoints);
                if (Animation.makePath(critter, critter.tile, destination, null, 1) > 0) {
                    break;
                }

                destination = TileMath.tileGetTileInDirection(critter.tile,
                    org.fallout2ce.Rotation.fromInt((rotation + 5) % 6), actionPoints);
                if (Animation.makePath(critter, critter.tile, destination, null, 1) > 0) {
                    break;
                }
            }

            if (actionPoints > 0) {
                Animation.regAnimBegin(Animation.ANIMATION_REQUEST_RESERVED);
                combataiMsg(critter, 2 /* AI_MESSAGE_TYPE_RUN */);
                Animation.animationRegisterRunToTile(critter, destination, critter.elevation,
                    combatData.ap, 0);
                if (Animation.regAnimEnd() == 0) {
                    aiFinishMovement(critter);
                }
            }
        } else {
            combatData.maneuver = combatData.maneuver.or(ObjTypes.CritterManeuver.DISENGAGING);
        }
    }

    /** Port of {@code _ai_move_away} (0x42899C). */
    public static int aiMoveAway(GameObject critter, GameObject enemy, int distance) {
        if (aiGetPacket(critter).distance == CombatAiDefs.DistanceMode.DISTANCE_STAY.id()) {
            return -1;
        }

        if (ObjectSystem.objectGetDistanceBetween(critter, enemy) <= distance) {
            int actionPoints = critter.data.critter.combat.ap;
            if (distance < actionPoints) {
                actionPoints = distance;
            }

            int rotation = TileMath.tileGetRotationTo(enemy.tile, critter.tile).value();

            int destination = critter.tile;
            int actionPointsLeft = actionPoints;
            for (; actionPointsLeft > 0; actionPointsLeft -= 1) {
                destination = TileMath.tileGetTileInDirection(critter.tile,
                    org.fallout2ce.Rotation.fromInt(rotation), actionPointsLeft);
                if (Animation.makePath(critter, critter.tile, destination, null, 1) > 0) {
                    break;
                }

                destination = TileMath.tileGetTileInDirection(critter.tile,
                    org.fallout2ce.Rotation.fromInt((rotation + 1) % 6), actionPointsLeft);
                if (Animation.makePath(critter, critter.tile, destination, null, 1) > 0) {
                    break;
                }

                destination = TileMath.tileGetTileInDirection(critter.tile,
                    org.fallout2ce.Rotation.fromInt((rotation + 5) % 6), actionPointsLeft);
                if (Animation.makePath(critter, critter.tile, destination, null, 1) > 0) {
                    break;
                }
            }

            if (actionPointsLeft > 0) {
                Animation.regAnimBegin(Animation.ANIMATION_REQUEST_RESERVED);
                Animation.animationRegisterMoveToTile(critter, destination, critter.elevation,
                    actionPoints, 0);
                if (Animation.regAnimEnd() == 0) {
                    aiFinishMovement(critter);
                }
            }
        }

        return 0;
    }

    /** Port of {@code _ai_find_friend} (0x428AC4). */
    public static boolean aiFindFriend(GameObject critter, int a2, int a3) {
        GameObject friend = aiFindNearestTeam(critter, critter, 1);
        if (friend == null) {
            return false;
        }

        int distance = ObjectSystem.objectGetDistanceBetween(critter, friend);
        if (distance > a2) {
            return false;
        }

        if (a3 > distance) {
            int steps = ObjectSystem.objectGetDistanceBetween(critter, friend) - a3;
            aiMoveStepsCloser(critter, friend, steps, false);
        }

        return true;
    }

    /** Port of {@code _ai_move_steps_closer} (0x429FC8). */
    public static int aiMoveStepsCloser(GameObject critter, GameObject target, int actionPoints,
            boolean taunt) {
        if (actionPoints <= 0) {
            return -1;
        }

        int distance = aiGetDistance(critter);
        if (distance == CombatAiDefs.DistanceMode.DISTANCE_STAY.id()) {
            return -1;
        }

        if (distance == CombatAiDefs.DistanceMode.DISTANCE_STAY_CLOSE.id()) {
            if (target != ObjectSystem.getDude()) {
                int currentDistance = ObjectSystem.objectGetDistanceBetween(critter,
                    ObjectSystem.getDude());
                if (currentDistance > 5
                    && ObjectSystem.objectGetDistanceBetween(target,
                        ObjectSystem.getDude()) > 5
                    && currentDistance + actionPoints > 5) {
                    return -1;
                }
            }
        }

        if (ObjectSystem.objectGetDistanceBetween(critter, target) <= 1) {
            return -1;
        }

        Animation.regAnimBegin(Animation.ANIMATION_REQUEST_RESERVED);

        if (taunt) {
            combataiMsg(critter, 0 /* AI_MESSAGE_TYPE_MOVE */);
        }

        GameObject initialTarget = target;

        boolean shouldUnhide;
        if ((target.flags.bits() & ObjTypes.ObjectFlags.MULTIHEX.bits()) != 0) {
            shouldUnhide = true;
            target.flags = target.flags.or(ObjTypes.ObjectFlags.HIDDEN);
        } else {
            shouldUnhide = false;
        }

        if (Animation.pathfinderFindPath(critter, critter.tile, target.tile, null, 0,
                ObjectSystem::objBlockingAt) == 0) {
            ObjectSystem.moveBlockObj = null;
            if (Animation.pathfinderFindPath(critter, critter.tile, target.tile, null, 0,
                    ObjectSystem::objAiBlockingAt) == 0
                && ObjectSystem.moveBlockObj != null
                && ProtoTypes.ProtoId.ofPid(ObjectSystem.moveBlockObj.pid)
                    .objectType() == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
                if (shouldUnhide) {
                    target.flags = target.flags.and(ObjTypes.ObjectFlags.HIDDEN.not());
                }

                target = ObjectSystem.moveBlockObj;
                if ((target.flags.bits() & ObjTypes.ObjectFlags.MULTIHEX.bits()) != 0) {
                    shouldUnhide = true;
                    target.flags = target.flags.or(ObjTypes.ObjectFlags.HIDDEN);
                } else {
                    shouldUnhide = false;
                }
            }
        }

        if (shouldUnhide) {
            target.flags = target.flags.and(ObjTypes.ObjectFlags.HIDDEN.not());
        }

        int tile = target.tile;
        if (target == initialTarget) {
            int[] tilePtr = new int[] { tile };
            caiRetargetTileFromFriendlyFire(critter, target, tilePtr);
            tile = tilePtr[0];
        }

        if (actionPoints >= Stat.critterGetStat(critter,
                StatDefs.Stat.STAT_MAXIMUM_ACTION_POINTS.id()) / 2
            && Art.artCritterFrmIdShouldRun(FrmId.ofFid(critter.fid)) != 0) {
            if ((target.flags.bits() & ObjTypes.ObjectFlags.MULTIHEX.bits()) != 0) {
                Animation.animationRegisterRunToObject(critter, target, actionPoints, 0);
            } else {
                Animation.animationRegisterRunToTile(critter, tile, critter.elevation,
                    actionPoints, 0);
            }
        } else {
            if ((target.flags.bits() & ObjTypes.ObjectFlags.MULTIHEX.bits()) != 0) {
                Animation.animationRegisterMoveToObject(critter, target, actionPoints, 0);
            } else {
                Animation.animationRegisterMoveToTile(critter, tile, critter.elevation,
                    actionPoints, 0);
            }
        }

        if (Animation.regAnimEnd() != 0) {
            return -1;
        }

        aiFinishMovement(critter);

        return 0;
    }

    /** {@code _ai_move_closer} (0x42A1C0). */
    public static int aiMoveCloser(GameObject critter, GameObject target, boolean taunt) {
        return aiMoveStepsCloser(critter, target, critter.data.critter.combat.ap, taunt);
    }

    /** Port of {@code _cai_perform_distance_prefs} (0x42AFDC). */
    public static int caiPerformDistancePrefs(GameObject a1, GameObject a2) {
        if (a1.data.critter.combat.ap <= 0) {
            return -1;
        }

        int distance = aiGetPacket(a1).distance;

        if (a2 != null) {
            if ((a2.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) != 0) {
                a2 = null;
            }
        }

        if (distance == CombatAiDefs.DistanceMode.DISTANCE_STAY_CLOSE.id()) {
            if (a1.data.critter.combat.whoHitMe != ObjectSystem.getDude()) {
                int dudeDistance = ObjectSystem.objectGetDistanceBetween(a1, ObjectSystem.getDude());
                if (dudeDistance > 5) {
                    aiMoveStepsCloser(a1, ObjectSystem.getDude(), dudeDistance - 5, false);
                }
            }
        } else if (distance == CombatAiDefs.DistanceMode.DISTANCE_CHARGE.id()) {
            if (a2 != null) {
                aiMoveCloser(a1, a2, true);
            }
        } else if (distance == CombatAiDefs.DistanceMode.DISTANCE_SNIPE.id()) {
            if (a2 != null) {
                // SFALL: Fix AI behavior for "Snipe" distance preference.
                int targetDistance = ObjectSystem.objectGetDistanceBetween(a1, a2);
                if (targetDistance < 10) {
                    int attackCost = Item.weaponGetActionPointCost(a1,
                        CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), false);
                    int movementPoints = a1.data.critter.combat.ap - attackCost;
                    if (movementPoints > 0) {
                        if (movementPoints + targetDistance - 1 < 5) {
                            int attackerRating = combataiRating(a1);
                            int defenderRating = combataiRating(a2);
                            if (attackerRating < defenderRating) {
                                aiMoveAway(a1, a2, 10);
                            }
                        }
                    } else {
                        aiMoveAway(a1, a2, 10);
                    }
                }
            }
        }

        int[] tilePtr = new int[] { a1.tile };
        if (caiRetargetTileFromFriendlyFire(a1, a2, tilePtr) == 0
                && tilePtr[0] != a1.tile) {
            Animation.regAnimBegin(Animation.ANIMATION_REQUEST_RESERVED);
            Animation.animationRegisterMoveToTile(a1, tilePtr[0], a1.elevation,
                a1.data.critter.combat.ap, 0);
            if (Animation.regAnimEnd() != 0) {
                return -1;
            }
            // _combat_turn_run: the combat tail is a game-phase seam (it
            // spins on the animation counter headless).
        }

        return 0;
    }

    /** Port of {@code _ai_called_shot} (0x42A670). */
    public static int aiCalledShot(GameObject attacker, GameObject defender, int hitMode) {
        int hitLocation = CombatDefs.HitLocation.HIT_LOCATION_TORSO.id();

        if (Item.weaponGetActionPointCost(attacker, hitMode, true)
                <= attacker.data.critter.combat.ap) {
            if (Item.critterCanAim(attacker, hitMode)) {
                AiPacket ai = aiGetPacket(attacker);
                if (GameRandom.randomBetween(1, ai.calledFreq) == 1) {
                    int intelligenceRequired = 5;
                    switch (Settings.settings.preferences.combat_difficulty) {
                    case 0: // COMBAT_DIFFICULTY_EASY
                        intelligenceRequired = 7;
                        break;
                    case 1: // COMBAT_DIFFICULTY_NORMAL
                        intelligenceRequired = 5;
                        break;
                    case 2: // COMBAT_DIFFICULTY_HARD
                        intelligenceRequired = 3;
                        break;
                    default:
                        break;
                    }

                    if (Stat.critterGetStat(attacker,
                            StatDefs.Stat.STAT_INTELLIGENCE.id()) >= intelligenceRequired) {
                        hitLocation = GameRandom.randomBetween(CombatDefs.HIT_LOCATION_FIRST,
                            CombatDefs.HIT_LOCATION_SPECIFIC_COUNT);
                        int chanceToHit = Combat.determineToHit(attacker, defender, hitLocation,
                            hitMode);
                        if (chanceToHit < ai.minToHit) {
                            hitLocation = CombatDefs.HitLocation.HIT_LOCATION_TORSO.id();
                        }
                    }
                }
            }
        }

        return hitLocation;
    }

    /** Port of {@code _ai_attack} (0x42A7D8). */
    public static int aiAttack(GameObject attacker, GameObject defender, int hitMode) {
        if ((attacker.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.FLEEING.bits()) != 0) {
            return -1;
        }

        Animation.regAnimBegin(Animation.ANIMATION_REQUEST_RESERVED);
        Animation.animationRegisterRotateToTile(attacker, defender.tile);
        Animation.regAnimEnd();
        // _combat_turn_run: the animation-pump tail is a game-phase seam.

        int hitLocation = aiCalledShot(attacker, defender, hitMode);
        if (Combat.combatAttack(attacker, defender, hitMode, hitLocation) != 0) {
            return -1;
        }

        // _combat_turn_run: game-phase seam.

        return 0;
    }

    /**
     * Port of {@code _combat_ai} (0x42B100), the AI turn. The chem loop
     * ({@code _ai_check_drugs}) and the full attack flow are game-phase
     * seams: the first is documented in the [aimdrug] slice (it stalls in
     * headless object churn), the second in the [aiattack] slice (the
     * rotate-registration step handler needs real art/object wiring). The
     * rest - the flee/distance decisions, the danger-source targeting, the
     * friendly-dead/teammate movement, the extra-AP behavior - follows the
     * C++ verbatim.
     */
    public static void combatAi(GameObject actor, GameObject target) {
        // 0x51820C aiPartyMemberDistances.
        final int[] aiPartyMemberDistances = { 5, 7, 7, 7, 50000 };

        AiPacket ai = aiGetPacket(actor);
        int hpRatio = caiGetMinHp(ai);
        if (ai.runAwayMode != CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_INVALID.id()) {
            int v7 = Stat.critterGetStat(actor,
                    StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id()) * hpRatio / 100;
            int minimumHitPoints = Stat.critterGetStat(actor,
                    StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id()) - v7;
            int currentHitPoints = Stat.critterGetStat(actor,
                    StatDefs.Stat.STAT_CURRENT_HIT_POINTS.id());
            Debug.print("\n%s minHp = %d; curHp = %d", actor.pid, minimumHitPoints,
                    currentHitPoints);
        }

        ObjTypes.CritterCombatData combatData = actor.data.critter.combat;
        if ((combatData.maneuver.bits() & ObjTypes.CritterManeuver.FLEEING.bits()) != 0
                || (combatData.results.bits() & ai.hurtTooMuch) != 0
                || Stat.critterGetStat(actor, StatDefs.Stat.STAT_CURRENT_HIT_POINTS.id())
                    < ai.minHp) {
            Debug.print("%s: FLEEING: I'm Hurt!", actor.pid);
            aiRunAway(actor, target);
            return;
        }

        // The chem loop (_ai_check_drugs) is a documented game-phase seam
        // (see [aimdrug]); the C++ runs it before the distance/attack
        // decisions.
        if (target == null) {
            target = aiDangerSource(actor);
        }

        caiPerformDistancePrefs(actor, target);

        if (target != null) {
            // _ai_try_attack: game-phase seam (see [aiattack]).
        }

        if (target != null
                && (target.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) == 0
                && actor.data.critter.combat.ap != 0
                && ObjectSystem.objectGetDistanceBetween(actor, target) > ai.maxDist) {
            GameObject friendlyDead = Combat.aiInfoGetFriendlyDead(actor);
            if (friendlyDead != null) {
                aiMoveAway(actor, friendlyDead, 10);
                Combat.aiInfoSetFriendlyDead(actor, null);
            } else {
                int perception = Stat.critterGetStat(actor,
                        StatDefs.Stat.STAT_PERCEPTION.id());
                if (!aiFindFriend(actor, perception * 2, 5)) {
                    combatData.maneuver = combatData.maneuver
                            .or(ObjTypes.CritterManeuver.DISENGAGING);
                }
            }
        }

        if (target == null && !PartyMembers.objectIsPartyMember(actor)) {
            GameObject whoHitMe = combatData.whoHitMe;
            if (whoHitMe != null) {
                if ((whoHitMe.data.critter.combat.results.bits()
                        & ObjTypes.Dam.DEAD.bits()) == 0
                        && combatData.damageLastTurn > 0) {
                    GameObject friendlyDead = Combat.aiInfoGetFriendlyDead(actor);
                    if (friendlyDead != null) {
                        aiMoveAway(actor, friendlyDead, 10);
                        Combat.aiInfoSetFriendlyDead(actor, null);
                    } else {
                        Debug.print("%s: FLEEING: Somebody is shooting at me that I can't see!",
                                actor.pid);
                        aiRunAway(actor, null);
                    }
                }
            }
        }

        GameObject friendlyDead = Combat.aiInfoGetFriendlyDead(actor);
        if (friendlyDead != null) {
            aiMoveAway(actor, friendlyDead, 10);
            if (ObjectSystem.objectGetDistanceBetween(actor, friendlyDead) >= 10) {
                Combat.aiInfoSetFriendlyDead(actor, null);
            }
        }

        GameObject nearestTeammate;
        int maxTeammateDistance = 5;
        if (actor.data.critter.combat.team != 0) {
            nearestTeammate = aiFindNearestTeamInCombat(actor, actor, 1);
        } else {
            nearestTeammate = ObjectSystem.getDude();
            if (PartyMembers.objectIsPartyMember(actor)) {
                int distance = aiGetDistance(actor);
                if (distance != CombatAiDefs.DistanceMode.DISTANCE_INVALID.id()) {
                    maxTeammateDistance = aiPartyMemberDistances[distance];
                }
            }
        }

        if (target == null && nearestTeammate != null
                && ObjectSystem.objectGetDistanceBetween(actor, nearestTeammate)
                    > maxTeammateDistance) {
            int currentDistance = ObjectSystem.objectGetDistanceBetween(actor,
                    nearestTeammate);
            aiMoveStepsCloser(actor, nearestTeammate, currentDistance - maxTeammateDistance,
                    false);
        } else {
            if (actor.data.critter.combat.ap > 0) {
                Debug.print("\n>>>NOTE: %s had extra AP's to use!<<<", actor.pid);
                caiPerformDistancePrefs(actor, target);
            }
        }
    }

    /**
     * Port of {@code _combatai_want_to_join} (0x42B3FC). The script-proc
     * branch and {@code _process_bk} are game-phase seams.
     */
    public static boolean combataiWantToJoin(GameObject a1) {
        if ((a1.flags.bits() & ObjTypes.ObjectFlags.HIDDEN.bits()) != 0) {
            return false;
        }

        if (a1.elevation != ObjectSystem.getDude().elevation) {
            return false;
        }

        if ((a1.data.critter.combat.results.bits()
                & (ObjTypes.Dam.DEAD.bits() | ObjTypes.Dam.KNOCKED_OUT.bits())) != 0) {
            return false;
        }

        if (a1.data.critter.combat.damageLastTurn > 0) {
            return true;
        }

        if (a1.sid != -1) {
            // scriptSetObjects/scriptSetFixedParam/scriptExecProc(COMBAT):
            // script-engine seam.
        }

        if ((a1.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.ENGAGING.bits()) != 0) {
            return true;
        }

        if ((a1.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.DISENGAGING.bits()) != 0) {
            return false;
        }

        if ((a1.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.FLEEING.bits()) != 0) {
            return false;
        }

        return aiDangerSource(a1) != null;
    }

    /**
     * Port of {@code _combatai_want_to_stop} (0x42B4A8).
     */
    public static boolean combataiWantToStop(GameObject a1) {
        if ((a1.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.DISENGAGING.bits()) != 0) {
            return true;
        }

        if ((a1.data.critter.combat.results.bits()
                & (ObjTypes.Dam.KNOCKED_OUT.bits() | ObjTypes.Dam.DEAD.bits())) != 0) {
            return true;
        }

        if ((a1.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.FLEEING.bits()) != 0) {
            return true;
        }

        GameObject enemy = aiDangerSource(a1);
        return enemy == null || !isWithinPerception(a1, enemy);
    }
    public static boolean aiSearchEnviron(GameObject critter, int itemType) {
        if (Critter.critterGetBodyType(critter)
                != ProtoTypes.BodyType.BODY_TYPE_BIPED) {
            return false;
        }

        GameObject[] objects = ObjectSystem.objectListCreate(-1, Map.gElevation,
                ObjTypes.ObjectType.OBJ_TYPE_ITEM.id());
        if (objects.length == 0) {
            return false;
        }

        aiSortListDistance(objects, objects.length, critter);

        int perception = Stat.critterGetStat(critter,
                StatDefs.Stat.STAT_PERCEPTION.id()) + 5;
        GameObject item2 = Inventory.critterGetItem2(critter);

        for (int index = 0; index < objects.length; index++) {
            GameObject item = objects[index];
            if (ObjectSystem.objectGetDistanceBetween(critter, item) > perception) {
                break;
            }

            if (Item.itemGetType(item).id() == itemType) {
                if (itemType == ProtoTypes.ItemType.ITEM_TYPE_WEAPON.id()) {
                    if (aiCanUseWeapon(critter, item,
                            CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id())) {
                        return true;
                    }
                } else if (itemType == ProtoTypes.ItemType.ITEM_TYPE_AMMO.id()) {
                    if (Item.weaponCanBeReloadedWith(item2, item)) {
                        return true;
                    }
                } else if (itemType == ProtoTypes.ItemType.ITEM_TYPE_DRUG.id()
                        || itemType == ProtoTypes.ItemType.ITEM_TYPE_MISC.id()) {
                    if (aiCanUseItem(critter, item)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** {@code AiRetargetData} (src/combat_ai.cc:60). */
    private static final class AiRetargetData {
        GameObject source;
        GameObject target;
        final GameObject[] critterList = new GameObject[100];
        final int[] ratingList = new int[100];
        int critterCount;
        int sourceTeam;
        int sourceRating;
        boolean notSameTile;
        int[] tiles;
        int currentTileIndex;
        int sourceIntelligence;
    }

    /** Port of {@code _cai_retargetTileFromFriendlyFire} (0x42A1D4). */
    public static int caiRetargetTileFromFriendlyFire(GameObject source, GameObject target,
            int[] tilePtr) {
        if (source == null) {
            return -1;
        }

        if (target == null) {
            return -1;
        }

        if (tilePtr == null) {
            return -1;
        }

        if (tilePtr[0] == -1) {
            return -1;
        }

        if (currCritList.length == 0) {
            return -1;
        }

        int[] tiles = new int[32];

        AiRetargetData aiRetargetData = new AiRetargetData();
        aiRetargetData.source = source;
        aiRetargetData.target = target;
        aiRetargetData.sourceTeam = source.data.critter.combat.team;
        aiRetargetData.sourceRating = combataiRating(source);
        aiRetargetData.critterCount = 0;
        aiRetargetData.tiles = tiles;
        aiRetargetData.notSameTile = tilePtr[0] != source.tile;
        aiRetargetData.currentTileIndex = 0;
        aiRetargetData.sourceIntelligence = Stat.critterGetStat(source,
            StatDefs.Stat.STAT_INTELLIGENCE.id());

        for (int index = 0; index < 32; index++) {
            tiles[index] = -1;
        }

        for (int index = 0; index < currCritList.length; index++) {
            GameObject obj = currCritList[index];
            if ((obj.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) == 0
                && obj.data.critter.combat.team == aiRetargetData.sourceTeam
                && Combat.aiInfoGetLastTarget(obj) == aiRetargetData.target
                && obj != aiRetargetData.source) {
                int rating = combataiRating(obj);
                if (rating >= aiRetargetData.sourceRating) {
                    aiRetargetData.critterList[aiRetargetData.critterCount] = obj;
                    aiRetargetData.ratingList[aiRetargetData.critterCount] = rating;
                    aiRetargetData.critterCount += 1;
                }
            }
        }

        aiSortListDistance(aiRetargetData.critterList, aiRetargetData.critterCount, source);

        if (caiRetargetTileFromFriendlyFireSubFunc(aiRetargetData, tilePtr[0]) == 0) {
            int minDistance = TileMath.TILE_MAX_DISTANCE + 1;
            int minDistanceIndex = -1;

            for (int index = 0; index < 32; index++) {
                int tile = tiles[index];
                if (!TileMath.tileIsValid(tile)) {
                    break;
                }

                if (ObjectSystem.objBlockingAt(null, tile, source.elevation) == null) {
                    int candidateDistance = TileMath.tileDistanceBetween(tilePtr[0], tile);
                    if (candidateDistance < minDistance) {
                        minDistance = candidateDistance;
                        minDistanceIndex = index;
                    }
                }
            }

            if (minDistanceIndex != -1) {
                tilePtr[0] = tiles[minDistanceIndex];
            }
        }

        return 0;
    }

    // 0x42A410 _cai_retargetTileFromFriendlyFireSubFunc.
    private static int caiRetargetTileFromFriendlyFireSubFunc(AiRetargetData aiRetargetData,
            int tile) {
        if (aiRetargetData.sourceIntelligence <= 0) {
            return 0;
        }

        int distance = 1;

        for (int index = 0; index < aiRetargetData.critterCount; index++) {
            GameObject obj = aiRetargetData.critterList[index];
            if (caiAttackWouldIntersect(obj, aiRetargetData.target, aiRetargetData.source, tile,
                    new int[] { distance })) {
                Debug.print("In the way!");

                aiRetargetData.tiles[aiRetargetData.currentTileIndex] =
                    TileMath.tileGetTileInDirection(tile,
                        org.fallout2ce.Rotation.fromInt((obj.rotation.id() + 1) % 6), distance);
                aiRetargetData.tiles[aiRetargetData.currentTileIndex + 1] =
                    TileMath.tileGetTileInDirection(tile,
                        org.fallout2ce.Rotation.fromInt((obj.rotation.id() + 5) % 6), distance);

                aiRetargetData.sourceIntelligence -= 2;
                aiRetargetData.currentTileIndex += 2;
                break;
            }
        }

        return 0;
    }

    // 0x42A518 _cai_attackWouldIntersect.
    static boolean caiAttackWouldIntersect(GameObject attacker, GameObject defender,
            GameObject attackerFriend, int tile, int[] distance) {
        int hitMode = CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id();
        boolean aiming = false;
        if (attacker == ObjectSystem.getDude()) {
            // interfaceGetCurrentHitMode: game phase; the dude is not a
            // vector actor here.
        }

        GameObject weapon = Item.critterGetWeaponForHitModeForAi(attacker, hitMode);
        if (weapon == null) {
            return false;
        }

        if (Item.weaponGetRange(attacker, hitMode) < 1) {
            return false;
        }

        GameObject object = ObjectSystem.makeStraightPath(attacker, attacker.tile, defender.tile);
        if (object != attackerFriend) {
            if (!Combat.combatTestIncidentalHit(attacker, defender, attackerFriend, weapon)) {
                return false;
            }
        }

        return true;
    }

    /** Test seam: fresh-process mirror. */
    static void resetForTests() {
        aiPackets = new AiPacket[0];
        aiInitialized = false;
        burstDisabledCritters.clear();
        combatAiOver();
        combatObj = null;
    }

    /** Test seam: direct table access (fresh-process mirror). */
    static AiPacket aiGetPacketByIndex(int index) {
        return aiPackets[index];
    }

    static int aiPacketsLength() {
        return aiPackets.length;
    }
}
