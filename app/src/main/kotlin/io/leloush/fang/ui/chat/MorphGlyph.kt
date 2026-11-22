package io.leloush.fang.ui.chat

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.LocalOrbsFrozen
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.components.activityglyph.ActivityGlyph
import io.leloush.fang.ui.components.activityglyph.AgentActivity
import io.leloush.fang.ui.theme.fangColors
import kotlinx.coroutines.delay

private const val FINALE_MS = 550
private const val HANDOFF_MS = 340
private val HandoffBlur = 6.dp

private enum class GlyphPhase { Working, Finale, Handoff, Done }

/**
 * A working activity glyph that, once [activity] turns DONE, holds a 0.55 s green finale and then hands off over
 * 0.34 s to a copy button, or to a check when there is nothing to copy. The handoff blurs and shrinks the glyph
 * toward the settled glyph's centre while that glyph sharpens in. Under Reduce Motion it is a cross-fade only.
 * Turning the run back on resets it.
 */
@Composable
internal fun WorkingToCopyGlyph(
  activity: AgentActivity,
  copyText: String?,
  size: Dp,
  contentDescription: String,
  @DrawableRes doneIcon: Int? = null,
  modifier: Modifier = Modifier,
  derive: (() -> AgentActivity)? = null,
) {
  val settled = activity == AgentActivity.DONE || activity == AgentActivity.FAILED
  var phase by remember { mutableStateOf(if (settled) GlyphPhase.Done else GlyphPhase.Working) }
  val wasWorking = remember { mutableStateOf(!settled) }
  val frozen = LocalOrbsFrozen.current
  val reduceMotion = rememberReduceMotion()
  LaunchedEffect(settled, frozen) {
    // A panel covering the chat freezes the orb, so the handoff must not animate out of sight either.
    if (frozen) {
      phase = if (settled) GlyphPhase.Done else GlyphPhase.Working
      wasWorking.value = !settled
      return@LaunchedEffect
    }
    if (!settled) {
      phase = GlyphPhase.Working
    } else if (wasWorking.value) {
      phase = GlyphPhase.Finale
      delay(FINALE_MS.toLong())
      phase = GlyphPhase.Handoff
      delay(HANDOFF_MS.toLong())
      phase = GlyphPhase.Done
    }
    wasWorking.value = !settled
  }
  val handoff by animateFloatAsState(
    targetValue = if (phase == GlyphPhase.Handoff || phase == GlyphPhase.Done) 1f else 0f,
    animationSpec = tween(HANDOFF_MS),
    label = "morph_handoff",
  )
  val blurPx = with(LocalDensity.current) { HandoffBlur.toPx() }
  val out = (handoff / 0.7f).coerceIn(0f, 1f)
  val into = ((handoff - 0.25f) / 0.75f).coerceIn(0f, 1f)
  Box(modifier, contentAlignment = Alignment.Center) {
    if (phase != GlyphPhase.Done) {
      ActivityGlyph(
        activity = if (settled) activity.finale() else activity,
        modifier = glyphLayer(out, reduceMotion, blurPx),
        size = size,
        isPaused = frozen,
        reduceMotion = reduceMotion,
        contentDescription = contentDescription,
        derive = derive,
      )
    }
    if (phase == GlyphPhase.Handoff || phase == GlyphPhase.Done) {
      DoneGlyph(copyText, doneIcon, contentDescription, size, copyLayer(into, reduceMotion, blurPx))
    }
  }
}

/** The settled glyph: a copy button when there is output, else the row's own icon or a check. */
@Composable
private fun DoneGlyph(
  copyText: String?,
  @DrawableRes doneIcon: Int?,
  contentDescription: String,
  size: Dp,
  modifier: Modifier,
) {
  if (copyText == null) {
    LucideIcon(
      icon = doneIcon ?: R.drawable.ic_lucide_check,
      contentDescription = contentDescription,
      modifier = modifier,
      size = size,
      tint = fangColors.foregroundMuted,
    )
    return
  }
  Box(modifier = modifier) {
    CopyButton(
      text = copyText,
      contentDescription = contentDescription,
      tint = fangColors.foregroundMuted,
      iconSize = size,
    )
  }
}

private fun glyphLayer(out: Float, reduceMotion: Boolean, blurPx: Float): Modifier = Modifier.graphicsLayer {
  alpha = if (reduceMotion) 1f - out else 1f - smoothstep(out)
  scaleX = if (reduceMotion) 1f else 1f - 0.5f * smoothstep(out)
  scaleY = scaleX
  renderEffect = blur(if (reduceMotion) 0f else blurPx * out)
}

private fun copyLayer(into: Float, reduceMotion: Boolean, blurPx: Float): Modifier {
  val eased = easeOutCubic(into)
  return Modifier.graphicsLayer {
    alpha = if (reduceMotion) into else eased
    scaleX = if (reduceMotion) 1f else 0.6f + 0.4f * eased
    scaleY = scaleX
    renderEffect = blur(if (reduceMotion) 0f else blurPx * (1f - into))
  }
}

private fun AgentActivity.finale(): AgentActivity = if (this == AgentActivity.FAILED) FAILED else DONE

private fun blur(radius: Float) = if (radius > 0f) BlurEffect(null, radius, radius, TileMode.Clamp) else null

private fun smoothstep(x: Float) = x * x * (3f - 2f * x)

private fun easeOutCubic(x: Float): Float {
  val inverse = 1f - x
  return 1f - inverse * inverse * inverse
}

/** Android's Reduce Motion (animator duration scale 0), read once per context. */
@Composable
private fun rememberReduceMotion(): Boolean {
  val context = LocalContext.current
  return remember(context) {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
  }
}
