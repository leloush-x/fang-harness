package io.leloush.fang.ui.git

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.host.GitChange
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import java.text.NumberFormat

/**
 * Paseo's document file header: the file's name and, muted, its folder, cut off at the end where they don't fit;
 * its added and removed line counts; and the icon of how it changed. A tap calls [onToggle], when given, to fold or
 * unfold the file; a long press does nothing, as in Paseo, where it only opens the file menu Fang Harness leaves out.
 */
@Composable
internal fun FileHeader(section: DiffSection, onToggle: (() -> Unit)?) {
  val colors = fangColors
  // Without a long-click handler, releasing a long press would count as a tap and fold the file.
  val clickable = if (onToggle == null) {
    Modifier
  } else {
    Modifier.combinedClickable(onLongClick = {}, hapticFeedbackEnabled = false, onClick = onToggle)
  }
  Column(modifier = Modifier.fillMaxWidth().background(colors.surface0)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .then(clickable)
        .height(29.dp)
        .padding(start = FangTokens.Spacing.s12, end = FangTokens.Spacing.s8),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    ) {
      Text(
        text = headerTitle(section, colors.foregroundMuted),
        modifier = Modifier.weight(1f),
        color = colors.foreground,
        fontSize = FangTokens.FontSize.base,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      DiffStat(section.file.additions, section.file.deletions)
      ChangeIcon(section.file.change)
    }
    HorizontalDivider(thickness = 1.dp, color = colors.borderAccent)
  }
}

/** The name, then the folder muted after a space, in one line so the name keeps the room it needs first. */
private fun headerTitle(section: DiffSection, folderColor: Color): AnnotatedString = buildAnnotatedString {
  append(section.name)
  if (section.folder.isNotEmpty()) withStyle(SpanStyle(color = folderColor)) { append(" ${section.folder}") }
}

/** Paseo's diff stat: `+added -removed`, compact, in green and red. */
@Composable
internal fun DiffStat(additions: Int, deletions: Int) {
  val colors = fangColors
  Row(horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4)) {
    Text(text = "+${compactCount(additions)}", color = colors.dotSuccess, fontSize = FangTokens.FontSize.sm)
    Text(text = "-${compactCount(deletions)}", color = colors.dotDanger, fontSize = FangTokens.FontSize.sm)
  }
}

/** [value] as Paseo's compact count, `1.2k`, with the app language's digits and unit. */
@Composable
private fun compactCount(value: Int): String {
  val (scaled, unit) = compactScale(value)
  // A compact count reads as one short figure: 2147.5m, not 2,147.5m.
  val number = NumberFormat.getNumberInstance().apply { isGroupingUsed = false }.format(scaled)
  return when (unit) {
    CountUnit.One -> number
    CountUnit.Thousand -> stringResource(R.string.git_count_thousands, number)
    CountUnit.Million -> stringResource(R.string.git_count_millions, number)
  }
}

/** Paseo's change icon: a green plus for an added file, a red minus for a deleted one, a dot for any other change. */
@Composable
private fun ChangeIcon(change: GitChange) {
  val colors = fangColors
  val (icon, tint, description) = when (change) {
    GitChange.Added -> Triple(R.drawable.ic_lucide_square_plus, colors.dotSuccess, R.string.git_file_added)

    GitChange.Deleted -> Triple(R.drawable.ic_lucide_square_minus, colors.dotDanger, R.string.git_file_deleted)

    GitChange.Modified, GitChange.Renamed, GitChange.Copied ->
      Triple(R.drawable.ic_lucide_square_dot, colors.dotWarning, R.string.git_file_modified)
  }
  LucideIcon(
    icon = icon,
    contentDescription = stringResource(description),
    size = FangTokens.IconSize.sm,
    tint = tint,
  )
}
