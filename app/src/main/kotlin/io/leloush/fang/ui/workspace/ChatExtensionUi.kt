package io.leloush.fang.ui.workspace

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.agent.AgentProcess
import io.leloush.fang.host.SessionInfo

/**
 * The title an extension set on the chat: its running [process]'s, or the [keptTitle] Fang Harness kept when it stopped the
 * chat's Pi. Else [sessionTitle].
 */
@Composable
internal fun chatTitle(session: SessionInfo, process: AgentProcess?, keptTitle: String?): String {
  val extensionTitle = if (process != null) process.extensionTitle.collectAsStateWithLifecycle().value else keptTitle
  return extensionTitle ?: sessionTitle(session)
}

/** The statuses extensions set on the chat: its running [process]'s, or the [kept] ones from when Fang Harness stopped its Pi. */
@Composable
internal fun chatStatuses(process: AgentProcess?, kept: Map<String, String>?): Map<String, String> =
  if (process != null) process.extensionStatuses.collectAsStateWithLifecycle().value else kept.orEmpty()
