package io.leloush.fang.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ColorSpace
import android.graphics.Shader
import android.os.Build
import android.util.Half
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The `cells` input to shader_03: a cols*2 by cols RGBA_F16 / EXTENDED_SRGB bitmap. Two pixels per cell hold the
 * geometry record (dot radius in pt, dot alpha, halo alpha, 1) and the colour record (rgb, 1). The original binds
 * it with RuntimeShader.setInputBuffer so the half-float values are sampled as raw data with no colour-space
 * conversion or alpha premultiplication; setInputShader would transform those values and corrupt the geometry.
 *
 * The exact per-cell function lives in an unrecovered helper (jd5.V). The math below is the documented shader_03
 * lookup contract, evaluated on the CPU with the recovered 1.7 s breath and tuning defaults.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class OrbCellBitmap(columns: Int) {
  private val size = columns
  private val bitmap = Bitmap.createBitmap(
    columns * 2,
    columns,
    Bitmap.Config.RGBA_F16,
    true,
    ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB),
  )
  private val bytes = ByteBuffer.allocateDirect(columns * columns * 16).order(ByteOrder.nativeOrder())
  val shader: BitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
    setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
  }

  /** Rebuilds every cell record for this frame and uploads the bitmap in place. */
  fun update(state: OrbShader.State, base: Color, core: Color, userGain: Float, agentGain: Float) {
    val tune = state.tuning
    val dotRadius = tune.dotPitch * 0.18f
    val omega = 2f * Math.PI.toFloat() / maxOf(0.2f, OrbShader.BREATH_SECONDS)
    val energyScale = state.intensity * tune.glow * tune.brightness * (userGain + agentGain) * 0.5f
    val haloGain = (state.halo + state.pulse * 0.2f).coerceIn(0f, 1f)
    val loud = maxOf(state.youLevel, state.agentLevel)
    bytes.clear()
    for (j in 0 until size) {
      val cy = (j + 0.5f) / size - 0.5f
      for (i in 0 until size) {
        val cx = (i + 0.5f) / size - 0.5f
        val phase = omega * state.time * 0.35f - hypot(cx - state.gazeX * 0.35f, cy - state.gazeY * 0.35f) * 9f
        val wave = 0.5f + 0.5f * sin(phase)
        val energy = (energyScale * (0.45f + 0.35f * state.breath + 0.20f * wave) * (0.6f + 0.4f * state.pulse))
          .coerceIn(0f, 1f)
        put(
          dotRadius * (0.70f + 0.30f * energy),
          (0.30f + 0.66f * energy).coerceAtMost(0.96f),
          if (energy > 0.05f) (haloGain * energy * 0.35f).coerceIn(0f, 1f) else 0f,
          1f,
        )
        val mix = (0.5f - cx + state.gazeX * 0.15f).coerceIn(0f, 1f)
        val shade = 0.55f + 0.45f * loud
        put(
          (base.red + (core.red - base.red) * mix) * shade,
          (base.green + (core.green - base.green) * mix) * shade,
          (base.blue + (core.blue - base.blue) * mix) * shade,
          1f,
        )
      }
    }
    bytes.rewind()
    bitmap.copyPixelsFromBuffer(bytes)
  }

  private fun put(r: Float, g: Float, b: Float, a: Float) {
    bytes.putShort(Half.toHalf(r))
    bytes.putShort(Half.toHalf(g))
    bytes.putShort(Half.toHalf(b))
    bytes.putShort(Half.toHalf(a))
  }
}
