package io.leloush.fang.ui.subagents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.R
import io.leloush.fang.agent.AgentProcess
import io.leloush.fang.agent.TimelineState
import io.leloush.fang.agent.askingSubagent
import io.leloush.fang.ui.components.FangSheet
import io.leloush.fang.ui.settings.formatTokens
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import java.text.NumberFormat

/**
 * The facts for the chat's currently listed subagents, kept in sync with the run list and the chat's question state,
 * so the capsule strip and the detail sheet name the runs the extension still has.
 */
@Composable
internal fun rememberChatSubagentFacts(
  process: AgentProcess,
  timeline: TimelineState,
  working: Boolean,
): List<SubagentFacts> {
  val runs by process.subagentRuns.collectAsStateWithLifecycle()
  val cutOff by process.subagentsCutOff.collectAsStateWithLifecycle()
  val subagents = remember(runs, cutOff, working, timeline) {
    ChatSubagents(runs, askingSubagent(timeline, runs), working, cutOff = cutOff)
  }
  return remember(subagents) { subagents.runs.map { subagentFacts(it.toolCallId, null, subagents, null) } }
}

/** Detail view of the subagent whose capsule was tapped in the chat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubagentsSheet(fact: SubagentFacts, onDismiss: () -> Unit) {
  FangSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = FangTokens.Spacing.s16),
    ) {
      Text(
        text = stringResource(R.string.subagents_sheet_title),
        color = fangColors.foreground,
        fontSize = FangTokens.FontSize.lg,
        modifier = Modifier.padding(bottom = FangTokens.Spacing.s12),
      )
      DetailRow(
        stringResource(R.string.subagents_sheet_agent),
        fact.agent.ifBlank { stringResource(R.string.capsule_unnamed) },
      )
      DetailRow(stringResource(R.string.subagent_task), fact.description)
      DetailRow(stringResource(R.string.subagents_sheet_status), stringResource(fact.phase.label()))
      val tokens = fact.resultTokens ?: fact.run?.tokens
      if (tokens != null) DetailRow(stringResource(R.string.subagents_sheet_tokens), formatTokens(tokens))
      fact.run?.let { run ->
        DetailRow(stringResource(R.string.subagents_sheet_turn), NumberFormat.getIntegerInstance().format(run.turn))
        run.lastActivity?.let { DetailRow(stringResource(R.string.subagents_sheet_last_activity), it) }
      }
      TextButton(onClick = onDismiss, modifier = Modifier.padding(top = FangTokens.Spacing.s12)) {
        Text(stringResource(R.string.subagents_sheet_close))
      }
    }
  }
}

/** One label over its value; names and descriptions come from Pi, so they are shown as sent. */
@Composable
private fun DetailRow(label: String, value: String) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = FangTokens.Spacing.s6)) {
    Text(text = label, color = fangColors.foregroundMuted, fontSize = FangTokens.FontSize.sm)
    Text(text = value, color = fangColors.foreground, fontSize = FangTokens.FontSize.base)
  }
}

/** The status word shown for a subagent's phase, as in the Subagents list. */
private fun SubagentPhase.label(): Int = when (this) {
  SubagentPhase.Queued -> R.string.subagent_status_queued
  SubagentPhase.Running -> R.string.subagent_status_running
  SubagentPhase.Asking -> R.string.subagent_status_asking
  SubagentPhase.Done -> R.string.subagent_status_done
  SubagentPhase.Failed -> R.string.subagent_status_failed
  SubagentPhase.Stopped -> R.string.subagent_status_stopped
  SubagentPhase.Interrupted -> R.string.subagent_status_interrupted
}
