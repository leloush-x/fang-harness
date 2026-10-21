package io.leloush.fang.ui.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.leloush.fang.R
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.files.Spinner
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** A note in place of a diff, centred: a spinner first while [loading], then [text] if any. */
@Composable
internal fun DiffNote(text: String?, modifier: Modifier = Modifier, loading: Boolean = false) {
  val colors = fangColors
  Column(
    modifier = modifier.fillMaxSize().padding(FangTokens.Spacing.s24),
    verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (loading) Spinner(FangTokens.IconSize.md, contentDescription = null)
    if (text != null) {
      Text(
        text = text,
        color = colors.foregroundMuted,
        fontSize = FangTokens.FontSize.base,
        textAlign = TextAlign.Center,
      )
    }
  }
}

/** Paseo's state for a diff too large to send: a file warning over the title and how to narrow it. */
@Composable
internal fun DiffTooLarge(modifier: Modifier = Modifier) {
  val colors = fangColors
  Column(
    modifier = modifier.fillMaxSize().padding(
      horizontal = FangTokens.Spacing.s24,
      vertical = FangTokens.Spacing.s48,
    ),
    verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s12),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    LucideIcon(
      icon = R.drawable.ic_lucide_file_warning,
      contentDescription = null,
      size = FangTokens.IconSize.lg,
      tint = colors.foregroundMuted,
    )
    Column(
      verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.git_diff_too_large_title),
        color = colors.foreground,
        fontSize = FangTokens.FontSize.base,
        textAlign = TextAlign.Center,
      )
      Text(
        text = stringResource(R.string.git_diff_too_large_description),
        color = colors.foregroundMuted,
        fontSize = FangTokens.FontSize.sm,
        textAlign = TextAlign.Center,
      )
    }
  }
}
