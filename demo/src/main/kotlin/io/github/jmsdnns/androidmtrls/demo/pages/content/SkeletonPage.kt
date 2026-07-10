package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.content.AMSkeleton
import io.github.jmsdnns.androidmtrls.content.amShimmer
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// Skeleton catalog entry
// ---------------------------------------------------------------------------

val skeletonEntry: ComponentEntry = ComponentEntry(
    id = "content-skeleton",
    section = ComponentSection.Content,
    title = "Skeleton",
    subtitle = "Shimmer loading placeholders",
    intro = "Shimmer placeholders for content that hasn't loaded yet. The AMSkeleton primitives " +
        "(circle, rect, multi-line text) each carry the shared sweep; Modifier.amShimmer() runs " +
        "that same sweep over ANY content, masked to what it draws. All share a 1.2s left-to-right " +
        "gradient, and render static under Reduce Motion.",
    icon = Icons.Default.ViewStream,
    content = { SkeletonPage() },
)

// ---------------------------------------------------------------------------
// SkeletonPage — demo composable
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMSkeleton] and [amShimmer].
 *
 * - "Row & text" — the [AMSkeleton] primitives (circle + rect lines, text block).
 * - "Redacted" — a real specimen (avatar + name + body) inside an [AMCard], put
 *   under a single [amShimmer] sweep. The Compose stand-in for iOS
 *   `realView.redacted(reason: .placeholder).imShimmer()`: shimmer the finished
 *   layout instead of hand-maintaining a skeleton twin.
 */
@Composable
fun SkeletonPage() {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current
    val textPrimary = AMTheme.Text.primary.resolve(isDark)

    catalogGroup(title = "Row & text") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            // Card 1: circle 40dp + two rect lines
            AMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = AMPadding.block,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
                ) {
                    AMSkeleton.circle(diameter = 40.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                    ) {
                        AMSkeleton.rect(height = 14.dp)
                        AMSkeleton.rect(height = 10.dp)
                    }
                }
            }

            // Card 2: text lines = 3
            AMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = AMPadding.block,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AMSkeleton.text(lines = 3)
                }
            }
        }
    }

    // Preferred pattern: shimmer the REAL layout — render it and sweep it —
    // instead of hand-maintaining a skeleton twin. One amShimmer sweep travels
    // across the whole card (fill + specimen) at once.
    catalogGroup(title = "Redacted") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp)
                .amShimmer(),
            padding = AMPadding.block,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AMAvatar(url = null, initials = "AL", size = AMAvatarSize.MD)
                Column(
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                ) {
                    BasicText(
                        text = "Ada Lovelace",
                        style = AMTheme.Type.secondary.toTextStyle(textPrimary)
                            .copy(fontWeight = FontWeight.SemiBold),
                    )
                    BasicText(
                        text = "Wrote the first algorithm intended for a machine.",
                        style = AMTheme.Type.secondary.toTextStyle(textPrimary),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
