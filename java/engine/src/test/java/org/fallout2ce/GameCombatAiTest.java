package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
 * [ai] + [aidec], golden/v164.txt: the AI packet table parse (data/ai.txt),
 * the packet lookup/readers, the threat rating, and the weapon-decision
 * helpers (min-hp, weapon preference, ranges, can-use checks, retaliation),
 * pinned against the real C++ aiInit + aiGetPacketByNum + _combatai_rating
 * + aiGetRunAwayMode + the decision functions.
 */
class GameCombatAiTest {
    private GameObject attacker;

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
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
        // Mirror the harness boot: the art lists and the critter FRMs come
        // out of the archives, and the hex grid is needed for distances.
        assertTrue(XFile.xbaseOpen(Paths.get(root, "master.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "patch000.dat").toString()));
        assertTrue(XFile.xbaseOpen(Paths.get(root, "critter.dat").toString()));
        assertEquals(0, Art.artInit());
        byte[] tileBuf = new byte[640 * 480];
        assertEquals(0, TileMath.tileInit(null, 100, 100, 200, 200, tileBuf, 640, 480, 640,
                rect -> {
                }));
        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, Stat.statsInit());

        attacker = create(16777558);
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

    @Test
    void aiInitVectors() {
        assertEquals(0, CombatAi.aiInit());
        assertEquals(187, CombatAi.aiPacketsLength());

        AiPacket p0 = CombatAi.aiGetPacketByNum(179);
        assertNotNull(p0);
        assertEquals("Addict - Wimpy", p0.name);
        assertEquals(179, p0.packetNum);
        assertEquals(10, p0.maxDist);
        assertEquals(90, p0.minToHit);
        assertEquals(100, p0.minHp);
        assertEquals(10, p0.aggression);
        assertEquals(0, p0.hurtTooMuch);
        assertEquals(5000, p0.secondaryFreq);
        assertEquals(10, p0.calledFreq);
        assertEquals(101, p0.font);
        assertEquals(58, p0.color);
        assertEquals(55, p0.outlineColor);
        assertEquals(15, p0.chance);
        assertEquals(2000, p0.run.start);
        assertEquals(2019, p0.run.end);
        assertEquals(2020, p0.move.start);
        assertEquals(2039, p0.move.end);
        assertEquals(2040, p0.attack.start);
        assertEquals(2059, p0.attack.end);
        assertEquals(2060, p0.miss.start);
        assertEquals(2079, p0.miss.end);
        assertEquals(1000, p0.hit[CombatDefs.HitLocation.HIT_LOCATION_HEAD.id()].start);
        assertEquals(1009, p0.hit[CombatDefs.HitLocation.HIT_LOCATION_HEAD.id()].end);
        assertEquals(1030, p0.hit[CombatDefs.HitLocation.HIT_LOCATION_TORSO.id()].start);
        assertEquals(1059, p0.hit[CombatDefs.HitLocation.HIT_LOCATION_TORSO.id()].end);
        assertEquals(1090, p0.hit[CombatDefs.HitLocation.HIT_LOCATION_GROIN.id()].start);
        assertEquals(1100, p0.hit[CombatDefs.HitLocation.HIT_LOCATION_GROIN.id()].end);
        assertEquals(CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_INVALID.id(), p0.areaAttackMode);
        assertEquals(CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_NONE.id(), p0.runAwayMode);
        assertEquals(CombatAiDefs.BestWeapon.BEST_WEAPON_INVALID.id(), p0.bestWeapon);
        assertEquals(CombatAiDefs.DistanceMode.DISTANCE_INVALID.id(), p0.distance);
        assertEquals(CombatAiDefs.AttackWho.ATTACK_WHO_INVALID.id(), p0.attackWho);
        assertEquals(CombatAiDefs.ChemUse.CHEM_USE_INVALID.id(), p0.chemUse);
        assertEquals(-1, p0.chemPrimaryDesire[0]);
        assertEquals(-1, p0.chemPrimaryDesire[1]);
        assertEquals(-1, p0.chemPrimaryDesire[2]);
        assertEquals(CombatAiDefs.Disposition.DISPOSITION_CUSTOM.id(), p0.disposition);

        AiPacket p1 = CombatAi.aiGetPacketByNum(149);
        assertNotNull(p1);
        assertEquals("Algernon", p1.name);
        assertEquals(149, p1.packetNum);
        assertEquals(10, p1.minHp);
        assertEquals(CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_INVALID.id(), p1.runAwayMode);
        assertEquals(CombatAiDefs.Disposition.DISPOSITION_INVALID.id(), p1.disposition);

        AiPacket p2 = CombatAi.aiGetPacketByNum(130);
        assertNotNull(p2);
        assertEquals("Alien", p2.name);
        assertEquals(1, p2.minHp);
        assertEquals(100, p2.aggression);
        assertEquals(0, p2.chance);
        assertEquals(CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_INVALID.id(), p2.runAwayMode);
    }

    @Test
    void ratingVectors() {
        assertEquals(0, CombatAi.aiInit());

        // Normalize the AC bonus (the proto's own bonus differs per loader),
        // mirroring the harness arrangement.
        assertEquals(0, Stat.critterSetBonusStat(attacker,
                StatDefs.Stat.STAT_ARMOR_CLASS.id(), 50));
        assertEquals(70, CombatAi.combataiRating(attacker));

        GameObject weapon = create(8);
        assertEquals(0, Item.itemAdd(attacker, weapon, 1));
        weapon.flags = weapon.flags.or(ObjTypes.ObjectFlags.IN_LEFT_HAND);
        attacker.data.critter.combat.aiPacket = 179;
        assertEquals(70, CombatAi.combataiRating(attacker));
        assertEquals(CombatAiDefs.AreaAttackMode.AREA_ATTACK_MODE_INVALID.id(),
                CombatAi.aiGetAreaAttackMode(attacker));
        assertEquals(CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_NONE.id(),
                CombatAi.aiGetRunAwayMode(attacker));
    }

    @Test
    void runAwayDeducedVector() {
        assertEquals(0, CombatAi.aiInit());

        GameObject critter2 = create(16777558);
        assertEquals(0, Stat.critterSetBaseStat(critter2,
                StatDefs.Stat.STAT_STRENGTH.id(), 5));
        assertEquals(0, Stat.critterSetBaseStat(critter2,
                StatDefs.Stat.STAT_ENDURANCE.id(), 5));
        Stat.critterUpdateDerivedStats(critter2);
        critter2.data.critter.combat.aiPacket = 130;
        assertEquals(CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_NONE.id(),
                CombatAi.aiGetRunAwayMode(critter2));
    }

    // [aidec] v164: aiGetWeaponRangeForHitMode (proto ranges, unarmed
    // reach, thrown-weapon strength clamp).
    @Test
    void weaponRangeVectors() {
        assertEquals(0, CombatAi.aiInit());
        GameObject critter = create(16777558);
        GameObject weapon = create(8);

        // [ai] pinned the shared maddog proto's base strength to 5 before
        // [aidec] ran, so the thrown-range clamp sees strength 5 here too.
        assertEquals(0, Stat.critterSetBaseStat(critter,
                StatDefs.Stat.STAT_STRENGTH.id(), 5));

        assertEquals(25, CombatAi.aiGetWeaponRangeForHitMode(critter, weapon,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));
        assertEquals(0, CombatAi.aiGetWeaponRangeForHitMode(critter, weapon,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id()));
        assertEquals(1, CombatAi.aiGetWeaponRangeForHitMode(critter, null,
                CombatDefs.HitMode.HIT_MODE_PUNCH.id()));

        Proto[] critterProto = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(critter), critterProto);
        ObjTypes.CritterFlags savedFlags = critterProto[0].critter.data.flags;
        critterProto[0].critter.data.flags =
                savedFlags.or(ObjTypes.CritterFlags.LONG_LIMBS);
        assertEquals(2, CombatAi.aiGetWeaponRangeForHitMode(critter, null,
                CombatDefs.HitMode.HIT_MODE_PUNCH.id()));
        critterProto[0].critter.data.flags = savedFlags;

        GameObject spear = create(7);
        Proto[] spearProto = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(spear), spearProto);
        int savedMaxRange2 = spearProto[0].item.data.weapon.maxRange2;
        assertEquals(2, CombatAi.aiGetWeaponRangeForHitMode(critter, spear,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id()));
        assertEquals(8, CombatAi.aiGetWeaponRangeForHitMode(critter, spear,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id()));
        assertEquals(5, Stat.critterGetStat(critter, StatDefs.Stat.STAT_STRENGTH.id()));

        spearProto[0].item.data.weapon.maxRange2 = 40;
        assertEquals(15, CombatAi.aiGetWeaponRangeForHitMode(critter, spear,
                CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_SECONDARY.id()));
        spearProto[0].item.data.weapon.maxRange2 = savedMaxRange2;
    }

    // [aidec] v164: aiWeaponAttackInRange at 1/2/3/9/26 tiles.
    @Test
    void weaponAttackInRangeVectors() {
        assertEquals(0, CombatAi.aiInit());
        GameObject critter = create(16777558);
        GameObject weapon = create(8);
        GameObject spear = create(7);
        GameObject target = create(16777558);

        assertEquals(0, ObjectSystem.objectSetLocation(critter, 20100, 0));
        assertEquals(0, ObjectSystem.objectSetLocation(target, 20102, 0));
        assertTrue(CombatAi.aiWeaponAttackInRange(critter, weapon, target));
        assertEquals(0, ObjectSystem.objectSetLocation(target, 20126, 0));
        assertFalse(CombatAi.aiWeaponAttackInRange(critter, weapon, target));

        assertEquals(0, ObjectSystem.objectSetLocation(target, 20101, 0));
        assertTrue(CombatAi.aiWeaponAttackInRange(critter, spear, target));
        assertEquals(0, ObjectSystem.objectSetLocation(target, 20103, 0));
        assertTrue(CombatAi.aiWeaponAttackInRange(critter, spear, target));
        assertEquals(0, ObjectSystem.objectSetLocation(target, 20109, 0));
        assertFalse(CombatAi.aiWeaponAttackInRange(critter, spear, target));
    }

    // [aidec] v164: _cai_get_min_hp (null, explicit modes, derived min_hp,
    // out-of-range mode).
    @Test
    void caiGetMinHpVectors() {
        assertEquals(0, CombatAi.aiInit());
        AiPacket ai = CombatAi.aiGetPacketByNum(179);
        assertNotNull(ai);
        int savedMode = ai.runAwayMode;
        int savedMinHp = ai.minHp;

        assertEquals(0, CombatAi.caiGetMinHp(null));
        ai.runAwayMode = CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_NONE.id();
        assertEquals(0, CombatAi.caiGetMinHp(ai));
        ai.runAwayMode = CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_COWARD.id();
        assertEquals(25, CombatAi.caiGetMinHp(ai));
        ai.runAwayMode = CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_NEVER.id();
        assertEquals(0, CombatAi.caiGetMinHp(ai));
        ai.runAwayMode = CombatAiDefs.RunAwayMode.RUN_AWAY_MODE_INVALID.id();
        ai.minHp = 37;
        assertEquals(37, CombatAi.caiGetMinHp(ai));
        ai.runAwayMode = -2;
        assertEquals(0, CombatAi.caiGetMinHp(ai));

        ai.runAwayMode = savedMode;
        ai.minHp = savedMinHp;
    }

    // [aidec] v164: _caiHasWeapPrefType, one bit mask per best_weapon row
    // ordered NONE, UNARMED, MELEE, THROW, RANGED.
    @Test
    void weapPrefVectors() {
        assertEquals(0, CombatAi.aiInit());
        AiPacket ai = CombatAi.aiGetPacketByNum(179);
        assertNotNull(ai);
        int savedBestWeapon = ai.bestWeapon;

        int[] types = {
            Item.AttackType.ATTACK_TYPE_NONE.id(),
            Item.AttackType.ATTACK_TYPE_UNARMED.id(),
            Item.AttackType.ATTACK_TYPE_MELEE.id(),
            Item.AttackType.ATTACK_TYPE_THROW.id(),
            Item.AttackType.ATTACK_TYPE_RANGED.id(),
        };
        int[] expected = { 31, 31, 5, 21, 21, 17, 3, 11, 1 };
        for (int best = -1; best < CombatAiDefs.BEST_WEAPON_COUNT; best++) {
            ai.bestWeapon = best;
            int mask = 0;
            for (int i = 0; i < types.length; i++) {
                if (CombatAi.caiHasWeapPrefType(ai, types[i])) {
                    mask |= 1 << i;
                }
            }
            assertEquals(expected[best + 1], mask, "best_weapon " + best);
        }

        ai.bestWeapon = savedBestWeapon;
    }

    // [aidec] v164: _ai_can_use_weapon (art gate, skill vs min_to_hit,
    // weapon preference). Frame 11 is hmjmps, whose pistol fire-single art
    // exists; the maddog object's own frame 16 has no such art.
    @Test
    void canUseWeaponVectors() {
        assertEquals(0, CombatAi.aiInit());
        GameObject critter = create(16777558);
        GameObject weapon = create(8);
        Proto[] critterProto = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(critter), critterProto);

        int maddogFid = critter.fid;
        critter.fid = hmjmpsFid();
        AiPacket ai = CombatAi.aiGetPacketByNum(179);
        ai.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_RANGED.id();
        ai.minToHit = 90;
        critter.data.critter.combat.aiPacket = 179;

        int guns = SkillDefs.Skill.SKILL_SMALL_GUNS.id();
        int savedSkill = critterProto[0].critter.data.skills[guns];
        int primary = CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id();
        assertFalse(CombatAi.aiCanUseWeapon(critter, weapon, primary));
        critterProto[0].critter.data.skills[guns] = 120;
        assertTrue(CombatAi.aiCanUseWeapon(critter, weapon, primary));

        ai.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_MELEE.id();
        assertFalse(CombatAi.aiCanUseWeapon(critter, weapon, primary));

        ai.bestWeapon = CombatAiDefs.BestWeapon.BEST_WEAPON_RANGED.id();
        critter.fid = maddogFid;
        assertFalse(CombatAi.aiCanUseWeapon(critter, weapon, primary));

        critter.fid = hmjmpsFid();
        critterProto[0].critter.data.skills[guns] = savedSkill;
    }

    // [aidec] v164: critterCanUseWeapon (type gates, crippled arms and
    // two-handed weapons).
    @Test
    void critterCanUseWeaponVectors() {
        assertEquals(0, CombatAi.aiInit());
        GameObject critter = create(16777558);
        GameObject weapon = create(8);
        GameObject rifle = create(10);
        GameObject nonWeapon = create(1);
        critter.fid = hmjmpsFid();
        int primary = CombatDefs.HitMode.HIT_MODE_RIGHT_WEAPON_PRIMARY.id();
        ObjTypes.Dam savedResults = critter.data.critter.combat.results;


        assertFalse(Critter.critterCanUseWeapon(null, weapon, primary));
        assertFalse(Critter.critterCanUseWeapon(critter, null, primary));
        assertFalse(Critter.critterCanUseWeapon(critter, nonWeapon, primary));
        assertTrue(Critter.critterCanUseWeapon(critter, weapon, primary));
        assertTrue(Critter.critterCanUseWeapon(critter, rifle, primary));

        critter.data.critter.combat.results = savedResults.or(ObjTypes.Dam.CRIP_ARM_RIGHT);
        assertFalse(Critter.critterCanUseWeapon(critter, rifle, primary));
        assertTrue(Critter.critterCanUseWeapon(critter, weapon, primary));

        critter.data.critter.combat.results = savedResults.or(ObjTypes.Dam.CRIP_ARM_ANY);
        assertFalse(Critter.critterCanUseWeapon(critter, weapon, primary));

        critter.data.critter.combat.results = savedResults;
    }

    // [aidec] v164: _combatai_check_retaliation (no target, dead, stronger
    // and equal ratings).
    @Test
    void retaliationVectors() {
        assertEquals(0, CombatAi.aiInit());
        GameObject attacker = create(16777558);
        GameObject weak = create(16777558);
        GameObject dead = create(16777558);
        GameObject strong = create(16777558);
        GameObject equal = create(16777558);
        GameObject armed1 = create(10);
        GameObject armed2 = create(10);

        attacker.data.critter.combat.team = 1;
        weak.data.critter.combat.team = 2;
        dead.data.critter.combat.team = 2;
        strong.data.critter.combat.team = 3;
        equal.data.critter.combat.team = 3;
        assertEquals(0, Item.itemAdd(strong, armed1, 1));
        armed1.flags = armed1.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        assertEquals(0, Item.itemAdd(equal, armed2, 1));
        armed2.flags = armed2.flags.or(ObjTypes.ObjectFlags.IN_RIGHT_HAND);
        dead.data.critter.combat.results =
                dead.data.critter.combat.results.or(ObjTypes.Dam.DEAD);

        attacker.data.critter.combat.whoHitMe = null;
        CombatAi.combataiCheckRetaliation(attacker, weak);
        assertSame(weak, attacker.data.critter.combat.whoHitMe);

        attacker.data.critter.combat.whoHitMe = strong;
        CombatAi.combataiCheckRetaliation(attacker, dead);
        assertSame(strong, attacker.data.critter.combat.whoHitMe);

        attacker.data.critter.combat.whoHitMe = weak;
        CombatAi.combataiCheckRetaliation(attacker, strong);
        assertSame(strong, attacker.data.critter.combat.whoHitMe);

        attacker.data.critter.combat.whoHitMe = strong;
        CombatAi.combataiCheckRetaliation(attacker, equal);
        assertSame(strong, attacker.data.critter.combat.whoHitMe);
    }

    private static int hmjmpsFid() {
        return FrmId.ofObject(ObjectType.CRITTER.value(), 11,
                AnimationType.STAND.value(), ArtDefs.WeaponAnimation.None.id(),
                Rotation.NE.value()).fid();
    }
}
