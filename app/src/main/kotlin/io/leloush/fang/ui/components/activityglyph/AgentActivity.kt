package io.leloush.fang.ui.components.activityglyph

/** The ten visual states of the small agent-activity glyph. */
enum class AgentActivity {
  IDLE,
  STARTUP,
  THINKING,
  LONG_WORK,
  TOOL,
  STREAMING,
  WAITING,
  HANDOFF,
  DONE,
  FAILED,
}

/** Done and Failed flash, settle and hold; every other state keeps moving. */
internal val AgentActivity.isFinal: Boolean
  get() = this == AgentActivity.DONE || this == AgentActivity.FAILED
