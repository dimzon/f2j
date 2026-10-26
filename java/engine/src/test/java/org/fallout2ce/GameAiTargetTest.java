package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.CombatAi.AiPacket;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * [aitarget], golden/v167.txt: the AI target-selection slice — the roster
 * sorts ({@code _ai_sort_list_distance/strength/weakness}), nearest-team
 * scans ({@code _ai_find_nearest_team[_in_combat]}), attacker categories
 * ({@code aiFindAttackers}), the danger source decision tree
 * ({@code _ai_danger_source}) and perception
 * ({@code isWithinPerceptionDetailed} with the sneak adjustments), pinned
 * against the real C++ sectionAiTarget in
 * {@code docker/refharness/ref_main.cc}.
 *
 * <p>The harness drives {@code isInCombat} through gCombatState and points
 * the dude's tile field directly (objectSetLocation(gDude) segfaults
 * headless); the roster, the cid assignment, the AI info list and the hp
 * seeding (fresh critters read as dead otherwise) mirror the section.
 */
class GameAiTargetTest {

    private GameObject dude;
    private GameObject h1;
    private GameObject d1;
    private GameObject d2;
    private GameObject d3;
    private GameObject d4;
    private GameObject d5;
    private GameObject[] roster;
    private AiPacket dudePacket;
    private int savedAttackWho;
    private int savedDisposition;
    private int savedDistance;
    private Proto dudeProto;

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

        // The C++ harness process accumulated proto mutations from earlier
        // sections ([ai] set the dog AC bonus to 50, [equip]-era slices left
        // melee/armor bonuses on the shared protos); mirror them so the
        // ratings match (dog 70, d3 17, d4 14, h1 10, dude 10).
        seedProtoBonus(16777558, StatDefs.Stat.STAT_MELEE_DAMAGE.id(), 13);
        seedProtoBonus(16777558, StatDefs.Stat.STAT_ARMOR_CLASS.id(), 50);
        seedProtoBonus(16777249, StatDefs.Stat.STAT_ARMOR_CLASS.id(), 8);
        seedProtoBonus(16777252, StatDefs.Stat.STAT_ARMOR_CLASS.id(), 8);
        seedProtoBonus(16777376, StatDefs.Stat.STAT_ARMOR_CLASS.id(), 5);

        dude = ObjectSystem.getDude();
        assertNotNull(dude);
        h1 = create(16777376);
        d1 = create(16777558);
        d2 = create(16777558);
        d3 = create(16777249);
        d4 = create(16777252);
        d5 = create(16777558);
        roster = new GameObject[] { dude, h1, d1, d2, d3, d4, d5 };
        dudePacket = CombatAi.aiGetPacket(dude);
        savedAttackWho = dudePacket.attackWho;
        savedDisposition = dudePacket.disposition;
        savedDistance = dudePacket.distance;
    }

    @AfterEach
    void tearDown() {
        dudePacket.attackWho = savedAttackWho;
        dudePacket.disposition = savedDisposition;
        dudePacket.distance = savedDistance;
        GameRandom.resetProviders();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
    }

    private static GameObject create(int pid) {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoId.ofPid(pid)));
        return slot[0];
    }

    private static void seedProtoBonus(int pid, int stat, int value) {
        Proto[] slot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofPid(pid), slot));
        slot[0].critter.data.bonusStats[stat] = value;
    }

    private void place(GameObject obj, int distance) {
        int a = 24101;
        assertEquals(0, ObjectSystem.objectSetLocation(obj,
                TileMath.tileGetTileInDirection(a, Rotation.SE, distance), 0));
    }

    private static int rating(GameObject obj) {
        return CombatAi.combataiRating(obj);
    }

    private static void assertOrder(GameObject[] list, int length, GameObject... expected) {
        assertEquals(length, expected.length);
        for (int index = 0; index < length; index++) {
            assertEquals(expected[index], list[index], "slot " + index);
        }
    }

    @Test
    void aiTargetVectors() {
        // Roster layout: distances from d1 are d2=1, h1=2, d3=3, d4=4,
        // dude=10, d5=30; d5 looks away so the far perception cases fall to
        // hearing. cid assignment and hp seeding mirror the C++ section
        // (fresh critters read as dead for the AI info guards otherwise).
        int[] distances = { 10, 2, 0, 1, 3, 4, 30 };
        for (int index = 0; index < 7; index++) {
            GameObject obj = roster[index];
            obj.cid = index;
            obj.data.critter.combat.team = index == 0 ? 0 : index <= 3 ? 1 : 2;
            obj.data.critter.combat.results = ObjTypes.Dam.NONE;
            obj.data.critter.combat.whoHitMe = null;
            obj.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
            obj.data.critter.hp = 50;
            obj.rotation = ObjTypes.Rotation.ROTATION_SE;
            if (index != 0) {
                place(obj, distances[index]);
            }
        }
        dude.tile = TileMath.tileGetTileInDirection(24101, Rotation.SE, distances[0]);
        dude.elevation = 0;
        d5.rotation = ObjTypes.Rotation.ROTATION_NW;

        CombatAi.combatAiBegin(roster, 7);

        // AI info list mirrors the _combat_begin allocation.
        Combat.aiInfoList = new Combat.CombatAiInfo[7];
        for (int index = 0; index < 7; index++) {
            Combat.aiInfoList[index] = new Combat.CombatAiInfo();
        }

        int savedCombatState = Combat.combatState;
        Combat.combatState |= Combat.COMBAT_STATE_IN_COMBAT | Combat.COMBAT_STATE_PLAYER_TURN;

        try {
            // Ratings: distinct species separate the strength sorts.
            assertEquals(10, rating(dude));
            assertEquals(10, rating(h1));
            assertEquals(70, rating(d1));
            assertEquals(70, rating(d2));
            assertEquals(17, rating(d3));
            assertEquals(14, rating(d4));
            assertEquals(70, rating(d5));

            GameObject[] list = { d3, null, d2, d4 };
            CombatAi.aiSortListDistance(list, 4, d1);
            assertOrder(list, 4, d2, d3, d4, null);

            list = new GameObject[] { d2, d3, d4, null };
            CombatAi.aiSortListStrength(list, 3);
            assertOrder(list, 4, d4, d3, d2, null);
            CombatAi.aiSortListWeakness(list, 3);
            assertOrder(list, 4, d2, d3, d4, null);

            // Nearest-team scans: same team (0x01) and other team (0x02),
            // skipping dead critters and the actor itself.
            assertEquals(d2, CombatAi.aiFindNearestTeam(d1, d1, 1));
            d2.data.critter.combat.results = ObjTypes.Dam.DEAD;
            assertEquals(h1, CombatAi.aiFindNearestTeam(d1, d1, 1));
            d2.data.critter.combat.results = ObjTypes.Dam.NONE;
            assertEquals(d3, CombatAi.aiFindNearestTeam(d1, d1, 2));
            d3.data.critter.combat.results = ObjTypes.Dam.DEAD;
            assertEquals(d4, CombatAi.aiFindNearestTeam(d1, d1, 2));
            d3.data.critter.combat.results = ObjTypes.Dam.NONE;

            // In-combat variant additionally requires whoHitMe.
            assertEquals(null, CombatAi.aiFindNearestTeamInCombat(d1, d1, 1));
            d2.data.critter.combat.whoHitMe = d1;
            assertEquals(d2, CombatAi.aiFindNearestTeamInCombat(d1, d1, 1));
            d2.data.critter.combat.whoHitMe = null;
            d3.data.critter.combat.whoHitMe = d1;
            assertEquals(d3, CombatAi.aiFindNearestTeamInCombat(d1, d1, 2));
            d3.data.critter.combat.whoHitMe = null;

            // find-attackers: slot 0 is preserved, slots 1-3 fill from
            // whoHitMe relations.
            GameObject[] targets = { dude, null, null, null };
            d2.data.critter.combat.whoHitMe = d1;
            h1.data.critter.combat.whoHitMe = d3;
            d4.data.critter.combat.whoHitMe = h1;
            CombatAi.aiFindAttackers(d1, targets);
            assertOrder(targets, 4, dude, d2, d3, d4);
            d2.data.critter.combat.whoHitMe = null;
            h1.data.critter.combat.whoHitMe = null;
            d4.data.critter.combat.whoHitMe = null;
            CombatAi.aiFindAttackers(d1, targets);
            assertOrder(targets, 4, dude, null, null, null);

            // Perception: visible branch, glass halving, hearing in combat
            // and the dude sneak adjustments. The C++ harness inherits
            // _sneak_working = true from earlier sections.
            assertEquals(1, CombatAi.isWithinPerceptionDetailed(h1, d2,
                    CombatAiDefs.PerceptionType.PERCEPTION_AI_TARGET.id()));
            d5.flags = d5.flags.or(ObjTypes.ObjectFlags.TRANS_GLASS);
            assertEquals(0, CombatAi.isWithinPerceptionDetailed(h1, d5,
                    CombatAiDefs.PerceptionType.PERCEPTION_AI_TARGET.id()));
            d5.flags = d5.flags.and(ObjTypes.ObjectFlags.TRANS_GLASS.not());
            assertEquals(1, CombatAi.isWithinPerceptionDetailed(h1, d5,
                    CombatAiDefs.PerceptionType.PERCEPTION_AI_TARGET.id()));

            dude.tile = TileMath.tileGetTileInDirection(24101, Rotation.SE, 13);
            assertEquals(1, CombatAi.isWithinPerceptionDetailed(h1, dude,
                    CombatAiDefs.PerceptionType.PERCEPTION_AI_TARGET.id()));
            Proto[] dudeProtoSlot = new Proto[1];
            assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(dude), dudeProtoSlot));
            dudeProto = dudeProtoSlot[0];
            dudeProto.critter.data.flags = dudeProto.critter.data.flags
                    .or(ObjTypes.CritterFlags.DUDE_SNEAKING);
            Critter.sneakWorking = true;
            assertEquals(0, CombatAi.isWithinPerceptionDetailed(h1, dude,
                    CombatAiDefs.PerceptionType.PERCEPTION_AI_TARGET.id()));
            dudeProto.critter.data.flags = dudeProto.critter.data.flags
                    .and(ObjTypes.CritterFlags.DUDE_SNEAKING.not());
            Critter.sneakWorking = false;
            int quietState = Combat.combatState & ~Combat.COMBAT_STATE_IN_COMBAT;
            Combat.combatState = quietState;
            assertEquals(1, CombatAi.isWithinPerceptionDetailed(h1, dude,
                    CombatAiDefs.PerceptionType.PERCEPTION_AI_TARGET.id()));
            Combat.combatState = savedCombatState
                    | (Combat.COMBAT_STATE_IN_COMBAT | Combat.COMBAT_STATE_PLAYER_TURN);
            dude.tile = TileMath.tileGetTileInDirection(24101, Rotation.SE, 10);
            dude.data.critter.combat.results = ObjTypes.Dam.NONE;

            // Danger source. Packet 0 (the dude's) drives the party branch.
            // Non-party actor: alive whoHitMe wins outright.
            d1.data.critter.combat.whoHitMe = d2;
            assertEquals(d2, CombatAi.aiDangerSource(d1));
            // Dead whoHitMe from the other team: nearest member of that team.
            d2.data.critter.combat.team = 2;
            d2.data.critter.combat.results = ObjTypes.Dam.DEAD;
            assertEquals(d3, CombatAi.aiDangerSource(d1));
            // No relations at all.
            clearRelations();
            d2.data.critter.combat.team = 1;
            assertEquals(null, CombatAi.aiDangerSource(d1));

            // Party branch, whomever-attacking-me with a valid last target.
            dudePacket.attackWho = CombatAiDefs.AttackWho.ATTACK_WHO_WHOMEVER_ATTACKING_ME.id();
            dudePacket.disposition = CombatAiDefs.Disposition.DISPOSITION_DEFENSIVE.id();
            dudePacket.distance = CombatAiDefs.DistanceMode.DISTANCE_STAY_CLOSE.id();
            d2.data.critter.combat.team = 2;
            assertEquals(0, Combat.aiInfoSetLastTarget(dude, d2));
            assertEquals(0, Combat.aiInfoSetLastTarget(d2, dude));
            assertEquals(d2, CombatAi.aiDangerSource(dude));
            // Fleeing target is skipped under DEFENSIVE; the scan picks a
            // critter attacking the dude.
            d2.data.critter.combat.maneuver = ObjTypes.CritterManeuver.FLEEING;
            d3.data.critter.combat.whoHitMe = null;
            assertEquals(0, Combat.aiInfoSetLastTarget(d3, dude));
            assertEquals(d3, CombatAi.aiDangerSource(dude));
            // Also fleeing: next attacker is knocked out, nobody left.
            d3.data.critter.combat.maneuver = ObjTypes.CritterManeuver.FLEEING;
            d4.data.critter.combat.whoHitMe = null;
            assertEquals(0, Combat.aiInfoSetLastTarget(d4, dude));
            d4.data.critter.combat.results = ObjTypes.Dam.KNOCKED_OUT;
            assertEquals(null, CombatAi.aiDangerSource(dude));
            // AGGRESSIVE: fleeing critters are not ignored, last target
            // returns.
            d2.data.critter.combat.maneuver = ObjTypes.CritterManeuver.FLEEING;
            dudePacket.disposition = CombatAiDefs.Disposition.DISPOSITION_AGGRESSIVE.id();
            assertEquals(d2, CombatAi.aiDangerSource(dude));
            // STRONGEST/WEAKEST/CLOSEST with a preserved whoHitMe and an
            // attacker: the weaponless dude rejects every candidate through
            // the bad-shot gate unless the pathfinder routes around, so the
            // first passable target wins the final loop.
            d3.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
            d4.data.critter.combat.results = ObjTypes.Dam.NONE;
            dude.data.critter.combat.whoHitMe = d3;
            d2.data.critter.combat.whoHitMe = dude;
            dudePacket.attackWho = CombatAiDefs.AttackWho.ATTACK_WHO_STRONGEST.id();
            assertEquals(d3, CombatAi.aiDangerSource(dude));
            dudePacket.attackWho = CombatAiDefs.AttackWho.ATTACK_WHO_WEAKEST.id();
            assertEquals(d2, CombatAi.aiDangerSource(dude));
            dudePacket.attackWho = CombatAiDefs.AttackWho.ATTACK_WHO_CLOSEST.id();
            assertEquals(d2, CombatAi.aiDangerSource(dude));
            // WHOMEVER with the whoHitMe cleared by the STRONGEST case: the
            // attacker slot fills and the closest passable target wins.
            dudePacket.attackWho = CombatAiDefs.AttackWho.ATTACK_WHO_WHOMEVER.id();
            assertEquals(d2, CombatAi.aiDangerSource(dude));
        } finally {
            Combat.combatState = savedCombatState;
            Combat.aiInfoList = null;
            CombatAi.combatAiOver();
        }
    }

    private void clearRelations() {
        for (GameObject obj : roster) {
            obj.data.critter.combat.whoHitMe = null;
            obj.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
            obj.data.critter.combat.results = ObjTypes.Dam.NONE;
            Combat.aiInfoSetLastTarget(obj, null);
        }
    }
}
