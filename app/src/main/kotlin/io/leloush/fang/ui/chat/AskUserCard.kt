package io.leloush.fang.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.leloush.fang.ui.theme.FangColors
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

/** How tall the scrollable question body grows before it scrolls, so a long question leaves room for the composer. */
private val ASK_PANEL_MAX_HEIGHT = 280.dp

/**
 * An extension's `ask` request as a small square bulge at the composer's top edge, like a widget tab: tap it to grow the
 * question card out of the composer, drag the card down or tap its chevron to fold it back. Questions show one at a time;
 * Skip leaves one unanswered and Dismiss cancels the whole request. Only the last question's Continue becomes Submit,
 * which sends every answer through [onSubmit]. A failed send keeps the card with Retry.
 */
@Composable
internal fun AskUserCard(
  request: AskRequest,
  failed: Boolean,
  onSubmit: (JSONObject) -> Unit,
  modifier: Modifier = Modifier,
) {
  val session = remember(request.id) { AskSession(request, onSubmit) }
  // Pi's own timeout aborts its wait; tell it the card is gone when that deadline passes too.
  val expiresAt = remember(request.id) { request.timeout?.let { System.currentTimeMillis() + it } }
  LaunchedEffect(expiresAt) {
    if (expiresAt == null) return@LaunchedEffect
    delay((expiresAt - System.currentTimeMillis()).coerceAtLeast(0))
    session.cancel()
  }
  var folded by remember(request.id) { mutableStateOf(false) }
  val colors = fangColors
  val counter = stringResource(R.string.chat_ask_counter, session.index + 1, request.questions.size)
  Box(modifier = modifier.fillMaxWidth().padding(horizontal = FangTokens.Spacing.s16), Alignment.TopStart) {
    Column(modifier = Modifier.widthIn(max = FangTokens.contentMaxWidth).fillMaxWidth()) {
      AskBulge(counter, request.questions.size, expanded = !folded, failed, onToggle = { folded = !folded })
      AnimatedVisibility(
        visible = !folded,
        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
        exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeOut(),
      ) {
        AskPanel(request, session, failed, colors) { folded = true }
      }
    }
  }
}

/** The card grown out of the bulge: a fixed heading with a fold chevron, and a capped, scrollable question body. */
@Composable
private fun AskPanel(
  request: AskRequest,
  session: AskSession,
  failed: Boolean,
  colors: FangColors,
  onFold: () -> Unit,
) {
  val shape = RoundedCornerShape(FangTokens.Radius.lg)
  Column(
    modifier = Modifier
      .clip(shape)
      .background(colors.surface1)
      .border(1.dp, colors.borderAccent, shape)
      .pointerInput(Unit) {
        detectVerticalDragGestures(onVerticalDrag = { _, drag -> if (drag > 0) onFold() })
      },
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
    Column(
      modifier = Modifier
        .padding(FangTokens.Spacing.s12)
        .heightIn(max = ASK_PANEL_MAX_HEIGHT)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s12),
    ) {
      QuestionSection(request.questions[session.index], request.allowComment, session.answers[session.index])
      if (failed) AskFailure(session)
      AskActions(session)
    }
  }
}

/** The failure row inside the expanded card, with the same Retry the folded bulge hides away. */
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
      text = stringResource(R.string.chat_ask_counter, index + 1, count),
      color = fangColors.foregroundMuted,
      fontSize = FangTokens.FontSize.sm,
    )
  }
}

/** One question's text, its optional context, its options and its optional freeform and comment fields. */
@Composable
private fun QuestionSection(question: AskQuestion, allowComment: Boolean, state: AnswerState) {
  Column(verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8)) {
    Text(text = question.question, color = fangColors.foreground, fontSize = FangTokens.FontSize.base)
    question.context?.let {
      Text(text = it, color = fangColors.foregroundMuted, fontSize = FangTokens.FontSize.sm)
    }
    question.options.forEach { OptionRow(it, question.allowMultiple, state) }
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
private fun OptionRow(option: AskOption, multiple: Boolean, state: AnswerState) {
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
      }
      .heightIn(min = 40.dp)
      .padding(horizontal = FangTokens.Spacing.s12, vertical = FangTokens.Spacing.s8),
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SelectionMark(picked, multiple)
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = option.title,
        color = if (picked) colors.foreground else colors.foregroundMuted,
        fontSize = FangTokens.FontSize.base,
      )
      option.description?.let { Text(it, color = colors.foregroundMuted, fontSize = FangTokens.FontSize.sm) }
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

/** Skip and Dismiss on one row, Back and the answer action on the next; Continue is off until something is picked. */
@Composable
private fun AskActions(session: AskSession) {
  Column(verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6)) {
    Row(horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6)) {
      AskButton(stringResource(R.string.chat_ask_skip), onClick = session::skip)
      AskButton(stringResource(R.string.chat_dismiss), onClick = session::dismiss)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6)) {
      if (session.index > 0) AskButton(stringResource(R.string.chat_ask_back), onClick = session::back)
      AskButton(
        label = stringResource(if (session.isLast) R.string.chat_submit else R.string.chat_ask_continue),
        filled = true,
        enabled = session.canContinue(),
        onClick = session::next,
      )
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
private class AnswerState {
  val selected = mutableStateListOf<String>()
  val freeform = TextFieldState()
  val comment = TextFieldState()

  fun pick(title: String, multiple: Boolean) {
    if (!multiple) selected.clear()
    if (title in selected) selected.remove(title) else selected.add(title)
  }
}

/** Navigation and the answer JSON for one [AskRequest], held across recompositions for its card. */
private class AskSession(private val request: AskRequest, private val onSubmit: (JSONObject) -> Unit) {
  val answers = List(request.questions.size) { AnswerState() }
  var index by mutableIntStateOf(0)
    private set

  /** The last submitted payload, so a failed send can be retried without changing any answer. */
  private var submitted: JSONObject? = null

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

  /** Pi's deadline passed: cancel with empty answers, exactly as the timeout did before folding existed. */
  fun cancel() {
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

private const val DISABLED_ALPHA = 0.5f
