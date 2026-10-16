package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code [eventqueue]} vectors from
 * {@code docker/refharness/golden/v50.txt} (produced by
 * {@code sectionEventQueue}). Needs no game data and no window system:
 * game time is the {@code 302400} static init, owners are {@code null},
 * and payloads are opaque cookies for types with no free/read/write
 * procs (KNOCKOUT/SNEAK), null-tested only through find.
 *
 * <p>{@code next=302450} pins the {@code 302400} base (50-tick SNEAK
 * delay), guarding regressions of the static init.
 */
class EventQueueTest {

    @TempDir
    Path temp;

    @BeforeEach
    void setup() {
        EventQueue.resetForTests();
    }

    @AfterEach
    void teardown() {
        EventQueue.resetForTests();
        Db.resetForTests();
    }

    @Test
    void eventQueueVectors() {
        // Static-init base (scripts.cc gGameTime = 302400), not wall clock.
        assertEquals(302400, EventQueue.gameTimeGetTime());

        EventQueue.queueInit();
        int emptyInit = EventQueue.queueIsEmpty() ? 1 : 0;
        int nextEmpty = EventQueue.queueGetNextEventTime();
        Object cookieKnock1 = new Object();
        Object cookieSneak = new Object();
        Object cookieKnock2 = new Object();
        int add1 = EventQueue.queueAddEvent(100, null, cookieKnock1, EventQueue.EventType.KNOCKOUT);
        int add2 = EventQueue.queueAddEvent(50, null, cookieSneak, EventQueue.EventType.SNEAK);
        int add3 = EventQueue.queueAddEvent(150, null, cookieKnock2, EventQueue.EventType.KNOCKOUT);
        int nextTime = EventQueue.queueGetNextEventTime();
        int emptyAfter = EventQueue.queueIsEmpty() ? 1 : 0;
        int hasKnock = EventQueue.queueHasEvent(null, EventQueue.EventType.KNOCKOUT) ? 1 : 0;
        int hasDrug = EventQueue.queueHasEvent(null, EventQueue.EventType.DRUG) ? 1 : 0;
        assertEquals(1, emptyInit);
        assertEquals(0, nextEmpty);
        assertEquals(0, add1);
        assertEquals(0, add2);
        assertEquals(0, add3);
        assertEquals(302450, nextTime);
        assertEquals(0, emptyAfter);
        assertEquals(1, hasKnock);
        assertEquals(0, hasDrug);

        // Find chain walks the two KNOCKOUT nodes in time order, then exhausts.
        Object found1 = EventQueue.queueFindFirstEvent(null, EventQueue.EventType.KNOCKOUT);
        Object found2 = EventQueue.queueFindNextEvent(null, EventQueue.EventType.KNOCKOUT);
        Object found3 = EventQueue.queueFindNextEvent(null, EventQueue.EventType.KNOCKOUT);
        assertNotNull(found1);
        assertNotNull(found2);
        assertNull(found3);
        assertSame(cookieKnock1, found1);
        assertSame(cookieKnock2, found2);

        // No due events (game time 302400 < 302450): no handler runs.
        int procIdle = EventQueue.queueProcessEvents();
        int nextIdle = EventQueue.queueGetNextEventTime();
        int remType = EventQueue.queueRemoveEventsByType(null, EventQueue.EventType.KNOCKOUT);
        int hasKnockAfter = EventQueue.queueHasEvent(null, EventQueue.EventType.KNOCKOUT) ? 1 : 0;
        assertEquals(0, procIdle);
        assertEquals(302450, nextIdle);
        assertEquals(0, remType);
        assertEquals(0, hasKnockAfter);

        // Save/clear/load round-trip (SNEAK has no serde procs: only
        // count/time/type/objectId cross the file; data loads back null).
        String scratch = temp.resolve("qev.bin").toString();
        XFile.Handle saveStream = Db.fileOpen(scratch, "wb");
        assertNotNull(saveStream);
        int saveRc = EventQueue.queueSave(saveStream);
        assertEquals(0, Db.fileClose(saveStream));
        EventQueue.queueClear();
        int emptyCleared = EventQueue.queueIsEmpty() ? 1 : 0;
        XFile.Handle loadStream = Db.fileOpen(scratch, "rb");
        assertNotNull(loadStream);
        int loadRc = EventQueue.queueLoad(loadStream);
        assertEquals(0, Db.fileClose(loadStream));
        int nextLoaded = EventQueue.queueGetNextEventTime();
        int hasSneakLoaded = EventQueue.queueHasEvent(null, EventQueue.EventType.SNEAK) ? 1 : 0;
        assertEquals(0, saveRc);
        assertEquals(1, emptyCleared);
        assertEquals(0, loadRc);
        assertEquals(302450, nextLoaded);
        assertEquals(1, hasSneakLoaded);

        // Conditional clear (fn returning 1 = remove) drops the survivor.
        int[] clearCalls = new int[1];
        EventQueue.queueClearByEventType(EventQueue.EventType.SNEAK, (owner, data) -> {
            clearCalls[0]++;
            return 1;
        });
        int emptyAfterClear = EventQueue.queueIsEmpty() ? 1 : 0;
        int exitRc = EventQueue.queueExit();
        assertEquals(1, clearCalls[0]);
        assertEquals(1, emptyAfterClear);
        assertEquals(0, exitRc);

        assertTrue(EventQueue.queueIsEmpty());
        assertFalse(EventQueue.queueHasEvent(null, EventQueue.EventType.SNEAK));
    }
}
