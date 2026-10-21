package io.leloush.fang.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import io.leloush.fang.agent.AgentProcess
import io.leloush.fang.appGraph
import io.leloush.fang.ui.chat.ChatScreen
import io.leloush.fang.ui.chat.ChatViews
import io.leloush.fang.ui.components.SnackbarStackState
import io.leloush.fang.ui.files.ExplorerPanel
import io.leloush.fang.ui.files.ExplorerSelection
import io.leloush.fang.ui.workspace.PaneControls
import io.leloush.fang.ui.workspace.WorkspaceTabScreen
import io.leloush.fang.workspace.Pane
import io.leloush.fang.workspace.PaneItem
import io.leloush.fang.workspace.WorkspaceTab

/** One pane's screen for what it shows: a chat, a terminal, a file or a commit, or a placeholder for nothing yet. */
@Composable
internal fun WorkspacePane(
  item: PaneItem<AgentProcess>?,
  actions: ShellActions,
  views: ChatViews,
  controls: PaneControls,
  onOpenSettings: () -> Unit,
  onOpenProviders: () -> Unit,
  onOpenOnboarding: () -> Unit,
) {
  when (item) {
    is PaneItem.Tab -> WorkspaceTabScreen(item.tab, controls)

    is PaneItem.Chat, null ->
      ChatScreen(item?.process, actions, views, controls, onOpenSettings, onOpenProviders, onOpenOnboarding)
  }
}

/**
 * Tells the dialog notifications and the chat inbox which [chats] the user sees: those shown in a pane with no side
 * panel over them, while Fang Harness is on screen and no other screen covers the panes.
 */
@Composable
internal fun ChatsOnScreen(chats: Set<AgentProcess>) {
  val chatsOnScreen = LocalContext.current.appGraph.chatsOnScreen
  LifecycleStartEffect(chats) {
    chatsOnScreen.value = chats
    onStopOrDispose { chatsOnScreen.value = emptySet() }
  }
}

/**
 * [pane]'s explorer, for the folder of [item], what the pane shows. Opening a file or a commit shows it in the pane
 * and slides back to it, as Paseo's compact explorer does; the shown file's row is highlighted. One the other pane
 * shows stays there, and [onSwapPanes] brings that pane to this side.
 */
@Composable
internal fun PaneExplorer(
  pane: Pane,
  item: PaneItem<AgentProcess>?,
  open: Boolean,
  snackbar: SnackbarStackState,
  onSwapPanes: () -> Unit,
  onClose: () -> Unit,
) {
  val graph = LocalContext.current.appGraph
  val cwd = item?.cwd
  val shownFile = ((item as? PaneItem.Tab)?.tab as? WorkspaceTab.File)?.file
  val closeOrSwap = { opened: Boolean ->
    if (!opened) onSwapPanes()
    onClose()
  }
  ExplorerPanel(
    cwd = cwd,
    open = open,
    selectedFile = shownFile?.let { ExplorerSelection(it.cwd, it.path) },
    snackbar = snackbar,
    onClose = onClose,
    onOpenFile = { root, path -> closeOrSwap(graph.files.open(pane, root, path)) },
    onOpenCommit = { root, commit -> closeOrSwap(graph.commits.open(pane, root, commit)) },
  )
}
