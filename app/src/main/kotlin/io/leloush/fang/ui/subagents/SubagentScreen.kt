package io.leloush.fang.ui.subagents

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.agent.AgentProcess
import io.leloush.fang.ui.components.HeaderBar
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import kotlinx.coroutines.launch

/** How far a pushed page slides in from the right, and how long the slide or fade takes. */
private val SlideIn = 72.dp
private const val MOTION_MILLIS = 180

/**
 * The subagent whose capsule was tapped, shown over the parent session. A subagent started from this
 * transcript pushes another page on top; Back pops one page, or closes the overlay on the root. Follows
 * each open run's live log, lets the user steer it while it runs, and keeps a covered parent's state.
 */
@Composable
internal fun SubagentChatScreen(
  process: AgentProcess,
  fact: SubagentFacts,
  onClose: () -> Unit,
  onError: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val stack = rememberSubagentStack()
  val entries = listOf(fact) + stack.pushed
  val openIds = remember(entries) { entries.mapTo(HashSet()) { it.toolCallId } }
  // A single handler: covered pages do not register one, so Back always acts on the top page.
  BackHandler { if (!stack.pop()) onClose() }
  Box(modifier = modifier.fillMaxSize().background(fangColors.surface0)) {
    entries.forEachIndexed { index, entry ->
      key(entry.toolCallId) {
        SubagentPage(process, entry, stack, index == entries.lastIndex, openIds, onClose, onError)
      }
    }
  }
}

/** One page of the stack: its transcript, header, status and steering box. */
@Composable
private fun SubagentPage(
  process: AgentProcess,
  fact: SubagentFacts,
  stack: SubagentStackState,
  isTop: Boolean,
  openIds: Set<String>,
  onClose: () -> Unit,
  onError: (String) -> Unit,
) {
  // The run leaves the list once it ends; keep the id followed when the page opened so it cannot change mid-view.
  val runId = remember(fact.toolCallId) { fact.run?.id ?: fact.toolCallId }
  FollowRun(process, runId)
  val name = fact.agent.ifBlank { stringResource(R.string.capsule_unnamed) }
  val active = fact.phase.active
  val failed = stringResource(R.string.subagent_chat_steer_failed)
  val scope = rememberCoroutineScope()
  val send: (String) -> Unit = { text ->
    scope.launch {
      if (!process.steerSubagent(runId, text)) onError(failed)
    }
  }
  val isRoot = fact.toolCallId !in stack.pushed.map { it.toolCallId }
  val backLabel = stringResource(if (isRoot) R.string.subagent_back else R.string.subagent_stack_back_to_parent)
  val reduceMotion = rememberReduceMotion()
  var shown by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { shown = true }
  val slide by animateDpAsState(
    targetValue = if (shown || !isTop) 0.dp else SlideIn,
    animationSpec = tween(if (reduceMotion) 0 else MOTION_MILLIS),
    label = "subagent-slide",
  )
  val fade by animateFloatAsState(
    targetValue = if (shown) 1f else 0f,
    animationSpec = tween(if (reduceMotion) 0 else MOTION_MILLIS),
    label = "subagent-fade",
  )
  Box(
    Modifier.fillMaxSize().offset(x = if (reduceMotion) 0.dp else slide).alpha(if (isTop) fade else 0f)
      .then(if (isTop) Modifier else inert()),
  ) {
    Column(Modifier.fillMaxSize()) {
      SubagentHeader(name, fact.description, active, backLabel) { if (!stack.pop()) onClose() }
      if (!active) {
        Text(
          text = stringResource(R.string.subagent_chat_finished),
          color = fangColors.foregroundMuted,
          fontSize = FangTokens.FontSize.sm,
          modifier = Modifier.fillMaxWidth().padding(
            horizontal = FangTokens.Spacing.s16,
            vertical = FangTokens.Spacing.s8,
          ),
        )
      }
      ChatSubagentScreen(process, runId, openIds, { stack.push(it) }, Modifier.weight(1f))
      if (active) SubagentComposer(stack.draft(fact.toolCallId), name, send)
    }
  }
}

/** Follows [runId]'s log while this page shows, stopping the extension's events when it leaves the stack. */
@Composable
private fun FollowRun(process: AgentProcess, runId: String) {
  LaunchedEffect(process, runId) { process.liveSubagents.follow(runId) }
  DisposableEffect(process, runId) { onDispose { process.liveSubagents.unfollow(runId) } }
}

/** Keeps a covered page mounted and laid out, but takes its touches and hides it from TalkBack. */
private fun Modifier.inert(): Modifier = clearAndSetSemantics {}
  .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }

/** The transcript's header: the run's name and task, a Back button and, while it runs, a status word. */
@Composable
private fun SubagentHeader(name: String, description: String, active: Boolean, backLabel: String, onBack: () -> Unit) {
  HeaderBar(
    title = stringResource(R.string.subagent_title, name, description),
    leading = {
      IconButton28(
        icon = R.drawable.ic_lucide_chevron_left,
        contentDescription = backLabel,
        onClick = onBack,
      )
    },
    trailing = {
      if (active) {
        Text(
          text = stringResource(R.string.subagent_chat_running),
          color = fangColors.foregroundMuted,
          fontSize = FangTokens.FontSize.sm,
        )
      }
    },
  )
}
