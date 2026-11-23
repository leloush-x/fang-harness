package io.leloush.fang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

/** A GFM task list item's marker: a check in an accent square, or an empty outlined square. */
@Composable
internal fun FangMarkdownCheckBox(content: String, node: ASTNode) {
  val checked = node.getTextInNode(content).contains("[x]")
  val colors = fangColors
  val shape = RoundedCornerShape(FangTokens.Radius.sm)
  val label = stringResource(if (checked) R.string.markdown_oc_task_done else R.string.markdown_oc_task_todo)
  Box(
    modifier =
      Modifier
        .size(CHECK_SIZE)
        .clip(shape)
        .background(if (checked) colors.accent else Color.Transparent)
        .border(1.dp, if (checked) colors.accent else colors.border, shape)
        .semantics {
          role = Role.Checkbox
          stateDescription = label
        },
    contentAlignment = Alignment.Center,
  ) {
    if (checked) {
      LucideIcon(
        R.drawable.ic_lucide_check,
        contentDescription = null,
        size = FangTokens.IconSize.xs,
        tint = colors.accentForeground,
      )
    }
  }
}

/**
 * A mermaid fence as a labelled card holding its source. Fang has no WebView renderer, so the diagram is left as the
 * model wrote it and a tap on the card's button copies it; nothing is fetched or executed.
 */
@Composable
internal fun MermaidCard(source: String, modifier: Modifier = Modifier) {
  val label = stringResource(R.string.markdown_oc_diagram)
  Column(modifier = modifier.fillMaxWidth().padding(vertical = FangTokens.Spacing.s6)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = FangTokens.Spacing.s4),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        modifier = Modifier.weight(1f),
        color = fangColors.foregroundMuted,
        fontSize = FangTokens.FontSize.sm,
      )
      OcCopyButton(source)
    }
    CodeBox(radius = FangTokens.Radius.base, padding = FangTokens.Spacing.s8) { CodeText(source) }
  }
}

private val CHECK_SIZE = 16.dp
