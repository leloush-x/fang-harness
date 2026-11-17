package io.leloush.fang.ui.subagents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.R
import io.leloush.fang.agent.AgentProcess
import io.leloush.fang.agent.AgentProcessState
import io.leloush.fang.agent.LiveSubagent
import io.leloush.fang.agent.TimelineState
import io.leloush.fang.ui.chat.ChatStream
import io.leloush.fang.ui.chat.ChatView
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/**
 * The followed run's transcript, drawn with the chat's own stream so its rows, markdown and scrolling look the same.
 * The log arrives after [AgentProcess.liveSubagents] follows the run, so a null live shows a starting notice.
 */
@Composable
internal fun ChatSubagentScreen(process: AgentProcess, runId: String, modifier: Modifier = Modifier) {
  val liveFlow = remember(process, runId) { process.liveSubagents.of(runId) }
  val live by liveFlow.collectAsStateWithLifecycle(initialValue = null)
  live?.let { LiveTranscript(it, runId, modifier) } ?: LoadingTranscript(modifier)
}

@Composable
private fun LiveTranscript(live: LiveSubagent, runId: String, modifier: Modifier) {
  val state by live.screenStates.collectAsStateWithLifecycle(initialValue = TimelineState())
  val view = remember(runId) { ChatView() }
  if (state.items.isEmpty()) {
    LoadingTranscript(modifier)
    return
  }
  ChatStream(
    timeline = state,
    processState = AgentProcessState.Ready,
    view = view,
    onOpenTool = {},
    onOpenProviders = {},
    onOpenOnboarding = {},
    onRestart = {},
    backgroundTasks = 0,
    modifier = modifier,
  )
}

@Composable
private fun LoadingTranscript(modifier: Modifier) {
  Box(modifier.fillMaxSize(), Alignment.Center) {
    Text(
      text = stringResource(R.string.subagent_transcript_starting),
      color = fangColors.foregroundMuted,
      fontSize = FangTokens.FontSize.base,
    )
  }
}
