package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMSkeleton
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amShimmer

internal val SkeletonEntry = ComponentEntry(
    id = "skeleton",
    category = CatalogCategory.Feedback,
    title = "Skeleton",
    summary = "Shimmering placeholders while content loads",
    icon = R.drawable.ic_segment,
    keywords = listOf(
        "AMSkeleton", "amShimmer", "Modifier.amShimmer", "shimmer", "placeholder", "loading",
        "skeleton screen",
    ),
) { SkeletonPage() }

@Composable
private fun SkeletonPage() {
    ComponentPage(
        SkeletonEntry,
        previewLayout = StageLayout.FullBleed,
        preview = {
            SkeletonContact(
                Modifier
                    .background(AMTheme.Surface.primary)
                    .padding(AMTheme.tokens.spacing.lg),
            )
        },
    ) {
        SkeletonPost()
    }
}

/**
 * A contact row while it loads: the person's avatar, shimmering, beside the name and the role
 * held in place by their text placeholders, each bar as long as its words.
 */
@Composable
private fun SkeletonContact(modifier: Modifier = Modifier) {
    val spacing = AMTheme.tokens.spacing
    val typography = MaterialTheme.typography
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AMAvatar(url = null, name = "Ada Lovelace", modifier = Modifier.amShimmer())
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            AMSkeleton.Text("Ada Lovelace", style = typography.bodyLarge)
            AMSkeleton.Text("Mathematician and writer", style = typography.bodyMedium)
        }
    }
}

/** A post card while it loads: its author, its photo and its text. */
@Composable
private fun SkeletonPost() {
    val spacing = AMTheme.tokens.spacing
    Column(
        Modifier
            .fillMaxWidth()
            .amCard(padding = PaddingValues(spacing.lg)),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        SkeletonContact()
        AMSkeleton.Rect(height = 160.dp, shape = MaterialTheme.shapes.medium)
        AMSkeleton.Text(lines = 3)
    }
}
