package io.leloush.fang.ui.onboarding

import io.leloush.fang.AppGraph
import io.leloush.fang.host.HostFailure
import io.leloush.fang.host.HostRejected
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

/** Whether onboarding can move past a step without it: required ones can't be skipped. */
internal enum class Requirement { Required, Recommended, Optional }

/**
 * Setup steps in the order onboarding asks for them: the Wolfi runtime first, so the agent install runs while the user
 * handles the recommended steps. [OnboardingState.visibleSteps] drops the ones this phone skips.
 */
internal enum class SetupStep(val requirement: Requirement) {
  Runtime(Requirement.Required),
  Notifications(Requirement.Recommended),
  Background(Requirement.Recommended),
  Storage(Requirement.Recommended),
  InstallPi(Requirement.Required),
  SignIn(Requirement.Optional),
}

/** A failed attempt pinned to the step that fixes it. */
internal data class SetupError(val step: SetupStep, val detail: String? = null, val failure: HostFailure? = null) {
  /** What went wrong, in the app's language; null for the generic failure. */
  val message: String? get() = failure?.message ?: detail
}

internal fun HostFailure.toSetupError(): SetupError {
  val step = when (kind) {
    HostFailure.Kind.Runtime,
    HostFailure.Kind.Permission,
    HostFailure.Kind.NodeMissing,
    HostFailure.Kind.NodeTooOld,
    -> SetupStep.Runtime

    else -> SetupStep.InstallPi
  }
  return SetupError(step, failure = this)
}

/**
 * Onboarding progress that outlives the screen, kept in memory for the process: which permissions Android stopped
 * prompting for, the Wolfi install and the Pi install. Both run in the app scope so leaving the screen does not abort
 * them.
 */
internal object SetupProgress {
  private const val PROGRESS_LINES = 4

  private val blockedPermissionsState = MutableStateFlow<Set<String>>(emptySet())
  private val runtimeInstallingState = MutableStateFlow(false)
  private val runtimeProgressState = MutableStateFlow<List<String>>(emptyList())
  private val runtimeErrorState = MutableStateFlow<SetupError?>(null)
  private val installingState = MutableStateFlow(false)
  private val progressState = MutableStateFlow<List<String>>(emptyList())
  private val errorState = MutableStateFlow<SetupError?>(null)
  private var runtimeJob: Job? = null
  private var job: Job? = null

  /** Permissions denied with "don't ask again"; their step opens system settings instead. */
  val blockedPermissions: StateFlow<Set<String>> = blockedPermissionsState.asStateFlow()

  val runtimeInstalling: StateFlow<Boolean> = runtimeInstallingState.asStateFlow()

  /** The last few lines of the Wolfi download or Node.js install. */
  val runtimeProgress: StateFlow<List<String>> = runtimeProgressState.asStateFlow()

  val runtimeError: StateFlow<SetupError?> = runtimeErrorState.asStateFlow()

  val installing: StateFlow<Boolean> = installingState.asStateFlow()

  /** The last few `npm ci` output lines of the running or last install. */
  val progress: StateFlow<List<String>> = progressState.asStateFlow()

  /** Why the last install failed; cleared when the next one starts. */
  val error: StateFlow<SetupError?> = errorState.asStateFlow()

  fun setPermissionBlocked(permission: String, blocked: Boolean) {
    blockedPermissionsState.update { if (blocked) it + permission else it - permission }
  }

  /** Downloads and prepares Wolfi OS unless an install is already running. Main thread only. */
  fun prepareRuntime(graph: AppGraph) {
    if (runtimeJob?.isActive == true) return
    runtimeErrorState.value = null
    runtimeProgressState.value = emptyList()
    runtimeInstallingState.value = true
    runtimeJob = graph.scope.launch {
      try {
        graph.keepAlive.acquire("Set up Wolfi OS").use {
          withContext(Dispatchers.IO) {
            graph.wolfi.install { line ->
              if (line.isNotBlank()) runtimeProgressState.update { (it + line).takeLast(PROGRESS_LINES) }
            }
          }
        }
      } catch (failure: IOException) {
        runtimeErrorState.value = SetupError(SetupStep.Runtime, failure.message)
      } catch (failure: CancellationException) {
        throw failure
      } catch (failure: IllegalStateException) {
        runtimeErrorState.value = SetupError(SetupStep.Runtime, failure.message)
      } catch (failure: IllegalArgumentException) {
        runtimeErrorState.value = SetupError(SetupStep.Runtime, failure.message)
      } catch (failure: SecurityException) {
        runtimeErrorState.value = SetupError(SetupStep.Runtime, failure.message)
      } finally {
        runtimeInstallingState.value = false
      }
    }
  }

  /** Installs the pinned Pi runtime unless an install is already running. Main thread only. */
  fun install(graph: AppGraph) {
    if (job?.isActive == true || !graph.wolfi.status().installed) return
    errorState.value = null
    progressState.value = emptyList()
    installingState.value = true
    job = graph.scope.launch {
      try {
        graph.keepAlive.acquire("install Pi").use {
          graph.hostClient.ensure { line ->
            if (line.isNotBlank()) progressState.update { (it + line).takeLast(PROGRESS_LINES) }
          }
        }
        graph.accountRepository.refresh()
      } catch (failure: HostFailure) {
        errorState.value = failure.toSetupError()
      } catch (rejected: HostRejected) {
        errorState.value = SetupError(SetupStep.InstallPi, rejected.error)
      } catch (failure: IOException) {
        errorState.value = SetupError(SetupStep.InstallPi, failure.message)
      } catch (failure: JSONException) {
        errorState.value = SetupError(SetupStep.InstallPi, failure.message)
      } finally {
        installingState.value = false
      }
    }
  }
}
