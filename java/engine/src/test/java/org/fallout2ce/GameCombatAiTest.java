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
 * [ai], golden/v163.txt: the AI packet table parse (data/ai.txt), the packet
 * lookup/readers and the threat rating, pinned against the real C++ aiInit
 * + aiGetPacketByNum + _combatai_rating + aiGetRunAwayMode.
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
}
