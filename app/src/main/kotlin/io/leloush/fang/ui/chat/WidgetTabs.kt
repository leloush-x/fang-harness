package io.leloush.fang.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.leloush.fang.R
import io.leloush.fang.agent.AskRequest
import io.leloush.fang.agent.ExtensionWidget
import io.leloush.fang.ui.components.FangSheet
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import org.json.JSONObject

private val TAB_HEIGHT = 28.dp
private val TAB_LABEL_MAX_WIDTH = 150.dp

/** Reserved upper-edge tab key for the extension's ask request, so it shares the widgets' strip and panel slot. */
internal const val ASK_TAB_KEY = "\u0000ask"

/** How tall a widget's lines grow inside the composer before they scroll; the sheet shows them all. */
private val PANEL_MAX_HEIGHT = 180.dp

/** The same while the keyboard is up or a dialog waits: four lines, so the field and the dialog keep their room. */
internal val PANEL_TIGHT_MAX_HEIGHT = 72.dp

/** Lowers the panel's lines so the first one centres on the 28 dp expand button beside it: (28 - 18) / 2. */
private val PANEL_TEXT_TOP = 5.dp

/** How far a tab strip or a panel fades at an edge it scrolls past. */
private val SCROLL_FADE = 24.dp

private const val SHEET_MAX_HEIGHT = 0.9f

/** A widget with its key, as a tab or panel shows it; [ask] is set on the synthetic tab for a pending ask request. */
internal class KeyedWidget(val key: String, val widget: ExtensionWidget, val ask: AskRequest? = null)

/**
 * Which widget tab is open on each edge of the composer, and which widgets changed since the user last saw them. Kept
 * in the chat's [ChatView], so switching chats leaves both as they were.
 */
@Stable
internal class WidgetTabs {
  private var openAbove by mutableStateOf<String?>(null)
  private var openBelow by mutableStateOf<String?>(null)
  private val seen = mutableStateMapOf<String, List<String>>()
  private var askOpen by mutableStateOf(false)
  private var askSession: AskSession? = null

  /**
   * Mirrors the ask send failure so the folded tab can tint itself. The panel that receives the failure state sets this,
   * since the tab strip is drawn by the composer, which does not hold that state.
   */
  var askFailed by mutableStateOf(false)

  /** The key of the tab open on the editor's [belowEditor] edge; the ask tab owns the upper edge when open. */
  fun open(belowEditor: Boolean): String? = when {
    belowEditor -> openBelow
    askOpen -> ASK_TAB_KEY
    else -> openAbove
  }

  /** Opens [key]'s tab in place of its edge's open one, or closes it when it is the open one. */
  fun toggle(key: String, belowEditor: Boolean) {
    if (key == ASK_TAB_KEY) return toggleAsk()
    val next = key.takeIf { it != open(belowEditor) }
    if (belowEditor) {
      openBelow = next
    } else {
      openAbove = next
      if (next != null) askOpen = false
    }
  }

  /** Opens or folds the ask tab; opening it folds whichever widget tab was open above the editor. */
  fun toggleAsk() {
    askOpen = !askOpen
    if (askOpen) openAbove = null
  }

  /** Folds the ask tab, e.g. after a drag down on its panel. */
  fun closeAsk() {
    askOpen = false
  }

  /** The answers for [request], kept while the ask tab folds so folding never discards what the user typed. */
  fun askSessionFor(request: AskRequest, onSubmit: (JSONObject) -> Unit): AskSession {
    val current = askSession
    if (current != null && current.requestId == request.id) return current
    return AskSession(request, onSubmit).also { askSession = it }
  }

  /** Whether [widget] changed since the user last saw it; a widget counts as seen when it first shows. */
  fun changed(key: String, widget: ExtensionWidget): Boolean = seen[key]?.let { it != widget.lines } == true

  fun see(key: String, widget: ExtensionWidget) {
    seen[key] = widget.lines
  }

  /**
   * Takes a new widget's lines as seen, so only its later changes mark its tab, and forgets widgets that are gone.
   * A tab closes when its widget goes or moves to the other edge.
   */
  fun track(widgets: Map<String, ExtensionWidget>) {
    seen.keys.retainAll(widgets.keys)
    for ((key, widget) in widgets) if (key !in seen) seen[key] = widget.lines
    val above = openAbove
    if (above != null && above != ASK_TAB_KEY && widgets[above]?.belowEditor != false) openAbove = null
    if (openBelow?.let { widgets[it] }?.belowEditor != true) openBelow = null
  }
}

/** The widgets on one edge of the composer, and the one whose panel shows; null while none does. */
internal class WidgetEdge(val widgets: List<KeyedWidget>, val shown: KeyedWidget?)

/**
 * The [widgets] on the editor's [belowEditor] edge, in their order, and the open tab's widget. A pending [ask] joins the
 * upper edge as a synthetic tab, so it uses the same strip and panel as the extension widgets.
 */
internal fun widgetEdge(
  widgets: Map<String, ExtensionWidget>,
  tabs: WidgetTabs,
  belowEditor: Boolean,
  ask: AskRequest? = null,
): WidgetEdge {
  val edge = widgets.filterValues {
    it.belowEditor == belowEditor
  }.map { KeyedWidget(it.key, it.value) }.toMutableList()
  if (!belowEditor && ask != null) {
    edge.add(0, KeyedWidget(ASK_TAB_KEY, ExtensionWidget(emptyList(), false), ask))
  }
  val open = tabs.open(belowEditor)
  return WidgetEdge(edge, edge.firstOrNull { it.key == open })
}

/**
 * The widgets Pi draws on one edge of the editor as tabs hanging from that edge of the composer: its top border for
 * widgets above the editor, its bottom border for those below. The strip shows its widgets at once and removes them
 * at once, with no unfolding. A label stops at 150 dp, and a strip with more tabs than fit scrolls sideways, fading
 * at an edge it continues past. A tab opens its widget's panel, or closes it when open.
 */
@Composable
internal fun WidgetTabStrip(edge: WidgetEdge, tabs: WidgetTabs, belowEditor: Boolean, modifier: Modifier = Modifier) {
  val widgets = edge.widgets
  if (widgets.isEmpty()) return
  val state = rememberLazyListState()
  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .height(TAB_HEIGHT)
      .clipToBounds()
      .fadeScrolledEdges(state, vertical = false),
    state = state,
    contentPadding = PaddingValues(horizontal = FangTokens.Spacing.s16),
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
  ) {
    items(widgets, key = { it.key }) { item ->
      val ask = item.ask
      val open = item.key == edge.shown?.key
      WidgetTab(
        label = if (ask != null) {
          pluralStringResource(R.plurals.chat_ask_bulge_count, ask.questions.size, ask.questions.size)
        } else {
          widgetLabel(item.key)
        },
        icon = when {
          ask != null -> R.drawable.ic_lucide_message_circle_question
          belowEditor -> R.drawable.ic_lucide_panel_bottom
          else -> R.drawable.ic_lucide_panel_top
        },
        open = open,
        changed = ask == null && tabs.changed(item.key, item.widget),
        failed = ask != null && tabs.askFailed,
        belowEditor = belowEditor,
        onClick = { tabs.toggle(item.key, belowEditor) },
        modifier = Modifier,
        actionLabel = if (ask != null) {
          stringResource(if (open) R.string.chat_ask_bulge_fold else R.string.chat_ask_bulge_show)
        } else {
          null
        },
      )
    }
  }
}

/**
 * One tab: an icon for its edge, the label, and a chevron while it's open or a dot when the widget changed since the
 * user last saw it. An open tab takes the composer's color and covers the border where it meets the composer, so the
 * two read as one.
 */
@Composable
private fun WidgetTab(
  label: String,
  icon: Int,
  open: Boolean,
  changed: Boolean,
  belowEditor: Boolean,
  onClick: () -> Unit,
  modifier: Modifier,
  failed: Boolean = false,
  actionLabel: String? = null,
) {
  val colors = fangColors
  val shape = widgetTabShape(belowEditor)
  val fill = if (open) colors.surface1 else colors.surface0
  val description = widgetTabDescription(label, failed, changed)
  val action = actionLabel ?: stringResource(if (open) R.string.chat_widget_close else R.string.chat_widget_open)
  Row(
    modifier = modifier
      .height(TAB_HEIGHT)
      .clip(shape)
      .drawWithContent {
        drawContent()
        drawTabEdge(fill, open, belowEditor)
      }
      .background(fill)
      .border(1.dp, if (failed) colors.destructive else colors.borderAccent, shape)
      .clickable(onClickLabel = action, role = Role.Tab, onClick = onClick)
      .semantics {
        contentDescription = description
        selected = open
      }
      .padding(horizontal = FangTokens.Spacing.s8 + FangTokens.Spacing.s2),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s6),
  ) {
    LucideIcon(icon, null, size = FangTokens.IconSize.sm, tint = widgetTabIconTint(failed, open))
    Text(
      label,
      Modifier.widthIn(max = TAB_LABEL_MAX_WIDTH),
      widgetTabLabelColor(failed, open),
      fontSize = FangTokens.FontSize.sm,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    when {
      open -> LucideIcon(
        icon = if (belowEditor) R.drawable.ic_lucide_chevron_up else R.drawable.ic_lucide_chevron_down,
        contentDescription = null,
        size = FangTokens.IconSize.xs,
        tint = colors.foregroundMuted,
      )

      failed || changed -> Box(
        Modifier.size(6.dp).background(if (failed) colors.destructive else colors.accent, CircleShape),
      )
    }
  }
}

/**
 * Fades the ends of a tab strip or a panel where it scrolls past them, so a tab or a line cut off at the edge reads as
 * more to scroll to.
 */
private fun Modifier.fadeScrolledEdges(state: ScrollableState, vertical: Boolean): Modifier =
  graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
    drawContent()
    val length = if (vertical) size.height else size.width
    val fade = SCROLL_FADE.toPx().coerceAtMost(length / 2)
    val fadeEdge = { start: Float, colors: List<Color> ->
      val end = start + fade
      drawRect(
        brush = if (vertical) {
          Brush.verticalGradient(colors, startY = start, endY = end)
        } else {
          Brush.horizontalGradient(colors, startX = start, endX = end)
        },
        topLeft = if (vertical) Offset(0f, start) else Offset(start, 0f),
        size = if (vertical) Size(size.width, fade) else Size(fade, size.height),
        blendMode = BlendMode.DstIn,
      )
    }
    if (state.canScrollBackward) fadeEdge(0f, listOf(Color.Transparent, Color.Black))
    if (state.canScrollForward) fadeEdge(length - fade, listOf(Color.Black, Color.Transparent))
  }

/**
 * The open tab inside the composer: a widget above the text field or under the toolbar, or the pending ask request's
 * questions. It grows open and folds closed, and switching tabs eases from one height to the other's. The user has seen
 * the widget's lines for as long as they show. A [tight] panel keeps to a few lines.
 */
@Composable
internal fun WidgetPanelSlot(
  shown: KeyedWidget?,
  tabs: WidgetTabs,
  belowEditor: Boolean,
  tight: Boolean,
  onExpand: (String) -> Unit,
  modifier: Modifier = Modifier,
  askFailed: Boolean = false,
  onSubmit: (JSONObject) -> Unit = {},
) {
  val ask = shown?.ask
  // Only the upper edge owns the ask tab; the lower slot's default false must not clear this.
  LaunchedEffect(belowEditor, askFailed) { if (!belowEditor) tabs.askFailed = askFailed }
  LaunchedEffect(shown?.key, shown?.widget) { if (ask == null) shown?.let { tabs.see(it.key, it.widget) } }
  if (shown == null) return
  Box(modifier) {
    if (ask != null) {
      AskPanel(tabs.askSessionFor(ask, onSubmit), askFailed, onFold = tabs::closeAsk)
    } else {
      WidgetPanel(shown, belowEditor, tight, onExpand = { onExpand(shown.key) })
    }
  }
}

/**
 * A widget's lines as Pi sent them, in monospace and wrapped like Pi's terminal UI, up to [PANEL_MAX_HEIGHT], or
 * [PANEL_TIGHT_MAX_HEIGHT] when [tight], and then scrolling. Unlike Pi's terminal UI, which shows the first 10 lines,
 * every line is here. Beside them at the top, a button opens the widget in a sheet.
 */
@Composable
private fun WidgetPanel(item: KeyedWidget, belowEditor: Boolean, tight: Boolean, onExpand: () -> Unit) {
  val colors = fangColors
  val text = remember(item.widget.lines, colors) { extensionWidgetText(item.widget.lines, colors) }
  val scroll = rememberScrollState()
  Column(modifier = Modifier.fillMaxWidth()) {
    if (belowEditor) HorizontalDivider(Modifier.padding(top = FangTokens.Spacing.s8), 1.dp, colors.border)
    // Weighted, so the dividers stay when the composer gives the panel less than its lines.
    Row(
      modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4),
    ) {
      Text(
        text = text,
        modifier = Modifier
          .weight(1f)
          .padding(top = PANEL_TEXT_TOP)
          .heightIn(max = if (tight) PANEL_TIGHT_MAX_HEIGHT else PANEL_MAX_HEIGHT)
          .fadeScrolledEdges(scroll, vertical = true)
          .verticalScroll(scroll),
        color = colors.foreground,
        fontFamily = FontFamily.Monospace,
        fontSize = FangTokens.FontSize.code,
        lineHeight = FangTokens.LineHeight.code,
      )
      IconButton28(
        icon = R.drawable.ic_lucide_maximize_2,
        contentDescription = stringResource(R.string.chat_widget_expand),
        onClick = onExpand,
        tint = colors.foregroundMuted,
        iconSize = FangTokens.IconSize.sm,
      )
    }
    if (!belowEditor) {
      HorizontalDivider(
        Modifier.padding(top = FangTokens.Spacing.s8, bottom = FangTokens.Spacing.s4),
        1.dp,
        colors.border,
      )
    }
  }
}

/** The whole widget over the chat, for one too long for its panel: its label over every line, selectable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetSheet(item: KeyedWidget, tabs: WidgetTabs, onDismiss: () -> Unit) {
  val colors = fangColors
  val widget = item.widget
  val text = remember(widget.lines, colors) { extensionWidgetText(widget.lines, colors) }
  LaunchedEffect(widget) { tabs.see(item.key, widget) }
  FangSheet(onDismissRequest = onDismiss, maxHeightFraction = SHEET_MAX_HEIGHT) {
    Row(
      modifier = Modifier.padding(
        start = FangTokens.Spacing.s16,
        end = FangTokens.Spacing.s16,
        bottom = FangTokens.Spacing.s12,
      ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    ) {
      LucideIcon(
        icon = if (widget.belowEditor) R.drawable.ic_lucide_panel_bottom else R.drawable.ic_lucide_panel_top,
        contentDescription = null,
        size = FangTokens.IconSize.md,
        tint = colors.accent,
      )
      Text(
        text = widgetLabel(item.key),
        modifier = Modifier.weight(1f),
        color = colors.foreground,
        fontSize = FangTokens.FontSize.lg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
      Text(
        text = text,
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(start = FangTokens.Spacing.s16, end = FangTokens.Spacing.s16, bottom = FangTokens.Spacing.s24),
        color = colors.foreground,
        fontFamily = FontFamily.Monospace,
        fontSize = FangTokens.FontSize.code,
        lineHeight = FangTokens.LineHeight.code,
      )
    }
  }
}

/**
 * A tab's label: the widget's key with dashes and underscores as spaces and its first letter capitalized, so
 * `plan-todos` reads "Plan todos". Pi gives a widget no title of its own, only the key its extension chose.
 */
internal fun widgetLabel(key: String): String =
  key.replace('-', ' ').replace('_', ' ').trim().replaceFirstChar { it.titlecase() }.ifEmpty { key }
