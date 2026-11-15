package io.leloush.fang.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.data.AccountRepository
import io.leloush.fang.host.AccountInfo
import io.leloush.fang.host.ProviderAccounts
import io.leloush.fang.ui.components.DashedActionRow
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.components.SnackbarStackState
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/**
 * The accounts box on a provider: one box per provider (in the spirit of the todos and background tabs) that expands
 * in place to show the provider's pooled accounts and the ways to add one — an API key, or another OAuth sign-in.
 * Full management is one tap away on the accounts screen.
 */
@Composable
internal fun ProviderAccountsPanel(
  subscription: Subscription,
  accounts: AccountRepository,
  snackbar: SnackbarStackState,
  refreshKey: Int,
  onAddOauth: (providerId: String) -> Unit,
  onManageAccounts: (providerId: String) -> Unit,
) {
  val providerId = subscription.id
  val failed = stringResource(R.string.provider_failed)
  val scope = rememberCoroutineScope()
  var expanded by rememberSaveable(providerId) { mutableStateOf(false) }
  var list by remember(providerId) { mutableStateOf<List<ProviderAccounts>?>(null) }
  var reloads by remember { mutableStateOf(0) }
  var addingLabel by remember { mutableStateOf(false) }
  var addingKey by remember { mutableStateOf<String?>(null) }
  var deleting by remember { mutableStateOf<AccountInfo?>(null) }
  // One capture per page open catches sign-ins made before the capture existed.
  LaunchedEffect(Unit) {
    accounts.captureAccount(providerId)
    reloads++
  }
  LaunchedEffect(reloads, refreshKey) {
    list = loadAccounts(accounts, providerId, snackbar, failed)
  }
  val provider = list?.firstOrNull()
  val count = provider?.accounts?.size
  val shape = RoundedCornerShape(FangTokens.Radius.lg)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .border(1.dp, fangColors.borderAccent, shape)
      .background(fangColors.surface0),
  ) {
    AccountsBoxButton(count = count, expanded = expanded, onToggle = { expanded = !expanded })
    AnimatedVisibility(expanded) {
      Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = fangColors.border, thickness = 1.dp)
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = FangTokens.Spacing.s12, vertical = FangTokens.Spacing.s8),
          verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
        ) {
          when {
            list == null -> SettingsCaption(text = stringResource(R.string.accounts_loading))
            provider == null || provider.accounts.isEmpty() ->
              SettingsCaption(text = stringResource(R.string.accounts_empty))

            else -> SettingsSection(title = null) {
              provider.accounts.forEachIndexed { index, account ->
                AccountRow(
                  account = account,
                  scope = scope,
                  accounts = accounts,
                  snackbar = snackbar,
                  failed = failed,
                  onDelete = { deleting = it },
                  onReload = { reloads++ },
                )
                if (index < provider.accounts.lastIndex) SettingsDivider()
              }
            }
          }
          AccountsAddActions(
            usesApiKey = subscription.usesApiKey,
            onAddApiKey = { addingLabel = true },
            onAddOauth = { onAddOauth(providerId) },
          )
          SettingsRow(
            title = stringResource(R.string.accounts_manage_all),
            onClick = { onManageAccounts(providerId) },
          ) { Chevron() }
        }
      }
    }
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

/** The box button: `Accounts`, plus how many, opening and folding the panel below it. */
@Composable
private fun AccountsBoxButton(count: Int?, expanded: Boolean, onToggle: () -> Unit) {
  val colors = fangColors
  val label = if (count == null) {
    stringResource(R.string.settings_accounts)
  } else {
    stringResource(R.string.settings_accounts) + " · " + count
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onToggle)
      .padding(horizontal = FangTokens.Spacing.s12, vertical = FangTokens.Spacing.s12),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    LucideIcon(
      icon = R.drawable.ic_lucide_key_round,
      contentDescription = null,
      size = FangTokens.IconSize.sm,
      tint = if ((count ?: 0) > 0) colors.accent else colors.foregroundMuted,
    )
    Text(
      text = label,
      modifier = Modifier.weight(1f),
      color = if ((count ?: 0) > 0) colors.foreground else colors.foregroundMuted,
      fontSize = FangTokens.FontSize.base,
    )
    LucideIcon(
      icon = if (expanded) R.drawable.ic_lucide_chevron_up else R.drawable.ic_lucide_chevron_down,
      contentDescription = null,
      size = FangTokens.IconSize.xs,
      tint = colors.foregroundMuted,
    )
  }
}

/** The two ways to add an account: a pasted API key, or another OAuth sign-in for providers that sign in that way. */
@Composable
private fun AccountsAddActions(usesApiKey: Boolean, onAddApiKey: () -> Unit, onAddOauth: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8)) {
    DashedActionRow(
      label = stringResource(R.string.accounts_add_key),
      icon = R.drawable.ic_lucide_key_round,
      onClick = onAddApiKey,
      modifier = Modifier.weight(1f),
    )
    if (!usesApiKey) {
      DashedActionRow(
        label = stringResource(R.string.accounts_add_oauth),
        icon = R.drawable.ic_lucide_shield_check,
        onClick = onAddOauth,
        modifier = Modifier.weight(1f),
      )
    }
  }
}
