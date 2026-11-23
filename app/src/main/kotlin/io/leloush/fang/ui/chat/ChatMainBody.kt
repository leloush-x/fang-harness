package io.leloush.fang.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.R
import io.leloush.fang.agent.InboxReason
import io.leloush.fang.agent.TimelineState
import io.leloush.fang.appGraph
import io.leloush.fang.ui.catchup.CatchUpDeck
import io.leloush.fang.ui.catchup.CatchUpSession
import io.leloush.fang.ui.components.SnackbarStackState
import io.leloush.fang.ui.notifications.NoticeCapsule
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import io.leloush.fang.ui.workspace.PaneControls

/**
 * The parent session's header, stream, dialog and composer. Split out of [ChatContent] so both stay under detekt's
 * method limit; the chat's changing state arrives in [state], and it keeps only whether Catch Up is open.
 */
@Composable
internal fun ChatMainBody(
  controller: ChatController,
  snackbar: SnackbarStackState,
  controls: PaneControls,
  timeline: TimelineState,
  state: ChatMainState,
  onOpenProviders: () -> Unit,
  onOpenOnboarding: () -> Unit,
  onSheet: (ChatSheet) -> Unit,
) {
  val process = controller.process
  var catchUpOpen by remember { mutableStateOf(false) }
  Column(
    modifier = Modifier.fillMaxSize().background(fangColors.surface0).closeContextDetailsOnPress(controller),
  ) {
    ChatHeader(
      process,
      controls,
      onActions = { onSheet(ChatSheet.Actions) },
      onCatchUp = { catchUpOpen = true },
      modifier = Modifier.onUnhandledTap(state.dismissKeyboard),
    )
    ChatStack(
      modifier = Modifier.weight(1f),
      stream = {
        StreamArea(controller, timeline, snackbar, onOpenProviders, onOpenOnboarding, onSheet, state.dismissKeyboard)
      },
      dialog = {
        Column {
          if (state.facts.isNotEmpty()) {
            StreamSubagents(
              state.facts,
              onOpen = { state.onOpenSubagent(it.toolCallId) },
              modifier = Modifier.padding(bottom = FangTokens.Spacing.s8),
            )
          }
          PendingDialogCard(controller, timeline, state.answered)
          state.notice?.let { NoticeCapsule(it.title, it.onClick, Modifier.padding(bottom = FangTokens.Spacing.s8)) }
        }
      },
      steering = { PendingSteeringBox(controller, timeline, compact = state.tight) },
      composer = {
        Composer(
          controller = controller,
          model = timeline.model,
          thinkingLevel = timeline.thinkingLevel,
          working = state.working,
          tight = state.tight,
          onSheet = onSheet,
        )
      },
    )
    if (catchUpOpen) CatchUpOverlay(state.onOpenChat) { catchUpOpen = false }
  }
}

/** The Catch Up deck over the chat, from the chats the inbox holds; its callbacks persist through the inbox. */
@Composable
private fun CatchUpOverlay(onOpenChat: (cwd: String, sessionPath: String) -> Unit, onClose: () -> Unit) {
  val inbox = LocalContext.current.appGraph.chatInbox
  val entries by inbox.entries.collectAsStateWithLifecycle()
  val newChat = stringResource(R.string.chat_new_chat)
  // Snapshot once: the deck owns its queue and Undo history while open, so later inbox changes must not rebuild it.
  val cards = remember {
    entries.map { entry ->
      CatchUpSession(
        id = entry.location.sessionPath,
        title = entry.title?.takeIf(String::isNotBlank) ?: newChat,
        subtitle = entry.detail,
        needsInput = entry.reason == InboxReason.Waiting,
      ) to entry
    }
  }
  val sessions = remember(cards) { cards.map { it.first } }
  Dialog(
    onDismissRequest = onClose,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Box(Modifier.fillMaxSize().background(fangColors.surface0).windowInsetsPadding(WindowInsets.safeDrawing)) {
      CatchUpDeck(
        sessions = sessions,
        onOpen = { session ->
          val entry = cards.firstOrNull { it.first.id == session.id }?.second
          if (entry != null) {
            inbox.remove(session.id)
            onOpenChat(entry.location.cwd, entry.location.sessionPath)
          }
          onClose()
        },
        onMarkRead = { inbox.remove(it.id) },
        onKeepUnread = { session -> cards.firstOrNull { it.first.id == session.id }?.second?.let(inbox::add) },
        onMarkAll = inbox::clear,
        onLeave = onClose,
      )
    }
  }
}
