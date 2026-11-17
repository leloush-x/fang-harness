package io.leloush.fang.ui.components

import android.content.Context
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.leloush.fang.R
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Loads shader_03 and binds the recovered Wisp Orb uniforms. Requires API 33+; below that the caller falls back
 * to the simmering pot. All nine AGSL uniforms are packed exactly as the original renderer does.
 */
object OrbShader {
  /** EmptyGlyphBreath: omega = 2*pi / max(0.2, period), value = 0.5 - cos(phase) * 0.5. */
  const val BREATH_SECONDS: Float = 1.7f
  const val FINALE_SECONDS: Float = 0.55f
  const val HANDOFF_SECONDS: Float = 0.34f

  /** Exponential colour ramp time constant (b0e.c). */
  const val COLOR_TAU: Float = 0.055f

  /** Original spring integrator tick; the Compose path uses continuous exponential smoothing instead. */
  const val SPRING_TICK: Float = 1f / 240f

  /** Shader constants, kept public for callers that theme or compare against the fixed kit palette. */
  val USER_BASE = Color(0.60f, 0.90f, 0.98f)
  val AGENT_BASE = Color(0.44f, 0.91f, 0.68f)
  val MUTED_PLATE = Color(26f / 255f, 14f / 255f, 16f / 255f)
  val MUTED_RIM = Color(248f / 255f, 113f / 255f, 113f / 255f)

  /** The original palette/mode enum has seven values, not five. */
  enum class Palette { SLEEPING, CONNECTING, LISTENING, SPEAKING, BOTH_SPEAKING, THINKING, CONFIRMING }

  /** Base/core RGB plus the per-mode user/agent gains (p5d.g and p5d.k). */
  data class Hues(val base: Color, val core: Color, val userGain: Float, val agentGain: Float)

  /** All sixteen tuning defaults recovered from WispOrbTuning. */
  data class Tuning(
    val diameter: Float = 88f,
    val restScale: Float = 0.76f,
    val dotPitch: Float = 2.2f,
    val dotSize: Float = 0.78f,
    val glow: Float = 0.42f,
    val brightness: Float = 1.15f,
    val contrast: Float = 2.6f,
    val response: Float = 2.9f,
    val interference: Float = 0.68f,
    val phosphor: Float = 0.62f,
    val gazeBlend: Float = 0.92f,
    val gazeReachDeg: Float = 40f,
    val pupilSize: Float = 0.19f,
    val pupilWell: Float = 0.96f,
    val irisWidth: Float = 0.30f,
    val rimGlow: Float = 0.85f,
  )

  /** Per-frame driver. [breath] is the exact EmptyGlyphBreath value; [link] is tintCore.w (-1 when absent). */
  data class State(
    val time: Float = 0f,
    val palette: Palette = Palette.LISTENING,
    val youLevel: Float = 0f,
    val agentLevel: Float = 0f,
    val confirmWeight: Float = 0f,
    val mutedWeight: Float = 0f,
    val isLive: Boolean = false,
    val link: Float = -1f,
    val breath: Float = 0f,
    val pulse: Float = 0f,
    val intensity: Float = 1f,
    val halo: Float = 0f,
    val gazeX: Float = 0f,
    val gazeY: Float = 0f,
    val glowGain: Float = 1f,
    val tuning: Tuning = Tuning(),
  )

  /** Canvas geometry: [pixelsPerPoint] is the shader's view.z, from which view.w (aa) is derived. */
  data class Layout(
    val width: Float,
    val height: Float,
    val centreX: Float,
    val centreY: Float,
    val pixelsPerPoint: Float,
  )

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  class Handle internal constructor(val orb: RuntimeShader) {
    private var columns = -1
    private var cellBitmap: OrbCellBitmap? = null
    private var lastTime = -1f
    private var base = Color.Transparent
    private var core = Color.Transparent

    fun setState(state: State, layout: Layout) {
      val tune = state.tuning
      val cols = max(8, min(160, ceil(tune.diameter / tune.dotPitch).toInt()))
      val radius = max((1f - state.breath * 0.045f) * 0.5f * tune.diameter * tune.restScale, 4f)
      if (cols != columns) {
        columns = cols
        cellBitmap = OrbCellBitmap(cols).also { orb.setInputBuffer("cells", it.shader) }
      }
      val hues = huesFor(state.palette)
      val dt = if (lastTime < 0f) 0f else max(0f, state.time - lastTime)
      lastTime = state.time
      base = rampColor(base, hues.base, dt)
      core = rampColor(core, hues.core, dt)
      val ppp = max(layout.pixelsPerPoint, 0.001f)
      val aa = max((1f / ppp) * 0.78f, 1e-4f)
      val fade = max(0f, 1f - 0.85f / radius)
      orb.setFloatUniform("grid", floatArrayOf(cols.toFloat(), tune.dotPitch, radius, fade * fade))
      orb.setFloatUniform("mixB", floatArrayOf(state.confirmWeight, state.mutedWeight, 0f, 0f))
      orb.setFloatUniform("tintBase", floatArrayOf(base.red, base.green, base.blue, if (state.isLive) 1f else 0f))
      orb.setFloatUniform("tintCore", floatArrayOf(core.red, core.green, core.blue, state.link))
      orb.setFloatUniform("rim", floatArrayOf(ramp(state.youLevel), ramp(state.agentLevel), 0f, 0f))
      orb.setFloatUniform("view", floatArrayOf(layout.centreX, layout.centreY, ppp, aa))
      val g = 0.85f + 0.15f * state.mutedWeight
      orb.setFloatUniform(
        "glow",
        floatArrayOf(
          g.coerceIn(0f, 1f) * 0.17f,
          max((min(g, 1f) * 0.35f + 0.82f) * radius, 0.001f),
          ((tune.dotSize / 0.78f) * (tune.dotPitch * 0.84f) + aa) / tune.dotPitch,
          state.glowGain * 2.8f,
        ),
      )
      val corner = min(8f * ppp, min(layout.width, layout.height) * 0.5f * 0.18f)
      orb.setFloatUniform("clipRect", floatArrayOf(0f, 0f, layout.width, layout.height))
      orb.setFloatUniform(
        "clipShape",
        floatArrayOf(corner, corner, layout.height + radius * ppp, max(1f, layout.height * 0.5f)),
      )
      cellBitmap?.update(state, base, core, hues.userGain, hues.agentGain)
    }

    /** ramp(0.02, 0.3, level) from the original rim computation. */
    private fun ramp(level: Float): Float = ((level - 0.02f) / 0.28f).coerceIn(0f, 1f)

    /** b0e.c: exponential approach with tau = 0.055 s; the first frame snaps to the target. */
    private fun rampColor(current: Color, target: Color, dt: Float): Color {
      if (current == Color.Transparent || dt <= 0f) return target
      val t = 1f - exp(-dt / COLOR_TAU)
      return Color(
        current.red + (target.red - current.red) * t,
        current.green + (target.green - current.green) * t,
        current.blue + (target.blue - current.blue) * t,
        1f,
      )
    }
  }

  fun readRawShader(context: Context, resId: Int): String =
    context.resources.openRawResource(resId).use { it.readBytes().toString(Charsets.UTF_8) }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  fun create(context: Context): Handle = Handle(RuntimeShader(readRawShader(context, R.raw.shader_03)))

  /** Creates the shader once per context; returns null below API 33 so callers can fall back. */
  @Composable
  fun rememberOrbShaders(): Handle? {
    val context = LocalContext.current
    return remember(context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) create(context) else null
    }
  }
}

/** p5d.g + p5d.k: exact base/core RGB and gains for each of the seven modes. */
private fun huesFor(palette: OrbShader.Palette): OrbShader.Hues = when (palette) {
  OrbShader.Palette.SLEEPING -> OrbShader.Hues(
    Color(0.52f, 0.70f, 0.65f),
    Color(0.84f, 0.93f, 0.90f),
    0.38f,
    0.08f,
  )

  OrbShader.Palette.CONNECTING -> OrbShader.Hues(
    Color(0.56f, 0.85f, 0.95f),
    Color(0.93f, 0.98f, 1.00f),
    0.25f,
    0.25f,
  )

  OrbShader.Palette.LISTENING -> OrbShader.Hues(
    Color(0.64f, 0.90f, 0.96f),
    Color(0.97f, 0.99f, 1.00f),
    1.0f,
    0.35f,
  )

  OrbShader.Palette.SPEAKING -> OrbShader.Hues(
    Color(0.44f, 0.91f, 0.68f),
    Color(0.99f, 0.93f, 0.72f),
    0.35f,
    1.0f,
  )

  OrbShader.Palette.BOTH_SPEAKING -> OrbShader.Hues(
    Color(0.56f, 0.92f, 0.82f),
    Color(0.99f, 0.96f, 0.82f),
    1.0f,
    1.0f,
  )

  OrbShader.Palette.THINKING -> OrbShader.Hues(
    Color(0.66f, 0.54f, 0.98f),
    Color(0.94f, 0.89f, 1.00f),
    0.30f,
    0.20f,
  )

  OrbShader.Palette.CONFIRMING -> OrbShader.Hues(
    Color(0.96f, 0.35f, 0.28f),
    Color(1.00f, 0.78f, 0.60f),
    0.35f,
    0.20f,
  )
}
