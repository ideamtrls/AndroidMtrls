package io.github.jmsdnns.androidmtrls.demo.pages.pickers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.pickers.AMDateComponents
import io.github.jmsdnns.androidmtrls.controls.pickers.AMDatePickerRow
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ---------------------------------------------------------------------------
// LocalDate Saver — saves as epochDay Long for rememberSaveable
// ---------------------------------------------------------------------------

/**
 * [Saver] for [LocalDate] that persists the epoch-day [Long] value.
 *
 * Required because [LocalDate] is not a `Parcelable` or `Serializable` that
 * `rememberSaveable` can persist natively across Activity recreation.
 */
private val LocalDateSaver: Saver<LocalDate, Long> = Saver(
    save    = { it.toEpochDay() },
    restore = { LocalDate.ofEpochDay(it) },
)

// ---------------------------------------------------------------------------
// datePickerEntry — catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMDatePickerRow].
 *
 * id: "pickers-date", section: [ComponentSection.Pickers].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Pickers
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val datePickerEntry: ComponentEntry = ComponentEntry(
    id       = "pickers-date",
    section  = ComponentSection.Pickers,
    title    = "Date Picker",
    tileTitle = "Date",
    subtitle = "Flat row date & time picker",
    intro    = "Flat row style for a DatePicker. Caller's label leading, system compact " +
        "date/time picker trailing. Place it in a card or section for the surface.",
    icon     = Icons.Default.DateRange,
    content  = { DatePickerPage() },
)

// ---------------------------------------------------------------------------
// DatePickerPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMDatePickerRow].
 *
 * Two groups per the flat-first card-second structure (mirroring §3.4 demo spec):
 *
 * - **Flat** — `catalogFlatDemo(padding = 0.dp)` wrapping the widget directly
 *   on a surface strip. The row self-pads (lg horizontal, md vertical).
 * - **Card** — widget inside a bare [AMCard] with horizontal page padding on
 *   the container, showing the standard card-wrapped usage.
 *
 * Both groups demo date-only components as specified by §3.4 ("Due date",
 * date components only).
 */
@Composable
fun DatePickerPage() {
    val zoneId = ZoneId.systemDefault()

    // Flat demo state — LocalDate stored as epochDay Long via custom Saver.
    var flatDate by rememberSaveable(stateSaver = LocalDateSaver) {
        mutableStateOf(LocalDate.now())
    }
    var cardDate by rememberSaveable(stateSaver = LocalDateSaver) {
        mutableStateOf(LocalDate.now())
    }

    // Helper: convert LocalDate to epoch millis for the component.
    fun LocalDate.toEpochMillis(): Long =
        atStartOfDay(zoneId).toInstant().toEpochMilli()

    // Helper: update a LocalDate from epoch millis.
    fun epochMillisToDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()

    // ── Flat ──────────────────────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        catalogFlatDemo(padding = 0.dp) {
            AMDatePickerRow(
                label               = "Due",
                selection           = flatDate.toEpochMillis(),
                onSelectionChange   = { flatDate = epochMillisToDate(it) },
                displayedComponents = AMDateComponents.Date,
            )
        }
    }

    // ── Card ──────────────────────────────────────────────────────────────────
    catalogGroup(title = "Card") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMDatePickerRow(
                label               = "Due",
                selection           = cardDate.toEpochMillis(),
                onSelectionChange   = { cardDate = epochMillisToDate(it) },
                displayedComponents = AMDateComponents.Date,
            )
        }
    }

}
