package io.leloush.fang.ui.subagents

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** One short label for a run, ellipsized in a narrow capsule: its agent name, or its task when unnamed. */
@Composable
internal fun SubagentText(label: String, modifier: Modifier = Modifier) {
  Text(
    text = label,
    modifier = modifier,
    color = fangColors.foreground,
    fontSize = FangTokens.FontSize.sm,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}

/** The small, muted activity word beside a capsule's name: Thinking, Working, Waiting, Done or Failed. */
@Composable
internal fun SubagentStatusText(label: String, modifier: Modifier = Modifier) {
  Text(
    text = label,
    modifier = modifier,
    color = fangColors.foregroundMuted,
    fontSize = FangTokens.FontSize.sm,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}
