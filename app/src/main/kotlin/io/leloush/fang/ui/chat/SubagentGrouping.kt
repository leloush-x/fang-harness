package io.leloush.fang.ui.chat

import io.leloush.fang.ui.components.activityglyph.AgentActivity
import io.leloush.fang.ui.subagents.SubagentFacts
import io.leloush.fang.ui.subagents.subagentActivity

/**
 * Most telling first, upstream's `AgentActivitySource.priority`: a subagent waiting on the user outranks a failure,
 * which outranks everything still running. A finished one never outranks an active one.
 */
private val priority = listOf(
  AgentActivity.WAITING,
  AgentActivity.FAILED,
  AgentActivity.HANDOFF,
  AgentActivity.STARTUP,
  AgentActivity.TOOL,
  AgentActivity.STREAMING,
  AgentActivity.LONG_WORK,
  AgentActivity.THINKING,
  AgentActivity.DONE,
  AgentActivity.IDLE,
)

/** The most telling activity across [facts] at [now]; the group capsule's one glyph follows it on the frame clock. */
internal fun leadingActivity(facts: List<SubagentFacts>, now: Long): AgentActivity =
  leading(facts.map { it.subagentActivity().activity(now) })

/** The highest-priority activity in [activities], or Idle when there are none. */
internal fun leading(activities: List<AgentActivity>): AgentActivity =
  activities.minByOrNull { activity -> priority.indexOf(activity).let { if (it < 0) priority.size else it } }
    ?: AgentActivity.IDLE
