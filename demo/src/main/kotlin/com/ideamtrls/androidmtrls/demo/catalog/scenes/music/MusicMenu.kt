package com.ideamtrls.androidmtrls.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMActionGrid
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow

/**
 * The remote's menu: the controls card over one 4-up row, Artists, Player, Queue, Exit, the root
 * view showing ringed. The card is the Player's own [MusicControlsCard]; as the grid's first row
 * its bottom edge lands where [musicControlsPlacement] puts it on the Player, so the controls don't
 * move when the menu opens there, and they work without closing it. Artists and Player swap the
 * root view, and the Artists stack keeps its place while the Player shows. Queue greys with
 * nothing in the queue.
 */
@Composable
internal fun MusicMenu(model: MusicModel, onExit: () -> Unit) {
    AMActionGrid {
        MusicControlsCard(model)
        AMActionGridRow(
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_artist), "Artists",
                    onClick = { model.switchTo(MusicRootView.Artists) },
                    active = model.rootView == MusicRootView.Artists,
                )
            },
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_play_circle), "Player",
                    onClick = { model.switchTo(MusicRootView.Player) },
                    active = model.rootView == MusicRootView.Player,
                )
            },
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_queue_music), "Queue",
                    onClick = model::openQueue,
                    enabled = model.currentTrack != null,
                )
            },
            { AMActionGridButton(painterResource(R.drawable.ic_logout), "Exit", onClick = onExit) },
        )
    }
}
