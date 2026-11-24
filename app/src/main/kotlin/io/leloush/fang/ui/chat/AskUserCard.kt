package io.leloush.fang.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.agent.AskOption
import io.leloush.fang.agent.AskQuestion
import io.leloush.fang.agent.AskRequest
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fold tuning from the upstream request dock (RequestCollapse.kt): a downward drag folds once it passes 0.3 of the
 * panel's height, or on a 700 dp/s flick. Fang folds without a mid-drag spring, so these just gate the same drag.
 */
private const val FOLD_THRESHOLD_FRACTION = 0.3f
private const val FOLD_FLICK_DP_PER_SEC = 700f

/** Folds once a downward drag passes the threshold fraction of the panel or the upstream flick speed. */
private fun Modifier.foldOnDrag(onFold: () -> Unit): Modifier = pointerInput(Unit) {
  var down = 0f
  detectVerticalDragGestures(onVerticalDrag = { change, drag ->
    if (drag <= 0f) {
      down = 0f
      return@detectVerticalDragGestures
    }
    down += drag
    val elapsed = (change.uptimeMillis - change.previousUptimeMillis).toFloat().coerceAtLeast(1f)
    if (down >= FOLD_THRESHOLD_FRACTION * size.height || drag * 1000f / elapsed >= FOLD_FLICK_DP_PER_SEC * density) {
      onFold()
    }
  })
}

/**
 * The pending `ask` request as the panel of the composer's Ask tab: a fixed heading with a fold chevron, and a capped,
 * scrollable question body with the action row pinned below it. Questions show one at a time; Skip leaves one unanswered
 * and Dismiss cancels the whole request. Only the last question's Continue becomes Submit, which sends every answer
 * through [onSubmit] via [AskSession]. A failed send keeps the panel with Retry. A drag down folds the tab.
 */
@Composable
internal fun AskPanel(session: AskSession, failed: Boolean, onFold: () -> Unit, modifier: Modifier = Modifier) {
  val colors = fangColors
  val scope = rememberCoroutineScope()
  val reduceMotion = rememberReduceMotion()
  val expiresAt = session.expiresAt
  // Pi's own timeout aborts its wait; tell it the panel is gone when that deadline passes too.
  LaunchedEffect(expiresAt) {
    if (expiresAt == null) return@LaunchedEffect
    delay((expiresAt - System.currentTimeMillis()).coerceAtLeast(0))
    session.cancel()
  }
  val request = session.request
  val shape = RoundedCornerShape(FangTokens.Radius.lg)
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(colors.surface1)
      .border(1.dp, colors.borderAccent, shape)
      .foldOnDrag(onFold),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(
        start = FangTokens.Spacing.s12,
        end = FangTokens.Spacing.s12,
        top = FangTokens.Spacing.s8,
      ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
    ) {
      AskHeader(request.heading, session.index, request.questions.size)
      LucideIcon(
        icon = R.drawable.ic_lucide_chevron_up,
        contentDescription = stringResource(R.string.chat_ask_bulge_fold),
        modifier = Modifier.clickable(onClick = onFold),
        size = FangTokens.IconSize.sm,
        tint = colors.foregroundMuted,
      )
    }
    // The body takes only the room left after the heading and the actions, and scrolls when the keyboard squeezes the
    // composer: Skip, the arrows and Submit must never be pushed off the bottom of the panel.
    Column(
      modifier = Modifier
        .weight(1f, fill = false)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = FangTokens.Spacing.s12, vertical = FangTokens.Spacing.s8),
      verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    ) {
      QuestionSection(request.questions[session.index], request.allowComment, session.answers[session.index]) { title ->
        scheduleAutoAdvance(session, title, scope, reduceMotion)
      }
      if (failed) AskFailure(session)
    }
    AskActions(session, Modifier.padding(horizontal = FangTokens.Spacing.s12, vertical = FangTokens.Spacing.s8))
  }
}

/** The failure row inside the expanded panel, with the Retry the closed tab can't reach. */
@Composable
private fun AskFailure(session: AskSession) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.md)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(colors.surface2)
      .border(1.dp, colors.border, shape)
      .padding(FangTokens.Spacing.s12),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    Text(
      text = stringResource(R.string.chat_ask_failed),
      modifier = Modifier.weight(1f),
      color = colors.foregroundMuted,
      fontSize = FangTokens.FontSize.sm,
    )
    AskButton(stringResource(R.string.chat_retry), onClick = session::retry)
  }
}

/** The request's heading, or a default, with the current question's place beside it. */
@Composable
private fun RowScope.AskHeader(heading: String?, index: Int, count: Int) {
  Row(horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6), modifier = Modifier.weight(1f)) {
    Text(
      text = heading ?: stringResource(R.string.chat_ask_title),
      modifier = Modifier.weight(1f),
      color = fangColors.foreground,
      fontSize = FangTokens.FontSize.base,
    )
    Text(
      text = stringResource(R.string.ask_page_progress, index + 1, count),
      color = fangColors.foregroundMuted,
      fontSize = FangTokens.FontSize.sm,
    )
  }
}

/** One question's text, its optional context, its options and its optional freeform and comment fields. */
@Composable
private fun QuestionSection(
  question: AskQuestion,
  allowComment: Boolean,
  state: AnswerState,
  onPick: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6)) {
    Text(text = question.question, color = fangColors.foreground, fontSize = FangTokens.FontSize.base)
    question.context?.let {
      Text(text = it, color = fangColors.foregroundMuted, fontSize = FangTokens.FontSize.sm)
    }
    // Two aligned columns keep several short options fully visible; long descriptions stay one per row.
    val columns = if (question.options.size > 3 && question.options.none { it.description != null }) 2 else 1
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
      verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4),
      maxItemsInEachRow = columns,
    ) {
      question.options.forEach { option ->
        Box(Modifier.weight(1f, fill = true)) { OptionRow(option, question.allowMultiple, state, onPick) }
      }
    }
    if (question.allowMultiple && question.options.isNotEmpty()) {
      Text(
        text = stringResource(R.string.ask_choose_multiple),
        color = fangColors.foregroundMuted,
        fontSize = FangTokens.FontSize.sm,
      )
    }
    // A question with no options has only the text field, even if the model left allowFreeform false.
    if (question.allowFreeform || question.options.isEmpty()) {
      Text(
        text = stringResource(R.string.chat_ask_freeform),
        color = fangColors.foregroundMuted,
        fontSize = FangTokens.FontSize.sm,
      )
      InlineField(state.freeform, stringResource(R.string.chat_ask_freeform_hint))
    }
    if (allowComment) InlineField(state.comment, stringResource(R.string.chat_ask_comment_hint))
  }
}

/** One option as a checkbox or radio row, with its title and, below it, its optional description. */
@Composable
private fun OptionRow(option: AskOption, multiple: Boolean, state: AnswerState, onPick: (String) -> Unit) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.md)
  val picked = option.title in state.selected
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(if (picked) colors.surface2 else Color.Transparent)
      .selectable(selected = picked, role = if (multiple) Role.Checkbox else Role.RadioButton) {
        state.pick(option.title, multiple)
        onPick(option.title)
      }
      .heightIn(min = 34.dp)
      .padding(horizontal = FangTokens.Spacing.s8, vertical = FangTokens.Spacing.s6),
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SelectionMark(picked, multiple)
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = option.title,
        color = if (picked) colors.foreground else colors.foregroundMuted,
        fontSize = FangTokens.FontSize.sm,
        maxLines = 2,
      )
      option.description?.let {
        Text(it, color = colors.foregroundMuted, fontSize = FangTokens.FontSize.sm, maxLines = 1)
      }
    }
  }
}

/** A filled checkbox with a tick, or a radio with an accent dot, in the option's colour when it is picked. */
@Composable
internal fun SelectionMark(picked: Boolean, multiple: Boolean) {
  val colors = fangColors
  val border = if (picked) colors.accent else colors.foregroundExtraMuted
  if (multiple) {
    Box(Modifier.size(16.dp).border(1.dp, border, RoundedCornerShape(FangTokens.Radius.sm)), Alignment.Center) {
      if (picked) LucideIcon(R.drawable.ic_lucide_check, null, size = 12.dp, tint = colors.accentForeground)
    }
  } else {
    Box(Modifier.size(16.dp).border(1.dp, border, CircleShape), Alignment.Center) {
      if (picked) Box(Modifier.size(7.dp).clip(CircleShape).background(colors.accent))
    }
  }
}

/** A small one-line field with a placeholder, for the freeform answer or the optional comment. */
@Composable
private fun InlineField(state: TextFieldState, hint: String) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.md)
  BasicTextField(
    state = state,
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(colors.surface2)
      .border(1.dp, colors.border, shape)
      .padding(FangTokens.Spacing.s12),
    textStyle = TextStyle(color = colors.foreground, fontSize = FangTokens.FontSize.base),
    cursorBrush = SolidColor(colors.accent),
    decorator = TextFieldDecorator { inner ->
      Box {
        if (state.text.isEmpty()) Text(hint, color = colors.foregroundMuted, fontSize = FangTokens.FontSize.sm)
        inner()
      }
    },
  )
}

/** Skip/Dismiss above a `<` back arrow and a `>` forward arrow; the forward arrow becomes Submit on the last page. */
@Composable
private fun AskActions(session: AskSession, modifier: Modifier = Modifier) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.md)
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6)) {
    Row(horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6)) {
      AskButton(stringResource(R.string.chat_ask_skip), onClick = session::skip)
      AskButton(stringResource(R.string.chat_dismiss), onClick = session::dismiss)
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // < backward — one question back, disabled on the first.
      Box(
        modifier = Modifier.size(40.dp).clip(shape).background(colors.surface2)
          .clickable(enabled = session.index > 0, onClick = session::back),
        contentAlignment = Alignment.Center,
      ) {
        LucideIcon(
          R.drawable.ic_lucide_chevron_left,
          stringResource(R.string.chat_ask_back),
          size = FangTokens.IconSize.sm,
          tint = if (session.index > 0) colors.foreground else colors.foregroundExtraMuted,
        )
      }
      if (session.isLast) {
        AskButton(
          label = stringResource(R.string.chat_submit),
          filled = true,
          enabled = session.canContinue(),
          onClick = session::next,
        )
      } else {
        // > forward — enabled once something is picked.
        val canGo = session.canContinue()
        Box(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (canGo) colors.accent else colors.surface2)
            .clickable(enabled = canGo, onClick = session::next),
          contentAlignment = Alignment.Center,
        ) {
          LucideIcon(
            R.drawable.ic_lucide_chevron_right,
            stringResource(R.string.chat_ask_continue),
            size = FangTokens.IconSize.md,
            tint = if (canGo) colors.accentForeground else colors.foregroundExtraMuted,
          )
        }
      }
    }
  }
}

/** A full-width card button: muted on the card, or accent-filled for the answer action. */
@Composable
private fun RowScope.AskButton(label: String, filled: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.md)
  val content = if (filled) colors.accentForeground else colors.foregroundMuted
  Row(
    modifier = Modifier
      .weight(1f)
      .alpha(if (enabled) 1f else DISABLED_ALPHA)
      .clip(shape)
      .background(if (filled) colors.accent else colors.surface2)
      .border(1.dp, if (filled) colors.accent else colors.border, shape)
      .clickable(enabled = enabled, onClick = onClick)
      .heightIn(min = 40.dp)
      .padding(horizontal = FangTokens.Spacing.s8, vertical = FangTokens.Spacing.s8),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, color = content, fontSize = FangTokens.FontSize.base)
  }
}

/** One question's picks and text fields, kept while the user moves between questions. */
internal class AnswerState {
  val selected = mutableStateListOf<String>()
  val freeform = TextFieldState()
  val comment = TextFieldState()

  fun pick(title: String, multiple: Boolean) {
    if (!multiple) selected.clear()
    if (title in selected) selected.remove(title) else selected.add(title)
  }
}

/** Navigation and the answer JSON for one [AskRequest], held across folds of its tab. */
internal class AskSession(internal val request: AskRequest, private val onSubmit: (JSONObject) -> Unit) {
  val requestId: String = request.id
  val expiresAt: Long? = request.timeout?.let { System.currentTimeMillis() + it }
  val answers = List(request.questions.size) { AnswerState() }
  var index by mutableIntStateOf(0)
    private set

  /** The last submitted payload, so a failed send can be retried without changing any answer. */
  private var submitted: JSONObject? = null

  /** Set once answered or cancelled, so a pending auto-advance beat can't revive a request that is already gone. */
  var finished = false
    private set

  val isLast: Boolean get() = index == request.questions.lastIndex

  fun canContinue(): Boolean = with(answers[index]) {
    selected.isNotEmpty() || freeform.text.isNotBlank()
  }

  fun next() = advance()

  fun skip() {
    with(answers[index]) {
      selected.clear()
      freeform.clearText()
      comment.clearText()
    }
    advance()
  }

  fun back() {
    index--
  }

  fun dismiss() = submit(cancelled = true)

  /** Pi's deadline passed: cancel with empty answers, exactly as before the tab existed. */
  fun cancel() {
    finished = true
    val data = JSONObject().put("id", request.id).put("answers", JSONArray()).put("cancelled", true)
    submitted = data
    onSubmit(data)
  }

  /** Resends the last answer unchanged; only the failure path offers this. */
  fun retry() {
    submitted?.let(onSubmit)
  }

  private fun advance() {
    if (isLast) submit(cancelled = false) else index++
  }

  private fun submit(cancelled: Boolean) {
    finished = true
    val all = JSONArray()
    answers.forEach { all.put(answer(it)) }
    val data = JSONObject().put("id", request.id).put("answers", all)
    if (cancelled) data.put("cancelled", true)
    submitted = data
    onSubmit(data)
  }

  private fun answer(state: AnswerState): JSONObject {
    if (state.freeform.text.isNotBlank()) {
      val response = JSONObject().put("kind", "freeform").put("text", state.freeform.text.toString().trim())
      return JSONObject().put("status", "answered").put("response", response)
    }
    if (state.selected.isEmpty()) return JSONObject().put("status", "skipped")
    val response = JSONObject().put("kind", "selection").put("selections", JSONArray(state.selected.toList()))
    state.comment.text.toString().trim().takeIf { it.isNotEmpty() }?.let { response.put("comment", it) }
    return JSONObject().put("status", "answered").put("response", response)
  }
}

/**
 * A single choice on an earlier question moves on after a beat, as the upstream request dock does: 280 ms, or 120 ms
 * under Reduce Motion. The guards drop a stale beat when the pick changed, the page moved or the request ended.
 */
private fun scheduleAutoAdvance(session: AskSession, title: String, scope: CoroutineScope, reduceMotion: Boolean) {
  if (session.isLast || session.request.questions[session.index].allowMultiple) return
  val page = session.index
  scope.launch {
    delay(if (reduceMotion) 120L else 280L)
    if (session.finished || session.index != page) return@launch
    val answer = session.answers[page]
    if (answer.selected.size != 1 || title !in answer.selected) return@launch
    session.next()
  }
}

private const val DISABLED_ALPHA = 0.5f
