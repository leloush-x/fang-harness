package io.leloush.fang.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.leloush.fang.agent.TimelineItem
import io.leloush.fang.ui.theme.FangTokens

/** Where a just-sent bubble starts below its resting place, tuned by eye so it begins near the composer. */
private val EntranceRise = 16.dp

/** The measured settle target: a sent bubble is at rest within 350 ms of the finger lifting. */
private const val ENTRANCE_DURATION_MS = 350

/** Which corners of a user bubble touch neighbouring bubbles in the same run. */
internal data class BubbleCorners(val joinedAbove: Boolean = false, val joinedBelow: Boolean = false)

/** Squares only the corners that meet another bubble; the free end keeps its rounded corners. */
internal fun bubbleShape(corners: BubbleCorners) = RoundedCornerShape(
  topStart = if (corners.joinedAbove) FangTokens.Radius.sm else FangTokens.Radius.xxl,
  topEnd = FangTokens.Radius.sm,
  bottomEnd = if (corners.joinedBelow) FangTokens.Radius.sm else FangTokens.Radius.xxl,
  bottomStart = if (corners.joinedBelow) FangTokens.Radius.sm else FangTokens.Radius.xxl,
)

/**
 * Decides which user messages rise on arrival. Keys already in the stream at first composition are restored history
 * and draw in place; a key arriving later rises once, then settles so scrolling back cannot replay it.
 */
internal class BubbleEntrances {
  private val decisions = HashMap<String, Boolean>()
  private val restored = HashSet<String>()
  private var seeded = false

  /**
   * Marks the first non-empty list as restored history. Pi can seed a chat after its first composition, so waiting
   * for the first keys rather than the first composition stops that history from rising as if it were just sent.
   */
  fun restore(items: List<TimelineItem>) {
    if (seeded || items.isEmpty()) return
    seeded = true
    items.forEach { item ->
      restored += item.key
      decisions[item.key] = false
    }
  }

  /** Stable per key: true only the first time a newly arrived key is seen. */
  fun shouldRise(key: String): Boolean = decisions.getOrPut(key) { key !in restored }

  /** Marks a key as done rising, so a later composition or a bubble recomposed after scrolling does not replay it. */
  fun settle(key: String) {
    decisions[key] = false
  }
}

/**
 * Offsets a just-arrived bubble below its place and rises it to rest once. [entrance] is false for restored history
 * and already-shown items, so those draw in their final place. The offset is draw-only, so layout, scrolling and
 * auto-follow never see it.
 */
@Composable
internal fun Modifier.bubbleEntrance(key: String, entrance: Boolean, onConsumed: (String) -> Unit): Modifier {
  val rise = remember { entrance }
  var risen by remember { mutableStateOf(!rise) }
  val distance = with(LocalDensity.current) { EntranceRise.toPx() }
  val offset = animateFloatAsState(
    targetValue = if (risen) 0f else distance,
    animationSpec = tween(ENTRANCE_DURATION_MS, easing = FastOutSlowInEasing),
    label = "bubble_entrance",
  )
  LaunchedEffect(key) {
    if (rise && !risen) {
      risen = true
      onConsumed(key)
    }
  }
  return this.graphicsLayer { translationY = offset.value }
}
