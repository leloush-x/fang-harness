package io.leloush.fang.ui.media

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.leloush.fang.R

private const val MAX_ZOOM = 5f

/** A fitted image with pinch-to-zoom and pan; at scale 1 it leaves single-finger drags untouched. */
@Composable
internal fun OcZoomableImage(
  data: Any,
  label: String,
  onZoomChange: (Boolean) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val bitmap = remember(data) { imageBitmapOf(data) }
  if (bitmap == null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(stringResource(R.string.oc_media_image_unavailable))
    }
    return
  }
  var scale by remember(data) { mutableFloatStateOf(1f) }
  var offset by remember(data) { mutableStateOf(Offset.Zero) }
  val zoomed = scale > 1.01f
  val zoomedState = stringResource(R.string.oc_media_zoomed_in)
  val reportZoom by rememberUpdatedState(onZoomChange)
  LaunchedEffect(zoomed) { reportZoom(zoomed) }
  Box(
    modifier
      .fillMaxSize()
      .pointerInput(bitmap) {
        detectTransformGestures { _, pan, zoom, _ ->
          val next = (scale * zoom).coerceIn(1f, MAX_ZOOM)
          val limitX = size.width * (next - 1f) / 2f
          val limitY = size.height * (next - 1f) / 2f
          offset = Offset((offset.x + pan.x).coerceIn(-limitX, limitX), (offset.y + pan.y).coerceIn(-limitY, limitY))
          scale = next
        }
      }
      .semantics {
        contentDescription = label
        if (zoomed) stateDescription = zoomedState
      },
    contentAlignment = Alignment.Center,
  ) {
    Image(
      bitmap = bitmap,
      contentDescription = null,
      contentScale = ContentScale.Fit,
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
          },
    )
  }
}

internal fun imageBitmapOf(data: Any): ImageBitmap? = when (data) {
  is ImageBitmap -> data
  is Bitmap -> data.asImageBitmap()
  else -> null
}

internal fun androidBitmapOf(data: Any): Bitmap? = when (data) {
  is Bitmap -> data
  is ImageBitmap -> data.asAndroidBitmap()
  else -> null
}
