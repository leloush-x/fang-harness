package io.leloush.fang.ui.chat

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.FangOrb
import io.leloush.fang.ui.components.LocalOrbsFrozen
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.components.OrbMood
import io.leloush.fang.ui.theme.fangColors
import kotlinx.coroutines.delay

private const val FINALE_MS = 550
private const val HANDOFF_MS = 340

private enum class GlyphPhase { Working, Finale, Handoff, Done }

/**
 * A breathing working orb that, once [working] turns false, holds a 0.55 s green finale and then hands off over
 * 0.34 s to a copy button, or to a check when there is nothing to copy. Turning [working] back on resets it.
 */
@Composable
internal fun WorkingToCopyGlyph(
  working: Boolean,
  copyText: String?,
  size: Dp,
  contentDescription: String,
  @DrawableRes doneIcon: Int? = null,
  modifier: Modifier = Modifier,
) {
  var phase by remember { mutableStateOf(if (working) GlyphPhase.Working else GlyphPhase.Done) }
  val wasWorking = remember { mutableStateOf(working) }
  val frozen = LocalOrbsFrozen.current
  LaunchedEffect(working, frozen) {
    // A panel covering the chat freezes the orb, so the handoff must not animate out of sight either.
    if (frozen) {
      phase = if (working) GlyphPhase.Working else GlyphPhase.Done
      wasWorking.value = working
      return@LaunchedEffect
    }
    if (working) {
      phase = GlyphPhase.Working
    } else if (wasWorking.value) {
      phase = GlyphPhase.Finale
      delay(FINALE_MS.toLong())
      phase = GlyphPhase.Handoff
      delay(HANDOFF_MS.toLong())
      phase = GlyphPhase.Done
    }
    wasWorking.value = working
  }
  val handoff by animateFloatAsState(
    targetValue = if (phase == GlyphPhase.Handoff || phase == GlyphPhase.Done) 1f else 0f,
    animationSpec = tween(HANDOFF_MS),
    label = "morph_handoff",
  )
  Box(modifier, contentAlignment = Alignment.Center) {
    if (phase != GlyphPhase.Done) {
      val mood = if (phase == GlyphPhase.Working) OrbMood.WORKING else OrbMood.DONE
      FangOrb(modifier = Modifier.alpha(1f - handoff), size = size, mood = mood)
    }
    if (phase == GlyphPhase.Handoff || phase == GlyphPhase.Done) {
      DoneGlyph(copyText, doneIcon, contentDescription, size, Modifier.alpha(handoff))
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
