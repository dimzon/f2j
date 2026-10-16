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
    }
}
