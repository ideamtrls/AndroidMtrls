package io.github.jmsdnns.androidmtrls.demo.pages.pickers

import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry

/**
 * All Pickers-section entries in iOS catalog order (`PickersPages.entries` in
 * iOSMtrls `Demo/Sources/Catalog/Pages/Pickers.swift`): Toggle, Segmented
 * Control, Menu Picker, Date Picker, Color Picker, Slider, Stepper, Selection
 * Check.
 *
 * Each component page lives in its own file in this package and exposes a
 * single `val <component>Entry`; this list is the only aggregation point.
 */
val pickersEntries: List<ComponentEntry> = listOf(
    toggleEntry,
    segmentedControlEntry,
    menuPickerEntry,
    datePickerEntry,
    colorPickerEntry,
    sliderEntry,
    stepperEntry,
    selectionCheckEntry,
)
