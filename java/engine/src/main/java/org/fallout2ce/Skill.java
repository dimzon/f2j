package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * Skill result codes + skill value engine slice, mirroring the pure parts of
 * {@code src/skill.cc} / {@code src/skill.h}.
 *
 * <p>Vectors: {@code [statskill]} costs/defaults/difficulty live here already;
 * {@code [critterstats]} skill values ({@code sneak/guns/stim/bad}) come
 * from {@link #skillGetValue} (game data needed for the proto skills).
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>{@code gDude} is the player that the object system made. The {@code null == null} C quirk is avoided via a
 * null guard in {@link #isDude}.</li>
 * <li>Perk lookups ({@code perkGetRank}, {@code perkGetSkillModifier}) need
 * the perk engine (unported; {@link Perk} only carries property ids). The
 * private stubs return 0 and are only reachable from the dormant dude
 * block.</li>
 * <li>{@code skillMaximum} (sfall {@code set_skill_max}, default 300) and
 * the tag-tuning flags ({@code tagSkillBonus} 20,
 * {@code tagPerkAppliesInitialBonus} false,
 * {@code tagSkillsDoublePointBonusDisabled} false) mirror
 * {@code skillsInitDefaults}; {@link #resetForTests} restores all of them
 * (plus the zeroed tags/usage) for the fresh-process mirror.</li>
 * <li>The raw-negative branch ({@code SKILLS_MIN_RAW_POINTS -128},
 * {@code SKILLS_MIN_VALUE -999}) is fully ported, including the
 * non-dude-gated {@code skillIsTagged} doubling inside it.</li>
 * </ul>
 */
public final class Skill {

    private Skill() {
    }

    public enum SkillStealResult {
        Caught(0),
        Success(1),
        Fail(2);

        private final int id;

        SkillStealResult(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, SkillStealResult> STEAL_BY_ID = new HashMap<>();

    static {
        for (SkillStealResult e : SkillStealResult.values()) {
            STEAL_BY_ID.put(e.id, e);
        }
    }

    public static SkillStealResult ofSkillStealResult(int id) {
        return STEAL_BY_ID.get(id);
    }

    /**
     * Compile-time per-skill default values, mirroring the
     * {@code defaultValue} column of {@code defaultSkillDescriptions} in
     * {@code src/skill.cc} (index = skill id). No init needed.
     */
    private static final int[] DEFAULT_VALUES = {
        5, // SmallGuns
        0, // BigGuns
        0, // EnergyWeapons
        30, // Unarmed
        20, // MeleeWeapons
        0, // Throwing
        0, // FirstAid
        5, // Doctor
        5, // Sneak
        10, // Lockpick
        0, // Steal
        10, // Traps
        0, // Science
        0, // Repair
        0, // Speech
        0, // Barter
        0, // Gambling
        0, // Outdoorsman
    };

    /**
     * Compile-time per-skill {@code baseValueMult} column of
     * {@code defaultSkillDescriptions} in {@code src/skill.cc} (index =
     * skill id). All ones in the vanilla table; the sfall
     * {@code SkillMultiN} loader (unported) is the only writer.
     */
    private static final int[] BASE_VALUE_MULT = {
        1, // SmallGuns
        1, // BigGuns
        1, // EnergyWeapons
        1, // Unarmed
        1, // MeleeWeapons
        1, // Throwing
        1, // FirstAid
        1, // Doctor
        1, // Sneak
        1, // Lockpick
        1, // Steal
        1, // Traps
        1, // Science
        1, // Repair
        1, // Speech
        1, // Barter
        1, // Gambling
        1, // Outdoorsman
    };

    /**
     * Port of {@code skillStatMultipliers[SKILL_COUNT][PRIMARY_STAT_COUNT]}
     * ({@code src/skill.cc}), as built by {@code skillsInitDefaults}:
     * {@code statModifier} on each skill's {@code stat1}/{@code stat2}
     * columns, 0 elsewhere. Column order is STR, PER, END, CHA, INT, AGI,
     * LUCK. Transcribed from the {@code statModifier}/{@code stat1}/
     * {@code stat2} columns of {@code defaultSkillDescriptions}:
     * SmallGuns(4,AGI) BigGuns(2,AGI) Energy(2,AGI) Unarmed(2,AGI+STR)
     * Melee(2,AGI+STR) Throwing(4,AGI) FirstAid(2,PER+INT) Doctor(1,PER+INT)
     * Sneak(3,AGI) Lockpick(1,PER+AGI) Steal(3,AGI) Traps(1,PER+AGI)
     * Science(4,INT) Repair(3,INT) Speech(5,CHA) Barter(4,CHA)
     * Gambling(5,LUCK) Outdoorsman(2,END+INT).
     */
    private static final double[][] SKILL_STAT_MULTIPLIERS = {
        { 0, 0, 0, 0, 0, 4, 0 }, // SmallGuns: AGI x4
        { 0, 0, 0, 0, 0, 2, 0 }, // BigGuns: AGI x2
        { 0, 0, 0, 0, 0, 2, 0 }, // EnergyWeapons: AGI x2
        { 2, 0, 0, 0, 0, 2, 0 }, // Unarmed: STR x2 + AGI x2
        { 2, 0, 0, 0, 0, 2, 0 }, // MeleeWeapons: STR x2 + AGI x2
        { 0, 0, 0, 0, 0, 4, 0 }, // Throwing: AGI x4
        { 0, 2, 0, 0, 2, 0, 0 }, // FirstAid: PER x2 + INT x2
        { 0, 1, 0, 0, 1, 0, 0 }, // Doctor: PER x1 + INT x1
        { 0, 0, 0, 0, 0, 3, 0 }, // Sneak: AGI x3
        { 0, 1, 0, 0, 0, 1, 0 }, // Lockpick: PER x1 + AGI x1
        { 0, 0, 0, 0, 0, 3, 0 }, // Steal: AGI x3
        { 0, 1, 0, 0, 0, 1, 0 }, // Traps: PER x1 + AGI x1
        { 0, 0, 0, 0, 4, 0, 0 }, // Science: INT x4
        { 0, 0, 0, 0, 3, 0, 0 }, // Repair: INT x3
        { 0, 0, 0, 5, 0, 0, 0 }, // Speech: CHA x5
        { 0, 0, 0, 4, 0, 0, 0 }, // Barter: CHA x4
        { 0, 0, 0, 0, 0, 0, 5 }, // Gambling: LUCK x5
        { 0, 0, 2, 0, 2, 0, 0 }, // Outdoorsman: END x2 + INT x2
    };

    /**
     * Compile-time per-skill {@code frmId} column of
     * {@code gSkillDescriptions}/{@code defaultSkillDescriptions} in
     * {@code src/skill.cc} (index = skill id), as raw skilldex frame ids.
     * Transcribed from the enum names: SmallGuns(28) BigGuns(29)
     * EnergyWeapons(30) Unarmed(31) MeleeWeapons(32) Throwing(33)
     * FirstAid(34) Doctor(35) Sneak(36) Lockpick(37) Steal(38) Traps(39)
     * Science(40) Repair(41) Speech(42) Barter(43) Gambling(44)
     * Outdoorsman(45). {@code Sneak=36} pins the {@code [frmids]} vector.
     */
    private static final int[] SKILL_FRM_IDS = {
        ArtDefs.SkillDexFrameId.SmallGuns.id(), // SmallGuns
        ArtDefs.SkillDexFrameId.BigGuns.id(), // BigGuns
        ArtDefs.SkillDexFrameId.EnergyWeapons.id(), // EnergyWeapons
        ArtDefs.SkillDexFrameId.Unarmed.id(), // Unarmed
        ArtDefs.SkillDexFrameId.MeleeWeapons.id(), // MeleeWeapons
        ArtDefs.SkillDexFrameId.Throwing.id(), // Throwing
        ArtDefs.SkillDexFrameId.FirstAid.id(), // FirstAid
        ArtDefs.SkillDexFrameId.Doctor.id(), // Doctor
        ArtDefs.SkillDexFrameId.Sneak.id(), // Sneak
        ArtDefs.SkillDexFrameId.Lockpick.id(), // Lockpick
        ArtDefs.SkillDexFrameId.Steal.id(), // Steal
        ArtDefs.SkillDexFrameId.Traps.id(), // Traps
        ArtDefs.SkillDexFrameId.Science.id(), // Science
        ArtDefs.SkillDexFrameId.Repair.id(), // Repair
        ArtDefs.SkillDexFrameId.Speech.id(), // Speech
        ArtDefs.SkillDexFrameId.Barter.id(), // Barter
        ArtDefs.SkillDexFrameId.Gambling.id(), // Gambling
        ArtDefs.SkillDexFrameId.Outdoorsman.id(), // Outdoorsman
    };

    /**
     * Tagged-state array, mirroring {@code gTaggedSkills} in
     * {@code src/skill.cc}. Zero-init (all ids 0 = SmallGuns).
     */
    private static final int[] TAGGED_SKILLS = new int[SkillDefs.NUM_TAGGED_SKILLS];

    /** Port of {@code skillMaximum} (sfall {@code set_skill_max}, default 300). */
    private static int skillMaximum = 300;

    /** Port of {@code tagSkillBonus} (default 20). */
    private static int tagSkillBonus = 20;

    /** Port of {@code tagPerkAppliesInitialBonus} (default false). */
    private static boolean tagPerkAppliesInitialBonus = false;

    /** Port of {@code tagSkillsDoublePointBonusDisabled} (default false). */
    private static boolean tagSkillsDoublePointBonusDisabled = false;

    /** Port of {@code SKILLS_MIN_RAW_POINTS} ({@code src/skill.cc}). */
    private static final int SKILLS_MIN_RAW_POINTS = -128;

    /** Port of {@code SKILLS_MIN_VALUE} ({@code src/skill.cc}). */
    private static final int SKILLS_MIN_VALUE = -999;

    /**
     * Port of {@code gDude}: null headless, so the dude block of
     * {@link #skillGetValue} is dormant (see class notes).
     */
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

    // Seams for the perk engine (unported): only reachable from the dormant
    // dude block.
    private static int perkGetRank(GameObject critter, int perk) {
        return 0;
    }

    private static int perkGetSkillModifier(GameObject critter, int skill) {
        return 0;
    }

    /** Port of {@code skillsGetCost(int)}. */
    public static int skillsGetCost(int skillValue) {
        if (skillValue >= 201) {
            return 6;
        } else if (skillValue >= 176) {
            return 5;
        } else if (skillValue >= 151) {
            return 4;
        } else if (skillValue >= 126) {
            return 3;
        } else if (skillValue >= 101) {
            return 2;
        } else {
            return 1;
        }
    }

    /** Port of {@code skillGetDefaultValue}: invalid skill yields -5. */
    public static int getDefaultValue(int skill) {
        return SkillDefs.skillIsValid(skill) ? DEFAULT_VALUES[skill] : -5;
    }

    /**
     * Port of {@code skillGetFrmId} ({@code src/skill.cc}): valid skill
     * yields the compile-time {@code frmId} frame id, invalid skill yields
     * {@code SkillDexFrameId::Invalid} ({@code -1}).
     */
    public static int skillGetFrmId(int skill) {
        return SkillDefs.skillIsValid(skill)
            ? SKILL_FRM_IDS[skill]
            : ArtDefs.SkillDexFrameId.Invalid.id();
    }

    /** Port of {@code skillIsTagged}. */
    public static boolean isTagged(int skill) {
        return skill == TAGGED_SKILLS[0]
            || skill == TAGGED_SKILLS[1]
            || skill == TAGGED_SKILLS[2]
            || skill == TAGGED_SKILLS[3];
    }

    /**
     * Port of {@code skillsSetTagged}: copies {@code count} slots verbatim.
     *
     * <p>NOTE: C loops {@code index < count} with no bounds check (count
     * &gt; 4 is UB, overflowing {@code gTaggedSkills}). Java clamps
     * defensively to {@code min(count, NUM_TAGGED_SKILLS, skills.length)};
     * the harness passes 2 and bulk callers pass 4, so both behave
     * identically on all in-range uses.
     */
    public static void setTagged(int[] skills, int count) {
        int n = Math.min(count, TAGGED_SKILLS.length);
        if (skills != null) {
            n = Math.min(n, skills.length);
        } else {
            return;
        }
        for (int index = 0; index < n; index++) {
            TAGGED_SKILLS[index] = skills[index];
        }
    }

    /**
     * Port of {@code skillsGetTagged}: reads {@code count} slots verbatim.
     *
     * <p>NOTE: same defensive clamp as {@link #setTagged}; C would
     * overflow on out-of-range counts.
     */
    public static void getTagged(int[] skills, int count) {
        int n = Math.min(count, TAGGED_SKILLS.length);
        if (skills != null) {
            n = Math.min(n, skills.length);
        } else {
            return;
        }
        for (int index = 0; index < n; index++) {
            skills[index] = TAGGED_SKILLS[index];
        }
    }

    /**
     * Port of {@code skillGetGameDifficultyModifier}. Reads
     * {@code Settings.settings.preferences.game_difficulty} live.
     */
    public static int getGameDifficultyModifier(int skill) {
        switch (skill) {
        case 6: // FirstAid
        case 7: // Doctor
        case 8: // Sneak
        case 9: // Lockpick
        case 10: // Steal
        case 11: // Traps
        case 12: // Science
        case 13: // Repair
        case 14: // Speech
        case 15: // Barter
        case 16: // Gambling
        case 17: { // Outdoorsman
            int gameDifficulty = Settings.settings.preferences.game_difficulty;
            if (gameDifficulty == Settings.GAME_DIFFICULTY_HARD) {
                return -10;
            } else if (gameDifficulty == Settings.GAME_DIFFICULTY_EASY) {
                return 20;
            }
            return 0;
        }
        default:
            return 0;
        }
    }

    /** Test seam: fresh-process mirror (tags, usage, maximum, tag flags). */
    static void resetForTests() {
        java.util.Arrays.fill(TAGGED_SKILLS, 0);
        for (int skill = 0; skill < SkillDefs.SKILL_COUNT; skill++) {
            java.util.Arrays.fill(TIMES_SKILL_USED[skill], 0);
        }
        skillMaximum = 300;
        tagSkillBonus = 20;
        tagPerkAppliesInitialBonus = false;
        tagSkillsDoublePointBonusDisabled = false;
    }

    /**
     * Port of {@code skillAddForce} (src/skill.cc 0x4AA7EC): raise a skill
     * of the player by one, straight into the proto. Not the player -5,
     * invalid skill -5, the value already at {@code skillMaximum} -3,
     * proto-miss -5, else the base in the proto goes up by one.
     */
    public static int skillAddForce(GameObject critter, int skill) {
        if (!isDude(critter)) {
            return -5;
        }

        if (!SkillDefs.skillIsValid(skill)) {
            return -5;
        }

        if (skillGetValue(critter, skill) >= skillMaximum) {
            return -3;
        }

        Proto[] slot = new Proto[1];
        if (ProtoDb.protoGetProto(ProtoId.ofGameObject(critter), slot) == -1) {
            return -5;
        }
        slot[0].critter.data.skills[skill] += 1;
        return 0;
    }

    /**
     * Port of {@code skillSubForce} (src/skill.cc 0x4AA9DC): lower a skill
     * of the player by one, straight into the proto. Not the player -5,
     * invalid skill -5, proto-miss -5, the base already at zero -2, else
     * the base in the proto goes down by one.
     */
    public static int skillSubForce(GameObject critter, int skill) {
        if (!isDude(critter)) {
            return -5;
        }

        if (!SkillDefs.skillIsValid(skill)) {
            return -5;
        }

        Proto[] slot = new Proto[1];
        if (ProtoDb.protoGetProto(ProtoId.ofGameObject(critter), slot) == -1) {
            return -5;
        }

        Proto proto = slot[0];
        if (proto.critter.data.skills[skill] <= 0) {
            return -2;
        }

        proto.critter.data.skills[skill] -= 1;
        return 0;
    }

    /**
     * Port of {@code skillGetValue} (src/skill.cc 0x4AA558): invalid skill
     * -5, non-critter 0, proto-miss -5, else
     * {@code default + base * mult + sum(primaryStat x multiplier)}, the
     * dormant dude block (tag doubling + tag bonus + trait + perk +
     * difficulty), the raw-negative adjustment (with the non-dude-gated
     * tagged doubling), truncation, the -999 floor for negative raws, and
     * the {@code skillMaximum} cap.
     */
    public static int skillGetValue(GameObject critter, int skill) {
        if (!SkillDefs.skillIsValid(skill)) {
            return -5;
        }

        if (critter == null
                || ProtoId.ofPid(critter.pid).objectType()
                        != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }

        Proto[] slot = new Proto[1];
        if (ProtoDb.protoGetProto(ProtoId.ofGameObject(critter), slot) == -1) {
            return -5;
        }
        Proto proto = slot[0];

        int baseValue = proto.critter.data.skills[skill];
        int rawSkillPoints = baseValue;
        if (baseValue < 0) {
            baseValue = 0;
        }

        double value = DEFAULT_VALUES[skill] + baseValue * BASE_VALUE_MULT[skill];
        for (int stat = StatDefs.STAT_FIRST; stat < StatDefs.PRIMARY_STAT_COUNT; stat++) {
            value += Stat.critterGetStat(critter, stat) * SKILL_STAT_MULTIPLIERS[skill][stat];
        }

        if (isDude(critter)) {
            if (isTagged(skill)) {
                if (!tagSkillsDoublePointBonusDisabled) {
                    value += baseValue * BASE_VALUE_MULT[skill];
                }

                if (tagPerkAppliesInitialBonus
                        || perkGetRank(critter, PerkDefs.Perk.PERK_TAG.id()) == 0
                        || skill != TAGGED_SKILLS[3]) {
                    value += tagSkillBonus;
                }
            }

            value += TraitDefs.traitGetSkillModifier(skill);
            value += perkGetSkillModifier(critter, skill);
            value += getGameDifficultyModifier(skill);
        }

        if (rawSkillPoints < 0) {
            if (rawSkillPoints < SKILLS_MIN_RAW_POINTS) {
                rawSkillPoints = SKILLS_MIN_RAW_POINTS;
            }

            rawSkillPoints *= BASE_VALUE_MULT[skill];
            if (isTagged(skill)) {
                rawSkillPoints *= 2;
            }

            value += rawSkillPoints;
        }

        int integerValue = (int) value;
        if (rawSkillPoints < 0 && integerValue < 0) {
            if (integerValue < SKILLS_MIN_VALUE) {
                integerValue = SKILLS_MIN_VALUE;
            }
        }

        if (integerValue > skillMaximum) {
            integerValue = skillMaximum;
        }

        return integerValue;
    }

    /**
     * Port of {@code skillRoll} (src/skill.cc 0x4AAAA4): invalid skill yields
     * {@code FAILURE} without touching RNG; otherwise
     * {@code randomRoll(skillGetValue + modifier, critChance, howMuch)},
     * where {@code critChance = critterGetStat(CRITICAL_CHANCE)} (clamped 0
     * headless) and {@code howMuch} follows the same {@code int[]} out-param
     * convention as {@link GameRandom#randomRoll} ({@code null} allowed).
     *
     * <p>Dude branches (dormant headless: the player is not there so
     * {@link #isDude} reads false with a null guard): the
     * {@code partyMemberGetBestInSkill} substitution (non-steal skills) and
     * the steal sneak {@code +30} bonus never fire. The party helpers live
     * in the unported party engine, so there is intentionally no call site
     * here — just the documented dormancy, like {@link #skillGetValue}.
     * The crit upgrade inside {@code randomRoll} takes the day-0 plain
     * path on both sides (harness day 0, Java default clock 0), so each leg
     * consumes exactly one d100.
     */
    public static GameRandom.Roll skillRoll(GameObject critter, int skill, int modifier, int[] howMuch) {
        if (!SkillDefs.skillIsValid(skill)) {
            return GameRandom.Roll.FAILURE;
        }

        int skillValue = skillGetValue(critter, skill);

        int criticalChance =
            Stat.critterGetStat(critter, StatDefs.Stat.STAT_CRITICAL_CHANCE.id());
        return GameRandom.randomRoll(skillValue + modifier, criticalChance, howMuch);
    }

    /**
     * Port of {@code skillSetMaximum}: out-of-range values restore the
     * vanilla ceiling (300), like sfall.
     */
    public static void skillSetMaximum(int maximum) {
        skillMaximum = maximum >= 0 && maximum <= 300 ? maximum : 300;
    }

    /** {@code SKILLS_MAX_USES_PER_DAY} ({@code src/skill.cc}): stamps per skill. */
    public static final int MAX_USES_PER_DAY = 3;

    /** {@code GAME_TIME_TICKS_PER_HOUR} ({@code src/scripts.h}). */
    private static final int GAME_TIME_TICKS_PER_HOUR = 36000;

    /**
     * {@code _timesSkillUsed} ({@code src/skill.cc}, BSS zeroed): last-use
     * game-time stamps, [skill][slot].
     */
    private static final int[][] TIMES_SKILL_USED =
        new int[SkillDefs.SKILL_COUNT][MAX_USES_PER_DAY];

    /**
     * Port of {@code skillGetFreeUsageSlot} (file-static in C): the first
     * zero-valued slot wins; when all slots are stamped, the oldest entry
     * (slot 0) decides: within 24 hours there is no free slot (-1), otherwise
     * the last slot is recycled.
     *
     * <p>NOTE: {@code skill_use_slot_clear} (inlined memset, unexported in
     * {@code src/skill.h}) is intentionally not ported; the
     * {@link #resetForTests} seam covers its fresh-state role.
     */
    private static int getFreeUsageSlot(int skill) {
        for (int slot = 0; slot < MAX_USES_PER_DAY; slot++) {
            if (TIMES_SKILL_USED[skill][slot] == 0) {
                return slot;
            }
        }

        int time = EventQueue.gameTimeGetTime();
        int hoursSinceLastUsage =
            (time - TIMES_SKILL_USED[skill][0]) / GAME_TIME_TICKS_PER_HOUR;
        if (hoursSinceLastUsage <= 24) {
            return -1;
        }

        return MAX_USES_PER_DAY - 1;
    }

    /**
     * Port of {@code skillUpdateLastUse}: stamps the free slot with
     * {@code gameTimeGetTime()}. A recycled (nonzero) last slot shifts the
     * older stamps down first. Returns 0, or -1 when no slot is free.
     */
    public static int updateLastUse(int skill) {
        int slot = getFreeUsageSlot(skill);
        if (slot == -1) {
            return -1;
        }

        if (TIMES_SKILL_USED[skill][slot] != 0) {
            for (int i = 0; i < slot; i++) {
                TIMES_SKILL_USED[skill][i] = TIMES_SKILL_USED[skill][i + 1];
            }
        }

        TIMES_SKILL_USED[skill][slot] = EventQueue.gameTimeGetTime();

        return 0;
    }

    /**
     * Port of {@code skillsUsageSave}: writes the table as
     * {@code SKILL_COUNT * SKILLS_MAX_USES_PER_DAY} (54) big-endian int32s in
     * row-major order (skill-major, slot-minor), matching the C
     * {@code (int*)_timesSkillUsed} cast.
     */
    public static int skillsUsageSave(XFile.Handle stream) {
        int count = SkillDefs.SKILL_COUNT * MAX_USES_PER_DAY;
        int[] flat = new int[count];
        for (int skill = 0; skill < SkillDefs.SKILL_COUNT; skill++) {
            for (int slot = 0; slot < MAX_USES_PER_DAY; slot++) {
                flat[skill * MAX_USES_PER_DAY + slot] = TIMES_SKILL_USED[skill][slot];
            }
        }
        return Db.fileWriteInt32List(stream, flat, 0, count);
    }

    /** Port of {@code skillsUsageLoad}: reads back {@link #skillsUsageSave}. */
    public static int skillsUsageLoad(XFile.Handle stream) {
        int count = SkillDefs.SKILL_COUNT * MAX_USES_PER_DAY;
        int[] flat = new int[count];
        if (Db.fileReadInt32List(stream, flat, 0, count) == -1) {
            return -1;
        }
        for (int skill = 0; skill < SkillDefs.SKILL_COUNT; skill++) {
            for (int slot = 0; slot < MAX_USES_PER_DAY; slot++) {
                TIMES_SKILL_USED[skill][slot] = flat[skill * MAX_USES_PER_DAY + slot];
            }
        }
        return 0;
    }
}
