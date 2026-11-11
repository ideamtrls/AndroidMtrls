package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amStatusBarEdge
import java.time.LocalDate
import java.time.YearMonth

/**
 * The months view, the scene's second root: headerless, its years' section headers its only
 * titling. Three years back through three forward, each a header over a frosted card of its twelve
 * months with inset dividers between them, the month on show checked. A tap on a month shows it in
 * the month view with its 1st selected. It first opens at the shown month's year, a section's gap
 * under the status bar, and after that where it was left; rows scrolling up under the status bar
 * fade out there, so the month's picture shows behind the clock.
 */
@Composable
internal fun CalendarMonthsView(model: CalendarModel) {
    val spacing = AMTheme.tokens.spacing
    val formats = rememberCalendarFormats()
    val years = remember { CalendarDates.pickerYears(LocalDate.now()) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = years.indexOf(model.displayedMonth.year).coerceAtLeast(0))
    val statusBar = WindowInsets.statusBars.asPaddingValues()
    val navigationBar = WindowInsets.navigationBars.asPaddingValues()
    LazyColumn(
        Modifier.fillMaxSize().amStatusBarEdge(),
        state = listState,
        contentPadding = PaddingValues(
            start = spacing.lg,
            top = statusBar.calculateTopPadding(),
            end = spacing.lg,
            bottom = navigationBar.calculateBottomPadding() + CalendarFabClearance,
        ),
    ) {
        items(years, key = { it }) { year ->
            // The gap above each year is part of it, so a year opened at the top rests under it.
            AMSection(
                year.toString(),
                Modifier.padding(top = spacing.xl),
                style = AMSectionStyle.Rows,
                fill = AMSurfaceFill.Frosted,
                inset = false,
            ) {
                (1..12).forEach { monthNumber ->
                    val month = YearMonth.of(year, monthNumber)
                    if (monthNumber > 1) AMDivider(inset = spacing.lg)
                    CalendarMonthRow(
                        formats.monthName(month),
                        isShown = month == model.displayedMonth,
                        onClick = { model.pickMonth(month) },
                    )
                }
            }
        }
    }
}

/** One month: its name, and a check when it's the month on show. */
@Composable
private fun CalendarMonthRow(name: String, isShown: Boolean, onClick: () -> Unit) {
    AMListRow(
        name,
        Modifier.semantics { selected = isShown },
        trailingContent = if (isShown) {
            { Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
        } else {
            null
        },
        onClick = onClick,
    )
}
