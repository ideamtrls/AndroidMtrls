package io.github.jmsdnns.androidmtrls.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test

/**
 * JVM unit tests for AMCalendar pure month-grid math: [amMonthDayCells] and
 * [amOrderedWeekdays]. No Compose, no Android — plain `java.time`.
 *
 * ## What is under test
 *
 * [amMonthDayCells] lays a month out as a flat cell list: leading `null`s pad
 * the 1st out to the given `firstDayOfWeek`, then one `LocalDate` per day, then
 * trailing `null`s complete the last week so `size % 7 == 0`.
 *
 * `java.time.DayOfWeek.value` is the ISO weekday number MONDAY=1 … SUNDAY=7,
 * which is exactly what the leading-pad formula
 * `(firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7` consumes.
 *
 * ## Reference fixtures (verified against `java.time` / Python `datetime`)
 *
 *   Feb 2024 → first day Thursday (value 4), 29 days (leap year)
 *   Feb 2026 → first day Sunday   (value 7), 28 days
 *   Jun 2026 → first day Monday   (value 1), 30 days
 */
class CalendarMathTest {

    private val february2024 = YearMonth.of(2024, 2)
    private val february2026 = YearMonth.of(2026, 2)
    private val june2026 = YearMonth.of(2026, 6)

    // -------------------------------------------------------------------------
    // Shared invariants — must hold for every month / first-weekday combination
    // -------------------------------------------------------------------------

    /** Non-null entries are the month's days in order, 1..lengthOfMonth. */
    private fun assertDaysInOrder(month: YearMonth, cells: List<LocalDate?>) {
        val days = cells.filterNotNull()
        assertEquals(
            "one cell per day of the month",
            month.lengthOfMonth(),
            days.size,
        )
        days.forEachIndexed { index, date ->
            assertEquals("day at slot $index", month.atDay(index + 1), date)
        }
    }

    // -------------------------------------------------------------------------
    // amMonthDayCells — leap February (29 days)
    // -------------------------------------------------------------------------

    @Test
    fun `leap Feb 2024 Sunday-first pads 4 leading and 2 trailing nulls`() {
        // First = Thursday(4); Sunday-first(7): leading = (4 - 7 + 7) % 7 = 4.
        // 4 + 29 = 33 → padded to 35 (5 weeks), 2 trailing nulls.
        val cells = amMonthDayCells(february2024, DayOfWeek.SUNDAY)

        assertEquals("total cells", 35, cells.size)
        assertEquals("leading nulls", 4, cells.takeWhile { it == null }.size)
        assertEquals("first day sits at column 4", february2024.atDay(1), cells[4])
        assertEquals("Feb 29 present", february2024.atDay(29), cells[32])
        assertEquals("trailing nulls", 2, cells.takeLastWhile { it == null }.size)
        assertDaysInOrder(february2024, cells)
    }

    @Test
    fun `leap Feb 2024 Monday-first pads 3 leading and 3 trailing nulls`() {
        // First = Thursday(4); Monday-first(1): leading = (4 - 1 + 7) % 7 = 3.
        // 3 + 29 = 32 → padded to 35, 3 trailing nulls.
        val cells = amMonthDayCells(february2024, DayOfWeek.MONDAY)

        assertEquals("total cells", 35, cells.size)
        assertEquals("leading nulls", 3, cells.takeWhile { it == null }.size)
        assertEquals("first day sits at column 3", february2024.atDay(1), cells[3])
        assertEquals("Feb 29 present", february2024.atDay(29), cells[31])
        assertEquals("trailing nulls", 3, cells.takeLastWhile { it == null }.size)
        assertDaysInOrder(february2024, cells)
    }

    // -------------------------------------------------------------------------
    // amMonthDayCells — Feb 2026 starts on a Sunday
    // -------------------------------------------------------------------------

    @Test
    fun `Feb 2026 starts on Sunday so Sunday-first has zero leading nulls`() {
        // First = Sunday(7); Sunday-first: leading = (7 - 7 + 7) % 7 = 0.
        // 28 days is already a multiple of 7 → no padding at all (exactly 4 weeks).
        val cells = amMonthDayCells(february2026, DayOfWeek.SUNDAY)

        assertEquals("total cells", 28, cells.size)
        assertEquals("no leading nulls", 0, cells.takeWhile { it == null }.size)
        assertEquals("no trailing nulls", 0, cells.takeLastWhile { it == null }.size)
        assertEquals("Feb 1 is column 0", february2026.atDay(1), cells.first())
        assertEquals("Feb 28 is last cell", february2026.atDay(28), cells.last())
        assertDaysInOrder(february2026, cells)
    }

    @Test
    fun `Feb 2026 Monday-first pushes the Sunday start to 6 leading nulls`() {
        // First = Sunday(7); Monday-first(1): leading = (7 - 1 + 7) % 7 = 6.
        // 6 + 28 = 34 → padded to 35, 1 trailing null.
        val cells = amMonthDayCells(february2026, DayOfWeek.MONDAY)

        assertEquals("total cells", 35, cells.size)
        assertEquals("leading nulls", 6, cells.takeWhile { it == null }.size)
        assertEquals("first day sits at column 6", february2026.atDay(1), cells[6])
        assertEquals("trailing nulls", 1, cells.takeLastWhile { it == null }.size)
        assertDaysInOrder(february2026, cells)
    }

    // -------------------------------------------------------------------------
    // amMonthDayCells — month starting exactly on the first day of the week
    // -------------------------------------------------------------------------

    @Test
    fun `month starting on firstDayOfWeek has no leading nulls`() {
        // Jun 2026 starts on Monday(1); Monday-first: leading = (1 - 1 + 7) % 7 = 0.
        val cells = amMonthDayCells(june2026, DayOfWeek.MONDAY)

        assertEquals("no leading nulls", 0, cells.takeWhile { it == null }.size)
        assertEquals("first day is column 0", june2026.atDay(1), cells.first())
        // 30 days, 0 leading → padded to 35, 5 trailing nulls.
        assertEquals("total cells", 35, cells.size)
        assertEquals("trailing nulls", 5, cells.takeLastWhile { it == null }.size)
        assertDaysInOrder(june2026, cells)
    }

    // -------------------------------------------------------------------------
    // amMonthDayCells — trailing-null padding to a multiple of 7
    // -------------------------------------------------------------------------

    @Test
    fun `cell count is always a multiple of 7 across many months and both week starts`() {
        val starts = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.SATURDAY)
        for (monthIndex in 0 until 24) {
            val month = YearMonth.of(2024, 1).plusMonths(monthIndex.toLong())
            for (start in starts) {
                val cells = amMonthDayCells(month, start)
                assertTrue(
                    "count ${cells.size} for $month / $start must be a multiple of 7",
                    cells.size % 7 == 0,
                )
                assertDaysInOrder(month, cells)
            }
        }
    }

    // -------------------------------------------------------------------------
    // amOrderedWeekdays — rotation to the locale's first weekday
    // -------------------------------------------------------------------------

    @Test
    fun `orderedWeekdays for US Sunday-first begins on Sunday`() {
        assertEquals(
            listOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
            ),
            amOrderedWeekdays(DayOfWeek.SUNDAY),
        )
    }

    @Test
    fun `orderedWeekdays for FR Monday-first begins on Monday`() {
        assertEquals(
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
            ),
            amOrderedWeekdays(DayOfWeek.MONDAY),
        )
    }

    @Test
    fun `orderedWeekdays for Saturday-first wraps Sunday and Friday to the ends`() {
        assertEquals(
            listOf(
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
            amOrderedWeekdays(DayOfWeek.SATURDAY),
        )
    }

    @Test
    fun `orderedWeekdays always returns 7 distinct days starting with firstDayOfWeek`() {
        for (start in DayOfWeek.entries) {
            val ordered = amOrderedWeekdays(start)
            assertEquals("seven days", 7, ordered.size)
            assertEquals("starts on the first day of the week", start, ordered.first())
            assertEquals("all seven weekdays present", DayOfWeek.entries.toSet(), ordered.toSet())
        }
    }
}
