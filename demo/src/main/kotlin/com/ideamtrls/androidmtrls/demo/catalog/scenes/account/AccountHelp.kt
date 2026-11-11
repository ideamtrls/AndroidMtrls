package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMDisclosureGroup
import com.ideamtrls.androidmtrls.AMTheme

/**
 * Margin's help, under the sheet's "Help" title: common account questions, each a card that folds
 * open to its answer.
 */
@Composable
internal fun AccountHelpSheet() {
    val spacing = AMTheme.tokens.spacing
    Column(
        // Room for the cards' shadows inside the scroll's clip.
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AccountQuestions.forEach { (question, answer) ->
            key(question) {
                var expanded by rememberSaveable { mutableStateOf(false) }
                AMDisclosureGroup(question, expanded, onExpandedChange = { expanded = it }, Modifier.fillMaxWidth()) {
                    Text(
                        answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Each question and its answer. */
private val AccountQuestions = listOf(
    "Where's my sign-in code?" to
        "We text a six-digit code to the phone on your account each time you sign in. If it hasn't arrived " +
        "after 30 seconds, tap Resend code.",
    "How do I reset my password?" to
        "Tap Forgot password? on the sign-in screen and we'll email you a reset link. The link works for an " +
        "hour, on any device.",
    "Can I change my email?" to
        "Your email is listed under Account in Settings. To change it, write to us from that address and " +
        "we'll switch it within a day.",
    "How do I delete my account?" to
        "Write to us from the email on your account and we'll delete it, with everything you've saved, " +
        "within 30 days. Deleted highlights and notes can't be recovered.",
)
