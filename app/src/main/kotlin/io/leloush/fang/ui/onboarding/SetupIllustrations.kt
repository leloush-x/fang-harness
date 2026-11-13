package io.leloush.fang.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** A step's icon on a rounded tile; it turns into an accent check once the step is [done]. */
@Composable
internal fun IconTile(@DrawableRes icon: Int, done: Boolean, modifier: Modifier = Modifier) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.xxl + FangTokens.Spacing.s8)
  val tile = modifier.size(TILE_SIZE)
    .background(if (done) colors.accent else colors.surface1, shape)
  Box(
    modifier = if (done) tile else tile.border(1.dp, colors.border, shape),
    contentAlignment = Alignment.Center,
  ) {
    LucideIcon(
      icon = if (done) R.drawable.ic_lucide_check else icon,
      contentDescription = if (done) stringResource(R.string.onboarding_status_done) else null,
      size = ICON_SIZE,
      tint = if (done) colors.accentForeground else colors.foreground,
    )
  }
}

/** Where onboarding is: a dot per step, the current one stretched, finished ones in accent. */
@Composable
internal fun ProgressDots(done: List<Boolean>, current: Int, modifier: Modifier = Modifier) {
  val colors = fangColors
  val description = stringResource(R.string.onboarding_progress, current + 1, done.size)
  Row(
    modifier = modifier.clearAndSetSemantics { contentDescription = description },
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    done.forEachIndexed { index, isDone ->
      val width = if (index == current) DOT_CURRENT_WIDTH else DOT_SIZE
      val color = when {
        index == current -> colors.accent
        isDone -> colors.accent.copy(alpha = DONE_DOT_ALPHA)
        else -> colors.borderAccent
      }
      Box(Modifier.size(width, DOT_SIZE).background(color, CircleShape))
    }
  }
}

private val TILE_SIZE = 88.dp
private val ICON_SIZE = 40.dp
private val DOT_SIZE = 6.dp
private val DOT_CURRENT_WIDTH = 20.dp
private const val DONE_DOT_ALPHA = 0.45f
