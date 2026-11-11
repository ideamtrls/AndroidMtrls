package com.ideamtrls.androidmtrls.demo

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTheme
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

// The Calendar scene's data: its calendars, events and their repeats, a day's agenda, the sample
// events, the month pictures, and how dates read. Everything is relative to today, so the scene
// always looks current; nothing persists beyond the scene's saved state.

// Calendars

/** The calendars an event can belong to, each with the color its events' dots take. */
internal enum class CalendarEventCalendar(val title: String) {
    Home("Home"),
    Work("Work"),
    Family("Family");

    /** The calendar's color: success green, the accent, and purple. */
    val color: Color
        @Composable @ReadOnlyComposable get() = when (this) {
            Home -> AMSemantic.Success.color
            Work -> AMSemantic.Info.color
            Family -> if (AMTheme.isDark) CalendarPurpleDark else CalendarPurpleLight
        }
}

/** Family's purple, a calendar color like a user would pick: brighter on the dark page. */
private val CalendarPurpleLight = Color(0xFFAF52DE)
private val CalendarPurpleDark = Color(0xFFBF5AF2)

/** A color picked for one event, the sample anniversary's. */
private val CalendarPink = Color(0xFFFF2D55)

/** How often an event repeats. */
internal enum class CalendarEventRepeat(val title: String) {
    Never("Never"),
    Daily("Every day"),
    Weekly("Every week"),
    Monthly("Every month"),
}

/** When an event alerts, relative to its start. */
internal enum class CalendarEventAlert(val title: String) {
    None("None"),
    AtTime("At time of event"),
    FiveMinutes("5 minutes before"),
    FifteenMinutes("15 minutes before"),
    OneHour("1 hour before"),
}

// Events

/**
 * One event, running from [start] to [end] and covering every day from [start]'s day through the
 * day it ends, or the day before when it ends exactly at midnight. An all-day event keeps its times
 * of day without showing them, so turning All-day off can bring them back. [repeatRule] makes it
 * recur; [occurrence] finds the repeat on a given day.
 */
@Immutable
internal data class CalendarEvent(
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val calendar: CalendarEventCalendar,
    val id: String = UUID.randomUUID().toString(),
    val isAllDay: Boolean = false,
    val location: String = "",
    /** A color for this event alone; null uses its calendar's. */
    val colorOverride: Color? = null,
    val notes: String = "",
    val repeatRule: CalendarEventRepeat = CalendarEventRepeat.Never,
    val alert: CalendarEventAlert = CalendarEventAlert.None,
) {
    /** The first day the event touches. */
    val firstDay: LocalDate get() = start.toLocalDate()

    /** The last day the event touches: an end exactly at midnight doesn't reach the next day. */
    val lastDay: LocalDate get() = maxOf(firstDay, end.minusNanos(1).toLocalDate())

    /** Whether the event, or one of its repeats, falls on [day]. */
    fun occursOn(day: LocalDate): Boolean = occurrence(day) != null

    /**
     * The occurrence that falls on [day]: this event, or for a repeating one a copy with the same
     * [id] moved to the repeat that touches [day]. A repeat is the whole event moved by whole days,
     * weeks or months from its own start, so times of day hold. Repeats never end and never come
     * before the first occurrence; a monthly repeat skips a month without its day number (the 31st
     * in June). When occurrences overlap, the latest to start wins.
     */
    fun occurrence(day: LocalDate): CalendarEvent? {
        if (day < firstDay) return null
        val latest = when (repeatRule) {
            CalendarEventRepeat.Never -> 0L
            CalendarEventRepeat.Daily -> ChronoUnit.DAYS.between(firstDay, day)
            CalendarEventRepeat.Weekly -> ChronoUnit.DAYS.between(firstDay, day) / 7
            CalendarEventRepeat.Monthly -> ChronoUnit.MONTHS.between(firstDay, day)
        }
        for (period in latest downTo 0L) {
            val occurrence = shifted(period) ?: continue
            // Earlier periods end earlier still.
            if (occurrence.lastDay < day) return null
            if (occurrence.firstDay <= day) return occurrence
        }
        return null
    }

    /** This event moved [periods] repeats later, or null for a month without its day number. */
    private fun shifted(periods: Long): CalendarEvent? {
        if (periods <= 0L) return this
        val newStart = when (repeatRule) {
            CalendarEventRepeat.Never -> start
            CalendarEventRepeat.Daily -> start.plusDays(periods)
            CalendarEventRepeat.Weekly -> start.plusWeeks(periods)
            CalendarEventRepeat.Monthly -> start.plusMonths(periods)
        }
        // A month too short is clamped to its last day; a repeat on the 31st skips it instead.
        if (newStart.dayOfMonth != start.dayOfMonth && repeatRule == CalendarEventRepeat.Monthly) return null
        // The end keeps its days after the start and its time of day.
        val endDays = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate())
        val newEnd = newStart.toLocalDate().plusDays(endDays).atTime(end.toLocalTime())
        return copy(start = newStart, end = newEnd)
    }

    /** What the scene saves of the event, read back by [fromBundle]. */
    fun toBundle(): Bundle = Bundle().apply {
        putString("id", id)
        putString("title", title)
        putString("start", start.toString())
        putString("end", end.toString())
        putBoolean("allDay", isAllDay)
        putString("location", location)
        putString("calendar", calendar.name)
        putString("notes", notes)
        putString("repeat", repeatRule.name)
        putString("alert", alert.name)
        colorOverride?.let { putInt("color", it.toArgb()) }
    }

    companion object {
        /** An event as [toBundle] saved it. */
        fun fromBundle(bundle: Bundle): CalendarEvent = CalendarEvent(
            id = bundle.getString("id").orEmpty(),
            title = bundle.getString("title").orEmpty(),
            start = LocalDateTime.parse(bundle.getString("start")),
            end = LocalDateTime.parse(bundle.getString("end")),
            isAllDay = bundle.getBoolean("allDay"),
            location = bundle.getString("location").orEmpty(),
            calendar = CalendarEventCalendar.valueOf(bundle.getString("calendar").orEmpty()),
            colorOverride = if (bundle.containsKey("color")) Color(bundle.getInt("color")) else null,
            notes = bundle.getString("notes").orEmpty(),
            repeatRule = CalendarEventRepeat.valueOf(bundle.getString("repeat").orEmpty()),
            alert = CalendarEventAlert.valueOf(bundle.getString("alert").orEmpty()),
        )
    }
}

/** The event's color: its own, or its calendar's. */
internal val CalendarEvent.tint: Color
    @Composable @ReadOnlyComposable get() = colorOverride ?: calendar.color

// Agenda

/** One entry in a day's agenda: an event, the current time, or a count of events left out. */
@Immutable
internal sealed interface CalendarAgendaEntry {
    /** Identifies the entry within its agenda. */
    val key: String

    @Immutable
    data class Event(val event: CalendarEvent) : CalendarAgendaEntry {
        override val key: String get() = "event-${event.id}"
    }

    @Immutable
    data class Now(val time: LocalDateTime) : CalendarAgendaEntry {
        override val key: String get() = "now"
    }

    @Immutable
    data class More(val count: Int) : CalendarAgendaEntry {
        override val key: String get() = "more"
    }
}

/** Start order, then end, then title, so events starting together list stably. */
private val CalendarStartsFirst = compareBy<CalendarEvent>({ it.start }, { it.end }, { it.title })

/**
 * [day]'s agenda: its all-day events first, then its timed events by start. On [now]'s day, when
 * it has a timed event, the now entry sits after the events that have started and before the
 * rest. It never holds a more entry.
 */
internal fun calendarAgenda(day: LocalDate, events: List<CalendarEvent>, now: LocalDateTime): List<CalendarAgendaEntry> {
    val onDay = events.mapNotNull { it.occurrence(day) }
    val allDay = onDay.filter { it.isAllDay }.sortedWith(CalendarStartsFirst)
    val timed = onDay.filterNot { it.isAllDay }.sortedWith(CalendarStartsFirst)
    val entries = allDay.map { CalendarAgendaEntry.Event(it) }.toMutableList<CalendarAgendaEntry>()
    if (day != now.toLocalDate() || timed.isEmpty()) {
        return entries + timed.map { CalendarAgendaEntry.Event(it) }
    }
    val started = timed.takeWhile { it.start <= now }
    entries += started.map { CalendarAgendaEntry.Event(it) }
    entries += CalendarAgendaEntry.Now(now)
    entries += timed.drop(started.size).map { CalendarAgendaEntry.Event(it) }
    return entries
}

/**
 * [day]'s agenda cut just before its event number [limit] + 1, closed by a more entry counting the
 * events left out. The now entry stays when it comes before the cut. A day with [limit] events or
 * fewer is its whole agenda.
 */
internal fun calendarCompactAgenda(
    day: LocalDate,
    events: List<CalendarEvent>,
    now: LocalDateTime,
    limit: Int,
): List<CalendarAgendaEntry> {
    val entries = calendarAgenda(day, events, now)
    val eventIndices = entries.indices.filter { entries[it] is CalendarAgendaEntry.Event }
    if (eventIndices.size <= limit) return entries
    return entries.take(eventIndices[limit]) + CalendarAgendaEntry.More(eventIndices.size - limit)
}

// Dates

/** The calendar math the scene shares. */
internal object CalendarDates {
    /** The years the months view lists: three back through three forward of [today]'s. */
    fun pickerYears(today: LocalDate): List<Int> = (today.year - 3..today.year + 3).toList()

    /** Whether [day] falls on a Saturday or Sunday. */
    fun isWeekend(day: LocalDate): Boolean =
        day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY
}

/**
 * How the scene's dates and times read, in the phone's locale and on its 12- or 24-hour clock:
 * "November 2026", "Tuesday, Nov 10", "9:41 AM", "9:00 – 9:45 AM". Built once per locale and clock
 * setting by [rememberCalendarFormats].
 */
@Stable
internal class CalendarFormats(private val context: Context, private val locale: Locale, is24Hour: Boolean) {
    private val monthTitle = pattern("MMMMy")
    private val dayHeading = pattern("EEEEMMMd")
    private val time = pattern(if (is24Hour) "Hm" else "hm")

    private fun pattern(skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

    /** "November 2026", the month view's title. */
    fun monthTitle(month: YearMonth): String = monthTitle.format(month.atDay(1))

    /** "November", a month in the months view. */
    fun monthName(month: YearMonth): String = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)

    /** "Today" on [today], else "Tuesday, Nov 10": the agenda's and the day peek's heading. */
    fun dayHeading(day: LocalDate, today: LocalDate): String = if (day == today) "Today" else dayHeading.format(day)

    /** "9:41 AM". */
    fun time(time: LocalDateTime): String = this.time.format(time)

    /**
     * The event's times, whatever its span ("9:00 – 9:45 AM", Android's own range format), or "All
     * day". An event ending on a later day, midnight included, joins its two times the range
     * format's way ("9:30 PM – 2:00 AM"), since the range format would add the dates.
     */
    fun timeLabel(event: CalendarEvent): String {
        if (event.isAllDay) return "All day"
        val end = maxOf(event.start, event.end)
        if (event.start.toLocalDate() != end.toLocalDate()) return "${time(event.start)} – ${time(end)}"
        return DateUtils.formatDateRange(context, millis(event.start), millis(end), DateUtils.FORMAT_SHOW_TIME)
    }

    /** The agenda's detail line: "9:00 – 9:45 AM · Room 4B", or the times alone. */
    fun agendaDetail(event: CalendarEvent): String =
        if (event.location.isEmpty()) timeLabel(event) else "${timeLabel(event)} · ${event.location}"

    private fun millis(time: LocalDateTime): Long = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/** The scene's [CalendarFormats] for the current locale and clock setting. */
@Composable
internal fun rememberCalendarFormats(): CalendarFormats {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(context, locale, is24Hour) { CalendarFormats(context, locale, is24Hour) }
}

/**
 * The time now, refreshed at the start of each minute, so the now entry and "Today" keep up with
 * the clock, across midnight too. It reads through [LaunchOptions.clock], so a `demoTime` launch
 * pins the time of day (today's date still follows the clock).
 */
@Composable
internal fun rememberCalendarNow(): LocalDateTime {
    val now by produceState(LaunchOptions.clock()) {
        while (true) {
            val current = LocalDateTime.now()
            value = LaunchOptions.clock(current)
            val nextMinute = current.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
            delay(Duration.between(current, nextMinute).toMillis().coerceAtLeast(1L))
        }
    }
    return now
}

// Month pictures

/**
 * One picture of each month's weather, January first, from Wikimedia Commons (each file's page
 * there credits its author and license).
 */
private val CalendarMonthPictures = listOf(
    "Beech forest Mátra in winter.jpg",
    "Hoar frost on bare trees in winter in Tuntorp.jpg",
    "Sancy Early Spring.jpg",
    "Ready To Blossom (206649103).jpeg",
    "Dandelion (249521023).jpeg",
    "Passo Pordoi-bops 01.jpg",
    "Sunny Beach - panoramio (13).jpg",
    "Landscape with stormy clouds and a pirogue on the Mekong at golden hour in Si Phan Don.jpg",
    "Beilstein - Burg Hohenbeilstein und Unteres Schloss - Ansicht vom Birkenweg mit Abendsonne.jpg",
    "Forest Heritage National Scenic Byway - Colorful Autumn Leaves at Looking Glass Falls - NARA - 7718474.jpg",
    "Morning Fog (253075957).jpeg",
    "Karlštejn in winter.jpg",
)

/** The picture of [month]'s weather, by the month alone, so every year repeats them. */
internal fun calendarMonthArt(month: YearMonth): String = commonsPhoto(CalendarMonthPictures[month.monthValue - 1])

// Sample events

/**
 * Twenty-eight events across the Home, Work and Family calendars: a full [today], a plan for each
 * of the next six days, and a scattering across two months either side so paging months shows
 * dots. Work happens on weekdays only and weekends get weekend plans, so the calendar reads true
 * whichever day today is.
 */
internal fun calendarSampleEvents(today: LocalDate): List<CalendarEvent> {
    val samples = CalendarSamples(today)
    return samples.today() + samples.comingWeek() + samples.scattered()
}

/** The sample events, laid out from [today]. */
private class CalendarSamples(private val today: LocalDate) {

    private fun day(offset: Int): LocalDate = today.plusDays(offset.toLong())

    private fun isWeekend(offset: Int): Boolean = CalendarDates.isWeekend(day(offset))

    /** [offset], moved to the nearest weekday when it falls on a weekend: Saturday to Friday, Sunday to Monday. */
    private fun workday(offset: Int): Int {
        for (distance in 0..3) {
            for (candidate in listOf(offset - distance, offset + distance)) {
                if (!isWeekend(candidate)) return candidate
            }
        }
        return offset
    }

    private fun timed(
        title: String,
        offset: Int,
        hour: Int,
        minute: Int = 0,
        minutes: Long,
        location: String = "",
        calendar: CalendarEventCalendar,
        colorOverride: Color? = null,
        notes: String = "",
        alert: CalendarEventAlert = CalendarEventAlert.None,
    ): CalendarEvent {
        val start = day(offset).atTime(LocalTime.of(hour, minute))
        return CalendarEvent(
            title = title,
            start = start,
            end = start.plusMinutes(minutes),
            calendar = calendar,
            location = location,
            colorOverride = colorOverride,
            notes = notes,
            alert = alert,
        )
    }

    private fun allDay(title: String, offset: Int, calendar: CalendarEventCalendar, notes: String = ""): CalendarEvent {
        val midnight = day(offset).atStartOfDay()
        return CalendarEvent(title = title, start = midnight, end = midnight, calendar = calendar, isAllDay = true, notes = notes)
    }

    /** Today, morning to night, so the now entry lands between events at most hours. */
    fun today(): List<CalendarEvent> {
        val birthday = allDay(
            "Dad's birthday", 0, CalendarEventCalendar.Family,
            notes = "Call before 10 — he's out on the course after that.",
        )
        val dinner = timed(
            "Birthday dinner for Dad", 0, 19, minutes = 90, location = "Rosa's Trattoria",
            calendar = CalendarEventCalendar.Family, notes = "Table for six. The cake is picked up at 6:30.",
            alert = CalendarEventAlert.OneHour,
        )
        if (!isWeekend(0)) {
            return listOf(
                birthday,
                timed("Morning run", 0, 7, minutes = 45, location = "Riverside loop", calendar = CalendarEventCalendar.Home),
                timed(
                    "Stand-up", 0, 9, 30, minutes = 30, location = "Video call", calendar = CalendarEventCalendar.Work,
                    alert = CalendarEventAlert.FiveMinutes,
                ),
                timed(
                    "Design review", 0, 11, minutes = 60, location = "Room 4B", calendar = CalendarEventCalendar.Work,
                    notes = "Walk through the new sign-up flow. Bring the onboarding numbers.",
                    alert = CalendarEventAlert.FifteenMinutes,
                ),
                timed("Lunch with Priya", 0, 12, 30, minutes = 60, location = "Juniper Café", calendar = CalendarEventCalendar.Home),
                timed(
                    "Pick up Leo from soccer", 0, 17, 15, minutes = 30, location = "Riverside Fields",
                    calendar = CalendarEventCalendar.Family,
                ),
                dinner,
                timed(
                    "Call with the Tokyo team", 0, 21, 30, minutes = 30, location = "Video call",
                    calendar = CalendarEventCalendar.Work,
                ),
            )
        }
        return listOf(
            birthday,
            timed("Morning run", 0, 8, minutes = 45, location = "Riverside loop", calendar = CalendarEventCalendar.Home),
            timed(
                "Farmers market", 0, 9, 30, minutes = 60, location = "Market Square", calendar = CalendarEventCalendar.Home,
                notes = "Peaches, sourdough, and flowers for Dad.",
            ),
            timed("Brunch with Priya", 0, 11, 30, minutes = 60, location = "Juniper Café", calendar = CalendarEventCalendar.Home),
            timed(
                "Leo's soccer game", 0, 14, minutes = 60, location = "Riverside Fields", calendar = CalendarEventCalendar.Family,
                alert = CalendarEventAlert.FifteenMinutes,
            ),
            timed("Bike ride with Leo", 0, 16, minutes = 60, location = "Bayshore Trail", calendar = CalendarEventCalendar.Family),
            dinner,
            timed("Movie night", 0, 21, 30, minutes = 90, calendar = CalendarEventCalendar.Home),
        )
    }

    /**
     * The next six days, one plan a day: workday plans on weekdays and weekend plans on Saturdays
     * and Sundays, each list taken in order. Six days hold at most five weekdays and two weekend
     * days, so neither list runs out.
     */
    fun comingWeek(): List<CalendarEvent> {
        val workdayPlans = ArrayDeque<(Int) -> List<CalendarEvent>>(
            listOf(
                { day ->
                    listOf(
                        timed(
                            "Roadmap planning", day, 13, minutes = 90, location = "Room 2A",
                            calendar = CalendarEventCalendar.Work, alert = CalendarEventAlert.FifteenMinutes,
                        ),
                        timed(
                            "Dinner with Hannah and Theo", day, 18, 30, minutes = 90, location = "Lark & Vine",
                            calendar = CalendarEventCalendar.Home,
                        ),
                    )
                },
                { day ->
                    listOf(
                        timed(
                            "Customer interview", day, 10, minutes = 45, location = "Video call",
                            calendar = CalendarEventCalendar.Work, notes = "Ask how they export reports today.",
                            alert = CalendarEventAlert.FifteenMinutes,
                        ),
                        timed(
                            "Parent–teacher conference", day, 16, minutes = 30, location = "Lincoln Elementary",
                            calendar = CalendarEventCalendar.Family,
                        ),
                    )
                },
                { day ->
                    listOf(
                        timed(
                            "Car service", day, 8, minutes = 60, location = "Bayside Auto", calendar = CalendarEventCalendar.Home,
                            notes = "Ask about the rattle in the rear door.", alert = CalendarEventAlert.OneHour,
                        ),
                        timed("Sprint demo", day, 15, minutes = 60, location = "Room 4B", calendar = CalendarEventCalendar.Work),
                    )
                },
                { day ->
                    listOf(
                        timed("Team lunch", day, 12, minutes = 60, location = "Noodle Bar", calendar = CalendarEventCalendar.Work),
                        timed(
                            "Leo's piano recital", day, 18, 30, minutes = 60, location = "Community Music Center",
                            calendar = CalendarEventCalendar.Family,
                        ),
                    )
                },
                { day ->
                    listOf(
                        timed(
                            "Dentist", day, 8, 30, minutes = 45, location = "Elm Street Dental",
                            calendar = CalendarEventCalendar.Home, alert = CalendarEventAlert.OneHour,
                        ),
                        timed(
                            "Book club", day, 19, 30, minutes = 90, location = "Maya's place",
                            calendar = CalendarEventCalendar.Home, notes = "Finish the last three chapters.",
                        ),
                    )
                },
            ),
        )
        val weekendPlans = ArrayDeque<(Int) -> List<CalendarEvent>>(
            listOf(
                { day ->
                    listOf(
                        allDay(
                            "Day trip to Half Moon Bay", day, CalendarEventCalendar.Home,
                            notes = "Leave by 8. Pack lunch and jackets.",
                        ),
                    )
                },
                { day ->
                    listOf(
                        timed(
                            "Leo's swim lesson", day, 9, minutes = 45, location = "Community Pool",
                            calendar = CalendarEventCalendar.Family,
                        ),
                        timed(
                            "Barbecue at Sam and Ji-woo's", day, 16, minutes = 90, location = "Sam and Ji-woo's",
                            calendar = CalendarEventCalendar.Home,
                        ),
                    )
                },
            ),
        )
        return (1..6).flatMap { day ->
            val plan = if (isWeekend(day)) weekendPlans.removeFirstOrNull() else workdayPlans.removeFirstOrNull()
            plan?.invoke(day).orEmpty()
        }
    }

    /** A few events across the months either side, work ones moved off weekends. */
    fun scattered(): List<CalendarEvent> = listOf(
        timed(
            "Annual checkup", workday(-45), 10, minutes = 45, location = "Dr. Patel's office",
            calendar = CalendarEventCalendar.Home,
        ),
        timed(
            "Quarterly review", workday(-30), 14, 30, minutes = 60, location = "Board room",
            calendar = CalendarEventCalendar.Work,
        ),
        allDay("Emma's birthday", -20, CalendarEventCalendar.Family),
        timed("Flu shot", -12, 9, minutes = 30, location = "Corner Pharmacy", calendar = CalendarEventCalendar.Home),
        timed(
            "Conference keynote", workday(10), 9, 30, minutes = 60, location = "Convention Center, Hall B",
            calendar = CalendarEventCalendar.Work,
        ),
        allDay("Grandma's birthday", 14, CalendarEventCalendar.Family),
        timed(
            "School open house", workday(21), 15, minutes = 60, location = "Lincoln Elementary",
            calendar = CalendarEventCalendar.Family,
        ),
        timed(
            "Performance reviews", workday(40), 11, minutes = 60, location = "Room 4B",
            calendar = CalendarEventCalendar.Work,
        ),
        timed(
            "Anniversary dinner", 55, 19, minutes = 90, location = "Lark & Vine", calendar = CalendarEventCalendar.Home,
            colorOverride = CalendarPink,
        ),
    )
}
