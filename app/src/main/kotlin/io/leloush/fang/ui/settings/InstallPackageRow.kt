package io.leloush.fang.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.leloush.fang.AppGraph
import io.leloush.fang.R
import io.leloush.fang.host.HostRejected
import io.leloush.fang.ui.chat.TextInputDialog
import io.leloush.fang.ui.components.SimmeringPot
import io.leloush.fang.ui.components.SnackbarStackState
import java.io.IOException
import kotlinx.coroutines.launch

/** Installs a Pi package (extension, skill, prompt or theme) into Fang Harness's config with `pi install`. */
@Composable
internal fun InstallPackageRow(graph: AppGraph, snackbar: SnackbarStackState) {
  val scope = rememberCoroutineScope()
  val failed = stringResource(R.string.provider_failed)
  val done = stringResource(R.string.settings_install_extension_done)
  var source by rememberSaveable { mutableStateOf<String?>(null) }
  var installing by remember { mutableStateOf(false) }
  SettingsRow(
    title = stringResource(R.string.settings_install_extension),
    subtitle = stringResource(R.string.settings_install_extension_subtitle),
    enabled = !installing,
    onClick = { source = "" },
  ) {
    if (installing) SimmeringPot()
  }
  val initial = source ?: return
  TextInputDialog(
    title = stringResource(R.string.settings_install_extension),
    initial = initial,
    placeholder = stringResource(R.string.settings_install_extension_placeholder),
    confirmLabel = stringResource(R.string.settings_install_extension_action),
    multiline = false,
    onConfirm = { value ->
      source = null
      val trimmed = value.trim()
      if (trimmed.isEmpty()) return@TextInputDialog
      installing = true
      scope.launch {
        try {
          graph.accountRepository.installPackage(trimmed) { }
          snackbar.show(done)
        } catch (failure: IOException) {
          snackbar.show(failure.message ?: failed)
        } catch (rejected: HostRejected) {
          snackbar.show(rejected.error.ifEmpty { failed })
        } finally {
          installing = false
        }
      }
    },
    onDismiss = { source = null },
  )
}
