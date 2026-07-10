package io.github.jmsdnns.androidmtrls.controls.pickers

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.jmsdnns.androidmtrls.controls.buttons.AMGhostButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButtonSize
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amElevation
import io.github.jmsdnns.androidmtrls.tokens.amHairline
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.amSquircle
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

// ---------------------------------------------------------------------------
// AMDateComponents — date/time component selector enum
// ---------------------------------------------------------------------------

/**
 * Controls which date/time components [AMDatePickerRow] displays and picks.
 *
 * iOS equivalent: `DatePickerComponents` (`.date`, `.hourAndMinute`, or both).
 */
@Immutable
enum class AMDateComponents {
    /** Date only (year, month, day). */
    Date,

    /** Time only (hour, minute). */
    Time,

    /** Both date and time. */
    DateAndTime,
}

// ---------------------------------------------------------------------------
// AMDatePickerRow — flat row with tappable date chip + custom dialog
// ---------------------------------------------------------------------------

/**
 * Flat row widget: [label] on the leading side, a tappable formatted-date chip
 * on the trailing side. Tapping the chip opens a custom house-styled date/time
 * selection dialog (`androidx.compose.ui.window.Dialog`).
 *
 * A flat widget — the caller's card or section provides the surface:
 * ```kotlin
 * AMCard {
 *     AMDatePickerRow(
 *         label     = "Due date",
 *         selection = epochMillis,
 *         onSelectionChange = { epochMillis = it },
 *     )
 * }
 * ```
 *
 * The dialog is custom-built with house tokens (no Material 3). It presents a
 * calendar-month grid for [AMDateComponents.Date] / [AMDateComponents.DateAndTime],
 * and an M3-style hour/minute numeric-input pair for [AMDateComponents.Time] /
 * [AMDateComponents.DateAndTime]. The calendar respects the platform locale's
 * first day of the week.
 *
 * Press feedback on the chip is the monochrome `amRipple()` state layer, clipped
 * to the chip's rounded rectangle — never opacity-dim.
 *
 * @param label              Row label text, headline style, leading alignment.
 * @param selection          Currently selected instant as epoch milliseconds
 *                           (UTC). Use `LocalDate.atStartOfDay(ZoneId.systemDefault())
 *                           .toInstant().toEpochMilli()` to convert from a date.
 * @param onSelectionChange  Called with the new epoch-millis value after the
 *                           user confirms selection in the dialog.
 * @param displayedComponents Which components to show/pick. Defaults to
 *                           [AMDateComponents.Date].
 * @param minimumDate        Lower bound in epoch millis (inclusive), or null
 *                           for no minimum.
 * @param maximumDate        Upper bound in epoch millis (inclusive), or null
 *                           for no maximum.
 * @param modifier           Modifier applied to the row's root layout node.
 */
@Composable
fun AMDatePickerRow(
    label: String,
    selection: Long,
    onSelectionChange: (Long) -> Unit,
    displayedComponents: AMDateComponents = AMDateComponents.Date,
    minimumDate: Long? = null,
    maximumDate: Long? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    var showDialog by rememberSaveable { mutableStateOf(false) }

    // Format the current selection for the trailing chip.
    val formattedValue = remember(selection, displayedComponents) {
        formatSelection(selection, displayedComponents)
    }

    // Press feedback on the chip — the monochrome amRipple() state layer.
    val chipInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AMTheme.rowMinHeight.dp)
            .padding(
                horizontal = tokens.spacing.lg,
                vertical   = tokens.spacing.md,
            ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // Leading label — fills remaining width, leading alignment.
        BasicText(
            text  = label,
            style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            modifier = Modifier.weight(1f),
        )

        // Trailing tappable chip showing the formatted value.
        Box(
            modifier = Modifier
                .clip(amSquircle(AMTheme.Radius.sm.dp))
                .background(
                    AMTheme.Text.primary
                        .resolve(isDark)
                        .copy(alpha = 0.08f)
                )
                .clickable(
                    interactionSource = chipInteraction,
                    indication        = amRipple(),
                    onClick           = { showDialog = true },
                )
                // Accessibility: the chip is the affordance that opens the picker —
                // name it with the label + current value and mark it a button.
                .semantics {
                    role = Role.Button
                    contentDescription = "$label, $formattedValue"
                }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text  = formattedValue,
                // Value reads at full primary emphasis — the iOS compact picker
                // shows its value chip in Text.primary, not the muted secondary.
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
        }
    }

    // Custom house-styled date/time dialog.
    if (showDialog) {
        AMDatePickerDialog(
            selection           = selection,
            onSelectionChange   = onSelectionChange,
            displayedComponents = displayedComponents,
            minimumDate         = minimumDate,
            maximumDate         = maximumDate,
            onDismiss           = { showDialog = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Private — selection formatter
// ---------------------------------------------------------------------------

/**
 * Format [epochMillis] for display in the trailing chip.
 *
 * Uses locale-aware [DateTimeFormatter] as specified in the visual recipe.
 */
private fun formatSelection(epochMillis: Long, components: AMDateComponents): String {
    val zoneId = ZoneId.systemDefault()
    // Instant → zoned → LocalDateTime is API 26-safe; LocalDateTime.ofInstant is
    // API 34+ and would crash on minSdk 31 (no core-library desugaring).
    val ldt = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()
    return when (components) {
        AMDateComponents.Date -> ldt.toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        AMDateComponents.Time -> ldt.toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        AMDateComponents.DateAndTime -> ldt.toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) +
            "  " +
            ldt.toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
}

// ---------------------------------------------------------------------------
// AMDatePickerDialog — custom house-styled calendar-month grid dialog
// ---------------------------------------------------------------------------

/**
 * Custom house-styled date/time selection dialog.
 *
 * Built entirely from house tokens — no Material 3:
 * - Surface: [AMTheme.Surface.primary] card with [AMElevation.Medium] shadow.
 * - Month navigation: leading/trailing [AMIconButton] chevrons; disabled at the
 *   selectable-range bounds.
 * - Day grid: 7-column grid whose columns start at the platform locale's first
 *   day of the week; selected day filled with the accent color, today marked
 *   with an accent dot, out-of-range days muted.
 * - Time section (if applicable): two M3-style numeric hour/minute input boxes.
 * - Confirm/cancel: right-aligned "Cancel" / "OK" ghost text buttons.
 *
 * Called internally by [AMDatePickerRow]. Not intended for direct use.
 */
@Composable
private fun AMDatePickerDialog(
    selection: Long,
    onSelectionChange: (Long) -> Unit,
    displayedComponents: AMDateComponents,
    minimumDate: Long?,
    maximumDate: Long?,
    onDismiss: () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    val zoneId = ZoneId.systemDefault()
    // Instant → zoned → LocalDateTime is API 26-safe; LocalDateTime.ofInstant is
    // API 34+ and would crash on minSdk 31 (no core-library desugaring).
    val initialLdt = Instant.ofEpochMilli(selection).atZone(zoneId).toLocalDateTime()

    // Internal mutable state — committed on "OK", discarded on dismiss.
    var pickedDate by remember { mutableStateOf(initialLdt.toLocalDate()) }
    var pickedHour by remember { mutableStateOf(initialLdt.hour) }
    var pickedMinute by remember { mutableStateOf(initialLdt.minute) }

    // Month being viewed in the calendar grid.
    var viewMonth by remember { mutableStateOf(pickedDate.withDayOfMonth(1)) }

    // Resolve date bounds. Instant.atZone(...).toLocalDate() is API 26-safe;
    // LocalDate.ofInstant is API 34+ and would crash on minSdk 31 (no desugaring).
    val minDate = minimumDate?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
    val maxDate = maximumDate?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
    val today = LocalDate.now(zoneId)

    // Locale-aware first day of the week (Sunday in en-US, Monday in most of
    // Europe, Saturday in parts of the Middle East, …).
    val locale = Locale.getDefault()
    val firstDay: DayOfWeek = WeekFields.of(locale).firstDayOfWeek

    // Localized full date for each day cell's accessibility label (e.g. "July 8,
    // 2026") so TalkBack announces the date rather than a bare number.
    val dayAccessibilityFormatter = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.LONG)
        .withLocale(locale)

    val surfaceColor   = AMTheme.Surface.primary.resolve(isDark)
    val textPrimary    = AMTheme.Text.primary.resolve(isDark)
    val textSecondary  = AMTheme.Text.secondary.resolve(isDark)
    val cardShape      = amSquircle(AMTheme.Radius.lg.dp)
    val accentColor    = if (isDark) Color.White else Color.Black

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .amElevation(level = AMElevation.Medium, shape = cardShape)
                .background(surfaceColor, cardShape)
                .padding(tokens.spacing.lg),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
            ) {

                // ── Calendar grid (shown for Date / DateAndTime) ────────────────
                if (displayedComponents == AMDateComponents.Date ||
                    displayedComponents == AMDateComponents.DateAndTime) {

                    // Month navigation header.
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        // Previous month button — disabled when the previous month
                        // ends before the minDate month (nothing selectable there).
                        val prevMonth = viewMonth.minusMonths(1)
                        val canGoPrev = minDate == null ||
                            prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()) >= minDate
                        AMIconButton(
                            icon               = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            size               = AMIconButtonSize.Small,
                            enabled            = canGoPrev,
                            contentDescription = "Previous month",
                            onClick            = { viewMonth = prevMonth },
                        )

                        BasicText(
                            text  = viewMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = AMTheme.Type.title.toTextStyle(textPrimary),
                        )

                        // Next month button — disabled when the next month starts
                        // after the maxDate month (nothing selectable there).
                        val nextMonth = viewMonth.plusMonths(1)
                        val canGoNext = maxDate == null ||
                            nextMonth.withDayOfMonth(1) <= maxDate
                        AMIconButton(
                            icon               = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            size               = AMIconButtonSize.Small,
                            enabled            = canGoNext,
                            contentDescription = "Next month",
                            onClick            = { viewMonth = nextMonth },
                        )
                    }

                    // Day-of-week header row — narrow locale names, rotated so the
                    // first column matches the locale's first day of the week.
                    val dowHeaders = (0..6).map { offset ->
                        firstDay.plus(offset.toLong())
                            .getDisplayName(JavaTextStyle.NARROW, locale)
                    }
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        dowHeaders.forEach { dow ->
                            Box(
                                modifier         = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    text  = dow,
                                    style = AMTheme.Type.labelSmall.toTextStyle(textSecondary),
                                )
                            }
                        }
                    }

                    // Build day grid: rows of 7 cells, starting from the locale's
                    // first weekday of the first week.
                    val firstDayOfMonth = viewMonth
                    // Column of the 1st = distance from the locale's first weekday.
                    val firstDow = (firstDayOfMonth.dayOfWeek.value - firstDay.value + 7) % 7
                    val daysInMonth = firstDayOfMonth.lengthOfMonth()
                    // Cells: leading blank + days.
                    val totalCells = firstDow + daysInMonth
                    val rows = (totalCells + 6) / 7

                    // One shared ripple factory for every day cell. It is stateless —
                    // each cell's clickable lazily creates its OWN interaction source
                    // (interactionSource = null below), so only the tapped cell ripples.
                    val dayCellRipple = amRipple()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in 0 until rows) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                for (col in 0..6) {
                                    val cellIndex = row * 7 + col
                                    val day = cellIndex - firstDow + 1
                                    val validDay = day in 1..daysInMonth
                                    val cellDate = if (validDay) firstDayOfMonth.withDayOfMonth(day) else null
                                    val isSelected = cellDate == pickedDate
                                    val isToday = cellDate == today
                                    val isDisabled = cellDate != null && (
                                        (minDate != null && cellDate < minDate) ||
                                        (maxDate != null && cellDate > maxDate)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            // Clip every cell to a circle so the bounded
                                            // ripple reads as a disc, not a 36dp square.
                                            .clip(CircleShape)
                                            .then(
                                                if (isSelected) Modifier.background(accentColor)
                                                else Modifier
                                            )
                                            // Selectable so TalkBack announces the day's
                                            // selected state; disabled (out-of-range) days
                                            // are non-interactive and marked disabled().
                                            .then(
                                                if (validDay && cellDate != null) Modifier.selectable(
                                                    selected          = isSelected,
                                                    interactionSource = null,
                                                    indication        = dayCellRipple,
                                                    enabled           = !isDisabled,
                                                    role              = Role.Button,
                                                    onClick           = { pickedDate = cellDate },
                                                ).semantics {
                                                    contentDescription = cellDate.format(dayAccessibilityFormatter)
                                                } else Modifier
                                            )
                                            .alpha(
                                                when {
                                                    !validDay     -> 0f
                                                    isDisabled    -> 0.30f
                                                    else          -> 1f
                                                }
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (validDay) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                BasicText(
                                                    text  = day.toString(),
                                                    style = AMTheme.Type.label
                                                        .toTextStyle(if (isSelected) surfaceColor else textPrimary)
                                                        .copy(
                                                            fontWeight = if (isToday || isSelected) FontWeight.Medium else FontWeight.Normal,
                                                        ),
                                                )
                                                // Today indicator dot.
                                                if (isToday && !isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(accentColor),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Time input (shown for Time / DateAndTime) ──────────────────
                if (displayedComponents == AMDateComponents.Time ||
                    displayedComponents == AMDateComponents.DateAndTime) {

                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(top = tokens.spacing.sm),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        // Hour field — M3 numeric input, clamps to 0..23 on commit.
                        AMTimeField(
                            value         = pickedHour,
                            range         = 0..23,
                            isDark        = isDark,
                            onValueChange = { pickedHour = it },
                        )

                        BasicText(
                            text  = ":",
                            style = TextStyle(
                                color      = textPrimary,
                                fontSize   = 24.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            modifier = Modifier.padding(horizontal = tokens.spacing.sm),
                        )

                        // Minute field — M3 numeric input, clamps to 0..59 on commit.
                        AMTimeField(
                            value         = pickedMinute,
                            range         = 0..59,
                            isDark        = isDark,
                            onValueChange = { pickedMinute = it },
                        )
                    }
                }

                // ── Dialog action buttons — right-aligned Cancel / OK ──────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    AMGhostButton(
                        label   = "Cancel",
                        onClick = onDismiss,
                    )
                    AMGhostButton(
                        label   = "OK",
                        onClick = {
                            // Clamp defensively: a field left focused with an
                            // out-of-range value would not have committed yet.
                            val h = pickedHour.coerceIn(0, 23)
                            val m = pickedMinute.coerceIn(0, 59)
                            val committed = when (displayedComponents) {
                                AMDateComponents.Date -> pickedDate.atStartOfDay(zoneId)
                                    .toInstant().toEpochMilli()
                                AMDateComponents.Time,
                                AMDateComponents.DateAndTime ->
                                    LocalDateTime.of(pickedDate, LocalTime.of(h, m))
                                        .atZone(zoneId).toInstant().toEpochMilli()
                            }
                            onSelectionChange(committed)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Private — M3-style numeric time input (hour / minute)
// ---------------------------------------------------------------------------

/**
 * A single M3-style numeric time input box for hour or minute.
 *
 * A 64×56dp outlined box (house hairline via [amHairline]; the border tints to
 * [AMTheme.Text.primary] while focused) holding a centered 24sp monospaced
 * (`tnum`) [BasicTextField]. Input is digit-only, capped at two characters, and
 * clamped into [range] when the field loses focus. The parent commits the
 * picked value only on "OK".
 *
 * @param value         Current numeric value (may briefly exceed [range] while
 *                      the user is typing; clamped on focus loss).
 * @param range         Valid inclusive range — `0..23` for hours, `0..59` for
 *                      minutes.
 * @param isDark        Current dark-mode flag.
 * @param onValueChange Called with the parsed value on each edit and with the
 *                      clamped value on focus loss.
 */
@Composable
private fun AMTimeField(
    value: Int,
    range: IntRange,
    isDark: Boolean,
    onValueChange: (Int) -> Unit,
) {
    val tokens = LocalAMTokens.current
    val textPrimary = AMTheme.Text.primary.resolve(isDark)
    val shape = amSquircle(tokens.input.radius)

    var text by remember { mutableStateOf(value.toString().padStart(2, '0')) }
    var focused by remember { mutableStateOf(false) }

    // Realign the field to an externally-changed value while unfocused (e.g. the
    // sibling's commit clamped this one). Inline compare-and-set, in composition.
    if (!focused && text.toIntOrNull() != value) {
        text = value.toString().padStart(2, '0')
    }

    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 56.dp)
            .amHairline(
                shape = shape,
                color = if (focused) textPrimary else Color.Unspecified,
            )
            .onFocusChanged { state ->
                // Losing focus commits a clamped, normalized value.
                if (focused && !state.isFocused) {
                    val clamped = (text.toIntOrNull() ?: range.first).coerceIn(range)
                    text = clamped.toString().padStart(2, '0')
                    onValueChange(clamped)
                }
                focused = state.isFocused
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(2)
                text = digits
                digits.toIntOrNull()?.let { onValueChange(it) }
            },
            textStyle = TextStyle(
                color               = textPrimary,
                fontSize            = 24.sp,
                fontWeight          = FontWeight.Medium,
                fontFamily          = FontFamily.Monospace,
                fontFeatureSettings = "tnum",
                textAlign           = TextAlign.Center,
            ),
            singleLine      = true,
            cursorBrush     = SolidColor(textPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier        = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — ROW states only (light + dark), not the dialog
// ---------------------------------------------------------------------------

@Composable
private fun DatePickerRowPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val zoneId = ZoneId.systemDefault()
    val sampleMillis = LocalDate.of(2026, 6, 15)
        .atStartOfDay(zoneId).toInstant().toEpochMilli()

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
    ) {
        // Date-only row.
        AMDatePickerRow(
            label               = "Due date",
            selection           = sampleMillis,
            onSelectionChange   = {},
            displayedComponents = AMDateComponents.Date,
        )

        // Time-only row.
        AMDatePickerRow(
            label               = "Reminder time",
            selection           = sampleMillis,
            onSelectionChange   = {},
            displayedComponents = AMDateComponents.Time,
        )

        // Date + time row.
        AMDatePickerRow(
            label               = "Event start",
            selection           = sampleMillis,
            onSelectionChange   = {},
            displayedComponents = AMDateComponents.DateAndTime,
        )
    }
}

@Preview(
    name            = "AMDatePickerRow · Light",
    group           = "AMDatePickerRow",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
)
@Composable
private fun AMDatePickerRowPreviewLight() {
    AMThemeRoot {
        DatePickerRowPreviewContent()
    }
}

@Preview(
    name            = "AMDatePickerRow · Dark",
    group           = "AMDatePickerRow",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMDatePickerRowPreviewDark() {
    AMThemeRoot {
        DatePickerRowPreviewContent()
    }
}
