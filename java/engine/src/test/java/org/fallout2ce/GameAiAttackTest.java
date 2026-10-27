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
 * [aiattack], golden/v170.txt: the AI attack head — {@code _ai_called_shot}
 * (aim-cost gate, {@code critterCanAim}, the called-shot roll with the
 * difficulty-scaled intelligence gate, and the to-hit re-check) and the
 * {@code _ai_attack} flee guard. The C++ section runs the called-shot cases
 * in combat with the small-guns skill raised so the aimed to-hit clears the
 * packet minimum; the full attack path's rotate-registration first-step
 * handler stalls headless, so the vector pins the pre-registration guard.
 */
class GameAiAttackTest {

    private GameObject dude;
    private GameObject h1;
    private GameObject d1;
    private GameObject tgt;
    private GameObject[] roster;
    private AiPacket humanPacket;
    private int savedCalledFreq;
    private int savedMinToHit;
    private Proto humanProto;
    private int savedSkill;
    private GameObject pistol;

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
        tgt = create(16777558);
        roster = new GameObject[] { dude, h1, tgt };

        int[] distances = { 10, 0, 4 };
        for (int index = 0; index < 3; index++) {
            GameObject obj = roster[index];
            obj.cid = index;
            obj.data.critter.combat.team = index == 2 ? 2 : 1;
            obj.data.critter.combat.results = ObjTypes.Dam.NONE;
            obj.data.critter.combat.whoHitMe = null;
            obj.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
            obj.data.critter.combat.ap = 10;
            obj.data.critter.hp = 50;
            obj.rotation = ObjTypes.Rotation.ROTATION_SE;
            if (index != 0) {
                assertEquals(0, ObjectSystem.objectSetLocation(obj,
                        TileMath.tileGetTileInDirection(A, Rotation.SE, distances[index]), 0));
            }
        }
        dude.tile = TileMath.tileGetTileInDirection(A, Rotation.SE, distances[0]);
        dude.elevation = 0;

        Animation.animationReset();
        CombatAi.combatAiBegin(roster, 3);
        Combat.aiInfoList = new Combat.CombatAiInfo[3];
        for (int index = 0; index < 3; index++) {
            Combat.aiInfoList[index] = new Combat.CombatAiInfo();
        }

        humanPacket = CombatAi.aiGetPacket(h1);
        savedCalledFreq = humanPacket.calledFreq;
        savedMinToHit = humanPacket.minToHit;
        humanPacket.calledFreq = 1;
        humanPacket.minToHit = 40;

        Proto[] humanProtoSlot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(h1), humanProtoSlot));
        humanProto = humanProtoSlot[0];
        savedSkill = humanProto.critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()];
        humanProto.critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()] = 200;

        pistol = create(8);
        assertEquals(0, Item.itemAdd(h1, pistol, 1));
        pistol.flags = pistol.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
    }

    @AfterEach
    void tearDown() {
        humanProto.critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()] = savedSkill;
        humanPacket.calledFreq = savedCalledFreq;
        humanPacket.minToHit = savedMinToHit;
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

    @Test
    void aiAttackVectors() {
        // The aimed shot with enough AP and the raised skill: the roll lands
        // on a specific location (seed 1 -> LEFT_ARM).
        GameRandom.randomSeedPrerandom(1);
        assertEquals(1, CombatAi.aiCalledShot(h1, tgt,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));

        // Too little AP for aiming: torso always.
        h1.data.critter.combat.ap = 1;
        GameRandom.randomSeedPrerandom(1);
        assertEquals(3, CombatAi.aiCalledShot(h1, tgt,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));
        h1.data.critter.combat.ap = 10;

        // Weaponless attacker: torso.
        assertEquals(0, Item.itemRemoveWithReason(h1, pistol, 1,
                Item.RemoveInventoryObjectHookReason.ITEM_REMOVED, null));
        GameRandom.randomSeedPrerandom(1);
        assertEquals(3, CombatAi.aiCalledShot(h1, tgt,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));
        assertEquals(0, Item.itemAdd(h1, pistol, 1));

        // _ai_attack: a fleeing attacker refuses before any registration.
        d1.data.critter.combat.maneuver = ObjTypes.CritterManeuver.FLEEING;
        assertEquals(-1, CombatAi.aiAttack(d1, tgt,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));
        d1.data.critter.combat.maneuver = ObjTypes.CritterManeuver.NONE;
    }
}
