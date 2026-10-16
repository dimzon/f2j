package org.fallout2ce;

/**
 * Port of the game-clock + script unique-id slice of {@code src/scripts.cc} /
 * {@code src/scripts.h} ({@code gGameTime} 0x51C720, {@code game_time} ...,
 * {@code gtime_q_add}, {@code uniqueObjectIdCounter} 0x51C7D4+4).
 *
 * <p>Time is an unsigned 32-bit tick count (1/10 second) stored in a Java
 * {@code int} (identical two's-complement wrap as C {@code unsigned int}).
 * The static init {@code 302400} mirrors
 * {@code static unsigned int gGameTime = 302400}.
 *
 * <p>Date constants from {@code src/scripts.h}: {@code GAME_TIME_TICKS_PER_HOUR}
 * (60 * 60 * 10), {@code GAME_TIME_TICKS_PER_DAY} (24 * 60 * 60 * 10),
 * {@code GAME_TIME_TICKS_PER_YEAR} (365 * 24 * 60 * 60 * 10).
 * (A separate {@code GAME_TIME_TICKS_PER_DAY} copy lives in
 * {@link GameRandom} for the RNG seam; left alone.)
 *
 * <p>Consolidation: the {@code gGameTime} get/set/add slice previously lived
 * in {@link EventQueue} ("until {@code Scripts} is ported"); it is now
 * canonical here and {@link EventQueue} delegates ({@code queue_add} /
 * {@code queue_process} read this clock). {@link EventQueue#resetForTests}
 * resets only the clock (never the id counter) so its vectors stay green.
 *
 * <p>Documented seams (all default to off/no-op, matching the harness BSS):
 * <ul>
 * <li>Endgame timeout in {@code gameTimeAddTicks}: C calls
 * {@code endgameSetupDeathEnding(ENDGAME_DEATH_ENDING_REASON_TIMEOUT)} and
 * sets the quit request to the main menu when
 * {@code gGameTime / GAME_TIME_TICKS_PER_YEAR >= 13} (needs
 * {@code game.cc}, unported). The year counter is computed faithfully, but
 * the side effects only run when a {@link TimeoutHandler} is installed
 * (default {@code null} = off/no-op). The dead
 * {@code shouldProcessGameTimeEvent} branch (always false in C) is not
 * ported.
 * <li>Map header in {@code gameTimeScheduleUpdateEvent}: C adds the second
 * ({@code MAP_UPDATE_EVENT}, 600-tick) event only when
 * {@code gMapHeader.name[0] != '\0'}. There is no map port yet, so a
 * package-static name mirror (default empty, like the BSS) stands in; see
 * {@link #setMapHeaderName}.
 * </ul>
 *
 * <p>Unique ids ({@code OBJECT_ID_UNIQUE_START} ...; all {@code scripts.cc}
 * residents, kept here with the clock as the one "game clock + script
 * unique ids" slice): {@link #scriptsIsUniqueObjectId} is ported although
 * unvectored (trivially pure one-liner); the test asserts only harness
 * vectors, no extra legs for it.
 */
public final class GameTime {

    private GameTime() {
    }

    /** Port of {@code GAME_TIME_TICKS_PER_HOUR} (60 * 60 * 10). */
    public static final int TICKS_PER_HOUR = 36000;

    /** Port of {@code GAME_TIME_TICKS_PER_DAY} (24 * 60 * 60 * 10). */
    public static final int TICKS_PER_DAY = 864000;

    /** Port of {@code GAME_TIME_TICKS_PER_YEAR} (365 * 24 * 60 * 60 * 10). */
    public static final int TICKS_PER_YEAR = 315360000;

    /** scripts.cc {@code gGameTime} (0x51C720) static init: 302400 ticks. */
    public static final int GAME_TIME_INITIAL = 302400;

    /** Port of {@code OBJECT_ID_UNIQUE_START} ({@code src/scripts.h}). */
    public static final int OBJECT_ID_UNIQUE_START = 0x0FFFFFFF;

    /** Port of {@code OBJECT_ID_UNIQUE_END} ({@code src/scripts.cc}). */
    public static final int OBJECT_ID_UNIQUE_END = 0x7FFFFFFF;

    // scripts.cc OBJECT_ID_PLAYER / OBJECT_ID_PARTY_MEMBER_END (for isUnique).
    private static final int OBJECT_ID_PLAYER = 18000;
    private static final int OBJECT_ID_PARTY_MEMBER_END = OBJECT_ID_PLAYER + 0x01000000;

    /** Port of {@code gGameTimeDaysPerMonth} (0x51C724), transcribed verbatim. */
    private static final int[] DAYS_PER_MONTH = {
        31, // Jan
        28, // Feb
        31, // Mar
        30, // Apr
        31, // May
        30, // Jun
        31, // Jul
        31, // Aug
        30, // Sep
        31, // Oct
        30, // Nov
        31, // Dec
    };

    // 0x51C720 fallout_game_time
    private static int gameTime = GAME_TIME_INITIAL;

    // Start-of-game date BSS zeros (set by game init, unported).
    private static int startYear;
    private static int startMonth;
    private static int startDay;
    private static int startTimeTicks;

    // 0x51C7D4+4 uniqueObjectIdCounter = OBJECT_ID_UNIQUE_START.
    private static int uniqueObjectIdCounter = OBJECT_ID_UNIQUE_START;

    // Stand-in for gMapHeader.name (map.cc BSS: empty); only emptiness is observed.
    private static String mapHeaderName = "";

    /**
     * Runs the year &gt;= 13 timeout side effects
     * ({@code endgameSetupDeathEnding(TIMEOUT)} + quit-to-main-menu request
     * in C). Unset ({@code null}, the default) means unported: no-op.
     */
    public interface TimeoutHandler {
        void onTimeout();
    }

    private static TimeoutHandler timeoutHandler;

    /** Test/porting seam for the future game/map ports (default empty = BSS). */
    public static void setMapHeaderName(String name) {
        mapHeaderName = name != null ? name : "";
    }

    /** Test/porting seam for the future game port (default {@code null} = off). */
    public static void setTimeoutHandler(TimeoutHandler handler) {
        timeoutHandler = handler;
    }

    /** Fresh-process mirror: clock to 302400, start fields to 0. */
    static void resetClockForTests() {
        gameTime = GAME_TIME_INITIAL;
        startYear = 0;
        startMonth = 0;
        startDay = 0;
        startTimeTicks = 0;
    }

    /** Fresh-process mirror: clock, start fields, id counter, both seams. */
    public static void resetForTests() {
        resetClockForTests();
        uniqueObjectIdCounter = OBJECT_ID_UNIQUE_START;
        mapHeaderName = "";
        timeoutHandler = null;
    }

    // 0x4A3330 game_time
    public static int gameTimeGetTime() {
        return gameTime;
    }

    /**
     * 0x4A3338. {@code null} outs are skipped, like the C null-pointer
     * guards; non-null outs must have length &ge; 1.
     */
    public static void gameTimeGetDate(int[] monthOut, int[] dayOut, int[] yearOut) {
        long days = Integer.toUnsignedLong(gameTime) / TICKS_PER_DAY + startDay;
        int year = (int) (days / 365) + startYear;
        int month = startMonth;
        int day = (int) (days % 365);

        while (true) {
            int daysInMonth = DAYS_PER_MONTH[month];
            if (day < daysInMonth) {
                break;
            }

            month++;
            day -= daysInMonth;

            if (month == 12) {
                year++;
                month = 0;
            }
        }

        if (dayOut != null) {
            dayOut[0] = day + 1;
        }

        if (monthOut != null) {
            monthOut[0] = month + 1;
        }

        if (yearOut != null) {
            yearOut[0] = year;
        }
    }

    // 0x4A33C8 game_time_hour (military hhmm).
    public static int gameTimeGetHour() {
        return 100 * ((gameTime / 600) / 60 % 24) + (gameTime / 600) % 60;
    }

    /**
     * 0x4A3420. C formats into a static 7-char buffer
     * ({@code "%d:%02d"}); Java returns the {@code String} directly.
     */
    public static String gameTimeGetTimeString() {
        return String.format("%d:%02d", (gameTime / 600) / 60 % 24, (gameTime / 600) % 60);
    }

    // 0x4A347C (0 maps to 1, like C; unsigned store).
    public static void gameTimeSetTime(int time) {
        if (time == 0) {
            time = 1;
        }
        gameTime = time;
    }

    /**
     * 0x4A34CC inc_game_time. The tick add and the year &gt;= 13 counter
     * are faithful; the endgame/quit side effects only run when a
     * {@link TimeoutHandler} is installed (default off/no-op).
     */
    public static void gameTimeAddTicks(int ticks) {
        gameTime += ticks;

        long year = Integer.toUnsignedLong(gameTime) / TICKS_PER_YEAR;
        if (year >= 13 && timeoutHandler != null) {
            timeoutHandler.onTimeout();
        }
    }

    // 0x4A3518 inc_game_time_in_seconds (x10).
    public static void gameTimeAddSeconds(int seconds) {
        gameTimeAddTicks(seconds * 10);
    }

    /**
     * 0x4A3570 gtime_q_add. Midnight-delay event plus the 600-tick map
     * update event only when the map-header name is non-empty (default
     * empty, so a single add, like the harness).
     */
    public static int gameTimeScheduleUpdateEvent() {
        // Ticks until midnight.
        int delay = 10 * (60 * (60 - (gameTime / 600) % 60 - 1)
            + 3600 * (24 - (gameTime / 600) / 60 % 24 - 1) + 60);
        if (EventQueue.queueAddEvent(delay, null, null, EventQueue.EventType.GAME_TIME) == -1) {
            return -1;
        }

        if (!mapHeaderName.isEmpty()) {
            if (EventQueue.queueAddEvent(600, null, null, EventQueue.EventType.MAP_UPDATE_EVENT) == -1) {
                return -1;
            }
        }

        return 0;
    }

    // scriptsIsUniqueObjectId (unvectored one-liner, ported as-is).
    public static boolean scriptsIsUniqueObjectId(int objectId) {
        return objectId > OBJECT_ID_UNIQUE_START
            || (objectId >= OBJECT_ID_PLAYER && objectId < OBJECT_ID_PARTY_MEMBER_END);
    }

    // scriptsNewUniqueObjectId (wrap-then-bump).
    public static int scriptsNewUniqueObjectId() {
        if (uniqueObjectIdCounter >= OBJECT_ID_UNIQUE_END) {
            uniqueObjectIdCounter = OBJECT_ID_UNIQUE_START;
        }

        uniqueObjectIdCounter++;
        return uniqueObjectIdCounter;
    }

    // scriptsGetUniqueObjectIdCounter
    public static int scriptsGetUniqueObjectIdCounter() {
        return uniqueObjectIdCounter;
    }

    // scriptsResetUniqueObjectIdCounter
    public static void scriptsResetUniqueObjectIdCounter() {
        uniqueObjectIdCounter = OBJECT_ID_UNIQUE_START;
    }

    // scriptsRestoreUniqueObjectIdCounter (range guard, else START).
    public static void scriptsRestoreUniqueObjectIdCounter(int savedCounter) {
        if (savedCounter >= OBJECT_ID_UNIQUE_START && savedCounter <= OBJECT_ID_UNIQUE_END) {
            uniqueObjectIdCounter = savedCounter;
        } else {
            uniqueObjectIdCounter = OBJECT_ID_UNIQUE_START;
        }
    }
}
