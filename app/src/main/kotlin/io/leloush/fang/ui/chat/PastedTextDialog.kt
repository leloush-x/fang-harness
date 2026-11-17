package io.leloush.fang.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** The full text of a long paste, in a scrollable, selectable view with copy and close actions. */
@Composable
internal fun PastedTextDialog(text: String, onDismiss: () -> Unit) {
  val colors = fangColors
  val copy = rememberCopyText()
  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(onClick = { copy(text) }) {
        Text(text = stringResource(R.string.chat_pasted_text_copy), color = colors.accent)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(text = stringResource(R.string.chat_pasted_text_close), color = colors.foreground)
      }
    },
    title = { Text(text = stringResource(R.string.chat_pasted_text), fontSize = FangTokens.FontSize.xl) },
    text = {
      SelectionContainer {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState()),
        ) {
          Text(text = text, color = colors.foreground, fontSize = FangTokens.FontSize.base)
        }
      }
    },
    containerColor = colors.surface1,
    titleContentColor = colors.foreground,
    textContentColor = colors.foreground,
  )
}

/** A 48dp tray card for a long paste: tapping opens its full text, and it has the same remove badge as other chips. */
@Composable
internal fun PastedTextChip(onOpen: () -> Unit, onRemove: () -> Unit) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.md)
  Box {
    Row(
      modifier = Modifier
        .height(48.dp)
        .widthIn(max = 200.dp)
        .background(colors.surface2, shape)
        .border(1.dp, colors.borderAccent, shape)
        .padding(horizontal = FangTokens.Spacing.s8)
        .clickable(onClickLabel = stringResource(R.string.chat_pasted_text_open), onClick = onOpen),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    ) {
      LucideIcon(icon = R.drawable.ic_lucide_file_text, contentDescription = null, size = FangTokens.IconSize.md)
      Text(
        text = stringResource(R.string.chat_pasted_text),
        color = colors.foreground,
        fontSize = FangTokens.FontSize.sm,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    RemoveBadge(onRemove)
  }
}
