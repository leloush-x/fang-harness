package io.leloush.fang.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import io.leloush.fang.data.Preferences

/** True while a side panel covers the chat; working glyphs hold still instead of animating out of sight. */
internal val LocalOrbsFrozen = compositionLocalOf { false }

/**
 * Publishes the freeze state to every working glyph. The orb-size setting is gone, so the single orb is no longer
 * scaled twice; [preferences] stays in the signature for the existing call site in MainScreen.
 */
@Composable
internal fun ProvideOrbSettings(preferences: Preferences, frozen: Boolean, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalOrbsFrozen provides frozen, content = content)
}
