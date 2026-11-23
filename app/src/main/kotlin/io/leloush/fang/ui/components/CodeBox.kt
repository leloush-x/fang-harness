package io.leloush.fang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.chat.rememberCopyText
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import kotlinx.coroutines.delay

/**
 * Paseo's code surface: surface2, 1dp border, monospace, scrolls sideways instead of wrapping. With [copyText], a small
 * copy button floats over the top-right corner, outside the scrolling content so it stays put on long lines.
 */
@Composable
internal fun CodeBox(
  modifier: Modifier = Modifier,
  radius: Dp = FangTokens.Radius.md,
  padding: Dp = FangTokens.Spacing.s12,
  copyText: String? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = fangColors
  val shape = RoundedCornerShape(radius)
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(shape)
        .background(colors.surface2)
        .border(1.dp, colors.border, shape),
  ) {
    Column(
      modifier = Modifier.horizontalScroll(rememberScrollState()).padding(padding),
      content = content,
    )
    if (copyText != null) {
      OcCopyButton(
        text = copyText,
        modifier = Modifier.align(
          Alignment.TopEnd,
        ).padding(FangTokens.Spacing.s6).background(colors.surface2, CircleShape),
      )
    }
  }
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

/**
 * A 28dp copy button whose icon turns into a check for two seconds after each copy; another copy restarts the timer.
 * It never takes focus, so the keyboard stays up.
 */
@Composable
internal fun OcCopyButton(text: String, modifier: Modifier = Modifier, tint: Color = fangColors.foregroundMuted) {
  val copy = rememberCopyText()
  var copies by remember { mutableIntStateOf(0) }
  var showCheck by remember { mutableStateOf(false) }
  LaunchedEffect(copies) {
    if (copies == 0) return@LaunchedEffect
    showCheck = true
    delay(COPIED_FEEDBACK_MS)
    showCheck = false
  }
  Box(
    modifier = modifier.size(28.dp).clip(CircleShape).clickable {
      copy(text)
      copies++
    },
    contentAlignment = Alignment.Center,
  ) {
    LucideIcon(
      icon = if (showCheck) R.drawable.ic_lucide_check else R.drawable.ic_lucide_copy,
      contentDescription = stringResource(
        if (showCheck) R.string.markdown_oc_copied else R.string.markdown_oc_copy_code,
      ),
      size = FangTokens.IconSize.sm,
      tint = tint,
    )
  }
}

private const val COPIED_FEEDBACK_MS = 2_000L
