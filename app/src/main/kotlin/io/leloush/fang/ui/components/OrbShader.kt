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

/** Loads the extracted Wisp Orb AGSL sources and binds their uniforms. Requires API 33+. */
object OrbShader {
  /** The orb's breath period, from the extracted shader math. */
  const val BREATH_SECONDS: Float = 3.8f

  /** The kit's fixed palette, from shader_03; the app themes user/agent tints before passing them in. */
  val USER_BASE = Color(0.60f, 0.90f, 0.98f)
  val AGENT_BASE = Color(0.44f, 0.91f, 0.68f)
  val MUTED_PLATE = Color(26f / 255f, 14f / 255f, 16f / 255f)
  val MUTED_RIM = Color(248f / 255f, 113f / 255f, 113f / 255f)

  /**
   * State-driven uniforms. [mixBX] fades the plate toward muted, [mixBY] mutes the rim;
   * [rimX]/[rimY] are the two sides' levels and [glowX] the core glow. [burst] is the Done flash.
   * A null [tintBase]/[tintCore] keeps the old behaviour of tracking [userTint]/[agentTint].
   */
  data class State(
    val time: Float = 0f,
    val userTint: Color = USER_BASE,
    val agentTint: Color = AGENT_BASE,
    val mutedPlate: Color = MUTED_PLATE,
    val mutedRim: Color = MUTED_RIM,
    val tintBase: Color? = null,
    val tintCore: Color? = null,
    val userLevel: Float = 0f,
    val agentLevel: Float = 0f,
    val mixBX: Float = 0f,
    val mixBY: Float = 0f,
    val rimX: Float = 0f,
    val rimY: Float = 0f,
    val glowX: Float = 0.55f,
    val burst: Float = 0f,
    val pulse: Float = 0f,
    val gazeX: Float = 0f,
    val gazeY: Float = 0f,
    val intensity: Float = 1f,
    val halo: Float = 0f,
  )

  /** Pixel geometry used by the orb shader's `grid`, `view` and clip uniforms. */
  data class Layout(
    val width: Float,
    val height: Float,
    val centreX: Float,
    val centreY: Float,
    val radius: Float,
    val pitch: Float,
    val columns: Float,
    val pixelsPerPoint: Float,
  )

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  class Handle internal constructor(val orb: RuntimeShader, val cells: RuntimeShader) {
    fun setState(state: State, layout: Layout) {
      val ppp = maxOf(layout.pixelsPerPoint, 0.001f)
      orb.setFloatUniform("grid", floatArrayOf(layout.columns, layout.pitch, layout.radius, 0f))
      orb.setFloatUniform("mixB", floatArrayOf(state.mixBX, state.mixBY, 0f, 0f))
      orb.setFloatUniform("tintBase", (state.tintBase ?: state.userTint).asFloatArray())
      orb.setFloatUniform("tintCore", (state.tintCore ?: state.agentTint).asFloatArray())
      orb.setFloatUniform("rim", floatArrayOf(state.rimX, state.rimY, 0f, 0f))
      orb.setFloatUniform("view", floatArrayOf(layout.centreX, layout.centreY, ppp, 1f / ppp))
      orb.setFloatUniform("glow", floatArrayOf(state.glowX, layout.radius, 3f, 0f))
      orb.setFloatUniform("clipRect", floatArrayOf(0f, 0f, layout.width, layout.height))
      orb.setFloatUniform("clipShape", floatArrayOf(-1f, 0f, layout.height, 1f))

      cells.setFloatUniform("grid", floatArrayOf(layout.columns, layout.pitch * 0.18f))
      cells.setFloatUniform("state", floatArrayOf(state.time, state.pulse, state.gazeX, state.gazeY))
      cells.setFloatUniform(
        "levels",
        floatArrayOf(
          state.userLevel,
          state.agentLevel,
          (state.halo + state.burst).coerceAtMost(1f),
          state.intensity,
        ),
      )
      cells.setFloatUniform("you", state.userTint.asFloatArray())
      cells.setFloatUniform("agent", state.agentTint.asFloatArray())
    }
  }

  fun readRawShader(context: Context, resId: Int): String =
    context.resources.openRawResource(resId).use { it.readBytes().toString(Charsets.UTF_8) }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  fun create(context: Context): Handle {
    val orb = RuntimeShader(readRawShader(context, R.raw.shader_03))
    val cells = RuntimeShader(readRawShader(context, R.raw.shader_07))
    orb.setInputShader("cells", cells)
    return Handle(orb, cells)
  }

  /** Creates the shader pair once per context; returns null below API 33 so callers can fall back. */
  @Composable
  fun rememberOrbShaders(): Handle? {
    val context = LocalContext.current
    return remember(context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) create(context) else null
    }
  }
}

private fun Color.asFloatArray(): FloatArray = floatArrayOf(red, green, blue, alpha)
