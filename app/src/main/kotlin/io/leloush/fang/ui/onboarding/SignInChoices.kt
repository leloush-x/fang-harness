package io.leloush.fang.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.host.AuthStatus
import io.leloush.fang.ui.auth.ProviderLogo
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.settings.isSignedIn
import io.leloush.fang.ui.settings.subscriptions
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** The ways to reach a model: the providers Pi ships that Fang Harness connects, or an OpenAI-compatible API. */
@Composable
internal fun SignInChoices(authStatus: AuthStatus?, enabled: Boolean, onAction: (SetupAction) -> Unit) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.lg)
  Column(
    modifier = Modifier.fillMaxWidth().background(colors.surface1, shape).padding(vertical = FangTokens.Spacing.s4),
  ) {
    subscriptions.forEach { subscription ->
      SignInRow(
        label = stringResource(subscription.title),
        signedIn = authStatus?.isSignedIn(subscription.id) == true,
        enabled = enabled,
        onClick = { onAction(SetupAction.Login(subscription.id)) },
      ) { ProviderLogo(subscription.id) }
    }
    SignInRow(
      label = stringResource(R.string.onboarding_signin_api),
      signedIn = authStatus?.custom?.isNotEmpty() == true,
      enabled = enabled,
      onClick = { onAction(SetupAction.AddProvider) },
    ) { LucideIcon(icon = R.drawable.ic_lucide_key_round, contentDescription = null) }
  }
}

@Composable
private fun SignInRow(
  label: String,
  signedIn: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
  icon: @Composable () -> Unit,
) {
  val colors = fangColors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .alpha(if (enabled) 1f else 0.5f)
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = FangTokens.Spacing.s16),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s12),
  ) {
    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) { icon() }
    Text(
      text = label,
      modifier = Modifier.weight(1f),
      color = colors.foreground,
      fontSize = FangTokens.FontSize.base,
      // Two lines fit "OpenAI-compatible API" at the largest font size.
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    if (signedIn) {
      LucideIcon(
        icon = R.drawable.ic_lucide_circle_check,
        contentDescription = stringResource(R.string.onboarding_signed_in),
        tint = colors.accent,
      )
    } else {
      LucideIcon(icon = R.drawable.ic_lucide_chevron_right, contentDescription = null, tint = colors.foregroundMuted)
    }
  }
}
