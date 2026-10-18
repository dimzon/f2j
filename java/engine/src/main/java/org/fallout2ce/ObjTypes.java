package org.fallout2ce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Game object model, mirroring {@code src/obj_types.h} (the
 * {@code worldmap.h} include is unused by the header body).
 *
 * <p>C++ {@code Object} is renamed to {@link GameObject}
 * ({@code java.lang.Object} clash). Bitmask enums become immutable bit bags
 * (see {@link MapDefs.MapHeaderFlags}); in-place {@code &=}/{@code |=}/
 * {@code ^=} become reassignment. Union overlays ({@code ItemObjectData},
 * {@code whoHitMe}/{@code whoHitMeCid}, {@code ObjectData}) keep all
 * variants as fields — only the active one is used; load/save ports must
 * keep overlaid pairs consistent. {@code Inventory} keeps the item list
 * ({@code length}/{@code capacity} collapse into {@code size()}).
 */
public final class ObjTypes {

    private ObjTypes() {
    }

    public static final int ROTATION_COUNT = 6;
    public static final int ROTATION_FIRST = 0;
    public static final int ROTATION_LAST = 5;

    public enum Rotation {
        ROTATION_INVALID(-1),
        ROTATION_NE(0),
        ROTATION_E(1),
        ROTATION_SE(2),
        ROTATION_SW(3),
        ROTATION_W(4),
        ROTATION_NW(5);

        private final int id;

        Rotation(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Port of {@code operator+}: raw add, no wrapping (like C++). */
        public Rotation plus(int rhs) {
            return ObjTypes.ofRotation(id + rhs);
        }

        /** Port of {@code operator-}: raw subtract, no wrapping (like C++). */
        public Rotation minus(int rhs) {
            return ObjTypes.ofRotation(id - rhs);
        }

        /** Port of {@code operator%}. */
        public Rotation mod(int rhs) {
            return ObjTypes.ofRotation(id % rhs);
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public Rotation next() {
            return ObjTypes.ofRotation(id + 1);
        }

        /** Previous id (C++ prefix-{@code --} step), or null if unknown. */
        public Rotation prev() {
            return ObjTypes.ofRotation(id - 1);
        }
    }

    private static final Map<Integer, Rotation> ROTATION_BY_ID = new HashMap<>();

    static {
        for (Rotation e : Rotation.values()) {
            ROTATION_BY_ID.put(e.id, e);
        }
    }

    public static Rotation ofRotation(int id) {
        return ROTATION_BY_ID.get(id);
    }

    public static boolean rotationIsValid(int rotation) {
        return rotation >= ROTATION_FIRST && rotation < ROTATION_COUNT;
    }

    public static final int OBJ_TYPE_COUNT = 11;
    public static final int OBJ_TYPE_FIRST = 0;
    public static final int OBJ_TYPE_PROTO_COUNT = 6;

    public enum ObjectType {
        OBJ_TYPE_INVALID(-1),
        OBJ_TYPE_ITEM(0),
        OBJ_TYPE_CRITTER(1),
        OBJ_TYPE_SCENERY(2),
        OBJ_TYPE_WALL(3),
        OBJ_TYPE_TILE(4),
        OBJ_TYPE_MISC(5),
        OBJ_TYPE_INTERFACE(6),
        OBJ_TYPE_INVENTORY(7),
        OBJ_TYPE_HEAD(8),
        OBJ_TYPE_BACKGROUND(9),
        OBJ_TYPE_SKILLDEX(10);

        private final int id;

        ObjectType(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public ObjectType next() {
            return ObjTypes.ofObjectType(id + 1);
        }
    }

    private static final Map<Integer, ObjectType> OBJTYPE_BY_ID = new HashMap<>();

    static {
        for (ObjectType e : ObjectType.values()) {
            OBJTYPE_BY_ID.put(e.id, e);
        }
    }

    public static ObjectType ofObjectType(int id) {
        return OBJTYPE_BY_ID.get(id);
    }

    public static boolean objectTypeIsValid(int type) {
        return type >= OBJ_TYPE_FIRST && type < OBJ_TYPE_COUNT;
    }

    /** Port of {@code OutlineType}: immutable bit bag. */
    public static final class OutlineType {
        public static final OutlineType NONE = new OutlineType(0x00);
        public static final OutlineType HOSTILE = new OutlineType(0x01);
        public static final OutlineType SAME_TEAM = new OutlineType(0x02);
        public static final OutlineType BODY = new OutlineType(0x04);
        public static final OutlineType FRIENDLY = new OutlineType(0x08);
        public static final OutlineType ITEM = new OutlineType(0x10);
        public static final OutlineType BLOCKED = new OutlineType(0x20);
        public static final OutlineType MAX = new OutlineType(0xFFFFFF);

        private final int bits;

        public OutlineType(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public OutlineType and(OutlineType rhs) {
            return new OutlineType(bits & rhs.bits);
        }

        public OutlineType and(int rhs) {
            return new OutlineType(bits & rhs);
        }

        public OutlineType or(OutlineType rhs) {
            return new OutlineType(bits | rhs.bits);
        }

        public OutlineType or(int rhs) {
            return new OutlineType(bits | rhs);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof OutlineType && ((OutlineType) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "OutlineType(0x" + Integer.toHexString(bits) + ")";
        }
    }

    public static final int OUTLINE_PALETTED = 0x40000000;
    public static final int OUTLINE_DISABLED = 0x80000000;

    /** Port of {@code ObjectFlags}: immutable bit bag. */
    public static final class ObjectFlags {
        public static final ObjectFlags NONE = new ObjectFlags(0x00);
        public static final ObjectFlags HIDDEN = new ObjectFlags(0x01);
        public static final ObjectFlags FLAG_0X02 = new ObjectFlags(0x02);
        public static final ObjectFlags NO_SAVE = new ObjectFlags(0x04);
        public static final ObjectFlags FLAT = new ObjectFlags(0x08);
        public static final ObjectFlags NO_BLOCK = new ObjectFlags(0x10);
        public static final ObjectFlags LIGHTING = new ObjectFlags(0x20);
        public static final ObjectFlags NO_REMOVE = new ObjectFlags(0x400);
        public static final ObjectFlags MULTIHEX = new ObjectFlags(0x800);
        public static final ObjectFlags NO_HIGHLIGHT = new ObjectFlags(0x1000);
        public static final ObjectFlags QUEUED = new ObjectFlags(0x2000);
        public static final ObjectFlags TRANS_RED = new ObjectFlags(0x4000);
        public static final ObjectFlags TRANS_NONE = new ObjectFlags(0x8000);
        public static final ObjectFlags TRANS_WALL = new ObjectFlags(0x10000);
        public static final ObjectFlags TRANS_GLASS = new ObjectFlags(0x20000);
        public static final ObjectFlags TRANS_STEAM = new ObjectFlags(0x40000);
        public static final ObjectFlags TRANS_ENERGY = new ObjectFlags(0x80000);
        public static final ObjectFlags IN_LEFT_HAND = new ObjectFlags(0x1000000);
        public static final ObjectFlags IN_RIGHT_HAND = new ObjectFlags(0x2000000);
        public static final ObjectFlags WORN = new ObjectFlags(0x4000000);
        public static final ObjectFlags WALL_TRANS_END = new ObjectFlags(0x10000000);
        public static final ObjectFlags LIGHT_THRU = new ObjectFlags(0x20000000);
        public static final ObjectFlags SEEN = new ObjectFlags(0x40000000);
        public static final ObjectFlags SHOOT_THRU = new ObjectFlags(0x80000000);

        public static final ObjectFlags IN_ANY_HAND = IN_LEFT_HAND.or(IN_RIGHT_HAND);
        public static final ObjectFlags EQUIPPED = IN_ANY_HAND.or(WORN);
        public static final ObjectFlags FLAG_0XFC000 = TRANS_ENERGY.or(TRANS_STEAM).or(TRANS_GLASS)
                .or(TRANS_WALL).or(TRANS_NONE).or(TRANS_RED);
        public static final ObjectFlags OPEN_DOOR = SHOOT_THRU.or(LIGHT_THRU).or(NO_BLOCK);

        private final int bits;

        public ObjectFlags(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public ObjectFlags and(ObjectFlags rhs) {
            return new ObjectFlags(bits & rhs.bits);
        }

        public ObjectFlags or(ObjectFlags rhs) {
            return new ObjectFlags(bits | rhs.bits);
        }

        public ObjectFlags not() {
            return new ObjectFlags(~bits);
        }

        public ObjectFlags xor(ObjectFlags rhs) {
            return new ObjectFlags(bits ^ rhs.bits);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ObjectFlags && ((ObjectFlags) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "ObjectFlags(0x" + Integer.toHexString(bits) + ")";
        }
    }

    public static final int DUDE_STATE_COUNT = 5;
    public static final int DUDE_STATE_FIRST = 0;

    public enum DudeState {
        DUDE_STATE_SNEAKING(0),
        DUDE_STATE_POISONED(1),
        DUDE_STATE_RADIATED(2),
        DUDE_STATE_LEVEL_UP_AVAILABLE(3),
        DUDE_STATE_ADDICTED(4);

        private final int id;

        DudeState(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final Map<Integer, DudeState> DUDESTATE_BY_ID = new HashMap<>();

    static {
        for (DudeState e : DudeState.values()) {
            DUDESTATE_BY_ID.put(e.id, e);
        }
    }

    public static DudeState ofDudeState(int id) {
        return DUDESTATE_BY_ID.get(id);
    }

    public static boolean dudeStateIsValid(int state) {
        return state >= DUDE_STATE_FIRST && state < DUDE_STATE_COUNT;
    }

    /** Port of {@code CritterFlags}: immutable bit bag. */
    public static final class CritterFlags {
        public static final CritterFlags NONE = new CritterFlags(0x00);
        public static final CritterFlags DUDE_SNEAKING = new CritterFlags(0x01);
        public static final CritterFlags DUDE_RADIATED = new CritterFlags(0x02);
        public static final CritterFlags DUDE_LEVEL_UP_AVAILABLE = new CritterFlags(0x08);
        public static final CritterFlags DUDE_ADDICTED = new CritterFlags(0x10);
        public static final CritterFlags BARTER = new CritterFlags(0x02);
        public static final CritterFlags NO_STEAL = new CritterFlags(0x20);
        public static final CritterFlags NO_DROP = new CritterFlags(0x40);
        public static final CritterFlags NO_LIMBS = new CritterFlags(0x80);
        public static final CritterFlags NO_AGE = new CritterFlags(0x100);
        public static final CritterFlags NO_HEAL = new CritterFlags(0x200);
        public static final CritterFlags INVULNERABLE = new CritterFlags(0x400);
        public static final CritterFlags FLAT = new CritterFlags(0x800);
        public static final CritterFlags SPECIAL_DEATH = new CritterFlags(0x1000);
        public static final CritterFlags LONG_LIMBS = new CritterFlags(0x2000);
        public static final CritterFlags NO_KNOCKBACK = new CritterFlags(0x4000);

        private final int bits;

        public CritterFlags(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public CritterFlags and(CritterFlags rhs) {
            return new CritterFlags(bits & rhs.bits);
        }

        public CritterFlags or(CritterFlags rhs) {
            return new CritterFlags(bits | rhs.bits);
        }

        public CritterFlags not() {
            return new CritterFlags(~bits);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CritterFlags && ((CritterFlags) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "CritterFlags(0x" + Integer.toHexString(bits) + ")";
        }
    }

    public static final int CONTAINER_FLAG_OPEN = 0x00000001;
    public static final int DOOR_FLAG_OPEN = 0x00000001;
    public static final int CONTAINER_FLAG_JAMMED = 0x04000000;
    public static final int DOOR_FLAG_JAMMED = 0x04000000;
    public static final int CONTAINER_FLAG_LOCKED = 0x02000000;
    public static final int DOOR_FLAG_LOCKED = 0x02000000;
    public static final int OBJ_LOCKED = 0x02000000;
    public static final int OBJ_JAMMED = 0x04000000;

    /** Port of {@code CritterManeuver}: immutable bit bag. */
    public static final class CritterManeuver {
        public static final CritterManeuver NONE = new CritterManeuver(0);
        public static final CritterManeuver ENGAGING = new CritterManeuver(0x01);
        public static final CritterManeuver DISENGAGING = new CritterManeuver(0x02);
        public static final CritterManeuver FLEEING = new CritterManeuver(0x04);

        private final int bits;

        public CritterManeuver(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public CritterManeuver and(CritterManeuver rhs) {
            return new CritterManeuver(bits & rhs.bits);
        }

        public CritterManeuver or(CritterManeuver rhs) {
            return new CritterManeuver(bits | rhs.bits);
        }

        public CritterManeuver not() {
            return new CritterManeuver(~bits);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CritterManeuver && ((CritterManeuver) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "CritterManeuver(0x" + Integer.toHexString(bits) + ")";
        }
    }

    /** Port of {@code Dam}: immutable bit bag. */
    public static final class Dam {
        public static final Dam NONE = new Dam(0x00);
        public static final Dam KNOCKED_OUT = new Dam(0x01);
        public static final Dam KNOCKED_DOWN = new Dam(0x02);
        public static final Dam CRIP_LEG_LEFT = new Dam(0x04);
        public static final Dam CRIP_LEG_RIGHT = new Dam(0x08);
        public static final Dam CRIP_ARM_LEFT = new Dam(0x10);
        public static final Dam CRIP_ARM_RIGHT = new Dam(0x20);
        public static final Dam BLIND = new Dam(0x40);
        public static final Dam DEAD = new Dam(0x80);
        public static final Dam HIT = new Dam(0x100);
        public static final Dam CRITICAL = new Dam(0x200);
        public static final Dam ON_FIRE = new Dam(0x400);
        public static final Dam BYPASS = new Dam(0x800);
        public static final Dam EXPLODE = new Dam(0x1000);
        public static final Dam DESTROY = new Dam(0x2000);
        public static final Dam DROP = new Dam(0x4000);
        public static final Dam LOSE_TURN = new Dam(0x8000);
        public static final Dam HIT_SELF = new Dam(0x10000);
        public static final Dam LOSE_AMMO = new Dam(0x20000);
        public static final Dam DUD = new Dam(0x40000);
        public static final Dam HURT_SELF = new Dam(0x80000);
        public static final Dam RANDOM_HIT = new Dam(0x100000);
        public static final Dam CRIP_RANDOM = new Dam(0x200000);
        public static final Dam BACKWASH = new Dam(0x400000);
        public static final Dam PERFORM_REVERSE = new Dam(0x800000);

        public static final Dam CRIP_LEG_ANY = CRIP_LEG_LEFT.or(CRIP_LEG_RIGHT);
        public static final Dam CRIP_ARM_ANY = CRIP_ARM_LEFT.or(CRIP_ARM_RIGHT);
        public static final Dam CRIP = CRIP_LEG_ANY.or(CRIP_ARM_ANY).or(BLIND);

        private final int bits;

        public Dam(int bits) {
            this.bits = bits;
        }

        public int bits() {
            return bits;
        }

        public Dam and(Dam rhs) {
            return new Dam(bits & rhs.bits);
        }

        public Dam or(Dam rhs) {
            return new Dam(bits | rhs.bits);
        }

        public Dam not() {
            return new Dam(~bits);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Dam && ((Dam) other).bits == bits;
        }

        @Override
        public int hashCode() {
            return bits;
        }

        @Override
        public String toString() {
            return "Dam(0x" + Integer.toHexString(bits) + ")";
        }
    }

    /** Port of {@code InventoryItem}. */
    public static final class InventoryItem {
        public GameObject item;
        public int quantity;
    }

    /**
     * Port of {@code Inventory}. The C array ({@code length}/{@code capacity}
     * + realloc) becomes a list; save/load ports handle the disk ints at
     * the boundary. {@code capacity} is kept because C round-trips the raw
     * value through the save file (it is the malloc'd slot count, not the
     * item count), so deriving it from {@code size()} changes the bytes.
     */
    public static final class Inventory {
        public final List<InventoryItem> items = new ArrayList<>();
        public int capacity;
    }

    /** Port of {@code WeaponObjectData}. */
    public static final class WeaponObjectData {
        public int ammoQuantity;
        public int ammoTypePid;
    }

    /** Port of {@code AmmoItemData}. */
    public static final class AmmoItemData {
        public int quantity;
    }

    /** Port of {@code MiscItemData}. */
    public static final class MiscItemData {
        public int charges;
    }

    /** Port of {@code KeyItemData}. */
    public static final class KeyItemData {
        public int keyCode;
    }

    /**
     * Port of {@code ItemObjectData}. The C union overlays the variants;
     * Java keeps all four (only the active one is used).
     */
    public static final class ItemObjectData {
        public final WeaponObjectData weapon = new WeaponObjectData();
        public final AmmoItemData ammo = new AmmoItemData();
        public final MiscItemData misc = new MiscItemData();
        public final KeyItemData key = new KeyItemData();
    }

    /** Port of {@code CritterCombatData}. */
    public static final class CritterCombatData {
        public CritterManeuver maneuver = CritterManeuver.NONE;
        public int ap;
        public Dam results = Dam.NONE;
        public int damageLastTurn;
        public int aiPacket;
        public int team;
        // C union: pointer vs serialized id. Keep both consistent.
        public GameObject whoHitMe;
        public int whoHitMeCid;
    }

    /** Port of {@code CritterObjectData}. */
    public static final class CritterObjectData {
        public int reaction;
        public final CritterCombatData combat = new CritterCombatData();
        public int hp;
        public int radiation;
        public int poison;
    }

    /** Port of {@code DoorSceneryData}. */
    public static final class DoorSceneryData {
        public int openFlags;
    }

    /** Port of {@code StairsSceneryData}. */
    public static final class StairsSceneryData {
        public WorldmapDefs.Map destinationMap;
        public int destinationBuiltTile;
    }

    /** Port of {@code ElevatorSceneryData}. */
    public static final class ElevatorSceneryData {
        public int type;
        public int level;
    }

    /** Port of {@code LadderSceneryData}. */
    public static final class LadderSceneryData {
        public WorldmapDefs.Map destinationMap;
        public int destinationBuiltTile;
    }

    /**
     * Port of {@code SceneryObjectData}. The C union overlays the variants;
     * Java keeps all four (only the active one is used).
     */
    public static final class SceneryObjectData {
        public final DoorSceneryData door = new DoorSceneryData();
        public final StairsSceneryData stairs = new StairsSceneryData();
        public final ElevatorSceneryData elevator = new ElevatorSceneryData();
        public final LadderSceneryData ladder = new LadderSceneryData();
    }

    /** Port of {@code MiscObjectData}. */
    public static final class MiscObjectData {
        public WorldmapDefs.Map map;
        public int tile;
        public int elevation;
        public Rotation rotation = Rotation.ROTATION_NE;
    }

    /**
     * Port of {@code ObjectData}. The nested C unions overlay
     * critter vs (flags + item/scenery/misc); Java keeps everything.
     */
    public static final class ObjectData {
        public final Inventory inventory = new Inventory();
        public final CritterObjectData critter = new CritterObjectData();
        public int flags;
        public final ItemObjectData item = new ItemObjectData();
        public final SceneryObjectData scenery = new SceneryObjectData();
        public final MiscObjectData misc = new MiscObjectData();
    }

    /** Port of {@code Object} (renamed: {@code java.lang.Object} clash). */
    public static final class GameObject {
        public int id;
        public int tile;
        public int x;
        public int y;
        public int sx;
        public int sy;
        public int frame;
        public Rotation rotation = Rotation.ROTATION_NE;
        public int fid;
        public ObjectFlags flags = ObjectFlags.NONE;
        public int elevation;
        public final ObjectData data = new ObjectData();
        public int pid;
        public int cid;
        public int lightDistance;
        public int lightIntensity;
        public OutlineType outline = OutlineType.NONE;
        public int sid;
        public GameObject owner;
        public int scriptIndex;
    }

    /** Port of {@code ObjectListNode}. */
    public static final class GameObjectListNode {
        public GameObject obj;
        public GameObjectListNode next;
    }

    public static final int BUILT_TILE_TILE_MASK = 0x3FFFFFF;
    public static final int BUILT_TILE_ELEVATION_MASK = 0xE0000000;
    public static final int BUILT_TILE_ELEVATION_SHIFT = 29;
    public static final int BUILT_TILE_ROTATION_MASK = 0x1C000000;
    public static final int BUILT_TILE_ROTATION_SHIFT = 26;

    public static int builtTileGetTile(int builtTile) {
        return builtTile & BUILT_TILE_TILE_MASK;
    }

    /**
     * Logical shift: the C++ mask is an unsigned hex literal, so the
     * expression is unsigned (verified by {@code [objtypes]}: elevation 7
     * of a negative tile word reads back as 7, not -1).
     */
    public static int builtTileGetElevation(int builtTile) {
        return (builtTile & BUILT_TILE_ELEVATION_MASK) >>> BUILT_TILE_ELEVATION_SHIFT;
    }

    public static Rotation builtTileGetRotation(int builtTile) {
        return ofRotation((builtTile & BUILT_TILE_ROTATION_MASK) >> BUILT_TILE_ROTATION_SHIFT);
    }

    public static int builtTileCreate(int tile, int elevation) {
        return tile | ((elevation << BUILT_TILE_ELEVATION_SHIFT) & BUILT_TILE_ELEVATION_MASK);
    }
}
