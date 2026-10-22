package io.leloush.fang.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.leloush.fang.R
import io.leloush.fang.host.ModelConfig
import io.leloush.fang.ui.components.LucideIcon
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/**
 * The models the endpoint offers: the free ones in their own section at the top, the rest below, each section
 * retractable. The rows are one line, so a long list still reads at a glance, and a retracted section composes
 * nothing at all, which is what keeps a provider with hundreds of models quick to open.
 */
@Composable
internal fun ModelPickList(models: List<ModelConfig>, draft: ModelDraft) {
  val free = remember(models) { models.filter { it.isFree() } }
  val rest = remember(models) { models.filterNot { it.isFree() } }
  var freeOpen by rememberSaveable { mutableStateOf(true) }
  var restOpen by rememberSaveable { mutableStateOf(false) }
  if (free.isNotEmpty()) {
    ModelSection(
      title = stringResource(R.string.models_free),
      count = free.size,
      open = freeOpen,
      onToggle = { freeOpen = !freeOpen },
    ) { ModelRows(free, draft) }
  }
  if (rest.isNotEmpty()) {
    ModelSection(
      title = stringResource(R.string.models_all),
      count = rest.size,
      open = restOpen,
      onToggle = { restOpen = !restOpen },
    ) { ModelRows(rest, draft) }
  }
}

/** A section of the model list: its name, how many models it holds, and a chevron that retracts it. */
@Composable
private fun ModelSection(
  title: String,
  count: Int,
  open: Boolean,
  onToggle: () -> Unit,
  content: @Composable () -> Unit,
) {
  val colors = fangColors
  SettingsSection(title = null) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onToggle)
        .padding(horizontal = FangTokens.Spacing.s16, vertical = FangTokens.Spacing.s10),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
    ) {
      LucideIcon(
        icon = if (open) R.drawable.ic_lucide_chevron_down else R.drawable.ic_lucide_chevron_right,
        contentDescription = null,
        tint = colors.foregroundMuted,
      )
      Text(
        text = title,
        modifier = Modifier.weight(1f),
        color = colors.foreground,
        fontSize = FangTokens.FontSize.base,
      )
      Text(text = count.toString(), color = colors.foregroundMuted, fontSize = FangTokens.FontSize.meta)
    }
    if (open) {
      SettingsDivider()
      content()
    }
  }
}

/** The models themselves, one line each: its name, what it can do, and whether it is added. */
@Composable
internal fun ModelRows(models: List<ModelConfig>, draft: ModelDraft) {
  val colors = fangColors
  Column(modifier = Modifier.fillMaxWidth()) {
    models.forEach { config ->
      val added = draft.isAdded(config.id)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { draft.toggle(config) }
          .padding(horizontal = FangTokens.Spacing.s16, vertical = FangTokens.Spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s12),
      ) {
        Text(
          text = config.name ?: config.id,
          modifier = Modifier.weight(1f),
          color = colors.foreground,
          fontSize = FangTokens.FontSize.base,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val details = modelDetails(config)
        if (details != null) {
          Text(
            text = details,
            color = colors.foregroundMuted,
            fontSize = FangTokens.FontSize.meta,
            maxLines = 1,
          )
        }
        LucideIcon(
          icon = if (added) R.drawable.ic_lucide_check else R.drawable.ic_lucide_plus,
          contentDescription = null,
          tint = if (added) colors.accent else colors.foregroundMuted,
        )
      }
    }
  }
}
