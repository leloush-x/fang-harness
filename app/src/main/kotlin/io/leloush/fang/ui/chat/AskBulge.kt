package io.leloush.fang.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** The folded ask request's square side: 28 dp tab height plus a border, rounded like a widget tab. */
private val BULGE_SIZE = 44.dp
private val BULGE_CORNER = 10.dp

/**
 * The folded ask request: a small square tab at the composer's top edge, in the same family as [WidgetTabStrip]'s tabs.
 * It carries the questions icon and the current question's place, taps to expand, and borders in the accent colour while
 * the card is open. A failed answer tints it so it stays noticeable while folded.
 */
@Composable
internal fun AskBulge(
  counter: String,
  count: Int,
  expanded: Boolean,
  failed: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = fangColors
  val shape = RoundedCornerShape(BULGE_CORNER)
  val description = pluralStringResource(R.plurals.chat_ask_bulge_count, count, count)
  val action = stringResource(if (expanded) R.string.chat_ask_bulge_fold else R.string.chat_ask_bulge_show)
  val border = when {
    failed -> colors.destructive
    expanded -> colors.borderAccent
    else -> colors.border
  }
  val tint = if (expanded || failed) colors.accent else colors.foregroundMuted
  Column(
    modifier = modifier
      .size(BULGE_SIZE)
      .clip(shape)
      .background(if (expanded) colors.surface1 else colors.surface0)
      .border(1.dp, border, shape)
      .clickable(onClickLabel = action, role = Role.Button, onClick = onToggle)
      .semantics { contentDescription = description }
      .padding(vertical = FangTokens.Spacing.s4),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    LucideIcon(icon = R.drawable.ic_lucide_message_circle_question, contentDescription = null, tint = tint)
    Text(text = counter, color = colors.foreground, fontSize = FangTokens.FontSize.sm, maxLines = 1)
  }
}
