package io.leloush.fang.ui.chat

import io.leloush.fang.agent.AskAnswered
import io.leloush.fang.ui.subagents.SubagentFacts

/** The changing state the parent chat's body renders, bundled to keep composable parameter lists short. */
internal data class ChatMainState(
  val working: Boolean,
  val tight: Boolean,
  val answered: AskAnswered?,
  val facts: List<SubagentFacts>,
  val onOpenSubagent: (String) -> Unit,
  val dismissKeyboard: () -> Unit,
)
