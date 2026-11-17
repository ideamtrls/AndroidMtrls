package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.ideamtrls.androidmtrls.AMTextField
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.ProvideAMOnSurface
import com.ideamtrls.androidmtrls.amCard

internal val TextFieldEntry = ComponentEntry(
    id = "textField",
    category = CatalogCategory.Inputs,
    title = "Text field",
    summary = "Single-line input with a clear button",
    icon = R.drawable.ic_text_fields,
    keywords = listOf(
        "AMTextField", "AMTextFieldAutofocus", "OutlinedTextField", "TextFieldState", "input", "keyboard",
        "autofocus", "caret", "clear", "label", "focus", "next field", "ImeAction",
    ),
) { TextFieldPage() }

@Composable
private fun TextFieldPage() {
    ComponentPage(
        TextFieldEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { NameField() },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl)) {
            FrostedContactCard()
            ContactForm()
        }
    }
}

/** An empty name field on a surface strip. */
@Composable
private fun NameField() {
    val name = rememberTextFieldState()
    Box(Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(AMTheme.tokens.spacing.lg)) {
        AMTextField(name, label = "Full name", keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
    }
}

/** Name and email in a frosted card over a photo. */
@Composable
private fun FrostedContactCard() {
    val spacing = AMTheme.tokens.spacing
    val name = rememberTextFieldState("Ada Lovelace")
    val email = rememberTextFieldState()
    CatalogFrostedStage {
        Column(
            Modifier.padding(spacing.lg).fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            ProvideAMOnSurface {
                AMTextField(name, label = "Full name")
                AMTextField(email, label = "Email", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            }
        }
    }
}

/** A contact's details: the name filled in, the keyboard's Next key moving down the form. */
@Composable
private fun ContactForm() {
    val spacing = AMTheme.tokens.spacing
    val name = rememberTextFieldState("Ada Lovelace")
    val email = rememberTextFieldState()
    val phone = rememberTextFieldState()
    val website = rememberTextFieldState()
    Column(
        Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AMTextField(
            name,
            label = "Full name",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        AMTextField(
            email,
            label = "Email",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
        )
        AMTextField(
            phone,
            label = "Phone number",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        )
        AMTextField(
            website,
            label = "Website",
            placeholder = "example.com",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
        )
    }
}
