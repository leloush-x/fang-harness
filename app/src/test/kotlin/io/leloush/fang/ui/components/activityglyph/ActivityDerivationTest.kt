package io.leloush.fang.ui.components.activityglyph

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityDerivationTest {
  private val now = 2_000_000L

  @Test
  fun workingFinalAndStoppingPhasesWinOverTheRunningFlag() {
    assertEquals(AgentActivity.FAILED, WorkingActivity(running = true, finished = true, failed = true).activity(now))
    assertEquals(AgentActivity.DONE, WorkingActivity(running = true, finished = true).activity(now))
    assertEquals(AgentActivity.IDLE, WorkingActivity(running = true, stopping = true).activity(now))
    assertEquals(AgentActivity.IDLE, WorkingActivity(running = false).activity(now))
  }

  @Test
  fun workingNeedsInputAndStartupUseUpstreamThresholds() {
    assertEquals(
      AgentActivity.WAITING,
      WorkingActivity(running = true, needsInput = true, startedAtMillis = now).activity(now),
    )
    assertEquals(AgentActivity.STARTUP, WorkingActivity(running = true, startedAtMillis = now - 1_399).activity(now))
    assertEquals(AgentActivity.THINKING, WorkingActivity(running = true, startedAtMillis = now - 1_400).activity(now))
    assertEquals(AgentActivity.THINKING, WorkingActivity(running = true).activity(now))
  }

  @Test
  fun subagentFinalPhasesAndNeedsInputComeFirst() {
    assertEquals(AgentActivity.FAILED, SubagentActivity(running = true, done = true, failed = true).activity(now))
    assertEquals(AgentActivity.DONE, SubagentActivity(running = true, done = true).activity(now))
    assertEquals(
      AgentActivity.WAITING,
      SubagentActivity(running = true, needsInput = true, startedAtMillis = now).activity(now),
    )
  }

  @Test
  fun subagentSpawningAndIdleDistinguishACallStillBeingWritten() {
    assertEquals(AgentActivity.HANDOFF, SubagentActivity(running = false, spawnedAtMillis = now).activity(now))
    assertEquals(AgentActivity.IDLE, SubagentActivity(running = false, startedAtMillis = now).activity(now))
  }

  @Test
  fun subagentRecencyFollowsTheUpstreamOrdering() {
    val recent = SubagentActivity(
      running = true,
      spawnedAtMillis = now - 2_599,
      startedAtMillis = now - 1_399,
      textAtMillis = now - 799,
      toolAtMillis = now - 1_999,
      activityAtMillis = now,
    )
    assertEquals(AgentActivity.HANDOFF, recent.activity(now))
    assertEquals(AgentActivity.STARTUP, recent.copy(spawnedAtMillis = now - 2_600).activity(now))
    assertEquals(AgentActivity.STREAMING, recent.copy(spawnedAtMillis = null, startedAtMillis = null).activity(now))
    assertEquals(
      AgentActivity.TOOL,
      recent.copy(
        spawnedAtMillis = null,
        startedAtMillis = null,
        textAtMillis =
          now - 800,
      ).activity(now),
    )
    assertEquals(
      AgentActivity.LONG_WORK,
      recent.copy(
        spawnedAtMillis = null,
        startedAtMillis = null,
        textAtMillis = null,
        toolAtMillis = null,
        activityAtMillis =
          now - 8_001,
      ).activity(now),
    )
    assertEquals(
      AgentActivity.THINKING,
      recent.copy(
        spawnedAtMillis = null,
        startedAtMillis = null,
        textAtMillis = null,
        toolAtMillis = null,
        activityAtMillis =
          now - 8_000,
      ).activity(now),
    )
  }
}
