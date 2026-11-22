package io.leloush.fang.ui.components.activityglyph

/** All thresholds are upstream's, in milliseconds (1.4 s start, 2.6 s spawn, 0.8 s text, 2 s tool, 8 s idle). */
private const val STARTUP_MS = 1_400L
private const val SPAWN_MS = 2_600L
private const val TEXT_MS = 800L
private const val TOOL_MS = 2_000L
private const val LONG_WORK_MS = 8_000L

/**
 * The main run beside a Working line. Final outcomes win over a stale running flag; stopping is
 * upstream's neutral Phase.stopping, which reads as Idle.
 */
data class WorkingActivity(
  val running: Boolean,
  val needsInput: Boolean = false,
  val startedAtMillis: Long? = null,
  val finished: Boolean = false,
  val failed: Boolean = false,
  val stopping: Boolean = false,
) {
  fun activity(now: Long = System.currentTimeMillis()): AgentActivity = when {
    failed -> AgentActivity.FAILED
    finished -> AgentActivity.DONE
    stopping -> AgentActivity.IDLE
    !running -> AgentActivity.IDLE
    needsInput -> AgentActivity.WAITING
    startedAtMillis != null && now - startedAtMillis < STARTUP_MS -> AgentActivity.STARTUP
    else -> AgentActivity.THINKING
  }
}

/** A subagent, derived from how recently each of its own events arrived. */
data class SubagentActivity(
  val running: Boolean,
  val needsInput: Boolean = false,
  val spawnedAtMillis: Long? = null,
  val startedAtMillis: Long? = null,
  val textAtMillis: Long? = null,
  val toolAtMillis: Long? = null,
  val activityAtMillis: Long? = null,
  val done: Boolean = false,
  val failed: Boolean = false,
) {
  fun activity(now: Long = System.currentTimeMillis()): AgentActivity = when {
    failed -> AgentActivity.FAILED

    done -> AgentActivity.DONE

    needsInput -> AgentActivity.WAITING

    // A call still being written, with no session of its own yet, is the spawning phase.
    !running && spawnedAtMillis != null && startedAtMillis == null -> AgentActivity.HANDOFF

    !running -> AgentActivity.IDLE

    spawnedAtMillis != null && now - spawnedAtMillis < SPAWN_MS -> AgentActivity.HANDOFF

    startedAtMillis != null && now - startedAtMillis < STARTUP_MS -> AgentActivity.STARTUP

    textAtMillis != null && now - textAtMillis < TEXT_MS -> AgentActivity.STREAMING

    toolAtMillis != null && now - toolAtMillis < TOOL_MS -> AgentActivity.TOOL

    activityAtMillis != null && now - activityAtMillis > LONG_WORK_MS -> AgentActivity.LONG_WORK

    else -> AgentActivity.THINKING
  }
}
