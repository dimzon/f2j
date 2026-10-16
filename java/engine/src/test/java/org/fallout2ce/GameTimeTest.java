package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [gametime]} vectors from
 * {@code docker/refharness/golden/v55.txt} (produced by
 * {@code sectionGameTime}). Needs no game data and no window system:
 * the clock starts at the {@code 302400} static init with zeroed start
 * fields, the map-header mirror is empty (single queue add), and the id
 * counter starts at {@code OBJECT_ID_UNIQUE_START}.
 *
 * <p>Flow mirrors the harness exactly, in order: t0/date/hour/timestr;
 * set/add(600 ticks + 6 seconds)/zero-quirk; schedule/next/has-gametime;
 * ids (fresh, two bumps, restore, bad-restore guard, reset).
 */
class GameTimeTest {

    @BeforeEach
    void setup() {
        GameTime.resetForTests();
        EventQueue.resetForTests();
    }

    @AfterEach
    void teardown() {
        GameTime.resetForTests();
        EventQueue.resetForTests();
        Db.resetForTests();
    }

    @Test
    void gameTimeVectors() {
        try {
            // Clock starts at the 302400 static init.
            assertEquals(302400, GameTime.gameTimeGetTime());

            int[] month = { -1 };
            int[] day = { -1 };
            int[] year = { -1 };
            GameTime.gameTimeGetDate(month, day, year);
            assertEquals(1, month[0]);
            assertEquals(1, day[0]);
            assertEquals(0, year[0]);

            assertEquals(824, GameTime.gameTimeGetHour());
            assertEquals("8:24", GameTime.gameTimeGetTimeString());

            // Set/add arithmetic (0 maps to 1).
            GameTime.gameTimeSetTime(1000);
            assertEquals(1000, GameTime.gameTimeGetTime());
            GameTime.gameTimeAddTicks(600);
            GameTime.gameTimeAddSeconds(6);
            assertEquals(1660, GameTime.gameTimeGetTime());
            GameTime.gameTimeSetTime(0);
            assertEquals(1, GameTime.gameTimeGetTime());
            GameTime.gameTimeSetTime(302400);

            // Midnight update event lands on the queue (no map header: single add).
            EventQueue.queueInit();
            int schedRc = GameTime.gameTimeScheduleUpdateEvent();
            int nextEvent = EventQueue.queueGetNextEventTime();
            int hasGameTime = EventQueue.queueHasEvent(null, EventQueue.EventType.GAME_TIME) ? 1 : 0;
            EventQueue.queueExit();
            assertEquals(0, schedRc);
            assertEquals(864000, nextEvent);
            assertEquals(1, hasGameTime);

            // Unique object id counter: fresh, bump, restore, guard rails.
            int idCounter0 = GameTime.scriptsGetUniqueObjectIdCounter();
            int idNew1 = GameTime.scriptsNewUniqueObjectId();
            int idNew2 = GameTime.scriptsNewUniqueObjectId();
            GameTime.scriptsRestoreUniqueObjectIdCounter(idNew1);
            int idRestored = GameTime.scriptsGetUniqueObjectIdCounter();
            GameTime.scriptsRestoreUniqueObjectIdCounter(-5);
            int idBadRestore = GameTime.scriptsGetUniqueObjectIdCounter();
            GameTime.scriptsResetUniqueObjectIdCounter();
            int idReset = GameTime.scriptsGetUniqueObjectIdCounter();
            assertEquals(268435455, idCounter0);
            assertEquals(268435456, idNew1);
            assertEquals(268435457, idNew2);
            assertEquals(268435456, idRestored);
            assertEquals(268435455, idBadRestore);
            assertEquals(268435455, idReset);
        } finally {
            // Other suites (EventQueueTest) assume the 302400 base.
            GameTime.gameTimeSetTime(302400);
        }
    }
}
