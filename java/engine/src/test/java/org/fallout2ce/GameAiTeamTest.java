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
 * [aimteam], golden/v174.txt: the team-combat helpers ({@code _caiSetupTeamCombat},
 * {@code _caiTeamCombatInit}, {@code _caiTeamCombatExit}) - the engage mark
 * on every elevation critter, the whoHitMe wiring by nearest same-team
 * search, and the guards.
 */
class GameAiTeamTest {

    private static final int A = 38101;

    private GameObject dude;
    private GameObject h1;
    private GameObject d1;
    private GameObject d2;
    private GameObject far;
    private GameObject[] roster;

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
    }

    @AfterEach
    void tearDown() {
        CombatAi.caiTeamCombatExit();
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
    void aiTeamVectors() {
        assertEquals(0, CombatAi.caiSetupTeamCombat(h1, d2));

        // Setup marks every elevation critter (except the dude) as engaging.
        assertEquals(1, (h1.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.ENGAGING.bits()) != 0 ? 1 : 0);
        assertEquals(1, (d1.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.ENGAGING.bits()) != 0 ? 1 : 0);
        assertEquals(1, (d2.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.ENGAGING.bits()) != 0 ? 1 : 0);
        assertEquals(1, (far.data.critter.combat.maneuver.bits()
                & ObjTypes.CritterManeuver.ENGAGING.bits()) != 0 ? 1 : 0);

        assertEquals(0, CombatAi.caiTeamCombatInit(roster, 4));
        // Team 1 members point at the nearest team-2 member (d2, cid 2).
        assertEquals(d2, h1.data.critter.combat.whoHitMe);
        assertEquals(d2, d1.data.critter.combat.whoHitMe);
        // Team 2 members point at the nearest team-1 member (d1, cid 1).
        assertEquals(d1, d2.data.critter.combat.whoHitMe);
        assertEquals(d1, far.data.critter.combat.whoHitMe);

        // Guards.
        assertEquals(-1, CombatAi.caiTeamCombatInit(null, 2));
        assertEquals(0, CombatAi.caiTeamCombatInit(roster, 0));

        // After exit, no setup -> init is a no-op.
        CombatAi.caiTeamCombatExit();
        h1.data.critter.combat.whoHitMe = null;
        d1.data.critter.combat.whoHitMe = null;
        assertEquals(0, CombatAi.caiTeamCombatInit(roster, 4));
        assertEquals(null, h1.data.critter.combat.whoHitMe);
        assertEquals(null, d1.data.critter.combat.whoHitMe);
    }
}
