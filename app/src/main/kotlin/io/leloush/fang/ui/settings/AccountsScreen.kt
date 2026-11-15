package io.leloush.fang.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import io.leloush.fang.ui.components.IconButton28
import io.leloush.fang.ui.components.SnackbarStackState
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors
import java.io.IOException
import kotlinx.coroutines.launch

/**
 * The accounts held for one provider, with the rotation strategy. A provider can hold several API keys or sign-ins;
 * Fang Harness picks one per chat and disables or cools down one that fails.
 */
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
    list = try {
      accounts.accounts().filter { it.providerId == providerId }
    } catch (failure: IOException) {
      emptyList()
    } catch (rejected: HostRejected) {
      emptyList()
    }
  }
  val provider = list.firstOrNull()
  SettingsPage(title = stringResource(R.string.settings_accounts), onBack = onBack, snackbar = snackbar) {
    if (provider == null) {
      SettingsCaption(text = stringResource(R.string.accounts_empty))
      return@SettingsPage
    }
    // Strategy
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
                accounts.accountStrategy(providerId, strategy)
                reloads++
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
    // Accounts
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
            onClick = { deleting = account },
            tint = fangColors.foregroundMuted,
          )
        }
        SettingsDivider()
      }
      DashedActionRow(label = stringResource(R.string.accounts_add), onClick = { addingLabel = true })
    }
  }
  if (addingLabel) {
    TextInputDialog(
      title = stringResource(R.string.accounts_label),
      initial = "",
      placeholder = null,
      confirmLabel = stringResource(R.string.accounts_add),
      multiline = false,
      onConfirm = { label ->
        addingLabel = false
        addingKey = label
      },
      onDismiss = { addingLabel = false },
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
        addingKey = null
        scope.launch {
          try {
            accounts.saveAccount(providerId, label, "api_key", key, null, true)
            reloads++
          } catch (failure: IOException) {
            snackbar.show(failure.message ?: failed)
          } catch (rejected: HostRejected) {
            snackbar.show(rejected.error.ifEmpty { failed })
          }
        }
      },
      onDismiss = { addingKey = null },
    )
  }
  deleting?.let { account ->
    ConfirmDialog(
      title = stringResource(R.string.accounts_remove),
      text = account.label,
      confirmLabel = stringResource(R.string.accounts_remove),
      destructive = true,
      onConfirm = {
        deleting = null
        scope.launch {
          try {
            accounts.removeAccount(account.id)
            reloads++
          } catch (failure: IOException) {
            snackbar.show(failure.message ?: failed)
          } catch (rejected: HostRejected) {
            snackbar.show(rejected.error.ifEmpty { failed })
          }
        }
      },
      onDismiss = { deleting = null },
    )
  }
}
