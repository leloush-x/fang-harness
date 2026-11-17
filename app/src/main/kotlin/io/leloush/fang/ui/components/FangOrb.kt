package io.leloush.fang.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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

private const val ORB_RADIUS_PT = 30f
private const val ORB_BLEED_PT = 10f
private const val GRID_COLUMNS = 9f
private const val GRID_PITCH = 2f * ORB_RADIUS_PT / GRID_COLUMNS
private const val FINALE_MS = 550
private const val FINALE_FLASH_GLOW = 1.25f
private const val DONE_SETTLED_GLOW = 0.60f
private val TAU = 2f * PI.toFloat()

/** The AGSL orb on API 33+, falling back to the simmering pot so no device gets a blank indicator. */
@Composable
internal fun FangOrb(
  modifier: Modifier = Modifier,
  size: Dp = FangTokens.IconSize.md,
  tint: Color = fangColors.foregroundMuted,
  mood: OrbMood = OrbMood.SPEAKING,
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
  // The finale is one finite flash per DONE episode; the clock above keeps breathing in every mood. Freezing
  // mid-flash skips the animation but still marks it spent, so unfreezing cannot re-trigger a finished orb.
  val burst = remember { mutableFloatStateOf(0f) }
  val finaleSpent = remember { mutableStateOf(false) }
  LaunchedEffect(mood) {
    if (mood != OrbMood.DONE) {
      finaleSpent.value = false
      burst.floatValue = 0f
      return@LaunchedEffect
    }
    if (finaleSpent.value) return@LaunchedEffect
    finaleSpent.value = true
    if (frozen) {
      burst.floatValue = 0f
      return@LaunchedEffect
    }
    burst.floatValue = 1f
    val start = withFrameMillis { it }
    while (isActive && burst.floatValue > 0f) {
      withFrameMillis { now ->
        burst.floatValue = 1f - ((now - start).toFloat() / FINALE_MS).coerceIn(0f, 1f)
      }
    }
  }
  val brush = remember(handle) { ShaderBrush(handle.orb) }
  val colors = fangColors
  val palette = OrbPalette(
    user = themeTint(OrbShader.USER_BASE, colors.accent, 0.18f),
    agent = themeTint(OrbShader.AGENT_BASE, colors.dotSuccess, 0.24f),
  )
  Canvas(modifier.size(size)) {
    val layout = orbLayout(widthPx = this.size.width, heightPx = this.size.height)
    val time = if (frozen) 0L else frame.longValue
    handle.setState(stateFor(mood, time, userLevel, agentLevel, burst.floatValue, palette), layout)
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

/** The kit palette, with the app's theme blended into the two live tints by [themeTint]. */
private data class OrbPalette(val user: Color, val agent: Color)

/** Picks the side, energy and tint each mode drives; levels come from the caller. */
private fun driveFor(mood: OrbMood, userLevel: Float, agentLevel: Float, palette: OrbPalette): MoodDrive = when (mood) {
  OrbMood.IDLE -> MoodDrive(
    glow = 0.05f,
    tint = OrbShader.MUTED_PLATE,
    energy = Energy(pulse = 0.25f, intensity = 0.55f, halo = 0.10f),
  )

  OrbMood.LISTENING -> MoodDrive(
    rimX = userLevel,
    glow = userLevel,
    tint = palette.user,
    energy = Energy(pulse = 0.50f, intensity = 0.80f, halo = 0.25f),
  )

  OrbMood.SPEAKING -> MoodDrive(
    rimY = agentLevel,
    glow = agentLevel,
    tint = palette.agent,
    energy = Energy(pulse = 0.60f, intensity = 1f, halo = 0.35f),
  )

  OrbMood.DONE -> MoodDrive(
    rimY = 1f,
    glow = DONE_SETTLED_GLOW,
    tint = palette.agent,
    energy = Energy(pulse = 0.30f, intensity = 0.85f, halo = 0.20f),
  )

  OrbMood.FAILED -> MoodDrive(
    mixBX = 1f,
    mixBY = 1f,
    glow = 0f,
    tint = OrbShader.MUTED_RIM,
    energy = Energy(pulse = 0.10f, intensity = 0.35f, halo = 0f),
  )
}

/** [tint] drives both tint uniforms; [rimX]/[rimY] are the rim sides and [glow] the settled core. */
private data class MoodDrive(
  val mixBX: Float = 0f,
  val mixBY: Float = 0f,
  val rimX: Float = 0f,
  val rimY: Float = 0f,
  val glow: Float = 0.55f,
  val tint: Color,
  val energy: Energy,
)

/** The child cells shader's liveliness, kept per mode so a mode change reads in one place. */
private data class Energy(val pulse: Float, val intensity: Float, val halo: Float)

/** A gentle blending of the kit hue with the theme accent; the kit tint stays the fallback. */
private fun themeTint(base: Color, theme: Color, amount: Float): Color = Color(
  red = base.red + (theme.red - base.red) * amount,
  green = base.green + (theme.green - base.green) * amount,
  blue = base.blue + (theme.blue - base.blue) * amount,
  alpha = base.alpha,
)

/** Resolves a mode into shader uniforms; the Done flash lifts both the core glow and the dot halo. */
private fun stateFor(
  mood: OrbMood,
  timeMs: Long,
  userLevel: Float,
  agentLevel: Float,
  burst: Float,
  palette: OrbPalette,
): OrbShader.State {
  val seconds = timeMs / 1000f
  val breath = 0.5f + 0.5f * sin(TAU * seconds / OrbShader.BREATH_SECONDS)
  val drive = driveFor(mood, userLevel, agentLevel, palette)
  // A slow, small gaze keeps the orb alive without bouncing while text streams.
  val gazeX = 0.14f * sin(TAU * seconds / 7.3f) * drive.energy.intensity
  val gazeY = 0.10f * sin(TAU * seconds / 5.7f + 1.1f) * drive.energy.intensity
  val flash = if (mood == OrbMood.DONE) burst else 0f
  return OrbShader.State(
    time = seconds,
    userTint = if (mood == OrbMood.DONE) palette.agent else palette.user,
    agentTint = palette.agent,
    mutedPlate = OrbShader.MUTED_PLATE,
    mutedRim = OrbShader.MUTED_RIM,
    tintBase = drive.tint,
    tintCore = drive.tint,
    userLevel = userLevel,
    agentLevel = agentLevel,
    mixBX = drive.mixBX,
    mixBY = drive.mixBY,
    rimX = drive.rimX,
    rimY = drive.rimY,
    glowX = drive.glow + (FINALE_FLASH_GLOW - drive.glow) * flash,
    burst = flash,
    pulse = drive.energy.pulse * (0.7f + 0.3f * breath),
    intensity = drive.energy.intensity,
    halo = drive.energy.halo,
    gazeX = gazeX,
    gazeY = gazeY,
  )
}
