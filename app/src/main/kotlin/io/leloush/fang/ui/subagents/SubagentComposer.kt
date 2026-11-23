package io.leloush.fang.ui.subagents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/**
 * The box under a live subagent's transcript: one steering line and a send button. [state] is owned by the
 * stack, so a draft survives covering the page with a nested subagent and returning to it.
 */
@Composable
internal fun SubagentComposer(
  state: TextFieldState,
  agentName: String,
  onSend: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val placeholder = stringResource(R.string.subagent_steer_placeholder, agentName)
  val sendLabel = stringResource(R.string.subagent_steer_send, agentName)
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.xxl)
  Box(
    modifier = modifier.fillMaxWidth().padding(
      horizontal = FangTokens.Spacing.s16,
      vertical = FangTokens.Spacing.s8,
    ),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier = Modifier.widthIn(max = FangTokens.contentMaxWidth).fillMaxWidth().clip(shape)
        .background(colors.surface2)
        .padding(
          start = FangTokens.Spacing.s16,
          end = FangTokens.Spacing.s4,
          top = FangTokens.Spacing.s8,
          bottom = FangTokens.Spacing.s8,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BasicTextField(
        state = state,
        modifier = Modifier.weight(1f).heightIn(min = 24.dp),
        textStyle = TextStyle(color = colors.foreground, fontSize = FangTokens.FontSize.content),
        cursorBrush = SolidColor(colors.accent),
        decorator = TextFieldDecorator { inner ->
          Box(contentAlignment = Alignment.CenterStart) {
            if (state.text.isEmpty()) {
              Text(placeholder, color = colors.foregroundMuted, fontSize = FangTokens.FontSize.content)
            }
            inner()
          }
        },
      )
      IconButton28(
        icon = R.drawable.ic_lucide_arrow_up,
        contentDescription = sendLabel,
        onClick = {
          val text = state.text.toString().trim()
          if (text.isNotEmpty()) {
            state.clearText()
            onSend(text)
          }
        },
        enabled = state.text.isNotBlank(),
        background = colors.accent,
        tint = colors.accentForeground,
      )
    }
  }
}
