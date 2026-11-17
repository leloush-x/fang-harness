package io.leloush.fang.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.leloush.fang.R
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

@Composable
internal fun UserMessageMenu(
  text: String,
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
      // An image-only message has no text to copy or quote; quoting it would also clear the user's draft.
      if (text.isNotEmpty()) {
        ActionItem(stringResource(R.string.chat_copy_full_message), R.drawable.ic_lucide_copy) {
          onDismiss()
          onAction(UserMessageAction.Copy)
        }
        ActionItem(stringResource(R.string.chat_quote_in_reply), null) {
          onDismiss()
          onAction(UserMessageAction.Quote)
        }
      }
      ActionItem(stringResource(R.string.chat_edit_message), R.drawable.ic_lucide_pencil) {
        onDismiss()
        onAction(UserMessageAction.Edit)
      }
      ActionItem(stringResource(R.string.chat_resend_message), R.drawable.ic_lucide_refresh_cw) {
        onDismiss()
        onAction(UserMessageAction.Resend)
      }
      ActionItem(stringResource(R.string.chat_delete_message), R.drawable.ic_lucide_trash_2, colors.destructive) {
        onDismiss()
        onAction(UserMessageAction.Delete)
      }
    }
  }
}

/** One menu row; a null icon leaves room for text-only actions such as Quote. */
@Composable
private fun ActionItem(label: String, icon: Int?, tint: Color = fangColors.foregroundMuted, onClick: () -> Unit) {
  DropdownMenuItem(
    text = { Text(label) },
    leadingIcon = icon?.let {
      {
        LucideIcon(icon = it, contentDescription = null, size = FangTokens.IconSize.sm, tint = tint)
      }
    },
    onClick = onClick,
  )
}
