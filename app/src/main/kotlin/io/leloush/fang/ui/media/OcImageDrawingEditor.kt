package io.leloush.fang.ui.media

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import io.leloush.fang.R
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.theme.FangTokens

private const val INK_RATIO = 0.006f

/** The five colours offered by the drawing editor, with their TalkBack names. */
enum class OcInk(val argb: Int, val label: Int) {
  BLACK(0xFF000000.toInt(), R.string.oc_media_ink_black),
  WHITE(0xFFFFFFFF.toInt(), R.string.oc_media_ink_white),
  RED(0xFFFF3B30.toInt(), R.string.oc_media_ink_red),
  GREEN(0xFF34C759.toInt(), R.string.oc_media_ink_green),
  BLUE(0xFF0A84FF.toInt(), R.string.oc_media_ink_blue),
}

/** One freehand stroke, in image pixel coordinates. */
data class OcStroke(val ink: OcInk, val points: List<Offset>)

/**
 * Marks up a copy of [image]; the original is never changed. [onAdd] receives the flattened bitmap
 * with every stroke drawn into it, so the caller can attach it wherever it likes.
 */
@Composable
fun OcImageDrawingEditor(
  image: Bitmap,
  onDismiss: () -> Unit,
  onAdd: (Bitmap) -> Unit,
  modifier: Modifier = Modifier,
) {
  var ink by remember { mutableStateOf(OcInk.RED) }
  var strokes by remember { mutableStateOf<List<OcStroke>>(emptyList()) }
  var live by remember { mutableStateOf<List<Offset>>(emptyList()) }
  val preview = remember(image) { image.asImageBitmap() }
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnClickOutside = false,
      decorFitsSystemWindows = false,
    ),
  ) {
    Column(modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
      OcDrawingTopBar(enabled = strokes.isNotEmpty(), onDismiss = onDismiss, onAdd = {
        onAdd(flattenDrawing(image, strokes))
      })
      Box(Modifier.fillMaxWidth().weight(1f)) {
        Canvas(
          modifier =
            Modifier
              .matchParentSize()
              .pointerInput(preview, ink) { collectStroke(ink, preview, { live = it }, { strokes = strokes + it }) },
        ) {
          val rect = fittedRect(preview.width, preview.height, size)
          drawMarks(preview, rect, strokes, live, ink)
        }
      }
      OcDrawingPalette(ink, { ink = it }, { strokes = strokes.dropLast(1) }) { strokes = emptyList() }
    }
  }
}

@Composable
private fun OcDrawingTopBar(enabled: Boolean, onDismiss: () -> Unit, onAdd: () -> Unit) {
  val close = stringResource(R.string.oc_media_close)
  val add = stringResource(R.string.oc_media_add_to_prompt)
  Row(Modifier.fillMaxWidth().padding(FangTokens.Spacing.s8), verticalAlignment = Alignment.CenterVertically) {
    IconButton28(icon = R.drawable.ic_lucide_x, contentDescription = close, onClick = onDismiss)
    Spacer(Modifier.weight(1f))
    TextButton(onClick = onAdd, enabled = enabled) { Text(add) }
  }
}

@Composable
private fun OcDrawingPalette(selected: OcInk, onSelect: (OcInk) -> Unit, onUndo: () -> Unit, onClear: () -> Unit) {
  val undo = stringResource(R.string.oc_media_undo)
  val clear = stringResource(R.string.oc_media_clear)
  Row(
    Modifier.fillMaxWidth().padding(FangTokens.Spacing.s8),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    OcInk.entries.forEach { ink -> OcInkSwatch(ink, ink == selected, onSelect) }
    Spacer(Modifier.weight(1f))
    TextButton(onClick = onUndo) { Text(undo) }
    TextButton(onClick = onClear) { Text(clear) }
  }
}

@Composable
private fun OcInkSwatch(ink: OcInk, selected: Boolean, onSelect: (OcInk) -> Unit) {
  val label = stringResource(ink.label)
  Box(
    Modifier
      .size(40.dp)
      .selectable(selected = selected, onClick = { onSelect(ink) })
      .semantics { contentDescription = label }
      .background(if (selected) Color.Gray else Color.Transparent, CircleShape)
      .padding(6.dp)
      .background(Color(ink.argb), CircleShape),
  )
}

/** Samples one finger into a stroke; points are converted from canvas pixels to image pixels. */
private suspend fun PointerInputScope.collectStroke(
  ink: OcInk,
  image: ImageBitmap,
  onLive: (List<Offset>) -> Unit,
  onCommit: (OcStroke) -> Unit,
) {
  val rect = fittedRect(image.width, image.height, Size(size.width.toFloat(), size.height.toFloat()))
  fun toImage(point: Offset): Offset = Offset(
    (point.x - rect.left) * image.width / rect.width,
    (point.y - rect.top) * image.height / rect.height,
  )
  awaitEachGesture {
    val down = awaitFirstDown()
    var points = listOf(toImage(down.position))
    onLive(points)
    while (true) {
      val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
      if (change == null || !change.pressed) break
      val point = toImage(change.position)
      if (points.last() != point) points = points + point
      onLive(points)
      change.consume()
    }
    if (points.size > 1) onCommit(OcStroke(ink, points))
    onLive(emptyList())
  }
}

/** The largest aspect-preserving rectangle for an image inside a canvas. */
private fun fittedRect(width: Int, height: Int, canvas: Size): Rect {
  val scale = minOf(canvas.width / width, canvas.height / height)
  val w = width * scale
  val h = height * scale
  return Rect(Offset((canvas.width - w) / 2f, (canvas.height - h) / 2f), Size(w, h))
}

private fun DrawScope.drawMarks(
  image: ImageBitmap,
  rect: Rect,
  strokes: List<OcStroke>,
  live: List<Offset>,
  ink: OcInk,
) {
  drawImage(
    image = image,
    dstOffset = IntOffset(rect.left.toInt(), rect.top.toInt()),
    dstSize = IntSize(rect.width.toInt(), rect.height.toInt()),
  )
  val pen = (rect.width * INK_RATIO).coerceAtLeast(2f)
  val marks = if (live.size > 1) strokes + OcStroke(ink, live) else strokes
  marks.forEach { stroke ->
    val points = stroke.points.map {
      Offset(
        rect.left + it.x * rect.width / image.width,
        rect.top + it.y * rect.height / image.height,
      )
    }
    drawPath(
      path = composePath(points),
      color = Color(stroke.ink.argb),
      style = Stroke(width = pen, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
  }
}

private fun composePath(points: List<Offset>): Path = Path().apply {
  moveTo(points.first().x, points.first().y)
  points.drop(1).forEach { lineTo(it.x, it.y) }
}

/** Draws the strokes into a fresh bitmap of the source's size and returns the annotated copy. */
private fun flattenDrawing(image: Bitmap, strokes: List<OcStroke>): Bitmap {
  val result = createBitmap(image.width, image.height)
  val canvas = android.graphics.Canvas(result)
  canvas.drawBitmap(image, 0f, 0f, null)
  val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    strokeWidth = (image.width * INK_RATIO).coerceAtLeast(2f)
  }
  strokes.forEach { stroke ->
    paint.color = stroke.ink.argb
    canvas.drawPath(androidPath(stroke.points), paint)
  }
  return result
}

private fun androidPath(points: List<Offset>): android.graphics.Path = android.graphics.Path().apply {
  moveTo(points.first().x, points.first().y)
  points.drop(1).forEach { lineTo(it.x, it.y) }
}
