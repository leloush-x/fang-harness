package io.leloush.fang.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.agent.AskAnswered
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/**
 * A sent answer shown as one quiet line above the composer, not a card: the chosen options (or the freeform text),
 * with a Dismiss action. The answer itself is already on its channel; this is only a local acknowledgement.
 */
@Composable
internal fun AskAnsweredSummary(record: AskAnswered, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  val colors = fangColors
  val summary = remember(record) { answeredLine(record) }
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = FangTokens.Spacing.s16, vertical = FangTokens.Spacing.s4),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LucideIcon(
      icon = R.drawable.ic_lucide_check,
      contentDescription = null,
      size = 12.dp,
      tint = colors.accent,
    )
    Text(
      text = stringResource(R.string.chat_ask_answered_line, summary),
      color = colors.foregroundMuted,
      fontSize = FangTokens.FontSize.sm,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .weight(1f)
        .padding(start = FangTokens.Spacing.s6),
    )
    Text(
      text = stringResource(R.string.chat_dismiss),
      color = colors.foregroundMuted,
      fontSize = FangTokens.FontSize.sm,
      modifier = Modifier.clickable(onClick = onDismiss),
    )
  }
}

/** A short, human-readable line for the picks or freeform text the user submitted. */
private fun answeredLine(record: AskAnswered): String {
  val parts = (0 until record.answers.length()).mapNotNull { index ->
    val answer = record.answers.optJSONObject(index) ?: return@mapNotNull null
    val response = answer.optJSONObject("response")
    val picks = response?.optJSONArray("selections")
    val text = response?.optString("text")?.takeIf { it.isNotEmpty() }
    when {
      answer.optString("status") == "cancelled" -> null
      text != null -> text
      picks != null && picks.length() > 0 -> (0 until picks.length()).joinToString(", ") { picks.optString(it) }
      else -> null
    }
  }
  return parts.joinToString(", ")
}
