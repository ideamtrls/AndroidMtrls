package com.ideamtrls.androidmtrls.demo

import android.icu.text.ListFormatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMFlowLayout
import com.ideamtrls.androidmtrls.AMLabeledContent
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/**
 * The signed-in landing view: who is signed in (avatar, name, email and the topics they follow)
 * with "Edit profile" under the card, then this month's reading and what was read last. The
 * light/dark chip sits at the end of its header.
 */
@Composable
internal fun AccountProfile(model: AccountModel) {
    AccountPage("Profile", actions = { SceneAppearanceButton() }) {
        AccountProfileCard(
            name = model.name.text.toString(),
            email = model.email.text.toString(),
            interests = model.orderedInterests,
        )
        AMButton(
            "Edit profile",
            onClick = { model.showsEditProfile = true },
            modifier = Modifier.fillMaxWidth(),
            style = AMButtonStyle.Secondary(),
        )
        AMSection("This month", style = AMSectionStyle.Rows, inset = false) {
            AMLabeledContent("Saved", "128")
            AMLabeledContent("Read", "42")
            AMLabeledContent("Highlights", "17")
        }
        // Nothing opens an article, so the rows take no taps.
        AMSection("Recently read", style = AMSectionStyle.Rows, inset = false) {
            AccountArticle.Recent.forEach { article ->
                AMListRow(article.title, subtitle = article.byline)
            }
        }
    }
}

/** The profile as one card: the avatar beside the name and email, then the interests as tags. */
@Composable
private fun AccountProfileCard(name: String, email: String, interests: List<AccountInterest>) {
    val spacing = AMTheme.tokens.spacing
    Column(
        Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
            // TalkBack skips the avatar: the name beside it reads the same thing.
            AMAvatar(url = null, name = name, modifier = Modifier.clearAndSetSemantics {}, size = AMAvatarSize.Large)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(
                    email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (interests.isEmpty()) {
            Text(
                "No interests yet. Pick a few and Margin will suggest more to read.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // The tags read as one element, "Interests" and their names.
            val names = ListFormatter.getInstance().format(interests.map { it.name })
            AMFlowLayout(Modifier.clearAndSetSemantics { contentDescription = "Interests: $names" }) {
                interests.forEach { interest ->
                    AMTag(interest.name, icon = painterResource(interest.icon))
                }
            }
        }
    }
}

/** An article in the reading list. */
private class AccountArticle(val title: String, val source: String, val minutes: Int) {
    /** "The Quiet Web · 6 min" */
    val byline: String get() = "$source · $minutes min"

    companion object {
        /** The last four articles read, newest first. */
        val Recent = listOf(
            AccountArticle("A Field Guide to Reading Slowly", "The Quiet Web", 6),
            AccountArticle("What the Deep Sea Sounds Like", "Field & Tide", 9),
            AccountArticle("The Islands That Were Never There", "Paper Atlas", 12),
            AccountArticle("Notes on Keeping a Commonplace Book", "Longhand", 5),
        )
    }
}

/**
 * Edits a copy of the profile in the profile form, under the sheet's "Edit profile" title. "Save"
 * commits it; closing the sheet any other way leaves the profile as it was.
 */
@Composable
internal fun AccountEditProfileSheet(model: AccountModel) {
    val name = rememberTextFieldState(model.name.text.toString())
    var interests by rememberSaveable(stateSaver = AccountInterestsSaver) { mutableStateOf(model.interests) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(AMTheme.tokens.spacing.lg)) {
        AccountSetupForm(
            name = name,
            interests = interests,
            onInterestsChange = { interests = it },
            actionTitle = "Save",
            onSave = { model.saveProfile(name.text.toString(), interests) },
        )
    }
}
