package io.leloush.fang.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.AppGraph
import io.leloush.fang.R
import io.leloush.fang.data.BundledExtension
import io.leloush.fang.host.HostRejected
import io.leloush.fang.ui.chat.ConfirmDialog
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.components.SimmeringPot
import io.leloush.fang.ui.components.SnackbarStackState
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A centred panel listing every extension Fang Harness knows: bundled ones switch on and off like Quick Settings tiles,
 * and Pi packages installed into the agent can be removed. [onDismiss] closes it without changing anything.
 */
@Composable
internal fun MoreExtensionsPanel(graph: AppGraph, snackbar: SnackbarStackState, onDismiss: () -> Unit) {
  val disabled by graph.preferences.disabledExtensions.collectAsStateWithLifecycle()
  var sources by remember { mutableStateOf<List<String>?>(null) }
  var readFailed by remember { mutableStateOf(false) }
  var removing by remember { mutableStateOf<String?>(null) }
  var confirming by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  LaunchedEffect(Unit) {
    try {
      sources = graph.hostClient.installedPackages()
    } catch (_: IOException) {
      readFailed = true
    } catch (_: HostRejected) {
      readFailed = true
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier.padding(24.dp).widthIn(max = PANEL_MAX_WIDTH).heightIn(max = PANEL_MAX_HEIGHT),
      shape = RoundedCornerShape(FangTokens.Radius.xxl),
      color = fangColors.surface1,
      contentColor = fangColors.foreground,
      tonalElevation = PANEL_ELEVATION,
    ) {
      Column {
        PanelHeader(onDismiss)
        HorizontalDivider(color = fangColors.border)
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
          section(R.string.settings_more_extensions_bundled)
          items(BundledExtension.entries, key = { it.id }) { extension ->
            BundledRow(extension, extension.id !in disabled) { on ->
              graph.preferences.setExtensionEnabled(extension, on)
              graph.sessionRepository.forgetExtensionUi()
              graph.registry.reloadIdle()
            }
          }
          section(R.string.settings_more_extensions_installed)
          item {
            SettingsCaption(
              text = stringResource(R.string.settings_more_extensions_installed_caption),
              modifier = Modifier.padding(horizontal = FangTokens.Spacing.s16),
            )
          }
          installedRows(sources, readFailed, removing) { confirming = it }
        }
        HorizontalDivider(color = fangColors.border)
      }
    }
  }

  confirmRemoval(graph, snackbar, scope, confirming, { confirming = null }, { removing = it }) { sources = it }
}

/** The panel's title and close button. */
@Composable
private fun PanelHeader(onDismiss: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = FangTokens.Spacing.s16, vertical = FangTokens.Spacing.s12),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    Text(
      text = stringResource(R.string.settings_more_extensions_title),
      modifier = Modifier.weight(1f).semantics { heading() },
      color = fangColors.foreground,
      fontSize = FangTokens.FontSize.xl,
    )
    IconButton28(
      icon = R.drawable.ic_lucide_x,
      contentDescription = stringResource(R.string.settings_more_extensions_close),
      onClick = onDismiss,
      tint = fangColors.foregroundMuted,
    )
  }
}

/** A muted label between the panel's two groups. */
private fun LazyListScope.section(@StringRes label: Int) {
  item {
    Text(
      text = stringResource(label),
      modifier = Modifier.padding(horizontal = FangTokens.Spacing.s16, vertical = FangTokens.Spacing.s8),
      color = fangColors.foregroundMuted,
      fontSize = FangTokens.FontSize.sm,
    )
  }
}

/** A bundled extension's switch, with the same reload chain the settings list uses. */
@Composable
private fun BundledRow(extension: BundledExtension, enabled: Boolean, onToggle: (Boolean) -> Unit) {
  SettingsRow(
    title = stringResource(extension.title),
    subtitle = stringResource(extension.summary),
    modifier = Modifier.toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle),
  ) {
    Switch(checked = enabled, onCheckedChange = null)
  }
}

/** The installed packages, their loading state, or the empty and failure lines. */
private fun LazyListScope.installedRows(
  sources: List<String>?,
  readFailed: Boolean,
  removing: String?,
  onRemove: (String) -> Unit,
) {
  when {
    sources == null && !readFailed -> item { PaddingLine(stringResource(R.string.settings_more_extensions_loading), true) }
    readFailed -> item { PaddingLine(stringResource(R.string.settings_more_extensions_failed), false) }
    sources.isNullOrEmpty() -> item { PaddingLine(stringResource(R.string.settings_more_extensions_empty), false) }
    else -> items(sources, key = { it }) { source ->
      SettingsRow(
        title = source,
        enabled = removing == null,
        leading = {
          LucideIcon(R.drawable.ic_lucide_plug, null, tint = fangColors.foregroundMuted)
        },
      ) {
        if (removing == source) {
          SimmeringPot()
        } else {
          IconButton28(
            icon = R.drawable.ic_lucide_trash_2,
            contentDescription = stringResource(R.string.settings_more_extensions_remove, source),
            onClick = { onRemove(source) },
            tint = fangColors.destructive,
          )
        }
      }
    }
  }
}

/** One muted line inside the panel's list. */
@Composable
private fun PaddingLine(text: String, busy: Boolean) {
  Row(
    modifier = Modifier.padding(horizontal = FangTokens.Spacing.s16, vertical = FangTokens.Spacing.s8),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    if (busy) SimmeringPot()
    Text(text = text, color = fangColors.foregroundMuted, fontSize = FangTokens.FontSize.sm)
  }
}

/** Asks before uninstalling, then reloads idle chats and reads the package list again. */
@Composable
private fun confirmRemoval(
  graph: AppGraph,
  snackbar: SnackbarStackState,
  scope: CoroutineScope,
  confirming: String?,
  onConfirming: (String?) -> Unit,
  onRemoving: (String?) -> Unit,
  onSources: (List<String>) -> Unit,
) {
  val source = confirming ?: return
  val done = stringResource(R.string.settings_more_extensions_removed)
  val failed = stringResource(R.string.settings_more_extensions_remove_failed)
  ConfirmDialog(
    title = stringResource(R.string.settings_more_extensions_remove_title, source),
    text = stringResource(R.string.settings_more_extensions_remove_text),
    confirmLabel = stringResource(R.string.settings_more_extensions_remove_confirm),
    destructive = true,
    onConfirm = {
      onConfirming(null)
      onRemoving(source)
      scope.launch {
        try {
          graph.hostClient.removePackage(source)
          graph.sessionRepository.forgetExtensionUi()
          graph.registry.reloadIdle()
          onSources(graph.hostClient.installedPackages())
          snackbar.show(done)
        } catch (_: IOException) {
          snackbar.show(failed)
        } catch (_: HostRejected) {
          snackbar.show(failed)
        } finally {
          onRemoving(null)
        }
      }
    },
    onDismiss = { onConfirming(null) },
  )
}

private val PANEL_MAX_WIDTH = 480.dp
private val PANEL_MAX_HEIGHT = 560.dp
private val PANEL_ELEVATION = 8.dp
