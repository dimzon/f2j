package org.fallout2ce;

import org.fallout2ce.EventQueue.EventType;

/**
 * The script request globals, mirroring {@code gScriptsRequests} and its
 * siblings in {@code src/scripts.h} / {@code src/scripts.cc}.
 */
public final class ScriptsState {

    private ScriptsState() {
    }

    /** Port of {@code SCRIPT_REQUEST_NONE} ({@code src/scripts.h}). */
    public static final int SCRIPT_REQUEST_NONE = 0x00;

    /** Port of {@code SCRIPT_REQUEST_COMBAT} ({@code src/scripts.h}). */
    public static final int SCRIPT_REQUEST_COMBAT = 0x01;

    /** Port of {@code SCRIPT_REQUEST_ELEVATOR} ({@code src/scripts.h}). */
    public static final int SCRIPT_REQUEST_ELEVATOR = 0x08;

    /** Port of {@code SCRIPT_REQUEST_COMBAT_NO_DATA} ({@code src/scripts.h}). */
    public static final int SCRIPT_REQUEST_COMBAT_NO_DATA = 0x40;

    /** Port of {@code SCRIPT_REQUEST_COMBAT_LOCKED} ({@code src/scripts.h}). */
    public static final int SCRIPT_REQUEST_COMBAT_LOCKED = 0x400;

    /** Port of {@code gScriptsRequests} ({@code src/scripts.h}). */
    public static int scriptsRequests;

    /** Port of {@code gScriptsRequestedElevatorType}. */
    public static int requestedElevatorType;

    /** Port of {@code gScriptsRequestedElevatorLevel}. */
    public static int requestedElevatorLevel;

    /**
     * Port of {@code CombatStartData} ({@code src/combat_defs.h}).
     */
    public static final class CombatStartData {
        public ObjTypes.GameObject attacker;
        public ObjTypes.GameObject defender;
        public int actionPointsBonus;
        public int accuracyBonus;
        public int damageBonus;
        public int minDamage;
        public int maxDamage;
        public int overrideAttackResults;
        public ObjTypes.Dam attackerResults = ObjTypes.Dam.NONE;
        public ObjTypes.Dam targetResults = ObjTypes.Dam.NONE;
    }

    /** Port of {@code gScriptsRequestedCSD}. */
    static final CombatStartData requestedCSD = new CombatStartData();

    /** Port of {@code gScriptsCSD}. */
    static final CombatStartData csd = new CombatStartData();

    private static void copyCombatStartData(CombatStartData dst, CombatStartData src) {
        dst.attacker = src.attacker;
        dst.defender = src.defender;
        dst.actionPointsBonus = src.actionPointsBonus;
        dst.accuracyBonus = src.accuracyBonus;
        dst.damageBonus = src.damageBonus;
        dst.minDamage = src.minDamage;
        dst.maxDamage = src.maxDamage;
        dst.overrideAttackResults = src.overrideAttackResults;
        dst.attackerResults = src.attackerResults;
        dst.targetResults = src.targetResults;
    }

    // 0x4A457C scriptsRequestCombat.
    public static int scriptsRequestCombat(CombatStartData combat) {
        if ((scriptsRequests & SCRIPT_REQUEST_COMBAT_LOCKED) != SCRIPT_REQUEST_NONE) {
            return -1;
        }

        if (combat != null) {
            copyCombatStartData(requestedCSD, combat);
        } else {
            scriptsRequests |= SCRIPT_REQUEST_COMBAT_NO_DATA;
        }

        scriptsRequests |= SCRIPT_REQUEST_COMBAT;

        return 0;
    }

    // 0x4A45D4 _scripts_request_combat_locked.
    public static void scriptsRequestCombatLocked(CombatStartData combat) {
        if (combat != null) {
            copyCombatStartData(requestedCSD, combat);
        } else {
            scriptsRequests |= SCRIPT_REQUEST_COMBAT_NO_DATA;
        }

        scriptsRequests |= SCRIPT_REQUEST_COMBAT_LOCKED | SCRIPT_REQUEST_COMBAT;
    }

    // 0x4A3F90 _scripts_clear_combat_requests.
    public static void scriptsClearCombatRequests(ObjectSystem.Script script) {
        if ((scriptsRequests & SCRIPT_REQUEST_COMBAT) != SCRIPT_REQUEST_NONE
                && requestedCSD.attacker == script.owner) {
            scriptsRequests &= ~(SCRIPT_REQUEST_COMBAT_LOCKED | SCRIPT_REQUEST_COMBAT);
        }
    }

    // 0x4A4468 scriptsHandleRequests. The non-combat requests (town map,
    // world map, elevator, explosion, dialog, looting, stealing, endgame)
    // all run game-phase handlers, so they are seams: the combat request is
    // the only one the port can service, and only up to the _combat seam.
    public static int scriptsHandleRequests() {
        if (scriptsRequests == SCRIPT_REQUEST_NONE) {
            return 0;
        }

        if ((scriptsRequests & SCRIPT_REQUEST_COMBAT) != SCRIPT_REQUEST_NONE) {
            // _action_explode_running(): render seam, false.
            scriptsRequests &= ~(SCRIPT_REQUEST_COMBAT_LOCKED | SCRIPT_REQUEST_COMBAT);
            copyCombatStartData(csd, requestedCSD);

            if ((scriptsRequests & SCRIPT_REQUEST_COMBAT_NO_DATA) != SCRIPT_REQUEST_NONE) {
                scriptsRequests &= ~SCRIPT_REQUEST_COMBAT_NO_DATA;
                combat(null);
            } else {
                combat(csd);
                // memset(&gScriptsCSD, 0): the Java fresh instance is zeroed.
            }
        }

        return 0;
    }

    /** Seam for {@code _combat} (src/combat.cc, game phase). */
    private static void combat(CombatStartData data) {
    }

    // gCritterProcessingIndex (src/scripts.cc): round-robin cursor over the
    // critter script list.
    private static int critterProcessingIndex;

    // gLastQueueProcessingTime (src/scripts.cc): the 100ms gate of the
    // scripts tick.
    private static int lastQueueProcessingTime;

    /** Test seam: tick-state reset (fresh-process mirror). */
    static void resetTickForTests() {
        critterProcessingIndex = 0;
        lastQueueProcessingTime = 0;
    }

    /**
     * Port of {@code _script_chk_critters} (0x4A3BF8): advance the
     * round-robin cursor over the critter script list and execute
     * CRITTER (out of combat) for exactly one script per tick. The dialog
     * and movie guards are game-phase (both false here); the combat guard
     * reads the live combat state.
     */
    static void scriptChkCritters() {
        if (Combat.isInCombat()) {
            return;
        }

        java.util.List<ObjectSystem.Script> list =
                ObjectSystem.scriptsOfType(ObjectSystem.SCRIPT_TYPE_CRITTER);
        int scriptsCount = list.size();

        critterProcessingIndex += 1;
        if (critterProcessingIndex >= scriptsCount) {
            critterProcessingIndex = 0;
        }

        if (critterProcessingIndex < scriptsCount) {
            int proc = Combat.isInCombat()
                    ? ObjectSystem.SCRIPT_PROC_COMBAT
                    : ObjectSystem.SCRIPT_PROC_CRITTER;
            ObjectSystem.Script script = list.get(critterProcessingIndex);
            ObjectSystem.scriptExecProc(script.sid, proc);
        }
    }

    /**
     * Port of {@code _script_chk_timed_events} (0x4A3D84): the 100ms gate
     * opens the queue drain. The dialog/movie guards are game-phase (both
     * false); game-time advance per gate is the event clock's job.
     */
    public static void scriptChkTimedEvents() {
        int currentTime = EventQueue.gameTimeGetTime();
        if (Integer.compareUnsigned(EventQueue.gameTimeGetTime() - lastQueueProcessingTime, 100) >= 0) {
            lastQueueProcessingTime = currentTime;
            while (!EventQueue.queueIsEmpty()) {
                if (Integer.compareUnsigned(EventQueue.gameTimeGetTime(),
                        EventQueue.queueGetNextEventTime()) < 0) {
                    break;
                }
                EventQueue.queueProcessEvents();
            }
        }
    }

    /**
     * Port of {@code scriptAddTimerEvent} (0x4A3E60): schedule a TIMED
     * script event on the owner with a delay in ticks.
     */
    public static int scriptAddTimerEvent(int sid, int delay, int param) {
        ObjectSystem.Script[] slot = new ObjectSystem.Script[1];
        if (ObjectSystem.scriptGetScript(sid, slot) == -1) {
            return -1;
        }

        return EventQueue.queueAddEvent(delay, slot[0].owner,
                new EventQueue.ScriptEvent(sid, param), EventType.SCRIPT);
    }

    /** Port of {@code scriptRemoveTimerEvents} (0x4A3E9C). */
    public static int scriptRemoveTimerEvents(int sid, int fixedParam) {
        ObjectSystem.Script[] slot = new ObjectSystem.Script[1];
        if (ObjectSystem.scriptGetScript(sid, slot) == -1) {
            return -1;
        }
        EventQueue.setScriptQueueRemoveUseFixedParam(true);
        EventQueue.queueRemoveTimerEventsBySid(sid, fixedParam);
        EventQueue.setScriptQueueRemoveUseFixedParam(false);
        return 0;
    }

    /** Test seam: pretend 100ms passed since the last queue processing. */
    static void pretendQueueGateElapsed() {
        lastQueueProcessingTime = EventQueue.gameTimeGetTime() - 100;
    }

    /** Test seam: the round-robin cursor (src/scripts.cc gCritterProcessingIndex). */
    static int critterProcessingIndexForTests() {
        return critterProcessingIndex;
    }

    /** Test seam: a script's fixed parameter. */
    static int scriptFixedParamForTests(int sid) {
        ObjectSystem.Script[] slot = new ObjectSystem.Script[1];
        if (ObjectSystem.scriptGetScript(sid, slot) == -1) {
            return -1;
        }
        return slot[0].fixedParam;
    }

    /**
     * Port of {@code scriptEventProcess} (0x4A4048): the SCRIPT queue
     * handler - set the fixed param and execute TIMED.
     */
    static int scriptEventProcess(Object owner, Object data) {
        EventQueue.ScriptEvent scriptEvent = (EventQueue.ScriptEvent) data;

        ObjectSystem.Script[] slot = new ObjectSystem.Script[1];
        if (ObjectSystem.scriptGetScript(scriptEvent.sid, slot) == -1) {
            return 0;
        }

        slot[0].fixedParam = scriptEvent.fixedParam;

        ObjectSystem.scriptExecProc(scriptEvent.sid, ObjectSystem.SCRIPT_PROC_TIMED);

        return 0;
    }

    /**
     * Port of the scripts tick ({@code _scripts_padv}/
     * {@code _script_chk_timed_events}, src/scripts.cc:1055-1094): the 100ms
     * gate advances game time out of combat and drains the queue while its
     * head is due. The dialog/movie guards are game-phase (both false).
     */
    public static void scriptsProcessBk() {
        int currentTime = EventQueue.gameTimeGetTime();
        int elapsed = EventQueue.gameTimeGetTime() - lastQueueProcessingTime;
        if (elapsed >= 100) {
            lastQueueProcessingTime = currentTime;
            while (!EventQueue.queueIsEmpty()) {
                if (Integer.compareUnsigned(EventQueue.gameTimeGetTime(),
                        EventQueue.queueGetNextEventTime()) < 0) {
                    break;
                }
                EventQueue.queueProcessEvents();
            }
        }

        if (!Combat.isInCombat()) {
            scriptChkCritters();
            // _script_chk_timed_events: the queue drain above is its body.
        }
    }

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
        requestedCSD.attacker = null;
        requestedCSD.defender = null;
        requestedCSD.actionPointsBonus = 0;
        requestedCSD.accuracyBonus = 0;
        requestedCSD.damageBonus = 0;
        requestedCSD.minDamage = 0;
        requestedCSD.maxDamage = 0;
        requestedCSD.overrideAttackResults = 0;
        requestedCSD.attackerResults = ObjTypes.Dam.NONE;
        requestedCSD.targetResults = ObjTypes.Dam.NONE;
    }
}
