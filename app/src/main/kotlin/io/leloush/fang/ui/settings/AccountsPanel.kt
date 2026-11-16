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
import androidx.compose.runtime.Stable
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
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.components.SnackbarStackState
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** The three things the accounts rows need to run a call and report its failure, bundled so helpers stay small. */
private class AccountActions(
  val scope: kotlinx.coroutines.CoroutineScope,
  val accounts: AccountRepository,
  val snackbar: SnackbarStackState,
  val failed: String,
)

/** The dialog state of one panel; snapshot-backed so composables read and write it during composition. */
@Stable
private class AccountsPanelState {
  var addingLabel by mutableStateOf(false)
  var addingKey by mutableStateOf<String?>(null)
  var deleting by mutableStateOf<AccountInfo?>(null)
}

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
  pool: ProviderAccounts?,
  onAddOauth: (providerId: String) -> Unit,
  onManageAccounts: (providerId: String) -> Unit,
  onChanged: () -> Unit,
) {
  val providerId = subscription.id
  val failed = stringResource(R.string.provider_failed)
  val scope = rememberCoroutineScope()
  val actions = remember(accounts, snackbar, failed, scope) {
    AccountActions(scope = scope, accounts = accounts, snackbar = snackbar, failed = failed)
  }
  var expanded by rememberSaveable(providerId) { mutableStateOf(false) }
  val state = remember { AccountsPanelState() }
  val count = pool?.accounts?.size
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
      AccountsExpanded(
        pool = pool,
        subscription = subscription,
        actions = actions,
        state = state,
        onAddOauth = { onAddOauth(providerId) },
        onManage = { onManageAccounts(providerId) },
        onChanged = onChanged,
      )
    }
  }
  AccountsPanelDialogs(state, providerId, actions, onChanged)
}

/** The expanded body: the account rows, the ways to add one, and a jump to the full management screen. */
@Composable
private fun AccountsExpanded(
  pool: ProviderAccounts?,
  subscription: Subscription,
  actions: AccountActions,
  state: AccountsPanelState,
  onAddOauth: () -> Unit,
  onManage: () -> Unit,
  onChanged: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    HorizontalDivider(color = fangColors.border, thickness = 1.dp)
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = FangTokens.Spacing.s12, vertical = FangTokens.Spacing.s8),
      verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    ) {
      when {
        pool == null -> SettingsCaption(text = stringResource(R.string.accounts_loading))

        pool.accounts.isEmpty() -> SettingsCaption(text = stringResource(R.string.accounts_empty))

        else -> AccountsPanelList(pool, actions, state, onChanged)
      }
      AccountsPanelFooter(
        usesApiKey = subscription.usesApiKey,
        state = state,
        onAddOauth = onAddOauth,
        onManage = onManage,
      )
    }
  }
}

@Composable
private fun AccountsPanelList(
  provider: ProviderAccounts,
  actions: AccountActions,
  state: AccountsPanelState,
  onChanged: () -> Unit,
) {
  SettingsSection(title = null) {
    provider.accounts.forEachIndexed { index, account ->
      AccountRow(
        account = account,
        scope = actions.scope,
        accounts = actions.accounts,
        snackbar = actions.snackbar,
        failed = actions.failed,
        onDelete = { state.deleting = it },
        onReload = onChanged,
      )
      if (index < provider.accounts.lastIndex) SettingsDivider()
    }
  }
}

/** The two ways to add an account beside the manage row. */
@Composable
private fun AccountsPanelFooter(
  usesApiKey: Boolean,
  state: AccountsPanelState,
  onAddOauth: () -> Unit,
  onManage: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8)) {
    AccountsAddActions(usesApiKey = usesApiKey, onAddApiKey = { state.addingLabel = true }, onAddOauth = onAddOauth)
    SettingsRow(title = stringResource(R.string.accounts_manage_all), onClick = onManage) { Chevron() }
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

@Composable
private fun AccountsPanelDialogs(
  state: AccountsPanelState,
  providerId: String,
  actions: AccountActions,
  onChanged: () -> Unit,
) {
  AccountsAddDialogs(
    addingLabel = state.addingLabel,
    addingKey = state.addingKey,
    onAddingLabelChange = { state.addingLabel = it },
    onAddingKeyChange = { state.addingKey = it },
    onSave = { label, key -> saveApiKey(actions.accounts, providerId, label, key, actions.snackbar, actions.failed) },
    onReload = onChanged,
  )
  state.deleting?.let { account ->
    AccountsDeleteDialog(
      account = account,
      accounts = actions.accounts,
      scope = actions.scope,
      snackbar = actions.snackbar,
      failed = actions.failed,
      onDeletingChange = { state.deleting = it },
      onReload = onChanged,
    )
  }
}
