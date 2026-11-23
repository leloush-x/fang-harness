package io.leloush.fang.ui.subagents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import io.leloush.fang.agent.ChatSubagents
import io.leloush.fang.agent.LiveSubagent
import io.leloush.fang.agent.SUBAGENT_TOOL
import io.leloush.fang.agent.SubagentAsker
import io.leloush.fang.agent.SubagentRun
import io.leloush.fang.agent.TimelineItem
import io.leloush.fang.agent.TimelineState
import io.leloush.fang.agent.subagentFacts
import io.leloush.fang.ui.chat.ChatStream
import io.leloush.fang.ui.chat.ChatView
import io.leloush.fang.ui.chat.SubagentCapsules
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/**
 * The followed run's transcript, drawn with the chat's own stream so its rows, markdown and scrolling look the same.
 * Subagents this run started show as a capsule strip and push onto the stack when tapped. The log arrives after
 * [AgentProcess.liveSubagents] follows the run, so a null live shows a starting notice.
 */
@Composable
internal fun ChatSubagentScreen(
  process: AgentProcess,
  runId: String,
  openIds: Set<String>,
  onPush: (SubagentFacts) -> Unit,
  modifier: Modifier = Modifier,
) {
  val runs by process.subagentRuns.collectAsStateWithLifecycle()
  val liveFlow = remember(process, runId) { process.liveSubagents.of(runId) }
  val live by liveFlow.collectAsStateWithLifecycle(initialValue = null)
  live?.let { LiveTranscript(it, runs, openIds, onPush, modifier) } ?: LoadingTranscript(modifier)
}

@Composable
private fun LiveTranscript(
  live: LiveSubagent,
  runs: List<SubagentRun>,
  openIds: Set<String>,
  onPush: (SubagentFacts) -> Unit,
  modifier: Modifier,
) {
  val state by live.screenStates.collectAsStateWithLifecycle(initialValue = TimelineState())
  val view = remember { ChatView() }
  val nested = remember(state.items, runs, openIds) { nestedSubagentFacts(state, runs, openIds) }
  val tools = remember(state.items) {
    state.items.filterIsInstance<TimelineItem.ToolCall>().associate { it.key to it.id }
  }
  if (state.items.isEmpty()) {
    LoadingTranscript(modifier)
    return
  }
  Column(modifier) {
    if (nested.isNotEmpty()) SubagentCapsules(nested, onPush)
    ChatStream(
      timeline = state,
      processState = AgentProcessState.Ready,
      view = view,
      onOpenTool = { key -> tools[key]?.let { id -> nested.firstOrNull { it.toolCallId == id }?.let(onPush) } },
      onOpenProviders = {},
      onOpenOnboarding = {},
      onRestart = {},
      backgroundTasks = 0,
      modifier = Modifier.weight(1f),
    )
  }
}

/** The subagents this transcript started that are not already open, matched against the extension's run list. */
private fun nestedSubagentFacts(
  state: TimelineState,
  runs: List<SubagentRun>,
  openIds: Set<String>,
): List<SubagentFacts> {
  val ids = state.items.filterIsInstance<TimelineItem.ToolCall>()
    .filter { it.toolName == SUBAGENT_TOOL && !it.byUser }
    .mapTo(HashSet()) { it.id }
    .minus(openIds)
  if (ids.isEmpty()) return emptyList()
  val subagents = ChatSubagents(runs, SubagentAsker.Unknown, working = false)
  return ids.mapNotNull { subagents.run(it) }.map { subagentFacts(it.toolCallId, null, subagents, null) }
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
