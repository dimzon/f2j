package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjectSystem.Script;

/**
 * [aimtick], golden/v175.txt: the scripts tick - the critter round-robin
 * cursor ({@code _script_chk_critters}) and the TIMED queue path
 * ({@code scriptAddTimerEvent}/{@code scriptEventProcess}/{@code scriptRemoveTimerEvents}).
 * The scripts are program-less (index=-1), so the phases are pure mechanics:
 * cursor rotation, queue submission, TIMED fixed-param handling and removal
 * by sid.
 */
class GameAiTickTest {

    private static final int A = 38101;

    private GameObject h1;
    private GameObject d1;
    private int sidA;
    private int sidB;
    private int sidT1;
    private int sidT2;

    @BeforeEach
    void setup() {
        Game.resetForTests();
        ScriptsState.resetTickForTests();

        Settings.settings.system.language = Message.ENGLISH;
        String root = System.getenv("F2J_GAMEDATA");
        assertNotNull(root);
        assertEquals(0, Db.dbOpen(Paths.get(root, "master.dat").toString(),
                Paths.get(root, "patch000.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "master.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "patch000.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "critter.dat").toString()));
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());
        assertEquals(0, Mouse.mouseInit());
        byte[] tileBuf = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf,
                64, 64, 64, rect -> {
                }));
        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, Stat.statsInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));
        assertEquals(0, CombatAi.aiInit());
        assertEquals(0, Game.gameInit());

        ObjectSystem.scriptsEnabled = true;

        h1 = create(16777376);
        d1 = create(16777558);
        h1.cid = 0;
        d1.cid = 1;
        h1.data.critter.combat.team = 1;
        d1.data.critter.combat.team = 1;
        h1.data.critter.combat.ap = 5;
        d1.data.critter.combat.ap = 5;
        h1.data.critter.hp = 50;
        d1.data.critter.hp = 50;
        assertEquals(0, ObjectSystem.objectSetLocation(h1, A, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(d1,
                TileMath.tileGetTileInDirection(A, Rotation.SE, 2), 0));

        CombatAi.combatAiBegin(new GameObject[] { h1, d1 }, 2);
        Combat.aiInfoList = new Combat.CombatAiInfo[2];
        for (int index = 0; index < 2; index++) {
            Combat.aiInfoList[index] = new Combat.CombatAiInfo();
        }

        // The two critter-phase scripts (program-less, index=-1).
        ObjectSystem.scriptsOfType(ObjectSystem.SCRIPT_TYPE_CRITTER).clear();
        int[] sidSlot = new int[1];
        assertEquals(0, ObjectSystem.scriptAdd(sidSlot, ObjectSystem.SCRIPT_TYPE_CRITTER));
        sidA = sidSlot[0];
        assertEquals(0, ObjectSystem.scriptAdd(sidSlot, ObjectSystem.SCRIPT_TYPE_CRITTER));
        sidB = sidSlot[0];

        Script[] slotA = new Script[1];
        Script[] slotB = new Script[1];
        assertEquals(0, ObjectSystem.scriptGetScript(sidA, slotA));
        assertEquals(0, ObjectSystem.scriptGetScript(sidB, slotB));
        slotA[0].index = -1;
        slotB[0].index = -1;
        slotA[0].owner = h1;
        slotA[0].ownerId = h1.id;
        slotB[0].owner = d1;
        slotB[0].ownerId = d1.id;
        h1.sid = sidA;
        d1.sid = sidB;

    }

    private void addTimerScripts() {
        int[] sidSlot = new int[1];
        assertEquals(0, ObjectSystem.scriptAdd(sidSlot, ObjectSystem.SCRIPT_TYPE_CRITTER));
        sidT1 = sidSlot[0];
        assertEquals(0, ObjectSystem.scriptAdd(sidSlot, ObjectSystem.SCRIPT_TYPE_CRITTER));
        sidT2 = sidSlot[0];
        Script[] t1 = new Script[1];
        Script[] t2 = new Script[1];
        assertEquals(0, ObjectSystem.scriptGetScript(sidT1, t1));
        assertEquals(0, ObjectSystem.scriptGetScript(sidT2, t2));
        t1[0].index = -1;
        t2[0].index = -1;
        t1[0].owner = h1;
        t1[0].ownerId = h1.id;
        t2[0].owner = d1;
        t2[0].ownerId = d1.id;
    }

    @AfterEach
    void tearDown() {
        ObjectSystem.scriptsEnabled = false;
        GameRandom.resetProviders();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
        Game.resetForTests();
    }

    private static GameObject create(int pid) {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoTypes.ProtoId.ofPid(pid)));
        return slot[0];
    }

    @Test
    void aiTickVectors() {
        // Critter roster: exactly two scripts.
        assertEquals(0, ObjectSystem.scriptsOfType(ObjectSystem.SCRIPT_TYPE_CRITTER).size() - 2);

        // Three ticks: cursor rotates 1, 0, 1 over two scripts.
        ScriptsState.scriptChkCritters();
        assertEquals(1, ScriptsState.critterProcessingIndexForTests());
        ScriptsState.scriptChkCritters();
        assertEquals(0, ScriptsState.critterProcessingIndexForTests());
        ScriptsState.scriptChkCritters();
        assertEquals(1, ScriptsState.critterProcessingIndexForTests());

        // Timed events: schedule on fresh timer scripts.
        addTimerScripts();
        assertEquals(0, ScriptsState.scriptAddTimerEvent(sidT1, 0, 42));
        assertEquals(0, ScriptsState.scriptAddTimerEvent(sidT2, 0, 7));
        assertFalse(EventQueue.queueIsEmpty());

        // Drain through the scripts tick; the due TIMED handlers set the
        // fixed params.
        ScriptsState.pretendQueueGateElapsed();
        ScriptsState.scriptChkTimedEvents();
        ScriptsState.scriptChkTimedEvents();
        assertEquals(42, ScriptsState.scriptFixedParamForTests(sidT1));
        assertEquals(7, ScriptsState.scriptFixedParamForTests(sidT2));

        // scriptRemoveTimerEvents drops only the sid's matching event.
        assertEquals(0, ScriptsState.scriptAddTimerEvent(sidT1, 0, 1));
        assertEquals(0, ScriptsState.scriptAddTimerEvent(sidT2, 0, 2));
        assertEquals(0, ScriptsState.scriptRemoveTimerEvents(sidT1, 1));
        // sidT2's event remains.
        assertFalse(EventQueue.queueIsEmpty());
        ScriptsState.pretendQueueGateElapsed();
        ScriptsState.scriptChkTimedEvents();
    }
}
