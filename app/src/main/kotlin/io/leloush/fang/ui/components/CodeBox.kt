package io.leloush.fang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** Paseo's code surface: surface2, 1dp border, monospace, scrolls sideways instead of wrapping. */
@Composable
internal fun CodeBox(
  modifier: Modifier = Modifier,
  radius: Dp = FangTokens.Radius.md,
  padding: Dp = FangTokens.Spacing.s12,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = fangColors
  val shape = RoundedCornerShape(radius)
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(shape)
        .background(colors.surface2)
        .border(1.dp, colors.border, shape)
        .horizontalScroll(rememberScrollState())
        .padding(padding),
    content = content,
  )
}

/** [text] in the code font, coloured for [language] (a fence name or file extension) when Highlights knows it. */
@Composable
internal fun CodeText(text: String, language: String? = null, color: Color = fangColors.foreground) {
  CodeText(rememberHighlightedCode(text, language), color)
}

/** Already styled [text] in the code font; [color] fills whatever its spans leave uncoloured. */
@Composable
internal fun CodeText(text: AnnotatedString, color: Color = fangColors.foreground) {
  Text(
    text = text,
    color = color,
    fontFamily = FontFamily.Monospace,
    fontSize = FangTokens.FontSize.code,
    lineHeight = FangTokens.LineHeight.code,
    softWrap = false,
  )
}
