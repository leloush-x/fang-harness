package io.leloush.fang.ui.settings

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.R
import io.leloush.fang.data.OrbSize
import io.leloush.fang.data.Preferences

/** The working orb's size and where it sits while a chat works. */
@Composable
internal fun OrbSettingsRows(preferences: Preferences) {
  val size by preferences.orbSize.collectAsStateWithLifecycle()
  val floats by preferences.orbFloats.collectAsStateWithLifecycle()
  SettingsRow(title = stringResource(R.string.settings_orb_size)) {
    SegmentedControl(
      options = OrbSize.entries,
      selected = size,
      onSelect = preferences::setOrbSize,
    ) { option, _ ->
      Text(
        text = stringResource(
          when (option) {
            OrbSize.Small -> R.string.settings_orb_size_small
            OrbSize.Medium -> R.string.settings_orb_size_medium
            OrbSize.Large -> R.string.settings_orb_size_large
          },
        ),
      )
    }
  }
  SettingsRow(
    title = stringResource(R.string.settings_orb_floats),
    modifier = Modifier.toggleable(value = floats, role = Role.Switch) {
      preferences.setOrbFloats(!floats)
    },
  ) {
    Switch(checked = floats, onCheckedChange = null)
  }
}
