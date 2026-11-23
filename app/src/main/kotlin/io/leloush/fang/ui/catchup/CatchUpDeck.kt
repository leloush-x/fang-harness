package io.leloush.fang.ui.catchup

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateDpAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * A self-contained Catch Up deck: the waiting [sessions] as a swipeable card stack. Tap the top card to
 * open it, swipe right to mark it read or left to keep it unread. Undo re-queues the last card with the
 * inverse callback; once the stack empties a short celebration plays and [onLeave] runs. The deck owns
 * only UI state: [onMarkRead], [onKeepUnread] and [onMarkAll] are the app's hook for persistence.
 */
@Composable
fun CatchUpDeck(
  sessions: List<CatchUpSession>,
  onOpen: (CatchUpSession) -> Unit,
  onMarkRead: (CatchUpSession) -> Unit,
  onKeepUnread: (CatchUpSession) -> Unit,
  onMarkAll: () -> Unit,
  onLeave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = remember(sessions) { CatchUpDeckState(sessions, onMarkRead, onKeepUnread, onMarkAll) }
  val reduceMotion = rememberReduceMotion()
  val density = LocalDensity.current
  val offsetSpec: AnimationSpec<Dp> = if (reduceMotion) snap() else spring()
  var targetOffset by remember { mutableStateOf(0.dp) }
  val offset by animateDpAsState(targetOffset, offsetSpec, label = "catchUpCardOffset")
  val top = state.queue.firstOrNull()

  fun dismiss(disposition: CatchUpDisposition) {
    if (state.dismiss(disposition) == null) return
    val distance = if (disposition == CatchUpDisposition.MarkRead) FLIGHT_DISTANCE_DP else -FLIGHT_DISTANCE_DP
    targetOffset = with(density) { distance.dp }
  }
  val empty = state.queue.isEmpty() && state.handling == null
  CatchUpEffects(state, top?.id, reduceMotion, onLeave) { targetOffset = 0.dp }
  val card: @Composable (CatchUpSession) -> Unit = { session ->
    SwipeCard(
      session = session,
      offset = if (reduceMotion) 0.dp else offset,
      enabled = state.handling == null,
      reduceMotion = reduceMotion,
      onOpen = { onOpen(session) },
      onDismiss = ::dismiss,
      onDrag = { targetOffset = with(density) { it.toDp() } },
    )
  }
  Column(
    modifier.fillMaxSize().padding(FangTokens.Spacing.s16),
    verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s12),
  ) {
    CatchUpChrome(
      state = state,
      onLeave = onLeave,
      onUndo = { state.undo() },
      onMarkAll = { state.markAll() },
      onDismiss = ::dismiss,
    )
    Box(Modifier.fillMaxWidth().weight(1f)) {
      if (reduceMotion) {
        Crossfade(targetState = top) { if (it != null) card(it) }
      } else if (top != null) {
        card(top)
      }
      CatchUpCelebration(show = empty, reduceMotion = reduceMotion)
    }
  }
}

/** The deck's clocks: reset on a new top card, land an exit flight, then leave after the celebration. */
@Composable
private fun CatchUpEffects(
  state: CatchUpDeckState,
  topId: String?,
  reduceMotion: Boolean,
  onLeave: () -> Unit,
  onReset: () -> Unit,
) {
  LaunchedEffect(topId) { onReset() }
  LaunchedEffect(state.handling) {
    if (state.handling != null) {
      delay(if (reduceMotion) 0L else FLIGHT_MS)
      state.settle()
    }
  }
  val empty = state.queue.isEmpty() && state.handling == null
  LaunchedEffect(empty) {
    if (empty) {
      delay(FINISH_MS)
      onLeave()
    }
  }
}

/** The front card with its drag gesture: [onDrag] receives the travel in pixels, also for the snap back. */
@Composable
private fun SwipeCard(
  session: CatchUpSession,
  offset: Dp,
  enabled: Boolean,
  reduceMotion: Boolean,
  onOpen: () -> Unit,
  onDismiss: (CatchUpDisposition) -> Unit,
  onDrag: (Float) -> Unit,
) {
  val density = LocalDensity.current
  val modifier =
    catchUpDrag(session.id, onDrag) { travelled ->
      val dragged = with(density) { travelled.toDp() }
      if (abs(dragged.value) >= SWIPE_THRESHOLD_DP) {
        onDismiss(if (dragged.value > 0f) CatchUpDisposition.MarkRead else CatchUpDisposition.KeepUnread)
      } else {
        onDrag(0f)
      }
    }.offset(x = if (reduceMotion) 0.dp else offset)
  CatchUpCard(
    session = session,
    modifier = modifier,
    onOpen = onOpen,
    onMarkRead = { onDismiss(CatchUpDisposition.MarkRead) },
    onKeepUnread = { onDismiss(CatchUpDisposition.KeepUnread) },
    enabled = enabled,
  )
}

/** Tracks one horizontal drag on the front card; [onEnd] gets the total travel in pixels. */
private fun catchUpDrag(key: Any?, onDrag: (Float) -> Unit, onEnd: (Float) -> Unit): Modifier =
  Modifier.pointerInput(key) {
    var travelled = 0f
    detectHorizontalDragGestures(
      onHorizontalDrag = { change, amount ->
        change.consume()
        travelled += amount
        onDrag(travelled)
      },
      onDragEnd = { onEnd(travelled) },
    )
  }

/** The front card's surface: tap opens, TalkBack gets both swipes as actions. */
@Composable
private fun CatchUpCard(
  session: CatchUpSession,
  modifier: Modifier,
  onOpen: () -> Unit,
  onMarkRead: () -> Unit,
  onKeepUnread: () -> Unit,
  enabled: Boolean,
) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.xl)
  val description =
    if (session.subtitle != null) {
      stringResource(R.string.catch_up_card_with_subtitle, session.title, session.subtitle)
    } else {
      stringResource(R.string.catch_up_card, session.title)
    }
  Column(
    modifier
      .fillMaxWidth()
      .clip(shape)
      .background(colors.surface2)
      .border(1.dp, colors.borderAccent, shape)
      .then(
        if (enabled) {
          Modifier.clickable(onClickLabel = stringResource(R.string.catch_up_open), onClick = onOpen)
        } else {
          Modifier
        },
      )
      .semantics {
        contentDescription = description
        customActions =
          listOf(
            CustomAccessibilityAction(stringResource(R.string.catch_up_mark_read)) {
              onMarkRead()
              true
            },
            CustomAccessibilityAction(stringResource(R.string.catch_up_keep_unread)) {
              onKeepUnread()
              true
            },
          )
      }
      .padding(FangTokens.Spacing.s16),
    verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    CatchUpCardBody(session, colors.foreground, colors.foregroundMuted, colors.accent)
  }
}

/** The card's text: title, optional subtitle and a needs-input badge. */
@Composable
private fun CatchUpCardBody(session: CatchUpSession, title: Color, subtitle: Color, accent: Color) {
  Text(
    text = session.title,
    color = title,
    fontSize = FangTokens.FontSize.xl,
    fontWeight = FontWeight.SemiBold,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
  )
  if (session.subtitle != null) {
    Text(
      text = session.subtitle,
      color = subtitle,
      fontSize = FangTokens.FontSize.base,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
    )
  }
  if (session.needsInput) {
    Text(
      text = stringResource(R.string.catch_up_needs_input),
      color = accent,
      fontSize = FangTokens.FontSize.sm,
      fontWeight = FontWeight.Medium,
    )
  }
}

/** The brief finish after the last card: a check and "All caught up", scaling in; a fade under Reduce Motion. */
@Composable
private fun CatchUpCelebration(show: Boolean, reduceMotion: Boolean) {
  if (!show) return
  val progress by animateFloatAsState(
    1f,
    tween(FINISH_MS.toInt(), easing = FastOutSlowInEasing),
    label = "catchUpFinish",
  )
  val scale = if (reduceMotion) 1f else 0.8f + 0.2f * progress
  val label = stringResource(R.string.catch_up_all_caught_up)
  Column(
    Modifier
      .fillMaxSize()
      .graphicsLayer {
        alpha = progress
        scaleX = scale
        scaleY = scale
      }
      .semantics { contentDescription = label },
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    LucideIcon(
      icon = R.drawable.ic_lucide_circle_check,
      contentDescription = null,
      tint = fangColors.dotSuccess,
      size = FINISH_CHECK_SIZE,
    )
    Text(
      text = label,
      color = fangColors.foreground,
      fontSize = FangTokens.FontSize.xxl,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

/** Android's Reduce Motion (animator duration scale 0), read once per context. */
@Composable
private fun rememberReduceMotion(): Boolean {
  val context = LocalContext.current
  return remember(context) {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
  }
}

private const val SWIPE_THRESHOLD_DP = 96f
private const val FLIGHT_DISTANCE_DP = 1200f
private const val FLIGHT_MS = 240L
private const val FINISH_MS = 600L
private val FINISH_CHECK_SIZE = 56.dp
