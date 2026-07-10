package io.github.jmsdnns.androidmtrls.demo.pages.textinputs

import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry

/**
 * All Text Inputs-section entries in curated display order (mirrors the iOS
 * catalog's ordering: Text Field, Secure Field, Text Editor, Search Field,
 * Field Label).
 *
 * Each component page lives in its own file in this package and exposes a
 * single `val <component>Entry`; this list is the only aggregation point.
 */
val textInputsEntries: List<ComponentEntry> = listOf(
    textFieldEntry,
    secureFieldEntry,
    textEditorEntry,
    searchFieldEntry,
    fieldLabelEntry,
)
