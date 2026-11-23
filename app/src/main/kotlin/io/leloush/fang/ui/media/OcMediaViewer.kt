package io.leloush.fang.ui.media

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.leloush.fang.R
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.subagents.rememberReduceMotion
import io.leloush.fang.ui.theme.FangTokens
import kotlin.math.abs

private const val DISMISS_DISTANCE_DP = 120f
private const val DISMISS_VELOCITY_DP = 900f
private const val MIN_SCALE = 0.6f

/**
 * A self-contained full-screen viewer. Back and the close button call [onDismiss]; a downward drag
 * past the distance or velocity threshold does too. Under Reduce Motion pages cross-fade and the
 * drag fades instead of shrinking the page.
 */
@Composable
fun OcMediaViewer(items: List<OcMediaItem>, initialIndex: Int, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  val reduceMotion = rememberReduceMotion()
  val pager =
    rememberPagerState(initialPage = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))) { items.size }
  var shown by remember { mutableStateOf(!reduceMotion) }
  var drawing by remember { mutableStateOf<Bitmap?>(null) }
  val pane = stringResource(R.string.oc_media_viewer_title)
  LaunchedEffect(Unit) { shown = true }
  BackHandler { onDismiss() }
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Box(modifier.fillMaxSize().background(Color.Black).semantics { paneTitle = pane }) {
      if (items.isEmpty()) {
        OcViewerChrome(0, 0, false, onDismiss) {}
        Text(stringResource(R.string.oc_media_empty), Modifier.align(Alignment.Center), color = Color.White)
      } else {
        val current = items[pager.currentPage]
        val canDraw = current is OcMediaItem.Image && androidBitmapOf(current.data) != null
        Crossfade(targetState = shown) { visible ->
          if (visible) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
              OcMediaPage(
                item = items[page],
                index = page,
                reduceMotion = reduceMotion,
                onDismiss = onDismiss,
                modifier =
                  Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (reduceMotion && page != pager.currentPage) 0f else 1f },
              )
            }
          }
        }
        OcViewerChrome(pager.currentPage + 1, items.size, canDraw, onDismiss) {
          if (current is OcMediaItem.Image) drawing = androidBitmapOf(current.data)
        }
      }
    }
  }
  drawing?.let { image ->
    OcImageDrawingEditor(image = image, onDismiss = { drawing = null }, onAdd = { drawing = null })
  }
}

@Composable
private fun OcMediaPage(
  item: OcMediaItem,
  index: Int,
  reduceMotion: Boolean,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var progress by remember { mutableFloatStateOf(0f) }
  var zoomed by remember { mutableStateOf(false) }
  val label = ocPageLabel(item, index)
  Box(
    modifier
      .fillMaxSize()
      .graphicsLayer {
        val shrunk = 1f - (1f - MIN_SCALE) * progress
        scaleX = if (reduceMotion) 1f else shrunk
        scaleY = scaleX
        alpha = 1f - progress
        shape = RoundedCornerShape(FangTokens.Radius.xl * progress)
        clip = true
      }
      .ocDragToDismiss(!zoomed, reduceMotion, { progress = it }, onDismiss)
      .semantics { contentDescription = label },
  ) {
    when (item) {
      is OcMediaItem.Image -> OcZoomableImage(item.data, label, { zoomed = it }, Modifier.fillMaxSize())

      is OcMediaItem.Text -> OcTextDocument(item.title, item.content)

      is OcMediaItem.File ->
        OcTextDocument(
          title = item.name,
          content = item.bytes?.decodeToString() ?: item.uri ?: stringResource(R.string.oc_media_file_unavailable),
        )
    }
  }
}

@Composable
private fun OcViewerChrome(current: Int, total: Int, canDraw: Boolean, onClose: () -> Unit, onDraw: () -> Unit) {
  val close = stringResource(R.string.oc_media_close)
  val draw = stringResource(R.string.oc_media_draw)
  val count = if (total == 0) "" else stringResource(R.string.oc_media_page_count, current, total)
  Row(
    Modifier.fillMaxWidth().systemBarsPadding().padding(FangTokens.Spacing.s8),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton28(icon = R.drawable.ic_lucide_x, contentDescription = close, onClick = onClose)
    Spacer(Modifier.weight(1f))
    Text(count, color = Color.White)
    Spacer(Modifier.weight(1f))
    if (canDraw) {
      IconButton28(icon = R.drawable.ic_lucide_pencil, contentDescription = draw, onClick = onDraw)
    } else {
      Spacer(Modifier.size(28.dp))
    }
  }
}

@Composable
private fun ocPageLabel(item: OcMediaItem, index: Int): String = when (item) {
  is OcMediaItem.Image -> stringResource(R.string.oc_media_image_label, index + 1)
  else -> stringResource(R.string.oc_media_document_label, index + 1)
}

/**
 * Claims downward drags in the Initial pass, before the image's zoom gestures see them. A drag is
 * only claimed once it is steeper than sideways, so horizontal page swipes still reach the pager.
 */
@Composable
private fun Modifier.ocDragToDismiss(
  canDismiss: Boolean,
  reduceMotion: Boolean,
  onProgress: (Float) -> Unit,
  onDismiss: () -> Unit,
): Modifier {
  val density = LocalDensity.current
  val distance = with(density) { DISMISS_DISTANCE_DP.dp.toPx() }
  val speed = with(density) { DISMISS_VELOCITY_DP.dp.toPx() }
  return pointerInput(canDismiss, reduceMotion, distance, speed, onProgress, onDismiss) {
    if (!canDismiss) return@pointerInput
    val tracker = VelocityTracker()
    awaitEachGesture {
      val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
      tracker.addPosition(down.uptimeMillis, down.position)
      var dy = 0f
      var claimed = false
      var progress = 0f
      while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
        tracker.addPosition(change.uptimeMillis, change.position)
        val dx = change.position.x - down.position.x
        dy = change.position.y - down.position.y
        if (!claimed && dy > 0f && dy > abs(dx)) claimed = true
        if (!claimed) {
          if (!change.pressed) break
          continue
        }
        change.consume()
        progress = (dy / (size.height * 0.5f)).coerceIn(0f, 1f)
        onProgress(progress)
        if (!change.pressed) break
      }
      if (!claimed) return@awaitEachGesture
      val threshold = if (reduceMotion) distance / 2f else distance
      settleDismiss(progress, dy > threshold || tracker.calculateVelocity().y > speed, onProgress, onDismiss)
    }
  }
}

private suspend fun settleDismiss(
  progress: Float,
  closing: Boolean,
  onProgress: (Float) -> Unit,
  onDismiss: () -> Unit,
) {
  if (closing) {
    animate(progress, 1f, animationSpec = tween(180)) { value, _ -> onProgress(value) }
    onDismiss()
  } else {
    animate(progress, 0f, animationSpec = spring()) { value, _ -> onProgress(value) }
    onProgress(0f)
  }
}
