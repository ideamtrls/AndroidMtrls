package com.ideamtrls.androidmtrls.demo

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.core.os.ConfigurationCompat
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMCalendar
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMSpotlight
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val CalendarEntry = ComponentEntry(
    id = "calendar",
    category = CatalogCategory.Selection,
    title = "Calendar",
    summary = "Month grid with event markers",
    icon = R.drawable.ic_calendar_month,
    keywords = listOf(
        "AMCalendar", "month", "date", "day", "markers", "events", "dots", "day picker", "onHold", "hold",
        "long press", "peek", "spotlight",
    ),
) { CalendarPage() }

@Composable
private fun CalendarPage() {
    ComponentPage(
        CalendarEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { MonthCalendar() },
    ) {
        DayView()
    }
}

// Stand-in for your own action.
private fun addEvent() {}

/** Dots on a handful of days, so the sample month looks lived in. */
private val SampleMarkers = mapOf(3 to 1, 7 to 3, 12 to 2, 18 to 1, 24 to 3, 28 to 2)

/** The month on a surface strip. */
@Composable
private fun MonthCalendar() {
    var day by rememberSaveable { mutableStateOf(LocalDate.now()) }
    AMCalendar(
        YearMonth.from(day),
        selected = day,
        onSelectedChange = { day = it },
        modifier = Modifier.background(AMTheme.Surface.primary),
        markers = { SampleMarkers[it.dayOfMonth] ?: 0 },
    )
}

/** Events by day of the month. Today always has a few, so the day view opens on an agenda. */
private val Schedule: Map<Int, List<String>> = mapOf(
    3 to listOf("Design review"),
    7 to listOf("Stand-up", "Lunch with Grace Hopper", "Sprint demo"),
    12 to listOf("Stand-up", "Planning"),
    18 to listOf("Dentist"),
    24 to listOf("Sprint demo", "Planning", "Dinner"),
    28 to listOf("Stand-up", "Retro"),
) + (LocalDate.now().dayOfMonth to listOf("Stand-up", "Design review", "Coffee with Grace"))

private fun eventsOn(day: LocalDate): List<String> = Schedule[day.dayOfMonth].orEmpty()

/**
 * A day view: the month in a card over the selected day's events. Hold a day to peek at it in the
 * spotlight, with a shortcut to open it.
 */
@Composable
private fun DayView() {
    var day by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var held by remember { mutableStateOf<LocalDate?>(null) }
    val spacing = AMTheme.tokens.spacing
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val heading = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), locale) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        AMCalendar(
            YearMonth.from(day),
            selected = day,
            onSelectedChange = { day = it },
            modifier = Modifier.amCard(),
            markers = { eventsOn(it).size },
            onHold = { held = it },
        )
        val events = eventsOn(day)
        if (events.isEmpty()) {
            // An empty day lies on the page, never on a card.
            AMEmptyState(
                painterResource(R.drawable.ic_calendar_today),
                "Nothing scheduled",
                Modifier.padding(spacing.lg),
                description = "No events on this day.",
            )
        } else {
            Column(Modifier.fillMaxWidth().amCard()) {
                events.forEachIndexed { index, event ->
                    if (index > 0) AMDivider(inset = spacing.lg)
                    EventRow(event)
                }
            }
        }
    }
    AMSpotlight(
        held,
        onDismissRequest = { held = null },
        title = { heading.format(it) },
        actions = { heldDay ->
            AMActionGridRow(
                {
                    AMActionGridButton(painterResource(R.drawable.ic_calendar_add_on), "Add event", onClick = {
                        addEvent()
                        held = null
                    })
                },
                {
                    AMActionGridButton(painterResource(R.drawable.ic_event_available), "Open day", onClick = {
                        day = heldDay
                        held = null
                    })
                },
            )
        },
    ) { heldDay ->
        val events = eventsOn(heldDay)
        Column(Modifier.fillMaxWidth()) {
            if (events.isEmpty()) {
                Text(
                    "Nothing scheduled",
                    Modifier.amRowPadding(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                events.forEach { EventRow(it) }
            }
        }
    }
}

/** One event: its title on the row recipe. */
@Composable
private fun EventRow(title: String) {
    Text(title, Modifier.amRowPadding(), style = MaterialTheme.typography.bodyLarge)
}
