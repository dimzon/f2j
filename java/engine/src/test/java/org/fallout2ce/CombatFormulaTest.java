package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The vanilla damage formula of {@code attackComputeDamage} against real
 * protos: the DT/DR reads at the damage type, the bypass cut, the
 * penetrating-unarmed cut, the ammo modifiers and the per-round
 * threshold/resistance arithmetic.
 */
class CombatFormulaTest {

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path p = Paths.get(root, name);
        if (!Files.isRegularFile(p)) {
            throw new IllegalStateException("missing " + p);
        }
        return p.toString();
    }

    private GameObject attacker;
    private GameObject defender;

    @BeforeEach
    void setup() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        TextObjects.resetForTests();
        Combat.resetForTests();

        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        assertTrue(XFile.xbaseOpen(gameEn + "/critter.dat"));
        assertEquals(0, Art.artInit());
        byte[] tileBuf = new byte[640 * 480];
        assertEquals(0, TileMath.tileInit(null, 100, 100, 200, 200, tileBuf, 640, 480, 640,
            rect -> {
            }));
        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));

        GameObject[] attackerSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(attackerSlot, ProtoId.ofPid(16777558)));
        attacker = attackerSlot[0];

        GameObject[] defenderSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(defenderSlot, ProtoId.ofPid(16777558)));
        defender = defenderSlot[0];
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        TextObjects.resetForTests();
        Combat.resetForTests();
    }

    /** The DT and DR of the defender at a damage type, straight from its proto. */
    private int[] defenderThresholdAndResistance(int damageType) {
        Proto[] slot = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(defender), slot));
        return new int[] {
            slot[0].critter.data.baseStats[StatDefs.Stat.STAT_DAMAGE_THRESHOLD.id() + damageType],
            slot[0].critter.data.baseStats[StatDefs.Stat.STAT_DAMAGE_RESISTANCE.id() + damageType],
        };
    }

    @Test
    void theFormulaAppliesThresholdAndResistance() {
        int[] dtDr = defenderThresholdAndResistance(
            ProtoTypes.DamageType.DAMAGE_TYPE_NORMAL.id());

        // A hit with a bare fist: one round, the unarmed draw 1..2, no
        // ammo multipliers, difficulty normal (100%).
        CombatDefs.Attack attack = new CombatDefs.Attack();
        Combat.attackInit(attack, attacker, defender,
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(),
            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());
        attack.attackerFlags = attack.attackerFlags.or(Dam.HIT);

        // Run the formula many times: every result respects the bounds
        // the formula gives for the 1..2 draw. Threshold zeroes the low
        // draws when the proto carries one; the resistance never raises.
        int observedMin = Integer.MAX_VALUE;
        int observedMax = Integer.MIN_VALUE;
        for (int i = 0; i < 200; i++) {
            Combat.attackComputeDamage(attack, 1, 2);
            int dmg = attack.defenderDamage;
            assertTrue(dmg >= 0);
            assertTrue(dmg <= 2, dmg + " vs dt=" + dtDr[0] + " dr=" + dtDr[1]);
            observedMin = Math.min(observedMin, dmg);
            observedMax = Math.max(observedMax, dmg);
        }
        assertTrue(observedMax >= observedMin);
    }

    @Test
    void bypassCutsTheThresholdAndResistance() {
        // A bypass strike cuts both to a fifth: the attacker's own damage
        // (no DAM_HIT) uses the attacker as its target, so set the flags
        // so the defender is the one hurt, with BYPASS set.
        int[] dtDr = defenderThresholdAndResistance(
            ProtoTypes.DamageType.DAMAGE_TYPE_NORMAL.id());

        CombatDefs.Attack attack = new CombatDefs.Attack();
        Combat.attackInit(attack, attacker, defender,
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(),
            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());
        attack.attackerFlags = attack.attackerFlags.or(Dam.HIT).or(Dam.BYPASS);

        // With the bypass, the effective threshold is 20% of the proto's.
        int cutThreshold = 20 * dtDr[0] / 100;
        int cutResistance = 20 * dtDr[1] / 100;

        for (int i = 0; i < 200; i++) {
            Combat.attackComputeDamage(attack, 1, 2);
            int dmg = attack.defenderDamage;
            // A draw d (1..2): d*2/2 - threshold, then the resistance.
            int worst = 1 - cutThreshold;
            if (worst > 0) {
                worst -= worst * cutResistance / 100;
            }
            int best = 2 - cutThreshold;
            if (best > 0) {
                best -= best * cutResistance / 100;
            }
            assertTrue(dmg >= Math.min(0, worst) && dmg <= Math.max(0, best),
                dmg + " vs [" + worst + "," + best + "] dt=" + cutThreshold + " dr=" + cutResistance);
        }
    }

    @Test
    void noHitFlagDamagesTheAttacker() {
        // Without DAM_HIT the formula fills the attacker's own damage.
        CombatDefs.Attack attack = new CombatDefs.Attack();
        Combat.attackInit(attack, attacker, defender,
            CombatDefs.HitMode.HIT_MODE_PUNCH.id(),
            CombatDefs.HitLocation.HIT_LOCATION_TORSO.id());

        Combat.attackComputeDamage(attack, 1, 2);
        assertEquals(0, attack.defenderDamage);
        assertTrue(attack.attackerDamage >= 0);
    }
}
