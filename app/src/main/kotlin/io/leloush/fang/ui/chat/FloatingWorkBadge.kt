package io.leloush.fang.ui.chat

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.leloush.fang.ui.components.FangOrb
import io.leloush.fang.ui.components.OrbMood
import io.leloush.fang.ui.theme.FangTokens

/** The opt-in floating work badge; callers place it after the stream so it draws above the chat. */
@Composable
internal fun BoxScope.FloatingWorkBadge(visible: Boolean) {
  if (!visible) return
  FangOrb(
    modifier = Modifier.align(Alignment.TopEnd).padding(FangTokens.Spacing.s12),
    size = FangTokens.IconSize.lg,
    mood = OrbMood.WORKING,
  )
}
