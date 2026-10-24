package org.fallout2ce;

/**
 * The script request globals, mirroring {@code gScriptsRequests} and its
 * siblings in {@code src/scripts.h} / {@code src/scripts.cc}.
 */
public final class ScriptsState {

    private ScriptsState() {
    }

    /** Port of {@code SCRIPT_REQUEST_NONE} ({@code src/scripts.h}). */
    public static final int SCRIPT_REQUEST_NONE = 0x00;

    /** Port of {@code SCRIPT_REQUEST_ELEVATOR} ({@code src/scripts.h}). */
    public static final int SCRIPT_REQUEST_ELEVATOR = 0x08;

    /** Port of {@code gScriptsRequests} ({@code src/scripts.h}). */
    public static int scriptsRequests;

    /** Port of {@code gScriptsRequestedElevatorType}. */
    public static int requestedElevatorType;

    /** Port of {@code gScriptsRequestedElevatorLevel}. */
    public static int requestedElevatorLevel;

    /**
     * Port of {@code scriptsRequestElevator} (0x4A46D8): ask for the
     * elevator near a critter.
     *
     * <p>The nearest elevator stub is searched five tiles out in every
     * direction; when one is found the request remembers its type and
     * level, and the flag goes up. An object with no tile, or with no
     * elevator around it, is refused.
     */
    public static int scriptsRequestElevator(ObjTypes.GameObject obj, int elevatorType) {
        int elevatorLevel = Map.gElevation;

        int tile = obj.tile;
        if (tile == -1) {
            return -1;
        }

        tile = tile - (MapDefs.HEX_GRID_WIDTH * 5) - 5;

        ObjTypes.GameObject elevator = null;
        for (int y = -5; y < 5; y++) {
            for (int x = -5; x < 5; x++) {
                elevator = ObjectSystem.objectFindFirstAtElevation(obj.elevation);
                while (elevator != null) {
                    if (tile == elevator.tile
                            && ProtoTypes.ProtoId.ofGameObject(elevator).equals(
                                ProtoTypes.ProtoId.ofSceneryType(
                                    ProtoTypes.SceneryProtoTypeId.ElevatorStub))) {
                        break;
                    }

                    elevator = ObjectSystem.objectFindNextAtElevation();
                }

                if (elevator != null) {
                    break;
                }

                tile += 1;
            }

            if (elevator != null) {
                break;
            }

            tile += MapDefs.HEX_GRID_WIDTH - 10;
        }

        if (elevator != null) {
            elevatorType = elevator.data.scenery.elevator.type;
            elevatorLevel = elevator.data.scenery.elevator.level;
        }

        if (elevatorType == -1) {
            return -1;
        }

        scriptsRequests |= SCRIPT_REQUEST_ELEVATOR;
        requestedElevatorType = elevatorType;
        requestedElevatorLevel = elevatorLevel;

        return 0;
    }

    /** Test seam: fresh-process mirror. */
    static void resetForTests() {
        scriptsRequests = SCRIPT_REQUEST_NONE;
        requestedElevatorType = 0;
        requestedElevatorLevel = 0;
    }
}
