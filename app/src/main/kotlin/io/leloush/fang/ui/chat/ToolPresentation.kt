package io.leloush.fang.ui.chat

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.agent.SUBAGENT_TOOL
import io.leloush.fang.agent.StepFigure
import io.leloush.fang.agent.TimelineItem
import io.leloush.fang.agent.ToolStatus
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.components.activityglyph.AgentActivity
import io.leloush.fang.ui.components.activityglyph.WorkingActivity
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import org.json.JSONObject

/** Past this many characters a tool's detail gets a folded line with a Show more control. */
private const val DETAIL_FOLD_CHARS = 120

/**
 * How a thinking block or tool call is labelled in its compact row and its detail sheet; [code] summaries are
 * monospace, and [figure] is what the row notes about a tool's result.
 */
internal data class ToolPresentation(
  @DrawableRes val icon: Int,
  val title: String,
  val summary: String?,
  val running: Boolean,
  val isError: Boolean,
  val code: Boolean = true,
  val figure: StepFigure? = null,
  /** The tool's result text, given to the completion glyph so it can hand off to a copy button. */
  val output: String? = null,
  /** The tool Pi called, kept so a row can tell a subagent call from an ordinary tool. */
  val toolName: String? = null,
) {
  /** Whether this call starts a subagent, which Pi may name `subagent` or `task`. */
  val isSubagent: Boolean get() = toolName == SUBAGENT_TOOL || toolName == "task"

  /** The longer of the row's two texts, when it is worth folding; the sheet shows both in full. */
  fun foldedDetail(): String? {
    val outputText = output?.trim().takeUnless { it.isNullOrEmpty() }
    val summaryText = summary?.trim().takeUnless { it.isNullOrEmpty() }
    return when {
      outputText != null && outputText.length > DETAIL_FOLD_CHARS -> outputText
      summaryText != null && summaryText.length > DETAIL_FOLD_CHARS -> summaryText
      else -> null
    }
  }
}

@Composable
internal fun toolPresentation(item: TimelineItem.ToolCall): ToolPresentation {
  val args = item.args
  val running = item.status != ToolStatus.Done
  val path = args?.stringOrNull("path")
  val presentation = when (item.toolName) {
    SUBAGENT_TOOL, "task" -> ToolPresentation(
      R.drawable.ic_lucide_bot,
      stringResource(R.string.tool_bubble_subagent),
      args?.stringOrNull("description") ?: path,
      running,
      item.isError,
    )

    "bash" -> ToolPresentation(
      R.drawable.ic_lucide_terminal,
      stringResource(R.string.chat_tool_shell),
      args?.stringOrNull("command")?.oneLine(),
      running,
      item.isError,
    )

    "read" -> ToolPresentation(
      R.drawable.ic_lucide_file_text,
      stringResource(R.string.chat_tool_read),
      path,
      running,
      item.isError,
    )

    "write" -> ToolPresentation(
      R.drawable.ic_lucide_file_pen,
      stringResource(R.string.chat_tool_write),
      path,
      running,
      item.isError,
    )

    "edit" -> ToolPresentation(
      R.drawable.ic_lucide_file_pen,
      stringResource(R.string.chat_tool_edit),
      path,
      running,
      item.isError,
    )

    else -> ToolPresentation(R.drawable.ic_lucide_wrench, item.toolName, path, running, item.isError)
  }
  return presentation.copy(figure = item.figure, output = item.output, toolName = item.toolName)
}

@Composable
internal fun toolPresentation(item: TimelineItem.Thinking): ToolPresentation = ToolPresentation(
  R.drawable.ic_lucide_brain,
  stringResource(R.string.chat_thinking),
  // Reasoning usually opens with a bold heading line; show it without the markdown markers.
  item.text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.trim('*', ' '),
  item.streaming,
  isError = false,
  code = false,
)

/**
 * The row's badge: a destructive tool icon on failure, else the working activity glyph, which settles into a copy
 * button or the tool's own icon. A subagent's [ToolPresentation.icon] is its bot, so the settled glyph names it too.
 */
@Composable
internal fun ToolStatusGlyph(presentation: ToolPresentation) {
  val colors = fangColors
  Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
    if (presentation.isError) {
      LucideIcon(
        icon = presentation.icon,
        contentDescription = null,
        size = FangTokens.IconSize.sm,
        tint = colors.destructive,
      )
    } else {
      // A finished tool hands its result to a copy button; without output it settles back to the tool's own icon.
      val copyText = presentation.output
      val glyphLabel = stringResource(if (copyText == null) R.string.morph_complete else R.string.morph_copy_output)
      val settledIcon = if (copyText == null) presentation.icon else null
      val working = WorkingActivity(running = presentation.running, finished = !presentation.running)
      // A subagent call with no result yet is still spawning, which the glyph shows as a handoff.
      val spawning = presentation.isSubagent && presentation.running && copyText == null
      fun phase(): AgentActivity = if (spawning) AgentActivity.HANDOFF else working.activity()
      WorkingToCopyGlyph(
        activity = phase(),
        copyText = copyText,
        size = FangTokens.IconSize.sm,
        contentDescription = glyphLabel,
        doneIcon = settledIcon,
        derive = { phase() },
      )
    }
  }
}

/** Android's `optString` turns JSON null and non-strings into text; this only accepts real strings. */
internal fun JSONObject.stringOrNull(name: String): String? = opt(name) as? String

private fun String.oneLine(): String = replace('\n', ' ').trim()
