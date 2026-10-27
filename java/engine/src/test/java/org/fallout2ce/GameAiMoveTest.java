package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * [aimove], golden/v168.txt: the AI movement slice — the _moveBlockObj quirk
 * of {@code _obj_ai_blocking_at}, the friendly-fire intersection
 * ({@code _cai_attackWouldIntersect}), the retarget
 * ({@code _cai_retargetTileFromFriendlyFire}) and the run/away/move-closer
 * helpers with the registered animation as the observable, pinned against
 * the real C++ sectionAiMove in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The move helpers run out of combat (the C++ reg_anim_end then skips
 * _combat_anim_begin, so _combat_turn_run cannot spin); the harness resets
 * the sequence store before each move because a pending sequence keeps the
 * owner busy for the registry check.
 */
class GameAiMoveTest {

    private GameObject dude;
    private GameObject h1;
    private GameObject d1;
    private GameObject d2;
    private GameObject tgt;
    private GameObject far;
    private GameObject[] roster;
    private AiPacket dogPacket;
    private int savedDogDistance;
    private Proto h1Proto;

    private static final int A = 38101;

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

        // The harness process carries earlier sections' proto mutations; the
        // human AC bonus pins the ratings (h1 10, d1 70).
        seedProtoBonus(16777376, StatDefs.Stat.STAT_ARMOR_CLASS.id(), 5);

        dude = ObjectSystem.getDude();
        assertNotNull(dude);
        h1 = create(16777376);
        d1 = create(16777558);
        d2 = create(16777558);
        tgt = create(16777558);
        far = create(16777558);
        roster = new GameObject[] { dude, h1, d1, tgt, far };

        // Roster: h1 at a (source, rating 10), d1 one step north-west (the
        // shooter behind the source, rating 70), tgt at +4, far at +40.
        int[] distances = { 20, 0, -1, 4, 40 };
        for (int index = 0; index < 5; index++) {
            GameObject obj = roster[index];
            obj.cid = index;
            obj.data.critter.combat.team = index == 3 || index == 4 ? 2 : 1;
            obj.data.critter.combat.results = ObjTypes.Dam.NONE;
            obj.data.critter.combat.whoHitMe = null;
            obj.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
            obj.data.critter.combat.ap = 5;
            obj.data.critter.hp = 50;
            obj.rotation = ObjTypes.Rotation.ROTATION_SE;
            if (index == 2) {
                assertEquals(0, ObjectSystem.objectSetLocation(d1,
                        TileMath.tileGetTileInDirection(A, Rotation.NW, 1), 0));
            } else if (index != 0) {
                assertEquals(0, ObjectSystem.objectSetLocation(obj,
                        TileMath.tileGetTileInDirection(A, Rotation.SE, distances[index]), 0));
            }
        }
        dude.tile = TileMath.tileGetTileInDirection(A, Rotation.SE, distances[0]);
        dude.elevation = 0;

        Animation.animationReset();
        CombatAi.combatAiBegin(roster, 5);
        Combat.aiInfoList = new Combat.CombatAiInfo[5];
        for (int index = 0; index < 5; index++) {
            Combat.aiInfoList[index] = new Combat.CombatAiInfo();
        }

        dogPacket = CombatAi.aiGetPacket(d1);
        savedDogDistance = dogPacket.distance;
    }

    @AfterEach
    void tearDown() {
        dogPacket.distance = savedDogDistance;
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

    private static void assertAnim(int[] dump, int kind, int anim, int dst, int ap) {
        assertEquals(kind, dump[0]);
        assertEquals(anim, dump[1]);
        assertEquals(dst, dump[2]);
        assertEquals(ap, dump[3]);
    }

    @Test
    void aiMoveVectors() {
        // _obj_ai_blocking_at: the first critter on the tile is remembered
        // and skipped; the next call returns it.
        ObjectSystem.moveBlockObj = null;
        GameObject first = ObjectSystem.objAiBlockingAt(null, A, 0);
        assertNull(first);
        assertEquals(h1, ObjectSystem.moveBlockObj);
        GameObject second = ObjectSystem.objAiBlockingAt(null, A, 0);
        assertEquals(h1, second);
        ObjectSystem.moveBlockObj = null;

        // Friendly-fire intersection, combat on.
        int savedCombatState = Combat.combatState;
        Combat.combatState |= Combat.COMBAT_STATE_IN_COMBAT | Combat.COMBAT_STATE_PLAYER_TURN;

        try {
            GameObject pistol = create(8);
            assertEquals(0, Item.itemAdd(d1, pistol, 1));
            pistol.flags = pistol.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);

            int tgtTile = tgt.tile;
            assertEquals(1, CombatAi.caiAttackWouldIntersect(d1, tgt, h1, tgtTile,
                    new int[] { 1 }) ? 1 : 0);
            assertEquals(0, CombatAi.caiAttackWouldIntersect(d1, tgt, dude, tgtTile,
                    new int[] { 1 }) ? 1 : 0);
            assertEquals(0, CombatAi.caiAttackWouldIntersect(h1, tgt, h1, tgtTile,
                    new int[] { 1 }) ? 1 : 0);
            assertEquals(0, CombatAi.caiAttackWouldIntersect(d1, tgt, null, tgtTile,
                    new int[] { 1 }) ? 1 : 0);

            // Friendly-fire retarget: the ally in the shot line abandons the
            // target tile for the nearest free flank.
            assertEquals(0, Combat.aiInfoSetLastTarget(d1, tgt));
            int[] tilePtr = { tgtTile };
            CombatAi.caiRetargetTileFromFriendlyFire(h1, tgt, tilePtr);
            assertEquals(38902, tilePtr[0]);

            tilePtr[0] = tgtTile;
            assertEquals(0, ObjectSystem.objectSetLocation(d2,
                    TileMath.tileGetTileInDirection(tgtTile,
                            Rotation.fromInt((d1.rotation.id() + 1) % 6), 1), 0));
            CombatAi.caiRetargetTileFromFriendlyFire(h1, tgt, tilePtr);
            assertEquals(38900, tilePtr[0]);
            assertEquals(0, ObjectSystem.objectSetLocation(d2, 199, 0));

            tilePtr[0] = tgtTile;
            CombatAi.caiRetargetTileFromFriendlyFire(d1, tgt, tilePtr);
            assertEquals(tgtTile, tilePtr[0]);

            assertEquals(0, Combat.aiInfoSetLastTarget(d1, null));
            tilePtr[0] = tgtTile;
            CombatAi.caiRetargetTileFromFriendlyFire(h1, tgt, tilePtr);
            assertEquals(tgtTile, tilePtr[0]);

            // Zero intelligence on the proto still clamps to the bound
            // minimum, so the scan runs (documents the clamp).
            Proto[] h1ProtoSlot = new Proto[1];
            assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(h1), h1ProtoSlot));
            h1Proto = h1ProtoSlot[0];
            int savedInt = h1Proto.critter.data.baseStats[StatDefs.Stat.STAT_INTELLIGENCE.id()];
            h1Proto.critter.data.baseStats[StatDefs.Stat.STAT_INTELLIGENCE.id()] = 0;
            assertEquals(0, Combat.aiInfoSetLastTarget(d1, tgt));
            tilePtr[0] = tgtTile;
            CombatAi.caiRetargetTileFromFriendlyFire(h1, tgt, tilePtr);
            assertEquals(38902, tilePtr[0]);
            h1Proto.critter.data.baseStats[StatDefs.Stat.STAT_INTELLIGENCE.id()] = savedInt;

            // Movement helpers, combat off.
            Combat.combatState = Combat.COMBAT_STATE_PLAYER_TURN;
            Animation.animationReset();

            // _ai_move_away with DISTANCE_STAY refuses.
            dogPacket.distance = CombatAiDefs.DistanceMode.DISTANCE_STAY.id();
            assertEquals(-1, CombatAi.aiMoveAway(d1, tgt, 3));
            // With STAY_CLOSE the dog walks away from the enemy.
            dogPacket.distance = CombatAiDefs.DistanceMode.DISTANCE_STAY_CLOSE.id();
            assertEquals(0, CombatAi.aiMoveAway(d1, tgt, 12));
            int[] dump = Animation.lastActionForTests();
            assertNotNull(dump);
            assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_TILE, 1, 36901, 5);

            // _ai_run_away: the enemy is within max_dist, so the dog flees
            // at a run.
            dogPacket.maxDist = 10;
            Animation.animationReset();
            CombatAi.aiRunAway(d1, tgt);
            assertTrue(Critter.critterIsFleeing(d1));
            dump = Animation.lastActionForTests();
            assertNotNull(dump);
            assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_TILE, 19, 36901, 5);
            // Out of range: disengaging, no animation. The maneuver
            // accumulates, so clear it first.
            d1.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
            dogPacket.maxDist = 0;
            CombatAi.aiRunAway(d1, far);
            assertTrue(!Critter.critterIsFleeing(d1));

            // _ai_move_steps_closer: run with plenty of action points.
            d1.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
            Animation.animationReset();
            assertEquals(0, CombatAi.aiMoveStepsCloser(d1, tgt, 10, false));
            dump = Animation.lastActionForTests();
            assertNotNull(dump);
            assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_TILE, 19, 38901, 10);
            // Walk with few.
            Animation.animationReset();
            assertEquals(0, CombatAi.aiMoveStepsCloser(d1, tgt, 1, false));
            dump = Animation.lastActionForTests();
            assertNotNull(dump);
            assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_TILE, 1, 38901, 1);
            // Multihex target: move to object plus the rotate action.
            Animation.animationReset();
            tgt.flags = tgt.flags.or(ObjTypes.ObjectFlags.MULTIHEX);
            int multiRc = CombatAi.aiMoveStepsCloser(d1, tgt, 10, true);
            assertEquals(0, multiRc);
            dump = Animation.lastActionForTests();
            assertNotNull(dump);
            assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_OBJECT, 19, 16777558, 10);
            assertEquals(7, dump[4]);
            assertEquals(38901, dump[6]);
            tgt.flags = tgt.flags.and(ObjTypes.ObjectFlags.MULTIHEX.not());

            // _ai_find_friend: the nearest same-team friend is the human at
            // distance 1; a3 above it fires the approach move.
            Animation.animationReset();
            assertTrue(CombatAi.aiFindFriend(d1, 30, 5));
            dump = Animation.lastActionForTests();
            assertNotNull(dump);
            assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_OBJECT, 19, 16777558, 10);
            assertEquals(7, dump[4]);
        } finally {
            Combat.combatState = savedCombatState;
            Combat.aiInfoList = null;
            CombatAi.combatAiOver();
        }
    }
}
