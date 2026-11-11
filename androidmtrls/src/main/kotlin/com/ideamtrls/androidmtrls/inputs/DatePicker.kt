package com.ideamtrls.androidmtrls

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Which parts of a moment an [AMDatePicker] shows and picks. */
public enum class AMDatePickerComponents {
    /** The day alone: a date chip, opening the calendar. The time of day is kept as it is. */
    Date,

    /** The time of day alone: a time chip, opening the clock dial. The day is kept as it is. */
    Time,

    /**
     * The day and the time: a date chip opening the calendar and a time chip opening the clock dial,
     * each with a row for the other part under it.
     */
    DateAndTime,
}

/** What the date picker's spotlight shows: the calendar or the clock dial. */
private enum class DatePickerSpotlightPart { Calendar, Dial }

/** The heading of the row under the calendar that switches to the dial. */
private const val DatePickerTimeLabel = "Time"

/** The heading of the row under the dial that switches back to the calendar. */
private const val DatePickerDateLabel = "Date"

/** Whether these components include the day. */
private val AMDatePickerComponents.showsDate: Boolean
    get() = this != AMDatePickerComponents.Time

/** Whether these components include the time of day. */
private val AMDatePickerComponents.showsTime: Boolean
    get() = this != AMDatePickerComponents.Date

/**
 * A row that picks a date, a time or both: its [title] at the start and the value as Material
 * chips at the end; a chip opens the picker in the spotlight.
 *
 * ```kotlin
 * var departs by rememberSaveable { mutableStateOf(LocalDateTime.of(2026, 11, 6, 9, 0)) }
 * AMDatePicker("Departs", departs, onValueChange = { departs = it }, dateRange = LocalDate.now()..LocalDate.MAX)
 * AMDatePicker("Wake-up alarm", alarm, onValueChange = { alarm = it }, components = AMDatePickerComponents.Time)
 * ```
 *
 * - **A row, not a surface.** It follows the row recipe (28 dp above and below, 16 dp at the
 *   sides) and draws no fill: stack pickers in a card with `AMDivider(inset = spacing.lg)` between
 *   them. At large text sizes, when the chips and the title's longest word can't share the row,
 *   the chips move under the title (and wrap under each other if they must).
 * - **The chips** are Material's assist chips, each sized to its own text, so the value always shows
 *   in full: the date in the locale's medium style ("Nov 5, 2026"), the time in the locale's hours
 *   and minutes, following the phone's 12- or 24-hour setting ("9:41 AM", "09:41").
 * - **The picker opens in the spotlight** ([AMSpotlight]): the screen blurs and the field's [title]
 *   sits over a solid card holding Material's pickers. The date chip opens Material's calendar
 *   (`DatePicker` without its header or mode toggle, lying on the card); the time chip opens
 *   Material's clock dial (`TimePicker`). For [AMDatePickerComponents.DateAndTime] a row under the
 *   calendar ("Time" and the time chip) switches the card to the dial, and a row under the dial
 *   ("Date" and the date chip) switches it back. Nothing takes focus and no keyboard comes up.
 *   Changes apply as they're made, through [onValueChange]; the spotlight stays up until a tap
 *   outside it or back. It's a window, so it covers the screen from inside a sheet too.
 * - [dateRange] limits the days the calendar offers; the time isn't limited.
 * - TalkBack reads the title, then each chip as a button named by its value.
 *
 * Disabled (its own [enabled], or a disabling container around it), the title and chips take
 * Material's disabled colors and the chips don't open.
 *
 * @param title the field's name: the row's title, and the spotlight's heading.
 * @param value the moment shown and picked. The parts [components] leaves out pass through
 *   unchanged.
 * @param onValueChange called with the new moment each time the picker changes: the day for a date,
 *   the hour and minute (seconds cleared) for a time.
 * @param modifier the row's placement.
 * @param components whether the row shows and picks the date, the time or both.
 * @param dateRange the first and last day the calendar lets you pick, or null for any day.
 * @param enabled whether the chips open the picker.
 */
@Composable
public fun AMDatePicker(
    title: String,
    value: LocalDateTime,
    onValueChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    components: AMDatePickerComponents = AMDatePickerComponents.DateAndTime,
    dateRange: ClosedRange<LocalDate>? = null,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    var opened by rememberSaveable { mutableStateOf<DatePickerSpotlightPart?>(null) }
    InputControlRow(enabled = isEnabled, modifier = modifier, label = { Text(title) }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.sm)) {
            if (components.showsDate) {
                AssistChip(
                    onClick = { opened = DatePickerSpotlightPart.Calendar },
                    label = { Text(rememberDateText(value)) },
                    enabled = isEnabled,
                )
            }
            if (components.showsTime) {
                AssistChip(
                    onClick = { opened = DatePickerSpotlightPart.Dial },
                    label = { Text(rememberTimeText(value)) },
                    enabled = isEnabled,
                )
            }
        }
    }
    AMSpotlight(opened != null && isEnabled, onDismissRequest = { opened = null }, title = title) {
        DatePickerSpotlightContent(
            value,
            onValueChange,
            components,
            dateRange,
            initialPart = opened ?: DatePickerSpotlightPart.Calendar,
        )
    }
}

/** The day in the locale's medium date style ("Nov 5, 2026"). */
@Composable
private fun rememberDateText(value: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    return formatter.format(value)
}

/** The time in the locale's hours and minutes, on the phone's 12- or 24-hour clock ("9:41 AM"). */
@Composable
private fun rememberTimeText(value: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val formatter = remember(locale, is24Hour) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"), locale)
    }
    return formatter.format(value)
}

/**
 * What the spotlight holds: the calendar (header and mode toggle removed, its container clear so it
 * lies on the card) or the clock dial, starting at [initialPart]. When the row picks both, a row
 * under each switches to the other. Neither takes focus or raises the keyboard. Each picker starts
 * from [value] as the spotlight opens and reports every change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSpotlightContent(
    value: LocalDateTime,
    onValueChange: (LocalDateTime) -> Unit,
    components: AMDatePickerComponents,
    dateRange: ClosedRange<LocalDate>?,
    initialPart: DatePickerSpotlightPart,
) {
    val spacing = AMTheme.tokens.spacing
    val latestValue = rememberUpdatedState(value)
    val latestOnValueChange = rememberUpdatedState(onValueChange)
    val timeState = rememberTimePickerState(initialHour = value.hour, initialMinute = value.minute)
    ApplyTimeChanges(timeState, latestValue, latestOnValueChange)
    if (!components.showsDate) {
        TimePicker(timeState, Modifier.padding(spacing.lg))
        return
    }
    val selectableDates = remember(dateRange) { dateRange?.let(::DatePickerDayRange) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = value.toLocalDate().utcMillis(),
        yearRange = dateRange?.pickerYears() ?: DatePickerDefaults.YearRange,
        selectableDates = selectableDates ?: DatePickerDefaults.AllDates,
    )
    LaunchedEffect(dateState) {
        snapshotFlow { dateState.selectedDateMillis }.collect { millis ->
            val day = millis?.utcDate() ?: return@collect
            val current = latestValue.value
            if (day != current.toLocalDate()) latestOnValueChange.value(LocalDateTime.of(day, current.toLocalTime()))
        }
    }
    var part by remember { mutableStateOf(if (components.showsTime) initialPart else DatePickerSpotlightPart.Calendar) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (part) {
            DatePickerSpotlightPart.Calendar -> {
                DatePicker(
                    dateState,
                    Modifier.padding(top = spacing.sm),
                    colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
                    title = null,
                    headline = null,
                    showModeToggle = false,
                )
                if (components.showsTime) {
                    DatePickerSwitchRow(DatePickerTimeLabel, rememberTimeText(value)) {
                        part = DatePickerSpotlightPart.Dial
                    }
                }
            }
            DatePickerSpotlightPart.Dial -> {
                TimePicker(timeState, Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg))
                DatePickerSwitchRow(DatePickerDateLabel, rememberDateText(value)) {
                    part = DatePickerSpotlightPart.Calendar
                }
            }
        }
    }
}

/**
 * A row under a picker in the spotlight, on the row recipe: [label] at the start and the other
 * part's value as a chip at the end, which switches the card to that part's picker.
 */
@Composable
private fun DatePickerSwitchRow(label: String, value: String, onClick: () -> Unit) {
    InputControlRow(enabled = true, modifier = Modifier, label = { Text(label) }) {
        AssistChip(onClick = onClick, label = { Text(value) })
    }
}

/** Reports each hour or minute [timeState] moves to, over the latest value, seconds cleared. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplyTimeChanges(
    timeState: TimePickerState,
    latestValue: State<LocalDateTime>,
    latestOnValueChange: State<(LocalDateTime) -> Unit>,
) {
    LaunchedEffect(timeState) {
        snapshotFlow { timeState.hour to timeState.minute }.collect { (hour, minute) ->
            val current = latestValue.value
            if (hour != current.hour || minute != current.minute) {
                latestOnValueChange.value(current.withHour(hour).withMinute(minute).withSecond(0).withNano(0))
            }
        }
    }
}

/** The calendar's days limited to a range, in the picker's UTC-midnight milliseconds. */
@OptIn(ExperimentalMaterial3Api::class)
private class DatePickerDayRange(private val range: ClosedRange<LocalDate>) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis.utcDate() in range

    override fun isSelectableYear(year: Int): Boolean = year in range.pickerYears()
}

/**
 * The years a range of days spans, within the calendar's own years (Material's default range), so
 * an open-ended range doesn't ask the calendar for millions of months.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun ClosedRange<LocalDate>.pickerYears(): IntRange {
    val years = DatePickerDefaults.YearRange
    return start.year.coerceIn(years)..endInclusive.year.coerceIn(years)
}

/** This day as Material's date picker holds it: milliseconds at its midnight in UTC. */
private fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** The day these UTC-midnight milliseconds name. */
private fun Long.utcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
