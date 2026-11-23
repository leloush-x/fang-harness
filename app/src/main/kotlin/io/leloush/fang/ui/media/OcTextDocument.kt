package io.leloush.fang.ui.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.FangMarkdown
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

private val headerHeight = 72.dp

/** A scrollable document with a markdown/source toggle; the top padding clears the viewer's chrome. */
@Composable
internal fun OcTextDocument(title: String, content: String, modifier: Modifier = Modifier) {
  var rendered by remember(content) { mutableStateOf(true) }
  val colors = fangColors
  val toggle = stringResource(if (rendered) R.string.oc_media_view_source else R.string.oc_media_view_rendered)
  Column(
    modifier
      .fillMaxSize()
      .systemBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(
        start = FangTokens.Spacing.s16,
        end = FangTokens.Spacing.s16,
        top = headerHeight,
        bottom = FangTokens.Spacing.s24,
      ),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = colors.foreground,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = { rendered = !rendered }) { Text(toggle) }
    }
    Spacer(Modifier.height(FangTokens.Spacing.s12))
    if (rendered) {
      FangMarkdown(markdown = content, modifier = Modifier.fillMaxWidth())
    } else {
      SelectionContainer {
        Text(
          text = content,
          color = colors.foreground,
          fontFamily = FontFamily.Monospace,
          fontSize = FangTokens.FontSize.code,
        )
      }
    }
  }
}
