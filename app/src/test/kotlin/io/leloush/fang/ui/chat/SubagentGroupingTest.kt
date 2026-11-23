package io.leloush.fang.ui.chat

import io.leloush.fang.ui.components.activityglyph.AgentActivity
import io.leloush.fang.ui.subagents.SubagentFacts
import io.leloush.fang.ui.subagents.SubagentPhase
import io.leloush.fang.ui.subagents.TranscriptSource
import org.junit.Assert.assertEquals
import org.junit.Test

class SubagentGroupingTest {
  private val now = 2_000_000L

  @Test
  fun waitingOutranksThinkingAndDone() {
    val facts = listOf(fact(SubagentPhase.Running), fact(SubagentPhase.Done), fact(SubagentPhase.Asking))
    assertEquals(AgentActivity.WAITING, leadingActivity(facts, now))
  }

  @Test
  fun allDoneLeadsToDone() {
    val facts = listOf(fact(SubagentPhase.Done), fact(SubagentPhase.Done))
    assertEquals(AgentActivity.DONE, leadingActivity(facts, now))
  }

  @Test
  fun noSubagentsLeadToIdle() {
    assertEquals(AgentActivity.IDLE, leadingActivity(emptyList(), now))
  }

  private fun fact(phase: SubagentPhase) = SubagentFacts(
    toolCallId = "call",
    agent = "agent",
    description = "task",
    phase = phase,
    run = null,
    resultTokens = null,
    source = TranscriptSource.None,
  )
}
