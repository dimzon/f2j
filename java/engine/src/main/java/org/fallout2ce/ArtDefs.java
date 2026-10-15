package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Art frame id tables, mirroring {@code src/art_defs.h}.
 *
 * <p>Enumerator names are kept 1:1 with C++ (PascalCase) for greppability.
 * Aliases resolve to the last declared enumerator ({@code First}/{@code Blank}
 * share id 0, like the C++ cast). {@code TileFlags} is an immutable bit bag
 * (see {@link MapDefs.MapHeaderFlags}); only its {@code |} masks with
 * {@code 0xF}, like C++.
 */
public final class ArtDefs {

    private ArtDefs() {
    }

    public enum HeadFrameId {
        Invalid(-1),
        None(0);

        private final int id;

        HeadFrameId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, HeadFrameId> HEAD_BY_ID = new HashMap<>();

    static {
        for (HeadFrameId e : HeadFrameId.values()) {
            HEAD_BY_ID.put(e.id, e);
        }
    }

    public static HeadFrameId headFrameIdOf(int id) {
        return HEAD_BY_ID.get(id);
    }

    public enum HeadAnimation {
        VeryGoodReaction(0),
        Good(1),
        GoodToNeutral(2),
        NeutralToGood(3),
        Neutral(4),
        NeutralToBad(5),
        BadToNeutral(6),
        Bad(7),
        VeryBadReaction(8),
        GoodPhonemes(9),
        NeutralPhonemes(10),
        BadPhonemes(11);

        private final int id;

        HeadAnimation(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, HeadAnimation> HEAD_ANIM_BY_ID = new HashMap<>();

    static {
        for (HeadAnimation e : HeadAnimation.values()) {
            HEAD_ANIM_BY_ID.put(e.id, e);
        }
    }

    public static HeadAnimation headAnimationOf(int id) {
        return HEAD_ANIM_BY_ID.get(id);
    }

    public enum HeadFidget {
        Invalid(-1),
        Good(1),
        Neutral(4),
        Bad(7);

        private final int id;

        HeadFidget(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, HeadFidget> HEAD_FIDGET_BY_ID = new HashMap<>();

    static {
        for (HeadFidget e : HeadFidget.values()) {
            HEAD_FIDGET_BY_ID.put(e.id, e);
        }
    }

    public static HeadFidget headFidgetOf(int id) {
        return HEAD_FIDGET_BY_ID.get(id);
    }

    public enum HeadFidgetAnimation {
        None(0),
        First(1),
        Second(2),
        Third(3);

        private final int id;

        HeadFidgetAnimation(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, HeadFidgetAnimation> HEAD_FIDGET_ANIM_BY_ID = new HashMap<>();

    static {
        for (HeadFidgetAnimation e : HeadFidgetAnimation.values()) {
            HEAD_FIDGET_ANIM_BY_ID.put(e.id, e);
        }
    }

    public static HeadFidgetAnimation headFidgetAnimationOf(int id) {
        return HEAD_FIDGET_ANIM_BY_ID.get(id);
    }

    public enum BackgroundFrameId {
        Invalid(-1),
        Reserved(0),
        Background(1),
        RustyMetal(2),
        Hub(3),
        Necropolis(4),
        Brotherhood(5),
        MilitaryBase(6),
        JunkTown(7),
        Cathedral(8),
        ShadySands(9),
        Vault(10),
        Master(11),
        Followers(12),
        Raiders(13),
        Cave(14),
        Enclave(15),
        Wasteland(16),
        EnclaveBoss(17),
        President(18),
        Tent(19),
        Adobe(20);

        private final int id;

        BackgroundFrameId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, BackgroundFrameId> BG_BY_ID = new HashMap<>();

    static {
        for (BackgroundFrameId e : BackgroundFrameId.values()) {
            BG_BY_ID.put(e.id, e);
        }
    }

    public static BackgroundFrameId backgroundFrameIdOf(int id) {
        return BG_BY_ID.get(id);
    }

    public enum DudeNativeLook {
        DUDE_NATIVE_LOOK_TRIBAL(0),
        DUDE_NATIVE_LOOK_JUMPSUIT(1);

        private final int id;

        DudeNativeLook(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public static final int DUDE_NATIVE_LOOK_COUNT = 2;

    private static final Map<Integer, DudeNativeLook> DUDE_LOOK_BY_ID = new HashMap<>();

    static {
        for (DudeNativeLook e : DudeNativeLook.values()) {
            DUDE_LOOK_BY_ID.put(e.id, e);
        }
    }

    public static DudeNativeLook dudeNativeLookOf(int id) {
        return DUDE_LOOK_BY_ID.get(id);
    }

    public enum WeaponAnimation {
        None(0),
        Knife(1),
        Club(2),
        Hammer(3),
        Spear(4),
        Pistol(5),
        Smg(6),
        Shotgun(7),
        LaserRifle(8),
        Minigun(9),
        Launcher(10),
        SfallS(11),
        SfallO(12),
        SfallP(13),
        SfallQ(14),
        SfallT(15);

        private final int id;

        WeaponAnimation(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, WeaponAnimation> WEAPON_ANIM_BY_ID = new HashMap<>();

    static {
        for (WeaponAnimation e : WeaponAnimation.values()) {
            WEAPON_ANIM_BY_ID.put(e.id, e);
        }
    }

    public static WeaponAnimation weaponAnimationOf(int id) {
        return WEAPON_ANIM_BY_ID.get(id);
    }

    public static boolean weaponAnimationIsValid(int weaponAnimation) {
        return weaponAnimation >= WeaponAnimation.None.id() && weaponAnimation <= WeaponAnimation.SfallT.id();
    }

    public static boolean weaponAnimationIsValid(WeaponAnimation weaponAnimation) {
        return weaponAnimation != null && weaponAnimationIsValid(weaponAnimation.id());
    }

    public enum CritterFrameId {
        Invalid(-1),
        Reserved(0),
        First(1);

        private final int id;

        CritterFrameId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, CritterFrameId> CRITTER_BY_ID = new HashMap<>();

    static {
        for (CritterFrameId e : CritterFrameId.values()) {
            CRITTER_BY_ID.put(e.id, e);
        }
    }

    public static CritterFrameId critterFrameIdOf(int id) {
        return CRITTER_BY_ID.get(id);
    }

    public enum SceneryFrameId {
        Invalid(-1),
        Reserved(0),
        ForceField3(501);

        private final int id;

        SceneryFrameId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, SceneryFrameId> SCENERY_BY_ID = new HashMap<>();

    static {
        for (SceneryFrameId e : SceneryFrameId.values()) {
            SCENERY_BY_ID.put(e.id, e);
        }
    }

    public static SceneryFrameId sceneryFrameIdOf(int id) {
        return SCENERY_BY_ID.get(id);
    }

    public enum WallFrameId {
        Invalid(-1),
        Reserved(0);

        private final int id;

        WallFrameId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, WallFrameId> WALL_BY_ID = new HashMap<>();

    static {
        for (WallFrameId e : WallFrameId.values()) {
            WALL_BY_ID.put(e.id, e);
        }
    }

    public static WallFrameId wallFrameIdOf(int id) {
        return WALL_BY_ID.get(id);
    }

    public enum ItemFrameId {
        Invalid(-1),
        Reserved(0);

        private final int id;

        ItemFrameId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, ItemFrameId> ITEM_BY_ID = new HashMap<>();

    static {
        for (ItemFrameId e : ItemFrameId.values()) {
            ITEM_BY_ID.put(e.id, e);
        }
    }

    public static ItemFrameId itemFrameIdOf(int id) {
        return ITEM_BY_ID.get(id);
    }

    public enum TileFrameId {
        Invalid(-1),
        Reserved(0),
        Grid(1),
        Last(4095);

        private final int id;

        TileFrameId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, TileFrameId> TILE_BY_ID = new HashMap<>();

    static {
        for (TileFrameId e : TileFrameId.values()) {
            TILE_BY_ID.put(e.id, e);
        }
    }

    public static TileFrameId tileFrameIdOf(int id) {
        return TILE_BY_ID.get(id);
    }

    public static final class TileFlags {
        public static final TileFlags None = new TileFlags(0);
        public static final TileFlags TemporarilyHidden = new TileFlags(1);
        public static final TileFlags AlwaysHidden = new TileFlags(2);

        private final int bits;

        public TileFlags(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public TileFlags not() {
            return new TileFlags(~bits);
        }

        public TileFlags and(TileFlags rhs) {
            return new TileFlags(bits & rhs.bits);
        }

        /** Bitwise or, masked with {@code 0xF} like the C++ operator. */
        public TileFlags or(TileFlags rhs) {
            return new TileFlags((bits | rhs.bits) & 0xF);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TileFlags && ((TileFlags) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "TileFlags(0x" + Integer.toHexString(bits) + ")";
        }
    }

    public enum MiscFrameId {
        Invalid(-1),
        Reserved(0),
        EmpExplosion(2),
        RocketExplosion(10),
        BloodPool(11),
        ScrollBlocker(12),
        FireExplosion(29),
        PlasmaExplosion(31),
        Exit2Grid1(33),
        Exit3Grid8(48);

        private final int id;

        MiscFrameId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, MiscFrameId> MISC_BY_ID = new HashMap<>();

    static {
        for (MiscFrameId e : MiscFrameId.values()) {
            MISC_BY_ID.put(e.id, e);
        }
    }

    public static MiscFrameId miscFrameIdOf(int id) {
        return MISC_BY_ID.get(id);
    }

    public enum SkillDexFrameId {
        Invalid(-1),
        Strength(0),
        Perception(1),
        Endurance(2),
        Charisma(3),
        Intelligence(4),
        Agility(5),
        Luck(6),
        Level(7),
        Experience(8),
        NextLevel(9),
        HitPoints(10),
        Poisoned(11),
        Radiated(12),
        EyeDamage(13),
        CrippledRightArm(14),
        CrippledLeftArm(15),
        CrippledRightLeg(16),
        CrippledLeftLeg(17),
        ArmorClass(18),
        ActionPoints(19),
        CarryWeight(20),
        MeleeDamage(21),
        DamageResistance(22),
        PoisonResistance(23),
        Sequence(24),
        HealingRate(25),
        CriticalChance(26),
        Skills(27),
        SmallGuns(28),
        BigGuns(29),
        EnergyWeapons(30),
        Unarmed(31),
        MeleeWeapons(32),
        Throwing(33),
        FirstAid(34),
        Doctor(35),
        Sneak(36),
        Lockpick(37),
        Steal(38),
        Traps(39),
        Science(40),
        Repair(41),
        Speech(42),
        Barter(43),
        Gambling(44),
        Outdoorsman(45),
        Kills(46),
        Karma(47),
        Reputation(48),
        AlcoholAddiction(52),
        DrugAddiction(53),
        Traits(54),
        FastMetabolism(55),
        Bruiser(56),
        SmallFrame(57),
        OneHander(58),
        Finesse(59),
        Kamikaze(60),
        HeavyHanded(61),
        FastShot(62),
        BloodyMess(63),
        Jinxed(64),
        GoodNatured(65),
        DrugAddict(66),
        DrugResistant(67),
        Skilled(69),
        Gifted(70),
        Perks(71),
        Awareness(72),
        BonusHandToHandAttacks(73),
        BonusHandToHandDamage(74),
        BonusMove(75),
        BonusRangedDamage(76),
        BonusRateOfFire(77),
        EarlierSequence(78),
        FasterHealing(79),
        MoreCriticals(80),
        NightVision(81),
        Presence(82),
        RadResistance(83),
        Toughness(84),
        PackAnimal(85),
        Sharpshooter(86),
        SilentRunning(87),
        Survivalist(88),
        MasterTrader(89),
        Educated(90),
        Healer(91),
        FortuneFinder(92),
        BetterCriticals(93),
        Empathy(94),
        Slayer(95),
        Sniper(96),
        SilentDeath(97),
        ActionBoy(98),
        MentalBlock(99),
        Lifegiver(100),
        Dodger(101),
        Snakeater(102),
        MrFixit(103),
        Medic(104),
        MasterThief(105),
        Speaker(106),
        HeaveHo(107),
        FriendlyFoe(108),
        Pickpocket(109),
        Ghost(110),
        CultOfPersonality(111),
        Scrounger(112),
        Explorer(113),
        FlowerChild(114),
        Pathfinder(115),
        AnimalFriend(116),
        Scout(117),
        MysteriousStranger(118),
        Ranger(119),
        QuickPockets(120),
        SmoothTalker(121),
        SwiftLearner(122),
        Tag(123),
        Mutate(124),
        Betrayer(125),
        BuffoutAddiction(126),
        DefenderReputation(127),
        DemonReputation(128),
        DespairReputation(129),
        Expert(130),
        FighterReputation(131),
        Gigolo(132),
        GraveRobber(133),
        GuardianReputation(134),
        IdolizedReputation(135),
        JetAddiction(136),
        LikedReputation(137),
        MadeManReputation(138),
        MarriedReputation(139),
        MentatsAddiction(140),
        NeutralReputation(141),
        NukaColaAddiction(142),
        PsychoAddiction(144),
        RadawayAddiction(145),
        TragicCardGameAddiction(149),
        VillifiedReputation(150),
        HatedReputation(153),
        GenericVaultGuy(154),
        AdrenalineRush(155),
        CautiousNature(156),
        DermalArmor(157),
        GeckoSkinning(158),
        HandToHandEvade(159),
        Harmless(160),
        HereAndNow(161),
        KarmaBeacon(162),
        KamaSutraMaster(163),
        LightStep(164),
        LivingAnatomy(165),
        MagneticPersonality(166),
        PackRat(167),
        PhoenixArmor(168),
        Pyromaniac(169),
        QuickRecovery(170),
        Stonewall(171),
        VaultCityInoculations(172),
        WeaponHandling(173);

        private final int id;

        SkillDexFrameId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, SkillDexFrameId> SKILLDEX_BY_ID = new HashMap<>();

    static {
        for (SkillDexFrameId e : SkillDexFrameId.values()) {
            SKILLDEX_BY_ID.put(e.id, e);
        }
    }

    public static SkillDexFrameId skillDexFrameIdOf(int id) {
        return SKILLDEX_BY_ID.get(id);
    }

    public enum InterfaceFrameId {
        Invalid(-1),
        First(0),
        Last(4095),
        Blank(0),
        HexMouseCursor(1),
        Egg(2),
        ExitGridMarker(3),
        BigRedButtonUp(6),
        BigRedButtonDown(7),
        LittleRedButtonUp(8),
        LittleRedButtonDown(9),
        AutomapButtonDown(10),
        AutomapButtonUp(13),
        MainInterface(16),
        OptionsButtonDown(17),
        OptionsButtonUp(18),
        SingleAttackBigDown(31),
        SingleAttackBigUp(32),
        CharacterEditorTraitsFolder(38),
        BurstText(40),
        KickText(41),
        PunchText(42),
        SingleText(43),
        SwingText(44),
        ThrustText(45),
        InventoryButtonDown(46),
        InventoryButtonUp(47),
        InventoryBox(48),
        InventoryButtonOutUp(49),
        InventoryButtonInUp(50),
        InventoryButtonOutDown(51),
        InventoryButtonInDown(52),
        InventoryButtonUpDisabled(53),
        InventoryButtonDownDisabled(54),
        CharacterButtonDown(56),
        CharacterButtonUp(57),
        PipBoyButtonDown(58),
        PipBoyButtonUp(59),
        SingleAttackBigUpUnreadied(73),
        HitPointsNumbers(82),
        HealthGreenLight(83),
        HealthYellowLight(84),
        HealthRedLight(85),
        PerkDialogBackground(86),
        DialogBigDownArrowUp(87),
        DialogBigDownArrowDown(88),
        DialogBigUpArrowUp(89),
        DialogBigUpArrowDown(90),
        DialogBigDoneButtonUp(91),
        DialogBigDoneButtonDown(92),
        DialogDownButtonUp(93),
        DialogDownButtonDown(94),
        DialogRedButtonDown(95),
        DialogRedButtonUp(96),
        DialogRestButtonUp(97),
        DialogRestButtonDown(98),
        DialogTalkSubwindow(99),
        DialogUpButtonUp(100),
        DialogUpButtonDown(101),
        DialogReviewBackground(102),
        DialogScreenBackground(103),
        EndTurnWindowAnimation(104),
        EndTurnButtonUp(105),
        EndTurnButtonDown(106),
        EndCombatButtonUp(107),
        EndCombatButtonDown(108),
        EndTurnGreenLight(109),
        EndTurnRedLight(110),
        BarterWindow(111),
        UseItemOnWindow(113),
        LootContainerWindow(114),
        DialogueUpperHighlight(115),
        DialogueLowerHighlight(116),
        ThrowText(117),
        CalledShotWindow(118),
        SkilldexOnButton(119),
        SkilldexOffButton(120),
        SkilldexWindow(121),
        LeftArrowUp(122),
        LeftArrowDown(123),
        RightArrowUp(124),
        RightArrowDown(125),
        WarningBox(126),
        PipBoyWindow(127),
        PipBoyNoteAboutVats(128),
        PipBoyMonthStrings(129),
        PipBoyNoteNumbers(130),
        PipBoySleepAlarmIn(131),
        PipBoySleepAlarmOut(132),
        PipBoyLogo(133),
        WorldMapDialogBox(136),
        WorldMapLocationMarker(138),
        WorldMapMoveTargetMarker1(139),
        MainMenuBackgroundImage(140),
        MapElevatorButtonIn(141),
        MapElevatorButtonOut(142),
        MapElevatorBos(143),
        MapElevatorMast1(144),
        MapElevatorMast2(145),
        MapElevatorMil1(146),
        MapElevatorMil2(147),
        MapElevatorVault(148),
        MapElevatorGaj000(149),
        MapElevatorBos2(150),
        MapElevatorMil3(151),
        WorldMapFightIcon1(154),
        WorldMapFightIcon2(155),
        TownMapHotspot1(168),
        CharacterEditorCreateBackground(169),
        BigNum(170),
        AutomapWindow(171),
        AutoUp(172),
        AutoDown(173),
        CharacterSelectorBackground(174),
        CharacterEditorAgeMask(175),
        CharacterEditorAgeOff(176),
        CharacterEditorEditBackground(177),
        CharacterEditorKarmaFolder(178),
        CharacterEditorKillsFolder(179),
        CharacterEditorPerksFolder(180),
        CharacterEditorDownArrowOff(181),
        CharacterEditorDownArrowOn(182),
        CharacterEditorNameMask(183),
        CharacterEditorNameOn(184),
        CharacterEditorNameOff(185),
        CharacterEditorFolderMask(186),
        CharacterEditorSexMask(187),
        CharacterEditorSexOff(188),
        CharacterEditorSexOn(189),
        CharacterEditorSlider(190),
        CharacterEditorSpecialNegativeOff(191),
        CharacterEditorSpecialNegativeOn(192),
        CharacterEditorSpecialPositiveOff(193),
        CharacterEditorSpecialPositiveOn(194),
        CharacterEditorStrengthNegativeOff(195),
        CharacterEditorStrengthNegativeOn(196),
        CharacterEditorStrengthPositiveOff(197),
        CharacterEditorStrengthPositiveOn(198),
        CharacterEditorUpArrowOff(199),
        CharacterEditorUpArrowOn(200),
        PremadeCharacterCombat(201),
        PremadeCharacterStealth(202),
        PremadeCharacterDiplomat(203),
        CharacterEditorAgeOn(204),
        CharacterEditorAgeBox(205),
        CharacterEditorAttributesBox(206),
        CharacterEditorAttributesWindow(207),
        CharacterWindow(208),
        DoneBox(209),
        CharacterEditorFemaleOff(210),
        CharacterEditorFemaleOn(211),
        CharacterEditorMaleOff(212),
        CharacterEditorMaleOn(213),
        CharacterEditorNameBox(214),
        CharacterEditorTaggedSkillsOff(215),
        CharacterEditorTaggedSkillsOn(216),
        LargeDialog(217),
        MediumDialog(218),
        CharacterEditorBarArrows(219),
        OptionsBase(220),
        OptionsButtonOff(221),
        OptionsButtonOn(222),
        TownMapHotspot2(223),
        LoadBox(224),
        SaveBox(225),
        PipBoyBomb(226),
        LoadSaveGame(237),
        LoadSaveBox(238),
        LoadSaveCover(239),
        PreferencesScreen(240),
        PreferencesSliderOff(241),
        PreferencesBackground(242),
        PreferencesBackgroundLeft(243),
        PreferencesXIn(244),
        PreferencesXOut(245),
        PreferencesCover(246),
        PreferencesSliderOn(247),
        ActionMove(249),
        ActionArrow(250),
        ActionCrosshair(251),
        CancelHighlighted(252),
        Cancel(253),
        DropHighlighted(254),
        Drop(255),
        InventoryHighlighted(256),
        Inventory(257),
        LookHighlighted(258),
        Look(259),
        RotateHighlighted(260),
        Rotate(261),
        TalkHighlighted(262),
        Talk(263),
        UseGetHighlighted(264),
        UseGet(265),
        MouseCursorBlank(266),
        MouseCursorStandardArrow(267),
        MouseCursorSmallUpArrow(268),
        MouseCursorSmallDownArrow(269),
        MouseCursorScrollNorthWest(270),
        MouseCursorScrollNorth(271),
        MouseCursorScrollNorthEast(272),
        MouseCursorScrollEast(273),
        MouseCursorScrollSouthEast(274),
        MouseCursorScrollSouth(275),
        MouseCursorScrollSouthWest(276),
        MouseCursorScrollWest(277),
        MouseCursorWaitPlanet(278),
        MouseCursorCrosshair(279),
        MouseCursorPlus(280),
        MouseCursorDestroy(281),
        ActionPick(282),
        ActionMenu(283),
        ActionToHit(284),
        ActionArrowMirrored(285),
        PointingHand(286),
        Bullseye(288),
        MovementPointText(289),
        MovementPointsNumbers(290),
        ReloadText(291),
        UseText(292),
        MouseCursorUseCrosshair(293),
        UseOnText(294),
        MouseCursorWaitWatch(295),
        HelpBackground(297),
        MainMenuButtonUp(299),
        MainMenuButtonDown(300),
        UnloadHighlighted(301),
        Unload(302),
        SkillHighlighted(303),
        Skill(304),
        MoveMultipleItemsInterface(305),
        TimerOverlay(306),
        AllButtonPressed(307),
        AllButtonUnpressed(308),
        DeathScene(309),
        MouseCursorSingleFrameWatch(310),
        PanningDesertImage(327),
        MouseCursorInvalidScrollEast(328),
        MouseCursorInvalidScrollNorthEast(329),
        MouseCursorInvalidScrollNorthWest(330),
        MouseCursorInvalidScrollNorth(331),
        MouseCursorInvalidScrollSouthEast(332),
        MouseCursorInvalidScrollSouthWest(333),
        MouseCursorInvalidScrollSouth(334),
        MouseCursorInvalidScrollWest(335),
        WorldSphereOverlay0(336),
        WorldSphereOverlay1(337),
        WorldSphereOverlay2(338),
        WorldMapOverlayScreen(363),
        WorldMapTownTabsUnderlay(364),
        WorldMapNightDayDial(365),
        WorldMapGlobeStampOverlay(366),
        WorldMapTownTabsEdgingOverlay(367),
        MapElevatorSierraBase(388),
        DialogTalkSubwindowParty(389),
        PartyControlInterface(390),
        PartyCustomInterface(391),
        PartyAggressiveDown(392),
        PartyAggressiveDisabled(393),
        PartyAggressiveUp(394),
        PartyBerserkDown(395),
        PartyBerserkDisabled(396),
        PartyBerserkUp(397),
        PartyCowardDown(398),
        PartyCowardDisabled(399),
        PartyCowardUp(400),
        PartyCustomDown(401),
        PartyCustomDisabled(402),
        PartyCustomUp(403),
        PartyDefensiveDown(404),
        PartyDefensiveDisabled(405),
        PartyDefensiveUp(406),
        PartyAttackDown(407),
        PartyAttackUp(408),
        PartyBurstDown(409),
        PartyBurstUp(410),
        PartyChemDown(411),
        PartyChemUp(412),
        PartyDistanceDown(413),
        PartyDistanceUp(414),
        PartyRunDown(415),
        PartyRunUp(416),
        PartyWeaponDown(417),
        PartyWeaponUp(418),
        PartyCustomSelect(419),
        TradeWindow(420),
        ChopPunch(421),
        DeathBlossomKick(422),
        DragonPunch(423),
        ForcePunch(424),
        HammerPunch(425),
        HipKick(426),
        JumpKick(427),
        LightningPunch(428),
        RoundhouseKick(429),
        StrongKick(430),
        SnapKick(431),
        StrongPunch(432),
        WorldMapCarMovie(433),
        PushHighlighted(434),
        Push(435),
        InventoryLootAllUp(436),
        InventoryLootAllDown(437),
        WorldMapRandomEncounterCursor2Bright(438),
        WorldMapRandomEncounterCursor2Dark(439);

        private final int id;

        InterfaceFrameId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, InterfaceFrameId> IFACE_BY_ID = new HashMap<>();

    static {
        for (InterfaceFrameId e : InterfaceFrameId.values()) {
            IFACE_BY_ID.put(e.id, e);
        }
    }

    public static InterfaceFrameId interfaceFrameIdOf(int id) {
        return IFACE_BY_ID.get(id);
    }
}
