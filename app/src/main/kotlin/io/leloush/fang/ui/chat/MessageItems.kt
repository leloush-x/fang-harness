package io.leloush.fang.ui.chat

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.agent.ImagePart
import io.leloush.fang.agent.TimelineItem
import io.leloush.fang.ui.components.FangMarkdown
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.components.MarkdownCache
import io.leloush.fang.ui.components.SimmeringPot
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import java.util.Date
import kotlinx.coroutines.delay

private val UserBubbleShape =
  RoundedCornerShape(
    topStart = FangTokens.Radius.xxl,
    topEnd = FangTokens.Radius.sm,
    bottomEnd = FangTokens.Radius.xxl,
    bottomStart = FangTokens.Radius.xxl,
  )
private val ImageMaxEdge = 160.dp

@Composable
internal fun UserMessage(
  item: TimelineItem.User,
  onAction: (UserMessageAction) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val colors = fangColors
  var menuOpen by remember { mutableStateOf(false) }
  Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
    Column(
      modifier = Modifier.background(colors.surface3, UserBubbleShape).padding(FangTokens.Spacing.s16),
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    ) {
      if (item.images.isNotEmpty()) UserImages(item.images)
      if (item.text.isNotEmpty()) {
        SelectionContainer {
          Text(
            text = item.text,
            color = colors.foreground,
            fontSize = FangTokens.FontSize.content,
            lineHeight = FangTokens.LineHeight.markdown,
          )
        }
      }
    }
    Row(
      modifier = Modifier.padding(top = FangTokens.Spacing.s4).heightIn(min = 28.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    ) {
      MessageTime(item.timestamp)
      if (item.text.isNotEmpty()) {
        CopyButton(
          text = item.text,
          contentDescription = stringResource(R.string.chat_copy_message),
          tint = colors.foregroundMuted,
          iconSize = FangTokens.IconSize.sm,
        )
      }
      UserMessageMenu(menuOpen = menuOpen, onOpen = { menuOpen = true }, onDismiss = { menuOpen = false }, onAction = onAction)
    }
  }
}

@Composable
private fun UserMessageMenu(
  menuOpen: Boolean,
  onOpen: () -> Unit,
  onDismiss: () -> Unit,
  onAction: (UserMessageAction) -> Unit,
) {
  val colors = fangColors
  Box {
    IconButton28(
      icon = R.drawable.ic_lucide_ellipsis,
      contentDescription = stringResource(R.string.chat_message_actions),
      onClick = onOpen,
      tint = colors.foregroundMuted,
      iconSize = FangTokens.IconSize.sm,
    )
    DropdownMenu(expanded = menuOpen, onDismissRequest = onDismiss) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.chat_edit_message)) },
        leadingIcon = { LucideIcon(icon = R.drawable.ic_lucide_pencil, contentDescription = null, size = FangTokens.IconSize.sm, tint = colors.foregroundMuted) },
        onClick = { onDismiss(); onAction(UserMessageAction.Edit) },
      )
      DropdownMenuItem(
        text = { Text(stringResource(R.string.chat_resend_message)) },
        leadingIcon = { LucideIcon(icon = R.drawable.ic_lucide_refresh_cw, contentDescription = null, size = FangTokens.IconSize.sm, tint = colors.foregroundMuted) },
        onClick = { onDismiss(); onAction(UserMessageAction.Resend) },
      )
      DropdownMenuItem(
        text = { Text(stringResource(R.string.chat_delete_message)) },
        leadingIcon = { LucideIcon(icon = R.drawable.ic_lucide_trash_2, contentDescription = null, size = FangTokens.IconSize.sm, tint = colors.destructive) },
        onClick = { onDismiss(); onAction(UserMessageAction.Delete) },
      )
    }
  }
}

internal enum class UserMessageAction { Edit, Delete, Resend }

@Composable
private fun UserImages(images: List<ImagePart>) {
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8, Alignment.End),
    verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    images.forEach { ImageThumbnail(it) }
  }
}

@Composable
internal fun ImageThumbnail(part: ImagePart) {
  val bitmap by produceState<ImageBitmap?>(null, part) { value = ChatImages.thumbnail(part) }
  val shape = RoundedCornerShape(FangTokens.Radius.md)
  val image = bitmap
  if (image == null) {
    Box(modifier = Modifier.size(ImageMaxEdge).clip(shape).background(fangColors.surface2))
    return
  }
  val aspect = image.width.toFloat() / image.height.coerceAtLeast(1)
  val size = if (aspect >= 1f) {
    DpSize(ImageMaxEdge, ImageMaxEdge / aspect)
  } else {
    DpSize(ImageMaxEdge * aspect, ImageMaxEdge)
  }
  Image(
    bitmap = image,
    contentDescription = stringResource(R.string.chat_image),
    modifier = Modifier.size(size).clip(shape),
    contentScale = ContentScale.Crop,
  )
}

/**
 * A block of the agent's reply; once it finished streaming, Paseo's copy button and its time sit under it. Its parse is
 * kept in [markdownCache] once it has finished, not for every text it streams through.
 */
@Composable
internal fun AssistantMessage(
  item: TimelineItem.AssistantText,
  markdownCache: MarkdownCache,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth().padding(top = FangTokens.Spacing.s12)) {
    FangMarkdown(
      markdown = item.markdown,
      modifier = Modifier.fillMaxWidth(),
      cache = markdownCache.takeUnless { item.streaming },
    )
    if (item.streaming) {
      Spacer(modifier = Modifier.height(FangTokens.Spacing.s12))
      return@Column
    }
    Row(
      modifier = Modifier.padding(top = FangTokens.Spacing.s4),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    ) {
      CopyButton(
        text = item.markdown,
        contentDescription = stringResource(R.string.chat_copy_reply),
        tint = fangColors.foregroundMuted,
        iconSize = FangTokens.IconSize.sm,
      )
      MessageTime(item.timestamp)
    }
  }
}

/** When a message was sent, as `2:47 PM` or `14:47` in the device's time zone, language and 12/24-hour setting. */
@Composable
internal fun MessageTime(timestamp: Long) {
  val context = LocalContext.current
  val time = remember(timestamp, context) { DateFormat.getTimeFormat(context).format(Date(timestamp)) }
  Text(text = time, color = fangColors.foregroundMuted, fontSize = FangTokens.FontSize.meta)
}

/** Paseo's compact tool-call row: icon badge, title, muted one-line summary. */
@Composable
internal fun ToolRow(presentation: ToolPresentation, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val colors = fangColors
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(FangTokens.Radius.lg))
        .background(colors.surface1)
        .clickable(onClick = onClick)
        .padding(horizontal = FangTokens.Spacing.s8, vertical = FangTokens.Spacing.s6),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
      if (presentation.running) {
        SimmeringPot(size = FangTokens.IconSize.sm)
      } else {
        LucideIcon(
          icon = presentation.icon,
          contentDescription = null,
          size = FangTokens.IconSize.sm,
          tint = if (presentation.isError) colors.destructive else colors.foregroundMuted,
        )
      }
    }
    Text(
      text = presentation.title,
      color = if (presentation.isError) colors.destructive else colors.foreground,
      fontSize = FangTokens.FontSize.base,
      maxLines = 1,
    )
    Text(
      text = presentation.summary.orEmpty(),
      modifier = Modifier.weight(1f),
      color = colors.foregroundMuted,
      fontSize = FangTokens.FontSize.base,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
internal fun ErrorRow(message: String, modifier: Modifier = Modifier) {
  val colors = fangColors
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = FangTokens.Spacing.s8),
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    LucideIcon(
      icon = R.drawable.ic_lucide_circle_alert,
      contentDescription = null,
      modifier = Modifier.padding(top = FangTokens.Spacing.s2),
      tint = colors.destructive,
    )
    SelectionContainer(modifier = Modifier.weight(1f)) {
      Text(text = message, color = colors.destructive, fontSize = FangTokens.FontSize.base)
    }
  }
}

@Composable
internal fun NoticeRow(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    modifier = modifier.fillMaxWidth().padding(vertical = FangTokens.Spacing.s4),
    color = fangColors.foregroundMuted,
    fontSize = FangTokens.FontSize.meta,
    textAlign = TextAlign.Center,
  )
}

/**
 * Live `m:ss` while the run works, then how long it took. While the chat waits on [backgroundTasks], the simmering
 * pot stays, saying how many.
 */
@Composable
internal fun TurnFooterRow(item: TimelineItem.TurnFooter, backgroundTasks: Int, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().heightIn(min = 28.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    val duration = item.durationMillis
    if (duration != null) {
      Text(text = durationText(duration), color = fangColors.foregroundMuted, fontSize = FangTokens.FontSize.meta)
      if (backgroundTasks > 0) SimmeringPot(size = FangTokens.IconSize.sm)
    } else {
      SimmeringPot(size = FangTokens.IconSize.sm)
      RunningElapsed(item.startedAt)
    }
    if (backgroundTasks == 0) return@Row
    Text(
      text = pluralStringResource(R.plurals.chat_background_tasks_running, backgroundTasks, backgroundTasks),
      color = fangColors.foregroundMuted,
      fontSize = FangTokens.FontSize.meta,
    )
  }
}

@Composable
private fun RunningElapsed(startedAt: Long) {
  val now by produceState(System.currentTimeMillis(), startedAt) {
    while (true) {
      value = System.currentTimeMillis()
      delay(1_000L - (value - startedAt).mod(1_000L))
    }
  }
  val seconds = ((now - startedAt) / 1_000L).coerceAtLeast(0)
  Text(
    text = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
    color = fangColors.foregroundMuted,
    fontSize = FangTokens.FontSize.meta,
  )
}

@Composable
private fun durationText(millis: Long): String {
  val seconds = (millis / 1_000L).toInt()
  return if (seconds >= 60) {
    stringResource(R.string.chat_duration_minutes, seconds / 60, seconds % 60)
  } else {
    stringResource(R.string.chat_duration_seconds, seconds)
  }
}
