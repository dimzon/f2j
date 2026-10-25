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

    /** Test seam: fresh-process mirror. */
    static void resetForTests() {
        aiPackets = new AiPacket[0];
        aiInitialized = false;
        burstDisabledCritters.clear();
    }

    /** Test seam: direct table access (fresh-process mirror). */
    static AiPacket aiGetPacketByIndex(int index) {
        return aiPackets[index];
    }

    static int aiPacketsLength() {
        return aiPackets.length;
    }
}
