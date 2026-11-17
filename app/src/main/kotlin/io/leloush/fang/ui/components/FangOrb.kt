package io.leloush.fang.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.Dp
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.isActive

private val USER_BASE = Color(0.60f, 0.90f, 0.98f)
private val AGENT_BASE = Color(0.44f, 0.91f, 0.68f)

private const val ORB_RADIUS_PT = 30f
private const val ORB_BLEED_PT = 10f
private const val GRID_COLUMNS = 9f
private const val GRID_PITCH = 2f * ORB_RADIUS_PT / GRID_COLUMNS
private val TAU = 2f * PI.toFloat()

/** The AGSL orb on API 33+, falling back to the simmering pot so no device gets a blank indicator. */
@Composable
internal fun FangOrb(
  modifier: Modifier = Modifier,
  size: Dp = FangTokens.IconSize.md,
  tint: Color = fangColors.foregroundMuted,
  mood: OrbMood = OrbMood.WORKING,
  userLevel: Float = 0f,
  agentLevel: Float = 0f,
) {
  val handle = OrbShader.rememberOrbShaders()
  val scaled = size * orbSizeScale()
  if (handle == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
    SimmeringPot(modifier = modifier, size = scaled, tint = tint)
    return
  }
  ShaderFangOrb(handle, modifier, scaled, mood, userLevel, agentLevel)
}

/** The shader path; every call here needs API 33, which the caller guards with a positive SDK check. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderFangOrb(
  handle: OrbShader.Handle,
  modifier: Modifier,
  size: Dp,
  mood: OrbMood,
  userLevel: Float,
  agentLevel: Float,
) {
  val frame = remember { mutableLongStateOf(0L) }
  val frozen = LocalOrbsFrozen.current
  LaunchedEffect(frozen) {
    if (frozen) return@LaunchedEffect
    while (isActive) {
      withFrameMillis { millis -> frame.longValue = millis }
    }
  }
  val brush = remember(handle) { ShaderBrush(handle.orb) }
  val colors = fangColors
  // Blend a little of the active theme into the kit's cyan/green so the orb recolours with light/dark/AMOLED.
  val userTint = themeTint(USER_BASE, colors.accent, 0.18f)
  val agentTint = themeTint(AGENT_BASE, colors.dotSuccess, 0.24f)
  Canvas(modifier.size(size)) {
    val layout = orbLayout(widthPx = this.size.width, heightPx = this.size.height)
    val time = if (frozen) 0L else frame.longValue
    handle.setState(stateFor(mood, time, userLevel, agentLevel, userTint, agentTint), layout)
    drawRect(brush = brush)
  }
}

/** Maps pixel bounds into the shader's point space, leaving room for the 10pt wisp bleed. */
private fun orbLayout(widthPx: Float, heightPx: Float): OrbShader.Layout {
  val extent = ORB_RADIUS_PT + ORB_BLEED_PT
  return OrbShader.Layout(
    width = widthPx,
    height = heightPx,
    centreX = widthPx * 0.5f,
    centreY = heightPx * 0.5f,
    radius = ORB_RADIUS_PT,
    pitch = GRID_PITCH,
    columns = GRID_COLUMNS,
    pixelsPerPoint = (minOf(widthPx, heightPx) * 0.5f) / extent,
  )
}

/** Mood drives the mute plate, energy and halo; WAITING is muted, SPEAKING is loud. */
private fun driveFor(mood: OrbMood): MoodDrive = when (mood) {
  OrbMood.CONNECTING -> MoodDrive(mixB = 0f, pulse = 0.25f, intensity = 0.55f, glow = 0.35f, halo = 0.10f)
  OrbMood.LISTENING -> MoodDrive(mixB = 0f, pulse = 0.50f, intensity = 0.80f, glow = 0.60f, halo = 0.25f)
  OrbMood.WORKING -> MoodDrive(mixB = 0f, pulse = 0.60f, intensity = 1f, glow = 0.70f, halo = 0.35f)
  OrbMood.SPEAKING -> MoodDrive(mixB = 0f, pulse = 1f, intensity = 1f, glow = 0.90f, halo = 0.60f)
  OrbMood.WAITING -> MoodDrive(mixB = 1f, pulse = 0.10f, intensity = 0.35f, glow = 0.25f, halo = 0f)
  OrbMood.DONE -> MoodDrive(mixB = 0f, pulse = 0.30f, intensity = 0.85f, glow = 0.60f, halo = 0.20f)
}

private data class MoodDrive(val mixB: Float, val pulse: Float, val intensity: Float, val glow: Float, val halo: Float)

/** A gentle blending of the kit hue with the theme accent; the kit tint stays the fallback. */
private fun themeTint(base: Color, theme: Color, amount: Float): Color = Color(
  red = base.red + (theme.red - base.red) * amount,
  green = base.green + (theme.green - base.green) * amount,
  blue = base.blue + (theme.blue - base.blue) * amount,
  alpha = base.alpha,
)

/** The theme-aware tints and a slow gaze drift drive the shader; the kit tint is for the pot fallback. */
private fun stateFor(
  mood: OrbMood,
  timeMs: Long,
  userLevel: Float,
  agentLevel: Float,
  userTint: Color,
  agentTint: Color,
): OrbShader.State {
  val seconds = timeMs / 1000f
  val breath = 0.5f + 0.5f * sin(TAU * seconds / OrbShader.BREATH_SECONDS)
  val drive = driveFor(mood)
  // A slow, small gaze keeps the orb alive without bouncing while text streams.
  val gazeX = 0.14f * sin(TAU * seconds / 7.3f) * drive.intensity
  val gazeY = 0.10f * sin(TAU * seconds / 5.7f + 1.1f) * drive.intensity
  return OrbShader.State(
    time = seconds,
    userTint = if (mood == OrbMood.DONE) agentTint else userTint,
    agentTint = agentTint,
    userLevel = userLevel,
    agentLevel = agentLevel,
    mixB = drive.mixB,
    glow = drive.glow,
    pulse = drive.pulse * (0.7f + 0.3f * breath),
    intensity = drive.intensity,
    halo = drive.halo,
    gazeX = gazeX,
    gazeY = gazeY,
  )
}
