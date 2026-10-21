package io.leloush.fang.ui.sidebar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.R
import io.leloush.fang.appGraph
import io.leloush.fang.ui.components.StatusDot
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** Wolfi/Pi health (tap fixes what needs attention), Add workspace, Archive and Settings. */
@Composable
internal fun SidebarFooter(
  onAddWorkspace: () -> Unit,
  onOpenArchive: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenOnboarding: () -> Unit,
) {
  val colors = fangColors
  Column(modifier = Modifier.fillMaxWidth()) {
    HorizontalDivider(thickness = 1.dp, color = colors.border)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .padding(start = FangTokens.Spacing.s16, end = FangTokens.Spacing.s8),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s4),
    ) {
      HostStatus(onOpenOnboarding, Modifier.weight(1f))
      SidebarIconButton(
        icon = R.drawable.ic_lucide_folder_plus,
        contentDescription = stringResource(R.string.shell_add_workspace),
        onClick = onAddWorkspace,
      )
      SidebarIconButton(
        icon = R.drawable.ic_lucide_archive,
        contentDescription = stringResource(R.string.archive_open),
        onClick = onOpenArchive,
      )
      SidebarIconButton(
        icon = R.drawable.ic_lucide_settings,
        contentDescription = stringResource(R.string.shell_settings),
        onClick = onOpenSettings,
      )
    }
  }
}

/**
 * Wolfi/Pi health. Tapping it opens onboarding when setup needs attention.
 */
@Composable
private fun HostStatus(onOpenOnboarding: () -> Unit, modifier: Modifier) {
  val colors = fangColors
  val context = LocalContext.current
  val hostClient = context.appGraph.hostClient
  val hello by hostClient.lastHello.collectAsStateWithLifecycle()
  val failure by hostClient.lastFailure.collectAsStateWithLifecycle()
  val installing by hostClient.installing.collectAsStateWithLifecycle()
  val onClick: (() -> Unit)? = when {
    installing -> null
    failure?.needsSetup == true -> onOpenOnboarding
    else -> null
  }
  val dot = when {
    installing -> colors.dotWarning
    failure != null -> colors.dotDanger
    hello != null -> colors.dotSuccess
    else -> null
  }
  val label = when {
    installing -> null
    failure?.needsSetup == true -> stringResource(R.string.shell_runtime_attention)
    else -> null
  } ?: hello?.piVersion?.let { stringResource(R.string.shell_runtime_status, it) }
    ?: stringResource(R.string.shell_runtime_status_unknown)
  Row(
    modifier = modifier
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .padding(vertical = FangTokens.Spacing.s8),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
  ) {
    dot?.let { StatusDot(it) }
    Text(
      text = label,
      color = if (onClick != null) colors.foreground else colors.foregroundMuted,
      fontSize = FangTokens.FontSize.sm,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
