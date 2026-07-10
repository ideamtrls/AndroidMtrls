package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMRowContent
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMSemantic
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// LabeledContent catalog entry
// ---------------------------------------------------------------------------

val labeledContentEntry: ComponentEntry = ComponentEntry(
    id = "content-labeled-content",
    section = ComponentSection.Content,
    title = "Labeled Content",
    tileTitle = "Labeled",
    subtitle = "Label/value row",
    intro = "Flat row for label/value pairs. The non-tappable cousin of AMListRow — " +
        "applied via AMRowContent with label/value display.",
    icon = Icons.Default.TableRows,
    content = { LabeledContentPage() },
)

// ---------------------------------------------------------------------------
// LabeledContentPage — demo composable
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMRowContent].
 *
 * One group "Label / value rows" with three stacked rows in a single surface
 * with no between-row dividers, mirroring iOS `LabeledContentRowPage` from
 * `Demo/Sources/Catalog/Pages/Content.swift` (a `VStack(spacing: 0)` of `.imRow`
 * rows): Version / Build / Account. The Account value is tinted (`semantic.info`
 * blue) to match iOS, where SwiftUI's automatic link detection renders the email
 * in the system accent.
 */
@Composable
fun LabeledContentPage() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    catalogGroup(title = "Label / value rows") {
        catalogFlatDemo(padding = 0.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AMRowContent(label = "Version", value = "1.0.0")
                AMRowContent(label = "Build", value = "2026.05.19")
                // Slot overload for the Account row — value is a link-tinted email
                AMRowContent(
                    label = {
                        BasicText(
                            text = "Account",
                            style = TextStyle(
                                color = AMTheme.Text.primary.resolve(isDark),
                            ),
                        )
                    },
                    content = {
                        BasicText(
                            text = "jdennis@gmail.com",
                            style = TextStyle(
                                color = AMSemantic.Info.color(tokens.semantic, isDark),
                            ),
                        )
                    },
                )
            }
        }
    }
}
