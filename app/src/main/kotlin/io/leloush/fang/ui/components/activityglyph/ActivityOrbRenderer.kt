package io.leloush.fang.ui.components.activityglyph

import android.content.Context
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import io.leloush.fang.R
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val darkBase = floatArrayOf(0.70f, 0.75f, 0.84f)
private val darkCore = floatArrayOf(0.92f, 0.95f, 1.0f)
private val lightBase = floatArrayOf(0.27f, 0.32f, 0.42f)
private val lightCore = floatArrayOf(0.09f, 0.12f, 0.2f)

/** The four phosphor accents and the two hot flash colours, from upstream's `AgentActivityOrb`. */
private class Palette(
  val base: FloatArray,
  val core: FloatArray,
  val accents: Map<ActivityAccent, Pair<FloatArray, FloatArray>>,
  val hot: Map<ActivityAccent, Pair<FloatArray, FloatArray>>,
)

private val darkPalette = Palette(
  base = darkBase,
  core = darkCore,
  accents = mapOf(
    ActivityAccent.ORANGE to (floatArrayOf(1.0f, 0.64f, 0.30f) to floatArrayOf(1.0f, 0.88f, 0.70f)),
    ActivityAccent.RED to (floatArrayOf(1.0f, 0.32f, 0.24f) to floatArrayOf(1.0f, 0.56f, 0.46f)),
    ActivityAccent.GREEN to (floatArrayOf(0.20f, 0.80f, 0.42f) to floatArrayOf(0.50f, 0.93f, 0.62f)),
    ActivityAccent.BLUE to (floatArrayOf(0.42f, 0.66f, 1.0f) to floatArrayOf(0.80f, 0.90f, 1.0f)),
  ),
  hot = mapOf(
    ActivityAccent.GREEN to (floatArrayOf(0.86f, 1.0f, 0.90f) to floatArrayOf(1f, 1f, 1f)),
    ActivityAccent.RED to (floatArrayOf(1.0f, 0.90f, 0.84f) to floatArrayOf(1f, 1f, 1f)),
  ),
)

private val lightPalette = Palette(
  base = lightBase,
  core = lightCore,
  accents = mapOf(
    ActivityAccent.ORANGE to (floatArrayOf(0.95f, 0.52f, 0.12f) to floatArrayOf(0.78f, 0.36f, 0.02f)),
    ActivityAccent.RED to (floatArrayOf(0.85f, 0.17f, 0.12f) to floatArrayOf(0.66f, 0.08f, 0.05f)),
    ActivityAccent.GREEN to (floatArrayOf(0.06f, 0.55f, 0.26f) to floatArrayOf(0.02f, 0.42f, 0.18f)),
    ActivityAccent.BLUE to (floatArrayOf(0.1f, 0.45f, 0.95f) to floatArrayOf(0.02f, 0.28f, 0.72f)),
  ),
  hot = mapOf(
    ActivityAccent.GREEN to (floatArrayOf(0.36f, 0.86f, 0.52f) to floatArrayOf(0.22f, 0.76f, 0.40f)),
    ActivityAccent.RED to (floatArrayOf(1.0f, 0.46f, 0.36f) to floatArrayOf(0.96f, 0.32f, 0.24f)),
  ),
)

/** The one shader and paint; uniforms are set immediately before each draw, on the main thread. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class ActivityOrbHandle(val shader: RuntimeShader) {
  val paint = Paint().apply { shader = this@ActivityOrbHandle.shader }
}

/** The packed uniforms for one frame. */
internal class Packed {
  val header0 = FloatArray(4)
  val header1 = FloatArray(4)
  val glowRGB = FloatArray(3)
  val neutral = FloatArray(3)
  val neutralCore = FloatArray(3)
  val accent = FloatArray(3)
  val accentCore = FloatArray(3)
  val cells = FloatArray(49 * 4)
}

/** Binds the recovered AGLS field to a single draw call, as upstream's `AgentActivityOrb.pack` does. */
internal object ActivityOrbRenderer {
  private var cached: ActivityOrbHandle? = null
  private val scratch = Packed()

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  fun handle(context: Context): ActivityOrbHandle {
    cached?.let { return it }
    val source = context.resources.openRawResource(R.raw.activity_orb).use { it.readBytes().toString(Charsets.UTF_8) }
    return ActivityOrbHandle(RuntimeShader(source)).also { cached = it }
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  fun DrawScope.drawOrb(frame: CellFrame, sizeDp: Float, dark: Boolean, context: Context) {
    val box = minOf(size.width, size.height)
    val margin = (sizeDp * 0.42f).roundToInt().toFloat() * density
    val side = box + 2 * margin
    val packed = pack(frame, side, sizeDp, density, dark, scratch)
    val handle = handle(context)
    handle.shader.setFloatUniform("header0", packed.header0)
    handle.shader.setFloatUniform("header1", packed.header1)
    handle.shader.setFloatUniform("glowRGB", packed.glowRGB)
    handle.shader.setFloatUniform("neutral", packed.neutral)
    handle.shader.setFloatUniform("neutralCore", packed.neutralCore)
    handle.shader.setFloatUniform("accent", packed.accent)
    handle.shader.setFloatUniform("accentCore", packed.accentCore)
    handle.shader.setFloatUniform("cells", packed.cells)
    translate((size.width - box) / 2 - margin, (size.height - box) / 2 - margin) {
      drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, side, side, handle.paint) }
    }
  }

  fun pack(frame: CellFrame, area: Float, sizeDp: Float, density: Float, dark: Boolean, out: Packed): Packed {
    val palette = if (dark) darkPalette else lightPalette
    val pair = frame.accent?.let { palette.accents[it] }
    val accent = pair?.first ?: palette.base
    val accentCore = pair?.second ?: palette.core
    var glowRGB = mix(
      glowColor(palette.base, palette.core, dark),
      glowColor(accent, accentCore, dark),
      if (pair ==
        null
      ) {
        0f
      } else {
        min(1f, frame.tint)
      },
    )
    var neutral = palette.base
    var neutralCore = palette.core
    val heat = clamp(frame.heat, 0f, 1f)
    if (heat > 1e-3) {
      val hot = frame.accent?.let { palette.hot[it] }
      if (hot != null) {
        neutral = mix(neutral, hot.first, heat)
        neutralCore = mix(neutralCore, hot.second, heat)
        glowRGB = mix(glowRGB, glowColor(hot.first, hot.second, dark), heat)
      }
    }
    val diameter = sizeDp - if (sizeDp >= 24f) 4f else 2f
    val pitch = max(2, (diameter * density / 7).roundToInt()).toFloat()
    val origin = floor((area - 7 * pitch) / 2)
    val peak = (if (dark) 0.3f else 0.19f) * 0.55f * 1.5f * (if (dark) 1f else 0.45f)
    out.header0[0] = origin
    out.header0[1] = origin
    out.header0[2] = pitch
    out.header0[3] = 1f
    out.header1[0] = frame.glow
    out.header1[1] = peak
    out.header1[2] = if (dark) 1f else 0f
    out.header1[3] =
      if (dark) 0.06f else 0.075f
    put(out.glowRGB, glowRGB)
    put(out.neutral, neutral)
    put(out.neutralCore, neutralCore)
    put(out.accent, accent)
    put(out.accentCore, accentCore)
    frame.cells.copyInto(out.cells)
    return out
  }

  private fun clamp(x: Float, a: Float, b: Float): Float = if (x < a) {
    a
  } else if (x > b) {
    b
  } else {
    x
  }

  private fun put(out: FloatArray, rgb: FloatArray) {
    out[0] = rgb[0]
    out[1] = rgb[1]
    out[2] = rgb[2]
  }

  private fun mix(a: FloatArray, b: FloatArray, t: Float): FloatArray = FloatArray(3) { a[it] + (b[it] - a[it]) * t }

  private fun saturate(c: FloatArray, k: Float): FloatArray {
    val l = c[0] * 0.299f + c[1] * 0.587f + c[2] * 0.114f
    return FloatArray(3) { clamp(l + (c[it] - l) * k, 0f, 1f) }
  }

  private fun glowColor(a: FloatArray, b: FloatArray, dark: Boolean): FloatArray {
    val c = if (dark) saturate(mix(a, b, 0.5f), 1.5f) else mix(saturate(a, 1.95f), floatArrayOf(1f, 1f, 1f), 0.15f)
    return FloatArray(3) { clamp(c[it], 0f, 1f) }
  }
}
