package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjTypes.ObjectFlags;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Party-member init/add slice, mirroring {@code src/party_member.cc}:
 * {@code partyMembersInit} ({@code partyMember_init}), the
 * {@code data\party.txt} parse ({@code partyMembersParseConfig},
 * CE-extracted), {@code partyMemberAdd} ({@code partyMemberAdd}) with the
 * fixed {@code (pid & kMaxProtoId) + 18000} id rule, plus
 * {@code partyMembersReset} ({@code partyMember_reset}),
 * {@code partyMembersExit} ({@code partyMember_exit}) and the description
 * init ({@code partyMemberAISlotInit}).
 *
 * <p>Vectors: {@code party=0} in {@code [partyobjects]} in
 * {@code docker/refharness/golden/v96.txt}, produced by
 * {@code sectionPartyObjects} in {@code docker/refharness/ref_main.cc}
 * (only the return code is observed; the 26 parsed descriptions ride
 * along unobserved).
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>Fid source: none — this slice never touches art.</li>
 * <li>{@code partyMemberAdd} duplicates C exactly: capacity
 * ({@code length >= descriptions + 20} → {@code -1}), pointer-or-pid
 * dupe scan (→ {@code 0}), {@code _partyStatePrepped} deny (→
 * {@code -1}), append with null script/vars, the fixed id
 * ({@code scriptsNewObjectId} is NOT consulted) and
 * {@code NO_REMOVE|NO_SAVE} flags. The id counter is untouched, so ids
 * stay structural (dude is always 18000), exactly like C.</li>
 * <li>Script tail ({@code scriptGetScript} flags/owner/sid remap): the
 * script engine is unported; harness objects carry {@code sid == -1},
 * so the tail is dormant.</li>
 * <li>{@code critterSetTeam(object, 0)}: the combat-AI function is
 * unported; the pure prefix (critter gate + team store) runs inline and
 * the {@code whoHitMe} tail stays a game-phase seam.</li>
 * <li>{@code queueRemoveEventsByType(object, EVENT_TYPE_SCRIPT)} runs
 * for real (the event queue is ported); {@code _gdialogActive} is false
 * headless (dialog unported), so the speaker tail is dormant.</li>
 * <li>A null object is not guarded (C dereferences; Java throws
 * {@link NullPointerException}, like
 * {@link ObjectSystem#objectGetLightIntensity}).</li>
 * <li>{@link Item} keeps its own {@code objectIsPartyMember} stub
 * (always false headless); wiring it to this roster is game phase
 * (save/load + map ports own the rest of {@code party_member.cc}).</li>
 * <li>Like C, {@code partyMembersInit} only zeroes the length before
 * parsing (vectors never re-init); {@link #resetForTests} clears
 * everything for test isolation.</li>
 * </ul>
 */
public final class PartyMembers {

    private PartyMembers() {
    }

    /** SFALL: maximum party member level (vanilla 5 → 10). */
    public static final int PARTY_MEMBER_MAX_LEVEL = 10;

    // gAreaAttackModeKeys (src/combat_ai.cc 0x518068).
    static final String[] AREA_ATTACK_MODE_KEYS = {
        "always",
        "sometimes",
        "be_sure",
        "be_careful",
        "be_absolutely_sure",
    };

    // gAttackWhoKeys (src/combat_ai.cc 0x5180D0).
    static final String[] ATTACK_WHO_KEYS = {
        "whomever_attacking_me",
        "strongest",
        "weakest",
        "whomever",
        "closest",
    };

    // gBestWeaponKeys (src/combat_ai.cc 0x51809C).
    static final String[] BEST_WEAPON_KEYS = {
        "no_pref",
        "melee",
        "melee_over_ranged",
        "ranged_over_melee",
        "ranged",
        "unarmed",
        "unarmed_over_thrown",
        "random",
    };

    // gChemUseKeys (src/combat_ai.cc 0x5180E4).
    static final String[] CHEM_USE_KEYS = {
        "clean",
        "stims_when_hurt_little",
        "stims_when_hurt_lots",
        "sometimes",
        "anytime",
        "always",
    };

    // gDistanceModeKeys (src/combat_ai.cc 0x5180BC).
    static final String[] DISTANCE_MODE_KEYS = {
        "stay_close",
        "charge",
        "snipe",
        "on_your_own",
        "stay",
    };

    // gRunAwayModeKeys (src/combat_ai.cc 0x518080).
    static final String[] RUN_AWAY_MODE_KEYS = {
        "none",
        "coward",
        "finger_hurts",
        "bleeding",
        "not_feeling_good",
        "tourniquet",
        "never",
    };

    // gDispositionKeys (src/combat_ai.cc 0x5180FC).
    static final String[] DISPOSITION_KEYS = {
        "none",
        "custom",
        "coward",
        "defensive",
        "aggressive",
        "berserk",
    };

    /** Port of {@code PartyMemberDescription} (src/party_member.cc). */
    public static final class PartyMemberDescription {
        public final boolean[] areaAttackMode = new boolean[CombatAiDefs.AREA_ATTACK_MODE_COUNT];
        public final boolean[] runAwayMode = new boolean[CombatAiDefs.RUN_AWAY_MODE_COUNT];
        public final boolean[] bestWeapon = new boolean[CombatAiDefs.BEST_WEAPON_COUNT];
        public final boolean[] distanceMode = new boolean[CombatAiDefs.DISTANCE_COUNT];
        public final boolean[] attackWho = new boolean[CombatAiDefs.ATTACK_WHO_COUNT];
        public final boolean[] chemUse = new boolean[CombatAiDefs.CHEM_USE_COUNT];
        public final boolean[] disposition = new boolean[CombatAiDefs.DISPOSITION_COUNT];
        public int levelMinimum;
        public int levelUpEvery;
        public int levelPidsNum;
        public final int[] levelPids = new int[PARTY_MEMBER_MAX_LEVEL];
    }

    /** Port of {@code STRU_519DBC} (level-up tracking, reset/exit only here). */
    public static final class PartyMemberLevelUpInfo {
        public int level;
        public int numLevelUps;
        public int isEarly;
    }

    /** Port of {@code PartyMemberListItem} (script/vars opaque: engine unported). */
    public static final class PartyMemberListItem {
        public GameObject object;
        public Object script;
        public int[] vars;
    }

    /** Port of {@code gPartyMemberDescriptionsLength} (0x519D9C). */
    static int descriptionsLength;

    /** Port of {@code gPartyMemberProtoIds} (0x519DA0). */
    static final List<ProtoId> partyMemberProtoIds = new ArrayList<>();

    /**
     * Port of {@code gPartyMembers} (0x519DA8): sized
     * descriptions + 20 (null-padded past the length, like the
     * value-initialized C++ vector).
     */
    static final List<PartyMemberListItem> partyMembers = new ArrayList<>();

    /** Port of {@code gPartyMemberCount} (0x519DAC). */
    static int partyMembersLength;

    /** Port of {@code _partyStatePrepped} (0x519DB4). */
    static int partyStatePrepped;

    /** Port of {@code gPartyMemberDescriptions} (0x519DB8). */
    static final List<PartyMemberDescription> descriptions = new ArrayList<>();

    /** Port of {@code _partyMemberLevelUpInfoList} (0x519DBC). */
    static final List<PartyMemberLevelUpInfo> levelUpInfos = new ArrayList<>();

    /** Port of {@code npcEngineLevelUp} (SFALL toggle). */
    static boolean npcEngineLevelUp = true;

    /**
     * Port of {@code partyMemberGetDescription} (src/party_member.cc): finds
     * the description whose proto id matches the object's, starting at index 1
     * (index 0 is the player character).
     */
    private static int partyMemberGetDescription(GameObject object, PartyMemberDescription[] out) {
        for (int index = 1; index < descriptionsLength; index++) {
            if (partyMemberProtoIds.get(index).equals(ProtoId.ofGameObject(object))) {
                out[0] = descriptions.get(index);
                return 0;
            }
        }
        return -1;
    }

    /**
     * Port of {@code partyMemberFindByProtoId} (0x4A2A34): the party member
     * with the given proto id, or null.
     */
    public static GameObject partyMemberFindByProtoId(ProtoId protoId) {
        for (int index = 0; index < partyMembersLength; index++) {
            GameObject object = partyMembers.get(index).object;
            if (object != null && ProtoId.ofGameObject(object).equals(protoId)) {
                return object;
            }
        }

        return null;
    }

    /** Port of {@code partyMemberGetCurrentLevel} (0x495FF0): 0 when not a member. */
    public static int partyMemberGetCurrentLevel(GameObject object) {
        if (object == null) {
            return 0;
        }

        for (int index = 1; index < descriptionsLength; index++) {
            if (partyMemberProtoIds.get(index).equals(ProtoId.ofGameObject(object))) {
                return levelUpInfos.get(index).level;
            }
        }

        return 0;
    }

    /** Port of {@code partyMemberSupportsBestWeapon} (0x4959D4). */
    public static boolean partyMemberSupportsBestWeapon(GameObject object, int bestWeapon) {
        if (ProtoId.ofGameObject(object).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return false;
        }
        if (!CombatAiDefs.bestWeaponIsValid(bestWeapon)) {
            return false;
        }
        PartyMemberDescription[] desc = new PartyMemberDescription[1];
        if (partyMemberGetDescription(object, desc) == -1) {
            return false;
        }
        return desc[0].bestWeapon[bestWeapon];
    }

    /** Port of {@code partyMemberSupportsDistance} (0x495A40). */
    public static boolean partyMemberSupportsDistance(GameObject object, int distanceMode) {
        if (ProtoId.ofGameObject(object).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return false;
        }
        if (!CombatAiDefs.distanceModeIsValid(distanceMode)) {
            return false;
        }
        PartyMemberDescription[] desc = new PartyMemberDescription[1];
        if (partyMemberGetDescription(object, desc) == -1) {
            return false;
        }
        return desc[0].distanceMode[distanceMode];
    }

    /** Port of {@code partyMemberSupportsChemUse}. */
    public static boolean partyMemberSupportsChemUse(GameObject object, int chemUse) {
        if (ProtoId.ofGameObject(object).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return false;
        }
        if (!CombatAiDefs.chemUseIsValid(chemUse)) {
            return false;
        }
        PartyMemberDescription[] desc = new PartyMemberDescription[1];
        if (partyMemberGetDescription(object, desc) == -1) {
            return false;
        }
        return desc[0].chemUse[chemUse];
    }

    /** Port of {@code partyMemberSupportsAreaAttackMode}. */
    public static boolean partyMemberSupportsAreaAttackMode(GameObject object, int mode) {
        if (ProtoId.ofGameObject(object).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return false;
        }
        if (!CombatAiDefs.areaAttackModeIsValid(mode)) {
            return false;
        }
        PartyMemberDescription[] desc = new PartyMemberDescription[1];
        if (partyMemberGetDescription(object, desc) == -1) {
            return false;
        }
        return desc[0].areaAttackMode[mode];
    }

    // CE: extracted from partyMembersInit().
    static int partyMembersParseConfig(Config.Cfg config, boolean reindex) {
        if (config == null) {
            return -1;
        }

        int searchIdx = reindex ? 0 : descriptions.size();
        int loopSafetyCounter = 0;

        while (loopSafetyCounter < 1000) {
            String section = "Party Member " + searchIdx;

            int[] partyMemberPid = new int[1];
            if (!Config.configGetInt(config, section, "party_member_pid", partyMemberPid)) {
                break;
            }

            partyMemberProtoIds.add(ProtoId.ofPid(partyMemberPid[0]));

            PartyMemberDescription desc = new PartyMemberDescription();
            partyMemberDescriptionInit(desc);

            if (parseKeyList(config, section, "area_attack_mode", AREA_ATTACK_MODE_KEYS, desc.areaAttackMode)) {
                // Parsed (return value unused, like C).
            }
            if (parseKeyList(config, section, "attack_who", ATTACK_WHO_KEYS, desc.attackWho)) {
                // Parsed (return value unused, like C).
            }
            if (parseKeyList(config, section, "best_weapon", BEST_WEAPON_KEYS, desc.bestWeapon)) {
                // Parsed (return value unused, like C).
            }
            if (parseKeyList(config, section, "chem_use", CHEM_USE_KEYS, desc.chemUse)) {
                // Parsed (return value unused, like C).
            }
            if (parseKeyList(config, section, "distance", DISTANCE_MODE_KEYS, desc.distanceMode)) {
                // Parsed (return value unused, like C).
            }
            if (parseKeyList(config, section, "run_away_mode", RUN_AWAY_MODE_KEYS, desc.runAwayMode)) {
                // Parsed (return value unused, like C).
            }
            if (parseKeyList(config, section, "disposition", DISPOSITION_KEYS, desc.disposition)) {
                // Parsed (return value unused, like C).
            }

            int[] levelUpEvery = new int[1];
            if (Config.configGetInt(config, section, "level_up_every", levelUpEvery)) {
                desc.levelUpEvery = levelUpEvery[0];

                int[] levelMinimum = new int[1];
                if (Config.configGetInt(config, section, "level_minimum", levelMinimum)) {
                    desc.levelMinimum = levelMinimum[0];
                }

                String[] levelPids = new String[1];
                if (Config.configGetString(config, section, "level_pids", levelPids)) {
                    StringParsers.Cursor cursor = new StringParsers.Cursor(levelPids[0]);
                    while (cursor.pos < cursor.text.length()
                            && desc.levelPidsNum < PARTY_MEMBER_MAX_LEVEL) {
                        int[] levelPid = new int[1];
                        StringParsers.strParseInt(cursor, levelPid);
                        desc.levelPids[desc.levelPidsNum] = levelPid[0];
                        desc.levelPidsNum++;
                    }
                }
            }

            descriptions.add(desc);
            levelUpInfos.add(new PartyMemberLevelUpInfo());

            searchIdx++;
            loopSafetyCounter++;
        }

        while (partyMembers.size() < descriptions.size() + 20) {
            partyMembers.add(null);
        }
        descriptionsLength = descriptions.size();

        return 0;
    }

    // One `area_attack_mode`-style key: tokenize the comma list and set the
    // matching slots (unknown tokens ignored, like C).
    private static boolean parseKeyList(Config.Cfg config, String section, String key,
            String[] keys, boolean[] slots) {
        String[] value = new String[1];
        if (!Config.configGetString(config, section, key, value)) {
            return false;
        }

        StringParsers.Cursor cursor = new StringParsers.Cursor(value[0]);
        while (cursor.pos < cursor.text.length()) {
            int[] parsed = new int[1];
            if (StringParsers.strParseStrFromList(cursor, parsed, keys) == 0) {
                slots[parsed[0]] = true;
            }
        }

        return true;
    }

    // 0x49425C partyMemberAISlotInit.
    private static void partyMemberDescriptionInit(PartyMemberDescription desc) {
        for (int index = CombatAiDefs.AREA_ATTACK_MODE_FIRST;
                index < CombatAiDefs.AREA_ATTACK_MODE_COUNT; index++) {
            desc.areaAttackMode[index] = false;
        }

        for (int index = CombatAiDefs.RUN_AWAY_MODE_FIRST;
                index < CombatAiDefs.RUN_AWAY_MODE_COUNT; index++) {
            desc.runAwayMode[index] = false;
        }

        for (int index = CombatAiDefs.BEST_WEAPON_FIRST;
                index < CombatAiDefs.BEST_WEAPON_COUNT; index++) {
            desc.bestWeapon[index] = false;
        }

        for (int index = CombatAiDefs.DISTANCE_FIRST;
                index < CombatAiDefs.DISTANCE_COUNT; index++) {
            desc.distanceMode[index] = false;
        }

        for (int index = CombatAiDefs.ATTACK_WHO_FIRST;
                index < CombatAiDefs.ATTACK_WHO_COUNT; index++) {
            desc.attackWho[index] = false;
        }

        for (int index = CombatAiDefs.CHEM_USE_FIRST;
                index < CombatAiDefs.CHEM_USE_COUNT; index++) {
            desc.chemUse[index] = false;
        }

        for (int index = CombatAiDefs.DISPOSITION_FIRST;
                index < CombatAiDefs.DISPOSITION_COUNT; index++) {
            desc.disposition[index] = false;
        }

        desc.levelMinimum = 0;
        desc.levelUpEvery = 0;
        desc.levelPidsNum = 0;

        desc.levelPids[0] = -1;

        for (int index = 0; index < descriptionsLength; index++) {
            PartyMemberLevelUpInfo info = levelUpInfos.get(index);
            info.level = 0;
            info.numLevelUps = 0;
            info.isEarly = 0;
        }
    }

    // 0x493BC0 partyMember_init.
    public static int partyMembersInit() {
        descriptionsLength = 0;

        Config.Cfg config = new Config.Cfg();
        Config.configInit(config);
        if (!Config.configRead(config, "data\\party.txt", true)) {
            return -1;
        }

        if (partyMembersParseConfig(config, false) == -1) {
            return -1;
        }

        return 0;
    }

    // 0x4940E4 partyMember_reset.
    public static void partyMembersReset() {
        npcEngineLevelUp = true;

        for (int index = 0; index < descriptionsLength; index++) {
            PartyMemberLevelUpInfo info = levelUpInfos.get(index);
            info.level = 0;
            info.numLevelUps = 0;
            info.isEarly = 0;
        }
    }

    // 0x494134 partyMember_exit.
    public static void partyMembersExit() {
        descriptionsLength = 0;

        partyMemberProtoIds.clear();
        partyMembers.clear();
        descriptions.clear();
        levelUpInfos.clear();
    }

    // 0x494E9C partyMemberSyncPosition: rotation pair off the dude, then
    // per-member placement at distance/2 tiles away (index 0 is the dude
    // itself, so the loop starts at 1). The attempt helper lives in
    // ObjectSystem (worldmap/pathfinder phase behind its seam); the vectors
    // never enter the loop (party is the dude alone).
    public static int partyMemberSyncPosition() {
        GameObject dude = ObjectSystem.getDude();
        if (dude == null) {
            return 0;
        }

        int clockwise = (dude.rotation.id() + 2) % ObjTypes.ROTATION_COUNT;
        int counterClockwise = (dude.rotation.id() + 4) % ObjTypes.ROTATION_COUNT;

        int n = 0;
        int distance = 2;
        for (int index = 1; index < partyMembersLength; index++) {
            PartyMemberListItem member = partyMembers.get(index);
            GameObject memberObj = member.object;
            if (memberObj.flags.and(ObjectFlags.HIDDEN).equals(ObjectFlags.NONE)
                    && ProtoId.ofGameObject(memberObj).objectType()
                            == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
                int rotation;
                if ((n % 2) != 0) {
                    rotation = clockwise;
                } else {
                    rotation = counterClockwise;
                }

                int tile = TileMath.tileGetTileInDirection(dude.tile,
                        Rotation.fromInt(rotation), distance / 2);
                ObjectSystem.objectAttemptPlacementPartyMember(memberObj, tile, dude.elevation);

                distance++;
                n++;
            }
        }

        return 0;
    }

    // 0x494378 partyMemberAdd.
    public static int partyMemberAdd(GameObject object) {
        if (partyMembersLength >= descriptionsLength + 20) {
            return -1;
        }

        for (int index = 0; index < partyMembersLength; index++) {
            PartyMemberListItem partyMember = partyMembers.get(index);
            if (partyMember.object == object || partyMember.object.pid == object.pid) {
                return 0;
            }
        }

        if (partyStatePrepped != 0) {
            Debug.print("\npartyMemberAdd DENIED: %s\n", object);
            return -1;
        }

        PartyMemberListItem partyMember = new PartyMemberListItem();
        partyMember.object = object;
        partyMember.script = null;
        partyMember.vars = null;
        partyMembers.set(partyMembersLength, partyMember);

        object.id = (object.pid & ProtoId.MAX_PROTO_ID) + 18000;
        object.flags = object.flags.or(ObjectFlags.NO_REMOVE).or(ObjectFlags.NO_SAVE);

        partyMembersLength++;

        if (object.sid != -1) {
            // scriptGetScript flags/owner/sid remap: script engine (game
            // phase); harness objects carry sid == -1, so this is dormant.
        }

        // critterSetTeam(object, 0) pure prefix (src/combat_ai.cc 0x42B504):
        // the combat-AI function is unported; the whoHitMe tail needs
        // critterSetWhoHitMe (game phase).
        if (ProtoId.ofGameObject(object).objectType() == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            object.data.critter.combat.team = 0;
        }

        EventQueue.queueRemoveEventsByType(object, EventQueue.EventType.SCRIPT);

        // _gdialogActive() is false headless (dialog unported), so the
        // speaker/status tail is dormant.

        return 0;
    }

    /** Test seam: parsed description count. */
    static int descriptionCount() {
        return descriptionsLength;
    }

    /** Test seam: current party length. */
    static int partyLength() {
        return partyMembersLength;
    }

    /** Test seam: fresh-process mirror (C has no reset; exit leaves the count). */
    static void resetForTests() {
        descriptionsLength = 0;
        partyMemberProtoIds.clear();
        partyMembers.clear();
        descriptions.clear();
        levelUpInfos.clear();
        partyMembersLength = 0;
        partyStatePrepped = 0;
        npcEngineLevelUp = true;
    }
}
