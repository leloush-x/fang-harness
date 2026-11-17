package io.leloush.fang.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.leloush.fang.agent.TimelineState
import io.leloush.fang.ui.components.SnackbarStackState
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import io.leloush.fang.ui.workspace.PaneControls

/**
 * The parent session's header, stream, dialog and composer. Split out of [ChatContent] so both stay under detekt's
 * method limit; it owns no state, everything it needs arrives in [state].
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
  Column(
    modifier = Modifier.fillMaxSize().background(fangColors.surface0).closeContextDetailsOnPress(controller),
  ) {
    ChatHeader(
      process,
      controls,
      onActions = { onSheet(ChatSheet.Actions) },
      Modifier.onUnhandledTap(state.dismissKeyboard),
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
  }
}
