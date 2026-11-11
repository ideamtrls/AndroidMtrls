package com.ideamtrls.androidmtrls

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The month grid's layout: blanks before the 1st back to the week's start, whole weeks after. */
class CalendarMonthCellsTest {

    @Test
    fun monthStartingOnTheWeeksFirstDayHasNoLeadingBlanks() {
        // November 1, 2026 is a Sunday.
        val cells = calendarMonthCells(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 1), cells.first())
        assertEquals(35, cells.size)
        assertEquals(LocalDate.of(2026, 11, 30), cells[29])
        cells.drop(30).forEach(::assertNull)
    }

    @Test
    fun leadingBlanksFollowTheWeeksFirstDay() {
        // With Monday first, Sunday the 1st sits in the last column, after six blanks.
        val cells = calendarMonthCells(YearMonth.of(2026, 11), DayOfWeek.MONDAY)
        cells.take(6).forEach(::assertNull)
        assertEquals(LocalDate.of(2026, 11, 1), cells[6])
        assertEquals(42, cells.size)
    }

    @Test
    fun aFebruaryStartingOnTheWeeksFirstDayFillsFourWeeksExactly() {
        // February 1, 2026 is a Sunday, and 2026 isn't a leap year.
        val cells = calendarMonthCells(YearMonth.of(2026, 2), DayOfWeek.SUNDAY)
        assertEquals(28, cells.size)
        assertEquals(LocalDate.of(2026, 2, 28), cells.last())
    }

    @Test
    fun everyLayoutIsWholeWeeksHoldingEachDayOnce() {
        for (first in DayOfWeek.entries) {
            for (monthOfYear in 1..12) {
                val month = YearMonth.of(2028, monthOfYear)
                val cells = calendarMonthCells(month, first)
                assertEquals(0, cells.size % 7)
                assertEquals((1..month.lengthOfMonth()).map(month::atDay), cells.filterNotNull())
                val column = cells.indexOf(month.atDay(1))
                assertEquals(first.plus(column.toLong()), month.atDay(1).dayOfWeek)
            }
        }
    }
}
