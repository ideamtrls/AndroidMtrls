package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

// ---------------------------------------------------------------------------
// Bespoke layout constants (hardcoded — NOT tokens, matching iOS IMCalendar)
// ---------------------------------------------------------------------------

/** Row height reserved for each day cell — the min touch target for a day tap. */
private val CalendarCellHeight: Dp = 44.dp

/** Diameter of the numeral frame and the today/selected background circle. */
private val CalendarNumberSize: Dp = 30.dp

/** Diameter of a single event dot. */
private val CalendarDotSize: Dp = 4.dp

/** Gap between the three fixed dot slots. */
private val CalendarDotSpacing: Dp = 3.dp

/** Gap between the numeral frame and the dot row (iOS `VStack(spacing: 2)`). */
private val CalendarNumberDotGap: Dp = 2.dp

/** Event dots are drawn capped at three so a busy day never grows the cell. */
private const val CalendarMaxDots = 3

// ---------------------------------------------------------------------------
// AMCalendar
// ---------------------------------------------------------------------------

/**
 * A month-at-a-glance calendar: a weekday header over a grid of bare day
 * numerals, one month at a time. Today's numeral sits inside a solid accent
 * circle, the selected day carries a neutral-fill wash, and each day shows up
 * to three event dots. A **display primitive first** — it lays out the month
 * and its markers — that also reports day taps: tapping a day calls
 * [onSelectionChange] with that date and moves the highlight. It is **not a
 * date picker**; there is no confirm step, no popup, no range. Cells outside
 * [month] render blank — no gridlines, no per-cell boxes, no spillover numbers.
 *
 * Like the other flat widgets, `AMCalendar` paints **no card chrome** and
 * self-pads (horizontal `spacing.lg`, vertical `spacing.md`, `fillMaxWidth`),
 * so it drops straight onto a full-bleed [AMTheme.Surface.primary] band or sits
 * inside a bare [AMCard] (no `padding:`) for the elevated treatment — the
 * surrounding container supplies the surface.
 *
 * ## Anatomy
 *
 * - **Weekday header** — seven equal columns of short weekday symbols
 *   ([DayOfWeek.getDisplayName] with [JavaTextStyle.SHORT], e.g. "Sun" — mirrors
 *   iOS `calendar.shortWeekdaySymbols`), rotated to the locale's first day of the
 *   week. Rendered in [AMTheme.Type.labelSmall] (11sp / Medium — the house map of
 *   iOS `caption2.semibold`; emphasis is Medium, never SemiBold) in
 *   [AMTheme.Text.secondary]. Matches the weekday header of `AMDatePickerRow`'s
 *   dialog.
 * - **Day grid** — rows of seven cells. Each cell is [CalendarCellHeight] tall;
 *   blank cells are empty spacers of the same height so the grid never jumps.
 * - **Day cell** — the whole cell is the tap target (a comfortable 44dp), with
 *   an **unbounded** circular [amRipple] centered on it: the cell is a
 *   flexible-width column, so a bounded ripple would clip to a wide ellipse —
 *   an unbounded disc reads as the round press that echoes the numeral badge.
 *   The numeral sits in a [CalendarNumberSize] frame with `tnum` monospaced
 *   digits so single- and double-digit days align across rows; today's numeral
 *   is Medium weight, the rest Normal.
 * - **Background circle** (behind the numeral) — one circle carries both
 *   states: `tokens.semantic.accent` for **today** (numeral inverts to
 *   [AMTheme.Surface.primary] — white-on-black in light, black-on-white in
 *   dark), [AMTheme.neutralFill] for the **selected** day (numeral stays
 *   [AMTheme.Text.primary]), transparent otherwise. Today wins when a day is
 *   both today and selected.
 * - **Event dots** — three fixed slots below the numeral, filled with the
 *   accent up to `min(`[markers]`(day), 3)`, transparent otherwise, so the cell
 *   height is stable whatever the count.
 *
 * Locale drives everything: the first day of the week and the weekday names
 * come from `WeekFields.of(Locale.getDefault()).firstDayOfWeek` and the platform
 * locale (the same recipe as `AMDatePickerRow`). Each cell's accessibility
 * label is the fully-formatted date ([FormatStyle.FULL]); the selected cell
 * additionally carries the `selected` state for TalkBack.
 *
 * ```kotlin
 * var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
 * var selectedDay by remember { mutableStateOf(LocalDate.now()) }
 * AMCard {
 *     AMCalendar(
 *         month = displayedMonth,
 *         selection = selectedDay,
 *         onSelectionChange = { selectedDay = it },
 *         markers = { day -> eventCounts[day] ?: 0 },
 *     )
 * }
 * ```
 *
 * @param month             The month to display — the grid lays out this month
 *                          only. Advance it (`month.plusMonths(1)`) from the
 *                          caller to page between months.
 * @param selection         The selected day; the neutral-fill highlight tracks
 *                          it. Days outside [month] simply do not appear.
 * @param onSelectionChange Invoked with the tapped day. Hoist [selection] and
 *                          write it here.
 * @param markers           Per-day event-dot count, drawn capped at three.
 *                          Defaults to no dots.
 * @param modifier          Modifier for the calendar's root column.
 */
@Composable
fun AMCalendar(
    month: YearMonth,
    selection: LocalDate,
    onSelectionChange: (LocalDate) -> Unit,
    markers: (LocalDate) -> Int = { 0 },
    modifier: Modifier = Modifier,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    val locale = Locale.getDefault()
    val firstDayOfWeek: DayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val today = LocalDate.now()

    // Resolved once for every cell below.
    val textPrimary = AMTheme.Text.primary.resolve(isDark)
    val textSecondary = AMTheme.Text.secondary.resolve(isDark)
    val surfacePrimary = AMTheme.Surface.primary.resolve(isDark)
    val accent = tokens.semantic.accent.resolve(isDark)
    val neutralFill = AMTheme.neutralFill()

    val orderedWeekdays = remember(firstDayOfWeek) { amOrderedWeekdays(firstDayOfWeek) }
    val weeks = remember(month, firstDayOfWeek) {
        amMonthDayCells(month, firstDayOfWeek).chunked(7)
    }
    val fullDateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    }

    // One shared, stateless ripple factory: every cell passes
    // `interactionSource = null`, so each lazily creates its own source and only
    // the tapped cell ripples (the AMDatePickerDialog day-grid pattern).
    val dayCellRipple = amRipple(bounded = false, radius = CalendarCellHeight / 2)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        // ── Weekday header — seven equal columns ────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            orderedWeekdays.forEach { dow ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        // SHORT (3-letter, e.g. "Sun") mirrors iOS
                        // `calendar.shortWeekdaySymbols`.
                        text = dow.getDisplayName(JavaTextStyle.SHORT, locale),
                        style = AMTheme.Type.labelSmall.toTextStyle(textSecondary),
                    )
                }
            }
        }

        // ── Day grid — rows of seven ────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        if (day == null) {
                            // Out-of-month cell: an empty spacer of the same
                            // height and width so the grid geometry never shifts.
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(CalendarCellHeight),
                            )
                        } else {
                            AMCalendarDayCell(
                                day = day,
                                isToday = day == today,
                                isSelected = day == selection,
                                dotCount = markers(day).coerceIn(0, CalendarMaxDots),
                                ripple = dayCellRipple,
                                accent = accent,
                                neutralFill = neutralFill,
                                textPrimary = textPrimary,
                                surfacePrimary = surfacePrimary,
                                contentDescription = fullDateFormatter.format(day),
                                onClick = { onSelectionChange(day) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Day cell
// ---------------------------------------------------------------------------

/**
 * One tappable day in the grid: numeral badge over three event-dot slots,
 * filling one flexible column via [RowScope.weight].
 */
@Composable
private fun RowScope.AMCalendarDayCell(
    day: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    dotCount: Int,
    ripple: IndicationNodeFactory,
    accent: Color,
    neutralFill: Color,
    textPrimary: Color,
    surfacePrimary: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    // One circle carries both states: solid accent for today (numeral inverts to
    // the surface color), a neutral wash for the selected day, else nothing.
    val fillColor = when {
        isToday -> accent
        isSelected -> neutralFill
        else -> Color.Transparent
    }
    val numeralColor = if (isToday) surfacePrimary else textPrimary
    // iOS numeral is `.subheadline` → the house map is Type.secondary (14sp).
    val numeralStyle = AMTheme.Type.secondary
        .toTextStyle(numeralColor)
        .copy(
            fontWeight = if (isToday) FontWeight.Medium else FontWeight.Normal,
            // Monospaced ("table") digits so 1 and 11 sit at the same x.
            fontFeatureSettings = "tnum",
        )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(CalendarCellHeight)
            .clickable(
                interactionSource = null,
                indication = ripple,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
                if (isSelected) selected = true
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CalendarNumberDotGap),
        ) {
            // Numeral inside its fixed circular frame.
            Box(
                modifier = Modifier
                    .size(CalendarNumberSize)
                    .clip(CircleShape)
                    .background(fillColor),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = day.dayOfMonth.toString(),
                    style = numeralStyle,
                    // The cell already announces the full date; drop the bare
                    // numeral from the a11y tree so TalkBack does not read "28"
                    // on top of "Saturday, February 28, 2026".
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }

            // Three fixed dot slots so numerals align across rows; unused slots
            // stay transparent, keeping every cell the same height.
            Row(horizontalArrangement = Arrangement.spacedBy(CalendarDotSpacing)) {
                repeat(CalendarMaxDots) { index ->
                    Box(
                        modifier = Modifier
                            .size(CalendarDotSize)
                            .clip(CircleShape)
                            .background(if (index < dotCount) accent else Color.Transparent),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews (light + dark, as required by conventions)
// ---------------------------------------------------------------------------

@Preview(
    name = "AMCalendar · Light",
    group = "AMCalendar",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMCalendarPreviewLight() {
    AMThemeRoot {
        AMCalendarPreviewContent()
    }
}

@Preview(
    name = "AMCalendar · Dark",
    group = "AMCalendar",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMCalendarPreviewDark() {
    AMThemeRoot {
        AMCalendarPreviewContent()
    }
}

@Composable
private fun AMCalendarPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val month = remember { YearMonth.now() }
    var flatDay by remember { mutableStateOf(LocalDate.now()) }
    var cardDay by remember { mutableStateOf(LocalDate.now()) }

    // Sample dot counts keyed by day-of-month so a handful of days show dots.
    val markerCounts = remember { mapOf(3 to 1, 7 to 3, 12 to 2, 18 to 1, 24 to 3, 28 to 2) }
    val markers: (LocalDate) -> Int = { day -> markerCounts[day.dayOfMonth] ?: 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(vertical = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Flat on a full-bleed primary-surface band (self-pads).
        Box(modifier = Modifier.background(AMTheme.Surface.primary.resolve(isDark))) {
            AMCalendar(
                month = month,
                selection = flatDay,
                onSelectionChange = { flatDay = it },
                markers = markers,
            )
        }

        // Inside a bare card for the elevated treatment.
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMCalendar(
                month = month,
                selection = cardDay,
                onSelectionChange = { cardDay = it },
                markers = markers,
            )
        }
    }
}
