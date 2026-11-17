package io.leloush.fang.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.leloush.fang.ui.subagents.SubagentFacts

/** The subagent strip the chat places above its composer; [onOpen] opens the tapped capsule's subagent. */
@Composable
internal fun StreamSubagents(
  facts: List<SubagentFacts>,
  onOpen: (SubagentFacts) -> Unit,
  modifier: Modifier = Modifier,
) {
  SubagentCapsules(facts = facts, onOpen = onOpen, modifier = modifier)
}
