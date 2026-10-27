package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * [combatendattempt], golden/v173.txt: the end-of-combat gate
 * ({@code combatAttemptEnd}) consulting the AI want-to-stop/want-to-join
 * decisions. Active enemies with a living target refuse (no exit); dead or
 * absent enemies agree (exit requested).
 */
class GameCombatEndAttemptTest {

    private static final int A = 38101;

    private GameObject dude;
    private GameObject h1;
    private GameObject d1;
    private GameObject d2;
    private GameObject[] roster;
    private GameObject[] combatList;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        ContentConfig.resetForTests();
        Message.resetForTests();
        Item.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        Map.resetForTests();
        Stat.resetForTests();
        Skill.resetForTests();
        TraitDefs.resetForTests();
        GameRandom.resetProviders();
        Combat.resetForTests();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
        CombatAi.resetForTests();
        Critter.sneakWorking = false;

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
        byte[] tileBuf = new byte[640 * 480];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf,
                640, 480, 640, rect -> {
                }));
        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, Stat.statsInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));
        assertEquals(0, CombatAi.aiInit());

        seedProtoBonus(16777376, StatDefs.Stat.STAT_ARMOR_CLASS.id(), 5);
        seedProtoBonus(16777558, StatDefs.Stat.STAT_MELEE_DAMAGE.id(), 13);
        seedProtoBonus(16777558, StatDefs.Stat.STAT_ARMOR_CLASS.id(), 50);

        dude = ObjectSystem.getDude();
        assertNotNull(dude);
        h1 = create(16777376);
        d1 = create(16777558);
        d2 = create(16777558);
        roster = new GameObject[] { dude, h1, d1, d2 };

        int[] distances = { 10, 0, 4, 8 };
        for (int index = 0; index < 4; index++) {
            GameObject obj = roster[index];
            obj.cid = index;
            obj.data.critter.combat.team = index >= 2 ? 2 : 1;
            obj.data.critter.combat.results = ObjTypes.Dam.NONE;
            obj.data.critter.combat.whoHitMe = null;
            obj.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
            obj.data.critter.combat.ap = 5;
            obj.data.critter.combat.damageLastTurn = 0;
            obj.data.critter.hp = 50;
            obj.rotation = ObjTypes.Rotation.ROTATION_SE;
            assertEquals(0, ObjectSystem.objectSetLocation(obj,
                    TileMath.tileGetTileInDirection(A, Rotation.SE, distances[index]), 0));
        }
        dude.elevation = 0;

        Animation.animationReset();
        CombatAi.combatAiBegin(roster, 4);
        Combat.aiInfoList = new Combat.CombatAiInfo[4];
        for (int index = 0; index < 4; index++) {
            Combat.aiInfoList[index] = new Combat.CombatAiInfo();
        }

        combatList = new GameObject[] { dude, h1, d1, d2 };
        Combat.combatList = combatList;
        Combat.combatElev = 0;
    }

    @AfterEach
    void tearDown() {
        Combat.combatList = null;
        GameRandom.resetProviders();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
        CombatAi.combatAiOver();
    }

    private static GameObject create(int pid) {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoTypes.ProtoId.ofPid(pid)));
        return slot[0];
    }

    private static void seedProtoBonus(int pid, int stat, int value) {
        ProtoTypes.Proto[] slot = new ProtoTypes.Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoTypes.ProtoId.ofPid(pid), slot));
        slot[0].critter.data.bonusStats[stat] = value;
    }

    @Test
    void combatEndAttemptVectors() {
        Combat.listCom = 2;
        Combat.listNoncom = 2;

        // Active enemies with a living target refuse the end.
        d1.data.critter.combat.whoHitMe = h1;
        d2.data.critter.combat.whoHitMe = h1;
        assertEquals(0, Combat.combatAttemptEnd());
        assertEquals(0, Combat.combatState & Combat.COMBAT_STATE_EXIT_REQUESTED);

        // Dead enemies agree - exit requested.
        d1.data.critter.combat.results = ObjTypes.Dam.DEAD;
        d2.data.critter.combat.results = ObjTypes.Dam.DEAD;
        assertEquals(1, Combat.combatAttemptEnd());
        assertTrue((Combat.combatState & Combat.COMBAT_STATE_EXIT_REQUESTED) != 0);
        Combat.combatState &= ~Combat.COMBAT_STATE_EXIT_REQUESTED;

        // No enemies: clean slate agrees.
        d1.data.critter.combat.results = ObjTypes.Dam.NONE;
        d2.data.critter.combat.results = ObjTypes.Dam.NONE;
        d1.data.critter.combat.whoHitMe = null;
        d2.data.critter.combat.whoHitMe = null;
        Combat.listCom = 1;
        Combat.listNoncom = 3;
        assertEquals(1, Combat.combatAttemptEnd());
        assertTrue((Combat.combatState & Combat.COMBAT_STATE_EXIT_REQUESTED) != 0);
    }
}
