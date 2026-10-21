package io.leloush.fang.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.leloush.fang.AppGraph
import io.leloush.fang.host.AuthStatus
import io.leloush.fang.host.HostHello
import io.leloush.fang.wolfi.WolfiRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal enum class StepStatus { Pending, Running, Done, Failed }

/** What a tap on a setup step asks for; handled by [rememberSetupActions]. */
internal sealed interface SetupAction {
  data object PrepareRuntime : SetupAction

  data object AllowNotifications : SetupAction

  data object AllowBackground : SetupAction

  data object AllowStorage : SetupAction

  data object InstallPi : SetupAction

  data class Login(val providerId: String) : SetupAction

  data object AddProvider : SetupAction
}

/** What Android and the app's own runtime report; re-read whenever Fang Harness comes back. */
internal data class DeviceChecks(
  val runtimeInstalled: Boolean,
  val runtimeVersion: String?,
  /** Null below API 33, where notifications need no runtime permission and the step is hidden. */
  val notificationsGranted: Boolean?,
  /** Fang Harness is exempt from battery optimization, so Android keeps its connection to the agent while the screen is off. */
  val backgroundAllowed: Boolean,
  /** Fang Harness can write outside its own folders, which is what lets the Wolfi shell work on the user's projects. */
  val allFilesAllowed: Boolean,
) {
  companion object {
    fun read(context: Context): DeviceChecks {
      val runtime = WolfiRuntime(context).status()
      return DeviceChecks(
        runtimeInstalled = runtime.installed,
        runtimeVersion = runtime.version,
        notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
          null
        },
        backgroundAllowed = context.getSystemService(PowerManager::class.java)
          .isIgnoringBatteryOptimizations(context.packageName),
        allFilesAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          Environment.isExternalStorageManager()
        } else {
          context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        },
      )
    }
  }
}

/** [DeviceChecks] kept current: [refresh] re-reads them for a few seconds, since some settings report late. */
@Stable
internal class LiveDeviceChecks(initial: DeviceChecks) {
  var value by mutableStateOf(initial)
    internal set
  internal var rechecks by mutableIntStateOf(0)
    private set

  fun refresh() {
    rechecks++
  }
}

/**
 * Re-reads [DeviceChecks] when Fang Harness resumes or its window regains focus (a permission dialog or the notification
 * shade closing), then every [RECHECK_MILLIS] for a few seconds, since Android reports some changes late. With [poll]
 * it keeps reading while resumed.
 */
@Composable
internal fun rememberDeviceChecks(poll: Boolean = false): LiveDeviceChecks {
  val context = LocalContext.current
  val live = remember { LiveDeviceChecks(DeviceChecks.read(context)) }
  var resumed by remember { mutableStateOf(false) }
  val focused = LocalWindowInfo.current.isWindowFocused
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    resumed = true
    live.refresh()
  }
  LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { resumed = false }
  LaunchedEffect(focused) { if (focused) live.refresh() }
  LaunchedEffect(live.rechecks) {
    repeat(RECHECKS) {
      live.value = withContext(Dispatchers.IO) { DeviceChecks.read(context) }
      delay(RECHECK_MILLIS)
    }
  }
  LaunchedEffect(poll && resumed) {
    if (!(poll && resumed)) return@LaunchedEffect
    while (true) {
      delay(POLL_MILLIS)
      live.value = withContext(Dispatchers.IO) { DeviceChecks.read(context) }
    }
  }
  return live
}

private const val RECHECKS = 10
private const val RECHECK_MILLIS = 300L
private const val POLL_MILLIS = 1_000L

/** Everything the setup screens render, derived into one status per step. */
internal data class OnboardingState(
  val checks: DeviceChecks,
  val hello: HostHello?,
  val expectedPiVersion: String,
  val setupComplete: Boolean,
  /** [SetupStep] names (and [WELCOME]) the user moved past without finishing; onboarding doesn't reopen them. */
  val passedPages: Set<String>,
  val blockedPermissions: Set<String>,
  val installing: Boolean,
  val runtimeInstalling: Boolean,
  val progress: List<String>,
  val runtimeProgress: List<String>,
  val error: SetupError?,
  val authStatus: AuthStatus?,
) {
  val piReady: Boolean get() = hello?.piVersion == expectedPiVersion

  /** The local Wolfi runtime is ready, so a host can be started at all. */
  val canLaunchHost: Boolean get() = checks.runtimeInstalled

  /** Pi installs as soon as the runtime is ready; a failure stops the automatic start until the screen is resumed. */
  val shouldAutoInstall: Boolean get() = canLaunchHost && !piReady && !installing

  /** The steps this phone needs, in order. */
  val visibleSteps: List<SetupStep>
    get() = SetupStep.entries.filter { step ->
      step != SetupStep.Notifications || checks.notificationsGranted != null
    }

  /** The failure still standing: one left over from a launch is dropped once Android shows the step fixed. */
  val currentError: SetupError?
    get() = error?.takeUnless { it.step == SetupStep.Runtime && checks.runtimeInstalled }

  fun errorFor(step: SetupStep): SetupError? = currentError?.takeIf { it.step == step }

  /** The first step after [after] (from the start when null) that still needs the user. */
  fun nextOpenStep(after: SetupStep? = null): SetupStep? = visibleSteps
    .dropWhile { after != null && it.ordinal <= after.ordinal }
    .firstOrNull { status(it) != StepStatus.Done && it.name !in passedPages }

  fun status(step: SetupStep): StepStatus = when (step) {
    SetupStep.Runtime -> runtimeStatus()
    SetupStep.Notifications -> if (checks.notificationsGranted == false) StepStatus.Pending else StepStatus.Done
    SetupStep.Background -> if (checks.backgroundAllowed) StepStatus.Done else StepStatus.Pending
    SetupStep.Storage -> if (checks.allFilesAllowed) StepStatus.Done else StepStatus.Pending
    SetupStep.InstallPi -> installPiStatus()
    SetupStep.SignIn -> if (authStatus?.anyConfigured == true) StepStatus.Done else StepStatus.Pending
  }

  private fun runtimeStatus(): StepStatus = when {
    checks.runtimeInstalled -> StepStatus.Done
    errorFor(SetupStep.Runtime) != null -> StepStatus.Failed
    runtimeInstalling -> StepStatus.Running
    else -> StepStatus.Pending
  }

  private fun installPiStatus(): StepStatus = when {
    installing -> StepStatus.Running
    piReady -> StepStatus.Done
    errorFor(SetupStep.InstallPi) != null -> StepStatus.Failed
    else -> StepStatus.Pending
  }

  companion object {
    /** The welcome page's entry in [passedPages]. */
    const val WELCOME = "welcome"
  }
}

@Composable
internal fun rememberOnboardingState(graph: AppGraph, checks: DeviceChecks): OnboardingState {
  val hello by graph.hostClient.lastHello.collectAsStateWithLifecycle()
  val lastFailure by graph.hostClient.lastFailure.collectAsStateWithLifecycle()
  val hostInstalling by graph.hostClient.installing.collectAsStateWithLifecycle()
  val setupComplete by graph.preferences.setupComplete.collectAsStateWithLifecycle()
  val passedPages by graph.preferences.setupPagesPassed.collectAsStateWithLifecycle()
  val authStatus by graph.accountRepository.status.collectAsStateWithLifecycle()
  val blockedPermissions by SetupProgress.blockedPermissions.collectAsStateWithLifecycle()
  val installing by SetupProgress.installing.collectAsStateWithLifecycle()
  val progress by SetupProgress.progress.collectAsStateWithLifecycle()
  val runtimeInstalling by SetupProgress.runtimeInstalling.collectAsStateWithLifecycle()
  val runtimeProgress by SetupProgress.runtimeProgress.collectAsStateWithLifecycle()
  val runtimeError by SetupProgress.runtimeError.collectAsStateWithLifecycle()
  val installError by SetupProgress.error.collectAsStateWithLifecycle()
  return OnboardingState(
    checks = checks,
    hello = hello,
    expectedPiVersion = graph.hostClient.expectedPiVersion,
    setupComplete = setupComplete,
    passedPages = passedPages,
    blockedPermissions = blockedPermissions,
    installing = installing || hostInstalling,
    runtimeInstalling = runtimeInstalling,
    progress = progress,
    runtimeProgress = runtimeProgress,
    // A failure fixed in the runtime brought the user here from elsewhere (chat, sidebar); show it on its step.
    error = runtimeError ?: installError ?: lastFailure?.takeIf { it.fixedBySetup }?.toSetupError(),
    authStatus = authStatus,
  )
}

/**
 * Starts the Wolfi install when the runtime is missing, then Pi as soon as the runtime is ready. A failure stops the
 * automatic start until the screen is resumed, rather than relaunching in a loop. Refreshes sign-in once Pi is installed.
 */
@Composable
internal fun AutoInstall(graph: AppGraph, state: OnboardingState, onChecksChanged: () -> Unit) {
  val needsRuntime = !state.checks.runtimeInstalled && !state.runtimeInstalling && state.errorFor(SetupStep.Runtime) == null
  LaunchedEffect(needsRuntime) {
    if (needsRuntime) SetupProgress.prepareRuntime(graph)
  }
  LaunchedEffect(state.runtimeInstalling) {
    if (!state.runtimeInstalling) onChecksChanged()
  }
  LaunchedEffect(state.shouldAutoInstall && state.currentError == null) {
    if (state.shouldAutoInstall && state.currentError == null) SetupProgress.install(graph)
  }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    if (needsRuntime) SetupProgress.prepareRuntime(graph)
    if (state.shouldAutoInstall) SetupProgress.install(graph)
  }
  LaunchedEffect(state.piReady) {
    if (state.piReady && graph.accountRepository.status.value == null) graph.accountRepository.refresh()
  }
}
