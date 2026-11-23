package io.leloush.fang.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import io.leloush.fang.R

/** The palette family picked in Settings; [Fang] keeps the app's original three palettes. */
enum class CuratedTheme(val rawValue: String) {
  Fang("fang"),
  TokyoNight("tokyonight"),
  Catppuccin("catppuccin"),
  Dracula("dracula"),
  RosePine("rosepine"),
  GitHub("github"),
  ;

  @StringRes
  val titleRes: Int
    get() = when (this) {
      Fang -> R.string.oc_theme_fang
      TokyoNight -> R.string.oc_theme_tokyo_night
      Catppuccin -> R.string.oc_theme_catppuccin
      Dracula -> R.string.oc_theme_dracula
      RosePine -> R.string.oc_theme_rose_pine
      GitHub -> R.string.oc_theme_github
    }

  companion object {
    /** Stored by raw value; unknown or missing values fall back to [Fang]. */
    fun fromRawValue(value: String?): CuratedTheme = entries.firstOrNull { it.rawValue == value } ?: Fang
  }
}

/**
 * The selection the root [FangTheme] holds and Settings drives. Writing either field re-themes the
 * app in place, because [FangTheme] reads them during composition.
 */
class ThemeState(mode: ThemeMode = ThemeMode.System, selectedTheme: CuratedTheme = CuratedTheme.Fang) {
  var mode by mutableStateOf(mode)
    private set

  var selectedTheme by mutableStateOf(selectedTheme)
    private set

  fun setMode(value: ThemeMode) {
    mode = value
  }

  fun selectTheme(value: CuratedTheme) {
    selectedTheme = value
  }
}

/** A state holder callers keep across recompositions and pass to [FangTheme]. */
@Composable
fun rememberThemeState(
  mode: ThemeMode = ThemeMode.System,
  selectedTheme: CuratedTheme = CuratedTheme.Fang,
): ThemeState = remember { ThemeState(mode, selectedTheme) }

/** Reads the selection from the nearest [FangTheme]. The singleton is only a fallback outside one. */
private val DefaultThemeState = ThemeState()

val LocalThemeState = staticCompositionLocalOf { DefaultThemeState }

/** Shorthand for the active theme state inside composables. */
val themeState: ThemeState
  @Composable
  @ReadOnlyComposable
  get() = LocalThemeState.current

/** Resolves the palette for a mode and family. AMOLED only special-cases [CuratedTheme.Fang]. */
@Composable
@ReadOnlyComposable
fun resolvedFangColors(mode: ThemeMode, selectedTheme: CuratedTheme): FangColors {
  val dark = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark, ThemeMode.AMOLED -> true
  }
  return when (selectedTheme) {
    CuratedTheme.Fang -> when (mode) {
      ThemeMode.System -> if (dark) DarkFangColors else LightFangColors
      ThemeMode.Light -> LightFangColors
      ThemeMode.Dark -> DarkFangColors
      ThemeMode.AMOLED -> AmoledFangColors
    }

    CuratedTheme.TokyoNight -> if (dark) TokyoNightDarkFangColors else TokyoNightLightFangColors

    CuratedTheme.Catppuccin -> if (dark) CatppuccinDarkFangColors else CatppuccinLightFangColors

    CuratedTheme.Dracula -> if (dark) DraculaDarkFangColors else DraculaLightFangColors

    CuratedTheme.RosePine -> if (dark) RosePineDarkFangColors else RosePineLightFangColors

    CuratedTheme.GitHub -> if (dark) GitHubDarkFangColors else GitHubLightFangColors
  }
}
