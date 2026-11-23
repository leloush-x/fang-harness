package io.leloush.fang.ui.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** What a notification setting needs: the current grant, a prompt, and the system screen once prompts run out. */
internal data class NotificationPermissionState(
  val granted: Boolean,
  val request: () -> Unit,
  val openSettings: () -> Unit,
)

/**
 * Checks POST_NOTIFICATIONS and exposes [NotificationPermissionState.request] and
 * [NotificationPermissionState.openSettings] for a settings row to call. The grant is rechecked on resume, so
 * changing it in Android's settings is reflected when the user comes back. On Android 12 and below notifications
 * need no runtime permission, so [NotificationPermissionState.granted] is always true there.
 */
@Composable
internal fun rememberNotificationPermissionState(): NotificationPermissionState {
  val context = LocalContext.current
  var granted by remember { mutableStateOf(notificationsGranted(context)) }
  val prompt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
    granted = result
  }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = notificationsGranted(context) }
  return NotificationPermissionState(
    granted = granted,
    request = {
      if (Build.VERSION.SDK_INT >= 33) prompt.launch(Manifest.permission.POST_NOTIFICATIONS) else granted = true
    },
    openSettings = { context.startActivity(appNotificationSettings(context)) },
  )
}

private fun notificationsGranted(context: Context): Boolean {
  if (Build.VERSION.SDK_INT < 33) return true
  return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED
}

private fun appNotificationSettings(context: Context): Intent {
  val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
  return intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
}
