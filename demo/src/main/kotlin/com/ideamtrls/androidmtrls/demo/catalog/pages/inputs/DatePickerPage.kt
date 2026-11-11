package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMDatePicker
import com.ideamtrls.androidmtrls.AMDatePickerComponents
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal val DatePickerEntry = ComponentEntry(
    id = "datePicker",
    category = CatalogCategory.Inputs,
    title = "Date picker",
    summary = "Row with date and time chips",
    icon = R.drawable.ic_calendar_clock,
    keywords = listOf(
        "AMDatePicker", "AMDatePickerComponents", "DatePicker", "TimeInput", "TimePicker", "date", "time",
        "calendar", "clock", "dial", "chip", "spotlight",
    ),
) { DatePickerPage() }

@Composable
private fun DatePickerPage() {
    ComponentPage(
        DatePickerEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { StartsPicker() },
    ) {
        TripDates()
    }
}

/** A date and time row, set to now ([LaunchOptions.clock]), on a surface strip. */
@Composable
private fun StartsPicker() {
    var starts by rememberSaveable { mutableStateOf(LaunchOptions.clock().withSecond(0).withNano(0)) }
    AMDatePicker("Starts", starts, onValueChange = { starts = it }, modifier = Modifier.background(AMTheme.Surface.primary))
}

/**
 * A trip's dates: departure from today on, a return day no earlier than it, a wake-up time, and a
 * booking time that can't change.
 */
@Composable
private fun TripDates() {
    var departs by rememberSaveable { mutableStateOf(tripDay(1, 9, 0)) }
    var returns by rememberSaveable { mutableStateOf(tripDay(5, 18, 0)) }
    var alarm by rememberSaveable { mutableStateOf(tripDay(1, 6, 30)) }
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        AMDatePicker(
            "Departs",
            departs,
            onValueChange = { departs = it },
            dateRange = LocalDate.now()..LocalDate.MAX,
        )
        AMDivider(inset = inset)
        AMDatePicker(
            "Returns",
            returns,
            onValueChange = { returns = it },
            components = AMDatePickerComponents.Date,
            dateRange = departs.toLocalDate()..LocalDate.MAX,
        )
        AMDivider(inset = inset)
        AMDatePicker(
            "Wake-up alarm",
            alarm,
            onValueChange = { alarm = it },
            components = AMDatePickerComponents.Time,
        )
        AMDivider(inset = inset)
        AMDatePicker("Booked", tripDay(-7, 20, 15), onValueChange = {}, enabled = false)
    }
}

/** A day [offset] days from today, at [hour]:[minute]. */
private fun tripDay(offset: Long, hour: Int, minute: Int): LocalDateTime =
    LocalDateTime.of(LocalDate.now().plusDays(offset), LocalTime.of(hour, minute))
