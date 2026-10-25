package org.fallout2ce;

import org.fallout2ce.Config.Cfg;
import org.fallout2ce.Config.ScopedConfig;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * Combat AI: port of {@code src/combat_ai.cc} slices. This covers the AI
 * packet table ({@code aiInit} parsing {@code data/ai.txt} + {@code aiGetPacketByNum}),
 * the packet reader helpers ({@code aiGetAreaAttackMode}, {@code aiGetRunAwayMode})
 * and the threat rating ({@code _combatai_rating}). The movement/attack/chem
 * decision tree stays with the turn/AI slice.
 */
public final class CombatAi {

    private CombatAi() {
    }

    public static final int AI_PACKET_CHEM_PRIMARY_DESIRE_COUNT = 3;

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
            int[] minMax = new int[2];
            if (Item.weaponGetDamageMinMax(item, minMax, minMax) != -1
                    && meleeDamage < minMax[1]) {
                meleeDamage = minMax[1];
            }
        }

        item = Inventory.critterGetItem1(obj);
        if (item != null && Item.itemGetType(item) == ProtoTypes.ItemType.ITEM_TYPE_WEAPON) {
            int[] minMax = new int[2];
            if (Item.weaponGetDamageMinMax(item, minMax, minMax) != -1
                    && meleeDamage < minMax[1]) {
                meleeDamage = minMax[1];
            }
        }

        return meleeDamage + Stat.critterGetStat(obj,
                StatDefs.Stat.STAT_ARMOR_CLASS.id());
    }

    /** Test seam: fresh-process mirror. */
    static void resetForTests() {
        aiPackets = new AiPacket[0];
        aiInitialized = false;
    }

    static int aiPacketsLength() {
        return aiPackets.length;
    }
}
