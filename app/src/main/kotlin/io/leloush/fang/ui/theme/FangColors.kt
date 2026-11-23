package io.leloush.fang.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paseo's palette with the accent swapped for the launcher icon's terracotta (https://github.com/getpaseo/paseo,
 * commit b26b923; Copyright (c) 2025-present Mohamed Boudra, Apache License 2.0, see `assets/licenses/paseo.txt`).
 */
@Immutable
data class FangColors(
  val isDark: Boolean,
  val surface0: Color,
  val surface1: Color,
  val surface2: Color,
  val surface3: Color,
  val surface4: Color,
  val sidebar: Color,
  val foreground: Color,
  val foregroundMuted: Color,
  val foregroundExtraMuted: Color,
  val border: Color,
  val borderAccent: Color,
  val accent: Color,
  val accentForeground: Color,
  val destructive: Color,
  val dotSuccess: Color,
  val dotDanger: Color,
  val dotWarning: Color,
)

val DarkFangColors =
  FangColors(
    isDark = true,
    surface0 = Color(0xFF181B1A),
    surface1 = Color(0xFF1E2120),
    surface2 = Color(0xFF272A29),
    surface3 = Color(0xFF434645),
    surface4 = Color(0xFF595B5B),
    sidebar = Color(0xFF141716),
    foreground = Color(0xFFFAFAFA),
    foregroundMuted = Color(0xFFA1A5A4),
    foregroundExtraMuted = Color(0xFF717574),
    border = Color(0xFF252B2A),
    borderAccent = Color(0xFF2F3534),
    accent = Color(0xFFDD774E),
    accentForeground = Color(0xFFFFFFFF),
    destructive = Color(0xFFC64F43),
    dotSuccess = Color(0xFF35C264),
    dotDanger = Color(0xFFF7796D),
    dotWarning = Color(0xFFDB932E),
  )

/**
 * The AMOLED palette: the dark one on pure black, for OLED screens. Its surfaces sit barely above the black, so
 * the borders are lifted, because the dark theme's edges vanish against it.
 */
val AmoledFangColors =
  FangColors(
    isDark = true,
    surface0 = Color(0xFF000000),
    surface1 = Color(0xFF0A0A0A),
    surface2 = Color(0xFF171717),
    surface3 = Color(0xFF232323),
    surface4 = Color(0xFF2F2F2F),
    sidebar = Color(0xFF000000),
    foreground = Color(0xFFFAFAFA),
    foregroundMuted = Color(0xFFA1A5A4),
    foregroundExtraMuted = Color(0xFF717574),
    border = Color(0xFF242424),
    borderAccent = Color(0xFF333333),
    accent = Color(0xFFDD774E),
    accentForeground = Color(0xFFFFFFFF),
    destructive = Color(0xFFC64F43),
    dotSuccess = Color(0xFF35C264),
    dotDanger = Color(0xFFF7796D),
    dotWarning = Color(0xFFDB932E),
  )

val LightFangColors =
  FangColors(
    isDark = false,
    surface0 = Color(0xFFFFFFFF),
    surface1 = Color(0xFFFAFAFA),
    surface2 = Color(0xFFF4F4F5),
    surface3 = Color(0xFFE4E4E7),
    surface4 = Color(0xFFD4D4D8),
    sidebar = Color(0xFFF4F4F5),
    foreground = Color(0xFF1A1A1E),
    foregroundMuted = Color(0xFF71717A),
    foregroundExtraMuted = Color(0xFFA1A1AA),
    border = Color(0xFFE4E4E7),
    borderAccent = Color(0xFFECECF1),
    accent = Color(0xFFBD5132),
    accentForeground = Color(0xFFFFFFFF),
    destructive = Color(0xFFB04138),
    dotSuccess = Color(0xFF299F51),
    dotDanger = Color(0xFFF12E2F),
    dotWarning = Color(0xFFB37824),
  )

/**
 * Curated palettes adapted from OpenCode for Android
 * (github.com/anomalyco/opencode-android, commit fc35d8f,
 * `core/src/main/kotlin/ai/opencode/core/ui/ThemePaletteGenerated.kt`). The floats are the exact
 * `ThemeRGBA` components, including the glass fills' alpha.
 *
 * Upstream's 28 slots map into Fang's 18 as follows: canvas→surface0, canvasWeak→surface1 and
 * sidebar, userBubble→surface2 and surface3, surfaceStrong→surface4, text→foreground,
 * textWeak→foregroundMuted, textTertiary→foregroundExtraMuted, border→border,
 * borderStrong→borderAccent, accent→accent, link→accent (both use the accent slot here),
 * diffRemovedText→destructive, glassDiffAddedFill→dotSuccess, glassDiffRemovedFill→dotDanger,
 * syntaxType→dotWarning. accentForeground is upstream's highContrastForeground(accent).
 */
val TokyoNightLightFangColors =
  FangColors(
    isDark = false,
    surface0 = Color(0.898f, 0.902f, 0.9176f),
    surface1 = Color(0.8627f, 0.8706f, 0.8902f),
    surface2 = Color(0.8199f, 0.8278f, 0.8513f),
    surface3 = Color(0.8199f, 0.8278f, 0.8513f),
    surface4 = Color(0.7719f, 0.7797f, 0.8029f),
    sidebar = Color(0.8627f, 0.8706f, 0.8902f),
    foreground = Color(0.1216f, 0.1569f, 0.2902f),
    foregroundMuted = Color(0.2314f, 0.2706f, 0.4f),
    foregroundExtraMuted = Color(0.453f, 0.4845f, 0.5807f),
    border = Color(0.7176f, 0.7294f, 0.7725f),
    borderAccent = Color(0.6275f, 0.6431f, 0.702f),
    accent = Color(0.1804f, 0.4902f, 0.9137f),
    accentForeground = Color(0.0667f, 0.0667f, 0.0784f),
    destructive = Color(0.6614f, 0.086f, 0.3049f),
    dotSuccess = Color(0f, 0.4837f, 0.3819f, 0.16f),
    dotDanger = Color(0.6539f, 0.1926f, 0.3351f, 0.14f),
    dotWarning = Color(0.4667f, 0.3176f, 0.0392f),
  )

val TokyoNightDarkFangColors =
  FangColors(
    isDark = true,
    surface0 = Color(0.051f, 0.0549f, 0.0902f),
    surface1 = Color(0.0745f, 0.0784f, 0.1176f),
    surface2 = Color(0.1216f, 0.1294f, 0.1765f),
    surface3 = Color(0.1216f, 0.1294f, 0.1765f),
    surface4 = Color(0.149f, 0.1569f, 0.2118f),
    sidebar = Color(0.0745f, 0.0784f, 0.1176f),
    foreground = Color(0.7686f, 0.8078f, 0.9843f),
    foregroundMuted = Color(0.6392f, 0.6706f, 0.8235f),
    foregroundExtraMuted = Color(0.4054f, 0.4259f, 0.5331f),
    border = Color(0.2353f, 0.251f, 0.3216f),
    borderAccent = Color(0.2941f, 0.3098f, 0.3882f),
    accent = Color(0.4784f, 0.6353f, 0.9686f),
    accentForeground = Color(0.0667f, 0.0667f, 0.0784f),
    destructive = Color(1f, 0.493f, 0.4792f),
    dotSuccess = Color(0f, 0.7539f, 0.8371f, 0.16f),
    dotDanger = Color(0.9108f, 0.4237f, 0.4135f, 0.14f),
    dotWarning = Color(0.9922f, 0.8471f, 0.6471f),
  )

val CatppuccinLightFangColors =
  FangColors(
    isDark = false,
    surface0 = Color(0.9451f, 0.9529f, 0.9647f),
    surface1 = Color(0.9137f, 0.9255f, 0.9412f),
    surface2 = Color(0.8692f, 0.877f, 0.8965f),
    surface3 = Color(0.8692f, 0.877f, 0.8965f),
    surface4 = Color(0.8206f, 0.8284f, 0.8477f),
    sidebar = Color(0.9137f, 0.9255f, 0.9412f),
    foreground = Color(0.2353f, 0.2431f, 0.3451f),
    foregroundMuted = Color(0.3529f, 0.3608f, 0.4588f),
    foregroundExtraMuted = Color(0.5535f, 0.5626f, 0.6344f),
    border = Color(0.7882f, 0.7961f, 0.8314f),
    borderAccent = Color(0.7098f, 0.7176f, 0.7647f),
    accent = Color(0.4163f, 0.4947f, 0.9539f),
    accentForeground = Color(0.0667f, 0.0667f, 0.0784f),
    destructive = Color(0.6742f, 0.0889f, 0.1878f),
    dotSuccess = Color(0.1933f, 0.4863f, 0.1313f, 0.16f),
    dotDanger = Color(0.6651f, 0.1963f, 0.2377f, 0.14f),
    dotWarning = Color(0.4941f, 0.302f, 0.0275f),
  )

val CatppuccinDarkFangColors =
  FangColors(
    isDark = true,
    surface0 = Color(0.0588f, 0.0588f, 0.1137f),
    surface1 = Color(0.0824f, 0.0824f, 0.1412f),
    surface2 = Color(0.1333f, 0.1373f, 0.1961f),
    surface3 = Color(0.1333f, 0.1373f, 0.1961f),
    surface4 = Color(0.1647f, 0.1686f, 0.2314f),
    sidebar = Color(0.0824f, 0.0824f, 0.1412f),
    foreground = Color(0.8039f, 0.8392f, 0.9608f),
    foregroundMuted = Color(0.6706f, 0.702f, 0.8078f),
    foregroundExtraMuted = Color(0.4274f, 0.4468f, 0.5355f),
    border = Color(0.2549f, 0.2667f, 0.3373f),
    borderAccent = Color(0.3137f, 0.3255f, 0.4039f),
    accent = Color(0.7059f, 0.7451f, 0.9961f),
    accentForeground = Color(0.0667f, 0.0667f, 0.0784f),
    destructive = Color(1f, 0.4665f, 0.6311f),
    dotSuccess = Color(0f, 0.7736f, 0.6993f, 0.16f),
    dotDanger = Color(0.8924f, 0.4161f, 0.5625f, 0.14f),
    dotWarning = Color(0.9922f, 0.9333f, 0.8039f),
  )

val DraculaLightFangColors =
  FangColors(
    isDark = false,
    surface0 = Color(0.9765f, 0.9765f, 0.9569f),
    surface1 = Color(0.9333f, 0.9373f, 0.9137f),
    surface2 = Color(0.8941f, 0.8941f, 0.8824f),
    surface3 = Color(0.8941f, 0.8941f, 0.8824f),
    surface4 = Color(0.8549f, 0.8549f, 0.8471f),
    sidebar = Color(0.9333f, 0.9373f, 0.9137f),
    foreground = Color(0.1059f, 0.1059f, 0.1686f),
    foregroundMuted = Color(0.2118f, 0.2118f, 0.2745f),
    foregroundExtraMuted = Color(0.4632f, 0.4644f, 0.5042f),
    border = Color(0.7686f, 0.7686f, 0.7686f),
    borderAccent = Color(0.6667f, 0.6667f, 0.6745f),
    accent = Color(0.4863f, 0.4196f, 0.9608f),
    accentForeground = Color(0.0667f, 0.0667f, 0.0784f),
    destructive = Color(0.5922f, 0.212f, 0.3398f),
    dotSuccess = Color(0f, 0.4938f, 0.2625f, 0.16f),
    dotDanger = Color(0.6493f, 0.1936f, 0.3571f, 0.14f),
    dotWarning = Color(0.5098f, 0.2902f, 0.0196f),
  )

val DraculaDarkFangColors =
  FangColors(
    isDark = true,
    surface0 = Color(0.0588f, 0.0627f, 0.098f),
    surface1 = Color(0.0824f, 0.0902f, 0.1255f),
    surface2 = Color(0.149f, 0.1529f, 0.1804f),
    surface3 = Color(0.149f, 0.1529f, 0.1804f),
    surface4 = Color(0.1843f, 0.1882f, 0.2157f),
    sidebar = Color(0.0824f, 0.0902f, 0.1255f),
    foreground = Color(0.9725f, 0.9725f, 0.949f),
    foregroundMuted = Color(0.8314f, 0.8314f, 0.8078f),
    foregroundExtraMuted = Color(0.5221f, 0.5249f, 0.5287f),
    border = Color(0.302f, 0.302f, 0.3216f),
    borderAccent = Color(0.3725f, 0.3765f, 0.3922f),
    accent = Color(0.7412f, 0.5765f, 0.9765f),
    accentForeground = Color(0.0667f, 0.0667f, 0.0784f),
    destructive = Color(1f, 0.4822f, 0.5491f),
    dotSuccess = Color(0.2349f, 0.7782f, 0.5526f, 0.16f),
    dotDanger = Color(0.9063f, 0.4173f, 0.4832f, 0.14f),
    dotWarning = Color(0.9961f, 0.8431f, 0.6941f),
  )

val RosePineLightFangColors =
  FangColors(
    isDark = false,
    surface0 = Color(0.9843f, 0.9608f, 0.9373f),
    surface1 = Color(0.9608f, 0.9294f, 0.9059f),
    surface2 = Color(0.9074f, 0.884f, 0.8762f),
    surface3 = Color(0.9074f, 0.884f, 0.8762f),
    surface4 = Color(0.8585f, 0.8353f, 0.8275f),
    sidebar = Color(0.9608f, 0.9294f, 0.9059f),
    foreground = Color(0.2706f, 0.251f, 0.4f),
    foregroundMuted = Color(0.4722f, 0.4533f, 0.5209f),
    foregroundExtraMuted = Color(0.6483f, 0.6282f, 0.6664f),
    border = Color(0.8275f, 0.8039f, 0.8235f),
    borderAccent = Color(0.749f, 0.7294f, 0.7686f),
    accent = Color(0.1922f, 0.4549f, 0.5608f),
    accentForeground = Color(1f, 1f, 1f),
    destructive = Color(0.5731f, 0.2339f, 0.3442f),
    dotSuccess = Color(0f, 0.4519f, 0.5933f, 0.16f),
    dotDanger = Color(0.6488f, 0.1937f, 0.3595f, 0.14f),
    dotWarning = Color(0.225f, 0.4697f, 0.5118f),
  )

val RosePineDarkFangColors =
  FangColors(
    isDark = true,
    surface0 = Color(0.0471f, 0.0392f, 0.0863f),
    surface1 = Color(0.0706f, 0.0627f, 0.1137f),
    surface2 = Color(0.1294f, 0.1216f, 0.1725f),
    surface3 = Color(0.1294f, 0.1216f, 0.1725f),
    surface4 = Color(0.1647f, 0.1569f, 0.2078f),
    sidebar = Color(0.0706f, 0.0627f, 0.1137f),
    foreground = Color(0.8784f, 0.8706f, 0.9608f),
    foregroundMuted = Color(0.4583f, 0.4426f, 0.5536f),
    foregroundExtraMuted = Color(0.2965f, 0.284f, 0.3715f),
    border = Color(0.2667f, 0.2588f, 0.3176f),
    borderAccent = Color(0.3333f, 0.3255f, 0.3843f),
    accent = Color(0.6118f, 0.8118f, 0.8471f),
    accentForeground = Color(0.0667f, 0.0667f, 0.0784f),
    destructive = Color(1f, 0.4688f, 0.6202f),
    dotSuccess = Color(0f, 0.7346f, 0.9437f, 0.16f),
    dotDanger = Color(0.8947f, 0.4159f, 0.5524f, 0.14f),
    dotWarning = Color(0.6118f, 0.8118f, 0.8471f),
  )

val GitHubLightFangColors =
  FangColors(
    isDark = false,
    surface0 = Color(1f, 1f, 1f),
    surface1 = Color(0.9608f, 0.9608f, 0.9569f),
    surface2 = Color(0.9176f, 0.9176f, 0.9216f),
    surface3 = Color(0.9176f, 0.9176f, 0.9216f),
    surface4 = Color(0.8784f, 0.8824f, 0.8863f),
    sidebar = Color(0.9608f, 0.9608f, 0.9569f),
    foreground = Color(0.1176f, 0.1373f, 0.1608f),
    foregroundMuted = Color(0.3412f, 0.3765f, 0.4157f),
    foregroundExtraMuted = Color(0.5639f, 0.5883f, 0.6156f),
    border = Color(0.7922f, 0.7961f, 0.8f),
    borderAccent = Color(0.6863f, 0.6941f, 0.702f),
    accent = Color(0.0353f, 0.4118f, 0.8549f),
    accentForeground = Color(1f, 1f, 1f),
    destructive = Color(0.6751f, 0.0982f, 0.1378f),
    dotSuccess = Color(0.0614f, 0.4951f, 0.2003f, 0.16f),
    dotDanger = Color(0.6661f, 0.2007f, 0.2003f, 0.14f),
    dotWarning = Color(0.7373f, 0.298f, 0f),
  )

val GitHubDarkFangColors =
  FangColors(
    isDark = true,
    surface0 = Color(0.0196f, 0.0314f, 0.0549f),
    surface1 = Color(0.0392f, 0.0549f, 0.0784f),
    surface2 = Color(0.0941f, 0.1098f, 0.1333f),
    surface3 = Color(0.0941f, 0.1098f, 0.1333f),
    surface4 = Color(0.1255f, 0.1412f, 0.1647f),
    sidebar = Color(0.0392f, 0.0549f, 0.0784f),
    foreground = Color(0.8f, 0.8314f, 0.8627f),
    foregroundMuted = Color(0.5451f, 0.5804f, 0.6196f),
    foregroundExtraMuted = Color(0.3329f, 0.3612f, 0.3962f),
    border = Color(0.2235f, 0.2392f, 0.2667f),
    borderAccent = Color(0.2824f, 0.302f, 0.3294f),
    accent = Color(0.3451f, 0.651f, 1f),
    accentForeground = Color(0.0667f, 0.0667f, 0.0784f),
    destructive = Color(1f, 0.497f, 0.4496f),
    dotSuccess = Color(0.4134f, 0.7632f, 0.4395f, 0.16f),
    dotDanger = Color(0.9107f, 0.4277f, 0.3847f, 0.14f),
    dotWarning = Color(0.8235f, 0.6f, 0.1333f),
  )
