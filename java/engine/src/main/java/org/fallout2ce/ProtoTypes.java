package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * Prototype tables, mirroring {@code src/proto_types.h}.
 *
 * <p>See {@link StatDefs} for the {@code operator++} iteration note.
 * Union overlays ({@code ItemProtoData}, {@code Proto}) keep all variants
 * as fields. The C++ template {@code MapProtoTypeIdToObjectType} becomes
 * the {@code objectTypeOf*} overloads; the template {@code ProtoId} ctors
 * become the {@code ProtoId.of*} factories.
 */
public final class ProtoTypes {

    private ProtoTypes() {
    }

    public static final int PROTO_LIST_EXTENT_SIZE = 16;
    public static final int PROTO_LIST_MAX_ENTRIES = 512;

    public enum Gender {
        GENDER_MALE(0),
        GENDER_FEMALE(1);

        private final int id;

        Gender(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public static final int GENDER_COUNT = 2;

    private static final Map<Integer, Gender> GENDER_BY_ID = new HashMap<>();

    static {
        for (Gender e : Gender.values()) {
            GENDER_BY_ID.put(e.id, e);
        }
    }

    public static Gender ofGender(int id) {
        return GENDER_BY_ID.get(id);
    }

    public static final int ITEM_TYPE_COUNT = 7;
    public static final int ITEM_TYPE_FIRST = 0;

    public enum ItemType {
        ITEM_TYPE_INVALID(-1),
        ITEM_TYPE_ARMOR(0),
        ITEM_TYPE_CONTAINER(1),
        ITEM_TYPE_DRUG(2),
        ITEM_TYPE_WEAPON(3),
        ITEM_TYPE_AMMO(4),
        ITEM_TYPE_MISC(5),
        ITEM_TYPE_KEY(6);

        private final int id;

        ItemType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public ItemType next() {
            return ProtoTypes.ofItemType(id + 1);
        }
    }

    private static final Map<Integer, ItemType> ITEMTYPE_BY_ID = new HashMap<>();

    static {
        for (ItemType e : ItemType.values()) {
            ITEMTYPE_BY_ID.put(e.id, e);
        }
    }

    public static ItemType ofItemType(int id) {
        return ITEMTYPE_BY_ID.get(id);
    }

    public static final int SCENERY_TYPE_COUNT = 6;
    public static final int SCENERY_TYPE_FIRST = 0;

    public enum SceneryType {
        SCENERY_TYPE_DOOR(0),
        SCENERY_TYPE_STAIRS(1),
        SCENERY_TYPE_ELEVATOR(2),
        SCENERY_TYPE_LADDER_UP(3),
        SCENERY_TYPE_LADDER_DOWN(4),
        SCENERY_TYPE_GENERIC(5);

        private final int id;

        SceneryType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public SceneryType next() {
            return ProtoTypes.ofSceneryType(id + 1);
        }
    }

    private static final Map<Integer, SceneryType> SCENERYTYPE_BY_ID = new HashMap<>();

    static {
        for (SceneryType e : SceneryType.values()) {
            SCENERYTYPE_BY_ID.put(e.id, e);
        }
    }

    public static SceneryType ofSceneryType(int id) {
        return SCENERYTYPE_BY_ID.get(id);
    }

    public static final int MATERIAL_TYPE_COUNT = 8;
    public static final int MATERIAL_TYPE_FIRST = 0;

    public enum MaterialType {
        MATERIAL_TYPE_INVALID(-1),
        MATERIAL_TYPE_GLASS(0),
        MATERIAL_TYPE_METAL(1),
        MATERIAL_TYPE_PLASTIC(2),
        MATERIAL_TYPE_WOOD(3),
        MATERIAL_TYPE_DIRT(4),
        MATERIAL_TYPE_STONE(5),
        MATERIAL_TYPE_CEMENT(6),
        MATERIAL_TYPE_LEATHER(7);

        private final int id;

        MaterialType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public MaterialType next() {
            return ProtoTypes.ofMaterialType(id + 1);
        }
    }

    private static final Map<Integer, MaterialType> MATERIAL_BY_ID = new HashMap<>();

    static {
        for (MaterialType e : MaterialType.values()) {
            MATERIAL_BY_ID.put(e.id, e);
        }
    }

    public static MaterialType ofMaterialType(int id) {
        return MATERIAL_BY_ID.get(id);
    }

    public static final int DAMAGE_TYPE_COUNT = 7;
    public static final int DAMAGE_TYPE_FIRST = 0;

    public enum DamageType {
        DAMAGE_TYPE_NORMAL(0),
        DAMAGE_TYPE_LASER(1),
        DAMAGE_TYPE_FIRE(2),
        DAMAGE_TYPE_PLASMA(3),
        DAMAGE_TYPE_ELECTRICAL(4),
        DAMAGE_TYPE_EMP(5),
        DAMAGE_TYPE_EXPLOSION(6);

        private final int id;

        DamageType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public DamageType next() {
            return ProtoTypes.ofDamageType(id + 1);
        }
    }

    private static final Map<Integer, DamageType> DAMAGE_BY_ID = new HashMap<>();

    static {
        for (DamageType e : DamageType.values()) {
            DAMAGE_BY_ID.put(e.id, e);
        }
    }

    public static DamageType ofDamageType(int id) {
        return DAMAGE_BY_ID.get(id);
    }

    public static boolean damageTypeIsValid(int damageType) {
        return damageType >= DAMAGE_TYPE_FIRST && damageType < DAMAGE_TYPE_COUNT;
    }

    /** Port of {@code Stat operator+(Stat, DamageType)} (DT/DR indexing). */
    public static StatDefs.Stat statPlusDamageType(StatDefs.Stat stat, DamageType damageType) {
        return StatDefs.ofStat(stat.id() + damageType.id());
    }

    public static final int CALIBER_TYPE_COUNT = 19;
    public static final int CALIBER_TYPE_FIRST = 0;

    public enum CaliberType {
        CALIBER_NONE(0),
        CALIBER_ROCKET(1),
        CALIBER_FLAMETHROWER_FUEL(2),
        CALIBER_C_ENERGY_CELL(3),
        CALIBER_D_ENERGY_CELL(4),
        CALIBER_223(5),
        CALIBER_5MM(6),
        CALIBER_40CAL(7),
        CALIBER_10MM(8),
        CALIBER_44CAL(9),
        CALIBER_14MM(10),
        CALIBER_12GAUGE(11),
        CALIBER_9MM(12),
        CALIBER_BB(13),
        CALIBER_45CAL(14),
        CALIBER_2MM(15),
        CALIBER_47MM_CASELESS(16),
        CALIBER_NH_NEEDLER(17),
        CALIBER_762(18);

        private final int id;

        CaliberType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public CaliberType next() {
            return ProtoTypes.ofCaliberType(id + 1);
        }
    }

    private static final Map<Integer, CaliberType> CALIBER_BY_ID = new HashMap<>();

    static {
        for (CaliberType e : CaliberType.values()) {
            CALIBER_BY_ID.put(e.id, e);
        }
    }

    public static CaliberType ofCaliberType(int id) {
        return CALIBER_BY_ID.get(id);
    }

    public static final int RACE_TYPE_COUNT = 2;
    public static final int RACE_TYPE_FIRST = 0;

    public enum RaceType {
        RACE_TYPE_CAUCASIAN(0),
        RACE_TYPE_AFRICAN(1);

        private final int id;

        RaceType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public RaceType next() {
            return ProtoTypes.ofRaceType(id + 1);
        }
    }

    private static final Map<Integer, RaceType> RACE_BY_ID = new HashMap<>();

    static {
        for (RaceType e : RaceType.values()) {
            RACE_BY_ID.put(e.id, e);
        }
    }

    public static RaceType ofRaceType(int id) {
        return RACE_BY_ID.get(id);
    }

    public static final int BODY_TYPE_COUNT = 3;
    public static final int BODY_TYPE_FIRST = 0;

    public enum BodyType {
        BODY_TYPE_BIPED(0),
        BODY_TYPE_QUADRUPED(1),
        BODY_TYPE_ROBOTIC(2);

        private final int id;

        BodyType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public BodyType next() {
            return ProtoTypes.ofBodyType(id + 1);
        }
    }

    private static final Map<Integer, BodyType> BODY_BY_ID = new HashMap<>();

    static {
        for (BodyType e : BodyType.values()) {
            BODY_BY_ID.put(e.id, e);
        }
    }

    public static BodyType ofBodyType(int id) {
        return BODY_BY_ID.get(id);
    }

    public static final int KILL_TYPE_DEFAULT_COUNT = 19;
    public static final int KILL_TYPE_OVERRIDE_COUNT = KILL_TYPE_DEFAULT_COUNT * 2;
    public static final int KILL_TYPE_PLAYER = KILL_TYPE_OVERRIDE_COUNT;
    public static final int KILL_TYPE_FIRST = 0;

    public enum KillType {
        KILL_TYPE_INVALID(-1),
        KILL_TYPE_MAN(0),
        KILL_TYPE_WOMAN(1),
        KILL_TYPE_CHILD(2),
        KILL_TYPE_SUPER_MUTANT(3),
        KILL_TYPE_GHOUL(4),
        KILL_TYPE_BRAHMIN(5),
        KILL_TYPE_RADSCORPION(6),
        KILL_TYPE_RAT(7),
        KILL_TYPE_FLOATER(8),
        KILL_TYPE_CENTAUR(9),
        KILL_TYPE_ROBOT(10),
        KILL_TYPE_DOG(11),
        KILL_TYPE_MANTIS(12),
        KILL_TYPE_DEATH_CLAW(13),
        KILL_TYPE_PLANT(14),
        KILL_TYPE_GECKO(15),
        KILL_TYPE_ALIEN(16),
        KILL_TYPE_GIANT_ANT(17),
        KILL_TYPE_BIG_BAD_BOSS(18);

        private final int id;

        KillType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public KillType next() {
            return ProtoTypes.ofKillType(id + 1);
        }
    }

    private static final Map<Integer, KillType> KILL_BY_ID = new HashMap<>();

    static {
        for (KillType e : KillType.values()) {
            KILL_BY_ID.put(e.id, e);
        }
    }

    public static KillType ofKillType(int id) {
        return KILL_BY_ID.get(id);
    }

    public static boolean killTypeIsValid(int killType) {
        return killType >= KILL_TYPE_FIRST && killType < KILL_TYPE_DEFAULT_COUNT;
    }

    public static boolean killTypeOverrideIsValid(int killType) {
        // Like C++: the player kill type (== OVERRIDE_COUNT) counts as valid.
        return killType >= KILL_TYPE_FIRST && killType <= KILL_TYPE_OVERRIDE_COUNT;
    }

    public enum ItemProtoTypeId {
        Reserved(0),
        PowerArmor(3),
        SmallEnergyCell(38),
        MicroFusionCell(39),
        Stimpak(40),
        Money(41),
        FirstAidKit(47),
        Radaway(48),
        Dynamite(51),
        GeigerCounter(52),
        Mentats(53),
        StealthBoy(54),
        MotionSensor(59),
        BigBookOfScience(73),
        DeansElectronics(76),
        Flare(79),
        FirstAidBook(80),
        PlasticExplosives(85),
        ScoutHandBook(86),
        Buffout(87),
        DoctorsBag(91),
        GunsAndBullets(102),
        NukaCola(106),
        Psycho(110),
        Beer(124),
        Booze(125),
        SuperStimpak(144),
        MolotovCocktail(159),
        LitFlare(205),
        ArmedDynamite(206),
        ActivatedGeigerCounter(207),
        ArmedPlasticExplosives(209),
        ActivatedStealthBoy(210),
        HardenedPowerArmor(232),
        Jet(259),
        JetAntidote(260),
        HealingPowder(273),
        DeckOfTragicCards(304),
        CatsPawIssue5(331),
        AdvancedPowerArmor(348),
        AdvancedPowerArmorMkII(349),
        Shiv(383),
        SolarScorcher(390),
        SuperCattleProd(399),
        MegaPowerFist(407),
        FieldMedicsFirstAidKit(408),
        ParamedicsBag(409),
        RamirezBoxClosed(431),
        MirroredShades(433),
        RaidersMap(444),
        CarTrunk(455),
        JesseContainer(467),
        PipBoyLingualEnhancer(499),
        PipBoyMedicalEnhancer(516),
        SurveyMap(523);

        private final int id;

        ItemProtoTypeId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, ItemProtoTypeId> ITEMID_BY_ID = new HashMap<>();

    static {
        for (ItemProtoTypeId e : ItemProtoTypeId.values()) {
            ITEMID_BY_ID.put(e.id, e);
        }
    }

    public static ItemProtoTypeId ofItemProtoTypeId(int id) {
        return ITEMID_BY_ID.get(id);
    }

    public enum CritterProtoTypeId {
        Reserved(0),
        Dude(0),
        GunGuardFemale(47),
        Cyberdog(136),
        Goris(152),
        Marcus(161),
        EnclavePatrolMale(250),
        Dogmeat(342),
        PariahDog(384),
        AutoCannon(480);

        private final int id;

        CritterProtoTypeId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, CritterProtoTypeId> CRITTERID_BY_ID = new HashMap<>();

    static {
        for (CritterProtoTypeId e : CritterProtoTypeId.values()) {
            CRITTERID_BY_ID.put(e.id, e);
        }
    }

    public static CritterProtoTypeId ofCritterProtoTypeId(int id) {
        return CRITTERID_BY_ID.get(id);
    }

    public enum SceneryProtoTypeId {
        Reserved(0),
        ExitGridAutomapMarker(49),
        BrotherhoodDoor(153),
        BlockingHexAutomap(344),
        ElevatorDoor(421),
        ElevatorDoorAlternate(470),
        ForceFieldNorthSouth(491),
        FirstRadioactiveGoo(985),
        SecondRadioactiveGoo(986),
        ThirdRadioactiveGoo(987),
        LastRadioactiveGoo(988),
        Car(1009),
        ElevatorStub(1293);

        private final int id;

        SceneryProtoTypeId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, SceneryProtoTypeId> SCENERYID_BY_ID = new HashMap<>();

    static {
        for (SceneryProtoTypeId e : SceneryProtoTypeId.values()) {
            SCENERYID_BY_ID.put(e.id, e);
        }
    }

    public static SceneryProtoTypeId ofSceneryProtoTypeId(int id) {
        return SCENERYID_BY_ID.get(id);
    }

    public enum MiscProtoTypeId {
        Reserved(0),
        Blood(4),
        Id0x0C(12),
        FirstExitGrid(16),
        LastExitGrid(23);

        private final int id;

        MiscProtoTypeId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, MiscProtoTypeId> MISCID_BY_ID = new HashMap<>();

    static {
        for (MiscProtoTypeId e : MiscProtoTypeId.values()) {
            MISCID_BY_ID.put(e.id, e);
        }
    }

    public static MiscProtoTypeId ofMiscProtoTypeId(int id) {
        return MISCID_BY_ID.get(id);
    }

    public enum WallProtoTypeId {
        Reserved(0);

        private final int id;

        WallProtoTypeId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public static WallProtoTypeId ofWallProtoTypeId(int id) {
        return id == 0 ? WallProtoTypeId.Reserved : null;
    }

    public enum TileProtoTypeId {
        Reserved(0);

        private final int id;

        TileProtoTypeId(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public static TileProtoTypeId ofTileProtoTypeId(int id) {
        return id == 0 ? TileProtoTypeId.Reserved : null;
    }

    /** Port of {@code ProtoFlags}: immutable bit bag. */
    public static final class ProtoFlags {
        public static final ProtoFlags NONE = new ProtoFlags(0x00);
        public static final ProtoFlags FLAT = new ProtoFlags(0x08);
        public static final ProtoFlags NO_BLOCK = new ProtoFlags(0x10);
        public static final ProtoFlags MULTIHEX = new ProtoFlags(0x800);
        public static final ProtoFlags NO_HIGHLIGHT = new ProtoFlags(0x1000);
        public static final ProtoFlags TRANS_RED = new ProtoFlags(0x4000);
        public static final ProtoFlags TRANS_NONE = new ProtoFlags(0x8000);
        public static final ProtoFlags TRANS_WALL = new ProtoFlags(0x10000);
        public static final ProtoFlags TRANS_GLASS = new ProtoFlags(0x20000);
        public static final ProtoFlags TRANS_STEAM = new ProtoFlags(0x40000);
        public static final ProtoFlags TRANS_ENERGY = new ProtoFlags(0x80000);
        public static final ProtoFlags WALL_TRANS_END = new ProtoFlags(0x10000000);
        public static final ProtoFlags LIGHT_THRU = new ProtoFlags(0x20000000);
        public static final ProtoFlags SHOOT_THRU = new ProtoFlags(0x80000000);

        private final int bits;

        public ProtoFlags(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public ProtoFlags and(ProtoFlags rhs) {
            return new ProtoFlags(bits & rhs.bits);
        }

        public ProtoFlags or(ProtoFlags rhs) {
            return new ProtoFlags(bits | rhs.bits);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ProtoFlags && ((ProtoFlags) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "ProtoFlags(0x" + Integer.toHexString(bits) + ")";
        }
    }

    /** Port of {@code ProtoExtendedFlags}: immutable bit bag. */
    public static final class ProtoExtendedFlags {
        public static final ProtoExtendedFlags NONE = new ProtoExtendedFlags(0x0000);
        public static final ProtoExtendedFlags BIG_GUN = new ProtoExtendedFlags(0x0100);
        public static final ProtoExtendedFlags IS_TWO_HANDED = new ProtoExtendedFlags(0x0200);
        public static final ProtoExtendedFlags CAN_USE = new ProtoExtendedFlags(0x0800);
        public static final ProtoExtendedFlags CAN_USE_ON = new ProtoExtendedFlags(0x1000);
        public static final ProtoExtendedFlags LOOK = new ProtoExtendedFlags(0x2000);
        public static final ProtoExtendedFlags CAN_TALK_TO = new ProtoExtendedFlags(0x4000);
        public static final ProtoExtendedFlags CAN_PICK_UP = new ProtoExtendedFlags(0x8000);
        public static final ProtoExtendedFlags HIDDEN = new ProtoExtendedFlags(0x08000000);
        public static final ProtoExtendedFlags MAGIC_HANDS_GROUND = new ProtoExtendedFlags(0x0001);
        public static final ProtoExtendedFlags NORTH_CORNER = new ProtoExtendedFlags(0x10000000);
        public static final ProtoExtendedFlags SOUTH_CORNER = new ProtoExtendedFlags(0x20000000);
        public static final ProtoExtendedFlags EAST_CORNER = new ProtoExtendedFlags(0x40000000);
        public static final ProtoExtendedFlags WEST_CORNER = new ProtoExtendedFlags(0x80000000);

        private final int bits;

        public ProtoExtendedFlags(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public ProtoExtendedFlags or(ProtoExtendedFlags rhs) {
            return new ProtoExtendedFlags(bits | rhs.bits);
        }

        public ProtoExtendedFlags xor(ProtoExtendedFlags rhs) {
            return new ProtoExtendedFlags(bits ^ rhs.bits);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ProtoExtendedFlags && ((ProtoExtendedFlags) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "ProtoExtendedFlags(0x" + Integer.toHexString(bits) + ")";
        }
    }

    /** Port of {@code ProtoItemArmorData}. */
    public static final class ProtoItemArmorData {
        public int armorClass;
        public final int[] damageResistance = new int[7];
        public final int[] damageThreshold = new int[7];
        public PerkDefs.Perk perk;
        public int maleFid;
        public int femaleFid;
    }

    /** Port of {@code ProtoItemContainerData}. */
    public static final class ProtoItemContainerData {
        public int maxSize;
        public int openFlags;
    }

    /** Port of {@code ProtoItemDrugData}. */
    public static final class ProtoItemDrugData {
        public final StatDefs.Stat[] stat = new StatDefs.Stat[3];
        public final int[] amount = new int[3];
        public int duration1;
        public final int[] amount1 = new int[3];
        public int duration2;
        public final int[] amount2 = new int[3];
        public int addictionChance;
        public PerkDefs.Perk withdrawalEffect;
        public int withdrawalOnset;
    }

    /** Port of {@code ProtoItemWeaponData}. */
    public static final class ProtoItemWeaponData {
        public ArtDefs.WeaponAnimation animationCode;
        public int minDamage;
        public int maxDamage;
        public DamageType damageType;
        public int maxRange1;
        public int maxRange2;
        public int projectilePid;
        public int minStrength;
        public int actionPointCost1;
        public int actionPointCost2;
        public int criticalFailureType;
        public PerkDefs.Perk perk;
        public int rounds;
        public CaliberType caliber;
        public int ammoTypePid;
        public int ammoCapacity;
        public int soundCode;
    }

    /** Port of {@code ProtoItemAmmoData}. */
    public static final class ProtoItemAmmoData {
        public CaliberType caliber;
        public int quantity;
        public int armorClassModifier;
        public int damageResistanceModifier;
        public int damageMultiplier;
        public int damageDivisor;
    }

    /** Port of {@code ProtoItemMiscData}. */
    public static final class ProtoItemMiscData {
        public int powerTypePid;
        public int powerType;
        public int charges;
    }

    /** Port of {@code ProtoItemKeyData}. */
    public static final class ProtoItemKeyData {
        public int keyCode;
    }

    /**
     * Port of {@code ItemProtoData}. The C union overlays the variants;
     * Java keeps all of them (only the active one is used).
     */
    public static final class ItemProtoData {
        public final int[] unknown = new int[7];
        public final ProtoItemArmorData armor = new ProtoItemArmorData();
        public final ProtoItemContainerData container = new ProtoItemContainerData();
        public final ProtoItemDrugData drug = new ProtoItemDrugData();
        public final ProtoItemWeaponData weapon = new ProtoItemWeaponData();
        public final ProtoItemAmmoData ammo = new ProtoItemAmmoData();
        public final ProtoItemMiscData misc = new ProtoItemMiscData();
        public final ProtoItemKeyData key = new ProtoItemKeyData();
    }

    /** Port of {@code ItemProto}. */
    public static final class ItemProto {
        public int pid;
        public int messageId;
        public int fid;
        public int lightDistance;
        public int lightIntensity;
        public ProtoFlags flags = ProtoFlags.NONE;
        public ProtoExtendedFlags extendedFlags = ProtoExtendedFlags.NONE;
        public int sid;
        public ItemType type;
        public final ItemProtoData data = new ItemProtoData();
        public MaterialType material;
        public int size;
        public int weight;
        public int cost;
        public int inventoryFid;
        public int soundId;
    }

    /** Port of {@code CritterProtoData}. */
    public static final class CritterProtoData {
        public ObjTypes.CritterFlags flags = ObjTypes.CritterFlags.NONE;
        public final int[] baseStats = new int[StatDefs.SAVEABLE_STAT_COUNT];
        public final int[] bonusStats = new int[StatDefs.SAVEABLE_STAT_COUNT];
        public final int[] skills = new int[SkillDefs.SKILL_COUNT];
        public BodyType bodyType;
        public int experience;
        public KillType killType;
        public DamageType damageType;
    }

    /** Port of {@code CritterProto}. */
    public static final class CritterProto {
        public int pid;
        public int messageId;
        public int fid;
        public int lightDistance;
        public int lightIntensity;
        public ProtoFlags flags = ProtoFlags.NONE;
        public ProtoExtendedFlags extendedFlags = ProtoExtendedFlags.NONE;
        public int sid;
        public final CritterProtoData data = new CritterProtoData();
        public int headFid;
        public int aiPacket;
        public int team;
    }

    /** Port of {@code SceneryProtoDoorData}. */
    public static final class SceneryProtoDoorData {
        public int openFlags;
        public int keyCode;
    }

    /** Port of {@code SceneryProtoStairsData}. */
    public static final class SceneryProtoStairsData {
        public int destinationBuiltTile;
        public WorldmapDefs.Map destinationMap;
    }

    /** Port of {@code SceneryProtoElevatorData}. */
    public static final class SceneryProtoElevatorData {
        public int type;
        public int level;
    }

    /** Port of {@code SceneryProtoLadderData}. */
    public static final class SceneryProtoLadderData {
        public int destinationBuiltTile;
    }

    /** Port of {@code SceneryProtoGenericData}. */
    public static final class SceneryProtoGenericData {
        public int genericFlags;
    }

    /**
     * Port of {@code SceneryProtoData}. The C union overlays the variants;
     * Java keeps all of them (only the active one is used).
     */
    public static final class SceneryProtoData {
        public final SceneryProtoDoorData door = new SceneryProtoDoorData();
        public final SceneryProtoStairsData stairs = new SceneryProtoStairsData();
        public final SceneryProtoElevatorData elevator = new SceneryProtoElevatorData();
        public final SceneryProtoLadderData ladder = new SceneryProtoLadderData();
        public final SceneryProtoGenericData generic = new SceneryProtoGenericData();
    }

    /** Port of {@code SceneryProto}. */
    public static final class SceneryProto {
        public int pid;
        public int messageId;
        public int fid;
        public int lightDistance;
        public int lightIntensity;
        public ProtoFlags flags = ProtoFlags.NONE;
        public ProtoExtendedFlags extendedFlags = ProtoExtendedFlags.NONE;
        public int sid;
        public SceneryType type;
        public final SceneryProtoData data = new SceneryProtoData();
        public MaterialType material;
        public int field30;
        public int soundId;
    }

    /** Port of {@code WallProto}. */
    public static final class WallProto {
        public int pid;
        public int messageId;
        public int fid;
        public int lightDistance;
        public int lightIntensity;
        public ProtoFlags flags = ProtoFlags.NONE;
        public ProtoExtendedFlags extendedFlags = ProtoExtendedFlags.NONE;
        public int sid;
        public MaterialType material;
    }

    /** Port of {@code TileProto}. */
    public static final class TileProto {
        public int pid;
        public int messageId;
        public int fid;
        public ProtoFlags flags = ProtoFlags.NONE;
        public ProtoExtendedFlags extendedFlags = ProtoExtendedFlags.NONE;
        public int sid;
        public MaterialType material;
    }

    /** Port of {@code MiscProto}. */
    public static final class MiscProto {
        public int pid;
        public int messageId;
        public int fid;
        public int lightDistance;
        public int lightIntensity;
        public ProtoFlags flags = ProtoFlags.NONE;
        public ProtoExtendedFlags extendedFlags = ProtoExtendedFlags.NONE;
        public int sid;
    }

    /**
     * Port of {@code Proto}. The C union overlays all variants on the
     * common header; Java keeps everything (only the active variant plus
     * the header fields are used).
     */
    public static final class Proto {
        public int pid;
        public int messageId;
        public int fid;
        public int lightDistance;
        public int lightIntensity;
        public ProtoFlags flags = ProtoFlags.NONE;
        public ProtoExtendedFlags extendedFlags = ProtoExtendedFlags.NONE;
        public int sid;
        public final ItemProto item = new ItemProto();
        public final CritterProto critter = new CritterProto();
        public final SceneryProto scenery = new SceneryProto();
        public final WallProto wall = new WallProto();
        public final TileProto tile = new TileProto();
        public final MiscProto misc = new MiscProto();
    }

    /** Port of {@code ProtoListExtent}. */
    public static final class ProtoListExtent {
        public final Proto[] proto = new Proto[PROTO_LIST_EXTENT_SIZE];
        public int length;
        public ProtoListExtent next;
    }

    /** Port of {@code ProtoList}. */
    public static final class ProtoList {
        public ProtoListExtent head;
        public ProtoListExtent tail;
        public int length;
        public int maxEntriesNum;
    }

    /** Port of {@code MapProtoTypeIdToObjectType} (type-level mapping). */
    public static ObjTypes.ObjectType objectTypeOf(ItemProtoTypeId id) {
        return ObjTypes.ObjectType.OBJ_TYPE_ITEM;
    }

    /** Port of {@code MapProtoTypeIdToObjectType} (type-level mapping). */
    public static ObjTypes.ObjectType objectTypeOf(CritterProtoTypeId id) {
        return ObjTypes.ObjectType.OBJ_TYPE_CRITTER;
    }

    /** Port of {@code MapProtoTypeIdToObjectType} (type-level mapping). */
    public static ObjTypes.ObjectType objectTypeOf(SceneryProtoTypeId id) {
        return ObjTypes.ObjectType.OBJ_TYPE_SCENERY;
    }

    /** Port of {@code MapProtoTypeIdToObjectType} (type-level mapping). */
    public static ObjTypes.ObjectType objectTypeOf(WallProtoTypeId id) {
        return ObjTypes.ObjectType.OBJ_TYPE_WALL;
    }

    /** Port of {@code MapProtoTypeIdToObjectType} (type-level mapping). */
    public static ObjTypes.ObjectType objectTypeOf(TileProtoTypeId id) {
        return ObjTypes.ObjectType.OBJ_TYPE_TILE;
    }

    /** Port of {@code MapProtoTypeIdToObjectType} (type-level mapping). */
    public static ObjTypes.ObjectType objectTypeOf(MiscProtoTypeId id) {
        return ObjTypes.ObjectType.OBJ_TYPE_MISC;
    }

    /**
     * Port of {@code ProtoId} (pid packing: 4 unused bits, 4 object-type
     * bits, 24 proto-id bits). The template ctors become the
     * {@code of*} factories.
     */
    public static final class ProtoId {
        public static final int EMPTY_PID = -1;
        public static final int INVALID_PROTO_ID = -1;
        public static final int MIN_PROTO_ID = 0;
        public static final int MAX_PROTO_ID = 16777215;

        private static final int PROTO_ID_MASK = 0x00FFFFFF;
        private static final int OBJECT_TYPE_MASK = 0x0F000000;
        private static final int OBJECT_TYPE_MASK_POSITION = 24;

        private final ObjTypes.ObjectType objectType;
        private final int pid;
        private final int protoId;

        private ProtoId(ObjTypes.ObjectType objectType, int pid, int protoId) {
            this.objectType = objectTypeIsValidProto(objectType) ? objectType
                    : ObjTypes.ObjectType.OBJ_TYPE_INVALID;
            this.pid = pid;
            this.protoId = protoId < MIN_PROTO_ID ? INVALID_PROTO_ID : protoIdFromPid(protoId);
        }

        public static ProtoId empty() {
            return new ProtoId(ObjTypes.ObjectType.OBJ_TYPE_INVALID, EMPTY_PID, INVALID_PROTO_ID);
        }

        public static ProtoId ofPid(int pid) {
            if (pid == EMPTY_PID) {
                return new ProtoId(ObjTypes.ObjectType.OBJ_TYPE_INVALID, pid, INVALID_PROTO_ID);
            }
            return new ProtoId(objectTypeFromPid(pid), pid, pid);
        }

        public static ProtoId ofProto(Proto proto) {
            return ofPid(proto == null ? EMPTY_PID : proto.pid);
        }

        public static ProtoId ofGameObject(ObjTypes.GameObject object) {
            return ofPid(object == null ? EMPTY_PID : object.pid);
        }

        public static ProtoId ofItemType(ItemProtoTypeId id) {
            return ofTypedId(ObjTypes.ObjectType.OBJ_TYPE_ITEM, id.id());
        }

        public static ProtoId ofCritterType(CritterProtoTypeId id) {
            return ofTypedId(ObjTypes.ObjectType.OBJ_TYPE_CRITTER, id.id());
        }

        public static ProtoId ofSceneryType(SceneryProtoTypeId id) {
            return ofTypedId(ObjTypes.ObjectType.OBJ_TYPE_SCENERY, id.id());
        }

        public static ProtoId ofWallType(WallProtoTypeId id) {
            return ofTypedId(ObjTypes.ObjectType.OBJ_TYPE_WALL, id.id());
        }

        public static ProtoId ofTileType(TileProtoTypeId id) {
            return ofTypedId(ObjTypes.ObjectType.OBJ_TYPE_TILE, id.id());
        }

        public static ProtoId ofMiscType(MiscProtoTypeId id) {
            return ofTypedId(ObjTypes.ObjectType.OBJ_TYPE_MISC, id.id());
        }

        private static ProtoId ofTypedId(ObjTypes.ObjectType type, int id) {
            return new ProtoId(type, buildPid(type, id), id);
        }

        public boolean hasObjectType() {
            return ObjTypes.objectTypeIsValid(objectType.id());
        }

        public boolean hasPid() {
            return pid > EMPTY_PID;
        }

        public ObjTypes.ObjectType objectType() {
            return hasObjectType() ? objectType : ObjTypes.ObjectType.OBJ_TYPE_INVALID;
        }

        public int pid() {
            return pid;
        }

        public int protoId() {
            return protoId;
        }

        public boolean valid() {
            return hasPid() && hasObjectType() && protoId >= MIN_PROTO_ID && protoId <= MAX_PROTO_ID;
        }

        public boolean matchesItemType(ItemProtoTypeId id) {
            return pid == buildPid(ObjTypes.ObjectType.OBJ_TYPE_ITEM, id.id());
        }

        public boolean matchesCritterType(CritterProtoTypeId id) {
            return pid == buildPid(ObjTypes.ObjectType.OBJ_TYPE_CRITTER, id.id());
        }

        public boolean matchesSceneryType(SceneryProtoTypeId id) {
            return pid == buildPid(ObjTypes.ObjectType.OBJ_TYPE_SCENERY, id.id());
        }

        public boolean matchesWallType(WallProtoTypeId id) {
            return pid == buildPid(ObjTypes.ObjectType.OBJ_TYPE_WALL, id.id());
        }

        public boolean matchesTileType(TileProtoTypeId id) {
            return pid == buildPid(ObjTypes.ObjectType.OBJ_TYPE_TILE, id.id());
        }

        public boolean matchesMiscType(MiscProtoTypeId id) {
            return pid == buildPid(ObjTypes.ObjectType.OBJ_TYPE_MISC, id.id());
        }

        public ItemProtoTypeId asItemTypeId() {
            if (hasPid()) {
                assert objectType == ObjTypes.ObjectType.OBJ_TYPE_ITEM
                        : "ProtoId object type doesn't match the id type";
                if (objectType == ObjTypes.ObjectType.OBJ_TYPE_ITEM) {
                    return ofItemProtoTypeId(protoId);
                }
            }
            return null;
        }

        public CritterProtoTypeId asCritterTypeId() {
            if (hasPid()) {
                assert objectType == ObjTypes.ObjectType.OBJ_TYPE_CRITTER
                        : "ProtoId object type doesn't match the id type";
                if (objectType == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
                    return ofCritterProtoTypeId(protoId);
                }
            }
            return null;
        }

        public MiscProtoTypeId asMiscTypeId() {
            if (hasPid()) {
                assert objectType == ObjTypes.ObjectType.OBJ_TYPE_MISC
                        : "ProtoId object type doesn't match the id type";
                if (objectType == ObjTypes.ObjectType.OBJ_TYPE_MISC) {
                    return ofMiscProtoTypeId(protoId);
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ProtoId)) {
                return false;
            }
            ProtoId that = (ProtoId) other;
            return pid == that.pid && objectType == that.objectType;
        }

        @Override
        public int hashCode() {
            return 31 * pid + objectType.id();
        }

        @Override
        public String toString() {
            return "ProtoId(pid=" + pid + ", type=" + objectType + ")";
        }

        // PID structure: 4 unused bits, 4 object-type bits, 24 proto-id bits.
        static int buildPid(ObjTypes.ObjectType objectType, int protoId) {
            if (!objectTypeIsValidProto(objectType) || protoId < MIN_PROTO_ID) {
                return EMPTY_PID;
            }
            return ((objectType.id() << OBJECT_TYPE_MASK_POSITION) & OBJECT_TYPE_MASK)
                    | (protoId & PROTO_ID_MASK);
        }

        // NOTE: C++ ProtoId clamps to prototype-bearing types only (< 6),
        // unlike the public objectTypeIsValid (< 11).
        static boolean objectTypeIsValidProto(ObjTypes.ObjectType type) {
            return type.id() >= ObjTypes.OBJ_TYPE_FIRST
                    && type.id() < ObjTypes.OBJ_TYPE_PROTO_COUNT;
        }

        static ObjTypes.ObjectType objectTypeFromPid(int pid) {
            // Out-of-range ids become INVALID (C++ yields an unnamed value
            // that fails the validity check the same way).
            ObjTypes.ObjectType type = ObjTypes.ofObjectType(pid >> OBJECT_TYPE_MASK_POSITION);
            return type != null ? type : ObjTypes.ObjectType.OBJ_TYPE_INVALID;
        }

        static int protoIdFromPid(int pid) {
            return pid & PROTO_ID_MASK;
        }
    }
}
