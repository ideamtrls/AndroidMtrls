package io.github.jmsdnns.androidmtrls.demo.scenes.calendar

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

/**
 * Value types, derived helpers, and sample data for the Calendar scene. Pure
 * data — no composables. All sample data is generated relative to
 * [LocalDate.now] so the demo always looks current; nothing is persisted, so it
 * resets each launch. Ports iOS `CalendarData.swift`.
 */

// ---------------------------------------------------------------------------
// EventData
// ---------------------------------------------------------------------------

/**
 * A single calendar event: a [title], a [date] (day + time), and an optional
 * [location]. [id] gives each event a stable identity so an edit writes back
 * over the matching event and a delete removes exactly one. Mirrors iOS
 * `EventData`.
 */
@Immutable
data class EventData(
    val title: String,
    val date: LocalDateTime,
    val location: String = "",
    val id: UUID = UUID.randomUUID(),
) {
    /** Localized short time, e.g. "9:00 AM" — rendered with monospaced digits. */
    val timeLabel: String
        get() = date.toLocalTime().format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()),
        )
}

// ---------------------------------------------------------------------------
// Date helpers + formatting
// ---------------------------------------------------------------------------

/** A concrete time on the day [dayOffset] days from today. Mirrors iOS `date(dayOffset:hour:minute:)`. */
private fun calendarDate(dayOffset: Int, hour: Int, minute: Int = 0): LocalDateTime =
    LocalDate.now().plusDays(dayOffset.toLong()).atTime(hour, minute)

/** "July 2026" — the Calendar landing view's page-header title. */
fun calendarMonthTitle(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

/** "July" — the full month name, a months-picker row title. */
fun calendarMonthName(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))

/**
 * The inline-events section heading: "Today" for the current day, else the wide
 * weekday with the abbreviated month and day (e.g. "Tuesday, Jul 7"). Mirrors
 * iOS `dayHeading(for:)`.
 */
fun calendarDayHeading(date: LocalDate): String =
    if (date == LocalDate.now()) {
        "Today"
    } else {
        date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }

/**
 * The years the months picker lists: three back through three forward of the
 * current year, inclusive (seven entries). Mirrors iOS `pickerYears`.
 */
fun calendarPickerYears(): List<Int> {
    val current = LocalDate.now().year
    return (current - 3..current + 3).toList()
}

// ---------------------------------------------------------------------------
// Sample data (relative to now)
// ---------------------------------------------------------------------------

/**
 * Nineteen events keeping the computing-history flavor: clusters on today,
 * tomorrow, and this week, plus a scattering across ±2 months so
 * month-scrolling shows dots. Copied verbatim from iOS `calendarSampleEvents()`.
 */
fun calendarSampleEvents(): List<EventData> = listOf(
    // Today
    EventData(title = "Stand-up", date = calendarDate(dayOffset = 0, hour = 9), location = "Lab 4"),
    EventData(title = "Code review", date = calendarDate(dayOffset = 0, hour = 10, minute = 30), location = "Conf B"),
    EventData(title = "Lunch with Grace", date = calendarDate(dayOffset = 0, hour = 12, minute = 30), location = "Cafeteria"),
    EventData(title = "Relay inspection", date = calendarDate(dayOffset = 0, hour = 15), location = "Machine room"),
    EventData(title = "Seminar prep", date = calendarDate(dayOffset = 0, hour = 19, minute = 30), location = "Home study"),
    // Tomorrow
    EventData(title = "Punch-card audit", date = calendarDate(dayOffset = 1, hour = 9, minute = 30), location = "Archive"),
    EventData(title = "Compiler working group", date = calendarDate(dayOffset = 1, hour = 14), location = "Conf A"),
    EventData(title = "Dinner with the countess", date = calendarDate(dayOffset = 1, hour = 18, minute = 30), location = "Analytical Society"),
    // This week
    EventData(title = "Difference engine demo", date = calendarDate(dayOffset = 2, hour = 11), location = "Exhibit hall"),
    EventData(title = "Tape backup window", date = calendarDate(dayOffset = 3, hour = 16), location = "Machine room"),
    EventData(title = "Chess against the engine", date = calendarDate(dayOffset = 3, hour = 17, minute = 30), location = "Rec room"),
    EventData(title = "Colloquium: stored programs", date = calendarDate(dayOffset = 5, hour = 13), location = "Lecture hall 2"),
    // Scattered back
    EventData(title = "Archive intake", date = calendarDate(dayOffset = -45, hour = 10), location = "Records annex"),
    EventData(title = "Patent review", date = calendarDate(dayOffset = -30, hour = 14, minute = 30), location = "Legal"),
    EventData(title = "Oscilloscope calibration", date = calendarDate(dayOffset = -12, hour = 9), location = "Lab 4"),
    // Scattered forward
    EventData(title = "Conference keynote", date = calendarDate(dayOffset = 10, hour = 9, minute = 30), location = "Convention hall"),
    EventData(title = "Lab open house", date = calendarDate(dayOffset = 21, hour = 15), location = "Lab 4"),
    EventData(title = "Grant deadline review", date = calendarDate(dayOffset = 40, hour = 11), location = "Office"),
    EventData(title = "Symposium dinner", date = calendarDate(dayOffset = 55, hour = 19), location = "Faculty club"),
)
