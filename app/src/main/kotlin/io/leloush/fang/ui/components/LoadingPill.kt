package io.leloush.fang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** A pot and [text] in a pill, pinned over other content while something loads. */
@Composable
internal fun LoadingPill(text: String, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(FangTokens.Radius.xxl))
      .background(fangColors.surface2)
      .padding(horizontal = FangTokens.Spacing.s12, vertical = FangTokens.Spacing.s8),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    SimmeringPot(size = FangTokens.IconSize.sm)
    Text(text = text, color = fangColors.foregroundMuted, fontSize = FangTokens.FontSize.meta)
  }
}
