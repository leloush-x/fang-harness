package io.leloush.fang.ui.files

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import io.leloush.fang.R
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import io.leloush.fang.wolfi.WolfiRuntime

/** A folder the explorer can be rooted at, shown under the name [label]. */
internal class ExplorerPlace(val path: String, @StringRes val label: Int)

/**
 * The places the explorer offers: the runtime's own root, which the app's home is, and Android's shared storage
 * beside it. Writing shared storage needs All files access, which Settings › Permissions asks for.
 */
internal fun explorerPlaces(): List<ExplorerPlace> = listOf(
  ExplorerPlace(WolfiRuntime.WOLFI_ROOT, R.string.files_place_root),
  ExplorerPlace(WolfiRuntime.SHARED_STORAGE, R.string.files_place_shared),
)

/**
 * The folders one explorer pane has shown, and which of them it shows now: the workspace it opened on, then each
 * place the user switched to. Back and forward walk the list, as a browser does.
 *
 * Held for the composition's life rather than saved, so a recreated activity starts the pane on its workspace again;
 * the trees it listed are saved (see [ExplorerTrees]), so returning to one is still free.
 */
internal class ExplorerRoots(initial: String) {
  private var roots by mutableStateOf(listOf(initial))
  private var index by mutableStateOf(0)

  /** The folder the pane shows. */
  val shown: String get() = roots.getOrElse(index) { roots.last() }

  val canGoBack: Boolean get() = index > 0

  val canGoForward: Boolean get() = index < roots.lastIndex

  fun goBack() {
    if (canGoBack) index--
  }

  fun goForward() {
    if (canGoForward) index++
  }

  /** Shows [path], dropping what forward would have shown, as a browser does. */
  fun open(path: String) {
    if (path == shown) return
    roots = roots.subList(0, index + 1) + path
    index = roots.lastIndex
  }
}

/** One place the explorer can be rooted at, highlighted while it is the one shown. */
@Composable
internal fun ExplorerPlaceButton(place: ExplorerPlace, active: Boolean, onClick: () -> Unit) {
  val colors = fangColors
  Text(
    text = stringResource(place.label),
    modifier = Modifier
      .clip(RoundedCornerShape(FangTokens.Radius.md))
      .background(if (active) sidebarHighlight() else Color.Transparent)
      .clickable(onClick = onClick)
      .semantics { selected = active }
      .padding(horizontal = FangTokens.Spacing.s8, vertical = FangTokens.Spacing.s4),
    color = if (active) colors.foreground else colors.foregroundMuted,
    fontSize = FangTokens.FontSize.meta,
    maxLines = 1,
  )
}
