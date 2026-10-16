package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill result codes, mirroring the pure parts of {@code src/skill.h}.
 * The skill engine ({@code skill.cc}) comes with the game phase.
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
     * Tagged-state array, mirroring {@code gTaggedSkills} in
     * {@code src/skill.cc}. Zero-init (all ids 0 = SmallGuns).
     */
    private static final int[] TAGGED_SKILLS = new int[SkillDefs.NUM_TAGGED_SKILLS];

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

    /** Port of {@code skillIsTagged}. */
    public static boolean isTagged(int skill) {
        return skill == TAGGED_SKILLS[0]
            || skill == TAGGED_SKILLS[1]
            || skill == TAGGED_SKILLS[2]
            || skill == TAGGED_SKILLS[3];
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

    /** Test seam: zero the tagged-state array (fresh-process mirror). */
    static void resetForTests() {
        java.util.Arrays.fill(TAGGED_SKILLS, 0);
        for (int skill = 0; skill < SkillDefs.SKILL_COUNT; skill++) {
            java.util.Arrays.fill(TIMES_SKILL_USED[skill], 0);
        }
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
