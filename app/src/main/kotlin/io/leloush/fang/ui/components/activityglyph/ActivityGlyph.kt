package io.leloush.fang.ui.components.activityglyph

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.leloush.fang.ui.theme.fangColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

/** 60 Hz looks better than 30; upstream's frame gate. */
private const val FRAME_INTERVAL = 1.0 / 60.0

/** The small activity glyph: one 7x7 dot disc, animated without recomposing its row. */
@Composable
fun ActivityGlyph(
  activity: AgentActivity,
  modifier: Modifier = Modifier,
  size: Dp = 18.dp,
  isPaused: Boolean = false,
  reduceMotion: Boolean = false,
  contentDescription: String? = null,
  derive: (() -> AgentActivity)? = null,
) {
  val model = remember { ActivityCellField() }
  val context = LocalContext.current
  val dark = fangColors.isDark
  val current = rememberUpdatedState(activity)
  val deriveState = rememberUpdatedState(derive)
  val paused = rememberUpdatedState(isPaused)
  val reduced = rememberUpdatedState(reduceMotion)
  // Bumped per drawn frame; only a Canvas draw reads it, so frames never recompose this row.
  val tick = remember { mutableLongStateOf(0L) }
  RunGlyphFrames(model, tick, current, deriveState, paused, reduced, dark, isPaused, reduceMotion)
  val description = contentDescription
  val described = if (description == null) modifier else modifier.semantics { contentDescription = description }
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ShaderActivityGlyph(described, size, model, dark, tick, context)
  } else {
    FallbackActivityGlyph(described, size, model.frame.accent)
  }
}

/** Drives the cell field on the frame clock; state reads stay in the effect so the row never recomposes. */
@Composable
private fun RunGlyphFrames(
  model: ActivityCellField,
  tick: androidx.compose.runtime.MutableState<Long>,
  current: State<AgentActivity>,
  derive: State<(() -> AgentActivity)?>,
  paused: State<Boolean>,
  reduced: State<Boolean>,
  dark: Boolean,
  isPaused: Boolean,
  reduceMotion: Boolean,
) {
  var shown by remember { mutableStateOf(current.value) }
  LaunchedEffect(dark, isPaused, reduceMotion) {
    fun target(): AgentActivity = derive.value?.invoke() ?: current.value
    fun draw(now: Double) {
      val state = target()
      model.advance(now, state, reduced.value)
      tick.value += 1
      if (shown != state) shown = state
    }
    if (paused.value) {
      snapshotFlow { current.value }.collect { draw(nowSeconds()) }
      return@LaunchedEffect
    }
    var lastDrawn = Double.NEGATIVE_INFINITY
    while (isActive) {
      val state = target()
      val settled = state == shown && model.isResting
      when {
        settled && state.isFinal -> {
          if (derive.value != null) delay(500) else snapshotFlow { current.value }.first { it != state }
        }

        settled && reduced.value -> {
          delay(500)
          draw(nowSeconds())
        }

        else -> {
          withFrameNanos { }
          val now = nowSeconds()
          if (now - lastDrawn >= FRAME_INTERVAL - 0.002) {
            lastDrawn = now
            draw(now)
          }
        }
      }
    }
  }
}

private fun nowSeconds(): Double = System.currentTimeMillis() / 1000.0

/** The API 33+ path: one RuntimeShader pass, with the tick read only inside the draw. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderActivityGlyph(
  modifier: Modifier,
  size: Dp,
  model: ActivityCellField,
  dark: Boolean,
  tick: State<Long>,
  context: Context,
) {
  Canvas(modifier.size(size)) {
    tick.value
    with(ActivityOrbRenderer) { drawOrb(model.frame, size.value, dark, context) }
  }
}

/** Fang's minSdk is 29: below API 33 draw the same dot grid as a still disc with Compose primitives. */
@Composable
private fun FallbackActivityGlyph(modifier: Modifier, size: Dp, accent: ActivityAccent?) {
  val color = fallbackColor(accent)
  Canvas(modifier.size(size)) {
    val px = this.size
    val pitch = minOf(px.width, px.height) / 9f
    val centre = Offset(px.width / 2, px.height / 2)
    for (cell in OrbCells.seven) {
      val at = centre + Offset((cell.column - 3) * pitch, (cell.row - 3) * pitch)
      drawCircle(color, radius = pitch * 0.18f * cell.size.toFloat(), center = at)
    }
  }
}

private fun fallbackColor(accent: ActivityAccent?): Color = when (accent) {
  ActivityAccent.ORANGE -> Color(1.0f, 0.64f, 0.30f)
  ActivityAccent.RED -> Color(1.0f, 0.32f, 0.24f)
  ActivityAccent.GREEN -> Color(0.20f, 0.80f, 0.42f)
  ActivityAccent.BLUE -> Color(0.42f, 0.66f, 1.0f)
  null -> Color(0.70f, 0.75f, 0.84f)
}
