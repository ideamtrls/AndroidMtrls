package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMCalendar
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import java.time.LocalDate
import java.time.YearMonth

// ---------------------------------------------------------------------------
// Calendar catalog entry
// ---------------------------------------------------------------------------

val calendarEntry: ComponentEntry = ComponentEntry(
    id = "content-calendar",
    section = ComponentSection.Content,
    title = "Calendar",
    subtitle = "Month grid with day selection",
    intro = "Month-at-a-glance calendar — bare day numerals with a solid accent ring on " +
        "today, a neutral highlight on the selected day, and per-day event dots capped at " +
        "three; day taps report through the selection callback. Flat by nature: place it on " +
        "a full-bleed surface or inside a card for the elevated treatment.",
    icon = Icons.Default.CalendarMonth,
    content = { CalendarPage() },
)

// ---------------------------------------------------------------------------
// CalendarPage — demo composable
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMCalendar].
 *
 * Three groups mirroring iOS `IMCalendarPage`:
 * - **Flat** — the calendar dropped straight onto a full-bleed
 *   [AMTheme.Surface.primary] band (it self-pads, so `catalogFlatDemo(padding = 0)`).
 * - **In a card** — the same calendar inside a bare [AMCard] for the elevated
 *   treatment.
 * - **With markers** — event-dot counts per day, capped at three.
 *
 * Each group hoists its own selected-day state so tapping one calendar does not
 * move another's highlight.
 */
@Composable
fun CalendarPage() {
    val month = remember { YearMonth.now() }

    // Sample event counts keyed by day-of-month so a handful of days show dots.
    val markerCounts = remember { mapOf(3 to 1, 7 to 3, 12 to 2, 18 to 1, 24 to 3, 28 to 2) }
    val markers: (LocalDate) -> Int = { day -> markerCounts[day.dayOfMonth] ?: 0 }

    // ── Flat on a surface band ──────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        var flatDay by remember { mutableStateOf(LocalDate.now()) }
        catalogFlatDemo(padding = 0.dp) {
            AMCalendar(
                month = month,
                selection = flatDay,
                onSelectionChange = { flatDay = it },
            )
        }
    }

    // ── Inside a card ───────────────────────────────────────────────────────────
    catalogGroup(title = "In a card") {
        var cardDay by remember { mutableStateOf(LocalDate.now()) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMCard(modifier = Modifier.fillMaxWidth()) {
                AMCalendar(
                    month = month,
                    selection = cardDay,
                    onSelectionChange = { cardDay = it },
                )
            }
        }
    }

    // ── With event markers ──────────────────────────────────────────────────────
    catalogGroup(title = "With markers") {
        var markedDay by remember { mutableStateOf(LocalDate.now()) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMCard(modifier = Modifier.fillMaxWidth()) {
                AMCalendar(
                    month = month,
                    selection = markedDay,
                    onSelectionChange = { markedDay = it },
                    markers = markers,
                )
            }
        }
    }
}
