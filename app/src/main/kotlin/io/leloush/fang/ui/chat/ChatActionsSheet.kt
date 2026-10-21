package io.leloush.fang.ui.chat

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.leloush.fang.R
import io.leloush.fang.ui.components.FangSheet
import io.leloush.fang.ui.components.SheetRow
import io.leloush.fang.ui.theme.fangColors

/** The header's ellipsis sheet; each action runs after the sheet is dismissed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatActionsSheet(actions: ChatActions, onDismiss: () -> Unit) {
  val dismissThen = { action: () -> Unit ->
    onDismiss()
    action()
  }
  FangSheet(onDismissRequest = onDismiss) {
    SheetRow(
      label = stringResource(R.string.chat_new_chat),
      icon = R.drawable.ic_lucide_square_pen,
      onClick = { dismissThen(actions.onNewChat) },
    )
    SheetRow(
      label = stringResource(R.string.terminal_new),
      icon = R.drawable.ic_lucide_square_terminal,
      onClick = { dismissThen(actions.onNewTerminal) },
    )
    SheetRow(
      label = stringResource(R.string.chat_action_rename),
      icon = R.drawable.ic_lucide_pencil,
      onClick = { dismissThen(actions.onRename) },
    )
    SheetRow(
      label = stringResource(R.string.chat_action_reload),
      icon = R.drawable.ic_lucide_refresh_cw,
      onClick = { dismissThen(actions.onReload) },
    )
    val copyPath = actions.onCopyPath
    SheetRow(
      label = stringResource(R.string.chat_action_copy_path),
      icon = R.drawable.ic_lucide_copy,
      enabled = copyPath != null,
      onClick = { copyPath?.let(dismissThen) },
    )
    val archive = actions.onArchive
    SheetRow(
      label = stringResource(R.string.chat_action_archive),
      icon = R.drawable.ic_lucide_archive,
      enabled = archive != null,
      onClick = { archive?.let(dismissThen) },
    )
  }
}
