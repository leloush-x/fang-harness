package io.leloush.fang.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

val LocalFangColors = staticCompositionLocalOf { DarkFangColors }

/** Shorthand for the active palette inside composables. */
val fangColors: FangColors
  @Composable
  @ReadOnlyComposable
  get() = LocalFangColors.current

/** Keeps the existing callers: the mode comes from Preferences and the palette family starts at Fang. */
@Composable
fun FangTheme(mode: ThemeMode, content: @Composable () -> Unit) {
  val selected = rememberThemeState()
  selected.setMode(mode)
  FangTheme(mode, selected, content)
}

/** Convenience for callers with only a palette family; the mode follows the system. */
@Composable
fun FangTheme(selectedTheme: CuratedTheme, content: @Composable () -> Unit) {
  FangTheme(rememberThemeState(selectedTheme = selectedTheme), content)
}

/** Callers that own a [ThemeState] (for persistence) can drive both the mode and the palette family. */
@Composable
fun FangTheme(themeState: ThemeState, content: @Composable () -> Unit) {
  FangTheme(themeState.mode, themeState, content)
}

@Composable
fun FangTheme(mode: ThemeMode, themeState: ThemeState, content: @Composable () -> Unit) {
  val colors = resolvedFangColors(mode, themeState.selectedTheme)
  val dark = colors.isDark
  val base = if (dark) darkColorScheme() else lightColorScheme()
  val scheme =
    base.copy(
      primary = colors.accent,
      onPrimary = colors.accentForeground,
      background = colors.surface0,
      onBackground = colors.foreground,
      surface = colors.surface0,
      onSurface = colors.foreground,
      surfaceVariant = colors.surface2,
      onSurfaceVariant = colors.foregroundMuted,
      surfaceContainerLowest = colors.surface0,
      surfaceContainerLow = colors.surface1,
      surfaceContainer = colors.surface1,
      surfaceContainerHigh = colors.surface2,
      surfaceContainerHighest = colors.surface3,
      outline = colors.border,
      outlineVariant = colors.borderAccent,
      error = colors.destructive,
      onError = colors.accentForeground,
    )
  val typography = MaterialTheme.typography
  // No colour here: MaterialTheme's LocalTextStyle is bodyLarge and buttons merge into it, so a colour would
  // override their content colour. Text colour comes from LocalContentColor instead.
  val bodyStyle = TextStyle(fontSize = FangTokens.FontSize.base)
  CompositionLocalProvider(LocalFangColors provides colors, LocalThemeState provides themeState) {
    MaterialTheme(
      colorScheme = scheme,
      typography =
        typography.copy(
          bodyLarge = typography.bodyLarge.merge(bodyStyle),
          bodyMedium = typography.bodyMedium.merge(bodyStyle),
        ),
    ) {
      CompositionLocalProvider(LocalContentColor provides colors.foreground, content = content)
    }
  }
}
