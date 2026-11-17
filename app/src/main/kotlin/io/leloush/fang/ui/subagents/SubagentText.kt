package io.leloush.fang.ui.subagents

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.leloush.fang.R
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** One line naming a run: its agent and the task the model gave it, ellipsized in a narrow capsule. */
@Composable
internal fun SubagentText(agent: String, description: String, modifier: Modifier = Modifier) {
  val text = if (description.isBlank()) agent else stringResource(R.string.capsule_title, agent, description)
  Text(
    text = text,
    modifier = modifier,
    color = fangColors.foreground,
    fontSize = FangTokens.FontSize.sm,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}
