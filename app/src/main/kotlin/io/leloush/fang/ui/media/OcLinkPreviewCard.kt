package io.leloush.fang.ui.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** A link card that presents a URL without loading anything; all three actions are caller callbacks. */
@Composable
fun OcLinkPreviewCard(
  url: String,
  title: String? = null,
  onCopy: () -> Unit,
  onShare: () -> Unit,
  onOpen: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val host = remember(url) { Uri.parse(url).host?.removePrefix("www.").orEmpty().ifBlank { url } }
  val colors = fangColors
  val label = stringResource(R.string.oc_media_link_card)
  Column(modifier.fillMaxWidth().semantics { contentDescription = label }) {
    Surface(shape = RoundedCornerShape(FangTokens.Radius.md), color = colors.surface2) {
      Row(Modifier.fillMaxWidth().padding(FangTokens.Spacing.s12), verticalAlignment = Alignment.CenterVertically) {
        Box(
          Modifier
            .size(36.dp)
            .background(colors.surface3, RoundedCornerShape(FangTokens.Radius.base)),
          contentAlignment = Alignment.Center,
        ) {
          LucideIcon(R.drawable.ic_lucide_external_link, contentDescription = null, tint = colors.foregroundMuted)
        }
        Spacer(Modifier.width(FangTokens.Spacing.s12))
        Column(Modifier.weight(1f)) {
          Text(
            text = title ?: host,
            color = colors.foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = host,
            color = colors.foregroundMuted,
            fontSize = FangTokens.FontSize.sm,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
    Row {
      TextButton(onClick = onCopy) { Text(stringResource(R.string.oc_media_link_copy)) }
      TextButton(onClick = onShare) { Text(stringResource(R.string.oc_media_link_share)) }
      TextButton(onClick = onOpen) { Text(stringResource(R.string.oc_media_link_open)) }
    }
  }
}
