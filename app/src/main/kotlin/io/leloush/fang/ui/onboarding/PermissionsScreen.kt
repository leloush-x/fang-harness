// POST_NOTIFICATIONS is a compile-time string; the notifications step only exists on API 33+.
@file:SuppressLint("InlinedApi")

package io.leloush.fang.ui.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import io.leloush.fang.R
import io.leloush.fang.appGraph
import io.leloush.fang.ui.components.HeaderBar
import io.leloush.fang.ui.components.IconButton28

/**
 * The setup steps that prepare the Wolfi runtime and grant Fang Harness Android access, as cards, without the agent
 * install and sign-in. Reached from Settings; steps re-check whenever Fang Harness comes back, like onboarding.
 */
@Composable
internal fun PermissionsScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val graph = remember(context) { context.appGraph }
  val checks = rememberDeviceChecks()
  val state = rememberOnboardingState(graph, checks.value)
  val onAction = rememberSetupActions(state, onPermissionResult = checks::refresh)

  SetupPage(
    header = {
      HeaderBar(
        title = stringResource(R.string.settings_permissions),
        leading = {
          IconButton28(
            icon = R.drawable.ic_lucide_chevron_left,
            contentDescription = stringResource(R.string.settings_back),
            onClick = onBack,
          )
        },
      )
    },
  ) {
    val steps = state.visibleSteps - SetupStep.InstallPi - SetupStep.SignIn
    steps.forEachIndexed { index, step -> StepCard(stepSpec(step, state, onAction), number = index + 1) }
  }
}

/**
 * Handles [SetupAction]s; [onPermissionResult] runs after a runtime permission prompt closes. [onLogin] and
 * [onAddProvider] serve the sign-in step, which only onboarding shows.
 */
@Composable
internal fun rememberSetupActions(
  state: OnboardingState,
  onPermissionResult: () -> Unit,
  onLogin: (providerId: String) -> Unit = {},
  onAddProvider: () -> Unit = {},
): (SetupAction) -> Unit {
  val context = LocalContext.current
  val graph = remember(context) { context.appGraph }
  val requestPermission = rememberPermissionRequest(state.blockedPermissions, onPermissionResult)
  return { action ->
    when (action) {
      SetupAction.PrepareRuntime -> SetupProgress.prepareRuntime(graph)
      SetupAction.AllowNotifications -> requestPermission(Manifest.permission.POST_NOTIFICATIONS)
      SetupAction.AllowBackground -> context.startSafely { requestBackgroundExemption(context) }
      SetupAction.AllowStorage -> context.startSafely {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          openAllFilesSettings(context)
        } else {
          requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
      }
      SetupAction.InstallPi -> SetupProgress.install(graph)
      is SetupAction.Login -> onLogin(action.providerId)
      SetupAction.AddProvider -> onAddProvider()
    }
  }
}

/**
 * Returns a request for a runtime permission. Once Android stops prompting ("don't ask again"),
 * the same request opens the system screen where the user can still grant it.
 */
@Composable
private fun rememberPermissionRequest(blocked: Set<String>, onResult: () -> Unit): (String) -> Unit {
  val context = LocalContext.current
  val activity = LocalActivity.current
  val currentBlocked by rememberUpdatedState(blocked)
  val currentOnResult by rememberUpdatedState(onResult)
  fun record(permission: String, granted: Boolean) {
    // Without a rationale after a denial, Android will not show the prompt again.
    val permanentlyDenied = !granted && activity?.shouldShowRequestPermissionRationale(permission) == false
    SetupProgress.setPermissionBlocked(permission, permanentlyDenied)
    currentOnResult()
  }
  val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    record(Manifest.permission.POST_NOTIFICATIONS, it)
  }
  val storage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    record(Manifest.permission.WRITE_EXTERNAL_STORAGE, it)
  }
  return { permission ->
    when {
      permission == Manifest.permission.WRITE_EXTERNAL_STORAGE -> storage.launch(permission)
      permission !in currentBlocked -> notifications.launch(permission)
      else -> context.startSafely { openNotificationSettings(context) }
    }
  }
}

private fun openNotificationSettings(context: Context) {
  context.startActivity(
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
  )
}

private fun openAllFilesSettings(context: Context) {
  val app = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:${context.packageName}".toUri())
  try {
    context.startActivity(app)
  } catch (_: ActivityNotFoundException) {
    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
  }
}

/**
 * Android's own exemption prompt rather than the settings list: Fang Harness is the client of a long-running agent and is
 * not distributed through Google Play, whose policy restricts this request.
 */
@SuppressLint("BatteryLife")
private fun requestBackgroundExemption(context: Context) {
  context.startActivity(
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri()),
  )
}

/** Runs an activity start; a device without a matching app gets a toast, not a crash. */
private inline fun Context.startSafely(start: () -> Unit) {
  try {
    start()
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(this, R.string.onboarding_no_activity, Toast.LENGTH_SHORT).show()
  }
}
