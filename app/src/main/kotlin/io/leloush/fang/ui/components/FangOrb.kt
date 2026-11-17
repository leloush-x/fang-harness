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
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.isActive

private const val FINALE_MS = 550L
private const val TAU = 2f * 3.14159265f

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
  if (handle == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
    SimmeringPot(modifier = modifier, size = size, tint = tint)
    return
  }
  ShaderFangOrb(handle, modifier, size, mood, userLevel, agentLevel)
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
  // One finite green flash per DONE episode. Freezing mid-flash marks it spent so unfreezing cannot replay it.
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
  Canvas(modifier.size(size)) {
    val seconds = if (frozen) 0f else frame.longValue / 1000f
    val breath = 0.5f - cos(TAU * seconds / OrbShader.BREATH_SECONDS) * 0.5f
    val extent = this.size
    val ppp = minOf(extent.width, extent.height) / (OrbShader.Tuning().diameter + 20f)
    val layout = OrbShader.Layout(
      width = extent.width,
      height = extent.height,
      centreX = extent.width * 0.5f,
      centreY = extent.height * 0.5f,
      pixelsPerPoint = ppp,
    )
    handle.setState(stateFor(mood, seconds, breath, userLevel, agentLevel, burst.floatValue), layout)
    drawRect(brush = brush)
  }
}

/** Level targets from b0e.a(..., z = true): listening 0.36 you; speaking 0.42 agent; both 0.38/0.4. */
private data class Drive(
  val palette: OrbShader.Palette,
  val you: Float = 0f,
  val agent: Float = 0f,
  val confirm: Float = 0f,
  val muted: Float = 0f,
  val live: Boolean = false,
  val pulse: Float = 0f,
  val intensity: Float = 0.5f,
  val halo: Float = 0f,
)

/** Maps the five legacy moods onto the exact seven-mode palette and level targets. */
private fun driveFor(mood: OrbMood, burst: Float): Drive = when (mood) {
  OrbMood.IDLE -> Drive(palette = OrbShader.Palette.SLEEPING, pulse = 0.25f, intensity = 0.55f, halo = 0.10f)

  OrbMood.LISTENING -> Drive(
    palette = OrbShader.Palette.LISTENING,
    you = 0.36f,
    live = true,
    pulse = 0.50f,
    intensity = 1f,
    halo = 0.25f,
  )

  OrbMood.SPEAKING -> Drive(
    palette = OrbShader.Palette.SPEAKING,
    agent = 0.42f,
    live = true,
    pulse = 0.60f,
    intensity = 1f,
    halo = 0.35f,
  )

  OrbMood.DONE -> if (burst > 0f) {
    // The 0.55 s finale flashes the speaking green before settling into the ended/sleeping palette.
    Drive(
      palette = OrbShader.Palette.SPEAKING,
      agent = 0.42f,
      live = true,
      pulse = 0.60f,
      intensity = 1f,
      halo = burst,
    )
  } else {
    Drive(palette = OrbShader.Palette.SLEEPING, pulse = 0.25f, intensity = 0.55f, halo = 0.10f)
  }

  OrbMood.FAILED -> Drive(
    palette = OrbShader.Palette.CONFIRMING,
    confirm = 1f,
    muted = 1f,
    pulse = 0.10f,
    intensity = 0.35f,
  )
}

/** Resolves a mood and its breath into the full uniform state; caller levels override the fixed targets. */
private fun stateFor(
  mood: OrbMood,
  seconds: Float,
  breath: Float,
  userLevel: Float,
  agentLevel: Float,
  burst: Float,
): OrbShader.State {
  val drive = driveFor(mood, burst)
  val tune = OrbShader.Tuning()
  val you = if (userLevel > 0f) userLevel.coerceIn(0f, 1f) else drive.you
  val agent = if (agentLevel > 0f) agentLevel.coerceIn(0f, 1f) else drive.agent
  return OrbShader.State(
    time = seconds,
    palette = drive.palette,
    youLevel = you,
    agentLevel = agent,
    confirmWeight = drive.confirm,
    mutedWeight = drive.muted,
    isLive = drive.live,
    breath = breath,
    pulse = drive.pulse * (0.7f + 0.3f * breath),
    intensity = drive.intensity,
    halo = drive.halo,
    gazeX = 0.14f * sin(TAU * seconds / 7.3f) * tune.gazeBlend,
    gazeY = 0.10f * sin(TAU * seconds / 5.7f + 1.1f) * tune.gazeBlend,
    glowGain = 1f + burst,
  )
}
