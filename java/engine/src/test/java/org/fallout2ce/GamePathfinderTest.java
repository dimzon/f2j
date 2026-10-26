package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ObjTypes.ObjectFlags;
import org.fallout2ce.ProtoTypes.KillType;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;
import org.fallout2ce.ProtoTypes.SceneryType;

/**
 * [path], golden/v166.txt: {@code pathfinderFindPath} ({@code _make_path},
 * src/animation.cc) with {@code _obj_blocking_at}, pinned against the real
 * C++ A*: straight/blocked/enclosed searches, the SFALL multihex retarget,
 * the {@code canUseDoor} gate (biped/gecko/lock/dude walk-through) and the
 * radioactive-goo cost surcharge.
 */
class GamePathfinderTest {
    private GameObject mover;
    private GameObject human;
    private GameObject door;
    private GameObject goo;
    private final GameObject[] dogs = new GameObject[6];

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

        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("master.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("patch000.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("critter.dat")));
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());
        assertEquals(0, Mouse.mouseInit());
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200,
                new byte[64 * 64], 64, 64, 64, rect -> { }));
        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));

        mover = create(16777558);
        human = create(16777376);
        door = create(33554585); // scenery 153
        goo = create(33555417); // scenery 985
        for (int index = 0; index < 6; index++) {
            dogs[index] = create(16777558);
        }
    }

    @AfterEach
    void tearDown() {
        GameRandom.resetProviders();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
    }

    private static GameObject create(int pid) {
        GameObject[] slot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(slot, ProtoId.ofPid(pid)));
        return slot[0];
    }

    private static void place(GameObject obj, int tile) {
        assertEquals(0, ObjectSystem.objectSetLocation(obj, tile, 0));
    }

    private static void park(GameObject obj) {
        place(obj, 199);
    }

    /** Mirrors the harness runPath lambda: length plus written rotations. */
    private static int[] runPath(GameObject obj, int to, int requireEmpty) {
        byte[] rotations = new byte[800];
        Arrays.fill(rotations, (byte) 0xAA);
        int rc = Animation.pathfinderFindPath(obj, obj.tile, to, rotations, requireEmpty,
                ObjectSystem::objBlockingAt);
        int[] out = new int[1 + Math.max(0, rc)];
        out[0] = rc;
        for (int index = 0; index < rc; index++) {
            out[1 + index] = rotations[index] & 0xFF;
        }
        return out;
    }

    private static void assertPath(int[] actual, int length, int... rotations) {
        assertEquals(length, actual[0], "path");
        assertEquals(rotations.length, actual.length - 1, "rotation count");
        for (int index = 0; index < rotations.length; index++) {
            assertEquals(rotations[index], actual[1 + index], "rotation " + index);
        }
    }

    @Test
    void pathVectors() {
        int a = 24101;
        int b = TileMath.tileGetTileInDirection(a, Rotation.SE, 4);
        int l1 = TileMath.tileGetTileInDirection(a, Rotation.SE, 1);
        int l2 = TileMath.tileGetTileInDirection(a, Rotation.SE, 2);
        int l3 = TileMath.tileGetTileInDirection(a, Rotation.SE, 3);
        assertEquals(24901, b);
        assertEquals(24301, l1);
        assertEquals(24501, l2);
        assertEquals(24701, l3);

        for (GameObject dog : dogs) {
            park(dog);
        }
        park(human);
        park(door);
        park(goo);
        place(mover, a);

        // Trivial and straight-line cases, with and without the
        // empty-destination guard.
        assertEquals(0, runPath(mover, a, 0)[0]);
        assertPath(runPath(mover, b, 0), 4, 2, 2, 2, 2);
        assertPath(runPath(mover, b, 1), 4, 2, 2, 2, 2);

        // Destination occupied by a plain dog: requireEmptyDest gates
        // upfront, otherwise the path ends on the occupied tile itself.
        park(dogs[0]);
        place(dogs[0], b);
        assertPath(runPath(mover, b, 0), 4, 2, 2, 2, 2);
        assertEquals(0, runPath(mover, b, 1)[0]);

        // Multihex blocker on the destination retargets the search to the
        // first reachable neighbour (SFALL center-tile fix).
        dogs[0].flags = dogs[0].flags.or(ObjectFlags.MULTIHEX);
        assertPath(runPath(mover, b, 0), 3, 2, 2, 2);
        dogs[0].flags = dogs[0].flags.and(ObjectFlags.MULTIHEX.not());

        // A wall of dogs across the straight line: detour around the flank.
        place(dogs[0], l1);
        place(dogs[1], l2);
        place(dogs[2], l3);
        assertPath(runPath(mover, b, 0), 5, 1, 2, 2, 2, 3);
        for (int index = 0; index < 3; index++) {
            park(dogs[index]);
        }

        // Enclosed destination: five dogs seal every approach except l3.
        int[] neighbours = new int[6];
        for (int index = 0; index < 6; index++) {
            neighbours[index] = TileMath.tileGetTileInDirection(b, Rotation.fromInt(index), 1);
        }
        int l3Slot = -1;
        for (int index = 0; index < 6; index++) {
            if (neighbours[index] == l3) {
                l3Slot = index;
                continue;
            }
            place(dogs[index], neighbours[index]);
        }
        assertPath(runPath(mover, b, 0), 4, 2, 2, 2, 2);
        place(dogs[l3Slot], l3);
        assertEquals(0, runPath(mover, b, 0)[0]);
        park(dogs[l3Slot]);

        // Door scenarios: five dogs seal the destination except l3, where
        // the door object stands. The door object must block (no NO_BLOCK)
        // for canUseDoor to be consulted at all.
        Proto[] doorProto = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofPid(33554585), doorProto));
        SceneryType savedDoorType = doorProto[0].scenery.type;
        int savedDoorGenericFlags = doorProto[0].scenery.data.generic.genericFlags;
        doorProto[0].scenery.type = SceneryType.SCENERY_TYPE_DOOR;
        doorProto[0].scenery.data.generic.genericFlags &= ~0x04;
        door.flags = door.flags.and(ObjectFlags.NO_BLOCK.not());

        int savedDoorOpenFlags = door.data.scenery.door.openFlags;
        door.data.scenery.door.openFlags &= ~ObjTypes.DOOR_FLAG_LOCKED;
        place(door, l3);
        for (int index = 0; index < 6; index++) {
            if (index != l3Slot) {
                place(dogs[index], neighbours[index]);
            }
        }

        place(human, a);
        park(mover);
        assertPath(runPath(human, b, 0), 4, 2, 2, 2, 2);
        door.data.scenery.door.openFlags |= ObjTypes.DOOR_FLAG_LOCKED;
        assertEquals(0, runPath(human, b, 0)[0]);
        door.data.scenery.door.openFlags &= ~ObjTypes.DOOR_FLAG_LOCKED;

        Proto[] humanProto = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(human), humanProto));
        KillType savedHumanKillType = humanProto[0].critter.data.killType;
        humanProto[0].critter.data.killType = KillType.KILL_TYPE_GECKO;
        assertEquals(0, runPath(human, b, 0)[0]);
        humanProto[0].critter.data.killType = savedHumanKillType;
        park(human);

        // The dude cannot be moved headless (objectSetLocation(gDude)
        // segfaults the C++ harness), so the harness points its tile field
        // at the scenario; the Java port mirrors exactly that.
        GameObject dude = ObjectSystem.getDude();
        int savedDudeTile = dude.tile;
        dude.tile = a;
        assertEquals(0, runPath(dude, b, 0)[0]);
        doorProto[0].scenery.data.generic.genericFlags |= 0x04;
        assertPath(runPath(dude, b, 0), 4, 2, 2, 2, 2);
        doorProto[0].scenery.data.generic.genericFlags &= ~0x04;
        dude.tile = savedDudeTile;

        doorProto[0].scenery.type = savedDoorType;
        doorProto[0].scenery.data.generic.genericFlags = savedDoorGenericFlags;
        park(door);
        for (GameObject dog : dogs) {
            park(dog);
        }

        // Goo scenarios: radioactive goo scenery. Without NO_BLOCK the tile
        // is skipped like any blocker; with it the tile is walkable and the
        // A* node cost surcharge applies (400, or 100 for a gecko).
        place(mover, a);
        place(goo, l2);
        int savedGooFlags = goo.flags.bits();
        goo.flags = goo.flags.and(ObjectFlags.NO_BLOCK.not());
        assertPath(runPath(mover, b, 0), 5, 1, 2, 2, 2, 3);
        goo.flags = goo.flags.or(ObjectFlags.NO_BLOCK);
        assertPath(runPath(mover, b, 0), 5, 1, 2, 2, 2, 3);
        Proto[] moverProto = new Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(ProtoId.ofGameObject(mover), moverProto));
        KillType savedMoverKillType = moverProto[0].critter.data.killType;
        moverProto[0].critter.data.killType = KillType.KILL_TYPE_GECKO;
        assertPath(runPath(mover, b, 0), 5, 1, 2, 2, 2, 3);
        moverProto[0].critter.data.killType = savedMoverKillType;

        // Forced crossing: the goo tile is the only approach left to the
        // destination, so the surcharge is paid either way.
        for (int index = 0; index < 6; index++) {
            if (index != l3Slot) {
                place(dogs[index], neighbours[index]);
            }
        }
        place(goo, l3);
        assertPath(runPath(mover, b, 0), 4, 2, 2, 2, 2);
        moverProto[0].critter.data.killType = KillType.KILL_TYPE_GECKO;
        assertPath(runPath(mover, b, 0), 4, 2, 2, 2, 2);
        moverProto[0].critter.data.killType = savedMoverKillType;
        goo.flags = new ObjectFlags(savedGooFlags);
    }
}
