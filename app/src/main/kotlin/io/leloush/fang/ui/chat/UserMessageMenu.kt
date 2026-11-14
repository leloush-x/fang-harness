package io.leloush.fang.ui.chat

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.leloush.fang.R
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

@Composable
internal fun UserMessageMenu(
  menuOpen: Boolean,
  onOpen: () -> Unit,
  onDismiss: () -> Unit,
  onAction: (UserMessageAction) -> Unit,
) {
  val colors = fangColors
  androidx.compose.foundation.layout.Box {
    IconButton28(
      icon = io.leloush.fang.R.drawable.ic_lucide_ellipsis,
      contentDescription = androidx.compose.ui.res.stringResource(io.leloush.fang.R.string.chat_message_actions),
      onClick = onOpen,
      tint = colors.foregroundMuted,
      iconSize = FangTokens.IconSize.sm,
    )
    DropdownMenu(expanded = menuOpen, onDismissRequest = onDismiss) {
      DropdownMenuItem(
        text = { Text(androidx.compose.ui.res.stringResource(io.leloush.fang.R.string.chat_edit_message)) },
        leadingIcon = { LucideIcon(icon = io.leloush.fang.R.drawable.ic_lucide_pencil, contentDescription = null, size = FangTokens.IconSize.sm, tint = colors.foregroundMuted) },
        onClick = { onDismiss(); onAction(UserMessageAction.Edit) },
      )
      DropdownMenuItem(
        text = { Text(androidx.compose.ui.res.stringResource(io.leloush.fang.R.string.chat_resend_message)) },
        leadingIcon = { LucideIcon(icon = io.leloush.fang.R.drawable.ic_lucide_refresh_cw, contentDescription = null, size = FangTokens.IconSize.sm, tint = colors.foregroundMuted) },
        onClick = { onDismiss(); onAction(UserMessageAction.Resend) },
      )
      DropdownMenuItem(
        text = { Text(androidx.compose.ui.res.stringResource(io.leloush.fang.R.string.chat_delete_message)) },
        leadingIcon = { LucideIcon(icon = io.leloush.fang.R.drawable.ic_lucide_trash_2, contentDescription = null, size = FangTokens.IconSize.sm, tint = colors.destructive) },
        onClick = { onDismiss(); onAction(UserMessageAction.Delete) },
      )
    }
  }
}
