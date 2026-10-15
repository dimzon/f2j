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
}
