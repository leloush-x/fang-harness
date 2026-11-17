package io.leloush.fang.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.data.OrbSize
import io.leloush.fang.data.Preferences

/** The orb size the user chose; working glyphs scale around their call site's base size. */
internal val LocalOrbSize = compositionLocalOf { OrbSize.Medium }

/** True while a side panel covers the chat; working glyphs hold still instead of animating out of sight. */
internal val LocalOrbsFrozen = compositionLocalOf { false }

/** Publishes the orb preferences to every working glyph in the app. */
@Composable
internal fun ProvideOrbSettings(preferences: Preferences, frozen: Boolean, content: @Composable () -> Unit) {
  val size by preferences.orbSize.collectAsStateWithLifecycle()
  CompositionLocalProvider(LocalOrbSize provides size, LocalOrbsFrozen provides frozen, content = content)
}

/** How much a glyph's base size grows or shrinks for the chosen orb size. */
@Composable
internal fun orbSizeScale(): Float = when (LocalOrbSize.current) {
  OrbSize.Small -> 0.82f
  OrbSize.Medium -> 1f
  OrbSize.Large -> 1.22f
}
