package io.leloush.fang.ui.subagents

import android.provider.Settings
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * The subagents pushed over the one the chat opened, bottom first, and their steering drafts. The root
 * stays with the caller, so its live status keeps updating; only nested pages live here. Covered pages
 * stay composed, so a parent's transcript and scroll keep their state while a child is on top.
 */
@Stable
internal class SubagentStack {
  var pushed by mutableStateOf<List<SubagentFacts>>(emptyList())
    private set

  private val drafts = mutableMapOf<String, TextFieldState>()

  /** The steering draft for [id], kept for as long as this overlay is composed. */
  fun draft(id: String): TextFieldState = drafts.getOrPut(id) { TextFieldState() }

  fun push(fact: SubagentFacts) {
    if (fact.toolCallId !in pushed.map { it.toolCallId }) pushed = pushed + fact
  }

  /** Pops one page; false means none were pushed and the caller should close the whole overlay. */
  fun pop(): Boolean {
    if (pushed.isEmpty()) return false
    pushed = pushed.dropLast(1)
    return true
  }
}

/** The stack and drafts for one open subagent overlay. */
@Composable
internal fun rememberSubagentStack(): SubagentStack = remember { SubagentStack() }

/** Android's Reduce Motion (animator duration scale 0), read once per context. */
@Composable
internal fun rememberReduceMotion(): Boolean {
  val context = LocalContext.current
  return remember(context) {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
  }
}
