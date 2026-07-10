package io.github.jmsdnns.androidmtrls.demo.pages.buttons

import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry

/**
 * All Buttons-section entries in curated display order (mirrors the iOS
 * catalog's Buttons ordering: Button Styles, Form Actions, Icon Button,
 * Card Button, Action Buttons, FAB).
 *
 * Each component page lives in its own file in this package and exposes a
 * single `val <component>Entry`; this list is the only aggregation point.
 */
val buttonsEntries: List<ComponentEntry> = listOf(
    buttonStylesEntry,
    formActionsEntry,
    iconButtonEntry,
    cardButtonEntry,
    actionGridButtonEntry,
    fabEntry,
)
