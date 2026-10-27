package org.fallout2ce;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Port of {@code src/queue.h} / {@code src/queue.cc} (queue_init ...
 * queue_find_next, 0x4A2320 ... 0x4A2994).
 *
 * <p>Time is an unsigned 32-bit tick count stored in a Java {@code int}
 * (identical two's-complement wrap as C {@code unsigned int}); all ordering
 * uses {@link Integer#compareUnsigned}. {@code queueGetNextEventTime}
 * returns the raw {@code int} bits; format with
 * {@link Integer#toUnsignedLong} for {@code %u} output.
 *
 * <p>Game time ({@code gGameTime}, 0x51C720, in {@code src/scripts.cc})
 * is canonical in {@link GameTime}; the {@code gameTime*} methods below
 * delegate there (the clock lived here until the scripts slice was
 * ported). This is unrelated to
 * {@link GameRandom.GameClock} (an injectable day-0 seam for the RNG,
 * still defaulting to 0); the two are deliberately not wired.
 *
 * <p>Owners: there is no game {@code Object} port yet, so owners are opaque
 * {@code java.lang.Object} references compared by identity ({@code ==},
 * like C pointer comparison; {@code null} owners work). C sets
 * {@code OBJECT_QUEUED} (0x2000) on the owner's flags in
 * {@code queue_add}; with no {@code flags} field available the bits are
 * tracked in an {@link IdentityHashMap} (never cleared, like C; the real
 * {@code Object} port will own the flag). See {@link #ownerFlagsOf}.
 *
 * <p>Data payloads are opaque cookies ({@code null}-tested only, like the
 * harness {@code 0x1001} pointers). {@code find} returns the stored
 * reference or {@code null} when exhausted, exactly like C returning
 * {@code queueListNode->data} / {@code nullptr}.
 *
 * <p>Descriptor table: the 14 rows (order, {@code clearOnMapExit} flags)
 * mirror {@code gEventTypeDescriptions} (0x51C540) faithfully, but every
 * game handler ({@code drugEffectEventProcess}, {@code knockoutClear},
 * ...) is unported, so handler slots default to a no-op returning 0 and
 * free/read/write slots are {@code null} (KNOCKOUT/SNEAK are
 * {@code null} in C too; elsewhere C's {@code internal_free} needs no
 * equivalent under GC and payload serde lives with the future type
 * ports). Vectors only hit the idle process path, so handlers are never
 * invoked there. Per-type handler overrides are available via
 * {@link #setHandler} for the future game ports. {@code _queue_leaving_map}
 * (the only consumer of {@code clearOnMapExit}/{@code mapExitHandlerProc})
 * is out of scope and not ported.
 *
 * <p>Save/load uses the {@link Db} big-endian helpers over
 * {@link XFile.Handle} (byte-identical to C, which byteswaps to
 * big-endian): count, then per event time/type/objectId plus the
 * type's {@code writeProc} (all {@code null} here, so nothing extra).
 * Object ids: {@code null} owner maps to {@code -2} like C; non-null
 * owners need the object system ({@code objectFindFirst} /
 * {@code inventoryFindById}, unported). Until then an optional
 * {@link OwnerIdMapper} seam applies, defaulting to write {@code -1} and
 * load {@code null} (mirroring C's lookup-miss fallthrough, which leaves
 * a {@code nullptr} owner without failing).
 *
 * <p>Faithful quirks preserved: {@code queueClearByEventType} with a
 * {@code null} fn removes (C: {@code fn != nullptr && ...} is false),
 * otherwise fn returning anything but 1 KEEPs the node; after a removal
 * the next pointer is re-read (sfall map-exit fix). {@code queueClear} /
 * remove / clear-by-type do not touch the find cursor
 * ({@code gLastFoundQueueListNode}), like C.
 */
public final class EventQueue {

    private EventQueue() {
    }

    /** Port of {@code EventType} (declaration order = C values 0-13). */
    public enum EventType {
        DRUG,
        KNOCKOUT,
        WITHDRAWAL,
        SCRIPT,
        GAME_TIME,
        POISON,
        RADIATION,
        FLARE,
        EXPLOSION,
        ITEM_TRICKLE,
        SNEAK,
        EXPLOSION_FAILURE,
        MAP_UPDATE_EVENT,
        GSOUND_SFX_EVENT;

        /** Port of {@code EVENT_TYPE_COUNT}. */
        public static final int COUNT = 14;

        /** Port of {@code EVENT_TYPE_FIRST}. */
        public static final EventType FIRST = DRUG;

        /** C enum value ({@code ordinal()} matches by declaration order). */
        public int id() {
            return ordinal();
        }

        /** Null on out-of-range (load then fails with -1, like C). */
        public static EventType fromId(int id) {
            EventType[] values = values();
            return (id >= 0 && id < values.length) ? values[id] : null;
        }
    }

    /** Port of {@code QueueEventHandler} (returns stop flag, 0/1, -1 possible). */
    public interface QueueEventHandler {
        int handle(Object owner, Object data);
    }

    /** Port of {@code QueueEventDataFreeProc}. */
    public interface QueueEventDataFreeProc {
        void free(Object data);
    }

    /** Port of {@code QueueEventDataReadProc} (0 ok / -1 error; data via out[0]). */
    public interface QueueEventDataReadProc {
        int read(XFile.Handle stream, Object[] dataOut);
    }

    /** Port of {@code QueueEventDataWriteProc} (0 ok / -1 error). */
    public interface QueueEventDataWriteProc {
        int write(XFile.Handle stream, Object data);
    }

    /**
     * Maps a non-null queue owner to its save id and back. Unset
     * ({@code null}, the default) means the object system is unavailable:
     * saves write {@code -1}, loads resolve any id other than {@code -2}
     * to {@code null} (C lookup-miss fallthrough).
     */
    public interface OwnerIdMapper {
        int ownerToId(Object owner);

        Object idToOwner(int objectId);
    }

    /** Port of {@code EventTypeDescription} (0x51C540 rows). */
    static final class EventTypeDescription {
        QueueEventHandler handlerProc;
        QueueEventDataFreeProc freeProc;
        QueueEventDataReadProc readProc;
        QueueEventDataWriteProc writeProc;
        boolean clearOnMapExit;
        QueueEventHandler mapExitHandlerProc;
    }

    /** Port of {@code QueueListNode}; {@code time} is unsigned (see class docs). */
    static final class QueueListNode {
        int time;
        EventType type;
        Object owner;
        Object data;
        QueueListNode next;
    }

    /** scripts.cc {@code gGameTime} (0x51C720) static init (canonical in {@link GameTime}). */
    public static final int GAME_TIME_INITIAL = GameTime.GAME_TIME_INITIAL;

    /** C object id written for a {@code null} owner (and read back to {@code null}). */
    static final int NULL_OBJECT_ID = -2;

    /** Default object id written when no {@link OwnerIdMapper} is installed. */
    static final int UNRESOLVED_OBJECT_ID = -1;

    /** {@code src/obj_types.h} {@code OBJECT_QUEUED} (0x2000). */
    static final int OBJECT_QUEUED = ObjTypes.ObjectFlags.QUEUED.bits();

    // 0x6648C0 queue
    private static QueueListNode queueListHead;

    // 0x51C690 tmpQNode
    private static QueueListNode lastFoundQueueListNode;

    private static final EventTypeDescription[] eventTypeDescriptions = new EventTypeDescription[EventType.COUNT];

    // Stand-in for Object.flags (no game Object port yet): OBJECT_QUEUED bits by identity.
    private static final Map<Object, Integer> ownerFlags = new IdentityHashMap<>();

    private static OwnerIdMapper ownerIdMapper;

    private static final QueueEventHandler NO_OP_HANDLER = (owner, data) -> 0;

    static {
        initDescriptors();
    }

    private static EventTypeDescription describe(boolean clearOnMapExit) {
        EventTypeDescription description = new EventTypeDescription();
        // Game-effect handlers are unported: vectors never invoke them
        // (idle process path only); default to a no-op returning 0.
        description.handlerProc = NO_OP_HANDLER;
        // KNOCKOUT/SNEAK are null in C too; elsewhere C's internal_free /
        // payload serde need no Java equivalent yet (see class docs).
        description.freeProc = null;
        description.readProc = null;
        description.writeProc = null;
        description.clearOnMapExit = clearOnMapExit;
        description.mapExitHandlerProc = null;
        return description;
    }

    private static void initDescriptors() {
        // Faithful row order/flags of gEventTypeDescriptions (0x51C540).
        eventTypeDescriptions[EventType.DRUG.id()] = describe(true);
        eventTypeDescriptions[EventType.KNOCKOUT.id()] = describe(true);
        eventTypeDescriptions[EventType.WITHDRAWAL.id()] = describe(true);
        eventTypeDescriptions[EventType.SCRIPT.id()] = describe(true);
        eventTypeDescriptions[EventType.GAME_TIME.id()] = describe(true);
        eventTypeDescriptions[EventType.POISON.id()] = describe(false);
        eventTypeDescriptions[EventType.RADIATION.id()] = describe(false);
        eventTypeDescriptions[EventType.FLARE.id()] = describe(true);
        eventTypeDescriptions[EventType.EXPLOSION.id()] = describe(true);
        eventTypeDescriptions[EventType.ITEM_TRICKLE.id()] = describe(true);
        eventTypeDescriptions[EventType.SNEAK.id()] = describe(true);
        eventTypeDescriptions[EventType.EXPLOSION_FAILURE.id()] = describe(true);
        eventTypeDescriptions[EventType.MAP_UPDATE_EVENT.id()] = describe(true);
        eventTypeDescriptions[EventType.GSOUND_SFX_EVENT.id()] = describe(true);
    }

    /** Test seam: override a type's handler ({@code null} restores the no-op). */
    public static void setHandler(EventType type, QueueEventHandler handler) {
        eventTypeDescriptions[type.id()].handlerProc = handler != null ? handler : NO_OP_HANDLER;
    }

    /** Test/porting seam for the future object system (save/load ids). */
    public static void setOwnerIdMapper(OwnerIdMapper mapper) {
        ownerIdMapper = mapper;
    }

    /** Fresh-process mirror: empty list, null find cursor, base game time. */
    static void resetForTests() {
        queueListHead = null;
        lastFoundQueueListNode = null;
        ownerFlags.clear();
        GameTime.resetClockForTests();
        ownerIdMapper = null;
        initDescriptors();
    }

    // scripts.cc 0x4A3330 game_time (canonical store: GameTime).
    public static int gameTimeGetTime() {
        return GameTime.gameTimeGetTime();
    }

    // scripts.cc 0x4A347C (0 maps to 1, like C; endgame side effects live
    // with the future Game port: see GameTime's timeout seam).
    public static void gameTimeSetTime(int time) {
        GameTime.gameTimeSetTime(time);
    }

    // scripts.cc 0x4A34CC inc_game_time (tick add only; the year>=13
    // endgame quit request is out of scope until Game is ported:
    // see GameTime's timeout seam, default off).
    public static void gameTimeAddTicks(int ticks) {
        GameTime.gameTimeAddTicks(ticks);
    }

    /** Stand-in {@code OBJECT_QUEUED} view (identity-keyed; 0 when never queued). */
    static int ownerFlagsOf(Object owner) {
        Integer flags = ownerFlags.get(owner);
        return flags != null ? flags : 0;
    }

    // 0x4A2320 queue_init
    public static void queueInit() {
        queueListHead = null;
    }

    // 0x4A2330 queue_reset
    public static int queueExit() {
        queueClear();
        return 0;
    }

    // 0x4A2338 queue_load
    public static int queueLoad(XFile.Handle stream) {
        int[] countOut = new int[1];
        if (Db.fileReadInt32(stream, countOut) == -1) {
            return -1;
        }
        int count = countOut[0];

        QueueListNode oldListHead = queueListHead;
        queueListHead = null;

        QueueListNode tail = null;

        int rc = 0;
        for (int index = 0; index < count; index++) {
            QueueListNode node = new QueueListNode();

            long[] timeOut = new long[1];
            if (Db.fileReadUInt32(stream, timeOut) == -1) {
                rc = -1;
                break;
            }
            node.time = (int) timeOut[0];

            int[] typeOut = new int[1];
            if (Db.fileReadInt32(stream, typeOut) == -1) {
                rc = -1;
                break;
            }

            EventType type = EventType.fromId(typeOut[0]);
            if (type == null) {
                rc = -1;
                break;
            }
            node.type = type;

            int[] objectIdOut = new int[1];
            if (Db.fileReadInt32(stream, objectIdOut) == -1) {
                rc = -1;
                break;
            }
            int objectId = objectIdOut[0];

            Object owner;
            if (objectId == NULL_OBJECT_ID) {
                owner = null;
            } else if (ownerIdMapper != null) {
                owner = ownerIdMapper.idToOwner(objectId);
            } else {
                // No object system: C lookup-miss fallthrough leaves null.
                owner = null;
            }
            node.owner = owner;

            EventTypeDescription description = eventTypeDescriptions[node.type.id()];
            if (description.readProc != null) {
                Object[] dataOut = new Object[1];
                if (description.readProc.read(stream, dataOut) == -1) {
                    rc = -1;
                    break;
                }
                node.data = dataOut[0];
            } else {
                node.data = null;
            }

            node.next = null;

            if (tail == null) {
                queueListHead = node;
            } else {
                tail.next = node;
            }
            tail = node;
        }

        if (rc == -1) {
            // Drop the partial load (freeing payloads like C), restoring the
            // old list below — C frees gQueueListHead then merges oldListHead
            // into the now-empty list, i.e. the old queue survives.
            QueueListNode node = queueListHead;
            while (node != null) {
                EventTypeDescription description = eventTypeDescriptions[node.type.id()];
                if (description.freeProc != null) {
                    description.freeProc.free(node.data);
                }
                node = node.next;
            }
            queueListHead = null;
        }

        // Merge the pre-load list back, sorted by time (stable: old nodes go
        // after equal-time loaded nodes, like C's `>` break).
        while (oldListHead != null) {
            QueueListNode next = oldListHead.next;

            QueueListNode prev = null;
            QueueListNode current = queueListHead;
            while (current != null) {
                if (Integer.compareUnsigned(current.time, oldListHead.time) > 0) {
                    break;
                }
                prev = current;
                current = current.next;
            }

            oldListHead.next = current;
            if (prev == null) {
                queueListHead = oldListHead;
            } else {
                prev.next = oldListHead;
            }
            oldListHead = next;
        }

        return rc;
    }

    // 0x4A24E0 queue_save
    public static int queueSave(XFile.Handle stream) {
        int count = 0;
        QueueListNode node = queueListHead;
        while (node != null) {
            count++;
            node = node.next;
        }

        if (Db.fileWriteInt32(stream, count) == -1) {
            return -1;
        }

        node = queueListHead;
        while (node != null) {
            int objectId;
            if (node.owner == null) {
                objectId = NULL_OBJECT_ID;
            } else if (ownerIdMapper != null) {
                objectId = ownerIdMapper.ownerToId(node.owner);
            } else {
                objectId = UNRESOLVED_OBJECT_ID;
            }

            if (Db.fileWriteUInt32(stream, Integer.toUnsignedLong(node.time)) == -1) {
                return -1;
            }

            if (Db.fileWriteInt32(stream, node.type.id()) == -1) {
                return -1;
            }

            if (Db.fileWriteInt32(stream, objectId) == -1) {
                return -1;
            }

            EventTypeDescription description = eventTypeDescriptions[node.type.id()];
            if (description.writeProc != null) {
                if (description.writeProc.write(stream, node.data) == -1) {
                    return -1;
                }
            }

            node = node.next;
        }

        return 0;
    }

    // 0x4A258C queue_add
    /**
     * Port of {@code ScriptEvent} ({@code src/scripts.cc}): the data of a
     * timer event — the script it belongs to and the parameter the script
     * reads back when the event fires.
     */
    public static final class ScriptEvent {
        public final int sid;
        public final int fixedParam;

        public ScriptEvent(int sid, int fixedParam) {
            this.sid = sid;
            this.fixedParam = fixedParam;
        }
    }

    public static int queueAddEvent(int delay, Object owner, Object data, EventType type) {
        QueueListNode newNode = new QueueListNode();

        // C unsigned wrap: identical in two's-complement int arithmetic.
        newNode.time = gameTimeGetTime() + delay;
        newNode.type = type;
        newNode.owner = owner;
        newNode.data = data;

        if (owner != null) {
            ownerFlags.put(owner, ownerFlagsOf(owner) | OBJECT_QUEUED);
        }

        // Stable sorted insert: new node goes after equal-time nodes
        // (C breaks only on `newTime < nodeTime`, unsigned).
        QueueListNode prev = null;
        QueueListNode current = queueListHead;
        while (current != null) {
            if (Integer.compareUnsigned(newNode.time, current.time) < 0) {
                break;
            }
            prev = current;
            current = current.next;
        }

        newNode.next = current;
        if (prev == null) {
            queueListHead = newNode;
        } else {
            prev.next = newNode;
        }

        return 0;
    }

    // 0x4A25F4 queue_remove
    public static int queueRemoveEvents(Object owner) {
        QueueListNode prev = null;
        QueueListNode node = queueListHead;

        while (node != null) {
            if (node.owner == owner) {
                QueueListNode next = node.next;
                if (prev == null) {
                    queueListHead = next;
                } else {
                    prev.next = next;
                }

                EventTypeDescription description = eventTypeDescriptions[node.type.id()];
                if (description.freeProc != null) {
                    description.freeProc.free(node.data);
                }

                node = next;
            } else {
                prev = node;
                node = node.next;
            }
        }

        return 0;
    }

    // 0x4A264C queue_remove_this
    public static int queueRemoveEventsByType(Object owner, EventType type) {
        QueueListNode prev = null;
        QueueListNode node = queueListHead;

        while (node != null) {
            if (node.owner == owner && node.type == type) {
                QueueListNode next = node.next;
                if (prev == null) {
                    queueListHead = next;
                } else {
                    prev.next = next;
                }

                EventTypeDescription description = eventTypeDescriptions[node.type.id()];
                if (description.freeProc != null) {
                    description.freeProc.free(node.data);
                }

                node = next;
            } else {
                prev = node;
                node = node.next;
            }
        }

        return 0;
    }

    // scriptQueueRemoveUseFixedParam (src/scripts.cc): the clear filter knob.
    private static boolean scriptQueueRemoveUseFixedParam;

    /** Sets {@code scriptQueueRemoveUseFixedParam} (src/scripts.cc). */
    static void setScriptQueueRemoveUseFixedParam(boolean enabled) {
        scriptQueueRemoveUseFixedParam = enabled;
    }

    // scriptQueueRemoveBySid (src/scripts.cc:1105): the SCRIPT clear filter -
    // drop the event when the sid matches (and, optionally, the fixed param).
    static int queueRemoveTimerEventsBySid(int sid, int fixedParam) {
        QueueListNode prev = null;
        QueueListNode node = queueListHead;

        while (node != null) {
            QueueListNode next = node.next;
            boolean drop = false;
            if (node.type == EventType.SCRIPT && node.data instanceof ScriptEvent) {
                ScriptEvent event = (ScriptEvent) node.data;
                if (event.sid == sid
                        && (!scriptQueueRemoveUseFixedParam
                            || event.fixedParam == fixedParam)) {
                    drop = true;
                }
            }
            if (drop) {
                if (prev == null) {
                    queueListHead = next;
                } else {
                    prev.next = next;
                }
            } else {
                prev = node;
            }
            node = next;
        }

        return 0;
    }

    // 0x4A26A8 queue_find
    public static boolean queueHasEvent(Object owner, EventType type) {
        QueueListNode node = queueListHead;
        while (node != null) {
            if (owner == node.owner && type == node.type) {
                return true;
            }
            node = node.next;
        }

        return false;
    }

    // 0x4A26D0 queue_process
    public static int queueProcessEvents() {
        int time = gameTimeGetTime();
        boolean stopProcess = false;

        while (queueListHead != null) {
            QueueListNode node = queueListHead;
            if (Integer.compareUnsigned(time, node.time) < 0 || stopProcess) {
                break;
            }

            queueListHead = node.next;

            EventTypeDescription description = eventTypeDescriptions[node.type.id()];
            stopProcess = description.handlerProc.handle(node.owner, node.data) != 0;

            if (description.freeProc != null) {
                description.freeProc.free(node.data);
            }
        }

        return stopProcess ? 1 : 0;
    }

    // 0x4A2748 queue_clear
    public static void queueClear() {
        QueueListNode node = queueListHead;
        while (node != null) {
            QueueListNode next = node.next;

            EventTypeDescription description = eventTypeDescriptions[node.type.id()];
            if (description.freeProc != null) {
                description.freeProc.free(node.data);
            }

            node = next;
        }

        queueListHead = null;
    }

    // 0x4A2790 queue_clear_type
    public static void queueClearByEventType(EventType type, QueueEventHandler fn) {
        QueueListNode prev = null;
        QueueListNode current = queueListHead;

        while (current != null) {
            if (type == current.type) {
                QueueListNode tmp = current;

                if (prev == null) {
                    queueListHead = current.next;
                } else {
                    prev.next = current.next;
                }
                current = (prev == null) ? queueListHead : prev.next;

                // fn == null removes (C: `fn != nullptr && ...` is false);
                // otherwise anything but 1 KEEPs the node.
                if (fn != null && fn.handle(tmp.owner, tmp.data) != 1) {
                    tmp.next = current;
                    if (prev == null) {
                        queueListHead = tmp;
                    } else {
                        prev.next = tmp;
                    }
                    prev = tmp;
                } else {
                    EventTypeDescription description = eventTypeDescriptions[tmp.type.id()];
                    if (description.freeProc != null) {
                        description.freeProc.free(tmp.data);
                    }

                    // SFALL: re-read next event since `fn` handler can change it.
                    // This fixes crash when leaving the map while waiting for
                    // someone to die of a super stimpak overdose.
                    current = (prev == null) ? queueListHead : prev.next;
                }
            } else {
                prev = current;
                current = current.next;
            }
        }
    }

    // 0x4A2808 queue_next_time
    public static int queueGetNextEventTime() {
        if (queueListHead == null) {
            return 0;
        }

        return queueListHead.time;
    }

    // 0x4A294C queue_is_empty
    public static boolean queueIsEmpty() {
        return queueListHead == null;
    }

    /** Test seam: live events of a type. */
    static java.util.List<Object> eventsForTests(EventType type) {
        java.util.ArrayList<Object> out = new java.util.ArrayList<>();
        QueueListNode node = queueListHead;
        while (node != null) {
            if (node.type == type) {
                out.add(node.data);
            }
            node = node.next;
        }
        return out;
    }

    // 0x4A295C queue_find_first
    public static Object queueFindFirstEvent(Object owner, EventType type) {
        QueueListNode node = queueListHead;
        while (node != null) {
            if (owner == node.owner && type == node.type) {
                lastFoundQueueListNode = node;
                return node.data;
            }
            node = node.next;
        }

        lastFoundQueueListNode = null;
        return null;
    }

    // 0x4A2994 queue_find_next
    public static Object queueFindNextEvent(Object owner, EventType type) {
        if (lastFoundQueueListNode != null) {
            QueueListNode node = lastFoundQueueListNode.next;
            while (node != null) {
                if (owner == node.owner && type == node.type) {
                    lastFoundQueueListNode = node;
                    return node.data;
                }
                node = node.next;
            }
        }

        lastFoundQueueListNode = null;

        return null;
    }
}
