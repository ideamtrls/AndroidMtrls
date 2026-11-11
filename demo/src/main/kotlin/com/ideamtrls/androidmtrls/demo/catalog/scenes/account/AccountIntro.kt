package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToolbar

/**
 * The first screen and the signed-out landing view: the light/dark chip at the end of its top
 * bar, Margin's logo in the room, and the two ways in.
 */
@Composable
internal fun AccountIntro(model: AccountModel) {
    val spacing = AMTheme.tokens.spacing
    Scaffold(
        topBar = { AMToolbar(actions = { SceneAppearanceButton() }) },
        containerColor = AMTheme.Surface.page,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = spacing.lg, end = spacing.lg, bottom = AccountFabClearance),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                AccountLogo()
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                AMButton(
                    "Sign in",
                    onClick = { model.begin(AccountPhase.SignIn) },
                    modifier = Modifier.fillMaxWidth(),
                    style = AMButtonStyle.Primary(),
                )
                AMButton(
                    "Create account",
                    onClick = { model.begin(AccountPhase.CreateAccount) },
                    modifier = Modifier.fillMaxWidth(),
                    style = AMButtonStyle.Secondary(),
                )
            }
        }
    }
}

/**
 * The logo of Margin, a made-up read-later company: a serif M on a tile, with a notebook's red
 * margin rule down its start side, over the name. One heading for TalkBack, "Margin". The tile
 * grows with the text size, the way large type does.
 */
@Composable
private fun AccountLogo() {
    val density = LocalDensity.current
    val side = with(density) { 96.sp.toDp() }
    val tile = RoundedCornerShape(side * 0.22f)
    val ink = MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.clearAndSetSemantics {
            contentDescription = "Margin"
            heading()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md),
    ) {
        Box(Modifier.size(side).clip(tile).background(ink)) {
            val letterSize = with(density) { (side * 0.56f).toSp() }
            Text(
                "M",
                Modifier.align(Alignment.Center).offset(x = side * 0.06f),
                color = AMTheme.Surface.primary,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = letterSize,
                    lineHeight = letterSize,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = side * 0.17f)
                    .width(max(2.dp, side * 0.025f))
                    .fillMaxHeight()
                    .background(AMSemantic.Destructive.color),
            )
        }
        Text(
            "Margin",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
            ),
            color = ink,
        )
    }
}
