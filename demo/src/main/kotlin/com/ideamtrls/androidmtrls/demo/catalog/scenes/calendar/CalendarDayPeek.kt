package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMSpotlight
import com.ideamtrls.androidmtrls.AMTheme
import java.time.LocalDate

/** How many events a day peek lists before folding the rest into "+2 more". */
private const val CalendarPeekEventLimit = 4

/**
 * The day peek: a held day in the spotlight, under its heading ("Today", or "Wednesday, Nov 11"),
 * on a flat card low on the screen where the remote's tiles sit: its first four events on the
 * timeline the month view lists (the now entry among them on today, then "+2 more"), or "Nothing
 * scheduled", over Add event and Open day. The day stays unselected unless Open day selects it.
 */
@Composable
internal fun CalendarDayPeek(model: CalendarModel) {
    val formats = rememberCalendarFormats()
    val now = rememberCalendarNow()
    AMSpotlight(
        model.peekDay,
        onDismissRequest = { model.peekDay = null },
        title = { day -> formats.dayHeading(day, now.toLocalDate()) },
        actions = { day ->
            AMActionGridRow(
                {
                    AMActionGridButton(
                        painterResource(R.drawable.ic_calendar_add_on), "Add event",
                        onClick = { model.addEvent(day) },
                    )
                },
                {
                    AMActionGridButton(
                        painterResource(R.drawable.ic_event_available), "Open day",
                        onClick = { model.openDay(day) },
                    )
                },
            )
        },
        // Flat, as the scene's cards are.
        elevation = AMElevation.None,
    ) { day ->
        CalendarDayPeekAgenda(day, model.events)
    }
}

/** The held day's compact agenda, read-only, or "Nothing scheduled" on a clear day. */
@Composable
private fun CalendarDayPeekAgenda(day: LocalDate, events: List<CalendarEvent>) {
    val now = rememberCalendarNow()
    val entries = calendarCompactAgenda(day, events, now, CalendarPeekEventLimit)
    Box(Modifier.fillMaxWidth().padding(AMTheme.tokens.spacing.lg)) {
        if (entries.isEmpty()) {
            CalendarNothingScheduled()
        } else {
            CalendarAgendaTimeline(entries)
        }
    }
}
