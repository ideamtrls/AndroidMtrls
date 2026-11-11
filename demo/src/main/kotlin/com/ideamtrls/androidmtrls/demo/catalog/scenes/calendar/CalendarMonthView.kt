package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.ideamtrls.androidmtrls.AMCalendar
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMTimeline
import com.ideamtrls.androidmtrls.AMTimelineDot
import com.ideamtrls.androidmtrls.AMTimelineLabel
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amTimelineFirstLine
import java.time.LocalDate

/**
 * The landing view: one month with the selected day's agenda under it, on the picture of that
 * month's weather. The month's name heads the page, with the light/dark chip at its end; under it
 * the month in a frosted card, then the day's heading over its agenda. A tap on a day selects it;
 * a hold peeks at it without selecting it; a tap on an event opens it in the form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarMonthView(model: CalendarModel) {
    val spacing = AMTheme.tokens.spacing
    val formats = rememberCalendarFormats()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AMPageHeader(
                formats.monthTitle(model.displayedMonth),
                actions = { SceneAppearanceButton() },
                scrollBehavior = scrollBehavior,
            )
        },
        // The month's picture is the scene's backdrop.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = CalendarFabClearance),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            AMCalendar(
                model.displayedMonth,
                selected = model.selectedDay,
                onSelectedChange = model::selectDay,
                modifier = Modifier.fillMaxWidth().amCard(fill = AMSurfaceFill.Frosted),
                markers = { day -> model.events.count { it.occursOn(day) } },
                onHold = { model.peekDay = it },
            )
            CalendarDayAgenda(model.selectedDay, model.events, onSelectEvent = model::editEvent)
        }
    }
}

/**
 * The selected day's heading ("Today", or "Tuesday, Nov 10") over its agenda, a timeline in a
 * frosted card; a clear day shows "Nothing scheduled" on the backdrop instead, never on a card. The
 * heading and the now entry follow the clock.
 */
@Composable
private fun CalendarDayAgenda(day: LocalDate, events: List<CalendarEvent>, onSelectEvent: (CalendarEvent) -> Unit) {
    val spacing = AMTheme.tokens.spacing
    val formats = rememberCalendarFormats()
    val now = rememberCalendarNow()
    val heading = formats.dayHeading(day, now.toLocalDate())
    val entries = calendarAgenda(day, events, now)
    if (entries.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            AMSectionHeader(heading)
            CalendarNothingScheduled(Modifier.padding(spacing.lg))
        }
    } else {
        AMSection(
            heading,
            style = AMSectionStyle.Card,
            fill = AMSurfaceFill.Frosted,
            elevation = AMElevation.None,
            inset = false,
        ) {
            CalendarAgendaTimeline(entries, onSelectEvent = onSelectEvent)
        }
    }
}

/** A clear day: the calendar glyph, "Nothing scheduled" and "No events on this day." */
@Composable
internal fun CalendarNothingScheduled(modifier: Modifier = Modifier) {
    AMEmptyState(
        painterResource(R.drawable.ic_calendar_today),
        "Nothing scheduled",
        modifier,
        description = "No events on this day.",
    )
}

/**
 * A day's agenda on a timeline: each event a dot in its color beside its title over its times and
 * place, in even digits so the times line up from event to event; the now entry a red dot beside the current time and a red rule; a compact agenda's last
 * entry a quiet dot beside "+2 more". With [onSelectEvent] a tap on an event opens it; without it
 * (the day peek) the events are read-only, each read as one piece.
 */
@Composable
internal fun CalendarAgendaTimeline(
    entries: List<CalendarAgendaEntry>,
    modifier: Modifier = Modifier,
    onSelectEvent: ((CalendarEvent) -> Unit)? = null,
) {
    val formats = rememberCalendarFormats()
    AMTimeline(
        entries,
        modifier.fillMaxWidth(),
        key = { it.key },
        marker = { entry ->
            when (entry) {
                is CalendarAgendaEntry.Event -> AMTimelineDot(color = entry.event.tint)
                is CalendarAgendaEntry.Now -> AMTimelineDot(color = AMSemantic.Destructive.color)
                is CalendarAgendaEntry.More -> AMTimelineDot()
            }
        },
    ) { entry ->
        when (entry) {
            is CalendarAgendaEntry.Event -> {
                val event = entry.event
                if (onSelectEvent != null) {
                    AMTimelineLabel(
                        event.title,
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable(role = Role.Button) { onSelectEvent(event) },
                        subtitle = formats.agendaDetail(event),
                        tabularFigures = true,
                    )
                } else {
                    AMTimelineLabel(
                        event.title,
                        Modifier.semantics(mergeDescendants = true) {},
                        subtitle = formats.agendaDetail(event),
                        tabularFigures = true,
                    )
                }
            }
            is CalendarAgendaEntry.Now -> CalendarNowLabel(formats.time(entry.time))
            is CalendarAgendaEntry.More -> CalendarMoreLabel(entry.count)
        }
    }
}

/** The caption the now and more entries share: a semibold caption with even digits. */
private val CalendarAgendaCaption: TextStyle
    @Composable get() = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")

/**
 * The now entry: "Now · 2:41 PM" in red with a red rule to the end, the way a day view marks the
 * current time. Its caption is the entry's first line, so the dot centers on it. Read as "Now, 2:41 PM".
 */
@Composable
private fun CalendarNowLabel(time: String) {
    val color = AMSemantic.Destructive.color
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Now, $time" },
        horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Now · $time", Modifier.amTimelineFirstLine(), color = color, maxLines = 1, style = CalendarAgendaCaption)
        // Two hairlines thick, so it reads at a glance without competing with the rail.
        Box(Modifier.weight(1f).height(AMTheme.hairlineWidth * 2).background(color))
    }
}

/** A compact agenda's last entry: "+2 more", quiet, read as "2 more". */
@Composable
private fun CalendarMoreLabel(count: Int) {
    Text(
        "+$count more",
        Modifier.amTimelineFirstLine().clearAndSetSemantics { contentDescription = "$count more" },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = CalendarAgendaCaption,
    )
}
