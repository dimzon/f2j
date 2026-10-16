package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Trait id table, mirroring {@code src/trait_defs.h}.
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 */
public final class TraitDefs {

    public static final int TRAITS_MAX_SELECTED_COUNT = 2;

    public static final int TRAIT_COUNT = 16;
    public static final int TRAIT_FIRST = 0;

    private TraitDefs() {
    }

    public enum Trait {
        TRAIT_INVALID(-1),
        TRAIT_FAST_METABOLISM(0),
        TRAIT_BRUISER(1),
        TRAIT_SMALL_FRAME(2),
        TRAIT_ONE_HANDER(3),
        TRAIT_FINESSE(4),
        TRAIT_KAMIKAZE(5),
        TRAIT_HEAVY_HANDED(6),
        TRAIT_FAST_SHOT(7),
        TRAIT_BLOODY_MESS(8),
        TRAIT_JINXED(9),
        TRAIT_GOOD_NATURED(10),
        TRAIT_CHEM_RELIANT(11),
        TRAIT_CHEM_RESISTANT(12),
        TRAIT_SEX_APPEAL(13),
        TRAIT_SKILLED(14),
        TRAIT_GIFTED(15);

        private final int id;

        Trait(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null past the end. */
        public Trait next() {
            return TraitDefs.of(id + 1);
        }
    }

    private static final Map<Integer, Trait> BY_ID = new HashMap<>();

    static {
        for (Trait trait : Trait.values()) {
            BY_ID.put(trait.id, trait);
        }
    }

    public static Trait of(int id) {
        return BY_ID.get(id);
    }

    public static boolean traitIsValid(int trait) {
        return trait >= TRAIT_FIRST && trait < TRAIT_COUNT;
    }

    // Selection state + hardcoded modifiers, mirroring the pure parts of
    // `src/trait.cc`. `traitsInit` is NOT ported: it needs the trait.msg
    // message list and segfaults headless, like `statsInit` (names/pics stay
    // null, as in the harness, which never calls it either).

    // List of selected traits (`gSelectedTraits`, 0x66BE40). BSS mirror:
    // a fresh process/class-load reads {0, 0} = (FAST_METABOLISM,
    // FAST_METABOLISM), exactly like the C BSS zeros. The golden vectors show
    // sel0=-1,-1 only because the harness calls `traitsReset()` first;
    // tests must do the same (see `resetForTests`).
    private static final int[] SELECTED_TRAITS = new int[TRAITS_MAX_SELECTED_COUNT];

    // `traitHardcodeDisabled[]` (`src/trait.cc`): set only by the sfall
    // perks-file loader (`traitsLoadSfallConfig`), which the harness never
    // runs, so every entry stays false here and `traitIsSelectedAndActive`
    // needs no script state — it is valid && selected && !disabled.
    private static final boolean[] TRAIT_HARDCODE_DISABLED = new boolean[TRAIT_COUNT];

    /**
     * Compile-time {@code frmId} column of {@code traitDescriptions} (and
     * {@code defaultTraitFrmIds}) in {@code src/trait.cc} (index = trait
     * id), as raw skilldex frame ids. Transcribed verbatim:
     * FastMetabolism(55) Bruiser(56) SmallFrame(57) OneHander(58)
     * Finesse(59) Kamikaze(60) HeavyHanded(61) FastShot(62) BloodyMess(63)
     * Jinxed(64) GoodNatured(65) ChemReliant&rarr;DrugAddict(66)
     * ChemResistant&rarr;DrugResistant(67) SexAppeal&rarr;Empathy(94)
     * Skilled(69) Gifted(70). The static initializer (not BSS zero, not
     * sfall) is the {@code Gifted=70} source the {@code [frmids]} harness
     * observes headless ({@code traitsInit} never runs there either).
     */
    private static final int[] TRAIT_FRM_IDS = {
        ArtDefs.SkillDexFrameId.FastMetabolism.id(), // TRAIT_FAST_METABOLISM
        ArtDefs.SkillDexFrameId.Bruiser.id(), // TRAIT_BRUISER
        ArtDefs.SkillDexFrameId.SmallFrame.id(), // TRAIT_SMALL_FRAME
        ArtDefs.SkillDexFrameId.OneHander.id(), // TRAIT_ONE_HANDER
        ArtDefs.SkillDexFrameId.Finesse.id(), // TRAIT_FINESSE
        ArtDefs.SkillDexFrameId.Kamikaze.id(), // TRAIT_KAMIKAZE
        ArtDefs.SkillDexFrameId.HeavyHanded.id(), // TRAIT_HEAVY_HANDED
        ArtDefs.SkillDexFrameId.FastShot.id(), // TRAIT_FAST_SHOT
        ArtDefs.SkillDexFrameId.BloodyMess.id(), // TRAIT_BLOODY_MESS
        ArtDefs.SkillDexFrameId.Jinxed.id(), // TRAIT_JINXED
        ArtDefs.SkillDexFrameId.GoodNatured.id(), // TRAIT_GOOD_NATURED
        ArtDefs.SkillDexFrameId.DrugAddict.id(), // TRAIT_CHEM_RELIANT
        ArtDefs.SkillDexFrameId.DrugResistant.id(), // TRAIT_CHEM_RESISTANT
        ArtDefs.SkillDexFrameId.Empathy.id(), // TRAIT_SEX_APPEAL
        ArtDefs.SkillDexFrameId.Skilled.id(), // TRAIT_SKILLED
        ArtDefs.SkillDexFrameId.Gifted.id(), // TRAIT_GIFTED
    };

    // Port of `traitsReset` (0x4B3AF8 trait_reset): stores TRAIT_INVALID in
    // both slots. NOTE: there is no "zero" reset in C; reset means INVALID.
    public static void traitsReset() {
        java.util.Arrays.fill(SELECTED_TRAITS, Trait.TRAIT_INVALID.id());
    }

    // Port of `traitsSetSelected` (0x4B3B48 trait_set).
    public static void traitsSetSelected(int trait1, int trait2) {
        SELECTED_TRAITS[0] = trait1;
        SELECTED_TRAITS[1] = trait2;
    }

    // Port of `traitsGetSelected` (0x4B3B54 trait_get): out[0]/out[1], like
    // the C out-params (cf. `Db.dbGetFileSize` int[] style).
    public static void traitsGetSelected(int[] out) {
        out[0] = SELECTED_TRAITS[0];
        out[1] = SELECTED_TRAITS[1];
    }

    // Port of `traitIsSelected` (0x4B3BC8 trait_level): raw slot equality,
    // no validity gating (INVALID slots match only trait -1, like C).
    public static boolean traitIsSelected(int trait) {
        return SELECTED_TRAITS[0] == trait || SELECTED_TRAITS[1] == trait;
    }

    // Port of `traitIsSelectedAndActive`: valid && selected && sfall
    // hardcode not disabled. No script/game state involved.
    public static boolean traitIsSelectedAndActive(int trait) {
        return traitIsValid(trait) && traitIsSelected(trait) && !TRAIT_HARDCODE_DISABLED[trait];
    }

    /**
     * Port of `traitGetFrmId` (0x4B3BA8 trait_pic, `src/trait.cc`): valid
     * trait yields the compile-time `frmId` frame id, invalid trait yields
     * `SkillDexFrameId::Invalid` (`-1`).
     */
    public static int traitGetFrmId(int trait) {
        return traitIsValid(trait)
            ? TRAIT_FRM_IDS[trait]
            : ArtDefs.SkillDexFrameId.Invalid.id();
    }

    // Port of `traitGetStatModifier` (0x4B3C7C trait_adjust_stat).
    //
    // The sfall pair-bonus tables (`traitStatBonuses`, filled only by
    // `traitsLoadSfallConfig` when a perks file enables `[Traits]`) stay all
    // zeros in the harness: `gSfallConfig` is zero-init there, so the loader
    // early-returns and `traitOverridesEnabled` stays false. The compiled-in
    // tables likewise contain no Gifted values — the effective +1s below are
    // hardcoded switch branches in C, transcribed here verbatim.
    //
    // NOTE: the `gDude`-dependent branches (ARMOR_CLASS zeroes armor via
    // `critterGetBaseStat`, CARRY_WEIGHT scales with Strength,
    // RADIATION/POISON_RESISTANCE subtract base resists) are omitted: they
    // need the Critter/game phase. Unreachable from the golden path
    // (Gifted/FastShot + STRENGTH).
    public static int traitGetStatModifier(int stat) {
        int modifier = 0;

        boolean gifted = traitIsSelectedAndActive(Trait.TRAIT_GIFTED.id());
        boolean bruiser = traitIsSelectedAndActive(Trait.TRAIT_BRUISER.id());
        boolean smallFrame = traitIsSelectedAndActive(Trait.TRAIT_SMALL_FRAME.id());
        boolean kamikaze = traitIsSelectedAndActive(Trait.TRAIT_KAMIKAZE.id());
        boolean heavyHanded = traitIsSelectedAndActive(Trait.TRAIT_HEAVY_HANDED.id());
        boolean finesse = traitIsSelectedAndActive(Trait.TRAIT_FINESSE.id());
        boolean fastMetabolism = traitIsSelectedAndActive(Trait.TRAIT_FAST_METABOLISM.id());

        switch (stat) {
        case 0: // STAT_STRENGTH
            if (gifted) {
                modifier += 1;
            }
            if (bruiser) {
                modifier += 2;
            }
            break;
        case 1: // STAT_PERCEPTION
            if (gifted) {
                modifier += 1;
            }
            break;
        case 2: // STAT_ENDURANCE
            if (gifted) {
                modifier += 1;
            }
            break;
        case 3: // STAT_CHARISMA
            if (gifted) {
                modifier += 1;
            }
            break;
        case 4: // STAT_INTELLIGENCE
            if (gifted) {
                modifier += 1;
            }
            break;
        case 5: // STAT_AGILITY
            if (gifted) {
                modifier += 1;
            }
            if (smallFrame) {
                modifier += 1;
            }
            break;
        case 6: // STAT_LUCK
            if (gifted) {
                modifier += 1;
            }
            break;
        case 8: // STAT_MAXIMUM_ACTION_POINTS
            if (bruiser) {
                modifier -= 2;
            }
            break;
        case 11: // STAT_MELEE_DAMAGE
            if (heavyHanded) {
                modifier += 4;
            }
            break;
        case 13: // STAT_SEQUENCE
            if (kamikaze) {
                modifier += 5;
            }
            break;
        case 14: // STAT_HEALING_RATE
            if (fastMetabolism) {
                modifier += 2;
            }
            break;
        case 15: // STAT_CRITICAL_CHANCE
            if (finesse) {
                modifier += 10;
            }
            break;
        case 16: // STAT_BETTER_CRITICALS
            if (heavyHanded) {
                modifier -= 30;
            }
            break;
        default:
            break;
        }

        return modifier;
    }

    // Port of `traitGetSkillModifier` (0x4B40FC trait_adjust_skill): Gifted
    // -10 on every skill, GoodNatured -10 combat / +15 social. Same sfall
    // note as `traitGetStatModifier`: the override table stays zero and
    // disabled in the harness, so only the hardcoded branches apply.
    public static int traitGetSkillModifier(int skill) {
        int modifier = 0;

        if (traitIsSelectedAndActive(Trait.TRAIT_GIFTED.id())) {
            modifier -= 10;
        }

        if (traitIsSelectedAndActive(Trait.TRAIT_GOOD_NATURED.id())) {
            switch (skill) {
            case 0: // SKILL_SMALL_GUNS
            case 1: // SKILL_BIG_GUNS
            case 2: // SKILL_ENERGY_WEAPONS
            case 3: // SKILL_UNARMED
            case 4: // SKILL_MELEE_WEAPONS
            case 5: // SKILL_THROWING
                modifier -= 10;
                break;
            case 6: // SKILL_FIRST_AID
            case 7: // SKILL_DOCTOR
            case 14: // SKILL_SPEECH
            case 15: // SKILL_BARTER
                modifier += 15;
                break;
            default:
                break;
            }
        }

        return modifier;
    }

    // Port of `traitsSave` (0x4B3B28 trait_save): the two selection slots as
    // big-endian int32s. `Db` has no Int32EnumList helpers, so this uses the
    // Int32List pair (EventQueue/CombatSkill save precedent); layout is
    // identical (C `fileWriteInt32EnumList` writes raw int32s).
    public static int traitsSave(XFile.Handle stream) {
        return Db.fileWriteInt32List(stream, SELECTED_TRAITS, 0, TRAITS_MAX_SELECTED_COUNT);
    }

    // Port of `traitsLoad` (0x4B3B08 trait_load): reads back `traitsSave`.
    public static int traitsLoad(XFile.Handle stream) {
        return Db.fileReadInt32List(stream, SELECTED_TRAITS, 0, TRAITS_MAX_SELECTED_COUNT);
    }

    // Test seam: mirrors `traitsReset` (INVALID slots), NOT zero-init —
    // matching C, where reset and BSS-zero are distinct states.
    static void resetForTests() {
        traitsReset();
    }
}
