package io.github.jmsdnns.androidmtrls.content

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

// ---------------------------------------------------------------------------
// AMCalendar month-grid math
// ---------------------------------------------------------------------------
//
// Pure `java.time` helpers extracted from AMCalendar so the grid-layout logic
// is unit-testable on the JVM without Compose or an Android device (the house
// "extract the math" pattern shared with CardStackMath / FlowMath). AMCalendar
// renders these; the tests in CalendarMathTest pin them.
//
// This is the same locale-correct recipe AMDatePickerDialog builds inline: the
// leading pad is the distance from the week's first day to the 1st's weekday,
// and `DayOfWeek.value` is the ISO weekday number MONDAY=1 … SUNDAY=7.

/**
 * Lays [month] out as a flat list of week cells, ready to `chunked(7)` into rows.
 *
 * Leading `null`s pad the 1st out so column 0 lines up with [firstDayOfWeek];
 * then one [LocalDate] per day of the month in order; then trailing `null`s
 * complete the final week. The result's size is always a multiple of 7, so
 * every row has exactly seven cells. `null` cells fall outside the displayed
 * month and render blank — no adjacent-month spillover numbers.
 *
 * @param month          The month to lay out.
 * @param firstDayOfWeek The weekday that starts each row (locale-derived by the
 *                       caller via `WeekFields.of(locale).firstDayOfWeek`).
 */
internal fun amMonthDayCells(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
): List<LocalDate?> {
    val firstOfMonth = month.atDay(1)
    // Distance (0..6) from the week's first day to the weekday the 1st falls on.
    val leading = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val length = month.lengthOfMonth()

    val cells = ArrayList<LocalDate?>(leading + length + 6)
    repeat(leading) { cells.add(null) }
    for (day in 1..length) cells.add(month.atDay(day))
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

/**
 * The seven weekdays rotated so the list begins on [firstDayOfWeek] — the
 * column order for the weekday header (e.g. Sunday-first `[SUN..SAT]` in the US,
 * Monday-first `[MON..SUN]` across most of Europe).
 *
 * @param firstDayOfWeek The weekday that starts each row.
 */
internal fun amOrderedWeekdays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
    (0L until 7L).map { firstDayOfWeek.plus(it) }
