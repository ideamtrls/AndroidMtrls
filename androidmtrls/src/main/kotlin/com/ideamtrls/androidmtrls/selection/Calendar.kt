package com.ideamtrls.androidmtrls

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.min

/**
 * One month at a glance: a weekday header over a grid of day numbers. Today's number sits in a
 * solid `primary` circle, the selected day in a `secondaryContainer` circle, and each day shows up
 * to three event dots under its number. A tap selects a day.
 *
 * ```kotlin
 * var day by rememberSaveable { mutableStateOf(LocalDate.now()) }
 * AMCalendar(YearMonth.from(day), selected = day, onSelectedChange = { day = it }, markers = { events.count(it) })
 * ```
 *
 * - **The month is the caller's.** The calendar draws no month title and no paging controls; a
 *   screen pages months by passing another [month] (from its own header, a menu or the remote).
 *   Days outside the month are blank, so the grid is four to six weeks tall, and a [selected] day
 *   outside the month shows no highlight.
 * - **Days.** Each is a 48 dp target taking the tap, with Material's ripple on its circle. A
 *   selected today shows today's circle alone. [markers] gives a day's event count: drawn as up to
 *   three `primary` dots in three fixed slots, so numbers line up across rows; spoken in full.
 * - **Holds.** With [onHold], pressing and holding a day calls it with that day, with the
 *   long-press haptic, and doesn't select it: the trigger for a day's [AMSpotlight]. A tap still
 *   selects, and a drag that starts on a day still scrolls. TalkBack users get it as the day's
 *   long-press action.
 * - **TalkBack** reads each day as its full date, then "Today" and its event count where they
 *   apply, and whether it's selected.
 * - **Type**: day numbers in `bodyLarge` with tabular digits, today's at medium weight; weekday
 *   names in `bodySmall`, `onSurfaceVariant`. The circles are fixed, so both grow with the font
 *   scale up to 130% and stop there, where a two-digit day still fits its circle.
 * - **Locale**: weekday names, the first day of the week and the spoken dates follow the
 *   configuration's locale. Today is the device's clock.
 *
 * Like the other flat widgets it draws no surface and pads itself (`spacing.lg` at the sides,
 * `spacing.md` above and below), so it goes straight on a surface strip or in a card with no
 * padding of its own.
 *
 * Disabled (its own [enabled], or a disabling container around it), the days grey and ignore taps
 * and holds.
 *
 * @param month the month to show.
 * @param selected the selected day.
 * @param onSelectedChange called with the day a tap selects.
 * @param modifier the calendar's placement. It fills the width it's given.
 * @param markers a day's event count, drawn as up to three dots. It runs for every day each time
 *   the calendar recomposes, so keep it cheap. No dots by default.
 * @param onHold called with a day that's pressed and held, instead of selecting it. Null (the
 *   default) leaves days tap-only.
 * @param enabled whether the days take taps and holds.
 */
@Composable
public fun AMCalendar(
    month: YearMonth,
    selected: LocalDate,
    onSelectedChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    markers: (day: LocalDate) -> Int = { 0 },
    onHold: ((day: LocalDate) -> Unit)? = null,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    val spacing = AMTheme.tokens.spacing
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val cells = remember(month, firstDay) { calendarMonthCells(month, firstDay) }
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale) }
    val today = LocalDate.now()
    val numberStyle = MaterialTheme.typography.bodyLarge.capped().copy(fontFeatureSettings = "tnum")
    val weekdayStyle = MaterialTheme.typography.bodySmall.capped()

    Column(
        modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        WeekdayHeader(firstDay, locale, weekdayStyle)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            cells.chunked(DaysInWeek).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        if (day == null) {
                            Spacer(Modifier.weight(1f).height(DayCellHeight))
                        } else {
                            DayCell(
                                day = day,
                                isToday = day == today,
                                isSelected = day == selected,
                                events = markers(day),
                                description = dateFormat.format(day),
                                numberStyle = numberStyle,
                                enabled = isEnabled,
                                onClick = { if (day != selected) onSelectedChange(day) },
                                onHold = onHold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The days of [month] laid out as whole weeks starting on [firstDayOfWeek]: nulls before the 1st
 * back to the week's start, then one date per day, then nulls to the end of the last week. Its
 * size is always a multiple of seven: four to six weeks.
 */
internal fun calendarMonthCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDayOfWeek.value + DaysInWeek) % DaysInWeek
    val cells = ArrayList<LocalDate?>(6 * DaysInWeek)
    repeat(leading) { cells.add(null) }
    for (day in 1..month.lengthOfMonth()) cells.add(month.atDay(day))
    while (cells.size % DaysInWeek != 0) cells.add(null)
    return cells
}

/** The weekday names over the grid's columns, starting on [firstDay]. */
@Composable
private fun WeekdayHeader(firstDay: DayOfWeek, locale: Locale, style: TextStyle) {
    Row(Modifier.fillMaxWidth()) {
        repeat(DaysInWeek) { column ->
            val weekday = firstDay.plus(column.toLong())
            val spoken = weekday.getDisplayName(DateTextStyle.FULL, locale)
            Text(
                weekday.getDisplayName(DateTextStyle.SHORT, locale),
                modifier = Modifier.weight(1f).semantics { contentDescription = spoken },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip,
                maxLines = 1,
                style = style,
            )
        }
    }
}

/**
 * One day: its number in the circle that marks today or the selection, over three dot slots. The
 * whole cell takes the tap and the hold; the ripple shows on the circle.
 */
@Composable
private fun RowScope.DayCell(
    day: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    events: Int,
    description: String,
    numberStyle: TextStyle,
    enabled: Boolean,
    onClick: () -> Unit,
    onHold: ((LocalDate) -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val press = remember { MutableInteractionSource() }
    // One circle carries both states, so a selected today shows today's circle alone.
    val (container, content) = when {
        isToday -> scheme.primary to scheme.onPrimary
        isSelected -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> Color.Transparent to scheme.onSurface
    }
    val alpha = if (enabled) 1f else DisabledAlpha
    val spoken = buildList {
        add(description)
        if (isToday) add("Today")
        if (events > 0) add(if (events == 1) "1 event" else "$events events")
    }.joinToString(", ")
    Column(
        Modifier
            .weight(1f)
            .height(DayCellHeight)
            .minimumInteractiveComponentSize()
            .amHoldable(onHold = { onHold?.invoke(day) }, enabled = enabled && onHold != null)
            .selectable(
                selected = isSelected,
                enabled = enabled,
                interactionSource = press,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .clearAndSetSemantics {}
                .size(DayCircleSize)
                .clip(CircleShape)
                .background(container.copy(alpha = container.alpha * alpha))
                .indication(press, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${day.dayOfMonth}",
                color = content.copy(alpha = content.alpha * alpha),
                maxLines = 1,
                softWrap = false,
                style = if (isToday) numberStyle.copy(fontWeight = FontWeight.Medium) else numberStyle,
            )
        }
        Spacer(Modifier.height(DotGap))
        Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing)) {
            val shown = min(events, MaxDots)
            repeat(MaxDots) { index ->
                val dot = if (index < shown) scheme.primary.copy(alpha = alpha) else Color.Transparent
                Box(Modifier.size(DotSize).background(dot, CircleShape))
            }
        }
    }
}

/**
 * This style with its font size and line height stopped at [MaxTextScale] times their size at the
 * default font scale, so text in the fixed circles grows with the font scale only until it fills
 * them. Below the cap the platform's nonlinear scaling applies unchanged.
 */
@Composable
private fun TextStyle.capped(): TextStyle = with(LocalDensity.current) {
    val size = fontSize.toDp().coerceAtMost(fontSize.value.dp * MaxTextScale).toSp()
    val line = if (lineHeight.isSp) lineHeight.toDp().coerceAtMost(lineHeight.value.dp * MaxTextScale).toSp() else lineHeight
    copy(fontSize = size, lineHeight = line)
}

private const val DaysInWeek = 7

/** A day's height: the touch target. */
private val DayCellHeight = 48.dp

/** The circle around a day's number that marks today or the selection. */
private val DayCircleSize = 36.dp

private val DotSize = 4.dp

private val DotSpacing = 3.dp

/** Between a day's circle and its dots. */
private val DotGap = 2.dp

private const val MaxDots = 3

/**
 * The largest font scale the grid's text follows: a two-digit day in `bodyLarge` still clears its
 * 36 dp circle by about 6 dp a side there.
 */
private const val MaxTextScale = 1.3f

/** Material's alpha for disabled content. */
private const val DisabledAlpha = 0.38f
