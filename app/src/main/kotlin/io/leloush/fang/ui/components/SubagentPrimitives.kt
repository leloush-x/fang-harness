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
fun PanningText(text: AnnotatedString, modifier: Modifier = Modifier) {
  Text(
    text = text,
    modifier = modifier.basicMarquee(
      iterations = Int.MAX_VALUE,
      initialDelayMillis = 2500,
      repeatDelayMillis = 2500,
    ),
    color = fangColors.foregroundMuted,
    fontSize = FangTokens.FontSize.sm,
    maxLines = 1,
    softWrap = false,
  )
}

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
