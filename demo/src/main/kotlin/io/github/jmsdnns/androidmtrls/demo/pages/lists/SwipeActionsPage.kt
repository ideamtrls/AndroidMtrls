package io.github.jmsdnns.androidmtrls.demo.pages.lists

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.lists.AMSwipeAction
import io.github.jmsdnns.androidmtrls.lists.AMSwipeActionRole
import io.github.jmsdnns.androidmtrls.lists.AMSwipeRow
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Swipe Actions catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMSwipeRow].
 *
 * id: "lists-swipe", section: [ComponentSection.Lists]. tileTitle "Swipe".
 */
val swipeActionsEntry: ComponentEntry = ComponentEntry(
    id = "lists-swipe",
    section = ComponentSection.Lists,
    title = "Swipeable Row",
    tileTitle = "Swipe",
    subtitle = "Swipe to reveal actions",
    intro = "Wraps a flat list row with iOS-style trailing swipe actions. Drag a row toward the " +
        "trailing edge to reveal buttons behind it; release past halfway (or flick) snaps it " +
        "open, otherwise it springs shut. The primitive owns the drag, overdrag, and snap so " +
        "AMListRow gets swipe without a list.",
    icon = Icons.Default.Swipe,
    content = { SwipeActionsPage() },
)

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

private data class Mail(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
)

// ---------------------------------------------------------------------------
// SwipeActionsPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMSwipeRow].
 *
 * A single **Default** group: an inbox of four flat [AMListRow]s on a
 * `catalogFlatDemo` strip, each with a leading tinted icon and wrapped in
 * [AMSwipeRow] with an **Archive** action (indigo tint) and a **Delete** action
 * ([AMSwipeActionRole.Destructive], red default fill). The actions are
 * non-destructive — they only echo the last action into a status caption above
 * the rows rather than removing anything.
 */
@Composable
fun SwipeActionsPage() {

    val isDark = LocalAMIsDarkTheme.current

    // Leading-icon tints — iOS system palette. Blue/green resolve the adaptive
    // semantic tokens; indigo/orange are the fixed iOS system hues.
    val blue = AMTheme.Semantic.info.resolve(isDark)
    val green = AMTheme.Semantic.success.resolve(isDark)
    val indigo = Color(0xFF5856D6)
    val orange = Color(0xFFFF9500)

    val messages = listOf(
        Mail(1, "Weekly digest", "3 new highlights", Icons.Default.Email, blue),
        Mail(2, "Build succeeded", "AndroidMtrls · 2m ago", Icons.Default.Build, green),
        Mail(3, "Grace commented", "On “Cache policies”", Icons.Default.ChatBubble, indigo),
        Mail(4, "Storage almost full", "1.2 GB remaining", Icons.Default.Storage, orange),
    )

    var status by remember {
        mutableStateOf("Swipe a row toward the trailing edge to reveal actions.")
    }

    // ── Default ──────────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {

        // Non-destructive status echo — the last action lands here, no row is
        // removed.
        BasicText(
            text = status,
            style = TextStyle(
                color = AMTheme.Text.secondary.resolve(isDark),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        )

        Spacer(modifier = Modifier.height(AMTheme.Spacing.sm.dp))

        catalogFlatDemo(padding = 0.dp) {
            Column {
                messages.forEach { mail ->
                    AMSwipeRow(
                        trailingActions = listOf(
                            // Normal role, indigo tint override.
                            AMSwipeAction(
                                icon = Icons.Default.Archive,
                                label = "Archive",
                                tint = indigo,
                                contentDescription = "Archive ${mail.title}",
                                onClick = { status = "Last action: Archived “${mail.title}”" },
                            ),
                            // Destructive role — red default fill.
                            AMSwipeAction(
                                icon = Icons.Default.Delete,
                                label = "Delete",
                                role = AMSwipeActionRole.Destructive,
                                contentDescription = "Delete ${mail.title}",
                                onClick = { status = "Last action: Deleted “${mail.title}”" },
                            ),
                        ),
                    ) {
                        AMListRow(
                            title = mail.title,
                            subtitle = mail.subtitle,
                            leading = {
                                Image(
                                    imageVector = mail.icon,
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(mail.tint),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
