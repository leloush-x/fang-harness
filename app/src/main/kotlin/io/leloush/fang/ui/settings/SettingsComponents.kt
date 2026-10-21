package io.leloush.fang.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.leloush.fang.R
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** Paseo settings section: an optional 12sp muted title over a bordered surface1 card. */
@Composable
internal fun SettingsSection(
  title: String?,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.lg)
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8)) {
    if (title != null) {
      Text(
        text = title,
        modifier = Modifier.padding(horizontal = FangTokens.Spacing.s4),
        color = colors.foregroundMuted,
        fontSize = FangTokens.FontSize.sm,
      )
    }
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .clip(shape)
          .background(colors.surface1)
          .border(1.dp, colors.border, shape),
      content = content,
    )
  }
}

@Composable
internal fun SettingsDivider() {
  HorizontalDivider(thickness = 1.dp, color = fangColors.border)
}

/** A 56dp settings card row: optional leading icon, title, optional muted subtitle, trailing control. */
@Composable
internal fun SettingsRow(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  titleColor: Color = fangColors.foreground,
  enabled: Boolean = true,
  onClick: (() -> Unit)? = null,
  leading: (@Composable () -> Unit)? = null,
  trailing: @Composable RowScope.() -> Unit = {},
) {
  val colors = fangColors
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .alpha(if (enabled) 1f else 0.5f)
        .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
        .padding(horizontal = FangTokens.Spacing.s16, vertical = FangTokens.Spacing.s8),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s12),
  ) {
    if (leading != null) {
      Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) { leading() }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, color = titleColor, fontSize = FangTokens.FontSize.base)
      if (subtitle != null) {
        Text(
          text = subtitle,
          color = colors.foregroundMuted,
          fontSize = FangTokens.FontSize.sm,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    trailing()
  }
}

/** Muted trailing value of a [SettingsRow]. */
@Composable
internal fun SettingsValue(text: String) {
  Text(text = text, color = fangColors.foregroundMuted, fontSize = FangTokens.FontSize.base, maxLines = 1)
}

/** A 12sp line under a section or field; destructive for errors. */
@Composable
internal fun SettingsCaption(text: String, modifier: Modifier = Modifier, color: Color = fangColors.foregroundMuted) {
  Text(
    text = text,
    modifier = modifier.padding(horizontal = FangTokens.Spacing.s4),
    color = color,
    fontSize = FangTokens.FontSize.sm,
  )
}

/**
 * An error on a settings page, washed in the destructive colour: an optional bold [title], what to do in [message],
 * and the raw [detail] (a server's own words) in a selectable code font. [standalone], it is a rounded card with an
 * alert icon; otherwise it fills the bottom of a settings card, its text lined up with the row titles above it, whose
 * status icon already marks the error.
 */
@Composable
internal fun ErrorPanel(
  title: String?,
  message: String,
  modifier: Modifier = Modifier,
  detail: String? = null,
  standalone: Boolean = true,
) {
  val colors = fangColors
  val wash = colors.destructive.copy(alpha = if (colors.isDark) DARK_ERROR_WASH else LIGHT_ERROR_WASH)
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .background(wash, if (standalone) RoundedCornerShape(FangTokens.Radius.lg) else RectangleShape)
        .padding(
          start = if (standalone) FangTokens.Spacing.s12 else CARD_TEXT_START,
          top = FangTokens.Spacing.s12,
          end = if (standalone) FangTokens.Spacing.s12 else FangTokens.Spacing.s16,
          bottom = FangTokens.Spacing.s12,
        ),
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    if (standalone) {
      LucideIcon(
        icon = R.drawable.ic_lucide_circle_alert,
        contentDescription = null,
        modifier = Modifier.padding(top = FangTokens.Spacing.s2),
        tint = colors.destructive,
      )
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4)) {
      if (title != null) {
        Text(
          text = title,
          color = colors.foreground,
          fontSize = FangTokens.FontSize.base,
          fontWeight = FontWeight.Medium,
        )
      }
      // Without a title the message is the headline.
      Text(
        text = message,
        color = if (title == null) colors.foreground else colors.foregroundMuted,
        fontSize = if (title == null) FangTokens.FontSize.base else FangTokens.FontSize.sm,
        lineHeight = if (title == null) 20.sp else 17.sp,
      )
      if (detail != null) ErrorDetail(detail)
    }
  }
}

/** A server's own words, on an inset in the code font, selectable for copying into a bug report or search. */
@Composable
private fun ErrorDetail(detail: String) {
  val colors = fangColors
  SelectionContainer(modifier = Modifier.padding(top = FangTokens.Spacing.s4)) {
    Text(
      text = detail,
      modifier =
        Modifier
          .fillMaxWidth()
          .background(colors.surface0.copy(alpha = DETAIL_BACKGROUND_ALPHA), RoundedCornerShape(FangTokens.Radius.md))
          .padding(horizontal = FangTokens.Spacing.s8, vertical = FangTokens.Spacing.s6),
      color = colors.foreground,
      fontFamily = FontFamily.Monospace,
      fontSize = FangTokens.FontSize.code,
      lineHeight = FangTokens.LineHeight.code,
    )
  }
}

private const val DARK_ERROR_WASH = 0.14f
private const val LIGHT_ERROR_WASH = 0.08f
private const val DETAIL_BACKGROUND_ALPHA = 0.7f

/** Where a [SettingsRow]'s text starts after its leading icon: the row's padding, the 20dp icon box and the gap. */
private val CARD_TEXT_START = FangTokens.Spacing.s16 + 20.dp + FangTokens.Spacing.s12

/** Paseo's segmented control: a surface2 track with the selected segment raised on surface3. */
@Composable
internal fun <T> SegmentedControl(
  options: List<T>,
  selected: T,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  segment: @Composable (option: T, isSelected: Boolean) -> Unit,
) {
  val colors = fangColors
  Row(
    modifier =
      modifier
        .clip(RoundedCornerShape(FangTokens.Radius.md))
        .background(colors.surface2)
        .padding(FangTokens.Spacing.s2),
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s2),
  ) {
    options.forEach { option ->
      val isSelected = option == selected
      Box(
        modifier =
          Modifier
            .heightIn(min = 28.dp)
            .clip(RoundedCornerShape(FangTokens.Radius.base))
            .background(if (isSelected) colors.surface3 else Color.Transparent)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
            .padding(horizontal = FangTokens.Spacing.s8),
        contentAlignment = Alignment.Center,
      ) {
        segment(option, isSelected)
      }
    }
  }
}

/** Paseo's dashed "add" row below a card. */
@Composable
internal fun DashedActionRow(
  label: String,
  @DrawableRes icon: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = fangColors
  val radius = FangTokens.Radius.lg
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .drawBehind {
          val stroke = 1.dp.toPx()
          val dash = 4.dp.toPx()
          drawRoundRect(
            color = colors.borderAccent,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
          )
        }
        .clip(RoundedCornerShape(radius))
        .clickable(onClick = onClick)
        .padding(horizontal = FangTokens.Spacing.s16),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    LucideIcon(icon = icon, contentDescription = null, tint = colors.foregroundMuted)
    Text(text = label, color = colors.foregroundMuted, fontSize = FangTokens.FontSize.base)
  }
}

/** Paseo's small outline button for row actions. */
@Composable
internal fun SmallButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.xxl)
  Box(
    modifier =
      modifier
        .heightIn(min = 28.dp)
        .clip(shape)
        .background(colors.surface2)
        .border(1.dp, colors.borderAccent, shape)
        .alpha(if (enabled) 1f else 0.5f)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = FangTokens.Spacing.s12),
    contentAlignment = Alignment.Center,
  ) {
    Text(text = label, color = colors.foreground, fontSize = FangTokens.FontSize.sm)
  }
}

internal fun copyToClipboard(context: Context, label: String, text: String) {
  context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
}
