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

    @Test
    void extendedCalendarRollovers() {
        GameTime.resetClockForTests();
        GameTime.setStartDateForTests(2241, 6, 24);

        // The default start 08:24 is 302400 ticks.
        GameTime.gameTimeSetTime(302400);
        int[] sm = new int[1], sd = new int[1], sy = new int[1];
        GameTime.gameTimeGetDate(sm, sd, sy);
        assertEquals(7, sm[0]);
        assertEquals(25, sd[0]);
        assertEquals(2241, sy[0]);
        assertEquals(824, GameTime.gameTimeGetHour());
        assertEquals("8:24", GameTime.gameTimeGetTimeString());

        // 23:59 + 1 min -> midnight, next day.
        GameTime.gameTimeSetTime(863400);
        GameTime.gameTimeAddSeconds(60);
        assertEquals(864000, GameTime.gameTimeGetTime());
        int[] mm = new int[1], md = new int[1], my = new int[1];
        GameTime.gameTimeGetDate(mm, md, my);
        assertEquals(7, mm[0]);
        assertEquals(26, md[0]);
        assertEquals(2241, my[0]);
        assertEquals(0, GameTime.gameTimeGetHour());
        assertEquals("0:00", GameTime.gameTimeGetTimeString());

        // December 31st -> January 1st of the next year.
        GameTime.gameTimeSetTime((364 - 24) * 864000);
        int[] dm = new int[1], dd = new int[1], dy = new int[1];
        GameTime.gameTimeGetDate(dm, dd, dy);
        assertEquals(6, dm[0]);
        assertEquals(30, dd[0]);
        assertEquals(2242, dy[0]);

        GameTime.gameTimeAddTicks(864000);
        int[] jm = new int[1], jd = new int[1], jy = new int[1];
        GameTime.gameTimeGetDate(jm, jd, jy);
        assertEquals(7, jm[0]);
        assertEquals(1, jd[0]);
        assertEquals(2242, jy[0]);

        // One 365-day year later: same month/day, next year.
        GameTime.gameTimeSetTime((365 - 24) * 864000);
        int[] lm = new int[1], ld = new int[1], ly = new int[1];
        GameTime.gameTimeGetDate(lm, ld, ly);
        assertEquals(7, lm[0]);
        assertEquals(1, ld[0]);
        assertEquals(2242, ly[0]);
    }

    @Test
    void theThirteenYearTimeoutRequestsTheMainMenu() {
        GameTime.resetClockForTests();
        GameTime.setStartDateForTests(2241, 6, 24);
        GameScriptApi.gameUserWantsToQuit = GameScriptApi.GAME_QUIT_REQUEST_NONE;

        GameTime.gameTimeSetTime(GameTime.TICKS_PER_YEAR * 13 - 100);
        GameTime.gameTimeAddTicks(100);

        assertEquals(4099680000L, Integer.toUnsignedLong(GameTime.gameTimeGetTime()));
        assertEquals(GameScriptApi.GAME_QUIT_REQUEST_MAIN_MENU,
            GameScriptApi.gameUserWantsToQuit);
        GameScriptApi.gameUserWantsToQuit = GameScriptApi.GAME_QUIT_REQUEST_NONE;
    }
}
