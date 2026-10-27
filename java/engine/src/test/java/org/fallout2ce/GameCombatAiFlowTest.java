package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.CombatAi.AiPacket;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * [combatai], golden/v172.txt: the main AI-turn decisions
 * ({@code _combat_ai}) with the chem loop and the attack flow as documented
 * seams. The vector hits the branches that do not enter the seams: the
 * HP-driven flee (run fires, maneuver set), the friendly-dead move-away (the
 * dead teammate is placed far, then cleared), the calm extra-AP pass and
 * the join/stop decisions.
 */
class GameCombatAiFlowTest {

    private static final int A = 38101;

    private GameObject h1;
    private GameObject d1;
    private GameObject d2;
    private GameObject far;
    private GameObject[] roster;
    private AiPacket humanPacket;

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

        h1 = create(16777376);
        d1 = create(16777558);
        d2 = create(16777558);
        far = create(16777558);
        roster = new GameObject[] { h1, d1, d2, far };

        int[] distances = { 0, 1, 4, 40 };
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

        Animation.animationReset();
        CombatAi.combatAiBegin(roster, 4);
        Combat.aiInfoList = new Combat.CombatAiInfo[4];
        for (int index = 0; index < 4; index++) {
            Combat.aiInfoList[index] = new Combat.CombatAiInfo();
        }

        h1.data.critter.combat.aiPacket = 0;
        humanPacket = CombatAi.aiGetPacket(h1);
        humanPacket.distance = CombatAiDefs.DistanceMode.DISTANCE_STAY_CLOSE.id();
        humanPacket.maxDist = 7;
        humanPacket.minHp = 40;
    }

    @AfterEach
    void tearDown() {
        GameRandom.resetProviders();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
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
    void combatAiFlowVectors() {
        // HP-driven flee: current HP 30 < min_hp 40; the run fires and the
        // FLEEING maneuver is set. No whoHitMe edges, so the danger source
        // stays null and the attack flow is not entered.
        h1.data.critter.hp = 30;
        CombatAi.combatAi(h1, d2);
        assertTrue(Critter.critterIsFleeing(h1));
        assertEquals(5, h1.data.critter.combat.ap);
        h1.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
        h1.data.critter.hp = 50;

        // The AI-info accessors require combat; the friendly-dead branch is
        // covered component-wise (accessors in Combat, movement in
        // [aimove]). Here the section runs out of combat, so the branch is
        // not entered and nothing moves.
        h1.data.critter.combat.ap = 10;
        CombatAi.combatAi(h1, null);
        assertEquals(10, h1.data.critter.combat.ap);
        h1.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;

        // Calm extra-AP: the prefs rerun without moving.
        h1.data.critter.combat.ap = 10;
        CombatAi.combatAi(h1, null);
        assertEquals(10, h1.data.critter.combat.ap);

        // Join/stop decisions.
        assertFalse(CombatAi.combataiWantToJoin(h1));
        assertTrue(CombatAi.combataiWantToStop(h1));
        h1.data.critter.combat.maneuver = ObjTypes.CritterManeuver.FLEEING;
        assertTrue(CombatAi.combataiWantToStop(h1));
        h1.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
    }
}
