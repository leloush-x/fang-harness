package io.leloush.fang.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.agent.AskAnswered
import io.leloush.fang.agent.AskQuestion
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import org.json.JSONObject

@Composable
internal fun AskAnsweredSummary(record: AskAnswered, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.lg)
  val views = remember(record) {
    (0 until record.answers.length()).map { index ->
      val answer = record.answers.optJSONObject(index) ?: JSONObject()
      val response = answer.optJSONObject("response")
      val picks = response?.optJSONArray("selections")
      Triple(
        answer.optString("status"),
        if (picks == null) emptySet() else (0 until picks.length()).map { picks.optString(it) }.toSet(),
        response?.optString("text")?.takeIf { it.isNotEmpty() },
      )
    }
  }
  Box(modifier = modifier.fillMaxWidth().padding(horizontal = FangTokens.Spacing.s16), Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = FangTokens.contentMaxWidth)
        .fillMaxWidth()
        .clip(shape)
        .background(colors.surface1)
        .border(1.dp, colors.borderAccent, shape)
        .padding(FangTokens.Spacing.s12)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = stringResource(R.string.chat_ask_answered),
          modifier = Modifier.weight(1f),
          color = colors.accent,
          fontSize = FangTokens.FontSize.sm,
        )
        Text(
          text = stringResource(R.string.chat_dismiss),
          modifier = Modifier.clickable(onClick = onDismiss),
          color = colors.foregroundMuted,
          fontSize = FangTokens.FontSize.sm,
        )
      }
      record.request.questions.forEachIndexed { index, question ->
        val (status, picked, text) = views.getOrNull(index) ?: Triple("", emptySet(), null)
        AnsweredQuestion(question, status, picked, text)
      }
    }
  }
}

/** One answered question's prompt, options with the picked ones marked, and any freeform text. */
@Composable
private fun AnsweredQuestion(
  question: AskQuestion,
  status: String,
  picked: Set<String>,
  text: String?,
) {
  val colors = fangColors
  Column(verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4)) {
    Text(text = question.question, color = colors.foreground, fontSize = FangTokens.FontSize.sm)
    question.options.forEach { option ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        SelectionMark(option.title in picked, question.allowMultiple)
        Text(
          text = option.title,
          color = if (option.title in picked) colors.foreground else colors.foregroundMuted,
          fontSize = FangTokens.FontSize.sm,
        )
      }
    }
    text?.let {
      Row(verticalAlignment = Alignment.CenterVertically) {
        LucideIcon(R.drawable.ic_lucide_check, null, size = 12.dp, tint = colors.accent)
        Text(text = it, color = colors.foreground, fontSize = FangTokens.FontSize.sm)
      }
    }
    if (status == "skipped" || (picked.isEmpty() && text == null)) {
      Text(
        text = stringResource(R.string.chat_ask_skipped),
        color = colors.foregroundMuted,
        fontSize = FangTokens.FontSize.sm,
      )
    }
  }
}

