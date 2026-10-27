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
 * [aidist], golden/v169.txt: distance preferences
 * ({@code _cai_perform_distance_prefs}) over the ported movement/retarget
 * helpers, pinned against the real C++ sectionAiDist in
 * {@code docker/refharness/ref_main.cc}. The section runs out of combat
 * (the retarget tail cannot fire headless - its combat turn run spins on
 * the animation counter), which the vectors document.
 */
class GameAiDistTest {

    private GameObject dude;
    private GameObject h1;
    private GameObject d1;
    private GameObject d2;
    private GameObject tgt;
    private GameObject far;
    private GameObject[] roster;
    private AiPacket dogPacket;
    private AiPacket humanPacket;
    private int savedDogDistance;
    private int savedHumanDistance;

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

        // The harness process carries earlier sections' proto mutations.
        seedProtoBonus(16777376, StatDefs.Stat.STAT_ARMOR_CLASS.id(), 5);
        seedProtoBonus(16777558, StatDefs.Stat.STAT_MELEE_DAMAGE.id(), 13);
        seedProtoBonus(16777558, StatDefs.Stat.STAT_ARMOR_CLASS.id(), 50);

        dude = ObjectSystem.getDude();
        assertNotNull(dude);
        h1 = create(16777376);
        d1 = create(16777558);
        d2 = create(16777558);
        tgt = create(16777558);
        far = create(16777558);
        roster = new GameObject[] { dude, h1, d1, tgt, far };

        int[] distances = { 10, 0, -1, 4, 40 };
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
        humanPacket = CombatAi.aiGetPacket(h1);
        savedDogDistance = dogPacket.distance;
        savedHumanDistance = humanPacket.distance;
    }

    @AfterEach
    void tearDown() {
        dogPacket.distance = savedDogDistance;
        humanPacket.distance = savedHumanDistance;
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
    void aiDistVectors() {
        // Unarmed AP cost for the human sniper.
        assertEquals(3, Item.weaponGetActionPointCost(h1,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), false));

        GameObject pistol = create(8);
        assertEquals(0, Item.itemAdd(d1, pistol, 1));
        pistol.flags = pistol.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);

        // STAY_CLOSE: whoHitMe is not the dude and the dude is more than 5
        // tiles away, so the dog steps closer.
        dogPacket.distance = CombatAiDefs.DistanceMode.DISTANCE_STAY_CLOSE.id();
        assertEquals(0, CombatAi.caiPerformDistancePrefs(d1, tgt));
        int[] dump = Animation.lastActionForTests();
        assertNotNull(dump);
        assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_TILE, 1, 39901, 5);

        // CHARGE: the dog charges the target (taunt prints the message).
        Animation.animationReset();
        dogPacket.distance = CombatAiDefs.DistanceMode.DISTANCE_CHARGE.id();
        assertEquals(0, CombatAi.caiPerformDistancePrefs(d1, tgt));
        dump = Animation.lastActionForTests();
        assertNotNull(dump);
        assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_TILE, 1, 38901, 5);

        // SNIPE weak: the human (rating 10) snipes the dog (70); with spare
        // AP after the shot cost the weaker actor retreats.
        Animation.animationReset();
        humanPacket.distance = CombatAiDefs.DistanceMode.DISTANCE_SNIPE.id();
        assertEquals(0, CombatAi.caiPerformDistancePrefs(h1, d1));
        dump = Animation.lastActionForTests();
        assertNotNull(dump);
        assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_TILE, 1, 39101, 5);

        // SNIPE no spare AP: AP 3 against a pistol cost of 5.
        Animation.animationReset();
        d1.data.critter.combat.ap = 3;
        dogPacket.distance = CombatAiDefs.DistanceMode.DISTANCE_SNIPE.id();
        assertEquals(0, CombatAi.caiPerformDistancePrefs(d1, tgt));
        dump = Animation.lastActionForTests();
        assertNotNull(dump);
        assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_TILE, 1, 37301, 3);

        // SNIPE whole-AP cost: AP 5 equals the pistol cost, so the movement
        // points are exhausted and the retreat fires despite equal ratings.
        Animation.animationReset();
        d1.data.critter.combat.ap = 5;
        assertEquals(0, CombatAi.caiPerformDistancePrefs(d1, tgt));
        dump = Animation.lastActionForTests();
        assertNotNull(dump);
        assertAnim(dump, Animation.ANIM_KIND_MOVE_TO_TILE, 1, 36901, 5);
    }
}
