package io.leloush.fang.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.leloush.fang.R
import io.leloush.fang.ui.components.LucideIcon

/** A pinned mini transcript in the selected palette; swapping the theme or mode dissolves it. */
@Composable
fun ThemePreview(mode: ThemeMode, modifier: Modifier = Modifier) {
  val selected = themeState.selectedTheme
  Crossfade(targetState = selected to mode, label = "theme-preview") { (theme, appearance) ->
    ThemePreviewBody(resolvedFangColors(appearance, theme), modifier)
  }
}

@Composable
private fun ThemePreviewBody(colors: FangColors, modifier: Modifier) {
  Box(
    modifier
      .clip(RoundedCornerShape(FangTokens.Radius.lg))
      .background(colors.surface0)
      .fillMaxWidth()
      .height(168.dp),
  ) {
    Column(Modifier.padding(horizontal = FangTokens.Spacing.s16, vertical = FangTokens.Spacing.s12)) {
      Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
          text = stringResource(R.string.oc_theme_preview_user),
          color = colors.foreground,
          fontSize = FangTokens.FontSize.sm,
          modifier =
            Modifier
              .clip(RoundedCornerShape(FangTokens.Radius.md))
              .background(colors.surface2)
              .padding(horizontal = FangTokens.Spacing.s12, vertical = FangTokens.Spacing.s8),
        )
      }
      Spacer(Modifier.height(FangTokens.Spacing.s12))
      val fileName = stringResource(R.string.oc_theme_preview_file)
      val reply = stringResource(R.string.oc_theme_preview_reply, fileName)
      val codeStart = reply.indexOf(fileName)
      Text(
        text =
          buildAnnotatedString {
            if (codeStart < 0) {
              append(reply)
            } else {
              append(reply.substring(0, codeStart))
              withStyle(SpanStyle(fontFamily = FangFonts.mono, background = colors.surface4)) { append(fileName) }
              append(reply.substring(codeStart + fileName.length))
            }
          },
        color = colors.foregroundMuted,
        fontSize = FangTokens.FontSize.sm,
      )
      Spacer(Modifier.height(FangTokens.Spacing.s8))
      PreviewCodeBand(colors)
    }
    // The soft scroll edge: the list content fades into the preview's canvas at the bottom.
    Box(
      Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(24.dp)
        .background(Brush.verticalGradient(listOf(Color.Transparent, colors.surface0))),
    )
  }
}

@Composable
private fun PreviewCodeBand(colors: FangColors) {
  val keyword = stringResource(R.string.oc_theme_preview_code_keyword)
  val type = stringResource(R.string.oc_theme_preview_code_type)
  val comment = stringResource(R.string.oc_theme_preview_code_comment)
  val literal = stringResource(R.string.oc_theme_preview_code_string)
  Box(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(FangTokens.Radius.base))
      .background(colors.surface1)
      .horizontalScroll(rememberScrollState())
      .padding(FangTokens.Spacing.s12),
  ) {
    Text(
      text =
        buildAnnotatedString {
          withStyle(SpanStyle(color = colors.accent)) { append(keyword) }
          append(" ")
          withStyle(SpanStyle(color = colors.dotWarning)) { append(type) }
          append(" { }\n  ")
          withStyle(SpanStyle(color = colors.foregroundMuted)) { append(comment) }
          append("\n  ")
          // The glass fill is translucent; make it legible as text in the preview.
          withStyle(SpanStyle(color = colors.dotSuccess.copy(alpha = 1f))) { append(literal) }
        },
      color = colors.foreground,
      fontFamily = FangFonts.mono,
      fontSize = FangTokens.FontSize.code,
      softWrap = false,
    )
  }
}

/** A horizontal selector of three-dot palette swatches; the checkmark marks the current one. */
@Composable
fun ThemeSwatchRow(
  mode: ThemeMode,
  selected: CuratedTheme,
  onSelect: (CuratedTheme) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = FangTokens.Spacing.s6),
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s12),
  ) {
    CuratedTheme.entries.forEach { theme ->
      ThemeSwatch(theme = theme, mode = mode, isSelected = theme == selected, onClick = { onSelect(theme) })
    }
  }
}

@Composable
private fun ThemeSwatch(theme: CuratedTheme, mode: ThemeMode, isSelected: Boolean, onClick: () -> Unit) {
  val colors = resolvedFangColors(mode, theme)
  val label = stringResource(theme.titleRes)
  val description = if (isSelected) stringResource(R.string.oc_theme_selected, label) else label
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      Modifier
        .clip(RoundedCornerShape(FangTokens.Radius.md))
        .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
        .semantics {
          contentDescription = description
          selected = isSelected
        }
        .padding(horizontal = FangTokens.Spacing.s8, vertical = FangTokens.Spacing.s6),
  ) {
    Box(Modifier.width(56.dp).height(28.dp), contentAlignment = Alignment.CenterStart) {
      listOf(colors.surface0, colors.accent, colors.dotWarning).forEachIndexed { index, fill ->
        Box(
          Modifier
            .offset(x = (index * 8).dp)
            .zIndex(index.toFloat())
            .size(FangTokens.Spacing.s16 + 3.dp)
            .clip(CircleShape)
            .background(fangColors.surface1)
            .padding(1.5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(Modifier.size(FangTokens.Spacing.s16).clip(CircleShape).background(fill).border(0.5.dp, colors.border))
        }
      }
      if (isSelected) {
        LucideIcon(
          icon = R.drawable.ic_lucide_check,
          contentDescription = null,
          modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
          tint = fangColors.accent,
          size = FangTokens.IconSize.sm,
        )
      }
    }
    Spacer(Modifier.height(FangTokens.Spacing.s4))
    Text(
      text = label,
      color = if (isSelected) fangColors.foreground else fangColors.foregroundMuted,
      fontSize = FangTokens.FontSize.sm,
      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
    )
  }
}
