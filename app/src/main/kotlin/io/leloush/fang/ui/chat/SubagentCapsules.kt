package io.leloush.fang.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.components.activityglyph.ActivityGlyph
import io.leloush.fang.ui.components.activityglyph.AgentActivity
import io.leloush.fang.ui.subagents.SubagentFacts
import io.leloush.fang.ui.subagents.SubagentStatusText
import io.leloush.fang.ui.subagents.SubagentText
import io.leloush.fang.ui.subagents.subagentActivity
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import kotlin.math.hypot

/** A fling at least this fast folds an expanded panel; a slow drag leaves it as it is. */
// shortcut: threshold tuned by eye, raise when device testing shows accidental collapses
private val FOLD_FLING = 600.dp

/** The chat's strip for its subagents: one grouped capsule that expands into a list of rows. */
@Composable
internal fun SubagentCapsules(
  facts: List<SubagentFacts>,
  onOpen: (SubagentFacts) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (facts.isEmpty()) return
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier) {
    AnimatedVisibility(
      visible = !expanded,
      enter = fadeIn(animationSpec = tween(350)),
      exit = fadeOut(animationSpec = tween(350)),
    ) {
      GroupedCapsule(facts = facts, onExpand = { expanded = true })
    }
    AnimatedVisibility(
      visible = expanded,
      enter = fadeIn(animationSpec = tween(350)),
      exit = fadeOut(animationSpec = tween(350)),
    ) {
      SubagentPanel(
        facts = facts,
        onOpen = { fact ->
          expanded = false
          onOpen(fact)
        },
        onCollapse = { expanded = false },
      )
    }
  }
}

/** One rounded pill: the group's leading glyph, its count, and a chevron hinting there is a list behind it. */
@Composable
private fun GroupedCapsule(facts: List<SubagentFacts>, onExpand: () -> Unit) {
  val activity = leadingActivity(facts, System.currentTimeMillis())
  val derive = { leadingActivity(facts, System.currentTimeMillis()) }
  val label = pluralStringResource(R.plurals.capsule_count, facts.size, facts.size)
  Row(
    modifier = Modifier
      // subagent-capsule-press: clip to the pill so the press ripple keeps both ends rounded
      .clip(RoundedCornerShape(50))
      .background(fangColors.surface2)
      .semantics { contentDescription = label }
      .clickable(onClickLabel = stringResource(R.string.capsule_open_panel), onClick = onExpand)
      .widthIn(max = 220.dp)
      .padding(horizontal = FangTokens.Spacing.s12, vertical = FangTokens.Spacing.s8),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
  ) {
    ActivityGlyph(activity = activity, size = FangTokens.IconSize.sm, derive = derive)
    SubagentText(label = label, modifier = Modifier.weight(1f, fill = false))
    LucideIcon(
      icon = R.drawable.ic_lucide_chevron_down,
      contentDescription = null,
      size = FangTokens.IconSize.sm,
      tint = fangColors.foregroundMuted,
    )
  }
}

/** The expanded capsule: a count header with a collapse chevron, then one row per subagent. */
@Composable
private fun SubagentPanel(facts: List<SubagentFacts>, onOpen: (SubagentFacts) -> Unit, onCollapse: () -> Unit) {
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(FangTokens.Radius.lg))
      .background(fangColors.surface2)
      .pointerInput(Unit) { foldOnFling(onCollapse) }
      .padding(FangTokens.Spacing.s8)
      .widthIn(max = 260.dp),
    verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4),
  ) {
    PanelHeader(count = facts.size, onCollapse = onCollapse)
    for (fact in facts) PanelRow(fact = fact, onOpen = { onOpen(fact) })
  }
}

/** The panel's header: the same count as the capsule, with a chevron that folds the panel back. */
@Composable
private fun PanelHeader(count: Int, onCollapse: () -> Unit) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(50))
      .clickable(onClickLabel = stringResource(R.string.capsule_collapse), onClick = onCollapse)
      .padding(vertical = FangTokens.Spacing.s4),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
  ) {
    SubagentStatusText(label = pluralStringResource(R.plurals.capsule_count, count, count))
    LucideIcon(
      icon = R.drawable.ic_lucide_chevron_up,
      contentDescription = null,
      size = FangTokens.IconSize.sm,
      tint = fangColors.foregroundMuted,
    )
  }
}

/** One subagent in the panel: its own glyph, name and activity word; a tap opens it and folds the panel. */
@Composable
private fun PanelRow(fact: SubagentFacts, onOpen: () -> Unit) {
  val activity = fact.subagentActivity().activity()
  val derive = { fact.subagentActivity().activity() }
  val name = fact.agent.ifBlank { fact.description.ifBlank { stringResource(R.string.capsule_unnamed) } }
  // The task stays available to TalkBack without widening the row's one-line label.
  val details = fact.description.ifBlank { name }
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(FangTokens.Radius.md))
      .semantics { contentDescription = details }
      .clickable(onClickLabel = stringResource(R.string.capsule_open, name), onClick = onOpen)
      .padding(horizontal = FangTokens.Spacing.s6, vertical = FangTokens.Spacing.s4),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
  ) {
    ActivityGlyph(activity = activity, size = FangTokens.IconSize.sm, derive = derive)
    SubagentText(label = name, modifier = Modifier.weight(1f, fill = false))
    SubagentStatusText(label = stringResource(activity.statusLabel()))
  }
}

/** The compact activity word beside a row's name, one per derived glyph state. */
private fun AgentActivity.statusLabel(): Int = when (this) {
  AgentActivity.IDLE -> R.string.capsule_activity_idle

  AgentActivity.STARTUP -> R.string.capsule_activity_starting

  AgentActivity.THINKING -> R.string.capsule_activity_thinking

  AgentActivity.LONG_WORK, AgentActivity.TOOL, AgentActivity.STREAMING, AgentActivity.HANDOFF ->
    R.string.capsule_activity_working

  AgentActivity.WAITING -> R.string.capsule_activity_waiting

  AgentActivity.DONE -> R.string.capsule_activity_done

  AgentActivity.FAILED -> R.string.capsule_activity_failed
}

/** Folds on a fling the parent didn't consume, so scrolling the chat never folds a panel. */
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
