package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMChipGroup
import com.ideamtrls.androidmtrls.AMFormActions
import com.ideamtrls.androidmtrls.AMProgressView
import com.ideamtrls.androidmtrls.AMProgressViewStyle
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMTextField
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/**
 * Profile setup, the step after a new account's code: where setup stands, over the profile form,
 * whose "Finish" signs the new account in.
 */
@Composable
internal fun AccountSetup(model: AccountModel) {
    AccountStepPage(title = "Set up your profile", onBack = model::goBack) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl)) {
            // The profile is the last of the three create-account steps (account details, code,
            // profile), with the first two done.
            Box(Modifier.fillMaxWidth().amCard()) {
                AMProgressView(
                    "Last step",
                    progress = { 2f / 3f },
                    valueLabel = "Step 3 of 3",
                    style = AMProgressViewStyle.Bar,
                )
            }
            AccountSetupForm(
                name = model.name,
                interests = model.interests,
                onInterestsChange = { model.interests = it },
                actionTitle = "Finish",
                onSave = model::finishSetup,
            )
        }
    }
}

/**
 * The profile form: the avatar's live initials beside the name field, the interest chips, and one
 * main button, disabled while the name is blank. Setup finishes with it ("Finish"); the "Edit
 * profile" sheet saves a draft with it ("Save"). The button trims the name before [onSave].
 */
@Composable
internal fun AccountSetupForm(
    name: TextFieldState,
    interests: Set<String>,
    onInterestsChange: (Set<String>) -> Unit,
    actionTitle: String,
    onSave: () -> Unit,
) {
    val spacing = AMTheme.tokens.spacing
    val hasName = name.text.isNotBlank()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xl)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
            // TalkBack skips the avatar: the field beside it already reads the name.
            AMAvatar(
                url = null,
                name = name.text.toString(),
                modifier = Modifier.clearAndSetSemantics {},
                size = AMAvatarSize.Large,
            )
            Box(Modifier.weight(1f).amCard(padding = PaddingValues(spacing.lg))) {
                AMTextField(
                    name,
                    label = "Your name",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            AMSectionHeader("Interests")
            AMChipGroup(
                items = AccountInterest.All,
                label = { it.name },
                selection = AccountInterest.All.filterTo(mutableSetOf()) { it.name in interests },
                onSelectionChange = { picked -> onInterestsChange(picked.mapTo(mutableSetOf()) { it.name }) },
                icon = { painterResource(it.icon) },
            )
        }
        AMFormActions {
            AMButton(
                actionTitle,
                onClick = {
                    name.setTextAndPlaceCursorAtEnd(name.text.trim().toString())
                    onSave()
                },
                enabled = hasName,
            )
        }
    }
}

/** A topic a reader can follow. Lists of chosen interests follow [All]'s order. */
@Immutable
internal class AccountInterest(val name: String, @param:DrawableRes val icon: Int) {
    companion object {
        val All: List<AccountInterest> = listOf(
            AccountInterest("Technology", R.drawable.ic_memory),
            AccountInterest("Science", R.drawable.ic_science),
            AccountInterest("History", R.drawable.ic_account_balance),
            AccountInterest("Design", R.drawable.ic_palette),
            AccountInterest("Business", R.drawable.ic_monitoring),
            AccountInterest("Books", R.drawable.ic_menu_book),
            AccountInterest("Food", R.drawable.ic_restaurant),
            AccountInterest("Travel", R.drawable.ic_flight),
        )
    }
}

/** Saves a set of interests as a list of their names. */
internal val AccountInterestsSaver = Saver<Set<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toSet() },
)
