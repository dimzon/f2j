package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjectSystem.Script;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [scriptattach]} vectors from
 * {@code docker/refharness/golden/v151.txt}, produced by
 * {@code sectionScriptAttach} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The script registry is the game's own state: creating an object runs
 * the script of its proto ({@code objectSetScriptFromProto}) and the
 * context builtins read the script's fields through the program
 * ({@code scriptGetSid} then {@code scriptGetScript}). A proto without a
 * script leaves the object's sid -1, and a program without a script still
 * answers null/0 (the {@code [scriptrun]} state).
 */
class GameScriptRegistryTest {

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

    @Test
    void aProtoWithoutAScriptLeavesTheObjectBare() {
        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertEquals(-1, dogSlot[0].sid);

        Script[] slot = new Script[1];
        assertEquals(-1, ObjectSystem.scriptGetScript(-1, slot));
        assertNull(slot[0]);
    }

    @Test
    void aProtoWithAScriptAttachesItToTheObject() {
        // pid 16777221 carries sid 67108883 (type 4, index 19) in the game
        // data; the probe that found it is ProbeScriptProtoTest (temporary).
        GameObject[] critterSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(critterSlot, ProtoId.ofPid(16777221)));
        GameObject critter = critterSlot[0];
        assertTrue(critter.sid != -1, "proto script sid not attached");

        Script[] slot = new Script[1];
        assertEquals(0, ObjectSystem.scriptGetScript(critter.sid, slot));
        Script script = slot[0];
        assertSame(critter, script.owner);
        assertEquals(critter.id, script.ownerId);
        // The attached script is a fresh registry instance; its index is the
        // proto's script index (19), carried from sid 67108883.
        assertEquals(19, script.index);
        assertEquals(19, critter.scriptIndex);
    }

    @Test
    void theContextReadsGoThroughTheProgram() throws Exception {
        GameObject[] critterSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(critterSlot, ProtoId.ofPid(16777221)));
        GameObject critter = critterSlot[0];

        Script[] slot = new Script[1];
        assertEquals(0, ObjectSystem.scriptGetScript(critter.sid, slot));
        Script script = slot[0];

        Program program = Program.programCreateByPath("scripts\\acdog.int");
        assertNotNull(program);
        script.program = program;

        assertEquals(script.sid, ObjectSystem.scriptGetSid(program));
        assertSame(critter, ObjectSystem.scriptGetSelf(program));

        script.target = critter;
        script.source = null;
        script.fixedParam = 42;
        script.actionBeingUsed = 7;
        assertSame(critter, ObjectSystem.scriptGetTarget(program));
        assertNull(ObjectSystem.scriptGetSource(program));
        assertEquals(42, ObjectSystem.scriptGetFixedParam(program));
        assertEquals(7, ObjectSystem.scriptGetActionBeingUsed(program));

        ObjectSystem.scriptSetOverrides(program);
        assertEquals(1, script.scriptOverrides);

        Program.programFree(program);
    }

    @Test
    void aProgramWithoutAScriptStillAnswersTheBareState() {
        assertNull(api.selfObj());
        assertNull(api.targetObj());
        assertNull(api.sourceObj());
        assertEquals(0, api.fixedParam());
        assertEquals(-1, api.actionBeingUsed());
        assertEquals(0, api.scriptAction());
        api.scriptOverrides();
    }
}
