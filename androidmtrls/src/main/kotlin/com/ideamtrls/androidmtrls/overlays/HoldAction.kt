package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

/**
 * One thing to do with a held item: a tile in its [AMSpotlight], and the same action for TalkBack
 * on the item itself ([Modifier.amHoldable][amHoldable]). A payload, like [AMSwipeAction]: what it
 * says, its glyph, its role and what it does.
 *
 * ```kotlin
 * AMHoldAction("Queue", painterResource(R.drawable.ic_playlist_add), contentDescription = "Add to queue") { addToQueue(track) }
 * AMHoldAction("Delete", painterResource(R.drawable.ic_delete), destructive = true) { delete(photo) }
 * ```
 *
 * @property label the tile's title, short enough for a third of the row.
 * @property icon the tile's glyph.
 * @property contentDescription what a screen reader says for the tile and the item's action, when
 *   the short [label] needs a fuller name ("Add to queue" for "Queue"). Null says [label].
 * @property destructive whether the action removes something: the tile takes
 *   [AMSemantic.Destructive].
 * @property onClick what the action does. A tile closes the spotlight first, then calls it.
 */
@Immutable
public class AMHoldAction(
    public val label: String,
    public val icon: Painter,
    public val contentDescription: String? = null,
    public val destructive: Boolean = false,
    public val onClick: () -> Unit,
)

/** What a screen reader calls [this] action. */
internal val AMHoldAction.spokenName: String get() = contentDescription ?: label

/**
 * [actions] as the spotlight's tiles: one [AMActionGridButton] each, in order, in the action
 * grid's equal columns, so they land where the remote's tiles do. A tile calls [onPicked] (closing
 * the spotlight), then its action. One action takes half the row, as one of two tiles would; there
 * are at most four.
 */
@Composable
internal fun HoldActionTiles(actions: List<AMHoldAction>, onPicked: () -> Unit, modifier: Modifier = Modifier) {
    require(actions.size <= 4) { "A held item offers at most four actions, as the remote's rows hold four tiles: ${actions.size}" }
    val tiles: List<@Composable () -> Unit> = actions.map { action ->
        {
            AMActionGridButton(
                action.icon,
                action.label,
                onClick = {
                    onPicked()
                    action.onClick()
                },
                contentDescription = action.contentDescription,
                destructive = action.destructive,
            )
        }
    }
    ActionTileRow(modifier, if (tiles.size == 1) tiles + {} else tiles)
}
