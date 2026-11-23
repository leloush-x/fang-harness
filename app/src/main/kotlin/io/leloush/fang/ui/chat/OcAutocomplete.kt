package io.leloush.fang.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.R
import io.leloush.fang.appGraph
import io.leloush.fang.host.SessionInfo
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import io.leloush.fang.ui.workspace.sessionTitle

private const val OC_MENTION_MAX_SUGGESTIONS = 5

/** The `@query` ending the draft: [start]..[end] is the span a picked suggestion replaces. */
internal data class OcMentionTrigger(val start: Int, val end: Int, val query: String, val key: String)

/** Remembers the dismissed token so Escape closes the row without clearing the draft. */
internal class OcAutocompleteState {
  private var dismissedKey by mutableStateOf<String?>(null)

  fun trigger(text: CharSequence, selection: TextRange, focused: Boolean): OcMentionTrigger? {
    val start = text.lastIndexOf('@', startIndex = (text.length - 1).coerceAtLeast(0))
    val key = if (start < 0) "" else text.subSequence(start, text.length).toString()
    val query = key.removePrefix("@")
    return when {
      !focused || !selection.collapsed || selection.end != text.length -> null
      start < 0 || (start > 0 && !text[start - 1].isWhitespace()) -> null
      dismissedKey == key -> null
      query.any { it.isWhitespace() || it == '@' } -> null
      else -> OcMentionTrigger(start, text.length, query, key)
    }
  }

  fun onKey(event: KeyEvent, controller: ChatController): Boolean {
    if (event.type != KeyEventType.KeyDown || event.key != Key.Escape) return false
    val current = trigger(controller.draft.text, controller.draft.selection, controller.composerFocused) ?: return false
    dismissedKey = current.key
    return true
  }

  /** Inserts the picked session's name in place of the `@query` and clears the dismissal for that token. */
  fun insert(controller: ChatController, trigger: OcMentionTrigger, label: String) {
    val inserted = "@$label "
    controller.draft.edit {
      replace(trigger.start, trigger.end, inserted)
      selection = TextRange(trigger.start + inserted.length)
    }
    dismissedKey = null
  }
}

@Composable
internal fun rememberOcAutocomplete(): OcAutocompleteState = remember { OcAutocompleteState() }

/**
 * A single row of recent sessions above the field while the draft ends in `@query`. Picking one inserts its name,
 * which closes the row; Escape closes it without changing the draft. Slash commands keep their own menu.
 */
@Composable
internal fun OcMentionAutocomplete(
  controller: ChatController,
  autocomplete: OcAutocompleteState,
  modifier: Modifier = Modifier,
) {
  val sessions by LocalContext.current.appGraph.sessionRepository.sessions.collectAsStateWithLifecycle()
  val trigger by remember(autocomplete) {
    derivedStateOf {
      autocomplete.trigger(controller.draft.text, controller.draft.selection, controller.composerFocused)
    }
  }
  val active = trigger ?: return
  val shown = remember(active.query, sessions) { matchOcSessions(sessions, active.query) }
  if (shown.isEmpty()) return
  Row(
    modifier = modifier.horizontalScroll(rememberScrollState()).padding(bottom = FangTokens.Spacing.s4),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4),
  ) {
    shown.forEach { session ->
      val label = sessionTitle(session)
      val shape = RoundedCornerShape(FangTokens.Radius.xxl)
      Text(
        text = "@$label",
        modifier = Modifier
          .clip(shape)
          .background(fangColors.surface2, shape)
          .clickable(onClickLabel = stringResource(R.string.oc_mention_session, label)) {
            autocomplete.insert(controller, active, label)
          }
          .padding(horizontal = FangTokens.Spacing.s8, vertical = FangTokens.Spacing.s4),
        color = fangColors.foreground,
        fontSize = FangTokens.FontSize.sm,
        maxLines = 1,
      )
    }
  }
}

/** The sessions whose name, first message or id contains the query, most recent first, capped for a single row. */
private fun matchOcSessions(sessions: List<SessionInfo>, query: String): List<SessionInfo> {
  val needle = query.trim().lowercase()
  return sessions
    .asSequence()
    .filter {
      it.name?.contains(needle, ignoreCase = true) == true ||
        it.firstMessage.contains(needle, ignoreCase = true) ||
        it.id.contains(needle, ignoreCase = true)
    }
    .take(OC_MENTION_MAX_SUGGESTIONS)
    .toList()
}
