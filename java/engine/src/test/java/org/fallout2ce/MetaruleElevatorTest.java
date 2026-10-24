package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The elevator metarule (15) of {@code op_metarule}: the request finds
 * the nearest elevator stub and raises the script request flag.
 */
class MetaruleElevatorTest {

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

    private GameObject dog;
    private GameScriptApi api;

    @BeforeEach
    void setup() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        ScriptsState.resetForTests();

        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));

        byte[] objBuf = new byte[640 * 480];
        ObjectSystem.objectsInit(objBuf, 640, 480, 640);

        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        dog = dogSlot[0];
        assertEquals(0, ObjectSystem.objectSetLocation(dog, 20100, 0));

        api = new GameScriptApi() {
            @Override
            public Object selfObj() {
                return dog;
            }
        };
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        ScriptsState.resetForTests();
    }

    private static Program.ProgramValue intValue(int value) {
        Program.ProgramValue v = new Program.ProgramValue();
        v.opcode = Program.VALUE_TYPE_INT;
        v.integerValue = value;
        return v;
    }

    @Test
    void elevatorRequestFindsTheStubNearby() {
        GameObject[] stubSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stubSlot,
            ProtoId.ofSceneryType(ProtoTypes.SceneryProtoTypeId.ElevatorStub)));
        GameObject stub = stubSlot[0];
        assertEquals(0, ObjectSystem.objectSetLocation(stub, 20100 - 4, 0));
        stub.data.scenery.elevator.type = 7;
        stub.data.scenery.elevator.level = 2;

        assertEquals(0, api.metarule(15, intValue(0)));
        assertTrue((ScriptsState.scriptsRequests
                & ScriptsState.SCRIPT_REQUEST_ELEVATOR) != 0);
        assertEquals(7, ScriptsState.requestedElevatorType);
        assertEquals(2, ScriptsState.requestedElevatorLevel);
    }

    @Test
    void elevatorRefusesWithNoStub() {
        // No elevator stub on the map: the request keeps the asked type,
        // which is -1, and refuses.
        assertEquals(-1, api.metarule(15, intValue(-1)));
        assertEquals(ScriptsState.SCRIPT_REQUEST_NONE, ScriptsState.scriptsRequests);
    }
}
