package io.leloush.fang.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import io.leloush.fang.R
import io.leloush.fang.agent.AgentProcessState
import io.leloush.fang.agent.TimelineItem
import io.leloush.fang.ui.components.CodeBox
import io.leloush.fang.ui.components.CodeText
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.components.SimmeringPot
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/**
 * The process banner at the top of the stream: starting, installing Pi, or failed with Retry. [onOpenOnboarding] opens
 * setup at the step a failure needs.
 */
@Composable
internal fun StatusBanner(
  state: AgentProcessState,
  onRetry: () -> Unit,
  onOpenOnboarding: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (state) {
    AgentProcessState.Starting -> BannerLine(
      text = stringResource(R.string.chat_starting),
      modifier = modifier,
      potSize = FangTokens.IconSize.lg,
      fontSize = FangTokens.FontSize.lg,
    )

    is AgentProcessState.Installing -> Column(modifier = modifier) {
      BannerLine(stringResource(R.string.chat_installing))
      state.progressLine?.let {
        Text(
          text = it,
          modifier = Modifier.fillMaxWidth(),
          color = fangColors.foregroundMuted,
          fontFamily = FontFamily.Monospace,
          fontSize = FangTokens.FontSize.code,
          lineHeight = FangTokens.LineHeight.code,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    is AgentProcessState.Failed -> FailedBanner(state, onRetry, onOpenOnboarding, modifier)

    AgentProcessState.Ready, is AgentProcessState.Exited -> Unit
  }
}

/** A pot beside [text]; Starting is the chat's only content while a session opens, so it uses larger sizes. */
@Composable
private fun BannerLine(
  text: String,
  modifier: Modifier = Modifier,
  potSize: Dp = FangTokens.IconSize.sm,
  fontSize: TextUnit = FangTokens.FontSize.meta,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = FangTokens.Spacing.s8),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8, Alignment.CenterHorizontally),
  ) {
    SimmeringPot(size = potSize)
    Text(text = text, color = fangColors.foregroundMuted, fontSize = fontSize)
  }
}

/**
 * Pi could not start. A runtime setup failure offers setup, which opens at the step that fixes it.
 */
@Composable
private fun FailedBanner(
  state: AgentProcessState.Failed,
  onRetry: () -> Unit,
  onOpenOnboarding: () -> Unit,
  modifier: Modifier,
) {
  val failure = state.failure
  BannerCard(modifier) {
    AlertLine(state.message)
    // Set up runtime and Retry don't fit on one line at large font sizes.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8)) {
      if (failure?.fixedBySetup == true) {
        TextButton(onClick = onOpenOnboarding) { Text(text = stringResource(R.string.chat_set_up_runtime)) }
      }
      TextButton(onClick = onRetry) { Text(text = stringResource(R.string.chat_retry)) }
    }
  }
}

/** Pi exited unasked: the exit code, an expandable stderr tail and Restart. */
@Composable
internal fun ExitBannerCard(item: TimelineItem.ExitBanner, onRestart: () -> Unit, modifier: Modifier = Modifier) {
  var expanded by rememberSaveable(item.key) { mutableStateOf(false) }
  BannerCard(modifier) {
    AlertLine(
      item.code?.let { stringResource(R.string.chat_exit_code, it) } ?: stringResource(R.string.chat_agent_stopped),
    )
    if (item.stderr.isNotBlank()) {
      Text(
        text = stringResource(if (expanded) R.string.chat_hide_details else R.string.chat_show_details),
        modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = FangTokens.Spacing.s4),
        color = fangColors.accent,
        fontSize = FangTokens.FontSize.meta,
      )
      if (expanded) SelectionContainer { CodeBox { CodeText(item.stderr.trimEnd()) } }
    }
    TextButton(onClick = onRestart) { Text(text = stringResource(R.string.chat_restart)) }
  }
}

@Composable
private fun AlertLine(text: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8)) {
    LucideIcon(
      icon = R.drawable.ic_lucide_circle_alert,
      contentDescription = null,
      modifier = Modifier.padding(top = FangTokens.Spacing.s2),
      tint = fangColors.destructive,
    )
    Text(
      text = text,
      modifier = Modifier.weight(1f),
      color = fangColors.foreground,
      fontSize = FangTokens.FontSize.base,
    )
  }
}

@Composable
private fun BannerCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(vertical = FangTokens.Spacing.s8)
        .clip(RoundedCornerShape(FangTokens.Radius.lg))
        .background(fangColors.surface2)
        .padding(start = FangTokens.Spacing.s12, top = FangTokens.Spacing.s12, end = FangTokens.Spacing.s12),
    verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4),
    content = content,
  )
}
