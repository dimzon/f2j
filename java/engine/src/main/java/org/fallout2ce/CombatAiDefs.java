package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Combat AI id tables, mirroring {@code src/combat_ai_defs.h} (pure enums;
 * the {@code obj_types.h} include is unused by the header body).
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note;
 * postfix {@code --} becomes {@link RunAwayMode#prev()} /
 * {@link Disposition#prev()}.
 */
public final class CombatAiDefs {

    private CombatAiDefs() {
    }

    public static final int AREA_ATTACK_MODE_COUNT = 5;
    public static final int AREA_ATTACK_MODE_FIRST = 0;

    public enum AreaAttackMode {
        AREA_ATTACK_MODE_INVALID(-1),
        AREA_ATTACK_MODE_ALWAYS(0),
        AREA_ATTACK_MODE_SOMETIMES(1),
        AREA_ATTACK_MODE_BE_SURE(2),
        AREA_ATTACK_MODE_BE_CAREFUL(3),
        AREA_ATTACK_MODE_BE_ABSOLUTELY_SURE(4);

        private final int id;

        AreaAttackMode(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, AreaAttackMode> AREA_BY_ID = new HashMap<>();

    static {
        for (AreaAttackMode e : AreaAttackMode.values()) {
            AREA_BY_ID.put(e.id, e);
        }
    }

    public static AreaAttackMode ofAreaAttackMode(int id) {
        return AREA_BY_ID.get(id);
    }

    public static boolean areaAttackModeIsValid(int mode) {
        return mode >= AREA_ATTACK_MODE_FIRST && mode < AREA_ATTACK_MODE_COUNT;
    }

    public static final int RUN_AWAY_MODE_COUNT = 7;
    public static final int RUN_AWAY_MODE_FIRST = 0;
    public static final int RUN_AWAY_MODE_LAST = 6;

    public enum RunAwayMode {
        RUN_AWAY_MODE_INVALID(-1),
        RUN_AWAY_MODE_NONE(0),
        RUN_AWAY_MODE_COWARD(1),
        RUN_AWAY_MODE_FINGER_HURTS(2),
        RUN_AWAY_MODE_BLEEDING(3),
        RUN_AWAY_MODE_NOT_FEELING_GOOD(4),
        RUN_AWAY_MODE_TOURNIQUET(5),
        RUN_AWAY_MODE_NEVER(6);

        private final int id;

        RunAwayMode(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public RunAwayMode next() {
            return CombatAiDefs.ofRunAwayMode(id + 1);
        }

        /** Previous id (C++ post-{@code --} step), or null past the start. */
        public RunAwayMode prev() {
            return CombatAiDefs.ofRunAwayMode(id - 1);
        }
    }

    private static final Map<Integer, RunAwayMode> RUNAWAY_BY_ID = new HashMap<>();

    static {
        for (RunAwayMode e : RunAwayMode.values()) {
            RUNAWAY_BY_ID.put(e.id, e);
        }
    }

    public static RunAwayMode ofRunAwayMode(int id) {
        return RUNAWAY_BY_ID.get(id);
    }

    public static boolean runAwayModeIsValid(int mode) {
        return mode >= RUN_AWAY_MODE_FIRST && mode < RUN_AWAY_MODE_COUNT;
    }

    public static final int BEST_WEAPON_COUNT = 8;
    public static final int BEST_WEAPON_FIRST = 0;

    public enum BestWeapon {
        BEST_WEAPON_INVALID(-1),
        BEST_WEAPON_NO_PREF(0),
        BEST_WEAPON_MELEE(1),
        BEST_WEAPON_MELEE_OVER_RANGED(2),
        BEST_WEAPON_RANGED_OVER_MELEE(3),
        BEST_WEAPON_RANGED(4),
        BEST_WEAPON_UNARMED(5),
        BEST_WEAPON_UNARMED_OVER_THROW(6),
        BEST_WEAPON_RANDOM(7);

        private final int id;

        BestWeapon(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, BestWeapon> BEST_BY_ID = new HashMap<>();

    static {
        for (BestWeapon e : BestWeapon.values()) {
            BEST_BY_ID.put(e.id, e);
        }
    }

    public static BestWeapon ofBestWeapon(int id) {
        return BEST_BY_ID.get(id);
    }

    public static boolean bestWeaponIsValid(int weapon) {
        return weapon >= BEST_WEAPON_FIRST && weapon < BEST_WEAPON_COUNT;
    }

    public static final int DISTANCE_COUNT = 5;
    public static final int DISTANCE_FIRST = 0;

    public enum DistanceMode {
        DISTANCE_INVALID(-1),
        DISTANCE_STAY_CLOSE(0),
        DISTANCE_CHARGE(1),
        DISTANCE_SNIPE(2),
        DISTANCE_ON_YOUR_OWN(3),
        DISTANCE_STAY(4);

        private final int id;

        DistanceMode(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, DistanceMode> DIST_BY_ID = new HashMap<>();

    static {
        for (DistanceMode e : DistanceMode.values()) {
            DIST_BY_ID.put(e.id, e);
        }
    }

    public static DistanceMode ofDistanceMode(int id) {
        return DIST_BY_ID.get(id);
    }

    public static boolean distanceModeIsValid(int mode) {
        return mode >= DISTANCE_FIRST && mode < DISTANCE_COUNT;
    }

    public static final int ATTACK_WHO_COUNT = 5;
    public static final int ATTACK_WHO_FIRST = 0;

    public enum AttackWho {
        ATTACK_WHO_INVALID(-1),
        ATTACK_WHO_WHOMEVER_ATTACKING_ME(0),
        ATTACK_WHO_STRONGEST(1),
        ATTACK_WHO_WEAKEST(2),
        ATTACK_WHO_WHOMEVER(3),
        ATTACK_WHO_CLOSEST(4);

        private final int id;

        AttackWho(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, AttackWho> WHO_BY_ID = new HashMap<>();

    static {
        for (AttackWho e : AttackWho.values()) {
            WHO_BY_ID.put(e.id, e);
        }
    }

    public static AttackWho ofAttackWho(int id) {
        return WHO_BY_ID.get(id);
    }

    public static boolean attackWhoIsValid(int who) {
        return who >= ATTACK_WHO_FIRST && who < ATTACK_WHO_COUNT;
    }

    public static final int CHEM_USE_COUNT = 6;
    public static final int CHEM_USE_FIRST = 0;

    public enum ChemUse {
        CHEM_USE_INVALID(-1),
        CHEM_USE_CLEAN(0),
        CHEM_USE_STIMS_WHEN_HURT_LITTLE(1),
        CHEM_USE_STIMS_WHEN_HURT_LOTS(2),
        CHEM_USE_SOMETIMES(3),
        CHEM_USE_ANYTIME(4),
        CHEM_USE_ALWAYS(5);

        private final int id;

        ChemUse(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, ChemUse> CHEM_BY_ID = new HashMap<>();

    static {
        for (ChemUse e : ChemUse.values()) {
            CHEM_BY_ID.put(e.id, e);
        }
    }

    public static ChemUse ofChemUse(int id) {
        return CHEM_BY_ID.get(id);
    }

    public static boolean chemUseIsValid(int use) {
        return use >= CHEM_USE_FIRST && use < CHEM_USE_COUNT;
    }

    public static final int DISPOSITION_COUNT = 6;
    public static final int DISPOSITION_FIRST = 0;

    public enum Disposition {
        DISPOSITION_INVALID(-1),
        DISPOSITION_NONE(0),
        DISPOSITION_CUSTOM(1),
        DISPOSITION_COWARD(2),
        DISPOSITION_DEFENSIVE(3),
        DISPOSITION_AGGRESSIVE(4),
        DISPOSITION_BERKSERK(5);

        private final int id;

        Disposition(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Previous id (C++ post-{@code --} step), or null past the start. */
        public Disposition prev() {
            return CombatAiDefs.ofDisposition(id - 1);
        }
    }

    private static final Map<Integer, Disposition> DISP_BY_ID = new HashMap<>();

    static {
        for (Disposition e : Disposition.values()) {
            DISP_BY_ID.put(e.id, e);
        }
    }

    public static Disposition ofDisposition(int id) {
        return DISP_BY_ID.get(id);
    }

    public static boolean dispositionIsValid(int disposition) {
        return disposition >= DISPOSITION_FIRST && disposition < DISPOSITION_COUNT;
    }

    public static final int HURT_COUNT = 4;
    public static final int HURT_FIRST = 0;

    public enum HurtTooMuch {
        HURT_BLIND(0),
        HURT_CRIPPLED(1),
        HURT_CRIPPLED_LEGS(2),
        HURT_CRIPPLED_ARMS(3);

        private final int id;

        HurtTooMuch(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, HurtTooMuch> HURT_BY_ID = new HashMap<>();

    static {
        for (HurtTooMuch e : HurtTooMuch.values()) {
            HURT_BY_ID.put(e.id, e);
        }
    }

    public static HurtTooMuch ofHurtTooMuch(int id) {
        return HURT_BY_ID.get(id);
    }

    /** Port of {@code PerceptionType} ({@code src/sfall_script_hooks.h}). */
    public enum PerceptionType {
        PERCEPTION_OTHER(0),
        PERCEPTION_SEE(1),
        PERCEPTION_HEAR(2),
        PERCEPTION_AI_TARGET(3);

        private final int id;

        PerceptionType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    /** Port of {@code PerceptionResult} ({@code src/sfall_script_hooks.h}). */
    public enum PerceptionResult {
        PERCEPTION_OUT_OF_RANGE(0),
        PERCEPTION_IN_RANGE(1),
        PERCEPTION_FORCE(2);

        private final int id;

        PerceptionResult(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }
}
