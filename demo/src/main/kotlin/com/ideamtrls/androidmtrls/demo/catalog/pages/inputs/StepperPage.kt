package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMStepper
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val StepperEntry = ComponentEntry(
    id = "stepper",
    category = CatalogCategory.Inputs,
    title = "Stepper",
    summary = "Row stepper for whole numbers",
    icon = R.drawable.ic_exposure,
    keywords = listOf("AMStepper", "count", "quantity", "guests", "increment", "decrement", "plus", "minus"),
) { StepperPage() }

@Composable
private fun StepperPage() {
    ComponentPage(
        StepperEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { GuestStepper() },
    ) {
        RoomBooking()
    }
}

/** A guest count on a surface strip. */
@Composable
private fun GuestStepper() {
    var guests by rememberSaveable { mutableIntStateOf(2) }
    AMStepper(
        "Guests",
        guests,
        onValueChange = { guests = it },
        valueRange = 1..12,
        modifier = Modifier.background(AMTheme.Surface.primary),
    )
}

/** A room booking: adults, children and rooms, then breakfasts with the count in the label. */
@Composable
private fun RoomBooking() {
    var adults by rememberSaveable { mutableIntStateOf(2) }
    var children by rememberSaveable { mutableIntStateOf(0) }
    var rooms by rememberSaveable { mutableIntStateOf(1) }
    var breakfasts by rememberSaveable { mutableIntStateOf(2) }
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        AMStepper("Adults", adults, onValueChange = { adults = it }, valueRange = 1..8)
        AMDivider(inset = inset)
        AMStepper("Children", children, onValueChange = { children = it }, valueRange = 0..6)
        AMDivider(inset = inset)
        AMStepper("Rooms", rooms, onValueChange = { rooms = it }, valueRange = 1..4)
        AMDivider(inset = inset)
        AMStepper(
            breakfasts,
            onValueChange = { breakfasts = it },
            valueRange = 0..10,
            contentDescription = "Breakfasts",
        ) {
            Icon(painterResource(R.drawable.ic_coffee), contentDescription = null)
            Text("Breakfast × $breakfasts", Modifier.clearAndSetSemantics {})
        }
    }
}
