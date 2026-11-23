package io.leloush.fang.ui.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private const val OC_SWIPE_MIN_DISTANCE_DP = 24f
private const val OC_SWIPE_MIN_FLING_DP_PER_S = 200f
private const val OC_SWIPE_VERTICAL_RATIO = 1.4f

/**
 * A quick downward fling on the focused field hides the keyboard; an upward fling on the unfocused field focuses it.
 * Pointer events are observed but never consumed, so taps, double-taps and text selection keep their own behaviour.
 */
@Composable
internal fun rememberOcSwipeModifier(controller: ChatController, hideKeyboard: () -> Unit): Modifier {
  val focus = remember { FocusRequester() }
  return remember(focus, controller, hideKeyboard) {
    Modifier
      .focusRequester(focus)
      .pointerInput(Unit) {
        observeOcVerticalSwipe(
          focused = { controller.composerFocused },
          onHide = hideKeyboard,
          onFocus = { focus.requestFocus() },
        )
      }
  }
}

/** shortcut: the distance and fling values are tuned for a thumb swipe; raise the fling speed if a slow selection drag dismisses the keyboard. */
private suspend fun PointerInputScope.observeOcVerticalSwipe(
  focused: () -> Boolean,
  onHide: () -> Unit,
  onFocus: () -> Unit,
) {
  val minDistance = with(density) { OC_SWIPE_MIN_DISTANCE_DP.dp.toPx() }
  val minFlingPerMs = with(density) { OC_SWIPE_MIN_FLING_DP_PER_S.dp.toPx() } / 1000f
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    var previous = down.position
    var totalX = 0f
    var totalY = 0f
    var lastTime = down.uptimeMillis
    var fast = false
    while (true) {
      val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
      if (change == null || !change.pressed) break
      val delta = change.position - previous
      previous = change.position
      totalX += delta.x
      totalY += delta.y
      val elapsed = change.uptimeMillis - lastTime
      if (elapsed > 0 && abs(delta.y) / elapsed >= minFlingPerMs) fast = true
      lastTime = change.uptimeMillis
    }
    if (!fast || abs(totalY) < minDistance || abs(totalY) < abs(totalX) * OC_SWIPE_VERTICAL_RATIO) {
      return@awaitEachGesture
    }
    if (focused()) {
      if (totalY >= minDistance) onHide()
    } else if (totalY <= -minDistance) {
      onFocus()
    }
  }
}
