package io.leloush.fang.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/**
 * A compact in-app notice: [title], a "Needs your input" hint, and a tap that opens the chat through [onOpen].
 * Additive to the chat inbox pill, for callers that want a single self-contained capsule without the inbox store.
 */
@Composable
internal fun NoticeCapsule(title: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
  val colors = fangColors
  val shape = CircleShape
  val openLabel = stringResource(R.string.oc_notifications_capsule_open)
  val shownTitle = title.ifBlank { stringResource(R.string.chat_new_chat) }
  Row(
    modifier = modifier
      .widthIn(max = CapsuleMaxWidth)
      .shadow(CapsuleElevation, shape)
      .clip(shape)
      .background(colors.surface2)
      .border(CapsuleBorderWidth, colors.accent, shape)
      .clickable(onClickLabel = openLabel, onClick = onOpen)
      .padding(horizontal = FangTokens.Spacing.s12)
      .heightIn(min = CapsuleHeight)
      .semantics { liveRegion = LiveRegionMode.Polite },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    LucideIcon(
      icon = R.drawable.ic_lucide_message_circle_question,
      contentDescription = null,
      size = FangTokens.IconSize.md,
      tint = colors.accent,
    )
    Text(
      text = shownTitle,
      modifier = Modifier.weight(1f, fill = false),
      color = colors.foreground,
      fontSize = FangTokens.FontSize.meta,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = stringResource(R.string.oc_notifications_capsule_needs_input),
      color = colors.foregroundMuted,
      fontSize = FangTokens.FontSize.meta,
      maxLines = 1,
      softWrap = false,
    )
  }
}

private val CapsuleMaxWidth = 320.dp
private val CapsuleHeight = 40.dp
private val CapsuleElevation = 4.dp
private val CapsuleBorderWidth = 1.dp
