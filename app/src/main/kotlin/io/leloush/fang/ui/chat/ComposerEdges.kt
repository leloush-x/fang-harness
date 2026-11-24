package io.leloush.fang.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
  // A new request opens its panel so Skip and the answer buttons are visible at once; it folds when answered.
  LaunchedEffect(askRequest?.id) { if (askRequest != null) tabs.openAsk() else tabs.closeAsk() }
  // The open tab is part of the key: without it, tapping a tab never invalidates the remembered panel.
  val above = remember(widgets, askRequest, tabs, tabs.open(belowEditor = false)) {
    widgetEdge(widgets, tabs, belowEditor = false, ask = askRequest)
  }
  val below = remember(widgets, tabs, tabs.open(belowEditor = true)) {
    widgetEdge(widgets, tabs, belowEditor = true)
  }
  return ComposerEdges(above, below, askFailed)
}

/** Hides the keyboard and clears focus, so the composer can call it without owning the focus/keyboard state. */
@Composable
internal fun rememberHideKeyboard(): () -> Unit {
  val keyboard = LocalSoftwareKeyboardController.current
  val focus = LocalFocusManager.current
  return remember(keyboard, focus) {
    {
      keyboard?.hide()
      focus.clearFocus()
    }
  }
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
