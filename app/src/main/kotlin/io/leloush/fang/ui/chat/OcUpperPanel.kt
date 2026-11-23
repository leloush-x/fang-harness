package io.leloush.fang.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The upper widget/ask panel, split out to keep [Composer] within detekt's method limit. */
@Composable
internal fun OcUpperPanel(
  edges: ComposerEdges,
  tabs: WidgetTabs,
  tight: Boolean,
  openWidget: (String) -> Unit,
  panelWeight: Modifier,
  controller: ChatController,
) {
  val above = edges.above
  UpperPanelSlot(
    above,
    tabs,
    tight,
    openWidget,
    if (above.shown != null) panelWeight else Modifier,
    edges.askFailed,
    controller::sendAskAnswer,
  )
}
