package io.leloush.fang.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/** A step's icon on a rounded tile; it turns into an accent check, with a small bounce, once the step is [done]. */
@Composable
internal fun IconTile(@DrawableRes icon: Int, done: Boolean, modifier: Modifier = Modifier) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.xxl + FangTokens.Spacing.s8)
  AnimatedContent(
    targetState = done,
    modifier = modifier.size(TILE_SIZE),
    transitionSpec = {
      (
        fadeIn(tween(FADE_MILLIS)) +
          scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.6f)
        ).togetherWith(fadeOut(tween(FADE_MILLIS / 2)))
    },
    label = "step icon",
  ) { isDone ->
    val tile = Modifier.fillMaxSize()
    if (isDone) {
      Box(tile.background(colors.accent, shape), contentAlignment = Alignment.Center) {
        LucideIcon(
          icon = R.drawable.ic_lucide_check,
          contentDescription = stringResource(R.string.onboarding_status_done),
          size = ICON_SIZE,
          tint = colors.accentForeground,
        )
      }
    } else {
      Box(
        tile.background(colors.surface1, shape).border(1.dp, colors.border, shape),
        contentAlignment = Alignment.Center,
      ) {
        LucideIcon(icon = icon, contentDescription = null, size = ICON_SIZE, tint = colors.foreground)
      }
    }
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
      val width by animateDpAsState(if (index == current) DOT_CURRENT_WIDTH else DOT_SIZE, label = "dot width")
      val color by animateColorAsState(
        when {
          index == current -> colors.accent
          isDone -> colors.accent.copy(alpha = DONE_DOT_ALPHA)
          else -> colors.borderAccent
        },
        label = "dot color",
      )
      Box(Modifier.size(width, DOT_SIZE).background(color, CircleShape))
    }
  }
}

private val TILE_SIZE = 88.dp
private val ICON_SIZE = 40.dp
private const val FADE_MILLIS = 240
private val DOT_SIZE = 6.dp
private val DOT_CURRENT_WIDTH = 20.dp
private const val DONE_DOT_ALPHA = 0.45f
