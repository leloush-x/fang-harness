package io.leloush.fang.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONObject

/** The composer's widget tabs and the pending ask state, collected together to keep [Composer] short. */
internal class ComposerEdges(val above: WidgetEdge, val below: WidgetEdge, val askFailed: Boolean)

/** Tracks the extension widgets and ask request and resolves the two tab edges. */
@Composable
internal fun rememberComposerEdges(controller: ChatController): ComposerEdges {
  val tabs = controller.view.widgetTabs
  val widgets by controller.process.extensionWidgets.collectAsStateWithLifecycle()
  val askRequest by controller.process.ask.request.collectAsStateWithLifecycle()
  val askFailed by controller.process.ask.failed.collectAsStateWithLifecycle()
  LaunchedEffect(widgets) { tabs.track(widgets) }
  // A new request starts folded, and the tab goes away with the one it answered.
  LaunchedEffect(askRequest?.id) { tabs.closeAsk() }
  // The open tab is part of the key: without it, tapping a tab never invalidates the remembered panel.
  val above = remember(widgets, askRequest, tabs, tabs.open(belowEditor = false)) {
    widgetEdge(widgets, tabs, belowEditor = false, ask = askRequest)
  }
  val below = remember(widgets, tabs, tabs.open(belowEditor = true)) {
    widgetEdge(widgets, tabs, belowEditor = true)
  }
  return ComposerEdges(above, below, askFailed)
}

/** The upper panel slot, split out to keep [Composer] within detekt's method limit. */
@Composable
internal fun UpperPanelSlot(
  shown: WidgetEdge,
  tabs: WidgetTabs,
  tight: Boolean,
  onExpand: (String) -> Unit,
  panelModifier: Modifier,
  askFailed: Boolean,
  onAnswer: (JSONObject) -> Unit,
) {
  WidgetPanelSlot(shown.shown, tabs, false, tight, onExpand, panelModifier, askFailed, onAnswer)
}
