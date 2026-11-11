package com.ideamtrls.androidmtrls.demo

import android.os.Bundle
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ideamtrls.androidmtrls.AMToastItem
import com.ideamtrls.androidmtrls.amInstantSwitch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

/** The scene's root views: the month (the landing view) and the months, which the remote swaps. */
internal enum class CalendarView { Month, Months }

/**
 * Owns the Calendar scene: the events, the month on screen and the selected day, the root view
 * showing, the held day the peek shows, the event form and its draft, the delete confirmation and
 * the toasts. A plain class holding Compose state, remembered at the scene root and saved with
 * [Saver], so edits, the open form and its draft come back after recreation. Nothing runs in the
 * background, so there's nothing to cancel.
 */
@Stable
internal class CalendarModel(events: List<CalendarEvent> = calendarSampleEvents(LocalDate.now())) {
    /** Every event, repeats stored once as their series. */
    val events = mutableStateListOf<CalendarEvent>().apply { addAll(events) }

    var currentView by mutableStateOf(CalendarView.Month)

    /** The month the month view shows. */
    var displayedMonth by mutableStateOf(YearMonth.now())
        private set

    /** The day whose agenda the month view lists. */
    var selectedDay by mutableStateOf(LocalDate.now())
        private set

    /** The day pressed and held: its peek is up while it's set. */
    var peekDay by mutableStateOf<LocalDate?>(null)

    var showsEventSheet by mutableStateOf(false)
        private set

    /** The event form's working copy; it stays after the form closes, so the sheet slides away intact. */
    var draft by mutableStateOf<CalendarDraft?>(null)
        private set

    var confirmsDelete by mutableStateOf(false)
    var toast by mutableStateOf<AMToastItem?>(null)

    /** Whether the month view already shows this month with today selected, which greys Today. */
    val isViewingToday: Boolean
        get() = displayedMonth == YearMonth.now() && selectedDay == LocalDate.now()

    /** The stored title of the event the form edits, for the delete confirmation. */
    val draftStoredTitle: String
        get() {
            val draft = draft ?: return ""
            return events.firstOrNull { it.id == draft.id }?.title ?: draft.title.text.toString()
        }

    /** Selects [day], which lists its agenda. */
    fun selectDay(day: LocalDate) {
        selectedDay = day
    }

    /** Shows this month with today selected. */
    fun showToday() {
        displayedMonth = YearMonth.now()
        selectedDay = LocalDate.now()
    }

    /**
     * Shows [month] in the month view, its 1st selected, so the agenda follows the month picked:
     * all at once, in one frame.
     */
    fun pickMonth(month: YearMonth) {
        amInstantSwitch {
            displayedMonth = month
            selectedDay = month.atDay(1)
            currentView = CalendarView.Month
        }
    }

    /** Opens the form on a new event, 9:00 to 10:00 on [day]. */
    fun newEvent(day: LocalDate) {
        draft = CalendarDraft.new(day)
        showsEventSheet = true
    }

    /** Opens the form on the series an [occurrence] belongs to, with the series' own dates. */
    fun editEvent(occurrence: CalendarEvent) {
        val series = events.firstOrNull { it.id == occurrence.id } ?: occurrence
        draft = CalendarDraft.editing(series)
        showsEventSheet = true
    }

    fun closeEventSheet() {
        showsEventSheet = false
    }

    /**
     * Stores the draft: over the event it edits, or as a new one. An event saved onto days that
     * don't include the selected day takes the selection and the month to its first day, so it's in
     * view when the form closes.
     */
    fun save() {
        val draft = draft ?: return
        if (!draft.canSave) return
        val event = draft.saved
        val index = events.indexOfFirst { it.id == event.id }
        if (index >= 0) {
            events[index] = event
            toast = AMToastItem("Event updated")
        } else {
            events += event
            toast = AMToastItem("Event added")
        }
        if (!event.occursOn(selectedDay)) {
            selectedDay = event.firstDay
            displayedMonth = YearMonth.from(event.firstDay)
        }
        showsEventSheet = false
    }

    /** The form's Delete asks first. */
    fun confirmDelete() {
        confirmsDelete = true
    }

    /** Removes the event the form edits, every repeat with it, and closes the form. */
    fun delete() {
        val id = draft?.id ?: return
        events.removeAll { it.id == id }
        confirmsDelete = false
        showsEventSheet = false
        toast = AMToastItem("Event deleted")
    }

    /** The peek's Add event: closes the peek and opens the form on the held day. */
    fun addEvent(day: LocalDate) {
        peekDay = null
        newEvent(day)
    }

    /** The peek's Open day: shows the held day's month with the day selected, and closes the peek. */
    fun openDay(day: LocalDate) {
        displayedMonth = YearMonth.from(day)
        selectedDay = day
        peekDay = null
    }

    companion object {
        /** Saves the events, the view and the day, the open peek, form, draft and confirmation. */
        val Saver: Saver<CalendarModel, Any> = mapSaver(
            save = { model ->
                mapOf(
                    "events" to ArrayList(model.events.map { it.toBundle() }),
                    "view" to model.currentView.name,
                    "month" to model.displayedMonth.toString(),
                    "day" to model.selectedDay.toString(),
                    "peek" to model.peekDay?.toString(),
                    "sheet" to model.showsEventSheet,
                    "draft" to model.draft?.toBundle(),
                    "confirmsDelete" to model.confirmsDelete,
                )
            },
            restore = { saved ->
                val events = (saved["events"] as List<*>).filterIsInstance<Bundle>().map(CalendarEvent::fromBundle)
                CalendarModel(events).apply {
                    currentView = CalendarView.valueOf(saved["view"] as String)
                    displayedMonth = YearMonth.parse(saved["month"] as String)
                    selectedDay = LocalDate.parse(saved["day"] as String)
                    peekDay = (saved["peek"] as String?)?.let(LocalDate::parse)
                    showsEventSheet = saved["sheet"] as Boolean
                    draft = (saved["draft"] as Bundle?)?.let(CalendarDraft::fromBundle)
                    confirmsDelete = saved["confirmsDelete"] as Boolean
                }
            },
        )
    }
}

/**
 * The times of day a timed event starts and ends, kept apart from its days so All-day can hide them
 * and bring them back. [endDayOffset] is 1 for an event that ends exactly at midnight (10 PM to
 * midnight ends the day after the last day it touches), else 0.
 */
internal data class CalendarEventTimes(val start: LocalTime, val end: LocalTime, val endDayOffset: Long) {
    /** The start time on the event's first day. */
    fun start(onFirstDay: LocalDate): LocalDateTime = onFirstDay.atTime(start)

    /** The end time on the event's last day, or the day after it for an event that ends at midnight. */
    fun end(onLastDay: LocalDate): LocalDateTime = onLastDay.plusDays(endDayOffset).atTime(end)

    companion object {
        /** 9:00 to 10:00, a new event's times. */
        val Default = CalendarEventTimes(LocalTime.of(9, 0), LocalTime.of(10, 0), 0)

        /** The length an event gets when its kept times don't fit its days. */
        val DefaultLength: Duration = Duration.ofHours(1)

        /** The times [start] to [end] run at, to the minute. */
        fun of(start: LocalDateTime, end: LocalDateTime): CalendarEventTimes {
            val lastDay = maxOf(start.toLocalDate(), end.minusNanos(1).toLocalDate())
            return CalendarEventTimes(
                start = start.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
                end = end.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
                endDayOffset = maxOf(0L, ChronoUnit.DAYS.between(lastDay, end.toLocalDate())),
            )
        }
    }
}

/**
 * The event form's working copy. The form binds to these properties, and their setters keep the
 * form's rules:
 * - Moving Starts carries Ends along, keeping the length: exactly while timed, in whole days while
 *   all-day.
 * - Ends never goes before Starts.
 * - All-day on keeps the times of day and shows the days the event touches; All-day off puts the
 *   kept times back on those days (the event's own, or 9:00 to 10:00 for a new event or an all-day
 *   one that never had times), an hour long if that would end before it starts.
 * - The color follows the calendar unless the event has its own.
 *
 * While all-day, [start] and [end] hold their days' midnights; [saved] puts the kept times back, so
 * an all-day event remembers them for next time.
 */
@Stable
internal class CalendarDraft private constructor(
    /** The event's id: a stored event's own, or a fresh one for a new event. */
    val id: String,
    val isNew: Boolean,
    title: String,
    location: String,
    notes: String,
    start: LocalDateTime,
    end: LocalDateTime,
    isAllDay: Boolean,
    calendar: CalendarEventCalendar,
    private val colorOverride: Color?,
    repeatRule: CalendarEventRepeat,
    alert: CalendarEventAlert,
    private var times: CalendarEventTimes,
) {
    val title = TextFieldState(title)
    val location = TextFieldState(location)
    val notes = TextFieldState(notes)
    var calendar by mutableStateOf(calendar)
    var repeatRule by mutableStateOf(repeatRule)
    var alert by mutableStateOf(alert)

    private var startValue by mutableStateOf(start)
    private var endValue by mutableStateOf(end)
    private var allDayValue by mutableStateOf(isAllDay)

    /** Moving Starts carries Ends along, keeping the length: exactly while timed, in whole days while all-day. */
    var start: LocalDateTime
        get() = startValue
        set(value) {
            if (allDayValue) {
                val day = value.toLocalDate().atStartOfDay()
                val days = ChronoUnit.DAYS.between(startValue, day)
                startValue = day
                endValue = endValue.plusDays(days)
            } else {
                val length = Duration.between(startValue, endValue)
                startValue = value
                endValue = value.plus(length)
            }
        }

    /** Never before Starts. */
    var end: LocalDateTime
        get() = endValue
        set(value) {
            val end = if (allDayValue) value.toLocalDate().atStartOfDay() else value
            endValue = maxOf(end, startValue)
        }

    /**
     * On: keeps the times of day and shows the days the event touches. Off: puts the kept times back
     * on those days, an hour long if that would end before it starts.
     */
    var isAllDay: Boolean
        get() = allDayValue
        set(value) {
            if (value == allDayValue) return
            if (value) {
                times = CalendarEventTimes.of(startValue, endValue)
                val firstDay = startValue.toLocalDate()
                val lastDay = maxOf(firstDay, endValue.minusNanos(1).toLocalDate())
                startValue = firstDay.atStartOfDay()
                endValue = lastDay.atStartOfDay()
            } else {
                val start = times.start(startValue.toLocalDate())
                val end = times.end(endValue.toLocalDate())
                startValue = start
                endValue = if (end > start) end else start.plus(CalendarEventTimes.DefaultLength)
            }
            allDayValue = value
        }

    /** Save waits for a title. */
    val canSave: Boolean
        get() = title.text.isNotBlank()

    /**
     * The event Save stores: the title and location trimmed, an all-day event carrying its kept
     * times (a day later when its end would land on the midnight opening the last day shown, so it
     * still covers that day), and never an end before the start.
     */
    val saved: CalendarEvent
        get() {
            var start = startValue
            var end = endValue
            if (allDayValue) {
                start = times.start(startValue.toLocalDate())
                end = times.end(endValue.toLocalDate())
                if (end <= endValue) end = end.plusDays(1)
            }
            return CalendarEvent(
                id = id,
                title = title.text.toString().trim(),
                start = start,
                end = maxOf(end, start),
                calendar = calendar,
                isAllDay = allDayValue,
                location = location.text.toString().trim(),
                colorOverride = colorOverride,
                notes = notes.text.toString(),
                repeatRule = repeatRule,
                alert = alert,
            )
        }

    /** What the scene saves of the draft, read back by [fromBundle]. */
    fun toBundle(): Bundle = Bundle().apply {
        putString("id", id)
        putBoolean("isNew", isNew)
        putString("title", title.text.toString())
        putString("location", location.text.toString())
        putString("notes", notes.text.toString())
        putString("start", startValue.toString())
        putString("end", endValue.toString())
        putBoolean("allDay", allDayValue)
        putString("calendar", calendar.name)
        putString("repeat", repeatRule.name)
        putString("alert", alert.name)
        putString("timesStart", times.start.toString())
        putString("timesEnd", times.end.toString())
        putLong("timesOffset", times.endDayOffset)
        colorOverride?.let { putInt("color", it.toArgb()) }
    }

    companion object {
        /** A 9:00 to 10:00 draft on the Home calendar on [day]. */
        fun new(day: LocalDate): CalendarDraft {
            val times = CalendarEventTimes.Default
            return CalendarDraft(
                id = UUID.randomUUID().toString(),
                isNew = true,
                title = "",
                location = "",
                notes = "",
                start = times.start(day),
                end = times.end(day),
                isAllDay = false,
                calendar = CalendarEventCalendar.Home,
                colorOverride = null,
                repeatRule = CalendarEventRepeat.Never,
                alert = CalendarEventAlert.None,
                times = times,
            )
        }

        /**
         * A draft of a stored [event]. An all-day event shows its days; it keeps the times it was
         * saved with, and one that runs midnight to midnight (the samples) gets 9:00 to 10:00.
         */
        fun editing(event: CalendarEvent): CalendarDraft {
            var times = CalendarEventTimes.of(event.start, event.end)
            var start = event.start
            var end = event.end
            if (event.isAllDay) {
                val keepsTimes = event.start != event.firstDay.atStartOfDay() ||
                    event.end != event.end.toLocalDate().atStartOfDay()
                if (!keepsTimes) times = CalendarEventTimes.Default
                start = event.firstDay.atStartOfDay()
                end = event.lastDay.atStartOfDay()
            }
            return CalendarDraft(
                id = event.id,
                isNew = false,
                title = event.title,
                location = event.location,
                notes = event.notes,
                start = start,
                end = end,
                isAllDay = event.isAllDay,
                calendar = event.calendar,
                colorOverride = event.colorOverride,
                repeatRule = event.repeatRule,
                alert = event.alert,
                times = times,
            )
        }

        /** A draft as [toBundle] saved it. */
        fun fromBundle(bundle: Bundle): CalendarDraft = CalendarDraft(
            id = bundle.getString("id").orEmpty(),
            isNew = bundle.getBoolean("isNew"),
            title = bundle.getString("title").orEmpty(),
            location = bundle.getString("location").orEmpty(),
            notes = bundle.getString("notes").orEmpty(),
            start = LocalDateTime.parse(bundle.getString("start")),
            end = LocalDateTime.parse(bundle.getString("end")),
            isAllDay = bundle.getBoolean("allDay"),
            calendar = CalendarEventCalendar.valueOf(bundle.getString("calendar").orEmpty()),
            colorOverride = if (bundle.containsKey("color")) Color(bundle.getInt("color")) else null,
            repeatRule = CalendarEventRepeat.valueOf(bundle.getString("repeat").orEmpty()),
            alert = CalendarEventAlert.valueOf(bundle.getString("alert").orEmpty()),
            times = CalendarEventTimes(
                start = LocalTime.parse(bundle.getString("timesStart")),
                end = LocalTime.parse(bundle.getString("timesEnd")),
                endDayOffset = bundle.getLong("timesOffset"),
            ),
        )
    }
}
