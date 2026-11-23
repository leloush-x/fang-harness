package io.leloush.fang.ui.chat

import io.leloush.fang.agent.AskAnswered
import io.leloush.fang.ui.subagents.SubagentFacts

/** One waiting chat the composer offers to open, with the title to show and the action that opens it. */
internal data class ChatNotice(val title: String, val onClick: () -> Unit)

/** The changing state the parent chat's body renders, bundled to keep composable parameter lists short. */
internal data class ChatMainState(
  val working: Boolean,
  val tight: Boolean,
  val answered: AskAnswered?,
  val facts: List<SubagentFacts>,
  val notice: ChatNotice?,
  val onOpenChat: (cwd: String, sessionPath: String) -> Unit,
  val onOpenSubagent: (String) -> Unit,
  val dismissKeyboard: () -> Unit,
)
