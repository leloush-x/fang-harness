package io.leloush.fang.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.theme.fangColors

/** The tab's corners follow the composer edge it hangs from. */
internal fun widgetTabShape(belowEditor: Boolean) = if (belowEditor) {
  androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
} else {
  androidx.compose.foundation.shape.RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
}

/** A tab's accessibility label: a failed answer reads so, then a change marker, then the widget's own label. */
@Composable
internal fun widgetTabDescription(label: String, failed: Boolean, changed: Boolean): String = when {
  failed -> stringResource(R.string.ask_failed_tab)
  changed -> stringResource(R.string.chat_widget_changed, label)
  else -> label
}

@Composable
internal fun widgetTabIconTint(failed: Boolean, open: Boolean): Color = when {
  failed -> fangColors.destructive
  open -> fangColors.accent
  else -> fangColors.foregroundMuted
}

@Composable
internal fun widgetTabLabelColor(failed: Boolean, open: Boolean): Color = when {
  failed -> fangColors.destructive
  open -> fangColors.foreground
  else -> fangColors.foregroundMuted
}

/** The open tab's accent hairline on the edge that meets the composer. */
internal fun DrawScope.drawTabEdge(fill: Color, open: Boolean, belowEditor: Boolean) {
  if (!open) return
  val line = 1.dp.toPx()
  drawRect(
    color = fill,
    topLeft = Offset(line, if (belowEditor) 0f else size.height - line),
    size = Size(size.width - 2 * line, line),
  )
}
