package io.leloush.fang.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.R
import io.leloush.fang.appGraph
import io.leloush.fang.data.AccountRepository
import io.leloush.fang.host.AccountInfo
import io.leloush.fang.host.HostRejected
import io.leloush.fang.host.ProviderAccounts
import io.leloush.fang.ui.chat.ConfirmDialog
import io.leloush.fang.ui.chat.TextInputDialog
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.components.SnackbarStackState
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import java.io.IOException
import kotlinx.coroutines.launch

@Composable
internal fun AccountsScreen(providerId: String, onBack: () -> Unit) {
  val context = LocalContext.current
  val accounts = context.appGraph.accountRepository
  val scope = rememberCoroutineScope()
  val snackbar = remember { SnackbarStackState() }
  var list by remember { mutableStateOf<List<ProviderAccounts>>(emptyList()) }
  var reloads by remember { mutableStateOf(0) }
  var addingLabel by remember { mutableStateOf(false) }
  var addingKey by remember { mutableStateOf<String?>(null) }
  var deleting by remember { mutableStateOf<AccountInfo?>(null) }
  val failed = stringResource(R.string.provider_failed)
  LaunchedEffect(reloads) {
    list = loadAccounts(accounts, providerId, snackbar, failed)
  }
  val provider = list.firstOrNull()
  SettingsPage(title = stringResource(R.string.settings_accounts), onBack = onBack, snackbar = snackbar) {
    if (provider == null) {
      SettingsCaption(text = stringResource(R.string.accounts_empty))
      return@SettingsPage
    }
    AccountsStrategy(provider, accounts, scope, snackbar, failed) { reloads++ }
    AccountsList(provider, scope, accounts, snackbar, failed, onAdd = { addingLabel = true }) { reloads++ }
  }
  AccountsDialogs(
    addingLabel = addingLabel,
    addingKey = addingKey,
    deleting = deleting,
    providerId = providerId,
    accounts = accounts,
    scope = scope,
    snackbar = snackbar,
    failed = failed,
    onAddingLabelChange = { addingLabel = it },
    onAddingKeyChange = { addingKey = it },
    onDeletingChange = { deleting = it },
    onReload = { reloads++ },
  )
}

@Composable
private fun AccountsDialogs(
  addingLabel: Boolean,
  addingKey: String?,
  deleting: AccountInfo?,
  providerId: String,
  accounts: AccountRepository,
  scope: kotlinx.coroutines.CoroutineScope,
  snackbar: SnackbarStackState,
  failed: String,
  onAddingLabelChange: (Boolean) -> Unit,
  onAddingKeyChange: (String?) -> Unit,
  onDeletingChange: (AccountInfo?) -> Unit,
  onReload: () -> Unit,
) {
  if (addingLabel) {
    TextInputDialog(
      title = stringResource(R.string.accounts_label),
      initial = "",
      placeholder = null,
      confirmLabel = stringResource(R.string.accounts_add),
      multiline = false,
      onConfirm = { label ->
        onAddingLabelChange(false)
        onAddingKeyChange(label)
      },
      onDismiss = { onAddingLabelChange(false) },
    )
  }
  addingKey?.let { label ->
    TextInputDialog(
      title = stringResource(R.string.accounts_key),
      initial = "",
      placeholder = null,
      confirmLabel = stringResource(R.string.accounts_add),
      multiline = false,
      onConfirm = { key ->
        onAddingKeyChange(null)
        scope.launch {
          try {
            accounts.saveAccount(providerId, label, "api_key", key, null, true)
            onReload()
          } catch (failure: IOException) {
            snackbar.show(failure.message ?: failed)
          } catch (rejected: HostRejected) {
            snackbar.show(rejected.error.ifEmpty { failed })
          }
        }
      },
      onDismiss = { onAddingKeyChange(null) },
    )
  }
  deleting?.let { account ->
    ConfirmDialog(
      title = stringResource(R.string.accounts_remove),
      text = account.label,
      confirmLabel = stringResource(R.string.accounts_remove),
      destructive = true,
      onConfirm = {
        onDeletingChange(null)
        scope.launch {
          try {
            accounts.removeAccount(account.id)
            onReload()
          } catch (failure: IOException) {
            snackbar.show(failure.message ?: failed)
          } catch (rejected: HostRejected) {
            snackbar.show(rejected.error.ifEmpty { failed })
          }
        }
      },
      onDismiss = { onDeletingChange(null) },
    )
  }
}

@Composable
private fun AccountsStrategy(
  provider: ProviderAccounts,
  accounts: AccountRepository,
  scope: kotlinx.coroutines.CoroutineScope,
  snackbar: SnackbarStackState,
  failed: String,
  onReload: () -> Unit,
) {
  SettingsSection(title = stringResource(R.string.accounts_strategy)) {
    listOf("round_robin", "sticky", "manual").forEach { strategy ->
      val selected = provider.strategy == strategy
      SettingsRow(
        title = stringResource(
          when (strategy) {
            "sticky" -> R.string.accounts_strategy_sticky
            "manual" -> R.string.accounts_strategy_manual
            else -> R.string.accounts_strategy_round_robin
          },
        ),
        onClick = {
          scope.launch {
            try {
              accounts.accountStrategy(provider.providerId, strategy)
              onReload()
            } catch (failure: IOException) {
              snackbar.show(failure.message ?: failed)
            } catch (rejected: HostRejected) {
              snackbar.show(rejected.error.ifEmpty { failed })
            }
          }
        },
      ) {
        if (selected) SettingsValue("✓")
      }
      SettingsDivider()
    }
  }
}

@Composable
private fun AccountsList(
  provider: ProviderAccounts,
  scope: kotlinx.coroutines.CoroutineScope,
  accounts: AccountRepository,
  snackbar: SnackbarStackState,
  failed: String,
  onAdd: () -> Unit,
  onReload: () -> Unit,
) {
  SettingsSection(title = stringResource(R.string.settings_accounts)) {
    if (provider.accounts.isEmpty()) {
      SettingsCaption(text = stringResource(R.string.accounts_empty))
    }
    provider.accounts.forEach { account ->
      SettingsRow(
        title = account.label,
        subtitle = account.masked + " · " + account.state,
      ) {
        IconButton28(
          icon = R.drawable.ic_lucide_trash_2,
          contentDescription = stringResource(R.string.accounts_remove),
          onClick = {
            scope.launch {
              try {
                accounts.removeAccount(account.id)
                onReload()
              } catch (failure: IOException) {
                snackbar.show(failure.message ?: failed)
              } catch (rejected: HostRejected) {
                snackbar.show(rejected.error.ifEmpty { failed })
              }
            }
          },
          tint = fangColors.foregroundMuted,
        )
      }
      SettingsDivider()
    }
    DashedActionRow(label = stringResource(R.string.accounts_add), icon = R.drawable.ic_lucide_plus, onClick = onAdd)
  }
}

private suspend fun loadAccounts(
  accounts: AccountRepository,
  providerId: String,
  snackbar: SnackbarStackState,
  failed: String,
): List<ProviderAccounts> = try {
  accounts.accounts().filter { it.providerId == providerId }
} catch (failure: IOException) {
  snackbar.show(failure.message ?: failed)
  emptyList()
} catch (rejected: HostRejected) {
  snackbar.show(rejected.error.ifEmpty { failed })
  emptyList()
}
