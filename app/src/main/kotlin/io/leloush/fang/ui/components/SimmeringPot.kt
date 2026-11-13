package io.leloush.fang.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import io.leloush.fang.R
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** The launcher icon's area around the pot, in its group's coordinates, with room for the steam to rise. */
private const val VIEW_LEFT = 85f
private const val VIEW_TOP = 62f
private const val VIEW_SIZE = 356f

/**
 * Fang Harness's loading indicator: the launcher icon's pot, drawn at rest with no animation loop so it shows
 * instantly instead of stepping frames.
 */
@Composable
fun SimmeringPot(
  modifier: Modifier = Modifier,
  size: Dp = FangTokens.IconSize.md,
  tint: Color = fangColors.foregroundMuted,
) {
  val icon = ImageVector.vectorResource(R.drawable.ic_launcher_foreground)
  val pot = remember(icon) { Pot(icon) }
  Canvas(modifier.size(size)) { pot.draw(this, tint) }
}

/** The launcher icon's parts as paths. */
private class Pot(icon: ImageVector) {
  private val handle = icon.path("handle")
  private val legs = icon.path("legs")
  private val body = icon.path("body")
  private val lid = icon.path("lid")
  private val legStroke = Stroke(width = icon.vectorPath("legs").strokeLineWidth, cap = StrokeCap.Round)

  fun draw(scope: DrawScope, tint: Color) = with(scope) {
    val scale = size.minDimension / VIEW_SIZE
    withTransform({
      scale(scale, scale, pivot = Offset.Zero)
      translate(-VIEW_LEFT, -VIEW_TOP)
    }) {
      drawPath(handle, tint)
      drawPath(legs, tint, style = legStroke)
      drawPath(body, tint)
      drawPath(lid, tint)
    }
  }
}

private fun ImageVector.vectorPath(name: String): VectorPath = root.group(name).first() as VectorPath

private fun ImageVector.path(name: String): Path {
  val vector = vectorPath(name)
  return vector.pathData.toPath().apply { fillType = vector.pathFillType }
}

private fun VectorGroup.group(name: String): VectorGroup = asSequence()
  .filterIsInstance<VectorGroup>()
  .flatMap { sequenceOf(it) + it.asSequence().filterIsInstance<VectorGroup>() }
  .first { it.name == name }
