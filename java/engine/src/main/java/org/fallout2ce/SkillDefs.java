package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill id table, mirroring {@code src/skill_defs.h}.
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 */
public final class SkillDefs {

    public static final int NUM_TAGGED_SKILLS = 4;
    public static final int DEFAULT_TAGGED_SKILLS = 3;

    public static final int SKILL_COUNT = 18;
    public static final int SKILL_FIRST = 0;

    private SkillDefs() {
    }

    public enum Skill {
        SKILL_INVALID(-1),
        SKILL_SMALL_GUNS(0),
        SKILL_BIG_GUNS(1),
        SKILL_ENERGY_WEAPONS(2),
        SKILL_UNARMED(3),
        SKILL_MELEE_WEAPONS(4),
        SKILL_THROWING(5),
        SKILL_FIRST_AID(6),
        SKILL_DOCTOR(7),
        SKILL_SNEAK(8),
        SKILL_LOCKPICK(9),
        SKILL_STEAL(10),
        SKILL_TRAPS(11),
        SKILL_SCIENCE(12),
        SKILL_REPAIR(13),
        SKILL_SPEECH(14),
        SKILL_BARTER(15),
        SKILL_GAMBLING(16),
        SKILL_OUTDOORSMAN(17);

        private final int id;

        Skill(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public Skill next() {
            return SkillDefs.of(id + 1);
        }
    }

    private static final Map<Integer, Skill> BY_ID = new HashMap<>();

    static {
        for (Skill skill : Skill.values()) {
            BY_ID.put(skill.id, skill);
        }
    }

    public static Skill of(int id) {
        return BY_ID.get(id);
    }

    public static boolean skillIsValid(int skill) {
        return skill >= SKILL_FIRST && skill < SKILL_COUNT;
    }
}
