package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMActionGrid
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMAlert
import com.ideamtrls.androidmtrls.AMAlertAction
import com.ideamtrls.androidmtrls.AMAlertItem
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMFloatingActions
import com.ideamtrls.androidmtrls.AMScene
import com.ideamtrls.androidmtrls.AMToast
import com.ideamtrls.androidmtrls.amInstantSwitch
import com.ideamtrls.androidmtrls.amPageBackdrop
import java.time.YearMonth

internal val CalendarSceneEntry = ComponentEntry(
    id = "calendarScene",
    category = CatalogCategory.Showcase,
    title = "Calendar",
    summary = "A month calendar with a day's events",
    icon = R.drawable.ic_calendar_month,
    keywords = listOf(
        "AMScene", "AMCalendar", "AMTimeline", "AMSpotlight", "AMDatePicker", "AMSheet", "AMMenuPicker", "events",
        "month", "agenda", "day peek", "hold", "event form", "toast", "empty state", "scene",
    ),
) { CalendarScene() }

/**
 * Room every Calendar view leaves under its last content, so it scrolls clear of the floating
 * action button: the button's 56 dp, with `spacing.lg` under it and the same over it.
 */
internal val CalendarFabClearance = 88.dp

/**
 * Calendar: one month at a time with the selected day's agenda under it, and a months view the
 * remote swaps in, the two roots of the [AMScene]. Nothing pushes. One [CalendarModel] owns the
 * events and every overlay. The scene lies on a picture of the month's weather (the month on show
 * in the month view, this month in the months view), which its frosted cards and its frosted event
 * sheet blur.
 */
@Composable
internal fun CalendarScene() {
    val model = rememberSaveable(saver = CalendarModel.Saver) { CalendarModel() }
    val navigator = LocalCatalogNavigator.current
    var showsRemote by rememberSaveable { mutableStateOf(false) }

    AMBackdrop(
        art = {
            val month = if (model.currentView == CalendarView.Month) model.displayedMonth else YearMonth.now()
            Box(Modifier.fillMaxSize().amPageBackdrop(calendarMonthArt(month)))
        },
        Modifier.fillMaxSize(),
    ) {
        AMScene(
            current = model.currentView,
            landing = CalendarView.Month,
            onCurrentChange = { model.currentView = it },
            containerColor = Color.Transparent,
            floatingActions = {
                AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }, contentDescription = "Actions") {
                    CalendarRemote(
                        model,
                        onClose = { showsRemote = false },
                        onExit = {
                            showsRemote = false
                            navigator.back()
                        },
                    )
                }
            },
            overlays = { CalendarOverlays(model) },
        ) { root ->
            when (root) {
                CalendarView.Month -> CalendarMonthView(model)
                CalendarView.Months -> CalendarMonthsView(model)
            }
        }
    }
}

/**
 * The remote's menu: Add event and Today over Month, Years and Exit, the view showing ringed. Add
 * event opens the form on the selected day; Today sets the month view to this month with today
 * selected, greyed while it already is.
 */
@Composable
private fun CalendarRemote(model: CalendarModel, onClose: () -> Unit, onExit: () -> Unit) {
    // The menu closes outside the switch, so its fade plays over the new view.
    fun show(view: CalendarView) {
        onClose()
        amInstantSwitch { model.currentView = view }
    }
    AMActionGrid {
        AMActionGridRow(
            {
                AMActionGridButton(painterResource(R.drawable.ic_calendar_add_on), "Add event", onClick = {
                    onClose()
                    model.newEvent(model.selectedDay)
                })
            },
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_radio_button_checked), "Today",
                    onClick = {
                        model.showToday()
                        onClose()
                    },
                    enabled = !model.isViewingToday,
                )
            },
        )
        AMActionGridRow(
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_calendar_month), "Month",
                    onClick = { show(CalendarView.Month) },
                    active = model.currentView == CalendarView.Month,
                )
            },
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_calendar_clock), "Years",
                    onClick = { show(CalendarView.Months) },
                    active = model.currentView == CalendarView.Months,
                )
            },
            { AMActionGridButton(painterResource(R.drawable.ic_logout), "Exit", onClick = onExit) },
        )
    }
}

/**
 * Everything the scene presents over its views, whichever view is showing: the event form, the
 * delete confirmation over it, the day peek, and the toasts ("Event added", "Event updated", "Event
 * deleted"), which rise above the floating action button. It lives in the scene's overlays slot,
 * outside both roots, so a swap of view leaves an open overlay up.
 */
@Composable
private fun CalendarOverlays(model: CalendarModel) {
    CalendarEventSheet(model)

    // After the sheet, so it opens over it. Cancel leaves the form open.
    val title = model.draftStoredTitle
    val deleteAlert = remember(model, title) {
        AMAlertItem(
            "Delete event?",
            confirm = AMAlertAction("Delete", destructive = true, onClick = model::delete),
            message = "“$title” will be removed from your calendar.",
            cancel = AMAlertAction("Cancel"),
        )
    }
    AMAlert(deleteAlert.takeIf { model.confirmsDelete }, onDismissRequest = { model.confirmsDelete = false })

    CalendarDayPeek(model)
    AMToast(model.toast, onDismissRequest = { model.toast = null })
}
