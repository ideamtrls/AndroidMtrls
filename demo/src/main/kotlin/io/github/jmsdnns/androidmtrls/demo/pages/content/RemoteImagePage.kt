package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.content.AMRemoteImage
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

val remoteImageEntry: ComponentEntry = ComponentEntry(
    id = "content-remote-image",
    section = ComponentSection.Content,
    title = "Remote Image",
    tileTitle = "Image",
    subtitle = "Async image with fallback",
    intro = "Async-loaded square image with a rounded shape and a fallback view for the " +
        "loading/error states. Goes through cache via AndroidMtrls.",
    icon = Icons.Default.Image,
    content = { RemoteImagePage() },
)

// ---------------------------------------------------------------------------
// RemoteImagePage — demo composable
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMRemoteImage].
 *
 * One group "Fallbacks" with iOS's three cases:
 * 1. nil url → default placeholder
 * 2. broken url → fallback system icon (music.note → MusicNote)
 * 3. nil url + custom fallback composable (globe → Language)
 *
 * Mirrors iOS `IMRemoteImagePage` from `Demo/Sources/Catalog/Pages/Content.swift`.
 */
@Composable
fun RemoteImagePage() {
    val isDark = LocalAMIsDarkTheme.current

    catalogGroup(title = "Fallbacks") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. nil url — default placeholder
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AMRemoteImage(
                    url = null,
                    size = 60.dp,
                    cornerRadius = AMTheme.Radius.sm.dp,
                    contentDescription = "nil url",
                )
                androidx.compose.foundation.text.BasicText(
                    text = "nil url",
                    style = androidx.compose.ui.text.TextStyle(
                        color = AMTheme.Text.secondary.resolve(isDark),
                        fontSize = 10.sp,
                    ),
                )
            }

            // 2. Broken url + fallback icon (music.note → MusicNote)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AMRemoteImage(
                    url = "https://example.com/missing.png",
                    size = 60.dp,
                    cornerRadius = AMTheme.Radius.sm.dp,
                    contentDescription = "fallback",
                    fallbackIcon = Icons.Default.MusicNote,
                )
                androidx.compose.foundation.text.BasicText(
                    text = "fallback",
                    style = androidx.compose.ui.text.TextStyle(
                        color = AMTheme.Text.secondary.resolve(isDark),
                        fontSize = 10.sp,
                    ),
                )
            }

            // 3. nil url + custom fallback composable (globe → Language)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AMRemoteImage(
                    url = null,
                    size = 60.dp,
                    cornerRadius = AMTheme.Radius.lg.dp,
                    contentDescription = "custom",
                    fallback = {
                        Image(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(AMTheme.Semantic.info.resolve(LocalAMIsDarkTheme.current)),
                            modifier = Modifier.size(30.dp),
                        )
                    },
                )
                androidx.compose.foundation.text.BasicText(
                    text = "custom",
                    style = androidx.compose.ui.text.TextStyle(
                        color = AMTheme.Text.secondary.resolve(isDark),
                        fontSize = 10.sp,
                    ),
                )
            }
        }
    }
}
