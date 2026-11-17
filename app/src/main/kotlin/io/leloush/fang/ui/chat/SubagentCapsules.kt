package io.leloush.fang.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.FangOrb
import io.leloush.fang.ui.components.OrbMood
import io.leloush.fang.ui.subagents.SubagentFacts
import io.leloush.fang.ui.subagents.SubagentPhase
import io.leloush.fang.ui.subagents.SubagentText
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import kotlin.math.hypot

/** A fling at least this fast folds a capsule; a slow drag leaves it as it is. */
// shortcut: threshold tuned by eye, raise when device testing shows accidental collapses
private val FOLD_FLING = 600.dp

/** The chat's strip of subagent capsules, folded ones staying small until tapped. */
@Composable
internal fun SubagentCapsules(
  facts: List<SubagentFacts>,
  onOpen: (SubagentFacts) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (facts.isEmpty()) return
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4),
  ) {
    for (fact in facts) CapsulePill(fact = fact, onOpen = onOpen)
  }
}

/** One subagent's pill: an orb and its short name, folding to just the orb under a fling. */
@Composable
private fun CapsulePill(fact: SubagentFacts, onOpen: (SubagentFacts) -> Unit, modifier: Modifier = Modifier) {
  var folded by remember(fact.toolCallId) { mutableStateOf(false) }
  val name = fact.agent.ifBlank { fact.description.ifBlank { stringResource(R.string.capsule_unnamed) } }
  val action = stringResource(if (folded) R.string.capsule_restore else R.string.capsule_open, name)
  // The task stays available to TalkBack without widening the pill's one-line label.
  val details = fact.description.ifBlank { name }
  val mood = when {
    fact.phase.active -> OrbMood.SPEAKING
    fact.phase == SubagentPhase.Failed -> OrbMood.FAILED
    else -> OrbMood.DONE
  }
  Row(
    modifier = modifier
      // subagent-capsule-press: clip to the pill so the press ripple keeps both ends rounded
      .clip(RoundedCornerShape(50))
      .background(fangColors.surface2)
      .semantics { contentDescription = details }
      .pointerInput(fact.toolCallId) { foldOnFling { folded = true } }
      .clickable(onClickLabel = action) { if (folded) folded = false else onOpen(fact) }
      .widthIn(max = 220.dp)
      .padding(
        horizontal = if (folded) FangTokens.Spacing.s8 else FangTokens.Spacing.s12,
        vertical = if (folded) FangTokens.Spacing.s4 else FangTokens.Spacing.s8,
      ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
  ) {
    FangOrb(size = FangTokens.IconSize.sm, mood = mood)
    if (!folded) SubagentText(label = name)
  }
}

/** Folds on a fling the parent didn't consume, so scrolling the chat never folds a pill. */
private suspend fun PointerInputScope.foldOnFling(onFling: () -> Unit) {
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val tracker = VelocityTracker()
    tracker.addPointerInputChange(down)
    var consumed = down.isConsumed
    var pressed = true
    while (pressed) {
      // Final pass: see whether the parent LazyColumn consumed the drag before deciding to fold.
      val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id }
      if (change != null) {
        tracker.addPointerInputChange(change)
        if (change.isConsumed) consumed = true
        pressed = change.pressed
      }
    }
    val velocity = tracker.calculateVelocity()
    if (!consumed && hypot(velocity.x, velocity.y) >= FOLD_FLING.toPx()) onFling()
  }
}
