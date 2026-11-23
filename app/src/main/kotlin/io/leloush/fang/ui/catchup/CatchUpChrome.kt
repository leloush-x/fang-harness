package io.leloush.fang.ui.catchup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.leloush.fang.R
import io.leloush.fang.ui.theme.FangTokens

/** The deck's chrome: the remaining count with Leave and Undo above, the three actions below. */
@Composable
internal fun CatchUpChrome(
  state: CatchUpDeckState,
  onLeave: () -> Unit,
  onUndo: () -> Unit,
  onMarkAll: () -> Unit,
  onDismiss: (CatchUpDisposition) -> Unit,
  modifier: Modifier = Modifier,
) {
  val count = state.queue.size
  val busy = state.handling != null
  Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = pluralStringResource(R.plurals.catch_up_left, count, count),
        modifier = Modifier.weight(1f),
        fontWeight = FontWeight.SemiBold,
      )
      TextButton(onClick = onUndo, enabled = state.canUndo) { Text(stringResource(R.string.catch_up_undo)) }
      TextButton(onClick = onLeave) { Text(stringResource(R.string.catch_up_leave)) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8)) {
      TextButton(
        onClick = { onDismiss(CatchUpDisposition.KeepUnread) },
        enabled = !busy && count > 0,
        modifier = Modifier.weight(1f),
      ) {
        Text(stringResource(R.string.catch_up_keep_unread))
      }
      TextButton(onClick = onMarkAll, enabled = !busy && count > 0, modifier = Modifier.weight(1f)) {
        Text(stringResource(R.string.catch_up_mark_all))
      }
      TextButton(
        onClick = { onDismiss(CatchUpDisposition.MarkRead) },
        enabled = !busy && count > 0,
        modifier = Modifier.weight(1f),
      ) {
        Text(stringResource(R.string.catch_up_mark_read))
      }
    }
  }
}
