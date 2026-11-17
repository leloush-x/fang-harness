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

  /**
   * State-driven uniforms. [mixB] feeds both `mixB.x` (mute plate) and `mixB.y` (muted rim).
   * [gazeX]/[gazeY] and [pulse]/[intensity]/[halo] drive the child cells shader.
   */
  data class State(
    val time: Float = 0f,
    val userTint: Color = Color(0.60f, 0.90f, 0.98f, 1f),
    val agentTint: Color = Color(0.44f, 0.91f, 0.68f, 1f),
    val userLevel: Float = 0f,
    val agentLevel: Float = 0f,
    val mixB: Float = 0f,
    val glow: Float = 0.55f,
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

  class Handle internal constructor(
    val orb: RuntimeShader,
    val cells: RuntimeShader,
  ) {
    fun setState(state: State, layout: Layout) {
      val ppp = maxOf(layout.pixelsPerPoint, 0.001f)
      orb.setFloatUniform("grid", floatArrayOf(layout.columns, layout.pitch, layout.radius, 0f))
      orb.setFloatUniform("mixB", floatArrayOf(state.mixB, state.mixB, 0f, 0f))
      orb.setFloatUniform("tintBase", state.userTint.asFloatArray())
      orb.setFloatUniform("tintCore", state.agentTint.asFloatArray())
      orb.setFloatUniform("rim", floatArrayOf(state.userLevel, state.agentLevel, 0f, 0f))
      orb.setFloatUniform("view", floatArrayOf(layout.centreX, layout.centreY, ppp, 1f / ppp))
      orb.setFloatUniform("glow", floatArrayOf(state.glow, layout.radius, 3f, 0f))
      orb.setFloatUniform("clipRect", floatArrayOf(0f, 0f, layout.width, layout.height))
      orb.setFloatUniform("clipShape", floatArrayOf(-1f, 0f, layout.height, 1f))

      cells.setFloatUniform("grid", floatArrayOf(layout.columns, layout.pitch * 0.18f))
      cells.setFloatUniform("state", floatArrayOf(state.time, state.pulse, state.gazeX, state.gazeY))
      cells.setFloatUniform(
        "levels",
        floatArrayOf(state.userLevel, state.agentLevel, state.halo, state.intensity),
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
