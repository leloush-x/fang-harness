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
      DashedActionRow(label = stringResource(R.string.accounts_add), icon = R.drawable.ic_lucide_plus, onClick = { addingLabel = true })
      return@SettingsPage
    }
    AccountsStrategy(provider, accounts, scope, snackbar, failed) { reloads++ }
    AccountsList(provider, scope, accounts, snackbar, failed, onAdd = { addingLabel = true }) { reloads++ }
  }
  AccountsAddDialogs(
    addingLabel = addingLabel,
    addingKey = addingKey,
    onAddingLabelChange = { addingLabel = it },
    onAddingKeyChange = { addingKey = it },
    onSave = { label, key -> saveApiKey(accounts, providerId, label, key, snackbar, failed) },
    onReload = { reloads++ },
  )
  deleting?.let { account ->
    AccountsDeleteDialog(
      account = account,
      accounts = accounts,
      scope = scope,
      snackbar = snackbar,
      failed = failed,
      onDeletingChange = { deleting = it },
      onReload = { reloads++ },
    )
  }
}

@Composable
private fun AccountsAddDialogs(
  addingLabel: Boolean,
  addingKey: String?,
  onAddingLabelChange: (Boolean) -> Unit,
  onAddingKeyChange: (String?) -> Unit,
  onSave: suspend (String, String) -> Boolean,
  onReload: () -> Unit,
) {
  val scope = rememberCoroutineScope()
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
          if (onSave(label, key)) {
            onReload()
          }
        }
      },
      onDismiss = { onAddingKeyChange(null) },
    )
  }
}

@Composable
private fun AccountsDeleteDialog(
  account: AccountInfo,
  accounts: AccountRepository,
  scope: kotlinx.coroutines.CoroutineScope,
  snackbar: SnackbarStackState,
  failed: String,
  onDeletingChange: (AccountInfo?) -> Unit,
  onReload: () -> Unit,
) {
  ConfirmDialog(
    title = stringResource(R.string.accounts_remove),
    text = account.label,
    confirmLabel = stringResource(R.string.accounts_remove),
    destructive = true,
    onConfirm = {
      onDeletingChange(null)
      scope.launch { accountCall(Unit, snackbar, failed) { accounts.removeAccount(account.id); onReload() } }
    },
    onDismiss = { onDeletingChange(null) },
  )
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
          scope.launch { accountCall(Unit, snackbar, failed) { accounts.accountStrategy(provider.providerId, strategy); onReload() } }
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
        onClick = { scope.launch { accountCall(Unit, snackbar, failed) { accounts.activateAccount(account.id); onReload() } } },
      ) {
        if (account.state == "active") SettingsValue("✓")
        IconButton28(
          icon = R.drawable.ic_lucide_trash_2,
          contentDescription = stringResource(R.string.accounts_remove),
          onClick = {
            scope.launch { accountCall(Unit, snackbar, failed) { accounts.removeAccount(account.id); onReload() } }
          },
          tint = fangColors.foregroundMuted,
        )
      }
      SettingsDivider()
    }
    DashedActionRow(label = stringResource(R.string.accounts_add), icon = R.drawable.ic_lucide_plus, onClick = onAdd)
  }
}

/** Runs an account call, showing its failure in the snackbar; [default] when it failed. */
private suspend fun <T> accountCall(
  default: T,
  snackbar: SnackbarStackState,
  failed: String,
  call: suspend () -> T,
): T = try {
  call()
} catch (failure: IOException) {
  snackbar.show(failure.message ?: failed)
  default
} catch (rejected: HostRejected) {
  snackbar.show(rejected.error.ifEmpty { failed })
  default
}

private suspend fun loadAccounts(
  accounts: AccountRepository,
  providerId: String,
  snackbar: SnackbarStackState,
  failed: String,
): List<ProviderAccounts> = accountCall(emptyList(), snackbar, failed) {
  accounts.accounts().filter { it.providerId == providerId }
}

private suspend fun saveApiKey(
  accounts: AccountRepository,
  providerId: String,
  label: String,
  apiKey: String,
  snackbar: SnackbarStackState,
  failed: String,
): Boolean = accountCall(false, snackbar, failed) {
  accounts.saveAccount(providerId, label, "api_key", apiKey, null, true)
  true
}
