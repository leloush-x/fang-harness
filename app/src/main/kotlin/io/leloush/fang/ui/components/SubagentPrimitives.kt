package io.leloush.fang.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
/** A circular icon button, 28dp unless [modifier] sets a size. */
@Composable
fun RoundIconButton(
  @DrawableRes icon: Int,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  background: Color = Color.Transparent,
  tint: Color = fangColors.foreground,
  iconSize: Dp = FangTokens.IconSize.md,
) {
  Box(
    modifier = modifier.roundButton(size = 28.dp, background = background, enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    LucideIcon(icon = icon, contentDescription = contentDescription, size = iconSize, tint = tint)
  }
}

internal fun Modifier.roundButton(size: Dp, background: Color, enabled: Boolean, onClick: () -> Unit): Modifier =
  alpha(if (enabled) 1f else 0.5f)
    .size(size)
    .clip(CircleShape)
    .background(background)
    .clickable(enabled = enabled, onClick = onClick)

@Composable
internal fun CapsLabel(
  text: String,
  color: Color,
  modifier: Modifier = Modifier,
  fontSize: TextUnit = 10.5.sp,
  fontWeight: FontWeight = FontWeight.ExtraBold,
) {
  Text(
    text = text.toUpperCase(LocaleList.current),
    modifier = modifier.semantics { contentDescription = text },
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    letterSpacing = 0.06.em,
    maxLines = 1,
  )
}


/** A [HeaderBar] button; a [filled] one sits on a raised disc, as the explorer's toggle does. */
@Composable
fun HeaderButton(
  @DrawableRes icon: Int,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  filled: Boolean = false,
  tint: Color = fangColors.foreground,
) {
  Box(
    modifier = modifier.roundButton(
      size = HeaderButtonSize,
      background = if (filled) fangColors.surface2 else Color.Transparent,
      enabled = enabled,
      onClick = onClick,
    ),
    contentAlignment = Alignment.Center,
  ) {
    LucideIcon(icon = icon, contentDescription = contentDescription, size = HEADER_ICON_SIZE, tint = tint)
  }
}

private val HEADER_ICON_SIZE = 18.dp

/** The colours of a [PillButton]. */
enum class PillTone { Primary, Destructive, Tonal }

/** A fully rounded button, 40dp tall, whose [label] wraps onto a second line rather than cutting off. */
@Composable
fun PillButton(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tone: PillTone = PillTone.Primary,
  enabled: Boolean = true,
  @DrawableRes icon: Int? = null,
) {
  val colors = fangColors
  val (container, content) = when (tone) {
    PillTone.Primary -> colors.accent to colors.accentForeground
    PillTone.Destructive -> colors.destructive to colors.destructiveForeground
    PillTone.Tonal -> colors.surface2 to colors.foregroundMuted
  }
  Button(
    onClick = onClick,
    modifier = modifier.heightIn(min = 40.dp),
    enabled = enabled,
    shape = RoundedCornerShape(20.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = container,
      contentColor = content,
      disabledContainerColor = container.copy(alpha = DISABLED_ALPHA * container.alpha),
      disabledContentColor = content.copy(alpha = DISABLED_ALPHA),
    ),
    contentPadding = PaddingValues(horizontal = FangTokens.Spacing.s16, vertical = FangTokens.Spacing.s8),
  ) {
    if (icon != null) {
      LucideIcon(icon = icon, contentDescription = null, size = 15.dp, tint = LocalContentColor.current)
      Box(Modifier.size(FangTokens.Spacing.s8))
    }
    Text(
      text = label,
      fontSize = FangTokens.FontSize.base,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

private const val DISABLED_ALPHA = 0.4f

/** A steering action: cobalt text, on a cobalt-tinted pill when [filled]. */
@Composable
internal fun SteerButton(label: String, enabled: Boolean, filled: Boolean, onClick: () -> Unit) {
  val colors = fangColors
  Text(
    text = label,
    modifier = Modifier
      .padding(start = FangTokens.Spacing.s4)
      .alpha(if (enabled) 1f else DISABLED_ALPHA)
      .heightIn(min = BUTTON_HEIGHT)
      .clip(CircleShape)
      .background(if (filled) colors.accentSoft else Color.Transparent)
      .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
      .padding(horizontal = if (filled) FangTokens.Spacing.s12 else 10.dp, vertical = 7.dp),
    color = colors.accent,
    fontSize = 12.5.sp,
    fontWeight = FontWeight.Bold,
    maxLines = 1,
  )
}

internal fun TabsButton(tabs: Int, onClick: () -> Unit) {
  val colors = fangColors
  val description = pluralStringResource(R.plurals.shell_tabs_button, tabs, tabs)
  val number = remember(tabs) { NumberFormat.getIntegerInstance().format(tabs.coerceAtMost(MAX_SHOWN_TABS)) }
  val count = if (tabs > MAX_SHOWN_TABS) stringResource(R.string.shell_tabs_more, number) else number
  Box(
    modifier = Modifier
      .roundButton(size = HeaderButtonSize, background = colors.surface2, enabled = true, onClick = onClick)
      .semantics {
        contentDescription = description
        role = Role.Button
      },
    contentAlignment = Alignment.Center,
  ) {
    LucideIcon(
      icon = R.drawable.ic_lucide_square,
      contentDescription = null,
      size = TABS_ICON_SIZE,
      tint = colors.foreground,
    )
    Text(
      text = count,
      modifier = Modifier.clearAndSetSemantics {},
      color = colors.foreground,
      fontSize = if (count.length > 2) 7.sp else 9.sp,
      fontWeight = FontWeight.ExtraBold,
      style = LocalTextStyle.current.copy(fontFeatureSettings = Pipki
