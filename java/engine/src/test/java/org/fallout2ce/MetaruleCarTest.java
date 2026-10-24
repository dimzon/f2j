package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The car metarules of {@code op_metarule}: CAR_CURRENT_TOWN (30),
 * GIVE_CAR_TO_PARTY (31), GIVE_CAR_GAS (32) and the car-trunk carry
 * amount (52/53), against real protos and the real city table.
 */
class MetaruleCarTest {

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

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        MapRegistry.resetForTests();
        MapAreas.resetForTests();
        Map.resetForTests();
        WorldmapTiles.resetForTests();
        WorldmapGenData.resetForTests();

        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
        Settings.settings.system.language = Message.ENGLISH;

        WorldmapGenData.wmGenDataInit();
        assertEquals(0, MapAreas.wmAreaInit());
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        WorldmapGenData.resetForTests();
    }

    private static Program.ProgramValue intValue(int value) {
        Program.ProgramValue v = new Program.ProgramValue();
        v.opcode = Program.VALUE_TYPE_INT;
        v.integerValue = value;
        return v;
    }

    @Test
    void carRules() {
        GameScriptApi api = new GameScriptApi();

        // The car starts with a full tank and no area.
        assertEquals(WorldmapGenData.CAR_FUEL_MAX, WorldmapGenData.wmCarGasAmount());
        assertEquals(MapAreas.City.CITY_INVALID.id(), api.metarule(30, intValue(0)));

        // Filling a full tank fits nothing: the excess is returned as 0.
        assertEquals(0, WorldmapGenData.wmCarFillGas(1000));
        assertEquals(WorldmapGenData.CAR_FUEL_MAX, WorldmapGenData.wmCarGasAmount());

        // Draw fuel down, top it up, then fill over the top: the amount
        // that did not fit is returned.
        assertEquals(0, WorldmapGenData.wmCarFillGas(-79000));
        assertEquals(1000, WorldmapGenData.wmCarGasAmount());
        assertEquals(0, WorldmapGenData.wmCarFillGas(1000));
        assertEquals(2000, WorldmapGenData.wmCarGasAmount());
        assertEquals(WorldmapGenData.CAR_FUEL_MAX - 2000,
            WorldmapGenData.wmCarFillGas(WorldmapGenData.CAR_FUEL_MAX));
        assertEquals(WorldmapGenData.CAR_FUEL_MAX, WorldmapGenData.wmCarGasAmount());

        // Give the car to the party while fuelled: it transitions and the
        // out-of-gas city hides.
        assertEquals(0, api.metarule(31, intValue(0)));
        assertEquals(0, MapAreas.wmAreaVisitedState(
            MapAreas.City.CITY_CAR_OUT_OF_GAS).id());

        // The trunk's carry amount is proto state: read, set, read.
        ProtoTypes.Proto[] slot = new ProtoTypes.Proto[1];
        assertEquals(0, ProtoDb.protoGetProto(
            ProtoId.ofPid(ProtoTypes.ItemProtoTypeId.CarTrunk.id()), slot));
        int original = slot[0].item.data.container.maxSize;

        assertEquals(1, api.metarule(52, intValue(original + 50)));
        assertEquals(original + 50, api.metarule(53, intValue(0)));
        assertEquals(original + 50, slot[0].item.data.container.maxSize);
    }
}
