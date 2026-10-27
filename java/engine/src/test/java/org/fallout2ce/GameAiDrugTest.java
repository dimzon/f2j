package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.EventQueue.EventType;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * [aimdrug], golden/v171.txt: the drug-taking engine ({@code drugItemTakeDrug}
 * and the effect/withdrawal events) plus {@code _ai_search_environ}. The
 * vector pins the jet/buffout proto shapes, the take results (REMOVE), the
 * event counts (0 -> 2 -> 4 DRUG events), the applied bonus stats and the
 * environ-scan gate refusals in this headless setup.
 */
class GameAiDrugTest {

    private static final int A = 38101;

    private GameObject h1;
    private GameObject d1;
    private GameObject pistol;
    private GameObject ammo;
    private GameObject jet;
    private GameObject buffout;
    private GameObject mentats;
    private GameObject stimpak;

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

        EventQueue.queueInit();

        h1 = create(16777376);
        d1 = create(16777558);
        h1.cid = 0;
        d1.cid = 1;
        h1.data.critter.combat.team = 1;
        d1.data.critter.combat.team = 1;
        h1.data.critter.combat.ap = 10;
        d1.data.critter.combat.ap = 10;
        h1.data.critter.hp = 50;
        d1.data.critter.hp = 50;
        h1.rotation = ObjTypes.Rotation.ROTATION_SE;
        d1.rotation = ObjTypes.Rotation.ROTATION_SE;
        assertEquals(0, ObjectSystem.objectSetLocation(h1, A, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(d1,
                TileMath.tileGetTileInDirection(A, Rotation.SE, 3), 0));

        pistol = create(8);
        ammo = create(29);
        jet = create(259);
        buffout = create(87);
        mentats = create(53);
        stimpak = create(40);
        assertEquals(0, ObjectSystem.objectSetLocation(pistol, 199, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(ammo, 199, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(jet, 199, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(buffout, 199, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(mentats, 199, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(stimpak, 199, 0));
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

    private static int countEvents(GameObject owner, EventType type) {
        int count = 0;
        Object event = EventQueue.queueFindFirstEvent(owner, type);
        while (event != null) {
            count++;
            event = EventQueue.queueFindNextEvent(owner, type);
        }
        return count;
    }

    @Test
    void aiDrugVectors() {
        // Jet proto shape.
        ProtoTypes.Proto[] jetSlot = new ProtoTypes.Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoTypes.ProtoId.ofGameObject(jet), jetSlot));
        assertEquals(8, jetSlot[0].item.data.drug.stat[0].id());
        assertEquals(0, jetSlot[0].item.data.drug.stat[1].id());
        assertEquals(1, jetSlot[0].item.data.drug.stat[2].id());
        assertEquals(2, jetSlot[0].item.data.drug.amount[0]);
        assertEquals(1, jetSlot[0].item.data.drug.amount[1]);
        assertEquals(1, jetSlot[0].item.data.drug.amount[2]);
        assertEquals(5, jetSlot[0].item.data.drug.duration1);
        assertEquals(1440, jetSlot[0].item.data.drug.duration2);
        assertEquals(50, jetSlot[0].item.data.drug.addictionChance);

        assertEquals(0, countEvents(h1, EventType.DRUG));
        assertEquals(0, countEvents(h1, EventType.WITHDRAWAL));

        GameRandom.randomSeedPrerandom(1);
        assertEquals(1, Drugs.drugItemTakeDrug(h1, jet));
        assertEquals(2, countEvents(h1, EventType.DRUG));
        assertEquals(1, countEvents(h1, EventType.WITHDRAWAL));

        // Jet applies +2 PE, +1 ST, +1 AG.
        assertEquals(2, Stat.critterGetBonusStat(h1, 8));
        assertEquals(1, Stat.critterGetBonusStat(h1, 0));
        assertEquals(1, Stat.critterGetBonusStat(h1, 1));

        ProtoTypes.Proto[] buffoutSlot = new ProtoTypes.Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoTypes.ProtoId.ofGameObject(buffout), buffoutSlot));
        assertEquals(0, buffoutSlot[0].item.data.drug.stat[0].id());
        assertEquals(5, buffoutSlot[0].item.data.drug.stat[1].id());
        assertEquals(2, buffoutSlot[0].item.data.drug.stat[2].id());
        assertEquals(2, buffoutSlot[0].item.data.drug.amount[0]);
        assertEquals(2, buffoutSlot[0].item.data.drug.amount[1]);
        assertEquals(3, buffoutSlot[0].item.data.drug.amount[2]);
        assertEquals(360, buffoutSlot[0].item.data.drug.duration1);
        assertEquals(1080, buffoutSlot[0].item.data.drug.duration2);
        assertEquals(25, buffoutSlot[0].item.data.drug.addictionChance);

        assertEquals(1, Drugs.drugItemTakeDrug(h1, buffout));
        assertEquals(4, countEvents(h1, EventType.DRUG));

        // Environ scans: equip a pistol and raise small guns, yet all three
        // gates still refuse in this headless setup (see PLAN.md).
        assertEquals(0, Item.itemAdd(h1, pistol, 1));
        pistol.flags = pistol.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        ProtoTypes.Proto[] humanSlot = new ProtoTypes.Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoTypes.ProtoId.ofGameObject(h1), humanSlot));
        int savedSkill = humanSlot[0].critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()];
        humanSlot[0].critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()] = 200;

        assertFalse(CombatAi.aiSearchEnviron(h1, ProtoTypes.ItemType.ITEM_TYPE_WEAPON.id()));
        assertFalse(CombatAi.aiSearchEnviron(h1, ProtoTypes.ItemType.ITEM_TYPE_AMMO.id()));
        assertFalse(CombatAi.aiSearchEnviron(h1, ProtoTypes.ItemType.ITEM_TYPE_DRUG.id()));

        humanSlot[0].critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()] = savedSkill;
    }
}
