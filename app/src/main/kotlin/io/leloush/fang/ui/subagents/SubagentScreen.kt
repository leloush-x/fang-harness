package io.leloush.fang.ui.subagents

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.leloush.fang.R
import io.leloush.fang.agent.AgentProcess
import io.leloush.fang.ui.components.HeaderBar
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import kotlinx.coroutines.launch

/**
 * The full chat of the subagent whose capsule was tapped, shown over the parent session. Follows the run's live log
 * while open, lets the user steer it while it runs, and returns to the parent on Back. A run that ends keeps its
 * transcript on screen with a finished notice until the user closes it.
 */
@Composable
internal fun SubagentChatScreen(
  process: AgentProcess,
  fact: SubagentFacts,
  onClose: () -> Unit,
  onError: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onClose() }
  // The run leaves the list once it ends; keep the id followed when the screen opened so it cannot change mid-view.
  val runId = remember(fact.toolCallId) { fact.run?.id ?: fact.toolCallId }
  FollowRun(process, runId)
  val name = fact.agent.ifBlank { stringResource(R.string.capsule_unnamed) }
  val active = fact.phase.active
  val failed = stringResource(R.string.subagent_chat_steer_failed)
  val scope = rememberCoroutineScope()
  val send: (String) -> Unit = { text ->
    scope.launch {
      if (!process.steerSubagent(runId, text)) onError(failed)
    }
  }
  Box(modifier = modifier.fillMaxSize().background(fangColors.surface0)) {
    Column(modifier = Modifier.fillMaxSize()) {
      SubagentHeader(name, fact.description, active, onClose)
      if (!active) {
        Text(
          text = stringResource(R.string.subagent_chat_finished),
          color = fangColors.foregroundMuted,
          fontSize = FangTokens.FontSize.sm,
          modifier = Modifier.fillMaxWidth().padding(
            horizontal = FangTokens.Spacing.s16,
            vertical = FangTokens.Spacing.s8,
          ),
        )
      }
      ChatSubagentScreen(process, runId, modifier = Modifier.weight(1f))
      if (active) SubagentComposer(name, send)
    }
  }
}

/** Follows [runId]'s log while this screen shows, stopping the extension's events when it leaves. */
@Composable
private fun FollowRun(process: AgentProcess, runId: String) {
  LaunchedEffect(process, runId) { process.liveSubagents.follow(runId) }
  DisposableEffect(process, runId) { onDispose { process.liveSubagents.unfollow(runId) } }
}

/** The transcript's header: the run's name and task, a Back button and, while it runs, a status word. */
@Composable
private fun SubagentHeader(name: String, description: String, active: Boolean, onClose: () -> Unit) {
  HeaderBar(
    title = stringResource(R.string.subagent_title, name, description),
    leading = {
      IconButton28(
        icon = R.drawable.ic_lucide_chevron_left,
        contentDescription = stringResource(R.string.subagent_back),
        onClick = onClose,
      )
    },
    trailing = {
      if (active) {
        Text(
          text = stringResource(R.string.subagent_chat_running),
          color = fangColors.foregroundMuted,
          fontSize = FangTokens.FontSize.sm,
        )
      }
    },
  )
}
