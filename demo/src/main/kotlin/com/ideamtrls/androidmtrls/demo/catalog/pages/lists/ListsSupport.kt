package com.ideamtrls.androidmtrls.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

// Helpers the Lists pages share.

/**
 * A list the user reorders and trims, starting as [all]. It's saved as its items' ids, so the
 * order survives leaving the page and coming back.
 */
@Composable
internal fun <T : Any> rememberListsItems(all: List<T>, id: (T) -> Int): SnapshotStateList<T> =
    rememberSaveable(
        saver = listSaver(
            save = { items -> items.map(id) },
            restore = { ids -> ids.mapNotNull { saved -> all.firstOrNull { id(it) == saved } }.toMutableStateList() },
        ),
    ) {
        all.toMutableStateList()
    }
