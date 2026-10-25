package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.CombatAi.AiPacket;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * [aiinv], golden/v165.txt: the AI inventory/weapon-decision slice — the
 * reload gate ({@code weaponCanBeReloadedWith}), ammo search with the
 * solar-scorcher light gate and right-hand unequip ({@code aiHaveAmmo}),
 * weapon/armor choice ({@code _ai_best_weapon},
 * {@code _ai_search_inven_weap}, {@code _ai_search_inven_armor}), heal
 * item use ({@code aiCanUseItem}), explosion safety
 * ({@code _combat_safety_invalidate_weapon}) and the hit-mode pick
 * ({@code _ai_pick_hit_mode}), pinned against the real C++ sectionAiInventory
 * in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The harness reuses one mutating packet and a few shared objects, so the
 * ordering (including the shared right-hand flag leaking into the null-defender
 * probe) is preserved exactly.
 */
class GameCombatAiInventoryTest {

    private GameObject dog;
    private GameObject target;
    private GameObject human;
    private GameObject pistol;
    private GameObject pistol2;
    private GameObject smg;
    private GameObject spear;
    private GameObject flare;
    private GameObject molotov;
    private GameObject ammo;
    private GameObject cell;
    private GameObject nonWeapon;
    private GameObject scorcher;
    private GameObject stimpak;
    private AiPacket a0;
    private Proto humanProto;
    private Proto pistolProto;
    private Proto targetProto;

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

        Settings.settings.system.language = Message.ENGLISH;
        Settings.settings.preferences.language_filter = false;
        String root = System.getenv("F2J_GAMEDATA");
        assertNotNull(root);
        assertEquals(0, Db.dbOpen(Paths.get(root, "master.dat").toString(),
                Paths.get(root, "patch000.dat").toString()));
        assertEquals(true, XFile.xbaseOpen(Paths.get(root, "master.dat").toString()));
        assertEquals(true, XFile.xbaseOpen(Paths.get(root, "patch000.dat").toString()));
        assertEquals(true, XFile.xbaseOpen(Paths.get(root, "critter.dat").toString()));
        assertEquals(0, Art.artInit());
        byte[] tileBuf = new byte[640 * 480];
        assertEquals(0, TileMath.tileInit(null, 100, 100, 200, 200, tileBuf, 640, 480, 640,
                rect -> {
                }));
        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, Stat.statsInit());

        // The heal/explosive tables read the content config (gameInit loads it).
        GameConfig.gameConfigInit(false, new String[] { "prog" });
        assertEquals(0, Item.itemsInit());

        assertEquals(0, CombatAi.aiInit());
        a0 = CombatAi.aiGetPacketByIndex(0);

        dog = create(16777558);
        target = create(16777558);
        human = create(16777376);
        pistol = create(8);
        pistol2 = create(8);
        smg = create(9);
        spear = create(7);
        flare = create(79);
        molotov = create(159);
        ammo = create(29);
        cell = create(38);
        nonWeapon = create(1);
        scorcher = create(390);
        stimpak = create(40);

        ObjectSystem.objectSetLocation(dog, 20100, 0);
        ObjectSystem.objectSetLocation(human, 20100, 0);
        ObjectSystem.objectSetLocation(target, 20101, 0);

        Proto[] hp = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(human), hp);
        humanProto = hp[0];
        Proto[] pp = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(pistol), pp);
        pistolProto = pp[0];
        Proto[] tp = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(target), tp);
        targetProto = tp[0];
    }

    @AfterEach
    void tearDown() {
        GameRandom.resetProviders();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
        CombatAi.resetForTests();
    }

    private static GameObject create(int pid) {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoId.ofPid(pid)));
        return slot[0];
    }

    private static void assign(GameObject obj, AiPacket p) {
        obj.data.critter.combat.aiPacket = p.packetNum;
    }

    private static void setHumanFid(GameObject critter) {
        critter.fid = FrmId.ofCritter(11,
                AnimationDefs.AnimationType.ANIM_STAND.id(),
                ArtDefs.WeaponAnimation.None.id(),
                ObjTypes.Rotation.ROTATION_NE.id()).fid();
    }

    private void setSkill(int value) {
        humanProto.critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()] = value;
    }

    @Test
    void aiInventoryVectors() {
        // ---- reload gate ----
        assertEquals(true, Item.weaponCanBeReloadedWith(pistol, ammo));
        assertEquals(false, Item.weaponCanBeReloadedWith(pistol, cell));
        assertEquals(false, Item.weaponCanBeReloadedWith(null, ammo));
        assertEquals(false, Item.weaponCanBeReloadedWith(pistol, null));
        assertEquals(false, Item.weaponCanBeReloadedWith(pistol, nonWeapon));
        assertEquals(false, Item.weaponCanBeReloadedWith(pistol, pistol2));

        int savedAmmoType = pistol.data.item.weapon.ammoTypePid;
        int savedAmmoQty = pistol.data.item.weapon.ammoQuantity;
        pistol.data.item.weapon.ammoQuantity = 5;
        pistol.data.item.weapon.ammoTypePid = cell.pid;
        assertEquals(false, Item.weaponCanBeReloadedWith(pistol, ammo));
        pistol.data.item.weapon.ammoQuantity = 0;
        assertEquals(true, Item.weaponCanBeReloadedWith(pistol, ammo));
        pistol.data.item.weapon.ammoTypePid = savedAmmoType;
        pistol.data.item.weapon.ammoQuantity = savedAmmoQty;

        // ---- aiHaveAmmo ----
        GameObject h1 = create(16777376);
        GameObject h2 = create(16777376);
        GameObject h3 = create(16777376);
        GameObject h4 = create(16777376);
        GameObject h5 = create(16777376);
        Item.itemAdd(h1, ammo, 1);
        GameObject[] out = new GameObject[1];
        assertEquals(true, CombatAi.aiHaveAmmo(h1, pistol, out));
        assertEquals(ammo, out[0]);

        out[0] = null;
        assertEquals(false, CombatAi.aiHaveAmmo(h2, pistol, out));
        assertEquals(null, out[0]);

        Item.itemAdd(h3, cell, 1);
        assertEquals(false, CombatAi.aiHaveAmmo(h3, pistol, null));

        Light.lightSetAmbientIntensity(Light.LIGHT_INTENSITY_MAX, false);
        assertEquals(true, CombatAi.aiHaveAmmo(h2, scorcher, null));
        Light.lightSetAmbientIntensity(0, false);
        assertEquals(false, CombatAi.aiHaveAmmo(h2, scorcher, null));
        Light.lightSetAmbientIntensity(Light.LIGHT_INTENSITY_MAX, false);

        Item.itemAdd(h4, spear, 1);
        spear.flags = spear.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        Item.itemAdd(h4, cell, 1);
        CombatAi.aiHaveAmmo(h4, spear, null);
        assertEquals(0, spear.flags.bits() & ObjTypes.ObjectFlags.IN_RIGHT_HAND.bits());

        Item.itemAdd(h5, pistol, 1);
        pistol.flags = pistol.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        Item.itemAdd(h5, cell, 1);
        CombatAi.aiHaveAmmo(h5, pistol, null);
        assertEquals(true, (pistol.flags.bits() & ObjTypes.ObjectFlags.IN_RIGHT_HAND.bits()) != 0);
        pistol.flags = pistol.flags.and(ObjTypes.ObjectFlags.IN_ANY_HAND.not());

        // ---- _ai_best_weapon ----
        assign(dog, a0);
        a0.minToHit = 0;
        a0.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_RANGED.id();
        assertEquals(pistol, CombatAi.aiBestWeapon(dog, pistol, spear, target));
        a0.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_MELEE.id();
        assertEquals(spear, CombatAi.aiBestWeapon(dog, pistol, spear, target));
        a0.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_NO_PREF.id();
        assertEquals(pistol, CombatAi.aiBestWeapon(dog, pistol, spear, target));
        assertEquals(pistol, CombatAi.aiBestWeapon(dog, pistol, pistol2, target));
        assertEquals(null, CombatAi.aiBestWeapon(dog, null, null, target));
        assertEquals(null, CombatAi.aiBestWeapon(null, pistol, spear, target));

        ObjectSystem.objectSetLocation(target, 20126, 0);
        assertEquals(null, CombatAi.aiBestWeapon(dog, null, null, target));
        ObjectSystem.objectSetLocation(target, 20101, 0);

        a0.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_RANDOM.id();
        GameRandom.randomSeedPrerandom(12345);
        assertEquals(pistol, CombatAi.aiBestWeapon(dog, pistol, spear, target));

        a0.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_RANGED.id();
        ProtoTypes.ProtoExtendedFlags savedExtendedFlags = pistolProto.item.extendedFlags;
        pistolProto.item.extendedFlags = savedExtendedFlags.or(ProtoTypes.ProtoExtendedFlags.HIDDEN);
        assertEquals(pistol, CombatAi.aiBestWeapon(dog, pistol, spear, target));
        pistolProto.item.extendedFlags = savedExtendedFlags;
        assertEquals(pistol, CombatAi.aiBestWeapon(dog, flare, pistol, target));

        // ---- _ai_search_inven_weap ----
        int savedFid = human.fid;
        setHumanFid(human);
        int savedSkill = humanProto.critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()];
        setSkill(120);
        assign(human, a0);
        a0.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_NO_PREF.id();
        a0.minToHit = 0;
        human.data.critter.combat.ap = 10;

        GameObject h6 = create(16777376);
        GameObject h7 = create(16777376);
        GameObject h8 = create(16777376);
        GameObject h9 = create(16777376);
        GameObject[] humans = { h6, h7, h8, h9 };
        for (GameObject h : humans) {
            setHumanFid(h);
            assign(h, a0);
            h.data.critter.combat.ap = 10;
        }

        assertEquals(null, CombatAi.aiSearchInvenWeap(h6, true, target));
        Item.itemAdd(h7, pistol, 1);
        assertEquals(null, CombatAi.aiSearchInvenWeap(h7, true, target));
        Item.itemAdd(h7, ammo, 1);
        assertEquals(pistol, CombatAi.aiSearchInvenWeap(h7, true, target));
        h7.data.critter.combat.ap = 3;
        assertEquals(null, CombatAi.aiSearchInvenWeap(h7, true, target));
        assertEquals(pistol, CombatAi.aiSearchInvenWeap(h7, false, target));
        h7.data.critter.combat.ap = 10;
        a0.minToHit = 200;
        assertEquals(null, CombatAi.aiSearchInvenWeap(h7, true, target));
        a0.minToHit = 0;
        Item.itemAdd(h8, spear, 1);
        assertEquals(spear, CombatAi.aiSearchInvenWeap(h8, true, target));
        Item.itemAdd(h9, pistol, 1);
        pistol.flags = pistol.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        Item.itemAdd(h9, spear, 1);
        assertEquals(spear, CombatAi.aiSearchInvenWeap(h9, true, target));
        assertEquals(null, CombatAi.aiSearchInvenWeap(h7, true, null));
        pistol.flags = pistol.flags.and(ObjTypes.ObjectFlags.IN_ANY_HAND.not());

        Item.itemAdd(dog, pistol, 1);
        Item.itemAdd(dog, ammo, 1);
        assertEquals(null, CombatAi.aiSearchInvenWeap(dog, true, target));

        assertEquals(null, CombatAi.aiSearchInvenArmor(dog));
        assertEquals(null, CombatAi.aiSearchInvenArmor(human));

        humanProto.critter.data.skills[SkillDefs.Skill.SKILL_SMALL_GUNS.id()] = savedSkill;
        human.fid = savedFid;

        // ---- aiCanUseItem ----
        assign(human, a0);
        a0.chemPrimaryDesire[0] = 40;
        a0.chemPrimaryDesire[1] = -1;
        a0.chemPrimaryDesire[2] = -1;
        human.data.critter.hp = 1000;
        assertEquals(true, CombatAi.aiCanUseItem(human, stimpak));
        a0.chemPrimaryDesire[0] = -1;
        a0.chemPrimaryDesire[1] = -1;
        a0.chemPrimaryDesire[2] = -1;
        human.data.critter.hp = 0;
        assertEquals(true, CombatAi.aiCanUseItem(human, stimpak));
        assertEquals(false, CombatAi.aiCanUseItem(human, nonWeapon));
        human.data.critter.hp = 1000;
        assertEquals(false, CombatAi.aiCanUseItem(human, stimpak));
        human.data.critter.hp = 0;
        assertEquals(false, CombatAi.aiCanUseItem(null, stimpak));
        assertEquals(false, CombatAi.aiCanUseItem(human, null));

        int savedInt = humanProto.critter.data.baseStats[StatDefs.Stat.STAT_INTELLIGENCE.id()];
        humanProto.critter.data.baseStats[StatDefs.Stat.STAT_INTELLIGENCE.id()] = 2;
        assertEquals(false, CombatAi.aiCanUseItem(human, stimpak));
        humanProto.critter.data.baseStats[StatDefs.Stat.STAT_INTELLIGENCE.id()] = savedInt;

        ProtoTypes.KillType savedKill = humanProto.critter.data.killType;
        humanProto.critter.data.killType = ProtoTypes.KillType.KILL_TYPE_DOG;
        assertEquals(false, CombatAi.aiCanUseItem(human, stimpak));
        humanProto.critter.data.killType = savedKill;

        ProtoTypes.BodyType savedBody = humanProto.critter.data.bodyType;
        humanProto.critter.data.bodyType = ProtoTypes.BodyType.BODY_TYPE_QUADRUPED;
        assertEquals(false, CombatAi.aiCanUseItem(human, stimpak));
        humanProto.critter.data.bodyType = savedBody;

        int savedMaxHp = humanProto.critter.data.baseStats[StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id()];
        int savedBonusMaxHp = humanProto.critter.data.bonusStats[StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id()];
        humanProto.critter.data.baseStats[StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id()] = 100;
        humanProto.critter.data.bonusStats[StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id()] = 0;
        human.data.critter.hp = 40;
        a0.chemUse = CombatAiDefs.ChemUse.CHEM_USE_CLEAN.id();
        assertEquals(false, CombatAi.aiCanUseItem(human, stimpak));
        a0.chemUse = CombatAiDefs.ChemUse.CHEM_USE_STIMS_WHEN_HURT_LITTLE.id();
        assertEquals(true, CombatAi.aiCanUseItem(human, stimpak));
        a0.chemUse = CombatAiDefs.ChemUse.CHEM_USE_STIMS_WHEN_HURT_LOTS.id();
        assertEquals(false, CombatAi.aiCanUseItem(human, stimpak));
        a0.chemUse = CombatAiDefs.ChemUse.CHEM_USE_ALWAYS.id();
        assertEquals(true, CombatAi.aiCanUseItem(human, stimpak));
        humanProto.critter.data.baseStats[StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id()] = savedMaxHp;
        humanProto.critter.data.bonusStats[StatDefs.Stat.STAT_MAXIMUM_HIT_POINTS.id()] = savedBonusMaxHp;
        human.data.critter.hp = 0;

        // ---- _combat_safety_invalidate_weapon ----
        assertEquals(false, Combat.combatSafetyInvalidateWeapon(human, pistol,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), target, null));
        assertEquals(false, Combat.combatSafetyInvalidateWeapon(human, smg,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id(), target, null));
        Item.explosionSetRadius(3);
        assertEquals(true, Combat.combatSafetyInvalidateWeapon(human, molotov,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), target, null));
        assertEquals(false, Combat.combatSafetyInvalidateWeapon(dog, molotov,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), target, null));
        ObjectSystem.objectSetLocation(target, 20126, 0);
        assertEquals(false, Combat.combatSafetyInvalidateWeapon(human, molotov,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(), target, null));
        ObjectSystem.objectSetLocation(target, 20101, 0);
        Item.explosionSetRadius(-1);

        // ---- _ai_pick_hit_mode ----
        setHumanFid(human);
        setSkill(120);
        assign(human, a0);
        a0.minToHit = 0;
        a0.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_NO_PREF.id();
        human.data.critter.combat.ap = 10;

        assertEquals(CombatDefs.HitMode.HIT_MODE_PUNCH.id(),
                CombatAi.aiPickHitMode(human, null, target));
        assertEquals(CombatDefs.HitMode.HIT_MODE_PUNCH.id(),
                CombatAi.aiPickHitMode(human, nonWeapon, target));
        assertEquals(CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                CombatAi.aiPickHitMode(human, pistol, target));

        a0.areaAttackMode = CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_ALWAYS.id();
        CombatAi.aiSetBurstDisabled(human, true);
        assertEquals(CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                CombatAi.aiPickHitMode(human, smg, target));
        CombatAi.aiSetBurstDisabled(human, false);
        assertEquals(CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id(),
                CombatAi.aiPickHitMode(human, smg, target));

        a0.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_MELEE.id();
        assertEquals(CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                CombatAi.aiPickHitMode(human, smg, target));
        a0.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_NO_PREF.id();

        a0.areaAttackMode = CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_SOMETIMES.id();
        a0.secondaryFreq = 1;
        GameRandom.randomSeedPrerandom(1);
        assertEquals(CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id(),
                CombatAi.aiPickHitMode(human, smg, target));

        a0.areaAttackMode = CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_INVALID.id();
        GameRandom.randomSeedPrerandom(1);
        assertEquals(CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id(),
                CombatAi.aiPickHitMode(human, smg, target));

        ObjectSystem.objectSetLocation(target, 20126, 0);
        GameRandom.randomSeedPrerandom(1);
        assertEquals(CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                CombatAi.aiPickHitMode(human, smg, target));
        ObjectSystem.objectSetLocation(target, 20101, 0);

        a0.areaAttackMode = CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_BE_SURE.id();
        // The reference "pick-tohit=-22" runs in-process after earlier sections
        // mutate the shared proto (AC bonus -> 50) and the difficulty, so its
        // absolute value is not reproducible in isolation; only the BE_SURE
        // gate (>= 85 to use the secondary mode) is, and it stays closed here.
        GameRandom.randomSeedPrerandom(1);
        assertEquals(CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id(),
                CombatAi.aiPickHitMode(human, smg, target));
    }
}
