package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The roll and the experience builtins of the game.
 *
 * <p>A critter that is not there has no roll at all, and the original
 * answers a critical failure for it. The experience goes to the player,
 * and the port has no player yet. The setup mirrors the harness flow of
 * {@link InventoryTest}.
 */
class GameRollVsSkillTest {

    private GameScriptApi api;

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path p = Paths.get(root, name);
        if (!Files.isRegularFile(p)) {
            throw new IllegalStateException("missing " + p);
        }
        return p.toString();
    }

    @BeforeEach
    void setup() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        api = new GameScriptApi();
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
    }

    @Test
    void theRollOfASkill() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject dog = dogSlot[0];

        // The roll is one of the four the game knows.
        int roll = api.rollVsSkill(dog, 0, 0);
        assertTrue(roll >= 0 && roll <= 3);

        // A critter that is not there has no roll at all.
        assertEquals(GameRandom.Roll.CRITICAL_FAILURE.ordinal(), api.rollVsSkill(null, 0, 0));
    }

    @Test
    void theExperienceGoesNowhere() {
        api.giveExpPoints(100);
    }
}
