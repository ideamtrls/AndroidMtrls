package io.github.jmsdnns.androidmtrls.demo.scenes.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMCalendar
import io.github.jmsdnns.androidmtrls.content.AMEmptyState
import io.github.jmsdnns.androidmtrls.content.AMPageHeader
import io.github.jmsdnns.androidmtrls.content.AMSectionHeader
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabClearance
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import java.time.LocalDate
import java.time.YearMonth

// ---------------------------------------------------------------------------
// CalendarLandingView — one month over its selected day's events
// ---------------------------------------------------------------------------

/**
 * The Calendar landing view: one month at a time with the selected day's events
 * listed inline below. An [AMPageHeader] month title sits over a flat,
 * full-bleed [AMTheme.Surface.primary] band holding the [AMCalendar], so the
 * grid reads as the same kind of surface as the event rows below; under the
 * band an [AMSectionHeader] names the selected day and its events render as a
 * bare stack of flat full-width [AMListRow]s (title, location subtitle, trailing
 * mono time), or an [AMEmptyState] on a clear day. The whole view is one scroll,
 * clearing the top safe area. Tapping a day re-points the highlight through
 * [onSelectDay]; tapping an event calls [onSelectEvent]. Pure — the scene root
 * owns the displayed month, the selected day, and the events. Ports iOS
 * `CalendarLandingView`.
 */
@Composable
fun CalendarLandingView(
    month: YearMonth,
    selectedDate: LocalDate,
    events: List<EventData>,
    eventCounts: Map<LocalDate, Int>,
    onSelectDay: (LocalDate) -> Unit,
    onSelectEvent: (EventData) -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(top = AMTheme.Spacing.lg.dp)
            .padding(bottom = (AMTheme.Spacing.xxl * 2).dp + sceneFabClearance()),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp),
    ) {
        // Month title — horizontally inset like the section header below.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMPageHeader(calendarMonthTitle(month))
        }

        // The month grid on a flat, full-bleed primary-surface band (self-pads).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AMTheme.Surface.primary.resolve(isDark)),
        ) {
            AMCalendar(
                month = month,
                selection = selectedDate,
                onSelectionChange = onSelectDay,
                markers = { day -> eventCounts[day] ?: 0 },
            )
        }

        // Selected-day heading over its time-sorted events (or an empty state).
        CalendarEventsSection(
            selectedDate = selectedDate,
            events = events,
            onSelectEvent = onSelectEvent,
        )
    }
}

/**
 * The selected day's heading over its time-sorted events — a bare stack of flat
 * rows, or an empty state when the day is clear.
 */
@Composable
private fun CalendarEventsSection(
    selectedDate: LocalDate,
    events: List<EventData>,
    onSelectEvent: (EventData) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMSectionHeader(title = calendarDayHeading(selectedDate))
        }

        if (events.isEmpty()) {
            Box(modifier = Modifier.padding(top = AMTheme.Spacing.lg.dp)) {
                AMEmptyState(
                    icon = Icons.Filled.CalendarMonth,
                    title = "Nothing scheduled",
                    description = "No events on this day.",
                )
            }
        } else {
            // Bare stack of flat full-width rows.
            Column {
                events.forEach { event ->
                    CalendarEventRow(event = event, onClick = { onSelectEvent(event) })
                }
            }
        }
    }
}

/**
 * One event row: title, location subtitle, and a trailing mono-digit time label.
 * The whole row taps into the edit sheet.
 */
@Composable
private fun CalendarEventRow(event: EventData, onClick: () -> Unit) {
    val isDark = LocalAMIsDarkTheme.current
    val interaction = remember(event.id) { MutableInteractionSource() }

    AMListRow(
        title = event.title,
        subtitle = event.location.takeIf { it.isNotEmpty() },
        modifier = Modifier.clickable(
            interactionSource = interaction,
            indication = amRipple(),
            onClick = onClick,
        ),
        trailing = {
            BasicText(
                text = event.timeLabel,
                style = AMTheme.Type.caption
                    .toTextStyle(AMTheme.Text.secondary.resolve(isDark))
                    .copy(fontFeatureSettings = "tnum"),
            )
        },
    )
}

// ---------------------------------------------------------------------------
// CalendarMonthsPicker — the year-sectioned months view
// ---------------------------------------------------------------------------

/**
 * Months picker — the scene's second root view (NOT a panel: no push, no back
 * chevron; the FAB's Months tile swaps it in, and picking a month or the Today
 * tile swaps it back). Deliberately headerless — the [AMSectionHeader] year
 * sections are its only titling: a list of years (three back through three
 * forward of the current year), each an [AMSectionHeader] with the year over
 * that year's twelve months as a bare stack of full-width [AMListRow]s. The row
 * for the displayed month carries the selection wash; the list opens scrolled to
 * the displayed month's year. Pure — the scene owns the displayed month and the
 * view swap. Ports iOS `CalendarMonthsPicker`.
 */
@Composable
fun CalendarMonthsPicker(
    displayedMonth: YearMonth,
    onPickMonth: (YearMonth) -> Unit,
) {
    val years = remember { calendarPickerYears() }
    val listState = rememberLazyListState()

    // Open scrolled to the displayed month's year (pragmatic: one item per year).
    LaunchedEffect(Unit) {
        val index = years.indexOf(displayedMonth.year)
        if (index >= 0) listState.scrollToItem(index)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            top = AMTheme.Spacing.md.dp,
            bottom = AMTheme.Spacing.xxl.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp),
    ) {
        items(years, key = { it }) { year ->
            CalendarYearSection(
                year = year,
                displayedMonth = displayedMonth,
                onPickMonth = onPickMonth,
            )
        }
    }
}

/** One year: an [AMSectionHeader] over its twelve flat month rows. */
@Composable
private fun CalendarYearSection(
    year: Int,
    displayedMonth: YearMonth,
    onPickMonth: (YearMonth) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMSectionHeader(title = year.toString())
        }

        Column {
            (1..12).forEach { monthValue ->
                val month = YearMonth.of(year, monthValue)
                val interaction = remember(month) { MutableInteractionSource() }
                AMListRow(
                    title = calendarMonthName(month),
                    isSelected = month == displayedMonth,
                    modifier = Modifier.clickable(
                        interactionSource = interaction,
                        indication = amRipple(),
                        onClick = { onPickMonth(month) },
                    ),
                )
            }
        }
    }
}
