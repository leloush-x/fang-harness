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
